package com.srscicomp.common.g2dviewer;

import java.awt.Graphics2D;

/**
 * This interface defines the contract that must be satisfied by any object that is renderable on the 
 * <code>RenderingCanvas</code>. It anticipates implementations of <code>RenderableModel</code> consisting of an 
 * adequately partitioned collection of <code>Renderable</code>s contained by a top-level <code>RootRenderable</code>. 
 * 
 * @see RenderingCanvas, RenderableModel, RootRenderable
 * @author 	sruffner
 */
public interface Renderable 
{
   /** 
    * Render the graphic "chunk" represented by this <code>Renderable</code> object by drawing into the provided Java2D 
    * graphics context.
    * 
    * <p>Maintenance of the graphics context is under the complete control of the model's rendering infrastructure. 
    * The <code>RenderingCanvas</code> guarantees only that the context delivered to the <code>RootRenderable</code> 
    * will have been prepared such that the logical coordinate system matches that defined by the 
    * <code>RootRenderable</code> and such that the encapsulated graphic picture is scaled and translated in accordance 
    * with the canvas's current viewing transform (zoom, pan, print preview, etc). <code>Renderable</code> 
    * implementations are discouraged from changing the <code>RenderingHints</code> set on this graphics context, 
    * however.</p>
    * 
    * <p>On "dirty regions", progress reporting, and cancelling a job: The <code>RenderTask</code> argument is really a 
    * callback hook into the <code>RenderingCanvas</code> itself, exposing two important methods:
    * <ul>
    *    <li><code>getDirtyRegions()</code>. This method provides a list of rectangular areas in the graphic picture 
    *    that require rendering. A <code>Renderable</code> object need not render itself if it does not make any marks 
    *    within any dirty region. All rectangles are expressed in the logical coordinate system of the current 
    *    <code>RootRenderable</code>.</li>
    *    <li><code>updateProgress()</code>. A rendering job takes place on a background thread administered by the 
    *    <code>RenderingCanvas</code>. Since any given rendering pass could take a significant amount of time, it is 
    *    desirable to provide some sort of feedback on job progress, and to cancel the job if a new job is posted on 
    *    the primary event dispatch thread (typically, because the user has changed the model, or changed the canvas's 
    *    view properties). Since the renderer thread spends most of the time in the model's rendering code, progress 
    *    reporting and timely job cancellation require the cooperation of all <code>Renderable</code>s in the model (we 
    *    do NOT want to cancel a job by terminating the thread!). To that end, every <code>Renderable</code> 
    *    implementation should invoke <code>updateProgress()</code> at least once, after rendering itself. If the 
    *    graphic chunk takes a long time to draw, the method should be invoked at regular intervals. When the method is 
    *    invoked, the canvas will update its heuristic estimate of job progress (based on elapsed time of the last 
    *    completed job) and return an indication of whether or not the rendering job should continue. If not, the 
    *    <code>Renderable</code> implementation should abort rendering immediately.</li>
    * </ul>
    * </p>
    * @param g2d The graphics context in which to draw. 
    * @param task See description above.
    * @return <code>True</code> if the rendering was completed; <code>false</code> if an error occurred or if the 
    * rendering job was cancelled.
    */
   boolean render(Graphics2D g2d, RenderTask task);
}
