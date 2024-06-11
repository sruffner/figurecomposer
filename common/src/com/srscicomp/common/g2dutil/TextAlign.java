package com.srscicomp.common.g2dutil;

/**
 * An enumeration of the common types of left-to-right, top-to-bottom text string alignment, defined with respect to a 
 * "starting position".  It does not support international text.
 * 
 * <p>Note that vertical alignment is tricky because it will depend upon how the "height" of the rendered text is 
 * defined: the total font height, the distance from descender to ascender line, the distance from baseline to ascender 
 * line, or perhaps the height of the true bounding box for the particular text rendered (which would be very different 
 * for "acg" vs "ACG"!). The enumerated types here just use the vague notions of "top edge", "bottom edge", and 
 * "middle".</p>
 * 
 * @author sruffner
 */
public enum TextAlign
{
   /**
    * For horizontal alignment, left edge of text lies at x-coordinate of "starting position".  For vertical alignment, 
    * top edge of text lies at y-coordinate. 
    */
   LEADING,

   /**
    * For horizontal alignment, right edge of text lies at x-coordinate of "starting position".  For vertical alignment, 
    * bottom edge of text lies at y-coordinate.
    */
   TRAILING,

   /**
    * For horizontal alignment, the midpoint between the left and right edges of the text lies at x-coordinate of 
    * "starting position".  For vertical alignment, midpoint between the top and bottom edges lies at y-coordinate.
    */
   CENTERED
}
