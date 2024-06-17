package com.srscicomp.fc.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import com.srscicomp.common.util.Utilities;

/**
 * <b>DataSetRepository</b> wraps a single binary file containing a repository of <i>FigureComposer</i> data sets. This 
 * "proprietary" binary file is formatted for random-access in an effort to minimize the amount of file I/O that must be 
 * done to put, get, or remove a data set from the file. It can increase its capacity as needed to accommodate an 
 * indefinite number of sets.
 * 
 * <p>The data repository file format encapsulated by this class was originally developed to persist all data in a 
 * <i>DataNav</i> portal hub's <i>data repository</i>. In that context, every data set is assigned a unique (across the
 * hub) positive integer identifier (UID). The data set UID, a 32-bit strictly positive integer, serves as an index by 
 * which the actual data set content  may be located within the repository file. While the <i>DataNav</i> projects has
 * since been abandoned, this repository file can still serve a general-purpose container for data sets in any of the
 * data formats currently recognized by the <i>Figure Composer</i> application. <b>NOTE</b> that you will see frequent
 * reference to the now-defunct <i>DataNav</i> in this source code file!</p>
 * 
 * <p>To improve performance, the repository proxy maintains an in-memory cache of data sets recently added to or
 * retrieved from the physical file. Only a soft reference to each data set is kept in the cache so that the garbage
 * collector can reclaim the memory used by a cached set as needed.</p>
 * 
 * <h2>Format description</h2>
 * <p>The file begins with an 8-byte tag followed by one or more <i>data sections</i>, each of which contain up to 500 
 * <i>allocation blocks</i>. The tag indicates how many sections are currently in the file. Each section starts with a 
 * <i>block index</i>. The index is present even if there are no sets currently stored in that section, and the file 
 * always contains at least one section, even if it has no data sets! Obviously, this is wasteful, but the repository is
 * intended to house potentially large amounts of data in a manner that optimizes random-access retrieval, addition, and
 * removal of any given data set stored therein. The section index is the "overhead" that makes this possible. Following
 * the index are up to 500 allocation blocks, in which the actual data is stored. Some blocks may be allocated but not 
 * contain a data set because it was previously removed. An allocated but unoccupied block will be reused whenever a 
 * data set that fits within the block is added to the file.</p>
 * 
 * <p>The initial 8-byte tag serves to identify the file as a <i>DataNav</i> repository file:
 * <ul>
 *    <li>Bytes 3-0: The file tag. It has one of two values. When used as the data repository for a portal hub, the 
 *    tag is 0x584E4440, which translates to "@DNX" in ASCII. When used as a general-purpose repository for data sets 
 *    existing outside a portal context, the tag is 0x524E4440, or "@DNR" in ASCII. The tag not only serves to identify 
 *    the file as a <i>DataNav</i> data source repository file, but it also determines the file's endianness. If the tag 
 *    reads as 0x40444E58 (or 0x4044E52) when little-endian byte order is assumed, then the file must have been saved 
 *    in big-endian order.</li>
 *    <li>Bytes 7-4: An integer N indicating the number of sections currently stored in the file.</li>
 * </ul>
 * 
 * Each section in the file begins with the block index for that section, which contains 500 84-byte entries:
 * <ul>
 *    <li>Bytes 3-0: (int) The UID of the data set assigned to this block. If block is unallocated, UID == -1; if block
 *    is allocated but unused (due to a prior data set removal), UID == 0. Note that -1 and 0 are not valid UIDs!</li>
 *    <li>Bytes 11-4: (long) This 8-byte integer is the file offset (in bytes) from the beginning of the file to the 
 *    first byte of the allocated data set block. If block is not yet allocated, offset == 0.</li>
 *    <li>Bytes 15-12: (int) The length of the allocated data set block in bytes. Zero if block not yet allocated.</li>
 *    <li>Bytes 83-16: Summary information on the data set assigned to this block (see {@link DataSetInfo}). If the 
 *    block is unallocated or unused, this section has no valid content. The breakdown:
 *    <ul>
 *       <li>Bytes 55-16: ID string, null-terminated and padded if the ID is less than 40 characters long. Single-byte 
 *       ASCII characters. Must satisfy the constraints defined by {@link DataSet#isValidIDString(String)}.</li>
 *       <li>Bytes 59-56: (int) Data format code. See {@link DataSet.Fmt} for the set of recognized values.</li>
 *       <li>Bytes 63-60: (int) Number of rows in data matrix, <i>N</i>. See {@link DataSet#getDataLength()}.</li>
 *       <li>Bytes 67-64: (int) Number of columns in data matrix, <i>M</i>. See {@link DataSet#getDataBreadth()}.</li>
 *       <li>Bytes 83-68: (float[4]) Four single-precision floating-pt parameters. For the SERIES and MSERIES formats, 
 *       the first two parameters are <i>dx</i> and <i>x0</i>; the remaining ones are unused. For XYZIMG, the four 
 *       elements specify the x- and y-coordinate ranges spanned by the image data Z(x,y): <i>[x0 x1 y0 y1]</i>. For the
 *       other formats, these parameters are unused.</li>
 *    </ul>
 *    </li>
 * </ul>
 * 
 * Immediately following the last entry in the block index is the first allocation block of the section. A data set's 
 * raw data array is stored within the allocated data set block, preceded by the data set UID:
 * <ul>
 *    <li>Bytes 3-0: (int) The UID. This serves to verify the integrity of the index entry.</li>
 *    <li>Bytes 4+ : (float[L]) The raw data array. For all data formats except RASTER1D, it will be a float array of 
 *    length <i>L=N*M</i>. For the four 2D formats, XYZSET, and XYZWSET, the <i>N M</i>-tuples are stored sequentially 
 *    in this array; for XYZIMG, the intensity image data are stored row-wise in the array. The RASTER1D array is 
 *    different. The first <i>M</i> entries are the lengths of the individual rasters in the collection, and the 
 *    remaining <i>N</i>  elements are the raster samples: <i>[n1 n2 .. nM x1(1..n1) x2(1..n2) .. xM(1..nM)]</i>. Note 
 *    that <i>N = n1+n2+ .. +nM</i>. In this case, the array length <i>L=N+M</i>.</li>
 * </ul>
 * </p> 
 * 
 * <p>If there is more than one section in the file, the block index for each section begins immediately after the
 * last allocation block of the preceding section, with no gap. The file's capacity is increased, or grown, by appending
 * a new section to the end of the file. The file is grown only when a data set is added to the file, the last section 
 * currently in the file contains no unallocated blocks, and the data set cannot fit in any existing allocated but 
 * unoccupied blocks in the file. It is always the case that, in a repository file with N sections, the first N-1
 * sections will be fully allocated (but not necessarily fully <i>occupied</i>).</p>
 * 
 * <p><i><b>A word on performance considerations.</b> Almost all file I/O operations by <code>DataSetRepository</code> 
 * are synchronous. This guarantees, for local disk storage, that the bytes have been truly written to the disk when 
 * the file write operation returns, but the tradeoff is a significant throughput penalty. Synchronous file I/O is not 
 * enforced during compaction of the repository, and tests showed a 3X-20X improvement in throughput for asynchronous 
 * over synchronous transfers.</i></p>
 * @author sruffner
 */
