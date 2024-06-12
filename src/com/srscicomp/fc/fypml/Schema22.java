package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.util.Utilities;
import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <b>Schema22</b> is the encapsulation of the <i>FypML</i> figure model XML schema version 22. It extends {@link 
 * Schema21} and includes support for migrating schema version 21 documents to version 22. This version was first 
 * introduced with version 5.0.1 of <i>FigureComposer</i>, which introduced support for contour plots.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul> 
 *    <li>(Aug2016, for app V5.0.1) Introduced new schema element <i>contour</i>, representing a 2D contour plot. It
 *    only admits <b>XYZIMG</b> data sets, and it offers 4 display modes: a contour plot with color-mapped contour level
 *    lines only; a filled contour plot in which the level lines are superimposed on color-mapped contour bands; a
 *    "heat map" plot; and the "heat map" with level-lines superimposed. As such, <i>contour</i> replaces the 
 *    <i>heatmap</i> element, which is deprecated as of this schema version. Note that <i>contour</i> has some
 *    additional attributes not found in its predecessor: "levels" is the list of desired contour levels; "mode" 
 *    selects the display mode. In addition <i>contour</i> has all the stroke-related styles except "strokePat".</li>
 *    <li>(01nov2016, for app V5.0.2) To support translucent colors, the value of the style attributes "strokeColor" and
 *    "fillColor" now can have 3 distinct forms: (1) the value "none", representing fully transparent black; (2) a 
 *    6-digit hexadecimal string "RRGGBB" representing an opaque color (alpha = 255); or (3) a 8-digit hex string 
 *    "AARRGGBB" in which the alpha component is explicit, thereby allowing specification of a translucent color. No 
 *    changes required during schema migration.</li>
 *    <li>(01nov2016) The "cmapnan" attribute may take on the 8-digit form, but its alpha component must be "FF" because
 *    it must be an opaque color. No changes required during schema migration.</li>
 *    <li>(01nov2016) The "bkgFill" attribute now allows specification of a translucent solid color fill. However -- as 
 *    in the prior schema, both color stop values must be opaque when a gradient fill is specified. No changes required
 *    during schema migration.</li>
 *    <li>(01nov2016) The "bar", "pie" and "area" nodes -- all "grouped-data" presentation nodes -- now support 
 *    translucent color. No changes required during schema migration, and text content is not validated by the schema
 *    object.</li>
 *    <li>(08nov2016) Mods to "raster" node: Additional display modes "pdf" and "cdf". Added optional attributes "start"
 *    and "end" to define the range over which raster data is binned to form a histogram in the "histogram", "pdf" and
 *    "cdf" display modes. If start >= end, they are ignored and the actual observed sample range is used. This is how
 *    the "histogram" mode worked prior to these changes, so start and end both default to 0. No changes required 
 *    during schema migration.</li>
 *    <li>(22nov2016, for app V5.1.0 in Nov 2017) Added optional boolean attribute "stemmed" to the "scatter3d" node. If
 *    true (the default), stem lines are drawn from each data point down to the base plane. If false, the data points 
 *    are connected by a poly-line instead. The stem lines or single poly-line are stroked IAW the node's stroke 
 *    properties. No changes required during schema migration.</li>
 *    <li>(13mar2018, for app V5.1.1 in Mar 2018) Added optional attribute "dotSize" and "dotColor" to the "scatter3d" 
 *    node. The former is a list of up to 3 whitespace-separated integers "xySize xzSize yzSize", while the latter is a 
 *    list of up to 3 color strings "xyC xzC yzC". These specify the size and color of the filled dots for the scatter 
 *    plot's projections onto the parent graph's XY, XZ, and YZ backplanes (when those backplanes are rendered -- they 
 *    are not drawn in some 3D backdrop styles). Dot size is in typographical points and is restricted to [0..10], where
 *    0 indicates that the projection is not rendered. Any missing values in the list are assumed to be 0. Note that the
 *    default value for "dotSize" is an empty string, which means that all 3 projections have zero dot size and thus are
 *    not drawn. Dot color is represented by a 6-digit hex string "RRGGBB" representing an opaque color, an 8-digit 
 *    hex string "AARRGGBB" for a translucent color, or "none" for a fully transparent color. A transparent dot color
 *    indicates that the projection is not rendered. Any missing dot color is set to opaque black. The default value for
 *    "dotColor" is "", meaning that all 3 projections have a dot color of opaque black.</li>
 *    <li>(14jun2018, for app V5.1.2) Added optional attribute "boxC" to the "graph" node, which lets user specify a
 *    background color for the 2D graph's data box. Its default value is "none" (transparent black), which ensures that
 *    all figures existing prior to this are unaffected by this change. No changes during schema migration.</li>
 *    <li>(08aug2018, for app V5.1.2) Added new schema element <i>pgraph</i>, representing a 2D graph container that is
 *    specialized to render polar plots with a linear axis. Unlike the polar mode of the <i>graph</i> element, this
 *    element automates labeling of the polar grid, allows theta to increase in the clockwise or counterclockwise 
 *    direction, can have "0 deg" point in any direction, and allows any theta axis range in the unit circle. It is
 *    modeled after Matlab's "polarAxes" object. The theta and radial axis components of the polar plot are represented 
 *    by another new schema element, <i>paxis</i>. No changes required during schema migration.</li>
 * </ul>
 * </p>
 * 
 * @author  sruffner
 */
