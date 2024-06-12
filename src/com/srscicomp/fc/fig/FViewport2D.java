package com.srscicomp.fc.fig;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import com.srscicomp.common.util.Utilities;


/**
 * <b>FViewport2D</b> encapsulates the notion of a two-dimensional viewport.
 * 
 * <p>In <i>FigureComposer</i>'s graphic model, a graphics node is positioned with respect to a parent "container" by 
 * defining the element's location and size within that container. The location and size attributes ASSUME a right-
 * handed coordinate system with an origin at the parent's bottom-left corner, x-axis increasing rightward, and y-axis 
 * increasing upward.</p>
 * 
 * <p>A node's width, height, and location coordinates are each encapsulated as a {@link Measure}, a floating-point 
 * value with associated measurement units. It supports four physical units appropriate to the production of scientific 
 * figures ("in", "cm", "mm"), as well as percentage and "user" units. {@link Measure.Unit#USER} makes sense ONLY in the
 * context of a graph container. Every graph defines a coordinate system -- in "user" units -- for all data displayed in
 * its "data viewport". There are two kinds of 2D graph containers in FC. The {@link GraphNode} class supports several
 * kinds of coordinate systems: the standard Cartesian X-Y graph, a loglog graph, a semilog graph in X or Y, and polar 
 * plots with a linear or logarithmic radial axis. The more recently introduced {@link PolarPlotNode} only supports a
 * polar coordinate system with a linear radial axis, but it offers more flexibility in specifying the layout of the
 * polar grid. Any graphics node within these two graph containers may be positioned and sized with respect to the data 
 * viewport if their location coordinates and dimensions are in "user" units.</p>
 * 
 * <p>[A 3D graph container class -- {@link Graph3DNode} -- was introduced in FC 5.0.0. Since it is a 3D container, it
 * does not fit will into FC's 2D rendering infrastructure. See <b>Graph3DNode</b> for more information.]</p>
 * 
 * <p>All rendering in FC takes place in a standard right-handed 2D coordinate system with units in milli-inches. 
 * <b>FViewport2D</b> provides the necessary infrastructure to convert the other coordinate systems and the various 
 * units of measure to this "rendering coordinate system". Coordinates specified in physical units are converted to 
 * milli-inches in a straightforward manner. Percentage coordinates are converted to milli-inches by multiplying by the 
 * actual width (or height) of the viewport.</p>
 * 
 * <p>Converting coordinates in "user" units is more difficult. To make the necessary transformations, we need to know
 * know the type of user coordinate system that governs the viewport, as well as the viewport's origin and dimensions 
 * in user units. (Note that the width and/or height in user units could be negative, since the user coordinate system 
 * may invert the sense of either graph axis relative to FC's convention.) <b>FViewport2D</b> encapsulates this 
 * information, and provides methods for converting measured lengths and coordinates to milli-inches with respect to the
 * viewport that it represents.</p>
 * 
 * <p>On polar coordinates: In FC, by convention, a point in polar coordinates is expressed as (theta, radius) -- the
 * angular coordinate maps to X and the radial coordinate to Y. Unlike (X,Y)-coordinates, polar coordinates are not 
 * orthogonal to each other. Also, the theta coordinate is intrinsically angular and thereby measured in degrees rather 
 * than inches or percentages. However, in FC's graphics model, the X and Y coordinates of any point may be measured in
 * physical units like inches, expressed as a percentage of the viewport size, or expressed in user units. We must 
 * establish somewhat arbitrary conventions for treating such coordinates in the context of a user coordinate system 
 * that is polar:
 * <ul>
 * 	<li>When both coordinates are measured in physical units or expressed as percentages, then they are treated like 
 * 	standard (X,Y) Cartesian coordinates. Thus, eg, (theta,r) = (50%,50%) is mapped to the center of the viewport, 
 * 	while (1in,100%) is mapped to the top edge of the viewport, 1in to the right of the top-left corner.</li>
 * 	<li>When the theta coordinate is measured in "user" units, regardless of the units for radius, a standard polar 
 * 	to Cartesian transformation is appropriate. The implicit units for theta are degrees, and all such theta values 
 * 	are mapped to the unit circle [0..360). If the radius is in percentage units, then it is interpreted as a 
 * 	percentage of the smaller physical dimension of the viewport. If the radius is in user units, then it is 
 * 	converted to milli-inches using information provided when the viewport object is created. Once the radius 
 * 	is in physical units, standard trig calculations transform (theta,r) to physical Cartesian location (X,Y). These 
 * 	coordinates are translated IAW the physical origin of the polar coordinate system within the viewport.</li>
 * 	<li>When the radial coordinate is measured in "user" units and theta in percentage units, then the theta value 
 * 	is interpreted as a percentage of 360 degrees. The polar to Cartesian transformation is applied as previously 
 * 	described.</li>
 * 	<li>When the radial coordinate is in "user" units and theta is expressed in physical units, the physical unit 
 * 	identifier for theta is simply ignored and theta is assumed to be in angular degrees. This particular combination 
 * 	of units makes little sense for polar coordinates.</li>
 * </ul>
 * </p>
 * 
 * <p>A second 2D graph node class was introduced in FC 5.2.0 that is dedicated to the presentation of polar plots --
 * {@link PolarPlotNode}. This 2D graph container only offers a polar coordinate system with a linear radial axis, but
 * it auto-generates the polar grid labels and offers more flexibility than the polar coordinate system modes in 
 * {@link GraphNode}: (i) &theta;=0 can point in any direction from the origin, and any angular span <= 360deg is
 * allowed; (2) the theta axis can increase in the CCW or CW direction, and the radial axis can increase toward the
 * origin instead of away. To support these features, several new parameters were added to the {@link #POLARCOORDS}
 * coordinate system type in <b>FViewport2D</b>.</p>
 * 
 * <p>On log coordinates: In a loglog or semilog graph, "user" coordinates should be expressed as standard Cartesian 
 * (X,Y) points. <b>FViewport2D</b> handles the logarithmic transformation when converting them to physical coordinates.
 * If a point has at least one NEGATIVE coordinate, then the logarithmic transformation will be undefined. In such 
 * cases, <b>FViewport2D</b> methods do not throw an exception; instead, they return  <b>null</b> or set the coordinates
 * of the point to {@link Double#NaN}.</p>
 * 
 * <p>Only FC's graph containers have a "data viewport" in which the measures specified in "user" units make any sense.
 * To encapsulate all the other elements that lack such a viewport, use the constructor version which does not specify 
 * the viewport's origin and size in user units. In this case, the viewport object will set the origin and size so that 
 * "user" units are interpreted as equivalent to thousandth-inches.</p>
 * 
 * @see Measure, FGraphicNode, GraphNode, PolarPlotNode
 * @author sruffner
 */
