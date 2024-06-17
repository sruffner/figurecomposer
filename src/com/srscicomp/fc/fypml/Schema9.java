package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema9</code> is the encapsulation of the <em>DataNav</em> figure model XML schema version 9. It extends 
 * <code>Schema8</code> and includes support for migrating schema version 8 documents to version 9. It includes those
 * schema changes that were required to support the concept of a "Z color axis" -- a special graph axis related to 
 * heatmaps that was introduced in <i>DataNav</i> v3.1.0. This axis's color-mapping properties (colormap LUT, mapped 
 * data range, and mapping mode) determine how the Z(x,y) dataset in a "heatmap" element is converted to an image that
 * is rendered in the parent graph.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul>
 *    <li>(30 Oct 2008) Additional attributes were added to the heatmap element to give the user more control over how 
 *    each image datum is mapped to an entry in the colormap. The new "range" attribute specifies the minimum and 
 *    maximum values of the image data range that maps onto the entries [0..N-1] in the N-length colormap array. It is 
 *    either null or a list of two float values, [R0..R1], where R0 &lt; R1. A null value means that the entire 
 *    <b>observed</b> range in the image data is mapped onto the color LUT. Otherwise, the specified range is used; any
 *    datum &le; R0 is mapped to the first entry in the LUT, while any datum &ge; R1 is mapped to the last entry. The
 *    new "mode" attribute specifies the mapping mode, the original "linear" mapping or new "log" (log10) mapping.
 *    <b><i>Both attributes are optional and have default values ("null" and "linear", respectively) that correspond to 
 *    the original manner in which image data was mapped to the color LUT, so no changes are needed to migrate a schema 
 *    8 document!</i></b>.</li>
 *    <li>(31 Oct 2008) In <code>Schema7</code>, the "axis" element was modified so that it no longer restricted the 
 *    number of tick set children it could contain. However, that schema version still enforced the upper limit of 4 
 *    tick sets, established in schema version 0. <code>Schema9</code> relaxes the restriction.</li>
 *    <li>(06 Nov 2008) Added new "zaxis" element. It is a <b>required</b> element of a "graph" and is located after
 *    the "axis" element representing the graph's secondary (Y) axis. Its attribute content is similar to the original
 *    "axis" element, but it lacks a "units" attribute and includes additional attributes describing the color-mapping
 *    properties associated with the Z axis. The "cmap" attribute selects the colormap LUT, as was the case for the
 *    original "heatmap" element introduced in <code>Schema8</code>. The required "start" and "end" attributes define
 *    the nominal Z-axis range in user units. This data range is mapped linearly or logarithmically onto the set of 
 *    integers [0..L-1], where L is the length of the colormap LUT array; the mapping mode is set by the new "cmode"
 *    attribute. Other attributes, including the new "edge" attribute, determine how the axis itself is rendered.</li>
 *    <li>(06 Nov 2008) Removed attribute "cmap" from the "heatmap" element. All heatmaps in a graph are now rendered
 *    IAW the data-to-color mapping properties defined on the new "zaxis". During migration, a default "zaxis" element
 *    is added to the parent graph, and its "cmap" attribute is set to the value of the "cmap" attribute on the first
 *    <code>Schema8</code> "heatmap" node found in the parent graph, if any. All other attributes are left intrinsic.
 *    <b><i>Ideally, the "start" and "end" attributes of the "zaxis" should be initialized to to encompass the entire 
 *    data range observed across all heatmaps in the graph. This was deemed too time-consuming, since it would require
 *    parsing all datasets during schema migration (that parsing occurs after the schema has been processed). As a
 *    result, the user will probably have to adjust the Z axis range after reading in the model!</i></b></li>
 *    <li>(27 Feb 2009) Added four new endcap adornments: "dart", "kite", "reversedart", and "reversekite". Since we
 *    merely added additional adornment types, existing documents are unaffected, so the schema version was not 
 *    incremented.</li>
 * </ul>
 * </p>
 * 
 * @author 	sruffner
 */
