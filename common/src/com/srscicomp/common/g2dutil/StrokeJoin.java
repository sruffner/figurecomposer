package com.srscicomp.common.g2dutil;

import java.awt.BasicStroke;

/**
 * An enumeration of the alternatives for the decoration applied at the intersection of two path segments and at the
 * intersection of the endpoints of a closed path, when stroking a 2D graphics primitive. These match the alternatives 
 * available in Java2D and Postscript.
 * @author sruffner
 */
public enum StrokeJoin
{
   /** 
    * Join path segments by extending their outside corners until they meet. If the ratio of miter length (length of 
    * line segment connecting outside and inside corners) to stroke width exceeds the miter limit (default = 10), a 
    * bevel join is used instead.
    */
   MITER,

   /** Join path segments by rounding off the corner at a radius of half a stroke width. */
   ROUND,

   /** Join path segments by connecting their outside corners with a straight line segment. */
   BEVEL;
   
   @Override public String toString() { return( super.toString().toLowerCase() ); }
   
   /**
    * Get the corresponding Java2D join style code.
    * @return <code>BasicStroke.JOIN_MITER, JOIN_ROUND</code>, or <code>JOIN_BEVEL</code>.
    */
   public int getJava2DLineJoin()
   { 
      return(this.equals(MITER) ? BasicStroke.JOIN_MITER : 
         (this.equals(ROUND) ? BasicStroke.JOIN_ROUND : BasicStroke.JOIN_BEVEL));
   }
   
   /** 
    * Get the Postscript line join code expected by the <i>setlinejoin</i> operator.
    * @return 0 for mitered, 1 for round, and 2 for beveled joins.
    */
   public int getPostscriptLineJoin()
   { 
      return((this.equals(MITER)) ? 0 : (this.equals(ROUND) ? 1 : 2));
   }
}
