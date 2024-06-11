package com.srscicomp.common.g2dutil;

import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.srscicomp.common.util.Utilities;

/**
 * <code>CircularArcPainter</code> is a <code>Painter</code> which paints one or more concentric circular arcs with an 
 * adornment drawn at the first endpoint, second endpoint, and/or midpoint of each arc.
 * 
 * <p>The painter renders an arc for each pair of points generated by its "location producer". Each point is assumed to 
 * be in polar coordinates WRT a shared origin: (theta0, r0) and (theta1, r1), where r0==r1. Angular coordinates are 
 * assumed to be in degrees, while radial coordinates should be in the logical units of the <code>Graphics2D</code> 
 * context in which the painter will render itself. If theta0==theta1, or r0!=r1, or if either point in the pair is 
 * ill-defined, that pair is ignored.</p>
 * 
 * <p><em><strong>IMPORTANT</strong>: Like all <code>Painter</code> implementations, <code>CircularArcPainter</code> 
 * assumes the target coordinate system has a y-axis increasing upwards and an x-axis increasing rightwards. Also, it 
 * always traces the arc in a CCW direction from the first to the second endpoint. Take, e.g., theta0 = 100deg and
 * theta1 = 80deg. Since arcs are always traced CCW, the resulting arc will have an extent of 340deg, not 20.</em></p>
 * 
 * <p>The arc's adornment is characterized by a <code>PaintableShape</code>. The 
 * adornment is oriented such that its positive x-axis in "design space" is colinear with and pointing in the same 
 * direction as the tangent ray emanating from the arc at the adornment's location in the direction toward the second 
 * endpoint. If the shape is <em>not</em> vertically symmetric, it will be flipped 180deg at the second endpoint of 
 * the arc -- a useful feature if the arc is to be adorned with arrows or similar shapes at the endpoints. If this 
 * default flip behavior is not desirable, you can override it. Finally, the adornment may optionally have a text label 
 * centered horizontally and vertically about the adornment's center point. The label is drawn in the stroke color 
 * rather than the fill/text color specified by the <code>PainterStyle</code> assigned to the adornment (else it would 
 * be obliterated inside a filled adornment!). Also, when the adornment at the second endpoint is flipped 180deg, the 
 * accompanying label is <em>not</em>, so that it "reads" the same way as the labels at the other endpoints.</p>
 * 
 * <p>Separate <code>PainterStyle</code>s may be specified for rendering the arc vs the adornment. Internally, 
 * <code>CircularArcPainter</code> delegates the task of rendering the adornments to a <code>ShapePainter</code>.</p>
 * 
 * <p><em>"Filling" an arc</em>. By default, <code>CircularArcPainter</code> only strokes each well-defined arc. 
 * However, by changing the painter's fill type, one can instead fill the chord, pie wedge, or radial section defined 
 * by the arc. A radial section is bound by the defined arc and a second circular arc at a different "reference radius", 
 * but with the same angular.  If the reference radius is zero, then the radial section is a pie wedge. For details, 
 * see methods <code>setFillType()</code> and <code>setReferenceRadius()</code>.
 * 
 * @author  sruffner
 */
public class CircularArcPainter extends Painter
{
   /** If set, the adornment (if there is one) will be rendered at the first endpoint of the arc. */
   private boolean isP0Adorned = false;

   /** If set, the adornment (if there is one) will be rendered at the second endpoint of the arc. */
   private boolean isP1Adorned = false;

   /** If set, the adornment (if there is one) will be rendered at the midpoint of the arc. */
   private boolean isMidAdorned = false;

   /** "Flip" policy for a non-vertically symmetric adornment at the first endpoint of the arc. */
   private boolean doFlip = true;

