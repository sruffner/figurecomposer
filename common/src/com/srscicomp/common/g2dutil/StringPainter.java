package com.srscicomp.common.g2dutil;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.srscicomp.common.util.Utilities;

/**
 * <code>StringPainter</code> paints text strings at one or more locations. It is intended only for short single-line 
 * text labels in which all the characters in the text string are rendered in the same font and color.  It does not 
 * support internationalization, and is really only suited for text that is oriented left-to-right, top-to-bottom.
 * 
 * <p>While <code>StringPainter</code> is most typically used to paint a single text label, it is possible to paint a 
 * series of labels (eg., the tick mark labels on an axis) in one go. Each string from its "label producer" is drawn 
 * at the corresponding location from its "location producer". If there are more labels than locations, the remaining 
 * labels are ignored. However, if there are more locations than labels, the last label is painted at all of the 
 * remaining locations.</p>
 * 
 * <p><code>StringPainter</code> supports horizontal (left, right, center) and vertical (top, bottom, middle) alignment 
 * of the text string with respect to the location, and the text may be rotated (about the location) with respect to 
 * the "painting coordinate system", ie, the coordinate system of the <code>Graphics2D</code> context in which it paints 
 * itself. The font and color are determined by the <code>PainterStyle</code> installed in the painter; there is no 
 * support for outlining or underlining the text.</p>
 * 
 * <p>Implementation considerations:
 * <ul>
 *    <li>Font metrics obtained from the <code>Graphics2D</code> context are used to calculate adjustments needed to 
 *    handle the different horizontal and vertical alignments. Given the string width in the current context, it is 
 *    easy to achieve centered and right-aligned text. Vertical alignment is tricky because it will depend upon how the 
 *    "height" of the rendered text is defined: the total font height, the distance from descender to ascender line, 
 *    the distance from baseline to ascender line, or perhaps the height of the true bounding box for the particular 
 *    text rendered (which would be very different for "acg" vs "ACG"!). <code>StringPainter</code> uses a general 
 *    rule-of-thumb that tends to work well with standard Latin fonts: the text is top-aligned by nudging it down from 
 *    the target location by 2/3 of the font height; it is centered vertically by moving it down half that amount.</li>
 *    <li>All text strings are rotated by the same angle and aligned in the same manner-- you cannot apply different 
 *    rotations and alignments to different strings in the painter's string list. Each string is always rotated about 
 *    its target location, and the alignment adjustments are applied after the rotation.</li>
 *    <li><strong><em>Important</em></strong>. All <code>Painter</code> implementations are designed with the assumption 
 *    that the painting coordinate system is right-handed (x-axis increasing rightward, y-axis increasing upward). But 
 *    the Java2D text-rendering routines fundamentally assume a left-handed coordinate system. To compensate, 
 *    <code>StringPainter</code> will temporarily flip the y-axis while rendering text.</li>
 *    <li>Reporting progress and cancelling paint job. <code>StringPainter</code> estimates render cost as the total 
 *    number of characters drawn. When rendering itself, it will report progress and check for cancellation after every 
 *    25 locations have been painted, and after rendering is finished.</li>
 *    <li><i>Special use case</i>. When multiple strings are to be painted, their locations all have the same Y
 *    coordinate, no rotation is applied, the <b>StringPainter</b> will render them along the same alphabetic baseline.
 *    That way, a line of strings like "WW", "gg", and "ee" will look better when the vertical alignment is not {@link
 *    TextAlign#TRAILING}.</li>
 * </ul>
 * </p>
 * 
 * @author sruffner
 */
public class StringPainter extends Painter
{
   /**
    * Construct an empty, default <code>StringPainter</code>. It is initialized with a zero rotation angle, left and 
    * bottom alignments, and standard default paint attributes. Since neither a string nor a location producer are 
    * specified, the painter constructed will not actually render anything.
    */
   public StringPainter()
   {
      this(null, null, null);
   }

   /**
    * Construct a <code>StringPainter</code> for rendering a single text label at a single location, with default text 
    * alignment (no rotation, left-edge horizontal alignment, bottom-edge vertical alignment) and standard default
    * paint attributes.
    * 
    * <p>The method creates internal "producers" that serve up the single string and the single location.</p>
    * 
    * @param s The text label. If <code>null</code> or empty string, nothing will be rendered.
    * @param loc Target location for the text label.  If <code>null</code>, nothing will be rendered.
    */
   public StringPainter(String s, Point2D loc)
   {
      this(null, null, null);
      setTextAndLocation(s, loc);
   }

