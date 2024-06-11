package com.srscicomp.common.util;

import java.awt.geom.Point2D;


/**
 * Representation of an infinite line in the Cartesian XY plane, as defined by the linear equation <i>y = mx + b</i>,
 * where <i>m</i> is the slope of the line and <i>b</i> is its y-intercept.
 * @author sruffner
 */
public class LineXY
{

   /**
    * Construct an infinite straight line in the XY plane, as represented by the linear equation <i>y = mx + b</i>.
    *  
    * @param m The slope. Specify 0 for a horizontal line, +/-Inf or NaN for a vertical line.
    * @param b The Y-intercept. If not well-defined, 0 is assumed. <i>Special case: If the first argument is infinite or
    * NaN, then the line is vertical, <i>x = C</i>, and this argument specifies the X-intercept <i>C</i>.
    */
   public LineXY(double m, double b)
   {
      if(!Utilities.isWellDefined(b)) b = 0;
      
      if(Utilities.isWellDefined(m))
      {
         slope = m;
         yint = b;
         xint = (m==0) ? Double.NaN : -yint/slope;
      }
      else
      {
         slope = Double.POSITIVE_INFINITY;
         yint = Double.NaN;
         xint = b;
      }
   }
   
   /**
    * Construct an infinite straight line in the XY plane that connects the two specified points. 
    * @param p1 The first point.
    * @param p2 The second point.
    * @throws IllegalArgumentException if either point is null, if the points are the same, or either is ill-defined.
    */
   public LineXY(Point2D p1, Point2D p2)
   {
      this(p1==null ? Double.NaN : p1.getX(),p1==null ? Double.NaN : p1.getY(), 
            p2==null ? Double.NaN : p2.getX(), p2==null ? Double.NaN : p2.getY());
   }
   
   /**
    * Construct an infinite straight line in the XY plane that connects the two specified points. 
    * <p>NOTE: If the X-coordinates are very close but not equal, the slope will get quite large. This leads to issues
    * when performing calculations wtih <b>LineXY</b>. To circumvent the problem, if the calcluated slope has an
    * absolute magnitude exceeding 570 -- corresponding to an approximate angle of inclination around 89.9deg -- then
    * the line is assumed to be vertical with an X-intercept at <i>x=x1</i> Analogously, if the magnitude of the slope
    * is less than 0.0017 (approximate AoI around 0.1deg), then the line is assumed to be horizontal with a Y-intercept
    * at <i>y=y1</i>.</p>
    * 
    * @param x1 X-coordinate of first point.
    * @param y1 Y-coordinate of first point.
    * @param x2 X-coordinate of second point.
    * @param y2 Y-coordinate of second point.
    * @throws IllegalArgumentException if the two points are the same, or if either point is not well-defined.
    */
   public LineXY(double x1, double y1, double x2, double y2)
   {
      boolean ok = Utilities.isWellDefined(new double[] {x1, y1, x2, y2});
      if(ok) ok = !(x1 == x2 && y1 == y2);
      if(!ok) throw new IllegalArgumentException("Points ill-defined or equal!");
      
      if(x1 == x2)
      {
         slope = Double.POSITIVE_INFINITY;
         yint = Double.NaN;
         xint = x1;
      }
      else if(y1 == y2)
      {
         slope = 0;
         yint = y1;
         xint = Double.NaN;
      }
      else
      {
         slope = (y2-y1) / (x2-x1);
         if(Math.abs(slope) >= 570)
         {
            slope = Double.POSITIVE_INFINITY;
            yint = Double.NaN;
            xint = x1;
         }
         else if(Math.abs(slope) <= 0.0017)
         {
            slope = 0;
            yint = y1;
            xint = Double.NaN;
         }
         else
         {
            yint = y1 - slope * x1;
            xint = -yint/slope;
         }
      }
   }
   
   /** 
    * Get the slope of this line. 
    * @return The slope. Will be 0 for a horizontal line and positive infinity for a vertical line. 
    */
   public double getSlope() { return(slope); }
   
   /**
    * Get the Y-intercept of the line.
    * @return The Y-intercept. Will be NaN for a vertical line.
    */
   public double getYIntercept() { return(yint); }
   
