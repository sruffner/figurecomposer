package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.MultiShapePainter;
import com.srscicomp.common.g2dutil.PainterStyle;
import com.srscicomp.common.g2dutil.PolylinePainter;
import com.srscicomp.common.g2dutil.MultiShapePainter.PaintedShape;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;


/**
 * <b>ScatterPlotNode</b> is a <b><i>data presentation</i></b> element that renders a set of 2D points <i>{(X,Y)}</i> as
 * a "scatter" plot, or a set of 3D points <i>{(X, Y, Z)}</i> as a 2D "bubble" plot, where the Z-coordinate controls the
 * relative size and/or color of the marker symbol drawn at location (X,Y) in a 2D graph. The scatter plot points can
 * be connected by a polyline as well, although that is not typical usage ("radar plots" is one possibility).
 * 
 * <p>The scatter plot supports two data formats for its underlying data set source: {@link Fmt#PTSET} for 2D
 * scatter plots and {@link Fmt#XYZSET} for 3D bubble plots. It does not render standard deviation data. Other
 * than the data source, the key defining attributes for the scatter plot node are: display mode M, minimum and maximum
 * symbol sizes 0 <= L < S, symbol type, fill color C, and stroke properties. The stroke properties determine how the
 * individual marker symbols in the scatter/bubble plot are stroked. There are no text attributes associated with this
 * presentation node. The display mode and data source format will determine how the individual points in the source set
 * are rendered:
 * <ul>
 * <li>M = "scatter". A typical scatter plot. A marker symbol of constant size S and fill color C is drawn at each 
 * well-defined point (X,Y) in the data source. Note that this mode simply ignores Z-coordinate data if the data source
 * format is <b>XYZSET</b>.</li>
 * <li>M = "trendline". Same as "scatter", except a line segment is drawn representing the least-mean-squares regression
 * line fitting the 2D data (again, Z-coordinate data is ignored). <i>The regression line segment will not be drawn if 
 * the parent graph is polar</i>.</li>
 * <li>M = "sizeBubble". A bubble plot in which the size of each marker symbol varies proportional to the Z-coordinate:
 * <i>size = max(L, S*|Z|/Zam)</i>, where Zam is the maximum observed absolute value of Z over the set. Note that the
 * scatter plot is really intended for Z data that is all of the same sign. If the underlying data source is not 3D,
 * then all markers have the same size S. All are filled with color C.</li>
 * <li>M = "colorBubble". A bubble plot in which all marker symbols drawn are size S, but the fill color depends on the
 * Z-coordinate AND the current state of the parent graph's "color axis". An integer color index I is computed from the
 * color axis range [Z0..Z1] and the Z-coordinate value, and this index selects a color from the 256-element color map
 * currently assigned to the color axis. If the underlying data source is not 3D, all symbols are filled with color C;
 * i.e., like a standard XY scatter plot. <i>NOTE: Not all marker symbol types are closed; the user should be sure to
 * select a closed symbol for this display mode.</i></li>
 * <li>M = "colorSizeBubble". A bubble plot in which both marker symbol size and fill color vary with the Z-coordinate
 * value, as described above. Again, this mode really only applies when the underlying data source is 3D; if the data
 * is 2D, it will be drawn as a typical scatter plot with like-sized and like-colored marker symbols.</li> 
 * </ul>
 * </p>
 * 
 * <p>A single component node, {@link ScatterLineStyleNode}, contains the stroke properties for the LMS regression line
 * in the "trendline" display mode -- so that the regression line can be stroked differently from the marker symbols. In
 * the other display modes, if a non-empty stroke is defined, the scatter points will be connected by a closed polyline
 * (the last point is connected to the first) and the closed region is filled with the fill color defined on this 
 * component node. Such usage is rare -- but it could be used to create a "radar plot" in a polar graph or simply to
 * bound a region in any 2D graph.</p>
 *
 * @author sruffner
 */
public class ScatterPlotNode extends FGNPlottableData implements Cloneable
{
   /**
    * Construct a scatter plot node initially configured to display an empty 2D point set as a typical fixed-symbol, X-Y
    * scatter plot, using 0.1-in circle symbol. It will make no marks in the parent graph. All styling attributes are
    * initially implicit (inherited); no font-related styles are associated with this node. It has no title initially.
    * The component {@link ScatterLineStyleNode} is initially configured with a zero stroke width and a fill color with
    * zero alpha.
    */
   public ScatterPlotNode()
   {
      super(HASSTROKEATTRS|HASSTRKPATNATTR|HASFILLCATTR|HASTITLEATTR|ALLOWATTRINTITLE);
      setTitle("");
      mode = DisplayMode.SCATTER;
      symbol = Marker.CIRCLE;
      size = new Measure(0.1, Measure.Unit.IN);
      minSize = new Measure(0, Measure.Unit.IN);
      addComponentNode(new ScatterLineStyleNode());
   }

   /**
    * When the parent graph's color map changes, the appearance of this scatter plot is affected only if the underlying
    * data source is a 3D point set and the display mode is one of the "color bubble' modes. In that scenario, this
    * method will update the internal painter that renders the plot.
    */
   @Override public boolean onColorMapChange() 
   { 
      if(isFillColorConstant()) return(false);
      updatePainters();
      return(true);  
   }
   
