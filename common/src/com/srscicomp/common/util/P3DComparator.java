package com.srscicomp.common.util;

import java.util.Comparator;

/** 
 * Compares two 3D points based only on their X- and Y-coordinates. Its purpose is to provide a mechanism for sorting a 
 * set of 3D points drawn in an XYZ coordinate system represented by a {@link com.srscicomp.common.g2dutil.Projector}
 * object so that the points are drawn from back to front, avoiding collusion issues to the extent possible.
 * @author sruffner
 */
public class P3DComparator implements Comparator<Point3D>
{
   /**
    * Create a comparator that orders 3D points by examining their X- and/or Y-coordinates only.
    * @param xFirst If true, comparison is based on the X-coordinates, unless they are equal, in which case the Y-
    * coordinates are examined. Vice versa if false.
    * @param xRev False to sort points so that X-coordinate value is in ascending order; true for descending order.
    * @param yRev False to sort points so that Y-coordinate value is in ascending order; true for descending order.
    */
   public P3DComparator(boolean xFirst, boolean xRev, boolean yRev)
   {
      this.xFirst = xFirst;
      this.xRev = xRev;
      this.yRev = yRev;
   }
   
   @Override public int compare(Point3D o1, Point3D o2)
   {
      if(o1 == null || o2 == null) return(0);
      if(xFirst)
      {
         if(o1.getX() != o2.getX())
            return((xRev ? -1 : 1) * (o1.getX() < o2.getX() ? -1 : 1));
         else
            return((yRev ? -1 : 1) * (Double.compare(o1.getY(), o2.getY())));
      }
      
      if(o1.getY() != o2.getY())
         return((yRev ? -1 : 1) * (o1.getY() < o2.getY() ? -1 : 1));
      else
         return((xRev ? -1 : 1) * (Double.compare(o1.getX(), o2.getX())));
   }
   
   /** 
    * True if X-coordinates are checked first and Y-coordinates are checked only if X-coordinates are equal. 
    * Vice versa if false. 
    */
   private final boolean xFirst;
   /** True to sort by descending X-coordinate value; false to sort by ascending value. */
   private final boolean xRev;
   /** True to sort by descending Y-coordinate value; false to sort by ascending value. */
   private final boolean yRev;
}
