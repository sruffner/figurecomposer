package com.srscicomp.common.g2dutil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import com.srscicomp.common.util.Utilities;

/**
 * <b>RadialSectionPainter</b> is a {@link Painter} which renders a set of concentric radial sections and/or pie
 * wedges, optionally filling each section with a different color. 
 * 
 * <p>The locations from this painter's "location producer" are expected to be in polar coordinates (theta, r), where
 * theta is the angular coordinate in degrees CCW and r is the radial distance from the origin, in the logical units of
 * the painter's graphical context. One radial section is defined by each pair of points (theta0, r0) and (theta1, r1)
 * from the producer, where theta0 != theta1 and r1 > r0. If r0 == 0, a pie wedge is rendered, connecting the endpoints 
 * of the arc at r=r1 between theta0 and theta1 to the polar origin. Otherwise, a radial section is formed: CCW arc from
 * (theta0, r1) to (theta1, r1); line to (theta1, r0); CW arc to (theta0, r0); line to (theta, r1). Any ill-defined 
 * section (theta0 == theta1; r1==r0; r1 < 0 or r0 < 0) is skipped.</p>
 * 
 * <p><i><b>IMPORTANT</b>: Like all {@link Painter} implementations, <b>RadialSectionPainter</b> assumes the target 
 * coordinate system has a y-axis increasing upwards and an x-axis increasing rightwards. Also, it always traces the 
 * section in a CCW direction as described. Take, e.g., section coordinates (100deg, 1) and (80deg, 2). Since the 
 * section is always traced CCW, the resulting radial section will have an extent of 340deg, not 20.</i></p>
 * 
 * <p>If the painter has a "color producer", each well-defined radial section is filled with the corresponding color
 * from the color producer. If no color producer is specified, the painter's fill color is applied to all sections.</p>
 * 
 * <p>All rendered radial sections are stroked IAW the painter's style.</p>
 * 
 * @author  sruffner
 */
public class RadialSectionPainter extends Painter
{
   /**
    * The common origin for all radial sections rendered by this pointer. It is assumed to be expressed in the logical 
    * coordinate system of the painter's graphics context.
    */
   private final Point2D origin = new Point2D.Double(0,0);

   /**
    * Set the common origin for all radial sections rendered by this painter.
    * 
    * @param p The origin, assumed to be in the logical coordinate system of the painter's graphics context. A private 
    * copy of the point is made. If null, (0,0) is assumed.
    */
   public void setOrigin(Point2D p)
   {
      origin.setLocation( (p == null) ? 0 : p.getX(), (p == null) ? 0 : p.getY() );
   }

   /** 
    * If null, all radial sections are filled IAW the painter's fill color; else, each section is filled with the
    * corresponding color generated by this color producer. If there are fewer colors than sections, the color 
    * producer is "recycled" as needed.
    */
   private Iterable<Color> colorProducer = null;
   
   /**
    * Set the color producer for this painter. During rendering, the painter will query this producer for an iterator
    * over the fill color applied to each radial section.
    * 
    * @param producer The color producer. It will replace any existing producer. If null, the painter will fill all
    * radial sections with the painter's own fill color.
    */
   public void setColorProducer(Iterable<Color> producer) {colorProducer = producer; }
   

   /**
    * Construct a <b>RadialSectionPainter</b> with no location or color producer, an origin at (0,0), and a default 
    * painter style. The painter will render nothing.
    */
   public RadialSectionPainter()
   {
      this(null, null, null, null);
   }

   /**
    * Construct a <b>RadialSectionPainter</b> to render each well-defined section supplied by the specified location
    * producer.
    * 
    * @param style Collection of graphic attributes applied to this painter.  If null, the painter uses defaults.
    * @param origin The location of the common origin shared by all radial sections, specified in the logical coordinate
    * system of the graphics context in which this painter will render itself. If null, (0,0) is assumed.
    * @param producer The location producer for this painter. Each radial section is defined by each pair of points
    * from this producer, as described in the class header. If null, then the painter renders nothing!
    * @param clrProd The color producer for this painter. If specified, each well-defined radial section is filled with
    * the corresponding color from this producer; otherwise, all sections are filled IAW the <i>style</i> argument.
    */
   public RadialSectionPainter(PainterStyle style, Point2D origin, Iterable<Point2D> producer, Iterable<Color> clrProd)
   {
      super(style, producer);
      setOrigin(origin);
      setColorProducer(clrProd);
   }

   
   //
   // Painting the radial sections
   //

   /**
    * The interval, in number of radial sections completed, at which the painter reports progress and checks for job 
    * cancellation during a rendering pass.
    */
   private final static int PROGRESSINTV = 50;

