package com.srscicomp.common.xml;

import java.util.List;


/**
 * This interface is intended to represent a simplified XML element, the building block of the simplified XML content 
 * model encapsulated by the <code>ISimpleXMLContent</code> interface. It is "simplified" because it does not support 
 * multiple namespaces and other less common aspects of an XML grammar. In addition, there is no support for 
 * interspersing character data with element content. All character data in a "mixed content" element is concatenated 
 * together into a single string; when written, the text content follows any child elements in the definition of the 
 * mixed-content element.
 * 
 * <p>The <code>ISimpleXMLContent/ISimpleXMLElement</code> framework provides a generic representation of XML content 
 * which can be read from and written to a stream or file "on the fly". Its design is obviously geared for use in an 
 * application that employs an event-based XML parser such as SAX or StAX or XMLPull, allowing the application to build 
 * an in-memory content tree of an XML document as it is parsed. The interface supports validation of the content as it 
 * is created, so that the application can "detect" an invalid document as soon as possible.</p>
 * 
 * <p>Proper implementations must respect the sequential nature of the <code>ISimpleXMLElement</code> methods which 
 * define and populate the element step-by-step: 
 * <ol>
 *    <li><code>startTag()</code> initiates modal binding.</li>
 *    <li>zero or more <code>addElement()</code> calls to process any element content.</li>
 *    <li>a single setTextContent() call -- if the element has text content.</li>
 *    <li><code>endTag()</code> terminates modal binding.</li>
 * </ol>
 * Implementations should enter a "modal" state upon invocation of <code>startTag()</code>, and leave that state when 
 * <code>endTag()</code> is invoked. Calling the methods out of order should throw an <code>XMLException</code>. 
 * Calling any other methods (which could alter the object's internal definition) while in this modal state should 
 * throw <code>XMLException</code> to catch the improper usage.</p>
 * 
 * <p>A simplified XML element is defined by its name tag, an attribute list, and its content. The interface treats 
 * these facets in their "raw" XML form: A name tag is simply a string (without namespace prefix); an attribute list is 
 * an list of [name string, value string] pairs; and the content is a (possibly empty) list of 
 * <code>ISimpleXMLElement</code> child elements plus a (possibly empty) text content string. Implementations are free 
 * to create their own more sophisticated internal representation of the element.</p>
 * 
 * @see ISimpleXMLContent
 * @see XMLException
 * @author sruffner
 */
public interface ISimpleXMLElement 
{
	/**
	 * Get the tag name (no namespace prefix) associated with this XML element.
	 * @return element tag 
	 */
	String getTag();

	/**
	 * Get a list of (name,value) pairs for all attributes that have been explicitly set for this XML element. 
	 * Namespace prefixes are not supported.
	 * @return List of strings containing, sequentially: {name1, value1, name2, value2, ...} for all attributes that 
	 * have been <em>explicitly</em> defined. If there are no explicitly set attributes, an empty list is returned.
	 */
	List<String> getAttributes();

	/**
	 * Does this element admit any text content? If so, all CDATA fragments returned during parsing of the element's 
	 * definition will be concatenated into a single text string.
	 * @return <code>True</code> if element can contain CDATA content.
	 */
	boolean allowsTextContent();

	/**
	 * Retrieve the entire text content of this XML element. Since a parser will use this method to write the text 
	 * content of the element to an output file, it is recommended that the text content be formatted with 
	 * carriage-return linefeed pairs if it is more than 255 characters (for readability of the XML file).
	 * @return The text content, or <code>null</code> if this element does not hold text content.
	 */
	String getTextContent();

	/**
	 * Supply the ordered list of children contained in this XML element. The order of child elements can be significant,
	 * so it should be preserved. The objects in the list must implement <code>ISimpleXMLElement</code>.
	 * @return A list of the XML element's child elements. The list should be empty if the element does not admit child
	 * elements, or if it currently has none.
	 */
	List<ISimpleXMLElement> getElementContent();

