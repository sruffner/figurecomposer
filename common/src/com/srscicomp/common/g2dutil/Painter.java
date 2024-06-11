package com.srscicomp.common.g2dutil;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.atomic.AtomicBoolean;

import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.g2dviewer.Renderable;

/**
 * <code>Painter</code> is the base class for all painter objects that can render graphic constructs into a Java 
 * <code>Graphics2D</code> context using a restricted set of graphic style attributes encapsulated by 
 * <code>PainterStyle</code>.
 * 
 * <p><code>Painter</code> implements the <code>com.srscicomp.common.g2dviewer.Renderable</code> interface, so it is loosely tied 
 * into the <code>com.srscicomp.common.g2dviewer.RenderingCanvas</code> framework. The <code>render()</code> method 
 * takes a <code>RenderTask</code> argument by which long-running painter implementations can "callback" to the canvas 
 * to update job progress and check to see if the painting operation should be cancelled. To support progress 
 * updates and timely job cancellation in this framework, an implementation's <code>paintInternal()</code> method 
 * should call <code>stopPainting()</code> at regular intervals <em>if</em> the rendering will take a long time to 
 * complete. If the painter can render itself very quickly, there's no need to invoke <code>stopPainting()</code>.</p>
 *
 * <p>Note that <code>Painter</code> and any implementations can be used outside the <code>RenderingCanvas</code> 
 * framework -- just invoke <code>Painter.render()</code> with a null <code>RenderTask</code> argument!</p>
 * 
 * <p><code>Painter</code> maintains two properties considered universal to all painter implementations:
 * <ul>
 *    <li>The <code>PainterStyle</code> construct, which determines what graphic attributes are assigned to the 
 *    graphics context during rendering.</li>
 *    <li>A "location producer" -- of the form <code>Iterable<Point2D></code> -- which generates an ordered sequence of 
 *    locations that somehow define where the painter draws on the graphic context. It is assumed that the graphics 
 *    context passed to the <code>render()</code> method has already been transformed into the coordinate system in 
 *    which the points delivered by the location provider are expressed. Exactly how the locations are consumed will 
 *    vary with the <code>Painter</code> implementation. The idea here is to leave the details of maintaining and 
 *    transforming loci up to the user of a <code>Painter</code> implementation.</li>
 * </ul>
 * </p>
 * 
 * <p><strong><em>IMPORTANT</em></strong>: All <code>Painter</code> implementations should assume that the logical 
 * coordinate system within which they paint is <strong><em>right-handed</em></strong>: x-coordinate values increase 
 * rightward, while y-coordinate values increase upward; a positive rotation angle yields a CCW rotation. This is 
 * different from the device coordinate system of the display screen, which is left-handed (y-axis increases downward 
 * rather than upward, positive angles yield CW rotation). AFAIK, this assumption only impacts the rendering of text; 
 * text-rendering uses font glyph info which fundamentally assume a coordinate system in which y increases downward. 
 * Therefore, any <code>Painter</code> implementation that renders text must temporarily invert the y-axis of its 
 * graphic context prior to painting the text.</p>
 * 
 * <p><code>Painter</code> implementations obviously must encapsulate other properties that define the actual graphic 
 * construct to be rendered. Such properties should be settable but not gettable -- the idea being that users of 
 * <code>Painter</code> objects should control the values of such properties, and they should not be exposed to other 
 * objects to which a <code>Painter</code> might be passed.</p>
 * 
 * @author 	sruffner
 */
public abstract class Painter implements Renderable
{
   /**
    * Construct a <code>Painter</code> that uses a default <code>PainterStyle</code> when it paints itself. The 
    * painter's location producer is initially unspecified.
    * 
    * <p>To change the graphic attributes later, use <code>setStyle()</code>. To set the location producer, use 
    * <code>setLocationProducer()</code>.
    */
   public Painter()
   {
      this(null, null);
   }

