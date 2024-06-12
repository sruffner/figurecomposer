package com.srscicomp.fc.fypml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.srscicomp.common.util.Utilities;
import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.ISimpleXMLContent;
import com.srscicomp.common.xml.ISimpleXMLContentProvider;
import com.srscicomp.common.xml.StaxWrapper;
import com.srscicomp.common.xml.XMLException;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSetInfo;

/**
 * <b>FGModelSchema</b> handles reading and writing the FypML document defining a <i>DataNav</i> figure model. It
 * implements the {@link ISimpleXMLContentProvider ISimpleXMLContentProvider} interface and
 * provides a number of static methods related to parsing the XML encoding of a <i>DataNav</i> figure.
 * 
 * <p>The graphic model for a <i>DataNav</i> figure, as defined in the <i>com.srscicomp.fc.fig</i> package, is 
 * independent of and developed separately from its serialized XML form. The <i>com.srscicomp.fc.fypml</i> 
 * package encapsulates all "knowledge" of the <i>DataNav</i> figure graphic XML document, including all versions of the 
 * <i>DataNav</i> XML document that have been generated since <i>DataNav</i> was first released.</p>
 * 
 * <p>When a new schema version is introduced, several steps must be taken to incorporate the changes into the schema
 * versioning framework:
 * <ul>
 *    <li>Create a new class to represent the new schema version. It must extend its immediate predecessor and handle
 *    the details of migrating from that version. For example, <code>Schema20</code> implements schema version 20 and 
 *    extends <code>Schema19</code>, which implements version 19.</li>
 *    <li>Update the current version number in the field {@link #CURRENTSCHEMAVERSION} and augment the method {@link 
 *    #getSchemaByVersionNumber(int)} so it can construct an instance of the new schema class.</li>
 *    <li>Update the glue class {@link com.srscicomp.fc.fig.FGModelSchemaConverter FGModelSchemaConverter} to handle 
 *    conversion of the new schema to the actual figure model, {@link com.srscicomp.fc.fig.FGraphicModel FGraphicModel},
 *    and vice versa. If you add any new FypML elements, be sure to update the glue code that matches FypML tag to the
 *    graphic node type and vice versa.</li>
 *    <li>If the new schema introduces any constants required for the XML schema-graphic model conversion, declare those
 *    constants as final static members of this class.</li>
 * </ul>
 * </p>
 * 
 * @author sruffner
 */
public class FGModelSchema implements ISimpleXMLContentProvider
{
   public String getTargetApplication() { return(Schema0Constants.EL_FYP); }

   public ISimpleXMLContent provideContentModel(String procInst)
   {
      ISchema schema = null;
      String target = Schema0Constants.EL_FYP;
      if(procInst != null && procInst.indexOf(target) == 0)
      {
         int nSchema = getSchemaVersionFromPI(procInst.substring(target.length()).trim());
         schema = getSchemaByVersionNumber(nSchema);
      }
      return(schema);
   }

   // A temporary test fixture for migrating older documents to later schema versions
   public static void main(String[] args)
   {
      System.out.println("FypML file migrate schema utility starting up...");
      
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      
      // get full path to figure file
      String input = null;
      System.out.print("\nEnter full path for FypML file to open > ");
      boolean ok = true;
      try
      {
         input = in.readLine().trim();
      }
      catch(IOException ioe)
      {
         System.out.println(" ---> Unexpected IO error while reading user input. QUITTING!");
         ok = false;
      }
      if(!ok) System.exit(0);
      
      File inFile = new File(input);
      ok = FGModelSchema.isFigureModelXMLFile(inFile);
      if(!ok)
      {
         System.out.println(" ---> Specified file does not exist or is not a valid FypML file. QUITTING!");
         System.exit(0);
      }
      int currVersion = FGModelSchema.getSchemaVersion(inFile);
      System.out.println(" : File has FypML schema version = " + currVersion);

      
      System.out.print("\nEnter destination file > ");
      try
      {
         input = in.readLine().trim();
      }
      catch(IOException ioe)
      {
         System.out.println(" ---> Unexpected IO error while reading user input. QUITTING!");
         ok = false;
      }
      if(!ok) System.exit(0);

      if(!input.endsWith(".fyp"))
      {
         System.out.println(" : NOTE -- appending .fyp to output file path...");
         input = input + ".fyp";
      }
      File outFile = new File(input);
      
      StaxWrapper staxWrapper = new StaxWrapper();

      Reader rdr = null;
      ISchema schema = null;
      try
      {
         rdr = new BufferedReader(new FileReader(inFile));
         
         // attempt to parse the content
         schema = (ISchema) staxWrapper.parseContent(rdr, new FGModelSchema());

         // if migration is necessary, do so by chaining from one version to the next until we reach the current version
         int v = schema.getVersion();
         while(v < FGModelSchema.CURRENTSCHEMAVERSION)
         {
            ISchema nextSchema = FGModelSchema.getSchemaByVersionNumber(v+1);
            nextSchema.migrateFromPreviousSchema(schema);
            schema = nextSchema;
            ++v;
         }
      }
      catch(Exception e)
      {
         System.out.println(" ---> Unable to migrate/parse source FypML:\n      " + e.getMessage());
         ok = false;
      }
      finally
      {
         try { if(rdr != null) rdr.close(); } catch(IOException ioe) {}
      }
      if(!ok) System.exit(0);
      
      BufferedWriter wrt = null;
      try 
      { 
         wrt = new BufferedWriter(new FileWriter(outFile));
         staxWrapper.writeContent(wrt, schema, false);
         System.out.println(" : Read and write back succeeded. BYE!");
      }
      catch(Exception e) 
      { 
         System.out.println(" ---> Unable to write FypML:\n      " + e.getMessage()); 
         ok = false;
      }
      finally
      {
         try { if(wrt != null) wrt.close(); } catch(IOException ioe) {}
      }

      System.exit(0);
      
     /*
      System.out.println("Figure document schema migrator starting up...");
      
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      
      // get full path to figure file
      String input = null;
      System.out.print("\nEnter full path for FypML file to migrate > ");
      boolean ok = true;
      try
      {
         input = in.readLine().trim();
      }
      catch(IOException ioe)
      {
         System.out.println(" ---> Unexpected IO error while reading input. QUITTING!");
         ok = false;
      }
      if(!ok) System.exit(0);
      
      File f = new File(input);
      ok = FGModelSchema.isFigureModelXMLFile(f);
      if(!ok)
      {
         System.out.println(" ---> Specified file does not exist or is not a valid FypML file. QUITTING!");
         System.exit(0);
      }
      int currVersion = FGModelSchema.getSchemaVersion(f);
      System.out.println(" : File has FypML schema version = " + currVersion);
      if(currVersion == FGModelSchema.getCurrentSchemaVersion())
      {
         System.out.println(" : This file conforms to the latest schema version. DONE.");
         System.exit(0);
      }
      
      System.out.print("\nEnter target schema # > ");
      try
      {
         input = in.readLine().trim();
      }
      catch(IOException ioe)
      {
         System.out.println(" ---> Unexpected IO error while reading input. QUITTING!");
         ok = false;
      }
      if(!ok) System.exit(0);

      int tgtVersion = -1;
      try { tgtVersion = Integer.parseInt(input); }
      catch(NumberFormatException nfe) { ok = false; }
      if(ok) ok = tgtVersion > currVersion && tgtVersion <= FGModelSchema.getCurrentSchemaVersion();
      if(!ok)
      {
         System.out.println(" ---> Not a valid target schema version for the migrator. QUITTING");
         System.exit(0);
      }
      
      System.out.print("\nEnter full path for migrated file > ");
      try
      {
         input = in.readLine().trim();
      }
      catch(IOException ioe)
      {
         System.out.println(" ---> Unexpected IO error while reading input. QUITTING!");
         ok = false;
      }
      if(!ok) System.exit(0);
      if(!input.endsWith(".fyp"))
      {
         System.out.println(" : NOTE -- appending .fyp to output file path...");
         input = input + ".fyp";
      }
      
      Reader rdr = null;
      ISchema schema = null;
      try
      {
         rdr = new BufferedReader(new FileReader(f));
         
         // attempt to parse the content
         XPPWrapper xppWrapper = new XPPWrapper();
         schema = (ISchema) xppWrapper.parseContent(rdr, new FGModelSchema());

         // if migration is necessary, do so by chaining from one version to the next until we reach the current version
         int v = schema.getVersion();
         while(v < tgtVersion)
         {
            ISchema nextSchema = FGModelSchema.getSchemaByVersionNumber(v+1);
            nextSchema.migrateFromPreviousSchema(schema);
            schema = nextSchema;
            ++v;
         }
      }
      catch(Exception e)
      {
         System.out.println(" ---> Unable to migrate/parse source FypML:\n      " + e.getMessage());
         ok = false;
      }
      finally
      {
         try { if(rdr != null) rdr.close(); } catch(IOException ioe) {}
      }
      if(!ok) System.exit(0);
      
      File outFile = new File(input);
      BufferedWriter wrt = null;
      try 
      { 
         wrt = new BufferedWriter(new FileWriter(outFile));
         XPPWrapper xpp = new XPPWrapper();
         xpp.writeContent(wrt, schema, false);
         System.out.println(" : Migration successful. BYE!");
      }
      catch(Exception e) 
      { 
         System.out.println(" ---> Unable to write migrated FypML:\n      " + e.getMessage()); 
         ok = false;
      }
      finally
      {
         try { if(wrt != null) wrt.close(); } catch(IOException ioe) {}
      }

      System.exit(0);
      */
   }
   
   /** 
    * Return the version number of the current <i>DataNav</i> figure graphics model XML schema.
    * @return The current schema version, a non-negative integer.
    */
   public static int getCurrentSchemaVersion() { return(CURRENTSCHEMAVERSION); }

   /**
    * Get an empty instance of the current <i>DataNav</i> figure graphics model XML schema. The model will be empty
    * initially (ie, null root).
    * @return The schema object. 
    */
   public static ISchema createCurrentSchemaDocument() { return(getSchemaByVersionNumber(CURRENTSCHEMAVERSION)); }
   
   /**
    * Create a blank instance of the current <i>DataNav</i> figure graphics model XML schema, representing a completely
    * empty figure. Unlike {@link #createCurrentSchemaDocument()}, the schema object will have a non-null root.
    * @return The schema object representing an empty figure.
    */
   public static ISchema createCurrentSchemaDocWithRoot()
   {
      ISchema schema = getSchemaByVersionNumber(CURRENTSCHEMAVERSION);
      try { schema.setRootElement(schema.createElement(schema.getRootTag()), false); }
      catch(XMLException xe) {}
      return(schema);
   }
   
   /**
    * Get the schema version for the <em>DataNav</em> XML document in the specified file.
    * @param f The file to be checked.
    * @return The document's schema version number, or -1 if an I/O or parsing exception occurred while attempting to 
    * find version number.
    */
   public static int getSchemaVersion(File f) 
   {
      int version = -1;
      FileReader r = null;
      try 
      { 
         r = new FileReader(f);
         version = FGModelSchema.getSchemaVersion(r); 
      }
      catch(Exception e)
      {
         version = -1;
      }
      finally
      {
         try { if(r != null) r.close(); }
         catch(IOException ioe) {}
      }

      return(version);
   }

