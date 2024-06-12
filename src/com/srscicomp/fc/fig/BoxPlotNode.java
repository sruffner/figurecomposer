package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.srscicomp.common.g2dutil.LineSegmentPainter;
import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.Painter;
import com.srscicomp.common.g2dutil.PolylinePainter;
import com.srscicomp.common.g2dutil.PolylinePainter.ConnectPolicy;
import com.srscicomp.common.g2dutil.ShapePainter;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;


/**
 * <b>BoxPlotNode</b> is a 2D data presentation element implementing the "box and whisker diagram", which depicts the
 * variation/distribution in a sample set, typically a set of repeated measurements of some stochastic phenomenon. The 
 * "box" portion of the diagram spans from the <i>first quartile (Q1)</i> to the <i>third quartile (Q3)</i>, while a 
 * line through the box marks the <i>median</i>. Error bar-like "whiskers" extend to the minimum and maximum of the 
 * sample set, excluding any outliers. An <i>outlier</i> is defined as any data value D such that: 
 * <pre>D < Q1 - 1.5*IQR or D > Q3 + 1.5*IQR,</pre> where <i>IQR = Q3-Q1</i> is the <i>interquartile range</i>. 
 * 
 * <p>The only supported data format for the box plot is the raster collection format, {@link DataSet#RASTER1D}. Each
 * raster in the collection is treated as a separate sample set, and a box  plot is rendered for each such set, in 
 * accordance with the node's properties. The box plot corresponding to the <i>i</i>th raster in the collection will be 
 * centered at <i>X(i) = x0 + (i-1)*dx (i=1..#rasters)</i>, where <i>x0</i> and <i>dx</i> are the "offset" and 
 * "interval" properties of the box plot node. Each box is filled and stroked IAW the node's graphic styles, while a 
 * subordinate {@link SymbolNode} controls the appearance of outlier markers and a subordinate {@link ErrorBarNode} 
 * governs the appearance of the whiskers.</p>
 * 
 * <p>The box plot may be oriented vertically or horizontally, with a line through the box marking the location of the
 * median value. The width of the box may be specified as a physical measurement, or as a percentage of the graph's 
 * width (if oriented vertically) or height (if oriented horizontally).</p>
 * 
 * <p>In addition, an optional "notch" in the box spans from the median to <pre>+/-1.57*IQR/sqrt(N),</pre> 
 * where <i>N</i> is the sample size. It represents the 95% confidence interval in the median. When comparing two box
 * plots side by side, if the notch regions do not overlap, then the medians are significantly different, at a 5%
 * significance level. To emphasize the notch region, only that region is filled IAW the box plot's fill color; without
 * the notch, the entire box is filled.</p>
 * 
 * <p>As of FC v5.4.5, each box plot may include the companion "violin plot", a two-sided symmetrical plot of the
 * underlying sample set's kernel density estimate (using a Gaussian kernel and assuming that the true PDF for the data
 * is Gaussian). A separate component node, {@link ViolinStyleNode}, governs the appearance and visibility of the
 * violin plot. By default, the violin plot's size is 0 -- meaning the violin plot is not drawn. Since the box, whisker,
 * outliers and violin are all separately styled, you can use <b>BoxPlotNode</b> to render any one or all of these
 * plot elements.</p>
 * 
 * <p><b>NOTE</b>: The box plot is intended only for use in a 2D Cartesian graph with linear X and Y axes. In any other
 * graph context, it will not be rendered.</p>
 * @author sruffner
 */
public class BoxPlotNode extends FGNPlottableData implements Cloneable
{
   /**
    * Construct a box plot node initially configured to display an empty raster collection in the "vertical" display 
    * mode. It will make no marks in the parent graph. The box width defaults to 10%, the plot offset to 0, and the plot
    * separation interval to 1. All styling attributes are initially implicit (inherited) on the box plot node and its
    * component nodes (governing the appearance of outlier symbols, the whiskers, and a companion violin plot) -- 
    * except that the box plot node's initial fill color is gray rather than black. The error bar component node is 
    * configured so that whiskers are NOT hidden, the symbol component node size is initially 0.05in, and the violin
    * plot style component is configured so that the violin plot is not drawn.
    */
   public BoxPlotNode()
   {
      super(HASSTROKEATTRS|HASSTRKPATNATTR|HASFILLCATTR|HASTITLEATTR|ALLOWATTRINTITLE);
      setTitle("");
      mode = DisplayMode.VERTICAL;
      boxWidth = new Measure(10, Measure.Unit.PCT);
      offset = 0f;
      interval = 1.0f;
      
      // override certain default attribute values for better initial appearance: gray fill color; whiskers NOT hidden;
      // outlier symbol size is 0.05in rather than 0.
      setFillColor(Color.lightGray);
      ErrorBarNode ebar = new ErrorBarNode();
      ebar.setHide(false);
      SymbolNode symbol = new SymbolNode();
      symbol.setSize(new Measure(0.05, Measure.Unit.IN));
      
      addComponentNode( symbol );
      addComponentNode( ebar );
      addComponentNode( new ViolinStyleNode() );
   }

   //
   // Support for child nodes -- none permitted!
   //
   
   @Override public FGNodeType getNodeType() { return(FGNodeType.BOX); }


   //
   // Properties
   //

   /** An enumeration of the different data display modes supported by {@link BoxPlotNode}. */
   public enum DisplayMode
   {
      /** Box plot is oriented parallel to Y-axis of parent graph, without a notch marking the median value. */
      VERTICAL, 
      /** Box plot is oriented parallel to Y-axis of parent graph, with a notch marking the median value. */
      VERTNOTCH,
      /** Box plot is oriented parallel to X-axis of parent graph, without a notch marking the median value. */
      HORIZONTAL,
      /** Box plot is oriented parallel to X-axis of parent graph, with a notch marking the median value. */
      HORIZNOTCH; 
      