   /**
    * Get the X-intercept of the line.
    * @return The X-intercept. Will be NaN for a horizontal line.
    */
   public double getXIntercept() { return(yint); }
   
   /**
    * Is this line vertical? If so, the Y-intercept is undefined and the line equation is <i>x = constant</i>.
    * @return True if line is vertical, else false.
    */
   public boolean isVertical() { return(!Utilities.isWellDefined(yint)); }
   /**
    * Is this line horizontal? If so, the X-intercept is undefined and the line equation is <i>y = constant</i>.
    * @return True if line is horizontal, else false.
    */
   public boolean isHorizontal() { return(!Utilities.isWellDefined(xint)); }
   /**
    * Get the angle between this line and the horizontal (X-axis)? 
    * @return The angle in degrees, lying in [-90..90]. If this line is horizontal, 0 is returned; if vertical, 90 is 
    * returned.
    */
   public double getAngleOfInclination()
   {
      double angle;
      if(isHorizontal()) angle = 0;
      else if(isVertical()) angle = 90;
      else angle = Math.atan(slope)*180/Math.PI;
      
      return(angle);
   }
   
   /**
    * Find the line in the XY plane that is parallel to this line and passes through the point specified.
    * @param x X-coordinate of a point on the desired line.
    * @param y Y-coordinate of a point on the desired line.
    * @return The desired line in the XY plane, or null if the specified point is not well-defined.
    */
   public LineXY getParallelLine(double x, double y)
   {
      LineXY out;
      if(!(Utilities.isWellDefined(x) && Utilities.isWellDefined(y)))
         out = null;
      else if(getDistanceFromPoint(x, y) == 0) 
         out = this;
      else if(isVertical())
         out = new LineXY(Double.POSITIVE_INFINITY, x);
      else
      {
         double b = y - slope*x;
         out = new LineXY(slope, b);
      }
      return(out);
   }
   
   /**
    * Find the line in the XY plane that is parallel to this line, separated from it by the perpendicular distance
    * specified, and either "above" or "below" this line (based on the Y-intercept).
    * 
    * <p><i>Algorithm</i>: A line parallel to this one must have the same slope but different Y-intercepts. If this
    * line's equation is <i>y = mx + b1</i>, then the parallel line is <i>y = mx + b2</i>. It can be shown that the
    * perpendicular distance D separating the lines is given by <i>D = |b2-b1| / sqrt(m^2 + 1)</i>. Thus, the two 
    * possible Y-intercepts of the parallel line are <i>b2 = b1 +/- D*sqrt(m^2 + 1)</i>.</p>
    * <p>The algorithm does not apply when this line is vertical, <i>x = c1</i>; in that case, you simply set the 
    * X-intercept <i>c2</i> of the parallel line to <i>c2 = c1 +/- D</i>.</p>
    * 
    * @param d Perpendicular distance separating this line from the line requested. If 0, this line is returned. If
    * not well-defined or negative, null is returned;
    * this line is returned.
    * @param above True to select the parallel line above this one; false to select the one below. <i>Special case: If 
    * this line is vertical, true selects the parallel line to the left (lesser X-intercept) and false selects the one 
    * to the right.</i>
    * @return The desired line in the XY plane, or null if distance argument is invalid.
    */
   public LineXY getParallelLine(double d, boolean above)
   {
      LineXY out;
      if((!Utilities.isWellDefined(d)) || d < 0) 
         out = null;
      else if(d == 0)
         out = this;
      else if(isVertical())
         out = new LineXY(Double.POSITIVE_INFINITY, xint + (above ? -d : d));
      else 
         out = new LineXY(slope, yint + (above ? 1 : -1)*d*Math.sqrt(slope*slope + 1));
      return(out);
   }
   
   /**
    * Find the line in the XY plane that is parallel to this line, separated from it by the perpendicular distance
    * specified, and further from the specified reference point.
    * 
    * <p>See {@link #getParallelLine(double, double, double, boolean)}.</p>
    * 
    * @param d Perpendicular distance separating this line from the line requested. If 0, this line is returned. If
    * not well-defined or negative, null is returned;
    * @param xRef X-coordinate of reference point. If NaN or infinite, returns null.
    * @param yRef Y-coordinate of reference point. If NaN or infinite, returns null.
    * @return The desired line in the XY plane, or null if any argument is invalid.
    */
   public LineXY getParallelLine(double d, double xRef, double yRef) { return(getParallelLine(d, xRef, yRef, false)); }
   
