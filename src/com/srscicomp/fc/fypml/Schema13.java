package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema13</code> is the encapsulation of the <i>DataNav</i> figure model XML schema version 13. It extends 
 * {@link Schema12} and includes support for migrating schema version 12 documents to version 13.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul>
 *    <li>(22Mar2013) Introduced schema support for enabling automatic axis range adjustment on each of the three axes
 *    of a graph INDEPEDENTLY. The <i>graph</i> node's boolean <i>auto</i> attribute is replaced by the enumerated 
 *    <i>autorange</i> attribute, with the value set: "none", "x", "y", "z", "xy", "xy", "yz", "xyz". During schema 
 *    migration, <i>auto="true" --&gt; autorange="xyz"</i>, while <i>auto="false" --&gt; autorange="none"</i>.</li>
 * </ul>
 * </p>
 * 
 * @author 	sruffner
 */
class Schema13 extends Schema12
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema13"); }
   @Override public int getVersion() { return(13); }
   @Override public String getLastApplicationVersion() { return("4.1.3"); }

   /** 
    * New enumerated attribute <i>autorange</i> for the <i>graph</i> element: This allows user to enable automatic
    * range adjustment on each graph axis independently, instead of all-or-none. Replaces <i>auto</i> attribute.
    */
   final static String A_AUTORANGE = "autorange";
   /** Axis auto-range choice: auto-ranging disabled on all graph axes. */ 
   final static String AUTORANGE_NONE = "none";
   /** Axis auto-range choice: auto-ranging enabled on all graph axes. */ 
   final static String AUTORANGE_ALL = "xyz";
   /** Set of choices for the <i>autorange</i> attribute. */
   final static String[] AUTORANGE_CHOICES = {AUTORANGE_NONE, "x", "y", "z", "xy", "xz", "yz", AUTORANGE_ALL};
   /** Default for the <i>autorange</i> attribute of the <i>graph</i> element (= "none"). */
   final static String DEFAULT_AUTORANGE = AUTORANGE_NONE;
   
   /**
	 * This element map contains {@link SchemaElementInfo SchemaElementInfo} objects for each element that
	 * is new to this schema or has a different attribute set compared to the previous schema.
	 */
	private static Map<String, SchemaElementInfo> elementMap13 = null;

	static
	{
		elementMap13 = new HashMap<String, SchemaElementInfo>();
		
      // (22mar2013) Replaced attribute "auto" with "autorange" the "graph" element. Optional. Default value is "none".
      elementMap13.put( EL_GRAPH, new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_ZAXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH, 
                          EL_FUNCTION, EL_TRACE, EL_RASTER, EL_HEATMAP}, 
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
      return(elementMap13.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in this schema version; 
	 * for all other element classes, it defers to the superclass implementation. 
	 */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = (SchemaElementInfo) elementMap13.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
    *    <li>The new <i>autorange</i> attribute of a <i>graph</i> is an enumerated attribute. Method verifies its value 
    *    is one of the allowed choices.</li>
	 * </ul>
	 */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      if(e.getTag().equals(EL_GRAPH))
      {
         if(A_AUTORANGE.equals(attr))
            return(isValidEnumAttributeValue(value, AUTORANGE_CHOICES));
      }
      return(super.isValidAttributeValue(e, attr, value));
	}

   /**
	 * This method handles the actual details of migrating <code>Schema12</code> content to <code>Schema13</code>. It 
    * makes the following changes:
	 * <ul>
    *    <li>For every <i>graph</i> element encountered, the deprecated <i>auto</i> attribute is replaced by the new
    *    <i>autorange</i> attribute: <i>auto="true"</i> maps to <i>autorange="xyz"</i>, while <i>auto="false"</i> maps
    *    to <i>autorange="none"</i>. Both attributes are optional, and their implicit values ("false" and "none") mean
    *    the same thing.</li>
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
         
         // for graph elements, determine whether or not auto-ranging is enabled, and remove old "auto" attribute.
         boolean isAutoSet = false;
         if(EL_GRAPH.equals(elTag))
         {
            String autoVal = e.getAttributeValueByName(A_AUTO);
            if(autoVal != null) isAutoSet = autoVal.equals("true");
            e.removeAttributeByName(A_AUTO);
         }
         
         // migrate the element object's schema info.
         e.updateSchema(this, null);

         // if current element is a graph, then set the new "autorange" attribute explicitly if necessary.
         if(EL_GRAPH.equals(elTag) && isAutoSet)
            e.setAttributeValueByName(A_AUTORANGE, AUTORANGE_ALL);
         
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
