package com.srscicomp.fc.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;
import java.util.StringTokenizer;

import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet.Fmt;

/**
 * This application singleton serves as a factory for sources that can store and provide {@link DataSet}s. For any file 
 * system object consistent with one of the supported data source formats, the factory class provides an {@link 
 * IDataSrc} instance that can read the source file, and write it if the implementation is NOT read-only. 
 * 
 * @author sruffner
 */
public final class DataSrcFactory
{
   /** The singleton instance, created at class-load time. */
   private final static DataSrcFactory instance = new DataSrcFactory();
   
   // Private constructor suppresses generation of a (public) default constructor
   private DataSrcFactory() {}

   /**
    * Get an instance of a data source factory.
    * @return A factory object which will provide the {@link IDataSrc} instance appropriate to any file format 
    * recognized by the factory as a source for {@link DataSet} objects.
    */
   public static DataSrcFactory getInstance() { return(instance); }

   /**
    * Is the specified file consistent with one of the supported data set source file formats? <i>As of FC 5.4.0,
    * the specified file must have one of these four accepted extensions -- .dna, .txt, .dnr, .dnb.</li>
    * @param f The file to check.
    * @return True if file exists and its content conforms to a supported data set source file format.
    */
   public static boolean isDataSource(File f)
   {
      String ext = Utilities.getExtension(f);
      if(!("txt".equals(ext) || "dna".equals(ext) || "dnr".equals(ext) || "dnb".equals(ext)))
         return(false);
      boolean ok = false;
      if(f.isFile())
      {
         ok = BinarySrc.checkFile(f);
         if(!ok) ok = DeprecatedBinarySrc.checkFile(f);
         if(!ok) ok = AnnotatedTextSrc.checkFile(f);
         if(!ok) ok = PlainTextSrc.checkFile(f);
      }
      return(ok);
   }
   
   /**
    * Create a {@link IDataSrc} instance that can read and, optionally, write the specified data set source file. 
    * Currently, four source file formats are supported.
    * <ol>
    * <li>The fastest and most versatile is the binary data set repository file (preferred extension ".dnr"), which is
    * a random-access binary format that optimizes the speed at which data sets can be retrieved, added and removed from
    * the physical file. Disadvantages: (1) Since file is modified in place, a catastrophic I/O failure can leave it in
    * a corrupted state. (2) As data sets are added and removed, the amount of wasted space in the file may get out of
    * hand. It can be fixed by compacting the file, but that functionality is not exposed by {@link IDataSrc}.</li>
    * <li>An older, more fail-safe binary format (preferred extension ".dnb"), which includes a table of contents with 
    * file offset information so that accessing a selected data set within the file can be done quickly. Disadvantage: 
    * The fail-safe design mandated that any modification to the file involve a complete rewrite to a temporary file to 
    * guard against data loss in the event of a catastrophic IO failure. The tradeoff is a dramatic decline in 
    * performance as more data sets are added to the file. <i><b>Deprecated.</b> All newly created binary data set 
    * source files (as of Jan 2010) will adhere to the random-access binary format.</i></li>
    * <li>Less efficient, but perhaps easier to pass across HTTP, is the annotated-text format (preferred extension
    * ".dna"). This format also includes a table of contents to speed up access to individual data sets. Disadvantages:
    * (1) Larger file size compared to binary format. (2) Slower read and write access. Even with table of contents, 
    * must still scan through file sequentially to retrieve any individual set. (3) Every file modification requires a
    * complete file rewrite.</li>
    * <li>Finally, a <b>read-only</b> {@link IDataSrc} implementation provides access to data set files originally 
    * created for use in <i>Phyplot</i>, <i>FigureComposer</i>'s predecessor (no preferred extension). These files 
    * contain no table of contents and little or no information on data set size, format, etc. Data is presented as a 
    * series of tuples, one per line, where each "tuple" is a set of whitespace-separated floating-point tokens. Blank 
    * lines separated one data set from the next. Optionally, the first line of a data set definition could be a header 
    * including the data set ID. This implementation is very inefficient and is provided only so that users can import 
    * the old data source files into <i>FC</i>. Disadvantages: (1) Slowest performance. (2) Largest size. (3) Lack 
    * of meta-data means that content may be ambiguous.</li>
    * </ol>
    * 
    * @param f The data set source file. Cannot be null.
    * @param textonly. If the file does not exist and this flag is set, the annotated plain text (US-ASCII) format is
    * selected. If the flag is not set, the (much more efficient!) binary format is chosen. If the file already exists,
    * the format is already set and this argument is ignored.
    * @return If the source file already exists, this method returns the data set source implementation to which the
    * file's content conforms. If the content does not conform to any supported file formats, it returns null. If the 
    * source file does not exist, the method returns the implementation for the annotated text or random-access binary 
    * format, depending on the value of the <i>textonly</i> flag.
    */
   public IDataSrc getDataSource(File f, boolean textonly)
   {
      if(f == null) throw new IllegalArgumentException("Null argument!");
      IDataSrc src = null;
      if(f.isFile())
      {
         if(BinarySrc.checkFile(f)) src = new BinarySrc(f);
         else if(DeprecatedBinarySrc.checkFile(f)) src = new DeprecatedBinarySrc(f);
         else if(AnnotatedTextSrc.checkFile(f)) src = new AnnotatedTextSrc(f);
         else if(PlainTextSrc.checkFile(f)) src = new PlainTextSrc(f);
      }
      else
         src = textonly ? new AnnotatedTextSrc(f) : new BinarySrc(f);
      return(src);
   }

