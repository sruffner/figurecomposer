package com.srscicomp.common.util;


/**
 * A point in three-dimensional space. All three coordinates are double-valued.
 * 
 * @author sruffner
 */
public class Point3D
{
   /**
    * Is the specified 3D point well-defined, having real, finite coordinate values?
    * @param p The point to test.
    * @return True if p is non-null and all three of its coordinates are real and finite. Else false.
    */
   public static boolean isWellDefined(Point3D p) { return(p != null && p.isWellDefined()); }

   /** Construct a point representing the origin (0, 0, 0) in an arbitrary 3D coordinate system. */
   public Point3D() {  x = y = z = 0.0; }
   
   /**
    * Construct the 3D point at (x, y, z).
    * @param x The X-coordinate of the point.
    * @param y The Y-coordinate of the point.
    * @param z The Z-coordinate of the point.
    */
   public Point3D(double x, double y, double z) 
   {
      this.x = x;
      this.y = y;
      this.z = z;
   }
   
   /** 
    * Change the coordinates of this 3D point.
    * 
    * @param x The new X-coordinate value.
    * @param y The new Y-coordinate value.
    * @param z The new Z-coordinate value.
    */
   public void setLocation(double x, double y, double z)
   {
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public double getX() { return(x); }
   public void setX(double x) { this.x = x; }
   public double getY() { return(y); }
   public void setY(double y) { this.y = y; }
   public double getZ() { return(z); }
   public void setZ(double z) { this.z = z; }
   
   /** Two points in 3D space equal if both are well-defined and have the same coordinate values. */
   @Override public boolean equals(Object obj)
   {
      boolean same = (obj.getClass().equals(Point3D.class));
      if(same)
      {
         Point3D p = (Point3D) obj;
         same = isWellDefined() && p.isWellDefined() && x == p.x && y == p.y && z == p.y;
      }
      return(same);
   }
   
   @Override public int hashCode()
   {
      long bits = Double.doubleToLongBits(x);
      bits ^= Double.doubleToLongBits(y) * 31;
      bits ^= Double.doubleToLongBits(z) * 31;
      return(((int) bits) ^ ((int) (bits >> 32)));
   }

   /**
    * Is this a well-defined point in 3D space, i.e., are all of its coordinates real, finite values?
    * @return True if point is well-defined; else false.
    */
   public boolean isWellDefined() 
   {
      return(Utilities.isWellDefined(x) && Utilities.isWellDefined(y) && Utilities.isWellDefined(z));
   }
   
   /**
    * Calculate the squared distance of this point from another point in 3D space.
    * @param x X-coordinate of the other point.
    * @param y Y-coordinate of the other point.
    * @param z Z-coordinate of the other point.
    * @return The squared distance. Will be NaN if any coordinate value is ill-defined.
    */
   public double distanceSq(double x, double y, double z)
   {
      return((this.x - x) * (this.x - x) + (this.y - y) * (this.y - y) + (this.z - z) * (this.z - z));
   }
   
   /** X-coordinate of the point. */
   private double x;
   /** Y-coordinate of the point. */
   private double y;
   /** Z-coordinate of the point. */
   private double z;
}
