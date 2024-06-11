package com.srscicomp.common.g2dutil;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;

/**
 * <code>Marker</code> is an enumeration of typical shapes that might be used as endcaps for line segments or as 
 * symbol markers along a polyline representation of a data set.  It implements the <code>PaintableShape</code> 
 * interface, so it can be passed to a <code>ShapePainter</code> for rendering.
 * 
 * @see ShapePainter, PaintableShape
 * @author 	sruffner
 */
public enum Marker implements PaintableShape
{
   TEE("M 0 -0.5 L 0 0.5 M 0.5 0 L -0.5 0", false, true, true),
   XHAIR("M -0.5 -0.5 L 0.5 0.5 M -0.5 0.5 L 0.5 -0.5", false, true, true),
   STAR("M 0 -0.5 L 0 0.5 M 0.5 0 L -0.5 0 M -0.5 -0.5 L 0.5 0.5 M -0.5 0.5 L 0.5 -0.5", false, true, true),
   CIRCLE(new Ellipse2D.Double(-0.5, -0.5, 1, 1)),
   OVAL12(new Ellipse2D.Double(-0.25, -0.5, 0.5, 1)),
   OVAL21(new Ellipse2D.Double(-0.5, -0.25, 1, 0.5)),
   BOX("M -0.5 -0.5 L -0.5 0.5 L 0.5 0.5 L 0.5 -0.5 Z"),
   RECT12("M -0.25 -0.5 L -0.25 0.5 L 0.25 0.5 L 0.25 -0.5 Z"),
   RECT21("M -0.5 -0.25 L -0.5 0.25 L 0.5 0.25 L 0.5 -0.25 Z"),
   DIAMOND("M 0 -0.5 L -0.5 0 L 0 0.5 L 0.5 0 Z"),
   DIAMOND12("M 0 -0.5 L -0.25 0 L 0 0.5 L 0.25 0 Z"),
   DIAMOND21("M 0 -0.25 L -0.5 0 L 0 0.25 L 0.5 0 Z"),
   PARALLELOGRAM("M -0.5 -0.5 L -0.375 0.5 L 0.5 0.5 L 0.375 -0.5 Z", true, false, false),
   PENTAGRAM("M 0 0.5 L 0.2939 -0.4045 L -0.4755 0.1545 L 0.4755 0.1545 L -0.2939 -0.4045 Z", true, true, false),
   HEXAGRAM("M -0.5 0 L 0.25 0.433 L 0.25 -0.433 L -0.5 0 M 0.5 0 L -0.25 -0.433 L -0.25 0.433 Z"),
   PENTAGON("M 0 0.5 L 0.4755 0.1545 L 0.2939 -0.4045 L -0.2939 -0.4045 L -0.4755 0.1545 Z", true, true, false),
   HEXAGON("M -0.5 0 L -0.25 0.433 L 0.25 0.433 L 0.5 0 L 0.25 -0.433 L -0.25 -0.433 Z"),
   OCTAGON("M -0.5 0.2071 L -0.2071 0.5 L 0.2071 0.5 L 0.5 0.2071 L 0.5 -0.2071 L 0.2071 -0.5 L -0.2071 -0.5 L -0.5 -0.2071 Z"),
   UPTRIANGLE("M -0.5 -0.433 L 0 0.433 L 0.5 -0.433 Z", true, true, false),
   DOWNTRIANGLE("M 0.5 0.433 L 0 -0.433 L -0.5 0.433 Z", true, true, false),
   LEFTTRIANGLE("M 0.433 -0.5 L -0.433 0 L 0.433 0.5 Z", true, false, true),
   RIGHTTRIANGLE("M -0.433 0.5 L 0.433 0 L -0.433 -0.5 Z", true, false, true),
   UPISOTRIANGLE("M 0 0.5 L 0.25 -0.5 L -0.25 -0.5 Z", true, true, false),
   DOWNISOTRIANGLE("M 0 -0.5 L -0.25 0.5 L 0.25 0.5 Z", true, true, false),
   LEFTISOTRIANGLE("M -0.5 0 L 0.5 0.25 L 0.5 -0.25 Z", true, false, true),
   RIGHTISOTRIANGLE("M 0.5 0 L -0.5 -0.25 L -0.5 0.25 Z", true, false, true),
   UPDART("M -0.3333 -0.5 L 0 0.5 L 0.3333 -0.5 L 0 -0.1667 Z", true, true, false),
   DOWNDART("M 0.3333 0.5 L 0 -0.5 L -0.3333 0.5 L 0 0.1667 Z", true, true, false),
   LEFTDART("M 0.5 -0.3333 L -0.5 0 L 0.5 0.3333 L 0.1667 0 Z", true, false, true),
   RIGHTDART("M -0.5 0.3333 L 0.5 0 L -0.5 -0.3333 L -0.1667 0 Z", true, false, true),
   UPARROW("M 0 0.5 L 0.5 0 L 0.1667 0 L 0.1667 -0.5 L -0.1667 -0.5 L -0.1667 0 L -0.5 0 Z", true, true, false),
   DOWNARROW("M 0 -0.5 L -0.5 0 L -0.1667 0 L -0.1667 0.5 L 0.1667 0.5 L 0.1667 0 L 0.5 0 Z", true, true, false),
   LEFTARROW("M -0.5 0 L 0 0.5 L 0 0.1667 L 0.5 0.1667 L 0.5 -0.1667 L 0 -0.1667 L 0 -0.5 Z", true, false, true),
   RIGHTARROW("M 0.5 0 L 0 -0.5 L 0 -0.1667 L -0.5 -0.1667 L -0.5 0.1667 L 0 0.1667 L 0 0.5 Z", true, false, true),
   HLINETHRU("M -0.5 0 L 0.5 0", false, true, true),
   LINETHRU("M 0 -0.5 L 0 0.5", false, true, true),
   LINEUP("M 0 0 L 0 0.5", false, true, false),
   LINEDOWN("M 0 0 L 0 -0.5", false, true, false),
   BRACKET("M -0.2 0.5 L 0 0.5 L 0 -0.5 L -0.2 -0.5", false, false, true),
   ARROW("M -0.5 -0.5 L 0 0 L -0.5 0.5", false, false, true),
   FILLARROW("M -0.5 -0.5 L 0 0 L -0.5 0.5 Z", true, false, true),
   THINARROW("M -0.5 -0.25 L 0 0 L -0.5 0.25", false, false, true),
   FILLTHINARROW("M -0.5 -0.25 L 0 0 L -0.5 0.25 Z", true, false, true),
   WIDEARROW("M -0.25 -0.5 L 0 0 L -0.25 0.5", false, false, true),
   FILLWIDEARROW("M -0.25 -0.5 L 0 0 L -0.25 0.5 Z", true, false, true),
   DART("M -0.5 0.1667 L 0 0 L -0.5 -0.1667 L -0.3333 0 Z", true, false, true),
   KITE("M -0.3333 0.2 L 0 0 L -0.3333 -0.2 L -0.5 0 Z", true, false, true),
   REVERSEARROW("M 0.5 -0.5 L 0 0 L 0.5 0.5", false, false, true),
   REVERSEFILLARROW("M 0.5 -0.5 L 0 0 L 0.5 0.5 Z", true, false, true),
   REVERSETHINARROW("M 0.5 -0.25 L 0 0 L 0.5 0.25", false, false, true),
   REVERSEFILLTHINARROW("M 0.5 -0.25 L 0 0 L 0.5 0.25 Z", true, false, true),
   REVERSEWIDEARROW("M 0.25 -0.5 L 0 0 L 0.25 0.5", false, false, true),
   REVERSEFILLWIDEARROW("M 0.25 -0.5 L 0 0 L 0.25 0.5 Z", true, false, true),
   REVERSEDART("M 0.5 -0.1667 L 0 0 L 0.5 0.1667 L 0.3333 0 Z", true, false, true),
   REVERSEKITE("M 0.3333 -0.2 L 0 0 L 0.3333 0.2 L 0.5 0 Z", true, false, true);

   
   private final Shape designShape;
   private final boolean isClosed;
   private final boolean isVSymmetric;
   private final boolean isHSymmetric;

