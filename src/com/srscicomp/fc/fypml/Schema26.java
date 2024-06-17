package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.ISimpleXMLElement;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <b>Schema26</b> is the encapsulation of the <i>FypML</i> figure model XML schema version 26. It extends {@link 
 * Schema25} and includes support for migrating schema version 25 documents to version 26. This version was first 
 * introduced with version 5.4.5 of <i>FigureComposer</i>.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul> 
 *    <li>(01 jun 2023, for app V5.4.5) The box plot now optional renders a "violin plot" illustrating the kernel 
 *    density estimate for the sample set. To control the appearance and visibility of the violin plot, the "box"
 *    element requires a third component element -- the new "violin" element" -- which specifies the fill color,
 *    stroke characteristics and width/height of the violin plot.</li>
 *    <li>(07 jun 2023, for app V5.4.5) Added "trendline" display mode for the "trace" element. Similar to "polyline",
 *    but instead of "connecting the dots", a line segment is drawn through the data points colinear with the least
 *    mean squares regression line.</li>
 *    <li>(07 jun 2023, for app V5.4.5) Added "trendline" display mode for the "scatter" element. Similar to "scatter",
 *    with the addition of a line segment drawn through the data points colinear with the least mean squares regression 
 *    line.</li>
 *    <li>(13 Jun 2023, for app V5.4.5) Added "len" attribute to "trace" element, specifying the window length for
 *    computing the sliding average trace in the "trendline" display mode only. If the window length is 1, the LMS
 *    regression line is drawn instead. Integer attribute restricted to values > 0. No schema migration required.</li>
 *    <li>(21jun2023, for app V5.4.5) The 2D scatter plot now supports a "connect the dots" polyline in the "scatter"
 *    and "bubble" display modes. A single component node -- the new "scatterline" element -- defines the stroke 
 *    properties for the polyline as well as a fill color; if the polyline is drawn, the last point connects back to the
 *    first and the resulting region is filled IAW that fill color. This allows authors to create arbitrary filled 
 *    regions in any 2D graph -- a practical example is a so-called "radar plot". The component also defines how the
 *    LMS regression line is stroked in the "trendline" display mode; the "scatter" element's own draw styles govern 
 *    the appearance of the marker symbols. During migration, the inserted "scatterline" element will have stroke width
 *    explicitly set to 0 -- thereby ensuring that existing figures are not affected by this change.</li>
 *    <li>(26 jun 2023, for app V5.4.5) Modified "area" node to support rendering the label inside or outside the
 *    corresponding area. Added the standard text-related style attributes, plus a new "mode" attribute specifying the 
 *    area label display mode. Defaults to "off", so existing figures are unaffected and no schema migration is
 *    required.</li>
 *    <li>(27 jun 2023, for app V5.4.5) Modified "bar" node to support rendering the legend labels on the bar plot 
 *    itself, as an alternative to listing them in the parent graph's legend. Added the standard text-related styles,
 *    plus the boolean attribute "auto". When set, each legend label is drawn inside or adjacent to one of the bars
 *    for the corresponding data group. It is "false" by default, so existing figures are unaffected and no schema 
 *    migration is required.</li>
 * </ul>
 * </p>
 * 
 * @author  sruffner
 */
