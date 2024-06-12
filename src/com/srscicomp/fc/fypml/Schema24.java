package com.srscicomp.fc.fypml;

import java.util.Stack;
import java.util.regex.Pattern;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.XMLException;

/**
 * <b>Schema24</b> is the encapsulation of the <i>FypML</i> figure model XML schema version 24. It extends {@link 
 * Schema23} and includes support for migrating schema version 23 documents to version 24. This version was first 
 * introduced with version 5.4.0 of <i>FigureComposer</i>.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul> 
 *    <li>(Feb 2023, for app V5.4.0) Introduced the concept of "styled text" format, in which font style, text color,
 *    underline, superscript, and subscript can vary on a character-by-character basis in the text. The attribute 
 *    changes are encoded in a suffix appended to the rendered text S, separated by a single '|' character. Modified 
 *    the following elements to allow styled text in the "title" attribute: "label", "shape", "axis", and "colorbar". 
 *    Also, the text content of a "textbox" element may be in styled text format (in the graphic model, the
 *    styled text is stored in the textbox node's "title" attribute). <i>This change does not require any schema 
 *    migration, but we've added a method to validate a string in styled text format. Also, incrementing the schema
 *    ensures that older versions of FC do not attempt to process a newer document containing styled text.</li>
 *    <li>(23 Feb 2023, for app V5.4.0) Added 7 new adornment shapes: "parallelogram", "pentagram", "hexagram",
 *    "pentagon", "hexagon", "octagon" and "hlinethru". Existing documents are unaffected by this enhancement.</li>
 *    <li>(24 Feb 2023, for app V5.4.0) Added support for 8 new colormap options analogous to Matlab's "cool", "copper",
 *    "bone", and "hsv" maps, along with their reverse-direction counterparts.</li>
 * </ul>
 * </p>
 * 
 * @author  sruffner
 */
