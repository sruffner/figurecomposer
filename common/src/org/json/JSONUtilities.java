package org.json;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Collection of static methods for reading and writing JSON-formatted content.
 * <p>This class was added to the freely provided org.json.* package and relies on classes from that package.</p>
 * 
 * @author sruffner
 */
public class JSONUtilities
{
   /** Simple test to see if I can parse a file as a valid JSON-encoded object. */
   public static void main(String[] args)
   {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      
      // get pathname to JSON file to test
      System.out.print("Specify full pathname for JSON text file\n> ");
      String fname = null;
      try
      {
         fname = in.readLine();
      }
      catch(IOException ioe)
      {
         System.out.println("Unexpected IO error while reading file path:\n   " + ioe.getMessage() + "\nQUITTING!");
         fname = null;
      }
      
      if(fname == null)
         System.exit(0);
      
      // read in file and report error if operation fails
      File f = new File(fname);
      String emsg = null;
      try { readJSONObject(f); }
      catch(JSONException jse) { emsg = jse.getMessage(); }
      catch(IOException ioe) { emsg = ioe.getMessage(); }
      
      if(emsg != null) System.out.println("FAIL: " + emsg);
      else System.out.println("SUCCESS!");
   }
   
   /**
    * Read and parse a JavaScript Object Notation (JSON)-formatted file containing the definition of a <i>single</i>
    * JSON object. A buffered reader is used to stream file contents through a JSON tokener, so the method can be used 
    * with large JSON files.
    * 
    * @param f Abstract pathname of JSON file
    * @return The JSON object parsed from the file.
    * @throws IOException if an IO error occurs while reading the file (including file not found).
    * @throws JSONException if a JSON syntax error occurs while parsing file content.
    */
   public static JSONObject readJSONObject(File f) throws IOException, JSONException
   {
      if(f == null) throw new IllegalArgumentException("Null file argument!");
      
      JSONObject jsonObj = null;
      BufferedReader rdr = null;
      try
      {
         rdr = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.US_ASCII));
         JSONTokener tokener = new JSONTokener(rdr);
         jsonObj = new JSONObject(tokener);
      }
      finally
      {
         try { if( rdr != null ) rdr.close(); } catch(IOException ioe) {}
      }
      return(jsonObj);
   }
   
   /**
    * Write a single JavaScript Object Notation (JSON) object to file. A buffered writer is used to handle a large,
    * complex object.
    * 
    * @param f Abstract pathname of target file. If the file already exists, its previous content is lost.
    * @param jsonObj The JSON object to be written.
    * @param pretty True to add linefeeds and whitespace indentation so file is easier to read in a text editor. If 
    * false, no linefeeds or indentation added (for minimum file size)
    * @throws IOException if an IO error occurs while writing file.
    * @throws JSONException if the JSON object contains an invalid number
    */
   public static void writeJSONObject(File f, JSONObject jsonObj, boolean pretty) throws IOException, JSONException
   {
      if(f == null || jsonObj == null) throw new IllegalArgumentException("Null argument!");
      
      BufferedWriter writer = null;
      try
      {
         writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.US_ASCII));
         if(pretty) jsonObj.write(writer, 2, 0);
         else jsonObj.write(writer);
      }
      finally
      {
         try{ if(writer != null) writer.close(); } catch(IOException ioe) {}
      }
   }
   
   /**
    * Read and parse a JavaScript Object Notation (JSON)-formatted file containing the definition of a <i>single</i>
    * JSON array. A buffered reader is used to stream file contents through a JSON tokener, so the method can be used 
    * with large JSON files.
    * 
    * @param f Abstract pathname of JSON file
    * @return The JSON array parsed from the file.
    * @throws IOException if an IO error occurs while reading the file (including file not found).
    * @throws JSONException if a JSON syntax error occurs while parsing file content.
    */
   public static JSONArray readJSONArray(File f) throws IOException, JSONException
   {
      if(f == null) throw new IllegalArgumentException("Null file argument!");
      
      JSONArray jsonAr = null;
      BufferedReader rdr = null;
      try
      {
         rdr = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.US_ASCII));
         JSONTokener tokener = new JSONTokener(rdr);
         jsonAr = new JSONArray(tokener);
      }
      finally
      {
         try { if( rdr != null ) rdr.close(); } catch(IOException ioe) {}
      }
      return(jsonAr);
   }
   
   /**
    * Write a single JavaScript Object Notation (JSON) array to file. A buffered writer is used to handle a large,
    * complex array.
    * 
    * @param f Abstract pathname of target file. If the file already exists, its previous content is lost.
    * @param jsonAr The JSON array to be written.
    * @param pretty True to add linefeeds and whitespace indentation so file is easier to read in a text editor. If 
    * false, no linefeeds or indentation added (for minimum file size)
    * @throws IOException if an IO error occurs while writing file.
    * @throws JSONException if the JSON object contains an invalid number
    */
   public static void writeJSONArray(File f, JSONArray jsonAr, boolean pretty) throws IOException, JSONException
   {
      if(f == null || jsonAr == null) throw new IllegalArgumentException("Null argument!");
      
      BufferedWriter writer = null;
      try
      {
         writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.US_ASCII));
         if(pretty) jsonAr.write(writer, 2, 0);
         else jsonAr.write(writer);
      }
      finally
      {
         try{ if(writer != null) writer.close(); } catch(IOException ioe) {}
      }
   }
   
}
