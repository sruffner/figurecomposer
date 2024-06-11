package com.srscicomp.common.g2dviewer;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * <code>RenderableModelViewer</code> represents a canvas-like element which renders the content of a 
 * <code>RenderableModel</code>. <code>Graph2DViewer</code> registers itself as a <code>RenderableModelViewer</code> on 
 * its installed <code>RenderableModel</code> so that it can respond to changes in the rendered appearance of that 
 * model. In essence, it encapsulates the minimum functionality of <code>Graph2DViewer</code> that must be exposed to 
 * the <code>RenderableModel</code>. 
 * 
 * @see Graph2DViewer, RenderableModel
 * @author sruffner
 */
public interface RenderableModelViewer
{
   /**
    * Method invoked when some chunk of the <code>RenderableModel</code> has been changed, requiring that all or some 
    * portion of the model be re-rendered.
    * 
    * <p><em>This method must be invoked only on the Swing/AWT event dispatch thread.</em></p>
    * 
    * @param dirtyRects A list of the rectangular regions bounding the areas that are affected by the change in the 
    * model. This argument can be <code>null</code>, indicating that the graphic model needs a full re-rendering. All 
    * rectangles in this list are defined WRT the logical coordinate system of the current <code>RootRenderable</code>. 
    * By design, the origin of this coord system lies at the bottom-left corner of the <code>RootRenderable</code>'s 
    * bounding rectangle, the x-axis increases rightward, the y-axis increases upward, and the logical units are 
    * milli-inches.
    */
   void modelMutated(List<Rectangle2D> dirtyRects);

   /**
    * Method invoked when the current "display focus" switches to a different graphic object in the 
    * <code>RenderableModel</code>. The rendered graphics do not require updating, only the translucent "focus 
    * highlight" drawn on top of the graphic picture displayed in <code>Graph2DViewer</code>.
    * 
    * <p>A <code>RenderableModel</code> implementation which does not support the notion of "display focus" should 
    * never invoke this method on its registered <code>RenderableModelViewer</code>.</p>
    */
   void focusChanged();

   /**
    * Method invoked when the <code>RenderableModel</code> needs access to the Java2D graphics context of the 
    * <code>RenderableModelViewer</code> in which it is displayed.
    * 
    * <p><em>This method exists because one needs access to the viewer's graphics context to accurately measure 
    * rendered text. However, it is <strong>imperative</strong> that callers do not hold onto the returned context 
    * for very long and that they <code>dispose()</code> of it as soon as they are done with it. A graphics context 
    * is a precious resource to be used with care!</em></p>
    * 
    * @return A compatible <code>Graphics2D</code> context for this <code>RenderableModelViewer</code>. The context is 
    * transformed so that logical units are in milli-inches, IAW established conventions for a model displayed in a
    * <code>RenderableModelViewer</code>. Returns <code>null</code> if no graphics context is available.
    */
   Graphics2D getViewerGraphicsContext();
}
