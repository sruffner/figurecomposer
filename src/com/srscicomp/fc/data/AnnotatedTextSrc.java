package com.srscicomp.fc.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;

import com.srscicomp.common.util.Utilities;

/**
 * This {@link IDataSrc} implementation reads and writes files containing <i>FigureComposer</i>-compatible data sets 
 * stored in an annotated plain-text format. The format is similar to that of {@link DeprecatedBinarySrc}, but will be 
 * much less efficient because data are stored as text strings. Converting strings to numbers and vice versa is much 
 * slower than reading and writing floating-point arrays directly.
 * 
 * <h2>Format description</h2>
 * <p>The file begins with a <i>header line</i> that includes the number <i>N</i> of data sets stored in the file. This
 * is followed by <i>N</i> "table of contents" (TOC) entries, each of which is a single line listing summary information
 * for each data set. The last TOC entry is followed immediately by <i>N</i> "data sections".</p>
 * 
 * <p>The file's character encoding is US-ASCII, and every line in the file ends with a carriage-return line-feed pair.
 * The implementation of the {@link #writeData()} method adheres to these rules. If a text file prepared by other means
 * fails to meet these requirements, <b>AnnotatedTextSrc</b> will be unable to read it.</p>
 * 
 * <p>The <i>header line</i> has the form "@DN,{V},{N}". Here the tag "@DN" identifies the file as an FC annotated text 
 * data source, "{V}" is an integer string specifying the file version number, and "{N}" is an integer string indicating
 * the number of data sets currently stored in the file. The current version number is 0, and there are no other 
 * versions. <i>N</i> must be non-negative.</p>
 * 
 * <p>Each TOC line has the form: "ID,FMT,NROWS,NCOLS[,PARAM0,PARAM1,PARAM2,PARAM3]". Note that the fields are
 * comma-separated with no intervening whitespace. In the field descriptions below, PTSET, MSET, SERIES, MSERIES, 
 * RASTER1D, XYZIMG, XYZSET, and XYZWSET represent the different data set formats supported in FC and enumerated in 
 * {@link DataSet.Fmt}.</p>
 * <ul>
 *    <li>ID: The data set identifier. Every data set in the file must have a different ID, and all such IDs must 
 *    satisfy the constraints imposed by {@link DataSet#isValidIDString()}.</li>
 *    <li>FMT: Data format. This string token must parse as an integer in [0..7], corresponding to one of the eight
 *    recognized data formats.</li>
 *    <li>NROWS: An integer string indicating the number of rows in data matrix. For RASTER1D data, it is the total 
 *    number of raster samples stored. For XYZIMG, it is the height of the image data Z(x,y), or the granularity in y. 
 *    For all other formats, this is the number of data points in the set (or in each set in the collection). Its value 
 *    should be non-negative; 0 indicates an empty set.</li>
 *    <li>NCOLS: An integer string indicating the number of columns in data matrix. For RASTER1D data, it is the number 
 *    of individual rasters stored (0+). For XYZIMG, it is the width of the image  data Z(x,y), or the granularity in x.
 *    For all other formats, this gives the data breadth, or the length of a single datum tuple, representing a single 
 *    data point. Note that the MSET and MSERIES formats are collections of data sets sharing the same x-coordinate 
 *    vector. The number of individual sets in an MSET is one less than the number of columns, while it is equal to the
 *    column count for an MSERIES.</li>
 *    <li>PARAM0..PARAM3: Additional defining parameters. For the SERIES and MSERIES formats, PARAM0 is the sample 
 *    interval <i>dx</i> and PARAM1 is the initial value <i>x0</i>; PARAM2,3 are ignored if present. Note that <i>dx</i>
 *    cannot be zero. For XYZIMG, all four parameters are required, specifying the x- and y-coordinate  anges spanned by
 *    the image data Z(x,y): <i>[x0 x1 y0 y1]</i>. Note that <i>x0</i> and <i>x1</i> cannot be equal, nor can <i>y0</i> 
 *    and <i>y1</i>. For all other data formats, no additional parameters are needed. If any are present, they are
 *    simply ignored.</li>
 * </ul>
 * 
 * <p>Each data section begins with a tag line of the form "{n}:", where "{n}" is the zero-based index position of the 
 * data set in the file. It serves to provide one more check that the file is correctly formatted. The actual data 
 * matrix begins on the next line. For all data formats except RASTER1D, it will be stored on NROWS lines, where each 
 * line is a whitespace-separated list of NCOLS floating-point numbers. For XYZIMG, each line represents the values in 
 * one row of the "image" matrix; for all formats besides RASTER1D and XYZIMG, each line stores a single "datum tuple" 
 * from the data set.</p>
 * 
 * <p>The data section format for a RASTER1D data set is different. It will contain NCOLS+1 lines -- remember that NCOLS
 * is the number of individual rasters, <i>M</i>. The first line will be a list of <i>M</i> whitespace-separated 
 * integers indicating the number of samples in each individual raster: <i>n1 n2 .. nM</i>. Each of the remaining lines 
 * will be a list of whitespace-separated floating-point values, representing the actual X-coordinate samples in each 
 * individual raster. The first of these must contain <i>n1</i> values, the second <i>n2</i>, and so on. Note that, if
 * an individual raster is empty, it will be represented by a blank line!</p>
 * 
 * <p>All floating-point numbers written to the file are stored with up to 6 digits after the decimal point. This will
 * fundamentally limit the universe of data that can be stored reasonably accurately in the text file. Undefined 
 * floating-point values should be stored as "NaN", and infinite values as "-Infinity" or "+Infinity".</p>
 * 
 * <p>It should be apparent from the above description that a <i>DataNav</i> annotated-text data source file is not 
 * necessarily easy to read -- e.g., a data matrix with a relatively small number of columns will result in very long 
 * text lines when viewed in a text editor. Nevertheless, it will be far more readable than a binary source file!</p>
 * 
 * @author sruffner
 */