   /**
    * Find the line in the XY plane that is parallel to this line, separated from it by the perpendicular distance
    * specified, and nearer to or further from the specified reference point.
    * 
    * <p><i>Algorithm</i> The two possible lines are computed as in {@link #getParallelLine(double, boolean)}, and the
    * line chosen that is nearer or further from <i>(xRef, yRef). If this line is vertical, then the method returns the 
    * parallel vertical line that is on the same side or opposite side of this line as the reference point.</p>
    * 
    * @param d Perpendicular distance separating this line from the line requested. If 0, this line is returned. If
    * not well-defined or negative, null is returned;
    * @param xRef X-coordinate of reference point. If NaN or infinite, returns null.
    * @param yRef Y-coordinate of reference point. If NaN or infinite, returns null.
    * @param near True/false to return the parallel line closer to/further from the specified reference point.
    * @return The desired line in the XY plane, or null if any argument is invalid.
    */
   public LineXY getParallelLine(double d, double xRef, double yRef, boolean near)
   {
      LineXY out;
      if((!Utilities.isWellDefined(new double[] {d, xRef, yRef})) || d < 0)
         out = null;
      else if(d == 0)
         out = this;
      else if(isVertical())
      {
         // in this case, the chosen line will be on the side of this line that is opposite or the same side as the
         // reference point, depending on the "near" argument.
         double ofs = d;
         if((near && (xRef < xint)) || ((!near) && (xRef > xint))) ofs = -d;
         out = new LineXY(Double.POSITIVE_INFINITY, xint + ofs);
      }
      else 
      {
         // in the general case, we calculate the distance from the reference point to each of the two possible lines
         // and choose the line IAW the "near" argument.
         out = new LineXY(slope, yint - d*Math.sqrt(slope*slope + 1));
         LineXY other = new LineXY(slope, yint + d*Math.sqrt(slope*slope + 1));
         double d1 = out.getDistanceFromPoint(xRef, yRef);
         double dOther = other.getDistanceFromPoint(xRef, yRef);
         if((near && (dOther < d1)) || ((!near) && (dOther > d1))) 
            out = other;
      }
      return(out);
   }
   
   /**
    * Create a line in the XY plane that is perpendicular to this line and that passes through the point specified.
    * 
    * <p><i>Algorithm</i>: The slope <i>m</i> of the perpendicular line <i>y = mx + b</i> is the negative reciprocal of 
    * this line's slope. Let (X1,Y1) be the specified point on this line. Then the Y-intercept is <i>b = Y1-m*X1</i>. If
    * this line is vertical, then the perpendicular line is horizontal with Y-intercept Y1. Similarly, if this line is 
    * horizontal, the perpendicular line is vertical with X-intercept X1.</p>
    * 
    * @param x X-coordinate of a point through which the perpendicular line passes.
    * @param y Y-coordinate of that point.
    * @return The perpendicular line as specified. If either coordinate is NaN or infinite, null is returned.
    */
   public LineXY getPerpendicularLine(double x, double y)
   {
      LineXY out;
      if(!(Utilities.isWellDefined(x) && Utilities.isWellDefined(y)))
         out = null;
      else if(isVertical())
         out = new LineXY(0, y);
      else if(isHorizontal())
         out = new LineXY(Double.POSITIVE_INFINITY, x);
      else
         out = new LineXY(-1.0/slope, y + x/slope);
      return(out);
   }
   
