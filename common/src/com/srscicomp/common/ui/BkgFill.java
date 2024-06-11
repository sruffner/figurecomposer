package com.srscicomp.common.ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;

import com.srscicomp.common.util.Utilities;

/**
 * An immutable class defining the background fill for the bounding box of a graphic object. It supports three distinct
 * types of background fills -- a solid color, an axial gradient with two color stops, or a radial gradient with two 
 * color stops. For both gradient types, each RGB color component varies linearly between the two color stops.
 * 
 * <p>If the fill is simply a solid color, it may be opaque, translucent, or fully transparent. An axial gradient is 
 * defined by the two color stops and the gradient's angle of orientation WRT a graphic object's  bounding box. By 
 * design, the end points of the gradient are chosen to satisfy the angle of orientation while ensuring that one cycle 
 * of the gradient covers the entire bounding box. A radial gradient is defined by an origin at the center of the 
 * bounding box, a radius such that the resulting circle circumscribes the box, a focal point somewhere within the 
 * bounding box (typically at the center, but it doesn't have to be), the starting color at the focus, and the ending 
 * color at the circumference of the circle. Both colors for the gradient fills must be opaque.</p>
 * 
 * <p>Obviously, the gradient fills are dependent on the geometry of the graphic object's bounding box. The bounding
 * box must be supplied in order to generate the {@link Paint} object that defines the fill WRT the coordinates
 * in which that box is defined. For details, see {@link #getPaintForFill(float, float, float, float)}.</p>
 * 
 * <p>Since the class is immutable, methods are provided for creating each supported type of background fill. Also, the
 * {@link #toXML()} and {@link #fromXML(String)} methods are used to convert to/from a character string which is
 * suitable for storing the background fill as an XML property.</p>
 * 
 * @author sruffner
 */
public class BkgFill
{
   /**
    * Create a solid, single-color background fill using the color specified.
    * @param c The fill color. Must not be null. The color may have any value for the alpha component.
    * @return The solid-color background fill, or null if specified fill color is invalid.
    */
   public static BkgFill createSolidFill(Color c)
   {
      return((c!=null) ? new BkgFill(c) : null);
   }
   
   /**
    * Create an axial gradient background fill.
    * @param rot Orientation of gradient with respect to graphic object's bounding box, in degrees CCW from +x-axis.
    * @param c1 Color at start of gradient. Must be opaque. Cannot be null.
    * @param c2 Color at end of gradient. Must be opaque. Cannot be null. <i>Note that if the two color stops are the
    * same, then the fill is effectively a solid color.</i>
    * @return An axial gradient background fill.
    */
   public static BkgFill createAxialGradientFill(int rot, Color c1, Color c2)
   {
      if(c1 == null || c1.getAlpha() != 255 || c2 == null || c2.getAlpha() != 255) return(null);
      
      rot = (int) Utilities.restrictAngle(rot);
      return(new BkgFill(rot, c1, c2));
   }
   
   /** 
    * Create a radial gradient background fill.
    * @param x X-coordinate of focus, as a percentage of graphic object's bounding box width. Restricted to [0..100].
    * @param y Y-coordinate of focus, as a percentage of graphic object's bounding box height. Restricted to [0..100].
    * @param c1 Color at focus. Must be opaque. Cannot be null.
    * @param c2 Color on and beyond the circle circumscribing the graphic object's bounding box. Must be opaque. Cannot 
    * be null. <i>Note that if the two color stops are the same, then the fill is effectively a solid color.</i>
    * @return A radial gradient background fill
    */
   public static BkgFill createRadialGradientFill(int x, int y, Color c1, Color c2)
   {
      if(c1 == null || c1.getAlpha() != 255 || c2 == null || c2.getAlpha() != 255) return(null); 
      x = Utilities.rangeRestrict(0, 100, x);
      y = Utilities.rangeRestrict(0, 100, y);
      return(new BkgFill(x, y, c1, c2));
   }
   