   /**
    * Does the specified file contain the XML schema definition of a <i>DataNav</i> figure model? The method only checks
    * for the processing instruction at the beginning of the file to verify the schema version. 
    * <p><b>NOTES</b><ol>
    * <li>Figure files created before schema versioning was introduced -- version 0 files -- lacked the processing 
    * instruction that contains the version number. Thus, this test is only valid for figure files created after 
    * versioning was introduced -- in which case the version reported will be 1 or greater.</li>
    * <li>As of FC 5.4.0, the file name MUST end with the ".fyp" extension.</li>
    * </ol></p>
    * 
    * @param f The file to test.
    * @return True if file has a valid processing instruction that includes a supported <i>DataNav</i> XML schema 
    * version number. Returns false if file not found or any other IO error occurs.
    */
   public static boolean isFigureModelXMLFile(File f)
   {
      if(!"fyp".equals(Utilities.getExtension(f))) return(false);
      
      Reader rdr = null;
      boolean ok = false;
      try
      {
         rdr = new BufferedReader(new FileReader(f));
         int v = FGModelSchema.getSchemaVersion(rdr);
         ok = (v>0 && v <=getCurrentSchemaVersion());   // version 0 files lacked the processing instruction!
      }
      catch(Throwable t) {}
      finally
      {
         try { if(rdr != null) rdr.close(); } catch(IOException ioe) {}
      }
      
      return(ok);
   }
   
   /**
    * Retrieve data set information on all sets stored in the XML schema document defining a <i>DataNav</i> figure.
    * @param doc The XML schema document. <i>It must be the current schema version, or the method fails.</i>
    * @param errBuf If non-null and operation fails, this will contain a brief description of the error that occurred.
    * @return A list of data set information objects, one for each data set stored in the document. If the document
    * schema is not current, or if it is not formatted correctly, method returns null.
    */
   public static List<DataSetInfo> getDataSetInfo(ISchema doc, StringBuffer errBuf)
   {
      if(doc == null) throw new IllegalArgumentException("Null document!");
      List<DataSetInfo> info = new ArrayList<DataSetInfo>();
      try
      {
         BasicSchemaElement fig = (BasicSchemaElement) doc.getRootElement();
         if(fig == null || fig.getChildCount() == 0) throw new XMLException("Null or empty root element!");
         BasicSchemaElement ref = (BasicSchemaElement) fig.getChildAt(fig.getChildCount()-1);
         if(!ref.getTag().equals(EL_REF)) throw new XMLException("Missing element: " + EL_REF);
         for(int i=0; i<ref.getChildCount(); i++)
         {
            BasicSchemaElement eSet = (BasicSchemaElement) ref.getChildAt(i);
            String id = eSet.getAttributeValueByName(FGModelSchema.A_ID);
            String fmt = eSet.getAttributeValueByName(FGModelSchema.A_FMT);
            String using = eSet.getAttributeValueByName(FGModelSchema.A_USING);
            boolean isV7 = "true".equals(eSet.getAttributeValueByName(FGModelSchema.A_V7));
            if(using == null)
            {
               DataSet ds = null;
               if(isV7)
               {
                  float dx = 1;
                  try { dx = Float.parseFloat(eSet.getAttributeValueByName(FGModelSchema.A_DX)); } catch(Throwable t) {}
                  DataSet.Fmt dsFmt = DataSet.Fmt.getFormatByName(fmt);
                  ds = DataSet.fromCommaSeparatedTuples(id, dsFmt, dx, eSet.getTextContent());
               }
               else ds = DataSet.fromBase64(eSet.getTextContent());
               
               if(ds == null) throw new XMLException("Bad dataset content found", FGModelSchema.EL_SET, null);
               if(!(ds.getID().equals(id) && ds.getFormat().toString().equals(fmt)))
                  throw new XMLException("Attributes inconsistent with dataset content", FGModelSchema.EL_SET, null);
               info.add(ds.getInfo());
            }
            else
            {
               // in this case, we need to add info on a dataset with a different ID that is otherwise identical to a 
               // dataset for which info has already been extracted.
               DataSetInfo found = null;
               for(DataSetInfo dsi : info) if(dsi.getID().equals(using) && dsi.getFormat().toString().equals(fmt))
               {
                  found = dsi;
                  break;
               }
               if(found == null)
                  throw new XMLException("Cannot find referenced dataset: " + using, FGModelSchema.EL_SET, 
                        FGModelSchema.A_USING);
               info.add(DataSetInfo.changeID(found, id));
            }
         }
      }
      catch(XMLException xe)
      {
         if(errBuf != null) errBuf.replace(0, errBuf.length(), xe.getMessage());
         return(null);
      }
      
      return(info);
   }
   
   /**
    * Retrieve the data set ID and format string for all datasets stored in the XML schema document defining a 
    * <i>DataNav</i> figure.
    * @param doc The XML schema document. <i>It must be the current schema version, or the method fails.</i>
    * @param errBuf If non-null and operation fails, this will contain a brief description of the error that occurred.
    * @return A list of strings in order: {id1, fmt1, id2, fmt2, ..., idN, fmtN}, where N is the number of data sets in
    * the figure, idM is the ID string for the Mth data set, and fmtM is its data format in string form. If the document
    * schema is not current, or if it is not formatted correctly, method returns null.
    */
   public static List<String> getDataSetIDAndFormat(ISchema doc, StringBuffer errBuf)
   {
      if(doc == null) throw new IllegalArgumentException("Null document!");
      List<String> out = new ArrayList<String>();
      try
      {
         BasicSchemaElement fig = (BasicSchemaElement) doc.getRootElement();
         if(fig == null || fig.getChildCount() == 0) throw new XMLException("Null or empty root element!");
         BasicSchemaElement ref = (BasicSchemaElement) fig.getChildAt(fig.getChildCount()-1);
         if(!ref.getTag().equals(EL_REF)) throw new XMLException("Missing element: " + EL_REF);
         for(int i=0; i<ref.getChildCount(); i++)
         {
            BasicSchemaElement eSet = (BasicSchemaElement) ref.getChildAt(i);
            String id = eSet.getAttributeValueByName(FGModelSchema.A_ID);
            String fmt = eSet.getAttributeValueByName(FGModelSchema.A_FMT);
            out.add(id);
            out.add(fmt);
         }
      }
      catch(XMLException xe)
      {
         if(errBuf != null) errBuf.replace(0, errBuf.length(), xe.getMessage());
         return(null);
      }
      
      return(out);
   }
   
   /**
    * Strip all raw data from the data sets stored in the XML schema document defining a <i>DataNav</i> figure.
    * <p>This method is useful for creating a figure "template" of minimum size from a normal figure in which 
    * potentially large data sets are embedded. It simply replaces each non-empty data set found in the <i>ref</i>
    * node with an empty set with the same ID, format, and parameters.</p>
    * @param srcDoc The XML schema document. It will NOT be altered by this method. <i>It must be the current schema 
    * version, or the method fails.</i>
    * @return A copy of the original schema document, stripped of all raw data sets. Returns null if operation failed.
    */
   public static ISchema removeRawData(ISchema srcDoc)
   {
      if(srcDoc == null || srcDoc.getVersion() != FGModelSchema.getCurrentSchemaVersion())
         return(null);
 
      // make a copy so we don't alter the original!
      ISchema doc = null;
      try
      {
         JSONObject jsonSchema = FGModelSchema.toJSON(srcDoc);
         doc = FGModelSchema.fromJSON(jsonSchema);
      }
      catch(JSONException jse) {}
      if(doc == null) return(null);
      
      String emsg = null;
      try
      {
         BasicSchemaElement fig = (BasicSchemaElement) doc.getRootElement();
         if(fig == null || fig.getChildCount() == 0) throw new XMLException("Null or empty root element!");
         BasicSchemaElement ref = (BasicSchemaElement) fig.getChildAt(fig.getChildCount()-1);
         if(!ref.getTag().equals(EL_REF)) throw new XMLException("Missing element: " + EL_REF);
         for(int i=0; i<ref.getChildCount(); i++)
         {
            BasicSchemaElement eSet = (BasicSchemaElement) ref.getChildAt(i);
            String id = eSet.getAttributeValueByName(FGModelSchema.A_ID);
            String fmt = eSet.getAttributeValueByName(FGModelSchema.A_FMT);
            String using = eSet.getAttributeValueByName(FGModelSchema.A_USING);
            boolean isV7 = "true".equals(eSet.getAttributeValueByName(FGModelSchema.A_V7));
            if(using == null)
            {
               DataSet ds = null;
               if(isV7)
               {
                  float dx = 1;
                  try { dx = Float.parseFloat(eSet.getAttributeValueByName(FGModelSchema.A_DX)); } catch(Throwable t) {}
                  DataSet.Fmt dsFmt = DataSet.Fmt.getFormatByName(fmt);
                  ds = DataSet.fromCommaSeparatedTuples(id, dsFmt, dx, eSet.getTextContent());
               }
               else ds = DataSet.fromBase64(eSet.getTextContent());
               
               if(ds == null) throw new XMLException("Bad dataset content found", FGModelSchema.EL_SET, null);
               if(!(ds.getID().equals(id) && ds.getFormat().toString().equals(fmt)))
                  throw new XMLException("Attributes inconsistent with dataset content", FGModelSchema.EL_SET, null);
               
               // replace text content with an empty version of the same dataset
               DataSet emptyDS = DataSet.createEmptySet(ds.getFormat(), ds.getID(), ds.getParams());
               eSet.setTextContent(DataSet.toBase64(emptyDS, false), false);
               if(isV7) eSet.setAttributeValueByName(FGModelSchema.A_V7, "false");
            }
         }
      }
      catch(XMLException xe)
      {
         emsg = xe.getMessage();
      }
      
      return(emsg == null ? doc : null);
   }
   