   /**
    * Get the point of intersection between this line and the line specified. If the lines are parallel, the point of
    * intersection does not exist (or is not unique, if the two lines are identical). If one line is vertical, then its
    * X-intercept gives the X-coordinate Xi of the intersection point, and we can use the equation of the other line to 
    * get the Y-coordinate: <i>Yi = m*xi + b</i>. Similarly, if one line is horizontal, then its Y-intercept is Yi and
    * <i>Xi = (Yi - b) / m</i>. If the two lines are neither horizontal nor vertical, then we can use their two slope-
    * intercept equations to solve for the intersection point: <i>Xi = (b2-b1) / (m1-m2)</i> and <i>Yi = m1*Xi + b1</i>.
    * 
    * @param line The line to intersect with this one.
    * @param pInt If non-null, this is initialized with the intersection point. It is set to (NaN, NaN) if the lines
    * do not intersect.
    * @return The point of intersection. Returns (NaN, NaN) if the line argument is null or parallel to this line.
    */
   public Point2D getIntersection(LineXY line, Point2D pInt)
   {
      Point2D out = (pInt != null) ? pInt : new Point2D.Double();
      
      if(line == null || line.slope == slope)
         out.setLocation(Double.NaN, Double.NaN);
      else if(line.isVertical())
         out.setLocation(line.xint, slope*line.xint + yint);
      else if(isVertical())
         out.setLocation(xint, line.slope*xint + line.yint);
      else
      {
         double x = (line.yint - yint) / (slope - line.slope);
         out.setLocation(x, slope * x + yint);
      }
      
      return(out);
   }
   
   /**
    * Find the point P' on this line such that it lies a distance D from the starting point P and -- since there are two
    * such points -- lies further from or closer to the specified reference point Pref. In the event that Pref is 
    * equidistant from the two possible points, the method chooses one of them.
    * 
    * <p><i>Algorithm</i>. Let P = (x,y) and P' = (x', y'). The distance D between the points is sqrt( (x'-x)^2 + 
    * + (y'-y)^2 ). Since both lie on this line, the two pairs of coordinates must satisfy the slope-intercept equation
    *  <i>y = mx + b</i>. Hence (y' - y) = m(x'-x). Plugging this into the equation for D and solving for x', we get
    *  x' = x +/- D/sqrt(m^2 + 1). We then use the slope-intercept equation to find y'. Finally, we compute the 
    *  distances of the two alternate points from Pref and select the one that is further away or closer.
    * 
    * @param p The starting point P. Cannot be null and must be well-defined. Assumed to be a point ON this line -- the
    * method will only use the X-coordinate and calculate the Y (if the line is horizontal, it uses the X-coordinate;
    * if the line is vertical, it uses the Y-coordinate). On return, it is set to the desired point P'.
    * @param d The distance separating P from P'. If 0, P' == P. If negative, NaN, or infinite, method fails.
    * @param pRef The reference point Pref. Cannot be null and must be well-defined.
    * @param closer True to select the point that is closer to Pref; false to select the one further away.
    * @throws IllegalArgumentException if any argument is invalid.
    */
   public void getPointAlongLine(Point2D p, double d, Point2D pRef, boolean closer)
   { 
      if((!Utilities.isWellDefined(p)) || (!Utilities.isWellDefined(d)) || (d < 0) || (!Utilities.isWellDefined(pRef)))
         throw new IllegalArgumentException();
      
      // ensure starting point P is ON the line
      double x, y;
      if(isVertical()) { x = xint; y = p.getY(); }
      else { x = p.getX(); y = slope * x + yint; }
      
      // special case: D = 0. Just return the point P (already adjusted to ensure it's on this line
      if(d == 0)
      {
         p.setLocation(x, y);
         return;
      }
      
      // find the two alternate points P' and their distances from Pref
      double x1, x2, y1, y2, d1, d2;
      if(isVertical())
      {
         x1 = x2 = x;
         y1 = y + d;
         y2 = y - d;
      }
      else
      {
         x1 = x + d / Math.sqrt(slope*slope + 1);
         y1 = slope*x1 + yint;
         x2 = x - d / Math.sqrt(slope*slope + 1);
         y2 = slope*x2 + yint;
      }
      d1 = Math.sqrt(Math.pow(x1-pRef.getX(), 2) + Math.pow(y1-pRef.getY(), 2));
      d2 = Math.sqrt(Math.pow(x2-pRef.getX(), 2) + Math.pow(y2-pRef.getY(), 2));

      // select the point P' that is either closer to or further from Pref.
      if(closer)
      {
         if(d1 <= d2) p.setLocation(x1, y1);
         else p.setLocation(x2, y2);
      }
      else
      {
         if(d1 >= d2) p.setLocation(x1, y1);
         else p.setLocation(x2, y2);
      }
   }
   