public class DataSetRepository
{
   public static void main(String[] args)
   {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      
      // get pathname to metadata repository file to test
      System.out.print("Specify full pathname for dataset repository file\n> ");
      String fname = null;
      try
      {
         fname = in.readLine();
      }
      catch(IOException ioe)
      {
         System.out.println("Unexpected IO error while reading file path:\n   " + ioe.getMessage() + "\nQUITTING!");
      }
      if(fname == null)
         System.exit(0);
      
      File f = new File(fname);
      DataSetRepository dnf = new DataSetRepository(f, false);
      if(!dnf.preload())
      {
         System.out.println("Error: " + dnf.getFailureReason());
         System.exit(0);
      }
      else
         System.out.println("Success! isEmpty() == " + (dnf.getCount() == 0));
      
      
      String input = "";
      boolean done = false;
      while(!done)
      {
         // get next command
         System.out.print("\n> ");
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
         String arg1 = null;
         String arg2 = null;
         StringTokenizer tokenizer = new StringTokenizer(input);
         int nTokens = tokenizer.countTokens();
         if(nTokens > 0) command = tokenizer.nextToken();
         if(nTokens > 1) arg1 = tokenizer.nextToken();
         if(nTokens > 2) arg2 = tokenizer.nextToken();
         
         // process command
         if("help".equals(command))
         {
            System.out.println("help : Show a list of available commands.");
            System.out.println("status: Print status of repository (capacity, #datasets stored, etc).");
            System.out.println("list: List UIDs and dataset info for all datasets stored in repository.");
            System.out.println("get uid [N]: Retrieve the identified dataset from the repository. If second\n" +
            		"argument is an integer, then list dataset info followed by the first N values in the dataset's\n" +
            		"raw data array. Max allowed value of N is 200.");
            System.out.println("put N M: Auto-generates N datasets and store them in the repository. All datasets\n" +
            		"will be the same: a 1000xM multiset where X=[0,1,..,999] and each Y-vector contains random\n" +
            		"data. Allowed range of N is [1..200]; for M, [2..100].");
            System.out.println("rename uid name: Rename the identified dataset in the repository.");
            System.out.println("remove N: Remove N randomly chosen datasets from the repository. Max allowed value\n" +
            		"of N is 200. If N exceeds the number of datasets remaining, repository will be empty.");
            System.out.println("compact: Compact the repository file.");
            System.out.println("quit: Exit the program (the repository file is not removed).");
         }
         else if("status".equals(command))
         {
            System.out.println(dnf.getStatus());
         }
         else if("list".equals(command))
         {
            int[] uids = dnf.getUIDs();
            if(uids == null)
            {
               System.out.println("  !!! FAIL: " + dnf.getFailureReason());
               done = true;
            }
            else
            {
               System.out.println("  Found " + uids.length + " datasets:");
               for(int uid : uids) 
               {
                  DataSetInfo info = dnf.getDataSetInfo(uid);
                  if(info != null)
                     System.out.println("  " + uid + " : " + info.getShortDescription());
                  else if(!dnf.isUnusable())
                     System.out.println("  " + uid + " : " + " ??? dataset not found!");
                  else
                  {
                     System.out.println("  !!! FAIL: " + dnf.getFailureReason());
                     done = true;
                     break;
                  }
               }
               System.out.println();
            }
         }
         else if("get".equals(command))
         {
            int uid = -1;
            if(arg1 != null)
            {
               try { uid = Integer.parseInt(arg1); } catch(NumberFormatException ignored) {}
            }
            if(uid <= 0)
            {
               System.out.println("  Bad UID argument!");
               continue;
            }
            
            int n = 0;
            if(arg2 != null)
            {
               try { n = Integer.parseInt(arg2); } catch(NumberFormatException ignored) {}
               n = Utilities.rangeRestrict(0, 200, n);
            }
            
            DataSet ds = dnf.get(uid);
            if(ds != null)
            {
               if(n == 0) System.out.println("  OK.");
               else
               {
                  System.out.println("  " + ds.getInfo().getShortDescription());
                  Iterator<Float> iter = ds.getRawDataIterator();
                  int i = 0;
                  while(i < n && iter.hasNext())
                  {
                     if((i%10) == 0) System.out.println("  ");
                     float fValue = iter.next();
                     System.out.print(String.format("%8.3f", fValue) + " ");
                     ++i;
                  }
               }
            }
            else if(!dnf.isUnusable())
               System.out.println("  !!! No such dataset found in repository!");
            else
            {
               System.out.println("  !!! FAIL: " + dnf.getFailureReason());
               done = true;
            }
         }
         else if("put".equals(command))
         {
            int n = 0;
            if(arg1 != null)
            {
               try { n = Integer.parseInt(arg1); } catch(NumberFormatException ignored) {}
               n = (n < 1) ? 0 : (n>200 ? 0 : n);
            }
            if(n == 0)
            {
               System.out.println("  Bad arg1 -- must be an int in [1..200]. No datasets added.");
               continue;
            }
            
            int m = 0;
            if(arg2 != null)
            {
               try { m = Integer.parseInt(arg2); } catch(NumberFormatException ignored) {}
               m = (m < 2) ? 0 : (m>100 ? 0 : m);
            }
            if(m == 0)
            {
               System.out.println("  Bad arg2 -- must be an int in [2..100]. No datasets added.");
               continue;
            }
                        
            long tStart = System.currentTimeMillis();
            for(int i=0; i<n; i++)
            {
               // generate a UID
               int uid = -1;
               while(uid < 0)
               {
                  uid = (int) (Math.random() * Math.pow(2.0, 31.0));
                  if(dnf.contains(uid)) uid = -1;
               }
               
               // create the dataset and store it in the repository. Remember that DataSet encapsulates the raw data
               // array, so we must construct a separate array for each set!
               float[] raw = new float[1000*(m+1)];
               for(int j=0; j<1000; j++)
               {
                  raw[j*(m+1)] = j;
                  for(int k=1; k<=m; k++) raw[j*(m+1) + k] = (float) Math.random();
               }
               DataSet ds = DataSet.createDataSet("set" + uid, DataSet.Fmt.MSET, null, 1000, m+1, raw);
               if(ds == null)
               {
                  System.out.println("  !!! Failed to create fake dataset!");
                  done = true;
                  break;
               }
               
               if(!dnf.put(uid, ds))
               {
                  System.out.println("  !!! FAILED: " + dnf.getFailureReason());
                  done = true;
                  break;
               }
            }
            long tElapsed = System.currentTimeMillis() - tStart;
            if(!done) 
               System.out.println("  OK: " + n + " datasets added to repository in " + tElapsed + " milliseconds.");
         }
         else if("rename".equals(command))
         {
            int uid = -1;
            if(arg1 != null)
            {
               try { uid = Integer.parseInt(arg1); } catch(NumberFormatException ignored) {}
            }
            if(uid <= 0)
            {
               System.out.println("  Bad UID argument!");
               continue;
            }
            
            if(!DataSet.isValidIDString(arg2))
            {
               System.out.println("  Bad dataset name string!");
               continue;
            }
            
            if(dnf.changeDataSetIDString(uid, arg2))
               System.out.println("  OK.");
            else if(!dnf.isUnusable())
               System.out.println("  !!! No such dataset found in repository!");
            else
            {
               System.out.println("  !!! FAIL: " + dnf.getFailureReason());
               done = true;
            }
         }
         else if("remove".equals(command))
         {
            int n = 0;
            if(arg1 != null)
            {
               try { n = Integer.parseInt(arg1); } catch(NumberFormatException ignored) {}
               n = Utilities.rangeRestrict(0, 200, n);
            }
            if(n == 0)
            {
               System.out.println("  Bad argument. No datasets removed.");
               continue;
            }
            
            if(n >= dnf.getCount())
            {
               boolean ok = dnf.removeAll();
               if(ok)
                  System.out.println("  OK. All remaining datasets were removed.");
               else
               {
                  System.out.println("  !!! FAILED: " + dnf.getFailureReason());
                  done = true;
               }
            }
            else
            {
               List<Integer> uids = dnf.getUIDs(null);
               for(int i=0; i<n; i++)
               {
                  int idx = (int) (Math.random() * uids.size());
                  if(idx >= uids.size()) idx = uids.size() - 1;
                  
                  if(!dnf.remove(uids.get(idx)))
                  {
                     System.out.println("  !!! FAILED: " + dnf.getFailureReason());
                     done = true;
                     break;
                  }
                  else
                     uids.remove(idx);
               }
               if(!done)
                  System.out.println("  OK: " + n + " datasets removed; " + dnf.getCount() + " remaining.");
            }
         }
         else if("compact".equals(command))
         {
            System.out.println("  File size before compaction = " + f.length() + " bytes.");
            long tStart = System.currentTimeMillis();
            if(!dnf.compact())
            {
               System.out.println("  !!! FAILED: " + dnf.getFailureReason());
               done = true;
            }
            else
            {
               long tElapsed = System.currentTimeMillis() - tStart;
               System.out.println("  File compacted in " + tElapsed + " milliseconds.\n  File size after " +
               		"compaction = " + f.length() + " bytes.");
            }
         }
         else if("quit".equals(command))
            done = true;
         else
            System.out.println("   !!! Command not recognized: " + command);
      }
      
      System.out.println("Bye!");
      System.exit(0);
   }
   
   /**
    * Is the content of the specified file consistent with the expected format of a <i>DataNav</i> binary data source
    * repository file? The method does not parse the entire file, but merely validates the file header and the first 
    * entry of the block index for the first data section. The file must exist.
    * @param f The file to test.
    * @param isHubRepo True if file is a backing file for a <i>DataNav</i> portal hub data repository, false if it is a 
    * general-purpose <i>DataNav</i> dataset source. The file header tags are different for these two contexts. 
    * @return True if file content is consistent with the expected format. Returns false if argument is null, if file 
    * does not exist, or if the file header or the first block index entry for the first data section is invalid.
    */
   static boolean checkFile(File f, boolean isHubRepo)
   {
      if(f == null || !f.isFile()) return(false);
      
      ByteBuffer bb = ByteBuffer.allocate(INDEXENTRYSZ);
      ByteOrder byteOrder = ByteOrder.nativeOrder();
      bb.clear();
      bb.order(byteOrder);
      
      int expectedTagLE = isHubRepo ? TAG_DNX_LE : TAG_DNR_LE;
      boolean ok = false;
      try(RandomAccessFile raf = new RandomAccessFile(f, "r"))
      {
         FileChannel fc = raf.getChannel();

         // read in and check file header: tag and num sections in file. Fix endianness.
         bb.limit(TAGSZ);
         if(TAGSZ != fc.read(bb))
            throw new IOException("Bad file header");
         bb.position(0);
         int tag = bb.getInt();
         if(tag != expectedTagLE)
         {
            int tagBE = (expectedTagLE == TAG_DNX_LE) ? TAG_DNX_BE : TAG_DNR_BE;
            if(tag != tagBE) throw new IOException("Unrecognized file tag: " + tag);
            if(byteOrder == ByteOrder.LITTLE_ENDIAN) byteOrder = ByteOrder.BIG_ENDIAN;
            else byteOrder = ByteOrder.LITTLE_ENDIAN;
            bb.order(byteOrder);
         }
         int nSections = bb.getInt();
         if(nSections <= 0) throw new IOException("Invalid number of data sections: " + nSections);

         // read in and parse first entry in block index for first data section
         bb.clear();
         bb.limit(INDEXENTRYSZ);
         if(INDEXENTRYSZ != fc.read(bb)) throw new IOException("Bad initial entry in block index of data section 0");
         bb.position(0);
         IndexEntry entry = getIndexEntry(bb);
         if(entry == null) throw new IOException("Bad initial entry in block index of data section 0");

         ok = true;
      }
      catch(IOException ignored) {}
      
      return(ok);
   }

