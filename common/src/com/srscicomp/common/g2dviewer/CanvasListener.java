package com.srscicomp.common.g2dviewer;

import java.util.EventListener;

/**
 * A listener class that is notified of various events on the {@link Graph2DViewer}'s graphic canvas, including changes 
 * in the canvas viewport and cursor position, as well as the progress of a render task running on the background thread 
 * managed by the canvas.
 * 
 * <p>Canvas listeners will be notified on the Swing/AWT event dispatch thread:
 * <ul>
 *    <li>When a new rendering task is started.</li>
 *    <li>When it is stopped prematurely (probably because a new render task has been posted to the) or successfully 
 *    completed.</li>
 *    <li>At other times to inform the listener of progress in completing the task. The frequency of these progress 
 *    updates is controlled by the graphic canvas.</li>
 *    <li>When the mouse ("cursor") enters, exits, or moves within the canvas viewport.</li>
 *    <li>When the location and/or size of the canvas viewport changes WRT the logical coordinate system of the graphic 
 *    model displayed on the canvas.</li>
 * </ul>
 * </p>
 * 
 * <p>For the render job progress events, keep in mind that some indeterminate amount of time will have elapsed since 
 * the occurrence of the actual event and the time that a registered canvas listener is notified by invoking one of the 
 * methods in this interface.</p>
 * 
 * <p>Like a typical Swing event listener, <code>CanvasListener</code> implements {@link EventListener}, a
 * tagging interface. <code>Graph2DViewer</code> guarantees that the methods in this interface are invoked only on the 
 * Swing/AWT event dispatch thread. The declared methods share a single common "event" argument, {@link CanvasEvent};
 * but unlike Swing-style events, this argument is not descended from {@link java.util.EventObject}. The "source" of the
 * event (a delegate object that implements the canvas for <code>Graph2DViewer</code>) is NOT exposed to listeners!</p>
 * 
 * @author sruffner
 */
public interface CanvasListener extends EventListener
{
   /**
    * Listener method invoked when a new rendering task is started by the <code>RenderingCanvas</code>.
    * 
    * @param e Information about the event.
    */
   void renderingStarted(CanvasEvent e);

   /**
    * Listener method invoked when a rendering task is stopped prematurely.
    * 
    * @param e Information about the event.
    */
   void renderingStopped(CanvasEvent e);

   /**
    * Listener method invoked when a rendering task is successfully completed.
    * 
    * @param e Information about the event.
    */
   void renderingCompleted(CanvasEvent e);

   /**
    * Listener method invoked to report progress on an ongoing render task.
    * 
    * @param e Information about the event.
    */
   void renderingInProgress(CanvasEvent e);

   /**
    * Listener method invoked when the mouse cursor enters, exits, or moves within the <code>RenderingCanvas</code>'s 
    * viewport.
    * 
    * @param e Information about the event.
    */
   void cursorMoved(CanvasEvent e);

   /**
    * Listener method invoked whenever there is a change in the logical size or location (WRT the coordinate system of 
    * the graphics displayed) of the <code>RenderingCanvas</code>'s viewport.
    * 
    * @param e Information about the event.
    */
   void viewportChanged(CanvasEvent e);
}
