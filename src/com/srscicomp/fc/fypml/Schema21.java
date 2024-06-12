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
 * <b>Schema21</b> is the encapsulation of the <i>DataNav</i> figure model XML schema version 21. It extends {@link 
 * Schema20} and includes support for migrating schema version 20 documents to version 21. This version accompanies
 * version 5.0 of the <i>DataNav</i> application suite, which introduces a major new graphic object -- the 3D graph.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul> 
 *    <li>(Jan2016, for app V5.0.0) Introduced new schema elements to represent a 3D graph node: <i>graph3d</i> defines
 *    the 3D graph itself, <i>axis</i> is "re-used" to represent each of the 3 axes (X, Y, Z), <i>gridline</i> is 
 *    "re-used" to control the appearance of grid lines perpendicular to each of the axes (X, Y, Z), and new element
 *    <i>back3d</i> controls the appearance of the 3D graph's back planes (XY, XZ, YZ). The <i>graph3d</i> has NINE
 *    required component nodes, which appear first in its child list: 3 <i>axis</i> elements, then 3 <i>gridline</i>
 *    elements, then 3 <i>back3d</i> elements. Any optional children follow these component nodes. <b>Note that the
 *    "units" attribute of the <i>axis</i> element and the "hide" attribute of the <i>gridline</i> element do not apply 
 *    in the 3D graph context.</li>
 *    <li>(Jan2016, for app V5.0.0) To support logarithmic 3D plots, user can configure any 3D axis to be logarithmic.
 *    (This is different from the 2D case, where the linear-vs-log scaling is determined by the 2D graph type.) Added
 *    a new "log" attribute to the <i>axis</i> element to encode this feature. It applies only to a 3D axis, not a
 *    2D axis. Default value is "false".</li>
 *    <li>(Jan2016, for app V5.0.0) Updated <i>graph3d</i> to allow any number of these 2D graphic objects as children:
 *    text labels or text boxes, line segments, shapes, and images.</li>
 *    <li>(Jan2016, for app V5.0.0) The <i>ticks</i> child of an <i>axis</i> represents a tick set defined on that axis,
 *    whether it is in a 2D or 3D graph. During initial development, we experimented with a significantly different
 *    definition for a 3D tick set, but that was dropped.</li>
 *    <li>(Feb2016, for app V5.0.0) Introduced new schema element <i>scatter3d</i> representing a 3D scatter plot.</li>
 *    <li>(Feb2016, for app V5.0.0) Introduced new schema element <i>surface</i> representing a 3D surface plot. New
 *    integer attribute <i>limit</i> specifies the mesh size limit for the surface. Reuse <i>cmap</i> as a boolean
 *    attribute indicating whether surface is color-mapped or painted a single color.</li>
 *    <li>(Feb2016, for app V5.0.0) Added an additional backdrop style to <i>graph3d</i> element: in the "xyPlane" 
 *    style, only the XY backplane is drawn, along with all 3 axes.</li>
 *    <li>(Feb2016, for app V5.0.0) Revamped <i>scatter3d</i> element to support stem plots, as well as all the display
 *    modes defined for the 2D scatter plot. Added a required "symbol" child to govern appearance of the marker symbols,
 *    although its "fillColor" and "title" attributes are ignored: the "bkg" attribute on the "scatter3d" node allows
 *    for gradient as well as solid fills; and a symbol character is NOT supported. The scatter plot's own stroke 
 *    styles govern the appearance of the stem lines. Reused <i>baseline</i> attribute to specify Z value for the stem 
 *    base plane.</li>
 *    <li>(Mar2016, for app V5.0.0) Added "cmap" attribute to <i>graph3d</i>. Also added <i>legend</i> as a required
 *    component of the 3D graph.</li>
 * </ul>
 * </p>
 * 
 * @author  sruffner
 */
