package com.srscicomp.fc.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import com.srscicomp.common.ui.LocalFontEnvironment;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSetIDFilter;
import com.srscicomp.fc.data.DataSet.Fmt;
import com.srscicomp.fc.fig.FGNPlottableData;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.WrapLayout;

/**
 * A modeless dialog by which the user can view and manually edit a <i>DataNav</i> data set, {@link DataSet}.
 * 
 * <p>The user can change the ID, the data format (but only if more than one format is supported by the relevant data 
 * presentation node), additional defining parameters (if any), and the raw data itself. The raw data is displayed and 
 * edited as an NxM matrix in one of two view modes, <i>tabular</i> or <i>text mode</i>. A pushbutton in the bottom-left
 * corner of the dialog lets one toggle the view mode.</p>
 * 
 * <h2>Tabular mode</h2>
 * <p>Data is exposed in a {@link JTable}. Each table row is a single <i>tuple</i> in the data set, so the total number 
 * of rows is the data set <i>length</i> and the total number of columns is the data set <i>breadth</i>. All of the
 * <i>DataNav<i> data formats fit well with this matrix-like view except for the {@link Fmt#RASTER1D} format.
 * Here, each "row" is a single raster, and the individual rasters can have drastically different lengths. Nevertheless,
 * a <b>RASTER1D</b> data set can be viewed as a NxM matrix where N is the number of rasters, M is the length of the 
 * longest raster, and shorter rasters are padded with "blank" cells. The table is embedded in a scroll pane so that the
 * user can browse over a potentially large set. The installed table model is a lightweight wrapper over a copy of the 
 * current data set's raw data array; in testing, this model worked well even for a 1000x1000 matrix.</p>
 * 
 * <p>While primarily intended for reviewing the raw data in a set, the dialog does provide support for modifying that 
 * data. In tabular mode, it is possible to delete, cut, copy, and paste the current rectangular selection in the table.
 * Pasted data are obtained from the system clip board, so it is possible to copy data in text form from an external 
 * application and paste it into this tabular view. Similarly, one can cut or copy all or a portion of the data table's 
 * contents and paste it, in text form, into an external application. These editing operations are available as standard
 * keyboard shortcuts when the data table has the keyboard focus (e.g., "Ctrl-C" for "Copy", etc.), or by pressing the 
 * relevant button in the vertical tool bar along the table's left edge. Additional buttons in this tool bar let the 
 * user append a row to the table, insert a row before the first row in the current selection, append a column to the 
 * table, and insert a column before the first column in the current selection. The tool bar buttons will be enabled or
 * disabled to indicate whether or not the corresponding operation is possible given the current data format and the 
 * current table selection.</p>
 * 
 * <p><i>More on pasting clip board content into the data table</i>. Two versions of the paste operation are possible: 
 * "standard paste", in which the clip board contents replace the current selection; and "insert paste", in which the
 * clip board contents are inserted before the first row of the current selection. If the clip board contents are plain
 * text, that text must be parsable as a sequence of lines -- separated by carriage-return and/or line-feed --, each of 
 * which is a list of whitespace-separated floating-point tokens. If every line has the same number of tokens, the text 
 * is interpreted as a rectangular float array NxM, where N is the number of lines and M the number of tokens. Else, it 
 * is treated as a sequence of N rasters, each of which can have a different length. If the clip board text cannot be 
 * parsed in this manner, the paste operation fails and no change is made to the underlying data set.</p>
 * 
 * <p>The end result of a paste operation generally depends on the data format, the extent and shape of the current 
 * data table selection, and the extent and shape of the pasted data chunk. If the current selection spans the table 
 * width, all chunk rows are inserted, but they may be truncated or padded with <i>NaN</i> column-wise to span the 
 * current table width. If the current selection spans the table height, all chunk columns are inserted if possible, 
 * truncated or padded row-wise to span the current table height; in this case, column-wise truncation or padding may be
 * necessary to ensure the results data matrix does not violate the data format's breadth constraints. Finally, if the
 * current selection is a block of cells that spans neither the height nor the width of the table and a "replace paste"
 * is requested, then the selected block is replaced with clip board data, truncated or padded both row-wise and 
 * column-wise as needed.</p>
 * 
 * <p>The foregoing discussion does not apply when the data format is {@link Fmt#RASTER1D}, for which the notion
 * of "column" makes no sense. Each row is a separate raster, and different rasters will often have different lengths.
 * The cut/copy/paste/delete operations are disabled when the current selection spans more than one row but does not
 * span the entire table width (ie, entire rasters are not selected). If the selection DOES span the table column-wise,
 * then operations are enabled. In a "paste" operation, all rows in the clip-board data chunk are inserted before the
 * first selected row, and the selected rows are deleted unless it's an "insert paste" op. If the selection does not
 * span the table column-wise but involves only a SINGLE row, then the cut/copy/paste/delete operations are enabled and
 * affect only the selected raster. In this case, a "paste" operation will insert the <i>first row</i> of the clip board
 * data chunk before the first selected sample in the raster, and the selected samples are then deleted unless it's an
 * insert paste.</p>
 * 
 * <p>The append/insert column operations also behave differently for a <b>RASTER1D</b> set. They will be enabled only 
 * when the current table selection involves a single row. In that case, an addition raster sample is either appended to
 * the end of the selected raster or inserted before the first selected raster sample. The new raster sample value will 
 * be <i>NaN</i>.</p>
 * 
 * <p>The data table supports the standard gestures for selecting a single contiguous range of cells both row-wise and
 * column-wise. In addition, as a convenience, the user can make full-row selections via similar gestures on the table's
 * row header, or full-column selection using the table's column header.</p>
 * 
 * <h2>Text mode</h2>
 * <p>Data is exposed in a {@link JTextArea}. The raw data is converted to and from plain text form via the methods 
 * {@link DataSet#toPlainText()} and {@link DataSet#fromPlainText(String, StringBuffer)}. Each line in the text area is
 * analogous to a
 * single table row in the tabular mode. Whenever any change is made to the text area, the resulting content may no 
 * longer be parsed as a valid data matrix. Thus, upon detecting a change, <i>Apply</i> and <i>Revert</i> buttons in an 
 * adjacent tool bar are enabled so that the user can apply the changes made to the data set or discard them and revert
 * to the set's last saved state. In addition, the dialog's <i>Update</i> button is disabled, as is the button that 
 * toggles the display mode and the combo box that selects data format (if more than one format is possible). If the 
 * <i>Apply</i> button is pressed and the text cannot be parsed as a valid data matrix consistent with the current 
 * data format, the operation fails and a pop-up dialog describes the parsing error that occurred. If parsing is 
 * successful, the data set is updated accordingly. Text mode has advantages over tabular mode because it is easy (and 
 * perhaps more intuitive) to cut, copy, and paste in any manner you wish to create a data set -- so long as the end 
 * result can be parsed as a data matrix consistent with the currently selected format. However, the text area widget is
 * MUCH more memory intensive, so text mode is restricted to data sets in which the data matrix contains roughly 50K 
 * elements or less, or the textual representation of the matrix is 250K characters in length or less. Any attempt to 
 * add text content beyond this limit will fail. Larger data sets can be displayed only in tabular mode.</p>
 * 
 * <h2>Usage</h2>
 * <p>This dialog is intended as an application singleton, for use only by the {@link FigComposer} component. For proper
 * behavior, the dialog's owner should be the top-level window in that component's container hierarchy. By design, the
 * modeless dialog will always appear on above this top-level container, although it won't block input to that window.
 * The dialog can be repositioned and resized by the user so that it does not overlap with the container -- so you can
 * switch back and forth between the two.
 * <ul>
 * <li>To raise the dialog, call {@link #editDataFor(FGNPlottableData, JComponent)}, passing the data presentation node
 * {@link FGNPlottableData}
 * containing the data set to be displayed/edited. On the first invocation the singleton dialog is created, using the
 * top-level ancestor of the passed-in component as the dialog's owner. This ensures that the data set editor dialog
 * will behave correctly whether the {@link FigComposer} component is installed in a modal dialog (in the DataNav 
 * Builder app) or in a frame window (in the Figure Composer app).</li>
 * <li>Once raised, the dialog sets a "sticky bit" that remains set until the user explicitly closes the dialog via the
 * <i>Cancel</i> button or the close button in the dialog title bar.</li>
 * <li>Whenever the identity of the selected graphic node changes in {@link FigComposer}, message the singleton dialog
 * via {@link #onNodeSelected(FGraphicNode)}. If the selected node is NOT a data presentation node, the dialog is
 * unloaded and
 * hidden. If it is a data presentation node and the dialog is currently hidden, it will be raised ONLY if the "sticky
 * bit" is set. The idea here is to ensure that the dialog appears only if the user has explicitly requested it. Once
 * the user closes the dialog, the sticky bit is reset and the dialog won't appear again until explicitly raised via
 * {@link #editDataFor(FGNPlottableData, JComponent)}.</li>
 * <li>Whenever the dialog is unloaded, the data set will be updated (silently) IF there are unsaved changes.</li>
 * <li>If the data presentation node containing the data set on display is changed in any way, notify the singleton
 * dialog via {@link #onNodeChanged(FGraphicNode)}. If the data set itself has changed, the dialog will reload itself
 * to display the data set's current contents.</li>
 * </ul>
 * </p>
 * 
 * <p>NOTE that each floating-point number presented in the data table is preserved to 6 significant digits. According
 * to the specification for single-precision floating-point, the maximum number of significant digits is 7.</p>
 * 
 * @author sruffner
 */
