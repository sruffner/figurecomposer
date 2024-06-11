package com.srscicomp.common.xml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


/**
 * <b>StaxWrapper</b> is a wrapper class that uses Java StAX (Streaming API for AXL) "cursor" API to read and write 
 * XML content adhering to a simple content model to which the Figure Composer document schema "FypML" conforms. The 
 * content model is encapsulated by implementations of the framework defined by the {@link ISimpleXMLContent} and 
 * {@link ISimpleXMLElement} interfaces. Such a content model assumes elements are drawn from a single default 
 * namespace and supports "on the fly" validation of the content as it is parsed from the input stream.
 * 
 * <p>Since <b>XPPWrapper</b> works entirely with instances of the <b>ISimpleXMLContent</b> and <b>ISimpleXMLElement</b>
 * interfaces, it can be used to parse, validate, and write any XML content model that adheres to the restrictions 
 * imposed by these interfaces. It is strictly intended for handling structurally valid XML, as its methods will throw 
 * an {@link XMLException} if the reader or writer detects a problem. It also enforces the single namespace constraint: 
 * if the namespace applicable to any element does not match the content model's namespace, the wrapper will throw an 
 * <b>XMLException</b>. Validation of the XML data stream against the content model is completely controlled by the 
 * particular implementation of the {@link ISimpleXMLContent} model that is passed to {@link #parseContent(Reader,
 * ISimpleXMLContentProvider)} and {@link #writeContent}.</p>
 * 
 * <p>Under the hood, <b>StaxWrapper</b> uses the {@link XMLStreamReader} from the StAX API to handle parsing the 
 * XML content stream. The parser implementation is configured to coalesce adjacent character data sections, is 
 * namespace-aware, and handles the replacement of entity references. On the output side, <b>StaxWrapper</b> does NOT
 * use StAX's XML writer, as it does not handle character entity replacement in the manner required for FypML. Instead,
 * we write the XML content directly to the supplied {@link Writer} in {@link #writeContent}. This
 * method "pretty-prints" the XML output with reasonable indentations, uses the shorthand for empty elements (lacking
 * any children or text content), and replaces non-ASCII characters with character entity references (eg, "&#x03BC" for
 * the Greek letter micron) in attribute values and text content.</p>
 * 
 * <p><b>StaxWrapper</b> sets the "standalone" attribute to "yes" when writing out an XML document.</p>
 * 
 * @author sruffner
 */
public class StaxWrapper
{
   private static final String INDENT = "   ";
   private static final String NEWLINE = "\r\n";
   private static final String SPACE = " ";
   
   /** Instance of a StAX pull parser used to read in and parse XML content. Lazily created. */
   private XMLStreamReader staxParser = null;
   
   /** Stack used to build the XML tree during reading, or traverse it when writing. */
   private Stack<ISimpleXMLElement> elementStack = null;
   /** Indent depth, applicable when formatting XML on output. */
   private int indentDepth = 0;
   
   /** Constructs a wrapper instance.  */
   public StaxWrapper() 
   {
   }

   /**
    * Create a StAX pull parser to parse the character stream reader supplying XML content. We use a namespace-aware 
    * reader, or the element and attribute names will include any namespace prefixes. We want these prefixes culled 
    * away, since we assume the XML document uses a single namespace.
    * 
    * @param r The character stream reader that sources the XML content.
    * @return The StAX pull parser for XML character stream.
    * @throws XMLException if unable to create the parser.
    */
   private static XMLStreamReader createXMLStreamReader(Reader r) throws XMLException
   {
      XMLStreamReader parser = null;
      try
      {
         XMLInputFactory factory = XMLInputFactory.newFactory();
         factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
         factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
         factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
   
         parser = factory.createXMLStreamReader(r);
      }
      catch(XMLStreamException xse)
      {
         throw new XMLException("Failed to create StAX parser: " + xse.getMessage());
      }
      return(parser);
   }

	
   /**
    * Get the contents of the first processing instruction "&lt;?target content... ?&gt;" in the XML content stream 
    * that is directed at the specified target application. The method scans the content sourced by the character 
    * stream reader until it encounters the processing instruction or the start tag of the content's root element.
    * 
    * @param r A character stream reader that sources the XML content. It is NOT closed by this method.
    * @param target The name of the application to which the processing instruction is targeted. This name is the first 
    * XML name token immediately following the PI's "&lt;?" start tag.
    * @return The contents of the processing instruction found, with the target name removed. If no PI was found, an 
    * empty string is returned.
    * @throws XMLException if an I/O error or a parsing error occurred.
    */
   public String getProcessingInstruction(Reader r, String target) throws XMLException 
   {
   	// create a StAX pull parser from the source
      staxParser = createXMLStreamReader(r);
   
   	String pi = "";
   	try
   	{
   	   while(staxParser.hasNext())
   	   {
   	      int evtCode = staxParser.next();
   	      if(evtCode == XMLStreamReader.START_ELEMENT)
   	         break;
   	      else if(evtCode == XMLStreamReader.PROCESSING_INSTRUCTION)
   	      {
   	         
   	         if(staxParser.getPITarget().equalsIgnoreCase(target))
   	         {
   	            pi = staxParser.getPIData();
   	            break;
   	         }
   	      }
   	   }
   	}
   	catch( XMLStreamException xse )
   	{
   		throw new XMLException( XMLException.PARSER_EXCP + ": " + xse.getMessage() );
   	}
   
   	staxParser = null;  // we're done with the parser, so discard it
   	return( pi );
   }
   
