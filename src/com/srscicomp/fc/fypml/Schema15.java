package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;


/**
 * <code>Schema15</code> is the encapsulation of the <i>DataNav</i> figure model XML schema version 15. It extends 
 * {@link Schema14} and includes support for migrating schema version 14 documents to version 15.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul>
 *    <li>(03feb2014) Introduced completely new <i>FypML</i> element: The <i>textbox</i> element lays out a text string
 *    on multiple lines to fit the width of a specified bounding box, minus a possibly non-zero margin (the same on all
 *    sides of the bounding box. The outer bounding box may be filled with a background color and stroked IAW the node's
 *    stroke-related properties, and the text may be clipped to the inner "text box" (bounding box less margins). All
 *    text is rendered in the same font and text color. The text is persisted in the text content of the XML element,
 *    even though it is encapsulated by the <i>title</i> property in the corresponding graphic model node. Both figure
 *    and graph nodes can serve as text box containers. No changes required during schema migration, since this is a 
 *    brand-new element.</li>
 *    <li>(18feb2014) Introduced another new <i>FypML</i> element: The <i>image</i> element renders an image within a
 *    rectangle determined by the element's bounding box, an inner margin, and the source image's aspect ratio (image AR 
 *    is always preserved). The outer bounding box is filled with a background color and stroked IAW the node's stroke-
 *    related properties. Both <i>figure</i> and <i>graph</i> nodes can contain <i>image</i> nodes. The actual image is
 *    stored in PNG format in the element's text content as a base64-encoded byte array. NOTE that this breaks the 
 *    readability of the <i>FypML</i> document as a whole, since the image data could be quite large.</li>
 *    <li>(17mar2014) Added new optional attribute to <i>image</i> element: <i>crop="ulx uly w h"</i>. Its value is an
 *    array of four integers specifying the upper-left corner (ulx, uly) and dimensions (w, h) of a cropping rectangle 
 *    (in image space pixels) to be applied to the original source image. The image region within the cropping rectangle
 *    is scaled to fit the element's specified bounding box, while preserving the aspect ratio (w/h) of that rectangle. 
 *    If implicit, the image is not cropped. Thus, there is no need to increment the schema version to accommodate this 
 *    change, and no changes are required during schema migration.</li>
 * </ul>
 * </p>
 * 
 * @author 	sruffner
 */
