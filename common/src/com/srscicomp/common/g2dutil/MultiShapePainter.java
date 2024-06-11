package com.srscicomp.common.g2dutil;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.srscicomp.common.util.Utilities;

/**
 * This {@link Painter} implementation can draw any number of shapes, each of which may be rendered with different
 * stroke and fill properties. The multiple shape painter does not render any text. 
 * 
 * <p>To support the rendering of so-called stem plots, in which a "stem line" is drawn from the center of a marker
 * symbol to some defined baseline, the painter may be configured to draw a line segment with each shape drawn.</p>
 * 
 * <p><i>Usage</i>. Construct the painter and supply it with an iterator over any number of {@link PaintedShape}
 * objects. This interface defines the properties required of each shape rendered by <b>MultiShapePainter</b>: the
 * shape primitive drawn with respect to an origin at (0,0); the shape's location in the graphics context in which all
 * shapes are drawn; location of the second end point of the line segment drawn from the shape's location (optional); 
 * and the {@link PainterStyle} properties to be applied when rendering the shape and the optional associated line 
 * segment. All drawing coordinates should be WRT the logical coordinate system of the graphics context.</p>
 * <p><b>MultiShapePainter</b> supports canceling an ongoing paint operation; it will check for cancellation of the
 * rendering task after painting every 50 shapes.</p>
 * 
 * @author sruffner
 */
public class MultiShapePainter extends Painter
{
   /** Construct a <b>MultiShapePainter</b> with no shape producer. It renders nothing! */
   public MultiShapePainter() { this(null); }
   
   /**
    * Construct a <b>MultiShapePainter</b>.
    * @param producer Iterates over the list of painted shapes to be drawn by this painter. If null, the painter will
    * not make any marks.
    */
   public MultiShapePainter(Iterable<PaintedShape> producer) { shapeProducer = producer; }
   
   /**
    * Set the shape producer for this painter. During rendering, the painter will query this producer for an iterator
    * over the painted shapes to be drawn.
    * 
    * @param producer The shape producer. It will replace any existing producer. If null, the painter will make no
    * marks.
    */
   public void setShapeProducer(Iterable<PaintedShape> producer) {shapeProducer = producer; }
   
   @Override protected boolean paintInternal(Graphics2D g2d)
   {
      if(shapeProducer == null) return(true);
      
      int nShapesPainted = 0;
      double xPrev = 0;
      double yPrev = 0;
            
      for(PaintedShape s : shapeProducer)
      {
         Point2D loc = s.getLocation();
         if(!Utilities.isWellDefined(loc))
         {
            ++nShapesPainted;
            continue;
         }
         
         // translate origin from previous point to the current point
         double dx = loc.getX() - xPrev;
         double dy = loc.getY() - yPrev;
         g2d.translate(dx, dy);
         xPrev = loc.getX();
         yPrev = loc.getY();
         
         // draw stem line if there is one. Since we've moved the origin, we have to adjust the end point of the
         // stem line so it's drawn WRT an origin at the shape's center.
         Point2D pEnd = s.getStemEnd();
         PainterStyle stemPS = s.getStemPainterStyle();
         if(stemPS == null) stemPS = s;
         if(stemPS.isStroked() && Utilities.isWellDefined(pEnd))
         {
            g2d.setStroke(stemPS.getStroke(0));
            g2d.setColor(stemPS.getStrokeColor());
            g2d.draw(new Line2D.Double(0, 0, pEnd.getX()-loc.getX(), pEnd.getY()-loc.getY()));
         }
         
         Shape shape = s.getShape();
         if(isFilled(s))
         {
            Paint fillPaint = s.getFillPaint();
            if(fillPaint == null) fillPaint = s.getFillColor();
            g2d.setPaint(fillPaint);
            g2d.fill(shape);
         }
         if(s.isStroked())
         {
            g2d.setStroke(s.getStroke(0));
            g2d.setColor(s.getStrokeColor());
            g2d.draw(shape);
         }

         // check for render task cancellation at regular intervals
         ++nShapesPainted;
         if(nShapesPainted >= 50)
         {
            nShapesPainted = 0;
            if(stopPainting()) return(false);
         }
      }
      
      return(true);
   }

