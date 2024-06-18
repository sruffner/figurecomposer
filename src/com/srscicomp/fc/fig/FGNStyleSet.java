package com.srscicomp.fc.fig;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import com.srscicomp.common.util.Utilities;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * <code>FGNStyleSet</code> is a container for styles extracted from a <i>FypML</i> graphic node that can be applied to 
 * another node. It includes the infrastructure for persisting to and restoring from a JSON object. The various 
 * subclasses of {@link FGraphicNode} includes methods that return the node's current style set or apply a style set to
 * that node in a single reversible operation. The user's "style palette" in <i>Figure Composer</i> is a collection of
 * up to 20 recent style sets copied from graphic nodes on display in the application.
 * 
 * @author sruffner
 */
public class FGNStyleSet
{
   /**
    * Construct a graphic style set based on properties extracted from the specified <i>FypML</i> node type. The style 
    * set is populated by calls to {@link #putStyle}.
    * @param src Type of <i>FypML</i> graphic node from which style set will be extracted.
    */
   FGNStyleSet(FGNodeType src)
   {
      assert(src != null);
      srcNodeType = src;
   }
   
   /**
    * Get the type of the <i>FypML</i> graphic node from which this style set was extracted.
    * @return The source node type.
    */
   public FGNodeType getSourceNodeType() { return(srcNodeType); }
      
   /**
    * Is this graphic node style set devoid of any style properties?
    * @return True if style set is empty.
    */
   public boolean isEmpty() { return(styleMap.isEmpty()); }
   
   /**
    * Add a style property to this style set. 
    * @param prop The <i>FypML</i> graphic node property ID. If null, no action is taken.
    * @param value The property value. If null, no action is taken.
    */
   void putStyle(FGNProperty prop, Object value) 
   { 
      if(prop != null && value != null) styleMap.put(prop, value);
   }
   
   /**
    * Does this style set include the specified property?
    * @param prop The <i>FypML</i> graphic node property ID.
    * @return True if identified property is in the set; else false.
    */
   boolean hasStyle(FGNProperty prop) { return(styleMap.containsKey(prop)); }
   
   /**
    * Get the value of the specified property in this style set.
    * @param prop The <i>FypML</i> graphic node property ID.
    * @return The property's value, or null if style set does not contain the property.
    */
   Object getStyle(FGNProperty prop) { return(styleMap.get(prop)); }
   
   /**
    * Get the set of properties in this style set.
    * @return A set of <i>FypML</i> graphic node property IDs identifying all properties in the set.
    */
   Set<FGNProperty> getProperties() { return(styleMap.keySet()); }
   
   /**
    * Remove a style property from this style set.
    * @param prop The property to be removed. If null, no action is taken.
    * @return The value that was assigned to the property, or null if there was no such property.
    */
   Object removeStyle(FGNProperty prop)
   {
      return(prop == null ? null : styleMap.remove(prop));
   }
   
   /**
    * Get the specified style property value within this style set, if present. If both optional arguments are null,
    * this is the same as {@link #getStyle(FGNProperty) }.
    * 
    * @param prop The property type. 
    * @param nodeType (Optional) The type of <i>FypML</i> graphic node to which the property applies. If non-null and
    * does not match the type of node that sourced this style set, then method returns null.
    * @param c (Optional) The expected class for the property value. If non-null and the specified property is present,
    * the method checks the class of the property value and returns null if there's no match. If you specify the value
    * class, then you can safely cast the return value to that class. Of course, you still have to check whether or not
    * the return value is null.
    * @return The property value requested, or null if style set lacks the specified property.
    */
   Object getCheckedStyle(FGNProperty prop, FGNodeType nodeType, Class<?> c)
   {
      if((prop == null) || (nodeType != null && nodeType != srcNodeType)) return(null);
      Object value = styleMap.get(prop);
      if(value != null && c != null && !value.getClass().equals(c)) value = null;
      return(value);
   }
   
