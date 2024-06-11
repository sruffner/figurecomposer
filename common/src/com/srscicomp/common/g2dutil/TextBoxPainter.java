package com.srscicomp.common.g2dutil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.LineMetrics;
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

import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.common.util.Utilities;

/**
 * <code>TextBoxPainter</code> paints a text string within a rectangular box to the extent possible, optionally clipping
 * any portion of the text that lies outside the box. The bounding box is optionally stroked IAW the painter's style, 
 * and its background is optionally filled with a uniform RGB color, linear two-color gradient, or radial two-color
 * gradient (see {@link BkgFill}). All the characters in the text string are rendered in the same font and color. The 
 * painter does not support internationalization, and is really only suited for text that is oriented left-to-right, 
 * top-to-bottom. 
 * 
 * <p>The full bounding box is identified by its bottom-left corner (X,Y), width, and height. It is this box which is 
 * stroked and filled IAW the painter's properties, as described above. The <i>text box</i> is the bounding box less a
 * margin, which is the same on all sides; if the margin is zero, then the bounding box and text box are one and the
 * same. The text box width is always respected when calculating the line breaks in the text string --  even if it is 
 * necessary to break a single word across two or more consecutive lines. If the box is not tall enough to accommodate 
 * the laid-out text, it will continue beyond the top and/or bottom edges of the box (depending on the vertical 
 * alignment) as needed -- unless the "clip" option is set.</p>
 * 
 * <p>Any line breaks in the original text string are preserved during text layout. The spacing between text lines can
 * be adjusting by changing the painter's "line height" parameter; the line height is specified as a fraction of the 
 * font size, and is range-restricted between 0.5 and 3.0.</p>
 * 
 * <p>Horizontal and vertical alignment properties also affect the layout of the boxed text. The lines of text can be
 * aligned along the left or right edge of the text box, or each line can be centered within it. Vertically, the text 
 * can start at the top of the text box, end at the bottom, or be centered within the box.</p>
 * 
 * <p>The text font and color are determined by the {@link PainterStyle} object installed in the painter; there is no 
 * support for outlining or underlining the text.</p>
 * 
 * <p><code>TextBoxPainter</code> was originally designed to render the <i>FypML</i> "textbox" element. Unlike the more
 * generic {@link StringPainter} -- which can draw different labels at different locations in one render cycle --, it 
 * does not support drawing multiple text boxes. If you pass a location producer to the painter, it will only use the 
 * first location provided by that producer.</p>
 * 
 * <p>Implementation considerations:
 * <ul>
 *    <li>Font metrics obtained from the <code>Graphics2D</code> context are used to calculate adjustments needed to 
 *    to layout the text properly. The implementation relies on {@link LineBreakMeasurer} to calculate
 *    the line breaks in the supplied text string.</li>
 *    <li>If the text box width is less than twice the average character advance in the supplied text, then the painter
 *    will render nothing. It will also render nothing if the clip option is set AND the text box height is less than 
 *    the font size.</li>
 *    <li><strong><em>Important</em></strong>. All <code>Painter</code> implementations are designed with the assumption 
 *    that the painting coordinate system is right-handed (x-axis increasing rightward, y-axis increasing upward). But 
 *    the Java2D text-rendering routines fundamentally assume a left-handed coordinate system. To compensate, 
 *    <code>TextBoxPainter</code> will temporarily flip the y-axis while rendering text.</li>
 * </ul>
 * </p>
 * 
 * @author sruffner
 */
public class TextBoxPainter extends Painter
{
   /**
    * Construct an empty, default text block painter. The painter's text string is empty, its bounding box is undefined,
    * and its painter style attributes are set to default values. The painter renders nothing at all in this state.
    * Use the appropriate <i>set</i> methods to specify the painter's key properties.
    */
   public TextBoxPainter() {}
   
   /**
    * Set the attributed text string to be rendered by this text box painter.
    * @param as The attributed string. If null or contains no characters, the painter renders nothing.
    */
   public void setText(AttributedString as)
   {
      if(as != null)
      {
         AttributedCharacterIterator iter = as.getIterator();
         if(iter.getEndIndex() - iter.getBeginIndex() < 1) as = null;
      }
      attrText = as;
      isPlainText = false;
   }
   
