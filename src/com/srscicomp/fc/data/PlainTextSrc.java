package com.srscicomp.fc.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.srscicomp.fc.data.DataSet.Fmt;

/**
 * This {@link IDataSrc} implementation that supports reading data sets from the ASCII plain-text files that served 
 * <i>FigureComposer</i>'s predecessor, <i>Phyplot</i>. It is a read-only implementation provided so that FC users 
 * can make use of data files originally created for <i>Phyplot</i>; there is no support for storing FC data sets in 
 * this now-discouraged format.
 * 
 * <p><i>(As of 12/11/2008)</i> Some users still prefer to use this source file format to load data sets, because it is 
 * very simple and intuitive (and one does not have to remember the format of any annotation header!). Since these users
 * want to be able to load data conforming to data formats that did not exist in the old <i>Phyplot</i> -- see {@link 
 * Fmt} for the new formats} -- the file format implementation has been extended accordingly. It remains a
 * read-only data source file format, as we want to discourage proliferation of this inefficient and potentially 
 * ambiguous format.</p>
 * 
 * <p><i>Format description</i>. The old <i>Phyplot</i> data source file lacked any kind of identifying header. It is
 * parsed one line at a time, where each line is interpreted as a separate "datum tuple" in a data set. Individual lines
 * must be terminated by "\r", "\n", or "\r\n". A blank line (whitespace and line termination characters only) 
 * terminates a data set. Two formats exist -- an ambiguous "numbers only" format and an unambiguous "annotated" format
 * in which the first non-blank line is a "data set header". For a full description, see the method {@link
 * DataSet#fromOldPlainTextSrcFileFormat(List, int, String, Fmt[], StringBuffer)}, upon which this class relies to parse
 * the set of text lines representing a single data set.</p>
 *  
 * <p>The implementation is inefficient because the storage format is inefficient. There is no "table-of-contents" 
 * information at the beginning of the file, so the entire file must be scanned to get summary information. Since the 
 * {@link IDataSrc} interface requires that the summary information include data set meta-data, we have to parse 
 * every data set during this scan to get the required meta-data. Thus, the initial call may be very slow. Also, the 
 * implementation is memory-heavy, since all data sets are maintained in memory once scanned in.</p>
 * 
 * <p>(08apr2020) We could consider eliminating this data source format altogether. Nowadays, most if not all FC users
 * create draft figures in Matlab which are then imported into FC for further editing. Those who might still use FC 
 * directly to create figures are likely "cutting and pasting" small data sets directly into FC's data set editor panel.
 * Thus, it is likely that any of the data set source file formats are being actively used, let alone this one!</p>
 * 
 * @author sruffner
 */
class PlainTextSrc implements IDataSrc
{
   /**
    * Is the content of the specified file consistent with the expected format of a <i>Phyplot</i>-era ASCII text 
    * data set source file? The method examines the first non-blank line in the file. If it starts with a colon (":"),
    * it verifies that it is a proper header for the "annotated" format. If so, it parses the next line and verifies that 
    * the tuple length is consistent with the specified format. Otherwise, it assumes the "numbers-only" format and 
    * parses two lines (possibly one if a data set consists of only one tuple!), ensuring that each such line is a 
    * whitespace-separated list of one or more floating-pt numbers.
    * @param f The file to test.
    * @return True if file content is consistent with expected format of an ASCII plain-text data source. Returns false 
    * if argument is null, if file does not exist, or if it does not pass the compatibility checks.
    */
   static boolean checkFile(File f)
   {
      if(f == null || !f.isFile()) return(false);

      try(BufferedReader rdr = new BufferedReader(
            new InputStreamReader(new FileInputStream(f), StandardCharsets.US_ASCII)))
      {
         // create a buffered reader for reading the ascii text file one line at a time

         // read in first two non-blank lines. In the "numbers-only" case, it is possible that the first data set in the
         // file has only 1 tuple, in which case we'll only get one line
         int n = 0;
         String[] lines = new String[2];
         while(n < 2)
         {
            String line = rdr.readLine();
            if(line == null) return (false);   // EOF!
            else line = line.trim();
            if(!line.isEmpty()) lines[n++] = line;
            else if(n > 0) break;             // a single-line dataset!
         }

         Annotation a = Annotation.parseAnnotationLine(lines[0]);
         if(a != null && n < 2) return (false);
         int minLen = (a != null) ? a.getMinTupleLen() : 1;
         int maxLen = (a != null) ? a.getMaxTupleLen() : Integer.MAX_VALUE;

         for(int i = (a != null ? 1 : 0); i < n; i++)
         {
            StringTokenizer st = new StringTokenizer(lines[i]);
            int nTokens = st.countTokens();
            if(nTokens < minLen || nTokens > maxLen) return (false);
            try
            {
               while(st.hasMoreTokens()) Float.parseFloat(st.nextToken());
            } catch(Throwable t)
            {
               return (false);
            }
         }
         return (true);
      }
      catch(IOException ioe)
      {
         System.out.println("Got IOExc: " + ioe.getMessage());
         return (false);
      }
   }
   