   /**
    * Add a subordinate style set encapsulating the styling of a required component node of the graphic node from which
    * this style set is constructed.
    * @param cs The component node's style set.
    */
   void addComponentStyleSet(FGNStyleSet cs) { if(cs != null) componentStyling.add(cs); }
   
   /**
    * Remove a subordinate style set encapsulating the styling of a required component node of the graphic node from 
    * which this style set is constructed.
    * @param idx Zero-based index of component style set to remove. No action taken if this is invalid.
    */
   void removeComponentStyleSet(int idx) { if(idx >=0 && idx<componentStyling.size()) componentStyling.remove(idx); }
   
   /**
    * Get the number of component node style sets subordinate to this graphic node style set. Relevant only to those
    * <i>FypML</i> graphic node types that include component nodes.
    * @return The number of component style sets.
    */
   int getNumberComponentStyleSets() { return(componentStyling.size()); }
   
   /**
    * Get one of the component node style sets subordinate to this graphic node style set. Relevant only to those
    * <i>FypML</i> graphic node types that include component nodes.
    * @param idx Zero-based index of component style set requested.
    * @return The requested component style set.
    * @throws IndexOutOfBoundsException if index is invalid.
    */
   FGNStyleSet getComponentStyleSet(int idx) { return(componentStyling.get(idx)); }
   
   /** The type of graphic node from which the style set was originally extracted. */
   private final FGNodeType srcNodeType;
   
   /** The node property values comprising this style set, keyed by property ID. */
   private final HashMap<FGNProperty, Object> styleMap = new HashMap<>();
   
   /** 
    * This list contains the style set for each component node of the node that sourced this style set. Will be empty if
    * the source node lacks any component nodes.
    */
   private final List<FGNStyleSet> componentStyling = new ArrayList<>();
   
   /**
    * Test whether or not the source and destination style sets "match". This is NOT a test for equality. The source
    * set matches the destination if:
    * <ol>
    * <li>For every style property in BOTH sets, the source and destination property value are equal.</li>
    * <li>If the source has NS component style sets and the destination has ND components, then for i=1..min(NS,ND),
    * the source and destination component style sets at index i both match (involves recursion).</li>
    * </ol>
    * <p>This method can be used to assess whether the source style set will have any effect it is applied to a graphic
    * node. If that node's current style set "matches" the source set, then graphic node will be unaffected.</p>
    * 
    * @param src The source style set.
    * @param dst The destination style set.
    * @return True if the two sets "match" in the sense described above.
    */
   public static boolean matching(FGNStyleSet src, FGNStyleSet dst)
   {
      if(src == null || dst == null) return(false);
      
      boolean match = true;
      for(FGNProperty prop : src.styleMap.keySet())
      {
         Object srcValue = src.styleMap.get(prop);
         Object dstValue = dst.styleMap.get(prop);
         if(dstValue != null && !dstValue.equals(srcValue))
         {
            match = false;
            break;
         }
      }
      
      // check any component nodes at like positions in the component node style set lists. The compared components 
      // must have the same node types as well as the same property values.
      if(match)
      {
         int n = Math.min(src.componentStyling.size(), dst.componentStyling.size());
         for(int i=0; match && i<n; i++) 
            match = src.componentStyling.get(i).getSourceNodeType() == dst.componentStyling.get(i).getSourceNodeType()
               && FGNStyleSet.matching(src.componentStyling.get(i), dst.componentStyling.get(i));
      }
      
      return(match);
   }
   
