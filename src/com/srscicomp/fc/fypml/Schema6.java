package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import com.srscicomp.common.util.Utilities;
import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema6</code> is the encapsulation of the <em>DataNav/Phyplot</em> XML schema version 6. It extends 
 * <code>Schema5</code> and includes support for migrating schema version 5 documents to version 6. The following 
 * schema changes were introduced in this version:
 * <ul>
 *		<li>[Mods thru 11/2/2006, for app version 1.0.1] The old "pointSet", "multiSet", and "series" elements have been 
 *    replaced by a single "trace" element, a presentation element which defines how a data set is rendered in a graph. 
 *    The "trace" element supports four different display modes -- as a polyline, as a collection of polylines (ie, a 
 *    multi-set), as an error band, or as a histogram. The "trace" element has most of the attributes of the old 
 *    "series" element, with some exceptions and a few additions. The "dx" attribute is gone -- this is considered an 
 *    attribute of the data itself, not its presentation. The "x0" attribute is now called "xoff" and is accompanied by 
 *    a new "yoff" attribute, allowing the user to apply an arbitrary y-offset to the actual data. The data set itself 
 *    is now stored separately (see below), and referenced by the "trace" element's new "src" attribute.</li>
 *    <li>[Mods thru 11/2/2006, for app version 1.0.1] The "figure" element now contains a single required "ref" 
 *    element, which is <em>always</em> its last child. The "ref" element has no attributes; it merely serves as a 
 *    container for any number of "set" nodes. This is where "locally stored data sets" are kept in the <em>DataNav</em> 
 *    document. As before, the data set is stored in the text content of the "set" node as a comma-separated list of 
 *    datum tuples, where each such tuple is a whitespace-separated list of 1+ floating-point numbers. Additional info 
 *    about the set is specified in three attributes. The "id" attribute is a non-empty string with no whitespace that 
 *    serves as an identifier for the data set: a "trace" element references a local "set" with id="myset" by setting its 
 *    src="#myset". The "fmt" attribute indicates the data set format -- "pointset", "series", "multiset", or 
 *    "multiseries". The "pointset" format corresponds directly to the old "pointSet" element, where each tuple is a 
 *    list of 2-6 numbers: x y [yStd ye xStd xe]. The "series" format corresponds directly to the old "series" element, 
 *    where each tuple is a list of 1-3 numbers: y [yStd ye]. The "multiset" format is a more general version of the old 
 *    "multiSet" element's format. Here each tuple is a list of 2 or more floating-pt numbers: x y1 [y2 y3 ...]. All 
 *    tuples should have the same length, but if that is not the case, the minimum tuple length determines the number of 
 *    individual sets in the collection (the extra numbers in the longer tuples are simply ignored). Note that, for 
 *    generality's sake, the format permits a collection that defines only a single point set! Finally, the 
 *    "multiseries" format is completely new -- it is essentially a sampled "multiset". Each tuple is a list of 1 or 
 *    more floating-pt numbers: y1 [y2 y3 ...]. For the two sampled formats, the "dx" attribute indicates the sample 
 *    interval for the x-coordinate; its implicit value is 1.</li>
 *    <li>[Mods thru 11/2/2006, for app version 1.0.1] The "function" element no longer has the "skip" attribute. It 
 *    was never really needed since "dx" can really serve the same purpose. During migration, if a "function" element 
 *    is encountered with skip!=1, its dx value is simply adjusted by dx' = dx*skip.</li>
 * </ul>
 * <p>[<em>DataNav</em> superceded <em>Phyplot</em> in 2008.]</p>
 * @author 	sruffner
 */