      @Override public String toString() { return(super.toString().toLowerCase()); }
   }

   /** The current box plot display mode. */
   private DisplayMode mode;

   /**
    * Get the current display mode for this box plot node.
    * @return The current data display mode.
    */
   public DisplayMode getMode() { return(mode); }

   /**
    * Set the display mode for this box plot node. If a change is made, {@link #onNodeModified()} is invoked.
    * @param mode The new display mode. Null is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setMode(DisplayMode mode)
   {
      if(mode == null) return(false);
      if(this.mode != mode)
      {
         if(doMultiNodeEdit(FGNProperty.MODE, mode)) return(true);
         
         DisplayMode oldMode = this.mode;
         this.mode = mode;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.MODE);
            FGNRevEdit.post(this, FGNProperty.MODE, mode, oldMode);
         }
      }
      return(true);
   }

   /** 
    * Is the box plot oriented vertically, ie, parallel to the Y-axis of the parent graph?
    * 
    * @return True for the vertical display modes; false for the horizontal display modes.
    */
   private boolean isVertical() { return(mode==DisplayMode.VERTICAL || mode==DisplayMode.VERTNOTCH); }
   
   /**
    * Is the box plot notched to mark the location of the median in the underlying data set?
    * @return True for the notched display mode; false otherwise.
    */
   private boolean isNotched() { return(mode==DisplayMode.VERTNOTCH || mode==DisplayMode.HORIZNOTCH); }
   
   /** Box plot location offset, in user units. */
   private float offset;
   
   /**
    * Get the box plot offset.
    * 
    * <p>When the underlying raster collection contains multiple rasters, a separate box plot is drawn for each 
    * individual raster (aka, sample set). The i-th raster is centered at <i>X(i) = X0 + i*DX</i>, where <i>X0</i> 
    * is the location offset and <i>DX</i> is the plot separation interval.</p>
    * 
    * @return The plot offset, in user units.
    */
   public float getOffset() { return(offset); }
   
