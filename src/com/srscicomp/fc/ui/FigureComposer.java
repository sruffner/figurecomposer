package com.srscicomp.fc.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.srscicomp.common.ui.GUIUtilities;
import com.srscicomp.common.ui.LocalFontEnvironment;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.uibase.FCAboutDlg;
import com.srscicomp.fc.uibase.FCChooser;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.FCWorkspace;
import com.srscicomp.fc.uibase.PreferencesDlg;
import com.srscicomp.fc.uibase.PrinterSupport;
import com.srscicomp.fc.uibase.FCWorkspace.EventID;


/**
 * TODO: Figure Composer TASK LIST
 * This is where I keep notes on the "big picture" about the current state of FigureComposer development.
 * <p>
 * CHANGES SINCE 5.4.5 release...
 * <p>
 * FUTURE WORK?:
 * <p>
 * ==> Can I get a hold of a figure containing a Matlab box plot to see if I can do the conversion to fyp? Note that
 * the statistics toolbox is required to generate box plots in Matlab...
 * <p>
 * ===================================================
 * <p>
 * The application frame window for the stand-alone <i>Figure Composer</i> application, a tool for constructing 
 * publication-quality figures displaying scientific data in a variety of formats.
 * 
 * <p><code>FigureComposer</code> is little more than a {@link JFrame} container for the {@link FigComposer} component, 
 * in which most of the application functionality is implemented.</p>
 * 
 * <h3>Desktop Integration Issues</h3>
 * <p>A common and popular mechanism for opening a file is to "double-click" on its representation in the host OS, which
 * requires registering a file type and associating it with an application. But this mechanism works quite differently 
 * under different platforms, and Java does not provide platform-independent support for tying into this mechanism. 
 * Since <i>Figure Composer</i> and <i>DataNav</i> primarily target Mac OSX and Windows, we have modified both apps to 
 * support the "double-click-file-to-open" feature for FypML figure files on these two platforms. The feature is not 
 * available on Linux.
 * <ul>
 *   <li><i>On Mac OSX</i>. The <code>.fyp</code> file type and association is defined in the application bundle's 
 *   <code>Info.plist</code>, in the <code>CFBundleDocumentTypes</code> and <code>UTExportedTypeDeclarations</code>
 *   keys. OS X sends "application events" when the user double-clicks a file to open it, and these are received via
 *   through {@link java.awt.Desktop}. Note that the same mechanism works
 *   for two similar use cases: double-clicking a .fyp file when the application is not running and when it is; both 
 *   cases are supported -- but only for a single file (Mac OSX supports opening multiple files at once in this manner).
 *   In the latter case, the application is de-iconified and brought to the foreground as needed before opening the 
 *   file.</li>
 *   
 *   <li><i>On Windows</i>. The file-type association was much more difficult to implement on Windows, because there is
 *   no way to create a file association with a Java desktop app, and because it requires making changes in the Windows
 *   registry. We chose to wrap the application as a WinRun4J-based .EXE, which is modified to include the application
 *   icon and splash screen. We then developed a simple installer using the free Inno Setup compiler that installed
 *   the wrapped executable and made the necessary changes to the registry. When a .fyp file is clicked under Windows,
 *   the wrapped executable is invoked with the file's full path as an argument, and the executable hands this off to 
 *   the Java application itself when it starts the JVM: <code>javaws -jar {appname}.jar "filepath"</code>. In addition,
 *   the wrapped executable was configured to ensure that only one instance runs at a time. When an already running 
 *   instance is detected, the new instance terminates immediately after sending the first instance a Dynamic Data 
 *   Exchange (DDE) message -- ultimately resulting in a call to the static function {@link #activate(String)}. This 
 *   function ensures the frame window is activated and, if the argument includes a valid path to an FypML file (which 
 *   will be the case when the user double-clicks such a file), the specified file is opened in a new tab in the figure
 *   composer view controller, which will be brought to the front.</li>
 * </ul> 
 * </p>
 * 
 * @author sruffner
 */
public class FigureComposer extends JFrame implements WindowListener, FCWorkspace.Listener
{