   /**
    * Utility method generates a string representation for the specified color, which is assumed to be defined on the
    * default sRGB colorspace. If the color is opaque, it is represented as a six-digit hexadecimal string preserving
    * the R, G, and B components: "RRGGBB". If it is translucent, an 8-digit hex string is necessary to preserve the
    * alpha component: "AARRGGBB". Finally, if the color is fully transparent (alpha = 0), the string "none" is returned
    * as a special case. 
    * 
    * @param c The color.
    * @return Corresponding hexadecimal string, as described. If argument is null, returns "000000".
    */
   public static String colorToHexString(Color c)
   {
      String hex = "000000";
      if(c != null)
      {
         int alpha = c.getAlpha();
         if(alpha == 0)
            hex = "none";
         else
         {
            int argb = c.getRGB();
            int reqLen = (alpha==255) ? 6 : 8;
            int mask = (alpha==255) ? 0x00FFFFFF : 0xFFFFFFFF;
            hex = Integer.toHexString( (argb & mask) );
            int len = hex.length();
            if(len < reqLen) hex = "00000000".substring(0,reqLen-len) + hex;
         }
      }

      return(hex);
   }
   
   /**
    * Utility method generates an ARGB color from the specified string, which may take one of three forms: the value 
    * "none", which represents transparent black (a=r=g=b=0); a 6-digit hexadecimal string "RRGGBB", representing an
    * opaque RGB color; or an 8-digit hex string, "AARRGGBB", in which the color's alpha component is explicit.
    * 
    * @param hex A 6- or 8-digit hexadecimal string, or the value "none".
    * @return The corresponding color. If the argument does not match one of the valid forms, opaque black is returned.
    */
   public static Color colorFromHexString(String hex)
   {
      if("none".equals(hex)) return(new Color(0,0,0,0));
      
      // NOTE: We use Long.parseLong() to parse the string because Integer.parseInt() converts the string to a signed
      // 32-bit int and throws an NFE for any 8-digit hex number above 0x80000000.
      int argb = 0xFF000000;  // opaque black
      boolean hasAlpha = true;
      if(hex != null && (hex.length() == 6 || hex.length() == 8))
      {
         try
         {
            long argb64 = Long.parseLong("00" + hex, 16);
            argb = (int) (argb64);
            hasAlpha = (hex.length() == 8);
         }
         catch(NumberFormatException ignored) {}
      }
      return(new Color(argb, hasAlpha));
   }
   
   /**
    * A stricter version of {@link #colorFromHexString(String)} that: (1) will not accept a non-opaque color
    * specification if <i>allowAlpha==false</i>; (2) returns null if the string representation is not valid (instead of
    * opaque black). It is used to validate the hexadecimal string representation of an ARGB color.
    * 
    * @param hex The string representation of the color, as described in {@link #colorFromHexString(String)}.
    * @param allowAlpha True if an alpha component other than 255 (opaque) is allowed.
    * @return The represented color, or null if unable to convert string argument to an ARGB color.
    */
   public static Color colorFromHexStringStrict(String hex, boolean allowAlpha)
   {
      if("none".equals(hex)) return(allowAlpha ? new Color(0,0,0,0) : null);

      // NOTE: We use Long.parseLong() to parse the string because Integer.parseInt() converts the string to a signed
      // 32-bit int and throws an NFE for any 8-digit hex number above 0x80000000.
      Color c = null;
      if(hex != null && (hex.length() == 6 || hex.length() == 8))
      {
         try
         {
            long argb64 = Long.parseLong("00" + hex, 16);
            int argb = (int) (argb64);
            c = new Color(argb, (hex.length()==8));
            if(c.getAlpha() < 255 && !allowAlpha) c = null;
         }
         catch(NumberFormatException ignored) {}
      }
      return(c);
   }
   
