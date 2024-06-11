package com.srscicomp.common.g2dutil;

/**
 * Enumeration of the typical resize anchor locations for a rectangular bounding box: four sides and four corners. A
 * "catch-all" non-directional anchor is included to handle the case of a non-rectangular resize outline.
 * @author sruffner
 */
public enum ResizeAnchor
{
   /** Top edge. */ NORTH,
   /** Bottom edge. */ SOUTH,
   /** Right edge. */ EAST,
   /** Left edge. */ WEST,
   /** Top left corner. */ NORTHWEST,
   /** Top right corner. */ NORTHEAST,
   /** Bottom left corner. */ SOUTHWEST,
   /** Bottom right corner. */ SOUTHEAST,
   /** Non-directional anchor for non-rectangular resize outlines. */ ANY;
   
   /**
    * Retrieve a resize anchor position consistent with the specified "hit test" results for a point on a rectangular
    * outline. The algorithm favors left (west) over right (east), top (north) over bottom (south), and corners over
    * edges.
    * @param isLeft True if point is on left edge of rectangular box.
    * @param isRight True if point is on right edge of rectangular box.
    * @param isTop True if point is on top edge of rectangular box.
    * @param isBottom True if point is on bottom edge of rectangular box.
    * @return The corresponding anchor position. Returns null if all arguments are false.
    */
   public static ResizeAnchor getAnchor(boolean isLeft, boolean isRight, boolean isTop, boolean isBottom)
   {
      ResizeAnchor ra = null;
      if(isLeft)
      {
         if(isTop) ra = NORTHWEST;
         else if(isBottom) ra = SOUTHWEST;
         else ra = WEST;
      }
      else if(isRight)
      {
         if(isTop) ra = NORTHEAST;
         else if(isBottom) ra = SOUTHEAST;
         else ra = EAST;
      }
      else if(isTop) ra = NORTH;
      else if(isBottom) ra = SOUTH;
      
      return(ra);
   }
   
   public boolean isWest() { return(this==WEST || this==NORTHWEST || this==SOUTHWEST); }
   public boolean isEast() { return(this==EAST || this==NORTHEAST || this==SOUTHEAST); }
   public boolean isNorth() { return(this==NORTH || this==NORTHWEST || this==NORTHEAST); }
   public boolean isSouth() { return(this==SOUTH || this==SOUTHEAST || this==SOUTHWEST); }
}
