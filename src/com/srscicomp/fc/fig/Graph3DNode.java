package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import com.srscicomp.common.g2dutil.Projector;
import com.srscicomp.common.g2dutil.ResizeAnchor;
import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dutil.StrokeJoin;
import com.srscicomp.common.g2dutil.TextBoxPainter;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;


/**
 * <b>Graph3DNode</b> encapsulates a 3D plot in the <i>DataNav</i> graphic model. A 3D Cartesian coordinate system XYZ
 * is presented as a 3D rectangular box that is projected onto the 2D "screen". The three "backplanes" of this box are
 * generally filled, while the three front sides are "see-through" to enhance the 3D illusion. It includes nine 
 * <i>component</i> nodes that define the 3D graph's X/Y/Z axes, the XY/XZ/YZ backplanes, and the X/Y/Z grid lines. Any
 * of the 3 dimensions can be scaled linearly or logarithmically, but this is controlled by the 3D axis node component
 * for each axis -- see {@link Axis3DNode}. An automated legend and a color bar is also supported. Also, this node does 
 * not admit other 2D or 3D graph objects as children; it may only appear in the graphic model as a child of the root 
 * figure node.
 * 
 * <p><b>Graph3DNode</b> possesses all of the inheritable style attributes, as well as the common positioning attributes 
 * <i>x</i>, <i>y</i>, <i>width</i>, <i>height</i>, and <i>rotate</i>. However, these have a very different meaning than
 * for the 2D graph object. The coordinates <i>(x,y)</i> locate, relative to the root figure's viewport, the origin of 
 * the XYZ system. The <i>width</i>, <i>height</i>, and <i>depth</i> properties specify the X, Y and Z dimensions, 
 * respectively, of the box that represents XYZ in the 3D "world". The <i>rotate</i> and <i>elevate</i> attributes
 * specify two sequential rotations in 3D that orient the XYZ system in that world, and the <i>distance</i> property
 * indicates the distance from the center of projection (the camera's viewpoint) to the projection plane. These various
 * properties, along with the axis ranges in X, Y and Z, define the 3D-to-2D projection from XYZ onto the <i>FypML</i>
 * model's 2D image plane.</p>
 * 
 * <p>Given its 3D nature, <b>Graph3DNode</b> doesn't fit as well into the <i>FypML</i> graphic model's 2D rendering
 * infrastructure. One of the basic tenets of that infrastructure is that any node that can contain children must
 * specify a 2D "parent viewport" within which those child nodes are located and sized. Any basic 2D object like a 
 * text label, line segment, or shape renders itself WRT the parent viewport.</p>
 * 
 * <p>To fit into this framework, <b>Graph3DNode</b> defines its 2D viewport as the rectangle that tightly bounds the
 * 2D projection of its 3D data box; the origin lies at the bottom-left corner of this rectangle. Note that the size
 * and location of the rectangle varies with any of the parameters that affect the projection geometry: location (X,Y)
 * in its parent viewport; width, height and depth of the 3D data box; the projection distance scale factor; and the
 * rotation and elevation angles. Also note that while the 3D box can be rotated around two axes; the 3D graph's 2D
 * viewport is never rotated -- its principal axes are parallel to the principal axes of the parent figure.</p>
 * 
 * @author 	sruffner
 */
public class Graph3DNode extends FGNGraph implements Cloneable
{
   /** Enumeration of the three axes for a 3D graph object. */
   public enum Axis { X, Y, Z }

   /** Enumeration of the three backplane sides for a 3D graph object. */
   public enum Side { XY, XZ, YZ }

   /**
    * Construct an empty 3D graph having a 2-in x 2-in x 2-in 3D coordinate system box, with the origin of that
    * coordinate system lying in the projection plane at (50%, 50%) in the graph's parent viewport. The backdrop
    * style is initially a 3D cube (with front projecting edges drawn). The cube's rotation about its Z and X axes are 
    * set to 30 and 15 degrees, respectively, and the projection distance scale factor is 5. The 3D graph has no title 
    * or ID initially, and all three axes are configured to span a range of [0..100], each with a user-defined default 
    * set of tick marks and a generic axis label. Most inherited style properties are implicit initially, with two 
    * exceptions: the stroke join and end cap are both set to rounded because this improves the appearance of the 3D 
    * box backdrop.
    */
   public Graph3DNode()
   {
      super(true, false, false);
      setX(new Measure(50, Measure.Unit.PCT));
      setY(new Measure(50, Measure.Unit.PCT));
      setWidth(new Measure(2, Measure.Unit.IN));
      setHeight(new Measure(2, Measure.Unit.IN));
      setDepth(new Measure(2, Measure.Unit.IN));
      setProjectionDistanceScale(5);
      setRotate(30);
      setElevate(15);
      setBackDrop(BackDrop.BOX3D);
      setTitle("");
      setID("");
      setStrokeEndcap(StrokeCap.ROUND);
      setStrokeJoin(StrokeJoin.ROUND);
      
      // set up component nodes for 3D graph: axes, grid lines, backplanes, legend, and color bar...
      // NOTE: Order is crucial here; e.g., grid lines must draw on top of backplanes!
      addComponentNode(new BackPlane3DNode(Side.XY));
      addComponentNode(new BackPlane3DNode(Side.XZ));
      addComponentNode(new BackPlane3DNode(Side.YZ));
      addComponentNode(new Grid3DNode(Axis.X));
      addComponentNode(new Grid3DNode(Axis.Y));
      addComponentNode(new Grid3DNode(Axis.Z));
      addComponentNode(new Axis3DNode(Axis.X));
      addComponentNode(new Axis3DNode(Axis.Y));
      addComponentNode(new Axis3DNode(Axis.Z));
      addComponentNode(new LegendNode());
      addComponentNode(new ColorBarNode());
   }

   /**
    * Get the rectangle that tightly bounds this 3D graph's "plot box", as defined by its current 3D-to-2D projection
    * geometry. The plot box includes only the area covered by the 2D projection of the graph's 3D coordinate system.
    * The graph axes generally lie outside this plot box.
    * 
    * <p>NOTE that this rectangle defines the viewport rectangle for the 3D graph. Child nodes like text and lines are
    * positioned WRT this viewport.</p>
    * 
    * @param r If non-null, the bounding rectangle is returned in this object. Else a new rectangle is allocated.
    * @return The bounding rectangle. Its bottom-left corner is always (0,0), as the rectangle is defined in the 3D
    * graph's own viewport.
    */
   public Rectangle2D getPlotBoxBounds(Rectangle2D r)
   {
      update2DProjection();
      if(r == null) r = new Rectangle2D.Double();
      r.setFrame(0, 0, rBoundsSelf.getWidth(), rBoundsSelf.getHeight());
      return(r);
   }
   
