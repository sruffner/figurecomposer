package com.srscicomp.fc.uibase;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.fc.fig.FGraphicNode;

/**
 * A table cell editor for in-place editing a single line of <i>FypML</i> "Styled Text". It embeds a compact version of
 * {@link StyledTextEditor}.
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
public class StyledTextCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener
{
   public StyledTextCellEditor()
   {
      editor = new StyledTextEditor(1, false, true);
      editor.addActionListener(this);
   }
   public Object getCellEditorValue() 
   { 
      return(FGraphicNode.toStyledText(editor.getCurrentContents(), FontStyle.PLAIN, Color.black));
   }

   public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
   {
      String s = (value instanceof String) ? ((String) value) : "";
      Font f = table.getFont();
      editor.loadContent(FGraphicNode.fromStyledText(s, f.getFamily(), 0, Color.black, FontStyle.PLAIN, true), 
            f.getFamily(), f.getSize(), Color.black, FontStyle.PLAIN);
      editor.showHideAttributeWidgets(false);
      return(editor);
   }

   private StyledTextEditor editor;

   @Override public void actionPerformed(ActionEvent e) { fireEditingStopped(); }
}
