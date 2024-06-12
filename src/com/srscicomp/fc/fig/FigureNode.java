package com.srscicomp.fc.fig;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dutil.StrokeJoin;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dutil.TextBoxPainter;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.ui.BkgFill;

/**
 * <b>FigureNode</b> encapsulates an entire figure of graphed data and other rendered elements. As the {@link 
 * FRootGraphicNode} in FC's graphical model, {@link FGraphicModel}, it defines the "global" coordinate system in which 
 * all of its descendant nodes are rendered. It sets a bounding box specified in physical units like inches, defining 
 * the figure's exact size and -- when printed or exported to a Postscript file -- its location WRT the printed page's 
 * imageable region. It may contain any number of graphs, shapes, text labels, and lines. It also defines explicit 
 * values for all inheritable graphic style attributes in the <i>FypML</i> schema. See {@link FGraphicNode} for a 
 * discussion of these styles.
 * 
 * <p>The figure node is, for the most part, just a container for other elements. It does render an optional border
 * and title. The title can be multi-line and can be aligned horizontally and vertically in the figure's bounding box
 * (inside the border, if any).</p>
 * 
 * @author 	sruffner
 */
public class FigureNode extends FRootGraphicNode implements Cloneable
{
   /**
    * Construct an empty figure with no title and no note. Its border width is 0in by default, and the background fill
    * is fully transparent. Its initial size is 6.5x9in, and its bottom-left corner is at (0.5in, 0.5in). As a result, 
    * the figure bounds are centered on an 8.5x11-in page with 1-in between each edge of the figure rectangle and the 
    * corresponding edge of the page (in portrait orientation).
    * 
    * <p>The figure title is hidden by default, centered horizontally in the figure's bounding box and vertically 
    * aligned with its top edge.</p>
    */
   public FigureNode()
   {
      super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR|HASLOCATTRS|HASSIZEATTRS|
            HASTITLEATTR|ALLOWLFINTITLE|ALLOWATTRINTITLE);

      // set initial values for figure properties (super-class sets initial values for all styles)
      setX(new Measure(0.5, Measure.Unit.IN));
      setY(new Measure(0.5, Measure.Unit.IN));
      setWidth(new Measure(6.5, Measure.Unit.IN));
      setHeight(new Measure(9, Measure.Unit.IN));
      setTitle("");
      
      hideTitle = true;
      hAlign = TextAlign.CENTERED;
      vAlign = TextAlign.LEADING;
      
