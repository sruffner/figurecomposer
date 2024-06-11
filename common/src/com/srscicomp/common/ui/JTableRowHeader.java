package com.srscicomp.common.ui;

import java.awt.Component;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

/**
 * A table implementation of a row header for another table. It maintains a reference to the main table in order to
 * stay in sync with that table, and it uses the main table model to determine the number of rows in the header and
 * the label for each row. To use it, it must be installed as the row header component in the scroll pane container for 
 * the main table.
 * 
 * <p>By default, the label for each row will simply reflect the row number in string form ("1", "2", and so on). If
 * you would like to customize the row labels, the main table model must implement the {@link RowHeaderTM} interface;
 * this interface augments {@link TableModel} with a method that supplies a header label for each row: {@link 
 * RowHeaderTM#getRowName(int)}.</p>
 * 
 * <p>The helper class {@link Renderer} implements a cell renderer for the row header table that attempts to mimic the
 * appearance of {@link JTableHeader}. If you want some additional control over the appearance, you can specify your
 * own renderer in the constructor.</p>
 * 
 * <p>CREDIT: Based on Rob Camick's RowNumberTable, https://tips4java.wordpress.com/2008/11/18/row-number-table.</p>
 *  
 * @author sruffner
 */
public class JTableRowHeader extends JTable implements ChangeListener, PropertyChangeListener
{
   /** The row header stays in sync with this table. */
   private final JTable main;

   /**
    * Construct a row header for the specified table.
    * @param table The main table. Cannot be null.
    * @param fixedW The fixed width for the row header table's single column. Values less than 10 are ignored, in which
    * case the width defaults to 50.
    * @param r (Optional) If not null, this is installed as the renderer for the single column of the row header table.
    */
   public JTableRowHeader(JTable table, int fixedW, TableCellRenderer r)
   {
      if(table == null) throw new NullPointerException();
      main = table;
      main.addPropertyChangeListener(this);

      setFocusable(false);
      setAutoCreateColumnsFromModel(false);
      
      setRowHeight(main.getRowHeight());
      setModel(main.getModel());
      setSelectionModel(main.getSelectionModel());

      TableColumn column = new TableColumn();
      column.setHeaderValue(" ");
      addColumn(column);
      column.setCellRenderer((r==null) ? new Renderer() : r);

      getColumnModel().getColumn(0).setPreferredWidth((fixedW < 10) ? 50 : fixedW);
      setPreferredScrollableViewportSize(getPreferredSize());
      getTableHeader().setReorderingAllowed(false);
   }

   /** Overridden to ensure row header stays in sync with the main table. */
   @Override public void addNotify()
   {
      super.addNotify();
      Component c = getParent();
      if(c instanceof JViewport) ((JViewport)c).addChangeListener(this);
   }

   /** Overridden to ensure row header row count matches the main table. */
   @Override public int getRowCount() { return(main.getRowCount()); }

   /** Overridden to ensure row heights track those in the main table. */
   @Override public int getRowHeight(int row) { return(main.getRowHeight(row)); }

   /**
    * Overridden to get the label for each row in the row header. If the main table's model implements {@link 
    * RowHeaderTM}, then it is used to get the row label. Else, the row number (index + 1) is returned
    * in string form.
    */
   @Override public Object getValueAt(int row, int column)
   {
      TableModel tm = main.getModel();
      if(tm instanceof RowHeaderTM) return(((RowHeaderTM) tm).getRowName(row));
      return(Integer.toString(row + 1));
   }

   /** The row header is not editable. */
   @Override  public boolean isCellEditable(int row, int column) { return(false); }
   @Override public void setValueAt(Object value, int row, int column) {}
   
   /** Handler ensures scrolling of row header is in sync with main table. */
   public void stateChanged(ChangeEvent e)
   {
      JViewport viewport = (JViewport) e.getSource();
      JScrollPane scrollPane = (JScrollPane)viewport.getParent();
      scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
   }
   
   /** Update the row header in response to changes in the main table's table model, selection model, and row height. */
   public void propertyChange(PropertyChangeEvent e)
   {
      if("selectionModel".equals(e.getPropertyName())) setSelectionModel(main.getSelectionModel());
      else if("rowHeight".equals(e.getPropertyName())) setRowHeight(main.getRowHeight());
      else if("model".equals(e.getPropertyName())) setModel(main.getModel());
   }

   /** Whenever the main table model changes, we resize and repaint the row header. */
   @Override public void tableChanged(TableModelEvent e) { revalidate(); repaint(); }

   /*
    *  Attempt to mimic the table header renderer
    */
   /**
    * A renderer for the row header table cells that attempts to mimic the renderer for a JTable's column header, 
    * {@link JTableHeader}. 
    * @author sruffner
    */
   public static class Renderer extends DefaultTableCellRenderer
   {
      public Renderer() {}

      public Component getTableCellRendererComponent(
         JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
      {
         if(table != null)
         {
            JTableHeader header = table.getTableHeader();
            if(header != null)
            {
               setForeground(header.getForeground());
               setBackground(header.getBackground());
               setFont(header.getFont());
            }
         }
         if(isSelected) setFont(getFont().deriveFont(Font.BOLD));

         setText((value == null) ? "" : value.toString());
         setBorder(UIManager.getBorder("TableHeader.cellBorder"));
         setHorizontalAlignment(DefaultTableCellRenderer.CENTER);
         setVerticalAlignment(DefaultTableCellRenderer.CENTER);
         return this;
      }
   }
   
   /** A simple {@link TableModel} extension that provides a label for each row in the table. */
   public interface RowHeaderTM extends TableModel
   {
      /**
       * Get the name of the row at the specified index, intended for display in a row header. Row names need not be 
       * unique.
       * @param row The row index.
       * @return The name of that row.
       */
      String getRowName(int row);
   }
}