   /**
    * Inject a raw dataset into the XML schema document defining a <i>DataNav</i> figure, replacing an existing dataset
    * with the same identifier and dataset format. If no match is found, no action is taken.
    * @param doc The XML schema document. <i>It must be the current schema version, or the method fails.</i>
    * @param ds The dataset to be injected.
    * @return 1 if dataset was successfully injected, 0 if no matching dataset was found in figure, -1 if an error was
    * detected.
    */
   public static int injectRawData(ISchema doc, DataSet ds)
   {
      if(doc == null || ds == null) throw new IllegalArgumentException("Null argument!");
      int res = 0;
      try
      {
         BasicSchemaElement fig = (BasicSchemaElement) doc.getRootElement();
         if(fig == null || fig.getChildCount() == 0) throw new XMLException("Null or empty root element!");
         BasicSchemaElement ref = (BasicSchemaElement) fig.getChildAt(fig.getChildCount()-1);
         if(!ref.getTag().equals(EL_REF)) throw new XMLException("Missing element: " + EL_REF);
         for(int i=0; i<ref.getChildCount(); i++)
         {
            BasicSchemaElement eSet = (BasicSchemaElement) ref.getChildAt(i);
            String id = eSet.getAttributeValueByName(FGModelSchema.A_ID);
            String fmt = eSet.getAttributeValueByName(FGModelSchema.A_FMT);
            if(ds.getID().equals(id) && ds.getFormat().toString().equals(fmt))
            {
               // found the match! Since we're injecting real data in base64 form, be sure that "using" attribute is 
               // not defined and that the V7 attribute is "false".
               eSet.removeAttributeByName(FGModelSchema.A_USING);
               eSet.setAttributeValueByName(FGModelSchema.A_V7, "false");
               eSet.setTextContent(DataSet.toBase64(ds, true), false);
               res = 1;
               break;
            }
         }
      }
      catch(XMLException xe)
      {
         res = -1;
      }
      
      return(res);
   }
   
   /**
    * Convert the <i>DataNav</i> figure graphic XML document to a single JSON object. If the supplied document does not
    * match the current schema version, it is migrated to that version before converting to JSON format. This method may 
    * take a significant amount of time to execute, so it should be invoked on a background thread. The JSON object 
    * prepared is essentially the root "figure" element converted to a JSON object as described in 
    * <code>BasicSchemaElement.toJSON()</code>, with one field added: the "schema" field is set to an integer indicating
    * the schema version number. This should be the current schema version, since the schema document is migrated if it
    * conforms to an older version.</p>
    * 
    * @param schema A <i>DataNav</i> figure graphic XML document, conforming to the current or an older schema version.
    * If schema migration is necessary, this document will be emptied in the process -- do not reuse!
    * @return A single JSON object encapsulating the figure graphic XML document, as described.
    * @throws JSONException if an error occurs during schema migration or conversion to the JSON format.
    */
   public static JSONObject toJSON(ISchema schema) throws JSONException
   {
      if(schema == null) throw new IllegalArgumentException("Null argument!");
      
      // if migration is necessary, do so by chaining from one version to the next until we reach the current version 
      try
      {
         while(schema.getVersion() < FGModelSchema.getCurrentSchemaVersion())
         {
            ISchema nextSchema = FGModelSchema.getSchemaByVersionNumber(schema.getVersion()+1);
            nextSchema.migrateFromPreviousSchema(schema);
            schema = nextSchema;
         }
      }
      catch(XMLException xe)
      {
         throw new JSONException("Failed during schema migration:\n" + xe.getMessage());
      }

      JSONObject jsonDoc = ((BasicSchemaElement) schema.getRootElement()).toJSON();
      jsonDoc.put("schema", schema.getVersion());
      
      return(jsonDoc);
   }
   
   /**
    * Translate the supplied JSON object into a <i>DataNav</i> figure graphic XML document to a single JSON object. 
    * If the schema document is successfully constructed but conforms to an older schema version, it is migrated to the
    * current version. This method may take a significant amount of time to execute, so it should be invoked on a 
    * background thread. The JSON object should be formatted as described in <code>toJSON()</code>.
    * 
    * @param jsonDoc A JSON object defining a <i>DataNav</i> figure graphic XML document, as described.
    * @return The figure graphic XML document, translated from the JSON object and migrated to the current version
    * if necessary.
    * @throws JSONException if an error occurs during schema migration or conversion from the JSON format.
    */
   public static ISchema fromJSON(JSONObject jsonDoc) throws JSONException
   {
      if(jsonDoc == null) throw new IllegalArgumentException("Null argument!");
      
      int nSchema = jsonDoc.getInt("schema");
      ISchema schema = FGModelSchema.getSchemaByVersionNumber(nSchema);
      if(schema == null) throw new JSONException("Invalid schema version: " + nSchema);
      
      try
      {
         BasicSchemaElement root = (BasicSchemaElement) schema.createElement(EL_FIGURE);
         root.fromJSON(jsonDoc);
         schema.setRootElement(root, true);
      }
      catch(XMLException xe)
      {
         throw new JSONException("Validation failed after converting JSON object to XML figure graphic:\n   " + xe.getMessage());
      }
      
      // if migration is necessary, do so by chaining from one version to the next until we reach the current version 
      try
      {
         while(schema.getVersion() < FGModelSchema.getCurrentSchemaVersion())
         {
            ISchema nextSchema = FGModelSchema.getSchemaByVersionNumber(schema.getVersion()+1);
            nextSchema.migrateFromPreviousSchema(schema);
            schema = nextSchema;
         }
      }
      catch(XMLException xe)
      {
         throw new JSONException("Failed during schema migration:\n" + xe.getMessage());
      }

      return(schema);
   }
   
   /**
    * Save the <i>DataNav</i> figure graphic XML document to the specified file. If the supplied document does not match
    * the current schema version, it is migrated to that version before writing to the file. This method may take a 
    * significant amount of time to execute, so it should be invoked on a background thread.
    * 
    * @param schema A <i>DataNav</i> figure graphic XML document, conforming to the current or an older schema version.
    * If schema migration is necessary, this document will be emptied in the process -- do not reuse!
    * @param f The target file. If it already exists, its contents will be overwritten.
    * @return Null if operation succeeds; else a brief message explaining the error that occurred.
    */
   public static String toXML(ISchema schema, File f)
   {
      String errDesc = null;
      BufferedWriter wrt = null;
      try 
      { 
         wrt = new BufferedWriter(new FileWriter(f));
         errDesc = toXML(schema, wrt); 
      }
      catch(IOException ioe) 
      { 
         errDesc = "Unable to write DataNav XML:\n" + ioe.getMessage(); 
      }
      finally
      {
         try { if(wrt != null) wrt.close(); } catch(IOException ioe) {}
      }
      
      return(errDesc);
   }

   /**
    * Save the <i>DataNav</i> figure graphic XML document to the specified stream writer. If the supplied document does
    * not match the current schema version, it is migrated to that version before writing to the stream. This method may 
    * take a significant amount of time to execute, so it should be invoked on a background thread.
    * 
    * @param schema A <i>DataNav</i> figure graphic XML document, conforming to the current or an older schema version.
    * If schema migration is necessary, this document will be emptied in the process -- do not reuse!
    * @param writer. The stream writer to which the XML document is written. The writer is <b>NOT</b> closed after the 
    * operation is completed.
    * @return Null if operation succeeds; else a brief message explaining the error that occurred.
    */
   public static String toXML(ISchema schema, Writer writer)
   {
      if(schema == null || writer == null) throw new IllegalArgumentException("Null argument!");
      
      String errDesc = null;
      try
      {
         // if migration is necessary, do so by chaining from one version to the next until we reach the current version 
         while(schema.getVersion() < FGModelSchema.getCurrentSchemaVersion())
         {
            ISchema nextSchema = FGModelSchema.getSchemaByVersionNumber(schema.getVersion()+1);
            nextSchema.migrateFromPreviousSchema(schema);
            schema = nextSchema;
         }

         StaxWrapper staxWriter = new StaxWrapper();
         staxWriter.writeContent(writer, schema, false);
      }
      catch(XMLException xe)
      {
         errDesc = "Unable to write DataNav XML:\n" + xe.getMessage();
      }

      return(errDesc);
   }

   /**
    * Retrieve the <i>DataNav</i> figure graphic model XML document stored in the specified file. If the file contents 
    * conform to an older schema, that schema document is migrated to the current schema. This method may take a 
    * significant amount of time to execute, so it should be invoked on a background thread.
    * 
    * @param f The plain text file that contains a <i>DataNav</i> graphic model XML document.
    * @param errBuf If non-null and operation fails, this will contain a brief description of the error that occurred.
    * @return The XML document, conforming to the current schema version. Returns null if the operation fails.
    */
   public static ISchema fromXML(File f, StringBuffer errBuf)
   {
      Reader rdr = null;
      ISchema schema = null;
      try
      {
         rdr = new BufferedReader(new FileReader(f));
         schema = fromXML(rdr, errBuf);
      }
      catch(FileNotFoundException fnfe)
      {
         if(errBuf != null) 
            errBuf.replace(0, errBuf.length(), "Unable to migrate/parse DataNav XML:\n" + fnfe.getMessage());
      }
      finally
      {
         try { if(rdr != null) rdr.close(); } catch(IOException ioe) {}
      }
      
      return(schema);
   }

   /**
    * Parse a <i>DataNav</i> figure graphic model XML document from the contents of the specified character stream 
    * reader. If the content stream conforms to an older schema, that schema document is migrated to the current schema.
    * This method may take a significant amount of time to execute, so it should be invoked on a background thread.
    * 
    * @param reader The character stream reader that sources a <i>DataNav</i> graphic XML document. The reader is 
    * <b>NOT</b> closed after the operation is completed. Reader must support the <code>mark()</code> and 
    * <code>reset()</code> operations!
    * @param errBuf If non-null and operation fails, this will contain a brief description of the error that occurred.
    * @return The XML document parsed from the character stream. Returns null if the operation fails.
    */
   public static ISchema fromXML(Reader reader, StringBuffer errBuf)
   {
      ISchema result = null;
      String errDesc = null;
      try
      {
         // attempt to parse the content
         StaxWrapper staxParser = new StaxWrapper();
         ISchema schema = (ISchema) staxParser.parseContent(reader, new FGModelSchema());
         assert(schema != null);
         
         // if migration is necessary, do so by chaining from one version to the next until we reach the current version
         int v = schema.getVersion();
         while(v < FGModelSchema.getCurrentSchemaVersion())
         {
            ISchema nextSchema = FGModelSchema.getSchemaByVersionNumber(v+1);
            nextSchema.migrateFromPreviousSchema(schema);
            schema = nextSchema;
            ++v;
         }
         
         result = schema;
      }
      catch(XMLException xe)
      {
         errDesc = "Unable to migrate/parse DataNav XML:\n" + xe.getMessage();
      }
      catch(Exception e)
      {
         errDesc = "Unexpected exception:\n" + e.getMessage();
      }
      
      if(result == null && errBuf != null)
         errBuf.replace(0, errBuf.length(), errDesc);
      
      return(result);
   }
   
   /** The version number for <i>DataNav</i>'s current XML schema. */
	private final static int CURRENTSCHEMAVERSION = 26;

