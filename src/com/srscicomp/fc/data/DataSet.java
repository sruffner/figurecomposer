package com.srscicomp.fc.data;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// import com.srscicomp.common.util.Base64;
import com.srscicomp.common.util.NeverOccursException;
import com.srscicomp.common.util.Utilities;

/**
 * A single immutable data set in a <i>FigureComposer</i> (FC) figure.
 * 
 * <p><b>DataSet</b> encapsulates the data that is stored and presented in an FC figure. It supports the representation 
 * of 1D, 2D, and -- to a limited extent -- 3D and 4D data in one of several different formats.</p>
 * <ul>
 * <li>{@link Fmt#PTSET}: A 2D point set with optional standard deviation data. It may be thought of as a set of zero or
 * more "tuples" <i>{x y [yStd YE xStd XE]}</i>, representing a single point <i>(x,y)</i> with standard deviations 
 * <i>(xStd, yStd)</i>. <i>(XE,YE)</i> are codes that indicate how the x- and y-error bars should be drawn for for that 
 * point. An error bar code of 0 selects a two-sided (+/-1STD) error bar, 1 selects a +1STD bar, and 2 selects a -1STD 
 * bar; otherwise, the error bar is not drawn. Of course, an error bar is never drawn if the standard deviation is zero!
 * The only required members of a tuple are the coordinates of the point <i>(x,y)</i>. Thus, tuple length may vary 
 * between [2..6], but all tuples in a given point set instance will have the same length. NOTE: A tuple of length 3 or 
 * 4 corresponds to a data point with an error bar in <i>y</i> only. To get a data point with a horizontal error bar 
 * only, the tuple must have the form <i>x y 0 0 xStd XE</i>. Such usage is rare.</li>
 * 
 * <li>{@link Fmt#MSET}: A collection of 1+ (usually many) 2D point sets, all sharing the same x-coordinates. It may be
 * thought of as a set of one or more "tuples" of the form <i>{x y1 [y2 y3 ...]}</i>, where <i>(x,y1)</i> is a point in
 * the first point set, <i>(x,y2)</i> in the second set, and so on. All tuples will have the same length L, where L-1 is
 * the number of point sets in the collection. In typical usage, each individual point set represents a repeated measure
 * of the same stochastic phenomenon, so the variation in that phenomenon is captured in the collection. Hence, this 
 * data set format does not include standard deviation data.</li>
 * 
 * <li>{@link Fmt#SERIES}: A 2D data series sampled at regular intervals in x, with optional standard deviation data. It 
 * may be thought of as a set of zero or more "tuples" of the form <i>{y [yStd YE]}</i>, where <i>yStd</i> is the 
 * standard deviation in y at the N-th point <i>(x0+N*dx, yN)</i> and <i>YE</i> is the error bar display code -- as 
 * described for the <b>PTSET</b> format. The sample interval <i>dx</i> and the initial value <i>x0</i> are required 
 * parameters for this data set format. Tuple length can vary between 1 and 3, but all tuples in a given series instance
 * will have the same length.</li>
 * 
 * <li>{@link Fmt#MSERIES}: A collection of 1+ (usually many) 2D data series, all sampled at regular intervals in x. It
 * may be thought of as a set of zero or more "tuples" of the form <i>{y1 [y2 y3 ...]}</i>, where <i>(x0 + N*dx, 
 * y1[N])</i> is the (N+1)-th point in the first series, etc. All tuples will have the same length L, which is equal to 
 * the number of individual data series in the collection. As with the <b>MSET</b> format, each individual series 
 * typically represents a repeated measure of the same stochastic phenomenon, so that variation in that phenomenon is 
 * captured by the collection itself. The sample interval <i>dx</i> and the initial value <i>x0</i> are required 
 * parameters for this data set format.</li>
 * 
 * <li>{@link Fmt#RASTER1D}: A collection of <i>M</i> 1D rasters in <i>x</i>. In this format, the data array begins with
 * a list of <i>M</i> raster lengths, followed by each raster's samples: <i>{n1 n2 .. nM x1(1)ix1(2) .. x1(n1) x2(1) 
 * x2(2) .. x2(n2) ... xM(1) xM(2) .. xM(nM)}.</i> Obviously, the individual rasters can and are likely to have 
 * different lengths.</li>
 * 
 * <li>{@link Fmt#XYZIMG}: This data set format encapsulates a 3D data set in which one variable is a function of two
 * independent variables: <i>{x, y, z(x,y)}</i>. The "data" in this case is really a 2D matrix storing an "intensity" 
 * <i>z(x,y)</i> at each "pixel location" <i>(x,y)</i>, where <i>x=[1..W]</i> and <i>y=[1..H]</i>. The defining 
 * parameters are the width W and height H of the intensity image, the actual range <i>[x0..x1]</i> spanned by the data
 * in x, and the range <i>[y0..y1]</i> spanned in y.</li>
 * 
 * <li>{@link Fmt#XYZSET}: A set of points in 3D space: {x, y, z}. There are no additional parameters associated with 
 * this format.</li>
 * 
 * <li>{@link Fmt#XYZWSET}: A set of points in 4D space, {x, y, z, w}. No additional parameters.</li>
 * </ul>
 * 
 * Internally, <b>DataSet</b> uses a single-precision floating point array (the <i>float</i> primitive type) to store
 * the data. This decision limits the universe of data for which <b>DataSet</b> is appropriate. Also, note that "NaN" is
 * acceptable as a floating-point number. It is used to mark an ill-defined point in any of the data set formats.</p>
 * 
 * @author sruffner
 */
public class DataSet implements Cloneable
{
   /** Enumeration of supported data set formats. */
   public enum Fmt
   {
      /** A 2D point set with optional standard deviation data in both X and Y. */ PTSET(0, 2, 6, 0), 
      /** Collection of 2D point sets sharing a common X-coordinate vector. */ MSET(1, 2, Integer.MAX_VALUE, 0), 
      /** A 2D sampled data series with optional standard deviation data in Y. */ SERIES(2, 1, 3, 2), 
      /** Collection of 2D sampled series sharing a common X-coordinate vector. */ MSERIES(3, 1, Integer.MAX_VALUE, 2), 
      /** Collection of X-coordinate rasters (spike trains). */ RASTER1D(4, 1, Integer.MAX_VALUE, 0), 
      /** A 3D set in which Z(x,y) is sampled over an X-Y grid, like an image. */ XYZIMG(5, 1, Integer.MAX_VALUE, 4), 
      /** A 3D point set {(x,y,z)}. */ XYZSET(6, 3, 3, 0),
      /** A 4D point set {(x,y,z,w)}. */ XYZWSET(7, 4, 4, 0);
      
      /** The ID code for this data set format, for the purposes of serializing the data set to file. */
      private int ID;
      
      /** Minimum breadth, or tuple length, for this data format. */
      private int minTL;
      /** Maximum breadth, or tuple length, for this data format. */
      private int maxTL;
      /** Number of additional defining parameters for this data format. */
      private int nParams;
      
      Fmt(int id, int minTL, int maxTL, int nParams) 
      {
         this.ID = id; this.minTL = minTL; this.maxTL = maxTL; this.nParams = nParams;
      }
      
      /**
       * Get the integer code used to serialize this data set format to file.
       * @return The unique integer ID for this data format.
       */
      public int getIntCode() { return(ID); }
      
      @Override
      public String toString() { return(super.toString().toLowerCase()); }

      /**
       * Get the enumerated data format corresponding to the specified integer code
       * @param id The integer code for the data format.
       * @return The corresponding data format, or null if argument is invalid.
       */
      static public Fmt getFormatByIntCode(int id)
      {
         Fmt[] vals = Fmt.values();
         return((id>=0 && id<vals.length) ? vals[id] : null);
      }
      
      /**
       * Get the enumerated data format corresponding to the specified format name
       * @param name The name of the data format.
       * @return The corresponding data format, or null if argument is invalid.
       */
      static public Fmt getFormatByName(String name)
      {
         if(name == null) return(null);
         for(Fmt choice : Fmt.values()) if(choice.toString().equals(name))
            return(choice);
         return(null);
      }
      
      /**
       * Get the number of additional defining parameters associated with the specified data format.
       * @return Number of additional parameters. The {@link #SERIES} and {@link #MSERIES} formats have two parameters
       * specifying sample interval in X and its initial value X0. The {@link #XYZIMG} format has four parameters 
       * specifying the actual X- and Y-coordinate ranges spanned by the image data. All other formats have no 
       * additional defining parameters.
       */
      public int getNumberOfParams() { return(nParams); }
      
      /** 
       * Get the minimum data breadth, or "tuple length", for this data format.
       * @return The minimum data breadth.
       */
      public int getMinBreadth() { return(minTL); }
      /**
       * Get the maximum data breadth, or "tuple length", for this data format.
       * @return The maximum data breadth.
       */
      public int getMaxBreadth() { return(maxTL); }
      
      /**
       * Is the specified data set breadth valid for a <i>non-empty</i> data set? The breadth varies with the format:
       * 2-6 for {@link #PTSET}, 1-3 for {@link #SERIES}, exactly 3 for {@link #XYZSET} and 4 for {@link #XYZWSET}, 
       * 2+ for {@link #MSET}, and 1+ for all other recognized formats.
       * @param n The data set breadth to check. Should be strictly positive.
       * @return True if specified breadth is acceptable for a non-empty data set in this format.
       */
      public boolean isValidDataBreadth(int n) { return(n >= minTL && n <= maxTL); }
      
      /**
       * Range-restrict specified breadth to a valid value for a <i>non-empty</i> data set in this format.
       * @param n A data breadth to test.
       * @return Same value if it is already valid; else, the minimum or maximum allowed breadth, whichever's closer.
       */
      public int restrictToValidDataBreadth(int n) { return( Math.min( Math.max(minTL, n), maxTL) ); }
      
      /**
       * Does this data set format represent a collection of individual data set members?
       * @return True only if this is a {@link #MSET}, {@link #MSERIES}, or {@link #RASTER1D} format.
       */
      public boolean isCollection() { return(this==MSET || this==MSERIES || this==RASTER1D); }
      
      /** Is this a 2D data set format? */
      public boolean is2D() { return(this==PTSET || this==SERIES || this==MSET || this==MSERIES); }
   }
   
   /**
    * A compiled regular expression that is used to validate candidate data set IDs: they must have at least one 
    * printable ASCII character that is an alphanumeric character or one of <em>$@|.<>_[](){}+-^!=</em>.
    */
   private static Pattern idVerifier = 
      Pattern.compile("[a-zA-Z0-9_=\\$\\@\\|\\.\\<\\>\\[\\]\\(\\)\\{\\}\\+\\-\\^\\!]+");
   
   /** Used to clean illegal characters from a candidate dataset ID.*/
   private static Matcher idCleaner = 
      Pattern.compile("[^a-zA-Z0-9_=\\$\\@\\|\\.\\<\\>\\[\\]\\(\\)\\{\\}\\+\\-\\^\\!]+").matcher(" ");
   
   /** The maximum length of a valid data set identifier string. */
   public final static int MAXIDLEN = 40;
   
   /**
    * Does the specified string qualify as the identifier for a <b>DataSet</b>? By convention, a valid ID must be a 
    * non-empty string that can be up to 40 characters long and that contains only ASCII alphanumeric characters or one 
    * of <b>$@|.<>_[](){}+-^!=</b>. Note that the ID cannot have any whitespace.
    * @param s The string to test.
    * @return True if string meets criteria described above.
    */
   public static boolean isValidIDString(String s) 
   { 
      return((s != null) && (s.length() <= MAXIDLEN) && idVerifier.matcher(s).matches()); 
   }

   /**
    * Modify the ID of the specified data set to ensure it does not match the ID of any data set in the specified list.
    * @param ds The data set under consideration.
    * @param compareList The list of data sets to which it is compared. These data sets are left unchanged, even if 
    * there are duplicate IDs among them.
    * @return If either argument is null, if the comparison list is empty, or if the data set already has a unique 
    * identifier, it is returned unchanged. Otherwise, {@link #changeID()} is called to generate a new <b>DataSet</b> 
    * instance that is identical to <b>ds</b> but has a new ID that is unique among the sets in the comparison list. The
    * new ID is derived from the original ID, with an integer string appended to make it unique.
    */
   public static DataSet ensureUniqueIdentifier(DataSet ds, List<DataSet> compareList)
   {
      if(ds == null || compareList == null || compareList.size() == 0) return(ds);
      
      HashMap<String, Object> idmap = new HashMap<String, Object>();
      for(DataSet set : compareList) 
         if(set != null) 
            idmap.put(set.getID(), null);
      
      String origID = ds.getID();
      if(!idmap.containsKey(origID)) return(ds);
      
      String adjID = origID;
      int origLen = origID.length();
      int n = 0;
      do
      {
         ++n;
         String nStr = Integer.toString(n);
         int adjLen = nStr.length();
         if(origLen + adjLen <= MAXIDLEN) adjID = origID + nStr;
         else adjID = origID.substring(0, MAXIDLEN-adjLen) + nStr;
      }
      while(idmap.containsKey(adjID));
      
      return(ds.changeID(adjID));
   }
   
   /**
    * Helper method ensures unique IDs among a collection of data sets represented by an array of {@link DataSetInfo}
    * objects. If a duplicate ID is found, the method appends an integer digit (or digits) such that the modified ID is 
    * unique.
    * 
    * @param dsInfo Array of {@link DataSetInfo} objects with summary information on the data sets in question, 
    * including the data set identifier. If any duplicates are found, the elements in this array are modified <i>in 
    * place</i> as needed.
    * @return True if all IDs were unique; false if any duplicate IDs were found and modified.
    */
   public static boolean ensureUniqueIdentifiers(DataSetInfo[] dsInfo)
   {
      if(dsInfo == null || dsInfo.length == 0) return(true);
      
      boolean unchanged = true;
      HashMap<String, Object> idmap = new HashMap<String, Object>();
      for(int i=0; i<dsInfo.length; i++)
      {
         String origID = dsInfo[i].getID();
         if(!idmap.containsKey(origID))
         {
            idmap.put(origID, null);
            continue;
         }

         // need to alter current ID to ensure uniqueness
         unchanged = false;
         String adjID = origID;
         int origLen = origID.length();
         int n = 0;
         do
         {
            ++n;
            String nStr = Integer.toString(n);
            int adjLen = nStr.length();
            if(origLen + adjLen <= MAXIDLEN) adjID = origID + nStr;
            else adjID = origID.substring(0, MAXIDLEN-adjLen) + nStr;
         }
         while(idmap.containsKey(adjID));
         
         idmap.put(adjID, null);
         dsInfo[i] = DataSetInfo.changeID(dsInfo[i], adjID);
      }
      
      return(unchanged);
   }
   
   /**
    * Modify the candidate data set ID if necessary to ensure that it meets the requirements for a valid data set 
    * identifier.
    * @param id The candidate data set ID.
    * @return If the candidate ID is valid, it is returned unchanged. If it is too long, it is truncated to the maximum 
    * allowed length. If it contains any illegal characters, those are removed. After these corrections, if the ID has 
    * zero length, "set" is returned.
    * @see #isValidIDString(String)
    */
   public static String ensureValidIdentifier(String id)
   {
      if(isValidIDString(id)) return(id);
      if(id == null || id.length() == 0) return("set");
      id = idCleaner.reset(id).replaceAll("");
      if(id.length() == 0) return("set");
      if(id.length() > MAXIDLEN) id = id.substring(0, MAXIDLEN);
      return(id);
   }
   