   @Override public FGNodeType getNodeType() { return(FGNodeType.SCATTER); }


   //
   // Properties
   //

   /** 
    * An enumeration of the different data display modes supported by {@link ScatterPlotNode}. 
    */
   public enum DisplayMode
   {
      /** 
       * Marker symbol drawn at each well-defined point (X,Y). Size and fill color the same for all symbols. Any
       * Z-coordinate data in the data source is ignored in this mode. If the component {@link ScatterLineStyleNode}
       * defines a non-empty stroke, a closed polyline connects all the points in order, with the last connecting back 
       * to the first. The resulting region is filled with the component node's fill color.
       */
      SCATTER("scatter"), 
      /** 
       * LMS regression line segment drawn through data IAW stroke properties defined on component node. Z-coordinate 
       * data is ignored in this mode. No regression line drawn if the parent graph is polar.
       */
      TRENDLINE("trendline"),
      /** Size of marker symbol at (X,Y) proportional to Z-coordinate value. */
      SIZEBUBBLE("sizeBubble"),
      /** Fill color for marker symbol indexed from color map based on Z-coordinate value and color axis range. */
      COLORBUBBLE("colorBubble"),
      /** Both size and fill color of marker symbol reflect Z-coordinate value. */
      COLORSIZEBUBBLE("colorSizeBubble"); 
      
      @Override public String toString() { return(niceName); }
      
      DisplayMode(String s) { niceName = s; }
      
      /** The display mode name as it should appear in GUI and in FypML markup. */
      private final String niceName;
   }

   /** The current data display mode. */
   private DisplayMode mode;

   /**
    * Get the current data display mode for this scatter plot node. <b>Note these caveats</b>:
    * 
    * <ul>
    * <li>The "bubble" display modes are intended only for a 3D point set {(X,Y,Z)}, while the "scatter" and "trendline"
    * modes are intended for a 2D point set {(X,Y}}. If the current data source is 2D, changing to one of the "bubble" 
    * modes has no effect; the plot is always rendered in the "scatter" mode.</li>
    * <li>If the current data source is 3D and the display mode is "scatter" or "trendline", Z-coordinate values in the 
    * data are simply ignored.</li>
    * </ul>
    * 
    * @return The current data display mode.
    */
   public DisplayMode getMode() { return(mode); }

   /**
    * Set the data display mode for this scatter plot node. If a change is made, {@link #onNodeModified} is invoked.
    * See {@link #getMode()} for a discussion of the relationship between the data source and the display mode.
    * 
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

   
   /** The type of marker symbol drawn at each well-defined point in the scatter plot. */
   private Marker symbol;

   /**
    * Get the type of marker symbol drawn at each well-defined point in the scatter plot.
    * 
    * @return The marker symbol shape, specified in a unit "design box".
    */
   public Marker getSymbol() { return(symbol); }

   /**
    * Set the type of marker symbol drawn at each well-defined point in the scatter plot. If a change is made, {@link  
    * #onNodeModified} is invoked. <i>A closed symbol is recommended if the display mode is such that the symbol's
    * fill color reflects the Z-coordinate value of the point (X,Y,Z); non-closed symbols are not filled.</i>
    * 
    * @param symbol The desired marker symbol shape, specified in a unit "design box". A null value is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setSymbol(Marker symbol)
   {
      if(symbol == null) return(false);

      if(this.symbol != symbol)
      {
         if(doMultiNodeEdit(FGNProperty.TYPE, symbol)) return(true);
         
         Marker oldType = this.symbol;
         this.symbol = symbol;
         if(this.areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.TYPE);
            String desc = "Set scatter plot marker symbol to: " + this.symbol.toString();
            FGNRevEdit.post(this, FGNProperty.TYPE, this.symbol, oldType, desc);
         }
      }
      return(true);
   }

   /**
    * Constraints on a scatter plot node's marker symbol size: must use non-relative units (in, cm, mm or pt);
    * limited to 4 significant and 3 fractional digits; and must lie in [0 .. 2] inches.
    */
   public final static Measure.Constraints MAXSYMSIZECONSTRAINTS = new Measure.Constraints(0, 2000.0, 4, 3);

   /** Maximum marker symbol size. */
   private Measure size;

   /**
    * Get the maximum marker symbol size for the scatter plot. 
    * <p>In bubble plots in which the symbol size is proportional to the Z-coordinate of a 3D point (X,Y,Z), this
    * symbol size will correspond to the maximum observed Z-coordinate value, <i>Zmax</i>, over the data set. For any
    * other value Z, the symbol is drawn at size max(L, S*Z/Zmax), where S is this maximum marker symbol size and L is
    * the minimum marker symbol size. If the display mode is such that all symbols are drawn at the same size, then all
    * are drawn at this size.</p>
    * 
    * @return The maximum marker symbol size, with associated units of measure (only physical units allowed).
    * @see #getMinSymbolSize()
    */
   public Measure getMaxSymbolSize() { return(size); }

