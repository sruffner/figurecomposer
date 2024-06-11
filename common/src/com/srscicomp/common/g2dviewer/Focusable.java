package com.srscicomp.common.g2dviewer;

import java.awt.Graphics2D;
import java.awt.Shape;

/**
 * This interface defines the contract that must be satisfied by any <code>Renderable</code> object that can also hold 
 * the "display focus" on the <code>Graph2DViewer</code>. The viewer "highlights" the object with the focus by filling 
 * a "focus shape" with a translucent light blue; that focus shape is provided by the <code>Focusable</code> object 
 * itself.
 * 
 * <p>A <code>RenderableModel/Focusable</code> implementation may optionally support interactive repositioning of the 
 * focus node via a mouse drag. If the user presses and holds down the mouse within the current focus highlight, the 
 * <code>Graph2DViewer</code> will invoke <code>canMove()</code> to check if the current <code>Focusable</code> 
 * supports an interactive move. If so, the viewer animates the drag and upon release of the mouse calls 
 * <code>RenderableModel.moveFocusable(double,double)</code> to tell the model that the focus node was moved.</p>
 * 
 * @see Graph2DViewer, RenderableModel, Renderable
 * @author  sruffner
 */
public interface Focusable extends Renderable
{
   /**
    * Get the <code>Shape</code> that represents the focus highlight for this <code>Focusable</code> object within a 
    * <code>RenderableModel</code> displayed on the <code>Graph2DViewer</code>. Since it is important that the shape be
    * painted quickly, implementations should keep the shape as simple as possible. In many situations, a simple 
    * rectangle is adequate. Also, this method should take as little time to execute as possible!
    * 
    * <p>The method will be invoked on the Swing event thread, whenever the viewer's canvas gets repainted. It is 
    * <em>NOT</em> invoked in the viewer's background rendering thread.
    * 
    * <p><strong><em>Important</em></strong>: The shape must be specified in the logical coordinate system defined by 
    * the <code>RootRenderable</code> within which this <code>Focusable</code> appears. The <code>Graph2DViewer</code> 
    * framework will always paint the returned shape in a graphic context transformed into that coordinate system.</p>
    * 
    * @param g2d The graphics context in which the graphic object and its focus shape is rendered. Logical units are in 
    * milli-in, as per <code>Graph2DViewer</code> convention. The context is provided only for the purposes of measuring 
    * text, which cannot be done accurately without such a context. The <code>Focusable</code> should not alter or make 
    * any marks on the graphic context, nor dispose of it!
    * @return The focus highlight shape for this <code>Focusable</code>, as described. If <code>null</code> is returned, 
    * the <code>Graph2DViewer</code> will not paint a focus highlight on its canvas.
    */
   Shape getFocusShape(Graphics2D g2d);

   /**
    * Does this <code>Focusable</code> support repositioning itself via an interactive mouse drag on the viewer canvas?
    * 
    * @return <code>True</code> if interactive repositioning is supported; <code>false</code> otherwise.
    */
   boolean canMove();
}
