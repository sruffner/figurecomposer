package com.srscicomp.fc.matlab;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.srscicomp.common.util.Point3D;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;
import com.srscicomp.fc.fig.ContourNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.fig.PieChartNode;
import com.srscicomp.fc.fig.StrokePattern;


/**
 * <code>HGObject</code> is a Java representation of a Matlab "Handle Graphics" (HG) object that has been converted to a
 * structure by the <i>handle2struct()</i> routine. It is based on a review of the relevant Mathworks documentation as 
 * well as an analysis of a number of figures in Matlab 7.13.0.564 (R2011b) and 8.4.0.150421 (R2014b).
 * 
 * <p>When a figure handle is passed to <i>handle2struct</i>, the figure's HG object hierarchy is preserved in a tree
 * of structures. Each object's structure has 5 fields: 'type', 'handle', 'properties', 'children', and 'special', where
 * 'children' is a structure array listing all the HG objects that are children of this object. <code>HGObject</code> 
 * duplicates this same construct, using a list of <code>HGObject</code>s to represent the 'children' field. Object 
 * properties are represented by a hash map keyed by property names, which correspond to field names in the structure 
 * field called 'properties'. Each property value is a {@link Object}.</p>
 * 
 * <p><code>HGObject</code> was written expressly for the purpose of converting figures in Matlab to <i>DataNav</i> 
 * FypML format to the extent possible, given the MANY differences in features and internal structure between the two
 * applications' figure models. A companion Matlab function first constructs the <code>HGObject</code> hierarchy for a
 * figure rendered in Matlab, selecting only those HG objects and properties that can be replicated in a FypML figure
 * (alternatively, the <code>HGObject</code> tree may be parsed directly from a Matlab FIG file that is to be imported
 * as a FypML figure -- see {@link MatlabFigureImporter#importMatlabFigureFromFile(File, StringBuffer)}). It then passes
 * the root of this HG object tree to {@link MatlabUtilities#matFigToFyp(HGObject, double, StringBuffer, boolean)}, 
 * which traverses the tree and constructs a FypML figure based on its contents.</p>
 * 
 * @author sruffner
 */
public class HGObject
{
   /**
    * Construct a Matlab Handle Graphics object with the specified type and handle ID. Initially, the object will have
    * no properties and no children.
    * 
    * <p>The third argument warrants some discussion. For 'axes' and 'scribe.colorbar' (which is a specialized 'axes')
    * objects, the defining Matlab structure includes a field named 'special' that is a vector containing the child 
    * indices of 'text' objects representing the 'axes' title and axis labels:  <i>[iTitle iXLabel iYLabel iZLabel]</i>.
    * If any label is undefined, the corresponding index is 0 (which is an invalid Matlab index).</p>
    * <p>When the Matlab-to-FypML conversion function  creates an <code>HGObject</code> for an 'axes' or 
    * 'scribe.colorbar', it checks the 'special' field for valid label indices. Since the function may NOT necessarily 
    * include all child objects during translation to Java, it cannot pass on the child indices. Instead, it passes on 
    * the original handles assigned to the relevant child text objects.</p>
    * @param type The HG object type name.
    * @param handle The object's handle.
    * @param labelHandles An array of four HG handles <i>[hTitle hXLabel hYLabel hZLabel]</i> for child text objects 
    * representing the title and X/Y/Z axis labels for a Matlab 'axes' or 'scribe.colorbar'. A handle value of 0 means
    * that the label child does not exist.
    */
   public HGObject(String type, double handle, double[] labelHandles)
   {
      this.type = type;
      this.handle = handle;
      if(("axes".equals(type) || "scribe.colorbar".equals(type)) && labelHandles != null && labelHandles.length >= 4)
      {
         hTitle = labelHandles[0];
         hXLabel = labelHandles[1];
         hYLabel = labelHandles[2];
         hZLabel = labelHandles[3];
      }
      properties = new HashMap<>();
      children = new ArrayList<>();
   }
   
   /**
    * Add an explicitly defined property to this Matlab Handle Graphics object. When an HG object is converted to a 
    * structure array S via the Matlab function <i>handle2struct</i>, all explicitly defined properties of that object
    * are generally reported in the structure S.properties, where the property name is a field and the property value is
    * the value assigned to that field. Different properties have different values, which is why we use a generic Java 
    * {@link Object} to represent the value. 
    * 
    * @param name The property name.
    * @param value The property value.
    */
   public void putProperty(String name, Object value) { properties.put(name, value); }
   
   /**
    * Append a Matlab Handle Graphics object to this object's children list.
    * 
    * @param child The child object. Must not be null.
    */
   public void addChild(HGObject child) 
   {
      if(child == null) throw new IllegalArgumentException("Null child object!");
      children.add(child); 
   }
   
   /**
    * Do "post-processing" on this Handle Graphics object. This method should be called only AFTER populating the
    * sub-tree rooted at this object. Currently, this method takes no action unless the object type is 'axes' or 
    * 'specgraph.barseries'.
    * 
    * <p>If this object is a bar series, the method will examine its single 'patch' child to calculate several 
    * properties which are later checked when coalescing bar series objects within the same parent 'axes'. For details,
    * see {@link #postProcessBarSeries()}.</p>
    * 
    * <p>If this object is a contour plot group ("specgraph.contourgroup"), its children are a set of 'patch' objects, 
    * each of which renders one contour path. If the 'LevelList' property is not explicitly set on the contour object, 
    * then this method will examine all of the 'patch' children to enumerate all the contour levels. See {@link 
    * #postProcessContourGroup()}.</p>
    * 
    * <p>If this object is an 'axes', the method checks whether or not it is configured to display a 3D view of data
    * projected onto a 2D screen. See {@link #checkFor3DPlot()}. There is only limited support for converting 3D Matlab
    * plots to 3D graph nodes in FypML. If the 3D graph contains any Matlab "surface" children, then those are examined
    * to see if they are the product of the bar3() function, which renders 3D bars. Any "surface" child that does not
    * render a 3D bar is removed; also, like "surface" kids may be coalesced into a single "surface" with a backing
    * data set. A "surface" object in a 3D graph is translated into a FypML 3D scatter plot with a bar plot-like 
    * display mode. For details, see {@link #postProcess3DPlot()}. Note that 3D surfaces generated by the surf() 
    * function have a unique object type, "graph3d.surfaceplot", even though it has all the same properties as a 
    * "surface". "Graph3d.surfaceplot" objects are not coalesced (usually there's just one per 3D graph!), and they are
    * converted to a FypML surface node.</p>
    * 
    * <p>If the 'axes' is 2D, the method then examines its children to determine whether or not it has been
    * rendered as a polar plot, as might be produced by the Matlab plotting functions <i>polar()</i>, <i>rose()</i>, or 
    * <i>compass()</i>. The following conditions must be met:
    * <ul>
    * <li>The 'Visible' property is set to 'off' (the regular Cartesian axes are not drawn).</li>
    * <li>The 'XLim' and 'YLim' properties are such that <i>abs(xMin) = xMax, abs(yMin) == yMax, and [yMin yMax] = 
    * 1.15 * [xMin xMax]</i>.</li>
    * <li>The children follow this pattern: First child is a 'patch', followed by N ('line', 'text') pairs, followed by
    * exactly 6 'line' objects and then 12 'text objects. These render the background, circles, radial lines, and 
    * associated tick mark labels for Matlab's version of a polar coordinate grid.
    * </ul>
    * If these conditions are satisfied, the method flags the 'axes' as a polar plot. It will be converted into a FypML
    * polar plot node ('pgraph', introduced in FC 5.1.2) with a theta axis range of [0 360] deg and tick interval of 30 
    * deg, a radial axis range of [0 xMax] and a tick interval of xMax/N. The 'patch' and 'line' objects used to render 
    * the polar grid are discarded from the 'axes' object's child list, as are the text objects representing the grid
    * labels, since these are rendered automatically by the FypML polar plot node. Each remaining 'graph2d.lineseries' 
    * child is then examined to see if it represents the output of the Matlab <i>rose()</i> function or one of the 
    * vectors drawn by the <i>compass()</i> function. If so, they are tagged accordingly; these tags are checked during 
    * plot coalescing (all vectors in one compass plot are coalesced into a single line-series, while rose plots are 
    * never coalesced) and when translating the plot object to a FypML trace node.</p>
    * 
    * <p>Next, the method will coalesce similar child plot objects into a single plot object backed by a data set that 
    * includes all the data in the individual plot objects. For details on the relevant use cases and how the coalesced
    * plot objects are rendered in the <i>FypML</i> figure, see {@link #coalesce(HGObject, HGObject)}. The four Matlab 
    * HG plot object types that may be coalesced into a single object are 'line', 'graph2d.lineseries', 
    * 'specgraph.barseries', and 'specgraph.areaseries'.</p>
    */
   public void afterChildrenAdded()
   {
      // handle post-processing of a barseries or contour group object
      if(type.equals("specgraph.barseries"))
      {
         postProcessBarSeries();
         return;
      }
      else if(type.equals("specgraph.contourgroup"))
      {
         postProcessContourGroup();
         return;
      }
      
      // handle post-processing of an axes object
      if(!type.equals("axes")) return;
      
      // check for and post-process an "axes" configured as a 3D plot. These are handled differently than 2D plots...
      checkFor3DPlot();
      if(is3D)
      {
         postProcess3DPlot();
         return;
      }
      
      // for 2D stem plots, the baseline value may not be available in the 'specgraph.stemseries' object, so here we
      // check for a 'specgraph.baseline' child and assign its 'BaseValue' to any and all 'stemseries' objects.
      get2DStemPlotBaseLine();
      
      // check if the definition of 'axes' object is consistent with a polar plot. If so, the first N children render
      // the polar coordinate grid lines and associated tick mark labels. We remove all of these child objects, since 
      // the polar grid and labels are rendered automatically by the FypML polar plot node. We save a reference to one 
      // grid 'line' object in an internal member so that the line's 'Color', 'LineStyle', and 'LineWidth' properties 
      // can be read. We assume all 'line' objects have the same styling.
      checkForPolarPlot();
      if(isPolarAxes)
      {
         polarGridLine = children.get(1);
         int nRmv = 1 + 2*nRadialTicks + 18;
         while((!children.isEmpty()) && nRmv > 0)
         {
            children.remove(0);
            nRmv--;
         }         
         tagRoseAndCompassPlotObjects();
      }
      
      // detect and pre-process a pie chart as generated by Matlab's pie() function
      if(!isPolarAxes) detectAndProcessPieChart();
      
      // coalesce matching plot objects under certain circumstances...
      List<HGObject> retained  = new ArrayList<>();
      
      for(HGObject kid : children) 
      {
         boolean retain = true;
         if(kid.type.equals("graph2d.lineseries") || kid.type.equals("line") || kid.type.equals("specgraph.barseries")
               || kid.type.equals("specgraph.areaseries") || kid.type.equals("specgraph.scattergroup"))
         {
            for(HGObject plot : retained) if(plot.coalesce(kid, this))
            {
               retain = false;
               break;
            }
         }
         
         if(retain) retained.add(kid);
      }
      
      if(retained.size() != children.size())
      {
         children.clear();
         children = retained;
      }
   }

   /**
    * Get this Matlab Handle Graphics object's type name (eg: "figure", "axes", "text", and so on).
    * @return The object's type.
    */
   public String getType() { return(type); }
   
   /**
    * Get the numeric handle that was assigned to this Matlab Handle Graphics object when the Matlab figure was
    * converted from a HG hierarchy to structure form.
    * @return The object handle.
    */
   public double getHandle() { return(handle); }
   
   /**
    * Is the specified property explicitly specified for this Matlab Handle Graphics object? This is needed to 
    * distinguish between an implicit property (unspecified) and a null-valued explicit property.
    * @param name The property name.
    * @return True if specified property is among those that were explicitly specified for this HG object.
    */
   public boolean hasProperty(String name) { return(properties.containsKey(name)); }
   
   /**
    * Get the value of the property specified for this Matlab Handle Graphics object.
    * @param name A property name.
    * @return The property value. Returns null if property value was an empty Matlab array OR if the property was not
    * explicitly specified. Call {@link #hasProperty(String)} to distinguish between these two possibilities.
    */
   public Object getProperty(String name) { return(properties.get(name)); }
   
   /**
    * Get the list of properties that were explicitly specified for this Matlab Handle Graphics object.
    * @return List of names of all explicitly specified object properties.
    */
   public List<String> getExplicitProperties() { return(new ArrayList<>(properties.keySet())); }
   
   /**
    * Get the number of child objects contained by this Matlab Handle Graphics object.
    * @return The child count.
    */
   public int getChildCount() { return(children.size()); }
   
   /**
    * Get the Matlab Handle Graphics object at the specified index in this object's child list.
    * @param i Child index.
    * @return The HG object at that index.
    */
   public HGObject getChildAt(int i) { return(children.get(i)); }
   
