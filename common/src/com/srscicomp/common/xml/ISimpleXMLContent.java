package com.srscicomp.common.xml;

/**
 * This interface is intended to represent a simplified XML content model, a hierarchical tree of simple XML elements
 * elements described by the <code>ISimpleXMLElement</code> interface. The model is "simplified" because it does not 
 * support multiple namespaces and other less common aspects of an XML grammar. In addition, a "mixed content" element 
 * is very restricted:  any text content must follow all element content.
 * 
 * <p>Together, <code>ISimpleXMLContent</code> and <code>ISimpleXMLElement</code> define a framework for enforcing an 
 * XML content schema as the XML data is read from or written to a stream or file "on the fly". Their design is 
 * obviously geared for use in an application that employs an event-based XML parser such as SAX or StAX or XMLPull, 
 * allowing the application to build an in-memory content tree of an XML document as it is parsed. The interfaces 
 * support validation of the content as it is parsed, so that the application can "detect" an invalid document as soon 
 * as possible.</p>
 * 
 * <p><code>ISimpleXMLContent</code> provides the <strong>single</strong> namespace URI that is associated with the 
 * content schema it represents, a factory method for creating all the different kinds of elements defined within that 
 * schema, and methods for setting and getting the root node of the XML tree content. All nodes in this tree must 
 * implement the <code>ISimpleXMLElement</code> interface, which enforces content schema restrictions at the element 
 * level.</p>
 * 
 * <p>Usage. Consider an application that wants to read in, manipulate, and write out XML documents adhering to a 
 * particular XML content schema. One approach is to design classes for each distinct kind of element defined in the 
 * schema; each such class would implement <code>ISimpleXMLElement</code>. Then design the content model class itself, 
 * implementing <code>ISimpleXMLContent</code>. It is relatively easy to design a wrapper for an XML parser that can 
 * populate and validate an XML content tree from a data source adhering to the particular schema encapsulated by the 
 * <code>ISimpleXMLContent</code> and <code>ISimpleXMLElement</code> implementations. <code>XPPWrapper</code> is an 
 * example of such a wrapper class for XMLPull parsers.</p>
 * 
 * @author sruffner
 */
public interface ISimpleXMLContent
{
	/**
	 * Retrieve the unique remote identifier for the namespace from which all elements and attributes in this content 
	 * model are drawn. An implementation intended to process XML content with a default namespace should return an 
	 * empty string rather than <code>null</code>.
	 * @return The namespace URI. It need not correspond to a retrievable resource.
	 */
	String getNamespaceUri();

	/**
	 * Retrieve any processing instruction that should be included when the content model is written out to an XML 
	 * stream. The processing instruction should be included immediately after the XML declaration that initiates the 
	 * XML stream. If no processing instruction is required, return a <code>null</code> or empty string.
	 * @return The processing instruction in the form mandated by the XML 1.0 specification.
	 */
	String getProcessingInstruction();

	/**
	 * Construct an instance of the implementation-specific class encapsulating an XML element with the specified 
	 * tag name. The object should be created in a valid default state, if possible.
	 * @param tag The name of the element to be created (without namespace prefix).
	 * @return A reference to the element created.
	 * @throws XMLException if element tag is not recognized by the content model schema.
	 * 
	 */
	ISimpleXMLElement createElement(String tag) throws XMLException;

	/** 
	 * Get the tag name for the root element in this content model.
	 * @return The tag name for the content model's root element.
	 */
	String getRootTag();
	
	/**
	 * Retrieve the current root element in this content model.
	 * @return The root element (<code>null</code> if the model is empty).
	 */
	ISimpleXMLElement getRootElement();

	/**
	 * Set the root element for this content model.
	 * @param root The new root element. Setting it to <code>null</code> will empty the content model.
	 * @param validate If <code>true</code>, the new root element and all descendants are validated prior to installing 
	 * it in the content model. Ignored if root is <code>null</code>.
	 * @throws XMLException if the element cannot serve as a root element of the content model, or
	 * <code>validate</code> is set and the root element or any of its descendants is invalid.
	 */
	void setRootElement(ISimpleXMLElement root, boolean validate) throws XMLException;

	/**
	 * Validate the current content in its entirety. Note that an empty content model should be considered invalid. 
	 * @throws XMLException if the content model is not valid.
	 */
	void validate() throws XMLException;
}
