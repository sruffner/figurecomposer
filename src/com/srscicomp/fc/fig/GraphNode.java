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
 * <code>GraphNode</code> encapsulates an entire 2D plot in the <i>DataNav</i> graphic model. It is the most 
 * important and the most complex graphic node in the schema. It includes six <i>component</i> nodes that define the 
 * graph's axes, grid lines, and automated legend, and it can contain any number of data traces, line segments, shapes, 
 * text labels, text boxes, calibration bars, and even other graphs. 
 * 
 * <p><code>GraphNode</code> possesses all of the inheritable style attributes, as well as the common positioning 
 * attributes <i>x</i>, <i>y</i>, <i>width</i>, <i>height</i>, and <i>rotate</i>. The coordinates <i>(x,y)</i> locate, 
 * relative to a parent viewport, the bottom-left corner of the graph's data window (the axes are typically rendered 
 * outside this window), while <i>rotate</i> specifies a rotation about that point. <i>Width</i> and <i>height</i> 
 * specify the size of the data window, again relative to the parent viewport. The <i>title</i> holds the graph's 
 * (optional) title (currently, this title is only for labeling the node in GUI components -- it is not rendered). The 
 * boolean <i>clip</i> property determines whether or not function and data traces are clipped to the bounds of the data 
 * window. The <i>boxColor</i> property specifies the background color for the graph's data box. Typically, this will
 * be transparent black, but any RGBA color may be specified. Finally, three enumerated properties specify the graph's 
 * coordinate system type (Cartesian XY, semilogX, semilogY, loglog, polar, and polar with a logarithmic radial axis), 
 * its quadrant layout (1st, 2nd, 3rd, 4th, or all 4 quadrants), and the auto-range enable state for each of its 3 axes 
 * (X, Y, and Z/color axis).</p>
 * 
 * @author 	sruffner
 */
public class GraphNode extends FGNGraph implements Cloneable
{
   /**
    * Construct an empty <code>GraphNode</code> having a 2.5-in x 2.5-in data box, the bottom-left corner of which is 
    * located at (0.75in, 0.75in) in the graph's parent viewport. The graph is configured to use a standard Cartesian 
    * coordinate system and a first quadrant axis layout. It has no title and does not clip data to its data box. The
    * data box background is fully transparent. All axes are both set up to span the range [0..100], each with a 
    * user-defined default set of tick marks and a generic axis label. The color bar, the automated legend, and the 
    * gridlines are initially hidden. Automatic range adjustment is disabled for all axes.
    */
   public GraphNode()
   {
      super(true, true, false);

      coordSys = CoordSys.CARTESIAN;
      layout = Layout.QUAD1;
      autorangeAxes = Autorange.NONE;
      boxColor = new Color(0,0,0,0);
      
      // set up component nodes for the axes, corresponding gridlines, and automated legend
      addComponentNode(new AxisNode(true));
      addComponentNode(new AxisNode(false));
      addComponentNode(new ColorBarNode());
      addComponentNode(new GridLineNode(true));
      addComponentNode(new GridLineNode(false));
      addComponentNode(new LegendNode());
   }


   // 
   // Required components of a GraphNode -- these are never removed.
   //

   /**
    * Get the component node encapsulating the primary axis of this graph. In Cartesian plots, this will be the x-axis;
    * in polar plots, the theta axis.
    * 
    * @return The graph's primary axis.
    */
   public AxisNode getPrimaryAxis() { return((AxisNode)getComponentNodeAt(0)); }

   /**
    * Get the component node encapsulating the secondary axis of this graph. In Cartesian plots, this will be the
    * y-axis; in polar plots, the radial axis.
    * 
    * @return The graph's primary axis.
    */
   public AxisNode getSecondaryAxis() { return((AxisNode)getComponentNodeAt(1)); }

   /**
    * Get the component node encapsulating the primary axis grid lines for this graph. In Cartesian plots these are
    * parallel to the x-axis and defined by the major tick marks along the y-axis. In polar plots they are circular 
    * arcs, the radii of which are defined by the major tick marks along the radial axis.
    * 
    * @return The graph's primary axis grid lines.
    */
   public GridLineNode getPrimaryGridLines() { return((GridLineNode)getComponentNodeAt(3)); }

   /**
    * Get the component node encapsulating the secondary axis grid lines for this graph. In Cartesian plots these are 
    * parallel to the y-axis and defined by the major tick marks along the x-axis. In polar plots they are radial 
    * lines, the angles of which are defined by the major tick marks for the theta axis.
    * 
    * @return The graph's seconday axis grid lines.
    */
   public GridLineNode getSecondaryGridLines() { return((GridLineNode)getComponentNodeAt(4)); }

   @Override public ColorBarNode getColorBar() { return((ColorBarNode)getComponentNodeAt(2)); }
   @Override public LegendNode getLegend() { return((LegendNode)getComponentNodeAt(5)); }


   //
   // Properties
   //

   /**
    * An enumeration of the different quadrant layouts supported by <code>GraphNode</code>. The layout determines 
    * where the axes are positioned relative to the graph's data box.
    */
   public enum Layout
   {
      /** First quadrant layout. H axis lies below and V axis lies left of the graph's data box. */
      QUAD1("quad1"),
      
      /** Second quadrant layout. H axis lies below and V axis lies right of the graph's data box. */
      QUAD2("quad2"), 
      
      /** Third quadrant layout. H axis lies above and V axis lies right of the graph's data box. */
      QUAD3("quad3"), 
      
      /** Fourth quadrant layout. H axis lies above and V axis lies left of the graph's data box. */
      QUAD4("quad4"),
      
      /** Full 4-quadrant layout, with origin at the center of graph's data box. Mostly used with polar plots. */
      ALLQUAD("allQuad");
      
      private String tag;
      
      Layout(String tag) { this.tag = tag; }
      
      @Override
      public String toString() { return(tag); }
   }

