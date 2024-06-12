package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.srscicomp.common.g2dutil.BasicPaintableShape;
import com.srscicomp.common.g2dutil.BasicPainterStyle;
import com.srscicomp.common.g2dutil.CircularArcPainter;
import com.srscicomp.common.g2dutil.LineSegmentPainter;
import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.PaintableShape;
import com.srscicomp.common.g2dutil.Painter;
import com.srscicomp.common.g2dutil.PainterStyle;
import com.srscicomp.common.g2dutil.PolylinePainter;
import com.srscicomp.common.g2dutil.ShapePainter;
import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;

/**
 * <b>TraceNode</b> is the a data presentation element for the 2D data set formats supported in <i>FypML</i>. It 
 * subclasses {@link FGNPlottableData}, which manages the actual data source.
 * 
 * <p>A trace node's properties determine how its data set (which is a property itself) will be rendered within the data
 * viewport of the parent graph (data presentation nodes may only exist as children of a graph in the FypML figure
 * graphic model). The <i>mode</i> attribute selects the data display mode. Six display modes modes are currently 
 * supported -- see {@link DisplayMode} for an explanation of each of these modes, as well as the various
 * properties of the trace node that affect the rendering of the data set.</p>
 * 
 * @author sruffner
 */
public class TraceNode extends FGNPlottableData implements Cloneable
{
   /**
    * Construct a trace node initially configured in the {@link DisplayMode#POLYLINE} mode with an empty data set, 
    * with zero x- and y-coordinate offsets, zero histogram bar width and baseline, the <i>avg</i> flag unset, and a 
    * plot skip size of 1 (all points plotted). All styling attributes are initially implicit (inherited). It has no 
    * title, and its automated legend entry is enabled. It contains two intrinsic component nodes: a {@link SymbolNode}
    * and an {@link ErrorBarNode}, which govern the appearance of any marker symbols and error bars, respectively, in 
    * the rendering of the 2D data set.
    */
   public TraceNode()
   {
      super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR|HASTITLEATTR|ALLOWATTRINTITLE);
      setTitle("");
      mode = DisplayMode.POLYLINE;
      xOffset = yOffset = 0;
      barWidth = baseline = 0;
      avg = false;
      skip = 1;
      slidingWindowLen = 1;
      
