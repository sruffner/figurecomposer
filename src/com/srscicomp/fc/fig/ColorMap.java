package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.util.Arrays;
import java.util.regex.Pattern;

import com.srscicomp.common.util.NeverOccursException;
import com.srscicomp.common.util.Utilities;


/**
 * A color map.
 * 
 * <p><i>Background</i>. In FC, 2D and 3D graphs include a {@link ColorBarNode} component that defines the color mapping
 * properties for the graph. Certain data presentation elements (contours, surfaces, scatter plots) use the color map to
 * map Z-coordinate data to a color. Originally, there were a few predefined, built-in color maps. Now FC supports 
 * custom, user-defined color maps in addition to 14 built-in ones.</p>
 * 
 * <p>Since a <i>FypML</i> figure is often created by preparing it in <i>Matlab</i> first, then importing the FIG file 
 * into <b>Figure Composer</b>, the built-in colormaps approximate some of the popular color maps in <i>Matlab</i>. Most
 * have the same name as the corresponding <i>Matlab</i> color map, with some exceptions. Other, more recently added
 * built-ins emulate the set of "perceptually uniform sequential" color maps defined in the <i>Matplotlib</i> graphing
 * library for Python.</p>
 * 
 * <p>All color maps in FC are defined by a list of 2 or more "key frames". A key frame is an index K between 0 and 255
 * and a corresponding opaque RGB color. Up to 10 such key frames may be specified, where the first key frame is at
 * index 0, the last at index 255, and the intervening frame indices must satisfy 0 < K1 < K2 < ... < Ki < 255. Two 
 * color maps are considered equal if their key frames are identical.</p>
 * 
 * <p>The companion class {@link ColorLUT} implements the color lookup table (LUT) functionality for a {@link 
 * ColorMap}. All color map LUTs have a set length of 256 entries. The LUT is calculated from the map's key frames via
 * piecewise-linear interpolation of the R/G/B components between adjacent key frames. The LUT may be accessed in either
 * the forward or reverse direction -- so you essentially get two LUTs for each unique color map.</p>
 * 
 * <p>{@link ColorMap}s are immutable. To create a new map, use {@link #createColorMap()}. You can also derive another
 * map from an existing one -- see, for example, {@link #deriveColorMap(int)}. To access any built-in color map, use the
 * enumeration {@link BuiltIn}.</p>
 * 
 * @author sruffner
 */
public class ColorMap
{
   /* The fixed size of the color lookup table for any color map. */
   public final static int LUTSIZE = 256;
   
   /** The maximum number of distinct key frames defining a custom color map. */
   public final static int MAXKEYFRAMES = 10;
   
   /**
    * Create a new color map.
    * 
    * <p>The map's 256-entry lookup table is defined by an ordered list of key frames. One "key frame" is defined by
    * an index K into the LUT and the opaque RGB color at that index. These are packed into a 32-bit integer as
    * 0xNNRRGGBB, where the upper 8 bits encode the index K and the remaining 24 bits encode the RGB color. At least two
    * and up to {@link #MAXKEYFRAMES} key frames may be specified, and the key frames must be listed in ascending order 
    * by index. The map's complete LUT is calculated from these key frame by piecewise-linear interpolation of the
    * R/G/B components between adjacent key frames.</p>
    *  
    * @param name The map name. Cannot match the name of any of the built-in color maps. Cannot be null or empty, and
    * restricted to the character set [a-zA-Z_0-9].
    * @param keyframes The map's key frames in packed-integer format, as described. Cannot duplicate the key frame
    * definition of any of the built-in color maps.
    * @return The color map, or null if the key frame definition is invalid or if it conflicts with a built-in map.
    */
   public static ColorMap createColorMap(String name, int... keyframes)
   {
      ColorMap cm = null;
      try 
      { 
         cm = new ColorMap(name, keyframes, true); 
         if(ColorMap.duplicatesBuiltin(cm)) cm = null;
      } catch(IllegalArgumentException iae) {}
      return(cm);
   }
   
   /**
    * Encode a color map as a plain text string in the form "name[F1 F2 ... FN]", where "name" is the map name and Fn
    * is the n-th key frame as an 8-digit hexadecimal string, using {@link Integer#toHexString(int)}. However, for the
    * built-in color maps, only the map name suffices, since it is forbidden to apply a built-in map name to any 
    * custom color map.
    * 
    * @param cm A color map
    * @return The color map definition encoded in string form, as described.
    */
   public static String asString(ColorMap cm)
   {
      if(!cm.isCustom()) return(cm.getName());
      
      StringBuilder sb = new StringBuilder();
      sb.append(cm.name).append("[");
      for(int kf : cm.keyFrames) sb.append(Integer.toHexString(kf)).append(" ");
      sb.setCharAt(sb.length()-1, ']');  // replace trailing space with closing bracket!
      return(sb.toString());
   }
   