   @Override protected void recalcBounds2D(Rectangle2D r)
   {
      // start out with an empty rectangle
      r.setFrame(0, 0, 0, 0);
      if(shapeProducer == null) return;

      // iterate over all shapes and find the left, right, top and bottom edges of the rectangle bounding all of them,
      // and any stem lines that are drawn.
      double xMin = Double.POSITIVE_INFINITY;
      double xMax = Double.NEGATIVE_INFINITY;
      double yMin = Double.POSITIVE_INFINITY;
      double yMax = Double.NEGATIVE_INFINITY;

      for(PaintedShape s : shapeProducer)
      {
         Point2D loc = s.getLocation();
         if(!Utilities.isWellDefined(loc)) continue;
         
         Shape shape = s.getShape();
         if(s.isStroked() || (s.isClosed() && s.getFillColor().getAlpha() != 0))
         {
            Rectangle2D bounds = 
                  s.isStroked() ? s.getStroke(0).createStrokedShape(shape).getBounds2D() : shape.getBounds2D();
            
            double x = loc.getX() + bounds.getX();
            if(x < xMin) xMin = x;
            x += bounds.getWidth();
            if(x > xMax) xMax = x;
            
            double y = loc.getY() + bounds.getY();
            if(y < yMin) yMin = y;
            y += bounds.getHeight();
            if(y > yMax) yMax = y;
            
            Point2D pEnd = s.getStemEnd();
            PainterStyle stemPS = s.getStemPainterStyle();
            if(stemPS == null) stemPS = s;
            if(stemPS.isStroked() && Utilities.isWellDefined(pEnd))
            {
               double halfSW = stemPS.getStrokeWidth() / 2.0;
               if(pEnd.getX()-halfSW < xMin) xMin = pEnd.getX()-halfSW;
               if(pEnd.getX()+halfSW > xMax) xMax = pEnd.getX()+halfSW;
               if(pEnd.getY()-halfSW < yMin) yMin = pEnd.getY()-halfSW;
               if(pEnd.getY()+halfSW > yMax) yMax = pEnd.getY()+halfSW;

            }
         }
      }
      
      // at least one shape must be rendered, or the bounding rectangle is empty
      if(Utilities.isWellDefined(xMin)) r.setRect(xMin, yMin, xMax-xMin, yMax-yMin);
   }

   /** Takes no action -- because this painter implementation never renders text. */
   @Override public void updateFontRenderContext(Graphics2D g2d) {}


   /** 
    * Interface defining the properties of a single shape rendered by <b>MultiShapePainter</b>. It encapsulates the
    * shape's location and path primitive as well as the graphics styles which define how it is stroked and/or filled.
    */
   public interface PaintedShape extends PainterStyle
   {
      /** 
       * Get the shape primitive to be drawn on the graphics context.
       * @return The shape to be drawn, assumed to be in the logical coordinate system of the painter's graphic context.
       */
      Shape getShape();
      /**
       * Get the (X,Y) coordinates of the shape's location. Before drawing the shape, the graphic context's current
       * origin is translated to this location.
       * @return The shape's location, in logical coordinate system of the painter's graphic context.
       */
      Point2D getLocation();
      /**
       * Get stem line segment end point. This is an optional feature: the <b>MultiShapePainter</b> can draw a line from
       * each shape's center point as specified by {@link #getLocation()} to the point returned by this method. 
       * Note that the line segment will be drawn before the corresponding shape is drawn.
       * @return The line segment's end point. Can be null or (NaN,NaN) -- in which case the line segment is not drawn.
       */
      Point2D getStemEnd();
      /**
       * Get the graphics style with which the stem line segment, if any, is rendered. See {@link #getStemEnd()}.
       * @return Painter style for the stem line. If null, then the stem line is rendered using the same stroke as the
       * painted shape.
       */
      PainterStyle getStemPainterStyle();
      /**
       * Get the color pattern used to fill the shape. 
       * @return The color pattern used fill shape. It may be a solid color, a gradient fill, or some other kind of
       * pattern satisfying the {@link Paint} interface. If null, the shape is filled with the solid color specified by
       * specified by {@link #getFillColor()}.
       */
      Paint getFillPaint();
      /**
       * Is this a closed shape? Only closed shapes will be filled by the <b>MultiShapePainter</b>.
       * @return True if shape is closed; else false.
       */
      boolean isClosed();
   }
   
   /** 
    * Producer iterates over the list of shapes rendered by this painter. Each painted shape includes its own 
    * location, path primitive, associated stem line end point, and graphics styling. 
    */
   private Iterable<PaintedShape> shapeProducer;
   
   /**
    * Should the painted shape be filled or not? The shape must be a closed path, and it must have a non-null fill
    * pattern that's not a solid color with zero alpha.
    * 
    * @param s The painted shape.
    * @return True if shape should be filled.
    */
   private boolean isFilled(PaintedShape s)
   {
      boolean filled = s.isClosed();
      if(filled)
      {
         Paint fillPaint = s.getFillPaint();
         if(fillPaint == null)
            filled = (s.getFillColor().getAlpha() != 0);
         else
            filled = (!(fillPaint instanceof Color)) || (((Color) fillPaint).getAlpha() != 0);
      }
      return(filled);
   }
}
