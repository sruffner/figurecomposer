package com.srscicomp.fc.uibase;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import com.srscicomp.fc.fig.ColorLUT;
import com.srscicomp.fc.fig.ColorMap;

/**
 * A utility class serving as the renderer for a combo box that displays/selects {@link ColorMap}. The renderer draws a 
 * gradient depicting the colormap's lookup table.
 * @author sruffner
 */
public class ColorMapGradientRenderer extends JComponent implements ListCellRenderer<ColorMap>
{
   private static final long serialVersionUID = 1L;

   /** 
    * Constructs a <code>ColorMapGradientRenderer</code> initialized to display a grayscale colormap in the 
    * unselected state.
    */
   public ColorMapGradientRenderer() { setOpaque(true); }
   
   /** The color map to be rendered. */
   private ColorMap cmap = ColorMap.BuiltIn.gray.cm;
   /** Background color of list control in which this list cell renderer is used. */
   private Color listBkgColor = Color.WHITE;
   /** Flag is set if renderer is painting the colormap in the selected state (a border is included). */
   private boolean selected = false;
   
   @Override public Component getListCellRendererComponent(JList<? extends ColorMap> list, ColorMap value,
         int index, boolean isSelected, boolean cellHasFocus)
   {
      listBkgColor = list.getBackground();
      cmap = value;
      selected = isSelected;
      return(this);
   }

   @Override protected void paintComponent(Graphics g)
   {
      int w = getWidth();
      int h = getHeight();

      Graphics2D g2d = (Graphics2D) g.create();
      try
      {
         if(cmap == null)
         {
            g2d.setColor(listBkgColor);
            g2d.fillRect(0, 0, w, h);
            return;
         }
         else
         {
            ColorLUT lut = new ColorLUT(cmap, false);
            g2d.setPaint(lut.getGradientPaint(0, ((float) h)/2f, w, ((float) h)/2f));
            g2d.fillRect(0,  0,  w,  h);
            
            // use black for the map name, unless the middle of the LUT is rather dark...
            Color midC = new Color(lut.getRGB(127));
            Color textC = (midC.getRed()*0.2126 + midC.getGreen()*0.7152 + midC.getBlue()*0.0722 < 128) ? 
                  Color.white : Color.black;
            Font font = getFont();
            g2d.setFont(font);
            TextLayout layout = new TextLayout(cmap.toString(), font, g2d.getFontRenderContext());
            Rectangle2D bounds = layout.getBounds();
            g2d.setColor(textC);
            float x = w/2f - ((float) bounds.getWidth()) / 2f;
            float y = h/2f + ((float) bounds.getHeight()) / 2f;
            g2d.drawString(cmap.toString(), x, y);     
         }

         if(selected) 
         {
            g2d.setColor(listBkgColor);
            g2d.drawRect(0, 0, w-1, h-1);
         }
      }
      finally { if(g2d != null) g2d.dispose(); }
   }
   
   // 
   // All of the following are overridden for performance reasons, following the example of DefaultListCellRenderer
   //
   @Override public boolean isOpaque() 
   { 
      Color back = getBackground();
      Component p = getParent(); 
      if(p != null) p = p.getParent(); 

      // p should now be the JList. 
      boolean colorMatch = (back != null) && (p != null) && back.equals(p.getBackground()) && p.isOpaque();
      return( !colorMatch && super.isOpaque() ); 
   }

   @Override public void validate() {}
   @Override public void invalidate() {}
   @Override public void repaint() {}
   @Override public void revalidate() {}
   @Override public void repaint(long tm, int x, int y, int width, int height) {}
   @Override public void repaint(Rectangle r) {}
   @Override protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {}
   @Override public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {}
   @Override public void firePropertyChange(String propertyName, char oldValue, char newValue) {}
   @Override public void firePropertyChange(String propertyName, short oldValue, short newValue) {}
   @Override public void firePropertyChange(String propertyName, int oldValue, int newValue) {}
   @Override public void firePropertyChange(String propertyName, long oldValue, long newValue) {}
   @Override public void firePropertyChange(String propertyName, float oldValue, float newValue) {}
   @Override public void firePropertyChange(String propertyName, double oldValue, double newValue) {}
   @Override public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {}
}