   /**
    * Find the point along this line that is the same distance from the two reference points specified.
    * 
    * <p><i>Algorithm</i>. Let P1 = (x1, y1) and P2 = (x2, y2) be the reference points and P = (x, mx+b) be the point on
    * the line that is equally distant from P1 and P2. In general, we can set the squared distances between the two 
    * pairs of points (P,P1) and (P,P2) equal to each other and solve for x (the x^2 parts cancel out!):
    * 
    * <pre>x = [x1*x1 + y1*y1 - x2*x2 - y2*y2 + 2*(y2-y1)*b] / [2*(x1 - x2 + m*(y1-y2))]</pre>
    * This formula is indeterminate for several special cases:
    * <ul>
    * <li><i>P1==P2</i>. If this is the case, the method finds the line passing through P1 that is perpendicular to this
    * line; then P is the intersection of the perpendicular with this line.</li>
    * <li><i>Horizontal line, and x1==x2</i>. In this case, P1 and P2 lie along the vertical line <i>x=x1</i>, while
    * this line is horizontal. There is no solution in this case, unless this line happens to intersect the vertical
    * line at (y1+y2)/2, in which case P=(x1,b).</li>
    * <li><i>Vertical line</i>. In this case, P=(C,y), where C is the x-intercept and we need to solve for y. We get
    *    <pre>y = [(x1-C)^2 - (x2-C)^2 + y1*y1 - y2*y2] / 2*(y1-y2).</pre>
    * The solution P is well-defined so long as the y-coordinates of P1 and P2 are not the same. If they are, then P
    * exists only if (x1+x2)/2 happens to be this line's x-intercept.</li>
    * <li><i>(x1 - x2) = m*(y2 - y1)</i> In this case, P1 and P2 happen to lie on the perpendicular to this line. In 
    * general, there's no solution unless P1 and P2 happen to be equidistant from the intersection of the perpendicular
    * with this line.</li>
    * </ul>
    * </p>
    * 
    * @param p1 The first reference point.
    * @param p2 The second reference point.
    * @param p If this is non-null, it is initialized with the coordinates of the point on this line that is equally
    * distant from the two reference points. Set to (NaN,NaN) if either argument is ill-defined or there's no solution.
    * @return The equally distant point, as described.
    */
   public Point2D getEquidistantPointOnLine(Point2D p1, Point2D p2, Point2D p)
   {
      Point2D pOut = (p != null) ? p : new Point2D.Double();
      pOut.setLocation(Double.NaN, Double.NaN);
      if(!(Utilities.isWellDefined(p1) && Utilities.isWellDefined(p2))) return(pOut);
      
      if(p1.equals(p2))
      {
         LineXY perp = getPerpendicularLine(p1.getX(), p1.getY());
         getIntersection(perp, pOut);
      }
      else if(isHorizontal() && p1.getX() == p2.getX())
      {
         // there's only a solution if the midpoint between P1 and P2 happens to lie on this line
         if(yint == (p1.getY() + p2.getY())/2.0)
            pOut.setLocation(p1.getX(), yint);
      }
      else if(isVertical())
      {
         // solution exists if the y-coordinates of P1 and P2 are different. If they're the same, then P1 and P2 lie
         // on a horizontal line, and solution exists only if that line intersects this one at (x1+x2)/2
         if(p1.getY() != p2.getY())
         {
            double y = (p1.getX()-xint)*(p1.getX()-xint) + p1.getY()*p1.getY();
            y -= (p2.getX()-xint)*(p2.getX()-xint) + p2.getY()*p2.getY();
            y /= 2*(p1.getY()-p2.getY());
            
            pOut.setLocation(xint, y);
         }
         else if(xint == (p1.getX() + p2.getX())/2.0)
            pOut.setLocation(xint, p1.getY());
      }
      else if(p1.getX() - p2.getX() == slope * (p2.getY() - p1.getY()))
      {
         if(getDistanceFromPoint(p1.getX(), p1.getY()) == getDistanceFromPoint(p2.getX(), p2.getY()))
            getIntersection(getPerpendicularLine(p1.getX(), p1.getY()), pOut);
      }
      else
      {
         double x = p1.getX()*p1.getX() + p1.getY()*p1.getY() - p2.getX()*p2.getX() - p2.getY()*p2.getY();
         x += 2 * yint * (p2.getY() - p1.getY());
         x /= 2 * (p1.getX() - p2.getX() + slope *(p1.getY() - p2.getY()));
         
         pOut.setLocation(x, slope*x + yint);
      }

      return(pOut);
   }
   
