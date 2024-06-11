package com.srscicomp.common.g2dutil;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.srscicomp.common.util.Utilities;

/**
 * An implementation of {@link Painter} which renders a single text string at a single location. In most respects, the 
 * implementation is identical to that of {@link StringPainter}, except: (1) there's only a single text string and 
 * location; and (2) the text "source" can be either a {@link String String} or an {@link
 * AttributedString AttributedString}.
 * 
 * <p>[NOTE: This class was introduced primarily to support exporting FypML figures to PDF. During export, the AWT font
 * is matched to a physical font installed on the host system or built into the Java PDF library. If the text to be
 * painted contains any characters that the physical PDF font lacks, the text will be drawn incorrectly in the exported
 * PDF file. To resolve this issue, the problem characters are drawn in a substitute AWT font which, in turn, maps to a
 * physical PDF font that can handle those characters. An attributed string is required to encapsulate such font
 * substitutions.]</p>
 * 
 * @author sruffner
 */
public class SingleStringPainter extends Painter
{
   /**
    * Construct an empty attributed string painter with default styling, no text, and no location. As constructed, the
    * painter renders nothing.
    */
   public SingleStringPainter()
   {
      this(null, null, null);
   }
   
   /**
    * Construct an attributed string painter with the specified style, text, and text location. The text is not rotated,
    * and is left- and bottom-aligned WRT the specified location.
    * 
    * @param style Collection of graphic attributes applied to this painter. If null, default styles are applied.
    * @param text The attributes text string to be drawn. If null, the painter renders nothing.
    * @param loc The starting location for the text string. If null or not well-defined, the painter renders nothing.
    */
   public SingleStringPainter(PainterStyle style, AttributedString text, Point2D loc)
   {
      super(style, null);
      setTextAndLocation(text, loc);
   }
   
   /** The attributed string that is rendered by this painter.  */
   private AttributedString attrText = null;
   
   /** Flag set to indicate that original text source was an unattributed string. */
   private boolean isPlainText = false;
   
   /** The location at which the attributed string is drawn. */
   private Point2D location = null;

   /**
    * Get the location at which this single-string painter draws the text string.
    * @param p If non-null, this will contain the location coordinates upon return. Otherwise, a new point object is
    * allocated on the heap, set to the coordinates and returned.
    * @return The text location.
    */
   public Point2D getTextLocation(Point2D p)
   {
      if(p == null) p = new Point2D.Double();
      if(Utilities.isWellDefined(location)) p.setLocation(location);
      else p.setLocation(Double.NaN, Double.NaN);
      return(p);
   }
   
   /**
    * Set the location at which this single-string painter draws the current text string.
    * @param p Coordinates of the text starting location. If ill-defined, painter will render nothing.
    */
   public void setLocation(Point2D p)
   {
      location = null;
      List<Point2D> ptList = null;
      if(Utilities.isWellDefined(p))
      {
         location = (Point2D) p.clone();
         ptList = new ArrayList<>();
         ptList.add(location);
      }
      super.setLocationProducer(ptList);
   }
   
   /**
    * Set the location at which this single-string painter draws the current text string.
    * @param x X-coordinate of the text starting location.
    * @param y Y-coordinate of the text starting location.
    */
   public void setLocation(double x, double y)
   {
      List<Point2D> ptList = null;
      location = new Point2D.Double(x,y);
      if(Utilities.isWellDefined(location))
      {
         ptList = new ArrayList<>();
         ptList.add(location);
      }
      else location = null;
      super.setLocationProducer(ptList);
   }
   
   /**
    * Set the attributed text drawn by this painter, as well as the text's starting location.
    * @param text The attributed text string.
    * @param x X-coordinate of the text starting location.
    * @param y Y-coordinate of the text starting location.
    */
   public void setTextAndLocation(AttributedString text, double x, double y)
   {
      attrText = text;
      location = null;
      if(Utilities.isWellDefined(x) && Utilities.isWellDefined(y)) location = new Point2D.Double(x, y);
      
      List<Point2D> ptList = null;
      if(location != null)
      {
         ptList = new ArrayList<>();
         ptList.add(location);
      }
      super.setLocationProducer(ptList);
      isPlainText = false;
   }
   