   /**
    * Set the box plot offset. If a change is made, {@link #onNodeModified()} is invoked.
    * @see {@link #getOffset()}
    * @param ofs The new plot offset, in user units. NaN and +/-infinity are rejected.
    * @return True if new value was accepted.
    */
   public boolean setOffset(float ofs) 
   {
      if(!Utilities.isWellDefined(ofs)) return(false);
      if(offset != ofs)
      {
         if(doMultiNodeEdit(FGNProperty.X0, new Float(ofs))) return(true);
         
         Float old = new Float(offset);
         offset = ofs;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.X0);
            FGNRevEdit.post(this, FGNProperty.X0, new Float(offset), old);
         }
      }
      return(true);
   }

   /** Box plot separation interval, in user units. */
   private float interval;

   /**
    * Get the interval separating the midpoints of adjacent box plots.
    * @see {@link #getOffset()}
    * @return The plot separation interval, in user units.
    */
   public float getInterval() { return(interval); }
    
   /**
    * Set the interval separating adjacent box plots. If a change is made, {@link #onNodeModified()} is invoked.
    * @see {@link #getOffset()}
    * @param sep The new separation interval, in user units. NaN and +/-infinity are rejected. Note that 0 is allowed,
    * but then the individual box plots will overlap entirely.
    * @return True if new value was accepted.
    */
   public boolean setInterval(float intv) 
   {
      if(!Utilities.isWellDefined(intv)) return(false);
      if(interval != intv)
      {
         if(doMultiNodeEdit(FGNProperty.DX, new Float(intv))) return(true);
         
         Float old = new Float(interval);
         interval = intv;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.DX);
            FGNRevEdit.post(this, FGNProperty.DX, new Float(interval), old);
         }
      }
      return(true);
   }

   /** 
    * Width of the box portion of the box plot, measured in physical units or as a percentage of the parent graph's
    * width (for vertical orientation) or height (for horizontal orientation).
    */
   private Measure boxWidth;
   
   /**
    * Constraints on the width of box plot: cannot be in "user" units; limited to 4 significant and 3 fractional digits;
    * must be nonnegative; and maximum allowed size is 1in.
    */
   public final static Measure.Constraints BOXWIDTHCONSTRAINTS = 
         new Measure.Constraints(0, 300, 0, 1000.0, 4, 3, true, false);


   /**
    * Get the width of the box portion of the box plot.
    * 
    * @return The box width measured in physical units (in/mm/cm/pt) or specified as a percentage of the parent graph's
    * width (vertical orientation) or height (horizontal orientation).
    */
   public Measure getBoxWidth() { return(boxWidth); }

   /**
    * Set the width of the box portion of the box plot. If a change occurs, {@link #onNodeModified()} is invoked.
    * 
    * @param m The new box width. A null value is rejected, as is a measure in "user units". Otherwise, the measure is
    * constrained by {@link #BOXWIDTHCONSTRAINTS}. 
    * @return True if successful; false if value was rejected.
    */
   public boolean setBoxWidth(Measure m)
   {
      if((m == null) || (m.getUnits() == Measure.Unit.USER)) return(false);
      
      m = BOXWIDTHCONSTRAINTS.constrain(m);

      if((boxWidth != m) && !Measure.equal(boxWidth, m))
      {
         if(doMultiNodeEdit(FGNProperty.BARWIDTH, m)) return(true);

         Measure oldW = boxWidth;
         boxWidth = m;
         
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.BARWIDTH);
            FGNRevEdit.post(this, FGNProperty.BARWIDTH, boxWidth, oldW, "Change box plot width");
         }
      }
      return(true);
   }


   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
         case MODE : ok = setMode((DisplayMode)propValue); break;
         case X0: ok = setOffset((Float)propValue); break;
         case DX: ok = setInterval((Float)propValue); break;
         case BARWIDTH: ok = setBoxWidth((Measure)propValue); break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
         case MODE : value = getMode(); break;
         case X0: value = new Float(getOffset()); break;
         case DX: value = new Float(getInterval()); break;
         case BARWIDTH: value = getBoxWidth(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }


   //
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   /**
    * The node-specific properties exported in a box plot node's style set include the display mode, box width, and the
    * include-in-legend flag. The plot offset and separation interval are not included.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.MODE, getMode());
      styleSet.putStyle(FGNProperty.BARWIDTH, getBoxWidth());
      styleSet.putStyle(FGNProperty.LEGEND, new Boolean(getShowInLegend()));
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      DisplayMode dm = (DisplayMode) applied.getCheckedStyle(FGNProperty.MODE, getNodeType(), DisplayMode.class);
      if(dm != null && !dm.equals(restore.getStyle(FGNProperty.MODE)))
      {
         mode = dm;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.MODE);
      
      Measure m = (Measure) applied.getCheckedStyle(FGNProperty.BARWIDTH, getNodeType(), Measure.class);
      if(m != null) m = BOXWIDTHCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.BARWIDTH, null, Measure.class)))
      {
         boxWidth = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.BARWIDTH);
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.LEGEND, null, Boolean.class);
      if(b != null && !b.equals(restore.getStyle(FGNProperty.LEGEND)))
      {
         setShowInLegendNoNotify(b);
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LEGEND);
            
      return(changed);
   }


   // 
   // FGNPlottableData, FGNPlottable
   //
   
   /**
    * Initializes the box plot node's data set to contain some sample data. It creates a {@link Fmt#RASTER1D} with two
    * rasters of 50 random samples each. The samples are chosen so that all lie in the specified Y-axis range, and the
    * node's offset and plot separation intervals are set so that the two box plots appear roughly at 1/3 and 2/3 along 
    * the X-axis.
    * 
    * <p>The <i>isPolar</i> argument is ignored, since the box plot will not be rendered in a polar graph context.</p>
    */
   @Override protected void initializeDefaultData(double[] axisRng, boolean isPolar)
   {
      float x0 = (float) axisRng[0];
      float x1 = (float) axisRng[1];
      float y0 = (float) axisRng[2];
      float y1 = (float) axisRng[3];
      
      offset = (x1-x0)/3.0f;
      interval = offset;
      
      float yMin = y0 + (y1-y0)*0.1f;
      float ySpan = (y1-y0)*0.9f;
      
      float[] fData = new float[2 + 2*50];
      fData[0] = fData[1] = 50;
      for(int i=0; i<50; i++)
      {
         fData[2+i] = yMin + 0.3f*ySpan*((float) Math.random());
         fData[52+i] = yMin + 0.7f*ySpan*((float) Math.random());
      }
      
      DataSet ds = DataSet.createDataSet(getDataSet().getID(), Fmt.RASTER1D, null, 2*50, 2, fData);
      if(ds != null) setDataSet(ds);
   }

   @Override protected boolean recalcDataRange(Object hint)
   {
      boolean needRecalc = hint==null || 
            hint==FGNProperty.SRC || hint==FGNProperty.X0 || hint==FGNProperty.DX || hint==FGNProperty.MODE;
      if(!needRecalc) return(false);
      
      // under various circumstances, we have to recompute the cached box plot statistics
      if(boxPlotStats == null || hint==null || hint==FGNProperty.SRC || hint==FGNProperty.X0 || hint==FGNProperty.DX) 
         computeBoxPlotStats();
      // if the display mode changes, we have to rebuild the vertex lists
      if(hint == null || hint == FGNProperty.MODE)
         boxVertices.clear();
      
      float minX = Float.POSITIVE_INFINITY;
      float maxX = Float.NEGATIVE_INFINITY;
      float minY = Float.POSITIVE_INFINITY;
      float maxY = Float.NEGATIVE_INFINITY;

      // from the current set of box plot stats, recompute data range.... NOTE that we do NOT account for the width
      // of the individual box plots.
      if(boxPlotStats != null) for(int i=0; i<boxPlotStats.size(); i++)
      {
         BoxStats bs = boxPlotStats.get(i);
         if(bs.sampleSize <= 0) continue;
         
         minX = Math.min(minX, bs.xCtr);
         maxX = Math.max(maxX, bs.xCtr);
         
         minY = Math.min(minY, bs.min);
         if(bs.outliers != null && bs.outliers.length > 0) minY = Math.min(minY, bs.outliers[0]);
         maxY = Math.max(maxY, bs.max);
         if(bs.outliers != null && bs.outliers.length > 0) maxY = Math.max(maxY, bs.outliers[bs.outliers.length-1]);
      }
      
      // if in one of the horizontal modes, transpose X and Y
      if(!isVertical())
      {
         float tmp = minX; minX = minY; minY = tmp;
         tmp = maxX; maxX = maxY; maxY = tmp;
      }
      
      // if there's no data, the data range is undefined -- so we just set the extrema to all zeros
      if(!Utilities.isWellDefined(minX))
         minX = maxX = minY = maxY = 0f;
      
      boolean changed = (cachedDataRange[0] != minX) || (cachedDataRange[1] != maxX);
      changed = changed || (cachedDataRange[2] != minY) || (cachedDataRange[3] != maxY);
      if(changed)
      {
         cachedDataRange[0] = minX;
         cachedDataRange[1] = maxX;
         cachedDataRange[2] = minY;
         cachedDataRange[3] = maxY;
      }
      
      return(changed);
   }

   /**  A box plot supports only 1D raster data collections as a data source. */
   @Override public boolean isSupportedDataFormat(Fmt fmt) { return(fmt == Fmt.RASTER1D); }
   @Override public Fmt[] getSupportedDataFormats() { return(new Fmt[] {Fmt.RASTER1D}); }
   
   
   @Override public SymbolNode getSymbolNode() { return((SymbolNode) getComponentNodeAt(0)); }
   @Override public boolean usesSymbols() { return(true); }
   @Override public boolean hasErrorData() { return(true); }
   @Override public boolean useBarInLegend()  { return(true); }
   @Override public boolean hasDataGroups() { return(false); }
   
   /**
    * Retrieve the component "error bar" node that governs the appearance of the whiskers in this box plot.
    * 
    * @return The error bar info.
    */
   public ErrorBarNode getWhiskerNode() { return((ErrorBarNode)getComponentNodeAt(1)); }
   
   /**
    * Are the whiskers in this box plot currently hidden? This will be the case if: 
    * <ul>
    *    <li>There are no box plots to render.</li>
    *    <li>The compponent node governing the appearance of the whiskers is defined such that the whiskers are
    *    neither stroked nor their endcaps filled.</li>
    * </ul>
    * 
    * @return True if no whiskers are rendered in this box plot.
    */
   public boolean areWhiskersHidden()
   {
      // are there any whiskers to draw?
      boolean foundWhiskers = false;
      if(boxPlotStats != null) for(int i=0; (!foundWhiskers) && i<boxPlotStats.size(); i++)
      {
         BoxStats bs = boxPlotStats.get(i);
         foundWhiskers = (bs.sampleSize > 2) && (bs.min != bs.q1 || bs.max != bs.q3);
      }
      if(!foundWhiskers) return(true);

      ErrorBarNode ebar = getWhiskerNode();
      if(ebar.getHide()) return(true);
      
      boolean noEndCaps = (ebar.getEndCapSizeInMilliInches() <= 0);
      boolean noClosedCaps = noEndCaps || !ebar.getEndCap().isClosed();
      return((!ebar.isStroked()) && (noClosedCaps || ebar.getFillColor().getAlpha() == 0));
   }

   /** 
    * Retrieve the component node that controls the appearance/visibility of a violin plot (kernel density estimate)
    * associated with the box plot.
    * 
    * @return The violin plot style node.
    */
   public ViolinStyleNode getViolinStyleNode() { return((ViolinStyleNode) getComponentNodeAt(2)); }
   
   
   //
   // Focusable/Renderable support
   //
   
   
   /**
    * Cached rectangle bounding only the marks made by this box plot node. An empty rectangle indicates that the
    * element makes no marks when "rendered". If null, the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;
   
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null)
      {
         rBoundsSelf = new Rectangle2D.Double();
         FGNGraph g = getParentGraph();
         FViewport2D vp = getParentViewport();
         boolean ok = (vp != null) && (g != null) && (g.getCoordSys() == FGNGraph.CoordSys.CARTESIAN);
         if(ok)
         {
            updatePainters();
            for(Painter p : painters)
            {
               p.updateFontRenderContext(g2d);
               p.invalidateBounds();
               Utilities.rectUnion(rBoundsSelf, p.getBounds2D(null), rBoundsSelf);
            }

            // if parent graph clips its data, then we need to clip the render bounds to the graph's data viewport
            if(g.getClip())
               Rectangle2D.intersect(rBoundsSelf, g.getBoundingBoxLocal(), rBoundsSelf);
         }
      }
      return((Rectangle2D)rBoundsSelf.clone());
   }

   @Override protected void releaseRenderResourcesForSelf() 
   { 
      rBoundsSelf = null; 
      painters.clear();
   }

   /**
    * Render this box plot node into the current graphics context IAW its current display mode. If the parent graph is 
    * not 2D Cartesian with linear X and Y axes, the plot is not rendered.
    */
   @Override public boolean render(Graphics2D g2d, RenderTask task)
   {
      FGNGraph g = getParentGraph();
      FGNGraph.CoordSys coordSys = (g != null) ? g.getCoordSys() : null;
      FViewport2D vp = getParentViewport();
      if((vp == null) || !(needsRendering(task) || coordSys == FGNGraph.CoordSys.CARTESIAN))
         return(true);

      updatePainters();
      for(Painter p : painters) if(!p.render(g2d, task)) return(false);
      
      return(true);
   }

   /** The painters which are responsible for rendering the box plot IAW its current state. */
   private List<Painter> painters = new ArrayList<Painter>();
   
   /** Vertices outlining box portion(s) of the rendered box plot(s). */
   private List<Point2D> boxVertices = new ArrayList<Point2D>();
   /** Vertices outlining notch fill region(s) across the rendered box plot(s). */
   private List<Point2D> notchVertices = new ArrayList<Point2D>();
   /** Vertices defining all whiskers across the rendered box plots. */
   private List<Point2D> whiskerVertices = new ArrayList<Point2D>();
   /** All outlier points across the rendered box plots. */
   private List<Point2D> outlierVertices = new ArrayList<Point2D>();
   
   /**
    * Create/update the list of painters that render this box plot in its current state.
    * 
    * <p>If the painter list is empty, it is initialized with a list of 6 painters, <i>in rendering order</i>:
    * <ul>
    * <li>A {@link PolylinePainter} that renders the associated violin plot (unless plot size is 0).</li>
    * <li>A {@link PolylinePainter} to render the box portion of each box plot in the non-notched display modes.</li>
    * <li>A {@link PolyLinePainter} to fill notch of each box plot in the notched display modes.</li>
    * <li>A {@link PolylinePainter} to stroke the box outline of each box plot in the notched display modes.</li>
    * <li>A {@link LineSegmentPainter} that renders all the box plot whiskers.</li>
    * <li>A {@link ShapePainter} to render any outlier markers.</li>
    * </ul>
    * </p>
    * 
    * <p>On each call, a list of vertices is prepared for each painter IAW the current box plot statistics. If a painter
    * is not applicable in the current state -- for example, if whiskers are hidden, no outliers exist, etc -- then the
    * list of vertices for that painter will be empty.</p>
    */
   private void updatePainters()
   {
      if(painters.size() == 0)
      {
         // draws the associated violin plot
         PolylinePainter pp = new PolylinePainter(getViolinStyleNode(), null);
         pp.setConnectionPolicy(ConnectPolicy.CLOSED);
         pp.setFilled(true);
         painters.add(pp);
         
         // strokes and fills box portion in non-notched modes
         pp = new PolylinePainter(this, null);
         pp.setConnectionPolicy(ConnectPolicy.CLOSED);
         pp.setFilled(true);
         painters.add(pp);
         
         // fills but does not stroke notch region in notched modes
         pp = new PolylinePainter(this, null);
         pp.setConnectionPolicy(ConnectPolicy.CLOSED);
         pp.setFilled(true);
         pp.setStroked(false);
         painters.add(pp);
         
         // strokes but does not fill notched box in notched modes
         painters.add(new PolylinePainter(this, null));
         
         // paints all whiskers
         painters.add(new LineSegmentPainter(getWhiskerNode(), null));
         
         // paints all outlier markers
         painters.add(new ShapePainter(getSymbolNode(), null, Marker.CIRCLE, 0f, ""));
      }
      
      // configure all painters IAW current state of box plot node. Need to supply vertices to all painters
      computeBoxVertices();
      
      painters.get(0).setLocationProducer(new ViolinVertexProducer());
      painters.get(1).setLocationProducer(isNotched() ? null : boxVertices);
      painters.get(2).setLocationProducer(isNotched() ? notchVertices : null);
      painters.get(3).setLocationProducer(isNotched() ? boxVertices : null);
      
      LineSegmentPainter lp = (LineSegmentPainter) painters.get(4);
      lp.setLocationProducer(areWhiskersHidden() ? null : whiskerVertices);
      ErrorBarNode whisker = getWhiskerNode();
      lp.setAdornment(whisker, whisker.getEndCap(), (float) whisker.getEndCapSizeInMilliInches(), "", 
            false, true, false, false);
      
      ShapePainter sp = (ShapePainter) painters.get(5);
      sp.setLocationProducer(outlierVertices);
      sp.setPaintedShape(getSymbolNode().getType());
      sp.setSize((float) getSymbolNode().getSizeInMilliInches());
   }

   /**
    * Helper method prepares the vertex lists for each of the painters that together render the box plots defined by 
    * this node.
    */
   private void computeBoxVertices()
   {
      if(boxPlotStats == null) computeBoxPlotStats();
      
      boxVertices.clear();
      notchVertices.clear();
      whiskerVertices.clear();
      outlierVertices.clear();
      
      boolean isV = isVertical();
      boolean notched = isNotched();
      FViewport2D graphVP = getParentViewport(); 
      float halfBoxWidthMI = 0f;
      if(graphVP != null)
      {
         double d = isV ? graphVP.fromMeasureToMilliInX(boxWidth) : graphVP.fromMeasureToMilliInY(boxWidth);
         halfBoxWidthMI = (float) (d/2.0);
      }
      List<Point2D> pts = new ArrayList<Point2D>();
      
      if(graphVP != null) for(BoxStats bs : boxPlotStats)
      {
         // omit degenerate box plots
         if(bs.sampleSize < 3 || bs.q1 == bs.median || bs.median == bs.q3)
            continue;
         
         // unnotched display mode -- simple rectangle for each box plot, plus line segment at median. An ill-defined
         // separates box from line segment and from next box plot (per PolylinePainter spec)
         if(!notched)
         {
            pts.clear();
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.q1, isV ? bs.q1 : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.q3, isV ? bs.q3 : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.q3, isV ? bs.q3 : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.q1, isV ? bs.q1 : bs.xCtr));
            pts.add(new Point2D.Float(Float.NaN, Float.NaN));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.median, isV ? bs.median : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.median, isV ? bs.median : bs.xCtr));
            pts.add(new Point2D.Float(Float.NaN, Float.NaN));
            graphVP.userUnitsToThousandthInches(pts);
            for(int i=0; i<7; i++)
            {
               Point2D p = pts.get(i);
               float delta = halfBoxWidthMI * ((i == 0 || i == 1 || i == 6) ? -1 : 1);
               p.setLocation(p.getX() + (isV ? delta : 0), p.getY() + (isV ? 0 : delta));
            }
            boxVertices.addAll(pts);
         }
         
         // notched display mode -- notch region spans 95% confidence interval around median and is filled but not
         // stroked. The box outline with the notch is an hourglass shape and is stroked but not filled. In each case,
         // one ill-defined point separates the box/notch path for one box plot from the next.
         if(notched)
         {
            float notchHi = bs.median + 1.57f*(bs.q3-bs.q1)/((float) Math.sqrt(bs.sampleSize));
            float notchLo = bs.median - 1.57f*(bs.q3-bs.q1)/((float) Math.sqrt(bs.sampleSize));
            
            pts.clear();
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.median, isV ? bs.median : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : notchHi, isV ? notchHi : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : notchHi, isV ? notchHi : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.median, isV ? bs.median : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : notchLo, isV ? notchLo : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : notchLo, isV ? notchLo : bs.xCtr));
            pts.add(new Point2D.Float(Float.NaN, Float.NaN));
            graphVP.userUnitsToThousandthInches(pts);
            for(int i=0; i<6; i++)
            {
               Point2D p = pts.get(i);
               float delta = halfBoxWidthMI * ((i == 0 || i == 1 || i == 5) ? -1 : 1);
               if(i==0 || i==3) delta *= 0.75f;
               p.setLocation(p.getX() + (isV ? delta : 0), p.getY() + (isV ? 0 : delta));

            }
            notchVertices.addAll(pts);

            pts.clear();
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.median, isV ? bs.median : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.median, isV ? bs.median : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : notchHi, isV ? notchHi : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.q3, isV ? bs.q3 : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.q3, isV ? bs.q3 : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : notchHi, isV ? notchHi : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.median, isV ? bs.median : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : notchLo, isV ? notchLo : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.q1, isV ? bs.q1 : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.q1, isV ? bs.q1 : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : notchLo, isV ? notchLo : bs.xCtr));
            pts.add(new Point2D.Float(isV ? bs.xCtr : bs.median, isV ? bs.median : bs.xCtr));
            pts.add(new Point2D.Float(Float.NaN, Float.NaN));
            graphVP.userUnitsToThousandthInches(pts);
            for(int i=0; i<12; i++)
            {
               Point2D p = pts.get(i);
               float delta = halfBoxWidthMI;
               if(i == 0 || i == 1 || i == 6 || i == 11) delta *= 0.75f;
               if((i > 0 && i < 4) || (i > 8 && i < 12)) delta *= -1f;
               p.setLocation(p.getX() + (isV ? delta : 0), p.getY() + (isV ? 0 : delta));
            }
            boxVertices.addAll(pts);
         }
         
         // a pair of points for the Q1 -> min whisker and a pair for the Q3 -> max whisker
         pts.clear();
         pts.add(new Point2D.Float(isV ? bs.xCtr : bs.q1, isV ? bs.q1 : bs.xCtr));
         pts.add(new Point2D.Float(isV ? bs.xCtr : bs.min, isV ? bs.min : bs.xCtr));
         pts.add(new Point2D.Float(isV ? bs.xCtr : bs.q3, isV ? bs.q3 : bs.xCtr));
         pts.add(new Point2D.Float(isV ? bs.xCtr : bs.max, isV ? bs.max : bs.xCtr));
         graphVP.userUnitsToThousandthInches(pts);
         whiskerVertices.addAll(pts);

         // a point for each outlier
         pts.clear();
         if(bs.outliers != null) for(float outlier : bs.outliers)
         {
            pts.add(new Point2D.Float(isV ? bs.xCtr : outlier, isV ? outlier : bs.xCtr));
         }
         graphVP.userUnitsToThousandthInches(pts);
         outlierVertices.addAll(pts);
      }
   }
   
   
   //
   // PSTransformable
   //
   
   public void toPostscript(PSDoc psDoc)
   {
      // only render in a Cartesian graph
      FGNGraph g = getParentGraph();
      if(g == null || (g.getCoordSys() != FGNGraph.CoordSys.CARTESIAN))
         return;
      
      computeBoxVertices();
      
      psDoc.startElement(this);
      
      // the companion violin plot is drawn first
      ViolinVertexProducer producer = new ViolinVertexProducer();
      List<Point2D> pts = new ArrayList<Point2D>();
      while(producer.hasNext())
      {
         Point2D p = producer.next();   // the producer reuses the Point2D object
         if(!Utilities.isWellDefined(p)) pts.add(null);
         else pts.add(new Point2D.Double(p.getX(), p.getY()));
      }
      if(pts.size() > 0)
      {
         psDoc.startElement(getViolinStyleNode());
         psDoc.renderPolygons(pts, true);
         psDoc.endElement();
      }

      // then the box, whiskers, and outliers...
      if(!isNotched())
      {
         psDoc.renderPolygons(boxVertices, true);
      }
      else
      {
         psDoc.renderPolygons(notchVertices, true, false);
         psDoc.renderPolygons(boxVertices, false);  
      }
      
      if(!areWhiskersHidden())
      {
         ErrorBarNode whisker = getWhiskerNode();
         psDoc.startElement(whisker);
         for(int i=0; i<whiskerVertices.size(); i+=2)
         {
            psDoc.renderAdornedLine(whiskerVertices.get(i), whiskerVertices.get(i+1), false, 
                  null, 0, whisker.getEndCap(), whisker.getEndCapSizeInMilliInches(), null, 0);
         }
         psDoc.endElement();
      }
      
      if(!outlierVertices.isEmpty())
      {
         SymbolNode symbol = getSymbolNode();
         psDoc.startElement(symbol);
         psDoc.renderMultipleAdornments(outlierVertices.toArray(new Point2D[outlierVertices.size()]), null, 
               symbol.getType(), symbol.getSizeInMilliInches(), "");
         psDoc.endElement();
      }
      
      
      psDoc.endElement();
   }
   
   
   // 
   // Object
   //

   /**
    * This override ensures that the rendering infrastructure for the clone is independent of the box plot node cloned. 
    * The clone will reference the same data set, however!
    */
   @Override protected Object clone()
   {
      BoxPlotNode copy = (BoxPlotNode) super.clone();
      copy.rBoundsSelf = null;
      copy.painters = new ArrayList<Painter>();
      copy.boxPlotStats = null;
      copy.boxVertices = new ArrayList<Point2D>();
      copy.notchVertices = new ArrayList<Point2D>();
      copy.whiskerVertices = new ArrayList<Point2D>();
      copy.outlierVertices = new ArrayList<Point2D>();
      return(copy);
   }
   
   
   /** Current box plot statistics for each sample set in this node's data source. If empty, box plot not rendered. */
   private List<BoxStats> boxPlotStats = null;
   
   /** 
    * Helper method computes the current box plot statistics for each sample set in the data source. It also clears
    * the vertex lists for the node's painters, since all vertices must be recomputed whenever the statistics change.
    * @see {@link #computeBoxVerticesIfNecessary()}
    */
   private void computeBoxPlotStats()
   {
      boxPlotStats = new ArrayList<BoxStats>();
      boxVertices.clear();  // if this list is empty, all vertices are recomputed

      DataSet ds = getDataSet();
      if(ds == null || ds.isEmpty() || !isSupportedDataFormat(ds.getFormat()))
         return;
     
      for(int i=0; i<ds.getNumberOfSets(); i++)
      {
         float[] samples = ds.getRasterSamples(i, true);
         boxPlotStats.add(BoxStats.computeBoxPlotStats(offset + i*interval, samples));
      }
   }
   

   /**
    * Helper class computes and encapsulates the five summary statistics of a box plot, plus any outliers.
    */
   private static class BoxStats
   {
      static BoxStats computeBoxPlotStats(float xCtr, float[] samples)
      {
         if(samples == null || samples.length == 0) return(new BoxStats());
         else if(samples.length == 1)
            return(new BoxStats(1, xCtr, samples[0], Float.NaN, Float.NaN, samples[0], samples[0], null));
         
         Arrays.sort(samples);
         if(samples.length == 2)
            return(new BoxStats(2, xCtr, (samples[0]+samples[1])/2.0f, 
                     samples[0], samples[1], samples[0], samples[1], null));

         int mid = samples.length / 2;
         boolean even = (samples.length % 2 == 0);
         float median = even ? ((samples[mid-1] + samples[mid]) / 2.0f) : samples[mid];
         int q1Mid = mid / 2;
         int q3Mid = mid + q1Mid + (even ? 0 : 1);
         float q1, q3;
         if(mid % 2 == 0)
         {
            q1 = (samples[q1Mid-1] + samples[q1Mid]) / 2.0f;
            q3 = (samples[q3Mid-1] + samples[q3Mid]) / 2.0f;
         }
         else
         {
            q1 = samples[q1Mid];
            q3 = samples[q3Mid];
         }
         
         // indices of min/max, excluding outliers
         float lim = q1 - 1.5f*(q3-q1);
         int jMin = 0; while((samples[jMin] < lim) && (jMin < q1Mid)) jMin++;
         lim = q3 + 1.5f*(q3-q1);
         int jMax = samples.length - 1; while((samples[jMax] > lim) && (jMax > q3Mid)) jMax--;
         
         // the outliers
         int nOutliers = jMin + (samples.length - 1 - jMax);
         float[] outliers = null;
         if(nOutliers > 0)
         {
            outliers = new float[nOutliers];
            int n = 0;
            for(int j=0; j<jMin; j++) outliers[n++] = samples[j];
            for(int j=jMax+1; j<samples.length; j++) outliers[n++] = samples[j];
         }
         
         return(new BoxStats(samples.length, xCtr, median, q1, q3, samples[jMin], samples[jMax], outliers));
      }
      
      /** Construct box plot stats for an empty data set (zero samples). */
      private BoxStats()
      {
         this(0, 0, 0, 0, 0, 0, 0, null);
      }
      
      private BoxStats(BoxStats src)
      {
         this(src.sampleSize, src.xCtr, src.median, src.q1, src.q3, src.min, src.max, src.outliers);
      }
      
      private BoxStats(int n, float x, float median, float q1, float q3, float min, float max, float[] outliers)
      {
         this.sampleSize = n;
         this.xCtr = x;
         this.median = median;
         this.q1 = q1;
         this.q3 = q3;
         this.min = min;
         this.max = max;
         this.outliers = outliers;
      }
      
      /** 
       * The number of well-defined samples in the sample set represented by the box plot.  If zero, box plot is not
       * rendered.
       */
      final int sampleSize;
      /** X-coordinate value about which box plot is centered. */
      final float xCtr;
      /** The median value across the sample set. */
      final float median;
      /** The first quartile for the sample set. Undefined if the sample size is 1. */
      final float q1;
      /** The third quartile for the sample set. Undefined if the sample size is 3. */
      final float q3;
      /** The minimum value in sample set that is >= Q1 - 1.5*(Q3-Q1). */
      final float min;
      /** The maximum value in sample set that is <= Q3 + 1.5*(Q3-Q1). */
      final float max;
      /** All outliers in sample set, <b>in ascending order</b>. If null, there are no outliers. */
      final float[] outliers;
   }
   

   /**
    * Helper class generates the sequence of points to outline the violin plot(s) associated with the box plot(s)
    * rendered by the enclosing class.
    * 
    * <p>The violin plot is a plot of the kernel density estimate on both sides of the box plot. The KDE computation
    * assumes a Gaussian kernel evaluated out to 2.5 standard deviations on either side of the kernel center. The KDE
    * is evaluated at 101 evenly spaced points between <b>m-2.4D</b> and <b>M+2.5D</b>, where <b>D</b> is the kernel's
    * standard deviation and <b>m, M</b> are the observed minimum and maximum samples, respectively. D is calculated 
    * using Silverman's rule of thumb, which assumes the actual probability density of the sampled data is Gaussian.</p>
    * 
    * <p>The violin outline is two-sided and mirror-symmetrical. This producer first generates the 101 points from one
    * end to the other, then another 100 points for the "flip side" of the plot. An undefined point is inserted before
    * generating the points for the next violin (if there are more than one rasters in the underlying raster data set).
    * <b>NOTE that no points are generated -- meaning violin plots are not drawn -- if the violin width/height is 0,
    * or if the {@link ViolinStyleNode} that controls the appearance of the violin plots defines an empty stroke and a
    * fully transparent fill.</b></p>
    */
   private class ViolinVertexProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      final DataSet set;
      final FViewport2D graphVP;
      final float ofs;
      final float intv;
      final boolean isVert;
      final double kdeHeightMI;
      
      double[][] kde = null;
      double center;
      Point2D nextVertex;
      int nSetsSoFar;
      int nVertsSoFar;
      boolean fwd;
      
      ViolinVertexProducer()
      {
         set = getDataSet();
         graphVP = getParentViewport();
         ofs = getOffset();
         intv = getInterval();
         isVert = isVertical();
         
         ViolinStyleNode violin = getViolinStyleNode();
         
         double d = 0;
         if(violin.isRendered() && graphVP != null)
            d = isVert ? graphVP.fromMeasureToMilliInX(violin.getSize()) : 
               graphVP.fromMeasureToMilliInY(violin.getSize());
         kdeHeightMI = d / 2;
         
         nextVertex = new Point2D.Double();
         nSetsSoFar = 0;
         nVertsSoFar = -1;
         fwd = true;
      }
      
      @Override public Iterator<Point2D> iterator() { return(new ViolinVertexProducer()); }

      @Override public boolean hasNext()
      {
         return((kdeHeightMI > 0) && (graphVP != null) && (nSetsSoFar < set.getNumberOfSets()));
      }

      @Override public Point2D next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");
         
         if(nVertsSoFar == -1)
         {
            if(!fwd)
            {
               nextVertex.setLocation(Double.NaN, Double.NaN);
               ++nSetsSoFar;
               fwd = true;
               return(nextVertex);
            }
            
            float[] samples = set.getRasterSamples(nSetsSoFar, true);
            center = ofs + nSetsSoFar*intv;
            BoxStats stats = BoxStats.computeBoxPlotStats((float)center, samples);
            kde = computeKDE(samples, stats);
            if(kde == null)
            {
               nextVertex.setLocation(Double.NaN, Double.NaN);
               ++nSetsSoFar;
               return(nextVertex);
            }
            nVertsSoFar = 0;
            fwd = true;
         }
         
         nextVertex.setLocation(isVert ? center : kde[nVertsSoFar][0], isVert ? kde[nVertsSoFar][0] : center);
         graphVP.userUnitsToThousandthInches(nextVertex);
         double kdeOffset = (fwd ? -1 : 1) * kdeHeightMI * kde[nVertsSoFar][1];
         nextVertex.setLocation(nextVertex.getX() + (isVert ? kdeOffset : 0), 
               nextVertex.getY() + (isVert ? 0 : kdeOffset));
         nVertsSoFar += fwd ? 1 : -1;
         if(fwd && nVertsSoFar == kde.length)
         {
            nVertsSoFar = kde.length -1;
            fwd = false;
         }

         return(nextVertex);
      }
      
      private double[][] computeKDE(float[] samples, BoxStats boxStats)
      {
         if(samples == null || samples.length < 3 || boxStats == null) return(null);
         
         double mean = 0;
         for(int i=0; i<samples.length; i++) mean += samples[i];
         mean /= samples.length;
         double std = 0;
         for(int i=0; i<samples.length; i++) std += (samples[i]-mean)*(samples[i]-mean);
         std = Math.sqrt(std/(samples.length - 1));
         
         double minX = boxStats.min;
         double maxX = boxStats.max;
         if(boxStats.outliers != null)
         {
            if(boxStats.outliers[0] < minX) minX = boxStats.outliers[0];
            int n = boxStats.outliers.length - 1;
            if(boxStats.outliers[n] > maxX) maxX = boxStats.outliers[n];
         }
         
         // Silverman's rule-of-thumb calculation of Gaussian kernel bandwidth (assumes that true probability density
         // is approximately Gaussian)
         double bw = 0.9 * Math.min(std, (boxStats.q3 - boxStats.q1)/1.34) * Math.pow(boxStats.sampleSize, -0.2);
         
         // calculate the kernel density function using a Gaussian kernel, then normalize to a peak of 1
         minX -= 2.5*bw;
         maxX += 2.5*bw;
         double dx = (maxX - minX)/100;
         double scale = 1.0/(samples.length * bw * Math.sqrt(2*Math.PI));
         
         double kdeMax = 0;
         double[][] kde = new double[101][2];
         for(int i=0; i<101; i++)
         {
            kde[i][0] = (i==0) ? minX : kde[i-1][0] + dx;
            kde[i][1] = 0;
            for(int j=0; j<samples.length; j++)
               kde[i][1] += scale * Math.exp(-0.5*Math.pow((kde[i][0] - samples[j])/bw, 2));
            if(kde[i][1] > kdeMax) kdeMax = kde[i][1];
         }
         for(int i=0; i<101; i++) kde[i][1] = kde[i][1] / kdeMax;

         return(kde);
      }
   }
}
