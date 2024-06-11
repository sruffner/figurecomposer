package com.srscicomp.common.g2dutil;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.srscicomp.common.util.LineXY;
import com.srscicomp.common.util.Point3D;
import com.srscicomp.common.util.Utilities;

/**
 * A utility class the defines the perspective projection of a point in an arbitrary 3D coordinate system XYZ onto a 
 * 2D window that lies at a perpendicular distance D from the "camera", which lies at the origin (0,0,0) in a fixed 
 * "world" coordinate system XYZw. The Z-axis in this world system is perpendicular to and intersects the center of the 
 * projection window such that the window center is at (0,0,-D) in world coordinates. The XYZ system's origin also lies 
 * at (0, 0, -D), initially with its +Z-axis parallel to and pointing in the same direction as the world's +Y-axis, and 
 * its +X-axis parallel to but pointing in the opposite direction as the world's +X-axis. Both XYZ and XYZw are right-
 * handed. Two rotations may be applied to the XYZ system to get a "better perspective" once it is projected onto the 2D
 * plane: a CCW rotation R about its Z axis, then a CCW rotation E about the X-axis.
 * 
 * <p>Each side of the 3D box representing the XYZ coordinate system can have a different "extent" in world units, and
 * the X/Y/Z coordinate ranges map to these X/Y/Z extents. Furthermore, each axis in XYZ can be linear or logarithmic
 * (base 10). Given all this information, along with the projection distance D, rotation angle R, elevation angle E, and
 * the assumptions described above, any point in XYZ can be projected onto the 2D plane at (0,0,-D) in XYZw. This class 
 * handles the necessary calculations. It also offers a number of methods useful when rendering a 3D "backdrop" of the
 * XYZ coordinate system on a 2D canvas.</p>
 *  
 *  <p>The projection distance D is not set directly. Instead, it is proportional to the maximum dimension (in world
 *  units) of the 3D "box" onto which the arbitrary coordinate system XYZ is mapped: D = S*max(XE,YE,ZE), where S is a
 *  scale factor restricted to [2..20]. We found that for shorter projection distances (fish-eye like lens!) -- where
 *  D &lt; 2*max(XE,YE,ZE) -- the projection algorithm broke down.</p>
 *  
 * @author sruffner
 */
public class Projector implements Cloneable
{
   /**
    * Construct a 3D-to-2D projector with default parameters: 3D box with equal extents of 4000 "world" units in the 
    * three cardinal directions; a projection distance scale of 4 (so that the projection distance D will be 16000); 
    * rotation of 30 deg and elevation of 15; origin of the projected XYZ system at (0,0) in the projection plane 
    * (Z=-D); and a range of [-100 .. 100] "native" units for all three axes of the projected coordinate system. All
    * axes are linear.
    * 
    * <p>Use the various setter methods to change these projection parameters.</p>
    */
   public Projector()
   {
      distScale = 4;
      xExtW = yExtW = zExtW = 4000;
      xOriginW = yOriginW = 0;
      rotDeg = 30;
      elevDeg = 15;
      xMin = yMin = zMin = -100;
      xMax = yMax = zMax = 100;
      
      for(int i=0; i<boxCorners.length; i++) boxCorners[i] = new Point2D.Double();
      recalc();
   }
   
   /** 
    * Scale factor S used to compute projection distance D from the maximum dimension of the 3D box representing the 
    * XYZ coordinate system: D = S*max(xExt,yExt,zExt). Range-restricted to [2..20]. 
    */
   private int distScale;
   /** X-coordinate extent, in world units, of the 3D box representing the XYZ coordinate system that is projected. */
   private double xExtW;
   /** Y-coordinate extent, in world units, of the 3D box representing the XYZ coordinate system that is projected. */
   private double yExtW;
   /** Z-coordinate extent, in world units, of the 3D box representing the XYZ coordinate system that is projected. */
   private double zExtW;
   
   /**
    * Get the scale factor S used to compute projection distance D from the maximum dimension of the 3D box containing
    * the XYZ coordinate system that is projected onto the 2D plane: <i>D = S * max(xExt, yExt, zExt)</i>.
    * @return The projection distance scale factor, S.
    */
   public int getDistanceScale() { return(distScale); }
   
   /**
    * Set the scale factor S used to compute projection distance D from the maximum dimension of the 3D box containing
    * the XYZ coordinate system that is projected onto the 2D plane: <i>D = S * max(xExt, yExt, zExt)</i>.
    * @param s The projection distance scale factor, S. Must lie in [2..20].
    * @return True if successful; false if projection distance invalid.
    */
   public boolean setDistanceScale(int s)
   {
      if(s < 2 || s > 20) return(false);
      distScale = s;
      recalc();
      return(true);
   }
   
   /**
    * Get the X extent of the 3D box representing the XYZ coordinate system. Its X-axis range is mapped to this extent.
    * @return Extent of 3D box in the X-direction, in world units.
    */
   public double getXExtent() { return(xExtW); }

   /**
    * Set the X extent of the 3D box representing the XYZ coordinate system. Its X-axis range is mapped to this extent.
    * @param xe Extent of 3D box in the X-direction, in world units. Strictly positive.
    * @return True if successful; false if argument is invalid.
    */
   public boolean setXExtent(double xe)
   {
      if(!Utilities.isWellDefined(xe) || xe <= 0) return(false);
      xExtW = xe;
      recalc();
      return(true);
   }
   
   /**
    * Get the Y extent of the 3D box representing the XYZ coordinate system. Its Y-axis range is mapped to this extent.
    * @return Extent of 3D box in the Y-direction, in world units.
    */
   public double getYExtent() { return(yExtW); }

   /**
    * Set the Y extent of the 3D box representing the XYZ coordinate system. Its Y-axis range is mapped to this extent.
    * @param ye Extent of 3D box in the Y-direction, in world units. Strictly positive.
    * @return True if successful; false if argument is invalid.
    */
   public boolean setYExtent(double ye)
   {
      if(!Utilities.isWellDefined(ye) || ye <= 0) return(false);
      yExtW = ye;
      recalc();
      return(true);
   }
   
   /**
    * Get the Z extent of the 3D box representing the XYZ coordinate system. Its Z-axis range is mapped to this extent.
    * @return Extent of 3D box in the Z-direction, in world units. Strictly positive.
    */
   public double getZExtent() { return(zExtW); }

   /**
    * Set the Z extent of the 3D box representing the XYZ coordinate system. Its Z-axis range is mapped to this extent.
    * @param ze Extent of 3D box in the Z-direction, in world units.
    * @return True if successful; false if argument is invalid.
    */
   public boolean setZExtent(double ze)
   {
      if(!Utilities.isWellDefined(ze) || ze <= 0) return(false);
      zExtW = ze;
      recalc();
      return(true);
   }
   
   /**
    * Set the dimensions of the 3D box representing the projected XYZ coordinate system. The remaining parameters of the
    * projection geometry for this 3D-to-2D projector -- the projection scale factor and the rotation and elevation 
    * angles -- are unchanged. All parameters are specified in "world" units, as opposed to "native" units, which are 
    * the units native to the XYZ system. 
    * @param xe X-coordinate extent of the 3D box representing the XYZ coordinate system. Must be positive.
    * @param ye Y-coordinate extent of the 3D box representing the XYZ coordinate system. Must be positive.
    * @param ze Z-coordinate extent of the 3D box representing the XYZ coordinate system. Must be positive.
    * @return True if successful; false if any parameter was invalid.
    */
   public boolean setProjectionGeometry(double xe, double ye, double ze)
   {
      if(!Utilities.isWellDefined(new double[] {xe, ye, ze})) return(false);
      if(xe <= 0 || ye <= 0 || ze <= 0) return(false);
      xExtW = xe;
      yExtW = ye;
      zExtW = ze;
      recalc();
      return(true);
   }
   