   /**
    * Modify the candidate data set ID if necessary to ensure that it is both a valid data set identifier and unique 
    * among the specified set of data sets.
    * @param id The candidate data set ID.
    * @param other An array of {@link DataSetInfo} objects containing information about data sets, including IDs. The 
    * candidate ID cannot duplicate the ID of any data set represented in this array.
    * @return If the candidate ID is already valid and unique, it is returned unchanged. Otherwise, a corrected ID
    * is returned.
    * @see #ensureValidIdentifier(String)
    */
   public static String ensureValidAndUniqueIdentifier(String id, DataSetInfo[] other)
   {
      id = ensureValidIdentifier(id);
      if(other == null || other.length == 0) return(id);
       
      HashMap<String, Object> forbidden = new HashMap<String, Object>();
      for(DataSetInfo info : other) { if(info != null) forbidden.put(info.getID(), null); }
      
      if(!forbidden.containsKey(id)) return(id);
      
      String adjID = id;
      int origLen = id.length();
      int n = 0;
      do
      {
         ++n;
         String nStr = Integer.toString(n);
         int adjLen = nStr.length();
         if(origLen + adjLen <= MAXIDLEN) adjID = id + nStr;
         else adjID = id.substring(0, MAXIDLEN-adjLen) + nStr;
      }
      while(forbidden.containsKey(adjID));
      
      return(adjID);
   }
   
   
   /**
    * Construct a data set object.
    * 
    * @param id The data set identifier. It must pass the requirements enforced by {@link #isValidIDString()}.
    * @param fmt The data format. Cannot be null.
    * @param params Additional parameters. For the {@link Fmt#XYZIMG} format, this array must contain at least 4 
    * elements, <i>[x0 x1 y0 y1]</i>, defining the x- and y-coordinate ranges spanned by the data. For the two data 
    * series formats, the array must contain at least two elements, <i>[dx x0]</i>, defining the sample interval and 
    * initial value in x, which define the data series' x-coordinate vector. Otherwise, this argument is ignored.
    * @param nrows Number of rows in the data. Its meaning depends on the data format. For {@link Fmt#RASTER1D}, it is 
    * the total number of raster samples available. For {@link Fmt#XYZIMG}, it is the height of the data, ie, the 
    * granularity in y. For all other formats, it is the number of tuples in the data set. Cannot be negative.
    * @param ncols The number of columns in the data. Its meaning depends on the data format. For {@link Fmt#RASTER1D}-
    * formatted data, this is the number of individual rasters available. For {@link Fmt#XYZIMG}, it is the width of the
    * data, ie, the granularity in x. For all other formats, it is the length of each tuple (all tuples have the same
    * length; see class header for a detailed description of what's in each tuple and what are valid tuple lengths for 
    * each format. Cannot be negative. If <b>nrow != 0</b>, <b>ncols</b> must be a valid tuple length.
    * @param fData The raw data array, arranged IAW the specified data format, as described below:
    * <ul>
    *    <li><b>PTSET, MSET, SERIES, MSERIES, XYZSET, XYZWSET</b>. The data array is a set of N M-tuples, where N is the
    *    number of data points and M is the tuple length. See class header for a full discussion of the tuple content 
    *    and allowed values of M for each of these formats. Tuples are placed one after another in the array, which must
    *    have a total length of N*M.</li>
    *    <li><b>RASTER1D</b>. The data array starts with a list of individual raster lengths followed by the raster
    *    samples themselves: <i>{n1 n2 .. nM x1(1..n1) x2(1..n2) ... xM(1..nM)}</i>. The total length of the array is
    *    (M+N), where M is the number of individual rasters in the collection and N is the total number of samples 
    *    across all rasters. It must be true that <i>N = n1 + n2 + .. + nM</i>, or a data set is not created. Also, if 
    *    N is positive, M cannot be zero. However, N could be zero with M positive -- ie, a collection of M empty 
    *    rasters.</li>
    *    <li><b>XYZIMG</b>. The data array in this case holds samples of the intensity image Z(x,y), stored row-wise. 
    *    The array length is N*M, where N is the number of rows in the image (or the granularity in y) and M is the 
    *    number of columns (granularity in x). If N or M is zero, the image is empty.</li>
    * </ul>
    * <b>NOTE that the array reference is stored internally rather than making an independent copy -- so it is 
    * important that the array not be changed externally!</b>
    * @return A new <b>DataSet</b> instance wrapping the raw data and other defining information. Returns null if any 
    * argument is invalid, if the data array length is not consistent with the specified dimensions, or if the array 
    * contents are otherwise inconsistent with the specified format.
    */
   public static DataSet createDataSet(String id, Fmt fmt, float[] params, int nrows, int ncols, float[] fData)
   {
      DataSetInfo info = DataSetInfo.createDataSetInfo(id, fmt, nrows, ncols, params);
      return(DataSet.createDataSet(info, fData));
   }
   
   /**
    * Construct a data set object from a double-precision floating-point array. The method simply converts the double-
    * precision array to single-precision and calls {@link #createDataSet(String, Fmt, float[], int, int, float[])}.
    * 
    * @param id
    * @param fmt
    * @param params 
    * @param nrows 
    * @param ncols
    * @param dData The raw data in double-precision format. Cannot be null. Length L &ge; N*M for most data formats, 
    * L &ge; N+M for {@link Fmt#RASTER1D}. Additional elements are ignored.
    */
   public static DataSet createDataSet(String id, Fmt fmt, float[] params, int nrows, int ncols, double[] dData)
   {
      DataSetInfo info = DataSetInfo.createDataSetInfo(id, fmt, nrows, ncols, params);
      if(info == null || dData == null || dData.length < info.getDataArraySize()) return(null);
      
      float[] fData = new float[info.getDataArraySize()];
      for(int i=0; i<fData.length; i++)
      {
         fData[i] = Utilities.isWellDefined(dData[i]) ? ((float) dData[i]) : Float.NaN;
      }
      return(DataSet.createDataSet(info, fData));
   }
   
   /**
    * Construct a data set object.
    * @param info Data set information other than the raw data array.
    * @param fData The raw data array, as described in {@link #createDataSet(String, Fmt, float[], int, int, float[])}.
    * @return A data set object wrapping the raw data and accompanying information. Returns null if either argument is 
    * null, if the data array length does not match the dimensions provided in the <b>info</b> argument, or if the data 
    * array contents are otherwise inconsistent with the specified format.
    */
   public static DataSet createDataSet(DataSetInfo info, float[] fData)
   {
      return(DataSet.createDataSet(info, fData, null));
   }
   
   /**
    * Construct a data set object. This version includes a means of specifying the coordinate range spanned by the raw 
    * data along the X, Y, and Z axes as applicable. This information is normally calculated from the raw data during 
    * construction, but that computation can be bypassed if the information is already known -- a possible time-saver 
    * for very large data sets.
    * 
    * @param info Data set information other than the raw data array.
    * @param fData The raw data array, as described in {@link #createDataSet(String, Fmt, float[], int, int, float[])}.
    * @param ranges If this is a non-null array of length 9, it is assumed to define the coordinate range information 
    * that would otherwise be calculated by this constructor. <i>[x0 x1 y0 y1 z0 z1 a b hasError]</i>. The first six
    * elements are self-explanatory. If <i>hasError &gt; 0</i>, then the raw data set includes standard deviation/error
    * data (2D data sets only). For the {@link Fmt#PTSET} and {@link Fmt#SERIES} formats, <i>a=y0_noStd, b=y1_noStd</i> 
    * -- indicating the y-coordinate range without taking into account any standard-deviation data in the set. For the 
    * {@link Fmt#XYZWSET} format, <i>a=w0, b=w1</i> -- i.e., the coordinate range for the fourth data dimension "w". For
    * all other formats, <i>(a,b)</i> are ignored. If the array is null, if its length is not 9 exactly, or if any 
    * coordinate range is invalid (ill-defined, or min &gt; max), the argument is ignored and the coordinate range 
    * information is calculated from scratch. 
    * @return A data set object wrapping the raw data and accompanying information. Returns null if either argument is 
    * null, if the data array length does not match the dimensions provided in the <b>info</b> argument, or if the data 
    * array contents are otherwise inconsistent with the specified format.
    */
   public static DataSet createDataSet(DataSetInfo info, float[] fData, float[] ranges)
   {
      // check for reasonable arguments
      if(info == null || fData == null) return(null);
      Fmt fmt = info.getFormat();
      int nLen = info.getDataLength();
      int nBreadth = info.getDataBreadth();
      int n = (fmt == Fmt.RASTER1D) ? (nLen + nBreadth) : (nLen * nBreadth);
      if(fData.length != n) return(null);
      
      // for RASTER1D, verify that the sum of the individual raster lengths = total number of samples
      if(fmt == Fmt.RASTER1D && nBreadth > 0)
      {
         int nSamp = 0;
         for(int i=0; i<nBreadth; i++) nSamp += (int) fData[i];
         if(nSamp != nLen) return(null);
      }
      
      DataSet ds = new DataSet();
      ds.id = info.getID();
      ds.format = fmt;
      ds.width = nBreadth;
      ds.height = nLen;
      ds.fData = (fData.length > 0) ? fData : new float[0];
     
      if(fmt == Fmt.SERIES || fmt == Fmt.MSERIES)
      {
         ds.dx = info.getParam(0);
         ds.x0 = info.getParam(1);
      }
      
      if(ranges != null && ranges.length == 9 && ranges[0] <= ranges[1] && ranges[2] <= ranges[3] &&
            ranges[4] <= ranges[5] && ranges[6] <= ranges[7])
      {
         ds.hasErrorData = ranges[8] > 0;
         ds.coordRanges = new float[8];
         for(int i=0; i<8; i++) ds.coordRanges[i] = ranges[i];
      }
      else
         ds.computeStats();

      if(fmt == Fmt.XYZIMG)
         for(int i=0; i<4; i++) ds.coordRanges[i] = info.getParam(i);

      return(ds);
   }
   
   /**
    * Create an empty data set conforming to the specified data format, with a default generic ID of "set" and default 
    * parameter values (depending on the format).
    * @param fmt The data format.
    * @return An empty set in the specified format.
    */
   public static DataSet createEmptySet(Fmt fmt) { return(createEmptySet(fmt, null, null)); }
   
   /**
    * Create an empty data set conforming to the specified data format and having the specified ID. If the format has 
    * additional defining parameters, standard default values are supplied for those parameters.
    * @param fmt The data format.
    * @param id The ID to be assigned. If null or otherwise invalid, a default ID string ("set") is supplied instead.
    * @return An empty data set in the specified format with the specified ID.
    */
   public static DataSet createEmptySet(Fmt fmt, String id) { return(createEmptySet(fmt, id, null)); }
   
   /**
    * Create an empty data set conforming to the specified data format, with the specified ID and additional defining 
    * parameters.
    * @param fmt The data format.
    * @param id The ID to be assigned. If null or otherwise invalid, a default ID string ("set") is supplied instead.
    * @param params Additional defining parameters. For {@link Fmt#SERIES} or {@link Fmt#MSERIES}, the first element 
    * should be the sample interval <i>dx!=0</i> and the second is the initial value x0. For {@link Fmt#XYZIMG}, the 
    * array should have four elements indicating the x- and y-coordinate ranges spanned by the data set: <i>[x0 x1 y0 
    * y1]</i>, where <i>x0&lt;x1, y0&lt;y1</i>. Otherwise ignored. If supplied array is null or otherwise contains invalid 
    * parameter values, standard defaults are used instead: <i>[dx x0] = [1 0], [x0 x1 y0 y1] = [0 10 0 10]</i>.
    * @return An empty data set in the specified format, with specified ID and additional parameters as described.
    */
   public static DataSet createEmptySet(Fmt fmt, String id, float[] params)
   {
      if(fmt == null) throw new IllegalArgumentException("Data format not specified!");
      DataSet ds = new DataSet();
      ds.id = isValidIDString(id) ? id : "set";
      ds.format = fmt;
      ds.width = 0;
      ds.height = 0;
      ds.fData = new float[0];
      
      if(fmt == Fmt.SERIES || fmt == Fmt.MSERIES)
      {
         ds.dx = 1;
         ds.x0 = 0;
         if(Utilities.isWellDefined(params) && (params.length >= 2) && (params[0] != 0))
         {
            ds.dx = params[0];
            ds.x0 = params[1];
         }
      }
      
      ds.computeStats();
      
      if(fmt == Fmt.XYZIMG)
      {
         ds.coordRanges[0] = 0;
         ds.coordRanges[1] = 10;
         ds.coordRanges[2] = 0;
         ds.coordRanges[3] = 10;
         
         if(Utilities.isWellDefined(params) && (params.length>=4) && (params[0]<params[1]) && (params[2]<params[3]))
         {
            for(int i=0; i<4; i++) ds.coordRanges[i] = params[i];
         }
      }

      return(ds);
   }
   
   /**
    * While a data set is an immutable object, the method {@link #changeID()} will generate another data set instance 
    * that is identical to the source instance except for the ID string. Also, it is possible to create multiple 
    * instances backed by the same raw data array. This method is used to check whether or not two immutable data set
    * instances are identical; the ID string may be excluded in the comparison.
    * 
    * @param ds1 The first data set.
    * @param ds2 The second data set.
    * @param ignoreID If true, the data set identifiers are ignored when making the comparison.
    * @return If either argument is null, method returns false; else if <b>ds1==ds2</b>, method returns true. Otherwise,
    * the method returns true only if the two data sets have the same definition and <b>share a reference to the same 
    * raw data array</b>. Note that two data sets which are backed by the same raw data array but have different 
    * additional defining parameters (if the format requires any) are NOT identical.
    */
   public static boolean areIdenticalSets(DataSet ds1, DataSet ds2, boolean ignoreID)
   {
      if(ds1 == null || ds2 == null) return(false);
      if(ds1 == ds2) return(true);
      boolean same = (ignoreID) ? true : ds1.id.equals(ds2.id);
      if(same) same = (ds1.format == ds2.format) && (ds1.width == ds2.width) && (ds1.height == ds2.height);
      if(same && (ds1.format == Fmt.SERIES || ds1.format == Fmt.MSERIES))
         same = (ds1.dx == ds2.dx) && (ds1.x0 == ds2.x0);
      if(same && ds1.format == Fmt.XYZIMG)
      {
         // here we're ensuring that [x0 x1 y0 y1] is the same for both XYZIMG sets
         same = (ds1.coordRanges == ds2.coordRanges);
         if(!same)
         {
            same = true;
            for(int i=0; same && i<4; i++) same = (ds1.coordRanges[i] == ds2.coordRanges[i]);
         }
      }
      if(same) same = (ds1.isEmpty() && ds2.isEmpty()) || (ds1.fData == ds2.fData);
      
      return(same);
   }
   
   /** This override simply calls {@link #equals(Object, boolean)} and includes the data set IDs in the comparison. */
   @Override public boolean equals(Object obj) { return(equals(obj, false)); }

   /** This override simply calls {@link #hashCode(boolean)} and includes the ID string in the hash calculation. */
   @Override public int hashCode() { return(hashCode(false)); }