   /** 
    * Construct a repository file for <i>DataNav</i> datasets. Two versions are supported, one intended as the backing
    * file for a portal hub (tag code = "@DNX") and one intended as a general-purpose <i>DataNav</i>-compatible binary
    * dataset source (tag = "@DNR"). In the latter context, every dataset stored must have a unique name, but that 
    * requirement is NOT enforced by this class. Rather, this proxy is used as a delegate by a wrapper class that 
    * implements the <i>IDataSrc</i> interface.
    *
    * @param f The abstract pathname of the binary repository file. The file is not opened in this constructor. It 
    * may not yet exist, in which case it will "contain" zero datasets.
    * @param isHubRepo True if repository file is or will be the backing file for a <i>DataNav</i> portal hub data
    * repository, false if it is a general-purpose <i>DataNav</i> dataset source. The file header tags are different
    * for these two contexts.
    */
   public DataSetRepository(File f, boolean isHubRepo)
   {
      if(f == null) throw new IllegalArgumentException("File argument cannot be null");
      filePath = f;
      tagLE = isHubRepo ? TAG_DNX_LE : TAG_DNR_LE;
      numSections = 0;
      allocatedBlocks = null;
      uid2BlockMap = null;
      failureReason = null;
      datasetCache = new HashMap<>();
   }
   
   /**
    * Get abstract pathname of the repository file.
    * @return The repository file's full path.
    */
   public File getFilePath() { return(filePath); }
   
   /**
    * Has this repository file been rendered unusable by a previous catastrophic error?
    * @return True if repository file is unusable.
    */
   public boolean isUnusable() { return(failureReason != null); }
   
   /**
    * Get a description of the error that occurred that rendered this repository file unusable. Once an operation fails, 
    * all further operations are disabled in hopes of minimizing the work required to recover from the error.
    * @return Null if no error has occurred; else a brief description of the error.
    */
   public String getFailureReason() { return(failureReason); }

   /**
    * Get a status description for this dataset repository file. If file is unusable, the description includes the
    * reason. Otherwise, it indicates the current capacity (total number of file blocks), number of blocks allocated,
    * and the number of blocks occupied by a dataset.
    * @return File status string, as described.
    */
   public String getStatus()
   {
      preload();
      String status;
      if(failureReason != null)
         status = "Disabled: " + failureReason;
      else
      {
         status = "Capacity = " + (numSections*SECTIONSZ) + " dataset blocks.\n";
         status += "  " + allocatedBlocks.size() + " blocks allocated.\n";
         status += "  " + uid2BlockMap.size() + " datasets stored.\n";
      }
      return(status);
   }
   
   /**
    * Preload this dataset repository file. Usage is optional, since it will be invoked automatically the first time 
    * any operation is attempted.
    * 
    * <p>The method reads and parses the block index for each data section in the physical file, preparing the allocated
    * block list and the dataset UID-to-assigned block hashmap. In doing so, it validates the structure of the each
    * section index and will disable this file proxy if any anomalies are detected. If the physical file does not yet 
    * exist, this method will create it, writing the 8-byte file tag and an initial section index, indicating that no 
    * dataset blocks have been allocated yet.</p>
    * @return True if successful or if file has already been preloaded; false if an IO error occurs, if file content
    * is not consistent with the expected dataset repository file format, or if this file proxy was already rendered 
    * unusable by a previous catastrophic error.
    */
   public boolean preload()
   {
      if(failureReason != null) return(false);
      if(allocatedBlocks != null) return(true);
      
      boolean needInit = !filePath.isFile();
      try(RandomAccessFile raf = new RandomAccessFile(filePath, "rwd"))
      {
         FileChannel fc = raf.getChannel();
         fc.position(0);

         ByteBuffer bb = getByteBuffer();

         if(needInit)
         {
            // file just created. Write the file tag and a 500-entry section index with no dataset blocks allocated.
            IndexEntry unallocated = new IndexEntry(UNALLOCATED_ID, 0, 0, null);

            bb.limit(TAGSZ + SECTIONSZ * INDEXENTRYSZ);
            bb.putInt(tagLE);
            bb.putInt(1);
            for(int i = 0; i < SECTIONSZ; i++) putIndexEntry(bb, unallocated);
            bb.position(0);
            if(TAGSZ + SECTIONSZ * INDEXENTRYSZ != fc.write(bb))
               throw new IOException("Unexpected error while initializing empty repository file");

            numSections = 1;
            allocatedBlocks = new ArrayList<>();
            uid2BlockMap = new HashMap<>();
         } else
         {
            // read in and check file header: tag and num sections in file. Fix endianness.
            bb.limit(TAGSZ);
            if(TAGSZ != fc.read(bb))
               throw new IOException("Unexpected EOF while reading file header");
            bb.position(0);
            int tag = bb.getInt();
            if(tag != tagLE)
            {
               int tagBE = (tagLE == TAG_DNX_LE) ? TAG_DNX_BE : TAG_DNR_BE;
               if(tag != tagBE) throw new IOException("Unrecognized file tag: " + tag);
               if(byteOrder == ByteOrder.LITTLE_ENDIAN) byteOrder = ByteOrder.BIG_ENDIAN;
               else byteOrder = ByteOrder.LITTLE_ENDIAN;
               bb.order(byteOrder);
            }
            int nSections = bb.getInt();
            if(nSections <= 0) throw new IOException("Invalid number of data sections: " + nSections);

            // parse the block index of each existing data section and create list of all allocated blocks. Validate
            // block index structure: first block starts immediately after index. Offset of next allocated block =
            // offset of prev allocated block + size of prev allocated block. Once an unallocated block is encountered,
            // all remaining blocks in index must be unallocated. Also, if there is more than one data section, only the
            // last section can have any unallocated blocks, and the block index for section N begins immediately after
            // the last allocated block of section N-1.
            //
            List<IndexEntry> allocated = new ArrayList<>();
            HashMap<Integer, IndexEntry> blockMap = new HashMap<>();
            IndexEntry lastEntry = null;
            int i = 0;
            boolean gotUnallocBlock = false;
            while(i < nSections)
            {
               // move file pointer to the start of the block index for the next data section
               if(i > 0) fc.position(lastEntry.offset + lastEntry.size);

               // read in the block index for the current section
               bb.clear();
               bb.limit(SECTIONSZ * INDEXENTRYSZ);
               if(SECTIONSZ * INDEXENTRYSZ != fc.read(bb))
                  throw new IOException("Unexpected EOF while reading block index for data section " + i);
               bb.position(0);

               // consume and validate section's block index, appending allocated blocks and update uid-to-block hash
               for(int j = 0; j < SECTIONSZ; j++)
               {
                  IndexEntry entry = getIndexEntry(bb);
                  if(entry == null)
                     throw new IOException("Could not parse index entry at block " + j + " in data section " + i);

                  if(entry.uid < UNOCCUPIED_ID || gotUnallocBlock)
                  {
                     gotUnallocBlock = true;
                     if(entry.uid != UNALLOCATED_ID || entry.offset != 0 || entry.size != 0)
                        throw new IOException("Bad unallocated entry at pos=" + i + " in block index ");
                     if(i != nSections - 1)
                        throw new IOException(
                              "Found unallocated block outside terminal data section (section " + i + ", blk " + j + ")");
                  } else
                  {
                     long expected;
                     if(j != 0) expected = lastEntry.offset + lastEntry.size;
                     else if(i == 0) expected = TAGSZ + SECTIONSZ * INDEXENTRYSZ;
                     else expected = lastEntry.offset + lastEntry.size + SECTIONSZ * INDEXENTRYSZ;

                     if(entry.offset != expected)
                        throw new IOException("Bad file offset in index entry: section " + i + ", block " + j +
                              ";\n  expected offset = " + expected + ", observed = " + entry.offset);

                     allocated.add(entry);
                     if(entry.uid > UNOCCUPIED_ID) blockMap.put(entry.uid, entry);
                     lastEntry = entry;
                  }
               }

               ++i;
            }

            numSections = nSections;
            allocatedBlocks = allocated;
            uid2BlockMap = blockMap;
         }
      }
      catch(IOException ioe)
      {
         failureReason = ioe.getMessage() + "\n  File: " + filePath;
         clearCache();
      }
      
      return(failureReason == null);
   }
      
