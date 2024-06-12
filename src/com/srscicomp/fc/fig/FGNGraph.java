package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.srscicomp.common.g2dutil.ResizeAnchor;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dutil.TextBoxPainter;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.fc.data.DataSet;

/**
 * <b>FGNGraph</b> is an abstract class representing any 2D or 3D graph container in the FypML graphic model.
 * 
 * <p>Initially, {@link GraphNode} was the only graph object in the FypML graphic model. It supports 2D Cartesian,
 * semilog, loglog, and polar plots. More recently, two additional plot containers have been introduced: FC 5.0.0 added
 * a 3D graph container, !@link Graph3DNode}, while FC 5.1.2 introduced a second kind of 2D graph container which only 
 * supports polar plots -- {@link PolarPlotNode}. This node automatically lays out the grid labels for a polar plot
 * and offers more versatility than the polar coordinate system mode in <b>GraphNode</b>.</p>
 * 
 * <p>While these are very different graph container objects, they do share some common implementation details, and in 
 * certain scenarios plottable data nodes and graph components -- axes, legend, color bar, etc -- need to refer to their
 * parent graph container. For these reasons, <b>FGNGraph</b> was introduced as an abstract super class representing any
 * kind of graph container. All concrete graph container nodes descend from this class.</p>
 * 
 * <p>As of FC 5.4.4, all graph containers admit a multi-line title that can be rendered WRT to the container's 
 * bounding box. This super class implements attributes controlling the visibility and positioning of the title, and
 * provides methods to render the title to a graphics context or to a Postscript document. By design, the title is
 * positioned in a text block above or below the graph's bounding box depending on {@link #isTitleAboveGraph()}, aligned
 * to the bottom or top of the block, respectively. The text block spans the width of the graph, can accommodate up to
 * 3 lines of text, and is offset from the top or bottom edge of the graph AW {@link #getTitleGap()}.</p>
 * 
 * @author sruffner
 */
public abstract class FGNGraph extends FGraphicNode implements Cloneable
{
   /**
    * Construct a new, empty graph container. This will setup the container with an initial bounding box located at
    * (0.75in, 0.75in) in the parent figure, with initial dimensions of 2.5in x 2.5in. The <i>title</i> attribute is
    * initially empty, and all graphic styles are inherited from the parent. 
    * 
    * @param allowRotate If true, the graph container supports the <i>rotate</i> property, so that its orientation
    * may be rotated WRT the parent viewport. The rotation angle will be 0 initially.
    * @param allowClip If true, the graph container can clip data to its data window. <b>FGNGraph</b> provides methods
    * for getting and setting the clip property.
    * @param enaClip If true, the graph container is initiallly configured to clip data to its data window. This 
    * property may be modified via {@link #setClip()}. Ignored if <i>allowClip=false</i>.
    */
   FGNGraph(boolean allowRotate, boolean allowClip, boolean enaClip) 
   { 
      super(allowRotate ? HASATTRMASKALL : (HASATTRMASKALL & ~HASROTATEATTR)); 
      this.canClip = allowClip;
      
      setX(new Measure(0.75, Measure.Unit.IN));
      setY(new Measure(0.75, Measure.Unit.IN));
      setWidth(new Measure(2.5, Measure.Unit.IN));
      setHeight(new Measure(2.5, Measure.Unit.IN));
      if(allowRotate) setRotate(0);
      setTitle("");
      
      clip = allowClip && enaClip;
      hideTitle = true;
      hAlign = TextAlign.CENTERED;
      titleGap = new Measure(0, Measure.Unit.IN);
   }

   /** True if data clipping is supported by the graph container, false otherwise. */
   private final boolean canClip;
   
   /** Enables clipping of data to the graph's data window. */
   private boolean clip;

   /** Are data traces currently clipped to the graph's data window? */
   public final boolean getClip() { return(clip); }

