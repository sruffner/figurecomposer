package com.srscicomp.fc.data;

import java.io.File;
import java.util.HashMap;

/**
 * A {@link IDataSrc} implementation that reads and writes a file containing <i>FigureComposer</i>-compatible data sets
 * stored in a custom binary format supporting random access to any data set therein. It was introduced as a general-
 * purpose data set source file format in Jan 2010, replacing an earlier version that was more fail safe at the expense 
 * of a severe degradation in performance as data sets were added to the file.
 * 
 * <h2>Format description</h2>
 * <p>This class implements the {@link IDataSrc} interface but delegates all file I/O to an instance of {@link 
 * DataSetRepository}. This repository file proxy, which was once used to store all the data sets in a <i>DataNav</i>
 * portal hub, indexes individual data sets by a unique positive integer ID, not by the data set string ID. Therefore,
 * <b>BinarySrc</b> itself enforces the interface requirement that all data sets have unique ID strings.</p>
 * 
 * <p>The <i>DataNav</i> project was ultimately abandoned circa 2017, but <b>BinarySrc</b> still can be used as a
 * repository for data sets that are used in <i>FigureComposer</i> files.</p>
 * 
 * <p>See {@link DataSetRepository} for a full explanation of the binary file format, which optimizes retrieval, 
 * addition, and removal of individual data sets and can grow to accommodate a large number of sets. The tradeoffs:
 * <ul>
 *    <li>Since the file is modified in place, a catastrophic failure during file I/O can leave the file in a corrupted 
 *    state. If such a situation arises, this data source proxy is rendered unusable.</li>
 *    <li>After many additions and removals, the file may end up containing lots of wasted space. This can be addressed
 *    by compacting the file. But compacting is a relatively time-consuming process and is currently not exposed in the
 *    {@link IDataSrc} interface.</li>
 * </ul>
 * </p>
 * @author sruffner
 */
class BinarySrc implements IDataSrc
{
   /**
    * Is the content of the specified file consistent with the expected format of a <i>DataNav</i> binary data source 
    * file? The method only checks the file header and the first entry in the block index for the first data section
    * -- in the first 100 bytes of the file -- so this is fast.
    * @param f The file to test.
    * @return True if file content is consistent with the expected format. Returns false if argument is null, if file 
    * does not exist, or if it does not pass the consistency check.
    */
   static boolean checkFile(File f) { return(DataSetRepository.checkFile(f, false)); }
   
   /** 
    * Construct a binary data source proxy that reads/writes <i>DataNav</i> data sets from/to the specified file.
    * @param f The abstract pathname of the binary data source file. The file is not opened in this constructor. It 
    * may not exist, in which case only the write operations will be available initially.
    */
   BinarySrc(File f) { repository = new DataSetRepository(f, false); }
   
   public File getSourceFile() { return(repository.getFilePath()); }
   public String getLastError() { return(failureReason != null ? failureReason : lastError); }
   public boolean isUnusable() { return(failureReason != null);}
   public boolean isReadOnly() { return(false); }

   public DataSetInfo[] getSummaryInfo()
   {
      lastError = "";
      if(!preload(true)) return(null);
      
      DataSetInfo[] info = new DataSetInfo[dsid2uidMap.size()];
      int i=0;
      for(String key : dsid2uidMap.keySet())
         info[i++] = repository.getDataSetInfo(dsid2uidMap.get(key));
      return(info);
   }

   public DataSet getDataByID(String id)
   {
      lastError = "";
      if(!preload(true)) return(null);
            
      Integer uid = dsid2uidMap.get(id);
      if(uid == null)
      {
         lastError = "No such dataset exists";
         return(null);
      }
      
      DataSet ds = repository.get(uid);
      if(ds == null && repository.isUnusable())
         failureReason = repository.getFailureReason();
      return(ds);
   }