   /**
    * Construct a <code>StringPainter</code> with default text alignment (no rotation, left-edge horizontal alignment, 
    * bottom-edge vertical alignment.
    * 
    * @param style Collection of graphic attributes applied to this <code>StringPainter</code>. If <code>null</code>, 
    * then default attributes are used.
    * @param locProducer The location producer, which provides the starting positions at which text strings are drawn.
    * If <code>null</code>, then this <code>StringPainter</code> renders nothing!
    * @param stringProducer The string producer, which provides the text strings to be painted. Each string produced is
    * drawn at the corresponding position drawn from the location producer. If there are more strings than locations, 
    * the remaining strings are ignored. However, if there are more locations than strings, the last string is painted 
    * at all of the remaining locations. If <code>null</code>, nothing will be rendered.
    */
   public StringPainter(PainterStyle style, Iterable<Point2D> locProducer, Iterable<String> stringProducer)
   {
      super(style, locProducer);
      this.stringProducer = stringProducer;
   }

   /**
    * Producer of the strings rendered by this <code>StringPainter</code>. 
    */
   private Iterable<String> stringProducer;

   /**
    * Set the string producer used by this <code>StringPainter</code>. 
    * 
    * <p>During rendering, each string provided by the string producer will be rendered at the corresponding location 
    * generated by the location producer. If there are more strings produced than locations, the remaining strings are 
    * ignored. However, if there are more locations than strings, the last string is painted at all of the remaining 
    * locations.</p>
    * 
    * @param stringProducer A producer of zero or more <code>String</code>s. If <code>null</code>, the painter renders 
    * nothing.
    */
   public void setStringProducer(Iterable<String> stringProducer)
   {
      this.stringProducer = stringProducer;
   }

   /**
    * Rotation angle in degrees.  Each text label is rotated about its starting position by this amount.
    */
   private double rotation = 0;

   /**
    * Set the rotation angle by which each text string in this <code>StringPainter</code> is rotated about its target 
    * location.  The direction of rotation will depend upon the coordinate system of the graphics context in which the 
    * painter draws itself.
    * 
    * @param rot Rotation angle in degrees.
    */
   public void setRotation(double rot)
   {
      this.rotation = rot;
   }

   /**
    * Horizontal alignment of each text string.
    */
   private TextAlign hAlign = TextAlign.LEADING;

   /**
    * Vertical alignment of each text string.
    */
   private TextAlign vAlign = TextAlign.TRAILING;

   /**
    * Set the horizontal and vertical alignment for all text strings rendered by this <code>StringPainter</code>.
    * 
    * @see TextAlign
    * @param ha Horizontal alignment.
    * @param va Vertical alignment.
    */
   public void setAlignment(TextAlign ha, TextAlign va)
   {
      hAlign = ha;
      vAlign = va;
   }

   /**
    * Get the current horizontal text alignment. 
    * @return The horizontal alignment.
    */
   public TextAlign getHorizontalAlignment() { return(hAlign); }
   /** 
    * Get the current vertical text alignment. 
    * @return The vertical alignment.
    */
   public TextAlign getVerticalAlignment() { return(vAlign); }
   
   /**
    * Configure this <code>StringPainter</code> to render a single text string at a single target location. The 
    * painter's current string producer is replaced by a private producer that generates the single string; similarly, 
    * the current location producer is replaced by a private producer that generates the single location.
    * 
    * @param s The text string to render. If it is <code>null</code> or empty, the painter's string producer is set to 
    * <code>null</code>, and the painter will render nothing.
    * @param p The target location.  If it is <code>null</code>, the painter's location producer is set to 
    * <code>null</code>, and the painter will render nothing.
    */
   public void setTextAndLocation(String s, Point2D p)
   {
      if(s == null || s.isEmpty()) stringProducer = null;
      else 
      {
         List<String> stringList = new ArrayList<>();
         stringList.add(s);
         stringProducer = stringList;
      }

      if(p == null) setLocationProducer(null);
      else
      {
         List<Point2D> ptList = new ArrayList<>();
         ptList.add( new Point2D.Double(p.getX(), p.getY()) );
         setLocationProducer(ptList);
      }
   }