public class FViewport2D
{
	/**
	 * Viewport type that does not support user coords; any supplied user coords are not transformed in any way 
	 * and thus are assumed to be in thousandth-inches relative to the viewport origin.
	 */
	private final static int NOUSERCOORDS = 0;

	/** Viewport type that is mapped to a standard XY Cartesian coordinate system. */
	private final static int CARTESIANCOORDS = 1;

	/** Viewport type that is mapped to a semilogX coordinate system. */
	private final static int SEMILOGXCOORDS = 2;

	/** Viewport type that is mapped to a semilogY coordinate system. */
	private final static int SEMILOGYCOORDS = 3;

	/** Viewport type that is mapped to a loglog coordinate system. */
	private final static int LOGLOGCOORDS = 4;

	/** Viewport type that is mapped to a polar coordinate system. */
	private final static int POLARCOORDS = 5;

	/** Viewport type that is mapped to polar coordinate system in which the radial axis is logarithmic. */
	private final static int SEMILOGRCOORDS = 6;

	/** Viewport type, ie, the type of user coordinate system mapped to the viewport. */
	private int userType = NOUSERCOORDS;

	/** Actual width of viewport in milli-inches. */
	private final double width;

   /** Actual height of viewport in milli-inches. */
   private final double height;

   /** Scale factor converts viewport width in user units to milli-in; 1 if viewport does not support user units. */
   private final double usrToRealW;

   /** Scale factor converts viewport height in user units to milli-in; 1 if viewport does not support user units. */
   private final double usrToRealH;

	/**
	 * Viewport origin in user units; (0,0) if viewport does not support user units. By convention, the viewport origin 
    * is at the bottom left corner (except for polar viewports, in which case the polar origin varies 
	 * depending on the layout). In the polar case, this field is NOT used. 
	 */
	private final Point2D userOrigin;

	/**
	 * When the user coord system associated with the viewport is polar, this field gives the physical location of the 
	 * polar origin in viewport coordinates, ie, in milli-inches WRT to the conventional origin at the bot-left 
    * corner of the viewport.
	 */
	private final Point2D polarOrigin;

	/**
	 * When the user coord system associated with the viewport is polar, this field holds the radial value at the polar
	 * origin. Its interpretation depends on whether or not the radial axis direction is reversed:
	 * <ul>
	 * <li>Normal direction (values increase away from the origin): This is the minimum radius. Any points in polar
	 * coordinates with radii less than this value are considered ill-defined. Must be non-negative for if radial axis
	 * is linear; strictly positive if radial axis is logarithmic.</li>
	 * <li>Reverse direction (values increase toward the origin): This is the maximum radius. The reverse direction is
	 * only supported for a linear radial axis. Any points with radii greater than this value are ill-defined.</li>
	 * </ul>
	 */
	private final double radiusAtOrigin;

	/** 
	 * For {@link #POLARCOORDS} viewport type only: If false, the angular axis increases in the counterclockwise 
	 * direction; if true, it increases in the clockwise direction.
	 */
	private final boolean thetaAxisReversed;
	
	/**
	 * For {@link #POLARCOORDS} viewport type only: If false, radial values increase in the normal direction, from the
	 * polar origin outward; if true, radial values increase as you approach the origin.
	 */
	private final boolean radialAxisReversed;
	
	/** 
	 * For {@link #POLARCOORDS} viewport type only: This is the angle made by the ray <i>&theta;=0</i> with the ray
	 * emanating from the origin, parallel to the top and bottom edges of the viewport, and pointing to the right. It is
	 * measured in degrees CCW (CCW is positive). This makes it possible to layout the polar coordinate system with 
	 * 0deg pointing in any direction. 
	 */
	private final double thetaZeroAngle;
	
	/**
	 * Construct a viewport that does not support user units. Any coordinate specified in user units will be treated by 
	 * this viewport object as though it was specified in milli-inches relative to (0,0).
	 * 
	 * @param width Width of viewport in milli-inches.
	 * @param height Height of viewport in milli-inches.
    * @throws IllegalArgumentException if any numeric argument is not well-defined (infinite or NaN).
	 */
	public FViewport2D(double w, double h)
	{
      if(!Utilities.isWellDefined(new double[] {w, h}))
         throw new IllegalArgumentException("All double-valued numbers must be well-defined");

      this.width = w;
      this.height = h;
      this.usrToRealW = 1;
      this.usrToRealH = 1;
		this.userOrigin = new Point2D.Double();
      this.polarOrigin = null;
      this.radiusAtOrigin = 0;
      this.thetaAxisReversed = false;
      this.radialAxisReversed = false;
      this.thetaZeroAngle = 0;
		this.userType = NOUSERCOORDS;
	}