   /**
    * Set the projection geometry for this 3D-to-2D projector: the projection distance scale factor, dimensions of the 
    * 3D box representing the projected XYZ coordinate system, and the rotation and elevation angles defining the 
    * orientation of that box in the 3D "world". The dimensions are specified in "world" units, as opposed to "native" 
    * units, which are the units native to the XYZ system. 
    * @param s Projection distance scale factor. Must lie in [2..20].
    * @param xe X-coordinate extent of the 3D box representing the XYZ coordinate system. Must be positive.
    * @param ye Y-coordinate extent of the 3D box representing the XYZ coordinate system. Must be positive.
    * @param ze Z-coordinate extent of the 3D box representing the XYZ coordinate system. Must be positive.
    * @param rot Rotation of XYZ coordinate system about its Z-axis, in degrees CCW [-180..180].
    * @param elev Subsequent rotation of XYZ coordinate system about its X-axis, again in degrees CCW [-90..90].
    * @return True if successful; false if any parameter was invalid.
    */
   public boolean setProjectionGeometry(int s, double xe, double ye, double ze, double rot, double elev)
   {
      if(s < 2 || s > 20) return(false);
      if(!Utilities.isWellDefined(new double[] {xe, ye, ze})) return(false);
      if(xe <= 0 || ye <= 0 || ze <= 0) return(false);
      if((!Utilities.isWellDefined(rot)) || rot < -180 || rot > 180) return(false);
      if((!Utilities.isWellDefined(elev)) || elev < -90 || elev > 90) return(false);
      distScale = s;
      xExtW = xe;
      yExtW = ye;
      zExtW = ze;
      rotDeg = rot;
      elevDeg = elev;
      recalc();
      return(true);
   }
   
   /** X-coordinate of the origin of the XYZ coordinate system in the projection plane (Z=-D), in world units. */
   private double xOriginW;
   
   /**
    * Get the X-coordinate of the origin of the projected XYZ coordinate system in the projection plane. AFTER a
    * 3D point in XYZ is projected onto the the 2D plane, its projected coordinates are offset relative to this origin 
    * -- so that the projected image may be translated to any location on the projection plane.
    * @return X-coordinate of origin in projection plane, in world units.
    */
   public double getXOrigin() { return(xOriginW); }

   /**
    * Set the X-coordinate of the origin of the projected XYZ coordinate system in the projection plane. See {@link 
    * #getXOrigin()}.
    * @param x The X-coordinate in world units. Must be well-defined.
    * @return False if coordinate value is not well-defined; else true.
    */
   public boolean setXOrigin(double x) 
   {
      if(!Utilities.isWellDefined(x)) return(false);
      xOriginW = x;
      return(true);
   }
   
   /** Y-coordinate of the origin of the XYZ coordinate system in the projection plane (Z=-D), in world units. */
   private double yOriginW;
   
   /**
    * Get the Y-coordinate of the origin of the projected XYZ coordinate system in the projection plane. See {@link 
    * #getXOrigin()}.
    * @return Y-coordinate of origin in projection plane, in world units.
    */
   public double getYOrigin() { return(yOriginW); }

   /**
    * Set the Y-coordinate of the origin of the projected XYZ coordinate system in the projection plane. See {@link 
    * #getXOrigin()}.
    * @param y The Y-coordinate in world units. Must be well-defined.
    * @return False if coordinate value is not well-defined; else true.
    */
   public boolean setYOrigin(double y) 
   {
      if(!Utilities.isWellDefined(y)) return(false);
      yOriginW = y;
      return(true);
   }

   /** Rotation of projected coordinate system XYZ about its Z-axis. In deg CCW. */
   private double rotDeg;
   
   /**
    * Get angle by which projected coordinate system XYZ is rotated about its Z-axis. Two sequential rotations determine
    * the orientation of XYZ with respect to the world coordinate system XYZw -- rotation about its Z axis, followed by 
    * a rotation about its X-axis.
    * @return The rotation angle in degrees CCW.
    */
   public double getRotationAngle() { return(rotDeg); }
   
   /**
    * Set angle by which projected coordinate system XYZ is rotated about its Z-axis. See {@link #getRotationAngle()}.
    * @param r Rotation angle in degrees; CCW is positive. Must lie in [-180..180].
    * @return False if angle invalid; else true.
    */
   public boolean setRotationAngle(double r)
   {
      if((!Utilities.isWellDefined(r)) || r < -180 || r > 180) return(false);
      rotDeg = r;
      recalc();
      return(true);
   }
   
   /** Elevation of projected coordinate system XYZ about its X-axis, AFTER rotation about Z. In deg CCW. */
   private double elevDeg;

   /**
    * Get the elevation angle for the projected coordinate system XYZ, i.e., the angle by which it is rotated about its
    * own X-axis, AFTER rotation about Z. See {@link #getRotationAngle()}.
    * @return The elevation angle in degrees CCW.
    */
   public double getElevationAngle() { return(elevDeg); }
   
   /**
    * Set the elevation angle for the projected coordinate system XYZ, i.e., the angle by which it is rotated about its
    * own X-axis, AFTER rotation about Z. See {@link #getRotationAngle()}.
    * 
    * @param e Elevation angle in degrees; CCW is positive. Must lie in [-90..90].
    * @return False if angle invalid; else true.
    */
   public boolean setElevationAngle(double e)
   {
      if((!Utilities.isWellDefined(e)) || e < -90 || e > 90) return(false);
      elevDeg = e;
      recalc();
      return(true);
   }
   
   /** Minimum X-coordinate value in the projected XYZ coordinate system, in that system's units. */
   private double xMin;
   /** Maximum X-coordinate value in the projected XYZ coordinate system, in that system's units. */
   private double xMax;
   /** Minimum Y-coordinate value in the XYZ coordinate system, in that system's units. */
   private double yMin;
   /** Maximum Y-coordinate value in the XYZ coordinate system, in that system's units. */
   private double yMax;
   /** Minimum Z-coordinate value in the XYZ coordinate system, in that system's units. */
   private double zMin;
   /** Maximum Z-coordinate value in the XYZ coordinate system, in that system's units. */
   private double zMax;

   /** Flag set if the X dimension in the XYZ coordinate system is on a log-10 scale; else linear scale. */
   private boolean xLog;
   /** Flag set if the Y dimension in the XYZ coordinate system is on a log-10 scale; else linear scale. */
   private boolean yLog;
   /** Flag set if the Z dimension in the XYZ coordinate system is on a log-10 scale; else linear scale. */
   private boolean zLog;
   
   /** 
    * Get the minimum of the X-coordinate range for the projected XYZ coordinate system.
    * @return Minimum X-coordinate value, in "native" units. If the axis is logarithmic, the actual minimum is the
    * base-10 logarithm of this value.
    */
   public double getXMin() { return(xMin); }
   
   /** 
    * Get the maximum of the X-coordinate range for the projected XYZ coordinate system.
    * @return Maximum X-coordinate value, in "native" units. If the axis is logarithmic, the actual maximum is the
    * base-10 logarithm of this value.
    */
   public double getXMax() { return(xMax); }
   
   /**
    * Get the value of the X that is at the middle of the X-axis range for the projected XYZ coordinate system. Note 
    * that, if the axis is logarithmic, then mid = pow(10, (log10(min)+log10(max))/2.0).
    * @return Value of X at the middle of the X-axis range.
    */
   public double getXMid()
   {
      double d;
      if(xLog)
      {
         d = Utilities.log10(xMin) + Utilities.log10(xMax);
         d = Math.pow(10, d/2.0);
      }
      else
         d = (xMin + xMax)/2.0;
      
      return(d);
   }
   
   /**
    * Set the X-axis range for the projected XYZ coordinate system.
    * @param xm Minimum of the X-axis range, in native units. Must be positive if axis is logarithmic.
    * @param xM Maximum of the X-axis range, in native units. Must be strictly greater than <i>xm</i>.
    * @param isLog True if X axis is logarithmic (base 10) scale; false if linear scale.
    * @return False if coordinate range is ill-defined or invalid. Else true.
    */
   public boolean setXAxisRange(double xm, double xM, boolean isLog)
   {
      if((!Utilities.isWellDefined(xm)) || (!Utilities.isWellDefined(xM)) || xm >= xM) return(false);
      if(isLog && xm <= 0) return(false);
      xMin = xm;
      xMax = xM;
      xLog = isLog;
      recalc();
      return(true);
   }
   
