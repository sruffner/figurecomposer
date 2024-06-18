package com.srscicomp.common.ui;

import javax.swing.DefaultCellEditor;

/**
 * A simple extension of {@link DefaultCellEditor} that uses a {@link NumericTextField} to edit table or tree cells 
 * that contain integer values.
 * 
 * @author sruffner
 */
public class IntegerCellEditor extends DefaultCellEditor
{
   public IntegerCellEditor(int minVal, int maxVal)
   {
      super(new NumericTextField(minVal, maxVal));

      ((NumericTextField)editorComponent).removeActionListener(delegate);

      delegate = new EditorDelegate() 
      {
         public void setValue(Object value) 
         {
            Integer iVal = (value instanceof Integer) ? ((Integer)value) : Integer.valueOf(0);
            ((NumericTextField)editorComponent).setValue(iVal);
         }

         public Object getCellEditorValue() 
         {
             return(((NumericTextField) editorComponent).getValue().intValue());
         }
      };
      ((NumericTextField)editorComponent).addActionListener(delegate);
   }
}