   /**
    * Get the {@link ColorMap} previously encoded in string form.
    * 
    * @param s String encoding a color map as described in {@link #asString()}.
    * @return The decoded color map, or null if <i>s</i> does not encode a color map.
    */
   public static ColorMap fromString(String s)
   {
      if(s == null || s.isEmpty()) return(null);
      int idx = s.indexOf('[');
      if(idx < 0)
      {
         BuiltIn builtIn = Utilities.getEnumValueFromString(s, BuiltIn.values());
         return(builtIn != null ? builtIn.cm : null);
      }
      
      try
      {
         String name = s.substring(0, idx);
         String[] sFrames = s.substring(idx+1, s.length()-1).split(" ");
         if(sFrames.length < 2 || sFrames.length > MAXKEYFRAMES) return(null);
         int[] frames = new int[sFrames.length];
         for(int i=0; i<frames.length; i++) frames[i] = Integer.parseUnsignedInt(sFrames[i], 16);
         return(createColorMap(name, frames));
      }
      catch(Exception e) {}
      
      return(null);
   }
   
   /**
    * Parses the contents of the system clip board as the key frame definition of a color map. The clip board content 
    * must meet the following requirements:
    * <ul>
    * <li>Must contain at least two and at most {@link #MAXKEYFRAMES} lines. Each line defines one key frame.</li>
    * <li>All lines must contain 3 numeric tokens, or all must contain 4. In the former case, the tokens are parsed
    * as the R,G and B color components of the key frame color, and the corresponding LUT index is computed so that
    * the key frames are evenly distributed over [0..255]. In the latter case, the first token is the LUT index and the
    * remaining 3 are the R, G and B color components.</li>
    * <li>If the frame indices are explicit, they must be in ascending order. Note that the first and last are ignored. 
    * These are always set to 0 and 255, respectively.</li>
    * <li>All numeric tokens must either be integers in [0..255] or floating-point numbers in [0..1]. Floating-point
    * values map to integers in [0..255].</li>
    * <li>The following characters may be present in the clip board content, since they are stripped before the content
    * is split into lines and parsed: ',', ')', '(', '[', ']', '{', '}'.</li>
    * </ul>
    * 
    * @return The color map parsed from the system clip's contents, or null if the content does not satisfy the
    * requirements described above. The map name is set to "fromClip".
    */
   public static ColorMap fromClipboardContents()
   {
      Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
      ColorMap cmap = null;
      try
      {
         if(clip.isDataFlavorAvailable(DataFlavor.stringFlavor))
         {
            String s = (String) clip.getData(DataFlavor.stringFlavor);
            s = s.replaceAll("[\\[\\](){},]", "");
            String[] lines = s.split("\n");
            if(lines.length < 2 || lines.length > ColorMap.MAXKEYFRAMES)
               throw new Exception("Number of lines in clip invalid: " + lines.length);
            
            int nPerLine = 0;
            String[] line1 = lines[0].trim().split("\\s+");
            if(line1.length != 3 && line1.length != 4)
               throw new Exception("Invalid number of tokens in first line: " + line1.length);
            nPerLine = line1.length;
            boolean isFloat = false;
            try { Integer.parseInt(line1[0]); } 
            catch(NumberFormatException nfe)
            {
               try { Float.parseFloat(line1[0]); isFloat = true; }
               catch(NumberFormatException nfe2) { throw new Exception("Invalid token: " + line1[0]); }
            }
            
            int[] keyframes = new int[lines.length];
            int idx = 0;
            int[] rgb = new int[3];
            for(int i=0; i<lines.length; i++)
            {
               String[] tokens = lines[i].trim().split("\\s+");
               if(tokens.length != nPerLine)
                  throw new Exception("All lines must have " + nPerLine + " numeric tokens");

               if(nPerLine == 3)
                  idx = Utilities.rangeRestrict(0, 255, i*255/(lines.length-1));
               else if((i == 0) || (i == lines.length - 1))
                  idx = (i==0) ? 0 : 255;
               else
               {
                  if(isFloat) idx = (int) (255.0f * Float.parseFloat(tokens[0]));
                  else idx = Integer.parseInt(tokens[0]);
                  if(idx < 0 || idx > 255)
                     throw new Exception("Invalid key frame index on line " + i + ": " + idx);
                }
               
               int start = nPerLine == 3 ? 0 : 1;
               for(int j=0; j<3; j++)
               {
                  if(isFloat) rgb[j] = (int) (255.0f * Float.parseFloat(tokens[start+j]));
                  else rgb[j] = Integer.parseInt(tokens[start+j]);
                  if(rgb[j] < 0 || rgb[j] > 255)
                     throw new Exception("Invalid color component on line " + i + ": " + rgb[j]);
               }
               
               keyframes[i] = (idx << 24) | (rgb[0] << 16) | (rgb[1] << 8) | rgb[2];
            }

            cmap = ColorMap.createColorMap("fromClip", keyframes);
         }
      }
      catch(Exception e) 
      {
         System.out.println("Got exception: " + e);
      }
      return(cmap);
   }
   
