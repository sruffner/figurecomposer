package com.srscicomp.fc.uibase;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.srscicomp.common.ui.GUIUtilities;
import com.srscicomp.common.ui.LocalFontEnvironment;
import com.srscicomp.common.ui.MainFrameShower;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.uibase.DirWatcher.Event;
import com.srscicomp.fc.uibase.DirWatcher.Listener;

import static java.nio.file.StandardWatchEventKinds.*;

import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

/**
 * A file system watcher that watches any number of directories for changes.
 * 
 * <p>The watcher registers directories with an underlying {@link WatchService} and continuously polls that service for
 * file system changes in a background daemon thread, while notifying any registered users of those changes on the
 * Swing event dispatch thread. The watcher may be configured to limit notifications to changes in files with certain
 * specified extensions, to ignore changes to "hidden" files (file name starting with '.'), and to ignore changes to
 * subdirectories within any watched directiory.</p>
 * 
 * <p>The watcher polls the underlying watch service for file system events with a specified timeout period. Whenever
 * a timeout occurs, the watcher checks to see if any registered directories have "disappeared" from the file system --
 * eg, when a directory is deleted or renamed, or a networked drive is lost because of a network failure). If a watched
 * directory disappears, the watcher moves it to a "dropped directories" list and notifies registered listeners. The
 * watcher also checks directories in the "dropped list" after a polling timeout to see if any of the lost directories
 * have been restored. If so, the directory is returned to the watch list and listeners are notified that the directory
 * has been restored.</p>
 * 
 * <p>It is possible to register a directory with the watcher that does not exist at time of registration. Such 
 * directory paths are put in the "dropped directories" list instead of the active watch list.</p>
 * 
 * <p><b>NOTE</b>: As of Feb 2023, the {@link WatchService} implementation for MacOSX is STILL a polling-type service 
 * that uses a thread per directory and a default scan periodicity of 10 seconds -- hence there will be delays before
 * <b>DirWatcher</b> is notified of file system changes on the Mac.</p>. 
 * 
 * @author sruffner
 */
public class DirWatcher implements Runnable
{
   /** The underlying service that efficiently watches any registered directories for changes. */
   private WatchService watcher = null;
   /** The map of watched directories. Concurrent hashmap because it may be accessed/modified on multiple threads. */
   private ConcurrentHashMap<Path, WatchKey> watchedMap = null;
   /** 
    * The map of previously watched directories that have disappeared from file system and thus were dropped from the
    * watch service. Value is the system time hen the watcher detected that the directory was lost. During each 
    * polling timeout, the watcher checks to see if any dropped directory has reappeared.
    */
   private ConcurrentHashMap<Path, Long> droppedMap = null;
   /** Flag indicating whether or not watcher is working. */
   private boolean available = false;
   /** If set, the watcher ignores changes to any watched directory entry with a name starting with a period ('.') */
   private boolean ignoreDotFiles = false;
   /** If set, the watcher ignores changes to sub-directory entries within any watched directory. */
   private boolean ignoreSubdirectories = false;
   /** If not empty, watcher only reports changes in files that end in one of these extensions (case ignored). */
   private final String[] watchedExts;
   /** Polling timeout period for the watcher thread, in seconds. */
   private long timeoutSec = 30;
   
   /**
    * Private constructor. Use {@link #startWatcher(List, boolean, boolean, String[])} to start watching directories.
    */
   private DirWatcher(String[] exts)
   {
      // process file extension list, if any
      if(exts != null)
      {
         ArrayList<String> accepted = new ArrayList<>();
         for(String s : exts) if((s != null) && (!s.isEmpty()) && !s.contains("."))
            accepted.add(s.toLowerCase());
         watchedExts = (!accepted.isEmpty()) ? accepted.toArray(new String[0]) : null;
      }
      else
         watchedExts = null;
      try
      {
         watcher = FileSystems.getDefault().newWatchService();
         watchedMap = new ConcurrentHashMap<>();
         droppedMap = new ConcurrentHashMap<>();
         available = true;
      }
      catch(Exception ignored) {}
   }
   
