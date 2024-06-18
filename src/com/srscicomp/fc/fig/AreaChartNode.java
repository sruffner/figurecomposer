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
import com.srscicomp.common.g2dutil.SingleStringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;
import com.srscicomp.fc.uibase.FCIcons;


/**
 * <b>AreaChartNode</b> is a <b><i>grouped-data presentation</i></b> element that renders a small collection of data 
 * sets as a stacked area chart. In this stacked presentation, each "band" in the area chart represents the relative 
 * contribution of a single member set to the collection, while the top of the chart represents the cumulative total 
 * across all member sets. The bottom of the chart is drawn at the node's "baseline" value (typically 0). The chart can 
 * be drawn in any supported coordinate system, including polar graphs.
 * 
 * <p>An area chart, like the bar plot, is an example of a "grouped-data" presentation node; each "band" in the
 * area chart represents one member set in the node's underlying data source collection. The super class {@link 
 * FGNPlottableData} implements support for specifying a fill color and a legend label for each such data group. It also
 * generates a distinct legend entry for each data group. <b>FGNPlottableData</b> limits the number of data groups to
 * {@link FGNPlottableData#MAX_DATAGRPS}.</p>
 * 
 * <p>As of FC 5.4.5, auto-positioned area labels are supported -- as an alternative to displaying the labels in the 
 * parent graph's legend. The area labels may be positioned inside the corresponding areas or outside; in the latter 
 * case, the labels are aligned along the left or right edge of the chart, depending on the location of the parent
 * graph's Y-axis. Styled text format is supported. When positioned inside, each label's location and font size are
 * adjusted to ensure the label fits entirely within the corresponding area. If no "fit" is found, that label will not
 * be rendered. The automated labels can be turned off, of course. See {@link #getLabelMode()}. The automated labels
 * are NOT supported for area charts in a polar graph; for these, it is best to turn the graph legend on.</p>
 * 
 * @author sruffner
 */