   // 
   // Required components of a GraphNode -- these are never removed.
   //

   /**
    * Get the component node encapsulating one of the three axes for this 3D graph.
    * 
    * @param a The 3D graph axis node requested.
    * @return The axis component node.
    */
   public Axis3DNode getAxis(Axis a) 
   { 
      int which = (a==Axis.X) ? 6 : (a==Axis.Y ? 7 : 8);
      return((Axis3DNode)getComponentNodeAt(which)); 
   }

   /**
    * Get the component node encapsulating one of the three sets of grid lines for this 3D graph. Each can be rendered 
    * with different stroke properties. Grid line locations are defined by the first tick set associated with the axis
    * perpendicular to the grid lines.
    * 
    * @param perpAxis The 3D graph axis that is perpendicular to the requested set of grid lines.
    * @return The grid lines component node.
    */
   public Grid3DNode getGrid3DNode(Axis perpAxis) 
   { 
      int which = (perpAxis==Axis.X) ? 3 : (perpAxis==Axis.Y ? 4 : 5);
      return((Grid3DNode)getComponentNodeAt(which)); 
   }

   /**
    * Get the component node encapsulating one of the backplanes of this 3D graph. Each can be rendered with different
    * stroke and background fill properties.
    * 
    * @param side Identifies the desired backplane.
    * @return The backplane component
    */
   public BackPlane3DNode getBackPlane(Side side) 
   { 
      int which = (side==Side.XY) ? 0 : (side==Side.XZ ? 1 : 2);
      return((BackPlane3DNode)getComponentNodeAt(which)); 
   }

   @Override public LegendNode getLegend() { return((LegendNode)getComponentNodeAt(9)); }
   @Override public ColorBarNode getColorBar() { return((ColorBarNode)getComponentNodeAt(10)); }


   //
   // FGNGraph
   //
   
   /** The 3D graph node only supports a 3D coordinate system. */
   @Override public CoordSys getCoordSys() { return(CoordSys.THREED); }
   /** Returns false always: The 3D graph node does not support the auto-scaling feature. */
   @Override boolean isAxisAutoranged(FGNGraphAxis axis) { return(false); }
   /** Returns false always: The 3D graph node does not support the auto-scaling feature. */
   @Override boolean autoScaleAxes() { return(false); }
   /** Returns the rectangle that tightly bounds the graph's 3D plot box projected onto the 2D plane. */
   @Override public Rectangle2D getBoundingBoxLocal() { return(getPlotBoxBounds(null)); }

   //
   // Properties
   //


   /** An enumeration of the different backdrop styles supported by the 3D graph node.
    */
   public enum BackDrop
   {
      /** Backdrop not shown: No axes, no backplanes, no grid. */
      HIDDEN("hidden"), 
      
      /** Standard 3D box backdrop, with XY, YZ and XZ backplanes, plus forward projecting front edges drawn. */
      BOX3D("box3D"), 

      /** Standard 3D box backdrop without the 3 front edges that "complete the box". */
      OPENBOX3D("openBox3D"), 
      
      /** XY backplane along with all 3 axes; XZ and YZ backplanes (and grid lines therein) are hidden. */
      XYPLANE("xyPlane"),
      
      /** No backplanes or grid; axes only drawn along outer edges of 3D box. */
      AXESOUTER("axesOuter"), 
      
      /** No backplanes or grid; axes drawn in traditional configuration, emanating from back corner of 3D box. */
      AXESBACK("axesBack");
      
      private final String tag;
      
      BackDrop(String tag) { this.tag = tag; }
      
      @Override
      public String toString() { return(tag); }
   }

   /** The 3D graph's backdrop style. */
   private BackDrop backDrop;
   
   /**
    * Get the current 3D backdrop style for the graph.
    * @return The backdrop style.
    */
   public BackDrop getBackDrop() { return(backDrop); }
   
   /**
    * Set the 3D backdrop style for this graph. If a change is made, the method {@link #onNodeModified} is invoked.
    * 
    * @param bd The new backdrop style. A null value is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setBackDrop(BackDrop bd)
   {
      if(bd == null) return(false);
      if(this.backDrop != bd)
      {
         if(doMultiNodeEdit(FGNProperty.LAYOUT, bd)) return(true);
         
         BackDrop old = this.backDrop;
         this.backDrop = bd;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.LAYOUT);
            FGNRevEdit.post(this, FGNProperty.LAYOUT, this.backDrop, old, "Change 3D backdrop style");
         }
      }
      return(true);
   }
   
   /**
    * Is the 3D graph's current color map scaled logarithmically or linearly? Since the color map pertains to Z 
    * coordinate values, the map is logarithmic if the Z axis is logarithmic.
    * @return True if color map is scaled logarithmically; else, linearly.
    */
   public boolean isLogarithmicColorMap() { return(getAxis(Axis.Z).isLogarithmic()); }
   

   /** Depth, or Z extent, of the box representing the 3D coordinate system, as measured in the "3D world". */
   private Measure depth;
   
   /**
    * Get the depth -- i.e., the dimension in the Z-direction -- of the box that represents the 3D coordinate system 
    * modeled by this 3D graph node. The width, height and depth of the box are all measured in the "3D world", not in
    * the projected 2D image that is ultimately rendered. The rendered size of the projected graph will depend on the
    * the projection distance, rotation and elevation angles as well as the specified box dimensions.
    * @return The current depth of the 3D graph box.
    */
   public Measure getDepth() { return(depth); }
   
   /**
    * Set the depth of the 3D graph box (see {@link #getDepth()} for details). If a change is made, {@link 
    * #onNodeModified} is invoked.
    * 
    * @param m The new depth. The measure is constrained to satsify {@link FGraphicModel#getSizeConstraints}. A null
    * value is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setDepth(Measure m)
   {
      if(m == null) return(false);
      m = FGraphicModel.getSizeConstraints(getNodeType()).constrain(m);
      
      if((depth != m) && !Measure.equal(depth, m))
      {
         if(doMultiNodeEdit(FGNProperty.DEPTH, m)) return(true);

         Measure oldD = depth;
         depth = m;
         
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.DEPTH);
            FGNRevEdit.post(this, FGNProperty.DEPTH, depth, oldD);
         }
      }
      return(true);
   }
   
   /** 
    * The projection distance scale factor. Restricted to [2..20]. The larger the value, the greater the distance from
    * the "camera" to the projection plane. The actual distance is this scale factor X the maximum dimension of the 3D
    * data box.
    */
   private int distanceScale;

