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
 * <code>Schema4</code> is the encapsulation of the <em>DataNav/Phyplot</em> XML schema version 4. It extends 
 * <code>Schema3</code> and includes support for migrating schema version 3 documents to version 4. The following 
 * schema changes were introduced in this version:
 * <ul>
 *		<li>[Mods 02/09/2006, for app version 0.8.2] The "line" element was substantially changed. Instead of supporting 
 *    marker symbols only at the endpoints and midpoint of the line segment, the element now accepts any number of 
 *    "shape" elements as children. Thus, an author can create a line with any number of symbols drawn along its length 
 *    or anywhere else. The endpoint/midpoint attributes "p0Cap", "p0CapSize", "p1Cap", "p1CapSize", "midCap", and 
 *    "midCapSize" have been removed from the schema. To migrate from a <code>Schema3</code> document, we must replace 
 *    these attributes with appropriately defined "shape" children.</li>
 * 	<li>[Mods thru 02/18/2006, for app version 0.8.2] The "symbol" and "symbolSize" attributes on data elements have 
 * 	been replaced by a single required "symbol" child element. This element is analogous to the generic "shape" 
 * 	element, but it does not allow any children. It defines the appearance of symbols rendered at the locations of 
 * 	well-defined points in the data set. The new "symbol" element includes a "title" attribute, the value of which 
 * 	can be empty (the default) or a single character. In the latter case, the character is centered inside the 
 * 	symbol. To migrate from a <code>Schema3</code> document, we must replace the two attributes with a single "symbol" 
 *    child with "type" and "size" attributes set accordingly. In addition, since symbols were always stroked solid 
 * 	regardless of the "lineType" of the data set, we must explicitly set the "symbol" child's "lineType" attribute 
 * 	(which can be inherited from the parent data set!) to "solid". Since "symbolSize" and "symbol" had document-level 
 * 	defaults defined on the "fyp" element, we need to take that into account while migrating. The corresponding 
 * 	"type" and "size" attributes on the "symbol" element do NOT have a document-level default.</li>
 * 	<li>[Mod 02/10/2006, for app version 0.8.2] To completely eliminate the attribute names "symbol" and "symbolSize" 
 * 	from the schema, we made several other changes: (1) The attributes were removed from the "fyp" element, where 
 * 	they specified document-level defaults. (2) The "symbolSize" attribute was renamed "size" on the "legend" 
 * 	element, but its usage was not changed.</li>
 * 	<li>[Mod 02/12/2006, for app version 0.8.2] Added "skip" attribute to "function" element -- even though it is not 
 * 	needed -- so that it is more "like" the other data set classes. No change is required during migration, since 
 * 	the implicit value of "skip" implies no skipping in the data plotted.</li>
 * </ul>
 * <p>[<em>DataNav</em> superceded <em>Phyplot</em> in 2008.]</p>
 * @author 	sruffner
 */