   /**
    * Is the specified string a valid representation of a background fill as generated by {@link #toXML()}?
    * @param rep The string representation to check.
    * @return True if valid, else false.
    */
   public static boolean isValidXML(String rep)
   {
      if(rep == null) return(false);
      String[] tokens = rep.split(" ");
      boolean ok;
      if(tokens.length == 1)
      {
         ok = (BkgFill.colorFromHexStringStrict(tokens[0], true) != null);
      }
      else if(tokens.length == 3)
      {
         ok = false;
         try 
         { 
            Integer.parseInt(tokens[0]); 
            ok = (BkgFill.colorFromHexStringStrict(tokens[1], false) != null) &&
                  (BkgFill.colorFromHexStringStrict(tokens[2], false) != null);
         } 
         catch(NumberFormatException ignored) {}
      }
      else if(tokens.length == 4)
      {
         ok = false;
         try 
         { 
            Integer.parseInt(tokens[0]); 
            Integer.parseInt(tokens[1]); 
            ok = (BkgFill.colorFromHexStringStrict(tokens[2], false) != null) &&
                  (BkgFill.colorFromHexStringStrict(tokens[3], false) != null);
         } 
         catch(NumberFormatException ignored) {}
      }
      else
         ok = false;
      
      return(ok);
   }
   
   /**
    * Reconstruct a background fill from an XML attribute string, as described in {@link #toXML()}.
    * @param s The XML representation of a background fill.
    * @return The background fill object. <b>If XML string value is invalid, returns a solid black fill.</b>
    */
   public static BkgFill fromXML(String s)
   {
      BkgFill bf = null;
      String[] tokens = (s == null) ? new String[0] : s.split(" ");
      if(tokens.length == 1)
         bf = new BkgFill(BkgFill.colorFromHexString(tokens[0]));
      else if(tokens.length == 3)
      {
         boolean ok = true;
         int rot = 0;
         try { rot = (int) Utilities.restrictAngle(Integer.parseInt(tokens[0])); } 
         catch(NumberFormatException nfe) { ok = false; }

         Color c1 = BkgFill.colorFromHexStringStrict(tokens[1], false);
         Color c2 = BkgFill.colorFromHexStringStrict(tokens[2], false);
         
         if(ok && c1 != null && c2 != null) bf = new BkgFill(rot, c1, c2);
      }
      else if(tokens.length == 4)
      {
         boolean ok = true;
         int x = 0;
         int y = 0;
         try 
         { 
            x = Utilities.rangeRestrict(0, 100, Integer.parseInt(tokens[0])); 
            y = Utilities.rangeRestrict(0, 100, Integer.parseInt(tokens[1])); 
         } 
         catch(NumberFormatException nfe) { ok = false; }
         
         Color c1 = BkgFill.colorFromHexStringStrict(tokens[2], false);
         Color c2 = BkgFill.colorFromHexStringStrict(tokens[3], false);
         
         if(ok && c1 != null && c2 != null) bf = new BkgFill(x, y, c1, c2);
      }
      
      return(bf != null ? bf : new BkgFill(Color.BLACK));
   }
   
   /**
    * The string representation of the background fill for storing as an attribute of an XML element. Its
    * form depends on the type of background fill:
    * <ul>
    * <li>Solid color: A 6-digit hexadecimal string representing an opaque RGB color ("RRGGBB"), or an 8-digit hex
    * string for an ARGB color "AARRGGBB". However, if the fill color is fully transparent, the string token "none" is 
    * used.</li>
    * <li>Axial gradient: Three white space-separated tokens, "rot c1 c2", where "rot" is an integer string indicating
    * the gradient's orientation angle with respect to the bounding box in degrees, and "c1" and "c2" are the gradient's
    * <b>opaque</b> color stops, encoded again as six-digit hex strings: "90 ff0000 00ff00".</li>
    * <li>Radial gradient: Four white space-separated tokens, "x y c1 c2", where "x" and "y" are integers specifying the
    * x- and y-coordinate of the gradient focal point in the graphic object's bounding box (as a percentage of the box's 
    * width and height), while "c1" and "c2" are the gradient's <b>opaque</b> color stops: "50 50 ff0000 ffff00".</li>
    * </ul>
    * @return The string representation of the background fill, for use as an XML attribute value.
    */
   public String toXML()
   {
      String out;
      switch(fillType)
      {
      case SOLID:
         out = BkgFill.colorToHexString(c1);
         break;
      case AXIAL:
         out = String.format("%d %s %s", rotDeg, BkgFill.colorToHexString(c1), BkgFill.colorToHexString(c2));
         break;
      case RADIAL:
      default:
         out = String.format("%d %d %s %s", focusX, focusY, BkgFill.colorToHexString(c1), BkgFill.colorToHexString(c2));
         break;
      }
      return(out);
   }

