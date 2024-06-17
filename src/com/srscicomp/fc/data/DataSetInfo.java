package com.srscicomp.fc.data;

import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet.Fmt;

/**
 * An immutable object encapsulating summary information on a single {@link DataSet}.
 * @author sruffner
 */
public class DataSetInfo
{
   /**
    * Create summary information for a {@link DataSet} object.
    * 
    * @param id The data set identifier. Must pass the test {@link DataSet#isValidIDString(String)}.
    * @param format The data format. Cannot be null.
    * @param nrows Number of rows in the data. See {@link DataSet#getDataLength()} for details. Cannot be negative.
    * @param ncols The number of columns in the data. See {@link DataSet#getDataBreadth()} for details. Cannot be 
    * negative. If <b>nrows</b> is nonzero, <b>ncols</b> must be valid.
    * @param params Other defining parameters for the data set. The contents will vary with the data set format:
    *    <ul>
    *    <li><b>PTSET, MSET, RASTER1D, XYZSET, XYZWSET</b>: Not used. Argument is ignored.</li>
    *    <li><b>SERIES, MSERIES</b>: <i>[dx, x0]</i>. The x-coordinates of the series data are defined by the
    *    expression <i>x0 + n*dx for n = [0..N-1]</i>, where <i>N</i> is the series length.</li>
    *    <li><i>XYZIMG</i>: <i>[x0, x1, y0, y1]</i>, where <i>[x0..x1]</i> is the x-coordinate range and <i>[y0..y1]</i>
    *    is the y-coordinate range spanned by the 3D data <i>z(x,y)</i>.</li>
    *    </ul>
    * @return The data set summary information, or null if any arguments are invalid.
    */
   public static DataSetInfo createDataSetInfo(String id, Fmt format, int nrows, int ncols, float[] params)
   {
      if(format == null || !DataSet.isValidIDString(id)) return(null);
      if(nrows < 0 || ncols < 0) return(null);

      float[] p = null;
      int nr = nrows;
      int nc = ncols;
      switch(format)
      {
         case PTSET :
         case MSET : 
            if(nr == 0) nc = 2;
            if(nc < 2 || (format == Fmt.PTSET && nc > 6)) return(null);
            break;
         case SERIES :
         case MSERIES :
            if(nr == 0) nc = 1;
            if(nc < 1 || (format == Fmt.SERIES && nc > 3)) return(null);
            if(params == null || params.length < 2) return(null);
            if(!Utilities.isWellDefined(params[0]) || (params[0] == 0) || !Utilities.isWellDefined(params[1]))
               return(null);
            p = new float[] {params[0], params[1]};
            break;
         case RASTER1D :
            if(nr > 0 && nc == 0) return(null);
            break;
         case XYZIMG :
            if(nr == 0 || nc == 0) {nr = 0; nc = 0;}
            if(params == null || params.length < 4) return(null);
            for(int i=0; i<4; i++) if(!Utilities.isWellDefined(params[i])) 
               return(null);
            if((params[0] == params[1]) || (params[2] == params[3])) return(null);
            p = new float[] {params[0], params[1], params[2], params[3]};
            break;
         case XYZSET:
            if(nr == 0) nc = 3;
            if(nc != 3) return(null);
            break;
         case XYZWSET:
            if(nr == 0) nc = 4;
            if(nc != 4) return(null);
            break;
      }

      return(new DataSetInfo(id, format, nr, nc, p));
   }

   /**
    * Create a summary information for a {@link DataSet} object. In this version, the data format is specified by an 
    * integer code instead of an enumerated value. See {@link #createDataSetInfo(String, Fmt, int, 
    * int, float[])} for details.
    * @return The data set summary information, or null if any arguments are invalid.
    */
   public static DataSetInfo createDataSetInfo(String id, int fmtCode, int nrows, int ncols, float[] params)
   {
      return(DataSetInfo.createDataSetInfo(id, Fmt.getFormatByIntCode(fmtCode), nrows, ncols, params));
   }
   
   /**
    * Change the data set identifier assigned to a <b>DataSetInfo</b> instance. Since <b>DataSetInfo</b> is immutable,
    * the method normally returns a new instance identical to the original one, except for the ID.
    * 
    * @param info The data set summary information to be updated.
    * @param id The new identifier. Must satisfy constraints enforced by {@link DataSet#isValidIDString(String)}.
    * @return Null if the new identifier is invalid. If it is identical to the ID of the <b>info</b> argument, then 
    * return that argument. Else, return a new <b>DataSetInfo</b> instance identical to the <b>info</b> argument 
    * except for the identifier.
    */
   public static DataSetInfo changeID(DataSetInfo info, String id)
   {
      if(!DataSet.isValidIDString(id)) return(null);
      return(new DataSetInfo(id, info.format, info.nrows, info.ncols, info.params));
   }
   
   /**
    * Checks whether or not two instances of <b>DataSetInfo</b> are identical.
    * @param dsi1 One instance.
    * @param dsi2 Another instance.
    * @return True only if both instances are non-null and contain exactly the same data set information.
    */
   public static boolean match(DataSetInfo dsi1, DataSetInfo dsi2)
   {
      if(dsi1 == null || dsi2 == null) return(false);
      if(dsi1 == dsi2) return(true);
      boolean same = (dsi1.format == dsi2.format) && dsi1.id.equals(dsi2.id) && 
            (dsi1.nrows == dsi2.nrows) && (dsi1.ncols == dsi2.ncols);
      if(same) 
      {
         int n = dsi1.format.getNumberOfParams();
         for(int i=0; same && i<n; i++) same = dsi1.params[i] == dsi2.params[i];
      }
      return(same);
   }
   