  /**
    * Set the properties of any adornments drawn along the arc by this <code>CircularArcPainter</code>.
    * 
    * @param style The painter style with which adornments should be rendered. If <code>null</code>,
    * any adornments are drawn using the painter style applied to the arc itself.
    * @param shape The <code>PaintableShape</code> that represents the adornment to be rendered, usually defined 
    * within a 1x1 "design box" (but this is not required). If <code>null</code>, no adornments will be rendered.
    * @param size The desired size of the adornment(s), in logical units. The adornment will be scaled by this value 
    * both horizontally and vertically at render time. If negative or zero, no adornments will be drawn.
    * @param label An optional label to be centered horizontally and vertically about each location at which the 
    * adornment is drawn. The label is drawn using the stroke color rather than the fill/text color of the 
    * <code>PainterStyle</code> in which adornment is rendered. If <code>null</code> or empty string, no label is 
    * drawn.
    * @param atP0 If <code>true</code>, the adornment will be rendered at the first endpoint of the arc.
    * @param atP1 If <code>true</code>, the adornment will be rendered at the second endpoint of the arc.
    * @param atMid If <code>true</code>, the adornment will be rendered at the midpoint of the arc.
    * @param doFlip If <code>true</code>, a non-vertically symmetric adornment will be rotated 180deg at the first 
    * endpoint, so it "opposes" the unflipped adornment drawn at the second endpoint. If <code>false</code>, no flip is 
    * performed.
    */
   public void setAdornment(PainterStyle style, PaintableShape shape, float size, String label, boolean atP0, 
         boolean atP1, boolean atMid, boolean doFlip)
   {
      this.isP0Adorned = atP0;
      this.isP1Adorned = atP1;
      this.isMidAdorned = atMid;
      this.doFlip = doFlip && shape != null && !shape.isVSymmetric();
      
      // update our internal shape painter and force a recalculation of render bounds
      adornPainter.setStyle((style != null) ? style : this.style);
      adornPainter.setPaintedShape(shape);
      adornPainter.setSize(shape == null ? 0f : size);
      adornPainter.setTextLabel(label); 
      adornPainter.invalidateBounds();
      invalidateBounds();
   }

   /**
    * Set the adornment text label as an attributed character string on this circular arc painter.
    * @param aStr The adornment label as an attributed string.
    */
   public void setAttributedAdornmentLabel(AttributedString aStr) { adornPainter.setTextLabel(aStr); }
   
   /**
    * The common center point for all arcs rendered by this <code>CircularArcPainter</code>. It is assumed to be 
    * expressed in the logical coordinate system of the graphics context in which the painter renders the arcs.
    */
   private final Point2D center = new Point2D.Double(0,0);

   /**
    * Set the common center for all arcs rendered by this <code>CircularArcPainter</code>. 
    * 
    * @param p The arc center point, assumed to be in the logical coordinate system of the graphics context in which 
    * the painter ultimately renders all arcs. A private copy of the point is made. If <code>null</code>, (0,0) is 
    * assumed.
    */
   public void setCenterPoint(Point2D p)
   {
      center.setLocation( (p == null) ? 0 : p.getX(), (p == null) ? 0 : p.getY() );
   }

   /**
    * Enumeration of closure types supported by <code>CircularArcPainter</code>. The closure type determines what kind 
    * of path is actually generated for each arc defined by the set of points obtained from the painter's location 
    * producer.
    * 
    * @author  sruffner
    */
   public enum Closure
   {
      /**
       * The arc is not closed.  It is stroked but not filled.
       */
      OPEN,

      /**
       * The arc is closed by drawing a straight line connecting its endpoints.
       */
      CHORD,

      /**
       * The arc defined by endpoints (theta0, r) and (theta1,r) is closed by forming a radial section: Let R be the 
       * "reference radius" of the <code>CircularArcPainter</code>. A straight line is drawn from (theta1,r) to 
       * (theta1,R), then a second arc is drawn <em>clockwise</em> from (theta1,R) to (theta0,R), then the path is 
       * closed. Of course, if R=0, there is no second arc and the radial section becomes a pie wedge.
       */
      SECTION
   }

    /**
    * The closure type for this <code>CircularArcPainter</code>
    */
   private Closure closure = Closure.OPEN;

