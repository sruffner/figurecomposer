package com.srscicomp.common.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <code>BasicSchemaElement</code> is a simple implementation of the <code>ISimpleXMLElement</code> interface that 
 * encapsulates the content of an XML element and relies on its owner schema for validating that content. To implement
 * the tree-like hierarchy of XML content, it extends <code>javax.swing.tree.DefaultMutableTreeNode</code>.
 * 
 * <p><em>WARNING</em>:  This class is NOT thread-safe.</p>
 * 
 * @see ISimpleXMLElement
 * @see ISchema
 * @author sruffner
 */
public class BasicSchemaElement extends DefaultMutableTreeNode implements ISimpleXMLElement 
{
   private static final long serialVersionUID = 1L;

   /** The XML tag name for this element. */
	private String tag = null;

	/** Flag is set whenever the element is currently being bound to an XML fragment. */
 	private boolean isBinding = false;

	/** The schema in which this element is valid; the schema is queried to validate this element's content. */
	private ISchema ownerSchema = null;

	/** Schema information for this element, obtained from the owner schema. */
	private SchemaElementInfo schemaInfo = null;

	/**
	 * The map of attributes EXPLICITLY defined for this element.  During binding, each attribute name-value pair is 
	 * entered in the map, keying each value by its associated name.
	 */
	private final Map<String, String> attrMap = new HashMap<String, String>();

	/**
	 * If this element admits text content, this buffer will contain it. At construction time, it is empty. If the 
	 * element does not admit text content, it will remain so.
	 */
	private final StringBuffer textContentBuf = new StringBuffer();

 	/**
 	 * Construct a new <code>BasicSchemaElement</code> with no parent, no children, and an empty attribute list.
 	 * 
 	 * @param 	tag The element's unique tag name in the owner schema.
 	 * @param	schema The owner schema.  Required for validating the element's content.
 	 */
	public BasicSchemaElement(String tag, ISchema schema)
	{
		super( null );
      assert(tag != null && schema != null);
		this.tag = tag;
		ownerSchema = schema;
		schemaInfo = ownerSchema.getSchemaElementInfo(tag);
		setAllowsChildren(schemaInfo.allowsChildren());
	}

	/**
	 * Update the schema to which this <code>BasicSchemaElement</code> belongs. This method is intended only for use 
	 * during "in-place" migration of an older schema to a newer one.
	 * 
	 * @param schema The new owner schema.
	 * @param tag The element tag in the new schema, or <code>null</code> if it is unchanged.
	 */
	public void updateSchema(ISchema schema, String tag)
	{
      assert(schema != null);
		if(tag != null) this.tag = tag;
		ownerSchema = schema;
		boolean didAllowText = schemaInfo.allowsText();
		schemaInfo = ownerSchema.getSchemaElementInfo(this.tag);
		if(didAllowText && !schemaInfo.allowsText())
			textContentBuf.replace(0, textContentBuf.length(), "");
		setAllowsChildren(schemaInfo.allowsChildren());
	}

	/**
	 * In the owner schema, can the specified attribute be defined on this element?
	 * 
	 * @param attr The name of the attribute to test.
	 * @return <code>True</code> if this element admits the specified attribute; <code>false</code> otherwise.
	 */
	public boolean hasAttribute(String attr)
	{
		return(schemaInfo.isAttributeAllowed(attr));
	}

	/**
	 * Get the current value of the specified attribute on this element.  Returns <code>null</code> if the attribute 
	 * has not been explicitly set.
	 * 
	 * @param attr The attribute name.
	 * @return The attribute value if it has been explicitly set, or <code>null</code> otherwise.
	 * @throws <code>XMLException</code> if the element does not possess the named attribute in the owner schema.
	 */
	public String getAttributeValueByName(String attr) throws XMLException
	{
		if(!schemaInfo.isAttributeAllowed(attr))
			throw new XMLException("Unrecognized attribute", getTag(), attr);
		return(attrMap.get(attr));
	}

	/**
	 * Set the EXPLICIT value of the specified attribute on this element. 
	 * 
	 * @param attr The attribute name.
	 * @param value The desired value for the attribute. Must not be <code>null</code>.
	 * @throws XMLException if the element does not possess the attribute or if the specified value is invalid IAW the 
	 * owner schema.
	 */
	public void setAttributeValueByName(String attr, String value) throws XMLException
	{
		if(!schemaInfo.isAttributeAllowed(attr))
			throw new XMLException("Unrecognized attribute", getTag(), attr);
		else if(!ownerSchema.isValidAttributeValue(this, attr, value))
			throw new XMLException("Invalid attribute value (" + value + ")", getTag(), attr);
		attrMap.put(attr, value);
	}