   /**
    * The graph node's "auto-range" property: an enumeration of all the different combinations for enabling/disabling
    * the auto-range feature for the x, y, and z (color) axis independently.
    * 
    * <p>[NOTE: This feature is primarily intended for when a figure is used as a view template for a <i>DataNav</i>
    * hub navigation view. When new data is "injected" into a graph in such a view, it may be useful to automatically
    * adjust the range of the axes to ensure that all of the data is viewable in the graph's data window.]</p>
    */
   public enum Autorange
   {
      NONE("none"), X("x"), Y("y"), Z("z"), XY("xy"), XZ("xz"), YZ("yz"), XYZ("xyz");
      
      Autorange(String tag) { this.tag = tag; }
      @Override public String toString() { return(tag); }
      
      /** 
       * Is auto-ranging enabled for the graph node's X-axis?
       * @return True if X-axis is auto-ranged.
       */
      public boolean isXAxisAutoranged() { return(tag.contains("x")); }
      /** 
       * Is auto-ranging enabled for the graph node's Y-axis?
       * @return True if Y-axis is auto-ranged.
       */
      public boolean isYAxisAutoranged() { return(tag.contains("y")); }
      /** 
       * Is auto-ranging enabled for the graph node's Z(color)-axis?
       * @return True if Z-axis is auto-ranged.
       */
      public boolean isZAxisAutoranged() { return(tag.contains("z")); }
      
      private String tag;
   }

   
   /**
    * The graph's coordinate system type.
    */
   private CoordSys coordSys;

   @Override public CoordSys getCoordSys() { return(coordSys); }

   /**
    * Set the type of coordinate system in which data traces are rendered within this <code>GraphNode</code>. If a 
    * change is made, the method <code>onNodeModified()</code> is invoked.
    * 
    * <p>When the coordinate system is switched from a non-polar to a polar type, the author will almost always want to 
    * hide the primary and secondary axes and change to a full-quadrant layout. Conversely, when switching back to one 
    * of the non-polar types, the author will typically show both axes and use the "quad1" layout. This method performs
    * all of these changes as a single, reversible operation.</p>
    * 
    * @param coordSys The new coordinate system type. A null value is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setCoordSys(CoordSys coordSys)
   {
      if(coordSys == null) return(false);
      if(this.coordSys != coordSys)
      {
         if(doMultiNodeEdit(FGNProperty.TYPE, coordSys)) return(true);
         
         boolean wasPolar = isPolar();
         CoordSys oldSys = this.coordSys;
         this.coordSys = coordSys;

         MultiRevEdit undoer = null;
         FGraphicModel fgm = getGraphicModel();
         if(isPolar() != wasPolar && fgm != null)
         {
            undoer = new MultiRevEdit(fgm, "Change graph type from " + oldSys + " to " + this.coordSys);
            undoer.addPropertyChange(this, FGNProperty.TYPE, this.coordSys, oldSys);
            
            if(wasPolar)
            {
               if(layout == Layout.ALLQUAD)
               {
                  undoer.addPropertyChange(this, FGNProperty.LAYOUT, Layout.QUAD1, layout);
                  layout = Layout.QUAD1;
               }
               if(getPrimaryAxis().getHide())
               {
                  undoer.addPropertyChange(getPrimaryAxis(), FGNProperty.HIDE, Boolean.FALSE, Boolean.TRUE);
                  getPrimaryAxis().setHideNoNotify(false);
               }
               if(getSecondaryAxis().getHide())
               {
                  undoer.addPropertyChange(getSecondaryAxis(), FGNProperty.HIDE, Boolean.FALSE, Boolean.TRUE);
                  getSecondaryAxis().setHideNoNotify(false);
               }
               if(!getPrimaryGridLines().getHide())
               {
                  undoer.addPropertyChange(getPrimaryGridLines(), FGNProperty.HIDE, Boolean.TRUE, Boolean.FALSE);
                  getPrimaryGridLines().setHideNoNotify(true);
               }
               if(!getSecondaryGridLines().getHide())
               {
                  undoer.addPropertyChange(getSecondaryGridLines(), FGNProperty.HIDE, Boolean.TRUE, Boolean.FALSE);
                  getSecondaryGridLines().setHideNoNotify(true);
               }
            }
            else
            {
               if(layout != Layout.ALLQUAD)
               {
                  undoer.addPropertyChange(this, FGNProperty.LAYOUT, Layout.ALLQUAD, layout);
                  layout = Layout.ALLQUAD;
               }
               
               AxisNode axis = getPrimaryAxis();
               axis.setNotificationsEnabled(false);
               try
               {
                  if(!axis.getHide())
                  {
                     undoer.addPropertyChange(axis, FGNProperty.HIDE, Boolean.TRUE, Boolean.FALSE);
                     axis.setHide(true);
                  }
                  if(axis.getStart() != 0)
                  {
                     undoer.addPropertyChange(axis, FGNProperty.START, new Double(0), new Double(axis.getStart()));
                     axis.setStart(0);
                  }
                  if(axis.getEnd() != 360)
                  {
                     undoer.addPropertyChange(axis, FGNProperty.END, new Double(360), new Double(axis.getEnd()));
                     axis.setEnd(360);
                  }
               }
               finally
               {
                  axis.setNotificationsEnabled(true);
               }
               
               TickSetNode majorTicks = axis.getMajorTickSet();
               if(majorTicks != null)
               {
                  majorTicks.setNotificationsEnabled(false);
                  try
                  {
                     if(!majorTicks.isTrackingParentAxis()) majorTicks.setTrackingParentAxis(true);
                     if(majorTicks.getInterval() != 45)
                     {
                        Double oldIntv = new Double(majorTicks.getInterval());
                        undoer.addPropertyChange(majorTicks, FGNProperty.INTV, new Double(45), oldIntv);
                        majorTicks.setInterval(45);
                     }
                  }
                  finally
                  {
                     majorTicks.setNotificationsEnabled(true);
                  }
               }
               
               if(!getSecondaryAxis().getHide())
               {
                  undoer.addPropertyChange(getSecondaryAxis(), FGNProperty.HIDE, Boolean.TRUE, Boolean.FALSE);
                  getSecondaryAxis().setHideNoNotify(true);
               }
               if(getPrimaryGridLines().getHide())
               {
                  undoer.addPropertyChange(getPrimaryGridLines(), FGNProperty.HIDE, Boolean.FALSE, Boolean.TRUE);
                  getPrimaryGridLines().setHideNoNotify(false);
               }
               if(getSecondaryGridLines().getHide())
               {
                  undoer.addPropertyChange(getSecondaryGridLines(), FGNProperty.HIDE, Boolean.FALSE, Boolean.TRUE);
                  getSecondaryGridLines().setHideNoNotify(false);
               }
            }
         }
         
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.TYPE);
            if(undoer != null) fgm.postReversibleEdit(undoer);
            else FGNRevEdit.post(this, FGNProperty.TYPE, this.coordSys, oldSys);
         }
      }
      return(true);
   }

   /**
    * The graph's quadrant layout. This determines positioning of the X,Y axes relative to the data box.
    */
   private Layout layout;