   /**
    * Set the attributed text drawn by this painter, as well as the text's starting location.
    * @param text The attributed text string. If null or empty, the painter renders nothing.
    * @param loc The starting location of the text. If null or not well-defined, the painter renders nothing.
    */
   public void setTextAndLocation(AttributedString text, Point2D loc)
   {
      attrText = text;
      location = (!Utilities.isWellDefined(loc)) ? null : new Point2D.Double(loc.getX(), loc.getY());
      
      List<Point2D> ptList = null;
      if(location != null)
      {
         ptList = new ArrayList<>();
         ptList.add(location);
      }
      super.setLocationProducer(ptList);
      isPlainText = false;
   }
   
   /**
    * Set the plain, unattributed text string to be drawn by this painter, as well as the text's starting location.
    * @param plainText The unattributed text string. If null or empty, the painter renders nothing.
    * @param x X-coordinate of the text starting location.
    * @param y Y-coordinate of the text starting location.
    */
   public void setTextAndLocation(String plainText, double x, double y)
   {
      AttributedString at = (plainText == null || plainText.isEmpty()) ? null : new AttributedString(plainText);
      setTextAndLocation(at, x, y);
      isPlainText = (at != null);
   }
   
   /**
    * Set the plain, unattributed text string to be drawn by this painter, as well as the text's starting location.
    * @param plainText The unattributed text string. If null or empty, the painter renders nothing.
    * @param loc The starting location of the text. If null or not well-defined, the painter renders nothing.
    */
   public void setTextAndLocation(String plainText, Point2D loc)
   {
      AttributedString at = (plainText == null || plainText.isEmpty()) ? null : new AttributedString(plainText);
      setTextAndLocation(at, loc);
      isPlainText = (at != null);
   }
   
   /**
    * Set the attributed text drawn by this painter.
    * @param text The attributed text string. If null or empty, the painter renders nothing.
    */
   public void setText(AttributedString text)
   {
      attrText = text;
      isPlainText = false;
   }
   
   /**
    * Set the plain, unattributed text string to be drawn by this painter.
    * @param plainText The unattributed text string. If null or empty, the painter renders nothing.
    */
   public void setText(String plainText)
   {
      AttributedString at = (plainText == null || plainText.isEmpty()) ? null : new AttributedString(plainText);
      setText(at);
      isPlainText = (at != null);
   }
   
   /** Rotation angle in degrees. The text string is rotated about its starting position by this amount. */
   private double rotation = 0;

   /**
    * Set the rotation angle by which each attributed text string is rotated about its target location. The direction of
    * rotation will depend upon the coordinate system of the graphics context in which the painter draws itself.
    * @param rot Rotation angle in degrees.
    */
   public void setRotation(double rot) { this.rotation = rot; }

   /**
    * Get the angle by which each attributed text string is rotated about its target location.
    * @return Rotation angle in degrees.
    */
   public double getRotation() { return(rotation); }
   
   /** Horizontal alignment of the attributed text string. */
   private TextAlign hAlign = TextAlign.LEADING;

   /** Vertical alignment of the attributed text string. */
   private TextAlign vAlign = TextAlign.TRAILING;

   /**
    * Set the horizontal and vertical alignment for the attributed text string rendered by this painter.
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
    * The attributed string painter does not support rendering multiple text strings. This override replaces the 
    * producer specified with a list containing just the first location produced.
    */
   @Override public void setLocationProducer(Iterable<Point2D> producer)
   {
      location = null;
      List<Point2D> ptList = null;
      Iterator<Point2D> iter = (producer == null) ? null : producer.iterator();
      if(iter != null && iter.hasNext())
      {
         Point2D first = iter.next();
         if(Utilities.isWellDefined(first))
         {
            location = new Point2D.Double(first.getX(), first.getY());
            ptList = new ArrayList<>();
            ptList.add(location);
         }
      }
      super.setLocationProducer(ptList);
   }

