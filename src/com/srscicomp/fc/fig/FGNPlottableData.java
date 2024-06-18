package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;

/**
 * This abstract extension of {@link FGNPlottable} defines and partially implements functionality common to the data set 
 * presentation nodes available in {@link FGraphicModel}. In particular, it provides get/set access to the data set that
 * sources the data and restricts it to one of the data formats returned by {@link #getSupportedDataFormats()}.
 * @author sruffner
 */
public abstract class FGNPlottableData extends FGNPlottable implements Cloneable
{
   /** 
    * Construct a new data set presentation node, initially containing an empty data set in a supported data format. 
    * @param attrFlags The attribute flags relevant to this node. See {@link FGraphicNode#FGraphicNode(int)}.
    */
   FGNPlottableData(int attrFlags)
   {
      super(attrFlags);
      if(hasDataGroups()) dataGrpInfo = new ArrayList<>();
      for(Fmt fmt : Fmt.values()) if(isSupportedDataFormat(fmt))
      {
         set = DataSet.createEmptySet(fmt);
         break;
      }
      assert(set != null);
   }

   /** Source of the raw data rendered by this data presentation node. Never null. */
   private DataSet set;
   
   /**
    * Get the data set currently rendered by this data presentation node.
    * @return This node's current data set.
    */
   public DataSet getDataSet() { return(set); }
   
   /**
    * Update the data set rendered by this data presentation node and re-render it accordingly.
    * <p><b>NOTE</b>. Since {@link DataSet} is immutable, this method may be used to modify only the set identifier. The
    * method checks for this possibility, in which case the node is NOT re-rendered since the raw data itself is 
    * unchanged. The preferred way to change the set identifier without changing the data is to call {@link 
    * #setDataSetID}.</p>
    * 
    * @param ds The new data set.
    * @return True if successful; false if the argument is null or represents data in an unsupported format.
    */
   public boolean setDataSet(DataSet ds)
   {
      if(ds == null || !isSupportedDataFormat(ds.getFormat())) return(false);
      if(ds == set || DataSet.areIdenticalSets(set, ds, false)) return(true);
      if(DataSet.areIdenticalSets(set, ds, true))
         return(setDataSetID(ds.getID()));
      
      DataSet old = set;
      set = ds;
      onNodeModified(FGNProperty.SRC);
      String desc = "Set dataset on " + getNodeType().getNiceName() + ": " + set.getInfo().getShortDescription();
      FGNRevEdit.post(this, FGNProperty.SRC, set, old, desc);
      return(true);
   }
   
   /**
    * Get the data set identifier for the data set currently rendered by this data presentation node.
    * @return The ID string of this node's current data set.
    */
   public String getDataSetID() { return(set.getID()); }
   
   /**
    * Update the data set identifier for the data set currently rendered by this data presentation node. Since 
    * {@link DataSet} is immutable, this effectively generates a new data set object identical to the existing one 
    * except for the ID string (and backed by the same raw data array).
    * @param id The candidate ID.
    * @return True if successful; false if the ID is invalid.
    */
   public boolean setDataSetID(String id)
   {
      DataSet adjSet = set.changeID(id);
      if(adjSet == null) return(false);
      if(adjSet == set) return(true);  // candidate ID matched current ID!

      DataSet old = set;
      set = adjSet;
      onNodeModified(FGNProperty.SRC);
      String desc = "Change dataset ID on " + getNodeType().getNiceName() + ": ";
      desc += old.getID() + " -> " + set.getID();
      FGNRevEdit.post(this, FGNProperty.SRC, set, old, desc);
      return(true);
   }
   
   /**
    * Does this data presentation node display data in the specified format?
    * @param fmt The data format to check.
    * @return True only if this data presentation node can render data in the specified format. Returns false if the
    * <i>fmt</i> argument is null.
    */
   public abstract boolean isSupportedDataFormat(Fmt fmt);
   
   /**
    * Get list of all data set formats supported by this data presentation node.
    * @return Array containing the distinct data formats that this data presentation node can render. Must contain at 
    * least one valid format.
    */
   public abstract Fmt[] getSupportedDataFormats();
   
