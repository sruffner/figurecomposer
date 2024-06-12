package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dutil.TextBoxPainter;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.common.util.Utilities;

/**
 * <code>TextBoxNode</code> represents a multi-line relocatable text box in the <em>DataNav</em> graphic model. The
 * text is broken into one or more lines depending on the dimensions of the node's rectangular bounding box. The node
 * includes properties for specifying the bottom-left corner, dimensions and orientation of the bounding box, a margin 
 * between the bounding box and the text block itself, and a background fill (supporting uniform color fill, linear
 * gradient at any orientation, or radial gradient). All characters in the text block are rendered in the same font and 
 * text color, and the block can be aligned both horizontally (left, right, center) and vertically (top, bottom, middle)
 * with respect to the bounding box. The text block's line height -- ie, the baseline to baseline spacing between 
 * consecutive text lines -- is specified as a fraction of the font size, range-limited to [0.8, 3.0].
 * 
 * <p>Line breaks always respect the width of the bounding box first, and word boundaries second. Thus, if a single word
 * is too long to fit in the bounding box, it will be broken across successive lines of text as needed. If the text 
 * block is too large to fit within the bounding box, it will continue beyond the bottom and/or top edges of the box
 * (depending on the vertical alignment) -- unless the <i>clip</i> flag is set.</p>
 * 
 * <p>If the original text content contains any line breaks, those are preserved, while inserting additional line breaks
 * as described above to ensure that the longest text line fits the bounding box width.</p>
 * 
 * <p>The text box's location and size depend upon its parent viewport. The text box node itself does not admit any
 * child nodes. Also note that the actual text is stored in the <i>title</i> attribute, which is managed by the base
 * class, {@link FGraphicNode}.</p>
 * 
 * <p>As of v5.4.0 (schema version 24), <b>TextBoxNode</b> supports "styled text" content, in which text color, font
 * style, underline state, superscript, and subscript can vary on a per-character basis. See {@link 
 * FGraphicNode#toStyledText()}.</p>
 * 
 * @author sruffner
 */
public class TextBoxNode extends FGraphicNode implements Cloneable
{
   /**
    * Construct a text block node with a 2-in x 1-in bounding box located at (50%, 50%) in its parent viewport, with
    * an initial text string "Sample text box content" centered both horizontally and vertically within the box. The 
    * margin separating the text block from the bounding box edges is set to 0in, the text is not clipped, and the 
    * background fill is uniform white. The line height is set to 120% of the font size.
    */
   public TextBoxNode()
   {
      super(HASATTRMASK|ALLOWLFINTITLE|ALLOWATTRINTITLE);
      setX(new Measure(50, Measure.Unit.PCT));
      setY(new Measure(50, Measure.Unit.PCT));
      setWidth(new Measure(2, Measure.Unit.IN));
      setHeight(new Measure(1, Measure.Unit.IN));
      setRotate(0);
      setTitle("Sample text box content");
      
      margin = new Measure(0, Measure.Unit.IN);
      bkgFill = BkgFill.createSolidFill(Color.WHITE);
      hAlign = TextAlign.CENTERED;
      vAlign = TextAlign.CENTERED;
      clip = false;
      lineHeight = 1.2;
   }

   
   //
   // Text block properties
   //

   /** The background fill for the text block's bounding box: solid color (possibly transparent) or gradient. */
   private BkgFill bkgFill = null;

   /**
    * Get the current background fill for the text block's bounding box.
    * @return The background fill descriptor.
    */
   public BkgFill getBackgroundFill() { return(bkgFill); }
   
   /**
    * Set the background fill for this text block's bounding box. If a change is made, an "undo" operation is posted and
    * {@link #onNodeModified()} is invoked.
    * @param The new background fill descriptor. A null value is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setBackgroundFill(BkgFill bf)
   {
      if(bf == null) return(false);
      if(!bkgFill.equals(bf))
      {
         if(doMultiNodeEdit(FGNProperty.BKGC, bf)) return(true);
         
         BkgFill old = bkgFill;
         bkgFill = bf;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.BKGC);
            FGNRevEdit.post(this, FGNProperty.BKGC, bkgFill, old);
         }
      }
      return(true);
   }
   
   /** The measured width of the inside margin separating the text block on all sides from its bounding box borders. */
   private Measure margin;

