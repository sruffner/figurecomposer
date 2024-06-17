package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;


/**
 * <code>Schema16</code> is the encapsulation of the <i>DataNav</i> figure model XML schema version 16. It extends 
 * {@link Schema15} and includes support for migrating schema version 15 documents to version 16.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul>
 *    <li>(07jul2014) Introduced support for a fully transparent color option for most RGB color attributes in the 
 *    <i>FypML</i> schema. Translucent colors (alpha channel between 0x01 and 0xFE) are NOT supported. To specify an
 *    RGB color as transparent, the attribute value is "none"; otherwise, it is a six-digit hexadecimal value "RRGGBB",
 *    as before. Affected attributes: <i>strokeColor, fillColor, bkg</i>. The other RGB color attribute, <i>cmapnan</i>
 *    on the <i>zaxis</i> element, cannot be transparent and thus does not allow "none" as a value. No changes are
 *    required during schema migration, since only opaque RGB color values were allowed prior to this version.</li>
 *    <li>(07jul2014) Support for transparent color eliminates the need for the boolean <i>filled</i> attribute on the 
 *    <i>trace</i> and <i>raster</i> elements. The same effect is achieved by setting the fill color to transparent.
 *    During schema migration, the <i>filled</i> attribute is removed from every <i>trace</i> and <i>raster</i> element.
 *    When applicable (the <i>filled</i> attribute applies only to the "histogram" and "errorband" display modes for
 *    <i>trace</i>, and only the "histogram" mode for <i>raster</i>) and its value was false (not filled), then the
 *    <i>fillColor</i> attribute is set to "none" on that element.</li>
 *    <li>(05aug2014) Added new optional attribute to <i>textbox</i>, <i>axis</i> and <i>zaxis</i> nodes: <i>lineHt</i>
 *    is a float-valued attribute, range-restricted to [0.8 .. 3.0], that specifies the node's text line height as a 
 *    fraction of its font size. Its implicit value is 1.2. Introduced to give user control over inter-line spacing in a
 *    multi-line text box or axis label.</li>
 * </ul>
 * </p>
 * 
 * @author 	sruffner
 */