   /**
    * Does this dataset repository file contain a dataset assigned to the given identifier?
    * @param uid The dataset's unique integer identifier (UID).
    * @return True if dataset is stored in this repository file. If UID is not strictly positive or if this repository
    * file object is disabled, returns false.
    */
   public boolean contains(int uid)
   {
      if(uid <= UNOCCUPIED_ID) return(false);
      preload();
      if(isUnusable()) return(false);
      return(uid2BlockMap.containsKey(uid));
   }
   
   /**
    * Get the total number of datasets currently stored in this dataset repository file.
    * @return Number of datasets. If the repository has been rendered unusable by a previous operational failure, this
    * method returns -1.
    */
   public int getCount()
   {
      preload();
      return(isUnusable() ? -1 : uid2BlockMap.size());
   }
   
   /**
    * Get the unique integer identifiers (UIDs) for all datasets currently stored in this dataset repository file.
    * @return An array holding the dataset UIDs, in no particular order. If the repository has been rendered unusable by 
    * a previous operational failure, this method returns null.
    */
   public int[] getUIDs()
   {
      preload();
      if(isUnusable()) return(null);
      
      Set<Integer> keys = uid2BlockMap.keySet();
      int[] uids = new int[keys.size()];
      int j = 0; for(Integer key : keys) uids[j++] = key;
      
      return(uids);
   }
   
   /**
    * Get the unique integer identifiers (UIDs) for all datasets currently stored in this dataset repository file.
    * @param uids If non-null, all dataset UIDs are appended to this list, in no particular order.
    * @return If the argument is non-null, it is returned with the UIDs added. If the argument is null, a new list 
    * object is returned holding the UIDs. If the repository has been rendered unusable by a previous operational 
    * failure, this method returns null.
    */
   public List<Integer> getUIDs(List<Integer> uids)
   {
      preload();
      if(isUnusable()) return(null);
      
      List<Integer> uidList = (uids != null) ? uids : new ArrayList<>();
      uidList.addAll(uid2BlockMap.keySet());
      return(uidList);
   }
   
   /**
    * Store a dataset in this dataset repository file.
    * <p><i>The time it takes to perform the operation (unless there's a file IO problem) should be proportional to the
    * dataset size. NOTE that it may be necessary to grow the file to accommodate a new entry, but this should not 
    * significantly increase execution time.</i></p>
    *
    * @param uid The unique integer ID to which the dataset is mapped. Must be strictly positive.
    * @param ds The dataset itself. Must not be null.
    * @return True if successful. Returns false if repository file already contains a dataset assigned to the specified 
    * ID, if operation fails, or if this repository file object was rendered unusable by a prior operational failure.
    */
   public boolean put(int uid, DataSet ds)
   {
      if(uid <= 0 ||ds == null) throw new IllegalArgumentException();
      if(contains(uid)) return(false);

      // if file is currently full, we must grow it to accommodate the new entry
      if(numSections*SECTIONSZ == uid2BlockMap.size())
      {
         if(!grow()) return(false);
      }
      
      // compute #bytes needed to store dataset
      DataSetInfo info = ds.getInfo();
      int size = computeStorageSize(info);
      
      // find first unoccupied but allocated block that can accommodate dataset, if there is one.
      int block = -1;
      for(int i=0; i<allocatedBlocks.size(); i++)
      {
         IndexEntry entry = allocatedBlocks.get(i);
         if(entry.uid == UNOCCUPIED_ID && size <= entry.size)
         {
            block = i;
            break;
         }
      }
      
      // if no unoccupied blocks will work, use first unallocated block
      if(block < 0 && allocatedBlocks.size() < numSections*SECTIONSZ) block = allocatedBlocks.size();
      
      // if no unoccupied blocks will work AND all blocks are allocated, try to coalesce any adjacent unoccupied blocks.
      if(block < 0) block = coalesce(size);
      
      // if we still don't have an unoccupied block that's big enough, then we must grow the file.
      if(block < 0) 
      {
         if(!grow()) return(false);
         block = allocatedBlocks.size();
      }
      
      // prepare index entry holding dataset ID and block offset and size. If block is yet unallocated, its offset will
      // be at EOF and its size will match the dataset's required storage size.
      IndexEntry entryAdded;
      if(block == allocatedBlocks.size())
      {
         if(allocatedBlocks.isEmpty())
            entryAdded = new IndexEntry(uid, TAGSZ + SECTIONSZ*INDEXENTRYSZ, size, info);
         else
         {
            IndexEntry entry = allocatedBlocks.get(allocatedBlocks.size()-1);
            
            // if allocating first block of a new data section, we have to account for that section's block index!
            long offset = entry.offset + entry.size;
            if((block % SECTIONSZ) == 0) offset += SECTIONSZ*INDEXENTRYSZ;
            
            entryAdded = new IndexEntry(uid, offset, size, info);
         }
      }
      else
      {
         IndexEntry entry = allocatedBlocks.get(block);
         entryAdded = new IndexEntry(uid, entry.offset, entry.size, info);
      }
      
      // write dataset into physical file, also updating file index. This also will store dataset in cache.
      if(!writeDataset(block, entryAdded, ds)) return(false);
      
      // update cached index and uid-to-block map
      if(block == allocatedBlocks.size()) allocatedBlocks.add(entryAdded);
      else allocatedBlocks.set(block, entryAdded);
      uid2BlockMap.put(uid, entryAdded);
      return(true);
   }
   
   /**
    * Retrieve a dataset from this dataset repository file.
    * @param uid The unique integer ID to which the dataset is mapped. Must be strictly positive.
    * @return The requested dataset. Returns null if dataset was not found, if this file proxy was rendered unusable by
    * a prior operational failure, or if this retrieval operation fails.
    */
   public DataSet get(int uid)
   {
      if(uid <= 0) throw new IllegalArgumentException();
      preload();
      if(isUnusable()) return(null);

      IndexEntry entry = uid2BlockMap.get(uid);
      if(entry == null) return(null);
      
      return(readDataset(entry));
   }
   
   /**
    * Retrieve summary information on a dataset stored in this repository file. All dataset summaries are cached 
    * in-memory, so this method returns quickly (unless a file preload was necessary).
    * @param uid The unique integer ID to which a dataset is mapped. Must be strictly positive.
    * @return Summary information on the requested dataset. Returns null if dataset was not found, or if this file proxy
    * was rendered unusable by a prior operational failure.
    */
   public DataSetInfo getDataSetInfo(int uid)
   {
      if(uid <= 0) throw new IllegalArgumentException();
      preload();
      if(isUnusable()) return(null);

      IndexEntry entry = uid2BlockMap.get(uid);
      return((entry != null) ? entry.info : null);
   }
   
   /** 
    * Change the ID string of the identified dataset in this repository file. The corresponding file index entry is
    * updated accordingly, and the physical file size is unchanged by the operation.
    * @param uid The dataset UID. Must be strictly positive.
    * @param dsID The new ID string assigned to the dataset. Must be a valid dataset ID string.
    * @return True if successful; false if specified dataset not found, or if operation failed, or if this file proxy 
    * was already unusable due to a prior operational failure.
    */
   public boolean changeDataSetIDString(int uid, String dsID)
   {
      if(uid <= 0 || !DataSet.isValidIDString(dsID)) throw new IllegalArgumentException();
      preload();
      if(isUnusable()) return(false);
     
      IndexEntry entry = uid2BlockMap.get(uid);
      if(entry == null) return(false);
      else if(entry.info.getID().equals(dsID)) return(true);
      
      entry.info = DataSetInfo.changeID(entry.info, dsID);
      
      // locate relevant block index entry: section number, block number, and absolute file offset. If there's more 
      // than one data section, we have to take into account that the block index of each subsequent section begins 
      // immediately after the last allocated block of the preceding section
      int entryPos = -1;
      for(int i=0; i<allocatedBlocks.size(); i++) if(allocatedBlocks.get(i) == entry)
      {
         entryPos = i;
         break;
      }
      int section = entryPos / SECTIONSZ;
      int block = entryPos % SECTIONSZ;
      long offset = TAGSZ;
      if(section > 0)
      {
         IndexEntry last = allocatedBlocks.get(section*SECTIONSZ - 1);
         offset = last.offset + last.size;
      }
      offset += block*INDEXENTRYSZ;
      
      // open the file and overwrite the block index entry
      try(RandomAccessFile raf = new RandomAccessFile(filePath, "rwd"))
      {
         // open file and reposition to beginning of the block index entry to be updated
         FileChannel fc = raf.getChannel();
         fc.position(offset);

         ByteBuffer bb = getByteBuffer();
         bb.clear();
         bb.limit(INDEXENTRYSZ);
         if(!putIndexEntry(bb, entry))
            throw new IOException("Unexpected error while preparing index entry");
         bb.position(0);
         if(INDEXENTRYSZ != fc.write(bb))
            throw new IOException("Unexpected error while writing index entry");
      } catch(IOException ioe)
      {
         failureReason = "Failed to update block index entry (section " + section + ", block " + block + "):\n  " +
               ioe.getMessage();
         clearCache();
      }

      // remove the affected dataset from the dataset cache if it is there, replacing it with the renamed dataset
      if(failureReason == null)
      {
         DataSet ds = retrieveFromCache(uid);
         if(ds != null) storeInCache(uid, ds.changeID(dsID));
      }
      
      return(failureReason == null);
   }
   
