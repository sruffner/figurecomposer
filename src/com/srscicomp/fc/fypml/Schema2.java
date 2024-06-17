package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema2</code> is the encapsulation of the <em>DataNav/Phyplot</em> XML schema version 2. It extends 
 * <code>Schema1</code> and includes support for migrating schema version 1 documents to version 2. The following 
 * schema changes were introduced in this version:
 * <ul>
 * 	<li>Schema changes to permit independent stroke characteristics for a graph's primary and secondary axis grid 
 * 	lines: The "graph" element's "showGrid" attribute was removed. A new "gridline" element was introduced, that 
 *		exists only in the context of a parent graph. Every "graph" element must have 2 "gridline" children, located at 
 *		indices 2 and 3 in the child list. The first grid line element defines the stroke characteristics for the parent 
 *		graph's primary axis grid lines; the second grid line defines the stroke characteristics for the secondary axis 
 *		grid lines. Each "gridline" element has no content and only 3 attributes: the inheritable "strokeWidth" and 
 *		"strokeColor" styles, plus a "lineType" attribute that defines the stroke dash array to be applied when stroking 
 *		the grid lines. A graph's "legend" child, if present, is now located at index 4.</li>
 *		<li>Schema changes that let author specify stroke characteristics of a data set's error bars that differ from 
 *		those applied when rendering the adorned polyline that connects the data points in the set. The attributes 
 *		"hideErrors", "cap" and "capSize" were removed from the "pointSet" and "series" elements. A new "ebar" element 
 *		was introduced. Both "pointSet" and "series" became mixed-content elements, each having an "ebar" element as a 
 *		single required child. The format of the text content of each element class was unchanged. The new "ebar" element
 *		governs the appearance of the error bars (or error traces in the case of a "series" element in the "trace" display
 *		mode). It has no content and 6 attributes: the "cap" and "capSize" attributes formerly defined on the parent data 
 *    set element, plus rendering styles "lineType", "strokeWidth", "strokeColor", and "fillColor". The latter four
 *    attributes are all inheritable. When "lineType" is hidden, all error bars are hidden -- which is equivalent to 
 *    setting the old "hideErrors" attribute to "true".</li>
 *		<li>[Added 5/20/2005, app version 0.7.3] A new attribute, "skip", was added to the "pointSet" and "series" 
 *		elements. Its value is a positive integer N.  If N &gt; 1, then every Nth point in the data set is plotted when 
 *		the owner data element is rendered. If N == 1, then all well-defined points are rendered. The attribute is 
 *		optional and defaults to 1 -- otherwise, files conforming to <code>Schema2</code> prior to this change would no 
 *    longer conform, and a new schema version would be required.</li>
 * </ul>
 * <p>[<em>DataNav</em> superceded <em>Phyplot</em> in 2008.]</p>
 * @author 	sruffner
 */