   private DataSetInfo(String id, Fmt format, int nrows, int ncols, float[] params)
   {
      this.id = id;
      this.format = format;
      this.nrows = nrows;
      this.ncols = ncols;
      this.params = params;
   }
   
   /** The data set's identifier. */
   private final String id;
   
   /** The data set format. */
   private final Fmt format;
   
   /** The length of, or number of rows in, the data matrix. */
   private final int nrows;

   /** The breadth of, or number of columns in, the data matrix. */
   private final int ncols;

   /** Other defining parameters for the data set. */
   private final float[] params;
   
   /** 
    * Get the data set identifier. 
    * @return ID string.
    */
   public String getID() { return(id); }
   
   /**
    * Get the data set's format.
    * @return The data format
    */
   public Fmt getFormat() { return(format); }
   
   /**
    * Get the data set length, ie, the number of rows in the data matrix. Its meaning depends on the data format.
    * @return The data set's length.
    * @see DataSet#getDataLength()
    */
   public int getDataLength() { return(nrows); }

   /**
    * Get the data set breadth, ie, the number of columns in the data matrix. Its meaning depends on the data format.
    * @return The data set's breadth.
    * @see DataSet#getDataBreadth()
    */
   public int getDataBreadth() { return(ncols); }
   
   /**
    * Is this a collection-type data set format?
    * @return True only if data set format represents a collection of individual data sets.
    */
   public boolean isCollectionSet()
   {
      return(format == Fmt.MSET || format == Fmt.MSERIES || format == Fmt.RASTER1D);
   }
   
   /**
    * Get number of individual member sets stored in a single data "collection".
    * @return Number of sets in the collection, as described above.
    * @see DataSet#getNumberOfSets()
    */
   public int getNumSetsInCollection() 
   {
      if(format == Fmt.MSET) return(ncols-1);
      else if(format == Fmt.MSERIES || format == Fmt.RASTER1D) return(ncols);
      else return(1);
   }
   
   /** 
    * Get the length of the raw data array consistent with this data set summary information.
    * @return For a {@link Fmt#RASTER1D} data set, the data array length is N+M, where N is the data length and
    * M the breadth. For all other data set formats, returns N*M.
    */
   public int getDataArraySize() { return((format == Fmt.RASTER1D) ? nrows + ncols : nrows * ncols); }
   
   /**
    * Get one of the additional defining parameters associated with the data format. Only selected data formats have 
    * such parameters:
    * <ul>
    *    <li>{@link Fmt#SERIES}, {@link Fmt#MSERIES}: <b>i=0</b> for sample interval <i>dx</i>;
    *    <b>i=1</b> for the initial value of x, <i>x0</i>.</li>
    *    <li>{@link Fmt#XYZIMG}: Four parameters specifying the x- and y-coordinate ranges spanned by the
    *    "image" data -- <b>i=0,1</b> retrieves <i>[x0,x1]</i>; <b>i=2,3</b> retrieves <i>[y0,y1]</i>.</li>
    * </ul>
    * @param i Index of requested parameter.
    * @return The parameter value, or NaN if index does not select a valid parameter.
    */
   public float getParam(int i)
   {
      float p = Float.NaN;
      if(i >= 0 && i<format.getNumberOfParams()) p = params[i];
      return(p);
   }
   
   public String getShortDescription() { return(getShortDescription(true)); }
   
   public String getShortDescription(boolean includeID) 
   {
      String desc = includeID ? (id + ": ") : "";
      switch(format)
      {
         case PTSET:
            desc += "point set {x y";
            if(ncols >= 3) desc += " yStd";
            if(ncols >= 4) desc += " yE";
            if(ncols >= 5) desc += " xStd";
            if(ncols >= 6) desc += " xE";
            desc += "}, n = " + nrows;
            break;
         case MSET:
            desc += "point set collection {x y1 ...}";
            desc += ", n = " + nrows + ", nSets = " + (ncols-1);
            break;
         case SERIES:
            desc += "series {y";
            if(ncols >= 2) desc += " yStd";
            if(ncols >= 3) desc += " yE";
            desc += "}, n = " + nrows;
            desc += "; dx = " + ((params != null && params.length > 0) ? Utilities.toString(params[0], 6, -1) : "?");
            desc += "; x0 = " + ((params != null && params.length > 1) ? Utilities.toString(params[1], 6, -1) : "?");
            break;
         case MSERIES:
            desc += "series collection {y1 y2 ...}";
            desc += ", n = " + nrows + ", nSets = " + ncols;
            desc += "; dx = " + ((params != null && params.length > 0) ? Utilities.toString(params[0], 6, -1) : "?");
            desc += "; x0 = " + ((params != null && params.length > 1) ? Utilities.toString(params[1], 6, -1) : "?");
           break;
         case RASTER1D:
            desc += "1D raster collection; #rasters = " + ncols + "; #pts = " + nrows;
            break;
         case XYZIMG:
            desc += "Z(x,y) image; width = " + ncols + ", height = " + nrows;
            if(params != null && params.length == 4)
               desc += " [x0 x1 y0 y1] = " + Utilities.toString(params, 6, -1);
            break;
         case XYZSET:
            desc += "3D point set: {x1 y1 z1 ...}; num points = " + nrows;
            break;
         case XYZWSET:
            desc += "4D point set: {x1 y1 z1 w1 ...}; num points = " + nrows;
            break;
      }
      return(desc);
   }

   /**  Overridden to return the identifier of the data set represented by this data set summary information object. */
   @Override public String toString() { return(id); } 
}