   /** 
    * Remove the identified dataset from this dataset repository file. The file block allocated for the dataset is not 
    * removed, so this method is relatively fast, and the physical file size is unchanged by it.
    * @param uid The dataset UID.
    * @return True if successful; false if specified dataset not found, or if operation failed, or if this file proxy 
    * was already unusable due to a prior operational failure.
    */
   public boolean remove(int uid)
   {
      if(!contains(uid)) return(false);
      
      // remove dataset from cache if it is there
      removeFromCache(uid);

      // find allocation block in which dataset is stored
      int block = -1;
      for(int i=0; i<allocatedBlocks.size(); i++)
      {
         IndexEntry entry = allocatedBlocks.get(i);
         if(uid == entry.uid) { block = i; break; }
      }
      
      // update the physical file simply by setting the corresponding block index entry to unoccupied.
      if(!unoccupyAllocatedBlock(block)) return(false);
      
      // update internals: block used by removed dataset is marked as unoccupied
      IndexEntry unoccupied = allocatedBlocks.get(block);
      unoccupied.uid = UNOCCUPIED_ID;
      unoccupied.info = null;
      uid2BlockMap.remove(uid);
      return(true);
   }
   
   /** 
    * Remove all datasets from this dataset repository file. This merely deletes the old file and creates an empty one.
    * However, if there are already no datasets in the file, the method takes no action.
    * @return True if successful; false if operation failed, or if this file proxy was already unusable due to a prior 
    * operational failure.
    */
   public boolean removeAll()
   {
      preload();
      if(isUnusable()) return(false);
      if(uid2BlockMap.isEmpty()) return(true);
      
      // clear out everything!
      clearCache();
      allocatedBlocks.clear();
      uid2BlockMap.clear();
      numSections = 0;
      allocatedBlocks = null;
      uid2BlockMap = null;
      
      // delete the current file
      if(!filePath.delete())
      {
         failureReason = "Unable to delete old repository file at:\n  " + filePath;
         return(false);
      }
      
      return(preload());
   }
   
   /**
    * Compact this repository file.
    * <p>This method optimizes the use of the physical file. However, it requires a complete file rewrite and thus can 
    * take a noticeable amount of time to complete if the file is very large. Thus, it is recommended that this method
    * only be called on a background thread. It works by copying only the occupied blocks in the current file to a new
    * temporary repository file. The old file is then removed and replaced by the temporary file. This should minimize 
    * the chance of losing data during compaction.</p>
    * @return True if successful, false if operation failed or if this file proxy was already unusable due to a prior
    * operation failure. If all file blocks are allocated and occupied and the amount of wasted space across all blocks
    * is less than 10% of the total file size, the file is considered "compact enough" -- in which case the method takes 
    * no action and returns true.
    */
   public boolean compact()
   {
      preload();
      if(isUnusable()) return(false);
      
      // compute the amount of allocated but unoccupied space. This includes all unoccupied blocks plus any extra space
      // in occupied blocks
      long wasted = 0;
      for(IndexEntry e : allocatedBlocks)
      {
         if(e.uid == UNOCCUPIED_ID) wasted += e.size;
         else if(e.uid > UNOCCUPIED_ID) wasted += e.size - computeStorageSize(e.info);
      }
      
      // if amount of wasted space is less than 10% of the total, don't bother compacting
      long total = filePath.length();
      if(total == 0 || (0.1 > ((double)wasted) / ((double)total))) return(true);
      
      // prepare compacted index and corresponding UID-to-block hashmap. Recover unused space in each occupied block!
      long compactedOffset = TAGSZ;
      List<IndexEntry> compactedIndex = new ArrayList<>();
      HashMap<Integer, IndexEntry> compactedMap = new HashMap<>();
      for(IndexEntry entry : allocatedBlocks)
      {
         if(entry.uid > UNOCCUPIED_ID)
         {
            // if next block will start a new data section, we have to make room for that section's block index!
            int nBlks = compactedIndex.size();
            if((nBlks % SECTIONSZ) == 0) compactedOffset += SECTIONSZ * INDEXENTRYSZ;

            IndexEntry entry2 = new IndexEntry(entry.uid, compactedOffset, computeStorageSize(entry.info), entry.info);
            compactedOffset += entry2.size;
            compactedIndex.add(entry2);
            compactedMap.put(entry2.uid, entry2);
         }
      }
      int nSections = compactedIndex.size() / SECTIONSZ;
      ++nSections;
      
      // create temporary name for file that will be fully compacted. 
      int i = 0;
      String absPathStr = filePath.getAbsolutePath();
      File tmpFile = new File(absPathStr + "." + i);
      while(tmpFile.exists()) {++i; tmpFile = new File(absPathStr + "." + i); }
      
      RandomAccessFile srcRAF = null;
      RandomAccessFile dstRAF = null;
      boolean ok = false;
      try
      {
         srcRAF = new RandomAccessFile(filePath, "rw");
         FileChannel srcFC = srcRAF.getChannel();
         srcFC.position(0);
         
         dstRAF = new RandomAccessFile(tmpFile, "rw");
         FileChannel dstFC = dstRAF.getChannel();
         dstFC.position(0);
         
         // write file header
         ByteBuffer bb = getByteBuffer();
         bb.limit(TAGSZ);
         bb.putInt(tagLE);
         bb.putInt(nSections);
         bb.position(0);
         if(TAGSZ != dstFC.write(bb))
            throw new IOException("Unexpected error while writing file header for compacted file");
         
         // write each data section into the compacted file, transferring data from old file. Each section begins with
         // its block index.
         IndexEntry unallocated = new IndexEntry(UNALLOCATED_ID, 0, 0, null);
         for(i=0; i<nSections; i++)
         {
            // first write the block index for the data section
            bb.clear();
            bb.limit(SECTIONSZ*INDEXENTRYSZ);
            for(int j=0; j<SECTIONSZ; j++)
            {
               int idx = i*SECTIONSZ + j;
               IndexEntry entry = (idx < compactedIndex.size()) ? compactedIndex.get(idx) : unallocated;
               if(!putIndexEntry(bb, entry))
                  throw new IOException("Unexpected error while preparing block index for section " + i);
            }
            bb.position(0);
            if(SECTIONSZ*INDEXENTRYSZ != dstFC.write(bb))
               throw new IOException("Unexpected error while writing block index for section " + i);
            
            // now transfer the actual data from the old to the new file
            for(int j=0; j<SECTIONSZ; j++)
            {
               int idx = i*SECTIONSZ + j;
               if(idx >= compactedIndex.size()) break;
               IndexEntry entry = compactedIndex.get(idx);
               IndexEntry old = uid2BlockMap.get(entry.uid);
               
               long nBytes = srcFC.transferTo(old.offset, entry.size, dstFC);
               if(nBytes != ((long)entry.size))
                  throw new IOException("Unexpected error while copying data to compacted file");
            }
         }

         srcRAF.close();
         dstRAF.close();
         ok = true;
      }
      catch(IOException ioe)
      {
         failureReason = "Failed during compaction:\n   " + ioe.getMessage() + "\n  File: " + filePath;
         clearCache();
      }
      finally
      {
         try { if(srcRAF != null) srcRAF.close(); } catch(IOException ignored) {}
         try { if(dstRAF != null) dstRAF.close(); } catch(IOException ignored) {}
      }
      
      if(ok)
      {
         ok = filePath.delete();
         if(ok) ok = tmpFile.renameTo(filePath);
         if(!ok) 
         {
            failureReason = "File delete or rename operation failed after compaction.\n" + 
                  "Compacted dataset repository saved in: " + tmpFile;
            clearCache();
         }
         
         if(ok)
         {
            // SUCCESS. Replace current block index and map with new versions reflecting file's compacted state!
            numSections = nSections;
            allocatedBlocks.clear();
            allocatedBlocks = compactedIndex;
            uid2BlockMap.clear();
            uid2BlockMap = compactedMap;
         }
      }
      return(ok);
   }
   