   /**
    * Get the current quadrant layout for this <code>GraphNode</code>. The quadrant layout determines the location of 
    * the horizontal and vertical axes when the data coordinate system is a non-polar type.
    * 
    * @return The current quadrant layout.
    */
   public Layout getLayout() { return(layout); }

   /**
    * Set the quadrant layout for this <code>GraphNode</code>. If a change is made, the method 
    * <code>onNodeModified()</code> is invoked.
    * 
    * @param layout The new quadrant layout. A <code>null</code> value is rejected.
    * @return <code>False</code> if argument was <code>null</code>; <code>true</code> otherwise.
    */
   public boolean setLayout(Layout layout)
   {
      if(layout == null) return(false);
      if(this.layout != layout)
      {
         if(doMultiNodeEdit(FGNProperty.LAYOUT, layout)) return(true);
         
         Layout old = this.layout;
         this.layout = layout;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.LAYOUT);
            FGNRevEdit.post(this, FGNProperty.LAYOUT, this.layout, old);
         }
      }
      return(true);
   }

   /** The auto-range enable state for EACH of the graph's axes, maintained as a single property. */
   private Autorange autorangeAxes;

   /**
    * Get the auto-range enable state for this graph's three axes. If an axis is thus enabled, its range is recomputed
    * and set each time a change in the graph or its content (like adding or removing a data presentation node or 
    * changing its data source) would necessitate such a recalculation.
    * @return An enumerate indicating which of the three axes -- X, Y, Z(color) -- are currently auto-ranged.
    */
   public Autorange getAutorangeAxes() { return(autorangeAxes); }

   /**
    * Set or clear the auto-range enable states for this graph's three axes. If a change is made, the method {@link 
    * #onNodeModified(Object)} is invoked.
    * 
    * @param state An enumerate indicating the new auto-range enable state for each axis.
    */
   public void setAutorangeAxes(Autorange state)
   {
      if(state != null && !autorangeAxes.equals(state))
      {
         if(doMultiNodeEdit(FGNProperty.AUTORANGE, state)) return;
         
         Autorange old = this.autorangeAxes;
         this.autorangeAxes = state;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.AUTORANGE);
            FGNRevEdit.post(this, FGNProperty.AUTORANGE, autorangeAxes, old, 
                  "Set graph auto-range state to: " + autorangeAxes.toString());
         }
      }
   }

   /**
    * Set or clear the auto-range enables states for this graph's three axes. If any change is made, the method {@link 
    * #onNodeModified(Object)} is invoked.
    * @param enaX True if auto-ranging should be enabled for X axis.
    * @param enaY True if auto-ranging should be enabled for Y axis.
    * @param enaZ True if auto-ranging should enabled for Z axis.
    */
   public void setAutorangeAxes(boolean enaX, boolean enaY, boolean enaZ)
   {
      Autorange updated = Autorange.NONE;
      if(enaX || enaY || enaZ)
      {
         String tagVal = (enaX ? "x":"") + (enaY ? "y":"") + (enaZ ? "z":"");
         for(Autorange ar : Autorange.values()) if(ar.tag.equals(tagVal))
         {
            updated = ar;
            break;
         }
      }
      setAutorangeAxes(updated);
   }

   /** The background color for the graph's data box (default is transparent black). */
   private Color boxColor = null;
   
   /** 
    * Get the background color for the graph's data box.
    * @return The background color. Includes alpha component.
    */
   public Color getBoxColor() { return(boxColor); }
   
   /** 
    * Set the background color for the graph's data box.
    * @param c The desired color. A null value is rejected. Translucent or transparent colors are permitted.
    * @return False if argument is null; else true.
    */
   public boolean setBoxColor(Color c)
   {
      if(c == null) return(false);
      if(!boxColor.equals(c))
      {
         if(doMultiNodeEdit(FGNProperty.BOXC, c)) return(true);
         
         Color old = boxColor;
         boxColor = c;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.BOXC);
            FGNRevEdit.post(this, FGNProperty.BOXC, boxColor, old);
         }
      }
      return(true);
   }
   
   /**
    * Overridden to make publicly accessible, since it makes sense to rescale an entire graph. NOTE that this applies
    * to scaling the rendering; it is unrelated to auto-ranging of the graph's axis ranges to fit data displayed within.
    */
   @Override public void rescale(int pct, boolean fontSizeOnly) { super.rescale(pct, fontSizeOnly); }

   /**
    * For most graph attributes, a change in attribute value requires a complete re-rendering of the graph and its 
    * children. The base-class implementation suffices in such cases. However, modifying the auto-range enable states of
    * the graph axes has no immediate effect, changes in the semi-automated title's text content or position have no 
    * effect when the title is hidden, and toggling the title's visibility has no effect when the title text is empty.
    * This override avoids any rendering overhead in these scenarios.
    * 
    * <p>In addition, if the coordinate system type or quadrant layout changes, an auto-range cycle is triggered if 
    * enabled for any of the axes.</p>
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;
      
      if((hint == FGNProperty.AUTORANGE) || 
            (hint==FGNProperty.HIDE && getTitle().trim().isEmpty()) ||
            (hint==FGNProperty.TITLE && isTitleHidden()) ||
            ((hint==FGNProperty.HALIGN || hint==FGNProperty.GAP) && !isTitleRendered()))
      {
         model.onChange(this, 0, false, null);
         return;
      }
      
      boolean triggeredUpdate = false;
      if(hint == FGNProperty.TYPE || hint == FGNProperty.LAYOUT) 
         triggeredUpdate = autoScaleAxes();
      if(!triggeredUpdate)
         super.onNodeModified(hint);
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
         case TYPE : ok = setCoordSys((CoordSys)propValue); break;
         case LAYOUT: ok = setLayout((Layout)propValue); break;
         case CLIP: setClip(((Boolean)propValue).booleanValue()); ok = true; break;
         case AUTORANGE: setAutorangeAxes((Autorange)propValue); ok = true; break;
         case BOXC : ok = this.setBoxColor((Color) propValue); break;
         default: ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
         case TYPE : value = getCoordSys(); break;
         case LAYOUT: value = getLayout(); break;
         case CLIP: value = new Boolean(getClip()); break;
         case AUTORANGE: value = getAutorangeAxes(); break;
         case BOXC: value = getBoxColor(); break;
         default: value = super.getPropertyValue(p); break;
      }
      return(value);
   }

   /**
    * When a plottable trace is inserted, the current range of one or more axes may need to be computed (if auto-ranging
    * is enabled for any axis). If such auto-ranging occurs, this method triggers a re-rendering of the entire graph and
    * its children. The insertion of a plottable trace will also require a re-rendering of the graph's automated legend,
    * if it is visible.
    */
   @Override protected void onSubordinateNodeInserted(FGraphicNode sub)
   {
      if(sub instanceof FGNPlottable)
      {
         if(!autoScaleAxes())
            getLegend().onInsertOrRemovePlottableNode();
      }
      super.onSubordinateNodeInserted(sub);
   }

   /**
    * When a plottable trace is removed, the current range of one or more axes may need to be computed (if auto-ranging
    * is enabled for any axis). If such auto-ranging occurs, this method triggers a re-rendering of the entire graph and
    * its children. The removal of a plottable trace will also require a re-rendering of the graph's automated legend,
    * if it is visible.
    */
   @Override protected void onSubordinateNodeRemoved(FGraphicNode sub)
   {
      if(sub instanceof FGNPlottable)
      {
         if(!autoScaleAxes())
            getLegend().onInsertOrRemovePlottableNode();
      }
      
      // NOTE: It is important that this is called AFTER updating the legend, since that update relies on cached 
      // information that will be wiped out by this call...
      super.onSubordinateNodeRemoved(sub);
   }

   /**
    * When the units token for the primary or secondary axis changes, it could impact the rendering of an associated 
    * calibration bar in this <code>GraphNode</code> if that bar is rendered and its auto label is on. This method 
    * searches for all {@link CalibrationBarNode}s currently defined as children of the graph. For each such node that 
    * meets the necessary criteria, it forces an update of the node's rendering infrastructure and accumulates the dirty
    * regions affected. If any dirty regions were accumulated as a result of this process, the graphic model is 
    * re-rendered in those regions.
    * 
    * @param isPrimary True if the axis that has changed is the primary axis of this graph. Else, it is the secondary 
    * axis (the Z axis does not have a units token!).
    */
   void onAxisUnitsChange(boolean isPrimary)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      Graphics2D g2d = model.getViewerGraphics();
      try
      {
         List<Rectangle2D> dirtyAreas = new ArrayList<Rectangle2D>();
         for(int i=0; i<getChildCount(); i++)
         {
            FGraphicNode n = getChildAt(i);
            if(n.getNodeType() == FGNodeType.CALIB)
            {
               CalibrationBarNode calib = (CalibrationBarNode) n;
               if((isPrimary == calib.isPrimary()) && calib.isRendered() && calib.isAutoLabelOn())
               {
                  dirtyAreas.add(calib.getCachedGlobalShape().getBounds2D());
                  calib.getRenderBounds(g2d, true, null);
                  FGraphicNode.updateAncestorRenderBounds(g2d, calib);
                  dirtyAreas.add(calib.getCachedGlobalShape().getBounds2D());
               }
            }
         }

         if(dirtyAreas.size() > 0)
            model.onChange(this, -1, true, dirtyAreas);
      }
      finally { if(g2d != null) g2d.dispose(); }
   }

   /**
    * Whenever the major tick set along the primary or secondary axis changes in some way, invoke this method to ensure 
    * that the corresponding set of grid lines are re-rendered. If the grid lines are currently hidden, no action is 
    * taken. Otherwise, this method updates the rendering infrastructure of the relevant {@link GridLineNode} component, 
    * then triggers a rendering update within the graph's data box, the rectangle within which grid lines are confined.
    * 
    * @param isPrimary True if the major tick set of the primary axis was changed, affecting the locations of secondary 
    * grid lines; false if the secondary axis's major tick set was change, affecting the locations of primary grid 
    * lines. (The graph's Z axis does not define the notion of a "major tick set".)
    */
   void onAxisMajorTicksChanged(boolean isPrimary)
   {
      FGraphicModel model = getGraphicModel();
      GridLineNode grid = isPrimary ? getSecondaryGridLines() : getPrimaryGridLines();
      
      if(model != null && grid.isStroked() && !grid.getHide())
      {
         Graphics2D g2d = model.getViewerGraphics();
         try
         {
            grid.getRenderBounds(g2d, true, null);
         }
         finally { if(g2d != null) g2d.dispose(); }
         
         List<Rectangle2D> dirty = new ArrayList<Rectangle2D>();
         dirty.add( getLocalToGlobalTransform().createTransformedShape(getBoundingBoxLocal()).getBounds2D() );
         model.onChange(this, -1, true, dirty);
      }
   }

   @Override boolean isAxisAutoranged(FGNGraphAxis axis)
   {
      boolean auto = false;
      if(axis != null)
      {
         if(axis == getColorBar()) auto = autorangeAxes.isZAxisAutoranged();
         else if(axis == getPrimaryAxis()) auto = autorangeAxes.isXAxisAutoranged();
         else if(axis == getSecondaryAxis()) auto = autorangeAxes.isYAxisAutoranged();
      }
      return(auto);
   }
   
   /**
    * Automatically fix the range of the graph's axes so that all plottable data and functions in this graph are 
    * visible within its data window, skipping any axis for which the auto-ranging feature is disabled. This method is 
    * central to the auto-ranging feature; it must be invoked whenever a {@link FGNPlottable} node is added or removed,
    * or whenever that node's displayed data range is altered for any reason (the underlying data set or function itself
    * is modified; or a node property that affects the data range is modified).
    * 
    * <p>If auto-ranging is currently disabled for all graph axes, this method takes no action. Otherwise, it traverses 
    * all child <code>FGNPlottable</code>s to calculate the overall min/max range needed along each auto-ranged axis to 
    * "show" all the data and function samples along that axis. The axis range is then set accordingly. Axes for which
    * auto-ranging is disabled are not touched. If a change in any axis range occurs (it's possible that the range could
    * be unchanged on any given invocation), then the method triggers a re-rendering of the graph and all children.</p>
    * 
    * @return True if the range of at least one axis was altered, in which case the caller need not trigger a refresh 
    * since this method will have already done so! Returns false otherwise.
    */
   @Override boolean autoScaleAxes()
   {
      // if auto-ranging feature is disabled for all axes, then return immediately
      if(autorangeAxes == Autorange.NONE) return(false);
      
      // traverse plottable nodes and get the overall min/max along each axis across all functions datasets in graph
      float xMin = Float.MAX_VALUE;
      float xMax = -Float.MAX_VALUE;
      float yMin =  Float.MAX_VALUE;
      float yMax = -Float.MAX_VALUE;
      float zMin = Float.MAX_VALUE;
      float zMax = -Float.MAX_VALUE;
      boolean autoRangeZ = false;
      for(int i=0; i<getChildCount(); i++) if(getChildAt(i) instanceof FGNPlottable)
      {
         FGNPlottable plottable = (FGNPlottable) getChildAt(i);
         float[] extrema = plottable.getDataRange();
         if(extrema[0] < xMin) xMin = extrema[0];
         if(extrema[1] > xMax) xMax = extrema[1];
         if(extrema[2] < yMin) yMin = extrema[2];
         if(extrema[3] > yMax) yMax = extrema[3];
         
         if(autorangeAxes.isZAxisAutoranged() && plottable.hasZData())
         {
            autoRangeZ = true;
            if(extrema[4] < zMin) zMin = extrema[4];
            if(extrema[5] > zMax) zMax = extrema[5];
         }
      }
      if(xMax < xMin) xMin = xMax = 0;
      if(yMax < yMin) yMin = yMax = 0;
      if(zMax < zMin) zMin = zMax = 0;
      
      // auto-range each axis for which the feature is enabled
      boolean needUpdate = autorangeAxes.isXAxisAutoranged() && getPrimaryAxis().doAutoRange(xMin, xMax);
      if(autorangeAxes.isYAxisAutoranged() && getSecondaryAxis().doAutoRange(yMin, yMax)) needUpdate = true;
      if(autoRangeZ && getColorBar().doAutoRange(zMin, zMax)) needUpdate = true;
      else autoRangeZ = false;  // will be set if Z axis range has changed
      
      // trigger re-rendering of the entire graph if the range of any axis was updated.
      if(needUpdate)
      {
         // if this auto-ranging cycle affected the Z axis, then the color map has changed: inform any plottables that
         // are 2D representations of 3D data.
         if(autoRangeZ) for(int i=0; i<getChildCount(); i++)
         {
            FGraphicNode n = getChildAt(i);
            if(n instanceof FGNPlottable)
               ((FGNPlottable) n).onColorMapChange();
         }
         super.onNodeModified(null);
      }
      
      return(needUpdate);
   }
   
   /**
    * Does any (X,Y) data point in any of the graph's data presentation nodes lie OUTSIDE the data window as defined by
    * the validated ranges for graph's primary and secondary axes? The rendering of a data presentation having such
    * "out of bounds" points will be partially hidden if the graph's clip flag is set.
    * @return True if any data points lie outside the graph's current data box.
    */
   public boolean isXYDataOutOfBounds()
   {
      // traverse plottable nodes and get the overall min/max along primary and secondary axes
      float xMin = Float.MAX_VALUE;
      float xMax = -Float.MAX_VALUE;
      float yMin =  Float.MAX_VALUE;
      float yMax = -Float.MAX_VALUE;
      for(int i=0; i<getChildCount(); i++) if(getChildAt(i) instanceof FGNPlottable)
      {
         FGNPlottable plottable = (FGNPlottable) getChildAt(i);
         float[] extrema = plottable.getDataRange();
         if(extrema[0] < xMin) xMin = extrema[0];
         if(extrema[1] > xMax) xMax = extrema[1];
         if(extrema[2] < yMin) yMin = extrema[2];
         if(extrema[3] > yMax) yMax = extrema[3];
      }
      if(xMax < xMin) xMin = xMax = 0;
      if(yMax < yMin) yMin = yMax = 0;
      
      // get validated range for primary and secondary axes
      double xStart = getPrimaryAxis().getValidatedStart();
      double xEnd = getPrimaryAxis().getValidatedEnd();
      if(xStart > xEnd) { double tmp = xEnd; xEnd = xStart; xStart = tmp; }
      double yStart = getSecondaryAxis().getValidatedStart();
      double yEnd = getSecondaryAxis().getValidatedEnd();
      if(yStart > yEnd) { double tmp = yEnd; yEnd = yStart; yStart = tmp; }
      
      return(xMin < xStart || xMax > xEnd || yMin < yStart || yMax > yEnd);
   }
   
   @Override protected boolean isTitleAboveGraph()
   {
      return(!getPrimaryAxis().isAboveLeft());
   }


   
   // 
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet()  { return(true); }

   /** 
    * The node-specific properties exported in a graph node's style set are the coordinate system and layout, the 
    * auto-range state, the 'clip' flag, the data box background color, and attributes affecting the visibility or
    * alignment of the graph's semi-automated title.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.TYPE, getCoordSys());
      styleSet.putStyle(FGNProperty.LAYOUT, getLayout());
      styleSet.putStyle(FGNProperty.AUTORANGE, getAutorangeAxes());
      styleSet.putStyle(FGNProperty.BOXC, getBoxColor());
      super.putNodeSpecificStyles(styleSet);
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      // as in setCoordSys(), we adjust the quadrant layout and the visibility of X,Y axes and gridlines when we switch
      // from a non-polar to a polar coordinates system, or vice versa. However, we do so WITHOUT triggering any
      // "node modified" notifications or posting an undo op -- since all the changes in a style application should
      // occur as a single atomic operation.
      CoordSys sys = (CoordSys) applied.getCheckedStyle(FGNProperty.TYPE, getNodeType(), CoordSys.class);
      if(sys != null && !sys.equals(restore.getStyle(FGNProperty.TYPE)))
      {
         boolean wasPolar = isPolar();
         coordSys = sys;
         if(isPolar() && !wasPolar)
         {
            layout = Layout.ALLQUAD;
            getPrimaryAxis().setHideNoNotify(true);
            getSecondaryAxis().setHideNoNotify(true);
            getPrimaryGridLines().setHideNoNotify(false);
            getSecondaryGridLines().setHideNoNotify(false);
         }
         else if(wasPolar && !isPolar())
         {
            layout = Layout.QUAD1;
            getPrimaryAxis().setHideNoNotify(false);
            getSecondaryAxis().setHideNoNotify(false);
            getPrimaryGridLines().setHideNoNotify(true);
            getSecondaryGridLines().setHideNoNotify(true);
         }

         changed = true;
      }
      else restore.removeStyle(FGNProperty.TYPE);
      
      Layout gl = (Layout) applied.getCheckedStyle(FGNProperty.LAYOUT, getNodeType(), Layout.class);
      if(gl != null && !gl.equals(restore.getStyle(FGNProperty.LAYOUT)))
      {
         layout = gl;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LAYOUT);
      
      Autorange r = (Autorange) applied.getCheckedStyle(FGNProperty.AUTORANGE, getNodeType(), Autorange.class);
      if(r != null && !r.equals(restore.getStyle(FGNProperty.AUTORANGE)))
      {
         autorangeAxes = r;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.AUTORANGE);
      
      Color c = (Color) applied.getCheckedStyle(FGNProperty.BOXC, getNodeType(), Color.class);
      if(c != null && !c.equals(restore.getCheckedStyle(FGNProperty.BOXC, null, Color.class)))
      {
         boxColor = c;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.BOXC);
      
      // the super class handles some styles for all graph types
      if(super.applyNodeSpecificStyles(applied, restore))
         changed = true;
      
      // an axis, color axis or legend node can be the source node for a style set. When applying such a style set to
      // a graph, the axis styles should be applied to all 3 graph axes, and the legend styles should be applied to the
      // graph's legend. We handle these possibilities below.
      
      // this is just a check to be sure...
      if(restore.getSourceNodeType() != FGNodeType.GRAPH || restore.getNumberComponentStyleSets() < 6) 
         return(changed);
      
      FGNodeType srcNT = applied.getSourceNodeType();
      if(srcNT == FGNodeType.LEGEND)
      {
         if(getLegend().applyTextDrawStyles(applied, restore.getComponentStyleSet(5))) changed = true;
         if(getLegend().applyNodeSpecificStyles(applied, restore.getComponentStyleSet(5))) changed = true;
      }
      else if(srcNT == FGNodeType.AXIS || srcNT == FGNodeType.CBAR)
      {
         for(int i=0; i<3; i++)
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

   @Override
   public boolean canInsert(FGNodeType nodeType)
   {
      return(nodeType == FGNodeType.TRACE || nodeType == FGNodeType.RASTER || nodeType == FGNodeType.CONTOUR ||
            nodeType == FGNodeType.BAR || nodeType == FGNodeType.BOX || nodeType == FGNodeType.SCATTER || 
            nodeType == FGNodeType.AREA || nodeType == FGNodeType.PIE || nodeType == FGNodeType.FUNCTION || 
            nodeType == FGNodeType.CALIB || nodeType == FGNodeType.LABEL || nodeType == FGNodeType.TEXTBOX || 
            nodeType == FGNodeType.LINE || nodeType == FGNodeType.SHAPE || nodeType == FGNodeType.IMAGE || 
            nodeType == FGNodeType.PGRAPH || nodeType == FGNodeType.GRAPH);
   }

   @Override public FGNodeType getNodeType() { return(FGNodeType.GRAPH); }

   
   //
   // Focusable/Renderable support
   //

   /**
    * The local rendering coordinate system of a <b>GraphNode</b> is defined by its location <i>(x,y)</i> and size 
    * <i>(width, height)</i>, properties that are specifed with respect to the parent viewport. The origin in the local 
    * coordinate system lies at <i>(x, y)</i>. The graph may be rotated WRT its parent via the <i>rotate</i> property. 
    * This method accounts for these properties when calculating the local-to-parent transform. If the transform is 
    * ill-defined for whatever reason, the identity transform is returned.
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

      // rotate wrt parent if necessary
      double rot = getRotate();
      if(rot !=  0) at.rotate(Math.toRadians(rot));
      
      return(at);
   }

   /**
    * The physical size of a <b>GraphNode</b>'s viewport is defined by the graph's bounding box (WRT to the graph's 
    * parent container). In addition, the graph's axis ranges, layout, and coordinate system type define a "user" 
    * coordinate system which is mapped onto the standard viewport coordinate system. Coordinates specified in real 
    * units (in,cm,pt,mm) or percentages are mapped to physical viewport units in a straightforward way based on the 
    * physical dimensions of the viewport. User coordinates, however, are transformed in a non-trivial way IAW the 
    * graph's current configuration and axis ranges.
    */
   @Override public FViewport2D getViewport()
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(null);
      Rectangle2D rect = parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      if(rect == null) return(null);
      double w = rect.getWidth();
      double h = rect.getHeight();

      AxisNode primaryAxis = getPrimaryAxis();
      AxisNode secondaryAxis = getSecondaryAxis();

      if( !isPolar() )
      {
         boolean isLogX = (coordSys == CoordSys.LOGLOG) || (coordSys == CoordSys.SEMILOGX);
         boolean isLogY = (coordSys == CoordSys.LOGLOG) || (coordSys == CoordSys.SEMILOGY);

         // get viewport bounds in user units, based on current validated axis ranges; for cartesian, semilog, and 
         // loglog graphs, these are the same regardless of axis layout
         double left = primaryAxis.getValidatedStart();
         double right = primaryAxis.getValidatedEnd();
         double bottom = secondaryAxis.getValidatedStart();
         double top = secondaryAxis.getValidatedEnd();

         return( new FViewport2D(w, h, left, right, bottom, top, isLogX, isLogY ) );
      }
      else
      {
         // the minimum radius for a polar or semilogR graph is the VALIDATED start of the radial axis range.  this 
         // will always be >=0 for a polar plot and >0 for semilogR. For these plots, minRadius (or log10(minRadius)) 
         // is mapped to the polar origin.
         double minRadius = secondaryAxis.getValidatedStart();

         // compute viewport coordinates of the polar coord system origin, WRT BL corner of data window -- depending on 
         // desired quadrant layout.
         double x0 = 0;
         double y0 = 0;
         if(layout == Layout.QUAD3 || layout == Layout.QUAD4) y0 += h;
         if(layout == Layout.QUAD2 || layout == Layout.QUAD3) x0 += w;
         if(layout == Layout.ALLQUAD)
         {
            x0 = w / 2.0;
            y0 = h / 2.0;
         }

         // compute physical length of a unit radius. This takes into account the radial axis range. If the viewport 
         // is rectangular, we use the smaller dimension so that the specified radial axis range is visible for all 
         // theta values. For a semilogR graph, we compute the physical length of one decade along the radial axis.
         double minDim = Math.min(w, h);
         double unitRadius = secondaryAxis.getValidatedEnd();
         if(coordSys == CoordSys.POLAR)
         {
            unitRadius = minDim / (unitRadius - minRadius);
         }
         else
         {
            double logMin = Utilities.log10( minRadius );
            double logMax = Utilities.log10( unitRadius );
            unitRadius = minDim / (logMax-logMin);
         }

         if(layout == Layout.ALLQUAD) unitRadius /= 2.0;

         return(new FViewport2D(w, h, new Point2D.Double(x0,y0), unitRadius, minRadius, coordSys==CoordSys.SEMILOGR));
      }
   }

   /**
    * A <code>GraphNode</code> does not do any rendering itself; all rendering is handled by its subordinates. However, 
    * it does optionally clip the rendering of all <code>TraceNode</code> and <code>FunctionNode</code> children. The 
    * axes, grid lines, legend, calibration bars, and any other kind of child node are rendered without clipping.
    * 
    * <p>Drawing order: The 3 axes are drawn first, then any visible gridlines, then all non-component children in the
    * order they were added to the graph, and lastly, the legend. (As of v4.3.2. In prior versions, the legend was 
    * drawn before any non-component children.)</p>
    * 
    * <p>When clipping is on, the clip shape depends on the graph type. If the graph is not polar, it is simply the 
    * graph's viewport rectangle. For polar graphs, it will be a circle or a quarter-circle, depending on the current 
    * quadrant layout.</p>
    */
   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(!(isRendered() && needsRendering(task)))
         return(true);

      // get graph's bounding rectangle with respect to the parent viewport; if it is ill-defined, render nothing.
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(true);
      Rectangle2D rBox = parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      if(rBox == null || rBox.getWidth() <= 0 || rBox.getHeight() <= 0)
         return(true);

      // render graph in a copy of the graphics context, so we do not alter the original. If clipping is on, we set the
      // clip shape ONLY when a plottable node is to be rendered. Depending on the order of the graph children, we may
      // have to set the clip shape then restore the old clip several times.
      // NOTE: In a previous version, we used two contexts -- and unclipped and a clipped version. While this works fine
      // for rendering to screen, it fails on export to PDF, because the PdfGraphics2D contexts are like separate layers
      // that are drawn in the order they're created. This could violate the intended order in which graph children are
      // drawn. See IText class PDFGraphics2D.
      double rot = getRotate();
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      Shape clipShape = getClip() ? getDataClipShape() : null;
      boolean isClipped = false;  
      try
      {
         // transform graphics context to the graph's viewport.
         g2dCopy.translate(rBox.getX(), rBox.getY());
         if(rot != 0) g2dCopy.rotate(Math.toRadians(rot));
         Shape clipOrig = g2dCopy.getClip();

         // if the data box (which could be a circle or quarter-circle in polar modes) background is not transparent,
         // fill it first. The clip shape is what we need to fill.
         if(boxColor.getAlpha() != 0)
         {
            g2dCopy.setColor(boxColor);
            g2dCopy.fill(clipShape != null ? clipShape : getDataClipShape());
         }
         
         // render all components first -- except the legend (they are never clipped)
         LegendNode legend = getLegend();
         for(int i=0; i<getComponentNodeCount(); i++) if(getComponentNodeAt(i) != legend)
         {
            if(!getComponentNodeAt(i).render(g2dCopy, task))
               return(false);
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
         
         // render the legend and semi-automated title, unclipped
         if(isClipped)
         {
            g2dCopy.setClip(clipOrig);
            isClipped = false;
         }
         if(!legend.render(g2dCopy, task)) return(false);
         
         if(!renderAutoTitle(g2dCopy, task)) return(false);
      }
      finally 
      { 
         if(g2dCopy != null) g2dCopy.dispose(); 
      }
      return(true);
   }

   /**
    * Helper method calculates the current data clipping shape for this <code>GraphNode</code> IAW with the current 
    * graph type and quadrant layout. The clip shape is simply the data box rect unless the graph is polar, in which 
    * case it is a full circle or quarter circle wedge, depending on the quadrant layout.
    * 
    * @return The clip shape, in local rendering coordinates of this <code>GraphNode</code>.
    */
   private Shape getDataClipShape()
   {
      // the data box rect in local coordinates
      Rectangle2D rBox = getBoundingBoxLocal();

      // if this is not a polar graph, or viewport rectangle is empty, just return that
      if(rBox.isEmpty() || !isPolar()) return(rBox);

      // polar graphs: in all-quad layout, clip shape is a full circle with diam = the smaller dimension of viewport 
      // rect. In single-quad layouts, it is a quarter circle wedge with radius = the smaller dimension.
      Shape clip = null;
      double w = rBox.getWidth();
      double h = rBox.getHeight();
      double minDim = Math.min(w, h);
      if(layout == Layout.QUAD1)
         clip = new Arc2D.Double(-minDim, -minDim, 2*minDim, 2*minDim, 0, -90, Arc2D.PIE);
      else if(layout == Layout.QUAD2)
         clip = new Arc2D.Double(w-minDim, -minDim, 2*minDim, 2*minDim, -90, -90, Arc2D.PIE);
      else if(layout == Layout.QUAD3)
         clip = new Arc2D.Double(w-minDim, h-minDim, 2*minDim, 2*minDim, -180, -90, Arc2D.PIE);
      else if(layout == Layout.QUAD4)
         clip = new Arc2D.Double(-minDim, h-minDim, 2*minDim, 2*minDim, -270, -90, Arc2D.PIE);
      else
         clip = new Ellipse2D.Double(w/2 - minDim/2, h/2 - minDim/2, minDim, minDim);

      return(clip);
   }

   //
   // PSTransformable implementation
   //
   
   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      // get parent viewport; if there is none, then the graph cannot be rendered
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      // get graph's bounding rectangle with respect to the parent viewport
      Rectangle2D rect = parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      if(rect == null) return;
      Point2D botLeft = new Point2D.Double(rect.getX(), rect.getY());

      // start the element and establish a new viewport with origin at graph's bottom left corner. We want a 
      // NON-CLIPPING viewport, so we don't need to specify W,H of bounding box
      psDoc.startElement(this);
      psDoc.setViewport(botLeft, 0, 0, getRotate(), false);
      Point2D localOrigin = new Point2D.Double();
      
      // origin, radius and start angle for the circular arc defining the polar clip shape (if applicable)
      Point2D polarOrigin = null;
      double polarRad = 0;
      double polarStart = 0;
      double polarExt = 90;
      if(isPolar())
      {
         polarOrigin = new Point2D.Double();
         polarRad = Math.min(rect.getWidth(), rect.getHeight());
         polarStart = 0;
         switch(layout)
         {
         case QUAD1 :
            break;
         case QUAD2 :
            polarOrigin.setLocation(rect.getWidth(), 0);
            polarStart = 90;
            break;
         case QUAD3 :
            polarOrigin.setLocation(rect.getWidth(), rect.getHeight());
            polarStart = 180;
            break;
         case QUAD4 :
            polarOrigin.setLocation(0, rect.getHeight());
            polarStart = 270;
            break;
         case ALLQUAD:
            polarRad /= 2;
            polarOrigin.setLocation(rect.getWidth()/2, rect.getHeight()/2);
            polarExt = 360;
            break;
         }
      }
      
      // if the data box (which could be a circle or quarter-circle in polar modes) background is not transparent,
      // fill it first.
      if(boxColor.getAlpha() != 0)
      {
         BkgFill bkgFill = BkgFill.createSolidFill(boxColor);
         
         // for a Cartesian coordinate system, we just fill the data box rectangle; for polar case, we have to set a
         // clipping viewport with a polar shape first. So we have to save and restore the graphics state in that
         // scenario.
         if(isPolar())
         {
            psDoc.saveGraphicsState();
            psDoc.setViewport(localOrigin, rect.getWidth(), rect.getHeight(), 0, true);
            psDoc.polarClip(polarOrigin, polarRad, polarStart, polarExt, false);
            psDoc.renderRect(rect.getWidth(), rect.getHeight(), 0, false, bkgFill);
            psDoc.restoreGraphicsState();
         }
         else 
            psDoc.renderRect(rect.getWidth(), rect.getHeight(), 0, false, bkgFill);
      }

      // render graph subcomponents (axes, etc) first, except for legend. These are never clipped.
      getPrimaryAxis().toPostscript(psDoc);
      getSecondaryAxis().toPostscript(psDoc);
      getColorBar().toPostscript(psDoc);
      getPrimaryGridLines().toPostscript(psDoc);
      getSecondaryGridLines().toPostscript(psDoc);

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
               if(isPolar()) 
                  psDoc.polarClip(polarOrigin, polarRad, polarStart, polarExt, false);
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

      // finally, render the legend as semi-automated title last, unclipped.
      getLegend().toPostscript(psDoc);
      autoTitleToPostscript(psDoc);

      // end the element
      psDoc.endElement();
   }
}
