package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;


/**
 * <code>Schema20</code> is the encapsulation of the <i>DataNav</i> figure model XML schema version 20. It extends 
 * {@link Schema19} and includes support for migrating schema version 19 documents to version 20.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul> 
 *    <li>(10jul2015, app V4.7.2) Extended <i>bkg</i> attribute on <i>textbox</i> to represent any of three types of
 *    background fills: a solid color fill as before; a two-color axial gradient; and a two-color radial gradient.
 *    The gradients are non-cyclic and by design span the object's bounding box. The attribute value takes on one of
 *    three forms depending on the fill type: "00ff00" (or "none") for a solid color fill; "0 ff00ff aa2200" for an
 *    axial gradient (first token must be an int in [0..359] representing the gradient orientation in degrees WRT the
 *    bounding box); and "50 50 ffff00 00bb38" for a radial gradient (first two tokens are integers in [0..100] that
 *    specify the gradient's focal point within the bounding box in percentage units). Values are validated without
 *    introducing package dependencies on the model object they represent.</li>
 *    <li>(15jul2015, app V4.7.2) Analogous redefinition of the <i>bkg</i> attribute on <i>image</i> element.</li>
 *    <li>(16jul2015, app V4.7.2) Analogous redefinition of the <i>bkg</i> attribute on <i>figure</i> node.</li>
 *    <li>(17jul2015, app V4.7.2) Added <i>bkg</i> attribute to <i>shape</i> node to allow gradient fills of shapes. The
 *    text/fill color is no longer used to specify the shape's background fill; instead, it specifies the text color of
 *    the auto-generated label (prior to this change, the stroke color was used to paint the label). During schema 
 *    migration, the <i>bkg</i> attribute is set to the value of the text/fill color attribute which, in turn, is set to
 *    the value of the stroke color attribute. This ensures that the shape's appearance is unaltered.</li>
 *    <li>(23jul2015, app V4.7.2) Added new display mode, "staircase", to the trace node.</li>
 *    <li>(04aug2015, app V4.7.2) Replaced the "size" attribute on the shape node with "width" and "height" attributes
 *    so that shapes can be resized independently in X and Y. During schema migration, the "width" and "height"
 *    attributes are set to the value of the old "size" attribute to ensure the shape's appearance is unchanged.</li>
 * </ul>
 * </p>
 * 
 * @author 	sruffner
 */