   /**
    * Helper method that increases the capacity of the dataset repository file by appending the block index for a new
    * data section to the end of the file. None of the blocks in the new section are allocated. The file header is also
    * updated to reflect the increased capacity.
    * @return True if successful; false if an error occurs, rendering the file proxy unusable.
    */
   private boolean grow()
   {
      preload();
      if(isUnusable()) return(false);
      
      // bug catcher: this method should never be called if the last existing section has any unallocated blocks!
      if(allocatedBlocks.size() < numSections*SECTIONSZ)
         throw new IllegalStateException("Cannot increase capacity until all existing sections are fully allocated!");
      
      RandomAccessFile raf = null;
      boolean ok = false;
      try
      {
         raf = new RandomAccessFile(filePath, "rwd");
         FileChannel fc = raf.getChannel();
         
         ByteBuffer bb = getByteBuffer();

         // increment number of data sections in file
         fc.position(0);
         bb.limit(TAGSZ);
         bb.putInt(tagLE);
         bb.putInt(numSections + 1);
         bb.position(0);
         if(TAGSZ != fc.write(bb))
            throw new IOException("Unexpected error while updating file header");
         
         // move to the end of the file and write the block index for the new section. No blocks are allocated yet.
         fc.position(fc.size());
         IndexEntry unallocated = new IndexEntry(UNALLOCATED_ID, 0, 0, null);
         bb.clear();
         bb.limit(SECTIONSZ*INDEXENTRYSZ);
         for(int i=0; i<SECTIONSZ; i++) if(!putIndexEntry(bb, unallocated))
            throw new IOException("Unexpected error while preparing block index for new data section");
         bb.position(0);
         if(SECTIONSZ*INDEXENTRYSZ != fc.write(bb))
            throw new IOException("Unexpected error while appending block index for new data section");
         ok = true;
      }
      catch(IOException ioe)
      {
         failureReason = "Failed while growing file:\n   " + ioe.getMessage() + "\n  File: " + filePath;
         clearCache();
      }
      finally
      {
         try { if(raf != null) raf.close(); } catch(IOException ignored) {}
      }
      
      if(ok) ++numSections;
      
      return(ok);
   }

   /**
    * Helper method called when all dataset blocks are allocated in the dataset repository file, but none of the 
    * unoccupied blocks can accommodate a dataset of the specified size. This method rewrites the block index of the 
    * <i>last data section only</i>, coalescing each set of consecutive unoccupied blocks within that section into a 
    * single larger block, thereby reducing the total number of blocks currently allocated in that section. 
    * 
    * <p>The method cannot coalesce any data section except the last, because to do so would violate the expected
    * structure of the repository file: only the last data section may have some unallocated blocks. If there's a lot of
    * wasted space in the preceding data sections, then compaction is warranted -- but it's a much more time-consuming
    * operation because it requires a complete file rewrite. Coalescing is intended to be a fast way to free up a large
    * enough block. It only involves rewriting the block index of the last data section.</p>
    * 
    * <p><i>A special case.</i> If coalescing is not possible, but the last block in the last data section is 
    * unoccupied, then that block is marked as unallocated and the file is truncated accordingly.</p>
    * 
    * @param spaceNeeded Size of space needed, in #bytes.
    * @return Zero-based index of block, after coalescing, that will accommodate the needed space. This block may or may
    * not be allocated already. If coalescing is not possible, or if the file write operation failed, returns -1.
    */
   private int coalesce(int spaceNeeded)
   {
      // handle silly cases
      if(spaceNeeded <= 0) throw new IllegalArgumentException();
      if(isUnusable()) return(-1);
      if(allocatedBlocks.size() < numSections*SECTIONSZ) return(allocatedBlocks.size());
      
      // is coalescing possible? Must be at least one adjacent pair of unoccupied blocks in the final data section.
      boolean canCoalesce = false;
      boolean lastEntryUnoccupied = false;
      for(int i=(numSections-1)*SECTIONSZ; i<numSections*SECTIONSZ; i++)
      {
         boolean isUnoccupied = allocatedBlocks.get(i).uid == UNOCCUPIED_ID;
         if(lastEntryUnoccupied && isUnoccupied) {canCoalesce = true; break; }
         lastEntryUnoccupied = isUnoccupied;
      }
      if(!(canCoalesce || lastEntryUnoccupied)) return(-1);
      
      // handle special case: last block in last section is unoccupied, and coalescing is not possible.
      if(!canCoalesce)
      {
         try(RandomAccessFile raf = new RandomAccessFile(filePath, "rwd"))
         {
            FileChannel fc = raf.getChannel();

            // position file pointer to the start of the last entry of last data section
            long sectionOffset = TAGSZ;
            if(numSections > 1)
            {
               IndexEntry e = allocatedBlocks.get((numSections - 1) * SECTIONSZ - 1);
               sectionOffset = e.offset + e.size;
            }
            fc.position(sectionOffset + (SECTIONSZ - 1) * INDEXENTRYSZ);

            // mark that entry as unallocated
            ByteBuffer bb = getByteBuffer();
            bb.limit(INDEXENTRYSZ);
            if(!putIndexEntry(bb, new IndexEntry(UNALLOCATED_ID, 0, 0, null)))
               throw new IOException("Unexpected error while preparing block index");
            bb.position(0);
            if(INDEXENTRYSZ != fc.write(bb))
               throw new IOException("Unexpected error while writing file index chunk");

            // truncate file at the end of the next-to-last block in the last data section
            IndexEntry e = allocatedBlocks.get(allocatedBlocks.size() - 2);
            fc.truncate(e.offset + e.size);

            // the last allocated block is now unallocated!
            allocatedBlocks.remove(allocatedBlocks.size() - 1);
         }
         catch(IOException ioe)
         {
            failureReason = "Failed while coalescing:\n   " + ioe.getMessage() + "\n   File: " + filePath;
            clearCache();
         }
         
         return((failureReason == null) ? allocatedBlocks.size() : -1);
      }
      
      // coalesce the last section's block index. We'll have gained at least one unallocated block as a result.
      List<IndexEntry> updated = new ArrayList<>();
      IndexEntry unoccupied = null;
      int i = (numSections-1)*SECTIONSZ;
      while(i < numSections*SECTIONSZ)
      {
         IndexEntry entry = allocatedBlocks.get(i);
         if(entry.uid == UNOCCUPIED_ID)
         {
            if(unoccupied != null)
            {
               long chkSz = ((long)unoccupied.size) + ((long)entry.size);
               if(chkSz > ((long)Integer.MAX_VALUE))
               {
                  updated.add(unoccupied);
                  unoccupied = entry;
               }
               else unoccupied.size += entry.size;
            }
            else unoccupied = entry;
         }
         else 
         {
            if(unoccupied != null) updated.add(unoccupied);
            unoccupied = null;
            updated.add(entry);
         }
         ++i;
      }
      if(unoccupied != null) updated.add(unoccupied);
      
      // write the coalesced block index to the physical file
      try(RandomAccessFile raf = new RandomAccessFile(filePath, "rwd"))
      {
         FileChannel fc = raf.getChannel();

         // position file pointer to the start of the block index of the last data section
         long sectionOffset = TAGSZ;
         if(numSections > 1)
         {
            IndexEntry e = allocatedBlocks.get((numSections - 1) * SECTIONSZ - 1);
            sectionOffset = e.offset + e.size;
         }
         fc.position(sectionOffset);

         ByteBuffer bb = getByteBuffer();
         bb.limit(SECTIONSZ * INDEXENTRYSZ);

         IndexEntry unallocated = new IndexEntry(UNALLOCATED_ID, 0, 0, null);
         for(i = 0; i < SECTIONSZ; i++)
         {
            IndexEntry entry = (i < updated.size()) ? updated.get(i) : unallocated;
            if(!putIndexEntry(bb, entry))
               throw new IOException("Unexpected error while preparing block index");
         }
         bb.position(0);
         if(SECTIONSZ * INDEXENTRYSZ != fc.write(bb))
            throw new IOException("Unexpected error while writing file index chunk");
      }
      catch(IOException ioe)
      {
         failureReason = "Failed while coalescing:\n   " + ioe.getMessage() + "\n   File: " + filePath;
         clearCache();
      }
      
      // return immediately if file update failed
      if(failureReason != null) return(-1);
      
      // success. Update the allocated block list and find first unoccupied block that's big enough to accommodate
      // the needed space. If there's still no such block, we're at least guaranteed there's at least one unallocated
      // block available. NOTE that the uid-to-entry hashmap is unaffected, because we did not modify any entries 
      // corresponding to stored datasets!!
      i = (numSections-1)*SECTIONSZ; 
      while(i < allocatedBlocks.size()) allocatedBlocks.remove(i);
      allocatedBlocks.addAll(updated);

      int block = -1;
      for(i=(numSections-1)*SECTIONSZ; i<allocatedBlocks.size(); i++)
      {
         IndexEntry entry = allocatedBlocks.get(i);
         if(entry.uid == UNOCCUPIED_ID && spaceNeeded <= entry.size)
         {
            block = i;
            break;
         }
      }
      return(block < 0 ? allocatedBlocks.size() : block);
   }
   