class Schema22 extends Schema21
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema22"); }
   @Override public int getVersion() { return(22); }
   @Override public String getLastApplicationVersion() { return("5.1.3"); }

   /** A new element -- the contour plot node. */
   final static String EL_CONTOUR = "contour";
   
   /** 
    * New attribute: The level list for a contour plot node -- a list of whitespace-separated numeric tokens, each of 
    * which is a desired contour level. It may be an empty string, in which case contour levels are auto-selected. 
    */
   final static String A_LEVELS = "levels";
   /** Default value for the {@link #A_LEVELS} attribute of a contour plot node. */
   final static String DEFAULT_LEVELS = "";
   /** The maximum number of different contour levels that can be specified in the {@link A_LEVELS} attribute. */
   final static int MAX_CONTOUR_LEVELS = 20;
   
   /** Display mode for contour node: color-mapped level-lines only. */
   final static String MODE_CONTOUR_LINES = "levelLines";
   /** Display mode for contour node: filled contour bands with super-imposed level-lines. */
   final static String MODE_CONTOUR_FILLED = "filledContours";
   /** Display mode for contour node: as a heat map image. */
   final static String MODE_CONTOUR_HEATMAP = "heatMap";
   /** Display mode for contour node: heat map image with super-imposed level-lines. */
   final static String MODE_CONTOUR_CONTOUREDHM = "contouredHeatMap";
   /** Set of choices for the contour node's display mode. */
   final static String[] MODE_CONTOUR_CHOICES = {
      MODE_CONTOUR_LINES, MODE_CONTOUR_FILLED, MODE_CONTOUR_HEATMAP, MODE_CONTOUR_CONTOUREDHM
   };
   /** Default value for the {@link #A_MODE} attribute of a contour node. */
   final static String DEFAULT_CONTOUR_MODE = MODE_CONTOUR_LINES;
   
   /** 
    * Regular expression that encapsulates the 8-digit hexadecimal representation of a color attribute, including the
    * alpha component. This format permits specification of translucent colors.
    */
   public final static String COLOR_REGEX_ARGB = "[0-9a-fA-F]{8}";

   /** New display mode for the "raster" element: Probability density function approximation. */
   final static String MODE_RASTER_PDF = "pdf";
   
   /** New display mode for the "raster" element: Cumulative density function approximation. */
   final static String MODE_RASTER_CDF = "cdf";
   
   /** Revised set of choices for the "raster" element's display mode (the "mode" attribute). */
   final static String[] MODE_RASTER_CHOICES_V22 = {
      MODE_RASTER_TRAINS, MODE_RASTER_TRAINS2, MODE_RASTER_HIST, MODE_RASTER_PDF, MODE_RASTER_CDF
   };

   /** 
    * Default value for the optional "start" and "end" attributes of the "raster" element. With start==end, the actual
    * observed sample range is used instead.
    */
   final static String DEFAULT_RASTER_STARTEND = "0";
   
   /** New boolean attribute for a 3D scatter/line plot node. Selects line plot or stem plot display. */
   final static String A_STEMMED = "stemmed";
   /** Default value for the {@link #A_STEMMED} attribute of a 3D scatter/line plot node. */
   final static String DEFAULT_STEMMED = "true";
   
   /** 
    * New attribute for 3D scatter plot node: list of 0-3 integers "xy xz yz" representing the size of the dots in the
    * scatter plot's XY, XZ and YZ backplane projections. Each integer must lie in [0..10]. Any missing dot size
    * defaults to 0.
    */
   final static String A_DOTSIZE = "dotSize";
   /** 
    * Default value for the {@link #A_DOTSIZE} attribute of 3D scatter plot; this is an empty string, indicating that 
    * dot size is zero for all three of the scatter plot's backplane projections.
    */
   final static String DEFAULT_DOTSIZE = "";
   /**
    * New attribute for 3D scatter plot node: list of 0-3 string tokens "xy xz yz" representing the color of the dots
    * in the scatter plot's XY, XZ and YZ backplane projections. Each string may be a 6-digit hex string RRGGBB for an
    * opaque color, an 8-digit hex string AARRGGBB for a translucent color, or "none" = transparent black. Any missing
    * dot color defaults to opaque black.
    */
   final static String A_DOTCOLOR = "dotColor";
   /** 
    * Default value for the {@link #A_DOTCOLOR} attribute of 3D scatter plot; this is an empty string, indicating that 
    * dot color is opaque black for all three of the scatter plot's backplane projections.
    */
   final static String DEFAULT_DOTCOLOR = "";

   /**
    * New attribute for 2D graph node: The background color for the graph's data box. Value may be a 6-digit hex string
    * RRGGBB for an opaque color, an 8-digit hex string AARRGGBB for a translucent color, or "none" = transparent black.
    * Also applies to the new 2D polar plot node, specifying the background color for the polar grid, which will be a 
    * full circle or pie wedge rather than a rectangle.
    */
   final static String A_BOXCOLOR = "boxColor";
   /** Default value for the {@link #A_BOXCOLOR} attribute of a 2D graph = "none" (transparent black). */
   final static String DEFAULT_BOXCOLOR = "none";
   
   /** A new element -- the 2D polar plot node. */
   final static String EL_PGRAPH = "pgraph";
   
   /** Boolean attribute for 2D polar plot, indicating whether or not polar grid is rendered on top of any data. */
   final static String A_GRIDONTOP = "gridontop";
   /** Default value for the {@link #A_GRIDONTOP} attribute of a 2D polar plot = "false". */
   final static String DEFAULT_GRIDONTOP = "false";
   /** Default value for the {@link #A_CLIP} attribute of a 2D polar plot = "true". */
   final static String DEFAULT_PGRAPH_CLIP = "true";

   /** New element representing the theta or radial axis compoment of a 2D polar plot. */
   final static String EL_PAXIS = "paxis";
   
   /** 
    * Boolean attribute for a polar plot axis. If true, the direction of increasing values is reversed for the axis.
    * For a theta axis, the normal direction is CCW and the reverse is CW. For a radial axis, values normally increase 
    * outward from the origin; if reversed, the values increase toward the origin.
    */
   final static String A_REVERSE = "reverse";
   /** Default value for the {@link #A_REVERSE} attribute of a 2D polar plot axis = "false". */
   final static String DEFAULT_REVERSE = "false";
   
   /** 
    * Integer attribute specifying the reference angle for a polar plot axis. Restricted to [-359..359]. For a theta
    * axis, this is the direction in which "0 degrees" points, measured CCW from "due east". For a radial axis, this
    * specifies the direction of the ray along which the radial axis grid labels, specified WRT the theta axis 
    * definition. If the polar grid is not "full circle" and the radial axis reference angle lies outside the theta axis
    * range, the radial grid labels are located at one arc endpoint or the other.
    */
   final static String A_REFANGLE = "refangle";
   /** Default value for the {@link #A_REFANGLE} attribute of a 2D polar plot axis = "0". */
   final static String DEFAULT_REFANGLE = "0";

   /** 
    * Required attribute specifying the grid divisions for a polar plot axis, formatted as a list of floating-point
    * number tokens, whitespace-separated. First 3 elements [S E M] specify regularly spaced grid divisions of size M 
    * starting at S and not exceeding E. Any additional elements are treated as individual custom grid divisions. Note
    * that if M&le;0, there are no regularly spaced grid divisions. If M&gt;0 and S&ge;E, S and E are replaced by the
    * current axis range endpoints -- so that the regular grid is updated whenever the axis range changes. If there are
    * fewer than 3 elements in the list, then any missing elements are set to 0.
    */
   final static String A_PDIVS = "pdivs";
   
   /** Default value for the {@link #A_GAP} attribute of a 2D polar plot axis = "0.2in". */
   final static String DEFAULT_PAXIS_GAP = "0.2in";

   /**
    * This element map contains {@link SchemaElementInfo} objects for each element that is new to this schema or has a 
    * different attribute set compared to the previous schema.
    */
   private static Map<String, SchemaElementInfo> elementMap22 = null;

   static
   {
      elementMap22 = new HashMap<String, SchemaElementInfo>();
      
      // 03aug2016: 2D graph accepts new "contour" element in place of deprecated "heat map" element
      // 14jun2018: Added new optional attribute specifying background color of data box.
      // 08aug2018: New 2D polar plot element "pgraph", along with polar axis component "paxis". Note that the 2D graph 
      // allows the polar plot element as a child, but not vice versa. Also, polar plot only allows certain 2D data 
      // presentation elements. The polar axis allows text content -- only for specifying custom grid labels as a
      // comma-separated token list.
      elementMap22.put( EL_GRAPH, new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_ZAXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE,
                          EL_IMAGE, EL_GRAPH, EL_PGRAPH, EL_FUNCTION, EL_TRACE, EL_RASTER, EL_CONTOUR, EL_BAR, 
                          EL_SCATTER, EL_AREA, EL_PIE}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, 
                          A_CLIP, A_AUTORANGE, A_ID, A_BOXCOLOR, 
                          A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

      elementMap22.put( EL_CONTOUR, 
            new SchemaElementInfo( false, 
                  new String[] {}, 
                  new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, 
                                A_TITLE, A_MODE, A_SMOOTH, A_LEVELS, A_SRC}, 
                  new String[] {A_SRC} ));

      elementMap22.put( EL_PGRAPH, new SchemaElementInfo( false, 
            new String[] {EL_PAXIS, EL_ZAXIS, EL_LEGEND, EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE,
                          EL_IMAGE, EL_FUNCTION, EL_TRACE, EL_RASTER, EL_SCATTER, EL_AREA, EL_PIE}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_CLIP, A_ID, A_BOXCOLOR, 
                          A_GRIDONTOP, A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));

      elementMap22.put( EL_PAXIS, new SchemaElementInfo( true, 
            new String[] {}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_REFANGLE, A_REVERSE, A_HIDE, 
                          A_GAP, A_FMT, A_START, A_END, A_PDIVS}, 
            new String[] {A_START, A_END, A_PDIVS} ));


      // 08aug2018: Figure element can parent any number of 2D polar plot objects.
      elementMap22.put( EL_FIGURE, new SchemaElementInfo( false, 
            new String[] {EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE, EL_IMAGE, EL_GRAPH, EL_PGRAPH, EL_GRAPH3D, EL_REF}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT, A_TITLE, 
                          A_BORDER, A_BKG, A_NOTE}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT} ));

      // 08nov2016: The "raster" element now has optional attributes "start" and "end" to set range over which data is 
      // binned to construct histogram.
      elementMap22.put( EL_RASTER, new SchemaElementInfo( false, 
            new String[] {}, 
            new String[] {A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_MODE, A_BASELINE, A_XOFF, A_HEIGHT, A_SPACER, A_NBINS, A_AVG, A_LEGEND, A_START, A_END,
                          A_SRC}, 
            new String[] {A_SRC} ));
      
      // 22nov2016: Added optional "stemmed" attribute to the "scatter3d" element.
      // 13mar2018: Added optional "dotSize" and "dotColor" attributes to the "scatter3d" element.
      elementMap22.put( EL_SCATTER3D, new SchemaElementInfo( false, 
            new String[] {EL_SYMBOL}, 
            new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_LEGEND, A_MODE, A_BASELINE, A_BKG, A_STEMMED, A_DOTSIZE, A_DOTCOLOR, A_SRC}, 
            new String[] {A_SRC} ));
      
   }
   
   /**
    * Overridden to recognize any elements added and exclude any elements removed in this schema version; else defers 
    * to the superclass implementation. Note that the <i>heatmap</i> element is deprected in v22, replaced by the new
    * <i>contour</i> element.
    */
   @Override public boolean isSupportedElementTag(String elTag)
   {
      if(EL_HEATMAP.equals(elTag)) return(false);
      return(elementMap22.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
   }

   /**
    * Overridden to provide schema element information for any element class added or revised in this schema version; 
    * for all other element classes, it defers to the superclass implementation. 
    */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
   {
      SchemaElementInfo info = (SchemaElementInfo) elementMap22.get(elTag);
      return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
   }

   /** 
    * Enforces any requirements on the children content of elements that were introduced or revised in schema version 
    * 22. For all other element classes, it defers to the superclass implementation.
    * <ul>
    * <li>The <i>pgraph</i> element has four required children, in the following order: <i>paxis</i>, <i>paxis</i>,
    * <i>zaxis</i>, and <i>legend</i>.</li>
    * </ul>
    */
   @SuppressWarnings("rawtypes")
   @Override public boolean hasRequiredChildren(BasicSchemaElement e)
   {
      String elTag = e.getTag();
      List children = e.getElementContent();
      if(EL_PGRAPH.equals(elTag))
      {
         boolean ok = children.size() >= 4;
         for(int i=0; ok && i<4; i++) 
            ok = isValidChildAtIndex(e, ((BasicSchemaElement) children.get(i)).getTag(), i);
         return(ok);
      }
      
      return(super.hasRequiredChildren(e));
   }

   /** 
    * Enforces the following requirements on elements introduced in schema version 22:
    * <ul>
    * <li>The <i>pgraph</i> element has four required children, in the following order: <i>paxis</i>, <i>paxis</i>,
    * <i>zaxis</i>, and <i>legend</i>.</li>
    * </ul>
    */
   @Override public boolean isValidChildAtIndex(BasicSchemaElement e, String childTag, int index)
   {
      String elTag = e.getTag();
      if(EL_PGRAPH.equals(elTag))
      {
         SchemaElementInfo eInfo = (SchemaElementInfo) getSchemaElementInfo(elTag);
         if(!eInfo.isChildAllowed(childTag)) return(false);
         if(EL_PAXIS.equals(childTag)) return(index >= 0 && index <= 1);
         else if(EL_ZAXIS.equals(childTag)) return(index == 2);
         else if(EL_LEGEND.equals(childTag)) return(index == 3);
         else return(index > 3);
      }

      return(super.isValidChildAtIndex(e, childTag, index));
   }
   
   /**
    * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
    * defers to the super class, with the following exceptions:
    * <ul>
    * <li>The <i>smooth</i> attribute of the new <i>contour</i> node is a boolean-valued attribute. It has the same
    * meaning as it did for the deprecated <i>heatmap</i> node.</li>
    * <li>The <i>levels</i> attribute of the <i>contour</i> node is a list of whitespace-separated numeric tokens. It
    * may also be an empty string, in which case contour levels are automatically selected.</li>
    * <li>The <i>mode</i> attribute of the <i>contour</i> node is an enumerated attribute with possible choices
    * in {@link #MODE_CONTOUR_CHOICES}.</li>
    * <li>The inherited attributes <i>strokeColor</i> and <i>fillColor</i> can now take 3 forms: 6-digit hex string
    * defining an opaque color ("RRGGBB"), 8-digit hex string in which the alpha component is explicit("AARRGGBB"), or
    * the value "none", representing fully transparent black. Similarly for the "cmapnan" attribute of a graph's color
    * axis, except that the color value must be opaque.</li>
    * <li>The <i>bkgFill</i> attribute now allows specification of an opaque, transparent or translucent solid fill.
    * When the fill is a gradient type, both color stops must be opaque.</li>
    * <li>The enumerated <i>mode</i> attribute of the <i>raster</i> now has additional choices, as listed in 
    * {@link #MODE_RASTER_CHOICES_V22}.</li>
    * <li>The <i>raster</i> element has optional float-valued attributes <i>start</i> and <i>end</i>. No range
    * restrictions on either.</li>
    * <li>The <i>scatter3d</i> element has optional boolean attribute <i>stemmed</i>.</li>
    * <li>The <i>scatter3d</i> element has optional attribute <i>dotSize</i> that is a list of 0-3 whitespace-separated
    * integer tokens. Each token is restricted to [0..10]; any missing tokens are set to 0.</li>
    * <li>The <i>scatter3d</i> element has optional attribute <i>dotColor</i> that is a list of 0-3 whitespace-separated
    * tokens, each of which must be parsable as an opaque, translucent or transparent RGB color as tested by 
    * {@link #isValidColorAttributeValue_V22()}. Any missing tokens are set to opaque black.</li>
    * <li>The attribute <i>boxColor</i> can be a 6- or 8-digit hex string for an opaque or translucent RGB color, or the
    * value "none" (transparent black).</li>
    * <li>New attributes <i>gridontop</i> and <i>reverse</i> are boolean-valued.</li>
    * <li>New attribute <i>pdivs</i> is a list of 0 or more whitespace-separated floating-point numbers.</li>
    * <li>New attribute <i>refangle</i> is an integer string restricted to [-359..359].</li>
    * <li>New <i>pgraph</i> element can have an <i>id</i> attribute like the other graph container elements. We need
    * to test the element type because <i>id</i> attribute is used by a number of different FypML elements and the 
    * requirements on its value may vary with the element class. For <i>pgraph</i>, it can be any non-null string.</li>
    * </ul>
    */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
   {
      String tag = e.getTag();
      
      if(A_STROKECOLOR.equals(attr) || A_FILLCOLOR.equals(attr) || A_CMAPNAN.equals(attr))
         return(isValidColorAttributeValue_V22(value, !A_CMAPNAN.equals(attr)));
      if(A_BKG.equals(attr)) return(isValidBkgFillAttributeValue(value));

      if(A_BOXCOLOR.equals(attr))
         return(isValidColorAttributeValue_V22(value, true));
      if(A_REVERSE.equals(attr) || A_GRIDONTOP.equals(attr)) return("true".equals(value) || "false".equals(value));
      if(A_PDIVS.equals(attr)) return(isValidFloatListAttributeValue(value, -Double.MAX_VALUE, Double.MAX_VALUE));
      if(A_REFANGLE.equals(attr)) return(isValidIntegerAttributeValue(value, -359, 359));
      
      if(EL_PGRAPH.equals(tag) && A_ID.equals(attr)) return(value != null);
      
      if(EL_CONTOUR.equals(tag))
      {
         if(A_SMOOTH.equals(attr)) return("true".equals(value) || "false".equals(value));
         if(A_MODE.equals(attr)) 
            return(isValidEnumAttributeValue(value, MODE_CONTOUR_CHOICES));
         if(A_TITLE.equals(attr)) return(value != null);
         if(A_LEVELS.equals(attr))
         {
            if(value == null) return(false);
            value = value.trim();
            return(value.isEmpty() || (Utilities.parseDoubleList(value, null, MAX_CONTOUR_LEVELS) != null));
         }
      }
      else if(EL_RASTER.equals(tag))
      {
         if(A_MODE.equals(attr)) return(isValidEnumAttributeValue(value, MODE_RASTER_CHOICES_V22));
         if(A_START.equals(attr) || A_END.equals(attr))
            return(isValidFloatAttributeValue(value, -Double.MAX_VALUE, Double.MAX_VALUE));
      }
      else if(EL_SCATTER3D.equals(tag))
      {
         if(value==null) return(false);
         
         if(A_STEMMED.equals(attr)) return("true".equals(value) || "false".equals(value));
         else if(A_DOTSIZE.equals(attr))
         {
            return(isValidIntListAttributeValue(value.trim(), 0, 3, 0, 10, true, null));
         }
         else if(A_DOTCOLOR.equals(attr))
         {
            String[] tokens = value.trim().split("\\s");
            boolean ok = tokens.length >= 0 && tokens.length <= 3;
            for(int i=0; ok && i<tokens.length; i++) ok = isValidColorAttributeValue_V22(tokens[i], true);
            return(ok);
         }
      }
      
     
      return(super.isValidAttributeValue(e, attr, value));
   }
   
   /**
    * Verify that the specified attribute value represents a list of zero or more whitespace-separated floating-point
    * numbers that all fall within the range specified.
    * @param value The test value. Null is considered equivalent to an empty string.
    * @param minVal The minimum allowed floating-pt value.
    * @param maxVal The maximum allowed floating-pt value.
    * @return True if the test value is valid, as described.
    */
   boolean isValidFloatListAttributeValue(String value, double minVal, double maxVal)
   {
      if(value == null) value =  "";
      boolean ok = true;
      try 
      {
         String[] tokens = value.split("\\s");
         for(int i=0; ok && i<tokens.length; i++) 
         {
            double d = Double.parseDouble(tokens[i]);
            ok = (d >= minVal) && (d <= maxVal);
         }
      }
      catch(NumberFormatException nfe) { ok = false; }
      return(ok);
   }

   /**
    * Support for translucent colors was introduced in schema version 22 as of app version 5.0.2. A color attribute can 
    * be specified in the old way as a 6-digit hex string representing an opaque color ("RRGGBB"), as an 8-digit hex 
    * string in which the alpha component is explicit ("AARRGGBB"), or as the value "none" (representing transparent 
    * black). This method verifies that the specified attribute value conforms to one of these 3 alternatives.
    * @param value The attribute value.
    * @param allowAlpha If true, the value can conform to any of the 3 forms. Otherwise, it must be a 6-digit hex
    * string, or an 8-digit hex string in which the alpha component is "FF".
    * @return True if value is valid for a color attribute, as described.
    */
   boolean isValidColorAttributeValue_V22(String value, boolean allowAlpha)
   {
      if(value==null) return(false);
      boolean ok = allowAlpha ? ATTRVAL_NONE.equals(value) : false;
      if(!ok) ok = value.matches(COLOR_REGEX);
      if(!ok) 
      {
         ok = value.matches(COLOR_REGEX_ARGB);
         if(ok && !allowAlpha) ok = value.substring(0,2).equalsIgnoreCase("FF");
      }
      return(ok);
   }
   
   /**
    * Validation of the background fill attribute revised to allow for translucent solid color fills. When the fill type
    * is a gradient, both color stops must be opaque (as in the prior schema version).
    */
   @Override boolean isValidBkgFillAttributeValue(String value)
   {
      if(value == null) return(false);
      String[] tokens = value.split(" ");
      boolean ok = false;
      if(tokens.length == 1) 
         ok = isValidColorAttributeValue_V22(tokens[0], true);
      else if(tokens.length == 3)
         ok = isValidIntegerAttributeValue(tokens[0], 0, 359) && isValidColorAttributeValue_V22(tokens[1], false) &&
            isValidColorAttributeValue_V22(tokens[1], false);
      else if(tokens.length == 4)
         ok = isValidIntegerAttributeValue(tokens[0], 0, 100) && isValidIntegerAttributeValue(tokens[1], 0, 100) &&
            isValidColorAttributeValue_V22(tokens[2], false) && isValidColorAttributeValue_V22(tokens[3], false);
      return(ok);
   }
   /**
    * This method handles the actual details of migrating from the previous schema version to this one. It makes the
    * following changes:
    * <ul>
    * <li>Any <i>heatmap</i> element encountered is migrated to the new <i>contour</i> element, with its display mode
    * set to "heatMap".</li>
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
         String elTag = e.getTag();
         
         
         // migrate the element object's schema info. Replace each heatmap node with a contour node. The contour node
         // has all the properties that a heatmap node did, plus the "levels" property and stroke properties, which are
         // all left implicit. The contour node's "mode" property is explicitly set to "heatMap" so that it behaves like
         // the old heatmap node.
         boolean isHeatMap = EL_HEATMAP.equals(elTag);
         e.updateSchema(this, isHeatMap ? EL_CONTOUR : null);
         if(isHeatMap)
         {
            e.setAttributeValueByName(A_MODE, MODE_CONTOUR_HEATMAP);
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