   /**
    * Construct a <code>Marker</code> that has a horizontally and vertically symmetric, closed shape. 
    * 
    * @param designShape The marker's shape, which should be closed and symmetric both horizontally and vertically.
    */
   Marker(Shape designShape)
   {
      this(designShape, true, true, true);
   }

   /**
    * Construct a <code>Marker</code>.
    * 
    * @param designShape The marker's shape.
    * @param isClosed <code>True</code> iff the marker shape is a closed path.
    * @param isVSymmetric <code>True</code> iff the marker shape is vertically symmetric.
    * @param isHSymmetric <code>True</code> iff the marker shape is horizontally symmetric.
    */
   Marker(Shape designShape, boolean isClosed, boolean isVSymmetric, boolean isHSymmetric)
   {
      assert(designShape != null);
      this.designShape = designShape;
      this.isClosed = true;
      this.isVSymmetric = isVSymmetric;
      this.isHSymmetric = isHSymmetric;
   }
 
   /**
    * Construct a <code>Marker</code> with a horizontally and vertically symmetric, closed shape defined by a path 
    * definition string conforming to the format requirements of <code>ShapePainter.parseShape()</code>. 
    * 
    * @see ShapePainter#parseShape(String)
    * @param pathStr Defines the <code>Marker</code>'s shape.
    */
   Marker(String pathStr)
   {
      this(pathStr, true, true, true);
   }