class Schema15 extends Schema14
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema15"); }
   @Override public int getVersion() { return(15); }
   @Override public String getLastApplicationVersion() { return("4.5.1"); }

   /** 
    * The "textbox" element, introduced in schema version 15. Essentially a multi-line text label laid out to fit
    * within a specified bounding box (to the extent possible, honoring the box's width at least).
    */
   final static String EL_TEXTBOX = "textbox";
   /** The "image" element, introduced in schema version 15. */
   final static String EL_IMAGE = "image";
   /** 
    * New attribute identifies the background color for the "textbox" or "image" element. Value is a six-digit 
    * hexadecimal string representing the RGB color: "RRGGBB".
    */
   final static String A_BKG = "bkg";
   /** Default for the "bkg" attribute of a "textbox" or "image" element. */
   final static String DEFAULT_TEXTBOX_BKG = "FFFFFF";
   /** Default for the "align" attribute of the "textbox" element. */
   final static String DEFAULT_TEXTBOX_ALIGN = Schema0Constants.HALIGN_CENTER;
   /** Default for the "valign" attribute of the "textbox" element. */
   final static String DEFAULT_TEXTBOX_VALIGN = Schema0Constants.VALIGN_CENTER;
   /** Default for the "gap" attribute of a "textbox" or "image" element, representing the inner margin. */
   final static String DEFAULT_TEXTBOX_GAP = "0in";

   /** New attribute defines the crop rectangle for the "image" element. If implicit, the "image" is not cropped. */
   final static String A_CROP = "crop";
   
   /**
	 * This element map contains {@link SchemaElementInfo SchemaElementInfo} objects for each element that
	 * is new to this schema or has a different attribute set compared to the previous schema.
	 */
	private static final Map<String, SchemaElementInfo> elementMap15;

	static
	{
		elementMap15 = new HashMap<>();
		
      // (03feb2014) New "textbox" element. The text appearing in the text box is stored as the text content of this
      // element. No children allowed.
      elementMap15.put( EL_TEXTBOX, new SchemaElementInfo( true, 
            new String[] {}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_CLIP, A_GAP, A_BKG, 
                          A_HALIGN, A_VALIGN, A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));
      
      // (18feb2014) New "image" element. The image data is stored in the text content of this element as a base64-
      // encoded binary (PNG format). No children allowed.
      // (17mar2014) Added optional "crop" attribute.
      elementMap15.put( EL_IMAGE, new SchemaElementInfo( true, 
            new String[] {}, 
            new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_GAP, A_BKG, 
                          A_CROP, A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));
      
      // (06feb2014) Graph and figure elements revised to accept text box and image nodes as children.
      elementMap15.put( EL_GRAPH, new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_ZAXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE,
                          EL_IMAGE, EL_GRAPH, EL_FUNCTION, EL_TRACE, EL_RASTER, EL_HEATMAP}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                          A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, A_CLIP, A_AUTORANGE,
                          A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));
      
      elementMap15.put( EL_FIGURE, new SchemaElementInfo( false, 
            new String[] {EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE, EL_IMAGE, EL_GRAPH, EL_REF}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                          A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_BORDER, A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                          A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT} ));

	}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in this schema version; else defers 
    * to the superclass implementation.
    */
   @Override public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap15.containsKey(elTag) || super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in this schema version; 
	 * for all other element classes, it defers to the superclass implementation. 
	 */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = elementMap15.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
    *    <li>The <i>loc</i> attribute of a <i>textbox</i> or <i>image</i> element is a measured point that allows any of
    *    the supported units, while the <i>width</i> and <i>height</i> attributes are measured values that allow any 
    *    units other than "user" units.</li>
    *    <li>The new <i>bkg</i> attribute of a <i>textbox</i> or <i>image</i> element must be a 6-digit hexadecimal 
    *    string representing the background color of the elemetn in packed-RGB format.</li>
    *    <li>The new <i>crop</i> attribute of an <i>image</i> element must be a list of exactly 4 whitespace-separated
    *    integer tokens -- "ulx uly w h" -- such that ulx &ge; 0, uly &ge; 0, w &gt; 0, and h &gt; 0.</li>
    *    <li>All other attributes of the <i>textbox</i> and <i>image</i> elements are already handled by the super 
    *    class implementation.</li>
	 * </ul>
	 */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      if(e.getTag().equals(EL_IMAGE) && A_CROP.equals(attr))
      {
         StringTokenizer st = new StringTokenizer(value);
         if(st.countTokens() != 4) return(false);
         
         boolean ok = false;
         try
         {
            int ulx = Integer.parseInt(st.nextToken());
            int uly = Integer.parseInt(st.nextToken());
            int w = Integer.parseInt(st.nextToken());
            int h = Integer.parseInt(st.nextToken());
            
            ok = (0 <= ulx) && (0 <= uly) && (0 < w) && (0 < h);
         }
         catch(NumberFormatException ignored) {}
         
         return(ok);
      }
      else if(e.getTag().equals(EL_TEXTBOX) || e.getTag().equals(EL_IMAGE))
      {
         if(A_LOC.equals(attr))
            return(isValidMeasurePointAttributeValue(value, true, true));
         else if(A_WIDTH.equals(attr) || A_HEIGHT.equals(attr))
            return(isValidMeasureAttributeValue(value, false, true, false));
         else if(A_BKG.equals(attr))
            return(isValidColorAttributeValue(value));
      }
      return(super.isValidAttributeValue(e, attr, value));
	}

   /**
	 * This method handles the actual details of migrating <code>Schema14</code> content to <code>Schema15</code>. It 
    * makes the following changes:
	 * <ul>
    *    <li><i>Currently, no changes are needed. However, since we're migrating "in place", we have to replace all
    *    references to the previous schema with the current schema!</i> </li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
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