      borderWidth = new Measure(0, Measure.Unit.IN);
      oldBorderWidth = borderWidth;
      bkgFill = BkgFill.createSolidFill(new Color(0, 0, 0, 0));
      note = "";
   }

   
   // 
   // Figure properties
   //

   /**
    * The measured width of the figure's border.
    */
   private Measure borderWidth;

   /**
    * The previous border width -- for the purpose of efficiently updating the figure when the border changes.
    */
   private Measure oldBorderWidth;

   /**
    * Get the border width for this <code>FigureNode</code>.
    * 
    * @return The border width with associated units of measure (only physical units allowed).
    */
   public Measure getBorderWidth() { return(borderWidth); }

   /**
    * Set the border width. The figure's rendering infrastructure is updated accordingly, and a rendering pass is 
    * triggered.
    * 
    * @param m The new border width. The measure is constrained to satisfy {@link #STROKEWCONSTRAINTS}. A null value is
    * rejected.
    * @return True if successful; false if value was rejected.
    * @see FigureNode#STROKEWCONSTRAINTS
    */
   public boolean setBorderWidth(Measure m)
   {
      if(m == null) return(false);
      m = STROKEWCONSTRAINTS.constrain(m);

      boolean changed = (borderWidth != m) && !Measure.equal(borderWidth, m);
      if(changed)
      {
         oldBorderWidth = borderWidth;
         borderWidth = m;
         if(areNotificationsEnabled())
         {
            if(oldBorderWidth.toMilliInches() != borderWidth.toMilliInches())
               onNodeModified(FGNProperty.BORDER);
            FGNRevEdit.post(this, FGNProperty.BORDER, borderWidth, oldBorderWidth);
         }
      }
      return(true);
   }

   /** The background fill for the figure's bounding box: solid color or gradient; almost always transparent. */
   private BkgFill bkgFill = null;

   /**
    * Get the current background fill for the figure's bounding box. Typically, the background is fully transparent,
    * meaning no fill whatsoever. But there are use cases for an opaque fill.
    * @return The background fill descriptor.
    */
   public BkgFill getBackgroundFill() { return(bkgFill); }
   
   /**
    * Set the background fill for this figure's bounding box. If a change is made, an "undo" operation is posted and
    * {@link #onNodeModified()} is invoked.
    * @param The new background fill descriptor. A null value is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setBackgroundFill(BkgFill bf)
   {
      if(bf == null) return(false);
      if(!bkgFill.equals(bf))
      {
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
      
   /** A description or note about the figure. It is never rendered. */
   private String note;
   
   public String getNote() { return(note); }
   
   /**
   * Update the figure's note. If a change is made, the method {@link #onNodeModified() is invoked; a render cycle will
   * not be triggered since the note is never rendered.
   * 
   * @param s The new note. Null is treated as an empty string. Carriage returns are silently stripped from the string; 
   * while they are part of the supported character set, they're not allowed in the note. However, line-feed characters 
   * are allowed in order to break the note onto multiple text lines. Furthermore, tabs are permitted.
   * @return True if new note was accepted without change; false if note was updated with changes (any CR characters 
   * removed; any unsupported characters replaced by a question mark).
   */
   public boolean setNote(String s)
   {
      String str = (s==null) ? "" : s;
      str = str.replaceAll("\r", "");
      if(!str.equals(note))
      {
         String oldNote = note;
         note = FGraphicModel.replaceUnsupportedCharacters(str);
         
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.NOTE);
            FGNRevEdit.post(this, FGNProperty.NOTE, note, oldNote);
         }
      }
      return(str.equals(note));
   }

   /** Hide or show the figure's title (if not an empty string). */
   private boolean hideTitle;

   /**
    * Is the figure's title hidden?
    * @return True if title is hidden; false if non-empty title is rendered.
    */
   public boolean isTitleHidden() { return(hideTitle); }

   /**
    * Hide or show the figure's title.
    * @param b True to show, false to hide the figure's title.
    */
   public void setTitleHidden(boolean b)
   {
      if(hideTitle != b)
      {
         Boolean old = new Boolean(hideTitle);
         hideTitle = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.HIDE);
            String desc = (hideTitle ? "Hide" : "Show") + " figure's title";
            FGNRevEdit.post(this, FGNProperty.HIDE, new Boolean(hideTitle), old, desc);
         }
      }
   }

   /** Is the figure's title rendered? The title string must be non-empty and it must not be hidden. */
   private boolean isTitleRendered() { return(!(hideTitle || getTitle().trim().isEmpty())); }
   
   /** Horizontal alignment of figure's title WRT its bounding box. */
   private TextAlign hAlign;

   /**
    * Get the horizontal alignment of the figure's title with respect to its bounding box (inside any border).
    * <ul>
    * <li>{@link TextAlign#LEADING}: Each line in title starts on the left edge.</li>
    * <li>{@link TextAlign#TRAILING}: Each line in title ends on the right edge.</li>
    * <li>{@link TextAlign#CENTERED}: Each line of text is centered horizontally.</li>
    * </ul>
    * 
    * @return Horizontal alignment of figure's title.
    */
   public TextAlign getTitleHorizontalAlignment() { return(hAlign); }

   /**
    * Set the horizontal alignment of the figure's title with respect to its bounding box.
    * @param align The new horizontal alignment.
    * @see {@link #getTitleHorizontalAlignment()}
    */
   public void setTitleHorizontalAlignment(TextAlign align)
   {
      if(hAlign != align)
      {
         TextAlign old = hAlign;
         hAlign = align;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.HALIGN);
            FGNRevEdit.post(this, FGNProperty.HALIGN, hAlign, old);
         }
      }
   }
   
   /** Vertical alignment of figure's title WRT its bounding box. */
   private TextAlign vAlign;

   /**
    * Get the vertical alignment of the figure's title with respect to its bounding box (inside any border).
    * <ul>
    * <li>{@link TextAlign#LEADING}: First line in title aligns with the top edge.</li>
    * <li>{@link TextAlign#TRAILING}: Last line in title aligns with the bottom edge.</li>
    * <li>{@link TextAlign#CENTERED}: The entire title text block is centered vertically.</li>
    * </ul>
    * 
    * @return Vertical alignment of figure's title.
    */
   public TextAlign getTitleVerticalAlignment() { return(vAlign); }

   /**
    * Set the vertical alignment of the figure's title with respect to its bounding box.
    * @param align The new vertical alignment.
    * @see {@link #getTitleVerticalAlignment()}
    */
   public void setTitleVerticalAlignment(TextAlign align)
   {
      if(vAlign != align)
      {
         TextAlign old = vAlign;
         vAlign = align;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.VALIGN);
            FGNRevEdit.post(this, FGNProperty.VALIGN, vAlign, old);
         }
      }
   }


   /**
    * Changes in most properties of the figure should trigger a full re-rendering, which requires sending a null dirty 
    * regions list. [Note that the super-class implementation does not work here, because it will prepare a list of two 
    * rectangles, the before and after bounds of the entire figure.] Exceptions:
    * <ul>
    * <li>When the figure's note changes, then the model is not re-rendered at all.</li>
    * <li>When the figure's title is shown/hidden or realigned but the title text is empty, or when the title text 
    * content changes or its H/V alignment changes but the title is hidden, then again there's no change to the 
    * rendered model.</li>
    * </ul>
    */
   @Override
   protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;
      
      // the figure's "note" is not rendered. Likewise enabling/disabling the auto title when there is no text to
      // render, or changing the title text when the automated title is disabled, has no effect on the rendered
      // appearance of the figure.
      if((hint == FGNProperty.NOTE) || 
            (hint==FGNProperty.HIDE && getTitle().trim().isEmpty()) ||
            (hint==FGNProperty.TITLE && hideTitle) ||
            ((hint==FGNProperty.HALIGN || hint==FGNProperty.VALIGN) && !isTitleRendered()))
      {
         model.onChange(this, 0, false, null);
         return;
      }
      
      Graphics2D g2d = model.getViewerGraphics();
      try
      {
         getRenderBounds(g2d, true, null);
         model.onChange(this, 0, true, null);
      }
      finally { if(g2d != null) g2d.dispose(); }
   }

   @Override protected void rescaleSelf(double scale, MultiRevEdit undoer)
   {
      Measure.Constraints c = STROKEWCONSTRAINTS;
      double d = borderWidth.getValue();
      if(d > 0)
      {
         Measure old = borderWidth;
         borderWidth = c.constrain(new Measure(d*scale, borderWidth.getUnits()));
         oldBorderWidth = borderWidth;
         
         undoer.addPropertyChange(this, FGNProperty.BORDER, borderWidth, old);
      }
   }

   /** Overridden to make publicly accessible, since it makes sense to rescale an entire figure. */
   @Override public void rescale(int pct, boolean fontSizeOnly) { super.rescale(pct, fontSizeOnly); }

   
   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
      case BORDER : ok = setBorderWidth((Measure) propValue); break;
      case BKGC : ok = setBackgroundFill((BkgFill) propValue); break;
      case NOTE : ok = setNote((String) propValue); break;
      case HIDE : setTitleHidden((Boolean) propValue); ok = true; break;
      case HALIGN : setTitleHorizontalAlignment((TextAlign) propValue); ok = true; break;
      case VALIGN : setTitleVerticalAlignment((TextAlign) propValue); ok = true; break;
      default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
         case BORDER : value = getBorderWidth(); break;
         case BKGC: value = getBackgroundFill(); break;
         case NOTE: value = getNote(); break;
         case HIDE: value = new Boolean(isTitleHidden()); break;
         case HALIGN: value = getTitleHorizontalAlignment(); break;
         case VALIGN: value = getTitleVerticalAlignment(); break;
         default: value = super.getPropertyValue(p); break;
      }
      return(value);
   }

   // 
   // Support for child nodes
   //

   @Override public boolean canInsert(FGNodeType nodeType)
   {
      return(nodeType == FGNodeType.GRAPH || nodeType == FGNodeType.PGRAPH || nodeType == FGNodeType.GRAPH3D || 
            nodeType == FGNodeType.LABEL || nodeType == FGNodeType.TEXTBOX || nodeType == FGNodeType.LINE || 
            nodeType == FGNodeType.SHAPE || nodeType == FGNodeType.IMAGE);
   }

   @Override public FGNodeType getNodeType() { return(FGNodeType.FIGURE); }

   /**
    * If no descendant node is under the point specified, but that point is within the bounding box of the figure
    * itself, then return <b>this</b>.
    */
   @Override protected FGraphicNode hitTest(Point2D p)
   {
      FGraphicNode n = super.hitTest(p);
      if(n == null && p.getX() >= 0 && p.getY() >= 0 && p.getX() <= getWidthMI() && p.getY() <= getHeightMI())
         n = this;
      return(n);
   }


   //
   // RootRenderable/Focusable/Renderable implementation
   //

   @Override public double getWidthMI() { return(getWidth().toMilliInches()); }
   @Override public double getHeightMI() { return(getHeight().toMilliInches()); }
   @Override public Point2D getPrintLocationMI() 
   { 
      return(new Point2D.Double(getX().toMilliInches(), getY().toMilliInches())); 
   }
   @Override public boolean hasTranslucentRegions() { return(isTranslucent()); }
   
   /** Since a <b>FigureNode</b> is always the root node, this method simply returns the identity transform. */
   @Override public AffineTransform getLocalToParentTransform() { return(new AffineTransform()); }

   /** A <b>FigureNode</b>'s viewport is defined by its absolute rectangular dimensions. */
   @Override public FViewport2D getViewport()
   {
      return(new FViewport2D(getWidth().toMilliInches(), getHeight().toMilliInches()));
   }

   /**
    * A <b>FigureNode</b> does not have a focus highlight shape. We only want to see a focus highlight when the display 
    * focus is on a descendant of the node.
    */
   @Override public Shape getFocusShape(Graphics2D g2d) { return(null); }

   /**
    * While a figure node has the standard location attributes <i>x</i> and <i>y</i>, it is not intended to be moved 
    * interactively because it is the root node. This override returns false always.
    */
   @Override public boolean canMove() { return(false); }

   /** A figure node may not be resized interactively. This override returns false always. */
   @Override public boolean canResize() { return(false); }

   /** 
    * Allow a figure node to participate in an alignment operation, although the figure itself is never adjusted during 
    * such operation. The other nodes are simply aligned to one of the edges or the H or V center of the figure -- if
    * the figure is the anchor node for the alignment operation.
    */
   @Override boolean canAlign() { return(true); }

   @Override Double getAlignmentLocus(FGNAlign align)
   {
      if(align == null) return(null);
      double locus = 0;
      switch(align)
      {
      case LEFT : 
      case BOTTOM : locus = 0; break;
      case RIGHT : locus = getWidthMI(); break;
      case TOP : locus = getHeightMI(); break;
      case HCENTER : locus = getWidthMI()/2.0; break;
      case VCENTER : locus = getHeightMI()/2.0; break;
      }
      return(new Double(locus));
   }

   /** No-op. A figure node is never adjusted during an alignment operation; the other nodes are aligned WRT it. */
   @Override boolean align(FGNAlign alignType, double locus, MultiRevEdit undoer) { return(false); }

   /** If the figure has a solid but translucent background fill, then obviously it is translucent! */
   @Override protected boolean isTranslucent()
   {
      if(bkgFill.getFillType() == BkgFill.Type.SOLID)
      {
         int alpha = bkgFill.getColor1().getAlpha();
         if(alpha > 0 && alpha < 255) return(true);
      }
      return(super.isTranslucent());
   }

   /** No-op. The figure node maintains no resources for rendering itself. */
   @Override  protected void releaseRenderResourcesForSelf() {}

   
   private final static double LINEHT_FOR_TITLE = 1.3;
   
   /** 
    * Prepare the {@link TextBoxPainter} that renders this figure's title IAW the figure's current state.
    * 
    * @return The painter, ready for rendering. Returns null if the title is not rendered.
    */
   private TextBoxPainter prepareAutoTitlePainter()
   {
      if(!isTitleRendered()) return(null);
      
      TextBoxPainter p = new TextBoxPainter();
      p.setStyle(this);
      p.setBorderStroked(false);
      p.setBoundingBox(new Point2D.Double(), getWidthMI(), getHeightMI(), borderWidth.toMilliInches());
      p.setText(fromStyledText(getTitle(), getFontFamily(), getFontSizeInPoints(), getFillColor(), 
            getFontStyle(), true));
      p.setAlignment(hAlign, vAlign);
      p.setLineHeight(LINEHT_FOR_TITLE);
      return(p);
   }
   
   /**
    * Typically, the figure node itself renders nothing, and this method returns an empty rectangle. However, if the
    * figure's border width is nonzero or its background fill is not transparent, then the figure's render bounds are 
    * its entire bounding box. If there's no border but the auto-generated title is rendered, then the figure's render
    * bounds are just the bounding box for that title. Regardless, the rectangle is calculated afresh on each 
    * invocation; it is not cached.
    */
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      Rectangle2D r = new Rectangle2D.Double();
      if(borderWidth.toMilliInches() > 0 || !bkgFill.isTransparent())
         r.setRect(0, 0, getWidthMI(), getHeightMI());
      else
      {
         TextBoxPainter p = prepareAutoTitlePainter();
         if(p != null)
         {
            p.updateFontRenderContext(g2d);
            p.invalidateBounds();
            p.getBounds2D(r);
         }
      }
      return(r);
   }

   /**
    * The figure node clips the rendering of its children to the figure's boundaries. If an opaque background fill is
    * specified, the entire figure bounding box is filled accordingly prior to rendering any children. If the figure's
    * border width is non-zero and/or its auto-generated title enabled, the border and title are rendered <i>after</i> 
    * all children have been rendered.
    */
   public boolean render(Graphics2D g2d, RenderTask task)
   {
      // make sure it needs rendering
      if(!needsRendering(task))
         return(true);

      // adjust clip so that no one can draw outside figure boundaries
      double figW = getWidthMI();
      double figH = getHeightMI();
      Rectangle2D figRect = new Rectangle2D.Double(0, 0, figW, figH);
      g2d.clip(figRect);
      
      // if background color is opaque, fill the figure bounding box with that color before rendering children
      if(!bkgFill.isTransparent())
      {
         g2d.setPaint(bkgFill.getPaintForFill((float) figW, (float) figH));
         g2d.fill(figRect);
      }
      
      // render all children.  Abort immediately, if any child aborts rendering
      if(!renderSubordinates(g2d, task))
         return(false);

      // render the border if it has nonzero width
      double borderW = borderWidth.toMilliInches();
      if(borderW > 0)
      {
         // border rectangle in figure viewport coords. Border is just inside figure rectangle.
         Rectangle2D rBorder = new Rectangle2D.Double(borderW/2, borderW/2, figW-borderW, figH-borderW);
         
         // border is drawn using figure's stroking properties -- except stroke width replaced by border width!
         g2d.setColor(getStrokeColor());
         StrokePattern sp = getStrokePattern();
         StrokeCap sc = getStrokeEndcap();
         StrokeJoin sj = getStrokeJoin();
         Stroke strk = sp.isSolid() ?
            new BasicStroke((float)borderW, sc.getJava2DLineCap(), sj.getJava2DLineJoin(), 10) :
            new BasicStroke((float)borderW, sc.getJava2DLineCap(), sj.getJava2DLineJoin(), 10, 
                  sp.getDashGapArrayMilliIn((float)borderW, sc != StrokeCap.BUTT), 0);

         g2d.setStroke(strk);
         g2d.draw(rBorder);
      }

      // render the auto-generated figure title, if applicable
      TextBoxPainter p = prepareAutoTitlePainter();
      if(p != null) p.render(g2d, null);
      
      // if we get here, the rendering "job" has completed successfully!
      return(true);
   }

   
   // 
   // PSTransformable implementation
   //
   
   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      // start a new page, then establish a clippling viewport within that page with origin at the figure's bottom-left 
      // corner and having the figure's dimensions in thousandth inches
      double w = getWidthMI();
      double h = getHeightMI();
      Point2D bottomLeft = new Point2D.Double(getX().toMilliInches(), getY().toMilliInches());

      psDoc.startPage(this);
      psDoc.setViewport(bottomLeft, w, h, 0, true);

      // if background color is opaque, fill the figure bounding box with that color before rendering children
      if(!bkgFill.isTransparent())
         psDoc.renderRect(w, h, 0, false, bkgFill);

      // render all descendants
      for(int i=0; i<getChildCount(); i++) getChildAt(i).toPostscript(psDoc);

      // paint the figure's border if there is one. Don't paint it if stroke color is transparent.
      double borderW = getBorderWidth().toMilliInches();
      if(borderW > 0 && getStrokeColor().getAlpha() > 0)
      {
         bottomLeft.setLocation(borderW/2.0, borderW/2.0);
         w -= borderW;
         h -= borderW;
         psDoc.renderRect(bottomLeft, w, h, borderW, true, false);
      }

      // paint the figure's title if applicable, just inside any border
      if(isTitleRendered())
      {
         if(borderW > 0)
         {
            bottomLeft.setLocation(borderW, borderW);
            w -= 2*borderW;
            h -= 2*borderW;
            psDoc.setViewport(bottomLeft, w, h, 0, true);
         }
         String content = getTitle().trim();
         psDoc.renderTextInBox(content, w, h, LINEHT_FOR_TITLE, 0, hAlign, vAlign);
      }
      
      // end the page
      psDoc.endPage();
   }
}