   /**
    * Enable or disable clipping of data to the graph's data window. If the clip state is changed, {@link 
    * #onNodeModified()} is invoked. Method has no effect if graph container does not support data clipping.
    * @param clip True to enable clipping, false to disable.
    */
   public void setClip(boolean clip)
   {
      if(!canClip) return;
      
      if(this.clip != clip)
      {
         if(doMultiNodeEdit(FGNProperty.CLIP, new Boolean(clip))) return;
         
         Boolean old = new Boolean(this.clip);
         this.clip = clip;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CLIP);
            FGNRevEdit.post(this, FGNProperty.CLIP, new Boolean(this.clip), old, 
                  (this.clip ? "Enable " : "Disable ") + "clipping of graph data");
         }
      }
   }

   /** Hide or show the graph's title (if not an empty string). */
   private boolean hideTitle;

   /**
    * Is the graph's title hidden?
    * @return True if title is hidden; false if non-empty title is rendered.
    */
   public boolean isTitleHidden() { return(hideTitle); }

   /**
    * Hide or show the graph's title.
    * @param b True to show, false to hide the graph's title.
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
            String desc = (hideTitle ? "Hide" : "Show") + " graph's title";
            FGNRevEdit.post(this, FGNProperty.HIDE, new Boolean(hideTitle), old, desc);
         }
      }
   }

   /** Is the graph's semi-automated title rendered? The title string must be non-empty and it must not be hidden. */
   boolean isTitleRendered() { return(!(hideTitle || getTitle().trim().isEmpty())); }
   
   /** 
    * Is the graph's semi-automated title positioned above or below the graph? The base class implememtation always
    * returns true -- override if necessary.
    */
   protected boolean isTitleAboveGraph() { return(true); }
   
   /** Horizontal alignment of graph's title WRT its bounding box. */
   private TextAlign hAlign;

   /**
    * Get the horizontal alignment of the graph's title with respect to its bounding box.
    * <ul>
    * <li>{@link TextAlign#LEADING}: Each line in title starts on the left edge of box.</li>
    * <li>{@link TextAlign#TRAILING}: Each line in title ends on the right edge of box.</li>
    * <li>{@link TextAlign#CENTERED}: Each line of text is centered horizontally WRT bounding box.</li>
    * </ul>
    * 
    * @return Horizontal alignment of graph's title.
    */
   public TextAlign getTitleHorizontalAlignment() { return(hAlign); }

   /**
    * Set the horizontal alignment of the graph's title with respect to its bounding box.
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
   
   /**
    * Constrains the graph's title gap to use physical units, a maximum of 4 significant and 3 fractional digits, and
    * lie in the range [0..0.5in].
    */
   public final static Measure.Constraints TITLEGAPCONSTRAINTS = new Measure.Constraints(0.0, 500.0, 4, 3);

   /** Gap separating rendered title from the top or bottom edge of graph's bounding box. */
   private Measure titleGap;

   /**
    * Get the size of the gap separating the rendered title from the top or bottom edge of the graph's bounding box.
    * @return The title gap, specified as a linear measurement with associated units.
    */
   public Measure getTitleGap() { return(titleGap); }

   /**
    * Get the size of the gap separating the rendered title from the top or bottom edge of the graph's bounding box.
    * If a change is made, {@link #onNodeModified()} is invoked.
    * 
    * @param m The new title gap. The measure is constrained to satisfy {@link #TITLEGAPCONSTRAINTS}. A null value 
    * is rejected.
    * @return True if change was accepted; false if rejected.
    */
   public boolean setTitleGap(Measure m)
   {
      if(m == null) return(false);
      m = TITLEGAPCONSTRAINTS.constrain(m);

      boolean changed = (titleGap != m) && !Measure.equal(titleGap, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.GAP, m)) return(true);
         
         Measure oldGap = titleGap;
         titleGap = m;
         if(areNotificationsEnabled())
         {
            if(oldGap.toMilliInches() != titleGap.toMilliInches()) onNodeModified(FGNProperty.GAP);
            String desc = "Set graph's title gap to: " + titleGap.toString();
            FGNRevEdit.post(this, FGNProperty.GAP, titleGap, oldGap, desc);
         }
      }
      return(true);
   }


   /** Overridden to handle changes to this graph container's clip state. */
   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
      case CLIP:
         if(canClip)
         {
            setClip((Boolean)propValue);
            ok = true;
         }
         break;
      case HIDE : setTitleHidden((Boolean) propValue); ok = true; break;
      case HALIGN : setTitleHorizontalAlignment((TextAlign) propValue); ok = true; break;
      case GAP: ok = setTitleGap((Measure) propValue); break;
      default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
      case CLIP: value = new Boolean(getClip()); break;
      case HIDE: value = new Boolean(isTitleHidden()); break;
      case HALIGN: value = getTitleHorizontalAlignment(); break;
      case GAP: value = getTitleGap(); break;
      default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }
   
   @Override public boolean supportsStyleSet() { return(true); }

   /**
    * Overridden to include the graph's clip flag (if supported), and several attributes controlling the visibility and
    * positioning of the graph's automated title.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      if(canClip) styleSet.putStyle(FGNProperty.CLIP, new Boolean(getClip()));
      styleSet.putStyle(FGNProperty.HIDE, new Boolean(isTitleHidden()));
      styleSet.putStyle(FGNProperty.HALIGN, getTitleHorizontalAlignment());
      styleSet.putStyle(FGNProperty.GAP, getTitleGap());
   }
   
   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      if(canClip)
      {
         Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.CLIP, getNodeType(), Boolean.class);
         if(b != null && !b.equals(restore.getCheckedStyle(FGNProperty.CLIP, null, Boolean.class)))
         {
            clip = b.booleanValue();
            changed = true;
         }
         else restore.removeStyle(FGNProperty.CLIP);
      }
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.HIDE, getNodeType(), Boolean.class);
      if(b != null && !b.equals(restore.getCheckedStyle(FGNProperty.HIDE, null, Boolean.class)))
      {
         hideTitle = b.booleanValue();
         changed = true;
      }
      else restore.removeStyle(FGNProperty.HIDE);
      
      TextAlign ta = (TextAlign) applied.getCheckedStyle(FGNProperty.HALIGN, getNodeType(), TextAlign.class);
      if(ta != null && !ta.equals(restore.getStyle(FGNProperty.HALIGN)))
      {
         hAlign = ta;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.HALIGN);
      
      Measure m = (Measure) applied.getCheckedStyle(FGNProperty.GAP, getNodeType(), Measure.class);
      if(m != null) m = TITLEGAPCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure)restore.getCheckedStyle(FGNProperty.GAP, null, Measure.class)))
      {
         titleGap = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.GAP);
      
      return(changed);
  }
   
   /**
    * Return the bounding box rectangle for this graph container, specified in its local rendering coordinates.
    * 
    * <p>The bottom-left corner of the returned rectangle will be (0,0); the width and height reflect the current values
    * of the graph's <i>width</i> and <i>height</i> properties, converted to milli-inches. If the parent container's 
    * viewport is not well-defined for any reason, an empty rectangle is returned.</p>
    * 
    * @return The graph's local bounding box rectangle, as described.
    */
   public Rectangle2D getBoundingBoxLocal()
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP != null)
      {
         Rectangle2D rect = parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
         if(rect != null)
         {
            rect.setFrame(0, 0, rect.getWidth(), rect.getHeight());
            return(rect);
         }
      }
      return( new Rectangle2D.Double() );
   }
   
   /** Get the legend component for this graph container. */
   public abstract LegendNode getLegend();
   
   /** 
    * Get the color bar for this graph container. The color bar encapsulates the graph's color map properties, which
    * map the third "Z" coordinate of 3D data sets to colors in 2D or 3D plots. It also optionally renders the color map
    * gradient with an associated axis line.
    */
   public abstract ColorBarNode getColorBar();
   
   
   /** Enumeration of the different kinds of coordinate systems which may be available in a graph container. */
   public enum CoordSys
   {
      /** The standard XY Cartesian coordinate system. */ CARTESIAN("cartesian"), 
      /** Cartesian coordinate system with a logarithmic X-axis. */ SEMILOGX("semilogX"), 
      /** Cartesian coordinate system with a logarithmic Y-axis. */ SEMILOGY("semilogY"),
      /** Cartesian coordinate system in which both axes are logarithmic. */ LOGLOG("loglog"), 
      /** A polar coordinate system (X = angular or theta coordinate, Y = radial coordinate). */ POLAR("polar"),
      /** A polar coordinate system in which the radial axis is logarithmic. */ SEMILOGR("semilogR"),
      /** A 3D Cartesian coordinate system. */ THREED("3D");

      private String tag;
      
      CoordSys(String tag) { this.tag = tag; }
      @Override public String toString() { return(tag); }
      
      public boolean isLogX() { return(this == SEMILOGX || this == LOGLOG); }
      public boolean isLogY() { return(this == SEMILOGY || this == LOGLOG); }
   }

   /** Get the type of coordinate system in which data traces are currently rendered within this graph container. */
   public abstract CoordSys getCoordSys();

   /**
    * Is this graph currently configured as a polar plot? This will be the case if the current coordinate system is
    * either {@link CoordSys#POLAR} or {@link CoordSys#SEMILOGR}.
    * @return True if current coordinate system is polar.
    */
   public final boolean isPolar() 
   {
      CoordSys sys = getCoordSys();
      return(sys == CoordSys.POLAR || sys == CoordSys.SEMILOGR);
   }
   
   /**
    * Is this a 3D graph container?
    * @return True only for a 3D graph container. Otherwise, graph is 2D.
    */
   public final boolean is3D() { return(getCoordSys() == CoordSys.THREED); }
   
   /** 
    * Is the specified axis of this graph container configured for auto-scaling when the data present in the graph is 
    * altered in some way?
    * @param axis An axis component of this graph.
    * @return True if auto-scaling is enabled for the specified axis. Returns false if the specified axis is not a 
    * component of this graph.
    */
   abstract boolean isAxisAutoranged(FGNGraphAxis axis);
   
   /**
    * Automatically fix the range of the axes for this graph so that all plottable data and functions are visbile
    * in the graph, skipping any axis for which the auto-ranging feature is disabled. This method is central to the 
    * auto-ranging feature; it must be invoked whenever a {@link FGNPlottable} node is added or removed, or whenever 
    * that node's displayed data range is altered for any reason (the underlying data set or function itself is 
    * modified; or a node property that affects the data range is modified).
    * 
    * <p>If auto-ranging is currently disabled for all graph axes, this method takes no action. Otherwise, it traverses 
    * all child <b>FGNPlottable</b>s to calculate the overall min/max range needed along each auto-ranged axis to 
    * "show" all the data and function samples along that axis. The axis range is then set accordingly. Axes for which
    * auto-ranging is disabled are not touched. If a change in any axis range occurs (it's possible that the range could
    * be unchanged on any given invocation), then the method triggers a re-rendering of the graph and all children.</p>
    * 
    * @return True if the range of at least one axis was altered, in which case the caller need not trigger a refresh 
    * since this method will have already done so! Returns false otherwise.
    */
   abstract boolean autoScaleAxes();

   
   /**
    * Retrieve a list of all plottable traces currently displayed in this graph. A "plottable trace" is any graphic 
    * node implementing the <b>FGNPlottable</b> interface.
    * 
    * @return The list of children that represent plottable nodes, in the order that they appear in this polar graph's
    * child list (thus preserving Z-order)..
    */
   public List<FGNPlottable> getPlottableNodes()
   {
      List<FGNPlottable> plottables = new ArrayList<FGNPlottable>(5);
      for(int i=0; i < getChildCount(); i++ )
      {
         FGraphicNode n = getChildAt(i);
         if(n instanceof FGNPlottable)
            plottables.add( (FGNPlottable) n );
      }
      return(plottables);
   }

   /**
    * Get an existing data presentation node in this graph that can render the specified data set AND that currently 
    * houses another dataset with the same ID.
    * @param ds A dataset. 
    * @return The matching data presentation node found that's an immediate child of this graph (the method does NOT
    * search child graphs), or null if no match was found.
    */
   public FGNPlottableData findMatchingDataNode(DataSet ds)
   {
      if(ds == null) return(null);
      List<FGNPlottable> plottables = getPlottableNodes();
      FGNPlottableData match = null;
      for(FGNPlottable plottable : plottables) if(plottable instanceof FGNPlottableData)
      {
         FGNPlottableData dataNode = (FGNPlottableData) plottable;
         if(dataNode.isSupportedDataFormat(ds.getFormat()) && dataNode.getDataSetID().equals(ds.getID()))
         {
            match = dataNode;
            break;
         }
      }
      return(match);
   }
   
   /**
    * Whenever one of the data-to-color mapping properties of the graph's color bar is modified, this method is called
    * to update all affected plots in the graph accordingly. If any dirty regions were accumulated as a result of this
    * process, the graphic model is re-rendered in those regions.
    */
   void onColorMapChange()
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      List<Rectangle2D> dirtyAreas = new ArrayList<Rectangle2D>();
      for(FGNPlottable plottable : getPlottableNodes()) if(plottable.onColorMapChange())
         dirtyAreas.add(plottable.getCachedGlobalShape().getBounds2D());

      if(dirtyAreas.size() > 0) model.onChange(this, -1, true, dirtyAreas);
   }
   


   //
   // Focusable/Renderable
   //
   
   /** 
    * The resize outline for a graph container is its bounding box rectangle, as defined by its location and dimensions.
    * It is likely that portions of the graph's rendering (such as axes, color bar, legend) will appear beyond the 
    * confines of this rectangle.
    */
   @Override Rectangle2D getResizeRectLocal()
   {
      Rectangle2D rData = getBoundingBoxLocal();
      return(rData.isEmpty() ? null : rData);
   }

   /**
    * During an interactive drag resize operation, the resize outline includes two rectangles: the graph's defined
    * bounding box, and the typically larger rectangle bounding all rendered content of the graph. The second rectangle 
    * is included only if the cached render bounds are available when the method is invoked, and the size change in this
    * rectangle may not accurately reflect the change in the rendered bounds of the graph upon resize.
    * 
    * <p>This implementation should suffice for the 2D graph containers. Any 3D graph container will override this
    * implementation, since the graph data window is the 2D projection of a 3D box.</p>
    */
   @Override public Shape getDragResizeShape(ResizeAnchor anchor, Point2D p0, Point2D p1, StringBuffer cueBuf)
   {
      // base class implementation prepares the resized data window and the cue.
      Shape s = super.getDragResizeShape(anchor, p0, p1, cueBuf);
      if(s == null || cachedLocalBounds == null || cachedLocalBounds.isEmpty()) return(s);
      
      // if the cached render bounds are available, we wish to expand/contract those bounds IAW the current drag state
      // and add the resulting rectangle to the dynamic drag shape. We take advantage of the fact that the base class
      // method already transformed the anchor and drag points.
      double dx = p1.getX() - p0.getX();
      double dy = p1.getY() - p0.getY();
      
      Rectangle2D rData = getBoundingBoxLocal();
      
      // local coordinates for the edges of the graph data box, which is the key part of the resize outline
      double left = rData.getX();
      double bot = rData.getY();
      double right = left + rData.getWidth();
      double top = bot + rData.getHeight();
      double w = right-left;
      double h = top-bot;
      
      // restrict changes based on anchor. For edge anchors, only that edge can change. For corner anchors, the two
      // edges can change. But, in all cases, don't let the edge move to within 10% of the opposing edge.
      switch(anchor)
      {
      case NORTH: 
         top += dy;
         top = Math.max(top, bot + 0.1*h);
         break;
      case SOUTH:
         bot += dy;
         bot = Math.min(bot,  top - 0.1*h);
         break;
      case EAST:
         right += dx;
         right = Math.max(right,  left + 0.1*w);
         break;
      case WEST:
         left += dx;
         left = Math.min(left,  right - 0.1*w);
         break;
      case NORTHWEST:
         top += dy;
         top = Math.max(top, bot + 0.1*h);
         left += dx;
         left = Math.min(left, right - 0.1*w);
         break;
      case NORTHEAST:
         top += dy;
         top = Math.max(top, bot + 0.1*h);
         right += dx;
         right = Math.max(right,  left + 0.1*w);
         break;
      case SOUTHWEST:
         bot += dy;
         bot = Math.min(bot, top - 0.1*h);
         left += dx;
         left = Math.min(left, right - 0.1*w);
         break;
      case SOUTHEAST:
      case ANY:
         bot += dy;
         bot = Math.min(bot,  top - 0.1*h);
         right += dx;
         right = Math.max(right,  left + 0.1*w);
         break;
      }
      
      // make the commensurate changes in the render bounds rectangle. For consistency with the base class 
      // implementation of the node's "focus shape", the render bounds are expanded outward by 25mi on all sides as 
      // well as making the drag-resize adjustment.
      Rectangle2D rBounds = new Rectangle2D.Double(
            cachedLocalBounds.getX() - 25 + left - rData.getX(),
            cachedLocalBounds.getY() - 25 + bot - rData.getY(),
            cachedLocalBounds.getWidth() + 50 + right - left - rData.getWidth(),
            cachedLocalBounds.getHeight() + 50 + top - bot - rData.getHeight()
            );
      
      // prepare the composite shape, transformed to global figure coordinates
      GeneralPath gp = new GeneralPath(s);
      gp.append(getLocalToGlobalTransform().createTransformedShape(rBounds), false);
      return(gp);
   }


   /** This method is a no-op, as no rendering resources are allocated. */
   @Override protected void releaseRenderResourcesForSelf() {}

   /**
    * This implementation returns the graph container's bounding box, which should be the minimum rectangle for focus 
    * shape and hit test calculations. The rectangle bounding the graph's semi-automated title is also included, if
    * that title is visible.
    */
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      Rectangle2D r = getBoundingBoxLocal();
      TextBoxPainter p = prepareAutoTitlePainter();
      if(p != null)
      {
         p.updateFontRenderContext(g2d);
         p.invalidateBounds();
         r.add(p.getBounds2D(null));
      }
      return(r);
   }


   /** A graph is always rendered, unless its bounding box has zero area. */
   @Override protected boolean isRendered()
   {
      Rectangle2D rBox = getBoundingBoxLocal();
      return(rBox.getWidth() > 0 && rBox.getHeight() > 0);
   }
   
   /**
    * Render the semi-automated title for this graph either above or below the graph's bounding box IAW its current
    * state.
    * <p><b>FGNGraph</b> manages the relevant attributes controlling the visibility, positioning, and alignment of the
    * graph's semi-automated title, and this helper method uses a {@link TextBoxPainter} to actually render the title.
    * It is intended to be called in an implementing subclass during a render cycle. It should be called after all of
    * graph's children have been rendered; if graphed data is clipped to the bounding box, be sure to remove that clip
    * path before calling this method -- or the title won't be included.</p>
    * 
    * see {@link #prepareAutoTitlePainter()}.</p
    * @param g2d The graphics context in which to draw.
    * @param task The rendering task in progress
    * @return True if successful, false if an error occurred while rendering or the rendering task was cancelled.
    * @see {@link #prepareAutoTitlePainter()}
    */
   protected boolean renderAutoTitle(Graphics2D g2d, RenderTask task)
   {
      TextBoxPainter p = prepareAutoTitlePainter();
      if(p != null) { if(!p.render(g2d, task)) return(false); }
      return(true);
   }
   
   /** Line height when semi-automated title has multiple lines (multiple of font size). */
   protected final static double LINEHT_FOR_TITLE = 1.3;
   
   /** 
    * Prepare the {@link TextBoxPainter} that renders this graph's semi-automated title IAW the current state. 
    * 
    * <p>The text block width is set to the graph container's width plus 2x {@link #getTitleGap()}, while the height
    * is set large enough to accommodate three text lines. The block is positioned above or below the graph's 
    * bounding box, depending on {@link #isTitleAboveGraph()}, and the title text is clipped to the box boundaries. The
    * text is aligned to the top or bottom of the text block if the block is below or above the graph, respectively;
    * and {@link #getTitleHorizontalAlignment()} sets the horizontal alignment within the block.</p>
    * <p><i>Note that this abstract base class does not do any rendering. This method is intended for use by the 
    * subclasses that implement the different types of graph containers.</i></p>
    * 
    * @return The painter, ready for rendering. Returns null if the title is not rendered.
    */
   protected TextBoxPainter prepareAutoTitlePainter()
   {
      if(!isTitleRendered()) return(null);
      
      // the title's bounding box is located above or below the graph's bounding box, on the opposite edge from the
      // x-axis. The box is tall enough to allow for 3 lines of text, which should be more than plenty!
      Rectangle2D rBox = getBoundingBoxLocal();
      double gap = getTitleGap().toMilliInches();
      double hMI = getFontSize() * LINEHT_FOR_TITLE * 3.0 + gap * 2.0;
      boolean above = isTitleAboveGraph();
      Point2D botL = new Point2D.Double(-gap, above ? rBox.getHeight() : -hMI);
      
      TextBoxPainter p = new TextBoxPainter();
      p.setStyle(this);
      p.setBorderStroked(false);
      p.setBoundingBox(botL, rBox.getWidth() + 2*gap, hMI, gap);
      p.setClipped(true);
      p.setText(fromStyledText(getTitle(), getFontFamily(), getFontSizeInPoints(), getFillColor(), 
            getFontStyle(), true));
      p.setAlignment(getTitleHorizontalAlignment(), above ? TextAlign.TRAILING : TextAlign.LEADING);
      p.setLineHeight(LINEHT_FOR_TITLE);
      return(p);
   }
   
   /**
    * Render this graph's semi-automated title to Postscript IAW its current state.
    * <p><b>FGNGraph</b> manages the relevant attributes controlling the visibility, positioning, and alignment of the
    * graph's semi-automated title, and this helper method appends the Postscript code to actually render the title.
    * It is intended to be called in an implementing subclass in {@link #toPostscript(PSDoc)}. It should be called after
    * all of graph's children have been rendered; if graphed data is clipped to the bounding box, be sure to remove that
    * clip path before calling this method -- or the title won't be included.</p>
    * 
    * @param psDoc The Postscript document being prepared.
    */
   protected void autoTitleToPostscript(PSDoc psDoc)
   {
      if(isTitleRendered())
      {
         Rectangle2D rBox = getBoundingBoxLocal();
         double wMI = rBox.getWidth();
         double gap = getTitleGap().toMilliInches();
         double hMI = getFontSize() * LINEHT_FOR_TITLE * 3.0;
         boolean above = isTitleAboveGraph();
         Point2D botL = new Point2D.Double(0, above ? rBox.getHeight() + gap : -hMI-gap);

         psDoc.setViewport(botL, wMI, hMI, 0, true);
         String content = getTitle().trim();
         psDoc.renderTextInBox(content, wMI, hMI, LINEHT_FOR_TITLE, 0, getTitleHorizontalAlignment(), 
               above ? TextAlign.TRAILING : TextAlign.LEADING);
      }
   }
}