   /**
    * Get the coordinates of a point offset from this line by the perpendicular distance specified, such that the 
    * perpendicular line contains the point specified. There are two such offset points, on either side of this line;
    * another argument selects which one is returned.
    * 
    * <p><i>Algorithm</i>. Find the line L1 perpendicular to this line and passing through <i>(x,y)</i>. Also find the 
    * line L2 parallel to this line and separated from it by the distance <i>d</i>, lying above or below IAW the 
    * <i>above</i> argument. The intersection of L1 and L2 is the desired point.</p>
    * 
    * @param d The offset distance. If NaN, infinite, or negative, (NaN, NaN) is returned. If 0, the point returned will
    * lie on this line.
    * @param x The X-coordinate of a point through which the perpendicular passes. Typically this will be a point on or
    * very nearly on this line, but that is not required. If NaN or infinite, (NaN, NaN) is returned.
    * @param y The Y-coordinate of the point through which the perpendicular passes.
    * @param above True to select the offset point lying above this line; false to select the one below. <i>Special 
    * case: If this line is vertical, true selects the offset point to the left (lesser X-coordinate) and false selects
    * the one to the right.</i>
    * @param p If non-null, this is initialized with the offset point. It is set to (NaN, NaN) if any argument is 
    * invalid.
    * @return The offset point, as described.
    */
   public Point2D getOffsetPoint(double d, double x, double y, boolean above, Point2D p)
   {
      Point2D out = (p != null) ? p : new Point2D.Double();
      LineXY line1 = getPerpendicularLine(x, y);
      LineXY line2 = getParallelLine(d, above);
      
      if(line1 == null || line2 == null)
         out.setLocation(Double.NaN, Double.NaN);
      else
         line1.getIntersection(line2, out);
      
      return(out);
   }
   
   /**
    * Does this line lie "above" the specified point in the XY plane? 
    * @param x X-coordinate of test point
    * @param y Y-coordinate of test point.
    * @return True if line lies above the test point; i.e, <i>m*x + b > y</i>. Special case: If the line is vertical, 
    * method returns true if the line is to the left of the test point, i.e., its X-intercept <i>&lt; x</i>.
    */
   public boolean liesAbove(double x, double y)
   {
      return(isVertical() ? (x > xint) : (y < (slope*x + yint)));
   }
   
   /**
    * Does this line lie "left of" the specified point in the XY plane? 
    * @param x X-coordinate of test point
    * @param y Y-coordinate of test point.
    * @return True if line lies left of the test point; i.e, <i>(y-b)/m < x</i>. Special cases: If the line is vertical, 
    * method returns true if its X-intercept <i>&lt; x</i>. If the line is horizontal, the method returns true if the 
    * line is above the test point, i.e., its Y-intercept <i>&gt; y</i>.
    */
   public boolean liesLeft(double x, double y)
   {
      return(isVertical() ? (x > xint) : (isHorizontal() ? (yint > y) : ((y-yint)/slope < x)));
   }
   
   /**
    * Get the perpendicular distance from the specified point to this line. In general, for line <i>y=mx + b</i> and
    * point <i>(x0,y0)</i>, the distance is |-mx0 + y0 - b| / sqrt(m^2 + 1). For a vertical line <i>x=C</i>, the 
    * distance is simply |x0 - C|.
    * 
    * @param x X-coordinate of point.
    * @param y Y-coordinate of point.
    * @return Perpendicular distance of point from this line.
    */
   public double getDistanceFromPoint(double x, double y)
   {
      double d;
      if(isVertical()) d = Math.abs(x - xint);
      else d = Math.abs(-slope*x + y - yint) / Math.sqrt(slope*slope + 1);
      return(d);
   }
   
   
   /** The slope of the line. */
   private double slope;
   /** The y-intercept of the line. */
   private final double yint;
   /** The x-intercept of the line. */
   private final double xint;
}
