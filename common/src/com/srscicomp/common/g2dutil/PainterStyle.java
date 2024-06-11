package com.srscicomp.common.g2dutil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;

/**
 * This interface represents a container for the subset of Java2D styling attributes that a <code>Painter</code> uses to 
 * render itself in a <code>Graphics2D</code> context. <code>PainterStyle</code> provides a somewhat higher-level 
 * abstraction that captures both the styling capabilities and limitations of <code>Painter</code> objects:
 * <ul>
 *    <li>A <code>Painter</code> never changes the alpha compositing rule set on its graphics context.</li>
 *    <li>No texture or gradient paint -- a <code>Painter</code> only uses solid, possibly translucent <code>Color</code>
 *    However, <code>PainterStyle</code> includes separate stroke and fill colors -- since some <code>Painter</code>s 
 *    will need to both stroke and fill a shape. The fill color is generally used for painting text; there is no support 
 *    for outlining text.</p>
 *    <li>Stroke-related parameters include stroke color, line width, endcap style, join style, and dash pattern. The
 *    miter limit (for mitered joins only) is left to the <code>PainterStyle</code> implementation, but a limit of 10.0
 *    is recommended.</li>
 *    <li>Text rendering is limited to short labels in which all characters are drawn in the same font.</li>
 * </ul>
 * 
 * <p>An important convention on sizing:  By design <code>Painter</code> implementations NEVER rescale the Graphics2D 
 * context in which they render.  The intention here is that users of the <code>Painter</code> framework will have 
 * already set up any affine transformations on the <code>Graphics2D</code> object before passing it to a painter. 
 * Therefore, dimensioned quantities like font size, stroke width and stroke dash/gap lengths are always assumed to be 
 * in "logical units" IAW the coordinate system of the <code>Graphics2D</code> that is passed to the painter.</p>
 * 
 * @author 	sruffner
 * @see Painter, java.awt.Graphics2D
 */
public interface PainterStyle
{
   /**
    * Get the installed font that the <code>Painter</code> should use to render any and all text content. It is 
    * <strong>essential</strong> that the returned font be sized in the logical units of the graphics context in which 
    * the <code>Painter</code> renders itself.
    * 
    * @return A <code>Font</code> object
    */
   Font getFont();

   /**
    * Return the color used to fill closed shapes or paint text.
    * 
    * @return A <code>Color</code> to be installed in the painter's <code>Graphics2D</code> context.
    */
   Color getFillColor();

   /**
    * Return the color of the stroke used to outline shapes or draw lines.
    * 
    * @return A <code>Color</code> to be installed in the painter's <code>Graphics2D</code> context.
    */
   Color getStrokeColor();

   /**
    * Return the pen stroke used to outline shapes or draw lines, with the specified dash phase. Parameters that define
    * the stroke include: line width (which must match the value returned by <code>getStrokeWidth()</code>), endcap 
    * style (butt, round, or square), join style (mitered, round, or beveled), miter limit, and the dash-gap pattern. 
    * 
    * @param dashPhase A distance in logical units, specifying an offset into the stroke dash-gap pattern.  Ignored if 
    * this <code>PainterStyle</code> uses a solid stroke.
    * @return The pen stroke.
    */
   Stroke getStroke(float dashPhase);

   /**
    * Return the size of the font encapsulated by this <code>PainterStyle</code>. A <code>Painter</code> may use this 
    * information to align text content or to estimate the cost of rendering itself.
    * 
    * @return Font size in logical units of the graphics context in which <code>Painter</code> will render itself.
    */
   double getFontSize();

   /**
    * Return the width of the pen stroke encapsulated by this <code>PainterStyle</code>. A <code>Painter</code> may use 
    * this information to assess the cost of rendering itself.
    * 
    * @return Stroke width in logical units of the graphics context in which <code>Painter</code> will render itself.
    */
   double getStrokeWidth();

   /**
    * Is the pen stroke encapsulated by this <code>PainterStyle</code> a solid stroke?
    * 
    * @return <code>True</code> iff pen stroke is solid (empty dash-gap pattern).
    */
   boolean isStrokeSolid();
   
   /**
    * Does the pen stroke encapsulated by this painter style make any marks when applied to a graphics object? If the 
    * stroke width is zero or the stroke color is transparent, the graphics object is not stroked.
    * @return True if pen stroke makes no marks when applied to a graphics object.
    */
   boolean isStroked();
}
