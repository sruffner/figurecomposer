package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;


/**
 * <code>Schema18</code> is the encapsulation of the <i>DataNav</i> figure model XML schema version 18. It extends 
 * {@link Schema17} and includes support for migrating schema version 17 documents to version 18.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul>
 *    <li>(12feb2015, app V4.6.2) Introduced a new data set format, <i>xyzset</i>, representing a set of points (X,Y,Z)
 *    in 3D space.
 *    <li>(12feb2015, app V4.6.2) Introduced a new type of data presentation node, the <i>scatter</i> element, which
 *    represents a scatter plot backed by a 2D <i>ptset</i> or the new 3D <i>xyzset</i> data source. When the data 
 *    source is 2D, a typical X-Y scatter plot is rendered, with a marker symbol drawn at each well-defined point. If
 *    the data source is 3D, then it may be rendered as a "bubble" plot, in which the size and/or fill color of each
 *    marker symbol can vary with the Z-coordinate value. No element nor text content.</li>
 *    <li>(20feb2015, app V4.6.2) Added two additional colormap lookup table options for <i>cmap</i> attribute of a 
 *    <i>zaxis</i> element: "tropic" and "reversetropic".</li>
 *    <li>(09mar2015, app V4.6.2) Added optional attribute <i>bkg</i> to the root <i>figure</i>, specifying a background
 *    fill color for the entire figure. Default == "none", meaning no background fill. Since FypML never supported a
 *    background fill, no changes are needed during schema migration.</li>
 *    <li>(09mar2015, app V4.6.2) Added optional attribute <i>id</i> to <i>label</i>, <i>textbox</i> and <i>graph</i>
 *    elements. Default = "". When not an empty string, it serves to uniquely identify that graphic object within the
 *    model. Note that <i>set</i> elements are NOT considered graphic objects, so they're not included in the 
 *    uniqueness check; in fact, no two data sets can have the same <i>id</i>. Unlike a data set ID, a graphic object
 *    ID can be any length and can contain any Unicode character supported in <i>FypML</i>. The graphic node ID was
 *    introduced to support programmatic modification of an existing <i>FypML</i> figure in ways other than injecting
 *    data sets.</li>
 * </ul>
 * </p>
 * 
 * @author 	sruffner
 */