class Schema20 extends Schema19
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema20"); }
   @Override public int getVersion() { return(20); }
   @Override public String getLastApplicationVersion() { return("4.7.3"); }

   /** Default for the "bkg" attribute of a "shape" element. */
   final static String DEFAULT_SHAPE_BKG = "000000";
   
   /**
    * New display mode for the "trace" element: Similar to "polyline" mode, except rendered as a sample-and-hold
    * staircase sequence.
    */
   final static String MODE_TRACE_STAIR = "staircase";
   
   /** Revised set of choices for the "trace" element's display mode (the "mode" attribute). */
   final static String[] MODE_TRACE_CHOICES_V20 = {
      DISPMODE_PTS, MODE_TRACE_STAIR, DISPMODE_BAND, DISPMODE_HIST, DISPMODE_MULTI};

   /** Default for the "width" and "height" attributes of a "shape" element. */
   final static String DEFAULT_SHAPE_DIM = Schema7.DEFAULT_SYMBOL_SIZE;
 
   /**
	 * This element map contains {@link SchemaElementInfo SchemaElementInfo} objects for each element that
	 * is new to this schema or has a different attribute set compared to the previous schema.
	 */
	private static final Map<String, SchemaElementInfo> elementMap20;

	static
	{
		elementMap20 = new HashMap<>();
		
		// 17jul2015: Added 'bkg' attribute.
		// 05aug2015: The 'size' attribute replaced by 'width' and 'height'. Both optional, with default="0.1in".
      elementMap20.put( EL_SHAPE, new SchemaElementInfo( false, 
            new String[] {EL_LABEL}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKECOLOR, 
                          A_STROKEWIDTH, A_STROKECAP,  A_STROKEJOIN, A_STROKEPAT, A_LOC, A_ROTATE, A_TYPE, 
                          A_WIDTH, A_HEIGHT, A_TITLE, A_BKG}, 
            new String[] {A_LOC} ));
	}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in this schema version; else defers 
    * to the superclass implementation.
    */
   @Override public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap20.containsKey(elTag) || super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in this schema version; 
	 * for all other element classes, it defers to the superclass implementation. 
	 */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = elementMap20.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
    *    <li>The <i>bkg</i> attribute of the <i>textbox, image, and figure</i> nodes now defines any of 3 types of 
    *    background fills.</li>
    *    <li>The <i>shape</i> node now has a <i>bkg</i> attribute to support gradient fills.</li> 
    *    <li>The <i>trace</i> node now supports a "staircase" display mode ("mode" attribute value). The revised list
    *    of possible mode is in {@link #MODE_TRACE_CHOICES_V20}.</li>
    *    <li>The <i>shape</i> node's <i>size</i> attribute was replaced by independent <i>width</i> and <i>height</i>.
    *    Both are measured attributes that must be non-negative and use physical units only.</li>
	 * </ul>
	 */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      if(A_BKG.equals(attr)) return(isValidBkgFillAttributeValue(value));
      if(EL_TRACE.equals(e.getTag()) && A_MODE.equals(attr)) 
         return(isValidEnumAttributeValue(value, MODE_TRACE_CHOICES_V20));
      if(EL_SHAPE.equals(e.getTag()) && (A_WIDTH.equals(attr) || A_HEIGHT.equals(attr)))
         return(this.isValidMeasureAttributeValue(value, false, false));
      
      return(super.isValidAttributeValue(e, attr, value));
	}

   /**
    * As of v4.7.2, the <i>bkg</i> attribute of a text box, image, shape, or figure node supports three distinct types 
    * of background fills: solid color (including transparent), a two-color axial gradient at any orientation WRT the 
    * object's bounding box, and a two-color radial gradient with a focus located anywhere in that bounding box. This
    * method validates the value of that attribute as follows:
    * <ul>
    * <li>The string value must contain 1, 3, or 4 whitespace-separated tokens.</li>
    * <li>If there is a single token, the background fill is a solid uniform color, and the token must be a six-digit
    * hexadecimal string defining the opaque RGB color, or the token "none" for transparent black.</li>
    * <li>If there are three tokens, the fill is a two-color axial gradient. The first token is the gradient angle
    * WRT the graphic object's bounding box, in degrees; it must be an integer string in [0..359]. The remaining two
    * tokens are six-digit hexadecimal strings defining the two RGB colors at the endpoints of the gradient. Neither
    * color token can be "none".</li>
    * <li>If there are four tokens, the fill is a radial gradient. The first two tokens are integer strings "x y", 
    * where both x and y lie in [0..100] and specify the coordinates of the gradient's focal point within the object's
    * bounding box in percentage units. The remaining two tokens are the two RGB colors, as 6-digit hex strings; again,
    * neither can have the value "none".</li>
    * </ul>
    * @param value The value to test.
    * @return True if value conforms to described constraints for a background fill value; else false.
    */
   boolean isValidBkgFillAttributeValue(String value)
   {
      if(value == null) return(false);
      String[] tokens = value.split(" ");
      boolean ok = false;
      if(tokens.length == 1) 
         ok = isValidColorAttributeValue(tokens[0], true);
      else if(tokens.length == 3)
         ok = isValidIntegerAttributeValue(tokens[0], 0, 359) && isValidColorAttributeValue(tokens[1], false) &&
            isValidColorAttributeValue(tokens[1], false);
      else if(tokens.length == 4)
         ok = isValidIntegerAttributeValue(tokens[0], 0, 100) && isValidIntegerAttributeValue(tokens[1], 0, 100) &&
            isValidColorAttributeValue(tokens[2], false) && isValidColorAttributeValue(tokens[3], false);
      return(ok);
   }
   
   /**
    * Verify that the specified value is valid for a color attribute that may or may not permit the value "none" to
    * represent "transparent black" (introduced in schema version 16).
    * @param value Value to test.
    * @param allowTransp If true, then the value may equal "none" to define transparent black (zero alpha).
    * @return True if valid, else false.
    */
   boolean isValidColorAttributeValue(String value, boolean allowTransp)
   {
      boolean ok = allowTransp && ATTRVAL_NONE.equals(value);
      if(!ok) ok = isValidColorAttributeValue(value);
      return(ok);
   }
   
   /**
    * This method handles the actual details of migrating from the previous schema version to this one. It makes the
    * following changes:
    * <ul>
    *    <li>During migration, the bkg attr for a shape is set to the value of its text/fill color attribute which, in 
    *    turn, is explicitly set to the stroke color. Complicating matters is the fact that the text/fill and stroke 
    *    color attribute values may be implicit, so we have to search up the hierarchy!</li>
    *    <li>The width and height of a shape are set to its old size to ensure the shape is unchanged. If the old size
    *    attribute is unspecified, then the width and height are also unspecified -- they will be set to the old default
    *    value for the size attribute (0.1in).</li>
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
         String elTag = e.getTag();
         
         // remember the stroke and text/fill colors assigned to a shape node prior to migrating its schema info. Since
         // these can be inherited, we may have to search up the hierarchy to get the explicit values.
         // In addition, remember the value of the shape's "size" attribute. The new "width" and "height" attrs will be
         // be set to this value after the element's schema info is updated.
         String shapeStrokeC = null;
         String shapeFillC = null;
         String shapeSz = null;
         if(EL_SHAPE.equals(elTag))
         {
            BasicSchemaElement node = e;
            while(!(node.hasAttribute(A_STROKECOLOR) && (node.getAttributeValueByName(A_STROKECOLOR) != null)))
               node = (BasicSchemaElement) node.getParent();
            shapeStrokeC = node.getAttributeValueByName(A_STROKECOLOR);
            
            node = e;
            while(!(node.hasAttribute(A_FILLCOLOR) && (node.getAttributeValueByName(A_FILLCOLOR) != null)))
               node = (BasicSchemaElement) node.getParent();
            shapeFillC = node.getAttributeValueByName(A_FILLCOLOR);
            
            shapeSz = e.getAttributeValueByName(A_SIZE);
            e.removeAttributeByName(A_SIZE);
         }
         
         // migrate the element object's schema info.
         e.updateSchema(this, null);

         // after updating schema info for a shape node, set its new bkg attribute to its text/fill color, and set its
         // text/fill color to the stroke color. Also set its new width and height attributes to the value of its old
         // size attribute, if that was specified. This ensures its appearance remains unchanged after migration.
         if(EL_SHAPE.equals(elTag))
         {
            e.setAttributeValueByName(A_BKG, shapeFillC);
            e.setAttributeValueByName(A_FILLCOLOR, shapeStrokeC);
            if(shapeSz != null)
            {
               e.setAttributeValueByName(A_WIDTH, shapeSz);
               e.setAttributeValueByName(A_HEIGHT, shapeSz);
            }
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
