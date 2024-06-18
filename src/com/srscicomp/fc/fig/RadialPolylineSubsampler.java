package com.srscicomp.fc.fig;

import java.awt.geom.Point2D;

import com.srscicomp.common.util.Utilities;

/**
 * <code>RadialPolylineSubsampler</code> implements a basic radial distance-based polyline sub-sampling algorithm that
 * preserves discontinuities in the original polyline. It is a utility class intended only for use by the various data 
 * point iterator objects that the data presentation nodes use to "produce" the list of data points to be rendered.
 * 
 * <p><i>Usage</i>. Construct the polyline sub-sampler, specifying the radial tolerance to be applied. Supply each 
 * point in the original polyline to the {@link #keep} method and include the point in the sub-sampled result if that
 * method returns true. The algorithm keeps track of the last point included in the sub-sampled polyline; each 
 * subsequent point that is within tolerance of this last point is skipped.</p>
 * 
 * @author sruffner
 */
class RadialPolylineSubsampler
{
   /**
    * Construct a radial distance-based polyline sub-sampler using the specified tolerance.
    * @param tol Each subsequent data point in the original polyline that is within this distance of the last data
    * point included in the sub-sampled output will be skipped. If zero, a value of 1 is assumed.
    */
   RadialPolylineSubsampler(double tol)
   {
      sqTol = (tol == 0) ? 1 : tol*tol;
      isDiscontinuity = true;
      pLast = new Point2D.Double(Double.NaN, Double.NaN);
   }
   
   /**
    * Test whether or not the next data point in the original polyline should be kept in the sub-sampled version. The
    * radial distance-based algorithm works as follows:
    * <ul>
    * <li>Any ill-defined points at the beginning or end of the original polyline are not included in the sub-sampled
    * version. Other ill-defined points are kept, except that two or more ill-defined points in a row are represented by
    * as single ill-defined point in the sub-sampled output. Such points represent discontiuities in the original
    * polyline.</li>
    * <li>Otherwise, if the last point "kept" in the sub-sampled output is well-defined, the next point kept will be
    * either an ill-defined point or the next well-defined point that is beyond the sub-sampler's tolerance of the last
    * point.</li>
    * </ul>
    * @param p The data point to test. A null point is treated as ill-defined.
    * @return True if the data point should be kept in the sub-sampled version of the original polyline.
    */
   boolean keep(Point2D p)
   {
      boolean keepPt;
      boolean isBadPt = !Utilities.isWellDefined(p);
      if(isDiscontinuity)
      {
         keepPt = !isBadPt;
         isDiscontinuity = isBadPt;
      }
      else if(isBadPt)
      {
         keepPt = true;
         isDiscontinuity = true;
      }
      else 
         keepPt = (!Utilities.isWellDefined(pLast)) || (pLast.distanceSq(p) > sqTol);
      
      if(keepPt) pLast.setLocation(p);
      return(keepPt);
   }
   
   /** 
    * The last data point that was "kept". The algorithm skips over subsequent well-defined points that are within
    * tolerance of this point.
    */
   final Point2D pLast;
   /** Flag is set initially and each time an ill-defined point is encountered. */
   boolean isDiscontinuity;
   /** If test data point is within this squared distance of the last data point kept, then it is skipped. */
   final double sqTol;
}