   /** 
    * Get the minimum of the Y-coordinate range for the projected XYZ coordinate system.
    * @return Minimum Y-coordinate value, in "native" units. If the axis is logarithmic, the actual minimum is the
    * base-10 logarithm of this value.
    */
   public double getYMin() { return(yMin); }
   
   /** 
    * Get the maximum of the Y-coordinate range for the projected XYZ coordinate system.
    * @return Maximum Y-coordinate value, in "native" units. If the axis is logarithmic, the actual maximum is the
    * base-10 logarithm of this value.
    */
   public double getYMax() { return(yMax); }
   
   /**
    * Get the value of the Y that is at the middle of the Y-axis range for the projected XYZ coordinate system. Note 
    * that, if the axis is logarithmic, then mid = pow(10, (log10(min)+log10(max))/2.0).
    * @return Value of Y at the middle of the Y-axis range.
    */
   public double getYMid()
   {
      double d;
      if(yLog)
      {
         d = Utilities.log10(yMin) + Utilities.log10(yMax);
         d = Math.pow(10, d/2.0);
      }
      else
         d = (yMin + yMax)/2.0;
      
      return(d);
   }
   
   /**
    * Set the Y-axis range for the projected XYZ coordinate system.
    * @param ym Minimum of the Y-axis range, in native units. Must be positive if axis is logarithmic.
    * @param yM Maximum of the Y-axis range, in native units. Must be strictly greater than <i>ym</i>.
    * @param isLog True if Y axis is logarithmic (base 10) scale; false if linear scale.
    * @return False if coordinate range is ill-defined or invalid. Else true.
    */
   public boolean setYAxisRange(double ym, double yM, boolean isLog)
   {
      if((!Utilities.isWellDefined(ym)) || (!Utilities.isWellDefined(yM)) || ym >= yM) return(false);
      if(isLog && ym <= 0) return(false);
      yMin = ym;
      yMax = yM;
      yLog = isLog;
      recalc();
      return(true);
   }
   
   /** 
    * Get the minimum of the Z-coordinate range for the projected XYZ coordinate system.
    * @return Minimum Z-coordinate value, in "native" units. If the axis is logarithmic, the actual minimum is the
    * base-10 logarithm of this value.
    */
   public double getZMin() { return(zMin); }
   
   /** 
    * Get the maximum of the Z-coordinate range for the projected XYZ coordinate system.
    * @return Maximum Z-coordinate value, in "native" units. If the axis is logarithmic, the actual maximum is the
    * base-10 logarithm of this value.
    */
   public double getZMax() { return(zMax); }
   
   /**
    * Get the value of the Z that is at the middle of the Z-axis range for the projected XYZ coordinate system. Note 
    * that, if the axis is logarithmic, then mid = pow(10, (log10(min)+log10(max))/2.0).
    * @return Value of Z at the middle of the Z-axis range.
    */
   public double getZMid()
   {
      double d;
      if(zLog)
      {
         d = Utilities.log10(zMin) + Utilities.log10(zMax);
         d = Math.pow(10, d/2.0);
      }
      else
         d = (zMin + zMax)/2.0;
      
      return(d);
   }
   
   /**
    * Set the Z-axis range for the projected XYZ coordinate system.
    * @param zm Minimum of the Z-axis range, in native units. Must be positive if axis is logarithmic.
    * @param zM Maximum of the Z-axis range, in native units. Must be strictly greater than <i>zm</i>.
    * @param isLog True if Z axis is logarithmic (base 10) scale; false if linear scale.
    * @return False if coordinate range is ill-defined or invalid. Else true.
    */
   public boolean setZAxisRange(double zm, double zM, boolean isLog)
   {
      if((!Utilities.isWellDefined(zm)) || (!Utilities.isWellDefined(zM)) || zm >= zM) return(false);
      if(isLog && zm <= 0) return(false);
      zMin = zm;
      zMax = zM;
      zLog = isLog;
      recalc();
      return(true);
   }
   
   /**
    * Set the X-, Y-, and Z-axis ranges for the projected XYZ coordinate system. Each range maps to the corresponding 
    * side of the 3D box representing the XYZ system. The ranges are needed in order to convert a point P(x,y,z) from
    * "native" units in XYZ to "world" units in XYZw; after conversion, P is projected onto the 2D plane.
    * 
    * @param xm Minimum of the X-axis range, in native units. Must be positive if the axis is logarithmic.
    * @param xM Maximum of the X-axis range, in native units. Must be strictly greater than <i>xm</i>.
    * @param isLogX True if X axis is logarithmic (base 10) scale; false if linear scale.
    * @param ym Minimum of the Y-axis range, in native units. Must be positive if the axis is logarithmic.
    * @param yM Maximum of the Y-axis range, in native units. Must be strictly greater than <i>ym</i>.
    * @param isLogY True if Y axis is logarithmic (base 10) scale; false if linear scale.
    * @param zm Minimum of the Z-axis range, in native units. Must be positive if the axis is logarithmic.
    * @param zM Maximum of the Z-axis range, in native units. Must be strictly greater than <i>zm</i>
    * @param isLogZ True if Z axis is logarithmic (base 10) scale; false if linear scale.
    * @return False if any coordinate range is ill-defined or invalid. Else true.
    */ 
   public boolean setAxisRanges(double xm, double xM, boolean isLogX, double ym, double yM, boolean isLogY,
         double zm, double zM, boolean isLogZ)
   {
      if((!Utilities.isWellDefined(new double[] {xm, xM, ym, yM, zm, zM})) || xm >= xM || ym >= yM || zm >= zM)
         return(false);
      if((isLogX && xm <= 0) || (isLogY && ym <= 0) || (isLogZ && zm <= 0)) return(false);
      xMin = xm;
      xMax = xM;
      xLog = isLogX;
      yMin = ym;
      yMax = yM;
      yLog = isLogY;
      zMin = zm;
      zMax = zM;
      zLog = isLogZ;
      recalc();
      return(true);
   }
   
   /**
    * Get the X-axis range limit that lies on the back side of the 3D box representing the projected coordinate system 
    * XYZ, given the current configuration of this projector. Typically, the three back sides of the projected box (in 
    * planes X=A, Y=B, Z=C) are colored and display grid lines, while the opposing three sides are "see-through".
    * @return X-axis limit A such that the projected side in plane X=A lies further from the center of projection. 
    */
   public double getBackSideX() { return(backPlaneMinX ? xMin : xMax); }
   
   /**
    * Get the X-axis range limit that lies on the front side of the 3D box representing the projected coordinate system 
    * XYZ, given the current configuration of this projector. See {@link #getBackSideX()}.
    * @return X-axis limit A such that the projected side in plane X=A lies closer to the center of projection.
    */
   public double getFrontSideX() { return(backPlaneMinX ? xMax : xMin); }
   
   /**
    * Get the Y-axis range limit that lies on the back side of the 3D box representing the projected coordinate system 
    * XYZ, given the current configuration of this projector. See {@link #getBackSideX()}.
    * @return Y-axis limit B such that the projected side in plane Y=B lies further from the center of projection.
    */
   public double getBackSideY() {  return(backPlaneMinY ? yMin : yMax); }
   
   /**
    * Get the Y-axis range limit that lies on the front side of the 3D box representing the projected coordinate system 
    * XYZ, given the current configuration of this projector. See {@link #getBackSideX()}.
    * @return Y-axis limit B such that the projected side in plane Y=B lies closer to the center of projection.
    */
   public double getFrontSideY() { return(backPlaneMinY ? yMax : yMin); }
   
   /**
    * Get the Z-axis range limit that lies on the back side of the 3D box representing the projected coordinate system 
    * XYZ, given the current configuration of this projector. See {@link #getBackSideX()}.
    * @return Z-axis limit C such that the projected side in plane Z=C lies further from the center of projection.
    */
   public double getBackSideZ() { return(backPlaneMinZ ? zMin : zMax); }
   
