package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;


/**
 * <code>Schema19</code> is the encapsulation of the <i>DataNav</i> figure model XML schema version 19. It extends 
 * {@link Schema18} and includes support for migrating schema version 18 documents to version 19.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul> 
 *    <li>(04may2015, app V4.7.0) Optional attribute <i>note</i> added to the <i>figure</i> element. It is intended as
 *    a figure description for the author's use only; it is not rendered in the figure.</li>
 *    <li>(28may2015, app V4.7.1) New <i>area</i> element, representing a (stacked) area chart. In addition to the usual
 *    stroke properties, a title, and show-in-legend attribute, this element has a "baseline" attribute specifying the
 *    chart's baseline in user units. Like all data presentation nodes, it has the required "src" attribute. As a
 *    "grouped-data" presentation node, its text content lists the data group fill colors and legend labels.</li>
 *    <li>(09jun2015, app V4.7.1) New <i>pie</i> element, representing a pie chart. In addition to the usual stroke
 *    properties, a title, and show-in-legend attribute, the required "start" and "end" attributes specify the inner and
 *    outer radii of the pie chart (a non-zero inner radius results in a "donut" chart). The "displace" attribute is an 
 *    integer bit flag vector where bitN = 1 indicates that the slice for data group N is displaced outward from the 
 *    pie chart origin. The "gap" attribute is an integer in [1..100] that defines the radial offset of any displaced 
 *    pie slice as a percentage of the outer radius. Like all data presentation nodes, it has the required "src" 
 *    attribute. As a grouped-data presentation node, its text content lists the data group colors and legend labels. 
 *    <b>Note that a pie chart will only be rendered when its parent graph is in polar coordinates.</b></li>
 * </ul>
 * </p>
 * 
 * @author 	sruffner
 */
class Schema19 extends Schema18
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema19"); }
   @Override public int getVersion() { return(19); }
   @Override public String getLastApplicationVersion() { return("4.7.1"); }

   /** Optional string-valued attribute on "figure" node: a note or description of the figure that is not rendered. */
   final static String A_NOTE = "note";
   /** Default value for figure node's optional "note" attribute. */
   final static String DEFAULT_NOTE = "";
   
   /** New data presentation element -- a (stacked) area chart. */
   final static String EL_AREA = "area";

   /** New data presentation element -- a pie chart. */
   final static String EL_PIE = "pie";
   
   /** 
    * Integer-valued pie chart attribute is a bit flag vector where bitN = 1 indicates that pie slice N is displaced
    * radially from the chart's origin.
    */
   final static String A_DISPLACE = "displace";
   /** The default value for the pie chart's "displace" attribute (indicating that no slices are displaced). */
   final static String DEFAULT_PIE_DISPLACE = "0";
   /** Minimum radial offset of a displaced slice in pie chart (as an integer percentage of the outer radius). */
   final static int MIN_PIE_GAP = 1;
   /** Maximum radial offset of a displaced slice in pie chart (as an integer percentage of the outer radius). */
   final static int MAX_PIE_GAP = 100;
   /** 
    * The default value for the pie chart's "gap" attribute (radial offset of a displaced pie slice, as an integer
    * percentage of the outer radius, [1..100]). 
    */
   final static String DEFAULT_PIE_GAP = "10";
   
   /**
	 * This element map contains {@link SchemaElementInfo SchemaElementInfo} objects for each element that
	 * is new to this schema or has a different attribute set compared to the previous schema.
	 */
	private static Map<String, SchemaElementInfo> elementMap19 = null;

	static
	{
		elementMap19 = new HashMap<String, SchemaElementInfo>();
		
      // (04may2015) Figure element now takes optional 'note' attribute specifying a figure description. Default="".
      elementMap19.put( EL_FIGURE, new SchemaElementInfo( false, 
            new String[] {EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE, EL_IMAGE, EL_GRAPH, EL_REF}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT, A_TITLE, 
                          A_BORDER, A_BKG, A_NOTE}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT} ));

	
      // (28may2015) A new data presentation element, the stacked area chart
      elementMap19.put( EL_AREA, new SchemaElementInfo( true, 
            new String[] {}, 
            new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_LEGEND, A_BASELINE, A_SRC}, 
            new String[] {A_SRC} ));
      
      // (09jun2015) A new data presentation element, the pie chart.
      elementMap19.put( EL_PIE, new SchemaElementInfo( true, 
            new String[] {}, 
            new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_LEGEND, A_GAP, A_DISPLACE, A_START, A_END, A_SRC}, 
            new String[] {A_START, A_END, A_SRC} ));
      
      // (28may2015) Graph element revised to accept area chart node as a child.
      // (09jun2015) Revised again to accept pie chart node as a child.
      elementMap19.put( EL_GRAPH, new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_ZAXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE,
                          EL_IMAGE, EL_GRAPH, EL_FUNCTION, EL_TRACE, EL_RASTER, EL_HEATMAP, EL_BAR, EL_SCATTER, 
                          EL_AREA, EL_PIE}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, 
                          A_CLIP, A_AUTORANGE, A_ID,
                          A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in this schema version; else defers 
    * to the superclass implementation.
    */
   @Override public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap19.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in this schema version; 
	 * for all other element classes, it defers to the superclass implementation. 
	 */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = (SchemaElementInfo) elementMap19.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
    *    <li>The <i>note</i> attribute on the root <i>figure</i> node can be any non-null string token.</li>
    *    <li>The <i>baseline</i> attribute of the <i>area</i> node, and the <i>start</i> and <i>end</> attributes of the
    *    <i>pie</i> element, are unrestricted floating-point values.</li>
    *    <li>The <i>displace</i> attribute of the <i>pie</i> node is a non-negative integer attribute.</li>
    *    <li>The <i>gap</i> attribute of the <i>pie</i> node is an integer attribute restricted to [1..100].</li>
	 * </ul>
	 */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      String tag = e.getTag();
      
      if(A_NOTE.equals(attr) && EL_FIGURE.equals(tag)) return(value != null);
      if(A_BASELINE.equals(attr) && EL_AREA.equals(tag))
         return(isValidFloatAttributeValue(value, -Double.MAX_VALUE, Double.MAX_VALUE));
      if(EL_PIE.equals(tag))
      {
         if(A_START.equals(attr) || A_END.equals(attr))
            return(isValidFloatAttributeValue(value, -Double.MAX_VALUE, Double.MAX_VALUE));
         if(A_DISPLACE.equals(attr))
            return(isValidIntegerAttributeValue(value, 0, Integer.MAX_VALUE));
         if(A_GAP.equals(attr))
            return(isValidIntegerAttributeValue(value, MIN_PIE_GAP, MAX_PIE_GAP));
      }
      
      return(super.isValidAttributeValue(e, attr, value));
	}

   /**
    * This method handles the actual details of migrating from the previous schema version to this one. It makes the
    * following changes:
    * <ul>
    *    <li><i>No changes are needed. However, since we're migrating "in place", we have to replace all references to 
    *    the previous schema with the current schema!</i> </li>
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
         // String elTag = e.getTag();
         
         // migrate the element object's schema info.
         e.updateSchema(this, null);

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
