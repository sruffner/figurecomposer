package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import com.srscicomp.common.g2dviewer.RenderTask;

/**
 * <b>Grid3DNode</b> is a required component of the 3D graph object, {@link Graph3DNode}. There is one such node
 * for each of the three sets of grid lines: constant X grid lines on the XY and XZ backplanes; constant Y grid lines
 * on the XY and YZ planes; and constant Z grid lines on the XZ and YZ planes. A 3D grid line node renders itself IAW 
 * the state of the parent 3D graph and its own stroke properties. It is not a focusable object but rather an integral 
 * component of the 3D graph.</p>
 * 
 * @author sruffner
 */
public class Grid3DNode extends FGraphicNode implements Cloneable
{
   /**
    * Construct a 3D grid line node. All stroke properties are implicit initially.
    * @param which Indicates the axis perpendicular to the grid lines that are governed by this 3D grid line node.
    */
   public Grid3DNode(Graph3DNode.Axis which)
   {
      super(HASSTROKEATTRS|HASSTRKPATNATTR);
      perpAxis = which == null ? Graph3DNode.Axis.X : which;
   }

   /** Indicates which 3D graph axis perpendicular to the grid lines governed by this 3D grid line node. */
   private final Graph3DNode.Axis perpAxis;
   
   /**
    * Get the identity of the 3D graph axis perpendicular to the grid lines governed by 3D grid line node.
    * @return The relevant axis.
    */
   public Graph3DNode.Axis getPerpendicularAxis() { return(perpAxis); }
   
   /**
    * Return a reference to the 3D graph object containing this 3D grid line node.
    * @return The parent 3D graph, or null if there is no parent.
    */
   final Graph3DNode getParentGraph3D()
   {
      FGraphicNode p = getParent();
      if(p == null) return(null);
      assert(p instanceof Graph3DNode);
      return((Graph3DNode)p);
   }
   
   //
   // Support for child nodes -- none permitted!
   //
   @Override public FGNodeType getNodeType() { return(FGNodeType.GRID3D); }
   @Override public boolean isLeaf() { return(true); }
   @Override public boolean canInsert(FGNodeType nodeType) { return(false); }
   
   /** A 3D grid line node is an intrinsic component of its parent {@link Graph3DNode}.*/
   @Override public boolean isComponentNode() { return(true); }

   
   //
   // Properties -- None except stroke properties handled by the base class
   //
   
   /**
    * Grid lines are rendered only for the certain backdrop styles of the parent {@link Graph3DNode}. This override
    * avoids unnecessary rendering updates when the grid lines are not drawn.
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      Graph3DNode g3 = getParentGraph3D();
      if(g3 != null && g3.hasGridLines(perpAxis)) super.onNodeModified(hint);
      else model.onChange(this, 0, false, null);
   }

   
   // 
   // Support for style sets -- no node-specific styles
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   //
   // Focusable/Renderable/PSTransformable support
   //

   /** Returns null always because a 3D grid line node cannot be selected interactively. */
   @Override protected FGraphicNode hitTest(Point2D p) { return(null); }
   /** Returns null always because a 3D grid line node is a non-focusable component of its parent 3D graph. */
   @Override public Shape getFocusShape(Graphics2D g2d) { return(null); }
   
   /** No action taken, since a 3D grid line node maintains no internal rendering resources. */
   @Override protected void releaseRenderResourcesForSelf() {}
   /** A 3D grid line node renders directly into the parent 3D graph, so the identity transform is returned. */
   @Override public AffineTransform getLocalToParentTransform() { return(new AffineTransform()); }
   /** A 3D grid line node renders directly into the parent 3D graph, so this method returns the parent viewport. */
   @Override public FViewport2D getViewport() { return(null); }
   
   /** 
    * A 3D grid line node is not rendered under certain circumstances: (a) the parent 3D graph's current backdrop style 
    * omits the grid lines; (b) no tick set is defined for the associated axis; or (b) the grid lines are not stroked.
    */
   @Override protected boolean isRendered()
   {
      Graph3DNode g3 = getParentGraph3D();
      return(g3 != null && g3.hasGridLines(perpAxis) && isStroked());
   }
   
   /** 
    * Returns an empty rectangle if 3D grid line node is not rendered; else returns the rectangle bounding its parent
    * 3D graph (for simplicity's sake).
    */
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      Rectangle2D r = null;
      if(isRendered())
      {
         Graph3DNode g3 = getParentGraph3D();
         if(g3 != null) r = g3.getRenderBoundsForSelf(g2d, forceRecalc);
      }
      return(r == null ? new Rectangle2D.Double() : r);
   }
   
   @Override public boolean render(Graphics2D g2d, RenderTask task) 
   { 
      if(!(isRendered() && needsRendering(task))) return(true);
      
      Graph3DNode g3 = getParentGraph3D();
      if(g3 == null) return(true);
      
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         g2dCopy.setColor(getStrokeColor());
         g2dCopy.setStroke(getStroke(0));
         g2dCopy.draw(g3.getGridLinePath(perpAxis));
      }
      finally 
      { 
         if(g2dCopy != null) g2dCopy.dispose(); 
      }
      
      return((task == null) ? true : task.updateProgress());

   }
   
   @Override public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException 
   { 
      Graph3DNode g3 = getParentGraph3D();
      if(g3 == null || !isRendered()) return;
      List<Point2D> verts = g3.getGridLineVertices(perpAxis);
      if(verts == null || verts.isEmpty()) return;
      
      psDoc.startElement(this);
      psDoc.renderPolyline(verts.toArray(new Point2D[0]), null, 0, null, null, false);
      psDoc.endElement();
   }
}
