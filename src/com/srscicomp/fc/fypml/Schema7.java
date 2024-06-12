package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import com.srscicomp.common.util.Utilities;
import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema7</code> is the encapsulation of the <em>DataNav/Phyplot</em> XML schema version 7. It extends 
 * <code>Schema6</code> and includes support for migrating schema version 6 documents to version 6. Introduced in 
 * <em>Phyplot</em> 2.0.0, the sundry schema changes in <code>Schema7</code>, for the most part, involve changes in 
 * element attribute names or allowed values. No element classes were added or removed. The most important changes are 
 * listed below. For a complete list, see <code>migrateFromPreviousSchema(ISchema)</code>. 
 * <ul>
 *    <li>Eliminated the so-called "document-level attribute defaults" defined on the "figure" element.</li>
 *    <li>The "lineType" attribute was replaced by "strokePat", and the "hidden" synonym no longer exists. The same 
 *    effect is achieved by setting the element's "hide" attribute to "true" or its stroke width to "0in", depending on 
 *    the element class. Also, "strokePat" is inheritable like other style properties and is now defined on the "figure" 
 *    and "graph" nodes.</li>
 *    <li>The names of the supported adornments are now all in lowercase letters, while the spelling is unchanged. In 
 *    the previous schema, some adornment names had capitals -- such as "fillThinArrow".</li>
 *    <li>The adornment shape choice "none" is no longer recognized. To achieve the same effect, the shape size is set 
 *    to zero. This change affects the "cap" and "capSize" attributes on "calib" and "ebar" elements, and the "type" and 
 *    "size" properties on "shape" and "symbol" elements. NOTE that "none" was the default value for "type" on "symbol" 
 *    elements in the prior schema version, while the default "cap" value was a document-level default. The default 
 *    type for a "shape" element was "box".</li>
 *    <li>The "legend" child is now required on a "graph" element. It is always the fifth child. During migration, if 
 *    any graph lacks a legend, one is provided. The legend's new "hide" attribute will be set so that it does not 
 *    change the graph's rendered appearance.</li>
 *    <li>An "axis" element no longer admits "label" and "line" elements. These are simply dropped -- which means that 
 *    a <code>Schema6</code> document migrated to <code>Schema7</code> may have visible differences from the original 
 *    version. Since these children were very rarely used on an axis, this was deemed acceptable.</li>
 * </ul>
 * <p>BUG FIX (as of v2.0.4, dtd 07Aug2007): When Schema7 was introduced, the adornment type "none" was removed, all 
 * type names were changed to their lowercase form (some had interior capitals in the prior schema), and the colon 
 * character (':') was removed from all adornment names that used it ("oval1:2" became "oval12", etc.). I forgot to 
 * remove the colon character from the relevant names, both in <code>ADORN7_CHOICES</code> and in 
 * <code>migrateFromPreviousSchema()</code>. Fixed as of <em>Phyplot</em> v2.0.4; did not change schema version.</p>
 * 
 * <p><strong>This is the last schema version recognized by <em>Phyplot</em>. The last <em>Phyplot</em> application 
 * version to use this schema is 2.1.3 <em>DataNav</em> superceded <em>Phyplot</em> in 2008; application versioning for
 * <em>DataNav</em> started at 3.0.0.</strong></p>
 * @author 	sruffner
 */
