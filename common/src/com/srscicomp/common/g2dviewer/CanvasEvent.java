package com.srscicomp.common.g2dviewer;

import java.awt.geom.Rectangle2D;

/**
 * An immutable class encapsulating information that the {@link Graph2DViewer} exposes to any registered listeners
 * regarding changes in the state of its graphics canvas, including significant events during a background render job.
 * 
 * <p>Note that, unlike Swing/AWT event objects, the "source" of the event is NOT available. This is because the 
 * graphics canvas delegate is visible only to the <code>Graph2DViewer</code>. Also note that not all of the information
 * in <code>CanvasEvent</code> is applicable for all types of events reported. Rather than create different listeners 
 * and different event objects for different types of events, we chose to stick with a single catch-all event.</p>
 * 
 * @author sruffner
 */
public class CanvasEvent
{
   /**
    * Approximate percentage of a rendering task completed when event was generated (zero if event is not related to a 
    * rendering job).
    */
   private final double pctComplete;

   /**
    * Approximate time of event, in milliseconds elapsed since the start of a rendering task (zero if event is not 
    * related to a rendering job). 
    */
   private final long elapsedTimeMS;

   /**
    * X-coordinate of cursor (mouse) position within the canvas at the time the event was generated, WRT the logical 
    * coordinate system of the graphic currently displayed, in milli-inches. Will be <code>Double.NaN</code> if 
    * cursor is not inside the canvas viewport or if the cursor position is not applicable to the particular 
    * <code>CanvasEvent</code> generated.
    */
   private final double xCursor;

   /**
    * X-coordinate of cursor (mouse) position within the canvas at the time the event was generated, WRT the logical 
    * coordinate system of the graphic currently displayed, in milli-inches. Will be <code>Double.NaN</code> if 
    * cursor is not inside the canvas viewport or if the cursor position is not applicable to the particular 
    * <code>CanvasEvent</code> generated.
    */
   private final double yCursor;

   /**
    * The canvas viewport rectangle expressed in the logical coordinate system of the graphic currently displayed on 
    * the canvas. Will be <code>null</code> if the canvas viewport is not applicable to the particular 
    * <code>CanvasEvent</code> generated.
    */
   private final Rectangle2D viewRectLogical;

   /**
    * Create an (immutable) <code>CanvasEvent</code> representing an epochal event during a rendering task that takes 
    * place in the <code>RenderingCanvas</code>'s background renderer thread.
    * 
    * @param elapsedTimeMS The (approximate) elapsed time at which this event was generated, in milliseconds since the 
    * start of the task.
    * @param pctComplete The (approximate) percentage of the task that was completed when this event was generated.
    * @return A <code>CanvasEvent</code> encapsulating the specified information.
    */
   static CanvasEvent createRenderProgressEvent(long elapsedTimeMS, double pctComplete)
   {
       return(new CanvasEvent(pctComplete, elapsedTimeMS));
   }

   /**
    * Create an (immutable) <code>CanvasEvent</code> reporting a change in the canvas viewport size and/or location.
    * 
    * @param rLog The new canvas viewport rectangle, in milli-inches WRT the logical coordinate system of the graphics 
    * displayed on the canvas. In print preview mode, the origin lies at the BL corner of the displayed page. 
    * <em>Note</em>: By convention, the y-axis increases upward in this logical coordinate system -- so the defining 
    * corner for the rectangle is its bottom-left corner rather than the top-left!
    * @return A <code>CanvasEvent</code> encapsulating the specified information.
    */
   static CanvasEvent createViewportChangedEvent(Rectangle2D rLog)
   {
       return(new CanvasEvent(Double.NaN, Double.NaN, rLog));
   }

   /**
    * Create an (immutable) <code>CanvasEvent</code> reporting a change in the position of the canvas mouse cursor, or 
    * the location of the cursor when the mouse was clicked by the user.
    * 
    * @param x X-coordinate of mouse cursor within canvas viewport, in milli-inches WRT the logical coordinate 
    * system of the graphics displayed on the canvas. Set to <code>Double.NaN</code> to indicate that the cursor is no 
    * longer inside the canvas viewport.
    * @param y Analogously, for the y-coordinate.
    * @return A <code>CanvasEvent</code> encapsulating the specified information.
    */
   static CanvasEvent createCursorEvent(double x, double y)
   {
       return(new CanvasEvent(x, y, null));
   }