   /**
    * Configure this string painter to render a single text string at a single target location. The painter's current 
    * string producer is replaced by a private producer that generates the single string; similarly, the current 
    * location producer is replaced by a private producer that generates the single location.
    * 
    * @param s The text string to render. If it is null or empty, the painter's string producer is set to null, and the
    * painter will render nothing.
    * @param x The X-coordinate of the target location. If infinite or NaN, the location producer is set to null.
    * @param y The Y-coordinate of the target location. If infinite or NaN, the location producer is set to null.
    */
   public void setTextAndLocation(String s, double x, double y)
   {
      if(s == null || s.isEmpty()) stringProducer = null;
      else 
      {
         List<String> stringList = new ArrayList<>();
         stringList.add(s);
         stringProducer = stringList;
      }

      if(!(Utilities.isWellDefined(x) && Utilities.isWellDefined(y))) setLocationProducer(null);
      else
      {
         List<Point2D> ptList = new ArrayList<>();
         ptList.add( new Point2D.Double(x, y) );
         setLocationProducer(ptList);
      }
   }

   //
   // Painting the text
   //


   /**
    * Interval at which <code>StringPainter</code> reports progress and checks for cancellation, in #locations actually 
    * painted.
    */
   private static final int PROGRESSINTV = 25;

   @Override
   protected boolean paintInternal(Graphics2D g2d)
   {
      // check for obvious cases in which nothing is rendered
      if(style.getFontSize() == 0 || stringProducer == null || locationProducer == null || 
            style.getFillColor().getAlpha() == 0)
         return(true);

      // under specific conditions, the same vertical adjustment is applied to all strings. Null if conditions not met.
      Double commonVAdj = calcCommonBaselineAdjustment();
      
      // set up attributes in graphic context that affect our text rendering. 
      Font font = style.getFont();
      g2d.setFont(font);
      g2d.setColor(style.getFillColor());

      // #locations actually painted -- for progress reporting
      int nLocsPainted = 0;

      // iterate over the string producer and the location producer simultaneously, drawing each non-empty string if 
      // it has a well-defined location. 
      Iterator<Point2D> locationIterator = locationProducer.iterator();
      Iterator<String> stringIterator = stringProducer.iterator();
      String lastString = null;
      while(locationIterator.hasNext())
      {
         // get the next (location, string) pair: If there are more strings than locations, the extra strings are 
         // ignored. If there are more locations and the last string is not empty, that string is repeated at all 
         // remaining well-defined locations.
         Point2D pLoc = locationIterator.next();
         if(stringIterator.hasNext())
            lastString = stringIterator.next();
         else if(lastString == null || lastString.isEmpty())
            break;

         // move on if there's no string to render or nowhere to render it to
         if(lastString == null || lastString.isEmpty() || !Utilities.isWellDefined(pLoc))
         {
            ++nLocsPainted;
            continue;
        }

         // translate to target location -- now the text starting position is (0,0)
         g2d.translate(pLoc.getX(), pLoc.getY());
         
         // apply rotation
         if(rotation != 0) 
            g2d.rotate(Math.toRadians(rotation));

         // flip y-axis temporarily, because we assume right-handed coord system, but text routines assume left-handed
         g2d.scale(1,-1);

         // adjust starting position to achieve desired horizontal and vertical alignment of **visible** bounds of 
         // text run. We adjust the horizontal coordinate to account for the fact that the first character may not 
         // start exactly at the nominal start position. This is important for getting exact horizontal alignment of 
         // the rendered text. Note that the TextLayout bounds are in a left-handed coord sys (y-axis incr downward).
         TextLayout layout = new TextLayout(lastString, font, fontRC);
         Rectangle2D textBounds = layout.getBounds();
         if(textBounds.getWidth() < layout.getAdvance()) 
            textBounds.setFrame(textBounds.getX(), textBounds.getY(), layout.getAdvance(), textBounds.getHeight());
         double hAdj = -textBounds.getX(); 
         double vAdj = 0;
         if(hAlign != TextAlign.LEADING || vAlign != TextAlign.TRAILING)
         {
            switch(hAlign)
            {
               case LEADING :    break;
               case TRAILING :   hAdj -= textBounds.getWidth(); break;
               case CENTERED :   hAdj += -textBounds.getWidth()/2.0; break;
            }
            
            if(commonVAdj != null)
            {
               // Under very specific conditions (no rotation, all string locations have the same Y coord, vertical 
               // alignment is top or middle), the same vertical adjustment is applied to  all strings to ensure they 
               // align on a common alphabetic baseline. Note: negative sign b/c we've flipped y-axis temporarily.
               vAdj = -commonVAdj;
            }
            else switch(vAlign)
            {
               case LEADING :
                   vAdj -= textBounds.getY(); break;
               case TRAILING :   break;
               case CENTERED :
                   vAdj -= (textBounds.getY() + textBounds.getHeight() / 2.0); break;
            }
         }

         // draw it!
         g2d.drawString(lastString, (float)hAdj, (float)vAdj);
         
         // undo previous transformations
         g2d.scale(1, -1);
         if(rotation != 0) g2d.rotate(-Math.toRadians(rotation));
         g2d.translate(-pLoc.getX(), -pLoc.getY());

         // accumulate work completed in drawing this string. Report progress and check for cancellation if it is time 
         // to do so.
         ++nLocsPainted;
         if(nLocsPainted >= PROGRESSINTV)
         {
            nLocsPainted = 0;
            if(stopPainting()) return(false);
         }
      }

      return(true);
   }