   /** 
    * Get the user-facing name for this color map.
    * @return Color map's name.
    */
   public String getName() { return(name); }
   
   /** Returns the map name ({@link #getName()}). For built-ins the map name is followed by "[builtin]". */
   @Override public String toString() { return(name + (custom ? "" : " [builtin]")); }
   
   /** 
    * Two color maps are considered "equal" if they have identical key frames. The map name is omitted from the test
    * for equality.
    */
   @Override public boolean equals(Object o)
   {
      return((o != null) && o.getClass().equals(ColorMap.class) && Arrays.equals(keyFrames, ((ColorMap)o).keyFrames));
   }

   /** To be consistent with {@link #equals(Object)}, excludes the map name from the hash code computation. */
   @Override public int hashCode()
   {
      int res = keyFrames.length;
      for(int i=0; i<keyFrames.length; i++) res = res*31 + keyFrames[i];
      return(res);
   }


   /**
    * Is this a custom, user-defined color map?
    * 
    * @return True if color map is user-defined; otherwise, it is one of the predefined maps built into the application.
    */
   public boolean isCustom() { return(custom); }
   

   /**
    * Get an entry in this color map's 256-entry lookup table.
    * 
    * @param idx Index of desired color. Range-restricted to [0..255].
    * @param reverse If true, access the LUT in reverse order: idx --> 255 - idx.
    * @return An integer specifying the color in packed RGB form: 0xFFRRGGBB, where each component lies in [0..0xFF]. 
    * Note that the alpha component in bits 24-32 is 0xFF (255); all LUT colors are opaque.
    */
   public int getRGB(int idx, boolean reverse) 
   { 
      idx = Utilities.rangeRestrict(idx, 0, LUTSIZE-1);
      if(reverse) idx = LUTSIZE-1-idx;
      for(int i=1; i<keyFrames.length; i++) if(idx <= getKeyFrameIndex(i))
      {
         int start = getKeyFrameIndex(i-1);
         int end = getKeyFrameIndex(i);
         Color c0 = getKeyFrameColor(i-1);
         Color c1 = getKeyFrameColor(i);
         
         int r = (int)Math.round(c0.getRed() + (idx-start)*(c1.getRed()-c0.getRed())/((float) end-start));
         int g = (int)Math.round(c0.getGreen() + (idx-start)*(c1.getGreen()-c0.getGreen())/((float) end-start));
         int b = (int)Math.round(c0.getBlue() + (idx-start)*(c1.getBlue()-c0.getBlue())/((float) end-start));
         
         return(0xFF000000 | ((r<<16) + (g<<8) + b));
      }
      throw new NeverOccursException("Not reachable");
   }
   
   /**
    * Return a copy of the color lookup table representing this color map.
    * 
    * @param reverse If true, the returned copy is the "reverse-direction" version of the map's LUT.
    * @return Array of length {@link LUTSIZE}; each element is an opaque RGB color packed into an int: 0xFFRRGGBB.
    */
   public int[] getLUT(boolean reverse)
   {
      int[] lut = new int[256];
      for(int i=0; i<keyFrames.length-1; i++)
      {
         int start = getKeyFrameIndex(i);
         int end = getKeyFrameIndex(i+1);
         Color c0 = getKeyFrameColor(i);
         Color c1 = getKeyFrameColor(i+1);
         for(int j=start; j<=end; j++)
         {
            int r = (int)Math.round(c0.getRed() + (j-start)*(c1.getRed()-c0.getRed())/((float) end-start));
            int g = (int)Math.round(c0.getGreen() + (j-start)*(c1.getGreen()-c0.getGreen())/((float) end-start));
            int b = (int)Math.round(c0.getBlue() + (j-start)*(c1.getBlue()-c0.getBlue())/((float) end-start));
            
            lut[j] = 0xFF000000 | ((r<<16) + (g<<8) + b);
         }
      }
      
      if(reverse)
      {
         int end = lut.length - 1, tmp = 0;
         for(int i=0; i<lut.length/2; i++) { tmp = lut[i]; lut[i] = lut[end-i]; lut[end-i] = tmp; }
      }
      return(lut);
   }

