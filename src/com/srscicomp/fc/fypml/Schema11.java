package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.srscicomp.common.util.Utilities;
import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema11</code> is the encapsulation of the <em>DataNav</em> figure model XML schema version 11. It extends 
 * <code>Schema10</code> and includes support for migrating schema version 10 documents to version 11.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul>
 *    <li>(13Jan2011) Introduced third display mode <i>trains2</i> for the <i>raster</i> element. Existing documents
 *    are unaffected by this change.</li>
 *    <li>(14Jan2011) Added boolean attribute <i>avg</i> to the <i>trace</i> element. It is optional, and its implicit
 *    value is false, meaning that the average trace should not be displayed (for <i>multitrace</i> display mode only). 
 *    During migration, we must explicitly set <i>avg==true</i> for all <i>trace</i> elements in which the display mode 
 *    is <i>multitrace</i>.</li>
 *    <li>(20Jan2011) Added boolean attribute <i>log2</i> to the <i>axis</i> element. It provides an explicit way to
 *    indicate that the axis's logarithmic base is 2 rather than 10. Default value is false (base 10). This attribute
 *    impacts how axis auto-scaling works and how tick mark locations are calculated for child tick sets. During
 *    schema migration, the attribute is set if the axis is logarithmic (this is determined by checking the parent
 *    graph's coordinate system type) AND the interval assigned to its first tick set is closer to a power of 2 than
 *    a power of 10. This is how FigureComposer decided whether a tick set was log2 or log10 prior to the introduction
 *    of this attribute.</li>
 *    <li>(31Jan2011) Added boolean attribute <i>auto</i> to the <i>graph</i> element. If set, the graph's axes (incl.
 *    the Z/color-axis are auto-scaled whenever a data presentation node is added to/removed from the parent graph, or 
 *    the dataset for an existing presentation node is modified.</li>
 * </ul>
 * </p>
 * 
 * @author 	sruffner
 */
class Schema11 extends Schema10
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema11"); }
   @Override public int getVersion() { return(11); }
   @Override public String getLastApplicationVersion() { return("3.6.0"); }

   /**
    * New display mode for the "raster" element: Similar to original "trains" mode, except that the vertical spacing of
    * individual rasters is defined differently.
    */
   final static String MODE_RASTER_TRAINS2 = "trains2";
   
   /** Revised (as of 13Jan2011) set of choices for the "raster" element's display mode (the "mode" attribute). */
   final static String[] MODE_RASTER_CHOICES_V11 = {MODE_RASTER_TRAINS, MODE_RASTER_TRAINS2, MODE_RASTER_HIST};

   /** Default for the <i>avg</i> attribute of the <i>trace</i> element. */
   final static String DEFAULT_TRACE_AVG = "false";

   /** 
    * New boolean attribute <i>log2</i> for the <i>axis</i> element: If axis is logarithmic, its base is 2 if this 
    * attribute is true; else its base is 10.
    */
   final static String A_LOG2 = "log2";
   /** Default for the <i>log2</i> attribute of the <i>axis</i> element (= "false"). */
   final static String DEFAULT_LOG2 = "false";
   
   /** Default for the <i>auto</i> attribute of the <i>graph</i> element (= "false"). */
   final static String DEFAULT_GRAPH_AUTO = "false";
   
   /**
	 * This element map contains <code>SchemaElementInfo</code> objects for each element that is new to this schema or 
    * has a different attribute set compared to the previous schema.
	 */
	private static Map<String, SchemaElementInfo> elementMap11 = null;

	static
	{
		elementMap11 = new HashMap<String, SchemaElementInfo>();
		
		// (14jan2011) Added optional attribute "avg" to the "trace" element.
      elementMap11.put( EL_TRACE, new SchemaElementInfo( false, 
               new String[] {EL_SYMBOL, EL_EBAR}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP,
                             A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_TITLE, A_LEGEND, A_SKIP, A_MODE, A_BARWIDTH, 
                             A_BASELINE, A_FILLED, A_AVG, A_XOFF, A_YOFF, A_SRC}, 
               new String[] {A_SRC} ));

      // (20jan2011) Added optional attribute "log2" to the "axis" element.
      elementMap11.put( EL_AXIS, new SchemaElementInfo( false, 
               new String[] {EL_TICKS}, 
               new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, 
                             A_STROKECAP, A_STROKEJOIN, A_STROKECOLOR, A_HIDE, A_UNITS, A_TITLE, A_SPACER, 
                             A_LABELOFFSET, A_START, A_END, A_LOG2}, 
               new String[] {A_START, A_END} ));
      
      // (31jan2011) Added optional attribute "auto" to the "graph" element.
      elementMap11.put( EL_GRAPH, new SchemaElementInfo( false, 
            new String[] {EL_AXIS, EL_ZAXIS, EL_GRIDLINE, EL_LEGEND, EL_CALIB, EL_LABEL, EL_LINE, EL_SHAPE, EL_GRAPH, 
                          EL_FUNCTION, EL_TRACE, EL_RASTER, EL_HEATMAP}, 
            new String[] {A_FONT, A_PSFONT_V7, A_ALTFONT, A_FONTSIZE, A_FONTSTYLE, A_FILLCOLOR, A_STROKEWIDTH, A_STROKECAP, 
                          A_STROKEJOIN, A_STROKECOLOR, A_STROKEPAT, A_ROTATE, A_TITLE, A_TYPE, A_LAYOUT, A_CLIP, A_AUTO,
                          A_LOC, A_WIDTH, A_HEIGHT}, 
            new String[] {A_LOC, A_WIDTH, A_HEIGHT} ));
	}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in schema version 11; otherwise defers 
    * to the superclass implementation.
    * @see ISchema#isSupportedElementTag(String)
    */
   @Override
   public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap11.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in schema version 10; for 
	 * all other element classes, it defers to the superclass implementation. 
	 * @see ISchema#getSchemaElementInfo(String)
	 */
   @Override
	public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = (SchemaElementInfo) elementMap11.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
    *    <li>The "mode" enumerated attribute on the "raster" element admits two additional choices as of 13Jan2011. The 
    *    full set of choices are in <code>MODE_RASTER_CHOICES_V11</code>.</li>
    *    <li>The "trace" element's new "avg" attribute is boolean; this is already checked by superclass 
    *    <code>Schema10</code>, so it is NOT checked here.</li>
    *    <li>The new "log2" attribute is boolean. Method verifies its value is "true" or "false".</li>
    *    <li>The "auto" attribute for "graph" element is boolean; this is already checked by superclass 
    *    <code>BaseSchema</code>, so it is NOT checked here.</li>
	 * </ul>
	 * @see ISchema#isValidAttributeValue(BasicSchemaElement,String,String)
	 */
   @Override
	public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      if(e.getTag().equals(EL_RASTER) && A_MODE.equals(attr)) 
         return(isValidEnumAttributeValue(value, MODE_RASTER_CHOICES_V11));
      if(A_LOG2.equals(attr)) return("true".equals(value) || "false".equals(value));
      
      return(super.isValidAttributeValue(e, attr, value));
	}
   
   
   /**
	 * This method handles the actual details of migrating <code>Schema10</code> content to <code>Schema11</code>. It 
    * makes the following changes:
	 * <ul>
    *    <li>For every <i>trace</i> element encountered, if the display mode is <i>multitrace</i>, the new <i>avg</i> 
    *    attribute MUST be explicitly set to true. Otherwise, it can be left implicit.</li>
    *    <li>For every <i>axis</i> element encountered, the new <i>log2</i> attribute may need to be explicitly set
    *    to <i>true</i> to ensure the existing figure is rendered in the same way. We set the flag only if (1) the axis
    *    is logarithmic, (2) it has at least one tick set, (3) the specified tick interval for the first tick set is
    *    closer to a positive power of 2 than it is to a positive power of 10. Step 3 is how FigureComposer decided
    *    whether to prepare a log2 vs a log10 tick set prior to introduction of the <i>log2</i> attribute.</li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
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
         
         // migrate the element object's schema info.
         e.updateSchema(this, null);

         // explicitly set avg==true on trace elements iff the display mode is multitrace.
         if(EL_TRACE.equals(elTag))
         {
            String mode = e.getAttributeValueByName(A_MODE);
            if(DISPMODE_MULTI.equals(mode)) e.setAttributeValueByName(A_AVG, "true");
         }
         
         // explicitly set log2==true on axis elements in certain situations
         if(EL_AXIS.equals(elTag)) migrateLog2Attr(e);
         
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
	
	private void migrateLog2Attr(BasicSchemaElement axis) throws XMLException
	{
	   BasicSchemaElement graph = (BasicSchemaElement) axis.getParent();
	   boolean isPrimary = (graph.getChildAt(0) == axis);
	   
	   String coordSys = graph.getAttributeValueByName(A_TYPE);
	   if(GRAPH_TYPE_LOGLOG.equals(coordSys) || (isPrimary && GRAPH_TYPE_SEMILOGX.equals(coordSys)) || 
	            (!isPrimary && (GRAPH_TYPE_SEMILOGY.equals(coordSys) || GRAPH_TYPE_SEMILOGR.equals(coordSys))))
	   {
	      if(axis.getChildCount() == 0) return;
	      BasicSchemaElement ticks = (BasicSchemaElement) axis.getChildAt(0);
	      double intv = 0;
	      try { intv = Double.parseDouble(ticks.getAttributeValueByName(A_INTV)); }
	      catch(NumberFormatException nfe) { return; }
	      
	      intv = Math.abs(intv);
	      if(intv == 0) return;
	      double exp = Math.floor(Utilities.log2(intv));
	      double validIntv2 = Math.pow(2, exp < 1 ? 1 : exp);
	      exp = Math.floor(Utilities.log10(intv));
	      double validIntv10 = Math.pow(10, exp < 1 ? 1 : exp);
	      if(Math.abs(intv-validIntv2) < Math.abs(intv-validIntv10))
	         axis.setAttributeValueByName(A_LOG2, "true");
	   }
	}
}
