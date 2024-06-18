package com.srscicomp.common.util;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A collection of static non-GUI utility methods. Any utility methods that do NOT require javax.swing or user interface
 * components in java.awt should go here; otherwise, put them in <i>com.srscicomp.common.ui.GUIUtilities</i>.
 * 
 * @author 	sruffner
 */
public class Utilities
{
   /** Flag set if operating system platform is Macintosh OSX. */
   private static final boolean isMac;
   /** Flag set if operating system platform is Windows. */
   private static final boolean isWindows;
   /** Flag set if operating system platform is Windows 2000. */
   private static final boolean isWin2000;
   /** Flag set if operating system platform is a flavor of Linux. */
   private static final boolean isLinux;
   
   static
   {
      String osname = System.getProperty("os.name", "unknown").toLowerCase();
      String osver = System.getProperty("os.version", "0.0");
      isMac = osname.startsWith("mac os x");
      isWindows = osname.startsWith("windows");
      isWin2000 = osname.startsWith("windows 2000") && osver.startsWith("5.0");
      isLinux = osname.startsWith("linux");
   }
   
   /**
    * Is current host operating system a version of Macintosh OSX? Mac Classic (OS9 or earlier) is not supported.
    * @return True if the Java VM is running under MacOSX; false otherwise.
    */
   public static boolean isMacOS() { return(isMac); }

   /**
    * Is current host operating system a version of Windows?
    * @return True if the Java VM is running under Windows; false otherwise.
    */
   public static boolean isWindows() { return(isWindows); }

   /**
    * Is current host operating system Windows 2000?
    * @return True if the Java VM is running under Windows 2000; false otherwise.
    */
   public static boolean isWindows2000() { return(isWin2000); }
   
   /**
    * Is current host operating system a flavor of Linux?
    * @return True if the Java VM is running under Linux; false otherwise.
    */
   public static boolean isLinux() { return(isLinux); }

   /**
    * Returns a string concatenating the values of the system properties <i>os.name</i>, <i>os.version</i>, and 
    * <i>os.arch</i>. If the operating system name cannot be retrieved, returns "Unknown".
    * @return Description of host operating system, as described.
    */
   public static String getOSDescription()
   {
      String name = System.getProperty("os.name");
      String ver = System.getProperty("os.version");
      String arch = System.getProperty("os.arch");
      if(name == null) return("Unknown");
      return(name + " (" + (ver==null ? "vers=?" : ver) + "; " + (arch==null ? "arch=?" : arch) + ")");
   }
   
   /**
    * Get the major version number for the Java runtime environment, as parsed from the "java.version" system property.
    * 
    * <p>For JRE 8 and earlier, the version string has the format "1.M.m_nnn", where the major version number is M. For
    * JRE 9 and later, the version string has the format "M.m.r". This method checks for both possibilities when 
    * parsing the major version number from the version string.</p>
    * 
    * @return The major version number parsed from version string. Returns 0 if unable to access the java.version 
    * system property, or if the version string format is not in one of the two described formats.
    */
   public static int getJavaMajorVersion()
   { 
      String jv = System.getProperty("java.version");
      
      int version = 0;
      if(jv != null)
      {
         String strMajor;
         if(jv.startsWith("1."))
         {
            // Pre-JDK9 version string format: 1.M.m_nnn, where M is the major version number
            int dot1 = 1;
            int dot2 = jv.indexOf('.', dot1+1);
            strMajor = dot2 > -1 ? jv.substring(dot1+1, dot2) : jv.substring(dot1+1);
         }
         else
         {
            // assume JDK9 (or later) version string format: M.m.r 
            int dot1 = jv.indexOf('.');
            strMajor = dot1 > -1 ? jv.substring(0,dot1) : jv;
         }
         
         try { version = Integer.parseInt(strMajor); } catch(NumberFormatException ignored) {}

      }

      return(version); 
   }
   
	/**
	 * Get the extension of a file, without the "." and with all lowercase letters.
	 * @param f The file
	 * @return The file's extension (characters after the last "." in filename) or null if there is none.
	 * @see Utilities#getExtension(String)
	 */  
	public static String getExtension(File f) 
	{
	   return((f == null) ? null : getExtension(f.getName()));
	}

   /**
    * Get the extension of a filename, without the "." and with all lowercase letters.
    * <p>CREDIT:  From code listed in The Java Tutorial topic "How To Use File Choosers".</p>
    * @param filename The file name
    * @return The filename's extension (ie, the characters after the last "."), or null if there is none.
    */  
	public static String getExtension(String filename)
	{
      String ext = null;
      int i = (filename == null) ? -1 : filename.lastIndexOf('.');
      if(i > 0 &&  i < filename.length() - 1) ext = filename.substring(i+1).toLowerCase();
      return(ext);	   
	}
	
   /**
    * Utility method that copies a source file to a destination file, creating the destination file if it does not yet
    * exist or replacing its original content otherwise. The method uses Java NIO channels to perform the file copy.
    * <p>CREDIT: Implementation adapted from <a href="https://gist.github.com/mrenouf/889747">Java snippet by
    * Mark Renouf</a>.</p>
    *
    * @param src Source file.
    * @param dst Destination file. It must exist, or it must be possible to create a file at this location.
    * @return Null if successful, otherwise a description of the IO exception caught. Possible reasons for failure: 
    * source file does not exist, is a directory, or is not readable; destination is an existing directory or does not 
    * exist and could not be created; or an IO error occurs during the operation. In the event of an error, the 
    * destination file is left as is.
    */
   public static String copyFile(File src, File dst)
   {
      FileInputStream fIn = null;
      FileOutputStream fOut = null;
      FileChannel srcFC = null;
      FileChannel dstFC = null;
      String eMsg = null;
      try 
      {
         // if destination does not exist, create a new file
         boolean dstExisted = !dst.createNewFile();

         fIn = new FileInputStream(src);
         srcFC = fIn.getChannel();
         fOut = new FileOutputStream(dst);
         dstFC = fOut.getChannel();
         
         // if destination file already existed, obliterate previous content
         if(dstExisted) dstFC.truncate(0L);
         dstFC.position(0L);
         
         // copy the bytes from source to destination file
         long bytesDone = 0;
         long bytesTotal = srcFC.size();
         while(bytesDone < bytesTotal)
         {
            bytesDone += srcFC.transferTo(bytesDone, bytesTotal-bytesDone, dstFC);
         }
      } 
      catch(IOException ioe) { eMsg = "File copy failed: " + ioe.getMessage(); }
      finally 
      {
        if(srcFC != null) try { srcFC.close(); } catch(IOException ignored) {}
        else if(fIn != null) try { fIn.close(); } catch(IOException ignored) {}
        
        if(dstFC != null) try { dstFC.close(); } catch(IOException ignored) {}
        else if(fOut != null) try { fOut.close(); } catch(IOException ignored) {}
      }
      
      return(eMsg);
   }

