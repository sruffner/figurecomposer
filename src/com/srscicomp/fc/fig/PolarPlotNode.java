package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.common.util.Utilities;


/**
 * <b>PolarPlotNode</b> represents a 2D graph container specialized to present data in a polar coordinate system. While
 * the original 2D graph container, {@link GraphNode}, supports polar coordinates as an option, we decided to introduce 
 * a strictly polar 2D graph that is designed specifically for polar plots and offers features not available in the
 * polar modes of {@link GraphNode}.
 * 
 * <p>Typical polar plots do not have visually distinct axes with tick marks -- like the X,Y axes of a Cartesian graph. 
 * Rather, the angular ("theta") and radial axes are represented by a polar coordinate grid, with companion labels 
 * marking the grid divisions:
 * <ul>
 * <li>The theta "grid" is a series of radial lines emanating from the origin, with text labels along the outer edge of
 * the grid reflecting the corresponding anglular values.</li>
 * <li>The "radial" grid is a series of circular arcs concentric about the origin. The arc extents depend on the 
 * displayed range of the "theta" axis (typically, the theta axis span is 90, 180 or 360 degrees, but there's no reason
 * not to allow for other angular spans). The radial arc grid labels reflect the radial value for each arc, and they may
 * be located along an invisible radial line at a specified angle within the defined range of the theta axis.</li>
 * </ul>
 * The layout of the polar grid may vary in terms of the location of zero angle, whether the angle increases in a 
 * clockwise or counterclockwise direction, and whether the radial value increases from the origin outwards or from
 * the outer circumference inward (rare usage).</p>
 * 
 * <p><b>PolarPlotNode</b> is designed to support this description of a polar plot, which is consistent with the new 
 * "polarAxes" object introduced in Matlab R2016a. The biggest advantage of this graphic node over {@link GraphNode} is
 * that it takes care of rendering the polar grid labels; if you create a polar plot with {@link GraphNode}, you must
 * insert a text label for each grid label.</p>
 * 
 * <p><b>PolarPlotNode</b> includes four component nodes that define the theta axis/grid, radial axis/grid, an 
 * associated "color bar" (pseudo Z axis), and an automated legend. it can contain any number of text labels, text 
 * boxes, images, shapes, and line segments. Plottable nodes that may be housed in the polar graph include scatter 
 * plots, data traces, rasters (for the histogram mode, yielding a rose plot), pie charts, area charts, and 
 * functions.</p>
 *
 * <p><b>PolarPlotNode</b> possesses all of the inheritable style attributes, as well as the common positioning 
 * attributes <i>x</i>, <i>y</i>, <i>width</i>, and <i>height</i>. The coordinates <i>(x,y)</i> locate, relative to a 
 * parent viewport, the bottom-left corner of the container's bounding box, while <i>width</i> and <i>height</i> define
 * its size. The polar coordinate grid will always fit inside this bounding box, centered within it if the the theta 
 * axis span is 360. The <i>title</i> holds the polar graph's (optional) title (not rendered -- only for labeling the 
 * node in GUI components). The boolean <i>clip</i> property determines whether or not plottable nodes are clipped to 
 * the bounds of the data window, and the <i>gridOnTop</i> property selects whether the grid is rendered on top of or
 * underneath plotted data. The <i>bkgColor</i> property specifies the background color for the polar grid. Typically, 
 * this will be transparent, but any RGBA color may be specified. Also note that the polar graph's axis components
 * affect exactly how the polar grid is laid out.</p>
 * 
 * @author 	sruffner
 */
public class PolarPlotNode extends FGNGraph implements Cloneable
{
   /**
    * Construct an empty <b>PolarPlotNode</b> having a 2.5-in x 2.5-in bounding box, the bottom-left corner of which
    * is located at (0.75in, 0.75in) in the graph's parent viewport. The theta axis spans 360 deg with 30-deg divisions,
    * with theta increasing CCW and 0 degrees pointing to the right. The radial axis spans the range [0..10] with 
    * 2-unit divisions. The radial grid labels are shown along an invisible line at an angle of 80 deg. All plotted
    * data are clipped and rendered on top of the polar grid; the title is an empty string.
    */
   public PolarPlotNode()
   {
      super(false, true, true);

      gridOnTop = false;
      bkgColor = new Color(0,0,0,0);
      
      // set up component nodes for the theta and radial axes, the color bar, and automated legend
      addComponentNode(new PolarAxisNode(true));
      addComponentNode(new PolarAxisNode(false));
      addComponentNode(new ColorBarNode());
      addComponentNode(new LegendNode());
   }