	/**
	 * This is essentially a factory method that returns the specific implementation of {@link 
	 * ISchema ISchema} that encapsulates a FypML document conforming to the specified schema
	 * version number.
	 * 
	 * @param version The schema version number.
	 * @return The corresponding schema, or null if version number is not recognized.
	 */
	private static ISchema getSchemaByVersionNumber(int version)
	{
		ISchema schema = null;
		if(version == 0) schema = new BaseSchema();
		else if(version == 1) schema = new Schema1();
		else if(version == 2) schema = new Schema2();
		else if(version == 3) schema = new Schema3();
		else if(version == 4) schema = new Schema4();
      else if(version == 5) schema = new Schema5();
      else if(version == 6) schema = new Schema6();
      else if(version == 7) schema = new Schema7();
      else if(version == 8) schema = new Schema8();
      else if(version == 9) schema = new Schema9();
      else if(version == 10) schema = new Schema10();
      else if(version == 11) schema = new Schema11();
      else if(version == 12) schema = new Schema12();
      else if(version == 13) schema = new Schema13();
      else if(version == 14) schema = new Schema14();
      else if(version == 15) schema = new Schema15();
      else if(version == 16) schema = new Schema16();
      else if(version == 17) schema = new Schema17();
      else if(version == 18) schema = new Schema18();
      else if(version == 19) schema = new Schema19();
      else if(version == 20) schema = new Schema20();
      else if(version == 21) schema = new Schema21();
      else if(version == 22) schema = new Schema22();
      else if(version == 23) schema = new Schema23();
      else if(version == 24) schema = new Schema24();
      else if(version == 25) schema = new Schema25();
      else if(version == 26) schema = new Schema26();
		
		return(schema);
	}

	/**
	 * Retrieve the schema version for the <i>DataNav</i> XML content delivered by a character stream reader.
	 * <p>The schema version is stored in a processing instruction assumed to be found near the beginning of the stream, 
	 * prior to the root element's start tag. If the processing instruction is not found, then the document is assumed 
    * to have been produced prior to the introduction of versioning.</p>
	 * @param r The character stream reader providing the XML content. Reader is not closed by this method.
	 * @return The content's schema version. Returns 0 if schema version not found, -1 if the processing instruction 
    * is not in the correct form.
	 * @throws XMLException if any error occurs while checking the schema version.
    * @see BaseSchema#getProcessingInstruction()
	 */
	public static int getSchemaVersion(Reader r) throws XMLException
	{
		int nSchema = 0;
		StaxWrapper staxParser = new StaxWrapper();
		String procInst = staxParser.getProcessingInstruction(r, Schema0Constants.EL_FYP);
		if(procInst.length() > 0) nSchema = getSchemaVersionFromPI(procInst);

		return(nSchema);
	}
	
	/**
	 * Extract the schema version number from a properly formatted processing instruction in a <i>DataNav</i> figure
	 * XML document. Such documents include a PI of the form <i>&lt;?fyp appVersion=N schemaVersion=M&gt;</i>, where
	 * M is the schema version to which the document conforms.
	 * @param procInst The processing instruction with XML tags and the target token ("fyp") stripped off.
	 * @return The schema version, or -1 if processing instruction is not properly formatted.
	 */
	private static int getSchemaVersionFromPI(String procInst)
	{
	   int nSchema = -1;
	   if(procInst != null && procInst.length() > 0)
	   {
         int n1 = procInst.indexOf("schemaVersion");
         if(n1 > -1) 
         {
            n1 = procInst.indexOf('"', n1) + 1;
            int n2 = procInst.lastIndexOf('"');
            if(n1 > 0 && n2 > -1 && n1 < n2)
            {
               try { nSchema = Integer.parseInt( procInst.substring(n1, n2) ); }
               catch(NumberFormatException nfe) { nSchema = -1; }
            }
         }
	   }
	   return(nSchema);
	}
	
	//
	// Schema-related constants required for model-schema conversion
	//
	/** (V>=0) The "figure" element, which is the root element in the <i>DataNav</i> graphic XML schema. */
	public final static String EL_FIGURE = Schema0Constants.EL_FIGURE;
	/** (V>=0) The "graph" element. */
   public final static String EL_GRAPH = Schema0Constants.EL_GRAPH;
   /** (V>=22) A specialized 2D polar graph element. */
   public final static String EL_PGRAPH = Schema22.EL_PGRAPH;
   /** (V>=22) Element representing the theta or radial axis compoment of a 2D polar plot. */
   public final static String EL_PAXIS = Schema22.EL_PAXIS;
   /** (V>=21) The 3D graph element. */
   public final static String EL_GRAPH3D = Schema21.EL_GRAPH3D;
   /** (V>=0) The "label" element defines a single-line text label. */
   public final static String EL_LABEL = Schema0Constants.EL_LABEL;
   /** (V>=15) The "textbox" element defines a multi-line text label restricted by the width of a bounding box. */
   public final static String EL_TEXTBOX = Schema15.EL_TEXTBOX;
   /** (V>=15) The "image" element embeds a PNG image within a specified bounding box, preserving its aspect ratio. */
   public final static String EL_IMAGE = Schema15.EL_IMAGE;
   /** (V>=0) The "line" element governs the location and appearance of an arbitrary line segment. */
   public final static String EL_LINE = Schema0Constants.EL_LINE;
   /** (V>=3) The "shape" element defines and renders a simple geometric shape. */
   public final static String EL_SHAPE = Schema3.EL_SHAPE;
   /** (V>=0) The "calib" element governs the appearance of a calibration bar within a graph. */
   public final static String EL_CALIB = Schema0Constants.EL_CALIB;
   /** (V>=0) The "function" element defines and renders a mathematical function f(x), x=[x0,x0+dx,...,x1]. */
   public final static String EL_FUNCTION = Schema0Constants.EL_FUNCTION;
   /** (V>=6) The "trace" element renders 2D datasets (point set, series, or collections thereof) within a graph. */
   public final static String EL_TRACE = Schema6.EL_TRACE;
   /** 
    * (V>=0) The "axis" element governs appearance of a graph's primary or secondary axis.
    * (V>=21) Similarly applies to any of the 3 axes of a 3D graph object.
    */
   public final static String EL_AXIS = Schema0Constants.EL_AXIS;
   /** (V>=0) The "ticks" element governs appearance of a tick mark set defined on a graph axis. */
   public final static String EL_TICKS = Schema0Constants.EL_TICKS;
   /** (V>=0) The "legend" element governs appearance of a graph's automated legend. */
   public final static String EL_LEGEND = Schema0Constants.EL_LEGEND;
   /** 
    * (V>=2) The "gridline element" governs appearance of the primary or secondary axis grid lines of a graph. 
    * (V>=21) Similarly applies to the 3D graph -- one each for the X, Y, and Z axes.
    */
   public final static String EL_GRIDLINE = Schema2.EL_GRIDLINE;
   /** 
    * (V>=21) The "back3d" element governs the appearance of one of the back planes (XY, XZ, or YZ) of a 3D graph in
    * either of two possible boxed backdrop styles.
    */
   public final static String EL_BACK3D = Schema21.EL_BACK3D;
   /** (V>=4) The "symbol" element governs appearance of marker symbols rendered at data point locations. */
   public final static String EL_SYMBOL = Schema4.EL_SYMBOL;
   /** (V>=2) The "ebar" element governs appearance of error bars or traces on selected data presentation nodes. */
   public final static String EL_EBAR = Schema2.EL_EBAR;
   /** (V>=8) The "raster" element is the sole data presentation element for "raster1d" datasets. */
   public final static String EL_RASTER = Schema8.EL_RASTER;
   /** 
    * (V>=8 && V<22) The "heatmap" element is the sole data presentation element for "xyzimg" datasets (3D data). It
    * was replaced by the "contour" element in schema V22. 
    */
   public final static String EL_HEATMAP = Schema8.EL_HEATMAP;
   /** (V>=22) The "contour" element presents "xyzimg" data sets as a contour plot or heat map plot. */
   public final static String EL_CONTOUR = Schema22.EL_CONTOUR;
   /** (V>=17) This data presentation element renders a typical bar plot. */
   public final static String EL_BAR = Schema17.EL_BAR;
   /** (V>=25) This data presentation element renders a box plot. */
   public final static String EL_BOX = Schema25.EL_BOX;
   /** (V>=26) Component of a "box" element defining the styling and size of the box plot's companion violin plot. */
   public final static String EL_VIOLIN = Schema26.EL_VIOLIN;
   /** (V>=18) This data presentation element renders a scatter or bubble plot. */
   public final static String EL_SCATTER = Schema18.EL_SCATTER;
   /** 
    * (V>=26) Dedicated component of the "scatter" plot element that governs appearance of the LMS regression line in
    * the "trendline" display mode or a "connect the dots" polyline in the other display modes.
    */
   public final static String EL_SCATTERLINE = Schema26.EL_SCATTERLINE;
   /** (V>=19) This data presentation element renders a (stacked) area chart. */
   public final static String EL_AREA = Schema19.EL_AREA;
   /** (V>=21) This data presentation element renders a 3D scatter plot (3D graphs only). */
   public final static String EL_SCATTER3D = Schema21.EL_SCATTER3D;
   /** (V>=21) This data presentation element renders a 3D surface plot (3D graphs only). */
   public final static String EL_SURFACE = Schema21.EL_SURFACE;
   /** (V>=19) This data presentation element renders a pie chart. */
   public final static String EL_PIE = Schema19.EL_PIE;
   /** (9<=V<=22) The "zaxis" element represents the graph axis corresponding to the third, or Z, dimension. */
   public final static String EL_ZAXIS = Schema9.EL_ZAXIS;
   /** 
    * (V>=23) The "colorbar" element replaces the "zaxis" element, which has always been used to define the colormap
    * for a graph container and an associated "colorbar" that depicts the color gradient mapped to the Z data range.
    */
   public final static String EL_COLORBAR = "colorbar";
   /** (V>=6) The "ref" element holds the datasets rendered in the figure in one or more "set" nodes. */
   public final static String EL_REF = Schema6.EL_REF;
   /** (V>=6) The "set" element encapsulates a <i>data set</i> rendered in the figure. */
   public final static String EL_SET = Schema6.EL_SET;

   /** (V>=0) The "font" attribute specifies name of font family to use when rendering text. */
   public final static String A_FONT = Schema0Constants.A_FONT;
   /** (V>=7) The "psFont" attribute specifies the font to use when exporting graphic to Postscript. */
   public final static String A_PSFONT = Schema7.A_PSFONT_V7;
   /** (V>=7) The "altFont" attribute specifies a generic font to use when the desired font family is not available. */
   public final static String A_ALTFONT = Schema7.A_ALTFONT;
   /** (V>=0) The "fontSize" attribute specifies the text font size: numerical value plus measurement units. */
   public final static String A_FONTSIZE = Schema0Constants.A_FONTSIZE;
   /** (V>=0) The "fontStyle" attribute selects the text font style: plain, bold, italic, bold-italic. */
   public final static String A_FONTSTYLE = Schema0Constants.A_FONTSTYLE;
   /** (V>=0) The "fillColor" attribute defines the color used to paint text or fill closed paths. */
   public final static String A_FILLCOLOR = Schema0Constants.A_FILLCOLOR;
   /** (V>=0) The "strokeWidth" attribute selects the width of the pen used to stroke paths. */
   public final static String A_STROKEWIDTH = Schema0Constants.A_STROKEWIDTH;
   /** (V>=10) The "strokeCap" attribute selects the endpoint decoration applied when stroking open paths. */
   public final static String A_STROKECAP = Schema10.A_STROKECAP;
   /** 
    * (V>=10) The "strokeJoin" attribute defines how adjacent path segments or the endpoints of a closed path are
    * joined when stroked.
    */
   public final static String A_STROKEJOIN = Schema10.A_STROKEJOIN;
   /** (V>=0) The "strokeColor" attribute selects the color used to stroke paths. */
   public final static String A_STROKECOLOR = Schema0Constants.A_STROKECOLOR;
   /** (V>=7) The "strokePat" attribute selects the stroke pattern style: solid, dotted, etc. */
   public final static String A_STROKEPAT = Schema7.A_STROKEPAT;