class Schema6 extends Schema5
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/phyplot/namespace/schema6"); }
   @Override public int getVersion() { return(6); }
   @Override public String getLastApplicationVersion() { return("1.0.7"); }

   /**
    * A data presentation element, introduced in schema version 6, determines how a data set is rendered in a graph. The 
    * data set is stored elsewhere in the document and referenced by the data presentation element's "src" attribute. 
    * This element replaces the following elements in the previous schema: "pointSet", "series", "multiSet".
    */
   final static String EL_TRACE = "trace";

   /**
    * One of the display modes for the new "trace" element. In this mode, data set is rendered as a polyline possibly 
    * adorned with marker symbols and error bars.
    */
   final static String DISPMODE_PTS = "polyline";

   /**
    * One of the display modes for the new "trace" element. In this mode, an unadorned polyline connects the actual data 
    * points in the set {x,y}, while two additional polylines represent +/-1 standard deviation (in Y) about the nominal 
    * data trace. The band between the two standard deviation polylines may optionally be  filled with a solid color. No 
    * symbols or error bars are rendered with this display mode.
    */
   final static String DISPMODE_BAND = "errorband";

   /**
    * One of the display modes for the new "trace" element: A histogram display, with each data point (x,y) represented 
    * by a vertical bar centered at x and drawn from a specified baseline yB to the value y. Data point marker symbols 
    * and error bars may be rendered on top of this histogram display IAW relevant attributes of the "trace" element.
    */
   final static String DISPMODE_HIST = "histogram";

   /**
    * One of the display modes for the new "trace" element: A multitrace display, intended for the display of data sets 
    * that contain a collection of individual point sets {x: y1 y2 ...} that represent repeated measurements of the 
    * same stochastic variable.
    */
   final static String DISPMODE_MULTI = "multitrace";

   /** The set of choices for a data presentation element's display mode (the "type" attribute). */
   final static String[] DISPMODE_CHOICES = {DISPMODE_PTS, DISPMODE_BAND, DISPMODE_HIST, DISPMODE_MULTI};

   /** Default for the "type" attribute of a "trace" element. */
   final static String DEFAULT_DISPMODE_TYPE = DISPMODE_PTS;

   /**
    * New attribute specifies an arbitrary offset applied to the x-coordinate of all data points when a "trace" 
    * presentation element is rendered.
    */
   final static String A_XOFF = "xoff";

   /**
    * New attribute specifies an arbitrary offset applied to the y-coordinate of all data points when a "trace" 
    * presentation element is rendered.
    */
   final static String A_YOFF = "yoff";

   /** The default (implicit) value for the "xoff" and "yoff" attributes of the new "trace" element. */
   final static String DEFAULT_XYOFF = "0";

   /**
    * New attribute identifies the data set to be rendered IAW the definition of the owning "trace" element. Currently, 
    * only <em>local</em> data sets are supported, and the attribute must take the form "#{id}", where {id} is the data 
    * set's identifier.
    */
   final static String A_SRC = "src";

   /** The default (implicit) value for the "src" attribute of the new "trace" element. */
   final static String DEFAULT_SRC = "#setno";

   /**
    * Regular expression used to validate the value of the "src" attribute. It must start with "#", must contain at 
    * least one other character, and cannot have any whitespace.
    */
   final static String REGEX_SRC = "#[\\S]+";

   /**
    * The "ref" element, introduced in schema vertion 6, serves only as a container for any number of "set" nodes, each 
    * of which stores a separate <em>local data set</em>. The "ref" element is always the last child of the root 
    * "figure" element. It has no attributes.
    */
   final static String EL_REF = "ref";

   /**
    * The "set" element, introduced in schema version 6, encapsulates a <em>local data set</em>. The text content 
    * defines the data as a list of comma-separated datum tuples, where each tuple is a whitespace-separated list of 1+ 
    * floating-pt numbers.
    */
   final static String EL_SET = "set";

   /**
    * The new "id" attribute of a "set" element is its local ID for the purpose of linking it to a particular "trace" 
    * element in the parent figure.
    */
   final static String A_ID = "id";

   /** The default (implicit) value for the "id" attribute of the new "set" element. */
   final static String DEFAULT_ID = "setno";

   /**
    * Regular expression used to validate the value of the "id" attribute. It must contain at least one character, but 
    * cannot have any whitespace.
    */
   final static String REGEX_ID = "[\\S]+";

   /** Data set format representing a single (x,y) point set with optional error info: "x y [yStd ye xStd xe]". */
   final static String SETFMT_PTS = "pointset";

   /** Data set format for a data series sampled at regular intervals in x, with optional error info: "y [yStd ye]". */
   final static String SETFMT_SERIES = "series";

   /** Data set format for a collection of individual point sets sharing the same x-coords: "x y1 [y2 y3 ...]". */
   final static String SETFMT_MSET = "multiset";

   /** Data set format for a collection of data series sampled at regular intervals in x: "y1 [y2 y3 ...]". */
   final static String SETFMT_MSERIES = "multiseries";

   /** The list of supported data set formats -- the possible values for a "set" element's "fmt" attribute. */
   final static String[] SETFMT_CHOICES = {SETFMT_PTS, SETFMT_SERIES, SETFMT_MSET, SETFMT_MSERIES};

   /** Default for the "fmt" attribute of a "set" element. */
   final static String DEFAULT_SETFMT = SETFMT_PTS;


   /**
	 * This element map contains <code>SchemaElementInfo</code> objects for each element that is new to this schema or 
    * has a different attribute set compared to the previous schema.
	 */
	private static final Map<String, SchemaElementInfo> elementMap6;

	static
	{
		elementMap6 = new HashMap<>();

      // revised element map entry for the 'figure' element, which now includes the new "ref" element
      elementMap6.put( EL_FIGURE, 
            new SchemaElementInfo( false, 
               new String[] {EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH, EL_REF}, 
               new String[] {A_FONT, A_PSFONT, A_SUBSTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                  A_STROKECOLOR, A_CAP, A_CAPSIZE, A_MID, A_SPACER, A_LABELOFFSET, A_PERLOGINTV, A_DIR, A_LEN, 
                  A_FMT, A_GAP, A_BORDER, A_TITLE, A_LOC, A_WIDTH, A_HEIGHT}, 
               new String[] {A_FONT, A_PSFONT, A_SUBSTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                  A_STROKECOLOR, A_CAP, A_CAPSIZE, A_MID, A_SPACER, A_LABELOFFSET, A_PERLOGINTV, A_DIR, A_LEN, 
                  A_FMT, A_GAP, A_LOC, A_WIDTH, A_HEIGHT} ));

      // revised element map entry for the 'graph' element: the "pointSet", "series" and "multiSet" nodes are replaced 
      // by the more generic "trace" element.
      elementMap6.put( EL_GRAPH, 
         new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH, 
                          EL_FUNCTION, EL_TRACE}, 
            new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
                          A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, A_CLIP, A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

      // revised element map entry for the 'function' element, which no longer has the "skip" attribute
      elementMap6.put( EL_FUNCTION, 
         new SchemaElementInfo( true, 
            new String[] {EL_SYMBOL}, 
            new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
                          A_TITLE, A_LINETYPE, A_HIDE, A_X0, A_X1, A_DX}, 
            new String[] {} ));

      // element map entry for the new 'trace' element
      elementMap6.put( EL_TRACE, 
            new SchemaElementInfo( false, 
                  new String[] {EL_SYMBOL, EL_EBAR}, 
                  new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
                                A_TITLE, A_LINETYPE, A_SKIP, A_HIDE, A_TYPE, A_BARWIDTH, A_BASELINE, A_FILLED,
                                A_XOFF, A_YOFF, A_SRC}, 
                  new String[] {A_SRC} ));

      // element map entry for the new 'ref' element
      elementMap6.put( EL_REF, 
            new SchemaElementInfo( false, 
                  new String[] {EL_SET}, 
                  new String[] {}, 
                  new String[] {} ));

      // element map entry for the new 'set' element
      elementMap6.put( EL_SET, 
            new SchemaElementInfo( true, 
                  new String[] {}, 
                  new String[] {A_ID, A_FMT, A_DX}, 
                  new String[] {A_ID, A_FMT} ));
	}

   /**
	 * Overridden to recognize any elements added in schema version 6 ("ref", "trace" and "set") and exclude elements 
    * that were removed in this version ("pointSet", "series", and "multiSet"). Otherwise it defers to the superclass 
	 * implementation.
	 * @see ISchema#isSupportedElementTag(String)
	 */
   @Override
	public boolean isSupportedElementTag(String elTag)
	{
      if(EL_POINTSET.equals(elTag) || EL_SERIES.equals(elTag) || EL_MULTISET.equals(elTag)) return(false);
      else return(elementMap6.containsKey(elTag) || super.isSupportedElementTag(elTag));
	}

   /**
    * Overridden to provide schema element information for any element class added or revised in schema version 6; for
    * all other element classes, it defers to the superclass implementation. 
    * @see ISchema#getSchemaElementInfo(String)
    */
   @Override
	public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = elementMap6.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * This method enforces any requirements on the children content of elements that were introduced or revised in 
	 * schema version 6. For all other element classes, it defers to the superclass implementation.
    * <ul>
    *    <li>The new "set" and "ref" elements have no required children.</li>
    *    <li>The "figure" element's last child must be the new "ref" element.</li>
    *    <li>The new "trace" element has two required children, a "symbol" element followed by an "ebar" element.</li>
    * </ul>
	 * @see ISchema#hasRequiredChildren(BasicSchemaElement)
	 */
   @SuppressWarnings("rawtypes")
   @Override
	public boolean hasRequiredChildren(BasicSchemaElement e)
	{
      String elTag = e.getTag();
      List children = e.getElementContent();
      if(EL_SET.equals(elTag) || EL_REF.equals(elTag)) 
         return(true);
      else if(EL_FIGURE.equals(elTag))
      {
         int n = children.size();
         return((n > 0) && EL_REF.equals(((BasicSchemaElement) children.get(n-1)).getTag()));
      }
      else if(EL_TRACE.equals(elTag))
         return(children.size() > 1 && 
            EL_SYMBOL.equals(((BasicSchemaElement) children.get(0)).getTag()) &&
            EL_EBAR.equals(((BasicSchemaElement) children.get(1)).getTag()));

      return(super.hasRequiredChildren(e));
	}

   /**
	 * Overridden to enforce any structural restrictions on the children content of elements that were introduced or 
	 * revised in schema version 6. For all other element classes, it defers to the superclass implementation.
    * <ul>
    *    <li>The new "trace" element has two required children, a "symbol" element followed by an "ebar" element -- and 
    *    no others.</li>
    *    <li>The "figure" element now has one required child, the new "ref" element, which must always be the last 
    *    child in the figure node.</li>
    * </ul>
	 * @see ISchema#isValidChildAtIndex(BasicSchemaElement,String,int)
	 */
   @Override
	public boolean isValidChildAtIndex(BasicSchemaElement e, String childTag, int index)
	{
      String elTag = e.getTag();
      if(EL_TRACE.equals(elTag))
         return((EL_SYMBOL.equals(childTag) && index == 0) || (EL_EBAR.equals(childTag) && index == 1));
      if(EL_FIGURE.equals(elTag))
      {
         // may be called during or after binding. In the latter case, index must point to last child; in the former, 
         // it must equal the current child count.
         int n = e.getChildCount();
         if(EL_REF.equals(childTag)) return(index == n || index == n-1);
      }

      return(super.isValidChildAtIndex(e, childTag, index));
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
	 *    <li>The "xoff" and "yoff" attributes of the new "trace" element must be parsable as floating-pt values.</li>
    *    <li>The "src" attribute of the new "trace" element must start with "#" and contain at least one other 
    *    character, but no whitespace.</li>
    *    <li>The "type" attribute of the new "trace" element is an enumerated attribute that must take on one of the 
    *    values in <code>DISPMODE_CHOICES</code>.</li>
    *    <li>The "id" attribute of the new "set" element must contain at least one character and no whitespace.</li>
    *    <li>The "fmt" attribute of the new "set" element is an enumerated attribute that must take on one of the 
    *    values in <code>SETFMT_CHOICES</code>.</li>
    *    <li>The "dx" attribute of the new "set" element must be parsable as a floating-pt value.</li>
	 * </ul>
	 * @see ISchema#isValidAttributeValue(BasicSchemaElement,String,String)
	 */
   @Override
	public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      String elTag = e.getTag();
      if(EL_TRACE.equals(elTag))
      {
         if(A_XOFF.equals(attr) || A_YOFF.equals(attr))
            return(isValidFloatAttributeValue(value, -Double.MAX_VALUE, Double.MAX_VALUE));
         else if(A_SRC.equals(attr))
            return((value != null) && value.matches(REGEX_SRC));
         else if(A_TYPE.equals(attr))
            return(isValidEnumAttributeValue(value, DISPMODE_CHOICES));
      }
      else if(EL_SET.equals(elTag))
      {
         if(A_ID.equals(attr))
            return((value != null) && value.matches(REGEX_ID));
         else if(A_FMT.equals(attr))
            return(isValidEnumAttributeValue(value, SETFMT_CHOICES));
         else if(A_DX.equals(attr))
            return(isValidFloatAttributeValue(value, -Double.MIN_VALUE, Double.MAX_VALUE));
      }

      return(super.isValidAttributeValue(e, attr, value));
	}

   /**
    * The new "set" element's text must be a list of comma-separated datum tuples. Each such tuple must be a 
    * whitespace-separated list of 1+ floating-pt numbers. Unlike the old "pointSet", "series", and "multiSet" 
    * elements, the "set" element does not place restrictions on the length of each tuplet, aside from the fact that it 
    * must be greater than 0. [A "set" element can be defined in an invalid state in this schema; <em>Phyplot</em> 
    * treated an invalid set as empty and warns the user.]
    * <p>For all other elements, the method defers to the super class.</p>
    */
   @Override
   public boolean isValidTextContent(BasicSchemaElement e, String text)
   {
      if(EL_SET.equals(e.getTag()))
      {
         if(text == null || text.isEmpty()) return(true);

         StringTokenizer st = new StringTokenizer(text, ",");
         while(st.hasMoreTokens())
         {
            String tuple = st.nextToken().trim();
            
            // 29apr2014: Allow for possibility that text content ENDS with a comma followed only by whitespace, in
            // which case we might get a zero-length tuple here.
            if(tuple.isEmpty()) return(!st.hasMoreTokens());

            StringTokenizer tupleTokenizer = new StringTokenizer(tuple);
            if(tupleTokenizer.countTokens() < 1) return(false);

            try
            {
               while(tupleTokenizer.hasMoreTokens()) Double.parseDouble(tupleTokenizer.nextToken());
            }
            catch(NumberFormatException nfe) { return(false); }
         }
         return(true);
      }

      return(super.isValidTextContent(e, text));
   }

   /**
	 * This method handles the actual details of migrating <code>Schema5</code> content to <code>Schema6</code>. It 
    * makes the following changes:
	 * <ul>
	 * 	<li>For each "function" element encountered, the "skip" attribute is removed. IF it was explicitly set to a 
    *    value <em>s&gt;1</em>, then the "dx" attribute is changed to <em>oldDx * s</em>.</li>
    *    <li>For every "pointSet", "series", and "multiSet" element, E, encountered:
    *    <ol>
    *       <li>A new "set" node is created, with "id" attribute set to "srcN", where N is the zero-based index of the 
    *       number of data sets created thus far. The text content of E is copied as is into the "set" node, the "fmt" 
    *       attribute is initialized IAW the identity of E, and the "dx" attribute matches the value of E's "dx".</li>
    *       <li>E is replaced by the generic "trace" presentation element. The "src" attribute is set to "#srcN". If E 
    *       has an explicit "x0" attribute, "xoff" takes on this value, and "x0" is removed. The "type" attribute is 
    *       set to "polyline" if E="pointSet", and "multitrace" if E="multiSet". If E="series", the attribute is set 
    *       IAW E's old "type" attribute ("points" = "polyline", the default; "trace" = "errorband"; "histogram" = 
    *       "histogram"). All other attributes explicitly set on E will also be explicitly set on the new 
    *       "trace" node, except that the "dx" attribute is removed ("dx" is considered a property of the data set 
    *       rather than the data presentation; "x0" is considered a property of the data presentation, but has been 
    *       renamed "xoff").</li>
    *    </ol>
    *    </li>
    *    <li>A new "ref" node is created as the last child of the root "figure" element. All "set" nodes prepared 
    *    during migration of the old "pointSet", "series" and "multiSet" elements are stored as children of the "ref" 
    *    node.</li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
	 */
	public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
	{
		if(oldSchema.getVersion() != getVersion() - 1) 
			throw new XMLException("A schema instance can only migrate from the previous version.");

      // create the new "ref" node. We'll add "set" nodes to this as they are created. After we're done migrating, 
      // we'll append this node as the last child of the root figure.
      BasicSchemaElement refNode = (BasicSchemaElement) createElement(EL_REF);

      // update the content of the old schema in place...
      Stack<BasicSchemaElement> elementStack = new Stack<>();
      elementStack.push((BasicSchemaElement) oldSchema.getRootElement());
      while(!elementStack.isEmpty())
      {
         BasicSchemaElement e = elementStack.pop();
         String elTag = e.getTag();

         // "function": remove explicitly set "skip" attribute and set "dx" to skip*oldDx
         if(EL_FUNCTION.equals(elTag))
         {
            String skip = e.getAttributeValueByName(A_SKIP);
            String dx = e.getAttributeValueByName(A_DX);
            if(skip != null)
            {
               int skipVal = 0;
               double dxVal = 0;
               try
               {
                  skipVal = Integer.parseInt(skip);
                  dxVal = (dx != null) ? Double.parseDouble(dx) : 1;
               }
               catch(NumberFormatException ignored) {}

               if(skipVal != 1)
               {
                  dxVal *= skipVal;
                  dx = Utilities.toString(dxVal, 7, -1);
                  e.setAttributeValueByName(A_DX, dx);
               }

               e.removeAttributeByName(A_SKIP);
            }
         }

         // for any "pointSet", "series", or "multiSet" encountered...
         boolean wasPointSet = EL_POINTSET.equals(elTag);
         boolean wasMultiSet = EL_MULTISET.equals(elTag);
         boolean wasSeries = EL_SERIES.equals(elTag);
         boolean isDataEl = wasPointSet || wasMultiSet || wasSeries;
         String srcAttrVal = null;
         String typeAttrVal = null;
         String xoffAttrVal = null;
         if(isDataEl)
         {
            // create a new "set" node for the data set and append it to our "ref" element. Required "id" attr will be
            // "srcN", where N is the ordinal position in the "ref" element. Required "fmt" attr depends on identity 
            // of the old element. 
            BasicSchemaElement set = (BasicSchemaElement) createElement(EL_SET);
            int pos = refNode.getChildCount();
            String id = "src" + pos;
            set.setAttributeValueByName(A_ID, id);
            set.setAttributeValueByName(A_FMT, wasPointSet ? SETFMT_PTS : (wasSeries ? SETFMT_SERIES : SETFMT_MSET));
            refNode.add(set);

            // if the old element was a "series" and had an explicit "dx" attribute, that is passed to the "set" node 
            // and removed from what will become the data "trace" presentation node. 
            String dx = wasSeries ? e.getAttributeValueByName(A_DX) : null;
            if(dx != null)
            {
               if(!dx.equals("1")) set.setAttributeValueByName(A_DX, dx);
               e.removeAttributeByName(A_DX);
            }

            // copy the text content as is from the old element to the "set" node
            set.setTextContent(e.getTextContent(), false);

            // determine what the value for the "type" attribute should be. Note that "polyline" is the implicit value.
            if(wasMultiSet) typeAttrVal = DISPMODE_MULTI;
            else if(wasSeries)
            {
               // for the old "series" element, the default value for "type" was "trace"!
               String oldType = e.getAttributeValueByName(A_TYPE);
               if(oldType == null || SERIES_TYPE_TRACE.equals(oldType)) typeAttrVal = DISPMODE_BAND;
               else if(SERIES_TYPE_HIST.equals(oldType)) typeAttrVal = DISPMODE_HIST;
            }

            // the "src" attribute for the "trace" node is set to "#srcN"
            srcAttrVal = "#" + id;
            
            // if the old element was a "series" and had an explicit "x0" attribute, its value is given to the "xoff" 
            // attribute on what will become the "trace" node. The old "x0" attribute is removed.
            String x0 = wasSeries ? e.getAttributeValueByName(A_X0) : null;
            if(x0 != null)
            {
               if(!DEFAULT_XYOFF.equals(x0)) xoffAttrVal = x0;
               e.removeAttributeByName(A_X0);
            }

         }

         // migrate the element object's schema info. This will update the tag name of "pointSet", "series" and 
         // "multiSet" to "trace", and it will remove any text content, which has already been copied elsewhere.
         e.updateSchema(this, isDataEl ? EL_TRACE : null);

         // for the migrated "trace" nodes: now that the schema is updated, we need to set the (explicit) values for the
         // new "src" and "xoff" attributes. Note that the "type" attribute could already be explicitly set if the 
         // element converted had been a "series".
         if(isDataEl)
         {
            e.setAttributeValueByName(A_SRC, srcAttrVal);
            if(xoffAttrVal != null) e.setAttributeValueByName(A_XOFF, xoffAttrVal);
            if(typeAttrVal != null) e.setAttributeValueByName(A_TYPE, typeAttrVal);
            else e.removeAttributeByName(A_TYPE);
         }

         // if element has any children, push them onto the stack so that we check them as well! 
         for(int i=0; i<e.getChildCount(); i++)
         {
            BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
            elementStack.push(child);
         }
      }

      // finally append the new "ref" node to the root figure
      ((BasicSchemaElement) oldSchema.getRootElement()).add(refNode);

      // the content model now conforms to this schema. We get the root element from the old schema and install it as 
      // the root of this schema, then empty the old schema. We also remember the original schema version of the 
      // migrated content.
      originalVersion = oldSchema.getOriginalVersion();
      setRootElement(oldSchema.getRootElement(), false);
      oldSchema.setRootElement(null, false);
	}
}