class Schema4 extends Schema3
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/phyplot/namespace/schema4"); }
   @Override public int getVersion() { return(4); }
   @Override public String getLastApplicationVersion() { return("0.8.2"); }

	/**
	 * A symbol element, introduced in <code>Schema4</code>, governs the appearance of symbols rendered at data point 
    * locations on any of the data set elements. It replaced the data set element's "symbol" and "symbolSize" attrs.
	 */
   final static String EL_SYMBOL = "symbol";

	/** The default (implicit) value for the "type" attribute of the new "symbol" element. */
	final static String DEFAULT_SYMBOL_TYPE = ATTRVAL_NONE;

	/** The default (implicit) value for the "size" attribute of the new "symbol" element. */
	final static String DEFAULT_SYMBOL_SIZE = "0.1in";

	/**
	 * This element map contains <code>SchemaElementInfo</code> objects for each element that is new to this schema or 
	 * has a different attribute set compared to the previous schema.
	 */
	private static Map<String, SchemaElementInfo> elementMap4 = null;

	static
	{
		elementMap4 = new HashMap<String, SchemaElementInfo>();

		// new element class: the 'symbol' element
		elementMap4.put( EL_SYMBOL, 
			new SchemaElementInfo( false, 
				new String[] {}, 
				new String[] {A_STROKEWIDTH, A_STROKECOLOR, A_FILLCOLOR, A_LINETYPE, A_TYPE, A_SIZE, A_TITLE}, 
				new String[] {} ));

		// revised element map entry for the 'line' element. Endpoint/midpoint cap attributes no longer exist; allow 
		// 'shape' element as a child.
		elementMap4.put( EL_LINE, 
			new SchemaElementInfo( false, 
				new String[] {EL_LABEL, EL_LINE, EL_SHAPE}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_LINETYPE, A_P0, A_P1}, 
				new String[] {A_P0, A_P1} ));

		// revised element map entry for the 'pointSet' element -- "symbol" and "symbolSize" attrs replaced by single 
		// required "symbol" element.
		elementMap4.put( EL_POINTSET, 
			new SchemaElementInfo( true, 
				new String[] {EL_SYMBOL, EL_EBAR}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SKIP, A_HIDE}, 
				new String[] {} ));

		// revised element map entry for the 'multiSet' element -- "symbol" and "symbolSize" attrs replaced by single 
		// required "symbol" element.
		elementMap4.put( EL_MULTISET, 
			new SchemaElementInfo( true, 
				new String[] {EL_SYMBOL, EL_EBAR}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SKIP, A_HIDE}, 
				new String[] {} ));

		// revised element map entry for the 'series' element -- "symbol" and "symbolSize" attrs replaced by single 
		// required "symbol" element.
		elementMap4.put( EL_SERIES, 
			new SchemaElementInfo( true, 
				new String[] {EL_SYMBOL, EL_EBAR}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SKIP, A_HIDE, A_X0, A_DX, A_TYPE, A_BARWIDTH, A_BASELINE, A_FILLED}, 
				new String[] {} ));

		// revised element map entry for the 'function' element -- "symbol" and "symbolSize" attrs replaced by single 
		// required "symbol" element; added "skip" attribute
		elementMap4.put( EL_FUNCTION, 
			new SchemaElementInfo( true, 
				new String[] {EL_SYMBOL}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_TITLE, A_LINETYPE, A_SKIP, A_HIDE, A_X0, A_X1, A_DX}, 
				new String[] {} ));

		// revised element map entry for the 'legend' element -- changed name of "symbolSize" attr to "size"
		elementMap4.put( EL_LEGEND, 
			new SchemaElementInfo( false, 
				new String[] {}, 
				new String[] {A_FONT, A_PSFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECOLOR, 
								  A_ROTATE, A_SPACER, A_SIZE, A_LEN, A_MID, A_LOC}, 
				new String[] {A_LOC} ));

		// revised element map entry for the 'fyp' element -- removed "symbol" and "symbolSize" attributes
		String[] rootAttrs = new String[] {A_FONT, A_PSFONT, A_SUBSTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, 
			A_STROKEWIDTH, A_STROKECOLOR, A_CAP, A_CAPSIZE, A_MID, A_SPACER, A_LABELOFFSET, A_PERLOGINTV, A_DIR, A_LEN, 
			A_FMT, A_GAP};
		elementMap4.put( EL_FYP, 
			new SchemaElementInfo( false, new String[] {EL_FIGURE}, rootAttrs, rootAttrs ) );

	}

   /**
    * Overridden to recognize the new element classes supported in schema 4.
    * @see ISchema#isSupportedElementTag(String)
    */
   @Override
	public boolean isSupportedElementTag(String elTag)
	{
      return(elementMap4.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
	}

   /**
    * Overridden to provide schema element information for any element classes were added or revised in schema 4.
    * @see ISchema#getSchemaElementInfo(String)
    */
   @Override
   public SchemaElementInfo getSchemaElementInfo(String elTag)
   {
      SchemaElementInfo info = (SchemaElementInfo) elementMap4.get(elTag);
      return((info==null) ? super.getSchemaElementInfo(elTag) : info);
   }

	/**
	 * This method enforces any requirements on the children content of elements that were introduced or revised in 
	 * schema 4. For all other element classes, it defers to the superclass implementation.
	 * <ul>
	 * 	<li>The new "symbol" element has no children.</li>
	 * 	<li>The "pointSet", "multiSet", and "series" elements now have two required children, a "symbol" element 
	 * followed by an "ebar" element.</li>
	 * 	<li>The "function" element now has one required child, a "symbol" element.</li>
	 * </ul>
	 * @see ISchema#hasRequiredChildren(BasicSchemaElement)
	 */
   @SuppressWarnings("rawtypes")
   @Override
	public boolean hasRequiredChildren(BasicSchemaElement e)
	{
		String elTag = e.getTag();
		List children = e.getElementContent();
		if(EL_SYMBOL.equals(elTag)) 
			return(true);
		else if(EL_FUNCTION.equals(elTag))
			return(children.size() > 0 && EL_SYMBOL.equals(((BasicSchemaElement) children.get(0)).getTag()));
		else if(EL_POINTSET.equals(elTag) || EL_MULTISET.equals(elTag) || EL_SERIES.equals(elTag))
			return(children.size() > 1 && 
				EL_SYMBOL.equals(((BasicSchemaElement) children.get(0)).getTag()) &&
				EL_EBAR.equals(((BasicSchemaElement) children.get(1)).getTag()));

		return(super.hasRequiredChildren(e));
	}

	/**
	 * This method enforces any structural restrictions on the children content of elements that were introduced or 
	 * revised in schema 4. For all other element classes, it defers to the superclass implementation.
	 * <ul>
	 * 	<li>The "pointSet", "multiSet", and "series" elements now have two required children, a "symbol" element 
	 * followed by an "ebar" element -- and no others.</li>
	 * 	<li>The "function" element now has one required child, a "symbol" element.</li>
	 * </ul>
	 * @see ISchema#isValidChildAtIndex(BasicSchemaElement,String,int)
	 */
   @Override
	public boolean isValidChildAtIndex(BasicSchemaElement e, String childTag, int index)
	{
		String elTag = e.getTag();
		if(EL_FUNCTION.equals(elTag))
			return(EL_SYMBOL.equals(childTag) && index == 0);
		else if(EL_POINTSET.equals(elTag) || EL_MULTISET.equals(elTag) || EL_SERIES.equals(elTag))
			return((EL_SYMBOL.equals(childTag) && index == 0) || (EL_EBAR.equals(childTag) && index == 1));

		return(super.isValidChildAtIndex(e, childTag, index));
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
	 *    <li>The "type" attribute for a "symbol" element is a multi-choice attribute selecting an adornment type. It 
	 * 	can be any value in <code>Schema1.ADORN1_CHOICES</code>.
	 * 	<li>There are no other exceptions! All other attributes of the new "symbol" element can be validated by the 
	 * 	superclass.</li>
	 * </ul>
	 * @see ISchema#isValidAttributeValue(BasicSchemaElement,String,String)
	 */
   @Override
	public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
		if(A_TYPE.equals(attr) && (e.getTag()).equals(EL_SYMBOL))
			return(isValidEnumAttributeValue(value, ADORN1_CHOICES));
		return(super.isValidAttributeValue(e, attr, value));
	}

	/**
	 * This method handles the actual details of migrating schema version 3 content to schema version 4.  It makes the 
	 * following changes:
	 * <ul>
	 * 	<li>For each "line" element encountered, we get rid of the endpoint/midpoint cap attributes that are no longer 
	 *    defined. For any such attributes that would cause a symbol to be rendered, we add a "shape" child that will
	 *    replicate the effect of the old attributes. These "shape" children will inherit all styling attributes from 
	 *    the parent "line", except that the "lineType" is set to "solid" explicitly -- because the symbols were always 
	 * 	stroked "solid" in the previous schema. They are inserted as the first 0-3 children of the modified "line" 
	 * 	element.</li>
	 * 	<li>For each "legend" encountered, we get rid of the "symbolSize" attr and replace it with the "size" attr. 
	 * 	If "symbolSize" was explicitly set, then "size" is set to that explicit value.</li>
	 * 	<li>For the "fyp" (root) element, we get rid of the "symbolSize" and "symbol" attributes, which are no longer 
	 * 	defined in this schema.</li>
	 * 	<li>For each data set element encountered, we must replace the old "symbol" and "symbolSize" attributes with a 
	 * 	new "symbol" child, which must be the first child of the data set. The "symbol" element's "type" attribute 
	 * 	will be set to the explicit value of the old "symbol" attribute. If "symbol" attribute was implicit, "type" 
	 * 	is set to the value of "symbol" on the "fyp" element (which is always encountered first during migration).  
	 * 	However, if the value is "none" (the default value for a "symbol" element's "type", then the attribute is left 
	 * 	implicit. Similarly for the "size" attr of the "symbol" element, replacing the old "symbolSize" attribute 
	 * 	(the default value for a "symbol" element's "size" is "0.1in"). In addition, since symbols were always stroked 
	 * 	solid regardless of the "lineType" of the data set, we must explicitly set the "symbol" child's "lineType" 
	 * 	attribute to "solid" regardless of the parent data set's lineType.</li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
	 */
   @Override
	public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
	{
		if(oldSchema.getVersion() != getVersion() - 1) 
			throw new XMLException("A schema instance can only migrate from the previous version.");

		// remember the document-level default values for the now-deprecated "symbol" and "symbolSize" attributes.  
		// We'll need these to migrate the data set elements. These were required attributes for "fyp".
		BasicSchemaElement oldRoot = (BasicSchemaElement) oldSchema.getRootElement();
		String oldDocDefSymbol = oldRoot.getAttributeValueByName(A_SYMBOL);
		String oldDocDefSymbolSize = oldRoot.getAttributeValueByName(A_SYMBOLSIZE);

		// update the content of the old schema in place...
		Stack<BasicSchemaElement> elementStack = new Stack<BasicSchemaElement>();
		elementStack.push((BasicSchemaElement) oldSchema.getRootElement());
		while(!elementStack.isEmpty())
		{
			BasicSchemaElement e = elementStack.pop();
			String elTag = e.getTag();

			// the root "fyp" element: remove the "symbolSize" and "symbol" attributes, which no longer exist.
			if(EL_FYP.equals(elTag))
			{
				e.removeAttributeByName(A_SYMBOLSIZE);
				e.removeAttributeByName(A_SYMBOL);
			}

			// handle migration of each "line" element encountered
			if(EL_LINE.equals(elTag))
			{
				int nInserted = 0;

				// if a symbol is rendered at the first endpoint, replicate it with a "shape" element child; then remove 
				// the deprecated attributes A_P0CAP and A_P0CAPSIZE. The "shape" child inherits most attributes from 
				// the parent "line". We do have to set the shape type and size, and locate it at the line's first endpt.
				// NOTE: If the "cap" attribute is present but the "capSize" is not, then it should be drawn. It has a 
				// default size (0.1in), and so the "shape" child's A_SIZE attribute should also remain implicit. We also 
				// have to explicitly set the line type to "solid" because symbols were always stroked solid in previous 
				// schema...
				String cap = e.getAttributeValueByName(A_P0CAP);
				String capSize = e.getAttributeValueByName(A_P0CAPSIZE);
				e.removeAttributeByName(A_P0CAP);
				e.removeAttributeByName(A_P0CAPSIZE);
				if((cap != null && !ATTRVAL_NONE.equals(cap)) && (capSize==null || getMeasureFor(capSize)>0))
				{
					BasicSchemaElement shape = (BasicSchemaElement) createElement(EL_SHAPE);
					shape.setAttributeValueByName(A_TYPE, cap);
					if(capSize != null) shape.setAttributeValueByName(A_SIZE, capSize);
					shape.setAttributeValueByName(A_LOC, "0% 100%");
					shape.setAttributeValueByName(A_LINETYPE, LINETYPE_SOLID);

					// if the shape is not symmetric in Y, we rotate it 180 -- as was done in the previous implementation 
					// of the LineElement
					if(!isAdornmentVerticallySymmetric(cap))
						shape.setAttributeValueByName(A_ROTATE, "180");

					e.insert(shape,nInserted);
					++nInserted;
				}

				// analogously for the second endpoint and midpoint...
				cap = e.getAttributeValueByName(A_P1CAP);
				capSize = e.getAttributeValueByName(A_P1CAPSIZE);
				e.removeAttributeByName(A_P1CAP);
				e.removeAttributeByName(A_P1CAPSIZE);
				if((cap != null && !ATTRVAL_NONE.equals(cap)) && (capSize==null || getMeasureFor(capSize)>0))
				{
					BasicSchemaElement shape = (BasicSchemaElement) createElement(EL_SHAPE);
					shape.setAttributeValueByName(A_TYPE, cap);
					if( capSize != null ) shape.setAttributeValueByName(A_SIZE, capSize);
					shape.setAttributeValueByName(A_LOC, "100% 100%");
					shape.setAttributeValueByName(A_LINETYPE, LINETYPE_SOLID);
					e.insert(shape,nInserted);
					++nInserted;
				}

				cap = e.getAttributeValueByName(A_MIDCAP);
				capSize = e.getAttributeValueByName(A_MIDCAPSIZE);
				e.removeAttributeByName(A_MIDCAP);
				e.removeAttributeByName(A_MIDCAPSIZE);
				if((cap != null && !ATTRVAL_NONE.equals(cap)) && (capSize==null || getMeasureFor(capSize)>0))
				{
					BasicSchemaElement shape = (BasicSchemaElement) createElement(EL_SHAPE);
					shape.setAttributeValueByName(A_TYPE, cap);
					if( capSize != null ) shape.setAttributeValueByName(A_SIZE, capSize);
					shape.setAttributeValueByName(A_LOC, "50% 100%");
					shape.setAttributeValueByName(A_LINETYPE, LINETYPE_SOLID);
					e.insert(shape,nInserted);
					++nInserted;
				}
			}

			// for each data set element encountered...
			if(EL_POINTSET.equals(elTag) || EL_MULTISET.equals(elTag) || EL_SERIES.equals(elTag) || 
				EL_FUNCTION.equals(elTag))
			{
				// get values for the old "symbol" and "symbolSize" attributes.  If not explicitly set, then use the 
				// document-level defaults.
				String symType = e.getAttributeValueByName(A_SYMBOL);
				if(symType == null) symType = oldDocDefSymbol;
				String symSize = e.getAttributeValueByName(A_SYMBOLSIZE);
				if(symSize == null) symSize = oldDocDefSymbolSize;

				// now remove these attributes, which no longer exist in Schema4
				e.removeAttributeByName(A_SYMBOL);
				e.removeAttributeByName(A_SYMBOLSIZE);

				// create a new "symbol" element and assign its "type" and "size" attrs IAW the old "symbol" and 
				// "symbolSize" attrs. If the old attr value is the same as the standard default for the new attr ("type" 
				// and "size" do not have document-level defaults as "symbol" and "symbolSize" did), then leave the new 
				// attr as implicit.
				BasicSchemaElement symbol = (BasicSchemaElement) createElement(EL_SYMBOL);
				if(!DEFAULT_SYMBOL_TYPE.equals(symType))
					symbol.setAttributeValueByName(A_TYPE, symType);
				if(!DEFAULT_SYMBOL_SIZE.equals(symSize))
					symbol.setAttributeValueByName(A_SIZE, symSize);

				// we must explicitly set the new "symbol" element's line type to solid. This is because data set symbols 
				// were always stroked solid prior to the changes introduced in Schema4.
				symbol.setAttributeValueByName(A_LINETYPE, LINETYPE_SOLID);

				// insert the new "symbol" element as the first child of the parent data set. Since a "function" element 
				// previously did not allow children, we must change this flag.
				if(EL_FUNCTION.equals(elTag)) e.setAllowsChildren(true); 
				e.insert(symbol, 0);
			}

			// for each "legend" element encountered: remember value of old "symbolSize" attribute, if it is explicitly 
			// set. Remove it from the list of explicitly set attributes. We'll set the replacement "size" attr after 
			// the element's schema info has been updated.
			String legendSymSize = null;
			if(EL_LEGEND.equals(elTag))
			{
				legendSymSize = e.getAttributeValueByName(A_SYMBOLSIZE);
				e.removeAttributeByName(A_SYMBOLSIZE);
			}

			// migrate the element object's schema info
			e.updateSchema(this, null);

			// for each "legend" element encountered: now that the schema is updated, we need to explicitly set the new 
			// "size" attribute if the old "symbolSize" attribute was explicitly set.
			if(EL_LEGEND.equals(elTag) && legendSymSize != null)
				e.setAttributeValueByName(A_SIZE, legendSymSize);

			// if element has any children, push them onto the stack so that we check them as well! No need to do so for 
			// the "shape" children that we've just added to any "line" element, nor for any "symbol" elements, since we 
			// just added them above!
			for(int i=0; i<e.getChildCount(); i++)
			{
				BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
				if(EL_SYMBOL.equals(elTag) || !(EL_LINE.equals(elTag) && EL_SHAPE.equals(child.getTag())))
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

	/** A hash map of those adorments supported in schema version 4 that are not vertically symmetric. */
	static HashMap<String, String> mapNotVSAdornments = null;
	static 
	{
		mapNotVSAdornments = new HashMap<String, String>();
		mapNotVSAdornments.put( ADORN_LFTRIANGLE, null );
		mapNotVSAdornments.put( ADORN_RTTRIANGLE, null );
		mapNotVSAdornments.put( ADORN_LFISOTRIANGLE, null );
		mapNotVSAdornments.put( ADORN_RTISOTRIANGLE, null );
		mapNotVSAdornments.put( ADORN_LFDART, null );
		mapNotVSAdornments.put( ADORN_RTDART, null );
		mapNotVSAdornments.put( ADORN_LFARROW, null );
		mapNotVSAdornments.put( ADORN_RTARROW, null ); 
		mapNotVSAdornments.put( ADORN_BRACKET, null );
		mapNotVSAdornments.put( ADORN_ARROW, null );
		mapNotVSAdornments.put( ADORN_FILLARROW, null );
		mapNotVSAdornments.put( ADORN_THINARROW, null );
		mapNotVSAdornments.put( ADORN_FILLTHINARROW, null );
		mapNotVSAdornments.put( ADORN_WIDEARROW, null );
		mapNotVSAdornments.put( ADORN_FILLWIDEARROW, null );
		mapNotVSAdornments.put( ADORN_REV_ARROW, null );
		mapNotVSAdornments.put( ADORN_REV_FILLARROW, null );
		mapNotVSAdornments.put( ADORN_REV_THINARROW, null );
		mapNotVSAdornments.put( ADORN_REV_FILLTHINARROW, null );
		mapNotVSAdornments.put( ADORN_REV_WIDEARROW, null );
		mapNotVSAdornments.put( ADORN_REV_FILLWIDEARROW, null );
	}

	/**
	 * Is the specified adornment vertically symmetric? The method assumes that the argument is the name of a valid 
	 * adornment type supported in schema version 4. If it is not, this method will incorrectly return <code>true</code>.
	 * @param adorn Name of an adornment supported in schema version 4.
	 * @return <code>True</code> if adornment is vertically symmetric.
	 */
	static boolean isAdornmentVerticallySymmetric(String adorn) { return(!mapNotVSAdornments.containsKey(adorn)); }
}