   /**
    * Get the key frame fractions which define this color map's lookup table (via piecewise-linear interpolation of 
    * color components between key frames) -- suitable for defining a linear color gradient representing the map.
    * @param reverse If true, return the key frame fractions for the reverse-direction LUT.
    * @return An array [0 k1 k2 .. kN 1], where 0 < k1 < k2 < ... < kN < 1 specifying the key frames defining this color
    * lookup table.
    */
   public float[] getKeyFrames(boolean reverse) 
   { 
      // NOTE: A color map LUT need not be symmetrical, so we have to be careful here!
      float[] out = new float[keyFrames.length];
      out[0] = 0f;
      out[out.length-1] = 1f;
      for(int i=1; i<out.length-1; i++)
         out[i] = (reverse ? (256 - getKeyFrameIndex(out.length - 1 - i)) : getKeyFrameIndex(i)) / 256.0f;
      return(out); 
   }
   
   /**
    * Get the key colors which define this color map's lookup table (via piecewise-linear interpolation of color 
    * components between key frames) -- suitable for defining a linear color gradient representing the map.
    * @param reverse If true, return the key colors for the reverse-direction LUT.
    * @return Array of key colors. 
    * @see {@link #getKeyFrames(boolean)}
    */
   public Color[] getKeyColors(boolean reverse) 
   { 
      int n = keyFrames.length;
      Color[] out = new Color[n];
      for(int i=0; i<n; i++) out[i] = getKeyFrameColor(reverse ? (n - i - 1) : i);
      return(out);
   }
   
   /** The number of key frames (between 2 and 10) defining this color map. */
   public int getNumKeyFrames() { return(keyFrames.length); }
   
   /** 
    * Get the mapping index (between 0 and 255) of one of the key frames defining this color map.
    * @param pos Ordinal position of key frame, between 0 and {@link #getNumKeyFrames()}.
    * @return The key frame's mapping index -- ie, its position in the color map's 256-element LUT.
    * @throws IndexOutOfBoundsException if <i>pos</i> is invalid.
    */
   public int getKeyFrameIndex(int pos) { return(0x0FF & (keyFrames[pos] >> 24)); }
   
   /** 
    * Get the RGB color for one of the key frames defining this color map.
    * @param pos Ordinal position of key frame, between 0 and {@link #getNumKeyFrames()}.
    * @return The key frame's color.
    * @throws IndexOutOfBoundsException if <i>pos</i> is invalid.
    */
   public Color getKeyFrameColor(int pos) { return(new Color(keyFrames[pos] & 0x00FFFFFF)); }
   
   private static final Pattern NAME_REGEX = Pattern.compile("[\\w]+");
   
   /**
    * Create a new color map with the same key frame definition as this one, but a different name.
    * 
    * @param s The desired name for the new color map. The new name cannot be null or empty, can only contain word
    * characters (a-z, A-Z, 0-9, and the underscore), and cannot match a built-in colormap name.
    * @return The new color map. However, if the name string is invalid,  then <b>this</b> is returned (ie, no new map 
    * is created).
    */
   public ColorMap rename(String s) 
   {
      if(s == null || s.equals(name) || isBuiltInName(s) || !NAME_REGEX.matcher(s).matches()) return(this);
      return(new ColorMap(s, keyFrames, true));
   }
   
   /** 
    * Create a new color map from this one by changing the LUT index of the specified key frame.
    * 
    * @param pos Ordinal position of key frame, between 1 and {@link #getNumKeyFrames()}-1. The first key frame always
    * defines the  color at LUT index 0, while the last always sets the color at LUT index 255.
    * @param lutIndex The new LUT index. The new value must preserve the strictly ascending order of LUT indices from
    * one key frame to the next.
    * @return The derived color map. However, if key frame position or the LUT index is not valid, returns <b>this</b>.
    */
   public ColorMap deriveColorMap(int pos, int lutIndex)
   {
      if(pos <= 0 || pos >= keyFrames.length-1 || 
            getKeyFrameIndex(pos-1) >= lutIndex || lutIndex >= getKeyFrameIndex(pos+1))
         return(this);
      int[] packed = new int[keyFrames.length];
      for(int i=0; i<keyFrames.length; i++)
      {
         if(i==pos) packed[i] = (keyFrames[i] & 0x00FFFFFF) | (lutIndex << 24);
         else packed[i] = keyFrames[i];
      }
      return(new ColorMap(name, packed, true));
   }
   