   /**
    * Construct a <code>Marker</code>, the shape of which is defined by a path definition string conforming to the 
    * format requirements of <code>ShapePainter.parseShape().</code>.
    * 
    * @see ShapePainter#parseShape(String)
    * @param pathStr Defines the <code>Marker</code>'s shape.
    * @param isClosed <code>True</code> iff the marker shape is a closed path.
    * @param isVSymmetric <code>True</code> iff the marker shape is vertically symmetric.
    * @param isHSymmetric <code>True</code> iff the marker shape is horizontally symmetric.
    */
   Marker(String pathStr, boolean isClosed, boolean isVSymmetric, boolean isHSymmetric)
   {
      this.designShape = ShapePainter.parseShape(pathStr);
      assert( this.designShape != null );

      this.isClosed = isClosed;
      this.isVSymmetric = isVSymmetric;
      this.isHSymmetric = isHSymmetric;
   }

   /**
    * Get the shape for this <code>Marker</code>.  To support scalability, the returned shape has unit size, having 
    * been designed to fit within a 1x1 "design box".  Typically the origin of the shape coordinates will lie at the 
    * center of the design box.
    * 
    * @return The design shape for this <code>Marker</code>.
    */
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

   /** 
    * Get a transformed version of this marker's design shape. The design shape is first translated, then scaled.
    * @param dx The translation in X.
    * @param dy The translation in Y.
    * @param scale The scale factor in both X and Y directions. If not strictly positive, and empty shape is returned. 
    * @return The transformed shape.
    */
   public Shape getTransformedShape(double dx, double dy, double scale)
   {
      return(getTransformedShape(dx, dy, scale, scale));
   }
   
   /** 
    * Get a transformed version of this marker's design shape. The design shape is first translated, then scaled.
    * @param dx The translation in X.
    * @param dy The translation in Y.
    * @param scaleX The scale factor in X. If not strictly positive, an empty shape is returned. 
    * @param scaleY The scale factor in Y. If not strictly positive, an empty shape is returned.
    * @return The transformed shape.
    */
   public Shape getTransformedShape(double dx, double dy, double scaleX, double scaleY)
   {
      GeneralPath gp = new GeneralPath();
      if(scaleX > 0 && scaleY > 0)
      {
         AffineTransform at = AffineTransform.getTranslateInstance(dx, dy);
         at.scale(scaleX, scaleY);
         PathIterator pi = designShape.getPathIterator(at);
         gp.setWindingRule(pi.getWindingRule());
         gp.append(pi, false);
      }
      return(gp);
   }
   
   @Override
   public String toString()
   {
      return( super.toString().toLowerCase() );
   }
}