   /**
    * Get the projection distance scale factor for this 3D graph node. The projection distance -- the perpendicular 
    * distance from the center of projection (the "camera lens") to the projection image plane -- is the product of this
    * scale factor and the maximum dimension of the graph's data box (width, height or depth). <i>Note: By setting the
    * scale factor rather than the projection distance, the projection algorithm is robust to changes in the data box
    * dimensions. The minimum allowed scale factor is 2; the projection algorithm breaks down at smaller scales.</i>
    * 
    * @return The current projection distance scale factor.
    */
   public int getProjectionDistanceScale() { return(distanceScale); }
   
   /** The minimum allowed value for the projection distance scale factor. */
   public final int MIN_PRJSCALE = 2;
   /** The maximum allowed value for the projection distance scale factor. */
   public final int MAX_PRJSCALE = 20;
   
   /**
    * Set the projection distance scale factor for this 3D graph node (see {@link #getProjectionDistanceScale()} for 
    * details). If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param s The new scale factor. Must lie in [{@link #MIN_PRJSCALE} .. {@link #MAX_PRJSCALE}].
    * @return True if successful; false if value was rejected.
    */
   public boolean setProjectionDistanceScale(int s)
   {
      if(s < MIN_PRJSCALE || s > MAX_PRJSCALE) return(false);
      
      if(s != distanceScale)
      {
         if(doMultiNodeEdit(FGNProperty.PSCALE, s)) return(true);

         Integer old = distanceScale;
         distanceScale = s;
         
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.PSCALE);
            FGNRevEdit.post(this, FGNProperty.PSCALE, distanceScale, old);
         }
      }
      return(true);
   }
   
   /**
    * The elevation angle, i.e, the rotation of the 3D coordinate system about its X-axis. This rotation follows an
    * initial rotation about its Z-axis, set by the {@link #getRotate()} property. In degrees; CCW is positive. 
    */
   private double elevate = 0;

   /**
    * Get the elevation angle for the 3D graph box. This angle specifies a rotation about the X-axis of the 3D
    * coordinate system, which follows a rotation about the Z-axis as specified by the {@link #getRotate()} property.
    * Together the two rotations determine the orientation of the 3D graph box projected onto the "screen".
    * <p>
    * Get the orientation of this <code>FGraphicNode</code> with respect to its parent. Orientation is expressed as 
    * the angle of rotation between the positive horizontal axes of this node's viewport and its parent viewport. 
    * Positive angles correspond to CCW rotation.
    * 
    * @return The elevation angle in degrees. CCW is positive. Limited to [-90..90].
    */
   public double getElevate() {  return(elevate); }

   /**
    * Set the elevation angle for the 3D graph box (see {@link #getElevate()} for details. If a change is made, {@link 
    * #onNodeModified} is invoked.
    * 
    * @param e The elevation angle, in degrees. Must lie in [-60..60].
    * @return True if successful; false if argument is out-of-range.
    */
   public boolean setElevate(double e)
   {
      if((!Utilities.isWellDefined(e)) || e < -60 || e > 60) return(false);

      if(e != elevate)
      {
         if(doMultiNodeEdit(FGNProperty.ELEVATE, e)) return(true);

         Double old = elevate;
         elevate = e;
         
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.ELEVATE);
            FGNRevEdit.post(this, FGNProperty.ELEVATE, elevate, old);
         }
      }
      return(true);
   }
   
   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok;
      switch(p)
      {
         case LAYOUT : ok = setBackDrop((BackDrop)propValue); break;
         case DEPTH : ok = setDepth((Measure)propValue); break;
         case PSCALE: ok = setProjectionDistanceScale((Integer) propValue); break;
         case ELEVATE: ok = setElevate((Double) propValue); break;
         default: ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value;
      switch(p)
      {
         case LAYOUT : value = getBackDrop(); break;
         case DEPTH : value = getDepth(); break;
         case PSCALE: value = getProjectionDistanceScale(); break;
         case ELEVATE: value = getElevate(); break;
         default: value = super.getPropertyValue(p); break;
      }
      return(value);
   }

   @Override protected void rescaleSelf(double scale, MultiRevEdit undoer)
   {
      Measure old = depth;
      depth = new Measure(old.getValue()*scale, old.getUnits());
      depth = FGraphicModel.getSizeConstraints(getNodeType()).constrain(depth);
      undoer.addPropertyChange(this, FGNProperty.DEPTH, depth, old);
   }

   /**
    * For most attributes, a change in attribute value requires a complete re-rendering of the 3D graph and its 
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

   /** Inserting a data presentation node into the 3D graph may require a re-rendering of the automated legend. */
   @Override protected void onSubordinateNodeInserted(FGraphicNode sub)
   {
      if(sub instanceof FGNPlottable) getLegend().onInsertOrRemovePlottableNode();
      super.onSubordinateNodeInserted(sub);
   }

   /** Removing a data presentation node from the 3D graph may require a re-rendering of the automated legend. */
   @Override protected void onSubordinateNodeRemoved(FGraphicNode sub)
   {
      if(sub instanceof FGNPlottable) getLegend().onInsertOrRemovePlottableNode();
      super.onSubordinateNodeRemoved(sub);
   }

   
   // 
   // Support for style sets
   //

   /** Only the 3D backdrop style is exported in the 3D graph node's style set. */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.LAYOUT, getBackDrop());
      super.putNodeSpecificStyles(styleSet);
   }
   
   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      BackDrop bd = (BackDrop) applied.getCheckedStyle(FGNProperty.LAYOUT, getNodeType(), BackDrop.class);
      if(bd != null && !bd.equals(restore.getStyle(FGNProperty.LAYOUT)))
      {
         this.backDrop = bd;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LAYOUT);
      
      // the super class handles some styles for all graph types
      if(super.applyNodeSpecificStyles(applied, restore))
         changed = true;
      
      return(changed);
   }

   
   //
   // Support for child nodes
   //

   @Override public boolean canInsert(FGNodeType nodeType) 
   { 
      return(nodeType == FGNodeType.LABEL || nodeType == FGNodeType.TEXTBOX || nodeType == FGNodeType.LINE || 
            nodeType == FGNodeType.SHAPE || nodeType == FGNodeType.IMAGE || 
            nodeType == FGNodeType.SCATTER3D || nodeType == FGNodeType.SURFACE); 
   }

   @Override public FGNodeType getNodeType() { return(FGNodeType.GRAPH3D); }

   
   //
   // Support for interactively changing the orientation (rotate, elevate) of the 3D graph
   //
   
   /**
    * Get the "hot spot" region in which an interactive mouse drag gesture may be initiated to alter the orientation 
    * (both rotation and elevation angles) of the 3D graph. The region is anywhere inside the projection of the 3D
    * graph data box onto the 2D canvas (and not too close to the edges).
    * 
    * @return The hot spot region, in root figure coordinates. Returns null if the shape could not be generated for 
    * any reason.
    */
   public Shape getReorientByDragHotSpot()
   {
      return(getLocalToGlobalTransform().createTransformedShape(projector.getBoxOutline(false, -300)));
   }
   
   /**
    * Get the shape that should be stroked during an interactive mouse drag gesture that alters the orientation (both
    * rotation and elevation angles) of the 3D graph. The shape is essentially the full outline of the 3D graph's box
    * backdrop, including the three edges that meet at the front-facing corner of that box.
    * 
    * <p>The rotation angle is adjusted proportionately to the horizontal separation between the drag anchor
    * position and the current drag location, while the elevation angle is adjusted proportionately to the vertical
    * separation. Both are restricted to their allowed ranges. The box outline is computed WRT the adjusted rotation and
    * elevation angles -- so that it represents how the 3D graph's orientation would change if the mouse was released at
    * the current drag location. Similarly, the descriptive cue takes the form "R=..., E=...", reflecting the adjusted
    * values of the rotation and elevation angles, in degrees.</p>
    * 
    * @param p0 The anchor point for the orient-by-drag interaction, in root figure's rendering coordinates. Note that
    * point may be altered by the method.
    * @param p1 The current drag location for the interaction, again in root figure's coordinates. It also may be
    * altered by the method.
    * @param cueBuf If non-null, this buffer will be reinitialized with a descriptive cue reflecting what the values of
    * the rotation and elevation angles would be if the mouse were released at its current location.
    * @return The interactive drag shape, as described above. Will be null if it could not be calculated for any reason.
    */
   public Shape getReorientByDragShape(Point2D p0, Point2D p1, StringBuffer cueBuf)
   {
      // calculate new rotation and elevation angles based on H and V separation, respectively, between the drag anchor
      // and current location. A milli-inch in H separation == 0.1 deg in rotation angle, while a milli-inch in vertical
      // separation = 0.03 deg in elevation angle (because elevation range is more restricted).
      double rotDeg = Utilities.rangeRestrict(-180, 180, getRotate() + (p1.getX()-p0.getX())*0.1);
      double elevDeg = Utilities.rangeRestrict(-60, 60, getElevate() - (p1.getY()-p0.getY())*0.1);
      
      // temporarily adjust the internal projector's rotation and elevation angles and use it to compute the full box
      // outline.
      projector.setRotationAngle(rotDeg);
      projector.setElevationAngle(elevDeg);
      Shape outline = projector.getBoxOutline(true, 0);
      projector.setRotationAngle(getRotate());
      projector.setElevationAngle(getElevate());
      
      // prepare descriptive cue if buffer provided
      if(cueBuf != null)
      {
         cueBuf.setLength(0);
         cueBuf.append("R=").append(Utilities.toString(rotDeg,4,1)).append("deg");
         cueBuf.append(", E=").append(Utilities.toString(elevDeg,4,1)).append("deg");
      }
      
      return(getLocalToGlobalTransform().createTransformedShape(outline));
   }
   
   /**
    * Update the rotation and elevation of the 3D graph at the termination of an interactive mouse drag gesture that
    * re-orients the 3D graph directly on the GUI. The new rotation and elevation angles are computed as in {@link 
    * #getReorientByDragShape}, the 3D graph node's <i>rotate</i> and <i>elevate</i> properties are updated
    * accordingly, {@link #onNodeModified} is invoked to update the graph's rendering, and a reversible edit
    * encapsulating the property changes is posted to the model's undo history. No action is taken if the new rotation
    * and elevation angles cannot be computed for some reason, or if neither value has changed.
    * 
    * @param p0 The anchor point for the orient-by-drag interaction, in root figure's rendering coordinates. Note that
    * point may be altered by the method.
    * @param p1 The drag location when the drag gesture terminated, again in root figure's coordinates. It also may be
    * altered by the method.
    */
   public void executeReorientByDrag(Point2D p0, Point2D p1)
   {
      FGraphicModel fgm = getGraphicModel();
      if(fgm != null && Utilities.isWellDefined(p0) && Utilities.isWellDefined(p1))
      {
         double rotDeg = Utilities.rangeRestrict(-180, 180, getRotate() + (p1.getX()-p0.getX())*0.1);
         rotDeg = Utilities.limitFracDigits(rotDeg, 1);
         double elevDeg = Utilities.rangeRestrict(-60, 60, getElevate() - (p1.getY()-p0.getY())*0.1);
         elevDeg = Utilities.limitFracDigits(elevDeg, 1);
         
         if(rotDeg != getRotate() || elevDeg != getElevate())
         {
            Double rotOld = getRotate();
            Double elevOld = elevate;
            
            setNotificationsEnabled(false);
            try { setRotate(rotDeg); } 
            finally { setNotificationsEnabled(true); }
            elevate = elevDeg;
            
            MultiRevEdit mre = new MultiRevEdit(fgm, "Reorient 3D graph: R=" + Utilities.toString(rotDeg,4,1) + 
                  "deg, E=" + Utilities.toString(elevDeg,4,1) + "deg");
            mre.addPropertyChange(this, FGNProperty.ROTATE, rotDeg, rotOld);
            mre.addPropertyChange(this, FGNProperty.ELEVATE, elevate, elevOld);
            
            onNodeModified(null);
            fgm.postReversibleEdit(mre);
         }
      }
   }
   
   //
   // Focusable/Renderable support
   //

   /** 
    * The 3D graph node CAN be resized interactively, although the interaction is unique because the resize shape is
    * not a rectangle, but a six-sided polygon that (at most orientations, but not all) outlines the 3D box backdrop 
    * (regardless if the node is currently configured in that backdrop style).
    */
   @Override public boolean canResize() { return(true); }

   /**
    * For a 3D graph node, the resize shape is a six-sided closed polygon outlining six of the 12 edges of the node's 
    * 3D box backdrop (regardless if the current backdrop style is one of the boxed styles). The three edges that
    * intersect at the front corner and the 3 edges that intersect at the back corner of the box are excluded. At most
    * orientations (but not all!), this polygon tightly bounds the 3D box backdrop. To avoid drawing over the boxed
    * backdrop edges, the outline returned is expanded outward by roughly 75 milli-inches.
    */
   @Override public Shape getResizeShape()
   {
      return(getLocalToGlobalTransform().createTransformedShape(projector.getBoxOutline(false, 75)));
   }

   /**
    * To resize a 3D graph node, the user must "grab" one of the six edges or six corners of the graph's 3D box that lie
    * along resize outline that {@link #getResizeShape()} returns. If the "grab point" is near enough to any of the
    * corners, {@link ResizeAnchor#ANY} is returned, and the drag resize interaction should change all three dimensions
    * of the 3D graph node by an equal amount determined by the separation between the initial and current locations of 
    * the drag cursor. Otherwise, the edges are checked. If it is near enough to either of the two front-facing edges in
    * Z, {@link ResizeAnchor#NORTH} is returned, and the drag resize interaction should only change the <i>depth</i> of 
    * the 3D graph. If it is near enough to either of the two front-facing edges in X, {@link ResizeAnchor#NORTHWEST} 
    * is returned, and the resize interaction should only change the <i>width</i>. Finally, if it is near enough to the 
    * front-facing edges in Y, then {@link ResizeAnchor#NORTHEAST} is returned, and the resize interaction should only 
    * change the <i>height</i>.
    * 
    * <p>If the "grab point" is not close enough to any of these, or if the resize outline cannot be calculated for any 
    * reason, then null is returned.</p>
    */
   @Override public ResizeAnchor getResizeAnchor(Point2D p, double tol)
   {
      // transform test point to 3D graph's local coordinates. 
      AffineTransform at = getLocalToGlobalTransform();
      try
      {
         AffineTransform invAT = at.createInverse();
         invAT.transform(p, p);
      }
      catch(NoninvertibleTransformException nte)
      {
         return(null);
      }
      
      ResizeAnchor ra = null;
      if(projector.isNearCorner(Projector.Corner.XFRONTYZBACK, p, tol) ||
            projector.isNearCorner(Projector.Corner.XYFRONTZBACK, p, tol) ||
            projector.isNearCorner(Projector.Corner.XZFRONTYBACK, p, tol) ||
            projector.isNearCorner(Projector.Corner.YFRONTXZBACK, p, tol) ||
            projector.isNearCorner(Projector.Corner.YZFRONTXBACK, p, tol) ||
            projector.isNearCorner(Projector.Corner.ZFRONTXYBACK, p, tol))
         ra = ResizeAnchor.ANY;
      else if(projector.isNearEdge(Projector.Edge.XFRONTYBACK, p, tol) ||
            projector.isNearEdge(Projector.Edge.XFRONTZBACK, p, tol))
         ra = ResizeAnchor.NORTHWEST;
      else if(projector.isNearEdge(Projector.Edge.YFRONTZBACK, p, tol) ||
            projector.isNearEdge(Projector.Edge.XBACKYFRONT, p, tol))
         ra = ResizeAnchor.NORTHEAST;
      else if(projector.isNearEdge(Projector.Edge.XBACKZFRONT, p, tol) ||
            projector.isNearEdge(Projector.Edge.YBACKZFRONT, p, tol))
         ra = ResizeAnchor.NORTH;
      return(ra);
   }

   /**
    * Compute the drag resize shape while interactively resizing the 3D graph.
    * 
    * <p>The resize anchor's identity determines which dimensions are affected -- see {@link #getResizeAnchor} for
    * details. The distance between the initial and current drag cursor locations determines the magnitude of the change
    * in the target dimension(s). If the current drag location is further from the graph's origin than the initial 
    * location, the resize shape indicates an increase in size; else a decrease, down to a minimum value of 1-in.
    * 
    * <p>The method returns what the resize outline would be if the target dimension(s) was(were) updated accordingly, 
    * and it prepares the cue string ("W=..., H=..., D=...") to reflect the change(s).</p>
    */
   @Override public Shape getDragResizeShape(ResizeAnchor anchor, Point2D p0, Point2D p1, StringBuffer cueBuf)
   {
      // transform the initial and current drag locations into the graph's local coordinate system, in which the origin 
      // is at the center of projection.
      AffineTransform at = getLocalToGlobalTransform();
      try
      {
         AffineTransform invAT = at.createInverse();
         invAT.transform(p0, p0);
         invAT.transform(p1,  p1);
      }
      catch(NoninvertibleTransformException nte)
      {
         return(null);
      }
      
      // compute the dimension delta. If current drag location is further from the origin than the anchor, then an
      // expansion is called for, else a reduction
      double delta = p1.distance(p0);
      if(p0.distanceSq(0,0) > p1.distanceSq(0,0)) delta = -delta;
      
      // update the dimension(s) implied by the identity of the resize anchor, both in milli-inches and in the current 
      // units of measurement. Since the dimension cannot be in % or user units, calculations are simple. Note that the
      // minimum allowed dimension for a 3D graph is 1 inch.
      Measure.Constraints c = FGraphicModel.getSizeConstraints(getNodeType());
      Measure mw = getWidth();
      Measure mh = getHeight();
      Measure md = getDepth();
      double w = mw.toMilliInches();
      double h = mh.toMilliInches();
      double d = md.toMilliInches();
      if(anchor == ResizeAnchor.NORTHWEST || anchor == ResizeAnchor.ANY)
      {
         w += delta;
         if(w < 1000) w = 1000;
         mw = Measure.getConstrainedRealMeasure(w, mw.getUnits(), c);
      }
      if(anchor == ResizeAnchor.NORTHEAST || anchor == ResizeAnchor.ANY)
      {
         h += delta;
         if(h < 1000) h = 1000;
         mh = Measure.getConstrainedRealMeasure(h, mh.getUnits(), c);
      }
      if(anchor == ResizeAnchor.NORTH || anchor == ResizeAnchor.ANY)
      {
         d += delta;
         if(d < 1000) d = 1000;
         md = Measure.getConstrainedRealMeasure(h, md.getUnits(), c);
      }
      
      // temporarily update internal projector and use it to calculate resize outline for current drag state
      projector.setProjectionGeometry(w, h, d);
      Shape outline = getResizeShape();
      projector.setProjectionGeometry(getWidth().toMilliInches(), getHeight().toMilliInches(), 
            getDepth().toMilliInches());
      
      // prepare cue
      if(cueBuf != null)
      {
         cueBuf.setLength(0);
         cueBuf.append("W=").append(mw.toString());
         cueBuf.append(", H=").append(mh.toString());
         cueBuf.append(", D=").append(md.toString());
      }

      return(outline);
   }

   @Override public void executeResize(ResizeAnchor anchor, Point2D p0, Point2D p1)
   {
      FGraphicModel fgm = getGraphicModel();
      if(fgm == null) return;
      
      // transform the drag anchor and terminal locations into the graph's local coordinate system, in which the origin 
      // is at the center of projection. If we can't do this, then we cannot resize the graph.
      AffineTransform at = getLocalToGlobalTransform();
      try
      {
         AffineTransform invAT = at.createInverse();
         invAT.transform(p0, p0);
         invAT.transform(p1,  p1);
      }
      catch(NoninvertibleTransformException nte)
      {
         return;
      }
      
      // compute the dimension delta. If final drag location is further from the origin than the anchor, then expand;
      // else contract.
      double delta = p1.distance(p0);
      if(p0.distanceSq(0,0) > p1.distanceSq(0,0)) delta = -delta;
      
      // update the dimension(s) implied by the identity of the resize anchor, applying the 3D graph's size constraints
      Measure.Constraints c = FGraphicModel.getSizeConstraints(getNodeType());
      double dim;
      Measure mw = null;
      Measure mh = null;
      Measure md = null;
      if(anchor == ResizeAnchor.NORTHWEST || anchor == ResizeAnchor.ANY)
      {
         mw = getWidth();
         dim = mw.toMilliInches() + delta;
         mw = Measure.getConstrainedRealMeasure(dim, mw.getUnits(), c);
      }
      if(anchor == ResizeAnchor.NORTHEAST || anchor == ResizeAnchor.ANY)
      {
         mh = getHeight();
         dim = mh.toMilliInches() + delta;
         mh = Measure.getConstrainedRealMeasure(dim, mh.getUnits(), c);
      }
      if(anchor == ResizeAnchor.NORTH || anchor == ResizeAnchor.ANY)
      {
         md = getDepth();
         dim = md.toMilliInches() + delta;
         md = Measure.getConstrainedRealMeasure(dim, md.getUnits(), c);
      }
      
      switch(anchor)
      {
      case NORTHWEST :
         setWidth(mw);
         break;
      case NORTHEAST :
         setHeight(mh);
         break;
      case NORTH :
         setDepth(md);
         break;
      default :  // all 3 dimensions changed
         {
            Measure mwOld = getWidth();
            Measure mhOld = getHeight();
            Measure mdOld = depth;
            
            setNotificationsEnabled(false);
            try { setWidth(md); setHeight(mh); } 
            finally { setNotificationsEnabled(true); }
            depth = md;
            
            MultiRevEdit mre = new MultiRevEdit(fgm, "Resize 3D graph to: W=" + getWidth() + 
                  ", H=" + getHeight() + ", D=" + depth);
            mre.addPropertyChange(this, FGNProperty.WIDTH, getWidth(), mwOld);
            mre.addPropertyChange(this, FGNProperty.HEIGHT, getHeight(), mhOld);
            mre.addPropertyChange(this, FGNProperty.DEPTH, depth, mdOld);
            
            onNodeModified(null);
            fgm.postReversibleEdit(mre);
         }
         break;
      }
   }


   /**
    * The returned transform simply translates the origin of the graph's 2D viewport so that it is in the coordinate
    * system of its parent figure. However, the origin is NOT given by the graph's X,Y coordinates; instead, it is the
    * bottom-left corner of the rectangle that tightly bounds the 3D "data box" once projected onto the 2D plane. For
    * more details, see {@link #getViewport()}.
    */
   @Override public AffineTransform getLocalToParentTransform()
   {
      AffineTransform identity = new AffineTransform();

      // make sure we've computed the rectangle tightly bounding the 3D data box using the projector. Its X,Y location
      // will have negative coordinates that are effectively the offset from the graph's nominal location to the
      // bottom left corner of the 3D graph's 2D viewport.
      update2DProjection();
      
      // get 3D graph's location with respect to the parent viewport
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(identity);
      Rectangle2D rect = parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      if(rect == null) return(identity);

      // the local origin is at the bottom-left corner of that bounding rect. Translate to parent viewport.
      // translate origin so that it is in coord system of parent viewport
      return(AffineTransform.getTranslateInstance(
            rect.getX() + rBoundsSelf.getX(), rect.getY() + rBoundsSelf.getY()));
   }

   /**
    * The 3D graph node does not fit well into the rendering infrastructure of {@link FGraphicNode}, which was 
    * originally designed only for 2D. All 3D data presentation nodes and graph component nodes render directly into
    * the graph's viewport using 3D-to-2D coordinate transformations handled by the projector object -- see {@link
    * #get2DProjection()}; its center of projection is located at the graph's origin <i>(Xo,Yo)</i>. However, to 
    * locate basic 2D objects like text labels and line segments as children of the 3D graph, we need to define a
    * 2D viewport. We chose to use the rectangle that tightly bounds the 2D projection of the graph's 3D "data box"
    * onto the 2D plane. The bottom-left corner of this rectangle is the origin in the graph's 2D viewport; this 2D
    * viewport is never rotated WRT the parent figure, even though the graph data box can be re-oriented by changing
    * its rotation and elevation angles (these are 3D rotations, not rotations in the plane of the figure).
    * 
    * <p>The transform returned by {@link #getLocalToGlobalTransform()} must be consistent with this 2D viewport in
    * order to mesh correctly with the rendering infrastructure. Also, to stay consistent, the origin is translated to
    * the BL corner of that bounding rectangle prior to rendering all component and child nodes of this 3D graph. For
    * all of this to work correctly, the 3D-to-2D projector is configured in {@link #update2DProjection()} with its 
    * origin initially at (0,0). Then it is used to compute the tight bounding rectangle, the location (Rx,Ry) of which 
    * will have negative x and y coordinates. The projector's origin is then set to (-Rx, -Ry), and all rendering
    * under the 3D graph node starts with the origin at (Xo+Rx, Yo+Ry).</p>
    */
   @Override public FViewport2D getViewport() 
   { 
      if(rBoundsSelf == null) update2DProjection();
      return(new FViewport2D(rBoundsSelf.getWidth(), rBoundsSelf.getHeight())); 
   }

   /**
    * Returns the rectangle that would tightly bound the backdrop in the {@link BackDrop#BOX3D} style -- regardless
    * what the current backdrop style is. This is considered a "minimum" bounds for the 3D graph and its contents.
    */
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      // always update the 3D-to-2D projection geometry whenever this method is called, to ensure any property changes
      // have been accounted for. This also computes the render bounds
      update2DProjection();
      
      // rectangle returned must have its BL corner at (0,0) -- since it's specified WRT the node's local 2D viewport
      Rectangle2D rOut = new Rectangle2D.Double(0, 0, rBoundsSelf.getWidth(), rBoundsSelf.getHeight());
      
      // add in rectangle bounding the semi-automated title, if rendered
      TextBoxPainter p = prepareAutoTitlePainter();
      if(p != null)
      {
         p.updateFontRenderContext(g2d);
         p.invalidateBounds();
         rOut.add(p.getBounds2D(null));
      }
      return(rOut);
   }

   /** A 3D graph is always rendered, unless its backdrop style is {@link BackDrop#HIDDEN} AND it has no children. */
   @Override protected boolean isRendered() 
   { 
      return(backDrop != BackDrop.HIDDEN || getChildCount() > 0); 
   }

   /**
    * The rendering of the 3D graph node is handled mostly by its components nodes, which give the user more control
    * over the appearance of various elements of the graph backdrop. Which components are actually rendered depends on
    * the current backdrop style. All components and all data presentation child nodes are drawn in the rendering
    * coordinate system of the 3D graph, which handles the projection of all 3D vertices onto the 2D projection plane.
    * 
    * <p>The three backplanes are drawn first, then the gridlines (if any) that appear within these backplanes, and then
    * the three axes (including any child tick mark sets), and finally the child nodes, in order. Finally, if the
    * backdrop style is {@link BackDrop#BOX3D}, the front three edges (which appear to project out of the screen
    * toward the viewer) are stroked to "complete the cube". Unlike with its 2D counterpart, no clipping occurs.</p>
    */
   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(!(isRendered() && needsRendering(task)))
         return(true);

      // get location of graph's origin with respect to the parent viewport; if it is ill-defined, render nothing.
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(true);
      Point2D pOrigin = parentVP.toMilliInches(getX(), getY());
      if(pOrigin == null) return(true);

      // we should never need to recalc the bounding rect for our 2D viewport, but just in case
      if(rBoundsSelf == null) update2DProjection();
      
      // render 3D graph in a copy of the graphics context, so we do not alter the original. We first translate to the
      // graph's origin, since all drawing is done relative to that point.
      Graphics2D g2dCopy = (Graphics2D) g2d.create();  
      try
      {
         // transform graphics context to the BL corner of graph's 2D viewport.
         g2dCopy.translate(pOrigin.getX() + rBoundsSelf.getX(), pOrigin.getY() + rBoundsSelf.getY());

         // render all component nodes first...
         for(int i=0; i<getComponentNodeCount(); i++) 
         {
            if(!getComponentNodeAt(i).render(g2dCopy, task))
               return(false);
         }
         
         // in boxed backdrop modes, any 3D scatter plots may render projections onto the backplanes. These must be
         // rendered before any children rendered inside the 3D box.
         if(backDrop==BackDrop.BOX3D || backDrop==BackDrop.OPENBOX3D || backDrop==BackDrop.XYPLANE)
         {
            for(int i=0; i<getChildCount(); i++)
            {
               FGraphicNode n = getChildAt(i);
               if(n instanceof Scatter3DNode) ((Scatter3DNode) n).renderProjections(g2dCopy, task);
            }
         }
         
         // render any child nodes
         for(int i=0; i<getChildCount(); i++)
         {
            FGraphicNode n = getChildAt(i);
            if(!n.render(g2dCopy, task))
               return(false);
         }
         
         // render the forward projecting edges in the box3d backdrop style
         if(backDrop == BackDrop.BOX3D)
         {
            g2dCopy.setColor(getStrokeColor());
            g2dCopy.setStroke(getStroke(0));
            g2dCopy.draw(projector.getBoxFrontPath());
         }
         
         if(!renderAutoTitle(g2dCopy, task)) return(false);
      }
      finally 
      { 
         if(g2dCopy != null) g2dCopy.dispose(); 
      }
      return(true);
   }


   //
   // PSTransformable implementation
   //
   
   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      // the 3D graph viewport is the rectangle that tightly bounds the 3D data box. The graph's origin (X,Y) is the 
      // center point of that rectangle rather than the BL corner (as is the case in 2D). We need that rectangle to find
      // its BL corner with respect to the parent viewport. If we can't calculate this for any reason, render nothing.
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;
      Point2D pOrigin = parentVP.toMilliInches(getX(), getY());
      if(pOrigin == null) return;
      if(rBoundsSelf == null) update2DProjection();
      pOrigin.setLocation(pOrigin.getX()+rBoundsSelf.getX(), pOrigin.getY()+rBoundsSelf.getY());
      
      // start the element and establish a new viewport with origin as calculated above. We want a  NON-CLIPPING 
      // viewport, so we don't need to specify W,H of bounding box
      psDoc.startElement(this);
      psDoc.setViewport(pOrigin, 0, 0, 0, false);

      // render all component nodes first
      for(int i=0; i<getComponentNodeCount(); i++) getComponentNodeAt(i).toPostscript(psDoc);
      
      // render any projections of 3D scatter plots onto backplanes, if applicable
      if(backDrop==BackDrop.BOX3D || backDrop==BackDrop.OPENBOX3D || backDrop==BackDrop.XYPLANE)
      {
         for(int i=0; i<getChildCount(); i++)
         {
            FGraphicNode n = getChildAt(i);
            if(n instanceof Scatter3DNode) ((Scatter3DNode) n).renderProjectionsToPostscript(psDoc);
         }
      }
      
      // render any child nodes (data presentation nodes)
      for(int i=0; i<getChildCount(); i++) getChildAt(i).toPostscript(psDoc);
      
      // render the forward projecting edges in the box3d backdrop style
      if(backDrop == BackDrop.BOX3D)
      {
         List<Point2D> pts = projector.getBoxFrontPathVertices();
         psDoc.renderPolyline(pts.toArray(new Point2D[0]), null, 0, null, null, false);
      }
      
      autoTitleToPostscript(psDoc);
      
      // end the element
      psDoc.endElement();
   }
   


   // 
   // Infrastructure for 3D-to-2D projection and backdrop rendering
   //
   
   /** The current 3D-to-2D projection, as defined by the 3D graph node's geometry and axis ranges. */
   private Projector projector = new Projector();
   
   /** 
    * Rectangle that tightly bounds the graph's 3D box projected onto the 2D plane, as computed by the 3D-to-2D 
    * projector when its origin is at (0,0). Thus, the rectangle's (X,Y) coordinates are negative and indicate the 
    * offset from the graph's nominal location to the bottom-left corner of this bounding rectangle.
    */
   private Rectangle2D rBoundsSelf = null;

   /**
    * Update the definition of the 3D-to-2D projection for this graph node. Call this method whenever any node property
    * affecting the projection is modified.
    */
   private void update2DProjection()
   {
      projector.setProjectionGeometry(distanceScale, 
            getWidth().toMilliInches(), getHeight().toMilliInches(), getDepth().toMilliInches(),
            getRotate(), getElevate());
      
      Axis3DNode xAxis = getAxis(Axis.X);
      Axis3DNode yAxis = getAxis(Axis.Y);
      Axis3DNode zAxis = getAxis(Axis.Z);
      projector.setAxisRanges(
            xAxis.getRangeMin(), xAxis.getRangeMax(), xAxis.isLogarithmic(), 
            yAxis.getRangeMin(), yAxis.getRangeMax(), yAxis.isLogarithmic(), 
            zAxis.getRangeMin(), zAxis.getRangeMax(), zAxis.isLogarithmic());
      
      // with the projector origin at (0,0), compute the rectangle tightly bounding the 3D data box. This is considered
      // the render bounds for the 3D graph itself
      projector.setXOrigin(0);
      projector.setYOrigin(0);
      rBoundsSelf = projector.getBounds2D(null);
      
      // now offset projector's origin so that the tight bounding rectangle will have its BL corner at (0,0). This
      // rectangle constitutes the local 2D viewport for the 3D graph!
      projector.setXOrigin(-rBoundsSelf.getX());
      projector.setYOrigin(-rBoundsSelf.getY());
   }
   
   /**
    * Get this 3D graph node's current 3D-to-2D projection, as defined by the graph's geometry and axis ranges. The
    * graph's component nodes need this to compute locations in the graph's rendering coordinate system.
    * 
    * @return A {@link Projector} object that projects 3D points in the graph's "native" coordinate system to the 2D
    * plane on which the 3D graph is rendered.
    */
   Projector get2DProjection() { return(new Projector(projector)); }
   
   /** 
    * Is the specified backplane drawn in the 3D graph node's current backdrop? All 3 backplanes are drawn for the two
    * "boxed" backdrop styles, while only the XY backplane is drawn in the {@link BackDrop#XYPLANE} style.
    * @param s The backplane side; if null, the XY backplane is assumed
    * @return True if backplane should be drawn; else false. (Note that the relevant backplane may still make no marks 
    * on the canvas, depending on the properties of the backplane node itself.)
    */
   boolean isBackPlaneDrawn(Side s)
   {
      if(s == null) s = Side.XY;
      return(backDrop==BackDrop.BOX3D || backDrop==BackDrop.OPENBOX3D || (backDrop==BackDrop.XYPLANE && s==Side.XY));
   }
   
   /**
    * Get the 4-sided polygon that outlines the specified backplane in the "boxed" backdrop styles.
    * @param s The backplane side; if null, the XY backplane outline is returned.
    * @return The polygon outlining the specified backplane. Note that backplane is not rendered if the 3D backdrop is
    * not one of the two "boxed" styles.
    */
   Shape getBackPlaneShape(Side s)
   {
      Shape shape;
      if(s == Side.XZ) shape = projector.getXZBackplaneShape();
      else if(s == Side.YZ) shape = projector.getYZBackplaneShape();
      else shape = projector.getXYBackplaneShape();
      return(shape);
   }
   
   /**
    * Get the four vertices defining the polygon that outlines the specified backplane in the "boxed" backdrop styles.
    * @param s The backplane side; if null, the vertices for the XY backplane outline are returned.
    * @return Vertices of the polygon outlining the specified backplane, in milli-inches WRT the graph's rendering
    * coordinate system. The vertices should be connected in order and the path closed to render the backplane polygon.
    */
   List<Point2D> getBackPlaneVertices(Side s)
   {
      List<Point2D> out;
      if(s == Side.XZ) out = projector.getXZBackplaneVertices();
      else if(s == Side.YZ) out = projector.getYZBackplaneVertices();
      else out = projector.getXYBackplaneVertices();
      return(out);
   }
   
   /**
    * Are there grid lines perpendicular to the specified axis in this 3D graph? Grid lines may be drawn for the {@link 
    * BackDrop#BOX3D}, {@link BackDrop#OPENBOX3D} and {@link BackDrop#XYPLANE} backdrop styles, although only X/Y grid
    * lines in the latter case. Of course, if the specified axis lacks a tick set, there are no grid lines.
    * @param a Axis identifier; if null, X axis is assumed.
    * @return True if grid lines should be rendered perpendicular to that axis; else false.
    */
   boolean hasGridLines(Axis a)
   {
      if(a == null) a = Axis.X;
      boolean doGrid = (backDrop==BackDrop.OPENBOX3D) || (backDrop==BackDrop.BOX3D) ||
            (backDrop==BackDrop.XYPLANE && a != Axis.Z);
      return(doGrid && getAxis(a).hasMajorTicks());
   }
   
   /**
    * Get the path that describes the grid lines perpendicular to the specified axis in the 3D graph. The path object
    * is specified in the rendering coordinate system of the 3D graph and is ready to be stroked IAW the associated 3D 
    * grid line component's stroke properties. Note that if the backdrop style omits grid lines, or if the specified
    * axis lacks a tick set (which determines the grid line locations), then the grid line path is empty.
    * @param a The axis perpendicular to the requested grid lines.
    * @return Path describing the grid lines, or empty if there are no grid lines for the reasons indicated.
    */
   GeneralPath getGridLinePath(Axis a)
   {
      List<Point2D> pts = getGridLineVertices(a);      
      GeneralPath gp = new GeneralPath();
      if(pts==null || pts.isEmpty()) return(gp);

      boolean isMoveTo = true;
      for(Point2D p : pts)
      {
         if(Utilities.isWellDefined(p))
         {
            if(isMoveTo) gp.moveTo(p.getX(), p.getY());
            else gp.lineTo(p.getX(), p.getY());
            isMoveTo = false;
         } else
            isMoveTo = true;
      }
      return(gp);
   }

   /**
    * Get a list of points that define a path representing the grid lines perpendicular to the specified axis in the 3D
    * graph. The grid can be rendered by connecting the sequence of well-defined points together, inserting a "gap" in
    * the path each time an ill-defined point is encountered. If a grid line lies at either end of the specified axis's
    * range, then that grid line is omitted since it would be drawn along the edge of the 3D boxed backdrop (grid lines
    * are only drawn in the "boxed" and "XY backplane only" backdrop styles).
    * <p>If there are no such grid lines for whatever reason (backdrop style lacks grid lines, axis does not have
    * a defined tick set), the list of points will be null or empty.</p>
    * @param a The axis perpendicular to the requested grid lines. If null, X axis is assumed.
    * @return The points defining the path that traces the grid lines requested. Null or empty if there are no such
    * grid lines.
    */
   List<Point2D> getGridLineVertices(Axis a)
   {
      if(a == null) a = Axis.X;
      boolean doGrid = (backDrop==BackDrop.OPENBOX3D) || (backDrop==BackDrop.BOX3D) ||
               (backDrop==BackDrop.XYPLANE && a != Axis.Z);
      if(!doGrid) return(null);
      
      double[] ticks = getAxis(a).getMajorTicks(true);
      
      List<Point2D> out;
      if(a == Axis.X) out = projector.getXGridLineVertices(ticks, backDrop==BackDrop.XYPLANE);
      else if(a == Axis.Y) out = projector.getYGridLineVertices(ticks, backDrop==BackDrop.XYPLANE);
      else out = projector.getZGridLineVertices(ticks);
      return(out);
   }
   
   /** 
    * Override ensures that the cloned node's internal 3D-to-2D projector is completely independent of the projector
    * for this 3D graph node.
    */
   @Override protected Graph3DNode clone() throws CloneNotSupportedException
   {
      Graph3DNode copy = (Graph3DNode) super.clone();
      copy.projector = new Projector();
      copy.rBoundsSelf = null;
      copy.update2DProjection();
      return(copy);
   }
}