   @Override protected boolean paintInternal(Graphics2D g2d)
   {
      // check for obvious cases in which nothing is rendered
      if(style.getFontSize() == 0 || attrText == null || location == null || style.getFillColor().getAlpha() == 0)
         return(true);
      AttributedCharacterIterator aci = attrText.getIterator();
      if(aci.getEndIndex() - aci.getBeginIndex() < 1)
         return(true);

      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         // set up attributes in graphic context that affect our text rendering.
         Font font = style.getFont();
         g2dCopy.setFont(font);
         g2dCopy.setColor(style.getFillColor());
         
         // translate to target location -- now the text starting position is (0,0)
         g2dCopy.translate(location.getX(), location.getY());
         
         // apply rotation
         if(rotation != 0) 
            g2dCopy.rotate(Math.toRadians(rotation));

         // flip y-axis temporarily, because we assume right-handed coord system, but text routines assume left-handed
         g2dCopy.scale(1,-1);

         // when the text source is an unattributed string, we must associate the painter's font with the entire string
         // for the rendering code to work correctly on the attributed string's character iterator!
         if(isPlainText) attrText.addAttribute(TextAttribute.FONT, font);
         aci = attrText.getIterator();

         // adjust starting position to achieve desired horizontal and vertical alignment of **visible** bounds of 
         // text run. We adjust the horizontal coordinate to account for the fact that the first character may not 
         // start exactly at the nominal start position. This is important for getting exact horizontal alignment of 
         // the rendered text. Note that the TextLayout bounds are in a left-handed coord sys (y-axis incr downward).
         TextLayout layout = new TextLayout(aci, fontRC);
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
            switch(vAlign)
            {
               case LEADING :    vAdj -= textBounds.getY(); break;
               case TRAILING :   break;
               case CENTERED :   vAdj -= (textBounds.getY() + textBounds.getHeight() / 2.0); break;
            }
         }

         // draw it!
         g2dCopy.drawString(aci, (float) hAdj, (float) vAdj);
      }
      finally { if(g2dCopy != null) g2dCopy.dispose(); }
      
      return(true);
   }

   @Override protected void recalcBounds2D(Rectangle2D r)
   {
      // start out with an empty rectangle
      r.setFrame(0,0,0,0);

      // if we don't yet have a font render context, we cannot compute anything
      if(fontRC == null) return;
      
      // check for obvious cases in which nothing is rendered
      if(style.getFontSize() == 0 || attrText == null || location == null || style.getFillColor().getAlpha() == 0)
         return;
      AttributedCharacterIterator aci = attrText.getIterator();
      if(aci.getEndIndex() - aci.getBeginIndex() < 1)
         return;
      
      // when the text source is an unattributed string, we must associate the painter's font with the entire string
      // for the rendering code to work correctly on the attributed string's character iterator!
      if(isPlainText) attrText.addAttribute(TextAttribute.FONT, style.getFont());

      // use a TextLayout to compute bounds of attributed text string. We convert the bounding rectangle from a left-
      // handed coordinate system (y-axis incr downward, defining corner of rectangle is UL) to a right-handed one 
      // (y-axis incr upward, defining corner is BL).
      TextLayout layout = new TextLayout(attrText.getIterator(), fontRC);
      Rectangle2D rText = layout.getBounds();
      double w = rText.getWidth();
      if(((double) layout.getAdvance()) > w) w = layout.getAdvance();
      double h = rText.getHeight();         
      rText.setRect(rText.getX(), -rText.getY()-h, w, h);

      // transform the visible bounds of text to painting coordinates
      AffineTransform txf = getAffineTransform(rText, w, h);
      r.setRect(txf.createTransformedShape(rText).getBounds2D());
   }

   /**
    * Prepare affine transform that transforms a bounding rectangle to its actual location AND orientation in this
    * painter's coordinate system. Translates to target location, applies rotation, and adjusts the starting position
    * to achieve desired horizontal and vertical alignment of the **visible bounds** of the text run. We adjust the
    * horizontal coordinate to account for the fact that the first character may not start exactly at the nominal start
    * position. This is important for getting exact horizontal alignment of the rendered text.
    *
    * @param rText Rectangle bounding a text run in text layout coordinates.
    * @param w Width of rectangle.
    * @param h Height of rectangle.
    * @return Affine transform to convert rectangle from text layout coordinate to a possibly rotated shape in this
    * painter's coordinate system.
    */
   private AffineTransform getAffineTransform(Rectangle2D rText, double w, double h) {
      AffineTransform txf = new AffineTransform();
      txf.translate(location.getX(), location.getY());
      if(rotation != 0) txf.rotate(Math.toRadians(rotation));

      double hAdj = -rText.getX();
      double vAdj = 0;
      switch(hAlign)
      {
         case LEADING :    break;
         case TRAILING :   hAdj -= w; break;
         case CENTERED :   hAdj += -w /2.0; break;
      }
      switch(vAlign)
      {
         case LEADING :    vAdj -= (rText.getY() + h); break;
         case TRAILING :   break;
         case CENTERED :   vAdj -= (rText.getY() + h / 2.0); break;
      }
      txf.translate(hAdj, vAdj);
      return txf;
   }
}
