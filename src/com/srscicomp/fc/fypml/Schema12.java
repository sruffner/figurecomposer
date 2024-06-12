package com.srscicomp.fc.fypml;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import com.srscicomp.common.util.Utilities;
import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.SchemaElementInfo;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema12</code> is the encapsulation of the <em>DataNav</em> figure model XML schema version 12. It extends 
 * <code>Schema11</code> and includes support for migrating schema version 11 documents to version 12.
 * 
 * <p><i>Summary of schema changes</i>.
 * <ul>
 *    <li>(27Apr2011) The constraints on the <i>loc</i> attribute of a <i>graph</i> node were changed: percentage ("%") 
 *    units are no longer allowed. During schema migration, if the measured x- or y-coordinate is specified in "%" 
 *    units, that coordinate is converted to inches. User units are still permitted.</li>
 *    <li>(27Apr2011) The constraints on the <i>width, height</i> attributes of a <i>graph</i> node were changed: 
 *    percentage ("%") and user ("u") units are no longer allowed. NOTE that while user units were allowed in previous
 *    versions, a graph defined as such was NEVER rendered, so it made no sense to specify a graph's size in user units.
 *    During schema migration, if the measured width or height is specified in "%" units, that coordinate is converted 
 *    to inches. If it is specified in "u" units, it is changed to inches and set equal to 1/4 the length of the 
 *    corresponding dimension in the parent node.</li>
 * </ul>
 * </p>
 * 
 * @author 	sruffner
 */
