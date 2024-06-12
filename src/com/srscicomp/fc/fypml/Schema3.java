package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema3</code> is the encapsulation of the <em>DataNav/Phyplot</em> XML schema version 3. It extends 
 * <code>Schema2</code> and includes support for migrating schema version 2 documents to version 3. The following 
 * schema changes were introduced in this version:
 * <ul>
 *		<li>[Added 11/7/2005, for app version 0.8.0] Flag attribute "hide" was added to all data set elements ("series", 
 *		"pointSet, and "function"). If "false" (the default), an entry is included for the data element in the parent 
 *		graph's legend (if the legend exists). If "true", the legend entry is hidden. Formerly, an empty "title" string
 *    caused the legend entry to be hidden. When migrating a <code>Schema2</code> document, the "hide" attribute will 
 *    be set to "true" if the element's "title" is empty; the "title" attribute is left unchanged.</li>
 * 	<li>[Added 11/11/2005, for app version 0.8.0] Introduced a new element tag, "multiSet", representing a set of 
 *    point sets all sharing the same series of x-coordinate values. 
 * 	<li>[Added 11/17/2005, for app version 0.8.0] Introduced a new element tag, "shape", representing a symbol-like 
 * 	shape that can be any size and appear in graphs or figures. It can be used to create flowcharts in a figure 
 * 	element, or to annotate graphs.</li>
 * </ul>
 * <p>[<em>DataNav</em> superceded <em>Phyplot</em> in 2008.]</p>
 * @author 	sruffner
 */
class Schema3 extends Schema2
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/phyplot/namespace/schema3"); }
   @Override public int getVersion() { return(3); }
   @Override public String getLastApplicationVersion() { return("0.8.1"); }

	/** A multiple point set element -- first introduced in schema version 3. */
	final static String EL_MULTISET = "multiSet";

	/** A shape element -- first introduced in schema version 3. */
	final static String EL_SHAPE = "shape";

	/**
	 * The "size" attribute defined on the new "shape" element. It specifies, in real units, the size of the design box 
	 * (a square) within which the shape is rendered, in real units.
	 */
	final static String A_SIZE = "size";

	/**
	 * This element map contains <code>SchemaElementInfo</code> objects for each element that is new to this schema or 
	 * has a different attribute set compared to the previous schema.
	 */
	private static Map<String, SchemaElementInfo> elementMap3 = null;

	static
	{
		elementMap3 = new HashMap<String, SchemaElementInfo>();

		// new element class -- the 'multiSet' element
		elementMap3.put( EL_MULTISET, 
			new SchemaElementInfo( true, 
				new String[] {EL_EBAR}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SYMBOL, A_SYMBOLSIZE, A_SKIP, A_HIDE}, 
				new String[] {} ));

		// new element class -- the 'shape' element
		elementMap3.put( EL_SHAPE, 
			new SchemaElementInfo( false, 
				new String[] {EL_LABEL}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_LINETYPE, A_LOC, A_ROTATE, A_TYPE, A_SIZE, A_TITLE}, 
				new String[] {A_LOC} ));

		// revised element map entry for the 'pointSet' element -- added "hide" attribute
		elementMap3.put( EL_POINTSET, 
			new SchemaElementInfo( true, 
				new String[] {EL_EBAR}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SYMBOL, A_SYMBOLSIZE, A_SKIP, A_HIDE}, 
				new String[] {} ));

		// revised element map entry for the 'series' element -- added "hide" attribute
		elementMap3.put( EL_SERIES, 
			new SchemaElementInfo( true, 
				new String[] {EL_EBAR}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SYMBOL, A_SYMBOLSIZE, A_SKIP, A_HIDE, 
								  A_X0, A_DX, A_TYPE, A_BARWIDTH, A_BASELINE, A_FILLED}, 
				new String[] {} ));

		// revised element map entry for the 'function' element -- added "hide" attribute
		elementMap3.put( EL_FUNCTION, 
			new SchemaElementInfo( true, 
				new String[] {}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SYMBOL, A_SYMBOLSIZE, A_HIDE, A_X0, A_X1, A_DX}, 
				new String[] {} ));

		// revised element map entry for the 'graph' element -- added new children 'multiset' and 'shape'
		elementMap3.put( EL_GRAPH, 
			new SchemaElementInfo( false, 
				new String[] {EL_AXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH, 
								  EL_POINTSET, EL_FUNCTION, EL_SERIES, EL_MULTISET}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, A_CLIP, A_LOC, A_WIDTH, A_HEIGHT}, 
				new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

		// revised element map entry for the 'figure' element -- added new child node 'shape'
		elementMap3.put( EL_FIGURE, 
			new SchemaElementInfo( false, 
				new String[] {EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_BORDER, A_TITLE, A_LOC, A_WIDTH, A_HEIGHT}, 
				new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

	}

	/**
	 * Overridden to recognize the new element classes supported in schema 3.
	 * @see ISchema#isSupportedElementTag(String)
	 */
   @Override
	public boolean isSupportedElementTag(String elTag)
	{
      return(elementMap3.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
	}

	/**
	 * Overridden to provide schema element information for any element classes were added or revised in schema 3.
	 * @see ISchema#getSchemaElementInfo(String)
	 */
   @Override
	public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = (SchemaElementInfo) elementMap3.get(elTag);
		return((info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

   /**
    * In schema version 3, we added two new element classes:
    * <ul>
    * 	<li><code>EL_MULTISET</code> has one required <code>EL_EBAR</code> child.</li>
    * 	<li><code>EL_SHAPE</code> has no required children.</li>
    * </ul>
    * @see ISchema#hasRequiredChildren(BasicSchemaElement)
    */
   @SuppressWarnings("rawtypes")
   @Override
	public boolean hasRequiredChildren(BasicSchemaElement e)
	{
		String elTag = e.getTag();
		List children = e.getElementContent();
		if(EL_SHAPE.equals(elTag)) 
			return(true);
		else if(EL_MULTISET.equals(elTag))
			return(children.size() > 0 && EL_EBAR.equals(((BasicSchemaElement) children.get(0)).getTag()));

		return(super.hasRequiredChildren(e));
	}

	/**
	 * In schema version 3, we added two new element classes:
	 * <ul>
	 * 	<li><code>EL_MULTISET</code> has one required <code>EL_EBAR</code> child, which MUST be the first child.</li>
	 * 	<li><code>EL_SHAPE</code> has no required children.</li>
	 * </ul>
	 * <p>The "shape" element may appear as a child of a figure or graph element, but it is an optional child and its 
	 * addition did not affect the structural restrictions on the children content of those two elements.</p>
	 * @see ISchema#isValidChildAtIndex(BasicSchemaElement,String,int)
	 */
   @Override
	public boolean isValidChildAtIndex(BasicSchemaElement e, String childTag, int index)
	{
		// changes in schema 3 are enforced here
		String elTag = e.getTag();
		if(EL_MULTISET.equals(elTag))
			return(EL_EBAR.equals(childTag) && index == 0);

		// defer to the super class checking any element class for which schema changes have not occurred
		return(super.isValidChildAtIndex(e, childTag, index));
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
	 * 	<li>The new <code>A_SIZE</code> must be a nonnegative floating-pt measure in real units.</li>
	 *    <li>The <code>A_TYPE</code> attribute for a "shape" element is a multi-choice attribute selecting an adornment 
	 *    type. It can be any value in <code>Schema1.ADORN1_CHOICES</code>.
	 * 	<li>There are no other exceptions! All other attributes of the new "shape" and "multiset" elements already 
	 *    exist in previous schemas and can be validated by the superclass.</li>
	 * </ul>
	 * @see ISchema#isValidAttributeValue(BasicSchemaElement,String,String)
	 */
   @Override
	public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
		if(A_SIZE.equals(attr))
			return(isValidMeasureAttributeValue(value, false, false));
		else if(A_TYPE.equals(attr) && (e.getTag()).equals(EL_SHAPE))
			return(isValidEnumAttributeValue(value, ADORN1_CHOICES));

		return(super.isValidAttributeValue(e, attr, value));
	}

   /**
    * The new "multiSet" element's text content must be a comma-separated list of datum tuples, where each such tuple 
    * is a whitespace-separated list of two or more floating-point numbers. All tuples must contain the same number of 
    * tokens.
    * <p>For all other elements, the method defers to the super-class implementation.</p>
    * <p>Note that this method was added 11/3/2006, well after <code>Schema3</code> was released.</p>
    */
	@Override
   public boolean isValidTextContent(BasicSchemaElement e, String text)
   {
      if(EL_MULTISET.equals(e.getTag()))
      {
         if(text == null || text.length() == 0) return(true);

         StringTokenizer st = new StringTokenizer(text, ",");

         int nPerTuple = -1;
         while(st.hasMoreTokens())
         {
            String tuple = st.nextToken().trim();
            
            // 29apr2014: Allow for possibility that text content ENDS with a comma followed only by whitespace, in
            // which case we might get a zero-length tuple here.
            if(tuple.length() == 0) return(!st.hasMoreTokens());

            StringTokenizer tupleTokenizer = new StringTokenizer(tuple);
            int nTokens = tupleTokenizer.countTokens();
            if(nTokens < 2) return(false);
            if(nPerTuple == -1) nPerTuple = nTokens;
            else if(nTokens != nPerTuple) return(false);

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
	 * This method handles the actual details of migrating schema version 2 content to schema version 3. It makes the 
	 * following changes:
	 * <ul>
	 * 	<li>For each data set element encountered, we revise element's schema info to include the "hide" attribute. 
	 *    The implicit value for this optional attribute is "false" -- meaning that the legend entry corresponding to 
	 * 	this element will be rendered. However, in the previous schema, a data set's legend entry was hidden if the 
	 * 	element's "title" attribute was an empty string. To migrate properly, the method check's the "title" attribute
	 * 	value and explicitly sets the "hide" attribute if the title is an empty string. The title itself is left
	 * 	unchanged.</li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
	 */
   @Override
	public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
	{
		if(oldSchema.getVersion() != getVersion() - 1) 
			throw new XMLException("A schema instance can only migrate from the previous version.");

		// update the content of the old schema in place...
		Stack<BasicSchemaElement> elementStack = new Stack<BasicSchemaElement>();
		elementStack.push((BasicSchemaElement) oldSchema.getRootElement());
		while(!elementStack.isEmpty())
		{
			BasicSchemaElement e = (BasicSchemaElement) elementStack.pop();
			String elTag = e.getTag();

			// migrate the element object's schema info
			e.updateSchema(this, null);

			// If any data set element has an implicit or empty "title" attribute, then its legend entry should be hidden 
			// -- so we must explicitly set the element's boolean "hide" attribute, which was added in Schema3. We don't 
			// need to do this for the "multiSet" element, which did not exist prior to Schema3.
			if(EL_POINTSET.equals(elTag) || EL_SERIES.equals(elTag) || EL_FUNCTION.equals(elTag))
			{
				String title = e.getAttributeValueByName(A_TITLE);
				if(title == null || title.length() == 0)
					e.setAttributeValueByName(A_HIDE, "true");
			}

			// if element has any children, push them onto the stack so that we check them as well!
			for(int i=0; i<e.getChildCount(); i++)
				elementStack.push((BasicSchemaElement) e.getChildAt(i));
		}

		// the content model now conforms to this schema.  We get the root element from the old schema and install it as 
		// the root of this schema, then empty the old schema. We also remember the original schema version of the 
      // migrated content.
      originalVersion = oldSchema.getOriginalVersion();
		setRootElement(oldSchema.getRootElement(), false);
		oldSchema.setRootElement(null, false);
	}
}
