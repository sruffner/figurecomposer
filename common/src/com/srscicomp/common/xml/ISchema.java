package com.srscicomp.common.xml;

/**
 * <code>ISchema</code> defines the basic requirements of a simplified XML schema. It provides a framework for reading 
 * and writing XML documents conforming to the schema, as well as migrating documents defined in an older schema to the 
 * current schema version. Each "historical schema" is encapsulated by a distinct implementation of this interface, 
 * using <code>BasicSchemaElement</code> to represent each element class. It is expected that each successive schema 
 * class will build upon the previous one, and be capable of migrating any valid XML document conforming to that 
 * previous version.
 * 
 * <p><code>ISchema</code> extends <code>ISimpleXMLContent</code>, which restricts the complexity of the XML schemas 
 * it can represent (e.g., there is only limited support for mixed content -- text and child elements).</p>
 * 
 * @author 	sruffner
 */
public interface ISchema extends ISimpleXMLContent
{
	/**
	 * Retrieve the version number for this schema. Schema versions are integers; the larger the value, the more recent 
	 * the version.  Version 0 refers to the base schema version assigned to all documents created prior to the 
	 * introduction of schema versioning for the XML content represented.
	 * @return Schema version number.
	 */
	int getVersion();

	/**
	 * When content conforming to an older schema is parsed, it is typically migrated to the latest schema version. This
	 * method should return the original schema version number assigned to the content.
	 * @return Original schema version assigned to the content prior to migration.
	 */
	int getOriginalVersion();
	
	/**
	 * Retrieve the version string for the last version of the application that used or generated this schema. This is 
	 * for information purposes only and is optional. If an implementation will not provide this version string, this 
	 * method should return an empty string, not <code>null</code>. Otherwise, a recommended form for the version 
	 * string is "M.m.r", where M, m, and r are integers specifying the major version, minor version, and revision, 
	 * respectively.
	 * @return An application version string, as described.
	 */
	String getLastApplicationVersion();

	/**
	 * Is the specified XML tag denote a valid element class in this XML schema?
	 * @param elTag The element tag to be checked.
	 * @return <code>True</code> if tag is a recognized element tag name in this schema; <code>false</code> otherwise.
	 */
	boolean isSupportedElementTag(String elTag);

	/**
	 * Retrieve schema element information for the specified element tag.
	 * <p>A <code>BasicSchemaElement</code> uses this information to validate some, but not all, aspects of the element's
	 * content during modal binding with a pull-parser. Other validation tasks are handled by calling other methods in 
	 * this interface.
	 * @param elTag The tag name of the element for which schema information is requested. Must be a valid tag.
	 * @return A read-only object containing the schema information for the element.
	 */
	SchemaElementInfo getSchemaElementInfo(String elTag);

	/**
	 * Check the value of an attribute owned by an element in this schema. This method will be invoked many times by 
	 * instances of <code>BasicSchemaElement</code> during modal binding of an XML document instance to this schema.
	 * @param e The owner element.
	 * @param attr The name of the attribute.
	 * @param val The attribute value to be checked.
	 * @return <code>True</code> if the value is a valid one for the specified attribute and element.
	 */
	boolean isValidAttributeValue(BasicSchemaElement e, String attr, String val);

	/**
	 * Ensure that the specified element has any and all child elements that are REQUIRED according to this schema.
	 * @param e The element to test.
	 * @return <code>True</code> if element has all required child elements.
	 */
	boolean hasRequiredChildren(BasicSchemaElement e);

	/**
	 * Verify that the specified element can hold a child element with the specified tag at the specified position in 
	 * its children list.
	 * 
	 * <p>A schema can place arbitrary restrictions on what child elements a parent element can contain. This method 
	 * must enforce all such restrictions for this schema. Note that it is invoked by a <code>BasicSchemaElement</code> 
	 * instance in two distinct situations -- when a new child element is about to be inserted during modal binding, or 
	 * when the element is validated after it has been bound.</p>
	 * @param e The parent element.
	 * @param childTag The unique tag of the candidate child element.
	 * @param index If child is about to be inserted during modal binding, this is the insertion location. Else it is 
	 * the actual location of the child in the parent's children list. The first child is at index 0.
	 * @return <code>True</code> if specified child may appear at the specified position in the parent's child list.
	 */
	boolean isValidChildAtIndex(BasicSchemaElement e, String childTag, int index);

	/**
	 * Validate the text content of the specified element IAW this schema.
	 * 
	 * @param e The element.
	 * @param text The candidate text content for the element.
	 * @return <code>True</code> if the element admits text content, and the specified content is valid.
	 */
	boolean isValidTextContent(BasicSchemaElement e, String text);

	/**
	 * Migrate XML content from a previous schema version to this schema.
	 * 
	 * <p><strong>IMPORTANT</strong>: Implementations may reuse the element objects in the old schema. Typically, the 
	 * old schema will be emptied during migration. Therefore, callers must not assume that the old  schema will be left 
	 * unaltered.</p>
	 * @param oldSchema A complete valid XML document conforming to the previous version of this schema.
	 * @return The content modified as necessary and bound to this schema instance, while preserving the author's 
	 * original intent as much as possible.
	 * @throws XMLException if the schema provided does not conform to the previous version, or if the migration is not 
	 * possible.
	 */
	void migrateFromPreviousSchema( ISchema oldSchema ) throws XMLException;
}