public class AreaChartNode extends FGNPlottableData implements Cloneable
{
   /**
    * Construct a stacked area chart initially configured to display an empty data collection. It will make no marks in 
    * the parent graph. The baseline is 0 (user units). All styling attributes are initially implicit (inherited). It 
    * has no title initially, and area labels are not rendered.
    */
   public AreaChartNode()
   {
      super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR|HASTITLEATTR);
      setTitle("");
      baseline = 0;
      labelMode = LabelMode.OFF;
   }

   //
   // Support for child nodes -- none permitted!
   //
   
   @Override public FGNodeType getNodeType() { return(FGNodeType.AREA); }


   //
   // Properties
   //

   /** The baseline for the area chart, in user units. */
   private float baseline;

   /**
    * Get the area chart baseline. 
    * @return The area chart baseline, in user units.
    */
   public float getBaseline() { return(baseline); }
    
   /**
    * Set the area chart baseline. If a change is made, {@link #onNodeModified} is invoked.
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

   /** An enumeration of the different automated label display modes supported by <b>AreaChartNode</b>. */
   public enum LabelMode
   {
      /** Area labels are not rendered. */ OFF,
      /** Each label is rendered inside its corresponding area. */ INSIDE,
      /** Each label is rendered outside its corresponding area. */ OUTSIDE;
      
      @Override public String toString() { return(super.toString().toLowerCase()); }
   }
   
   /** The current label display mode. */
   private LabelMode labelMode;
   
   /** 
    * Get the current label display mode for this area chart.
    * @return The current label mode.
    */
   public LabelMode getLabelMode() { return(labelMode); }
   
   /**
    * Set the label display mode for this area chart. If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param mode The new label mode. A null value is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setLabelMode(LabelMode mode)
   {
      if(mode == null) return(false);
      if(labelMode != mode)
      {
         if(doMultiNodeEdit(FGNProperty.MODE, mode)) return(true);
         
         LabelMode oldMode = labelMode;
         labelMode = mode;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.MODE);
            FGNRevEdit.post(this, FGNProperty.MODE, labelMode, oldMode);
         }
      }
      return(true);
   }
   

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok;
      switch(p)
      {
         case BASELINE: ok = setBaseline((Float)propValue); break;
         case MODE: ok = setLabelMode((LabelMode)propValue); break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value;
      switch(p)
      {
         case BASELINE: value = getBaseline(); break;
         case MODE: value = getLabelMode(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }


   //
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   /**
    * The only node-specific properties exported in an area chart node's style set is the include-in-legend flag and
    * the label display mode.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.LEGEND, getShowInLegend());
      styleSet.putStyle(FGNProperty.MODE, getLabelMode());
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
         labelMode = lm;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.MODE);
      
      return(changed);
   }


   // 
   // FGNPlottableData, FGNPlottable
   //
   
   /** 
    * The area chart can optionally render the label for each area. Any change in its text-related styles will affect 
    * the labels, but only if they are turned on. Similarly if a data group label is changed.
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGNProperty prop = (hint instanceof FGNProperty) ? (FGNProperty) hint : null;
      boolean forceRender = (labelMode != LabelMode.OFF) && 
            ((prop == FGNProperty.FILLC) || FGNProperty.isFontProperty(prop));
      if(hasDataGroups() && (prop == FGNProperty.DGLABEL) && (labelMode != LabelMode.OFF))
         forceRender = true;
      super.onNodeModified(forceRender ? null : hint);
   }


   /**
    * Initializes the area chart node's data set to contain random data for three data groups. The data spans the X-axis
    * range, the baseline is set at the bottom of the Y-axis range, and the Y-data is chosen randomly so that the
    * rendered chart will span much of the Y-axis range.
    */
   @Override protected void initializeDefaultData(double[] axisRng, boolean isPolar)
   {
      float x0 = (float) axisRng[0];
      float x1 = (float) axisRng[1];
      float y0 = (float) axisRng[2];
      float y1 = (float) axisRng[3];
      
      baseline = y0;
      
      float dx = (x1-x0)/20f;
      float[] params = new float[] {dx, x0};
      
      float ySpan = (y1-y0);
      
      float[] fData = new float[20*3];
      for(int i=0; i<20; i++)
      {
         fData[i*3] = y0 + ySpan* 0.3f * ((float) Math.random());
         fData[i*3+1] = y0 + ySpan * 0.3f * ((float) Math.random());
         fData[i*3+2] = y0 + ySpan * 0.3f * ((float) Math.random());
      }
      
      DataSet ds = DataSet.createDataSet(getDataSet().getID(), Fmt.MSERIES, params, 20, 3, fData);
      if(ds != null) setDataSet(ds);
   }

   @Override protected boolean recalcDataRange(Object hint)
   {
      boolean needRecalc = (hint == null || hint == FGNProperty.SRC || hint == FGNProperty.BASELINE);
      if(!needRecalc) return(false);
      
      float minX;
      float maxX;
      float minY;
      float maxY;

      DataSet set = getDataSet();
      minX = set.getXMin();
      maxX = set.getXMax();
      
      // since the area chart is "stacked", we have to add up the Y values of the individual data set, starting from the
      // baseline. You would expect all Y values to have the same sign, but we don't require it.
      minY = baseline;
      maxY = baseline;
      for(int i=0; i<set.getDataLength(); i++) if(Utilities.isWellDefined(set.getX(i, 0)))
      {
         float sum = baseline;
         for(int j=0; j<getNumDataGroups(); j++)
         {
            float y = set.getY(i, j);
            if(Utilities.isWellDefined(y)) sum += y;
         }
         if(sum < minY) minY = sum;
         else if(sum > maxY) maxY = sum;
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

   /** An area chart can render any of the standard 2D data formats. */
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
   @Override protected int getDataGroupCount() { return(getDataSet().getNumberOfSets()); }
   
   //
   // Focusable/Renderable support
   //

   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null)
      {
         rBoundsSelf = new Rectangle2D.Double();
         FGNGraph g = getParentGraph();
         FViewport2D vp = getParentViewport();
         boolean ok = (vp != null) && (g != null);
         if(ok)
         {
            SingleStringPainter labelPainter = null;
            if(labelMode != LabelMode.OFF)
            {
               labelPainter = new SingleStringPainter();
               labelPainter.setStyle(this);
               labelPainter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
            }
            MutableFillPS pStyle = new MutableFillPS(this);
            PolylinePainter painter = new PolylinePainter(pStyle, null);
            painter.setConnectionPolicy(PolylinePainter.ConnectPolicy.CLOSED);
            painter.setFilled(true);
            
            for(int i=0; i<getNumDataGroups(); i++)
            {
               // set fill color for the i-th member set.
               pStyle.setFillColor(getDataGroupColor(i));
               
               painter.setLocationProducer(new AreaVertexProducer(i));
               painter.invalidateBounds();
               Utilities.rectUnion(rBoundsSelf, painter.getBounds2D(null), rBoundsSelf);
               
               // account for area label, if labels turned on
               if(labelPainter != null)
               {
                  LabelInfo info = prepareLabel(i, g2d);
                  if(info != null)
                  { 
                     labelPainter.setTextAndLocation(info.attrLabel, info.loc);
                     labelPainter.setAlignment(info.hAlign, TextAlign.CENTERED);
                     labelPainter.updateFontRenderContext(g2d);
                     labelPainter.invalidateBounds();
                     Utilities.rectUnion(rBoundsSelf, labelPainter.getBounds2D(null), rBoundsSelf);
                  }
               }
            }
         }

         if(ok && g.getClip())
            Rectangle2D.intersect(rBoundsSelf, g.getBoundingBoxLocal(), rBoundsSelf);
      }
      
      return((Rectangle2D)rBoundsSelf.clone());
   }

   /** Releases the cached rectangle bounding any marks made by this node. */
   @Override protected void releaseRenderResourcesForSelf() { rBoundsSelf = null; }

   /** Render this area chart into the current graphics context. */
   @Override public boolean render(Graphics2D g2d, RenderTask task)
   {
      FGNGraph g = getParentGraph();
      if(g == null) return(true);

      if(needsRendering(task))
      {
         // prepare attributed string painter if area labels are drawn
         SingleStringPainter labelPainter = null;
         if(labelMode != LabelMode.OFF)
         {
            labelPainter = new SingleStringPainter();
            labelPainter.setStyle(this);
            labelPainter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
         }
         
         // construct the polyline painter that will render all bands in the area chart. We use a custom painter style 
         // with a mutable fill color so that we can set the fill color for each band in the chart.
         MutableFillPS pStyle = new MutableFillPS(this);
         PolylinePainter painter = new PolylinePainter(pStyle, null);
         painter.setConnectionPolicy(PolylinePainter.ConnectPolicy.CLOSED);
         painter.setFilled(true);
         
         for(int i=0; i<getNumDataGroups(); i++)
         {
            // set fill color for the i-th member set.
            pStyle.setFillColor(getDataGroupColor(i));
            
            painter.setLocationProducer(new AreaVertexProducer(i));
            if(!painter.render(g2d, task)) return(false);
            
            // render area label, if labels turned on
            if(labelPainter != null)
            {
               LabelInfo info = prepareLabel(i, g2d);
               if(info != null)
               { 
                  labelPainter.setTextAndLocation(info.attrLabel, info.loc);
                  labelPainter.setAlignment(info.hAlign, TextAlign.CENTERED);
               
                  if(!labelPainter.render(g2d,  task)) return(false);
               }
            }
         }
      }
      return(true);
   }

   /**
    * Cached rectangle bounding only the marks made by this bar plot node. An empty rectangle indicates that the
    * element makes no marks when "rendered". If null, the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;

   /** Helper class encsapsulates information needed to render an area label. */
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
      /** Label font size in points. Label font size may be reduced to fit inside area. */
      int fontSizePts;
   }

   /**
    * Helper method prepares the information needed to render an area label. The label's location, horizontal text
    * alignment, and (possibly) font size are calculated IAW the current label display mode. 
    * 
    * <p>Area labels are not supported when the parent graph is polar.</p>
    * 
    * <p>For the {@link LabelMode#OUTSIDE} display mode, the area labels are aligned along the left or right edge of
    * the area chart, centered vertically with respect to the corresponding area slice at that edge and offset from 
    * the edge by 0.1 inch.</p>
    * 
    * <p>For the {@link LabelMode#INSIDE} display mode, the method tries the label vertically centered within the 
    * corresponding area band at each X-coordinate value in the underlying data set. If it doesn't fit when centered
    * horizontally, leading and trailing alignment is also tried. If a fit is not found, the method will decrement the
    * font size in 2-pt increments -- down to a minimum of 6pt -- and try again. If a fit is still not found, then the
    * label will not be rendered.</p>
    * 
    * @param idx The data group index for the area. If invalid, method returns null.
    * @param g2 Graphics context -- needed to measure text in order to calculate label's render bounds. If null, 
    * method does nothing and returns null.
    * @return The rendering information for the area label. Returns null if area labels are turned off, if the parent
    * graph is polar, or if unable to determine a suitable location, etc. for the label.
    */
   private LabelInfo prepareLabel(int idx, Graphics2D g2)
   {
      if(labelMode == LabelMode.OFF || g2 == null || idx < 0 || idx >= getNumDataGroups()) return(null);
      FViewport2D vp = getParentViewport();
      FGNGraph g = getParentGraph();
      if(g == null || vp == null || g.isPolar()) return(null);
      
      LabelInfo info = new LabelInfo();
      
      GraphNode graph = (GraphNode) g;
      GraphNode.Layout layout = graph.getLayout();
      boolean onLeft = (layout == GraphNode.Layout.QUAD2 || layout == GraphNode.Layout.QUAD3) && 
            !graph.getSecondaryAxis().getHide();

      info.label = getDataGroupLabel(idx);
      info.attrLabel = fromStyledText(info.label, getFontFamily(), getFontSizeInPoints(), getFillColor(),
            getFontStyle(), true);
      info.loc = new Point2D.Double();
      info.hAlign = TextAlign.CENTERED;
      info.fontSizePts = getFontSizeInPoints();
      
      DataSet ds = getDataSet();
      boolean ok = false;
      if(labelMode == LabelMode.OUTSIDE)
      {
         info.hAlign = onLeft ? TextAlign.TRAILING : TextAlign.LEADING;
         
         double x = ds.getX(onLeft ? 0 : ds.getDataSize(idx)-1, idx);
         double y0 = getBaseline(), y1 = getBaseline();
         for(int i=0; i<=idx; i++)
         {
            double y = ds.getY(onLeft ? 0 : ds.getDataSize(idx)-1, i);
            if(Utilities.isWellDefined(y))
            {
               if(i < idx) y0 += y;
               y1 += y;
            }
         }
   
         info.loc.setLocation(x, y0);
         vp.userUnitsToThousandthInches(info.loc);
         double xUser = info.loc.getX();
         y0 = info.loc.getY();
         info.loc.setLocation(x, y1);
         vp.userUnitsToThousandthInches(info.loc);
         y1 = info.loc.getY();
         info.loc.setLocation(xUser + (onLeft ? -100 : 100), (y0 + y1)/2);
         ok = Utilities.isWellDefined(info.loc); 
      }
      else
      {
         // the area outline -- the last point delivered by the vertex producer is (NaN,NaN) -- to close the path
         GeneralPath areaOutline = new GeneralPath();
         AreaVertexProducer producer = new AreaVertexProducer(idx);
         boolean first = true;
         while(producer.hasNext())
         {
            Point2D p = producer.next();
            if(first) { areaOutline.moveTo(p.getX(), p.getY()); first = false; }
            else if(Utilities.isWellDefined(p)) areaOutline.lineTo(p.getX(), p.getY());
            else
            {
               areaOutline.closePath();
               break;
            }
         }
         
         // initialize a string painter to calculate the label's render bounds centered horizontally and vertically at
         // (0,0) using this area chart's graphic styles
         SingleStringPainter painter = new SingleStringPainter();
         painter.setStyle(this);
         painter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
         painter.setLocation(0, 0);
         painter.setText(info.attrLabel);
         Rectangle2D rectLabel = new Rectangle2D.Double();
         
         // try the label centered at each location (X, Y), first at the node's font size, and then decrease font
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
            
            for(int i=0; (!ok) && i<ds.getDataSize(-1); i++)
            {
               double x = ds.getX(i, idx);
               double y0 = getBaseline(), y1 = getBaseline();
               for(int j=0; j<=idx; j++)
               {
                  double y = ds.getY(i, j);
                  if(Utilities.isWellDefined(y))
                  {
                     if(j < idx) y0 += y;
                     y1 += y;
                  }
               }
         
               info.loc.setLocation(x, y0);
               vp.userUnitsToThousandthInches(info.loc);
               double xUser = info.loc.getX();
               y0 = info.loc.getY();
               info.loc.setLocation(x, y1);
               vp.userUnitsToThousandthInches(info.loc);
               y1 = info.loc.getY();
               info.loc.setLocation(xUser, (y0 + y1)/2);
               if(!Utilities.isWellDefined(info.loc)) continue;
               
               // if horizontally centered label doesn't fit, try leading and trailing alignment also
               rectLabel.setFrame(info.loc.getX() - rectLabel.getWidth()/2, info.loc.getY() - rectLabel.getHeight()/2, 
                     rectLabel.getWidth(), rectLabel.getHeight());
               ok = areaOutline.contains(rectLabel);
               if(!ok)
               {
                  rectLabel.setFrame(info.loc.getX() + 25, info.loc.getY() - rectLabel.getHeight()/2, 
                        rectLabel.getWidth(), rectLabel.getHeight());
                  ok = areaOutline.contains(rectLabel);
                  if(ok)
                  {
                     info.loc.setLocation(info.loc.getX() + 25, info.loc.getY());
                     info.hAlign = TextAlign.LEADING;
                  }
                  else
                  {
                     rectLabel.setFrame(info.loc.getX() - rectLabel.getWidth() - 25, 
                           info.loc.getY() - rectLabel.getHeight()/2, rectLabel.getWidth(), rectLabel.getHeight());
                     ok = areaOutline.contains(rectLabel);
                     if(ok)
                     {
                        info.loc.setLocation(info.loc.getX() - 25, info.loc.getY());
                        info.hAlign = TextAlign.TRAILING;
                     }
                  }
               }
            }
            
            fontSize -= 2;
         } while(fontSize >= 6 && !ok);
      }
      
      return(ok ? info : null);
   }

   /** 
    * This helper class provides an iterator over the vertices that define one "band" in the stacked area chart, 
    * representing one member set in the underlying data source. If there's only one such set, then there's only one 
    * band. The band defining the first member set is bound by the trace connecting the well-defined points {X,Y} in
    * that set and a line segment at the baseline value B: (X1,B) to (Xn,B). The band-defining the N-th member set is
    * bound by two traces: the summation of the member sets {1..N} and the summation of the member sets {1..N-1}. That's
    * how the "stacked" appearance is achieved.
    * 
    * @author sruffner
    */
   private class AreaVertexProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      final int setIdx;
      final DataSet set;
      final FViewport2D graphVP;
      final double baseline;
      
      boolean done;
      boolean ptReady;
      
      final int nPtsInSet;
      int nPtsSoFar;
      boolean onReturnPath;

      final Point2D pCurrent = new Point2D.Double();
      final Point2D pNext = new Point2D.Double();
      
      public Iterator<Point2D> iterator() { return(new AreaVertexProducer(setIdx)); }

      public AreaVertexProducer(int idx)
      {
         setIdx = idx;
         set = getDataSet();
         graphVP = getParentViewport();
         baseline = getBaseline();
         
         nPtsSoFar = 0;
         nPtsInSet = set.getDataLength();
         done = false;
         ptReady = false;
         onReturnPath = false;
         
         int ns = getNumDataGroups();
         if(graphVP == null || ns <= 0 || setIdx < 0 || setIdx >= ns || nPtsInSet == 0)
            done = true;
         else 
         {
            while(hasNext())
            {
               prepareNextPoint();
               if(ptReady) break;
            }
         }
      }

      public boolean hasNext()
      {
         return(graphVP != null && (ptReady || !done));
      }

      public Point2D next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         pCurrent.setLocation(pNext);
         ptReady = false;
         while(hasNext())
         {
            prepareNextPoint();
            if(ptReady) break;
         }
         
         return(pCurrent);
      }

      private void prepareNextPoint()
      {
         boolean isLast = false;
         if(!onReturnPath)
         {
            // the forward path is the trace representing the summation of the sets up to and including the current one
            double y = baseline;
            for(int i=0; i<=setIdx; i++) if(Utilities.isWellDefined(set.getY(nPtsSoFar, i)))
               y += set.getY(nPtsSoFar, i);
            pNext.setLocation(set.getX(nPtsSoFar, setIdx), y);
         }
         else if(setIdx == 0)
         {
            // the "return path" for the first set is simply the baseline
            if(nPtsSoFar == nPtsInSet)
               pNext.setLocation(set.getX(nPtsInSet-1, setIdx), baseline);
            else if(nPtsSoFar == nPtsInSet + 1)
               pNext.setLocation(set.getX(0, setIdx), baseline);
            else
            {
               pNext.setLocation(Double.NaN, Double.NaN);
               isLast = true;
            }
         }
         else
         {
            // when we're not on the first set, the "return path" is the trace representing the summation of the sets 
            // before the current one. Note that we traverse it in the opposite direction to get a polygon region.
            if(nPtsSoFar == 2*nPtsInSet)
            {
               pNext.setLocation(Double.NaN, Double.NaN);
               isLast = true;
            }
            else
            {
               double y = baseline;
               int pos = 2*nPtsInSet - (nPtsSoFar+1);
               for(int i=0; i<setIdx; i++) if(Utilities.isWellDefined(set.getY(pos, i)))
                  y += set.getY(pos, i);
               pNext.setLocation(set.getX(pos, setIdx), y);
            }
         }
         graphVP.userUnitsToThousandthInches(pNext);
         ptReady = isLast || Utilities.isWellDefined(pNext);
         
         // advance to the next point to be prepared
         ++nPtsSoFar;
         if(isLast) done = true;
         else if(nPtsSoFar == nPtsInSet && !onReturnPath) onReturnPath = true;
      }
      
      public void remove() { throw new UnsupportedOperationException("Removal not supported by this iterator."); }
   }


   //
   // PSTransformable
   //
   
   public void toPostscript(PSDoc psDoc)
   {
      FGNGraph g = getParentGraph();
      if(g == null) return;

      // if area labels are drawn, we need a Graphics2D to measure text; create one from a buffered image
      Graphics2D g2BI = null;
      if(labelMode != LabelMode.OFF)
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
         // get the vertices defining the area for the i-th data group
         vertices.clear();
         Iterator<Point2D> iterator = new AreaVertexProducer(i);
         while(iterator.hasNext()) 
            vertices.add((Point2D) iterator.next().clone());    // must clone, b/c producer reuses a Point2D
         if(vertices.isEmpty()) continue;
         
         psDoc.renderPolygons(vertices, getDataGroupColor(i));
         
         // if automated labels are turned on, render the label for this area
         LabelInfo info = prepareLabel(i, g2BI);
         if(info != null)
            psDoc.renderText(info.label, info.loc.getX(), info.loc.getY(), info.hAlign, TextAlign.CENTERED, 0,
                  info.fontSizePts * 1000.0 / 72.0);
      }
      psDoc.endElement();
   }
   
   // 
   // Object
   //

   /**
    * This override ensures that the rendering infrastructure for the clone is independent of the area chart cloned. 
    * The clone will reference the same data set, however!
    */
   @Override protected AreaChartNode clone() throws CloneNotSupportedException
   {
      AreaChartNode copy = (AreaChartNode) super.clone();
      copy.rBoundsSelf = null;
      return(copy);
   }
}
