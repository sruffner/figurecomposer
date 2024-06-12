package com.srscicomp.fc.uibase;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.filechooser.FileSystemView;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.AbstractDocument;

import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.SVGHints;
import org.jfree.svg.SVGUnits;
import org.jfree.svg.util.DoubleConverter;

import com.srscicomp.common.g2dviewer.Graph2DViewer;
import com.srscicomp.common.g2dviewer.RenderableModel;
import com.srscicomp.common.g2dviewer.RootRenderable;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSetIDFilter;
import com.srscicomp.fc.data.DataSetInfo;
import com.srscicomp.fc.data.DataSrcFactory;
import com.srscicomp.fc.data.IDataSrc;
import com.srscicomp.fc.data.DataSet.Fmt;
import com.srscicomp.fc.fig.FGModelSchemaConverter;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FigureNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.fig.PDFSupport;
import com.srscicomp.fc.fig.PSDoc;
import com.srscicomp.fc.fypml.FGModelSchema;
import com.srscicomp.fc.matlab.MatlabFigureImporter;


/**
 * <b>FCChooser</b> provides a custom file chooser dialog and other file operations for use in <i>Figure Compsoer</i>..
 * 
 * <p>[DEV NOTE: I've just had too many problems with <b>JFileChooser</b>, particularly under Mac OS X -- so I decided 
 * to roll my own chooser dialog specifically tailored for <i>FC</i>.]</p>
 * 
 * <p><b>FCChooser</b> provides methods for opening or saving files having <em>FC</em>-specific content. Several of 
 * these file operations raise a modal "chooser dialog" that lets the user browse the file system for the file to open 
 * or save. Below is a list of the primary components on this dialog:</p>
 * <ul>
 *    <li><i>Filename</i> field. In file-open modes, this is a read-only field displaying the name of the file 
 *    currently selected. If there is no selection, it will be blank. In file-save modes, the field is editable so that
 *    the user can specify the name of a new destination file. Since file extensions must conform to the currently 
 *    selected file type, whatever the user enters here may not be the actual filename used. Also, if the user enters 
 *    any invalid filename, the dialog's confirm button will be disabled. Hitting the "Enter" key when the keyboard 
 *    focus is on this field will confirm the filename specified in the field and extinguish the dialog (same as 
 *    clicking the "OK" button).</li>
 *    <li><i>Current directory</i> combo box. This non-editable combo box displays the chooser's current "working 
 *    directory", and the drop-down list contains its ancestors in order from its immediate parent to the containing 
 *    file system root. If there any directories in the user's workspace path cache that have relevant FC source files,
 *    those "workspace directories" will be listed after the root in the drop-down list. The user can switch directories
 *    by selecting any item in the list.</li>
 *    <li><i>Directory listing</i>. This is a two-column tabular listing of the current directory's contents. All 
 *    sub-directories are listed first, from most recent to least, followed by all files that match the current file 
 *    type, again from most recent to least. If the user selects a different file in this listing, the other components 
 *    in the dialog are updated accordingly. To open a sub-directory (ie, to make it the current directory), user must
 *    double-click on it. Double-clicking on a file in this listing will select that file and extinguish the dialog 
 *    (same as clicking the "OK" button).</li>
 *    <li><i>File format</i> combo box. All {@link FCFileType}s compatible with the dialog's current operational 
 *    mode are listed in this combo box. For example, when exporting a figure, the user can select among three 
 *    different file formats -- Postscript, JPEG, or PNG. When the file format is changed, the other components on the
 *    dialog are updated as needed.</li>
 *    <li><i>Data set</i> control group. This panel of components is present only when extracting a data set from a 
 *    <i>FC</i>-compatible data source file or saving a data set to such a file. In the first case, only a list control 
 *    appears in which all data sets available in the currently selected source file are listed by ID. The selected data 
 *    set is rendered in the preview canvas. If the current file is not a valid data source file, this list will be 
 *    empty. When saving data, the list control is accompanied by an <i>ID</i> field and a pair of mutually exclusive 
 *    radio buttons. If the currently selected destination file exists and contains data sets, their IDs are enumerated 
 *    in the list as before, but they cannot be selected. The user must enter a valid ID that does not duplicate one of 
 *    the existing IDs (if it is a duplicate, the confirm button will be disabled and the selection field will display a
 *    message indicating the problem); the radio buttons let the user either append the data set to the existing source 
 *    file or overwrite the file's contents entirely. On the other hand, if the current destination file does not exist,
 *    the ID list is empty and the radio buttons are disabled. Also, the file format combo box lets the user choose 
 *    between the different available data source file formats.</li>
 *    <li><i>Preview canvas</i>. An instance of {@link Graph2DViewer} on which the currently selected object is 
 *    displayed. In any file-save mode, it simply displays the object to be saved -- either a figure ({@link 
 *    FGraphicModel}) or a data set ({@link DataSet} wrapped in a {@link DataSetPreview}). In any file-open mode, it 
 *    displays the object currently selected on the chooser dialog -- again a figure or data set. If there is no 
 *    selection or an incompatible file is selected, the canvas will display an appropriate message. Also, since it can 
 *    take an indeterminate amount of time to read in the selected file, it will display the message "LOADING..."  or 
 *    "IMPORTING..." (when importing a Matlab figure) while a background thread ({@link FCFileLoader}) does the 
 *    work.</li>
 *    <li><i>Current selection field</i>. This read-only text field displays the full path of the current selection.
 *    When opening or saving a data set, the file path is followed by a colon and the currently selected ID. If the 
 *    current selection is invalid in some way, this field will display a string describing the problem.</li>
 *    <li><i>Confirm and cancel buttons</i>. The <i>confirm</i> button has different text labels depending on the 
 *    action: "Save", "Open", or "Export". If the current selection is invalid in some way, this button is disabled. The
 *    <i>cancel</i> button, of course, is always enabled.</li>
 * </ul>
 * 
 * <p><b>FCChooser</b> is dependent upon the user's <i>Figure Composer</i> workspace, encapsulated by the singleton
 * workspace manager, {@link FCWorkspace}. It examines the workspace to set its initial working directory at startup, 
 * and recently opened figure models are cached by the workspace manager to improve performance in typical usage
 * scenarios. It uses {@link FCFileLoader} to read files and prepare graphic models of figures and data sets.</p.
 * 
 * <p><b>FCChooser</b> was originally designed as a singleton {@link JDialog} owned by the application frame window. 
 * This design had a flaw -- if the dialog was raised from another modal dialog, it would appear on top of the frame but
 * underneath the other dialog. To work around this issue, <b>FCChooser</b> is now a singleton {@link JPanel} that is 
 * installed in a new modal dialog each time it is raised. When that dialog is extinguished, the panel is removed from 
 * the dialog before the dialog is disposed.</p>
 * 
 * <p>[NOTE (Jan2017): <b>FCChooser</b> was updated for Java 8 code compatibility. This involved fixing compiler
 * warnings about raw types. As of JDK 7, a number of JDK classes were made generic, such as JList and JComboBox.]</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
public class FCChooser extends JPanel implements ActionListener, ListSelectionListener, DocumentListener, MouseListener,
      ComponentListener
{
   /**
    * Get the singleton instance of <i>Figure Composer</i>'s custom file chooser. Created on first invocation. 
    * <p><b>IMPORTANT</b>: On the first invocation, the method constructs the chooser panel. It must be called as early 
    * as possible during application startup -- prior to setting up any other GUI components which may access the 
    * chooser. The panel is merely constructed and initialized here. It is installed in a suitable modal dialog whenever
    * the chooser is raised.</p>
    * @return The singleton file chooser object.
    */
   public static FCChooser getInstance() 
   { 
      if(theChooser == null) theChooser = new FCChooser();
      return(theChooser); 
   }
   
   /** 
    * The one and only instance of the file chooser. The chooser's panel is installed in a suitable modal dialog as 
    * needed. Thus, it can be raised from the application frame window or from another modal dialog!
    */
   private static FCChooser theChooser = null;
   
   
   /** 
    * Get the working directory -- i.e., the last directory the user browsed via the file chooser.
    * @return The current directory. Will be null while chooser is in use.
    */
   public File getWorkingDirectory() { return(busy ? null : currentDir); }
   
   /**
    * Get the file last selected by user in the file chooser It will typically be called immediately after a successful 
    * file operation to retrieve the path of the file just accessed.
    * @return File last selected in chooser. May or may not exist. Will be null while chooser is in use.
    */
   public File getSelectedFile() { return(busy ? null : currentFile); }
   
   /**
    * Raise the chooser dialog, let the user select a single existing FypML or Matlab figure file from the host file 
    * system, and prepare the graphic model rendered IAW the contents of the file selected. When the source file is a
    * Matlab FIG file, the graphic model returned is the FypML figure that matches the original Matlab figure to the
    * extent possible. 
    * 
    * @param c The GUI component which initiated the request. The chooser dialog's owner will be this component's 
    * top-level ancestor, and the dialog is positioned relative to that owner. If null, the dialog's owner will be an 
    * invisible frame window and the dialog is centered on the screen.
    * @param startingLoc If this argument is non-null and specifies an existing directory, the chooser dialog will be 
    * initialized to reflect the contents of that directory. If it specifies an existing file, the chooser will start up
    * in its parent directory. If argument is null or does not specify a valid file-system location, the chooser will
    * start up in the most recently used figure directory, or the user's home directory (in that order of precedence).
    * @return The graphic model extracted from the selected figure file, or null if the user cancelled or if chooser is
    * currently in use. To retrieve path of the figure's source file, call <code>getSelectedFile()</code>.
    */
   public FGraphicModel openFigure(JComponent c, File startingLoc)
   {
      if(busy) return(null);
      busy = true;
      
      // construct dialog that will house the chooser panel
      Container tla = (c == null) ? null : c.getTopLevelAncestor();
      owner = (tla instanceof Window) ? (Window) tla : null;
      chooserDlg = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
      
      // install chooser panel into dialog, configure, and show
      chooserDlg.setTitle("Browse for figure file");
      chooserDlg.add(this, BorderLayout.CENTER);
      configure(OpMode.OPENFIG, startingLoc, null);
      chooserDlg.pack();
      chooserDlg.setMinimumSize(MINDLGSZ);
      chooserDlg.setSize(lastDlgSize.width, lastDlgSize.height);
      chooserDlg.setLocationRelativeTo(owner);
      chooserDlg.setVisible(true);

      busy = false;
      if(wasCancelled || !(theObject instanceof FGraphicModel)) return(null);
      
      // if selected file exists, add it to workspace recent file list
      if(currentFile != null && currentFile.isFile()) workspace.addToRecentFiles(currentFile);
      
      FGraphicModel res = (FGraphicModel) theObject;
      theObject = null;
      return(res);
   }
   
   /**
    * Raise the chooser dialog and let the user select one or more existing <i>FypML</i> or <i>Matlab</i> figure files
    * from the host operating system. The dialog is configured to permit a multi-selection in the file listing for the
    * current directory; the preview pane displays the last figure added to the selection.
    * 
    * @param c The GUI component which initiated the request. The chooser dialog's owner will be this component's 
    * top-level ancestor, and the dialog is positioned relative to that owner. If null, the dialog's owner will be an 
    * invisible frame window and the dialog is centered on the screen.
    * @param startingLoc If this argument is non-null and specifies an existing directory, the chooser dialog will be 
    * initialized to reflect the contents of that directory. If it specifies an existing file, the chooser will start up
    * in its parent directory. If argument is null or does not specify a valid file-system location, the chooser will
    * start up in the most recently used figure directory, or the user's home directory (in that order of precedence).
    * @return The list of figure files selected. Returns null if the user cancelled or if the chooser is already in use.
    */
   public File[] selectFigures(JComponent c, File startingLoc)
   {
      if(busy) return(null);
      busy = true;
      
      // construct dialog that will house the chooser panel
      Container tla = (c == null) ? null : c.getTopLevelAncestor();
      owner = (tla instanceof Window) ? (Window) tla : null;
      chooserDlg = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
      
      // install chooser panel into dialog, configure, and show
      chooserDlg.setTitle("Select one or more figure files");
      chooserDlg.add(this, BorderLayout.CENTER);
      configure(OpMode.SELFIG, startingLoc, null);
      chooserDlg.pack();
      chooserDlg.setMinimumSize(MINDLGSZ);
      chooserDlg.setSize(lastDlgSize.width, lastDlgSize.height);
      chooserDlg.setLocationRelativeTo(owner);
      chooserDlg.setVisible(true);

      busy = false;
      if(wasCancelled) return(null);
      
      // if there's at least one selected file, add it to the workspace path cache, in case the selected directory is
      // not yet in that cache
      if(currentFile != null && currentFile.isFile()) workspace.addToWorkspace(currentFile);
      
      // get the list of selected files. If empty return null.
      int[] selected = fileTable.getSelectedRows();
      if(selected.length == 0) return(null);
      
      File[] files = new File[selected.length];
      for(int i=0; i<selected.length; i++) 
         files[i] = ((FilesTableModel) fileTable.getModel()).getFileAt(selected[i]);
      
      return(files);
   }
   
   /**
    * Raise the chooser dialog and let the user browse the host file system for <i>Figure Composer</i>-compatible data 
    * source files and retrieve a single data set from a selected source file. The chooser dialog initially displays any 
    * data source files in its most recently used data source directory, its most recently used figure directory, 
    * or the user's home directory (in that order of precedence). 
    * 
    * @param c The GUI component which initiated the request. The chooser dialog's owner will be this component's 
    * top-level ancestor, and the dialog is positioned relative to that owner. If null, the dialog's owner will be an 
    * invisible frame window and the dialog is centered on the screen.
    * @param reqFmts If non-null and not empty, this is a list of data set format types requested by the caller. While 
    * browsing, the chooser will hide from view any data set within a source file that is not in one of these formats. 
    * If null or empty, all data set types are shown.
    * @return The {@link DataSet} selected, or null if the user cancelled or an error occurred, or if the chooser
    * is currently in use. To retrieve source file path, call {@link #getSelectedFile()}.
    */
   public DataSet selectDataSet(JComponent c, Fmt[] reqFmts)
   {
      if(busy) return(null);
      busy = true;
      
      // construct dialog that will house the chooser panel
      Container tla = (c == null) ? null : c.getTopLevelAncestor();
      owner = (tla instanceof Window) ? (Window) tla : null;
      chooserDlg = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
            
      // install chooser panel into dialog, configure, and show
      chooserDlg.setTitle("Browse for dataset");
      chooserDlg.add(this, BorderLayout.CENTER);
      requestedDSFmts = (reqFmts != null && reqFmts.length > 0) ? reqFmts : null;
      configure(OpMode.OPENDS, null, null);
      chooserDlg.pack();
      chooserDlg.setMinimumSize(MINDLGSZ);
      chooserDlg.setSize(lastDlgSize.width, lastDlgSize.height);
      chooserDlg.setLocationRelativeTo(owner);
      chooserDlg.setVisible(true);

      busy = false;
      
      if(wasCancelled || !(theObject instanceof DataSet)) return(null);
      
      // if selected file exists, add it to workspace recent file list
      if(currentFile != null && currentFile.isFile()) workspace.addToRecentFiles(currentFile);
      
      DataSet res = (DataSet) theObject;
      theObject = null;
      return(res);
   }
   
   /**
    * Save a <i>Figure Composer</i> figure to a new or existing <i>FypML</i> figure definition file, optionally raising 
    * a chooser dialog to let the user specify the destination. Once the user confirms a valid selection, a background 
    * thread performs the file-save operation while the user interface is blocked by a modal message dialog. 
    * If the file operation fails, that dialog will display an error message to the user.
    * 
    * @param c The GUI component which initiated the request. The owner of the chooser and message dialogs will be this 
    * component's top-level ancestor, and the dialogs are positioned relative to that owner. If null, the owner will be 
    * an invisible frame window and the dialogs are centered on the screen.
    * @param fig The figure to be saved. Must not be null. If the save operation is successful, the figure's modified 
    * flag will be cleared.
    * @param f The destination file. If this file exists and has a valid extension for a <i>FypML</i> figure definition 
    * file, then the file will be overwritten <i>without</i> raising a chooser dialog -- unless the <b>saveAs</b> 
    * argument is set. Otherwise, if this argument specifies an existing parent directory, the chooser will start in 
    * that directory; else it will start in the most recently used figure directory or the user's ome directory (in that
    * order of precedence).
    * @param saveAs If set, a chooser dialog is raised to let the user specify a different destination file.
    * @param selOnly If set, the chooser dialog is raised merely to let user specify a destination file for the figure
    * <b>without actually saving the figure</b>. This option might be useful if you need to accumulate the destination
    * files for several figures that are then saved all at once via {@link #saveAllFigures(JComponent, List, List)}. If
    * this flag is set, then <i>f</i> simply specifies a starting point for the chooser.
    * @return True if successful, false if user cancelled operation or an error occurred during the file save. In the 
    * former case, call {@link #getSelectedFile()} to retrieve the path of the file just saved. In the latter case, 
    * the user will have already been informed of the problem. The method will return false immediately (and silently) 
    * if <b>fig</b> is null or if the chooser is currently in use.
    */
   @SuppressWarnings("ConstantValue")
   public boolean saveFigure(JComponent c, FGraphicModel fig, File f, boolean saveAs, boolean selOnly)
   {
      if(busy || fig == null) return(false);
      busy = true;
      
      // construct dialog that will house the chooser panel
      Container tla = (c == null) ? null : c.getTopLevelAncestor();
      owner = (tla instanceof Window) ? (Window) tla : null;
      
      if((!saveAs) && (!selOnly) && f != null && f.isFile() && OpMode.SAVEFIG.hasValidExtension(f))
      {
         currentFile = f;      // THIS IS CRITICAL, IN CASE getSelectedFile() is called after a successful save!
         save(f, fig, false);
      }
      else
      {
         selectFileOnly = selOnly;
         chooserDlg = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
         
         chooserDlg.setTitle(selOnly ? "Select destination for figure" : "Save figure to file");
         chooserDlg.add(this, BorderLayout.CENTER);
         chooserDlg.addComponentListener(this);  // so we can load preview canvas AFTER it is displayable
         configure(OpMode.SAVEFIG, f, fig);
         chooserDlg.pack();
         chooserDlg.setMinimumSize(MINDLGSZ);
         chooserDlg.setSize(lastDlgSize.width, lastDlgSize.height);
         chooserDlg.setLocationRelativeTo(owner);
         chooserDlg.setVisible(true);
         busy = false;
         
         theObject = null;
         if(wasCancelled) return(false);
      }
      
      busy = false;
      if(selOnly) return(!wasCancelled);
      
      if(fileOpOk)
      {
         fig.setModified(false);
         
         // if selected file exists, add it to workspace recent file list
         if(currentFile != null && currentFile.isFile()) workspace.addToRecentFiles(currentFile);
      }
      return(fileOpOk);
   }

   /**
    * Save a list of <i>FypML</i> figures.
    * 
    * <p>For each figure to be saved, a destination file must be specified for which the parent directory must exist.
    * If not, that figure will not be saved. <i>Any existing destination file is overwritten without prompting the 
    * user</i>. All of the file operations occur on a background thread while the user interface is blocked by a modal 
    * message dialog. The dialog's message is updated as each figure is saved to file. If any file operation fails, that
    * dialog will display an error message to the user and no further operations are attempted.</p>
    * 
    * <p>Method fails silently if the chooser is currently busy performing another operation.</p>
    * 
    * @param c The GUI component which initiated the request. The owner of the chooser and message dialogs will be this 
    * component's top-level ancestor, and the dialogs are positioned relative to that owner. If null, the owner will be 
    * an invisible frame window and the dialogs are centered on the screen.
    * @param figs List of figures to be saved. Any null elements are skipped.
    * @param figFiles List of file destinations. If list size does not match that of <i>figs</i>.
    * argument, the method fails silently. A valid file destination must be specified for each figure, or that figure
    * will not be saved.
    * @return Number of figures successfully saved.
    */
   public int saveAllFigures(JComponent c, List<FGraphicModel> figs, List<File> figFiles)
   {
      if(busy || figs == null || figFiles == null || figs.size() != figFiles.size() || figs.isEmpty())
         return(0);
      busy = true;
      
      // get the window owner for our modal progress dialog
      Container tla = (c == null) ? null : c.getTopLevelAncestor();
      owner = (tla instanceof Window) ? (Window) tla : null;

      // prepare the list of jobs to be performed, skipping over any bad entries in the input args
      List<SaveJob> jobs = new ArrayList<>();
      for(int i=0; i<figs.size(); i++) if(figs.get(i) != null)
      {
         File dst = figFiles.get(i);
         if(dst != null && (dst.isFile() || (dst.getParentFile() != null && dst.getParentFile().isDirectory())))
         {
            dst = FCFileType.FYP.fixExtension(dst);
            jobs.add(new SaveJob(dst, figs.get(i)));
         }
      }
      
      if(jobs.isEmpty()) return(0);
      
      int nOK = saveAll(jobs);
      busy = false;
      return(nOK);
   }
   
   /**
    * Export a <i>Figure Composer</i> figure to one of four supported external file formats: Postscript (an encapsulated 
    * PS file), Portable Document Format (PDF), JPEG image, or Portable Network Graphics (PNG) image. A chooser dialog 
    * is raised to let the user select both the destination file and the file format. Once the user confirms a valid 
    * selection, a background thread performs the export while the user interface is blocked by a modal message dialog. 
    * If the file operation fails, that dialog will display an error message to the user.
    * 
    * <p><b>NOTE 1: Image file resolution.</b> When exported to either of the two supported image file formats, the
    * figure is rendered to an offscreen bitmap at a fixed resolution of 300dpi before writing it to the file, and the
    * image resolution meta-data is included in the file as well (otherwise, external programs will assume 72dpi). See 
    * {@link FileTaskWorker#saveOrExportFigure(SaveJob)}.</p>
    * 
    * <p><b>NOTE 2: Fonts in PDF files.</b> When exported to PDF, any fonts needed to render text elements in the figure
    * will be embedded (as a subsetted font) in the file. This should be sufficient to view -- but not edit -- the PDF
    * on any host. However, if a given font cannot be embedded because of license restrictions or because the font
    * definition file is not supported by the underlying PDF library, then one of the built-in standard Type 1 fonts 
    * will be substituted for the problematic font. This, of course, means that the exported figure will not have the
    * same appearance as it does on screen on the machine on which it was created.</p>
    * 
    * @param c The GUI component which initiated the request. The owner of the chooser and message dialogs will be this 
    * component's top-level ancestor, and the dialogs are positioned relative to that owner. If null, the owner will be 
    * an invisible frame window and the dialogs are centered on the screen.
    * @param fig The figure to be exported. Must not be null.
    * @param f If this file's parent directory exists, it serves as a starting location for the chooser -- even if the 
    * file itself does not exist; the file's extension is corrected if necessary. Otherwise, the chooser starts out in 
    * the MRU figure directory or, if there is none, the user's home directory. In these latter cases, a default 
    * filename of "exportfig.ext" is supplied, using the appropriate extension.
    * @return True if successful, false if user cancelled operation or an error occurred during the export. In the 
    * former case, call {@link #getSelectedFile()} to retrieve the path of the file just saved. In the latter case, 
    * the user will have already been informed of the problem. The method will return false immediately (and silently) 
    * if <b>fig</b> is null or the chooser is currently in use.
    */
   public boolean exportFigure(JComponent c, FGraphicModel fig, File f)
   {
      if(busy || fig == null) return(false);
      busy = true;
      
      // construct dialog that will house the chooser panel
      Container tla = (c == null) ? null : c.getTopLevelAncestor();
      owner = (tla instanceof Window) ? (Window) tla : null;
      chooserDlg = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);

      // install chooser panel into dialog, configure, and show
      chooserDlg.setTitle("Export");
      chooserDlg.add(this, BorderLayout.CENTER);
      chooserDlg.addComponentListener(this);  // so we can load preview canvas AFTER it is displayable
      configure(OpMode.EXPFIG, f, fig);
      chooserDlg.pack();
      chooserDlg.setMinimumSize(MINDLGSZ);
      chooserDlg.setSize(lastDlgSize.width, lastDlgSize.height);
      chooserDlg.setLocationRelativeTo(owner);
      chooserDlg.setVisible(true);

      busy = false;
      theObject = null;
      boolean ok = (!wasCancelled) && fileOpOk;
      
      if(ok && currentFile != null && currentFile.isFile()) workspace.addToRecentFiles(currentFile);
      
      return(ok);
   }
   
   /**
    * Export a list of <i>Figure Composer</i> figures to one of four supported external file formats: Postscript (an 
    * encapsulated PS file), Portable Document Format (PDF), JPEG image, or Portable Network Graphics (PNG) image. 
    * 
    * <p>For each figure model to be exported, a destination file must be specified for which the parent directory must
    * exist. If not, that figure will not be exported. If so, the figure will be exported in the specified target format
    * to that file location; the file extension is corrected if necessary. <i>An existing file is overwritten without 
    * prompting the user</i>. All of the file export operations occur on a background thread while the user interface is
    * blocked by a modal message dialog. If any file operation fails, that dialog will display an error message to the 
    * user and no further exports are attempted.</p>
    * 
    * <p>Method fails silently if the chooser is currently busy performing another operation.</p>
    * 
    * @param c The GUI component which initiated the request. The owner of the chooser and message dialogs will be this 
    * component's top-level ancestor, and the dialogs are positioned relative to that owner. If null, the owner will be 
    * an invisible frame window and the dialogs are centered on the screen.
    * @param figs List of figures to be exported. Any null elements are skipped.
    * @param figFiles List of file destinations. If length of this array does not match that of the <b>figs</b> 
    * argument, the method fails silently. A valid file destination must be specified for each figure, or that figure
    * will not be exported.
    * @param exportType The target export file format. Method fails silently if this is null or not one of the supported 
    * external file formats listed above.
    * @return Number of figures successfully exported.
    */
   public int exportAllFigures(JComponent c, FGraphicModel[] figs, File[] figFiles, FCFileType exportType)
   {
      if(busy || figs == null || figFiles == null || figs.length != figFiles.length)
         return(0);
      if(!FCFileType.isFigureExportFileType(exportType))
         return(0);
      busy = true;
      
      // get the window owner for our modal progress dialog
      Container tla = (c == null) ? null : c.getTopLevelAncestor();
      owner = (tla instanceof Window) ? (Window) tla : null;

      // prepare the list of export jobs to be performed, fixing file extensions as needed and skipping over any bad
      // entries in the input args
      List<SaveJob> jobs = new ArrayList<>();
      for(int i=0; i<figs.length; i++) if(figs[i] != null)
      {
         File dst = figFiles[i];
         if(dst != null && (dst.isFile() || (dst.getParentFile() != null && dst.getParentFile().isDirectory())))
         {
            dst = exportType.fixExtension(dst);
            jobs.add(new SaveJob(dst, figs[i]));
         }
      }
      
      if(jobs.isEmpty()) return(0);
      
      int nOK = saveAll(jobs);
      busy = false;
      return(nOK);
   }
   
   /**
    * Save a <i>Figure Composer</i>-compatible data set to a new or existing data source file. A chooser dialog is 
    * raised, displaying any data source files in its most recently used data set source directory, its most recently 
    * used figure directory, or the user's home directory (in that order of precedence). The user can browse the file 
    * system, select either binary or annotated plain-text source files, and choose to overwrite an existing file or 
    * append the new data set to it. If the user selects an existing data source file, the dialog will list its current 
    * contents and prevent the user from specifying a data set ID identical to one that is already in the file. Once the
    * user confirms a valid selection, a background thread performs the file-save operation while the user interface is 
    * blocked by a modal message dialog. If the file operation fails, that dialog displays an error message to the user.
    * 
    * @param c The GUI component which initiated the request. The owner of the chooser and message dialogs will be this 
    * component's top-level ancestor, and the dialogs are positioned relative to that owner. If null, the owner will be 
    * an invisible frame window and the dialogs are centered on the screen.
    * @param ds The data set to be saved. Must not be null.
    * @return True if successful, false if user cancelled operation or an error occurred during the file save. In the 
    * former case, call {@link #getSelectedFile()} to retrieve the path of the file just saved. In the latter case, 
    * the user will have already been informed of the problem. The method will return false immediately (and silently) 
    * if <b>ds</b> is null or the chooser is currently in use.
    */
   public boolean saveDataSet(JComponent c, DataSet ds)
   {
      if(busy || ds == null) return(false);
      busy = true;
      
      // construct dialog that will house the chooser panel
      Container tla = (c == null) ? null : c.getTopLevelAncestor();
      owner = (tla instanceof Window) ? (Window) tla : null;
      chooserDlg = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
      
      // install chooser panel into dialog, configure, and show
      chooserDlg.setTitle("Save dataset to file");
      chooserDlg.add(this, BorderLayout.CENTER);
      chooserDlg.addComponentListener(this);  // so we can load preview canvas AFTER it is displayable
      configure(OpMode.SAVEDS, null, ds);
      chooserDlg.pack();
      chooserDlg.setMinimumSize(MINDLGSZ);
      chooserDlg.setSize(lastDlgSize.width, lastDlgSize.height);
      chooserDlg.setLocationRelativeTo(owner);
      chooserDlg.setVisible(true);

      busy = false;
      theObject = null;
      boolean ok = (!wasCancelled) && fileOpOk;
      
      if(ok && currentFile != null && currentFile.isFile()) workspace.addToRecentFiles(currentFile);

      return(ok);
   }

   /**
    * Raise the chooser dialog, let the user select a single image source file from the host file system, and return
    * the image loaded from the selected file. The supported image file types are JPEG and PNG.
    * 
    * @param c The GUI component which initiated the request. The chooser dialog's owner will be this component's 
    * top-level ancestor, and the dialog is positioned relative to that owner. If null, the dialog's owner will be an 
    * invisible frame window and the dialog is centered on the screen.
    * @param startingLoc If this argument is non-null and specifies an existing directory, the chooser dialog will be 
    * initialized to reflect the contents of that directory. If it specifies an existing file, the chooser will start up
    * in its parent directory. If argument is null or does not specify a valid file-system location, the chooser will
    * start up in the most recently used figure directory, or the user's home directory (in that order of precedence).
    * @return The image loaded from the user-selected source file, or null if the user cancelled or if chooser is
    * currently in use. To retrieve path of the image source file, call {@link #getSelectedFile()}.
    */
   public BufferedImage loadImage(JComponent c, File startingLoc)
   {
      if(busy) return(null);
      busy = true;
      
      // construct dialog that will house the chooser panel
      Container tla = (c == null) ? null : c.getTopLevelAncestor();
      owner = (tla instanceof Window) ? (Window) tla : null;
      chooserDlg = new JDialog(owner, Dialog.ModalityType.APPLICATION_MODAL);
      
      // install chooser panel into dialog, configure, and show
      chooserDlg.setTitle("Load an image from file");
      chooserDlg.add(this, BorderLayout.CENTER);
      configure(OpMode.OPENIMG, startingLoc, null);
      chooserDlg.pack();
      chooserDlg.setMinimumSize(MINDLGSZ);
      chooserDlg.setSize(lastDlgSize.width, lastDlgSize.height);
      chooserDlg.setLocationRelativeTo(owner);
      chooserDlg.setVisible(true);

      busy = false;
      if(wasCancelled || !(theObject instanceof BufferedImage)) return(null);
      
      BufferedImage res = (BufferedImage) theObject;
      theObject = null;
      return(res);
   }
   
   /**
    * The chooser panel is installed as a component listener on the parent dialog. Only when the dialog is visible 
    * will the embedded chooser's preview canvas have the necessary graphics resources to render a preview. Thus, we
    * need to delay loading the preview canvas until that time. This is only critical in the "save" modes, since the
    * "preview" is just the object being saved, so it need be loaded just once.
    */
   @Override public void componentShown(ComponentEvent e) { updatePreview(); }
   @Override public void componentResized(ComponentEvent e) {}
   @Override public void componentMoved(ComponentEvent e) {}
   @Override public void componentHidden(ComponentEvent e) {}
   
   /** Private constructor for singleton class. */
   private FCChooser() 
   {  
      chooserDlg = null;
      owner = null;
      busy = false;
      workspace = FCWorkspace.getInstance();
      fsView = FileSystemView.getFileSystemView();

      createComponents();
      layoutComponents();

      currentDir = null;
      currentFile = null;
   }
   
   /** The current dialog container for the chooser. Recreated each time the chooser is raised. */
   private JDialog chooserDlg;
   
   /** Minimum size for the file chooser dialog container. */
   private final static Dimension MINDLGSZ = new Dimension(600,400);
   
   /** Size of the file chooser's dialog container the last time it was shown. */
   private Dimension lastDlgSize = new Dimension(MINDLGSZ);
   
   /** Owner window (either a dialog or frame window, or null) for the chooser or progress dialogs. */
   private Window owner;
   
   /** Flag set while the singleton chooser is in use. */
   private transient boolean busy;
   
   /** Current user's <em>Figure Composer</em> workspace. */
   private final FCWorkspace workspace;
   
   /** View of the host's file system. */
   private final FileSystemView fsView;
   
   /** The chooser's current directory (or, the directory it was displaying when it was last raised). */
   private File currentDir = null;
   
   /** 
    * Index of entry in the directory selection combo that represents the file system root for the selected directory.
    * Any entries AFTER this one in the combo box are most recently used directories, which are rendered differently 
    * in the dropdown list.
    */
   private int indexOfRoot = -1;
   
   /** 
    * The chooser's currently selected file (may or may not exist). If browsing an empty directory in open mode, this
    * will be null, and the confirm button will be disabled.
    */
   private File currentFile = null;
   
   /** If the currently selected file exists and has data sets, this wrapper provides access to those sets. */
   private IDataSrc dataSource = null;
   
   /** Flag set if the user cancelled the operation the last time the chooser was extinguished. */
   private boolean wasCancelled = false;
   
   /** The chooser's current operational mode. */
   private OpMode mode = null;
   
   /** 
    * For {@link OpMode#SAVEFIG} operation only, if this flag is set, then the figure is NOT actually saved to the
    * selected file. The operation merely chooses a destination for the figure.
    */
   private boolean selectFileOnly = false;
   
   /** 
    * For {@link OpMode#OPENDS} operation only, this is the list of data set formats requested. Any data sets in the 
    * currently selected file that do not conform to one of these formats are not shown. If null, then all data sets 
    * are shown.
    */
   private Fmt[] requestedDSFmts = null;
   
   /**
    * The object to be saved in a "file-save" operation or retrieved in a "file-open" operation. Typically, it will be 
    * either a <i>FypML</i> figure or data set. For a "file-save" it is specified when the chooser dialog is raised; 
    * else, it is null until the dialog is extinguished and the background loader thread has finished loading the object
    * from the file selected by the user. For the {@link OpMode#OPENIMG} operation, the object will be an instance of
    * {@link BufferedImage}. Not used for the {@link OpMode#SELFIG} operation.
    */
   private Object theObject = null;
   
   /** Background thread which prepares model and loads it into preview canvas. */
   private FCFileLoader fileLoader = null;
   
   /**
    * This simple rendered model is installed in preview canvas while loading the real preview, or to display an error
    * message, or to indicate no selection.
    */
   private final CenteredMessageModel placeHolderPreview = new CenteredMessageModel("");
   
   /** Flag set while reloading dialog controls -- so we know to ignore events thrown while doing so. */
   private transient boolean isReloading = false;
   
   /** 
    * Flag set while in document listener responding to update in the <i>filename</i> field. That field's content must
    * never be altered while this flag is set.
    */
   private transient boolean isFilenameUpdate = false;
   
   /** 
    * Maximum number of MRU directories that can appear in the directory path combo AFTER the list item representing
    * the file system root of the currently selected directory. 
    */
   private final static int MAX_MRUDIRS = 5;


   //
   // Widgets
   //
   
   /** Combo box displays currently selected directory; drop down shows entries in path back to a file system root. */
   private JComboBox<File> dirCombo = null;
   
   /** Filtered list of all files and directories under the currently selected directory, in tabular form. */
   private JTable fileTable = null;
   
   /** Combo box displays/selects the file type. */
   private JComboBox<FCFileType> fileTypeCombo = null;
   
   /** Filename field. */
   private JTextField fileNameField = null;

   /** Data set ID field. */
   private JTextField dsIDField = null;
   
   /** If checked, data set saved will replace an existing data set with the same ID in the source file (if any). */
   private JCheckBox replaceChk = null;
   
   /** List of data sets found in the currently selected file. */
   private JList<DataSetInfo> dataSetList = null; 
   
   /** Panel holds data set-specific stuff -- so it can be hidden when not opening/saving a data source file. */
   private JPanel dataSetPanel = null;
   
   /** Panel visible only when saving a data set -- for changing the data set ID. */
   private JPanel dsIDPanel = null;

    /** The canvas on which a figure model or a single dataset is rendered for preview purposes. */
   private Graph2DViewer previewer = null;

   /** 
    * For open/save figure modes only, text area shows the note/description for figure currently previewed. If 
    * preview fails to load in open mode, an error description appears here.
    */
   private JTextArea figNoteTextArea = null;
   /** The scroll pane for the figure note text area. Visible only in the open/save figure modes. */
   private JScrollPane noteScroller = null;
   
   /** 
    * Displays full path to the object selected. Read-only. Whenever the current selection is invalid, this displays a 
    * short description of the problem instead.
    */
   private JTextField selectedField = null;
   
   /** This button confirms the current selection and extinguishes the chooser dialog. */
   private JButton okBtn = null;
   
   /** This button cancels the operation entirely and extinguishes the chooser dialog. */
   private JButton cancelBtn = null;

   /** 
    * Helper method creates all widgets in the chooser dialog panel. The panel is created once and installed in a
    * container dialog whenever the chooser must be raised. 
    */
   private void createComponents()
   {
      dirCombo = new JComboBox<>();
      dirCombo.setRenderer(new DirPathComboRenderer());
      dirCombo.setEditable(false);
      dirCombo.addActionListener(this);
      
      fileTable = new JTable(new FilesTableModel());
      fileTable.setRowSelectionAllowed(true);
      fileTable.setColumnSelectionAllowed(false);
      fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      fileTable.setShowHorizontalLines(false);
      fileTable.getSelectionModel().addListSelectionListener(this);
      FilesTableRenderer renderer = new FilesTableRenderer();
      fileTable.setDefaultRenderer(File.class, renderer);
      fileTable.setDefaultRenderer(Date.class, renderer);
      fileTable.addMouseListener(this);
      
      fileTypeCombo = new JComboBox<>();
      fileTypeCombo.setEditable(false);
      fileTypeCombo.addActionListener(this);

      fileNameField = new JTextField();
      fileNameField.getDocument().addDocumentListener(this);
      fileNameField.addActionListener(this);
      
      dsIDField = new JTextField("set", 10);
      ((AbstractDocument)dsIDField.getDocument()).setDocumentFilter(new DataSetIDFilter());
      dsIDField.setToolTipText(
               "Enter dataset ID (1-" + DataSet.MAXIDLEN + " alphanumeric or certain puncuation chars)");
      dsIDField.getDocument().addDocumentListener(this);
      
      replaceChk = new JCheckBox("Allow replace?");
      replaceChk.setSelected(false);
      replaceChk.setToolTipText("<html>If checked and dataset ID matches that of an existing dataset in the<br>" +
            "source file, the existing dataset is replaced.</html>");
      replaceChk.addActionListener(this);
      
      dataSetList = new JList<>();
      dataSetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      dataSetList.setLayoutOrientation(JList.VERTICAL_WRAP);
      dataSetList.setCellRenderer(new DataSetInfoLCR());
      dataSetList.setVisibleRowCount(4);
      dataSetList.addListSelectionListener(this);
      
      previewer = new Graph2DViewer(false);
      
      figNoteTextArea = new JTextArea();
      figNoteTextArea.setEditable(false);
      figNoteTextArea.setTabSize(6);
      figNoteTextArea.setLineWrap(true);
      figNoteTextArea.setWrapStyleWord(true);
      figNoteTextArea.setRows(6);
      noteScroller = new JScrollPane(figNoteTextArea);

      selectedField = new JTextField();
      selectedField.setEditable(false);
      
      okBtn = new JButton("Open");
      okBtn.addActionListener(this);
      cancelBtn = new JButton("Cancel");
      cancelBtn.addActionListener(this);
   }
 
   /** Helper method lays out all widgets in the chooser dialog panel. */
   private void layoutComponents()
   {
      int gap = FCIcons.UIGAPSZ;

      JPanel boxPanel = new JPanel();
      SpringLayout layout = new SpringLayout();
      boxPanel.setLayout(layout);
      
      JLabel fileLbl = new JLabel("File:");
      boxPanel.add(fileLbl);
      boxPanel.add(fileNameField);
      
      JLabel dirLbl = new JLabel("In:");
      boxPanel.add(dirLbl);
      boxPanel.add(dirCombo);
      
      JScrollPane fileTableScroller = new JScrollPane(fileTable);
      boxPanel.add(fileTableScroller);
      
      JLabel fmtLbl = new JLabel("File Format:");
      boxPanel.add(fmtLbl);
      boxPanel.add(fileTypeCombo);
      
      layout.putConstraint(SpringLayout.WEST, fileLbl, 0, SpringLayout.WEST, boxPanel);
      layout.putConstraint(SpringLayout.WEST, fileNameField, gap, SpringLayout.EAST, fileLbl);
      layout.putConstraint(SpringLayout.EAST, fileNameField, 0, SpringLayout.EAST, boxPanel);
      layout.putConstraint(SpringLayout.WEST, dirLbl, 0, SpringLayout.WEST, boxPanel);
      layout.putConstraint(SpringLayout.WEST, dirCombo, gap, SpringLayout.EAST, dirLbl);
      layout.putConstraint(SpringLayout.EAST, dirCombo, 0, SpringLayout.EAST, boxPanel);
      layout.putConstraint(SpringLayout.WEST, fileTableScroller, 0, SpringLayout.WEST, boxPanel);
      layout.putConstraint(SpringLayout.EAST, fileTableScroller, 0, SpringLayout.EAST, boxPanel);
      layout.putConstraint(SpringLayout.WEST, fmtLbl, 0, SpringLayout.WEST, boxPanel);
      layout.putConstraint(SpringLayout.WEST, fileTypeCombo, gap, SpringLayout.EAST, fmtLbl);
      layout.putConstraint(SpringLayout.EAST, fileTypeCombo, 0, SpringLayout.EAST, boxPanel);
      
      String vc = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(SpringLayout.NORTH, fileNameField, 0, SpringLayout.NORTH, boxPanel);
      layout.putConstraint(SpringLayout.NORTH, dirCombo, gap*2, SpringLayout.SOUTH, fileNameField);
      layout.putConstraint(SpringLayout.NORTH, fileTableScroller, gap, SpringLayout.SOUTH, dirCombo);
      layout.putConstraint(SpringLayout.SOUTH, fileTableScroller, -gap, SpringLayout.NORTH, fileTypeCombo);
      layout.putConstraint(SpringLayout.SOUTH, fileTypeCombo, 0, SpringLayout.SOUTH, boxPanel);
      
      layout.putConstraint(vc, fileLbl, 0, vc, fileNameField);
      layout.putConstraint(vc, dirLbl, 0, vc, dirCombo);
      layout.putConstraint(vc, fmtLbl, 0, vc, fileTypeCombo);

      dataSetPanel = new JPanel(new BorderLayout());
      dataSetPanel.setBorder(BorderFactory.createCompoundBorder(
               BorderFactory.createEmptyBorder(gap*2, 0, 0, 0),
               BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder("Dataset"),
                        BorderFactory.createEmptyBorder(gap, gap, gap, gap))));
      
      JPanel p = new JPanel();
      p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
      p.add(new JLabel("ID:"));
      p.add(Box.createHorizontalStrut(gap));
      p.add(dsIDField);
      p.add(Box.createHorizontalStrut(gap*2));
      p.add(replaceChk);
      
      dsIDPanel = new JPanel(new BorderLayout());
      dsIDPanel.add(p, BorderLayout.WEST);
      dsIDPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, gap, 0));
      dataSetPanel.add(dsIDPanel, BorderLayout.NORTH);
      
      int prefHt = 75;
      Component c = dataSetList.getCellRenderer().getListCellRendererComponent(
            dataSetList, DataSetInfo.createDataSetInfo("MM", Fmt.PTSET, 0, 2, null), 0, true, false);
      if(c != null) prefHt = c.getPreferredSize().height * (dataSetList.getVisibleRowCount() + 1);
      JScrollPane scroller = new JScrollPane(dataSetList);
      prefHt += scroller.getHorizontalScrollBar().getPreferredSize().height;
      scroller.setPreferredSize(new Dimension(300, prefHt));
      dataSetPanel.add(scroller, BorderLayout.CENTER);

      JPanel browsePanel = new JPanel(new BorderLayout());
      browsePanel.add(boxPanel, BorderLayout.CENTER);
      browsePanel.add(dataSetPanel, BorderLayout.SOUTH);
      browsePanel.setBorder(BorderFactory.createEmptyBorder(gap*2, gap*2, gap*2, gap*2));

      JPanel previewPanel = new JPanel(new BorderLayout());
      previewPanel.add(previewer, BorderLayout.CENTER);
      previewPanel.add(noteScroller, BorderLayout.SOUTH);
      previewPanel.setBorder(BorderFactory.createCompoundBorder(
               BorderFactory.createEmptyBorder(gap*2, 0, gap*2, gap*2),
               BorderFactory.createCompoundBorder(
                        BorderFactory.createEtchedBorder(), 
                        BorderFactory.createEmptyBorder(gap, gap, gap, gap))));


      p = new JPanel();
      p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
      p.add(Box.createHorizontalStrut(gap*4));
      p.add(okBtn);
      p.add(Box.createHorizontalStrut(gap*4));
      p.add(cancelBtn);
       
      JPanel btnPanel = new JPanel(new BorderLayout());
      btnPanel.add(selectedField, BorderLayout.CENTER);
      btnPanel.add(p, BorderLayout.EAST);
      btnPanel.setBorder(BorderFactory.createEmptyBorder(0, gap*2, gap*2, gap*2));
      
      // using GridBagLayout so that right-hand browse panel and left-hand preview panel share the extra horizontal
      // space in the ratio 1:3. 
      // HACK: had to set minimum and preferred sizes on the browse panel to get things to work reasonably well. In 
      // particular, the large preferred height was necessary or the browse panel would get reset to ~300px when the 
      // dialog exceeded a certain height, which seems like a bug to me.
      browsePanel.setMinimumSize(new Dimension(200,400));
      browsePanel.setPreferredSize(new Dimension(300,10000));

      setLayout(new GridBagLayout());
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.fill = GridBagConstraints.BOTH;
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.weightx = 0.25;
      gbc.weighty = 1.0;
      add(browsePanel, gbc);
      
      gbc.fill = GridBagConstraints.BOTH;
      gbc.gridx = 1;
      gbc.weightx = 0.75;
      add(previewPanel, gbc);
      
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridx = 0;
      gbc.gridy = 1;
      gbc.gridwidth = 2;
      gbc.weightx = 1.0;
      gbc.weighty = 0;
      add(btnPanel, gbc);
   }
   
   /** 
    * Helper method configures the controls on the chooser panel for the specified operational mode and makes an initial
    * file selection. It must be called BEFORE re-packing and raising the dialog in which chooser panel is installed.
    * @param m The desired operational mode (if null, {@link OpMode#OPENFIG} is assumed).
    * @param startingFile This argument is used to set the chooser's initial directory and selected file. If the 
    * argument itself is an existing directory, then that becomes the chooser's current directory and the selected
    * file will have a base name of "Untitled" plus an extension that's valid in the current op mode. Else, if the
    * parent directory exists, that becomes the current directory and the argument itself is the initially selected 
    * file, with its extension corrected if necessary (the file need not exist). Otherwise, the chooser will initially
    * select the most recently used figure or data set source file, as appropriate to the operational mode. If that's
    * not possible, it will start in the user's home directory, with a default filename as described above. Optional -- 
    * may be null.
    * @param saveObj Ignored if entering one of the "file open" modes. Otherwise, this must be consistent with the 
    * specified "file save" mode: a {@link DataSet} if saving a data set, or a {@link FGraphicModel} otherwise. 
    */
   private void configure(OpMode m, File startingFile, Object saveObj)
   {
      // the user can change the screen DPI, so we set it each time the dialog is raised
      previewer.setResolution(workspace.getScreenDPI());
      
      mode = (m == null) ? OpMode.OPENFIG : m;
      wasCancelled = false;

      boolean isOpenMode = mode.isOpenMode();
      boolean isDataMode = mode.isDataMode();
      
      theObject = isOpenMode ? null : saveObj;
      
      isReloading = true;
      try
      {
         currentDir = null;
         currentFile = null;
         
         // if a starting file location is specified, use it to set the chooser's current directory and file to the
         // extent possible. Otherwise, the chooser starts with the focus on the most recently used figure file or
         // dataset source file in the user's workspace (as appropriate to the operation performed). If no such MRU file
         // exists, switch to the user's home directory.
         if(startingFile != null)
         {
            if(startingFile.isDirectory())
            {
               currentDir = startingFile;
               currentFile = new File(currentDir, "Untitled");
            }
            else if(startingFile.getParentFile().isDirectory())
            {
               currentDir = startingFile.getParentFile();
               currentFile = startingFile;
            }
         }

         if(currentDir == null || !currentDir.isDirectory())
         {
            if(mode != OpMode.OPENIMG)
            {
               boolean wantData = (mode == OpMode.OPENDS || mode == OpMode.SAVEDS);
               File f = workspace.getMostRecentFile(wantData);
               if(f == null && wantData) f = workspace.getMostRecentFile(false);
               if(f != null)
               {
                  currentDir = f.getParentFile();
                  currentFile = f;
               }
            }
         }
         
         if(currentDir == null || !currentDir.isDirectory()) 
         {
            currentDir = fsView.getDefaultDirectory();
            currentFile = new File(currentDir, "Untitled");
         }
         

         // load file filter combo with the valid file types for current operational mode. Initially select file type
         // that matches the currently selected file, if possible.
         int iTypeSel = -1;
         fileTypeCombo.removeAllItems();
         for(int i=0; i<mode.validDNFileTypes.length; i++) 
         {
            fileTypeCombo.addItem(mode.validDNFileTypes[i]);
            if(iTypeSel < 0 && currentFile != null && currentFile.isFile() && 
                  mode.validDNFileTypes[i].hasValidExtension(currentFile))
               iTypeSel = i;
         }
         fileTypeCombo.setSelectedIndex(Math.max(iTypeSel, 0));
         
         // in SELFIGS mode only, multi-selection in the file table is supported. Set the list selection mode before
         // we reload the file table.
         fileTable.setSelectionMode(mode == OpMode.SELFIG ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : 
            ListSelectionModel.SINGLE_SELECTION);
         
         // update the chooser -- note that we force a full reload of the dialog controls here!
         changeCurrentDirectory(currentDir, true);
         
         // visibility and configuration of selected controls varies with operational mode
         fileNameField.setEditable(!isOpenMode);
         dataSetPanel.setVisible(isDataMode);
         dataSetList.setEnabled(isOpenMode);
         dsIDPanel.setVisible(mode == OpMode.SAVEDS);
         
         noteScroller.setVisible(mode == OpMode.OPENFIG || mode == OpMode.SAVEFIG);
         
         String okTxt = "Open";
         if(mode == OpMode.SELFIG) okTxt = "Select"; 
         else if(mode == OpMode.EXPFIG) okTxt = "Export";
         else if(!isOpenMode) okTxt = (mode == OpMode.SAVEFIG && selectFileOnly) ? "Select" : "Save";
         okBtn.setText(okTxt);
      }
      finally { isReloading = false; }
   }
    
   /** 
    * Change to the specified directory and update controls on the chooser panel IAW the current operational mode.
    * @param tgtDir The desired directory. No action taken if this is identical to the current directory -- unless the 
    * <i>force</i> argument is set. No action taken if it is null or not a directory.
    * @param force If this flag is set, the chooser is updated even if the desired directory is identical to the 
    * current one. It is set only while reloading the chooser panel prior to raising the containing dialog.
    */
   private void changeCurrentDirectory(File tgtDir, boolean force)
   {
      if(tgtDir == null || !tgtDir.isDirectory() || (currentDir.equals(tgtDir) && !force)) return;

      currentDir = tgtDir;
      
      boolean wasReloading = isReloading;
      if(!wasReloading) isReloading = true;
      try
      {
         // reload directory path combo box and file list IAW current directory
         indexOfRoot = -1;
         dirCombo.removeAllItems();
         dirCombo.addItem(currentDir);
         File dir = currentDir;
         while(dir != null && !fsView.isRoot(dir))
         {
            dir = fsView.getParentDirectory(dir);
            dirCombo.addItem(dir);
         }
         dirCombo.setSelectedItem(currentDir);
         indexOfRoot = dirCombo.getItemCount() - 1;
         
         // if there are any directories in the workspace path cache containing relevant content for the op mode, add up
         // to MAX_MRUDIRS to the directory path combo. These will be rendered somewhat differently, with a line 
         // separating the file system root list item from the first MRU directory item. Not all operational modes
         // have relevant MRU directories...
         if(mode != OpMode.OPENIMG)
         {
            List<File> mruDirs = workspace.getRecentDirectories(mode==OpMode.OPENDS || mode==OpMode.SAVEDS);
            for(int i=0; i<mruDirs.size() && i<MAX_MRUDIRS; i++) dirCombo.addItem(mruDirs.get(i));
         }
         
         FCFileType currDNFileType = (FCFileType) fileTypeCombo.getSelectedItem();
         if(currDNFileType == null) currDNFileType = FCFileType.FYP;
         FilesTableModel ftm = (FilesTableModel) fileTable.getModel();
         ftm.init(currentDir, currDNFileType);

         // update currently selected file. In a "file save" mode, this need not exist. In a "file open" mode, we try 
         // to select an existing file, but it's possible that the current directory has no files of the required type.
         File select;
         if(!mode.isOpenMode())
         {
            select = new File(currentDir, (currentFile != null) ? currentFile.getName() : "Untitled");
            select = currDNFileType.fixExtension(select);
         }
         else if(currentFile == null || !currentFile.isFile() || !currentFile.getParentFile().equals(currentDir) ||
                  !currDNFileType.hasValidExtension(currentFile))
         {
            select = ftm.getMostRecentFile();
         }
         else select = currentFile;
         
         // when we change directory but there's no file selected before or after, we need to update selection field
         // to reflect the new directory path
         if(select == null && currentFile == null && !force) updateSelection();
         
         changeCurrentFile(select, force);
      }
      finally { if(!wasReloading) isReloading = false; }
   }

   /** 
    * Select the specified file if possible and update controls on the chooser panel IAW the current operational mode.
    * @param tgtFile The desired file. If this is a directory, it becomes the chooser's current directory, and the 
    * current file is updated IAW the operational mode and the contents of that directory. If it is null, the current 
    * selection becomes undefined. If file exists but is not in the current directory, no action is taken. If desired 
    * file is identical to the currently selected file (including null), no action is taken unless the <i>force</i> 
    * argument is set.
    * @param force If this flag is set, the chooser panel is updated even if the desired file is identical to the 
    * currently selected file. It is set only while reloading the chooser panel prior to raising the containing dialog.
    */
   private void changeCurrentFile(File tgtFile, boolean force)
   {
      if(tgtFile != null && tgtFile.isDirectory())
      {
         changeCurrentDirectory(tgtFile, force);
         return;
      }
      
      if((!force) && (Objects.equals(currentFile, tgtFile))) return;
      if(tgtFile != null && tgtFile.isFile() && !currentDir.equals(tgtFile.getParentFile())) return;
      
      currentFile = tgtFile;
      
      boolean wasReloading = isReloading;
      if(!wasReloading) isReloading = true;
      try
      {
         int row = ((FilesTableModel)fileTable.getModel()).findRowContaining(currentFile);
         if(row >= 0) fileTable.setRowSelectionInterval(row, row);
         else fileTable.getSelectionModel().clearSelection();
         
         // NOTE: if called because user is changing filename, we must not alter the fileNameField itself!
         if(!isFilenameUpdate) fileNameField.setText((currentFile != null) ? currentFile.getName() : "");
         
         // in dataset operations, stuff the dataset-related controls appropriately. If selected file exists, we get
         // IDs of all existing dataset in the source file.
         if(mode.isDataMode())
         {
            dataSource = null;
            DataSetInfo[] existing = null;
            if(currentFile != null && currentFile.isFile()) 
            {
               dataSource = DataSrcFactory.getInstance().getDataSource(
                        currentFile, (mode == OpMode.OPENDS ? requestedDSFmts : null), false);
               if(dataSource != null) 
               {
                  existing = dataSource.getSummaryInfo();
                  if(existing == null) dataSource = null;  // something went wrong. Assume file is bad.
               }
            }
            if(existing == null) existing = new DataSetInfo[0];
            else if(mode == OpMode.OPENDS && requestedDSFmts != null)
            {
               // restrict datasets shown to those matching requested dataset formats
               List<DataSetInfo> dsinfo = new ArrayList<>();
               for(DataSetInfo dsi : existing)
               {
                  Fmt fmt = dsi.getFormat();
                  for(Fmt requestedDSFmt : requestedDSFmts)
                     if(fmt == requestedDSFmt)
                     {
                        dsinfo.add(dsi);
                        break;
                     }
               }
               if(dsinfo.size() < existing.length)
               {
                  existing = new DataSetInfo[dsinfo.size()];
                  for(int i=0; i<dsinfo.size(); i++) existing[i] = dsinfo.get(i);
               }
            }
            
            dataSetList.setListData(existing);
            if(mode == OpMode.OPENDS && existing.length > 0) dataSetList.setSelectedValue(existing[0], true);
            else dataSetList.setSelectedIndex(-1);
            
            if(mode == OpMode.SAVEDS)
            {
               dsIDField.setText(((DataSet) theObject).getID());
            }
         }
         
         // update selection readout and enabled state of confirm button
         updateSelection();
         
         // update preview of file content
         updatePreview();
      }
      finally { if(!wasReloading) isReloading = false; }
   }
   
   /**
    * Update contents of read-only field that displays the full path of the current selection, as well as the enabled 
    * state of the chooser's confirm button. If the current selection is not valid for the current operational mode,
    * this button must be disabled. In addition, a short message explaining the problem appears below the path field.
    */
   private void updateSelection()
   {
      // check filename, and the dataset ID if saving a dataset, for validity
      boolean isValid = (currentFile != null);
      String errMsg = isValid ? null : "No file specified";
      if(isValid)
      {
         switch(mode)
         {
            case SELFIG :
            case OPENFIG : 
               isValid = FGModelSchema.isFigureModelXMLFile(currentFile) || 
                     MatlabFigureImporter.isMatlabFigFile(currentFile);
               if(!isValid) errMsg = "Selected file is NOT a supported figure source file";
               break;
            case OPENDS : 
               isValid = (dataSource != null); 
               if(!isValid) errMsg = "Selected file is NOT a supported DataNav data file";
               break;
            default : 
               isValid = isValidFilename(currentFile.getName()) && mode.hasValidExtension(currentFile); 
               if(!isValid) errMsg = currentFile.getName() + " is not a valid filename";
               else if(mode == OpMode.SAVEDS && dataSource != null)
               {
                  // if chosen dataset ID is valid but matches that of an existing dataset, then operation is allowed
                  // only if the "replace" checkbox is checked.
                  String id = dsIDField.getText();
                  String corrected = DataSet.ensureValidAndUniqueIdentifier(id, dataSource.getSummaryInfo());
                  isValid = id.equals(corrected) || replaceChk.isSelected();
                  if(!isValid) errMsg = "Duplicate dataset ID; specify a different ID";
               }
               break;
          }
      }
      
      // if current selection is invalid, the confirm button is disabled
      okBtn.setEnabled(isValid);
      
      // if current selection is invalid, the selection field displays the error message. Otherwise, it displays the
      // fullpath for the selection. When applicable, dataset ID is added to path with a separating colon.
      String s;
      if(isValid)
      {
         s = currentFile.getAbsolutePath();
         if(mode == OpMode.OPENDS || mode == OpMode.SAVEDS)
         {
            s += ":";
            if(mode == OpMode.OPENDS)
            {
               DataSetInfo info = dataSetList.getSelectedValue();
               if(info != null) s += info.getID();
            }
            else s += dsIDField.getText();
         }
         
         // special case: SELFIG mode with multiple files selected. Set selection field to display all file names (not
         // the full paths), separated by semicolons.
         if(mode == OpMode.SELFIG)
         {
            int[] selection = fileTable.getSelectedRows();
            if(selection.length > 1)
            {
               StringBuilder sb = new StringBuilder();
               for(int row : selection)
               {
                  File f = ((FilesTableModel) fileTable.getModel()).getFileAt(row);
                  if(f != null) sb.append(f.getName()).append("; ");
               }
               s = sb.toString();
            }
         }
      }
      else s = errMsg;
      selectedField.setText(s);
   }
   
   //
   // Restricting characters in filename
   //
   
   private static final Matcher LEGALFILENAMEMATCHER =
      Pattern.compile("[^\\u0000-\\u001f\"\\\\?\\<\\>\\*\\|\\:/]+").matcher("test");
   private static final Matcher ILLEGALBASENAMEMATCHER =
      Pattern.compile("com[1-9]{1}|lpt[1-9]{1}|con|nul|prn|clock\\$").matcher("test");
   
   /**
    * Is the specified filename valid? 
    * <p>Different operating systems and file systems place different constraints on filenames, with Windows being more
    * restrictive than Linux and Mac OS X. Rather than trying to adapt to each OS/file system environment, we chose to
    * go for the "common denominator". The restrictions currently imposed here:</p>
    * <ul>
    *    <li>Must be nonempty and less than 255 characters in length (practically speaking, a filename should be much 
    *    shorter on Windows).</li>
    *    <li>Cannot contain any of these characters: "\?<>*|:/ and Unicode 0x00-0x1f.</li>
    *    <li>File basename (minus any extension and the last '.') cannot be one of the reserved Windows device names:
    *    con, nul, prn, clock$, com1 to com9, lpt1 to lpt9 (case-insensitive comparison).</li>
    *    <li>Filename cannot begin or end with a space or a period.</li>
    * </ul>
    * @param fname The filename to test.
    * @return <code>True</code> if filename string satisfies restrictions described above.
    */
   private boolean isValidFilename(String fname)
   {
      
      boolean isValid = (fname != null) && (!fname.isEmpty()) && (fname.length() < 255);
      if(isValid)
         isValid = LEGALFILENAMEMATCHER.reset(fname).matches();
      if(isValid)
      {
         int iDot = fname.lastIndexOf('.');
         String base = ((iDot > 0) ? fname.substring(0, iDot) : fname).toLowerCase();
         isValid = !ILLEGALBASENAMEMATCHER.reset(base).matches();
      }
      if(isValid) 
         isValid = !(fname.startsWith(" ") || fname.endsWith(" ") || fname.startsWith(".") || fname.endsWith("."));
      return(isValid);
   }
   
   /**
    * Update what's rendered in the preview panel IAW the current selection and operational mode. The panel contains a 
    * figure canvas on which is rendered the currently selected figure or data set.
    * 
    * <p>In a "file open" mode, if the current file exists, the method prepares and starts a background thread, {@link 
    * FCFileLoader}, that loads the content from the file, prepares the rendering model, and notifies the chooser (on 
    * the event dispatch thread) when it is done. In this case, the canvas will initially display a "placeholder" model 
    * with the the message "LOADING..." centered there. When the worker thread has finished, the chooser will load the 
    * prepared model into the canvas, or display an error message if something went wrong.</p>
    * 
    * <p>In {@link OpMode#OPENFIG} mode only, a scrolled text area below the preview canvas will display any note
    * associated with the selected figure (once that figure is loaded). Initially the text area is emptied. Note that
    * this text area is hidden in all other modes.</p>
    * 
    * <p>In a "file save" mode, this method takes no action if the chooser panel is not yet displayable -- because the
    * preview will not render correctly otherwise! Once the panel is displayable, a graphic model of the data set or
    * figure to be saved/exported is loaded into the canvas. Once loaded, it need not be loaded again.</p>
    */
   private void updatePreview()
   {
      if(fileLoader != null)
      {
         fileLoader.abort();
         fileLoader = null;
      }

      if(mode == OpMode.SELFIG || mode == OpMode.OPENFIG || mode == OpMode.OPENDS || mode == OpMode.OPENIMG)
      {
         placeHolderPreview.setTextMessage("<No selection>");
         if(currentFile != null)
         {
            if(mode == OpMode.OPENDS)
            {
               if(dataSource == null) 
                  placeHolderPreview.setTextMessage("<Not a dataset source>");
               else if(dataSetList.getSelectedValue() == null) 
                  placeHolderPreview.setTextMessage("");
               else
               {
                  placeHolderPreview.setTextMessage("LOADING...");
                  String id = dataSetList.getSelectedValue().getID();
                  fileLoader = new FCFileLoader(dataSource, id, this);
                  fileLoader.execute();
               }
            }
            else
            {
               placeHolderPreview.setTextMessage("LOADING...");
               fileLoader = new FCFileLoader(currentFile, this);
               fileLoader.execute();
            }
         }
         previewer.setModel(placeHolderPreview);
         figNoteTextArea.setText("");
      }
      else if(isDisplayable() && previewer.getModel() == null)
      {
         RenderableModel model;
         if(mode == OpMode.SAVEDS) model = new DataSetPreview((DataSet) theObject);
         else
         {
            // if the figure is already installed in a viewer, we have to make a copy!
            FGraphicModel fgm = (FGraphicModel) theObject;
            model = (fgm.getViewer() != null) ? FGraphicModel.copy(fgm) : fgm;
            
            // load figure note/description if applicable
            if(mode == OpMode.SAVEFIG)
            {
               String note = ((FigureNode) fgm.getRoot()).getNote().trim();
               figNoteTextArea.setText(!note.isEmpty() ? note : "No description provided.");
            }
         }
         previewer.setModel(model);
      }
   }
   
   /**
    * Update the contents of the file list and the current file selection in response to a change in the target file 
    * type. Method has no effect for operations that admit only one file type.
    */
   private void changeFileType()
   {
      if(fileTypeCombo.getItemCount() <= 1) return;
      
      boolean wasReloading = isReloading;
      if(!wasReloading) isReloading = true;
      try
      {
         FCFileType currDNFileType = (FCFileType) fileTypeCombo.getSelectedItem();
         assert currDNFileType != null;
         FilesTableModel ftm = (FilesTableModel) fileTable.getModel();
         ftm.init(currentDir, currDNFileType);
         File select = new File(currentDir, (currentFile != null) ? currentFile.getName() : "Untitled");
         select = currDNFileType.fixExtension(select);
         changeCurrentFile(select, false);
      }
      finally { if(!wasReloading) isReloading = false; }
   }

   /**
    * Helper method extinguishes the chooser dialog, removes the singleton chooser panel from it, disposes the dialog,
    * and performs other clean-up. For a confirmed file-save operation, it immediately launches a modal progress dialog 
    * and a separate worker thread to perform the save.
    * @param cancelled True if user cancelled out of the chooser dialog.
    */
   @SuppressWarnings("BusyWait")
   private void extinguish(boolean cancelled)
   {
      if(chooserDlg == null) throw new IllegalStateException("Chooser dialog does not exist!");
      
      lastDlgSize = chooserDlg.getSize();
      chooserDlg.setVisible(false);
      chooserDlg.remove(this);
      chooserDlg.removeComponentListener(this);
      chooserDlg.dispose();
      chooserDlg = null;
      wasCancelled = cancelled;
      
      // if user confirmed operation to open a figure, data source file, or image file, we need to wait for loader 
      // thread to finish!
      boolean isLoadContent = (mode == OpMode.OPENFIG) || (mode == OpMode.OPENDS) || (mode == OpMode.OPENIMG);
      boolean gaveUp = false;
      if(isLoadContent && !wasCancelled)
      {
         if(fileLoader != null && !fileLoader.isDone())
         {
            Cursor oldCursor = getCursor();
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            long tStart = System.currentTimeMillis();
            do
            {
               try { Thread.sleep(400); } catch(InterruptedException ignored) {}
            } while((!fileLoader.isDone()) && (System.currentTimeMillis() - tStart < 5000));
            
            gaveUp = !fileLoader.isDone();
            fileLoader.abort();
            setCursor(oldCursor);
         }
         fileLoader = null;
      }
      else if(fileLoader != null)
      {
         fileLoader.abort();
         fileLoader = null;
      }
      
      // don't leave a model rendered in preview canvas, since these can take up lots of memory.
      previewer.setModel(null);
      
      // if operation was cancelled, we're done.
      if(wasCancelled) 
      {
         theObject = null;
         return;
      }
      
      // for operations that merely choose a file but do nothing with it, we're done
      if(mode == OpMode.SELFIG || (mode == OpMode.SAVEFIG && selectFileOnly))
         return;
      
      // for a file-open operation, retrieve the figure, data set, or image. Figures and data sets can be obtained from 
      // the workspace model cache, unless the loader thread failed. The image object is stored in theObject when the
      // loader finishes.
      if(mode == OpMode.OPENFIG || mode == OpMode.OPENDS || mode == OpMode.OPENIMG)
      {
         if(!gaveUp)
         {
            if(mode == OpMode.OPENFIG)
            {
               RenderableModel model = workspace.getModelFromCache(currentFile, null);
               if(model instanceof FGraphicModel) theObject = model;
            }
            else if(mode == OpMode.OPENDS)
            {
               String id = dataSetList.getSelectedValue().getID();
               RenderableModel model = workspace.getModelFromCache(currentFile, id);
               if(model instanceof DataSetPreview)
                  theObject = ((DataSetPreview)model).getDataSet();
            }
            else
            {
               // in this case, the buffered image is already stored in theObject when the loader finishes. Here we
               // just verify that.
               if(!(theObject instanceof BufferedImage)) theObject = null;
            }
         }
         else
         {
            theObject = null;
            String msg = "File loader timeout -- unable to complete operation!";
            JOptionPane.showMessageDialog(owner, msg, "Error", JOptionPane.ERROR_MESSAGE);
         }
      }
      else
         save(currentFile, theObject, replaceChk.isSelected());
   }

   /** 
    * Handler responds to user selections in the dataset ID list or the file table.
    * @see ListSelectionListener#valueChanged(ListSelectionEvent)
    */
   public void valueChanged(ListSelectionEvent e)
   {
      if(isReloading || e.getValueIsAdjusting()) return;
      Object src = e.getSource();
      if(src == fileTable.getSelectionModel()) 
      {
         if(mode == OpMode.SELFIG)
         {
            // in SELFIG mode, we always set the file displayed to the one corresponding to the current "lead selection 
            // index", which will be the last row clicked -- regardless whether the user is selecting one row or 
            // building a multi-interval multi-selection. Thus, the user can get a preview of that file. NOTE that it
            // may not be part of the selection in certain situations, but at least this lets the user look at files
            // as he builds a multi-selection (rather than having a blank preview).

            int[] selection = fileTable.getSelectedRows();
            if(selection.length == 0) return;  // this should never happen
            
            int lead = fileTable.getSelectionModel().getLeadSelectionIndex();
            File f = ((FilesTableModel)fileTable.getModel()).getFileAt(lead);
            if(f == null) f = ((FilesTableModel)fileTable.getModel()).getFileAt(selection[0]);
            if(f == null || f.isDirectory()) return;  // this should never happen
            
            // we always update file selection field and preview pane in this op mode because they're handled 
            // somewhat differently.
            if(!Utilities.filesEqual(f, currentFile))
            {
               currentFile = f;
               updateSelection();
               updatePreview();
            }
         }
         else
         {
            File f = ((FilesTableModel)fileTable.getModel()).getFileAt(fileTable.getSelectedRow());
            if(f == null || f.isDirectory()) return;  // this should never happen
            changeCurrentFile(f, false);
         }
      }
      else if(src == dataSetList && mode == OpMode.OPENDS)
      {
         updateSelection();
         updatePreview();
      }
   }

   /**
    * Handler responds to user actions from the current directory or file type combo boxes, or the action buttons. It
    * also responds to a "task done" notification from the worker thread (<code>FCFileLoader</code>) that prepares 
    * rendered graphic model of the selected figure, dataset, or image.
    * @see ActionListener#actionPerformed(ActionEvent)
    */
   public void actionPerformed(ActionEvent e) 
   { 
      if(isReloading) return;
      Object src = e.getSource();
      if(src == dirCombo)
         changeCurrentDirectory((File)dirCombo.getSelectedItem(), false);
      else if(src == fileTypeCombo)
         changeFileType();
      else if(src == replaceChk)
         updateSelection();
      else if(src == fileNameField && !mode.isOpenMode())
      {
         updateSelection();
         if(okBtn.isEnabled()) extinguish(false);
      }
      else if(src == okBtn || src == cancelBtn)
         extinguish(src == cancelBtn);
      else if(src instanceof FCFileLoader)
      {
         FCFileLoader fl = (FCFileLoader)src;
         RenderableModel rm = fl.getModel();
         if(rm != null)
         {
            previewer.setModel(rm);
            if(mode == OpMode.OPENIMG) theObject = fl.getImageLoaded(); 
            if(mode == OpMode.OPENFIG && (rm instanceof FGraphicModel))
            {
               FigureNode figNode = (FigureNode) ((FGraphicModel) rm).getRoot();
               String note = figNode.getNote().trim();
               figNoteTextArea.setText(!note.isEmpty() ? note : "No description provided.");
            }
         }
         else 
         {
            if(mode == OpMode.OPENIMG) theObject = null; 
            placeHolderPreview.setTextMessage("Failed to load");
            if(previewer.getModel() != placeHolderPreview)
               previewer.setModel(placeHolderPreview);
            figNoteTextArea.setText("An error has occurred. Report to developer:\n\n" + fl.getErrorMessage());
         }
      }
   }

   /** 
    * Handler detects double-clicks on cells in the first column of the files table. Double-clicking on a directory 
    * makes it the current directory. Double-clicking on a valid file for the current operational mode selects that
    * file and extinguishes the chooser.
    * @see MouseListener#mouseClicked(MouseEvent)
    */
   public void mouseClicked(MouseEvent e) 
   {
      if(isReloading || e.getSource() != fileTable || e.getClickCount() != 2) return;
      Point p = e.getPoint();
      int row = fileTable.rowAtPoint(p);
      int col = fileTable.columnAtPoint(p);
      if(row < 0 || col < 0) return;
      
      File f = ((FilesTableModel) fileTable.getModel()).getFileAt(row);
      if(f == null) return;
      
      changeCurrentFile(f, false);
      if(f.isFile() && okBtn.isEnabled())
         extinguish(false);
   }
   
   public void mouseEntered(MouseEvent e) {}
   public void mouseExited(MouseEvent e) {}
   public void mousePressed(MouseEvent e) {}
   public void mouseReleased(MouseEvent e) {}

   /** 
    * Handler method that updates selected chooser panel controls whenever the user changes text in the <i>file name</i> 
    * or <i>dataset ID</i> text fields, which are editable only in one of the file-save operational modes. 
    * <p>Since this is a <code>DocumentListener</code> handler, it is essential that the text field's content not be
    * altered by the handler -- or thread deadlock could result!</p>
    * @param isFileName True if the <i>file name</i> field was updated; else, <i>dataset ID</i> field.
    */
   private void onDestinationUpdate(boolean isFileName)
   {
      if(mode.isOpenMode()) return;

      isFilenameUpdate = true;
      try
      {
         if(isFileName)
         {
            // form the current file path. If it lacks a valid extension, tack one on to the end of filename. 
            // Partially completed extensions are completed, eg: name.fy --> name.fyp
            FCFileType type = (FCFileType) fileTypeCombo.getSelectedItem();
            assert(type != null);
            File select = null;
            String fname = fileNameField.getText();
            if(fname != null && !fname.isEmpty())
            {
               fname = type.addValidExtension(fname);
               select = (currentDir == null) ? new File(fname) : new File(currentDir, fname); 
            }
            changeCurrentFile(select, false);
         }
         else
            updateSelection();
      }
      finally { isFilenameUpdate = false; }
   }
   
   public void changedUpdate(DocumentEvent e) { onDestinationUpdate(e.getDocument() == fileNameField.getDocument()); }
   public void insertUpdate(DocumentEvent e) { onDestinationUpdate(e.getDocument() == fileNameField.getDocument()); }
   public void removeUpdate(DocumentEvent e) { onDestinationUpdate(e.getDocument() == fileNameField.getDocument()); }

   
   /** Enumeration of the file chooser's different operational modes, with file type support. */
   private enum OpMode
   {
      /** Select one or more FypML and/or <i>Matlab</i> FIG files (multi-selection support). */
      SELFIG( new FCFileType[] {FCFileType.FGX, FCFileType.FYP, FCFileType.FIG} ),
      /** Load a FypML figure from a FypML file or import it from a compatible <i>Matlab</i> FIG file. */ 
      OPENFIG( new FCFileType[] {FCFileType.FGX, FCFileType.FYP, FCFileType.FIG} ),
      /** Save figure definition file. */ 
      SAVEFIG( new FCFileType[] {FCFileType.FYP} ),
      /** Export figure to a PS, PDF, JPEG, PNG, or SVG file. */ 
      EXPFIG( new FCFileType[] {FCFileType.PS, FCFileType.PDF, FCFileType.JPEG, FCFileType.PNG, FCFileType.SVG} ),
      /** Load an image to be embedded in a <i>FypML</i> figure. */
      OPENIMG( new FCFileType[] {FCFileType.IMG} ),
      /** Retrieve dataset from <em>DataNav</em>-compatible source file. */ 
      OPENDS( new FCFileType[] {FCFileType.DNX} ),
      /** Save dataset to <em>DataNav</em>-compatible source file. */ 
      SAVEDS( new FCFileType[] {FCFileType.DNB, FCFileType.DNA} );
      
      /** The set of valid file types for this operational mode. */
      private final FCFileType[] validDNFileTypes;
      
      OpMode(FCFileType[] types) { validDNFileTypes = types; }
      
      /**
       * Is this one of the chooser's "file open" operational modes?
       * @return True for a file-open mode; else false.
       */
      boolean isOpenMode()
      {
         return(this==SELFIG || this==OPENFIG || this==OPENDS || this==OPENIMG);
      }
      
      /**
       * Is this chooser operational mode related to a data set source file?
       * @return True if this mode is {@link #OPENDS} or {@link #SAVEDS}. Else false.
       */
      boolean isDataMode() { return(this==OPENDS || this==SAVEDS); }
      
      /** 
       * Does the file have a valid extension for this operational mode? 
       * @param f The abstract pathname to be tested.
       * @return <code>True</code> if extension is valid; <code>false</code> otherwise.
       */
      boolean hasValidExtension(File f) 
      {
         for(FCFileType type : validDNFileTypes) if(type.hasValidExtension(f)) 
            return(true);
         return(false);
      }
      
      /**
       * Fix the extension of a file so that it is valid for this operational mode.
       * @param f The abstract pathname to be corrected.
       * @return If argument already has a valid extension, it is returned. Else, method returns a new <code>File</code> 
       * with a valid extension replacing the invalid one. It is appended if original filename lacked an extension.
       */
      @SuppressWarnings("unused")
      File fixExtension(File f)
      {
         if(f == null) throw new IllegalArgumentException();
         return(hasValidExtension(f) ? f : validDNFileTypes[0].fixExtension(f));
      }
   }
   
   /** 
    * Helper class thar renders items in the data set list in the chooser dialog's UI. A data set is represented by
    * its ID and an icon that roughly reflects the set format.
    * @author sruffner
    */
   private static class DataSetInfoLCR extends DefaultListCellRenderer
   {
      @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                              boolean select, boolean focus)
      {
         super.getListCellRendererComponent(list, value, index, select, focus);
         String s = "";
         String tooltip = null;
         Icon icon = null;
         if(value instanceof DataSetInfo)
         {
            DataSetInfo info = (DataSetInfo)value;
            s = info.getID();
            Fmt fmt = info.getFormat();
            if(fmt == Fmt.RASTER1D) icon = FCIcons.V4_RASTER_16;
            else if(fmt == Fmt.XYZIMG) icon = FCIcons.V4_HEATMAP_16;
            else icon = FCIcons.V4_TRACE_16;            
            tooltip = info.getShortDescription();
         }
       
         if(list.getLayoutOrientation() != JList.VERTICAL) s += "   ";
         setText(s);
         setToolTipText(tooltip);
         setIcon(icon);
         return(this);
      }
   }

   /**
    * Helper class that renders items in the directory path combo box dropdown list within <code>FCChooser</code>. It
    * extends <code>FileOrDatasetIDListCellRenderer</code> with two minor changes: (1) It modifies the border of the
    * first MRU directory (if any) in the dropdown list so that a thickened line separates the directories that form
    * the hierarchy of the currently selected directory from the MRU directories. (2) It displays the full path of the
    * MRU directories.
    * @author sruffner
    */
   private class DirPathComboRenderer extends DefaultListCellRenderer
   {
      @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                                                              int index, boolean select, boolean focus)
      {
         super.getListCellRendererComponent(list, value, index, select, focus);
         String s = "";
         String tooltip = null;
         Icon icon = null;
         if(value instanceof File) 
         {
            File f = (File)value;
            
            s = f.getName();
            tooltip = f.getAbsolutePath();
            if(index > indexOfRoot)
            {
               s = f.getAbsolutePath();
               tooltip = null;
               if(index == indexOfRoot+1)
                  setBorder(BorderFactory.createMatteBorder(3,0,0,0, getForeground()));
            }

            if(f.isDirectory()) icon = fsView.getSystemIcon(f);
            else
            {
               FCFileType fileType = FCFileType.getFileType(f);
               if(fileType != null) icon = fileType.getRepresentativeIcon();
               else icon = FCIcons.V4_BLANKDOC_16;
            }
         }
         
         if(list.getLayoutOrientation() != JList.VERTICAL) s += "   ";
         setText(s);
         setToolTipText(tooltip);
         setIcon(icon);
         return(this);
      }
   }
   
   //
   // Tabular listing of current directory filtered contents
   //
   
   /** 
    * A simple read-only table model for the two-column tabular listing of the current directory's filtered contents. 
    * The first column ("Name") is the <code>File</code> itself, while the second column ("Last Modified") is a 
    * <code>Date</code> representing that file's last-modified time.
    * 
    * <p>Since the current directory could be a file system root or a special system folder like "My Computer" in 
    * <em>Windows</em> -- containing entries for hard drives or other special folders --, entries are ordered as 
    * follows:</p>
    * <ul>
    * <li>Any drive partitions, floppy drives or special system folders are listed first, in alphabetical order. Since
    * it makes little sense, these entities do NOT have "last-modified" times. Also, we discovered in testing that 
    * accessing the "last-modified" time of a non-existent floppy drive on Windows (Windows for some reason thinks it's
    * there!) took over 5 seconds!</li>
    * <li>Any normal file system directories are listed next, in descending order by the last-modified time (most 
    * recent first).</li>
    * <li>Any real files are listed last, again in descending order by the last-modified time.</li>
    * </ul>
    * 
    * <p><strong>IMPORTANT</strong>: <code>FilesTableModel</code> uses the <code>FileSystemView</code> API rather than
    * the <code>File</code> API to get the contents of a given "directory", which may be a pseudo directory such as 
    * Window's <em>Desktop</em>, <em>MyDocuments</em>, etc. The latter API does not handle these correctly! Directory 
    * contents are filtered by an instance of <code>FCFileType</code>, which hides all files that have filenames 
    * starting with a period and accepts all directories and all files ending in a valid extension for that type.</p>
    */
   private class FilesTableModel extends AbstractTableModel
   {
      private static final long serialVersionUID = 1L;
      
      /** The filtered file list, already ordered as described in class header. */
      private final List<File> contents = new ArrayList<>();
      
      /** 
       * The cached last-modified times for the filtered file list. Certain special file system entities -- drives,
       * floppy drives, and other special folders -- will have <code>null</code> entries here.
       */
      private final List<Long> lastModTimes = new ArrayList<>();
      
      /** Index position of the first file in the filtered file list. If negative, there are no files (only folders). */
      private int firstFileIndex = -1;
      
      /** 
       * Reinitialize the table model, then inform all listeners that the table data has changed entirely. The specified
       * directory's contents will be filtered IAW the specified file type, and the filtered contents ordered as 
       * described in the class header.
       * @param dir The directory whose contents are to be displayed. If this is <code>null</code> or not a directory,
       * the table will be cleared. 
       * @param type The file format type, for filtering purposes. If <code>null</code>, <code>FCFileType.FYP</code> is
       * assumed.
       */
      void init(File dir, FCFileType type)
      {
         contents.clear();
         
         if(dir == null || !dir.isDirectory()) 
         {
            fireTableDataChanged();
            return;
         }
         if(type == null) type = FCFileType.FYP;
         
         // NOTE: On macOS 10.14+, the listFiles() call returns null for special folders like the user's Documents and 
         // Downloads folders. MacOS denies access to such folders unless the user has specifically granted access...
         File[] files = null;
         try { files = dir.listFiles(); } 
         catch(SecurityException se) 
         {
            System.out.println("listFiles() failed: " + se.getMessage());
         }
         if(files == null)
         {
            fireTableDataChanged();
            JOptionPane.showMessageDialog(FCChooser.this, 
                  String.format("FigureComposer was denied or failed to access the file listing for directory:\n   %s", dir),
                  "Directory access failure", JOptionPane.ERROR_MESSAGE);
            return;
         }
         
         // pass 1: Filter out all entries in raw file list that do pass the file type filter. Also, list all 
         // directories in descending order by last modified time (most recent first). Drive partitions, floppy drives,
         // and other file system entities that are not real files or directories will appear first and be ordered 
         // alphabetically by display name rather than by last modified time.
         for(int i=0; i<files.length; i++)
         {
            if(!type.accept(files[i]))
            {
               files[i] = null;
               continue;
            }
            else if(!files[i].isDirectory()) continue;

            File folder = files[i];
            files[i] = null;
            
            if(fsView.isDrive(folder) || fsView.isFloppyDrive(folder) || !fsView.isFileSystem(folder))
            {
               String dispname = fsView.getSystemDisplayName(folder);
               int j = 0;
               while(j < contents.size() && lastModTimes.get(j) == null)
               {
                  if(dispname.compareTo(fsView.getSystemDisplayName(contents.get(j))) < 0) break;
                  ++j;
               }
               contents.add(j, folder);
               lastModTimes.add(j, null);
            }
            else
            {
               long lastModT = folder.lastModified();
               
               int j = 0;
               while(j < contents.size())
               {
                  Long lmt = lastModTimes.get(j);
                  if(lmt != null && lmt < lastModT) break;
                  ++j;
               }
               contents.add(j, folder);
               lastModTimes.add(j, lastModT);
            }
         }
         
         // pass 2: All files in descending order by last modified time (most recent first)
         firstFileIndex = -1;
         for(File file : files)
            if(file != null && file.isFile())
            {
               if(firstFileIndex < 0) firstFileIndex = contents.size();

               long lastModT = file.lastModified();
               int j = firstFileIndex;
               while(j < contents.size())
               {
                  if(lastModTimes.get(j) < lastModT) break;
                  ++j;
               }
               contents.add(j, file);
               lastModTimes.add(j, lastModT);
            }

         fireTableDataChanged();
      }

      /** 
       * Get the most recent file in this <code>FilesTableModel</code>. 
       * @return The first non-directory entry in the file list, or <code>null</code> if there is no such entry.
       */
      File getMostRecentFile() { return(firstFileIndex < 0 ? null : contents.get(firstFileIndex)); }
      
      /**
       * Get the table row containing the specified pathname.
       * @param f The abstract pathname sought.
       * @return The row index containing the pathname, or -1 if not found.
       */
      int findRowContaining(File f)
      {
         if(f == null) return(-1);
         for(int i=0; i<contents.size(); i++) if(f.equals(contents.get(i))) return(i);
         return(-1);
      }
      
      /** 
       * Get the abstract pathname located in the specified table row.
       * @param row Table row index.
       * @return The corresponding pathname, or <code>null</code> if row index is invalid.
       */
      File getFileAt(int row) { return(row >= 0 && row < contents.size() ? contents.get(row) : null); }
      
      
      private final static String NAMECOL = "Name";
      private final static String DATECOL = "Last Modified";
      
      @Override public Class<?> getColumnClass(int c) { return(c == 0 ? File.class : Date.class); }
      @Override public String getColumnName(int c) { return(c == 0 ? NAMECOL : DATECOL); }


      public int getColumnCount() { return(2); }
      public int getRowCount() { return(contents.size()); }

      public Object getValueAt(int r, int c)
      {
         if(c < 0 || c > 1 || r < 0 || r >= contents.size()) return(null);
         Long lastModT = lastModTimes.get(r);
         return((c==0) ? contents.get(r) : (lastModT != null ? new Date(lastModT) : null));
      }
   }

   /**
    * A simple extension of <code>DefaultTableCellRenderer</code> for the two-column tabular listing of the current 
    * directory's contents. If the value to be rendered is a <code>File</code> instance, the renderer's text is the 
    * filename and its icon is the file system icon for that <code>File</code>. If the value is a <code>Date</code>, 
    * there is no icon and the renderer's text is set to the default date-time string for the current locale. Any other 
    * value class or a null value is rendered as the string "  -- ";
    * @author sruffner
    */
   private class FilesTableRenderer extends DefaultTableCellRenderer
   {
      private static final long serialVersionUID = 1L;
      final DateFormat dateFormatter = DateFormat.getDateTimeInstance();

      @Override
      protected void setValue(Object value)
      {
         if(value instanceof File)
         {
            File f = (File) value;
            setIcon(f.isDirectory() ? fsView.getSystemIcon(f) : null);
            setText(fsView.getSystemDisplayName(f));
         }
         else if(value instanceof Date)
         {
            setIcon(null);
            setText(dateFormatter.format((Date)value));
         }
         else
         {
            setIcon(null);
            setText("  -- ");
         }
      }
   }
   
   
   //
   // Infrastructure for background file IO operations WITHOUT first using FCChooser to select target file.
   //
   
   /** Flag indicating success or failure of last background file operation using <code>FileTaskWorker</code>. */
   private boolean fileOpOk;
   
   /**
    * Helper method which raises a modal message dialog and initiates the worker thread that completes a file-save 
    * operation. A chooser dialog must not be visible when this method is called.
    * @param dst The destination file. It must have a valid extension.
    * @param saveObj The object to be saved -- an instance of <code>FGraphicModel</code> or <code>DataSet</code>.
    * @param replace When saving a dataset, this flag indicates whether the dataset should replace an existing dataset
    * in the destination that has the same ID. If not set and the file contains a dataset with the same ID, the 
    * operation will be aborted.
    * @return True if task was completed successfully.
    */
   private boolean save(File dst, Object saveObj, boolean replace)
   {
      // fails silently if chooser dialog is still visible or file does not have a supported extension
      if(chooserDlg != null && chooserDlg.isVisible()) return(false);
      FCFileType type = FCFileType.getFileType(dst);
      if(type == null) return(false);
      
      SaveJob job;
      if(type == FCFileType.FYP || FCFileType.isFigureExportFileType(type))
         job =new SaveJob(dst, (FGraphicModel) saveObj);
      else 
         job =new SaveJob(dst, (DataSet) saveObj, replace);
      
      FileTaskWorker saver = new FileTaskWorker(job);
      
      Dlg progressDlg =new Dlg(owner, saver);
      progressDlg.pack();
      progressDlg.setLocationRelativeTo(owner);
      
      progressDlg.setVisible(true);
      
      fileOpOk = saver.wasSuccessful();
      if(fileOpOk) workspace.addToWorkspace(dst);        // add file saved to user's workspace.
      return(fileOpOk);
   }
   
   /**
    * Helper method which raises a modal message dialog and initiates the worker thread that completes one or more
    * file-save operations. A chooser dialog must not be visible when this method is called.
    * @param jobs The figure-save, figure-export, or data set-save operations to be performed.
    * @return The number of jobs successfully completed. The worker thread will not attempt to perform any more jobs 
    * in the list once one job fails.
    */
   private int saveAll(List<SaveJob> jobs)
   {
      // fails silently if chooser dialog is still visible, or no jobs to process.
      if((chooserDlg != null && chooserDlg.isVisible()) || jobs == null || jobs.isEmpty()) return(0);
      
      FileTaskWorker saver = new FileTaskWorker(jobs);
      
      Dlg progressDlg =new Dlg(owner, saver);
      progressDlg.pack();
      progressDlg.setLocationRelativeTo(owner);
      
      progressDlg.setVisible(true);
      
      int nOk = saver.getNumJobsDone();
      fileOpOk = saver.wasSuccessful();
      
      // add saved figure files to user's workspace
      for(int i=0; i<nOk; i++) workspace.addToWorkspace(jobs.get(i).dst);
      
      return(nOk);
   }
   
   /**
    * This is just a container wrapping a <i>FypML</i> figure or data set with the file to which it is to be saved or
    * exported.
    * 
    * @author sruffner
    */
   private static class SaveJob
   {
      SaveJob(File dst, FGraphicModel fgm)
      {
         this.dst = dst;
         this.fgm = fgm;
      }
      
      SaveJob(File dst, DataSet ds, boolean replace)
      {
         this.dst = dst;
         this.ds = ds;
         this.replace = replace;
      }
      
      final File dst;
      FGraphicModel fgm = null;
      DataSet ds = null;
      boolean replace = false;
   }
   
   /**
    * This simple modal message dialog blocks the user interface while a {@link FileTaskWorker} thread executes a
    * file-load or file-save operation. The worker notifies the dialog (on the event dispatch thread) when it is done, 
    * and also to report progress during a multi-object load. If the file operation was successful, the dialog is 
    * extinguished and normal GUI activity resumes. If any error occurred, this dialog is updated to display the error 
    * message and an "OK" button by which the user can extinguish the dialog after reading the message.
    * @author sruffner
    */
   private static class Dlg extends JDialog implements ActionListener, WindowListener
   {
      Dlg(Window owner, FileTaskWorker worker)
      {
         super(owner, "Please wait...", ModalityType.APPLICATION_MODAL);
         setDefaultCloseOperation(DISPOSE_ON_CLOSE);
         setResizable(false);
         addWindowListener(this);

         Color bkg = new Color(34, 52, 96);
         
         this.worker = worker;
         this.worker.setBlockingDialog(this);
         
         msgArea = new JTextArea(6, 60);
         msgArea.setForeground(Color.WHITE);
         msgArea.setBackground(bkg);
         msgArea.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 16));
         
         final JScrollPane scroller = new JScrollPane(msgArea);
         
         confirmBtn = new JButton("OK");
         confirmBtn.addActionListener(this);
         confirmBtn.setEnabled(false);
         
         JPanel contentPane = new JPanel();
         contentPane.setBackground(bkg);
         setContentPane(contentPane);

         SpringLayout layout = new SpringLayout();
         contentPane.setLayout(layout);
         contentPane.add(scroller);
         contentPane.add(confirmBtn);
         
         layout.putConstraint(SpringLayout.NORTH, scroller, 20, SpringLayout.NORTH, contentPane);
         layout.putConstraint(SpringLayout.NORTH, confirmBtn, 10, SpringLayout.SOUTH, scroller);
         layout.putConstraint(SpringLayout.SOUTH, contentPane, 20, SpringLayout.SOUTH, confirmBtn);

         layout.putConstraint(SpringLayout.WEST, scroller, 50, SpringLayout.WEST, contentPane);
         layout.putConstraint(SpringLayout.EAST, contentPane, 50, SpringLayout.EAST, scroller);
         layout.putConstraint(SpringLayout.HORIZONTAL_CENTER,confirmBtn,0,SpringLayout.HORIZONTAL_CENTER,contentPane);
      }
      
      private JTextArea msgArea = null;
      private JButton confirmBtn = null;
      private FileTaskWorker worker = null;
      private final static String LINEFEED = "\n";
      
      void workerDone(String errMsg)
      {
         if(errMsg == null)
         {
            setVisible(false);
            dispose();
         }
         else
         {
            String[] lines = errMsg.split("\n");
            for(String line : lines) msgArea.append(line + LINEFEED);
            confirmBtn.setEnabled(true);
         }
      }
      
      void progressUpdate(String msg) { msgArea.append(msg + LINEFEED); }
      
      public void actionPerformed(ActionEvent e) 
      { 
         setVisible(false); 
         dispose();
      }

      @Override public void windowOpened(WindowEvent e) { worker.execute(); }
      @Override public void windowClosing(WindowEvent e) {}
      @Override public void windowClosed(WindowEvent e) {}
      @Override public void windowIconified(WindowEvent e) {}
      @Override public void windowDeiconified(WindowEvent e) {}
      @Override public void windowActivated(WindowEvent e) {}
      @Override public void windowDeactivated(WindowEvent e) {}
   }


   /**
    * A background worker thread that saves a single <i>FypML</i> figure to the file system, exports a single figure
    * to another file format (PS, PNG, JPEG), or saves a single <i>FypML</i> data set to a data source file.
    * 
    * <p>In most cases, the file operation won't take long -- but it could if the figure has many large data sets, or
    * if the data set to be saved is large. The worker is launched after a modal dialog -- {@link Dlg} -- is raised to
    * block the UI. This dialog displays a simple message indicating what's happening, and that message can be updated
    * by the worker thread. When the worker is done, the dialog is notified back on the event dispatch thread. If the
    * operation is successful, the dialog is automatically hidden and disposed. If not, the dialog displays a brief
    * error description and must be dismissed manually by the user.</p>
    * 
    * @author sruffner
    */
   private static class FileTaskWorker extends SwingWorker<Object, String>
   {
      FileTaskWorker(SaveJob job)
      {
         jobList = new ArrayList<>();
         jobList.add(job);
      }
      
      FileTaskWorker(List<SaveJob> jobs)
      {
         jobList = jobs;
      }
      
      /**
       * Set the modal dialog that blocks the UI while this worker thread is running.
       * @param dlg The dialog object.
       */
      void setBlockingDialog(Dlg dlg) { blockingDlg = dlg; }

      /**
       * Did this worker thread complete its task successfully?
       * @return True if task was completed successfully, or if it is still running.
       */
      boolean wasSuccessful() { return(errMsg == null); }
      
      /**
       * Get the number of file-save jobs successfully completed by the worker thread.
       * @return Number of completed jobs.
       */
      int getNumJobsDone() { return(nJobsDone); }
      
      @Override protected Object doInBackground()
      {
         // we catch any uncaught exceptions so we are sure to dismiss the progress dialog. Failure to do so leaves
         // that modal dialog hanging and renders the program unusable!
         try
         {
            for(SaveJob job : jobList)
            {
               if(job == null) { throw new IllegalStateException("Programming error: null job"); }
               
               // publish progess message, which should get displayed in modal progress dialog
               FCFileType type = FCFileType.getFileType(job.dst);
               String msg;
               if(type == FCFileType.FYP) 
                  msg = "Saving figure to: " + job.dst.getAbsolutePath() + " ...";
               else if(FCFileType.isFigureExportFileType(type))
                  msg = "Exporting figure to: " + job.dst.getAbsolutePath() + " ...";
               else 
               {
                  msg = "Saving dataset in: ";
                  msg += job.dst.getAbsolutePath() + " ...";
               }
               publish(msg);
               
               if(job.ds != null) errMsg = saveDataSet(job);
               else errMsg = saveOrExportFigure(job);
               
               if(errMsg != null) break;
               ++nJobsDone;
            }
         }
         catch(Throwable t)
         {
            errMsg = "Uncaught exception:\n  " + t.getClass().getName() + "\n  " + t.getMessage();
         }
         
         return(null);
      }

      /** Updates the blocking dialog's progress message. */
      @Override protected void process(List<String> chunks)
      {
         if(blockingDlg != null && chunks != null && !chunks.isEmpty())
         {
            blockingDlg.progressUpdate(chunks.get(chunks.size()-1));
         }
      }
      
      @Override protected void done() { if(blockingDlg != null) blockingDlg.workerDone(errMsg); }

      /** The file-save task(s) to be completed. */
      private final List<SaveJob> jobList;
      
      /** The number of tasks successfully completed. */
      private int nJobsDone = 0;
      
      /** If an error occurs during file op, this is a brief error description; else null. */
      private String errMsg = null;
      
      /** 
       * This dialog is notified (on event dispatch thread) when worker thread has finished. Also, its displayed message
       * can be updated while the worker is still running via {@link #publish(String...)}.
       */
      private Dlg blockingDlg = null;
      

      /** Helper method handles the task of saving a dataset to the destination file. */
      private String saveDataSet(SaveJob job)
      {
         FCFileType type = FCFileType.getFileType(job.dst);
         boolean ok = (type == FCFileType.DNB || type == FCFileType.DNA);
         if(!ok) return("Unrecognized file type!");
         
         DataSet dataSet = job.ds;
         File altDst = job.dst;
         IDataSrc source = DataSrcFactory.getInstance().getDataSource(job.dst, (type == FCFileType.DNA));
         if(source == null)
         {
            // in this case, destination file exists but isn't a valid dataset source file -- we must write to a
            // temporary file first.
            job.replace = false;
            int i=0;
            while(altDst.isFile()) altDst = new File(altDst.getParentFile(), altDst.getName() + ".tmp" + (i++));
            source = DataSrcFactory.getInstance().getDataSource(altDst, (type == FCFileType.DNA));
         }
         
         ok = source.writeData(dataSet, job.replace);
         String emsg = null;
         if(!ok) 
         {
            if(!altDst.equals(job.dst)) //noinspection ResultOfMethodCallIgnored
               altDst.delete();
            emsg = source.getLastError();
         }
         else if(!altDst.equals(job.dst))
         {
            if(!job.dst.delete()) emsg = "Unable to remove old file; content saved in: " + altDst.getAbsolutePath();
            else if(!altDst.renameTo(job.dst))
               emsg = "Unable to rename temp file; content saved in: " + altDst.getAbsolutePath();
         }
         
         return(emsg);
      }
      
      /** Helper method handles task of saving figure or exporting to an external format. */
      private String saveOrExportFigure(SaveJob job)
      {
         // if we're exporting and the export format is invalid, abort
         FCFileType type = FCFileType.getFileType(job.dst);
         if(type != FCFileType.FYP && !FCFileType.isFigureExportFileType(type)) 
            return("Unsupported figure export format");
 
         FGraphicModel model = job.fgm;
         
         // if destination file exists, write to a temporary file first
         File tmpDst = job.dst;
         int i=0;
         while(tmpDst.isFile()) tmpDst = new File(tmpDst.getParentFile(), "tmp" + (i++) + "_" + tmpDst.getName());
         
         String emsg = null;
         switch(type)
         {
            case FYP :
               emsg = FGModelSchemaConverter.toXML(model, tmpDst);
               break;
            case PS :
               try
               {
                  FigureNode fig = (FigureNode) model.getRoot();
                  PageFormat psPageFmt = PrinterSupport.getInstance().getCurrentPageFormat();
                  PSDoc psDoc = PSDoc.createPostscriptDoc(fig, psPageFmt, true);
   
                  // set PS bounding box to figure bounds on printed page
                  final double MILLI_IN2PT = 72.0 / 1000.0;
                  Point2D figBotLeft = fig.getPrintLocationMI();
                  Rectangle2D bb = new Rectangle2D.Double(
                        psPageFmt.getImageableX() + figBotLeft.getX() * MILLI_IN2PT,
                        psPageFmt.getHeight() - psPageFmt.getImageableY() - psPageFmt.getImageableHeight() + 
                           figBotLeft.getY()*MILLI_IN2PT,
                        fig.getWidthMI() * MILLI_IN2PT, fig.getHeightMI() * MILLI_IN2PT);
                  psDoc.setBoundingBox(bb);
                  
                  String title = "Figure Composer figure ";
                  if(!fig.getTitle().isEmpty()) title += " (" + fig.getTitle() + ")";
                  String author = FCWorkspace.getApplicationTitle() + " " + FCWorkspace.getApplicationVersion();
                  psDoc.printToFile(tmpDst, title, author);
               }
               catch(IOException | UnsupportedOperationException ioe)
               {
                  emsg = "Export failed: " + ioe.getMessage();
               }
               break;
            case PDF :
               emsg = PDFSupport.getInstance().exportFigure(model.getCurrentRootRenderable(), 
                     PrinterSupport.getInstance().getCurrentPageFormat(), tmpDst);
               break;
            case JPEG :
            case PNG :
               GraphicsConfiguration gc = null;
               if(model.getViewer() != null) 
                  gc = model.getViewer().getViewerGraphicsContext().getDeviceConfiguration();
               else if(blockingDlg != null)
                  gc = blockingDlg.getGraphicsConfiguration();
               if(gc == null)
               {
                  emsg = "Unable to obtain a graphics context to construct image data";
                  break;
               }
               BufferedImage bi = FGraphicModel.getOffscreenImage(model, gc, 300.0f);
               if(bi == null)
               {
                  emsg = "Unable to render image data";
                  break;
               }
               emsg = writeBufferedImageToFile(tmpDst, type, bi, 300);
               break;
            case SVG :
               emsg = exportFigureToSVG(model, tmpDst, null);
               break;
            default :
               emsg = "Unsupported export format: " + type;
               break;
         }
         
         if(emsg != null)
            //noinspection ResultOfMethodCallIgnored
            tmpDst.delete();
         else if(!tmpDst.equals(job.dst))
         {
            if(!job.dst.delete()) 
               emsg = "Unable to overwrite old file; content saved in: " + tmpDst.getAbsolutePath();
            else if(!tmpDst.renameTo(job.dst))
               emsg = "Unable to rename temp file; content saved in: " + tmpDst.getAbsolutePath();
         }
         
         return(emsg);
      }
      
      /** 
       * Helper method handles the task of exporting a figure to a Scalable Vector Graphics (SVG) file
       * 
       * <p><b>NOTES</b>: <ul>
       * <li>Relies on the JFreeSVG library, which supplies a Graphics2D implementation that renders Java2D graphics 
       * directly to a string buffer containing the SVG markup.</li>
       * <li>To reduce file size somewhat at the expense of speed, all geometric coordinates are limited to 3 decimal 
       * points (the default approach for converting doubles to string form is faster but writes decimals out to 13 
       * places!)</li>
       * <li>All string content is rendered in the SVG document as filled glyph outlines (via TextLayout under the hood)
       * to avoid font-related issues associated with use of the SVG <i>text</i> element. The disadvantage is that, when
       * the SVG file is imported into another program (eg, PowerPoint), the text is not editable as text!</li>
       * </ul></p>
       * 
       * @param model The figure graphic.
       * @param f The destination file.
       * @param pgFmt The current page format. If not null, then the SVG output will be sized IAW this page format and
       * the specified figure will be positioned within that page IAW its definition. Otherwise, the SVG graphic will
       * have the same dimensions as the figure.
       * @return Null if successful, else an error description.
       */
      private static String exportFigureToSVG(FGraphicModel model, File f, PageFormat pgFmt)
      {
         RootRenderable root = model.getRoot();
         
         // determine dimensions and margins for the SVG output. If a printed page format is specified, use that; else
         // the margins are all zero and the SVG dimensions match the figure's dimensions. In typographical points.
         float w, h, left=0, bot=0;
         if(pgFmt != null)
         {
            w = (float) pgFmt.getWidth();
            h = (float) pgFmt.getHeight();
            left = (float) pgFmt.getImageableX();
            bot = h - (float) (pgFmt.getImageableY() + pgFmt.getImageableHeight());
         }
         else
         {
            
            w = (float) Measure.milliInToRealUnits(root.getWidthMI(), Measure.Unit.PT);
            h = (float) Measure.milliInToRealUnits(root.getHeightMI(), Measure.Unit.PT);
         }
         
         SVGGraphics2D svg = null;
         BufferedWriter wrt = null;
         String emsg = null;
         try
         {
            // prepare the custom Graphics2D implementation that will convert Java 2D to SVG vector graphics. Move
            // origin to the BL corner and flip Y-axis so it increases upwards rather than downwards
            svg = new SVGGraphics2D(w, h, SVGUnits.PT);
            
            // slower performance, but reduces file size
            svg.setGeomDoubleConverter(new DoubleConverter(3));
            
            // so that all text is rendered as SVG paths rather than using SVG text element, which relies on 
            // correct font information.
            svg.setRenderingHint(SVGHints.KEY_DRAW_STRING_TYPE, SVGHints.VALUE_DRAW_STRING_TYPE_VECTOR);
            
            svg.translate(left, h-bot);
            svg.scale(1, -1);
            if(pgFmt != null)
               svg.clipRect(0, 0, (int) pgFmt.getImageableWidth(), (int) pgFmt.getImageableHeight());
            else
               svg.clipRect(0,  0,  (int) w, (int) h);
            
            // now rescale to milli-inches
            svg.scale(72.0/1000.0, 72.0/1000.0);
            
            // if we're positioning figure within printed page format, translate origin from bottom-left corner of 
            // imageable area to bottom-left corner of the figure within that area
            if(pgFmt != null)
            {
               Point2D figOri = root.getPrintLocationMI();
               svg.translate(figOri.getX(), figOri.getY());
            }
            
            // render the figure into the SVG graphics context
            root.render(svg, null);
            
            // now write the generated SVG to file
            wrt = new BufferedWriter(new FileWriter(f));
            wrt.write(svg.getSVGDocument());
         }
         catch(Exception e)
         {
            emsg = "Export-to-SVG failed: " + e.getMessage();
         }
         finally
         {
            if(svg != null) svg.dispose();
            try { if(wrt != null) wrt.close(); } catch(IOException ignored) {}
         }
         return(emsg);
      }

      /**
       * Write the specified buffered image to file in the specified format, including meta-data describing the image
       * resolution.
       * 
       * @param dst The destination file.
       * @param type The image file format type. The supported formats are JPEG and PNG only.
       * @param bi The buffered image to be written.
       * @param dpi The resolution of the image's underlying bitmap in pixels per inch.
       * @return Null if operation succeeds; an error message otherwise.
       */
      private static String writeBufferedImageToFile(File dst, FCFileType type, BufferedImage bi, int dpi)
      {
         String formatName = (type == FCFileType.JPEG) ? "jpeg" : "png";
         boolean foundWriter = false;
         String emsg = null;
         
         // BUG FIX: JPEG writer does not handle alpha channel well. Under certain circumstances, this led to an IO
         // exception ("metadata components != number of destination bands) or to an image with a translucent pink 
         // layer. Here, if there is an alpha channel in the image and we're writing to JPEG format, we first remove
         // the alpha channel from the source.
         if(type == FCFileType.JPEG && bi.getColorModel().hasAlpha())
         {
            BufferedImage convertedImg = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
            convertedImg.getGraphics().drawImage(bi, 0, 0, null);
            bi = convertedImg;
         }
         
         try
         {
            for(Iterator<ImageWriter> iw = ImageIO.getImageWritersByFormatName(formatName); iw.hasNext();) 
            {
               ImageWriter writer = iw.next();
               ImageWriteParam writeParam = writer.getDefaultWriteParam();
               ImageTypeSpecifier typeSpecifier =
                    ImageTypeSpecifier.createFromBufferedImageType(bi.getType());
               IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);
               if(metadata.isReadOnly() || !metadata.isStandardMetadataFormatSupported()) 
               {
                  continue;
               }

               foundWriter = true;
               
               // here's where we set the DPI in the meta-data. Long-standing bugs in the plug-in implementations for
               // both JPEG and PNG create a problem: HorizontalPixelSize and VerticalPixelSize are SUPPOSED to be 
               // specified in millimeters per dot. However, the PNG plug-in assumes dots per mm, while the JPEG plug-in
               // assumes decimeters per dot. Here we give the specific plug-ins what they want, but this code will
               // BREAK when/if the bugs ever get fixed.
               // TODO: How to deal with bugs getting fixed eventually...
               double density = ((double) dpi) / 25.4; 
               if(type == FCFileType.JPEG) density = 0.254 / ((double) dpi);
               IIOMetadataNode horiz = new IIOMetadataNode("HorizontalPixelSize");
               horiz.setAttribute("value", Double.toString(density));
               IIOMetadataNode vert = new IIOMetadataNode("VerticalPixelSize");
               vert.setAttribute("value", Double.toString(density));
               IIOMetadataNode dim = new IIOMetadataNode("Dimension");
               dim.appendChild(horiz);
               dim.appendChild(vert);
               IIOMetadataNode root = new IIOMetadataNode("javax_imageio_1.0");
               root.appendChild(dim);
               metadata.mergeTree("javax_imageio_1.0", root);

               try(ImageOutputStream stream = ImageIO.createImageOutputStream(dst))
               {
                  writer.setOutput(stream);
                  writer.write(metadata, new IIOImage(bi, null, metadata), writeParam);
               }
               break;
            }
         }
         catch(IOException ioe)
         {
            emsg = "IO error while writing image file: " + ioe.getMessage();
         }
         catch(Exception e)
         {
            emsg = "Unexpected error while writing image file: " + e.getMessage();
         }
         
         if(emsg == null && !foundWriter)
            emsg = "Unable to find plug-in to write image file using format: " + type;
         
         return(emsg);
      }
   }
}