	/** 
	 * Utility method dumps the contents of a string buffer to a destination file, overwriting that file's contents if
	 * it existed prior to the call. If the buffer is empty, the destination file will be empty. Not thread safe.
	 * @param dst The destination file.
	 * @param buf Buffer the contents of which are to be dumped into the file. <b>NOTE: Method <code>toString()</code> is
	 * used to retrieve buffer contents. Therefore, this method is not recommended for dumping very large string 
	 * buffers</b>.
	 * @return True if successful, false if unable to create the destination file or if an IO error occurs while
	 * writing to it. In the event of an error, the file is not removed.
	 */
	public static boolean dumpToFile(File dst, StringBuilder buf) 
	{
        // delete file if it exists
        if(dst.isFile())
            if(!dst.delete())
                return(false);

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(dst), StandardCharsets.US_ASCII)))
        {
            writer.write(buf.toString());
            return(true);
        }
        catch(IOException ignored)
        {
            return(false);
        }
    }
	
	/**
	 * Do the specified abstract pathnames refer to the same file system object? Method returns true iff neither
	 * argument is null and <code>f1.getCanonicalFile.equals(f2.getCanonicalFile())</code>.
	 * 
	 * <p><i>NOTE that getting the canonical path of a file can throw an IO exception. If that happens, the method 
	 * resorts to a lexicographical comparison of the absolute path strings, which could yield a wrong answer. Also, 
	 * this is NOT intended for intense use, since it involves object allocations.</i></p>.
	 * <p>This utility method addresses an acknowledged bug with <code>java.io.File.equals()</code>. That method does
	 * a lexicographical comparison of absolute paths that may not correctly determine equality. In one case, I had two 
	 * file instances with identical absolute paths and yet <code>File.equals()</code> returned false. The two file 
	 * instances referred to file objects on a mapped network drive, which may have something to do with it.
	 * See Java Bug Database ID 4787260.</p>
	 * @param f1 An abstract pathname.
	 * @param f2 A second abstract pathname.
	 * @return True if pathnames refer to the same file system object, with caveats as described above.
	 */
	public static boolean filesEqual(File f1, File f2)
	{
	   if(f1 == null || f2 == null) return(false);
	   boolean same;
	   try
	   {
	      File f1c = f1.getCanonicalFile();
	      File f2c = f2.getCanonicalFile();
	      same = f1c.equals(f2c);
	   }
	   catch(IOException ioe) { same = f1.getAbsolutePath().equals(f2.getAbsolutePath()); }
	   return(same);
	}
	
	/**
	 * Limit the length of the argument string to the specified number of characters.
	 * @param s A string
	 * @param lim Maximum allowed string length.
	 * @return An empty string if the argument is null or the limit is non-positive. Otherwise, if the string is not too
	 * long, that same string is returned; else, it is truncated to the specified limit.
	 */
	public static String limitLength(String s, int lim)
	{
	   return((lim <= 0 || s==null) ? "" : ((s.length() <= lim) ? s : s.substring(0, lim)));
	}
	
   /**
    * Restrict any rotational angle in degrees to the equivalent angle in the range [0..360).
    * 
    * @param deg Any angle in degrees.
    * @return An equivalent angle in [0..360), if the argument is not already in that range.
    */
   public static double restrictAngle(double deg)
   {
      return((deg>=0) ? (deg % 360) : ((deg % (-360) + 360)) );
   }

   /**
    * Restrict any rotational angle in degrees to an equivalent angle in the range [S..S+360).
    * @param deg Any angle in degrees.
    * @param origin The start S of the 360-degree range over which the angle is to be restricted, in degrees.
    * @return The equivalent angle in the specified range.
    */
   public static double restrictAngle(double deg, double origin)
   {
      return(restrictAngle(deg-origin) + origin);
   }
   
   /**
    * Convert a rotational angle from radians to degrees and optionally restrict it to the unit circle.
    * @param rad Any angle in radians.
    * @param restrict If 0, the converted angle is not restricted to the unit circle. If positive, it is restricted to
    * [0..360); if negative, it is restricted to [-180..180).
    * @return The converted angle in degrees.
    */
   public static double rad2deg(double rad, int restrict)
   {
      double deg = rad * 180.0 / Math.PI;
      if(restrict != 0)
      {
         deg = restrictAngle(deg);
         if(restrict < 0 && deg >= 180) deg -= 360;
      }
      return(deg);
   }
   
   /**
    * Restrict an integer value to a specified range. 
    * @param lo The low end of the range, inclusive.
    * @param hi The high end of the range, inclusive.
    * @param val The value to be range-restricted.
    * @return The same value if it lies in the range; else, the range endpoint that's closest to the value.
    */
   public static int rangeRestrict(int lo, int hi, int val)
   {
      if(lo > hi) { int swap = lo; lo = hi; hi = swap; }
      return(Math.min(hi, Math.max(lo, val)));
   }

   /** 
    * Restrict a float value to a specified range. If any argument is NaN, returns NaN.
    * @see Utilities#rangeRestrict(int, int, int)
    */
   public static float rangeRestrict(float lo, float hi, float val)
   {
      if(Float.isNaN(val) || Float.isNaN(lo) || Float.isNaN(hi)) return(Float.NaN);
      if(lo > hi) { float swap = lo; lo = hi; hi = swap; }
      return(Math.min(hi, Math.max(lo, val)));
   }

   /** 
    * Restrict a double value to a specified range. If any argument is NaN, returns NaN.
    * @see Utilities#rangeRestrict(int, int, int)
    */
   public static double rangeRestrict(double lo, double hi, double val)
   {
      if(Double.isNaN(val) || Double.isNaN(lo) || Double.isNaN(hi)) return(Double.NaN);
      if(lo > hi) { double swap = lo; lo = hi; hi = swap; }
      return(Math.min(hi, Math.max(lo, val)));
   }

   
	/**
	 * Limit the precision of a double-valued number.
	 * 
	 * @param d The number to be rounded.
	 * @param prec If absolute value of number is greater than or equal to 1, this is the maximum number of digits after 
	 * decimal point; otherwise, it is the number of significant digits after decimal point. Allowed range is [0..10].
	 * @return The value limited to the specified precision, as described.
	 */
	public static double limitPrecision(double d, int prec)
	{
        if( Double.isInfinite(d) || Double.isNaN(d) || d == 0 ) return( d );                // special cases

		int adjPrec = Utilities.rangeRestrict(0, 10, prec);
		double absD = Math.abs(d);
		if( absD < 1.0 )
		{
			adjPrec += (int) -Math.ceil( Utilities.log10(absD) );
		}
		double frac = absD - Math.rint(absD);
		double scale = Math.pow( 10, adjPrec );
		frac = Math.rint(frac * scale)/scale;
		return( (d<0 ? -1 : 1) * (Math.rint(absD) + frac) );
	}

   /**
    * Limit the number of fractional digits <i>N</i> in a double-valued number <i>D</i>. This method returns the value
    * <i>Math.rint(D*S)/S</i>, where <i>S = 10^N</i>.
    * 
    * @param d The number to be rounded.
    * @param nFrac The maximum number of digits after decimal point. Allowed range is [0..10].
    * @return The same number rounded and limited to the specified number of fractional digits, as described.
    */
   public static double limitFracDigits(double d, int nFrac)
   {
      if(Double.isInfinite(d) || Double.isNaN(d) || d == 0) return(d);                // special cases
      int adj = Utilities.rangeRestrict(0, 10, nFrac);
      double scale = Math.pow(10, adj);
      return(Math.rint(d*scale)/scale);
   }

   /**
    * Limit the number of significant digits in a double-valued number by rounding the value to the specified number of 
    * significant digits.
    * <p>(NOTE: The algorithm is not perfect due to limitations with the representation of double-precision floating
    * point values. For example, with nSig=3 and value=1.255, the method returns 1.25 instead of 1.26. Based on an
    * <a href="https://stackoverflow.com/questions/202302/rounding-to-an-arbitrary-number-of-significant-digits.">
    * algorithm posted on StackOverflow</a>.
    *
    * @param d The number to be rounded.
    * @param nSig The allowed number of significant digits. Allowed range is [1..16].
    * @return The same number rounded and limited to the specified number of significant digits. No action is taken if
    * the number if NaN or infinite or zero.
    */
   public static double limitSignificantDigits(double d, int nSig)
   {
      if(Double.isInfinite(d) || Double.isNaN(d) || d == 0) return(d);                // special cases
      
      nSig = Utilities.rangeRestrict(1, 16, nSig);
      
      final double exp = Math.ceil(Math.log10(d < 0 ? -d: d));
      final int power = nSig - (int) exp;

      final double magnitude = Math.pow(10, power);
      final long shifted = Math.round(d*magnitude);
      return(shifted/magnitude);   
   }
   
   /** 
    * Limit the number of significant digits and fractional digits in a double-valued number. If you need to restrict
    * both the fractional and significant digits, use this method rather than calling {@link #limitFracDigits(double,
    * int)} followed by {@link #limitSignificantDigits(double, int)}.
    * 
    * @param d The number to be rounded.
    * @param nSig The allowed number of significant digits. Allowed range is [1..16].
    * @param nFrac The maximum number of digits after decimal point. Range-restricted to [-1..10]. If -1, then the 
    * number of fractional digits is not restricted.
    * @return The number rounded to the specified number of significant digits and fractional digits. Returns NaN,
    * +/-Infinity, and 0 unchanged.
    */
   public static double limitSigAndFracDigits(double d, int nSig, int nFrac)
   {
      if(Double.isInfinite(d) || Double.isNaN(d) || d == 0) return(d);                // special cases

      nSig = Utilities.rangeRestrict(1, 16, nSig);
      if(nFrac > 10) nFrac = 10;
      
      int maxFrac = nSig - ((int) Math.log10(d < 0 ? -d: d)) - 1;
      if(nFrac >=0 && maxFrac > nFrac) 
      {
         double scale = Math.pow(10, nFrac);
         d = Math.rint(d*scale)/scale;
         if(d==0) return(0);
      }
      
      final double exp = Math.ceil(Math.log10(d < 0 ? -d: d));
      final int power = nSig - (int) exp;

      final double magnitude = Math.pow(10, power);
      final long shifted = Math.round(d*magnitude);
      return(shifted/magnitude);   
   }
   
   /**
    * Return a string representation of a floating-point number that preserves up to a specified number of significant
    * digits and, optionally, a specified number of fractional digits (right of the decimal point). 
    * 
    * <p>Essentially, this method rounds the double-precision value <i>num</i> to satisfy the limits on significant and 
    * fractional digits, then relies on {@link Double#toString(double)} to convert the adjusted value to string form. 
    * However, if the <i>compact</i> flag is set, the method returns a slightly more compact form under certain 
    * scenarios:
    * <ul>
    * <li>If <i>4 &le; log|num| &lt; 7</i> and <i>nSig &lt; log|num|</i>, the number is put in exponential notation
    * rather than the decimal notation returned by {@link Double#toString(double)}. Thus, 12600.0 ==> 1.26E4, 
    * -350000.0 ==> -3.5E5.</li>
    * <li>If <i>1 &le; |num| &lt; 1E4</i> and the string returned by {@link Double#toString(double)} ends with ".0",
    * the ".0" is trimmed.</li>
    * </ul>
    * </p>
    * 
    * <p>Limiting the number of fractional digits is a special use case when you want to round numbers <b>in decimal 
    * notation</b> to the nearest nearest integer, tenth place, hundredth place, or thousandth place. If 
    * <i>|num| &lt; pow(10,-nFrac)</i>, it will effectively be rounded to "0".</p>
    * 
    * <p>The method first rounds <i>num</i> to limit the number of fractional digits, then rounds again to limit the 
    * number of significant digits. The first rounding step is skipped if <i>nSig - log|num| - 1 &le; nFrac</i>, 
    * since that means rounding to <i>nSig</i> significant digits will effectively restrict the number of fractional
    * digits to <i>nFrac</i> or less. For example, let <i>nSig=4 and nFrac=2</i>. If the value is 1.23456, limiting it 
    * to 2 fractional digits yields 1.23, which only has 3 significant digits, and the string returned is "1.23". 
    * However, if the value is 123.4567, there's no need to round to limit the number to 2 fractional digits, because
    * rounding to 4 significant digits already restricts the result to 1 digit after the decimal place; the string
    * "123.5" is returned.</p>
    * 
    * @param num The value to convert to string form.
    * @param nSig Number of significant digits preserved in the string representation. Range-restricted to [1..16].
    * @param nFrac Number of fractional digits preserved when string representation is in decimal notation. If this lies
    * outside the range [0..3], then the number of fractional digits is only limited by the allowed number of 
    * significant digits.
    * @param compact If true, the string returned deviates from that returned by {@link Double#toString(double)} --
    * as described above.
    * @return The string representation of the value. Note that, "NaN" is returned if the number is infinite or not a
    * number.
    */
   public static String toString(double num, int nSig, int nFrac, boolean compact)
   {
      // special cases
      if(Double.isInfinite(num) || Double.isNaN(num)) return("NaN");
      if(num == 0) return("0");
      
      int exp = (int) Math.log10(Math.abs(num));
      nSig = Utilities.rangeRestrict(1, 16, nSig);

      // round to limit # of fractional digits if warranted, then round to limit # of significant digits. 
      if(nFrac >= 0 && nFrac <= 3 && (nSig - exp - 1) > nFrac) num = Utilities.limitFracDigits(num, nFrac);
      num = Utilities.limitSignificantDigits(num, nSig);
      
      // convert adjusted value to string representation (see comments in function header)
      String s = Double.toString(num);
      if(exp >= 7 || exp < 0 || !compact) return((num==0 && compact) ? "0" : s);
      
      if(exp >= 4 && nSig < exp)
      {
         s = Utilities.limitSignificantDigits(num * Math.pow(10, -exp), nSig) + "E" + exp;
         return(s);
      }
      
      if(s.endsWith(".0")) return(s.substring(0, s.length()-2));
      return(s);
   }
   

	/**
    * Return a string representation of a floating-point number that preserves up to a specified number of significant
    * digits and, optionally, a specified number of fractional digits (right of the decimal point).
    * 
    * <p>This is merely a wrapper for {@link #toString(double, int, int, boolean)} with <i>compact=true</i> to achieve
    * as compact a representation as possible.</p>
    * 
    * @param d The value to convert to string form.
    * @param nSig Number of significant digits preserved in the string representation. Range-restricted to [1..16].
    * @param nFrac Number of fractional digits preserved when string representation is in decimal notation. If this lies
    * outside the range [0..3], then the number of fractional digits is only limited by the allowed number of 
    * significant digits.
    * @return The string representation of the value. Note that, "NaN" is returned if the number is infinite or not a
    * number.
	 */
	public static String toString(double d, int nSig, int nFrac) { return(toString(d, nSig, nFrac, true)); }

	/**
	 * Return a string representation of the integer array, with a single space separating each value in its string form.
	 * @param values An array of integer values.
	 * @return A string representation of the values, separated by space characters.
	 */
	public static String toString(int[] values)
	{
	   StringBuilder sb = new StringBuilder();
	   if(values != null && values.length > 0)
	   {
	      for(int i=0; i<values.length-1; i++) sb.append(values[i]).append(" ");
	      sb.append(values[values.length-1]);
	   }
	   return(sb.toString());
	}

	/**
	 * Return a string representation of the array of double-precision floating-point values, preserving the specified
	 * number of significant and, optionally, fractional digits in each value. A single space separates each numeric 
	 * token in the string. Each floating-point value is converted using {@link #toString(double, int, int)}.
	 * 
	 * @param values An array of double values. If null or empty, method returns an empty string.
    * @param nSig Number of significant digits preserved in each numeric token. Range-restricted to [1..16].
    * @param nFrac Number of fractional digits preserved in each numeric token if in decimal notation. If this lies
    * outside the range [0..3], then the number of fractional digits is only limited by the allowed number of 
    * significant digits.
	 * @return The array as a string of numeric tokens, as described. Any value in the array that is infinite or not a
	 * number is represented by the token "NaN".
	 */
	public static String toString(double[] values, int nSig, int nFrac)
	{
      StringBuilder sb = new StringBuilder();
      if(values != null && values.length > 0)
      {
         for(int i=0; i<values.length-1; i++) sb.append(toString(values[i], nSig, nFrac)).append(" ");
         sb.append(toString(values[values.length-1], nSig, nFrac));
      }
      return(sb.toString());
	}

   /**
    * Return a string representation of a portion of an array of single-precision floating-point values, preserving the 
    * specified number of significant and, optionally, fractional digits in each value. A single space separates each 
    * numeric token in the string. Each floating-point value is converted using {@link #toString(double, int, int)}.
    * 
    * @param values Array of float values. If null or empty, the method returns an empty string.
    * @param start The array index at which to begin converting values. If negative or greater than or equal to the 
    * array length, the method returns an empty string.
    * @param n The number of values to convert. If <i>start+n</i> exceeds the array length, then the number of numeric
    * tokens in the output string will be less than <i>n</i>.
    * @param nSig Number of significant digits preserved in each numeric token. Range-restricted to [1..7]. <i>Note that
    * single-precision floating-point specification preserves up to 7 significant digits, 16 for double-precision.</i>
    * @param nFrac Number of fractional digits preserved in each numeric token if in decimal notation. If this lies
    * outside the range [0..3], then the number of fractional digits is only limited by the allowed number of 
    * significant digits.
    * @return The array as a string of numeric tokens, as described. Any value in the array that is infinite or not a
    * number is represented by the token "NaN".
    */
   public static String toString(float[] values, int start, int n, int nSig, int nFrac)
   {
      if(values == null || values.length == 0 || start < 0 || start >= values.length) return("");
      nSig = Utilities.rangeRestrict(1, 7, nSig);
      
      StringBuilder sb = new StringBuilder();
      int last = Math.min(values.length, start+n) - 1;
      for(int i=start; i<last; i++) sb.append(toString(values[i], nSig, nFrac)).append(" ");
      sb.append(toString(values[last], nSig, nFrac));
      
      return(sb.toString());
   }

   /**
    * Return a string representation of an array of single-precision floating-point values, preserving the 
    * specified number of significant and, optionally, fractional digits in each value. A single space separates each 
    * numeric token in the string. Each floating-point value is converted using {@link #toString(double, int, int)}.
    * 
    * <p>This is merely a convenience method wrapping {@link #toString(float[], int, int, int, int)}, passing 
    * <i>start=0</i> and <i>n=values.length</i>.
    */
	public static String toString(float[] values, int nSig, int nFrac)
	{
	   return((values == null) ? "" : toString(values, 0, values.length, nSig, nFrac));
	}

   /**
    * Return a string representation of the buffer of single-precision floating-point valuess, preserving the specified
    * number of significant and, optionally, fractional digits in each value. A single space separates each numeric 
    * token in the string. Each floating-point value is converted using {@link #toString(double, int, int)}.
    * 
    * @param fBuf Buffer of floating-point values. If null or if the buffer has no values remaining, the method returns 
    * an empty string.
    * @param nSig Number of significant digits preserved in each numeric token. Range-restricted to [1..7]. <i>Note that
    * single-precision floating-point specification preserves up to 7 significant digits, 16 for double-precision.</i>
    * @param nFrac Number of fractional digits preserved in each numeric token if in decimal notation. If this lies
    * outside the range [0..3], then the number of fractional digits is only limited by the allowed number of 
    * significant digits.
    * @return The contents of the buffer as a string of whitespace-separated numeric tokens. Any value in the buffer
    * that is infinite or not a number is represented by the token "NaN".
    */
   public static String toString(FloatBuffer fBuf, int nSig, int nFrac)
   {
      if(fBuf == null || !fBuf.hasRemaining()) return("");
      nSig = Utilities.rangeRestrict(1, 7, nSig);

      StringBuilder sb = new StringBuilder();
      while(fBuf.hasRemaining())
      {
         sb.append(toString(fBuf.get(), nSig, nFrac));
         if(fBuf.hasRemaining()) sb.append(" ");
      }
      return(sb.toString());
   }
   
   /**
    * Return a string representation of the (x,y)-coord pair, with a single space separating the coordinate values. Each
    * coordinate is converted to string from using {@link #toString(double, int, int)}.
    * 
    * @param pt An (x,y)-coordinate pair.
    * @param nSig Number of significant digits preserved in the string representation. Range-restricted to [1..16].
    * @param nFrac Number of fractional digits preserved when string representation is in decimal notation. If this lies
    * outside the range [0..3], then the number of fractional digits is only limited by the allowed number of 
    * significant digits.
    * @return A string representation of the coordinate pair: "x y". If either coordinate is infinite or not a number,
    * it is represented by the token "NaN".
    */
   public static String toString(Point2D pt, int nSig, int nFrac)
   {
      if(pt != null) return( toString(new double[] { pt.getX(), pt.getY() }, nSig, nFrac));
      return("");
   }
   /**
    * Return an array of single-precision floating-point values parsed from a string containing one or more 
    * whitespace-separated floating-point numeric tokens.
    * 
    * @param s A list of one or more whitespace-separated string tokens representing floating-point numbers.
    * @return The array of floating-point numbers parsed from the string, in the order listed. If the string is empty
    * or null or contains <b>ANY</b> non-numeric tokens, the method returns null.
    */
	public static float[] fromString(String s)
	{
	   if(s == null) return(null);
	   StringTokenizer tokenizer = new StringTokenizer(s);
	   int n = tokenizer.countTokens();
	   if(n == 0) return(null);
	   
	   float[] values = new float[n];
	   try
	   {
	      for(int i=0; i<n; i++) values[i] = Float.parseFloat(tokenizer.nextToken());
	   }
	   catch(NumberFormatException nfe) { return(null); }
	   
	   return(values);
	}
	
	/** 
	 * Parse the specified string as a list of numeric tokens separated by the specified regular expression. The method
	 * uses {@link String#split(String)} to tokenize the string argument and {@link Double#parseDouble(String)} to 
	 * parse each individual token.
	 * 
	 * @param s The source string.
	 * @param regexSep The regular expression that serves as the token separator. For example, for a comma-separated
	 * list, simply specify ",". If null, the method uses whitespace as a separator.
	 * @param tokenLimit If positive, this is the maximum number of tokens allowed; otherwise, it is ignored and any 
	 * number of tokens are allowed.
	 * @return The list of numeric values parsed from the source string. Returns an empty array if the source string is
	 * empty or contains only whitespace. Returns null if: (1) the source string is null, (2) <i>tokenLimit &gt; 0</i> 
	 * and the number of tokens in the string exceeds this limit; or (3) any token cannot be parsed as a floating-point 
	 * numeric token.
	 */
	public static double[] parseDoubleList(String s, String regexSep, int tokenLimit)
	{
	   if(s==null) return(null);
	   s = s.trim(); 
	   if(s.isEmpty()) return(new double[0]);
	   String[] tokens = s.split(regexSep == null ? "\\s" : regexSep);
      if(tokenLimit > 0 && tokens.length > tokenLimit) return(null);
      double[] vals = new double[tokens.length];
      try
      {
         for(int i=0; i<tokens.length; i++) vals[i] = Double.parseDouble(tokens[i]); 
      }
      catch(NumberFormatException nfe) { return(null); }
      
      return(vals);
	}
	

	private final static double LN_10 = Math.log(10);
	private final static double LN_2 = Math.log(2);

	/**
	 * Return the base-10 logarithm of a double value.  The caveats of {@link Math#log(double)} apply.
	 * 
	 * @param 	d the value
	 * @return	its base-10 logarithm
	 */
	public static double log10( double d )
	{
		return( Math.log(d) / LN_10 );
	}

	/**
	 * Return the base-2 logarithm of a double value.  The caveats of {@link Math#log(double)} apply.
	 * 
	 * @param 	d the value
	 * @return	its base-2 logarithm
	 */
	public static double log2( double d )
	{
		return( Math.log(d) / LN_2 );
	}

   /** 
    * A convenience method for testing whether the value is finite and can be represented as a double value (ie, it 
    * does NOT equal {@link Double#NaN}).
    * 
    * @param   d the value to test
    * @return  <code>true</code> if the specified value is finite, valid double
    */
   public static boolean isWellDefined( double d ) { return(Double.isFinite(d)); }

   /**
    * A convenience method for testing whether all of the values in a double-valued array are well-defined.
    * 
    * @param dVals The array to test.
    * @return <code>True</code> iff the specified array is not <code>null</code>, not empty, and contains all 
    * well-defined values (valid and finite).
    */
   public static boolean isWellDefined( double[] dVals )
   {
      if(dVals == null || dVals.length == 0) return(false);
      for(double d : dVals) if(!Double.isFinite(d)) return(false);

      return(true);
   }

   /** 
    * Is the specified value a valid, finite single-precision FP number?
    * @param f The value to test.
    * @return True if the specified value is a finite, valid single-precision floating-point value.
    */
   public static boolean isWellDefined(float f) { return(Float.isFinite(f) ); }

   /** 
    * Are the specified values both finite single-precision FP numbers?
    * @return True if both values are finite, single-precision floating-point numbers.
    */
   public static boolean isWellDefined(float f1, float f2) { return( Float.isFinite(f1) && Float.isFinite(f2) ); }
   
   /**
    * Does the specified array contain valid, finite single-precision floating-point numbers?
    * @param fVals The array to test.
    * @return <code>True</code> iff the specified array is not <code>null</code>, not empty, and contains all 
    * well-defined values (valid and finite).
    */
   public static boolean isWellDefined( float[] fVals )
   {
      if(fVals == null || fVals.length == 0) return(false);
      for(float f : fVals) if(!Float.isFinite(f)) return false;

      return(true);
   }

   /**
	 * A convenience method for checking that the coordinates of the specified point in 2D are both finite numbers that 
	 * can be represented as double values.
	 * 
	 * @param 	pt the point to test
	 * @return	<code>true</code> if the coordinates of the specified point are both finite, valid doubles.  If the 
	 * 	argument is <code>null</code>, <code>false</code> is returned.
	 */
	public static boolean isWellDefined( Point2D pt )
	{
		return( (pt!=null) && Double.isFinite(pt.getX()) && Double.isFinite(pt.getY()) );
	}

	/**
	 * A convenience method for checking that the coordinates of the specified point in 3D are both finite numbers that
	 * can be represented as double values.
	 * 
	 * @param p3 The 3D point to test.
	 * @return True if the X,Y,Z coordinates of the specified point are both finite, value doubles. If the argument is
	 * null, returns false.
	 */
	public static boolean isWellDefined(Point3D p3)
	{
	   return(p3 != null && Double.isFinite(p3.getX()) && Double.isFinite(p3.getY()) && Double.isFinite(p3.getZ()));
	}
	
   /**
    * A convenience method that transforms the specified point and returns an indication of whether or not the 
    * transformed point is well-defined. 
    * 
    * @param pBef The original point.  Will not be altered by this method.  If this point is not well-defined, then 
    * the transformed point is obviously not well-defined either.
    * @param xfm The transform to apply.  If <code>null</code>, the identity transform is assumed, in which case this 
    * method acts like <code>isWellDefined(Point2D)</code>.
    * @param pAft If this <code>Point2D</code> is not <code>null</code>, the coordinates of the transformed point are 
    * stored in it.  If the result of the transformation is not well-defined, the point is set to <code>(NaN, NaN)
    * </code>. If this argument is <code>null</code>, the method must allocate a <code>Point2D</code> object on the 
    * heap, and the caller won't have access to the transformed coordinates.
    * @return <code>True</code> iff the transformed point is well-defined.
    */
   public static boolean isWellDefinedAfterTransform(Point2D pBef, AffineTransform xfm, Point2D pAft)
   {
      Point2D p = (pAft==null) ? new Point2D.Double() : pAft;

      if(!Utilities.isWellDefined(pBef))
      {
         p.setLocation(Double.NaN, Double.NaN);
         return(false);
      }

      p.setLocation(pBef);
      if(xfm != null) xfm.transform(p, p);
      return(Utilities.isWellDefined(p));
   }

   /** 
	 * A convenience method for checking that an array of 2D points are all well-defined, ie, their coordinates are 
	 * finite numbers that can be represented as double values.
	 * 
	 * @param 	pts the array of points to test
	 * @return	<code>true</code> if the array if all the points in the array are well-defined IAW the criteria of 
	 * {@link #isWellDefined(Point2D).  Returns <code>true</code> if the array is empty, but returns <code>false</code>
    *  if the array is <code>null</code>.
	 */
	public static boolean isWellDefined( Point2D[] pts )
	{
		if( pts == null ) return( false );

      for(Point2D pt : pts)
         if (!isWellDefined(pt)) return (false);

		return( true );
	}

	/**
	 * Swap the two points. No action is taken if either argument is null, or they are the same (<i>p1 == p2</i>).
	 * @param p1 A point in 2D.
	 * @param p2 Another point
	 */
	public static void swap(Point2D p1, Point2D p2)
	{
	   if(p1 == null || p2 == null || p1 == p2) return;
	   double x = p1.getX();
	   double y = p1.getY();
	   p1.setLocation(p2);
	   p2.setLocation(x, y);
	}
	
	/**
	 * Determine the intersection of the specified rectangles, assuming that they are defined WRT a Cartesian coordinate 
	 * system in which the y-axis increases UPWARD rather than downward.  One of the source rectangles may also serve as 
	 * the destination rectangle, in which case the source rectangle's original definition is overwritten.  If either 
	 * source rectangle is empty, so will be the intersection.
	 * 
	 * <p>The source rectangles should be defined by a top-left corner, width, and height -- IAW the documentation on 
	 * {@link Rectangle2D Rectangle2D}.  However, this method assumes that each rectangle's bottom-left
	 * corner has y-coord at yTL - H, where yTL is the y-coord of the top-left corner and H is the height of the 
	 * rectangle.</p>
	 * 
	 * @param 	src1 A rectangle.
	 * @param 	src2 A second rectangle.
	 * @param 	dest The intersection of the two rectangles.
	 */
	public static void intersectRectYUp( Rectangle2D src1, Rectangle2D src2, Rectangle2D dest )
	{
		if( src1.isEmpty() || src2.isEmpty() ) 
			dest.setFrame(0,0,0,0);
		else
		{
			double x = Math.max( src1.getX(), src2.getX() );
			double y = Math.min( src1.getY(), src2.getY() );
			double w = Math.min( src1.getX() + src1.getWidth(), src2.getX() + src2.getWidth() ) - x;
			double h = y - Math.max( src1.getY() - src1.getHeight(), src2.getY() - src2.getHeight() );
			dest.setFrame( x, y, w, h );
		}
	}

	/**
	 * Determine the union of the specified rectangles, assuming that they are defined WRT a Cartesian coordinate system 
	 * in which the y-axis increases UPWARD rather than downward.  One of the source rectangles may also serve as the 
	 * destination rectangle, in which case the source rectangle's original definition is overwritten.
	 * 
	 * <p>The source rectangles should be defined by a top-left corner, width, and height -- IAW the documentation on 
	 * {@link Rectangle2D Rectangle2D}.  However, this method assumes that each rectangle's bottom-left
	 * corner has y-coord at yTL - H, where yTL is the y-coord of the top-left corner and H is the height of the 
	 * rectangle.</p>
	 * 
	 * @param 	src1 A rectangle.
	 * @param 	src2 A second rectangle.
	 * @param 	dest The union of the two rectangles.
	 */
	public static void unionRectYUp( Rectangle2D src1, Rectangle2D src2, Rectangle2D dest )
	{
		if( src1.isEmpty() && src2.isEmpty() )
			dest.setFrame(0,0,0,0);
		else if( src1.isEmpty() )
			dest.setFrame( src2.getX(), src2.getY(), src2.getWidth(), src2.getHeight() );
		else if( src2.isEmpty() )
			dest.setFrame( src1.getX(), src1.getY(), src1.getWidth(), src1.getHeight() );
		else
		{
			double x = Math.min( src1.getX(), src2.getX() );
			double y = Math.max( src1.getY(), src2.getY() );
			double w = Math.max( src1.getX() + src1.getWidth(), src2.getX() + src2.getWidth() ) - x;
			double h = y - Math.min( src1.getY() - src1.getHeight(), src2.getY() - src2.getHeight() );
			dest.setFrame( x, y, w, h );
		}
	}

   /**
    * Form the union of two source rectangle, ignoring empty ones. If both rectangles are empty, the result is an empty 
    * rectangle at (0,0). If either source rectangle is empty, the result is the other one. If neither rectangle is 
    * empty, the method simply calls <code>Rectangle2D.union()</code> to calculate their union.
    * 
    * @param src1 The first source rectangle.
    * @param src2 The second source rectangle.
    * @param dst This argument will contain the union of the two source rectangles. One of the source rectangles can
    * also be safely specified as the destination.
    * @throws IllegalArgumentException if any argument is <code>null</code>.
    */
   public static void rectUnion(Rectangle2D src1, Rectangle2D src2, Rectangle2D dst)
   {
      if(src1 == null || src2 == null || dst == null) throw new IllegalArgumentException("Null argument!");

      if(src1.isEmpty()) dst.setRect(src2);
      else if(src2.isEmpty()) dst.setRect(src1);
      else Rectangle2D.union(src1, src2, dst);
   }

   /**
    * Adjusts the given endpoints as necessary so that the line segment connecting them is clipped to the specified 
    * rectangle. If the line segment is entirely outside the rectangle, then both points become ill-defined (ie, their 
    * coordinates are set to <code>Double.NaN</code>). A point lying on the edge of the rectangle is NOT outside it.
    * 
    * <p><em>IMPORTANT</em>: The method assumes both points and rectangle are defined in a RIGHT-handed coordinate 
    * system (x-axis increasing rightward, y-axis increasing upward). ALSO, in the interests of performance, the 
    * arguments are not checked for validity!</p>
    * 
    * <p><em>CREDITS</em>: Uses the Cohen-Sutherland algorithm for line-clipping. Consult a basic computer graphics 
    * tutorial for an explanation of this algorithm.</p>
    * 
    * @param p0 One endpoint of line segment. Must not be <code>null</code> and must be well-defined.
    * @param p1 Other endpoint of line segment. Must not be <code>null</code> and must be well-defined.
    * @param rClip The clipping rectangle. Must not be <code>null</code> and must be well-defined.
    * @return If the line segment lies entirely outside the clipping rectangle, -1 is returned and the coordinates of 
    * both endpoints are set to NaN. If it lies entirely inside the rectangle, 0 is returned and neither endpoint is 
    * changed. Otherwise, the line segment is partially clipped, and one or both endpoints are changed to define the 
    * resulting clipped line segment: 1 is returned if only <code>p0</code> is changed, 2 if only <code>p1</code> is 
    * changed, and 3 if both endpoints must be changed. No other return values are possible.
    */
   @SuppressWarnings("ConstantValue")
   public static int clipLine(Point2D p0, Point2D p1, Rectangle2D rClip)
   {
      // careful! Rectangle2D assumes left-handed coord system! (see below).
      int outCode0 = rClip.outcode(p0);
      int outCode1 = rClip.outcode(p1);

      // both endpts entirely inside clip rectangle, so no clipping was needed
      if(outCode0 == 0 && outCode1 == 0) return(0);

      // adjust endpts as needed to form the clipped line segment, if possible.
      boolean adjP0 = false;
      boolean adjP1 = false;
      while(outCode0 != 0 || outCode1 != 0)
      {
         // line is entirely outside clip rectangle
         if((outCode0 & outCode1) == 0) 
         {
            p0.setLocation(Double.NaN, Double.NaN);
            p1.setLocation(Double.NaN, Double.NaN);
            return(-1);
         }

         // if first endpt is outside of clip rectangle, try to adjust it first; else look at second endpt
         int outCodeAdj = (outCode0 != 0) ? outCode0 : outCode1;
         double x = 0;
         double y = 0;
         if((outCodeAdj & Rectangle2D.OUT_BOTTOM) == Rectangle2D.OUT_BOTTOM)
         {
            // in our right-handed coord system, this means point is above TOP of rect
            y = rClip.getY() + rClip.getHeight();
            x = p0.getX() + (p1.getX() - p0.getX()) * (y - p0.getY()) / (p1.getY() - p0.getY());
         }
         else if((outCodeAdj & Rectangle2D.OUT_TOP) == Rectangle2D.OUT_TOP)
         {
            // in our right-handed coord system, this means point is below BOTTOM of rect
            y = rClip.getY();
            x = p0.getX() + (p1.getX() - p0.getX()) * (y - p0.getY()) / (p1.getY() - p0.getY());
         }
         else if((outCodeAdj & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT)
         {
            x = rClip.getX() + rClip.getWidth();
            y = p0.getY() + (p1.getY() - p0.getY()) * (x - p0.getX()) / (p1.getX() - p0.getX());
         }
         else if((outCodeAdj & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT)
         {
            x = rClip.getX();
            y = p0.getY() + (p1.getY() - p0.getY()) * (x - p0.getX()) / (p1.getX() - p0.getX());
         }

         if(outCodeAdj == outCode0)
         {
            adjP0 = true;
            p0.setLocation(x, y);
            outCode0 = rClip.outcode(p0);
         }
         else
         {
            adjP1 = true;
            p1.setLocation(x, y);
            outCode1 = rClip.outcode(p1);
         }
      }
      return( (adjP0 && adjP1) ? 3 : (adjP0 ? 1 : 2) );
   }

   /**
    * Clip the specified "connect the dots" polyline to the specified rectangle. Ill-defined points in the polyline's 
    * point list are interpreted as gaps in the polyline and are left as is. The polyline point list is modified as 
    * needed so that, if one connects the dots (with gaps whenever an ill-defined point appears in the list) with an 
    * infinitely thin pen, the resulting path traces over the original polyline INSIDE the clip rectangle. In addition, 
    * the method can generate a list of all well-defined points in the original polyline that are within the clipped 
    * rectangle; this is useful if one wants to draw marker symbols only at defining points on the clipped polyline 
    * (but NOT at "dangling" endpoints where the polyline enters or leaves the rectangle!).
    * 
    * <p>The clip rectangle and the polyline points are assumed to be defined in a right-handed coordinate system 
    * (x-axis increases rightward, y-axis increases upward), rather than the left-handed coord system assumed by the 
    * implementation of <code>Rectangle2D</code>. A point on the edge of the clip rectangle is NOT outside it.</p>
    * 
    * @param polyline List of points defining a 2D polyline, in plotting order. Any point that is <code>null</code> or 
    * otherwise ill-defined is interpreted as introducing a "gap" in the polyline. Such gaps are left undisturbed. Upon 
    * return, this list may be altered to reflect the clipped version of the polyline. Clipping will introduce 
    * additional gaps in the polyline as it leaves and enters the clip rectangle.
    * @param rClip The clip rectangle. 
    * @param bridgeClipGaps If this flag is set, the point at which the polyline leaves the clip rectangle will be 
    * connected to the next point at which the polyline reenters the rectangle. This may yield some undesirable effects, 
    * but is necessary if you need to fill the polyline path.
    * @param doClipPts If this flag is set, the method generates a second point list containing ONLY those well-defined 
    * points in the original polyline that are also within the clip rectangle. 
    * @return <code>Null</code> unless the <code>doClipPts</code> flag is set, in which case it returns an independent 
    * copy of all well-defined polyline points that are not clipped.
    */
   public static List<Point2D> clipPolyline(
         List<Point2D> polyline, Rectangle2D rClip, boolean bridgeClipGaps, boolean doClipPts)
   {
      if(polyline == null || polyline.isEmpty() || rClip == null || rClip.isEmpty()) return(null);

      List<Point2D> insidePts = doClipPts ? new ArrayList<>() : null;

      // get first well-defined point along the polyline, removing any initial ill-defined points. If there are no 
      // well-defined points, we'll effectively empty the polyline point list!
      Point2D p0 = null;
      while(!polyline.isEmpty())
      {
         p0 = polyline.remove(0);
         if(Utilities.isWellDefined(p0)) 
            break;
      }
      if(!Utilities.isWellDefined(p0))
         return(insidePts);

      // as we process points in the polyline, we remove them from the front of the list. Points in the clipped polyline 
      // are appended to the end of the list.
      int nPts = polyline.size();
      for(int i = 0; i<nPts; i++)
      {
         Point2D p1 = polyline.remove(0);

         boolean isP0Defined = Utilities.isWellDefined(p0);
         boolean isP1Defined = Utilities.isWellDefined(p1);
         boolean isP0Inside = isP0Defined && rClip.contains(p0);
         boolean isP1Inside = isP1Defined && rClip.contains(p1);

         // if we're preparing the list of all pts inside clip rect and p0 is inside, add a clone of it to the list
         if(doClipPts && isP0Defined && rClip.contains(p0))
            insidePts.add((Point2D) p0.clone());

         // p0 is appended to the clipped polyline if it is inside the clip rectangle or if it is undefined and p1 is 
         // defined (we're starting a new subpath in the original polyline)
         if(isP0Inside || (isP1Defined && !isP0Defined))
            polyline.add(p0);

         // if p0 is inside and p1 is outside, polyline path leaves clip rect: Append the exit pt to clipped polyline, 
         // and unless we're bridging the gaps introduced by clipping, add an extra ill-defined point to create a new 
         // gap in polyline.
         if(isP0Inside && !isP1Inside)
         {
            Point2D pEdge = new Point2D.Double(p1.getX(), p1.getY());
            Utilities.clipLine(p0, pEdge, rClip);
            polyline.add(pEdge);
            if(!bridgeClipGaps) polyline.add( new Point2D.Double(Double.NaN, Double.NaN) );
         }

         // if p0 is outside and p1 is inside, polyline path is entering clip rect: Append the entry pt to clipped 
         // polyline.
         if(isP1Inside && !isP0Inside)
         {
            Point2D pEdge = new Point2D.Double(p0.getX(), p0.getY());
            Utilities.clipLine(pEdge, p1, rClip);
            polyline.add(pEdge);
         }

         p0 = p1;
      }

      // the endpoint of the polyline should now be in p0. We include it in the clipped polyline IF it is inside the 
      // clip rectangle.
      if(Utilities.isWellDefined(p0) && rClip.contains(p0))
      {
         if(doClipPts) insidePts.add((Point2D) p0.clone());
         polyline.add(p0);
      }

      return(insidePts);
   }
   
   /**
    * Finds the value of an enumerated data type that corresponds to the specified string.
    * 
    * @param s The value of an enumerated property in string form.
    * @param choices All possible choices for the enumerated property. The argument <i>s</i> is compared to the value
    * returned by {@link Object#toString()} to find the matching enumerant.
    * @return The particular value picked by the string argument. It will be null if the string argument is null or if 
    * there is no match in the choice array.
    */
   public static <T> T getEnumValueFromString(String s, T[] choices)
   {
      if(s == null) return(null);
      for(T choice : choices) 
         if(s.equals(choice.toString())) 
            return(choice);
      return(null);
   }
}
