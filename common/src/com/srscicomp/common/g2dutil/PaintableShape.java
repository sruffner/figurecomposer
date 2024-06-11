package com.srscicomp.common.g2dutil;

import java.awt.Shape;

/**
 * <code>PaintableShape</code> encapsulates the information required by a <code>ShapePainter</code> to render an 
 * arbitrary Java2D-renderable shape primitive:
 * <ul>
 *    <li>The "design shape" -- A <code>java.awt.Shape</code> that describes how to draw the actual shape. By 
 *    convention, for consistent appearance of all <code>PaintableShape</code>s when scaled and translated by a 
 *    <code>ShapePainter</code>, it is suggested that the shape be designed within a unit (1x1) "design box".  This is 
 *    not mandatory, however.</li>
 *    <li>Some shape properties: closed/unclosed, horizontal symmetry, and vertical symmetry.  <code>ShapePainter</code> 
 *    uses the closed flag to decide whether or not to fill the shape.  It does not use the symmetry properties, but 
 *    they may be useful in other contexts.</li>
 * </ul>
 *
 * @see ShapePainter
 * @author  sruffner
 */
public interface PaintableShape
{
   /**
    * Get the design shape.  To support scalability, the design shape is typically specified in a unit (1x1) "design 
    * box", with the origin at the center of the box rather than a corner.
    * 
    * @return A <code>Shape</code> object tracing the path for this <code>PaintableShape</code>.
    */
   Shape getDesignShape();

   /**
    * Does this <code>PaintableShape</code> represent a closed shape? Typically, <code>ShapePainter</code> will both 
    * fill and outline a closed shape, but only stroke an unclosed one.
    * 
    * @return <code>True</code> iff the <code>PaintableShape</code> is closed.
    */
   boolean isClosed();

   /**
    * Is this <code>PaintableShape</code> horizontally symmetric?  <code>ShapePainter</code> does not use this attribute 
    * when rendering a <code>PaintableShape</code>, but it might be useful information in other contexts.
    * 
    * @return <code>True</code> iff the <code>PaintableShape</code> is symmetric about a horizontal line passing 
    * through its design origin.
    */
   boolean isHSymmetric();

   /**
    * Is this <code>PaintableShape</code> vertically symmetric?  <code>ShapePainter</code> does not use this attribute 
    * when rendering a <code>PaintableShape</code>, but it might be useful information in other contexts.
    * 
    * @return <code>True</code> iff the <code>PaintableShape</code> is symmetric about a vertical line passing 
    * through its design origin.
    */
   boolean isVSymmetric();
}