	/**
	 * Does this XML element have any child elements?
	 * @return <code>True</code> if element has children.
	 */
	boolean hasElementContent();

	/**
	 * Destroy any existing content and attributes in this XML element, validate the provided attribute list, and enter 
	 * a modal state mirroring the step-by-step definition (start tag, content, end tag) of the element via a parser. A 
	 * call to this method should be immediately followed by any number of calls to <code>addElement()</code> and 
	 * <code>appendTextContent()</code>, followed by <code>endTag()</code>. If any other method is called in this modal 
	 * state that alters the internal representation of the element, the implementation should throw a checked exception 
	 * indicating a usage error.
	 * @param tag The element's name tag (without namespace prefix).
	 * @param attributes List of explicitly set attributes, in name-value pairs: name1, value1, name2, value2, etc.
	 * @throws XMLException Tf element tag is incorrect, any attributes are unrecognized or illegal, any
	 * attribute in the list lacks an accompanying value, any required attributes are not included in the attribute 
	 * list, or the method is invoked out of context.
	 */
	@SuppressWarnings("rawtypes")
	void startTag(String tag, List attributes) throws XMLException;
	
	/**
	 * Concatenate the specified string to the current text content ("CDATA") of this element. This method may be 
	 * invoked any number of times during modal binding (between calls to <code>startTag()</code> and 
	 * <code>endTag()</code>).  Since the <code>ISimpleXMLContent/ISimpleXMLElement</code> framework only supports 
	 * "mixed-content" elements with a SINGLE text node, it is expected that any implementation will simply append the 
	 * new text content to whatever content came before. Validation of the text content should be put off until after 
	 * modal binding is complete.
	 * @param text The element's text content
	 * @throws XMLException if element does not admit text content, or the method is invoked out of context.
	 * @see #startTag(String, List)
	 */
	void appendTextContent(String text) throws XMLException;

	/**
	 * Append a simple XML element to this XML element's children list. This method can be invoked any number of times 
	 * after <code>startTag()</code> is called (within the restrictions imposed by the element's content schema, which 
	 * is specific to the implementation). <strong>NOTE</strong> that the supplied child element may not be completely 
	 * defined and so may be invalid. The implementing class should not check the child's validity at this time -- it 
	 * can do so in <code>endTag()</code>. However, it must read the child's tag or otherwise determine its identity to 
	 * decide whether or not it belongs in this element!
	 * @param child The simple XML element to be appended. 
	 * @throws XMLException if adding the child would violate the expected structure of this element, or
	 * the method is invoked out of context.
	 * @see #startTag(String, List)
	 */
	void addElement(ISimpleXMLElement child) throws XMLException;

	/**
	 * Exit the modal state that was initiated by a previous call to <code>startTag()</code> and verify that the current 
	 * definition of the element is valid (IAW some implementation-specific content schema). For example, an 
	 * implementation might have to check for any required elements that are missing from the definition, or check the 
	 * validity of any text content.
	 * @param tag The element's name tag (without namespace prefix).
	 * @throws XMLException if element tag is incorrect, element's internal definition is invalid in any
	 * way, or method is invoked out of context.
	 * @see #startTag(String, List)
	 */
	void endTag(String tag) throws XMLException;

	/**
	 * Check the validity of this XML element against some implementation-specific content schema. This provides a means
	 * of checking an XML element immediately after reading its end tag from an XML input stream, or prior to writing it 
	 * to an output stream. The check can be deep (validate all descendants) or shallow (validate this element only).
	 * @param deep If <code>true</code>, validate all descendants (if any) as well as this element itself.
	 * @throws XMLException if element's internal definition is invalid in any way, or if method is invoked
	 * during the modal state between calls to <code>startTag()</code> and <code>endTag()</code>.
	 */
	void validate(boolean deep) throws XMLException;
}
