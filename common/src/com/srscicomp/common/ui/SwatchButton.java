package com.srscicomp.common.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;

import javax.swing.*;

import com.srscicomp.common.util.Utilities;

/** 
 * <b>SwatchButton</b> is a custom-painted button that represents a specified {@link BkgFill}. It serves as the base 
 * class for {@link RGBColorPicker} and {@link BkgFillPicker}, and is also used to render the preview swatches in the 
 * internal pop-up editor panels associated with these custom "color picker" components. 
 * 
 * <p>The bounding rectangle, inside a border margin, is filled IAW the background fill currently associated with the 
 * button, while a custom border reflects rollover, focus, and disabled effects. To depict a translucent (alpha < 255) 
 * solid-color fill, a 4x4 gray and white checkerboard is drawn within the component's bounding rectangle, and then half
 * of this rectangle is painted with the non-opaque solid color, and the other half is painted with the opaque version.
 * If the fill is completely transparent, a red "X" is drawn.</p>
 * 
 * <p>The default size of the swatch region is 16x16 with a margin of 3 pixels all around, resulting in an overall size
 * of 22x22 pixels. You can specify swatch width and height and the margin size; however, both width and height are 
 * restricted to [16..100] in multiples of 4, while the margin size is restricted to [3..12] pixels.</p>
 * 
 * <p>While <b>SwatchButton</b> subclasses {@link JButton}, it is intended as a self-contained component. Do NOT use
 * {@link #setText(String)} , {@link #setIcon(Icon)} ()}, or other such <b>JButton</b> methods -- doing so will
 * interfere with this button's intended behavior.</p>
 * 
 * @author sruffner
 */
class SwatchButton extends JButton
{
   /**
    * Create a 22x22 swatch button with a 3-pixel margin and 16x16 swatch size. The button initially represents a solid 
    * fill in opaque black. Border effects are enabled, using default colors.
    */
   SwatchButton() { this(16, 16, 3); }
   
   /** 
    * Create a swatch button with specified swatch dimensions WxH and margin size M. The overall dimensions of the 
    * button will be (W+2M) x (H+2M). The button initially represents a solid fill in opaque black. Border effects are
    * enabled, using default colors.
    * 
    * @param w The (fixed) width of the rectangular swatch area within the button rectangle. It is auto-corrected as
    * needed to ensure it is a multiple of 4 in [16..64].
    * @param h The (fixed) height of the rectangular swatch area within the button rectangle. It is auto-corrected as
    * needed to ensure it is a multiple of 4 in [16..64].
    * @param m The size of the margin in which the button border is painted. It is range-restricted to [3..12].
    */
   SwatchButton(int w, int h, int m) { this(w, h, m, null, null, null, null); }
   
   /** 
    * Create a swatch button with specified swatch dimensions WxH and margin size M. The overall dimensions of the 
    * button will be (W+2M) x (H+2M). The button initially represents a solid fill in opaque black. Border effects are
    * enabled, using the colors specified; if any of these colors is null, a default color is used. To disable rollover 
    * effect, use {@link #setRolloverEnabled(boolean)}; to disable focus effect, use {@link #setFocusable(boolean)}.
    * 
    * @param w The (fixed) width of the rectangular swatch area within the button rectangle. It is auto-corrected as
    * needed to ensure it is a multiple of 4 in [16..64].
    * @param h The (fixed) height of the rectangular swatch area within the button rectangle. It is auto-corrected as
    * needed to ensure it is a multiple of 4 in [16..64].
    * @param m The size of the margin in which the button border is painted. It is range-restricted to [3..12].
    * @param normalC Color of border if button is enabled and no transient effects are in play.
    * @param disabledC Color of border if button is disabled.
    * @param focusC Color of border if button is enabled and currently has the focus.
    * @param rolloverC Highlight color used to render the rollover effect. The margin background is filled with this
    * color, while the border is painted in the focus color or -- if the button also has the focus, a combination of
    * this color and the focus color.
    */
   SwatchButton(int w, int h, int m, Color normalC, Color disabledC, Color focusC, Color rolloverC)
   {
      super();
      
      // restrict dimensions of the swatch rectangle and the margin
      while((w % 4) != 0) ++w;
      while((h % 4) != 0) ++h;
      swatchW = Utilities.rangeRestrict(16, 64, w);
      swatchH = Utilities.rangeRestrict(16, 64, h);
      margin = Utilities.rangeRestrict(3, 12, m);
      
      // set colors used for border effects
      this.normalC = (normalC != null) ? normalC : defaultNormalC;
      this.disabledC = (disabledC != null) ? disabledC : defaultDisabledC;
      this.focusC = (focusC != null) ? focusC : defaultFocusC;
      this.rolloverC = (rolloverC != null) ? rolloverC : defaultRolloverC;
      
      // we take care of painting the button ourselves...
      setOpaque(false); 
      setContentAreaFilled(false);
      setBorderPainted(false);
      setFocusPainted(false);
      setRolloverEnabled(true);
      setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
      
      // fix the overall button size
      Dimension sz = new Dimension(swatchW + 2*margin, swatchH + 2*margin);
      setPreferredSize(sz);
      setMinimumSize(sz);
      setMaximumSize(sz);
   }
   
   /**
    * Get the current background fill associated with this swatch button.
    * @return The current background fill.
    */
   BkgFill getBkgFill() { return(bkgFill); }
   
