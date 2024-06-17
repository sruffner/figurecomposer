package com.srscicomp.fc.fypml;

import java.util.Stack;

import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.XMLException;

/**
 * <code>Schema1</code> is the encapsulation of the <em>DataNavPhyplot</em> XML schema version 1, which became effective 
 * with the introduction of schema versioning in <em>Phyplot</em> version 0.7.0. It includes support for migrating older 
 * <em>Phyplot</em> XML documents -- conforming to schema version 0 -- to this version. <code>Schema1</code> extends 
 * <code>BaseSchema</code>. Only a few minor schema changes were introduced in this version: 
 * <ul>
 * 	<li>Hollow symbols were jettisoned. As a result, the adornments "fillcircle", "fillbox", and "filldiamond" were 
 * 	no longer needed, since they were no longer distinct from the formerly hollow symbols "circle", "box", and 
 * 	"diamond" respectively.</li>
 * 	<li>In <code>BaseSchema</code>, the "symbol" attribute on data set elements was restricted to a small portion of 
 *    the entire set of adornments supported by <em>DataNav</em>. Those adornments intended as endcap decorations were 
 *    excluded. With <code>Schema1</code>, this is no longer the case. The "symbol" attribute can now be any adornment 
 *    supported in <em>DataNav</em>.</li>
 * </ul>
 * <p>[<em>DataNav</em> superceded <em>Phyplot</em> in 2008.]</p>
 * @author 	sruffner
 */
class Schema1 extends BaseSchema
{
   @Override public String getNamespaceUri() { return("http://www.keck.ucsf.edu/phyplot/namespace/schema1"); }
   @Override public int getVersion() { return(1); }
   @Override public String getLastApplicationVersion() { return("0.7.0"); }

	// the following adornments were added in schema version 1
	final static String ADORN_OVAL12 = "oval1:2";
	final static String ADORN_OVAL21 = "oval2:1";
	final static String ADORN_RECT12 = "rect1:2";
	final static String ADORN_RECT21 = "rect2:1";
	final static String ADORN_DIAMOND12 = "diamond1:2";
	final static String ADORN_DIAMOND21 = "diamond2:1";
	final static String ADORN_UPTRIANGLE = "upTriangle";
	final static String ADORN_DNTRIANGLE = "downTriangle";
	final static String ADORN_LFTRIANGLE = "leftTriangle";
	final static String ADORN_RTTRIANGLE = "rightTriangle";
	final static String ADORN_UPISOTRIANGLE = "upIsoTriangle";
	final static String ADORN_DNISOTRIANGLE = "downIsoTriangle";
	final static String ADORN_LFISOTRIANGLE = "leftIsoTriangle";
	final static String ADORN_RTISOTRIANGLE = "rightIsoTriangle";
	final static String ADORN_UPDART = "upDart";
	final static String ADORN_DNDART = "downDart";
	final static String ADORN_LFDART = "leftDart";
	final static String ADORN_RTDART = "rightDart";
	final static String ADORN_UPARROW = "upArrow";
	final static String ADORN_DNARROW = "downArrow";
	final static String ADORN_LFARROW = "leftArrow";
	final static String ADORN_RTARROW = "rightArrow";
	
	/**
	 * The names of all adornments supported in schema version 1. Each of the following enumerated attributes may take 
	 * on any value from this set: "symbol", "cap", "p0Cap", "p1Cap", "midCap".
	 */
	final static String[] ADORN1_CHOICES = {
		ATTRVAL_NONE, ADORN_TEE, ADORN_XHAIR, ADORN_STAR, ADORN_CIRCLE, ADORN_OVAL12, ADORN_OVAL21, 
		ADORN_BOX, ADORN_RECT12, ADORN_RECT21, ADORN_DIAMOND, ADORN_DIAMOND12, ADORN_DIAMOND21, 
		ADORN_UPTRIANGLE, ADORN_DNTRIANGLE, ADORN_LFTRIANGLE, ADORN_RTTRIANGLE, 
		ADORN_UPISOTRIANGLE, ADORN_DNISOTRIANGLE, ADORN_LFISOTRIANGLE, ADORN_RTISOTRIANGLE, 
		ADORN_UPDART, ADORN_DNDART, ADORN_LFDART, ADORN_RTDART, 
		ADORN_UPARROW, ADORN_DNARROW, ADORN_LFARROW, ADORN_RTARROW, 
		ADORN_LINETHRU, ADORN_LINEUP, ADORN_LINEDOWN, ADORN_BRACKET, ADORN_ARROW, ADORN_FILLARROW, 
		ADORN_THINARROW, ADORN_FILLTHINARROW, ADORN_WIDEARROW, ADORN_FILLWIDEARROW, ADORN_REV_ARROW, ADORN_REV_FILLARROW, 
		ADORN_REV_THINARROW, ADORN_REV_FILLTHINARROW, ADORN_REV_WIDEARROW, ADORN_REV_FILLWIDEARROW, 
		};