	/**
	 * Construct a viewport that supports user units in a standard Cartesian x,y-coordinate system.
	 * 
    * @param width Width of viewport in milli-inches. 
    * @param height Height of viewport in milli-inches. 
    * @param userW Width of viewport in user units.
    * @param userH Height of viewport in user units.
	 * @param userOrigin The viewport's bottom-left corner in user units; this is the origin by convention in a 
	 * <em>DataNav</em> viewport. If not well-defined, (0,0) assumed.
    * @throws IllegalArgumentException if any numeric argument is not well-defined (infinite or NaN).
	 */
	public FViewport2D(double w, double h, double userW, double userH, Point2D userOrigin)
	{
      if(!Utilities.isWellDefined(new double[] {w, h, userW, userH}))
         throw new IllegalArgumentException("All double-valued numbers must be well-defined");

      this.width = w;
      this.height = h;
      this.usrToRealW = w / userW;
      this.usrToRealH = h / userH;
      this.userOrigin = Utilities.isWellDefined(userOrigin) ? userOrigin : new Point2D.Double();
      this.polarOrigin = null;
      this.radiusAtOrigin = 0;
      this.thetaAxisReversed = false;
      this.radialAxisReversed = false;
      this.thetaZeroAngle = 0;
      this.userType = CARTESIANCOORDS;
	}

	/**
	 * Construct a viewport that supports user units in cartesian, loglog, or semilog coordinate systems.  The range 
	 * of coordinate values along a logarithmic axis must be strictly non-negative.
	 * 
    * @param width Width of viewport in milli-inches.
    * @param height Height of viewport in milli-inches.
	 * @param left The x-coord of the viewport's left edge in user units.
	 * @param right The x-coord of the viewport's right edge in user units.
	 * @param bottom The y-coord of the viewport's bottom edge in user units.
	 * @param top The y-coord of the viewport's top edge in user units.
	 * @param isLogX <code>True</code> if the user coordinate system is logarithmic in X.
	 * @param isLogY <code>True</code> if the user coordinate system is logarithmic in Y.
	 * @throws IllegalArgumentException if the user coordinate range for a logarithmic axis includes non-positive values, 
    * or if any numeric argument is ill-defined (infinite or NaN).
	 */
	public FViewport2D(double w, double h, double left, double right, double bottom, double top, boolean isLogX, 
		boolean isLogY) throws IllegalArgumentException
	{
      if(!Utilities.isWellDefined(new double[] {w, h, left, right, bottom, top}))
         throw new IllegalArgumentException("All double-valued numbers must be well-defined");
      if( (isLogX && (left <= 0.0 || right <= 0.0)) || (isLogY && (bottom <= 0.0 || top <= 0.0)) )
         throw new IllegalArgumentException( "User coordinate range must be strictly positive for logarithmic axis" );

      // for each dimension that is logarithmic, we replace the coordinate with its base10 logarithm
      double xLeft = left;
      double xRight = right;
      double yBot = bottom;
      double yTop = top;
      if(isLogX)
      {
         xLeft = Utilities.log10(xLeft);
         xRight = Utilities.log10(xRight);
      }
      if(isLogY)
      {
         yBot = Utilities.log10(yBot);
         yTop = Utilities.log10(yTop);
      }
      
      this.width = w;
      this.height = h;
      this.usrToRealW = w /(xRight - xLeft);
      this.usrToRealH = h /(yTop - yBot);
      this.userOrigin = new Point2D.Double( xLeft, yBot );
      this.polarOrigin = null;
      this.radiusAtOrigin = 0;
      this.thetaAxisReversed = false;
      this.radialAxisReversed = false;
      this.thetaZeroAngle = 0;

		if(isLogX) this.userType = (isLogY) ? LOGLOGCOORDS : SEMILOGXCOORDS;
		else if(isLogY) this.userType = SEMILOGYCOORDS;
		else this.userType = CARTESIANCOORDS;
	}