   /**
    * Get the Z-axis range limit that lies on the front side of the 3D box representing the projected coordinate system 
    * XYZ, given the current configuration of this projector. See {@link #getBackSideX()}.
    * @return Z-axis limit C such that the projected side in plane Z=C lies closer to the center of projection.
    */
   public double getFrontSideZ() { return(backPlaneMinZ ? zMax : zMin); } 
   
   /**
    * Project a point in the 3D coordinate system XYZ onto the 2D projection window defined by this projector. The
    * projected point (Xp, Yp) is defined in world units with respect to an origin at ({@link #getXOrigin()}, {@link 
    * #getYOrigin()}) in the projection plane.
    * 
    * @param p3 A point in the arbitrary coordinate system XYZ encapsulated by this 3D-to-2D projector.
    * @param p2 If non-null, the projected point (Xp, Yp) is stored in this object and a reference to it is returned.
    * @return The projected point. 
    */
   public Point2D project(Point3D p3, Point2D p2)
   {
      // transform from native to world units.
      double val = p3.getX();
      double minVal = xMin;
      if(xLog) { val = Utilities.log10(val); minVal = Utilities.log10(minVal); }
      double x = xFactor * (val - minVal) - xExtW / 2.0;
      
      val = p3.getY();
      minVal = yMin;
      if(yLog) { val = Utilities.log10(val); minVal = Utilities.log10(minVal); }
      double y = yFactor * (val - minVal) - yExtW / 2.0;
      
      val = p3.getZ();
      minVal = zMin;
      if(zLog) { val = Utilities.log10(val); minVal = Utilities.log10(minVal); }
      double z = zFactor * (val - minVal) - zExtW / 2.0;
      
      // rotate
      double tmp = x;
      x = (-x) * cosRot + y * sinRot;
      y = -(tmp * sinRot + y * cosRot);
      
      // elevate and project
      double t = distW / (y * cosElev - z * sinElev + distW);
      x = x * t + xOriginW;
      y = (y * sinElev + z * cosElev) * t + yOriginW;
      
      if(p2 == null) p2 = new Point2D.Double(x, y);
      else p2.setLocation(x, y);
      
      return(p2);
   }
   
   /**
    * Project a point in the 3D coordinate system XYZ onto the 2D projection window defined by this projector. The
    * projected point (Xp, Yp) is defined in world units with respect to an origin at ({@link #getXOrigin()}, {@link 
    * #getYOrigin()}) in the projection plane.
    * @param x X-coordinate of a point in the arbitrary coordinate system XYZ encapsulated by this 3D-to-2D projector.
    * @param y Y-coordinate of the point.
    * @param z Z-coordinate of the point.
    * @param p If non-null, the projected point (Xp, Yp) is stored in this object and a reference to it is returned.
    * @return The projected point. 
    */
   public Point2D project(double x, double y, double z, Point2D p)
   {
      // transform from native to world units.
      double minVal = xMin;
      if(xLog) { x = Utilities.log10(x); minVal = Utilities.log10(minVal); }
      x = xFactor * (x - minVal) - xExtW/2.0;
      
      minVal = yMin;
      if(yLog) { y = Utilities.log10(y); minVal = Utilities.log10(minVal); }
      y = yFactor * (y - minVal) - yExtW/2.0;
      
      minVal = zMin;
      if(zLog) { z = Utilities.log10(z); minVal = Utilities.log10(minVal); }
      z = zFactor * (z - minVal) - zExtW/2.0;
      
      // rotate
      double tmp = x;
      x = (-x) * cosRot + y * sinRot;
      y = -(tmp * sinRot + y * cosRot);
      
      // elevate and project
      double t = distW / (y * cosElev - z * sinElev + distW);
      x = x * t + xOriginW;
      y = (y * sinElev + z * cosElev) * t + yOriginW;
      
      if(p == null) p = new Point2D.Double(x, y);
      else p.setLocation(x, y);
      
      return(p);
   }
   
   /**
    * Transform a point in the 3D coordinate system XYZ to world coordinates, then optionally offset the coordinates of
    * the transformed point, and finally project the point onto the 2D projection window defined by this projector. The
    * projected point (Xp, Yp) is defined in world units with respect to an origin at ({@link #getXOrigin()}, {@link 
    * #getYOrigin()}) in the projection plane.
    * @param x X-coordinate of a point in the arbitrary coordinate system XYZ encapsulated by this 3D-to-2D projector.
    * @param y Y-coordinate of the point.
    * @param z Z-coordinate of the point.
    * @param xOfsW X-coordinate offset applied to the transformed point (in world units).
    * @param yOfsW X-coordinate offset applied to the transformed point (in world units).
    * @param zOfsW X-coordinate offset applied to the transformed point (in world units).
    * @param p If non-null, the projected point (Xp, Yp) is stored in this object and a reference to it is returned.
    * @return The projected point. 
    */
   public Point2D offsetAndProject(double x, double y, double z, double xOfsW, double yOfsW, double zOfsW, Point2D p)
   {
      // transform from native to world units.
      double minVal = xMin;
      if(xLog) { x = Utilities.log10(x); minVal = Utilities.log10(minVal); }
      x = xFactor * (x - minVal) - xExtW/2.0 + xOfsW;
      
      minVal = yMin;
      if(yLog) { y = Utilities.log10(y); minVal = Utilities.log10(minVal); }
      y = yFactor * (y - minVal) - yExtW/2.0 + yOfsW;
      
      minVal = zMin;
      if(zLog) { z = Utilities.log10(z); minVal = Utilities.log10(minVal); }
      z = zFactor * (z - minVal) - zExtW/2.0 + zOfsW;
      
      // rotate
      double tmp = x;
      x = (-x) * cosRot + y * sinRot;
      y = -(tmp * sinRot + y * cosRot);
      
      // elevate and project
      double t = distW / (y * cosElev - z * sinElev + distW);
      x = x * t + xOriginW;
      y = (y * sinElev + z * cosElev) * t + yOriginW;
      
      if(p == null) p = new Point2D.Double(x, y);
      else p.setLocation(x, y);
      
      return(p);
   }
   
   /**
    * Compute the rectangle bounding the projection of the 3D coordinate system XYZ onto the 2D projection window
    * defined by this projector. The rectangle is specified in world units with respect to an origin at ({@link 
    * #getXOrigin()}, {@link #getYOrigin()}) in the projection plane.
    * @param r If not null, the bounds are returned in this rectangle. Otherwise, a new rectangle is created.
    * @return The bounding rectangle.
    */
   public Rectangle2D getBounds2D(Rectangle2D r)
   {
      Rectangle2D res = (r != null) ? r : new Rectangle2D.Double();
      
      double xm = Double.POSITIVE_INFINITY;
      double xM = Double.NEGATIVE_INFINITY;
      double ym = Double.POSITIVE_INFINITY;
      double yM = Double.NEGATIVE_INFINITY;
      
      Point2D p = new Point2D.Double();
      for(int i=0; i<2; i++)
      {
         double x = (i==0) ? xMin : xMax;
         for(int j=0; j<2; j++)
         {
            double y = (j==0) ? yMin : yMax;
            for(int k=0; k<2; k++)
            {
               double z = (k==0) ? zMin : zMax;
               project(x, y, z, p);
               if(p.getX() < xm) xm = p.getX();
               if(p.getX() > xM) xM = p.getX();
               if(p.getY() < ym) ym = p.getY();
               if(p.getY() > yM) yM = p.getY();
            }
         }
      }

      res.setFrame(xm, ym, xM-xm, yM-ym);
      return(res);
   }
   

   /**
    * Get the shape outlining the XY backplane of the XYZ coordinate system once projected onto the 2D projection window
    * as specified by this projector.
    * @return The 4-sided closed polygon that bounds the projected XY backplane. In "world" units.
    */
   public Shape getXYBackplaneShape()
   {
      Point2D p = new Point2D.Double();
      GeneralPath gp = new GeneralPath();
      double zBack = getBackSideZ();
      project(xMin, yMin, zBack, p);
      gp.moveTo(p.getX(), p.getY());
      project(xMax, yMin, zBack, p);
      gp.lineTo(p.getX(), p.getY());
      project(xMax, yMax, zBack, p);
      gp.lineTo(p.getX(), p.getY());
      project(xMin, yMax, zBack, p);
      gp.lineTo(p.getX(), p.getY());
      gp.closePath();

      return(gp);
   }
   
