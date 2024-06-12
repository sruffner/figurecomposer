package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.srscicomp.common.g2dviewer.RenderTask;

/**
 * <b>ScatterLineStyleNode</b> is a required child of {@link ScatterPlotNode}. It is a <b><i>non-renderable, 
 * non-focusable</i></b> graphic node that governs some aspects of the appearance of a scatter plot, depending on the 
 * display mode:
 * <ul>
 * <li>In the "trendline" display mode, this defines how the LMS regression line is stroked independent of the scatter
 * plot marker symbols, which are stroked IAW the properties defined on {@link ScatterPlotNode} itself.</li>
 * <li>In the other display modes, it determines if and how a "connect-the-dots" polyline is stroked. The default stroke
 * width for this node is 0 because typically this polyline is NOT drawn. <i>If it is drawn, the polyline is closed, ie, 
 * the last plot point is connected to the first</i>. In that scenario, the resulting shape will be filled IAW the fill
 * color defined on this node. Otherwise, the fill color style is unused and defaults to transparent black.</li>
 * </ul>
 * This node extends {@link FGraphicNode} only so that it can take advantage of base-class support for the common 
 * <i>FypML</i> style properties and the style inheritance mechanism.
 * 
 * @author sruffner
 */
public class ScatterLineStyleNode extends FGraphicNode implements Cloneable
{

   /**
    * Construct a <b>LineStyleNode</b> with zero stroke width and a transparent black fill color -- in which case the
    * parent {@link ScatterPlotNode} will not draw a trend line or "connect-the-dots" polyline. All other graphic style 
    * attributes are initially implicit.
    */
   public ScatterLineStyleNode()
   {
      super(HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR);
      setMeasuredStrokeWidth(new Measure(0, Measure.Unit.IN));
      setFillColor(new Color(0,0,0,0));
   }

   
   //
   // Support for child nodes -- none permitted!
   //
   @Override
   public FGNodeType getNodeType() { return(FGNodeType.SCATLINESTYLE); }

   @Override
   public boolean isLeaf() { return(true); }

   @Override
   public boolean canInsert(FGNodeType nodeType) { return(false); }

   /** A <b>LineStyleNode</b> is instrinsic to the definition of its parent {@link ScatterPlotNode}. */
   @Override
   public boolean isComponentNode() { return(true); }

   
   //
   // Properties
   //
   
   /**
    * Whenever this <b>LineStyleNode</b>'s definition changes, the parent {@link ScatterPlotNode} is notified in case 
    * the change affects that node's rendering.
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      if(getParent() instanceof ScatterPlotNode) 
         getParent().onNodeModified(null);
   }
   

   //
   // Support for style sets
   //
   
   /**
    * While style sets are supported, <b>ViolinStyleNode</b> only occurs as a component of a box plot node, and it 
    * should not receive the display focus in the UI.
    */
   @Override public boolean supportsStyleSet() { return(true); }


   //
   // Focusable/Renderable/PSTransformable support -- never rendered directly
   //

   @Override protected FGraphicNode hitTest(Point2D p) { return(null); }
   @Override protected void releaseRenderResourcesForSelf() {}
   @Override public AffineTransform getLocalToParentTransform() { return(new AffineTransform()); }
   @Override public FViewport2D getViewport() { return(null); }
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      return(new Rectangle2D.Double());
   }
   @Override public boolean render(Graphics2D g2d, RenderTask task) { return(true); }
   @Override public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException { /* NOOP */ }
}