class Schema24 extends Schema23
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema24"); }
   @Override public int getVersion() { return(24); }
   @Override public String getLastApplicationVersion() { return("5.4.0"); }

   final static String ADORN_PARALLELOGRAM = "parallelogram";
   final static String ADORN_PENTAGRAM = "pentagram";
   final static String ADORN_HEXAGRAM = "hexagram";
   final static String ADORN_PENTAGON = "pentagon";
   final static String ADORN_HEXAGON = "hexagon";
   final static String ADORN_OCTAGON = "octagon";
   final static String ADORN_HLINETHRU = "hlinethru";
   /**
    * The names of all adornments supported in schema version 24, as of 02/23/23. Same as the list defined in schema 9,
    * with sevem additions.
    */
   final static String[] ADORN24_CHOICES;
   static
   {
      int n = ADORN9_CHOICES.length;
      ADORN24_CHOICES = new String[n+7];
      for(int i=0; i<n; i++) ADORN24_CHOICES[i] = ADORN9_CHOICES[i];
      ADORN24_CHOICES[n] = ADORN_PARALLELOGRAM;
      ADORN24_CHOICES[n+1] = ADORN_PENTAGRAM;
      ADORN24_CHOICES[n+2] = ADORN_HEXAGRAM;
      ADORN24_CHOICES[n+3] = ADORN_PENTAGON;
      ADORN24_CHOICES[n+4] = ADORN_HEXAGON;
      ADORN24_CHOICES[n+5] = ADORN_OCTAGON;
      ADORN24_CHOICES[n+6] = ADORN_HLINETHRU;
   }

   /** A colormap similar to Matlab's predefined "cool" colormap. */
   final static String CMAP_COOL = "cool";
   /** The reverse-direction version of the "cool" colormap. */
   final static String CMAP_REVERSECOOL = "reversecool";
   /** A colormap similar to Matlab's predefined "copper" colormap. */
   final static String CMAP_COPPER = "copper";
   /** The reverse-direction version of the "copper" colormap. */
   final static String CMAP_REVERSECOPPER = "reversecopper";
   /** A colormap similar to Matlab's predefined "bone" colormap. */
   final static String CMAP_BONE = "bone";
   /** The reverse-direction version of the "bone" colormap. */
   final static String CMAP_REVERSEBONE = "reversebone";
   /** A colormap similar to Matlab's predefined "hsv" colormap. */
   final static String CMAP_HSV = "hsv";
   /** The reverse-direction version of the "hsv" colormap. */
   final static String CMAP_REVERSEHSV = "reversehsv";

   /** Revised (as of 24feb2023) set of choices for the <i>cmap</i> attribute of a color bar element. */
   final static String[] CMAP_CHOICES_V24 = {
      CMAP_GRAY, CMAP_REVERSEGRAY, CMAP_HOT, CMAP_REVERSEHOT, CMAP_AUTUMN, CMAP_REVERSEAUTUMN, 
      CMAP_JET, CMAP_REVERSEJET, CMAP_TROPIC, CMAP_REVERSETROPIC, CMAP_COOL, CMAP_REVERSECOOL,
      CMAP_COPPER, CMAP_REVERSECOPPER, CMAP_BONE, CMAP_REVERSEBONE,CMAP_HSV, CMAP_REVERSEHSV,
   };

   /** For validating the attributes suffix of <i>FypML styled text format</i>. */
   final static Pattern styledTextSuffixPattern = Pattern.compile("[a-fA-F0-9SsnUupwiI:,-]+");
   
   /** 
    * Does the specified string conform to the requirements of the <i>FypML styled text format</i>: "S|C", in which
    * changes in font style, text color, underline, sub/superscript are encoded in the attribute codes suffix C? Note
    * that C is separated from the character sequence S by a single pipe '|' character, and that S without the pipe or
    * code suffix is also valid (all characters in S are styled the same).
    * 
    * <p>This method only verifies that the attribute code suffix, if present, only contains the limited set of 
    * characters that may appear in that suffix. It does not validate it fully.</p>
    * 
    * @param s A string.
    * @return False if <i>s</i> contains an attribute codes suffix with 1 or more invalid characters. 
    */
   boolean isValidStyledTextString(String s)
   {
      if(s == null) return(false);
      int idx = s.indexOf('|');
      if((idx > 0) && (idx < s.length() - 1)) 
         return(styledTextSuffixPattern.matcher(s.substring(idx+1)).matches());
      else
         return(true);
   }
   
   /**
    * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
    * defers to the super class, with the following exceptions:
    * <ul>
    * <li>The <i>title</i> attribute of some elements may be formatted as <i>FypML styled text</i>, in which an
    * attributes "suffix" follows the actual text: "S[|N1:codes1,N2:codes2,...]". If the suffix is present, it must
    * only contain certain characters. Affected elements: "label", "shape", "axis", "colorbar", and "axis3d".<li>
    * <li>Added 7 new adornment shapes. Validation of the relevant properties ("cap"; "type" on "shape", "symbol", and
    * "scatter" elements) was updated accordingly.</li>
    * <li>Added 8 new colormap choices. Validation of the <i>cmap</i> property updated accordingly.</li>
    * </ul>
    */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
   {
      String tag = e.getTag();
      if(A_TITLE.equals(attr) &&
            (EL_LABEL.equals(tag) || EL_SHAPE.equals(tag) || EL_AXIS.equals(tag) || EL_COLORBAR.equals(tag)))
         return(isValidStyledTextString(value));
      else if(A_CAP.equals(attr) || 
            (A_TYPE.equals(attr) && (EL_SHAPE.equals(tag) || EL_SYMBOL.equals(tag) || EL_SCATTER.equals(tag))))
         return(isValidEnumAttributeValue(value, ADORN24_CHOICES));
      else if(EL_COLORBAR.equals(tag) && A_CMAP.equals(attr))
         return(isValidEnumAttributeValue(value, CMAP_CHOICES_V24));

      return(super.isValidAttributeValue(e, attr, value));
   }
   
   /**
    * Overridden to validate the text content of the "textbox" element, which may be formatted as <i>FypmL styled 
    * text</i>> "S[|N1:codes1,N2:codes2,...]", where S is the rendered text (including linefeeds) and the remainder is
    * the attributes "suffix". If the suffix is present, it must contain only certain characters.
    */
   @Override public boolean isValidTextContent(BasicSchemaElement e, String text)
   {
      if(e.getTag().equals(EL_TEXTBOX))
      {
         if(text == null) return(true);
         text = text.trim();
         int idx = text.indexOf('|');
         if((idx > 0) && (idx < text.length() - 1)) 
            return(styledTextSuffixPattern.matcher(text.substring(idx+1)).matches());
         else
            return(true);
      }
      return super.isValidTextContent(e, text);
   }
   
   /**
    * This method handles the actual details of migrating from the previous version to this one. It makes the following 
    * changes:
    * <ul>
    *    <li><i>Currently, no changes are needed. However, since we're migrating "in place", we have to replace all
    *    references to the previous schema with the current schema!</i> </li>
    * </ul>
    */
   public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
   {
      if(oldSchema.getVersion() != getVersion() - 1) 
         throw new XMLException("A schema instance can only migrate from the previous version.");

      // update the content of the old schema in place...
      Stack<BasicSchemaElement> elementStack = new Stack<BasicSchemaElement>();
      elementStack.push((BasicSchemaElement) oldSchema.getRootElement());
      while(!elementStack.isEmpty())
      {
         BasicSchemaElement e = elementStack.pop();
         // String elTag = e.getTag();
         
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