   /**
    * Set the unattributed string rendered by this text block painter.
    * @param s An unattributed string. Leading and trailing whitespace is removed. If null, painter renders nothing.
    */
   public void setText(String s)
   {
      String text = (s == null) ? "" : s.trim();
      AttributedString aStr = (text == null || text.isEmpty()) ? null : new AttributedString(text);
      setText(aStr);
      isPlainText = (aStr != null);
   }
   
   /**
    * Set the location and dimensions of the bounding box within which the painter should render the text block. The
    * location coordinates and dimensions should be expressed in the logical coordinate system associated with the 
    * graphics context passed to the {@link #render} method. If the location is null, or if any coordinate or
    * dimension is infinite or NaN, then the painter will render nothing. If clip option is set and the bounding box 
    * height is less than the painter's font size, or if the bounding box width is less than two character advances in 
    * the painter's font, then the painter will again render nothing.
    * 
    * @param loc Location of the <b>bottom-left corner</b> of the bounding box.
    * @param w The width of the bounding box.
    * @param h The height of the bounding box.
    * @param m The margin. If non-zero, then the rectangular area to which the text is restricted -- the "text box"
    * -- will be the bounding box minus this margin on all sides. A reasonable margin will be a small fraction of the
    * specified width; range will be restricted to [0..width/2].
    */
   public void setBoundingBox(Point2D loc, double w, double h, double m)
   {
      boolean ok = Utilities.isWellDefined(loc) && Utilities.isWellDefined(w) && Utilities.isWellDefined(h);
      if(ok)
      {
         location = new Point2D.Double(loc.getX(), loc.getY());
         List<Point2D> ptList = new ArrayList<Point2D>();
         ptList.add(location);
         super.setLocationProducer(ptList);
         
         width = w;
         height = h;
         
         margin =  Utilities.isWellDefined(m) ? Utilities.rangeRestrict(0, w/2, m) : 0;
      }
      else
      {
         location = null;
         super.setLocationProducer(null);
         width = height = margin = 0;
      }
   }

   /**
    * Set the location of the bottom-left corner of the bounding box within which painter renders the text block.
    * @param loc The bottom-left corner coordinates. If not well-defined, painter will render nothing.
    */
   public void setBoundingBoxLocation(Point2D loc)
   {
      if(Utilities.isWellDefined(loc))
      {
         location = new Point2D.Double(loc.getX(), loc.getY());
         List<Point2D> ptList = new ArrayList<Point2D>();
         ptList.add(location);
         super.setLocationProducer(ptList);
      }
      else
      {
         location = null;
         super.setLocationProducer(null);
      }
   }
   
   /**
    * Get the location of the bottom-left corner of the bounding box within which painter renders the text block.
    * @return The bottom-left corner. Will be (0,0) if location is undefined.
    */
   public Point2D getBoundingBoxLocation()
   {
      Point2D p = new Point2D.Double();
      if(location != null) p.setLocation(location);
      return(p);
   }
   /** 
    * Get the width of the bounding box within which painter renders the text block.
    * @return The bounding box width.
    */
   public double getBoundingBoxWidth() { return(width); }
   /** 
    * Get the height of the bounding box within which painter renders the text block.
    * @return The bounding box height.
    */
   public double getBoundingBoxHeight() { return(height); }
   
   /**
    * The text block painter does not support rendering multiple text blocks, each in a different bounding box. This
    * override replaces the producer specified with a list containing just the first location produced.
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
            ptList = new ArrayList<Point2D>();
            ptList.add(location);
         }
      }
      super.setLocationProducer(ptList);
   }

   /**
    * Set the rotation angle by which the entire text box is rotated about its bottom-left corner. The direction of 
    * rotation will depend upon the coordinate system of the graphics context in which the painter draws itself.
    * @param rot Rotation angle <b>in degrees</b>.
    */
   public void setRotation(double rot) { rotation = rot; }
   
   /**
    * Get the angle by which the entire text box is rotated about its bottom-left corner.
    * @return Rotation angle in degrees.
    */
   public double getRotation() { return(rotation); }
   
