package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import com.srscicomp.common.g2dviewer.RenderTask;

/**
 * <b>BackPlane3DNode</b> is a required component of the 3D graph object, {@link Graph3DNode}. There is one such node
 * for each of the three "backplanes" or "back sides" of the 3D graph box: the XY, XZ, and YZ backplanes. The backplane
 * node renders itself IAW the state of the parent 3D graph and its own fill and stroke properties. It is not a 
 * focusable object but rather an integral component of the 3D graph.</p>
 * 
 * @author sruffner
 */
public class BackPlane3DNode extends FGraphicNode implements Cloneable
{
   /**
    * Construct a backplane node. Initially the background fill is transparent, and all stroke properties are implicit.
    * @param which The back side of the parent 3D graph that is encapsulated by this backplane node.
    */
   public BackPlane3DNode(Graph3DNode.Side which)
   {
      super(HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR);
      side = which == null ? Graph3DNode.Side.XY : which;
      setFillColor(new Color(0,0,0,0));
   }

   /** Indicates which back side of the parent 3D graph is represented by this backplane node. */
   private final Graph3DNode.Side side;
   
   /**
    * Get the back side of the 3D graph box represented by this backplane node.
    * @return The backplane side.
    */
   public Graph3DNode.Side getBackplaneSide() { return(side); }
   
   /**
    * Return a reference to the 3D graph object containing this backplane node.
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
   @Override public FGNodeType getNodeType() { return(FGNodeType.BACK3D); }
   @Override public boolean isLeaf() { return(true); }
   @Override public boolean canInsert(FGNodeType nodeType) { return(false); }
   
   /** A backplane node is an intrinsic component of its parent {@link Graph3DNode}.*/
   @Override public boolean isComponentNode() { return(true); }

   
   //
   // Properties -- none beyond what's handled by base class
   //
   
   /**
    * A backplane node is rendered only for selected backdrop styles of the parent {@link Graph3DNode}. This override
    * avoids unnecessary rendering updates when the backplane is not drawn.
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      Graph3DNode g3 = getParentGraph3D();
      if(g3 != null && g3.isBackPlaneDrawn(side)) super.onNodeModified(hint);
      else model.onChange(this, 0, false, null);
   }

   
   // 
   // Support for style sets -- no node-specific styles
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   //
   // Focusable/Renderable/PSTransformable support
   //

   /** Returns null always because the backplane node cannot be selected interactively. */
   @Override protected FGraphicNode hitTest(Point2D p) { return(null); }
   /** Returns null always because the backplane node is a non-focusable component of its parent 3D graph. */
   @Override public Shape getFocusShape(Graphics2D g2d) { return(null); }

   /** No action taken, since the backplane node maintains no internal rendering resources. */
   @Override protected void releaseRenderResourcesForSelf() {}
   /** The backplane node renders directly into the parent 3D graph, so the identity transform is returned. */
   @Override public AffineTransform getLocalToParentTransform() { return(new AffineTransform()); }
   /** The backplane node renders directly into the parent 3D graph, so this method returns the parent viewport. */
   @Override public FViewport2D getViewport() { return(getParentViewport()); }
   
   /** 
    * A backplane node is not rendered under certain circumstances: (a) the parent 3D graph's backdrop style omits the 
    * backplanes; or (b) the backplane is neither stroked nor filled.
    */
   @Override protected boolean isRendered()
   {
      Graph3DNode g3 = getParentGraph3D();
      boolean ok = g3 != null && g3.isBackPlaneDrawn(side);
      if(ok) ok = isStroked() || (getFillColor().getAlpha() != 0);
      return(ok);
   }
   
   /** 
    * Returns an empty rectangle if backplane is not rendered; else returns the rectangle bounding it, grown by a
    * half stroke-width in all directions.
    */
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      Rectangle2D r = null;
      if(isRendered())
      {
         Graph3DNode g3 = getParentGraph3D();
         if(g3 != null) r = g3.getBackPlaneShape(side).getBounds2D();
      }
      if(r == null) return(new Rectangle2D.Double());
      
      double hsw = (isStroked() ? getStrokeWidth() : 0)/2.0;
      r.setFrame(r.getX() - hsw, r.getY() - hsw, r.getWidth() + hsw * 2.0, r.getHeight() + hsw * 2.0);
      return(r);
   }
   
   public boolean render(Graphics2D g2d, RenderTask task) 
   { 
      if(!(isRendered() && needsRendering(task))) return(true);
      
      Graph3DNode g3 = getParentGraph3D();
      if(g3 == null) return(true);
      
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         Shape s = g3.getBackPlaneShape(side);
         g2dCopy.setColor(getFillColor());
         g2dCopy.fill(s);
         if(isStroked())
         {
            g2dCopy.setColor(getStrokeColor());
            g2dCopy.setStroke(getStroke(0));
            g2dCopy.draw(s);
         }
      }
      finally 
      { 
         if(g2dCopy != null) g2dCopy.dispose(); 
      }
      
      return(task == null || task.updateProgress());
   }
   
   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException 
   { 
      Graph3DNode g3 = getParentGraph3D();
      if(g3 == null || !isRendered()) return;
      List<Point2D> verts = g3.getBackPlaneVertices(side);
      if(verts == null || verts.size() != 4) return;
      verts.add(null);   // to close the path
      
      psDoc.startElement(this);
      psDoc.renderPolygons(verts, getFillColor().getAlpha() != 0);
      psDoc.endElement();
   }

   @Override public BackPlane3DNode clone() throws CloneNotSupportedException
   {
      return (BackPlane3DNode) super.clone();
   }
}
