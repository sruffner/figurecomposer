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
 * <b>Schema25</b> is the encapsulation of the <i>FypML</i> figure model XML schema version 25. It extends {@link 
 * Schema24} and includes support for migrating schema version 24 documents to version 25. This version was first 
 * introduced with version 5.4.1 of <i>FigureComposer</i>.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul> 
 *    <li>(15 mar 2023, for app V5.4.1) 2D bar plots can now be rendered in a polar plot. Updated the "pgraph"
 *    element to admit the "bar" element as a child.</li>
 *    <li>(23 mar 2023, for app V5.4.1) Introduced new 2D presentation element, the "box" plot. Only permitted in a 2D
 *    Cartesian graph.</li>
 *    <li>(28 mar 2023, for app V5.4.1) Modified definition of the "colorbar" element. The choice set for the enumerated
 *    attribute "cmap" no longer includes the reverse-direction version of each supported color map, which start with
 *    the word "reverse". Instead, the element now has a boolean attribute, "reverse" (default is "false"), which, if 
 *    "true", selects the reverse-direction version of the map specified by the "cmap" attribute. During schema
 *    migration, each color bar using a reverse-direction color map must be updated so that "cmap" is the base name of
 *    the map (without the prepended "reverse") and "reverse" = "true". No change is necessary when the old value for 
 *    "cmap" does not start with "reverse"</li>
 *    <li>(29 mar 2023, for app V5.4.1) Added 5 additional choices for the "cmap" attribute of the "colorbar" element,
 *    representing 5 perceptually uniform sequential colormaps defined in the Matplotlib library - viridis, cividis,
 *    plasma, inferno, and magma.</li>
 *    <li>(24 apr 2023, for app V5.4.3) Modification to support custom-defined color maps: The "cmap" attribute of a
 *    "colorbar" element can be set to one of the 14 built-in color map names OR the definition of a user-defined
 *    colormap: "map_name[k1 k2 ... kN]". Here "map_name" is the name assigned to the custom color map, and the string
 *    tokens within the brackets are the list of key frames defining the map. Each key frame is an 8-digit hexadecimal 
 *    string NNRRGGBB where NN is an index into the color map's 256-entry lookup table, and RRGGBB is the opaque RGB
 *    color at that index. The rest of the LUT is calculated by piecewise linear interpolation between key frames. The
 *    index for the first key frame is always 0, and the last is always 255. The color map definition can have 2-10
 *    key frames, listed in ascending order by LUT index. The color map name can only contain word characters (A-Z, a-z,
 *    0-9, and the underscore), and it cannot duplicate one of the built-in color map names.</li>
 *    <li>(25 apr 2023, for app V5.4.4) Modified the "trace", "scatter", "scatter3d", "raster", "box", and "function"
 *    elements to allow <i>FypML</i> styled text in the "title" attribute. No schema migration required.</li>
 *    <li>(26 apr 2023, for app V5.4.4) Now using a double-semicolon (";;") instead of a comma (',') as the separator
 *    between string tokens specifying the data group colors and labels -- found in the text content of the grouped-data
 *    presentation nodes ("bar", "area", "pie"). As of this change, data group labels can contain commas (though not
 *    double-semicolons!), and can be "styled text", which uses commas in the attribute codes suffix. No
 *    schema migration required, as the code which parses the text content of grouped-data nodes can handle both
 *    scenarios.</li>
 *    <li>(03 may 2023, for app V5.4.4) Modified "figure" node to include 3 additional properties for rendering the
 *    figure title aligned WRT the bounding box: "hide" (default=true), "align" (default=centered), and "valign"
 *    (default="top"). Also, the figure's "title" now allows styled text and linefeeds. No schema migration required. 
 *    For existing figures with a non-empty title, the title won't be rendered b/c "hide=true" by default!</li>
 *    <li>(11 may 2023, for app V5.4.4) Modified "graph", "graph3d", and "pgraph" nodes to support a semi-automated
 *    graph title: "hide" (default=true), "align" (default=centered), and "gap" (default="0in"). The "title" 
 *    attribute now allows styled text and linefeeds. No schema migration required. For existing figures with a
 *    non-empty title, the title won't be rendered b/c "hide=true" by default.</li>
 *    <li>(17 may 2023, for app V5.4.4) Modified "pie" node to support rendering labels on each pie slice. Added
 *    the standard text-related style attributes, plus a new "mode" attribute specifying the slice label display
 *    mode. Default label mode is "off", so no existing figures unaffected and no schema migration required.</li>
 * </ul>
 * </p>
 * 
 * @author  sruffner
 */