   @Override public boolean equals(Object obj)
   {
      if(obj == null || !obj.getClass().equals(BkgFill.class)) return(false);
      BkgFill other = (BkgFill) obj;
      if(other == this) return(true);
      return(other.fillType==fillType && other.c1==c1 && other.c2==c2 && 
            other.rotDeg==rotDeg && other.focusX==focusX && other.focusY==focusY);
   }

   @Override public int hashCode()
   {
      int res = fillType.ordinal();
      res = res*31 + c1.hashCode();
      res = res*31 + c2.hashCode();
      res = res*31 + rotDeg;
      res = res*31 + focusX;
      res = res*31 + focusY;
      return(res);
   }

   /**
    * Get a human-readable description of this background fill (suitable for use as a tooltip, for example).
    * @return The description string.
    */
   public String getDescription()
   {
      String out;
      switch(fillType)
      {
      case SOLID:
         out = String.format("Solid [%s]", RGBColorPicker.getColorString(c1));
         break;
      case AXIAL:
         out = String.format("Axial [%d deg; %s to %s]", rotDeg,  RGBColorPicker.getColorString(c1), 
               RGBColorPicker.getColorString(c2));
         break;
      case RADIAL:
      default:
         out = String.format("Radial [focus at (%d,%d) %%; %s to %s]", focusX, focusY, 
               RGBColorPicker.getColorString(c1), RGBColorPicker.getColorString(c2));
         break;
      }
      return(out);
   }
   
   /** Type of background fill. */
   public enum Type 
   {
      /** A solid single-color fill. */ SOLID,
      /** An oriented axial gradient with two color stops, covering the graphic object's bounding box. */ AXIAL,
      /** A radial gradient with two color stops, covering the graphic object's bounding box. */ RADIAL;

      @Override public String toString() { return(super.toString().toLowerCase()); }
   }
   
   /** 
    * Get the background fill type. 
    * @return The fill type.
    */
   public Type getFillType() { return(fillType); }
   /**
    * Get the color for a solid-color background fill, or the first color for a gradient fill. For a radial gradient, 
    * this is the color at the origin.
    * @return The color.
    */
   public Color getColor1() { return(c1); }
   /**
    * Get the second color for a gradient fill. For a radial gradient, this is the color at the circumference of the 
    * circle bounding the gradient.
    * @return The color. For a solid fill, this returns the same value as {@link #getColor1()}.
    */
   public Color getColor2() { return(c2); }
   /**
    * Get the orientation angle for an axial gradient with respect to graphic object's bounding box.
    * @return Angle in degrees; 0 if this is not an axial gradient fill.
    */
   public int getOrientation() { return(rotDeg); }
   /**
    * Get the X-coordinate for the radial gradient's focal point with respect to graphic object's bounding box.
    * @return X-coordinate as a percentage of the bounding box width; 0 if this is not a radial gradient fill.
    */
   public int getFocusX() { return(focusX); }
   /**
    * Get the Y-coordinate of the radial gradient's focal point with respect to graphic object's bounding box.
    * @return Y-coordinate as a percentage of the bounding box height; 0 if this is not a radial gradient fill.
    */
   public int getFocusY() { return(focusY); }
   