	/**
	 * Construct a viewport that supports user units in a standard polar coordinate system, or a polar coordinate system
	 * with a logarithmic radial axis.
	 * 
	 * <p>To get a first-quadrant view of the polar coordinate system, place the polar origin at the bottom left corner 
	 * of the viewport rectangle. Analogous for 2nd-, 3rd-, and 4th-quadrant views. Typical usage will place the polar
	 * origin at the center of the viewport.</p>
	 * 
	 * <p>Radial values increase outward from the origin; angular values increase in the CCW direction; and 0 degrees
	 * always points to the right.</p>
	 * 
    * @param width Width of viewport in milli-inches.
    * @param height Height of viewport in milli-inches.
	 * @param polarOrigin Location of the polar coordinate system origin, specified in viewport units -- ie, in 
	 * milli-inches WRT an origin at the bottom-left corner and y-axis increasing upward. If <code>null</code> or 
    * otherwise ill-defined, it is assumed to lie at the center of the viewport rectangle.
	 * @param unitRadius The length of the unit radius R1 in milli-inches. If radial coordinate is logarithmic, then 
    * this is the length of one decade along the radial axis.
	 * @param minRadius Minimum radius in "user" units. Any point with user coords (theta, r0) such that r0 is smaller 
    * than the minimum radius is considered to be undefined. For a semilogR coord system, if this argument is less than 
    * or equal to zero, a minimum radius of 0.01 will be assumed. For a standard polar coord system, if this argument is
    * less than zero, a minimum radius of 0 is assumed.
    * @param isLogR True for a polar coordinate system with logarithmic radial axis; false for a linear radial axis.
    * @throws IllegalArgumentException if any numeric argument is ill-defined (infinite or NaN).
	 */
	public FViewport2D(double w, double h, Point2D polarOrigin, double unitRadius, double minRadius, boolean isLogR)
	{
      if(!Utilities.isWellDefined(new double[] {w, h, unitRadius, minRadius}))
         throw new IllegalArgumentException("All double-valued numbers must be well-defined");

      this.width = w;
      this.height = h;
      this.usrToRealW = 1;
      this.usrToRealH = unitRadius;
      this.userOrigin = null;
      this.polarOrigin = Utilities.isWellDefined(polarOrigin) ? polarOrigin : new Point2D.Double(w/2, h/2);
      this.userType = isLogR ? SEMILOGRCOORDS : POLARCOORDS;
		this.radiusAtOrigin = isLogR ? ((minRadius <= 0) ? 0.01 : minRadius) : ((minRadius < 0) ? 0 : minRadius);
      this.thetaAxisReversed = false;
      this.radialAxisReversed = false;
      this.thetaZeroAngle = 0;
	}

	/**
	 * Construct a viewport that supports user units in a polar coordinate system with a linear radial axis and:
	 * <ul>
	 * <li>allows the theta axis to increase in the counterclockwise or clockwise direction;</li>
	 * <li>allows the radial axis to increase away from or toward the polar origin; and</li>
	 * <li>allows "0 degrees" to point in any direction around the unit circle.</li>
	 * </ul>
	 * 
	 * @param w Width of viewport in milli-inches.
	 * @param h Height of viewport in milli-inches.
	 * @param polarOrigin Location of the polar coordinate system origin, specified in viewport units -- ie, in 
    * milli-inches WRT an origin at the bottom-left corner and y-axis increasing upward. If null or otherwise undefined,
    * it is assumed to lie at the center of the viewport rectangle.
	 * @param unitRad The length of the unit radius R1 in milli-inches. 
	 * @param radAtOrigin The radial value at the origin. If the radial axis is in the normal direction, this is the
	 * minimum radial value; if reversed, it is the maximum radial value.
	 * @param zeroAngle The direction of the ray representing &theta;=0 deg. It is measured in degress WRT a ray 
	 * emanating from the polar origin and pointing to the right, with a positive value representing a counterclockwise 
	 * rotation (regardless the value of <i>revTheta</i>).
	 * @param revTheta If true, the polar coordinate system is left-handed, in which a positive theta coordinate
	 * represents a clockwise rotation; if false, CCW is positive.
	 * @param revRad If false, radial values increase outward from the polar origin, and the radial value at the origin 
	 * is the minimum observable radius. If true, radial values increase toward the origin, and the radius at the origin
	 * is the maximum observable radius.
	 */
	public FViewport2D(double w, double h, Point2D polarOrigin, double unitRad, double radAtOrigin, double zeroAngle,
	      boolean revTheta, boolean revRad)
	{
	   this.userType = POLARCOORDS;
	   this.width = w;
	   this.height = h;
	   this.usrToRealW = 1;
	   this.usrToRealH = unitRad;
	   this.userOrigin = null;
	   boolean ok = Utilities.isWellDefined(polarOrigin);
	   this.polarOrigin = new Point2D.Double(ok ? polarOrigin.getX() : w/2, ok ? polarOrigin.getY() : h/2);
	   this.radiusAtOrigin = radAtOrigin;
	   this.thetaAxisReversed = revTheta;
	   this.radialAxisReversed = revRad;
	   this.thetaZeroAngle = Utilities.restrictAngle(zeroAngle);
	}

	/**
	 * Get viewport's width.
	 * @return Viewport width in milli-inches.
	 */
	public double getWidthMI() { return(width); }

   /**
    * Get viewport's height.
    * @return Viewport height in milli-inches.
    */
   public double getHeightMI() { return(height); }

	/**
	 * Does this viewport represent a polar coordinate system? (Radial axis may be linear or logarithmic.)
	 * 
	 * @return True if viewport encapsulates a polar coordinate system.
	 */
	public boolean isPolar() { return(userType == POLARCOORDS || userType == SEMILOGRCOORDS); }

	/**
	 * Get the physical location of the "user" origin in rendered viewport coordinates, ie, in milli-inches WRT to the 
    * bottom-left corner of the viewport, with x-axis increasing rightward and y-axis increasing upward.
    * 
    * <p>For the Cartesian (linear, semilog, or loglog) graphs supported in <em>DataNav</em>, the physical user origin 
    * is coincident width/the viewport's BL corner, by design -- so (0,0) is always returned. However, for the polar 
    * graph types, the user origin is at different locations depending on the graph layout.</p>
	 * 
	 * @return Viewport location of "user" origin, in milli-in WRT BL corner.
	 */
	public Point2D getPhysicalUserOrigin()
	{
		if(isPolar()) return( new Point2D.Double(polarOrigin.getX(), polarOrigin.getY()) );
		else return( new Point2D.Double(0,0) );
	}