class Schema7 extends Schema6
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/phyplot/namespace/schema7"); }
   @Override public int getVersion() { return(7); }
   @Override public String getLastApplicationVersion() { return("2.1.3"); }

   /** The "altFont" attr replaces "substfont" and is now defined on all elements that have font-related properties. */
   final static String A_ALTFONT = "altFont";

   /** The "psFont" attr replaces "psfont" -- renamed with interior capital for consistency's sake. */
   final static String A_PSFONT_V7 = "psFont";

   /** The "strokePat" attr replaces "lineType", is inheritable, and does NOT recognize the value "hidden". */
   final static String A_STROKEPAT = "strokePat";

   /** A new stroke pattern synonym, representing a dash followed by two dots. */
   final static String STROKEPAT_DASHDOTDOT = "dashdotdot";

   /** Recognized synonyms for the "strokePat" attribute. */
   final static String[] STROKEPAT_SYNONYMS = {
      LINETYPE_SOLID, LINETYPE_DOTTED, LINETYPE_DASHED, LINETYPE_DASHDOT, STROKEPAT_DASHDOTDOT};

   /** Minimum number of entries in the stroke pattern dash-gap array. */
   final static int MINSTRKPATARRAYLEN = 1;

   /** Maximum number of entries in the stroke pattern dash-gap array. */
   final static int MAXSTRKPATARRAYLEN = 6;

   /** The minimum length of a dash or gap, in 0.1 line widths. */
   final static int MINSTRKPATDASHLEN = 1;

   /** The maximum length of a dash or gap, in 0.1 line widths. */
   final static int MAXSTRKPATDASHLEN = 99;

   /** Boolean "legend" attr replaces "hide" on "function" and "trace" elements. */
   final static String A_LEGEND = "legend";

   /** The display "mode" of a data trace element; replaces "type" attribute. */
   final static String A_MODE = "mode";
   
   /** Boolean "primary" attr replaces "horiz" on a calibration bar element. */
   final static String A_PRIMARY = "primary";

   /** Minimum font size in typographical points -- for integer-valued "fontSize" property. */
   final static int MINFONTSIZE = 1;
   
   /** Maximum font size in typographical points -- for integer-valued "fontSize" property. */
   final static int MAXFONTSIZE = 99;
   
   /**
    * The names of all adornments supported in schema version 7. Same list as what was defined back in schema version 1, 
    * except that the "none" choice is no longer supported, and all adornment names are now entirely in lowercase 
    * letters.
    * <p>BUG FIX(as of v2.0.4): I forgot about several adornments that used to have ':' in their names. In schema 7, 
    * these colons are not present.</p>
    */
   final static String[] ADORN7_CHOICES;
   static
   {
      ADORN7_CHOICES = new String[ADORN1_CHOICES.length-1];
      for(int i=1; i<ADORN1_CHOICES.length; i++) ADORN7_CHOICES[i-1] = ADORN1_CHOICES[i].toLowerCase().replace(":", "");
   }

   // The data set format type names have (mostly) changed
   final static String SETFMT_PTS_V7 = "ptset";
   final static String SETFMT_SERIES_V7 = "series";
   final static String SETFMT_MSET_V7 = "mset";
   final static String SETFMT_MSERIES_V7 = "mseries";

   /** Possible values for a "set" element's "fmt" attribute. The names changed in this schema version. */
   final static String[] SETFMT_CHOICES_V7 = {SETFMT_PTS_V7, SETFMT_SERIES_V7, SETFMT_MSET_V7, SETFMT_MSERIES_V7};

   // One of the generic font names has changed.
   final static String ALTFONT_SERIF = SUBSTFONT_SERIF;
   final static String ALTFONT_SANSSERIF = "sanserif";
   final static String ALTFONT_MONOSPACE = SUBSTFONT_MONOSPACE;

   /** Possible values for the "altFont" attribute, formerly "substfont" ("sans-serif" is now "sanserif"). */
   final static String[] ALTFONT_CHOICES = {ALTFONT_SERIF, ALTFONT_SANSSERIF, ALTFONT_MONOSPACE};

   // 
   // Property defaults. During serialization to XML, any non-style property equal to its corresponding default need 
   // not be included in the XML output. During deserialization, if a property is not present in the output, it is 
   // automatically set to the relevant default value. These are needed for converting the XML schema document to the 
   // in-memory graphic model, which does not support implicit values except for style properties. All default values 
   // are specified as XML string values.
   //
   final static String DEFAULT_BORDER = "0in";
   final static String DEFAULT_ROTATE = "0";
   final static String DEFAULT_TITLE = "";
   final static String DEFAULT_GRAPH_TYPE = "cartesian";
   final static String DEFAULT_LAYOUT = "quad1";
   final static String DEFAULT_CLIP = "false";
   final static String DEFAULT_SYMBOL = "box";
   final static String DEFAULT_SYMBOL_SIZE = "0.1in";
   final static String DEFAULT_PRIMARY = "true";
   final static String DEFAULT_AUTO = "true";
   final static String DEFAULT_CALIB_CAP = "linedown";
   final static String DEFAULT_CAPSIZE = "0.2in";
   final static String DEFAULT_LEGEND = "true";
   final static String DEFAULT_X0 = "0";
   final static String DEFAULT_X1 = "10";
   final static String DEFAULT_DX = "1";
   final static String DEFAULT_SKIP = "1";
   final static String DEFAULT_MODE = "polyline";
   final static String DEFAULT_BARWIDTH = "0";
   final static String DEFAULT_BASELINE = "0";
   final static String DEFAULT_FILLED = "false";
   final static String DEFAULT_XYOFF = "0";
   final static String DEFAULT_AXIS_HIDE = "false";
   final static String DEFAULT_UNITS = "";
   final static String DEFAULT_AXIS_SPACER = "0.05in";
   final static String DEFAULT_LABELOFFSET = "0.5in";
   final static String DEFAULT_PERLOGINTV = "";
   final static String DEFAULT_TICKDIR = "out";
   final static String DEFAULT_TICKFMT = "int";
   final static String DEFAULT_TICKLEN = "0.06in";
   final static String DEFAULT_TICKGAP = "0.04in";
   final static String DEFAULT_LEGEND_SPACER = "0.25in";
   final static String DEFAULT_LEGEND_SIZE = "0in";
   final static String DEFAULT_LEGEND_LEN = "0.5in";
   final static String DEFAULT_LEGEND_HIDE = "true";
   final static String DEFAULT_MID = "false";
   final static String DEFAULT_GRIDLINE_HIDE = "true";
   final static String DEFAULT_EBAR_CAP = "bracket";
   final static String DEFAULT_EBAR_HIDE = "false";
   final static String DEFAULT_ALIGN = HALIGN_LEFT;
   final static String DEFAULT_VALIGN = VALIGN_BOTTOM;
   
   /**
	 * This element map contains <code>SchemaElementInfo</code> objects for each element that is new to this schema or 
    * has a different attribute set compared to the previous schema.
	 */
	private static Map<String, SchemaElementInfo> elementMap7 = null;

	static
	{
		elementMap7 = new HashMap<String, SchemaElementInfo>();

      // Document-level default attributes were scrapped. Changed some attribute names.
      // Added "strokePat" as a generally inheritable attribute.
      elementMap7.put( EL_FIGURE, 
            new SchemaElementInfo( false, 
               new String[] {EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH, EL_REF}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                  A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_BORDER, A_LOC, A_WIDTH, A_HEIGHT}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                  A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT} ));

      // Changed "psfont" to "psFont"; added "altFont" and "strokePat". Legend node is now required.
      elementMap7.put( EL_GRAPH, 
         new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH, 
                          EL_FUNCTION, EL_TRACE}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, A_CLIP, 
                          A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

      // Changed "psfont" to "psFont"; added "altFont"; "strokePat" replaces "lineType" (and no "hidden" stroke patn!); 
      // "legend" replaces "hide".
      elementMap7.put( EL_FUNCTION, 
         new SchemaElementInfo( true, 
            new String[] {EL_SYMBOL}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_LEGEND, A_X0, A_X1, A_DX}, 
            new String[] {} ));

      // Changed "psfont" to "psFont"; added "altFont"; "strokePat" replaces "lineType" (and no "hidden" stroke patn!);
      // "mode" replaces "type"; "legend" replaces "hide".
      // *** If "src" attribute (no longer required) is absent, then there is no data source linked to this trace! 
      // Otherwise, its form is as described in Schema6.
      elementMap7.put( EL_TRACE, 
            new SchemaElementInfo( false, 
                  new String[] {EL_SYMBOL, EL_EBAR}, 
                  new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                                A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_LEGEND, A_SKIP, A_MODE, A_BARWIDTH, A_BASELINE, 
                                A_FILLED, A_XOFF, A_YOFF, A_SRC}, 
                  new String[] {} ));
      
      // Axis element no longer allows arbitrary lines and labels; just 0-4 tick mark sets. Changed "psfont" to 
      // "psFont"; added "altFont". Former "document-level default" attributes "labelOffset" and "spacer" now have 
      // standard defaults ("0.5in" and "0in", respectively).
      elementMap7.put( EL_AXIS, 
            new SchemaElementInfo( false, 
               new String[] {EL_TICKS}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                             A_STROKECOLOR, A_HIDE, A_UNITS, A_TITLE, A_SPACER, A_LABELOFFSET, A_START, A_END}, 
               new String[] {A_START, A_END} ));

      // Changed "psfont" to "psFont"; added "altFont". Also, former "document-level defaulted" attributes "len"
      // ("0.06in"), "gap" ("0.04in"), "perLogIntv" (""), "dir" ("out"), and "fmt" ("int") now have standard defaults.
      elementMap7.put( EL_TICKS, 
            new SchemaElementInfo( false, 
               new String[] {}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                             A_STROKECOLOR, A_START, A_END, A_INTV, A_PERLOGINTV, A_DIR, A_LEN, A_FMT, A_GAP}, 
               new String[] {A_INTV} ));

      // Changed "psfont" to "psFont"; added "altFont". Former "doc-level defaulted" attributes "cap" and "capSize" now 
      // have standard defaults ("linedown" and "0.2in", respectively). "Cap" can no longer have the value "none". The 
      // boolean "horiz" attr renamed "primary".
      elementMap7.put( EL_CALIB, 
            new SchemaElementInfo( false, 
               new String[] {EL_LABEL}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                             A_STROKECOLOR, A_PRIMARY, A_AUTO, A_CAP, A_CAPSIZE, A_LOC, A_LEN}, 
               new String[] {A_LOC, A_LEN} ));

      // Changed "psfont" to "psFont"; added "altFont". Former "doc-level defaulted attrib "mid" is now a standard 
      // attribute (default = "true"). Got rid of stroking styles, since legend uses styles defined on the various 
      // trace/function elements represented. Added "hide" attribute (default = "true").
      elementMap7.put( EL_LEGEND, 
            new SchemaElementInfo( false, 
               new String[] {}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, 
                             A_ROTATE, A_SPACER, A_SIZE, A_LEN, A_MID, A_HIDE, A_LOC}, 
               new String[] {A_LOC} ));

      // Changed "psfont" to "psFont"; added "altFont".
      elementMap7.put( EL_LABEL, 
            new SchemaElementInfo( false, 
               new String[] {}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, 
                             A_ROTATE, A_HALIGN, A_VALIGN, A_LOC, A_TITLE}, 
               new String[] {A_LOC, A_TITLE} ));

      // Changed "psfont" to "psFont"; added "altFont". Also, "strokePat" replaces "lineType", and there is no "hidden" 
      // stroke pattern!
      elementMap7.put( EL_LINE, 
         new SchemaElementInfo( false, 
            new String[] {EL_LABEL, EL_LINE, EL_SHAPE}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECOLOR, A_STROKEPAT, A_P0, A_P1}, 
            new String[] {A_P0, A_P1} ));

      // Changed "psfont" to "psFont"; added "altFont". Attr "lineType" renamed "strokePat" (and there is no "hidden" 
      // stroke patn!). The "type" attr can no longer have the value "none"; must specify zero shape size instead.
      elementMap7.put( EL_SHAPE, 
         new SchemaElementInfo( false, 
            new String[] {EL_LABEL}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECOLOR, A_STROKEPAT, A_LOC, A_ROTATE, A_TYPE, A_SIZE, A_TITLE}, 
            new String[] {A_LOC} ));

      // The "lineType" attr replaced by "strokePat" (and "hidden" stroke patn no longer exists). Added "hide" attr, 
      // which defaults to "true".
      elementMap7.put( EL_GRIDLINE, 
            new SchemaElementInfo( false, 
               new String[] {}, 
               new String[] {A_STROKEWIDTH, A_STROKECOLOR, A_STROKEPAT, A_HIDE}, 
               new String[] {} ));

      // The "lineType" attr replaced by "strokePat" (and "hidden" stroke patn no longer exists). No longer recognize 
      // the "none" marker; specify zero size instead.
      elementMap7.put( EL_SYMBOL, 
            new SchemaElementInfo( false, 
               new String[] {}, 
               new String[] {A_STROKEWIDTH, A_STROKECOLOR, A_FILLCOLOR, A_STROKEPAT, A_TYPE, A_SIZE, A_TITLE}, 
               new String[] {} ));

      // The "lineType" attr replaced by "strokePat" (and "hidden" stroke patn no longer exists). Former "doc-level 
      // defaulted" attributes "cap" and "capSize" now have standard defaults ("bracket" and "0in", respectively). 
      // "Cap" can no longer have the value "none". Added "hide" attribute (default = "false").
      elementMap7.put( EL_EBAR, 
            new SchemaElementInfo( false, 
               new String[] {}, 
               new String[] {A_STROKEWIDTH, A_STROKECOLOR, A_FILLCOLOR, A_STROKEPAT, A_CAP, A_CAPSIZE, A_HIDE}, 
               new String[] {} ));
	}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in schema version 7; otherwise defers 
    * to the superclass implementation.
    * @see ISchema#isSupportedElementTag(String)
    */
   @Override
   public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap7.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schem element information for any element class added or revised in schema version 7; for 
	 * all other element classes, it defers to the superclass implementation. 
	 * @see ISchema#getSchemaElementInfo(String)
	 */
   @Override
	public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = (SchemaElementInfo) elementMap7.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * This method enforces any requirements on the children content of elements that were introduced or revised in 
	 * schema version 7. For all other element classes, it defers to the superclass implementation.
    * <ul>
    *    <li>The "graph" element now requires the "legend" element as its 5th child. The "hide" attribute on the 
    *    "legend" determines whether or not the legend is rendered.</li>
    * </ul>
	 * @see ISchema#hasRequiredChildren(BasicSchemaElement)
	 */
   @SuppressWarnings("rawtypes")
   @Override
	public boolean hasRequiredChildren(BasicSchemaElement e)
	{
      String elTag = e.getTag();
      List children = e.getElementContent();
      if(EL_GRAPH.equals(elTag))
      {
         if(children.size() < 5) return(false);
         String tag0 = ((BasicSchemaElement) children.get(0)).getTag();
         String tag1 = ((BasicSchemaElement) children.get(1)).getTag();
         String tag2 = ((BasicSchemaElement) children.get(2)).getTag();
         String tag3 = ((BasicSchemaElement) children.get(3)).getTag();
         String tag4 = ((BasicSchemaElement) children.get(4)).getTag();
         return(EL_AXIS.equals(tag0) && EL_AXIS.equals(tag1) && EL_GRIDLINE.equals(tag2) && 
                  EL_GRIDLINE.equals(tag3) && EL_LEGEND.equals(tag4));
      }
      return(super.hasRequiredChildren(e));
	}

   /**
	 * Overridden to enforce any structural restrictions on the children content of elements that were introduced or 
	 * revised in schema version 7. For all other element classes, it defers to the superclass implementation.
    * <ul>
    *    <li>The "graph" element now requires a "legend" child, which must appear as the fifth child.</li>
    * </ul>
	 * @see ISchema#isValidChildAtIndex(BasicSchemaElement,String,int)
	 */
   @Override
	public boolean isValidChildAtIndex(BasicSchemaElement e, String childTag, int index)
	{
      String elTag = e.getTag();
      if(EL_GRAPH.equals(elTag))
      {
         SchemaElementInfo eInfo = (SchemaElementInfo) getSchemaElementInfo(elTag);
         if(!eInfo.isChildAllowed(childTag)) return(false);
         if(EL_AXIS.equals(childTag)) return(index == 0 || index == 1);
         else if(EL_GRIDLINE.equals(childTag)) return(index == 2 || index == 3);
         else if(EL_LEGEND.equals(childTag)) return(index == 4);
         else return(index >= 5);
      }
      return(super.isValidChildAtIndex(e, childTag, index));
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
    *    <li>The "font" property now contains only a single font family name. Any non-empty string is valid.</li>
    *    <li>The "fontSize" property is no longer a measurement, but a point size. It is an integer in [1..99.]</li>
    *    <li>The new "altFont" property replaces the old "substfont". As before, it is an enumerated attribute with 
    *    three alternatives, but one of those has been renamed ("sans-serif" is now "sanserif").</li>
    *    <li>The "psFont" property, which renames the multi-choice "psfont", is validated in the same manner.</li>
    *    <li>The "strokePat" property, which replaces "lineType", is validated as an integer list containing 1-6 
    *    integers that must lie in [1..99]. Five human-readable synonyms are recognized: "solid", "dotted", "dashed", 
    *    "dashdot", and "dashdotdot". The dash-gap lengths in the stroke pattern array are no longer measured in 
    *    milli-inches, but in tenths of a linewidth.</li>
    *    <li>The new "legend" property is a boolean property replacing "hide" on "function" and "trace" elements.</li>
    *    <li>The "mode" property replaces the "type" property on a "trace" element, and it is validated in the same 
    *    manner. It is only defined on a "trace" element.</li>
    *    <li>The new "primary" property is a boolean property replacing "horiz" on a "calib" element.</li>
    *    <li>The value "none" is no longer recognized as a valid adornment type. Also, all adornment type names are now 
    *    entirely lowercase, and any colon (':') characters are removed; in the previous schema, selected adornments had
    *    some interior capital letters (eg, "fillThinArrow"), and others had a colon (eg, "oval1:2", "rect2:1"). 
    *    Validation of the relevant properties ("cap", "type" on "shape" and "symbol" elements) was updated 
    *    accordingly.</li>
    *    <li>The "perLogIntv" attribute is unchanged, except that it can now be an empty string, and it no longer has 
    *    any human-readable synonyms.</li>
    *    <li>Choices for the "fmt" attribute on the "set" element changed (same meaning, just different spellings).</li>
    *    <li>In <em>Phyplot</em> 2.0.0, we introduced reasonable constraints on measured properties like stroke width, 
    *    location coordinates, size, etc. These constraints <code>are not</em> checked here; violating such a constraint 
    *    is not considered "invalid". Rather, when the schema is converted into the in-memory graphic model, any 
    *    measured property that is out-of-bounds is adjusted accordingly. Of course, each measured property is still 
    *    validated to ensure it has the proper form.</li>
	 * </ul>
	 * @see ISchema#isValidAttributeValue(BasicSchemaElement,String,String)
	 */
   @Override
	public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      String elTag = e.getTag();
      if(A_FONT.equals(attr))
         return(value != null && value.length() > 0);
      else if(A_FONTSIZE.equals(attr))
         return(isValidIntegerAttributeValue(value, MINFONTSIZE, MAXFONTSIZE));
      else if(A_PSFONT_V7.equals(attr))
         return(isValidEnumAttributeValue(value, PSFONT_CHOICES));
      else if(A_ALTFONT.equals(attr))
         return(isValidEnumAttributeValue(value, ALTFONT_CHOICES));
      else if(A_STROKEPAT.equals(attr))
         return(isValidIntListAttributeValue(value, MINSTRKPATARRAYLEN, MAXSTRKPATARRAYLEN, MINSTRKPATDASHLEN, 
               MAXSTRKPATDASHLEN, true, STROKEPAT_SYNONYMS));
      else if(A_LEGEND.equals(attr) || A_PRIMARY.equals(attr))
         return("true".equals(value) || "false".equals(value));
      else if(A_MODE.equals(attr))
         return(isValidEnumAttributeValue(value, DISPMODE_CHOICES));
      else if(A_CAP.equals(attr) || (A_TYPE.equals(attr) && (EL_SHAPE.equals(elTag) || EL_SYMBOL.equals(elTag))))
         return(isValidEnumAttributeValue(value, ADORN7_CHOICES));
      else if(A_PERLOGINTV.equals(attr))
         return( isValidIntListAttributeValue(value, 0, 9, 1, 9, false, null) );
      else if(A_FMT.equals(attr) && EL_SET.equals(elTag))
         return(isValidEnumAttributeValue(value, SETFMT_CHOICES_V7));
      
      return(super.isValidAttributeValue(e, attr, value));
	}

   /**
	 * This method handles the actual details of migrating <code>Schema6</code> content to <code>Schema7</code>. Below 
    * is a summary of the similarities and differences between the two schemas, and what this method does to migrate 
    * from the older to the newer schema.
	 * <ul>
    *    <li>No changes in the element namespace. Most changes involve either attribute names or allowed attribute 
    *    values.</li>
    *    <li>The "graph" element now requires a "legend" child, which is always the fifth (and last) of the required 
    *    child nodes. During migration, if any graph lacks a legend, one is provided. The legend's new "hide" attribute
    *    will be set so that it does not change the graph's rendered appearance.</li>
    *    <li>The "axis" element no longer admits "label" or "line" elements as children. If any are encountered during 
    *    migration, they are simply removed -- with ONE exception: If the original axis has an empty "title" attribute 
    *    but contains at least one "label" child, the first such label (that actually has some text!) is assumed to 
    *    represent the axis label. The "title" property of the new "axis" is set to the text of this label. This 
    *    exception was made because a fair number of users in the past have used a child label element to define the 
    *    axis's label. Note that this is not a perfect fix: the label child may not have been intended as the axis label; 
    *    and no adjustment is made in the "labelOffset" property.</li>
    *    <li>The "legend" element never used its own stroke-related properties, so these have been removed in 
    *    <code>Schema7</code>. A "hide" attribute has been added (default = "false").</li>
    *    <li>Eliminated the concept of "document-level default" attribute values. The attributes "cap", "capSize", 
    *    "mid", "spacer", "labelOffset", "perLogIntv", "dir", "len", "fmt", and "gap" had default values specified on 
    *    the document's root "figure" element, where these attributes were required. This method remembers the default 
    *    values and, where necessary, explicitly sets the attribute values as it traverses the element tree.</li>
    *    <li>The "font" property is now just a single font family name, instead of two separated by a comma. This 
    *    method simply takes the first family name in the old "font" as the value for the new "font".</li>
    *    <li>The "fontSize" property is no longer a measurement, but an integer in [1..99] specifying the font size in 
    *    typographical points. This method converts the old "fontSize" to typographical points, rounds to the nearest 
    *    integer, and range-limits the result to [1..99].</li>
    *    <li>The new "altFont" property replaces the old "substfont" attribute. Also, "altFont" is defined on any 
    *    element class that has the font-related properties (in prior versions, "substfont" was only defined on the 
    *    "figure" element). During migration, the figure's "altFont" takes on the same value as 
    *    the old "substfont" (except that "sans-serif" is replaced by "sanserif"!!), and it is implicit (inherited) on 
    *    all other elements.</li>
    *    <li>The "psFont" property renames the old "psfont" property; its value is unchanged.</li>
    *    <li>The "strokePat" property replaces "lineType", with some important differences. It is still an integer list 
    *    of 1-6 integers representing a sequence of dash-gap lengths. However, each dash-gap length is restricted to 
    *    [1..99] and is measured in 0.1 linewidths instead of milli-inches. In addition, there's no longer such a thing 
    *    as a "hidden" line style! Finally, "strokePat" is treated as inheritable property, even though it is not 
    *    defined for all element classes. Migration strategy: The method keeps track of the current stroke width as it 
    *    traverses the document tree. It uses this value to convert each dash-gap length from milli-inches to 0.1 
    *    linewidths; the result is then range-restricted to [1..99]. If the "hidden" synonym is encountered, it is 
    *    replaced by "solid" and another attribute is changed to achieve the same effect. For the "gridline" elements, 
    *    a new "hide" attribute is set to "true". For a "line", "shape", "symbol", "ebar", "function", or "trace" 
    *    element, the "strokeWidth" attribute is set to "0in". Doing so, however, can mess up rendering any children 
    *    that inherit the node's stroke width. For all such child nodes, the stroke width is explicitly set to the 
    *    parent node's old stroke width.</li>
    *    <li>The new "legend" property replaces "hide" on "function" and "trace" elements; it's value is OPPOSITE that 
    *    of "hide". Note that the implicit value of "hide" was "false", which is equivalent to the implicit value for 
    *    "legend", which is "true".</li>
    *    <li>The "mode" property replaces the "type" property on a "trace" element; value unchanged.</li>
    *    <li>The new "primary" property replaces "horiz" on a "calib" element; value unchanged.</li>
    *    <li>The value "none" is no longer recognized as a valid adornment type for the "cap" property on "calib" and 
    *    "ebar" elements, or the "type" property on "shape" and "symbol" elements. Whenever encountered, it is replaced 
    *    with the default adornment type (the "cap" or "type" property is made implicit") and the marker size ("capSize"
    *    on "calib" and "ebar" elements; "size" on "shape" or "symbol" elements) is explicitly set to "0in". For 
    *    "symbol" elements only, "none" was the default, implicit value for the "type" property!</li>
    *    <li>Selected adornment type names had interior capitals ("fillThinArrow", "upDart") in the previous schema 
    *    version. Now, while spelled the same, they are all lowercase. In addition, the adornment type names that had 
    *    a colon (':') in them before no longer do so ("oval1:2" --> "oval12", etc.).</li>
    *    <li>The "perLogIntv" attribute on the "ticks" element is unchanged, except that it no longer has "synonyms". 
    *    The old synonym "all" is replaced by "1 2 3 4 5 6 7 8 9", "even" by "2 4 6 8", and "odd" by "1 3 5 7 9".</li>
    *    <li>Most of the allowed values for the "fmt" attribute on the "set" element have changed: "pointSet" is now 
    *    "ptSet", "series" is unchanged, "multiSet" is now "mset", and "multiSeries" is now "mseries".</li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
	 */
	public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
	{
		if(oldSchema.getVersion() != getVersion() - 1) 
			throw new XMLException("A schema instance can only migrate from the previous version.");

      // the document-level default attribute values from the old "figure" node
      HashMap<String, String> docDefaults = new HashMap<String, String>();
      
      // use this to set new attribute values or to set the values of attributes that have changed
      HashMap<String, String> attrUpdates = new HashMap<String, String>();

      // update the content of the old schema in place...
      Stack<BasicSchemaElement> elementStack = new Stack<BasicSchemaElement>();
      elementStack.push((BasicSchemaElement) oldSchema.getRootElement());
      while(!elementStack.isEmpty())
      {
         BasicSchemaElement e = elementStack.pop();
         String elTag = e.getTag();
         attrUpdates.clear();
         boolean graphLegendWasAdded = false;

         if(e.hasAttribute(A_FONT))
         {
            String family = e.getAttributeValueByName(A_FONT);
            if(family != null)  
            {
               attrUpdates.put(A_FONT, family.substring(0, family.indexOf(',')));
               e.removeAttributeByName(A_FONT);
            }
         }

         if(e.hasAttribute(A_FONTSIZE))
         {
            String fontSize = e.getAttributeValueByName(A_FONTSIZE);
            if(fontSize != null) 
            {
               attrUpdates.put(A_FONTSIZE, convertMeasuredFontSizeToPoints(fontSize));
               e.removeAttributeByName(A_FONTSIZE);
            }
         }

         if(e.hasAttribute(A_PSFONT))
         {
            attrUpdates.put(A_PSFONT_V7, e.getAttributeValueByName(A_PSFONT));
            e.removeAttributeByName(A_PSFONT);
         }

         if(e.hasAttribute(A_SUBSTFONT))
         {
            String value = e.getAttributeValueByName(A_SUBSTFONT);
            attrUpdates.put(A_ALTFONT, SUBSTFONT_SANSSERIF.equals(value) ? ALTFONT_SANSSERIF : value);
            e.removeAttributeByName(A_SUBSTFONT);
         }

         // convert old "lineType" attribute to "strokePat", and deal with obsolete "hidden" synonym appropriately
         if(e.hasAttribute(A_LINETYPE))
         {
            String lineType = e.getAttributeValueByName(A_LINETYPE);
            e.removeAttributeByName(A_LINETYPE);
            
            // handling 'lineType' on a 'gridline': in previous schema, 'lineType' had an implicit value of 'hidden' 
            // for a 'gridline' element (its value was NOT inherited). In Schema7, the new 'hide' flag, which defaults 
            // to 'true', must be explicitly set if 'lineType' is NOT hidden.
            boolean gridlineHidden = true;

            if(lineType != null)
            {
               // get stroke width in MI. We need it to do the conversion. Remember that value may be inherited!
               BasicSchemaElement node = e;
               while(!(node.hasAttribute(A_STROKEWIDTH) && (node.getAttributeValueByName(A_STROKEWIDTH) != null)))
                  node = (BasicSchemaElement) node.getParent();

               String sw = node.getAttributeValueByName(A_STROKEWIDTH);
               double strokeWidthMI = getMeasureFor(sw);
               if(sw.indexOf("in") > 0) strokeWidthMI *= 1000;
               else if(sw.indexOf("mm") > 0) strokeWidthMI *= (MM2IN * 1000);
               else if(sw.indexOf("cm") > 0) strokeWidthMI *= (MM2IN * 10000);
               else if(sw.indexOf("pt") > 0) strokeWidthMI *= (PT2IN * 1000);
               else assert(false);

               if(LINETYPE_HIDDEN.equals(lineType))
               {
                  attrUpdates.put(A_STROKEPAT, LINETYPE_SOLID);
                  if(EL_LINE.equals(elTag) || EL_SHAPE.equals(elTag) || EL_TRACE.equals(elTag) ||
                        EL_FUNCTION.equals(elTag) || EL_EBAR.equals(elTag) || EL_SYMBOL.equals(elTag))
                  {
                     sw = Utilities.toString(strokeWidthMI/1000.0, 4, 3) + "in";
                     for(int i=0; i<e.getChildCount(); i++)
                     {
                        BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
                        if(child.hasAttribute(A_STROKEWIDTH) && (null == child.getAttributeValueByName(A_STROKEWIDTH)))
                           child.setAttributeValueByName(A_STROKEWIDTH, sw);
                     }

                     e.setAttributeValueByName(A_STROKEWIDTH, "0in");
                  }
               }
               else
               {
                  attrUpdates.put(A_STROKEPAT, convertOldLineType(lineType, strokeWidthMI));
                  if(EL_GRIDLINE.equals(elTag)) gridlineHidden = false;
               }
            }
            
            // here we explicitly set the 'hide' flag to false on a 'gridline' element, if necessary
            if(EL_GRIDLINE.equals(elTag) && !gridlineHidden)
               attrUpdates.put(A_HIDE, "false");
         }

         if(EL_FIGURE.equals(elTag))
         {
            docDefaults.put(A_CAP, e.getAttributeValueByName(A_CAP));
            docDefaults.put(A_CAPSIZE, e.getAttributeValueByName(A_CAPSIZE));
            docDefaults.put(A_MID, e.getAttributeValueByName(A_MID));
            docDefaults.put(A_SPACER, e.getAttributeValueByName(A_SPACER));
            docDefaults.put(A_LABELOFFSET, e.getAttributeValueByName(A_LABELOFFSET));

            String perLogIntv = e.getAttributeValueByName(A_PERLOGINTV);
            if("all".equals(perLogIntv)) perLogIntv = "1 2 3 4 5 6 7 8 9";
            else if("even".equals(perLogIntv)) perLogIntv = "2 4 6 8";
            else if("odd".equals(perLogIntv)) perLogIntv = "1 3 5 7 9";
            docDefaults.put(A_PERLOGINTV, perLogIntv);

            docDefaults.put(A_DIR, e.getAttributeValueByName(A_DIR));
            docDefaults.put(A_LEN, e.getAttributeValueByName(A_LEN));
            docDefaults.put(A_FMT, e.getAttributeValueByName(A_FMT));
            docDefaults.put(A_GAP, e.getAttributeValueByName(A_GAP));

            e.removeAttributeByName(A_CAP);
            e.removeAttributeByName(A_CAPSIZE);
            e.removeAttributeByName(A_MID);
            e.removeAttributeByName(A_SPACER);
            e.removeAttributeByName(A_LABELOFFSET);
            e.removeAttributeByName(A_PERLOGINTV);
            e.removeAttributeByName(A_DIR);
            e.removeAttributeByName(A_LEN);
            e.removeAttributeByName(A_FMT);
            e.removeAttributeByName(A_GAP);
            
            attrUpdates.put(A_STROKEPAT, LINETYPE_SOLID);
         }
         else if(EL_GRAPH.equals(elTag))
         {
            if(e.getChildCount() < 5 || !EL_LEGEND.equals( ((BasicSchemaElement) e.getChildAt(4)).getTag() ))
            {
               BasicSchemaElement legend = (BasicSchemaElement) createElement(EL_LEGEND);
               legend.setAttributeValueByName(A_HIDE, "true");
               legend.setAttributeValueByName(A_LOC, "0% 0%");
               e.insert(legend, 4);
               graphLegendWasAdded = true;
            }
         }
         else if(EL_AXIS.equals(elTag))
         {
            boolean hasAutoTitle = (e.getAttributeValueByName(A_TITLE) != null);
            
            int i = 0;
            while(i < e.getChildCount())
            {
               BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
               String childTag = child.getTag();
               if(EL_TICKS.equals(childTag))
                  ++i;
               else if(EL_LABEL.equals(childTag) && !hasAutoTitle)
               {
                  String axisLabel = child.getAttributeValueByName(A_TITLE);
                  if(axisLabel != null && axisLabel.length() > 0)
                  {
                     e.setAttributeValueByName(A_TITLE, axisLabel);
                     hasAutoTitle = true;
                  }
                  e.remove(i);
               }
               else e.remove(i);
            }

            String value = e.getAttributeValueByName(A_SPACER);
            if(value == null) e.setAttributeValueByName(A_SPACER, docDefaults.get(A_SPACER));

            value = e.getAttributeValueByName(A_LABELOFFSET);
            if(value == null) e.setAttributeValueByName(A_LABELOFFSET, docDefaults.get(A_LABELOFFSET));
         }
         else if(EL_TICKS.equals(elTag))
         {
            String value = e.getAttributeValueByName(A_PERLOGINTV);
            if(value == null) e.setAttributeValueByName(A_PERLOGINTV, docDefaults.get(A_PERLOGINTV));
            else if("all".equals(value)) e.setAttributeValueByName(A_PERLOGINTV, "1 2 3 4 5 6 7 8 9");
            else if("even".equals(value)) e.setAttributeValueByName(A_PERLOGINTV, "2 4 6 8");
            else if("odd".equals(value)) e.setAttributeValueByName(A_PERLOGINTV, "1 3 5 7 9");
            
            value = e.getAttributeValueByName(A_DIR);
            if(value == null) e.setAttributeValueByName(A_DIR, docDefaults.get(A_DIR));
            
            value = e.getAttributeValueByName(A_LEN);
            if(value == null) e.setAttributeValueByName(A_LEN, docDefaults.get(A_LEN));
            
            value = e.getAttributeValueByName(A_FMT);
            if(value == null) e.setAttributeValueByName(A_FMT, docDefaults.get(A_FMT));
            
            value = e.getAttributeValueByName(A_GAP);
            if(value == null) e.setAttributeValueByName(A_GAP, docDefaults.get(A_GAP));
         }
         else if(EL_LEGEND.equals(elTag))
         {
            e.removeAttributeByName(A_STROKECOLOR);
            e.removeAttributeByName(A_STROKEWIDTH);

            String value = e.getAttributeValueByName(A_MID);
            if(value == null) e.setAttributeValueByName(A_MID, docDefaults.get(A_MID));
            
            // if legend was present in Schema6, then it is NOT hidden. In Schema7, default value of "hide" 
            // attribute is "true", so we must explicitly set it to "false"!
            attrUpdates.put(A_HIDE, "false");
         }
         else if(EL_TRACE.equals(elTag))
         {
            String value = e.getAttributeValueByName(A_TYPE);
            if(value != null)
            {
               attrUpdates.put(A_MODE, value);
               e.removeAttributeByName(A_TYPE);
            }

            value = e.getAttributeValueByName(A_HIDE);
            if(value != null)
            {
               attrUpdates.put(A_LEGEND, "true".equals(value) ? "false" : "true");
               e.removeAttributeByName(A_HIDE);
            }
         }
         else if(EL_FUNCTION.equals(elTag))
         {
            String value = e.getAttributeValueByName(A_HIDE);
            if(value != null)
            {
               attrUpdates.put(A_LEGEND, "true".equals(value) ? "false" : "true");
               e.removeAttributeByName(A_HIDE);
            }
         }
         else if(EL_CALIB.equals(elTag))
         {
            String value = e.getAttributeValueByName(A_HORIZ);
            if(value != null)
            {
               attrUpdates.put(A_PRIMARY, value);
               e.removeAttributeByName(A_HORIZ);
            }
            
            value = e.getAttributeValueByName(A_CAPSIZE);
            if(value == null) e.setAttributeValueByName(A_CAPSIZE, docDefaults.get(A_CAPSIZE));
            
            // REM: All adornment type names are now entirely lowercase, and any ':' is removed.
            value = e.getAttributeValueByName(A_CAP);
            e.removeAttributeByName(A_CAP);

            if(value == null) value = docDefaults.get(A_CAP);
            value = value.toLowerCase().replace(":", "");
            
            if(ATTRVAL_NONE.equals(value)) e.setAttributeValueByName(A_CAPSIZE, "0in");
            else if(!DEFAULT_CALIB_CAP.equals(value)) attrUpdates.put(A_CAP, value);
         }
         else if(EL_EBAR.equals(elTag))
         {
            String value = e.getAttributeValueByName(A_CAPSIZE);
            if(value == null) e.setAttributeValueByName(A_CAPSIZE, docDefaults.get(A_CAPSIZE));
            
            // REM: All adornment type names are now entirely lowercase, and any ':' is removed.
            value = e.getAttributeValueByName(A_CAP);
            e.removeAttributeByName(A_CAP);

            if(value == null) value = docDefaults.get(A_CAP);
            value = value.toLowerCase().replace(":", "");
            
            if(ATTRVAL_NONE.equals(value)) e.setAttributeValueByName(A_CAPSIZE, "0in");
            else if(!DEFAULT_EBAR_CAP.equals(value)) attrUpdates.put(A_CAP, value);
         }
         else if(EL_SHAPE.equals(elTag))
         {
            // REM: All adornment type names are now entirely lowercase, and any ':' is removed.
            String value = e.getAttributeValueByName(A_TYPE);
            if(value == null) value = ADORN_BOX;
            e.removeAttributeByName(A_TYPE);
            value = value.toLowerCase().replace(":", "");
            if(ATTRVAL_NONE.equals(value)) e.setAttributeValueByName(A_SIZE, "0in");
            else if(!DEFAULT_SYMBOL.equals(value)) attrUpdates.put(A_TYPE, value);
         }
         else if(EL_SYMBOL.equals(elTag))
         {
            // REM: All adornment type names are now entirely lowercase and any ':' is removed. Also, the default, 
            // implicit value for "type" attr was "none" in the prior schema version!
            String value = e.getAttributeValueByName(A_TYPE);
            if(value == null) value = ATTRVAL_NONE;
            e.removeAttributeByName(A_TYPE);
            value = value.toLowerCase().replace(":", "");
            if(ATTRVAL_NONE.equals(value)) e.setAttributeValueByName(A_SIZE, "0in");
            else if(!DEFAULT_SYMBOL.equals(value)) attrUpdates.put(A_TYPE, value);
         }
         else if(EL_SET.equals(elTag))
         {
            String value = e.getAttributeValueByName(A_FMT);
            e.removeAttributeByName(A_FMT);
            if(SETFMT_SERIES.equals(value)) value = SETFMT_SERIES_V7;
            else if(SETFMT_MSET.equals(value)) value = SETFMT_MSET_V7;
            else if(SETFMT_MSERIES.equals(value)) value = SETFMT_MSERIES_V7;
            else value = SETFMT_PTS_V7;
            attrUpdates.put(A_FMT, value);
         }

         // migrate the element object's schema info.
         e.updateSchema(this, null);

         // now that the schema is updated, update any new/renamed/modified attributes that have been explicitly set
         Set<String> attrNames = attrUpdates.keySet();
         for(String name : attrNames)
         {
            String value = attrUpdates.get(name);
            if(value != null) e.setAttributeValueByName(name, value);
         }

         // if element has any children, push them onto the stack so that we check them as well! We don't push on any 
         // child that was added during migration of the parent, since it will already conform to current schema.
         for(int i=0; i<e.getChildCount(); i++)
         {
            BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
            if(EL_GRAPH.equals(elTag) && EL_LEGEND.equals(child.getTag()) && graphLegendWasAdded) 
               continue;
            elementStack.push(child);
         }
      }

      // the content model now conforms to this schema.  We get the root element from the old schema and install it as 
      // the root of this schema, then empty the old schema. We also remember the original schema version of the 
      // migrated content.
      originalVersion = oldSchema.getOriginalVersion();
      setRootElement(oldSchema.getRootElement(), false);
      oldSchema.setRootElement(null, false);
	}

   /** Multiply a measure in typographical points by this factor to convert to inches (1pt = 1/72 in). */
   final static double PT2IN = 0.0138888889;

   /** Multiply a measure in millimeters by this factor to convert to inches. */
   final static double MM2IN = 0.03937;

   /** 
    * Helper method takes a "fontSize" attribute from <code>Schema6</code> -- a measurement with numerical value and 
    * physical units ("in", "cm", "mm", or "pt") -- and converts it to the <code>Schema7</code> form -- an integer 
    * representing the font size in typographical points (= 1/72 in), restricted to the range [1..99].
    * 
    * 27apr2011: Discovered and fixed a bug in the calculation that converts font size in cm or mm to pt. This bug was
    * probably never found because the old "fontSize" attribute was never expressed as a measure in cm or mm!
    * 
    * @param fontSize A <code>Schema6</code> "fontSize" value, in its string form.
    * @return The closest possible <code>Schema7</code> "fontSize" value, again in its string form. If argument is 
    * <code>null</code>, <code>null</code> is also returned.
    * @throws IllegalArgumentException if arg is not a valid measurement with units "in", "cm", "mm", or "pt".
    */
   String convertMeasuredFontSizeToPoints(String fontSize)
   {
      if(fontSize == null) return(null);
      double d = getMeasureFor(fontSize);
      if(fontSize.indexOf("in") > 0) d /= PT2IN;
      else if(fontSize.indexOf("cm") > 0) d *= (MM2IN*10)/PT2IN;
      else if(fontSize.indexOf("mm") > 0) d *= MM2IN/PT2IN;
      int pts = (int) Math.round(d);
      if(pts < MINFONTSIZE) pts = MINFONTSIZE;
      else if(pts > MAXFONTSIZE) pts = MAXFONTSIZE;
      return(Integer.toString(pts));
   }

   /**
    * Helper method takes the value of the pre-<code>Schema7</code> attribute "lineType" and converts it to the closest 
    * matching value for the replacement attribute "strokePat". The former attribute was an integer list of 1-6 dash/gap 
    * lengths in <em>milli-inches</em>, whereas the latter is measured in units of 0.1 linewidths. The linewidth arg is 
    * needed to make the conversion. Note that each dash/gap length is restricted to [1..99].
    * @param lineType The old "lineType" attribute, in its string form.
    * @param lineWidthMI The linewidth (ie, stroke width) of the element for which the attribute is being converted, 
    * in milli-inches. If this is non-positive, then it is not possible to do the conversion, and "solid" is returned.
    * @return The corresponding value for the new "strokePat" attribute that best matches the "lineType" value, also in 
    * string form.
    * @throws IllegalArgumentException if the "lineType" argument is not parsable as a list of 1-6 integers or one of 
    * the recognized synonyms -- but not the obsolete "hidden" synonym.
    */
   String convertOldLineType(String lineType, double lineWidthMI)
   {
      if(lineWidthMI <= 0) return(LINETYPE_SOLID);
      
      int[] dashgapseq = null;
      if(LINETYPE_HIDDEN.equals(lineType)) throw new IllegalArgumentException("Bad lineType attr value!");
      else if(LINETYPE_SOLID.equals(lineType)) dashgapseq = new int[] {10};
      else if(LINETYPE_DOTTED.equals(lineType)) dashgapseq = new int[] {10, 30};
      else if(LINETYPE_DASHED.equals(lineType)) dashgapseq = new int[] {100, 50};
      else if(LINETYPE_DASHDOT.equals(lineType)) dashgapseq = new int[] {100, 50, 10, 50};
      else
      {
         StringTokenizer tokenizer = new StringTokenizer(lineType);
         int n = tokenizer.countTokens();
         if(n > MAXSTRKPATARRAYLEN) throw new IllegalArgumentException("Bad lineType attr value!");
         dashgapseq = new int[n];
         int i = 0;
         while(tokenizer.hasMoreTokens())
         {
            try { dashgapseq[i] = Integer.parseInt(tokenizer.nextToken()); }
            catch(NumberFormatException nfe) {throw new IllegalArgumentException("Bad lineType attr value!");}
            ++i;
         }
      }

      if(dashgapseq.length == 1) return(LINETYPE_SOLID);
      else
      {
         for(int i=0; i<dashgapseq.length; i++)
         {
            int w = (int) Math.round((dashgapseq[i] / lineWidthMI) * 10.0);
            dashgapseq[i] = (w<MINSTRKPATDASHLEN) ? MINSTRKPATDASHLEN : ((w>MAXSTRKPATDASHLEN) ? MAXSTRKPATDASHLEN : w);
         }
         return(Utilities.toString(dashgapseq));
      }
   }
}