   /** 
    * Get the coordinates of the origin of for this polar graph. The coordinates will depend on how the theta and 
    * radial axes are defined. In particular, the start of the radial axis range need not be zero, and the axis
    * direction can be reversed (so that the end of the axis range lies at the origin!).
    * @return A point containing the origin (&theta;<sub>0</sub>, R<sub>0</sub>), with &theta;<sub>0</sub> in degrees.
    */
   public Point2D getOrigin()
   {
      double t0 = getThetaAxis().getRangeMin();
      double r0 = (getRadialAxis().getReversed()) ? getRadialAxis().getRangeMax() : getRadialAxis().getRangeMin();
      return(new Point2D.Double(t0, r0));
   }

   /**
    * Does this polar graph span a full 360-degree circle?
    * @return True if the range for the polar graph's theta axis is a complete 360 degrees.
    */
   public boolean isFullCircleGrid()
   {
      return(getThetaAxis().getRangeMax() - getThetaAxis().getRangeMin() >= 360);
   }
   
   // 
   // Required components -- these are never removed.
   //

   /**
    * Get the component node encapsulating the angular, or "theta", axis of this polar plot -- defining the angular
    * span and divisions of the polar grid.
    * 
    * @return The theta axis.
    */
   public PolarAxisNode getThetaAxis() { return((PolarAxisNode)getComponentNodeAt(0)); }

   /**
    * Get the component node encapsulating the radial axis of this polar plot, defining the radial span and divisions
    * of the polar grid.
    * 
    * @return The radial axis.
    */
   public PolarAxisNode getRadialAxis() { return((PolarAxisNode)getComponentNodeAt(1)); }

   @Override public ColorBarNode getColorBar() { return((ColorBarNode)getComponentNodeAt(2)); }
   @Override public LegendNode getLegend() { return((LegendNode)getComponentNodeAt(3)); }

   //
   // FGNGraph
   //
   
   /** The polar graph node only supports a polar coordinate system, with a linear radial axis. */
   @Override public CoordSys getCoordSys() { return(CoordSys.POLAR); }
   /** Returns false always: The polar graph node does not support the auto-scaling feature. */
   @Override boolean isAxisAutoranged(FGNGraphAxis axis) { return(false); }
   /** Returns false always: The polar graph node does not support the auto-scaling feature. */
   @Override boolean autoScaleAxes() { return(false); }
   
   //
   // Properties
   //

   /** If true, polar coordinate grid is rendered after all child nodes; else it is rendrered before. */
   private boolean gridOnTop;
   
   /** 
    * Is polar coordinate grid rendered on top of all child nodes?
    * <p>Normally, the grid is rendered first and the data on top. However, in some circumstances the rendered data
    * might obscure too much of the grid, so you can choose to draw it last. Note that the grid is always drawn before
    * the polar graph's color bar and legend.</p>
    * @return True if grid is on top of child nodes; false if it lies underneath in the "Z order".
    */
   public boolean getGridOnTop() { return(gridOnTop); }
   