   /**
    * Set the maximum marker symbol size for this scatter plot. If a change is made, {@link #onNodeModified} is
    * invoked.
    * 
    * @param m The new size. It is constrained to satisfy {@link #MAXSYMSIZECONSTRAINTS}. A null value is rejected, as
    * is a value less than or equal to the current minimum symbol size.
    * @return True if successful; false if value was rejected.
    * @see #getMaxSymbolSize()
    */
   public boolean setMaxSymbolSize(Measure m)
   {
      if(m == null) return(false);
      m = MAXSYMSIZECONSTRAINTS.constrain(m);
      if(m.toMilliInches() <= minSize.toMilliInches()) return(false);

      boolean changed = (size != m) && !Measure.equal(size, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.SIZE, m)) return(true);
         
         Measure oldSize = size;
         size = m;
         if(this.areNotificationsEnabled())
         {
            if(oldSize.toMilliInches() != size.toMilliInches())
               onNodeModified(FGNProperty.SIZE);
            String desc = "Set scatter plot max symbol size to: " + size.toString();
            FGNRevEdit.post(this, FGNProperty.SIZE, size, oldSize, desc);
         }
      }
      return(true);
   }

   /** Minimum marker symbol size. */
   private Measure minSize;

   /**
    * Get the minimum marker symbol size for the scatter plot.
    *
    * @return The minimum marker symbol size, with associated units of measure (only physical units allowed).
    * @see #getMaxSymbolSize()
    */
   public Measure getMinSymbolSize() { return(minSize); }

   /**
    * Set the minimum marker symbol size for this scatter plot. If a change is made, {@link #onNodeModified} is
    * invoked.
    *
    * @param m The new size. It is constrained to satisfy {@link #MAXSYMSIZECONSTRAINTS}. A null value is rejected, as
    * is a value greater than or equal to the maximum symbol size.
    * @return True if successful; false if value was rejected.
    * @see #getMaxSymbolSize()
    */
   public boolean setMinSymbolSize(Measure m)
   {
      if(m == null) return(false);
      m = MAXSYMSIZECONSTRAINTS.constrain(m);
      if(m.toMilliInches() >= size.toMilliInches()) return(false);

      boolean changed = (minSize != m) && !Measure.equal(minSize, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.MINSIZE, m)) return(true);

         Measure oldSize = minSize;
         minSize = m;
         if(this.areNotificationsEnabled())
         {
            if(oldSize.toMilliInches() != minSize.toMilliInches())
               onNodeModified(FGNProperty.MINSIZE);
            String desc = "Set scatter plot min symbol size to: " + minSize.toString();
            FGNRevEdit.post(this, FGNProperty.MINSIZE, minSize, oldSize, desc);
         }
      }
      return(true);
   }


   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok;
      switch(p)
      {
         case MODE : ok = setMode((DisplayMode)propValue); break;
         case TYPE: ok = setSymbol((Marker)propValue); break;
         case SIZE: ok = setMaxSymbolSize((Measure)propValue); break;
         case MINSIZE: ok = setMinSymbolSize((Measure)propValue); break;
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
         case TYPE: value = getSymbol(); break;
         case SIZE: value = getMaxSymbolSize(); break;
         case MINSIZE: value = getMinSymbolSize(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }


   //
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   /**
    * The node-specific properties exported in a scatter plot node's style set include the display mode, the marker
    * symbol type, the minimum and maximum symbol sizes, and the include-in-legend flag.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.MODE, getMode());
      styleSet.putStyle(FGNProperty.TYPE, getSymbol());
      styleSet.putStyle(FGNProperty.SIZE, getMaxSymbolSize());
      styleSet.putStyle(FGNProperty.MINSIZE, getMinSymbolSize());
      styleSet.putStyle(FGNProperty.LEGEND, getShowInLegend());
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
      
      Marker m = (Marker) applied.getCheckedStyle(FGNProperty.TYPE, getNodeType(), Marker.class);
      if(m != null && !m.equals(restore.getStyle(FGNProperty.TYPE)))
      {
         symbol = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.TYPE);

      Measure sz = (Measure) applied.getCheckedStyle(FGNProperty.SIZE, getNodeType(), Measure.class);
      if(sz != null && (!sz.equals(restore.getStyle(FGNProperty.SIZE))) &&
            (MAXSYMSIZECONSTRAINTS.constrain(sz).toMilliInches() > minSize.toMilliInches()))
      {
         size = MAXSYMSIZECONSTRAINTS.constrain(sz);
         changed = true;
      }
      else restore.removeStyle(FGNProperty.SIZE);

      sz = (Measure) applied.getCheckedStyle(FGNProperty.MINSIZE, getNodeType(), Measure.class);
      if(sz != null && (!sz.equals(restore.getStyle(FGNProperty.MINSIZE))) &&
            (MAXSYMSIZECONSTRAINTS.constrain(sz).toMilliInches() < size.toMilliInches()))
      {
         minSize = MAXSYMSIZECONSTRAINTS.constrain(sz);
         changed = true;
      }
      else restore.removeStyle(FGNProperty.MINSIZE);

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
    * Initializes the scatter plot node's data set to a 2D set of 25 points randomly scattered over the available X-Y
    * range of the graph.
    */
   @Override protected void initializeDefaultData(double[] axisRng, boolean isPolar)
   {
      float x0 = (float) axisRng[0];
      float x1 = (float) axisRng[1];
      float y0 = (float) axisRng[2];
      float y1 = (float) axisRng[3];
      
      float[] fData = new float[25*2];
      for(int i=0; i<25; i++)
      {
         fData[i*2] = x0 + (x1-x0) * ((float) Math.random());
         fData[i*2+1] = y0 + (y1-y0) * ((float) Math.random());
      }
      
      DataSet ds = DataSet.createDataSet(getDataSet().getID(), Fmt.PTSET, null, 25, 2, fData);
      if(ds != null) setDataSet(ds);
   }

   @Override protected boolean recalcDataRange(Object hint)
   {
      boolean needRecalc = (hint == null) || (hint == FGNProperty.SRC);
      if(!needRecalc) return(false);
      
      DataSet set = getDataSet();

      boolean changed = (cachedDataRange[0] != set.getXMin()) || (cachedDataRange[1] != set.getXMax());
      changed = changed || (cachedDataRange[2] != set.getYMin()) || (cachedDataRange[3] != set.getYMax());
      changed = changed || (cachedDataRange[4] != set.getZMin()) || (cachedDataRange[5] != set.getZMax());
      if(changed)
      {
         cachedDataRange[0] = set.getXMin();
         cachedDataRange[1] = set.getXMax();
         cachedDataRange[2] = set.getYMin();
         cachedDataRange[3] = set.getYMax();
         cachedDataRange[4] = set.getZMin();
         cachedDataRange[5] = set.getZMax();
      }
      return(changed);
   }

   /** A scatter plot renders only 2D or 3D point sets: {@link Fmt#PTSET} or {@link Fmt#XYZSET}. */
   @Override public boolean isSupportedDataFormat(Fmt fmt) { return(fmt == Fmt.PTSET || fmt == Fmt.XYZSET); }


   /** The data formats that may be rendered by a scatter plot node. */
   private final static Fmt[] supportedFormats = new Fmt[] {Fmt.PTSET, Fmt.XYZSET};
   
   @Override public Fmt[] getSupportedDataFormats() { return(supportedFormats); }
   
   /** The scatter plot node draws a marker symbol for each well-defined point in the data source. */
   @Override public boolean usesSymbols() { return(true); }

   /** The scatter plot can represent Z data only if its underlying data set contains Z data. */
   @Override public boolean hasZData() { return(getDataSet().getFormat() == Fmt.XYZSET); }

   /** 
    * The legend entry for a scatter plot node shows only the marker symbol -- no trace line segment -- unless the
    * display mode is {@link DisplayMode#TRENDLINE}. Given the way legend entries are currently handled, it is necessary
    * to provide a custom legend entry to handle this scenario.
    */
   @Override public List<LegendEntry> getLegendEntries()
   {
      if(!getShowInLegend()) return(null);
      List<LegendEntry> out = new ArrayList<>();
      out.add(new ScatterPlotLegendEntry(this));
      return(out);
   }
   
   /**
    * Get the component node that determines the appearance of the LMS regression line in the "trendline" display
    * mode, or in any other display mode, the region formed by connecting all the scatter points in order (and 
    * connecting the last to the first).
    */
   public ScatterLineStyleNode getLineStyle() { return((ScatterLineStyleNode)getComponentNodeAt(0)); }

   
   //
   // Focusable/Renderable support
   //
   
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null || shapePainter == null)
      {
         updatePainters();
         shapePainter.invalidateBounds();
         rBoundsSelf = shapePainter.getBounds2D(null);
         linePainter.invalidateBounds();
         Utilities.rectUnion(rBoundsSelf, linePainter.getBounds2D(null), rBoundsSelf);
         
         // if parent graph clips its data, then we need to clip the render bounds to the graph's data viewport
         FGNGraph g = getParentGraph();
         if(g != null && g.getClip())
            Rectangle2D.intersect(rBoundsSelf, g.getBoundingBoxLocal(), rBoundsSelf);
      }
      return((Rectangle2D)rBoundsSelf.clone());
   }

   /**
    * This method releases the internal painter used to render the scatter plot, as well as the cached rectangle 
    * bounding any marks made by that painter.
    */
   @Override protected void releaseRenderResourcesForSelf() 
   { 
      rBoundsSelf = null; 
      shapePainter = null;
      linePainter = null;
   }

   /** Render this scatter plot node into the current graphics context IAW its current display mode. */
   @Override public boolean render(Graphics2D g2d, RenderTask task)
   {
      FGNGraph g = getParentGraph();
      if(g == null) return(true);

      if(needsRendering(task))
      {
         if(shapePainter == null) updatePainters();
         
         // the LMS regression line is drawn on top of the markers in "trendline" mode, but the "connect the dots"
         // polyline is rendered before the marker symbols in all other display modes
         if(mode == DisplayMode.TRENDLINE)
         {
            if(!shapePainter.render(g2d, task)) return(false);
            return linePainter.render(g2d, task);
         }
         else
         {
            if(!linePainter.render(g2d, task)) return(false);
            return shapePainter.render(g2d, task);
         }
      }
      return(true);
   }

   /**
    * Cached rectangle bounding only the marks made by this scatter plot node. An empty rectangle indicates that the
    * element makes no marks when "rendered". If null, the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;

   /** The painter which is responsible for rendering the scatter plot symbols IAW this node's current state. */
   private MultiShapePainter shapePainter = null;

   /** 
    * Paints the regression line segment in {@link DisplayMode#TRENDLINE} mode, or the closed and filled polyline
    * connecting all the scatter points in the other display modes.
    */
   private PolylinePainter linePainter = null;
   
   /** Create/update the painters responsible for rendering this scatter plot node IAW its current definition. */
   private void updatePainters()
   {
      if(shapePainter == null) shapePainter = new MultiShapePainter();
      shapePainter.setShapeProducer(new ShapeProducer());
      if(linePainter == null)
      {
         linePainter = new PolylinePainter(getLineStyle(), null);
         linePainter.setFilled(true);
         linePainter.setConnectionPolicy(PolylinePainter.ConnectPolicy.CONNECTED);
         linePainter.setAllowChunking(true);
      }
      if(mode == DisplayMode.TRENDLINE)
         linePainter.setLocationProducer(new RegressionLineProducer());
      else
         linePainter.setLocationProducer(new OutlineProducer()); 
   }

   /**
    * Does this scatter plot node render all marker symbols at the same size? This will be the case in the display modes
    * {@link DisplayMode#SCATTER}, {@link DisplayMode#TRENDLINE}, and {@link DisplayMode#COLORBUBBLE}, and if the 
    * underlying data source is 2D.
    * @return True only if all marker symbols are drawn at the same size ({@link #getMaxSymbolSize()}) when this scatter
    * plot is rendered.
    */
   private boolean isSymbolSizeConstant()
   {
      return(getDataSet().getFormat() == Fmt.PTSET || mode == DisplayMode.SCATTER || mode == DisplayMode.TRENDLINE ||
            mode == DisplayMode.COLORBUBBLE);
   }
   
   /**
    * Does this scatter plot fill all (closed) marker symbols with the same fill color? This will be the case in the
    * display modes {@link DisplayMode#SCATTER}, {@link DisplayMode#TRENDLINE}, and {@link DisplayMode#SIZEBUBBLE}, and 
    * if the underlying data is 2D.
    * @return True only if all marker symbols are filled with the same color when this scatter plot is rendered.
    */
   private boolean isFillColorConstant()
   {
      return(getDataSet().getFormat() == Fmt.PTSET || mode == DisplayMode.SCATTER || mode == DisplayMode.TRENDLINE ||
            mode == DisplayMode.SIZEBUBBLE);
   }

   
   //
   // PSTransformable
   //
   
   public void toPostscript(PSDoc psDoc)
   {
      FGNGraph g = getParentGraph();
      if(g == null) return;
      
      psDoc.startElement(this);

      // component node controls stroking of LMS regression line or "connect the dots" polyline
      ScatterLineStyleNode lineStyle = getLineStyle();

      // if not in "trendline" mode and the polyline is drawn, stroke and fill the region bounded by it...
      // NOTE that we populate the entire point list and render the polygon fill in one go -- which may not work
      // if there are a great many points!
      List<Point2D> ptList = new ArrayList<>();
      if((mode != DisplayMode.TRENDLINE) && lineStyle.isStroked())
      {
         OutlineProducer producer = new OutlineProducer();
         while(producer.hasNext())
         {
            Point2D p = producer.next();
            if(Utilities.isWellDefined(p)) ptList.add(new Point2D.Double(p.getX(), p.getY()));
         }
         if(ptList.size() > 2)
         {
            ptList.add(null);
            psDoc.startElement(lineStyle, lineStyle, true);
            psDoc.renderPolygons(ptList, true);
            psDoc.endElement();
         }
         ptList.clear();
      }

      // generate the list of locations at which symbols are rendered and, depending on the display mode, the
      // size and/or fill color for each symbol.
      ShapeProducer producer = new ShapeProducer();
      List<Double> symbolSizes = new ArrayList<>();
      if(producer.sizeConstant) symbolSizes.add(producer.maxSymSizeMI);
      List<Color> fillColors = producer.fillConstant ? null : new ArrayList<>();
      
      // if there are many thousands of points in the scatter plot, the PS document generated could exhaust the virtual
      // memory of the Postscript interpreter. Since the scatter points are not connected in any way, we render at 
      // most 20K points at a time.
      while(producer.hasNext())
      {
         // NOTE: this works b/c the inner class itself implements the PaintedShape implementation
         producer.next();
         Point2D p = new Point2D.Double();
         p.setLocation(producer.getLocation());
         ptList.add(p);
         if(!producer.sizeConstant) symbolSizes.add(producer.currSymSzMI);
         if(!producer.fillConstant) fillColors.add(new Color(producer.getFillColor().getRGB()));
         
         if(ptList.size() == 20000)
         {
            psDoc.renderMultipleAdornments(ptList, symbolSizes, fillColors, getSymbol(), true);
            ptList.clear();
            if(!producer.sizeConstant) symbolSizes.clear();
            if(!producer.fillConstant) fillColors.clear(); 
         }
      }

      // render the last chunk of <20K points
      if(!ptList.isEmpty()) psDoc.renderMultipleAdornments(ptList, symbolSizes, fillColors, getSymbol(), true);
      
      // render regression line segment, if applicable
      if(mode == DisplayMode.TRENDLINE && lineStyle.isStroked())
      {
         psDoc.startElement(lineStyle, lineStyle, true);
         
         RegressionLineProducer lineProducer = new RegressionLineProducer();
         try
         {
            Point2D p0 = lineProducer.next();
            Point2D p1 = lineProducer.next();
            psDoc.renderLine(p0, p1);
         }
         catch(NoSuchElementException ignored) {}
         
         psDoc.endElement();
      }
      psDoc.endElement();
   }
   
   // 
   // Object
   //

   /**
    * This override ensures that the rendering infrastructure for the clone is independent of the scatter plot node that
    * was cloned. The clone will reference the same data set, however!
    */
   @Override protected ScatterPlotNode clone() throws CloneNotSupportedException
   {
      ScatterPlotNode copy = (ScatterPlotNode) super.clone();
      copy.rBoundsSelf = null;
      copy.shapePainter = null;
      copy.linePainter = null;
      return(copy);
   }
   
   
   /**
    * Helper class defines an iterator over the set of shapes that are to be painted by the {@link MultiShapePainter}
    * that renders the scatter plot node in its current state. It serves both as the iterator implementation and the 
    * iterator provider (it simply provides fresh copies of itself).
    * 
    * <p>For each well-defined data point (X,Y,Z) in the underlying data set, the iterator supplies the shape
    * primitive as well as its location, fill color, and stroking properties. The stroking properties are the same for
    * all shapes and match the stroking properties of the scatter plot node itself. In the {@link DisplayMode#SCATTER}
    * display mode, or if the data set is 2D instead of 3D, the shape primitive and fill color are the same for all
    * shapes; otherwise, the shape primitive's size and/or fill color can vary with the display mode chosen.</p>
    * 
    * @author  sruffner
    */
   private class ShapeProducer implements Iterable<PaintedShape>, Iterator<PaintedShape>, PaintedShape
   {
      ShapeProducer()
      {
         graphVP = getParentViewport();
         style = ScatterPlotNode.this;
         symbol = getSymbol();
         maxSymSizeMI = getMaxSymbolSize().toMilliInches();
         minSymSizeMI = getMinSymbolSize().toMilliInches();
         set = getDataSet();
         sizeConstant = ScatterPlotNode.this.isSymbolSizeConstant();
         fillConstant = ScatterPlotNode.this.isFillColorConstant();
         
         shapePrimitive = new GeneralPath();
         if(sizeConstant)
         {
            PathIterator pi = symbol.getDesignShape().getPathIterator(
                  AffineTransform.getScaleInstance(maxSymSizeMI, maxSymSizeMI));
            shapePrimitive.setWindingRule(pi.getWindingRule());
            shapePrimitive.append(pi, false);
            currSymSzMI = maxSymSizeMI;
         }
         else
            xfm = new AffineTransform();
         
         fillC = ScatterPlotNode.this.getFillColor();
         if(fillConstant)
         {
            zRange = null;
            cmapLUT = null;
            isLogCMap = false;
         }
         else
         {
            ColorBarNode cbar = getParentGraph().getColorBar();
            double start = cbar.getStart();
            double end = cbar.getEnd();
            if(start > end)
            {
               double d = start; 
               start = end; 
               end = d;
            }
            else if(start == end) end = start + 1;
            
            zRange = new float[] { (float)start, (float)end };
            cmapLUT = cbar.getColorLUT().getLUT();
            isLogCMap = cbar.isLogarithmic();
         }
            
         
         nPtsSoFar = 0;
         pCurrent = new Point2D.Double();
      }

      /** 
       * Construct a copy of the specified shape producer.
       * @param src The shape producer to copy.
       */
      private ShapeProducer(ShapeProducer src)
      {
         graphVP = src.graphVP;
         style = src.style;
         symbol = src.symbol;
         maxSymSizeMI = src.maxSymSizeMI;
         minSymSizeMI = src.minSymSizeMI;
         set = src.set;
         
         sizeConstant = src.sizeConstant;
         if(sizeConstant) 
         {
            shapePrimitive = src.shapePrimitive;
            currSymSzMI = maxSymSizeMI;
         }
         else
         {
            shapePrimitive = new GeneralPath();  // this is mutable, so the clone can't reference the src member!
            xfm = new AffineTransform();
         }
         
         fillConstant = src.fillConstant;
         fillC = src.fillC;
         zRange = src.zRange;
         cmapLUT = src.cmapLUT;
         isLogCMap = src.isLogCMap;
         

         nPtsSoFar = 0;
         pCurrent = new Point2D.Double();
      }
      
      public Iterator<PaintedShape> iterator() { return(new ShapeProducer(this)); }

      public boolean hasNext() { return(nPtsSoFar < set.getDataSize(-1)); }

      public PaintedShape next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         pCurrent.setLocation(set.getX(nPtsSoFar, -1), set.getY(nPtsSoFar, -1));
         if(graphVP != null) graphVP.userUnitsToThousandthInches(pCurrent);
         
         // if size and/or fill color varies with Z, compute the shape primitive and/or fill color accordingly. If the
         // Z-coordinate is not well-defined, then set shape location to (NaN,NaN) so shape won't be drawn.
         if(!(sizeConstant && fillConstant))
         {
            float z = set.getZ(nPtsSoFar);
            if(!Utilities.isWellDefined(z)) 
               pCurrent.setLocation(Float.NaN, Float.NaN);
            else
            {
               if(!fillConstant) 
                  fillC = mapZValueToColor(z);
               if(!sizeConstant)
               {
                  shapePrimitive.reset();
                  xfm.setToIdentity();
                  
                  // (note degenerate case: all Z-coordinates are 0)
                  double zAbsMax = Math.max(Math.abs(set.getZMin()), Math.abs(set.getZMax()));
                  double sz = (zAbsMax == 0) ? maxSymSizeMI :
                        Math.max(minSymSizeMI, Math.abs(z) * maxSymSizeMI / zAbsMax);

                  xfm.scale(sz, sz);
                  currSymSzMI = sz;
                  
                  PathIterator pi = symbol.getDesignShape().getPathIterator(xfm);
                  shapePrimitive.setWindingRule(pi.getWindingRule());
                  shapePrimitive.append(pi, false);
               }
            }
         }
         
         ++nPtsSoFar;
         
         return(this);
      }

      /**
       * Helper method maps the Z-coordinate value to a color in the parent graph's color map. The mapping is linear or
       * logarithmic. Let L be the length of the color map's look-up table LUT, and let [R0..R1] be the graph's color
       * axis (Z-axis) range, where R0 &lt; R1. Then:
       * <ul>
       * <li>Linear: index = floor( (Z-R0) * L / (R1-R0) ), range-restricted to [0..L-1].</li>
       * <li>Log: index = floor( log(Z-R0+1) * L / log(R1-R0+1) ), again range-restricted to [0..L-1].</li>
       * </ul>
       * 
       * @param z A Z-coordinate value.
       * @return The color to which the Z-coordinate is mapped through the parent graph's color map.
       */
      private Color mapZValueToColor(float z)
      {
         if(fillConstant) return(fillC);
         
         double d;
         if(isLogCMap)
            d = Math.log(z-zRange[0]+1) * cmapLUT.length / Math.log(zRange[1]-zRange[0]+1);
         else
            d = (z-zRange[0]) * cmapLUT.length / (zRange[1]-zRange[0]);
         int i = Utilities.rangeRestrict(0, cmapLUT.length-1, (int) Math.floor(d+0.5));

         return(new Color(cmapLUT[i]));
      }
      
      public void remove() { throw new UnsupportedOperationException("Removal not supported by this iterator."); }
      
      @Override public Font getFont() { return(style.getFont()); }
      @Override public Color getStrokeColor() { return(style.getStrokeColor()); }
      @Override public Stroke getStroke(float dashPhase) { return(style.getStroke(dashPhase)); }
      @Override public double getFontSize() { return(style.getFontSize()); }
      @Override public double getStrokeWidth() { return(style.getStrokeWidth()); }
      @Override public boolean isStrokeSolid() { return(style.isStrokeSolid()); }
      @Override public boolean isStroked() { return(style.isStroked()); }
      /**
       * If all scatter plot markers are filled the same, use the painter style's fill color. Else the color is
       * selected from the graph's color map based on the Z-coordinate value associated with the shape.
       */
      @Override public Color getFillColor() { return(fillC); }

      /**
       * If all scatter plot markers are drawn at the same size, this method returns the shape primitive prepared at
       * construction time. Otherwise, the marker size is computed from the Z-coordinate value associated with the
       * shape, and the unit shape primitive is scaled accordingly.
       */
      @Override public Shape getShape() { return(shapePrimitive); }

      @Override public Point2D getLocation() { return(pCurrent); }
      @Override public Point2D getStemEnd() { return(null); }
      @Override public PainterStyle getStemPainterStyle() { return(null); }
      @Override public Paint getFillPaint() { return(null); }
      @Override public boolean isClosed() { return(symbol.isClosed()); }
            
      /** The parent graph view port converts each point from user units to rendering units. */
      final FViewport2D graphVP;
      /** The scatter plot marker symbol. This provides the "unit-box" shape primitive. */
      final Marker symbol;
      /** The maximum size for the scatter plot marker symbols, in milli-inches. */
      final double maxSymSizeMI;
      /** The minimum size for the scatter plot marker symbols, in milli-inches. */
      final double minSymSizeMI;
      /** The underlying data set source. */
      final DataSet set;
      /** This painter style determines how each shape is stroked; text-related properties unused. */
      final PainterStyle style;
      /** True if all symbols are drawn at the same size. */
      final boolean sizeConstant;
      /** True if all symbols are filled with the same fill color. */
      final boolean fillConstant;
      /** The Z-axis (color axis) range for the parent graph; applicable only when fill color varies with Z. */
      final float[] zRange;
      /** The color map look-up table for the parent graph; applicable only when fill color varies with Z. */
      final int[] cmapLUT;
      /** True if color map mode is logarithmic; applicable only when fill color varies with Z. */
      final boolean isLogCMap;

      /** Number of data points processed thus far. */
      int nPtsSoFar;
      /** 
       * The current shape location. This is reused to deliver each point. IT IS ASSUMED that the consumer will NOT 
       * store a reference to this point, but will make a copy if needed.
       */
      final Point2D pCurrent;
      /** 
       * The current shape to draw. If symbol size is fixed, this is determined at construction time and never
       * changes. Otherwise, it is calculated for each data point (X,Y,Z) in the source data set.
       */
      final GeneralPath shapePrimitive;
      /** 
       * The fill color for the current shape. If fill color is fixed, this is set at construction time and never
       * changes. Otherwise, it is calculated for each data point (X,Y,Z) in the source data set.
       */
      Color fillC;
      /** 
       * The size of the (square) bounding box for the current symbol, in milli-inches. If all are rendered at the 
       * same size, this is the same value, {@link #maxSymSizeMI}, for all data points processed.
       */
      double currSymSzMI = 0;
      /** Affine transform used to scale each symbol; unused if symbol size is fixed. */
      AffineTransform xfm = null;
   }
   
   
   /**
    * Helper class defines an iterator over the two points that define the linear regression line segment for the data 
    * set in the display mode {@link DisplayMode#TRENDLINE}. It merely calculates the endpoints of the line segment at
    * construction time and iterates over the 2 (or zero, if the regression line does not exist) points.
    * 
    * <p>This solution is better than supplying a list of the 2 points as the "producer" -- bc this approach will update
    * the endpoint coordinates whenever anything (the data set, the parent graph's axis ranges, the X- or Y-coordinate 
    * offset, etc) changes that affects their values.</p>
    */
   private class RegressionLineProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      final List<Point2D> endPts;
      int nPtsSoFar;
      
      RegressionLineProducer()
      {
         FViewport2D parentVP = getParentViewport();
         FGNGraph g = getParentGraph();
         boolean ok = (getMode() == DisplayMode.TRENDLINE) && (g!=null) && (!g.isPolar()) && (!g.is3D()) &&
               (parentVP != null);
         endPts = new ArrayList<>();
         if(ok)
         {
            FGNGraph.CoordSys sys = g.getCoordSys();
            TraceNode.calculateRegressionLineSegment(getDataSet(), endPts, sys.isLogX(), sys.isLogY());
            if(endPts.size() == 2) parentVP.userUnitsToThousandthInches(endPts);
         }
         nPtsSoFar = 0;
      }
      
      @Override public Iterator<Point2D> iterator() { return(new RegressionLineProducer()); }
      
      @Override public boolean hasNext() { return(nPtsSoFar < endPts.size()); }
      @Override public Point2D next() 
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");
         return( endPts.get(nPtsSoFar++) );
      }
   }

   /**
    * Helper class iterates over all <i>well-defined</i> points in the scatter plot's underlying data source in order
    * to render a "connect the dots" polyline connecting the points. After iterating over the data set, the producer
    * generates one more point to connect the last well-defined point to the first in order to "close" the polyline.
    * 
    * <p>The polyline is rendered ONLY if the component {@link ScatterLineStyleNode} is stroked -- ie, it has a
    * non-zero stroke width and a stroke color with non-zero alpha. Otherwise, this producer generates no points.</p>
    * 
    * <p>Intended only for use in display modes <i>other than</i> {@link DisplayMode#TRENDLINE}.</p>
    */
   private class OutlineProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      OutlineProducer()
      {
         graphVP = getParentViewport();
         set = getDataSet();
         isOutlined = getLineStyle().isStroked();
         
         nPtsSoFar = 0;
         pCurrent = new Point2D.Double();
      }
      
      @Override public Iterator<Point2D> iterator() { return(new OutlineProducer()); }
      
      @Override public boolean hasNext() 
      { 
         return(isOutlined && (graphVP != null) && (nPtsSoFar < set.getDataSize(-1) + 1)); 
      }
      
      @Override public Point2D next() 
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");
         
         if(nPtsSoFar == set.getDataSize(-1))
         {
            if(pFirst != null) pCurrent.setLocation(pFirst.getX(), pFirst.getY());
            else pCurrent.setLocation(Double.NaN, Double.NaN);
         }
         else
         {
            pCurrent.setLocation(set.getX(nPtsSoFar, -1), set.getY(nPtsSoFar, -1));
            graphVP.userUnitsToThousandthInches(pCurrent);
            if(Utilities.isWellDefined(pCurrent) && (pFirst == null))
               pFirst = new Point2D.Double(pCurrent.getX(), pCurrent.getY());
         }
         ++nPtsSoFar;
         return(pCurrent);
      }
      
      /** The parent graph view port converts each point from user units to rendering units. */
      final FViewport2D graphVP;
      /** The underlying data set source. */
      final DataSet set;
      /** False if outline is not drawn, in which case no points are generated! */
      final boolean isOutlined;
      
      /** Number of data points processed thus far. */
      int nPtsSoFar;
      /** The first well-defined scatter plot point. */
      Point2D pFirst = null;
      /** 
       * The current well-defined scatter plot point. This is reused to deliver each point. IT IS ASSUMED that the 
       * consumer will NOT store a reference to this point, but will make a copy if needed.
       */
      final Point2D pCurrent;
   }
}