   /**
    * Test equality with another data set object, optionally excluding the ID string in the comparison.
    * @param obj The other data set object.
    * @param ignoreID If true, the data set IDs are ignored in making the comparison. <i>Calling this method with 
    * <b>ignoreID==false</b> is equivalent to calling {@link #equals(Object)}</i>.
    * @return True if specified object is an instance of <b>DataSet</b> and: (1) contains a reference to the 
    * same raw data array, or has a data array with the same length and content as this one; (2) has the same data
    * format and size as this data set; (3) has the same values for any additional data set parameters; and (4) if
    * <b>ignoreID==false</b>, has the same ID string.
    */
   public boolean equals(Object obj, boolean ignoreID)
   {
      if(obj == null || !obj.getClass().equals(DataSet.class)) return(false);
      
      DataSet other = (DataSet) obj;
      if(other == this) return(true);
      
      if(format != other.format || width != other.width || height != other.height) return(false);
      if(format == Fmt.SERIES || format == Fmt.MSERIES)
      {
         if(dx != other.dx || x0 != other.x0) return(false);
      }
      else if(format == Fmt.XYZIMG)
      {
         for(int i=0; i<4; i++) if(coordRanges[i] != other.coordRanges[i]) return(false);
      }
      if(!ignoreID)
      { 
         if(!id.equals(other.id)) return(false);
      }
      if(!Arrays.equals(fData, other.fData)) return(false);
      
      return(true);
   }
   
   /** 
    * Compute the 32-bit integer hash code for this data set, optionally excluding the ID string from the computation. 
    * The implementation relies on {@link Arrays#hashCode(float[])} to compute the hash for the raw data array itself, 
    * {@link String#hashCode()} to compute the hash code for the ID, combining these with the other parameters of the 
    * data set's definition to form the hash code of the data set object itself. 
    * 
    * @param ignoreID If true, the data set ID string is omitted from the hash code calculation. <i>Calling this
    * method with <i>ignoreID==false</i> is equivalent to calling {@link #hashCode()}</i>.
    * @return The hash code for the data set, as described.
    */
   public int hashCode(boolean ignoreID)
   {
      int res = Arrays.hashCode(fData);
      res = res * 31 + width;
      res = res * 31 + height;
      res = res * 31 + format.getIntCode();
      if(format == Fmt.SERIES || format == Fmt.MSERIES)
      {
         res = res * 31 + Float.floatToIntBits(dx);
         res = res * 31 + Float.floatToIntBits(x0);
      }
      else if(format == Fmt.XYZIMG)
      {
         for(int i=0; i<4; i++) res = res * 31 + Float.floatToIntBits(coordRanges[i]);
      }
      
      if(!ignoreID) res = res * 31 + id.hashCode();
      
      return(res);
   }
   
   
   // 
   // Support for plain-text editing of DataSet
   //
   
   /**
    * Get the internal raw data array of this data set as formatted plain text.
    * 
    * <p>The plain-text form of the data, as generated by this method, varies with the data format:</p>
    * <ul>
    *    <li><b>PTSET, SERIES, MSET, MSERIES, XYZIMG, XYZSET, XYZWSET</b>. A list of N M-tuples, where N is the data 
    *    length and M is the data breadth. Each M-tuple is a list of M whitespace-separated numeric tokens. Tuples are 
    *    separated by carriage-return-linefeed (CRLF) pairs.</li>
    *    <li><b>RASTER1D</b>. A list of M CRLF-separated raster tuples, where M is the data breadth. Each raster tuple
    *    is a whitespace-separated list of numeric tokens representing the actual raster samples. Unlike in the other
    *    data formats, the tuples need not (and typically will not) have the same length. It is possible to have an
    *    empty tuple, representing a zero-length raster! The total number of numeric tokens in the text content should
    *    be the data length N.</li>
    * </ul>
    * 
    * <p><b>Note that floating-pt values are written with up to 7 significant digits -- which is the maximum for 
    * single-precision floating-point. Very small and very large values will be presented in exponential notation.</b> 
    * See {@link Utilities#toString(double, int, int)}.</p>
    * 
    * <p>This method and {@link #fromPlainText()} exist to support manual editing of a data set's raw data in a GUI
    * text panel. Obviously, this is only practical for relatively small data sets. The simple formatted text 
    * input/output will prove unwieldy for larger data sets!</p>
    * 
    * @return This dataset's internal data array in text form, as described. Note that the set ID, format code, and any
    * additional defining parameters are NOT included in the text.
    */
   public String toPlainText()
   {
      return(toPlainText(0, format==Fmt.RASTER1D ? width : height, 7));
   }
   
   /**
    * Get the internal raw data array of this data set as formatted plain text, ensuring each floating-point number
    * is rounded to the specified number of significant digits. See {@link #toPlainText()} for more details.
    * 
    * @param nSig The maximum number of significant digits displayed for each floating-point value. Range: [1..7].
    * @return This dataset's internal data array in text form, as described. Note that the set ID, format code, and any
    * additional defining parameters are NOT included in the text.
    */
   public String toPlainText(int nSig)
   {
      return(toPlainText(0, format==Fmt.RASTER1D ? width : height, nSig));
   }
   
   /**
    * Extract a contiguous range of datum tuples from the internal raw data array of this data set object, formatted
    * as plain text. Each tuple is represented as a string of whitespace-separated floating-point numeric tokens, and 
    * tuples are separated by a carriage return line-feed pair ("\r\n"). See {@link #toPlainText()} for more details.
    * 
    * @param start Index of the first tuple in the range.
    * @param n The number of tuples in the contiguous range.
    * @param nSig The maximum number of significant digits displayed for each floating-point value. Range: [1..7].
    * @return The specified range of datum tuples in string form, as described.
    */
   public String toPlainText(int start, int n, int nSig)
   {
      int nTuples = (format == Fmt.RASTER1D) ? width : height;
      if(start < 0 || start >= nTuples || start+n > nTuples)
         throw new IllegalArgumentException("Requested tuple range is out of bounds!");
      
      StringBuffer buf = new StringBuffer(2000);
      if(format == Fmt.RASTER1D) 
      {
         int k = width;
         for(int i=0; i<start; i++) k += (int) fData[i];
         for(int i=start; i<start+n; i++)
         {
            int nSamples = (int) fData[i];
            for(int j=0; j<nSamples; j++)
               buf.append(Utilities.toString(fData[k+j], nSig, -1) + " ");
            k += nSamples;
            if(i < start+n-1) buf.append("\r\n");
         }
      }
      else
      {
         int first = start*width;
         int last = first + n*width - 1;
         for(int i=first; i<=last; i++)
         {
            if(i > first && (i % width == 0)) buf.append("\r\n");
            buf.append(Utilities.toString(fData[i], nSig, -1) + " ");
         }
      }
      return(buf.toString());
   }
   
   /**
    * Replace this data set's internal raw data array by parsing numeric data from a plain-text string. Since the
    * <b>DataSet</b> object is immutable, the method generates a new <b>DataSet</b> instance with the same ID, format,
    * and additional parameters as this instance, but with a new internal data array. The formatted numeric text data 
    * must satisfy the specifications described for the inverse method {@link #toPlainText()}, with the following 
    * exceptions:
    * <ul>
    *    <li><b>PTSET</b> format. Every tuple must have a length of 2 or greater. The data set created will have a data
    *     breadth equal to <i>min(6, maximum observed tuple length)</i>. Any "extra values" are discarded, and any 
    *    "missing values" are set to zero.</li>.
    *    <li><b>SERIES</b>. Every tuple must have a length of 1 or greater. The data set created will have a data 
    *    breadth equal to <i>min(3, maximum observed tuple length)</i>. Any "extra values" are discarded, and any 
    *    "missing values" are set to zero.</li>.
    *    <li><b>MSET</b>. Every tuple must have a length of 2 or greater. The data set created will have a data breadth
    *    equal to the minimum observed tuple length. Any "extra values" are discarded.</li>.
    *    <li><b>MSERIES</b>. Every tuple must have a length of 1 or greater. The data set created will have a data 
    *    breadth equal to the minimum observed tuple length. Any "extra values" are discarded.</li>.
    *    <li><b>XYZSET</b>. Every tuple must have a length of 3 or greater. Since the data breadth is always 3 for this
    *    format, the data set created will have a data breadth of 3, and all "extra values" are discarded.</li>
    *    <li><b>XYZWSET</b>. Every tuple must have a length of 4 or greater. Since the data breadth is always 4 for this
    *    format, the data set created will have a data breadth of 4, and all "extra values" are discarded.</li>
    * </ul>
    * @param text The raw data in text form, as described.
    * @param errMsg If a parsing error occurs, this will contain a short explanation. Else, it will be empty.
    * @return If successful, a new data set object defined IAW the arguments; else, null.
    */
   public DataSet fromPlainText(String text, StringBuffer errMsg)
   {
      // reset error message 
      errMsg.delete(0, errMsg.length());
      
      // special case: an empty dataset
      if(text == null || text.length() == 0) 
         return(DataSet.createEmptySet(format, id, getParams()));

      // parse the tuples. If a parsing error occurs, abort.
      StringTokenizer st = new StringTokenizer(text, "\n");
      int nTuples = st.countTokens();
      ArrayList<float[]> tuples = new ArrayList<float[]>(nTuples);
      int minLen = Integer.MAX_VALUE;
      int maxLen = Integer.MIN_VALUE;
      int lineNo = 1;
      try
      {
         while(st.hasMoreTokens())
         {
            StringTokenizer tupleTokenizer = new StringTokenizer(st.nextToken().trim());
            int n = tupleTokenizer.countTokens();
            if(n < minLen) minLen = n;
            if(n > maxLen) maxLen = n;
            
            float[] tuple = new float[n];
            n = 0;
            while(tupleTokenizer.hasMoreTokens()) 
               tuple[n++] = Float.parseFloat(tupleTokenizer.nextToken());
            tuples.add(tuple);
            ++lineNo;
         }
      } 
      catch(Throwable t) 
      { 
         errMsg.append("Illegal character or token on line " + lineNo);
         return(null); 
      }

      // check tuple lengths IAW expected data format
      if(format == Fmt.XYZIMG && minLen != maxLen)
      {
         errMsg.append("For XYZIMG data, each line of text must have same number of tokens!");
         return(null);
      }
      if((format == Fmt.PTSET || format == Fmt.MSET) && minLen < 2)
      {
         errMsg.append("For PTSET or MSET data, each line of text must have at least 2 numeric tokens!");
         return(null);
      }
      if((format == Fmt.SERIES || format == Fmt.MSERIES) && minLen < 1)
      {
         errMsg.append("For SERIES or MSERIES data, each line of text must have at least 1 numeric token!");
         return(null);
      }
      if((format == Fmt.XYZSET) && minLen < 3)
      {
         errMsg.append("For XYZSET data, each line of text must have at least 3 numeric tokens!");
         return(null);
      }
      if((format == Fmt.XYZWSET) && minLen < 4)
      {
         errMsg.append("For XYZWSET data, each line of text must have at least 4 numeric tokens!");
         return(null);
      }
      
      // load the tuples into a 1D array in the proper order for the specified format. For the original 2D formats and
      // for XYZSET and XYZWSET, we discard extra tuple values and/or set missing tuple values to zero. RASTER1D is a 
      // special case.
      int nrows;
      int ncols;
      float[] fData;
      if(format == Fmt.RASTER1D)
      {
         ncols = tuples.size();
         nrows = 0;
         for(int i=0; i<ncols; i++) nrows += tuples.get(i).length;
         
         fData = new float[ncols+nrows];
         int k = ncols;
         for(int i=0; i<ncols; i++)
         {
            float[] tuple = tuples.get(i);
            fData[i] = tuple.length;
            for(int j=0; j<tuple.length; j++) fData[k++] = tuple[j];
         }
      }
      else
      {
         nrows = tuples.size();
         ncols = (format == Fmt.PTSET || format == Fmt.SERIES) ? Math.min((format==Fmt.PTSET ? 6 : 3), maxLen) : minLen;
         if(format == Fmt.XYZSET) ncols = 3;
         if(format == Fmt.XYZWSET) ncols = 4;
         
         fData = new float[nrows*ncols];
         int k = 0;
         for(int i=0; i<nrows; i++)
         {
            float[] tuple = tuples.get(i);
            int j = 0;
            while(j < tuple.length && j < ncols) fData[k++] = tuple[j++];
            while(j < ncols) { fData[k++] = 0; j++; }
         }
      }

      return(DataSet.createDataSet(id, format, getParams(), nrows, ncols, fData));      
   }
   
   //
   // Support for plain-text serialization of DataSet
   //
   
   /**
    * Create and initialize a data set object from the now deprecated <i>FigureComposer</i> plain-text storage format: 
    * a comma-separated list of whitespace-separated floating-point string tokens.
    * <p><i>Background</i>. In <i>FigureComposer</i>'s predecessor, <i>Phyplot</i>, data sets were saved in the XML 
    * figure model file in plain-text form as a list of comma-separated "tuples", where each tuple was a whitespace-
    * separated list of numeric tokens (eg, "21.683 35 2.4478 ..."). The four data set formats recognized then are a 
    * subset of the data formats supported now. The expected tuple lengths varied with the format: <b>PTSET</b>, 2-6 
    * tokens per tuple (excess tokens simply ignored); <b>SERIES</b>, 1-3 (any additional tokens ignored); <b>MSET</b>, 
    * 2+ tokens (tokens beyond the minimum observed tuple length were ignored); and <b>MSERIES</b>, 1+ tokens (excess 
    * tokens again ignored). This method handles the task of parsing a text string in this format and creating a data 
    * set object encapsulating the raw data. It is heavily used when reading in old <i>Phyplot</i> XML figure files.</p>
    * 
    * @param id The identifier to be assigned to the new data set. It must satisfy {@link #isValidIDString()}.
    * @param fmt The data set format. This can only be one of the four data set formats that were available in 
    * <i>Phyplot</i>: <b>PTSET, MSET, SERIES, MSERIES</b>.
    * @param dx The sample interval in X for a data series. Ignored if not one of the series data formats.
    * @param text String holds the data in the form described above.
    * @return A data set object encapsulating the data and other parameters specified. If the identifier or data format 
    * is invalid, or if parsing of the text string fails, method returns null.
    */
   public static DataSet fromCommaSeparatedTuples(String id, Fmt fmt, float dx, String text)
   {
      // if ID is invalid or format unspecified, we cannot proceed. Also, the newer data formats did not exist when data
      // was saved as comma-separated tuples.
      if(!(DataSet.isValidIDString(id) && fmt != null)) return(null);
      if(!(fmt==Fmt.PTSET || fmt==Fmt.SERIES || fmt==Fmt.MSET || fmt==Fmt.MSERIES)) return(null);
      
      // special case: an empty dataset
      if(text == null || text.length() == 0) 
      {
         float[] params = null;
         if(fmt == Fmt.SERIES || fmt == Fmt.MSERIES)
         {
            params = new float[2];
            params[0] = (Utilities.isWellDefined(dx) && dx != 0) ? dx : 1;
            params[1] = 0;
         }
         return(createEmptySet(fmt, id, params));
      }

      // parse the tuples. If a parsing error occurs, abort.
      StringTokenizer st = new StringTokenizer(text, ",");
      int nSamp = st.countTokens();
      ArrayList<float[]> tuples = new ArrayList<float[]>(nSamp);
      int minLen = Integer.MAX_VALUE;
      int maxLen = Integer.MIN_VALUE;
      try
      {
         while(st.hasMoreTokens())
         {
            // 29apr2014: Allow for possibility that text content ENDS with a comma followed only by whitespace, in
            // which case we might get a zero-length tuple here.
            String strTuple = st.nextToken().trim();
            if(strTuple.length() == 0)
            {
               if(!st.hasMoreTokens()) break;
            }

            StringTokenizer tupleTokenizer = new StringTokenizer(strTuple);
            int n = tupleTokenizer.countTokens();
            if(n < 1) throw new Throwable();
            if(n < minLen) minLen = n;
            if(n > maxLen) maxLen = n;
            
            float[] tuple = new float[n];
            n = 0;
            while(tupleTokenizer.hasMoreTokens()) 
               tuple[n++] = Float.parseFloat(tupleTokenizer.nextToken());
            tuples.add(tuple);
         }
      } 
      catch(Throwable t) 
      { 
         return(null); 
      }
      
      // load the tuples into a 1D array in the proper order for the specified format. If the minimum tuple length is 
      // invalid for the specified format, abort. For the collection formats, ignore extra elements in tuples that are 
      // longer then the min observed tuple length. For the other two, we pad missing values with 0.
      if(minLen < ((fmt==Fmt.PTSET || fmt==Fmt.MSET) ? 2 : 1)) return(null);
      int nrows = tuples.size(); 
      int ncols = (fmt == Fmt.PTSET || fmt == Fmt.SERIES) ? Math.min((fmt==Fmt.PTSET ? 6 : 3), maxLen) : minLen;
      
      float[] fData = new float[nrows*ncols];
      int k = 0;
      for(int i=0; i<nrows; i++)
      {
         float[] tuple = tuples.get(i);
         int j = 0;
         while(j < tuple.length && j < ncols) fData[k++] = tuple[j++];
         while(j < ncols) { fData[k++] = 0; j++; }
      }

      // create the data set
      float[] params = null;
      if(fmt == Fmt.SERIES || fmt == Fmt.MSERIES) 
      {
         params = new float[2];
         params[0] = (Utilities.isWellDefined(dx) && dx != 0) ? dx : 1;
         params[1] = 0;
      }

      return(DataSet.createDataSet(id, fmt, params, nrows, ncols, fData));      
   }
   
