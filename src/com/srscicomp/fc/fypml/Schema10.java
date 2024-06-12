package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema10</code> is the encapsulation of the <em>DataNav</em> figure model XML schema version 10. It extends 
 * <code>Schema9</code> and includes support for migrating schema version 9 documents to version 10. It includes those
 * schema changes that were required to support two additional stroke-related styles -- the stroke endcap and join
 * properties -- introduced in <i>DataNav</i> v3.2.0. As inheritable styles, these are required on the "figure" node
 * and are properties of most graphic nodes in the figure model.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul>
 *    <li>(24 Sep 2009) Introduced new enumerated attributes <i>strokeCap = {butt, round, square}</i> and <i>strokeJoin
 *    = {miter, round, bevel}</i>. They are both inheritable styles and, as such, are required attributes for the
 *    <i>figure</i> node. They were added as optional attributes to the following elements: <i>calib, ebar, function, 
 *    gridline, line, raster, shape, symbol, ticks, trace</i>. Prior to v3.2.0, <i>DataNav</i> always used butt-ended
 *    strokes with mitered joins, so migration of <code>Schema9</code> documents is simply a matter of setting 
 *    <i>strokeCap=butt</i> and <i>strokeJoin=miter</i> on the top-level <i>figure</i> element.</li>
 *    <li>(30 Sep 2009) Changed the values of several dash-gap pattern synonyms: <i>dashed</i> =[30 30] instead of 
 *    [99 50]; <i>dashdot</i> = [30 30 10 30] instead of [99 50 10 50]; and <i>dashdotdot</i> = [30 30 10 30 10 30] 
 *    instead of [99 50 10 50 10 50]. During migration, these synonyms are replaced by their old values so that the
 *    rendered figure is unchanged.</li>
 *    <li>(22 Mar 2010) Added color attribute <i>cmapnan</i> to the <i>zaxis</i> element. Prior to v3.4.0, there was an 
 *    inconsistency between Postscript and onscreen rendering of heatmaps. Any ill-defined (infinite or NaN) data were 
 *    mapped to completely transparent pixels in the onscreen rendering, but in Postscript they were mapped to an opaque
 *    color (index entry 255 in the colormap). Rather than try to implement transparency in PS, which requires LL3, we 
 *    decided to make heatmap renderings opaque. We now only use entries [1..255] of the supported colormap LUTs, and 
 *    entry 0 is replaced by the RGB color specified in the <i>cmapnan</i> attribute. All ill-defined data in heatmaps 
 *    are now mapped to color index 0, whether the rendering is onscreen or Postscript; all well-defined data are mapped
 *    to entries [1..255]. Since the attribute is optional, it was not necessary to increment the schema version upon 
 *    making this change. If the attribute is not explicitly specified -- as will be the case for all existing schema 10
 *    documents prior to this change -- it defaults to white.</li>
 *    <li>(07 May 2010) Added boolean attribute <i>smooth</i> to the <i>heatmap<i> element. Prior to v3.4.2, smooth
 *    interpolation of the heatmap image was always enabled, but some users didn't want it because it can be misleading
 *    -- suggesting a smoothness in the underlying data that does not exist. This attribute was added so that users can
 *    now turn off smoothing. Since the attribute is optional, it was not necessary to increment the schema version upon 
 *    making this change. If the attribute is not explicitly specified -- as will be the case for all existing schema 10
 *    documents prior to this change -- it defaults to true.</li>
 *    <li>(03 Jun 2010) Added two additional colormap lookup table options for <i>cmap</i> attribute of a <i>zaxis</i>
 *    element: "jet" and "reversejet". No need to increment schema version upon making this change.</li>
 *    <li>(03 Dec 2010) Added new boolean attribute <i>avg</i> to the <i>raster</i> element. Relevant only in the 
 *    histogram display mode. If false, calculated histogram reflects the <i><b>total</b></i> count per bin; if tre,
 *    the <i><b>average</b></i> count per bin (total count divided by number of individual rasters in the collection).
 *    Since the attribute is optional, it was not necessary to increment the schema version upon making this change. If 
 *    the attribute is not explicitly specified, it defaults to false -- since the histogram reflected total counts per 
 *    bin prior to this change.</li>
 * </ul>
 * </p>
 * 
 * @author 	sruffner
 */