      addComponentNode( new SymbolNode() );
      addComponentNode( new ErrorBarNode() );
   }

   
   //
   // Support for child nodes -- none permitted!
   //
   
   @Override public FGNodeType getNodeType() { return(FGNodeType.TRACE); }


   //
   // Properties
   //

   /** An enumeration of the different data display modes supported by <code>TraceNode</code>. */
   public enum DisplayMode
   {
      /** 
       * The dataset is drawn as a "connect the dots" polyline, rendered IAW the trace node's own styling attributes. If
       * the data includes nonzero standard deviations in <i>x</i> and/or <i>y</i>, error bars are drawn at each data 
       * point, defined and styled IAW the {@link ErrorBarNode} component. In a polar plot, error bars in <i>x</i> will 
       * be circular arcs. Finally, marker symbols may be rendered at each data point, defined and styled IAW the
       * {@link SymbolNode} component.
       */
      POLYLINE, 
      
      /**
       * A linear regression line or a sliding average trace is computed and drawn through the data points IAW the trace 
       * node's draw styles. <i>The "trend line" is omitted if the parent graph is polar or if cannot be computed for
       * whatever reason.</i> Error bars and marker symbols are rendered as in the POLYLINE mode.
       */
      TRENDLINE,
      
      /**
       * Data set is drawn as a "sample-and-hold" polyline. Two well-defined points (x0, y0) and (x1, y1) in the data
       * are connected by a stair step: (x0,y0) to (x1,y0) to (x1,y1). Error bars and marker symbols are drawn, if
       * applicable, in the same manner as in the {@link #POLYLINE} mode.
       */
      STAIRCASE,
      
      /**
       * Intended for data set with nonzero <em>y</em>-standard deviation data. In this mode, the variation in the 
       * nominal data is rendered as a band encompassing +/-1 standard deviation about the nominal trace <em>{x,y}</em>.
       * A polyline connects the points <em>{x, y+dy}</em>, and another connects the points <em>{x, y-dy}</em>, where 
       * <em>dy</em> represents one standard deviation in <em>y</em> at each point <em>(x,y)</em>. Both standard 
       * deviation polylines are styled IAW the <code>TraceNode</code>'s <code>ErrorBarNode</code> component. The 
       * area between the two polylines will be filled with the current text/fill color if the <em>filled</em> flag is 
       * set. Finally, a polyline representing the nominal trace is drawn IAW this node's own style attributes. Note 
       * that marker symbols are never rendered in this display mode.
       */
      ERRORBAND, 

      /**
       * The dataset is drawn as a histogram, possibly adorned with symbols and error bars. The histogram is drawn IAW 
       * the style attributes of the <code>TraceNode</code> itself, and the histogram bars are filled with the node's 
       * current text/fill color if the <em>filled</em> flag is set. The <em>barWidth</em> and <em>baseline</em> 
       * properties define the histogram bar width and baseline level for this display mode (they do not apply to the 
       * other modes). If the <em>barWidth</em> is 0, the bars are drawn as lines instead. In a polar graph context, the
       * bars become circular pie wedges (baseline = 0) or radial sections. The symbols and error bars are rendered as 
       * in the <code>POLYLINE</code> display mode.
       */
      HISTOGRAM,
      
      /**
       * This display mode is intended for a dataset which is actually a collection of two or more individual point 
       * sets sharing the same <em>x</em>-coordinates: <em>{x: y1 y2 ...}</em>. Typically, the individual point sets 
       * represent repeated measures of the same stochastic variable over time, so that the collection captures the 
       * variation in that variable. The individual point sets are drawn as separate polylines, each styled IAW the 
       * <code>TraceNode</code>'s <code>ErrorBarNode</code> component. That node's <em>endcap</em> -- if it is 
       * nonzero size -- is rendered at each well-defined data point across all sets. On top of all this, the nominal 
       * or average trace is rendered as a polyline with marker symbols, but only if <i>avg==true</i>. This average 
       * polyline is rendered IAW the styling of the <code>TraceNode</code> itself and its <code>SymbolNode</code> 
       * component. <i>Note</i> that, if the referenced dataset does not contain two or more component point sets, then 
       * all that will be rendered is the "nominal" trace.
       */
      MULTITRACE; 
      
      @Override
      public String toString() { return(super.toString().toLowerCase()); }
   }

   /** The current data display mode. */
   private DisplayMode mode;

   /**
    * Get the current data display mode for this data trace.
    * @return The current data display mode.
    */
   public DisplayMode getMode() { return(mode); }

   /**
    * Set the data display mode for this data trace. If a change is made, {@link #onNodeModified()} is invoked.
    * 
    * <p>Upon switching to the {@link DisplayMode#MULTITRACE}, a couple of other changes are made automatically: (1) The 
    * <i>avg</i> flag is set to false because users rarely want the average trace rendered on top of the multitrace 
    * polylines. (2) The endcap size is set to zero on the child <i>ebar</i> element, because users rarely want to 
    * render marker symbols on top of the multitrace polylines. Note that no commensurate adjustment is made when 
    * switching out of <i>multitrace</i> mode.</p>
    * 
    * @param mode The new display mode. A null value is rejected.
    * @return False if argument was rejected; true otherwise.
    */
   public boolean setMode(DisplayMode mode)
   {
      if(mode == null) return(false);
      if(this.mode != mode)
      {
         if(doMultiNodeEdit(FGNProperty.MODE, mode)) return(true);
         
         DisplayMode oldMode = this.mode;
         this.mode = mode;
         if(areNotificationsEnabled()) onNodeModified(FGNProperty.MODE);
         
         // if we switch to multitrace mode, ensure that avg==false and that the endcap size for the child ebar is zero
         // because users almost always prefer these settings when rendering in that display mode. We don't want these 
         // additional changes posted separately to the containing model's undo history.
         if(this.mode == DisplayMode.MULTITRACE)
         {
            FGraphicModel model = getGraphicModel();
            if(model != null) model.blockEditHistory();
            
            setShowAverage(false);
            getErrorBarNode().setEndCapSize(new Measure(0, Measure.Unit.IN));
            
            if(model != null) model.unblockEditHistory();
         }

         if(areNotificationsEnabled()) FGNRevEdit.post(this, FGNProperty.MODE, this.mode, oldMode);
      }
      return(true);
   }

   /**
    * An offset applied to the primary coordinate (<em>x</em> or <em>theta</em>, depending on graph context) of each 
    * well-defined data point in this <code>TraceNode</code>'s data source.
    */
   private float xOffset;

   /**
    * Get the primary coordinate (<em>x</em> or <em>theta</em>) offset. Each well-defined data point <em>(x,y)</em> 
    * will actually be drawn at <em>(x+x0, y+y0)</em>, where <em>x0</em> is the primary coordinate offset and 
    * <em>y0</em> is the secondary coordinate offset returned by <code>getYOffset()</code>.
    * @return The primary coordinate offset value.
    */
   public float getXOffset() { return(xOffset); }
   
   /**
    * Set the primary coordinate (<em>x</em> or <em>theta</em>) offset. If a change is made, the method 
    * <code>onNodeModified()</code> is invoked.
    * @param xoff A new value for the primary coordinate offset. NaN and +/-infinity are rejected.
    * @return <code>True</code> iff new value was accepted.
    */
   public boolean setXOffset(float xoff) 
   {
      if(!Utilities.isWellDefined(xoff)) return(false);
      if(xOffset != xoff)
      {
         if(doMultiNodeEdit(FGNProperty.XOFF, new Float(xoff))) return(true);
         
         Float old = new Float(xOffset);
         xOffset = xoff;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.XOFF);
            FGNRevEdit.post(this, FGNProperty.XOFF, new Float(xOffset), old);
         }
      }
      return(true);
   }

   /**
    * An offset applied to the secondary coordinate (<em>y</em> or <em>radius</em>, depending on graph context) of each 
    * well-defined data point in this <code>TraceNode</code>'s data source.
    */
   private float yOffset;

   /**
    * Get the secondary coordinate (<em>y</em> or <em>radius</em>) offset. Each well-defined data point <em>(x,y)</em> 
    * will actually be drawn at <em>(x+x0, y+y0)</em>, where <em>y0</em> is the secondary coordinate offset and 
    * <em>x0</em> is the primary coordinate offset returned by <code>getXOffset()</code>.
    * @return The secondary coordinate offset value.
    */
   public float getYOffset() { return(yOffset); }
    
   /**
    * Set the secondary coordinate (<em>y</em> or <em>radius</em>) offset. If a change is made, the method 
    * <code>onNodeModified()</code> is invoked.
    * @param yoff A new value for the secondary coordinate offset. NaN and +/-infinity are rejected.
    * @return <code>True</code> iff new value was accepted.
    */
   public boolean setYOffset(float yoff) 
   {
      if(!Utilities.isWellDefined(yoff)) return(false);
      if(yOffset != yoff)
      {
         if(doMultiNodeEdit(FGNProperty.YOFF, new Float(yoff))) return(true);
         
         Float old = new Float(yOffset);
         yOffset = yoff;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.YOFF);
            FGNRevEdit.post(this, FGNProperty.YOFF, new Float(yOffset), old);
         }
      }
      return(true);
   }

   /** Width of a histogram bar, in user units (for histogram display mode only). Cannot be negative. */
   private float barWidth;

   /**
    * Get the histogram bar width (applicable only in {@link DisplayMode#HISTOGRAM}).
    * @return The histogram bar width, in user units.
    */
   public float getBarWidth() { return(barWidth); }
    
   /**
    * Set the histogram bar width. If a change is made, {@link #onNodeModified()} is invoked.
    * @param w The new histgoram bar width, in user units. Negative numbers, NaN and +/-infinity are rejected.
    * @return True iff new value was accepted.
    */
   public boolean setBarWidth(float w) 
   {
      if(!Utilities.isWellDefined(w) || w < 0) return(false);
      if(barWidth != w)
      {
         if(doMultiNodeEdit(FGNProperty.BARWIDTH, new Float(w))) return(true);
         
         Float old = new Float(barWidth);
         barWidth = w;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.BARWIDTH);
            FGNRevEdit.post(this, FGNProperty.BARWIDTH, new Float(barWidth), old);
         }
      }
      return(true);
   }

   /** The histogram baseline, in user units (applicable only in {@link DisplayMode#HISTOGRAM}). */
   private float baseline;

   /**
    * Get the histogram baseline (applicable only in {@link DisplayMode#HISTOGRAM}).
    * @return The histogram baseline, in user units.
    */
   public float getBaseline() { return(baseline); }
    
   /**
    * Set the histogram baseline. If a change is made, {@link #onNodeModified()} is invoked.
    * @param base The new baseline, in user units. NaN and +/-infinity are rejected.
    * @return True iff new value was accepted.
    */
   public boolean setBaseline(float base) 
   {
      if(!Utilities.isWellDefined(base)) return(false);
      if(baseline != base)
      {
         if(doMultiNodeEdit(FGNProperty.BASELINE, new Float(base))) return(true);
         
         Float old = new Float(baseline);
         baseline = base;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.BASELINE);
            FGNRevEdit.post(this, FGNProperty.BASELINE, new Float(baseline), old);
         }
      }
      return(true);
   }

   /** Enable/disable rendering of the average trace in the <i>multitrace</i> display mode. */
   private boolean avg = false;

   /**
    * Is the nominal or average trace rendered in the {@link DisplayMode#MULTITRACE} mode? The flag attribute is not 
    * relevant in any other display modes.
    * 
    * @return True if nominal trace or the sliding average trace is shown.
    */
   public boolean getShowAverage() { return(avg); }

   /**
    * Enable/disable rendering of the nominal trace in {@link DisplayMode#MULTITRACE} mode. If a change is made, {@link 
    * #onNodeModified()} is invoked. <i>Note that changing this property has no effect in the other display modes.</i>
    * 
    * @param b True to enable, false to disable rendering of the nominal or sliding average trace. 
    */
   public void setShowAverage(boolean b)
   {
      if(b != avg)
      {
         if(doMultiNodeEdit(FGNProperty.AVG, new Boolean(b))) return;
         
         Boolean old = new Boolean(avg);
         avg = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.AVG);
            FGNRevEdit.post(this, FGNProperty.AVG, new Boolean(avg), old);
         }
      }
   }

   /** The plot skip size. Must be 1 or larger. A value of 1 means that all points are plotted. */
   private int skip;

   /**
    * Get the plot skip size, ie, the number of points to skip per point actually rendered. Normally 1 (plot all 
    * points), it can be set to a larger value to speed up rendering of a large data set -- at the expense of 
    * subsampling the data.
    * 
    * @return The plot skip size. A value of 1, not zero, means that every point from the data source is plotted.
    */
   public int getSkip() { return(skip); }

   /**
    * Set the plot skip size, ie, the number of points to skip per point actually rendered. If a change is made, the 
    * method {@link #onNodeModified()} is invoked.
    * @param n The new plot skip size. Any negative integer or zero will be rejected.
    * @returns True iff argument was accepted.
    */
   public boolean setSkip(int n)
   {
      if(n < 1) return(false);
      if(skip != n)
      {
         if(doMultiNodeEdit(FGNProperty.SKIP, new Integer(n))) return(true);
         
         Integer old = new Integer(skip);
         skip = n;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.SKIP);
            FGNRevEdit.post(this, FGNProperty.SKIP, new Integer(skip), old);
         }
      }
      return(true);
   }

   /** 
    * Length P of the window for computing the sliding average trace in {@link DisplayMode#TRENDLINE} only. P>=1; if 1,
    * the trend line is the regression line through the data rather than a sliding average trace.
    */
   private int slidingWindowLen;
   
   /**
    * Get the window length for computing the sliding average trace -- applicable to {@link DisplayMode#TRENDLINE} only.
    * When the length is 1, the trend line is the "least mean squares" regression line through the {x,y} data rather 
    * than a sliding average trace.
    * 
    * @return The sliding average window length (number of data points preceding the current point, inclusive).
    */
   public int getSlidingWindowLength() { return(slidingWindowLen); }
   
   /**
    * Set the window length for computing the sliding average trace in {@link DisplayMode#TRENDLINE} mode. If a change
    * is made, {@link #onNodeModified()] is invoked.
    * 
    * <p>Note that the window lengths exceeding the number of data points in the set will be accepted, but the sliding
    * average trace will not be rendered in that case.</p>
    * 
    * @param n The window length. Must be >= 1.
    * @return True if specified window length is valid.
    */
   public boolean setSlidingWindowLength(int n)
   {
      if(n < 1) return(false);
      
      if(slidingWindowLen != n)
      {
         if(doMultiNodeEdit(FGNProperty.LEN, new Integer(n))) return(true);
         
         Integer old = new Integer(slidingWindowLen);
         slidingWindowLen = n;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.LEN);
            FGNRevEdit.post(this, FGNProperty.LEN, new Integer(slidingWindowLen), old,
                  String.format("Set window length for sliding average trace to %d data points (1=regression line)",
                        slidingWindowLen));
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
         case BARWIDTH: ok = setBarWidth((Float)propValue); break;
         case BASELINE: ok = setBaseline((Float)propValue); break;
         case XOFF: ok = setXOffset((Float)propValue); break;
         case YOFF: ok = setYOffset((Float)propValue); break;
         case AVG: setShowAverage((Boolean)propValue); ok = true; break;
         case SKIP: ok = setSkip((Integer)propValue); break;
         case LEN: ok = setSlidingWindowLength((Integer)propValue); break;
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
         case BARWIDTH: value = new Float(getBarWidth()); break;
         case BASELINE: value = new Float(getBaseline()); break;
         case XOFF: value = new Float(getXOffset()); break;
         case YOFF: value = new Float(getYOffset()); break;
         case AVG: value = new Boolean(getShowAverage()); break;
         case SKIP: value = new Integer(getSkip()); break;
         case LEN: value = new Integer(getSlidingWindowLength()); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }


   //
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   /** 
    * The node-specific properties exported in a trace node's style set are the display mode, the show-average flag, and
    * include-in-legend flag. All other trace node-specific properties (skip factor, X and Y offsets, histogram bar 
    * width and baseline) are deemed to be data-related or effect the position of the trace within the parent graph.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.MODE, getMode());
      styleSet.putStyle(FGNProperty.AVG, new Boolean(getShowAverage()));
      styleSet.putStyle(FGNProperty.LEGEND, new Boolean(getShowInLegend()));
   }
   
   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      // If display mode is switched to multi-trace mode, then we also clear the show-average flag and set error bar
      // end caps to zero size, as is done in setMode(). Of course, these changes could get overridden. That's why it's
      // important to apply the display mode first!
      DisplayMode dm = (DisplayMode) applied.getCheckedStyle(FGNProperty.MODE, getNodeType(), DisplayMode.class);
      if(dm != null && !dm.equals(restore.getStyle(FGNProperty.MODE)))
      {
         mode = dm;
         avg = false;
         getErrorBarNode().setEndCapSizeNoNotify(new Measure(0, Measure.Unit.IN));
         changed = true;
      }
      else restore.removeStyle(FGNProperty.MODE);
      
      // the next 3 flags have similar meanings on raster node, so we don't check node type
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.AVG, null, Boolean.class);
      if(b != null && !b.equals(restore.getStyle(FGNProperty.AVG)))
      {
         avg = b.booleanValue();
         changed = true;
      }
      else restore.removeStyle(FGNProperty.AVG);

      b = (Boolean) applied.getCheckedStyle(FGNProperty.LEGEND, null, Boolean.class);
      if(b != null && !b.equals(restore.getStyle(FGNProperty.LEGEND)))
      {
         setShowInLegendNoNotify(b);
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LEGEND);
      
      return(changed);
   }
   
   
   //
   // Miscellaneous
   //

   /**
    * Return the array of data points that should be plotted when this trace node is rendered, taking into account the
    * node's current plot skip size. Normally, every point in a data set is plotted. However, if the plot skip size 
    * <em>N</em> is greater than 1, then every <em>N</em>-th data point is plotted instead.
    * 
    * @param allowSubSampling If this flag is set and the data set is very large (exceeding 5000 points after the plot 
    * skip size is accounted for), a radial distance-based sub-sampling algorithm is applied to reduce the total number 
    * of points in the array.
    * @param isStair If true, then generate the intervening points to render data as a staircase rather than a 
    * "connect-the-dots" polyline.
    * @return Array of data points to be plotted when element is rendered, in plotting order. Each data point is 
    * transformed from "user" coordinates to \ "painting" coordinates WRT the parent graph viewport. Can contain 
    * ill-defined points, representing discontinuities in the rendered polyline.
    */
   private Point2D[] getPlottedCoords(boolean allowSubSampling, boolean isStair)
   {
      // use the appropriate location producer to traverse the point sequence. This ensures that, in the event that the
      // polyline sub-sampling algorithm is engaged, the Postscript output will replicate what's rendered onscreen.
      Iterator<Point2D> iterator = 
            isStair ? new StairPointProducer(allowSubSampling) : new DataPointProducer(allowSubSampling);
      List<Point2D> ptList = new ArrayList<Point2D>();
      while(iterator.hasNext())
      {
         Point2D next = (Point2D) iterator.next().clone();    // must clone, b/c DataPointProducer reuses a Point2D
         ptList.add(next);
      }
      Point2D[] coords = new Point2D[ptList.size()];
      for(int i = 0; i < ptList.size(); i++) coords[i] = ptList.get(i);
      
      // in case the point list is really large
      ptList.clear();
      ptList = null;
      
      return(coords);
   }

   /**
    * Retrieve the <code>ErrorBarNode</code> component that governs the appearance of this <code>TraceNode</code>'s 
    * error bars, if it has any.
    * @return The error bar info.
    */
   public ErrorBarNode getErrorBarNode() { return((ErrorBarNode)getComponentNodeAt(1)); }

   /**
    * Does this <code>TraceNode</code> currently hide all error bars? This will be the case if: 
    * <ul>
    *    <li>The data source contains no error data.</li>
    *    <li>The component <code>ErrorBarNode</code>'s <em>hide</em> flag is set; <em>or</em></li>
    *    <li>In the "error band" display mode, the error band is neither filled nor stroked; in all other display modes,
    *    the the <code>ErrorBarNode</code>'s stroke width is zero, and it does not have filled endcaps.</li>
    * </ul>
    * 
    * @return <code>True</code> if no error bars are rendered for this data set.
    */
   public boolean areErrorBarsHidden()
   {
      ErrorBarNode ebar = getErrorBarNode();
      if(ebar.getHide() || !hasErrorData()) return(true);
      
      boolean noEndCaps = (ebar.getEndCapSizeInMilliInches() <= 0);
      boolean noClosedCaps = noEndCaps || !ebar.getEndCap().isClosed();
      if(mode == DisplayMode.ERRORBAND)
         return(!(ebar.isStroked() || getFillColor().getAlpha() != 0));
      else
         return((!ebar.isStroked()) && (noClosedCaps || ebar.getFillColor().getAlpha() == 0));
   }


   // 
   // FGNPlottableData, FGNPlottable
   //
   
   /**
    * The initial default data set is a {@link Fmt#SERIES} generated by the function <i>y = C + A *
    * sinSq(2*pi*x/T)</i>, where <i>sinSq() = sin()*sin()</i>, <i>C, A</i> are chosen so the data set spans most of the
    * available Y-axis range of the graph specified, and <i>T</i> is chosen so that roughly two cycles of the periodic 
    * function span the available X-axis range. However, if graph is polar, data is instead generated by the function
    * <i>y = C + A*x/360</i>, where <i>A</i> is the radial axis range and <i>C</i> is the start of that range.
    */
   @Override protected void initializeDefaultData(double[] axisRng, boolean isPolar)
   {
      float x0 = (float) axisRng[0];
      float x1 = (float) axisRng[1];
      float y0 = (float) axisRng[2];
      float y1 = (float) axisRng[3];
      
      float[] params = new float[2];
      float[] fData = new float[60];

      if(isPolar)
      {
         params[0] = 6;
         params[1] = 0;

         for(int i=0; i<60; i++) fData[i] = y0 + (y1-y0)*i*6/360f;
      }
      else
      {
         params[0] = (x1-x0) / 60f;
         params[1] = x0;
         
         for(int i=0; i<60; i++)
         {
            double d = Math.sin(2.0*Math.PI*i*params[0]/(x1-x0));
            fData[i] = (float) (y0 + (y1-y0)*d*d);
         }
      }
      
      DataSet ds = DataSet.createDataSet(getDataSet().getID(), Fmt.SERIES, params, 60, 1, fData);
      if(ds != null) setDataSet(ds);
   }

   @Override protected boolean recalcDataRange(Object hint)
   {
      boolean needRecalc = 
         hint == null || hint == FGNProperty.SRC || hint == FGNProperty.MODE || hint == FGNProperty.XOFF || 
         hint == FGNProperty.YOFF || 
         (mode == DisplayMode.HISTOGRAM && (hint == FGNProperty.BASELINE || hint == FGNProperty.BARWIDTH));
      if(!needRecalc) return(false);
      
      DataSet set = getDataSet();
      float minX = xOffset + set.getXMin();
      float maxX = xOffset + set.getXMax();
      float minY = yOffset + set.getYMin();
      float maxY = yOffset + set.getYMax();
      if(mode == DisplayMode.HISTOGRAM)
      {
         minX -= barWidth / ((float)2.0);
         maxX += barWidth / ((float)2.0);
         minY = Math.min(baseline, minY);
         maxY = Math.max(baseline, maxY);
      }
      
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

   @Override  public boolean isSupportedDataFormat(Fmt fmt)
   {
      return(fmt == Fmt.PTSET || fmt == Fmt.MSET || fmt == Fmt.SERIES || fmt == Fmt.MSERIES);
   }


   /** The dataset formats renderable in a data trace node. */
   private final static Fmt[] supportedFormats = new Fmt[] {Fmt.PTSET, Fmt.MSET, Fmt.SERIES, Fmt.MSERIES};
   
   @Override public Fmt[] getSupportedDataFormats() { return(supportedFormats); }
   
   @Override public SymbolNode getSymbolNode() { return((SymbolNode) getComponentNodeAt(0)); }

   /** A trace node will never render marker symbols when in the error-band display mode. */
   @Override public boolean usesSymbols() { return(mode != DisplayMode.ERRORBAND); }

   @Override public boolean useBarInLegend() { return(mode == DisplayMode.HISTOGRAM && barWidth > 0f); }

   
   //
   // Focusable/Renderable support
   //
   
   /**
    * The focus shape is the rectangle that bounds the current rendering of the data trace. If the parent graph clips 
    * its data to the data window, then the focus rectangle is likewise clipped.
    * 
    * <p>For performance reasons, the cached bounding rectangle is used if it is available; it is recalculated only if
    * it is not yet defined. In the latter case, if the rectangle could not be calculated, method returns null.</p>
    */
   public Shape getFocusShape(Graphics2D g2d)
   {
      if(rBoundsSelf == null) getRenderBoundsForSelf(g2d, false);
      return(rBoundsSelf == null ? null : getLocalToGlobalTransform().createTransformedShape(rBoundsSelf));
   }

   /**
    * The "hit test" for a <b>TraceNode</b> is not as precise as for other elements. The node is considered "hit" by the
    * specified point if that point is within a radius of 100 milli-in of a well-defined point from the node's data 
    * source. However, since the data source could be very large, the method will only check up to 50 different points. 
    * If a "hit" is detected, then the method returns a reference to the trace node itself, since it has no renderable 
    * descendants to search!
    */
   @Override protected FGraphicNode hitTest(Point2D p)
   {
      // trace will only be rendered in a well-defined graph viewport
      FViewport2D graphVP = getParentViewport();
      if(getParentGraph() == null || graphVP == null) return(null);

      // make sure there's data to check!
      DataSet set = getDataSet();
      int nTotal = set.getDataSize(-1);
      int n = nTotal/skip;
      double dSkip = skip * ((n<=50) ? 1 : ((double)n)/50.0);

      double index = 0;
      int i = 0;
      Point2D pDatum = new Point2D.Double(0,0);
      AffineTransform at = getLocalToGlobalTransform();
      while(i < nTotal)
      {
         pDatum.setLocation(set.getX(i,-1)+xOffset, set.getY(i,-1)+yOffset);
         graphVP.userUnitsToThousandthInches(pDatum);
         if(Utilities.isWellDefined(pDatum))
         {
            at.transform(pDatum, pDatum);
            if(pDatum.distance(p) <= 100) return(this);
         }

         index += dSkip;
         i = (int)index;
      }

      return(null);
   }

   /**
    * This method clears the internal list of painter(s) used to render the data trace node, as well as the cached 
    * rectangle bounding any marks made by those painters.
    */
   @Override protected void releaseRenderResourcesForSelf()
   {
      painters.clear();
      rBoundsSelf = null;
   }

   /**
    * Cached rectangle bounding only the marks made by this trace node. An empty rectangle indicates that the element 
    * makes no marks when "rendered". If null, the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;

   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null || painters.size() == 0)
      {
         updatePainters();
         rBoundsSelf = new Rectangle2D.Double();
         for(Painter p : painters)
         {
            p.updateFontRenderContext(g2d);
            p.invalidateBounds();
            Utilities.rectUnion(rBoundsSelf, p.getBounds2D(null), rBoundsSelf);
         }

         // if parent graph clips its data, then we need to clip the render bounds to the graph's data viewport
         FGNGraph g = getParentGraph();
         if(g != null && g.getClip())
            Rectangle2D.intersect(rBoundsSelf, g.getBoundingBoxLocal(), rBoundsSelf);
      }
      return((Rectangle2D)rBoundsSelf.clone());
   }

   /**
    * Render this data trace into the current graphics context in the manner appropriate to the current display mode.
    * 
    * <p>Rendering is handled by a set of painters that are maintained and updated internally as the node's definition 
    * changes. Different painters are used to render the different display modes.</p>
    */
   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(needsRendering(task))
      {
         if(painters.size() == 0) 
            updatePainters();

         for(Painter p : painters)
         {
            if(!p.render(g2d, task)) return(false);
         }
      }
      return(true);
   }


   //
   // Internal rendering resources
   //

   /** The painters which are responsible for rendering this data trace node IAW its current state. */
   private List<Painter> painters = new ArrayList<Painter>();

   /**
    * The display mode for which the internal painter list has been configured. The painter list is reinitialized
    * whenever the current display mode does not match this.
    */
   private DisplayMode paintedDisplayMode = null;

   /**
    * Create/update the list of painter responsible for rendering this data trace node IAW its current definition.
    * 
    * <p><b>TraceNode</b> maintains an internal list of configured painters. The composition of this list and the 
    * configuration of the specific painters will vary with the current data display mode.</p>
    * 
    * <p>{@link DisplayMode#POLYLINE}. In this case, the referenced dataset is rendered as a "connect the dots" 
    * polyline, possibly adorned with error bars and/or marker symbols. The following painters are configured:
    * <ul>
    * <li>An {@link #ErrorBarPainter} renders any error bars defined on the data set, styled IAW the trace node's 
    * {@link ErrorBarNode} component.</li>
    * <li>A {@link PolylinePainter} is configured to connect the well-defined data points in the set with a polyline 
    * styled IAW the element's own draw styles. Any ill-defined points introduce gaps in the polyline. The inner class 
    * {@link #DataPointProducer} serves as the location producer for this painter.</li>
    * <li>A {@link ShapePainter} is configured to render a symbol (if any) at all well-defined points in the set, 
    * styled IAW the node's {@link SymbolNode} component.</li>
    * </ul>
    * </p>
    * 
    * <p>{@link DisplayMode#TRENDLINE}. Same as {@link DisplayMode#POLYLINE}, except that the location producer for the
    * {@link PolylinePainter} generates the points for a "trend line" -- either the LMS regression line segment if
    * N = {@link #getSlidingWindowLength()} == 1, or a sliding average trace with window length N when N > 1. The points
    * list will be empty if the regression line or the sliding average trace cannot be determined for whatever reason. 
    * In addition, the painters are ordered so that the trend line is drawn last.</p>
    * 
    * <p>{@link DisplayMode#STAIRCASE}. Same as {@link DisplayMode#POLYLINE}, except that the location producer for the
    * {@link PolylinePainter} generates additional points to render the data as a staircase sequence.</p>
    * 
    * <p>{@link DisplayMode#ERRORBAND}. The nominal data trace, if displayed, is drawn as a connected polyline 
    * without symbols, and error information is represented by marker-less polylines drawn at +1STD and -1STD. The band 
    * between the STD polylines is optionally filled. This graphical rendering is handled by a set of three painters:
    * <ul>
    * <li>A {@link PolylinePainter} is configured to fill but not stroke the region bounded by the +1 and -1STD 
    * polylines. A {@link #StdDevPointProducer} generates the locations for this painter, tracing the +1STD polyline in 
    * one direction and the -1STD polyline in the other. If the referenced data source lacks any nonzero standard 
    * deviations or if the error band is not filled, this painter will have an empty location producer and thus will 
    * render nothing.</li>
    * <li>A second {@link PolylinePainter} is configured to stroke the two +1 and -1STD polyline. Again, a {@link 
    * StdDevPointProducer} generates the points for this painter, which is styled IAW the {@link ErrorBarNode} 
    * component. If the referenced data source lacks any nonzero standard deviations, the location producer will be 
    * empty and this painter will render nothing.</li>
    * <li>A third {@link PolylinePainter} is configured to render a polyline connecting the well-defined data points 
    * from the referenced data source, styled IAW the trace node's own draw styles.</li>
    * </ul>
    * </p>
    * 
    * <p>{@link DisplayMode#HISTOGRAM}. The dataset is rendered as a histogram, possibly with standard error bars 
    * and marker symbols drawn on top of it. Three painters are required:
    * <ul>
    * <li>A single painter renders the histogram, the appearance of which varies depending upon the histogram bar width 
    * and the type of graph in which the data appears. If the bar width is zero, the histogram bars are just lines, and 
    * a {@link LineSegmentPainter} is used. Otherwise, if the graph is polar, the histogram bars are circular pie 
    * wedges, requiring a {@link CircularArcPainter}; if the graph is not polar, a {@link PolylinePainter}> is 
    * configured to draw the bars. A {@link #HistogramVertexProducer}  generates the locations for each of these 
    * alternative painters. <i>NOTE</i> that whenever the histogram bar width or the display type of the enclosing graph
    * changes, this method must be invoked so that the histogram painter can be updated accordingly.</li>
    * <li>An {@link #ErrorBarPainter} renders any error bars defined by the data source, styled IAW the trace node's
    * {@link ErrorBarNode} component.</li>
    * <li>A {@link ShapePainter} is configured to render a symbol (if any) at all well-defined points in the data set, 
    * styled IAW this node's {@link SymbolNode} component. <i>Special case for "compass plots": If the graph is polar, 
    * the bar width is zero, the symbol size is nonzero, and the symbol is not a circle, a "rotation angle producer" is 
    * specified on this shape painter so that each symbol is rotated so that it points in the direction of the ray 
    * emanating from baseline and ending on data point.</i></li>
    * </ul>
    * </p>
    * 
    * <p>{@link DisplayMode#MULTITRACE}. This display mode is intended only for the display of a collection of point 
    * sets sharing the same x-coordinates and typically representing repeated measures of the same stochastic 
    * phenomenon. The individual point sets in the collection are all rendered in the same style, and the average across 
    * all sets may be rendered on top, in a different style. (In the degenerate case -- when the collection only has a 
    * single set, then that set will be drawn as the "average" or nominal trace.) For this mode, the method creates and 
    * maintains a list of four painters, as follows:
    * <ul>
    * <li>A {@link PolylinePainter} for rendering the individual point sets as polylines. The inner class {@link 
    * #MultiSetPointProducer} generates the locations for this painter, traversing all well-defined points in all the 
    * data sets, and inserting an ill-defined point between sets so the individual polylines are not connected. Styled 
    * IAW this node's {@link ErrorBarNode}, since the individual point sets typically capture the variability or 
    * uncertainty in the observed phenomenon.</li>
    * <li>A {@link ShapePainter} configured to render symbols for the individual polylines. The marker symbol is defined
    * by the "endcap" of the component {@link ErrorBarNode} and is styled IAW the draw styles of that node. Again, the 
    * location producer for this painter is {@link #MultiSetPointProducer}.</li>
    * <li>A second {@link PolylinePainter} is configured to render the <i>average</i> data trace as a polyline styled 
    * IAW this trace node's own draw styles. Here the location producer {@link #DataPointProducer}. The average trace is
    * not rendered when <i>avg==false</i>, in which case this painter has an empty location producer.</li>
    * <li>A second {@link ShapePainter} is configured to render a symbol (if any) at all well-defined points in the 
    * <i>average</i> trace, styled IAW this node's {@link SymbolNode} component.</li>
    * </ul>
    * </p>
    * 
    * <p>Note that the painters are rendered in the order they appear in the list. Thus, for the POLYLINE display mode, 
    * the error bars are drawn first, followed by the polyline and then the symbols. This order makes sense, since we 
    * don't want the polyline or the error bars to be drawn inside the symbols!</p>
    */
   private void updatePainters()
   {
      // catch a change in display mode since last time painter list was updated. In this case we must reinit list!
      if(paintedDisplayMode != mode)
         painters.clear();
      paintedDisplayMode = mode;

      if(mode == DisplayMode.POLYLINE || mode == DisplayMode.STAIRCASE)
      {
         if(painters.size() == 0)
         {
            painters.add( new ErrorBarPainter() );
            
            Iterable<Point2D> producer = (mode == DisplayMode.POLYLINE) ? 
                  new DataPointProducer() : new StairPointProducer();
            painters.add( new PolylinePainter(this, producer) );

            SymbolNode symbol = getSymbolNode();
            painters.add( new ShapePainter(symbol, new DataPointProducer(), symbol.getType(), 
                  (float) symbol.getSizeInMilliInches(), symbol.getCharacter()) );
         }
         else
         {
            SymbolNode symbol = getSymbolNode();
            ShapePainter symbolPainter = (ShapePainter) painters.get(2);
            symbolPainter.setPaintedShape(symbol.getType());
            symbolPainter.setSize((float) symbol.getSizeInMilliInches());
            symbolPainter.setTextLabel(symbol.getCharacter());
         }
      }
      else if(mode == DisplayMode.TRENDLINE)
      {
         if(painters.size() == 0)
         {
            painters.add( new ErrorBarPainter() );
            
            SymbolNode symbol = getSymbolNode();
            painters.add( new ShapePainter(symbol, new DataPointProducer(), symbol.getType(), 
                  (float) symbol.getSizeInMilliInches(), symbol.getCharacter()) );
            
            painters.add( new PolylinePainter(this, new TrendLineProducer()) );
         }
         else
         {
            SymbolNode symbol = getSymbolNode();
            ShapePainter symbolPainter = (ShapePainter) painters.get(1);
            symbolPainter.setPaintedShape(symbol.getType());
            symbolPainter.setSize((float) symbol.getSizeInMilliInches());
            symbolPainter.setTextLabel(symbol.getCharacter());
         }
      }
      else if(mode == DisplayMode.ERRORBAND)
      {
         ErrorBarNode ebar = getErrorBarNode();
         if(painters.size() == 0)
         {
            PolylinePainter polyPainter = new PolylinePainter(ebar, null);
            polyPainter.setConnectionPolicy(PolylinePainter.ConnectPolicy.CONNECTED);
            polyPainter.setFilled(true);
            polyPainter.setStroked(false);
            painters.add( polyPainter );

            polyPainter = new PolylinePainter(ebar, null);
            painters.add( polyPainter );

            painters.add( new PolylinePainter(this, new DataPointProducer()) );
         }
         boolean hasStdDev = !areErrorBarsHidden();
         boolean filled = ebar.getFillColor().getAlpha() != 0;
         painters.get(0).setLocationProducer( (hasStdDev && filled) ? new StdDevPointProducer() : null );
         painters.get(1).setLocationProducer( (hasStdDev && ebar.isStroked()) ? new StdDevPointProducer() : null );
      }
      else if(mode == DisplayMode.HISTOGRAM)
      {
         boolean isFill = getFillColor().getAlpha() != 0;
         if(painters.size() == 0)
         {
            FViewport2D parentVP = getParentViewport();
            HistogramVertexProducer vertexProvider = new HistogramVertexProducer(false);
            Painter painter = null;
            if(barWidth == 0)
            {
               LineSegmentPainter linePainter = new LineSegmentPainter(this, vertexProvider);
               painter = linePainter;
            }
            else if(parentVP.isPolar())
            {
               Point2D origin = parentVP.getPhysicalUserOrigin();
               CircularArcPainter arcPainter = new CircularArcPainter(this, vertexProvider, origin);
               arcPainter.setClosure(CircularArcPainter.Closure.SECTION);
               arcPainter.setFilled(isFill);

               // reference radius is the baseline property, converted from user to logical painting units
               double r = baseline;
               if(r != 0)
               {
                  Point2D p = new Point2D.Double(0,r);
                  parentVP.userUnitsToThousandthInches(p);
                  r = origin.distance(p);
               }
               arcPainter.setReferenceRadius(r);

               painter = arcPainter;
            }
            else
            {
               PolylinePainter rectPainter = new PolylinePainter(this, vertexProvider);
               rectPainter.setConnectionPolicy(PolylinePainter.ConnectPolicy.CLOSED);
               rectPainter.setFilled(isFill);
               painter = rectPainter;
            }
            painters.add( painter );

            painters.add( new ErrorBarPainter() );

            SymbolNode symbol = getSymbolNode();
            Marker symType = symbol.getType();
            float sz = (float) symbol.getSizeInMilliInches();
            ShapePainter symbolPainter = 
               new ShapePainter(symbol, new DataPointProducer(), symType, sz, symbol.getCharacter());
            if(parentVP.isPolar() && getBarWidth() == 0 && symType != Marker.CIRCLE && sz > 0)
               symbolPainter.setRotationProducer(new CompassPlotAngleProducer());
            
            painters.add(symbolPainter);
         }
         else
         {
            Painter histPainter = painters.get(0);
            FViewport2D parentVP = getParentViewport();
            HistogramVertexProducer vertexProvider = new HistogramVertexProducer(false);
            if(barWidth == 0)
            {
               if( !(histPainter instanceof LineSegmentPainter) )
                  painters.set(0, new LineSegmentPainter(this, vertexProvider));
            }
            else if(parentVP.isPolar())
            {
               Point2D origin = parentVP.getPhysicalUserOrigin();
               CircularArcPainter arcPainter = null;
               if( !(histPainter instanceof CircularArcPainter) )
               {
                  arcPainter = new CircularArcPainter(this, vertexProvider, origin);
                  arcPainter.setClosure(CircularArcPainter.Closure.SECTION);
                  painters.set(0, arcPainter);
               }
               else
               {
                  arcPainter = (CircularArcPainter) histPainter;
                  arcPainter.setCenterPoint(origin);
               }
               arcPainter.setFilled(isFill);

               // reference radius is the baseline property, converted from user to logical painting units
               double r = baseline;
               if(r != 0)
               {
                  Point2D p = new Point2D.Double(0,r);
                  parentVP.userUnitsToThousandthInches(p);
                  r = origin.distance(p);
               }
               arcPainter.setReferenceRadius(r);
            }
            else
            {
               if( !(histPainter instanceof PolylinePainter) )
               {
                  PolylinePainter rectPainter = new PolylinePainter(this, vertexProvider);
                  rectPainter.setConnectionPolicy(PolylinePainter.ConnectPolicy.CLOSED);
                  rectPainter.setFilled(isFill);
                  painters.set(0, rectPainter);
               }
               else
                  ((PolylinePainter)histPainter).setFilled(isFill);
            }

            SymbolNode symbol = getSymbolNode();
            Marker symType = symbol.getType();
            float sz = (float) symbol.getSizeInMilliInches();
            ShapePainter symbolPainter = (ShapePainter) painters.get(2);
            symbolPainter.setPaintedShape(symType);
            symbolPainter.setSize(sz);
            symbolPainter.setTextLabel(symbol.getCharacter()); 
            if(parentVP.isPolar() && getBarWidth() == 0 && symType != Marker.CIRCLE && sz > 0)
               symbolPainter.setRotationProducer(new CompassPlotAngleProducer());
            else
               symbolPainter.setRotationProducer(null);
         }
      }
      else if(mode == DisplayMode.MULTITRACE)
      {
         if(painters.size() == 0)
         {
            ErrorBarNode ebar = getErrorBarNode();
            painters.add( new PolylinePainter(ebar, new MultiSetPointProducer()) );

            painters.add( new ShapePainter(ebar, new MultiSetPointProducer(), ebar.getEndCap(), 
                  (float)ebar.getEndCapSizeInMilliInches(), null));

            painters.add( new PolylinePainter(this, avg ? new DataPointProducer() : null) );

            SymbolNode symbol = getSymbolNode();
            painters.add( new ShapePainter(symbol, avg ? new DataPointProducer() : null, symbol.getType(), 
                  (float) symbol.getSizeInMilliInches(), symbol.getCharacter()) );
         }
         else
         {
            ErrorBarNode ebar = getErrorBarNode();
            ShapePainter symbolPainter = (ShapePainter) painters.get(1);
            symbolPainter.setPaintedShape(ebar.getEndCap());
            symbolPainter.setSize((float)ebar.getEndCapSizeInMilliInches());

            PolylinePainter polyPainter = (PolylinePainter) painters.get(2);
            polyPainter.setLocationProducer(avg ? new DataPointProducer() : null);
            
            SymbolNode symbol = getSymbolNode();
            symbolPainter = (ShapePainter) painters.get(3);
            symbolPainter.setPaintedShape(symbol.getType());
            symbolPainter.setSize((float)symbol.getSizeInMilliInches());
            symbolPainter.setTextLabel(symbol.getCharacter());
            symbolPainter.setLocationProducer(avg ? new DataPointProducer() : null);
         }
      }
      else
         throw new IllegalStateException("Undefined display mode!");
   }

   /**
    * Calculates the end points of a line segment colinear with the least-mean-squares regression line segment fitted to
    * the source data set and and evaluated at the minimum and maximum observed values of the X-coordinate data. This is
    * intended for 2D data sets; any Z-coordinate data in the source is simply ignored.
    * 
    * <p>The regression line is undefined if the data set contains less than 2 valid points. If there are only 2, then
    * the method simply returns those two data points. Otherwise, it calculates the slope M and Y-intercept B of the
    * regression line, finds the minimum and maximum X-coordinates <i>xMin, xMax</i> in the data set, and returns the 
    * endpoints (xMin, M*xMin + B) and (xMax, M*xMax + B). Note that the regression line is undefined if the sample
    * standard deviation in X or Y is zero.</p>
    * 
    * <p>The regression line can be computed for semilogx, semilogy, or loglog 2D graph contexts, but is considered
    * undefined if the dataset contains any non-positive X- and/or Y-coordinate datum values.</p>
    * 
    * @param ds The data set.
    * @param pts A point list. Initially cleared, it will contain the end points of the regression line segment, as 
    * described. If the regression line cannot be found for whatever reason (1D or image data format; zero std dev in X 
    * or Y; etc), the list will be empty.
    * @param isLogX True to compute the regression line using log(X) rather than X.
    * @param isLogY True to compute the regression line using log(Y) rather than Y.
    */
   protected static void calculateRegressionLineSegment(DataSet ds, List<Point2D> pts, boolean isLogX, boolean isLogY)
   {
      pts.clear();
      if(ds == null || ds.getFormat() == Fmt.RASTER1D || ds.getFormat() == Fmt.XYZIMG) return;
      
      // require all X- and/or Y-coordinate data be strictly positive in semilogX,Y or loglog scenarios
      if(isLogX || isLogY)
      {
         for(int i=0; i<ds.getDataSize(-1); i++)
         {
            float x = ds.getX(i, -1), y = ds.getY(i, -1);
            if(!Utilities.isWellDefined(x, y)) continue;
            if((isLogX && x <= 0f) || (isLogY && y <= 0f)) return;
         }
      }
      
      // statistics...
      double xMean=0, yMean=0;
      int nPts = 0, iMin = -1, iMax = -1;
      for(int i=0; i<ds.getDataSize(-1); i++)
      {
         double x = ds.getX(i, -1), y = ds.getY(i, -1);
         if(!(Utilities.isWellDefined(x) && Utilities.isWellDefined(y)))
            continue;
         ++nPts;
         xMean += isLogX ? Math.log10(x) : x;
         if((iMin == -1) || (ds.getX(iMin, -1) > x)) iMin = i;
         if((iMax == -1) || (ds.getX(iMax, -1) < x)) iMax = i;
         yMean += isLogY ? Math.log10(y) : y;;
      }
      if(nPts == 2)
      {
         // special case: only 2 well-defined points
         Point2D p = new Point2D.Double(ds.getX(iMin, -1), ds.getY(iMin, -1));
         pts.add(p);
         p = new Point2D.Double(ds.getX(iMax, -1), ds.getY(iMax, -1));
         pts.add(p);
      }
      else if(nPts > 2)
      {
         // compute sample mean in x (or logX), y (or logY)
         xMean /= nPts; 
         yMean /= nPts; 
         
         // compute sample std dev in x (or logX), y (or logY)
         double xStd=0, yStd=0;
         for(int i=0; i<ds.getDataSize(-1); i++)
         {
            double x = ds.getX(i, -1), y = ds.getY(i, -1);
            if(!(Utilities.isWellDefined(x) && Utilities.isWellDefined(y)))
               continue;
            if(isLogX) x = Math.log10(x);
            if(isLogY) y = Math.log10(y);
            xStd += (x-xMean)*(x-xMean);
            yStd += (y-yMean)*(y-yMean);
         }
         xStd = Math.sqrt(xStd/(nPts-1));
         yStd = Math.sqrt(yStd/(nPts-1));
         
         // special case: zero standard deviation in X or Y -- regression line not calculated.
         if(xStd == 0 || yStd == 0) return;
         
         // correlation coefficient r
         double r = 0;
         for(int i=0; i<ds.getDataSize(-1); i++)
         {
            double x = ds.getX(i, -1), y = ds.getY(i, -1);
            if(!(Utilities.isWellDefined(x) && Utilities.isWellDefined(y)))
               continue;
            if(isLogX) x = Math.log10(x);
            if(isLogY) y = Math.log10(y);
            r += (x - xMean) * (y - yMean) / (xStd * yStd);
         }
         r /= (nPts - 1);
         
         // slope and y-intercept of regression line, which always passes through (xMean, yMean)
         double m = r * yStd / xStd;
         double b = yMean - m * xMean;
         
         // finally, compute endpoints of regression line at the min & max observed values of X. Apply the X and Y 
         // trace offsets.
         // IMPORTANT: in the semilog and loglog scenarios, we return the (X,Y) coordinates of each point, NOT 
         // (logX, Y), (x, logY), or (logX, logY)!
         double xMin = ds.getX(iMin, -1), xMax = ds.getX(iMax, -1);
         double y0 = m * (isLogX ? Math.log10(xMin) : xMin) + b;
         if(isLogY) y0 = Math.pow(10, y0);
         double y1 = m * (isLogX ? Math.log10(xMax) : xMax) + b;
         if(isLogY) y1 = Math.pow(10, y1);
         pts.add(new Point2D.Double(xMin, y0));
         pts.add(new Point2D.Double(xMax, y1));
      }
   }


   //
   // PSTransformable implementation
   //
   
   public void toPostscript(PSDoc psDoc)
   {
      // if there's no data to render, do nothing!
      if(getDataSet().isEmpty()) return;
 
      // render in Postscript IAW the current display mode...
      switch(mode)
      {
         case POLYLINE : renderAsPoints(psDoc); break;
         case TRENDLINE : renderAsTrendline(psDoc); break;
         case STAIRCASE : renderAsStaircase(psDoc); break;
         case ERRORBAND : renderAsErrorBand(psDoc); break;
         case HISTOGRAM : renderAsHistogram(psDoc); break;
         case MULTITRACE : renderAsMultitrace(psDoc); break;
      }
   }

   /**
    * Helper method for toPostscript(). Handles PS rendering of the trace node in the "polyline" display mode, possibly 
    * adorned with error bars and marker symbols.
    * @param psDoc The Postscript document in which this trace node is rendered.
    */
   private void renderAsPoints(PSDoc psDoc)
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      // get array of (x,y)-coords of all plotted points (in plotting order) and convert from the user coord system to 
      // the physical SVG coordinates of the parent viewport.  If there are no points, there's nothing to render.
      Point2D[] coords = getPlottedCoords(true, false);
      if(coords.length == 0) return;

      psDoc.startElement(this);

      // render error bars first, followed by connecting polyline possibly with marker symbols
      renderErrorBars(psDoc);

      // then draw the connecting polyline with symbols centered on all well-defined data points.  Symbols may have 
      // different draw styles than the polyline itself, and may include a letter centered within them. 
      SymbolNode symbolInfo = getSymbolNode();
      psDoc.renderPolyline(coords, symbolInfo.getType(), symbolInfo.getSizeInMilliInches(), symbolInfo, 
            symbolInfo.getCharacter(), !isStroked());

      psDoc.endElement();         
   }

   /**
    * Helper method for toPostscript(). Handles PS rendering of the trace node in the "trendline" display mode. 
    * @param psDoc The Postscript document in which this trace node is rendered.
    */
   private void renderAsTrendline(PSDoc psDoc)
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      // get array of (x,y)-coords of all plotted points (in plotting order) and convert from the user coord system to 
      // the physical SVG coordinates of the parent viewport.  If there are no points, there's nothing to render.
      Point2D[] coords = getPlottedCoords(true, false);
      if(coords.length == 0) return;

      psDoc.startElement(this);

      // render error bars first
      renderErrorBars(psDoc);

      // then the marker symbols. 
      SymbolNode symbolInfo = getSymbolNode();
      psDoc.renderPolyline(coords, symbolInfo.getType(), symbolInfo.getSizeInMilliInches(), symbolInfo, 
            symbolInfo.getCharacter(), true);

      // finally, draw the trend line itself: either a line segment representing the LMS regression line, or the sliding
      // average trace.
      List<Point2D> pts = new ArrayList<Point2D>();
      TrendLineProducer tlp = new TrendLineProducer();
      while(tlp.hasNext())
      {
         Point2D p = tlp.next();
         pts.add(new Point2D.Double(p.getX(), p.getY()));
      }
      if(pts.size() == 2) psDoc.renderLine(pts.get(0), pts.get(1));
      else if(pts.size() > 2)
      {
         coords = pts.toArray(new Point2D[pts.size()]);
         psDoc.renderPolyline(coords, null, 0.0, null, null, false);
      }
      
      psDoc.endElement();         
   }


   /**
    * Helper method for toPostscript(). Handles PS rendering of the trace node in the "staircase" display mode, possibly 
    * adorned with error bars and marker symbols.
    * @param psDoc The Postscript document in which this trace node is rendered.
    */
   private void renderAsStaircase(PSDoc psDoc)
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      // get array of (x,y)-coords of all plotted points (in plotting order) in the staircase polyline and convert from 
      // the user coord system to the physical SVG coordinates of the parent viewport.  If there are no points, there's 
      // nothing to render.
      Point2D[] coords = getPlottedCoords(true, true);
      if(coords.length == 0) return;
      
      psDoc.startElement(this);

      // render error bars first, then the polyline
      renderErrorBars(psDoc);
      psDoc.renderPolyline(coords, null, 0, null, null, !isStroked());
      
      // finally draw the marker symbols, but only at the points representing data, not at the intervening "sample and
      // hold" locations in the staircase.
      coords = getPlottedCoords(true, false);
      SymbolNode symbolInfo = getSymbolNode();
      psDoc.renderPolyline(coords, symbolInfo.getType(), symbolInfo.getSizeInMilliInches(), symbolInfo, 
            symbolInfo.getCharacter(), true);

      psDoc.endElement();         
   }

   /**
    * Helper method for toPostscript(). Handles PS rendering of the <code>TraceNode</code> as a nominal polyline with 
    * a +/-1STD error band, optionally filled.  No symbols are drawn in this display mode.
    * @param psDoc The Postscript document in which this <code>TraceNode</code> is rendered.
    */
   private void renderAsErrorBand( PSDoc psDoc )
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      psDoc.startElement(this);

      // render error band, possibly filled and delimiting polylines stroked. If there is no error data in the data 
      // set, or if error band is not filled and the delimiting polylines are hidden, then error band is not rendered.
      if(!areErrorBarsHidden())
      {
         // use StdDevPointProducer to traverse the +1/-1 STD polylines that bound the error band, connecting them with 
         // an ill-defined point so that the stroked lines are not connected.
         Iterator<Point2D> iterator = new StdDevPointProducer();
         List<Point2D> ptList = new ArrayList<Point2D>();
         while(iterator.hasNext())
         {
            Point2D next = (Point2D) iterator.next().clone();    // must clone, b/c StdDevPointProducer reuses a Point2D
            ptList.add(next);
         }
         Point2D[] coords = new Point2D[ptList.size()];
         for(int i = 0; i < ptList.size(); i++) coords[i] = ptList.get(i);
         
         // fill and/or stroke the resulting polyline IAW style properties of ErrorBarNode subordinate
         boolean filled = getFillColor().getAlpha() != 0;
         ErrorBarNode ebar = getErrorBarNode();
         psDoc.startElement(ebar);
         psDoc.renderPolyFill(coords, ebar.isStroked(), filled);
         psDoc.endElement();
      }

      // render the nominal data trace polyline itself last, so that it is painted on top of a filled error band!
      if(isStroked())
      {
         Point2D[] coords = getPlottedCoords(true, false);
         psDoc.renderPolyline(coords, null, 0, null, null, false );
      }

      psDoc.endElement();
   }

   /**
    * Helper method for toPostscript().  Handles PS rendering of <code>TraceNode</code> as a histogram with symbols and 
    * error bars (if specified) superimposed.
    * @param psDoc The Postscript document in which this <code>TraceNode</code> is rendered.
    */
   private void renderAsHistogram(PSDoc psDoc)
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      psDoc.startElement(this);

      // use HistogramVertexProducer to generate the vertices for the histogram bar shapes.
      Iterator<Point2D> iterator = new HistogramVertexProducer(true);
      List<Point2D> vertices = new ArrayList<Point2D>();
      while(iterator.hasNext())
      {
         Point2D next = (Point2D) iterator.next().clone();    // must clone, b/c HistogramVertexProducer reuses point
         vertices.add(next);
      }

      // if there are some histogram bars to render, invoke the Postscript document's appropriate utility method. 
      boolean filled = getFillColor().getAlpha() != 0;
      if((vertices.size() > 0) && (isStroked() || (filled && (barWidth > 0))))
      {
         if(barWidth == 0) 
         {
            Point2D[] coords = new Point2D[vertices.size()];
            for(int i = 0; i < coords.length; i++) coords[i] = (Point2D) vertices.get(i);
            psDoc.renderPolyline(coords, null, 0, null, null, false);
         }
         else if(!parentVP.isPolar()) 
            psDoc.renderPolygons(vertices, filled);
         else 
         {
            Point2D origin = parentVP.getPhysicalUserOrigin();
            Point2D p = new Point2D.Float(0, baseline);
            parentVP.userUnitsToThousandthInches(p);
            double baseRad = p.distance(origin);
            psDoc.renderConcentricWedges(origin, vertices, baseRad, filled);
         }
      }

      // render error bars and symbols at data point locations, but don't connect the points with a polyline. 
      // Symbols are rendered with their own draw styles IAW attributes on the "symbol" child!
      renderErrorBars(psDoc);
      SymbolNode symbolInfo = getSymbolNode();
      Marker symbol = symbolInfo.getType();
      double symSize = symbolInfo.getSizeInMilliInches();
      if(symSize > 0)
      {
         Point2D[] coords = getPlottedCoords(false, false);
         if(parentVP.isPolar() && (getBarWidth() == 0) && (symbolInfo.getType() != Marker.CIRCLE))
         {
            // special use case: "compass plot" -- each symbol is rotated by angle of emanating ray
            Iterator<Float> rotIterator = new CompassPlotAngleProducer();
            double[] rot = new double[coords.length];
            int i = 0;
            while(rotIterator.hasNext() && i < coords.length) rot[i++] = rotIterator.next();
            
            psDoc.startElement(symbolInfo);
            psDoc.renderMultipleAdornments(coords, rot, symbolInfo.getType(), symSize, symbolInfo.getCharacter());
            psDoc.endElement();
         }
         else 
            psDoc.renderPolyline(coords, symbol, symSize, symbolInfo, symbolInfo.getCharacter(), true);
      }

      psDoc.endElement();
   }

   /**
    * Helper method for toPostscript(). Handles PS rendering of the <code>TraceNode</code> in the "multi-trace" display 
    * node. This mode is intended only for multi-set data sources. Individual data sets are drawn as separate polylines 
    * styled IAW the subordinate <code>ErrorNode</code>, while the average across all of the data sets is drawn as
    * another polyline styled IAW the properties on this node and its subordinate <code>SymbolNode</code>.
    * 
    * @param psDoc The Postscript document in which this <code>TraceNode</code> is rendered.
    */
   private void renderAsMultitrace(PSDoc psDoc)
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      psDoc.startElement(this);

      // use MultiSetPointProducer to traverse the points in the composite polyline that will render all of the 
      // individual point sets. This ensures that, in the event that the polyline sub-sampling algorithm is engaged,
      // the Postscript output will replicate what's rendered onscreen.
      Iterator<Point2D> iterator = new MultiSetPointProducer();
      List<Point2D> ptList = new ArrayList<Point2D>();
      while(iterator.hasNext())
      {
         Point2D next = (Point2D) iterator.next().clone();    // must clone, b/c StdDevPointProducer reuses a Point2D
         ptList.add(next);
      }
      Point2D[] coords = new Point2D[ptList.size()];
      for(int i = 0; i < ptList.size(); i++) coords[i] = ptList.get(i);
      
      // in case the point list is really large
      ptList.clear();
      ptList = null;
      
      // if the composite polyline is not empty, render it IAW properties defined on subordinate ErrorBarNode
      if(coords.length > 1)
      {
         ErrorBarNode ebar = getErrorBarNode();
         Marker symbol = ebar.getEndCap();
         double symSize = ebar.getEndCapSizeInMilliInches();

         // draw the compound polyline (the individual polylines are not connected because the point sets are separated by 
         // undefined points in the coords array! Note the call to startElement() ensures that we use the stroke and fill 
         // properties defined on the "ebar" child rather on this TraceNode itself. Also, symbols are rendered with the 
         // same draw styles as the polylines (the "ebar" child does NOT have a "symbol" child!).
         psDoc.startElement(ebar);
         psDoc.renderPolyline(coords, symbol, symSize, null, null, false);
         psDoc.endElement();
      }
      
      // now draw the polyline that represents the AVERAGE across all point sets. Style attributes are now taken from 
      // this node itself. However, symbols on the average trace are rendered IAW styles set on SymbolNode subordinate!
      if(avg)
      {
         coords = getPlottedCoords(true, false);
         SymbolNode symbolInfo = getSymbolNode();
         psDoc.renderPolyline(coords, symbolInfo.getType(), symbolInfo.getSizeInMilliInches(), symbolInfo, 
               symbolInfo.getCharacter(), !isStroked());
      }

      // restore graphics state to what it was prior to rendering this MultiSetElement
      psDoc.endElement();
   }

   /**
    * Helper method for <code>toPostscript()</code>. It prepares the Postscript code to render any error bars defined 
    * for individual data points in this <code>TraceNode</code>.
    * @param doc The Postscript document into which this <code>TraceNode</code> is rendered.
    */
   private void renderErrorBars(PSDoc psDoc)
   {
      // if error bars are hidden, there's nothing to render!
      if(areErrorBarsHidden()) return;

     // get endcap attributes and line style for error bars
      ErrorBarNode ebar = getErrorBarNode();
      Marker endCap = ebar.getEndCap();
      double endCapSz = ebar.getEndCapSizeInMilliInches();

      // we'll need the parent graph viewport to convert user to physical coordinates
      FViewport2D parentVP = getParentViewport();
      
      DataSet set = getDataSet();
      int nPts = set.getDataSize(-1);
      if(nPts == 0) return;
      Point2D p0 = new Point2D.Double();              // the datum
      Point2D pStart = new Point2D.Double();          // the start and end points of the error bar line/arc
      Point2D pEnd = new Point2D.Double(); 
      Point2D pGraphOrigin = parentVP.getPhysicalUserOrigin();
      boolean isPolar = parentVP.isPolar();
      double xOffset = getXOffset();
      double yOffset = getYOffset();

      // error bars can be rendered with different stroke characteristics than the polyline connecting data points -- 
      // so we start a new element with a (possibly) different graphics state 
      psDoc.startElement(ebar);

      // iterate over the plotted data points in set (in case skip size > 1) and render any well-defined, nonzero 
      // error bars
      int nSkipBy = getSkip();
      for(int i = 0; i < nPts; i += nSkipBy)
      {
         // each error bar starts at the data point location.  If data point not well-defined, skip it!
         double x = set.getX(i, -1) + xOffset;
         double y = set.getY(i, -1) + yOffset;
         p0.setLocation(x, y);
         parentVP.userUnitsToThousandthInches(p0);
         if(!Utilities.isWellDefined(p0)) continue;

         double xStd = set.getXStdDev(i);
         double yStd = set.getYStdDev(i);

         // append an error bar along the primary or "x" coordinate if the std dev is nonzero and the physical endpoints 
         // of the error bar are well-defined
         int ecode = set.getXErrorBarStyle(i);
         if(Math.abs(ecode) < 2 && xStd != 0)
         {
            boolean isTwoSided = (ecode == 0);
            boolean isPlusOne = (ecode > 0);
            boolean isMinusOne = (ecode < 0);
            int iErrCode = isTwoSided ? 0 : (isPlusOne ? 1 : -1);

            // compute user coords of error bar endpoints: to be consistent across all error bar styles, we always 
            // start at -1 or 0 STD and end at 0 or +1 STD
            pStart.setLocation( x + ((isTwoSided||isMinusOne) ? -xStd : 0), y );
            pEnd.setLocation( x + ((isPlusOne||isTwoSided) ? xStd : 0), y );

            // translate to physical coords WRT parent SVG viewport; either point may become ill-defined
            parentVP.userUnitsToThousandthInches(pStart);
            parentVP.userUnitsToThousandthInches(pEnd);

            // if physical endpoints are well-defined, render the appropriately adorned error bar
            if(!Double.isNaN(pStart.getX()) && !Double.isNaN(pEnd.getX()))
               psDoc.renderErrorBar(true, iErrCode, pStart, pEnd, isPolar ? pGraphOrigin : null, endCap, endCapSz);
         }

         // append an error bar along the secondary or "y" coordinate if the std dev is nonzero and the physical 
         // endpoints of the error bar are well-defined
         ecode = set.getYErrorBarStyle(i);
         if(Math.abs(ecode) < 2 && yStd != 0)
         {
            boolean isTwoSided = (ecode == 0);
            boolean isPlusOne = (ecode > 0);
            boolean isMinusOne = (ecode < 0);
            int iErrCode = isTwoSided ? 0 : (isPlusOne ? 1 : -1);

            // compute user coords of error bar endpoints: to be consistent across all error bar styles, we always 
            // start at -1 or 0 STD and end at 0 or +1 STD
            pStart.setLocation( x, y + ((isTwoSided||isMinusOne) ? -yStd : 0) );
            pEnd.setLocation( x, y + ((isPlusOne||isTwoSided) ? yStd : 0) );

            // translate to physical coords WRT parent SVG viewport; either point may become ill-defined
            parentVP.userUnitsToThousandthInches( pStart );
            parentVP.userUnitsToThousandthInches( pEnd );

            // if physical endpoints are well-defined, render the appropriately adorned error bar
            if(!Double.isNaN(pStart.getX()) && !Double.isNaN(pEnd.getX()))
               psDoc.renderErrorBar(false, iErrCode, pStart, pEnd, isPolar ? pGraphOrigin : null, endCap, endCapSz);
         }
      }

      // restore graphics state changes introduced to render error bars
      psDoc.endElement();
   }
   
   
   // 
   // Object
   //

   /**
    * This override ensures that the rendering infrastructure for the <code>TraceNode</code> clone is independent of 
    * the node cloned. The clone will reference the same dataset, however!
    */
   @Override protected Object clone()
   {
      TraceNode copy = (TraceNode) super.clone();
      copy.paintedDisplayMode = null;
      copy.painters = new ArrayList<Painter>();
      copy.rBoundsSelf = null;
      return(copy);
   }

   
   /**
    * Helper class defines an iterator over the data points currently defined in the trace node's data source. It serves
    * both as the iterator implementation and the iterator provider (it simply provides fresh copies of itself).
    * 
    * <p>The iterator generates the points in the order they appear in the dataset, while respecting the trace node's
    * {@link #nSkipBy} parameter. If the total number of points to be generated exceeds 5000, a simple radial distance-
    * based sub-sampling algorithm can be optionally applied. Each data point is transformed from "user" coordinates to 
    * "painting" coordinates WRT the parent graph viewport. Thus, it is intended primarily for use while rendering the 
    * data trace into a graphics context.</p>
    * 
    * <p>Iterators provided by <code>DataPointProducer</code> do <em>not</em> support removal of a data point. Also, 
    * the class is <em>not</em> thread-safe. Since it is used to iterate over data during rendering (which occurs in 
    * a background thread), this could be problematic!</p>
    * 
    * @author  sruffner
    */
   private class DataPointProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      /**
       * Construct an iterator over the rendered data points in the trace node that does not allow sub-sampling of the
       * data point sequence.
       */
      DataPointProducer() { this(false); }
      
      /**
       * Construct an iterator over the rendered data points in the trace node.
       * @param allowSubSample True to allow sub-sampling of a data point sequence exceeding 5000 points.
       */
      DataPointProducer(boolean allowSubSample)
      {
         nSkipBy = getSkip();
         graphVP = getParentViewport();
         set = getDataSet();
         xOffset = getXOffset();
         yOffset = getYOffset();
         nPtsSoFar = 0;
         pCurrent = new Point2D.Double();
         
         int nTotal = set.getDataSize(-1) / nSkipBy;
         if(allowSubSample && nTotal > 5000)
         {
            double d = getStrokeWidth() * 2.0;
            subSampler = new RadialPolylineSubsampler( d <= 0 ? 20 : d);
         }
      }

      public Iterator<Point2D> iterator() { return(new DataPointProducer(subSampler != null)); }

      public boolean hasNext() { return(nPtsSoFar < set.getDataSize(-1)); }

      public Point2D next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         if(subSampler == null)
         {
            prepareNextPoint();
            return(pCurrent);
         }
         
         boolean keepPt = false;
         while(hasNext() && !keepPt)
         {
            prepareNextPoint();
            keepPt = subSampler.keep(pCurrent);
         }
         
         return(pCurrent);
      }

      private void prepareNextPoint()
      {
         pCurrent.setLocation(set.getX(nPtsSoFar, -1) + xOffset, set.getY(nPtsSoFar, -1) + yOffset);
         if(graphVP != null) graphVP.userUnitsToThousandthInches(pCurrent);

         nPtsSoFar += nSkipBy;
      }
      
      /** The trace node's plot skip interval (== 1 if no data points are skipped). */
      final int nSkipBy;
      /** The parent graph viewport converts each point from user units to rendering units. */
      final FViewport2D graphVP;
      /** The underlying data set source. */
      final DataSet set;
      /** The X-coordinate offset. */
      final double xOffset;
      /** The Y-coordinate offset. */
      final double yOffset;
      /** Number of data points processed thus far. */
      int nPtsSoFar;
      /** 
       * The current data point. This is reused to deliver each point. IT IS ASSUMED that the consumer will NOT store
       * a reference to this point, but will make a copy if needed.
       */
      Point2D pCurrent;
      /** Non-null if the polyline point sequence is being sub-sampled (when there are too many function samples). */
      RadialPolylineSubsampler subSampler = null;
   }

   /**
    * Helper class defines an iterator over the points that define a "trend line" for the data set in the display 
    * mode {@link DisplayMode#TRENDLINE}. Two kinds of trend lines are supported, depending on the value of the {@link 
    * #getSlidingWindowLength()} attribute:
    * <ul>
    * <li><i>len > 1</i>: A "sliding average" trace. Let N be the total # of points in the data set and let P be the 
    * sliding window length. The producer will generate a sequence of N-P+1 points, starting at index i=P-1, where
    * the Y-coordinate is the average of the preceding P Y-coordinate values, including the current Y: Y'(i) = [Y(i) + 
    * Y(i-1) + ... + Y(i-P+1)] / P.  (Ill-defined points are omitted from the average; if all P points are ill-defined,
    * then the sliding average at that point is ill-defined.) Note that the sliding average is intended for data sets
    * where X is monotonically increasing or decreasing, but this is NOT a requirement. Also, if P >= N-2, where N is
    * the number of data points in the set, then the "sliding average" trace is undefined and the producer will 
    * generate no points.</li>
    * <li><i>len == 1</i>: A least mean squares regression line through the data. In this case, the producer calculates
    * the endpoints of the regression line segment and iterates over the 2 points. If the regression line does not exist
    * for whatever reason, the producer generates no points.</li>
    * </ul>
    * 
    * <p>Note that these trend lines are never rendered in a polar context. In a semilogX, semilogY, or loglog context,
    * the regression line is computed for logX vs Y, X vs logY, or logX vs logY, respectively -- but will be undefined
    * if there are any non-positive X- and/or Y-coordinate data. In any scenario in which the trend line cannot be 
    * computed, this producer generates no points. Also, Z-coordinates in the data source, if any, are ignored.</p>
    */
   private class TrendLineProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      /** The parent graph viewport converts each data point from user units to rendering units. */
      final FViewport2D graphVP;
      /** The underlying data set source. */
      final DataSet set;
      /** Window length for sliding average. If 1, then regression line segment endpoints generated instead. */
      final int windowLen;
      /** The trace node's current X-coordinate offset. */
      final float xOfs;
      /** The trace node's current Y-coordinate offset. */
      final float yOfs;
      /** The endpoints of the LMS regression line, calculated at construction time. Unused for sliding avg trace. */
      final List<Point2D> endPts;
      

      int nPtsSoFar, nPtsTotal;
      Point2D pCurrent;
      
      TrendLineProducer()
      {
         graphVP = getParentViewport();
         set = getDataSet();
         windowLen = getSlidingWindowLength();
         xOfs = getXOffset();
         yOfs = getYOffset();
         endPts = new ArrayList<Point2D>();
         FGNGraph g = getParentGraph();
         FGNGraph.CoordSys sys = g.getCoordSys();
         boolean ok = (graphVP != null) && (g != null) && (!g.isPolar()) && (!g.is3D());
         
         nPtsSoFar = nPtsTotal = 0;
         pCurrent = null; 
         if(ok)
         {
            if(windowLen > 1 && (windowLen < set.getDataLength() - 2))
            {
               nPtsSoFar = windowLen - 1;
               nPtsTotal = set.getDataLength();
               pCurrent = new Point2D.Double();
            }
            else if(windowLen == 1)
            {
               calculateRegressionLineSegment(set, endPts, sys.isLogX(), sys.isLogY());
               if(endPts.size() == 2)
               {
                  if(xOfs != 0 || yOfs != 0)
                     for(Point2D p : endPts)
                        p.setLocation(p.getX() + xOfs, p.getY() + yOfs);
                  nPtsTotal = 2;
               }
            }
         }
      }
      
      @Override public Iterator<Point2D> iterator() { return(new TrendLineProducer()); }
      
      @Override public boolean hasNext() { return(nPtsSoFar < nPtsTotal); }
      @Override public Point2D next() 
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");
         
         if(windowLen > 1)
         {
            
            float xCurr = set.getX(nPtsSoFar, -1);
            if(!Utilities.isWellDefined(xCurr))
               pCurrent.setLocation(Double.NaN, Double.NaN);
            else
            {
               float ySum = 0;
               int nValid = 0;
               for(int i = 0; i<windowLen; i++)
               {
                  float x = set.getX(nPtsSoFar - i, -1), y = set.getY(nPtsSoFar - i, -1);
                  if(Utilities.isWellDefined(x, y))
                  {
                     ySum += y;
                     ++nValid;
                  }
               }
               if(nValid == 0)
                  pCurrent.setLocation(Double.NaN, Double.NaN);
               else
               {
                  pCurrent.setLocation(xCurr + xOfs, (ySum / nValid) + yOfs);
                  graphVP.userUnitsToThousandthInches(pCurrent);
               }
            }
            ++nPtsSoFar;
            return(pCurrent);
         }
         else
         {
            Point2D p = endPts.get(nPtsSoFar++);
            graphVP.userUnitsToThousandthInches(p);
            return(p);
         }
      }
   }
   
   /**
    * Helper class defines an iterator over the points that render the trace node in the staircase display mode. For
    * each data point (Xn, Yn) in the data source, two points are produced in order to generate the staircase: 
    * (Xn, Yn-1) and (Xn, Yn). Of course, for the first data point in the source, the first point in the pair is not
    * defined. In all other respects, the iterator behaves in the same manner as {@link DataPointProducer} -- it just
    * generates twice as many points!
    * 
    * <p>Iterators provided by <b>StairPointProducer</b> do <i>not</i> support removal of a data point. Also, the class
    * is <i>not</i> thread-safe. Since it is used to iterate over data during rendering (which occurs in a background 
    * thread), this could be problematic!</p>
    * 
    * @author  sruffner
    */
   private class StairPointProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      /**
       * Construct an iterator over the points that render the trace node as a staircase seqeunce. Do not allow 
       * sub-samppling of the underlying data source, even if it is very large.
       */
      StairPointProducer() { this(false); }
      
      /**
       * Construct an iterator over the points that render the trace node as a staircase sequence.
       * @param allowSubSample True to allow sub-sampling when the underlying data source size exceeds 5000.
       */
      StairPointProducer(boolean allowSubSample)
      {
         nSkipBy = getSkip();
         graphVP = getParentViewport();
         set = getDataSet();
         xOffset = getXOffset();
         yOffset = getYOffset();
         nPtsSoFar = 0;
         pCurrPair = new Point2D[] { 
               new Point2D.Double(Double.NaN, Double.NaN), new Point2D.Double(Double.NaN, Double.NaN)
         };
         nextIdx = 2;
         lastDatumIdx = -1;
         
         int nTotal = set.getDataSize(-1) / nSkipBy;
         if(allowSubSample && nTotal > 5000)
         {
            double d = getStrokeWidth() * 2.0;
            subSampler = new RadialPolylineSubsampler( d <= 0 ? 20 : d);
         }
      }

      public Iterator<Point2D> iterator() { return(new StairPointProducer(subSampler != null)); }

      public boolean hasNext() { return((nPtsSoFar < set.getDataSize(-1)) || (nextIdx < 2)); }

      public Point2D next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         if(nextIdx < 2) return(pCurrPair[nextIdx++]);
         
         if(subSampler == null)
         {
            prepareNextPointPair();
            return(pCurrPair[nextIdx++]);
         }
         
         boolean keepPt = false;
         while(hasNext() && !keepPt)
         {
            prepareNextPointPair();
            keepPt = subSampler.keep(pCurrPair[1]);
         }
         
         return(pCurrPair[nextIdx++]);
      }

      private void prepareNextPointPair()
      {
         pCurrPair[1].setLocation(set.getX(nPtsSoFar, -1) + xOffset, set.getY(nPtsSoFar, -1) + yOffset);
         if(graphVP != null) graphVP.userUnitsToThousandthInches(pCurrPair[1]);
         if(lastDatumIdx > -1)
         {
            pCurrPair[0].setLocation(set.getX(nPtsSoFar, -1) + xOffset, set.getY(lastDatumIdx,  -1) + yOffset);
            if(graphVP != null) graphVP.userUnitsToThousandthInches(pCurrPair[0]);
         }
         
         nextIdx = 0;
         lastDatumIdx = nPtsSoFar;
         nPtsSoFar += nSkipBy;
      }
      
      /** The trace node's plot skip interval (== 1 if no data points are skipped). */
      final int nSkipBy;
      /** The parent graph viewport converts each point from user units to rendering units. */
      final FViewport2D graphVP;
      /** The underlying data set source. */
      final DataSet set;
      /** The X-coordinate offset. */
      final double xOffset;
      /** The Y-coordinate offset. */
      final double yOffset;
      /** Number of data points processed thus far. */
      int nPtsSoFar;
      /** 
       * The pair of points rendered for the current data point (X,Y) in the underlying data source. If (Xo,Yo) is the
       * previous point in the source, then this array will contain (X,Yo) and (X,Y) -- transformed to the parent graph
       * viewport. The array is reused to deliver each point. IT IS ASSUMED that the consumer will NOT store a reference
       * to each point it receives, but will make a copy if needed.
       */
      Point2D[] pCurrPair;
      /** Index into the two-element point array, indicating the point to deliver next. */
      int nextIdx;
      /** Index (into underlying data source) of the last data point to be generated -- so we can get its Y value. */
      int lastDatumIdx;
      /** Non-null if the polyline point sequence is being sub-sampled (when there are too many function samples). */
      RadialPolylineSubsampler subSampler = null;
   }

   /**
    * Helper class defines an iterator over the points tracing out the polyline that is one standard deviation above and
    * below the trace node's nominal data set. It serves both as the iterator implementation and the iterator provider 
    * (it simply provides fresh copies of itself).
    * 
    * <p>The iterator is designed to be the location source for the {@link PolylinePainter} that
    * renders the +1 and -1STD polylines when the trace node is in the errorband display mode. Each data point generated
    * is supplied in "painting" coordinates WRT the parent graph viewport, and the trace node's {@link #nSkipBy} 
    * parameter is honored. Since the band between the +1 and -1STD polylines may be filled, it traverses the -1STD 
    * polyline backward. A single <i>(NaN, NaN)</i> point is inserted between the end of the +1STD polyline and the 
    * beginning of the -1STD polyline so that the two polylines are not connected to each other. Finally, if the total
    * number of data points in the underlying data set exceeds 5000, a simple radial distance-based sub-sampling 
    * algorithm is applied.</p>
    * 
    * <p>The iterator does <i>not</i> support removal of a data point. Also, the class is <i>not</i> thread-safe. Since 
    * it is used to iterate over data during rendering (which occurs in a background thread), this could be 
    * problematic!</p>
    * 
    * @author  sruffner
    */
   private class StdDevPointProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      /**
       * Construct an iterator over the +1 and -1STD polylines defined by the trace node's data set source.
       * <p>Let <i>{Xi, Y(Xi)}</i> for <i>i = 0..n-1</i> represent the "nominal" data trace. The iterator traverses 
       * <i>{Xi, Y(Xi) + DY(Xi)}</i> and <i>{Xi, Y(Xi) - DY(Xi)}</i>, where <i>DY(Xi)</i> is the standard deviation in 
       * <i>Y</i> at <i>X = Xi</i>. The iterator will traverse the -1STD trace backwards, creating a single path for the 
       * purposes of filling the "error band" between +1 and -1STD. An undefined point is inserted between the ends of 
       * the two traces so that they are not connected when the path is stroked.
       */
      public StdDevPointProducer()
      {
         nSkipBy = getSkip();
         graphVP = getParentViewport();
         set = getDataSet();
         xOffset = getXOffset();
         yOffset = getYOffset();
         nPtsSoFar = 0;
         which = 1;
         pCurrent = new Point2D.Double();
         
         
         int nTotal = set.getDataSize(-1) / nSkipBy;
         if(nTotal > 5000)
         {
            double d = getStrokeWidth() * 2.0;
            subSampler = new RadialPolylineSubsampler( d <= 0 ? 20 : d);
         }
      }

      public Iterator<Point2D> iterator() { return(new StdDevPointProducer()); }

      public boolean hasNext()
      {
         if(set.getDataSize(-1) == 0) return(false);
         return( !(which == -1 && nPtsSoFar < 0) );
      }

      public Point2D next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         if(subSampler == null)
         {
            prepareNextPoint();
            return(pCurrent);
         }
         
         boolean keepPt = false;
         while(hasNext() && !keepPt)
         {
            prepareNextPoint();
            keepPt = subSampler.keep(pCurrent);
         }
         
         return(pCurrent);
      }

      private void prepareNextPoint()
      {
         // between the +1STD and -1STD traces, insert one undefined point to create a gap in the (stroked) path
         if(which > 0 && nPtsSoFar >= set.getDataSize(-1))
         {
            pCurrent.setLocation(Double.NaN, Double.NaN);
            nPtsSoFar -= nSkipBy;
            which = -1;
            return;
         }

         pCurrent.setLocation(set.getX(nPtsSoFar, -1) + xOffset, 
               set.getY(nPtsSoFar, -1) + yOffset + which*set.getYStdDev(nPtsSoFar));
         if(graphVP != null) graphVP.userUnitsToThousandthInches(pCurrent);

         // advance to next point
         nPtsSoFar += which*nSkipBy;
      }
      
      public void remove() { throw new UnsupportedOperationException("Removal not supported by this iterator."); }
      
      /** Set to 1 or -1 while traversing points on the +1 or -1STD trace, respectively. */
      int which;
      
      /** The trace node's plot skip interval (== 1 if no data points are skipped). */
      final int nSkipBy;
      /** The parent graph viewport converts each point from user units to rendering units. */
      final FViewport2D graphVP;
      /** The underlying data set source. */
      final DataSet set;
      /** The X-coordinate offset. */
      final double xOffset;
      /** The Y-coordinate offset. */
      final double yOffset;
      /** Number of data points processed thus far. */
      int nPtsSoFar;
      /** 
       * The current data point. This is reused to deliver each point. IT IS ASSUMED that the consumer will NOT store
       * a reference to this point, but will make a copy if needed.
       */
      Point2D pCurrent;
      /** Non-null if the polyline point sequence is being sub-sampled (when there are too many function samples). */
      RadialPolylineSubsampler subSampler = null;
   }


   /**
    * <code>HistogramVertexProducer</code> is a helper class that provides an iterator over the vertices that define 
    * the outlines of histogram bars when the enclosing <code>TraceNode</code> is in the histogram display mode. It 
    * serves both as the iterator implementation and the iterator provider (it simply provides fresh copies of itself).
    * 
    * <p>Histogram "bars" come in several different forms depending on the <em>barWidth</em> property of the enclosing 
    * <code>TraceNode</code> and whether or not the parent graph is in polar coordinates.
    * <ul>
    *    <li>If the bar width is zero, the histogram bars are simply line segments. Under the hood, a 
    *    <code>LineSegmentPainter</code> is used to render the histogram, and <code>HistogramVertexProducer</code> 
    *    serves as its "location source", delivering a pair of points for each well-defined line in the histogram.</li>
    *    <li>If the bar width is nonzero and the parent graph is <em>not</em> polar, each histogram bar is a closed 
    *    rectangle. In this case, a <code>PolylinePainter</code> is used to render the histogram, and 
    *    <code>HistogramVertexProducer</code> delivers the four vertices of each rectangle, separated by one undefined
    *    point so that the <code>PolylinePainter</code> closes each rectangle before starting the next one.</li>
    *    <li>Finally, if the bar width is nonzero and the parent graph is polar, each histogram bar becomes a circular 
    *    pie wedge or radial section (depending on the value of the <em>baseline</em> property). In this case, an 
    *    appropriately configured <code>CircularArcPainter</code> renders the histogram, and 
    *    <code>HistogramVertexProducer</code> delivers a pair of points in polar coordinates -- (theta0,r) and 
    *    (theta1,r), where r is the distance from the polar origin in logical units -- for each well-defined arc. If 
    *    this producer is generating vertices for the Postscript rendering, the vertices are left in "painting" 
    *    coordinates -- the relevant <code>PSDoc</code> method handles the conversion to polar coordinates. <em>[Such 
    *    usage would seem extremely rare; nevertheless it is supported.]</em></li>
    * </ul>
    * </p>
    * 
    * <p>Iterators provided by <code>HistogramVertexProducer</code> do <em>not</em> support removal of a vertex. Also, 
    * the class is <em>not</em> thread-safe. Since it is used to iterate over data during rendering (which occurs in 
    * a background thread), this could be problematic!</p>
    * 
    * @author  sruffner
    */
   private class HistogramVertexProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      final int nSkipBy;
      final FViewport2D graphVP;
      final DataSet set;
      final double xOffset;
      final double yOffset;
      final double barWidth;
      final double baseline;
      final double halfSignedBarW;
      final boolean isLines;
      final boolean isPolar;
      final Point2D polarOrigin;
      final boolean forPS;
      
      int nPtsSoFar;
      int nVertsSoFar;
      Point2D[] barVertices;

      /**
       * Retrieve a new iterator for this <code>HistogramVertexProducer</code>. This merely returns a fresh copy of 
       * <code>HistogramVertexProducer</code> itself, which acts both as <code>Iterable</code> and <code>Iterator</code>.
       */
      public Iterator<Point2D> iterator() { return(new HistogramVertexProducer(forPS)); }

      /**
       * Construct a <code>HistogramVertexProducer</code>, which provides a special iterator over the vertices that 
       * define the outline of the histogram bars when the enclosing instance of <code>TraceNode</code> is in the
       * histogram display mode. See class header for a complete description. 
       * @param forPostscript Flag set if this producer will generate vertices for a Postscript rendering. When the 
       * parent graph is polar and the bar width is nonzero, the vertices are left in "painting coordinate" if this 
       * flag is set, rather than being converted to polar form. Also, if the bar width is zero, each histogram line is 
       * represented by three vertices instead of two, where the last vertex is always ill-defined to introduce a gap 
       * between one line segment and the next. This is necessary because we use <code>PSDoc.renderPolyline()</code> to 
       * draw the histogram when the bar width is zero.
       */
      public HistogramVertexProducer(boolean forPostscript)
      {
         forPS = forPostscript;
         nSkipBy = getSkip();
         graphVP = getParentViewport();
         set = getDataSet();
         xOffset = getXOffset();
         yOffset = getYOffset();
         barWidth = getBarWidth();
         baseline = getBaseline();

         if(set.isSampledSeries())
            halfSignedBarW = barWidth / ((set.getDX()<0) ? -2.0 : 2.0);
         else
            halfSignedBarW = barWidth / 2.0;

         isLines = (barWidth == 0.0);
         isPolar = graphVP.isPolar();
         polarOrigin = (graphVP != null) ? graphVP.getPhysicalUserOrigin() : new Point2D.Double(0,0);

         nPtsSoFar = 0;
         nVertsSoFar = (barWidth==0 || isPolar) ? 2 : 5;
         if(barWidth == 0 && forPS) nVertsSoFar = 3;
         barVertices = new Point2D[nVertsSoFar];
         for(int i = 0; i < nVertsSoFar; i++) 
            barVertices[i] = new Point2D.Double(0,0);
      }

      public boolean hasNext()
      {
         if(graphVP == null) return(false);
         if(set.getDataSize(-1) == 0) return(false);
         return( !(nPtsSoFar >= set.getDataSize(-1) && nVertsSoFar >= barVertices.length) );
      }

      public Point2D next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         // if we're done with the current histogram bar, compute vertices for the next one
         if(nVertsSoFar >= barVertices.length)
         {
            boolean gotNextBar = false;
            while(nPtsSoFar < set.getDataSize(-1) && !gotNextBar)
            {
               double x = set.getX(nPtsSoFar, -1) + xOffset;
               double y = set.getY(nPtsSoFar, -1) + yOffset;
               
               // prepare vertices needed to render next bin of histogram. Vertices prepared depend upon whether we're 
               // rendering lines, arcs, or rectangles.  These are in "user" coords.
               if(isLines)
               {
                  barVertices[0].setLocation(x, baseline);
                  barVertices[1].setLocation(x, y);
                  if(forPS) barVertices[2].setLocation(0, 0);   // so "well-defined" test below works!
               }
               else if(isPolar)
               {
                  barVertices[0].setLocation(x - halfSignedBarW, y);
                  barVertices[1].setLocation(x + halfSignedBarW, y);
               }
               else
               {
                  barVertices[0].setLocation(x - halfSignedBarW, baseline);
                  barVertices[1].setLocation(x + halfSignedBarW, baseline);
                  barVertices[2].setLocation(x + halfSignedBarW, y);
                  barVertices[3].setLocation(x - halfSignedBarW, y);
                  barVertices[4].setLocation(0, 0);   // so "well-defined" test below works!
               }

               // convert to logical "painting" coordinates in the parent graph's viewport. If any of the vertices of 
               // the histogram bar are ill-defined, then we simply do not render it.
               graphVP.userUnitsToThousandthInches(barVertices);
               gotNextBar = Utilities.isWellDefined(barVertices);

               // if we got a valid histogram bar...
               if(gotNextBar)
               {
                  // if we're doing rectangles, set last point to (NaN,NaN) so PolylinePainter will close rectangle.
                  // Likewise if we're doing histogram lines for a Postscript rendering
                  if(barVertices.length == 5) barVertices[4].setLocation(Double.NaN, Double.NaN);
                  if(isLines && forPS) barVertices[2].setLocation(Double.NaN, Double.NaN);
                  
                  // if we're doing arcs, we must put vertices in polar coordinate form -- except in Postscript case
                  if(isPolar && (!isLines) && (!forPS))
                  {
                     double r = polarOrigin.distance(barVertices[0]);
                     barVertices[0].setLocation(x - halfSignedBarW, r);
                     barVertices[1].setLocation(x + halfSignedBarW, r);
                  }
               }

               // skip forward to next datum to be plotted
               nPtsSoFar += nSkipBy;
            }

            // degenerate case: If some histogram bars are ill-defined for whatever reason, we might have no vertices 
            // left to supply. In this case, the last bar contains ill-defined points, which painters should ignore.
            // The next call to hasNext() will return false.
            if(!gotNextBar)
            {
               for(int i = 0; i<barVertices.length; i++) barVertices[i].setLocation(Double.NaN, Double.NaN);
            }

            nVertsSoFar = 0;
         }

         Point2D nextVtx = barVertices[nVertsSoFar];
         ++nVertsSoFar;
         return(nextVtx);
      }

      public void remove() { throw new UnsupportedOperationException("Removal not supported by this iterator."); }
   }

   /**
    * Helper class that serves as the "rotation angle producer" for a very narrow use case: a polar histogram plot with 
    * zero bar width (rays emanate from origin or baseline radius to each polar data point) and non-circle marker 
    * symbols with nonzero size. In this case (a "compass plot" in Matlab), authors would like the marker symbol at the
    * end of each ray to be rotated so that it is aligned with that ray. In all other use cases, it produces nothing!
    * 
    * @author sruffner
    */
   private class CompassPlotAngleProducer implements Iterable<Float>, Iterator<Float>
   {
      final int nSkipBy;
      final FViewport2D graphVP;
      final DataSet set;
      final double xOffset;
      final double yOffset;
      final double baseline;
      final boolean applicable;
      int nPtsSoFar;

      public Iterator<Float> iterator() { return(new CompassPlotAngleProducer()); }

      CompassPlotAngleProducer()
      {
         nSkipBy = getSkip();
         graphVP = getParentViewport();
         set = getDataSet();
         xOffset = getXOffset();
         yOffset = getYOffset();
         baseline = getBaseline();
         
         SymbolNode sn = getSymbolNode();
         applicable = graphVP.isPolar() && (getMode() == DisplayMode.HISTOGRAM) && (getBarWidth() == 0) && 
               (sn.getSizeInMilliInches() > 0) && (sn.getType() != Marker.CIRCLE);
         nPtsSoFar = 0;
      }

      public boolean hasNext() { return(applicable && nPtsSoFar < set.getDataSize(-1)); }

      public Float next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         double angle = set.getX(nPtsSoFar, -1) + xOffset;
         double radius = set.getY(nPtsSoFar, -1) + yOffset;
         if(radius < baseline) angle += 180;
         if(!(Utilities.isWellDefined(angle) && Utilities.isWellDefined(radius) && radius > 0)) angle = 0;

         nPtsSoFar += nSkipBy;

         return(new Float(angle));
      }


      public void remove(){ throw new UnsupportedOperationException("Removal not supported by this iterator."); }
   }
   
   /**
    * Helper class defines an iterator over all of the data points in all of the individual point sets defined in the 
    * trace node's data set source. It is intended for use when rendering one of the collection-type datasets in the 
    * "multitrace" display mode. It serves both as the iterator implementation and the iterator provider (it simply 
    * provides fresh copies of itself).
    * 
    * <p>By design, the iterator serves as the location source for a {@link PolylinePainter} that
    * renders the polylines for the two or more individual but related point sets. In this role, it iterates over 
    * the point sets in the order they appear in the data set, transforming each point from "user" coordinates to 
    * "painting" coordinates WRT the parent graph viewport. A single <i>(NaN, NaN)</i> point is inserted between the end
    * of one point set and the beginning of the next -- so that the polylines are not connected to each other. However, 
    * if the data set contains only a single point set, the iterator acts as though it were empty. On the other hand, if
    * the total number of points across all of the member sets -- while honoring the trace node's {@link #nSkipBy} 
    * parameter -- exceeds 5000, then a simple radial distance-based polyline sub-sampling algorithm is applied to 
    * reduce the total length of the polyline rendered.</p>
    *  
    * <p>The iterator does <i>not</i> support removal of a data point. Also, the class is <i>not</i> thread-safe. Since 
    * it is used to iterate over data during rendering (which occurs in a background thread), this could be 
    * problematic!</p>
    * 
    * @author  sruffner
    */
   private class MultiSetPointProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      MultiSetPointProducer()
      {
         nSkipBy = getSkip();
         graphVP = getParentViewport();
         set = getDataSet();
         xOffset = getXOffset();
         yOffset = getYOffset();
         nPtsSoFar = 0;
         nSetsSoFar = 0;
         insertGap = false;
         pCurrent = new Point2D.Double();
         
         int nSets = set.getNumberOfSets();
         int nTotal = nSets * set.getDataSize(-1);
         if(nSets > 1 && nTotal > 5000)
         {
            double d = getStrokeWidth() * 2.0;
            subSampler = new RadialPolylineSubsampler( d <= 0 ? 20 : d);
         }
      }

      public Iterator<Point2D> iterator() { return(new MultiSetPointProducer()); }

      public boolean hasNext()
      {
         if(graphVP == null) return(false);
         int nSets = set.getNumberOfSets();
         return( (nSets > 1) && (nSetsSoFar < nSets) );
      }

      public Point2D next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         if(subSampler == null)
         {
            prepareNextPoint();
            return(pCurrent);
         }
         
         boolean keepPt = false;
         while(hasNext() && !keepPt)
         {
            prepareNextPoint();
            keepPt = subSampler.keep(pCurrent);
         }
         
         return(pCurrent);
      }

      private void prepareNextPoint()
      {
         // between point sets, insert one undefined point to create a gap in the rendered polyline
         if(insertGap)
         {
            pCurrent.setLocation(Double.NaN, Double.NaN);
            insertGap = false;
            return;
         }

         pCurrent.setLocation(set.getX(nPtsSoFar, nSetsSoFar) + xOffset, set.getY(nPtsSoFar, nSetsSoFar) + yOffset);
         if(graphVP != null) graphVP.userUnitsToThousandthInches(pCurrent);

         nPtsSoFar += nSkipBy;
         if(nPtsSoFar >= set.getDataSize(nSetsSoFar))
         {
            ++nSetsSoFar;
            nPtsSoFar = 0;
            if(nSetsSoFar < set.getNumberOfSets()) insertGap = true;
         }
      }
      
      public void remove() { throw new UnsupportedOperationException("Removal not supported by this iterator."); }
      
      /** The trace node's plot skip interval (== 1 if no data points are skipped). */
      final int nSkipBy;
      /** The parent graph viewport converts each point from user units to rendering units. */
      final FViewport2D graphVP;
      /** The underlying data set source. */
      final DataSet set;
      /** The X-coordinate offset. */
      final double xOffset;
      /** The Y-coordinate offset. */
      final double yOffset;
      /** Number of data points processed thus far. */
      int nPtsSoFar;
      /** Number of member points sets processed thus far. */
      int nSetsSoFar;
      /** 
       * Flag set if next point should be (NaN,NaN) to insert a discontinuity between end of the polyline for one
       * member point set and the start of the polyline for the next member set.
       */
      boolean insertGap = false;
      /** 
       * The current data point. This is reused to deliver each point. IT IS ASSUMED that the consumer will NOT store
       * a reference to this point, but will make a copy if needed.
       */
      Point2D pCurrent;
      /** Non-null if the polyline point sequence is being sub-sampled (when there are too many function samples). */
      RadialPolylineSubsampler subSampler = null;
   }

   
   /**
    * <code>ErrorBarPainter</code> is a custom <code>Painter</code> implementation that renders any error bars defined 
    * on an instance of a <code>TraceNode</code>.
    * 
    * <p>This custom painter implicitly assumes that all rendering occurs on a graphics context that is configured so 
    * that it represents the viewport of the node's parent <code>GraphNode</code>, in logical rendering coordinates. It 
    * uses the parent graph's viewport to convert error bar endpoints from "user" to "rendering" coordinates.</p>
    * 
    * <p>Error bars are rendered as line segments or circular arcs (for theta error bars in a polar plot) with optional 
    * endcap adornments. The stroke and endcap characteristics for the error bars are based on attributes of the 
    * <code>ErrorBarNode</code> component of the <code>TraceNode</code> -- so that the error bars can be stroked 
    * differently from the polyline that connects the data points. Each datum's error bar style determines whether the 
    * associated error bar is 2-sided, minus-one-sided, plus-one-sided, or hidden. <em>NOTE</em> that if the 
    * <code>TraceNode</code> has a plot skip size <em><code>N &gt; 1</code></em>, then the method only includes the 
    * error bar for every <em><code>N</code></em>-th data point in the rendering.</p>
    * 
    * <p>The stroke pattern style for the <code>ErrorBarNode</code> is only applied when rendering the line or arc that 
    * represents the error bar itself. The endcaps are always stroked with a solid line. If that node's <em>hide</em> 
    * flag is set, then the error bars are not rendered at all!</p>
    * 
    * <p>To be consistent regarding the orientation of endcap adornments, we always draw the error bar line/arc from a 
    * starting point at -1STD (for minus-one and 2-sided bars) or 0 (for plus-one bar) to an ending point at +1STD (for 
    * plus-one and 2-sided bars) or 0 (for minus-one bar). The <code>Marker</code> enumeration that defines the endcap 
    * adornments supported in <em>DataNav</em> ensure that each such shape is oriented so that the positive x-axis in 
    * "design space" will be colinear with the error bar (or with the line tangent to the error bar arc at the endpoint)
    * and points in the same direction as a ray from the first to the second endpoint (or the same direction as the 
    * tangent ray leaving the endpoint in a CCW direction). Adornments at the first endpoint of the error bar line/arc 
    * are flipped 180deg IF they are NOT vertically symmetric.</p>
    * 
    * <p>Special case: If the error bar endcap is a "lineup" or "linedown" adornment, we adjust the adornment's 
    * position and length by a half stroke width to get a cleaner join.</p>
    * 
    * @author  sruffner
    */
   private class ErrorBarPainter extends Painter
   {
      /** Construct a painter that will render any error bars for the enclosing instance of <code>TraceNode</code>. */
      public ErrorBarPainter()
      {
         super();
      }

      /**
       * Interval -- in # data points processed -- at which painter reports progess and checks for render task 
       * cancellation.
       */
      private final static int PROGRESSINTV = 50;

      @Override
      protected boolean paintInternal(Graphics2D g2d)
      {
         // if error bars are hidden, there's nothing to render!
         if(areErrorBarsHidden()) return(true);

         // make sure there is some data to render!
         DataSet set = getDataSet();
         int nPts = set.getDataSize(-1);
         if(nPts == 0)
            return(true);

         // get endcap info
         ErrorBarNode ebar = getErrorBarNode();
         double endCapSz = ebar.getEndCapSizeInMilliInches();
         Marker endCapType = ebar.getEndCap();

         // get the marker that defines the endcap shape. If the endcap size is zero, there is NO endcap shape. For the 
         // "lineup" and "linedown" endcaps, IF the stroke endcap style is butt-end, we won't get a nice join between
         // the lineup/linedown and the error bar itself. In this case we create a custom shape that shifts the 
         // lineup/down marker down/up by 1/2 a stroke width. DRAW A PICTURE!
         PaintableShape endCapShape = null;
         if(endCapSz > 0)
         {
            if(ebar.getStrokeEndcap() != StrokeCap.BUTT && (endCapType == Marker.LINEUP || endCapType == Marker.LINEDOWN))
            {
               double adj = ebar.getStrokeWidth()/2;
               if(endCapType == Marker.LINEUP)
                  endCapShape = new BasicPaintableShape(new Line2D.Double(0, -adj, 0, endCapSz), false, false, true);
               else
                  endCapShape = new BasicPaintableShape(new Line2D.Double(0, adj, 0, -endCapSz), false, false, true);

               // since the "design shape" is at the final size, set scaled size to 1
               endCapSz = 1;
            }
            else
               endCapShape = endCapType;
         }

         // we'll need the parent graph viewport to convert user to physical coordinates
         FViewport2D parentVP = getParentViewport();
         if(parentVP == null) return(true);

         // we use a LineSegmentPainter or CircularArcPainter to render each error bar separately. 
         List<Point2D> endPts = new ArrayList<Point2D>();
         Point2D pStart = new Point2D.Double();
         Point2D pEnd = new Point2D.Double();
         endPts.add(pStart);
         endPts.add(pEnd);
         LineSegmentPainter linePainter = new LineSegmentPainter(ebar, endPts);
         CircularArcPainter arcPainter = null;
         if(parentVP.isPolar())
            arcPainter = new CircularArcPainter(ebar, endPts, parentVP.getPhysicalUserOrigin());

         // endcaps are always stroked solid; otherwise they have same graphic style as error bar itself
         PainterStyle endCapStyle = ebar;
         if(!endCapStyle.isStrokeSolid())
         {
            endCapStyle = BasicPainterStyle.createBasicPainterStyle(
                  ebar.getFont(), ebar.getStrokeWidth(), null, ebar.getStrokeColor(), ebar.getFillColor());
         }

         // keep track of #error bars processed so we can report progress
         int nBarsDone = 0;

         // iterate over plotted data points in set (in case skip size > 1) and render any well-defined, nonzero error 
         // bars
         Point2D p0 = new Point2D.Double();                             // the datum
         Point2D pGraphOrigin = parentVP.getPhysicalUserOrigin();       // need this for polar graphs
         boolean isPolar = parentVP.isPolar();
         int nSkipBy = getSkip();
         double xOffset = getXOffset();
         double yOffset = getYOffset();
         for(int i=0; i<nPts; i+=nSkipBy)
         {
            // get next data point location; if it is not well-defined, skip it!
            double x = set.getX(i, -1) + xOffset;
            double y = set.getY(i, -1) + yOffset;
            p0.setLocation(x, y);
            parentVP.userUnitsToThousandthInches(p0);
            if(!Utilities.isWellDefined(p0)) 
            {
               ++nBarsDone;
               continue;
            }

            double xStd = set.getXStdDev(i);
            double yStd = set.getYStdDev(i);

            // error bar for the "x" or "theta" coordinate, if applicable
            int ecode = set.getXErrorBarStyle(i);
            if(Math.abs(ecode) < 2 && xStd != 0)
            {
               boolean isTwoSided = (ecode==0);
               boolean isPlusOne = (ecode>0);
               boolean isMinusOne = (ecode<0);

               // compute user coords of error bar endpoints
               pStart.setLocation( x + ((isTwoSided||isMinusOne) ? -xStd : 0), y );
               pEnd.setLocation( x + ((isPlusOne||isTwoSided) ? xStd : 0), y );

               // translate to physical coords WRT parent SVG viewport; either point may become ill-defined
               parentVP.userUnitsToThousandthInches( pStart );
               parentVP.userUnitsToThousandthInches( pEnd );

               // if physical endpoints are well-defined, render the appropriately adorned error bar
               if(!Double.isNaN(pStart.getX()) && !Double.isNaN(pEnd.getX()))
               {
                  if(isPolar)
                  {
                     // endpoints need to be in polar coordinates, with radius in logical units of graphic context
                     double r = pGraphOrigin.distance(p0);
                     pStart.setLocation(x + ((isTwoSided||isMinusOne) ? -xStd : 0), r);
                     pEnd.setLocation(x + ((isPlusOne||isTwoSided) ? xStd : 0), r);
                     arcPainter.setAdornment(endCapStyle, endCapShape, (float)endCapSz, null,
                           isTwoSided||isMinusOne, isTwoSided||isPlusOne, false, true);
                     arcPainter.render(g2d, null);
                  }
                  else
                  {
                     linePainter.setAdornment(endCapStyle, endCapShape, (float)endCapSz, null, 
                           isTwoSided||isMinusOne, isTwoSided||isPlusOne, false, true);
                     linePainter.render(g2d,null);
                  }
               }
            }

            // error bar for the "y" or "radial" coordinate, if applicable
            ecode = set.getYErrorBarStyle(i);
            if(Math.abs(ecode) < 2 && yStd != 0)
            {
               boolean isTwoSided = (ecode == 0);
               boolean isPlusOne = (ecode > 0);
               boolean isMinusOne = (ecode < 0);

               // compute user coords of error bar endpoints
               pStart.setLocation( x, y + ((isTwoSided||isMinusOne) ? -yStd : 0) );
               pEnd.setLocation( x, y + ((isPlusOne||isTwoSided) ? yStd : 0) );

               // translate to physical coords WRT parent SVG viewport; either point may become ill-defined
               parentVP.userUnitsToThousandthInches( pStart );
               parentVP.userUnitsToThousandthInches( pEnd );

               // if physical endpoints are well-defined, render the appropriately adorned error bar
               if( !Double.isNaN(pStart.getX()) && !Double.isNaN(pEnd.getX()) )
               {
                  linePainter.setAdornment(endCapStyle, endCapShape, (float)endCapSz, null, 
                        isTwoSided||isMinusOne, isTwoSided||isPlusOne, false, true);
                  linePainter.render(g2d,null);
               }
            }

            // report progress and check for job cancellation after processing every PROGRESSINTV error bars
            ++nBarsDone;
            if(nBarsDone >= PROGRESSINTV)
            {
               nBarsDone = 0;
               if(stopPainting())
                  return(false);
            }
         }

         return(true);
      }

      @Override protected void recalcBounds2D(Rectangle2D r)
      {
         // start with an empty rectangle
         r.setFrame(0, 0, 0, 0);

         // if error bars are hidden, there's nothing to render!
         if(areErrorBarsHidden()) return;

         // make sure there is some data to render!
         DataSet set = getDataSet();
         int nPts = set.getDataSize(-1);
         if(nPts == 0)
            return;

         // get endcap info
         ErrorBarNode ebar = getErrorBarNode();
         double endCapSz = ebar.getEndCapSizeInMilliInches();

         // we'll need the parent graph viewport to convert user to physical coordinates
         FViewport2D parentVP = getParentViewport();

         // bounds of rectangle that encompasses all well-defined error bars
         boolean gotValidBar = false;
         double xMin = Double.POSITIVE_INFINITY;
         double xMax = Double.NEGATIVE_INFINITY;
         double yMin = Double.POSITIVE_INFINITY;
         double yMax = Double.NEGATIVE_INFINITY;

         // iterate over plotted data points in set (in case skip size > 1): find rect bounding all well-defined 
         // error bars (ignoring endcaps for now).
         Point2D p0 = new Point2D.Double();                             // the datum
         Point2D pStart = new Point2D.Double();                         // error bar endpoints
         Point2D pEnd = new Point2D.Double();
         int nSkipBy = getSkip();
         double xOffset = getXOffset();
         double yOffset = getYOffset();
         for(int i=0; i<nPts; i+=nSkipBy)
         {
            // get next data point location; if it is not well-defined, skip it!
            double x = set.getX(i, -1) + xOffset;
            double y = set.getY(i, -1) + yOffset;
            p0.setLocation(x, y);
            parentVP.userUnitsToThousandthInches(p0);
            if(!Utilities.isWellDefined(p0)) continue;

            double xStd = set.getXStdDev(i);
            double yStd = set.getYStdDev(i);

            // error bar for the "x" or "theta" coordinate, if applicable
            int ecode = set.getXErrorBarStyle(i);
            if(Math.abs(ecode) < 2 && xStd != 0)
            {
               boolean isTwoSided = (ecode==0);
               boolean isPlusOne = (ecode>0);
               boolean isMinusOne = (ecode<0);

               // compute user coords of error bar endpoints
               pStart.setLocation( x + ((isTwoSided||isMinusOne) ? -xStd : 0), y );
               pEnd.setLocation( x + ((isPlusOne||isTwoSided) ? xStd : 0), y );

               // translate to physical coords WRT parent SVG viewport; either point may become ill-defined
               parentVP.userUnitsToThousandthInches( pStart );
               parentVP.userUnitsToThousandthInches( pEnd );

               // if physical endpoints are well-defined, update bounding box
               if(!Double.isNaN(pStart.getX()) && !Double.isNaN(pEnd.getX()))
               {
                  gotValidBar = true;
                  if(pStart.getX() < xMin) xMin = pStart.getX();
                  if(pStart.getX() > xMax) xMax = pStart.getX();
                  if(pStart.getY() < yMin) yMin = pStart.getY();
                  if(pStart.getY() > yMax) yMax = pStart.getY();
 
                  if(pEnd.getX() < xMin) xMin = pEnd.getX();
                  if(pEnd.getX() > xMax) xMax = pEnd.getX();
                  if(pEnd.getY() < yMin) yMin = pEnd.getY();
                  if(pEnd.getY() > yMax) yMax = pEnd.getY();
               }

               if(parentVP.isPolar() && pStart.getY() == pEnd.getY())
               {
                  // if error bar arc passes thru 0, 90, 180 or 270, then the arc's bounds include one or more of the 
                  // edges of the quare that bounds the containing circle
                  Point2D center = parentVP.getPhysicalUserOrigin();
                  double start = x + ((isTwoSided||isMinusOne) ? -xStd : 0);
                  double end = x + ((isPlusOne||isTwoSided) ? xStd : 0);
                  if(end < start) end += 360;

                  
                  while(end >= start)
                  {
                     int qtrAngle = (((int)(end/90)) * 90);
                     if(qtrAngle <= start) break;

                     qtrAngle = qtrAngle % 360;
                     if(qtrAngle == 0 && (center.getX() + y) > xMax) xMax = center.getX() + y;
                     else if(qtrAngle == 90 && (center.getY() + y) > yMax) yMax = center.getY() + y;
                     else if(qtrAngle == 180 && (center.getX() - y) < xMin) xMin = center.getX() - y;
                     else if(qtrAngle == 270 && (center.getY() - y) < yMin) yMin = center.getY() - y;

                     end -= 90;
                  }

               }
            }

            // error bar for the "y" or "radial" coordinate, if applicable
            ecode = set.getYErrorBarStyle(i);
            if(Math.abs(ecode) < 2 && yStd != 0)
            {
               boolean isTwoSided = (ecode == 0);
               boolean isPlusOne = (ecode > 0);
               boolean isMinusOne = (ecode < 0);

               // compute user coords of error bar endpoints
               pStart.setLocation( x, y + ((isTwoSided||isMinusOne) ? -yStd : 0) );
               pEnd.setLocation( x, y + ((isPlusOne||isTwoSided) ? yStd : 0) );

               // translate to physical coords WRT parent viewport; either point may become ill-defined
               parentVP.userUnitsToThousandthInches( pStart );
               parentVP.userUnitsToThousandthInches( pEnd );

               // if physical endpoints are well-defined, update bounding box
               if(!Double.isNaN(pStart.getX()) && !Double.isNaN(pEnd.getX()))
               {
                  gotValidBar = true;
                  if(pStart.getX() < xMin) xMin = pStart.getX();
                  if(pStart.getX() > xMax) xMax = pStart.getX();
                  if(pStart.getY() < yMin) yMin = pStart.getY();
                  if(pStart.getY() > yMax) yMax = pStart.getY();
 
                  if(pEnd.getX() < xMin) xMin = pEnd.getX();
                  if(pEnd.getX() > xMax) xMax = pEnd.getX();
                  if(pEnd.getY() < yMin) yMin = pEnd.getY();
                  if(pEnd.getY() > yMax) yMax = pEnd.getY();
               }
            }
         }

         // grow bounding box by half a stroke width or half the endcap size, whichever is larger
         if(gotValidBar)
         {
            double grow = Math.max(ebar.getStrokeWidth(), endCapSz);
            r.setRect(xMin-grow/2, yMin-grow/2, xMax-xMin+grow, yMax-yMin+grow);
         }
      }

      /**
       * Ignored. <code>ErrorBarPainter</code> gets all location information from the enclosing <code>TraceNode</code> 
       * instance.
       */
      @Override
      public void setLocationProducer(Iterable<Point2D> producer)
      {
      }

      /**
       * Ignored. <code>ErrorBarPainter</code> gets style attributes from the <code>ErrorBarNode</code> component of 
       * the enclosing <code>TraceNode</code> instance.
       */
      @Override
      public void setStyle(PainterStyle style)
      {
      }
   }
}