   /**
    * Read in and parse XML content in the specified file and and prepare a simple XML content model encapsulating the 
    * parsed content. The file must contain a well-formed XML fragment adhering to the XML grammar recognized by the 
    * content provider, and the XML data is validated against the content model as it is parsed.
    * 
    * @param xmlFile The file containing the XML character data.
    * @param provider The content model provider. The parser will invoke the provider's <i>provideContentModel()</i>
    * method for each processing instruction encountered that matches the provider's target until a valid content model
    * is provided. The processing instruction should contain the content model version and any other information needed
    * by the provider to prepare the content model.
    * @return The simple XML content model to which the parsed XML data is bound.
    * @throws XMLException if the specified file cannot be found, if the parser detects a structural error in the XML 
    * fragment, if it lacked the processing instruction required by the provider to construct an appropriate content 
    * model, if the XML data is invalid with respect to that model, or if any IO error occurs during parsing.
    */
   public ISimpleXMLContent parseContent(File xmlFile, ISimpleXMLContentProvider provider) throws XMLException 
   {
   	// construct a file reader for the file containing XML data; catch and rewrap the "file not found" exception;
   	// be sure to close the file reader no matter what, or we'll leave the file open!
   	FileReader rdr = null;
   	ISimpleXMLContent content = null;
   	try
   	{
   		rdr = new FileReader(xmlFile);
   		content = parseContent(rdr, provider);
   	}
   	catch(IOException ioe)
   	{
   		throw new XMLException(ioe.getMessage());
   	}
   	finally
   	{
   		try { if(rdr != null) rdr.close(); }
   		catch(IOException ioe) {}
   	}
   
   	return(content);
   }
   