class Schema18 extends Schema17
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema18"); }
   @Override public int getVersion() { return(18); }
   @Override public String getLastApplicationVersion() { return("4.6.2"); }

   /** New data presentation element -- a scatter or bubble plot. */
   final static String EL_SCATTER = "scatter";
   /** A display mode for the "scatter" element: typical X-Y scatter plot; all symbols are same size and color. */
   final static String MODE_SCATTER_XY = "scatter";
   /** A display mode for the "scatter" element: bubble plot where symbol size proportional to Z-coordinate. */
   final static String MODE_SCATTER_SIZE = "sizeBubble";
   /** A display mode for the "scatter" element: bubble plot in which symbol color varies with Z-coordinate. */
   final static String MODE_SCATTER_COLOR = "colorBubble";
   /** A display mode for the "scatter" element: bubble plot in which both symbol size and color vary with Z. */
   final static String MODE_SCATTER_COLORSZ = "colorSizeBubble";
   /** The set of choices for the "scatter" element's display mode (the "mode" attribute). */
   final static String[] MODE_SCATTER_CHOICES = {
      MODE_SCATTER_XY, MODE_SCATTER_SIZE, MODE_SCATTER_COLOR, MODE_SCATTER_COLORSZ
   };
   /** Default for the "mode" attribute of the "scatter" element. */
   final static String DEFAULT_SCATTER_MODE = MODE_SCATTER_XY;
   /** Default for the "size" attribute of the "scatter" element (maximum symbol size). */
   final static String DEFAULT_SCATTER_SIZE= "0.2in";
   /** Default for the "type" attribute of the "scatter" element (marker symbol type). */
   final static String DEFAULT_SCATTER_TYPE = "circle";
   
   /** New data format introduced in schema 18: A list of 3D points. Rendered only by the new "scatter" element. */
   final static String SETFMT_XYZSET = "xyzset";
   
   /** Choices for a "set" element's "fmt" attribute, including the new format introduced in schema version 18. */
   final static String[] SETFMT_CHOICES_V18 = {
      SETFMT_PTS_V7, SETFMT_SERIES_V7, SETFMT_MSET_V7, SETFMT_MSERIES_V7, SETFMT_RASTER1D, SETFMT_XYZIMG, SETFMT_XYZSET
   };

   /** Colormap choice for <i>cmap</i> attribute: indigo to dark blue to blue to green to orange to yellow. */
   final static String CMAP_TROPIC = "tropic";
   /** Colormap choice for <i>cmap</i> attribute: reverse of "tropic" colormap. */
   final static String CMAP_REVERSETROPIC = "reversetropic";
   
   /** Revised (as of 20feb2015) set of choices for the <i>cmap</i> attribute of <i>zaxis</i>. */
   final static String[] CMAP_CHOICES_V18 = {
      CMAP_GRAY, CMAP_REVERSEGRAY, CMAP_HOT, CMAP_REVERSEHOT, CMAP_AUTUMN, CMAP_REVERSEAUTUMN, 
      CMAP_JET, CMAP_REVERSEJET, CMAP_TROPIC, CMAP_REVERSETROPIC
   };

   /** Default for the "bkg" attribute of the "figure" element. */
   final static String DEFAULT_FIGURE_BKG = ATTRVAL_NONE;
   
   /** 
    * Default for the "id" attribute of selected elements: text labels, text boxes, graphs. Data sets are distinct from 
    * graphic objects in a figure, so data set IDs are not compared against graphic object IDs.
    */
   final static String DEFAULT_OBJ_ID = "";
   
   /**
	 * This element map contains {@link SchemaElementInfo SchemaElementInfo} objects for each element that
	 * is new to this schema or has a different attribute set compared to the previous schema.
	 */
	private static final Map<String, SchemaElementInfo> elementMap18;

	static
	{
		elementMap18 = new HashMap<>();
		
      // (12feb2015) A new data presentation element, the scatter/bubble plot.
      elementMap18.put( EL_SCATTER, new SchemaElementInfo( false, 
            new String[] {}, 
            new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_FILLCOLOR, A_TITLE, 
                          A_LEGEND, A_MODE, A_TYPE, A_SIZE, A_SRC}, 
            new String[] {A_SRC} ));
      
      // (13feb2015) Graph element revised to accept scatter plot node as a child.
      // (09mar2015) Added optional "id" attribute to the graph element.
      elementMap18.put( EL_GRAPH, new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_ZAXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE,
                          EL_IMAGE, EL_GRAPH, EL_FUNCTION, EL_TRACE, EL_RASTER, EL_HEATMAP, EL_BAR, EL_SCATTER}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, 
                          A_CLIP, A_AUTORANGE, A_ID,
                          A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

      // (09mar2015) Figure element now takes optional 'bkg' attribute setting a background fill color. Default="none".
      elementMap18.put( EL_FIGURE, new SchemaElementInfo( false, 
            new String[] {EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE, EL_IMAGE, EL_GRAPH, EL_REF}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                          A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT, A_TITLE, A_BORDER, A_BKG}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                          A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT} ));

      // (09mar2015) Added optional attribute "id" to the "textbox" and "label" elements.
      elementMap18.put( EL_TEXTBOX, new SchemaElementInfo( true, 
            new String[] {}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_CLIP, A_GAP, A_BKG, 
                          A_HALIGN, A_VALIGN, A_LINEHT, A_ID, A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));
      
      elementMap18.put( EL_LABEL, 
            new SchemaElementInfo( false, 
               new String[] {}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, 
                             A_ROTATE, A_HALIGN, A_VALIGN, A_ID, A_LOC, A_TITLE}, 
               new String[] {A_LOC, A_TITLE} ));
	}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in this schema version; else defers 
    * to the superclass implementation.
    */
   @Override public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap18.containsKey(elTag) || super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in this schema version; 
	 * for all other element classes, it defers to the superclass implementation. 
	 */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = elementMap18.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
    *    <li>The <i>mode</i> attribute of the new <i>scatter</i> element must be one of the four possible choices listed
    *    in {@link #MODE_SCATTER_CHOICES}.</li>
    *    <li>The <i>type</i> attribute of the new <i>scatter</i> element must be one of the currently supported marker
    *    symbol types in {@link #ADORN9_CHOICES}.</li>
    *    <li>The <i>fmt</i> attribute of the <i>set</i> element is an enumerated attribute value that now allows one
    *    additional value -- for the new "xyzset" format.</li>
    *    <li>The "cmap" enumerated attribute admits two additional choices as of 20feb2015. The full set of choices are
    *    in {@link #CMAP_CHOICES_V18}.</li>
    *    <li>The <i>bkg</i> attribute of the <i>figure</i> element is an RGB color, or the value "none" (meaning no
    *    background fill).</li>
    *    <li>The <i>id</i> attribute on a <i>label</i>, <i>textbox</i>, or <i>graph</i> can be any string token, as long
    *    as it is not null.</li>
	 * </ul>
	 */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      String tag = e.getTag();
      
      if(A_CMAP.equals(attr)) return(isValidEnumAttributeValue(value, CMAP_CHOICES_V18));
      if(tag.equals(EL_SCATTER))
	   {
         if(A_MODE.equals(attr)) return(isValidEnumAttributeValue(value, MODE_SCATTER_CHOICES));
         if(A_TYPE.equals(attr)) return(isValidEnumAttributeValue(value, ADORN9_CHOICES));
	   }
      if(tag.equals(EL_SET) && A_FMT.equals(attr))
         return(isValidEnumAttributeValue(value, SETFMT_CHOICES_V18));
      if(tag.equals(EL_FIGURE) && A_BKG.equals(attr))
         return(ATTRVAL_NONE.equals(value) || isValidColorAttributeValue(value));
      
      if(A_ID.equals(attr) && (tag.equals(EL_GRAPH) || tag.equals(EL_LABEL) || tag.equals(EL_TEXTBOX)))
         return(value != null);
      
      return(super.isValidAttributeValue(e, attr, value));
	}

   /**
    * This method handles the actual details of migrating from the previous schema version to this one. It makes the
    * following changes:
    * <ul>
    *    <li><i>No changes are needed. However, since we're migrating "in place", we have to replace all references to 
    *    the previous schema with the current schema!</i> </li>
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