   /**
    * Get the 'String' property of this Matlab HG 'text' object.
    * 
    * <p>The 'String' property of the 'text' object may be a character string (including a single character), a cell
    * array of strings, or a padded string matrix. When converted to Java, the property value may be any of three 
    * classes: String, Character, or String[]. This method handles all 3 possibilities. If the property value is a
    * string array, then it is assumed it represents the label text for a multi-line FypML text label, or the 
    * multi-line label of a FypML axis. The string returned is a concatenation of all of the lines, with a line-feed
    * character separating them.</p>
    * 
    * @return The string value in this text object's 'String' property, with multi-line strings concatenated with
    * line-feed characters. Returns null if this is not a 'text' object.
    */
   public String getTextStringProperty()
   {
      String out = null;
      Object strProp = getProperty("String");
      if("text".equals(type) && strProp != null)
      {
         Class<?> strC = strProp.getClass();
         if(strC.equals(String.class)|| strC.equals(Character.class))
            out = strProp.toString().trim();
         else if(strC.equals(String[].class))
         {
            String[] lines = (String[]) strProp;
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<lines.length; i++)
            {
               sb.append((lines[i] != null) ? lines[i].trim() : "");
               if(i < lines.length - 1) sb.append("\n");
            }
            out = sb.toString().trim();
         }
      }
      return(out);
   }
   

   /**
    * Get the title for this Handle Graphics object. This applies only to an 'axes', 'matlab.graphics.axis.PolarAxes', 
    * or 'scribe.colorbar' object. The method searches the object's children for a 'text' object whose handle matches 
    * the title handle specified when the object was constructed. The child's 'String' property value is returned.
    * 
    * <p><b>Matlab R2014b</b>: The title is stored as a full-blown Matlab Text object in the new 'axes' property called 
    * 'Title'. When a HG figure hierarchy is converted to structure form via <i>handle2struct()</i>, this property is 
    * NOT set, and a child HG 'text' object representing the title label may or may not be present. The Matlab function 
    * matfig2fyp() runs in the Matlab environment and thus has access to the 'Title' property. It will extract the text 
    * string and store it in a manufactured property called 'Title_Str'. If this method finds no child 'text' object 
    * identified as the title label, then it will check for this property.</p>
    * 
    * <p>Note that the newer 'PolarAxes' object, when converted via <i>handle2struct()</i>, will NOT have a child 'text'
    * object containing the title string; it only has a Matlab Text object stored in the 'Title' property.</p>
    * 
    * @return The title string, or null if this is not an 'axes', 'PolarAxes' or 'scribe.colorbar' object, or if no 
    * title is defined.
    */
   public String getAxesTitle()
   {
      String title = null;
      if(hTitle != 0)
      {
         for(HGObject child : children) if(child.getHandle() == hTitle)
         {
            title = child.getTextStringProperty();
            break;
         }
      }
      if(title == null) title = getStringValuedProperty(this, "Title_Str");
      return(title);
   }
   
   /**
    * Get the X-axis label for this Handle Graphics object. This applies only to an 'axes' or 'scribe.colorbar' object. 
    * The method searches the object's children for a 'text' object whose handle matches the X-axis label handle 
    * specified when the 'axes' object was constructed. The child's 'String' property value is returned.
    * 
    * <p><b>Matlab R2014b</b>: The X-axis label is now stored as a full-blown Matlab Text object in the new 'axes'
    * property called 'XLabel'. When a HG figure hierarchy is converted to structure form via <i>handle2struct()</i>,
    * this property is NOT set, and a child HG 'text' object representing the X-axis label may or may not be present.
    * The Matlab function matfig2fyp() runs in the Matlab environment and thus has access to the 'XLabel' property. It
    * will extract the text string and store it in a manufactured property called 'XLabel_Str'. If this method finds no
    * child 'text' object identified as the X-axis label, then it will check for this property.</p>
    * 
    * @return The X-axis label string, or null if this is not an 'axes' or 'scribe.colorbar' object, or if no X-axis
    * label is defined.
    */
   public String getXLabel()
   {
      String label = null;
      if(hXLabel != 0)
      {
         for(HGObject child : children) if(child.getHandle() == hXLabel)
         {
            label = child.getTextStringProperty();
            break;
         }
      }
      if(label == null) label = getStringValuedProperty(this, "XLabel_Str");
      return(label);
   }
   
   /**
    * Get the Y-axis label for this Handle Graphics object. This applies only to an 'axes' or 'scribe.colorbar' object. 
    * The method searches the object's children for a 'text' object whose handle matches the Y-axis label handle 
    * specified when the 'axes' object was constructed. The child's 'String' property value is returned.
    * 
    * <p><b>Matlab R2014b</b>: The Y-axis label is now stored as a full-blown Matlab Text object in the new 'axes'
    * property called 'YLabel'. When a HG figure hierarchy is converted to structure form via <i>handle2struct()</i>,
    * this property is NOT set, and a child HG 'text' object representing the Y-axis label may or may not be present.
    * The Matlab function matfig2fyp() runs in the Matlab environment and thus has access to the 'YLabel' property. It
    * will extract the text string and store it in a manufactured property called 'YLabel_Str'. If this method finds no
    * child 'text' object identified as the Y-axis label, then it will check for this property.</p>
    * 
    * @return The Y-axis label string, or null if this is not an 'axes' or 'scribe.colorbar' object, or if no Y-axis
    * label is defined.
    */
   public String getYLabel()
   {
      String label = null;
      if(hYLabel != 0)
      {
         for(HGObject child : children) if(child.getHandle() == hYLabel)
         {
            label = child.getTextStringProperty();
            break;
         }
      }
      if(label == null) label = getStringValuedProperty(this, "YLabel_Str");
      return(label);
   }
   
   /**
    * Get the Z-axis label for this Handle Graphics object. This applies only to an 'axes' or 'scribe.colorbar' object. 
    * The method searches the object's children for a 'text' object whose handle matches the Z-axis label handle 
    * specified when the 'axes' object was constructed. The child's 'String' property value is returned.
    * 
    * <p><b>Matlab R2014b</b>: The Z-axis label is now stored as a full-blown Matlab Text object in the new 'axes'
    * property called 'ZLabel'. When a HG figure hierarchy is converted to structure form via <i>handle2struct()</i>,
    * this property is NOT set, and a child HG 'text' object representing the Z-axis label may or may not be present.
    * The Matlab function matfig2fyp() runs in the Matlab environment and thus has access to the 'ZLabel' property. It
    * will extract the text string and store it in a manufactured property called 'ZLabel_Str'. If this method finds no
    * child 'text' object identified as the Z-axis label, then it will check for this property.</p>
    * 
    * @return The Z-axis label string, or null if this is not an 'axes' or 'scribe.colorbar' object, or if no Z-axis
    * label is defined.
    */
   public String getZLabel()
   {
      String label = null;
      if(hZLabel != 0)
      {
         for(HGObject child : children) if(child.getHandle() == hZLabel)
         {
            label = child.getTextStringProperty();
            break;
         }
      }
      if(label == null) label = getStringValuedProperty(this, "ZLabel_Str");
      return(label);
   }
   
   /**
    * Does the specified Handle Graphics object handle correspond to the child 'text' object representing an X, Y or Z
    * axis label for this 'axes' or 'scribe.colorbar' object?
    * @param handle The object handle to check.
    * @return True iff it meets the criteria described above. Returns false if this is HG object is neither an 'axes'
    * nor a 'scribe.colorbar'.
    */
   public boolean isAxesLabelHandle(double handle)
   {
      return((handle != 0) && (handle == hXLabel || handle == hYLabel || handle == hZLabel));
   }
   
   /**
    * Does the specified Handle Graphics object handle correspond to the child 'text' object representing the title of
    * this 'axes' or 'scribe.colorbar' object?
    * @param handle The object handle to check.
    * @return True if handle is the child handle for the title. Returns false if this is HG object is neither an 'axes'
    * nor a 'scribe.colorbar'.
    */
   public boolean isAxesTitleHandle(double handle) { return(handle != 0 && handle == hTitle); }
   
   /** 
    * Get handle of child object representing the title of this 'axes' or 'scribe.colorbar' Handle Graphics object.
    * @return The child handle; 0 if child not present, or this is neither an 'axes' or 'scribe.colorbar' object.
    */
   public double getTitleHandle() { return(hTitle); }
   /** 
    * Get handle of child representing the X-axis label of this 'axes' or 'scribe.colorbar' Handle Graphics object.
    * @return The child handle; 0 if child not present, or this is neither an 'axes' or 'scribe.colorbar' object.
    */
   public double getXLabelHandle() { return(hXLabel); }
   /** 
    * Get handle of child representing the Y-axis label of this 'axes' or 'scribe.colorbar' Handle Graphics object.
    * @return The child handle; 0 if child not present, or this is neither an 'axes' or 'scribe.colorbar' object.
    */
   public double getYLabelHandle() { return(hYLabel); }
   /** 
    * Get handle of child representing the Z-axis label of this 'axes' or 'scribe.colorbar' Handle Graphics object.
    * @return The child handle; 0 if child not present, or this is neither an 'axes' or 'scribe.colorbar' object.
    */
   public double getZLabelHandle() { return(hZLabel); }
   
   /**
    * If this Matlab Handle graphics object is one of the supported plot object types listed below, extract the 
    * FypML-compatible raw data set from its relevant properties.
    * <ul>
    * <li>Plot object type = "graph2d.lineseries":
    *    <ul>
    *    <li>The plot object's <b>XData, YData</b> properties contain the data set's X- and Y-coordinates. The 
    *    method expects both to be N-vectors, and the data set returned will be one of two FypML-supported data set 
    *    formats -- a <i>ptset</i>  or <i>series</i>. No support for error data here.</li>
    *    <li>If <b>XData</b> property is implicit and <b>XDataMode</b> property is implicit or set to "auto", then it is
    *    assumed that the X-coordinates are simply the sequence <i>1:N</i>, where <i>N = length(<b>YData</b>)</i>. In 
    *    this use case, the returned data set will be a <i>series</i> with <i>x0 = 1</i> and <i>dx = 1</i>. If 
    *    <b>XDataMode = "manual"</b> (or if <b>XData</b> is available -- Matlab 2014b does not store <b>XDataMode</b>
    *    when <b>XData</b> is explicitly set), the  returned data set will be a <i>series</i> only if the X-coordinate 
    *    values listed in the N-vector can be  represented as a sequence <i>x(n) = x0 + dx*n</i>.</li>
    *    <li><b>Special case: 3D line plot.</b> If the object also has <b>ZData</b> property, and the 3 properties
    *    <b>XData, YData, ZData</b> are ALL vectors of the same length N, then the line series object is assumed to
    *    represent a 3D line plot (from Matlab's plot3() function), and an XYZSET data set is returned.</li>.
    *    <li><b>Special case: Coalesced multi-set.</b> If an HG "axes" contains multiple "graph2d.lineseries" objects
    *    all of which share the same X-coordinate data vector and the same appearance, then {@link #addChild(HGObject)} 
    *    will coalesce these into a single line series object with an internal list of the Y-coordinate data vectors
    *    coalesced from the individual line series. In this case, the method returns an MSET or MSERIES data set, 
    *    depending on the nature of the common <b>XData</b>.</li>
    *    <li><b>Special case: Coalesced scatter plot.</b> If an HG "axes" contains multiple "graph2d.lineseries" objects
    *    all of which have the same appearance and contain just a single data point, then {@link #addChild(HGObject)} 
    *    will coalesce these into a single line series object with an internal list of the coalesced data points.
    *    In this case, the method returns the entire list of points as a two-column PTSET.</li>
    *    <li><b>Special case: Compass plot.</b> Matlab renders a compass plot as a set of vectors emanating
    *    from the origin of a parent "axes" that is in a polar grid configuration. Each vector is represented by a 
    *    separate 5-point line-series object. These are coalesced into a single line-series object with an internal
    *    collection of the vector endpoints. In this case, the method returns the entire list of points as a two-column 
    *    PTSET. The points are converted from Cartesian to polar coordinates (data in a Matlab polar plot is always in
    *    Cartesian coordinates; the Cartesion axis lines are simply hidden and the plots are drawn on top of polar 
    *    grid; in FypML, the data needs to be in polar coordinates).</li>
    *    <li><b>Special case: Rose plot, or angle histogram.</b> Matlab's <i>rose()</i> function renders an angle
    *    histogram with a single line-series object within a parent "axes" that is in a polar grid configuration. The
    *    underlying data contains 4N points, where N is the number of bins in the histogram and each set of 4 points
    *    draws a single bin as a pie wedge emanating from the origin, but with a straight line connecting the endpoints
    *    of the two radii, instead of a circular arc as in FypML: (0, 0), (x1, y1), (x2, y2), (0, 0). In this case, 
    *    the method returns a single-column SERIES data set with DX=2*pi/N, X0=pi/N, and the Y values set to the
    *    radius of each bin: sqrt(x1*x1 + y1*y1) == sqrt(x2*x2 + y2*y2).</li>
    *    </ul>
    * </li>
    * <li>Plot object type = "specgraph.barseries": Treated exactly like "graph2d.lineseries", except there are no
    * special cases other than the multi-set case. In fact, typically a "specgraph.barseries" object will coalesce the
    * Y-data from all the 'barseries' objects that are part of the same Matlab bar plot, so we'll see the multi-set
    * case most often for this plot object type.</li>
    * <li>Plot object type = "specgraph.stairseries: Treated exactly like "graph2d.lineseries", except there are no
    * special cases and no coalescing. Each stair series object is converted into a separate FypML trace node.</li>
    * <li>Plot object type = "specgraph.areaseries": Analogous to a 'barseries', except that both coalesced and 
    * singleton 'areaseries' objects are converted to <i>FypML</i> area chart nodes.</li>
    * <li>Plot object type = "line": When a 'line' object contains 3 or more data points, is a coalesced set of multiple
    * HG 'line' objects, or has a defined marker symbol, then it will be translated into a FypML trace node.
    *    <ul>
    *    <li>The line object's <b>XData, YData</b> properties contain the data set's X- and Y-coordinates. Both will be
    *    N-vectors, and the data set returned is always a two-column PTSET.</li>
    *    <li><b>Special case: Coalesced 'line' objects.</b> If an HG "axes" contains multiple "line" objects all of 
    *    which share the same appearance and contain 3+ data points each (or use a marker symbol), then {@link 
    *    #addChild(HGObject)} will coalesce these into a single 'line' object with an internal list of the data points
    *    {X,Y} defining the coalesced lines. An undefined point (NaN, NaN) separates one line from the next -- so that
    *    the lines will be disjoint when rendered in FypML. The entire list of coalesced data is again returned as a 
    *    two-column PTSET.</li>
    *    </ul>
    * </li>
    * <li>Plot object type = "specgraph.errorbarseries":
    *    <ul>
    *    <li>The plot object's <b>XData, YData</b> properties contain the data points <i>{x,y}</i>, while other 
    *    properties contain the error bar lengths below and above each data point. Prior to Matlab R2016b, only vertical
    *    error bars were supported, with the error bar deltas {yNeg, yPos} in <b>LData, UData</b>. As of R2016, 
    *    horizontal and vertical error bars are supported. <b>LData, UData</b> are considered obsolete, replaced by 
    *    <b>YNegativeDelta, YPositiveDelta</b>, and the horizontal error bar deltas are defined in <b>XNegativeDelta,
    *    XPositiveDelta</b>. Prior to R2016b, <b>X,Y,L,UData</b> were all N-vectors, where N is the number of data 
    *    points; the <b>X*Delta</b> and <b>Y*Delta</b> properties, on the other hand, will either be vectors with the
    *    same length as <b>YData</b>, or empty arrays; an empty array indicates that no error bars are drawn for that
    *    direction.</li>
    *    <li>If <b>XData</b> property is implicit and <b>XDataMode</b> property is implicit or set to "auto", then it is
    *    assumed that the X-coordinates are simply the sequence <i>1:N</i>, where <i>N = length(<b>YData</b>)</i>. In 
    *    this use case, the returned data set will be a <i>series</i> with <i>x0 = 1</i> and <i>dx = 1</i> -- unless 
    *    horizontal error bars are present. If <b>XDataMode = "manual"</b> (or if <b>XData</b> is available -- Matlab 
    *    2014b does not store <b>XDataMode</b> when <b>XData</b> is explicitly set), the returned data set will be a 
    *    <i>series</i> only if there are no horizontal error bars AND the X-coordinate values listed in the N-vector can
    *    be  represented as a sequence <i>x(n) = x0 + dx*n</i>.</li>
    *    <li>Note that FypML does NOT support unsymmetrical error bars, while Matlab clearly does. If upper/left error
    *    bar length does not match the lower/right error bar at any given data point, then technically we cannot 
    *    reproduce the error bar series accurately in FypML. We choose instead to take the larger error bar length as 
    *    the standard deviation in X or Y: <i>yStd = max(yLo, yHi)</i>.</li>
    *    <li>FypML CAN reproduce one-sided error bars. When examining the error bar delta properties, this method 
    *    takes the absolute value of each delta, since these should be non-negative. If any delta is zero or NaN, then
    *    that constitutes a one-sided error bar. Note that <b>X*Delta, Y*Delta</b> may be empty arrays, indicating that
    *    all the error bars for a given direction are one-sided (or not present at all).</li>
    *    <li>If there are no horizontal error bars and the x-coordinates of the data points are regularly spaced, the
    *    extracted data set will be a <i>series</i>; otherwise, it will be a <i>ptset</i>. The length of the tuple
    *    will depend on whether there are horizontal error bars and whether any one-sided error bars are defined. If 
    *    there are no horizontal error bars nor one-sided vertical error bars, then each tuple is <i>[y yStd]</i> for a
    *    <i>series</i>, and <i>[x y yStd]</i> for a <i>ptset</i>. If there are no horizontal error bars but at least one
    *    one-sided error bar in Y, then each tuple will be <i>[y yStd ye]</i> or <i>[x y yStd ye]</i>. If there are any
    *    horizontal error bars, then each tuple will be <i>[x y yStd ye xStd]</i> or <i>[x y yStd ye xStd xe]</i>, the 
    *    latter used if any horizontal error bar is one-sided.</i>
    *    </ul>
    * </li>
    * <li>Plot object type = "specgraph.scattergroup": Matlab generates this plot object for both 2D and 3D scatter and
    * "bubble" plots. The plot object's <b>XData, YData, ZData</b> properties contain the X,Y,Z coordinates of the data,
    * but <b>ZData</b> will be an empty vector in the 2D case. The <b>SizeData</b> and <b>CData</b> properties control
    * the size and color of the "bubbles" in a bubble plot, and they are parsed to extract the Z data in the 2D case and
    * the 4th data dimension "W" in the 3D case (a 3D bubble plot in FypML and Matlab is a means of representing a 4D
    * data set):
    *    <ul>
    *    <li><i>2D</i>: Let <b>XData, YData</b> be N-vectors of the same length N. If the object's <b>CData</b> or 
    *    <b>SizeData</b> property is also an N-vector, then that vector is treated as the Z-coordinate data. If both are
    *    N-vectors, then <b>CData</b> is used (which means the FypML scatter plot could look quite different than the
    *    original Matlab scatter plot). The extracted data set will be an XYZSET. If neither are N-vectors, then there
    *    is no Z-coordinate data, and the extracted data set will be a PTSET.</li>
    *    <li><i>3D</i>: <b>XData, YData, ZData</b> must all be N-vectors of the same length N. In this scenario, if 
    *    <b>CData</b> or <b>SizeData</b> is also an N-vector, then that vector is treated as "W"-coordinate data, and
    *    the extracted data set is a 4D XYZWSET. If both <b>CData</b> and <b>SizeData</b> are N-vectors, then the former
    *    will serve as the source for W-coordinate data, in which case the FypML 3D scatter plot could look quite 
    *    different from the Matlab version. If neither are N-vectors, then the extracted data set is a 3D XYZSET.</li>
    *    </ul>
    * </li>
    * <li>Plot object type = "bubblechart": Matlab introduced this chart object in R2020b as a better way to generate
    * bubble plots. The data set extraction is very similar to that for "specgraph.scattergroup", except that:
    *    <ol>
    *       <li><b>SizeData</b> is preferred over <b>CData</b> as the Z-coordinate data when both are N-vectors.</li>
    *       <li>If the HG object tree was prepared via matfig2fyp() or savefigasis(), each "bubblechart" object will
    *       have a 4x1 vector [D1 D2 L1 L2] containing the bubble size diameter range [D1 D2] in points and the range
    *       restriction [L1 L2] on data in the SizeData vector (from the Matlab bubblesize() and bubblelim() methods,
    *       respectively. The Z-coordinate data in SizeData is range-restricted to [L1 L2].</li>
    *    </ol>
    * Note that we CANNOT reproduce the appearance of a bubblechart entirely because of the way the FypML scatter and
    * scatter3d nodes use the Z-coordinate data to determine the symbol size at each data point.
    * </li>
    * <li>Plot object type = "specgraph.scattergroup", coalesced: If a Matlab 'axes' contains multiple scatter groups 
    * in which the same symbol (size and appearance identical) is drawn at each scatter point, then these scatter groups
    * are coalesced into a single object, with the 'XData' and 'YData' vectors (and 'ZData' vectors for the 3D case) 
    * collected internally. This collection of scatter point coordinates across the coalesced scatter groups are merged
    * into a single PTSET (2D case) or XYZSET (3D case) data source for the <i>scatter</i> or <i>scatter3d</i> node
    * created.</li>
    * <li>Plot object type = "specgraph.stemseries": Matlab generates this plot object for both 2D and 3D stem plots. In
    * the 3D case, the <b>XData, YData, ZData</b> contain the X,Y,Z coordinates of the data. All must be vectors of the
    * same length N, and the extracted data set will be an <i>xyzset</i> constructed in a straightforward manner from
    * the contents of these vectors. In the 2D case, <b>ZData</b> will be empty, and the extracted data set will be a
    * <i>ptset</i> constructed from the contents of the N-vectors in <b>XData, YData</b>.</li>
    * <li>Plot object type = "surface" or "image" -- in 2D graphs only.
    *    <ul>
    *    <li><code>XData,YData,ZData</code> specify the vertex coordinates (x,y,z) in 3D, while <code>CData</code> 
    *    specifies the vertex colors. This potentially 3D surface is collapsed into a 2D heat map image by completely 
    *    ignoring <code>ZData</code> and taking the <i>xyzimg</i> data from <code>CData</code>.</li>
    *    <li>Note that Matlab supports two color-mapping modes: <code>CDataMapping = "scaled" | "direct"</code>. 
    *    <i>DataNav</i> only supports the "scaled" mode, in which the vertex color data are mapped linearly onto a 
    *    defined color map; if "direct" mode is specified, <b>no data set will be extracted</b>. Otherwise, the raw data
    *    in MxN matrix <code>CData</code> provides the raw data for an MxN <i>xyzimg</i> data set.</li>
    *    <li><code>CData</code> could be a MxNx3 matrix, in which case it contains the true RGB color for each point in
    *    MxN space. FypML doesn't support this; in fact, the import engine does not support reading in a matrix with 
    *    more than 2 dimensions.</li>
    *    <li><code>XData</code> may be a row vector of length N or an MxN matrix; in the latter case, we only look at 
    *    the first row. It is assumed the row vector contains monotonically increasing or decreasing X-coordinates, so
    *    that <code>[XData(1), XData(end)]</code> specify the range of the <i>xyzimg</i> data along the X-axis.</li>
    *    <li><code>YData</code> may be a column vector of length M or an MxN matrix; in the latter case, we only look at 
    *    the first column. We assume the column vector contains monotonically increasing or decreasing Y-coordinates, so
    *    that <code>[YData(1), YData(end)]</code> specify the range of the <i>xyzimg</i> data along the Y-axis.</li>
    *    </ul>
    * </li>
    * <li>Plot object type = "surface" -- in a 3D graph: Matlab's bar3() function generates one or more "surface"
    * objects to render the 3D bars. During post-processing of the 3D "axes" parent, these "surface" objects are 
    * analyzed to confirm they render 3D bars, in which case the 3D data points are calculated from the bar vertices
    * and stored in the internal member {@link #bar3DPoints}. During the coalescing stage, "surface" children with the
    * same appearance are coalesced, merging their data points in {@link #bar3DPoints}. The set of 3D points is
    * easily converted to an XYZSET, which serves as the data source for the <i>scatter3d</i> node that is configured
    * to render the 3D bar plot.</li>
    * <li>Plot object type = "graph3d.surfaceplot" -- in 3D graphs only.
    *    <ul>
    *    <li><b>XData,YData,ZData</b> specify the vertex coordinates {(x,y,z)} in 3D. Unlike the 2D case, we ignore 
    *    <b>CData</b> entirely and rely on the contents of these three properties to generate the <i>xyzimg</i> data
    *    set for the FypML 3D surface node.</li>
    *    <li><b>ZData</b> must be an MxN numeric matrix. It provides the raw data for the MxN <i>xyzimg</i> set.</li>
    *    <li>Given that <b>ZData</b> is MxN, <b>XData</b> may be an MxN matrix or an N-vector, and <b>YData</b> may be
    *    an MxN matrix or an M-vector. Regardless, it is ASSUMED that <b>XData</b> and <b>YData</b> simply represent a
    *    regular grid of points in the X-Y plane. We look at the first and last values in each to set the X- and Y-data
    *    ranges [x0 x1 y0 y1] for the <i>xyzimg</i> data set.</li>
    *    </ul>
    * </li>
    * <li>Plot object type = "specgraph.contourgroup": Matlab generates this object for both 2D and 3D contour plots, 
    * but FypML only supports 2D contour plots, via the <i>contour</i> node. The contour node's <i>xyzimg</i> data set
    * is extracted from the <b>ZData,XData,YData</b> properties in the same manner as for "graph3d.surfaceplot".</li>
    * <li>Special case: "patch" object representing pie chart. In Matlab, a pie chart is rendered using a sequence of 
    * "patch", "text" pairs in an "axes". When a pie chart is detected, the "patch" children are consolidated into a
    * single "patch" that represents the pie chart -- see {@link #detectAndProcessPieChart()}. The chart data is merely
    * the sequence of angular extents of the slices, in order. In this scenario, this method returns a SERIES containing
    * this list of angular extents.</li>
    * <li>Plot object type = "histogram": Generated by the Matlab function histogram(), the sample data is in the 
    * <i>Data</i> property, which may be a vector, matrix or multidimensional array. Converted to a RASTER1D set, but
    * only if the Matlab array is 1D or 2D. Otherwise, an empty set is returned.</li>
    * </ul>
    * 
    * <p><i>In polar plots</i>: The plot object is a child of a polar graph, the data set is converted from Cartesian to
    * polar coordinates (with theta in degrees, not radians). Prior to R2016a, Matlab always plotted data in Cartesian 
    * coordinates in an "axes" object configured to look like a polar plot. As of R2016a, a new "polaraxes" object
    * represents a true polar coordinate system, in which any plot objects have data already in polar coordinates, 
    * although the theta coordinate is in radians rather than degrees. Since FypML expects data displayed in a polar 
    * graph to be in polar coordinates with theta in degrees, this method handles the necessary conversions. Also note
    * there's no support for certain HG plot objects in a polar context.</p>
    *
    * @param suggestedID A suggested ID for the returned data set. This is pre-pended with "src_" to form the actual
    * ID string. If the resulting string is not a valid ID, then "src_{id}" is used instead, where {id} is a random 
    * 2-digit integer.</li>
    * @param isPolarPlot True if data is presented in a Matlab polar plot. As of Matlab R2016a, there are two possible
    * scenarios for the parent of the plot object: (1) an "axes" object configured to look like a polar plot; (2) a true
    * "polaraxes" object. In the former case, plot object data is in Cartesian coordinates; in the latter case, the
    * data is in polar coordinates, with the theta coordinate in radians.
    * @param isTruePolarAxes True only if the parent of the plot object is a "polaraxes", not an "axes" object.
    * @return The raw data set extracted from the object. Returns null if this Handle Graphics object is not one of the
    * supported plot objects. In the case of a "surface" object, returns null if the mapping mode is "direct", which
    * is not supported by FypML.
    */
   @SuppressWarnings("DataFlowIssue")
   public DataSet extractDataSetFromPlotObject(String suggestedID, boolean isPolarPlot, boolean isTruePolarAxes)
   {
      if(!(type.equals("graph2d.lineseries") || type.equals("line") || type.equals("specgraph.errorbarseries") ||
            type.equals("specgraph.scattergroup") || type.equals("surface") || type.equals("image") ||
            type.equals("specgraph.barseries") || type.equals("specgraph.areaseries") ||
            type.equals("specgraph.stairseries") || type.equals("specgraph.stemseries") || 
            type.equals("graph3d.surfaceplot") || type.equals("specgraph.contourgroup") ||
            type.equals("bubblechart") || type.equals("histogram") || (type.equals("patch") && isPieChart)))
         return(null);

      // do NOT support specgraph.barseries or specgraph.contourgroup in a FypML polar plot
      if(isPolarPlot && (type.equals("specgraph.barseries") || type.equals("specgraph.contourgroup"))) 
         return(null);
      
      // expect pie chart to be in an "axes" tailored to look like a polar plot
      if((isTruePolarAxes || !isPolarPlot) && type.equals("patch")) return(null);
      
      // generate a data set ID string
      String id = null;
      if(DataSet.isValidIDString(suggestedID))
      {
         id = "src_" + suggestedID;
         if(!DataSet.isValidIDString(id)) id = null;
      }
      if(id == null)
      {
         int n = ((int) (Math.random() * 3000.0)) % 100;
         id = "src_" + n;
      }
      
      DataSet ds = null;
      if(type.equals("graph2d.lineseries") || type.equals("specgraph.barseries") || 
            type.equals("specgraph.areaseries") || type.equals("specgraph.stairseries"))
      {
         // get the Y-coordinate data
         double[] yData = getDoubleVectorFromProperty(getProperty("YData"));
         if(yData == null) throw new UnsupportedOperationException("Data type for 'YData' not supported!");

         // get the X-coordinate data -- or the {dx, x0} params for a series/mseries data set
         // NOTE: It is possible that 'XDataMode' is omitted, implying "auto"; yet 'XData' is specified -- this was the
         // case in a Matlab 2014b FIG file. We no longer assume XData is missing if XDataMode==auto!
         Object prop = getProperty("XDataMode");
         boolean isAuto = (prop == null) || "auto".equals(prop);
         double[] xData = getDoubleVectorFromProperty(getProperty("XData"));
         float[] params = null;
         
         if(xData == null)
         {
            if(!isAuto) throw new UnsupportedOperationException("Data type for 'XData' not supported!");
            else params = new float[] {1, 1};
         }
         else
         {
            if(xData.length != yData.length)
               throw new IllegalStateException("Plot object's 'XData' and 'YData' are not the same length!");
            
            if(xData.length > 2)
            {
               double x0 = xData[0];
               double dx = xData[1] - xData[0];
               boolean isSeries = (dx != 0); 
               for(int i=2; isSeries && i<xData.length; i++) isSeries = (xData[i] == xData[i-1] + dx);
               
               if(isSeries) params = new float[] { (float) dx, (float) x0 };
            }
         }
         
         // get the Z-coordinate data, if any
         double[] zData = getDoubleVectorFromProperty(getProperty("ZData"));
         
         // if this line series contains data coalesced from other matching line series, then build the appropriate
         // data set with the coalesced data (either a multi-set trace or a scatter plot of individual points).
         // Else, create a single point set or series containing the data for this line-series object only. Also handle
         // the various special cases: 3D line plot, rose plot, and compass plot.
         if(type.equals("specgraph.barseries"))
         {
            // the FypML 'bar' element requires MSERIES or MSET data set, even if the bar plot has just one group. If 
            // the 'barseries' object has been coalesced, data for the individual groups has already been coalesced...
            Fmt fmt = (params == null) ? Fmt.MSET : Fmt.MSERIES;
            int nrows = (multiSetData != null) ? multiSetData.get(0).length : yData.length;
            int ncols = (multiSetData != null) ? multiSetData.size() : 1;
            if(fmt == Fmt.MSET) ++ncols;
            
            // remember, the raw data array is filled column-wise: [x1 y11 y21 y31 x2 y12 y22 y32 ...].
            float[] fData = new float[nrows*ncols];
            int iCol = 0;
            if(params == null) 
            {
               for(int i=0; i<nrows; i++) fData[ncols*i] = (float) xData[i];
               ++iCol;
            }
            if(multiSetData != null)
            {
               for(double[] multiSetDatum : multiSetData)
               {
                  for(int i = 0; i < nrows; i++) fData[ncols * i + iCol] = (float) multiSetDatum[i];
                  ++iCol;
               }
            }
            else
            {
               for(int i=0; i<nrows; i++) fData[ncols*i + iCol] = (float) yData[i];
            }
            
            ds = DataSet.createDataSet(id, fmt, params, nrows, ncols, fData);
         }
         else if(zData != null && type.equals("graph2d.lineseries"))
         {
            // 3D line plot: the XData, YData, and ZData properties must all be vectors of equal length
            if(xData == null || (yData.length != zData.length)) return(null);
            
            float[] fData = new float[xData.length*3];
            for(int i=0; i<xData.length; i++)
            {
               fData[3*i] = (float) xData[i];
               fData[3*i+1] = (float) yData[i];
               fData[3*i+2] = (float) zData[i];
            }
            ds = DataSet.createDataSet(id, Fmt.XYZSET, null, xData.length, 3, fData);
         }
         else if(isRosePlot())
         {            
            // (NOTE: we've already verified the 'XData', 'YData' properties conform to the pattern for a rose plot)
            int nBins = yData.length / 4;
            params = new float[] { 360.0f / nBins, 180.0f / nBins };
            float[] fData = new float[nBins];
            for(int i=0; i<nBins; i++)
            {
               double x = xData[4*i + 1];
               double y = yData[4*i + 1];
               fData[i] = (float) Math.sqrt(x*x + y*y);
            }
            
            ds = DataSet.createDataSet(id, Fmt.SERIES, params, nBins, 1, fData);
         }
         else if(multiSetData != null)
         {
            Fmt fmt = (params == null) ? Fmt.MSET : Fmt.MSERIES;
            int nrows = multiSetData.get(0).length;
            int ncols = multiSetData.size();
            if(fmt == Fmt.MSET) ++ncols;
            
            // remember, the raw data array is filled column-wise: [x1 y11 y21 y31 x2 y12 y22 y32 ...].
            float[] fData = new float[nrows*ncols];
            int iCol = 0;
            if(params == null) 
            {
               for(int i=0; i<nrows; i++) fData[ncols*i] = (float) xData[i];
               ++iCol;
            }
            for(double[] multiSetDatum : multiSetData)
            {
               for(int i = 0; i < nrows; i++) fData[ncols * i + iCol] = (float) multiSetDatum[i];
               ++iCol;
            }
            
            // NOTE: Coalescing of line-series containing more than one point is disabled in a polar context, since 
            // converting {x, y1, .., yN} to polar coordinates would, in general, result in different theta coords for
            // each member set!
            
            ds = DataSet.createDataSet(id, fmt, params, nrows, ncols, fData);
         }
         else if(scatterPoints != null)
         {
            // NOTE: this handles both a normal scatter plot in a polar or Cartesian graph, or a compass plot in a
            // polar graph. In the latter case, the scatter points are actually the endpoints of the individual vectors
            // comprising the original compass plot.
            
            int nrows = scatterPoints.size();
            int ncols = 2;
            float[] fData = new float[nrows*ncols];
            for(int i=0; i<nrows; i++)
            {
               Point2D pt = scatterPoints.get(i);
               fData[ncols*i] = (float) pt.getX();
               fData[ncols*i + 1] = (float) pt.getY();
            }
            
            // in a polar context, convert to polar coordinates: { (thetaDeg, r) }
            if(isPolarPlot) for(int i=0; i<nrows; i++)
            {
               double x = fData[ncols*i];
               double y = fData[ncols*i + 1];
               if(Utilities.isWellDefined(x) && Utilities.isWellDefined(y))
               {
                  fData[ncols*i] = (float) Math.toDegrees(Math.atan2(y, x));
                  fData[ncols*i + 1] = (float) Math.sqrt(x*x + y*y);
               }
            }

            ds = DataSet.createDataSet(id, Fmt.PTSET, null, nrows, ncols, fData);
         }
         else if(params != null)
            ds = DataSet.createDataSet(id, Fmt.SERIES, params, yData.length, 1, yData);
         else
         {
            float[] fData = new float[yData.length*2];
            for(int i=0; i<yData.length; i++)
            {
               fData[i*2] = (float) xData[i];
               fData[i*2 + 1] = (float) yData[i];
            }
            
            // in a polar context, convert to polar coordinates: { (thetaDeg, r) }. If it's in a true polar plot,
            // then data is already in polar coordinates -- but theta must be converted from radians to degrees
            if(isPolarPlot && !isTruePolarAxes) for(int i=0; i<yData.length; i++)
            {
               double x = fData[2*i];
               double y = fData[2*i + 1];
               if(Utilities.isWellDefined(x) && Utilities.isWellDefined(y))
               {
                  fData[2*i] = (float) Math.toDegrees(Math.atan2(y, x));
                  fData[2*i + 1] = (float) Math.sqrt(x*x + y*y);
               }
            }
            else if(isPolarPlot && isTruePolarAxes) for(int i=0; i<xData.length; i++)
            {
               fData[2*i] = (float) Math.toDegrees(fData[2*i]);
            }

            ds = DataSet.createDataSet(id, Fmt.PTSET, null, yData.length, 2, fData);
         }
      }
      else if(type.equals("line"))
      {
         // prepare two-column PTSET containing the data points: { (x,y) }
         float[] fData;
         int ncols = 2;
         int nrows;
         if(scatterPoints != null)
         {
            nrows = scatterPoints.size();
            fData = new float[nrows*ncols];
            for(int i=0; i<nrows; i++)
            {
               Point2D pt = scatterPoints.get(i);
               fData[ncols*i] = (float) pt.getX();
               fData[ncols*i + 1] = (float) pt.getY();
            }
         }
         else
         {
            // get the X-coordinate data
            double[] xData = getDoubleVectorFromProperty(getProperty("XData"));
            if(xData == null) throw new UnsupportedOperationException("Data type for 'XData' not supported!");
            
            // get the Y-coordinate data
            double[] yData = getDoubleVectorFromProperty(getProperty("YData"));
            if(yData == null) throw new UnsupportedOperationException("Data type for 'YData' not supported!");
            if(xData.length != yData.length)
               throw new IllegalStateException("Plot object's 'XData' and 'YData' are not the same length!");

            nrows = yData.length;
            fData = new float[nrows*ncols];
            for(int i=0; i<nrows; i++)
            {
               fData[ncols*i] = (float) xData[i];
               fData[ncols*i + 1] = (float) yData[i];
            }
         }
         
         // in a polar context, convert to polar coordinates: { (thetaDeg, r) }
         if(isPolarPlot) for(int i=0; i<nrows; i++)
         {
            double x = fData[ncols*i];
            double y = fData[ncols*i + 1];
            if(Utilities.isWellDefined(x) && Utilities.isWellDefined(y))
            {
               fData[ncols*i] = (float) Math.toDegrees(Math.atan2(y, x));
               fData[ncols*i + 1] = (float) Math.sqrt(x*x + y*y);
            }
         }
         
         ds = DataSet.createDataSet(id, Fmt.PTSET, null, nrows, ncols, fData);
      }
      else if(type.equals("specgraph.errorbarseries"))
      {
         // not supported in a polar context
         if(isPolarPlot) return(null);
         
         // get data point coordinates and error bar deltas in Y, X
         double[] xData = getDoubleVectorFromProperty(getProperty("XData"));
         double[] yData = getDoubleVectorFromProperty(getProperty("YData"));
         double[] lData = getDoubleVectorFromProperty(getProperty("LData"));
         double[] uData = getDoubleVectorFromProperty(getProperty("UData"));
         double[] xNeg = getDoubleVectorFromProperty(getProperty("XNegativeDelta"));
         double[] xPos = getDoubleVectorFromProperty(getProperty("XPositiveDelta"));
         double[] yNeg = getDoubleVectorFromProperty(getProperty("YNegativeDelta"));
         double[] yPos = getDoubleVectorFromProperty(getProperty("YPositiveDelta"));

         // property checks...
         if(yData == null) throw new UnsupportedOperationException("Data type for 'YData' not supported!");
         
         boolean isSeries = false;
         float[] params = null;
         Object prop = getProperty("XDataMode");
         boolean isAuto = (prop == null) || "auto".equals(prop);
         if(xData == null)
         {
            if(!isAuto) throw new UnsupportedOperationException("Data type for 'XData' not supported!");
            isSeries = true;
         }
         else
         {
            if(xData.length != yData.length)
               throw new IllegalStateException("'XData' and 'YData' are must be the same length!");
            if(xData.length > 2 && Utilities.isWellDefined(xData[0]) && Utilities.isWellDefined(xData[1]))
            {
               double x0 = xData[0];
               double dx = xData[1] - xData[0];
               isSeries = (dx != 0);
               for(int i=2; isSeries && i<xData.length; i++) isSeries = (xData[i] == xData[i-1] + dx);
               
               if(isSeries) params = new float[] { (float) dx, (float) x0 };
            }
         }

         if(lData != null && lData.length != 0 && lData.length != yData.length)
            throw new IllegalStateException("'LData' must be empty or the same length as 'YData'!");
         if(uData != null && uData.length != 0 && uData.length != yData.length)
            throw new IllegalStateException("'UData' must be empty or the same length as 'YData'!");
         if(xNeg != null && xNeg.length != 0 && xNeg.length != yData.length)
            throw new IllegalStateException("'XNegativeDelta' must be empty or the same length as 'YData'!");
         if(xPos != null && xPos.length != 0 && xPos.length != yData.length)
            throw new IllegalStateException("'XPositiveDelta' must be empty or the same length as 'YData'!");
         if(yNeg != null && yNeg.length != 0 && yNeg.length != yData.length)
            throw new IllegalStateException("'YNegativeDelta' must be empty or the same length as 'YData'!");
         if(yPos != null && yPos.length != 0 && yPos.length != yData.length)
            throw new IllegalStateException("'YPositiveDelta' must be empty or the same length as 'YData'!");
         
         // are there horizontal error bars? We check for the "silly case" in which all deltas are NaN or zero.
         boolean hasHNeg = false;
         if(xNeg != null && xNeg.length > 0)
         {
            for(int i=0; (!hasHNeg) && i<xNeg.length; i++) 
               hasHNeg = Utilities.isWellDefined(xNeg[i]) && (xNeg[i] != 0);
         }
         boolean hasHPos = false;
         if(xPos != null && xPos.length > 0)
         {
            for(int i=0; (!hasHPos) && i<xPos.length; i++) 
               hasHPos = Utilities.isWellDefined(xPos[i]) && (xPos[i] != 0);
         }

         // if there are horizontal error bars, are any or all of them one-sided?
         boolean hasOneSidedHBars = false;
         if(hasHNeg || hasHPos)
         {
            if(hasHNeg != hasHPos) hasOneSidedHBars = true;
            else for(int i=0; (!hasOneSidedHBars) && i<xNeg.length; i++)
            {
               hasOneSidedHBars = 
                     (Utilities.isWellDefined(xNeg[i]) && (xNeg[i] != 0)) != 
                     (Utilities.isWellDefined(xPos[i]) && (xPos[i] != 0));
            }
         }
         
         // repeat for vertical error bars. If defined, always use the newer properties 'YNegative/PositiveDelta'.
         boolean hasVNeg = false;
         if((yNeg != null && yNeg.length > 0) || (lData != null && lData.length > 0))
         {
            if(yNeg == null) yNeg = lData;
            for(int i=0; (!hasVNeg) && i<yNeg.length; i++) 
               hasVNeg = Utilities.isWellDefined(yNeg[i]) && (yNeg[i] != 0);
         }
         boolean hasVPos = false;
         if((yPos != null && yPos.length > 0) || (uData != null && uData.length > 0))
         {
            if(yPos == null) yPos = uData;
            for(int i=0; (!hasVPos) && i<yPos.length; i++) 
               hasVPos = Utilities.isWellDefined(yPos[i]) && (yPos[i] != 0);
         }

         boolean hasOneSidedVBars = false;
         if(hasVNeg || hasVPos)
         {
            if(hasVNeg != hasVPos) hasOneSidedVBars = true;
            else for(int i=0; (!hasOneSidedVBars) && i<yNeg.length; i++)
            {
               hasOneSidedVBars = 
                     (Utilities.isWellDefined(yNeg[i]) && (yNeg[i] != 0)) != 
                     (Utilities.isWellDefined(yPos[i]) && (yPos[i] != 0));
            }
         }
         
         // construct raw data array: tuple length depends on whether it's a 'series' or 'ptset', whether or not there
         // are H/V error bars, and whether there are any one-sided H/V error bars. If there are horizontal error bars,
         // we cannot encode data set as a 'series', regardless the content of 'XData'.
         isSeries = isSeries && !(hasHNeg || hasHPos);
         int nrows = yData.length;
         int ncols;
         if(isSeries)
         {
            if(hasVNeg || hasVPos) ncols = hasOneSidedVBars ? 3 : 2; 
            else ncols = 1;
         }
         else
         {
            if(hasHNeg || hasHPos) ncols = hasOneSidedHBars ? 6 : 5;
            else if(hasVNeg || hasVPos) ncols = hasOneSidedVBars ? 4 : 3;
            else ncols = 2;
         }
         float[] fData = new float[nrows*ncols];

         Fmt fmt;
         if(isSeries)
         {
            fmt = Fmt.SERIES;
            for(int i=0; i<nrows; i++)
            {
               fData[ncols*i] = (float) yData[i];
               if(ncols > 1)
               {
                  double negDelta = 0, posDelta = 0;
                  if(hasVNeg && Utilities.isWellDefined(yNeg[i])) negDelta = Math.abs(yNeg[i]);
                  if(hasVPos && Utilities.isWellDefined(yPos[i])) posDelta = Math.abs(yPos[i]);
                  fData[ncols*i + 1] = (float) Math.max(negDelta, posDelta);
                  if(ncols > 2) 
                     fData[ncols*i + 2] = (negDelta==posDelta || (negDelta>0 && posDelta>0)) ? 0 : 
                        ((negDelta==0) ? 1 : -1);
               }
            }
         }
         else
         {
            fmt = Fmt.PTSET;
            for(int i=0; i<nrows; i++)
            {
               fData[ncols*i] = (float) ((xData==null) ? (i+1) : xData[i]);
               fData[ncols*i + 1] = (float) yData[i];
               if(ncols > 2)
               {
                  double negDelta = 0, posDelta = 0;
                  if(hasVNeg && Utilities.isWellDefined(yNeg[i])) negDelta = Math.abs(yNeg[i]);
                  if(hasVPos && Utilities.isWellDefined(yPos[i])) posDelta = Math.abs(yPos[i]);
                  fData[ncols*i + 2] = (float) Math.max(negDelta, posDelta);
                  if(ncols > 3) 
                     fData[ncols*i + 3] = (negDelta==posDelta || (negDelta>0 && posDelta>0)) ? 0 : 
                        ((negDelta==0) ? 1 : -1);
               }
               if(ncols > 4)
               {
                  double negDelta = 0, posDelta = 0;
                  if(hasHNeg && Utilities.isWellDefined(xNeg[i])) negDelta = Math.abs(xNeg[i]);
                  if(hasHPos && Utilities.isWellDefined(xPos[i])) posDelta = Math.abs(xPos[i]);
                  fData[ncols*i + 4] = (float) Math.max(negDelta, posDelta);
                  if(ncols > 5) 
                     fData[ncols*i + 5] = (negDelta==posDelta || (negDelta>0 && posDelta>0)) ? 0 : 
                        ((negDelta==0) ? 1 : -1);
               }
            }
         }
         
         ds = DataSet.createDataSet(id, fmt, params, nrows, ncols, fData);
      }
      else if(type.equals("specgraph.scattergroup") && multiSetData != null)
      {
         // XData,YData -- and ZData in 3D case -- are collected for all scatter groups that have been coalesced into
         // this scatter group object
         
         // check whether we're dealing with a 2D or 3D scatter plot
         double[] zData = getDoubleVectorFromProperty(getProperty("ZData"));
         boolean is3D = (zData != null) && (zData.length > 0);
         
         // if N groups coalesced, we must have 2N or 3N vectors in list
         int delta = is3D ? 3 : 2;
         if((multiSetData.size() % delta) != 0) return(null);

         // count the total number of scatter points
         int nPts = 0;
         for(int i=0; i<multiSetData.size(); i+=delta) nPts += multiSetData.get(i).length;
         
         // construct the data set for 2D or 3D case
         if(!is3D)
         {
            float[] fData = new float[nPts*2];
            int n = 0;
            for(int i=0; i<multiSetData.size(); i+=2)
            {
               double[] x = multiSetData.get(i);
               double[] y = multiSetData.get(i+1);
               for(int j=0; j<x.length; j++)
               {
                  fData[n++] = (float) x[j];
                  fData[n++] = (float) y[j];
               }
            }
            ds = DataSet.createDataSet(id, Fmt.PTSET, null, nPts, 2, fData);
         }
         else
         {
            float[] fData = new float[nPts*3];
            int n = 0;
            for(int i=0; i<multiSetData.size(); i+=3)
            {
               double[] x = multiSetData.get(i);
               double[] y = multiSetData.get(i+1);
               double[] z = multiSetData.get(i+2);
               for(int j=0; j<x.length; j++)
               {
                  fData[n++] = (float) x[j];
                  fData[n++] = (float) y[j];
                  fData[n++] = (float) z[j];
               }
            }
            ds = DataSet.createDataSet(id, Fmt.XYZSET, null, nPts, 3, fData);
         }
      }
      else if(type.equals("specgraph.scattergroup") || type.equals("specgraph.stemseries") ||
            type.equals("bubblechart"))
      {
         // get the X-coordinate data
         double[] xData = getDoubleVectorFromProperty(getProperty("XData"));
         if(xData == null) throw new UnsupportedOperationException("XData must be a vector for scatter or stem plot");
         
         // get the Y-coordinate data
         double[] yData = getDoubleVectorFromProperty(getProperty("YData"));
         if(yData == null) throw new UnsupportedOperationException("YData must be a vector for scatter or stem plot");
         if(xData.length != yData.length)
            throw new IllegalStateException("Scatter/stem plot object's XData and YData are not the same length!");

         // get the Z-coordinate data from 'ZData' property, if present and NOT empty; else, assume 2D context.
         double[] zData = getDoubleVectorFromProperty(getProperty("ZData"));
         if(zData != null && zData.length == 0) zData = null;
         if(zData != null && zData.length != xData.length)
            throw new IllegalStateException("3D scatter/stem plot object's XData and ZData are not the same length!");

         // for scatter/bubble plot, check SizeData and CData properties. If either or both are N-vectors, then they
         // represent an additional dimension of the data set, either Z- or W-coordinate data. If both are N-vectors,
         // CData is preferred over SizeData when converting a "specgraph.scattergroup" object, unless N=3 and CData
         // can be interpreted as an RGB color spec. When converting a "bubblechart" object, SizeData is always
         // preferred over CData.
         double[] wData = null;
         if(type.equals("specgraph.scattergroup") || type.equals("bubblechart"))
         {
            double[] cData = getDoubleVectorFromProperty(getProperty("CData"));
            double[] szData = getDoubleVectorFromProperty(getProperty("SizeData"));
            boolean isSpecialCase = xData.length == 3 && szData != null && szData.length == 3 &&
                  (MatlabUtilities.processColorSpec(getProperty("CData")) != null);
            if((cData != null) && (cData.length == xData.length) && (!isSpecialCase) && !type.equals("bubblechart"))
            {
               if(zData == null) zData = cData;
               else wData = cData;
            }
            else
            {
               if(szData != null && szData.length == xData.length)
               {
                  // HG "UserData" property: Bubble size range in points [D1 D2] = bubblesize()) and in data units
                  // [L1 L2] = bubblelim(). These are axes-level properties that are not accessible from the output of
                  // handle2struct(). As a hack-around, both matfig2fyp() and savefigais() store this info in 'UserData'
                  // as a 4x1 vector [D1 D2 L1 L2]. If present, we use [L1 L2] to range-restrict SizeData conetents.
                  double[] limits = HGObject.getDoubleVectorFromProperty(getProperty("UserData"));
                  if(limits != null && limits.length == 4 && limits[0] < limits[1] && limits[2] < limits[3])
                  {
                     for(int i=0;i<szData.length; i++)
                     {
                        szData[i] = Utilities.rangeRestrict(limits[2], limits[3], szData[i]);
                     }
                  }
                  if(zData == null) zData = szData;
                  else wData = szData;
               }
            }
         }
         
         // construct raw data array for a FypML ptset, xyzset, or xyzwset. In a polar context, convert each
         // (x,y) to polar coordinates (theta, r).
         int nrows = xData.length;
         int ncols = (zData == null) ? 2 : ((wData == null) ? 3 : 4);
         float[] fData = new float[nrows*ncols];
         if(ncols == 2)
         {
            for(int i=0; i<nrows; i++)
            {
               fData[ncols*i] = (float) xData[i];
               fData[ncols*i + 1] = (float) yData[i];
            }
         }
         else if(ncols == 3)
         {
            for(int i=0; i<nrows; i++)
            {
               fData[ncols*i] = (float) xData[i];
               fData[ncols*i + 1] = (float) yData[i];
               fData[ncols*i + 2] = (float) zData[i];
            }
         }
         else
         {
            for(int i=0; i<nrows; i++)
            {
               fData[ncols*i] = (float) xData[i];
               fData[ncols*i + 1] = (float) yData[i];
               fData[ncols*i + 2] = (float) zData[i];
               fData[ncols*i + 3] = (float) wData[i];
            }
         }
         
         // if the "scattergroup" is in a polar graph, convert data to polar coordinates. However, if the HG parent is
         // a true "polaraxes", the data is already in polar coordinates -- except that we have to convert the theta
         // coordinate from radians to degrees. Note that a "bubblechart" in a polar conteext is always contained in a
         // true "polaraxes" object.
         if(isPolarPlot && !isTruePolarAxes) for(int i=0; i<nrows; i++)
         {
            double x = fData[ncols*i];
            double y = fData[ncols*i + 1];
            if(Utilities.isWellDefined(x) && Utilities.isWellDefined(y))
            {
               fData[ncols*i] = (float) Math.toDegrees(Math.atan2(y, x));
               fData[ncols*i + 1] = (float) Math.sqrt(x*x + y*y);
            }
         }
         else if(isPolarPlot && isTruePolarAxes) 
         {
            for(int i=0; i<nrows; i++) fData[ncols*i] = (float) Math.toDegrees(fData[ncols*i]);
         }
        
         ds = DataSet.createDataSet(id, ncols==2 ? Fmt.PTSET : (ncols==3 ? Fmt.XYZSET : Fmt.XYZWSET), 
               null, nrows, ncols, fData);
      }
      else if(type.equals("surface") && isBar3D)
      {
         // the "surface" renders a set of 3D bars and will be converted to a FypML scatter3d node configured in a a
         // bar plot-like display mode. The data points have already been extracted from the "surface" data and stored
         // in bar3DPoints. Easy conversion to an XYZSET.
         if(bar3DPoints == null) return(null);   
         
         float[] fData = new float[bar3DPoints.size()*3];
         for(int i=0; i<bar3DPoints.size(); i++)
         {
            Point3D p3 = bar3DPoints.get(i);
            fData[i*3] = (float) p3.getX();
            fData[i*3+1] = (float) p3.getY();
            fData[i*3+2] = (float) p3.getZ();
         }
         
         ds = DataSet.createDataSet(id,  Fmt.XYZSET, null, bar3DPoints.size(), 3, fData);
      }
      else if(type.equals("surface") || type.equals("image") || type.equals("graph3d.surfaceplot") ||
            type.equals("specgraph.contourgroup"))
      {
         // not supported in a polar context
         if(isPolarPlot) return(null);
         
         boolean useZData = type.equals("graph3d.surfaceplot") || type.equals("specgraph.contourgroup");
         
         // if we'll use CData, make sure CData mapping mode is "scaled" (the default); we don't support "direct" mode.
         Object prop = getProperty("CDataMapping");
         if(!useZData)
         {
            if(prop != null && prop.getClass().equals(String.class) && "direct".equals(prop))
               return(null);
         }
         
         // we get raw data from 'ZData' or 'CData'. In both cases, the property is a MxN matrix. If it is null or 
         // empty, then an empty data set is returned.
         int nrows = 0;
         int ncols = 0;
         float[] fData = null;
         prop = getProperty(useZData ? "ZData" : "CData");
         if(prop != null && prop.getClass().equals(double[][].class))
         {
            double[][] cData = (double[][]) prop;
            nrows = cData.length;
            ncols = (cData.length > 0) ? cData[0].length : 0;
            if(nrows > 0 && ncols > 0)
            {
               fData = new float[nrows*ncols];
               for(int r = 0; r<nrows; r++)
                  for(int c = 0; c<ncols; c++)
                     fData[ncols*r + c] = (float) cData[r][c];
            }
         }
         
         // XData: Get X-coordinate range endpoints X0, X1. These are parameters for the XYZIMG data set.
         float x0 = 0.0f;
         float x1 = 10.0f;
         prop = getProperty("XData");
         if(prop != null)
         {
            if(prop.getClass().equals(double[].class))
            {
               double[] xData = (double[]) prop;
               if(xData.length > 1)
               {
                  if(Utilities.isWellDefined(xData[0])) x0 = (float) xData[0];
                  if(Utilities.isWellDefined(xData[xData.length-1])) x1 = (float) xData[xData.length-1];
               }
            }
            else if(prop.getClass().equals(double[][].class))
            {
               double[][] xData = (double[][]) prop;
               if(xData[0].length > 1)
               {
                  if(Utilities.isWellDefined(xData[0][0])) x0 = (float) xData[0][0];
                  if(Utilities.isWellDefined(xData[0][xData[0].length-1])) x1 = (float) xData[0][xData[0].length-1];
               }
            }
         }
         
         // YData: Get Y-coordinate range endpoints Y0, Y1. These are parameters for the XYZIMG data set.
         float y0 = 0.0f;
         float y1 = 10.0f;
         prop = getProperty("YData");
         if(prop != null)
         {
            if(prop.getClass().equals(double[].class))
            {
               double[] yData = (double[]) prop;
               if(yData.length > 1)
               {
                  if(Utilities.isWellDefined(yData[0])) y0 = (float) yData[0];
                  if(Utilities.isWellDefined(yData[yData.length-1])) y1 = (float) yData[yData.length-1];
               }
            }
            else if(prop.getClass().equals(double[][].class))
            {
               double[][] yData = (double[][]) prop;
               if(yData.length > 1)
               {
                  if(yData[0].length > 0 && Utilities.isWellDefined(yData[0][0])) 
                     y0 = (float) yData[0][0];
                  if(yData[yData.length-1].length > 0 && Utilities.isWellDefined(yData[yData.length-1][0])) 
                     y1 = (float) yData[yData.length-1][0];
               }
            }
         }
         
         if(fData == null)
            ds = DataSet.createEmptySet(Fmt.XYZIMG, id, new float[] {x0, y0, x1, y1});
         else
            ds = DataSet.createDataSet(id, Fmt.XYZIMG, new float[]{x0, x1, y0, y1}, nrows, ncols, fData);
      }
      else if(type.equals("patch") && isPieChart)
      {
         double[] slices = multiSetData.get(0);
         ds = DataSet.createDataSet(id, Fmt.SERIES, new float[]{1, 1}, slices.length, 1, slices);
      }
      else if(type.equals("histogram"))
      {
         // NOTE: The "histogram" object may appear as a child of an "axes" or "polaraxes". The latter case is generated
         // by the polarhistogram() function, in which case the data is already in polar coordinates -- except that the
         // theta coordinates are in radians and must be converted to degreess for FypML.

         float[] fData = null;
         int nRasters = 0;
         int nTotalSamps = 0;
         Object prop = getProperty("Data");
         if(prop != null)
         {
            if(prop.getClass().equals(double[].class))
            {
               double[] samps = (double[]) prop;
               nRasters = 1;
               nTotalSamps = samps.length;
               fData = new float[nRasters + nTotalSamps];
               fData[0] = nTotalSamps;
               
               if(isTruePolarAxes)
               {
                  for(int i=0; i<nTotalSamps; i++) fData[i+1] = (float) Math.toDegrees(samps[i]);
               }
               else
               {
                  for(int i=0; i<nTotalSamps; i++) fData[i+1] = (float) samps[i];
               }
            }
            else if(prop.getClass().equals(double[][].class))
            {
               double[][] samps = (double[][]) prop;
               nRasters = samps.length;
               for(int i=0; i<nRasters; i++) nTotalSamps += (samps[i]).length;
               fData = new float[nRasters + nTotalSamps];
               for(int i=0; i<nRasters; i++) fData[i] = (samps[i]).length;
               int idx = nRasters;
               
               if(isTruePolarAxes)
               {
                  for(int i=0; i<nRasters; i++)
                  {
                     for(int j=0; j<(samps[i]).length; j++) fData[idx++] = (float) Math.toDegrees(samps[i][j]);
                  }
               }
               else
               {
                  for(int i=0; i<nRasters; i++)
                  {
                     for(int j=0; j<(samps[i]).length; j++) fData[idx++] = (float) samps[i][j];
                  }
               }
            }
         }
         if(fData == null) ds = DataSet.createEmptySet(Fmt.RASTER1D, id);
         else ds = DataSet.createDataSet(id, Fmt.RASTER1D, null, nTotalSamps, nRasters, fData);
      }
      
      if(ds == null)
         throw new IllegalStateException("Unexpected error: Unable to construct valid FypML-compatible data set!");
      return(ds);
   }

   /**
    * Does this Matlab Handle Graphics object represent a "graph2d.lineseries" plot containing the data points 
    * coalesced from two or more line-series plots, all of which contain a single (x,y) point each? Such a composite
    * plot object should be converted into a FypML trace node that is rendered like a Matlab "scatter plot".
    * @return True if this HG object is a "graph2d.lineseries" object containing scatter plot points collected from 
    * other matching single-point line-series plots under the same "axes" parent.
    */
   public boolean isCoalescedScatterPlot() 
   {
      // coalesced compass plot uses same internal list as a coalesced scatter plot; hence the extra test here:
      return(type.equals("graph2d.lineseries") && scatterPoints != null && !isCompassPlotVector);
   }
   
   /**
    * Does this Matlab Handle Graphics object represent a "line" plot containing two or more disjoint polylines, all of
    * which are rendered with the same appearance? This "set of polylines" should be converted into a FypML trace node.
    * @return True if this HG object is a "line" object containing polylines collected from other "line" objects that
    * have the same appearance and share the same "axes" parent.
    */
   public boolean isCoalescedLineSet() { return(type.equals("line") && scatterPoints != null); }
   
   /**
    * Does this Matlab Handle Graphics object represent a "specgraph.barseries" plot object that has been coalesced
    * with the other "specgraph.barseries" siblings defining the other bar groups in a Matlab bar plot? If so, it will
    * contain the Y-data, bar group legend labels, and bar group colors needed to convert it into a <i>FypML</i> bar
    * plot node.
    * @return True if this HG object is a coalesced "specgraph.barseries" object containing the coalesced {X: Y1 Y2 ...}
    * data and other information needed to convert it to a <i>FypML</i> bar plot node.
    */
   public boolean isCoalescedBarPlot() 
   { 
      return(type.equals("specgraph.barseries") && multiSetData != null); 
   }
   
   /**
    * Get the legend labels attached to the different data groups comprising a bar chart ('specgraph.barseries' object),
    * an area chart ('specgraph.areaseries'), or a pie chart ("patch"). The returned list will contain one entry for 
    * each data group, in order of appearance. If an entry is null, then that data group had no label and there was no 
    * indication it should appear in the graph legend.
    * @return The legend label list, as described. Returns null if this plot object does not represent a bar, area, or 
    * pie chart.
    */
   public List<String> getDataGroupLabels() 
   { 
      ArrayList<String> out = null;
      if(type.equals("specgraph.barseries") || type.equals("specgraph.areaseries") ||
            (type.equals("patch") && isPieChart))
      {
         if(dataGrpLabels != null) out = new ArrayList<>(dataGrpLabels);
         else 
         {
            out = new ArrayList<>();
            out.add(extractDataGroupLegendLabel(0));
         }
      }
      
      return(out);
   }
   
   /**
    * Get the fill colors for the different data groups comprising a bar chart ('specgraph.barseries' object), an area
    * chart ('specgraph.areaseries'), or pie chart ('patch' object).
    * @param matCM The Matlab figure's colormap. We need this to process the data group colors, in case any were defined
    * as color map indices instead of RGB colors.
    * @param cLim The portion of the Matlab figure's colormap assigned to the plot object's parent graph -- needed to 
    * process any scaled color map indices. Should be a two-element array.
    * @return The data group fill color list. Returns null if this plot object does not represent a bar, area, or pie
    * chart.
    */
   public List<Color> getDataGroupFillColors(double[][] matCM, double[] cLim)
   {
      List<Color> colors = null;
      if(type.equals("specgraph.barseries") || type.equals("specgraph.areaseries") ||
            (type.equals("patch") && isPieChart))
      {
         colors = new ArrayList<>();
         
         List<Object> colorObjects = dataGrpColors;
         if(colorObjects == null)
         {
            // the singleton case -- there's only one data group
            colorObjects = new ArrayList<>();
            colorObjects.add(extractDataGroupFillColor());
         }
         
         for(Object colorObj : colorObjects)
         {
            if(colorObj == null) colors.add(Color.BLACK);
            else if(colorObj.getClass().equals(Color.class)) colors.add((Color) colorObj);
            else if(colorObj.getClass().equals(Integer.class))
            {
               // process a direct or scaled color map index
               int idx = (Integer) colorObj;
               Color c = Color.BLACK;
               if(matCM != null && matCM.length > 0 && idx != 0)
               {
                  if(idx < 0)
                  {
                     // negative value signals a scaled index. Negate it and convert it to a direct color index.
                     idx = -idx;
                     if(cLim == null || cLim.length != 2) 
                        idx = 1;
                     else
                     {
                        double d = ((double) (idx - 1)) / (cLim[1] - cLim[0]);
                        d *= matCM.length;
                        idx = Utilities.rangeRestrict(1, matCM.length, ((int) d) + 1);
                     }
                  }
                  
                  idx = Utilities.rangeRestrict(0, matCM.length-1, idx-1);
                  double[] rgb = matCM[idx];
                  if(rgb != null && rgb.length == 3)
                     c = new Color((float) rgb[0], (float) rgb[1], (float) rgb[2]);
               }
               colors.add(c);
            }
            else colors.add(Color.BLACK);
         }

      }
      
      return(colors);
   }
   
   /**
    * Does this Matlab Handle Graphics object represent a "graph2d.lineseries" plot object with X- and Y-coordinate data 
    * consistent with a raster train plot? Such a plot is defined by X- and Y-coordinate vectors that have a nonzero
    * length that is a multiple of 3. Each triplet of points (x0, y0), (x1, y1), and (x2, y2) represent a vertical line
    * in a Cartesian plot and must satisfy: x0 == x1, 1 > H > 0, and x2 == NaN, where H = (y1-y0) is a constant across
    * ALL triplets in the raster train. These constraints ensure that every vertical line has the same height H, and no
    * two lines are connected to each other.
    * <p>Finally, the set of <b>distinct</b> y-coordinate values for the first data point across all triplets, {y0(A), 
    * y0(B), ...}, when sorted in ascending order, must satisfy: yOffset + [0 1 2 ... N-1]. This reflects the fact that
    * the first y-coordinate in a triplet is really just the integer ordinal identifying the raster train to which that
    * particular raster mark belongs, optionally offset by some floating-point value that is shared by ALL raster marks
    * across all of the N raster trains in the plot.</p>
    * 
    * @return True iff this HG object is a "graph2d.lineseries" with "XData" and "YData" properties meeting the 
    * requirements described for a raster train plot.
    */
   public boolean isRasterPlot()
   {
      // the HG object must be a line series plot object
      if(!type.equals("graph2d.lineseries")) return(false);
  
      // get the Y-coordinate data
      double[] yData = getDoubleVectorFromProperty(getProperty("YData"));
      if(yData == null) throw new UnsupportedOperationException("Data type for 'YData' not supported!");

      // get the X-coordinate data. If this property is implicit, then line series is NOT a raster plot
      double[] xData = getDoubleVectorFromProperty(getProperty("XData"));
      if(xData == null) throw new UnsupportedOperationException("Data type for 'XData' not supported!");
      if(xData.length != yData.length)
         throw new IllegalStateException("Line series XData and YData vectors are not the same length");
      
      // number of data points must be a multiple of 3 that is greater than 0
      boolean ok = (xData.length > 0) && ((xData.length % 3) == 0);
      if(!ok) return(false);
      
      // line height must lie in (0, 1)
      double h = yData[1] - yData[0];
      ok = (1.0 > h) && (h > 0);
      
      // for each triplet of points: x1==x2, x3=NaN, and y2-y1 == h. While we're at it, compile the set of distinct 
      // raster train ordinals (y1 for each triplet y1, y2, y3).
      List<Double> ordinals = new ArrayList<>();
      int idx = 0;
      while(ok && idx < xData.length)
      {
         ok = (xData[idx] == xData[idx+1]) && Double.isNaN(xData[idx+2]) && (yData[idx+1] - yData[idx] == h);
         if(ok)
         {
            Double ord = yData[idx];
            if(!ordinals.contains(ord)) ordinals.add(ord);
         }
         idx += 3;
      }
      
      // verify that list of distinct raster train ordinals, when sorted into ascending order [p(0) p(2) ... p(N-1)], 
      // satisfy p(n) - yOffset == n for n=1:N and yOffset = p(1);
      if(ok)
      {
         Collections.sort(ordinals);
         double yOffset = ordinals.get(0);
         for(int i=0; ok && i<ordinals.size(); i++)
            ok = ((double) i) == (ordinals.get(i) - yOffset);
      }
      
      return(ok);
   }

   /**
    * Is this a Handle Graphics "axes" object rendering a 3D plot in Matlab? The method should be called only after the
    * object has been constructed and populated with its descendants. 
    * @return True if this HG object is an "axes" configured as a 3D plot in Matlab.
    */
   public boolean is3DAxes() { return(type.equals("axes") && is3D); }
   
   /**
    * Is this a Handle Graphics "axes" object rendering a polar plot in Matlab? The method should be called only after 
    * the Matlab Handle Graphics figure has been translated into a tree of <code>HGObject</code>s, since the "axes"
    * child list must be processed via {@link #afterChildrenAdded()} to determine whether or not it is a polar plot and,
    * if so, remove the core objects that render the polar coordinate grid in Matlab.
    * @return True if this HG object is an "axes" configured as a polar plot in Matlab.
    */
   public boolean isPolarAxes() { return(type.equals("axes") && isPolarAxes); }
   
   /**
    * Get the maximum value for the radial axis of a Handle Graphics "axes" object rendered as a polar plot in Matlab.
    * The method should be called only after the Matlab Handle Graphics figure has been translated into a tree of 
    * <code>HGObject</code>s, since the "axes" child list must be processed via {@link #afterChildrenAdded()} to 
    * determine whether or not it is a polar plot and, if so, determine the maximum along the radial axis.
    * @return The maximum value for the radial axis of a polar plot. Returns 0 if this is not an HG "axes" object, or if
    * it is not defined as a polar plot.
    */
   public double getMaxRadius() { return(maxRad); }
   
   /**
    * Get the tick interval for the radial axis of a Handle Graphics "axes" object rendered as a polar plot in Matlab.
    * The method should be called only after the Matlab Handle Graphics figure has been translated into a tree of 
    * <code>HGObject</code>s, since the "axes" child list must be processed via {@link #afterChildrenAdded()} to 
    * determine whether or not it is a polar plot and, if so, determine the radial axis tick interval.
    * @return The radial axis tick interval for a polar plot. Returns 1 if this is not an HG "axes" object, or if
    * it is not defined as a polar plot.
    */
   public double getRadialTickInterval() { return(isPolarAxes() ? maxRad/nRadialTicks : 1); }
   
   /**
    * Get a representative HG "line" object that is used to render one of the grid lines in an HG "axes" that is 
    * configured as a polar plot.
    * <p>The method should be called only after the Matlab Handle Graphics figure has been translated into a tree of 
    * <b>HGObject</b>s, since the "axes" child list must be processed via {@link #afterChildrenAdded()} to determine 
    * whether or not it is a polar plot. That method removes all "line" objects from the child list that render the
    * polar grid lines so they're not confused with regular 'line" objects that render data in the plot; the first such
    * "line" object is saved internally so that its color, line style, and line width properties can be accessed. It
    * is assumed that all "line" objects rendering the polar grid lines are styled identically.</p>
    * @return A "line" object representing one of the circular or radial grid lines in a Matlab polar plot. Returns
    * null if this HG object is not an "axes".
    */
   public HGObject getPolarGridLineObject() { return(polarGridLine); }

   /**
    * Is this a Handle Graphics "text" object configured with multiple text lines and/or a visible text box? Such an
    * object must be converted to a FypML "textbox" element rather than a "label".
    * @return True if this HG object is a "text" object best represented by an FypML "textbox" element.
    */
   public boolean isTextBox()
   {
      boolean istextbox = false;
      if("text".equals(type))
      {
         String textStr = getTextStringProperty();
         Color edgeC = MatlabUtilities.processColorSpec(getProperty("EdgeColor"));
         StrokePattern lineStyle = MatlabUtilities.processLineStyle(this);
         Color bkgC = MatlabUtilities.processColorSpec(getProperty("BackgroundColor"));
         istextbox = (textStr.indexOf('\n') > -1) || (edgeC != null && lineStyle != null) || (bkgC != null);
      }
      return(istextbox);
   }

   /**
    * Is this a Handle Graphics "graph2d.lineseries" object rendering an angle histogram as generated by Matlab's 
    * <i>rose()</i> function?
    * @return True if this HG object represents an angle histogram or "rose plot" in a Matlab "axes" object that has 
    * been configured to look like a polar graph.
    */
   public boolean isRosePlot() { return(isRosePlot); }
   
   /**
    * Is this a Handle Graphics "graph2d.lineseries" representing one of the vectors emanating from the origin in a
    * compass plot generated by Matlab's <i>compass()</i> function? 
    * @return True if this HG object represents a vector in a Matlab compass plot within a polar axes. After plot
    * coalescing, all of the individual vectors (their endpoints only) will have been coalesced into a single line 
    * series object, which is ultimately translated into a FypML trace node to render the compass plot.
    */
   public boolean isCompassPlot() { return(isCompassPlotVector); }
   
   /**
    * Is this Handle Graphics 'axes' or 'patch' object representative of a pie chart generated by Matlab's <i>pie()</i>
    * function? 
    * <p>A pie chart is an axes with gridlines and axis lines all turned off, containing a series of 'patch','text'
    * pairs in its child list that render the "slices" of the pie chart with accompanying labels. During pre-processing,
    * this particular infrastructure is detected and: (i) the 'axes' object itself is tagged as the container for the
    * pie chart; (ii) all 'patch' child objects except the first are removed; (iii) the remaining 'patch' is tagged as 
    * a pie chart for conversion into the <i>FypML</i> <b>pie</b> element; and (iv) the information needed to prepare
    * the <i>FypML</i> pie chart (outer radius, radial offset of a displaced slice, the slice data, color, label, and
    * displaced flag) is consolidated in that single 'patch' object. The text labels remain in the child list so that
    * they'll be ultimately translated into labels in the <i>FypML</i> figure.</p>
    * <p>This method should be called only after the Matlab Handle Graphics figure has been translated into a tree of 
    * <b>HGObject</b>s, since the child list of the 'axes' must be examined to detect the presence of a pie chart and
    * pre-process it as described.</p>
    * 
    * @return True if this is an 'axes' containing a pie chart, in which case it is a polar graph; or true if it is the
    * remaining 'patch' object in which the pie chart information was consolidated during pre-processing. Else false.
    */
   public boolean isPieChart() { return(isPieChart); }
   
   /** 
    * Get the outer radius of the pie chart represented by this pre-processed 'patch' object. For full details, see
    * {@link #isPieChart()}.
    * @return The pie chart radius, or 0 if not applicable to this HG object.
    */
   public double getPieOuterRadius()
   {
      return(isPieChart && "patch".equals(type) ? this.pieRadius : 0);
   }
   
   /**
    * Get the radial offset of a displaced slice in the pie chart represented by this pre-processed 'patch' object. For
    * full details, see {@link #isPieChart()}.
    * @return The radial offset, expressed as a percentage of the pie chart's outer radius, in [1..100]. Returns 0 if
    * not applicable to this HG object.
    */
   public int getPieSliceRadialOffset()
   {
      return(isPieChart && "patch".equals(type) ? this.pieRadOfsPct : 0);
   }
   
   /**
    * Is the specified slice displaced in the pie chart represented by this pre-processed 'patch' object? For full 
    * details, see {@link #isPieChart()}.
    * @param pos The slice's index position (order in which slices are rendered in the Matlab pie chart).
    * @return True if: (i) this is a 'patch' object in which the pie chart information was consolidated during 
    * pre-processing; (ii) the slice index position is valid; and (iii) that slice is displaced radially from the pie
    * chart origin. False otherwise.
    */
   public boolean isPieSliceDisplaced(int pos)
   {
      boolean displaced = false;
      if(isPieChart && "patch".equals(type))
      {
         int n = multiSetData.get(0).length;
         if(pos >= 0 && pos < n) displaced = ((pieDisplacedBits & (1<<pos)) != 0);
      }
      return(displaced);
   }
   
   /**
    * Helper method for {@link #afterChildrenAdded()}. It checks whether select properties of this Handle Graphics 
    * object are consistent with a Matlab 'axes' that has been configured to display 3D data projected onto a 2D plane.
    * If the 'CameraUpVector' is [0 1 0] or [0 -1 0] and 'Projection' == 'orthographic', then it is assumed the 'axes' 
    * object displays a flat 2D view of data in the XY plane. Else, it is considered a 3D plot.
    */
   private void checkFor3DPlot()
   {
      is3D = false;
      if(!type.equals("axes")) return;
      
      // get the relevant properties. If they're not available or don't have the right data type, assume 2D.
      Object prop = getProperty("CameraUpVector");
      if(prop == null || !prop.getClass().equals(double[].class)) return;
      double[] vec = (double[]) prop;
      if(vec.length != 3) return;
      
      prop = getProperty("Projector");
      if(prop != null && !(prop.getClass().equals(String.class) || prop.getClass().equals(Character.class))) return;
      String prj = (prop == null) ? "orthographic" : getStringValuedProperty(this, "Projection");
      
      is3D = !("orthographic".equals(prj) && (vec[0] == 0) && (vec[2] == 0) && (Math.abs(vec[1]) - 1 < 0.0001));
   }
   
   /**
    * Helper method for {@link #afterChildrenAdded()}. It examines all "surface" children of a 3D 'axes' to see if
    * they were generated by Matlab's <i>bar3()</i> function. Any "surface" that does not render 3D bars is removed
    * from the child list, since it cannot be converted to FypML. In a second pass, any "surface" objects that have
    * the same appearance are coalesced into a single "surface". Ultimately, that object will be converted to a FypML
    * 3D scatter plot with a bar plot-like display mode. NOTE that the "surface" object should NOT be confused with
    * the "graph3d.surfaceplot" object, which is the object generated by Matlab's <i>surf()</i> function.
    * 
    * <p>For details, see {@link #postProcess3DBarPlot(HGObject)} and {@link #coalesce(HGObject, HGObject)}.</p>
    */
   private void postProcess3DPlot()
   {
      // sanity check
      if(!(type.equals("axes") && is3D)) return;
      
      // first pass: pre-process all "surface" children that render 3D bars and remove all other "surface" children
      int i = 0;
      while(i < children.size())
      {
         HGObject child = children.get(i);
         boolean remove = false;
         if(child.type.equals("surface")) remove = !postProcess3DBarPlot(child);
         if(remove) children.remove(i);
         else ++i;
      }
      
      // second pass: coalesce all "surface" children with the same appearance into one "surface
      List<HGObject> retained  = new ArrayList<>();
      for(HGObject kid : children) 
      {
         boolean retain = true;
         if(kid.type.equals("surface"))
         {
            for(HGObject plot : retained) if(plot.coalesce(kid, this))
            {
               retain = false;
               break;
            }
         }
         if(retain) retained.add(kid);
      }
      
      if(retained.size() != children.size())
      {
         children.clear();
         children = retained;
      }
   }
   
   /**
    * Post-process any HG "surface" object within a 3D axes to see if it renders a 3D bar plot as generated by Matlab's
    * <i>bar3()</i> function.
    * 
    * <p>The method enforces the following restrictions on the properties of the "surface" object:
    * <ul>
    * <li><i>FaceColor</i> can only be an RGB color specification, "none", or "interp". "Flat" (the default value) is 
    * NOT supported.</li>
    * <li><i>FaceAlpha</i> must be a scalar double in [0..1] (default is 1).</li>
    * <li><i>EdgeColor</i> must be "none" or an RGB color specification (default is black).</li>
    * <li><i>EdgeAlpha</i> must be a scalar double in [0..1] (default is 1).</li>
    * <li><i>XData, YData, ZData</i> must all have the size 6N x 4, where N is the number of 3D bars rendered by the
    * surface object.</li>
    * <li>Every block of 6 rows in the X/Y/ZData members define the vertices of the 3D bar, but in a very obtuse way.
    * Here is how one block looks:
    * <pre>
    *                XData                    YData                    ZData
    *         NaN   x0   x1   NaN      NaN   y0   y0   NaN      NaN   z0   z0   NaN
    *         x0    x0   x1   x1       y0    y0   y0   y0       z0    z1   z1   z0
    *         x0    x0   x1   x1       y1    y1   y1   y1       z0    z1   z1   z0
    *         NaN   x0   x1   NaN      NaN   y1   y1   NaN      NaN   z0   z0   NaN
    *         NaN   x0   x1   NaN      NaN   y0   y0   NaN      NaN   z0   z0   NaN
    *         NaN   NaN  NaN  NaN      NaN   NaN  NaN  NaN      NaN   NaN  NaN  NaN
    * </pre>
    * The method verifies that the 3 data matrices are consistent with this structure. Furthermore, it requires that
    * the value z0 be the same for all 6-row blocks in ZData; z=z0 is the base plane for the 3D bars in the "detached"
    * style, which is the only <i>bar3()</i> style that may be imported. Finally, the actual "data point" represented
    * by this single 3D bar is x = (x0+x1)/2, y = (y0+y1)/2, and z = z1.
    * </li>
    * <li>The 3D bar width is abs(x1-x0)/2 or abs(y1-y0)/2, which must evaluate to the same value B, and that value
    * must be the same for 6-row blocks in X/YData.</li>
    * </ul>
    * If any of these restrictions are violated, the "surface" object cannot be converted to a FypML equivalent, in 
    * which case it should be removed from the child list of the parent "axes".
    * </p>
    * 
    * <p>If the "surface" is consistent with a 3D bar plot that can be translated into a FypML 3D scatter plot (in a 
    * bar plot-like display mode), then the method sets an internal flag to indicate this. It saves the base plane 
    * Z-coordinate z0 and the bar size B, as well as the computed set of 3D data points (one per bar); all of this
    * information is needed when converting the "surface" to the FypML equivalent.</p>
    * 
    * @param surf The "surface" object to be processed.
    * @return True if the "surface" renders a 3D bar plot that can be converted to a FypML equivalent; else false.
    */
   private static boolean postProcess3DBarPlot(HGObject surf)
   {
      // FaceColor: Must be "none", "interp", or RGB color spec. Default value ("flat") not supported.
      Object prop = surf.getProperty("FaceColor");
      boolean ok = "none".equals(prop) || "interp".equals(prop) || (null != MatlabUtilities.processColorSpec(prop));
      
      // FaceAlpha: Must be implicit (null), or a scalar double in [0..1]
      if(ok)
      {
         prop = surf.getProperty("FaceAlpha");
         if(prop != null)
         {
            double[] alpha = getDoubleVectorFromProperty(prop);
            ok = (alpha != null) && (alpha.length==1) && (alpha[0] >= 0) && (alpha[0] <= 1);
         }
      }
      
      // EdgeColor: Must be "none" or RGB color spec. Default value is black.
      if(ok)
      {
         prop = surf.getProperty("EdgeColor");
         ok = (prop==null) || "none".equals(prop) || (null != MatlabUtilities.processColorSpec(prop));
      }
      
      // EdgeAlpha: Must be implicit (null), or a scalar double in [0..1]
      if(ok)
      {
         prop = surf.getProperty("EdgeAlpha");
         if(prop != null)
         {
            double[] alpha = getDoubleVectorFromProperty(prop);
            ok = (alpha != null) && (alpha.length==1) && (alpha[0] >= 0) && (alpha[0] <= 1);
         }
      }
      

      // XData,YData,ZData: All must be same size, 6Nx4, where N is the number of 3D bars rendered by surface
      double[][] xData = null;
      double[][] yData = null;
      double[][] zData = null;
      int nRows = 0, nCols = 0;
      if(ok)
      {
         prop = surf.getProperty("XData");
         ok = (prop != null && prop.getClass().equals(double[][].class));
         if(ok)
         {
            xData = (double[][]) prop;
            nRows = xData.length;
            nCols = (xData.length > 0) ? xData[0].length : 0;
            ok = (nRows > 0) && (nCols > 0) && (nRows % 6 == 0);
         }
      }
      
      if(ok)
      {
         prop = surf.getProperty("YData");
         ok = (prop != null && prop.getClass().equals(double[][].class));
         if(ok)
         {
            yData = (double[][]) prop;
            ok = (nRows == yData.length) && (nCols == yData[0].length);
         }
      }

      if(ok)
      {
         prop = surf.getProperty("ZData");
         ok = (prop != null && prop.getClass().equals(double[][].class));
         if(ok)
         {
            zData = (double[][]) prop;
            ok = (nRows == zData.length) && (nCols == zData[0].length);
         }
      }

      // next, validate the structure of the vertex coordinates listed in every 6 rows of X/Y/ZData -- as described in
      // the function header.
      double zBase = Double.NaN, barSz = Double.NaN;
      for(int i=0; ok && i<nRows; i+=6)
      {
         double x0, y0, x1, y1, z0=0, z1, xBarSz=0, yBarSz=0;
         
         x0 = xData[i+1][0];
         x1 = xData[i+1][3];
         ok = (x0 == xData[i+2][0]) && (x1 == xData[i+2][3]);
         for(int j=0; ok && j<5; j++) ok = (x0 == xData[i+j][1]) && (x1 == xData[i+j][2]);
         if(ok) xBarSz = Math.abs(x1-x0)/2.0;
         
         if(ok)
         {
            y0 = yData[i][1];
            y1 = yData[i+3][1];
            ok = (y0 == yData[i][2]) && (y0 == yData[i+4][1]) && (y0 == yData[i+4][2]) && (y1 == yData[i+3][2]);
            for(int j=0; ok && j<4; j++) ok = (y0 == yData[i+1][j]) && (y1 == yData[i+2][j]);
            if(ok) yBarSz = Math.abs(y1-y0)/2.0;
            if(ok) ok = (xBarSz>0) && (Math.abs(xBarSz-yBarSz)/Math.abs(xBarSz) < 0.001);
         }
         
         if(ok)
         {
            z0 = zData[i][1];
            z1 = zData[i+1][1];
            ok = (z0 == zData[i][2]) && (z0 == zData[i+1][0]) && (z0 == zData[i+1][3]);
            if(ok) ok = (z0 == zData[i+2][0]) && (z0 == zData[i+2][3]);
            if(ok) ok = (z0 == zData[i+3][1]) && (z0 == zData[i+3][2]);
            if(ok) ok = (z0 == zData[i+4][1]) && (z0 == zData[i+4][2]);
            if(ok) ok = (z1 == zData[i+1][2]) && (z1 == zData[i+2][1]) && (z1 == zData[i+2][2]);
         }
         
         // ensure all 3D bars have the same Z base plane and the same bar size.
         if(ok)
         {
            if(i==0) { zBase = z0; barSz = xBarSz; }
            else ok = (zBase==z0) && (Math.abs(barSz - xBarSz)/barSz < 0.001);
            
         }
      }

      // if the surface object passed all tests, mark it as a 3D bar plot, save the base Z and bar size, and prepare
      // the list of 3D points (one per bar).
      if(ok)
      {
         surf.isBar3D = true;
         surf.bar3DBaseZ = zBase;
         surf.bar3DSize = barSz;
         
         surf.bar3DPoints = new ArrayList<>();
         for(int i = 0; i<nRows; i+=6)
         {
            double x0 = xData[i+1][0], x1 = xData[i+1][3], y0 = yData[i][1], y1 = yData[i+3][1], z1 = zData[i+1][1];
            surf.bar3DPoints.add(new Point3D((x0+x1)/2.0, (y0+y1)/2.0, z1));
         }
      }
      
      return(ok);
   }
   
   /** 
    * Is this Handle Graphics object a primitive "surface" that is configured to render a set of 3D bars in a 3D graph
    * context, as generated by Matlab's <i>bar3()</i> function. The FIG-to-FypML import engine will convert this
    * "surface" to a 3D scatter plot configured in one of the bar plot-like display modes.
    * 
    * @return True if HG object is a "surface" object that renders a set of 3D bars; else false.
    */
   public boolean is3DBarPlot() { return(isBar3D); }
   /**
    * If this Handle Graphics object is a primitive "surface" rendering a set of 3D bars in a 3D graph context, as
    * generated by Matlab's <i>bar3()</i> function, then this method returns the calculated bar size (in X and Y 
    * directions) in native graph units.
    * @return Bar size for 3D bar plot, if applicable. Returns 0 otherwise.
    */
   public double get3DBarPlotBarSize() { return(bar3DSize); }
   /**
    * If this Handle Graphics object is a primitive "surface" rendering a set of 3D bars in a 3D graph context, as
    * generated by Matlab's <i>bar3()</i> function, then this method returns the Z-coordinate of the common Z baseplane
    * for all 3D bars in the plot.
    * @return The baseplane Z-coordinate, if applicable. Returns 0 otherwise.
    */
   public double get3DBarPlotBaseplaneZ() { return(bar3DBaseZ); }
   
   /**
    * Helper method for {@link #afterChildrenAdded()}. It checks whether or not this object is a 2D 'axes' containing
    * at least one 'specgraph.stemseries' and a 'specgraph.baseline' object -- as generated by Matlab's stem() function.
    * All 'stemseries' in the graph share the same 'baseline', and it may be that the stem plot's "BaseValue" property
    * is not set (even if it is not the default of 0). When this is the case, we need to check the 'baseline' object's
    * "BaseValue" property and apply that to all 'stemseries' children under the 'axes' object.
    */
   private void get2DStemPlotBaseLine()
   {
      // ignore if this is not a 2D 'axes'
      if((!type.equals("axes")) || is3D) return;
      
      boolean isStemPlot = false;
      HGObject baselineObj = null;
      for(HGObject child : children)
      {
         if(!isStemPlot) isStemPlot = "specgraph.stemseries".equals(child.type);
         if(baselineObj == null && "specgraph.baseline".equals(child.type)) baselineObj = child;
      }
      if(!(isStemPlot && baselineObj != null)) return;
      
      // get the scalar value for the stem plot baseline from the 'baseline' object. If implicit, default is 0.
      Object prop = baselineObj.getProperty("BaseValue");
      double baseVal = 0;
      if(prop != null && prop.getClass().equals(Double.class)) baseVal = (Double) prop;
      
      // now, for each 'stemseries' child, explicitly set its "BaseValue" property if it is not already set.
      for(HGObject child : children) if("specgraph.stemseries".equals(child.type))
      {
         prop = child.getProperty("BaseValue");
         if(prop == null || !prop.getClass().equals(Double.class))
            child.putProperty("BaseValue", baseVal);
      }
   }
   
   /**
    * Helper method for {@link #afterChildrenAdded()}. It checks whether or not the properties and child content of this
    * Handle Graphics object are consistent with that of a Matlab 'axes' generated by one of the polar plot functions
    * <i>polar(), rose(), compass()</i>. If so, it marks the 'axes' object as a polar plot and saves some information 
    * needed to convert the 'axes' to a FypML graph with a polar coordinate system.
    */
   private void checkForPolarPlot()
   {
      if(!type.equals("axes")) return;
      
      // 'Visible' must be 'off'
      if(!"off".equals(getProperty("Visible"))) return;
      
      // 'XLim'=[x0 x1], 'YLim'=[y0 y1] must satisfy abs(x0)==x1, abs(y0)==y1, and 1.15*[x0 x1] = [y0 y1].
      Object prop = getProperty("XLim");
      if(prop == null || !prop.getClass().equals(double[].class)) return;
      double[] xLim = (double[]) prop;
      prop = getProperty("YLim");
      if(prop == null || !prop.getClass().equals(double[].class)) return;
      double[] yLim = (double[]) prop;
      
      if(xLim.length < 2 || yLim.length < 2) return;
      if(Math.abs(xLim[0]) != xLim[1] || Math.abs(yLim[0]) != yLim[1]) return;
      if((1.15 * xLim[0] != yLim[0]) || (1.15 * xLim[1] != yLim[1])) return;
      
      // verify presence of children that render the polar coordinate grid over a Cartesian coordinate plane. There
      // must be at least two radial tick marks, so two ('line', 'text' pairs). So the absolute minimum # child objects
      // used to render the grid is 1 + 2*2 + 6 + 12 = 23.
      if(children.size() < 23) return;
      nRadialTicks = 0;
      int iKid = 0;
      if(!"patch".equals(children.get(iKid).type)) return;
      ++iKid;
      boolean isRadialTickPair;
      do
      {
         HGObject kid1 = children.get(iKid++);
         HGObject kid2 = children.get(iKid++);
         isRadialTickPair = "line".equals(kid1.type) && "text".equals(kid2.type);
         if(isRadialTickPair) ++nRadialTicks;
         else iKid -= 2;
      } while(isRadialTickPair);
      
      if(nRadialTicks < 2) return;
      
      // now we should be on the first of 6 radial lines, followed by 12 associated 'text' labels. So there must be at
      // least 18 kids left to process.
      if(children.size() - iKid < 18) return;
      for(int i=0; i<6; i++) 
      {
         HGObject kid = children.get(iKid++);
         if(!"line".equals(kid.type)) return;
      }
      for(int i=0; i<12; i++) 
      {
         HGObject kid = children.get(iKid++);
         if(!"text".equals(kid.type)) return;
      }
      
      // all tests were passed. We now assume the 'axes' object represents a polar plot.
      isPolarAxes = true;
      maxRad = xLim[1];
   }

   /**
    * Helper method for {@link #afterChildrenAdded()}. If this Handle Graphics object is an 'axes' configured as a polar
    * plot, then the method checks each 'graph2d.lineseries' object in its child list and tags those generated by 
    * Matlab's <i>rose()</i> and <i>compass()</i> functions.
    * <ul>
    * <li>The line series for a rose plot will contain 4N points, where N is the number of bins in the angle histogram
    * generated by <i>rose()</i>. Each set of 4 points {(x1, y1), (x2, y2), (x3, y3), (x4, y4)} renders one bin of the
    * histogram as a triangle emanating from the origin. Thus, x1=y1=x4=y4=0. It should also be true that sqrt(x2*x2 +
    * y2*y2) ~= sqrt(x3*x3 + y3*y3), atan2(y2, x2) ~= 2*pi/N, and atan2(y3, x3) ~= 2*pi/N. These are not the only 
    * requirements, but these are the only ones checked.</li>
    * <li>Each vector in a compass plot is represented by a separate 'graph2d.lineseries' object containing exactly 5
    * points following this pattern: (0,0) --> (a,b) --> (c, d) --> (a,b) --> (e, f).</li>
    * </ul>
    */
   private void tagRoseAndCompassPlotObjects()
   {
      if(!isPolarAxes) return;
      
      for(HGObject child : children) if(child.type.equals("graph2d.lineseries"))
      {
         double[] x = getDoubleVectorFromProperty(child.getProperty("XData"));
         double[] y = getDoubleVectorFromProperty(child.getProperty("YData"));
         if(x == null || y == null || x.length != y.length) continue;
         
         if(x.length == 5)
            child.isCompassPlotVector = (x[0] == 0) && (y[0] == 0) && (x[1] == x[3]) && (y[1] == y[3]);
         else if(x.length > 0 && (x.length % 4) == 0)
         {
            int nBins = x.length / 4;
            double binSz = Math.PI* 2.0 / nBins;
            boolean isRose = true;
            for(int i=0; isRose && i<nBins; i++)
            {
               isRose = (x[i*4] == 0) && (y[i*4] == 0) && (x[i*4 + 3] == 0) && (y[i*4 + 3] == 0);
               if(isRose)
               {
                  double r1 = Math.sqrt(x[i*4 + 1] * x[i*4 + 1] + y[i*4 + 1] * y[i*4 + 1]);
                  double r2 = Math.sqrt(x[i*4 + 2] * x[i*4 + 2] + y[i*4 + 2] * y[i*4 + 2]);
                  isRose = Utilities.isWellDefined(r1) && Utilities.isWellDefined(r2) && (Math.abs(r2-r1) / r1) < 0.01;
               }
               if(isRose)
               {
                  double theta1 = Math.atan2(y[i*4 + 1], x[i*4 + 1]);
                  if(theta1 < 0) theta1 += Math.PI * 2.0;
                  double theta2 = Math.atan2(y[i*4 + 2], x[i*4 + 2]);
                  if(theta2 < 0) theta2 += Math.PI * 2.0;
                  double diff = Math.abs(theta2 - theta1);
                  isRose = Utilities.isWellDefined(diff) && (Math.abs(binSz-diff) / binSz) < 0.01;
               }
            }
            child.isRosePlot = isRose;
         }
      }
   }

   /**
    * Helper method for {@link #afterChildrenAdded()}. It checks whether or not the properties and child content of this
    * Handle Graphics 'axes' is consistent with that generated by the <i>pie()</i> function. If so, the properties and
    * children of this 'axes' are pre-processed to prepare for conversion to a <i>FypML</i> polar graph containing a
    * pie chart element. No action is taken if this is not a HG 'axes' object.
    * 
    * <p>Let A be an HG 'axes' object". To be consistent with a pie chart, its properties and content must satisfy the
    * following conditions:
    * <ul>
    * <li>A.Visible = 'off'. A.XLim=[x0,x1] and YLim=[y0,y1] must satisfy abs(x0) == x1 and abs(y0) == y1.</li>
    * <li>For a pie chart with N slices, the first 2N children of A must be in 'patch'-'text' pairs. For each patch P,
    * P.Faces must be a vector 1xM and P.Vertices an array Mx2; thus, each patch contains a single face, a "slice" of 
    * the pie chart. The vertices define the outline of the slice; M increases with the slice's angular extent.</li>
    * <li>Let Vi = Pi.Vertices, the vertices arrray for the i-th patch. It must be true that the first and last vertex
    * in Vi are the same; this is the origin of the slice represented by the patch. Typically, the origin will be (0,0),
    * which is the center of the pie chart. If it is not, then the slice is "displaced".</li>
    * <li>All remaining points (index pos 2 to L-1, where L is the array length) define the arc of the pie slice. Thus,
    * expect the distance from each of these points to the slice origin to be the same value -- and this value must be
    * the same across all the patches (i.e., slices). This distance is the outer radius R of the pie chart!</li>
    * <li>The patches define the pie slices in order, going counterclockwise around the pie chart (starting at 90deg).
    * Verify this by calculating the angle S of the ray connecting the first vertex of a patch to its second vertex, and
    * the angle E of the ray connecting the last vertext to its second-to-last vertex. It must be true that ray S(i) for
    * the i-th slice matches ray E(i-1) of the slice before it; and E for the last slice must match ray S(0).</li>
    * </ul>
    * </p>
    * 
    * <p>If the above conditions are met, then the 'axes' is marked as a polar plot containing a pie chart. The N 
    * 'patch' objects defining the pie slices are coalesced into a single patch specially marked as representing a pie
    * chart. The stroke-related properties of that patch determine the stroke properties of the <i>FypML</i> <b>pie</b>
    * node to which it is converted. The data and other properties of the node are determined as follows:
    * <ul>
    * <li>The angular extents E(i) - S(i), for i=1:N, form the data set for the pie chart.</li>
    * <li>The 'text' object paired with each 'patch' contains a text string associated with that patch; this serves as
    * the legend label for the corresponding slice in the pie chart. The slice's fill color is determined by analyzing
    * the 'FaceVertexCData' property of the patch; if a color index is supplied, we'll need to resolved it against the
    * figure's colormap later on. Finally, as mentioned above, if the first vertex in a patch's 'Vertices' array is not
    * (0,0), then the corresponding slice is displaced.</li>
    * <li>The pie chart's outer radius R was already calculated (it will be 1 typically). The inner radius is 0; Matlab
    * does not support a "donut" chart.</li>
    * <li>If a slice is displaced, the radial offset RO = sqrt(x0*x0 + y0*y0), where (x0,y0) is the first vertex of the
    * patch. This is converted to an integer percentage: RO' = round(RO*100 / R). It is the same for all slices.</li>
    * </ul>
    * </p>
    */
   private void detectAndProcessPieChart()
   {
      if(isPolarAxes || !type.equals("axes")) return;

      // 'Visible' must be 'off'
      if(!"off".equals(getProperty("Visible"))) return;

      // 'XLim'=[x0 x1], 'YLim'=[y0 y1] must satisfy abs(x0)==x1, abs(y0)==y1.
      Object prop = getProperty("XLim");
      if(prop == null || !prop.getClass().equals(double[].class)) return;
      double[] xLim = (double[]) prop;
      prop = getProperty("YLim");
      if(prop == null || !prop.getClass().equals(double[].class)) return;
      double[] yLim = (double[]) prop;
      if(xLim.length < 2 || yLim.length < 2) return;
      if(Math.abs(xLim[0]) != xLim[1] || Math.abs(yLim[0]) != yLim[1]) return;

      // verify that the first 2N children are patch/text pairs, with N > 1 
      int nSlices = 0;
      for(int i=0; i<children.size(); i+=2)
      {
         if(!"patch".equals(children.get(i).type)) break;
         if(i+1 >= children.size() || !"text".equals(children.get(i+1).type)) break;
         ++nSlices;
      }
      if(nSlices < 2) return;
      
      // for each patch P representing a pie slice, verify P.Faces is 1xM, V=P.Vertices is Mx2, with M>= 4; that V(1)
      // and V(M) are the same (the slice origin); and that R=dist(V(1),V(k)) is a constant for k=2..M-1. R is the 
      // radius of the pie slice, and it must be the same across all patches.
      //
      // Also: Let S and E be the angles made by the rays that connect the second and second-to-last vertices of the 
      // slice to the slice origin. Verify that S(i) == E(i-1) for i=2..N, and E(N) = S(1), for N slices.
      //
      // As we make these checks, we extract the information we'll need to generate the FypML pie chart: outer radius,
      // radial offset of a displaced slice (same for all displaced slices), and per-slice information -- angular
      // extent in degrees, fill color, legend label, and displace flag.
      List<String> labels = new ArrayList<>();
      List<Object> fillColors = new ArrayList<>();   // could be Integer or Color
      int flagBits = 0;
      double[] sliceArcs = new double[nSlices];
      boolean gotRadOfs = false;
      int radOfs = 10;  // default
      double radius = 0;
      
      double s0 = 0;
      double eLastDeg = 0;
      for(int i=0; i<nSlices; i++)
      {
         HGObject slice = children.get(i*2);
         double[] faces = getDoubleVectorFromProperty(slice.getProperty("Faces"));
         if(faces == null || faces.length < 4) return;

         int m = faces.length;
         prop = slice.getProperty("Vertices");
         if(prop == null || !prop.getClass().equals(double[][].class)) return;

         double[][] verts = (double[][]) prop;
         if(verts.length != m || verts[0].length != 2) return;

         if(Math.abs(verts[0][0] - verts[m-1][0]) > 0.001) return;
         if(Math.abs(verts[0][1] - verts[m-1][1]) > 0.001) return;

         for(int j=1; j<m-1; j++)
         {
            double dx = verts[j][0] - verts[0][0];
            double dy = verts[j][1] - verts[0][1];
            double r = Math.sqrt(dx*dx + dy*dy);
            if(r == 0) return;
            if(radius == 0) 
               radius = r;
            else if(Math.abs(radius - r) > 0.001) 
               return;
         }
         
         double sDeg = Math.atan2(verts[1][1] - verts[0][1], verts[1][0] - verts[0][0]) * 180 / Math.PI;
         sDeg = Utilities.restrictAngle(sDeg);
         if(i == 0) s0 = sDeg;
         else if(Math.abs(sDeg-eLastDeg) > 0.001) return;
         
         eLastDeg = Math.atan2(verts[m-2][1] - verts[0][1], verts[m-2][0] - verts[0][0]) * 180 / Math.PI;
         eLastDeg = Utilities.restrictAngle(eLastDeg);

         // the "slice" passed all the checks. Save angular extent, displaced flag, associated label, and fill color.
         sliceArcs[i] = Utilities.restrictAngle(eLastDeg - sDeg);
         boolean displaced = (Math.abs(verts[0][0]) > 0.001) || (Math.abs(verts[0][1]) > 0.001);
         if(displaced)
         {
            flagBits |= (1<<i);
            if(!gotRadOfs)
            {
               gotRadOfs = true;
               double r = Math.sqrt(verts[0][0]*verts[0][0] + verts[0][1]*verts[0][1]);
               r *= (100 / radius);
               radOfs = Utilities.rangeRestrict((int)Math.round(r), PieChartNode.MIN_RADOFS, PieChartNode.MAX_RADOFS);
            }
         }
         
         String str = children.get(i*2+1).getTextStringProperty();
         labels.add(str == null ? "" : str);
         
         fillColors.add(slice.extractDataGroupFillColor());
      }
      if(Math.abs(s0-eLastDeg) > 0.001) return;

      // if we get here, a pie plot has been detected. Mark the 'axes' as a polar plot containing a pie chart, and 
      // remove all the 'patch' children representing the individual pie slices -- except the first. Then store the pie 
      // chart information in that first "coalesced" patch. It will ultimately be converted into a FypML pie chart node.
      isPolarAxes = true;
      maxRad = Math.max(xLim[1], yLim[1]);
      isPieChart = true;
      
      for(int i = nSlices-1; i>0; i--) children.remove(i*2);

      HGObject slice0 = children.get(0);
      slice0.isPieChart = true;
      slice0.pieRadius = radius;
      slice0.pieRadOfsPct = radOfs;
      slice0.pieDisplacedBits = flagBits;
      slice0.multiSetData = new ArrayList<>();
      slice0.multiSetData.add(sliceArcs);
      slice0.dataGrpColors = fillColors;
      slice0.dataGrpLabels = labels;
   }
   

   /**
    * Helper method for {@link #afterChildrenAdded()}; handles post-processing of a 'specgraph.barseries' object. Each
    * such object represents one bar group in a bar plot. This method analyzes the object's single 'patch' child and
    * calculates the following properties:
    * <ul>
    * <li>The bar group's orientation. Stored as the Matlab property "Horizontal" = "on" or "off". When saved to a FIG
    * file, this property will have the SAME value across ALL 'barseries' in the same parent 'axes' -- even if some
    * are oriented horizontally and some vertically. Only by examing the patch object can we unambiguously determine
    * the orientation of each individual bar group.</li>
    * <li>The bar group's actual bar width. All bars in the group should have the same width. The calculated value is
    * stored as a {@link Double Double} under the property name {@link #ACTUALBW_PROP}. Actual bar width is
    * related to the relative bar width as specified by the Matlab "BarWidth" property. However, again, it is possible
    * to create two bar plots width different "BarWidth" values, yet when saved to a FIG file, "BarWidth" will have
    * the same value across all 'barseries' objects.</li>
    * <li>The first vertex P1 of the first bar in the bar group. This is saved as a double[] under the property name 
    * {@link #P1BAR_PROP}. When a barseries object is coalesced with another, comparing the P1 for each will 
    * unambiguously determine the layout (Matlab property "BarLayout") and baseline value ("BaseValue") of the coalesced
    * bar plot.</li> 
    * </ul>
    * 
    * <p><b>Why is this done?</b> It is possible to create a single Matlab 'axes' containing more than one "bar plot",
    * but this is NOT apparent in the HG tree structure. All 'barseries' siblings share a single 'specgraph.baseline'
    * object, along with certain properties like "Horizontal", "BarWidth", "BaseValue", "BarLayout", and others. For
    * example, even if you create an axes with two bar plots, one "grouped" and one "stacked", the "BarLayout" property
    * will have the same value for all 'barseries' objects (either "grouped" or "stacked", depending on which value was
    * last applied) once you save the figure to a FIG file. To properly coalesce individual 'barseries' objects into 
    * related collections that are converted to <i>FypML</i> bar plot nodes, we need to unambiguously determine the
    * values of these properties. We do this by looking at the 'patch' child object that renders the bar group
    * represented by a single 'barseries' object. The bar group's orientation and actual bar width are calculated here.
    * "BarLayout" and "BaseValue" can be unambiguously determined when the first two 'barseries' in a collection are
    * coalesced, by comparing the first bar vertex in each. See {@link #coalesce(HGObject, HGObject)}.</p>
    * 
    * <p><b>IMPORTANT</b>: The method examines the "Faces" and "Vertices" properties of the barseries object's single
    * "patch" child. For a barseries containing N bars, the "Faces" propety will be an Nx4 matrix, each element of which
    * is an index into the "Vertices" matrix. The "Vertices" matrix is MxP, where M is the number of vertices and P is 2
    * or 3, depending on whether the coordinates are 2D or 3D. For a barseries, the bars typically do not share any 
    * vertices, so we expect M=4N. This is the case in barseries generated by Matlab R2017b. However, in R2016b and
    * earlier, the "Vertices" matrix included an extra point for each bar -- points lying along the baseline for the 
    * bar plot -- such that M=5N+1. (Note: This is an undocumented detail about a Matlab barseries object). The method
    * will fail to process the barseries and calculate the aforementioned properties if M is neither 4N nor 5N+1.</p>
    */
   private void postProcessBarSeries()
   {
      // get the 'patch' child of the specgraph.barseries object and verify that its "Faces" and "Vertices" properties
      // are as described in the function header.
      HGObject patchObj = null;
      if(getChildCount() > 0) patchObj = getChildAt(0);
      if(patchObj == null || !patchObj.getType().equals("patch")) return;
      
      int nBars = 0, nVerts = 0;
      double[][] faces = null;
      double[][] vertices = null;
      Object prop = patchObj.getProperty("Faces");
      boolean ok = (prop != null) && prop.getClass().equals(double[][].class);
      if(ok)
      {
         faces = (double[][]) prop;
         ok = faces.length > 1;
         for(int i=0; ok && i < faces.length; i++) ok = (faces[i] != null) && (faces[i].length == 4);
         if(ok) nBars = faces.length;
      }

      if(ok)
      {
         prop = patchObj.getProperty("Vertices");
         ok = (prop != null) && prop.getClass().equals(double[][].class);
         if(ok)
         {
            vertices = (double[][]) prop;
            for(int i=0; ok && i < vertices.length; i++) 
               ok = (vertices[i] != null) && (vertices[i].length == 2 || vertices[i].length == 3);
         }
         if(ok)
         {
            nVerts = vertices.length;
            ok = (nVerts == 4*nBars) || (nVerts == 5*nBars + 1);
         }
      }
      if(!ok) return;
      
      // verify that p1.x == p2.x, p3.x == p4.x, p2.y == p3.y, and p1.y == p4.y for all bars, and that the actual bar
      // width W ~= p3.x - p2.x is the same across all bars. If so, the bar series orientation is vertical. Also 
      // remember the index of the first vertex of the first bar drawn.
      boolean isVert = true;
      double actualBW = 0;
      int p1Idx = -1;
      for(int i=0; ok && i<nBars; i++)
      {
         int v1 = ((int) faces[i][0]) - 1;
         int v2 = ((int) faces[i][1]) - 1;
         int v3 = ((int) faces[i][2]) - 1;
         int v4 = ((int) faces[i][3]) - 1;
         ok = (v1>=0 && v1<nVerts) && (v2>=0 && v2<nVerts) && (v3>=0 && v3<nVerts) && (v4>=0 && v4<nVerts);
         if(!ok) break;
         
         ok = (vertices[v1][0] == vertices[v2][0]) && (vertices[v3][0] == vertices[v4][0]) && 
               (vertices[v2][1] == vertices[v3][1]) && (vertices[v1][1] == vertices[v4][1]);
         if(ok)
         {
            if(i == 0)
            {
               actualBW = Math.abs(vertices[v3][0] - vertices[v2][0]);
               p1Idx = v1;
            }
            else
               ok = (Math.abs((vertices[v3][0] - vertices[v2][0]) - actualBW) / actualBW) <= 0.001;
         }
      }
      
      // if prior test failed, then presumably the bar series orientation is horizontal, in which case p1.x == p4.x,
      // p2.x == p3.x, p1.y == p2.y, and p3.y == p4.y across all bars, and W ~= p3.y - p2.y.
      if(!ok)
      {
         ok = true;
         isVert = false;
         for(int i=0; ok && i<faces.length; i++)
         {
            int v1 = ((int) faces[i][0]) - 1;
            int v2 = ((int) faces[i][1]) - 1;
            int v3 = ((int) faces[i][2]) - 1;
            int v4 = ((int) faces[i][3]) - 1;
            ok = (v1>=0 && v1<nVerts) && (v2>=0 && v2<nVerts) && (v3>=0 && v3<nVerts) && (v4>=0 && v4<nVerts);
            if(!ok) break;
            
            ok = (vertices[v1][0] == vertices[v4][0]) && (vertices[v2][0] == vertices[v3][0]) && 
                  (vertices[v1][1] == vertices[v2][1]) && (vertices[v3][1] == vertices[v4][1]);
            if(ok)
            {
               if(i == 0)
               {
                  actualBW = Math.abs(vertices[v3][1] - vertices[v2][1]);
                  p1Idx = v1;
               }
               else
                  ok = (Math.abs((vertices[v3][1] - vertices[v2][1]) - actualBW) / actualBW) <= 0.001;
            }
         }
      }
      
      // if both checks above failed, then something is wrong with the patch definition. Otherwise, go ahead and save
      // the computed property values
      if(!ok) return;

      putProperty("Horizontal", isVert ? "off" : "on");
      putProperty(ACTUALBW_PROP, actualBW);
      putProperty(P1BAR_PROP, vertices[p1Idx]);
   }
   
   /** A computed property value for 'specgraph.barseries' objects: the actual bar width. Double-valued. */
   final static String ACTUALBW_PROP = "actualBW";
   /** A computed property value for 'specgraph.barseries' objects: first vertex of first bar. Type is double[]. */
   final static String P1BAR_PROP = "p1Bar";
   
   /**
    * Helper method for {@link #afterChildrenAdded()}; handles post-processing of a 'specgraph.contourgroup' object to 
    * determine the level list for the contour plot.
    * 
    * <p>If the contour group object's "LevelList" property is explicit, the method takes no action. Otherwise, it must
    * examine each of the object's 'patch' children to compile the level list. Each 'patch' renders one contour path in
    * the plot, and its 'UserData' property should be a scalar value specifying the contour level for that path. If the
    * level list is successfully constructed, the contour group's "LevelList" property is set accordingly.</p>
    */
   private void postProcessContourGroup()
   {
      // check the "LevelList" property: If it is explicit and is a non-empty double-valued array, then we don't need
      // to examine the contour group's children to determine the level list.
      double[] levelList = getDoubleVectorFromProperty(getProperty("LevelList"));
      if(levelList != null && levelList.length > 0) return;
      
      // compile the list of distinct contour levels by examining the 'patch' children.
      List<Double> levels = new ArrayList<>();
      for(int i=0; i<getChildCount(); i++)
      {
         HGObject patchObj = getChildAt(i);
         if(patchObj == null || !patchObj.getType().equals("patch")) continue;
         
         double[] userData = getDoubleVectorFromProperty(patchObj.getProperty("UserData"));
         if(userData != null && userData.length == 1 && !levels.contains(userData[0]))
            levels.add(userData[0]);
      }
      
      // if some levels were found, sort in ascending order. Limit number of levels accepted IAW constraint on FypML
      // contour node. Explicitly save the level list in the "LevelList" property.
      if(!levels.isEmpty())
      {
         Collections.sort(levels);
         while(levels.size() > ContourNode.MAXLEVELS) levels.remove(levels.size()-1);
         
         levelList = new double[levels.size()];
         for(int i=0; i<levelList.length; i++) levelList[i] = levels.get(i);
         putProperty("LevelList", levelList);
      }
   }
   
   /**
    * Under certain circumstances, coalesce the data in the specified Matlab Handle Graphics plot object with the data 
    * contained in this object. Coalescing happens after all children have been added to an 'axes' object; thus, if a 
    * plot object is indeed coalesced, it should be removed from the children list of the 'axes' (it is no longer 
    * needed). Listed here are the types of HG objects that may be coalesced, the conditions that must be met for 
    * coalescing to occur, and how the coalesced plot object is ultimately converted to a <i>FypML</i> data presentation
    * node:
    * <ol>
    * <li>Two 'line' objects are candidates for coalescing if they share the same values for selected HG properties 
    * governing their appearance -- 'Color', 'LineWidth', 'LineStyle', 'Marker', 'MarkerSize', 'MarkerEdgeColor', and 
    * 'MarkerFaceColor' --  and they have the same 'DisplayName' (which may be null). If these conditions are met, they
    * will be coalesced as long as they contain 3+ data points if no marker symbol is defined; or 2+ data points if a 
    * marker is defined. (This is because a 'line' object with just two data points and no marker symbol is translated 
    * as a <i>FypML</i> line segment and is not subject to coalescing.) If coalescing is deemed appropriate, then the 
    * specified 'line' object's data points are concatenated to an internal list of data points accumulated in the
    * coalesced 'line' object thus far. Each concatenated line's data is separated from the previous line's data with an
    * undefined point (NaN, NaN). Ultimately, this composite 'line' object will be converted to a <i>FypML</i> trace 
    * node backed by PTSET-formatted data and rendered as a collection of disjoint polylines -- thanks to those 
    * intervening undefined points.</li>
    * <li>Two 'graph2d.lineseries' objects are candidates for coalescing if they share the same values for the selected
    * HG properties governing appearance (as listed above), as well as the 'DisplayName' property. In this case, if both
    * objects contain only a single data point ('XData' and 'YData' are scalars), they are coalesced. All such objects 
    * are coalesced into a single line-series rendered as a "scatter plot" in the <i>FypML</i> figure.</li>
    * <li>Alternatively, if two 'graph2d.lineseries' objects are candidates for coalescing and contain N data points, 
    * with N &gt; 1, then they should be coalesced ONLY if the two X-coordinate vectors are IDENTICAL. Neither can have
    * the 'ZData' property, as we do NOT coalesce 3D line plots. All such objects are coalesced into one line-series 
    * that is ultimately translated into a <i>FypML</i> trace node configured in the "multitrace" display mode, with 
    * MSET- or MSERIES-formatted data. However, coalescing is disabled when the containing HG 'axes' is configured as a 
    * polar plot. Upon converting each line-series from Cartesian to polar coordinates, the individual theta-coordinate 
    * vectors will no longer be identical and thus cannot be stored as MSET-formatted data. Other considerations in the
    * polar context:
    *    <ol>
    *    <li>Any 'graph2d.lineseries' consistent with the output of Matlab's <i>rose()</i> function ("angle histogram")
    *    is NOT coalesced; each rose plot is ultimately translated as a distinct <i>FypML</i> trace node.</li>
    *    <li>All 'graph2d.lineseries' objects that represent the individual vectors of a compass plot are coalesced
    *    into a single plot object. Each such vector includes an arrowhead proportional to vector length and is defined
    *    by a sequence of 5 points: (0,0), (a,b), (c,d), (a,b), (e,f). Here (a,b) is the vector endpoint, while (c,d)
    *    and (e,f) are the endpoints of the arrowhead. In this case, the coalesced data is merely the sequence of 
    *    vector endpoints. The arrowheads are NOT preserved upon translation to FypML; instead, the arrowhead size is
    *    the same for all vectors.</li>
    *    </ol>
    * </li>
    * 
    * <li>Another 'specgraph.barseries' object can be coalesced with this 'barseries' if they have the same orientation
    * (H or V), actual bar width, the same 'XData', and the 'YData' should be an N-vector where N is the same for both 
    * objects. We cannot rely on the reported values of the Matlab barseries properties "Horizontal", "BarLayout", and 
    * "BarWidth". These will all have the same value even when you create a graph with two different kinds of bar plots.
    * The "Horizontal" property is set unambiguously by analyzing each barseries' child "patch" object; at the same 
    * time, the actual bar width is found and stored as the {@link #ACTUALBW_PROP} property, and the first bar vertex
    * in each barseries is stored in the {@link #P1BAR_PROP} property. These "calculated" properties are set when the
    * 'barseries' HG object is first created, after adding its lone 'patch" child; see {@link #postProcessBarSeries()}.
    * Once one 'barseries object has been coalesced with this one, we now have a bar plot with at least two bar groups,
    * and we can unambiguously determine the true values for "BarLayout" and "BaseValue" properties of the coalesced
    * plot by checking the first bar vertex of the two series. For vertical grouped or horizontal stacked plots, the
    * Y-coordinates of the two vertices should match; otherwise, the X-coordinates should match. This becomes an 
    * additional check once the first two 'barseries' have been coalesced. If the candidate 'barseries' satisfies all
    * constraints, its Y-data is coalesced with this 'barseries' object, which will be backed by a MSET or MSERIES data 
    * source, and ultimately converted to a <i>FypML</i> bar plot node. The bar plot layout (H or V, grouped or stacked)
    * is determined as describe above, and its stroke properties are based on the relevant HG properties defined on this
    * 'barseries' object; typically, these will be the same across all bar groups, and the <i>FypML</i> bar element does
    * not support applying different stroke properties to different groups. It DOES support applying a different fill 
    * color and legend label to each group, and this information is collected in this 'barseries' object, where it will 
    * be needed when the object is converted to <i>FypML</i>. <b><i>FypML</i> does not support bar plots in a polar 
    * coordinate system, so coalescing of 'barseries' is disabled in a polar context.</b></li>
    * 
    * <li>Another 'specgraph.areaseries' object can be coalesced with this 'areaseries' if they have the same baseline
    * value, the same 'XData', and the 'YData' should be an N-vector where N is the same for both objects. When the 
    * candidate 'areaseries' satisfies these constraints, its Y-data is coalesced with this 'areaseries' object, which 
    * will be backed by a MSET or MSERIES data source, and ultimately converted to a <i>FypML</i> area chart node. The 
    * chart's stroke properties are based on the relevant HG properties defined on this 'areaseries' object; typically, 
    * these will be the same across all data groups ("bands") in the area chart; in fact, the <i>FypML</i> area element 
    * does not support applying different stroke properties to different groups. It DOES support applying a different 
    * fill color and legend label to each group, and this information is collected in this 'areaseries' object, where it
    * will be needed when the object is converted to <i>FypML</i>.</li>
    * 
    * <li>Another 'specgraph.scattergroup' object can be coalesced with this 'scattergroup' if both represent simple
    * scatter plots in which all symbols have the same size and appearance, and that common symbol is the same for both
    * 'scattergroup' objects (same shape, same size, same stroke, and same fill color). In that case, the 'XData',
    * 'YData' -- and in the 3D case, 'ZData' -- vectors of the coalesced scatter groups are collected internally and 
    * used to form the PTSET or XYZSET raw data array for the <i>FypML</i> scatter plot node generated.</li>
    * 
    * <li>In a 3D graph context, another 'surface' object can be coalesced with this 'surface' if both represent 3D
    * bar plots generated by the Matlab <i>bar3()</i> function and both have the same appearance. They must have the 
    * same Z base plane, the same bar size, and the same values for the following properties: <i>FaceColor, FaceAlpha, 
    * EdgeColor, EdgeAlpha, LineStyle, LineWidth</i>. They need not render the same number of bars, although that will 
    * typically be the case for the output of <i>bar3()</i>. Both this and the candidate surface object are analyzed
    * prior to the coalescing stage, and the set of (X,Y,Z) data points represented by the bars are stored internally
    * in {@link #bar3DPoints}, so coalescing is simply a matter of adding the coalesced surface object's points to
    * this object. The list of 3D points are used to form the XYZSET data array for the <i>FypML</i> 3D scatter plot
    * node that will render the 3D bar plot.</li>
    * </ol>
    * 
    * @param hgObj The candidate HG object to be coalesced with this plot object.
    * @param hgAxes The Matlab "axes" object containing this plot object and the candidate object.
    * @return True if specified object was coalesced with this object, as described; else false.
    */
   private boolean coalesce(HGObject hgObj, HGObject hgAxes)
   {
      // if the candidate object passes this test, then coalescing is warranted
      if(hgObj==null || hgAxes==null || !shouldCoalesce(this, hgObj, hgAxes)) return(false);

      switch(type)
      {
         case "line":
         {
            // for "line" objects, XData and YData are double[] with a minimum length of 2.
            double[] xData;
            double[] yData;
            if(scatterPoints == null)
            {
               scatterPoints = new ArrayList<>();
               xData = (double[]) getProperty("XData");
               yData = (double[]) getProperty("YData");
               for(int i = 0; i < xData.length; i++) scatterPoints.add(new Point2D.Double(xData[i], yData[i]));
            }
            scatterPoints.add(new Point2D.Double(Double.NaN, Double.NaN));
            xData = (double[]) hgObj.getProperty("XData");
            yData = (double[]) hgObj.getProperty("YData");
            for(int i = 0; i < xData.length; i++) scatterPoints.add(new Point2D.Double(xData[i], yData[i]));
            break;
         }
         case "graph2d.lineseries":
         {
            double[] yData = getDoubleVectorFromProperty(hgObj.getProperty("YData"));
            if(yData.length == 1)
            {
               // in this case, we know the XData and YData properties are all java.lang.Double
               if(scatterPoints == null)
               {
                  scatterPoints = new ArrayList<>();
                  scatterPoints.add(new Point2D.Double(((Double) getProperty("XData")), ((Double) getProperty("YData"))));
               }
               scatterPoints.add(new Point2D.Double(((Double) hgObj.getProperty("XData")), yData[0]));
            } else if(hgAxes.isPolarAxes() && isCompassPlotVector && hgObj.isCompassPlotVector)
            {
               // in this case, we know the XData and YData properties are of length 5, and the coalesced compass plot
               // object only saves the vector endpoint == (x[1], y[1]).
               double[] xData;
               if(scatterPoints == null)
               {
                  scatterPoints = new ArrayList<>();
                  xData = (double[]) getProperty("XData");
                  yData = (double[]) getProperty("YData");
                  scatterPoints.add(new Point2D.Double(xData[1], yData[1]));
               }
               xData = (double[]) hgObj.getProperty("XData");
               yData = (double[]) hgObj.getProperty("YData");
               scatterPoints.add(new Point2D.Double(xData[1], yData[1]));
            } else
            {
               if(multiSetData == null)
               {
                  multiSetData = new ArrayList<>();
                  multiSetData.add((double[]) getProperty("YData"));
               }
               multiSetData.add(yData);
            }
            break;
         }
         case "specgraph.barseries":
         {
            // it is assumed that each 'barseries' object represents one bar group in the Matlab bar plot. This 'barseries'
            // object contains the properties that determine the layout, orientation, and common stroke properties of the
            // eventual <i>FypML</i> bar plot node. Here we need to collect the following information for the 'barseries'
            // object being coalesed: the YData, the bar group legend label, and the bar group color.

            // if we're coalescing the first two bar groups, we can now unambiguously determine the bar group layout and
            // baseline value by examing the first bar vertex of each barseries object. The vertex and orientation (H,V)
            // of each bar group was determined when the corresponding HGObject was initially created.
            boolean initial = (multiSetData == null);
            if(initial)
            {
               Object prop = getProperty("Horizontal");
               boolean isVert = prop == null || "off".equals(prop);

               double[] p1 = getDoubleVectorFromProperty(getProperty(P1BAR_PROP));
               double[] p1_2 = getDoubleVectorFromProperty(hgObj.getProperty(P1BAR_PROP));
               if(p1 != null && p1_2 != null && p1.length >= 2 & p1_2.length >= 2)
               {
                  boolean isGrp;
                  double baseline;
                  if(isVert)
                  {
                     // for V grouped(stacked) bar plots, the x-coordinates are unequal (equal). Baseline value is the
                     // Y-coordinate of the first bar vertex of the first bar group.
                     isGrp = (p1[0] != p1_2[0]);
                     baseline = p1[1];
                  } else
                  {
                     // for H grouped (stacked) bar plots, the y-coordinates are unequal (equal). Baseline value is the
                     // X-coordinate of the first bar vertex of the first bar group.
                     isGrp = (p1[1] != p1_2[1]);
                     baseline = p1[0];
                  }

                  putProperty("BarLayout", isGrp ? "grouped" : "stacked");
                  putProperty("BaseValue", baseline);
               }
            }

            if(initial)
            {
               multiSetData = new ArrayList<>();
               multiSetData.add(getDoubleVectorFromProperty(getProperty("YData")));
            }
            multiSetData.add(getDoubleVectorFromProperty(hgObj.getProperty("YData")));

            if(initial)
            {
               dataGrpLabels = new ArrayList<>();
               dataGrpLabels.add(extractDataGroupLegendLabel(0));
            }
            dataGrpLabels.add(hgObj.extractDataGroupLegendLabel(dataGrpLabels.size()));

            if(initial)
            {
               dataGrpColors = new ArrayList<>();
               dataGrpColors.add(extractDataGroupFillColor());
            }
            dataGrpColors.add(hgObj.extractDataGroupFillColor());
            break;
         }
         case "specgraph.areaseries":
         {
            // each coalesced 'areaseries' object represents one band in the Matlab area chart. This 'areaseries' object
            // contains the properties that determine the baseline value and stroke properties of the eventual <i>FypML</i>
            // area chart node. Here we need to collect the following information for the 'areaseries' object being
            // coalesed: the YData, the data group legend label, and the data group color.

            boolean initial = (multiSetData == null);
            if(initial)
            {
               multiSetData = new ArrayList<>();
               multiSetData.add(getDoubleVectorFromProperty(getProperty("YData")));
            }
            multiSetData.add(getDoubleVectorFromProperty(hgObj.getProperty("YData")));

            if(initial)
            {
               dataGrpLabels = new ArrayList<>();
               dataGrpLabels.add(extractDataGroupLegendLabel(0));
            }
            dataGrpLabels.add(hgObj.extractDataGroupLegendLabel(dataGrpLabels.size()));

            if(initial)
            {
               dataGrpColors = new ArrayList<>();
               dataGrpColors.add(extractDataGroupFillColor());
            }
            dataGrpColors.add(hgObj.extractDataGroupFillColor());
            break;
         }
         case "specgraph.scattergroup":
         {
            // the coalesced 'scattergroup' objects are combined into one FypML scatter or scatter3d node with a PTSET or
            // XYZSET data source, respectively. Here we compile a list of the data vectors extracted from the individual
            // scattergroup objects: XData1, YData1, XData2, YData2,... for the 2D scatter plot; XData1, YData1, ZData1,
            // XData2, ... for the 3D case

            boolean initial = (multiSetData == null);
            if(initial)
            {
               multiSetData = new ArrayList<>();
               multiSetData.add(getDoubleVectorFromProperty(getProperty("XData")));
               multiSetData.add(getDoubleVectorFromProperty(getProperty("YData")));
               double[] zData = getDoubleVectorFromProperty(getProperty("ZData"));
               if(zData != null && zData.length > 0) multiSetData.add(zData);
            }
            multiSetData.add(getDoubleVectorFromProperty(hgObj.getProperty("XData")));
            multiSetData.add(getDoubleVectorFromProperty(hgObj.getProperty("YData")));
            double[] zData = getDoubleVectorFromProperty(hgObj.getProperty("ZData"));
            if(zData != null && zData.length > 0) multiSetData.add(zData);
            break;
         }
         case "surface":
            // 'surface' objects rendering 3D bar plots in a 3D graph context (generated by bar3()): during
            // post-processing and before coalescing, the set of 3D data points represented by the individual bars rendered
            // by the 'surface' are computed and stored internally. All we have to do here is add the coalesced surface's
            // data points to this surface.
            bar3DPoints.addAll(hgObj.bar3DPoints);
            break;
      }
      
      return(true);
   }

   /**
    * Helper method that checks the properties of this HG 'specgraph.barseries' or 'specgraph.areaseries' object for the
    * string that serves as the data group's legend label. The 'DisplayName' property, if present, serves as the legend 
    * label. If that is not set, the method looks for the field 'legend_texthandle' in the object's 'ApplicationData' 
    * property. If present, then it is assumed that the bar or area chart component appears in the legend, but the 
    * actual label string is not available (it's probably in a 'text' child of a 'scribe.legend' object, but the 
    * FIG-to-FypML import engine does not handle this level of detail!); in this scenario, the label is set to "group 
    * {i}", where {i} is the data group index as an integer string. If neither property is found, the legend label will 
    * be null.
    * 
    * @param idx The index position of the data group represented by this bar or area series plot object.
    * @return The legend label, if any found; else null. Returns null always if this is neither a bar series nor an
    * area series plot object.
    */
   private String extractDataGroupLegendLabel(int idx)
   {
      if(!(type.equals("specgraph.barseries") || type.equals("specgraph.areaseries"))) return(null);
      
      // HG "DisplayName" property, if present and not an empty string, is the data group's legend label. 
      String label = null;
      Object prop = getProperty("DisplayName");
      if(prop != null && (prop.getClass().equals(String.class) || prop.getClass().equals(Character.class)))
      {
         label = prop.toString().trim();
         if(label.isEmpty()) label = null;
      }

      // if no label in 'DisplayName', check field 'legend_texthandle' in the HG object's 'ApplicationData' property. If
      // exists, the code that parses Matlab HG structure sets it as a Double-valued property directly on the HG object.
      // if present, set label to "group N".
      if(label == null)
      {
         prop = getProperty("legend_texthandle");
         if(prop != null && prop.getClass().equals(Double.class)) label = "group " + idx;
      }
      
      return(label);
   }
   
   /**
    * Helper method examines the properties of this HG 'specgraph.barseries', 'specgraph.areaseries' or 'patch' object 
    * B to determine the fill color assigned to the data group represented by the object B within a coalesced series.
    * <ul>
    * <li>First process B.FaceColor. If "none", use transparent black as the fill color (<i>FypML</i> now supports 
    * translucent data group fill colors in bar or area charts). If it is a Matlab ColorSpec, use that.</li>
    * <li>If B.FaceColor == "flat" or "interp", or if it is implicit (the default is "flat"), look at B.FaceVertexCData 
    * next. When the object B is a bar- or area-series, it will normally have a single 'patch' child P, and we examine
    * P rather than B for the 'FaceVertexCData' property; when B is a 'patch' itself, we check it directly. Since 
    * <i>FypML</i> does not support gradient fills, we just look at the first color entry available in this property,
    * which is typically a vector of color map indices or an Nx3 matrix of color specs.</li>
    * <li>If we get a color map index out of FaceVertexCData, then we have to check B.CDataMapping to see if it's a
    * scaled mapping, in which case we negate the color index to indicate this (valid color indices are non-zero).
    * In this case, the fill color is returned as an integer. When the coalesced plot is converted to its <i>FypML</i>
    * counterpart, this color index will be converted to an RGB color.</li>
    * <li>In the end, if we fail to find a color specification, the data group fill color defaults to opaque black.</li>
    * <li>For a 'barseries or 'areaseries' object, if an RGB color was found (not a color index), we then check the
    * B.FaceAlpha to see if the face color should be translucent or transparent. B.FaceAlpha should be a scalar value
    * between 0 (transparent) and 1 (opaque), with a default value of 1.</li>
    * </ul>
    * 
    * @return The data group fill color, returned either as an RGB color specification or an integer color index.
    * Returns null if this is not a "patch", "specgraph.barseries", or "specgraph.areaseries" object.
    */
   private Object extractDataGroupFillColor()
   {
      if(!(type.equals("specgraph.barseries") || type.equals("specgraph.areaseries") || type.equals("patch"))) 
         return(null);

      // check B.FaceAlpha for alpha component of face color
      Object prop = getProperty("FaceAlpha");
      int alpha = 255;
      if(prop != null && prop.getClass().equals(Double.class))
         alpha = Utilities.rangeRestrict(0, 255, (int) (255.0 * (Double) prop));
      
      // the default (implicit) value for FaceColor property is "flat"
      prop = getProperty("FaceColor");
      if(prop == null) prop = "flat";
      
      // transparent black
      if("none".equals(prop)) return(new Color(0,0,0,0));
      
      Color fillC = MatlabUtilities.processColorSpec(prop);
      if(fillC != null) return(new Color(fillC.getRed(), fillC.getGreen(), fillC.getBlue(), alpha));
      if(!("flat".equals(prop) || "interp".equals(prop))) return(Color.BLACK);
      
      // get the "FaceVertexCData" property from the 'patch' object itself -- or from a child 'patch' object in the case
      // of a bar- or area-series. We just want to get one color out of it, since we don't support different colors per 
      // face or per vertex. It may be indexed or true color, which complicates things. If there is no 'patch', or the 
      // 'FaceVertexCData' property is missing, then we use black as the fill color.
      int colorIdx = -1;
      HGObject patchObj = type.equals("patch") ? this : ((getChildCount() > 0) ? getChildAt(0) : null);
      prop = ((patchObj != null) && patchObj.type.equals("patch")) ? patchObj.getProperty("FaceVertexCData") : null;
      if(prop == null) return(new Color(0,0,0,alpha));
      
      if(prop.getClass().equals(Double.class))
      {
         colorIdx = (int) ((Double) prop).doubleValue();
      }
      else if(prop.getClass().equals(double[].class))
      {
         // array case is ambiguous when length is 3, since it could represent a single RBG color spec to be applied
         // to all faces. If this is not the case, then it contains color indices, and we just go ahead and use the
         // first one.
         double[] a = (double[]) prop;
         if(a.length == 3 && a[0] >= 0 && a[0] <= 1 && a[1] >= 0 && a[1] <= 1 && a[2] >= 0 && a[2] <= 1)
            fillC = new Color((float) a[0], (float) a[1], (float) a[2]);
         else
            colorIdx = (int) a[0];
      }
      else if(prop.getClass().equals(double[][].class))
      {
         // matrix case: matrix will be Nx3, where N is the number of faces or number of vertices, and each triplet
         // is an RGB color. We simply use the first triplet, since we don't support multiple colors or gradients!
         double[][] a = (double[][]) prop;
         double[] rgb = a[0];
         if(rgb == null || rgb.length != 3 || 
               !(rgb[0] >= 0 && rgb[0] <= 1 && rgb[1] >= 0 && rgb[1] <= 1 && rgb[2] >= 0 && rgb[2] <= 1))
            fillC = Color.BLACK;
         else
            fillC = new Color((float) rgb[0], (float) rgb[1], (float) rgb[2]);
      }
      else
         fillC = Color.BLACK;
     
      if(fillC != null) return(new Color(fillC.getRed(), fillC.getGreen(), fillC.getBlue(), alpha));
      if(colorIdx < 1) return(new Color(0,0,0,alpha));
      
      // fill color is specified by a color index. If the mapping is scaled rather than direct, we negate the index to
      // indicate this. The color index will be converted to an RGB color later.
      prop = getProperty("CDataMapping");
      if(prop == null || "scaled".equals(prop)) colorIdx = -colorIdx;
      
      return(colorIdx);
   }
   
   /**
    * This method tests whether or not two Handle Graphics objects are plot objects of the same type that can be 
    * coalesced into one plot. Coalescing handles use cases in which a collection of Matlab HG plot objects within an
    * 'axes' object are related in some way and can be converted to a single <i>FypML</i> data presentation node. For 
    * details on the relevant uses cases and the conditions that must be met to coalesce the HG objects, see {@link 
    * #coalesce(HGObject, HGObject)}.
    * 
    * @param hg1 One Handle Graphics plot object. Must not be null.
    * @param hg2 A second Handle Graphics plot object. Must not be null. The method tests whether this object may be
    * coalesced with the first. The first plot object may already represent a coalesced plot object.
    * @param hgAxes The Matlab "axes" object that contains the two plot objects
    * @return True if the second plot object may be coalesced with the first; else false.
    */
   private static boolean shouldCoalesce(HGObject hg1, HGObject hg2, HGObject hgAxes)
   {
      String plotType = hg1.type;
      if(!plotType.equals(hg2.type)) return(false);
      
      boolean ok = plotType.equals("graph2d.lineseries") || plotType.equals("line") ||
            plotType.equals("specgraph.barseries") || plotType.equals("specgraph.areaseries") ||
            plotType.equals("specgraph.scattergroup") || plotType.equals("surface");
      if(!ok) return(false);

      switch(plotType)
      {
         case "specgraph.scattergroup":
         {
            // both must be simple scatter plots, in which all symbols have the same size and appearance.
            ok = isSimpleScatterPlot(hg1) && isSimpleScatterPlot(hg2);

            // verify that the marker symbol has the same size and appearance
            if(ok)
            {
               Object prop1 = hg1.getProperty("SizeData");
               Object prop2 = hg2.getProperty("SizeData");
               ok = (prop1 == null && prop2 == null) || (prop1 != null && prop1.equals(prop2));
            }
            if(ok)
            {
               Object prop1 = hg1.getProperty("LineWidth");
               Object prop2 = hg2.getProperty("LineWidth");
               ok = (prop1 == null && prop2 == null) || (prop1 != null && prop1.equals(prop2));
            }
            if(ok)
            {
               Object prop1 = hg1.getProperty("Marker");
               Object prop2 = hg2.getProperty("Marker");
               ok = (prop1 == null && prop2 == null) || (prop1 != null && prop1.equals(prop2));
            }
            if(ok)
            {
               // MarkerEdgeColor = "flat" (default), "none", or a Matlab color specification
               Object prop1 = hg1.getProperty("MarkerEdgeColor");
               Object prop2 = hg2.getProperty("MarkerEdgeColor");

               Color c1 = null;
               if(!"none".equals(prop1))
               {
                  if(prop1 == null || "flat".equals(prop1))
                     c1 = MatlabUtilities.processColorSpec(hg1.getProperty("CData"));
                  else c1 = MatlabUtilities.processColorSpec(prop1);
               }
               Color c2 = null;
               if(!"none".equals(prop2))
               {
                  if(prop2 == null || "flat".equals(prop2))
                     c2 = MatlabUtilities.processColorSpec(hg2.getProperty("CData"));
                  else c2 = MatlabUtilities.processColorSpec(prop2);
               }
               ok = (c1 == null && c2 == null) || (c1 != null && c1.equals(c2));

               // if edge color is not "none", ensure alpha components are the same
               if(ok && !"none".equals(prop1))
               {
                  prop1 = hg1.getProperty("MarkerEdgeAlpha");
                  prop2 = hg2.getProperty("MarkerEdgeAlpha");
                  ok = (prop1 == null && prop2 == null) || (prop1 != null && prop1.equals(prop2));
               }
            }
            if(ok)
            {
               // MarkerFaceColor = "none" (default), "flat", "auto", or Matlab color specification
               Object prop1 = hg1.getProperty("MarkerFaceColor");
               Object prop2 = hg2.getProperty("MarkerFaceColor");

               boolean isNone = (prop1 == null || "none".equals(prop1)) && (prop2 == null || "none".equals(prop2));
               boolean isAuto = ("auto".equals(prop1) && "auto".equals(prop2));
               ok = isNone || isAuto;
               if(!ok)
               {
                  Color c1 = MatlabUtilities.processColorSpec(prop1);
                  if(c1 == null && "flat".equals(prop1))
                     c1 = MatlabUtilities.processColorSpec(hg1.getProperty("CData"));

                  Color c2 = MatlabUtilities.processColorSpec(prop2);
                  if(c2 == null && "flat".equals(prop2))
                     c2 = MatlabUtilities.processColorSpec(hg2.getProperty("CData"));

                  ok = (c1 != null) && (c1.equals(c2));
                  if(ok)
                  {
                     // make sure alpha components are the same
                     prop1 = hg1.getProperty("MarkerFaceAlpha");
                     prop2 = hg2.getProperty("MarkerFaceAlpha");
                     ok = (prop1 == null && prop2 == null) || (prop1 != null && prop1.equals(prop2));
                  }
               }
            }

            // finally, verify that the XData and YData properties have the same length for each individual scatter group.
            // If there's a ZData vector, that also must have the same length as XData. Any 'scattergroup' that does not
            // meet this criterion won't be converted to FypML anyway. Note that there's no need to compare across the two
            // groups -- XData1.length need not match XData2.length. However, if one group has ZData, so must the other --
            // for coalescing 3D scatter plots.
            if(ok)
            {
               double[] xData = getDoubleVectorFromProperty(hg1.getProperty("XData"));
               double[] yData = getDoubleVectorFromProperty(hg1.getProperty("YData"));
               double[] zData = getDoubleVectorFromProperty(hg1.getProperty("ZData"));
               ok = xData != null && yData != null && xData.length == yData.length;
               boolean hasZData = zData != null && zData.length > 0;
               if(ok && hasZData) ok = xData.length == zData.length;

               if(ok)
               {
                  xData = getDoubleVectorFromProperty(hg2.getProperty("XData"));
                  yData = getDoubleVectorFromProperty(hg2.getProperty("YData"));
                  zData = getDoubleVectorFromProperty(hg2.getProperty("ZData"));
                  ok = xData != null && yData != null && xData.length == yData.length;
                  if(ok)
                  {
                     if(hasZData) ok = zData != null && zData.length == xData.length;
                     else ok = zData == null || zData.length == 0;
                  }
               }
            }

            return (ok);
         }
         case "specgraph.barseries":
         {
            // a bar plot in a polar graph is not supported. Don't bother coalescing at all.
            if(hgAxes.isPolarAxes()) return (false);

            // check 'Horizontal' property: 'on' or 'off'. The implicit default is 'off'.
            Object prop1 = hg1.getProperty("Horizontal");
            boolean isVert = (prop1 == null) || "off".equals(prop1);
            Object prop2 = hg2.getProperty("Horizontal");
            ok = isVert == ((prop2 == null) || "off".equals(prop2));

            // is YData the same length for both 'barseries' objects? NOTE that we assume here that YData will only be a
            // vector, not a matrix.
            int len = 0;
            if(ok)
            {
               double[] yData1 = getDoubleVectorFromProperty(hg1.getProperty("YData"));
               double[] yData2 = getDoubleVectorFromProperty(hg2.getProperty("YData"));
               ok = (yData1 != null) && (yData2 != null) && (yData1.length == yData2.length);
               if(ok) len = yData1.length;
            }

            // check 'XDataMode' and 'XData' properties. If 'XData' is explicit, no need to check 'XDataMode'. If 'XData'
            // is implicit, then expect 'XDataMode' == 'auto' (the default). Coalescing is appropriate only if the two
            // bar series' X-vectors are both implicit, or they're both explicit and have the same length = len(YData).
            if(ok)
            {
               double[] xData1 = getDoubleVectorFromProperty(hg1.getProperty("XData"));
               double[] xData2 = getDoubleVectorFromProperty(hg2.getProperty("XData"));
               ok = (xData1 != null) && (xData2 != null) && (xData1.length == xData2.length) && (len == xData1.length);
               if(!ok)
               {
                  // check for 'auto' case
                  prop1 = hg1.getProperty("XDataMode");
                  prop2 = hg2.getProperty("XDataMode");
                  boolean isAuto1 = (prop1 == null) || "auto".equals(prop1);
                  boolean isAuto2 = (prop2 == null) || "auto".equals(prop2);
                  ok = xData1 == null && xData2 == null && (isAuto1 == isAuto2);
               }
            }

            // check the calculated actual bar width. Should be explicit for both bar series, else ignore it.
            if(ok)
            {
               prop1 = hg1.getProperty(ACTUALBW_PROP);
               prop2 = hg2.getProperty(ACTUALBW_PROP);
               if(prop1 != null && prop2 != null)
                  ok = Math.abs((Double) prop1 - (Double) prop2) < 0.0001;
            }

            // if the first barseries already contains coalesced data, then the 'BarLayout' property has been determined
            // unambiguously. In this case, we have expectations on the first bar vertex of the candidate barseries...
            if(ok && hg1.isCoalescedBarPlot())
            {
               double[] p1_1 = getDoubleVectorFromProperty(hg1.getProperty(P1BAR_PROP));
               double[] p1_2 = getDoubleVectorFromProperty(hg2.getProperty(P1BAR_PROP));

               // the above properties should always be defined at this point. But if they aren't, we simply exclude this
               // check in our considerations.
               if(p1_1 != null && p1_2 != null && p1_1.length >= 2 && p1_2.length >= 2)
               {
                  prop1 = hg1.getProperty("BarLayout");
                  boolean isGrp = (prop1 == null) || "grouped".equals(prop1);

                  // for V grouped(stacked) bar plots, the x-coordinates are unequal (equal). For H grouped (stacked) bar
                  // plots, the y-coordinates are unequal (equal).
                  int idx = isVert ? 0 : 1;
                  ok = isGrp ? (p1_1[idx] != p1_2[idx]) : (p1_1[idx] == p1_2[idx]);
               }
            }

            return (ok);
         }
         case "specgraph.areaseries":
         {
            // check 'BaseValue' property. Both must share the same baseline value. Object will be a Double, so we can
            // safely use Object.equals.
            // value will be a Double, so we can safely use Object.equals
            Object prop1 = hg1.getProperty("BaseValue");
            Object prop2 = hg2.getProperty("BaseValue");
            ok = (prop1 == null && prop2 == null) || (prop1 != null && prop1.equals(prop2));

            // is YData the same length for both 'areaseries' objects? NOTE that we assume here that YData will only be a
            // vector, not a matrix.
            int len = 0;
            if(ok)
            {
               double[] yData1 = getDoubleVectorFromProperty(hg1.getProperty("YData"));
               double[] yData2 = getDoubleVectorFromProperty(hg2.getProperty("YData"));
               ok = (yData1 != null) && (yData2 != null) && (yData1.length == yData2.length);
               if(ok) len = yData1.length;
            }

            // check 'XDataMode' and 'XData' properties. If 'XData' is explicit, no need to check 'XDataMode'. If 'XData'
            // is implicit, then expect 'XDataMode' == 'auto' (the default). Coalescing is appropriate only if the two
            // X-vectors are both implicit, or they're both explicit, have the same length = len(YData), and are equal.
            if(ok)
            {
               double[] xData1 = getDoubleVectorFromProperty(hg1.getProperty("XData"));
               double[] xData2 = getDoubleVectorFromProperty(hg2.getProperty("XData"));
               ok = (xData1 != null) && (xData2 != null) && Arrays.equals(xData1, xData2) && (xData1.length == len);
               if(!ok)
               {
                  // check for 'auto' case
                  prop1 = hg1.getProperty("XDataMode");
                  prop2 = hg2.getProperty("XDataMode");
                  boolean isAuto1 = (prop1 == null) || "auto".equals(prop1);
                  boolean isAuto2 = (prop2 == null) || "auto".equals(prop2);
                  ok = xData1 == null && xData2 == null && (isAuto1 == isAuto2);
               }
            }

            return (ok);
         }
         case "surface":
         {
            if(!(hgAxes.is3D && hg1.isBar3D && hg2.isBar3D)) return (false);

            // difference in base plane Z-coordinates must be 0.1% or less, but we have to be careful b/c the base plane Z
            // could be zero, and we cannot divide by zero to find %-age!
            ok = (hg1.bar3DBaseZ == hg1.bar3DBaseZ);
            if(!ok)
            {
               double d = Math.abs(hg1.bar3DBaseZ - hg2.bar3DBaseZ);
               ok = ((d / Math.abs(hg1.bar3DBaseZ != 0 ? hg1.bar3DBaseZ : hg2.bar3DBaseZ)) <= 0.001);
            }

            // difference in bar size must be 0.1% or less (bar size cannot be zero).
            if(ok) ok = (Math.abs(hg1.bar3DSize - hg2.bar3DSize) / Math.abs(hg1.bar3DSize) <= 0.001);

            // FaceColor: Both can be "interp" or "none", or both can be an RGB color spec. Default (implicit) value is
            // "flat", which FypML does not support.
            if(ok)
            {
               Object prop1 = hg1.getProperty("FaceColor");
               Object prop2 = hg2.getProperty("FaceColor");
               ok = (prop1 != null) && prop1.equals(prop2);
               if(!ok)
               {
                  Color c1 = MatlabUtilities.processColorSpec(prop1);
                  Color c2 = MatlabUtilities.processColorSpec(prop2);
                  ok = (c1 != null) && c1.equals(c2);
               }
            }

            // FaceAlpha: Must be scalar double in [0..1]. If implicit, alpha is 1 (opaque).
            if(ok)
            {
               Object prop1 = hg1.getProperty("FaceAlpha");
               double alpha1 = 1.0;
               if(prop1 != null)
               {
                  double[] vec = getDoubleVectorFromProperty(prop1);
                  if(vec != null && vec.length == 1) alpha1 = vec[0];
               }
               Object prop2 = hg2.getProperty("FaceAlpha");
               double alpha2 = 1.0;
               if(prop2 != null)
               {
                  double[] vec = getDoubleVectorFromProperty(prop2);
                  if(vec != null && vec.length == 1) alpha2 = vec[0];
               }
               ok = (alpha1 == alpha2);
            }

            // EdgeColor: Both can be none or RGB color spec; implicit value is black. */
            if(ok)
            {
               Object prop1 = hg1.getProperty("EdgeColor");
               Object prop2 = hg2.getProperty("EdgeColor");
               ok = "none".equals(prop1) && "none".equals(prop2);
               if(!ok)
               {
                  Color c1 = (prop1 == null) ? Color.BLACK : MatlabUtilities.processColorSpec(prop1);
                  Color c2 = (prop2 == null) ? Color.BLACK : MatlabUtilities.processColorSpec(prop2);
                  ok = (c1 != null) && c1.equals(c2);
               }
            }

            // EdgeAlpha: Must be scalar double in [0..1]. If implicit, alpha is 1 (opaque).
            if(ok)
            {
               Object prop1 = hg1.getProperty("EdgeAlpha");
               double alpha1 = 1.0;
               if(prop1 != null)
               {
                  double[] vec = getDoubleVectorFromProperty(prop1);
                  if(vec != null && vec.length == 1) alpha1 = vec[0];
               }
               Object prop2 = hg2.getProperty("EdgeAlpha");
               double alpha2 = 1.0;
               if(prop2 != null)
               {
                  double[] vec = getDoubleVectorFromProperty(prop2);
                  if(vec != null && vec.length == 1) alpha2 = vec[0];
               }
               ok = (alpha1 == alpha2);
            }

            // LineStyle, LineWidth must match
            if(ok)
            {
               StrokePattern sp1 = MatlabUtilities.processLineStyle(hg1);
               StrokePattern sp2 = MatlabUtilities.processLineStyle(hg2);
               ok = (sp1 == null && sp2 == null) || StrokePattern.equal(sp1, sp2);
            }

            if(ok) ok = Measure.equal(MatlabUtilities.processLineWidth(hg1), MatlabUtilities.processLineWidth(hg2));

            return (ok);
         }
      }


      // do not coalesce 'graph2d.lineseries' that have 'ZData' property (3D line plot)
      if(plotType.equals("graph2d.lineseries"))
         ok = (hg1.getProperty("ZData") == null) && (hg2.getProperty("ZData") == null); 
      
      // handle 'line' and 'graph2d.lineseries' use cases...
      if(ok)
      {
         Color c1 = MatlabUtilities.processColorSpec(hg1.getProperty("Color"));
         Color c2 = MatlabUtilities.processColorSpec(hg2.getProperty("Color"));
         ok = (c1==null && c2==null) || (c1 != null && c1.equals(c2));
      }
      
      if(ok)
      {
         StrokePattern sp1 = MatlabUtilities.processLineStyle(hg1);
         StrokePattern sp2 = MatlabUtilities.processLineStyle(hg2);
         ok = (sp1 == null && sp2 == null) || StrokePattern.equal(sp1, sp2);
      }
      
      if(ok) ok = Measure.equal(MatlabUtilities.processLineWidth(hg1), MatlabUtilities.processLineWidth(hg2));      

      // compare marker-related properties: 'Marker', 'MarkerSize', 'MarkerEdgeColor', 'MarkerFaceColor'. 
      if(ok)
      {
         // value will be a Character or String, so we can safely use Object.equals
         Object prop1 = hg1.getProperty("Marker");
         Object prop2 = hg2.getProperty("Marker");
         ok = (prop1 == null && prop2 == null) || (prop1 != null && prop1.equals(prop2));
      }
      if(ok)
      {
         // value will be a Double, so we can safely use Object.equals
         Object prop1 = hg1.getProperty("MarkerSize");
         Object prop2 = hg2.getProperty("MarkerSize");
         ok = (prop1 == null && prop2 == null) || (prop1 != null && prop1.equals(prop2));
      }
      if(ok)
      {
         // value may be a double[] array, in which case we have to use Arrays.equals
         Object prop1 = hg1.getProperty("MarkerEdgeColor");
         Color c1 = MatlabUtilities.processColorSpec(prop1);
         Object prop2 = hg2.getProperty("MarkerEdgeColor");
         Color c2 = MatlabUtilities.processColorSpec(prop1);
         if(c1 != null && c2 != null) ok = c1.equals(c2);
         else ok = (prop1 == null && prop2 == null) || (prop1 != null && prop1.equals(prop2));
      }
      if(ok)
      {
         // value may be a double[] array, in which case we have to use Arrays.equals
         Object prop1 = hg1.getProperty("MarkerFaceColor");
         Color c1 = MatlabUtilities.processColorSpec(prop1);
         Object prop2 = hg2.getProperty("MarkerFaceColor");
         Color c2 = MatlabUtilities.processColorSpec(prop1);
         if(c1 != null && c2 != null) ok = c1.equals(c2);
         else ok = (prop1 == null && prop2 == null) || (prop1 != null && prop1.equals(prop2));
      }
      
      // finally, check the 'DisplayName' property
      if(ok)
      {
         Object prop1 = hg1.getProperty("DisplayName");
         Object prop2 = hg2.getProperty("DisplayName");
         ok = (prop1==null && prop2==null) || (prop1 != null && prop1.equals(prop2));
      }
      
      if(!ok) return(false);
      
      // if two 'line' objects have the same appearance, we coalesce them as long as they have more than two points, or
      // if they have markers drawn at the data point locations
      if(plotType.equals("line"))
      {
         double[] yData = getDoubleVectorFromProperty(hg1.getProperty("YData"));
         Object mark = hg1.getProperty("Marker");
         ok = (yData.length > 2) || (mark != null && !mark.equals("none"));
         if(ok)
         {
            yData = getDoubleVectorFromProperty(hg2.getProperty("YData"));
            mark = hg2.getProperty("Marker");
            ok = (yData.length > 2) || (mark != null && mark.equals("none"));
         }
         return(ok);
      }
      
      // if the two 'graph2d.lineseries' objects are in a polar graph, then they are coalesced only if they are both 
      // compass plot vectors or if they both contain just one point (which is checked below), and never if either is 
      // a rose plot. 
      if(hgAxes.isPolarAxes())
      {
         if(hg1.isRosePlot() || hg2.isRosePlot()) return(false);
         if(hg1.isCompassPlot() && hg2.isCompassPlot()) return(true);
      }
      
      // is YData the same length for both 'graph2d.lineseries' objects? NOTE that we assume here that YData will only 
      // be a vector, not a matrix.
      int len = 0;
      double[] yData1 = getDoubleVectorFromProperty(hg1.getProperty("YData"));
      double[] yData2 = getDoubleVectorFromProperty(hg2.getProperty("YData"));
      ok = (yData1 != null) && (yData2 != null) && (yData1.length == yData2.length);
      if(ok) len = yData1.length;

      // compare "XData/XDataMode" property of each line series. 'XData' must explicit for both or implicit for both.
      // If implicit, 'XDataMode' must be 'auto' for both; in this case, both N vector are assumed to be [1:N]. If 
      // 'XData' is explicit for both, then coalescing is possible only if the two N-vectors are equal and have the same
      // length N as 'YData" -- with ONE exception: if N==1, then the two x-coordinates do not have to match.
      if(ok)
      {
         double[] xData1 = getDoubleVectorFromProperty(hg1.getProperty("XData"));
         double[] xData2 = getDoubleVectorFromProperty(hg2.getProperty("XData"));
         Object prop1 = hg1.getProperty("XDataMode");
         Object prop2 = hg2.getProperty("XDataMode");
         boolean isAuto1 = prop1 == null || prop1.equals("auto");
         boolean isAuto2 = prop2 == null || prop2.equals("auto");
         
         if(xData1 != null && xData2 != null)
         {
            ok = (xData1.length == xData2.length) && (len == xData1.length);

            // in polar context, coalescing is allowed ONLY if both contain just one point. Otherwise, the X-vectors
            // must match (unless length == 1).
            if(ok)
            {
               if(hgAxes.isPolarAxes()) ok = (len == 1);
               else if(len > 1) ok = Arrays.equals(xData1, xData2);
            }
         }
         else
         {
            ok = (xData1 == null) && (xData2 == null) && (isAuto1 == isAuto2) && isAuto1;
            if(ok) ok = !hgAxes.isPolarAxes();
         }
      }
      
      return(ok);
   }

   /**
    * Helper method checks the properties of a "specgraph.scattergroup" object to determine whether or not it maps to
    * a simple <i>FypML</i> scatter plot in which all scatter plot symbols are the same size and color. This method
    * verifies that the HG object type is "specgraph.scattergroup", then checks the "SizeData", "MarkerFaceColor", and
    * "CData" properties to determine whether or not all scatter plot symbols will have the same size and appearance.
    * The "SizeData" must be null, empty, or a scalar; "MarkerFaceColor" must not == 'flat'; if it does, then 'CData'
    * must be null or must represent a single RGB color spec. Since <i>FypML</i> does not support a different stroke 
    * color for each scatter plot symbol, "MarkerEdgeColor"='flat' has no bearing on this check.
    * @param hgObj The scatter group object to test.
    * @return True if it represents a simple scatter plot (could be 2D or 3D), as described above.
    */
   static boolean isSimpleScatterPlot(HGObject hgObj)
   {
      boolean ok = (hgObj != null) && "specgraph.scattergroup".equals(hgObj.type);
      if(ok)
      {
         double[] szData = HGObject.getDoubleVectorFromProperty(hgObj.getProperty("SizeData"));
         ok = (szData == null) || (szData.length <= 1);
         if(ok)
         {
            ok = !"flat".equals(hgObj.getProperty("MarkerFaceColor"));
            if(!ok)
            {
               double[] cData = HGObject.getDoubleVectorFromProperty(hgObj.getProperty("CData"));
               ok = (cData == null) || (MatlabUtilities.processColorSpec(hgObj.getProperty("CData")) != null);
            }
         }
      }
      return(ok);
   }
   
   /**
    * Helper method that checks the class of the specified property and, if it represents a floating-point 1D array or
    * scalar, returns the property value as a <b>double[]</b> as follows:
    * <ul>
    * <li>Property class is double[] - Return property recast to double[].</li>
    * <li>Property class is a scalar Double - Return a 1-element double[] array containing the property value.</li>
    * </ul>
    * (Note: While Matlab may store floating-point arrays in single-precision, these arrays are converted to 
    * double-precision during import to simplify things.) This method is intended primarily for handling the 'XData' and
    * 'YData' properties of Matlab plot objects.
    * @param prop The property object.
    * @return The property value as a floating-point array, in the manner described above. If the property object's
    * class is not one of the above supported classes, method returns null.
    */
   static double[] getDoubleVectorFromProperty(Object prop)
   {
      double[] data = null;
      if(prop != null)
      {
         if(prop.getClass().equals(double[].class)) 
            data = (double[]) prop;
         else if(prop.getClass().equals(Double.class))
            data = new double[] {(Double) prop};
      }
      return(data);
   }
   
   /**
    * Helper method gets the specified string-valued property from a Handle Graphics object. The property value must be
    * a {@link String} or {@link Character}.
    * @param hgo The Java counterpart of a Matlab Handle Graphics object.
    * @param name The name of a string-valued property defined on that object.
    * @return The string value. Returns null if property not found or if it is not string-valued.
    */
   static String getStringValuedProperty(HGObject hgo, String name)
   {
      String s = null;
      if(hgo != null && name != null)
      {
         Object o = hgo.getProperty(name);
         if(o != null && (o.getClass().equals(String.class) || o.getClass().equals(Character.class)))
            s = o.toString();
      }
      return(s);
   }
   
   /** The Matlab Handle Graphics object type. */
   private final String type;
   /** The graphic object's handle. */
   private final double handle;
   /** The graphic object's explicit properties, keyed by property name. */
   private final HashMap<String, Object> properties;
   /** The graphic object's child objects. */
   private List<HGObject> children;
   /** For 'axes' and 'scribe.colorbar' only: Handle of child text object representing title. */
   private double hTitle = 0;
   /** For 'axes' and 'scribe.colorbar' only: Handle of child text object representing X-axis label. */
   private double hXLabel = 0;
   /** For 'axes' and 'scribe.colorbar' only: Handle of child text object representing Y-axis label. */
   private double hYLabel = 0;
   /** For 'axes' and 'scribe.colorbar' only: Handle of child text object representing Z-axis label. */
   private double hZLabel = 0;
   /** For 'axes' only: True if HG 'axes' object content is consistent with a Matlab polar plot. */
   private boolean isPolarAxes = false;
   /** 
    * For 'axes' only: True if HG 'axes' object definition is consistent with a Matlab 3D plot. In reality, Matlab
    * 'axes' are always 3D, but XY plots are achieved by setting the "camera view" so that the view line runs parallel
    * to the Z-axis.
    */
   private boolean is3D = false;
   /** For a HG 'axes' configured as polar plot: number of tick marks along radial axis. */
   private int nRadialTicks = 0;
   /** For a HG 'axes' configured as polar plot: The maximum radial value (min is always 0). */
   private double maxRad = 0;
   /** 
    * For a HG 'axes' configured as polar plot: This is one of the 'line' objects that renders the polar grid. It is
    * is removed from the 'axes' object's child  list during post-processing, but we save the first one here so we can
    * get the grid line styling ('Color', 'LineStyle', and 'LineWidth').
    */
   private HGObject polarGridLine = null;
   /**
    * For a HG 'graph2d.lineseries' within a polar plot: True if it represents an angle histogram as rendered by 
    * Matlab's <i>rose()</i> function.
    */
   private boolean isRosePlot = false;
   /**
    * For a HG 'graph2d.lineseries' within a polar plot: True if it represents a vector emanating from origin and
    * terminating with an arrow head, as generated by Matlab's <i>compass()</i> function.
    */
   private boolean isCompassPlotVector = false;
   
   /** 
    * For 'axes' only: True if HG 'axes' object content is consistent with a pie chart as generated by Matlab's 
    * <i>pie()</i> function. 
    */
   private boolean isPieChart = false;
   
   /**
    * The outer radius of a pie chart. This will be set on a single 'patch' object in which information is coalesced 
    * about the Matlab pie chart detected during post-processing of the parent 'axes'. The other 'patch' objects used to
    * render the slices in the pie chart in Matlab are removed. Otherwise, always 0.
    */
   private double pieRadius = 0;
   
   /**
    * The relative radial offset of a displaced slice in a pie chart, as a percentage of the chart's radius. It is set
    * on a single 'patch' object in which information about the Matlab pie chart detected during post-processing of its
    * parent 'axes'. Otherwise, always 0.
    */
   private int pieRadOfsPct = 0;
   
   /** 
    * Bit flag vector, indicating which slices are displaced in the pie chart that was detected during post-processing 
    * of a Matlab 'axes' object. It is set on the first 'patch' child of that 'axes', and all other 'patch' children 
    * used to render the slices of the pie chart are removed. BitN = 1 indicates that slice N is displaced radially from
    * the pie chart's nominal origin. 
    */
   private int pieDisplacedBits = 0;
   
   /** 
    * Internal collection of coalesced data for select use cases:
    * <ul>
    * <li>If this HG object is a 'patch' containing coalesced information for a Matlab pie chart, then this list 
    * contains one vector holding the angular extents of the pie slices comprising the chart, in order CCW.</li>
    * <li>If this HG object is a 'graph2d.lineseries' or 'specgraph.barseries', this list contains the data from this 
    * line-series or bar-series plot object, plus additional matching line-series or bar-series that have been coalesced 
    * with it. Each element in the list is the 'YData' N-vector (N &ge; 1; N &gt; 1 for a line-series) from an 
    * individual plot object. When this HG plot object is ultimately converted to a <i>FypML</i> trace or bar plot node,
    * the individual vectors are combined to form the raw data array for the node's collection-type data source (either
    * a MSET or MSERIES). 
    * <li>If this is a 'specgraph.scattergroup' object, then this list contains the 'XData' and 'YData' vectors from 
    * this scatter plot object and every scatter plot object in the same graph that have been coalesced with it. If the
    * containing graph is 3D, then the 'ZData' vectors are also included. Note that scatter group objects are coalesced
    * only if they are simple scatter plots in which the size and appearance of the scatter plot symbols are all the
    * same. When this HG plot object is ultimately converted to a <i>FypML</i> scatter plot node, the individual 
    * vectors are combined to form the raw data array for the node's PTSET or XYZSET data source.</li>
    * </ul>
    */
   private List<double[]> multiSetData = null;
   
   /**
    * Internal collection of coalesced data. Three use cases:
    * <ul>
    * <li>If a collection of 'graph2d.lineseries' plots have the same appearance and each contain a single point 
    * ('XData' and 'YData' are scalars), then those plot objects are coalesced into one object, effectively a scatter
    * plot. The individual scatter points are stored in this list. When the coalesced line-series object is converted
    * to a FypML trace node, the list of points is used to populate the PTSET data set rendered by that node.</li>
    * <li>If a collection of 'line' objects have the same appearance and contain 3 or more points (or 2+ points if a
    * marker symbol is defined), those objects are coalesced into one 'line' object. The data points defining each 
    * individual line are accumulated in this list, with an undefined point (NaN, NaN) separating each line. When the 
    * coalesced line object is converted to a FypML trace node, the list of points is used to populate the PTSET data 
    * set rendered by that node. The intervening ill-defined points ensure that the component lines are not connected
    * to each other.</li>
    * <li>In a polar context only, if a collections 'graph2d.lineseries' objects represent the individual vectors of a
    * compass plot, then they are coalesced into a single object and the vector endpoints are stored in this list (all
    * vectors originate at the origin in the compass plot). The coalesced line-series object is converted to a FypML 
    * trace node in histogram display mode with zero bar width and baseline, and this list of points is used to 
    * popluate the PTSET data rendered by that node. The points here are in Cartesian coordinates; they are converted
    * to polar coordinates when the PTSET data is prepared.</li>
    * </ul>
    */
   private List<Point2D> scatterPoints = null;
   
   /** 
    * For a coalesced 'specgraph.barseries' or 'specgraph.areaseries' object only, or the 'patch' object containing
    * coalesced information for a Matlab pie chart: The associated legend labels, one per  data group. These may or may 
    * not be accurate labels, and some could be null. If all are null, then the "show in legend" flag in the appropriate
    * <i>FypML</i> presentation node will be set to false. Otherwise, any null labels are replaced with "group N", where
    * N is the group's index position in the "grouped-data" plot.
    */
   private List<String> dataGrpLabels = null;
   /** 
    * For a coalesced 'specgraph.barseries' or 'specgraph.areaseries' object only, or the 'patch' object containing 
    * coalesced information for a Matlab pie chart: The associated fill colors, one per data group. The color may be 
    * either a color index (Integer object) or an RGBA color (Color object). A color index will be positive if 
    * it's a direct color map index, negative if it's a scaled index; no color index should be 0. When the coalesced 
    * plot object is converted to a <i>FypML</i> bar, area, or pie chart node, any color indices will be converted to 
    * RGB colors through the figure's color map. Any null entry is mapped to opaque black.
    */
   private List<Object> dataGrpColors = null;
   
   /** 
    * True if this Handle Graphics object is a 'surface' that renders a 3D bar plot vis-a-vis the bar3() function. 
    * This object will be converted to a FypML 3D scatter plot configured in a bar plot-like display mode.
    */
   private boolean isBar3D = false;
   /** The base plane Z-coordinate for a bar3() plot rendered as a HG 'surface' object. */
   private double bar3DBaseZ = 0;
   /** The bar size (in X and Y) for a bar3() plot rendered as a HG 'surface' object. */
   private double bar3DSize = 0;
   /** 
    * The set of 3D data points {X,Y,Z} for a bar3() plot rendered as a HG 'surface' object. It will contain data
    * points from any other 'surface' objects coalesced with this one. Not applicable if {@link #isBar3D} = false.
    */
   private List<Point3D> bar3DPoints = null;
}