   /** 
    * (V>=0) The "border" attribute specifies width of the border around the bounding box of a figure, or
    * (V>=23) a graph legend.
    */
   public final static String A_BORDER = Schema0Constants.A_BORDER;
   /** (V>=7) The default border width (when "border" attribute not explicitly specified). */
   public final static String DEFAULT_BORDER = Schema7.DEFAULT_BORDER;
   /** (V>=0) The "title" attribute defines a title (rendered or not) for the owner element. */
   public final static String A_TITLE = Schema0Constants.A_TITLE;
   /** (V>=7) The default title (when "title" attribute not explicitly specified). */
   public final static String DEFAULT_TITLE = Schema7.DEFAULT_TITLE;
   /** (V>=0) The "loc" attribute defines the location of some elements with respect to their parent's viewport. */
   public final static String A_LOC = Schema0Constants.A_LOC;
   /** (V>=0) The "width" attribute defines the viewport width for selected elements. */
   public final static String A_WIDTH = Schema0Constants.A_WIDTH;
   /**
    * (V>=0) The "height" attribute defines the viewport height of selected elements, or
    * (V>=8) the height of a raster's hash marks. 
    */
   public final static String A_HEIGHT = Schema0Constants.A_HEIGHT;
   /** (V>=8) Default height for a raster's hash marks (when "height" attribute not explicitly specified). */
   public final static String DEFAULT_RASTER_HEIGHT = Schema8.DEFAULT_RASTER_HEIGHT;

   /** (V>=19) The "note" attribute for the root "figure" node: an optional figure description (not rendered). */
   public final static String A_NOTE = Schema19.A_NOTE;
   /** (V>=19) The default figure note/description (when "note" attribute not explicitly specified). */
   public final static String DEFAULT_NOTE = Schema19.DEFAULT_NOTE;

   /**
    * (V>=0) The "rotate" attribute defines angle with which element's viewport is rotated about its origin. 
    * (V>=21) For the "graph3d" element, it defines the rotation angle about the Z-axis of the 3D graph.
    */
   public final static String A_ROTATE = Schema0Constants.A_ROTATE;
   /** (V>=7) The default rotation angle in degrees (when "rotate" attribute not explicitly specified). */
   public final static String DEFAULT_ROTATE = Schema7.DEFAULT_ROTATE;
   /** (V>=21) Default value for the {@link #A_ROTATE} attribute of a 3D graph. */
   public final static String DEFAULT_GRAPH3D_ROTATE = Schema21.DEFAULT_GRAPH3D_ROTATE;
   /** (V>=0) The "type" attribute selects type of graph, shape, or marker symbol. */
   public final static String A_TYPE = Schema0Constants.A_TYPE;
   /** (V>=7) The default graph type (when "type" is not explicitly specified). */
   public final static String DEFAULT_GRAPH_TYPE = Schema7.DEFAULT_GRAPH_TYPE;
   /** (V>=18) The default scatter plot marker symbol type (when "type" is not explicitly specified). */
   public final static String DEFAULT_SCATTER_TYPE = Schema18.DEFAULT_SCATTER_TYPE;
   /** (V>=0) The "layout" attribute for a graph element selects the axis layout: 1st quadrant, 2nd quadrant, etc. */
   public final static String A_LAYOUT = Schema0Constants.A_LAYOUT;
   /** (V>=7) The default graph axis layout (when "layout" attribute is not explicitly specified). */
   public final static String DEFAULT_LAYOUT = Schema7.DEFAULT_LAYOUT;
   /** 
    * (V>=0) The boolean "clip" attribute determines whether or not datasets are clipped to the graph's data window. 
    * Also applicable to the 2D polar plot element (V>=22).
    * (V>=15) Same attribute determines whether or not text is clipped to the bounding box of a text box element.
    * */
   public final static String A_CLIP = Schema0Constants.A_CLIP;
   /** (V>=7) The default clip state for a graph or (V>=15) text box (when not explicitly specified). */
   public final static String DEFAULT_CLIP = Schema7.DEFAULT_CLIP;
   /** (V>=22) Default value for the {@link #A_CLIP} attribute of a 2D polar plot. */
  public final static String DEFAULT_PGRAPH_CLIP = Schema22.DEFAULT_PGRAPH_CLIP;
   /** (V>=13) The "autorange" attribute enables automatic range adjustment on each graph axis independently. */
   public final static String A_AUTORANGE = "autorange";
   /** (V>=13) Default value for the "autorange" enumerated attribute for a "graph" element. */
   public final static String DEFAULT_AUTORANGE = Schema13.AUTORANGE_NONE;
   /** 
    * (V>=22) The "boxColor" attribute sets the background color of the data box for a 2D graph or the polar grid for
    * a 2D polar plot.
    * (V>=23) It also specifies the background color for the bounding box of a graph legend.
    */
   public final static String A_BOXCOLOR = Schema22.A_BOXCOLOR;
   /** (V>=22) Default value for the "boxColor" attribute of a 2D graph or 2D polar plot, or (V>=23) a graph legend. */
   public final static String DEFAULT_BOXCOLOR = Schema22.DEFAULT_BOXCOLOR;

   /** (V>=21) Attribute sets the elevation angle for the 3D graph node (rotation about X-axis), in degrees. */
   public final static String A_ELEVATE = Schema21.A_ELEVATE;
   /** (V>=21) Default value for the {@link #A_ELEVATE} attribute of a 3D graph. */
   public final static String DEFAULT_ELEVATE = Schema21.DEFAULT_ELEVATE;
   /** (V>=21) Attribute selects the backdrop style for the 3D graph node. */
   public final static String A_BACKDROP = Schema21.A_BACKDROP;
   /** (V>=21) Default value for the {@link #A_BACKDROP} attribute of a 3D graph. */
   public final static String DEFAULT_BACKDROP = Schema21.DEFAULT_BACKDROP;
   /** (V>=21) Attribute sets the projection distance scale factor for a 3D graph. */
   public final static String A_PSCALE = Schema21.A_PSCALE;
   /** (V>=21) Default value for the {@link #A_PSCALE} attribute of a 3D graph. */
   public final static String DEFAULT_PSCALE = Schema21.DEFAULT_PSCALE;
   /** (V>=21) Attribute specifies the depth of a 3D graph (extent in the Z direction). */
   public final static String A_DEPTH = Schema21.A_DEPTH;
   /** (V>=21) Attribute sets the mesh size limit for a 3D surface plot. */
   public final static String A_LIMIT = Schema21.A_LIMIT;
   /** (V>=21) Default value for the {@link #A_LIMIT} attribute of a 3D surface plot. */
   public final static String DEFAULT_LIMIT = Schema21.DEFAULT_LIMIT;


   /** (V>=0) The "align" attribute, which specifies horizontal alignment for a label or text box element. */
   public final static String A_HALIGN = Schema0Constants.A_HALIGN;
   /** (V>=0) Value for "align" attribute indicating left-aligned or leading text. */
   public final static String HALIGN_LEFT = Schema0Constants.HALIGN_LEFT;
   /** (V>=0) Value for "align" attribute indicating right-aligned or trailing text. */
   public final static String HALIGN_RIGHT = Schema0Constants.HALIGN_RIGHT;
   /** (V>=0) Value for "align" attribute indicating horizontally centered text. */
   public final static String HALIGN_CENTER = Schema0Constants.HALIGN_CENTER;
   /** (V>=0) The "valign" attribute, which specifies vertical alignment for a label or text box element. */
   public final static String A_VALIGN = Schema0Constants.A_VALIGN;
   /** (V>=0) Value for "valign" attribute indicating bottom-aligned text. */
   public final static String VALIGN_BOTTOM = Schema0Constants.VALIGN_BOTTOM;
   /** (V>=0) Value for "valign" attribute indicating top-aligned text. */
   public final static String VALIGN_TOP = Schema0Constants.VALIGN_TOP;
   /** (V>=0) Value for "valign" attribute indicating vertically centered text. */
   public final static String VALIGN_CENTER = Schema0Constants.VALIGN_CENTER;
   
   /** (V>=0) Default horizontal alignment ("align" attribute) for a label element. */
   public final static String DEFAULT_LABEL_HALIGN = HALIGN_LEFT;
   /** (V>=0) Default vertical alignment ("valign" attribute) for a label element. */
   public final static String DEFAULT_LABEL_VALIGN = VALIGN_BOTTOM;
   /** (V>=15) Default horizontal alignment ("align" attribute) for a text box element. */
   public final static String DEFAULT_TEXTBOX_HALIGN = Schema15.DEFAULT_TEXTBOX_ALIGN;
   /** (V>=15) Default vertical alignment ("valign" attribute) for a text box element. */
   public final static String DEFAULT_TEXTBOX_VALIGN = Schema15.DEFAULT_TEXTBOX_VALIGN;
   /** (V>=25) Default horizontal alignment ("align" attribute) for the title of a figure element. */
   public final static String DEFAULT_FIGURE_HALIGN = Schema25.DEFAULT_FIGURE_HALIGN;
   /** (V>=25) Default vertical alignment ("valign" attribute) for the title of a figure element. */
   public final static String DEFAULT_FIGURE_VALIGN = Schema25.DEFAULT_FIGURE_VALIGN;
   /** (V>=25) Default horizontal alignment ("align" attribute) for the title of any graph container element. */
   public final static String DEFAULT_GRAPH_HALIGN = Schema25.DEFAULT_GRAPH_HALIGN;


   /** 
    * (V>=16) Float-valued attribute specifies text line height for a "textbox", "axis", or "colorbar" element -- as a 
    * fraction of font size. Gives user control over inter-line spacing in a text block or a multi-line axis label.
    */
   public final static String A_LINEHT = Schema16.A_LINEHT;
   /** (V>=16) Default value for the "lineHt" attribute. */
   public final static String DEFAULT_LINEHT = Schema16.DEFAULT_LINEHT;

