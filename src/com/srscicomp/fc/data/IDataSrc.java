package com.srscicomp.fc.data;

import java.io.File;

/**
 * This interface defines the requirements on any entity that can source {@link DataSet}s. It is intended to isolate 
 * <i>FigureComposer</i> somewhat from the details of how the data is actually stored. Currently, the interface requires
 * that the data source be a file system object.
 * 
 * <p>Implementations can choose to be "fail-safe" in the sense that, if an operation which modifies the underlying 
 * physical file fails in a way that corrupts that file, the state of the file prior to the operation can be recovered. 
 * The obvious way to do this is to write to a temporary file, then remove the old file and rename the temporary file
 * after the operation succeeded. However, this becomes seriously problematic as the number of data sets stored in the
 * file grows, because you have to rewrite the entire file every time you change it! So "fail-safe" operation is not a
 * requirement.</p>
 * 
 * @author sruffner
 */
public interface IDataSrc 
{
   /**
    * Get the absolute abstract pathname of the physical file associated with this data set source.
    * @return The file encapsulated by this data set source proxy.
    */
   File getSourceFile();
   
   /**
    * Get a brief description of the error that occurred (if any) when one of the data source interface methods is 
    * invoked -- suitable for use in a GUI environment. 
    * @return An empty string if the last method calll was successful; otherwise, a brief message explaining the reason 
    * for the failure. If the data set source has been rendered unusable by a catastrophic failure that has left the 
    * backing file in a corrupted or otherwise unrecoverable state, this method should return a failure description.
    */
   String getLastError();

   /**
    * Has this data set source proxy been rendered unusable by a previous catastrophic failure? If an I/O or other
    * error occurs during any operation that modifies the contents of the source file, the file could be left in a
    * corrupted state. If this is the case, the file is no longer usable.
    * @return True if data set source is unusable because of a previous catastrophic failure.
    */
   boolean isUnusable();
   
   /**
    * Is this data set source read-only?
    * @return True if source is read-only, false if both reading and writing are supported.
    */
   boolean isReadOnly();
   
   /**
    * Get summary information for all data sets available from this data source: the data set identifier, format, 
    * data size and breadth, and selected other parameters. Each data set in the source must have a unique ID. If the 
    * source lacks such IDs, then it must generate them (e.g., "set1", "set2", and so on). These IDs are used to extract
    * a particular data set from the source via {@link #getDataByID()}.
    * 
    * <p>If the source implementor lacks an efficient "table of contents" mechanism, this method could take a while to 
    * execute as the entire source is scanned for IDs. Implementors MUST cache the data set info -- so that subsequent 
    * invocations of this method do not require a source scan.</p>
    * 
    * @return Summary information for all data sets stored in this source; no particular order is guaranteed. If the 
    * source is empty, array is zero-length. Returns null if a problem occurs while reading source, or if source was 
    * rendered unusable by a previous catastrophic error.
    */
   DataSetInfo[] getSummaryInfo();
   
   /**
    * Retrieve a single identified data set from this data source.
    * <p>If the source implementor must sequentially scan for the specified data set, this method could take a while to 
    * execute if the source entity is very large.</p>
    * @param id The data set ID.
    * @return The data set with the specified ID, or null if no such data set exists in source, if a problem occurs 
    * while reading the source, or if the source was rendered unusable by a previous catastrophic error.
    */
   DataSet getDataByID(String id);
   
   /**
    * Store a data set in this data source.
    * @param set The data set to save. If null, the method returns successfully but takes no action.
    * @param replace If a data set with the same ID already exists in the source, that data set is replaced by the new 
    * one if this flag is set; otherwise, the operation fails.
    * @return True if successful; false if a data set with the same ID already exists and <i>replace==false</i>. Also 
    * returns false if the source is read-only, if a problem occurs while writing source, or if the source was rendered
    * unusable by a previous catastrophic error.
    */
   boolean writeData(DataSet set, boolean replace);
   
   /**
    * Change the ID of a single data set in this source.
    * @param id The ID of the affected data set.
    * @param idNew The new data set ID. This must be a valid ID string that does not duplicate the ID of any data set 
    * already stored in the source. See {@link DataSet#isValidIDString()}.
    * @return True if successful; false if the data set was not found or if the candidate ID is invalid or non-unique.
    * Also returns false if this data source is read-only, if a problem occurs while writing source, or if the source 
    * was rendered unusable by a previous catastrophic error.
    */
   boolean changeID(String id, String idNew);
   
   /**
    * Remove a single data set from this data source.
    * 
    * @param id The ID of the data set to be removed. If it does not identify an existing data set in source, the method
    * returns successfully but takes no action.
    * @return True if successful; false if this data set source is read-only, if a problem occurs while writing source,
    * or if the source was rendered unusable by a previous catastrophic error.
    */
   boolean removeData(String id);
   
   /**
    * Remove all data sets from this data source.
    * <p>If the source file did not yet exist -- meaning that this source was already empty --, this method nevertheless
    * writes the source file. Thus, this method provides a means of creating a data source file that has zero data sets
    * within it.</p>
    * @return True if successful; false if this data source is read-only, if a problem occurs while writing source, or
    * if the source was rendered unusable by a previous catastrophic error.
    */
   boolean removeAll();
}
