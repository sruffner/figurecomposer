package com.srscicomp.common.xml;


/**
 * The <code>ISimpleXMLContent</code> model provider interface. It provides an empty content model conforming to the
 * schema version specified in the processing instruction that should immediately precede the content's start tag in an
 * XML document. The expected form of the processing instruction is left to the implementation.
 * 
 * @author sruffner
 */
public interface ISimpleXMLContentProvider
{
   /**
    * Provide a simple XML content model (initially empty) corresponding to the schema version specified in the 
    * processing instruction parsed from the XML document. This method is called early during pull-parsing of the XML
    * document by <code>XPPWrapper</code>.
    * @param procInst A processing instruction found in the XML document being parsed, the target of which is the name
    * token returned by <code>getTargetApplication()</code>.
    * @return The simple content model conforming to the schema version specified in the processing instruction. If the
    * processing instruction is empty or does not conform to the format expected by the provide, return null.
    */
   ISimpleXMLContent provideContentModel(String procInst);
   
   /**
    * Get the name of this content provider. The name token must be that which appears in a processing instruction 
    * containing the content version (and possibly other information): <i>&lt;target content... &gt;</i>. During 
    * parsing -- if the content model is still undetermined -- any processing instruction with this target token is 
    * passed to <code>provideContentModel()</code>.
    * @return The name of this content provider.
    */
   String getTargetApplication();
}