   /** 
    * (V>=15) "bkg" attribute specifies background fill color for a text box or image element (6-digit hex string). As
    * of V>=18, the attribute also applies to the root figure element.
    * (V>=20) The "bkg" attribute has been redefined to represent a solid-color fill, an axial gradient, or a radial
    * gradient. It was also added to the "shape" element.
    * (V>=21) Added to new "scatter3d" element -- a 3D scatter plot.
    */
   public final static String A_BKG = Schema15.A_BKG;
   /** (V>=15) Default background fill for a text box element. */
   public final static String DEFAULT_TEXTBOX_BKG = Schema15.DEFAULT_TEXTBOX_BKG;
   /** (V>=15) Default background fill for an image element. */
   public final static String DEFAULT_IMAGE_BKG = Schema15.DEFAULT_TEXTBOX_BKG;
   /** (V>=18) The default figure background fill (when "bkg" attribute implicit). */
   public final static String DEFAULT_FIGURE_BKG = Schema18.DEFAULT_FIGURE_BKG;
   /**
    * (V>=20) The default shape background fill (when "bkg" attribute implicit).
    * (V>=21) Same default used for a 3D scatter plot.
    */
   public final static String DEFAULT_SHAPE_BKG = Schema20.DEFAULT_SHAPE_BKG;
   
   /** 
    * (V>=15) Optional "crop" attribute specifies cropping rectangle "ulx uly w h" for the original source image
    * of an image element (array of 4 whitespace-separated integer tokens). If implicit, image is not cropped.
    */
   public final static String A_CROP = Schema15.A_CROP;
   
   /** (V>=0) The "p0" attribute defines location of a line segment's first endpoint within the parent viewport. */
   public final static String A_P0 = Schema0Constants.A_P0;
   /** (V>=0) The "p1" attribute defines location of a line segment's second endpoint within the parent viewport. */
   public final static String A_P1 = Schema0Constants.A_P1;
   
   /** (V>=7) The default symbol type (when "type" is not explicitly specified on a "symbol" element). */
   public final static String DEFAULT_SYMBOL = Schema7.DEFAULT_SYMBOL;
   /** 
    * (V>=3) The "size" attribute defines size of a shape or marker symbol; the uniform size for symbols rendered in
    * a legend; the width of gradient bar in the rendering of a z-axis. 
    */
   public final static String A_SIZE = Schema3.A_SIZE;
   /** (V>=7) Default size of a marker symbol (when "size" attribute not explicitly specified on a "symbol" element). */
   public final static String DEFAULT_SYMBOL_SIZE = Schema7.DEFAULT_SYMBOL_SIZE;
   /** (V>=7) Default uniform size for legend symbols (when "size" attribute not explicit on a "legend" element). */
   public final static String DEFAULT_LEGEND_SIZE = Schema7.DEFAULT_LEGEND_SIZE;
   /** 
    * (V>=9) Default width of colorbar's gradient bar (when "size" attribute not explicit on a "colorbar" element). 
    * (V>=23) The old "zaxis" element was renamed "colorbar", a more accurate moniker. 
    */
   public final static String DEFAULT_COLORBAR_SIZE = Schema9.DEFAULT_ZAXIS_SIZE;
   /** 
    * (V>=18) Default maximum symbol size for "scatter" element (when "size" attribute not explicit). 
    * (V>=21) Same default used for 3D scatter plots as well.
    */
   public final static String DEFAULT_SCATTER_SIZE = Schema18.DEFAULT_SCATTER_SIZE;
   /** (V>=26) Default plot size for the violin plot component of a box plot. */
   public final static String DEFAULT_VIOLIN_SIZE = Schema26.DEFAULT_VIOLIN_SIZE;
   /** (V>=20) Default value for the "width" and "height" attributes of a "shape" element (replaced "size" attr). */
   public final static String DEFAULT_SHAPE_DIM = Schema20.DEFAULT_SHAPE_DIM;
   
   /** (V>=7) The "primary" attribute indicates the graph axis to which a calibration bar is attached. */
   public final static String A_PRIMARY = Schema7.A_PRIMARY;
   /** (V>=7) Default value for calibration bar's "primary" flag attribute (when not explicitly specified). */
   public final static String DEFAULT_PRIMARY = Schema7.DEFAULT_PRIMARY;
   /** (V>=0) The "auto" attribute enables/disable auto-generation of a calibration bar's text label. */
   public final static String A_AUTO = Schema0Constants.A_AUTO;
   /** (V>=7) Default value for calibration bar's "auto" flag attribute (when not explicitly specified). */
   public final static String DEFAULT_AUTO = Schema7.DEFAULT_AUTO;
   /** (V>=26) Default value for bar plot element's  "auto" flag attribute (when not explicitly specified). */
   public final static String DEFAULT_BAR_AUTO = Schema26.DEFAULT_BAR_AUTO;
   /** (V>=0) The "cap" attribute specifies the endcap adornment for a calibration or error bar. */
   public final static String A_CAP = Schema0Constants.A_CAP;
   /** (V>=7) Default endcap adornment for a calibration bar (when "cap" attribute not explicitly specified). */
   public final static String DEFAULT_CALIB_CAP = Schema7.DEFAULT_CALIB_CAP;
   /** (V>=7) Default endcap adornment for an error bar (when "cap" attribute not explicitly specified). */
   public final static String DEFAULT_EBAR_CAP = Schema7.DEFAULT_EBAR_CAP;
   /** (V>=0) The "capSize" attribute specifies the endcap size for a calibration or error bar. */
   public final static String A_CAPSIZE = Schema0Constants.A_CAPSIZE;
   /** (V>=7) Default endcap size (when "capSize" attribute not explicitly specified). */
   public final static String DEFAULT_CAPSIZE = Schema7.DEFAULT_CAPSIZE;
   /** 
    * (V>=0) The "len" attribute specifies length of a tick mark, calibration bar, or a legend entry's line segment. 
    * (V>=26) Specifies window length for sliding average trendline on a "trace" node.
    */
   public final static String A_LEN = Schema0Constants.A_LEN;
   /** (V>=7) Default tick mark length (when "len" attribute not explicit on a "ticks" element). */
   public final static String DEFAULT_TICK_LEN = Schema7.DEFAULT_TICKLEN;
   /** (V>=7) Default length of legend entry's line segment (when "len" attribute not explicit on a "legend" node). */
   public final static String DEFAULT_LEGEND_LEN = Schema7.DEFAULT_LEGEND_LEN;
   /** (V>=26) Default window length for sliding average trendline computation on a "trace" node. */
   public final static String DEFAULT_TRACE_LEN = Schema26.DEFAULT_TRACE_LEN;
   
   /** 
    * (V>=7) The "legend" flag controls visibility of a legend entry for a "trace" or "function" element. (V>=14) The
    * flag applies to a "raster" element as well.
    */
   public final static String A_LEGEND = Schema7.A_LEGEND;
   /** (V>=7) The default value for the "legend" flag (when not explicitly specified). */
   public final static String DEFAULT_LEGEND = Schema7.DEFAULT_LEGEND;
   /** 
    * (V>=0) The "x0" and "x1" attributes specify the x-coordinate range [x0,x1] over which a function is defined.
    * (V>=24) "x0" is the plot offset for the "box" element.
    */
   public final static String A_X0 = Schema0Constants.A_X0;
   /** (V>=7) The default value for the "x0" attribute for a "function" or "box" (V>=24) element. */
   public final static String DEFAULT_X0 = Schema7.DEFAULT_X0;
   /** (V>=0) The "x0" and "x1" attributes specify the x-coordinate range [x0,x1] over which a function is defined. */
   public final static String A_X1 = Schema0Constants.A_X1;
   /** (V>=7) The default value for the "x1" attribute of a "function" element (when not explicitly specified). */
   public final static String DEFAULT_X1 = Schema7.DEFAULT_X1;
   /** 
    * (V>=0) The "dx" attribute specifies the sample interval for a "function" element. 
    * (V>=24) It specifies the plot separation interval for a "box" element.
    */
   public final static String A_DX = Schema0Constants.A_DX;
   /** (V>=7) The default value for the "dx" attribute of a "function" or "box" (V>=24) element. */
   public final static String DEFAULT_DX = Schema7.DEFAULT_DX;

   /** (V>=2) The plot skip interval for a "trace" element. */
   public final static String A_SKIP = Schema2.A_SKIP;
   /** (V>=7) The default plot skip interval for a "trace" element (when "skip" attribute not explicitly specified). */
   public final static String DEFAULT_SKIP = Schema7.DEFAULT_SKIP;
   /** 
    * (V>=7) The data display mode for a "trace" or "raster" element. Serves the same role for "bar" (V>=17), 
    * "scatter" (V>=18), "scatter3d" (V>=21), "contour" (V>=22), and "box" (V>=24).
    */
   public final static String A_MODE = Schema7.A_MODE;
   /** (V>=7) The default display mode for a "trace" element (when "mode" attribute not explicitly specified). */
   public final static String DEFAULT_MODE = Schema7.DEFAULT_MODE;
   /** (V>=8) The default display mode for a "raster" element (when "mode" attribute not explicitly specified). */
   public final static String DEFAULT_RASTER_MODE = Schema8.DEFAULT_RASTER_MODE;
   /** (V>=17) The default display mode for a "bar" element (when "mode" attribute not explicitly specified). */
   public final static String DEFAULT_BAR_MODE= Schema17.DEFAULT_BAR_MODE;
   /** 
    * (V>=18) The default display mode for a "scatter" element (when "mode" attribute not explicitly specified). 
    * (V>=21) Also applies to "scatter3d" element.
    */
   public final static String DEFAULT_SCATTER_MODE= Schema18.DEFAULT_SCATTER_MODE;
   /** (V>=22) The default display mode for a "contour" element (when "mode" attribute not explicitly specified). */
   public final static String DEFAULT_CONTOUR_MODE= Schema22.DEFAULT_CONTOUR_MODE;
   /** (V>=25) The default display mode for a "box" element (when "mode" attribute not explicitly specified). */
   public final static String DEFAULT_BOX_MODE= Schema25.DEFAULT_BOX_MODE;
   /** (V>=25) The default label display mode for a "pie" element (when "mode" attribute not explicitly specified). */
   public final static String DEFAULT_PIE_MODE= Schema25.DEFAULT_PIE_MODE;
   /** (V>=26) The default label display mode for a "area" element (when "mode" attribute not explicitly specified). */
   public final static String DEFAULT_AREA_MODE= Schema26.DEFAULT_AREA_MODE;
   