   /**
    * Parse XML content from the specified character stream reader and prepare a simple XML content model encapsulating
    * the parsed content. The character data must represent a well-formed XML fragment adhering to the XML grammar 
    * recognized by the content provider, and the XML data is validated against the content model as it is parsed.
    * 
    * @param rdr Character stream reader -- the source of the XML data. The reader is NOT closed by this method.
    * @param provider The content model provider. The parser will invoke the provider's <i>provideContentModel()</i>
    * method for each processing instruction encountered that matches the provider's target until a valid content model
    * is provided. The processing instruction should contain the content model version and any other information needed
    * by the provider to prepare the content model.
    * @return The simple XML content model to which the parsed XML data is bound.
    * @throws XMLException if parser detects a structural error in the XML fragment, if it lacked the processing
    * instruction required by the provider to construct an appropriate content model, if the XML data is invalid with 
    * respect to that model, or if any IO error occurs during parsing.
    */
   public ISimpleXMLContent parseContent(Reader rdr, ISimpleXMLContentProvider provider) throws XMLException 
   {
   	if(rdr == null || provider == null) 
   	   throw new IllegalArgumentException("No source or no content provider specified");
   
   	// create the StAX pull parser from the source
   	staxParser = createXMLStreamReader(rdr);
   
   	// allocate a stack for holding element "objects" as we create them
   	elementStack = new Stack<ISimpleXMLElement>();
   
   	// parsing loop. Note that we catch all parser exceptions here rather than in the individual helper methods 
   	// that process the START_TAG, END_TAG, and TEXT event types. Whether or not an exception occurs, we empty 
   	// and discard the element stack and reset the parser.
   	ISimpleXMLContent contentModel = null;
   	try
   	{
   	   while(staxParser.hasNext())
   	   {
   	      int evtCode = staxParser.next();
   	      
   	      if(evtCode == XMLStreamReader.PROCESSING_INSTRUCTION && contentModel == null)
   	      {
   	         String target = staxParser.getPITarget();
   	         if(target.equalsIgnoreCase(provider.getTargetApplication()))
   	            contentModel = provider.provideContentModel(target + " " + staxParser.getPIData());
   	      }
   	      else if(staxParser.isStartElement())
   	      {
               if(contentModel == null) 
                  throw new XMLException("Missing processing instruction that identifies content model and version;" +
                        "or version number not recognized.");
               processStartElement(contentModel);
   	      }
   	      else if(staxParser.isEndElement())
   	         processEndElement();
   	      else if(staxParser.isCharacters())
   	         processText();
   	      else if(evtCode == XMLStreamReader.ENTITY_REFERENCE)
   	         processEntityRef();
   	   }
   	}
   	catch(XMLStreamException xse)
   	{
   		throw new XMLException(XMLException.PARSER_EXCP + ": " + xse.getMessage());
   	}
   	catch(IOException ioe)
   	{
   		throw new XMLException("IO exception: " + ioe.getMessage());
   	}
   	finally
   	{
   		elementStack.clear();
   		elementStack = null;
   		try { staxParser.close(); }
   		catch( XMLStreamException xse ) { staxParser = null; }
   	}
   	
   	return(contentModel);
   }
   
   /**
    * Handles the details of parsing an XML start element. Queries the content model to create a data object matching 
    * this element tag, collects any explicitly defined attributes associated with the start element, puts the model 
    * object in a modal state corresponding to its step-by-step definition as a series of parser events ending with the
    * matching end tag, and adds the new element to the content model.
    * 
    * @param contentModel The simplified XML content model to which new element is to be bound.
    * @throws XMLException if element's namespace does not match the content model's namespace, or the element is
    * invalid WRT the content model, or a general parsing error occurs.
    * @throws 	IOException if there is a file I/O problem.
    */
   private void processStartElement(ISimpleXMLContent contentModel) throws XMLException, IOException
   {
   	if(!(staxParser.getNamespaceURI().equals( contentModel.getNamespaceUri())))
   		throw new XMLException( XMLException.UNKNOWN_NS + "[" + staxParser.getNamespaceURI() + "]", 
   										staxParser.getLocalName(), null );
   
   	// create a data object in the content model corresponding to the new element's tag name
   	ISimpleXMLElement element = contentModel.createElement( staxParser.getLocalName() );
   
   	// construct an array of Strings containing any explicit attributes associated with the start tag in 
   	// (name,value) pairs: name1, value1, name2, value2, etc.  if there were no explicit attributes, the array 
   	// must be empty.
   	ArrayList<String> attrList = new ArrayList<String>();
   	int nAttr = staxParser.getAttributeCount();
   	if( nAttr < 0 ) nAttr = 0;
   	for( int i=0; i<nAttr; i++ )
   	{
   		attrList.add( staxParser.getAttributeLocalName(i) );
   		attrList.add( staxParser.getAttributeValue(i) );
   	}
   
   	// if the element stack is empty, we MUST be parsing the root element. install the data object at the root of 
   	// the content model (don't validate it; the entire content tree is validated as it is created).  if the stack 
   	// is not empty, then append the new element to the child list of the current element at the top of the stack.
   	if(elementStack.empty())
   		contentModel.setRootElement(element, false);
   	else
   	{
   		ISimpleXMLElement parent = (ISimpleXMLElement) elementStack.peek();
   		parent.addElement( element );
   	}
   
   	// begin modal binding of XML element data to the corresponding data object in the content model.  We have to 
   	// do this AFTER adding element to the current parent, else the inherited attribute mechanism breaks!
   	element.startTag( staxParser.getLocalName(), attrList );
   
   	// now push the new data element onto our element stack
   	elementStack.push( element );
   }
   