   /**
    * Construct a <code>Painter</code>.
    * 
    * @param style Collection of graphic attributes applied to this <code>Painter</code>.  If <code>null</code>, then 
    * the <code>Painter</code> uses defaults.
    * @param producer The location producer for this <code>Painter</code>. If <code>null</code>, then a realistic
    * implementation is unlikely to draw anything!
    */
   public Painter(PainterStyle style, Iterable<Point2D> producer)
   {
      this.style = (style == null) ? BasicPainterStyle.createDefaultPainterStyle() : style;
      this.locationProducer = producer;
   }

   /**
    * The collection of graphic attributes that the Painter will apply to its graphic context when it paints itself. 
    */
   protected PainterStyle style = null;

   /**
    * Set the collection of graphic attributes that the <code>Painter</code> will apply to its graphic context when it 
    * paints itself. 
    * 
    * @param style A set of graphic attributes. If <code>null</code>, a default <code>PainterStyle</code> is used.
    */
   public void setStyle(PainterStyle style)
   {
      this.style = (style == null) ? BasicPainterStyle.createDefaultPainterStyle() : style;
   }

   /**
    * The location producer for this <code>Painter</code>.
    */
   protected Iterable<Point2D> locationProducer = null;

   /**
    * Set the location producer which provides an <code>Iterator<Point2D></code> over the defining locations at which 
    * this <code>Painter</code> paints itself. Exactly how these location(s) are used will depend on each particular 
    * implementation.
    * 
    * <p>The <code>Point2D</code> locations generated by the location producer should be expressed in the logical 
    * coordinate system associated with the graphics context passed to the <code>render()</code> method. Of course, 
    * implementations are free to provide a means of transforming these locations before using them.</p>
    * 
    * @param producer The location producer for this <code>Painter</code>. If <code>null</code>, any realistic 
    * <code>Painter</code> implementation will render nothing.
    */
   public void setLocationProducer(Iterable<Point2D> producer)
   {
      this.locationProducer = producer;
   }


   //
   // Painting
   //

   /**
    * A callback hook into the rendering framework: for reporting progress while painting and checking to see if the 
    * paint job should be cancelled. 
    */
   protected transient RenderTask progressHook = null;

   /**
    * Check whether or not the paint job should be aborted immediately.
    * 
    * <p>A <code>Painter</code> implementation that supports cancellation of a paint task should invoke this method at 
    * reasonable intervals while painting itself in <code>paintInternal()</code>.</p>
    * 
    * @return <code>True</code> iff paint job should stop now. Will return <code>false</code> if no 
    * <code>RenderTask</code> hook was passed into the <code>render</code> method.
    */
   protected final boolean stopPainting()
   {
      return(progressHook != null && !progressHook.updateProgress());
   }

   /**
    * Paint into the specified graphics context in accordance with the current definition of this <code>Painter</code>.
    * 
    * @param g2d The graphics context. It is not changed by this method.
    * @param progressHook If not <code>null</code>, this object provides callbacks into the rendering framework for 
    * reporting progress while painting and checking to see if the paint job should be cancelled.
    */
   public final boolean render(Graphics2D g2d, RenderTask progressHook)
   {
      this.progressHook = progressHook;
      updateFontRenderContext(g2d);
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      boolean finished = false;
      try 
      { 
         finished = paintInternal(g2dCopy) && !stopPainting(); 
      }
      finally 
      { 
         g2dCopy.dispose(); 
      }
      this.progressHook = null;
      return(finished);
   }

   /**
    * Paint into the specified graphics context in accordance with the current definition of this Painter.
    * 
    * <p>It is this method which <code>Painter</code> implementations must override to accomplish painting.  The public 
    * <code>render()</code> method protects the original graphics context from modification: it makes a copy of the 
    * context and calls <code>paintInternal()</code> with that copy, then disposes of the copy afterwards.</p>
    * 
    * <p>To update progress and check for job cancellation during a long-lived paint task, call 
    * <code>stopPainting()</code>. If the paint job should be cancelled, then this method should abort immediately, 
    * returning <code>false</code> to indicate that painting was not finished.</p>
    * 
    * @param g2d The graphics context. May be safely modified in any way. No need to dispose after use.
    * @return <code>False</code> iff the method aborted prematurely because the paint job was cancelled.
    */
   protected abstract boolean paintInternal(Graphics2D g2d);