class Schema9 extends Schema8
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema9"); }
   @Override public int getVersion() { return(9); }
   @Override public String getLastApplicationVersion() { return("3.1.7"); }

   /** The new "zaxis" element represents the graph axis corresponding to the third, or Z, dimension. */
   final static String EL_ZAXIS = "zaxis";
   
   /** Enumerated "cmode" attr specifies the data-to-color index mapping mode for the "zaxis" element. */
   final static String A_CMODE = "cmode";

   /** Data-to-color mapping mode choice for the "zaxis" element: linear. */
   final static String CMODE_LIN = "linear";
   /** Data-to-color mapping mode choice for the "zaxis" element: log (base-10 logarithm). */
   final static String CMODE_LOG = "log";
   
   /** The set of choices for the "zaxis" element's data-to-color mapping mode (the "cmode" attribute). */
   final static String[] CMODE_CHOICES = { CMODE_LIN, CMODE_LOG };

   /** Default for the "cmode" attribute of the "zaxis" element. */
   final static String DEFAULT_ZAXIS_CMODE = CMODE_LIN;

   /** Enumerated "edge" attr specifies the graph box egde beside which the "zaxis" element is rendered. */
   final static String A_EDGE = "edge";

   /** Edge location choice for the "zaxis" element: left. */
   final static String EDGE_LEFT = "left";
   /** Edge location choice for the "zaxis" element: right. */
   final static String EDGE_RIGHT = "right";
   /** Edge location choice for the "zaxis" element: top. */
   final static String EDGE_TOP = "top";
   /** Edge location choice for the "zaxis" element: bottom. */
   final static String EDGE_BOTTOM = "bottom";
   
   /** The set of choices for the "zaxis" element's edge location (the "edge" attribute). */
   final static String[] EDGE_CHOICES = { EDGE_LEFT, EDGE_RIGHT, EDGE_TOP, EDGE_BOTTOM };

   /** Default for the "edge" attribute of the "zaxis" element. */
   final static String DEFAULT_EDGE = EDGE_RIGHT;
   
   /** Default for the "hide" attribute of the "zaxis" element. */
   final static String DEFAULT_ZAXIS_HIDE = "true";
   /** Default for the "gap" attribute (graph-gradient bar separation) of the "zaxis" element. */
   final static String DEFAULT_ZAXIS_GAP = "0.05in";
   /** Default for the "size" attribute (gradient bar width) of the "zaxis" element. */
   final static String DEFAULT_ZAXIS_SIZE = "0.2in";
   /** Default for the "cmap" attribute (colormap lookup table choice) of the "zaxis" element. */
   final static String DEFAULT_ZAXIS_CMAP = CMAP_GRAY;
   
   /**
    * The names of all adornments supported in schema version 9, as of 02/27/09. Same as the list defined in schema 7,
    * with four additions.
    */
   final static String ADORN_DART = "dart";
   final static String ADORN_REV_DART = "reversedart";
   final static String ADORN_KITE = "kite";
   final static String ADORN_REV_KITE = "reversekite";
   final static String[] ADORN9_CHOICES;
   static
   {
      int n = ADORN7_CHOICES.length;
      ADORN9_CHOICES = new String[n+4];
      System.arraycopy(ADORN7_CHOICES, 0, ADORN9_CHOICES, 0, n);
      ADORN9_CHOICES[n] = ADORN_DART;
      ADORN9_CHOICES[n+1] = ADORN_REV_DART;
      ADORN9_CHOICES[n+2] = ADORN_KITE;
      ADORN9_CHOICES[n+3] = ADORN_REV_KITE;
      
   }


   /**
	 * This element map contains <code>SchemaElementInfo</code> objects for each element that is new to this schema or 
    * has a different attribute set compared to the previous schema.
	 */
	private static final Map<String, SchemaElementInfo> elementMap9;

	static
	{
		elementMap9 = new HashMap<>();
		
      // The new "zaxis" element is always the child of a graph.
      elementMap9.put( EL_GRAPH, 
         new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_ZAXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH, 
                          EL_FUNCTION, EL_TRACE, EL_RASTER, EL_HEATMAP}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, A_CLIP, 
                          A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

      // The new "zaxis" element.
      elementMap9.put( EL_ZAXIS, 
            new SchemaElementInfo( false, 
               new String[] {EL_TICKS}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                             A_STROKECOLOR, A_HIDE, A_TITLE, A_SPACER, A_LABELOFFSET, A_START, A_END,
                             A_GAP, A_SIZE, A_EDGE, A_CMAP, A_CMODE}, 
               new String[] {A_START, A_END} ));

      // The "heatmap" element no longer possesses the "cmap" attribute.
      elementMap9.put( EL_HEATMAP, 
            new SchemaElementInfo( false, 
                  new String[] {}, 
                  new String[] {A_TITLE, A_SRC}, 
                  new String[] {A_SRC} ));
      
	}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in schema version 9; otherwise defers 
    * to the superclass implementation.
    * @see ISchema#isSupportedElementTag(String)
    */
   @Override
   public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap9.containsKey(elTag) || super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in schema version 9; for 
	 * all other element classes, it defers to the superclass implementation. 
	 * @see ISchema#getSchemaElementInfo(String)
	 */
   @Override
	public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = elementMap9.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

   /**
    * This method enforces any requirements on the children content of elements that were introduced or revised in 
    * schema version 9. For all other element classes, it defers to the superclass implementation.
    * <ul>
    *    <li>With the addition of the "zaxis" element, the "graph" element now requires six children in the following
    *    order: axis, axis, zaxis, gridline, gridline, legend.</li>
    *    <li>The new "zaxis" element has no required children.</li>
    * </ul>
    * @see ISchema#hasRequiredChildren(BasicSchemaElement)
    */
   @SuppressWarnings("rawtypes")
   @Override
   public boolean hasRequiredChildren(BasicSchemaElement e)
   {
      String elTag = e.getTag();
      List children = e.getElementContent();
      if(EL_GRAPH.equals(elTag))
      {
         if(children.size() < 6) return(false);
         boolean ok = true;
         for(int i=0; ok && i<6; i++) 
         {
            ok = isValidChildAtIndex(e, ((BasicSchemaElement) children.get(i)).getTag(), i);
         }
         return(ok);
      }
      else if(EL_ZAXIS.equals(elTag))
         return(true);
      return(super.hasRequiredChildren(e));
   }

   /**
    * Overridden to enforce any structural restrictions on the children content of elements that were introduced or 
    * revised in schema version 9. For all other element classes, it defers to the superclass implementation.
    * <ul>
    *    <li>With the addition of the "zaxis" element, the "graph" element now requires six children in the following
    *    order: axis, axis, zaxis, gridline, gridline, legend.</li>
    *    <li>The "axis" element does not restrict the number of "tick set" children it contains. This change was 
    *    actually made in version 7, but was never enforced in schema versioning until now.</li>
    * </ul>
    * @see ISchema#isValidChildAtIndex(BasicSchemaElement,String,int)
    */
   @Override
   public boolean isValidChildAtIndex(BasicSchemaElement e, String childTag, int index)
   {
      String elTag = e.getTag();
      if(EL_GRAPH.equals(elTag))
      {
         SchemaElementInfo eInfo = getSchemaElementInfo(elTag);
         if(!eInfo.isChildAllowed(childTag)) return(false);
         if(EL_AXIS.equals(childTag)) return(index == 0 || index == 1);
         else if(EL_ZAXIS.equals(childTag)) return(index == 2);
         else if(EL_GRIDLINE.equals(childTag)) return(index == 3 || index == 4);
         else if(EL_LEGEND.equals(childTag)) return(index == 5);
         else return(index > 5);
      }
      else if(EL_AXIS.equals(elTag))
      {
         SchemaElementInfo eInfo = getSchemaElementInfo(elTag);
         return(eInfo.isChildAllowed(childTag));
      }
      return super.isValidChildAtIndex(e, childTag, index);
   }
   
	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
	 *    <li>For the new "zaxis" element, this method validates the following attributes: "edge" must be one of 
	 *    <code>EDGE_CHOICES</code>; "cmap" must be one of <code>CMAP_CHOICES</code>; and "cmode" must be one of 
	 *    <code>CMODE_CHOICES</code>. All other "zaxis" attributes are existing element attributes that are validated
	 *    correctly by the super class implementation.</li>
	 *    <li>(As of 2/27/2009) Added four new endcap adornments. Validation of the relevant properties ("cap", "type" 
	 *    on "shape" and "symbol" elements) was updated accordingly.</li>
	 * </ul>
	 * @see ISchema#isValidAttributeValue(BasicSchemaElement,String,String)
	 */
   @Override
	public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      String elTag = e.getTag();
      if(EL_ZAXIS.equals(elTag))
      {
         if(A_EDGE.equals(attr))
            return(isValidEnumAttributeValue(value, EDGE_CHOICES));
         else if(A_CMODE.equals(attr))
            return(isValidEnumAttributeValue(value, CMODE_CHOICES));
         else if(A_CMAP.equals(attr))
            return(isValidEnumAttributeValue(value, CMAP_CHOICES));
      }
      else if(A_CAP.equals(attr) || (A_TYPE.equals(attr) && (EL_SHAPE.equals(elTag) || EL_SYMBOL.equals(elTag))))
         return(isValidEnumAttributeValue(value, ADORN9_CHOICES));
      
      return(super.isValidAttributeValue(e, attr, value));
	}
   
   
   /**
	 * This method handles the actual details of migrating <code>Schema8</code> content to <code>Schema9</code>. It 
    * makes the following changes:
	 * <ul>
    *    <li>Removes the "cmap" attribute from all "heatmap" elements encountered. Revises the schema element info for
    *    all such "heatmap" elements.</li>
    *    <li>For each "graph" encountered, inserts a default "zaxis" as the third child element and revises the schema
    *    element info for the "graph". If the graph contains any "heatmap" elements, the value of the old "cmap" 
    *    attribute on the first "heatmap" encountered becomes the value of that attribute on the graph's "zaxis" child. 
    *    This is not a perfect migration for a couple reasons. (1) The new schema does not support heatmaps with 
    *    different colormaps in the same graph; now it's the Z axis that controls the color scheme for ALL heatmaps 
    *    within a graph. (2) In the case of a single heatmap in a graph, perfect migration would require setting the
    *    Z axis range to match the observed data range of the heatmap's dataset. This would require parsing that 
    *    dataset, which actually occurs after schema migration. It is not done, so it is likely the user will have to
    *    tweak some things after migration!</li>
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
         
         // for heatmaps, remove the old "cmap" attribute
         if(EL_HEATMAP.equals(elTag))
            e.removeAttributeByName(A_CMAP);
         
         // migrate the element object's schema info.
         e.updateSchema(this, null);

         // after updating schema element info, insert the required Z axis as the third child in each graph encountered.
         // Set its "cmap" attribute to that of the first "heatmap" child in the graph (those children will not have 
         // been migrated yet).
         if(EL_GRAPH.equals(elTag))
         {
            BasicSchemaElement zaxis = (BasicSchemaElement) createElement(EL_ZAXIS);
            zaxis.setAttributeValueByName(A_START, "0");
            zaxis.setAttributeValueByName(A_END, "100");
            BasicSchemaElement ticks = (BasicSchemaElement) createElement(EL_TICKS);
            ticks.setAttributeValueByName(A_INTV, "50");
            zaxis.add(ticks);
            e.insert(zaxis, 2);

            // if "cmap" is explicitly set on FIRST heatmap in graph, apply it to the new Z axis.
            for(int i=0; i<e.getChildCount(); i++)
            {
               BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
               if(EL_HEATMAP.equals(child.getTag()))
               {
                  String cmap = child.getAttributeValueByName(A_CMAP);
                  if(cmap != null) zaxis.setAttributeValueByName(A_CMAP, cmap);
                  break;
               }
            }
         }
         
         // if element has any children, push them onto the stack so that we check them as well! But don't include any
         // "zaxis" encountered, since we just added it and we know if conforms to the current schema!
         for(int i=0; i<e.getChildCount(); i++)
         {
            BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
            if(EL_GRAPH.equals(elTag) && EL_ZAXIS.equals(child.getTag())) continue;
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