   @SuppressWarnings("ExtractMethodRecommender")
   @Override protected void recalcBounds2D(Rectangle2D r)
   {
      // start out with an empty rectangle
      r.setFrame(0, 0, 0, 0);

      // if we don't yet have a font render context, we cannot compute anything
      if(fontRC == null) return;

      // check for obvious cases in which nothing is rendered
      if(style.getFontSize() == 0 || stringProducer == null || locationProducer == null ||
            style.getFillColor().getAlpha() == 0)
         return;

      // under specific conditions, the same vertical adjustment is applied to all strings. Null if conditions not met.
      Double commonVAdj = calcCommonBaselineAdjustment();
      
      // iterate over the string producer and the location producer simultaneously: For each non-empty string with a 
      // well-defined target location, find the smallest rectangle in the painting coordinate system that bounds any 
      // marks made by the text. Then combine it with the rectangular bounds computed thus far...
      Iterator<Point2D> locationIterator = locationProducer.iterator();
      Iterator<String> stringIterator = stringProducer.iterator();
      String lastString = null;
      Font f = style.getFont();
      while(locationIterator.hasNext())
      {
         // get the next (location, string) pair: If there are more strings than locations, the extra strings are 
         // ignored. If there are more locations and the last string is not empty, that string is repeated at all 
         // remaining well-defined locations.
         Point2D pLoc = locationIterator.next();
         if(stringIterator.hasNext())
            lastString = stringIterator.next();
         else if(lastString == null || lastString.isEmpty())
            break;

         // move on if there's no string to render or nowhere to render it to
         if(lastString == null || lastString.isEmpty() || !Utilities.isWellDefined(pLoc))
            continue;

         // use a TextLayout to compute string bounds. We convert the bounding rectangle from a left-handed coordinate 
         // system (y-axis incr downward, defining corner of rectangle is UL) to a right-handed one (y-axis incr upward, 
         // defining corner is BL).
         TextLayout layout = new TextLayout(lastString, f, fontRC);
         Rectangle2D rText = layout.getBounds();
         double w = rText.getWidth();
         if(((double) layout.getAdvance()) > w) w = layout.getAdvance();
         double h = rText.getHeight();         
         rText.setRect(rText.getX(), -rText.getY()-h, w, h);

         // transform this rectangle to its actual location and orientation in the painting coordinate system:
         // translate to the target location, rotate, and then adjust starting position to achieve desired horizontal 
         // and vertical alignment of **visible** bounds of text run. We adjust the horizontal coordinate to account 
         // for the fact that the first character may not start exactly at the nominal start position. This is important 
         // for getting exact horizontal alignment of the rendered text. Under very specific conditions (no rotation,
         // all string locations have the same Y coord, vertical alignment is top or middle), the same vertical 
         // adjustment is applied to  all strings to ensure they align on a common alphabetic baseline.
         AffineTransform txf = new AffineTransform();
         txf.translate(pLoc.getX(), pLoc.getY());
         if(rotation != 0) txf.rotate(Math.toRadians(rotation));

         double hAdj = -rText.getX(); 
         double vAdj = 0;
         switch(hAlign)
         {
            case LEADING :    break;
            case TRAILING :   hAdj -= w; break;
            case CENTERED :   hAdj += -w/2.0; break;
         }
         if(commonVAdj != null)
            vAdj = commonVAdj;
         else switch(vAlign)
         {
            case LEADING :    vAdj -= (rText.getY() + h); break;
            case TRAILING :   break;
            case CENTERED :   vAdj -= (rText.getY() + h / 2.0); break;
         }
         txf.translate(hAdj, vAdj);

         // after transforming the visible bounds of text to painting coordinates, combine it with bounds thus far
         rText = txf.createTransformedShape(rText).getBounds2D();
         if(r.isEmpty())
            r.setRect(rText);
         else
            Rectangle2D.union(r, rText, r);
      }
   }
   