class Schema10 extends Schema9
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema10"); }
   @Override public int getVersion() { return(10); }
   @Override public String getLastApplicationVersion() { return("3.5.4"); }

   /** Enumerated <i>strokeCap</i> inheritable style specifies how ends of stroked open paths are decorated. */
   final static String A_STROKECAP= "strokeCap";

   /** Stroke endcap style: butt-end, meaning no projecting decoration at endpoints of an open path when stroked. */
   final static String STROKECAP_BUTT = "butt";
   /** Stroke endcap style: round decoration projecting 1/2 line width past butt end of an open path. */
   final static String STROKECAP_ROUND = "round";
   /** Stroke endcap style: square decoration projecting 1/2 line width past butt end of an open path. */
   final static String STROKECAP_SQUARE = "square";
   
   /** The set of choices for the <i>strokeCap</i> style attribute. */
   final static String[] STROKECAP_CHOICES = {STROKECAP_BUTT, STROKECAP_ROUND, STROKECAP_SQUARE};

   /** 
    * Enumerated <i>strokeJoin</i> inheritable style specifies stroking decoration applied at the intersection of
    * adjacent segments in a path or where the path is closed. 
    */
   final static String A_STROKEJOIN= "strokeJoin";

   /** Stroke join style: mitered (outside corners extended until they meet; beveled if miter limit of 10 exceeded). */
   final static String STROKEJOIN_MITER = "miter";
   /** Stroke join style: rounded (filled circle with radius = 1/2 line width is rendered at intersection). */
   final static String STROKEJOIN_ROUND = "round";
   /** Stroke endcap style: beveled (straight line connects outside corners). */
   final static String STROKEJOIN_BEVEL = "bevel";
   
   /** The set of choices for the <i>strokeJoin</i> style attribute. */
   final static String[] STROKEJOIN_CHOICES = {STROKEJOIN_MITER, STROKEJOIN_ROUND, STROKEJOIN_BEVEL};

   /** Integer string array value corresponding to the <i>dashed</i> stroke pattern prior to schema version 10. */
   final static String DASHED_VALUE_V9 = "99 50";
   
   /** Integer string array value corresponding to the <i>dashdot</i> stroke pattern prior to schema version 10. */
   final static String DASHDOT_VALUE_V9 = "99 50 10 50";
   
   /** Integer string array value corresponding to the <i>dashdotdot</i> stroke pattern prior to schema version 10. */
   final static String DASHDOTDOT_VALUE_V9 = "99 50 10 50 10 50";
   
   /** Color attr <i>cmapnan</i> specifies the "NaN color" for the <i>zaxis</i> element. */
   final static String A_CMAPNAN = "cmapnan";

   /** Default for the <i>cmapnan</i> attribute of the <i>zaxis</i> element. */
   final static String DEFAULT_ZAXIS_CMAPNAN = "FFFFFF";

   /** Boolean attr <i>smooth</i> enables/disables smoothed image interpolation for the <i>heatmap</i> element. */
   final static String A_SMOOTH = "smooth";

   /** Default for the <i>smooth</i> attribute of the <i>heatmap</i> element. */
   final static String DEFAULT_HEATMAP_SMOOTH = "true";

   /** Colormap choice for <i>cmap</i> attr of <i>z-axis</i>: dark blue to blue to cyan to yellow to red to dark red. */
   final static String CMAP_JET = "jet";
   /** Colormap choice for <i>cmap</i> attr of <i>z-axis</i>: reverse of "jet" colormap. */
   final static String CMAP_REVERSEJET = "reversejet";
   
   /** Revised (as of 03Jun2010) set of choices for the <i>cmap</i> attribute of <i>zaxis</i>. */
   final static String[] CMAP_CHOICES_V10 = {
      CMAP_GRAY, CMAP_REVERSEGRAY, CMAP_HOT, CMAP_REVERSEHOT, CMAP_AUTUMN, CMAP_REVERSEAUTUMN, CMAP_JET, CMAP_REVERSEJET
   };

   /** Boolean attr <i>avg</i> selects average or total counts-per-bin histogram display for <i>raster</i> element. */
   final static String A_AVG = "avg";

   /** Default for the <i>avg</i> attribute of the <i>raster</i> element. */
   final static String DEFAULT_RASTER_AVG = "false";


   /**
	 * This element map contains <code>SchemaElementInfo</code> objects for each element that is new to this schema or 
    * has a different attribute set compared to the previous schema.
	 */
	private static Map<String, SchemaElementInfo> elementMap10 = null;

	static
	{
		elementMap10 = new HashMap<String, SchemaElementInfo>();
		
      // Added "strokeCap" and "strokeJoin" as inheritable attributes. They are required on the "figure" node.
      elementMap10.put( EL_FIGURE, new SchemaElementInfo( false, 
         new String[] {EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH, EL_REF}, 
         new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                       A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_BORDER, A_LOC, A_WIDTH, A_HEIGHT}, 
         new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                       A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT} ));

      // Added optional "strokeCap" and "strokeJoin" attributes to all other graphic nodes that possess stroke-related 
      // attributes. If not explicit, their values are inherited from an ancestor node. 
      // (22mar2010) Added optional "cmapnan" attribute to the "zaxis" element.
      // (03dec2010) Added optional "avg" attribute to the "raster" element.
      elementMap10.put( EL_AXIS, new SchemaElementInfo( false, 
         new String[] {EL_TICKS}, 
         new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                       A_STROKEJOIN, A_STROKECOLOR, A_HIDE, A_UNITS, A_TITLE, A_SPACER, A_LABELOFFSET, A_START, A_END}, 
         new String[] {A_START, A_END} ));
      elementMap10.put( EL_CALIB, new SchemaElementInfo( false, 
         new String[] {EL_LABEL}, 
         new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                       A_STROKEJOIN, A_STROKECOLOR, A_PRIMARY, A_AUTO, A_CAP, A_CAPSIZE, A_LOC, A_LEN}, 
         new String[] {A_LOC, A_LEN} ));
      elementMap10.put( EL_ZAXIS, new SchemaElementInfo( false, 
         new String[] {EL_TICKS}, 
         new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                       A_STROKEJOIN, A_STROKECOLOR, A_HIDE, A_TITLE, A_SPACER, A_LABELOFFSET, A_START, A_END, A_GAP, 
                       A_SIZE, A_EDGE, A_CMAP, A_CMODE, A_CMAPNAN}, 
         new String[] {A_START, A_END} ));
      elementMap10.put( EL_EBAR, new SchemaElementInfo( false, 
         new String[] {}, 
         new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_FILLCOLOR, A_STROKEPAT, A_CAP, 
                       A_CAPSIZE, A_HIDE}, 
         new String[] {} ));
      elementMap10.put( EL_FUNCTION, new SchemaElementInfo( true, 
         new String[] {EL_SYMBOL}, 
         new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                       A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_LEGEND, A_X0, A_X1, A_DX}, 
         new String[] {} ));
      elementMap10.put( EL_GRAPH, new SchemaElementInfo( false, 
         new String[] {EL_AXIS, EL_ZAXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH, 
                       EL_FUNCTION, EL_TRACE, EL_RASTER, EL_HEATMAP}, 
         new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                       A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, A_CLIP, 
                       A_LOC, A_WIDTH, A_HEIGHT}, 
         new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));
      elementMap10.put( EL_GRIDLINE, new SchemaElementInfo( false, 
         new String[] {}, 
         new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_HIDE}, 
         new String[] {} ));
      elementMap10.put( EL_LINE, new SchemaElementInfo( false, 
         new String[] {EL_LABEL, EL_LINE, EL_SHAPE}, 
         new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                       A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_P0, A_P1}, 
         new String[] {A_P0, A_P1} ));
      elementMap10.put( EL_RASTER, new SchemaElementInfo( false, 
         new String[] {}, 
         new String[] {A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                       A_MODE, A_BASELINE, A_FILLED, A_XOFF, A_HEIGHT, A_SPACER, A_NBINS, A_AVG, A_SRC}, 
         new String[] {A_SRC} ));
      elementMap10.put( EL_SHAPE, new SchemaElementInfo( false, 
         new String[] {EL_LABEL}, 
         new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP,
                       A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_ROTATE, A_TYPE, A_SIZE, A_TITLE}, 
         new String[] {A_LOC} ));
      elementMap10.put( EL_SYMBOL, new SchemaElementInfo( false, 
         new String[] {}, 
         new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_FILLCOLOR, A_STROKEPAT, A_TYPE, 
                       A_SIZE, A_TITLE}, 
         new String[] {} ));
      elementMap10.put( EL_TICKS, new SchemaElementInfo( false, 
         new String[] {}, 
         new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP,
                       A_STROKEJOIN, A_STROKECOLOR, A_START, A_END, A_INTV, A_PERLOGINTV, A_DIR, A_LEN, A_FMT, A_GAP}, 
         new String[] {A_INTV} ));
      elementMap10.put( EL_TRACE, new SchemaElementInfo( false, 
         new String[] {EL_SYMBOL, EL_EBAR}, 
         new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP,
                       A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_LEGEND, A_SKIP, A_MODE, A_BARWIDTH, 
                       A_BASELINE, A_FILLED, A_XOFF, A_YOFF, A_SRC}, 
         new String[] {A_SRC} ));

      // (07may2010) Added optional "smooth" attribute to the "heatmap" element.
      elementMap10.put( EL_HEATMAP, 
               new SchemaElementInfo( false, 
                     new String[] {}, 
                     new String[] {A_TITLE, A_SMOOTH, A_SRC}, 
                     new String[] {A_SRC} ));

	}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in schema version 10; otherwise defers 
    * to the superclass implementation.
    * @see ISchema#isSupportedElementTag(String)
    */
   @Override
   public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap10.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in schema version 10; for 
	 * all other element classes, it defers to the superclass implementation. 
	 * @see ISchema#getSchemaElementInfo(String)
	 */
   @Override
	public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = (SchemaElementInfo) elementMap10.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
	 *    <li>The new "strokeCap" style attribute must be one of <code>STROKECAP_CHOICES</code>.</li>
    *    <li>The new "strokeJoin" style attribute must be one of <code>STROKEJOIN_CHOICES</code>.</li>
    *    <li>The new "cmapnan" attribute must be a valid color attribute.</li>
    *    <li>The new "smooth" attribute must be a valid boolean attribute.</li>
    *    <li>The new "avg" attribute must be a valid boolean attribute.</li>
    *    <li>The "cmap" enumerated attribute admits two additional choices as of 03Jun2010. The full set of choices are
    *    in <code>CMAP_CHOICES_V10</code>.</li>
	 * </ul>
	 * @see ISchema#isValidAttributeValue(BasicSchemaElement,String,String)
	 */
   @Override
	public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      if(A_STROKECAP.equals(attr)) return(isValidEnumAttributeValue(value, STROKECAP_CHOICES));
      if(A_STROKEJOIN.equals(attr)) return(isValidEnumAttributeValue(value, STROKEJOIN_CHOICES));
      if(A_CMAPNAN.equals(attr)) return(isValidColorAttributeValue(value));
      if(A_SMOOTH.equals(attr) || A_AVG.equals(attr)) return("true".equals(value) || "false".equals(value));
      if(A_CMAP.equals(attr)) return(isValidEnumAttributeValue(value, CMAP_CHOICES_V10));
      
      return(super.isValidAttributeValue(e, attr, value));
	}
   
   
   /**
	 * This method handles the actual details of migrating <code>Schema9</code> content to <code>Schema10</code>. It 
    * makes the following changes:
	 * <ul>
    *    <li>Updates schema element info for all elements that were modified in <code>Schema10</code>, ie, all for
    *    which the <i>strokeCap</i> and <i>strokeJoin</i> styles were added.</li>
    *    <li>When <i>strokePat</i> style is explicitly set to <i>dashed</i>, <i>dashdot</i>, or <i>dashdotdot</i>, the
    *    synonym is replaced with its pre-Schema10 value (an integer array in string form): "99 50", "99 50 10 50", and
    *    "99 50 10 50 10 50", respectively.</li>
    *    <li>For the <i>figure</i> element, <i>strokeCap</i> and <i>strokeJoin</i> are required. They are explicitly
    *    set to <i>butt</i> and <i>miter</i>, respectively.</li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
	 */
	public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
	{
		if(oldSchema.getVersion() != getVersion() - 1) 
			throw new XMLException("A schema instance can only migrate from the previous version.");

      // update the content of the old schema in place...
      Stack<BasicSchemaElement> elementStack = new Stack<BasicSchemaElement>();
      elementStack.push((BasicSchemaElement) oldSchema.getRootElement());
      while(!elementStack.isEmpty())
      {
         BasicSchemaElement e = elementStack.pop();
         String elTag = e.getTag();
         
         // migrate the element object's schema info.
         e.updateSchema(this, null);

         // if element's strokePat is explicitly set to one of the synonyms that has been redefined in Schema10, then
         // replace the synonym with its pre-V10 value.
         if(e.hasAttribute(A_STROKEPAT))
         {
            String strokePat = e.getAttributeValueByName(A_STROKEPAT);
            if(LINETYPE_DASHED.equals(strokePat))
               e.setAttributeValueByName(A_STROKEPAT, DASHED_VALUE_V9);
            else if(LINETYPE_DASHDOT.equals(strokePat))
               e.setAttributeValueByName(A_STROKEPAT, DASHDOT_VALUE_V9);
            else if(STROKEPAT_DASHDOTDOT.equals(strokePat))  
               e.setAttributeValueByName(A_STROKEPAT, DASHDOTDOT_VALUE_V9);
         }
         
         // after updating schema element info, explicitly set strokeCap=butt, strokeJoin=miter -- the stroke endcap
         // and join styles that DataNav always used prior to this schema change!
         if(EL_FIGURE.equals(elTag))
         {
            e.setAttributeValueByName(A_STROKECAP, STROKECAP_BUTT);
            e.setAttributeValueByName(A_STROKEJOIN, STROKEJOIN_MITER);
         }

         // if element has any children, push them onto the stack so that we check them as well! We don't push on any 
         // child that was added during migration of the parent, since it will already conform to current schema.
         for(int i=0; i<e.getChildCount(); i++)
         {
            BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
            elementStack.push(child);
         }
     }
      
      // the content model now conforms to this schema. We get the root element from the old schema and install it as 
      // the root of this schema, then empty the old schema. We also remember the original schema version of the 
      // migrated content.
      originalVersion = oldSchema.getOriginalVersion();
      setRootElement(oldSchema.getRootElement(), false);
      oldSchema.setRootElement(null, false);
	}
}