class AnnotatedTextSrc implements IDataSrc
{
   /**
    * Is the content of the specified file consistent with the expected format of a <i>DataNav</i> annotated ASCII
    * text data source file? The method checks the validity of the header line, and the "table-of-contents" entries. 
    * It also verifies that the file contains the correct tag line for the first data section.
    * @param f The file to test.
    * @return True if file content is consistent with format of a <i>DataNav</i> annotated ASCII text data source. 
    * Returns false if argument is null, if file does not exist, or if it does not pass the consistency check.
    */
   static boolean checkFile(File f)
   {
      if(f == null || !f.isFile()) return(false);
      
      BufferedReader rdr = null;
      try
      {
         // create a buffered reader for reading the ascii text file one line at a time
         rdr = new BufferedReader( new InputStreamReader( new FileInputStream(f), "us-ascii" ) );

         // parse header line and TOC
         DataSetInfo[] toc = getTOC(rdr);
         if(toc == null) return(false);
         if(toc.length == 0) return(true);
         
         // next line should be the tag line for the first data section.
         return("0:".equals(rdr.readLine()));        
      }
      catch(IOException ioe) { System.out.println("Got IOExc: " + ioe.getMessage()); return(false); }
      finally
      {
         try { if(rdr != null) rdr.close(); } catch(IOException ioe) {}
      }
   }
   
   /** 
    * Construct an annotated-text data source proxy that reads/writes data sets from/to the specified file.
    * @param f The abstract pathname of the annotated-text source file. The file is not opened in this constructor. It 
    * may not exist, in which case only the write operations will be available initially.
    */
   AnnotatedTextSrc(File f) { srcPath = f; }
   
   public File getSourceFile() { return(srcPath); }
   public String getLastError() { return(lastErrorMsg); }
   public boolean isUnusable() { return(srcPath==null || (!srcPath.isFile()) || tableOfContents == null); }
   public boolean isReadOnly() { return(false); }

   public DataSetInfo[] getSummaryInfo()
   {
      // if there's no TOC in source file, then something is wrong!
      updateTOCCacheIfNecessary();
      if(tableOfContents == null) return(null);
      
      DataSetInfo[] info = new DataSetInfo[tableOfContents.length];
      for(int i=0; i<info.length; i++) info[i] = tableOfContents[i];
      return(info);
   }

   public DataSet getDataByID(String id)
   {
      // if there's no TOC in source file, then something is wrong!
      updateTOCCacheIfNecessary();
      if(tableOfContents == null) return(null);
      
      // find TOC entry corresponding to specified ID
      int tocIndex = -1;
      for(int i=0; i<tableOfContents.length; i++) if(tableOfContents[i].getID().equals(id))
      {
         tocIndex = i;
         break;
      }
      if(tocIndex < 0)
      {
         lastErrorMsg = "Dataset ID not found!";
         return(null);
      }
      
      // compute the number of lines that need to be read to get to the tag line of the requested data section. Each 
      // RASTER1D section passed over has (1+M) lines in addition to the tag line, while all other data sections have 
      // N lines in addition to the tag line -- where N is the data length and M is the data breadth.
      int nSkip = 1 + tableOfContents.length;
      for(int i=0; i<tocIndex; i++)
      {
         DataSetInfo info = tableOfContents[i];
         if(info.getFormat() == DataSet.Fmt.RASTER1D) nSkip += 2 + info.getDataBreadth();
         else nSkip += 1 + info.getDataLength();
      }
      
      // open a buffered reader for the file, skip the number of lines computed above, check for the tag line of the 
      // requested set, then read in and parse the required number of lines to obtain the requested data matrix
      LineNumberReader rdr = null;
      DataSet ds = null;
      try
      {
         // create a buffered reader for reading the ascii text file one line at a time
         rdr = new LineNumberReader( new InputStreamReader( new FileInputStream(srcPath), "us-ascii" ) );
         
         // skip to the beginning of the data section for the requested dataset
         for(int i=0; i<nSkip; i++) 
         { 
            if(null == rdr.readLine()) throw new IOException("Unexpected EOF at line " + rdr.getLineNumber());
         }
         
         // parse data section and create the DataSet object.
         ds = getDataSet(rdr, tocIndex, tableOfContents[tocIndex]);
      }
      catch(IOException ioe) { lastErrorMsg = ioe.getMessage(); }
      finally
      {
         try { if(rdr != null) rdr.close(); } catch(IOException ioe) {}
      }
      return(ds);
   }