class Schema12 extends Schema11
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/datanav/namespace/schema12"); }
   @Override public int getVersion() { return(12); }
   @Override public String getLastApplicationVersion() { return("4.0.3"); }

   /**
	 * This element map contains <code>SchemaElementInfo</code> objects for each element that is new to this schema or 
    * has a different attribute set compared to the previous schema. As of 27Apr2011, this map is EMPTY, as there are
    * no changes in the attributes sets for each element in the schema.
	 */
	private static Map<String, SchemaElementInfo> elementMap12 = null;

	static
	{
		elementMap12 = new HashMap<String, SchemaElementInfo>();
	}

   /**
    * Overridden to recognize any elements added and exclude any elements removed in schema version 12; otherwise defers 
    * to the superclass implementation.
    * @see ISchema#isSupportedElementTag(String)
    */
   @Override
   public boolean isSupportedElementTag(String elTag)
   {
      return(elementMap12.containsKey(elTag) ? true : super.isSupportedElementTag(elTag));
   }

	/**
	 * Overridden to provide schema element information for any element class added or revised in schema version 10; for 
	 * all other element classes, it defers to the superclass implementation. 
	 * @see ISchema#getSchemaElementInfo(String)
	 */
   @Override
	public SchemaElementInfo getSchemaElementInfo(String elTag)
	{
		SchemaElementInfo info = (SchemaElementInfo) elementMap12.get(elTag);
		return( (info==null) ? super.getSchemaElementInfo(elTag) : info);
	}

	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to the super class, with the following exceptions:
	 * <ul>
    *    <li>The <i>width, height</i> measure attributes on the "graph" element must not be in "%" or "u" units.</li>
    *    <li>The measured coordinates within the <i>loc</i> attribute of a "graph" must not be in "%" units.</li>
	 * </ul>
	 * @see ISchema#isValidAttributeValue(BasicSchemaElement,String,String)
	 */
   @Override
	public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
      if(e.getTag().equals(EL_GRAPH))
      {
         if(A_WIDTH.equals(attr) || A_HEIGHT.equals(attr))
            return(isValidMeasureAttributeValue(value, false, false, false));
         else if(A_LOC.equals(attr))
            return(this.isValidMeasurePointAttributeValue(value, false, true));
      }
      
      return(super.isValidAttributeValue(e, attr, value));
	}
   
   /**
    * Verify that the specified value is valid for a measure attribute. This version can separately disallow the "%" and
    * "u" unit tokens, so that we can validate the location coordinates, width and size of a graph node in this and
    * future schemas.  
    * @param value The test value.
    * @param allowNeg True if the measure can be negative.
    * @param allowPct True if percentage units ("%" token) are allowed.
    * @param allowUser True if user units ("u" token) are allowed.
    * @return True if the test value is valid for a measure attribute, as described.
    */
   boolean isValidMeasureAttributeValue(String value, boolean allowNeg, boolean allowPct, boolean allowUser)
   {
      boolean ok = (value!=null) && value.matches(MEASURE_REGEX);
      if(ok && !allowPct) ok = !value.endsWith( "%" );
      if(ok && !allowUser) ok = !value.endsWith( "u" );
      if(ok && !allowNeg) ok = !(value.trim().indexOf('-') == 0);
      return(ok);
   }

   /**
    * Verify that the specified value is valid for a measured point attribute. A measured point attribute is simply a 
    * pair of measure attributes, separated by whitespace, representing the (x,y) coordinates of a point. Each 
    * coordinate must satisfy the format restrictions of a measure attribute. This version can separately disallow the 
    * "%" and "u" unit tokens, so that we can validate the "loc" attribute of a graph node in this and future schemas.  
    * 
    * @param value The test value.
    * @param allowPct True if percentage units ("%" token) are allowed on each coordinate in the measured point.
    * @param allowUser True if user units ("u" token) are allowed on each coordinate in the measured point.
    * @return True if the test value is valid for a measured point attribute, as described.
    */
   boolean isValidMeasurePointAttributeValue(String value, boolean allowPct, boolean allowUser)
   {
      StringTokenizer st = new StringTokenizer(value);
      if(st.countTokens() != 2) return(false);

      String xStr = st.nextToken();
      String yStr = st.nextToken();

      return(isValidMeasureAttributeValue(xStr, true, allowPct, allowUser) 
            && isValidMeasureAttributeValue(yStr, true, allowPct, allowUser));
   }
   
   /**
	 * This method handles the actual details of migrating <code>Schema11</code> content to <code>Schema12</code>. It 
    * makes the following changes:
	 * <ul>
    *    <li>For every <i>graph</i> element encountered: if its width, height, or either coordinate of its location
    *    attribute is expressed in "%" units, that measure is converted to the equivalent in inches WRT the parent
    *    viewport size. Furthermore, if the width or height is in "u" units (unlikely, since the graph would not render
    *    in this case), it is set to 1/4 the width or height of its parent in "in". This means that as we traverse the 
    *    model, we have to keep track of the current parent viewport dimensions in inches (remember that a graph can be
    *    a child of either the figure OR another graph!).</li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
	 */
	public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
	{
		if(oldSchema.getVersion() != getVersion() - 1) 
			throw new XMLException("A schema instance can only migrate from the previous version.");

		// this stack keeps track of the size of the parent viewport for the next element popped off the element stack.
		// We only care about this for figure and graph node parents; for all other elements that have children, we just
		// push a null onto the stack. Note that we need to push one on for each child pused onto the element stack, or
		// things will get screwed up!. The stack elements are either null or a two-element array [w h], with dimensions
		// expressed in inches.
		Stack<double[]> parentSizeStack = new Stack<double[]>();
		parentSizeStack.push(null);
		
      // update the content of the old schema in place...
      Stack<BasicSchemaElement> elementStack = new Stack<BasicSchemaElement>();
      elementStack.push((BasicSchemaElement) oldSchema.getRootElement());
      while(!elementStack.isEmpty())
      {
         BasicSchemaElement e = elementStack.pop();
         String elTag = e.getTag();
         
         // get parent viewport size applicable to the element. Will be null if parent is not a figure or graph.
         double[] vpSize = parentSizeStack.isEmpty() ? null : parentSizeStack.pop();
         
         // migrate the element object's schema info.
         e.updateSchema(this, null);

         // if current element is a figure, then compute its viewport size now
         if(EL_FIGURE.equals(elTag))
         {
            vpSize = new double[2];
            vpSize[0] = measureToInches(e.getAttributeValueByName(A_WIDTH), false);
            vpSize[1] = measureToInches(e.getAttributeValueByName(A_HEIGHT), false);
         }
         
         // for each graph node, convert any width, height or location coordinate expressed in "%" units. We also MUST
         // compute the graph's viewport size in inches if the graph has any subgraphs!
         if(EL_GRAPH.equals(elTag))
            vpSize = fixGraphNode(e, vpSize);

         
         // if the current element is neither figure nor graph, then we don't care about the viewport size
         if(!(EL_FIGURE.equals(elTag) || EL_GRAPH.equals(elTag))) vpSize = null;
         
         // if element has any children, push them onto the stack so that we check them as well! We don't push on any 
         // child that was added during migration of the parent, since it will already conform to current schema.
         for(int i=0; i<e.getChildCount(); i++)
         {
            BasicSchemaElement child = (BasicSchemaElement) e.getChildAt(i);
            elementStack.push(child);
            
            // need to push a copy of the parent viewport size onto its respective stack as well -- for each child!
            parentSizeStack.push(vpSize);
         }
     }
      
      // the content model now conforms to this schema. We get the root element from the old schema and install it as 
      // the root of this schema, then empty the old schema. We also remember the original schema version of the 
      // migrated content.
      originalVersion = oldSchema.getOriginalVersion();
      setRootElement(oldSchema.getRootElement(), false);
      oldSchema.setRootElement(null, false);
	}
	
	/**
	 * Convert a measure attribute in string form to its equivalent value in inches. For measures in physical units only.
	 * @param m The measure attribute value (eg: "2.5cm").
	 * @param neg True if negative measures are allowed -- for validating the attribute value
	 * @return The measured length in inches.
	 * @throws XMLException if the attribute value is not a valid DataNav measure.
	 */
	private double measureToInches(String m, boolean neg) throws XMLException
	{
	   if(!isValidMeasureAttributeValue(m, neg, false, false))
	      throw new XMLException("Cannot convert measure attribute to inches -- attribute value invalid!");
	   
	   double res = getMeasureFor(m);
	   if(m.indexOf("pt") > 0) res *= PT2IN;
	   else if(m.indexOf("cm") > 0) res *= MM2IN*10;
	   else if(m.indexOf("mm") > 0) res *= MM2IN;
      
	   return(res);
	}
	
	/**
	 * Fix the <i>loc</i>, <i>width</i>, and <i>height</i> attributes of a Schema11 graph node to conform to the new
	 * constraints imposed in Schema12. If either x- or y-coordinate in the <i>loc</i> attribute is in "%" units, it is
	 * converted to inches. If either <i>width</i> or <i>height</i> is in "%" or "u" units, it is converted to inches. In
	 * the case of "u" units, the dimension is set to 1/4 the size of the corresponding dimension in the parent viewport.
	 * 
	 * @param graph The graph node.
	 * @param vpSize A two-element array [w h] holding the width and height of the parent viewport, in inches.
	 * @return A two-element array holding the width and height of the graph node's viewport, in inches.
	 * @throws XMLException if specified graph node is missing or has an invalid (IAW the prior schema, that is) 
	 * <i>loc</i>, <i>width</i>, or <i>height</i> attribute.
	 */
	private double[] fixGraphNode(BasicSchemaElement graph, double[] vpSize) throws XMLException
	{
	   double w = 0;
	   String wm = graph.getAttributeValueByName(A_WIDTH);
	   if(!isValidMeasureAttributeValue(wm, false, true, true))
	      throw new XMLException("Graph width attribute is not a valid measure!");
	   if(wm.indexOf("%") > 0 || wm.indexOf("u") > 0)
	   {
	      // convert to inches, update attribute value accordingly. Preserve value to nearest 0.001 inch.
	      if(vpSize == null || vpSize.length != 2) 
	         throw new XMLException("Cannot convert graph width from relative units to inches!");
	      
	      if(wm.indexOf("%") > 0) w = getMeasureFor(wm) * vpSize[0] / 100.0;
	      else w = 0.25 * vpSize[0];
	      
	      w = Math.round(w*1000) / 1000.0;
	      graph.setAttributeValueByName(A_WIDTH, Utilities.toString(w,5,-1) + "in");
	   }
	   else
	      w = measureToInches(wm, false);

      double h = 0;
      String hm = graph.getAttributeValueByName(A_HEIGHT);
      if(!isValidMeasureAttributeValue(hm, false, true, true))
         throw new XMLException("Graph height attribute is not a valid measure!");
      if(hm.indexOf("%") > 0 || hm.indexOf("u") > 0)
      {
         // convert to inches, update attribute value accordingly. Preserve value to nearest 0.001 inch.
         if(vpSize == null || vpSize.length != 2) 
            throw new XMLException("Cannot convert graph height from relative units to inches!");
         
         if(hm.indexOf("%") > 0) h = getMeasureFor(hm) * vpSize[1] / 100.0;
         else h = 0.25 * vpSize[1];
         
         h = Math.round(h*1000) / 1000.0;
         graph.setAttributeValueByName(A_HEIGHT, Utilities.toString(h,5,-1) + "in");
      }
      else 
         h = measureToInches(hm, false);
      
      // convert the x- and y-coordinates in the "loc" attribute, IF either is expressed in "%" units.
      String loc = graph.getAttributeValueByName(A_LOC);
      if(!isValidMeasurePointAttributeValue(loc, true, true))
         throw new XMLException("Graph loc attribute is not a valid measured point!");
      if(loc.indexOf("%") > 0)
      {
         // convert one or both coordinates to inches. Preserve values to nearest 0.001 in.
         StringTokenizer st = new StringTokenizer(loc);
         String xm = st.nextToken();
         String ym = st.nextToken();
         if(xm.indexOf("%") > 0)
         {
            if(vpSize == null || vpSize.length != 2) 
               throw new XMLException("Cannot convert graph x-coord from '%' units to inches!");
            double x = getMeasureFor(xm) * vpSize[0] / 100.0;
            x = Math.round(x*1000) / 1000.0;
            xm = Utilities.toString(x,5,-1) + "in";
         }
         if(ym.indexOf("%") > 0)
         {
            if(vpSize == null || vpSize.length != 2) 
               throw new XMLException("Cannot convert graph y-coord from '%' units to inches!");
            double y = getMeasureFor(ym) * vpSize[1] / 100.0;
            y = Math.round(y*1000) / 1000.0;
            ym = Utilities.toString(y,5,-1) + "in";
         }
         graph.setAttributeValueByName(A_LOC, xm + " " + ym);
      }
      
      return( new double[] {w, h} );
	}
}