   /**
    * Handles the details of parsing an XML end element event, marking the end of the definition of an XML element. 
    * Closes the modal state of the element currently being parsed.
    * 
    * @throws XMLException if the element is invalid WRT its content model, or an end tag is missing (this should
    * be detected by the parser however).
    * @throws IOException if there is a file I/O problem.
    */
   private void processEndElement() throws XMLException, IOException
   {
   	// (just in case parser fails to detect a missing end tag)
   	if(elementStack.empty())
   		throw new XMLException( XMLException.NO_END_TAG, staxParser.getLocalName(), null );
   
   	// pop current element off the element stack and exit the modal state entered upon encountering its start tag
   	ISimpleXMLElement element = (ISimpleXMLElement) elementStack.pop();
   	element.endTag( staxParser.getLocalName() );
   }
   
   /**
    * Handles the details of parsing a character data event, encapsulating a CDATA fragment in the definition of the 
    * current XML element. Whitespace-only CDATA is ignored. Since the {@link ISimpleXMLContent} interface implies that
    * any text content appears in a single block (rather than intermixed among child elements), all CDATA found in a 
    * "mixed content" element are collected into a single text string.
    * 
    * @throws XMLException if the current element being bound does not accept text content, the text content is
    * otherwise invalid, or unexpected text content was found prior to the root element in the XML stream.
    */
   private void processText() throws XMLException
   {
   	// ignore whitespace
   	if(staxParser.isWhiteSpace()) return;
   
   	// play it safe -- there SHOULD be an element on the element stack before this method is called!
   	if(elementStack.empty()) throw new XMLException( "Missing root element tag?" );
   
   	// append text fragment to the current text content of the current element (at top of stack)
   	ISimpleXMLElement element = (ISimpleXMLElement) elementStack.peek();
   	element.appendTextContent(staxParser.getText());
   }
   
   /**
    * Handles the details of parsing an XML entity reference event, encapsulating a character or entity reference within
    * the text content of the current XML element. The resolved character or entity is simply appended to the text 
    * content of the current XML element. 
    * 
    * @throws XMLException if the current element being bound does not accept text content, the character or named 
    * entity reference could not be resolved, or unexpected text content was found prior to the root element.
    */
   private void processEntityRef() throws XMLException 
   {
      // play it safe -- there SHOULD be an element on the element stack before this method is called!
      if( elementStack.empty() ) throw new XMLException( "Missing root element tag?" );
   
      String resolved = staxParser.getText();
      if(resolved == null) throw new XMLException("Unresolved character or entity reference in text content");
      
      // append text fragment to the current text content of the current element (at top of stack)
      ISimpleXMLElement element = (ISimpleXMLElement) elementStack.peek();
      element.appendTextContent(resolved);
   }
   
   /**
    * Write contents of specified XML content model as XML character data using the specified character stream 
    * writer. The writer is NOT closed by this method!
    * 
    * <p>This method only supports writing XML content from a single namespace, as per the requirements of the {@link 
    * ISimpleXMLContent} interface. The content model's namespace is always declared as the default namespace (prefix 
    * is the empty string) in the root element's start tag. In addition, the method attempts to format the output 
    * nicely, using a string of three blanks as the indentation string and a carriage return-linefeed combo ("\r\n") 
    * as the line separator. Thus, this implementation may be wasteful in applications that write the XML data to an 
    * entity that is not meant for human consumption.</p>
    * <p>The method uses an element stack to avoid recursion.</p>
    * 
    * <p><b>IMPORTANT</b>: The method does NOT use <b>XMLStreamWriter</b>, output analog for {@link XMLStreamReader},
    * because it does not handle the character entities in the way we expect. Instead, this method writes directly to
    * the character stream writer and only delivers characters from the US-ASCII character set. There's no namespace 
    * support, which is OK because that's assumed in our simple XML content model. Double-quotes, not single quotes, 
    * surround attribute values. Character escaping in attribute values and text content works as follows:
    * <ul>
    * <li>In attribute values, a literal ampersand, less-than sign or double-quote is represented by its named 
    * character entity. Any character code point N < 32 || N > 255 is represented by the entity "&amp;#xN;".</li>
    * <li>In text content, a literal ampersand or less-than sign is represented by its named character entity, and any
    * character code point N==0 || N>255 is represented by the entity "&amp;#xN;".</li>
    * </ul>
    * </p>
    * 
    * @param wrt The character stream writer
    * @param contentModel The simple XML content model to be serialized
    * @param validate If true, the content model is validated before serialization.
    * @throws XMLException if the content model is found to be invalid, or if any IO error occurs during writing.
    */
   public void writeContent(Writer wrt, ISimpleXMLContent contentModel, boolean validate) throws XMLException
   {
      if(contentModel == null) throw new IllegalArgumentException( "No content model specified");
      if(contentModel.getRootElement() == null) throw new XMLException( "Empty content model cannot be written" );
   
   	// if requested, validate the entire content tree now
   	if(validate) contentModel.validate();
   
   	// allocate stack for holding elements as we write them; push the content model's root element onto it
      elementStack = new Stack<ISimpleXMLElement>();
   	elementStack.push( contentModel.getRootElement() );
      indentDepth = 0;
      
   	try
   	{
   	   // write the XML declaration on the first line
         wrt.append("<?xml version=\"1.0\" encoding=\"us-ascii\" standalone=\"yes\"?>").append(NEWLINE);
   
   		// if the content model has a processing instruction, write it by itself on the second line.  Its format is 
   		// trusted.
   		String pi = contentModel.getProcessingInstruction();
   		if( pi!=null && !pi.isEmpty())
   		   wrt.append("<?" + pi + "?>").append(NEWLINE);
   
   		// write elements in our element stack until it is empty
   		while(!elementStack.empty())
   		{
   			writeElement(wrt, contentModel);
   		}
   
   		wrt.append(NEWLINE);
   	}
   	catch(IOException ioe)
   	{
   	   throw new XMLException(ioe.getMessage());
   	}
   	finally
   	{
   		elementStack.clear();
   		elementStack = null;
   	}
   }
   