   /** 
    * Create a new ColorMap object from this one by changing the color of the specified key frame.
    * @param pos Ordinal position of key frame, between 0 and {@link #getNumKeyFrames()}.
    * @param c The key frame color in the new color map. Transparency ignored.
    * @return The derived color map. However, if key frame position is invalid or color is null, returns <b>this</b>.
    */
   public ColorMap deriveColorMap(int pos, Color c)
   {
      if(pos < 0 || pos >= keyFrames.length || c == null) return(this);
      int[] packed = new int[keyFrames.length];
      for(int i=0; i<keyFrames.length; i++)
      {
         if(i==pos) packed[i] = (keyFrames[i] & 0x0FF000000) | (c.getRGB() & 0x00FFFFFF);
         else packed[i] = keyFrames[i];
      }
      return new ColorMap(name, packed, true);
   }
   
   /** 
    * Create a new ColorMap object from this one by changing the number of defined key frames. The first and last 
    * key frames in this color map are always preserved in the derived map; the middle frames are found by sampling
    * evenly across this map's 256-entry LUT.
    * 
    * @param nFrames The number of key frames in the new color map. Must lie in [2..{@link #MAXKEYFRAMES}].
    * @return The derived color map. However, if number of frames is out of bounds, returns <b>this</b>.
    */
   public ColorMap deriveColorMap(int nFrames)
   {
      if(nFrames < 2 || nFrames > MAXKEYFRAMES || nFrames == keyFrames.length) return(this);
      int[] packed = new int[nFrames];
      packed[0] = keyFrames[0];
      packed[nFrames-1] = keyFrames[keyFrames.length-1];
      int delta = 255/(nFrames-1);
      for(int i=1; i<nFrames-1; i++)
         packed[i] = ((i*delta) << 24) + (0x00FFFFFF & getRGB(i*delta, false));
      
      return(new ColorMap(name, packed, true));
   }
   

   /**
    * Construct a color map.
    * @param The color map's name. Cannot be null or empty.
    * @param packed Array of key frames. Each is represented as a packed integer: 0xNNRRGGBB. 
    * @param custom True for a user-defined colormap, false for one of the built-in maps.
    * @throws IllegalArgumentException if either argument is invalid.
    */
   private ColorMap(String name, int[] packed, boolean custom)
   {
      if(name == null || ((!NAME_REGEX.matcher(name).matches())) || !validateKeyFrames(packed))
         throw new IllegalArgumentException("Invalid ColorMap definition");
      this.name = name;
      this.custom = custom;
      keyFrames = new int[packed.length];
      for(int i=0; i<packed.length; i++) keyFrames[i] = packed[i];
   }
   
   private boolean validateKeyFrames(int[] frames)
   {
      boolean ok = (frames != null) && (frames.length >= 2) && (frames.length <= MAXKEYFRAMES);
      int prev = 0;
      for(int i=0; ok && i<frames.length; i++)
      {
         int index = (frames[i] >> 24) & 0x0FF;
         if(i == 0) ok = (index == 0);
         else if(i == frames.length - 1) ok = (index == 255);
         else ok = (prev < index);

         prev = index;
      }
      return(ok);
   }
   
   /** 
    * The key frames (index + color) that fundamentally defined the color map's 256-entry LUT. Each key frame is packed
    * into a 32-bit integer as: 0xNNRRGGBB, where bits 31-24 encode an LUT index (between 0 and 255) and the remaining 
    * 24 bits encode the opaque RGB color at that index. The key frame list is in ascending order by index. The full LUT
    * is computed by piecewise-linear interpolation between of color components between key frames.
    */
   private final int[] keyFrames;
   /** The color map's user-facing name. */
   private final String name;
   /** True for a user-defined custom color map; false for one of the built-in colormaps. */
   private final boolean custom;