   /** 
    * (V>=7) Histogram bar width for a "trace" element in the histogram display mode.
    * (V>=17) Relative bar width (integer percentage) for the bars rendered by a "bar" element.
    * (V>=23) Relative bar size of XY cross-section of 3D bars in a "scatter3d" element configured in one of its
    * bar plot display modes. The bar size is expressed as an integer percentage of the 3D X-axis extent.
    * (V>=25) Box width as a measure in in/cm/mm/pt/%.
    */
   public final static String A_BARWIDTH = Schema0Constants.A_BARWIDTH;
   /** (V>=7) Default histogram bar width for a "trace" element (when "barWidth" attribute not explicitly specified). */
   public final static String DEFAULT_BARWIDTH = Schema7.DEFAULT_BARWIDTH;
   /** (V>=17) Default value for "barWidth" attribute of the "bar" element (when attribute not explicitly specified). */
   public final static String DEFAULT_BAR_BARWIDTH = Schema17.DEFAULT_BAR_BARWIDTH;
   /** (V>=25) Default value for "barWidth" attribute of the "box" element (when attribute not explicitly specified). */
   public final static String DEFAULT_BOX_BARWIDTH = Schema25.DEFAULT_BOX_BARWIDTH;
   /** (V>=17) Minimum relative bar width (as an integer percentage) for the "bar" element. */
   public final static int MIN_BAR_BARW = Schema17.MIN_BAR_BARW;
   /** (V>=17) Maximum relative bar width (as an integer percentage) for the "bar" element. */
   public final static int MAX_BAR_BARW = Schema17.MAX_BAR_BARW;
   /** (V>=23) Default bar size (int %-age of 3D X-axis extent) for a "scatter3d" element. */
   public final static String DEFAULT_SCAT3D_BARWIDTH = Schema23.DEFAULT_SCAT3D_BARWIDTH;
   /** (V>=23) Minimum bar size (int %-age of 3D X-axis extent) of a "scatter3d" element. */
   public final static int MIN_SCAT3D_BARW = Schema23.MIN_SCAT3D_BARW;
   /** (V>=23) Maximum bar size (int %-age of 3D X-axis extent) for a "scatter3d" element. */
   public final static int MAX_SCAT3D_BARW = Schema23.MAX_SCAT3D_BARW;


   /** 
    * (V>=0) Histogram baseline for a "trace" or "raster" element in the histogram display mode. 
    * (V>=17) Baseline for a bar plot.
    * (V>=19) Baseline for an area chart.
    * (V>=21) Stem base plane Z value for a 3D scatter plot.
    */
   public final static String A_BASELINE = Schema0Constants.A_BASELINE;
   /** (V>=7) Default value for the {@link #A_BASELINE} attribute. */
   public final static String DEFAULT_BASELINE = Schema7.DEFAULT_BASELINE;
   /** (V>=6) Arbitrary x-coordinate offset applied to data rendered by a "trace" or "raster" element. */
   public final static String A_XOFF = Schema6.A_XOFF;
   /** (V>=6) Arbitrary y-coordinate offset applied to data rendered by a "trace" element. */
   public final static String A_YOFF = Schema6.A_YOFF;
   /** (V>=7) Default value for x- and y-coordinate offsets (when "xoff", "yoff" not explicitly specified). */
   public final static String DEFAULT_XYOFF = Schema7.DEFAULT_XYOFF;
   /** 
    * (0<=V<16) Flag enables/disables color-fill of histogram bars or error bands when rendering a trace or raster. 
    * [Obsolete as of V=16; replaced by a transparent fill color.]
    */
   public final static String A_FILLED = Schema0Constants.A_FILLED;
   /** (7<=V<16) Default value for the "filled" flag (when not explicitly specified). */
   public final static String DEFAULT_FILLED = Schema7.DEFAULT_FILLED;
   /** (V>=6) The "src" attribute identifies the "set" element containing the dataset to be rendered. */
   public final static String A_SRC = Schema6.A_SRC;
   /** 
    * (V>=0) The space separating an axis from the graph's data viewport, between consecutive entries in a graph 
    * legend, or between raster lines in a "raster" element. 
    */
   public final static String A_SPACER = Schema0Constants.A_SPACER;
   /** (V>=7) Default axis spacer size (when "spacer" attribute not explicitly specified on "axis" element). */
   public final static String DEFAULT_AXIS_SPACER = Schema7.DEFAULT_AXIS_SPACER;
   /** (V>=7) Default legend entry offset (when "spacer" attribute not explicitly specified on "legend" element). */
   public final static String DEFAULT_LEGEND_SPACER = Schema7.DEFAULT_LEGEND_SPACER;
   /** (V>=8) Default size of space between raster lines (when "spacer" attribute not explicitly specified). */
   public final static String DEFAULT_RASTER_SPACER = Schema8.DEFAULT_RASTER_SPACER;
   /** (V>=8) Number of bins in the histogram display of raster data. */
   public final static String A_NBINS = Schema8.A_NBINS;
   /** (V>=8) Default number of bins in histogram display of raster data (when "nbins" attribute not explicit). */
   public final static String DEFAULT_NBINS = Schema8.DEFAULT_NBINS;
   /** 
    * (V>=10) Flag indicates whether histogram display of raster data shows average or total counts per bin (for the
    * "raster" element; (V>=11) enables/disables rendering of the average trace in the "multitrace" display mode of the
    * "trace" element.
    */
   public final static String A_AVG = Schema10.A_AVG;
   /** (V>=10) Default value of "avg" flag (when "avg" attribute not explicit) for "raster" element. */
   public final static String DEFAULT_RASTER_AVG = Schema10.DEFAULT_RASTER_AVG;
   /** (V>=11) Default value of "avg" flag (when "avg" attribute not explicit) for "trace" element. */
   public final static String DEFAULT_TRACE_AVG = Schema11.DEFAULT_TRACE_AVG;

   /** (V>=0) Flag determines whether or not an axis, legend, gridline or error bar element is rendered. */
   public final static String A_HIDE = Schema0Constants.A_HIDE;
   /** (V>=7) Default value of "hide" flag for an "axis" element (when "hide" attribute not explicitly specified). */
   public final static String DEFAULT_AXIS_HIDE = Schema7.DEFAULT_AXIS_HIDE;
   /** (V>=7) Default value of "hide" flag for a "legend" element (when "hide" attribute not explicitly specified). */
   public final static String DEFAULT_LEGEND_HIDE = Schema7.DEFAULT_LEGEND_HIDE;
   /** (V>=7) Default value of "hide" flag for a "gridline" element (when "hide" attribute not explicitly specified). */
   public final static String DEFAULT_GRIDLINE_HIDE = Schema7.DEFAULT_GRIDLINE_HIDE;
   /** (V>=7) Default value of "hide" flag for a "ebar" element (when "hide" attribute not explicitly specified). */
   public final static String DEFAULT_EBAR_HIDE = Schema7.DEFAULT_EBAR_HIDE;
   /** 
    * (V>=9) Default value of "hide" flag for a "colorbar" element (when "hide" attribute not explicitly specified). 
    * (V>=23) The old "zaxis" element was renamed "colorbar", a more accurate moniker. 
    */
   public final static String DEFAULT_COLORBAR_HIDE = Schema9.DEFAULT_ZAXIS_HIDE;
   /** (V>=25) Default value of "hide" flag for a "figure" element (when "hide" attribute not explicitly specified). */
   public final static String DEFAULT_FIGURE_HIDE = Schema25.DEFAULT_FIGURE_HIDE;
   /** (V>=25) Default value of "hide" flag for any graph container element (controls title visibility). */
   public final static String DEFAULT_GRAPH_HIDE = Schema25.DEFAULT_FIGURE_HIDE;

   /** (V>=0) The units token for a graph axis. */
   public final static String A_UNITS = Schema0Constants.A_UNITS;
   /** (V>=7) Default units token for a graph axis (when "units" attribute not explicitly specified). */
   public final static String DEFAULT_UNITS = Schema7.DEFAULT_UNITS;
   /** 
    * (V>=0) Distance between axis line and its label. 
    * (V>=14) Offset from right end of trace line and start of accompanying text label in a graph legend entry.
    */
   public final static String A_LABELOFFSET = Schema0Constants.A_LABELOFFSET;
   /** (V>=7) Default axis label offset (when "labelOffset" attribute not explicitly specified). */
   public final static String DEFAULT_LABELOFFSET = Schema7.DEFAULT_LABELOFFSET;
   /** (V>=14) Default legend label offset (gap between right edge of trace line and accompanying text label). */
   public final static String DEFAULT_LEGEND_LABELOFFSET = Schema14.DEFAULT_LEGEND_LABELOFFSET;
   
   /** 
    * (V>=0) Start of coordinate range spanned by a graph axis or an axis tick mark set. Also applicable to a 3D graph
    * axis (V>=21) and the polar axis components of a 2D polar plot (V>=22).
    * (V>=19) The inner radius of a pie chart.
    * (V>=22) Optional attribute specifying start of range over which raster data is binned to create a histogram.
    */
   public final static String A_START = Schema0Constants.A_START;
   /** 
    * (V>=0) End of coordinate range spanned by a graph axis or an axis tick mark set. Also applicable to a 3D graph
    * axis (V>=21) and the polar axis components of a 2D polar plot (V>=22).
    * (V>=19) The outer radius of a pie chart.
    * (V>=22) Optional attribute specifying end of range over which raster data is binned to create a histogram.
    * 
    */
   public final static String A_END = Schema0Constants.A_END;

   /** (V>=22) Default value for the optional "start" and "end" attributes of the "raster" element. */
   public final static String DEFAULT_RASTER_STARTEND = Schema22.DEFAULT_RASTER_STARTEND;
   
   /** 
    * (V>=23) Axis scale factor (base-10 exponent) for scaling child tick mark labels to improve appearance. Applies
    * to "axis" and "colorbar" elements (but not to polar axes).
    */
   public final static String A_SCALE = Schema23.A_SCALE;
   /** (V>=23) Default axis scale factor for "axis" and "colorbar" elements */
   public final static String DEFAULT_SCALE = Schema23.DEFAULT_SCALE;
   
   /** (V>=11) Flag determines whether axis's logarithmic base is 2 (true) or 10 (false). */
   public final static String A_LOG2 = Schema11.A_LOG2;
   /** (V>=11) Default value of "log2" flag for an "axis" element. */
   public final static String DEFAULT_LOG2 = Schema11.DEFAULT_LOG2;
   /** (V>=21) Flag determines whether 3D axis (and its corresponding dimension in the graph) is logarithmic. */
   public final static String A_LOG = Schema21.A_LOG;
   /** (V>=21) Default value of "log" flag for an "axis" element (3D graph context only). */
   public final static String DEFAULT_LOG = Schema21.DEFAULT_LOG;
   