class Schema26 extends Schema25
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema26"); }
   @Override public int getVersion() { return(26); }
   @Override public String getLastApplicationVersion() { return("5.4.5"); }

   /** New element defining the styling and size of the violin plot component of a box plot. */
   final static String EL_VIOLIN = "violin";
   /** Default for the "size" attribute of the "violin" element; by default, the violin plot is not drawn. */
   final static String DEFAULT_VIOLIN_SIZE= "0%";

   /**
    * New display mode for the "trace" element: Similar to "polyline" mode, except data points are unconnected and a
    * line segment is drawn colinear with the least mean squares regression line fitting the data. Also a new display
    * mode for the "scatter" element -- essentially the "scatter" display mode with a trend line drawn.
    */
   final static String MODE_TRENDLINE = "trendline";
   
   /** Revised set of choices for the "trace" element's display mode (the "mode" attribute). */
   final static String[] MODE_TRACE_CHOICES_V26 = {
      DISPMODE_PTS, MODE_TRENDLINE, MODE_TRACE_STAIR, DISPMODE_BAND, DISPMODE_HIST, DISPMODE_MULTI};

   /** 
    * Default value for the "len" attribute of a "trace" element, specifying the window length N for computing the
    * sliding average trend line. If N==1, the trend line is the LMS regression line.
    */
   final static String DEFAULT_TRACE_LEN = "1";
   
   /** Revised set of choices for the "scatter" element's display mode (the "mode" attribute). */
   final static String[] MODE_SCATTER_CHOICES_V26 = {
      MODE_SCATTER_XY, MODE_TRENDLINE, MODE_SCATTER_SIZE, MODE_SCATTER_COLOR, MODE_SCATTER_COLORSZ
   };
   
   /** 
    * New element defines line style for the LMS regression line or a "connect the dots" polyline for the "scatter"
    * plot node.
    */
   final static String EL_SCATTERLINE = "scatterline";

   /** Label display mode for the "area" element: individual area labels turned off. */
   final static String MODE_AREA_OFF = "off";
   /** Label display mode for the "area" element: area label confined to interior of corresponding area. */
   final static String MODE_AREA_INSIDE = "inside";
   /** Label display mode for the "area" element: label is adjacent to and outside the corresponding area. */
   final static String MODE_AREA_OUTSIDE = "outside";
   /** The set of choices for the "area" element's label display mode (the "mode" attribute). */
   final static String[] MODE_AREA_CHOICES = {MODE_AREA_OFF, MODE_AREA_INSIDE, MODE_AREA_OUTSIDE};
   /** Default for the "mode" attribute of the "area" element. */
   final static String DEFAULT_AREA_MODE= MODE_AREA_OFF;

   /** By default, auto-generated labels in the <i>bar</i> plot element are turned off. */
   final static String DEFAULT_BAR_AUTO = "false";
   
   
   /**
    * This element map contains {@link SchemaElementInfo} objects for each element that is new to this schema or has a 
    * different attribute set compared to the previous schema.
    */
   private static final Map<String, SchemaElementInfo> elementMap26;

   static
   {
      elementMap26 = new HashMap<>();

      // 01jun2023: Added new "violin" element, controlling appearance of violin plot component of a box plot
      elementMap26.put( EL_VIOLIN, new SchemaElementInfo( false, 
            new String[] {}, 
            new String[] {A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_SIZE}, 
            new String[] {} ));
      
      // 01jun2023: The new "violin" element is a required component of the "box" plot element.
      elementMap26.put( EL_BOX, new SchemaElementInfo( false, 
            new String[] {EL_SYMBOL, EL_EBAR, EL_VIOLIN}, 
            new String[] {A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_LEGEND, A_MODE, A_BARWIDTH, A_X0, A_DX, A_SRC}, 
            new String[] {A_SRC} ));
      
      // (13jun2023) Added "len" attribute to "trace" element
      elementMap26.put( EL_TRACE, new SchemaElementInfo( false, 
            new String[] {EL_SYMBOL, EL_EBAR}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP,
                          A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_LEGEND, A_SKIP, A_MODE, A_BARWIDTH, 
                          A_BASELINE, A_AVG, A_XOFF, A_YOFF, A_LEN, A_SRC}, 
            new String[] {A_SRC} ));

      // 21jun2023: Added new "scatterline" element as a required component of the "scatter" element. It controls the
      // appearance of LMS regression line or a "connect the dots" polyline in the scatter plot.
      elementMap26.put( EL_SCATTERLINE, new SchemaElementInfo( false, 
            new String[] {}, 
            new String[] {A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT}, 
            new String[] {} ));
      elementMap26.put( EL_SCATTER, new SchemaElementInfo( false, 
            new String[] {EL_SCATTERLINE}, 
            new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_FILLCOLOR, A_TITLE, 
                          A_LEGEND, A_MODE, A_TYPE, A_SIZE, A_SRC}, 
            new String[] {A_SRC} ));
      
      // 26jun2023: Added text-related attributes and label display mode to the "area" element.
      elementMap26.put( EL_AREA, new SchemaElementInfo( true, 
            new String[] {}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_LEGEND, A_BASELINE, A_MODE, A_SRC}, 
            new String[] {A_SRC} ));
      
      // 27jun2023: Added text-related attributes and auto-label on flag ("auto") to the "bar" element.
      elementMap26.put( EL_BAR, new SchemaElementInfo( true, 
            new String[] {}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_LEGEND, A_MODE, A_BARWIDTH, A_BASELINE, A_AUTO, A_SRC}, 
            new String[] {A_SRC} ));
      

   }
   
   /**
    * Overridden to recognize any elements added and exclude any elements removed in this schema version; else defers 
    * to the superclass implementation. 
    */
   @Override public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap26.containsKey(elTag) || super.isSupportedElementTag(elTag));
   }

   /**
    * Overridden to provide schema element information for any element class added or revised in this schema version; 
    * for all other element classes, it defers to the superclass implementation. 
    */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
   {
      SchemaElementInfo info = elementMap26.get(elTag);
      return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
   }

   /** 
    * Enforces any requirements on the children content of elements that were introduced or revised in this schema
    * version. For all other element classes, it defers to the superclass implementation.
    * <ul>
    * <li>The "box" element now has 3 required children, in order: "symbol", "ebar", and "violin".</li>
    * <li>The "scatter" element now has 1 required child: "scatterline".</li>
    * </ul>
    */
   @Override public boolean hasRequiredChildren(BasicSchemaElement e)
   {
      String elTag = e.getTag();
      List<ISimpleXMLElement> children = e.getElementContent();
      if(EL_BOX.equals(elTag))
         return(children.size() == 3 && 
            EL_SYMBOL.equals(children.get(0).getTag()) &&
            EL_EBAR.equals(children.get(1).getTag()) &&
            EL_VIOLIN.equals(children.get(2).getTag()));
      else if(EL_SCATTER.equals(elTag))
         return(children.size() == 1 && EL_SCATTERLINE.equals(children.get(0).getTag()));
      
      return(super.hasRequiredChildren(e));
   }

   /** 
    * Enforces the following requirements on elements introduced in this schema version:
    * <ul>
    * <li>The "box" element now has 3 required children, in order: "symbol", "ebar", and "violin".</li>
    * <li>The "scatter" element now has 1 required child: "scatterline".</li>
    * </ul>
    */
   @Override public boolean isValidChildAtIndex(BasicSchemaElement e, String childTag, int index)
   {
      String elTag = e.getTag();
      SchemaElementInfo eInfo = getSchemaElementInfo(elTag);
      if(!eInfo.isChildAllowed(childTag)) return(false);
      if(EL_BOX.equals(elTag))
         return((EL_SYMBOL.equals(childTag) && index == 0) || (EL_EBAR.equals(childTag) && index == 1) ||
               (EL_VIOLIN.equals(childTag) && index == 2));
      else if(EL_SCATTER.equals(elTag))
         return(EL_SCATTERLINE.equals(childTag) && index == 0);

      return(super.isValidChildAtIndex(e, childTag, index));
   }

   /**
    * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
    * defers to the super class, with the following exceptions:
    * <ul>
    * <li>Validate the "size" attribute of the new <i>violin</i> element: It must be a valid measure attribute (user 
    * units not allowed).</li>
    * <li>The <i>trace</i> node now supports a "trendline" display mode ("mode" attribute value). The revised list
    * of possible modes is in {@link #MODE_TRACE_CHOICES_V26}.</li>
    * <li>The <i>scatter</i> node now supports a "trendline" display mode ("mode" attribute value). The revised list
    * of possible modes is in {@link #MODE_SCATTER_CHOICES_V26}.</li>
    * <li>Added integer attribute <i>len</i> >= 1 to <i>trace</i> node.</li>
    * <li>The <i>mode</i> attribute of an <i>area</i> node is an enumerated attribute selecting the label display mode 
    * from one of three possible choices.</li>
    * <li>Added boolean attribute <i>auto</i> to <i>bar</i> node. Validation handled by super class instance.</li>
    * </ul>
    */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
   {
      String tag = e.getTag();
      if(EL_VIOLIN.equals(tag) && A_SIZE.equals(attr))
      {
         return(isValidMeasureAttributeValue(value, false, true, false));
      }
      else if(EL_TRACE.equals(tag))
      {
         if(A_MODE.equals(attr)) return(isValidEnumAttributeValue(value, MODE_TRACE_CHOICES_V26));
         else if(A_LEN.equals(attr)) return(isValidIntegerAttributeValue(value, 1, Integer.MAX_VALUE));
      }
      else if(EL_SCATTER.equals(tag) && A_MODE.equals(attr)) 
         return(isValidEnumAttributeValue(value, MODE_SCATTER_CHOICES_V26));
      else if(A_MODE.equals(attr) && EL_AREA.equals(tag))
         return(isValidEnumAttributeValue(value, MODE_AREA_CHOICES));
      
      return(super.isValidAttributeValue(e, attr, value));
   }

   /**
    * This method handles the actual details of migrating from the previous version to this one. It makes the following 
    * changes:
    * <ul>
    *    <li>The new "violin" element is inserted as the third child of each "box" element encountered. All of its
    *    attributes are implicit. Since the default size is 0, this ensures the violin plot won't be drawn -- which is
    *    as it should be since the feature did not exist prior to this schema change!</li>
    *    <li>The new "scatterline" element is inserted as the first and only child of the each "scatter" element
    *    encountered. To ensure that existing scatter plots are unaffected, the element's stroke width is explicitly
    *    set to "0in".</li>
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

         // after updating schema element info, insert the new "violin" element as the third child in each "box" plot
         // element encountered. All attributes implicit initially.
         if(EL_BOX.equals(elTag))
            e.insert((BasicSchemaElement) createElement(EL_VIOLIN), 2);
         
         // also insert the component "scatterline" element as the only child of each "scatter" element encountered.
         // The component's stroke width is explictly set to "0in" to ensure existing scatter plots are unaffected.
         if(EL_SCATTER.equals(elTag))
         {
            BasicSchemaElement child = (BasicSchemaElement) createElement(EL_SCATTERLINE);
            child.setAttributeValueByName(A_STROKEWIDTH, "0in");
            e.insert(child, 0);
         }
         
         // if element has any children, push them onto the stack so that we check them as well! We don't push on any 
         // child that was added during migration of the parent, since it will already conform to current schema.
         for(int i=0; i<e.getChildCount(); i++)
         {
            BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
            if( (EL_BOX.equals(elTag) && EL_VIOLIN.equals(child.getTag())) ||
                (EL_SCATTER.equals(elTag) && EL_SCATTERLINE.equals(child.getTag())) )
               continue;
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