class Schema2 extends Schema1
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/phyplot/namespace/schema2"); }
   @Override public int getVersion() { return(2); }
   @Override public String getLastApplicationVersion() { return("0.7.4"); }

	/**
	 * A grid line element, governing the appearance of the primary axis or secondary axis grid lines of a graph.  
	 * Introduced in schema version 2.
	 */
	final static String EL_GRIDLINE = "gridline";

	/**
	* An error bar element, governing the appearance of the error bars of a point set or the error bars/traces of 
	* a series element.
	*/
	final static String EL_EBAR = "ebar";

	/**
	 * The "skip" attribute, added in <em>Phyplot</em> 0.7.3, is defined on the point set and series elements. Its value 
	 * is a positive integer N such that every Nth point in the data set is plotted when the element is rendered. Useful 
	 * when rendering a data set that is very large with closely spaced points. Since this attribute was added AFTER 
	 * schema version 2 was introduced, it is imperative that it be optional and have a default value of 1, which means 
	 * that no points are skipped.
	 */
	final static String A_SKIP = "skip";


	/**
	 * This element map contains <code>SchemaElementInfo</code> objects for each element that is new to this schema or 
	 * has a different attribute set compared to the previous schema.
	 */
	private static final Map<String, SchemaElementInfo> elementMap2;

	static
	{
		elementMap2 = new HashMap<>();

		// new element class: the 'gridline' element
		elementMap2.put( EL_GRIDLINE, 
			new SchemaElementInfo( false, 
				new String[] {}, 
				new String[] {A_STROKEWIDTH, A_STROKECOLOR, A_LINETYPE}, 
				new String[] {} ));

		// new element class: the 'ebar' element
		elementMap2.put( EL_EBAR, 
			new SchemaElementInfo( false, 
				new String[] {}, 
				new String[] {A_STROKEWIDTH, A_STROKECOLOR, A_FILLCOLOR, A_LINETYPE, A_CAP, A_CAPSIZE}, 
				new String[] {} ));

		// revised element map entry for the 'graph' element
		elementMap2.put( EL_GRAPH, 
			new SchemaElementInfo( false, 
				new String[] {EL_AXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_LINE, EL_GRAPH, EL_POINTSET, 
					           EL_FUNCTION, EL_SERIES}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, A_CLIP, A_LOC, A_WIDTH, A_HEIGHT}, 
				new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

		// revised element map entry for the 'pointSet' element
		elementMap2.put( EL_POINTSET, 
			new SchemaElementInfo( true, 
				new String[] {EL_EBAR}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SYMBOL, A_SYMBOLSIZE, A_SKIP}, 
				new String[] {} ));

		// revised element map entry for the 'series' element
		elementMap2.put( EL_SERIES, 
			new SchemaElementInfo( true, 
				new String[] {EL_EBAR}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SYMBOL, A_SYMBOLSIZE, A_SKIP, 
								  A_X0, A_DX, A_TYPE, A_BARWIDTH, A_BASELINE, A_FILLED}, 
				new String[] {} ));

	}

	/** 
    * Overridden to recognize the new element classes supported in schema 2, "gridline" and "ebar".
	 * @see ISchema#isSupportedElementTag(String)
	 */
   @Override
	public boolean isSupportedElementTag(String elTag)
	{
      return(elementMap2.containsKey(elTag) || super.isSupportedElementTag(elTag));
	}

	/**
	 * Overridden to provide the schema element information for the new element classes "gridline" and "ebar, plus 
	 * revised information for the existing element classes "graph", "pointSet", and "series".
	 * @see ISchema#getSchemaElementInfo(String)
	 */
   @Override
	public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = elementMap2.get(elTag);
		return((info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * In schema version 2, the "graph" element has four required children:  the first two define the axes while the 
    * second two define the primary and secondary axis grid lines. In addition, the "pointSet" and "series" elements 
    * both require a single "ebar" child element. 
	 * @see ISchema#hasRequiredChildren(BasicSchemaElement)
	 */
   @SuppressWarnings("rawtypes")
@Override
	public boolean hasRequiredChildren(BasicSchemaElement e)
	{
		String elTag = e.getTag();
		List children = e.getElementContent();
		if(EL_POINTSET.equals(elTag) || EL_SERIES.equals(elTag))
			return((!children.isEmpty()) && EL_EBAR.equals(((BasicSchemaElement) children.get(0)).getTag()));
		else if(EL_GRAPH.equals(elTag))
		{
			// graph element must have at least four children, the first two must be axis elements, and the next two must be 
			// grid line elements
			if(children.size() < 4) return(false);
			String tag0 = ((BasicSchemaElement) children.get(0)).getTag();
			String tag1 = ((BasicSchemaElement) children.get(1)).getTag();
			String tag2 = ((BasicSchemaElement) children.get(2)).getTag();
			String tag3 = ((BasicSchemaElement) children.get(3)).getTag();
			return(EL_AXIS.equals(tag0) && EL_AXIS.equals(tag1) && EL_GRIDLINE.equals(tag2) && EL_GRIDLINE.equals(tag3));
		}

		return(super.hasRequiredChildren(e));
	}

	/**
	 * In schema version 2, the child content restrictions of several elements were revised:
	 * <ul>
	 * 	<li>"graph": The first four children MUST exist, the first two being axis elements and the next two being 
    *    gridline elements. The graph may contain at most one legend; if present, it MUST be the fifth child.</li>
	 * 	<li>"pointSet": Must possess one and only one child, an "ebar" element governing the appearance of the point 
    *    set's error bars.</li>
	 * 	<li>"series: Must possess one and only one child, an "ebar" element governing the appearance of the series' 
    *    error bars or error traces.</li>
	 * </ul>
	 * @see ISchema#isValidChildAtIndex(BasicSchemaElement,String,int)
	 */
    @SuppressWarnings("rawtypes")
    @Override
	public boolean isValidChildAtIndex(BasicSchemaElement e, String childTag, int index)
	{
		// changes in schema 2 are enforced here
		String elTag = e.getTag();
		if(EL_POINTSET.equals(elTag) || EL_SERIES.equals(elTag))
			return(EL_EBAR.equals(childTag) && index == 0);
		else if(EL_GRAPH.equals(elTag))
		{
			SchemaElementInfo eInfo = getSchemaElementInfo(elTag);
			if(!eInfo.isChildAllowed(childTag)) return(false);
			List existingChildren = e.getElementContent();
			if(EL_AXIS.equals(childTag))
				return(index == 0 || index == 1);
			else if(EL_GRIDLINE.equals(childTag))
				return(index == 2 || index == 3);
			else if(EL_LEGEND.equals(childTag))
				return(index == 4);
			else
			{
				if(existingChildren.size() < 4) return(false);
				int minIndex = 4;
				if(existingChildren.size() > 4 && EL_LEGEND.equals(((BasicSchemaElement) existingChildren.get(4)).getTag()))
					++minIndex;
				return(minIndex <= index);
			}
		}

		// defer to the super class checking any element class for which schema changes have not occurred
		return(super.isValidChildAtIndex(e, childTag, index));
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
	 * 	<li>The "skip attribute was introduced into <code>Schema2</code> in <em>Phyplot</em> version 0.7.3. Its value 
    *    must be a positive integer.</li>
	 * 	<li>There are no other exceptions! The attributes of the new element classes EL_GRIDLINE and EL_EBAR are all 
	 * 	common attributes that are validated the same way regardless of the identity of the owner element.</li>
	 * </ul>
	 * @see ISchema#isValidAttributeValue(BasicSchemaElement,String,String)
	 */
   @Override
	public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
		if(A_SKIP.equals(attr))
			return(isValidIntegerAttributeValue(value, 1, Integer.MAX_VALUE));
		else 
			return(super.isValidAttributeValue(e, attr, value));
	}

	/**
	 * Verify that the specified value is valid for an integer attribute. The value must parse as an integer and must 
	 * lie in the specified range.
	 * @param value The test value.
	 * @param minVal The minimum allowed value for the integer.
	 * @param maxVal The maximum allowed value for the integer.
	 * @return <code>True</code> if test value is valid for an integer attribute, as described.
	 */
	boolean isValidIntegerAttributeValue(String value, int minVal, int maxVal)
	{
		boolean ok;
		try 
		{
			int i = Integer.parseInt(value);
			ok = (i >= minVal) && (i <= maxVal);
		}
		catch(NumberFormatException nfe) { ok = false; }
		return(ok);
	}

	/**
	 * This method handles the actual details of migrating schema version 1 content to schema version 2. It makes the 
	 * following changes:
	 * <ul>
	 * 	<li>For each graph element encountered, the method inserts two "gridline" child elements at indices 2 and 
	 * 	3 in the graph's child list. Both gridline elements will have inherited stroke width and color, and their 
	 * 	"lineType" attributes will be implicit if the graph's old "showGrid" attribute is implicit or "false". Else, 
	 * 	they are both set to "solid". The old "showGrid" attribute is removed.</li>
	 * 	<li>For each "pointSet" or "series" element encountered, the method inserts a single "ebar" child element, 
    *    initially with no explicit attributes. The "cap" and "capSize" attributes are transferred from the data set 
    *    element to the error bar element. If the old "hideErrors" attribute was set on the data set, then the error 
    *    bar element's "lineType" attribute is set to "hidden"; else it is set to "solid". Finally, the three 
    *    attributes "hideErrors", "cap", and "capSize" are removed from the explicit attribute list of the parent data 
    *    set element.</li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
	 */
   @Override
	public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
	{
		if(oldSchema.getVersion() != getVersion() - 1) 
			throw new XMLException("A schema instance can only migrate from the previous version.");

		// update the content of the old schema in place...
		Stack<BasicSchemaElement> elementStack = new Stack<>();
		elementStack.push((BasicSchemaElement) oldSchema.getRootElement());
		while(!elementStack.isEmpty())
		{
			BasicSchemaElement e = elementStack.pop();
			String elTag = e.getTag();

			if(EL_GRAPH.equals(elTag))
			{
				// if obsolete "showGrid" attribute is explicitly set, we need to remember its value, then remove it 
				// from the graph's attribute list
				String showGridValue = e.getAttributeValueByName(A_SHOWGRID);
				boolean showGrid = "true".equals(showGridValue);
				e.removeAttributeByName(A_SHOWGRID);

				// add two "gridline" children at indices 2 and 3.  For each, set the "lineType" attribute to "solid" 
				// if the old "showGrid" attribute is set on the parent graph.  Else, leave "lineType" as implicit.
				for( int i=0; i<2; i++ )
				{
					BasicSchemaElement gridline = (BasicSchemaElement) createElement(EL_GRIDLINE);
					if(showGrid) gridline.setAttributeValueByName(A_LINETYPE, LINETYPE_SOLID);
					e.insert(gridline, 2);
				}
			}
			else if(EL_POINTSET.equals(elTag) || EL_SERIES.equals(elTag))
			{
				// remove the attributes "hideErrors", "cap" and "capSize" from the element's explicit attribute list, but 
				// remember their values so we can set up the "ebar" child appropriately
				boolean hideErrors = "true".equals(e.getAttributeValueByName(A_HIDEERRORS));
				e.removeAttributeByName(A_HIDEERRORS);
				String cap = e.getAttributeValueByName(A_CAP);
				e.removeAttributeByName(A_CAP);
				String capSize = e.getAttributeValueByName(A_CAPSIZE);
				e.removeAttributeByName(A_CAPSIZE);

				// add a single "ebar" child to the data set element. If the old "hideErrors" attribute was set, then we 
				// set the error bar element's "lineType" to hidden. We also copy the values of the "cap" and "capSize" 
				// attrs, if they were explicitly set on the data set element.
				BasicSchemaElement ebar = (BasicSchemaElement) createElement(EL_EBAR);
				ebar.setAttributeValueByName(A_LINETYPE, hideErrors ? LINETYPE_HIDDEN : LINETYPE_SOLID);
				if(cap != null) ebar.setAttributeValueByName(A_CAP, cap);
				if(capSize != null) ebar.setAttributeValueByName(A_CAPSIZE, capSize);
				e.setAllowsChildren(true);		// formerly, it did NOT allow children!
				e.add(ebar);
				
			}

			// finally, migrate the element object's schema info.  We skip over any "gridline" or "ebar" elements, since 
			// these were added during migration of the parent graph or data set, respectively.
			if(!(EL_GRIDLINE.equals(elTag) || EL_EBAR.equals(elTag))) 
				e.updateSchema(this, null);

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