	/**
	 * Remove the specified attribute from the list of attributes that are explicitly defined on this element.
	 * Use with care, since it permits removal of a required attribute (ie, one that must always be set explicitly)!
	 * 
	 * @param attr The attribute name.
	 */
	public void removeAttributeByName(String attr) { attrMap.remove(attr); }


	//
	// ISimpleXMLElement
	//

	public String getTag() { return(tag); }

	@SuppressWarnings("rawtypes")
	public List<String> getAttributes()
	{
		Iterator i = attrMap.entrySet().iterator();
		List<String> nameValuePairs = new ArrayList<String>();
		while( i.hasNext() )
		{
			Map.Entry entry = (Map.Entry) i.next();
			nameValuePairs.add( (String) entry.getKey() );
			nameValuePairs.add( (String) entry.getValue() );
		}
		return( nameValuePairs );
	}

	public final boolean allowsTextContent() { return(schemaInfo.allowsText()); }

	public String getTextContent() { return( schemaInfo.allowsText() ? textContentBuf.toString() : null ); }

   /**
    * Replace the text content of this XML element, optionally validating it first. The method will have no effect if 
    * the element does not allow text content!
    * 
    * @param content The new text content. If <code>null</code> and the element allows text, the text content will be 
    * reset to an empty string.
    * @param validate If <code>true</code>, the new text content is validated against this element's owner schema prior 
    * to making any change. If <code>false</code>, the new text content is accepted without validation.
    * @return <code>True</code> iff the element's text content was updated. Returns <code>false</code> if the 
    * <code>validate</code> flag was set and the candidate content failed the validation test.
    */
   public boolean setTextContent(String content, boolean validate)
   {
      boolean ok = false;
      if(schemaInfo.allowsText())
      {
         ok = true;
         if(validate) ok = ownerSchema.isValidTextContent(this, content);
         if(ok)
            textContentBuf.replace(0, textContentBuf.length(), (content == null) ? "" : content);
      }
      return(ok);
   }
   
	/** 
	 * Retrieve a list of this element's children, in forward order.  The returned list is backed by the actual content 
	 * of the element, so each entry in the list is an instance of <code>BasicSchemaElement</code> corresponding to one 
	 * of this element's children.
	 * 
	 * @return List of element's children, all of which are instances of <code>BasicSchemaElement</code>; an empty list 
	 * if node has no children. 
	 * @see ISimpleXMLElement#getElementContent()
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<ISimpleXMLElement> getElementContent()
	{
		if( getChildCount() == 0 ) return( new ArrayList(0) );
		else return( (List) children.clone() );
	}

	public boolean hasElementContent() { return(getChildCount() > 0); }

	@SuppressWarnings("rawtypes")
	public void startTag(String tag, List attributes) throws XMLException
	{
		if( isBinding )
			throw new XMLException( "Attempt to rebind instance during modal binding", getTag(), null );
		if( !getTag().equals( tag ) )
			throw new XMLException( "Instance cannot be bound to specified XML tag", tag, null );
		if( attributes.size() % 2 != 0 )
			throw new XMLException( "Attribute specified without a value", getTag(), null );

		// in case we're reusing element, reset its definition: empty text content and any element content, and all 
		// attributes returned to default state
		textContentBuf.replace(0, textContentBuf.length(), "");
		removeAllChildren();
		attrMap.clear();

		// store in the attribute map all attributes that were explicitly defined in the start tag.
		// For each attribute name listed, make sure that attribute is supported for this element, that its value is a 
		// valid one, and that its not a repeat attribute (which is not allowed).
		for( int i = 0; i < attributes.size(); i += 2 )
		{
			String name = (String) attributes.get(i);
			String value = (String) attributes.get(i+1);
         
			// check for repeated attributes
			if( attrMap.containsKey(name) )
				throw new XMLException("Repeated attribute", getTag(), name);

			// make sure attribute is supported and that its value is valid
			if( !schemaInfo.isAttributeAllowed(name) )
				throw new XMLException("Unrecognized attribute", getTag(), name);
			if( !ownerSchema.isValidAttributeValue(this, name, value) )
				throw new XMLException( "Invalid attribute value", getTag(), name );

			// finally, store the [attr, value] pair in our map
			attrMap.put(name, value);
		}

		// check that all required attributes are defined
		String missing = schemaInfo.checkForRequiredAttributes(attrMap);
		if( missing != null )
			throw new XMLException( "Missing required attribute", getTag(), missing );

		// we've begun sequential definition of element's content, so enter modal state
		isBinding = true;
	}

	/**
	 * Append text fragment to the current text content of this XML element during modal binding. 
	 * 
	 * <p>28apr2014 (sar): To allow for character references in the text content, the implementation was modified in
	 * anticipation of being invoked multiple times when the XML parser has to deliver both text and entity references.
	 * Leading and trailing whitespace could be significant in this scenario, so the parsed text fragment is no longer
	 * trimmed prior to being appended to the element's current text content string. (Eff version 4.4.3).</p>
	 * 
	 * @param frag The text fragment just parsed.
	 * @throws XMLException if element does not admit text content or the method is invoked out of context.
	 * @see ISimpleXMLElement#appendTextContent(String)
	 */
	public final void appendTextContent(String frag) throws XMLException
	{
		if(!allowsTextContent())
			throw new XMLException("This element must not contain CDATA", getTag(), null);
		if(!isBinding)
			throw new XMLException("Invoke only during modal binding");

		if(frag != null)
			textContentBuf.append(frag);
	}