   /**
    * Get the physical location of the "user" origin in rendered viewport coordinates, ie, in milli-inches WRT to the 
    * bottom-left corner of the viewport, with x-axis increasing rightward and y-axis increasing upward.
    * 
    * <p>For the Cartesian (linear, semilog, or loglog) graphs supported in <em>DataNav</em>, the physical user origin 
    * is coincident width/the viewport's BL corner, by design -- so (0,0) is always returned. However, for the polar 
    * graph types, the user origin is at different locations depending on the graph layout.</p>
    * 
    * @param p This point object will contain the origin's coordinates. If null, a new point object is created.
    * @return The supplied point, or a new point, initialized to contain the origin's coordinates.
    */
	public Point2D getPhysicalUserOrigin(Point2D p)
	{
	   if(p == null) p = new Point2D.Double();
	   if(isPolar()) p.setLocation(polarOrigin);
	   else p.setLocation(0,0);
	   
	   return(p);
	}
	
	/**
	 * Convert the specified measure to milli-inches with respect to the x-dimension of this viewport. The method applies 
    * only to measures in real or percentage units. 
	 * 
	 * @param m A measurement/coordinate along the x-dimension of viewport.
	 * @return The measurement/coordinate value in milli-inches.
	 * @throws IllegalArgumentException if argument is null or is specified with "user" units.
	 */
	public double fromMeasureToMilliInX(Measure m) throws IllegalArgumentException
	{
		if(m == null || (m.getUnits() == Measure.Unit.USER)) 
		   throw new IllegalArgumentException("Argument is null or in user units!");
		if(!m.isRelative()) 
			return(m.toMilliInches());
		else 
			return(m.getValue() * width / 100.0);
	}

	/**
	 * Return a measure in the specified units that is equivalent to the specified length/coordinate in milli-in along 
    * the x-dimension of the viewport. The method applies only to measures in real or percentage units. 
	 * 
	 * @param x A measurement or coordinate in milli-in WRT x-dimension of this viewport.
	 * @param u The desired measurement units.
    * @return An equivalent measure in the specified units.
	 * @throws IllegalArgumentException if the specified unit is null or the "user" unit.
	 */
	public Measure fromMilliInToMeasureX(double x, Measure.Unit u) throws IllegalArgumentException
	{
		if(u == null || u == Measure.Unit.USER)
			throw new IllegalArgumentException("Unit arg cannot be null or represent user units!");
      if(u != Measure.Unit.PCT)
         return(Measure.getConstrainedRealMeasure(x, u, null));
		else 
		   return(Measure.getConstrainedMeasure(Utilities.limitSigAndFracDigits(x*100.0/width, 5, 2), u, null));
	}

   /**
    * Convert the specified measure to milli-inches with respect to the y-dimension of this viewport. The method applies 
    * only to measures in real or percentage units. 
    * 
    * @param m A measurement/coordinate along the y-dimension of viewport.
    * @return The measurement/coordinate value in milli-inches.
    * @throws IllegalArgumentException if argument is null or is specified with "user" units.
    */
   public double fromMeasureToMilliInY(Measure m) throws IllegalArgumentException
   {
      if(m == null || (m.getUnits() == Measure.Unit.USER))
         throw new IllegalArgumentException("Argument is null or in user units!");
      if(!m.isRelative()) 
         return(m.toMilliInches());
      else 
         return(m.getValue() * height / 100.0);
   }

   /**
    * Return a measure in the specified units that is equivalent to the specified length/coordinate in milli-in along 
    * the y-dimension of the viewport. The method applies only to measures in real or percentage units. 
    * 
    * @param y A measurement or coordinate in milli-in WRT y-dimension of this viewport.
    * @param u The desired measurement units.
    * @return An equivalent measure in the specified units.
    * @throws IllegalArgumentException if the specified unit is null or the "user" unit.
    */
   public Measure fromMilliInToMeasureY(double y, Measure.Unit u) throws IllegalArgumentException
   {
      if(u == null || u == Measure.Unit.USER)
         throw new IllegalArgumentException("Unit arg cannot be null or represent user units!");
      if(u != Measure.Unit.PCT)
         return(Measure.getConstrainedRealMeasure(y, u, null));
      else 
         return(Measure.getConstrainedMeasure(Utilities.limitSigAndFracDigits(y*100.0/height, 5, 2), u, null));
   }

	/**
	 * Transform a rectangle in measured coordinates to this viewport's rendering coordinate system in milli-inches. By 
    * convention, the "rendering coordinate system" of a <b>FViewport2D</b> is a right-handed Cartesion coordinate 
    * system, in which the origin is at the viewport's bottom-left corner, with x-axis increasing rightward, y-axis 
    * increasing upward, and units in milli-inches. The method handles any non-linear transformations required when the 
    * viewport object encapsulates a polar, loglog, or semilog user coordinate system.
	 * 
	 * <p>The measured rectangle is ill-defined if the specified bottom-left corner is specified in user coordinates that
	 * cannot be converted to viewport coordinates for any reason. It will also be ill-defined if the width or height is 
	 * negative. In these situations the method returns null.</p>
	 * 
    * @param x X-coordinate of bottom-left corner of rectangle, with associated measurement units.
    * @param y Y-coordinate of bottom-left corner of rectangle, with associated measurement units.
	 * @param mw Measured width of the rectangle. {@link Measure.Unit#USER} units not allowed.
	 * @param mh Measured height of the rectangle. {@link Measure.Unit#USER} units not allowed.
	 * @return Rectangle specified in rendered viewport coordinates, as described; null is returned if the rectangle is 
	 * ill-defined.
	 */
	public Rectangle2D toMilliInches(Measure x, Measure y, Measure mw, Measure mh)
	{
      Rectangle2D result = null;
      try
      {
         Point2D ptBL = toMilliInches(x, y);
         if(ptBL != null)
         {
            double w = fromMeasureToMilliInX(mw);
            double h = fromMeasureToMilliInY(mh);
            if(w >= 0 && h >= 0)
               result = new Rectangle2D.Double(ptBL.getX(), ptBL.getY(), w, h);
         }
      }
      catch(IllegalArgumentException iae) {}
  
      return(result);
	}

