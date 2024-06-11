package com.srscicomp.common.g2dviewer;

import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * The <code>RenderTask</code> interface serves to provide a hook into the <code>RenderingCanvas</code> (without 
 * exposing the <code>RenderingCanvas</code> itself) by which the individual <code>Renderable</code>s in a 
 * <code>RenderableModel</code> implementation can get the list of dirty regions that need to be rendered (to avoid 
 * rendering graphic nodes that don't need to be!), update progress on a rendering job, and check whether or not that 
 * job should be cancelled immediately -- probably because a new rendering task is already pending on the canvas.
 * 
 * @see RenderingCanvas, Renderable, RenderableModel
 * @author sruffner
 */
public interface RenderTask
{
   /**
    * Get a list of rectangles defining the "dirty regions" that need to be redrawn during the rendering task 
    * represented by this <code>RenderTask</code>. The list must not change while the task is in progress, and 
    * callers should not alter the list in any way. The rectangles are defined in the logical coordinate system of the 
    * top-level graphic object (the <code>RenderableModel</code>'s current <code>RootRenderable</code>) that is being 
    * rendered. If a full rendering is required, the method should return an empty list.
    * 
    * <p>A <code>RenderableModel</code> implementation can choose to ignore the list of dirty regions and simply perform 
    * a complete redraw for every render job, but such an implementation is less efficient. By checking the dirty 
    * regions and skipping portions of the graphic that do not need to be rendered, significantly better rendering 
    * performance may be achieved.</p>
    * 
    * @return A list of dirty regions for the rendering job represented by this <code>RenderTask</code>.
    */
   List<Rectangle2D> getDirtyRegions();
   
   /**
    * Update task progress and check for job cancellation.
    * 
    * <p>Whenever this method is called, the <code>RenderingCanvas</code> will update its heuristic estimate of 
    * progress in completing the current rendering job. This estimate is based upon the elapsed time of the last 
    * completed rendering pass, adjusted as necessary. In addition, if a new rendering job is pending on the canvas, 
    * this method will return <code>false</code> to indicate that the current job should be cancelled immediately.</p>
    * 
    * @return <code>True</code> if rendering job should continue; <code>false</code> if it should be aborted.
    */
   boolean updateProgress();
}