   /**
    * Helper method analyzes the strings to be drawn and their locations. When the following conditions are met, the
    * method calculates the common vertical adjustment to be applied to all strings so that they share the same
    * alphabetic baseline.
    * <ul>
    * <li>The rotation is zero.</li>
    * <li>The vertical text alignment is NOT {@link TextAlign#TRAILING}. In that case, the vertical adjustment is 
    * always zero anyway.</li>
    * <li>There is more than one string to draw, and their locations have the same Y coordinate.</li>
    * </ul>
    * @return The common vertical adjustment, or null if the conditions above are not met. <b>NOTE that the adjustment
    * is computed WRT a coordinate system in which the vertical coordinate increases in the upward direction, not the
    * downward one. When painting text, the vertical coordinate increases in the downward direction.
    */
   private Double calcCommonBaselineAdjustment()
   {
      if(rotation != 0 || vAlign == TextAlign.TRAILING) return(null);
      
      // iterate over the strings and their locations. Count the number of strings to render and check whether or not
      // they all share a common Y coordinate. Concatenate the strings into a single one. If check fails, or less than 
      // 2 strings drawn, stop.
      Iterator<Point2D> locationIterator = locationProducer.iterator();
      Iterator<String> stringIterator = stringProducer.iterator();
      StringBuilder sb = new StringBuilder();
      String lastString = null;
      double yCoord = Double.NaN;
      boolean ok = true;
      int n = 0;
      Font f = style.getFont();
      while(ok && locationIterator.hasNext())
      {
         // get the next (location, string) pair: If there are more strings than locations, the extra strings are 
         // ignored. If there are more locations and the last string is not empty, that string is repeated at all 
         // remaining well-defined locations.
         Point2D pLoc = locationIterator.next();
         if(stringIterator.hasNext())
            lastString = stringIterator.next();
         else if(lastString == null || lastString.isEmpty())
            break;

         // move on if there's no string to render or nowhere to render it to
         if(lastString == null || lastString.isEmpty() || !Utilities.isWellDefined(pLoc))
            continue;

         if(!Utilities.isWellDefined(yCoord)) yCoord = pLoc.getY();
         else ok = (yCoord == pLoc.getY());
         
         if(ok)
         {
            ++n;
            sb.append(lastString);
         }
      }
      ok = ok && (n > 1) && sb.length() > 0;
      if(!ok) return(null);
      
      // common baseline desired. Use a TextLayout to compute bounds of the concatenated version of all strings drawn. 
      // Convert the bounding rectangle from a left-handed coordinate system (y-axis incr downward, defining corner 
      // of rectangle is UL) to a right-handed one (y-axis incr upward, defining corner is BL). Use the rectangle to
      // calculate the vertical adjustment that will be applied to all strings separately at render time.
      TextLayout layout = new TextLayout(sb.toString(), f, fontRC);
      Rectangle2D rText = layout.getBounds();      
      double w = rText.getWidth();
      if(((double) layout.getAdvance()) > w) w = layout.getAdvance();
      double h = rText.getHeight();         
      rText.setRect(rText.getX(), -rText.getY()-h, w, h);

      double vAdj = 0;
      switch(vAlign)
      {
         case LEADING :    vAdj -= (rText.getY() + h); break;
         case TRAILING :   break;
         case CENTERED :   vAdj -= (rText.getY() + h / 2.0); break;
      }      

      return(vAdj != 0 ? vAdj : null);
   }
}