   /**
    * Create a directory watcher for the specified list of file system directories. If successful, a background daemon 
    * thread is started on which this watcher polls a {@link WatchService} to monitor the directories for changes.
    * 
    * @param directories The directory list. Each existing directory in the list is registered in the watcher's active
    * watche list, while any non-existent directory is added to the watcher's "dropped directories" list. Any invalid
    * directory path is ignored. Can be null or empty, in which case no directories are registered initially. See 
    * {@link #registerDirectory(File)}.
    * @param ignoreDotFiles If true, the watcher ignores changes to a file within a watched directory if that file's
    * name begins with a period ('.').
    * @param ignoreSubdirectories If true, the watcher ignores changes to any subdirectory within a watched directory.
    * @param exts If null or empty, the watcher reports changes in any file within a watched directory. Otherwise, it
    * only reports changes in files ending in one of the file extensions (case ignored) in this list. A valid extension
    * must have a non-zero length and cannot contain a period ('.').
    * @return The directory watcher object, or null if unable to create the watcher.
    */
   public static DirWatcher startWatcher(
         List<File> directories, boolean ignoreDotFiles, boolean ignoreSubdirectories, String[] exts)
   {
      DirWatcher out = new DirWatcher(exts);
      if(out.isAvailable())
      {
         if(directories != null) 
            for(File dir : directories) 
               out.registerDirectory(dir);

         
         Thread thread = new Thread(out);
         thread.setDaemon(true);
         thread.start();
         return(out);
      }
      return(null);
   }
   
   /** Is this directory watcher available? Once closed for any reason, the watcher object cannot be reused. */
   public boolean isAvailable() { return(available); }
   
   /**
    * Is the specified directory registered with this watcher? This includes any directories that were dropped from the
    * active watch list because they "disappeared" from the file system. The watcher continues to monitor dropped
    * directories in case they "reappear".
    * 
    * @param dir The abstract pathname for the directory.
    * @return True if directory is registered with the watcher; False if not, or if a system dependent file path cannot 
    * be constructed rom the abstract path specified.
    */
   public boolean isRegistered(File dir)
   {
      try { return(dir != null && isRegistered(dir.toPath())); }
      catch(InvalidPathException ignored) {}
      return(false);
   }
   
   /**
    * Is the specified directory registered this watcher?
    * 
    * @param dir The directory path.
    * @return True if directory is registered with the watcher; else false.
    */
   private boolean isRegistered(Path dir) 
   {
      return(watchedMap.containsKey(dir) || droppedMap.containsKey(dir));
   }
   
   /** 
    * Get the list of file system directories currently registered with this watcher.
    * @return The directory list.
    */
   public File[] getRegisteredDirectories()
   {
      Set<Path> paths= new HashSet<>(watchedMap.keySet());
      paths.addAll(droppedMap.keySet());
      File[] out = new File[paths.size()];
      int i = 0;
      for(Path p : paths) out[i++] = p.toFile();
      return(out);
   }
   
   /**
    * Register a file system directory to be monitored for changes. The directory need not exist at the time of 
    * registration -- in this case, it is added immediately to the watcher's "dropped directories" list -- so the
    * watcher can detect if the directory "reappears" in the file system.
    * 
    * @param dir Abstract pathname for the directory.
    * @return True if successful; false if abstract pathname is invalid or identifies an existing regular file, or if
    * directory could not be registered with the underlying watch service.
    */
   public boolean registerDirectory(File dir)
   {
      if((!available) || (dir == null) || dir.isFile()) return(false);
      if(isRegistered(dir)) return(true);
      
      boolean ok = false;
      try
      {
         Path dirPath = dir.toPath();
         if(dir.isDirectory())
         {
            WatchKey key = dirPath.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            watchedMap.put(dirPath, key);
         }
         else 
            droppedMap.put(dirPath, System.currentTimeMillis());
         ok = true;
      }
      catch(Exception ignored) {}
      return(ok);
   }
   
   /**
    * Unregister a file system directory with this watcher. Any queued file system events related to the directory are
    * discarded. An {@link EventKind#UNREGISTERED} event notification is sent to the watcher's registered listeners.
    * 
    * @param dir Abstract pathname for the directory.
    */
   public void unregisterDirectory(File dir)
   {
      try 
      {
         WatchKey key = watchedMap.remove(dir.toPath());
         if(key != null)
         {
            key.cancel();
            key.pollEvents();
            notifyListeners(new Event(EventKind.UNREGISTERED, dir));
         }
         else
         {
            Long dropTime = droppedMap.remove(dir.toPath());
            if(dropTime != null)
               notifyListeners(new Event(EventKind.UNREGISTERED, dir));
         }
      } 
      catch(Exception ignored) {}
   }
   
