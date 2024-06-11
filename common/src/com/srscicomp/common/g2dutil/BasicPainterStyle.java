package com.srscicomp.common.g2dutil;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;

import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.GenericFont;
import com.srscicomp.common.ui.LocalFontEnvironment;

/**
 * BasicPainterStyle is an immutable implementation of PainterStyle. All attributes are specified at construction time.
 * The miter limit for mitered joins is fixed at the recommended default value of 10.0.
 * 
 * @author 	sruffner
 */
public class BasicPainterStyle implements PainterStyle
{
   private final Font font;
   private final double strokeWidth;
   private final StrokeCap strokeCap;
   private final StrokeJoin strokeJoin;
   private final float[] strokePattern;
   private final Color strokeColor;
   private final Color fillColor;
   

   /**
    * Update the specified painter style's fill color.
    * @param style A painter style. If null, method returns null.
    * @param fillC New fill color
    * @return A copy of the specified painter style, but using the fill color specified.
    */
   public static BasicPainterStyle updateFillColor(BasicPainterStyle style, Color fillC)
   {
      if(style == null) return(null);
      return(new BasicPainterStyle(style.font, (float) style.strokeWidth, style.strokeCap, style.strokeJoin,
            style.strokePattern, style.strokeColor, fillC));
   }
   
   /**
    * Create a <b>BasicPainterStyle</b> representing a plain Arial font backed up by a generic sans-serif font, 
    * with a 10-unit font size, a solid black stroke of unit width with butt ends and mitered joins, and a white fill.
    * 
    * @return A <b>BasicPainterStyle</b>, as described.
    */
   public static BasicPainterStyle createDefaultPainterStyle()
   {
      return( new BasicPainterStyle("Arial", GenericFont.SANSERIF, FontStyle.PLAIN, 10f, 1f, 
               null, null, null, 0, 0x00ffffff) );
   }

   /**
    * Create a painter style with a plain Arial font, a 10-unit font size, a black fill, and ZERO stroke width.
    * @return A <b>BasicPainterStyle</b>, as described.
    */
   public static BasicPainterStyle createNoStrokePainterStyle()
   {
      return( new BasicPainterStyle("Arial", GenericFont.SANSERIF, FontStyle.PLAIN, 10f, 0f, null, null, null, 0, 0) );
   }
   
   /**
    * Create a <code>BasicPainterStyle</code> representing the specified font, a solid black stroke of unit width with 
    * butt ends and mitered joins, and a white fill.
    * 
    * @param family The name of the desired font family.
    * @param generic A generic substitute font, in the event that the font family specified is not available.
    * @param fontStyle Desired font style.
    * @param fontSize Desired font size in logical units.
    * 
    * @return A <code>BasicPainterStyle</code>, as described.
    */
   public static BasicPainterStyle createBasicPainterStyle(String family, GenericFont generic, FontStyle fontStyle, 
         float fontSize)
   {
      return( new BasicPainterStyle(family, generic, fontStyle, fontSize, 1f, null, null, null, 0, 0x00ffffff) );
   }

   /**
    * Create a <code>BasicPainterStyle</code> that uses the font and stroke properties supplied. Strokes will default 
    * to using butt ends and mitered joins.
    * 
    * @param font The desired font. If <code>null</code>, a default font will be supplied.
    * @param strokeW The desired stroke width in logical units.
    * @param dashgap An array specifying the lengths of alternating dashes and gaps in the stroke dash pattern. Lengths 
    * are specified in logical units. A <code>null</code> or empty array represents a solid stroke.
    * @param strokeC Desired stroke color as a <code>java.awt.Color</code>.
    * @param fillC Desired fill color as a <code>java.awt.Color</code>.
    * 
    * @return A <code>BasicPainterStyle</code>, as described.
    */
   public static BasicPainterStyle createBasicPainterStyle(Font font, double strokeW, float[] dashgap, Color strokeC, 
         Color fillC)
   {
      return( new BasicPainterStyle(font, strokeW, strokeC, fillC, dashgap) );
   }

   
   /**
    * Create a <code>BasicPainterStyle</code> with the specified graphic attributes.
    * 
    * @param family The name of the desired font family.
    * @param generic A generic substitute font, in the event that the font family specified is not available.
    * @param fontStyle Desired font style.
    * @param fontSize Desired font size in logical units.
    * @param strokeW The desired stroke width in logical units.
    * @param cap How endpoints of unclosed paths and dash segments are decorated. If <code>null</code>, no decoration
    * is applied ("butt" endcaps).
    * @param join How the intersections of path segments are joined or how a path is closed. If <code>null</code>,
    * a mitered join is assumed.
    * @param dashgap An array specifying the lengths of alternating dashes and gaps in the stroke dash pattern. Lengths 
    * are specified in logical units. A <code>null</code> or empty array represents a solid stroke.
    * @param strokeC Desired stroke color as a packed RGB integer (R in byte 2, G in byte 1, B in byte 0).
    * @param fillC Desired fill color as a packed RGB integer.
    * 
    * @return A <code>BasicPainterStyle</code>, as described.
    */
   public static BasicPainterStyle createBasicPainterStyle(String family, GenericFont generic, FontStyle fontStyle, 
         float fontSize, float strokeW, StrokeCap cap, StrokeJoin join, float[] dashgap, int strokeC, int fillC)
   {
      return(new BasicPainterStyle(family, generic, fontStyle, fontSize, strokeW, cap, join, dashgap, strokeC, fillC));
   }

