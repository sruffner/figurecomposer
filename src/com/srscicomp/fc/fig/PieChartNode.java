package com.srscicomp.fc.fig;


import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.srscicomp.common.g2dutil.SingleStringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * <b>PieChartNode</b> is a <b><i>grouped-data presentation</i></b> element that renders a 1D list of values <i>{y0, y1,
 * .., yN} as a typical pie chart. For obvious reasons, the pie chart is intended only for use in a polar plot. It will 
 * not be rendered within a semilogR, Cartesian, semilog, or loglog graph.
 * 
 * <p>None of the <i>FypML</i> data formats really represent a simple 1D list of values. By convention, the pie chart
 * accepts data sets in any of the four 2D formats, but it completely ignores the X-coordinate values. Like all grouped
 * data presentation nodes, the maximum allowed number of data groups Nmax = {@link FGNPlottableData#MAX_DATAGRPS}. 
 * Below is a brief summary of how it processes the data source to prepare the list of values <i>{y0 .. yN}</i>:
 * <ul>
 * <li>{@link Fmt#PTSET} or {@link Fmt#SERIES} : Look at the first Nmax data points {x,y} in the set and
 * compile the list of Y-values, completely ignoring the X-coordinates. Any value that is NaN or non-positive is set to
 * zero.</li>
 * </li>
 * <li>{@link DataSet.MSET} or {@link Fmt#MSERIES} : Look at the first Nmax data points across the member sets
 * in the collection. Again, ignore the X-coordinate of each point and find the average Y-value across the member sets. 
 * Ill-defined values are omitted when computing the average. Any computed average that is NaN or non-positive is set 
 * to 0. These averages comprise the list of Y-values represented in the pie chart.</li>
 * </ul>
 * The number of data groups in the pie chart -- ie, the number of "pie slices" -- is the length of the compiled list
 * of Y-values, {y0, y1, .., yN}, N&le;Nmax. Let T = the sum of these values. The angular extent of the pie slice 
 * representing the value yN is then 360 deg * yN / T. If there are 0 data groups, or if all Y-values are 0, then the
 * pie chart is degenerate and is not rendered at all.</p>
 * 
 * <p>Observe that the data does not define the radial extent of the pie chart. The inner and outer radii are 
 * properties of the pie chart node. While the inner radius is typically 0, it can be set to a non-zero value to turn
 * the pie chart into a "donut".</p>
 * 
 * <p>As described above, each "data group" in a pie chart is rendered as a "slice in the pie". The super class {@link 
 * FGNPlottableData} implements support (common to bar, area and pie charts alike) for specifying a fill color and a 
 * legend label for each such data group. The fill color for each group may be explicitly specified, or it may be 
 * selected by sampling the parent graph's color map. Typically, the user will select a different color for each pie
 * slice to clearly distinguish the different data groups represented. Similarly, the user can specify a legend label 
 * for each group; this label appears only in the parent graph's automated legend; if not specified, the label is set to
 * "group N", where N is the ordinal position of the corresponding data group.</p>
 * 
 * <p>In addition, the user can control which slices, if any, are displaced radially from the origin. The radial offset,
 * another pie chart property, is the same for all slices that are offset.</p>
 * 
 * <p>All slices are stroked identically, IAW the stroke properties of this node.</p>
 * 
 * <p>As of FC 5.4.4, pie slice labels are supported -- as an alternative to displaying the labels in the parent graph's
 * legend. The label can reflect the percentage represented by the slice, the corresponding data group label, or both 
 * label and percentage: "label text (NN.N%)". Styled text format is supported. Each label's location, orientation, and 
 * font size are automatically adjusted to ensure the label fits entirely within the pie slice. The automated labels can
 * be turned off, of course. See {@link #getSliceLabelMode()}.</p>
 * 
 * @author sruffner
 */
public class PieChartNode extends FGNPlottableData implements Cloneable
{
   /**
    * Construct a pie chart node initially configured to display zero data groups (empty data set). It will make no 
    * marks in the parent graph. Initially, the inner and outer radii are set to 0 and 1, respectively (user units); the
    * radial slice offset is set to 10%. All styling attributes are initially implicit (inherited). It has no title 
    * initially, and slice labels are turned off.
    */
   public PieChartNode()
   {
      super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR|HASTITLEATTR);
      setTitle("");
      innerRadius = 0;
      outerRadius = 1;
      radialOffset = 10;
      displacedBits = 0;
      sliceLabelMode = LabelMode.OFF;
   }

   //
   // Support for child nodes -- none permitted!
   //
   
   @Override public FGNodeType getNodeType() { return(FGNodeType.PIE); }


   /** The inner radius for the pie chart, in user units. Must be non-negative and less than the outer radius. */
   private double innerRadius;

   /** The outer radius for the pie chart, in user units. Must be non-negative and greater than the inner radius. */
   private double outerRadius;

   /**
    * Get the pie chart's inner radius. In a polar graph, the "pie" is a "donut" when the inner radius is non-zero.
    * @return The inner radius, in user units.
    */
   public double getInnerRadius() { return(innerRadius); }
    
   /**
    * Set the pie chart's inner radius. If a change is made, {@link #onNodeModified()} is invoked.
    * @param r The new inner radius, in user units. Must be well-defined, non-negative, and less than the outer radius.
    * @return True if new value was accepted.
    */
   public boolean setInnerRadius(double r) 
   {
      if(!(Utilities.isWellDefined(r) && r >= 0f && r < outerRadius)) return(false);
      if(innerRadius != r)
      {
         if(doMultiNodeEdit(FGNProperty.IRAD, new Double(r))) return(true);
         
         Double old = new Double(innerRadius);
         innerRadius = r;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.IRAD);
            FGNRevEdit.post(this, FGNProperty.IRAD, new Double(innerRadius), old);
         }
      }
      return(true);
   }

   /**
    * Get the pie chart's outer radius.
    * @return The outer radius, in user units.
    */
   public double getOuterRadius() { return(outerRadius); }
    
   /**
    * Set the pie chart's outer radius. If a change is made, {@link #onNodeModified()} is invoked.
    * @param r The new outer radius, in user units. Must be well-defined, positive, and greater than the inner radius.
    * @return True if new value was accepted.
    */
   public boolean setOuterRadius(double r) 
   {
      if(!(Utilities.isWellDefined(r) && r > 0f && r > innerRadius)) return(false);
      if(outerRadius != r)
      {
         if(doMultiNodeEdit(FGNProperty.ORAD, new Double(r))) return(true);
         
         Double old = new Double(outerRadius);
         outerRadius = r;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.ORAD);
            FGNRevEdit.post(this, FGNProperty.ORAD, new Double(outerRadius), old);
         }
      }
      return(true);
   }

   /** Radial offset of any "displaced" pie slice, as a percentage of the outer radius. */
   private int radialOffset;

   /** Minimum value for the radial offset of a "displaced" slice (integer percentage of outer radius). */
   public final static int MIN_RADOFS = 1;
   /** Maximum value for the radial offset of a "displaced" slice (integer percentage of outer radius). */
   public final static int MAX_RADOFS = 100;
   
   /**
    * Get the radial offset for any "exploded slice" in the pie chart.
    * @return The radial offset as a percentage of the pie chart's outer radius. 
    */
   public int getRadialOffset() { return(radialOffset); }
    
   /**
    * Set the radial offset for any "exploded slice" in the pie chart. If a change is made, {@link #onNodeModified()} is
    * invoked. 
    * @param ofs Radial offset, as a percentage of the outer radius. Values outside [{@link #MIN_RADOFS} .. {@link 
    * #MAX_RAD_OFS}] are rejected.
    * @return True if new value was accepted.
    */
   public boolean setRadialOffset(int ofs) 
   {
      if(ofs < MIN_RADOFS || ofs > MAX_RADOFS) return(false);
      if(radialOffset != ofs)
      {
         if(doMultiNodeEdit(FGNProperty.SLICEOFS, new Integer(ofs))) return(true);
         
         Integer old = new Integer(radialOffset);
         radialOffset = ofs;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.SLICEOFS);
            FGNRevEdit.post(this, FGNProperty.SLICEOFS, new Integer(radialOffset), old);
         }
      }
      return(true);
   }

   /** Bit flag vector: if bit N is set, then the pie slice for data group N is displaced radially in the pie chart. */
   private int displacedBits;
   
   /**
    * Get the bit flag vector that indicates which data groups in the pie chart are represented by pie slices displaced
    * radially from the chart origin.
    * @return The bit flag vector, where bit N is set if the slice for data group N is radially displaced.
    */
   int getDisplacedBits()
   { 
      int n = getNumDataGroups();
      int ena = 0;
      for(int i=0; i<n; i++) ena |= (1<<i);
      return(displacedBits & ena); 
   }
   
   /**
    * Set the bit flag vector that indicates which data groups in the pie chart are represented by pie slices radially
    * displaced from the chart origin. Note that {@link #onNodeModified()} is not called by this package-private method,
    * which is intended only for use when reconstructing a figure model from <i>FypML</i>.
    * 
    * @param flags The bit flag vector, where bit N is set if the slice for data group N is radially displaced.
    */
   void setDisplacedBits(int flags)
   {
      displacedBits = flags;
   }
   
   /**
    * Is the pie slice representing the specified data group radially displaced in this pie chart?
    * @param pos Data group index position. 
    * @return True if pie slice is radially displaced to highlight the data group; false if not, or if index invalid.
    */
   public boolean isSliceDisplaced(int pos)
   {
      return((pos >= 0 && pos < getNumDataGroups()) ? ((displacedBits & (1<<pos)) != 0) : false);
   }
   
   /**
    * Radially displace the pie slice representing the specified data group in this pie chart, or restore it to its
    * normal position in the pie chart.
    * @param pos Data group index position. No action is taken if invalid.
    * @param displace True to displace the slice radially from the chart origin, false to restore it to its normal 
    * position in the chart.
    * @return True if new value was accepted; false otherwise (invalid data group index).
    */
   public boolean setSliceDisplaced(int pos, boolean ena)
   {
      if(pos < 0 || pos >= getNumDataGroups()) return(false);
      
      int bitFlag = (1<<pos);
      boolean wasDisplaced = (displacedBits & bitFlag) != 0;
      if(wasDisplaced != ena)
      {
         if(ena) displacedBits |= bitFlag;
         else displacedBits &= ~bitFlag;
         
         if(areNotificationsEnabled())
         {
            // index of the affected data group is included with old and new values of the data group color
            Object[] oldInfo = new Object[] {new Integer(pos), new Boolean(!ena)};
            Object[] newInfo = new Object[] {new Integer(pos), new Boolean(ena)};
            onNodeModified(FGNProperty.DGSLICE);
            FGNRevEdit.post(this, FGNProperty.DGSLICE, newInfo, oldInfo, 
                  (ena ? "Displace " : "Restore normal ") + " pie slice at index " + (pos+1));
         }
      }
      return(true);
   }
   
   /** An enumeration of the different slice label modes supported by <b>PieChartNode</b>. */
   public enum LabelMode
   {
      /** Individual slice labels are turned off. */ OFF,
      /** Label each slice with the percentage of the whole it represents. */ PERCENT,
      /** Label each slice with the legend label assigned to the data group it represents. */ LEGENDLABEL,
      /** Label each slice with both the group legend label and the percentage. */ FULL;
      
      @Override public String toString() { return(super.toString().toLowerCase()); }
   }
   
   /** The current slice label mode. */
   private LabelMode sliceLabelMode;
   
   /** 
    * Get the current slice label node for this pie chart.
    * @return The current label mode.
    */
   public LabelMode getSliceLabelMode() { return(sliceLabelMode); }
   
   /**
    * Set the slice label mode for this pie chart. If a change is made, {@link #onNodeModified()} is invoked.
    * 
    * @param mode The new label mode. A null value is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setSliceLabelMode(LabelMode mode)
   {
      if(mode == null) return(false);
      if(sliceLabelMode != mode)
      {
         if(doMultiNodeEdit(FGNProperty.MODE, mode)) return(true);
         
         LabelMode oldMode = sliceLabelMode;
         sliceLabelMode = mode;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.MODE);
            FGNRevEdit.post(this, FGNProperty.MODE, sliceLabelMode, oldMode);
         }
      }
      return(true);
   }
   
   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
         case IRAD: ok = setInnerRadius((Double)propValue); break;
         case ORAD: ok = setOuterRadius((Double)propValue); break;
         case SLICEOFS: ok = setRadialOffset((Integer)propValue); break;
         case DGSLICE:
         {
            // special case: The "property value" here is a mixed object array. The first is an Integer holding the
            // position of the affected pie slice; the second holds the state of the "displaced" bit for that slice.
            Object[] arObj = null;
            ok = propValue != null && propValue.getClass().equals(Object[].class);
            if(ok)
            {
               arObj = (Object[]) propValue;
               ok = arObj.length == 2;
               if(ok) ok = arObj[0] != null && arObj[0].getClass().equals(Integer.class);
               if(ok) ok = arObj[1] != null && arObj[1].getClass().equals(Boolean.class);
               if(ok) ok = setSliceDisplaced(((Integer) arObj[0]).intValue(), ((Boolean) arObj[1]).booleanValue());
            }
            break;
         }
         case MODE: ok = setSliceLabelMode((LabelMode)propValue); break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      // NOTE: the DGSLICE property not supported for multi-object edit, as this requires the data group index
      Object value = null;
      switch(p)
      {
         case IRAD: value = new Double(getInnerRadius()); break;
         case ORAD: value = new Double(getOuterRadius()); break;
         case SLICEOFS: value = new Integer(getRadialOffset()); break;
         case MODE: value = getSliceLabelMode(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }


   //
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   /**
    * The only node-specific properties exported in a pie chart node's style set is the include-in-legend flag and
    * the slice label display mode.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.LEGEND, new Boolean(getShowInLegend()));
      styleSet.putStyle(FGNProperty.MODE, getSliceLabelMode());
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.LEGEND, null, Boolean.class);
      if(b != null && !b.equals(restore.getStyle(FGNProperty.LEGEND)))
      {
         setShowInLegendNoNotify(b);
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LEGEND);
      
      LabelMode lm = (LabelMode) applied.getCheckedStyle(FGNProperty.MODE, getNodeType(), LabelMode.class);
      if(lm != null && !lm.equals(restore.getStyle(FGNProperty.MODE)))
      {
         sliceLabelMode = lm;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.MODE);

      return(changed);
   }


   // 
   // FGNPlottableData, FGNPlottable
   //

   /** 
    * Unlike most other plottable nodes, the pie chart can optionally render its pie slice labels. Any change in the
    * pie chart's text-related styles will affect the labels, but only if they are turned on. Similarly if a data group
    * label is changed.
    */
   @Override protected void onNodeModified(Object hint)
   {
      boolean forceRender = (hint instanceof FGNProperty) && FGNProperty.isFontProperty((FGNProperty)hint) && 
            (sliceLabelMode != LabelMode.OFF);
      if(hasDataGroups() && (hint == FGNProperty.DGLABEL) && (sliceLabelMode != LabelMode.OFF) && 
            (sliceLabelMode != LabelMode.PERCENT))
         forceRender = true;
      super.onNodeModified(forceRender ? null : hint);
   }


   /**
    * Initializes the area chart node's data set to represent a pie chart with six unequal slices. The inner radius is
    * set to 0 and the outer radius to the extent of the parent graph's radial range. However, if the parent graph is
    * NOT polar, the outer radius is simply set to 1.
    */
   @Override protected void initializeDefaultData(double[] axisRng, boolean isPolar)
   {
      innerRadius = 0;
      outerRadius = (isPolar) ? axisRng[3] : 1;
      
      float[] fData = new float[] {6, 5, 4, 3, 2, 1};
      float[] params = new float[] {1, 1};
      
      DataSet ds = DataSet.createDataSet(getDataSet().getID(), Fmt.SERIES, params, 6, 1, fData);
      if(ds != null) setDataSet(ds);
   }
   /**
    * For a pie chart -- which makes sense only in a polar graph -- the range in X is always [0..360]. In Y, regardless
    * the value of the inner radius, the range is [0 R] or [0 R+D], where R is the outer radius and D is the radial 
    * offset of a displaced pie slice. Note that the underlying data set has no role in defining the data range here!
    */
   @Override protected boolean recalcDataRange(Object hint)
   {
      boolean needSlices = (hint == null) || (hint == FGNProperty.SRC);
      boolean needRecalc = 
            needSlices || hint == FGNProperty.ORAD || hint == FGNProperty.SLICEOFS || hint == FGNProperty.DGSLICE;
      if(!needRecalc) return(false);
      
      float minX = 0;
      float maxX = 360f;
      float minY = 0;
      float maxY = ((float) outerRadius);
      
      // if any (non-zero) slices are offset, add the explode offset to maxY.
      boolean displace = false;
      if(slices == null || needSlices) calcSlices();
      for(int i=0; i<slices.length && !displace; i++) displace = (slices[i] > 0) && isSliceDisplaced(i);
      if(displace) maxY += (float) (radialOffset * outerRadius / 100.0);
         
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

   /** 
    * A pie chart will accept any of the standard 2D data formats. See class header for an explanation of how the 1D
    * list of values is obtained from the data set in any of these four formats.
    */
   @Override public boolean isSupportedDataFormat(Fmt fmt) 
   { 
      return(fmt == Fmt.PTSET || fmt == Fmt.SERIES || fmt == Fmt.MSET || fmt == Fmt.MSERIES); 
   }
   @Override public Fmt[] getSupportedDataFormats() 
   { 
      return(new Fmt[] {Fmt.PTSET, Fmt.SERIES, Fmt.MSET, Fmt.MSERIES}); 
   }
   
   
   @Override public boolean useBarInLegend()  { return(true); }
   @Override public boolean hasDataGroups() { return(true); }
   
   /**
    * In the case of a pie chart, the number of data groups is the length of the underlying data set, up to a maximum
    * of {@link FGNPlottableData#MAX_DATAGRPS}. Each data group is represented by a pie slice. Zero-percent slices are
    * included, as long as at least one slice is non-zero. If all slices are zero-percent, then the pie chart is 
    * degenerate, has zero data groups, and is never rendered.
    */
   @Override protected int getDataGroupCount() 
   { 
      if(slices == null) calcSlices();
      return(slices.length); 
   }

   //
   // Focusable/Renderable support
   //
   
   /** 
    * The sizes of the slices in the pie chart, as computed from the source data (as a fraction of the whole). Null if
    * not yet calculated. Will be zero-length if the pie chart is degenerate (0 data groups or data groups are 0).
    */
   private double[] slices = null;
   
   /**
    * Helper method processes the pie chart's underlying data source and computes the size of the "pie slice" that
    * represents each data group in the chart. It must be called whenever the data source changes. See class header for
    * a brief explanation of how the underlying data is processed.
    */
   private void calcSlices()
   {
      DataSet ds = getDataSet();
      if(isSupportedDataFormat(ds.getFormat()))
      {
         int n = Math.min(ds.getDataLength(), MAX_DATAGRPS);
         slices = new double[n];
         double sum = 0;
         for(int i=0; i<n; i++)
         {
            double y = ds.getY(i, -1);
            slices[i] = (Utilities.isWellDefined(y) && y > 0) ? y : 0;
            sum += slices[i];
         }
         if(sum > 0) 
         {
            for(int i=0; i<n; i++) slices[i] /= sum;
         }
         else if(slices.length > 0)
         {
            // degenerate case -- no data groups in the pie chart (or data total is zero).
            slices = new double[0];
         }
      }
      else
         slices = new double[0];
   }
   
   /**
    * Finds the rectangle bounding the pie chart (including any displaced slices). If parent graph is not polar, then 
    * the rectangle is empty b/c pie chart won't be rendered.
    */
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null)
      {
         rBoundsSelf = new Rectangle2D.Double();
         
         // NOTE: This overestimates the render bounds because Path2D.getBounds2D() sometimes includes bezier control
         // points in the rectangular bounds even though they lie outside. Not fixed until Java 19.
         FGNGraph g = getParentGraph();
         FViewport2D vp = getParentViewport();
         boolean ok = (g != null) && (g.getCoordSys() == FGNGraph.CoordSys.POLAR) && (vp != null) && 
               (slices != null) && (slices.length > 0);
         if(ok)
         {
            GeneralPath pieSlicePath = new GeneralPath();
            for(int i=0; i<slices.length; i++)
            {
               prepareSliceOutline(i, pieSlicePath);
               if(i==0) rBoundsSelf = pieSlicePath.getBounds2D();
               else rBoundsSelf.add(pieSlicePath.getBounds2D());
            }
         }
         if(ok)
         {
            double sw = getStrokeWidth();
            if(sw > 0) rBoundsSelf.setFrame(rBoundsSelf.getX()-sw, rBoundsSelf.getY()-sw, 
                     rBoundsSelf.getWidth()+2*sw, rBoundsSelf.getHeight()+ 2*sw);
            if(g.getClip())
               Rectangle2D.intersect(rBoundsSelf, g.getBoundingBoxLocal(), rBoundsSelf);
         }
      }
      return((Rectangle2D)rBoundsSelf.clone());
   }

   /** Releases the cached rectangle bounding any marks made by this pie chart node. */
   @Override protected void releaseRenderResourcesForSelf() { rBoundsSelf = null; }

   /**
    * Render this pie chart node into the current graphics context IAW its current display mode. If the parent graph is 
    * not polar, the plot is not rendered.
    */
   @Override public boolean render(Graphics2D g2d, RenderTask task)
   {
      // never rendered inside a non-polar graph (semilogR not allowed either)
      FGNGraph g = getParentGraph();
      if(g == null || g.getCoordSys() != FGNGraph.CoordSys.POLAR) return(true);
      FViewport2D vp = getParentViewport();
      if(vp == null) return(true);
      
      if(needsRendering(task) && slices != null && slices.length > 0)
      {
         // prepare attributed string painter if pie slice labels are drawn
         SingleStringPainter labelPainter = null;
         if(sliceLabelMode != LabelMode.OFF)
         {
            labelPainter = new SingleStringPainter();
            labelPainter.setStyle(this);
            labelPainter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
         }
         
         GeneralPath pieSlicePath = new GeneralPath();
         g2d.setStroke(getStroke(0f));
         for(int i=0; i<slices.length; i++) if(slices[i] > 0)
         {
            prepareSliceOutline(i, pieSlicePath);
            g2d.setColor(getDataGroupColor(i));
            g2d.fill(pieSlicePath);
            g2d.setColor(getStrokeColor());
            g2d.draw(pieSlicePath);
            
            if(labelPainter != null)
            {
               SliceLabelInfo info = prepareSliceLabel(i, g2d);
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
      return(true);
   }

   /**
    * Prepare the {@link GeneralPath} that outlines one of the pie "slices" rendered in this pie chart.
    * 
    * @param idx Index of the data group represented by the pie slice.
    * @param path On return, this should be initialized with the geometric path outlining the slice.
    */
   private void prepareSliceOutline(int idx, GeneralPath path)
   {
      path.reset();
      if(idx < 0 || idx > slices.length) return;
      
      FViewport2D vp = getParentViewport();
      if(vp == null) return;
      Point2D p = vp.getPhysicalUserOrigin();
      double oriX = p.getX();
      double oriY = p.getY();
      p.setLocation(90, outerRadius);
      vp.userUnitsToThousandthInches(p);
      double orMI = p.distance(oriX, oriY);
      double irMI = 0;
      if(innerRadius > 0)
      {
         p.setLocation(90, innerRadius);
         vp.userUnitsToThousandthInches(p);
         irMI = p.distance(oriX, oriY);
      }
      double startDeg = 90;
      for(int i=0; i<idx; i++) startDeg += 360*slices[i];
      double extentDeg = slices[idx]*360;
      
      if(isSliceDisplaced(idx))
      {
         double ofsRad = (outerRadius * radialOffset) / 100;
         p.setLocation(startDeg + extentDeg/2.0, ofsRad);
         vp.userUnitsToThousandthInches(p);
         oriX = p.getX();
         oriY = p.getY();
      }
      
      if(irMI > 0)
      {
         Arc2D arc = new Arc2D.Double(oriX-orMI, oriY-orMI, orMI*2, orMI*2, -startDeg, -extentDeg, Arc2D.OPEN);
         path.append(arc, false);
         arc.setArc(oriX-irMI, oriY-irMI, irMI*2, irMI*2,  -(startDeg + extentDeg), extentDeg, Arc2D.OPEN);
         path.append(arc, true);
         path.closePath();
      }
      else
         path.append(new Arc2D.Double(oriX-orMI, oriY-orMI, orMI*2, orMI*2, -startDeg, -slices[idx]*360, Arc2D.PIE), 
               false);
   }
   
   /** Helper class encsapsulates information needed to render a pie slice label. */
   private class SliceLabelInfo
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
      /** Label font size in points. Label font size may be reduced to fit label in slice. */
      int fontSizePts;
   }
   
   /**
    * Helper method prepares the information needed to render a pie slice label. The label's location, orientation,
    * horizontal text alignment, and font size are adjusted to ensure that if fits entirely within the corresponding
    * pie slice/annulus.
    * 
    * <p>The method first tries to fit the label at 3 different locations using the pie charts' current font size:
    * <ol>
    * <li>Centered at intersection of radial line bisecting the slice and the arc that is 3/5 of the way from the inner 
    * to the outer radius of slice.</li>
    * <li>Centered at intersection of radial line bisecting the slice and the arc that is 4/5 of the way from the inner 
    * to the outer radius of slice.</li>
    * <li>Oriented radially, centered on the bisecting radial line, and left- or right-aligned to the outer radius of
    * the pie slice.</li>
    * </ol>
    * If none of those locations work, the label font size is reduced to 10pt (assuming node font size is larger) and
    * each of the locations are tested again until a fit is achieved. If not, the font size is further reduced to 6pt.
    * If still unable to fit the label, and the inner radius is non-zero (pie slices are annuli), the method tries
    * centering the label within the annulus, perpendicular to the radial bisector.
    * </p>
    * <p>If the label cannot be fit within the pie slice/annulus at any of the above locations/orienatations, then the
    * label will not be rendered.</p>
    * 
    * @param idx The slice index. If invalid, method returns null.
    * @param g2 Graphics context -- needed to measure text in order to calculate label's render bounds. If null, 
    * method does nothing and returns null.
    * @return The rendering information for the slice label. Returns null if pie slice labels are turned off, if either
    * argument is invalid, or if unable to fit label entirely inside the pie slice.
    */
   private SliceLabelInfo prepareSliceLabel(int idx, Graphics2D g2)
   {
      // if labels aren't drawn or either argument is invalid, return null
      if(sliceLabelMode == LabelMode.OFF || idx < 0 || idx >= slices.length || g2 == null) return(null);
      
      FViewport2D vp = getParentViewport();
      if(vp == null) return(null);
      
      GeneralPath slicePath = new GeneralPath();
      prepareSliceOutline(idx, slicePath);
      
      double startDeg = 90;
      for(int i=0; i<idx; i++) startDeg += 360*slices[i];
      double midDeg = startDeg + slices[idx]*360/2.0;
      Point2D p = vp.getPhysicalUserOrigin();
      double oriX = p.getX();
      double oriY = p.getY();
      double displacedDX = 0, displacedDY = 0;
      if(isSliceDisplaced(idx))
      {
         p.setLocation(midDeg, (outerRadius * radialOffset) / 100);
         vp.userUnitsToThousandthInches(p);
         displacedDX = p.getX() - oriX;
         displacedDY = p.getY() - oriY;
      }
      
      SliceLabelInfo info = new SliceLabelInfo();
      
      // prepare the label text, which may be in styled text format
      String pct = Utilities.toString(slices[idx]*100.0, 3, 1) + "%";
      if(sliceLabelMode == LabelMode.PERCENT)
         info.label = pct;
      else if(sliceLabelMode == LabelMode.LEGENDLABEL)
         info.label = getDataGroupLabel(idx);
      else
      {
         // LabelMode.FULL: tack on "(NN.N%)" to the styled text legend label 
         String attrLabel = getDataGroupLabel(idx);
         int pipeIdx = attrLabel.lastIndexOf('|');
         if((pipeIdx > -1) && (pipeIdx < attrLabel.length() - 1))
         {
            String codeSfx = attrLabel.substring(pipeIdx+1) + String.format(",%d:-pnu", pipeIdx);
            info.label = String.format("%s (%s)|%s", attrLabel.substring(0,pipeIdx), pct, codeSfx);
         }
         else info.label = String.format("%s (%s)", attrLabel, pct);
      }
      info.attrLabel = fromStyledText(info.label, getFontFamily(), getFontSizeInPoints(), getFillColor(),
            getFontStyle(), true);
      
      // initialize a string painter to calculate the label's render bounds centered horizontally and vertically at
      // (0,0) using this pie chart's graphic styles
      info.loc = new Point2D.Double();
      info.hAlign = TextAlign.CENTERED;
      info.rotDeg = 0;
      SingleStringPainter painter = new SingleStringPainter();
      painter.setStyle(this);
      painter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
      painter.setLocation(0, 0);
      painter.setText(info.attrLabel);
      Rectangle2D rectLabel = new Rectangle2D.Double();
      
      // try 3 different locations/orientations of the label, first at the node's font size, and then decrease fontl
      // size in 2-pt increments down to 6pt, until the label fits inside the pie slice. If it doesn't fit under any
      // of these scenarios, the label is not rendered.
      int fontSize = getFontSizeInPoints();
      do
      {
         // we change the font size when we prepare the attributed string content for the painter
         info.fontSizePts = fontSize;
         info.attrLabel = fromStyledText(info.label, getFontFamily(), fontSize, getFillColor(),
               getFontStyle(), true);
         painter.setText(info.attrLabel);
         painter.updateFontRenderContext(g2);
         painter.invalidateBounds();
         painter.getBounds2D(rectLabel);
         
         // location 1: on radial line bisecting slice (offset if applicable), 3/5 of way to the outer radius
         info.loc.setLocation(midDeg, 0.6*(outerRadius - innerRadius) + innerRadius);
         vp.userUnitsToThousandthInches(info.loc);
         info.loc.setLocation(info.loc.getX() + displacedDX, info.loc.getY() + displacedDY);
         rectLabel.setFrame(info.loc.getX() - rectLabel.getWidth()/2, info.loc.getY() - rectLabel.getHeight()/2, 
               rectLabel.getWidth(), rectLabel.getHeight());
         if(slicePath.contains(rectLabel))
            return(info);
         
         // location 2: on radial line bisecting slice, 4/5 of way to outer radius
         info.loc.setLocation(midDeg, 0.8*(outerRadius - innerRadius) + innerRadius);
         vp.userUnitsToThousandthInches(info.loc);
         info.loc.setLocation(info.loc.getX() + displacedDX, info.loc.getY() + displacedDY);
         rectLabel.setFrame(info.loc.getX() - rectLabel.getWidth()/2, info.loc.getY() - rectLabel.getHeight()/2, 
               rectLabel.getWidth(), rectLabel.getHeight());
         if(slicePath.contains(rectLabel))
            return(info);
         
         // location 3: rotated so that label is parallel to radial line bisecting slice and starts (or ends) close to 
         // outer radius. Horizontal alignment and rotation angle depend on whether that bisector lies in the left half or 
         // right half of pie chart. To test if label fits, we rotate the slice shape so that the bisecting radius lies at
         // 0 deg back to 0 deg compensate the label bounds accordingly.
         info.loc.setLocation(0, outerRadius*0.95);
         vp.userUnitsToThousandthInches(info.loc);
         if(isSliceDisplaced(idx))
            info.loc.setLocation(info.loc.getX() + Math.sqrt(displacedDX*displacedDX + displacedDY*displacedDY), 
                  info.loc.getY());
         rectLabel.setFrame(info.loc.getX()-rectLabel.getWidth(), info.loc.getY()-rectLabel.getHeight()/2, 
               rectLabel.getWidth(), rectLabel.getHeight());
         p = vp.getPhysicalUserOrigin(p);
         if(slicePath.createTransformedShape(AffineTransform.getRotateInstance(-midDeg*Math.PI/180, 
               p.getX(), p.getY())).contains(rectLabel))
         {
            info.loc.setLocation(midDeg, outerRadius*0.95);
            vp.userUnitsToThousandthInches(info.loc);
            info.loc.setLocation(info.loc.getX() + displacedDX, info.loc.getY() + displacedDY);
            info.rotDeg = midDeg + ((midDeg > 270) ? 0 : 180);
            info.hAlign = midDeg > 270 ? TextAlign.TRAILING : TextAlign.LEADING;
            return(info);
         }
         
         fontSize -= 2;
      } while(fontSize >= 6);
     
      // as last resort, if the pie slice is annular (non-zero inner radius), we try to center the label in the annular
      // section, perpendicular to the radial line bisecting the slice.
      if(innerRadius > 0)
      {
         fontSize = getFontSizeInPoints();
         do
         {
            info.fontSizePts = fontSize;
            info.attrLabel = fromStyledText(info.label, getFontFamily(), fontSize, getFillColor(),
                  getFontStyle(), true);
            painter.setText(info.attrLabel);
            painter.updateFontRenderContext(g2);
            painter.invalidateBounds();
            painter.getBounds2D(rectLabel);
            
            info.loc.setLocation(90, (innerRadius + outerRadius)/2);
            vp.userUnitsToThousandthInches(info.loc);
            if(isSliceDisplaced(idx))
               info.loc.setLocation(info.loc.getX(),
                     info.loc.getY() + Math.sqrt(displacedDX*displacedDX + displacedDY*displacedDY));
            rectLabel.setFrame(info.loc.getX()-rectLabel.getWidth()/2, info.loc.getY()-rectLabel.getHeight()/2, 
                  rectLabel.getWidth(), rectLabel.getHeight());
            p = vp.getPhysicalUserOrigin(p);
            if(slicePath.createTransformedShape(AffineTransform.getRotateInstance((90-midDeg)*Math.PI/180, 
                  p.getX(), p.getY())).contains(rectLabel))
            {
               info.loc.setLocation(midDeg, (innerRadius+outerRadius)/2);
               vp.userUnitsToThousandthInches(info.loc);
               info.loc.setLocation(info.loc.getX() + displacedDX, info.loc.getY() + displacedDY);
               info.rotDeg = (midDeg < 180 || midDeg > 360) ? midDeg - 90 : midDeg - 270;
               info.hAlign = TextAlign.CENTERED;
               return(info);
            }
            
            fontSize -= 2;
         } while(fontSize >= 6);
      }

      // no scenarios worked! Label is not rendered.
      return(null);
   }
   
   /**
    * Cached rectangle bounding only the marks made by this pie chart node. An empty rectangle indicates that the
    * element makes no marks when "rendered". If null, the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;
   

   //
   // PSTransformable
   //
   
   public void toPostscript(PSDoc psDoc)
   {
      // never rendered inside a non-polar graph
      FGNGraph g = getParentGraph();
      if(g == null || g.getCoordSys() != FGNGraph.CoordSys.POLAR) return;
      FViewport2D vp = getParentViewport();
      if(vp == null || slices == null || slices.length == 0) return;
      
      // if pie slice labels are drawn, we need a Graphics2D to measure text; create one from a buffered image
      Graphics2D g2BI = null;
      if(sliceLabelMode != LabelMode.OFF)
      {
         Image img = FCIcons.V4_BROKEN.getImage();
         BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
         g2BI = bi.createGraphics();
      }
      
      try
      {
         psDoc.startElement(this);
         
         Point2D origin = vp.getPhysicalUserOrigin();
         
         // each pie slice is defined by its outer arc, which is defined its two endpoints.
         List<Point2D> arcPoints = new ArrayList<Point2D>();
         arcPoints.add(new Point2D.Double());
         arcPoints.add(new Point2D.Double());
         
         // if inner radius non-zero, compute its value in painting coordinates.
         double baseRad = 0;
         if(innerRadius != 0)
         {
            arcPoints.get(0).setLocation(0, innerRadius);
            vp.userUnitsToThousandthInches(arcPoints.get(0));
            baseRad = origin.distance(arcPoints.get(0));
         }
         
         // start rendering slices in order starting at theta=90deg and going counterclockwise
         double theta = 90;
         boolean isDisplaced = false;
         double ofsRad = (outerRadius * radialOffset) / 100;
         for(int i=0; i<slices.length; i++) if(slices[i] > 0)
         {
            double arcDeg = 360 * slices[i];
            arcPoints.get(0).setLocation(theta, outerRadius);
            arcPoints.get(1).setLocation(theta+arcDeg, outerRadius);
            vp.userUnitsToThousandthInches(arcPoints);
            
            // if slice is NOT radially displaced, make sure wedge origin is the polar graph origin (in case previous wedge
            // was displaced). Else, origin is offset from the polar origin, AND we have to offset the arc points as well.
            if(!isSliceDisplaced(i))
            {
               if(isDisplaced)
               {
                  origin = vp.getPhysicalUserOrigin(origin);
                  isDisplaced = false;
               }
            }
            else
            {
               // adjust origin IAW radial offset. It will be different for each displaced slice.
               if(isDisplaced) origin = vp.getPhysicalUserOrigin(origin);
               double oriX = origin.getX();
               double oriY = origin.getY();
               origin.setLocation(theta + arcDeg/2.0, ofsRad);
               vp.userUnitsToThousandthInches(origin);
               double deltaX = origin.getX() - oriX;
               double deltaY = origin.getY() - oriY;

               // adjust the arc endpoints also
               arcPoints.get(0).setLocation(arcPoints.get(0).getX() + deltaX, arcPoints.get(0).getY() + deltaY);
               arcPoints.get(1).setLocation(arcPoints.get(1).getX() + deltaX, arcPoints.get(1).getY() + deltaY);
               
               isDisplaced = true;
            }
            
            // because PSDoc will alter the list of arc points, we have to create a new list each time
            List<Point2D> pts = new ArrayList<Point2D>(arcPoints);
            psDoc.renderConcentricWedges(origin, pts, baseRad, getDataGroupColor(i));
            
            theta += arcDeg;
            
            // render pie slice label, if applicable
            SliceLabelInfo info = this.prepareSliceLabel(i, g2BI);
            if(info != null)
               psDoc.renderText(info.label, info.loc.getX(), info.loc.getY(), info.hAlign, TextAlign.CENTERED, 
                     info.rotDeg, info.fontSizePts * 1000.0 / 72.0);
         }

         psDoc.endElement();         
      }
      finally
      {
         if(g2BI != null) g2BI.dispose();
      }
      
   }
   
   // 
   // Object
   //

   /**
    * This override ensures that the rendering infrastructure for the clone is independent of the pie chart node cloned. 
    * The clone will reference the same data set, however!
    */
   @Override protected Object clone()
   {
      PieChartNode copy = (PieChartNode) super.clone();
      copy.rBoundsSelf = null;
      copy.slices = (slices != null) ? Arrays.copyOf(slices, slices.length) : null;
      return(copy);
   }
}
