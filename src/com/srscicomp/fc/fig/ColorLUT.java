/**
 * 
 */
package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.LinearGradientPaint;

import com.srscicomp.common.util.Utilities;

/**
 * The color lookup table (LUT) associated with a specific color map in <i>Figure Composer</i>.
 * 
 * <p>All FC color maps are represented by a {@link ColorMap} instance, an immutable object encapsulating the "key
 * frames" that define the color map. Two 256-entry color lookup tables are associated with each unique color map: one
 * running in the "forward" direction and one in the "reverse" direction. {@link ColorLUT} represents a single LUT
 * instance -- it is defined by a {@link ColorMap} instance and a flag selecting the forward or reverse direction.
 * With it you can:
 * <ul>
 * <li>Map an arbitrary floating-point value within a specified range to an integer in [0..255] and thence to the 
 * corresponding {@link Color} in the LUT.</li>
 * <li>Generate a copy of the LUT as an integer array, where the RGB colors are represented in the packed integer 
 * format with an alpha component of 255 (opaque).</li>
 * <li>Generate a linear gradient paint that can be used to render a visual representation of the color LUT.</p>
 * </ul>
 * <?p>
 *
 * @author sruffner
 */
public class ColorLUT
{
   /** Construct an instance of the built-in, gray scale (black to white) color map. */
   public ColorLUT() { this(null, false); }
   
   /** 
    * Construct the color lookup table for the specified color map and direction.
    * 
    * @param cmap The color map. If null, the built-in gray scale color map is assumed.
    * @param reversed If true, the lookup table is reversed. For example, the built-in gray scale map normally runs
    * from black at index 0 to white at index 255, while the reversed version runs from white at 0 to black at 255.
    */
   public ColorLUT(ColorMap cmap, boolean reversed)
   {
      this.colorMap = (cmap == null) ? ColorMap.BuiltIn.gray.cm : cmap;
      this.reversed = reversed;
   }
   
   /** The color map. */
   private final ColorMap colorMap;
   /** If true, color map's lookup table is flipped, ie, accessed in the reverse direction (index i --> 256-i-1). */
   private final boolean reversed;
   
   /** The color map for this color lookup table. */
   public ColorMap getColorMap() { return(colorMap); }
   
   /**
    * Is this color lookup table configured in the reverse direction?
    * @return True if the lookup table is reversed in direction.
    */
   public boolean isReversed() { return(reversed); }
   
   /**
    * Get an entry in this color lookup table.
    * @param idx Index of desired entry. Range-restricted to [0.255].
    * @return An integer specifying the color in packed RGB form: 0xFFRRGGBB, where each component lies in [0..0xFF]. 
    * Note that the alpha component in bits 24-32 is 0xFF (255); all LUT colors are opaque.
    */
   public int getRGB(int idx) { return(colorMap.getRGB(idx, reversed)); }
   
   /**
    * Get a copy of the 256-entry lookup table for this {@link ColorLUT}. If the LUT is configured for the reverse
    * direction, the contents of the returned array reflect this.
    * 
    * @return Integer array where each element specifies a color in packed opaque RGB form: 0xFFRRGGBB.
    */
   public int[] getLUT() { return(colorMap.getLUT(reversed)); }
   
   /**
    * Map an arbitrary value to an index in this color map's lookup table. 
    * @param x The value.
    * @param xMin Minimum value. Lesser values are mapped to index 0. Must be strictly less than maximum.
    * @param xMax Maximum value. Greater values are mapped to the last index.
    * @param isLog True for a logarithmic mapping, false for linear.
    * @return The corresponding color map index.
    * @throws IllegalArgumentException if the value range is invalid.
    */
   public int mapValueToIndex(double x, double xMin, double xMax, boolean isLog)
   {
      if(xMin >= xMax) throw new IllegalArgumentException("Invalid value range");
      double xRng = xMax-xMin;
      int n = ColorMap.LUTSIZE;
      if(isLog) xRng = Math.log10(xRng + 1);
      x = isLog ? (Math.log10(x-xMin+1) * n / xRng) : ((x-xMin) * n / xRng);
      return(Utilities.rangeRestrict(0, n-1, ((int) (x + 0.5))));
   }
   
   /**
    * Map an arbitrary value to an RGB entry in this color map's lookup table.
    * @param x The value.
    * @param xMin Minimum value. Lesser values are mapped to index 0. Must be strictly less than maximum.
    * @param xMax Maximum value. Greater values are mapped to the last index.
    * @param isLog True for a logarithmic mapping, false for linear.
    * @return An integer specifying the color as a packed RGB integer: 0x00RRGGBB.
    * @throws IllegalArgumentException if the value range is invalid.
    */
   public int mapValueToRGB(double x, double xMin, double xMax, boolean isLog)
   {
      return(getRGB(mapValueToIndex(x, xMin, xMax, isLog)));
   }
   
   /**
    * Map an arbitrary value to an RGB color in this color lookup table.
    * @param x The value.
    * @param xMin Minimum value. Lesser values are mapped to index 0. Must be strictly less than maximum.
    * @param xMax Maximum value. Greater values are mapped to the last index.
    * @param isLog True for a logarithmic mapping, false for linear.
    * @return The mapped color.
    * @throws IllegalArgumentException if the value range is invalid.
    */
   public Color mapValueToRGBColor(double x, double xMin, double xMax, boolean isLog)
   {
      return(new Color(mapValueToRGB(x, xMin, xMax, isLog)));
   }
   