   /**
    * Set the horizontal and vertical alignment for the text block.
    * 
    * <p>For horizontal alignment, each line of the text block is aligned to the left edge of the box for {@link 
    * TextAlign#LEADING}, aligned to the right edge of the box for {@link TextAlign#TRAILING}, or centered horizontally
    * in the box for {@link TextAlign#CENTERED}. Vertical alignment determines the location of the entire text block
    * with respect to the bounding box. For {@link TextAlign#LEADING}, the first line of text aligns with the top edge
    * of the box. For {@link TextAlign#TRAILING}, the last line of text aligns with the bottom edge. For {@link 
    * TextAlign#CENTERED}, the text block is centered within the box.</p>
    *
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
    * Set the background fill for the bounding box to the solid color specified.
    * @param c The background color. Only opaque or fully transparent (alpha=0) colors permitted. If translucent or if
    * null, the bounding box background is not painted.
    */
   public void setBackgroundColor(Color c) 
   { 
      bkgFill = (c != null) ? BkgFill.createSolidFill(c) : null;
   }
   
   /**
    * Set the background fill for the bounding box.
    * @param fill The background fill. If null, the bounding box background is not painted.
    */
   public void setBackgroundFill(BkgFill fill)
   {
      bkgFill = fill;
   }
   
   /**
    * Set flag that determines whether or not the bounding box's border is stroked in accordance with the text box
    * painter's stroke properties. 
    * @param stroked True if border should be drawn, else false.
    */
   public void setBorderStroked(boolean stroked) { isBorderStroked = stroked; }
   
   /**
    * Set flag that determines whether or not the text block is clipped to the bounding box dimensions.
    * 
    * <p>Depending on the dimensions of the bounding box, the length of the text string to be rendered, the font
    * characteristics, and the text block's H and V alignment, the rendered text block could bleed over any of the
    * edges of the bounding box. By setting this flag, the client ensures that no marks will be made outside the
    * bounding box.</p>
    * 
    * @param clip True to clip the text block, false otherwise.
    */
   public void setClipped(boolean clip) { clipped = clip; }
   
   /**
    * Set the line height -- ie, the baseline-to-baseline spacing between consecutive text lines -- for the text block 
    * rendered by this painter.
    * 
    * @param lh Line height <i>specified as a fraction of the painter's font size</i>. Range-restricted to [0.8, 3.0].
    */
   public void setLineHeight(double lh)
   {
      if(Utilities.isWellDefined(lh)) lineHeight = Utilities.rangeRestrict(0.8, 3.0, lh);
   }
   
