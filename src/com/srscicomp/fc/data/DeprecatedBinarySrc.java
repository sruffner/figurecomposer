package com.srscicomp.fc.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * <b>DeprecatedBinarySrc</b> reads and writes files containing <i>FigureComposer</i>-compatible datasets stored in a 
 * custom binary format. As of Jan 2010, it has been replaced by a new version that optimizes addition, retrieval and
 * removal performance. The class is maintained to support reading in existing files in the deprecated format.
 * 
 * <p><b>NOTE: The 3D data set format {@link DataSet.Fmt#XYZSET} and the 4D data set format {@link DataSet.Fmt#XYZWSET} 
 * were introduced long after this file format was deprecated; however, it is possible to store and retrieve these 
 * data sets from a source file in this format.</b></p>
 * 
 * <h2>Format description</h2>
 * <p>The file begins with a <i>header</i> that includes a 16-byte tag followed by <i>N</i> "table of contents" 
 * (TOC) entries, where <i>N</i> is the total number of distinct data sets stored in the file. The header is followed 
 * immediately by <i>N</i> "data sections".</p>
 * 
 * <p>The initial 16-byte tag serves to identify the file as an FC binary data set source file, and it includes the 
 * number of data sets <i>N</i> stored in the file:</p>
 * <ul>
 *    <li>Bytes 3-0: The file tag code 0x004E4440, which includes the file format version (=0) in the most significant 
 *    byte (note that 0x4E4440 translates to "@DN" is ASCII). This tag not only serves to identify the file as a 
 *    <i>FC</i>-compatible binary dataset source file, but it also determines the file's endianness. If the tag 
 *    reads as 0x40444E00 when little-endian byte order is assumed, then the file must have been saved in big-endian 
 *    order.</li>
 *    <li>Bytes 7-4: An integer holding the value <i>N</i>.</li>
 *    <li>Bytes 15-8: All zeros (unused).</li>
 * </ul>
 * 
 * <p>Each TOC entry in the header is exactly 80 bytes long and contains key information about the dataset it 
 * represents. In what follows, PTSET, MSET, SERIES, MSERIES, RASTER1D, XYZIMG, XYZSET, XYZWSET represent the different 
 * data set formats supported in <i>FigureComposer</i> and enumerated in {@link DataSet.Fmt}.</p>
 * <ul>
 *    <li>Bytes 39-0: ID string, null-terminated and padded if the ID is less than 40 characters long. Single-byte 
 *    ASCII characters. Must satisfy the constraints defined by {@link DataSet#isValidIDString(String)}.</li>
 *    <li>Bytes 43-40: (int) Data format code. See {@link DataSet.Fmt} for the set of recognized values.</li>
 *    <li>Bytes 47-44: (int) Number of rows in data matrix. See {@link DataSet#getDataLength()}.</li>
 *    <li>Bytes 51-48: (int) Number of columns in data matrix. See {@link DataSet#getDataBreadth()}.</li>
 *    <li>Bytes 71-52: (float[5]) Five single-precision floating-point parameters. For the SERIES and MSERIES formats, 
 *    the first two parameters are <i>dx</i> and <i>x0</i>; the remaining ones are unused. For XYZIMG, the first four 
 *    elements specify the x- and y-coordinate ranges spanned by the image data Z(x,y): <i>[x0 x1 y0 y1]</i>. For the
 *    other formats, these parameters are unused.</li>
 *    <li>Bytes 79-72: (long) This 8-byte integer is the file offset from the beginning of the file to the first byte 
 *    of the data section for the data set represented by this TOC entry.</li>
 * </ul>
 * 
 * <p>Each data section begins with a 4-byte integer equal to the ordinal position of the data set in the TOC; this 
 * provides one more check that the file is correctly formatted. The "raw" data array immediately follows this tag. For 
 * all data formats except RASTER1D, it will be a float array of length <i>N*M</i>, where <i>N</i> is the number of rows
 * and <i>M</i> is the number of columns (as specified in the corresponding TOC entry). The total number of bytes in the
 * data section here is <i>4*N*M + 4</i>, including the 4-byte tag. For the four 2D formats, XYZSET, and XYZWSET, the 
 * N M-tuples are stored sequentially in this array; for XYZIMG, the intensity image data are stored row-wise in the 
 * array. The RASTER1D data array is different. The first <i>M</i> entries are the lengths of the individual rasters in 
 * the collection, and remaining N elements are raster samples: <i>[n1 n2 .. nM x1(1..n1) x2(1..n2) .. xM(1..nM)]</i>. 
 * Note that <i>N = n1 + n2 + .. + nM</i>. In this case, the total length of the data section is <i>4 + 4*(N+M)</i> 
 * bytes.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
class DeprecatedBinarySrc implements IDataSrc
{
   /**
    * Is the content of the specified file consistent with the expected format of a <i>DataNav</i> binary dataset source 
    * file? The method checks the 16-byte header and the first "table of contents" entry. It also verifies that the file 
    * is large enough to hold the dataset identified in that first entry.
    * @param f The file to test.
    * @return True if file content is consistent with the expected format. Returns false if argument is null, if file 
    * does not exist, or if it does not pass the compatibility checks.
    */
   static boolean checkFile(File f)
   {
      if(f == null || !f.isFile()) return(false);

      try(FileInputStream fis = new FileInputStream(f))
      {
         FileChannel in = fis.getChannel();

         ByteBuffer bb = ByteBuffer.allocate(TOCENTRYSZ);
         bb.order(ByteOrder.LITTLE_ENDIAN);

         // read in and check 16-byte header, adjust for endianness, get number of datasets stored in file
         bb.limit(HEADERSZ);
         if(HEADERSZ != in.read(bb)) return (false);
         if(bb.getInt(0) != HEADERTAG_LE)
         {
            if(bb.getInt(0) != HEADERTAG_BE) return (false);
            bb.order(ByteOrder.BIG_ENDIAN);
         }
         int n = bb.getInt(4);
         if(n < 0) return (false);
         if(bb.getLong(8) != 0) return (false);

         // if there are no datasets in file, there's nothing more to check!
         if(n == 0) return (true);

         // read in and check first TOC entry
         bb.clear();
         if(TOCENTRYSZ != in.read(bb)) return (false);
         bb.position(0);
         TOCEntry entry = getTOCEntry(bb);
         if(entry == null) return (false);

         long minFileSz = entry.fileOffset + entry.getDataSizeInBytes() + 4;  // 4 for the data section index tag!
         return (in.size() >= minFileSz);
      }
      catch(IOException ioe)
      {
         return (false);
      }
   }
   
   /** 
    * Construct a binary dataset source file that reads/writes <i>DataNav</i> data sets from/to the specified file.
    * @param f The abstract pathname of the binary data source file. The file is not opened in this constructor. It 
    * may not exist, in which case only the write operations will be available initially.
    */
   DeprecatedBinarySrc(File f) { srcPath = f; }
   
   public File getSourceFile() { return(srcPath); }
   public String getLastError() { return(lastErrorMsg); }
   public boolean isUnusable() { return(false); }
   public boolean isReadOnly() { return(false); }

   public DataSetInfo[] getSummaryInfo()
   {
      lastErrorMsg = "";
      
      // if there's no TOC in source file, then something is wrong!
      updateTOCCacheIfNecessary();
      if(tableOfContents == null) return(null);
      
      DataSetInfo[] info = new DataSetInfo[tableOfContents.length];
      for(int i=0; i<info.length; i++) info[i] = tableOfContents[i].info;
      return(info);
   }

   public DataSet getDataByID(String id)
   {
      lastErrorMsg = "";
      
      // if there's no TOC in source file, then something is wrong!
      updateTOCCacheIfNecessary();
      if(tableOfContents == null) return(null);
      
      // find TOC entry corresponding to specified ID
      int tocIndex = -1;
      for(int i=0; i<tableOfContents.length; i++) if(tableOfContents[i].info.getID().equals(id))
      {
         tocIndex = i;
         break;
      }
      if(tocIndex < 0)
      {
         lastErrorMsg = "Dataset ID not found!";
         return(null);
      }
      
      ByteBuffer bb = getByteBuffer();
      FileInputStream fis = null;
      DataSet ds = null;
      try
      {
         fis = new FileInputStream(srcPath);
         FileChannel in = fis.getChannel();
         float[] fData = getDataArray(in, tocIndex, tableOfContents[tocIndex], bb);
         ds = DataSet.createDataSet(tableOfContents[tocIndex].info, fData);
         if(ds == null) throw new IOException("Data section not consistent with table-of-contents entry");
      }
      catch(IOException ioe) { lastErrorMsg = ioe.getMessage(); }
      finally
      {
         try{ if(fis != null) fis.close(); } catch(IOException ignored) {}
      }
      return(ds);
   }

   public boolean writeData(DataSet set, boolean replace)
   {
      lastErrorMsg = "";
      
      // if no set is provided, return success
      if(set == null) return(true); 
      
      // if file exists, retrieve current table of contents and see if file contains a dataset with the same ID
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
            if(tableOfContents[i].info.getID().equals(set.getID()))
            {
               iMatch = i;
               break;
            }
         }
      }

      // if replace flag not set and there already exists a dataset with the same ID, fail.
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
      
      // construct new TOC. Whether we replace an existing dataset or simply append, new dataset will be located at the 
      // end of the file and the TOC. If appending, note that file offsets are adjusted to account for an additional 
      // entry in TOC! 
      TOCEntry[] dstTOC;
      if(tableOfContents == null)
      {
         dstTOC = new TOCEntry[1];
         dstTOC[0] = new TOCEntry(set.getInfo(), HEADERSZ+TOCENTRYSZ);
      }
      else
      {
         int n = (replace && iMatch >= 0) ? tableOfContents.length : tableOfContents.length + 1;
         dstTOC = new TOCEntry[n];
         
         long offset = HEADERSZ + (long) n *TOCENTRYSZ;
         int j = 0;
         for(int i=0; i<tableOfContents.length; i++)
         {
            if(replace && i==iMatch) continue;
            dstTOC[j++] = new TOCEntry(tableOfContents[i].info, offset);
            offset += tableOfContents[i].getDataSizeInBytes() + 4;
         }
         dstTOC[n-1] = new TOCEntry(set.getInfo(), offset);
      }    
      
      ByteBuffer bb = getByteBuffer();
      
      FileInputStream fis = null;
      FileOutputStream fos = null;
      boolean ok = true;
      try
      {
         FileChannel in = null;
         if(srcPath.isFile())
         {
            fis = new FileInputStream(srcPath);
            in = fis.getChannel();
         }
         
         fos = new FileOutputStream(dst);
         FileChannel out = fos.getChannel();
         
         // write the new file header and TOC
         bb.limit(HEADERSZ);
         bb.putInt(HEADERTAG_LE);
         bb.putInt(dstTOC.length);
         bb.putLong(0);
         bb.position(0);
         if(HEADERSZ != out.write(bb)) throw new IOException("Unexpected error while writing header");
         bb.clear();
         
         int nEntries = dstTOC.length;
         int nWrt = 0;
         while(nWrt < nEntries)
         {
            int nBytes = Math.min(bb.capacity(), TOCENTRYSZ*(nEntries-nWrt));
            int nDelta = nBytes/TOCENTRYSZ;
            nBytes = nDelta * TOCENTRYSZ;
            bb.limit(nBytes);
            for(int i=0; i<nDelta; i++) 
            {
               if(!putTOCEntry(bb, dstTOC[nWrt+i]))
                  throw new IOException("Unexpected error while writing TOC entry " + i);
            }
            bb.position(0);
            if(nBytes != out.write(bb)) throw new IOException("Unexpected error while writing TOC");
            bb.clear();
            nWrt += nDelta;
         }
         
         // copy all existing data sections directly. If we're replacing an existing dataset, we have to omit that --
         // which complicates things...
         if(in != null)
         {
            if(tableOfContents != null && !(replace && iMatch >= 0))
            {
               long start = HEADERSZ + (long) TOCENTRYSZ *tableOfContents.length;
               long count = in.size() - start;
               in.transferTo(start, count, out);
            }
            else if(tableOfContents != null)
            {
               // copy all data sections up to the one being removed
               long start = HEADERSZ + (long) TOCENTRYSZ *tableOfContents.length;
               long count = tableOfContents[iMatch].fileOffset;
               count -= start;
               in.transferTo(start, count, out);

               // copy all data sections after the one being removed (if any). For these, we must fix the 4-byte integer
               // at the beginning of the section that reflects the datasets ordinal position in the TOC!
               if(iMatch < tableOfContents.length-1)
               {
                  start = tableOfContents[iMatch].fileOffset + tableOfContents[iMatch].getDataSizeInBytes() + 4;
                  count = in.size() - start;
                  in.transferTo(start, count, out);

                  for(int i=iMatch; i<dstTOC.length-1; i++)
                  {
                     out.position(dstTOC[i].fileOffset);

                     bb.limit(4);
                     bb.putInt(i);
                     bb.position(0);
                     if(4 != out.write(bb)) throw new IOException("Unexpected error while writing data section");
                     bb.clear();
                  }
               }
            }

         }

         // now write the data section for the added data set
         out.position(dstTOC[dstTOC.length-1].fileOffset);
         
         bb.limit(4);
         bb.putInt(dstTOC.length-1);
         bb.position(0);
         if(4 != out.write(bb)) throw new IOException("Unexpected error while writing data section");
         bb.clear();
         
         int nBytes = dstTOC[dstTOC.length-1].getDataSizeInBytes();
         nWrt = 0;
         while(nWrt < nBytes)
         {
            int nChunk = Math.min(CHUNKSZ, nBytes-nWrt);
            bb.limit(nChunk);
            FloatBuffer fbuf = bb.asFloatBuffer();
            set.copyRawData(nWrt/4, nChunk/4, fbuf);
            bb.position(0);
            if(nChunk != out.write(bb)) throw new IOException("Unexpected error while writing data section");
            bb.clear();
            nWrt += nChunk;
         }
      }
      catch(IOException ioe)
      {
         lastErrorMsg = ioe.getMessage();
         ok = false;
      }
      finally
      {
         try { if(fis != null) fis.close(); } catch(IOException ignored) {}
         try { if(fos != null) fos.close(); } catch(IOException ignored) {}
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
         byteOrder = bb.order();
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
         if(tocIndex < 0 && tableOfContents[i].info.getID().equals(id)) tocIndex = i;
         if(dupIndex < 0 && tableOfContents[i].info.getID().equals(idNew)) dupIndex = i;

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
         if(dupIndex != tocIndex) lastErrorMsg = "Candidate ID is duplicates that of an existing dataset!";
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
      
      // construct new TOC, replacing the relevant entry. This is the only thing changed in the file!
      DataSetInfo changedInfo = DataSetInfo.changeID(tableOfContents[tocIndex].info, idNew);
      TOCEntry[] dstTOC = new TOCEntry[tableOfContents.length];
      for(int i=0; i<dstTOC.length; i++) 
      {
         if(i == tocIndex) dstTOC[i] = new TOCEntry(changedInfo, tableOfContents[i].fileOffset);
         else dstTOC[i] = tableOfContents[i];
      }

      
      ByteBuffer bb = getByteBuffer();
      
      FileInputStream fis = null;
      FileOutputStream fos = null;
      boolean ok = true;
      try
      {
         fos = new FileOutputStream(dst);
         FileChannel out = fos.getChannel();
         fis = new FileInputStream(srcPath);
         FileChannel in = fis.getChannel();
         
         // write the new file header and TOC
         bb.limit(HEADERSZ);
         bb.putInt(HEADERTAG_LE);
         bb.putInt(dstTOC.length);
         bb.putLong(0);
         bb.position(0);
         if(HEADERSZ != out.write(bb)) throw new IOException("Unexpected error while writing header");
         bb.clear();
         
         int nEntries = dstTOC.length;
         int nWrt = 0;
         while(nWrt < nEntries)
         {
            int nBytes = Math.min(bb.capacity(), TOCENTRYSZ*(nEntries-nWrt));
            int nDelta = nBytes/TOCENTRYSZ;
            nBytes = nDelta * TOCENTRYSZ;
            bb.limit(nBytes);
            for(int i=0; i<nDelta; i++) 
            {
               if(!putTOCEntry(bb, dstTOC[nWrt+i]))
                  throw new IOException("Unexpected error while writing TOC entry " + i);
            }
            bb.position(0);
            if(nBytes != out.write(bb)) throw new IOException("Unexpected error while writing TOC");
            bb.clear();
            nWrt += nDelta;
         }
         
         // copy all existing data sections directly
         long start = HEADERSZ + (long) TOCENTRYSZ *tableOfContents.length;
         long count = in.size() - start;
         in.transferTo(start, count, out);
      }
      catch(IOException ioe)
      {
         lastErrorMsg = ioe.getMessage();
         ok = false;
      }
      finally
      {
         try { if(fis != null) fis.close(); } catch(IOException ignored) {}
         try { if(fos != null) fos.close(); } catch(IOException ignored) {}
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
         byteOrder = bb.order();
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
      for(int i=0; i<tableOfContents.length; i++) if(tableOfContents[i].info.getID().equals(id))
      {
         iRemove = i;
         break;
      }
      if(iRemove < 0) return(true);
      
      // special case: removing the last dataset in source file!
      if(tableOfContents.length == 1) return(removeAll());
      
      // we always write to a temporary location. Original source file MUST exist.
      File dst = DataSrcFactory.getTempFilePath(srcPath);
      if(dst == null)
      {
         lastErrorMsg = "Unable to generate a temp file name (bad source file path?). Write failed!";
         return(false);
      }
      
      
      // construct new TOC that omits the dataset to be removed. We have to be careful to adjust all file offsets
      // correctly!
      int removedSize = tableOfContents[iRemove].getDataSizeInBytes() + 4;
      TOCEntry[] dstTOC = new TOCEntry[tableOfContents.length - 1];
      int j = 0;
      for(int i=0; i<tableOfContents.length; i++) if(i != iRemove)
      {
         dstTOC[j] = new TOCEntry(tableOfContents[i].info, tableOfContents[i].fileOffset);
         
         // adjust all file offsets to account for the removed TOC entry. Further adjust file offsets of those 
         // datasets located after the one removed by the size of the removed set.
         dstTOC[j].fileOffset -= TOCENTRYSZ;
         if(i > iRemove) dstTOC[j].fileOffset -= removedSize;
         
         j++;
      }
      
      ByteBuffer bb = getByteBuffer();
      
      FileInputStream fis = null;
      FileOutputStream fos = null;
      boolean ok = true;
      try
      {
         fos = new FileOutputStream(dst);
         FileChannel out = fos.getChannel();
         fis = new FileInputStream(srcPath);
         FileChannel in = fis.getChannel();
         
         // write the new file header and TOC
         bb.limit(HEADERSZ);
         bb.putInt(HEADERTAG_LE);
         bb.putInt(dstTOC.length);
         bb.putLong(0);
         bb.position(0);
         if(HEADERSZ != out.write(bb)) throw new IOException("Unexpected error while writing header");
         bb.clear();
         
         int nEntries = dstTOC.length;
         int nWrt = 0;
         while(nWrt < nEntries)
         {
            int nBytes = Math.min(bb.capacity(), TOCENTRYSZ*(nEntries-nWrt));
            int nDelta = nBytes/TOCENTRYSZ;
            nBytes = nDelta * TOCENTRYSZ;
            bb.limit(nBytes);
            for(int i=0; i<nDelta; i++) 
            {
               if(!putTOCEntry(bb, dstTOC[nWrt+i]))
                  throw new IOException("Unexpected error while writing TOC entry " + i);
            }
            bb.position(0);
            if(nBytes != out.write(bb)) throw new IOException("Unexpected error while writing TOC");
            bb.clear();
            nWrt += nDelta;
         }
         
         // copy all data sections up to the one being removed
         long start;
         long count;
         if(iRemove > 0)
         {
            start = HEADERSZ + (long) TOCENTRYSZ *tableOfContents.length;
            count = (iRemove==tableOfContents.length-1) ? in.size() : tableOfContents[iRemove].fileOffset;
            count -= start;
            in.transferTo(start, count, out);
         }
         
         // copy all data sections after the one being removed (if any). For these, we must fix the 4-byte integer at
         // the beginning of the section that reflects the datasets ordinal position in the TOC!
         if(iRemove < tableOfContents.length-1)
         {
            start = tableOfContents[iRemove].fileOffset + tableOfContents[iRemove].getDataSizeInBytes() + 4;
            count = in.size() - start;
            in.transferTo(start, count, out);
            
            for(int i=iRemove; i<dstTOC.length; i++)
            {
               out.position(dstTOC[i].fileOffset);
               
               bb.limit(4);
               bb.putInt(i);
               bb.position(0);
               if(4 != out.write(bb)) throw new IOException("Unexpected error while writing data section");
               bb.clear();
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
         try { if(fis != null) fis.close(); } catch(IOException ignored) {}
         try { if(fos != null) fos.close(); } catch(IOException ignored) {}
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
         byteOrder = bb.order();
         tableOfContents = dstTOC;
      }
      
      return(ok);
   }
   
   public boolean removeAll()
   {
      lastErrorMsg = "";
      
      // we always write to a temporary location, UNLESS original source file does not yet exist.
      File dst = DataSrcFactory.getTempFilePath(srcPath);
      if(dst == null)
      {
         lastErrorMsg = "Unable to generate a temp file name (bad source file path?). Write failed!";
         return(false);
      }
      
      // write an empty TOC to the destination file.
      TOCEntry[] dstTOC = new TOCEntry[0];
      ByteBuffer bb = getByteBuffer();
      
      FileOutputStream fos = null;
      boolean ok = true;
      try
      {
         fos = new FileOutputStream(dst);
         FileChannel out = fos.getChannel();
         
         // write the new file header indicating zero-length TOC
         bb.limit(HEADERSZ);
         bb.putInt(HEADERTAG_LE);
         bb.putInt(dstTOC.length);
         bb.putLong(0);
         bb.position(0);
         if(HEADERSZ != out.write(bb)) throw new IOException("Unexpected error while writing header");
         bb.clear();
      }
      catch(IOException ioe)
      {
         lastErrorMsg = ioe.getMessage();
         ok = false;
      }
      finally
      {
         try { if(fos != null) fos.close(); } catch(IOException ignored) {}
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
         byteOrder = bb.order();
         tableOfContents = dstTOC;
      }
      
      return(ok);
   }
   
   
   private final static int HEADERSZ = 16;
   private final static int TOCENTRYSZ = 80;
   private final static int HEADERTAG_LE = 0x004E4440; // header tag in LE order. MSByte is current version
   private final static int HEADERTAG_BE = 0x40444E00; // header tag in BE order.
   private final static int IDLEN = DataSet.MAXIDLEN;
   private final static int NPARAMS = 5;
   private final static int CHUNKSZ = 1024 * 16;
   
   /** The abstract pathname for the data source file. */
   private final File srcPath;
   
   /** A soft reference to the byte buffer allocated and used for file IO operations. */
   private SoftReference<ByteBuffer> softBB = null;

   /** Source file's endianness. Will be null if undetermined. */
   private ByteOrder byteOrder = null;
   
   /** Description of error that occurred during last operation, or empty string if operation was successful. */
   private String lastErrorMsg = "";
   
   /** Source file's modification time the last time we cached TOC information extracted from it. */
   private long srcLastModified = -1;
   
   /** Cache of source file's table of contents section. Will be null if it has not been cached. */
   private TOCEntry[] tableOfContents = null;
   
   /** 
    * Get the byte buffer allocated for use and cached by this binary dataset source proxy.
    * 
    * <p>Cacheing the byte buffer avoids having to reallocate it frequently if this proxy is accessed often. Only a soft
    * reference to the buffer is maintained, so that the garbage collector can reclaim it when memory resources are low.
    * This method handles reallocating the buffer when this happens.</p>
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
      bb.order((byteOrder != null) ? byteOrder : ByteOrder.nativeOrder());
      return(bb);
   }
   
   /**
    * Helper method checks if source file has changed since the last time we cached table-of-contents information 
    * extracted from it. If so, the cache is discarded and the table of contents is read in again. If unable to get 
    * the table of contents, <code>getLastError()</code> will return the reason for the failure.
    */
   private void updateTOCCacheIfNecessary()
   {
      if(srcLastModified > 0 && srcLastModified != srcPath.lastModified())
      {
         srcLastModified = -1;
         byteOrder = null;
         tableOfContents = null;
      }
      if(tableOfContents != null) return;
      
      // fail immediately if path is not specified or does not exist
      if(srcPath == null || !srcPath.isFile())
      {
         lastErrorMsg = (srcPath==null) ? "File unspecified" : "File not found";
         return;
      }

      long modT = srcPath.lastModified();
      ByteBuffer bb = getByteBuffer();

      FileInputStream fis = null;
      TOCEntry [] toc = null;
      boolean ok = false;
      try
      {
         fis = new FileInputStream(srcPath);
         FileChannel in = fis.getChannel();
         long filesize = in.size();
         
         // read in and check 16-byte header, adjust for endianness, get number of datasets stored in file
         bb.limit(HEADERSZ);
         if(HEADERSZ != in.read(bb)) throw new IOException("Unexpected EOR while reading 16-byte header");
         int tag = bb.getInt(0);
         if(tag != HEADERTAG_LE)
         {
            if(tag != HEADERTAG_BE) throw new IOException("Invalid header. Not a DataNav binary source");
            bb.order(ByteOrder.BIG_ENDIAN);
         }
         int n = bb.getInt(4);
         if(n < 0 || bb.getLong(8) != 0) throw new IOException("Invalid header. Not a DataNav binary source");
         
         // read in and parse TOC entries
         toc = new TOCEntry[n];
         int i = 0;
         while(i < n)
         {
            bb.clear();
            int nEntries = Math.min(n-i, CHUNKSZ/TOCENTRYSZ);
            bb.limit(nEntries*TOCENTRYSZ);
            if(nEntries*TOCENTRYSZ != in.read(bb)) throw new IOException("Unexpected EOF while reading TOC");
            bb.position(0);
            
            for(int j=0; j<nEntries; j++)
            {
               TOCEntry entry = getTOCEntry(bb);
               if(entry == null) throw new IOException("TOC entry " + i + " is invalid!");
               if(filesize < (entry.fileOffset + entry.getDataSizeInBytes() + 4))
                  throw new IOException("File is not large enough to hold dataset defined in TOC entry " + i);
               toc[i++] = entry;
            }
         }

         ok = true;
      }
      catch(IOException ioe) { lastErrorMsg = ioe.getMessage(); }
      finally
      {
         try { if(fis != null) fis.close(); } catch(IOException ignored) {}
      }
      
      if(ok)
      {
         srcLastModified = modT;
         byteOrder = bb.order();
         tableOfContents = toc;
         ensureUniqueIdentifiers(tableOfContents);
      }
   }
   

   
   
   /**
    * Helper method parses the next <code>TOCENTRYSZ</code> bytes in the specified buffer as an entry in the "table of 
    * contents" section of a <code>DeprecatedBinarySrc</code>. See class header for a description of the TOC layout.
    * @param bb The source buffer. Method fails if less than <code>TOCENTRYSZ</code> bytes remain.
    * @return If successful, a <code>TOCEntry</code> instance encapsulating the table-of-contents entry that was parsed 
    * from the byte buffer; <code>null</code> otherwise.
    */
   private static TOCEntry getTOCEntry(ByteBuffer bb)
   {
      if(bb.remaining() < TOCENTRYSZ) return(null);
      byte[] idBytes = new byte[IDLEN+1];
      idBytes[IDLEN] = (byte) '\0';
      bb.get(idBytes, 0, IDLEN);
      String id = new String(idBytes, StandardCharsets.US_ASCII);
      id = id.trim();
      if(!DataSet.isValidIDString(id)) return(null);

      DataSet.Fmt fmt = DataSet.Fmt.getFormatByIntCode(bb.getInt());
      if(fmt == null) return(null);
      
      int nrows = bb.getInt();
      int ncols = bb.getInt();
      if(nrows < 0 || ncols < 0) return(null);
      
      float[] params = new float[NPARAMS];
      for(int i=0; i<NPARAMS; i++) params[i] = bb.getFloat();
      DataSetInfo info = DataSetInfo.createDataSetInfo(id, fmt, nrows, ncols, params);
      if(info == null) return(null);
      
      long fileOffset = bb.getLong();
      if(fileOffset < 0) return(null);
      
      return(new TOCEntry(info, fileOffset));
   }

   /**
    * Helper method put the contents of the specified table of contents entry into the next <code>TOCENTRYSZ</code> 
    * bytes available in the provided buffer, in the format expected for the "table of contents" section of a 
    * <code>DeprecatedBinarySrc</code>. See class header for a description of the TOC layout.
    * @param bb The write buffer. Method fails if less than <code>TOCENTRYSZ</code> bytes remain.
    * @param entry The TOC entry to be written.
    * @return <code>True</code> if successful; <code>false</code> otherwise.
    */
   private static boolean putTOCEntry(ByteBuffer bb, TOCEntry entry)
   {
      if(bb.remaining() < TOCENTRYSZ) return(false);
      
      byte[] idBytes = new byte[IDLEN];

      DataSetInfo info = entry.info;
      byte[] id = info.getID().getBytes(StandardCharsets.US_ASCII);
      for(int i=0; i<id.length && i<IDLEN; i++) idBytes[i] = id[i];

      bb.put(idBytes);
      bb.putInt(info.getFormat().getIntCode());
      bb.putInt(info.getDataLength());
      bb.putInt(info.getDataBreadth());
      for(int i=0; i<NPARAMS; i++) bb.putFloat(info.getParam(i));
      
      bb.putLong(entry.fileOffset);
      
      return(true);
   }

   /**
    * Helper method ensures that every entry in the specified "table of contents" list has a unique dataset ID. If a 
    * duplicate is found, that ID is modified by appending an integer N such that the modified ID is unique.
    * 
    * @param toc The table of contents list to be checked.
    * @see DataSet#ensureUniqueIdentifiers(DataSetInfo[])
    */
   private static void ensureUniqueIdentifiers(TOCEntry[] toc)
   {
      if(toc == null || toc.length < 2) return;
      
      DataSetInfo[] dsInfo = new DataSetInfo[toc.length];
      for(int i=0; i<dsInfo.length; i++) dsInfo[i] = toc[i].info;
      
      if(!DataSet.ensureUniqueIdentifiers(dsInfo))
         for(int i=0; i<dsInfo.length; i++)
            toc[i].info = dsInfo[i];
   }
   
   /**
    * Helper method that extracts the data array for a specified table-of-contents entry from the data section of a 
    * source file conforming to the layout enforced by <code>DeprecatedBinarySrc</code>.
    * @param in An open <code>FileChannel</code> providing access to the source file.
    * @param which Zero-based index position of the data in the file. In the source file, the data section begins with 
    * a 4-byte integer that MUST match this index.
    * @param entry The corresponding table of contents entry. Includes file offset to the start of the data section as 
    * well as the size of the data array in bytes.
    * @param bb The byte buffer to be used to read file contents. Assumed to be set to the correct byte order.
    * @return The single-precision floating-pt data array extracted from the source file.
    * @throws IOException if an IO error occurs or a file format problem is detected.
    */
   private static float[] getDataArray(FileChannel in, int which, TOCEntry entry, ByteBuffer bb) throws IOException
   {
      in.position(entry.fileOffset);
      bb.clear();
      bb.limit(4);
      if(4 != in.read(bb)) throw new IOException("Unexpected EOF while reading in data");
      int pos = bb.getInt(0);
      if(which != pos) throw new IOException("Invalid TOC index; file corrupted? (expected=" + which +"; actual=" + pos);

      int nBytes = entry.getDataSizeInBytes();
      float[] fData = new float[nBytes/4];
      
      int nBytesRead = 0;
      while(nBytesRead < nBytes)
      {
         int nRead = Math.min(nBytes-nBytesRead, bb.capacity());
         bb.clear();
         bb.limit(nRead);
         if(nRead != in.read(bb)) throw new IOException("Unexpected EOF while reading in data");
         bb.position(0);
         FloatBuffer fbuf = bb.asFloatBuffer();
         fbuf.get(fData, nBytesRead/4, nRead/4);
         nBytesRead += nRead;
      }
      return(fData);
   }
   
   /**
    * The content of a single entry in the "table-of-contents" section of a <code>DeprecatedBinarySrc</code>.
    * @author sruffner
    *
    */
   private static class TOCEntry
   {
      DataSetInfo info;
      long fileOffset;
      
      TOCEntry(DataSetInfo info, long fileOffset)
      {
         this.info = info;
         this.fileOffset = fileOffset;
      }
      
      int getDataSizeInBytes()
      {
         int datasize = 0;
         if(info != null)
         {
            datasize = info.getDataLength();
            if(info.getFormat() == DataSet.Fmt.RASTER1D) datasize += info.getDataBreadth();
            else datasize *= info.getDataBreadth();
            datasize *= 4;
         }
         return(datasize);
      }
   }
}