   /**
    * Construct a <code>CanvasEvent</code> that reports progress on a background rendering task.
    * 
    * @param pctComplete The (approximate) percentage of the task that was completed when this event was constructed.
    * @param elapsedTimeMS The (approximate) elapsed time at which this event was constructed, in milliseconds since the 
    * start of the task.
    */
   private CanvasEvent(double pctComplete, long elapsedTimeMS)
   {
      this.pctComplete = (pctComplete<0) ? 0 : ((pctComplete>100) ? 100 : pctComplete);
      this.elapsedTimeMS = (elapsedTimeMS<0) ? 0 : elapsedTimeMS;
      this.xCursor = Double.NaN;
      this.yCursor = Double.NaN;
      this.viewRectLogical = null;
   }

   /**
    * Construct a <code>CanvasEvent</code> that reports a change in the canvas viewport and/or the canvas cursor 
    * (mouse) position.
    * 
    * @param x X-coordinate of mouse cursor within canvas viewport, in milli-inches WRT the logical coordinate 
    * system of the graphics displayed on the canvas. Set to <code>Double.NaN</code> to indicate that the cursor is no 
    * longer inside the canvas viewport.
    * @param y Analogously, for the y-coordinate.
    * @param rLog The canvas viewport rectangle, in milli-inches WRT the logical coordinate system of the graphics 
    * displayed on the canvas. <em>Note</em>: By convention, the y-axis increases upward in this logical coordinate 
    * system -- so the defining corner for the rectangle is its bottom-left corner rather than the top-left!
    */
   private CanvasEvent(double x, double y, Rectangle2D rLog)
   {
      this.pctComplete = 0;
      this.elapsedTimeMS = 0;
      this.xCursor = x;
      this.yCursor = y;
      this.viewRectLogical = rLog;
    }

   /**
    * Get the approximate percentage of the rendering task that had been completed by the time that this event was 
    * generated.  Of course, since this event is posted on the Swing/AWT event thread and the rendering task occurs on 
    * a background thread, there's no telling how much further along the task has progressed when a registered 
    * <code>CanvasListener</code> is notified.
    * 
    * @return Percent completed, a value in [0..100], where 0 corresponds to a task that has just started, while 100 
    * is sent if the task has completed successfully.
    */
   public double getTaskPctComplete()
   {
      return( pctComplete );
   }

   /**
    * Get the time that this event was generated, as an elapsed time in milliseconds since the rendering task started.
    * 
    * @return Elapsed time since start of rendering task, in ms.
    */
   public long getTaskElapsedTimeMS()
   {
      return( elapsedTimeMS );
   }

   /**
    * Get the x-coordinate of the cursor (ie, mouse) within the canvas viewport at the time this <code>CanvasEvent</code> 
    * was generated -- expressed in the logical coordinate system of the graphic currently displayed on the canvas.
    * 
    * <p>If the x- or y-coordinate [<code>getCurrentCursorY()</code>] of the cursor position is <code>Double.NaN</code>, 
    * then the cursor is not inside the canvas viewport, or the cursor position is not applicable to the particular 
    * event represented by this <code>CanvasEvent</code> object.</p>
    * 
    * @return X-coordinate of cursor, as described.
    */
   public double getCurrentCursorX()
   {
      return(xCursor);
   }

   /**
    * Get the y-coordinate of the cursor (ie, mouse) within the canvas viewport at the time this <code>CanvasEvent</code> 
    * was generated -- expressed in the logical coordinate system of the graphic currently displayed on the canvas.
    * 
    * <p>If the x- or y-coordinate [<code>getCurrentCursorY()</code>] of the cursor position is <code>Double.NaN</code>, 
    * then the cursor is not inside the canvas viewport, or the cursor position is not applicable to the particular 
    * event represented by this <code>CanvasEvent</code> object.</p>
    * 
    * @return Y-coordinate of cursor, as described.
    */
   public double getCurrentCursorY()
   {
      return(yCursor);
   }

   /**
    * Get the canvas viewport rectangle, expressed in the logical coordinate system of the graphic currently displayed 
    * on the canvas. <em>Note</em>: By convention, the y-axis increases upward in this logical coordinate system -- so 
    * the defining corner for the rectangle is its bottom-left corner rather than the top-left!
    * 
    * @return Canvas viewport rectangle in milli-inches, as described. Will be <code>null</code> if the canvas viewport
    * is not applicable to the particular event represented by this <code>CanvasEvent</code> object.
    */
   public Rectangle2D getViewportRectLogical()
   {
      return(viewRectLogical);
   }
}