   /**
    * Handles details of writing a simplified XML element, using the element stack to handle all the children of the 
    * current element without having to resort to recursion.  
    *
    * @param wrt The character stream writer.
    * @param contentModel the simplified XML content model to which new element is to be bound
    * @throws IOException if a problem occurs while writing data
    */
   private void writeElement(Writer wrt, ISimpleXMLContent contentModel) throws IOException
   {
      // algorithm:  pop stack. If object is an XML element, then it has not been processed yet. Write out its start
   	// start tag and attributes. If it has no element content, write the text content (if any) and close tag.  
   	// Otherwise, push it back onto the stack, followed by a NULL marker indicating that the next element on the 
   	// stack just needs to be closed, followed by the element's children in reverse order (so they'll be taken off 
   	// the stack in the correct order!). 
   	Object o = elementStack.pop();
   	if(o instanceof ISimpleXMLElement)
   	{
   		ISimpleXMLElement el = (ISimpleXMLElement) o;
   		boolean isRoot = elementStack.isEmpty();
   		
   		// indent IAW the current depth
   		for(int i=0; i<indentDepth; i++) wrt.append(INDENT);
   
   		// write start tag
   		wrt.append("<").append(el.getTag()).append(SPACE);
   		
   		// write attribute="value" pairs. for the root element only: write the default namespace URI, then remaining 
   		// attributes start on next line.
   		if(isRoot)
   		   wrt.append("xmlns=\"").append(contentModel.getNamespaceUri()).append("\"").append(NEWLINE+INDENT);
   
   		List<String> attrList = el.getAttributes();
   		for( int i=0; i < attrList.size(); i+=2 )
   		{
   		   writeAttribute(wrt, attrList.get(i), attrList.get(i+1));
   		   if(i+2 < attrList.size()) wrt.write(SPACE);
   		}
   
   		if(!el.hasElementContent())
   		{
   		   // if element is empty (no child elements nor text content), then close start tag with shorthand "/>".
   		   // Else, if element has ONLY text content, close start tag with ">", write the text content, then 
   		   // complete the element with the full end tag "</tag>". In either case, we're done with the current
   		   // element, so go to next line.
   		   String s = el.getTextContent();
   		   if(s==null || s.isEmpty())
   		      wrt.append("/>").append(NEWLINE);
   		   else
   			{
   		      wrt.append(">");
   		      
   			   // if text content relatively long, we go to next line and don't bother to indent first line of text.
   		      // but we do have to indent the end tag
   			   if(s.length() > 200)
   			   {
   			      wrt.append(NEWLINE);
   		         writeTextContent(wrt, s);
   		         wrt.append(NEWLINE);
   		         for(int i=0; i<indentDepth; i++) wrt.append(INDENT);
   			   }
   			   else writeTextContent(wrt, s);
   			   wrt.append("</").append(el.getTag()).append(">").append(NEWLINE);
   			}
   		}
   		else
   		{
   		   // if element has children, close the start tag, go to the next line, and increase the indent depth.
   		   // Push the element back onto stack, followed by null marker and the child elements.
   		   wrt.append(">").append(NEWLINE);
   		   ++indentDepth;
   		   
   			elementStack.push( el );
   			elementStack.push( null );
   			List<ISimpleXMLElement> kids = el.getElementContent();
   			int i = kids.size() - 1;
   			while( i >= 0 )
   			{
   				elementStack.push(kids.get(i));
   				--i;
   			}
   		}
   	}
   	else
   	{
   		// when the object popped off the stack is not an ISimpleXMLElement, then it must be the NULL marker that 
   		// indicates we're done processing the child elements of the next element on the stack. We pop that element, 
   		// write out its text content (if any), and close its definition with endTag(). Indent to ensure text content
   	   // starts at same depth as children of the element, and that end tag is at same depth as element's start tag.
   		ISimpleXMLElement el = (ISimpleXMLElement) elementStack.pop();
   		
   		if(el.allowsTextContent())
   		{
   		   String s = el.getTextContent();
   		   if(!s.isEmpty())
   		   {
   	         for(int i=0; i<indentDepth; i++) wrt.append(INDENT);
   	         writeTextContent(wrt, s);
   	         wrt.append(NEWLINE);
   		   }
   		}
   
   		--indentDepth;
   		for(int i=0; i<indentDepth; i++) wrt.append(INDENT);
   		wrt.append("</").append(el.getTag()).append(">").append(NEWLINE);
   	}
   }
   
