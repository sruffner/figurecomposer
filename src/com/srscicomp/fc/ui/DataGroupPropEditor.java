package com.srscicomp.fc.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

import com.srscicomp.common.ui.ColorCellEditor;
import com.srscicomp.common.ui.ColorCellRenderer;
import com.srscicomp.fc.fig.FGNPlottableData;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.PieChartNode;
import com.srscicomp.fc.uibase.StyledTextCellEditor;
import com.srscicomp.fc.uibase.StyledTextCellRenderer;

/**
 * <b>DataGroupPropEditor</b> displays/edits the fill colors and legend labels associated with the individual data
 * groups in a "grouped-data" presentation node -- such as a bar plot, area chart, or pie chart. When the edited node is
 * a pie chart, it also displays/edits the "displaced" flag for each data group; the pie slice representing a data group
 * is offset radially when this flag is set. The editor panel is intended only to be used as a component within the 
 * property editor for such data presentation nodes.
 * 
 * <p>It consists of a simple scrolled table with 2 or 3 fixed columns: the displaced flag in column 1 if applicable;
 * the fill color in column 1 or 2; the legend label in column 2 or 3. The table cells are editable. An {@link 
 * StyledTextCellEditor} and {@link StyledTextCellRenderer} modify and display the legend label as <i>FypML</i>
 * "styled text"; a {@link ColorCellEditor} and {@link ColorCellRenderer} modify and display the fill color; and a check
 * box displays and toggles the state of the "displaced" flag.</p>
 * 
 * @author sruffner
 */
final class DataGroupPropEditor extends JPanel
{
   /**
    * Construct the data group property editor panel. The panel is configured differently for a pie chart, which has
    * a "slice displaced" flag for each data group as well as the fill color and legend label.
    * 
    * @param isPie True if panel should be configured to edit a pie chart; otherwise, it is configured to edit a bar or
    * area chart. 
    */
   DataGroupPropEditor(boolean isPie)
   {
      super(new BorderLayout());
      this.isPie = isPie;
      
      propsTable = new JTable(new DataGrpPropsTM());
      propsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      propsTable.setDefaultRenderer(Color.class, new ColorCellRenderer());
      propsTable.setDefaultEditor(Color.class, new ColorCellEditor(new Dimension(COLW_COLOR, ROWH), true));
      propsTable.setDefaultRenderer(String.class, new StyledTextCellRenderer());
      propsTable.setDefaultEditor(String.class, new StyledTextCellEditor());
      propsTable.setRowHeight(ROWH);
      propsTable.setShowHorizontalLines(true);
      propsTable.setGridColor(Color.LIGHT_GRAY);
      propsTable.setColumnModel(new DGPropsTCM());
      propsTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
      propsTable.setPreferredScrollableViewportSize(
            new Dimension(COLW_COLOR*4, 6*propsTable.getRowHeight()));

      JScrollPane scroller = new JScrollPane(propsTable);
      scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      add(scroller, BorderLayout.CENTER);
   }
   
   /** 
    * Get the grouped-data presentation node currently loaded into this editor.
    * @return The currently edited data presentation node; null if no such node is currently loaded.
    */
   public FGNPlottableData getEditedNode() { return(plottable); }
   
   /**
    * Reload this editor to display and modify the properties associated with the different data groups in the specified
    * "grouped-data" presentation node. 
    * @param fpd The grouped-data node to be edited.
    * @return True if node was loaded into editor; false if <i>n == null</i> or does not support data groups. In the
    * latter case, the editor panel is hidden.
    */
   public boolean loadPlottableNode(FGNPlottableData fpd)
   {
      plottable = (fpd != null && fpd.hasDataGroups()) ? fpd : null;
      if(plottable != null && isPie && (plottable.getNodeType() != FGNodeType.PIE)) plottable = null;
      
      setVisible(plottable != null);
      if(plottable == null) return(false);
      
      ((DataGrpPropsTM) propsTable.getModel()).reload();
      return(true);
   }
   
   /**
    * Call this method prior to hiding the data group property editor. It makes sure that the transient pop-up window 
    * associated with the in-place color picker (for selecting fill color in the table) is hidden.
    */
   public void cancelEditing()
   { 
      propsTable.getDefaultEditor(Color.class).cancelCellEditing();
   }
   
