package com.srscicomp.fc.uibase;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.srscicomp.common.g2dutil.BasicPainterStyle;
import com.srscicomp.common.g2dutil.SingleStringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.fc.fig.FGraphicNode;

/**
 * A table cell renderer that renders a single line of <i>FypML</i> "Styled Text".
 * 
 * @author sruffner
 */
public class StyledTextCellRenderer extends DefaultTableCellRenderer
{
   
   public Component getTableCellRendererComponent(
         JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
   {
      String strValue = (value instanceof String) ? ((String)value) : "";
      setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
      setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
      Font f = table.getFont();
      painter.setStyle(BasicPainterStyle.createBasicPainterStyle(f, 1, null, Color.white, getForeground()));
      painter.setAlignment(TextAlign.LEADING, TextAlign.CENTERED);
      painter.setText(FGraphicNode.fromStyledText(strValue, f.getFamily(), 0, getForeground(), FontStyle.PLAIN, true));
      
      return(this);
   }
   
   @Override protected void paintComponent(Graphics g)
   {
      Color bkgC = getBackground();
      // Color fgC = getForeground();

      
      Graphics2D g2 = (Graphics2D) g.create();
      try
      {
         int w = getWidth();
         int h = getHeight();
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2.setColor(bkgC);
         g2.fillRect(0, 0, w, h);
         g2.scale(1, -1);
         painter.setLocation(2, -(h/2.0));
         painter.render(g2, null);
         g2.scale(1, -1);
      }
      finally
      {
         if(g2 != null) g2.dispose();
      }
   }

   private final SingleStringPainter painter = new SingleStringPainter();
}