class Schema21 extends Schema20
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema21"); }
   @Override public int getVersion() { return(21); }
   @Override public String getLastApplicationVersion() { return("5.0.0"); }

   /** A major new graphic object -- the 3D graph node. */
   final static String EL_GRAPH3D = "graph3d";
   
   /** Default value for the {@link #A_ROTATE} attribute of a 3D graph. */
   final static String DEFAULT_GRAPH3D_ROTATE = "30";
   
   /** 
    * New attribute: The elevation angle for the 3D graph node (rotation about X-axis), in degrees. A float-valued 
    * string restricted to [-60..60] degrees. Positive angles correspond to clockwise rotations.
    */
   final static String A_ELEVATE = "elevate";
   /** Default value for the {@link #A_ELEVATE} attribute of a 3D graph. */
   final static String DEFAULT_ELEVATE = "15";
   /** Minimum allowed value for the 3D graph's elevation angle, in degrees. */
   final static double MIN_ELEVATE = -60;
   /** Maximum allowed value for the 3D graph's elevation angle, in degrees. */
   final static double MAX_ELEVATE = 60;

   /** New attribute: The backdrop style for the 3D graph node. Enumerated attribute. */
   final static String A_BACKDROP = "backdrop";
   /** 3D graph backdrop style: completely hidden (no back planes, grid lines or axes drawn). */
   final static String BACKDROP_HIDDEN = "hidden";
   /** 3D graph backdrop style: full 3D box with back planes, grid lines and axes. */
   final static String BACKDROP_BOX3D = "box3D";
   /** 3D graph backdrop style: open 3D box -- edges joining at front-most corner are not drawn. */
   final static String BACKDROP_OPENBOX3D = "openBox3D";
   /** 3D graph backdrop style: open 3D box -- edges joining at front-most corner are not drawn. */
   final static String BACKDROP_XYPLANE = "xyPlane";
   /** 3D graph backdrop style: axes drawn on outer edges of 3D box, but no back planes or grid lines. */
   final static String BACKDROP_AXESOUTER = "axesOuter";
   /** 3D graph backdrop style: axes emanate from back corner of 3D box, but no back planes or grid lines. */
   final static String BACKDROP_AXESBACK = "axesBack";
   /** Set of choices for the 3D graph's backdrop style. */
   final static String[] BACKDROP_CHOICES = {
      BACKDROP_HIDDEN, BACKDROP_BOX3D, BACKDROP_OPENBOX3D, BACKDROP_XYPLANE, BACKDROP_AXESOUTER, BACKDROP_AXESBACK
   };
   /** Default value for the {@link #A_BACKDROP} attribute of a 3D graph. */
   final static String DEFAULT_BACKDROP = BACKDROP_BOX3D;
   
   
   /** New attribute: The projection distance scale factor for the 3D graph node. Integer attribute in [2..20]. */
   final static String A_PSCALE = "pscale";
   /** Default value for the {@link #A_PSCALE} attribute of a 3D graph. */
   final static String DEFAULT_PSCALE = "5";
   /** Minimum allowed value for the projection distance scale factor of a 3D graph node. */
   final static int MIN_PSCALE = 2;
   /** Minimum allowed value for the projection distance scale factor of a 3D graph node. */
   final static int MAX_PSCALE = 20;
   
   /** New attribute: The depth dimension for the 3D graph node (extent in the Z direction). Measured attribute. */
   final static String A_DEPTH = "depth";
   
   /** 
    * The 3D back plane node -- controls appearance of one of 3 back planes of a 3D graph when it's configured in one 
    * of two possible boxed backdrop styles.
    */
   final static String EL_BACK3D = "back3d";
   
   /** 
    * New boolean attribute for the <i>axis</i> element. <b>Applicable in the 3D context only.</b> If set, the 3D axis
    * and the corresponding dimension in the parent 3D graph are scaled logarithmically; else linearly.
    */
   final static String A_LOG = "log";
   /** Default value for the {@link #A_LOG} attribute of a 3D graph axis. */
   final static String DEFAULT_LOG = "false";
   
   /** A 3D scatter plot element. */
   final static String EL_SCATTER3D = "scatter3d";
   
   /** A 3D surface plot element. */
   final static String EL_SURFACE = "surface";
   /** New attribute: The mesh size limit for a 3D surface plot. */
   final static String A_LIMIT = "limit";
   /** Default value for the {@link #A_LIMIT} attribute of a 3D surface plot. */
   final static String DEFAULT_LIMIT = "100";
   /** Minimum allowed value for the mesh size limit of a 3D surface plot. */
   final static int MIN_LIMIT = 25;
   /** Maximum allowed value for the mesh size limit of a 3D surface plot. */
   final static int MAX_LIMIT = 500;
   /** Default value for the {@link #A_CMAP} attribute of a 3D surface plot (a boolean-valued attribute). */
   final static String DEFAULT_SURFACE_CMAP = "true";
   
   /**
    * This element map contains {@link SchemaElementInfo SchemaElementInfo} objects for each element that
    * is new to this schema or has a different attribute set compared to the previous schema.
    */
   private static Map<String, SchemaElementInfo> elementMap21 = null;

   static
   {
      elementMap21 = new HashMap<String, SchemaElementInfo>();
      
      // (12jan2016) Figure element can parent any number of 3D graph objects.
      elementMap21.put( EL_FIGURE, new SchemaElementInfo( false, 
            new String[] {EL_LABEL, EL_TEXTBOX, EL_LINE, EL_SHAPE, EL_IMAGE, EL_GRAPH, EL_GRAPH3D, EL_REF}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT, A_TITLE, 
                          A_BORDER, A_BKG, A_NOTE}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_LOC, A_WIDTH, A_HEIGHT} ));

      // (13jan2016) New element - 3D graph object.
      // (03mar2016) Added "cmap" property
      // (04mar2016) Added "legend" as required component
      elementMap21.put( EL_GRAPH3D, new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_GRIDLINE, EL_BACK3D, EL_LEGEND, EL_LABEL, EL_LINE, EL_SHAPE, EL_TEXTBOX, EL_IMAGE,
                          EL_SCATTER3D, EL_SURFACE}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, 
                          A_ROTATE, A_ELEVATE, A_TITLE, A_ID, A_BACKDROP, A_PSCALE, A_CMAP,
                          A_LOC, A_WIDTH, A_HEIGHT, A_DEPTH}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT, A_DEPTH} ));

      // (13jan2016) New element - back plane component of a 3D graph node. 
      elementMap21.put( EL_BACK3D, new SchemaElementInfo( false, 
            new String[] {}, 
            new String[] {A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT}, 
            new String[] {} ));

      // (13jan2016) Axis element revised to allow both ticks and ticks3d elements, since we're using it in both 2D
      // and 3D graph contexts.
      // (21jan2016) Added "log" attribute to indicate whether or not 3D axis and the corresponding graph dimension are
      // scaled logarithmically or linearly. Ignored in a 2D context. Default value = "false".
      // (26jan2016) Dropped the "ticks3d" element. 3D tick sets defined exactly the same as 2D tick sets.
      elementMap21.put( EL_AXIS, new SchemaElementInfo( false, 
            new String[] {EL_TICKS}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                          A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_HIDE, A_UNITS, A_TITLE, A_SPACER, 
                          A_LABELOFFSET, A_START, A_END, A_LOG2, A_LINEHT, A_LOG}, 
            new String[] {A_START, A_END} ));
      
      // (03feb2016) A new data presentation element, the 3D scatter plot.
      // (15feb2016) Completely revised element to support stem plots also. Added optional "baseline" property to set 
      // the Z value for the stem base plane. Added a (required) "symbol" child to control appearance of scatter plot 
      // symbols separately from stem lines. However, a symbol "character" is not supported in this plot, and the 
      // symbols may be filled with a gradient as specified by the node's "bkg" attribute, which overrides the symbol
      // node's fill color.
      elementMap21.put( EL_SCATTER3D, new SchemaElementInfo( false, 
            new String[] {EL_SYMBOL}, 
            new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, 
                          A_LEGEND, A_MODE, A_BASELINE, A_BKG, A_SRC}, 
            new String[] {A_SRC} ));
      
      // (11feb2016) A new data presentation element, the 3D surface plot. 
      elementMap21.put( EL_SURFACE, new SchemaElementInfo( false, 
            new String[] {}, 
            new String[] {A_STROKEWIDTH, A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_FILLCOLOR, A_TITLE, 
                          A_LIMIT, A_CMAP, A_SRC}, 
            new String[] {A_SRC} ));
      
   }
   
   /**
    * Overridden to recognize any elements added and exclude any elements removed in this schema version; else defers 
    * to the superclass implementation.
    */
   @Override public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap21.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
   }

   /**
    * Overridden to provide schema element information for any element class added or revised in this schema version; 
    * for all other element classes, it defers to the superclass implementation. 
    */
   @Override public SchemaElementInfo getSchemaElementInfo(String elTag)
   {
      SchemaElementInfo info = (SchemaElementInfo) elementMap21.get(elTag);
      return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
   }

   /** 
    * Enforces any requirements on the children content of elements that were introduced or revised in schema version 
    * 21. For all other element classes, it defers to the superclass implementation.
    * <ul>
    *    <li>The <i>graph3d</i> element now requires 10 children in this order: 3 <i>axis</i>, 3 <i>gridline</i>, 
    *    3 <i>back3d</i>, and one <i>legend</i>.</li>
    *    <li>The <i>scatter3d</i> element has a single required child, a <i>symbol</i> element, at index 0.</li>
    * </ul>
    */
   @SuppressWarnings("rawtypes")
   @Override public boolean hasRequiredChildren(BasicSchemaElement e)
   {
      String elTag = e.getTag();
      List children = e.getElementContent();
      if(EL_SCATTER3D.equals(elTag))
         return(children.size() == 1 && isValidChildAtIndex(e, ((BasicSchemaElement) children.get(0)).getTag(), 0));
      else if(EL_GRAPH3D.equals(elTag))
      {
         boolean ok = children.size() >= 10;
         for(int i=0; ok && i<10; i++) 
            ok = isValidChildAtIndex(e, ((BasicSchemaElement) children.get(i)).getTag(), i);
         return(ok);
      }
      
      return(super.hasRequiredChildren(e));
   }

   /** 
    * Enforces the following requirements on elements introduced in schema version 21:
    * <ul>
    * <li>The <i>scatter3d</i> element have one and only one child, a <i>symbol</i> node.</li>
    * <li>The <i>graph3d</i> element has 10 required component nodes, in order: 3 <i>axis</i> nodes, 3 <i>gridline</i>
    * nodes, 3 <i>back3d</i> nodes, and a <i>legend</i>.</li>
    * </ul>
    */
   @Override public boolean isValidChildAtIndex(BasicSchemaElement e, String childTag, int index)
   {
      String elTag = e.getTag();
      if(EL_SCATTER3D.equals(elTag)) 
         return(EL_SYMBOL.equals(childTag) && index == 0);
      else if(EL_GRAPH3D.equals(elTag))
      {
         SchemaElementInfo eInfo = (SchemaElementInfo) getSchemaElementInfo(elTag);
         if(!eInfo.isChildAllowed(childTag)) return(false);
         if(EL_AXIS.equals(childTag)) return(index >=0 && index <= 2);
         else if(EL_GRIDLINE.equals(childTag)) return(index >= 3 && index <= 5);
         else if(EL_BACK3D.equals(childTag)) return(index >= 6 && index <= 8);
         else if(EL_LEGEND.equals(childTag)) return(index == 9);
         else return(index > 9);
      }

      return(super.isValidChildAtIndex(e, childTag, index));
   }
   
   /**
    * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
    * defers to the super class, with the following exceptions:
    * <ul>
    * <li>The new <i>elevate</i> attribute of the <i>graph3d</i> node is a float-valued attribute restricted to the
    * range [{@link #MIN_ELEVATE}, {@link #MAX_ELEVATE}].</li>
    * <li>The new <i>backdrop</i> attribute of the <i>graph3d</i> node is an enumerated attribute with possible choices
    * in {@link #BACKDROP_CHOICES}.</li>
    * <li>The new <i>pscale</i> attribute of the <i>graph3d</i> node is an int-valued attribute restricted to the
    * range [{@link #MIN_PSCALE}, {@link #MAX_PSCALE}].</li>
    * <li>The three dimension attributes (<i>width, height, depth</i>) for a <i>graph3d</i> node must be measured
    * attributes that only use physical units and are not negative.</li>
    * <li>The <i>loc</i> attribute of a <i>graph3d</i> node must be a measured point attribute value that allows all 
    * units except "user" units.</li>
    * <li>The <i>title</i> and <i>id</i> attributes on a <i>graph3d</i> node can have any value, as long as it is not 
    * null.</li>
    * <li>The <i>cmap</i> attribute of a <i>graph3d</i> node is an enumerated attribute with possible choices in {@link 
    * #CMAP_CHOICES_V18}.</li>
    * <li>The new <i>log</i> attribute must be boolean-valued.</li>
    * <li>The <i>mode</i> attribute of the new <i>scatter3d</i> element is an enumerated attribute with the same choices
    * as for the 2D scatter plot display mode: {@link #MODE_SCATTER_CHOICES}.</li>
    * <li>The <i>bkg</i> attribute of the new <i>scatter3d</i> element defines any of 3 types of background fills. See
    * {@link #isValidBkgFillAttributeValue()}.</li>
    * <li>The <i>baseline</i> attribute of a <i>scatter3d</i> element is an unrestricted floating-point value.</li>
    * <li>The <i>limit</i> attribute for the new <i>surface</i> element is an integer-valued attribute restricted to the
    * range [{@link #MIN_LIMIT}, {@link #MAX_LIMIT}].</li>
    * <li>The existing <i>cmap</i> attribute is "reused" as a boolean attribute for the <i>surface</i> element.</li>
    * </ul>
    */
   @Override public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
   {
      String tag = e.getTag();
      
      if(EL_GRAPH3D.equals(tag))
      {
         if(A_ELEVATE.equals(attr)) return(isValidFloatAttributeValue(value, MIN_ELEVATE, MAX_ELEVATE));
         if(A_BACKDROP.equals(attr)) 
            return(isValidEnumAttributeValue(value, BACKDROP_CHOICES));
         if(A_PSCALE.equals(attr)) return(isValidIntegerAttributeValue(value, MIN_PSCALE, MAX_PSCALE));
         if(A_WIDTH.equals(attr) || A_HEIGHT.equals(attr) || A_DEPTH.equals(attr))
            return(isValidMeasureAttributeValue(value, false, false));
         if(A_LOC.equals(attr)) return(this.isValidMeasurePointAttributeValue(value, true, false));
         if(A_ID.equals(attr) || A_TITLE.equals(attr)) return(value != null);
         if(A_CMAP.equals(attr)) return(isValidEnumAttributeValue(value, CMAP_CHOICES_V18));
      }
      
      if(EL_SCATTER3D.equals(tag))
      {
         if(A_MODE.equals(attr)) return(isValidEnumAttributeValue(value, MODE_SCATTER_CHOICES));
         if(A_BKG.equals(attr)) return(isValidBkgFillAttributeValue(value));
         if(A_BASELINE.equals(attr)) return(isValidFloatAttributeValue(value, -Double.MAX_VALUE, Double.MAX_VALUE));
      }
      
      if(A_LOG.equals(attr)) return("true".equals(value) || "false".equals(value));
      
      if(EL_SURFACE.equals(tag))
      {
         if(A_LIMIT.equals(attr)) return(isValidIntegerAttributeValue(value, MIN_LIMIT, MAX_LIMIT));
         if(A_CMAP.equals(attr)) return("true".equals(value) || "false".equals(value));
      }
     
      return(super.isValidAttributeValue(e, attr, value));
   }
   
   /**
    * This method handles the actual details of migrating from the previous schema version to this one. It makes the
    * following changes:
    * <ul>
    * <li><i>No changes are needed. However, since we're migrating "in place", we have to replace all references to the 
    * previous schema with the current schema!</i> </li>
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