   /**
    * Set polar grid on top of or below all other child nodes of this polar graph? See {@link #getGridOnTop()}.
    * @param onTop True to render grid on top of other child nodes; if false, the grid is drawn first, followed by all
    * other child nodes. Note that legend and color bar nodes are always drawn last.
    */
   public void setGridOnTop(boolean onTop)
   {
      if(gridOnTop != onTop)
      {
         if(doMultiNodeEdit(FGNProperty.GRIDONTOP, new Boolean(onTop))) return;
         
         Boolean old = new Boolean(gridOnTop);
         gridOnTop = onTop;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.GRIDONTOP);
            FGNRevEdit.post(this, FGNProperty.CLIP, new Boolean(this.gridOnTop), old, 
                  "Render polar grid " + (gridOnTop ? "on top of " : "underneath ") + "the data nodes");
         }
      }
   }
   
   /** The background color for the polar coordinate grid. */
   private Color bkgColor = null;
   
   /** 
    * Get the background color for the polar coordinate grid.
    * @return The background color. Includes alpha component.
    */
   public Color getGridBkgColor() { return(bkgColor); }
   
   /** 
    * Set the background color for the polar coordinate grid.
    * @param c The desired color. A null value is rejected. Translucent or transparent colors are permitted.
    * @return False if argument is null; else true.
    */
   public boolean setGridBkgColor(Color c)
   {
      if(c == null) return(false);
      if(!bkgColor.equals(c))
      {
         if(doMultiNodeEdit(FGNProperty.BOXC, c)) return(true);
         
         Color old = bkgColor;
         bkgColor = c;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.BOXC);
            FGNRevEdit.post(this, FGNProperty.BOXC, bkgColor, old);
         }
      }
      return(true);
   }
      
   /** Overridden to make publicly accessible, since it makes sense to rescale an entire graph. */
   @Override public void rescale(int pct, boolean fontSizeOnly) { super.rescale(pct, fontSizeOnly); }

   /**
    * For most attributes, a change in attribute value requires a complete re-rendering of the polar graph and its 
    * children. The base-class implementation suffices in such cases. However, changes in the semi-automated title's 
    * text content or position have no effect when the title is hidden, and toggling the title's visibility has no 
    * effect when the title text is empty. This override avoids any rendering overhead in these scenarios.
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;
      
      if((hint==FGNProperty.HIDE && getTitle().trim().isEmpty()) ||
            (hint==FGNProperty.TITLE && isTitleHidden()) ||
            ((hint==FGNProperty.HALIGN || hint==FGNProperty.GAP) && !isTitleRendered()))
         model.onChange(this, 0, false, null);
      else
         super.onNodeModified(hint);
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
         case GRIDONTOP : setGridOnTop(((Boolean)propValue).booleanValue()); ok = true; break;
         case BOXC : ok = setGridBkgColor((Color) propValue); break;
         default: ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
         case GRIDONTOP: value = new Boolean(getGridOnTop()); break;
         case BOXC: value = getGridBkgColor(); break;
         default: value = super.getPropertyValue(p); break;
      }
      return(value);
   }

   /** The insertion of a data presentation node may require a re-rendering of the polar graph's legend. */
   @Override protected void onSubordinateNodeInserted(FGraphicNode sub)
   {
      if(sub instanceof FGNPlottable) getLegend().onInsertOrRemovePlottableNode();
      super.onSubordinateNodeInserted(sub);
   }

   /** The insertion of a data presentation node may require a re-rendering of the polar graph's legend. */
   @Override protected void onSubordinateNodeRemoved(FGraphicNode sub)
   {
      if(sub instanceof FGNPlottable) getLegend().onInsertOrRemovePlottableNode();
      
      // NOTE: It is important that this is called AFTER updating the legend, since that update relies on cached 
      // information that will be wiped out by this call...
      super.onSubordinateNodeRemoved(sub);
   }

   /**
    * Does any (&theta;, r) data point in any of the polar graph's data presentation nodes lie OUTSIDE the polar grid
    * as defined by the ranges for the graph's &theta; and radial axes? The rendering of a data presentation node having
    * such "out of bounds" points will be partially hidden if the polar graph's clip flag is set.
    * @return True if any data points lie outside the confines of the polar grid.
    */
   public boolean isDataOutOfBounds()
   {
      // traverse plottable nodes and get the overall min/max along primary and secondary axes
      double tMin = Float.MAX_VALUE;
      double tMax = -Float.MAX_VALUE;
      double rMin =  Float.MAX_VALUE;
      double rMax = -Float.MAX_VALUE;
      for(int i=0; i<getChildCount(); i++) if(getChildAt(i) instanceof FGNPlottable)
      {
         FGNPlottable plottable = (FGNPlottable) getChildAt(i);
         float[] extrema = plottable.getDataRange();
         if(extrema[0] < tMin) tMin = extrema[0];
         if(extrema[1] > tMax) tMax = extrema[1];
         if(extrema[2] < rMin) rMin = extrema[2];
         if(extrema[3] > rMax) rMax = extrema[3];
      }
      if(rMax < rMin) rMin = rMax = getRadialAxis().getRangeMin();
      
      boolean radOOB = (rMin < getRadialAxis().getRangeMin()) || (rMax > getRadialAxis().getRangeMin());
      
      // does either min or max observed theta value fall outside graph's theta axis range? We have to be careful about
      // the comparisons here since theta value repeat every 360 deg....
      boolean thetaOOB = false;
      if(!isFullCircleGrid())
      {
         double thetaMin = getThetaAxis().getRangeMin();
         double thetaMax = getThetaAxis().getRangeMax();
         tMin = Utilities.restrictAngle(tMin, thetaMin);
         tMax = Utilities.restrictAngle(tMax, thetaMin);
         thetaOOB = (tMin < thetaMin) || (tMax > thetaMax);
      }
      return(radOOB || thetaOOB);
   }
   
   
   // 
   // Support for style sets
   //
   
   /** 
    * The node-specific properties exported in a polar graph node's style set are the "grid on top" and "clip" flags,
    * the polar grid background color.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.GRIDONTOP, new Boolean(getGridOnTop()));
      styleSet.putStyle(FGNProperty.BOXC, getGridBkgColor());
      super.putNodeSpecificStyles(styleSet);  // for styles handled by super-class
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.GRIDONTOP, getNodeType(), Boolean.class);
      if(b != null && !b.equals(restore.getCheckedStyle(FGNProperty.GRIDONTOP, null, Boolean.class)))
      {
         gridOnTop = b.booleanValue();
         changed = true;
      }
      else restore.removeStyle(FGNProperty.GRIDONTOP);
      
      Color c = (Color) applied.getCheckedStyle(FGNProperty.BOXC, getNodeType(), Color.class);
      if(c != null && !c.equals(restore.getCheckedStyle(FGNProperty.BOXC, null, Color.class)))
      {
         bkgColor = c;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.BOXC);
      
      // the super class handles some styles for all graph types
      if(super.applyNodeSpecificStyles(applied, restore))
         changed = true;
      
      // an axis, color bar or legend node can be the source node for a style set. When applying such a style set to
      // a polar graph, the styles should be applied to the corresponding component node (theta or radial axis, color
      // bar/axis, or legend). We handle these scenarios below.
      
      // this is just a check to be sure...
      if(restore.getSourceNodeType() != FGNodeType.PGRAPH || restore.getNumberComponentStyleSets() != 4) 
         return(changed);
      
      FGNodeType srcNT = applied.getSourceNodeType();
      if(srcNT == FGNodeType.LEGEND)
      {
         if(getLegend().applyTextDrawStyles(applied, restore.getComponentStyleSet(3))) changed = true;
         if(getLegend().applyNodeSpecificStyles(applied, restore.getComponentStyleSet(3))) changed = true;
      }
      else if(srcNT == FGNodeType.CBAR)
      {
         if(getColorBar().applyTextDrawStyles(applied, restore.getComponentStyleSet(2))) changed = true;
         if(getColorBar().applyNodeSpecificStyles(applied, restore.getComponentStyleSet(2))) changed = true;
      }
      else if(srcNT == FGNodeType.PAXIS)
      {
         for(int i=0; i<2; i++)
         {
            FGraphicNode cmpt = getComponentNodeAt(i);
            FGNStyleSet cmptSS = restore.getComponentStyleSet(i);
            if(cmpt.applyTextDrawStyles(applied, cmptSS)) changed = true;
            if(cmpt.applyNodeSpecificStyles(applied, cmptSS)) changed = true;
         }
      }
      return(changed);
   }

   
   //
   // Support for child nodes
   //

   @Override public boolean canInsert(FGNodeType nodeType)
   {
      return(nodeType == FGNodeType.TRACE || nodeType == FGNodeType.SCATTER || nodeType == FGNodeType.PIE || 
            nodeType == FGNodeType.AREA || nodeType == FGNodeType.BAR || nodeType == FGNodeType.FUNCTION || 
            nodeType == FGNodeType.RASTER || nodeType == FGNodeType.LABEL || nodeType == FGNodeType.TEXTBOX || 
            nodeType == FGNodeType.LINE || nodeType == FGNodeType.SHAPE || nodeType == FGNodeType.IMAGE);
   }

   @Override public FGNodeType getNodeType() { return(FGNodeType.PGRAPH); }

   
   
   //
   // Focusable/Renderable support
   //

   /**
    * The local rendering coordinate system of a polar graph is defined by its location <i>(x,y)</i> and size <i>(width,
    * height)</i>, properties that are specifed with respect to the parent viewport. The origin in the local coordinate 
    * system  (NOT the graph's polar coordinate system!) lies at <i>(x, y)</i>. Unlike the more general {@link 
    * GraphNode}, this polar graph cannot be rotated WRT its parent. 
    * 
    * <p>This method accounts for the aforementioned properties when calculating the local-to-parent transform. If the 
    * transform is ill-defined for whatever reason, the identity transform is returned.</p>
    */
   @Override public AffineTransform getLocalToParentTransform()
   {
      AffineTransform identity = new AffineTransform();

      // get graph's bounding rectangle with respect to the parent viewport
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(identity);
      Rectangle2D rect = parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      if(rect == null) return(identity);

      // translate origin so that it is in coord system of parent viewport
      AffineTransform at = AffineTransform.getTranslateInstance(rect.getX(), rect.getY());

      return(at);
   }

   /**
    * The polar graph's 2D viewport is defined by its bounding box (x,y, width*height), which is defined WRT its parent 
    * container. In addition, the graph's theta and radial axes define a polar coordinate system that is mapped onto
    * the standard right-handed viewport coordinate system. Coordinates specified in real units (in,cm,pt,mm) or 
    * percentages are mapped to physical viewport units in a straightforward way based on the physical dimensions of the
    * viewport. User coordinates, however, are transformed in a non-trivial way IAW the current definitions of the
    * graph's theta and radial axes.
    *  
    * <p>If the theta axis range is the full 360 deg, then the polar origin is at the center of the bounding box and
    * the grid diameter will be the smaller bounding box dimension (if not square). If the theta axis range is less
    * than 360, then the origin of the polar grid and its size are adjusted to fill as much of the bounding box as
    * possible. The grid will always appear circular (1:1 aspect ratio), not ellliptical.</p>
    */
   @Override public FViewport2D getViewport()
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(null);
      Rectangle2D rect = parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      if(rect == null) return(null);
      double w = rect.getWidth();
      double h = rect.getHeight();
      if(w <= 0 || h <= 0) return(null);
      
      if(!Utilities.isWellDefined(polarOrigin)) updateGridLayout(w, h);
      
      boolean revTheta = getThetaAxis().getReversed();
      double radRange = getRadialAxis().getRangeMax() - getRadialAxis().getRangeMin();
      boolean revRad = getRadialAxis().getReversed();
      double radiusAtOrigin = revRad ? getRadialAxis().getRangeMax() : getRadialAxis().getRangeMin();
      
      return(new FViewport2D(w, h, polarOrigin, polarRadius/radRange, radiusAtOrigin, 
            getThetaAxis().getReferenceAngle(), revTheta, revRad));
   }

   /**
    * The only rendering done directly by the polar graph node itself is to optionally fill the background of the polar
    * coordinate grid, which always fits inside the bounding box rectangle. This method always returns that bounding 
    * box rectangle, since we want this to be the minimum rectangle for focus shape and hit test calculations.
    * 
    * <p>In addition, if necessary, it recalculates the location of the polar origin WRT the BL corner of the bounding
    * box and the polar grid radius such that the grid fills as much of the bounding box as possible and is otherwise
    * centered within the box.</p>
    */
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || !Utilities.isWellDefined(polarOrigin))
      {
         FViewport2D parentVP = getParentViewport();
         Rectangle2D rect = (parentVP==null) ? null : parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
         if(rect != null) updateGridLayout(rect.getWidth(), rect.getHeight());
      }
      
      return(super.getRenderBoundsForSelf(g2d, forceRecalc));
   }

   /** Origin of polar coordinate grid in milli-inches WRT BL corner of node's defined bounding box. */
   private Point2D polarOrigin = new Point2D.Double(Double.NaN, Double.NaN);
   /** Radius of polar grid in milli-inches. */
   private double polarRadius = 0;
   
   /** 
    * Given the polar graph's current definition, find the origin of the polar coordinate grid in milli-inches WRT
    * the bottom-left corner of this node's defined bounding box B and the grid radius in milli-inches such that the
    * grid fills as much of B as possible. When the grid is a full circle (theta spans 360 deg), the origin lies at
    * the center of B and the radius is half of the smaller dimension of B. Otherwise, the calculations are rather
    * complicated. <i>This method must be invoked whenever the theta axis range, orientation angle, or direction
    * changes, or whenever the diemnsions of B change.</i>
    * 
    * @param wMI Width of polar graph's bounding box, in milli-inches.
    * @param hMI Height of polar graph's bounding box, in milli-inches.
    */
   private void updateGridLayout(double wMI, double hMI)
   {
      if(wMI <= 0 || hMI <= 0) return;
      
      // the simple case: polar coordinate grid spans a full circle
      if(isFullCircleGrid())
      {
         polarOrigin.setLocation(wMI/2.0, hMI/2.0);
         polarRadius = 0.5 * Math.min(wMI, hMI);
         return;
      }
      
      // angles -- measured CCW (CW is neg) in the standard right-handed coordinate system (not the graph's polar 
      // coordinate system) -- at which the the theta axis starts, ends
      boolean isCW = getThetaAxis().getReversed();
      double thetaStart = getThetaAxis().getReferenceAngle() + (isCW ? -1 : 1) * getThetaAxis().getRangeMin();
      double thetaSpan = getThetaAxis().getRangeMax() - getThetaAxis().getRangeMin();
      double thetaEnd = thetaStart + (isCW ? -1 : 1) * thetaSpan;

      // for now, assume a unit radius and find all the points touching the tightest rectangle bounding the pie
      // segment that represents the polar grid when the angular span is less than 360 deg. One or both arc endpoints 
      // could be touch points, as can the polar origin. In addition, touch points include any tangent point at angle 
      // 0, 90, 180 and 270 that is part of the "arc". We find the touch point coordinates WRT the polar origin and a 
      // radius of 1.
      List<Point2D> touchPoints = new ArrayList<Point2D>();
      touchPoints.add(new Point2D.Double());
      touchPoints.add(new Point2D.Double(Math.cos(thetaStart*Math.PI/180.0), Math.sin(thetaStart*Math.PI/180.0)));
      touchPoints.add(new Point2D.Double(Math.cos(thetaEnd*Math.PI/180.0), Math.sin(thetaEnd*Math.PI/180.0)));
      
      // we restrict arc endpoints to [0..360) so that we can figure out whether or not 0, 90, 180 and 270 is part of
      // the arc in both the CW and CCW cases...
      double s = Utilities.restrictAngle(thetaStart);
      double e = Utilities.restrictAngle(thetaEnd);
      if(!isCW)
      {
         if(s > e) touchPoints.add(new Point2D.Double(1,0));
         if((s < 90 && (e > 90 || e < s)) || (s > 90 && e > 90 && e < s)) touchPoints.add(new Point2D.Double(0,1));
         if((s < 180 && (e > 180 || e < s)) || (s > 180 && e > 180 && e < s)) touchPoints.add(new Point2D.Double(-1,0));
         if((s < 270 && (e > 270 || e < s)) || (s > 270 && e > 270 && e < s)) touchPoints.add(new Point2D.Double(0,-1));
      }
      else
      {
         if(s < e) touchPoints.add(new Point2D.Double(1,0));
         if((s > 90 && (e < 90 || e > s)) || (s < 90 && e < 90 && e > s)) touchPoints.add(new Point2D.Double(0,1));
         if((s > 180 && (e < 180 || e > s)) || (s < 180 && e < 180 && e > s)) touchPoints.add(new Point2D.Double(-1,0));
         if((s > 270 && (e < 270 || e > s)) || (s < 270 && e < 270 && e > s)) touchPoints.add(new Point2D.Double(0,-1));
      }
      
      // width of box bounding the polar grid is the difference between the maximum and minimum X-coordinates among the
      // touch points. Analogously for the height of the box.
      double xMax = -1, xMin = 1, yMax = -1, yMin = 1;
      for(Point2D p : touchPoints)
      {
         if(p.getX() < xMin) xMin = p.getX();
         if(p.getX() > xMax) xMax = p.getX();
         if(p.getY() < yMin) yMin = p.getY();
         if(p.getY() > yMax) yMax = p.getY();
      }
      double wBox = xMax - xMin;
      double hBox = yMax - yMin;
      
      // to find maximum radius such that box R tightly bounding polar grid will fit in the node's defined bounding box
      // B, we have to their compare aspect ratios. Here we also find the dimensions of R in milli-inches.
      if(wMI/hMI <= wBox/hBox)
      {
         polarRadius = wMI/wBox;
         wBox = wMI;
         hBox *= polarRadius;
      }
      else
      {
         polarRadius = hMI/hBox;
         hBox = hMI;
         wBox *= polarRadius;
      }
      
      // finally, we get the coordinates of the polar origin WRT the BL corner of the node's bounding box B.
      polarOrigin.setLocation((wMI-wBox)/2.0 - polarRadius*xMin, (hMI-hBox)/2.0 - polarRadius*yMin);
   }

   /**
    * The polar graph container itself does not render anything other than the background color of the polar coordinate
    * grid; all other rendering is handled by its subordinate components. The container does control whether or not the
    * grid is drawn on top of or underneath any other child nodes, and whether or not any plottable data is clipped to
    * the polar coordinate grid shape (which may not be a full circle!). Note that component nodes (axes, legend) and
    * any non-data child nodes are rendered without clipping.
    */
   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(!(isRendered() && needsRendering(task)))
         return(true);

      // get polar graph's bounding rectangle with respect to the parent viewport; if it is ill-defined, render nothing.
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(true);
      Rectangle2D rBox = parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      if(rBox == null || rBox.getWidth() <= 0 || rBox.getHeight() <= 0)
         return(true);

      // render polar graph in a copy of the graphics context, so we do not alter the original. If clipping is on, we 
      // set the clip shape ONLY when a plottable node is to be rendered. Depending on the order of the children, we may
      // have to set the clip shape then restore the old clip several times.
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      Shape clipShape = getClip() ? getDataClipShape() : null;
      boolean isClipped = false;  
      try
      {
         // transform graphics context to the graph's viewport.
         g2dCopy.translate(rBox.getX(), rBox.getY());
         Shape clipOrig = g2dCopy.getClip();

         // if the polar grid background is not transparent, fill it first. The clip shape is what we need to fill.
         if(bkgColor.getAlpha() != 0)
         {
            g2dCopy.setColor(bkgColor);
            g2dCopy.fill(clipShape != null ? clipShape : getDataClipShape());
         }
         
         // render the polar coordinate grid first, unless it is on top of the data
         if(!gridOnTop)
         { 
            if(!getThetaAxis().render(g2dCopy,  task)) return(false);
            if(!getRadialAxis().render(g2dCopy, task)) return(false);
         }

         // render all other children with or without the clip shape installed -- as appropriate
         for(int i=0; i<getChildCount(); i++)
         {
            FGraphicNode n = getChildAt(i);
            if(getClip() && (n instanceof FGNPlottable))
            {
               if(!isClipped)
               {
                  g2dCopy.clip(clipShape);
                  isClipped = true;
               }
            }
            else if(isClipped)  
            {
               g2dCopy.setClip(clipOrig);
               isClipped = false;
            }
               
            if(!n.render(g2dCopy, task))
               return(false);
         }
         
         // render the color bar and legend, the axes if the grid is on top of the data, and the semi-automated
         // title -- all unclipped
         if(isClipped)
         {
            g2dCopy.setClip(clipOrig);
            isClipped = false;
         }
         if(gridOnTop)
         { 
            if(!getThetaAxis().render(g2dCopy,  task)) return(false);
            if(!getRadialAxis().render(g2dCopy, task)) return(false);
         }
         if(!getColorBar().render(g2dCopy, task)) return(false);
         if(!getLegend().render(g2dCopy, task)) return(false);
         if(!renderAutoTitle(g2dCopy, task)) return(false);
      }
      finally 
      { 
         if(g2dCopy != null) g2dCopy.dispose(); 
      }
      return(true);
   }

   /**
    * Helper method calculates the current data clipping shape for this polar graph node IAW with the current layout
    * of the polar coordinate grid, as defined by the theta and radial axis components.
    * 
    * @return The clip shape, in the local rendering coordinates of this polar graph node.
    */
   private Shape getDataClipShape()
   {
      // the bounding box rect in local coordinates. If it is empty, just return it (nothing is rendered anyway).
      Rectangle2D rBox = getBoundingBoxLocal();
      if(rBox.isEmpty()) return(rBox);

      // if the polar grid is full-circle, the clip shape is that circle with the origin in the center of the bounding
      // box and a diameter = the smaller box dimension.
      Shape clip = null;
      double w = rBox.getWidth();
      double h = rBox.getHeight();
      double minDim = Math.min(w, h);
      if(isFullCircleGrid())
         clip = new Ellipse2D.Double(w/2 - minDim/2, h/2 - minDim/2, minDim, minDim);
      else
      {
         // angles -- measured CCW (CW is neg) in the standard right-handed coordinate system (not the graph's polar 
         // coordinate system) -- at which the the theta axis starts, ends
         boolean isCW = getThetaAxis().getReversed();
         double thetaStart = getThetaAxis().getReferenceAngle() + (isCW ? -1 : 1) * getThetaAxis().getRangeMin();
         double thetaSpan = getThetaAxis().getRangeMax() - getThetaAxis().getRangeMin();

         // Java graphics use a left-handed coordinate system, so CCW is negative. Hence the sign flips.
         Arc2D arcSeg = new Arc2D.Double();
         arcSeg.setArcByCenter(polarOrigin.getX(), polarOrigin.getY(), polarRadius, 
               -thetaStart, (isCW ? 1 : -1) * thetaSpan, Arc2D.PIE);
         clip = arcSeg;
      }
      
      return(clip);
   }
   
   
   //
   // PSTransformable implementation
   //
   
   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      // get parent viewport; if there is none, then the polar graph cannot be rendered
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      // get the bounding rectangle with respect to the parent viewport
      Rectangle2D rect = parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      if(rect == null) return;
      Point2D botLeft = new Point2D.Double(rect.getX(), rect.getY());

      // start the element and establish a new viewport with origin at node's bottom left corner. We want a 
      // NON-CLIPPING viewport, so we don't need to specify W,H of bounding box
      psDoc.startElement(this);
      psDoc.setViewport(botLeft, 0, 0, 0, false);
      Point2D localOrigin = new Point2D.Double();
      
      // start angle, extent and direction for the circular arc defining the polar clip shape
      boolean isCW = getThetaAxis().getReversed();
      double thetaStart = getThetaAxis().getReferenceAngle() + (isCW ? -1 : 1) * getThetaAxis().getRangeMin();
      double thetaSpan = getThetaAxis().getRangeMax() - getThetaAxis().getRangeMin();
      
      // if the polar grid background is not transparent, fill it first.
      if(bkgColor.getAlpha() != 0)
      {
         BkgFill bkgFill = BkgFill.createSolidFill(bkgColor);
         
         // we have to set a clipping viewport with a polar shape first. So we have to save and restore the graphics 
         // state in that scenario.
         psDoc.saveGraphicsState();
         psDoc.setViewport(localOrigin, rect.getWidth(), rect.getHeight(), 0, true);
         psDoc.polarClip(polarOrigin, polarRadius, thetaStart, thetaSpan, isCW);
         psDoc.renderRect(rect.getWidth(), rect.getHeight(), 0, false, bkgFill);
         psDoc.restoreGraphicsState();
      }

      // render polar coordinate grid next -- unless the grid is to be drawn on top of data.
      if(!gridOnTop)
      {
         getThetaAxis().toPostscript(psDoc);
         getRadialAxis().toPostscript(psDoc);
      }

      // render all other descendants. Whenever we have to switch from the non-clipped to the clipped viewport, we 
      // save the graphics state, then restore it when returning to the non-clipped viewport. This is because the 
      // user could easily intersperse clipped and non-clipped children.
      boolean isClipped = false;
      for(int i=0; i<getChildCount(); i++)
      {
         FGraphicNode child = getChildAt(i);
         boolean needClip = getClip() && (child instanceof FGNPlottable);
         if(needClip != isClipped)
         {
            if(!isClipped)
            {
               psDoc.saveGraphicsState();
               psDoc.setViewport(new Point2D.Double(0,0), rect.getWidth(), rect.getHeight(), 0, true);
               psDoc.polarClip(polarOrigin, polarRadius, thetaStart, thetaSpan, isCW);
               isClipped = true;
            }
            else
            {
               psDoc.restoreGraphicsState();
               isClipped = false;
            }
         }
         child.toPostscript(psDoc);
      }

      // must undo last switch to a clipped viewport, if necessary.
      if(isClipped) psDoc.restoreGraphicsState();

      // render the grid if it is on top of the data, the color bar, legend, and semi-automated title last. These are
      // never clipped.
      if(gridOnTop)
      {
         getThetaAxis().toPostscript(psDoc);
         getRadialAxis().toPostscript(psDoc);
      }
      getColorBar().toPostscript(psDoc);
      getLegend().toPostscript(psDoc);
      autoTitleToPostscript(psDoc);
      
      // end the element
      psDoc.endElement();
   }
   
   
   /** 
    * This override ensures that the cloned polar graph node's calculate polar origin is independent of this node's
    * origin. 
    */
   @Override protected Object clone()
   {
      PolarPlotNode copy = (PolarPlotNode) super.clone();
      copy.polarOrigin = new Point2D.Double(Double.NaN, Double.NaN);
      return(copy);
   }
}