	/**
	 * Checks whether or not an attribute's value is valid for the specified owner element in this schema. The method 
	 * defers to <code>BaseSchema</code>, with the following exceptions:
	 * <ul>
	 * 	<li>Attribute "symbol" can now be any supported adornment type.</li>
	 * 	<li>Hollow symbols were jettisoned in <code>Schema1</code>, making several adornment types redundant. The 
    *    revised set of adornments supported in <code>Schema1</code> are listed in <code>ADORN1_CHOICES</code>.</li>
	 * </ul>
	 * @see ISchema#isValidAttributeValue(BasicSchemaElement,String,String)
	 */
   @Override
	public boolean isValidAttributeValue(BasicSchemaElement e, String attr, String value)
	{
		if(A_SYMBOL.equals(attr) || A_CAP.equals(attr) || A_P0CAP.equals(attr) || A_P1CAP.equals(attr) || 
		      A_MIDCAP.equals(attr))
			return(isValidEnumAttributeValue(value, ADORN1_CHOICES));
		else
			return(super.isValidAttributeValue(e, attr, value));
	}

	/**
	 * This method handles the actual details of migrating schema version 0 content to schema version 1. It makes the 
	 * following changes:
	 * <ul>
	 * 	<li>If any "pointSet", "series", or "function" uses one of the formerly "hollow" symbols ("circle", "box", 
    *    "diamond"), then that element's "fillColor" attribute is set to pure white to approximate the appearance of a 
    *    hollow symbol. This change will only cause an artifact if the data element uses a filled adornment as the 
    *    error bar endcap; in this case, the endcap adornment will now be filled with white.</li>
	 * 	<li>Since "hollow symbols" are no longer supported, several adornment types in schema version 0 are redundant 
	 * 	in version 1: "fillcircle" replaced by "circle"; "fillbox" by "box"; and "filldiamond" by "diamond".</li>
	 * </ul>
	 * @see ISchema#migrateFromPreviousSchema(ISchema)
	 */
   @Override
	public void migrateFromPreviousSchema(ISchema oldSchema) throws XMLException
	{
		if(oldSchema.getVersion() != getVersion() - 1) 
			throw new XMLException("A schema instance can only migrate from the previous version.");

		// update the content of the old schema in place...
		boolean defSymWasHollow = false;
		Stack<BasicSchemaElement> elementStack = new Stack<>();
		elementStack.push((BasicSchemaElement)oldSchema.getRootElement());
		while(!elementStack.isEmpty())
		{
			BasicSchemaElement e = elementStack.pop();
			String elTag = e.getTag();

			// the "symbol" attribute has and still has a global default value specified in the root element. Remember 
			// whether or not that default was set to one of the formerly "hollow" symbols.
			if(EL_FYP.equals(elTag))
			{
				String symbol = e.getAttributeValueByName(A_SYMBOL);
				defSymWasHollow = ADORN_CIRCLE.equals(symbol) || ADORN_BOX.equals(symbol) || ADORN_DIAMOND.equals(symbol);
			}

			// if formerly "hollow" symbol specified for the "symbol" attribute of a data set, then explicitly set the 
			// element's "fillColor" attribute to pure white. The "symbol" attribute itself does not have to be changed.
			if(EL_POINTSET.equals(elTag) || EL_FUNCTION.equals(elTag) || EL_SERIES.equals(elTag))
			{
				String symbol = e.getAttributeValueByName(A_SYMBOL);
				if((symbol == null && defSymWasHollow) || 
				    ADORN_CIRCLE.equals(symbol) || ADORN_BOX.equals(symbol) || ADORN_DIAMOND.equals(symbol))
					e.setAttributeValueByName(A_FILLCOLOR, "FFFFFF");
			}

			// if the "symbol", "cap", "p0Cap", "p1Cap", or "midCap" attributes is defined for the element and is 
			// currently set to "fillcircle", "fillbox", or "filldiamond", then change it to "circle", "box", or 
			// "diamond", respectively.
			final String[] adornAttrs = new String[] {A_SYMBOL, A_CAP, A_P0CAP, A_P1CAP, A_MIDCAP};
         for(String attr : adornAttrs)
         {
            if(e.hasAttribute(attr))
            {
               String value = e.getAttributeValueByName(attr);
               if(ADORN_FILLCIRCLE.equals(value)) e.setAttributeValueByName(attr, ADORN_CIRCLE);
               else if(ADORN_FILLBOX.equals(value)) e.setAttributeValueByName(attr, ADORN_BOX);
               else if(ADORN_FILLDIAMOND.equals(value)) e.setAttributeValueByName(attr, ADORN_DIAMOND);
            }
         }

			// finally, migrate the element object's schema info. We do not need to change the names of any attributes 
			// or elements, and we've already corrected any attribute values above.
			e.updateSchema(this, null);

			// if element has any children, push them onto the stack so that we check them as well!
			for(int i=0; i<e.getChildCount(); i++)
				elementStack.push((BasicSchemaElement) e.getChildAt(i));
		}

		// the content model now conforms to this schema. We get the root element from the old schema and install it as 
		// the root of this schema, then empty the old schema. We also remember the original schema version of the 
		// migrated content.
		originalVersion = oldSchema.getOriginalVersion();
		setRootElement(oldSchema.getRootElement(), false);
		oldSchema.setRootElement(null, false);
	}
}