   /**
    * Create and initialize a data set object consistent with content extracted from a plain-text data set source file 
    * supported in <i>FigureComposer</i>'s predecessor, <i>Phyplot</i>.
    * 
    * <p><i>Format description</i>. The old <i>Phylot</i> data source file lacked any kind of identifying header. It is
    * parsed one line at a time, where each line is interpreted as a separate "datum tuple" in a data set. Individual 
    * lines must be terminated by "\r", "\n", or "\r\n". A blank line (whitespace and line termination characters only) 
    * terminates a data set. <b>Even though the 1D, 3D and 4D formats did not exist in <i>Phyplot</i>, these newer data 
    * formats are also supported in this old data source file format!</b> Two source file formats are recognized:</p>
    * <ul>
    *    <li><i>Numbers only</i>. Each non-blank line must contain 1+ whitespace-separated floating-point numbers. If
    *    multiple data sets are defined in the file, they must be separated by at least one blank line. This is an 
    *    ambiguous format, because the method must guess at the data set's format. The loaded set is assigned a valid 
    *    data format based on the minimum and maximum observed lengths of the datum tuples read in. If a list of 
    *    preferred data set formats is provided, the method will choose formats from this list over other possible 
    *    guesses. Each set is assigned an identifier of the form "src<i>N</i>", where <i>N</i> indicates the order in 
    *    which the data sets were extracted from the file. If the selected data set format requires additional defining
    *    parameters, default parameter values are supplied.</li>
    *    <li><i>Annotated</i>. The first non-blank line is a "data set header", starting with a colon and containing 
    *    1, 2, 3 or 5 string tokens. The first is always the set ID, which must have at least one character and can 
    *    contain no whitespace. For the {@link Fmt#RASTER1D} format, this is the only token present. For the {@link 
    *    Fmt#XYZIMG} format, the header has the form <i>:id x0 x1 y0 y1</i>, where the last four tokens are floating-pt 
    *    numbers specifying the contiguous ranges spanned by the data along the x- and y-axes. For {@link Fmt#XYZSET} 
    *    and {@link Fmt#XYZWSET}, the header has exactly two tokens: <i>:id 2</i> for the former and <i>:id 3</i> for
    *    the latter. For the other data formats, all of which were supported in <i>Phyplot</i>, the header has the form 
    *    <i>:id {0|1} [dx]</i>. The second token indicates whether the data set should be interpreted as a single point 
    *    set or a collection of multiple point sets. The optional third token must be a floating-point number indicating
    *    the sample interval to be assigned to the data series. Thus, without the third token, the set format is {@link 
    *    Fmt#PTSET} if the second token is "0" and {@link Fmt#MSET} otherwise. If the third token IS present, then the
    *    set format is {@link Fmt#SERIES} if the second token is "0" and {@link Fmt#MSERIES} otherwise. Remaining lines 
    *    must each contain one or more whitespace-separated floating-point numbers. In this case, the tuple length L 
    *    <b>MUST</b> be consistent with the set format indicated in the header. For <b>XYZSET</b>, every tuple must 
    *    contain exactly 3 tokens; for <b>XYZWSET</b>, exactly 4. For <b>XYZIMG</b>, L should be the same across all 
    *    tuples, but if any tuple is shorter than the maximum observed tuple length, that tuple is padded with NaN. In 
    *    the case of a RASTER1D data set, each tuple is interpreted as a separate raster in the collection. <b>(NOTE 
    *    that an empty raster is not really possible in this format, but you can effectively get one by creating a tuple
    *    of length 1 in which the only token is "NaN".)</b> A blank line terminates each data set definition.</li>
    * </ul>
    * 
    * <p><i>Algorithm for guessing the data set format in the "numbers-only" case</i>. If a list of preferred data set 
    * formats is provided, choose the first format in that list that is consistent with the parsed data. If the list is
    * empty or no match is found, then guess based on the number of tuples parsed, <i>N</i>, and the minimum and 
    * maximum observed tuple lengths, <i>Lmin</i> and <i>Lmax</i>:
    * <ul>
    *    <li>If <i>Lmin &ge; 2</i> and <i>Lmax &le;= 3</i>, choose <b>PTSET</b>; else</li>
    *    <li>if <i>1 &le; Lmin &le; 3</i> and <i>1 &le; Lmax &le; 3</i>, choose <b>SERIES</b>; else</li>
    *    <li>if <i>2 &le; Lmin &le; 6</i> and <i>2 &le; Lmax &le; 6</i>, choose <b>PTSET</b>; else</li>
    *    <li>if <i>Lmin == Lmax</i> and <i>0.5 &le; (N/Lmax) &le; 2</i>, choose <b>XYZIMG</b>; else</li>
    *    <li>if <i>Lmin != Lmax</i> and <i>(N/Lmax < 0.5)</i>, choose <b>RASTER1D</b>; else</li>
    *    <li>if <i>Lmin == 1</i>, choose <b>MSERIES</b>, else choose <b>MSET</b>.</li>
    * </ul>
    * <b>NOTE</b> that the algorithm will never "guess" the format <b>XYZSET</b> or <b>XYZWSET</b>, since either could 
    * be interpreted as a <b>PTSET</b>, and the former as a <b>SERIES</b>. Yet another reason to avoid the ambiguous 
    * "numbers-only" format.
    * </p>
    * 
    * @param lines A contiguous sequence of text lines read from the <i>Phyplot</i>-era source file, constituting the 
    * definition of one data set in "numbers-only" or "annotated" form.
    * @param start Line number in file at which this sequence of lines began.
    * @param defID Default ID for the new data set, if the format is "numbers-only". If null, "set" is assumed. For the
    * "annotated" format, the ID is taken from the first line of the input, as described.
    * @param preferredFmts A list of preferred data set formats. Can be null or empty. It is intended to guide the 
    * choice of data set format in the "numbers-only" case. The method will choose the first format in the list that is 
    * consistent with the parsed data. The argument is ignored in the "annotated" case, which is unambiguous.
    * @param errMsg Optional error message buffer. If not null and a parsing error occurs, this buffer is initialized 
    * with an explanatory message, including the file line number at which the error was detected.
    * @return If parsing succeeded, return a new data set initialized IAW the sequence of text lines provided. Else, 
    * return null to indicate failure.
    */
   public static DataSet fromOldPlainTextSrcFileFormat(
            List<String> lines, int start, String defID, Fmt[] preferredFmts, StringBuffer errMsg)
   {
      Fmt fmt = null;
      float[] params = null;
      String id = (defID == null) ? "set" : defID;
      
      // if the first line starts with a colon, parse it as the annotation header and from it get the ID, set 
      // format, and additional definition parameters (if any). Abort if it is incorrectly formatted.
      boolean isAnnotated = false;
      int minAllowedLen = 1;
      int maxAllowedLen = Integer.MAX_VALUE;
      String firstLine = lines.get(0);
      if(firstLine.startsWith(":"))
      {
         isAnnotated = true;
         
         StringTokenizer st = new StringTokenizer(firstLine.substring(1));
         int nTokens = st.countTokens();
         if(nTokens < 1 || nTokens > 5 || nTokens == 4)
         {
            if(errMsg != null) errMsg.append("Bad annotation header at line " + start);
            return(null);
         }

         id = st.nextToken();
         
         if(nTokens == 1)
            fmt = Fmt.RASTER1D;
         else if(nTokens == 5)
         {
            fmt = Fmt.XYZIMG;
            params = new float[4];
            try
            {
               for(int i=0; i<4; i++) params[i] = Float.parseFloat(st.nextToken());
            }
            catch(NumberFormatException nfe)
            {
               if(errMsg != null) errMsg.append("Bad annotation header at line " + start);
               return(null);
            }
         }
         else
         {
            boolean isSampled = (nTokens == 3);
            String token = st.nextToken();
            boolean isMulti = !token.equals("0");
            if(token.equals("2"))
            {
               isSampled = false;  // in case a third token is there, ignore it
               fmt = Fmt.XYZSET;
               minAllowedLen = maxAllowedLen = 3;
            }
            else if(token.equals("3"))
            {
               isSampled = false;
               fmt = Fmt.XYZWSET;
               minAllowedLen = maxAllowedLen = 4;
            }
            else if(!(isSampled || isMulti))
            {
               fmt = Fmt.PTSET;
               minAllowedLen = 2;
               maxAllowedLen = 6;
            }
            else if(isSampled && !isMulti)
            {
               fmt = Fmt.SERIES;
               maxAllowedLen = 3;
            }
            else if((!isSampled) && isMulti)
            {
               fmt = Fmt.MSET;
               minAllowedLen = 2;
            }
            else
               fmt = Fmt.MSERIES;

            if(isSampled)
            {
               token = st.nextToken();
               params = new float[2];
               params[1] = 0;
               try { params[0] = Float.parseFloat(token);}
               catch(NumberFormatException nfe) 
               {
                  if(errMsg != null) errMsg.append("Bad annotation header at line " + start);
                  return(null);
               }
            }
         }
      }
      
      // abort if ID (either default ID or the one taken from annotation header) is invalid
      if(!isValidIDString(id)) 
      {
         if(errMsg != null) errMsg.append("Invalid dataset ID (" + id + ") for dataset starting at line " + start);
         return(null);
      }


      // parse all (or all remaining lines) as datum tuples. If annotation header provided, then enforce allowed range 
      // for individual tuple lengths. Abort immediately if parsing error occurs.
      ArrayList<float[]> tuples = new ArrayList<float[]>(lines.size());
      int minLen = Integer.MAX_VALUE;
      int maxLen = 0;
      for(int i = (isAnnotated ? 1:0); i < lines.size(); i++)
      {
         try
         {
            StringTokenizer tupleTokenizer = new StringTokenizer(lines.get(i));
            int n = tupleTokenizer.countTokens();
            if(n < 1 || (isAnnotated && (n < minAllowedLen || n > maxAllowedLen)))
            {
               if(errMsg != null) errMsg.append("Invalid tuple length " + n + " on line " + (start + i));
               return(null);
            }
            
            if(n < minLen) minLen = n;
            if(n > maxLen) maxLen = n;
         
            float[] tuple = new float[n];
            n = 0;
            while(tupleTokenizer.hasMoreTokens()) 
               tuple[n++] = Float.parseFloat(tupleTokenizer.nextToken());
            tuples.add(tuple);
         }
         catch(Throwable t)
         {
            if(errMsg != null) errMsg.append("Parsing error at line " + (start + i));
            return(null);
         }
      }

      // if there was no annotation header, guess set format. If a preferred list of data set formats is provided, we 
      // choose the FIRST format in the list that is compatible with the observed min and max tuple lengths. If no list
      // is provided or if no match was found in that list, then we guess based on minimum observed tuple length.
      if(!isAnnotated)
      {
         fmt = null;
         if(preferredFmts != null) for(int i=0; i<preferredFmts.length; i++)
         {
            switch(preferredFmts[i])
            {
               case PTSET:
                  if(minLen >= 2 && maxLen <= 6) {fmt = preferredFmts[i]; i = preferredFmts.length; }
                  break;
               case SERIES:
                  if(minLen >= 1 && maxLen <= 3) {fmt = preferredFmts[i]; i = preferredFmts.length; }
                  break;
               case MSET:
                  if(minLen >= 2) {fmt = preferredFmts[i]; i = preferredFmts.length; }
                  break;
               case MSERIES:
               case RASTER1D:
                  fmt = preferredFmts[i]; i = preferredFmts.length;
                  break;
               case XYZIMG:
                  if(minLen == maxLen) { fmt = preferredFmts[i]; i = preferredFmts.length; }
                  break;
               case XYZSET:
                  if(minLen == 3 && maxLen == 3) { fmt = preferredFmts[i]; i = preferredFmts.length; }
               case XYZWSET:
                  if(minLen == 4 && maxLen == 4) { fmt = preferredFmts[i]; i = preferredFmts.length; }
                  break;
            }
         }
         
         if(fmt == null)
         {
            double ratio = ((double)tuples.size()) / ((double)maxLen);
            
            if(minLen >= 2 && maxLen <= 3) fmt = Fmt.PTSET;
            else if(minLen >=1 && minLen <= 3 && maxLen >=1 && maxLen <= 3) fmt = Fmt.SERIES;
            else if(minLen >= 2 && minLen <= 6 && maxLen >= 2 && maxLen <= 6) fmt = Fmt.PTSET;
            else if(minLen == maxLen && 0.5 <= ratio && ratio <= 2) fmt = Fmt.XYZIMG;
            else if(minLen != maxLen && ratio <= 0.5) fmt = Fmt.RASTER1D;
            else if(minLen == 1) fmt = Fmt.MSERIES;
            else fmt = Fmt.MSET;
         }
      }
      
      
      // load the tuples into a 1D array in the proper order for the specified format. For the 2D collection formats, 
      // ignore extra elements in tuples that are longer than the min observed tuple length. For the other 2D formats, 
      // we pad missing values with 0. For RASTER1D, we store the N tuple lengths, then the tuples themselves in order.
      // For XYZIMG, all tuple lengths should be the same. If not, shorter tuples are padded with Float.NaN out to the
      // max observed tuple length. For XYZSET or XYZWSET, all tuples must have length 3 or 4, resp.
      int nrows = 0;
      int ncols = 0;
      float[] fData = null;
      if(fmt == Fmt.RASTER1D)
      {
         ncols = tuples.size();
         for(int i=0;i<ncols; i++) nrows += tuples.get(i).length;
         fData = new float[ncols + nrows];
         for(int i=0;i<ncols; i++) fData[i] = tuples.get(i).length;
         int i=ncols;
         for(float[] tuple : tuples)
         {
            for(int j=0; j<tuple.length; j++)
               fData[i++] = tuple[j];
         }
      }
      else if(fmt == Fmt.XYZIMG)
      {
         nrows = tuples.size();
         ncols = maxLen;
         fData = new float[nrows*ncols];
         int i = 0;
         for(float[] tuple : tuples)
         {
            for(int j=0; j<tuple.length; j++)
               fData[i++] = tuple[j];
            for(int j=tuple.length; j<maxLen; j++)
               fData[i++] = Float.NaN;
         }
      }
      else if(fmt == Fmt.XYZSET || fmt == Fmt.XYZWSET)
      {
         nrows = tuples.size();
         ncols = (fmt == Fmt.XYZSET) ? 3 : 4;
         fData = new float[nrows*ncols];
         int i = 0;
         for(float[] tuple : tuples)
         {
            for(int j=0; j<ncols; j++) fData[i++] = tuple[j];
         }
      }
      else
      {
         nrows = tuples.size(); 
         ncols = (fmt == Fmt.PTSET || fmt == Fmt.SERIES) ? Math.min((fmt==Fmt.PTSET ? 6 : 3), maxLen) : minLen;
         
         fData = new float[nrows*ncols];
         int k = 0;
         for(int i=0; i<nrows; i++)
         {
            float[] tuple = tuples.get(i);
            int j = 0;
            while(j < tuple.length && j < ncols) fData[k++] = tuple[j++];
            while(j < ncols) { fData[k++] = 0; j++; }
         }
      }

      // if not annotated, we have to set default values for additional parameters assoc with certain formats
      if(!isAnnotated)
      {
         if(fmt == Fmt.SERIES || fmt == Fmt.MSERIES) 
            params = new float[] {1, 0};
         else if(fmt == Fmt.XYZIMG)
            params = new float[] {-1, 1, -1, 1};
      }

      return(DataSet.createDataSet(id, fmt, params, nrows, ncols, fData));      
   }
   