   /**
    * Set the background fill associated with this swatch button. It is repainted to reflect the change.
    * @param bf The new background fill. If null, a solid opaque black fill is assumed.
    */
   void setBkgFill(BkgFill bf)
   {
      if(bkgFill.equals(bf)) return;
      bkgFill = (bf != null) ? bf : BkgFill.createSolidFill(Color.BLACK);
      repaint();
   }
   
   /**
    * Set the background fill for this swatch button to be a solid fill in the specified color. 
    * @param c The color of the new solid background fill. If null, solid opaque black fill is assumed.
    */
   void setBkgFill(Color c) { setBkgFill(BkgFill.createSolidFill(c!=null ? c : Color.BLACK)); }
   
   @Override protected void paintComponent(Graphics g)
   {
      // for the button's current focus/rollover/enable states
      ButtonModel model = getModel();
      boolean hasFocus = isFocusOwner();
      boolean isRollover = model.isRollover();
      
      Graphics2D g2 = (Graphics2D) g.create();
      try
      {
         int w = getWidth();
         int h = getHeight();
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         
         // fill the entire button rectangle with transparent black
         g2.setColor(transparentBlack);
         g2.fillRect(0, 0, w, h);
         
         // draw and fill rounded rectangular border to reflect current focus/rollover/enable state. Note that 
         // border is thicker when button has focus or is in the rollover state.
         Color bkgC = null;
         Color borderC = normalC;
         if(model.isEnabled())
         {
            if(isRollover) bkgC = rolloverC;
            if(isRollover || hasFocus) borderC = focusC;
         }
         else borderC = disabledC;
         
         if(bkgC != null)
         {
            g2.setColor(bkgC);
            g2.fillRoundRect(0, 0, w-1, h-1, 5, 5);
         }
         g2.setColor(borderC);
         g2.drawRoundRect(0, 0, w-1, h-1, 5, 5);
         if(isRollover || hasFocus) 
         {
            // inner border is different from outer border during rollover when button also has focus
            if(isRollover && hasFocus) g2.setColor(rolloverC);
            g2.drawRoundRect(1, 1, w-3, h-3, 5, 5);
         }
         
         // now paint the rectangular color swatch to reflect the current background fill. We do a checkerboard 
         // background when it is a solid fill using a translucent or transparent color.
         g2.translate(margin, margin);
         if(bkgFill.getFillType() == BkgFill.Type.SOLID && bkgFill.getColor1().getAlpha() < 255)
         {
            // 4x4 gray and white checkerboard background. 
            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, swatchW, swatchH);
            g2.setColor(Color.LIGHT_GRAY);
            for(int i=0; i<=3; i++)
            {
               for(int j=0; j<=3; j++) 
               {
                  if(i%2 == j%2) g2.fillRect(i*swatchW/4, j*swatchH/4, swatchW/4, swatchH/4);
               }
            }
            
            // if fully transparent, draw a red "X". Else, draw checkerboard background and cover half with the
            // translucent color and the other half with its fully opaque version.
            Color c = bkgFill.getColor1();
            if(c.getAlpha() == 0)
            {
               g2.setColor(Color.RED);
               g2.setStroke(transparentXStroke);
               g2.drawLine(0, 0, swatchW, swatchH);
               g2.drawLine(0, swatchH, swatchW, 0);
            }
            else
            {
               GeneralPath gp = new GeneralPath();
               gp.moveTo(0, swatchH); gp.lineTo(swatchW, swatchH); gp.lineTo(swatchW, 0); gp.closePath();
               g2.setColor(c);
               g2.fill(gp);
               gp.reset();
               gp.moveTo(0, 0); gp.lineTo(0, swatchH); gp.lineTo(swatchW, 0); gp.closePath();
               g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue()));
               g2.fill(gp);
            }

         }
         else
         {
            g2.translate(0, swatchH);
            g2.scale(1, -1);
            g2.setPaint(bkgFill.getPaintForFill(swatchW, swatchH));
            g2.fillRect(0, 0, swatchW, swatchH);
         }
      }
      finally
      {
         if(g2 != null) g2.dispose();
      }
   }
   
   /** Width of the rectangular swatch that is filled IAW the background fill currently associated with the button. */
   private final int swatchW;
   /** Height of the rectangular swatch that is filled IAW the background fill currently associated with the button. */
   private final int swatchH;
   /** The size of the margin surrounding the button's rectangular swatch; the border is painted in this margin. */
   private final int margin;
   
   /** The normal border color, when button does not have the focus and mouse is not hovering over it. */
   private final Color normalC;
   /** The disabled border color. */
   private final Color disabledC;
   /** The border color when button has the keyboard focus or mouse is hovering over it. */
   private final Color focusC;
   /** The highlight color used when mouse is hovering over the button. */
   private final Color rolloverC;
   
   /** The background fill for the swatch button. */
   private BkgFill bkgFill = BkgFill.createSolidFill(Color.BLACK);

   private final static Color transparentBlack = new Color(0,0,0,0);
   /** Default for normal border color, when button does not have the focus and mouse is not hovering over it. */
   private final static Color defaultNormalC = Color.GRAY;
   /** Default for border color when button is disabled. */
   private final static Color defaultDisabledC = Color.LIGHT_GRAY;
   /** Default border color when button has the keyboard focus or mouse is hovering over it. */
   private final static Color defaultFocusC = new Color(70, 130, 180);
   /** Default highlight color used when mouse is hovering over the button. */
   private final static Color defaultRolloverC = new Color(248, 248, 255);
   /** Stroke used to paint red "X" that stands for a completely transparent color. */
   private final static BasicStroke transparentXStroke = new BasicStroke(1);
}