   /** 
    * Is this a fully transparent background fill? This will be the case only when the fill type is {@link Type#SOLID} 
    * and the fill color has an alpha of zero.
    * @return True if background fill is transparent.
    */
   public boolean isTransparent() { return(fillType==Type.SOLID && c1.getAlpha() == 0); }
   
   /**
    * Get the paint object defined by this background fill for a graphic object with the specified bounding box
    * dimensions. For a solid fill, the dimensions are ignored and a {@link Color} is returned. For the gradient fills,
    * an appropriate gradient paint object is returned such that a single cycle of the gradient spans the specified 
    * bounding box dimensions. The method assumes a coordinate system in which the origin (0,0) lies at the bounding 
    * box's bottom left corner, and the top-right corner is at (w,h).
    * <p>The endpoints of the axial gradient depend on the bounding box and the gradient's orientation with respect 
    * to that box. For any orientation other than 0,90,180,270, the endpoints actually lie outside the box so that a
    * single cycle of the gradient covers the bounding box. Draw a diagram and do the trigonometry!</p>
    * <p>The radial gradient is defined by a circle centered on and circumscribing the bounding box. Thus, its radius
    * must be sqrt(w*w  + h*h). The first color is at the focal point ({@link #getFocusX()}, {@link #getFocusY}), while
    * the second color is mapped to the perimeter of the circle.</p>
    * 
    * @param w Width of graphic object's bounding box.
    * @param h Height of graphic object's bounding box.
    * @return The paint object, as described. Note that, for a gradient fill, if the bounding box dimensions are not
    * both strictly positive, the gradient is ill-defined, and this method will simply return a {@link Color} 
    * representing the first color stop.
    */
   public Paint getPaintForFill(float w, float h)
   {
      Paint p;
      if(fillType == Type.SOLID || w <= 0 || h <= 0)
         p = c1;
      else if(fillType == Type.AXIAL)
      {
         Point2D[] pts = getAxialEndpoints(w, h);
         p = new GradientPaint(pts[0], c1, pts[1], c2, false);
      }
      else // Type.RADIAL
      {
         p = new RadialGradientPaint(w/2.0f, h/2.0f, (float) (Math.sqrt(w*w + h*h) / 2.0), focusX*w/100.0f, 
               focusY*h/100.0f, new float[] {0f , 1f}, new Color[] {c1, c2}, CycleMethod.NO_CYCLE);
      }
      
      return(p);
   }
   
   /**
    * Get the paint object defined by this background fill for a graphic object with the specified bounding box
    * dimensions. For a solid fill, the dimensions are ignored and a {@link Color} is returned. For the gradient fills,
    * an appropriate gradient paint object is returned such that a single cycle of the gradient spans the specified 
    * bounding box dimensions. The method assumes a coordinate system in which the point (x0, y0) lies at the bounding 
    * box's bottom left corner, and the top-right corner is at (x0+w, y0+h).
    * <p>The endpoints of the axial gradient depend on the bounding box and the gradient's orientation with respect 
    * to that box. For any orientation other than 0,90,180,270, the endpoints actually lie outside the box so that a
    * single cycle of the gradient covers the bounding box. Draw a diagram and do the trigonometry!</p>
    * <p>The radial gradient is defined by a circle centered on and circumscribing the bounding box. Thus, its radius
    * must be sqrt(w*w  + h*h). The first color is at the focal point ({@link #getFocusX()}, {@link #getFocusY}), while
    * the second color is mapped to the perimeter of the circle.</p>
    * 
    * @param w Width of graphic object's bounding box.
    * @param h Height of graphic object's bounding box.
    * @param x0 X-coordinate of bounding box's BL corner in the coordinate system in which object will be painted.
    * @param y0 Y-coordinate of bounding box's BL corner in the coordinate system in which object will be painted.
    * @return The paint object, as described. Note that, for a gradient fill, if the bounding box dimensions are not
    * both strictly positive, the gradient is ill-defined, and this method will simply return a {@link Color} 
    * representing the first color stop.
    */
   public Paint getPaintForFill(float w, float h, float x0, float y0)
   {
      if(x0==0f && y0==0f) return(getPaintForFill(w,h));
      Paint p;
      if(fillType == Type.SOLID || w <= 0 || h <= 0)
         p = c1;
      else if(fillType == Type.AXIAL)
      {
         Point2D[] pts = getAxialEndpoints(w, h);
         for (Point2D pt : pts) pt.setLocation(pt.getX() + x0, pt.getY() + y0);
         p = new GradientPaint(pts[0], c1, pts[1], c2, false);
      }
      else // Type.RADIAL
      {
         p = new RadialGradientPaint(w/2.0f + x0, h/2.0f + y0, (float) (Math.sqrt(w*w + h*h) / 2.0), 
               focusX*w/100.0f + x0, focusY*h/100.0f + y0, new float[] {0f , 1f}, new Color[] {c1, c2}, 
               CycleMethod.NO_CYCLE);
      }
      
      return(p);
   }
   
