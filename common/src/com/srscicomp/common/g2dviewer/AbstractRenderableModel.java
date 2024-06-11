package com.srscicomp.common.g2dviewer;

import java.awt.geom.Point2D;

/**
 * A partial implementation of <code>RenderableModel</code> suitable for non-interactive models that do not support a 
 * display focus. Implements registration of the model with a <code>RenderableModelViewer</code>.
 * @author sruffner
 */
public abstract class AbstractRenderableModel implements RenderableModel
{
   /** The viewer with which this model is registered, or <code>null</code> if it is not registered. */
   protected RenderableModelViewer rmviewer = null;
   
   public boolean registerViewer(RenderableModelViewer viewer)
   {
      if(rmviewer != null || viewer == null) return(false);
      rmviewer = viewer;

      return(true);
   }
   public void unregisterViewer(RenderableModelViewer viewer) { if(rmviewer == viewer) rmviewer = null; }
   public RenderableModelViewer getViewer() { return(rmviewer); }

   public Focusable getCurrentFocusable() { return(null); }
   public boolean isFocusSupported() { return(false); }
   public void moveFocusable(double dx, double dy) {}
   public void pointClicked(Point2D p) {}
}
