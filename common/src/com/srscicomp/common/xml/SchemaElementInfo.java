package com.srscicomp.common.xml;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>SchemaElementInfo</code> is part of the <code>ISchema/BasicSchemaElement</code> framework for defining a 
 * versionable XML content schema. It encapsulates some basic information about a XML schema element that is used by 
 * <code>BasicSchemaElement</code> to validate some aspects of element content. It is a read-only object and should be 
 * prepared and maintained by a schema object that implements the <code>ISchema</code> interface.
 * 
 * @see ISchema
 * @author sruffner
 */
public class SchemaElementInfo
{
	/** If set, this flag indicates that the element admits text content. */
	private final boolean allowsText;

	/**
	 * A hash map keyed by the tag names of all child elements admitted by the element defined by this 
	 * <code>SchemaElementInfo</code>. The map values are <code>null</code>.
	 */
	private final Map<String, String> allowedChildren;

	/**
	 * A hash map keyed by the names of all attributes that are supported for the element defined by this 
	 * <code>SchemaElementInfo</code>. The map values are <code>null</code>.
	 */
	private final Map<String, String> allowedAttributes;

	/** List of names of REQUIRED attributes for the element defined by this <code>SchemaElementInfo</code>. */
	private final String[] reqdAttributes;

	/**
	 * Construct a <code>SchemaElementInfo</code> object.
	 * 
	 * @param allowsText Set if the represented element admits text content.
	 * @param children The tag names of all elements that can be children of the represented element.
	 * @param attrs The names of all attributes for the represented element.
	 * @param attrReqd The names of all REQUIRED attributes for the represented element.
	 */
	public SchemaElementInfo(boolean allowsText, String[] children, String[] attrs, String[] attrReqd)
	{
		this.allowsText = allowsText;

		allowedChildren = new HashMap<String, String>();
		for(int i=0; i<children.length; i++) 
			allowedChildren.put(children[i],null);

		allowedAttributes = new HashMap<String, String>();
		for(int i=0; i<attrs.length; i++) 
			allowedAttributes.put(attrs[i],null);

		reqdAttributes = new String[attrReqd.length];
		for(int i=0; i<attrReqd.length; i++)
			reqdAttributes[i] = attrReqd[i];
	}

	/**
	 * Does the <code>BasicSchemaElement</code> represented by this <code>SchemaElementInfo</code> admit text content? 
	 * A <code>BasicSchemaElement</code> only supports the so-called "mixed content" element to the extent required by 
	 * the <code>ISimpleXMLElement</code> interface.
	 * @return <code>True</code> if represented element admits text content.
	 */
	public boolean allowsText() { return(allowsText); }

	/**
	 * Does the <code>BasicSchemaElement</code> represented by this <code>SchemaElementInfo</code> allow any child 
	 * element content? A <code>BasicSchemaElement</code> can be text-only, children-only, mixed-content (to the extent 
	 * permitted by the <code>ISimpleXMLElement</code> interface), or no content. 
	 * @return <code>True</code> if represented element can contain child elements.
	 */
	public boolean allowsChildren() { return(!allowedChildren.isEmpty()); }

	/**
	 * Can the <code>BasicSchemaElement</code> represented by this <code>SchemaElementInfo</code> allow a child element 
	 * with the specified tag name? Note that this is NOT a complete validity test, since a schema could restrict the 
	 * positions or number of children within a parent element.
	 * @param tag The child element tag name.
	 * @return <code>True</code> if represented element can contain at least one child with the specified tag.
	 */
	public boolean isChildAllowed(String tag) { return(allowedChildren.containsKey(tag)); }

	/**
	 * Does the <code>BasicSchemaElement</code> represented by this <code>SchemaElementInfo</code> recognize the named 
	 * attribute?
	 * @param attr The attribute name.
	 * @return <code>True</code> if represented element possesses an attribute with the specified name.
	 */
	public boolean isAttributeAllowed(String attr) { return(allowedAttributes.containsKey(attr)); }

	/**
	 * Does the <code>BasicSchemaElement</code> represented by this <code>SchemaElementInfo</code> REQUIRE the named 
	 * attribute?
	 * @param attr The attribute name.
	 * @return <code>True</code> if represented element requires an explicit value for the specified attribute.
	 */
	public boolean isAttributeRequired(String attr)
	{
		for(int i=0; i<reqdAttributes.length; i++)
		{
			if(reqdAttributes[i].equals(attr)) return(true);
		}
		return(false);
	}

	/**
	 * Verify that the attribute map provided includes all attributes that are REQUIRED by the 
	 * <code>BasicSchemaElement</code> that is represented by this <code>SchemaElementInfo</code>.
	 * @param attrMap The attribute map to test. It is assumed that the keys of the map are the attribute names. The 
	 * values in the map are ignored.
	 * @return If all required attributes are found in the map, the method returns <code>null</code>. Otherwise, it 
	 * returns the name of the first required attribute found to be missing from the map.
	 */
	public String checkForRequiredAttributes(Map<String, ?> attrMap)
	{
		for(int i=0; i<reqdAttributes.length; i++)
		{
			if(!attrMap.containsKey(reqdAttributes[i])) return(reqdAttributes[i]);
		}
		return(null);
	}
}
