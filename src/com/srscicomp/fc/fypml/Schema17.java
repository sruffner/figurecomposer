package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;


/**
 * <code>Schema17</code> is the encapsulation of the <i>DataNav</i> figure model XML schema version 17. It extends 
 * {@link Schema16} and includes support for migrating schema version 16 documents to version 17.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul>
 *    <li>(15jan2015, app V4.6.1) Modifications to the <i>ticks</i> element to support custom non-numeric tick labels.
 *    The element now allows text content, and that content is parsed as a comma-separated, ordered list of label 
 *    tokens. If there are no tokens, then the usual numeric labels are generated IAW the <i>fmt</i> attribute. If 
 *    there are tokens, the numeric labels are replaced by the custom label tokens in the order specified. If there are 
 *    not enough, they are reused until all tick mark locations are labeled. No changes required during schema migration
 *    -- all pre-existing figures will not have any text content in their <i>ticks</i> elements.</li>
 *    <li>(19jan2015, app V4.6.1) Introduced a new type of data presentation node, the <i>bar</i> element, representing
 *    a bar plot containing up to 20 bar groups. It is backed by a collection-type data set, an "mset" or "mseries". The
 *    text content of the element is a comma-separated list of tokens <i>color1, title1, color2, title2, ...</i> listing
 *    the color and title assigned to each bar group. The bar group colors and titles do not have to be specified; FC
 *    will use the node's fill color for all missing bar group colors, and any missing bar group titles are set to 
 *    "data N" if needed (for the graph legend). No element content.</li>
 * </ul>
 * </p>
 * 
 * @author 	sruffner
 */
class Schema17 extends Schema16
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema17"); }
   @Override public int getVersion() { return(17); }
   @Override public String getLastApplicationVersion() { return("4.6.1"); }

   /** New data presentation element -- a bar plot. */
   final static String EL_BAR = "bar";
   /** A display mode for the "bar" element: vertical bar groups. */
   final static String MODE_BAR_VGRP = "vgroup";
   /** A display mode for the "bar" element: horizontal bar groups. */
   final static String MODE_BAR_HGRP = "hgroup";
   /** A display mode for the "bar" element: vertical stacked bars. */
   final static String MODE_BAR_VSTK = "vstack";
   /** A display mode for the "bar" element: horizontal stacked bars. */
   final static String MODE_BAR_HSTK = "hstack";
   /** The set of choices for the "bar" element's display mode (the "mode" attribute). */
   final static String[] MODE_BAR_CHOICES = {MODE_BAR_VGRP, MODE_BAR_HGRP, MODE_BAR_VSTK, MODE_BAR_HSTK};
   /** Default for the "mode" attribute of the "bar" element. */
   final static String DEFAULT_BAR_MODE= MODE_BAR_VGRP;

   /** Minimum relative bar width (as an integer percentage) for the "bar" element. */
   final static int MIN_BAR_BARW = 5;
   /** Maximum relative bar width (as an integer percentage) for the "bar" element. */
   final static int MAX_BAR_BARW = 100;
   /** Default value for "barWidth" attribute of the "bar" element. */
   final static String DEFAULT_BAR_BARWIDTH = "80";
   

   /**
	 * This element map contains {@link SchemaElementInfo SchemaElementInfo} objects for each element that
	 * is new to this schema or has a different attribute set compared to the previous schema.
	 */
	private static Map<String, SchemaElementInfo> elementMap17 = null;

	static
	{
		elementMap17 = new HashMap<String, SchemaElementInfo>();
		
      // (19jan2015) A new data presentation element, the bar plot.
      elementMap17.put( EL_BAR, new SchemaElementInfo( true, 
            new String[] {}, 
            new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_LEGEND, A_MODE, A_BARWIDTH, A_BASELINE, A_SRC}, 
            new String[] {A_SRC} ));
      
      // (15jan2015) The "ticks" element now allows text content, which is parsed as a comma-separated, ordered list of 
      // custom tick labels.
      elementMap17.put( EL_TICKS, new SchemaElementInfo( true, 
            new String[] {}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP,
                          A_STROKEJOIN, A_STROKECOLOR, A_START, A_END, A_INTV, A_PERLOGINTV, A_DIR, A_LEN, A_FMT, A_GAP}, 
            new String[] {A_INTV} ));

      // (21jan2015) Graph element revised to accept bar plot node as a child.
      elementMap17.put( EL_GRAPH, new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_ZAXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE,
                          EL_IMAGE, EL_GRAPH, EL_FUNCTION, EL_TRACE, EL_RASTER, EL_HEATMAP, EL_BAR}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                          A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, A_CLIP, A_AUTORANGE,
                          A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));
      
	}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in this schema version; else defers 
    * to the superclass implementation.
    */
   @Override public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap17.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in this schema version; 
	 * for all other element classes, it defers to the superclass implementation. 
	 */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = (SchemaElementInfo) elementMap17.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
    *    <li>The <i>mode</i> attribute of the new <i>bar</i> element must be one of the four possible choices listed in
    *    {@link #MODE_BAR_CHOICES}</li>
    *    <li>The <i>barWidth</i> attribute of the new <i>bar</i> element must be an integer value restricted to the
    *    range {@link #MIN_BAR_BARW} to {@link #MAX_BAR_BARW}.</li>
	 * </ul>
	 */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      if(e.getTag().equals(EL_BAR))
      {
         if(A_BARWIDTH.equals(attr)) 
            return(this.isValidIntegerAttributeValue(value, MIN_BAR_BARW, MAX_BAR_BARW));
         else if(A_MODE.equals(attr))
            return(this.isValidEnumAttributeValue(value, MODE_BAR_CHOICES));
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