   /**
    * Helper method stores the specified dataset in the physical repository file, updating the specified block index
    * entry accordingly. If the specified block is not allocated, the file's size will increase to accommodate the 
    * dataset content. The dataset is also stored in the in-memory dataset cache.
    * @param idx Index of dataset block. If block is not yet allocated, this must equal the #blocks already allocated.
    * @param entry Index entry to be written into the file index (UID, file offset, block size, dataset info).
    * @param ds The dataset itself.
    * @return True if successful, false if any file-write operation fails.
    */
   private boolean writeDataset(int idx, IndexEntry entry, DataSet ds)
   {
      if(idx < 0 || idx >= numSections*SECTIONSZ || idx > allocatedBlocks.size() || entry == null || ds == null) 
         throw new IllegalArgumentException();

      // store dataset in cache now
      storeInCache(entry.uid, ds);

      try(RandomAccessFile raf = new RandomAccessFile(filePath, "rwd"))
      {
         // open file and reposition to beginning of target block
         FileChannel fc = raf.getChannel();
         fc.position(entry.offset);

         // write the dataset UID
         ByteBuffer bb = getByteBuffer();
         bb.limit(4);
         bb.putInt(entry.uid);
         bb.position(0);
         if(4 != fc.write(bb)) throw new IOException("Unexpected error while writing UID to allocated block");
         bb.clear();

         // write the raw data array in CHUNKSZ chunks
         int nBytes = computeStorageSize(entry.info) - 4;
         int nWrt = 0;
         while(nWrt < nBytes)
         {
            int nChunk = Math.min(CHUNKSZ, nBytes - nWrt);
            bb.limit(nChunk);
            FloatBuffer fbuf = bb.asFloatBuffer();
            ds.copyRawData(nWrt / 4, nChunk / 4, fbuf);
            bb.position(0);
            if(nChunk != fc.write(bb)) throw new IOException("Unexpected error while writing dataset raw data");
            bb.clear();
            nWrt += nChunk;
         }

         // update entry in block index. If entry is not in the first data section, we have to examine the last block
         // in the preceding section to find the offset to the start of the section containing the entry to be updated.
         int iSect = idx / SECTIONSZ;
         long offset = TAGSZ;
         if(iSect > 0)
         {
            IndexEntry e = allocatedBlocks.get(iSect * SECTIONSZ - 1);
            offset = e.offset + e.size;
         }
         offset += (idx % SECTIONSZ) * INDEXENTRYSZ;

         fc.position(offset);
         bb.clear();
         bb.limit(INDEXENTRYSZ);
         if(!putIndexEntry(bb, entry))
            throw new IOException("Unexpected error while preparing index entry");
         bb.position(0);
         if(INDEXENTRYSZ != fc.write(bb))
            throw new IOException("Unexpected error while writing index entry");
      }
      catch(IOException ioe)
      {
         failureReason = "Failed to write dataset in block " + idx + ":\n  " + ioe.getMessage();
         clearCache();
      }
      
      return(failureReason == null);
   }
   
   /**
    * Helper method reads the dataset at the specified block in the physical repository file. The method will first 
    * check the in-memory dataset cache. If it's available there, the file IO operation will be avoided.
    * @param entry Index entry for file block where dataset is stored (UID, file offset, block size, dataset info).
    * @return The dataset read from the file. Null if any file-read operation fails, which renders this repository file 
    * object unusable.
    */
   private DataSet readDataset(IndexEntry entry)
   {
      if(entry == null) throw new IllegalArgumentException();

      // try in-memory cache first!
      DataSet ds = this.retrieveFromCache(entry.uid);
      if(ds != null) return(ds);

      try(RandomAccessFile raf = new RandomAccessFile(filePath, "r"))
      {
         // open file and reposition to beginning of target block
         FileChannel fc = raf.getChannel();
         fc.position(entry.offset);

         // read the dataset UID and verify
         ByteBuffer bb = getByteBuffer();
         bb.limit(4);
         if(4 != fc.read(bb)) throw new IOException("Unexpected error while dataset UID from data block");
         bb.position(0);
         int uid = bb.getInt();
         if(uid != entry.uid) throw new IOException("Retrieved dataset UID does not match index entry!");
         bb.clear();

         // read the raw data array in CHUNKSZ chunks
         float[] fData = new float[entry.info.getDataArraySize()];
         int nBytes = fData.length * 4;
         int nWrt = 0;
         while(nWrt < nBytes)
         {
            int nChunk = Math.min(CHUNKSZ, nBytes - nWrt);
            bb.limit(nChunk);
            if(nChunk != fc.read(bb)) throw new IOException("Unexpected error while reading dataset raw data");
            bb.position(0);
            FloatBuffer fbuf = bb.asFloatBuffer();
            fbuf.get(fData, nWrt / 4, nChunk / 4);
            nWrt += nChunk;
            bb.clear();
         }

         // create the dataset
         ds = DataSet.createDataSet(entry.info, fData);
         if(ds == null)
            throw new IOException("Retrieved raw data array does not match dataset info in cached index entry!");
      }
      catch(IOException ioe)
      {
         failureReason = "Failed to read dataset (UID=" + entry.uid + ") at offset " +
               entry.offset + ":\n  " + ioe.getMessage();
         clearCache();
      }
      
      return(ds);
   }
   
   /**
    * Helper method updates the physical repository file, marking the allocated block as unoccupied. Only affects the
    * corresponding entry in the block index of the relevant data section. The index entry's file offset and size are 
    * left unchanged, since the corresponding file block still exists. The file size is unchanged after this operation.
    * @param idx Zero-based position in the file index. Must be in [0..N-1], where N is the total number of allocated
    * blocks in the file.
    * @return True if successful, false if a catastrophic IO error occurred, rendering this repository file unusable.
    */
   private boolean unoccupyAllocatedBlock(int idx)
   {
      if(idx < 0 || idx >= allocatedBlocks.size()) throw new IllegalArgumentException();
      
      int iSect = idx / SECTIONSZ;
      int iBlk = idx % SECTIONSZ;
      try(RandomAccessFile raf = new RandomAccessFile(filePath, "rwd"))
      {
         FileChannel fc = raf.getChannel();

         // find offset to the relevant block index entry. If it is not in the first data section, we must examine last
         // block in preceding section to find the offset to start of the section containing the entry to be updated.
         long offset = TAGSZ;
         if(iSect > 0)
         {
            IndexEntry e = allocatedBlocks.get(iSect * SECTIONSZ - 1);
            offset = e.offset + e.size;
         }
         offset += iBlk * INDEXENTRYSZ;

         fc.position(offset);

         ByteBuffer bb = getByteBuffer();
         bb.limit(4);
         bb.putInt(UNOCCUPIED_ID);
         bb.position(0);
         if(4 != fc.write(bb)) throw new IOException("Unexpected error while writing file");
      }
      catch(IOException ioe)
      {
         failureReason = "Failed to unoccupy allocated block (section " + iSect + ", block " + iBlk + "):\n  " +
               ioe.getMessage();
         clearCache();
      }
      
      return(failureReason == null);
   }
   
   /** 
    * Get the byte buffer allocated for use and cached by this <code>DNRepositoryFile</code>.
    * <p>Cacheing the byte buffer avoids having to reallocate it frequently if this <code>DNRepositoryFile</code> is 
    * accessed often. Only a soft reference to the buffer is maintained, so that the garbage collector can reclaim it 
    * when memory resources are low. This method handles reallocating the buffer when this happens.</p>
    * @return Hard reference to the byte buffer. The buffer will be initially cleared, with a limit and capacity of 
    * <code>CHUNKSZ</code> bytes. If the source file's byte order is known, the buffer will be set accordingly; else 
    * its byte order will be set to the platform's native byte order.
    */
   private ByteBuffer getByteBuffer()
   {
      ByteBuffer bb = (softBB == null) ? null : softBB.get();
      if(bb == null)
      {
         bb = ByteBuffer.allocate(CHUNKSZ);
         softBB = new SoftReference<>(bb);
      }
      bb.clear();
      if(byteOrder == null) byteOrder = ByteOrder.nativeOrder();
      bb.order(byteOrder);
      return(bb);
   }
   
   /**
    * Store a dataset in the repository's in-memory dataset cache. If the cache is at capacity, at least one entry is 
    * removed to make room.
    * @param uid Unique identifier of the dataset to be stored.
    * @param ds The dataset to be stored in cache.
    */
   private void storeInCache(int uid, DataSet ds)
   {
      // if it was already there, remove it and replace it
      SoftReference<DataSet> ref = datasetCache.remove(uid);
      if(ref != null)
      {
         ref.clear();
         datasetCache.put(uid, new SoftReference<>(ds));
         return;
      }
      
      // otherwise, if cache is at capacity, remove all entries that have been garbage-collected. If it is still full
      // after doing so, then remove 20% of the entries. NOTE use of iterator.remove(). Cant use HashMap.remove() 
      // while iterating over its keys!!
      if(datasetCache.size() == MAXCACHESZ)
      {
         Iterator<Integer> iter = datasetCache.keySet().iterator();
         while(iter.hasNext())
         {
            ref = datasetCache.get(iter.next());
            if(ref.get() == null) iter.remove();
         }
      }
      if(datasetCache.size() == MAXCACHESZ)
      {
         int n = MAXCACHESZ / 5;
         Iterator<Integer> iter = datasetCache.keySet().iterator();
         while(iter.hasNext())
         {
            ref = datasetCache.get(iter.next());
            if(ref != null) ref.clear();
            iter.remove();
            --n;
            if(n == 0) break;
         }
      }
      
      // store the dataset in the cache
      datasetCache.put(uid, new SoftReference<>(ds));
   }
   
