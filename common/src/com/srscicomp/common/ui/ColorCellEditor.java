package com.srscicomp.common.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 * A simple in-place table cell editor that is intended for editing a {@link Color} object displayed in a table cell. It
 * uses the {@link RGBColorPicker} component to do the editing. Has some focus change issues.
 * <p>(22jan2015) Revised to allow specifying certain characteristics of the delegate RGB color picker.</p>
 * @author sruffner
 */
public class ColorCellEditor extends AbstractCellEditor implements TableCellEditor, PropertyChangeListener
{
   /**
    * Construct a color cell editor in which the embedded color picker button is minimum size and supports opaque
    * colors only.
    */
   public ColorCellEditor() { this(null, false); }
   
   /**
    * Construct a color cell editor.
    * @param sz Desired size of the embedded color picker button.
    * @param enaAlpha True if editor supports changing the color's alpha component; else, opaque colors only.
    */
   public ColorCellEditor(Dimension sz, boolean enaAlpha)
   {
      picker = new RGBColorPicker(sz, enaAlpha);
      picker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
      picker.addPropertyChangeListener(RGBColorPicker.POPUPVISIBLE_PROP, this);
   }
   
   public Object getCellEditorValue() { return(picker.getCurrentColor()); }

   public Component getTableCellEditorComponent(JTable table, Object value,
         boolean isSelected, int row, int column)
   {
      Color c = (value instanceof Color) ? ((Color) value) : Color.BLACK;
      picker.setCurrentColor(c, false);
      return(picker);
   }
   
   @Override public void cancelCellEditing()
   {
      picker.cancelPopup();
      super.cancelCellEditing();
   }
   
   public void propertyChange(PropertyChangeEvent e)
   {
      String prop = e.getPropertyName();
      if(prop == null || e.getSource() != picker) return;
      
      if(prop.equals(RGBColorPicker.COLOR_PROPERTY)) fireEditingStopped();
      else if(prop.equals(RGBColorPicker.POPUPVISIBLE_PROP))
      {
         Object value = e.getNewValue();
         if(value != null && value.equals(Boolean.FALSE)) fireEditingCanceled();
      }
   }
   
   private final RGBColorPicker picker;
}
