package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema14</code> is the encapsulation of the <i>DataNav</i> figure model XML schema version 14. It extends 
 * {@link Schema13} and includes support for migrating schema version 13 documents to version 14.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul>
 *    <li>(04jun2013) <i>DataNav</i> version 4.2.0 enhanced the graph's automated legend to include entries for any
 *    child <i>raster</i> nodes. If the raster is in the histogram display mode, the legend entry is a horizontal bar
 *    (styled IAW the raster node's properties) followed by the node's title; otherwise, it is a solid horizontal line.
 *    Added the <i>legend</i> flag to the <i>raster</i> node so that user can control whether or not to include the
 *    node in the automated legend. Since the default value for this flag is <i>true</i> (for consistency with the same 
 *    attribute defined on <i>trace</i> and <i>function</i> nodes!), we explicitly set it to <i>false</i> on all raster
 *    nodes during schema migration to preserve the appearance of existing figures.</li>
 *    <li>(31dec2013) Added optional <i>labelOffset</i> attribute to the <i>legend</i> node to give author control over
 *    the gap between the end of the trace lines in the automated graph legend and the start of the corresponding text
 *    labels. Prior to this change (v4.4.0), the offset was hard-coded at 0.1in. So that existing figures are not
 *    affected, 0.1in was chosen as the attribute's default value. Restricted to [0-5in]; physical units only. No
 *    changes required during schema migration.</li>
 * </ul>
 * </p>
 * 
 * @author 	sruffner
 */
class Schema14 extends Schema13
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema14"); }
   @Override public int getVersion() { return(14); }
   @Override public String getLastApplicationVersion() { return("4.4.0"); }

   /** Default for the "labelOffset" attribute of the "legend" element. */
   final static String DEFAULT_LEGEND_LABELOFFSET = "0.1in";

   /**
	 * This element map contains {@link SchemaElementInfo SchemaElementInfo} objects for each element that
	 * is new to this schema or has a different attribute set compared to the previous schema.
	 */
	private static Map<String, SchemaElementInfo> elementMap14 = null;

	static
	{
		elementMap14 = new HashMap<String, SchemaElementInfo>();
		
      // (04jun2013) Added boolean attribute "legend" to the "raster" element. Optional. Default value is "true".
      elementMap14.put( EL_RASTER, new SchemaElementInfo( false, 
            new String[] {}, 
            new String[] {A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_MODE, A_BASELINE, A_FILLED, A_XOFF, A_HEIGHT, A_SPACER, A_NBINS, A_AVG, A_LEGEND, A_SRC}, 
            new String[] {A_SRC} ));
      
      // (31dec2013) Added measured attribute "labelOffset" to "legend". Optional. Default = "0.1in".
      elementMap14.put( EL_LEGEND, 
            new SchemaElementInfo( false, 
               new String[] {}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, 
                             A_ROTATE, A_SPACER, A_SIZE, A_LEN, A_MID, A_HIDE, A_LABELOFFSET, A_LOC}, 
               new String[] {A_LOC} ));
	}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in this schema version; else defers 
    * to the superclass implementation.
    */
   @Override public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap14.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in this schema version; 
	 * for all other element classes, it defers to the superclass implementation. 
	 */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = (SchemaElementInfo) elementMap14.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
    *    <li>The new <i>legend</i> attribute of a <i>raster</i> is a boolean attribute. Must be "true" or "false".</li>
    *    <li>The new <i>labelOffset</i> attribute of the <i>legend</i> node is a non-negative measure restricted to
    *    physical units. We rely on the super class implementation, since "labelOffset" on axis nodes has identical
    *    restrictions. Remember that range-checking does not happen in schema validation.</li>
	 * </ul>
	 */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      if(e.getTag().equals(EL_RASTER))
      {
         if(A_LEGEND.equals(attr))
            return("true".equals(value) || "false".equals(value));
      }
      return(super.isValidAttributeValue(e, attr, value));
	}

   /**
	 * This method handles the actual details of migrating <code>Schema13</code> content to <code>Schema14</code>. It 
    * makes the following changes:
	 * <ul>
    *    <li>For every <i>raster</i> element encountered, the new <i>legend</i> attribute is explicitly set to "false",
    *    since a legend entry was never drawn for a raster node prior to this schema version.</li>
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

         // explicitly set new "legend" attribute to "false" on all raster node (the implicit default is "true")
         if(EL_RASTER.equals(elTag))
            e.setAttributeValueByName(A_LEGEND, "false");
         
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