   /**
    * Get the vertices of the 4-sided closed polygon outlining the XY backplane of the XYZ coordinate system once
    * projected on the 2D projection window as specified by this projector.
    * @return The vertex list, in connection order and in "world" units. The last vertex must be connected to the first 
    * to close the path.
    */
   public List<Point2D> getXYBackplaneVertices()
   {
      List<Point2D> pts = new ArrayList<>();
      double zBack = getBackSideZ();
      pts.add(project(xMin, yMin, zBack, null));
      pts.add(project(xMax, yMin, zBack, null));
      pts.add(project(xMax, yMax, zBack, null));
      pts.add(project(xMin, yMax, zBack, null));
      return(pts);
   }
   
   /**
    * Get the shape outlining the XZ backplane of the XYZ coordinate system once projected onto the 2D projection window
    * as specified by this projector.
    * @return The 4-sided closed polygon that bounds the projected XZ backplane. In "world" units.
    */
   public Shape getXZBackplaneShape()
   {
      Point2D p = new Point2D.Double();
      GeneralPath gp = new GeneralPath();
      double yBack = getBackSideY();
      project(xMin, yBack, zMin, p);
      gp.moveTo(p.getX(), p.getY());
      project(xMax, yBack, zMin, p);
      gp.lineTo(p.getX(), p.getY());
      project(xMax, yBack, zMax, p);
      gp.lineTo(p.getX(), p.getY());
      project(xMin, yBack, zMax, p);
      gp.lineTo(p.getX(), p.getY());
      gp.closePath();

      return(gp);
   }
   
   /**
    * Get the vertices of the 4-sided closed polygon outlining the XZ backplane of the XYZ coordinate system once
    * projected on the 2D projection window as specified by this projector.
    * @return The vertex list, in connection order and in "world" units. The last vertex must be connected to the first 
    * to close the path.
    */
   public List<Point2D> getXZBackplaneVertices()
   {
      List<Point2D> pts = new ArrayList<>();
      double yBack = getBackSideY();
      pts.add(project(xMin, yBack, zMin, null));
      pts.add(project(xMax, yBack, zMin, null));
      pts.add(project(xMax, yBack, zMax, null));
      pts.add(project(xMin, yBack, zMax, null));
      return(pts);
   }
   
   /**
    * Get the shape outlining the YZ backplane of the XYZ coordinate system once projected onto the 2D projection window
    * as specified by this projector.
    * @return The 4-sided closed polygon that bounds the projected YZ backplane. In "world" units.
    */
   public Shape getYZBackplaneShape()
   {
      Point2D p = new Point2D.Double();
      GeneralPath gp = new GeneralPath();
      double xBack = getBackSideX();
      project(xBack, yMin, zMin, p);
      gp.moveTo(p.getX(), p.getY());
      project(xBack, yMax, zMin, p);
      gp.lineTo(p.getX(), p.getY());
      project(xBack, yMax, zMax, p);
      gp.lineTo(p.getX(), p.getY());
      project(xBack, yMin, zMax, p);
      gp.lineTo(p.getX(), p.getY());
      gp.closePath();

      return(gp);
   }
   
   /**
    * Get the vertices of the 4-sided closed polygon outlining the YZ backplane of the XYZ coordinate system once
    * projected on the 2D projection window as specified by this projector.
    * @return The vertex list, in connection order and in "world" units. The last vertex must be connected to the first 
    * to close the path.
    */
   public List<Point2D> getYZBackplaneVertices()
   {
      List<Point2D> pts = new ArrayList<>();
      double xBack = getBackSideX();
      pts.add(project(xBack, yMin, zMin, null));
      pts.add(project(xBack, yMax, zMin, null));
      pts.add(project(xBack, yMax, zMax, null));
      pts.add(project(xBack, yMin, zMax, null));
      return(pts);
   }
   
   /**
    * Get the sequence of points defining a path tracing the grid lines perpendicular to the X-axis of the XYZ 
    * coordinate system once projected onto the 2D projection window as specified by this projector.
    * @param vals The values of X at which the constant-X grid lines are drawn in the XZ and XY backplanes. Any X value
    * that is outside the current X-axis range is skipped.
    * @param xyOnly If true, prepares vertices for grid lines in the XY backplane only; else, in XY and XZ planes.
    * @return The list of path points defining the grid lines. To render the path, move to the first point in the list
    * and "connect the dots", inserting a "gap" in the path for each null or ill-defined point encountered. The list 
    * could be empty if the argument is null or empty or contains no well-defined values
    */
   public List<Point2D> getXGridLineVertices(double[] vals, boolean xyOnly)
   {
      List<Point2D> pts = new ArrayList<>();
      if(vals != null) for (double val : vals) {
          if ((!Utilities.isWellDefined(val)) || val < xMin || val > xMax) continue;
          if (!xyOnly) pts.add(project(val, getBackSideY(), getFrontSideZ(), null));
          pts.add(project(val, getBackSideY(), getBackSideZ(), null));
          pts.add(project(val, getFrontSideY(), getBackSideZ(), null));
          pts.add(null);
      }
      return(pts);
   }
   
   /**
    * Get the sequence of points defining a path tracing the grid lines perpendicular to the Y-axis of the XYZ 
    * coordinate system once projected onto the 2D projection window as specified by this projector.
    * @param vals The values of Y at which the constant-Y grid lines are drawn in the YZ and XY backplanes. Any Y value
    * that is outside the current Y-axis range is skipped.
    * @param xyOnly If true, prepares vertices for grid lines in the XY backplane only; else, in XY and YZ planes.
    * @return The list of path points defining the grid lines. To render the path, move to the first point in the list
    * and "connect the dots", inserting a "gap" in the path for each null or ill-defined point encountered. The list 
    * could be empty if the argument is null or empty or contains no well-defined values
    */
   public List<Point2D> getYGridLineVertices(double[] vals, boolean xyOnly)
   {
      List<Point2D> pts = new ArrayList<>();
      if(vals != null) for (double val : vals) {
          if ((!Utilities.isWellDefined(val)) || val < yMin || val > yMax) continue;
          if (!xyOnly) pts.add(project(getBackSideX(), val, getFrontSideZ(), null));
          pts.add(project(getBackSideX(), val, getBackSideZ(), null));
          pts.add(project(getFrontSideX(), val, getBackSideZ(), null));
          pts.add(null);
      }
      return(pts);
   }
   
   /**
    * Get the sequence of points defining a path tracing the grid lines perpendicular to the Z-axis of the XYZ 
    * coordinate system once projected onto the 2D projection window as specified by this projector.
    * @param vals The values of Z at which the constant-Z grid lines are drawn in the XZ and YZ backplanes. Any Z value
    * that is outside the current Z-axis range is skipped.
    * @return The list of path points defining the grid lines. To render the path, move to the first point in the list
    * and "connect the dots", inserting a "gap" in the path for each null or ill-defined point encountered. The list 
    * could be empty if the argument is null or empty or contains no well-defined values
    */
   public List<Point2D> getZGridLineVertices(double[] vals)
   {
      List<Point2D> pts = new ArrayList<>();
      if(vals != null) for (double val : vals) {
          if ((!Utilities.isWellDefined(val)) || val < zMin || val > zMax) continue;
          pts.add(project(getFrontSideX(), getBackSideY(), val, null));
          pts.add(project(getBackSideX(), getBackSideY(), val, null));
          pts.add(project(getBackSideX(), getFrontSideY(), val, null));
          pts.add(null);
      }
      return(pts);
   }
   
   
   /**
    * Get the path that outlines the front three edges that complete the 3D box representation of the XYZ coordinate
    * system once projected onto the 2D projection window as specified by this projector.
    * @return The path defining the "front" of the 3D box, in "world" units. Intended to be stroked, not filled.
    */
   public Shape getBoxFrontPath()
   {
      GeneralPath gp = new GeneralPath();
      Point2D p = boxCorners[Corner.YZFRONTXBACK.ordinal()]; gp.moveTo(p.getX(), p.getY());
      p = boxCorners[Corner.XYZFRONT.ordinal()]; gp.lineTo(p.getX(), p.getY());
      p = boxCorners[Corner.XYFRONTZBACK.ordinal()]; gp.lineTo(p.getX(), p.getY());
      p = boxCorners[Corner.XYZFRONT.ordinal()]; gp.moveTo(p.getX(), p.getY());
      p = boxCorners[Corner.XZFRONTYBACK.ordinal()]; gp.lineTo(p.getX(), p.getY());

      return(gp);
   }
   
