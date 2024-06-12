package com.srscicomp.fc.fig;

import java.awt.Graphics2D;

import com.srscicomp.common.g2dviewer.RootRenderable;

/**
 * <code>FRootGraphicNode</code> is an extension of <code>FGraphicNode</code> that can serve as the root node in the 
 * <em>DataNav</em> graphic model, <code>FGraphicModel</code>. The root node serves as the <code>RootRenderable</code> 
 * object in the graphic model, and it provides descendant nodes access to the model itself. This access is important 
 * so that an individual <code>FGraphicNode</code> can notify the model when it needs to be re-rendered.
 * 
 * <p>[<em>NOTE</em>. At this time, the only type of root node we anticipate supporting in the <em>DataNav</em> graphic 
 * model is a figure element. However, having a more general root node will permit the possible introduction of other 
 * root nodes in the future.]</p>
 * 
 * @author 	sruffner
 */
public abstract class FRootGraphicNode extends FGraphicNode implements RootRenderable, Cloneable
{
   /**
    * The <em>DataNav</em> graphics model in which this <code>FRootGraphicNode</code> serves as the root node.
    */
   private FGraphicModel ownerModel;

   /**
    * Construct a <code>FRootGraphicNode</code>. The node initially contains no child nodes and is not attached to a 
    * graphic model.
    * 
    * @param attrFlags Bit flags that selectively enable/disable <code>FGraphicNode</code>-support for various common 
    * properties. Passed directly to the super-class constructor.
    * @see FGraphicNode#FGraphicNode(int)
    */
   public FRootGraphicNode(int attrFlags)
   {
      super(attrFlags);
      ownerModel = null;
   }

   /**
    * Get the <em>DataNav</em> graphics model containing this <code>FRootGraphicNode</code> and all of its descendant 
    * nodes. Descendant nodes use this method to access the model in order to trigger rendering updates, gain access 
    * to the graphics context of the <code>RenderableModelViewer</code> canvas registered with the model, etc.
    * 
    * @return The graphics model containing this <code>FRootGraphicNode</code>.
    */
   FGraphicModel getOwnerModel() { return( ownerModel ); }

   /**
    * Set the graphic model for which this <code>FRootGraphicNode</code> serves as root node.
    * 
    * @param owner The <em>DataNav</em> graphic mode in which this node appears. If <code>null</code>, the entire 
    * graphic node tree rooted at this node will be detached from any model. In this case, all rendering resources 
    * associated with the graphic nodes are released. Otherwise, the entire tree's rendering infrastructure is 
    * reconstituted, and a full re-rendering of the graphic model is triggered.
    */
   void setOwnerModel(FGraphicModel owner)
   {
      assert(owner != null);
      if(ownerModel != owner)
      {
         ownerModel = owner;
         if(ownerModel == null)
            releaseRenderResources();
         else
         {
            Graphics2D g2d = ownerModel.getViewerGraphics();
            try { getRenderBounds(g2d, true, null); }
            finally { if(g2d != null) g2d.dispose(); }
            ownerModel.onChange(this, 2, true, null);
         }
      }
   }

   /**
    * Overridden to ensure that the cloned root node does not have an owner model.
    */
   @Override
   protected Object clone()
   {
      // we must set owner model of THIS to null before cloning, because cloning of FGraphicNode will trigger model
      // notifications. We then restore owner model afterwards.
      FGraphicModel myOwner = ownerModel;
      ownerModel = null;
      FRootGraphicNode copy = (FRootGraphicNode) super.clone();
      ownerModel = myOwner;
      return(copy);
   }


}
