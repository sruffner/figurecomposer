package com.srscicomp.common.g2dutil;

import java.awt.BasicStroke;

/**
 * An enumeration of the alternatives for the decoration applied to the endcaps of open subpaths and dash segments when
 * stroking a 2D graphics primitive. These match the alternatives available in Java2D and Postscript.
 * @author sruffner
 */
public enum StrokeCap
{
   /** No added endcap decoration. */
   BUTT,

   /** A round endcap decoration with radius equal to half the stroke width. */
   ROUND,

   /** Decoration that effectively extends the line segment by half a stroke width. */
   SQUARE;
   
   @Override public String toString() { return( super.toString().toLowerCase() ); }
   
   /**
    * Get the corresponding Java2D line endcap code.
    * @return <code>BasicStroke.CAP_BUTT, CAP_ROUND</code>, or <code>CAP_SQUARE</code>.
    */
   public int getJava2DLineCap()
   { 
      return(this.equals(BUTT) ? BasicStroke.CAP_BUTT : 
         (this.equals(ROUND) ? BasicStroke.CAP_ROUND : BasicStroke.CAP_SQUARE));
   }
   
   /** 
    * Get the Postscript line endcap code expected by the <i>setlinecap</i> operator.
    * @return 0 for butt ends, 1 for rounded, and 2 for a projecting square cap.
    */
   public int getPostscriptLineCap()
   { 
      return((this.equals(BUTT)) ? 0 : (this.equals(ROUND) ? 1 : 2));
   }

}