   /**
    * Get the sequence of vertices that, when connected in a poly-line, render the front three edges that complete the 
    * 3D box representation of the XYZ coordinate system once projected onto the 2D projection window as specified by
    * this projector. An ill-defined or null vertex in the sequence represents a "gap" in the polyline (a "moveto").
    * @return The sequence of vertices, in "world" units.
    */
   public List<Point2D> getBoxFrontPathVertices()
   {
      List<Point2D> pts = new ArrayList<>();
      pts.add((Point2D) boxCorners[Corner.YZFRONTXBACK.ordinal()].clone());
      pts.add((Point2D) boxCorners[Corner.XYZFRONT.ordinal()].clone());
      pts.add((Point2D) boxCorners[Corner.XYFRONTZBACK.ordinal()].clone());
      pts.add(null);
      pts.add((Point2D) boxCorners[Corner.XYZFRONT.ordinal()].clone());
      pts.add((Point2D) boxCorners[Corner.XZFRONTYBACK.ordinal()].clone());
      return(pts);
   }
   
   /**
    * Get the path that outlines the 3D box representation of the XYZ coordinate system once projected onto the 2D 
    * projection plane as specified by this projector.
    * 
    * @param full If true, all 12 edges of the 3D box are outlined. Otherwise, only the six outer edges of the three
    * "back sides" of the box are outlined, forming a closed six-sided polygon. At most orientations (but not all!) this
    * polygon tightly bounds the projection of the 3D "box" onto the 2D plane.
    * @param grow If non-zero, the outline path is scaled such that the rectangle bounding the scaled outline is
    * wider and higher (or narrower and shorter, if negative) than the initial bounding rectangle by this amount (in 
    * "world" units).
    * @return The box outline as described, in "world" units.
    */
   public Shape getBoxOutline(boolean full, double grow)
   {
      Point2D p;
      
      // six-sided polygon outlining the six outer edges of the 3 back sides
      GeneralPath gp = new GeneralPath();
      p = boxCorners[Corner.ZFRONTXYBACK.ordinal()]; gp.moveTo(p.getX(), p.getY());
      p = boxCorners[Corner.XZFRONTYBACK.ordinal()]; gp.lineTo(p.getX(), p.getY());
      p = boxCorners[Corner.XFRONTYZBACK.ordinal()]; gp.lineTo(p.getX(), p.getY());
      p = boxCorners[Corner.XYFRONTZBACK.ordinal()]; gp.lineTo(p.getX(), p.getY());
      p = boxCorners[Corner.YFRONTXZBACK.ordinal()]; gp.lineTo(p.getX(), p.getY());
      p = boxCorners[Corner.YZFRONTXBACK.ordinal()]; gp.lineTo(p.getX(), p.getY());
      gp.closePath();
      
      if(full)
      {
         // the 3 front-facing edges
         p = boxCorners[Corner.YZFRONTXBACK.ordinal()]; gp.moveTo(p.getX(), p.getY());
         p = boxCorners[Corner.XYZFRONT.ordinal()]; gp.lineTo(p.getX(), p.getY());
         p = boxCorners[Corner.XYFRONTZBACK.ordinal()]; gp.lineTo(p.getX(), p.getY());
         p = boxCorners[Corner.XYZFRONT.ordinal()]; gp.moveTo(p.getX(), p.getY());
         p = boxCorners[Corner.XZFRONTYBACK.ordinal()]; gp.lineTo(p.getX(), p.getY());

         // the 3 back-facing edges
         p = boxCorners[Corner.ZFRONTXYBACK.ordinal()]; gp.moveTo(p.getX(), p.getY());
         p = boxCorners[Corner.XYZBACK.ordinal()]; gp.lineTo(p.getX(), p.getY());
         p = boxCorners[Corner.XFRONTYZBACK.ordinal()]; gp.lineTo(p.getX(), p.getY());
         p = boxCorners[Corner.XYZBACK.ordinal()]; gp.moveTo(p.getX(), p.getY());
         p = boxCorners[Corner.YFRONTXZBACK.ordinal()]; gp.lineTo(p.getX(), p.getY());
      }
      
      if(grow != 0)
      {
         Rectangle2D bounds = gp.getBounds2D();
         double w = bounds.getWidth();
         double h = bounds.getHeight();
         if(grow < 0) grow = Math.min(grow, Math.min(w/2, h/2));
         AffineTransform at = AffineTransform.getTranslateInstance(xOriginW, yOriginW);
         at.scale((w+grow)/w, (h+grow)/h);
         at.translate(-xOriginW, -yOriginW);
         gp.transform(at);
      }
      return(gp);
   }
   
   /** Enumeration of the 12 edges of the projection of a 3D box outline of the XYZ coordinate system. */
   public enum Edge
   {
      /** Box edge at intersection of front-side planes in X and Y. */ XYFRONT,
      /** Box edge at intersection of front-side plane in X and back-side plane in Y. */ XFRONTYBACK,
      /** Box edge at intersection of back-side plane in X and front-side plane in Y. */ XBACKYFRONT,
      /** Box edge at intersection of back-side planes in X and Y. */ XYBACK,
      /** Box edge at intersection of front-side planes in X and Z. */ XZFRONT,
      /** Box edge at intersection of front-side plane in X and back-side plane in Z. */ XFRONTZBACK,
      /** Box edge at intersection of back-side plane in X and front-side plane in Z. */ XBACKZFRONT,
      /** Box edge at intersection of back-side planes in X and Z. */ XZBACK,
      /** Box edge at intersection of front-side planes in Y and Z. */ YZFRONT,
      /** Box edge at intersection of front-side plane in Y and back-side plane in Z. */ YFRONTZBACK,
      /** Box edge at intersection of back-side plane in Y and front-side plane in Z. */ YBACKZFRONT,
      /** Box edge at intersection of back-side planes in Y and Z. */ YZBACK
   }

    /**
    * Is the specified point near enough to one of the 12 edges of the projection of the 3D box outline of the XYZ
    * coordinate system?
    * 
    * @param e The box edge
    * @param p The test point, in world units.
    * @param tol Nearness tolerance value, in world units.
    * @return True if the squared distance between the test point and the edge is &le; this value squared; else false.
    * Also returns false if test point or edge is undefined.
    */
   public boolean isNearEdge(Edge e, Point2D p, double tol)
   {
      if(e == null || !Utilities.isWellDefined(p)) return(false);
      Point2D p1, p2;
      switch(e)
      {
      case XYFRONT: 
         p1 = boxCorners[Corner.XYFRONTZBACK.ordinal()]; p2 = boxCorners[Corner.XYZFRONT.ordinal()]; break;
      case XFRONTYBACK: 
         p1 = boxCorners[Corner.XZFRONTYBACK.ordinal()]; p2 = boxCorners[Corner.XFRONTYZBACK.ordinal()]; break;
      case XBACKYFRONT: 
         p1 = boxCorners[Corner.YZFRONTXBACK.ordinal()]; p2 = boxCorners[Corner.YFRONTXZBACK.ordinal()]; break;
      case XYBACK: 
         p1 = boxCorners[Corner.ZFRONTXYBACK.ordinal()]; p2 = boxCorners[Corner.XYZBACK.ordinal()]; break;
      case XZFRONT: 
         p1 = boxCorners[Corner.XZFRONTYBACK.ordinal()]; p2 = boxCorners[Corner.XYZFRONT.ordinal()]; break;
      case XFRONTZBACK: 
         p1 = boxCorners[Corner.XYFRONTZBACK.ordinal()]; p2 = boxCorners[Corner.XFRONTYZBACK.ordinal()]; break;
      case XBACKZFRONT: 
         p1 = boxCorners[Corner.YZFRONTXBACK.ordinal()]; p2 = boxCorners[Corner.ZFRONTXYBACK.ordinal()]; break;
      case XZBACK: 
         p1 = boxCorners[Corner.YFRONTXZBACK.ordinal()]; p2 = boxCorners[Corner.XYZBACK.ordinal()]; break;
      case YZFRONT: 
         p1 = boxCorners[Corner.YZFRONTXBACK.ordinal()]; p2 = boxCorners[Corner.XYZFRONT.ordinal()]; break;
      case YFRONTZBACK: 
         p1 = boxCorners[Corner.XYFRONTZBACK.ordinal()]; p2 = boxCorners[Corner.YFRONTXZBACK.ordinal()]; break;
      case YBACKZFRONT: 
         p1 = boxCorners[Corner.XZFRONTYBACK.ordinal()]; p2 = boxCorners[Corner.ZFRONTXYBACK.ordinal()]; break;
      case YZBACK: 
      default: 
         p1 = boxCorners[Corner.XFRONTYZBACK.ordinal()]; p2 = boxCorners[Corner.XYZBACK.ordinal()]; break;
      }
      
      return(Line2D.ptSegDistSq(p1.getX(), p1.getY(), p2.getX(), p2.getY(), p.getX(), p.getY()) <= tol*tol);
   }
   