   /**
    * Write out the attribute-value pair as <i>attrName="value"</i>. Any character in the attribute value that has a
    * code point outside of [32..255] are written as character entities in the form "&#xHHHH;", where HHHH is the
    * hexadecimal value for the 16-bit character code. The range [32..255] corresponds to the printable ASCII set. The
    * method also handles escaping of the special XML characters "&" and "<", as well as the double-quote character 
    * used to delimit the attribute value.
    * 
    * @param wrt The character stream writer.
    * @param attr The literal attribute name. Assumed to only contain printable ASCII characters.
    * @param value The value of the attribute
    */
   private void writeAttribute(Writer wrt, String attr, String value) throws IOException
   {
      wrt.append(attr).append("=").append("\"");
      
      int pos = 0;
      int posSpecial = 0;
      while(posSpecial < value.length())
      { 
         char c = value.charAt(posSpecial);
         if(c < 0x020 || c > 0x07F || c == '<' || c == '&' || c == '"')
         {
            if(pos < posSpecial) wrt.write(value.substring(pos, posSpecial));
            pos = posSpecial + 1;
            if(c == '<') wrt.write("&lt;");
            else if(c == '&') wrt.write("&amp;");
            else if(c == '"') wrt.write("&quot;");
            else wrt.append("&#x").append(Integer.toHexString((int) c)).append(";"); 
         }
         ++posSpecial;
      }
   
      if(pos < value.length()) wrt.write(value.substring(pos));
      wrt.write("\"");
   }
   
   /**
    * Writes the text content of an element, writing any non-ASCII characters as XML character references of the form 
    * "&#xHHHH;", where HHHH is the hexadecimal value for the 16-bit non-ASCII character to be serialized. Any char 
    * value == 0 or greater than 255 is considered non-ASCII. The method also handles escaping of the special XML 
    * characters "&" and "<".
    * 
    * @param wrt The character stream writer.
    * @param text Text content of element currently being serialized.
   */
   private void writeTextContent(Writer wrt, String text) throws IOException
   {
      int pos = 0;
      int posSpecial = 0;
      while( posSpecial < text.length() )
      { 
         char c = text.charAt(posSpecial);
         if(c == 0 || c > 0x07F || c == '<' || c == '&')
         {
            if(pos < posSpecial) wrt.write(text.substring(pos, posSpecial));
            pos = posSpecial + 1;
            if(c == '<') wrt.write("&lt;");
            else if(c == '&') wrt.write("&amp;");
            else wrt.append("&#x").append(Integer.toHexString((int) c)).append(";"); 
         }
         ++posSpecial;
      }
   
      if(pos < text.length()) wrt.write(text.substring(pos));
   }
}
