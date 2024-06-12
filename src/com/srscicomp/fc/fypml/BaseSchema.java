package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.ISimpleXMLElement;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>BaseSchema</code> is the encapsulation of the <em>DataNav/Phyplot</em> XML schema in effect just prior to the 
 * introduction of schema versioning, which began with <em>Phyplot</em> version 0.7.0. [<em>DataNav</em> 3.0 replaced 
 * <em>Phyplot</em> in 2008; the last version of <em>Phyplot</em> produced was 2.1.2 (15Jan2008).] All <em>Phyplot</em> 
 * XML documents generated prior to version 0.7.0 did not include version info. Beginning with version 0.7.0, the XML 
 * document includes a simple processing instruction that contains both the program version number and the schema 
 * version number.
 * 
 * <p><code>BaseSchema</code> serves as the base class for all future <em>DataNav</em> schemas. It is part of the 
 * framework for handling migration from an older to a newer schema. It supports the use of a pull parser to read or 
 * write any <em>DataNav</em> XML document conforming to the baseline schema (version 0). It uses a single XML element 
 * class, <code>BasicSchemaElement</code>, to encapsulate all elements in the schema.</p>
 * 
 * @author sruffner
 */
class BaseSchema implements Schema0Constants, ISchema
{
   public String getNamespaceUri() { return("http://www.keck.ucsf.edu/phyplot/namespace"); }
   public int getVersion() { return(0); }
   public String getLastApplicationVersion() { return("0.6.4"); }

   /**
    * Get the schema version number of the XML content from which this schema instance was populated. During migration,
    * each schema class should preserve the original version of its schema predecessor in the protected field
    * <code>originalVersion</code>. It should not be necessary to override this method in subclasses.
    * @return Original schema version assigned to the content prior to migration.
    */
   public int getOriginalVersion() { return(originalVersion); }

	/**
	 * The set of all elements supported in <code>BaseSchema</code>. This is a map keyed by the element's tag name. The 
    * value retrieved is a <code>SchemaElementInfo</code> object that provides some schema validation information for 
    * the corresponding element.
	 */
	private static Map<String, SchemaElementInfo> elementMap = null;

	static 
	{
		elementMap = new HashMap<String, SchemaElementInfo>();

		String[] rootAttrs = new String[] {A_FONT, A_PSFONT, A_SUBSTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, 
			A_STROKEWIDTH, A_STROKECOLOR, A_SYMBOL, A_SYMBOLSIZE, A_CAP, A_CAPSIZE, A_MID, 
			A_SPACER, A_LABELOFFSET, A_PERLOGINTV, A_DIR, A_LEN, A_FMT, A_GAP};
		elementMap.put( EL_FYP, 
			new SchemaElementInfo( false, new String[] {EL_FIGURE}, rootAttrs, rootAttrs ) );

		elementMap.put( EL_FIGURE, 
			new SchemaElementInfo( false, 
				new String[] {EL_LABEL, EL_LINE, EL_GRAPH}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
					           A_BORDER, A_TITLE, A_LOC, A_WIDTH, A_HEIGHT}, 
				new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

		elementMap.put( EL_GRAPH, 
			new SchemaElementInfo( false, 
				new String[] {EL_AXIS, EL_LEGEND, EL_CALIB, EL_LABEL, EL_LINE, EL_GRAPH, EL_POINTSET, EL_FUNCTION, EL_SERIES}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
					           A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, A_SHOWGRID, A_CLIP, A_LOC, A_WIDTH, A_HEIGHT}, 
				new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

		elementMap.put( EL_AXIS, 
			new SchemaElementInfo( false, 
				new String[] {EL_TICKS, EL_LABEL, EL_LINE}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_HIDE, A_UNITS, A_SPACER, A_TITLE, A_LABELOFFSET, A_START, A_END}, 
				new String[] {A_START, A_END} ));

		elementMap.put( EL_TICKS, 
			new SchemaElementInfo( false, 
				new String[] {}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_START, A_END, A_PERLOGINTV, A_DIR, A_LEN, A_FMT, A_GAP, A_INTV}, 
				new String[] {A_INTV} ));

		elementMap.put( EL_CALIB, 
			new SchemaElementInfo( false, 
				new String[] {EL_LABEL}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_HORIZ, A_AUTO, A_CAP, A_CAPSIZE, A_LOC, A_LEN}, 
				new String[] {A_LOC, A_LEN} ));

