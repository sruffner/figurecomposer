package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.srscicomp.common.g2dutil.PolylinePainter;
import com.srscicomp.common.g2dutil.RadialSectionPainter;
import com.srscicomp.common.g2dutil.SingleStringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * <b>BarPlotNode</b> is a <b><i>grouped-data presentation</i></b> element that renders a small {@link Fmt#
 * MSERIES} or {@link Fmt#MSET} collection as a typical bar plot. It is intended for collections of N &le;
 * {@link FGNPlottableData#MAX_DATAGRPS} individual data sets; it simply ignores any member sets beyond the first
 * N. Like other data presentation nodes, it subclasses {@link FGNPlottableData}, which manages the actual data set 
 * source. It is intended only for use in a 2D Cartesian graph with linear axes, or a polar graph with a linear radial
 * axis; it will not be rendered within a semilog, loglog , semilogR, or 3D graph.
 * 
 * <p>The bar plot may be rendered in a "grouped" or "stacked" configuration, either vertically or horizontally. The
 * underlying data set {X Y1 Y2 ...} (where X and Yn are column vectors) should always be prepared for the vertical
 * presentation; if one of the horizontal display modes are chosen, the data is simply transposed (in Cartesian bar
 * plots only; horizontal modes ignored for polar bar plots).</p>
 * 
 * <p>In the vertical grouped display mode, at each X-coordinate value, vertical bars are drawn from the baseline to Y1,
 * Y2, etcetera. The horizontal extent E covered by the bars at one X-coordinate value -- which we call the bar group
 * span -- and the spacing between them depends on the number of individual sets N, the interval DX in the X-coordinates
 * (if they are not regularly spaced, then DX is the minimum observed interval between two X-coordinate values), and the 
 * relative bar width W as specified by the node's "barWidth" attribute:
 * <ul>
 * <li>If N==1, E = DX. If there's only one X-coordinate (unlikely), DX is undefined and E=1.</li>
 * <li>For N=2..6, E = DX * 2N / (2N + 3).</li>
 * <li>For N&gt;6, E = 0.8 * DX.</li>
 * </ul>
 * Then the actual width of each bar is W * E / N. The bars are drawn in order to cover the calculated extent E; if
 * W=1.0 (100%), the bars will just touch; otherwise, there will be be small gaps between them. This algorithm generally
 * follows the way "grouped" bar plots are laid out in Matlab.</p>
 * 
 * <p>In the vertical stacked display mode, the bars are drawn stacked one upon the other at each X-coordinate value. 
 * The stacked bars all have the same width, W*DX. The first bar is drawn from the baseline to (baseline+Y1), the next
 * bar from (baseline+Y1) to (baseline+Y1+Y2), and so on. Note that negative Y values are allowed, but don't make much
 * sense in the stacked configuration!</p>
 * 
 * <p><u><i>Polar bar plots</i></u>. In a polar graph context, the "bars" become pie wedges (if starting at r==0) or
 * "radial sections" (if baseline radius is > 0). The X-coordinates of the data set are interpreted as angles in degrees
 * CCW. For the best results, these should be evenly distributed over the unit circle, [0, 360). Also, the "horizontal" 
 * display modes make no sense in the polar context; so {@link DisplayMode#HSTACK} is equivalent to {@link 
 * DisplayMode#VSTACK} and {@link DisplayMode#HGROUP} is equivalent to {@link DisplayMode#VGROUP}. Otherwise, the
 * layout of the radial sections in the "grouped" and "stacked" configurations is analogous to that of the rectangular
 * bars in the Cartesian bar plot. A polar bar plot in the "stacked" configuration implements a "wind rose" plot.</p>
 * 
 * <p>A bar plot is an example of a "grouped-data" presentation node; each "data group" in a bar plot represents one 
 * member set in the node's underlying data source collection. The super class {@link FGNPlottableData} implements
 * support for specifying a fill color and a legend label for each such data group. The fill color for each group may be
 * explicitly specified, or it may be selected by sampling the parent graph's color map. Typically, the user will select
 * a different color for each bar group to clearly distinguish the different data sets represented. Similarly, the user
 * can specify a legend label for each group; this label appears only in the parent graph's automated legend; if not
 * specified, the label is set to "data N", where N is the ordinal position of the corresponding set in the data
 * collection.</p>
 * 
 * <p>As of FC 5.4.5, the bar group legend labels may be rendered on the bar plot itself -- as an alternative to 
 * displaying the labels in the parent graph's legend. The automated labels are disabled when the parent graph is polar.
 * Otherwise, if {@link #isAutoLabelOn()} is true, the labels are rendered on the first or last bar group/stack,
 * whichever is further from the axis parallel to the bars. Every effort is made to put the label inside the 
 * corresponding bar in that group/stack, reducing the font size if necessary to a minimum of 6pt. If it does not fit
 * even at that size, then the label is located adjacent to but outside the bar. For the grouped modes, the label is 
 * placed above or below a vertical bar and left or right of a horizontal bar. In the stacked modes, the label is placed
 * to the left or right of a vertical bar and above or below the horizontal bar. In the vertical modes, the label is
 * rotated 90 deg to match the bar's orientation.</p>
 * 
 * @author sruffner
 */
public class BarPlotNode extends FGNPlottableData implements Cloneable
{
   /**
    * Construct a bar plot node initially configured to display an empty data collection in the "vertical grouped"
    * display mode. It will make no marks in the parent graph. Initially, the relative bar width is set to 80% and the 
    * baseline to 0 (user units).  All styling attributes are initially implicit (inherited). It has no title initially,
    * and the bar legend labels are not rendered on the bar plot itself.
    */
   public BarPlotNode()
   {
      super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR|HASTITLEATTR);
      setTitle("");
      mode = DisplayMode.VGROUP;
      baseline = 0;
      barWidth = 80;
      autoLabelOn = false;
   }

   //
   // Support for child nodes -- none permitted!
   //
   
   @Override public FGNodeType getNodeType() { return(FGNodeType.BAR); }


   //
   // Properties
   //

   /** An enumeration of the different data display modes supported by {@link BarPlotNode}. */
   public enum DisplayMode
   {
      /** 
       * At each X, bars representing different data sets are drawn side by side, vertically oriented. In a polar
       * graph, the radial sections (or pie wedges) are drawn side-by-side. 
       */
      VGROUP, 
      /** 
       * At each X, bars are drawn stacked upon each other in order. The single stacked bar is vertically oriented. In
       * a polar graph, the radial sections are stacked radially.
       */
      VSTACK,
      /** 
       * Similar to the vertical grouped mode, except that bars are horizontally oriented. Data is transposed. Not 
       * supported in a polar context; maps to VGROUP.
       */
      HGROUP,
      /** 
       * Similar to the horizontal stacked mode, except bars are horizontally oriented. Data is transposed. Not
       * supported in a polar context; maps to VSTACK. 
       */
      HSTACK; 
      
      @Override public String toString() { return(super.toString().toLowerCase()); }
   }

   /** The current data display mode. */
   private DisplayMode mode;

   /**
    * Get the current data display mode for this bar plot node
    * @return The current data display mode.
    */
   public DisplayMode getMode() { return(mode); }

   /**
    * Set the data display mode for this bar plot node. If a change is made, {@link #onNodeModified} is invoked.
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
    * Are the bars oriented vertically in this bar plot?
    * 
    * <p>In a polar context, regardless the display mode, the bars are rendered as pie wedges or radial sections.</p>
    * @return True for the vertical display modes; false for the horizontal display modes.
    */
   private boolean isVertical() { return(mode==DisplayMode.VGROUP || mode==DisplayMode.VSTACK); }
   
   /**
    * Are the bars grouped or stacked in this bar plot?
    * @return True for the grouped display modes; false for the stacked display modes.
    */
   private boolean isGrouped() { return(mode==DisplayMode.VGROUP || mode==DisplayMode.HGROUP); }
   
   /** The baseline for the bar plot, in user units. */
   private float baseline;

   /**
    * Get the bar plot baseline. In the grouped display modes, bars are drawn from this baseline value to the 
    * represented Y-coordinate value. In the stacked display modes, the first bar at a given X-coordinate is drawn from
    * this baseline to the Y-coordinate value, and the remaining bars are "stacked" upon this first one, in order. In 
    * the horizontal display modes, the baseline is an X-coordinate value rather than a Y-coordinate value.
    * 
    * <p>In polar bar plot, the baseline defines the radial value at the origin of the polar graph. Negative values are
    * permitted (bar plot is not supported in a semilogR graph).</p>
    * 
    * @return The bar plot baseline, in user units.
    */
   public float getBaseline() { return(baseline); }
    
   /**
    * Set the bar plot baseline. If a change is made, {@link #onNodeModified} is invoked.
    * @param base The new baseline, in user units. NaN and +/-infinity are rejected.
    * @return True if new value was accepted.
    */
   public boolean setBaseline(float base) 
   {
      if(!Utilities.isWellDefined(base)) return(false);
      if(baseline != base)
      {
         if(doMultiNodeEdit(FGNProperty.BASELINE, base)) return(true);
         
         Float old = baseline;
         baseline = base;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.BASELINE);
            FGNRevEdit.post(this, FGNProperty.BASELINE, baseline, old);
         }
      }
      return(true);
   }

   /** Minimum relative bar width (as a percentage). */
   public final static int MINRELBW = 5;
   
   /** Maximum relative bar width (as a percentage). */
   public final static int MAXRELBW = 100;
   
   /** Relative bar width (as a percentage).*/
   private int barWidth;

   /**
    * Get the bar plot node's relative bar width.
    * @return Relative bar width as a percentage.
    */
   public int getBarWidth() { return(barWidth); }
    
   /**
    * Set the bar plot's relative bar width. If a change is made, {@link #onNodeModified} is invoked.
    * @param bw Relative bar width, as a percentage. Values outside [{@link #MINRELBW} .. {@link #MAXRELBW}] are 
    * rejected.
    * @return True if new value was accepted.
    */
   public boolean setBarWidth(int bw) 
   {
      if(bw < MINRELBW || bw > MAXRELBW) return(false);
      if(barWidth != bw)
      {
         if(doMultiNodeEdit(FGNProperty.BARWIDTH, bw)) return(true);

         Integer old = barWidth;
         barWidth = bw;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.BARWIDTH);
            FGNRevEdit.post(this, FGNProperty.BARWIDTH, barWidth, old);
         }
      }
      return(true);
   }

   /** If this flag is set, the data group legend labels are rendered on the bar plot itself. */
   private boolean autoLabelOn;

   /**
    * Are the data group legend labels rendered on the bar plot?
    * 
    * <p>If enabled, a data group's legend label is rendered inside or adjacent to one of the corresponding bars for
    * that data group -- as an alternative to relying on the parent graph's legend label. The exact location and font
    * size of the rendered label will depend on the bar plot's display mode and the relative bar width. <i>Note: The
    * labels are never rendered on the bar plot when the parent graph is in polar coordinates.</i></p>
    * 
    * @return True if data group legend labels are rendered on the bar plot itself, as described.
    */
   public boolean isAutoLabelOn() { return(autoLabelOn); }

   /**
    * Enable/disable auto-labeling of the bar plot.
    * 
    * @param b True to enable, false to disable auto-labeling
    * @see #isAutoLabelOn()
    */
   public void setAutoLabelOn(boolean b)
   {
      if(autoLabelOn != b)
      {
         if(doMultiNodeEdit(FGNProperty.AUTO, b)) return;
         
         Boolean old = autoLabelOn;
         autoLabelOn = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.AUTO);
            String desc = (autoLabelOn ? "Enable" : "Disable") + " automated legend labels on bar plot";
            FGNRevEdit.post(this, FGNProperty.AUTO, autoLabelOn, old, desc);
         }
      }
   }


   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok;
      switch(p)
      {
         case MODE : ok = setMode((DisplayMode)propValue); break;
         case BASELINE: ok = setBaseline((Float)propValue); break;
         case BARWIDTH: ok = setBarWidth((Integer)propValue); break;
         case AUTO: ok = true; setAutoLabelOn((Boolean)propValue); break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value;
      switch(p)
      {
         case MODE : value = getMode(); break;
         case BASELINE: value = getBaseline(); break;
         case BARWIDTH: value = getBarWidth(); break;
         case AUTO: value = isAutoLabelOn(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }


   //
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   /**
    * The node-specific properties exported in a bar plot node's style set include the display mode, relative bar width,
    * the include-in-legend flag, and the auto-label flag. Colors assigned to individual bar groups are NOT included.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.MODE, getMode());
      styleSet.putStyle(FGNProperty.BARWIDTH, getBarWidth());
      styleSet.putStyle(FGNProperty.LEGEND, getShowInLegend());
      styleSet.putStyle(FGNProperty.AUTO, isAutoLabelOn());
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
      
      Integer n = (Integer) applied.getCheckedStyle(FGNProperty.BARWIDTH, getNodeType(), Integer.class);
      if(n != null && (n>=MINRELBW) && (n<=MAXRELBW) && !n.equals(restore.getStyle(FGNProperty.BARWIDTH)))
      {
         barWidth = n;
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

      b = (Boolean) applied.getCheckedStyle(FGNProperty.AUTO, null, Boolean.class);
      if(b != null && !b.equals(restore.getStyle(FGNProperty.AUTO)))
      {
         autoLabelOn = b;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.AUTO);
      
      return(changed);
   }


   // 
   // FGNPlottableData, FGNPlottable
   //
   
   /** 
    * The bar plot can optionally render the label for each bar group. Any change in its text-related styles will affect 
    * the labels, but only if they are turned on. Similarly if a data group label is changed.
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGNProperty prop = (hint instanceof FGNProperty) ? (FGNProperty) hint : null;
      boolean forceRender = (isAutoLabelOn()) && 
            ((prop == FGNProperty.FILLC) || FGNProperty.isFontProperty(prop));
      if(hasDataGroups() && (prop == FGNProperty.DGLABEL) && (isAutoLabelOn()))
         forceRender = true;
      super.onNodeModified(forceRender ? null : hint);
   }



   /**
    * Initializes the bar plot node's data set to contain random data.
    * <p>For a Cartesian graph, we create 3 bar groups drawn at 4 different locations spanning the graph's X-axis range. 
    * The baseline is set in the middle of the Y-axis range, and the Y-data is chosen randomly to span most of the 
    * Y-axis range.</p>
    * <p>If the parent graph is polar (not semilogR), the four X-coordinates are set to [45, 135, 225, 315] degrees, the
    * baseline radius is set to the start of the radial range, and the Y-data are chosen randomly to span most of the
    * radial axis range.</p>
    */
   @Override protected void initializeDefaultData(double[] axisRng, boolean isPolar)
   {
      float x0 = (float) axisRng[0];
      float x1 = (float) axisRng[1];
      float y0 = (float) axisRng[2];
      float y1 = (float) axisRng[3];
      
      baseline = isPolar ? y0 : (y0+y1) / 2.0f;
      
      float dx = (x1-x0)/5.0f;
      float[] params = isPolar ? new float[] {90, 45} : new float[] {dx, x0+dx};
      
      float yMin = y0 + (y1-y0)*0.1f;
      float ySpan = (y1-y0)*0.9f;
      
      float[] fData = new float[4*3];
      for(int i=0; i<4; i++)
      {
         fData[i*3] = yMin + 0.3f*ySpan*((float) Math.random());
         fData[i*3+1] = yMin + 0.7f*ySpan*((float) Math.random());
         fData[i*3+2] = yMin + ySpan*((float) Math.random());
      }
      
      DataSet ds = DataSet.createDataSet(getDataSet().getID(), Fmt.MSERIES, params, 4, 3, fData);
      if(ds != null) setDataSet(ds);
   }

   @SuppressWarnings("SuspiciousNameCombination")
   @Override protected boolean recalcDataRange(Object hint)
   {
      boolean needRecalc = 
         hint == null || hint == FGNProperty.SRC || hint == FGNProperty.MODE || hint == FGNProperty.BASELINE || 
         hint == FGNProperty.BARWIDTH;
      if(!needRecalc) return(false);
      
      float minX = 0;
      float maxX = 0;
      float minY = 0;
      float maxY = 0;

      calcBarGroupSpan();
      DataSet set = getDataSet();
      int nGrps = getNumDataGroups();
      if(barGrpSpan != 0)
      {
         minX = set.getXMin();
         maxX = set.getXMax();
         if(isGrouped())
         {
            minY = Math.min(baseline, set.getYMin());
            maxY = Math.max(baseline, set.getYMax());
         }
         else
         {
            // in stacked modes, we have to add up the Y values, starting from the baseline. In these modes you would
            // expect all Y values to have the same sign, but we don't require it.
            minY = baseline;
            maxY = baseline;
            for(int i=0; i<set.getDataLength(); i++) if(Utilities.isWellDefined(set.getX(i, 0)))
            {
               float sum = baseline;
               for(int j=0; j<nGrps; j++)
               {
                  float y = set.getY(i, j);
                  if(Utilities.isWellDefined(y)) sum += y;
               }
               if(sum < minY) minY = sum;
               else if(sum > maxY) maxY = sum;
            } 
         }
         
         if(!Utilities.isWellDefined(new float[]{minX, maxX, minY, maxY}))
         {
            minX = 0;
            maxX = 0;
            minY = 0;
            maxY = 0;
         }
         else
         {
            // adjust for the bar group span at the endpoints of the X range
            minX -= (float) (barGrpSpan / 2.0);
            maxX += (float) (barGrpSpan / 2.0);
            
            // in the horizontal modes, the data is transposed, so swap the X and Y ranges; but in a polar graph,
            // these modes are ignored.
            FGNGraph g = getParentGraph();
            if(g != null && (!g.isPolar()) && !isVertical())
            {
               float tmp = minX; minX = minY; minY = tmp;
               tmp = maxX; maxX = maxY; maxY = tmp;
            }
         }
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

   /** A bar plot renders only {@link Fmt#MSERIES} or {@link Fmt#MSET} data set collections. */
   @Override public boolean isSupportedDataFormat(Fmt fmt) { return(fmt == Fmt.MSET || fmt == Fmt.MSERIES); }
   @Override public Fmt[] getSupportedDataFormats() { return(new Fmt[] {Fmt.MSET, Fmt.MSERIES}); }
   
   @Override public boolean useBarInLegend()  { return(true); }
   @Override public boolean hasDataGroups() { return(true); }
   @Override protected int getDataGroupCount() 
   { 
      return(Math.min(MAX_DATAGRPS, getDataSet().getNumberOfSets())); 
   }
   
   //
   // Focusable/Renderable support
   //
   
   /** The bar group span, in user units, non-negative. If 0, the bar plot is not rendered. */
   private double barGrpSpan = 0;
   
   /**
    * Recalculate the bar plot node's current bar group span, which is the distance covered by each bar group centered
    * on an X-coordinate (or Y-coordinate in a horizontal display mode). It must be recalculated whenever the data 
    * source, the display mode, or the relative bar width changes. If the data set is empty or otherwise ill-defined,
    * the bar group span is set to 0, and the bar plot is not rendered.
    * 
    * <p>Bar group span E is important when laying out the bar plot in either of the "grouped" display modes. The 
    * algorithm, described in the class header, generally follows the approach used in <i>Matlab</i> bar plots. Once the
    * span E is calculated, the actual bar width B is E*W/N, where W is the relative bar width in [0.05 .. 1.0] and N is
    * the number of member sets in the data collection. The first bar at a given X-coordinate is drawn centered at 
    * X - E/2 + B/2, and subsequent bars (representing the values of the other member sets at that value of X) are 
    * shifted over by B. In the "stacked" display modes, E is still calculated, but with N = 1 always, since only one 
    * "stacked" bar is drawn at each X-coordinate value.</p>
    */
   private void calcBarGroupSpan()
   {
      // the number of sets in the collection-type data source is the number of bar groups (limited to MAX_DATAGRPS).
      // if there are no bar groups, the bar plot is (obviously) not drawn.
      int nGrps = getNumDataGroups();
      if(nGrps == 0)
      {
         barGrpSpan = 0;
         return;
      }
      
      DataSet ds = getDataSet();
      
      // if there are no well-defined X-coordinates, then the bar plot is not rendered (zero bar group span). If there 
      // is only one, the bar group span defaults to 1.0, since cannot computer minimum interval between neighboring
      // X-coordinates.
      List<Integer> validIndices = new ArrayList<>();
      for(int i=0; i<ds.getDataLength(); i++) if(Utilities.isWellDefined(ds.getX(i, 0)))
         validIndices.add(i);
      if(validIndices.size() <= 1)
      {
         barGrpSpan = (validIndices.isEmpty()) ? 0 : 1.0;
         return;
      }

      // find the minimum interval between neighboring X-coordinates
      double minDX = Double.POSITIVE_INFINITY;
      for(int i=0; i<validIndices.size()-1; i++)
      {
         double x1 = ds.getX(validIndices.get(i), 0);
         for(int j=i+1; j<validIndices.size(); j++)
         {
            double x2 = ds.getX(validIndices.get(j), 0);
            if(x1 == x2)
            {
               // no two well-defined X-coordinates can be the same, else the bar plot is not rendered
               barGrpSpan = 0;
               return;
            }
            double dx = Math.abs(x1-x2);
            if(dx < minDX) minDX = dx;
         }
      }
      
      // calculate the bar group span
      if(nGrps == 1 || !isGrouped())
         barGrpSpan = minDX;
      else if(nGrps > 1 && nGrps < 6)
         barGrpSpan = minDX * 2.0 * nGrps / (2.0 * nGrps + 3.0);
      else
         barGrpSpan = minDX * 0.8;
   }
   

   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null)
      {
         rBoundsSelf = new Rectangle2D.Double();
         FGNGraph g = getParentGraph();
         FViewport2D vp = getParentViewport();
         boolean ok = (vp != null) && (g != null) && 
               (g.getCoordSys() == FGNGraph.CoordSys.CARTESIAN || g.getCoordSys() == FGNGraph.CoordSys.POLAR);
         if(ok)
         {
            SingleStringPainter labelPainter = null;
            if(isAutoLabelOn())
            {
               labelPainter = new SingleStringPainter();
               labelPainter.setStyle(this);
               labelPainter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
            }

            MutableFillPS pStyle = new MutableFillPS(this);
            if(g.isPolar())
            {
               Point2D origin = vp.getPhysicalUserOrigin();
               RadialSectionPainter painter = new RadialSectionPainter(pStyle, origin, null, null);
               
               for(int i=0; i<getNumDataGroups(); i++)
               {
                  pStyle.setFillColor(getDataGroupColor(i));
                  painter.setLocationProducer(new PolarBarVertexProducer(i));
                  painter.invalidateBounds();
                  Utilities.rectUnion(rBoundsSelf, painter.getBounds2D(null), rBoundsSelf);
               }
            }
            else
            {
               PolylinePainter painter = new PolylinePainter(pStyle, null);
               painter.setConnectionPolicy(PolylinePainter.ConnectPolicy.CLOSED);
               painter.setFilled(true);
               
               for(int i=0; i<getNumDataGroups(); i++)
               {
                  pStyle.setFillColor(getDataGroupColor(i));
                  painter.setLocationProducer(new BarVertexProducer(i));
                  painter.invalidateBounds();
                  Utilities.rectUnion(rBoundsSelf, painter.getBounds2D(null), rBoundsSelf);
                  
                  // account for bar group label, if labels turned on
                  if(labelPainter != null)
                  {
                     LabelInfo info = prepareLabel(i, g2d);
                     if(info != null)
                     { 
                        labelPainter.setTextAndLocation(info.attrLabel, info.loc);
                        labelPainter.setRotation(info.rotDeg);
                        labelPainter.setAlignment(info.hAlign, TextAlign.CENTERED);
                        labelPainter.updateFontRenderContext(g2d);
                        labelPainter.invalidateBounds();
                        Utilities.rectUnion(rBoundsSelf, labelPainter.getBounds2D(null), rBoundsSelf);
                     }
                  }
               }
            }
         }
      }
      return((Rectangle2D)rBoundsSelf.clone());
   }

   /** Releases the cached rectangle bounding any marks made by this bar plot node. */
   @Override protected void releaseRenderResourcesForSelf() { rBoundsSelf = null; }

   /**
    * Render this bar plot node into the current graphics context IAW its current display mode. If the parent graph is 
    * not Cartesian or polar, the plot is not rendered.
    */
   @Override public boolean render(Graphics2D g2d, RenderTask task)
   {
      // never rendered inside a graph that's neither Cartesian or polar
      FGNGraph g = getParentGraph();
      FGNGraph.CoordSys coordSys = (g != null) ? g.getCoordSys() : null;
      FViewport2D vp = getParentViewport();
      if((vp == null) || !(needsRendering(task) || coordSys == FGNGraph.CoordSys.CARTESIAN || 
            coordSys == FGNGraph.CoordSys.POLAR))
         return(true);

      // prepare attributed string painter if bar group labels are drawn
      SingleStringPainter labelPainter = null;
      if(isAutoLabelOn())
      {
         labelPainter = new SingleStringPainter();
         labelPainter.setStyle(this);
         labelPainter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
      }

      // we use a custom painter style with a mutable fill color so that we can set the fill color for each bar group
      MutableFillPS pStyle = new MutableFillPS(this);

      if(coordSys == FGNGraph.CoordSys.CARTESIAN)
      {
         PolylinePainter painter = new PolylinePainter(pStyle, null);
         painter.setConnectionPolicy(PolylinePainter.ConnectPolicy.CLOSED);
         painter.setFilled(true);
         
         for(int i=0; i<getNumDataGroups(); i++)
         {
            pStyle.setFillColor(getDataGroupColor(i));
            painter.setLocationProducer(new BarVertexProducer(i));
            if(!painter.render(g2d, task)) return(false);
            
            if(labelPainter != null)
            {
               LabelInfo info = prepareLabel(i, g2d);
               if(info != null)
               { 
                  labelPainter.setTextAndLocation(info.attrLabel, info.loc);
                  labelPainter.setRotation(info.rotDeg);
                  labelPainter.setAlignment(info.hAlign, TextAlign.CENTERED);
               
                  if(!labelPainter.render(g2d,  task)) return(false);
               }
            }
         }
      }
      else
      {
         Point2D origin = vp.getPhysicalUserOrigin();
         RadialSectionPainter painter = new RadialSectionPainter(pStyle, origin, null, null);
         
         for(int i=0; i<getNumDataGroups(); i++)
         {
            pStyle.setFillColor(getDataGroupColor(i));
            painter.setLocationProducer(new PolarBarVertexProducer(i));
            if(!painter.render(g2d, task)) return(false);
         }

      }
      return(true);
   }

   /**
    * Cached rectangle bounding only the marks made by this bar plot node. An empty rectangle indicates that the
    * element makes no marks when "rendered". If null, the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;

   
   /** Helper class encsapsulates information needed to render the label for a bar group. */
   private static class LabelInfo
   {
      /** The label text in FypML styled text format. */
      String label;
      /** The label converted to an attributed string for rendering and text bounds calculation. */
      AttributedString attrLabel;
      /** The starting location for the label, in milli-in WRT the parent graph's viewport. */
      Point2D loc;
      /** The label's horizontal alignment WRT starting location. It is always centered vertically. */
      TextAlign hAlign;
      /** Rotation applied to the label in degrees CCW. */
      double rotDeg;
      /** Label font size in points. Label font size may be reduced to fit inside a bar. */
      int fontSizePts;
   }

   /**
    * Helper method prepares the information needed to render a bar group label. The label's location, horizontal text
    * alignment, rotation angle and font size are determined so that label is contained entirely within or adjacent to
    * one of the bars for that bar group. <i>Labels are NOT rendered when the parent graph is polar.</i>
    * 
    * <p>Algorithm for placement:
    * <ul>
    * <li>Label is placed on one bar for the specified data group: the last bar if axis parallel to bars is hidden; the
    * first or last horizontal bar if X axis is below or above the graph data window; the first or last vertical bar if
    * the Y axis is right or left of data window.</li>
    * <li>If the bar is vertical, the label is always rotated 90 deg.</li>
    * <li>Regardless the display mode, the method tries to fit the label entirely within the bar rectangle, accounting
    * for stroke width. If the label does not fit at the bar plot's specified font size, the font size is reduced until
    * a fit is achieved, down to a minimum of 6pt. If the label still does not fit, it is placed adjacent to but outside
    * the bar.</li>
    * <li>For the <i>grouped</i> modes, whether the label is inside or outside, it is aligned to the end of the bar
    * that is further from the perpendicular axis; if that axis is hidden or the coordinate system is "all-quad", the 
    * label aligns to the top (vertical bars) or right (horizontal bars) end of the bar.
    * <li>For the <i>stacked</i> modes, the label is centered within the bar if it fits. If it does not fit, it is still
    * centered along the extent of the bar but is placed outside of it. If the data group index is even, the label is
    * placed on the left side of a vertical bar and above a horizontal bar; if the index is odd, the label is located
    * just to the right of a vertical bar and below a horizontal bar.
    * </ul>
    * </p>
    * 
    * @param idx The bar group index. If invalid, method returns null.
    * @param g2 Graphics context -- needed to measure text in order to calculate label's render bounds. If null, 
    * method does nothing and returns null.
    * @return The rendering information for the bar group label. Returns null if automated bar plot labels are disabled,
    * if parent graph is polar, or if enable to prepare location info for the label, for whatever reason. The label
    * won't be rendered in this case.
    */
   private LabelInfo prepareLabel(int idx, Graphics2D g2)
   {
      if(g2 == null || idx < 0 || idx >= getNumDataGroups() || !isAutoLabelOn()) return(null);
      FViewport2D vp = getParentViewport();
      FGNGraph g = getParentGraph();
      if(g == null || vp == null || g.isPolar()) return(null); 
      
      boolean isVert = isVertical();
      boolean isGrp = isGrouped();
      
      LabelInfo info = new LabelInfo();
      info.label = getDataGroupLabel(idx);
      info.loc = new Point2D.Double();
      info.hAlign = TextAlign.CENTERED;
      info.rotDeg = isVert ? 90 : 0;
      info.fontSizePts = getFontSizeInPoints();
      
      // initialize a string painter to calculate the label's render bounds centered horizontally and vertically at
      // (0,0) using this bar plot's graphic styles
      SingleStringPainter painter = new SingleStringPainter();
      painter.setStyle(this);
      painter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
      painter.setLocation(0, 0);
      painter.setText(info.attrLabel);

      // select which bar in the data group (if there's more than one) is labeled -- the first or last -- depending on
      // loacation/vis of axis parallel to the bars
      GraphNode graph = (GraphNode) g;
      boolean isAxisHidden = isVert ? graph.getSecondaryAxis().getHide() : graph.getPrimaryAxis().getHide();
      boolean isAboveLeft = isVert ? graph.getSecondaryAxis().isAboveLeft() : graph.getPrimaryAxis().isAboveLeft();

      boolean labelFirstBar = isVert ? !(isAxisHidden || isAboveLeft) : (isAboveLeft && !isAxisHidden);

      // get the outline of the selected bar.
      GeneralPath barPath = new GeneralPath();
      BarVertexProducer producer = new BarVertexProducer(idx);
      int n = 0;
      while(producer.hasNext())
      {
         Point2D p = producer.next();
         if(!Utilities.isWellDefined(p))
         {
            // got next bar. Is it the one we want?
            if(n < 4) return(null);   // shouldn't happen
            barPath.closePath();
            if(labelFirstBar || !producer.hasNext()) break;

            n = 0;
            barPath.reset();
         }
         else
         {
            if(n == 0) barPath.moveTo(p.getX(), p.getY());
            else barPath.lineTo(p.getX(), p.getY());
            ++n;
         }
      }

      // get bar length and thickness -- depends on whether bars are vertical or horizontal!
      Rectangle2D barRect = barPath.getBounds2D();
      double barLen = isVert ? barRect.getHeight() : barRect.getWidth();
      double barThickness = isVert ? barRect.getWidth() : barRect.getHeight();
      
      // reduce font size as needed (down to 6pt minimum) so that label fits inside bar rectangle, accounting for 
      // stroke width. At the same time we convert the FypML styled text label to an attributed str with that font size.
      double gap = getStrokeWidth() + 4;  // extra space separating label from inside edge of bar border
      boolean inside = false;
      Rectangle2D labelRect = new Rectangle2D.Double();
      info.fontSizePts += (info.fontSizePts >= 8) ? 2 : 1;
      do
      {
         info.fontSizePts -= (info.fontSizePts > 8) ? 2 : 1;
         info.attrLabel = fromStyledText(info.label, getFontFamily(), info.fontSizePts, getFillColor(),
               getFontStyle(), true);
         painter.setText(info.attrLabel);
         painter.updateFontRenderContext(g2);
         painter.invalidateBounds();
         painter.getBounds2D(labelRect);
         if((labelRect.getHeight() <= barThickness - gap) && (labelRect.getWidth() <= barLen - gap))
         {
            inside = true;
            break;
         }
      } while(info.fontSizePts > 6);

      // determine label location and horizontal alignment (vertical alignment is always centered).
      if(isGrp)
      {
         // location/vis of axis perpendicular to bars determines at which end we align the label
         isAxisHidden = isVert ? graph.getPrimaryAxis().getHide() : graph.getSecondaryAxis().getHide();
         isAboveLeft = isVert ? graph.getPrimaryAxis().isAboveLeft() : graph.getSecondaryAxis().isAboveLeft();

         boolean labelsAboveOrRight = isVert ? (isAxisHidden || !isAboveLeft) : (isAxisHidden || isAboveLeft);
         
         if(labelsAboveOrRight) info.hAlign = inside ? TextAlign.TRAILING : TextAlign.LEADING;
         else info.hAlign = inside ? TextAlign.LEADING : TextAlign.TRAILING;

         double adj = labelsAboveOrRight ? ((inside ? -1 : 1) * gap/2) : ((inside ? 1 : -1) * gap/2);
         double x = barRect.getX(), y = barRect.getY();
         if(isVert) x += barRect.getWidth() / 2;
         else x += (labelsAboveOrRight ? barRect.getWidth() : 0) + adj;
         
         if(isVert) y += (labelsAboveOrRight ? barRect.getHeight() : 0) + adj;
         else y += barRect.getHeight() / 2;
         
         info.loc.setLocation(x, y);
      }
      else
      {
         // the bar center = label location in stacked modes if label "fits" inside
         double x = barRect.getX() + barRect.getWidth()/2;
         double y = barRect.getY() + barRect.getHeight()/2;
         
         // if not, we shift it left/right of bar (vertical case) or below/above bar (horizontal case), depending on
         // the group index. Rather than change vertical alignment, we take into account chosen font size to compute
         // the adjustment
         if(!inside)
         {
            double offsetSign = (idx % 2 == 0) ? -1 : 1;
            if(isVert) x += offsetSign * (barRect.getWidth()/2 + gap + labelRect.getHeight()/2);
            else y += offsetSign * (barRect.getHeight()/2 + gap + labelRect.getHeight()/2);
         }
         info.loc.setLocation(x, y);
      }

      return(info);
   }
   
   
   /**
    * <b>BarVertexProducer</b> is a helper class that provides an iterator over the vertices that define one bar group
    * in the bar plot node. It serves both as the iterator implementation and the iterator provider (it simply provides 
    * fresh copies of itself).
    * 
    * <p>A bar group represents one {X,Y} data set in the bar plot's data collection. All bars in a group are drawn as
    * rectangles. This vertex producer generates the four vertices for each rectangle, followed by one undefined point.
    * In this form, it can serve as the location producer for a {@link PolylinePainter} that will close the path before 
    * starting the next rectangle -- so the rectangles do not appear connected when rendered.</p>
    * 
    * @author  sruffner
    */
   private class BarVertexProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      /** Index of member set for which bar vertices are generated. */
      final int setIdx;
      /** The source data collection for the bar plot. */
      final DataSet set;
      /** The parent graph's viewport. If not Cartesian, no vertices are generated. */
      final FViewport2D graphVP;
      /** The bar plot's baseline in user units. */
      final double baseline;
      /** True for grouped display modes, false for stacked modes. */
      final boolean isGrp;
      /** True for vertical bars, false for horizontal bars (in the latter case, data is transposed). */
      final boolean isVert;
      /** The width of each bar in user units. */
      final double wBar;
      /** 
       * In grouped modes, the offset from the nominal X-coordinate to the center of the vertical bar. Value depends
       * on the member set index. The grouped bars span across a region centered over the nominal X-coordinate. (In
       * horizontal grouped mode, the data is transposed, so replace "X" with "Y".) Always zero for stacked modes.
       */
      final double xoff;
      
      int nPtsSoFar;
      int nVertsSoFar;
      Point2D[] barVertices;

      /**
       * Retrieve a new iterator. This merely returns a fresh copy of the producer, which acts both as {@link Iterable}
       * and {@link Iterator}.
       */
      public Iterator<Point2D> iterator() { return(new BarVertexProducer(setIdx)); }

      /**
       * Construct a <b>BarVertexProducer</b>, which provides a special iterator over the vertices that define the
       * outline of the bars in the bar plot that represent the specified member set in the source data collection.
       * 
       * @param idx Ordinal position of the bar group generated by this producer == the index of the corresponding 
       * member set in the bar plot node's source data collection.
       */
      public BarVertexProducer(int idx)
      {
         setIdx = idx;
         set = getDataSet();
         baseline = getBaseline();
         isGrp = isGrouped();
         isVert = isVertical();
         
         // this producer only applies in a 2D graph with a linear axes in X and Y
         FGNGraph g = getParentGraph();
         if((g == null) || (g.getCoordSys() != FGNGraph.CoordSys.CARTESIAN)) graphVP = null;
         else graphVP = getParentViewport();
         
         int ns = getNumDataGroups();
         if(graphVP != null && ns > 0 && setIdx >= 0 && setIdx < ns && barGrpSpan > 0)
         {
            if(isGrp)
            {
               double fullW = barGrpSpan / ns;
               wBar = (fullW * barWidth) / 100;
               xoff = -barGrpSpan/2.0 + fullW/2.0 + fullW*setIdx;
            }
            else
            {
               wBar = barGrpSpan * barWidth / 100;
               xoff = 0;
            }
         }
         else
         {
            wBar = 0;
            xoff = 0;
         }

         if(ns > 0)
         {
            nPtsSoFar = 0;
            nVertsSoFar = 5;
            barVertices = new Point2D[nVertsSoFar];
            for(int i = 0; i < nVertsSoFar; i++) 
               barVertices[i] = new Point2D.Double(0,0);
         }
      }

      public boolean hasNext()
      {
         if(graphVP == null || wBar == 0) return(false);
         return( !(nPtsSoFar >= set.getDataSize(-1) && nVertsSoFar >= barVertices.length) );
      }

      public Point2D next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         // if we're done with the current bar, compute vertices for the next one
         if(nVertsSoFar >= barVertices.length)
         {
            boolean gotNextBar = false;
            while(nPtsSoFar < set.getDataSize(-1) && !gotNextBar)
            {
               double x = set.getX(nPtsSoFar, 0);
               double y = set.getY(nPtsSoFar, setIdx);
               
               // if data point not well-defined, go to the next one
               if(!(Utilities.isWellDefined(x) && Utilities.isWellDefined(y)))
               {
                  ++nPtsSoFar;
                  continue;
               }
               
               // prepare vertices needed to render next bar. These are in user units.
               prepareBarVertices(nPtsSoFar, x, y);
               barVertices[4].setLocation(0, 0);   // so "well-defined" test below works!

               // convert to logical "painting" coordinates in the parent graph's viewport. If any of the vertices of 
               // the histogram bar are ill-defined, then we simply do not render it.
               graphVP.userUnitsToThousandthInches(barVertices);
               gotNextBar = Utilities.isWellDefined(barVertices);
               
               // always set last point to (NaN, NaN) so PolylinePainter will close the rectangle.
               barVertices[4].setLocation(Double.NaN, Double.NaN);

               // go to the next data point (next bar)
               ++nPtsSoFar;
            }

            // degenerate case: If some bars are ill-defined for whatever reason, we might have no vertices 
            // left to supply. In this case, the last bar contains ill-defined points, which painters should ignore.
            // The next call to hasNext() will return false.
            if(!gotNextBar)
            {
               for(Point2D barVertex : barVertices) barVertex.setLocation(Double.NaN, Double.NaN);
            }

            nVertsSoFar = 0;
         }

         Point2D nextVtx = barVertices[nVertsSoFar];
         ++nVertsSoFar;
         return(nextVtx);
      }

      @SuppressWarnings("SuspiciousNameCombination")
      private void prepareBarVertices(int pos, double x, double y)
      {
         double x1 = x + xoff - wBar/2.0;
         double x2 = x1 + wBar;
         double y1 = baseline;
         double y2 = y;
         if(!isGrp)
         {
            // in stacked modes, must set the Y-coords so that bars stack on one another
            for(int i=0; i<setIdx; i++)
            {
               double yPrev = set.getY(pos, i);
               if(Utilities.isWellDefined(yPrev)) y1 += yPrev;
            }
            y2 = y1 + y;
         }
         
         // in horizontal modes, the X and Y coordinates are transposed.
         if(isVert)
         {
            barVertices[0].setLocation(x1, y1);
            barVertices[1].setLocation(x2, y1);
            barVertices[2].setLocation(x2, y2);
            barVertices[3].setLocation(x1, y2);
         }
         else
         {
            barVertices[0].setLocation(y1, x1);
            barVertices[1].setLocation(y1, x2);
            barVertices[2].setLocation(y2, x2);
            barVertices[3].setLocation(y2, x1);
         }
      }
      
      public void remove() { throw new UnsupportedOperationException("Removal not supported by this iterator."); }
   }


   /**
    * <b>PolarBarVertexProducer</b> is a helper class that provides an iterator over the radial sections (or pie wedges)
    * that define one bar group in the bar plot node -- when that node is rendered in a polar graph context. It serves 
    * both as the iterator implementation and the iterator provider (it simply provides fresh copies of itself).
    * 
    * <p>A bar group represents one {theta, R} data set in the bar plot's data collection. All bars in a group are drawn
    * as radial sections, each of which is defined by two vertices: (theta0, r0) and (theta1, r1) -- where theta0 !=
    * theta1 and r1 > r0. If r0 == 0, the radial section becomes a pie wedge. This vertex producer generates the two
    * vertices for each radial section, with theta in degrees and radial coordinates in user units. It serves as the
    * location producer for the {@link RadialSectionPainter} that will render the bars.</p>
    * 
    * @author  sruffner
    */
   private class PolarBarVertexProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      /** Index of member set for which radial sections are generated. */
      final int setIdx;
      /** The source data collection for the bar plot. */
      final DataSet set;
      /** The parent graph's viewport. Vertices are generated only for a polar (and NOT semilogR) graph. */
      final FViewport2D graphVP;
      /** The bar plot's baseline in user units. */
      final double baseline;
      /** True for grouped display modes, false for stacked modes. */
      final boolean isGrp;
      /** The width of each bar in degrees. */
      final double wBar;
      /** 
       * In grouped modes, the offset from the nominal angle (theta) coordinate to the center of the radial section.  
       * Depends on the member set index. Always zero for stacked modes.
       */
      final double thetaOff;
      

      int nPtsSoFar;
      int nVertsSoFar;
      Point2D[] sectionVertices;
      final Point2D origin;
      
      /**
       * Retrieve a new iterator. This merely returns a fresh copy of the producer, which acts both as {@link Iterable}
       * and {@link Iterator}.
       */
      public Iterator<Point2D> iterator() { return(new PolarBarVertexProducer(setIdx)); }

      /**
       * Construct a <b>PolarBarVertexProducer</b>, which provides a special iterator over the vertices that define the
       * outline of the radial sections in a polar bar plot that represent the specified member set in the source data 
       * collection.
       * @param idx Ordinal position of the bar group generated by this producer == the index of the corresponding 
       * member set in the bar plot node's source data collection.
       */
      public PolarBarVertexProducer(int idx)
      {
         setIdx = idx;
         set = getDataSet();
         baseline = getBaseline();
         isGrp = isGrouped();
         
         // this producer only applies in a polar graph with a linear radial axis
         FGNGraph g = getParentGraph();
         if((g == null) || (g.getCoordSys() != FGNGraph.CoordSys.POLAR)) graphVP = null;
         else graphVP = getParentViewport();
         
         int ns = getNumDataGroups();
         if(graphVP != null && ns > 0 && setIdx >= 0 && setIdx < ns && barGrpSpan > 0)
         {
            if(isGrp)
            {
               double fullW = barGrpSpan / ns;
               wBar = (fullW * barWidth) / 100;
               thetaOff = -barGrpSpan/2.0 + fullW/2.0 + fullW*setIdx;
            }
            else
            {
               wBar = barGrpSpan * barWidth / 100;
               thetaOff = 0;
            }
         }
         else
         {
            wBar = 0;
            thetaOff = 0;
         }

         if(ns > 0)
         {
            nPtsSoFar = 0;
            nVertsSoFar = 2;
            sectionVertices = new Point2D[nVertsSoFar];
            for(int i = 0; i < nVertsSoFar; i++) 
               sectionVertices[i] = new Point2D.Double(0,0);
         }
         origin = (graphVP != null) ? graphVP.getPhysicalUserOrigin() : new Point2D.Double(0,0);
      }

      public boolean hasNext()
      {
         if(graphVP == null || wBar == 0) return(false);
         return( !(nPtsSoFar >= set.getDataSize(-1) && nVertsSoFar >= sectionVertices.length) );
      }

      public Point2D next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         // if we're done with the current radial section, compute vertices for the next one
         if(nVertsSoFar >= sectionVertices.length)
         {
            boolean gotNextSection = false;
            while(nPtsSoFar < set.getDataSize(-1) && !gotNextSection)
            {
               double x = set.getX(nPtsSoFar, 0);
               double y = set.getY(nPtsSoFar, setIdx);
               
               // if data point not well-defined, go to the next one
               if(!(Utilities.isWellDefined(x) && Utilities.isWellDefined(y)))
               {
                  ++nPtsSoFar;
                  continue;
               }
               
               // prepare vertices needed to render next bar. This converts the radial coordinates to milli-in instead
               // of user units, but theta coordinates still in degrees as required by RadialSectionPainter. Ordered
               // so that second vertex corresponds to the larger radius.
               prepareSectionVertices(nPtsSoFar, x, y);
               gotNextSection = Utilities.isWellDefined(sectionVertices);

               // go to the next data point (next bar)
               ++nPtsSoFar;
            }

            // degenerate case: If some sections are ill-defined for whatever reason, we might have no vertices 
            // left to supply. In this case, the last section contains ill-defined points, which painters should ignore.
            // The next call to hasNext() will return false.
            if(!gotNextSection)
            {
               for(Point2D sectionVertex : sectionVertices) sectionVertex.setLocation(Double.NaN, Double.NaN);
            }

            nVertsSoFar = 0;
         }

         Point2D nextVtx = sectionVertices[nVertsSoFar];
         ++nVertsSoFar;
         return(nextVtx);
      }

      private void prepareSectionVertices(int pos, double x, double y)
      {
         double theta0 = x + thetaOff - wBar/2.0;
         double theta1 = theta0 + wBar;
         double r0 = baseline;
         double r1 = y;
         if(!isGrp)
         {
            // in stacked modes, must set the radial coordinates so that bars stack on one another
            for(int i=0; i<setIdx; i++)
            {
               double rPrev = set.getY(pos, i);
               if(Utilities.isWellDefined(rPrev)) r0 += rPrev;
            }
            r1 += r0;
         }
         sectionVertices[0].setLocation(theta0, r0);
         sectionVertices[1].setLocation(theta1, r1);
         
         // convert vertices from user units to logical viewport units (milli-inches WRT BL corner of viewport) 
         graphVP.userUnitsToThousandthInches(sectionVertices);

         // if the vertices are well-defined after conversion...
         if(Utilities.isWellDefined(sectionVertices))
         {
            // put vertices back into polar coordinate form WRT polar origin, except now radii are in milli-inches. At
            // the same time, swap radial coordinates to endure first vertex has the smaller radius.
            r0 = origin.distance(sectionVertices[0]);
            r1 = origin.distance(sectionVertices[1]);
            sectionVertices[0].setLocation(theta0, Math.min(r0, r1));
            sectionVertices[1].setLocation(theta1, Math.max(r0, r1));
         }
      }
      
      public void remove() { throw new UnsupportedOperationException("Removal not supported by this iterator."); }
   }


   //
   // PSTransformable
   //
   
   public void toPostscript(PSDoc psDoc)
   {
      // only render in a Cartesian or polar graph
      FGNGraph g = getParentGraph();
      if(g == null || (g.getCoordSys() != FGNGraph.CoordSys.CARTESIAN && g.getCoordSys() != FGNGraph.CoordSys.POLAR))
         return;
      
      Point2D origin = null;
      if(g.isPolar())
      {
         FViewport2D vp = g.getViewport();
         if(vp == null) return;
         origin = vp.getPhysicalUserOrigin();
      }
      
      // if bar plot labels are turned on (not supported in polar graph context), we need a Graphics2D to measure text; 
      // create one from a buffered image
      Graphics2D g2BI = null;
      if(isAutoLabelOn() && !g.isPolar())
      {
         assert FCIcons.V4_BROKEN != null;
         Image img = FCIcons.V4_BROKEN.getImage();
         BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
         g2BI = bi.createGraphics();
      }

      psDoc.startElement(this);
      List<Point2D> vertices = new ArrayList<>();
      for(int i=0; i<getNumDataGroups(); i++)
      {
         // get the vertices defining the bars for the i-th bar group
         vertices.clear();
         Iterator<Point2D> iterator = g.isPolar() ? new PolarBarVertexProducer(i) : new BarVertexProducer(i);
         while(iterator.hasNext()) 
            vertices.add((Point2D) iterator.next().clone());    // must clone, b/c producer reuses a Point2D
         if(vertices.isEmpty()) continue;
         
         if(g.isPolar())
            psDoc.renderConcentricWedges(origin, vertices, getDataGroupColor(i));
         else
            psDoc.renderPolygons(vertices, getDataGroupColor(i));
         
         // if automated labels are turned on, render the label for this data group
         LabelInfo info = prepareLabel(i, g2BI);
         if(info != null)
            psDoc.renderText(info.label, info.loc.getX(), info.loc.getY(), info.hAlign, TextAlign.CENTERED, info.rotDeg,
                  info.fontSizePts * 1000.0 / 72.0);
      }
      psDoc.endElement();
   }
   
   // 
   // Object
   //

   /**
    * This override ensures that the rendering infrastructure for the clone is independent of the bar plot node cloned. 
    * The clone will reference the same data set, however!
    */
   @Override protected BarPlotNode clone() throws CloneNotSupportedException
   {
      BarPlotNode copy = (BarPlotNode) super.clone();
      copy.rBoundsSelf = null;
      return(copy);
   }
}
