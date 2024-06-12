package com.srscicomp.fc.uibase;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EventListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DropMode;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;

import com.srscicomp.common.g2dviewer.Graph2DViewer;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.fig.FGModelSchemaConverter;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FigureNode;
import com.srscicomp.fc.matlab.MatlabFigureImporter;
import com.srscicomp.fc.ui.FigComposer;

/**
 * The workspace figures browser.
 * 
 * <p><code>WSFigBrowser</code> is a compact component that lets the user browse all known directories in his workspace
 * that contain <i>DataNav</i> figure sources: either FypML figure definition files (extension .fyp), or Matlab FIG
 * files (which can be imported to FypML format). It queries the singleton workspace object, {@link FCWorkspace}, to 
 * retrieve the list of all folders on the host file system that are known to contain FypML or Matlab FIG files, as well
 * as the list of figure files in each such folder.</p>
 * 
 * <p>The browser's UI consists of two list controls and a preview canvas, arranged in a vertical column. A nested 
 * split-pane configuration allows users to apportion the amount of vertical space available to the canvas, the figure
 * notes area, and the panel containing the two list controls. The topmost list displays all figure-containing folders 
 * on the system, while the second list displays all figure files in the currently selected folder. The preview canvas 
 * displays the figure stored in the FypML file currently selected in the second list. When a Matlab FIG file is 
 * selected, the canvas displays the FypML figure that reproduces -- to the extent possible -- the Matlab figure defined
 * in that file. A combo box is included for specifying the order in which folders and files are sorted ({@link 
 * FCWorkspace.SortOrder SortOrder}), and a "close" button to hide the browser. Two small buttons under the figure file 
 * list -- on the same row as the sort-order combo box -- let the user copy or delete the figure file(s) currently 
 * selected in the file list. They are enabled only when the figure file list has the keyboard focus. Finally, the user 
 * can rename a file in the file list by holding the mouse down over the target filename for at least a half-second. 
 * Upon release, an "in-place" text field appear over the list item, allowing the user to enter a new filename. The new 
 * filename must have the same extension as the original, and it must not duplicate the name of another file in the 
 * parent directory.</p>
 * 
 * <p>The figure file list allows the user to select multiple files, which can then be duplicated or deleted via the
 * pushbuttons mentioned above. Alternatively, drag the selection and drop it over a different folder in the figure 
 * folder list to move or copy the files selected to that target folder. The "move" action is the default; press the 
 * <b>Ctrl</b> key to copy the selection to the target folder instead. After completing the file move or copy operation,
 * the target folder becomes the current folder, and the moved/copied files are initially selected in the figure file
 * list.</p>
 * 
 * <p><code>WSFigBrowser</code> is intended to serve as a sub-component anchored to the right-hand side of the figure
 * composer perspective, {@link FigComposer}. The panel's background is custom-painted with a title and the close button
 * along the bottom so that the browser "looks right" in this role. Furthermore, it implements a listener mechanism so
 * so that the <code>FigComposer</code> may be notified when the user hides the browser via the embedded "close" button,
 * or double-clicks a file name in the figure file list. In the latter case, the figure composer responds by opening the
 * double-clicked file. Finally, whenever the user loads a different figure into the figure composer, the browser can
 * be updated to reflect the contents of the corresponding folder (assuming the selected figure is associated with an
 * existing source file) -- see {@link #selectFigureFolder(File)}.</p>
 * 
 * <p>[NOTE (08jan2017): <b>WSFigBrowser</b> was updated for Java 8 code compatibility. This involved fixing compiler
 * warnings about raw types. As of JDK 7, a number of JDK classes were made generic, such as JList. Also the JList
 * function getSelectedValues() was deprecated in favor of the type-specific getSelectedValuesList().]</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
public class WSFigBrowser extends JPanel 
      implements MouseListener, ActionListener, ListSelectionListener, FCWorkspace.Listener, FocusListener
{
   /** Construct the workspace figures browser. */
   public WSFigBrowser()
   {
      workspace = FCWorkspace.getInstance();
      construct();
      reload(true);
      workspace.addWorkspaceListener(this);
      
      // visibility of browser determined by workspace setting
      if(!workspace.getWSFigBrowserOn()) setVisible(false);
   }

   /**
    * Perform any initializations at application start-up (after application window opened) or exit (just before closing
    * the application window).
    * @param isExit True if application will exit; else, application is starting up.
    */
   public void onApplicationStartOrExit(boolean isExit)
   {
      if(isExit)
      {
         // save visibility state and split pane divider locations in workspace settings file
         workspace.setWSFigBrowserOn(isVisible());
         workspace.setWSFigBrowserDivLocs(splitMain.getDividerLocation(), 
               noteHidden ? noteDivLoc : splitPreview.getDividerLocation());
      }
      else
      {
         // restore divider locations from workspace settings. We must queue the change to the nested split pane until
         // after the parent split pane has been updated!!
         final int[] locs = workspace.getWSFigBrowserDivLocs();
         splitMain.setDividerLocation(locs[0]);
         if(locs[1] > -1)
         {
            noteDivLoc = locs[1];
            if(!noteHidden)
            {
               SwingUtilities.invokeLater(new Runnable() {
                  public void run() { splitPreview.setDividerLocation(noteDivLoc); }
              });
            }
         }
         
         // since browser starts reloading itself at construction time, the figure initially displayed on canvas may 
         // not be rendered correctly -- so re-render now
         if(selectedFGM != null)
         {
            previewer.setModel(null);
            previewer.setModel(selectedFGM);
            noteArea.setText(((FigureNode) selectedFGM.getRoot()).getNote().trim());
            
            // update visible status of figure note text area if necessary
            showHideNoteText();
         }
         
      }
   }
   
   /**
    * Make the specified workspace directory the current selection in the workspace figure browser's folder list, and
    * update the file list and preview canvas accordingly.
    * @param dir The directory to be selected. If null, not a directory, or not found in the current folder list, then
    * no action is taken.
    */
   public void selectFigureFolder(File dir)
   {
      if(dir == null || !dir.isDirectory()) return;
      figFolderList.setSelectedValue(dir, true);
   }

   /** 
    * Register a listener that will receive any events broadcast by this workspace figures browser. This is the 
    * mechanism by which the browser sends a notification when the user selects a figure file to be opened, or when the
    * user closes the browser via its "close" button.
    * @param l The listener to be added.
    */
   public void addListener(Listener l) { listeners.add(Listener.class, l); }
   
   /**
    * Unregister a listener so that it will no longer receive events broadcast by this workspace figures browser.
    * @param l The listener to be removed.
    */
   public void removeListener(Listener l) { listeners.remove(Listener.class, l); }
   
   /** Overridden to enforce a fixed width for the component. */
   @Override public Dimension getPreferredSize()
   {
      Dimension d = super.getPreferredSize();
      d.width = FIXEDWIDTH;
      return(d);
   }
   /** Overridden to enforce a fixed width for the component. */
   @Override public Dimension getMaximumSize()
   {
      Dimension d = super.getMaximumSize();
      d.width = FIXEDWIDTH;
      return(d);
   }
   /** Overridden to enforce a fixed width for the component. */
   @Override public Dimension getMinimumSize()
   {
      Dimension d = super.getMinimumSize();
      d.width = FIXEDWIDTH;
      return(d);
   }

   /** The fixed width of browser in pixels (height is unrestricted). */
   private final static int FIXEDWIDTH = 300;
   
   
   /** Handler for user selections in the figure folder or file lists. */
   @Override
   public void valueChanged(ListSelectionEvent e)
   {
      if(isReloading || e.getValueIsAdjusting()) return;
      
      isReloading = true;
      try
      {
         Object src = e.getSource();
         if(src == figFolderList)
         {
            // include any workspace directories containing either FypML or Matlab FIG files
            File dir = (File) figFolderList.getSelectedValue();
            List<File> files = workspace.getWorkspaceFiles(dir, FCWorkspace.SrcType.FYP);
            List<File> figFiles = workspace.getWorkspaceFiles(dir, FCWorkspace.SrcType.MATFIG);
            for(File f : files) figFiles.remove(f);
            files.addAll(figFiles);
            if(workspace.getFileSortOrder() != FCWorkspace.SortOrder.NONE) Collections.sort(files, fileSorter);
            
            figFileList.setListData(files.toArray(new File[0]));
            if(files.size() > 0) figFileList.setSelectedIndex(0);
            updatePreviewedFile();
         }
         else if(src == figFileList)
            updatePreviewedFile();
      }
      finally { isReloading = false; }
   }

   /**
    * The "delete file" and "copy file" buttons are enabled whenever the figure file list gets the keyboard focus, and
    * an item is selected in that list.
    */
   @Override public void focusGained(FocusEvent e) { focusLost(e); }

   /**
    * The "delete file" and "copy file" buttons are disabled whenever the figure file list loses the keyboard focus.
    */
   @Override public void focusLost(FocusEvent e)
   {
      boolean ena = figFileList.isFocusOwner() && figFileList.getSelectedIndex() >= 0;
      deleteFileBtn.setEnabled(ena);
      copyFileBtn.setEnabled(ena);
   }
   
   /**
    * Handler responds to sundry widgets inside the workspace figures browser:
    * <ul>
    * <li>Updates the current sort order IAW user's selection in the relevant combo box, then reloads the browser's
    * contents accordingly.</li>
    * <li>Responds to the "close" button by hiding the browser altogether.</li>
    * <li>Responds to the "delete file" and "copy file" buttons by performing the relevant operation.</li>
    * <li>When the user hits "Return" after entering text in the "file-rename" pop-up editor, the selected file in the
    * figure file list is renamed accordingly -- unless the new filename is rejected.</li>
    * </ul>
    * Handler updates the current sort order IAW user's selection in from the relevant combo box, then reloads the
    * browser contents accordingly. Also responds to the "close" button, hiding the browser altogether.
    */
   @Override public void actionPerformed(ActionEvent e)
   {
      if(isReloading) return;
      
      Object src = e.getSource();
      if(src == sortOrderCB)
      {
         workspace.setFileSortOrder((FCWorkspace.SortOrder) sortOrderCB.getSelectedItem());
         reload(false);
      }
      else if(src == closeBtn)
      {
         setVisible(false);
         notifyListeners(null);
      }
      else if(src == copyFileBtn)
         copySelectedFigureFiles();
      else if(src == deleteFileBtn)
         deleteSelectedFigureFiles();
      else if(src == renameTF)
         finishRenamingSelectedFigureFile(renameTF.getText());
   }

   /**
    * If user double-clicks on an item in the figure file list or SINGLE-clicks on the figure preview canvas, the user's
    * intent is to open the relevant figure file. Any registered listeners are notified accordingly.
    */
   public void mouseClicked(MouseEvent e)
   {
      Object src = e.getSource();
      if((src == previewer) || (src == figFileList && e.getClickCount() == 2))
      {
         File f = (File) figFileList.getSelectedValue();
         if(f != null && f.isFile())
         {
            FCFileType ft = FCFileType.getFileType(f);
            if(ft == FCFileType.FYP || ft == FCFileType.FIG)
               notifyListeners(f);
         }
      }
   }
   public void mouseEntered(MouseEvent e) {}
   public void mouseExited(MouseEvent e) {}
   
   /** Initiate detection of the "file-rename" gesture in figure file list. See {@link #mouseReleased(MouseEvent)}. */
   public void mousePressed(MouseEvent e) 
   {
      if((e.getSource() != figFileList) || !SwingUtilities.isLeftMouseButton(e)) return;

      itemUnderMouse = -1;
      Point p = e.getPoint();
      int idx = figFileList.locationToIndex(p);
      if(idx > -1 && (figFileList.getSelectedIndex() == idx) && figFileList.getCellBounds(idx, idx).contains(p))
      {
         tMousePressed = e.getWhen();
         itemUnderMouse = idx;
      }
   }
   
   /** 
    * Detect a "file-rename" gesture in the figure file list. If the user presses the primary mouse button over an 
    * actual row in the list and then releases it over the same row at least a half-second later, then a pop-up in-place
    * editor is raised to let the user change the name of the selected figure file.
    */
   public void mouseReleased(MouseEvent e) 
   {
      if(e.getSource() != figFileList) return;
      
      boolean ok = itemUnderMouse > -1;
      if(ok) 
      {
         long tElapsed = e.getWhen() - tMousePressed;
         ok = (tElapsed >= 500L);
      }
      if(ok)
      {
         Point p = e.getPoint();
         int idx = figFileList.locationToIndex(p);
         ok = (idx == itemUnderMouse) && figFileList.getCellBounds(idx, idx).contains(p);
      }
      if(ok) startRenamingSelectedFigureFile();
      else itemUnderMouse = -1;
   }
   
   /** Time at which last mouse press occurred within figure file list widget. Used to detect "rename file" gesture. */
   private long tMousePressed = 0L;
   /** Index of figure file list item under mouse on last mouse press. Used to detect "rename file" gesture. */
   private int itemUnderMouse = -1;
   
   /**
    * When a workspace path cache update occurs, the workspace figures browser is reloaded in its entirety, while trying
    * to preserve the current selection.
    */
   @Override public void onWorkspaceUpdate(FCWorkspace.EventID id, File fp1, File fp2)
   {
      if(id == FCWorkspace.EventID.PATHCACHE && !isReloading) reload(false);
   }

   @Override protected void paintComponent(Graphics g)
   {
      Graphics2D g2 = (Graphics2D) g;
      int w = getWidth();
      int h = getHeight();
      
      // we always want nice-looking renderings
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      
      // paint the background with a gradient highlighting panel "title bar" along bottom edge
      g2.setPaint(new GradientPaint(0, h, getBackground(), 0, h-20, getBackground().brighter().brighter(), true));
      g2.fillRect(0, h-40, getWidth(), 40);
      g2.setPaint(getBackground());
      g2.fillRect(0, 0, getWidth(), h-40);
      
      g2.setColor(Color.GRAY);
      borderPath.setRoundRect(1, 1, w-2, h-2, 16, 16);
      g2.draw(borderPath);
   }

   /** Current user's <em>DataNav</em> workspace. */
   private FCWorkspace workspace = null;

   /**
    * This simple rendered model is installed in preview canvas while loading the real preview, or to display an error
    * message, or to indicate no selection.
    */
   private CenteredMessageModel placeHolderPreview = new CenteredMessageModel("LOADING...");

   /** The figure file currently selected in the browser's file list. Will be null if there is no selection. */
   private File selectedFile = null;
   
   /** 
    * Last modified time of the figure file currently previewed. This is stored when the figure begins loading and is
    * checked each time a path cache update event occurs -- to detect when the previewed file has changed.
    */
   private long selFileLastMod = 0L;
   
   /** 
    * The figure graphic model rendered in the preview for the current selection. It is null while model is loaded in 
    * the background, or if there is no current selection.
    */
   private FGraphicModel selectedFGM = null;
   
   /** Flag set while reloading contents -- so we know to ignore events thrown while doing so. */
   private transient boolean isReloading = false;
   
   /** The set of listeners registered to receive events broadcast by this workspace figures browser. */
   private EventListenerList listeners = new EventListenerList();

   /** Listing of all workspace folders containing any figure files. */
   private JList<File> figFolderList = null;

   /** Listing of all figure files in the currently selected workspace folder. */
   private JList<File> figFileList = null;
   
   /** The preview canvas on which the currently selected figure (if any) is rendered. */
   private Graph2DViewer previewer = null;
   
   /** 
    * Read-only text area displays any note associated with the currently selected figure, or if the figure failed
    * to load, an error description.
    */
   private JTextArea noteArea = null;
   
   /** The scroll pane container for the figure notes text area. */
   private JScrollPane noteScroller = null;
   
   /** Primary split pane controlling vertical real estate for folder/file lists panel versus the preview panel. */
   private JSplitPane splitMain = null;
   
   /** Nested split pane controlling vertical real estate within preview panel */
   private JSplitPane splitPreview = null;
   
   /** Combo box selects the order in which directories/files are listed. */
   private JComboBox<FCWorkspace.SortOrder> sortOrderCB = null;
   
   /** Pressing this button deletes selected file in the figure file list. Enabled only when that list has focus. */
   private JButton deleteFileBtn = null;
   /** Pressing this button copies selected file in the figure file list. Enabled only when that list has focus. */
   private JButton copyFileBtn = null;
   
   /** Pressing this button hides the workspace figures browser. */
   private JButton closeBtn = null;
   
   /** A rounded rectangle border for the (custom-painted) panel. */
   private RoundRectangle2D borderPath = new RoundRectangle2D.Double();
   
   /** Construct and layout the UI for this workspace figures browser. */
   private void construct()
   {
      //
      // NOTE: Wed call setPrototype***Value() to avoid a serious performance hit when a list box has many items. 
      // Without it, the renderer is called once for EVERY item in the list whenever the list contents are changed -- to 
      // calculate preferred size. We use a fake File with a long name as the prototype, as the LCE expects it.
      //
      File proto = new File("a_long_file_name.ext");

      figFolderList = new JList<File>();
      figFolderList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      figFolderList.setCellRenderer(new FigOrFolderLCE());
      // figFolderList.setVisibleRowCount(NVISROWS);
      figFolderList.setPrototypeCellValue(proto);
      figFolderList.addListSelectionListener(this);
      JScrollPane folderScroller = new JScrollPane(figFolderList);
      
      figFileList = new JList<File>();
      figFileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      figFileList.setCellRenderer(new FigOrFolderLCE());
      // figFileList.setVisibleRowCount(NVISROWS);
      figFileList.setPrototypeCellValue(proto);
      figFileList.addListSelectionListener(this);
      figFileList.addMouseListener(this);
      figFileList.addFocusListener(this);
      JScrollPane figScroller = new JScrollPane(figFileList);
      
      // support for an internal DnD gesture: dragging selected files from the figure file list and dropping them onto
      // a target directory in the figure folder list causes those files to be moved or copied to the target folder
      figFileList.setDragEnabled(true);
      figFileList.setTransferHandler(new FileListTransferHandler());
      figFolderList.setDragEnabled(true);
      figFolderList.setTransferHandler(new FileListTransferHandler());
      figFolderList.setDropMode(DropMode.ON);
      
      previewer = new Graph2DViewer(false);
      previewer.setResolution(workspace.getScreenDPI());
      previewer.addMouseListener(this); 
      
      noteArea = new JTextArea();
      noteArea.setTabSize(6);
      noteArea.setEditable(false);
      noteArea.setLineWrap(true);
      noteArea.setWrapStyleWord(true);
      noteArea.setToolTipText(NOTETOOLTIP);
      noteScroller = new JScrollPane(noteArea);
      
      sortOrderCB = new JComboBox<FCWorkspace.SortOrder>(FCWorkspace.SortOrder.values());
      sortOrderCB.setSelectedItem(workspace.getFileSortOrder());
      sortOrderCB.addActionListener(this);

      deleteFileBtn = new JButton(FCIcons.V4_DELETE_16);
      deleteFileBtn.setToolTipText("Delete all files currently selected in the figure file list");
      deleteFileBtn.setMargin(new Insets(0,0,0,0));
      deleteFileBtn.addActionListener(this);
      deleteFileBtn.setFocusable(false);  // so figure file list doesn't lose focus when button pressed
      
      copyFileBtn = new JButton(FCIcons.V4_COPY_16);
      copyFileBtn.setToolTipText("Copy all files currently selected in the figure file list");
      copyFileBtn.setMargin(new Insets(0,0,0,0));
      copyFileBtn.addActionListener(this);
      copyFileBtn.setFocusable(false);  // so figure file list doesn't lose focus when button pressed

      // the delete and copy file buttons are only enabled when fig file list has keyboard focus. It won't initially.
      deleteFileBtn.setEnabled(false);
      copyFileBtn.setEnabled(false);
      
      JLabel title = new JLabel("Workspace Figures");
      title.setIcon(FCIcons.V4_FIGURE_32);
      title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
      
      closeBtn = new JButton(FCIcons.V4_CLOSE_24);
      closeBtn.setBorder(null); closeBtn.setContentAreaFilled(false); closeBtn.setBorderPainted(false);
      closeBtn.setMargin(new Insets(0,0,0,0));
      closeBtn.setFocusable(false);
      closeBtn.addActionListener(this);
      
      // put the two lists and preview canvas in a single-row grid so they get proportionately the same space. The sort
      // order combo box and delete/copy file buttons are grouped underneath the figure file list, while the figure note
      // area is under the canvas.
      JPanel sortCBPanel = new JPanel(new SpringLayout());
      sortCBPanel.add(sortOrderCB);
      sortCBPanel.add(deleteFileBtn);
      sortCBPanel.add(copyFileBtn);
      SpringLayout layout = (SpringLayout) sortCBPanel.getLayout();
      layout.putConstraint(SpringLayout.NORTH, sortOrderCB, 3, SpringLayout.NORTH, sortCBPanel);
      layout.putConstraint(SpringLayout.SOUTH, sortCBPanel, 3, SpringLayout.SOUTH, sortOrderCB);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, deleteFileBtn, -2, SpringLayout.VERTICAL_CENTER, sortOrderCB);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, copyFileBtn, -2, SpringLayout.VERTICAL_CENTER, sortOrderCB);
      layout.putConstraint(SpringLayout.WEST, sortOrderCB, 0, SpringLayout.WEST, sortCBPanel);
      layout.putConstraint(SpringLayout.EAST, copyFileBtn, 0, SpringLayout.EAST, sortCBPanel);
      layout.putConstraint(SpringLayout.EAST, deleteFileBtn, -3, SpringLayout.WEST, copyFileBtn);

      JPanel figListPanel = new JPanel(new BorderLayout());
      figListPanel.add(figScroller, BorderLayout.CENTER);
      figListPanel.add(sortCBPanel, BorderLayout.SOUTH);
      
      // use nested split panes to give user control over allocation of vertical space in the browser. The folder and
      // file lists equally share the top half of the first splitter, while the preview panel is in the bottom half.
      // The preview panel is a nested splitter apportioning vertical space between the preview canvas and the figure
      // notes text scroll pane.
      JPanel p = new JPanel(new GridLayout(2, 0, 0, 10));
      p.add(folderScroller);
      p.add(figListPanel);
      
      splitPreview = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, previewer, noteScroller);
      splitPreview.setResizeWeight(0.8);
      previewer.setMinimumSize(new Dimension(FIXEDWIDTH, 150));
      noteScroller.setMinimumSize(new Dimension(FIXEDWIDTH, 50));      
      
      splitMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, p, splitPreview);
      Dimension minSz = new Dimension(FIXEDWIDTH, 200);
      p.setMinimumSize(minSz);
      splitPreview.setMinimumSize(minSz);
      
      layout = new SpringLayout();
      setLayout(layout);
      add(title);
      add(closeBtn);
      add(splitMain);
      layout.putConstraint(SpringLayout.NORTH, splitMain, 10, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, title, 10, SpringLayout.SOUTH, splitMain);
      layout.putConstraint(SpringLayout.SOUTH, this, 10, SpringLayout.SOUTH, title);
      layout.putConstraint(SpringLayout.WEST, title, 10, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, closeBtn, -10, SpringLayout.EAST, this);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, closeBtn, 0, SpringLayout.VERTICAL_CENTER, title);
      layout.putConstraint(SpringLayout.WEST, splitMain, 10, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 10, SpringLayout.EAST, splitMain);
   }
   
   /** Flag set whenever the figure note text is hidden (because there is no note). */
   private boolean noteHidden = false;
   /** Divider location for the preview panel splitter the last time the figure note text was visible. */
   private int noteDivLoc = -1;
   
   /**
    * Show or hide the figure note text area that is part of the preview panel in the bottom section of the workspace
    * figures browser. The text area is shown if it contains text, else it is hidden. This leaves more vertical space
    * for the preview canvas when there is no figure note to display.
    */
   private void showHideNoteText()
   {
      boolean hide = (noteArea.getText().length() == 0);
      if(noteHidden == hide) return;
      
      boolean wasHidden = noteHidden;
      noteHidden = hide;
      if(wasHidden)
      {
         splitPreview.setBottomComponent(noteScroller);
         if(noteDivLoc >= splitPreview.getMinimumDividerLocation() && 
               noteDivLoc <= splitPreview.getLastDividerLocation())
            splitPreview.setDividerLocation(noteDivLoc);
         else
            noteDivLoc = splitPreview.getDividerLocation();
      }
      else
      {
         noteDivLoc = splitPreview.getDividerLocation();
         splitPreview.setBottomComponent(null);
      }
   }
   
   /**
    * Load or reload the contents of the workspace figures browser.
    * @param initial True if we're loading browser for the first time, in which case we initialize it to select the
    * most recently used figure file (if there is one). Otherwise, the browser contents are simply reloaded while trying
    * to preserve the current selection -- if it exists and is still there after reload.
    */
   private void reload(boolean initial)
   {
      FileSystemView fsView = FileSystemView.getFileSystemView();
      
      isReloading = true;
      try
      {
         // we'll try to restore the previous file selected in the file list, if possible. On initial load, however, we 
         // display the most recently used figure file (if there is one).
         File oldSelectedFile = selectedFile;
         File oldDir = (selectedFile != null) ? selectedFile.getParentFile() : null;
         if(initial)
         {
            List<File> mruFigs = workspace.getRecentFiles(false);
            if(!mruFigs.isEmpty())
            {
               oldSelectedFile = mruFigs.get(0);
               oldDir = oldSelectedFile.getParentFile();
            }
         }
         
         // do we need to sort folder and file listings?
         boolean needSort = (workspace.getFileSortOrder() != FCWorkspace.SortOrder.NONE);
         
         // reload figure folder list to include any workspace directories containing either FypML or Matlab FIG files.
         // If previously selected folder is still there, select it and load all figure files in it into the file list. 
         // Else, load the first folder's files into the file list. If the previously selected file is in the selected 
         // folder, then select it; else, select first available file in that folder. Both folder and file lists are 
         // sorted IAW user's sort order preference.
         figFolderList.setListData(new File[0]);
         figFileList.setListData(new File[0]);
         
         List<File> dirs = workspace.getWorkspaceDirectories(FCWorkspace.SrcType.FYP);
         List<File> figDirs = workspace.getWorkspaceDirectories(FCWorkspace.SrcType.MATFIG);
         for(File f : dirs) figDirs.remove(f);
         dirs.addAll(figDirs);
         if(needSort) Collections.sort(dirs);
         figFolderList.setListData(dirs.toArray(new File[0]));
         figFolderList.clearSelection();
         File iniDir = dirs.contains(oldDir) ? oldDir : ((dirs.size() > 0) ? dirs.get(0) : null);
         
         boolean hasFiles = false;
         if(iniDir != null)
         {
            figFolderList.setSelectedValue(iniDir, true);
            
            List<File> files = workspace.getWorkspaceFiles(iniDir, FCWorkspace.SrcType.FYP);
            List<File> figFiles = workspace.getWorkspaceFiles(iniDir, FCWorkspace.SrcType.MATFIG);
            // for(File f : files) figFiles.remove(f);  NOT SURE WHY THIS WAS HERE!
            files.addAll(figFiles);
            if(needSort) Collections.sort(files, fileSorter);

            figFileList.setListData(files.toArray(new File[0]));
            figFileList.clearSelection();
            hasFiles = (files.size() > 0);
         }
         if(hasFiles)
         {
            if(fsView.isParent(iniDir, oldSelectedFile)) figFileList.setSelectedValue(oldSelectedFile, true);
            else figFileList.setSelectedIndex(0);
         }

         updatePreviewedFile();
      }
      finally { isReloading = false; }
   }

   /** For sorting contents of figure folder and file lists IAW user's sort order preference. */
   private FCWorkspace.FileComparator fileSorter = new FCWorkspace.FileComparator();

   /** 
    * Worker reads the selected FypML figure file or Matlab FIG file and constructs its graphic model in the background,
    * then -- back on the event dispatch thread -- loads that model into the preview canvas.
    */
   private FigureLoader figLoader = null;
   
   /**
    * Helper method updates the currently previewed file. It spawns a loader thread to read the file and prepare the 
    * figure's graphic model. Upon completion (back on the event dispatch thread), the prepared model is installed in
    * the preview canvas. A placeholder model displaying the message "LOADING..." is installed in the preview canvas 
    * prior to spawning the loader thread.
    */
   private void updatePreviewedFile()
   {
      File f = (File) figFileList.getSelectedValue();
      if(selectedFile==f || (selectedFile != null && selectedFile.equals(f)))
      {
         if(selectedFile == null || selectedFile.lastModified() == selFileLastMod) return;
      }
      
      // if a figure load task is in progress, cancel it if we can.
      if(figLoader != null)
      {
         figLoader.cancel(true);
         figLoader = null;
      }
      
      selectedFile = f;
      boolean isFile = (selectedFile != null) && selectedFile.isFile();
      
      // load placeholder preview initially
      placeHolderPreview.setTextMessage(!isFile ? "" : "LOADING...");
      previewer.setModel(placeHolderPreview);
      noteArea.setText("");
      if(!isFile)
      {
         // no file, therefore no figure note -- hide the figure note text area if it is not already hidden
         showHideNoteText();
         return;
      }
      
      // spawn new task to load the graphic model from the figure definition file selected
      selectedFGM = null;
      selFileLastMod = selectedFile.lastModified();  // remember this so we can detect when selected file changes!
      figLoader = new FigureLoader(selectedFile);
      figLoader.execute();
   }
   
   /**
    * Helper method that makes a copy of each file currently selected in the workspace browser's figure file list, 
    * storing the file copies in the same parent directory. If no file is selected, no action is taken. Otherwise, the 
    * figure file list is reloaded to reflect the changes, and all of the copied figures are selected. If a single file
    * is copied, the user is immediately given an opportunity to rename the copied file.
    */
   private void copySelectedFigureFiles()
   {
      if(isReloading) return;
      List<File> selection = figFileList.getSelectedValuesList();
      if(selection.size() == 0) return;
      
      isReloading = true;
      try
      {
         File dir = selection.get(0).getParentFile();
         
         // copy selected files and add them to the user's workspace. The file copy appears in the same directory as the
         // source file, but the name is changed from "name.ext" to "name_Copyn.ext", where n is an integer string to 
         // ensure uniqueness
         List<File> selAfter = new ArrayList<File>();
         for(File f : selection) 
         {
            // get base name and extension for filename. Then construct a filename for the copied file that does not
            // exist in the parent directory
            String[] nameAndExt = f.getName().split("\\.(?=[^\\.]+$)");
            int i = 1;
            File dst = new File(dir, nameAndExt[0] + "_Copy" + i + "." + nameAndExt[1]);
            while(dst.isFile()) { ++i; dst = new File(dir, nameAndExt[0] + "_Copy" + i + "." + nameAndExt[1]); }
            
            // copy the file
            if(null == Utilities.copyFile(f, dst))
            {
               workspace.addToWorkspace(dst);
               selAfter.add(dst);
            }
         }
         if(selAfter.size() == 0) return;
         
         // now reload the figure file list to reflect the new files
         List<File> files = workspace.getWorkspaceFiles(dir, FCWorkspace.SrcType.FYP);
         List<File> figFiles = workspace.getWorkspaceFiles(dir, FCWorkspace.SrcType.MATFIG);
         for(File f : files) figFiles.remove(f);
         files.addAll(figFiles);
         if((workspace.getFileSortOrder() != FCWorkspace.SortOrder.NONE)) Collections.sort(files, fileSorter);

         figFileList.setListData(files.toArray(new File[0]));
         
         // if just one file was copied, make it the current selection and give the user an immediate opportunity to 
         // change its file name. If multiple copies were made, simply select all of the file copies 
         if(selAfter.size() > 1)
            selectMultipleFigures(selAfter);
         else
         {
            figFileList.setSelectedValue(selAfter.get(0), true);
            updatePreviewedFile();
            startRenamingSelectedFigureFile();
         }
      }
      finally { isReloading = false; }
   }
   
   /**
    * Helper method selects multiple figure files in the workspace browser's figure file list. It is used to update the
    * selection after a multi-file copy or move operation.
    * @param select The list of files that should be selected.
    */
   private void selectMultipleFigures(List<File> select)
   {
      ListModel<File> lm = figFileList.getModel();
      int[] selected = new int[select.size()];
      for(int i=0; i<select.size(); i++) 
      {
         selected[i] = -1;
         for(int j=0; j<lm.getSize(); j++)
         {
            File f =  lm.getElementAt(j);
            if(Utilities.filesEqual(f, select.get(i)))
            {
               selected[i] = j;
               break;
            }
         }
      }
      figFileList.setSelectedIndices(selected);
      figFileList.ensureIndexIsVisible(selected[0]);
   }

   /**
    * Helper method that permanently deletes file(s) currently selected the workspace browser's figure file list. If
    * no file is selected, no action is taken. Otherwise, the figure file list is reloaded to reflect the change. The
    * file before or after the first file (lowest index) selected will be selected after the files are removed.
    */
   private void deleteSelectedFigureFiles()
   {
      if(isReloading) return;
      List<File> selection = figFileList.getSelectedValuesList();
      if(selection.size() == 0) return;
      
      isReloading = true;
      try
      {
         File dir = selection.get(0).getParentFile();
         
         // delete selected figure files and remove them from user's workspace
         int nDel = 0;
         for(File f : selection) 
         {
            if(f.delete())
            {
               ++nDel;
               workspace.removeFromWorkspace(f);
            }
         }
         if(nDel == 0) return;
         
         // get index of file to select after reload. If multiple files were deleted, this chooses the index before the
         // first file (smallest index) selected.
         int idx = figFileList.getSelectedIndex() - 1;
         if(idx < 0) idx = 0; 
         
         // now reload the figure file list to reflect the change, and restore the selection.
         List<File> files = workspace.getWorkspaceFiles(dir, FCWorkspace.SrcType.FYP);
         List<File> figFiles = workspace.getWorkspaceFiles(dir, FCWorkspace.SrcType.MATFIG);
         files.addAll(figFiles);
         if((workspace.getFileSortOrder() != FCWorkspace.SortOrder.NONE)) Collections.sort(files, fileSorter);

         figFileList.setListData(files.toArray(new File[0]));
         if(files.size() > 0)
         {
            if(idx >= files.size()) idx = 0;  // just in case
            figFileList.setSelectedValue(files.get(idx), true);
         }
         
         updatePreviewedFile();
         
         figFileList.requestFocusInWindow();  // in case figure file list loses the keyboard focus
      }
      finally { isReloading = false; }
   }


   /**
    * Helper method raises an in-place editor on top of the currently selected item in the figure file list to allow the
    * user to rename the selected file. Since {@link JList} does not support its own in-place editor, we use a text
    * field wrapped in an undecorated {@link JDialog}. The text field fills the entire dialog, which is sized and 
    * positioned so that it appears directly on top of the list item itself. The user can type a new name into the text 
    * field and hit return to confirm the name change. The new filename will be rejected if the extension was changed,
    * or if another file with the same name already exists in the parent folder. Clicking outside the dialog or hitting 
    * the ESCape key will cancel the rename operation.
    */
   private void startRenamingSelectedFigureFile()
   {
      int row = figFileList.getSelectedIndex();
      if(row < 0 || selectedFile == null) return;
      
      // the rename popup is lazily created
      if(renamePopup == null)
      {
         renameTF = new JTextField();
         Border b = UIManager.getBorder("List.focusCellHighlightBorder");
         renameTF.setBorder(b != null ? b : BorderFactory.createLineBorder(figFileList.getForeground()));
         renameTF.addActionListener(this);
         renameTF.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
         renameTF.getActionMap().put("cancel", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
               renamePopup.setVisible(false);
               figFileList.requestFocus();
            }
         });
         renamePopup = new JDialog();
         renamePopup.setUndecorated(true);
         renamePopup.setAlwaysOnTop(true);
         renamePopup.setAutoRequestFocus(true);
         renamePopup.add(renameTF);
         renamePopup.addWindowFocusListener(new WindowFocusListener() {
            @Override public void windowGainedFocus(WindowEvent e) {}
            @Override public void windowLostFocus(WindowEvent e) { renamePopup.setVisible(false); }
         });
      }
      
      renameTF.setText(selectedFile.getName());
      renameTF.select(0, selectedFile.getName().length() - 4);  // don't select the .fyp or .fig extension initially
      renameTF.requestFocusInWindow();
      
      Rectangle r = figFileList.getCellBounds(row, row);
      Point pUL = figFileList.getLocationOnScreen();
      renameTF.setPreferredSize(new Dimension(r.width - 16, r.height));
      renamePopup.pack();
      renamePopup.setLocation(pUL.x + r.x + 16, pUL.y + r.y);
      renamePopup.setVisible(true);
   }
   
   /**
    * Helper method called when the user hits the "Return" key after entering a new file name in the figure file list's 
    * in-place editor, which was raised by {@link #startRenamingSelectedFigureFile()}. The method verifies that the 
    * proposed file name carries the same extension as the original and does not duplicate another figure file in the 
    * parent folder. If either constraint is violated, the original filename is restored and the in-place editor remains
    * visible. Otherwise, the selected figure file is renamed and the workspace path cache updated accordingly. The
    * in-place editor popup is then extinguished and the figure file list is reloaded to reflect the change.
    * @param fileName Proposed filename for the file currently selected in the figure file list. If the name lacks an 
    * extension, the original file extension will be tacked on automatically.
    */
   private void finishRenamingSelectedFigureFile(String fileName)
   {
      if(selectedFile == null || selectedFile.getName().equals(fileName))
      {
         // no file selected, or file name is unchanged
         renamePopup.setVisible(false);
         return;
      }
      
      // if the proposed filename has no extension, add it. 
      FCFileType ft = FCFileType.getFileType(selectedFile);
      if(fileName.lastIndexOf('.') == -1) fileName = ft.addValidExtension(fileName);
      File renamedFile = new File(selectedFile.getParentFile(), fileName);
      
      // if the proposed filename has a valid extension and does not duplicate an existing file, then rename the file.
      // If unable to rename it, the file-rename popup remains visible, with original file name restored in text field.
      if(renamedFile.isFile() || 
            !ft.hasValidExtension(renamedFile) || 
            !selectedFile.renameTo(renamedFile))
      {
         Toolkit.getDefaultToolkit().beep();
         renameTF.setText(selectedFile.getName());
         renameTF.select(0, selectedFile.getName().length() - 4); 
         return;
      }
      
      // success! Hide pop-up editor, update workspace, reload fig file list. The renamed file is selected.
      renamePopup.setVisible(false);

      isReloading = true;
      try
      {
         if(!workspace.renameInWorkspace(selectedFile, renamedFile)) return;
         
         // now reload the figure file list to reflect the new file, and make it the current selection.
         File dir = renamedFile.getParentFile();
         List<File> files = workspace.getWorkspaceFiles(dir, FCWorkspace.SrcType.FYP);
         List<File> figFiles = workspace.getWorkspaceFiles(dir, FCWorkspace.SrcType.MATFIG);
         for(File f : files) figFiles.remove(f);
         files.addAll(figFiles);
         if((workspace.getFileSortOrder() != FCWorkspace.SortOrder.NONE)) Collections.sort(files, fileSorter);

         figFileList.setListData(files.toArray(new File[0]));
         figFileList.setSelectedValue(renamedFile, true);
         updatePreviewedFile();
      }
      finally { isReloading = false; }
   }
   
   /** 
    * An undecorated dialog that serves as an in-place editor to rename a file in the figure file list. Lazily created. 
    */
   private JDialog renamePopup = null;
   /** The text field widget within the figure file-rename popup dialog. */
   private JTextField renameTF = null;
   
   /**
    * A {@link SwingWorker SwingWorker} that loads a <i>DataNav</i> figure graphic from a FypML figure
    * definition file or imports a Matlab FIG file as a FypML figure. After that task has finished, back on the Swing 
    * event dispatch thread, the figure graphic is loaded into the preview canvas on the workspace figure browser's UI.
    * If the figure could not be loaded, a brief error message is displayed in the canvas.
    * 
    * @author sruffner
    */
   private class FigureLoader extends SwingWorker<FGraphicModel, Object>
   {
      /**
       * Construct a Swing worker thread that loads the figure defined in the specified source file.
       * @param srcFile Pathname for the FypML or Matlab FIG source file from which figure is loaded.
       */
      FigureLoader(File srcFile) { this.figSrcFile = srcFile; }

      @Override protected FGraphicModel doInBackground() throws Exception
      {
         StringBuffer eBuf = new StringBuffer();
         FGraphicModel model = null;
         if(FCFileType.getFileType(figSrcFile) == FCFileType.FYP)
            model = FGModelSchemaConverter.fromXML(figSrcFile, eBuf);
         else
            model = MatlabFigureImporter.importMatlabFigureFromFile(figSrcFile, eBuf);
         
         if(model == null) emsg = eBuf.toString();
         return(model);
      }
      
      @Override protected void done()
      {
         if(!isCancelled())
         {
            FGraphicModel model = null;
            try{ model = get(); } catch(Exception e) 
            {
               System.err.println("Exception on SwingWorker.get(): " + e.getMessage()); 
               e.printStackTrace();
            }
            
            if(emsg != null)
            {               
               // when the figure fails to load, we display the error description in the text area below the preview
               // canvas. The canvas itself displays the message "Failed to load".
               placeHolderPreview.setTextMessage("Failed to load");
               if(previewer.getModel() != placeHolderPreview) previewer.setModel(placeHolderPreview);
               noteArea.setText("An error has occurred. Report to developer:\n\n" + emsg);
               noteArea.setToolTipText(null);
            }
            else
            {
               selectedFGM = model;
               
               previewer.setModel(selectedFGM);
               String note = ((FigureNode) selectedFGM.getRoot()).getNote().trim();
               noteArea.setText(note);
               noteArea.setToolTipText(NOTETOOLTIP);
            }
            
            // update visible status of figure note text area if necessary
            showHideNoteText();
         }
      }

      /** The figure source file processed by this figure loader. */
      private File figSrcFile = null;
      /** If not null, this is a brief description of the error that occurred while attempting to load figure. */
      private String emsg = null;
   }

   private final static String NOTETOOLTIP = "Author's note/desecription of figure";
   
   /**
    * A simple list cell renderer for the workspace figure browser's folder and file lists. It uses 16-pixel icons to
    * represent the folders and figure source files (using different icons to distinguish FypML from Matlab FIG files), 
    * and it generates a tooltip that shows the absolute path to the folder or file. It also sets the background color
    * of the current selected item to light-gray when the list control is NOT the focus owner -- so the user knows when
    * a list has the keyboard focus or not.
    * @author sruffner
    */
   private class FigOrFolderLCE extends DefaultListCellRenderer
   {
      @Override public Component getListCellRendererComponent(JList<? extends Object> list, Object value, int index, 
            boolean select, boolean focus)
      {
         super.getListCellRendererComponent(list, value, index, select, focus);
         String s = "";
         String tooltip = null;
         Icon icon = null;
         Color bkg = list.getBackground();
         Color fg = list.getForeground();
         if(value instanceof File) 
         {
            File f = (File)value;
            s = f.getName();
            if(f.isFile())
            {
               icon = FCFileType.getFileType(f) == FCFileType.FYP ? FCIcons.V4_FIGURE_16 : FCIcons.V4_MATFIG_16;
               sb.setLength(0);
               sb.append("<html>" + f.getAbsolutePath().replace("/", "&#47;") + "<br/>");
               sb.append("" + f.length() + " bytes<br/>");
               long t = f.lastModified();
               if(t == 0) sb.append("--");
               else
               {
                  lastModDate.setTime(t);
                  sb.append(dateFormatter.format(lastModDate));
               }
               sb.append("</html>");
               tooltip = sb.toString();
            }
            else
            {
               icon = FCIcons.V4_FIGFOLDER_16;
               tooltip = f.getAbsolutePath();
            }
            
            JList.DropLocation dropLocation = list.getDropLocation();
            if(dropLocation != null && !dropLocation.isInsert() && dropLocation.getIndex() == index)
            {
               bkg = list.getSelectionBackground();
               fg = list.getSelectionForeground();
            }
            else if(select)
            {
               bkg = list.isFocusOwner() ? list.getSelectionBackground() : Color.LIGHT_GRAY;
               fg = list.isFocusOwner() ? list.getSelectionForeground() : list.getForeground();
            }
         }
       
         if(list.getLayoutOrientation() != JList.VERTICAL) s += "   ";
         setText(s);
         setToolTipText(tooltip);
         setIcon(icon);
         setBackground(bkg);
         setForeground(fg);
         return(this);
      }
      
      private DateFormat dateFormatter = DateFormat.getDateTimeInstance();
      private Date lastModDate = new Date(0);
      private StringBuilder sb = new StringBuilder();
   }
   
   /**
    * Notifies any registered listeners that the user selected a figure file to be opened, or closed this workspace
    * figures browser. All event notifications are queued for later invocation on the Swing event dispatch thread.
    * @param figFile The FypML figure definition file to be opened. If null, then the browser has been closed via its
    * own "close" button.
    */
   private void notifyListeners(final File figFile)
   {
      final Object[] rcvrs = listeners.getListenerList();
      if(rcvrs.length == 0) return;
      
      SwingUtilities.invokeLater(new Runnable() {
         public void run()
         {
            for(int i = rcvrs.length-2; i>=0; i-=2) if(rcvrs[i] == Listener.class)
            {
               if(figFile == null) ((Listener) rcvrs[i+1]).onWSFBClose();
               else ((Listener) rcvrs[i+1]).onWSFBOpenFigure(figFile);
            }
         }
      });
   }

   /** A listener that can receive and respond to events that may be broadcast by the workspace figures browser. */
   public interface Listener extends EventListener
   {
      /**
       * Messaged when user double-clicks on a figure file in the workspace browser's figure file list -- meaning that
       * the user would like to open or otherwise select that file. <i>The file could be a FypML figure definition (with
       * extension ".fyp") or a Matlab figure (".fig").</i>
       * @param f The figure source file that was selected.
       */
      void onWSFBOpenFigure(File f);
      
      /**
       * Messaged when the user closes the workspace figures browser by pressing its "close" button. This is messaged
       * <b>after</b> the browser has been hidden.
       */
      void onWSFBClose();
   }
   
   
   //
   // DnD support for moving/copying selected files in file list to a target folder in folder list
   //
   
   private static DataFlavor dndInternalFlavor;
   static
   {
      try 
      {
         dndInternalFlavor = new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" +
               ArrayList.class.getName() + "\"");
      }
      catch(ClassNotFoundException cnfe) {}
   }
   
   /**
    * The transferable for the internal DnD operation by which the user drags selected files in the figure file list 
    * and drops the selection onto a target directory in the figure folder list. It encapsulates the list of selected
    * files.
    * 
    * @author sruffner
    */
   private class FileListTransferable implements Transferable
   {
      FileListTransferable(List<File> fileValues)
      {
         files = new ArrayList<File>();
         files.addAll(fileValues);
      }
      
      @Override public DataFlavor[] getTransferDataFlavors() { return( new DataFlavor[] {dndInternalFlavor} ); }

      @Override public boolean isDataFlavorSupported(DataFlavor flavor) { return(dndInternalFlavor.equals(flavor)); }

      @Override public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
      {
         if(!dndInternalFlavor.equals(flavor)) throw new UnsupportedFlavorException(flavor);
         return(new ArrayList<File>(files));
      }
      
      private List<File> files;
   }
   
   /**
    * The transferable for the internal DnD operation by which the user drags selected files in the figure file list 
    * and drops the selection onto a target directory in the figure folder list. It only supports that specific DnD
    * operation; clipboard interactions are not supported. The selected files may be moved or copied to the target
    * directory.
    * 
    * @author sruffner
    */
   private class FileListTransferHandler extends TransferHandler 
   {
      /**
       * This transfer handler only imports a list of {@link File} objects -- the path names of the figure
       * files to be moved or copied.
       */
      public boolean canImport(TransferSupport info)
      {
         if(!info.isDataFlavorSupported(dndInternalFlavor)) return(false);
         
         // we ONLY allow drop onto the figure folder list.
         if(info.getComponent() != figFolderList) return(false);
         
         return(true);
      }

      /** The transferable is the list of files currently selected in the workspace browser's figure file list. */
      protected Transferable createTransferable(JComponent c) 
      {
         if(c != figFileList) return(null);
         List<File> values = figFileList.getSelectedValuesList();
         if(values.size() == 0) return(null);
         
         return( new FileListTransferable(values));
      }
      
      /** Selected files may be copied or moved to the drop folder in the workspace browser's figure folder list. */
      public int getSourceActions(JComponent c) { return(TransferHandler.COPY_OR_MOVE); }
      
      /**
       * This transfer handler only supports dragging selected files from the figure file list and dropping them onto a
       * target folder in the figure folder list within the workspace browser. Thus, the drop target must be the figure
       * folder list, and the transferable is the list of files selected in the figure file list. Furthermore, this
       * method handles both the "import" and "export" aspects of the operation. The selected files are copied to the
       * target folder, and then removed from their original parent folder in the DnD is a "move" rather than a "copy"
       * operation. No action is taken in {@link #exportDone()}.
       */
      @SuppressWarnings("unchecked")
      public boolean importData(TransferSupport info)
      {
         if(!info.isDrop()) return(false);
         if(info.getComponent() != figFolderList) return(false);
          
         JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
         File targetDir = (File) figFolderList.getModel().getElementAt(dl.getIndex());
         
         // get the list of files that are being dropped
         Transferable t = info.getTransferable();
         List<File> files;
         try { files = (List<File>) t.getTransferData(dndInternalFlavor); } 
         catch (Exception e) { return(false); }
         if(files == null || files.isEmpty()) return(false);
          
         // if the first file's parent directory is the same as the target directory, then there's nothing to do!
         File parent = files.get(0).getParentFile();
         if(Utilities.filesEqual(targetDir, parent)) return(false);
          
         // copy or move the files to the target directory. 
         List<File> selAfter = new ArrayList<File>();
         boolean isMove = (info.getDropAction() == TransferHandler.MOVE);
         int nOk = 0;
         for(File f : files)
         {
            // prepare path name for file so that it appears in the target directory with the same file name. However, 
            // if the directory already contains a file with that name, prepend "v{n}_" to the file name, where the
            // integer {n} is chosen to ensure uniqueness.
            String fname = f.getName();
            File dst = new File(targetDir, fname);
            if(dst.isFile())
            {
               int i = 1;
               while(dst.isFile()) { ++i; dst = new File(targetDir, "v" + i + "_" + fname); }
            }
            
            // move or copy the file to the new location. If unsuccessful, do NOT continue.
            boolean ok = isMove ? f.renameTo(dst) : (null == Utilities.copyFile(f,  dst));
            if(!ok) break;
            
            // let workspace know about the copied/moved figure file
            if(isMove) workspace.renameInWorkspace(f, dst);
            else workspace.addToWorkspace(dst);
            
            selAfter.add(dst);
            ++nOk;
         }
          
         // if some files were copied/moved, reload workspace browser to refresh its content, then select the target 
         // directory, and select the files just copied/moved to that directory, so user can see the files in their new
         // location. 
         if(nOk > 0)
         {
            reload(false);
            selectFigureFolder(targetDir);
            selectMultipleFigures(selAfter);
         }
         
         // if one or more files were not successfully transferred, inform user
         if(nOk < files.size())
         {
            String title = isMove ? "File move" : "File copy";
            String msg = (files.size() - nOk) + " files could not be " + (isMove ? "moved" : "copied");
            JOptionPane.showMessageDialog(figFolderList, msg, title, JOptionPane.ERROR_MESSAGE);
         }
         return(true);
      }

      /** No action taken. See {@link #importData(TransferSupport)}. */
      protected void exportDone(JComponent c, Transferable data, int action) {}
   }
}