   /**
    * Reconstruct a data set from its base-64 binary encoding.
    * <p><b>IMPORTANT</b>: The optional inclusion of coordinate range information in the base-64 encoding was introduced
    * in May 2011. This method handles both versions of the base-64 string. It does so by reading in the first 4 bytes 
    * as an integer. If that integer is negative, the next 36 bytes are decoded as 9 single-precision floating-point
    * numbers representing the data set's coordinate range information: <i>x0, x1, y0, y1, z0, z1, a, b, hasError</i>. 
    * The rest of the data set follows, beginning with the ID string. Otherwise, that first integer is the number of 
    * bytes in the ID string, and decoding proceeds immediately with that string.</p>
    * 
    * @param text A string containing the entire contents of a data set encoded in base-64 exactly as described in 
    * {@link #toBase64()}.
    * @return A data set object as reconstituted from the source string; null if decoding fails.
    */
   public static DataSet fromBase64(String text)
   {
      if(text == null || text.length() == 0) return(null);
      
      ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getMimeDecoder().decode(text));
      DataInputStream dis = new DataInputStream(bis);

      DataSet ds = null;
      try
      {
         float[] ranges = null;
         
         int len = dis.readInt();
         if(len < 0)
         {
            ranges = new float[9];
            for(int i=0; i<9; i++) ranges[i] = dis.readFloat();
            len = dis.readInt();
         }
         byte[] idBytes = new byte[len];
         dis.readFully(idBytes);
         String id = new String(idBytes, "us-ascii");
         
         Fmt fmt = Fmt.getFormatByIntCode(dis.readInt());
         if(fmt == null) return(null);
         
         int n = fmt.getNumberOfParams();
         float[] params = null;
         if(n > 0)
         {
            params = new float[n];
            for(int i=0; i<n; i++) params[i] = dis.readFloat();
         }
         
         int ncols = dis.readInt();
         int nrows = dis.readInt();
         n = (fmt == Fmt.RASTER1D) ? ncols + nrows : ncols*nrows;
         float[] fData = new float[n];
         for(int i=0; i<n; i++) fData[i] = dis.readFloat();

         /* This old impl uses com.srscicomp.common.util.Base64, which I removed in FC 5.2.0
         Base64.Decoder decoder = new Base64.Decoder(text);
         float[] ranges = null;
         
         int len = decoder.getInt();
         if(len < 0)
         {
            ranges = decoder.getFloatArray(9);
            len = decoder.getInt();
         }
         byte[] idBytes = decoder.get(len);
         String id = new String(idBytes, "us-ascii");
         
         Fmt fmt = Fmt.getFormatByIntCode(decoder.getInt());
         if(fmt == null) return(null);
         
         int n = fmt.getNumberOfParams();
         float[] params = (n > 0) ? decoder.getFloatArray(n) : null;
         
         int ncols = decoder.getInt();
         int nrows = decoder.getInt();
         n = (fmt == Fmt.RASTER1D) ? ncols + nrows : ncols*nrows;
         float[] fData = (n>0) ? decoder.getFloatArray(n) : new float[0];
         */
         
         ds = DataSet.createDataSet(DataSetInfo.createDataSetInfo(id, fmt, nrows, ncols, params), fData, ranges);
      }
      catch(IOException ioe) {}
      finally
      {
         if(dis != null) try { dis.close(); } catch(IOException ioe) {}
      }
      