   /** The rectangle bounding all marks made by this painter. */
   private final Rectangle2D renderBounds = new Rectangle2D.Double();

   /** Flag set when the cached rectangle bounding all marks made by this painter needs to be recalculated. */
   private final AtomicBoolean boundsInvalid = new AtomicBoolean(true);
   
   /**
    * A font rendering context, important for measuring text strings when computing the rectangle that bounds all marks 
    * made by a <code>Painter</code>. It is initialized each time the painter is rendered (because we have access to 
    * a <code>Graphics2D</code> context then!).
    */
   protected FontRenderContext fontRC = null;

   /**
    * Update the cached copy of this <code>Painter</code>'s font rendering context. This context contains information 
    * important for accurately measuring text.
    * 
    * <p>This method is called automatically when the <code>Painter.render(Graphics2D,RenderTask)</code> is called. 
    * However, if the painter's font changes, this method can be called to update the font rendering context so that 
    * the the painter can accurately recalculate its rendered bounds prior to the next rendering cycle.</p>
    * 
    * <p>If the font rendering context has changed in a meaningful way, <code>invalidateBounds()</code> is called to 
    * clear the cached rectangle bounding any marks made by the <code>Painter</code> -- since the change in font 
    * rendering context could affect the size of this rectangle.</p>
    * 
    * @param g2d The graphics context in which the <code>Painter</code> is rendered. If <code>null</code>, the method 
    * does nothing. The context is not altered by this method.
    */
   public void updateFontRenderContext(Graphics2D g2d)
   {
      if(g2d != null)
      {
         Font f = g2d.getFont();
         g2d.setFont(style.getFont());
         FontRenderContext oldFRC = fontRC;
         fontRC = g2d.getFontRenderContext();
         g2d.setFont(f);

         if((oldFRC != null) && !fontRC.equals(oldFRC))
            invalidateBounds();
      }
   }

   /**
    * Recalculate the rectangle bounding all marks made by this painter, IAW its current definition. The rectangle must 
    * be defined WRT the same logical coordinate system in which the painter renders itself. 
    * @param r Upon return, this rectangle should be set to the painter's bounding box. If the painter makes no marks,
    * of the bounding box cannot be calculated, it must be set to an empty rectangle.
    */
   protected abstract void recalcBounds2D(Rectangle2D r);

   /**
    * Get the rectangle bounding all marks made by this painter.
    * 
    * <p>For optimal performance, the method returns a cached computation of the bounding rectangle, if one is 
    * available. If not, the bounding rectangle is computed from scratch. To guarantee a fresh computation of the 
    * bounding rectangle, call {@link #invalidateBounds()} before invoking this method.</p>
    * 
    * <p>A cached result is returned whenever possible because the bounding rectangle computation could be a rather 
    * lengthy one (consider a painter that draws a shape with 10000 vertices!) that only rarely needs to be redone. By 
    * caching the result, access to the rendered bounds is very fast for applications such as a "focus" highlight or 
    * "hit-testing"</p>
    * 
    * @param bounds If not null, this rectangle is update to contain the result; else a new <code>Rectangle2D</code> is 
    * allocated on the heap.
    * @return The bounding box for all marks made in rendering this painter, defined in the same logical coordinate 
    * system in which the painter is rendered. If the painter makes no marks or if the bounding box is indeterminate for
    * any reason, the method returns an empty rectangle (zero width and height). If the argument is not null, the method
    * updates it to hold the result, then returns a reference to it.
    */
   final public Rectangle2D getBounds2D(Rectangle2D bounds)
   {
      Rectangle2D rect = (bounds != null) ? bounds : new Rectangle2D.Double();
      if(boundsInvalid.get())
      {
         recalcBounds2D(renderBounds);
         boundsInvalid.set(false);
      }
      rect.setRect(renderBounds);

      return(rect);
   }

   /**
    * Clear the cached computation of the bounding rectangle within which this painter makes any marks during rendering.
    * This will force a fresh computation of the painter's bounds the next time {@link #getBounds2D(Rectangle2D)} is
    * invoked.
    */
   final public void invalidateBounds() { boundsInvalid.set(true); }
}