   /** The "grouped-data" presentation node currently being edited. Null if no node is loaded into editor panel. */
   private FGNPlottableData plottable = null;
   
   /** True if editor panel configured to edit a pie chart; else, a bar or area chart. */
   private final boolean isPie;
   
   /** The properties for each data group are displayed and edited in this table. */
   private final JTable propsTable;
   
   /** Fixed width of table columns displaying fill color. */
   private final static int COLW_COLOR = 80;
   /** Fixed width of table columns displaying the displaced flag for a data group (pie chart only). */
   private final static int COLW_DISPLACE = 60;
   /** Height of RGB color picker used as color editor in bar groups table. Determines table's fixed row height. */
   private final static int ROWH = 36;
   
   /**
    * A simple table model exposing the properties assigned to the grouped-data presentation node's different data
    * groups. The number of rows in the model matches the number of data groups. If the loaded node is a pie chart,
    * there are 3 columns: displaced-slice flag, fill color, and legend label. Otherwise, there are two columns: the
    * fill color in column 0 and legend label in column 1.
    * @author sruffner
    */
   private class DataGrpPropsTM extends AbstractTableModel
   {
      @Override public int getRowCount() { return(nRows); }
      @Override public int getColumnCount() { return(isPie ? 3 : 2); }
      @Override public Class<?> getColumnClass(int c) 
      { 
         if(isPie)
            return(c==0 ? Boolean.class : (c==1 ? Color.class : String.class));
         else
            return(c==0 ? Color.class : String.class); 
      }
      @Override public String getColumnName(int c) 
      { 
         if(c < 0 || c >= getColumnCount()) return("");
         return(COL_LABELS[isPie ? c : c+1]);
      }
      
      @Override public Object getValueAt(int r, int c)
      {
         Object out = null;
         if(plottable != null && r >= 0 && r<nRows && c >= 0 && c < getColumnCount())
         {
            if(isPie && c == 0) out = ((PieChartNode) plottable).isSliceDisplaced(r);
            else if(c == 0 || (isPie && c==1)) out = plottable.getDataGroupColor(r);
            else out = plottable.getDataGroupLabel(r);
         }
         return(out);
      }
      
      @Override public boolean isCellEditable(int r, int c) 
      { 
         return(r >= 0 && r < nRows && c >= 0 && c < getColumnCount());
      }
      
      @Override public void setValueAt(Object value, int r, int c)
      {
         if(value==null || plottable==null || !value.getClass().equals(getColumnClass(c)) || !isCellEditable(r, c))
            return;
         
         boolean ok;
         if(isPie && c == 0) ok = ((PieChartNode)plottable).setSliceDisplaced(r, (Boolean)value);
         else if(c == 0 || (isPie && c==1)) ok =  plottable.setDataGroupColor(r, (Color)value);
         else ok = plottable.setDataGroupLabel(r, (String)value);
         
         if(ok) fireTableCellUpdated(r,c);
      }
      
      private void reload()
      {
         nRows = (plottable != null) ? plottable.getNumDataGroups() : 0;
         fireTableStructureChanged();
      }
      
      /** Number of rows in the table is the always the number of data groups in the plottable data node. */
      private int nRows = 0;
   }

   /** Column labels for the properties table. */
   private final static String[] COL_LABELS = new String[] {"Displace?", "Fill Color", "Legend Label"};
   
   /**
    * Table column model which handles the two possible configurations of the table: two columns for bar and area 
    * charts, but three columns for pie charts.
    * 
    * @author sruffner
    */
   private class DGPropsTCM extends DefaultTableColumnModel
   {
      @Override public void addColumn(TableColumn column)
      {
         int nCols = getColumnCount();
         if(nCols == 0 || (isPie && nCols == 1))
         {
            int w = (isPie && nCols == 0) ? COLW_DISPLACE : COLW_COLOR;
            column.setPreferredWidth(w);
            column.setMinWidth(w);
            column.setMaxWidth(w);
            column.setResizable(false);
         }
         else
            column.setPreferredWidth(COLW_COLOR*3);
         super.addColumn(column);
      }
   }
}