   /**
    * Get the width of the margin separating the text block from its bounding box borders.
    * @return The margin width with associated units of measure (only physical units allowed). The same margin applies
    * to all four sides of the bounding box.
    */
   public Measure getMargin() { return(margin); }

   /**
    * Set the margin width. An "undo" operation is posted, and {@link #onNodeModified()} is invoked.
    * 
    * @param m The new margin width. It is constrained to satisfy {@link #STROKEWCONSTRAINTS}. Null is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setMargin(Measure m)
   {
      if(m == null) return(false);
      m = STROKEWCONSTRAINTS.constrain(m);

      boolean changed = (margin != m) && !Measure.equal(margin, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.GAP, m)) return(true);
         
         Measure old = margin;
         margin = m;
         if(areNotificationsEnabled())
         {
            if(old.toMilliInches() != margin.toMilliInches()) onNodeModified(FGNProperty.GAP);
            FGNRevEdit.post(this, FGNProperty.GAP, margin, old);
         }
      }
      return(true);
   }

   /** Horizontal alignment of text block relative to the bounding box. */
   private TextAlign hAlign;

   /**
    * Get the horizontal alignment of the text block with respect to its bounding box:
    * <ul>
    * <li><i>leading:</i> Each line of text starts on the left margin.</li>
    * <li><i>trailing:</i> Each line of text end on the right margin.</li>
    * <li><i>centered:</i> Each line of text is centered horizontally within the bounding box.</li>
    * </ul>
    * 
    * @return Horizontal alignment of text block within its bounding box.
    */
   public TextAlign getHorizontalAlignment() { return(hAlign); }