   /** 
    * (V>=0) Gap between tick marks and their labels, or between z-axis's gradient bar and the adjacent graph edge.
    * (V>=19) The radial offset of an exploded slice in a pie chart, as an integer percentage of the outer radius.
    * (V>=22) Displacement of labels from polar grid lines/arcs for a 2D polar plot axis component. 
    */
   public final static String A_GAP = Schema0Constants.A_GAP;
   /** (V>=7) Default tick mark-label gap (when "gap" attribute not explicit on a "ticks" element). */
   public final static String DEFAULT_TICK_GAP = Schema7.DEFAULT_TICKGAP;
   /** 
    * (V>=9) Default gap between colorbar's gradient bar and graph edge (when "gap" attribute not explicitly specified). 
    * (V>=23) The old "zaxis" element was renamed "colorbar", a more accurate moniker. 
    */
   public final static String DEFAULT_COLORBAR_GAP = Schema9.DEFAULT_ZAXIS_GAP;
   /** (V>=15) Default gap (margin) for a text box element. */
   public final static String DEFAULT_TEXTBOX_GAP = Schema15.DEFAULT_TEXTBOX_GAP;
   /** (V>=15) Default gap (margin) for an image element. */
   public final static String DEFAULT_IMAGE_GAP = Schema15.DEFAULT_TEXTBOX_GAP;
   /** (V>=19) Minimum radial offset of displaced slice in pie chart (as an integer percentage of the outer radius). */
   public final static int MIN_PIE_GAP = Schema19.MIN_PIE_GAP;
   /** (V>=19) Maximum radial offset of displaced slice in pie chart (as an integer percentage of the outer radius). */
   public final static int MAX_PIE_GAP = Schema19.MAX_PIE_GAP;
   /** 
    * (V>=19) Default value for the pie chart's "gap" attribute (radial offset of a displaced pie slice, as an integer
    * percentage of the outer radius). 
    */
   public final static String DEFAULT_PIE_GAP = Schema19.DEFAULT_PIE_GAP;
   /** Default value for the {@link #A_GAP} attribute of a 2D polar plot axis = "0.2in". */
   public final static String DEFAULT_PAXIS_GAP = Schema22.DEFAULT_PAXIS_GAP;
   /** 
    * (V>=25) Default value for the {@link #A_GAP} attribute of any graph container node (specifying vertical 
    * separation of semi-automated title from top/bottom edge of graph's data box).
    */
   public final static String DEFAULT_GRAPH_GAP = Schema25.DEFAULT_GRAPH_GAP;
   
   /** (V>=19) The "displace" bit flag vector for a pie chart element; bitN=1 indicates that slice N is displaced. */
   public final static String A_DISPLACE = Schema19.A_DISPLACE;
   /** (V>=19) Default value for the pie chart's "displace" attribute. */
   public final static String DEFAULT_PIE_DISPLACE = Schema19.DEFAULT_PIE_DISPLACE;
   
   /** 
    * (V>=9) The color map, or lookup table, assigned to a graph's Z-axis. 
    * (V>=21) The color map for a 3D graph.
    */
   public final static String A_CMAP = Schema9.A_CMAP;
   /**
    * (V>=9) Default colormap for a graph colorbar (when "cmap" attribute not explicitly specified). 
    * (21<=V<=22) Same default also applies to "graph3d" element.
    * (V>=23) The old "zaxis" element was renamed "colorbar", a more accurate moniker. Also, a colorbar was added to
    * "graph3d"; so the latter element no longer has a "cmap" attribute.
    */
   public final static String DEFAULT_COLORBAR_CMAP = Schema9.DEFAULT_ZAXIS_CMAP;
   /** (V>=21) Default value for {@link #A_CMAP} attribute of a 3D surface plot (boolean value). */
   public final static String DEFAULT_SURFACE_CMAP = Schema21.DEFAULT_SURFACE_CMAP;
   /** (V>=9) The color mapping mode for a graph's Z-axis. */
   public final static String A_CMODE = Schema9.A_CMODE;
   /** 
    * (V>=9) Default color mapping mode for a colorbar (when "cmode" attribute not explicitly specified). 
    * (V>=23) The old "zaxis" element was renamed "colorbar", a more accurate moniker. 
    */
   public final static String DEFAULT_COLORBAR_CMODE = Schema9.DEFAULT_ZAXIS_CMODE;
   /** (V>=9) The graph data window edge along which a Z-axis is rendered. */
   public final static String A_EDGE = Schema9.A_EDGE;
   /** (V>=9) Default choice for the graph edge along which Z-axis is rendered (when "edge" attribute not explicit). */
   public final static String DEFAULT_EDGE = Schema9.DEFAULT_EDGE;
   /** (V>=10) The "NaN color" for a graph's Z-axis; any ill-defined (NaN or infinite) datum maps to this color. */
   public final static String A_CMAPNAN = Schema10.A_CMAPNAN;
   /** 
    * (V>=10) Default value for "NaN color" of a colorbar (when "cmapnan" attribute not explicitly specified). 
    * (V>=23) The old "zaxis" element was renamed "colorbar", a more accurate moniker. 
    */
   public final static String DEFAULT_COLORBAR_CMAPNAN = Schema10.DEFAULT_ZAXIS_CMAPNAN;
   /** 
    * (10<=V<22) Flag enables/disables image smoothing for a heatmap element. 
    * (V>=22) Same meaning for the contour element, which replaced the now-deprecated heatmap element.
    */
   public final static String A_SMOOTH = Schema10.A_SMOOTH;
   /** 
    * (10<=V<22) Default value for image smoothing flag of a heatmap (when "smooth" attr not explicitly specified). 
    * (V>=22) Same meaning for the contour element, which replaced the now-deprecated heatmap element.
    */
   public final static String DEFAULT_HEATMAP_SMOOTH = Schema10.DEFAULT_HEATMAP_SMOOTH;

   /** (V>=22) The contour level list for a contour element. */
   public final static String A_LEVELS = Schema22.A_LEVELS;
   /** (V>=22) Default value for the contour element's "levels" attribute. */
   public final static String DEFAULT_LEVELS = Schema22.DEFAULT_LEVELS;
   /** (V>=22) The maximum number of contour levels that can be listed in the "levels" attribute for a contour node. */
   public final static int MAX_CONTOUR_LEVELS = Schema22.MAX_CONTOUR_LEVELS;
   
   /** (V>=0) The "perLogIntv" attribute of a "ticks" element defines tick locations when axis is logarithmic. */
   public final static String A_PERLOGINTV = Schema0Constants.A_PERLOGINTV;
   /** (V>=7) Default value for "perLogIntv" attribute (when not explicitly specified). */
   public final static String DEFAULT_PERLOGINTV = Schema7.DEFAULT_PERLOGINTV;
   /** (V>=0) The direction of tick marks rendered for an axis tick set. */
   public final static String A_DIR = Schema0Constants.A_DIR;
   /** (V>=7) Default direction for axis tick marks (when "dir" attribute not explicit on a "ticks" element). */
   public final static String DEFAULT_TICK_DIR = Schema7.DEFAULT_TICKDIR;
   /** (V>=0) Numeric format for automated tick mark labels on a "ticks" element, or dataset format for a "set". */
   public final static String A_FMT = Schema0Constants.A_FMT;
   /** (V>=7) Default tick mark label numeric format (when "fmt" attribute not explicit on a "ticks" element). */
   public final static String DEFAULT_TICK_FMT = Schema7.DEFAULT_TICKFMT;
   /** (V>=0) The tick mark interval. */
   public final static String A_INTV = Schema0Constants.A_INTV;
   
   /** (V>=0) Flag selects whether or not symbol shown at midpoint or endpoints of a legend entry's line segment. */
   public final static String A_MID = Schema0Constants.A_MID;
   /** (V>=7) Default value for the "mid" flag of a "legend" element (when not explicitly specified). */
   public final static String DEFAULT_MID = Schema7.DEFAULT_MID;

   /** 
    * (V>=22) Flag selects 3D scatter plot is presented as a stem plot (data points connected to a Z-base plane by
    * stem lines) or a line plot (points are connected to each other by a poly-line).
    */
   public final static String A_STEMMED = Schema22.A_STEMMED;
   /** (V>=22) Default value for the {@link #A_STEMMED} attribute of a {@link #EL_SCATTER3D} element. */
   public final static String DEFAULT_STEMMED = Schema22.DEFAULT_STEMMED;
   /** 
    * (V>=22) For 3D scatter plot: list of 0-3 integers "xy xz yz" representing the size of the dots in the scatter 
    * plot's XY, XZ and YZ backplane projections. Dot size must lie in [0..10]. Any missing dot size defaults to 0.
    */
   public final static String A_DOTSIZE = Schema22.A_DOTSIZE;
   /**  Default value for the {@link #A_DOTSIZE} attribute of 3D scatter plot. */
   public final static String DEFAULT_DOTSIZE = Schema22.DEFAULT_DOTSIZE;
   /**
    * (V>=22) For 3D scatter plot: list of 0-3 string tokens "xy xz yz" representing the color of the dots in the 
    * scatter plot's XY, XZ and YZ backplane projections. Each string may be a 6-digit hex string RRGGBB for an opaque
    * color, an 8-digit hex string AARRGGBB for a translucent color, or "none" = transparent black. Any missing dot
    * color defaults to opaque black.
    */
   public final static String A_DOTCOLOR = Schema22.A_DOTCOLOR;
   /** Default value for the {@link #A_DOTCOLOR} attribute of 3D scatter plot. */
   public final static String DEFAULT_DOTCOLOR = Schema22.DEFAULT_DOTCOLOR;
   
   /** Boolean attribute for 2D polar plot, indicating whether or not polar grid is rendered on top of any data. */
   public final static String A_GRIDONTOP = Schema22.A_GRIDONTOP;
   /** Default value for the {@link #A_GRIDONTOP} attribute of a 2D polar plot. */
   public final static String DEFAULT_GRIDONTOP = Schema22.DEFAULT_GRIDONTOP;

   /** 
    * (V>=22) Boolean attribute for a 2D polar plot axis. If true, the direction of increasing values is reversed for 
    * the axis.
    * (V>=25) Boolean attribute for a color bar selects the reverse-direction version of the color map.
    */
   public final static String A_REVERSE = Schema22.A_REVERSE;
   /** Default value for the {@link #A_REVERSE} attribute of a 2D polar plot axis or a color bar. */
   public final static String DEFAULT_REVERSE = Schema22.DEFAULT_REVERSE;
   /** (V>=22) Integer attribute specifying the reference angle for a 2D polar plot axis. */
   public final static String A_REFANGLE = Schema22.A_REFANGLE;
   /** Default value for the {@link #A_REFANGLE} attribute of a 2D polar plot axis. */
   public final static String DEFAULT_REFANGLE = Schema22.DEFAULT_REFANGLE;
   /** (V>=22) Required attribute specifying the grid divisions for a 2D polar plot axis. */
   public final static String A_PDIVS = Schema22.A_PDIVS;
   
   /** 
    * (V>=6) The dataset identifier, uniquely identifying the "set" element within the figure document. 
    * (V>=18) A graphic object ID, for label, textbox, and graph elements. <b>If this is a non-empty string</b>, it
    * should uniquely identify the graphic object within the figure document. Not compared against data set IDs!</b>.
    */
   public final static String A_ID = Schema6.A_ID;
   /** Default value for "id" attribute for a label, textbox, or graph element. */
   public final static String DEFAULT_OBJ_ID = Schema18.DEFAULT_OBJ_ID;

   /** 
    * (V>=8) If "set" element is a copy of another dataset in the figure document, the text content of the element will
    * be an empty string and this attribute specifies the ID of the other "set" node containing the actual data. 
    */
   public final static String A_USING = Schema8.A_USING;
   /** 
    * (V>=8) Special attribute defined on a "set" element only during migration from schema version 7, when the format
    * of the "set" element's text content changed. The dataset identifier, uniquely identifying the "set" element within
    * the figure document. The text content is left unchanged during migration; it is transformed only after schema
    * migration is completed.
    */
   public final static String A_V7 = Schema8.A_V7;
}