   /**
    * Compute the end points of this axial gradient such that a single cycle spans a bounding box with the dimensions
    * specified. For any orientation other than 0,90,180,270, the endpoints actually lie outside the box. The method 
    * assumes a coordinate system in which the origin (0,0) lies at the bounding box's bottom left corner, and the 
    * top-right corner is at (w,h).
    * @param w Width of graphic object's bounding box.
    * @param h Height of graphic object's bounding box.
    * @return A 2-element array holding the two endpoints. Will be null if this is NOT an axial gradient fill.
    */
   public Point2D[] getAxialEndpoints(double w, double h)
   {
      Point2D[] out = null;
      if(fillType == Type.AXIAL && w > 0 && h > 0)
      {
         // the half-diagonal, the angle made by the diagonal, and the gradient orientation in radians
         double d = Math.sqrt(w*w + h*h) / 2.0;
         double alpha = Math.atan2(h, w);
         double theta = Math.PI * rotDeg / 180.0;
         
         // compute deltas from center of b-box to the gradient endpoints (project diagonal onto line || to gradient).
         // draw pictures to figure this out!!!
         if(rotDeg <= 90) d *= Math.cos(alpha-theta);
         else if(rotDeg <= 180) d *= -Math.cos(alpha+theta);
         else if(rotDeg <= 270) d *= -Math.cos(alpha-theta);
         else d *= Math.cos(alpha+theta);
         
         double dx = d*Math.cos(theta);
         double dy = d*Math.sin(theta);
         out = new Point2D[] {new Point2D.Double(w/2.0 - dx, h/2.0 - dy), new Point2D.Double(w/2.0 + dx, h/2.0 + dy)};
      }
      return(out);
   }
   
   private BkgFill(Color c) 
   {
      fillType = Type.SOLID;
      c1 = c;
      c2 = c;
      rotDeg = focusX = focusY = 0;
   }
   
   private BkgFill(int rot, Color start, Color end)
   {
      fillType = Type.AXIAL;
      c1 = start;
      c2 = end;
      rotDeg = rot;
      focusX = focusY = 0;
   }
   
   private BkgFill(int x, int y, Color start, Color end)
   {
      fillType = Type.RADIAL;
      c1 = start;
      c2 = end;
      focusX = x;
      focusY = y;
      rotDeg = 0;
   }
   
   /** The fill type. */
   private final Type fillType;
   /** The assigned color for a solid-color fill, or the first color stop for a gradient fill. */
   private final Color c1;
   /** The assigned color for a solid-color fill, or the second color stop for a gradient fill. */
   private final Color c2;
   /** The orientation angle for an axial gradient fill, in degrees [0..359]. */
   private final int rotDeg;
   /** X-coordinate of focal point for a radial gradient, as a percentage of bounding box width [0..100]. */
   private final int focusX;
   /** Y-coordinate of focal point for a radial gradient, as a percentage of bounding box height [0..100]. */
   private final int focusY;
}