	public void addElement(ISimpleXMLElement child) throws XMLException
	{
		if(!isBinding) throw new XMLException("addElement() invoked outside of binding context"); 
		if(!(child instanceof BasicSchemaElement)) throw new IllegalArgumentException("Inconsistent base class");
		if(!getAllowsChildren()) throw new XMLException("This element does not allow children", getTag(), null);

		BasicSchemaElement node = (BasicSchemaElement) child;
		if(!ownerSchema.isValidChildAtIndex(this, node.getTag(), getChildCount()))
			throw new XMLException("Invalid child <" + node.getTag() + "> at index " + getChildCount(), getTag(), null);

		add(node);
	}

	public void endTag(String tag) throws XMLException
	{
		if(!isBinding)
			throw new XMLException("Attempt to exit modal binding before entering modal state", getTag(), null);
		if(!getTag().equals(tag))
			throw new XMLException("Mismatched end tag for element", getTag(), null);

		isBinding = false;
		validate(false);		
	}

	/**
	 * Validate element content.  A shallow check only validates the text content string (if there is one), plus the 
	 * identities and locations of all child elements, under the assumption that the element's attributes were already 
	 * validated during modal binding.  A deep check will validate the attributes and call each child's own validate() 
	 * method.
	 * 
	 * @param deep If <code>true</code>, perform a complete validation as described.
	 * @throws XMLException if method is invoked during the modal state between calls to startTag() and endTag().
	 * @see ISimpleXMLElement#validate(boolean)
	 */
	@SuppressWarnings("rawtypes")
	public void validate(boolean deep) throws XMLException
	{
		if(isBinding) throw new XMLException("Invoked validate() while still binding XML data to this element");

		// validate attribute content only if we're doing a deep check
		if(deep)
		{
			// check that all required attributes are defined
			String missing = schemaInfo.checkForRequiredAttributes(attrMap);
			if(missing != null)
				throw new XMLException("Missing required attribute", getTag(), missing);

			// ensure that each attribute in our map is supported for this element and has a valid value
			List attrValuePairs = getAttributes();
			for(int i=0; i<attrValuePairs.size(); i+=2)
			{
				String name = (String) attrValuePairs.get(i);
				String value = (String) attrValuePairs.get(i+1);
				if(!schemaInfo.isAttributeAllowed(name))
					throw new XMLException("Unrecognized attribute", getTag(), name);
				if(!ownerSchema.isValidAttributeValue(this, name, value))
					throw new XMLException( "Invalid attribute value", getTag(), name );
			}
		}

		// validate text content if the element admits text
		if(allowsTextContent())
		{
			if(!ownerSchema.isValidTextContent(this, textContentBuf.toString()))
				throw new XMLException("Invalid text content", getTag(), null);
		}

		// make sure element has any required children
		if(!ownerSchema.hasRequiredChildren(this)) 
			throw new XMLException("Lacks one or more required child elements");

		// now check the index pos of every child, since an element could restrict where a particular child element 
		// can be inserted.  IF we're doing a deep check, validate the content of each child deeply!
		for(int i = 0; i < getChildCount(); i++)
		{
			BasicSchemaElement node = (BasicSchemaElement) getChildAt(i);
			if(!ownerSchema.isValidChildAtIndex(this, node.getTag(), i)) 
				throw new XMLException("Invalid child <" + node.getTag() + "> at index " + i, getTag(), null);
			if(deep)
				node.validate(true);
		}
	}

