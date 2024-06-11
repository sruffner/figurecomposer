package com.srscicomp.common.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * A simple table cell renderer representing a {@link Color Color} value. It essentially fills most of the cell
 * with that color, except for a narrow border painted in the parent table's background or selection background color,
 * depending on the cell's selection state (no border is drawn if the component is smaller than 26 x 14). Its tool tip 
 * text reflects the current color value, as constructed via {@link RGBColorPicker#getColorString(Color)}.
 * 
 * <p>If the current color is not opaque, the cell is covered by a gray-and-white checker board to indicate that the
 * color is translucent, ie, alpha < 255. If alpha == 0 (transparent), a red "X" is drawn on top. Else, the lower
 * diagonal half of the checker board is covered by the translucent color, while the upper half is covered by its
 * opaque version.</p>
 * 
 * @author sruffner
 */
public class ColorCellRenderer extends DefaultTableCellRenderer
{
   // note that the represented color is stored as the component background, and the border color as its foreground
   public Component getTableCellRendererComponent(
         JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
   {
      Color newColor = (value instanceof Color) ? ((Color)value) : Color.BLACK;
      setBackground(newColor);
      setForeground(isSelected ? table.getSelectionBackground() : table.getBackground());
      setToolTipText(RGBColorPicker.getColorString(newColor));
      return(this);
   }
   
   @Override protected void paintComponent(Graphics g)
   {
      Color c = getBackground();
      Color borderC = getForeground();

      Graphics2D g2 = (Graphics2D) g.create();
      try
      {
         int w = getWidth();
         int h = getHeight();
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         
         // if color is opaque, fill the component rectangle with that color
         if(c.getAlpha() == 255)
         {
            g2.setColor(c);
            g2.fillRect(0, 0, w, h);
         }
         else
         {
            // start with a gray-and-white checker board with 5x5-pixel blocks
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, w, h);
            g2.setColor(Color.LIGHT_GRAY);
            int nW = w/5;
            int nH = h/5;
            for(int i=0; i<nW; i++)
            {
               for(int j=0; j<nH; j++)
               {
                  if(i%2 == j%2) g2.fillRect(i*5, j*5, 5, 5);
               }
            }
            
            // if color is fully transparent, draw a red "X" over checker board. Otherwise, fill lower diagonal half 
            // with the translucent color and the upper diagonal half with its opaque version.
            if(c.getAlpha() == 0)
            {
               g2.setColor(Color.RED);
               g2.setStroke(transparentXStroke);
               g2.drawLine(0, 0, w, h);
               g2.drawLine(0, h, w, 0);
            }
            else
            {
               GeneralPath gp = new GeneralPath();
               gp.moveTo(0, h); gp.lineTo(w, h); gp.lineTo(w, 0); gp.closePath();
               g2.setColor(c);
               g2.fill(gp);
               gp.reset();
               gp.moveTo(0, 0); gp.lineTo(0, h); gp.lineTo(w, 0); gp.closePath();
               g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue()));
               g2.fill(gp);
            }
         }
         
         // finally draw an opaque border in the margins that's 2pix wide on T/B, 8pix wide L/R. No border if the
         // component is not "big enough". Also draw an etched border to highlight the color-filled rectangle
         g2.setColor(borderC);
         if(borderPath == null || lastW != w || lastH != h)
         {
            lastW = w;
            lastH = h;
            if(borderPath == null) borderPath = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
            else borderPath.reset();
            if(w-16 >= 10 && h-4 >= 10)
            {
               borderPath.moveTo(0, 0); borderPath.lineTo(w, 0); borderPath.lineTo(w, h);
               borderPath.lineTo(0, h); borderPath.closePath();
               borderPath.moveTo(8, 2); borderPath.lineTo(w-8, 2); borderPath.lineTo(w-8, h-2);
               borderPath.lineTo(8, h-2); borderPath.closePath();
            }
         }
         g2.fill(borderPath);
         if(w-16 >= 10 && h-4 >= 10)
         {
            g2.setColor(Color.black);
            g2.drawRect(8, 2, w-16, h-4);
            g2.setColor(Color.white);
            g2.drawRect(9, 3, w-18, h-6);
         }
      }
      finally
      {
         if(g2 != null) g2.dispose();
      }
   }

   /** A rectangular annulus around the margins of the component. Rebuilt whenever component size changes. */
   private GeneralPath borderPath = null;
   private int lastW = -1;
   private int lastH = -1;
   
   /** Stroke used to paint red "X" that stands for a completely transparent color. */
   private final static BasicStroke transparentXStroke = new BasicStroke(1);
}