   /*    
    * NOTE: 5/1/2014 I've modified this code to use Graphics2D.drawString() to draw each line, rather than 
    * TextLayout.draw(). When exporting to PDF, the latter method draws glyph shapes, which is not OK if we want the
    * text to be editable.
    */
   @Override protected boolean paintInternal(Graphics2D g2d)
   {
      // if no location specified, we render nothing
      if(location == null) return(true);
      
      // translate to the bottom-left corner of the bounding box and rotate if necessary
      g2d.translate(location.getX(), location.getY());
      if(rotation != 0) g2d.rotate(Math.toRadians(rotation));

      // fill and stroke the bounding box IAW parameters
      boolean doStroke = isBorderStroked && style.isStroked();
      boolean doFill = (bkgFill != null) && !bkgFill.isTransparent();
      if(doFill || doStroke)
      {
         Rectangle2D r = new Rectangle2D.Double(0, 0, width, height);
         if(doFill)
         {
            g2d.setPaint(bkgFill.getPaintForFill((float)width, (float)height));
            g2d.fill(r);
         }
         if(doStroke)
         {
            g2d.setColor(style.getStrokeColor());
            g2d.setStroke(style.getStroke(0));
            g2d.draw(r);
         }
      }
      
      // check for obvious cases in which no text is rendered
      double textBoxW = width - 2*margin;
      double textBoxH = height - 2*margin;
      if(style.getFontSize() == 0 || attrText == null || textBoxW <= 0 || textBoxH <= 0)
         return(true);
      AttributedCharacterIterator iterC = attrText.getIterator();
      if(iterC.getEndIndex() - iterC.getBeginIndex() < 1)
         return(true);
      
      // set up attributes in graphic context that affect our text rendering. 
      Font font = style.getFont();
      g2d.setFont(font);
      g2d.setColor(style.getFillColor());

      // prepare the object that will break the text block into one or more lines
      if(isPlainText) attrText.addAttribute(TextAttribute.FONT, font);
      LineBreakMeasurer lineBreaker = new LineBreakMeasurer(iterC, fontRC);

      // because the same font characteristics apply to the entire block of text, we can get the ascent and descent
      // for a sample text line using the current font render context. We need there to measure the overall height of 
      // the text block, and to locate the baseline of the first text line WRT the top of the text block.
      LineMetrics lm = font.getLineMetrics("AHMPWSFgjpqy", fontRC);
      float ascent = lm.getAscent();
      float descent = lm.getDescent();
      
      // get line height in logical units
      float lineHt = (float) (lineHeight * style.getFontSize());
      
      // prepare a list of the text lines comprising the text block, respecting the text box width = bounding box width
      // less margins. Preserve line-feed characters in the original text string by inserting additional line breaks
      // as needed. At the same time, compute the average character width.
      double avgCharW = 0;
      List<TextLayout> lines = new ArrayList<TextLayout>();
      List<AttributedString> textFrags = new ArrayList<AttributedString>();
      while(lineBreaker.getPosition() < iterC.getEndIndex())
      {
         // if there are any line feed characters after the line just laid out, insert a blank line (null) for each one
         int pos = lineBreaker.getPosition();
         while(pos < iterC.getEndIndex() && iterC.setIndex(pos) == '\n')
         {
            lines.add(null);
            textFrags.add(null);
            ++pos;
            lineBreaker.setPosition(pos);
         }
         
         // if the next layout has a line feed character, limit it accordingly so that the line feed is accounted for.
         int next = lineBreaker.nextOffset((float) textBoxW);
         int limit = next;
         for(int i=lineBreaker.getPosition(); i<next; i++) if(iterC.setIndex(i) == '\n')
         {
            limit = i+1;
            break;
         }
         
         int oldPos = pos;
         TextLayout line = lineBreaker.nextLayout((float) textBoxW, limit, false);
         lines.add(line);
         pos = lineBreaker.getPosition();
         textFrags.add(new AttributedString(iterC, oldPos, pos));
         
         avgCharW = Math.max(avgCharW, line.getBounds().getWidth() / line.getCharacterCount());
      }
      if(lines.isEmpty()) return(true);

      // calculate text block height: (N-1)*lineHt + ascent of first line + descent of last line, where N is the total 
      // number of text lines in the block. Draw a picture to convince yourself! If box is not wide enough to display at
      // least two characters, render nothing. If text content is clipped and the box is shorter than the font size, 
      // then again render nothing.
      double blockHt = (lines.size() - 1) * lineHt + ascent + descent;
      if(textBoxW < avgCharW * 2 || (clipped && textBoxH < style.getFontSize())) return(true);
      
      // determine vertical coordinate of the top of the first text line. Remember that Y increases upward, and remember
      // that we've transformed coordinate system so that bottom-left corner is now at origin.
      double yPos = 0;
      switch(vAlign)
      {
      case LEADING : yPos += textBoxH + margin; break;
      case TRAILING: yPos += margin + blockHt; break;
      case CENTERED: yPos += margin + (textBoxH + blockHt)/2.0; break;
      }

      // drop down to the alphabetic baseline of the first line of text.
      yPos -= ascent;
      
      // set clip rect to text box if text block should be clipped
      if(clipped)
         g2d.clip(new Rectangle2D.Double(margin, margin, textBoxW, textBoxH));
            
      // draw the text lines from first (top) to last (bottom)
      double xPos = margin;
      for(int i=0; i<lines.size(); i++)
      {
         TextLayout line = lines.get(i);
         
         // if next line is a blank line, move to baseline of next line of text.
         if(line == null)
         {
            yPos -= lineHt;
            continue;
         }
         
         // compute horizontal coordinate adjustment from left edge of text box to achieve desired H alignment
         double dx = 0;
         if(hAlign == TextAlign.TRAILING) dx = textBoxW - line.getVisibleAdvance();
         else if(hAlign == TextAlign.CENTERED) dx = (textBoxW - line.getVisibleAdvance()) / 2.0;

         // draw text, flipping y-axis temporarily because we assume right-handed coord system, but text routines assume
         // left-handed system. Note that we have to negate the y-coordinate when we do this!
         g2d.scale(1,-1);
         g2d.drawString(textFrags.get(i).getIterator(), (float) (xPos + dx), (float) -yPos); 
         g2d.scale(1, -1);
         
         // move to baseline of next line of text.
         yPos -= lineHt;
      }

      return(true);
   }

