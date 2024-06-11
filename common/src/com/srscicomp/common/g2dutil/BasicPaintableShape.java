package com.srscicomp.common.g2dutil;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;

/**
 * A simple immutable implementation of the <code>PaintableShape</code> interface.
 * 
 * @author  sruffner
 */
public class BasicPaintableShape implements PaintableShape
{
   private final Shape designShape;
   private final boolean isClosed;
   private final boolean isHSymmetric;
   private final boolean isVSymmetric;

   /**
    * Construct a <code>BasicPaintableShape</code> representing the unit circle.
    */
   public BasicPaintableShape()
   {
      this(null, true, true, true);
   }

   /**
    * Construct a <code>BasicPaintableShape</code>.
    * 
    * @param designShape The design of the shape, usually specified within a 1x1 design box such that the origin is at 
    * the center of the box, the x-axis increases rightward, and the y-axis increases upward. Design shapes should 
    * adhere to these conventions to support scalability in the <code>Painter</code> framework. If <code>null</code>, 
    * the remaining arguments are ignored, and a unit circle is assumed.
    * @param isClosed Set <code>true</code> for a closed shape.
    * @param isHSymmetric Set <code>true</code> for a shape that is symmetric about its x-axis in design space.
    * @param isVSymmetric Set <code>true</code> for a shape that is symmetric about its y-axis in design space.
    */
   public BasicPaintableShape(Shape designShape, boolean isClosed, boolean isHSymmetric, boolean isVSymmetric)
   {
      if(designShape != null)
      {
         this.designShape = designShape;
         this.isClosed = isClosed;
         this.isHSymmetric = isHSymmetric;
         this.isVSymmetric = isVSymmetric;   
      }
      else
      {
         this.designShape = new Ellipse2D.Double(-0.5, -0.5, 1, 1);
         this.isClosed = true;
         this.isHSymmetric = true;
         this.isVSymmetric = true;
      }
   }
   public Shape getDesignShape()
   {
      return(designShape);
   }

   public boolean isClosed()
   {
      return(isClosed);
   }

   public boolean isHSymmetric()
   {
      return(isHSymmetric);
   }

   public boolean isVSymmetric()
   {
      return(isVSymmetric);
   }

}