	/** 
	 * Return the contents of this basic XML schema element as a single JSON object with the form:
    * <pre>
    * {
    *    "tag" : "...",       // the XML element's tag name
    *    "attrs" : {...},     // JSON object containing all EXPLICITLY SET attributes. Each key in the object is the
    *                         // name of an explicit attribute; the corresponding value is the attribute value -- a 
    *                         // string exactly as it appears in the XML document.
    *    "text" : "...",      // the XML element's text content. Omitted if element does not allow text content.
    *    "kids" : [...]       // the element's child elements, each represented by a JSON object. Omitted if element
    *                         // has no children.
    * }
    * </pre>
	 * @return A JSON object encapsulating this schema element, as described.
	 * @throws JSONException if a JSON formatting error occurs during construction of the JSON object.
	 */
	public JSONObject toJSON() throws JSONException
	{
	   JSONObject jsonEl = new JSONObject();
	   jsonEl.put("tag", tag);
	   
	   JSONObject jsonAttrs = new JSONObject();
	   for(String name : attrMap.keySet()) jsonAttrs.put(name, attrMap.get(name));
	   jsonEl.put("attrs", jsonAttrs);
	   
	   if(allowsTextContent())
	      jsonEl.put("text", textContentBuf.toString());
	   
	   if(getChildCount() > 0)
	   {
	      JSONArray jsonKids = new JSONArray();
	      for(int i=0; i<getChildCount(); i++) 
	      {
	         BasicSchemaElement kid = (BasicSchemaElement) getChildAt(i);
	         jsonKids.put(kid.toJSON());
	      }
	      jsonEl.put("kids", jsonKids);
	   }
	   
	   return(jsonEl);
	}
	
	/**
	 * Redefine this XML schema element IAW the content of the JSON object supplied. The object is expected to be 
	 * formatted exactly as described in <code>toJSON()</code>, and the "tag" field in that object must match this 
	 * element's tag.
	 * 
	 * @param jsonEl A JSON object encapsulating the content of this XML schema element. 
	 * @throws JSONException if the JSON object definition is not compatible with the restrictions enforced by the owner
	 * schema for this XML element.
	 */
	public void fromJSON(JSONObject jsonEl) throws JSONException
	{
	   if(!jsonEl.getString("tag").equals(tag)) throw new JSONException("Tag mismatch!");
	   
	   try
	   {
	      attrMap.clear();
	      JSONObject jsonAttrs = jsonEl.getJSONObject("attrs");
	      String[] keys = JSONObject.getNames(jsonAttrs);
	      if(keys != null) for(int i=0; i<keys.length; i++) 
	         setAttributeValueByName(keys[i], jsonAttrs.getString(keys[i]));
	      
   	   if(allowsTextContent() && jsonEl.has("text"))
   	      setTextContent(jsonEl.getString("text"), false);
   	   else
   	      setTextContent("", false);
   	   
   	   removeAllChildren();
   	   if(jsonEl.has("kids"))
   	   {
   	      isBinding = true;
   	      JSONArray jsonKids = jsonEl.getJSONArray("kids");
   	      for(int i=0; i<jsonKids.length(); i++)
   	      {
   	         JSONObject jsonKid = jsonKids.getJSONObject(i);
   	         BasicSchemaElement kid = (BasicSchemaElement) ownerSchema.createElement(jsonKid.getString("tag"));
   	         kid.fromJSON(jsonKid);
   	         addElement(kid);
   	      }
   	      isBinding = false;
   	   }
	   }
	   catch(XMLException xe)
	   {
	      throw new JSONException("Invalid JSON definition of XML element:\n  " + xe.getMessage());
	   }
	}
	
	//
	// Object
	//

	/**
	 * Returns a string representation of the element: its XML name tag, surrounded by XML tag brackets "<" and ">".
	 * @return A string representation of the object.
	 */
	public String toString() { return( "<" + getTag() + ">" ); }
}