   private BasicPainterStyle(String family, GenericFont generic, FontStyle fontStyle, float fontSize, float strokeW, 
         StrokeCap cap, StrokeJoin join, float[] dashgap, int strokeC, int fillC)
   {
      this.font = BasicPainterStyle.getFontForPainter(family, generic, fontStyle, fontSize);
      this.strokeWidth = strokeW;
      this.strokeCap = (cap == null) ? StrokeCap.BUTT : cap;
      this.strokeJoin = (join == null) ? StrokeJoin.MITER : join;
      this.strokeColor = new Color((strokeC>>16) & 0x00ff, (strokeC>>8) & 0x00ff, strokeC & 0x00ff);
      this.fillColor = new Color((fillC>>16) & 0x00ff, (fillC>>8) & 0x00ff, fillC & 0x00ff);
      this.strokePattern = (dashgap==null) ? new float[0] : dashgap.clone();
   }

   private BasicPainterStyle(Font font, float strokeW, StrokeCap cap, StrokeJoin join, float[] dashgap, 
         Color strokeC, Color fillC)
   {
      this.font = font != null ? font: BasicPainterStyle.getFontForPainter("Arial", null, null, 10f);
      this.strokeWidth = strokeW;
      this.strokeCap = (cap == null) ? StrokeCap.BUTT : cap;
      this.strokeJoin = (join == null) ? StrokeJoin.MITER : join;
      this.strokePattern = (dashgap==null) ? new float[0] : dashgap.clone();
      this.strokeColor = strokeC != null ? strokeC : Color.BLACK;
      this.fillColor = (fillC != null) ? fillC : Color.WHITE;
   }

   private BasicPainterStyle(Font font, double strokeW, Color strokeC, Color fillC, float[] dashgap)
   {
      this.font = font!=null ? font : BasicPainterStyle.getFontForPainter("Arial", null, null, 10f);
      this.strokeWidth = strokeW;
      this.strokeCap = StrokeCap.BUTT;
      this.strokeJoin = StrokeJoin.MITER;
      this.strokeColor = (strokeC != null) ? strokeC : Color.BLACK;
      this.fillColor = (fillC != null) ? fillC : Color.WHITE;
      this.strokePattern = (dashgap==null) ? new float[0] : dashgap.clone();
   }

   public double getFontSize()
   {
      return(font.getSize2D());
   }

   public double getStrokeWidth()
   {
      return( strokeWidth );
   }

   public boolean isStrokeSolid()
   {
      return( strokePattern.length < 2 );
   }

   public boolean isStroked() { return(strokeWidth > 0 && strokeColor.getAlpha() != 0); }
   
   public Color getStrokeColor()
   {
      return(strokeColor);
   }

   public Color getFillColor()
   {
      return(fillColor);
   }

   public Stroke getStroke(float dashPhase)
   {
      Stroke s;
      if( strokePattern.length > 0 )
      {
         s = new BasicStroke((float)strokeWidth, strokeCap.getJava2DLineCap(), strokeJoin.getJava2DLineJoin(), 10f, 
                  strokePattern, dashPhase);
      }
      else
         s = new BasicStroke((float)strokeWidth, strokeCap.getJava2DLineCap(), strokeJoin.getJava2DLineJoin());
      return(s);
   }

   public Font getFont()
   {
      return(font);
   }

   /**
    * Get the <code>java.awt.Font</code> installed on the host platform that best matches the font properties given.
    * 
    * @param family The font family name.
    * @param generic A generic substitute font, if the specified font family is not installed on host.
    * @param style The font style.
    * @param size The font size in logical units.
    * @return A <code>Font</code> object.
    */
   public static Font getFontForPainter(String family, GenericFont generic, FontStyle style, double size)
   {
      // get installed font based on family name and font style, if it exists
      FontStyle fontStyle = (style==null) ? FontStyle.PLAIN : style;
      String facename = LocalFontEnvironment.getInstalledVariant(family, fontStyle.isBold(), fontStyle.isItalic());

      // if it does not, then we have to use the generic font
      if(facename==null)
      { 
         GenericFont substFont = (generic==null) ? GenericFont.SANSERIF : generic;
         if(substFont == GenericFont.MONOSPACE)
            facename = LocalFontEnvironment.getMonospaceFont();
         else if(substFont == GenericFont.SERIF)
            facename = LocalFontEnvironment.getSerifFont();
         else
            facename = LocalFontEnvironment.getSansSerifFont();
      }

      Font f = new Font(facename, Font.PLAIN, 10);
      return( f.deriveFont((float)size) );
   }

}