   /** Enumeration of the color maps built into <i>Figure Composer</i>. */
   public static enum BuiltIn
   {
      /** Grayscale (black to white) color map. */
      gray(new int[] {0, 0xFFFFFFFF}),
      /** Varies from blue to cyan to green to yellow to red. */
      hot(new int[] {0x000000FF, 0x4000FFFF, 0x8000FF00, 0xC0FFFF00, 0xFFFF0000}),
      /** Varies from black to red to yellow to white. Similar to Matlab's 'hot'. */
      autumn(new int[] {0, 0x60FF0000, 0xC0FFFF00, 0xFFFFFFFF}),
      /** Varies from dark blue to blue to cyan to yellow to red to dark red. Similar to Matlab's 'jet'. */
      jet(new int[] {0x00000080, 0x200000FF, 0x6000FFFF, 0xA0FFFF00, 0xE0FF0000, 0xFF800000}),
      /** Varies from indigo to dark blue to blue to green to orange to yellow. Similar to Matlab's 'parula'. */
      tropic(new int[] {0x004020A0, 0x200060E0, 0x6000A0C0, 0xA080C080, 0xE0FFD040, 0xFFFFEF02}),
      /** Varies from cyan to magenta. Similar to Matlab's 'cool'. */
      cool(new int[] {0x0000FFFF, 0xFFFF00FF}),
      /** Varies from black to copper to light copper. Similar to Matlab's 'copper'. */
      copper(new int[] {0, 0xCCFF9F65, 0xFFFFC77F}),
      /** Varies from black to steel blue to grey green to white. Similar to Matlab's 'bone'. */
      bone(new int[] {0, 0x60535373, 0xC0A7C7C7, 0xFFFFFFFF}),
      /** R->Y->G->C->B->M->R. Similar to Matlab's 'hsv'. */
      hsv(new int[] {0x00FF0000, 0x2CFFFF00, 0x5500FF00, 0x8000FFFF, 0xAB0000FF, 0xD4FF00FF, 0xFFFF0000}),
      /** Matplotlib perceptually uniform colormap: deep purple -> teal -> yellow. */
      viridis(new int[] {0x00440154, 0x1D482979, 0x7823898E, 0x8D1E9D89, 0xF4E2E418, 0xFFFDE725}),
      /** Matplotlib perceptually uniform colormap: dark teal -> med gray -> yellow. */
      cividis(new int[] {0x0000224E, 0x16003171, 0x3234456C, 0x4D50576C, 0x8A868379, 0xFCFEE434, 0xFFFEE806}),
      /** Matplotlib perceptually uniform colormap: med blue -> dark pink -> yellow. */
      plasma(new int[] {0x000D0887, 0x377100A8, 0xDDFEC029, 0xEEF9DC24, 0xFBF2F227, 0xFFF0F921}),
      /** Matplotlib perceptually uniform colormap: black -> dark orange -> bright yellow. */
      inferno(new int[] {0x00000004, 0x23260C51, 0x313E0966, 0x53751B6E, 0xC7FB9B06, 0xF4F1F179, 0xFFFCFFA4}),
      /** Matplotlib perceptually uniform colormap: black -> med red -> yellowish white. */
      magma(new int[] {0x00000004, 0x27271258, 0x38440F76, 0x5E802582, 0xB6F8765C, 0xEBFED89A, 0xFFFCFDBF});
      
      /** The one and only instance of this built-in color map. */
      public final ColorMap cm;
      
      BuiltIn(int[] frames) { cm = new ColorMap(toString(), frames, false); }
   }
   
   /** Get the set of all predefined color maps built into <i>Figure Composer</i>. */
   public static ColorMap[] getBuiltins()
   {
      BuiltIn[] values = BuiltIn.values();
      ColorMap[] out = new ColorMap[values.length];
      for(int i=0; i<out.length; i++) out[i] = values[i].cm;
      return(out);
   }
   
   /** Does the specified string match the name of one of the built-in color maps? */
   public static boolean isBuiltInName(String s)
   {
      if(s != null && !s.isEmpty())
      {
         for(ColorMap bcm : ColorMap.getBuiltins()) if(bcm.getName().equals(s)) return(true);
      }
      return(false);
   }
   
   /** 
    * Does the specified color map duplicate one of the predefined, built-in color maps -- or use the same name as a
    * built-in?
    * @param cm A color map.
    * @return True if the color map has the same name OR the same key frame definition as a built-in color map.
    */
   public static boolean duplicatesBuiltin(ColorMap cm)
   {
      if(cm != null)
      {
         for(ColorMap bcm : ColorMap.getBuiltins())
            if(cm.equals(bcm) || cm.getName().equals(bcm.getName()))
               return(true);
      }
      return(false);
   }
}
