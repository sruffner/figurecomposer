package com.srscicomp.common.g2dviewer;

import java.awt.geom.Point2D;

/**
 * This interface defines a top-level <code>Renderable</code> object, which establishes the logical coordinate system 
 * in which that object (and the collection of any <code>Renderable</code>s within it -- most likely organized in a 
 * tree structure) is rendered on a <code>RenderingCanvas</code>.
 * 
 * <p>Prior to starting a render cycle on its backbuffer, the <code>RenderingCanvas</code> queries the 
 * <code>RootRenderable</code> in order to properly transform the graphics context (<code>Graphics2D</code>) of the 
 * backbuffer image so that:
 * <ul>
 *    <li>logical units are milli-inches (support for other units may come at a later date);</li>
 *    <li>the horizontal axis increases from left to right on the canvas;</li>
 *    <li>the vertical axis increases from bottom to top on the canvas (note: in "device" coords, the y-axis normally  
 *    increases top to bottom!); and</li>
 *    <li>the initial origin lies at the bottom-left corner of the <code>RootRenderable</code>.</li>
 * </ul>
 * </p>
 * <p>It then starts the render cycle by invoking the <code>render()</code> method on the <code>RootRenderable</code>, 
 * passing it the transformed graphics context.</p>
 * 
 * @author 	sruffner
 */
public interface RootRenderable extends Renderable
{
   /**
    * Get width of the bounding rectangle that encompasses all physical marks made in rendering this 
    * <code>RootRenderable</code>.
    * 
    * @return Width of bounding rectangle in milli-inches.
    */
   double getWidthMI();

   /**
    * Get height of the bounding rectangle that encompasses all physical marks made in rendering this 
    * <code>RootRenderable</code>.
    * 
    * @return Height of bounding rectangle in milli-inches.
    */
   double getHeightMI();
   
   /**
    * Get location of the bottom-left corner of this <code>RootRenderable</code>'s bounding rectangle with respect to 
    * the bottom-left corner of the printable area (inside margins) on the printed page.
    * 
    * <p>This method is provided solely to support a "print preview" mode on the <code>RenderingCanvas</code>, in which 
    * the graphics are drawn to the canvas as they would appear on the printed page (based on a page format supplied to 
    * the canvas). The returned location assumes the origin is at the bottom-left corner of the printable region, and 
    * that the x-axis increases rightward while the y-axis increases upward.</p>
    * 
    * @return Location of bounding rectangle's bottom-left corner relative to bottom-left corner of printable rect.
    * Coordinates in milli-inches.
    */
   Point2D getPrintLocationMI();
   
   /**
    * Does this rendered graphic contain any translucent regions? When this is the case, the {@link RenderingCanvas}
    * will ignore dirty regions and render the graphic in full during each render cycle. Otherwise, artifacts may occur
    * in the translucent regions.
    * @return True if rendered graphic contains translucent regions; false otherwise.
    */
   boolean hasTranslucentRegions();
}