   /**
    * Get the line in the 2D projection plane that contains the specified edge of the 3D box outline of the XYZ 
    * coordinate system represented by this projector.
    * @param e The box edge. If null, {@link Edge#XYFRONT} is assumed.
    * @return The (infinite) line containing that edge. 
    */
   public LineXY getLineContainingEdge(Edge e)
   {
      if(e == null) e = Edge.XYFRONT;
      Point2D p1, p2;
      switch(e)
      {
      case XYFRONT: 
         p1 = boxCorners[Corner.XYFRONTZBACK.ordinal()]; p2 = boxCorners[Corner.XYZFRONT.ordinal()]; break;
      case XFRONTYBACK: 
         p1 = boxCorners[Corner.XZFRONTYBACK.ordinal()]; p2 = boxCorners[Corner.XFRONTYZBACK.ordinal()]; break;
      case XBACKYFRONT: 
         p1 = boxCorners[Corner.YZFRONTXBACK.ordinal()]; p2 = boxCorners[Corner.YFRONTXZBACK.ordinal()]; break;
      case XYBACK: 
         p1 = boxCorners[Corner.ZFRONTXYBACK.ordinal()]; p2 = boxCorners[Corner.XYZBACK.ordinal()]; break;
      case XZFRONT: 
         p1 = boxCorners[Corner.XZFRONTYBACK.ordinal()]; p2 = boxCorners[Corner.XYZFRONT.ordinal()]; break;
      case XFRONTZBACK: 
         p1 = boxCorners[Corner.XYFRONTZBACK.ordinal()]; p2 = boxCorners[Corner.XFRONTYZBACK.ordinal()]; break;
      case XBACKZFRONT: 
         p1 = boxCorners[Corner.YZFRONTXBACK.ordinal()]; p2 = boxCorners[Corner.ZFRONTXYBACK.ordinal()]; break;
      case XZBACK: 
         p1 = boxCorners[Corner.YFRONTXZBACK.ordinal()]; p2 = boxCorners[Corner.XYZBACK.ordinal()]; break;
      case YZFRONT: 
         p1 = boxCorners[Corner.YZFRONTXBACK.ordinal()]; p2 = boxCorners[Corner.XYZFRONT.ordinal()]; break;
      case YFRONTZBACK: 
         p1 = boxCorners[Corner.XYFRONTZBACK.ordinal()]; p2 = boxCorners[Corner.YFRONTXZBACK.ordinal()]; break;
      case YBACKZFRONT: 
         p1 = boxCorners[Corner.XZFRONTYBACK.ordinal()]; p2 = boxCorners[Corner.ZFRONTXYBACK.ordinal()]; break;
      case YZBACK: 
      default: 
         p1 = boxCorners[Corner.XFRONTYZBACK.ordinal()]; p2 = boxCorners[Corner.XYZBACK.ordinal()]; break;
      }
      return(new LineXY(p1, p2));
   }
   
   /**
    * Get the point in the 2D projection plane that lies at the midpoint of the specified edge of the 3D box outline of
    * the XYZ coordinate system represented by this projector.
    * 
    * @param e The box edge. If null, {@link Edge#XYFRONT} is assumed.
    * @return The midpoint of the specified edge, in 2D projection plane.
    */
   public Point2D getEdgeMidpoint(Edge e)
   {
      if(e == null) e = Edge.XYFRONT;
      Point2D p1, p2;
      switch(e)
      {
      case XYFRONT: 
         p1 = boxCorners[Corner.XYFRONTZBACK.ordinal()]; p2 = boxCorners[Corner.XYZFRONT.ordinal()]; break;
      case XFRONTYBACK: 
         p1 = boxCorners[Corner.XZFRONTYBACK.ordinal()]; p2 = boxCorners[Corner.XFRONTYZBACK.ordinal()]; break;
      case XBACKYFRONT: 
         p1 = boxCorners[Corner.YZFRONTXBACK.ordinal()]; p2 = boxCorners[Corner.YFRONTXZBACK.ordinal()]; break;
      case XYBACK: 
         p1 = boxCorners[Corner.ZFRONTXYBACK.ordinal()]; p2 = boxCorners[Corner.XYZBACK.ordinal()]; break;
      case XZFRONT: 
         p1 = boxCorners[Corner.XZFRONTYBACK.ordinal()]; p2 = boxCorners[Corner.XYZFRONT.ordinal()]; break;
      case XFRONTZBACK: 
         p1 = boxCorners[Corner.XYFRONTZBACK.ordinal()]; p2 = boxCorners[Corner.XFRONTYZBACK.ordinal()]; break;
      case XBACKZFRONT: 
         p1 = boxCorners[Corner.YZFRONTXBACK.ordinal()]; p2 = boxCorners[Corner.ZFRONTXYBACK.ordinal()]; break;
      case XZBACK: 
         p1 = boxCorners[Corner.YFRONTXZBACK.ordinal()]; p2 = boxCorners[Corner.XYZBACK.ordinal()]; break;
      case YZFRONT: 
         p1 = boxCorners[Corner.YZFRONTXBACK.ordinal()]; p2 = boxCorners[Corner.XYZFRONT.ordinal()]; break;
      case YFRONTZBACK: 
         p1 = boxCorners[Corner.XYFRONTZBACK.ordinal()]; p2 = boxCorners[Corner.YFRONTXZBACK.ordinal()]; break;
      case YBACKZFRONT: 
         p1 = boxCorners[Corner.XZFRONTYBACK.ordinal()]; p2 = boxCorners[Corner.ZFRONTXYBACK.ordinal()]; break;
      case YZBACK: 
      default: 
         p1 = boxCorners[Corner.XFRONTYZBACK.ordinal()]; p2 = boxCorners[Corner.XYZBACK.ordinal()]; break;
      }

      p1.setLocation((p1.getX()+p2.getX())/2.0, (p1.getY()+p2.getY())/2.0);
      return(p1);
   }
   
   /** Enumeration of the 8 corners of the projection of a 3D box outline of the XYZ coordinate system. */
   public enum Corner
   {
      /** Corner at intersection of X,Y and Z back-side planes. */ XYZBACK,
      /** Corner at intersection of X front-side and Y,Z back-side planes. */ XFRONTYZBACK,
      /** Corner at intersection of Y front-side and X,Z back-side planes. */ YFRONTXZBACK,
      /** Corner at intersection of X,Y front-side and Z back-side planes. */ XYFRONTZBACK,
      /** Corner at intersection of Z front-side and X,Y back-side planes. */ ZFRONTXYBACK,
      /** Corner at intersection of X,Z front-side and Y back-side planes. */ XZFRONTYBACK,
      /** Corner at intersection of Y,Z front-side and Z back-side planes. */ YZFRONTXBACK,
      /** Corner at intersection of X,Y and Z front-side planes. */ XYZFRONT
   }
   
