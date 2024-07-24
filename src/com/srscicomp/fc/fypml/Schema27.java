package com.srscicomp.fc.fypml;

import com.srscicomp.common.xml.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * <b>Schema27</b> is the encapsulation of the <i>FypML</i> figure model XML schema version 27. It extends {@link
 * Schema26} and includes support for migrating schema version 26 documents to version 27. This version was first
 * introduced with version 5.5.0 of <i>FigureComposer</i>.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul> 
 *    <li>(23-24 jul 2024, for app V5.5.0) Added "minSize" attribute to the "scatter" and "scatter3d" elements. This
 *    measured value specifies a minimum symbol size for the scatter plot - as a way to ensure a visible mark is made
 *    for every scatter point. It also helps with replicating a Matlab bubblechart. The default is 0in, so existing
 *    figures are unaffected and no schema migration is required.</li>
 * </ul>
 * </p>
 * 
 * @author  sruffner
 */
class Schema27 extends Schema26
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema27"); }
   @Override public int getVersion() { return(27); }
   @Override public String getLastApplicationVersion() { return("5.5.0"); }

   /** New scatter plot attribute specifies a minimum symbol size as a physical measure in in/cm/mm/pt. */
   final static String A_MINSIZE = "minSize";
   /** Default for the "minSize" attribute of a "scatter" or "scatter3d" element. */
   final static String DEFAULT_MINSIZE= "0in";

   
   /**
    * This element map contains {@link SchemaElementInfo} objects for each element that is new to this schema or has a 
    * different attribute set compared to the previous schema.
    */
   private static final Map<String, SchemaElementInfo> elementMap27;

   static
   {
      elementMap27 = new HashMap<>();

      // 23jul2024: Added "minSize" property to "scatter" element.
      elementMap27.put( EL_SCATTER, new SchemaElementInfo( false,
            new String[] {EL_SCATTERLINE}, 
            new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_FILLCOLOR, A_TITLE, 
                          A_LEGEND, A_MODE, A_TYPE, A_SIZE, A_MINSIZE, A_SRC},
            new String[] {A_SRC} ));

      // 24jul2024: Added "minSize" property to "scatter3d"
      elementMap27.put( EL_SCATTER3D, new SchemaElementInfo( false,
            new String[] {EL_SYMBOL},
            new String[] {A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE,
                  A_LEGEND, A_MODE, A_BASELINE, A_BKG, A_STEMMED, A_DOTSIZE, A_DOTCOLOR, A_BARWIDTH, A_MINSIZE, A_SRC},
            new String[] {A_SRC} ));


   }
   
   /**
    * Overridden to recognize any elements added and exclude any elements removed in this schema version; else defers 
    * to the superclass implementation. 
    */
   @Override public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap27.containsKey(elTag) || super.isSupportedElementTag(elTag));
   }

   /**
    * Overridden to provide schema element information for any element class added or revised in this schema version; 
    * for all other element classes, it defers to the superclass implementation. 
    */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
   {
      SchemaElementInfo info = elementMap27.get(elTag);
      return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
   }

   /**
    * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
    * defers to the super class, with the following exceptions:
    * <ul>
    * <li>Validate the new "minSize" attribute of the <i>scatter</i> and <i>scatter3d</i> elements: It must be a valid
    * measure attribute (nonnegative; physical units only).</li>
    * </ul>
    */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
   {
      String tag = e.getTag();
      if((EL_SCATTER.equals(tag) || EL_SCATTER3D.equals(tag)) && A_MINSIZE.equals(attr))
         return(isValidMeasureAttributeValue(value, false, false));
      
      return(super.isValidAttributeValue(e, attr, value));
   }

   /**
    * This method handles the actual details of migrating from the previous version to this one. It makes the following 
    * changes:
    * <ul>
    *    <li><i>Currently, no changes are needed. However, since we're migrating "in place", we have to replace all
    *    references to the previous schema with the current schema!</i></li>
    * </ul>
    */
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
         
         // migrate the element object's schema info.
         e.updateSchema(this, null);

         // if element has any children, push them onto the stack so that we check them as well! We don't push on any
         // child that was added during migration of the parent, since it will already conform to current schema.
         for(int i=0; i<e.getChildCount(); i++)
         {
            BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
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

}
