package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Objects;

import com.srscicomp.common.g2dviewer.RenderTask;

/**
 * <b>ViolinStyleNode</b> is a <b><i>non-renderable, non-focusable</i></b> graphic node that defines the appearance of
 * the kernel density estimate plot -- known as a violin plot -- that may accompany a box plot. It exists so that the 
 * author can style the violin plot differently from box plot and its whiskers and outlier symbols. It extends 
 * {@link FGraphicNode} only so that it can take advantage of base-class support for the common <i>FypML</i> style
 * properties and the style inheritance mechanism.
 * 
 * <p>This node is a required child of {@link BoxPlotNode}, and rendering of the violin plot is handled entirely in
 * that parent node. In addition to the fill color and stroking styles, <b>ViolinStyleNode</b> includes a <i>size</i> 
 * property that controls the width/height of the violin plot, which typically encompasses the box plot.</p>
 * 
 * @author sruffner
 */
public class ViolinStyleNode extends FGraphicNode implements Cloneable
{

   /**
    * Construct a <b>ViolinStyleNode</b> with zero width -- so that the violin plot is not rendered. All graphic style 
    * attributes are initially implicit.
    */
   public ViolinStyleNode()
   {
      super(HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR);
      size = new Measure(0.0, Measure.Unit.PCT);
   }

   
   //
   // Support for child nodes -- none permitted!
   //
   @Override
   public FGNodeType getNodeType() { return(FGNodeType.VIOLIN); }

   @Override
   public boolean isLeaf() { return(true); }

   @Override
   public boolean canInsert(FGNodeType nodeType) { return(false); }

   /** A <b>ViolinStyleNode</b> is instrinsic to the definition of its parent {@link BoxPlotNode}. */
   @Override
   public boolean isComponentNode() { return(true); }

   
   //
   // Properties
   //

   /** The rendered width/height of the violin plot associated with the parent {@link BoxPlotNode}. */
   private Measure size;

   /**
    * Get rendered size of the violin plot -- its width if oriented vertically, or height if oriented horizontally. 
    * If 0, the plot is not drawn
    * 
    * @return The rendered size of the violin plot.
    */
   public Measure getSize() { return(size); }

   /**
    * Set the rendered size of the violin plot. If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param m The new size. A null value is rejected, as is a measure in "user units". Otherwise, the measure is
    * constrained by {@link BoxPlotNode#BOXWIDTHCONSTRAINTS}. 
    * @return True if successful; false if value was rejected.
    */
   public boolean setSize(Measure m)
   {
      if((m == null) || (m.getUnits() == Measure.Unit.USER)) return(false);
      m = BoxPlotNode.BOXWIDTHCONSTRAINTS.constrain(m);

      boolean changed = (size != m) && !Measure.equal(size, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.SIZE, m)) return(true);
         
         Measure oldSize = size;
         size = m;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.SIZE);
            String desc = "Set violin plot width to: " + size.toString();
            FGNRevEdit.post(this, FGNProperty.SIZE, size, oldSize, desc);
         }
      }
      return(true);
   }

   
   /**
    * Whenever this <b>ViolinStyleNode</b>'s definition changes, the parent {@link BoxPlotNode} is notified in case the 
    * change affects that node's rendering. If a graphic style changes but the violin plot size is 0, there's no
    * effect on the rendering.
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      if(getParent() instanceof BoxPlotNode)
      {
         if((hint == null || hint == FGNProperty.SIZE) || (size.getValue() > 0))
            getParent().onNodeModified(null);
         else
            model.onChange(this, 0, false, null);
      }
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok;
      if(Objects.requireNonNull(p) == FGNProperty.SIZE)
         ok = setSize((Measure) propValue);
      else
         ok = super.setPropertyValue(p, propValue);
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value;
      if(Objects.requireNonNull(p) == FGNProperty.SIZE)
         value = getSize();
      else
         value = super.getPropertyValue(p);
      return(value);
   }
   

   //
   // Support for style sets
   //
   
   /**
    * While style sets are supported, <b>ViolinStyleNode</b> only occurs as a component of a box plot node, and it 
    * should not receive the display focus in the UI.
    */
   @Override public boolean supportsStyleSet() { return(true); }

   /** Only the plot size is a node-specific style for the violin plot. */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet) 
   { 
      styleSet.putStyle(FGNProperty.SIZE, getSize());
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      Measure sz = (Measure) applied.getCheckedStyle(FGNProperty.SIZE, getNodeType(), Measure.class);
      if(sz != null) sz = BoxPlotNode.BOXWIDTHCONSTRAINTS.constrain(sz);
      if(sz != null && !Measure.equal(sz, (Measure) restore.getCheckedStyle(FGNProperty.SIZE, null, Measure.class)))
      {
         size = sz;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.SIZE);

      return(changed);
   }


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
   
   /** The violin plot is not drawn if it has zero size, or if it is neither filled nor stroked. */
   @Override protected boolean isRendered()
   {
      return((size.getValue() > 0) && ((getFillColor().getAlpha() > 0) ||
            ((getStrokeColor().getAlpha() > 0) && (getStrokeWidth() > 0))));
   }

   @Override public ViolinStyleNode clone() throws CloneNotSupportedException
   {
      return (ViolinStyleNode) super.clone();
   }
}