      return(ds);
   }
   
   /**
    * Encode the contents of a data set object in base-64 binary format, suitable for plain-text serialization. 
    * Coordinate range information is not encoded. See {@link #toBase64(DataSet, boolean, boolean)}.
    * data set contents are stored as follows, in the order listed.
    * 
    * @param ds The data set to be encoded in base-64.
    * @param lineBreaks If set, the string returned will include a line break (carriage-return linefeed pair) after 
    * every 76 characters.
    * @return Base-64 encoding of the dataset, in the manner described.
    */
   public static String toBase64(DataSet ds, boolean lineBreaks) { return(DataSet.toBase64(ds, false, lineBreaks)); }
   
   /**
    * Encode the contents of a data set object in base-64 binary format, suitable for plain-text serialization. The 
    * data set contents are stored as follows, in the order listed.
    * <ol>
    *    <li>If <i>includeRangeInfo==true</i>, the integer -1 followed by the data set's coordinate range information as
    *    an array of 9 floating-point values: <i>x0, x1, y0, y1, z0, z1, a, b, hasError</i>. For the {@link Fmt.PTSET}
    *    and {@link Fmt#SERIES} formats, the range [a, b] is the Y-coordinate range WITHOUT accounting for any standard
    *    deviation data in the set. For the 4D {@link Fmt#XYZWSET} format, [a, b] is the coordinate range for the 4th
    *    data dimension, "W". Any coordinate range pair not applicable to the data format is set to [0 0]. The last 
    *    value, <i>hasError</i>, will be strictly positive ONLY if the data set contains standard-deviation data.</li>
    *    <li>Number of bytes in the data set ID, encoded in the US-ASCII character set.</li>
    *    <li>The array of bytes representing the dataset ID, encoded in US-ASCII.</li>
    *    <li>The data set format integer code.</li>
    *    <li>Additional parameters, if applicable. For {@link Fmt#XYZIMG}, four single-precision floating-point values 
    *    are stored, representing the x- and y-coordinate ranges spanned by the data: <i>[x0 x1 y0 y1]</i>. For {@link 
    *    Fmt#SERIES} and {@link Fmt#MSERIES}, two floating-point values are stored: <i>[dx x0]</i>. The remaining data 
    *    formats have no additional parameters.</li>
    *    <li>The breadth of the data set, <i>B</i>, an integer (possibly 0).</li>
    *    <li>The length of the data set,  <i>L</i>, an integer (possibly 0).</li>
    *    <li>The raw data array. The number of single-precision floating-point values encoded is the array length: 
    *    <i>B+L</i> for {@link Fmt#RASTER1D} data sets; <i>B*L</i> otherwise.</li>
    * </ol>
    * <p><b>IMPORTANT</b>: The optional inclusion of coordinate range information was introduced in May 2011. It is 
    * optional because the coordinate ranges can be calculated from the data, but providing the pre-calculated ranges
    * can save time for large data sets. The preceding integer of -1 is essential. The decoding algorithm reads the 
    * first integer from the source string. If it is negative, then it reads in the 9 floats, followed by the rest of 
    * the data set as described. If not, then it assumes no coordinate range info is present, and it interprets the 
    * integer as the number of bytes in the data set ID to follow.</p>
    * 
    * @param ds The data set to be encoded in base-64.
    * @param includeRangeInfo If set, the data set's coordinate range information will be included at the beginning of
    * the base-64 string, as described.
    * @param lineBreaks If set, the string returned will include a line break (carriage-return line-feed pair) after 
    * every 76 characters.
    * @return Base-64 encoding of the data set, in the manner described.
    */
   public static String toBase64(DataSet ds, boolean includeRangeInfo, boolean lineBreaks)
   {
      if(ds == null) throw new IllegalArgumentException("Null argument");
      
      ByteArrayOutputStream bos = new ByteArrayOutputStream(ds.getRawDataSize()*4);
      Base64.Encoder enc = lineBreaks ? Base64.getMimeEncoder() : Base64.getEncoder();
      DataOutputStream dos = new DataOutputStream(enc.wrap(bos));
      
      try
      {
         // include coordinate range information first, if requested. Tag with integer -1 so decoder can distinguish it
         // from the integer length of the ID string.
         if(includeRangeInfo)
         {
            dos.writeInt(-1);
            for(int i=0; i<ds.coordRanges.length; i++ ) dos.writeFloat(ds.coordRanges[i]);
            dos.writeFloat(ds.hasErrorData ? 1.0f : -1.0f);
         }
         
         try 
         { 
            byte[] idBytes = ds.id.getBytes("us-ascii"); 
            dos.writeInt(idBytes.length);
            dos.write(idBytes);
         }
         catch(UnsupportedEncodingException uee) { throw new NeverOccursException(uee); } // us-ascii always supported

         dos.writeInt(ds.format.getIntCode());
         if(ds.format == Fmt.SERIES || ds.format == Fmt.MSERIES)
         {
            dos.writeFloat(ds.dx);
            dos.writeFloat(ds.x0);
         }
         else if(ds.format == Fmt.XYZIMG)
         {
            for(int i=0; i< 4; i++) dos.writeFloat(ds.coordRanges[i]);
         }
         
         dos.writeInt(ds.width);
         dos.writeInt(ds.height);
         for(int i=0; i<ds.fData.length; i++) dos.writeFloat(ds.fData[i]);
         
      }
      catch(IOException ioe)
      {
         // since the underlying stream wraps a byte array, we will not get any IOException
         throw new NeverOccursException(ioe);
      }
      finally
      {
         if(dos != null) try { dos.close(); } catch(IOException ioe) {}
      }

      return(bos.toString());
      
      
      // NOTE: This old impl uses com.srscicomp.common.util.Base64, which I removed in FC 5.1.4
      /*
      Base64.Encoder encoder = new Base64.Encoder(lineBreaks);
      
      // include coordinate range information first, if requested. Tag with integer -1 so decoder can distinguish it
      // from the integer length of the ID string.
      if(includeRangeInfo)
      {
         encoder.put(-1);
         encoder.put(ds.coordRanges);
         encoder.put(ds.minY_ignoreStd);
         encoder.put(ds.maxY_ignoreStd);
         encoder.put(ds.hasErrorData ? 1.0f : -1.0f);
      }
      
      try 
      { 
         byte[] idBytes = ds.id.getBytes("us-ascii"); 
         encoder.put(idBytes.length);
         encoder.put(idBytes);
      }
      catch(UnsupportedEncodingException uee) { assert(false); } // us-ascii always supported by JVM 

      encoder.put(ds.format.getIntCode());
      if(ds.format == Fmt.SERIES || ds.format == Fmt.MSERIES)
      {
         encoder.put(ds.dx);
         encoder.put(ds.x0);
      }
      else if(ds.format == Fmt.XYZIMG)
      {
         for(int i=0; i< 4; i++) encoder.put(ds.coordRanges[i]);
      }
      
      encoder.put(ds.width);
      encoder.put(ds.height);
      if(ds.fData.length > 0) encoder.put(ds.fData);
      
      return(encoder.getOutput());
      */
   }
   
   
   private DataSet() {}
   
   /** The data set ID. */
   private String id = null;
   
   /** 
    * Get the ID for this data set.
    * @return Data set's ID string, assigned at construction time.
    */
   public String getID() { return(id); }

   /**
    * Change the ID for this data set. Since <b>DataSet</b> is immutable, this method returns another  <b>DataSet</b> 
    * instance that is identical to this one except for the ID string. Both instances will reference the same raw data 
    * array!
    * 
    * @param id The candidate ID.
    * @return A distinct <b>DataSet</b> instance sharing the same data array, data format, and any additional
    * defining parameters, but having the specified identifier. However, if the candidate ID is invalid, null is 
    * returned. Also, if the candidate ID matches this data set's identifier, this data set object is returned. 
    */
   public DataSet changeID(String id)
   {
      if(!isValidIDString(id)) return(null);
      if(this.id.equals(id)) return(this);
      
      DataSet ds = null;
      try { ds = (DataSet) this.clone(); } catch(CloneNotSupportedException cnse) { assert(false); }
      ds.id = id;
      return(ds);
   }

   /**
    * Extract a single member set from this collection-type data set.
    * @param idx The index position of the member set to be extracted. Ignored if this set is not a collection.
    * @returns The extracted member set, as described. Returns an empty set if <b>idx</b> is invalid.
    */
   public DataSet extractMember(int idx) { return(extractBlock(idx, 1)); }
   
   /**
    * Extract a contiguous block of member sets from this collection-type data set.
    * 
    * <p>The three supported collection-type data set formats are <b>MSET, MSERIES</b> and <b>RASTER1D</b>. If this 
    * data set is not in one of these formats, then the method simply returns this set object. Otherwise, it returns a
    * new data set object containing the specified block of member sets <i>in the same format</i>, even if only a single
    * member is extracted. The ID and any defined parameters of the extracted set will match this set's ID and 
    * parameters.</p>
    * 
    * @param start The index position of the first member set in the block to be extracted.
    * @param n The number of sets to extract. The actual size of the block will be less than <i>n</i> if <i>start + n 
    * &gt; {@link #getNumberOfSets()}</i>. If less than 1, 1 is assumed.
    * @returns The extracted block of member set(s), as described. Returns an empty set if <i>start</i> is not a valid 
    * index.
    */
   public DataSet extractBlock(int start, int n)
   {
      if(format != Fmt.MSET && format != Fmt.MSERIES && format != Fmt.RASTER1D) return(this);
      if(start < 0 || start >= getNumberOfSets()) return(DataSet.createEmptySet(format, id, getParams()));
      
      // compute index of last member set to be included in block
      if(n < 1) n = 1;
      int end = start + n - 1;
      if(end >= getNumberOfSets()) end = getNumberOfSets() - 1;
      
      int w = 0;
      int h = 0;
      Fmt dstFmt = null;
      float[] extractedData = null;
      switch(format)
      {
      case MSET:
         w = end - start + 2;
         h = height;
         dstFmt = (start==end) ? Fmt.PTSET : Fmt.MSET;
         extractedData = new float[w*h];
         if(start == end)
         {
            for(int i=0; i<h; i++)
            {
               extractedData[i*w] = fData[i*width];
               extractedData[i*w + 1] = fData[i*width + start + 1];
            }
         }
         else for(int i=0; i<h; i++)
         {
            extractedData[i*w] = fData[i*width];
            for(int j=1; j<w; j++)
               extractedData[i*w + j] = fData[i*width + start + j];
         }
         break;
      case MSERIES:
         w = end - start + 1; 
         h = height;
         dstFmt = (start==end) ? Fmt.SERIES : Fmt.MSERIES;
         extractedData = new float[w*h];
         if(w == 1) 
         {
            for(int i=0; i<h; i++) extractedData[i] = fData[i*width + start];
         }
         else for(int i=0; i<h; i++)
         {
            for(int j=0; j<w; j++)
               extractedData[i*w + j] = fData[i*width + start+j];
         }
         break;
      case RASTER1D:
         w = end - start + 1;
         h = 0;
         for(int i=start; i<= end; i++) h += (int) fData[i];

         dstFmt = Fmt.RASTER1D;
         extractedData = new float[w+h];
         for(int i=start; i<= end; i++) extractedData[i-start] = fData[i];
         if(h > 0)
         {
            int ofs = width; for(int i=0; i<start; i++) ofs += (int) fData[i];
            System.arraycopy(fData, ofs, extractedData, w, h); 
         }
         break;
      default:
         break;
      }

      return(DataSet.createDataSet(this.id, dstFmt, this.getParams(), h, w, extractedData));
   }
   
   /**
    * Get the raster samples for the specified raster in a {@link Fmt#RASTER1D} data set.
    * 
    * @param pos Index of the raster.
    * @param omitNaN If true, any ill-defined raster samples are excluded from the output.
    * @return The raster samples. Returns null if this is not a raster collection, or if the raster index is invalid.
    * Returns an empty array if the specified raster has no samples, or <i>omitNaN=true</> and the raster contains no
    * well-defined samples.
    */
   public float[] getRasterSamples(int pos, boolean omitNaN)
   {
      if(format != Fmt.RASTER1D || pos<0 || pos>=getNumberOfSets()) return(null);
      
      int ofs = width;
      for(int i=0; i<pos; i++) ofs += (int) fData[i];
      int nSamples = (int) fData[pos];
      if(nSamples == 0) return new float[0];
      
      // if we're omitting ill-defined samples, count them
      int nBad = 0;
      if(omitNaN)
      {
         for(int i=0; i<nSamples; i++) if(!Utilities.isWellDefined(fData[ofs+i])) ++nBad;
      }
      
      float[] out = new float[nSamples-nBad];
      if(nBad == 0)
         System.arraycopy(fData, ofs, out, 0, nSamples);
      else
      {
         int n = 0;
         for(int i=0; i<nSamples; i++) if(Utilities.isWellDefined(fData[ofs+i]))
         {
            out[n++] = fData[ofs+i];
         }
      }
      return(out);
   }
   
   /** The format for this data set. */
   private Fmt format = null;
   
   /** 
    * Get the format of this data set.
    * @return The data set format.
    */
   public Fmt getFormat() { return(format); }
   
   /**
    * Does this data set contain Y data sampled at regular intervals in X?
    * @return True only if the data set format is either {@link Fmt#SERIES} or {@link Fmt#MSERIES}.
    */
   public boolean isSampledSeries() { return(format == Fmt.SERIES || format == Fmt.MSERIES); }
   
   /** 
    * Data set breadth: granularity in X or image width for XYZIMG; the number of individual rasters for RASTER1D; tuple
    * length for all other formats.
    */
   private int width = 0;
   
   /**
    * Get the breadth of the data set: the image width or granularity in X for the {@link Fmt#XYZIMG} format; the number
    * of individual rasters for the {@link Fmt#RASTER1D} format; the "tuple length" for all other formats.
    * @return The raw data breadth, as described.
    */
   public int getDataBreadth() { return(width); }
   
   /** 
    * Data set length: granularity in Y or image height for XYZIMG; total number of raster samples (across all rasters) 
    * for RASTER1D; number of tuples for all other data formats. 
    */
   private int height = 0;
   
   /**
    * Get the length of the data set: the image height or granularity in Y for the {@link Fmt#XYZIMG} format; the total 
    * number of raster samples (across all rasters) for the {@link Fmt#RASTER1D} format; or the total number of datum
    * tuples in the set otherwise.
    * @return The raw data length, as described.
    */
   public int getDataLength() { return(height); }
   
   /** Sample interval in X (data series formats only); default value is 1. */
   private float dx = 1;
   
   /**
    * Get the sample interval in X for a sampled series-formatted data set.
    * @return The sample interval <i>dx</i>, defining each x-coordinate in the series by <i>x0 + n*dx</i>. If this data
    * set is not a sampled data series, the method returns 1 by default.
    */
   public float getDX() { return(dx); }
   
   /** Initial value of X (data series formats only); default value is 0. */
   private float x0 = 0;
   
   /**
    * Get the initial value of X for a sampled series-formatted data set.
    * @return The initial value <i>x0</i>, defining each x-coordinate in the series by <i>x0 + n*dx</i>. If this data
    * set is not a sampled data series, the method returns 0 by default.
    */
   public float getX0() { return(x0); }
  
   /** 
    * Single-precision floating-point raw data array. For the four 2D formats, XYZSET and XYZWSET, N M-tuples are stored 
    * sequentially. The array length is N*M. For RASTER1D, the rasters are stored sequentially and the first raster is 
    * preceded by a list of the individual raster lengths: <i>[n1 .. nM x1(1..n1) x2(1..n2) .. xM(1..nM)]</i>; the total
    * array length is N+M. For XYZIMG, this holds the values {z=f(x,y)}, which may be interpreted as an intensity image;
    * the array is populated row-wise and contains WxH entries, where W is width of the image and H is its height.
    */
   private float[] fData = null;
   
   /**
    * This package-private method provides direct access to the single-precision floating-pt data array that backs this 
    * data set. It avoids the possibly significant memory cost of retrieving an independent copy of the data through
    * {@link #copyRawData()}. It is intended only for use when writing a data set to a data source file or stream. 
    * <i><b>It is VITAL that callers make NO CHANGES to the array contents, nor pass the array reference to untrusted 
    * code!</b></i>
    * @returns The raw data array, possibly empty.
    */
   float[] getRawDataArray() { return(fData); }
   
   /**
    * Make an independent copy of the single-precision floating-point data array that backs this data set.
    * <p><i>Data array explained</i>: Let N be the data length and M the data breadth, as returned by the methods 
    * {@link #getDataLength()} and {@link #getDataBreadth()}, respectively. For {@link Fmt#RASTER1D}, the array contains
    * N+M elements. The first M elements of the array are the individual raster lengths. The remaining elements are the 
    * raster samples themselves: <i>[n1 .. nM x1(1..n1) x2(1..n2) .. xM(1..nM)]</i>, where <i>N=n1 + n2 + .. + nM</i>. 
    * For {@link Fmt#XYZIMG}, the array holds the values {z=f(x,y)}, which may be interpreted as an intensity image; the
    * image is stored row-wise in the NxM array, where M is the width of the image (or granularity in x) and N is its 
    * height (or granularity in y). For all other data formats, N M-tuples are stored sequentially in the array, which 
    * is exactly N*M elements long.</p>
    * @return An independent copy of the raw data array, formatted as described. Possibly empty.
    */
   public float[] copyRawData()
   {
      float[] raw = new float[fData.length];
      System.arraycopy(fData, 0, raw, 0, fData.length);
      return(raw);
   }
   
   /**
    * This method copies a portion of the single-precision floating-point data array that backs this data set. It is 
    * intended only for use when writing a data set to a data source file or stream.
    * @param offset Offset into the backing array.
    * @param length Number of elements to be copied.
    * @param fbuf Buffer into which selected portion of backing array is copied.
    * @throws IndexOutOfBoundsException if <b>offset</b> or <b>length</b> is negative, or if their sum exceeds the 
    * length of the backing array.
    */
   public void copyRawData(int offset, int length, FloatBuffer fbuf)
   { 
      if(offset < 0 || length < 0 || offset+length > fData.length) throw new IndexOutOfBoundsException();
      fbuf.put(fData, offset, length); 
   }
      
   /** 
    * Coordinate ranges spanned by data along X, Y, Z and W dimensions: <i>[x0 x1 y0 y1 z0 z1 w0 w1]</i>. Depending on 
    * the dimensionality of the data, irrelevant entries are set to zero. For the 2D data sets only, <i>w0=y0_noStd</i> 
    * and <i>w1=y1_noStd</i> -- i.e., the Y-coordinate range NOT accounting for any standard deviation data in Y.
    */
   private float[] coordRanges = null;
   
   /** 
    * Get the minimum of the x-coordinate range spanned by data set (accounts for standard deviation data!).
    * @return Minimum x-coordinate. 
    */
   public float getXMin() { return(coordRanges[0]); }

   /** 
    * Get the maximum of the x-coordinate range spanned by data set (accounts for standard deviation data!).
    * @return Maximum x-coordinate. 
    */
   public float getXMax() { return(coordRanges[1]); }

   /** 
    * Get the minimum of the y-coordinate range spanned by data set (accounts for standard deviation data!).
    * @return Minimum y-coordinate. Returns 0 always for a 1D data set.
    */
   public float getYMin() { return(coordRanges[2]); }

   /** 
    * Get the maximum of the y-coordinate range spanned by data set (accounts for standard deviation data!).
    * @return Maximum y-coordinate. Returns 0 always for a 1D data set.
    */
   public float getYMax() { return(coordRanges[3]); }

   /**
    * Get the minimum of the y-coordinate range spanned by data set, NOT accounting for any standard deviation data.
    * @return Minimum y-coordinate, standard deviation data excluded. Applicable only to 2D data set formats; returns 0
    * always for non-2D formats.
    */
   public float getYMin_IgnoreStd() { return(format.is2D() ? coordRanges[6] : 0); }
   
   /**
    * Get the maximum of the y-coordinate range spanned by data set, NOT accounting for any standard deviation data.
    * @return Maximum y-coordinate, standard deviation data excluded. Applicable only to 2D data set formats; returns 0
    * always for non-2D formats.
    */
   public float getYMax_IgnoreStd() { return(format.is2D() ? coordRanges[7] : 0); }
   
   /** 
    * Get smallest z-coordinate value observed across data set -- for 3D,4D formats only.
    * @return Minimum z-coordinate. Returns 0 always for 1D,2D data formats.
    */
   public float getZMin() { return(coordRanges[4]); }

   /** 
    * Get largest z-coordinate value observed across data set -- for 3D,4D formats only.
    * @return Maximum z-coordinate. Returns 0 always for 1D,2D data formats.
    */
   public float getZMax() { return(coordRanges[5]); }
   
   /** 
    * Get smallest w-coordinate value observed across data set -- for the 4D format only.
    * @return Minimum w-coordinate. Returns 0 always for non-4D data formats.
    */
   public float getWMin() { return((format==Fmt.XYZWSET) ? coordRanges[6] : 0); }

   /** 
    * Get largest w-coordinate value observed across data set -- for the 4D format only.
    * @return Maximum w-coordinate. Returns 0 always for non-4D data formats.
    */
   public float getWMax() { return((format==Fmt.XYZWSET) ? coordRanges[7] : 0); }
   
   /**
    * Get the additional defining parameters for this data set.
    * @return Array contains the current parameter values. The attributes vary with the data format. For the sampled
    * series formats, method returns [dx x0]. For {@link Fmt#XYZIMG}, [x0 x1 y0 y1], the x- and y-coord ranges spanned 
    * by the {z(x,y)} intensity "image". For all other formats, an empty array is returned.
    */
   public float[] getParams()
   {
      float[] params;
      if(format == Fmt.SERIES || format == Fmt.MSERIES)
         params = new float[] {dx, x0};
      else if(format == Fmt.XYZIMG)
         params = new float[] {coordRanges[0], coordRanges[1], coordRanges[2], coordRanges[3]};
      else 
         params = new float[0];
      return(params);
   }
   
   /**
    * Get the number of additional defining parameters for this data set.
    * @return Number of addition defining parameters for this data set: 4 (XYZIMG), 2 (SERIES/MSERIES), or 0.
    */
   public int getNumParams() 
   {
      return((format==Fmt.XYZIMG) ? 4 : ((format==Fmt.SERIES || format==Fmt.MSERIES) ? 2 : 0));
   }
   
   /**
    * Change the additional defining parameters specified for this data set. Since <b>DataSet</b> is immutable, this
    * method returns a new instance that's identical to this one except for the relevant parameters. The new instance 
    * will reference the same internal raw data array.
    * @param params The new parameter values. Content varies with the data format. Ignored if current format has no 
    * additional parameters.
    * @return A <b>DataSet</b> identical to this one but for the parameter values specified. If the data format has no 
    * additional parameters, the method simply returns a reference to this data set object. Returns null if the if the 
    * <b>params</b> argument is invalid in any way for this data set's format. 
    */
   public DataSet changeParams(float[] params) 
   { 
      if(format.getNumberOfParams() == 0) return(this);
      return(DataSet.createDataSet(id, format, params, height, width, fData));
   }
   
   /**
    * Get summary information for this data set object, including the set ID, data format, data dimensions, and any 
    * additional defining parameters as applicable.
    * @return The data set's summary information.
    */
   public DataSetInfo getInfo() { return(DataSetInfo.createDataSetInfo(id, format, height, width, getParams())); }
   
   /** Flag set if there's any nonzero standard deviation data in the set (2D data formats only). */
   private boolean hasErrorData = false;
   
   /** 
    * Does this data set contain any data points with nonzero standard deviations (in x- or y-coord)? (Since the 
    * presence or absence of error data is cached internally at construction time, this method returns quickly.) 
    * @return True only if at least one point in the set includes a nonzero standard deviation. Applicable to the 
    * 2D data formats only. For all other data set formats, the method returns false always.
    */
   public boolean hasErrorData() { return(hasErrorData); }
   
   /**
    * Helper method scans the data set and caches several statistics:
    * <ul>
    * <li>The coordinate range spanned along the cardinal directions X, Y, Z and W -- as applicable for the data set 
    * format. If a coordinate range pair is irrelevant to the data format, it is set to [0..0]. For the 2D data formats
    * only, the computed X- and Y-coordinate ranges include the effects of any non-zero standard deviations in the data 
    * set, while [w0 w1] = [y0_noStd y1_noStd] -- i.e., the Y-coordinate range NOT accounting for standard deviation 
    * data. For empty sets, all coordinate ranges are set to [0..0].</li>
    * <li>Whether or not the data set includes any non-zero standard deviation data (2D data formats only).</li>
    * </ul>
    */
   private void computeStats()
   {
      hasErrorData = false;
      coordRanges = new float[8];
      for(int i=0; i<8; i++) coordRanges[i] = 0;

      
      if(isEmpty()) return;
      
      if(format == Fmt.RASTER1D)
      {
         if(height == 0) return;    // special case: a collection of 1+ rasters, all of which are EMPTY!
         coordRanges[0] = Float.POSITIVE_INFINITY;
         coordRanges[1] = Float.NEGATIVE_INFINITY;
         for(int i=width; i<fData.length; i++)
         {
            if(fData[i] < coordRanges[0]) coordRanges[0] = fData[i];
            if(fData[i] > coordRanges[1]) coordRanges[1] = fData[i];
         }
      }
      else if(format == Fmt.XYZIMG)
      {
         coordRanges[4] = Float.POSITIVE_INFINITY;
         coordRanges[5] = Float.NEGATIVE_INFINITY;
         for(float f : fData)
         {
            if(f < coordRanges[4]) coordRanges[4] = f;
            if(f > coordRanges[5]) coordRanges[5] = f;
         }
      }
      else if(format == Fmt.XYZSET)
      {
         coordRanges[0] = Float.POSITIVE_INFINITY;
         coordRanges[1] = Float.NEGATIVE_INFINITY;
         coordRanges[2] = Float.POSITIVE_INFINITY;
         coordRanges[3] = Float.NEGATIVE_INFINITY;
         coordRanges[4] = Float.POSITIVE_INFINITY;
         coordRanges[5] = Float.NEGATIVE_INFINITY;
         for(int i=0; i<fData.length; i+=3)
         {
            float f = fData[i]; 
            if(f < coordRanges[0]) coordRanges[0] = f;
            if(f > coordRanges[1]) coordRanges[1] = f;
            
            f = fData[i+1];
            if(f < coordRanges[2]) coordRanges[2] = f;
            if(f > coordRanges[3]) coordRanges[3] = f;

            f = fData[i+2];
            if(f < coordRanges[4]) coordRanges[4] = f;
            if(f > coordRanges[5]) coordRanges[5] = f;
         }
      }
      else if(format == Fmt.XYZWSET)
      {
         coordRanges[0] = Float.POSITIVE_INFINITY;
         coordRanges[1] = Float.NEGATIVE_INFINITY;
         coordRanges[2] = Float.POSITIVE_INFINITY;
         coordRanges[3] = Float.NEGATIVE_INFINITY;
         coordRanges[4] = Float.POSITIVE_INFINITY;
         coordRanges[5] = Float.NEGATIVE_INFINITY;
         coordRanges[6] = Float.POSITIVE_INFINITY;
         coordRanges[7] = Float.NEGATIVE_INFINITY;
         for(int i=0; i<fData.length; i+=4)
         {
            float f = fData[i]; 
            if(f < coordRanges[0]) coordRanges[0] = f;
            if(f > coordRanges[1]) coordRanges[1] = f;
            
            f = fData[i+1];
            if(f < coordRanges[2]) coordRanges[2] = f;
            if(f > coordRanges[3]) coordRanges[3] = f;

            f = fData[i+2];
            if(f < coordRanges[4]) coordRanges[4] = f;
            if(f > coordRanges[5]) coordRanges[5] = f;
            
            f = fData[i+3];
            if(f < coordRanges[6]) coordRanges[6] = f;
            if(f > coordRanges[7]) coordRanges[7] = f;
         }
      }
      else 
      {
         coordRanges[0] = Float.POSITIVE_INFINITY;
         coordRanges[1] = Float.NEGATIVE_INFINITY;
         coordRanges[2] = Float.POSITIVE_INFINITY;
         coordRanges[3] = Float.NEGATIVE_INFINITY;
         coordRanges[6] = Float.POSITIVE_INFINITY;
         coordRanges[7] = Float.NEGATIVE_INFINITY;
         
         int n = getDataSize(0);
         int nSets = getNumberOfSets();

         for(int i = 0; i < n; i++)
         {
            float xStd = getXStdDev(i);
            xStd = (xStd != 0 && Utilities.isWellDefined(xStd)) ? Math.abs(xStd) : 0;
            float yStd = getYStdDev(i);
            yStd = (yStd != 0 && Utilities.isWellDefined(yStd)) ? Math.abs(yStd) : 0;
            
            if(!hasErrorData) hasErrorData = (xStd != 0) || (yStd != 0);
            
            float x = getX(i, 0);
            if((x-xStd) < coordRanges[0]) coordRanges[0] = x-xStd;
            if((x+xStd) > coordRanges[1]) coordRanges[1] = x+xStd;
            if(nSets > 1)
            {
               for(int j = 0; j < nSets; j++)
               {
                  float y = getY(i,j);
                  if(y < coordRanges[2]) coordRanges[2] = y;
                  if(y > coordRanges[3]) coordRanges[3] = y;
               }
               coordRanges[6] = coordRanges[2];
               coordRanges[7] = coordRanges[3];
            }
            else
            {
               float y = getY(i, 0);
               if(y < coordRanges[6]) coordRanges[6] = y;
               if(y > coordRanges[7]) coordRanges[7] = y;
               if((y-yStd) < coordRanges[2]) coordRanges[2] = y-yStd;
               if((y+yStd) > coordRanges[3]) coordRanges[3] = y+yStd;
            }
         }
      }
      
      // make sure all the ranges at least make sense
      for(int i=0; i<8; i+=2) if(coordRanges[i] > coordRanges[i+1])
      {
         coordRanges[i] = 0;
         coordRanges[i+1] = 0;
      }
   }
   
   /**
    * Get the number of member data sets stored within this data set. 
    * @return Number of individual data sets in collection, for the collection-type formats {@link Fmt#MSET, {@link 
    * Fmt#MSERIES}, and {@link Fmt#RASTER1D}. For all other data set formats, the method always returns 1. However, if 
    * set is empty, returns 0.
    */
   public int getNumberOfSets()
   {
      if(fData.length == 0) return(0);
      int n = 1;
      if(format == Fmt.MSERIES || format == Fmt.RASTER1D) n = width;
      else if(format == Fmt.MSET) n = width - 1;
      return(n);
   }
   
   /** 
    * Get the size of the raw data array backing this data set.
    * @return The length of the data set's raw data array. For {@link Fmt#RASTER1D}, this is the sum of the data set's 
    * breadth and length; for all other formats, it is the product.
    */
   public int getRawDataSize() { return((format == Fmt.RASTER1D) ? width + height : width * height); }
   
   /**
    * Get the size of an individual member set within this data set. 
    * @param iSet Index of individual point set. This argument applies only to {@link Fmt#RASTER1D} data sets, since 
    * this is the only format in which the length of an individual set can vary. For all other formats, it is ignored.
    * @return The number of points in the data set: For {@link Fmt#RASTER1D}, the method returns the length of the 
    * specified raster. For the 2D formats, returns the number of "datum tuples" in the set/collection. For {@link 
    * Fmt#XYZSET}, returns the number of data points (x,y,z) in the set. For {@link Fmt#XYZWSET}, returns the number of
    * data points (x,y,z,w} in the set. For the {@link Fmt#XYZIMG} format, returns the total number of elements in the 
    * underlying data/image matrix.
    * @throws IndexOutOfBoundsException if format is <b>RASTER1D</b> and the argument is less than zero or greater than 
    * or equal to the number of component rasters in the data set.
    */
   public int getDataSize(int iSet) 
   { 
      if(format == Fmt.RASTER1D)
      {
         if(iSet < 0 || iSet >= width) throw new IndexOutOfBoundsException();
         return((int)fData[iSet]);
      }
      else return((format == Fmt.XYZIMG) ? width*height : height);
   }
   
   /**
    * Is the data set empty?
    * @return True if data set contains no data. (Note that a {@link Fmt#RASTER1D} collection of one or more EMPTY 
    * rasters is not considered empty.)
    */
   public boolean isEmpty() { return(fData.length == 0); }
   
   /**
    * Retrieve the x-coordinate of a single specified point in this data set.
    * 
    * <p>For sampled data series formats, the x-coordinate is <i>x0 + i*dx</i>, where <i>i</i> is the point's index 
    * position, <i>dx</i> is the sample interval, and <i>x0</i> is the initial value of x. For the other 2D data set 
    * formats, {@link Fmt#XYZSET}, and {@link Fmt#XYZWSET}, the x-coordinate is specified in the data itself. For 
    * {@link Fmt.RASTER1D}, the individual raster samples are the x-coordinate values, and the arguments select a 
    * particular sample within a particular raster. For {@link Fmt#XYZIMG}, the index position locates a single value 
    * <i>z(x,y)</i> stored row-wise in the data array, and the corresponding X-coordinate is computed based on that 
    * position and the x-coordinate range spanned by the data (specified at construction time).</p>
    * @param pos Index of desired point.
    * @param iSet Index of individual point set. This argument applies only to {@link Fmt#RASTER1D}, since this is the
    * only format in which the length of an individual set can vary. For all other formats, it is ignored.
    * @return The x-coordinate of the specified data point in the specified set, as described above.
    * @throws IndexOutOfBoundsException if either argument is invalid.
    */
   public float getX(int pos, int iSet)
   {
      if(pos < 0 || pos >= getDataSize(iSet)) throw new IndexOutOfBoundsException();
      
      float x = 0;
      switch(format)
      {
         case PTSET :
         case MSET : 
         case XYZSET :
         case XYZWSET :
            x = fData[pos*width]; 
            break;
         case SERIES :
         case MSERIES :
            x = x0 + pos * dx;
            break;
         case RASTER1D :
            int start = width;
            for(int i=0; i<iSet; i++) start += (int) fData[i];
            x = fData[start + pos];
            break;
         case XYZIMG :
            x = ((float) (pos % width)) * (coordRanges[1] - coordRanges[0])  + coordRanges[0];
            break;
      }
      return(x);
   }
   
   /**
    * Retrieve the standard deviation in the X-coordinate of the data point at the specified position in data set.
    * @param pos Index of desired point.
    * @return Standard deviation in X-coordinate. It can be nonzero only when the data format is {@link Fmt#PTSET}.
    * @throws IndexOutOfBoundsException if argument is invalid.
    */
   public float getXStdDev(int pos)
   {
      if(pos < 0 || (format != Fmt.RASTER1D && pos >= getDataSize(0))) throw new IndexOutOfBoundsException();
      return((format != Fmt.PTSET || width < 5) ? 0 : fData[pos*width + 4]);
   }
   
   /**
    * Retrieve the error bar style for depicting the standard deviation in the x-coordinate of the data point at the 
    * specified position in this data set. The error bar may be rendered four different ways: invisible (not drawn!), 
    * two-sided, (x-xStd,y) to (x+xStd,y); plus-sided, (x,y) to (x+xStd,y); or minus-sided, (x,y) to (x-xStd,y). 
    * 
    * <p>The x-error bar style is applicable only to the {@link Fmt#PTSET} data format, since it is the only format that
    * can specify nonzero standard deviations in x. The style code can be explicitly specified in the last entry of a 
    * 6-tuple representing a single "point" in the set. If the tuple length is less than 6, then a two-sided error 
    * bar is assumed.</p>
    * @param pos Index of desired point.
    * @return 0 for two-sided, 1 for plus-sided, -1 for minus-sided error bar; 2 (or any other value) to hide error bar.
    * @throws IndexOutOfBoundsException if argument is invalid.
    */
  public int getXErrorBarStyle(int pos)
   {
     if(pos < 0 || (format != Fmt.RASTER1D && pos >= getDataSize(0))) throw new IndexOutOfBoundsException();
     return((format != Fmt.PTSET) ? 2 : ((width < 6) ? 0 : (int) fData[pos*width + 5]));
   }
   
  /**
   * Retrieve the Y-coordinate of a single specified point in this data set.
   * 
   * <p>For all of the 2D formats, {@link Fmt#XYZSET}, and {@link Fmt#XYZWSET}, the Y-coordinate of a given point is 
   * specified in the data itself. Furthermore, for the collection-type 2D formats, this method will return the 
   * <b>average</b> Y value at the indexed position if the <b>iSet</b> argument is invalid. Undefined or infinite values
   * are excluded from the calculation. For the one-dimensional {@link Fmt#RASTER1D} format, the method always returns 
   * NaN. For the {@link Fmt#XYZIMG} format, the index position locates a single value <i>z(x,y)</i> stored row-wise in 
   * the data array, and the corresponding Y-coordinate is computed based on that index position and the y-coord range 
   * spanned by the data (specified at construction time).</p>
   * @param pos Index of desired point.
   * @param iSet Index of individual point set. Applicable only to the 2D collection types {@link Fmt#MSET} and {@link 
   * Fmt#MSERIES}. If invalid, the method will compute the average across all data sets in the collection.
   * @return The Y-coordinate of the specified data point, from the specified point set, as described above.
   * @throws IndexOutOfBoundsException if either argument is invalid.
   */
   public float getY(int pos, int iSet)
   {
      if(pos < 0 || (format != Fmt.RASTER1D && pos >= getDataSize(0))) throw new IndexOutOfBoundsException();

      boolean doAvg = (iSet < 0 || iSet >= getNumberOfSets());
      float y = 0;
      int offset = (format==Fmt.PTSET || format==Fmt.MSET || format==Fmt.XYZSET || format==Fmt.XYZWSET) ? 1 : 0;
      switch(format)
      {
         case PTSET : 
         case SERIES : 
         case XYZSET :
         case XYZWSET :
            y = fData[pos*width + offset]; 
            break;
         case MSET : 
         case MSERIES :
            if(!doAvg)
               y = fData[pos*width + iSet + offset]; 
            else
            {
               double sum = 0;
               int n = 0;
               for(int i=0; i<getNumberOfSets(); i++) 
               {
                  float f = fData[pos*width + i + offset];
                  if(Utilities.isWellDefined(f)) { sum += f; ++n; }
               }
               y = (n == 0) ? Float.NaN : ((float) (sum/n));
            }
            break;
         case RASTER1D : 
            y = Float.NaN; 
            break;
         case XYZIMG :
            y = ((float) (pos / width)) * (coordRanges[3] - coordRanges[2])  + coordRanges[2];
            break;
      }
      return(y);
   }
   
   /**
    * Retrieve the standard deviation in the Y-coordinate of the data point at the specified position in this data set.
    * 
    * <p>For the {@link Fmt#PTSET} and {@link Fmt#SERIES} formats, the standard deviation in y is an optional member of
    * the datum tuple (<i>x y [yStd ye xStd xe]</i> or <i>y [yStd ye]</i>); if it is not present, it is assumed to be 
    * zero. For the {@link Fmt#MSET} and {@link Fmt#MSERIES} formats, the standard deviation in y is computed across the
    * individual point sets in the collection -- the assumption being that each such set is a repeated measure of the 
    * same stochastic variable. Undefined or infinite values are excluded from the calculation. For the {@link 
    * Fmt#RASTER1D} format, the method always returns NaN; for the 3D and 4D data formats, the method returns 0.</p>
    * @param pos Index of desired point.
    * @return Standard deviation in y-coordinate, as described above.
    * @throws IndexOutOfBoundsException if argument is invalid.
    */
   public float getYStdDev(int pos)
   {
      if(pos < 0 || (format != Fmt.RASTER1D && pos >= getDataSize(0))) throw new IndexOutOfBoundsException();

      float yStd = 0;
      
      switch(format)
      {
         case PTSET : 
         case SERIES : 
            int offset = (format == Fmt.PTSET) ? 2 : 1;
            yStd = (offset < width) ? fData[pos*width + offset] : 0; 
            break;
         case MSET : 
         case MSERIES :
            offset = (format == Fmt.MSET) ? 1 : 0;
            double mean = getY(pos, -1);
            if(!Utilities.isWellDefined(mean)) yStd = Float.NaN;
            else
            {
               double sum = 0;
               int n = 0;
               for(int i=0; i<getNumberOfSets(); i++) 
               {
                  float f = fData[pos*width + i + offset];
                  if(Utilities.isWellDefined(f)) 
                  { 
                     double diff = mean - f;
                     sum += diff*diff; 
                     ++n; 
                  }
               }
               yStd = (n == 0) ? Float.NaN : ((float) (Math.sqrt( sum / ((double)n) )));
            }
            break;
         case RASTER1D : 
            yStd = Float.NaN; 
            break;
         case XYZIMG :
         case XYZSET :
         case XYZWSET :
            yStd = 0;
            break;
      }
      return(yStd);
   }
   
   /**
    * Retrieve the error bar style for depicting the standard deviation in the y-coordinate of the data point at the 
    * specified position in this data set. The error bar may be rendered four different ways: invisible (not drawn!), 
    * two-sided, (x-xStd,y) to (x+xStd,y); plus-sided, (x,y) to (x+xStd,y); or minus-sided, (x,y) to (x-xStd,y). 
    * 
    * <p>The Y-error bar style is applicable only to the 2D data formats; for the other formats, no such error bars are 
    * rendered. The error bar style code may be explicitly specified as the 4th entry in each tuple of a {@link 
    * Fmt#PTSET} data set, or the 3rd entry in each tuple of a {@link Fmt#SERIES} set. If not present, a two-sided error
    * bar is assumed. Likewise, two-sided error bars in Y are assumed for all points of a data set in the {@link 
    * Fmt#MSET} or {@link Fmt#MSERIES} formats.</p>
    * @param pos Index of desired point.
    * @return 0 for two-sided, 1 for plus-sided, -1 for minus-sided error bar; 2 (or any other value) to hide error bar.
    * @throws IndexOutOfBoundsException if argument is invalid.
    */
   public int getYErrorBarStyle(int pos)
   {
      if(pos < 0 || (format != Fmt.RASTER1D && pos >= getDataSize(0))) throw new IndexOutOfBoundsException();
      
      int style = 2;
      if(format.is2D())
      {
         style = 0;
         if(format == Fmt.PTSET && width >= 4) style = (int) fData[pos*width + 3];
         else if(format == Fmt.SERIES && width >= 3) style = (int) fData[pos*width + 2];
      }
      return(style);
   }
   
   /**
    * Retrieve the Z-coordinate of a single specified point in this data set. Note that only 3D and 4D data formats 
    * have Z-coordinate data.
    * @param pos Index of desired point. Valid values lie in [0..L-1], where L = {@link #getDataSize()}. For {@link 
    * Fmt#XYZIMG}, this is an index into the image matrix; for {@link Fmt#XYZSET} and {@link Fmt#XYZWSET}, it is the 
    * index position of a particular data point in the set.
    * @return The Z-coordinate value. Returns <i>NaN</i> for any 1D or 2D data format.
    * @throws IndexOutOfBoundsException if <i>pos</i> argument is invalid.
    */
   public float getZ(int pos)
   {
      float z = Float.NaN;
      if(format == Fmt.XYZIMG || format == Fmt.XYZSET || format == Fmt.XYZWSET)
      {
         if(pos < 0 || pos >= getDataSize(0)) throw new IndexOutOfBoundsException();
      
         if(format == Fmt.XYZIMG) z = fData[pos]; 
         else z = fData[pos*width + 2];
      }
      return(z);
   }
   
   /**
    * Retrieve the W-coordinate of a single specified point in this 4D data set {X,Y,Z,W}. Applicable only to the 4D
    * data set format {@link Fmt#XYZWSET}
    * @param pos Index of desired point. Valid values lie in [0..L-1], where L = {@link #getDataSize()}.
    * @return The W-coordinate value. Returns <i>NaN</i> for any 1D, 2D, or 3D data format.
    * @throws IndexOutOfBoundsException if <i>pos</i> argument is invalid.
    */
   public float getW(int pos)
   {
      float z = Float.NaN;
      if(format == Fmt.XYZWSET)
      {
         if(pos < 0 || pos >= getDataSize(0)) throw new IndexOutOfBoundsException();
         z = fData[pos*width + 3];
      }
      return(z);
   }
   
   /**
    * Prepare an image representing the data in a 3D dataset conforming to the {@link Fmt#XYZIMG} format. Each pixel in
    * the image corresponds to a single datum in the Z(x,y) data matrix. That datum is mapped to a color in the image 
    * using the colormap provided: <i>pixel(i,j) = colormap[M(i,j)]</i>. Here <i>M(i,j)</i> is a simple linear or 
    * logarithmic function mapping the datum <i>Z(i,j)</i> to an array index <i>[1..L-1]</i>, where <i>L</i> is the
    * length of the colormap array. In the linear case, <i>M(i,j) = 1 + floor((Z(i,j)-R0)*(L-1)/(R1-R0))</i> and in the 
    * logarithmic (base 10) case, <i>M(i,j) = 1 + floor( log(Z(i,j)-R0+1)*(L-1)/log(R1-R0+1) )</i>. Here <i>[R0,R1]</i>, 
    * with <i>R0 &le; R1</i>, is the data range mapped onto the color array. This may be the actual observed data range 
    * <i>[Zmin .. Zmax]</i>, or a different range that could be a subset or superset of the observed range, or not 
    * overlap with the observed range at all (which makes little sense of course!). Each datum <i>Z(i,j) &lt; R0</i> is 
    * mapped to <i>R0</i>, and each <i>Z(i,j) &gt; R1</i> is mapped to <i>R1</i> (saturation effect).
    * 
    * <p><b>If Z(i,j) is not well-defined ("NaN"), then pixel(i,j) = colormap[0]; ie, all undefined values in the data
    * matrix are mapped to the first entry in the colormap.</b> In an earlier version, undefined data were mapped to 
    * fully transparent pixels, but this was difficult to reproduce in Postscript (requires LL3), so we decided to use
    * a completely opaque image.</p>
    * 
    * <p>In typical usage, the image provided should have the same dimensions as the data matrix. If the image is larger
    * (in either dimension), then a portion of the image will be left uninitialized by this method. If the image is 
    * smaller, then the resulting image will display a clipped view of the data matrix.</p>
    * 
    * @param bi The image to be initialized with a colormap-indexed representation of a <code>XYZIMG</code> dataset.
    * @param range Desired data range that should be mapped onto colormap array. If <code>null</code>, the observed
    * data range is used. Otherwise, this must be a two-element array <i>[R0,R1]</i> with <i>R0 &lt; R1</i>.
    * @param isLog <code>True</code> for the logarithmic data-to-color index mapping; <code>false</code> for linear.
    * @param colormap The colormap to be used. Each entry represents an ARGB quadruplet, with 8bits per component, the 
    * alpha component in the MSByte and the blue in the LSByte. This is the form expected by 
    * <code>BufferedImage.setRGB()</code>, which the method uses to populate the image.
    * @returns <code>True</code> if successful. If the dataset format is not <code>DataSet.Fmt.XYZIMG</code>, if the
    * image buffer or colormap argument is <code>null</code>, if the image buffer is empty, if the colormap contains 
    * fewer than 16 entries, or if the data range argument is invalid, the method fails.
    */
   public boolean prepareImage(BufferedImage bi, float[] range, boolean isLog, int[] colormap)
   {
      if(format != Fmt.XYZIMG || bi == null || colormap == null || colormap.length < 16) return(false);
      if(range != null && (range.length != 2 || range[0] >= range[1])) return(false);
      
      int imgW = Math.min(bi.getWidth(), width);
      int imgH = Math.min(bi.getHeight(), height);
      if(imgW == 0 || imgH == 0) return(true);
      
      float zMin = (range == null) ? coordRanges[4] : range[0];
      float zMax = (range == null) ? coordRanges[5] : range[1];
      if(zMin == zMax) return(false);
      
      float zRng = zMax-zMin;
      if(isLog) zRng = (float) Math.log10(zRng + 1);
      int len = colormap.length - 1;
      
      for(int j=0; j<imgH; j++)
      {
         for(int i=0; i<imgW; i++)
         {
            int k = j*width + i;
            float val = Utilities.rangeRestrict(zMin, zMax, fData[k]);
            if(!Utilities.isWellDefined(val)) bi.setRGB(i, j, colormap[0]);
            else
            {
               int idx = 0;
               if(isLog) idx = (int) (Math.log10(val-zMin+1) * len / zRng);
               else idx = (int) (((val-zMin) * len) / zRng);
               if(idx == len) --idx;
               bi.setRGB(i, j, colormap[idx+1]); 
            }
         }
      }
      return(true);
   }
   
   /** 
    * Get an iterator over the image data in a {@link Fmt#XYZIMG} data set. The iterator scans over the image data
    * row by row -- in the order it appears in the underlying data matrix. The raw floating-pt data is NOT provided;
    * instead, each raw datum <i>Z(i,j)</i> is converted to an integer index in <i>M(i,j) in [0..N-1]</i>, where 
    * <i>N</i> may be interpreted as the length of an indexed colormap array containing RGB colors. <i>M(i,j)</i> is a
    * linear or logarithmic mapping function. In the linear case, <i>M(i,j) = 1 + floor((Z(i,j)-R0)*(N-1)/(R1-R0))</i> 
    * and in the logarithmic (base 10) case, <i>M(i,j) = 1 + floor( log(Z(i,j)-R0+1)*(N-1)/log(R1-R0+1) )</i>. Here
    * <i>[R0,R1]</i>, with <i>R0 &le; R1</i>, is the data range mapped onto the color array. This may be the actual 
    * observed data range <i>[Zmin .. Zmax]</i>, or a different range that could be a subset or superset of the observed
    * range, or not overlap with the observed range at all (which makes little sense of course!). Each datum <i>Z(i,j) 
    * &lt; R0</i> is mapped to <i>R0</i>, and each <i>Z(i,j) &gt; R1</i> is mapped to <i>R1</i> (saturation effect). Any
    * ill-defined (infinite or NaN) datum is mapped to index 0. Thus, the image data provided by this iterator 
    * represents an indexed-color image consistent with the buffered image created by <code>prepareImage()</code>. 
    * 
    * @param n The length of the colormap through which the indexed-color image data might be passed to generate a
    * physical image of the underlying data.
    * @param range Desired data range that should be mapped onto colormap array. If <code>null</code>, the observed
    * data range is used. Otherwise, this must be a two-element array <i>[R0,R1]</i> with <i>R0 &lt; R1</i>.
    * @param isLog <code>True</code> for the logarithmic data-to-color index mapping; <code>false</code> for linear.
    * @return The image data iterator, or <code>null</code> if this is not a <code>Fmt.XYZIMG</code> dataset, or if the
    * <code>range</code> argument is invalid.
    */
   public Iterator<Integer> getIndexedImageDataIterator(int n, float[] range, boolean isLog)
   {
      if(format != Fmt.XYZIMG || (range != null && (range.length != 2 || range[0] >= range[1]))) return(null);
      return(new ImageDataProducer(n, range, isLog));
   }
   
   /**
    * Get an iterator that traverses the internal raw data array for this data set from start to finish. This iterator
    * is intended for use when writing the data set object to file or other output stream. It exposes the raw data
    * array in a read-only -- and, hopefully, a fast and efficient -- manner.
    * @return An iterator over this dataset's raw data array. If the data set is emtpy, it will have no elements!
    */
   public Iterator<Float> getRawDataIterator() { return( new RawDataProducer()); }
   
   /**
    * Helper class providing an iterator over the "image data" in a {@link Fmt#XYZIMG} data set. It provides, for each 
    * pixel <i>(i,j)</i>, the colormap index I(i,j) to which the raw datum Z(i,j) would be mapped given a colormap of 
    * length N. Each pixel value will lie in the range [0..N-1]. Any ill-defined datum is mapped to index 0; otherwise, 
    * data are mapped to [1..N-1]. Thus, it generates image data very similar to that provided by {@link 
    * DataSet#prepareImage()}, except that the pixels hold colormap indices rather than actual RGB colors.
    * 
    * @author sruffner
    */
   private class ImageDataProducer implements Iterable<Integer>, Iterator<Integer>
   {
      final int len;
      final float[] range;
      final boolean isLog;
      final float zMin;
      final float zMax;
      final float zRng;
      int iRow = 0;
      int iCol = 0;
      boolean done;
      
      public Iterator<Integer> iterator() { return(new ImageDataProducer(len, range, isLog)); }

      ImageDataProducer(int colorMapLen, float[] rng, boolean isLog) 
      { 
         len = colorMapLen; 
         range = rng;
         this.isLog = isLog; 
         
         done = format != Fmt.XYZIMG || width == 0 || height == 0 || len <= 0;   // ridiculous cases
         if(!done) done = (rng != null) && (rng.length != 2);
         
         zMin = (range == null) ? coordRanges[4] : range[0];
         zMax = (range == null) ? coordRanges[5] : range[1];
         if(!done) done = (zMin >= zMax);
         zRng = done ? 1 : (isLog ? (float)Math.log10(zMax-zMin+1) : (zMax-zMin)); 
      }

      public boolean hasNext() { return(!done); }

      public Integer next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         float val = Utilities.rangeRestrict(zMin, zMax, fData[iRow*width + iCol]);
         int idx = 0;
         if(Utilities.isWellDefined(val))
         {
            if(isLog) idx = (int) (Math.log10(val-zMin+1) * (len-1) / zRng);
            else idx = (int) (((val-zMin) * (len-1)) / zRng);
            idx = Utilities.rangeRestrict(0, len-2, idx) + 1;
         }
         if(++iCol == width)
         {
            iCol = 0;
            if(++iRow == height) done = true;
         }
         
         return(new Integer(idx));
      }

      public void remove()
      {
         throw new UnsupportedOperationException("Removal not supported by this iterator.");
      }
   }
   
   /**
    * <code>RawDataProducer</code> is a helper class for <code>DataSet</code> that provides an iterator over the set's
    * internal raw data array.
    * @author  sruffner
    */
   private class RawDataProducer implements Iterable<Float>, Iterator<Float>
   {
      int idx = 0;
      
      public Iterator<Float> iterator() { return(new RawDataProducer()); }

      public boolean hasNext() { return(idx < fData.length); }

      public Float next()
      {
         if(idx >= fData.length) throw new NoSuchElementException("Out of elements.");
         return(fData[idx++]);
      }

      public void remove()
      {
         throw new UnsupportedOperationException("Removal not supported by this iterator.");
      }
   }
}