   /**
    * Configure this watcher to report or ignore changes to a file within a watched directory if that file's name begins
    * with a period ('.').
    * @param ignore True to ignore changes in "dot" files; false to report them.
    */
   public void setIgnoreDotFiles(boolean ignore)
   {
      ignoreDotFiles = ignore;
   }
   
   /**
    * Configure this watcher to report or ignore changes to any sub-directory entry within any watched directory. If 
    * set, the watcher only reports changes to file objects in the watched directories.
    * 
    * <p><b>LIMITATION</b>: When a sub-directory is deleted, the watcher cannot conclusively determine that it WAS a
    * directory or a regular file. In this case, the watcher ASSUMES it was a directory if the file name does not 
    * contain a period. Thus, even if this flag is set, the watcher will report the deletion of any subdirectory entry
    * within a watched directory if the deleted folder's name contains at least one period.</p>
    * @param ignore True to ignore changes in sub-directory entries; false to report these changes.
    */
   public void setIgnoreSubdirectories(boolean ignore)
   {
      ignoreSubdirectories = ignore;
   }
   
   /**
    * Set the watcher's polling timeout period.
    * @param seconds The timeout in seconds, range-restricted to [1..60].
    */
   public void setPollingTimeout(int seconds)
   {
      timeoutSec = Math.max(1, Math.min(60, seconds));
   }
   
   /**
    * Stop this directory watcher. This will close the underlying watch service, terminating the daemon thread that was
    * spawned to poll that service for file system events. The watcher object will no longer be available after this
    * method is called, and cannot be restarted.
    */
   public void stop()
   {
      if(available)
      {
         try { watcher.close(); } catch(IOException ignored) {}
         available = false;
         notifyListeners(new Event(EventKind.CLOSED, null));
      }
   }
   
   /** The watcher's daemon thread procedure. */
   @Override public void run()
   {
      try
      {
         //noinspection InfiniteLoopStatement
         while(true)
         {
            // poll watch service with a finite timeout
            WatchKey key = watcher.poll(timeoutSec, TimeUnit.SECONDS);
            
            // process file system events for the returned watch key -- if any
            Event eventChain = null;
            if((key != null) && isRegistered((Path) key.watchable()))
            {
               Path dir = (Path) key.watchable();
               for(WatchEvent<?> event : key.pollEvents())
               {
                  WatchEvent.Kind<?> kind = event.kind();
                  if(kind == ENTRY_CREATE || kind == ENTRY_DELETE || kind == ENTRY_MODIFY)
                  {
                     File tgtFile = dir.resolve((Path) event.context()).toFile();
                     if(ignoreDotFiles && tgtFile.getName().startsWith("."))
                        continue;
                     if(watchedExts != null)
                     {
                        String tgtExt = Utilities.getExtension(tgtFile);
                        boolean found = false;
                        for(int i=0; (!found) && i<watchedExts.length; i++) found = watchedExts[i].equals(tgtExt);
                        if(!found) continue;
                     }
                     if(ignoreSubdirectories)
                     {
                        // there's no way to tell that a deleted path WAS a file or a directory. We ASSUME it was a
                        // directory if the directory name lacks a "dot".
                        if((kind != ENTRY_DELETE && tgtFile.isDirectory()) || 
                              (kind == ENTRY_DELETE && !tgtFile.getName().contains(".")))
                           continue;
                     }
                     EventKind ek = (kind==ENTRY_CREATE) ? EventKind.CREATE : 
                        ((kind==ENTRY_DELETE) ? EventKind.DELETE : EventKind.MODIFY);
                     if(eventChain == null) 
                        eventChain = new Event(ek, tgtFile);
                     else
                        eventChain.append(new Event(ek, tgtFile));
                  }
               }
               
               boolean valid = key.reset();
               if(!valid) 
               {
                  watchedMap.remove(dir);
                  droppedMap.put(dir, System.currentTimeMillis());
                  if(eventChain == null) 
                     eventChain = new Event(EventKind.DROPPED, dir.toFile());
                  else
                     eventChain.append(new Event(EventKind.DROPPED, dir.toFile()));
               }
            }
            
            // check to see if any watched directories have disappeared
            for(Path p : watchedMap.keySet())
            {
               File f = p.toFile();
               if(!f.isDirectory())
               {
                  key = watchedMap.remove(p);
                  key.cancel();
                  droppedMap.put(p, System.currentTimeMillis());
                  if(eventChain == null) eventChain = new Event(EventKind.DROPPED, f);
                  else eventChain.append(new Event(EventKind.DROPPED, f));
               }
            }
            
            // and check to see if any dropped directories have reappeared. Return each "restored" directory to the
            // active watch list.
            for(Path p : droppedMap.keySet())
            {
               File f = p.toFile();
               if(f.isDirectory())
               {
                  Long dropTime = droppedMap.remove(p);
                  try
                  {
                     key = p.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
                     watchedMap.put(p, key);
                     if(eventChain == null) eventChain = new Event(EventKind.RESTORED, f);
                     else eventChain.append(new Event(EventKind.RESTORED, f));
                  }
                  catch(Exception e) { // this shouldn't happen, but just in case, we put it back in dropped map
                     droppedMap.put(p, dropTime);
                  }
               }
            }
            
            // notify listeners of all file system events that occurred
            if(eventChain != null)
               notifyListeners(eventChain);
         }
         
      }
      catch(InterruptedException|ClosedWatchServiceException ignored) {}

      stop();
   }

   
   //
   // Directory change notifications and listener list
   //
   