   /**
    * Converts a style set to a JSON object for the purpose of persisting the style set to file. The source node type is
    * stored in string form in field "fgnType", while the each property in the set is stored as a string value in a 
    * field named after the property. If the style set includes any subordinate component style sets, the JSON object
    * includes an additional field, "components", the value of which is a JSON array of JSON objects. Each JSON object
    * represents one of the component style sets, generated by a recursive call to this function. If the style set
    * lacks any components, the "components" field is omitted entirely
    * <p>Here is an example:
    * <pre>
    * {
    *    "fgnType" : "Graph",
    *    "x" : "1in",
    *    "y" : "1.5in",
    *    "title" : "The label's text",
    *    "fillc" : "#FF0000", 
    *    ....,
    *    "components" : [ {"fgntype" : "axis", ...}, ... ]
    * }
    * </pre>
    * </p>
    * 
    * <p>Property values are converted to string form using {@link FGModelSchemaConverter#fgnPropertyToString}.</p>
    * 
    * @param styleSet The style set.
    * @return A JSON object encapsulating the style set's contents, as described. Returns null if conversion fails.
    */
   public static JSONObject toJSON(FGNStyleSet styleSet)
   {
      JSONObject out = new JSONObject();
      boolean ok = true;
      try
      {
         out.put("fgnType", styleSet.srcNodeType.toString());
         for(FGNProperty prop : styleSet.styleMap.keySet())
         {
            String strValue = FGModelSchemaConverter.fgnPropertyToString(styleSet.srcNodeType, prop, 
                  styleSet.styleMap.get(prop));
            if(strValue == null) throw new JSONException("Unable to convert property " + prop +  " to string form");
            out.put(prop.toString(), strValue);
         }
         
         if(!styleSet.componentStyling.isEmpty())
         {
            JSONArray jCmpts = new JSONArray();
            for(FGNStyleSet cmptStyle : styleSet.componentStyling) 
            {
               JSONObject jsonObj = FGNStyleSet.toJSON(cmptStyle);
               if(jsonObj == null) throw new JSONException("Failed to convert component style set");
               jCmpts.put(jsonObj);
            }
            out.put("components", jCmpts);
         }
      }
      catch(JSONException jse) { ok = false; }
      
      return(ok ? out : null);
   }
   
   /**
    * The inverse of {@link #toJSON(FGNStyleSet)}.
    * 
    * @param jsonStyleSet A JSONObject encapsulating a graphic node style set, as prepared by {@link #toJSON}.
    * @return The style set represented by the argument. If the argument does not conform to the format expected, method
    * returns null.
    */
   public static FGNStyleSet fromJSON(JSONObject jsonStyleSet)
   {
      if(jsonStyleSet == null) return(null);
      
      FGNStyleSet styleSet = null;
      boolean ok = true;
      try
      {
         FGNodeType nt = Utilities.getEnumValueFromString(jsonStyleSet.getString("fgnType"), FGNodeType.values());
         styleSet = new FGNStyleSet(nt);
         
         // handle any component style sets if the JSON object has the "components" field
         if(jsonStyleSet.has("components"))
         {
            JSONArray jCmpts = jsonStyleSet.getJSONArray("components");
            for(int i=0; i<jCmpts.length(); i++)
            {
               FGNStyleSet styleCmpt = FGNStyleSet.fromJSON(jCmpts.getJSONObject(i));
               if(styleCmpt == null) throw new JSONException("Unable to convert component style set!");
               styleSet.addComponentStyleSet(styleCmpt);
            }
         }
         
         @SuppressWarnings("rawtypes")
         Iterator keys = jsonStyleSet.keys();
         while(keys.hasNext())
         {
            Object keyObj = keys.next();
            if("components".equals(keyObj) || "fgnType".equals(keyObj) || !String.class.equals(keyObj.getClass())) 
               continue;
            
            String strProp = (String) keyObj;
            String strValue = jsonStyleSet.getString(strProp);
            FGNProperty prop = Utilities.getEnumValueFromString(strProp, FGNProperty.values());
            if(prop == null) 
               throw new JSONException("Bad property ID: " + strProp);
            Object value = FGModelSchemaConverter.fgnPropertyFromString(nt, prop, strValue);
            if(value == null) 
               throw new JSONException("Unable to convert property value: " + strProp + "=" + strValue);
            
            styleSet.putStyle(prop, value);
         }
      }
      catch(JSONException jse) { ok = false; }
      
      return(ok ? styleSet : null);
   }
}
