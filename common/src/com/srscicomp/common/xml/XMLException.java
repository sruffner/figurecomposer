package com.srscicomp.common.xml;

/**
 * <code>XMLException</code> is a generic checked exception thrown when a problem is detected while parsing, 
 * validating, or writing XML content. It provides some built-in strings to describe some common errors that may occur 
 * while processing XML content.
 * 
 * @author sruffner
  */
public class XMLException extends Exception
{
   private static final long serialVersionUID = 1L;

   /** Error description: Parsed element tag is not recognized. */
	public static final String UNKNOWN_TAG = "XML element tag not recognized in current context";

	/** Error description: Missing end tag for an XML element. */
	public static final String NO_END_TAG = "Missing XML element end tag";

	/** Error description: Attribute name is not recognized. */
	public static final String UNKNOWN_ATTR = "XML attribute name not recognized in current context";

	/** Error description: Current element is missing a required attribute. */
	public static final String MISSING_ATTR = "Required XML attribute missing in current context";

	/** Error description: Attribute value is illegal or out of bounds. */
	public static final String ILLEGAL_ATTR = "XML attribute value is illegal";

	/** Error description: Application does not support XML elements with mixed content. */
	public static final String NO_MIXED_CONTENT = "Mixed content element is not supported";

	/** Error description: The XML parser detected a problem; use this when wrapping the parser's exception. */
	public static final String PARSER_EXCP = "General XML parser exception";

	/** Error description: A required XML element is missing. */
	public static final String MISSING_ELEM = "Required XML element missing in current context";

	/** Error description: Application does not recognize this namespace. */
	public static final String UNKNOWN_NS = "XML namespace unrecognized";

	/** The element tag for which exception was thrown; <code>null</code> if not specified. */
	private String tag = null;

	/** The attribute name, if any, for which exception was throw; <code>null</code> if not specified. */
	private String attr = null;

   /** 
    * Construct a new <code>XMLException</code> with the specified description.
    * @param description Description of the exception.
    */
   public XMLException(String description) { super(description); }

	/**
	 * Constructs a new <code>XMLException</code>, describing the exception and identifying the XML element and, 
	 * possibly, attribute that was invalid.
	 * 
	 * @param description The exception description. Retrieved by <code>getMessage()</code>.
	 * @param tag The XML element that was invalid. Retrieved by <code>getRelevantTag()</code>.
	 * @param attribute The element's attribute that caused the exception. May be <code>null</code>. Ignored if 
	 * <code>tag</code> is <code>null</code>. Retrieved by <code>getRelevantAttribute()</code>.
	 */
	public XMLException(String description, String tag, String attribute)
	{
		super(description);
		this.tag = tag;
		if(tag != null) this.attr = attribute;
	}

   /** 
    * Construct a new <code>XMLException</code> with the specified description and originating cause.
    * @param description Description of the exception.
    * @param cause The <code>Throwable</code> that triggered this exception.
    */
	public XMLException(String description, Throwable cause) { super(description, cause); }

	/**
	 * Retrieves the description of this exception, including the relevant XML element tag and attribute, if they 
	 * were specified when the exception was thrown.
	 * @return The description string. It will not be <code>null</code>.
	 */
	public String getMessage()
	{
		String s = super.getMessage();
		if(tag != null)
		{
			if(s == null) s = "General XML-related fault occurred ";
			s += " [tag=" + tag;
			if(attr != null) s += ", attr=" + attr;
			s += "]";
		}
		return(s);
	}

	/**
	 * Retrieves the tag of the invalid XML element for which this exception was thrown.
	 * @return The tag name of the invalid XML element. May be <code>null</code>.
	 */
	public String getRelevantTag() { return(tag); }

	/**
	 * Retrieves the name of a particular attribute of the invalid XML element for which this exception was thrown.
	 * 
	 * @return The name of the invalid XML attribute. May be <code>null</code>.
	 */
	public String getRelevantAttribute() { return(attr); }
}