class Schema25 extends Schema24
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema25"); }
   @Override public int getVersion() { return(25); }
   @Override public String getLastApplicationVersion() { return("5.4.4"); }

   /** New data presentation element -- a box plot. */
   final static String EL_BOX = "box";
   /** A display mode for the "box" element: vertically oriented, without notch. */
   final static String MODE_BOX_V = "vertical";
   /** A display mode for the "box" element: horizontally oriented, without notch. */
   final static String MODE_BOX_H = "horizontal";
   /** A display mode for the "box" element: vertically oriented, with notch. */
   final static String MODE_BOX_VNOTCH = "vertnotch";
   /** A display mode for the "box" element: horizontally oriented with notch. */
   final static String MODE_BOX_HNOTCH = "horiznotch";
   /** The set of choices for the "box" element's display mode (the "mode" attribute). */
   final static String[] MODE_BOX_CHOICES = {MODE_BOX_V, MODE_BOX_H, MODE_BOX_VNOTCH, MODE_BOX_HNOTCH};
   /** Default for the "mode" attribute of the "box" element. */
   final static String DEFAULT_BOX_MODE= MODE_BOX_V;
   /** Default for the "barWidth" attribute of the "box" element. */
   final static String DEFAULT_BOX_BARWIDTH = "10%";

   /** 
    * [As of 29mar2023] Revised set of choices for the <i>cmap</i> attribute of a color bar element. As of this schema,
    * the reverse-direction version of each color map is chosen by setting the new <i>reverse</i> attribute to true. 
    * Also added 5 new "perceptually uniform" color maps defined by the Matplotlib library.
    */
   final static String CMAP_VIRIDIS = "viridis";
   final static String CMAP_CIVIDIS = "cividis";
   final static String CMAP_PLASMA = "plasma";
   final static String CMAP_INFERNO = "inferno";
   final static String CMAP_MAGMA = "magma";
   final static String[] CMAP_CHOICES_V25 = {
      CMAP_GRAY, CMAP_HOT, CMAP_AUTUMN, CMAP_JET, CMAP_TROPIC, CMAP_COOL, CMAP_COPPER, CMAP_BONE, CMAP_HSV,
      CMAP_VIRIDIS, CMAP_CIVIDIS, CMAP_PLASMA, CMAP_INFERNO, CMAP_MAGMA
   };

   /** Default for "hide" attribute of a "figure" element; by default, the figure title is hidden. */
   final static String DEFAULT_FIGURE_HIDE = "true";
   /** Default for "align" attribute of a "figure", horizontally centering figure title WRT bounding box. */
   final static String DEFAULT_FIGURE_HALIGN = Schema0Constants.HALIGN_CENTER;
   /** Default for "valign" attribute of a "figure", aligning figure title with top edge of bounding box. */
   final static String DEFAULT_FIGURE_VALIGN = Schema0Constants.VALIGN_TOP;
   /** Default for "hide" attribute of any graph container; by default, the graph title is hidden. */
   final static String DEFAULT_GRAPH_HIDE = "true";
   /** Default for "align" attribute of any graph container, horizontally centering graph title WRT bounding box. */
   final static String DEFAULT_GRAPH_HALIGN = Schema0Constants.HALIGN_CENTER;
   /** 
    * Default for "gap" attribute of any graph container, specifying graph title's vertical separation from the top
    * or bottom edge of its bounding box. 
    */
   final static String DEFAULT_GRAPH_GAP = "0in";

   /** Label display mode for the "pie" element: individual pie slice labels turned off. */
   final static String MODE_PIE_OFF = "off";
   /** Label display mode for the "pie" element: each slice labeled with percentage represented. */
   final static String MODE_PIE_PERCENT = "percent";
   /** Label display mode for the "pie" element: each slice labeled with group's legend label. */
   final static String MODE_PIE_LEGENDLABEL = "legendlabel";
   /** Label display mode for the "pie" element: each slice labeled with legend label and percentage. */
   final static String MODE_PIE_FULL = "full";
   /** The set of choices for the "pie" element's label display mode (the "mode" attribute). */
   final static String[] MODE_PIE_CHOICES = {MODE_PIE_OFF, MODE_PIE_PERCENT, MODE_PIE_LEGENDLABEL, MODE_PIE_FULL};
   /** Default for the "mode" attribute of the "pie" element. */
   final static String DEFAULT_PIE_MODE= MODE_PIE_OFF;
   
   /**
    * This element map contains {@link SchemaElementInfo} objects for each element that is new to this schema or has a 
    * different attribute set compared to the previous schema.
    */
   private static final Map<String, SchemaElementInfo> elementMap25;

   static
   {
      elementMap25 = new HashMap<>();
      
      // 15mar2023: Polar graph now admits bar plot as a child.
      // 11may2023: New attributes to support a semi-automated graph title.
      elementMap25.put( EL_PGRAPH, new SchemaElementInfo( false, 
            new String[] {EL_PAXIS, EL_COLORBAR, EL_LEGEND, EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE,
                          EL_IMAGE, EL_FUNCTION, EL_TRACE, EL_RASTER, EL_SCATTER, EL_AREA, EL_PIE, EL_BAR}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_CLIP, A_ID, A_BOXCOLOR, 
                          A_GRIDONTOP, A_HIDE, A_HALIGN, A_GAP, A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

      // 23mar2023: Added new data presentation element, the "box" plot. It can only appear in a 2D "graph" element.
      elementMap25.put( EL_BOX, new SchemaElementInfo( false, 
            new String[] {EL_SYMBOL, EL_EBAR}, 
            new String[] {A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_LEGEND, A_MODE, A_BARWIDTH, A_X0, A_DX, A_SRC}, 
            new String[] {A_SRC} ));
      
      // 23mar2023: The 2D graph admits the new "box" plot.
      // 11may2023: New attributes to support a semi-automated graph title.
      elementMap25.put( EL_GRAPH, new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_COLORBAR, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_TEXTBOX, EL_LINE, 
                          EL_SHAPE, EL_IMAGE, EL_GRAPH, EL_PGRAPH, EL_FUNCTION, EL_TRACE, EL_RASTER, EL_CONTOUR, 
                          EL_BAR, EL_SCATTER, EL_AREA, EL_PIE, EL_BOX}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, 
                          A_CLIP, A_AUTORANGE, A_ID, A_BOXCOLOR, A_HIDE, A_HALIGN, A_GAP, 
                          A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));
      
      // 28mar2023: Added "reverse" attribute to "color bar" element to select the reverse-direction version of each
      // supported color map scheme.
      elementMap25.put( EL_COLORBAR, new SchemaElementInfo( false, 
            new String[] {EL_TICKS}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_HIDE, A_TITLE, A_SPACER, A_LABELOFFSET, A_START,
                          A_END, A_GAP, A_SIZE, A_EDGE, A_CMAP, A_CMODE, A_CMAPNAN, A_LINEHT, A_SCALE, A_REVERSE}, 
            new String[] {A_START, A_END} ));
      
      // 03may2023: Added "hide", "align", "valign" attributes to support a rendered figure title.
      elementMap25.put( EL_FIGURE, new SchemaElementInfo( false, 
            new String[] {EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE, EL_IMAGE, EL_GRAPH, EL_PGRAPH, EL_GRAPH3D, EL_REF}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT, A_TITLE, 
                          A_BORDER, A_BKG, A_NOTE, A_HIDE, A_HALIGN, A_VALIGN}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT} ));
      
      // 11may2023: New attributes to support a semi-automated graph title.
      elementMap25.put( EL_GRAPH3D, new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_GRIDLINE, EL_BACK3D, EL_LEGEND, EL_COLORBAR, EL_LABEL, EL_LINE, EL_SHAPE, 
                          EL_TEXTBOX, EL_IMAGE, EL_SCATTER3D, EL_SURFACE}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, 
                          A_ROTATE, A_ELEVATE, A_TITLE, A_ID, A_BACKDROP, A_PSCALE, A_HIDE, A_HALIGN, A_GAP, 
                          A_LOC, A_WIDTH, A_HEIGHT, A_DEPTH}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT, A_DEPTH} ));
      

      // 17may2023: Added text-related attributes and slice label display mode.
      elementMap25.put( EL_PIE, new SchemaElementInfo( true, 
            new String[] {}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_LEGEND, A_GAP, A_DISPLACE, A_MODE, A_START, A_END, A_SRC}, 
            new String[] {A_START, A_END, A_SRC} ));
      

   }
   
   /**
    * Overridden to recognize any elements added and exclude any elements removed in this schema version; else defers 
    * to the superclass implementation. 
    */
   @Override public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap25.containsKey(elTag) || super.isSupportedElementTag(elTag));
   }

   /**
    * Overridden to provide schema element information for any element class added or revised in this schema version; 
    * for all other element classes, it defers to the superclass implementation. 
    */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
   {
      SchemaElementInfo info = elementMap25.get(elTag);
      return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
   }

   /** 
    * Enforces any requirements on the children content of elements that were introduced or revised in this schema
    * version. For all other element classes, it defers to the superclass implementation.
    * <ul>
    * <li>The new "box" element has two required children, a "symbol" element followed by an "ebar" element.</li>
    * </ul>
    */
   @Override public boolean hasRequiredChildren(BasicSchemaElement e)
   {
      String elTag = e.getTag();
      List<ISimpleXMLElement> children = e.getElementContent();
      if(EL_BOX.equals(elTag))
         return(children.size() == 2 && 
            EL_SYMBOL.equals(children.get(0).getTag()) &&
            EL_EBAR.equals(children.get(1).getTag()));

      return(super.hasRequiredChildren(e));
   }

   /** 
    * Enforces the following requirements on elements introduced in this schema version:
    * <ul>
    * <li>The new "box" element has two required children, a "symbol" element followed by an "ebar" element.</li>
    * </ul>
    */
   @Override public boolean isValidChildAtIndex(BasicSchemaElement e, String childTag, int index)
   {
      String elTag = e.getTag();
      SchemaElementInfo eInfo = getSchemaElementInfo(elTag);
      if(!eInfo.isChildAllowed(childTag)) return(false);
      if(EL_BOX.equals(elTag))
         return((EL_SYMBOL.equals(childTag) && index == 0) || (EL_EBAR.equals(childTag) && index == 1));
      
      return(super.isValidChildAtIndex(e, childTag, index));
   }

   /**
    * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
    * defers to the super class, with the following exceptions:
    * <ul>
    * <li>Validate select attributes of the new <i>box</i> element: The "mode" must be one of the box plot display
    * modes; the "barWidth" must be a valid measure attribute (user units not allowed).</li>
    * <li>The set of choices for the "cmap" attribute of the <i>colorbar</i> element changed in this schema. The
    * "reverse"-direction version are no longer among the choices. The reverse direction version is chosen by setting
    * the new "reverse" flag on the "colorbar" element. Validation of the boolean-valued "reverse" flag is handled in
    * schema 23.</li> 
    * <li>[24apr2023, for v5.4.3] The "cmap" attribute can also be a custom color map in the form "name[k1 k2 ... kN]",
    * as described in the class header.</li>
    * <li>[25apr2023, for v5.4.4] The "title" attribute of a "trace", "scatter", "scatter3d", "raster", "box", and
    * "function" nodes can now contain styled text.</li>
    * <li>[03may2023, for v5.4.4] The "title" attribute of a "figure" node can now contain styled text.</li>
    * <li>[11may2023, for v5.4.4] The "title" attribute of any graph container can now contain styled text.</li>
    * <li>[17may2023, for v5.4.4] The "mode" attribute of a "pie" node is an enumerated attribute selecting the slice
    * label display mode from one of 4 possible choices.</li>
    * </ul>
    */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
   {
      String tag = e.getTag();
      if(EL_BOX.equals(tag))
      {
         if(A_MODE.equals(attr))
            return(isValidEnumAttributeValue(value, MODE_BOX_CHOICES));
         if(A_BARWIDTH.equals(attr))
            return(isValidMeasureAttributeValue(value, false, true, false));
         if(A_TITLE.equals(attr))
            return(isValidStyledTextString(value));
      }
      else if(EL_COLORBAR.equals(tag) && A_CMAP.equals(attr))
         return(isValidEnumAttributeValue(value, CMAP_CHOICES_V25) || isValidCustomColorMapDefinition(value));
      else if(A_TITLE.equals(attr) && (EL_TRACE.equals(tag) || EL_SCATTER.equals(tag) || EL_SCATTER3D.equals(tag) ||
            EL_RASTER.equals(tag) || EL_FUNCTION.equals(tag) || EL_FIGURE.equals(tag)) ||
            EL_GRAPH.equals(tag) || EL_GRAPH3D.equals(tag) || EL_PGRAPH.equals(tag))
         return(isValidStyledTextString(value));
      else if(A_MODE.equals(attr) && EL_PIE.equals(tag))
         return(isValidEnumAttributeValue(value, MODE_PIE_CHOICES));
      
      return(super.isValidAttributeValue(e, attr, value));
   }
   
   
   /** 
    * Validate a custom color map definition in string form: "map_name[k1 k2 ... kN]". The color map name must contain
    * one or more word characters (A-Z, a-z, 0-9, or the underscore) and cannot duplicate the name of any of the 
    * built-in color maps. At least 2 and up to 10 key frame tokens <i>kn</i> can be specified, each of which must be
    * parsable as an 8-digit hexadecimal integer NNRRGGBB, where NN is the LUT index for the key frame and RRGGBB is
    * the corresponding opaque RGB color at that index. The key frames must be listed in ascending order by LUT index,
    * the first key frame must have index 0, the last must have index 255, and no two key frames can have the same 
    * index. Both the leading and trailing brackets must be present in the string.
    * 
    * @param s The string to validate.
    * @return True if string contains a valid definition of a custom color map, as described; false otherwise.
    */
   boolean isValidCustomColorMapDefinition(String s)
   {
      if(s == null) return(false);
      int i0 = s.indexOf('['), i1 = s.indexOf(']');
      if((i0 < 0) || (i0 >= i1) || (i1 != s.length() - 1)) return(false);
      String name = s.substring(0, i0);
      if(isValidEnumAttributeValue(name, CMAP_CHOICES_V25) || !name.matches("\\w+")) return(false);
      
      String[] sframes = s.substring(i0+1, i1).split(" ");
      if((sframes.length < 2) || (sframes.length > 10)) return(false);
      
      try
      {
         int prevIdx = -1;
         for(int i=0; i<sframes.length; i++)
         {
            int idx = (Integer.parseUnsignedInt(sframes[i], 16) >> 24) & 0x0FF;
            if((prevIdx >= idx) || ((i == 0) && (idx != 0)) || ((i == sframes.length - 1) && (idx != 255)))
               return(false);
            prevIdx = idx;
         }
         
         return(true);
      }
      catch(Exception e) { return(false); }
   }
   
   /**
    * This method handles the actual details of migrating from the previous version to this one. It makes the following 
    * changes:
    * <ul>
    *    <li>For each "colorbar" element, explicitly set the new "reverse" flag to "true" if the old value of "cmap"
    *    is one of the reverse-direction choices, then set "cmap" to the forward-direction choice. If the old value of
    *    "cmap" is a forward-direction choice, leave it be and let "reverse" be implicit (the default is false).</li>
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
         
         boolean wasReverseCMap = false;
         if(EL_COLORBAR.equals(elTag))
         {
            String cmap = e.getAttributeValueByName(A_CMAP);
            wasReverseCMap = (cmap != null) && cmap.startsWith("reverse");
            if(wasReverseCMap) e.setAttributeValueByName(A_CMAP, cmap.substring(7));
         }
         
         // migrate the element object's schema info.
         e.updateSchema(this, null);

         if(wasReverseCMap)
            e.setAttributeValueByName(A_REVERSE, "true");
         
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