   /** 
    * Construct a <i>Phyplot</i>-era ASCII plain text data source proxy.
    * @param f The abstract pathname of the data source file. The file is not opened nor validated here. If it does 
    * not exist or is not the correct format, all operations will fail and the proxy will be rendered unusable.
    */
   PlainTextSrc(File f) { this(f, null); }
   
   /** 
    * Construct a <i>Phyplot</i>-era ASCII plain text data source proxy.
    * 
    * <p>Since some users prefer this old source file format (particularly the numbers-only version) despite its 
    * inefficiencies, support is included for reading a set in the file that is in a data format that did not exist in 
    * <i>Phyplot</i>. However, in the numbers-only case, the data format is ambiguous, so this constructor admits a 
    * second argument specifying a list of preferred data formats. If the source file is in the numbers-only form, the
    * data source proxy must guess the format of each data set. If a valid list of preferred formats is specified, the
    * algorithm will try to select the first data format in this list that is consistent with the parsed data set. The 
    * list is ignored if the file is in the annotated form, which is not ambiguous.</p>
    * 
    * @param f The abstract pathname of the data source file. The file is not opened nor validated here. If it does 
    * not exist or is not the correct format, all operations will fail and the proxy will be rendered unusable.
    * @param prefFormats Array of preferred data formats, as described above. Ignored if null or empty.
    */
   PlainTextSrc(File f, Fmt[] prefFormats) 
   {
      srcPath = f;
      if(prefFormats != null && prefFormats.length > 0)
      {
         preferredDSFormats = new Fmt[prefFormats.length];
         System.arraycopy(prefFormats, 0, preferredDSFormats, 0, prefFormats.length);
      }
   }
   
   public File getSourceFile() { return(srcPath); }
   public String getLastError() { return(lastErrorMsg); }
   public boolean isUnusable() { return(isInvalidSrcFile); }
   public boolean isReadOnly() { return(true); }

   public DataSetInfo[] getSummaryInfo()
   {
      reparseIfNecessary();
      if(isInvalidSrcFile) return(null);
      
      DataSetInfo[] info = new DataSetInfo[data.length];
      for(int i=0; i<info.length; i++) info[i] = data[i].getInfo();
      return(info);
   }

   public DataSet getDataByID(String id)
   {
      reparseIfNecessary();
      if(isInvalidSrcFile) return(null);
      
      // return data set with specified ID, if it exists
      DataSet ds = null;
      for(DataSet dataSet : data)
         if(dataSet.getID().equals(id))
         {
            ds = dataSet;
            break;
         }
      
      lastErrorMsg = (ds == null) ? "Dataset (ID=" + id + ") not found in source!" : "";
      return(ds);
   }

   public boolean writeData(DataSet set, boolean replace) { lastErrorMsg = ERR_NOWRITE; return(false); }
   public boolean changeID(String id, String idNew) { lastErrorMsg = ERR_NOWRITE; return(false);  }
   public boolean removeData(String id) { lastErrorMsg = ERR_NOWRITE; return(false);  }
   public boolean removeAll() { lastErrorMsg = ERR_NOWRITE; return(false);  }

   
   private final static String ERR_NOWRITE = 
         "Writing to inefficient, possibly ambiguous Phyplot-era data source is not supported.";
   
   /** Description of error that occurred during last operation, or empty string if operation was successful. */
   private String lastErrorMsg = "";
   
   /** The abstract pathname for the data source file. */
   private final File srcPath;
   
   /**
    * If not null, this is a list of preferred data formats, which guides choosing the format for a parsed data set in 
    * an ambiguous, numbers-only source file.
    */
   private Fmt[] preferredDSFormats = null;
   
   /** Source file's modification time the last time we parsed it. */
   private long srcLastModified = -1;
   
   /** All data sets found in the file. */
   private DataSet[] data = null;
   
   /** Flag set if unable to parse the file the last time it was scanned. */
   private boolean isInvalidSrcFile = false;