   /**
    * Retreive a dataset from the repository's in-memory dataset cache, if it's available there.
    * @param uid Unique identifier of dataset to retrieve.
    * @return The dataset requested, or null if it was not found in cache.
    */
   private DataSet retrieveFromCache(int uid)
   {
      DataSet ds = null;
      SoftReference<DataSet> ref = datasetCache.get(uid);
      if(ref != null)
      {
         ds = ref.get();
         if(ds == null) datasetCache.remove(uid);
      }
      return(ds);
   }
   
   /**
    * Remove a dataset from the repository's in-memory dataset cache, if it's there.
    * @param uid Unique identifier of dataset to be removed from cache.
    */
   private void removeFromCache(int uid)
   {
      SoftReference<DataSet> ref = datasetCache.remove(uid);
      if(ref != null) ref.clear();
   }
   
   /** Clear the repository's in-memory dataset cache. */
   private void clearCache()
   {
      for(SoftReference<DataSet> ref : datasetCache.values())
         ref.clear();
      datasetCache.clear();
   }
   
   /** Absolute pathname of the repository file. */
   private final File filePath;
   
   /** Little-endian file tag code for this repository file. It has one of two possible values. Set at construction. */
   private final int tagLE;
   
   /** A soft reference to the byte buffer allocated and used to read/write repository file. */
   private SoftReference<ByteBuffer> softBB = null;
   
   /** Repository file's endianness. Will be <code>null</code> if undetermined. */
   private ByteOrder byteOrder = null;
   
   /** Total number of data sections in the repository file. */
   private int numSections;
   
   /** 
    * List of all <b>allocated</b> blocks in dataset repository file, in order of appearance. The number of allocated
    * blocks always lies in <i>[(N-1)*S .. N*S]</i>, where <i>N</i> is the number of data sections in the file and 
    * <i>S</i> is the number of allocated blocks in a fully allocated section. Only the last section in the file can
    * be partially allocated! Any allocated block with a zero-valued UID is allocated but unoccupied.
    */
   private List<IndexEntry> allocatedBlocks;
   
   /** Maps dataset UID to the index entry which defines the file block in which the dataset is stored. */
   private HashMap<Integer, IndexEntry> uid2BlockMap;
   
   /** Cache of recent datasets. Maintained as soft references so they can be garbage-collected when necessary. */
   private final HashMap<Integer, SoftReference<DataSet>> datasetCache;
   
   /** If non-null, this describes why last attempted operation failed. Once set, all further activity is disabled. */
   private String failureReason;
   
   
   /** Length of file tag in bytes. */
   private final static int TAGSZ = 8;
   /** Little-endian tag code for repository file that does NOT require each dataset to have a unique ID string. */
   private final static int TAG_DNX_LE = 0x584E4440; 
   /** Big-endian tag code for repository file that does NOT require each dataset to have a unique ID string. */
   private final static int TAG_DNX_BE = 0x40444E58;
   /** Little-endian tag code for repository file that DOES require each dataset to have a unique ID string. */
   private final static int TAG_DNR_LE = 0x524E4440; 
   /** Big-endian tag code for repository file that DOES require each dataset to have a unique ID string. */
   private final static int TAG_DNR_BE = 0x40444E52;
   
   /** Length of a single dataset block index entry, in bytes. */
   private final static int INDEXENTRYSZ = 84;
   /** Portion of dataset index entry occupied by the summary information on a dataset, in bytes. */
   private final static int DATASETINFOSZ = 68;
   /** Number of floating point parameters stored in the dataset summary info in index entry. */
   private final static int NDSIPARAMS = 4;
   /** Number of entries in the allocation block index for each file data section. */
   private final static int SECTIONSZ = 500;
   /** Value of UID field in index entry when corresponding allocation block is not yet allocated (= -1). */
   private final static int UNALLOCATED_ID = -1;
   /** 
    * Value of UID field in index entry when corresponding allocation block is allocated but unoccupied (= 0). Any UID 
    * greater than this value is a valid UID and indicates the corresponding block is occupied.
    */
   private final static int UNOCCUPIED_ID = 0;
   
   /** 
    * Size of byte buffer used to read/write the repository file. It's large enough to read in the entire block index
    * for a data section in one go.
    */
   private final static int CHUNKSZ = TAGSZ + INDEXENTRYSZ*SECTIONSZ + 16;
   
   /** 
    * Maximum number of entries in the dataset cache. When capacity is reached, one entry is removed when a new one
    * is added.
    */
   private final static int MAXCACHESZ = 50;
   
   /** 
    * Compute #bytes required to store a dataset having the specified summary information.
    * @param info Summary info on dataset, including dataset length and breadth and format type.
    * @return Total #bytes required to store dataset in the repository file. See file header for details.
    */
   private static int computeStorageSize(DataSetInfo info)
   {
      int datasize = 0;
      if(info != null)
      {
         datasize = info.getDataArraySize();
         datasize *= 4;
         datasize += 4; // the raw data array is preceded by the dataset's 32-bit UID
      }
      return(datasize);
   }
   
   /**
    * Helper method parses the next <code>INDEXENTRYSZ</code> bytes in the specified buffer as an entry in the block
    * index of a data section in the dataset repository. See class header for a description of the index layout.
    * @param bb The source buffer. Method fails if less than <code>INDEXENTRYSZ</code> bytes remain.
    * @return If successful, a <code>IndexEntry</code> instance encapsulating the dataset index entry that was parsed 
    * from the byte buffer; <code>null</code> otherwise.
    */
   private static IndexEntry getIndexEntry(ByteBuffer bb)
   {
      if(bb.remaining() < INDEXENTRYSZ) return(null);
      
      int uid = bb.getInt();
      long offset = bb.getLong();
      int size = bb.getInt();
      
      // if corresponding block is unused, dataset info is meaninglesss, but we do need to get past it in the buffer!
      if(uid <= UNOCCUPIED_ID)
      {
         bb.position(bb.position() + DATASETINFOSZ);
         return(new IndexEntry(uid, offset, size, null));
      }
      
      String id;
      byte[] idBytes = new byte[DataSet.MAXIDLEN+1];
      idBytes[DataSet.MAXIDLEN] = (byte) '\0';
      bb.get(idBytes, 0, DataSet.MAXIDLEN);
      id = new String(idBytes, StandardCharsets.US_ASCII);
      id = id.trim();
      if(!DataSet.isValidIDString(id)) return(null);

      DataSet.Fmt fmt = DataSet.Fmt.getFormatByIntCode(bb.getInt());
      if(fmt == null) return(null);
      
      int nrows = bb.getInt();
      int ncols = bb.getInt();
      if(nrows < 0 || ncols < 0) return(null);
      
      float[] params = new float[NDSIPARAMS];
      for(int i=0; i<NDSIPARAMS; i++) params[i] = bb.getFloat();
      DataSetInfo info = DataSetInfo.createDataSetInfo(id, fmt, nrows, ncols, params);
      
      return((info==null) ? null : new IndexEntry(uid, offset, size, info));
   }

   /**
    * Helper method puts the contents of the specified dataset index entry into the next <code>INDEXENTRYSZ</code> 
    * bytes available in the provided buffer, in the format expected in the block index of a data section within the
    * dataset repository. See class header for a description of the index layout.
    * @param bb The write buffer. Method fails if less than <code>INDEXENTRYSZ</code> bytes remain.
    * @param entry The dataset index entry to be written.
    * @return <code>True</code> if successful; <code>false</code> otherwise.
    */
   private static boolean putIndexEntry(ByteBuffer bb, IndexEntry entry)
   {
      if(bb.remaining() < INDEXENTRYSZ) return(false);
      
      bb.putInt(entry.uid);
      bb.putLong(entry.offset);
      bb.putInt(entry.size);
      
      if(entry.uid <= UNOCCUPIED_ID)
      {
         for(int i=0; i<DATASETINFOSZ; i++) bb.put((byte)0);
      }
      else
      {
         byte[] id = entry.info.getID().getBytes(StandardCharsets.US_ASCII);
         bb.put(id);
         for(int i=id.length; i<DataSet.MAXIDLEN; i++) bb.put((byte)0);

         bb.putInt(entry.info.getFormat().getIntCode());
         bb.putInt(entry.info.getDataLength());
         bb.putInt(entry.info.getDataBreadth());
         for(int i=0; i<NDSIPARAMS; i++) bb.putFloat(entry.info.getParam(i));
      }
      
      return(true);
   }

   
   /**
    * A single entry in the block index for a data section within the repository file.
    * @author sruffner
    */
   private static class IndexEntry
   {
      /** UID of dataset stored in the file block defined by this index. -1 == unallocated, 0 = allocated but unused. */
      int uid;
      /** Offset in bytes from start of file to first byte of file block. 0 if block not allocated. */
      final long offset;
      /** File block size in bytes. 0 if block not allocated. */
      int size;
      /** Summary info on dataset stored in the file block defined by this index. Ignore if block is unoccupied. */
      DataSetInfo info;
      
      IndexEntry(int uid, long offset, int size, DataSetInfo info)
      {
         this.uid = uid;
         this.offset = offset;
         this.size = size;
         this.info = info;
      }
   }
}