   public boolean writeData(DataSet set, boolean replace)
   {
      lastErrorMsg = "";
      
      // if no set is provided, return success
      if(set == null) return(true); 
      
      // if file exists, retrieve current table of contents and see if file contains a data set with the same ID
      int iMatch = -1;
      if(srcPath.isFile())
      {
         updateTOCCacheIfNecessary();
         if(tableOfContents == null) 
         {
            lastErrorMsg = "Cannot append data sets to an incorrectly formatted source file!";
            return(false);
         }
         else for(int i=0; i<tableOfContents.length; i++)
         {
            if(tableOfContents[i].getID().equals(set.getID()))
            {
               iMatch = i;
               break;
            }
         }
      }

      // if replace flag not set and there already exists a data set with the same ID, fail.
      if(iMatch >= 0 && !replace)
      {
         lastErrorMsg = "Source already contains a dataset with ID=" + set.getID() + ". Replace?";
         return(false);
      }
      
      // we always write to a temporary location, UNLESS the source file does not yet exist.
      File dst = DataSrcFactory.getTempFilePath(srcPath);
      if(dst == null)
      {
         lastErrorMsg = "Unable to generate a temp file name (bad source file path?). Write failed!";
         return(false);
      }
      
      // construct new TOC. Whether we replace an existing data set or simply append, the new data set will be located
      // at the end of the file and the TOC.
      DataSetInfo[] dstTOC = null;
      if(tableOfContents == null)
         dstTOC = new DataSetInfo[] {set.getInfo()};
      else
      {
         int n = (replace && iMatch >= 0) ? tableOfContents.length : tableOfContents.length + 1;
         dstTOC = new DataSetInfo[n];
         
         int j = 0;
         for(int i=0; i<tableOfContents.length; i++)
         {
            if(replace && i==iMatch) continue;
            dstTOC[j++] = tableOfContents[i];
         }
         dstTOC[n-1] = set.getInfo();
      }
      
      BufferedReader in = null;
      BufferedWriter out = null;
      boolean ok = true;
      try
      {
         if(srcPath.isFile()) in = new BufferedReader( new InputStreamReader( new FileInputStream(srcPath), "us-ascii" ) );
         out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dst), "us-ascii"));
         
         // write the new header line and TOC
         putTOC(out, dstTOC);
         
         // copy all existing data sections directly, except the one replaced (if applicable)
         if(in != null)
         {
            // first, skip over the header line and the TOC in the old file
            for(int i=0; i<tableOfContents.length + 1; i++)
            { 
               if(null == in.readLine()) throw new IOException("Unexpected EOF while copying existing data");
            }
            
            for(int i=0; i<tableOfContents.length; i++)
            {
               int nLines = 1;
               nLines += (tableOfContents[i].getFormat() == DataSet.Fmt.RASTER1D) ? 
                        (1+tableOfContents[i].getDataBreadth()) : tableOfContents[i].getDataLength();
               
               // if we're replacing a data set, we have to skip over the old data set, then fix the tag line for each
               // dataset after it to ensure we get the index position right
               if(replace && iMatch >= 0) 
               {
                  if(i==iMatch)
                  {
                     for(int k=0; k<nLines+1; k++) in.readLine();
                     continue;
                  }
                  else if(i>iMatch)
                  {
                     in.readLine();
                     out.write(Integer.toString(i-1) + ":" + CRLF);
                  }
               }
                        
               // NOTE: We have to add the CRLF back in b/c the readLine() removes it!
               for(int j=0; j<nLines; j++)
               {
                  String line = in.readLine();
                  if(line == null) throw new IOException("Unexpected EOF while copying existing data");
                  out.write(line + CRLF);
               }
            }
         }
         
         // now write the data section for the added data set
         putDataSet(out, dstTOC.length - 1, set);
      }
      catch(IOException ioe)
      {
         lastErrorMsg = ioe.getMessage();
         ok = false;
      }
      finally
      {
         try 
         { 
            if(in != null) in.close(); 
            if(out != null) out.close();
         }
         catch(IOException ioe) {}
      }
      
      // file successfully written. If we wrote to a temp file, now delete old file and move temp to its place. If 
      // delete succeeds but we cannot move temp file, leave temp file and inform user via error message. If file was
      // not successfully written, delete the temp file.
      if(ok)
      {
         if(!dst.equals(srcPath))
         {
            ok = srcPath.delete();
            if(!ok) 
            {
               dst.delete();
               lastErrorMsg = "Unable to remove old source file. Contents unchanged.";
            }
            else
            {
               ok = dst.renameTo(srcPath);
               if(!ok) lastErrorMsg = "Old source file deleted, but cannot move temp file. New contents found in:\n" +
                     dst.getAbsolutePath();
            }
         }
      }
      else dst.delete();
      
      // if successful, update TOC cache
      if(ok)
      {
         srcLastModified = srcPath.lastModified();
         tableOfContents = dstTOC;
      }
      
      return(ok);
   }

   public boolean changeID(String id, String idNew)
   {
      lastErrorMsg = "";
      
      // if there's no TOC in source file, then something is wrong!
      updateTOCCacheIfNecessary();
      if(tableOfContents == null) return(false);
      
      // find TOC entry corresponding to specified ID, and make sure new ID is not already there
      int tocIndex = -1;
      int dupIndex = -1;
      for(int i=0; i<tableOfContents.length; i++) 
      {
         if(tocIndex < 0 && tableOfContents[i].getID().equals(id)) tocIndex = i;
         if(dupIndex < 0 && tableOfContents[i].getID().equals(idNew)) dupIndex = i;

         if(tocIndex > -1 && dupIndex > -1) break;
      }
      if(tocIndex < 0)
      {
         lastErrorMsg = "Dataset ID not found!";
         return(false);
      }
      if(dupIndex > -1)
      {
         // note the trivial case: ID unchanged!
         if(dupIndex != tocIndex) lastErrorMsg = "Candidate ID duplicates that of an existing dataset!";
         return(dupIndex == tocIndex);
      }
      
      if(!DataSet.isValidIDString(idNew))
      {
         lastErrorMsg = "Candidate ID is not a valid DataNav dataset identifier!";
         return(false);
      }
      
      // we always write to a temporary location. Original source file MUST exist.
      File dst = DataSrcFactory.getTempFilePath(srcPath);
      if(dst == null)
      {
         lastErrorMsg = "Unable to generate a temp file name (bad source file path?). Write failed!";
         return(false);
      }
      
      // construct new TOC, replacing the relevant entry. That's the only change made to the file!!
      DataSetInfo[] dstTOC = new DataSetInfo[tableOfContents.length];
      for(int i=0; i<tableOfContents.length; i++) 
      {
         if(i == tocIndex) dstTOC[i] = DataSetInfo.changeID(tableOfContents[i], idNew);
         else dstTOC[i] = tableOfContents[i];
      }

      BufferedReader in = null;
      BufferedWriter out = null;
      boolean ok = true;
      try
      {
         out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dst), "us-ascii"));
         in = new BufferedReader( new InputStreamReader( new FileInputStream(srcPath), "us-ascii" ) );
         
         // write the new header line and TOC
         putTOC(out, dstTOC);
         
         // first, skip over the header line and the TOC in the old file
         for(int i=0; i<tableOfContents.length + 1; i++)
         { 
            if(null == in.readLine()) throw new IOException("Unexpected EOF while copying existing data");
         }
         
         // copy all existing data sections directly
         for(int i=0; i<tableOfContents.length; i++)
         {
            int nLines = 1;
            nLines += (tableOfContents[i].getFormat() == DataSet.Fmt.RASTER1D) ? 
                     (1+tableOfContents[i].getDataBreadth()) : tableOfContents[i].getDataLength();
            
            for(int j=0; j<nLines; j++)
            {
               String line = in.readLine();
               if(line == null) throw new IOException("Unexpected EOF while copying existing data");
               out.write(line);
            }
         }
      }
      catch(IOException ioe)
      {
         lastErrorMsg = ioe.getMessage();
         ok = false;
      }
      finally
      {
         try 
         { 
            if(in != null) in.close(); 
            if(out != null) out.close();
         }
         catch(IOException ioe) {}
      }
      
      // file successfully written. If we wrote to a temp file, now delete old file and move temp to its place. If 
      // delete succeeds but we cannot move temp file, leave temp file and inform user via error message. If file was
      // not successfully written, delete the temp file.
      if(ok)
      {
         if(!dst.equals(srcPath))
         {
            ok = srcPath.delete();
            if(!ok) 
            {
               dst.delete();
               lastErrorMsg = "Unable to remove old source file. Contents unchanged.";
            }
            else
            {
               ok = dst.renameTo(srcPath);
               if(!ok) lastErrorMsg = "Old source file deleted, but cannot move temp file. New contents found in:\n" +
                     dst.getAbsolutePath();
            }
         }
      }
      else dst.delete();
      
      // if successful, update TOC cache
      if(ok)
      {
         srcLastModified = srcPath.lastModified();
         tableOfContents = dstTOC;
      }
      
      return(ok);
   }
   
   public boolean removeData(String id)
   {
      lastErrorMsg = "";
      
      // if source file does not exist, then return successfully
      if(!srcPath.isFile()) return(true);
      
      // if there's no TOC in source file, then something is wrong!
      updateTOCCacheIfNecessary();
      if(tableOfContents == null) return(false);
      
      // find TOC entry corresponding to specified ID. If it's not there, then return successfully.
      int iRemove = -1;
      for(int i=0; i<tableOfContents.length; i++) if(tableOfContents[i].getID().equals(id))
      {
         iRemove = i;
         break;
      }
      if(iRemove < 0) return(true);
      
      // special case: removing the last data set in source file!
      if(tableOfContents.length == 1) return(removeAll());
      
      // we always write to a temporary location. Original source file MUST exist.
      File dst = DataSrcFactory.getTempFilePath(srcPath);
      if(dst == null)
      {
         lastErrorMsg = "Unable to generate a temp file name (bad source file path?). Write failed!";
         return(false);
      }
      
      // construct new TOC
      DataSetInfo[] dstTOC = new DataSetInfo[tableOfContents.length-1];
      int j=0;
      for(int i=0; i<tableOfContents.length; i++) if(i != iRemove)
      {
         dstTOC[j++] = tableOfContents[i];
      }
      
      BufferedReader in = null;
      BufferedWriter out = null;
      boolean ok = true;
      try
      {
         out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dst), "us-ascii"));
         in = new BufferedReader( new InputStreamReader( new FileInputStream(srcPath), "us-ascii" ) );
         
         // write the new header line and TOC
         putTOC(out, dstTOC);
         
         // first, skip over the TOC in the old file
         for(int i=0; i<tableOfContents.length + 1; i++)
         { 
            if(null == in.readLine()) throw new IOException("Unexpected EOF while copying existing data");
         }
         
         // copy all existing data sections, EXCEPT the one removed! We must revise the tag line for each section to
         // ensure we get the index position right.
         j = 0;
         for(int i=0; i<tableOfContents.length; i++)
         {
            // number of lines for current data section, excluding tag line
            int nLines = (tableOfContents[i].getFormat() == DataSet.Fmt.RASTER1D) ? 
                  (1+tableOfContents[i].getDataBreadth()) : tableOfContents[i].getDataLength();
         
            if(i == iRemove) 
            {
               for(int k=0; k<nLines+1; k++) in.readLine();
               continue;
            }
            
            // we revise the tag line for each section to ensure we get the index position right
            in.readLine();
            out.write(Integer.toString(j) + ":" + CRLF);
            
            // NOTE: We have to add the CRLF back in b/c the readLine() removes it!
            for(int k=0; k<nLines; k++)
            {
               String line = in.readLine();
               if(line == null) throw new IOException("Unexpected EOF while copying existing data");
               out.write(line + CRLF);
            }
            
            ++j;
         }
      }
      catch(IOException ioe)
      {
         lastErrorMsg = ioe.getMessage();
         ok = false;
      }
      finally
      {
         try 
         { 
            if(in != null) in.close(); 
            if(out != null) out.close();
         }
         catch(IOException ioe) {}
      }
      
      // file successfully written. If we wrote to a temp file, now delete old file and move temp to its place. If 
      // delete succeeds but we cannot move temp file, leave temp file and inform user via error message. If file was
      // not successfully written, delete the temp file.
      if(ok)
      {
         if(!dst.equals(srcPath))
         {
            ok = srcPath.delete();
            if(!ok) 
            {
               dst.delete();
               lastErrorMsg = "Unable to remove old source file. Contents unchanged.";
            }
            else
            {
               ok = dst.renameTo(srcPath);
               if(!ok) lastErrorMsg = "Old source file deleted, but cannot move temp file. New contents found in:\n" +
                     dst.getAbsolutePath();
            }
         }
      }
      else dst.delete();
      
      // if successful, update TOC cache
      if(ok)
      {
         srcLastModified = srcPath.lastModified();
         tableOfContents = dstTOC;
      }
      
      return(ok);
   }

   public boolean removeAll()
   {
      lastErrorMsg = "";
      
      // we always write to a temporary location, UNLESS source file does not yet exist.
      File dst = DataSrcFactory.getTempFilePath(srcPath);
      if(dst == null)
      {
         lastErrorMsg = "Unable to generate a temp file name (bad source file path?). Write failed!";
         return(false);
      }
      
      // write an empty TOC to the destination file.
      DataSetInfo[] dstTOC = new DataSetInfo[0];
      BufferedWriter out = null;
      boolean ok = true;
      try
      {
         out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dst), "us-ascii"));
         
         // write the new header line with an empty TOC
         putTOC(out, dstTOC);
      }
      catch(IOException ioe)
      {
         lastErrorMsg = ioe.getMessage();
         ok = false;
      }
      finally
      {
         try { if(out != null) out.close(); } catch(IOException ioe) {}
      }
      
      // file successfully written. If we wrote to a temp file, now delete old file and move temp to its place. If 
      // delete succeeds but we cannot move temp file, leave temp file and inform user via error message. If file was
      // not successfully written, delete the temp file.
      if(ok)
      {
         if(!dst.equals(srcPath))
         {
            ok = srcPath.delete();
            if(!ok) 
            {
               dst.delete();
               lastErrorMsg = "Unable to remove old source file. Contents unchanged.";
            }
            else
            {
               ok = dst.renameTo(srcPath);
               if(!ok) lastErrorMsg = "Old source file deleted, but cannot move temp file. New contents found in:\n" +
                     dst.getAbsolutePath();
            }
         }
      }
      else dst.delete();
      
      // if successful, update TOC cache
      if(ok)
      {
         srcLastModified = srcPath.lastModified();
         tableOfContents = dstTOC;
      }
      
      return(ok);
   }
   
   private final static String HEADERTAG = "@DN";
   private final static String CURRVERSIONSTR = "0";

   private final static String COMMA = ",";
   private final static String CRLF = "\r\n";
   
   /** The abstract pathname for the data source file. */
   private File srcPath = null;
   
   /** Source file's modification time the last time we cached TOC information extracted from it. */
   private long srcLastModified = -1;
   
   /** Cache of source file's table of contents. Will be null if it has not been cached, or an error occurred. */
   private DataSetInfo[] tableOfContents = null;
   
   /** Description of error that occurred during last operation, or empty string if operation was successful. */
   private String lastErrorMsg = "";
   
   /**
    * Helper method checks if source file has changed since the last time we cached table-of-contents information 
    * extracted from it. If so, the cache is discarded and the table of contents is read in again. If unable to get 
    * the table of contents, {@link #getLastError()} will return the reason for the failure.
    */
   private void updateTOCCacheIfNecessary()
   {
      // fail immediately if path is not specified or does not exist
      if(srcPath == null || !srcPath.isFile())
      {
         lastErrorMsg = (srcPath==null) ? "File unspecified" : "File not found";
         return;
      }

      if(srcLastModified > 0 && srcLastModified != srcPath.lastModified())
      {
         srcLastModified = -1;
         tableOfContents = null;
      }
      if(tableOfContents != null) return;
      
      long modT = srcPath.lastModified();


      BufferedReader rdr = null;
      DataSetInfo[] toc = null;
      try
      {
         // create a buffered reader for reading the ascii text file one line at a time
         rdr = new BufferedReader( new InputStreamReader( new FileInputStream(srcPath), "us-ascii" ) );
         
         // parse header line and TOC
         toc = getTOC(rdr);
         if(toc == null) throw new IOException("Invalid TOC. Not a DataNav annotated text data source.");
      }
      catch(IOException ioe) { lastErrorMsg = ioe.getMessage(); }
      finally
      {
         try { if(rdr != null) rdr.close(); } catch(IOException ioe) {}
      }

      srcLastModified = modT;
      tableOfContents = toc;
   }   

   /**
    * Helper method parses the header line and "table of contents" (TOC) section of the annotated-text data source. Each
    * line in the TOC should have the form "ID,FMT,NROWS,NCOLS[,PARAM0,PARAM1,PARAM2,PARAM3]". See class header for a
    * detailed description of the header line and the TOC fields.
    * @param rdr A reader supplying lines read from the data source file. <i>It is assumed that the next line read from 
    * this reader will be the first line of the file.</i> After a successful invocation, the next line read should be 
    * the tag line of the first data section (unless the file has no data sets).
    * @return If successful, an array holding the data set information gleaned from the table-of-contents section. If 
    * the source is correctly formatted but contains no data sets, this will be an empty array. If the source is not 
    * correctly formatted, the method returns null. <b>NOTE</b>: If the method is successful but there are duplicate 
    * data set identifiers among the TOC entries, the duplicates are silently modified to ensure uniqueness.
    */
   private static DataSetInfo[] getTOC(BufferedReader rdr) throws IOException
   {
      String line = rdr.readLine();
      if(line == null) return(null);
      StringTokenizer tokenizer = new StringTokenizer(line, COMMA);
      if(tokenizer.countTokens() != 3) return(null);
      if(!HEADERTAG.equals(tokenizer.nextToken())) return(null);
      if(!CURRVERSIONSTR.equals(tokenizer.nextToken())) return(null);

      int n = 0;
      try { n = Integer.parseInt(tokenizer.nextToken()); } catch(NumberFormatException nfe) { return(null); }
      if(n < 0) return(null);

      DataSetInfo[] toc = new DataSetInfo[n];
      for(int i=0; i<n; i++)
      { 
         tokenizer = new StringTokenizer(rdr.readLine(), COMMA);
         int nTokens = tokenizer.countTokens();
         if(nTokens < 4) return(null);
         
         String id = tokenizer.nextToken().trim();
         if(!DataSet.isValidIDString(id)) return(null); 

         DataSet.Fmt fmt = null;
         int nrows = -1;
         int ncols = -1;
         try 
         {
            fmt = DataSet.Fmt.getFormatByIntCode(Integer.parseInt(tokenizer.nextToken())); 
            nrows = Integer.parseInt(tokenizer.nextToken());
            ncols = Integer.parseInt(tokenizer.nextToken());
         } 
         catch(NumberFormatException nfe) { return(null); }

         if(fmt == null || nrows < 0 || ncols < 0) return(null);
         
         int nParams = fmt.getNumberOfParams();
         if(nTokens < 4 + nParams) return(null);
         
         float[] params = new float[nParams];
         try
         {
            for(int j=0; j<nParams; j++) params[j] = Float.parseFloat(tokenizer.nextToken());
         }
         catch(NumberFormatException nfe) { return(null); }
         
         DataSetInfo info = DataSetInfo.createDataSetInfo(id, fmt, nrows, ncols, params);
         if(info == null) return(null);
         
         toc[i] = info;
      }
      
      DataSet.ensureUniqueIdentifiers(toc);
      return(toc);
   }

   /**
    * Helper method writes the header line and the specified table of contents (TOC), in the format required for an 
    * annotated-text data source file. See class header for a detailed description of the header line and TOC layout.
    * @param writer The buffered output stream. It is assumed that the writer is positioned at the beginning of the 
    * output stream. 
    * @param toc The table of contents to be written.
    * @return True if successful; false otherwise.
    */
   private static void putTOC(BufferedWriter writer, DataSetInfo[] toc) throws IOException
   {
      writer.write(HEADERTAG + COMMA + CURRVERSIONSTR + COMMA + toc.length + CRLF);
      
      for(int i=0; i<toc.length; i++)
      {
         DataSetInfo info = toc[i];
         String line = info.getID() + COMMA + info.getFormat().getIntCode() + COMMA + info.getDataLength() + COMMA +
               info.getDataBreadth();
         for(int j=0; j<info.getFormat().getNumberOfParams(); j++)
         {
            line += COMMA + Utilities.toString(info.getParam(j), 6, -1);
         }
         line += CRLF;
         writer.write(line);
      }
   }

   /**
    * Helper method that extracts and parses the data section for a specified table-of-contents entry in a source file 
    * conforming to the layout expected for an annotated-text data source. See class header for a description of the 
    * data section layout in the source file, and see {@link DataSet#createDataSet(DataSetInfo, float[])} for a 
    * description of how the raw data array should be organized for each data format. This method handles the process of
    * extracting the data from the text file and packing it into the data array correctly.
    * @param rdr A text line reader providing access to the file content. <b>It is assumed that the next line delivered 
    * by this reader is the initial tag line of the requisite data section. If not, the method will fail.</b>
    * @param pos Zero-based index position, P, of the data set in the file. The initial tag line of the requisite data 
    * section must have the form "{P}:", where {P} is the ASCII string representation of the integer value P. The 
    * method will fail if this is not the case.
    * @param entry Summary information for the data set requested. This includes information needed to correctly parse 
    * the lines in the data section and to create the {@link DataSet} object that will wrap the raw data array.
    * @return The extracted data set.
    * @throws IOException if an IO error occurs or a file format problem is detected.
    */
   private static DataSet getDataSet(LineNumberReader rdr, int pos, DataSetInfo info) throws IOException
   {
      // first line should be the tag line for the requested data section. If not, something is wrong.
      String tagLine = Integer.toString(pos) + ":";
      if(!tagLine.equals(rdr.readLine())) throw new IOException("Bad tag line for requested dataset");
      
      int n = info.getDataLength();
      int m = info.getDataBreadth();
      float[] fData = null;
      if(info.getFormat() == DataSet.Fmt.RASTER1D)
      {
         // RASTER1D is fundamentally different from the other types in how it is stored. The first line is the set of M
         // individual raster lengths (note that it will be an empty line if its an empty raster collection!), and the 
         // next M lines hold the samples for each raster. It must be the case that the number of samples extracted in 
         // the first line equal the number of tokens parsed from the the corresponding raster sample line.
         
         fData = new float[m+n];

         StringTokenizer tokenizer = new StringTokenizer(rdr.readLine());
         if(m != tokenizer.countTokens()) 
            throw new IOException("Incorrect # of raster lengths for RASTER1D at line " + rdr.getLineNumber());
         
         try
         {
            int sum = 0;
            for(int i=0; i<m; i++) 
            {
               int rasterlen = Integer.parseInt(tokenizer.nextToken());
               sum += rasterlen;
               fData[i] = (int) rasterlen;
            }
            if(sum != n) throw new IOException("Sum of raster lengths invalid at line " + rdr.getLineNumber());     

            int k = m;
            for(int i=0; i<m; i++)
            {
               tokenizer = new StringTokenizer(rdr.readLine());
               if(tokenizer.countTokens() != (int) fData[i])
                  throw new IOException("Unexpected #samples in raster " + i + " at line " + rdr.getLineNumber());
               while(tokenizer.hasMoreTokens())
                  fData[k++] = Float.parseFloat(tokenizer.nextToken());
            }
         }
         catch(NumberFormatException nfe)
         {
            throw new IOException("Number parsing error on line " + rdr.getLineNumber());
         }
      }
      else
      {
         // for other formats, the translation from source file format to the 1D array is simple. The source should have
         // N lines of M floating-pt tokens, and these tokens are parsed to floats and stored in the array in order,
         // one line ("row" in data matrix) at a time.
         
         fData = new float[m*n];
         try
         {
            int k = 0;
            for(int i=0; i<n; i++)
            {
               StringTokenizer tokenizer = new StringTokenizer(rdr.readLine());
               if(tokenizer.countTokens() != m)
                  throw new IOException("Incorrect tuple length at line " + rdr.getLineNumber());
               while(tokenizer.hasMoreTokens())
                  fData[k++] = Float.parseFloat(tokenizer.nextToken());
            }
         }
         catch(NumberFormatException nfe)
         {
            throw new IOException("Number parsing error on line " + rdr.getLineNumber());
         }
      }
      
      // create the data set object
      DataSet ds = DataSet.createDataSet(info, fData);
      if(ds == null) throw new IOException("Unable to create dataset after parsing data section; invalid data?");
      return(ds);
   }
   
   /**
    * Helper method that writes a data section conforming to the layout expected for an annotated-text data source. This 
    * method handles the process of writing the data as a series of whitespace-separated floating-point numbers on one 
    * or more lines IAW the expected layout.
    * @param wrt A buffered writer providing write access to the file. <b>It is assumed that the writer is correctly 
    * positioned. If this is not the case, the resulting file will be corrupted.</b>
    * @param pos Zero-based index position, P, of the data set in the file. It is used to write he initial tag line of 
    * the data section -- "{P}:" -- where {P} is the ASCII string representation of the integer value P.
    * @param ds The data set to be written.
    * @throws IOException if an IO error occurs.
    */
   private static void putDataSet(BufferedWriter wrt, int pos, DataSet ds) throws IOException
   {
      // write the tag line for the data section 
      wrt.write(Integer.toString(pos) + ":" + CRLF);
      
      int n = ds.getDataLength();
      int m = ds.getDataBreadth();
      float[] fData = ds.getRawDataArray();
      
      if(ds.getFormat() == DataSet.Fmt.RASTER1D)
      {
         // RASTER1D is fundamentally different from the other types in how it is stored. The first line is the set of M
         // individual raster lengths (note that it will be an empty line if its an empty raster collection!), and the 
         // next M lines hold the samples for each raster. It must be the case that the number of samples extracted in 
         // the first line equal the number of tokens parsed from the the corresponding raster sample line.
         int maxLen = 0;
         int[] rasterLen = new int[m];
         for(int i=0; i<m; i++) 
         {
            rasterLen[i] = ds.getDataSize(i);
            maxLen = Math.max(maxLen, rasterLen[i]);
         }
         String line = Utilities.toString(rasterLen) + CRLF;
         wrt.write(line);
         
         int k = m;
         for(int i=0; i<m; i++)
         {
            line = Utilities.toString(fData, k, rasterLen[i], 6, -1) + CRLF;
            wrt.write(line);
            k += rasterLen[i];
         }
      }
      else
      {
         // for other formats, the translation from source file format to the 1D array is simple. The source should have
         // N lines of M floating-pt tokens, and these tokens are parsed to floats and stored in the array in order,
         // one line ("row" in data matrix) at a time.
         int k = 0;
         for(int i=0; i<n; i++)
         {
            String line = Utilities.toString(fData, k, m, 6, -1) + CRLF;
            wrt.write(line);
            k += m;
         }
      }
   }
}
