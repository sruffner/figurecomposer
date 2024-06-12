package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.util.Utilities;
import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema5</code> is the encapsulation of the <em>DataNav/Phyplot</em> XML schema version 5. It extends 
 * <code>Schema4</code> and includes support for migrating schema version 4 documents to version 5. The following 
 * schema changes were introduced in this version:
 * <ul>
 *		<li>[Mods thru 10/11/2006, for app version 1.0.0] Each <em>DataMav</em> document can now contain only a single 
 *    figure! The old root node -- the "fyp" element -- has been completely removed, and the "figure" element now 
 *    serves as the root. All attributes that were defined on the old "fyp" node are now defined on "figure". To migrate 
 *    from a <code>Schema4</code> document, we must save all of the old "fyp" attributes and set them on the first 
 *    "figure" node parsed. Also, only the first "figure" node in the <code>Schema4</code> document is incorporated into 
 *    <code>Schema5</code>. Since all users to date have been creating single-figure documents, this was considered a 
 *    safe simplification of the migration process.</li>
 * 	<li>[Mods thru 10/11/2006, for app version 1.0.0] Because of the major changes in how <em>Phyplot</em> renders a 
 *    figure (the Batik engine was replaced by a simpler in-house rendering framework), a positive rotation angle now 
 *    causes a CCW rotation, instead of the CW rotation in previous versions. Therefore, during migration, the value of 
 *    the "rotate" attribute is negated wherever it is explicitly defined.</li>
 *    <li>[Mods thru 10/17/2006, for app version 1.0.0] "Horizontal" calibration bars for a polar graph would really 
 *    be arcs. In previous versions they were drawn as straight horizontal lines, but now they are turned off 
 *    altogether. If the containing graph is polar (not semilogR), the author probably meant to create a calibration 
 *    bar for the radial (vertical) axis. During migration, we switch "horizontal" calibration bars in polar graphs to 
 *    "vertical" ones, via the "horiz" attribute.</li>
 * </ul>
 * <p>[<em>DataNav</em> superceded <em>Phyplot</em> in 2008.]</p>
 * @author 	sruffner
 */