   @Override protected void recalcBounds2D(Rectangle2D r)
   { 
      // start out with an empty rectangle
      r.setFrame(0,0,0,0);
      
      // if we don't yet have a font render context, we cannot compute anything
      if(fontRC == null) return;

      // if no location specified, we render nothing
      if(location == null) return;

      // if the bounding box is either stroked and filled, then include that rectangle. The rectangle is specified in
      // the transformed coordinate system after translating to the bottom-left corner and rotating.
      boolean doStroke = isBorderStroked && style.isStroked();
      boolean doFill = (bkgFill != null) && !bkgFill.isTransparent();
      if(doFill || doStroke)
      {
         double hsw = (doStroke ? style.getStrokeWidth() : 0) / 2.0;
         r.setFrame(-hsw, -hsw, width + hsw*2, height + hsw*2);
      }
      
      // check for obvious cases in which no text is rendered
      double textBoxW = width - 2*margin;
      double textBoxH = height - 2*margin;
      if(style.getFontSize() == 0 || attrText == null || textBoxW <= 0 || textBoxH <= 0) return;
      AttributedCharacterIterator iterC = attrText.getIterator();
      if(iterC.getEndIndex() - iterC.getBeginIndex() < 1) return;
      
      // prepare the object that will break the text block into one or more lines
      Font font = style.getFont();
      if(isPlainText) attrText.addAttribute(TextAttribute.FONT, font);
      LineBreakMeasurer lineBreaker = new LineBreakMeasurer(iterC, fontRC);
      
      // because the same font characteristics apply to the entire block of text, we can get the ascent and descent
      // for a sample text line using the current font render context. We need there to measure the overall height of 
      // the text block, and to locate the baseline of the first text line WRT the top of the text block.
      LineMetrics lm = font.getLineMetrics("AHMPWSFgjpqy", fontRC);
      float ascent = lm.getAscent();
      float descent = lm.getDescent();
      
      // get line height in logical units
      float lineHt = (float) (lineHeight * style.getFontSize());
      
      // prepare a list of the text lines comprising the text block, respecting the text box width = bounding box width
      // less margins. Preserve line-feed characters in the original text string by inserting additional line breaks
      // as needed. At the same time, compute the average character width and the advance of the longest line.
      double avgCharW = 0;
      double maxAdvance = 0;
      List<TextLayout> lines = new ArrayList<TextLayout>();
      while(lineBreaker.getPosition() < iterC.getEndIndex())
      {
         // if there are any line feed characters after the line just laid out, insert a blank line (null) for each one
         int pos = lineBreaker.getPosition();
         while(pos < iterC.getEndIndex() && iterC.setIndex(pos) == '\n')
         {
            lines.add(null);
            ++pos;
            lineBreaker.setPosition(pos);
         }
         
         // if the next layout has a line feed character, limit it accordingly so that the line feed is accounted for.
         int next = lineBreaker.nextOffset((float) textBoxW);
         int limit = next;
         for(int i=lineBreaker.getPosition(); i<next; i++) if(iterC.setIndex(i) == '\n')
         {
            limit = i+1;
            break;
         }
         
         TextLayout line = lineBreaker.nextLayout((float) textBoxW, limit, false);
         lines.add(line);
         
         double adv = line.getVisibleAdvance();
         maxAdvance = Math.max(maxAdvance, adv);
         avgCharW = Math.max(avgCharW, adv/line.getCharacterCount());
      }
      if(lines.isEmpty()) return;

      // calculate text block height: (N-1)*lineHt + ascent of first line + descent of last line, where N is the total 
      // number of text lines in the block. Draw a picture to convince yourself! If box is not wide enough to display at
      // least two characters, render nothing. If text content is clipped and the box is shorter than the font size, 
      // then again render nothing.
      double blockHt = (lines.size() - 1) * lineHt + ascent + descent;
      if(textBoxW < avgCharW * 2 || (clipped && textBoxH < style.getFontSize())) return;
      
      // now compute the bounds of the text block in a right-handed coord sys (Y incr upward, origin at BL).
      // Block width is the width of the longest text line. Location depends on H and V alignment.
      double xPos = margin;
      if(hAlign == TextAlign.TRAILING) xPos += textBoxW - maxAdvance;
      else if(hAlign == TextAlign.CENTERED) xPos += (textBoxW - maxAdvance) / 2.0;
      
      double yPos = margin;
      if(vAlign == TextAlign.LEADING) yPos += textBoxH - blockHt;
      else if(vAlign == TextAlign.CENTERED) yPos += (textBoxH - blockHt)/2.0;
      
      Rectangle2D rText = new Rectangle2D.Double(xPos, yPos, maxAdvance, blockHt);
      if(clipped)
      {
         Rectangle2D rTmp = new Rectangle2D.Double(margin, margin, textBoxW, textBoxH);
         Rectangle2D.intersect(rTmp, rText, rText);
      }
      
      // add the text rectangle to what we have so far (if anything)
      if(r.isEmpty()) r.setFrame(rText);
      else Rectangle2D.union(rText, r, r);

      // now transform the rectangular bounds to the painting coordinate system in which the text box is rendered. We
      // have to account for the translation to the BL corner of the bounding box, and any rotation about that point.
      // This gives us a non-rectangular shape whenever the rotation is non-zero.
      AffineTransform txf = new AffineTransform();
      txf.translate(location.getX(), location.getY());
      if(rotation != 0) txf.rotate(Math.toRadians(rotation));
      
      r.setRect(txf.createTransformedShape(r).getBounds2D());
   }
   