   /**
    * This alternate version of the factory method includes a list of data set formats requested by the caller. The list
    * serves as a hint to the {@link IDataSrc} implementation for <i>Phyplot</i>-era "numbers-only" text data. This data
    * source has been extended to support loading newer data formats which did not exist in <i>Phyplot</i>. However, if
    * the plain-text data is "numbers only", the data format cannot be unambiguously determined. In such situations, the
    * data source will choose the first data format in the provided list that is compatible with the parsed data, if
    * one exists; otherwise, it will make a "best guess".
    * <p>The other supported data source file formats are unambiguous and thus ignore the data format list.</p>
    * 
    * @param f The data source file. Cannot be null.
    * @param requestedFmts List of data formats in order of preference, to resolve ambiguities in the "numbers-only"
    * plain-text file format.
    * @param textonly. If the file does not exist and this flag is set, the annotated plain text (US-ASCII) format is
    * selected. If the flag is not set, the (much more efficient!) binary format is chosen. If the file already exists,
    * the format is already set and this argument is ignored.
    * @return If the source file already exists, this method returns the data set source implementation to which the
    * file's content conforms. If the content does not conform to any supported file formats, it returns null. If the 
    * source file does not exist, the method returns the implementation for the annotated text or random-access binary 
    * format, depending on the value of the <i>textonly</i> flag.
    */
   public IDataSrc getDataSource(File f, Fmt[] requestedFmts, boolean textonly)
   {
      if(f == null) throw new IllegalArgumentException("Null argument!");
      IDataSrc src = null;
      if(f.isFile())
      {
         if(BinarySrc.checkFile(f)) src = new BinarySrc(f);
         else if(DeprecatedBinarySrc.checkFile(f)) src = new DeprecatedBinarySrc(f);
         else if(AnnotatedTextSrc.checkFile(f)) src = new AnnotatedTextSrc(f);
         else if(PlainTextSrc.checkFile(f)) src = new PlainTextSrc(f, requestedFmts);
      }
      else
         src = textonly ? new AnnotatedTextSrc(f) : new BinarySrc(f);
      return(src);
   }

   
   /**
    * Helper method intended for use by {@link IDataSrc} implementations. It generates a new non-existent temporary 
    * path for writing a data set source file. The path will be in the same directory as the source file, but its name 
    * will be appended with ".tmpN", where N is chosen such that the resulting file path does not yet exist. <i><b>If 
    * the source file pathname does not exist, it will be returned unchanged.</b></i>
    * @param src An abstract pathname. The temp file name is based on this. Must not be null and must specify an
    * existing parent directory. If this path does not exist, a reference to it is returned -- no temp path is needed!
    * @return A new, non-existent path, as described. Returns null if <i>src</i> is null or does not specify an existing
    * parent directory.
    */
   static File getTempFilePath(File src)
   {
      if(src == null) return(null);
      File dir = src.getParentFile();
      if(dir == null || !dir.isDirectory()) return(null);

      if(!src.exists()) return(src);
      
      int n = 0;
      File tmpPath = new File(dir, src.getName() + ".tmp" + n);
      while(tmpPath.exists())
      {
         ++n;
         tmpPath = new File(dir, src.getName() + ".tmp" + n);
      }
      return(tmpPath);
   }
   
   
   /**
    * <i>For test/debug only.</i> This program reads commands from standard input and writes to standard output. It is 
    * intended for testing the various data set source file implementations in this package. Type "help" to view a list
    * of available command.
    * @param args Command-line arguments are ignored.
    */
   public static void main(String args[])
   {
      DataSrcFactory factory = DataSrcFactory.getInstance();
      IDataSrc source = null;
      
      // create an array of random floats that we'll use to construct dataset for the "write" test
      int nrows = 100;
      int ncols = 10;
      float[] raw = new float[nrows*ncols];
      Random rng = new Random();
      for(int i=0; i<raw.length; i++) raw[i] = rng.nextFloat() * 1000.0f;
      
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      String input = "";
      boolean done = false;
      while(!done)
      {
         // get next command
         System.out.print("\nTestDataSrc> ");
         try
         {
            input = in.readLine();
         }
         catch(IOException ioe)
         {
            System.out.println("Unexpected IO error while reading command:\n   " + ioe.getMessage() + "\nQUITTING!");
            done = true;
         }
         
         String command = "??";
         String arg = null;
         StringTokenizer tokenizer = new StringTokenizer(input);
         int nTokens = tokenizer.countTokens();
         if(nTokens > 0) command = tokenizer.nextToken();
         if(nTokens > 1) arg = tokenizer.nextToken();
         
         // process command
         if("help".equals(command))
         {
            System.out.println("help : Show a list of available commands.");
            System.out.println("select path : Selects file specified by 'path' as the current data source.\n  " +
                  "Be sure to specify a full path. Always begin command session with this command, since all\n  " +
                  "other commands require that a valid data source be defined");
            System.out.println("list : Prints out a summary of all datasets in the current data source.");
            System.out.println("read dsid : Retrieve dataset with identifier 'dsid' from the current source.");
            System.out.println("write dsid : Create an MSERIES dataset with random data and the identifier\n  " +
            		"'dsid', then append it to the current source. If a dataset with the same name already\n  " +
            		"exists, it will be replaced by the new dataset!");
            System.out.println("remove dsid : Remove the dataset with identifier 'dsid' from the current source.");
            System.out.println("removeall : Remove all datasets from the current source.");
            System.out.println("quit: Exit the program.");
         }
         else if("select".equals(command))
         {
            if(arg == null)
            {
               System.out.println("   !!! Argument required specifying pathname of data source file!");
               continue;
            }
            source = factory.getDataSource(new File(arg), false);
            if(source == null)
               System.out.println("   !!! Specified file does not conform to a supported data source file format!");
            else
               System.out.println("   OK." + (source.getSourceFile().exists() ? "" : " (file does not exist)"));
         }
         else if("list".equals(command))
         {
            if(source == null)
               System.out.println("   Failed: Data source file is currently undefined!");
            else
            {
               DataSetInfo[] info = source.getSummaryInfo();
               if(info == null)
                  System.out.println("   Failed: " + source.getLastError());
               else
               {
                  System.out.println("   OK. Source contains " + info.length + " datasets.");
                  for(int i=0; i<info.length; i++)
                     System.out.println("      " + info[i].getShortDescription());
               }
            }
         }
         else if("read".equals(command))
         {
            if(arg == null)
            {
               System.out.println("   !!! Argument required specifying dataset ID!");
               continue;
            }
            if(source == null)
               System.out.println("   Failed: Data source file is currently undefined!");
            else
            {
               DataSet ds = source.getDataByID(arg);
               if(ds == null)
                  System.out.println("   Failed: " + source.getLastError());
               else
                  System.out.println("   OK.");
            }
         }
         else if("write".equals(command))
         {
            if(arg == null)
            {
               System.out.println("   !!! Argument required specifying pathname of data source file!");
               continue;
            }
            if(source == null)
               System.out.println("   Failed: Data source file is currently undefined!");
            else
            {
               DataSet ds = DataSet.createDataSet(arg, Fmt.MSERIES, new float[] {1,0}, nrows, ncols, raw);
               if(!source.writeData(ds, true))
                  System.out.println("   Failed: " + source.getLastError());
               else
                  System.out.println("   OK.");
            }
            
         }
         else if("remove".equals(command))
         {
            if(arg == null)
            {
               System.out.println("   !!! Argument required specifying dataset ID!");
               continue;
            }
            if(source == null)
               System.out.println("   Failed: Data source file is currently undefined!");
            else
            {
               if(!source.removeData(arg)) System.out.println("   Failed: " + source.getLastError());
               else System.out.println("   OK.");
            }
         }
         else if("removeall".equals(command))
         {
            if(source == null)
               System.out.println("   Failed: Data source file is currently undefined!");
            else
            {
               if(!source.removeAll()) System.out.println("   Failed: " + source.getLastError());
               else System.out.println("   OK.");
            }
         }
         else if("quit".equals(command))
            done = true;
         else
            System.out.println("   !!! Command not recognized: " + command);
      }
      
      System.out.println("\n\nTestDataSrc> BYE!");
      System.exit(0);
   }
}