	/**
	 * Transform a point in measured coordinates to this viewport's rendering coordinate system in milli-inches. By 
    * convention, the "rendering coordinate system" of a <b>FViewport2D</b> is a right-handed Cartesion coordinate 
    * system, in which the origin is at the viewport's bottom-left corner, with x-axis increasing rightward, y-axis 
    * increasing upward, and units in milli-inches. The method handles any non-linear transformations required when the 
    * viewport object encapsulates a polar, loglog, or semilog user coordinate system.
	 * 
	 * <p>The measured point is ill-defined if either coordinate is in user coordinates and cannot be converted to 
	 * viewport coordinates for any reason. In such cases the method returns null.</p>
	 * 
    * @param x X-coordinate of point, with associated measurement units.
    * @param y Y-coordinate of point, with associated measurement units.
	 * @return Point specified in rendered viewport coordinates, as described; null is returned if the point cannot be 
	 * transformed, as noted.
	 */
	public Point2D toMilliInches(Measure x, Measure y)
	{
		if(x == null || y == null)
         return(null);

      double xVal = x.getValue();
      Measure.Unit xUnits = x.getUnits();
      double yVal = y.getValue();
      Measure.Unit yUnits = y.getUnits();

		if((userType == POLARCOORDS || userType == SEMILOGRCOORDS) && 
            (xUnits == Measure.Unit.USER || yUnits == Measure.Unit.USER))
		{
			// for polar and semilogR coord systems, when both coords are expressed in physical or % units, they are 
			// interpreted as Cartesian coords. The polar-to-Cartesian transformations here apply only when at least one 
			// coord is measured in "user" units.  The implied "user" units for theta is degrees!

			// if theta coord is in % units, interpret as % of 360deg. If physical units, ignore and assume units are deg. 
			// then restrict to unit circle and convert to radians.
         double theta = xVal;
			if(xUnits == Measure.Unit.PCT) theta = 360.0 * theta / 100.0;
			theta = thetaZeroAngle + (thetaAxisReversed ? -1 : 1) * theta;
         theta = theta % 360.0;
			if(theta < 0) theta += 360.0;
         theta *= Math.PI / 180.0;

			// if radial coord is in % units, interpret as % of the viewport's smaller dimension. If physical units, just 
			// convert to milli-in. In either case, if the value is negative, the point is considered ill-defined. If user 
         // units, convert to milli-inches based on size of "unit" radius, taking into account the minimum radius 
         // assigned to the viewport's polar origin. Any point with radius r < radiusAtOrigin is ill-defined.
         double r = yVal;
			if(yUnits != Measure.Unit.PCT && yUnits != Measure.Unit.USER)
         {
			   if(r <= 0) r = Double.NaN;
            else r = Measure.realUnitsToMI(r, yUnits);
         }
			else if(yUnits == Measure.Unit.PCT)
			{
				if(r <= 0) r = Double.NaN;
            else
            {
               double d = width;
               if(d > height) d = height;
               r *= d / 100.0;
            }
			}
			else
			{
	         if(radialAxisReversed)
	         {
	            if(r > radiusAtOrigin) r = Double.NaN;
	            else if(userType==SEMILOGRCOORDS) r = Utilities.log10(radiusAtOrigin) - Utilities.log10(r);
	            else r = radiusAtOrigin - r;
	         }
	         else
	         {
	            if(r < radiusAtOrigin) r = Double.NaN;
	            else if(userType == SEMILOGRCOORDS) r = Utilities.log10(r) - Utilities.log10(radiusAtOrigin);
	            else r -= radiusAtOrigin;
	         }
	         r *= usrToRealH;
			}

			// now convert polar to Cartesian and translate WRT physical location of viewport origin
			xVal = r * Math.cos(theta);
			yVal = r * Math.sin(theta);
			xVal += polarOrigin.getX();
			yVal += polarOrigin.getY();
		}
		else
		{
			// for loglog, semilog, and cartesian coord systems, the axes are orthogonal -- so each coordinate is 
			// processed separately. 
			if(!x.isRelative()) 
				xVal = Measure.realUnitsToMI(xVal, xUnits);
			else if(xUnits == Measure.Unit.PCT)
				xVal *= width / 100.0;
			else
			{
				if(userType == SEMILOGXCOORDS || userType == LOGLOGCOORDS)
					xVal = Utilities.log10(xVal);
				xVal = (xVal - userOrigin.getX()) * usrToRealW;
			}

         if(!y.isRelative()) 
            yVal = Measure.realUnitsToMI(yVal, yUnits);
         else if(yUnits == Measure.Unit.PCT)
            yVal *= height / 100.0;
         else
         {
            if(userType == SEMILOGYCOORDS || userType == LOGLOGCOORDS)
               yVal = Utilities.log10(yVal);
            yVal = (yVal - userOrigin.getY()) * usrToRealH;
         }
		}

		// if either coord is undefined, return null
      return((Utilities.isWellDefined(xVal) && Utilities.isWellDefined(yVal)) ? new Point2D.Double(xVal, yVal) : null);
	}