class DSEditorToolDlg extends JDialog
      implements ActionListener, DocumentListener, ListSelectionListener, FocusListener, WindowListener
{
   /**
    * Raise the data set editor dialog to display/edit the data source associated with the given data presentation node.
    * 
    * @param dataOwner The data presentation node. If null, no action is taken.
    * @param c The component in the UI that raised the dialog. If the singleton dialog instance has bee created, this
    * argument is ignored; else, if non-null, the top-level ancestor of this component serves as the dialog owner. This
    * is important because the figure composer may be contained in a frame window or a modal dialog.
    */
   static void editDataFor(FGNPlottableData dataOwner, JComponent c)
   {
      if(singleton == null)
      {
         Container ctr = (c != null) ? c.getTopLevelAncestor() : null;
         singleton = new DSEditorToolDlg((ctr instanceof Window) ? ((Window) ctr) : null);
      }
      
      if(dataOwner == null || (singleton.isVisible() && singleton.dataOwner == dataOwner)) 
         return;
      
      singleton.wasRaised = true;
      singleton.display(dataOwner);
   }
   
   /**
    * Update the state of the data set editor dialog in response to a change in the node currently selected.
    * 
    * <p>The action taken depends on the type of node selected, the dialog's current state and whether or not its 
    * "sticky bit" is set. This flag is set whenever the dialog was explicitly raised by user action, and it is reset
    * whenever it was explicitly dismissed.
    * <ul>
    * <li>If the selected node is not a data presentation node, the dialog is always hidden.</li>
    * <li>If the selected node is a data presentation node, the dialog is raised IF the "sticky bit" is set. Else it
    * remains hidden. The user must take action to raise it, and {@link #editDataFor(FGNPlottableData, JComponent)}
    * must be invoked to explicitly raise the dialog (and set the sticky bit).</li>
    * <li>If the dialog is already raised when this method is invoked and the data set displayed has been changed, 
    * those changes are saved before loading the new data set (or hiding the dialog because the just-selected node is
    * NOT a data presentation node). Thus, if the user forgets to explicitly update the data set after making some
    * changes, those changes are not lost.</li>
    * </ul>
    * </p>
    * 
    * @param fgn The graphic node selected. May be null (for example, when multiple nodes selected). May or may not be 
    * a data presentation node.
    */
   static void onNodeSelected(FGraphicNode fgn)
   {
      if(singleton == null) return;
      
      if(fgn instanceof FGNPlottableData)
      {
         // if dialog is already visible and contains changes to the data set currently displayed, then save those
         // changes to its data owner first.
         if(singleton.isVisible() && fgn != singleton.dataOwner)
            singleton.updateDataSetOnOwner();
         
         // now display the data set for the data presentation node specified
         if(singleton.wasRaised)
            singleton.display((FGNPlottableData) fgn);
      }
      else if(singleton.isVisible())
      {
         // dialog is currently visible, but the selected node is NOT a data presentation node. First update the 
         // data owner if the displayed data set has changed.
         singleton.updateDataSetOnOwner();
         
         // hide dialog and unload it. The "sticky bit" remains set, so the dialog will reappear if a data presentation
         // node is selected in the future.
         singleton.setVisible(false);
         singleton.dataOwner = null;
         singleton.originalDS = null;
         singleton.currDS = null;
         singleton.currRawData = null;
         singleton.datasetChanged = false;
         singleton.textDataChanged = false;
      }
   }
   
   /**
    * Update the state of the data set editor dialog in response to a change in the graphic node specified. 
    * <p>If the dialog is raised, if the node specified is the data presentation node that owns the data set on display
    * in the dialog, AND that data set has changed, then the dialog is reloaded to display the current content of the 
    * data set. Otherwise, no action is taken.</p>
    * 
    * @param fgn The graphic node that has changed.
    */
   static void onNodeChanged(FGraphicNode fgn)
   {
      if(singleton != null && singleton.isVisible() && fgn == singleton.dataOwner)
      {
         DataSet ds = singleton.dataOwner.getDataSet();
         if(!ds.equals(singleton.originalDS))
         {
            singleton.display(singleton.dataOwner);
         }
         else
         {
            // just in case the presentation node's title changed, update dialog title accordingly
            singleton.setDialogTitle();
         }
      }
   }
   
   
   /** The singleton dialog instance. */
   private static DSEditorToolDlg singleton = null;
   
   private DSEditorToolDlg(Window owner)
   {
      super(owner, "Dataset Editor", ModalityType.MODELESS);
      
      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      addWindowListener(this);
      
      dataOwner = null;
      originalDS = null;
      currDS = null;
      currRawData = null;
      dataTableModel = new DataSetTM();
      
      createComponents();
      layoutComponents(); 
      
      setMinimumSize(new Dimension(400, 400));
   }
   

   //
   // Widgets
   //
   
   /** Text field that displays/edits the data set's identifier. */
   private JTextField idField = null;
   
  /** Selects the data set's format -- if multiple formats are allowed. */
   private JComboBox<Fmt> formatCombo = null;

   /** Numeric fields for editing the data set's additional defining parameters -- varies with data format. */
   private NumericTextField[] paramFields = null;

   /** Label precedes the numeric fields that display/edit any additional defining parameters for the data set. */
   private JLabel paramLabel = null;
   
   /** Exposes the data set's underlying data as an NxM matrix in tabular form. */
   private final JTable dataTable = new JTable();
   
   /** A one-column table that serves as the row header for the main table exposing the data matrix. */
   private final JTable rowHdrForDataTable = new JTable();
   
   /** Press button to append a new row to data table. */
   private JButton appendRowBtn = null;
   
   /** Press button to insert a new row into data table, before the first row of the current selection. */
   private JButton insertRowBtn = null;
   
   /** Press button to append a new column to data table. */
   private JButton appendColBtn = null;
   
   /** Press button to insert a new column into data table, before the first column of the current selection. */
   private JButton insertColBtn = null;
   
   /** Press button to copy current selection in data table to system clip board. */
   private JButton copyBtn = null;
   
   /** Press button to remove current selection in data table and place it in system clip board. */
   private JButton cutBtn = null;
   
   /** Press button to replace current selection in data table with contents of system clip board. */
   private JButton pasteBtn = null;
   
   /** Press button to insert clip board contents into data table before the current selection. */
   private JButton insPasteBtn = null;
   
   /** Press button to delete the current selection in data table. */
   private JButton deleteBtn = null;
   
   /** Widgets for the tabular view of data are contained in this panel. */
   private JPanel tableView = null;
   
   /** 
    * An alternate view of the raw data matrix displayed as plain text. Because textual display of floating-point data 
    * is far more memory-intensive, this view is restricted to data sets with a data size of ~50K or less. Attempts to 
    * increase the text size past ~250K characters will also fail.
    */
   private JTextArea dataTextArea = null;
   
   /** Pressing this button applies any changes in the text view, creating a new data set, if possible. */
   private JButton applyTextBtn = null;
   
   /** Pressing this button reverts text view to the last valid state of data set. */
   private JButton revertTextBtn = null;
   
   /** Widgets for the text view of data are contained in this panel. */
   private JPanel textView = null;
   
   /** Button switches from tabular to text view of data and vice versa. */
   private JButton viewBtn = null;
   
   /** The <i>Update</i> button saves any changes made thus far in the data set. The dialog remains visible. */
   private JButton okBtn = null;
   
   /** The <i>Start Over</i> button wipes out all changes made in data set since dialog was raised. */
   private JButton startOverBtn = null;
   
   /** The <i>Cancel</i> button discards any changes made in data set and extinguishes the dialog. */
   private JButton cancelBtn = null;
   
   /** Number of rows visible in the preferred scrolling view port for the data table, including header row. */
   private final static int NVISDATAROWS = 21;
   /** Number of columns visible in the preferred scrolling view port for the data table. */
   private final static int NVISDATACOLS = 10;
   
   /** Helper method creates the widgets appearing in the data set edit dialog. */
   private void createComponents()
   {
      idField = new JTextField("setID", 15);
      ((AbstractDocument)idField.getDocument()).setDocumentFilter(new DataSetIDFilter());
      idField.setToolTipText("Enter dataset ID (1-" + DataSet.MAXIDLEN + " alphanumeric or certain puncuation chars)");
      idField.addActionListener(this);
      idField.addFocusListener(this);
      
      formatCombo = new JComboBox<>();
      formatCombo.addActionListener(this);
      formatCombo.setToolTipText("Select data format. Choices limited by the type of data presentation node.");
      
      paramFields = new NumericTextField[4];
      for(int i=0; i<paramFields.length; i++)
      {
         paramFields[i] = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
         paramFields[i].setColumns(6);
         paramFields[i].setValue(0);
         paramFields[i].addActionListener(this);
      }
      
      paramLabel = new JLabel(" ", JLabel.TRAILING);

      dataTable.setModel(dataTableModel);
      Font f = new Font(LocalFontEnvironment.getMonospaceFont(), 8, Font.PLAIN);
      dataTable.setFont( f.deriveFont(Font.PLAIN, 8) );  // NOTE: setFont(f) directly behaved as if no font installed!
      dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      dataTable.setShowVerticalLines(true);
      dataTable.setShowHorizontalLines(false);
      dataTable.setGridColor(dataTable.getBackground().darker());
      
      // customize data table's header row: no reordering or resizing allowed. Centered column labels.
      JTableHeader hdr = dataTable.getTableHeader();
      hdr.setReorderingAllowed(false);
      hdr.setResizingAllowed(false);
      TableCellRenderer hdrRenderer = hdr.getDefaultRenderer();
      if(hdr.getDefaultRenderer() instanceof JLabel)
          ((JLabel) hdr.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
      
      // install custom mouse handler to support full-column selections via standard mouse gestures on the table hdr
      // It is also installed on the header of the single-column row header table -- see below.
      hdr.setAutoscrolls(true);
      HeaderMouseHandler headerHandler = new HeaderMouseHandler();
      hdr.addMouseListener(headerHandler);
      hdr.addMouseMotionListener(headerHandler);
      hdr.setFocusable(false);
      
      dataTable.setDefaultRenderer(Float.class, new FloatTCRenderer());
      dataTable.setDefaultEditor(Float.class, new SelectAllFloatEditor());
      
      // compute a column width that can handle most reasonable values, modify column model to force columns to use
      // this fixed width, and set table's viewport size to expose 10 columns and all rows in a single block
      Component renderer = dataTable.getDefaultRenderer(Float.class).getTableCellRendererComponent(
               dataTable, (float) -1.2345E-10, true, true, 0, 0);
      int colWidth = renderer.getPreferredSize().width + 5;
      dataTable.setColumnModel(new FixedWidthTCM(colWidth));
      
      // we need to set these properties AFTER installing our custom column model!
      dataTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
      dataTable.setColumnSelectionAllowed(true);
      dataTable.setRowSelectionAllowed(true);

      int prefH = dataTable.getRowHeight() * NVISDATAROWS;
      int prefW = colWidth * NVISDATACOLS;
      dataTable.setPreferredScrollableViewportSize(new Dimension(prefW, prefH));
      
      // we handle cut/copy/paste on table in a custom manner, so disable built-in support
      dataTable.setTransferHandler(null);
      
      // prepare supported data table actions, install key input-action bindings, and create toolbar buttons that are
      // mapped to these actions
      createAndInstallDataTableActions();

      // so we can update enable state of toolbar buttons in response to changes in the table selection
      dataTable.getSelectionModel().addListSelectionListener(this);
      
      // we use a simple table for the row header of the data table. We customize it so that it is rendered much like
      // the column header, and we install the same mouse handler installed on the table's column header to mediate 
      // full-row selections on the data table. That same handler is also installed on the row header's header so we can
      // scroll the top-left corner of the data table into view whenever the user single-clicks on that header, which is 
      // in the top-left corner of the parent scroll pane.
      rowHdrForDataTable.setModel(dataTableModel.getRowHeaderModel());
      LookAndFeel.installColorsAndFont(rowHdrForDataTable, 
               "TableHeader.background", "TableHeader.foreground", "TableHeader.font");
      rowHdrForDataTable.setIntercellSpacing(new Dimension(0, 0));
      rowHdrForDataTable.setRowHeight(dataTable.getRowHeight());
      rowHdrForDataTable.setDefaultRenderer(Integer.class, hdrRenderer);
      rowHdrForDataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      rowHdrForDataTable.setRowSelectionAllowed(false);
      rowHdrForDataTable.setColumnSelectionAllowed(false);
      rowHdrForDataTable.addMouseListener(headerHandler);
      rowHdrForDataTable.addMouseMotionListener(headerHandler);
      rowHdrForDataTable.setFocusable(false);
      
      rowHdrForDataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
      hdr = rowHdrForDataTable.getTableHeader();
      hdr.setReorderingAllowed(false);
      hdr.setResizingAllowed(false);
      hdr.addMouseListener(headerHandler);
      if(hdr.getDefaultRenderer() instanceof JLabel)
         ((JLabel) hdr.getDefaultRenderer()).setHorizontalAlignment(JLabel.CENTER);
      rowHdrForDataTable.setColumnModel(new FixedWidthTCM(colWidth));
      rowHdrForDataTable.setPreferredScrollableViewportSize(new Dimension(colWidth, prefH));
      
      dataTextArea = new JTextArea();
      dataTextArea.setFont(f.deriveFont(Font.PLAIN, 10));
      dataTextArea.setEditable(true);
      dataTextArea.setLineWrap(false);
      dataTextArea.getDocument().addDocumentListener(this);
      ((AbstractDocument)dataTextArea.getDocument()).setDocumentFilter(new MaxSizeFilter());
      
      applyTextBtn = new JButton(FCIcons.V4_REDO_22);
      applyTextBtn.setToolTipText("Apply changes in text view to the raw data matrix");
      applyTextBtn.addActionListener(this);
      makeToolButton(applyTextBtn);
      
      revertTextBtn = new JButton(FCIcons.V4_UNDO_22);
      revertTextBtn.setToolTipText("Revert changes in text view of the raw data matrix");
      revertTextBtn.addActionListener(this);
      makeToolButton(revertTextBtn);
      
      viewBtn = new JButton("Text");
      viewBtn.addActionListener(this);
      viewBtn.setToolTipText("Click here to toggle between tabular and text view of raw data");
      
      okBtn = new JButton("Update", FCIcons.V4_OK_16);
      okBtn.addActionListener(this);
      startOverBtn = new JButton("Start Over", FCIcons.V4_UNDO_16);
      startOverBtn.addActionListener(this);
      cancelBtn = new JButton("Cancel");
      cancelBtn.addActionListener(this);
   }
   
   /** Helper method arranges the widget appearing within this <code>DataSetDialog</code>. */
   private void layoutComponents()
   {
      int gap = FCIcons.UIGAPSZ;
      Border b = BorderFactory.createEmptyBorder(gap*2, gap*2, gap*2, gap*2);

      JPanel grp1 = new JPanel();
      grp1.setLayout(new BoxLayout(grp1, BoxLayout.LINE_AXIS));
      grp1.add(new JLabel("ID: "));
      grp1.add(idField);
      
      JPanel grp2 = new JPanel();
      grp2.setLayout(new BoxLayout(grp2, BoxLayout.LINE_AXIS));
      grp2.add(formatCombo);
      
      JPanel grp3 = new JPanel();
      grp3.setLayout(new BoxLayout(grp3, BoxLayout.LINE_AXIS));
      grp3.add(paramLabel);
      grp3.add(Box.createHorizontalStrut(gap));
      for(int i=0; i<paramFields.length; i++)
      {
         grp3.add(paramFields[i]);
         if(i < paramFields.length-1) grp3.add(Box.createHorizontalStrut(gap));
      }

      JPanel topRow = new JPanel(new WrapLayout(WrapLayout.LEADING, gap*3, gap));
      topRow.add(grp1);
      topRow.add(grp2);
      topRow.add(grp3);

      JScrollPane scrollPane = new JScrollPane(dataTable);
      scrollPane.setRowHeaderView(rowHdrForDataTable);
      scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowHdrForDataTable.getTableHeader());
      scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      
      JPanel toolPanel = new JPanel();
      toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.LINE_AXIS));
      toolPanel.add(Box.createHorizontalStrut(gap));
      toolPanel.add(appendRowBtn);
      toolPanel.add(insertRowBtn);
      toolPanel.add(appendColBtn);
      toolPanel.add(insertColBtn);
      toolPanel.add(Box.createHorizontalStrut(gap*5));
      toolPanel.add(copyBtn);
      toolPanel.add(cutBtn);
      toolPanel.add(pasteBtn);
      toolPanel.add(insPasteBtn);
      toolPanel.add(deleteBtn);
      toolPanel.setBorder(BorderFactory.createEtchedBorder());
      
      tableView = new JPanel(new BorderLayout());
      tableView.add(toolPanel, BorderLayout.NORTH);
      tableView.add(scrollPane, BorderLayout.CENTER);
      tableView.setBorder(b);
      
      scrollPane = new JScrollPane(dataTextArea);
      scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      
      toolPanel = new JPanel();
      toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.LINE_AXIS));
      toolPanel.add(Box.createHorizontalStrut(gap));
      toolPanel.add(applyTextBtn);
      toolPanel.add(revertTextBtn);
      toolPanel.setBorder(BorderFactory.createEtchedBorder());
      
      textView = new JPanel(new BorderLayout());
      textView.add(toolPanel, BorderLayout.NORTH);
      textView.add(scrollPane, BorderLayout.CENTER);
      textView.setBorder(b);
      textView.setVisible(false);
      
      JPanel centerPanel = new JPanel();
      centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.LINE_AXIS));
      centerPanel.add(tableView);
      centerPanel.add(textView);
      
      JPanel btnPanel = new JPanel(new BorderLayout());
      JPanel line = new JPanel();
      line.setLayout(new BoxLayout(line, BoxLayout.LINE_AXIS));
      line.add(okBtn);
      line.add(Box.createHorizontalStrut(gap*2));
      line.add(startOverBtn);
      line.add(Box.createHorizontalStrut(gap*2));
      line.add(cancelBtn);
      btnPanel.add(line, BorderLayout.EAST);
      btnPanel.add(viewBtn, BorderLayout.WEST);
      btnPanel.add(Box.createHorizontalStrut(100), BorderLayout.CENTER);
      
      JPanel contentPane = new JPanel(new BorderLayout());
      contentPane.setBorder(b);
      contentPane.add(topRow, BorderLayout.NORTH);
      contentPane.add(centerPanel, BorderLayout.CENTER);
      contentPane.add(btnPanel, BorderLayout.SOUTH);
      setContentPane(contentPane);
   }
   
   /** 
    * Flag set whenever dialog was raised by user action and reset whenever it was closed by user. This determines the
    * dialog's automatic show/hide behavior whenever a data presentation node is selected.
    */
   private boolean wasRaised = false;
   
   /** 
    * The first time the dialog is shown, it is centered over the dialog owner. After that, the user controls where
    * it appears.
    */
   private boolean isFirstShow = true;
   
   /** The data presentation node that uses the data set currently displayed/modified in this dialog. */
   private FGNPlottableData dataOwner = null;
   
   /** The original data set loaded when the dialog was raised. */
   private DataSet originalDS = null;

   /** The "current" data set reflects all changes made in the original since the dialog was raised. */
   private DataSet currDS = null;
   
   /** 
    * The raw data array for the current data set. <b>Single-element changes here actually alter the normally immutable 
    * data set; we do this to avoid reallocating a potentially large array each time one datum is changed in the tabular 
    * view!</b>
    */
   private float[] currRawData = null;

   /** Flag set once any kind of change has been made to the original data set. */
   private boolean datasetChanged = false;
   
   /** Table model exposes current data set's underlying raw data as an NxM matrix in tabular form. */
   private DataSetTM dataTableModel = null;
   
   /** Flag set when displaying raw data in the table view; reset for the text view. */
   private boolean isTableView = true;
   
   /** 
    * Flag set in text mode once the user has altered the content of the text area. It is reset once the changes are
    * successfully applied and the data set changed accordingly.
    */
   private boolean textDataChanged = false;
   
   /** Flag set when we need to temporarily ignore any actions from the controls in this dialog (during reload). */
   private transient boolean ignoreActions = false;

   /** Flag set after text view shown for the first time. */
   private boolean textViewShown = false;

   /** 
    * Flag set once we've fixed cell editor font to match that of cell renderer. Cannot get to the cell editor 
    * component until a non-empty table model is present, because it requires presence of a valid column!
    */
   private boolean fixedEditorFont = false;
   
   /**
    * Raise the dialog to display/edit the data for the specified data presentation node. The first time it is raised, 
    * the dialog will be centered over the dialog owner (or centered on screen if no owner was specified). After that,
    * it reappears wherever it was when it was last hidden.
    * @param dataOwner A data presentation node. If null, the dialog is not raised.
    */
   private void display(FGNPlottableData dataOwner)
   {
      if(dataOwner == null) return;
      this.dataOwner = dataOwner;
      originalDS = dataOwner.getDataSet();
      currRawData = originalDS.copyRawData();
      currDS = DataSet.createDataSet(originalDS.getInfo(), currRawData);
      datasetChanged = false;
      textDataChanged = false;
      reload();
      
      // HACK: Cannot access default table cell editor component in createComponents(), because model is empty. The
      // getTableCellEditorComponent() call expects a valid column at index 0 or it catches an exception and returns
      // null. This is how we get around that problem.
      if(!(currDS.isEmpty() || fixedEditorFont))
      {
         Font f = new Font(LocalFontEnvironment.getMonospaceFont(), 10, Font.PLAIN);
         dataTable.getDefaultEditor(Float.class).getTableCellEditorComponent(dataTable, (float) 0, false, 0, 0)
               .setFont(f.deriveFont(Font.PLAIN, 10));
         fixedEditorFont = true;
      }

      if(isFirstShow)
      {
         isFirstShow = false;
         pack();
         setLocationRelativeTo(getOwner());
         setVisible(true);
      }
      else if(!isVisible()) 
         setVisible(true);
      
      setDialogTitle();
   }
   
   /**
    * Helper method sets/updates the dialog title to reflect the title of the data presentation node that "owns" the
    * data set on display in the dialog. If the node lacks a title, its node type name is used instead.
    */
   private void setDialogTitle()
   {
      String s;
      if(dataOwner == null) 
         s = "Dataset Editor";
      else
      {
         s = dataOwner.getTitle().trim();
         if(s.isEmpty()) s = dataOwner.getNodeType().getNiceName();
         s = "Data for: " + s;
      }
      setTitle(s);
   }
   
   /**
    * Hide the data set editor dialog and can discard any unsaved changes to the data set on display. The "sticky bit"
    * is reset, so {@link #editDataFor(FGNPlottableData, JComponent)} must be invoked to raise the dialog again.
    */
   private void cancel()
   {
      setVisible(false);
      dataOwner = null;
      originalDS = null;
      currDS = null;
      currRawData = null;
      wasRaised = false;
      datasetChanged = false;
      textDataChanged = false;
   }
   
   /** Closing the dialog is the same as canceling. */
   @Override public void windowClosing(WindowEvent e) { cancel(); }
   @Override public void windowOpened(WindowEvent e) {}
   @Override public void windowClosed(WindowEvent e) {}
   @Override public void windowIconified(WindowEvent e) {}
   @Override public void windowDeiconified(WindowEvent e) {}
   @Override public void windowActivated(WindowEvent e) {}
   @Override public void windowDeactivated(WindowEvent e) {}

   /** Helper method reloads all widgets in dialog from scratch. */
   private void reload()
   {
      ignoreActions = true;

      idField.setText(currDS.getID());
      
      // one may only change the format if multiple formats are supported by the presentation node.
      formatCombo.removeAllItems();
      for(Fmt fmt : dataOwner.getSupportedDataFormats()) formatCombo.addItem(fmt);
      formatCombo.setSelectedItem(currDS.getFormat());
      formatCombo.setEnabled(dataOwner.getSupportedDataFormats().length > 1);
      
      reloadParamFields();
      
      reloadTableView();
      reloadTextView();
      
      refresh();
      
      ignoreActions = false;
   }
   
   /** Helper method for {@link #reload()}. It reloads the tabular view of the current data set. */
   private void reloadTableView()
   {
      if(!isTableView) return;
      
      dataTableModel.reload();
      dataTable.clearSelection();
      
      // make sure the top-left corner (0,0) is scrolled into view.
      if(!currDS.isEmpty()) dataTable.scrollRectToVisible(dataTable.getCellRect(0,0,true));
   }
   
   /** 
    * Helper method for {@link #reload()}. It reloads the text view of the current data set. If the data set exceeds
    * the size restrictions for display in text mode, then the method warns user with an informational dialog and 
    * switches to the tabular view.
    */
   private void reloadTextView()
   {
      if(isTableView) return;
      
      Fmt fmt = currDS.getFormat();
      int nSz = (fmt == Fmt.RASTER1D) ? currDS.getDataLength() : currDS.getDataLength() * currDS.getDataBreadth();
      boolean isBig = nSz > (MAXTEXTDATALEN / 5);
      String textData = "";
      if(!isBig) 
      {
         textData = currDS.toPlainText(6);
         isBig = textData.length() > MAXTEXTDATALEN;
      }
      
      if(isBig)
      {
         JOptionPane.showMessageDialog(this, 
                  "<html>The dataset's raw data matrix is too large to edit in<br/>" +
                  "text mode -- switching to tabular mode.</html>", 
                  "Text mode disabled", JOptionPane.WARNING_MESSAGE);
         isTableView = !isTableView;
         viewBtn.setText(isTableView ? "Text" : "Table");
         tableView.setVisible(isTableView);
         textView.setVisible(!isTableView);
         
         reloadTableView();
      }
      else
         dataTextArea.setText(textData);
   }
   
   /** Helper method updates enabled state for certain widgets on the data set dialog. */
   private void refresh()
   {
      startOverBtn.setEnabled(datasetChanged);
      
      refreshDataTableActions();
      
      boolean textHasChanged = (!isTableView) && textDataChanged;
      applyTextBtn.setEnabled(textHasChanged);
      revertTextBtn.setEnabled(textHasChanged);
      okBtn.setEnabled(datasetChanged && !textHasChanged);
      viewBtn.setEnabled(!textHasChanged);
      formatCombo.setEnabled((dataOwner.getSupportedDataFormats().length > 1) && !textHasChanged);
    }
   
   /** Helper method reloads the additional parameter fields and label, hiding unused ones, IAW current data format. */
   private void reloadParamFields()
   {
      Fmt fmt = currDS.getFormat();
      int nParams = fmt.getNumberOfParams();
      
      paramLabel.setText((nParams==0) ? "" : (fmt == Fmt.XYZIMG ? "x0 x1 y0 y1" : "dx x0"));
      paramLabel.setVisible(nParams > 0);
      
      if(fmt == Fmt.XYZIMG)
      {
         paramFields[0].setVisible(true);
         paramFields[0].setValue(currDS.getXMin());
         paramFields[0].setToolTipText("Enter start of X-coordinate range spanned by dataset Z{x,y}");
         paramFields[1].setVisible(true);
         paramFields[1].setValue(currDS.getXMax());
         paramFields[1].setToolTipText("Enter end of X-coordinate range spanned by dataset Z{x,y}");
         paramFields[2].setVisible(true);
         paramFields[2].setValue(currDS.getYMin());
         paramFields[2].setToolTipText("Enter start of Y-coordinate range spanned by dataset Z{x,y}");
         paramFields[3].setVisible(true);
         paramFields[3].setValue(currDS.getYMax());
         paramFields[3].setToolTipText("Enter start of Y-coordinate range spanned by dataset Z{x,y}");
      }
      else if(fmt == Fmt.SERIES || fmt == Fmt.MSERIES)
      {
         paramFields[0].setVisible(true);
         paramFields[0].setValue(currDS.getDX());
         paramFields[0].setToolTipText("Enter sample interval in X for data series(cannot be zero)");
         paramFields[1].setVisible(true);
         paramFields[1].setValue(currDS.getX0());
         paramFields[1].setToolTipText("Enter initial value of X for data series");
         paramFields[2].setVisible(false);
         paramFields[3].setVisible(false);
      }
      else for(NumericTextField paramField : paramFields) paramField.setVisible(false);
         
   }
   
   @Override public void focusGained(FocusEvent e) 
   {
      if(e.getSource() == idField && !e.isTemporary()) idField.selectAll();
   }
   @Override public void focusLost(FocusEvent e)
   {
      if(e.getSource() == idField && !e.isTemporary()) idField.postActionEvent();
   }

   public void actionPerformed(ActionEvent e)
   {
      if(ignoreActions || currDS == null) return;
      Object src = e.getSource();
      
      if(src == idField)
      {
         // update ID of current dataset and refresh canvas to show the new ID
         DataSet ds = currDS.changeID(idField.getText());
         if(ds == null) idField.setText(currDS.getID());
         else 
         {
            currDS = ds;
            if(!datasetChanged)
            {
               datasetChanged = true;
               refresh();
            }
         }
      }
      else if(src == formatCombo)
      {
         // changing the data format may require adding or removing columns in the raw data matrix to satisfy the target
         // format. It also may be necessary to set/correct any additional defining parameters.
         Fmt fmt = (Fmt)formatCombo.getSelectedItem();
         if(fmt == null) formatCombo.setSelectedItem(currDS.getFormat());
         else if(fmt != currDS.getFormat())
         {
            int nr = currDS.getDataLength();
            int ncOld = currDS.getDataBreadth();
            int ncNew = ncOld;
            ncNew = Utilities.rangeRestrict(ncNew, fmt.getMinBreadth(), fmt.getMaxBreadth());
            
            float[] raw = null;
            if(ncNew != ncOld)
            {
               raw = new float[nr*ncNew];
               if(ncNew < ncOld)
               {
                   for(int i=0; i<nr; i++)
                      if(ncNew >= 0)
                         System.arraycopy(currRawData, i * ncOld, raw, i * ncNew, ncNew);
               }
               else 
               {
                  for(int i=0; i<nr; i++)
                  {
                     if(ncOld >= 0) System.arraycopy(currRawData, i * ncOld, raw, i * ncNew, ncOld);
                     for(int j=ncOld; j<ncNew; j++) raw[i*ncNew + j] = Float.NaN;
                  }
               }
            }
            
            float[] params = null;
            if(fmt == Fmt.SERIES || fmt == Fmt.MSERIES)
            {
               // params = [dx x0], where dx != 0
               params = currDS.getParams();
               if(params.length != 2) params = new float[] {1, 0}; 
               else if(params[0] == 0) params[0] = 1; 
            }
            else if(fmt == Fmt.XYZIMG)
            {
               // params = [x0 x1 y0 y1], where x0 != x1 and y0 != y1
               params = currDS.getParams();
               if(params.length != 4 || params[0] == params[1] || params[2] == params[3])
                  params = new float[] {0, 10, 0, 10};
            }
            
            DataSet ds = DataSet.createDataSet(currDS.getID(), fmt, params, nr, ncNew, raw != null ? raw : currRawData);
            if(ds == null)
               formatCombo.setSelectedItem(currDS.getFormat());
            else
            {
               currDS = ds;
               if(raw != null) currRawData = raw;
               datasetChanged = true;
               reload();
            }
         }
      }
      else if(src instanceof NumericTextField)
      {
         // update one of the dataset's metadata attributes
         float[] oldParams = currDS.getParams();
         float[] params = new float[oldParams.length];
         for(int i=0; i<params.length; i++) params[i] = paramFields[i].getValue().floatValue();
         DataSet ds = currDS.changeParams(params);
         if(ds == null) reloadParamFields();
         else 
         {
            currDS = ds;
            if(!datasetChanged)
            {
               datasetChanged = true;
               refresh();
            }
         }
      }
      else if(src == startOverBtn)
      {
         if(!datasetChanged) return;
         currRawData = originalDS.copyRawData();
         currDS = DataSet.createDataSet(originalDS.getInfo(), currRawData);
         datasetChanged = false;
         textDataChanged = false;
         reload();
      }
      else if(src == applyTextBtn)
      {
         if(isTableView) return;
         StringBuffer errMsg = new StringBuffer();
         DataSet ds = currDS.fromPlainText(dataTextArea.getText(), errMsg);
         if(ds == null)
            JOptionPane.showMessageDialog(this, errMsg.toString(), "Bad data matrix", JOptionPane.ERROR_MESSAGE);
         else
         {
            currDS = ds;
            currRawData = currDS.copyRawData();
            datasetChanged = true;
            textDataChanged = false;
            refresh();
         }
      }
      else if(src == revertTextBtn)
      {
         if(isTableView) return;
         dataTextArea.setText(currDS.toPlainText());
         textDataChanged = false;
         refresh();
      }
      else if(src == viewBtn)
      {
         // if switching to text view, abort if dataset exceeds size restrictions
         if(isTableView)
         {
            Fmt fmt = currDS.getFormat();
            int nSz = (fmt == Fmt.RASTER1D) ? currDS.getDataLength() : currDS.getDataLength() * currDS.getDataBreadth();
            if(nSz > (MAXTEXTDATALEN / 5))
            {
               JOptionPane.showMessageDialog(this, "Data set's raw data matrix is too large to edit in text mode!", 
                        "Text mode disabled", JOptionPane.WARNING_MESSAGE);
               return;
            }
         }
         
         isTableView = !isTableView;
         viewBtn.setText(isTableView ? "Text" : "Table");
         tableView.setVisible(isTableView);
         textView.setVisible(!isTableView);
         
         // HACK. Before text area is made visible for the first time, it will be minimal height, and so we need to wait
         // for component layout (which is queued on the event dispatch thread upon calling setVisible()) before loading
         // it with tuples. Otherwise, we'll incorrectly calculate the number of fully visible lines in the text area.
         if((!isTableView) && (!textViewShown))
         {
            SwingUtilities.invokeLater(this::reload);
            textViewShown = true;
         }
         else
            reload();
      }
      else if(src == okBtn && datasetChanged)
      {
         // if any revisions have been made to the data, the revised data set becomes the original data set, the data
         // presentation node is updated accordingly, and the dialog remains up.
         // NOTE: In text view mode, any unapplied changes in the text are lost in this scenario.
         updateDataSetOnOwner();
         if(!isTableView) reload();
         else refresh();
      }
      else if(src == cancelBtn)
         cancel();
   }

   /**
    * If the data set on display has been changed by user interaction, this method will update the data presentation
    * node accordingly. 
    */
   private void updateDataSetOnOwner()
   {
      if(dataOwner == null || !datasetChanged) return;
      originalDS = currDS;
      currRawData = originalDS.copyRawData();
      currDS = DataSet.createDataSet(originalDS.getInfo(), currRawData);
      dataOwner.setDataSet(originalDS);
      datasetChanged = false;
      textDataChanged = false;
   }
   
   /**
    * Once the contents of the text area in the "Text View" have changed, the <i>Apply</i> and <i>Revert</i> buttons are
    * enabled, while the <i>OK</i> button and the view-toggling button are disabled. The combo box controlling dataset
    * format is also disabled, since the format should not be changed until the modified text content has been applied
    * to the dataset.
    */
   public void changedUpdate(DocumentEvent e) { removeUpdate(e); }
   public void insertUpdate(DocumentEvent e) { removeUpdate(e); }
   public void removeUpdate(DocumentEvent e)
   {
      if(ignoreActions) return;
      if(!textDataChanged)
      {
         textDataChanged = true;
         refresh();
      }
   }

   /**
    * Whenever the current selection in the data table changes, certain operations on the data table may become 
    * enabled or disabled. Here we refresh the corresponding toolbar buttons.
    */
   public void valueChanged(ListSelectionEvent e)
   {
      if(e.getSource() == dataTable.getSelectionModel() && !e.getValueIsAdjusting()) 
         refresh();
   }

   
   //
   // Inner-class helpers
   //
   
   /** 
    * Max length of string displayed in the "Text View", which restricts size of datasets that can be edited in that 
    * view mode.
    */
   private final static int MAXTEXTDATALEN = 250000;
   
   /**
    * This document filter restricts the size of the text that can be displayed and edited in the "Text View". This is
    * necessary because displaying and editing very large datasets as plain text is memory intensive and can easily
    * generate OOMs if the dataset size is not bounded.
    * @author sruffner
    */
   public static class MaxSizeFilter extends DocumentFilter
   {
      @Override 
      public void insertString(FilterBypass fb, int ofs, String string, AttributeSet attr) throws BadLocationException
      {
         replace(fb, ofs, 0, string, attr);
      }
      
      @Override 
      public void replace(FilterBypass fb, int ofs, int len, String text, AttributeSet attrs) throws BadLocationException
      {
         // calculate length of result string
         int currDocLen = fb.getDocument().getLength();
         int nextDocLen = currDocLen + (text != null ? text.length() : 0) - len;
         if(nextDocLen < MAXTEXTDATALEN)
            super.replace(fb, ofs, len, text, attrs);
         else
            Toolkit.getDefaultToolkit().beep();
      }
      
      @Override public void remove(FilterBypass fb, int offset, int length) throws BadLocationException
      {
         replace(fb, offset, length, "", null);
      }
   }

   /**
    * The table cell renderer for the data table. It displays a floating-point datum with up to 6 significant digits 
    * (per the specification for single-precision floating-point), formatting the value in exponential notation when 
    * necessary. Any value that is not a {@link Float} is displayed as an empty string.
    * @author sruffner
    */
   private static class FloatTCRenderer extends DefaultTableCellRenderer
   {
      public FloatTCRenderer()
      {
         super();
         setHorizontalAlignment(JLabel.RIGHT);
      }

      @Override protected void setValue(Object value)
      {
         String text = (value instanceof Float) ? Utilities.toString((Float)value, 6, -1) : "";
         setText(text);
      }
   }

   /**
    * This custom table cell editor is installed in place of the default number editor provided by <code>JTable</code>.
    * Its primary functional difference is that it automatically selects the underlying text field's contents when 
    * the editor is invoked, whether by a double-click of the mouse, the F2 key, or another keypress. In the case of the
    * keypress, the key character immediately replaces the selected text -- and this helps editing the table contents
    * more quickly and intuitively. The other functional difference is that, if the user enters a text string that 
    * cannot be parsed as a float value, the cell editor value is set to <i>Float.NaN</i> rather than keeping the cell
    * editor up displaying the bad string.
    *
    * <p>NOTE: The "select all on invocation" feature does NOT work in Mac OSX when cell editor is invoked by a 
    * double-click or by the F6 (instead of F2) key. However, one can immediately start typing printable characters, 
    * which is the critical functionality we need.</p>
    *
    * CREDITS: Adapted from the "select all" solution found
    * <a href="http://forums.java.net/jive/thread.jspa?threadID=42682&tstart=0">here</a>.
    */
   private static class SelectAllFloatEditor extends DefaultCellEditor
   {
      SelectAllFloatEditor() 
      { 
         super(new EditorTextField()); 
         ((JTextField)getComponent()).setHorizontalAlignment(JTextField.LEFT);
      }

      /**
       * Overridden to select all before calling super. This is for selecting the text after starting edits with
       * the mouse.
       * 
       * <p>NOTE: UI-delegates are not guaranteed to call this after starting the edit triggered with the mouse. On the 
       * other hand, not all are expected to dispatch the starting mouse event to the editing component (which is the 
       * event which destroys the selection done in addNotify).</p>
       */
      @Override public boolean shouldSelectCell(EventObject anEvent) 
      {
          ((EditorTextField)getComponent()).selectAll();
          return(super.shouldSelectCell(anEvent));
      }

      /**
       * Overridden to parse the text string in the underlying text-field component. The string is parsed as a float
       * value, which becomes this cell editor's current value (limited to 6 significant digits. If parsing fails, the
       * cell editor's value is set to {@link Float#NaN}. In either case, editing stops.
       */
      @Override public boolean stopCellEditing() 
      {
         String s = (String)super.getCellEditorValue();
         try { value = (float) Utilities.limitSignificantDigits(Float.parseFloat(s), 6); }
         catch(NumberFormatException nfe) 
         {
            value = Float.NaN;
         }
         return(super.stopCellEditing());
      }

      public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) 
      {
         this.value = null;
         
         // we only show first 6 significant digits
         float entry = (value instanceof Float) ? (Float) value : 0.0f;
         entry = (float) Utilities.limitSignificantDigits(entry, 6);
         
         ((JTextField)getComponent()).setBorder(new LineBorder(Color.black));
         Font f = getComponent().getFont();
         getComponent().setFont( f.deriveFont(Font.PLAIN, 8));
         return(super.getTableCellEditorComponent(table, entry, isSelected, row, column));
      }

      public Object getCellEditorValue() { return(value); }
     
      /** The floating-point value parsed from text field contents, or <code>null</code> if invalid. */
      private Float value = null;
      
      /** A customized text field that's used as the editor component for the enclosing cell editor. */
      private static class EditorTextField extends JTextField 
      {
         /** Overridden to select all. */ 
         @Override public void addNotify() { super.addNotify(); selectAll(); }
      }
   }

   /** The data table column names when the data set format is {@link Fmt#PTSET}. */
   private final static String[] PTSET_COLNAMES = new String[] {"x", "y", "yStd", "ye", "xStd", "xe"};
   /** The data table column names when the data set format is {@link Fmt#SERIES}. */
   private final static String[] SERIES_COLNAMES = new String[] {"y", "yStd", "ye"};
   /** The data table column names when the data set format is {@link Fmt#XYZSET}. */
   private final static String[] XYZSET_COLNAMES = new String[] {"x", "y", "z"};
   /** The data table column names when the data set format is {@link Fmt#XYZWSET}. */
   private final static String[] XYZWSET_COLNAMES = new String[] {"x", "y", "z", "w"};
   
   /**
    * <code>DataSetTM</code> serves as the model for the data table in the data set dialog. It is a lightweight wrapper 
    * around the raw data array of the data set currently displayed in the dialog. For all data formats except {@link 
    * Fmt#RASTER1D}, the table has N rows and M columns, where N is the number of tuples in the set and M is the
    * tuple length. For <b>RASTER1D</b>, each table row lists the samples in a single raster, the number of columns is 
    * equal to the length of the longest raster, and shorter rasters are padded with "blank" cells.
    * 
    * <p><code>DataSetTM</code> supports changing an individual datum in the array without having to create a new 
    * data set instance, which could involve a hefty heap allocation if the set is large. For details, see {@link 
    * #setValueAt(Object, int, int)}. In addition, it provides support for deleting, cutting, and copying the current
    * table selection, as well as pasting the system clip board contents (if possible) into the table. For more
    * information on these operations, see the enclosing class header.</p>
    * 
    * @author sruffner
    */
   private class DataSetTM extends AbstractTableModel
   {
      /** Contents of a table cell that does not correspond to a real datum: an empty string. */
      private static final String NULLDATUM = "";
      
      public int getColumnCount() { return(nCols); }
      public int getRowCount() { return(nRows);  }

      /**
       * Returns an empty string if the cell position is invalid, or if it is a valid table cell that does not contain
       * a datum (eg, to pad the cells of a row when presenting a <b>RASTER1D</b> dataset, which will typically have
       * rasters of different lengths). Otherwise, returns a {@link Float} extracted from the data set's internal raw 
       * data array IAW the row and column indices.
       */
      public Object getValueAt(int rowIndex, int columnIndex)
      {
         if(rowIndex < 0 || rowIndex >= getRowCount() || columnIndex < 0 || columnIndex >= getColumnCount())
            return(NULLDATUM);
         if(currDS == null || nRows == 0 || nCols == 0) return(NULLDATUM);

         int w = currDS.getDataBreadth();
         Object datum = NULLDATUM;
         if(currDS.getFormat() == Fmt.RASTER1D)
         {
            int n = (int) currRawData[rowIndex];
            if(columnIndex < n)
            {
               int start = nRows;
               for(int i = 0; i< rowIndex; i++) start += (int) currRawData[i];
               datum = currRawData[start + columnIndex];
            }
         }
         else
            datum = currRawData[rowIndex * w + columnIndex];
         return(datum);
      }
 
      /** All editable cells contain single-precision floating-point values. */
      @Override public Class<?> getColumnClass(int columnIndex) { return(Float.class);  }

      /**
       * Returns column names consistent with the data format of the current data set. For the <b>RASTER1D</b> and
       * <b>XYZIMG</b> formats, the column name is just <code>Integer.toString(column + 1)</code>.
       * @see TableModel#getColumnName(int)
       */
      @Override public String getColumnName(int column) 
      { 
         String colName = Integer.toString(column + 1);
         if(currDS != null)
         {
            Fmt fmt = currDS.getFormat();
            if(fmt == Fmt.PTSET && column >= 0 && column < PTSET_COLNAMES.length) 
               colName = PTSET_COLNAMES[column];
            else if(fmt == Fmt.MSET) 
            {
               if(column == 0) colName = "x";
               else colName = "y" + column;
            }
            else if(fmt == Fmt.MSERIES) colName = "y" + (column + 1);
            else if(fmt == Fmt.SERIES && column >= 0 && column < SERIES_COLNAMES.length) 
               colName = SERIES_COLNAMES[column];
            else if(fmt == Fmt.XYZSET && column >= 0 && column < XYZSET_COLNAMES.length)
               colName = XYZSET_COLNAMES[column];
            else if(fmt == Fmt.XYZWSET && column >= 0 && column < XYZWSET_COLNAMES.length)
               colName = XYZWSET_COLNAMES[column];
         }
         return(colName); 
      }

      /**
       * For all data formats except <b>RASTER1D</b>, any valid cell can be edited. For <b>RASTER1D</b>, any cell that 
       * corresponds to an actual raster sample value may be edited; since individual rasters in the set will typically 
       * have different lengths, there will typically be some blank read-only cells in this case.
       */
      @Override public boolean isCellEditable(int rowIndex, int columnIndex)
      {
         if(rowIndex < 0 || rowIndex >= getRowCount() || columnIndex < 0 || columnIndex >= getColumnCount())
            return(false);
         if(currDS == null || nRows == 0 || nCols == 0) return(false);
         
         return((currDS.getFormat() != Fmt.RASTER1D) || (columnIndex < (int)currRawData[rowIndex]));
      }

      /**
       * Updates the element in the current data set's raw data array that corresponds to the specified row and column
       * indices, refreshes the preview canvas to reflect the change (but: single datum changes may not be seen in the
       * preview if the data set is large!), and notifies all registered listeners that a table cell has changed.
       * 
       * <p><b>NOTE</b>. {@link DataSet} is an immutable object, so it is not possible to change its data array via an
       * instance method call. Instead, the enclosing class maintains a reference to the raw data array that is passed 
       * to the method that creates the displayed data set in its current state. This method changes an element in that 
       * array, then passes the array to the appropriate "create" method to update the data set without having to 
       * reallocate the raw data array. This approach avoids potentially large and frequent heap allocations should the 
       * user need to edit numerous individual data in a very large set -- even though it is unlikely the data set edit
       * dialog will be used this way.</p>
       */
      @Override public void setValueAt(Object value, int rowIndex, int columnIndex)
      {
         if(currDS == null || !(value instanceof Float) || rowIndex < 0 || rowIndex >= getRowCount() || 
               columnIndex < 0 || columnIndex >= getColumnCount())
            return;

         int w = currDS.getDataBreadth();
         float f = (Float) value;
         
         if(currDS.getFormat() == Fmt.RASTER1D)
         {
            int n = (int) currRawData[rowIndex];
            if(columnIndex < n)
            {
               int start = nRows;
               for(int i = 0; i< rowIndex; i++) start += (int) currRawData[i];
               if(currRawData[start + columnIndex] == f) return;
               currRawData[start + columnIndex] = f;
            }
         }
         else 
         {
            if(currRawData[rowIndex * w + columnIndex] == f) return;
            currRawData[rowIndex * w + columnIndex] = f;
         }

         // create a new dataset using the just-modified raw data array
         currDS = DataSet.createDataSet(currDS.getInfo(), currRawData);

         // refresh rest of dialog
         datasetChanged = true;
         refresh();

         fireTableCellUpdated(rowIndex, columnIndex);
      }

      /**
       * Reload the table model from scratch to reflect the contents and definition of the data set currently being 
       * edited in the data set dialog. The total number of rows and columns in the table are recomputed based on the 
       * current data format, length and breadth; then all registered listeners are notified that the table structure 
       * has changed completely.
       * 
       * <p>For a {@link Fmt#RASTER1D} set, the number of rows in the table is equal to the number of individual
       * rasters, while the number of columns is the length of the longest raster. For all other formats, the number of
       * rows is the number of tuples in the data set and the number of columns is the tuple length.</p>
       */
      void reload()
      {
         nRows = 0;
         nCols = 0;
         if(currDS != null)
         {
            if(currDS.getFormat() == Fmt.RASTER1D)
            {
               nRows = currDS.getDataBreadth();
               for(int i=0; i<nRows; i++) nCols = Math.max(nCols, currDS.getDataSize(i));
            }
            else
            {
               nRows = currDS.getDataLength();
               nCols = currDS.getDataBreadth();
            }
            
            if(nCols == 0) nCols = 1;
         }
         fireTableStructureChanged();
         rowHeaderTableModel.fireTableStructureChanged();
      }

      /**
       * Can the current selection within the data table be copied? There are only a couple of instances in which the
       * copy operation is disabled: (1) the current selection is changing; (2) the current selection is empty; (3)
       * the current data format is <b>RASTER1D</b> and the current selection spans multiple rows (rasters) but does
       * not span the table column-wise; or (4) the current data set is <b>RASTER1D</b>, the current selection covers a
       * single row but does not cover any actual samples in the selected raster.
       * @return True if copying of the current selection is possible, as described.
       */
      boolean canCopyCurrentSelection()
      {
         if(dataTable.getSelectionModel().getValueIsAdjusting() || dataTable.getSelectionModel().isSelectionEmpty()) 
            return(false);
         
         // get row selection
         int firstRow = dataTable.getSelectedRow();
         int nr = dataTable.getSelectedRowCount();
         if(firstRow < 0 || firstRow>=nRows) nr = 0;
         if(firstRow + nr > nRows) nr = nRows - firstRow;

         // get column selection
         int firstCol = dataTable.getSelectedColumn();
         int nc = dataTable.getSelectedColumnCount();
         if(firstCol < 0 || firstCol >= nCols) nc = 0;
         if(firstCol + nc > nCols) nc = nCols - firstCol;

         boolean ok = true;
         if(currDS.getFormat() == Fmt.RASTER1D)
         {
            if(nr > 1) ok = (nc == nCols);
            else ok = (firstCol < currDS.getDataSize(firstRow));
         }
         return(ok);
      }
      
     /**
       * Prepare an independent copy of the chunk of raw data encompassed by the current selection in the data table. 
       * 
       * <p>This method supports copying or cutting the current selection to the system clip board in a manner which is
       * faster and more memory-efficient than storing it in text form. The first three elements of the prepared array 
       * hold important information about the "shape" of the raw data chunk:</p>
       * <ul>
       * <li>First element is a flag. A nonzero value indicates that data chunk contains <b>RASTER1D</b> data, which is
       * formatted very differently from the other <i>DataNav</i> data formats.</li>
       * <li>Second element is the data length (cast to an integer).</li>
       * <li>Third element is the data breadth (cast to an integer).</li>
       * </ul>
       * <p>The remainder of the array is the data chunk itself, formatted in exactly the same manner as the raw data
       * array of the complete <i>DataNav</i> data set from which the chunk is extracted. The information listed above 
       * is all that is needed to correctly traverse the data chunk.</p>
       * 
       * <p><b>NOTE</b>. It does not make sense to copy a multi-row cell block of <b>RASTER1D</b> unless the block
       * spans the table width so that you're copying two or more complete rasters. Thus, when the current data set 
       * contains raster data, this method returns null when the current selection includes more than one row but does 
       * not span the table column-wise. It is considered OK to copy/cut a contiguous block out of a single raster.</p>
       * 
       * @return An array holding a copy of the currently selected data, formatted as described. If the current 
       * selection is empty, or in the special case described above, returns null.
       */
      float[] getCurrentSelectionAsDataChunk()
      {
         // get row selection
         int firstRow = dataTable.getSelectedRow();
         int nr = dataTable.getSelectedRowCount();
         if(firstRow < 0 || firstRow>=nRows) nr = 0;
         if(firstRow + nr > nRows) nr = nRows - firstRow;

         // get column selection 
         int firstCol = dataTable.getSelectedColumn();
         int nc =  dataTable.getSelectedColumnCount();
         if(firstCol < 0 || firstCol >= nCols) nc = 0;
         if(firstCol + nc > nCols) nc = nCols - firstCol;
         
         float[] chunk;
         if(nr == 0 || nc == 0)
            chunk = null;
         else if(currDS.getFormat() == Fmt.RASTER1D)
         {
            if(nr == 1)
            {
               // single raster selected. Copy all or a portion of that raster. It's possible that the selected portion
               // is after the end of the raster, in which case there's nothing to copy!
               int nSkip = 0; for(int i=0; i<firstRow; i++) nSkip += (int) currRawData[i];
               int nCopy = ((int) currRawData[firstRow]) - firstCol;
               if(nCopy <= 0) return(null);
               else if(nCopy > nc) nCopy = nc;
               
               chunk = new float[3 + nr + nCopy];
               chunk[0] = 1;
               chunk[1] = nCopy;
               chunk[2] = nr;
               chunk[3] = nCopy;
               System.arraycopy(currRawData, nRows+nSkip+firstCol, chunk, 3+nr, nCopy);
            }
            else
            {
               // multiple rasters selected. Fail if selection does not span table width
               if(nCols  != nc) return(null);
               
               int nSkip = 0; for(int i=0; i<firstRow; i++) nSkip += (int) currRawData[i];
               int nCopy = 0; for(int i=firstRow; i<firstRow+nr; i++) nCopy += (int) currRawData[i];
               
               chunk = new float[3 + nr + nCopy];
               chunk[0] = 1;
               chunk[1] = nCopy;
               chunk[2] = nr;
               if(firstRow + nr - firstRow >= 0)
                  System.arraycopy(currRawData, firstRow, chunk, 3 + firstRow - firstRow, firstRow + nr - firstRow);
               System.arraycopy(currRawData, nRows+nSkip, chunk, 3+nr, nCopy);
            }
         }
         else if(nc == nCols)
         {
            // easy case. We copy a contiguous portion of the dataset's raw data array (or the entire array).
            chunk = new float[3 + nr*nCols];
            chunk[0] = 0;
            chunk[1] = nr;
            chunk[2] = nCols;
            System.arraycopy(currRawData, firstRow*nCols, chunk, 3, nr*nCols);
         }
         else
         {
            // harder case. We have to copy the selected columns within each selected row separately.
            chunk = new float[3 + nr*nc];
            chunk[0] = 0;
            chunk[1] = nr;
            chunk[2] = nc;
            int srcOffset = firstCol + firstRow*nCols;
            int dstOffset = 3;
            for(int i=0; i<nr; i++)
            {
               System.arraycopy(currRawData, srcOffset, chunk, dstOffset, nc);
               srcOffset += nCols;
               dstOffset += nc;
            }
         }
         return(chunk);
      }
      
      /**
       * Alter the displayed data set by removing the chunk of raw data currently selected within the tabular view of
       * this data model. The action taken depends on what is selected:
       * <ul>
       * <li>If the table selection is empty or currently being adjusted, no action is taken.</li>
       * <li>If the current data format is <b>RASTER1D</b>...
       *    <ol>
       *    <li>and the selection spans the table column-wise, the selected rows (rasters) are removed.</li>
       *    <li>and the selection does NOT span the table width, removal is disallowed unless only a single row is 
       *    selected, in which case the selected samples are removed from the corresponding raster.</li>
       *    </ol>
       * </li>
       * <li>For all other data formats:
       *    <ol>
       *    <li>The selection spans the entire width of the table (all columns selected), in which case the selected 
       *    rows are removed.</li>
       *    <li>The selection spans the entire height of the table (all rows selected), in which case the selected 
       *    columns are removed ONLY if the number of columns remaining is either zero (empty set) or satisfies the 
       *    dataset's format restrictions.</li>
       *    <li>The contiguous block of cells selected spans neither the height nor width of the table. No rows or
       *    columns are removed in this case, but all selected cells are cleared to <i>Float.NaN</i>.</li>
       *    </ol>
       * </li>
       * </ul>
       * @return True if operation succeeded; false otherwise.
       */
      boolean removeCurrentSelectionIfPossible()
      {
         if(dataTable.getSelectionModel().getValueIsAdjusting() || dataTable.getSelectionModel().isSelectionEmpty()) 
            return(false);
         
         // get row selection
         int firstRow = dataTable.getSelectedRow();
         int nr = dataTable.getSelectedRowCount();
         if(firstRow < 0 || firstRow>=nRows) nr = 0;
         if(firstRow + nr > nRows) nr = nRows - firstRow;

         // get column selection
         int firstCol = dataTable.getSelectedColumn();
         int nc = dataTable.getSelectedColumnCount();
         if(firstCol < 0 || firstCol >= nCols) nc = 0;
         if(firstCol + nc > nCols) nc = nCols - firstCol;
         
         // just in case
         if(nr == 0 || nc == 0) return(false);
         
         Fmt currFmt = currDS.getFormat();
         
         // case 1: Entire table selected.
         if(nr == nRows && nc == nCols)
         {
            currRawData = new float[0];
            currDS = DataSet.createDataSet(currDS.getID(), currFmt, currDS.getParams(), 0, 0, currRawData);
         }
         
         // case 2: Raster dataset with selection spanning table width -- remove selected rasters.
         else if(currFmt == Fmt.RASTER1D && nc == nCols)
         {
            int deletedLen = 0;
            for(int i=firstRow; i<firstRow+nr; i++) deletedLen += (int) currRawData[i];
            
            float[] raw = new float[currRawData.length - deletedLen - nr];
            int nSamples = 0;
            for(int i=0; i<firstRow; i++)
            {
               raw[i] = currRawData[i];
               nSamples += (int) currRawData[i];
            }
            if(nSamples > 0) System.arraycopy(currRawData, nRows, raw, nRows-nr, nSamples);
            int srcOffset = nRows + nSamples + deletedLen;
            int dstOffset = nRows-nr + nSamples;
            
            int j = firstRow;
            int nSamplesRem = nSamples;
            nSamples = 0;
            for(int i=firstRow+nr; i<nRows; i++)
            {
               raw[j++] = currRawData[i];
               nSamples += (int) currRawData[i];
            }
            if(nSamples > 0) System.arraycopy(currRawData, srcOffset, raw, dstOffset, nSamples);
            nSamplesRem += nSamples;
            
            DataSet ds = DataSet.createDataSet(currDS.getID(), Fmt.RASTER1D, null, nSamplesRem, nRows-nr, raw);
            if(ds == null) return(false);
            currRawData = raw;
            currDS = ds;
         }
         
         // case 2a: Raster dataset, selection DOES NOT span table width. Abort if more than one row selected. Else, 
         // remove only those raster samples that are selected (if any). Note that #rasters stays the same; even if all
         // the samples in the selected raster are removed, that raster remains in the dataset, though empty.
         else if(currFmt == Fmt.RASTER1D && nc < nCols)
         {
            if(nr > 1) return(false);
            
            // find # actual raster samples covered by the contiguous selection; only these samples are removed.
            int nDel = ((int) currRawData[firstRow]) - firstCol;
            if(nDel <= 0) return(false);
            else if(nDel > nc) nDel = nc;
            
            // copy raster lengths, then update the length of the affected raster
            float[] raw = new float[currRawData.length - nDel];
            System.arraycopy(currRawData, 0, raw, 0, nRows);
            raw[firstRow] = ((int)raw[firstRow]) - nDel;
            
            // copy all raster samples up to the first deleted sample in the selected raster
            int nSamples = firstCol; for(int i=0; i<firstRow; i++) nSamples += (int) currRawData[i];
            if(nSamples > 0) System.arraycopy(currRawData, nRows, raw, nRows, nSamples);
            
            // copy all raster samples in the dataset after the last deleted sample in the selected raster
            int dstOffset = nRows + nSamples;
            nSamples = ((int)currRawData[firstRow]) - firstCol - nDel;
            for(int i=firstRow+1; i<nRows; i++) nSamples += (int) currRawData[i];
            if(nSamples > 0) System.arraycopy(currRawData, dstOffset+nDel, raw, dstOffset, nSamples);
            
            nSamples = currDS.getDataLength() - nDel;
            DataSet ds = DataSet.createDataSet(currDS.getID(), Fmt.RASTER1D, null, nSamples, nRows, raw);
            if(ds == null) return(false);
            currRawData = raw;
            currDS = ds;
         }
         
         // case 3: Non-raster data and selection spans table width. Remove selected rows.
         else if(nc == nCols)
         {
            int offset = firstRow*nCols;
            float[] raw = new float[currRawData.length - nCols*nr];
            if(firstRow > 0) System.arraycopy(currRawData, 0, raw, 0, offset);
            if((firstRow + nr) < nRows)
               System.arraycopy(currRawData, offset+nCols*nr, raw, offset, (nRows-firstRow-nr)*nCols);
            
            DataSet ds = DataSet.createDataSet(currDS.getID(), currFmt, currDS.getParams(), nRows-nr, nCols, raw);
            if(ds == null) return(false);
            currRawData = raw;
            currDS = ds;
         }
         
         // case 4: Non-raster data and selection spans table height. Remove selected columns if possible.
         else if(nr == nRows)
         {
            if(!currFmt.isValidDataBreadth(nCols-nc)) return(false);
            
            float[] raw = new float[currRawData.length - nRows*nc];
            int offsetOld = 0;
            int offsetNew = 0;
            for(int i=0; i<nRows; i++)
            {
               if(firstCol >= 0) System.arraycopy(currRawData, offsetOld, raw, offsetNew, firstCol);
               if(nCols - (firstCol + nc) >= 0)
                  System.arraycopy(currRawData, offsetOld + firstCol + nc, raw, offsetNew + firstCol + nc - nc, nCols - (firstCol + nc));
               offsetOld += nCols;
               offsetNew += (nCols - nc);
            }
            
            DataSet ds = DataSet.createDataSet(currDS.getID(), currFmt, currDS.getParams(), nRows, nCols-nc, raw);
            if(ds == null) return(false);
            currRawData = raw;
            currDS = ds;
         }
         
         // case 5: Non-raster data and selection spans neither width nor height of table. No rows or columns are
         // removed, but selected cells are reset to Float.NaN
         else
         {
            int offset = firstRow*nCols;
            for(int i=0; i<nr; i++)
            {
               for(int j=firstCol; j<firstCol+nc; j++) currRawData[offset+j] = Float.NaN;
               offset += nCols;
            }
            currDS = DataSet.createDataSet(
                  currDS.getID(), currDS.getFormat(), currDS.getParams(), nRows, nCols, currRawData);
         }
         
         // reload the table model because #rows or #cols (or both) may have changed
         reload();
         
         // refresh rest of dialog
         datasetChanged = true;
         refresh();
         return(true);
      }
      
      /**
       * Can the current selection within the data table be removed without violating the data set's format constraints?
       * <ul>
       *    <li>If the current selection is empty or changing, removal is not possible. Otherwise...</li>
       *    <li>If current data set is <b>RASTER1D</b> and the selection spans multiple rows, removal is allowed only if
       *    the selection spans the table width. If the selection includes a single row, removal is allowed as long as 
       *    the selection includes at least one valid sample in the selected raster. Otherwise...</li>
       *    <li>Removal is disabled only when the current selection spans all rows but only a subset of columns. In this
       *    case the columns would be deleted, and we must ensure that the number of columns remaining is either zero
       *    (empty data set) or does not violate data set format restrictions.</li>
       * </ul>
       * @return True only if removal of the current selection is possible, as described.
       */
      boolean canRemoveCurrentSelection()
      {
         if(dataTable.getSelectionModel().getValueIsAdjusting() || dataTable.getSelectionModel().isSelectionEmpty()) 
            return(false);
         
         // get row selection
         int firstRow = dataTable.getSelectedRow();
         int nr = dataTable.getSelectedRowCount();
         if(firstRow < 0 || firstRow>=nRows) nr = 0;
         if(firstRow + nr > nRows) nr = nRows - firstRow;

         // get column selection
         int firstCol = dataTable.getSelectedColumn();
         int nc = dataTable.getSelectedColumnCount();
         if(firstCol < 0 || firstCol >= nCols) nc = 0;
         if(firstCol + nc > nCols) nc = nCols - firstCol;

         boolean ok = true;
         if(currDS.getFormat() == Fmt.RASTER1D)
         {
            if(nr > 1) ok = (nc == nCols);
            else ok = (firstCol < currDS.getDataSize(firstRow));
         }
         else if(nr == nRows && nc < nCols)
            ok = currDS.getFormat().isValidDataBreadth(nCols-nc);
         
         return(ok);
      }
      
      /**
       * Insert a chunk of data into the data set currently encapsulated by this table model and displayed in the data
       * set edit dialog's tabular view, optionally replacing the current table selection. If the data chunk is not the 
       * right shape, it will be truncated or padded with <i>Float.NaN</i> as needed. The action taken depends on what 
       * is selected and whether or not it is being replaced:
       * <ul>
       * <li>If the table selection is currently being adjusted, no action is taken.</li>
       * <li>If the data set is empty or is to be replaced in its entirety, the data chunk becomes the new data set -- 
       * though it may be truncated or padded with <i>Float.NaN</i> to satisfy the breadth constraints of the current
       * data format.</li>
       * <li>If the current data set's format is <b>RASTER1D</b>, there are two distinct possibilities:
       *    <ol>
       *    <li>If the selection spans the entire table column-wise, the entire data chunk is pasted in before the first 
       *    selected row, and the selected rows are removed if the <i>replace</i> flag is set. No padding or truncation 
       *    needed.</li>
       *    <li>If the selection does not span the entire table column-wise, no action is taken UNLESS only a single row
       *    is selected, and the selected block of cells includes at least one valid raster sample. In this case, the 
       *    operation will only affect the selected raster. The first chunk row -- all other chunk rows are ignored -- 
       *    is pasted into the raster before the first selected raster sample, and the selected raster samples are 
       *    removed if the <i>replace</i> flag is set.</li>
       *    </ol>
       * <li>For all other formats, the raw data matrix is rectangular, and there are three distinct kinds of table
       * selections:
       *    <ol>
       *    <li>The selection spans the entire width of the table (all columns selected). The chunk rows are pasted in 
       *    before the first selected row, truncated or padded column-wise as necessary. The number of columns in the 
       *    altered dataset will be unchanged. If the <i>replace</i> flag is set, the selected rows are removed.</li>
       *    <li>The selection spans the entire height of the table (all rows selected). The chunk columns are pasted in
       *    before the first selected column, truncated or padded row-wise as necessary. The number of rows in the
       *    altered dataset will be unchanged. If the <i>replace</i> flag is set, the selected columns are removed.
       *    <b>Exception</b>: If the number of columns that result after the insert (and optional replace) would violate
       *    the breadth constraints of the current data format (eg., the breadth of a <code>PTSET</code> is 2-6), 
       *    then the chunk will also be truncated or padded column-wise as needed. If the number of columns is already 
       *    at the maximum allowed and the current selection is not to be replaced, then the insertion is not possible 
       *    and the method fails.</li>
       *    <li>The contiguous block of cells selected spans neither the height nor width of the table. If the 
       *    <i>replace</i> flag is set, then the chunk replaces the selected block of cells, truncated or padded both 
       *    row-wise and column-wise as needed. If not, the chunk rows are pasted in before the selected row, 
       *    truncated or padded column-wise as necessary to match the current table width.</li>
       *    </ol>
       * </li>
       * </ul>
       * <p>If the operation is successful, the inserted block of data becomes the current selection in the table.</p>
       * 
       * @param data The chunk of data to be inserted. This array must be formatted in the same manner as that prepared
       * by {@link #getCurrentSelectionAsDataChunk()}; otherwise, the operation fails.
       * @param replace If true, the currently selected chunk of table data is replaced by the chunk provided, in the 
       * manner described.
       * @return True if operation was successful. 
       */
      boolean insertDataChunk(float[] data, boolean replace)
      {
         // verify structure of data chunk array
         if(data == null || data.length < 3) return(false);
         boolean isRasterChunk = (data[0] != 0);
         int nr = (int) data[1];
         int nc = (int) data[2];
         int dataSize = (isRasterChunk) ? nr + nc : nr*nc;
         if(data.length != 3 + dataSize) return(false);

         int nMaxRasterLen = 0;
         if(isRasterChunk) 
         {
            int nTotal = 0;
            for(int i=0; i<nc; i++)
            {
               int nSamps = (int) data[3+i];
               if(nMaxRasterLen < nSamps) nMaxRasterLen = nSamps;
               nTotal += nSamps;
            }
            if(nTotal != nr) return(false);
         }
         
         
         // get selected rows or selected cols. If there's no current selection, fail immediately -- the selection 
         // defines the insertion point!
         Fmt currFmt = currDS.getFormat();
         boolean isFullReplace;
         int rSelStart;
         int nrSel;
         int cSelStart;
         int ncSel;

         
         if(dataTable.getSelectionModel().getValueIsAdjusting()) return(false);
         else if(currDS.isEmpty())
         {
            // SPECIAL CASE: paste always allowed when dataset is empty!
            isFullReplace = true;
            rSelStart = nrSel = cSelStart = ncSel = 0;
         }
         else
         {
            rSelStart = dataTable.getSelectedRow();
            nrSel = dataTable.getSelectedRowCount();
            if(rSelStart < 0 || rSelStart>=nRows) nrSel = 0;
            if(rSelStart + nrSel > nRows) nrSel = nRows - rSelStart;
            if(nrSel == 0) return(false);
            
            cSelStart = dataTable.getSelectedColumn();
            ncSel = dataTable.getSelectedColumnCount();
            if(cSelStart < 0 || cSelStart>=nCols) ncSel = 0;
            if(cSelStart + ncSel > nCols) ncSel = nCols - cSelStart;
            if(ncSel == 0) return(false);
            
            isFullReplace = replace && (nrSel == nRows) && (ncSel == nCols);
         }
         

         // case1: Paste into raster dataset when selection spans table column-wise (or dataset is empty). Use ALL of 
         // chunk data. 
         if(currFmt == Fmt.RASTER1D && (isFullReplace || ncSel == nCols))
         {
            int nCurrRasters = currDS.getDataBreadth();
            int nCurrSamples = currDS.getDataLength();
            
            int nDelRasters = replace ? nrSel : 0;
            int nDelSamps = 0;
            if(replace) for(int i=rSelStart; i<rSelStart+nrSel; i++) nDelSamps += (int) currRawData[i];
            
            int nAddSamps = isRasterChunk ? nr : nr*nc;
            int nAddRasters = isRasterChunk ? nc : nr;
            
            int nNetRasters = nCurrRasters - nDelRasters + nAddRasters;
            int nNetSamples = nCurrSamples - nDelSamps + nAddSamps;
            float[] raw = new float[nNetRasters + nNetSamples];

            // copy existing rasters preceding the selection
            int nSamples = 0;
            for(int i=0; i<rSelStart; i++)
            {
               raw[i] = currRawData[i];
               nSamples += (int) currRawData[i];
            }
            if(nSamples > 0) System.arraycopy(currRawData, nCurrRasters, raw, nNetRasters, nSamples);
            int srcOffset = nCurrRasters + nSamples + nDelSamps;
            int dstOffset = nNetRasters + nSamples;
            
            // copy rasters from chunk data provided
            if(isRasterChunk)
            {
               if(nAddRasters >= 0) System.arraycopy(data, 3, raw, rSelStart, nAddRasters);
               System.arraycopy(data, 3+nAddRasters, raw, dstOffset, nAddSamps);
            }
            else
            {
               for(int i=0; i<nAddRasters; i++) raw[rSelStart+i] = nc;
               System.arraycopy(data, 3, raw, dstOffset, nAddSamps);
            }
            dstOffset += nAddSamps;

            // now copy remaining rasters in original array, excluding current selection if it's being replaced.
            nSamples = 0;
            int j = rSelStart + nAddRasters;
            for(int i=rSelStart+nDelRasters; i<nCurrRasters; i++)
            {
               raw[j++] = currRawData[i];
               nSamples += (int) currRawData[i];
            }
            if(nSamples > 0) System.arraycopy(currRawData, srcOffset, raw, dstOffset, nSamples);
            
            // prepare the new dataset accordingly
            DataSet ds = DataSet.createDataSet(currDS.getID(), Fmt.RASTER1D, null, nNetSamples, nNetRasters, raw);
            if(ds == null) return(false);
            currRawData = raw;
            currDS = ds;
            
            // update table display, and select what has been pasted in!
            reload();
            dataTable.changeSelection(rSelStart, 0, false, false);
            dataTable.setAutoscrolls(false);
            dataTable.changeSelection(rSelStart+nAddRasters-1, nCols-1, false, true);
            dataTable.setAutoscrolls(true);

            // refresh rest of dialog
            datasetChanged = true;
            refresh();

            return(true);
         }
         
         // case 1a: Paste into raster dataset when selection does NOT span table column-wise. Abort if selection spans
         // more than one row, or if the first cell selected does not correspond to a valid raster sample. Otherwise, 
         // insert first row of chunk before this raster sample, and remove the selected raster samples if the replace
         // flag is set.
         if(currFmt == Fmt.RASTER1D && ncSel < nCols)
         {
            if(nrSel > 1) return(false);
            
            int nCurrRasters = currDS.getDataBreadth();
            int nCurrSamples = currDS.getDataLength();
            
            // find # raster samples covered by the contiguous selection; only these samples are removed in a replace 
            // paste op. If no valid samples are in the selection, abort.
            int nDelSamples = ((int) currRawData[rSelStart]) - cSelStart;
            if(nDelSamples <= 0) return(false);
            
            if(!replace) nDelSamples = 0;
            else if(nDelSamples > ncSel) nDelSamples = ncSel;

            // # samples to be inserted is length of first chunk row. If that first row has no data and we're doing an
            // insert, then abort because no change occurs.
            int nAddSamples = isRasterChunk ? ((int) data[3]) : nc;
            if(nAddSamples == 0 && !replace) return(false);
            
            // allocate the new raster data array
            int nNetSamples = nCurrSamples - nDelSamples + nAddSamples;
            float[] raw = new float[nCurrRasters + nNetSamples];

            // copy existing raster lengths, and update the length of the affected raster
            System.arraycopy(currRawData, 0, raw, 0, nCurrRasters);
            raw[rSelStart] = ((int) currRawData[rSelStart]) - nDelSamples + nAddSamples;
            
            // copy existing rasters preceding the selection
            int nSamples = 0; for(int i=0; i<rSelStart; i++) nSamples += (int) currRawData[i];
            if(nSamples > 0) System.arraycopy(currRawData, nCurrRasters, raw, nCurrRasters, nSamples);
            int srcOffset = nCurrRasters + nSamples;
            int dstOffset = srcOffset;
            
            // update the selected raster: copy existing samples up to the first selected raster sample, then insert
            // data from the first chunk row, then copy remaining samples in the raster, excluding the selected samples
            // if we're doing a replace paste.
            if(cSelStart > 0)
            {
               System.arraycopy(currRawData, srcOffset, raw, dstOffset, cSelStart);
               srcOffset += cSelStart;
               dstOffset += cSelStart;
            }
            
            System.arraycopy(data, isRasterChunk ? (3+nc) : 3, raw, dstOffset, nAddSamples);
            dstOffset += nAddSamples;

            srcOffset += nDelSamples;
            nSamples = ((int) currRawData[rSelStart]) - cSelStart - nDelSamples;
            if(nSamples > 0)
            {
               System.arraycopy(currRawData, srcOffset, raw, dstOffset, nSamples);
               dstOffset += nSamples;
               srcOffset += nSamples;
            }
            
            // now copy remaining rasters from the original array, if any.
            nSamples = 0; for(int i=rSelStart+1; i<nCurrRasters; i++) nSamples += (int) currRawData[i];
            if(nSamples > 0) System.arraycopy(currRawData, srcOffset, raw, dstOffset, nSamples);
            
            // prepare the new dataset accordingly
            DataSet ds = DataSet.createDataSet(currDS.getID(), Fmt.RASTER1D, null, nNetSamples, nCurrRasters, raw);
            if(ds == null) return(false);
            currRawData = raw;
            currDS = ds;
            
            // update table display, and select what has been pasted in!
            reload();
            dataTable.changeSelection(rSelStart, cSelStart, false, false);
            if(nAddSamples > 1) 
            {
               dataTable.setAutoscrolls(false);
               dataTable.changeSelection(rSelStart, cSelStart+nAddSamples-1, false, true);
               dataTable.setAutoscrolls(true);
            }

            // refresh rest of dialog
            datasetChanged = true;
            refresh();

            return(true);
         }
         
         // case 2: Non-raster dataset being entirely replaced. Use all chunk rows, but pad or truncate columns if nec.
         if(isFullReplace)
         {
            float[] raw;
            DataSet ds;
            if(isRasterChunk)
            {
               // convert raster chunk data into non-raster data...
               int nColsRes = currFmt.restrictToValidDataBreadth(nMaxRasterLen);

               raw = new float[nc *nColsRes];
               int dstOffset = 0;
               int srcOffset = 3 + nc;
               for(int i = 0; i< nc; i++)
               {
                  int nSamps = (int) data[3+i];
                  int nCopy = Math.min(nColsRes, nSamps);
                  System.arraycopy(data, srcOffset, raw, dstOffset, nCopy);
                  while(nCopy < nColsRes) { raw[dstOffset+nCopy] = Float.NaN; ++nCopy; }
                  srcOffset += nSamps;
                  dstOffset += nColsRes;
               }
               ds = DataSet.createDataSet(currDS.getID(), currFmt, currDS.getParams(), nc, nColsRes, raw);
            }
            else
            {
               int nColsRes = currFmt.restrictToValidDataBreadth(nc);
               if(nColsRes == nc)
               {
                  raw = new float[data.length - 3];
                  System.arraycopy(data, 3, raw, 0, raw.length);
                  ds = DataSet.createDataSet(currDS.getID(), currFmt, currDS.getParams(), nr, nc, raw);
               }
               else
               {
                  raw = new float[nr*nColsRes];
                  int srcOffset = 3;
                  int dstOffset = 0;
                  int nCopy = Math.min(nc, nColsRes);
                  for(int i=0; i<nr; i++)
                  {
                     System.arraycopy(data, srcOffset, raw, dstOffset, nCopy);
                     for(int j=nCopy; j<nColsRes; j++) raw[dstOffset+j] = Float.NaN;
                     dstOffset += nColsRes;
                     srcOffset += nc;
                  }
                  ds = DataSet.createDataSet(currDS.getID(), currFmt, currDS.getParams(), nr, nColsRes, raw);
               }
            }
            
            // update the dataset if successful
            if(ds == null) return(false);
            currRawData = raw;
            currDS = ds;
            
            // update table display, and select what has been pasted in!
            reload();
            dataTable.selectAll();
            dataTable.scrollRectToVisible(dataTable.getCellRect(0, 0, true));

            // refresh rest of dialog
            datasetChanged = true;
            refresh();

            return(true);
         }
         
         
         // case 3: Selection spans table width, non-raster dataset. 
         // case 5b: Selection spans neither table width nor height, non-raster dataset, and selection NOT replaced.
         // In both of these cases, all chunk rows are inserted, truncated or padded column-wise if necessary
         // !! Number of columns in dataset does NOT change here !!
         if((ncSel == nCols) || (ncSel < nCols && nrSel < nRows && !replace))
         {
            int nDelRows = replace ? nrSel : 0;
            int nAddRows = isRasterChunk ? nc : nr;
            
            int nTotalRows = nRows - nDelRows + nAddRows;
            float[] raw = new float[nTotalRows*nCols];
            
            // copy existing data before the selection
            if(rSelStart > 0) 
               System.arraycopy(currRawData, 0, raw, 0, rSelStart*nCols);
            
            // copy existing data after the selection
            if(rSelStart + nDelRows < nRows)
            {
               int nRem = (nRows - rSelStart - nDelRows)*nCols;
               System.arraycopy(currRawData, (rSelStart+nDelRows)*nCols, raw, (rSelStart+nAddRows)*nCols, nRem);
            }
            
            // copy chunk data in between, padded/truncated as necessary
            if(isRasterChunk)
            {
               int srcOffset = 3 + nAddRows;
               int dstOffset = rSelStart*nCols;
               for(int i=0; i<nAddRows; i++)
               {
                  int nSamps = (int) data[3+i];
                  int nCopy = Math.min(nCols, nSamps);
                  System.arraycopy(data, srcOffset, raw, dstOffset, nCopy);
                  while(nCopy < nCols) { raw[dstOffset+nCopy] = Float.NaN; ++nCopy; }
                  srcOffset += nSamps;
                  dstOffset += nCols;
               }
            }
            else
            {
               int srcOffset = 3;
               int dstOffset = rSelStart*nCols;
               int nCopy = Math.min(nc, nCols);
               for(int i=0; i<nAddRows; i++)
               {
                  System.arraycopy(data, srcOffset, raw, dstOffset, nCopy);
                  for(int j=nCopy; j<nCols; j++) raw[dstOffset+j] = Float.NaN;
                  dstOffset += nCols;
                  srcOffset += nc;
               }
            }
            
            // prepare the new dataset accordingly
            DataSet ds = DataSet.createDataSet(currDS.getID(), currFmt, currDS.getParams(), nTotalRows, nCols, raw);
            if(ds == null) return(false);
            currRawData = raw;
            currDS = ds;
            
            // update table display, and select what has been pasted in!
            reload();
            dataTable.changeSelection(rSelStart, 0, false, false);
            dataTable.setAutoscrolls(false);
            dataTable.changeSelection(rSelStart+nAddRows-1, nCols-1, false, true);
            dataTable.setAutoscrolls(true);

            // refresh rest of dialog
            datasetChanged = true;
            refresh();

            return(true);
         }
         
         // case 4: Selection spans table height, non-raster dataset. All chunk columns are inserted, optionally 
         // replacing selected columns. Truncated or padded row-wise if necessary. May be truncated or padded col-wise
         // also to satisfy dataset format's breadth constraints. If selected columns are NOT to be replaced and the
         // number of columns is already at the maximum, operation fails.
         // !! Number of rows in dataset does NOT change in this case !!
         if(nrSel == nRows)
         {
            // if we're not replacing and we cannot add any more columns, fail!
            if((!replace) && !currFmt.isValidDataBreadth(nCols+1)) return(false);
            
            int nDelCols = replace ? ncSel : 0;
            int nChunkRows;
            int nChunkCols = nc;
            if(isRasterChunk)
            {
               nChunkCols = 0;
               nChunkRows = Math.min(nRows, nc);
               for(int i=0; i<nChunkRows; i++)
               {
                  int nSamps = (int) data[3+i];
                  if(nSamps > nChunkCols) nChunkCols = nSamps;
               }
            }
            
            int nTotalCols = nCols - nDelCols + nChunkCols;
            int nRevised = currFmt.restrictToValidDataBreadth(nTotalCols);
            int nAddCols = nChunkCols;
            if(nRevised != nTotalCols) nAddCols += (nRevised-nTotalCols);

            float[] raw = new float[nRows*nRevised];
            
            // copy existing columns before the selection
            if(cSelStart > 0) 
            {
               int srcOffset = 0;
               int dstOffset = 0;
               for(int i=0; i<nRows; i++)
               {
                  System.arraycopy(currRawData, srcOffset, raw, dstOffset, cSelStart);
                  srcOffset += nCols;
                  dstOffset += nRevised;
               }
            }
            
            // copy existing columns after the selection
            if(cSelStart + nDelCols < nCols)
            {
               int nRem = (nCols - cSelStart - nDelCols);
               int srcOffset = cSelStart + nDelCols;
               int dstOffset = cSelStart + nAddCols;
               for(int i=0; i<nRows; i++)
               {
                  System.arraycopy(currRawData, srcOffset, raw, dstOffset, nRem);
                  srcOffset += nCols;
                  dstOffset += nRevised;
               }
            }
            
            // copy chunk data in between, padded/truncated as necessary
            if(isRasterChunk)
            {
               int dstOffset = cSelStart;
               int srcOffset = 3 + nc;
               for(int i=0; i<nRows; i++)
               {
                  // pad remaining rows if data chunk has fewer rows than current dataset
                  if(i >= nc) for(int j=0; j<nAddCols; j++) raw[dstOffset+j] = Float.NaN;
                  else
                  {
                     int nSamps = (int) data[3+i];
                     int nCopy = Math.min(nAddCols, nSamps);
                     System.arraycopy(data, srcOffset, raw, dstOffset, nCopy);
                     while(nCopy < nAddCols) { raw[dstOffset+nCopy] = Float.NaN; ++nCopy; }
                     srcOffset += nSamps;
                  }
                  dstOffset += nRevised;
               }
            }
            else
            {
               int dstOffset = cSelStart;
               int srcOffset = 3;
               int nCopy = Math.min(nc, nAddCols);
               for(int i=0; i<nRows; i++)
               {
                  // pad remaining rows if data chunk has fewer rows than current dataset
                  if(i >= nr) for(int j=0; j<nAddCols; j++) raw[dstOffset+j] = Float.NaN;
                  else
                  {
                     System.arraycopy(data, srcOffset, raw, dstOffset, nCopy);
                     while(nCopy < nAddCols) { raw[dstOffset+nCopy] = Float.NaN; ++nCopy; }
                     srcOffset += nc;
                  }
                  dstOffset += nRevised;
               }
            }
            
            // prepare the new dataset accordingly
            DataSet ds = DataSet.createDataSet(currDS.getID(), currFmt, currDS.getParams(), nRows, nRevised, raw);
            if(ds == null) return(false);
            currRawData = raw;
            currDS = ds;
            
            // update table display, and select what has been pasted in!
            reload();
            dataTable.changeSelection(0, cSelStart, false, false);
            dataTable.setAutoscrolls(false);
            dataTable.changeSelection(nRows-1, cSelStart+nAddCols-1, false, true);
            dataTable.setAutoscrolls(true);

            // refresh rest of dialog
            datasetChanged = true;
            refresh();
         }
         
         // case 5a: Non-raster data. Block selection spans neither height nor width of data, and it's a replace paste.
         // In this case, chunk data replaces selected block, truncated or padded both row-wise and col-wise as needed.
         // Neither the #rows nor #cols in dataset changes here.
         if(isRasterChunk)
         {
            int dstOffset = rSelStart*nCols + cSelStart;
            int srcOffset = 3 + nc;
            for(int i=0; i<nrSel; i++)
            {
               // pad remaining rows if data chunk has fewer rows than the block selection
               if(i >= nc) for(int j=0; j<ncSel; j++) currRawData[dstOffset+j] = Float.NaN;
               else
               {
                  int nSamps = (int) data[3+i];
                  int nCopy = Math.min(ncSel, nSamps);
                  System.arraycopy(data, srcOffset, currRawData, dstOffset, nCopy);
                  while(nCopy < ncSel) { currRawData[dstOffset+nCopy] = Float.NaN; ++nCopy; }
                  srcOffset += nSamps;
               }
               dstOffset += nCols;
            }
         }
         else
         {
            int dstOffset = rSelStart*nCols + cSelStart;
            int srcOffset = 3;
            int nCopy = Math.min(nc, ncSel);
            for(int i=0; i<nrSel; i++)
            {
               // pad remaining rows if data chunk has fewer rows than the block selection
               if(i >= nr) for(int j=0; j<ncSel; j++) currRawData[dstOffset+j] = Float.NaN;
               else
               {
                  System.arraycopy(data, srcOffset, currRawData, dstOffset, nCopy);
                  for(int j=nCopy; j<ncSel; j++) currRawData[dstOffset+j] = Float.NaN;
                  srcOffset += nc;
               }
               dstOffset += nCols;
            }
         }
         
         // prepare the new dataset accordingly (this should always succeeed!)
         currDS = DataSet.createDataSet(currDS.getID(), currFmt, currDS.getParams(), nRows, nCols, currRawData);
         
         // update table display, and select what has been pasted in!
         reload();
         dataTable.changeSelection(rSelStart, cSelStart, false, false);
         dataTable.setAutoscrolls(false);
         dataTable.changeSelection(rSelStart+nrSel-1, cSelStart+ncSel-1, false, true);
         dataTable.setAutoscrolls(true);

         // refresh rest of dialog
         datasetChanged = true;
         refresh();

         return(true);
      }
      
      /**
       * If possible, translate the text provided into a chunk of floating-point data, then insert it into the data set
       * currently encapsulated by this table model and displayed in the data set edit dialog's tabular view, optionally
       * replacing the current table selection. If the data chunk is not the right shape, it will be padded with
       * <i>Float.NaN</i> or truncated as necessary. If the shape of the raw data after the insertion would violate the 
       * shape constraints of the data set format, then the operation fails.
       * 
       * <p>The text argument must be a sequence of text lines (separated by a carriage return and/or line-feed), and
       * each such line must be a whitespace-separated list of floating-point string tokens. If it cannot be parsed as 
       * such, the operation fails. Otherwise, the method prepares a floating-point array that it then passes to 
       * {@link #insertDataChunk(float[], boolean)} to complete the insertion.</p>
       * 
       * @param textData The chunk of data to be inserted, in text form as described.
       * @param replace If true, the currently selected chunk of table data is replaced by the chunk provided. 
       * Otherwise, the chunk is inserted immediately before the first selected row.
       * @return True if insertion was successful. 
       */
      boolean insertDataChunk(String textData, boolean replace)
      {
         if(textData == null || textData.isEmpty()) return(false);
         
         // parse each line in the text source as a "row" of floating-pt data values. Abort if this is not possible.
         // While parsing, count the total data size and determine if rows are all the same length or not. If not, it's 
         // packaged as raster data.
         boolean isRasterChunk = false;
         int nTotal = 0;
         int nc = -1;
         
         List<float[]> rows = new ArrayList<>();
         StringTokenizer tokenizer = new StringTokenizer(textData, "\r\n");
         while(tokenizer.hasMoreTokens())
         {
            StringTokenizer lineTokenizer = new StringTokenizer(tokenizer.nextToken());
            float[] row = new float[lineTokenizer.countTokens()];
            for(int i=0; i<row.length; i++)
            {
               try { row[i] = Float.parseFloat(lineTokenizer.nextToken()); }
               catch(NumberFormatException nfe) { return(false); }
            }
            rows.add(row);
            
            nTotal += row.length;
            if(!isRasterChunk) 
            {
               if(nc == -1) nc = row.length;
               else if(nc != row.length) isRasterChunk = true;
            }
         }
         
         // text could be one or more empty lines!
         if(nTotal == 0) return(false);
         
         // prepare 1D float array containing the parsed data.
         float[] data;
         if(isRasterChunk)
         {
            data = new float[3 + rows.size() + nTotal];
            data[0] = 1;
            data[1] = nTotal;
            data[2] = rows.size();
            int dstOffset = 3 + rows.size();
            for(int i=0; i<rows.size(); i++)
            {
               float[] row = rows.get(i);
               data[3+i] = row.length;
               System.arraycopy(row, 0, data, dstOffset, row.length);
               dstOffset += row.length;
            }
         }
         else
         {
            data = new float[3 + nTotal];
            data[0] = 0;
            data[1] = rows.size();
            data[2] = nc;
            int dstOffset = 3;
            for(float[] row : rows)
            {
               System.arraycopy(row, 0, data, dstOffset, nc);
               dstOffset += nc;
            }
         }
         
         return(insertDataChunk(data, replace));
      }
      
      /**
       * Can a new row or column be appended or inserted into the tabular view of the current data set? Rules enforced:
       * <ul>
       *    <li>A row can always be added to the data table. A row insertion requires a non-empty selection to define
       *    the insertion point for the new row. The selection need not span the table column-wise.</li>
       *    <li>If the data format is <b>RASTER1D</b>, it does not make sense to add a column. However, it does make 
       *    sense to insert or append a new raster sample to a single row (raster) -- so a column insert or append 
       *    is allowed only IF the selection is non-empty and involves a single row.</li>
       *    <li>For all other data formats, a column insert/append is enabled only if the addition will not violate the
       *    breadth constraints of the current data format. Again, a column insertion requires a non-empty selection to 
       *    define the insertion point for the new column. It need not span table row-wise.</li>
       *    <li>If the data set is currently empty, all operations are enabled.</li>
       * </ul>
       * @param isCol True to check if column insertion is possible, false for row insertion.
       * @param isAppend If set, check if it is possible to append rather than insert the new row or column. Insertion
       * is more restrictive, since it generally requires a non-empty table selection to define the insertion point.
       * @return True if insert/append operation is possible given the current state and contents of the tabular view.
       */
      boolean canInsertNewRowOrCol(boolean isCol, boolean isAppend)
      {
         // we can always insert or append when the dataset is empty
         if(currDS.isEmpty()) return(true);
         
         // we can always append a row, and we can always insert a row so long as an insertion point is defined
         if(!isCol)
            return(isAppend || (dataTable.getSelectedRow() > -1));
         
         // if the dataset format is RASTER1D, we can append/insert a column only if a single row is selected.
         Fmt fmt = currDS.getFormat();
         if(fmt == Fmt.RASTER1D) return(dataTable.getSelectedRowCount() == 1);
         
         // for all other formats, we can append/insert column so long as the resulting #cols does not violate format
         // breadth constraints. An insertion is possible only if an insertion point is defined.
         return(fmt.isValidDataBreadth(currDS.getDataBreadth()+1) && (isAppend || dataTable.getSelectedColumn() > -1));
      }
      
      /**
       * Insert or append a new row or column in the tabular view of the current data set, if possible. The added row or 
       * column is selected in the table and populated with <i>Float.NaN</i>. See {@link #canInsertNewRowOrCol(boolean,
       * boolean)}.
       * @param isCol True to add a new column, false to add a new row.
       * @param isAppend If set, append rather than insert a new row or column.
       * @return True if operation was successful.
       */
      boolean insertNewRowOrCol(boolean isCol, boolean isAppend)
      {
         Fmt fmt = currDS.getFormat();
         int selRowNum;
         int selColNum;
         
         // special case: dataset is currently empty.
         boolean addRasterSample = false;
         if(currDS.isEmpty())
         {
            if(fmt == Fmt.RASTER1D)
            {
               float[] raw = new float[] {1, Float.NaN};  // a single raster with a single NaN sample!
               DataSet ds = DataSet.createDataSet(currDS.getID(), fmt, null, 1, 1, raw);
               if(ds == null) return(false);
               currRawData = raw;
               currDS = ds;
            }
            else
            {
               int breadth = 1;
               while(!fmt.isValidDataBreadth(breadth)) ++breadth;
               
               float[] raw = new float[breadth];
               for(int i=0; i<breadth; i++) raw[i] = Float.NaN;
               
               DataSet ds = DataSet.createDataSet(currDS.getID(), fmt, currDS.getParams(), 1, breadth, raw);
               if(ds == null) return(false);
               currRawData = raw;
               currDS = ds;
            }
            selRowNum = selColNum = 0;
         }
         else if(!isCol)
         {
            // if inserting a row, the insertion point must be defined!
            int insertRow = dataTable.getSelectedRow();
            if((!isAppend) && insertRow == -1) return(false);
            
            if(fmt == Fmt.RASTER1D)
            {
               // adds a raster of length 1, the single raster sample is NaN
               int nRasters = currDS.getDataBreadth();
               int nTotalSamples = currDS.getDataLength();
               if(isAppend) insertRow = nRasters;
               float[] raw = new float[currRawData.length + 2];
               
               // copy raster data before insertion point
               int nSamples = 0;
               for(int i=0; i<insertRow; i++) 
               {
                  raw[i] = currRawData[i];
                  nSamples += (int) currRawData[i];
               }
               if(nSamples > 0) System.arraycopy(currRawData, nRasters, raw, nRasters+1, nSamples);

               // the inserted raster of length 1
               raw[insertRow] = 1;
               raw[nRasters+1+nSamples] = Float.NaN;
               
               // copy raster data after insertion point
               int srcOffset = nRasters + nSamples;
               nSamples = 0;
               for(int i=insertRow; i<nRasters; i++) 
               {
                  raw[i+1] = currRawData[i];
                  nSamples += (int) currRawData[i];
               }
               if(nSamples > 0) System.arraycopy(currRawData, srcOffset, raw, srcOffset+2, nSamples);
               
               DataSet ds = DataSet.createDataSet(currDS.getID(), Fmt.RASTER1D, null, nTotalSamples+1, nRasters+1, raw);
               if(ds == null) return(false);
               currRawData = raw;
               currDS = ds;
               selRowNum = insertRow;
               selColNum = 0;
            }
            else
            {
               int tupleLen = currDS.getDataBreadth();
               int nTuples = currDS.getDataLength();
               if(isAppend) insertRow = nTuples;
               int offset = insertRow*tupleLen;
               float[] raw = new float[currRawData.length + tupleLen];
               if(insertRow > 0) 
                  System.arraycopy(currRawData, 0, raw, 0, offset);
               if(insertRow < nTuples) 
                  System.arraycopy(currRawData, offset, raw, offset+tupleLen, (nTuples-insertRow)*tupleLen);
               for(int i=0; i<tupleLen; i++) raw[offset+i] = Float.NaN;
               
               DataSet ds = DataSet.createDataSet(currDS.getID(), fmt, currDS.getParams(), nTuples+1, tupleLen, raw);
               if(ds == null) return(false);
               currRawData = raw;
               currDS = ds;
               selRowNum = insertRow;
               selColNum = 0;
            }
         }
         else
         {
            // can't insert if insertion point is undefined.
            int insertCol = dataTable.getSelectedColumn();
            if((!isAppend) && insertCol == -1) return(false);
            
            if(fmt == Fmt.RASTER1D)
            {
               // can insert/append a new sample to a single raster ONLY if a single row is selected
               int insertRow = dataTable.getSelectedRow();
               if(dataTable.getSelectedRowCount() != 1 || insertRow == -1 || insertCol == -1) return(false);
               
               int nRasters = currDS.getDataBreadth();
               int nTotalSamples = currDS.getDataLength();
               int nSelRasterSamps = (int) currRawData[insertRow];
               
               // if insert op and first sel column is past last raster sample, then it's the same as an append op!
               if(isAppend || insertCol > nSelRasterSamps) insertCol = nSelRasterSamps;
               
               // we're adding a single raster sample to the dataset!
               float[] raw = new float[currRawData.length + 1];
               
               // copy raster lengths and increment the length of the selected raster
               System.arraycopy(currRawData, 0, raw, 0, nRasters);
               raw[insertRow] = nSelRasterSamps + 1;
               
               // copy all raster data before the inserted sample
               int nSamples = 0; for(int i=0; i<insertRow; i++) nSamples += (int) currRawData[i];
               nSamples += insertCol;
               if(nSamples > 0) System.arraycopy(currRawData, nRasters, raw, nRasters, nSamples);
               int srcOffset = nRasters + nSamples;
               
               // init the inserted sample
               raw[srcOffset] = Float.NaN;
               
               // copy all raster data after the inserted sample
               nSamples = nSelRasterSamps - insertCol;
               for(int i=insertRow + 1; i<nRasters; i++) nSamples += (int) currRawData[i];
               if(nSamples > 0) System.arraycopy(currRawData, srcOffset, raw, srcOffset+1, nSamples);
               
               DataSet ds = DataSet.createDataSet(currDS.getID(), Fmt.RASTER1D, null, nTotalSamples+1, nRasters, raw);
               if(ds == null) return(false);
               currRawData = raw;
               currDS = ds;
               selRowNum = insertRow;
               selColNum = insertCol;
               addRasterSample = true;    // a special case: we just want to select the single sample added
            }
            else
            {
               // column insert/append not allowed if we've already reached max #columns for this dataset format!
               int tupleLen = currDS.getDataBreadth();
               if(!fmt.isValidDataBreadth(tupleLen+1)) return(false);
               if(isAppend) insertCol = tupleLen;
               
               int nTuples = currDS.getDataLength();
               float[] raw = new float[currRawData.length + nTuples];
               int offsetOld = 0;
               int offsetNew = 0;
               for(int i=0; i<nTuples; i++)
               {
                  System.arraycopy(currRawData, offsetOld, raw, offsetNew, insertCol);
                  raw[offsetNew + insertCol] = Float.NaN;
                  System.arraycopy(currRawData, offsetOld + insertCol, raw, offsetNew + insertCol + 1, 
                        tupleLen-insertCol);
                  offsetOld += tupleLen;
                  offsetNew += (tupleLen + 1);
               }
               
               DataSet ds = DataSet.createDataSet(currDS.getID(), fmt, currDS.getParams(), nTuples, tupleLen+1, raw);
               if(ds == null) return(false);
               currRawData = raw;
               currDS = ds;
               selRowNum = 0;
               selColNum = insertCol;
            }
         }
         
         reload();
         if(addRasterSample) 
            dataTable.changeSelection(selRowNum, selColNum, false, false);
         else
         {
            dataTable.changeSelection(isCol ? 0 : selRowNum, isCol ? selColNum : 0, false, false);
            dataTable.setAutoscrolls(false);
            dataTable.changeSelection(isCol ? nRows-1 : selRowNum, isCol ? selColNum : nCols-1, false, true);
            dataTable.setAutoscrolls(true);
         }
         
         // refresh rest of dialog
         datasetChanged = true;
         refresh();

         return(true);
      }
      
      
      /** Total number of rows in the raw data matrix. */
      private int nRows = 0;
      
      /** Total number of columns in the raw data matrix. */
      private int nCols = 0;
      
      /** A table model that acts as a row header for the enclosing instance of <code>DataSetTM</code>. */
      private final RowHdrTM rowHeaderTableModel = new RowHdrTM();
      
      /**
       * Get a table model that serves as a row header for this <code>DataSetTM</code>. The contents of the row
       * header table stay in synch with the contents of the data table itself. This model should be set as the model
       * for a simple table installed in the row header view of a scroll pane containing the data table itself in the 
       * scroll pane's main viewport.
       * @return The row header table model, as described.
       */
      TableModel getRowHeaderModel() { return(rowHeaderTableModel); }
      
      /** 
       * This table model serves as the model for a row header view for the enclosing <code>DataSetTM</code>. It always
       * has one column and the same number of rows as the data set table model, and the value in each row is simply one
       * more than the row index.
       * @author sruffner
       */
      private class RowHdrTM extends AbstractTableModel
      {
         private static final long serialVersionUID = 1L;

         public int getColumnCount() { return(1); }
         public int getRowCount() { return(DataSetTM.this.getRowCount()); }

         public Object getValueAt(int rowIndex, int columnIndex)
         {
           if(rowIndex < 0 || rowIndex >= getRowCount() || columnIndex != 0) return(NULLDATUM);
           return(rowIndex + 1);
         }
         
         @Override public Class<?> getColumnClass(int columnIndex) { return(Integer.class); }
         @Override public String getColumnName(int column) { return("N : M"); }
      }
   }


   /**
    * A fixed-width table column model.
    * @author sruffner
    */
   private static class FixedWidthTCM extends DefaultTableColumnModel
   {
      /** The desired width of all columns in this column model, in pixels. */
      private final int fixedW;
      
      /**
       * Construct a <code>FixedWidthTCM</code>.
       * @param w Desired width for all columns in this model, in pixels. If non-positive, then 50 is assumed.
       */
      FixedWidthTCM(int w) { super(); fixedW = (w <= 0) ? 50 : w; }
      
      @Override public void addColumn(TableColumn column)
      {
         column.setPreferredWidth(fixedW);
         column.setMinWidth(fixedW);
         column.setMaxWidth(fixedW);
         super.addColumn(column);
      }
   }

   /**
    * This mouse handler is installed on the data table's column header ({@link JTableHeader}), row header (a one-column
    * {@link JTable}), and the row header's header. It mediates full-column selects via mouse-click or drag gestures on 
    * the column header, and full-row selects via similar gestures on the row header. In addition, upon clicking the row
    * header's header (which appears in the top-left corner of the parent scroll pane), this handler clears the data 
    * table selection and ensures that the data table cell at (0,0) is scrolled into view. 
    * 
    * <p>At the completion of a mouse-click or mouse-drag gesture, the handler always returns the keyboard focus to the
    * data table itself, thereby ensuring that keyboard accelerators associated with the various data table editing 
    * operations will work as expected.</p>
    * @author sruffner
    */
   private class HeaderMouseHandler extends MouseInputAdapter
   {
      @Override public void mouseClicked(MouseEvent e)
      {
         if(e.getClickCount() != 1) {dataTable.requestFocusInWindow(); return;}
         
         Object src = e.getSource();
         if(src == rowHdrForDataTable.getTableHeader())
         {
            Rectangle r = dataTable.getCellRect(0, 0, true);
            dataTable.scrollRectToVisible(r);
            dataTable.clearSelection();
         }
         else if(src == rowHdrForDataTable)
         {
            int row = rowHdrForDataTable.rowAtPoint(e.getPoint());
            if(row >= 0)
            {
               // check for selection extension: SHIFT key down, and current table selection spans all rows.
               int mods = e.getModifiersEx();
               boolean extend = ((mods & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK);
               extend = extend && (dataTable.getSelectedColumnCount() == dataTable.getColumnCount());
               
               // select a single column or extend current full-column selection. In either case, we turn off 
               // autoscrolling of table when extending the selection so that table does not scroll to the right.
               if(!extend) dataTable.changeSelection(row, 0, false, false);
               dataTable.setAutoscrolls(false);
               dataTable.changeSelection(row, dataTable.getColumnCount()-1, false, true);
               dataTable.setAutoscrolls(true);
            }
         }
         else if(currDS != null && currDS.getFormat() != Fmt.RASTER1D)
         {
            JTableHeader hdr = dataTable.getTableHeader();
            int col = hdr.columnAtPoint(e.getPoint());
            if(col >= 0)
            {
               // check for selection extension: SHIFT key down, and current table selection spans all rows.
               int mods = e.getModifiersEx();
               boolean extend = ((mods & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK);
               extend = extend && (dataTable.getSelectedRowCount() == dataTable.getRowCount());
               
               // select a single column or extend current full-column selection. In either case, we turn off 
               // autoscrolling of table when extending the selection so that table does not scroll down to the bottom.
               if(!extend) dataTable.changeSelection(0, col, false, false);
               dataTable.setAutoscrolls(false);
               dataTable.changeSelection(dataTable.getRowCount()-1, col, false, true);
               dataTable.setAutoscrolls(true);
            }
         }
         
         // return keyboard focus to data table so that keyboard accelerators work; also make sure edit action have
         // been updated in response to the possible selection change.
         dataTable.requestFocusInWindow();
         DSEditorToolDlg.this.refresh();
      }

      @Override public void mousePressed(MouseEvent e) 
      { 
         Object src = e.getSource();
         if(src == dataTable.getTableHeader() || src == rowHdrForDataTable)
         {
            isDraggingColHdr = isDraggingRowHdr = false; 
            anchorPt = e.getPoint(); 
         }
      }

      @Override public void mouseDragged(MouseEvent e)
      {
         Object src = e.getSource();
         if(src == rowHdrForDataTable.getTableHeader()) return;
         
         if(src == rowHdrForDataTable && isDraggingColHdr)
         {
            isDraggingColHdr = false;
            return;
         }
         else if(src == dataTable.getTableHeader() && 
               (isDraggingRowHdr || (currDS == null) || (currDS.getFormat() == Fmt.RASTER1D)))
         {
            isDraggingRowHdr = false;
            return;
         }
         
         Point p = e.getPoint();
         if(src == rowHdrForDataTable)
         {
            if(!isDraggingRowHdr)
            {
               if(anchorPt == null) { anchorPt = p; return; }
               if(Math.abs(p.x-anchorPt.x) > 2 || Math.abs(p.y-anchorPt.y) > 2)
               {
                  // drag started. Select the row indicated by drag's anchor point. We turn off autoscrolling of the
                  // the table when extending selection over entire row so that table does not scroll to rightmost col.
                  int anchorRow = rowHdrForDataTable.rowAtPoint(anchorPt);
                  if(anchorRow == -1) {anchorPt = p; return; }
                  isDraggingRowHdr = true;
                  dataTable.changeSelection(anchorRow, 0, false, false);
                  dataTable.setAutoscrolls(false);
                  dataTable.changeSelection(anchorRow, dataTable.getColumnCount()-1, false, true);
                  dataTable.setAutoscrolls(true);
               }
            }
            else
            {
               // drag in progress. Current full-row selection is extended to row under cursor (this may increase or
               // decrease the #rows selected!). If cursor is above or below the row header's current visible rect, 
               // then the row "under the cursor" is the one just out of view, and the table is scrolled so that row
               // becomes visible.
               
               Rectangle visR = rowHdrForDataTable.getVisibleRect();
               int top = visR.y;
               int bot = top + visR.height;
               int halfRowHt = rowHdrForDataTable.getRowHeight() / 2;
               
               if(p.y < top) p.y = top - halfRowHt;
               else if(p.y >= bot) p.y = bot + halfRowHt;
               int row = rowHdrForDataTable.rowAtPoint(p);
               if(row == -1) return;
               
               dataTable.setAutoscrolls(false);
               dataTable.changeSelection(row, dataTable.getColumnCount()-1, false, true);
               dataTable.setAutoscrolls(true);
               
               // 
               // NOTE: At least on Windows, the row header has already been autoscrolled by the time this listener is
               // called, so that the cursor is always inside the row header's visible rect (until it has scrolled to
               // the beginning or end). Thus, the same check that worked for the column header does not work here.
               // Instead, we check whether or not the data table's current visible rect contains the rect of the cell
               // in the first column of the row added to the selection, and if not, we scroll it into view.
               //
               Rectangle cellR = dataTable.getCellRect(row, 0, true);
               visR = dataTable.getVisibleRect();
               if(visR != null && !visR.contains(cellR))
                  dataTable.scrollRectToVisible(cellR);
            }
         }
         else
         {
            if(!isDraggingColHdr)
            {
               if(anchorPt == null) { anchorPt = p; return; }
               if(Math.abs(p.x-anchorPt.x) > 2 || Math.abs(p.y-anchorPt.y) > 2)
               {
                  // drag started. Select the column indicated by drag's anchor point. We turn off autoscrolling of the
                  // table when extending selection over entire column so that table does not scroll to the bottom row.
                  int anchorCol = dataTable.getTableHeader().columnAtPoint(anchorPt);
                  if(anchorCol == -1) {anchorPt = p; return; }
                  isDraggingColHdr = true;
                  dataTable.changeSelection(0, anchorCol, false, false);
                  dataTable.setAutoscrolls(false);
                  dataTable.changeSelection(dataTable.getRowCount()-1, anchorCol, false, true);
                  dataTable.setAutoscrolls(true);
               }
            }
            else
            {
               // drag in progress. Current full-column selection is extended to column under cursor (may increase or
               // decrease the #columns selected!). If cursor is right or left of the table header's current vis rect, 
               // then the column "under the cursor" is the one just out of view, and the table is scrolled so that 
               // column becomes visible.
               JTableHeader hdr = dataTable.getTableHeader();
               Rectangle visR = hdr.getVisibleRect();
               int left = visR.x;
               int right = left + visR.width;
               int halfW = hdr.getColumnModel().getColumn(0).getWidth() / 2;
               
               if(p.x < left) p.x = left - halfW;
               else if(p.x >= right) p.x = right + halfW;
               
               int col = hdr.columnAtPoint(p);
               if(col == -1) return;
               dataTable.setAutoscrolls(false);
               dataTable.changeSelection(dataTable.getRowCount()-1, col, false, true);
               dataTable.setAutoscrolls(true);
               if(p.x < left || p.x > right)
               {
                  Rectangle scrollTo = dataTable.getCellRect(0, col, true);
                  dataTable.scrollRectToVisible(scrollTo);
               }
            }
         }
      }

      @Override public void mouseReleased(MouseEvent e) 
      { 
         Object src = e.getSource();
         if(src == dataTable.getTableHeader() || src == rowHdrForDataTable)
         {
            isDraggingColHdr = isDraggingRowHdr = false; 
            anchorPt = null; 
         }
         
         // return keyboard focus to data table so that keyboard accelerators work; also make sure edit action have
         // been updated in response to the possible selection change.
         dataTable.requestFocusInWindow();
         DSEditorToolDlg.this.refresh();
      }
      
      /** Flag set when a drag gesture on data table column header is in progress. */
      private boolean isDraggingColHdr = false;
      /** Flag set when a drag gesture on data table row header is in progress. */
      private boolean isDraggingRowHdr = false;
      /** The cursor location of the mousepress that initiated a drag gesture on the either table header. */
      private Point anchorPt = null;
   }
   
   
   //
   // Data table-related operations
   //
   
   /** Data table action code: Append a new row to the table. */
   private final static int TABLE_APPENDROW = 0;
   /** Data table action code: Insert a new row into the table, before first row of the current selection. */
   private final static int TABLE_INSERTROW = 1;
   /** 
    * Data table action code: Append a new column to the table. For raster datasets, the action appends a new raster
    * sample to a SINGLE raster (in which case it is enabled only if a single row is selected in the table..
    */
   private final static int TABLE_APPENDCOL = 2;
   /** 
    * Data table action code: Insert a new column into the table, before first column of the current selection. For
    * raster datasets, the action inserts a new raster sample in a SINGLE raster (in which case it is enabled only if a
    * single row is selected in the table.
    */
   private final static int TABLE_INSERTCOL = 3;
   /** Data table action code: Copy the current selection in the table to the system clipboard. */
   private final static int TABLE_COPY = 4;
   /** Data table action code: Copy the current selection to the system clipboard, then remove it from the table. */
   private final static int TABLE_CUT = 5;
   /** Data table action code: Paste clipboard contents into table, replacing the current selection. */
   private final static int TABLE_PASTE = 6;
   /** Data table action code: Paste clipboard contents into table, inserted before the current selection. */
   private final static int TABLE_INSPASTE = 7;
   /** Data table action code: Delete the current selection from the table. */
   private final static int TABLE_DELETE = 8;
   /** Number of supported data table actions. */
   private final static int NTABLEACTIONS = TABLE_DELETE+1;
   
   /** Supported data table actions, lazily created. */
   private DataTableAction[] dataTableActions = null;
   
   /** 
    * Helper method for {@link #createComponents()}. It creates the list of supported data table edit actions, installs
    * them in the table's action map, installs corresponding <b>WHEN_FOCUSED</b> key bindings for them, and creates a 
    * tool bar button for each. This should only be called after the data table has been constructed!
    */
   private void createAndInstallDataTableActions()
   {
      if(dataTableActions != null) return;
      if(dataTable == null) return;
      int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
      dataTableActions = new DataTableAction[NTABLEACTIONS];
      
      dataTableActions[TABLE_APPENDROW] = new DataTableAction(TABLE_APPENDROW, "Append New Row", 
            FCIcons.V4_APPENDROW_22, KeyStroke.getKeyStroke(KeyEvent.VK_R, mask|KeyEvent.SHIFT_DOWN_MASK),
            "Append a new row to dataset");
      appendRowBtn = new JButton(dataTableActions[TABLE_APPENDROW]);
      makeToolButton(appendRowBtn);
      
      dataTableActions[TABLE_INSERTROW] = new DataTableAction(TABLE_INSERTROW, "Insert New Row", 
            FCIcons.V4_INSROW_22, KeyStroke.getKeyStroke(KeyEvent.VK_R, mask), 
            "Insert a new row into dataset, before first row selected");
      insertRowBtn = new JButton(dataTableActions[TABLE_INSERTROW]);
      makeToolButton(insertRowBtn);
      
      dataTableActions[TABLE_APPENDCOL] = new DataTableAction(TABLE_APPENDCOL, "Append New Column", 
            FCIcons.V4_APPENDCOL_22, KeyStroke.getKeyStroke(KeyEvent.VK_K, mask|KeyEvent.SHIFT_DOWN_MASK),
            "Append a new column to dataset");
      appendColBtn = new JButton(dataTableActions[TABLE_APPENDCOL]);
      makeToolButton(appendColBtn);
         
      dataTableActions[TABLE_INSERTCOL] = new DataTableAction(TABLE_INSERTCOL, "Insert New Column", 
            FCIcons.V4_INSCOL_22, KeyStroke.getKeyStroke(KeyEvent.VK_K, mask), 
            "Insert a new column into dataset, before first column selected");
      insertColBtn = new JButton(dataTableActions[TABLE_INSERTCOL]);
      makeToolButton(insertColBtn);
         
      dataTableActions[TABLE_COPY] = new DataTableAction(TABLE_COPY, "Copy", 
            FCIcons.V4_COPY_22, KeyStroke.getKeyStroke(KeyEvent.VK_C, mask),
            "Copy current selection to clipboard");
      copyBtn = new JButton(dataTableActions[TABLE_COPY]);
      makeToolButton(copyBtn);
      
      dataTableActions[TABLE_CUT] = new DataTableAction(TABLE_CUT, "Cut", 
            FCIcons.V4_CUT_22, KeyStroke.getKeyStroke(KeyEvent.VK_X, mask),
            "Remove current selection to clipboard");
      cutBtn = new JButton(dataTableActions[TABLE_CUT]);
      makeToolButton(cutBtn);
      
      dataTableActions[TABLE_PASTE] = new DataTableAction(TABLE_PASTE, "Paste", 
            FCIcons.V4_PASTE_22, KeyStroke.getKeyStroke(KeyEvent.VK_V, mask),
            "Paste data from clipboard, replacing current selection");
      pasteBtn = new JButton(dataTableActions[TABLE_PASTE]);
      makeToolButton(pasteBtn);
      
      dataTableActions[TABLE_INSPASTE] = new DataTableAction(TABLE_INSPASTE, "Insert Paste", 
            FCIcons.V4_INSPASTE_22, KeyStroke.getKeyStroke(KeyEvent.VK_I, mask),
            "Paste data from clipboard, inserted before current selection");
      insPasteBtn = new JButton(dataTableActions[TABLE_INSPASTE]);
      makeToolButton(insPasteBtn);
      
      dataTableActions[TABLE_DELETE] = new DataTableAction(TABLE_DELETE, "Delete", 
            FCIcons.V4_DELETE_22, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            "Delete the current selection");
      deleteBtn = new JButton(dataTableActions[TABLE_DELETE]);
      makeToolButton(deleteBtn);
      

      // install key input-action bindings for the data table
      ActionMap am = dataTable.getActionMap();
      InputMap im = dataTable.getInputMap();
      for(DataTableAction dataTableAction : dataTableActions)
      {
         String name = (String) dataTableAction.getValue(Action.NAME);
         KeyStroke accel = (KeyStroke) dataTableAction.getValue(Action.ACCELERATOR_KEY);
         if(name != null && accel != null)
         {
            am.put(name, dataTableAction);
            im.put(accel, name);
         }
      }
   }
   
   /**
    * Helper method makes some property changes to the button specified so that it looks right as an icon-only button
    * suitable for a toolbar-like widget.
    * @param btn The button.
    */
   private void makeToolButton(JButton btn)
   {
      btn.setText("");
      btn.setIconTextGap(0);
      btn.setBorder(BorderFactory.createEmptyBorder(1,0,2,12));
      btn.setContentAreaFilled(false); btn.setBorderPainted(false);
      btn.setFocusable(false);
   }
   
   /** 
    * Helper method refreshes the enable state of all data table-related actions given the current state of the 
    * displayed dataset and the current selection in the data table.
    */
   private void refreshDataTableActions()
   {
      if(dataTableActions != null) for(DataTableAction dataTableAction : dataTableActions) dataTableAction.refresh();
   }
   
   /**
    * This helper class mediates all data table-related operations supported in the data set edit dialog's tabular view.
    *
    * @author sruffner
    */
   private class DataTableAction extends AbstractAction
   {
      /**
       * Construct a <code>DataTableAction</code>.
       * @param code The unique action code, in <code>[0 .. NTABLEACTIONS-1]</code>.
       * @param name The name of the action.
       * @param icon A small (16x16) icon for the action.
       * @param accel Accelerator keystroke bound to the action when focus is on the data table.
       * @param tip Tooltip installed on toolbar button or menu item associated with action.
       */
      DataTableAction(int code, String name, Icon icon, KeyStroke accel, String tip)
      {
         super(name);
         this.code = code;
         if(icon != null) putValue(SMALL_ICON, icon);
         if(accel != null) putValue(ACCELERATOR_KEY, accel);
         if(tip != null) putValue(Action.SHORT_DESCRIPTION, tip);
      }
      
      public void actionPerformed(ActionEvent e)
      {
         boolean ok = false;
         float[] chunk;
         if(isEnabled()) switch(code)
         {
            case TABLE_APPENDROW :
            case TABLE_INSERTROW :
               ok = dataTableModel.insertNewRowOrCol(false, code == TABLE_APPENDROW);
               break;
            case TABLE_APPENDCOL : 
            case TABLE_INSERTCOL :
               ok = dataTableModel.insertNewRowOrCol(true, code == TABLE_APPENDCOL);
               break;
            case TABLE_COPY :
            case TABLE_CUT :
               chunk = dataTableModel.getCurrentSelectionAsDataChunk();
               if(chunk != null)
               {
                  // copying huge chunks to system clipboard is unavoidably slow!
                  Cursor oldCursor = null;
                  if(chunk.length > 25000)
                  {
                     oldCursor = DSEditorToolDlg.this.getCursor();
                     DSEditorToolDlg.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                  }
                  
                  Transferable t = new DataSetChunkTransferable(chunk);
                  Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                  try 
                  {
                     clip.setContents(t, null);
                     if(code == TABLE_CUT) ok = dataTableModel.removeCurrentSelectionIfPossible();
                     else ok = true;
                  }
                  catch(IllegalStateException ignored) {}
                  
                  if(oldCursor != null) DSEditorToolDlg.this.setCursor(oldCursor);
               }
               break;
            case TABLE_PASTE :
            case TABLE_INSPASTE :
               if(dataTable.getSelectionModel().isSelectionEmpty() && !currDS.isEmpty()) break;
               
               Cursor oldCursor = null;
               Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
               try
               {
                  if(clip.isDataFlavorAvailable(dataChunkFlavor))
                  {
                     chunk = (float[]) clip.getData(dataChunkFlavor);
                     if(chunk.length > 25000)
                     {
                        oldCursor = DSEditorToolDlg.this.getCursor();
                        DSEditorToolDlg.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                     }
                     ok = dataTableModel.insertDataChunk(chunk, code==TABLE_PASTE);
                  }
                  else if(clip.isDataFlavorAvailable(DataFlavor.stringFlavor))
                  {
                     String s = (String) clip.getData(DataFlavor.stringFlavor);
                     if(s.length() > 125000)
                     {
                        oldCursor = DSEditorToolDlg.this.getCursor();
                        DSEditorToolDlg.this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                     }
                     ok = dataTableModel.insertDataChunk(s, code==TABLE_PASTE);
                  }
               }
               catch(IllegalStateException | UnsupportedFlavorException | IOException ignored) {}
               finally
               {
                  if(oldCursor != null) DSEditorToolDlg.this.setCursor(oldCursor);
               }
               break;
            case TABLE_DELETE :
               ok = dataTableModel.removeCurrentSelectionIfPossible();
               break;
         }
         
         if(!ok) Toolkit.getDefaultToolkit().beep();
      }

      /** 
       * Refresh the enabled state of this action IAW the current state of the dataset displayed in the data table 
       * and the current selection within the table.
       */
      void refresh()
      {
         boolean ena = false;
         if(isTableView) switch(code)
         {
            case TABLE_APPENDROW: 
            case TABLE_INSERTROW: 
               ena = dataTableModel.canInsertNewRowOrCol(false, code == TABLE_APPENDROW); 
               break;
            case TABLE_APPENDCOL: 
            case TABLE_INSERTCOL: 
               ena = dataTableModel.canInsertNewRowOrCol(true, code == TABLE_APPENDCOL); 
               break;
            case TABLE_COPY:
               ena = dataTableModel.canCopyCurrentSelection();
               break;
            case TABLE_CUT:
            case TABLE_DELETE: 
               ena = dataTableModel.canRemoveCurrentSelection();
               break;
            case TABLE_PASTE:
            case TABLE_INSPASTE:
               if(currDS.isEmpty()) ena = true;
               else
               {
                  ListSelectionModel lsm = dataTable.getSelectionModel();
                  ena = !(lsm.getValueIsAdjusting() || lsm.isSelectionEmpty());
                  if(ena && currDS.getFormat() == Fmt.RASTER1D)
                     ena = dataTable.getSelectedRowCount() == 1 ||
                           dataTable.getSelectedColumnCount() == dataTable.getColumnCount();
               }
               break;
         }
         setEnabled(ena);
      }
      
      /** This action's unique ID. */
      private final int code;
   }
   
   
   /** Data flavor available only for pasting into tabular view (more efficient than reconverting text!) */
   private static final DataFlavor dataChunkFlavor = new DataFlavor(float[].class, "DataNav data chunk");
   
   /**
    * Custom <code>Transferable</code> implementation that encapsulates a "chunk" of a dataset formed by copying or
    * cutting the current selection in the data set editor's tabular view. Internally, the "chunk" is maintained as a 
    * float array prepared by {@link DataSetTM#getCurrentSelectionAsDataChunk()}. This is much faster and more memory 
    * efficient than converting the data chunk to text form. Nevertheless, to support "pasting" the data into another 
    * application, this implementation supports the standard string flavor, in which the internal data chunk is 
    * translated into a plain-text string in which each data "row" is a whitespace-separated list of floating-point 
    * string tokens and ends with a carriage return-linefeed pair.
    * 
    * <p><i>Performance tweaks</i>. Tests on both Mac OSX and Windows showed a long delay when copying a large data
    * chunk (200K) -- as long as 60 seconds on Windows! After testing/debugging, we learned that when copying a 
    * transferable to the system clipboard via {@link Clipboard#setContents(Transferable, ClipboardOwner)}, the data is
    * transferred into the clipboard immediately rather than storing the transferable object (apparently for security
    * reasons). Thus, the {@link #getTransferData(DataFlavor)} method is called under the hood when you copy to the
    * clipboard. Under Windows XP, this call happened three times, and I determined that this is because the generic
    * "string flavor" maps to three native data types (not sure what they are). Furthermore, the initial implementation
    * of <b>getTransferData()</b> used {@link Utilities#toString()}, which is not particularly efficient (eg, it uses
    * string concatenation rather than a string buffer).</p>
    * 
    * <p>Given these discoveries, we dramatically improved the performance by using a string buffer and {@link 
    * Float#toString()} to prepare the text form of the data chunk. Then we saved a weak reference to the enerated 
    * string so that a repeat invocation of <code>getTransferData()</code> would not have to reconstruct the text. A 
    * weak reference is used so that it can be garbage-collected if memory is low.</p>
    * 
    * <p>Finally, we found that once you place the transferable object on the system clipboard, you cannot get that
    * same transferable back. It is encapsulated by a proxy object to ensure that applications sharing the clipboard
    * don't "step on each other". In order to cut/copy and then paste data chunks efficiently, it was important that
    * <code>DataSetChunkTransferable</code> support a custom data flavor that provides the data chunk as a float array
    * exactly in the format generated by {@link DataSetTM#getCurrentSelectionAsDataChunk()}.</p>
    * 
    * @author sruffner
    */
   private class DataSetChunkTransferable implements Transferable
   {
      public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
      {
         if(!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);

         if(dataChunkFlavor.equals(flavor))
         {
            float[] chunk = new float[dataChunk.length];
            System.arraycopy(dataChunk, 0, chunk, 0, dataChunk.length);
            return(chunk);
         }
         
         String cachedText;
         if(dataAsText != null)
         {
            cachedText = dataAsText.get();
            if(cachedText != null) return(cachedText);
         }
         
         StringBuilder buf = new StringBuilder(10000);
         final String CRLF = "\r\n";
         if(dataChunk[0] != 0)
         {
            int nRasters = (int) dataChunk[2];
            int offset = 3+nRasters;
            for(int i=0; i<nRasters; i++)
            {
               int nSamps = (int) dataChunk[3+i];
               for(int j=0; j<nSamps; j++)
               {
                  buf.append(dataChunk[offset + j]);
                  if(j < (nSamps-1)) buf.append(" ");
               }
               buf.append(CRLF);
               offset += nSamps;
            }
         }
         else
         {
            int nr = (int) dataChunk[1];
            int nc = (int) dataChunk[2];
            int offset = 3;
            for(int i=0; i<nr; i++)
            {
               for(int j=0; j<nc; j++)
               {
                  buf.append(dataChunk[offset + j]);
                  if(j < (nc-1)) buf.append(" ");
               }
               buf.append(CRLF);
               offset += nc;
            }
         }
         
         cachedText = buf.toString();
         dataAsText = new WeakReference<>(cachedText);
         return(cachedText);
      }

      public DataFlavor[] getTransferDataFlavors() 
      { 
         return(new DataFlavor[] {dataChunkFlavor, DataFlavor.stringFlavor}); 
      }

      public boolean isDataFlavorSupported(DataFlavor flavor)
      {
         return(dataChunkFlavor.equals(flavor) || DataFlavor.stringFlavor.equals(flavor));
      }
      
      /**
       * Construct a <code>DataSetChunkTransferable</code> wrapping the specified array. 
       * @param chunk An array holding the dataset chunk. First three elements provide info about the shape of the data
       * stored in the remainder of the array. The first element is nonzero if the array contains <code>RASTER1D</code>
       * data, and the second and third elements are the length and breadth of the chunk. The remainder of the array is 
       * organized in the exact same manner as the raw data array of the <code>DataSet</code> from which the chunk was 
       * originally copied. <b>NOTE that the method assumes the array argument satisfies this structure!</b>
       */
      DataSetChunkTransferable(float[] chunk) { dataChunk = chunk; }
      
      /** 
       * Does this transferable object contain <code>RASTER1D</code> data? If so, the first element of the internal data
       * array is nonzero. The "shape" of the actual data chunk is very different for raster data versus the other
       * <i>DataNav</i> dataset formats.
       * @return <code>True</code> iff this transferable contains raster data.
       */
      @SuppressWarnings("unused")
      boolean isRasterData() { return(dataChunk[0] != 0); }
      
      /**
       * Get the length of the data chunk encapsulated by this transferable object (the second element of the internal
       * data array, cast to integer type).
       * @return The data chunk length. Its meaning is the same as that of a normal <i>DataNav</i> dataset.
       */
      @SuppressWarnings("unused")
      int getDataChunkLength() { return((int)dataChunk[1]); }
      
      /**
       * Get the breadth of the data chunk encapsulated by this transferable object (the third element of the internal
       * data array, cast to integer type).
       * @return The data chunk breadth. Its meaning is the same as that of a normal <i>DataNav</i> dataset.
       */
      @SuppressWarnings("unused")
      int getDataChunkBreadth() { return((int)dataChunk[2]); }
      
      /**
       * Get an independent copy of the actual data chunk encapsulated by this transferable object (the internal data
       * array except for the first three elements).
       * @return The data chunk. It is organized in the same fashion as the raw data array of a normal <i>DataNav</i> 
       * dataset. Note that traversing the chunk correctly requires knowledge of the chunk length and breadth and 
       * whether or not it is raster data.
       */
      @SuppressWarnings("unused")
      float[] getDataChunk() 
      {
         float[] chunk = new float[dataChunk.length-3];
         System.arraycopy(dataChunk, 3, chunk, 0, dataChunk.length-3);
         return(chunk);
      }
      
      /** The transferable data chunk. */
      private final float[] dataChunk;
      
      /** 
       * A weak reference to the data converted to string form. This is here in case the data is requested in string
       * form more than once. For large data chunks, it takes significant time to generate the text. This is here to
       * avoid that overhead if we can (ie, if the string has not been garbarge-collected).
       */
      private WeakReference<String> dataAsText = null;
   }
}