		elementMap.put( EL_LEGEND, 
			new SchemaElementInfo( false, 
				new String[] {}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_ROTATE, A_SPACER, A_SYMBOLSIZE, A_LEN, A_MID, A_LOC}, 
				new String[] {A_LOC} ));

		elementMap.put( EL_LABEL, 
			new SchemaElementInfo( false, 
				new String[] {}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_ROTATE, A_HALIGN, A_VALIGN, A_LOC, A_TITLE}, 
				new String[] {A_LOC, A_TITLE} ));

		elementMap.put( EL_LINE, 
			new SchemaElementInfo( false, 
				new String[] {EL_LABEL, EL_LINE}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_P0CAP, A_P0CAPSIZE, A_P1CAP, A_P1CAPSIZE, A_MIDCAP, A_MIDCAPSIZE, A_LINETYPE, A_P0, A_P1}, 
				new String[] {A_P0, A_P1} ));

		elementMap.put( EL_POINTSET, 
			new SchemaElementInfo( true, 
				new String[] {}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SYMBOL, A_SYMBOLSIZE, A_HIDEERRORS, A_CAP, A_CAPSIZE}, 
				new String[] {} ));

		elementMap.put( EL_SERIES, 
			new SchemaElementInfo( true, 
				new String[] {}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SYMBOL, A_SYMBOLSIZE, A_HIDEERRORS, A_CAP, A_CAPSIZE, 
								  A_X0, A_DX, A_TYPE, A_BARWIDTH, A_BASELINE, A_FILLED}, 
				new String[] {} ));

		elementMap.put( EL_FUNCTION, 
			new SchemaElementInfo( true, 
				new String[] {}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SYMBOL, A_SYMBOLSIZE, A_X0, A_X1, A_DX}, 
				new String[] {} ));
	}

	/**
	 * Construct a <i>DataNav</i> figure XML content model conforming to base schema version 0. The model is initially
	 * empty (null root), and the original version is set equal to the value returned by <code>getVersion()</code>.
	 */
	public BaseSchema()
	{
	   root = null;
	   originalVersion = getVersion();
	}
	
	/** The root element of the DataNav XML content conforming to this schema version. */
	private BasicSchemaElement root;

	/** 
	 * The original version of the XML content fragment from which this schema instance was prepared. At construction,
	 * this is set to the schema instance's own version, but during migration it is set to the original version 
	 * associated with the migrated schema instance.
	 */
	protected int originalVersion;
	
	/**
	 * With the introduction of schema versioning, documents generated by <em>DataNav</em> contain a processing 
	 * instruction of the form "root appVersion=M schemaVersion=N", where root is the tag name for <em>DataNav</em>'s 
	 * root element, N is the schema version number, and M is application version.  This method returns that 
	 * instruction, using the latest application version that employed the schema.  However, for <code>BaseSchema</code>, 
	 * it returns <code>null</code>, since documents created prior to schema versioning did not have a processing 
	 * instruction.
	 * <p>This method should be sufficient for all future historical schemas, unless the format of the processing 
	 * instruction should change.</p>
	 * @see com.srscicomp.common.xml.ISimpleXMLContent#getProcessingInstruction()
	 */
	public String getProcessingInstruction()
	{
		if(getVersion() == 0) return(null);
		else return(EL_FYP + " appVersion=\"" + getLastApplicationVersion() + "\" schemaVersion=\"" + getVersion() + "\"");
	}

	public boolean isSupportedElementTag(String elTag) { return(elementMap.containsKey(elTag)); }

	/**
	 * This method first checks that the specified element tag is supported in this schema. If not, an exception is 
	 * thrown. Otherwise, it returns an instance of <code>BasicSchemaElement</code> conforming to this schema.
	 * <p>This implementation should be adequate for all future schemas so long as <code>BasicSchemaElement</code> can be
    * used to encapsulate all elements and future schema classes appropriately override the schema-specific methods in 
    * interface <code>ISchema</code>.</p>
	 * @see com.srscicomp.common.xml.ISimpleXMLContent#createElement(String)
	 */
	public ISimpleXMLElement createElement(String tag) throws XMLException
	{
		// make sure element tag is recognized in this schema!
		if(!isSupportedElementTag(tag))
			throw new XMLException("XML tag <" + tag + "> not recognized in schema ver " + getVersion());
		
		return(new BasicSchemaElement(tag, this));
	}

   public  String getRootTag() { return(EL_FYP); }
   
	public ISimpleXMLElement getRootElement() { return(root); }

	/**
    * This method first verifies that the candidate root node extends <code>BasicSchemaElement</code> and possesses the 
    * root tag required in this schema. If so, it validates the node if required, then makes it the root node.
    * <p>This implementation should be adequate for all future schemas so long as <code>isValidRootTag(String)</code> 
    * is overridden in schema implementations in which the identity of the root node changes.</p>
	 * @see com.srscicomp.common.xml.ISimpleXMLContent#setRootElement(ISimpleXMLElement, boolean)
	 */
	public void setRootElement(ISimpleXMLElement root, boolean validate) throws XMLException
	{
		// new root is null, so set it without validation 
		if(root == null)
		{
			this.root = null;
			return;
		}

		// the specified element must be valid as a root element of our XML content model 
		if(!(root instanceof BasicSchemaElement) || !isValidRootTag(root.getTag()))
			throw new XMLException("Invalid root node! (tag = " + root.getTag() + ")");

		// if requested, validate new root and its descendants BEFORE installing it as root
		if(validate) root.validate(true);
		this.root = (BasicSchemaElement) root;
	}

	public void validate() throws XMLException
	{
		if(root == null) throw new XMLException( "Null root node" );
		root.validate(true);
	}

	public SchemaElementInfo getSchemaElementInfo(String elTag) { return((SchemaElementInfo) elementMap.get(elTag)); }

   /**
    * Is the specified element tag correct for the root element in this schema? The root tag in <code>BaseSchema</code> 
    * is "fyp". Subsequent schema implementations must override this method if the root tag changes!
    * @param tag The candidate element tag for the schema's root node.
    * @return <code>True</code> iff the tag is valid for the root node in this schema.
    */
   boolean isValidRootTag(String tag) { return(EL_FYP.equals(tag)); }
   
   /**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema.  Here is a 
	 * summary of the rules enforced for attributes defined in schema version 0:
	 * <ul>
	 * 	<li>"font": Must be two non-empty string tokens separated by a single comma. The tokens can be anything as 
    *    long as they are not all whitespace; we do NOT check whether a token represents a valid font name, since the 
    *    set of all fonts is infinite!</li>
	 * 	<li>Any color attribute value must be a 6-digit hexadecimal string. The two color attributes in 
    *    <code>BaseSchema</code> are "fillColor" and "strokeColor".</li>
	 * 	<li>The attributes "p0", "p1", and "loc" are point-valued attributes, where the x and y coords are "measured 
    *    values", ie, floating-pt numbers with attached units "in", "cm", "mm", "pt", "%" or "u". Note that the 
    *    coordinates in the "loc" attribute of a "figure" element are restricted to non-relative units (ie, "%" and "u" 
    *    not allowed).</li>
	 * 	<li>The "len" attribute is an unrestricted floating-point value for a "calib" element; otherwise, it is a 
    *    nonnegative measure restricted to non-relative units.</li>
	 * 	<li>A boolean attribute value can only be one of two strings: "true" or "false".  The boolean attributes in 
	 * 	<code>BaseSchema</code> include "hideErrors", "horiz", "auto", "mid", "hide", "showGrid", "clip", and 
    *    "filled".</li>
	 * 	<li>A string token attribute can take on any non-null string value. The only string token attributes in 
	 * 	<code>BaseSchema</code> are "title" and "units".</li>
	 * 	<li>A float attribute is parsable as a floating-point number, and may be restricted to a specific range. 
	 * 	Unrestricted float attributes in <code>BaseSchema</code> include "start", "end", "intv", "x0", "x1", "dx", and 
	 * 	"baseline".  The "rotate" attribute is restricted to [-180..180], while the "barWidth" attribute can be any 
    *    floating-pt number &gt;= 0.</li>
	 * 	<li>The "lineType" attribute is a list of 1-6 integers, separated by whitespace and each &gt;=1, or it may 
    *    take on any of the "synonyms" in <code>Schema0Constants.LINETYPE_SYNONYMS</code>.</li>
	 * 	<li>The "perLogIntv" attribute is a list of 1-9 integers, separated by whitespace and each restricted to 
    *    [1..9], or it may take on any of the "synonyms" in <code>Schema0Constants.PERLOGINTV_SYNONYMS</code>.</li>
	 * 	<li>The following attributes are enumerated attributes that must take on a value from a multi-choice list:
    *    "psFont", from <code>Schema0Constants.PSFONT_CHOICES</code>; "substfont", from <code>SUBSTFONT_CHOICES</code>; 
    *    "fontStyle", from <code>FONTSTYLE_CHOICES</code>; "align", from <code>HALIGN_CHOICES</code>; "vAlign", from 
    *    <code>VALIGN_CHOICES</code>; "symbol", from <code>SYMBOL_CHOICES</code>; "dir", from <code>DIR_CHOICES</code>;
    *    "fmt", from <code>FMT_CHOICES</code>; "layout", from <code>GRAPH_LAYOUT_CHOICES</code>.</li>
	 * 	<li>The enumerated attributes "cap", "p0Cap", "p1Cap" and "midCap" each must take on one of the values in 
    *    <code>Schema0Constants.ADORN_CHOICES</code>.</li>
	 * 	<li>The attribute "type" is an enumerated attribute that specifies the type of a graph or data series element. 
    *    In a "graph" element, it must take on one of the values in <code>Schema0Constants.GRAPH_TYPE_CHOICES</code>. In
    *    a "series" element, it must take on a value in <code>Schema0Constants.SERIES_TYPE_CHOICES</code>.</li>
	 *		<li>The "width" and "height" propertier are non-negative measure attributes. In a graph element they can be 
    *    "measured" in relative units; otherwise, they are restricted to the non-relative units.</li>
	 *		<li>All of the following are non-negative measure attributes restricted to non-relative units: "fontSize",
    *    "strokeWidth", "symbolSize", "capSize", "p0CapSize", "p1CapSize", "midCapSize", "spacer", "labelOffset, "gap", 
    *    "len", and "border".</li>
	 * </ul>
	 * @see Schema0Constants
	 * @see ISchema#isValidAttributeValue(BasicSchemaElement,String,String)
	 */
	public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
		// the tag name of the owner element
		String elTag = e.getTag();

		if(A_FONT.equals(attr))
		{
			// the string must contain one and only one comma
			int commaPos = value.indexOf(',');
			if(commaPos < 0 || (commaPos != value.lastIndexOf(','))) return(false);

			// the comma-separated substrings cannot be all whitespace
			if(value.substring(0,commaPos).trim().length() == 0) return(false);
			if(value.substring(commaPos+1).trim().length() == 0) return(false);

			return(true);
		}
		else if(A_FILLCOLOR.equals(attr) || A_STROKECOLOR.equals(attr))
			return(isValidColorAttributeValue(value));
		else if(A_LOC.equals(attr) || A_P0.equals(attr) || A_P1.equals(attr))
		{
			// the loc attr on a figure cannot use relative units
			boolean allowRelative = !(A_LOC.equals(attr) && EL_FIGURE.equals(elTag));
			return(isValidMeasurePointAttributeValue(value, allowRelative));
		}
		else if(A_LEN.equals(attr))
		{
			// for a calib element, this attribute is an unrestricted float.  In all other contexts it is a nonnegative 
			// non-relative measure.
			return( EL_CALIB.equals(elTag) ? isValidFloatAttributeValue(value, -Double.MAX_VALUE, Double.MAX_VALUE) : 
				                              isValidMeasureAttributeValue(value, false, false) );
		}
		else if(A_HIDEERRORS.equals(attr) || A_HORIZ.equals(attr) || A_AUTO.equals(attr) || A_MID.equals(attr) || 
		        A_HIDE.equals(attr) || A_SHOWGRID.equals(attr) || A_CLIP.equals(attr) || A_FILLED.equals(attr))
		{
			// boolean attributes can only be "true" or "false"!
			return("true".equals(value) || "false".equals(value));
		}
		else if(A_TITLE.equals(attr) || A_UNITS.equals(attr))
		{
			// string token attributes can be any string, as long as it is not null
			return(value != null);
		}
		else if(A_ROTATE.equals(attr) || A_START.equals(attr) || A_END.equals(attr) || A_INTV.equals(attr) || 
			     A_X0.equals(attr) || A_X1.equals(attr) || A_DX.equals(attr) || A_BARWIDTH.equals(attr) || 
			     A_BASELINE.equals(attr))
		{
			// floating-pt valued attributes, some with restricted ranges
			double minVal = -Double.MAX_VALUE;
			double maxVal = Double.MAX_VALUE;
			if(A_ROTATE.equals(attr)) 
			{
				minVal = -180;
				maxVal = 180;
			}
			else if(A_BARWIDTH.equals(attr))
				minVal = 0;

			return(isValidFloatAttributeValue(value, minVal, maxVal));
		}
		else if(A_LINETYPE.equals(attr))
		{
			// the lineType attribute is a list of 1-6 integers in [1..max_int], or one of the recognized synonyms
			return(isValidIntListAttributeValue(value, 1, 6, 1, Integer.MAX_VALUE, true, LINETYPE_SYNONYMS));
		}
		else if(A_PERLOGINTV.equals(attr))
		{
			// the perLogIntv attribute is a list of 1-9 integers in [1..9], or one of the recognized synonyms.  No 
			// integer can appear more than once in the list.
			return(isValidIntListAttributeValue(value, 1, 9, 1, 9, false, PERLOGINTV_SYNONYMS));
		}
		else if(A_PSFONT.equals(attr))
			return(isValidEnumAttributeValue(value, PSFONT_CHOICES));
		else if(A_SUBSTFONT.equals(attr))
			return(isValidEnumAttributeValue(value, SUBSTFONT_CHOICES));
		else if(A_FONTSTYLE.equals(attr))
			return(isValidEnumAttributeValue(value, FONTSTYLE_CHOICES));
		else if(A_HALIGN.equals(attr))
			return(isValidEnumAttributeValue(value, HALIGN_CHOICES));
		else if(A_VALIGN.equals(attr))
			return(isValidEnumAttributeValue(value, VALIGN_CHOICES));
		else if(A_SYMBOL.equals(attr))
			return(isValidEnumAttributeValue(value, SYMBOL_CHOICES));
		else if(A_CAP.equals(attr) || A_P0CAP.equals(attr) || A_P1CAP.equals(attr) || A_MIDCAP.equals(attr))
			return(isValidEnumAttributeValue(value, ADORN_CHOICES));
		else if(A_DIR.equals(attr))
			return(isValidEnumAttributeValue(value, DIR_CHOICES));
		else if(A_FMT.equals(attr))
			return(isValidEnumAttributeValue(value, FMT_CHOICES));
		else if(A_TYPE.equals(attr))
		{
			// handle graph type or data series type attribute
			String[] choices = EL_GRAPH.equals(elTag) ? GRAPH_TYPE_CHOICES : SERIES_TYPE_CHOICES;
			return(isValidEnumAttributeValue(value, choices));
		}
		else if(A_LAYOUT.equals(attr))
			return(isValidEnumAttributeValue(value, GRAPH_LAYOUT_CHOICES));
		else if(A_FONTSIZE.equals(attr) || A_STROKEWIDTH.equals(attr) || A_WIDTH.equals(attr) || A_HEIGHT.equals(attr) || 
		        A_BORDER.equals(attr) || A_SYMBOLSIZE.equals(attr) || A_CAPSIZE.equals(attr) || A_P0CAPSIZE.equals(attr) || 
		        A_P1CAPSIZE.equals(attr) || A_MIDCAPSIZE.equals(attr) || A_SPACER.equals(attr) || A_GAP.equals(attr) || 
		        A_LABELOFFSET.equals(attr))
		{
			// all of these attributes are non-negative, non-relative units -- except that the width and height attributes 
			// of a graph element can have relative units.
			boolean allowRelative = EL_GRAPH.equals(elTag) && (A_WIDTH.equals(attr) || A_HEIGHT.equals(attr));
			return(isValidMeasureAttributeValue(value, false, allowRelative));
		}

		// if we got here, then attribute name is not recognized in this schema
		return(false);
	}

	/**
	 * Verify that the specified value is among the specified choice set for an enumerated attribute.
	 * @param value The test value.
	 * @param choices The set of possible values for an enumerated attribute.
	 * @return <code>True</code> if the test value is found in the choice set.
	 */
	boolean isValidEnumAttributeValue(String value, String[] choices)
	{
		if(choices != null) for(int i=0; i<choices.length; i++) 
		{
			if(choices[i].equals(value)) return(true);
		}
		return(false);
	}

	/**
	 * Verify that the specified value is valid for a float attribute in this schema. A float attribute must be parsable 
	 * as a <code>java.lang.Double</code> and must fall within the specified range.
	 * @param value The test value.
	 * @param minVal The minimum allowed floating-pt value for the attribute.
	 * @param maxVal The maximum allowed floating-pt value for the attribute.
	 * @return <code>True</code> if the test value is a valid for a float attribute, as described.
	 */
	boolean isValidFloatAttributeValue(String value, double minVal, double maxVal)
	{
		boolean ok = true;
		try 
		{
			double d = Double.parseDouble(value);
			ok = (d >= minVal) && (d <= maxVal);
		}
		catch(NumberFormatException nfe) { ok = false; }
		return(ok);
	}

	/**
	 * Verify that the specified value is valid for a color attribute. In this schema, a color attribute is a 6-digit
    * hexadecimal string "RRGGBB" representing an RGB color.
	 * @param  value The test value.
	 * @return <code>True</code> if the test value is valid for a color attribute, as described.
	 */
	boolean isValidColorAttributeValue(String value) { return(value != null && value.matches(COLOR_REGEX)); }

	/**
	 * Verify that the specified value is valid for a measure attribute. In this schema, a measure attribute is a
    * floating-pt number and accompanying units, with no intervening whitespace -- such as "0.01in" or "1pt". The 
    * recognized unit tokens are "in", "cm", "mm", "pt", "%", and "u", the last representing the native coordinates 
    * assigned to the data window of a <em>DataNav</em> graph element.  
	 * @param value The test value.
	 * @param allowNeg If set, then the measure can be negative.
	 * @param allowRelative If set, then all supported unit tokens are acceptable; else, the "%" and "u" units are not 
	 * allowed. A number of attributes in this schema are non-negative and must be in physical units.
	 * @return True if the test value is valid for a measure attribute, as described.
	 */
	boolean isValidMeasureAttributeValue(String value, boolean allowNeg, boolean allowRelative)
	{
	   return(isValidMeasureAttributeValue(value, allowNeg, allowRelative, allowRelative));
	}

	/**
	 * Verify that the specified value is valid for a measure attribute. 
	 * @param value The test value.
	 * @param allowNeg If set, then the measure can be negative.
	 * @param allowPct If set, then "%" units are permitted.
	 * @param allowUser If set, then "u" units are permitted.
	 * @return True if test value is valid for a measure attribute and satisfies the above constraints.
	 * @see {@link #isValidMeasureAttributeValue(String, boolean, boolean)}
	 */
	boolean isValidMeasureAttributeValue(String value, boolean allowNeg, boolean allowPct, boolean allowUser)
	{
	   boolean ok = (value!=null) && value.matches(MEASURE_REGEX);
	   if(ok) ok = (allowPct || (!value.endsWith("%"))) && (allowUser || (!value.endsWith("u")));
      if(ok && !allowNeg) ok = !(value.trim().indexOf('-') == 0);
      return(ok);
	}
	
	/**
	 * Return the numeric part of the specified value for a measure attribute. 
	 * @param attrVal The test value.
	 * @return The numeric part of the measure.
	 * @throws IllegalArgumentException if argument is not a valid value for a measure attribute.
	 */
	double getMeasureFor(String attrVal)
	{
		if(attrVal==null || !attrVal.matches(MEASURE_REGEX))
			throw new IllegalArgumentException("Expected valid value for a floating-pt measure!");
		for(int i=0; i < MEASURE_TOKENS.length; i++) 
		{
			int unitIndex = attrVal.indexOf(MEASURE_TOKENS[i]);
			if(unitIndex > 0)
			{
				return(Double.parseDouble(attrVal.substring(0,unitIndex)));
			}
		}
		throw new IllegalArgumentException( "Expected valid value for a floating-pt measure!" );
	}

	/**
	 * Verify that the specified value is valid for a measured point attribute. A measured point attribute is simply a 
    * pair of measure attributes, separated by whitespace, representing the (x,y) coordinates of a point. Each 
    * coordinate must satisfy the format restrictions of a measure attribute.
	 * 
	 * @param value The test value.
	 * @param allowRelative If set, then all supported unit tokens are acceptable for either coordinate; else, the 
	 * "%" and "u" units are not allowed.
	 * @return <code>True</code> if the test value is valid for a measured point attribute, as described.
    * @see BaseSchema#isValidMeasureAttributeValue(String,boolean,boolean)
	 */
	boolean isValidMeasurePointAttributeValue(String value, boolean allowRelative)
	{
		StringTokenizer st = new StringTokenizer(value);
		if(st.countTokens() != 2) return(false);

		String xStr = st.nextToken();
		String yStr = st.nextToken();

		return(isValidMeasureAttributeValue(xStr, true, allowRelative) 
		      && isValidMeasureAttributeValue(yStr, true, allowRelative));
	}

	/**
	 * Verify that the specified value is valid for an integer-list attribute. An integer list attribute can be either a 
    * list of integers or any one of a recognized set of "synonyms". A particular attribute places restrictions on the 
    * minimum and maximum number of integers in the list, the range of legal integer values, and the set of synonyms. A 
    * valid value must either be one of the synonyms, or it must be parsable as a list of integers satisfying the 
    * specified constraints.
	 * @param value The test value.
	 * @param nMin The minimum number of integers in the list.
	 * @param nMax The maximum number of integers in the list.
	 * @param minVal The minimum allowed value for any integer in the list.
	 * @param maxVal The maximum allowed value for any integer in the list.
	 * @param allowRepeats If unset, then the same integer cannot appear more than once in the list.
	 * @param synonyms The set of recognized synonyms that "stand in" for some commonly used instance of the integer 
	 * list attribute. Should be null or empty if there are no such synonyms.
	 * @return True if test value is valid for an integer list attribute, as described.
	 */
	boolean isValidIntListAttributeValue(String value, int nMin, int nMax, int minVal, int maxVal, boolean allowRepeats, 
		String[] synonyms)
	{
		// first, check to see if test value is one of the recognized synonyms
		if(isValidEnumAttributeValue(value, synonyms))
			return(true);

		// otherwise, make sure value is a list of whitespace-separated integers.  Ensure that restrictions on list 
		// size and integer values are satisfied
		StringTokenizer st = new StringTokenizer(value);
		int nTokens = st.countTokens();
		if(nTokens < nMin || nTokens > nMax) return(false);

		// if repeats are not allowed, keep track of what we've parsed thus far
		boolean noRepeats = !allowRepeats;
		Map<String, String> valuesThusFar = null;
		if(noRepeats) valuesThusFar = new HashMap<String, String>();
		
		boolean ok = true;
		try
		{
	      for(int i=0; ok && i<nTokens; i++)
	      {
            String token = st.nextToken();
            if(noRepeats)
            {
               if(valuesThusFar.containsKey(token)) ok = false;
               else valuesThusFar.put(token, null);
            }
            if(ok)
            {
               int iVal = Integer.parseInt(token);
               ok = (iVal >= minVal && iVal <= maxVal);
            }
	      }
		}
		catch(NumberFormatException nfe) { ok = false; }

		return(ok);
	}

	/**
	 * In schema version 0, the only element that has required child elements is the "graph" element, which requires 
    * that the first two children define its axes.
	 * 
	 * @see ISchema#hasRequiredChildren(BasicSchemaElement)
	 */
	@SuppressWarnings("rawtypes")
	public boolean hasRequiredChildren(BasicSchemaElement e)
	{
		String elTag = e.getTag();
		if(!EL_GRAPH.equals(elTag)) return(true);

		// graph element must have at least two children, and these two must be axis elements
		List children = e.getElementContent();
		if(children.size() < 2) return(false);
		String tag0 = ((BasicSchemaElement) children.get(0)).getTag();
		String tag1 = ((BasicSchemaElement) children.get(1)).getTag();
		return(EL_AXIS.equals(tag0) && EL_AXIS.equals(tag1));
	}

	/**
	 * In schema version 0, most elements place no restrictions on the specific positioning of children within their 
	 * children lists, except:
	 * <ul>
	 * 	<li>An "axis" element: May contain 0-4 tick sets, and these elements must appear first in the child list.</li>
	 * 	<li>A "graph" element: First two children MUST exist and MUST be axis elements. May contain 1 legend element; 
    *    if present, it MUST be the third child.</li>
	 * </ul>
	 * 
	 * @see ISchema#isValidChildAtIndex(BasicSchemaElement,String,int)
	 */
	@SuppressWarnings("rawtypes")
	public boolean isValidChildAtIndex(BasicSchemaElement e, String childTag, int index)
	{
		// first, make sure that child is allowed at all -- this check is done for all elements
		String elTag = e.getTag();
		SchemaElementInfo eInfo = (SchemaElementInfo) getSchemaElementInfo(elTag);
		if(!eInfo.isChildAllowed(childTag)) return(false);

		// enforce restrictions on an axis element
		if(EL_AXIS.equals(elTag))
		{
			List existingChildren = e.getElementContent();

			// first, find out how many tick sets are already defined
			int nTicks = 0;
			for(int i=0; i<existingChildren.size(); i++)
			{
				BasicSchemaElement child = (BasicSchemaElement) existingChildren.get(i);
				if(EL_TICKS.equals(child.getTag())) ++nTicks;
			}

			// if child is a tick set, make sure it is among the first N children or will be inserted immediately 
			// after those N children, where N is the # of tick sets already contained in the axis element.  If it is 
			// not a tick set, make sure it is located after any and all tick set children.
			if(EL_TICKS.equals(childTag))
				return((nTicks==MAX_TICKSETS) ? (index < nTicks) : (index <= nTicks));
			else 
				return(nTicks <= index);
		}

		// enforce restrictions on a graph element
		if(EL_GRAPH.equals(elTag))
		{
			List existingChildren = e.getElementContent();

			// a graph element only allows an axis as its first two children, and a legend (if present) must be the third 
			// element.  All other children can appear in any quantity and in any order, but they must appear after the 
			// axes and legend.
			if(EL_AXIS.equals(childTag))
				return(index == 0 || index == 1);
			else if(EL_LEGEND.equals(childTag))
				return(index == 2);
			else
			{
				if(existingChildren.size() < 2) return(false);
				int minIndex = 2;
				if(existingChildren.size() > 2 && EL_LEGEND.equals(((BasicSchemaElement) existingChildren.get(2)).getTag()))
					++minIndex;
				return(minIndex <= index);
			}
		}

		return(true);
	}

	/**
	 * In schema version 0, only the data set elements (function, series, and pointSet) contain text content. The text
	 * content of a function does not require validation, but the content of a series or pointSet follow a very specific
	 * format. 
	 * <p>The data points comprising a pointSet are defined in the text content as an ordered list of comma-separated 
	 * "data point tuples". Each such data point tuple is a whitespace-delimited list of 2 to 6 floating-point numbers
	 * "x y [ye ye_pos xe xe_pos]". Empty content is OK.</p>
	 * <p>Similarly, the samples comprising a data series are defined in the text content as a list of comma-separated 
	 * "sample datum tuples". Each such tuple is a whitespace-delimited list of 1 to 3 floating-point numbers
	 * "y [ye ye_pos]". Empty content is OK.</p>
	 * <p>If called on an element that does not admit text content, the method returns <code>false</code>.</p>
	 * @see ISchema#isValidTextContent(BasicSchemaElement,String)
	 */
	public boolean isValidTextContent(BasicSchemaElement e, String text)
	{
		String elTag = e.getTag();

		// make sure element admits text content
		if(!e.allowsTextContent()) return(false);

		// validate content of a pointSet or series element
		if(EL_POINTSET.equals(elTag) || EL_SERIES.equals(elTag))
		{
			if(text == null || text.length() == 0) return(true);

			int nMinTupleSize = EL_POINTSET.equals(elTag) ? 2 : 1;
			int nMaxTupleSize = EL_POINTSET.equals(elTag) ? 6 : 3;

			StringTokenizer st = new StringTokenizer(text, ",");
			while(st.hasMoreTokens())
			{
				String tuple = st.nextToken().trim();

				// 29apr2014: Allow for possibility that text content ENDS with a comma followed only by whitespace, in
				// which case we might get a zero-length tuple here.
				if(tuple.length() == 0) return(!st.hasMoreTokens());

				StringTokenizer tupleTokenizer = new StringTokenizer(tuple);
				int nTokens = tupleTokenizer.countTokens();
				if(nTokens < nMinTupleSize || nTokens > nMaxTupleSize) return(false);

				try
				{
				   while(tupleTokenizer.hasMoreTokens()) Double.parseDouble(tupleTokenizer.nextToken());
				} 
				catch(NumberFormatException nfe) { return(false); }
			}
		}

		return(true);
	}

	public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
	{
		throw new XMLException("Schema version 0 does not support migration!");
	}
}