   /**
    * Set the horizontal alignment of the text block with respect to its bounding box. If a change is made, an "undo" 
    * operation is posted, and {@link #onNodeModified()} is invoked.
    * @param align The new horizontal alignment.
    */
   public void setHorizontalAlignment(TextAlign align)
   {
      if(hAlign != align)
      {
         if(doMultiNodeEdit(FGNProperty.HALIGN, align)) return;
         
         TextAlign old = hAlign;
         hAlign = align;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.HALIGN);
            FGNRevEdit.post(this, FGNProperty.HALIGN, hAlign, old);
         }
      }
   }
   
   /** Vertical alignment of text block relative to the bounding box. */
   private TextAlign vAlign;

   /**
    * Get the vertical alignment of the text block with respect to its bounding box:
    * <ul>
    * <li><i>leading:</i> The first line of text is aligned with the top margin.</li>
    * <li><i>trailing:</i> The last line of text is aligned with the bottom margin.</li>
    * <li><i>centered:</i> The entire text block is centered vertically within the bounding box.</li>
    * </ul>
    * Note that, if there are too many lines in the text block to fit within the bounding box, the block will continue
    * past the top and/or bottom edges of the box (depending on the alignment) -- unless the text is clipped to the
    * box boundaries.
    * 
    * @return Vertical alignment of text block within its bounding box.
    */
   public TextAlign getVerticalAlignment() { return(vAlign); }

   /**
    * Set the vertical alignment of the text block with respect to its bounding box. If a change is made, an "undo" 
    * operation is posted, and {@link #onNodeModified()} is invoked.
    * @param align The new vertical alignment.
    */
   public void setVerticalAlignment(TextAlign align)
   {
      if(vAlign != align)
      {
         if(doMultiNodeEdit(FGNProperty.VALIGN, align)) return;
         
         TextAlign old = vAlign;
         vAlign = align;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.VALIGN);
            FGNRevEdit.post(this, FGNProperty.VALIGN, vAlign, old);
         }
      }
   }

   /** If set, the text block is clipped to the interior of the bounding box (minus any non-zero margin). */
   private boolean clip;

   /**
    * Does this text box node clip its text content to the interior of its bounding box? If it is clipped, no marks
    * are made outside the rectangle specified by the bounding box minus a non-zero margin.
    * 
    * @return True if text block is clipped, false otherwise.
    */
   public boolean getClip() { return(clip); }

   /**
    * Set the clip flag for this text box node. If the flag's value is changed, an "undo" operation is posted and {@link
    * #onNodeModified()} is invoked.
    * 
    * @param clip True to enable clipping, false to disable.
    */
   public void setClip(boolean clip)
   {
      if(this.clip != clip)
      {
         if(doMultiNodeEdit(FGNProperty.CLIP, new Boolean(clip))) return;

         Boolean old = new Boolean(this.clip);
         this.clip = clip;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CLIP);
            FGNRevEdit.post(this, FGNProperty.CLIP, new Boolean(this.clip), old);
         }
      }
   }

   /** The text block's line height as a fraction of its font size. Defaults to 1.2; range-restricted to [0.8, 3.0]. */
   private double lineHeight = 1.2;

   /**
    * Get the text block line height, ie, the baseline to baseline spacing between consecutive lines of text.
    * 
    * @return The line height <i>as a fraction of the current font size</i>. Range-restricted to [0.8, 3.0].
    */
   public double getLineHeight() {  return(lineHeight); }

   /**
    * Set the text block line height, ie, the baseline to baseline spacing between consecutive lines of text.
    * 
    * @param lh The desired line height <i>as a fraction of the current font size</i>. Restricted to [0.8, 3.0].
    * @return True if successful; false if argument is NaN. Note that any out-of-range value will be silently corrected.
    */
   public boolean setLineHeight(double lh)
   {
      if(!Utilities.isWellDefined(lh)) return(false);
      
      lh = Utilities.rangeRestrict(0.8, 3.0, lh);
      if(lineHeight != lh)
      {
         if(doMultiNodeEdit(FGNProperty.LINEHT, new Double(lh))) return(true);
         
         Double old = new Double(lineHeight);
         lineHeight = lh;
         
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.LINEHT);
            FGNRevEdit.post(this, FGNProperty.LINEHT, new Double(lineHeight), old);
        }
      }
      return(true);
   }


   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
      case BKGC : ok = setBackgroundFill((BkgFill) propValue); break;
      case GAP : setMargin((Measure) propValue); ok = true; break;
      case HALIGN : setHorizontalAlignment((TextAlign) propValue); ok = true; break;
      case VALIGN : setVerticalAlignment((TextAlign) propValue); ok = true; break;
      case CLIP : setClip((Boolean) propValue); ok = true; break;
      case LINEHT : ok = setLineHeight((Double) propValue); break;
      default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
      case BKGC : value = getBackgroundFill(); break;
      case GAP : value = getMargin(); break;
      case HALIGN : value = getHorizontalAlignment(); break;
      case VALIGN : value = getVerticalAlignment(); break;
      case CLIP : value = new Boolean(getClip()); break;
      case LINEHT : value = new Double(getLineHeight()); break;
      default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }

   // 
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet()  { return(true); }

   /** 
    * The node-specific properties exported in a text box node's style set include the background fill, margin, the H 
    * and V text block alignment, the clip flag, and the text line height. 
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.BKGC, getBackgroundFill());
      styleSet.putStyle(FGNProperty.GAP, getMargin());
      styleSet.putStyle(FGNProperty.HALIGN, getHorizontalAlignment());
      styleSet.putStyle(FGNProperty.VALIGN, getVerticalAlignment());
      styleSet.putStyle(FGNProperty.CLIP, new Boolean(getClip()));
      styleSet.putStyle(FGNProperty.LINEHT, new Double(getLineHeight()));
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      BkgFill bf = (BkgFill) applied.getCheckedStyle(FGNProperty.BKGC, null, BkgFill.class);
      if(bf != null && !bf.equals(restore.getStyle(FGNProperty.BKGC)))
      {
         bkgFill = bf;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.BKGC);
      
      // this "applicable" check is because the GAP property has a different meaning for other nodes.
      FGNodeType nt = applied.getSourceNodeType();
      boolean applicable = (nt==FGNodeType.IMAGE || nt == FGNodeType.TEXTBOX);
      Measure m = (Measure) applied.getCheckedStyle(FGNProperty.GAP, null, Measure.class);
      if(applicable && m != null && !Measure.equal(m, (Measure) restore.getStyle(FGNProperty.GAP)))
      {
         margin = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.GAP);
      
      TextAlign ta = (TextAlign) applied.getCheckedStyle(FGNProperty.HALIGN, null, TextAlign.class);
      if(ta != null && !ta.equals(restore.getStyle(FGNProperty.HALIGN)))
      {
         hAlign = ta;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.HALIGN);
      
      ta = (TextAlign) applied.getCheckedStyle(FGNProperty.VALIGN, null, TextAlign.class);
      if(ta != null && !ta.equals(restore.getStyle(FGNProperty.VALIGN)))
      {
         vAlign = ta;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.VALIGN);
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.CLIP, null, Boolean.class);
      if(b != null && !b.equals(restore.getStyle(FGNProperty.CLIP)))
      {
         clip = b.booleanValue();
         changed = true;
      }
      else restore.removeStyle(FGNProperty.CLIP); 

      Double lineHt = (Double) applied.getCheckedStyle(FGNProperty.LINEHT, null, Double.class);
      if(lineHt != null && !lineHt.equals(restore.getStyle(FGNProperty.LINEHT)))
      {
         lineHeight = Utilities.rangeRestrict(0.8, 3.0, lineHt.doubleValue());
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LINEHT); 

      return(changed);
   }


   // 
   // Support for child nodes 
   //

   /** The text box node does not admit any child nodes whatsoever. */
   @Override public boolean isLeaf() { return(true); }

   /** The text box node does not admit any child nodes whatsoever. */
   @Override public boolean canInsert(FGNodeType nodeType) { return(false); }

   @Override public FGNodeType getNodeType() { return(FGNodeType.TEXTBOX); }

   //
   // Renderable/Focusable support
   //
   
   /** Checks for a translucent background fill, else defers to super class implementation. */
   @Override protected boolean isTranslucent()
   {
      if(bkgFill.getFillType() == BkgFill.Type.SOLID)
      {
         int alpha = bkgFill.getColor1().getAlpha();
         if(alpha > 0 && alpha < 255) return(true);
      }
      return(super.isTranslucent());
   }

   /**
    * The local rendering coordinate system of a text box node is defined by its location <i>(x,y)</i> and size 
    * <i>(width, height)</i>, properties that are specified WRT the parent viewport. The local origin lies at <i>(x, 
    * y)</i> WRT the parent, and the coordinate system may be rotated about this point IAW the <i>rotate</i> property. 
    * This method accounts for these properties when calculating the local-to-parent transform. If the transform is 
    * ill-defined for whatever reason, the identity transform is returned.
    */
   @Override public AffineTransform getLocalToParentTransform()
   {
      AffineTransform identity = new AffineTransform();

      // get bounding rectangle with respect to the parent viewport
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(identity);
      Rectangle2D rect = parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      if(rect == null) return(identity);

      // translate origin so that it is in coord system of parent viewport
      AffineTransform at = AffineTransform.getTranslateInstance(rect.getX(), rect.getY());

      // rotate wrt parent if necessary
      double rot = getRotate();
      if(rot !=  0) at.rotate(Math.toRadians(rot));
      
      return(at);
   }

   /**
    * The viewport for an text box node is simply its rectangular bounding box, with the origin at its BL corner. Never
    * used, since an image node does not admit children.
    */
   @Override public FViewport2D getViewport()
   { 
      // get bounding rectangle with respect to the parent viewport
      FViewport2D parentVP = getParentViewport();
      Rectangle2D rect = (parentVP==null) ? null : parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      return(rect == null ? new FViewport2D(1,1) : new FViewport2D(rect.getWidth(), rect.getHeight()));
   }

   /** The painter which renders the text block within the bounding box of this text box node. */
   private TextBoxPainter textBoxPainter = null;

   /**
    * Update the internal painter that renders the text block for this text box node. The painter is set up to draw
    * the text block into a graphics context that represents the text box's own viewport in conventional <i>DataNav</i> 
    * painting coordinates IAW the node's current properties.
    */
   private void updatePainter()
   {
      // painter is lazily constructed
      if(textBoxPainter == null) 
      {
         textBoxPainter = new TextBoxPainter();
         textBoxPainter.setStyle(this); 
      }
      
      // get bottom left corner and dimensions of text box in the parent viewport
      FViewport2D parentVP = getParentViewport();
      Rectangle2D r = (parentVP==null) ? null : parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      
      textBoxPainter.setText(getAttributedTitle(true));
      textBoxPainter.setBoundingBox(r==null ? null : new Point2D.Double(), 
            r.getWidth(), r.getHeight(), margin.toMilliInches());;
      textBoxPainter.setAlignment(hAlign, vAlign);
      textBoxPainter.setBackgroundFill(bkgFill);
      textBoxPainter.setClipped(clip);
      textBoxPainter.setLineHeight(lineHeight);
   }

   @Override protected void releaseRenderResourcesForSelf() { textBoxPainter = null; }

   /**
    * If text content is clipped, the render bounds of the text box node is its bounding box, expanded on all sides by a
    * half stroke-width. However, if content is not clipped, we have to union the bounding box with the render bounds of
    * the laid out text. If the bounding box is currently ill-defined, an empty rectangle is returned.
    */
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      // we always update our textbox painter delegate here, since this gets called whenever a node property changes.
      updatePainter();
      
      if(clip)
      {
         Rectangle2D r = new Rectangle2D.Double();
         FViewport2D parentVP = getParentViewport();
         Rectangle2D bbRect = (parentVP==null) ? null : parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
         double hsw = (isStroked() ? getStrokeWidth() : 0)/2.0;
         if(bbRect != null) r.setFrame(-hsw, -hsw, bbRect.getWidth() + hsw*2, bbRect.getHeight() + hsw*2);
         return(r);
      }
      
      textBoxPainter.updateFontRenderContext(g2d);
      textBoxPainter.invalidateBounds();
      return(textBoxPainter.getBounds2D(null));
   }

   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(textBoxPainter == null) updatePainter();
      FViewport2D parentVP = getParentViewport();
      Rectangle2D r = (parentVP==null) ? null : parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());

      boolean needRender = r != null && needsRendering(task) && r.getWidth() > 0 && r.getHeight() > 0;      
      if(!needRender) return(task == null ? true : task.updateProgress());

      // render text box node in a copy of the graphics context, so we do not alter the original
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         g2dCopy.translate(r.getX(), r.getY());
         if(getRotate() != 0) g2dCopy.rotate(Math.toRadians(getRotate()));

         textBoxPainter.render(g2dCopy, task);
      }
      finally {if(g2dCopy != null) g2dCopy.dispose(); }

      return((task == null) ? true : task.updateProgress());
   }

   
   //
   // PSTransformable implementation
   //

   /** 
    * Overridden to just specify the first 10 characters of the text content, with any linefeeds removed.
    */
   @Override public String getPSComment()
   {
      String s = getTitle();
      if(s.length() > 10) s = s.substring(0,10) + "...";
      s = s.replaceAll("(?:\\n|\\r|\\t)", "");
      return(getNodeType().toString() + ": " + s);
   }

   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      Point2D loc = parentVP.toMilliInches(getX(), getY());
      double w = parentVP.fromMeasureToMilliInX(getWidth());
      double h = parentVP.fromMeasureToMilliInY(getHeight());
      if(loc == null || w <= 0 || h <= 0) return;

      psDoc.startElement(this);
      psDoc.translateAndRotate(loc, getRotate());
      
      // stroke and fill the bounding box. If the fill color is transparent, the box is not filled.
      psDoc.renderRect(w, h, getStrokeWidth(), getStrokeColor().getAlpha() != 0, bkgFill);

      // if the text content is an empty string, we're done! We're also done if the text/fill color is transparent!
      String content = getTitle().trim();
      if(content.isEmpty() || getFillColor().getAlpha() == 0)
      {
         psDoc.endElement();
         return;
      }
      
      // shift origin to the bottom left corner of the bounding box LESS the margin (the tight text bounding box). Set
      // clip rectangle if specified.
      double m = getMargin().toMilliInches();
      loc.setLocation(m, m);
      w -= 2*m;
      h -= 2*m;
      psDoc.setViewport(loc, w, h, 0, getClip());
      
      // render the text content, laying it out to respect the tight bounding box width and aligned with respect to 
      // that box. If not clipped and the text does not fit, it will extend beyond the top and/or bottom edges of the
      // tight bounding box -- depending on the vertical alignment.
      psDoc.renderTextInBox(content, w, h, getLineHeight(), getNetRotate(), getHorizontalAlignment(), 
            getVerticalAlignment());

      psDoc.endElement();
   }

   
   //
   // Object
   //

   /** Ensures that the rendering infrastructure for the clone is independent of the text box node cloned. */
   @Override protected Object clone()
   {
      TextBoxNode copy = (TextBoxNode) super.clone();
      copy.textBoxPainter = null;
      return(copy);
   }
}