   /**
    * This helper class serves as the "launcher" for the Figure Composer application. It performs various initialization
    * tasks, then creates and shows the application frame window. It also displays a splash screen and handles updates
    * to a progress bar displayed over that splash screen. As recommended for all Swing applications, this {@link 
    * Runnable} should be invoked on the Swing event dispatch thread; otherwise, race conditions may occur which cause
    * the application to terminate ungracefully!. Periodic calls to {@link #updateProgress(int, String)} from the
    * start-up code ensure that the progress bar gets updated as the application is initialized and the GUI prepared.
    * 
    * @author sruffner
    */
   private static class Launcher implements Runnable
   {
      public void run() 
      {
         updateProgress(0, null);
         GUIUtilities.initLookAndFeel();
         LocalFontEnvironment.initialize();
         
         // as of FC 5.2.0, Java 8 is required, but we allow it to run on JRE 9 or better.
         if(Utilities.getJavaMajorVersion() < 8)
         {
            JOptionPane.showMessageDialog(null, 
                  "FigureComposer requires a Java 8 runtime environment or better.", 
                  "Launch failed", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
            return;
         }
        
         // load workspace and printer inits. We deliberately sleep 300ms afterward.
         updateProgress(10, "Loading workspace...");
         FCWorkspace ws = FCWorkspace.getInstance();
         if(ws == null)
         {
            JOptionPane.showMessageDialog(null, "Failed to load Figure Composer workspace; cannot continue.", 
                  "Launch failed", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
            return;
         }
         PrinterSupport.getInstance();
         try { Thread.sleep(300); } catch(InterruptedException ignored) {}
         
         // construct and show the UI
         updateProgress(15, "Preparing application...");

         // the main application frame window opened by this launcher.
         FigureComposer appFrame = new FigureComposer();
         updateProgress(100, "Finishing...");
         
         // under Windows only, the application may be invoked with a single argument specifying the full path to a figure
         // file that should be opened. See comments in file header. Here we save the file path (if it exists), so that we 
         // can open it once the frame window is up and initialized. The mechanism for doing this in Mac OSX is different; 
         // and file associations are not supported in Linux at this time.
         if(mainArgs != null && mainArgs.length > 0 && Utilities.isWindows())
         {
            File f = new File(mainArgs[0]);
            if(f.isFile()) appFrame.figFileToOpenOnAppStart = f;
         }
         registerAppForWindowsActivation(appFrame);

         appFrame.pack();
         appFrame.updateMinimumSize(true);
         
         Rectangle bounds = ws.getFrameBounds();
         Dimension sz = Toolkit.getDefaultToolkit().getScreenSize();
         if(bounds.x < 0) bounds.x = 0;
         if(bounds.y < 0) bounds.y = 0;
         if(bounds.width > sz.width) bounds.width = sz.width;
         if(bounds.height > sz.height) bounds.height = sz.height;
         
         int state = NORMAL;
         if(bounds.width == sz.width) state |= MAXIMIZED_HORIZ;
         if(bounds.height == sz.height) state |= MAXIMIZED_VERT;
         
         appFrame.setBounds(bounds);
         appFrame.setExtendedState(state);

         appFrame.validate();
         
         appFrame.setVisible(true);
         
         // we're done. Nullify launcher object so that it's GC'd
         appLauncher = null;
      }

      /**
       * If the application splash screen is still visible, update the progress bar drawn over it to indicate progress
       * during application start-up.
       * @param prg Progress as percent complete; range-restricted to [0..100].
       * @param msg An optional message string. Restricted to 30 characters is length.
       */
      private void updateProgress(int prg, String msg)
      {
         int progress = Math.max(0, Math.min(100, prg));
         if(splash != null && splash.isVisible())
         {
            Dimension d = splash.getSize();
            Graphics2D g2d = splash.createGraphics();
            g2d.setColor(new Color(255, 255, 255, 128));
            g2d.fillRect(30, d.height-32, d.width-60, 16);
            g2d.setColor(new Color(176, 196, 222));
            int w = progress * (d.width - 64) / 100;
            g2d.fillRect(32, d.height-31, w, 14);
            if(msg != null && !msg.isEmpty())
            {
               if(msg.length() > 30) msg = msg.substring(0, 30) + "...";
               g2d.setColor(Color.BLACK);
               g2d.drawString(msg, 45, d.height-20);
            }
            splash.update();
         }
      }

      Launcher(String[] args) 
      { 
         mainArgs = args; 
         splash = SplashScreen.getSplashScreen();
      }
      
      /** Arguments passed into main() when application launched. */
      private final String[] mainArgs;
      /** Application's splash screen. */
      private final SplashScreen splash;

   }

   /** The application launcher. Active and non-null only during start-up. */
   private static Launcher appLauncher = null;
   
   /**
    * Handler called from start-up code to update progress bar on the application splash screen.
    * <p>This is a HACK. However, prior experience has shown that it is important to create and show the application
    * GUI on the Swing event dispatch thread. To occasionally update the splash screen, I have to call this method from
    * key points in the start-up code, which were determined by trial and error.</p>
    * 
    * @param prg Progress as percent complete; range-restricted to [0..100].
    * @param msg An optional message string. Restricted to 30 characters is length.
    */
   static void updateStartupProgress(int prg, String msg)
   {
      if(appLauncher != null) appLauncher.updateProgress(prg, msg);
   }
   
   /** 
    * The application entry point. 
    * @param args Command-line arguments (ignored).
    */
   public static void main(final String[] args)
   {
      appLauncher = new Launcher(args);
      SwingUtilities.invokeLater( appLauncher );
   }


   //
   // Support for opening a file in the currently running instance of the application under Windows
   //

   /** 
    * If non-null, this is the current running instance of <i>FigureComposer</i>. The app is packaged to run as a single
    * instance application under Windows, and DDE (dynamic data exchange) is used to activate the running instance when 
    * a user double-clicks a file. Applicable only when OS platform is Windows.
    */
   private static FigureComposer currentAppForWindowsActivation = null;
   
   /**
    * Register or unregister a <i>FigureComposer</i> application object as the global single-instance application to 
    * receive Windows activation events via DDE. Applicable only when OS platform is Windows. The application object 
    * MUST implement the following static methods:
    * <ul>
    *    <li><code>public static void execute(String)</code>. This method is not used for single-instance activation,
    *    but it must be implemented. It can be a no-op.</li>
    *    <li><code>public static void activate(String)</code>. This method will be invoked to notify the running 
    *    instance of the application that the user has requested a FypML file be opened within it. The argument should
    *    contain the full pathname of the file to open.</li>
    * </ul>
    * @param app The <i>FigureComposer</i> application frame window to receive activation events. 
    */
   private static void registerAppForWindowsActivation(FigureComposer app)
   {
      if(Utilities.isWindows()) currentAppForWindowsActivation = app;
   }
   
   /**
    * Activate the application, bring it to the foreground, and open the file specified.
    * @param cmd This argument should contain the full pathname of the file to be opened.
    */
   public static void activate(String cmd)
   {
      if(currentAppForWindowsActivation != null)
      {
         if(cmd == null || cmd.trim().isEmpty())
         {
            // activate sent without a file path to open. Merely activate the frame window.
            currentAppForWindowsActivation.setState(JFrame.NORMAL);
            currentAppForWindowsActivation.toFront();
            currentAppForWindowsActivation.requestFocusInWindow();
            return;
         }
         
         // the argument should be the path of the file to open. It may be surrounded in double-quotes, so strip
         // these off.
         if(cmd.startsWith("\"") && cmd.endsWith("\"")) cmd = cmd.substring(1, cmd.length()-1);

         File f = new File(cmd);
         if(f.isFile())
         {
            List<File> files = new ArrayList<>();
            files.add(f);
            currentAppForWindowsActivation.doFileOpen(files);
         }
      }
   }
   
   /**
    * Execute the specified command. This method must be implemented as part of the Windows DDE mechanism. All it does
    * is pass the specified argument on to {@link #activate(String)}.
    * @param cmd The command line.
    */
   public static void execute(String cmd) { activate(cmd); }
   

   /**
    * Constructor for the <i>FigureComposer</i> application frame window, which also serves as application controller. 
    * The entire user interface is created here, but it is not displayed. The frame window can be created from the 
    * main() thread, but it should be shown on the event dispatch thread.
    * @throws HeadlessException if there is no display, keyboard or mouse
    */
   public FigureComposer() throws HeadlessException
   {
      super(FCWorkspace.getApplicationTitle());
      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      addWindowListener(this);
      
      FCWorkspace.getInstance().addWorkspaceListener(this);

      if(FCIcons.FC_APP16 != null && FCIcons.FC_APP32 != null && FCIcons.FC_APP48 != null && FCIcons.FC_APP128 != null)
      {
         List<Image> appIcons = new ArrayList<>();
         appIcons.add(FCIcons.FC_APP16.getImage());
         appIcons.add(FCIcons.FC_APP32.getImage());
         appIcons.add(FCIcons.FC_APP48.getImage());
         appIcons.add(FCIcons.FC_APP128.getImage());
         setIconImages(appIcons);
      }

      updateStartupProgress(20, "Preparing application...");

      // initialize custom file chooser singleton providing workspace access and file browsing
      FCChooser.getInstance();
      
      // actions handled by this frame window object directly
      appActions = new AppAction[NAPPACTIONS];
      appActions[ABOUT] = new AppAction(ABOUT, "About " + FCWorkspace.getApplicationTitle(), null, KeyEvent.VK_B);
      appActions[PREFS] = new AppAction(PREFS, "Preferences...", null, KeyEvent.VK_R);
      appActions[EXIT] = new AppAction(EXIT, "Exit", null, KeyEvent.VK_X);

      // hook into Apple application menu if we're running on Mac OS X
      createMacApplicationMenuHooks();
      
      updateStartupProgress(25, "Constructing UI...");

      // create the GUI: Everything is in the figure composer view controller, which provides the menu bar as well.
      fcView = new FigComposer(appActions);
      setContentPane(fcView);
      setJMenuBar(fcView.getMenuBar());
      
      // HACK FIX: the figure composer view controller is the one-and-only view controller, so it's always visible. It 
      // never gets component-shown events as a result, which breaks the mechanism by which it responds to certain hot
      // keys not tied into the menu infrastructure. So we register it here
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(fcView);
   }

   public Action getAboutAction() { return(appActions[ABOUT]); }
   public Action getPrefsAction() { return(appActions[PREFS]); }
   public Action getExitAction() { return(appActions[EXIT]); }
   

   @Override public void onWorkspaceUpdate(EventID id, File fp1, File fp2)
   {
      if(id == EventID.SCREENRES)
      {
         fcView.onScreenResolutionChange();
         validate();
      }
   }

   /** Override invokes standard exit routine -- which may abort application exit. */
   @Override public void windowClosing(WindowEvent e) { doExit(); }

   /**
    * If the user starts the application by double-clicking on a figure file or some other "Open With" mechanism, the
    * figure file's path is supplied on the command line. In this particular scenario, we try to open the specified 
    * figure file within the composer view controller in the figure perspective.
    */
   @Override public void windowOpened(WindowEvent e) 
   {
      fcView.onApplicationWindowOpened(figFileToOpenOnAppStart);
      
      hasFrameOpened = true;
      figFileToOpenOnAppStart = null;
   }
   
   @Override public void windowActivated(WindowEvent e) {}
   @Override public void windowClosed(WindowEvent e) {}
   @Override public void windowDeactivated(WindowEvent e) {}
   @Override public void windowDeiconified(WindowEvent e) {}
   @Override public void windowIconified(WindowEvent e) {}

   
   //
   // OSXCompatibleApp: About, Preferences, and Exit handlers for Mac OS X (and for all platforms)
   //
   
   /**
    * Exit the application.
    * 
    * <p>Before application exit, check if there are any unsaved changes among the figures currently open. If so, give 
    * the user an opportunity to save any modified figures there. If the user cancels that operation, application exit 
    * is aborted. Otherwise, this method saves the current frame window bounds in the user's workspace settings, and 
    * calls {@link System#exit(int) System.exit(0)}.
    */
   public void doExit()
   {
      if(!fcView.onVetoableApplicationExit())
         return;
      fcView.onApplicationWillExit();
      
      FCWorkspace ws = FCWorkspace.getInstance();
      if(ws != null) ws.setFrameBounds(true, getBounds());
      
      System.exit(0);
   }

   /**
    * Displays a simple modal "about" dialog with version number and some other application information. Dialog is
    * disposed upon closing.
    */
   public void doAbout()
   {
      FCAboutDlg dlg = new FCAboutDlg(this);
      dlg.setLocationRelativeTo(this);
      dlg.setVisible(true);
   }

   /**
    * Displays modal preference dialog used to set screen DPI and a variety of default values for various properties
    * of a DataNav figure.
    */
   public void doPreferences()
   { 
      PreferencesDlg.editPreferences(this); 

      // user may have changed the screen DPI; update the minimum frame window size just in case
      updateMinimumSize(false);
   }

   /**
    * <i>FigureComposer</i> supports opening a single FypML figure definition file in response to a file-open app event
    * from OS X. If the file list contains more than one file, only the first is opened. 
    */
   public void doFileOpen(List<File> files)
   {
      if(files == null || files.isEmpty()) return;
      File f = files.get(0);
      if(f == null || !f.isFile()) return;
      
      // if the frame window has not opened yet, then the application was probably started by double-clicking a file.
      // in this situation, we delay opening the file until after the frame window has come up, so that we can put the
      // app in the state it was in when it last exited. Otherwise, go ahead and open the figure file.
      if(!hasFrameOpened)
         figFileToOpenOnAppStart = f;
      else
      {
         // if app is iconified, restore it before opening the file requested
         if((getState() & JFrame.ICONIFIED) == JFrame.ICONIFIED) setState(JFrame.NORMAL);
         toFront();
         requestFocusInWindow();
         
         fcView.openFigure(f);
      }
   }

   
   /** Flag set once the application frame window has been opened after application start. */
   private boolean hasFrameOpened = false;
   /** 
    * The application may be started by double-clicking a figure file. In this use case, the system will send the
    * file-open application event BEFORE the frame window has opened. Since we want to finish initializing the frame
    * window before responding to that event, we preserve the path of the file that is to be opened. It will be handled
    * as part of the tasks done in {@link #windowOpened(WindowEvent)}.
    */
   private File figFileToOpenOnAppStart = null;

   /** The figure composer component -- which serves as the entire content pane for the app frame window. */
   private FigComposer fcView = null;
   
   /**
    * Compute and set minimum size of frame window. The frame window's minimum size matches that of its one-and-only
    * view controller -- the figure composer component, plus the frame window's insets (title bar, etc) and menu bar.
    * @param initial True when calling during initial frame window construction. In this case, the minimum size is
    * set; otherwise, the method first checks to see if the current minimum size is different from the size just
    * computed.
    */
   private void updateMinimumSize(boolean initial)
   {
      Dimension minD = fcView.getMinimumSize();
      Insets insets = getInsets();
      minD.width += insets.left + insets.right;
      minD.height += insets.top + insets.bottom;
      Dimension mbSz = getJMenuBar().getMinimumSize();
      if(mbSz != null) minD.height += mbSz.height;
      if(initial || !minD.equals(getMinimumSize()))
         setMinimumSize(minD);
   }

   /** 
    * If host is Mac OSX, install standard items that should appear in the Mac application menu. This menu (part of the
    * "Apple" menu) includes common application commands "About", "Preferences", and "Quit". Mac UI guidelines suggest 
    * using Apple's Java Extensions to hook these commands to the appropriate handlers in the application. This is taken
    * care of by OSXAdapter. Here we call the static method in OSXAdapter that sets up the hooks, using class reflection
    * techniques.
    */
   private void createMacApplicationMenuHooks()
   {
      if(!Utilities.isMacOS()) return;

      try
      {
         Desktop desktop = Desktop.getDesktop();
         desktop.setAboutHandler(e -> doAbout());
         desktop.setPreferencesHandler(e -> doPreferences());
         desktop.setQuitHandler((e, response) -> {
            doExit();
            response.cancelQuit();   // We only get here if user cancelled exit.
         });
         desktop.setOpenFileHandler(e -> doFileOpen(e.getFiles()));
      }
      catch(Exception e)
      {
         JOptionPane.showMessageDialog(FigureComposer.this,
               "Exception (" + e.getMessage() + ") occurred while registering macOS Application menu " +
                     "handlers.\n" + "Application menu handling and double-click file open feature disabled.",
               "MacOS Desktop Integration Failed", JOptionPane.WARNING_MESSAGE);
      }
   }


   /** Action type: Raise typical "About" dialog for the application. */
   private final static int ABOUT = 0;
   /** Action type: Edit user application preferences. */
   private final static int PREFS = 1;
   /** Action type: Exit the application. */
   private final static int EXIT = 2;
   /** Number of different action types. */
   private final static int NAPPACTIONS = EXIT + 1;

   /** The different command-type actions supported directly by <code>FigureComposer</code>. */
   private AppAction[] appActions = null;

   /**
    * This helper class encapsulates several application-level actions handled directly by <code>FigureComposer</code>:
    * displaying the application's "About" and "Preferences" dialogs, or exiting the application.
    * @author sruffner
    */
   private class AppAction extends AbstractAction
   {
      /** This action type code. */
      private final int type;
      
     /**
       * Construct one of the application-level actions exposed.
       * @param type The action's type.
       * @param name The name of the action. It is also installed as the tooltip. If installing the action on an iconic
       * button, be sure to hide the action text!
       * @param accel The action's accelerator key stroke. If <code>null</code>, it will have no shortcut key. 
       * @param menuKey The action's mnemonic key. Must be a defined constant in <code>KeyEvent</code>, or -1 to 
       * indicate that the action has no mnemonic key.
       */
      AppAction(int type, String name, KeyStroke accel, int menuKey)
      {
         assert(type >= 0 && type < NAPPACTIONS);
         this.type = type;
         putValue(NAME, name);
         putValue(SHORT_DESCRIPTION, name);
         putValue(ACTION_COMMAND_KEY, name);
         if(accel != null) putValue(ACCELERATOR_KEY, accel);
         if(menuKey != -1) putValue(MNEMONIC_KEY, menuKey);
      }
      
      public void actionPerformed(ActionEvent e)
      {
         switch(type)
         {
            case ABOUT: doAbout(); break;
            case PREFS: doPreferences(); break;
            case EXIT: doExit(); break;
            default: break;
         }
      }
   }
}