   /** The set of listeners registered to receive events broadcast by the directory watcher. */
   private final EventListenerList listeners = new EventListenerList();

   /** Enumeration of the kinds of events broadcast by the directory watcher. */
   public enum EventKind {
      /** An entry was created within a watched directory. */ CREATE,
      /** An entry was modified within a watched directory. */ MODIFY,
      /** An entry was deleted within a watched directory. */ DELETE,
      /** A watched directory was dropped from active watch list because it disappeared from file system. */ DROPPED,
      /** A previously dropped directory has reappeared and is restored to the active watch list. */ RESTORED,
      /** A watched directory has been removed from watch list by unregistering it with the watcher. */ UNREGISTERED,
      /** The directory watcher has shut down. */ CLOSED
   }
   
   /** 
    * An event object sent in directory watcher notifications. Supports chaining of multiple events in a singly-linked
    * list fashion. 
    * */
   public static class Event 
   {
      Event(EventKind kind, File f)
      {
         this.kind = kind;
         this.target = f;
      }
      
      /** Append the specified event object to the tail of the event chain starting with this event. */
      void append(Event e)
      {
         Event parent = this;
         while(parent.next != null) parent = parent.next;
         parent.next = e;
      }
      
      /** 
       * Get the next event object in the event chain, if any.
       * @return The next event object, or null at chain's end.
       */
      public Event getNext() { return(next); }
      
      /** The event type. */
      public final EventKind kind;
      
      /** 
       * The target file or directory for the event. For {@link EventKind#CREATE}, {@link EventKind#MODIFY} or
       * {@link EventKind#DELETE}, this is the abstract pathname of the affected file. For {@link EventKind#DROPPED},
       * this is the directory that was dropped by the watcher because it is no longer "seen" on the file system. For
       * {@link EventKind#CLOSED}, this will be null.
       */
      public final File target;
      
      /** The next event in the event chain, or null at chain's end. */
      private Event next = null;
   }
   
   /** Interface implemented by objects that want to receive events broadcast by the directory watcher. */
   public interface Listener extends EventListener
   {
      /**
       * Invoked (on the Swing event dispatch thread) when an event (or chain of events) occurs within a directory
       * monitored by the directory watcher.
       * 
       * @param e The event object. The watcher may chain several events together in a single notification. Call {@link 
       * Event#getNext()} to traverse the chain (not in any particular order).
       */
      void onDirectoryWatchUpdate(Event e);
   }

   /**
    * Add the specified listener to this directory watcher's listener list.
    * @param l The listener to add.
    */
   public void addListener(Listener l) { if(l != null) listeners.add(Listener.class, l); }
   
   /**
    * Remove the specified listener from this directory watcher's listener list.
    * @param l The listener to remove.
    */
   public void removeListener(Listener l) { listeners.remove(Listener.class, l); }

   /**
    * A {@link Runnable} which sends directory watcher notifications to registered listeners.
    */
   private class Notifier implements Runnable
   {
      Notifier(Event e) { event = e; }
      
      @Override public void run()
      {
         if(event == null) return;
         
         Listener[] rcvrs = listeners.getListeners(Listener.class);
         for(Listener r : rcvrs) r.onDirectoryWatchUpdate(event);
      }

      private final Event event;
   }

   /** 
    * Send directory watcher event (or event chain) to all registered listeners. Ensure it is sent on the Swing event 
    * dispatch thread.
    * @param e The watcher event (or event chain).
    */
   private void notifyListeners(Event e)
   {
      Notifier notifier = new Notifier(e);
      if(SwingUtilities.isEventDispatchThread())
         notifier.run();
      else
         SwingUtilities.invokeLater(notifier);
   }

