package com.srscicomp.common.g2dviewer;

import java.awt.geom.Point2D;

/**
 * <code>RenderableModel</code> encapsulates a vector graphics picture that can be drawn into a <code>Graphics2D</code> 
 * context using the Java2D API. It was designed for use with the <code>Graph2DViewer</code> user interface component.
 * 
 * <p>When a <code>RenderableModel</code> is installed in <code>Graph2DViewer</code>, it will register itself as a 
 * <code>RenderableModelViewer</code> on the model. The <code>RenderableModelViewer</code> interface exposes all of the
 * functionality of <code>Graph2DViewer</code> that the model needs, including key methods for notifying the viewer
 * whenever the model state changes.</p>
 * 
 * @see Graph2DViewer
 * @author sruffner
 */
public interface RenderableModel
{
   /**
    * Register a <code>RenderableModelViewer</code> on the model. Only one such viewer may be registered with the 
    * model at a time. 
    * 
    * @param viewer The viewer object to be registered.
    * @return <code>True</code> iff the viewer was successfully registered with the model. The method should succeed 
    * if the specified viewer is already registered on the model, and must fail if a different viewer is already 
    * registered.
    */
   boolean registerViewer(RenderableModelViewer viewer);

   /**
    * Unregister the specified <code>RenderableModelViewer</code> as the model's <code>RenderableModelViewer</code>. 
    * Only one such viewer may be registered with the model at a time.
    * 
    * @param viewer The viewer to be disconnected from the model. If argument does not refer to the currently registered
    * viewer or is <code>null</code>, the method does nothing.
    */
   void unregisterViewer(RenderableModelViewer viewer);

   /** 
    * Get the <code>RenderableModelViewer</code> currently registered with this <code>RenderableModel</code>. 
    * @return The model's registered viewer, or <code>null</code> if the model is currently unregistered.
    */
   RenderableModelViewer getViewer();
   
   /**
    * Return a reference to the current top-level graphic node in the model.
    * 
    * <p>It is possible for a model to maintain a collection of <code>RootRenderable</code> objects, but the 
    * <code>RenderableModel2DViewer</code> can only display one such object at a time. If the identity of the top-level 
    * graphic node changes via user selection, the model must inform its registered viewer by invoking 
    * <code>RenderableModelViewer.modelMutated(RootRenderable, List<Rectangle2D>)</code>.</p>
    * 
    * @return The current <code>RootRenderable</code> object in this <code>RenderableModel</code>.
    */
   RootRenderable getCurrentRootRenderable();

   /**
    * Does this model support the notion of "display focus"? The <code>RenderableModelViewer</code> provides support 
    * for painting a translucent "focus shape" to highlight the particular <code>Focusable</code> node in the model 
    * that currently holds the focus for display purposes.
    * 
    * @return <code>True</code> iff this <code>RenderableModel</code> provides support for focussing on individual 
    * <code>Focusable</code>s appearing within the model's currently displayed <code>RootRenderable</code>.
    */
   boolean isFocusSupported();

   /**
    * Return a reference to the graphic node in the model that currently holds the "display focus".
    * 
    * @return The current <code>Focusable</code> object in this <code>RenderableModel</code>. If the model does not 
    * support the notion of "display focus", this method should always return <code>null</code>.
    */
   Focusable getCurrentFocusable();

   /**
    * Reposition the current <code>Focusable</code> node with respect to the logical coordinate system of the containing 
    * <code>RootRenderable</code>. This method will always be invoked on the Swing event dispatch thread.
    * 
    * <p>The <code>RenderableModelViewer</code> allows the user to interactively reposition the graphics node with the 
    * display focus, but only if the installed <code>RenderableModel</code> provides the necessary support. When the 
    * user mouses down within the focus highlight in the viewer, the viewer calls <code>Focusable.canMove()</code> to
    * see whether that node can be interactively repositioned. If so, the viewer initiates a mouse-drag interaction 
    * whereby the user can drag the focus highlight to a different location. Upon releasing the mouse, the viewer will 
    * invoke this method -- the model is responsible for actually updating the position of the current 
    * <code>Focusable</code> node and posting a render job to update the viewer's canvas accordingly.</p>
    * 
    * @param dx Net change in horizontal position of the current <code>Focusable</code> node, in milli-in WRT the 
    * logical coordinate system of the <code>RootRenderable</code> within which the node appears.
    * @param dy Net change in vertical position of the current <code>Focusable</code> node, in milli-in WRT the logical 
    * coordinate system of the <code>RootRenderable</code> within which the node appears.
    */
   void moveFocusable(double dx, double dy);

   /**
    * As part of its framework for supporting the notion of "focus" within a rendered graphic picture, the 
    * <code>RenderableModelViewer</code> will invoke this method to inform its <code>RenderableModel</code> whenever 
    * the user mouse-clicks somewhere within the viewer's canvas. Typically, the model would respond by changing the 
    * focus to the <code>Focusable</code> node that is closest to the point clicked.
    * 
    * <p>Implementations that do not support the notion of "display focus" can simply provide an empty implementation 
    * of this method. Even if "display focus" is supported, the model need not respond to mouse-clicks in the viewer; 
    * it could be that focus changes occur in some other manner in the particular application.</p>
    * 
    * @param p The point at which the mouse was "clicked" on the <code>RenderableModelViewer</code> in which this 
    * <code>RenderableModel</code> is displayed. Coordinates are in milli-inches WRT the logical painting coordinate 
    * (x-axis increasing rightward, y-axis increasing upward, origin at bottom-left) system of the currently displayed 
    * <code>RootRenderable</code>.
    */
   void pointClicked(Point2D p);
}