   public boolean writeData(DataSet set, boolean replace)
   {
      lastError = "";
      
      // if no set is provided, return success
      if(set == null) return(true); 
      
      if(!preload(false)) return(false);
      
      // if there's already a dataset with the same ID, fail unless replace flag set, in which case remove it first.
      Integer existingUID = dsid2uidMap.get(set.getID());
      if(existingUID != null)
      {
         if(!replace)
         {
            lastError = "Source already contains a dataset with ID=" + set.getID() + ". Replace?";
            return(false);
         }
         repository.remove(existingUID);
         if(repository.isUnusable())
         {
            failureReason = repository.getFailureReason();
            return(false);
         }
         dsid2uidMap.remove(set.getID());
      }
      
      // generate a unique positive integer ID for the data set to be added
      int uid = -1; while(uid <= 0 || repository.contains(uid)) uid = (int) (Math.random() * Integer.MAX_VALUE);
      
      // add the dataset
      if(!repository.put(uid, set))
      {
         failureReason = repository.getFailureReason();
         return(false);
      }
      dsid2uidMap.put(set.getID(), uid);
      
      return(true);
   }

   public boolean changeID(String id, String idNew)
   {
      lastError = "";
      if(!preload(true)) return(false);

      if(id == null || !dsid2uidMap.containsKey(id))
      {
         lastError = "Dataset ID not found!";
         return(false);
      }
      if(id.equals(idNew)) return(true);
      
      if(!DataSet.isValidIDString(idNew))
      {
         lastError = "Candidate ID is not a valid DataNav dataset identifier!";
         return(false);
      }
      if(dsid2uidMap.containsKey(idNew))
      {
         lastError = "Candidate ID duplicates that of an existing dataset!";
         return(false);
      }
      
      boolean ok = repository.changeDataSetIDString(dsid2uidMap.get(id), idNew);
      if(ok)
      {
         Integer uid = dsid2uidMap.remove(id);
         dsid2uidMap.put(idNew, uid);
      }
      else if(repository.isUnusable()) failureReason = repository.getFailureReason();
      else lastError = "Data set ID change failed";

      return(ok);
   }

   public boolean removeData(String id)
   {
      lastError = "";
      if(!preload(false)) return(false);

      Integer uid = dsid2uidMap.remove(id);
      if(uid == null) return(true);
      
      boolean ok = repository.remove(uid);
      if(!ok)
      {
         if(repository.isUnusable()) failureReason = repository.getFailureReason();
         else lastError = "Data set removal failed";
      }
      
      return(ok);
   }

   public boolean removeAll()
   {
      lastError = "";
      if(!preload(false)) return(false);
      
      dsid2uidMap.clear();
      boolean ok = repository.removeAll();
      if((!ok) && repository.isUnusable()) failureReason = repository.getFailureReason();
      return(ok);
   }
   
   /** Delegate object that handles all file I/O with the underlying data set repository file. */
   private final DataSetRepository repository;
   
   /** Maps data set ID strings to the unique positive integer ID under which data set is stored in repository. */
   private HashMap<String, Integer> dsid2uidMap = null;
   
   /** Description of error that occurred during last operation; empty string if that operation was successful. */
   private String lastError = "";
   
   /** Description of why data set source has been rendered unusable; null otherwise. */
   private String failureReason = null;
   
   /**
    * Helper method loads the underlying data set repository file and verifies that all data sets contained therein
    * satisfy the constraint that no two data sets have the same string ID. If not, the file is not consistent with the
    * {@link IDataSrc} interface. 
    * @param isGetOp True if operation that triggered the load will attempt to access an existing source file. If file
    * does not exist, abort and report the error. It is OK, however, to write to the file when it does not yet exist, 
    * of course.
    * @return True if successful or if repository is already loaded, false if a fatal error occurs or if file is not a
    * valid data set repository file with unique string IDs assigned to each data set therein.
    */
   private boolean preload(boolean isGetOp)
   {
      if(failureReason != null) return(false);
      if(dsid2uidMap != null) return(true);
      if(isGetOp && !repository.getFilePath().isFile())
      {
         lastError = "File not found!";
         return(false);
      }
      
      if(!repository.preload()) 
      {
         failureReason = repository.getFailureReason();
         return(false);
      }
      
      // prepare data set ID name to integer UID hash map, verifying that no two data sets have the same ID string
      int[] uids = repository.getUIDs();
      dsid2uidMap = new HashMap<>();
      for(int uid : uids)
      {
         DataSetInfo dsi = repository.getDataSetInfo(uid);
         if(dsid2uidMap.containsKey(dsi.getID()))
         {
            dsid2uidMap.clear();
            failureReason = "Bad file format: File contains at least two data sets with the same ID";
            return(false);
         }
         dsid2uidMap.put(dsi.getID(), uid);
      }
      
      return(true);
   }
}