   /**
    * Override returns whether or not the node's currently rendered data set has nonzero error data, as indicated by 
    * {@link DataSet#hasErrorData()}.
    */
   @Override public boolean hasErrorData() { return(set.hasErrorData()); }

   /** Override returns true, since data set presentation nodes do not admit any children. */
   @Override public boolean isLeaf() { return(true); }
   
   /** Override returns <b>false</b> since data set presentation nodes do not admit any children. */
   @Override public boolean canInsert(FGNodeType nodeType) { return(false); }

   /**
    * Data set presentation nodes are rendered into the data viewport of their parent graph container and do not 
    * establish their own viewport. Therefore, this override returns the identity transform always.
    */
   @Override public AffineTransform getLocalToParentTransform() { return( new AffineTransform() ); }

   /**
    * Data set presentation nodes do not admit children and do not define their own viewport, so this override always 
    * returns the parent viewport.
    */
   @Override public FViewport2D getViewport() { return(getParentViewport()); }
   
   /** 
    * Overridden to handle changes in the data set presentation node's data set, or the color or label of a data group,
    * if the node has data groups.
    */
   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      if(p == FGNProperty.SRC)
      {
         setDataSet((DataSet)propValue);
         return(true);
      }
      else if(p == FGNProperty.DGCOLOR)
      {
         // special case: The "property value" here is a mixed object array. The first is an Integer holding the
         // position of the affected data group; the second holds the fill color for that group.
         Object[] arObj = null;
         boolean ok = propValue != null && propValue.getClass().equals(Object[].class);
         if(ok)
         {
            arObj = (Object[]) propValue;
            ok = arObj.length == 2;
            if(ok) ok = arObj[0] != null && arObj[0].getClass().equals(Integer.class);
            if(ok) ok = arObj[1] != null && arObj[1].getClass().equals(Color.class);
         }
         if(ok)
         {
            int pos = (Integer) arObj[0];
            ok = dataGrpInfo != null && pos >= 0 && pos < dataGrpInfo.size();
            if(ok) ok = setDataGroupColor(pos, (Color) arObj[1]);
         }
         return(ok);
      }
      else if(p == FGNProperty.DGLABEL)
      { 
         // special case: The "property value" here is a mixed object array. The first is an Integer holding the
         // position of the affected data group; the second holds the legend label for that group (a String).
         Object[] arObj = null;
         boolean ok = propValue != null && propValue.getClass().equals(Object[].class);
         if(ok)
         {
            arObj = (Object[]) propValue;
            ok = arObj.length == 2;
            if(ok) ok = arObj[0] != null && arObj[0].getClass().equals(Integer.class);
            if(ok) ok = arObj[1] != null && arObj[1].getClass().equals(String.class);
         }
         if(ok)
         {
            int pos = (Integer) arObj[0];
            ok = dataGrpInfo != null && pos >= 0 && pos < dataGrpInfo.size();
            if(ok) ok = setDataGroupLabel(pos, (String) arObj[1]);
         }
         return(ok);
      }
      else return(super.setPropertyValue(p, propValue));
   }
   
   @Override Object getPropertyValue(FGNProperty p)
   {
      // NOTE: None of the properties maintained by FGNPlottableData are eligible for multi-object edit, so this
      // method is unused. Can't get the DGCOLOR and DGLABEL properties without knowing the group index!!
      return(p==FGNProperty.SRC ? getDataSet() : super.getPropertyValue(p));
   }
   
   /**
    * Whenever the data source for a grouped-data presentation node is modified, the number of data groups in the plot 
    * could change. Ensure that a fill color and label are defined for each data group, with no extras.
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;
      
      if(hasDataGroups() && (hint == null || hint == FGNProperty.SRC)) fixDataGroupInfo(); 

      if(hasDataGroups() && hint == FGNProperty.DGLABEL)
      {
         // in this case, the rendered figure is affected only if the plottable is currently included in the parent
         // graph's legend AND the legend is shown. 
         boolean affected = getShowInLegend();
         if(affected)
         {
            FGNGraph g = getParentGraph();
            if(g != null) affected = !g.getLegend().getHide();
         }
         if(!affected)
         {
            model.onChange(this, 0, false, null);
            return;
         }
      }
      super.onNodeModified(hint);
   }

   
   /** 
    * The grouped-data presentation nodes support <i>FypML</i> styled text in the data group labels, so this method
    * must include any Postscript font faces used to render those labels. These nodes lack child nodes, so the
    * <i>traverse</i> flag is ignored.
    */
   @Override public void getAllPSFontFacesUsed(Map<String, String> fontFaceMap, boolean traverse)
   {
      String face = getPSFontFace();
      if(face != null) fontFaceMap.put(face, null);
      
      // add any font faces for style changes in data group labels (w=bold, i=italic, I=bolditalic, p=plain)
      for(int i = 0; i<getNumDataGroups(); i++)
      {
         String s = getDataGroupLabel(i);
         int idx = s.lastIndexOf('|');
         if((idx >= 0) && (idx < s.length()-1))
         {
            s = s.substring(idx+1);
            if(s.indexOf('p') > -1)
            {
               face = PSDoc.getStandardFontFace(getPSFont(), false, false);
               if(face != null) fontFaceMap.put(face, null);
            }
            if(s.indexOf('w') > -1)
            {
               face = PSDoc.getStandardFontFace(getPSFont(), true, false);
               if(face != null) fontFaceMap.put(face, null);
            }
            if(s.indexOf('i') > -1)
            {
               face = PSDoc.getStandardFontFace(getPSFont(), false, true);
               if(face != null) fontFaceMap.put(face, null);
            }
            if(s.indexOf('I') > -1)
            {
               face = PSDoc.getStandardFontFace(getPSFont(), true, true);
               if(face != null) fontFaceMap.put(face, null);
            }
         }
      }
   }

   //
   // Support for grouped-data presentation nodes: bar plots, area charts, pie charts. Each "data group" has an
   // associated fill color and text label, and each has its own legend entry, in the form of a rectangular bar.
   // As of v5.0.2, translucent or transparent fill colors are permitted.
   //

   /** 
    * Maximum number of data groups in a grouped-data presentation node. If the underlying data collection has more
    * than this, the extra groups are simply ignored.
    */
   public final static int MAX_DATAGRPS = 20;
   
   /**
    * If this is a grouped-data presentation node and any data group is assigned a translucent fill color, then this
    * node is translucent.
    */
   @Override protected boolean isTranslucent()
   {
      if(super.isTranslucent()) return(true);
      for(int i=0; i<getNumDataGroups(); i++) 
      {
         int alpha = getDataGroupColor(i).getAlpha();
         if(alpha > 0 && alpha < 255) return(true);
      }
      return(false);
   }

   /**
    * Is this a grouped-data presentation node? <i>This base-class implementation returns false always. Any class
    * implementing a grouped-data node must override and return true to enable support for data group properties.</li>
    * @return True if this is a grouped-data presentation node.
    */
   public boolean hasDataGroups() { return(false); }
   
   /**
    * Get the number of data groups in this grouped-data presentation node. The number can never exceed {@link 
    * #MAX_DATAGRPS}.
    * @return The number of data groups; 0 if this data presentation node does not have data groups.
    */
   public final int getNumDataGroups() 
   { 
      if(!hasDataGroups()) return(0);
      fixDataGroupInfo();
      return(getDataGroupCount()); 
   }
   
   /**
    * Get the fill color assigned to the specified data group in this grouped-data presentation node.
    * @param pos Data group index.
    * @return Null if index is invalid or if presentation node lacks data groups, else the assigned color.
    */
   public Color getDataGroupColor(int pos) 
   {
      return((pos>=0 && pos<getNumDataGroups()) ? dataGrpInfo.get(pos).fillC : null);
   }
   
   /**
    * Set the fill color for the specified data group in this grouped-data presentation node. If a change is made, 
    * {@link #onNodeModified} is invoked.
    * @param pos Data group index. No action is taken if invalid, or if this is not a grouped-data presentation node.
    * @param c The desired fill color. A null value is rejected. Opaque or translucent color allowed.
    * @return True if value was accepted; false otherwise.
    */
   public boolean setDataGroupColor(int pos, Color c)
   {
      if(pos < 0 || pos >= getNumDataGroups() || c==null) return(false);
      
      if(!c.equals(dataGrpInfo.get(pos).fillC))
      {
         Color old = dataGrpInfo.get(pos).fillC;
         dataGrpInfo.get(pos).fillC = c;
         if(areNotificationsEnabled())
         {
            // index of the affected data group is included with old and new values of the data group color
            Object[] oldInfo = new Object[] {pos, old};
            Object[] newInfo = new Object[] {pos, c};
            onNodeModified(FGNProperty.DGCOLOR);
            FGNRevEdit.post(this, FGNProperty.DGCOLOR, newInfo, oldInfo, "Change data group color, index " + (pos+1));
         }
      }
      return(true);
   }
   
   /**
    * Get the legend label assigned to the specified data group in this grouped-data presentation node. The label may
    * conform to <i>FypML</i> "styled text" format. See {@link FGraphicNode#toStyledText}.
    * 
    * @param pos Data group index.
    * @return Null if index is invalid or if presentation node lacks data groups, else the assigned label.
    */
   public String getDataGroupLabel(int pos)
   {
      return((pos>=0 && pos<getNumDataGroups()) ? dataGrpInfo.get(pos).label : null);
   }
   
   /**
    * Set the legend label for the specified data group in this bar grouped-data presentation node. The label may be
    * a simple string, or it may conform to <i>FypML</i> "styled text" format. If a change is made, {@link 
    * #onNodeModified} is invoked.
    * 
    * <p>A data group label cannot contain the character sequence ";;", which is used to separate the data group colors
    * and labels when the presentation node is converted to <i>FypML</i>. Prior to V5.4.4, a comma was used as the 
    * separator, but this precluded not only labels containing a comma, but also "styled text" labels. In <i>FypML</i>,
    * styled text is encoded in plain text as "S|X", where S is the rendered sequence of characters and X is an
    * attribute suffix that defines character attribute changes that occur with that sequence; X can contain one or
    * more commas.</p>
    * 
    * @param pos Data group index. No action taken if invalid, or if this is not a grouped-data presentation node.
    * @param s The desired label. Null is rejected, as is any string containing two consecutive semicolons (";;"). Any 
    * unsupported characters are silently removed from the string, which is also trimmed of leading and trailing 
    * whitespace.
    * @return True if value was accepted; false otherwise.
    */
   public boolean setDataGroupLabel(int pos, String s)
   {
      if(pos < 0 || pos >= getNumDataGroups() || s==null || s.contains(";;")) return(false);
      
      s = s.trim();
      if(!s.equals(dataGrpInfo.get(pos).label))
      {
         String old = dataGrpInfo.get(pos).label;
         dataGrpInfo.get(pos).label = s;
         if(areNotificationsEnabled())
         {
            // index of the affected data group is included with old and new values of the data group label
            Object[] oldInfo = new Object[] {pos, old};
            Object[] newInfo = new Object[] {pos, s};
            onNodeModified(FGNProperty.DGLABEL);
            FGNRevEdit.post(this, FGNProperty.DGLABEL, newInfo, oldInfo, "Change data group label, index " + (pos+1));
         }
      }
      return(true);
   }
   
   /**
    * Get the data group properties (fill color and legend label for each data group) defined on this grouped-data
    * presentation node as a list of tokens separated by semicolon pairs: <i>color1;;label1;;color2;;label2;;...</i>.
    * The method {@link BkgFill#colorToHexString} is used to convert each color to string form. Also note that any or
    * all legend labels could be empty strings.
    * 
    * <p>This method is used exclusively while preparing the grouped-data node's representation in <i>FypML</i>; the 
    * token list appears in the text content of the <i>FypML</i> element defining the node. Before generating the list, 
    * any missing data group properties are auto-generated.</p>
    * 
    * @return The token list, as described. Returns an empty string if this is NOT a grouped-data presentation node.
    * string is returned.
    */
   String getDataGroupInfoAsTokenizedString()
   {
      if(!hasDataGroups()) return("");
      
      fixDataGroupInfo();
      
      StringBuilder sb = new StringBuilder();
      for(int i=0; i<getDataGroupCount(); i++)
      {
         GrpInfo gi = dataGrpInfo.get(i);
         sb.append(BkgFill.colorToHexString(gi.fillC)).append(";;").append(gi.label);
         if(i<dataGrpInfo.size()-1) sb.append(";;");
      }
      return(sb.toString());
   }
   
   /**
    * Parse a list of string tokens and from it generate the data group properties for this grouped-data presentation
    * node.
    * 
    * <p>This method is used exclusively while reconstructing a grouped-data presentation node from its <i>FypML</i> 
    * mark-up definition; the token list appears in the text content of the corresponding <i>FypML</i> element. Prior to 
    * V5.4.4, a single comma (',') was used as the token separator; from 5.4.4 onward, a pair of semicolons serves as
    * the separator so that commas can appear in the data group labels, which may be formatted as <i>FypML</i> "styled
    * text" (introduced in V5.4.0). If the string argument contains at least one ";;" token, then that is assumed to be
    * the token separator; otherwise, a comma is assumed.</p>
    * 
    * <p>Typically, the list will take the same form as generated by {@link #getDataGroupInfoAsTokenizedString()}, but
    * this is not guaranteed since a <i>FypML</i> file could be created or modified outside <i>Figure Composer</i>. This
    * method will attempt to parse each string token as a color using {@link BkgFill#colorFromHexStringStrict}; any
    * token that cannot be parsed as a color is interpreted as a data group legend label, which may be in the "styled 
    * text" format. Up to {@link #MAX_DATAGRPS} color-label pairs will be parsed from the list; any additional content 
    * is ignored.</p>
    * 
    * <p>This method should be used only when a figure is being reconstituted from its <i>FypML</i> definition; it does
    * not notify the model of a change to the grouped-data node. If this is NOT a grouped-data node, the method has no
    * effect whatsoever.</p>
    * 
    * @param csTokens The string content to be parsed.
    */
   void setDataGroupInfoFromTokenizedString(String csTokens)
   {
      if(!hasDataGroups()) return;
      
      dataGrpInfo.clear();
      if(csTokens == null) return;
      else csTokens = csTokens.trim();
      
      String separator = (csTokens.indexOf(";;")) > 0 ? ";;" : ",";
      String[] tokens = (csTokens.isEmpty()) ? new String[0] : csTokens.split(separator, -1);
      List<Color> colors = new ArrayList<>();
      List<String> labels = new ArrayList<>();
      for(String s : tokens)
      {
         Color c = BkgFill.colorFromHexStringStrict(s, true);
         if(c != null) colors.add(c);
         else labels.add(s.trim());
      }
      
      int n = Math.min(colors.size(), labels.size());
      if(n > MAX_DATAGRPS) n = MAX_DATAGRPS;
      for(int i=0; i<n; i++) dataGrpInfo.add(new GrpInfo(colors.get(i), labels.get(i)));
   }
   
   /**
    * Get the number of data groups for this grouped-data presentation node. <i>This base-class implementation returns 
    * 0 always. Any class implementing a grouped-data node must override and return the actual number of data groups.
    * The number returned should be range-restricted to [0 .. {@link #MAX_DATAGRPS}].</li>
    * @return Number of distinct data groups for this grouped-data node.
    */
   protected int getDataGroupCount() { return(0); }
   
   /**
    * Fix the data group information for this grouped-data presentation node.
    * 
    * <p>Bar, area, and pie charts are all examples of "grouped-data" nodes, in which each data group is rendered in a
    * different color and is associated with a different entry in the parent graph's automated legend. A fill color and
    * legend label constitute the information -- or "decoration" -- assigned to each data group. This method ensures 
    * that there is a fill color and label for each data group. It should be called whenever the underlying data source 
    * changes, since that could change the number of groups.</p>
    * <p>If there are more data groups than defined group decorations, this method will auto-generate the missing colors
    * and labels. The colors are selected from the {@link ColorMap.BuiltIn#jet} color map based on the index position I 
    * of the data group, while the legend label is set to "group I". <b>However, if there are too many decorations, the 
    * list is left unchanged -- the extra decorations are simply unused.</b> This is important when the node is being 
    * rebuilt from its XML representation, because the data group decorations will be reconstituted BEFORE the node's 
    * data set is applied!</p>
    * <p>If the number of data groups in the source exceeds {@link #MAX_DATAGRPS}, the extra data groups are ignored.
    * The grouped-data presentation nodes are only intended for rendering small collections of data.</p>
    */
   private void fixDataGroupInfo()
   {
      if(!hasDataGroups()) return;

      int nGrps = Math.min(getDataGroupCount(), MAX_DATAGRPS);
      if(dataGrpInfo.size() < nGrps)
      {
         ColorLUT lut = new ColorLUT(ColorMap.BuiltIn.jet.cm, false);
         int len = ColorMap.LUTSIZE;
         while(dataGrpInfo.size() < nGrps)
         {
            int idx = dataGrpInfo.size();
            String label = "group " + idx;
            idx = Utilities.rangeRestrict(0, len-1, idx * len / nGrps);
            dataGrpInfo.add(new GrpInfo(new Color(lut.getRGB(idx)), label));
         }
      }
   }
   
   /**
    * The fill colors and labels assigned to individual data groups in a grouped-data presentation node. The list is
    * augmented or truncated as necessary whenever the number of data groups changes. Null if this is not a grouped-data
    * presentation node.
    */
   private List<GrpInfo> dataGrpInfo = null;
   
   /** 
    * Each group in a grouped-data presentation node is assigned different fill color and legend label. This helper 
    * class is a simple container for those two properties.
    * 
    * @author sruffner
    */
   static class GrpInfo
   {
      /**
       * Construct a data group properties object.
       * @param fillC The data group's fill color. If null, opaque white is assumed.
       * @param s The data group's label. If null, an empty string is assumed. If the label contains any tab, carriage
       * return, or line-feed characters, they are removed. Any unsupported characters are also removed.
       */
      GrpInfo(Color fillC, String s)
      {
         this.fillC = (fillC == null) ? Color.WHITE : fillC;
         if(s == null) 
            this.label = "";
         else
            this.label = FGraphicModel.removeUnsupportedCharacters(s, true);
      }
      
      @Override public int hashCode() { return(fillC.hashCode() * 31 + label.hashCode()); }
      @Override public boolean equals(Object o)
      {
         if(o == null || !o.getClass().equals(GrpInfo.class)) return(false);
         GrpInfo gi = (GrpInfo) o;
         return(fillC.equals(gi.fillC) && label.equals(gi.label));
      }

      /** The data group's fill color. */
      Color fillC;
      /** The data group's legend label. */
      String label;

   }
   
   /** 
    * A grouped-data presentation node includes one legend entry for EACH data group in the plot. Each entry is rendered
    * as a rectangular bar in the parent graph's automated legend. The bar is stroked IAW the node's stroke properties, 
    * but its fill color and legend label are the particular values assigned to the corresponding data group.
    */
   @Override public int getNumLegendEntries()  
   { 
      if(!hasDataGroups()) return(super.getNumLegendEntries());
      
      if(!getShowInLegend()) return(0);
      return(getNumDataGroups()); 
   }
   
   @Override public List<LegendEntry> getLegendEntries()
   {
      if(!hasDataGroups()) return(super.getLegendEntries());
      
      if(!getShowInLegend()) return(null);
      fixDataGroupInfo();
      List<LegendEntry> out = new ArrayList<>();
      int n = getNumDataGroups();
      for(int i=0; i<n; i++) out.add(new LegendEntry(this, dataGrpInfo.get(i)));
      return(out);
   }
   
   /**
    * This override ensures that any data group information for the clone is an independent copy of this node's data
    * group information. The clone will reference the same data set, however.
    */
   @Override protected FGNPlottableData clone() throws CloneNotSupportedException
   {
      FGNPlottableData copy = (FGNPlottableData) super.clone();
      copy.dataGrpInfo = null;
      if(dataGrpInfo != null)
      {
         copy.dataGrpInfo = new ArrayList<>();
         for(GrpInfo gi : dataGrpInfo) copy.dataGrpInfo.add(new GrpInfo(gi.fillC, gi.label));
      }
      return(copy);
   }
}