   /** A simple GUI application for testing {@link DirWatcher}. */
   public static void main(String[] args)
   {
      GUIUtilities.initLookAndFeel();
      LocalFontEnvironment.initialize();

      final DirWatcherTestApp appFrame = new DirWatcherTestApp();

      appFrame.addWindowListener( new WindowAdapter() {
         public void windowClosing( WindowEvent e ) { appFrame.onExit(); }
      });

      Runnable runner = new MainFrameShower( appFrame );
      SwingUtilities.invokeLater( runner );
   }
}

final class DirWatcherTestApp extends JFrame implements ActionListener, ListSelectionListener, Listener
{
   private DirWatcher watcher = null;
   private JTextArea messageArea = null;
   private final JList<File> watchList;
   private JTextField pathField = null;
   private JButton addBtn = null;
   private JButton rmvBtn = null;
   private JCheckBox ignoreDotCB = null;
   private JCheckBox ignoreSubDirCB = null;
   private NumericTextField timeoutField = null;
   
   DirWatcherTestApp() throws HeadlessException
   {
      super("Test Directory Watcher");
      
      watchList = new JList<>();
      watchList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      watchList.setVisibleRowCount(20);
      watchList.addListSelectionListener(this);
      JScrollPane listScroller = new JScrollPane(watchList);
      
      pathField = new JTextField();
      pathField.addActionListener(this);
      
      messageArea = new JTextArea();
      messageArea.setLineWrap(false);
      JScrollPane areaScroller = new JScrollPane(messageArea);
      areaScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      
      addBtn = new JButton("Add");
      addBtn.addActionListener(this);
      
      rmvBtn = new JButton("Remove");
      rmvBtn.setEnabled(false);
      rmvBtn.addActionListener(this);
      
      ignoreDotCB = new JCheckBox("Ignore dot files?");
      ignoreDotCB.addActionListener(this);
      ignoreSubDirCB = new JCheckBox("Ignore subdirectories?");
      ignoreSubDirCB.addActionListener(this);

      JLabel timeoutLabel = new JLabel("Polling timeout (sec) ");
      timeoutField = new NumericTextField(1, 60);
      timeoutField.setValue(30);
      timeoutField.addActionListener(this);
      
      JPanel contentPane = new JPanel(new SpringLayout());
      SpringLayout layout = (SpringLayout) contentPane.getLayout();
      contentPane.add(listScroller);
      contentPane.add(pathField);
      contentPane.add(areaScroller);
      contentPane.add(addBtn);
      contentPane.add(rmvBtn);
      contentPane.add(ignoreDotCB);
      contentPane.add(ignoreSubDirCB);
      contentPane.add(timeoutLabel);
      contentPane.add(timeoutField);
      
      
      layout.putConstraint(SpringLayout.WEST, pathField, 5, SpringLayout.WEST, contentPane);
      layout.putConstraint(SpringLayout.WEST, addBtn, 5, SpringLayout.EAST, pathField);
      layout.putConstraint(SpringLayout.WEST, rmvBtn, 5, SpringLayout.EAST, addBtn);
      layout.putConstraint(SpringLayout.WEST, areaScroller, 10, SpringLayout.EAST, rmvBtn);
      layout.putConstraint(SpringLayout.EAST, contentPane, 5, SpringLayout.EAST, areaScroller);

      layout.putConstraint(SpringLayout.WEST, ignoreDotCB, 5, SpringLayout.WEST, contentPane);
      layout.putConstraint(SpringLayout.WEST, ignoreSubDirCB, 5, SpringLayout.WEST, contentPane);
      layout.putConstraint(SpringLayout.WEST, timeoutLabel, 5, SpringLayout.WEST, contentPane);
      layout.putConstraint(SpringLayout.WEST, timeoutField, 0, SpringLayout.EAST, timeoutLabel);
      
      layout.putConstraint(SpringLayout.WEST, listScroller, 5, SpringLayout.WEST, contentPane);
      layout.putConstraint(SpringLayout.EAST, listScroller, 0, SpringLayout.EAST, rmvBtn);
      
      layout.putConstraint(SpringLayout.NORTH, addBtn, 5, SpringLayout.NORTH, contentPane);
      layout.putConstraint(SpringLayout.NORTH, listScroller, 10, SpringLayout.SOUTH, addBtn);
      layout.putConstraint(SpringLayout.NORTH, ignoreDotCB, 10, SpringLayout.SOUTH, listScroller);
      layout.putConstraint(SpringLayout.NORTH, ignoreSubDirCB, 5, SpringLayout.SOUTH, ignoreDotCB);
      layout.putConstraint(SpringLayout.NORTH, timeoutField, 5, SpringLayout.SOUTH, ignoreSubDirCB);
      layout.putConstraint(SpringLayout.SOUTH, contentPane, 5, SpringLayout.SOUTH, timeoutField);
      
      layout.putConstraint(SpringLayout.NORTH, areaScroller, 5, SpringLayout.NORTH, contentPane);
      layout.putConstraint(SpringLayout.SOUTH, contentPane, 5, SpringLayout.SOUTH, areaScroller);
      
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, pathField, 0, SpringLayout.VERTICAL_CENTER, addBtn);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, rmvBtn, 0, SpringLayout.VERTICAL_CENTER, addBtn);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, timeoutLabel, 0, SpringLayout.VERTICAL_CENTER, timeoutField);

      setContentPane(contentPane);
      setMinimumSize(new Dimension(1200, 800));
      
      watcher = DirWatcher.startWatcher(null, false, false, new String[] {"fyp", "fig", "dnb", "dnr", "dna", "txt"});
      if(watcher == null)
      {
         System.out.println("Sorry, failed to start watcher.");
         System.exit(0);
      }
      watcher.addListener(this);
   }
   
   void onExit()
   {
      watcher.stop();
      System.exit(0);
      
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      Object src = e.getSource();
      if(src == addBtn || src == pathField)
      {
         File f = new File(pathField.getText());
         if(watcher.isRegistered(f))
            messageArea.append(String.format("!! Already watching %s...\n", f.getAbsolutePath()));
         else
         {
            boolean ok = watcher.registerDirectory(f);
            if(ok)
            {
               messageArea.append(String.format("Started watching %s...\n", f.getAbsolutePath()));
               watchList.setListData(watcher.getRegisteredDirectories());
               watchList.setSelectedValue(f, true);
            }
            else
               messageArea.append(String.format("!! Unable to watch %s", f.getAbsolutePath()));
         }
      }
      else if(src == rmvBtn)
      {
         File f = watchList.getSelectedValue();
         if(watcher.isRegistered(f))
         {
            messageArea.append(String.format("Stopped watching %s...\n", f.getAbsolutePath()));
            watcher.unregisterDirectory(f);
            watchList.setListData(watcher.getRegisteredDirectories());
         }
      }
      else if(src == ignoreDotCB)
         watcher.setIgnoreDotFiles(ignoreDotCB.isSelected());
      else if(src == ignoreSubDirCB)
         watcher.setIgnoreSubdirectories(ignoreSubDirCB.isSelected());
      else if(src == timeoutField)
      {
         int timeOut = timeoutField.getValue().intValue();
         watcher.setPollingTimeout(timeOut);
         messageArea.append(String.format("Set polling timeout to %d seconds.\n", timeOut));
      }
   }

   @Override public void valueChanged(ListSelectionEvent e)
   {
      if((e.getSource() == watchList) && !e.getValueIsAdjusting())
      {
         File f = watchList.getSelectedValue();
         pathField.setText((f != null) ? f.getAbsolutePath() : "");
         rmvBtn.setEnabled(f != null);
      }
   }

   @Override public void onDirectoryWatchUpdate(Event e)
   {
      Event nextEvt = e;
      while(nextEvt != null)
      {
         switch(nextEvt.kind) {
         case CREATE:
            messageArea.append(String.format("==> CREATED: %s\n", nextEvt.target));
            break;
         case MODIFY:
            messageArea.append(String.format("==> MODIFIED: %s\n", nextEvt.target));
            break;
         case DELETE:
            messageArea.append(String.format("==> DELETED: %s\n", nextEvt.target));
            break;
         case DROPPED:
            messageArea.append(String.format("==> DROPPED: %s\n", nextEvt.target));
            break;
         case RESTORED:
            messageArea.append(String.format("==> RESTORED: %s\n", nextEvt.target));
            break;
         case UNREGISTERED:
            messageArea.append(String.format("==> UNREGISTERED: %s\n", nextEvt.target));
            break;
         case CLOSED:
            messageArea.append("!! The directory watcher has stopped.\n");
            break;
         }
         
         nextEvt = nextEvt.getNext();
      }
   }
}