   @Override protected boolean paintInternal(Graphics2D g2d)
   {
      // check for obvious cases in which nothing is rendered
      boolean isStroked = style.isStroked();
      boolean isFilled = (colorProducer != null) || (style.getFillColor().getAlpha() != 0);
      if(locationProducer == null || !(isStroked || isFilled)) 
         return(true);

      GeneralPath section =  new GeneralPath();
      Arc2D arc = new Arc2D.Double();

      // draw each arc in turn...
      int nArcsDone = 0;
      g2d.setColor(style.getStrokeColor());
      g2d.setStroke(style.getStroke(0));
      
      Iterator<Point2D> locIterator = locationProducer.iterator();
      Iterator<Color> colorIterator = null;
      if(colorProducer != null)
      {
         colorIterator = colorProducer.iterator();
         if(!colorIterator.hasNext()) colorIterator = null;  // protect against an empty color producer!
      }
      Point2D p0 = null;
      Point2D p1 = null;
      while(locIterator.hasNext())
      {
         // get next pair of points (theta0,r0) and (theta1,r1) from location provider
         p0 = locIterator.next();
         if(!locIterator.hasNext()) break;
         p1 = locIterator.next();

         // get next fill color from color producer, if specified; else use painter's fill color
         Color fillC = null;
         if(colorIterator != null)
         {
            // recycle the fill colors from the color producer as needed
            if(!colorIterator.hasNext())
               colorIterator = colorProducer.iterator();
            fillC = colorIterator.next();
         }
         if(fillC == null) fillC = style.getFillColor();
         
         // both points must be well-defined with r1 > r0 and theta0 != theta1. Else skip.
         if(!(Utilities.isWellDefined(p0) && Utilities.isWellDefined(p1) && 
               (p1.getY() > p0.getY()) && (p0.getX() != p1.getX()))) 
         {
            ++nArcsDone;
            continue;
         }

         // arc radii in logical units; start angle, end angle, and angular extent of radial section in degrees
         double r0 = p0.getY();
         double r1 = p1.getY();
         double startDeg = Utilities.restrictAngle(p0.getX());
         double endDeg = Utilities.restrictAngle(p1.getX());
         double extentDeg = (endDeg>startDeg) ? endDeg-startDeg : endDeg+360-startDeg;

         // paint the radial section or pie wedge.
         // NOTE: Arc start angle and angular extent are flipped in sign b/c we assume a coord system 
         // in which y-axis increases upward rather than downward.
         section.reset();
         arc.setArc(origin.getX()-r1, origin.getY()-r1, r1*2, r1*2, -startDeg, -extentDeg, Arc2D.OPEN);
         section.append(arc, false);
         if(r0 == 0)
         {
            section.lineTo(origin.getX(), origin.getY());
            section.closePath();
         }
         else
         {
            arc.setArc(origin.getX()-r0, origin.getY()-r0, r0*2, r0*2, -endDeg, extentDeg, Arc2D.OPEN);
            section.append(arc, true);
            section.closePath();
         }
         g2d.setColor(style.getFillColor());
         g2d.fill(section);
         g2d.setColor(style.getStrokeColor());
         g2d.draw(section);

         // report progress and check for cancellation every once in a while
         ++nArcsDone;
         if(nArcsDone >= PROGRESSINTV)
         {
            nArcsDone = 0;
            if(stopPainting()) return(false);
         }

      }

      return(true);
   }

   @Override protected void recalcBounds2D(Rectangle2D r)
   {
      // start out with an empty rectangle
      r.setFrame(0, 0, 0, 0);

      // check for obvious cases in which nothing is rendered
      boolean isStroked = style.isStroked();
      boolean isFilled = (colorProducer != null) || (style.getFillColor().getAlpha() != 0);
      if(locationProducer == null || !(isStroked || isFilled)) 
         return;

      // find bounding box enclosing all valid radial sections
      boolean gotValidSection = false;
      double xMin = Double.POSITIVE_INFINITY;
      double xMax = Double.NEGATIVE_INFINITY;
      double yMin = Double.POSITIVE_INFINITY;
      double yMax = Double.NEGATIVE_INFINITY;
      Iterator<Point2D> locIterator = locationProducer.iterator();
      Point2D p0 = null;
      Point2D p1 = null;
      while(locIterator.hasNext())
      {
         // get next pair of points from location provider
         p0 = locIterator.next();
         if(!locIterator.hasNext()) break;
         p1 = locIterator.next();

         // both points must be well-defined with r1 > r0 and theta0 != theta1. Else skip.
         if(!(Utilities.isWellDefined(p0) && Utilities.isWellDefined(p1) && 
               (p1.getY() > p0.getY()) && (p0.getX() != p1.getX()))) 
            continue;


         gotValidSection = true;
         double r1 = p1.getY();   // radius of outer arc or radial section or pie wedge

         double start = Utilities.restrictAngle(p0.getX());
         double end = Utilities.restrictAngle(p1.getX());
         if(end < start) end += 360;

         double radians = Math.toRadians(start);
         double x = origin.getX() + r1 * Math.cos(radians);
         double y = origin.getY() + r1 * Math.sin(radians);
         if(x < xMin) xMin = x;
         if(x > xMax) xMax = x;
         if(y < yMin) yMin = y;
         if(y > yMax) yMax = y;

         radians = Math.toRadians(end);
         x = origin.getX() + r1 * Math.cos(radians);
         y = origin.getY() + r1 * Math.sin(radians);
         if(x < xMin) xMin = x;
         if(x > xMax) xMax = x;
         if(y < yMin) yMin = y;
         if(y > yMax) yMax = y;

         // if arc passes thru 0, 90, 180 or 270, then the arc's bounds include one or more of the edges of the 
         // square that bounds the containing circle
         while(end >= start)
         {
            int qtrAngle = (((int)(end/90)) * 90);
            if(qtrAngle <= start) break;

            qtrAngle = qtrAngle % 360;
            if(qtrAngle == 0 && (origin.getX() + r1) > xMax) xMax = origin.getX() + r1;
            else if(qtrAngle == 90 && (origin.getY() + r1) > yMax) yMax = origin.getY() + r1;
            else if(qtrAngle == 180 && (origin.getX() - r1) < xMin) xMin = origin.getX() - r1;
            else if(qtrAngle == 270 && (origin.getY() - r1) < yMin) yMin = origin.getY() - r1;

            end -= 90;
         }
      }

      // now grow bounding box by 1/2 of painter's stroke width -- if the radial sections are stroked
      if(gotValidSection && isStroked)
      {
         double grow = style.getStrokeWidth();
         r.setRect(xMin-grow/2, yMin-grow/2, xMax-xMin+grow, yMax-yMin+grow);
      }
   }
}