   /**
    * Set the closure type for this <code>CircularArcPainter</code>. Three types of closure are supported:
    * <ul>
    *    <li>{@link Closure#OPEN} (the default): The arc path is not closed. It is not filled, only stroked.</li>
    *    <li>{@link Closure#CHORD}: The arc path is closed by a straight line connecting its endpoints. The path
    *    can be filled and stroked.</li>
    *    <li>{@link Closure#SECTION}: The arc path is closed by forming a radial section: Radial lines from the
    *    arc's endpoints are connected to a second arc drawn at the painter's "reference radius". If the reference 
    *    radius is zero, there is no second arc and the section becomes a pie wedge. It can be filled and stroked.</li>
    * </ul>
    * 
    * @param c The desired closure type.
    */
   public void setClosure(Closure c)
   {
      this.closure = (c == null) ? Closure.OPEN : c;
   }

   /**
    * The reference radius used to form a radial section for each defined arc whenever the painter's closure type is 
    * <code>Closure.SECTION</code>. The default value is zero, in which case the radial section is a pie wedge.
    */
   private double refRadius = 0;

   /**
    * Set the "reference radius" for this <code>CircularArcPainter</code>.
    * 
    * <p>The reference radius is only relevant when the painter's closure type is <code>Closure.SECTION</code>.</p>
    * 
    * @see CircularArcPainter#setClosure(Closure)
    * @param r The desired value for the reference radius, in logical units of the graphic context in which this painter 
    * will render itself. Nonpositive values are forced to 0.
    */
   public void setReferenceRadius(double r)
   {
      this.refRadius = (r<=0) ? 0 : r;
   }
 
   /**
    * Fill policy for this <code>CircularArcPainter</code>. If set, each closed arc path rendered by this painter is 
    * both filled and stroked; else, it is only stroked. Ignored if closure type is <code>Closure.OPEN</code>.
    */
   private boolean filled = false;

   /**
    * Set the fill policy for this <code>CircularArcPainter</code>.
    * 
    * @param filled If <code>true</code>, any closed arc paths rendered by this painter will be filled then stroked; 
    * otherwise, the paths are only stroked.
    */
   public void setFilled(boolean filled)
   {
      this.filled = filled;
   }

   /**
    * Construct a <code>CircularArcPainter</code> with no location producer, no adornment defined, a default painter 
    * style, and a default center point at (0,0). It is configured to only stroke open arcs. The painter constructed 
    * will render nothing.
    */
   public CircularArcPainter()
   {
      this(null, null, null);
   }

   /**
    * Construct a <code>CircularArcPainter</code>.  Initially, no adornment is defined, and the painter is configured 
    * to only stroke open arcs between each pair of well-defined polar points generated by its location producer.
    * 
    * @param style Collection of graphic attributes applied to this painter.  If <code>null</code>, then the painter
    * uses defaults.
    * @param producer The location producer for this painter. If <code>null</code>, then the painter renders nothing!
    * @param center The location of the common center point shared by all arcs drawn. It should be specified in the 
    * logical coordinate system of the <code>Graphics2D</code> context in which this painter will paint itself. If 
    * <code>null</code>, (0,0) is assumed.
    */
   public CircularArcPainter(PainterStyle style, Iterable<Point2D> producer, Point2D center)
   {
      super(style, producer);
      setCenterPoint(center);
      setClosure(Closure.OPEN);
      setFilled(false);
      setReferenceRadius(0);

      // set up internal painter for rendering adornments
      adornPainter = new ShapePainter();
      pAdorn = new Point2D.Double();

      List<Point2D> adornLoc = new ArrayList<>();
      adornLoc.add(pAdorn);
      adornPainter.setLocationProducer(adornLoc);
      setAdornment(null, null, 0f, null, false, false, true, true);
   }

   
   //
   // Painting the arcs
   //

   /**
    * The interval, in #arcs completed, at which a <code>CircularArcPainter</code> reports progress and checks for job 
    * cancellation during a rendering pass.
    */
   private final static int PROGRESSINTV = 50;

   /**
    * The <code>ShapePainter</code> used to draw endpoint/midpoint adornments for this <code>CircularArcPainter</code>. 
    * It is set up during a rendering pass.
    */
   private ShapePainter adornPainter = null;

   /**
    * The point at which an adornment is drawn by the internal <code>ShapePainter</code>. It is reused to draw all 
    * adornments during a rendering pass.
    */
   private Point2D pAdorn = null;