class Schema5 extends Schema4
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/phyplot/namespace/schema5"); }
   @Override public int getVersion() { return(5); }
   @Override public String getLastApplicationVersion() { return("1.0.0"); }

	/**
	 * This element map contains <code>SchemaElementInfo</code> objects for each element that is new to this schema or 
	 * has a different attribute set compared to the previous schema.
	 */
	private static Map<String, SchemaElementInfo> elementMap5 = null;

	static
	{
		elementMap5 = new HashMap<String, SchemaElementInfo>();

		// revised element map entry for the 'figure' element, which now serves as the root element as well. This 
      // includes all the attributes in the Schema4 'fyp' element, all of which are required, plus several 
      // figure-specific attributes.
      elementMap5.put( EL_FIGURE, 
            new SchemaElementInfo( false, 
               new String[] {EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH}, 
               new String[] {A_FONT, A_PSFONT, A_SUBSTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                  A_STROKECOLOR, A_CAP, A_CAPSIZE, A_MID, A_SPACER, A_LABELOFFSET, A_PERLOGINTV, A_DIR, A_LEN, 
                  A_FMT, A_GAP, A_BORDER, A_TITLE, A_LOC, A_WIDTH, A_HEIGHT}, 
               new String[] {A_FONT, A_PSFONT, A_SUBSTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                  A_STROKECOLOR, A_CAP, A_CAPSIZE, A_MID, A_SPACER, A_LABELOFFSET, A_PERLOGINTV, A_DIR, A_LEN, 
                  A_FMT, A_GAP, A_LOC, A_WIDTH, A_HEIGHT} ));
	}

   /**
	 * Overridden to validate any elements added in schema version 5 (none so far), but otherwise defers to the 
	 * superclass implementation. Since the old "fyp" root element was removed in schema 5, the method excludes that tag 
	 * as a valid element tag!
	 * @see ISchema#isSupportedElementTag(String)
	 */
   @Override
	public boolean isSupportedElementTag(String elTag)
	{
      if(EL_FYP.equals(elTag)) return(false);
      else return(elementMap5.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
	}

	/**
	 * Overridden to provide schema element information for any element class added or revised in schema version 5; for
    * all other element classes, it defers to the superclass implementation. 
	 * @see ISchema#getSchemaElementInfo(String)
	 */
   @Override
	public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = (SchemaElementInfo) elementMap5.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

   /** Overridden to enforce the fact that the root node is now the "figure" element as of schema version 5. */
	@Override boolean isValidRootTag(String tag) { return(EL_FIGURE.equals(tag)); }

	/** Overridden to reflect fact that the root node is now the "figure" element as of schema version 5. */
	public String getRootTag() { return(EL_FIGURE); }
	
   /**
	 * This method handles the actual details of migrating schema version 4 content to schema version 5.  It makes the 
	 * following changes:
	 * <ul>
	 * 	<li>For each "rotate" attribute encountered, the rotation angle value (a float) is negated. As of schema 
    *    version 5, a positive rotation angle yields CCW rotation; in prior versions, it caused CW rotation.</li>
	 * 	<li>The old "fyp" root node was removed in schema version 5, replaced by the "figure" element. Only the first 
    *    "figure" node will be migrated, all other "figure" nodes are lost. This imperfect migration was considered 
    *    acceptable because none of <em>Phyplot</em>'s users to-date had created multi-figure documents. All of the 
    *    required attributes from the old "fyp" node are set on this first "figure" node, unless they were already 
    *    explicitly set on that node. If the old "fyp" node happened to contain no figures at all (an unlikely but 
    *    possible case), then a "figure" node will be created as the root node in the current schema.</li>
    *    <li>For each "calib" element encountered: If the parent graph's type is "polar" and the calibration bar is 
    *    currently horizontal, then it is switched to vertical ("horiz" = "false"). This is because <em>Phyplot</em> 
    *    no longer renders a calibration bar associated with the theta axis of a polar graph. Before, both horizontal 
    *    and vertical bars were associated with the radial axis.</li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
	 */
	public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
	{
		if(oldSchema.getVersion() != getVersion() - 1) 
			throw new XMLException("A schema instance can only migrate from the previous version.");

		// get the (name,value) pairs for all attributes of the "fyp" root element in the old schema. Since all are 
      // required, we will get a (name,value) pair for each and every attribute.
		BasicSchemaElement oldRoot = (BasicSchemaElement) oldSchema.getRootElement();
      List<String> oldRootAttrs = oldRoot.getAttributes();

      // if the old schema instance is devoid of figures, just create an empty "figure" node that conforms to the new 
      // schema. Transfer the values of all required attributes from the old "fyp" root to this "figure" node, which is 
      // the root node in the new schema.
      if(oldRoot.getChildCount() == 0)
      {
         BasicSchemaElement figRoot = (BasicSchemaElement) createElement(EL_FIGURE);
         for(int i=0; i < oldRootAttrs.size(); i+=2)
            figRoot.setAttributeValueByName(oldRootAttrs.get(i), oldRootAttrs.get(i+1));
         setRootElement(figRoot, false);
         oldSchema.setRootElement(null, false);
         return;
      }

      // remove the first child under the old "fyp" root, which will be a "figure" node. This will become the new root 
      // node in the current schema, and any other "figure" elements in the old schema are lost! 
      BasicSchemaElement figRoot = (BasicSchemaElement) oldRoot.getChildAt(0);
      oldRoot.remove(0);

		// update the content of the single figure/root node in place (any remaining content of old schema is ignored)...
		Stack<BasicSchemaElement> elementStack = new Stack<BasicSchemaElement>();
		elementStack.push(figRoot);
		while(!elementStack.isEmpty())
		{
			BasicSchemaElement e = elementStack.pop();
			String elTag = e.getTag();

			// migrate the element object's schema info
			e.updateSchema(this, null);

         // the new root "figure" element: must explicitly set all attributes from the old "fyp" node (they are 
         // required) on this node, unless they are already explicitly set (this is possible because the old "figure" 
         // node could override certain inheritable attribute values from the old "fyp" node).
         if(EL_FIGURE.equals(elTag)) for(int i = 0; i < oldRootAttrs.size(); i += 2)
         {
            if(e.getAttributeValueByName(oldRootAttrs.get(i)) == null)
               e.setAttributeValueByName(oldRootAttrs.get(i), oldRootAttrs.get(i+1));
         }

         // for each element having its "rotate" attribute explicitly set, negate its value.
         if(e.hasAttribute(A_ROTATE))
         {
            String value = e.getAttributeValueByName(A_ROTATE);
            if(value != null && value.length() > 0)
            {
               try 
               {
                  double d = Double.parseDouble(value);
                  e.setAttributeValueByName(A_ROTATE, Utilities.toString(-d, 4, 1));
                  // note that rotate attr is limited to [-180..180] and increments of 0.1 deg is plenty!
               }
               catch(NumberFormatException nfe)
               {
                  // THIS SHOULD NEVER HAPPEN
               }
            }
         }

         // each "calib" element in a polar (NOT semilogR) graph, is associated with the radial axis ("horiz" is false)
         // -- calibration bars associated with the theta axis are never rendered.
         if(EL_CALIB.equals(elTag) && 
               GRAPH_TYPE_POLAR.equals(((BasicSchemaElement)e.getParent()).getAttributeValueByName(A_TYPE)))
         {
            e.setAttributeValueByName(A_HORIZ, "false");
         }
            
         // if element has any children, push them onto the stack so that we check them as well! 
			for(int i=0; i<e.getChildCount(); i++)
			{
				BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
				elementStack.push(child);
			}
		}

		// the content model of the single figure/root node conforms to this schema -- so make it the root element now.
      // We also empty the old schema. We also remember the original schema version of the migrated content.
      originalVersion = oldSchema.getOriginalVersion();
		setRootElement(figRoot, false);
		oldSchema.setRootElement(null, false);
	}
}