   /**
    * Adjust the coordinates of a measured point so that it maps to the specified physical coordinates with respect to 
    * this viewport -- WITHOUT changing the units of measure applied to each coordinate of the measured point.
    * 
    * <p>This method is the inverse of <code>toMilliInches(Measure, Measure)</code>. It takes a point expressed in this 
    * viewport's rendering coordinates in milli-inches, and converts each coordinate to the specified target units, if 
    * possible.</p>
    * 
    * <p><i>[As of 04may2020]</i> Note that the number of significant digits in the coordinate value is restricted to
    * 7 (consistent with a single-precision floating-point number).
    *  
    * @param x X-coordinate of a location, in milli-inches WRT this viewport's rendering coordinate system.
    * @param xUnits The target measurement units for the converted x-coordinate.
    * @param x Y-coordinate of a location, in milli-inches WRT this viewport's rendering coordinate system.
    * @param xUnits The target measurement units for the converted y-coordinate.
    * @param prec Desired precision of converted coordinate values (max # of fractional digits). Restricted to [0..3].
    * @return A two-element array containing the coordinates x,y (in that order), with the desired measurement units.
    * If unable to convert, null is returned.
    */
   public Measure[] fromMilliInches(double x, Measure.Unit xUnits, double y, Measure.Unit yUnits, int prec)
   {
      if(!Utilities.isWellDefined(new double[] {x, y})) return(null);

      Measure[] coords = new Measure[2];
      int p = (prec < 0) ? 0 : ((prec > 3) ? 3 : prec);
      
      if((userType == POLARCOORDS || userType == SEMILOGRCOORDS) && 
            (xUnits == Measure.Unit.USER || yUnits == Measure.Unit.USER))
      {
         // for polar and semilogR coord systems, when both coords are expressed in physical or % units, they are 
         // interpreted as Cartesian coords. The polar-to-Cartesian transformations here apply only when at least one 
         // coord is measured in "user" units. The implied "user" units for theta is degrees!

         // convert Cartesian viewport coords in thousandth-in to polar coords WRT viewport's polar origin
         x -= polarOrigin.getX(); 
         y -= polarOrigin.getY();
         double r = x*x + y*y;
         if(r == 0) return(null);
         double theta = Math.toDegrees(Math.atan2(y, x));
         if(!Utilities.isWellDefined( theta )) return(null);

         // if theta coord is in % units, then restrict theta coord to [0..360) deg and convert result to a percentage 
         // of 360deg. If real units, ignore and treat as degrees, which are the implied user units for theta axis.
         theta = theta % 360.0;
         if(theta < 0) theta += 360.0;
         if(xUnits == Measure.Unit.PCT) theta = theta * 100.0 / 360.0;
         else theta = (thetaAxisReversed ? -1 : 1) * (theta - thetaZeroAngle); 
         coords[0] = new Measure(Utilities.limitSigAndFracDigits(theta,6,3), xUnits);

         // if radial coord is in % units, interpret as % of the viewport's smaller dimension. If physical units, just 
         // convert from thousandth-inches. If user units, then scale thousandth-in to user units and account for the 
         // radial value at the origin and the direction of increasing values for the radial axis.
         Measure m;
         if(yUnits != Measure.Unit.PCT && yUnits != Measure.Unit.USER)
            m = Measure.getConstrainedRealMeasure(r, yUnits, new Measure.Constraints(7,p,false));
         else if(yUnits == Measure.Unit.PCT)
         {
            double minDim = width;
            if(minDim > height) minDim = height;
            if(minDim == 0) return(null);
            r *= 100.0 / minDim;
            m = new Measure(Utilities.limitSigAndFracDigits(r, 7, p), yUnits);
         }
         else
         {
            if(usrToRealH == 0) return(null);
            r /= usrToRealH;
            if(radialAxisReversed)
            {
               if(userType == SEMILOGRCOORDS) r = radiusAtOrigin - Math.pow(10, r);
               else r = radiusAtOrigin - r;
            }
            else
            {
               if(userType == SEMILOGRCOORDS)
                  r = radiusAtOrigin + Math.pow(10,r);
               else
                  r = radiusAtOrigin + r;
            }
            m = new Measure(Utilities.limitSigAndFracDigits(r, 7, p), yUnits);
         }
         coords[1] = m; 
      }
      else
      {
         // for loglog, semilog, and cartesian coord systems, the axes are orthogonal -- so each coordinate is 
         // processed separately
         Measure m;
         if(xUnits != Measure.Unit.PCT && xUnits != Measure.Unit.USER) 
            m = Measure.getConstrainedRealMeasure(x, xUnits, new Measure.Constraints(7,p,false));
         else if(xUnits == Measure.Unit.PCT)
         {
            if(width == 0) return(null);
            x *= 100.0 / width;
            m = new Measure(Utilities.limitSigAndFracDigits(x, 7, p), xUnits);
         }
         else
         {
            if(usrToRealW == 0) return(null);
            x = x / usrToRealW + userOrigin.getX();
            if(userType == SEMILOGXCOORDS || userType == LOGLOGCOORDS)
               x = Math.pow( 10, x );
            m = new Measure(Utilities.limitSigAndFracDigits(x, 7, p), xUnits);
         }
         coords[0] = m;

         if(yUnits != Measure.Unit.PCT && yUnits != Measure.Unit.USER) 
            m = Measure.getConstrainedRealMeasure(y, yUnits, new Measure.Constraints(7,p,false));
         else if(yUnits == Measure.Unit.PCT)
         {
            if(height == 0) return(null);
            y *= 100.0 / height;
            m = new Measure(Utilities.limitSigAndFracDigits(y, 7, p), yUnits);
         }
         else
         {
            if(usrToRealH == 0) return(null);
            y = y / usrToRealH + userOrigin.getY();
            if(userType == SEMILOGYCOORDS || userType == LOGLOGCOORDS)
               y = Math.pow( 10, y );
            m = new Measure(Utilities.limitSigAndFracDigits(y, 7, p), yUnits);
         }
         coords[1] = m;

      }
      return(coords);
   }