   @Override
   protected boolean paintInternal(Graphics2D g2d)
   {
      // check for obvious cases in which nothing is rendered
      boolean isStroked = style.isStroked();
      boolean isFilled = (closure != Closure.OPEN) && filled;
      boolean doAdorn = adornPainter.isRendered() && (isP0Adorned || isP1Adorned || isMidAdorned);
      if(locationProducer == null || !(isStroked || isFilled || doAdorn)) 
         return(true);

      // prepare internal ShapePainter used to draw the adornments, if any
      boolean flip = doAdorn && doFlip;

      // we'll need this to draw radial sections whenever the reference radius is not 0.
      GeneralPath section = null;
      if(closure == Closure.SECTION && refRadius > 0)
         section = new GeneralPath();

      // the arc type
      int arcType = Arc2D.OPEN;
      if(closure == Closure.CHORD) arcType = Arc2D.CHORD;
      else if(closure == Closure.SECTION && refRadius == 0) arcType = Arc2D.PIE;

      // draw each arc in turn...
      int nArcsDone = 0;
      g2d.setColor(style.getStrokeColor());
      g2d.setStroke(style.getStroke(0));
      Arc2D arc = new Arc2D.Double();
      Iterator<Point2D> locIterator = locationProducer.iterator();
      Point2D p0;
      Point2D p1;
      while(locIterator.hasNext())
      {
         // get next pair of points (theta0,r) and (theta1,r) from location provider
         p0 = locIterator.next();
         if(!locIterator.hasNext()) break;
         p1 = locIterator.next();

         // ignore pair if they're not both well-defined, if their radial coordinates are unequal, or if their 
         // theta coordinates are identical.
         if(!(Utilities.isWellDefined(p0) && Utilities.isWellDefined(p1) && 
               (p0.getY() == p1.getY()) && (p0.getX() != p1.getX()))) 
         {
            ++nArcsDone;
            continue;
         }

         // arc radius in logical units, start angle and angular extent of arc in radians
         double r = p0.getY();
         double startDeg = Utilities.restrictAngle(p0.getX());
         double endDeg = Utilities.restrictAngle(p1.getX());
         double extentDeg = (endDeg>=startDeg) ? endDeg-startDeg : endDeg+360-startDeg;
         double startRad = Math.toRadians(startDeg);
         double extentRad = Math.toRadians(extentDeg);

         // paint the arc itself.
         // NOTE: Arc start angle and angular extent are flipped in sign b/c we assume a coord system 
         // in which y-axis increases upward rather than downward.
         if(isStroked || isFilled)
         {
            if(section != null )
            {
               // the ugly case: a radial section w/ nonzero reference radius. Note that arc at reference radius is 
               // traced in a CW direction rather than CCW.
               section.reset();
               arc.setArc(center.getX()-r, center.getY()-r, r*2, r*2, -startDeg, -extentDeg, arcType);
               section.append(arc, false);
               arc.setArc(center.getX()-refRadius, center.getY()-refRadius, refRadius*2, refRadius*2, 
                     -endDeg, extentDeg, arcType);
               section.append(arc, true);
               section.closePath();
               if(isFilled) 
               {
                  g2d.setColor(style.getFillColor());
                  g2d.fill(section);
                  g2d.setColor(style.getStrokeColor());
               }
               g2d.draw(section);
            }
            else
            {
               // an open arc, chord, or pie
               arc.setArc(center.getX()-r, center.getY()-r, r*2, r*2, -startDeg, -extentDeg, arcType);
               if(isFilled) 
               {
                  g2d.setColor(style.getFillColor());
                  g2d.fill(arc);
                  g2d.setColor(style.getStrokeColor());
               }
               g2d.draw(arc);
            }
         }

         if(doAdorn)
         {
            if(isP0Adorned)
            {
               pAdorn.setLocation(center.getX() + r*Math.cos(startRad), center.getY() + r*Math.sin(startRad));
               adornPainter.setRotation(p0.getX() - 90);
               adornPainter.render(g2d, null);
            }
            if(isMidAdorned)
            {
               double w = startRad + extentRad/2;
               pAdorn.setLocation(center.getX() + r*Math.cos(w), center.getY() + r*Math.sin(w));
               adornPainter.setRotation(startDeg + extentDeg/2 - 90);
               adornPainter.render(g2d, null);
            }
            if(isP1Adorned)
            {
               double w = startRad + extentRad;
               pAdorn.setLocation(center.getX() + r*Math.cos(w), center.getY() + r*Math.sin(w));
               adornPainter.setRotation(startDeg + extentDeg - 90 + (flip ? 180 : 0));
               if(flip) adornPainter.setLabelRotation(-180);   // text is not flipped like shape is
               adornPainter.render(g2d, null);
               if(flip) adornPainter.setLabelRotation(0);
            }
         }

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

   @Override  protected void recalcBounds2D(Rectangle2D r)
   {
      // start out with an empty rectangle
      r.setFrame(0, 0, 0, 0);

      // check for obvious cases in which nothing is rendered
      boolean isStroked = style.isStroked();
      boolean isFilled = (closure != Closure.OPEN) && filled;
      boolean doAdorn = adornPainter.isRendered() && (isP0Adorned || isP1Adorned || isMidAdorned);
      if(locationProducer == null || !(isStroked || isFilled || doAdorn)) 
         return;

      // find bounding box enclosing all valid arcs
      boolean gotValidArc = false;
      double xMin = Double.POSITIVE_INFINITY;
      double xMax = Double.NEGATIVE_INFINITY;
      double yMin = Double.POSITIVE_INFINITY;
      double yMax = Double.NEGATIVE_INFINITY;
      Iterator<Point2D> locIterator = locationProducer.iterator();
      Point2D p0;
      Point2D p1;
      while(locIterator.hasNext())
      {
         // get next pair of points from location provider
         p0 = locIterator.next();
         if(!locIterator.hasNext()) break;
         p1 = locIterator.next();

         // ignore pair if they're not both well-defined, if their radial coordinates are unequal, or if their 
         // theta coordinates are identical.
         if(!(Utilities.isWellDefined(p0) && Utilities.isWellDefined(p1) && 
               (p0.getY() == p1.getY()) && (p0.getX() != p1.getX()))) 
            continue;

         gotValidArc = true;
         double rad = p0.getY();

         // here's the only situation in which the closure type affects bounding box: if closure type is section and the 
         // reference radius is larger than the arc radius, then we need to use the reference radius instead.
         if(closure == Closure.SECTION && rad < refRadius)
            rad = refRadius;

         double start = Utilities.restrictAngle(p0.getX());
         double end = Utilities.restrictAngle(p1.getX());
         if(end < start) end += 360;

         double radians = Math.toRadians(start);
         double x = center.getX() + rad * Math.cos(radians);
         double y = center.getY() + rad * Math.sin(radians);
         if(x < xMin) xMin = x;
         if(x > xMax) xMax = x;
         if(y < yMin) yMin = y;
         if(y > yMax) yMax = y;

         radians = Math.toRadians(end);
         x = center.getX() + rad * Math.cos(radians);
         y = center.getY() + rad * Math.sin(radians);
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
            if(qtrAngle == 0 && (center.getX() + rad) > xMax) xMax = center.getX() + rad;
            else if(qtrAngle == 90 && (center.getY() + rad) > yMax) yMax = center.getY() + rad;
            else if(qtrAngle == 180 && (center.getX() - rad) < xMin) xMin = center.getX() - rad;
            else if(qtrAngle == 270 && (center.getY() - rad) < yMin) yMin = center.getY() - rad;

            end -= 90;
         }
      }

      // now grow bounding box by 1/2 of the larger of: the arc's stroke width and the larger dimension of the 
      // adornment painter's bounding box (if applicable).
      if(gotValidArc)
      {
         double grow = isStroked ? style.getStrokeWidth() : 0;
         if(doAdorn)
         {
            adornPainter.getBounds2D(r);
            double s = Math.max(r.getWidth(),r.getHeight()) + adornPainter.style.getStrokeWidth();
            if(s > grow) grow = s;
         }
         r.setRect(xMin-grow/2, yMin-grow/2, xMax-xMin+grow, yMax-yMin+grow);
      }
   }
}