   /**
    * Layout the text content as it will be rendered in the text box, IAW the current painter state.
    * 
    * @param g2d A 2D graphics context. The method will not make any marks in the context, but it will change its state.
    * @param aStrFrags (out) This list will contain the text fragment for each line in the text block. Will be empty if
    * no text content is rendered (or unable to compute the text lines for whatever reason).
    * @param coords (out) List of coordinates: the starting position (x, y), in logical units, for each text line.
    * @return True if successful; False if unable to compute text lines or text box is too narrow to render any text.
    */
   public boolean layoutTextInBox(Graphics2D g2d, List<AttributedString> aStrFrags, List<Point2D> coords)
   {
      // if no location specified, or output lists not provided, fail.
      if(location == null || g2d == null || aStrFrags == null || coords == null) return(false);
      aStrFrags.clear();
      coords.clear();
      
      // translate to the bottom-left corner of the bounding box and rotate if necessary
      g2d.translate(location.getX(), location.getY());
      if(rotation != 0) g2d.rotate(Math.toRadians(rotation));
      
      // check for obvious cases in which no text is rendered
      double textBoxW = width - 2*margin;
      double textBoxH = height - 2*margin;
      if(style.getFontSize() == 0 || attrText == null || textBoxW <= 0 || textBoxH <= 0)
         return(true);
      AttributedCharacterIterator iterC = attrText.getIterator();
      if(iterC.getEndIndex() - iterC.getBeginIndex() < 1)
         return(true);
      
      // set up attributes in graphic context that affect our text rendering. 
      Font font = style.getFont();
      g2d.setFont(font);
      g2d.setColor(style.getFillColor());
      fontRC = g2d.getFontRenderContext();
      
      // prepare the object that will break the text block into one or more lines
      if(isPlainText) attrText.addAttribute(TextAttribute.FONT, font);
      LineBreakMeasurer lineBreaker = new LineBreakMeasurer(iterC, fontRC);

      // because the same font characteristics apply to the entire block of text, we can get the ascent and descent
      // for a sample text line using the current font render context. We need there to measure the overall height of 
      // the text block, and to locate the baseline of the first text line WRT the top of the text block.
      LineMetrics lm = font.getLineMetrics("AHMPWSFgjpqy", fontRC);
      float ascent = lm.getAscent();
      float descent = lm.getDescent();
      
      // get line height in logical units
      float lineHt = (float) (lineHeight * style.getFontSize());
      
      // prepare a list of the text lines comprising the text block, respecting the text box width = bounding box width
      // less margins. Preserve line-feed characters in the original text string by inserting additional line breaks
      // as needed. At the same time, compute the average character width.
      double avgCharW = 0;
      List<AttributedString> textFrags = new ArrayList<AttributedString>();
      List<TextLayout> lines = new ArrayList<TextLayout>();
      while(lineBreaker.getPosition() < iterC.getEndIndex())
      {
         // if there are any line feed characters after the line just laid out, insert a blank line (null) for each one
         int pos = lineBreaker.getPosition();
         while(pos < iterC.getEndIndex() && iterC.setIndex(pos) == '\n')
         {
            lines.add(null);
            textFrags.add(null);
            ++pos;
            lineBreaker.setPosition(pos);
         }
         
         // if the next layout has a line feed character, limit it accordingly so that the line feed is accounted for.
         int next = lineBreaker.nextOffset((float) textBoxW);
         int limit = next;
         for(int i=lineBreaker.getPosition(); i<next; i++) if(iterC.setIndex(i) == '\n')
         {
            limit = i+1;
            break;
         }
         
         int oldPos = pos;
         TextLayout line = lineBreaker.nextLayout((float) textBoxW, limit, false);
         lines.add(line);
         pos = lineBreaker.getPosition();
         textFrags.add(new AttributedString(iterC, oldPos, pos));
         
         avgCharW = Math.max(avgCharW, line.getBounds().getWidth() / line.getCharacterCount());
      }
      if(lines.isEmpty()) return(true);

      // calculate text block height: (N-1)*lineHt + ascent of first line + descent of last line, where N is the total 
      // number of text lines in the block. Draw a picture to convince yourself! If box is not wide enough to display at
      // least two characters, render nothing. If text content is clipped and the box is shorter than the font size, 
      // then again render nothing.
      double blockHt = (lines.size() - 1) * lineHt + ascent + descent;
      if(textBoxW < avgCharW * 2 || (clipped && textBoxH < style.getFontSize())) return(false);
      
      // determine vertical coordinate of the top of the first text line. Remember that Y increases upward, and remember
      // that we've transformed coordinate system so that bottom-left corner is now at origin.
      double yPos = 0;
      switch(vAlign)
      {
      case LEADING : yPos += textBoxH + margin; break;
      case TRAILING: yPos += margin + blockHt; break;
      case CENTERED: yPos += margin + (textBoxH + blockHt)/2.0; break;
      }

      // drop down to the alphabetic baseline of the first line of text.
      yPos -= ascent;
      
      // set clip rect to text box if text block should be clipped
      // prepare the text lines from first (top) to last (bottom)
      double xPos = margin;
      for(int i=0; i<lines.size(); i++)
      {
         TextLayout line = lines.get(i);
         if(line != null)   // for a blank line, there's no string to render
         {
            // compute horizontal coordinate adjustment from left edge of text box to achieve desired H alignment
            double dx = 0;
            if(hAlign == TextAlign.TRAILING) dx = textBoxW - line.getVisibleAdvance();
            else if(hAlign == TextAlign.CENTERED) dx = (textBoxW - line.getVisibleAdvance()) / 2.0;
   
            // save the text fragment and its starting location
            aStrFrags.add(textFrags.get(i));
            coords.add(new Point2D.Double(xPos + dx, yPos));
         }
         
         // move to baseline of next line of text.
         yPos -= lineHt;
      }

      return(true);
      
   }