   /**
    * Get this color lookup table's linear gradient paint for filling a shape along the line segment specified.
    * 
    * @param startX X-coordinate of line segment endpoint mapping to the first color in the LUT.
    * @param startY Y-coordinate of line segment endpoint mapping to the first color in the LUT.
    * @param endX X-coordinate of line segment endpoint mapping to the last color in the LUT.
    * @param endY Y-coordinate of line segment endpoint mapping to the last color in the LUT.
    * @return A linear gradient fill pattern along the line segment specified IAW this color lookup table's definition.
    * Regions beyond the endpoints map to the terminal colors of the lookup table (the pattern does not repeat).
    */
   public LinearGradientPaint getGradientPaint(float startX, float startY, float endX, float endY)
   {
      return(new LinearGradientPaint(startX, startY, endX, endY, getKeyFrames(), getKeyColors()));
   }

   /**
    * Get the key frame fractions which define this color lookup table (via piecewise-linear interpolation of color 
    * components between key frames) -- suitable for defining a linear color gradient representing the map.
    * @return An array [0 k1 k2 .. kN 1], where 0 < k1 < k2 < ... < kN < 1 specifying the key frames defining this color
    * lookup table.
    */
   public float[] getKeyFrames() { return(colorMap.getKeyFrames(reversed)); }
   
   /**
    * Get the key colors which define this color lookup table (via piecewise-linear interpolation of color components
    * between key frames) -- suitable for defining a linear color gradient representing the map.
    * @return Array of key colors. 
    * @see #getKeyFrames
    */
   public Color[] getKeyColors() { return(colorMap.getKeyColors(reversed)); }
   
   /**
    * Return a color lookup table that most closely resembles the arbitrary RGB lookup table specified.
    * 
    * <p>This method is used when converting a <i>Matlab</i> figure to <i>FypML</i>. It compares the specified LUT 
    * against the reverse- and forward-direction LUTs of each built-in color map available in <i>Figure Composer</i> and
    * chooses the built-in LUT that is "nearest" to the LUT specified (smallest accumulated "distance" between 
    * corresponding LUT colors in RGB space).</p>
    * 
    * @param cm An Nx3 lookup table where each 3-element row represents an RGB color with normalized components [r g b]
    * all lying in [0..1]. If null, or if any row is not a normalized RGB triplet, then this method will return the
    * built-in gray scale (black to white) LUT.
    * @return The best match color lookup table built into FC.
    */
   public static ColorLUT selectBestMatchFor(double[][] cm)
   {
      if(cm == null || cm.length == 0) return(new ColorLUT());
      
      // convert to packed RGB format -- and protect against bad input.
      int[] rgbLUT = new int[cm.length];
      try 
      {
         for(int i=0; i<cm.length; i++)
         {
            for(int j=0; j<3; j++) if(cm[i][j] < 0 || cm[i][j] > 1) 
               throw new IllegalArgumentException();
            int r = Utilities.rangeRestrict(0, 255, (int) (256 * cm[i][0]));
            int g = Utilities.rangeRestrict(0, 255, (int) (256 * cm[i][1]));
            int b = Utilities.rangeRestrict(0, 255, (int) (256 * cm[i][2]));
            rgbLUT[i] = 0xFF000000 | ((r<<16) + (g<<8) + b);
         }

      }
      catch(Exception e) { return(new ColorLUT()); }
      
      ColorLUT best = null;
      long minDst = Long.MAX_VALUE;
      for(ColorMap.BuiltIn builtin : ColorMap.BuiltIn.values())
      {
         long dst = 0;
         ColorLUT lut = new ColorLUT(builtin.cm, false);
         for(int i=0; i < rgbLUT.length; i++)
         {
            int j = (i * ColorMap.LUTSIZE) / rgbLUT.length;
            dst += distance(lut.getRGB(j), rgbLUT[i]);
         }
         
         if(dst < minDst)
         {
            minDst = dst;
            best = lut;
         }
         
         lut = new ColorLUT(builtin.cm, true);
         for(int i=0; i < rgbLUT.length; i++)
         {
            int j = (i * ColorMap.LUTSIZE) / rgbLUT.length;
            dst += distance(lut.getRGB(j), rgbLUT[i]);
         }
         
         if(dst < minDst)
         {
            minDst = dst;
            best = lut;
         }
      }
      
      return(best);
   }
   
   /** 
    * Compute the somewhat contrived "distance" between two packed RGB triplets (0xRRGGBB) as the Euclidean distance
    * between (r1, g1, b1) and (r2, g2, b2) in Cartesian RGB space. Note that the alpha component is omitted in the
    * calculation; also, this measure could be very different from the "perceived distance" between two colors.
    * @param rgb1 A packed RGB triplet.
    * @param rgb2 Another packed RGB triplet.
    * @return The "distance" between the two colors, as described.
    */
   private static int distance(int rgb1, int rgb2)
   {
      double diff = Math.pow((0x0FF & rgb2) - (0x0FF & rgb1), 2);
      rgb1 >>= 8;
      rgb2 >>= 8;
      diff += Math.pow((0x0FF & rgb2) - (0x0FF & rgb1), 2);
      rgb1 >>= 8;
      rgb2 >>= 8;
      diff += Math.pow((0x0FF & rgb2) - (0x0FF & rgb1), 2);
      return((int) Math.sqrt(diff));
   }
}