   /**
    * Map a point from the viewport's user coordinate system to its rendered coordinate system expressed in 
    * milli-inches. If the point cannot be converted for whatever reason (a negative coordinate in a loglog or semilog 
    * coordinate system, for example), its coordinates are reset to {@link Double#NaN} to indicate that the conversion 
    * is undefined. 
    * 
    * @param point The point to be converted, with coords in "user" units. The point is modified in place. If it is 
    * null, the method does nothing.
    */
   public void userUnitsToThousandthInches(Point2D point)
   {
      if(point == null) return;
      double x = point.getX();
      double y = point.getY();

      if(userType == POLARCOORDS || userType == SEMILOGRCOORDS)
      {
         double theta = x;
         double r = y;

         // convert radial coordinate to physical units
         if(radialAxisReversed)
         {
            if(r > radiusAtOrigin) r = Double.NaN;
            else if(userType==SEMILOGRCOORDS) r = Utilities.log10(radiusAtOrigin) - Utilities.log10(r);
            else r = radiusAtOrigin - r;
         }
         else
         {
            if(r < radiusAtOrigin) r = Double.NaN;
            else if(userType == SEMILOGRCOORDS) r = Utilities.log10(r) - Utilities.log10(radiusAtOrigin);
            else r -= radiusAtOrigin;
         }
         r *= usrToRealH;

         // convert theta coordinate from the native polar coordinate system to a standard right-handed coordinate
         // system in which CCW angles are positive and 0 deg points rightward. Then restrict to unit circle and convert
         // to radians.
         theta = thetaZeroAngle + (thetaAxisReversed ? -1 : 1) * theta;
         theta = theta % 360.0;
         if(theta < 0) theta += 360.0;
         theta *= Math.PI / 180.0;

         // now convert polar to Cartesian and translate WRT physical location of viewport origin
         x = r * Math.cos( theta );
         y = r * Math.sin( theta );
         x += polarOrigin.getX();
         y += polarOrigin.getY();
      }
      else
      {
         if(userType == SEMILOGXCOORDS || userType == LOGLOGCOORDS)
            x = Utilities.log10(x);
         x = (x - userOrigin.getX()) * usrToRealW;

         if(userType == SEMILOGYCOORDS || userType == LOGLOGCOORDS)
            y = Utilities.log10(y);
         y = (y - userOrigin.getY()) * usrToRealH;
      }

      // modify provided point in place; if either coord is ill-defined, set both to NaN
      if(Utilities.isWellDefined(x) && Utilities.isWellDefined(y))
         point.setLocation(x, y);
      else
         point.setLocation(Double.NaN, Double.NaN);
   }

   /**
	 * Map an array of points from the viewport's user coordinate system to its rendered coordinate system in 
    * milli-inches. This conveniense method simply calls {@link #userUnitsToThousandthInches(Point2D)} for each
    * point in the array.
	 *
	 * @param points The array of points. Each non-null point in the array is modified in place.
	 */
	public void userUnitsToThousandthInches(Point2D[] points)
	{
		for(int i=0; i < points.length; i++)
			userUnitsToThousandthInches(points[i]);
	}

   /**
    * Map a list of points from the viewport's user coordinate system to its rendered coordinate system in milli-inches.
    * This convenience method simply calls {@link #userUnitsToThousandthInches(Point2D)} for each point in the list.
    * 
    * @param points The list of points. Each non-null point in the list is modified in place.
    */
   public void userUnitsToThousandthInches(List<Point2D> points)
   {
      if(points == null || points.size() == 0) return;
      for(Point2D p : points)
         userUnitsToThousandthInches(p);
   }

	/** 
	 * Calculate the physical distance between two points expressed in user coordinates with respect to this viewport. 
	 * 
	 * @param p0 One point in viewport, assumed to be in user coordinates.
	 * @param p1 A second point in viewport, assumed to be in user coordinates.
	 * @return Physical distance between the points IAW viewport definition, in milli-inches. If either point is 
    * ill-defined, 0 is returned.
	 */
	public double convertUserDistanceToThousandthInches(Point2D p0, Point2D p1)
	{
      if(p0 == null || p1 == null) return(0);

		// copy the points since we do not claim to modify them!
		Point2D copyP0 = new Point2D.Double(p0.getX(), p0.getY());
		Point2D copyP1 = new Point2D.Double(p1.getX(), p1.getY());

		// convert to physical locations in thousandth-in
		userUnitsToThousandthInches(copyP0);
		userUnitsToThousandthInches(copyP1);

		// if either point is ill-defined, return 0; else return computed distance in thousandth-in
		if( copyP0.getX() == Double.NaN || copyP1.getX() == Double.NaN )
			return( 0.0 );
		else
			return( copyP0.distance( copyP1 ) );
	}
}
