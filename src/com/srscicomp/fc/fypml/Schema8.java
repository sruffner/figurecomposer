package com.srscicomp.fc.fypml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema8</code> is the encapsulation of the <em>DataNav</em> figure model XML schema version 8. It extends 
 * <code>Schema7</code> and includes support for migrating schema version 7 documents to version 8. This was the first 
 * schema introduced since the figure-creation program <em>Phyplot</em> was integrated as one component of the larger 
 * <em>DataNav</em> application. Early development of <em>DataNav</em> brought with it significant changes in the 
 * figure model XML schema, particularly with respect to datasets.
 * 
 * <p>Two new element classes were added, "raster" and "heatmap". These are dataset presentation elements that are 
 * specialized for the rendering of two new dataset types. Introduced new attribute names in support of these elements: 
 * "nbins", "cmap".</p>
 * 
 * <p>The storage and structure of datasets changed substantially in <em>DataNav</em>.</p>
 * <ul>
 *    <li>Two new dataset formats were added: "raster1d" is a collection of 0 or more x-coordinate vectors, or rasters 
 *    (typically used to store spike trains collected over multiple presentations of the same stimulus); and "xyzimg" is 
 *    a 3D dataset in which one variable Z is a function of two independent variables X and Y. Z(X,Y) is "sampled" over
 *    the rectangle [x0..x1, y0..y1] in the (X,Y)-plane. It can be thought of as an image, and it is in fact rendered 
 *    as such via a color LUT in the "heatmap" element.</li> 
 *    <li>The "series" and "mseries" formats now include -- in addition to the sample interval "dx" -- a second 
 *    parameter "x0" specifying the x-coordinate for the first sample in the series.</li>
 *    <li>The figure model entity encapsulating datasets was entirely rewritten for <em>DataNav</em>. The "raw data" 
 *    is generally interpreted as a matrix with a fixed number of rows (data "breadth") and columns (data "length")
 *    packed row-wise into a single-precision floating-point array. In <em>Phyplot</em>, the raw data was a list of 
 *    double-precision floating-pt tuples which could (but usually didn't) have different lengths.</li>
 *    <li>The figure model no longer manages a "data store" as in schema versions 6 and 7. "Orphaned" datasets are no
 *    longer possible. Now, a data presentation element ("trace", "raster", or "heatmap") stores a reference to the 
 *    dataset it uses, and such references can be shared because the dataset object is immutable.</li>
 * </ul>
 * 
 * <p>The definition of the "set" element changed substantially to support this new way of representing datasets. By 
 * far the biggest change was to store the raw data as base64-encoded binary. This decision sacrifices the flexibility 
 * to read or write a valid <em>DataNav</em> XML figure file using a simple text editor or a homespun script. But it 
 * provides a form that is much more precise (full single-precision float VS a numeric string with up to 6 decimal 
 * digits), compact, and opaque. The last two are important considerations given that datasets and figure models need to 
 * be transported over the Internet from a <em>DataNav</em> portal server to client machines that are viewing the 
 * portal's content.</p>
 * 
 * <p>In the new schema, the "set" element supports both the old "comma-separated tuples" and the new base64-encoded 
 * text content. This simplifies migration and puts off the task of parsing the old-style tuples until the dataset 
 * entity is actually created (more efficient and easier than trying to parse the old-style tuples and converting them 
 * directly to the base64-encoded binary form). During migration a new attribute, "v7", is explicitly set to "true" on 
 * each existing "set" element, which is otherwise left unchanged. The presence of this attribute informs 
 * <code>FGModelConverter</code> to expect the schema 7 version of the "set" element.</p>
 * 
 * <p>Finally, <em>DataNav</em> places additional restrictions on the ID of a dataset. This schema enforces those 
 * restrictions and handles the task of altering the "id" attributes of schema version 7 "set" elements as needed to 
 * meet these criteria: max length of 40 characters, only ASCII alphanumeric characters and selected punctuation marks
 * are allowed. Of course, the "src" attribute of each "trace" element that references the set with the corrected ID is 
 * likewise corrected.</p>
 * 
 * <p><em>Revised 04Sep08</em>. Increased the allowed character set for dataset IDs. Additional punctuation marks 
 * <em>@&lt;&gt;|.</em> are now allowed. Since this is a superset of the original character set, all existing 
 * <code>Schema8</code> documents will satisfy schema.</p>
 * 
 * @author 	sruffner
 */
class Schema8 extends Schema7
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema8"); }
   @Override public int getVersion() { return(8); }
   @Override public String getLastApplicationVersion() { return("3.0.4"); }

   
   /** The new "raster" element is the sole data presentation element for "raster1d" datasets. */
   final static String EL_RASTER = "raster";
   
   /**
    * One of the display modes for the "raster" element. In this mode, the individual raster trains in the data set 
    * are rendered as vertically spaced sequences of vertical hash marks.
    */
   final static String MODE_RASTER_TRAINS = "trains";

   /**
    * One of the display modes for the "raster" element: A histogram displaying the "counts per bin" observed across 
    * the entire dataset, which typically contains multiple individual rasters.
    */
   final static String MODE_RASTER_HIST = DISPMODE_HIST;
   
   /** The set of choices for the "raster" element's display mode (the "mode" attribute). */
   final static String[] MODE_RASTER_CHOICES = {MODE_RASTER_TRAINS, MODE_RASTER_HIST};

   /** Default for the "mode" attribute of the "raster" element. */
   final static String DEFAULT_RASTER_MODE = MODE_RASTER_TRAINS;

   /** Int-valued attr "nbins" is the number of bins in the histogram display mode for the "raster" element. */
   final static String A_NBINS = "nbins";

   /** Minimum number of bins allowed in the histogram display mode for the "raster" element. */
   final static int MIN_NBINS = 2;
   
   /** Maximum number of bins allowed in the histogram display mode for the "raster" element. */
   final static int MAX_NBINS = 100;
   
   /** Default value for number of bins in histogram display of the "raster" element. */
   final static String DEFAULT_NBINS = "10";
   
   /** Default value for train height (<code>A_HEIGHT</code>) of the "raster" element, in stroke widths. */
   final static String DEFAULT_RASTER_HEIGHT = "1";
   
   /** Default value for inter-train spacing (<code>A_SPACER</code>) of the "raster" element, in stroke widths. */
   final static String DEFAULT_RASTER_SPACER = "3";
   
   
   /** The new "heatmap" element is the sole data presentation element for "xyzimg" datasets (3D data). */
   final static String EL_HEATMAP = "heatmap";
   
   /** Enumerated "cmap" attr indicates what type of colormap to apply when rendering the "heatmap" element. */
   final static String A_CMAP = "cmap";

   /** Colormap choice for the "heatmap" element: 256-entry grayscale, black to white. */
   final static String CMAP_GRAY = "gray";
   /** Colormap choice for the "heatmap" element: 256-entry grayscale, white to black. */
   final static String CMAP_REVERSEGRAY = "reversegray";
   /** Colormap choice for the "heatmap" element: 256-entry, blue to cyan to green to yellow to red. */
   final static String CMAP_HOT = "hot";
   /** Colormap choice for the "heatmap" element: 256-entry, red to yellow to green to cyan to blue. */
   final static String CMAP_REVERSEHOT = "reversehot";
   /** Colormap choice for the "heatmap" element: 256-entry, black to red to yellow to white. */
   final static String CMAP_AUTUMN = "autumn";
   /** Colormap choice for the "heatmap" element: 256-entry, white to yellow to red to black. */
   final static String CMAP_REVERSEAUTUMN = "reverseautumn";
   
   /** The set of choices for the "heatmap" element's colormap (the "cmap" attribute). */
   final static String[] CMAP_CHOICES = {
      CMAP_GRAY, CMAP_REVERSEGRAY, CMAP_HOT, CMAP_REVERSEHOT, CMAP_AUTUMN, CMAP_REVERSEAUTUMN
   };

   /** Default for the "cmap" attribute of the "heatmap" element. */
   final static String DEFAULT_CMAP= CMAP_GRAY;

   /** 
    * New dataset format introduced in schema version 8: collection of 1-dimensional rasters (in X). Rendered only by 
    * the new "raster" element.
    */
   final static String SETFMT_RASTER1D = "raster1d";
   /** 
    * New dataset format introduced in schema version 8: 3D dataset Z=f(X,Y) sampled over [x0..x1, y0..y1]. Rendered 
    * only by the new "heatmap" element.
    */
   final static String SETFMT_XYZIMG = "xyzimg";
   
   /** Choices for a "set" element's "fmt" attribute, including the new formats introduced in schema version 8. */
   final static String[] SETFMT_CHOICES_V8 = {
      SETFMT_PTS_V7, SETFMT_SERIES_V7, SETFMT_MSET_V7, SETFMT_MSERIES_V7, SETFMT_RASTER1D, SETFMT_XYZIMG
   };

   /** 
    * New attr on a "set" element. It is only used during migration to mark pre-existing "set" elements as containing 
    * data in the pre-<code>Schema8</code> format: comma-separated tuples, where each tuple is a whitespace-separated 
    * list of floating-point tokens with up to 6 decimal digits in each token. As of schema version 8, the data is 
    * stored in the text content as base64-encoded binary. Conversion code must be able to handle both old and new 
    * formats.
    */
   final static String A_V7 = "v7";
   
   /** 
    * New attr on a "set" element. If present, it indicates that the dataset is identical -- except for the ID --
    * to one that is already defined in the schema document. In this case, the "set" element should have no text 
    * content!
    */
   final static String A_USING = "using";
   
   /**
	 * This element map contains <code>SchemaElementInfo</code> objects for each element that is new to this schema or 
    * has a different attribute set compared to the previous schema.
	 */
	private static Map<String, SchemaElementInfo> elementMap8 = null;

	static
	{
		elementMap8 = new HashMap<String, SchemaElementInfo>();
		
      // New elements "raster" and "heatmap" appear as children of a graph.
      elementMap8.put( EL_GRAPH, 
         new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH, 
                          EL_FUNCTION, EL_TRACE, EL_RASTER, EL_HEATMAP}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, A_CLIP, 
                          A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

      // "src" attribute is once again required. Otherwise, 'trace" element is as described in Schema7.
      elementMap8.put( EL_TRACE, 
            new SchemaElementInfo( false, 
                  new String[] {EL_SYMBOL, EL_EBAR}, 
                  new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                                A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_LEGEND, A_SKIP, A_MODE, A_BARWIDTH, A_BASELINE, 
                                A_FILLED, A_XOFF, A_YOFF, A_SRC}, 
                  new String[] {A_SRC} ));
      
      // The new "raster" element.
      elementMap8.put( EL_RASTER, 
            new SchemaElementInfo( false, 
                  new String[] {}, 
                  new String[] {A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                                A_MODE, A_BASELINE, A_FILLED, A_XOFF, A_HEIGHT, A_SPACER, A_NBINS, A_SRC}, 
                  new String[] {A_SRC} ));
      
      // The new "heatmap" element.
      elementMap8.put( EL_HEATMAP, 
            new SchemaElementInfo( false, 
                  new String[] {}, 
                  new String[] {A_TITLE, A_CMAP, A_SRC}, 
                  new String[] {A_SRC} ));
      
      // The "set" element has been significantly amended. Optional attributes "v7" and "using" were added. If the 
      // "v7" flag is "true", the text content will be in the old "comma-separated tuples" format of version 7. In this 
      // case, if the "dx" attribute is explicit, it is taken to specify the sample interval for a "series" or 
      // "mseries" datset. If the "v7" attribute is not present, any "dx" attribute is ignored, and the text content is 
      // a base64 encoding of the dataset, which is more compact but not readable. Everything about the dataset is 
      // encoded here: ID, format, other defining parameters (dx and x0 for "series" and "mseries", [x0 x1 y0 y1] for 
      // "xyzimg"), and the raw data itself. The <code>FGModelXMLConverter</code> must be able to handle both formats. 
      // Note that, in the new schema, the required "id" and "fmt" attribute values should match what's in the text 
      // content. An empty dataset is represented by an empty text content. The "using" attribute is a schema 8 
      // mechanism by which the same dataset is referenced under different IDs. When present, the text content of the 
      // "set" element is ignored and should be empty.
      elementMap8.put( EL_SET, 
            new SchemaElementInfo( true, 
                  new String[] {}, 
                  new String[] {A_ID, A_FMT, A_DX, A_V7, A_USING}, 
                  new String[] {A_ID, A_FMT} ));
	}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in schema version 8; otherwise defers 
    * to the superclass implementation.
    * @see ISchema#isSupportedElementTag(String)
    */
   @Override
   public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap8.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in schema version 8; for 
	 * all other element classes, it defers to the superclass implementation. 
	 * @see ISchema#getSchemaElementInfo(String)
	 */
   @Override
	public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = (SchemaElementInfo) elementMap8.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
	 *    <li>For all data presentation elements ("trace", "raster", "heatmap"), the "src" attribute must start with "#" 
	 *    and the rest of the string must represent a valid dataset identifier, as described below.</li>
	 *    <li>The "id" and "using" attributes of the revised "set" element must be non-empty and contain no whitespace.
	 *    They may contain only ASCII alphanumeric characters or any of the puncuation marks <em>$@|.<>_[](){}+-^!=</em>, 
	 *    and they can be no more than 40 characters long. The "fmt" attr must be one <code>SETFMT_CHOICES_V8</code>, 
	 *    which includes the two new dataset formats, "raster1d" and "xyzimg".</li>
    *    <li>The "mode" attribute of the new "raster" element must be one of <code>MODE_RASTER_CHOICES</code>.</li>
    *    <li>The "nbins" attribute of the "raster" element must be an integer attribute in <code>[MIN_NBINS .. 
    *    MAX_NBINS]</code>.</li>
    *    <li>The "height" and "spacer" attributes of the "raster" element must be integer attributes that are 
    *    strictly positive (1 or greater).</li>
    *    <li>The "cmap" attribute of the new "heatmap" element must be one of <code>CMAP_CHOICES</code>.</li>
	 * </ul>
	 * @see ISchema#isValidAttributeValue(BasicSchemaElement,String,String)
	 */
   @Override
	public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      if(A_SRC.equals(attr))
         return((value != null) && (value.length() > 1) && isValidSetID(value.substring(1)));
      
      String elTag = e.getTag();
      if(EL_SET.equals(elTag))
      {
         if(A_ID.equals(attr))
            return(isValidSetID(value));
         else if(A_FMT.equals(attr))
            return(isValidEnumAttributeValue(value, SETFMT_CHOICES_V8));
         else if(A_USING.equals(attr))
            return(isValidSetID(value));
         else if(A_V7.equals(attr))
            return("true".equals(value) || "false".equals(value));
      }
      else if(EL_RASTER.equals(elTag))
      {
         if(A_MODE.equals(attr)) return(isValidEnumAttributeValue(value, MODE_RASTER_CHOICES));
         else if(A_NBINS.equals(attr)) return(isValidIntegerAttributeValue(value, MIN_NBINS, MAX_NBINS));
         else if(A_HEIGHT.equals(attr) || A_SPACER.equals(attr))
            return(isValidIntegerAttributeValue(value, 1, Integer.MAX_VALUE));
         else if(A_XOFF.equals(attr) || A_BASELINE.equals(attr))
            return(isValidFloatAttributeValue(value, -Double.MAX_VALUE, Double.MAX_VALUE));
      }
      else if(EL_HEATMAP.equals(elTag))
      {
         if(A_CMAP.equals(attr)) return(isValidEnumAttributeValue(value, CMAP_CHOICES));
      }
      
      return(super.isValidAttributeValue(e, attr, value));
	}

   
   /**
    * The "set" element's text content was dramatically changed in <code>Schema8</code>. For compactness and a certain 
    * degree of data protection, the data is now encoded in base64. Also, the ID, format, and other metadata is 
    * included in the text content, followed by the raw data. As a result, we chose NOT to validate the content until
    * the schema document is actually converted to figure model, so this method simply returns <code>true</code>. 
    * (NOTE: If the element's <code.A_V7</code> attribute is explicitly set, then the text content is in the old 
    * "comma-separated tuples" format, and the method defers to the super class.
    * <p>For all other elements, the method defers to the super class.</p>
    * 
    * @see BaseSchema#isValidTextContent(BasicSchemaElement, String)
    */
   @Override
   public boolean isValidTextContent(BasicSchemaElement e, String text)
   {
      if(EL_SET.equals(e.getTag()))
      {
         String v7 = null;
         try { v7 = e.getAttributeValueByName(A_V7); } catch(XMLException xe) { return(false); }
         if(!"true".equals(v7)) return(true);
      }
      
      return(super.isValidTextContent(e, text));
   }
   
   /**
	 * This method handles the actual details of migrating <code>Schema7</code> content to <code>Schema8</code>. It 
    * makes the following changes:
	 * <ul>
    *    <li>The "src" attribute on a "trace" element was optional in the previous schema, indicating that the trace 
    *    element was not linked to a dataset. This is no longer permissible in the current schema. If any such "trace" 
    *    elements are found, an empty dataset in "ptset" format is appended to the "ref" element, and all "data-less" 
    *    traces are linked to this set.</li>
    *    <li>For each "set" element under the "ref" node, the "v7" attribute is explicitly set to "true", indicating
    *    that this element conforms to the schema version 7. As of version 8, raw data is stored using an entirely 
    *    different structure and is written to the text content of the "set" as base64-encoded binary. To convert 
    *    directly from the "comma-separated tuples" format of version 7 to the new form was not worth the effort, so we 
    *    decided to support both formats -- BUT ONLY FOR MIGRATION PURPOSES. The schema framework will not save newly 
    *    created content in the older format.</li>
    *    <li>Each "set" element in the old schema that is not referenced by a "trace" element is an "orphan". Since 
    *    orphaned datasets are not supported in schema version 8, such orphaned "sets" are excised here.</li>
    *    <li>Correct any invalid dataset IDs to satisfy the new rules enforced in schema version 8 (very restricted 
    *    character set, max length of 40). Fix both the affected "set" element and any "trace" elements that used the 
    *    set.</li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
	 */
	public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
	{
		if(oldSchema.getVersion() != getVersion() - 1) 
			throw new XMLException("A schema instance can only migrate from the previous version.");

		// this flag is set if there is at least one "trace" element that is not linked to a dataset via "src" attrib
		boolean fixNullSrcAttr = false;
		
		// list of ALL "trace" elements in the old schema, in case we need to change their "src" attribute
		List<BasicSchemaElement> traces = new ArrayList<BasicSchemaElement>();
		
		// original IDs of all datasets referenced in any "trace" elements found. The original IDs are the map keys and 
		// all values are initially null. This is used to (1) detect and remove dataset orphans, and (2) to fix any IDs 
		// that are invalid in schema 8. After processing of the schema, the keys in the hashmap are examined. If any ID 
		// is found to be invalid, the corresponding VALUE in the hashmap will hold a replacement ID. The corresponding 
		// "set" and all referencing "trace" elements are updated with the replacement ID. See below...
		HashMap<String, String> setIDMap = new HashMap<String, String>();
		
      // update the content of the old schema in place...
      Stack<BasicSchemaElement> elementStack = new Stack<BasicSchemaElement>();
      elementStack.push((BasicSchemaElement) oldSchema.getRootElement());
      while(!elementStack.isEmpty())
      {
         BasicSchemaElement e = elementStack.pop();
         String elTag = e.getTag();

         // account for dataset IDs in use, as well as data traces that have no dataset reference (now required)
         if(EL_TRACE.equals(elTag))
         {
            traces.add(e);
            String setID = e.getAttributeValueByName(A_SRC);
            if(setID != null) setIDMap.put(setID.substring(1), null);      // strip off leading "#"
            else fixNullSrcAttr = true;
         }
         
         // migrate the element object's schema info.
         e.updateSchema(this, null);

         // mark all "set" elements with the new "v7=true" attribute
         if(EL_SET.equals(elTag)) e.setAttributeValueByName(A_V7, "true");

         // if element has any children, push them onto the stack so that we check them as well! We don't push on any 
         // child that was added during migration of the parent, since it will already conform to current schema.
         for(int i=0; i<e.getChildCount(); i++)
         {
            BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
            elementStack.push(child);
         }
      }

      // remove any "set" elements in "ref" node that are orphans, ie, not referenced by any "trace" element.
      List<BasicSchemaElement> sets = new ArrayList<BasicSchemaElement>();
      BasicSchemaElement fig = (BasicSchemaElement) oldSchema.getRootElement();
      BasicSchemaElement ref = (BasicSchemaElement) fig.getChildAt(fig.getChildCount()-1);
      int i = 0;
      while(i < ref.getChildCount())
      {
         BasicSchemaElement set = (BasicSchemaElement) ref.getChildAt(i);
         String id = set.getAttributeValueByName(A_ID);
         if(setIDMap.containsKey(id)) 
         {
            sets.add(set);
            ++i;
         }
         else ref.remove(i);
      }
      
      // found at least one "trace" that was not linked to a dataset in the previous schema. Fix by adding an empty 
      // point set and linking it to all such traces.
      if(fixNullSrcAttr)
      {
         // ensure assigned ID is unique!
         String uid = "emptySet";
         if(setIDMap.containsKey(uid))
         {
            String adjID = uid;
            int n = 0;
            do
            {
               ++n;
               adjID = uid + n;
            }
            while(setIDMap.containsKey(adjID));
            uid = adjID;
         }
         
         BasicSchemaElement emptySet = (BasicSchemaElement) createElement(EL_SET);
         emptySet.setAttributeValueByName(A_ID, uid);
         emptySet.setAttributeValueByName(A_FMT, SETFMT_PTS_V7);
         emptySet.setAttributeValueByName(A_V7, "true");   // in Schema8, an empty set has some text content!
         ref.add(emptySet);

         sets.add(emptySet);
         setIDMap.put(uid, null);

         String src = "#" + uid;
         for(BasicSchemaElement trace : traces) if(null == trace.getAttributeValueByName(A_SRC))
            trace.setAttributeValueByName(A_SRC, src);
      }

      // fix any invalid dataset IDs while maintaining uniqueness. If we make any changes, the original ID is still the 
      // hashmap key, and the hashmap value is the replacement ID. Update relevant "set" element and all "trace" 
      // elements that were linked to the original ID.
      List<String> badIDsFound = enforceValidAndUniqueSetIDs(setIDMap);
      for(String badID : badIDsFound)
      {
         String fixedID = setIDMap.get(badID);
         for(BasicSchemaElement set : sets) if(badID.equals(set.getAttributeValueByName(A_ID)))
         {
            set.setAttributeValueByName(A_ID, fixedID);
            break;  // there can only be one set with the original bad ID (that's enforced in previous schema)
         }
         
         for(BasicSchemaElement trace : traces) if(badID.equals(trace.getAttributeValueByName(A_SRC).substring(1)))
         {
            trace.setAttributeValueByName(A_SRC, "#" + fixedID);
         }
      }
      
      // the content model now conforms to this schema. We get the root element from the old schema and install it as 
      // the root of this schema, then empty the old schema. We also remember the original schema version of the 
      // migrated content.
      originalVersion = oldSchema.getOriginalVersion();
      setRootElement(oldSchema.getRootElement(), false);
      oldSchema.setRootElement(null, false);
	}
	
	
	//
	// Enforcing valid dataset IDs IAW with new rules in Schema 8
	//
	
   /**
    * A compiled regular expression that is used to validate candidate dataset IDs in schema version 8: they must have 
    * at least one printable ASCII character that is an alphanumeric character or one of <em>$@|.<>_[](){}+-^!=</em>.
    */
	private static Pattern idVerifier = 
	   Pattern.compile("[a-zA-Z0-9_=\\$\\@\\|\\.\\<\\>\\[\\]\\(\\)\\{\\}\\+\\-\\^\\!]+");

	/** Used to clean illegal characters from a candidate dataset ID in schema version 8.*/
	private static Matcher idCleaner = 
	   Pattern.compile("[^a-zA-Z0-9_=\\$\\@\\|\\.\\<\\>\\[\\]\\(\\)\\{\\}\\+\\-\\^\\!]+").matcher(" ");
	
   /** The maximum length of a valid dataset identifier string, as of schema version 8. */
   private final static int MAXIDLEN = 40;
   
   /**
    * Does the specified string qualify as the identifier for a <em>DataNav</em> dataset as of schema version 8? By 
    * convention, a valid ID must be a non-empty string that can be up to 40 characters long and that contains only 
    * ASCII alphanumeric characters or <em>$@|.<>_[](){}+-^!=</em>. Note that the ID cannot have any whitespace.
    * @param s The string to test.
    * @return <code>True</code> iff string meets criteria described above.
    */
   private static boolean isValidSetID(String s) 
   { 
      return((s != null) && (s.length() <= MAXIDLEN) && idVerifier.matcher(s).matches()); 
   }

   /**
    * Helper method for <code>migrateFromPreviousSchema()</code>. It verifies that each dataset ID appearing as a key 
    * in the hashmap provided is valid IAW the new rules imposed in schema version 8. If it is not, a new ID is created 
    * and is entered into the hashmap as the value keyed by the original, invalid ID. The new ID is derived from the 
    * original by removing any illegal characters and truncating to a maximum of 40 characters. If the fixed ID is no 
    * longer unique, it is adjusted (typically by appending a one-digit integer string) to make it so.
    * @param idmap Hashmap of dataset IDs in use. Initially, all values in the map should be <code>null</code>. If this 
    * method fixes any of the IDs, the replacement ID will appear in the map as the value keyed by the original, 
    * invalid ID.
    * @return The list of invalid IDs found.
    */
   private static List<String> enforceValidAndUniqueSetIDs(HashMap<String, String> idmap)
   {
      List<String> invalidOrigIDs = new ArrayList<String>();
      Set<String> originalIDs = idmap.keySet();
      for(String origID : originalIDs) if(!isValidSetID(origID))
      {
         invalidOrigIDs.add(origID);
         
         String adjID = (origID==null || origID.length()==0) ? "set" : idCleaner.reset(origID).replaceAll("");
         if(adjID.length() == 0) adjID = "set";
         else if(adjID.length() > MAXIDLEN) adjID = adjID.substring(0, MAXIDLEN);
         
         if(idmap.containsKey(adjID) || idmap.containsValue(adjID))
         {
            String uid = adjID;
            int n = 0;
            do
            {
               ++n;
               uid = adjID + n;
            }
            while(idmap.containsKey(uid) || idmap.containsValue(uid));
            adjID = uid;
         }
         idmap.put(origID, adjID);
      }
      return(invalidOrigIDs);
   }
}