   /**
    * Is the specified point near enough to one of the 8 corners of the projection of the 3D box outline of the XYZ
    * coordinate system?
    * 
    * @param c One corner of the 3D box outline.
    * @param p The test point, in world units.
    * @param tol Nearness tolerance value, in world units.
    * @return True if the squared distance between the test point and the corner is &le; the squared tolerance; else 
    * false. Also returns false if test point or corner is undefined.
    */
   public boolean isNearCorner(Corner c, Point2D p, double tol)
   {
      return(c != null && Utilities.isWellDefined(p) && (boxCorners[c.ordinal()].distanceSq(p) < tol * tol));
   }
   
   
   /** 
    * Perpendicular distance D from center of projection (0,0,0) to projection window, in "world" units. This is
    * computed from the projection scale factor and the maximum dimension of the 3D box.
    */
   private double distW;

   /** Multiplier converts X-coordinate from units in XYZ coordinate system to world units in XYZw. */
   private double xFactor;
   /** Multiplier converts Y-coordinate from units in XYZ coordinate system to world units in XYZw. */
   private double yFactor;
   /** Multiplier converts Z-coordinate from units in XYZ coordinate system to world units in XYZw. */
   private double zFactor;
   /** Cosine of the rotation angle. */
   private double cosRot;
   /** Sine of the rotation angle. */
   private double sinRot;
   /** Cosine of the elevation angle. */
   private double cosElev;
   /** Sine of the elevation angle. */
   private double sinElev;
   
   /** True if X=Xmin is a "back side" plane; else X=Xmax is the back side (further from center of projection). */
   private boolean backPlaneMinX = false;
   /** True if Y=Ymin is a "back side" plane; else Y=Ymax is the back side (further from center of projection). */
   private boolean backPlaneMinY = false;
   /** True if Z=Zmin is a "back side" plane; else Z=Zmax is the back side (further from center of projection). */
   private boolean backPlaneMinZ = false;
   
   /** 
    * Corners of the 3D box outline of the XYZ coordinate system, projected onto the 2D plane, in world units. 
    * <i>Listed in the same order as the ordinal values of the enum {@link Corner}, so that we can use the enumeration
    * to index the array.</i>
    */
   private Point2D[] boxCorners = new Point2D[8];
   
   private void recalc()
   {
      distW = ((double)distScale) * Math.max(xExtW, Math.max(yExtW, zExtW));
      
      double minVal = xLog ? Utilities.log10(xMin) : xMin;
      double maxVal = xLog ? Utilities.log10(xMax) : xMax;
      xFactor = xExtW / (maxVal - minVal);
      
      minVal = yLog ? Utilities.log10(yMin) : yMin;
      maxVal = yLog ? Utilities.log10(yMax) : yMax;
      yFactor = yExtW / (maxVal - minVal);
      
      minVal = zLog ? Utilities.log10(zMin) : zMin;
      maxVal = zLog ? Utilities.log10(zMax) : zMax;
      zFactor = zExtW / (maxVal - minVal);
      
      cosRot = Math.cos(Math.PI * rotDeg / 180.0);
      sinRot = Math.sin(Math.PI * rotDeg / 180.0);
      cosElev = Math.cos(Math.PI * elevDeg / 180.0);
      sinElev = Math.sin(Math.PI * elevDeg / 180.0);
      
      // to figure out whether the plane x=xMin or x=xMax is the backplane in the X dimension, we transform a point at
      // the center of the two planes into world coordinates and calculate its distance from the camera at (0,0,0).
      // Whichever is further is the backplane. Analogously for Y and Z.
      double midX = xLog ? Math.pow(10, (Utilities.log10(xMin)+Utilities.log10(xMax))/2.0) : (xMin + xMax) / 2.0;
      double midY = yLog ? Math.pow(10, (Utilities.log10(yMin)+Utilities.log10(yMax))/2.0) : (yMin + yMax) / 2.0;
      double midZ = zLog ? Math.pow(10, (Utilities.log10(zMin)+Utilities.log10(zMax))/2.0) : (zMin + zMax) / 2.0;
      
      Point3D p3 = new Point3D(xMin, midY, midZ);
      transformToWorldCoords(p3);
      double dSq = p3.distanceSq(0, 0, 0);
      p3.setLocation(xMax, midY, midZ);
      transformToWorldCoords(p3);
      backPlaneMinX = (dSq >= p3.distanceSq(0, 0, 0));

      p3.setLocation(midX, yMin, midZ);
      transformToWorldCoords(p3);
      dSq = p3.distanceSq(0, 0, 0);
      p3.setLocation(midX, yMax, midZ);
      transformToWorldCoords(p3);
      backPlaneMinY = (dSq >= p3.distanceSq(0, 0, 0));

      p3.setLocation(midX, midY, zMin);
      transformToWorldCoords(p3);
      dSq = p3.distanceSq(0, 0, 0);
      p3.setLocation(midX, midY, zMax);
      transformToWorldCoords(p3);
      backPlaneMinZ = (dSq >= p3.distanceSq(0, 0, 0));
      
      // calculate the 8 corners of the 3D box, projected onto 2D
      project(getBackSideX(), getBackSideY(), getBackSideZ(), boxCorners[Corner.XYZBACK.ordinal()]);
      project(getFrontSideX(), getBackSideY(), getBackSideZ(), boxCorners[Corner.XFRONTYZBACK.ordinal()]);
      project(getBackSideX(), getFrontSideY(), getBackSideZ(), boxCorners[Corner.YFRONTXZBACK.ordinal()]);
      project(getFrontSideX(), getFrontSideY(), getBackSideZ(), boxCorners[Corner.XYFRONTZBACK.ordinal()]);
      project(getBackSideX(), getBackSideY(), getFrontSideZ(), boxCorners[Corner.ZFRONTXYBACK.ordinal()]);
      project(getFrontSideX(), getBackSideY(), getFrontSideZ(), boxCorners[Corner.XZFRONTYBACK.ordinal()]);
      project(getBackSideX(), getFrontSideY(), getFrontSideZ(), boxCorners[Corner.YZFRONTXBACK.ordinal()]);
      project(getFrontSideX(), getFrontSideY(), getFrontSideZ(), boxCorners[Corner.XYZFRONT.ordinal()]);
   }
   
   /**
    * Helper method transforms a point in the arbitrary coordinate system XYZ to the "world" coordinate system as
    * embodied by the projector parameters. It is used to determine which three sides of the rectangular prism that 
    * represents the XYZ system are further from the center of projection, which is the origin in world coordinates.
    * @param p3 A point in XYZ native coordinates. Upon return, the point is converted to world coordinates.
    */
   private void transformToWorldCoords(Point3D p3)
   {
      // transform from native to world units.
      double val = p3.getX();
      double minVal = xMin;
      if(xLog) { val = Utilities.log10(val); minVal = Utilities.log10(minVal); }
      double x = xFactor * (val - minVal) - xExtW / 2.0;
      
      val = p3.getY();
      minVal = yMin;
      if(yLog) { val = Utilities.log10(val); minVal = Utilities.log10(minVal); }
      double y = yFactor * (val - minVal) - yExtW / 2.0;
      
      val = p3.getZ();
      minVal = zMin;
      if(zLog) { val = Utilities.log10(val); minVal = Utilities.log10(minVal); }
      double z = zFactor * (val - minVal) - zExtW / 2.0;
      
      // rotate
      double tmp = x;
      x = (-x) * cosRot + y * sinRot;
      y = -(tmp * sinRot + y * cosRot);
      
      // elevate
      tmp = y;
      y = y * sinElev + z * cosElev;
      z = -distW + z * sinElev - tmp * cosElev;
      
      p3.setLocation(x, y, z);
   }

   @Override public Object clone() throws CloneNotSupportedException
   {
      Projector copy = (Projector) super.clone();
      copy.boxCorners = new Point2D[8];
      for(int i=0; i<8; i++) copy.boxCorners[i] = new Point2D.Double();
      copy.recalc();
      return(copy);
   }
   
}