class Schema16 extends Schema15
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema16"); }
   @Override public int getVersion() { return(16); }
   @Override public String getLastApplicationVersion() { return("4.6.0"); }

   /** 
    * New float-valued attribute specifies text line height for a "textbox", "axis", or "zaxis" element -- as a fraction
    * of the element's font size. Range-restricted. Gives user control over inter-line spacing in a text block or a
    * multi-line axis label.
    */
   final static String A_LINEHT = "lineHt";
   /** Default value for the "lineHt" attribute. */
   final static String DEFAULT_LINEHT = "1.2";
   /** Minimum value for text line height (as a fraction of current font size). */
   final static double MINLINEHT = 0.8;
   /** Maximum value for text line height (as a fraction of current font size). */
   final static double MAXLINEHT = 3.0;
   
   /**
	 * This element map contains {@link SchemaElementInfo SchemaElementInfo} objects for each element that
	 * is new to this schema or has a different attribute set compared to the previous schema.
	 */
	private static final Map<String, SchemaElementInfo> elementMap16;

	static
	{
		elementMap16 = new HashMap<>();
		
      // (07jul2014) The "trace" and "raster" elements no longer possess the "filled" attribute. Setting the "filled"
		// attribute to "true" is the same as setting the element's "fillColor" to "none" (transparent).
      elementMap16.put( EL_TRACE, new SchemaElementInfo( false, 
            new String[] {EL_SYMBOL, EL_EBAR}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP,
                          A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_LEGEND, A_SKIP, A_MODE, A_BARWIDTH, 
                          A_BASELINE, A_AVG, A_XOFF, A_YOFF, A_SRC}, 
            new String[] {A_SRC} ));

      elementMap16.put( EL_RASTER, new SchemaElementInfo( false, 
            new String[] {}, 
            new String[] {A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_MODE, A_BASELINE, A_XOFF, A_HEIGHT, A_SPACER, A_NBINS, A_AVG, A_LEGEND, A_SRC}, 
            new String[] {A_SRC} ));
      
      // (05aug2014) Added optional attribute "lineHt" to the "textbox", "axis" and "zaxis" elements.
      elementMap16.put( EL_TEXTBOX, new SchemaElementInfo( true, 
            new String[] {}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_CLIP, A_GAP, A_BKG, 
                          A_HALIGN, A_VALIGN, A_LOC, A_WIDTH, A_HEIGHT, A_LINEHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));
      
      elementMap16.put( EL_AXIS, new SchemaElementInfo( false, 
               new String[] {EL_TICKS}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                             A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_HIDE, A_UNITS, A_TITLE, A_SPACER, 
                             A_LABELOFFSET, A_START, A_END, A_LOG2, A_LINEHT}, 
               new String[] {A_START, A_END} ));
      
      elementMap16.put( EL_ZAXIS, new SchemaElementInfo( false, 
            new String[] {EL_TICKS}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                          A_STROKEJOIN, A_STROKECOLOR, A_HIDE, A_TITLE, A_SPACER, A_LABELOFFSET, A_START, A_END, A_GAP, 
                          A_SIZE, A_EDGE, A_CMAP, A_CMODE, A_CMAPNAN, A_LINEHT}, 
            new String[] {A_START, A_END} ));
	}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in this schema version; else defers 
    * to the superclass implementation.
    */
   @Override public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap16.containsKey(elTag) || super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in this schema version; 
	 * for all other element classes, it defers to the superclass implementation. 
	 */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = elementMap16.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
    *    <li>The generic <i>strokeColor</i> and <i>fillColor</i> attributes, and the <i>bkg</i> attribute of a 
    *    <i>textbox</i> or <i>image</i> element, must be a 6-digit hexadecimal string representing an opaque RGB color 
    *    ("RRGGBB"), <b>OR</b> the value "none" for fully transparent black. The only other RGB color-valued attribute
    *    in <i>FypML</i>, <i>cmapnan</i> of the <i>zaxis</i> element, does NOT allow the value "none", so validation of
    *    that attribute is unchanged.</li>
    *    <li>The new <i>lineHt</i> attribute is a float-valued, range-restricted attribute.</li>
	 * </ul>
	 */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      if(A_FILLCOLOR.equals(attr) || A_STROKECOLOR.equals(attr) || 
            (A_BKG.equals(attr) && (e.getTag().equals(EL_IMAGE) || e.getTag().equals(EL_TEXTBOX))))
      {
         return(ATTRVAL_NONE.equals(value) || isValidColorAttributeValue(value));
      }
      else if(A_LINEHT.equals(attr))
         return(this.isValidFloatAttributeValue(value, MINLINEHT, MAXLINEHT));
      
      return(super.isValidAttributeValue(e, attr, value));
	}

   /**
	 * This method handles the actual details of migrating <code>Schema15</code> content to <code>Schema16</code>. It 
    * makes the following changes:
	 * <ul>
	 *    <li>For every <i>trace</i> and <i>raster</i> element, check the value of its <i>filled</i> attribute in the
	 *    old schema. If it is false or not explicitly set, then set the element's <i>fillColor = "none"</i>, for a 
	 *    transparent fill. However, do this ONLY when the <i>filled</i> attribute is used: for "histogram" and 
	 *    "errorband" display modes of a <i>trace</i> node, and for the "histogram" mode of a <i>raster</i>. The 
	 *    <i>filled</i> attribute is removed during migration.</li>
	 *    <li>Special case: A <i>trace</i> node in the "errorband" display mode. Here, the fill color for the errorband 
	 *    comes from the trace's <i>ebar</i> child, NOT the trace node itself. To migrate this special case, we leave
	 *    the trace node's <i>fillColor</i> attribute unchanged and explicitly set <i>fillColor = "none"</i> on its 
	 *    <i>ebar</i> child.</li>
    *    <li><i>As with all incremental migrations, since we're migrating "in place", we have to replace all references 
    *    to the previous schema with the current schema!</i></li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
	 */
	public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
	{
		if(oldSchema.getVersion() != getVersion() - 1) 
			throw new XMLException("A schema instance can only migrate from the previous version.");

		// for trace in the "errorband" display mode, if obsolete "filled" flag is false, then that is achieved in 
		// schema 16 by explicitly setting "fillColor=none" on the trace's ebar child, not on the trace node itself. This
		// flag is set when we need to do that for an "ebar".
		boolean needTransparentFillEBar = false;
		
      // update the content of the old schema in place...
      Stack<BasicSchemaElement> elementStack = new Stack<>();
      elementStack.push((BasicSchemaElement) oldSchema.getRootElement());
      while(!elementStack.isEmpty())
      {
         BasicSchemaElement e = elementStack.pop();
         String elTag = e.getTag();
         
         // for trace and raster elements, determine whether obsolete "filled" flag is set or not, then remove it. If
         // the element is in a display mode for which the "filled" flag is ignored, then we assume "filled=true" so
         // that we don't change the fillColor attribute after migrating the object's schema info. Finally, here we
         // detect the special case of a trace node in the errorband display mode with filled=false. We have to set
         // fillColor="none" on the trace's "ebar" node, not on the trace itself.
         boolean wasFilled = false;
         if(EL_TRACE.equals(elTag) || EL_RASTER.equals(elTag))
         {
            String mode = e.getAttributeValueByName(A_MODE);
            String value = e.getAttributeValueByName(A_FILLED);
            wasFilled = true;
            if(DISPMODE_BAND.equals(mode) || DISPMODE_HIST.equals(mode))
            {
               wasFilled = "true".equals(value);
               if(DISPMODE_BAND.equals(mode) && !wasFilled)
               {
                  needTransparentFillEBar = true;
                  wasFilled = true;
               }
            }
            e.removeAttributeByName(A_FILLED);
         }
         
         // migrate the element object's schema info.
         e.updateSchema(this, null);

         // for trace and raster elements, if the obsolete "filled" flag was false, then explicitly set "fillColor" to
         // "none", indicating a transparent fill color
         if((EL_TRACE.equals(elTag) || EL_RASTER.equals(elTag)) && !wasFilled)
         {
            e.setAttributeValueByName(A_FILLCOLOR, ATTRVAL_NONE);
         }
         
         // for an ebar element, if the parent trace element is in the "errorband" display mode and the old "filled"
         // flag was false, then the errorband is filled IAW the ebar's fillColor. That scenario is detected during
         // migration of the parent trace, and here we explicitly set the ebar child's fillColor to "none"
         if(EL_EBAR.equals(elTag) && needTransparentFillEBar)
         {
            e.setAttributeValueByName(A_FILLCOLOR, ATTRVAL_NONE);
            needTransparentFillEBar = false;
         }
         
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