   /**
    * Helper method checks if source file has changed since the last time we parsed it for data sets. If so, the cached 
    * data is discarded and the file is parsed again. If parsing fails, {@link #getLastError()} will return the reason
    * for the failure.
    */
   private void reparseIfNecessary()
   {
      // fail immediately if path is not specified or does not exist
      if(srcPath == null || !srcPath.isFile())
      {
         data = null;
         lastErrorMsg = (srcPath==null) ? "File unspecified" : "File not found";
         isInvalidSrcFile = true;
         return;
      }

      // if we've already parsed the file and its file mod time has not changed, then there's nothing to do
      if(srcLastModified > 0)
      {
         if(srcLastModified == srcPath.lastModified()) return;
      }
      
      srcLastModified = srcPath.lastModified();
      data = null;
      isInvalidSrcFile = false;
      lastErrorMsg = "";

      // parse the entire file
      BufferedReader rdr = null;
      List<DataSet> sets = new ArrayList<>();
      StringBuffer errMsgBuf = new StringBuffer();
      try
      {
         // create a buffered reader for reading the ascii text file one line at a time
         rdr = new BufferedReader( new InputStreamReader( new FileInputStream(srcPath), StandardCharsets.US_ASCII) );
         
         // read in and parse data sets
         int nLinesRead = 0;
         List<String> lines = new ArrayList<>();
         boolean done = false;
         while(!done)
         {
            lines.clear();

            // read in all lines until EOF or an empty line is encountered, skipping any initial blank lines. These 
            // lines will define a single data set.
            int start = -1;
            while(true)
            {
               String nextLine = rdr.readLine();
               if(nextLine == null)
               {
                  done = true;
                  break;
               }
               
               ++nLinesRead;
               nextLine = nextLine.trim();
               
               if(start < 0)
               {
                  if(nextLine.isEmpty()) continue;
                  else start = nLinesRead-1;
               }
               else if(nextLine.isEmpty())
                  break;
               
               lines.add(nextLine);
            }
            
            // construct a new data set initialized IAW with the contiguous sequence of lines just read. If no parsing 
            // error occurred, append the new data set to the list of sets loaded thus far.
            if(!lines.isEmpty())
            {
               String defID = "set" + sets.size();
               errMsgBuf.setLength(0);
               DataSet ds = DataSet.fromOldPlainTextSrcFileFormat(lines, start, defID, preferredDSFormats, errMsgBuf);
               if(ds != null)
                  sets.add(ds);
               else
               {
                  lastErrorMsg = errMsgBuf.toString();
                  isInvalidSrcFile = true;
                  done = true;
               }
            }
         }

      }
      catch(FileNotFoundException fnfe) { lastErrorMsg = "File not found!"; isInvalidSrcFile = true; }
      catch(UnsupportedEncodingException uee) { lastErrorMsg = "Cannot handle USASCII!"; isInvalidSrcFile = true; }
      catch(IOException ioe) { lastErrorMsg = ioe.getMessage(); isInvalidSrcFile = true; }
      finally
      {
         try { if(rdr != null) rdr.close(); } catch(IOException ignored) {}
      }

      if(!lastErrorMsg.isEmpty()) return;
      
      data = new DataSet[sets.size()];
      for(int i=0; i<sets.size(); i++) data[i] = sets.get(i);
   }
   
   /**
    * Helper class encapsulating the <i>Phyplot</i>-style annotation header format. For a complete description, see
    * {@link DataSet#fromOldPlainTextSrcFileFormat(List, int, String, Fmt[], StringBuffer)}.
    * @author sruffner
    */
   private static class Annotation
   {
      /**
       * Parse the specified text as a <i>Phyplot</i>-era dataset annotation header.
       * @param line Text string to parse.
       * @return An <code>Annotation</code> instance including the ID, data format, and sample interval extracted 
       * from the header. If the argument is NOT a valid data set annotation header, returns null.
       */
      static Annotation parseAnnotationLine(String line)
      {
         if(!line.startsWith(":")) return(null);
         StringTokenizer st = new StringTokenizer(line.substring(1));
         int nTokens = st.countTokens();
         if(nTokens < 1 || nTokens > 5 || nTokens == 4) return(null);

         Annotation a = new Annotation();
         a.id = st.nextToken();
         a.fmt = null;
         a.dx = 0;
         
         if(nTokens == 1)
         {
            a.fmt = Fmt.RASTER1D;
            return(a);
         }
         else if(nTokens == 5)
         {
            while(st.hasMoreTokens())
            {
               try { Float.parseFloat(st.nextToken()); }
               catch(NumberFormatException nfe) { return(null); }
            }
            a.fmt = Fmt.XYZIMG;
            return(a);
         }
            
         boolean isSampled = (nTokens == 3);
         String token = st.nextToken();
         boolean isMulti = !token.equals("0");
         if(token.equals("2")) 
         {
            isSampled = false;
            a.fmt = Fmt.XYZSET;
         }
         else if(token.equals("3"))
         {
            isSampled = false;
            a.fmt = Fmt.XYZWSET;
         }
         else if(!(isSampled || isMulti)) a.fmt = Fmt.PTSET;
         else if(isSampled && !isMulti) a.fmt = Fmt.SERIES;
         else if(!isSampled) a.fmt = Fmt.MSET;
         else a.fmt = Fmt.MSERIES;

         if(isSampled)
         {
            token = st.nextToken();
            try { a.dx = Float.parseFloat(token);}
            catch(NumberFormatException nfe) { return(null); }
         }

         return(a);
      }
      
      Fmt fmt;
      @SuppressWarnings("unused")
      String id;
      @SuppressWarnings("unused")
      float dx;
      
      int getMinTupleLen() { return(fmt.getMinBreadth()); }
      int getMaxTupleLen() { return(fmt.getMaxBreadth()); }
   }
}