   /** The attributed text string rendered as a block within a specified bounding box. */
   private AttributedString attrText = null;
   
   /** Flag set if original text source for the painter was a plain, unattributed text string. */
   private boolean isPlainText = false;
   
   /** The bottom-left corner of the bounding box within which the text block is rendered. */
   private Point2D location = null;

   /** The width of the bounding box within which the text block is rendered. */
   private double width = 0;
   
   /** The height of the bounding box within which the text block is rendered. */
   private double height = 0;
   
   /** The margin: the text box in which text block is fitted is the bounding box minus the margin on all sides. */
   private double margin = 0;
   
   /**
    * Rotation angle in degrees. The text box is rotated about its center point by this amount.
    */
   private double rotation = 0;

   /** Horizontal alignment of the text block with respect to the bounding box. */
   private TextAlign hAlign = TextAlign.LEADING;

   /** Vertical alignment of the text block with respect to the bounding box. */
   private TextAlign vAlign = TextAlign.CENTERED;
   
   /** Bounding box background fill. If null, the bounding box is not filled. */
   private BkgFill bkgFill = null;
   
   /** If true, bounding box border is stroked IAW the painter's stroke styling (default = true). */
   private boolean isBorderStroked = true;
   
   /** 
    * If true, any portion of the text block lying outside the bounding box is not rendered (by clipping to the
    * bounding box dimensions).
    */
   private boolean clipped = false;
   
   /** 
    * The line height (baseline-to-baseline spacing between consecutive text lines of text) as a fraction of the font 
    * size. Default = 1.2, range = [0.8, 3.0].
    */
   private double lineHeight = 1.2;
}
