package com.srscicomp.fc.matlab;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Stack;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.util.NeverOccursException;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;
import com.srscicomp.fc.fig.AreaChartNode;
import com.srscicomp.fc.fig.Axis3DNode;
import com.srscicomp.fc.fig.AxisNode;
import com.srscicomp.fc.fig.BarPlotNode;
import com.srscicomp.fc.fig.ColorBarNode;
import com.srscicomp.fc.fig.ColorMap;
import com.srscicomp.fc.fig.ColorLUT;
import com.srscicomp.fc.fig.ContourNode;
import com.srscicomp.fc.fig.ErrorBarNode;
import com.srscicomp.fc.fig.FGNGraph;
import com.srscicomp.fc.fig.FGNPlottable;
import com.srscicomp.fc.fig.FGNPreferences;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.FigureNode;
import com.srscicomp.fc.fig.Graph3DNode;
import com.srscicomp.fc.fig.GraphNode;
import com.srscicomp.fc.fig.GridLineNode;
import com.srscicomp.fc.fig.LabelNode;
import com.srscicomp.fc.fig.LegendNode;
import com.srscicomp.fc.fig.LineNode;
import com.srscicomp.fc.fig.LogTickPattern;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.fig.PieChartNode;
import com.srscicomp.fc.fig.PolarAxisNode;
import com.srscicomp.fc.fig.PolarPlotNode;
import com.srscicomp.fc.fig.RasterNode;
import com.srscicomp.fc.fig.Scatter3DNode;
import com.srscicomp.fc.fig.ScatterPlotNode;
import com.srscicomp.fc.fig.StrokePattern;
import com.srscicomp.fc.fig.SurfaceNode;
import com.srscicomp.fc.fig.SymbolNode;
import com.srscicomp.fc.fig.TickSetNode;
import com.srscicomp.fc.fig.Ticks3DNode;
import com.srscicomp.fc.fig.TraceNode;
import com.srscicomp.fc.fig.TickSetNode.LabelFormat;
import com.srscicomp.fc.fig.TickSetNode.Orientation;
import com.srscicomp.fc.fig.TraceNode.DisplayMode;


/**
 * <b>MatlabUtilities</b> is a collection of static methods that implement the conversion of a Matlab figure to its
 * FypML counterpoart. It is used both in Matlab -- particularly by the M-function MATFIG2FYP.M -- and in Figure 
 * Composer itself, to open Matlab .FIG files and convert them to a FypML graphic directly in FC.
 * 
 * <p>The most important public method, {@link #matFigToFyp}, converts a Matlab figure into a FypML figure model -- to
 * the extent possible. The Matlab figure is represented by a tree of {@link HGObject}s. There are two avenues by which 
 * this Java representation of a Matlab figure is prepared:
 * <ul>
 * <li>The Matlab M-function MATFIG2FYP.M, which processes an open figure in the Matlab environment and relies on 
 * {@link #matFigToFyp} to generate the "equivalent" FypML figure.</li>
 * <li>Figure Composer calls the method {@link MatlabFigureImporter#importMatlabFigureFromFile} to open a Matlab FIG
 * file and parse its content to rebuild the figure's Handle Graphics tree, which is passed to {@link #matFigToFyp} to
 * complete the import.</li>
 * For additional details on the current capabilities and limitations of the Matlab-to-FypML figure conversion process,
 * see the help documentation for MATFIG2FYP.M.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("DataFlowIssue")
public class MatlabUtilities
{
   /**
    * Replace the note/description for a <i>FypML</i> figure (the "note" property of the root figure node).
    * 
    * <p>This method helps implement the Matlab utility <b>put2fyp(F,'note',S)</b>, where F is the file path for the
    * figure and S is the replacement text for the figure's note. S may be a Java string or string array. When a string 
    * array, each element is interpreted as a separate line of text; the strings are concatenated with intervening 
    * line-feed characters.</p>
    * 
    * @param fgm The figure to be updated.
    * @param oText The replacement note. Must be a single character, string, or string array. Null is treated as an
    * empty string (b/c Matlab maps an empty array or an empty Matlab string to a null Java object).
    * @return True if figure note was successfully updated; else false (invalid figure or text object).
    */
   public static boolean replaceFigureNote(FGraphicModel fgm, Object oText)
   {
      if(fgm == null) return(false);
      
      String s;
      if(oText == null)
         s = "";
      else if(oText.getClass().equals(Character.class) || oText.getClass().equals(String.class) ) 
         s = processSpecialCharacters(oText.toString());
      else if(oText.getClass().equals(String[].class))
      {
         String[] lines = (String[]) oText;
         StringBuilder sb = new StringBuilder();
         for(int i=0; i<lines.length-1; i++) sb.append(processSpecialCharacters(lines[i])).append("\n");
         sb.append(processSpecialCharacters(lines[lines.length-1]));
         
         s = sb.toString();
      }
      else
         return(false);
      
      return(fgm.replaceFigureNote(s));
   }
      
   /**
    * Replace the title of a graphic node within a <i>FypML</i> figure. 
    * 
    * <p>This method helps implement the Matlab utility <b>put2fyp(F,OP,ID,S)</b>, where F is the file path for the
    * figure, OP='text' or 'title', ID is the identifier assigned to the target node within the figure, and S is the
    * replacement title. S may be a Java string or string array. When a string array, each element is interpreted as
    * a separate line of text; the strings are concatenated with intervening line-feed characters.</p>
    * 
    * <p>Note that only the <i>text box</i> node supports a multi-line title. For any other node type, the line-feed
    * characters are stripped out, resulting in one long single-line string.</p>
    * 
    * @param fgm The figure to be updated.
    * @param id Identifier assigned to target graphic node.
    * @param oText The replacement title. Must be a single character, string, or string array. Null is treated as an
    * empty string (b/c Matlab maps an empty array or an empty Matlab string to a null Java object).
    * @return True if title of target node was successfully replaced; else false (invalid text object; target label or
    * text box not found in figure).
    */
   public static boolean replaceTitle(FGraphicModel fgm, String id, Object oText)
   {
      if(fgm == null || id == null || id.isEmpty()) return(false);
      
      String s = null;
      if(oText == null)
         s = "";
      else if(oText.getClass().equals(Character.class) || oText.getClass().equals(String.class) ) 
         s = processSpecialCharacters(oText.toString());
      else if(oText.getClass().equals(String[].class))
      {
         String[] lines = (String[]) oText;
         StringBuilder sb = new StringBuilder();
         for(int i=0; i<lines.length-1; i++) sb.append(processSpecialCharacters(lines[i])).append("\n");
         sb.append(processSpecialCharacters(lines[lines.length-1]));
         
         s = sb.toString();
      }
      
      return(fgm.replaceObjectTitleInFigure(id, s));
   }
   
   /**
    * Replace the legend label associated with a particular data group in the identified data presentation node within a
    * figure. The presentation node must be a bar plot, area chart, or pie chart -- all of which support data groups
    * and include a separate automated legend entry for each such group.
    * 
    * <p>This method helps implement the Matlab utility <b>put2fyp(F,OP,ID,POS,S)</b>, where F is the file path for the
    * <i>FypML</i> figure, OP="legend", ID is the identifier assigned to the data source for the target bar, area or pie
    * chart within the figure, POS is the index position of the target data group, and S is the replacement label.</p>
    * 
    * @param fgm The figure to be updated.
    * @param id Identifier assigned to data set of the target data presentation node, which must be a bar, area, or pie
    * chart.
    * @param grpIdx Index position of the target data group.
    * @param oLabel The replacement legend label for the specified data group. Must be a single character or string. 
    * Null is treated as an empty string (b/c Matlab maps an empty array or an empty Matlab string to a null Java 
    * object).
    * @return True if the update was successful; else false (invalid replacement label; target presentation node not 
    * found; or data group index position invalid).
    */
   public static boolean replaceDataGroupLegendLabel(FGraphicModel fgm, String id, int grpIdx, Object oLabel)
   {
      if(fgm == null || id == null || id.isEmpty()) return(false);
      
      
      if(oLabel == null || oLabel.getClass().equals(Character.class) || oLabel.getClass().equals(String.class) ) 
      {
         String s = (oLabel == null) ? "" : processSpecialCharacters(oLabel.toString());
         return(fgm.replaceDataGroupLegendLabelInFigure(id, grpIdx, s));
      }
      else
         return(false);
   }
   
   /**
    * Locate a particular 'axes' or 'PolarAxes' object within a Matlab Handle Graphics figure and convert it to its 
    * <i>FypML</i> counterpart: <i>graph</i> (2D Cartesian plot), <i>pgraph</i> (2D polar plot), or <i>graph3d</i> (3D
    * plot).
    * 
    * <p>This method helps implement the 'graph' and 'addgraph' operations for the Matlab utility <b>put2fyp()</b>. This
    * method performs the task of converting the relevant Matlab 'axes' or 'PolarAxes' object to a <i>FypML</i> 2D or 3D
    * graph node; <b>put2fyp()</b> takes care of the rest.</p>
    * 
    * @param hgFig A Java representation of a Matlab Handle Graphics "figure" object, as prepared by <b>put2fyp()</b>.
    * @param axesH Handle of the 'axes' or 'PolarAxes' object that is to be converted to its <i>FypML</i> counterpart.
    * @return The FypML graph prepared. It will be an instance of {@link GraphNode}, {@link PolarPlotNode}, or {@link 
    * Graph3DNode}, or null if operation failed (did not find specified Matlab object, or the conversion failed).
    */
   public static FGraphicNode extractGraphFromMatFig(HGObject hgFig, double axesH)
   {
      if(hgFig == null) throw new NullPointerException("Matlab figure structure was null (programming error).");
      
      // HACK to load user's preferred defaults for selected FypML node properties without going through DNWorkspace
      loadFGNPreferences();
      
      // the code below essentially duplicates the function convertFigure(), except that it skips over 'axes' or 
      // 'PolarAxes' children until it finds the one identified by the handle specified. No need to convert any other 
      // objects.
      FGraphicModel srcFig = new FGraphicModel();
      FigureNode fig = (FigureNode) srcFig.getRoot();
      try 
      { 
         Object nameProp = hgFig.getProperty("Name");
         if(nameProp != null && nameProp.getClass().equals(String.class))
         {
            String name = ((String) nameProp).trim();
            if(!name.isEmpty()) fig.setTitle(processSpecialCharacters(name));
         }
         
         Object posProp = hgFig.getProperty("PaperPosition");
         if(posProp == null || !posProp.getClass().equals(double[].class))
         {
            fig.setX(new Measure(0.25, Measure.Unit.IN));
            fig.setY(new Measure(2.5, Measure.Unit.IN));
            fig.setWidth(new Measure(8, Measure.Unit.IN));
            fig.setHeight(new Measure(6, Measure.Unit.IN));
         }
         else
         {
            double[] pos = (double[]) posProp;
            Object unitsProp = hgFig.getProperty("PaperUnits");
            if(unitsProp != null && unitsProp.getClass().equals(String.class))
            {
               String units = (String) unitsProp;
               switch(units)
               {
               case "normalized":
                  pos[0] = Utilities.limitFracDigits(pos[0] * 8.5, 3);
                  pos[1] = Utilities.limitFracDigits(pos[1] * 11, 3);
                  pos[2] = Utilities.limitFracDigits(pos[2] * 8.5, 3);
                  pos[3] = Utilities.limitFracDigits(pos[3] * 11, 3);
                  break;
               case "centimeters":
                  for(int i = 0; i < pos.length; i++)
                     pos[i] = Utilities.limitFracDigits(pos[i] * Measure.MM2IN * 10.0, 3);
                  break;
               case "points":
                  for(int i = 0; i < pos.length; i++) pos[i] = Utilities.limitFracDigits(pos[i] * Measure.PT2IN, 3);
                  break;
               }
            }
            
            fig.setX(new Measure(pos[0], Measure.Unit.IN));
            fig.setY(new Measure(pos[1], Measure.Unit.IN));
            fig.setWidth(new Measure(pos[2], Measure.Unit.IN));
            fig.setHeight(new Measure(pos[3], Measure.Unit.IN));
         }
         
         double[][] matCMap;
         ColorMap cMap = ColorMap.BuiltIn.gray.cm;
         Object cmProp = hgFig.getProperty("Colormap");
         if(cmProp != null && cmProp.getClass().equals(double[][].class))
         {
            matCMap = (double[][]) cmProp;
            cMap = ColorLUT.selectBestMatchFor(matCMap).getColorMap();
         }
         else
         {
            if(cmProp == null) cMap = ColorMap.BuiltIn.tropic.cm;
            
            ColorLUT lut = new ColorLUT(cMap, false);
            matCMap = new double[ColorMap.LUTSIZE][];
            for(int i=0; i<matCMap.length; i++)
            {
               matCMap[i] = new double[3];
               int rgb = lut.getRGB(i);
               matCMap[i][0] = ((double) (0x0FF & (rgb >> 16))) / 255.0;
               matCMap[i][1] = ((double) (0x0FF & (rgb >> 8))) / 255.0;
               matCMap[i][2] = ((double) (0x0FF & rgb)) / 255.0;
            }
         }
         
         // find the HG "axes" or "PolarAxes" child we're interested in and convert it to a 2D or 3D graph node
         for(int i=0; i<hgFig.getChildCount(); i++)
         {
            HGObject hgChild = hgFig.getChildAt(i);
            if(axesH == hgChild.getHandle())
            {
               if(hgChild.getType().equals("axes"))
               {
                  if(hgChild.isPolarAxes())
                     addPolarPlotToFigure(fig, hgFig, hgChild, cMap, matCMap);
                  else if(hgChild.is3DAxes())
                     add3DGraphToFigure(fig, hgFig, hgChild, cMap, matCMap);
                  else
                     addGraphToFigure(fig, hgFig, hgChild, cMap, matCMap);
               }
               else if(hgChild.getType().equals("matlab.graphics.axis.PolarAxes"))
                  addPolarAxesToFigure_r2016(fig, hgFig, hgChild, cMap, matCMap);
               else
                  return(null);
               
               int nKids = fig.getChildCount();
               FGraphicNode n = (nKids == 0) ? null : fig.getChildAt(nKids-1);
               if(n instanceof GraphNode || n instanceof PolarPlotNode || n instanceof Graph3DNode)
                  return(n);
               else
                  return(null);
            }
         }
      }
      catch(Throwable ignored) {}

      return(null);
   }
   
   /**
    * Convert a Matlab Handle Graphics figure to a <i>DataNav</i> FypML figure that preserves as much of the general
    * appearance -- and all of the raw data in plot objects -- of the original Matlab figure as possible. Only those
    * features in the Matlab figure that are reproducible in a FypML figure will be converted.
    * 
    * <p>Matlab handles the positioning of axis labels differently <i>DataNav</i> FypML. FypML specifies an "axis label
    * offset" which positions the label WRT the corresponding axis line; when the Matlab figure is converted to FypML,
    * all axis label offsets are set to a value specified in the user's workspace preferences. If this method is called
    * in the Matlab environment, those preferences are not available and the default label offset of 0.5in is used. 
    * This may be too large in some situations, leading to axis labels being cutoff because they're outside the 
    * enclosing figure's bounds. To address this issue, this method includes a label offset argument which will override
    * the default value.</p>
    * 
    * @param hgFig A Java representation of a Matlab Handle Graphics "figure" object, as prepared by the custom Matlab
    * utility function MATFIG2FYP. MATFIG2FYP calls this Java function to complete the conversion.
    * @param axisLabelOfs Axis label offset applied to all graph axes in the figure, in inches. If this value lies in
    * [0.1 .. 1], it is used as the axis label offset rather than what is specified in the user's workspace preferences.
    * If it lies outside that range, it is ignored.
    * @param buf If non-null, a text dump of the Matlab figure hierarchy is optionally appended to this string buffer.
    * Also, if the conversion fails, a brief error description is appended to the buffer.
    * @param dumpTree If true and a string buffer is provided, a text dump of the Matlab figure hierarchy is appended
    * to this string buffer prior to starting the conversion.
    * @return The FypML figure generated, or null if conversion failed.
    */
   public static FGraphicModel matFigToFyp(HGObject hgFig, double axisLabelOfs, StringBuffer buf, boolean dumpTree)
   {
      if(hgFig == null) throw new NullPointerException("Matlab figure structure was null (programming error).");
      
      // HACK to load user's preferred defaults for selected FypML node properties without going through DNWorkspace
      loadFGNPreferences();
      
      // override default axis label offset if relevant argument is in allowed range
      Measure oldLabelOfs = null;
      if(axisLabelOfs >= 0.1 && axisLabelOfs <= 1)
      {
         oldLabelOfs = FGNPreferences.getInstance().getPreferredAxisLabelOffset();
         FGNPreferences.getInstance().setPreferredAxisLabelOffset(new Measure(axisLabelOfs, Measure.Unit.IN));
      }
      
      FGraphicModel fgm = new FGraphicModel();
      FigureNode fig = (FigureNode) fgm.getRoot();
      try 
      { 

         if(dumpTree && buf != null) dumpMatlabFigureTree(hgFig, buf);
         convertFigure(hgFig, fig); 
      }
      catch(Throwable t) 
      {
         fgm = null;
         if(buf != null)
         {
            // put the short error message at beginning of string, followed by the tree structure dump (if enabled),
            // followed by the exception stack trace
            buf.insert(0, String.format("%n%s%n", "Conversion failed: " + t.getMessage()));
            buf.append(String.format("%nException class: %s%n", t.getClass().getCanonicalName()));
            StackTraceElement[] trace = t.getStackTrace();
            buf.append(String.format("STACK TRACE%n"));
            for(StackTraceElement stackTraceElement : trace)
            {
               buf.append(String.format("%s%n", stackTraceElement.toString()));
            }
         }
      }
      
      // restore original value of preferred axis label offset, if it was changed
      if(oldLabelOfs != null) FGNPreferences.getInstance().setPreferredAxisLabelOffset(oldLabelOfs);
      
      return(fgm);
   }
   
   /**
    * Helper method for {@link #matFigToFyp} that reads in user's preferred default values for various <i>FypML</i>
    * graphic node properties directly from the settings file in the user's <i>DataNav</i> workspace WITHOUT going
    * through the workspace object.
    * 
    * <p>This is a HACK to avoid dependency on the workspace object, requiring the <i>com.srscicomp.fc.uibase</i>
    * package. Note that the method assumes the settings file is at $HOME/datanavx/settings.txt, where $HOME is the 
    * user's home  directory. If the method is unable to load the settings file for any reason, it fails silently, and 
    * all defaults are set to application-defined values.</p>
    */
   private static void loadFGNPreferences()
   {
      // prepare settings file path. Note we may not be able to access property that specifies user's home directory.
      File home;
      try
      {
         home = new File(System.getProperty("user.home"), ".datanavx");
         if(!home.isDirectory()) 
         {
            if(!home.mkdir()) home = null;
         }
      }
      catch(SecurityException se) { home = null; }
      if(home == null) return;
      
      File f = new File(home, "settings.txt");
      if(!f.isFile()) return;
      
      // load user preferences from settings file
      Properties settings = new Properties();
      FileInputStream in = null;
      boolean ok = false;
      try
      {
         in = new FileInputStream(f);
         settings.load(in);
         ok = true;
      } 
      catch(IOException ignored) {}
      finally { try{ if(in != null) in.close(); } catch(IOException ignored) {} }
      if(!ok) return;
      
      // finally, load the FypML graphic node defaults from settings object
      FGNPreferences.getInstance().load(settings);
   }

   private static void dumpMatlabFigureTree(HGObject hgFig, StringBuffer buf)
   {
      Stack<HGObject> hgStack = new Stack<>();
      hgStack.push(hgFig);
      StringBuilder indent = new StringBuilder();
      while(!hgStack.isEmpty())
      {
         HGObject hgObj = hgStack.pop();
         if(hgObj == null)
         {
            // marks the end of a child list
            indent = new StringBuilder(indent.substring(5));
            continue;
         }
         
         buf.append(indent).append(String.format("HGObject=%s; handle=%.4f%n", hgObj.getType(), hgObj.getHandle()));
         if(hgObj.getType().equals("axes") || hgObj.getType().equals("scribe.colorbar"))
         {
            buf.append(indent).append(String.format(" Title handle=%.4f%n", hgObj.getTitleHandle()));
            buf.append(indent).append(String.format(" X-Label handle=%.4f%n", hgObj.getXLabelHandle()));
            buf.append(indent).append(String.format(" Y-Label handle=%.4f%n", hgObj.getYLabelHandle()));
         }
         buf.append(indent).append(String.format(" PROPERTIES:%n"));
         List<String> props = hgObj.getExplicitProperties();
         for(String name : props)
         {
            Object val = hgObj.getProperty(name);
            if(val == null)
               buf.append(indent).append(String.format("  %s=null%n", name));
            else
            {
               if(val.getClass().equals(double[].class))
               {
                  double[] dblAr = (double[]) val;
                  buf.append(indent).append(String.format("  %s=[", name));
                  int idx = 0;
                  while(idx < dblAr.length && idx < 4)
                  {
                     buf.append(String.format("%.04f ", dblAr[idx]));
                     ++idx;
                  }
                  buf.append(String.format("%s%n", (idx<dblAr.length) ? "... ]" : "]"));
               }
               else
                  buf.append(indent).append(
                        String.format("  %s=(%s) %s%n", name, val.getClass().getCanonicalName(), val));
            }
         }
         
         if(hgObj.getChildCount() > 0)
         {
            indent.append("     ");
            // push children onto stack in reverse order so they come out in correct order and use a null to mark end
            hgStack.push(null);
            for(int i=hgObj.getChildCount()-1; i>= 0; i--) hgStack.push(hgObj.getChildAt(i));
         }
      }
   }
   
   /**
    * To the extent possible, modify the FypML figure to resemble the Matlab handle graphics figure specified. 
    * 
    * <p>This method uses the Matlab figure's "Name" property as the FypML figure title, and parses the "PaperUnits",
    * "PaperSize", and "PaperPosition" properties to determine the FypML figure's size and location on the printed
    * page. It also adds a {@link GraphNode GraphNode} to the FypML figure for each "axes" child
    * found in the Matlab figure.</p>
    * 
    * <p><i>Matlab 2014b considerations</i>. This Matlab release introduced major graphics changes. Among them was a new
    * color map, "parula", that is now an implicit default. Thus, an HG figure object in a FIG file saved by Matlab
    * 2014b may no longer have the 'Colormap' property explicitly set; prior to that release, it always was set. To
    * handle this change, the figure color map is set to a similar <i>FypML</i> color map, {@link
    * ColorMap.BuiltIn#tropic}, if  the 'Colormap' property is implicit. In addition, Matlab 2014b lets you assign
    * different color maps to different axes within a figure (just like <i>FypML</i>). Unfortunately, there's no
    * documented way to ascertain when the color map for an 'axes' is different from that for its parent figure -- an
    * 'axes' does not have a 'Colormap' property. So, for now, we ASSUME all axes use the figure color map!</p>
    * 
    * <p><i>Matlab 2016b considerations</i>. A new HG object was introduced to represent a real polar plot, with type
    * name "matlab.graphics.axis.PolarAxes". Its properties are quite different from the standard "axes" object, and
    * its conversion is handled by {@link #addPolarAxesToFigure_r2016}.</p>
    * 
    * @param hgFig The handle graphics object representing a Matlab figure.
    * @param fig The root graphics node of the FypML figure.
    */
   private static void convertFigure(HGObject hgFig, FigureNode fig)
   {
      Object nameProp = hgFig.getProperty("Name");
      if(nameProp != null && nameProp.getClass().equals(String.class))
      {
         String name = ((String) nameProp).trim();
         if(!name.isEmpty()) fig.setTitle(processSpecialCharacters(name));
      }
      
      // if "PaperPosition" not specified, assume figure 8x6in and centered on 8.5x11in page
      Object posProp = hgFig.getProperty("PaperPosition");
      if(posProp == null || !posProp.getClass().equals(double[].class))
      {
         fig.setX(new Measure(0.25, Measure.Unit.IN));
         fig.setY(new Measure(2.5, Measure.Unit.IN));
         fig.setWidth(new Measure(8, Measure.Unit.IN));
         fig.setHeight(new Measure(6, Measure.Unit.IN));
      }
      else
      {
         // we expect "PaperPosition" to be a 4-element double array: [left, bottom, width, height]. Units depend on
         // the "PaperUnits" property (default = inches). If "PaperUnits" = "normalized", assume an 8.5x11-inch page in
         // the portrait orientation. Regardless "PaperUnits", we convert to "inches".
         double[] pos = (double[]) posProp;
         Object unitsProp = hgFig.getProperty("PaperUnits");
         if(unitsProp != null && unitsProp.getClass().equals(String.class))
         {
            String units = (String) unitsProp;
            switch(units)
            {
            case "normalized":
               pos[0] = Utilities.limitFracDigits(pos[0] * 8.5, 3);
               pos[1] = Utilities.limitFracDigits(pos[1] * 11, 3);
               pos[2] = Utilities.limitFracDigits(pos[2] * 8.5, 3);
               pos[3] = Utilities.limitFracDigits(pos[3] * 11, 3);
               break;
            case "centimeters":
               for(int i = 0; i < pos.length; i++) pos[i] = Utilities.limitFracDigits(pos[i] * Measure.MM2IN * 10.0, 3);
               break;
            case "points":
               for(int i = 0; i < pos.length; i++) pos[i] = Utilities.limitFracDigits(pos[i] * Measure.PT2IN, 3);
               break;
            }
         }
         
         fig.setX(new Measure(pos[0], Measure.Unit.IN));
         fig.setY(new Measure(pos[1], Measure.Unit.IN));
         fig.setWidth(new Measure(pos[2], Measure.Unit.IN));
         fig.setHeight(new Measure(pos[3], Measure.Unit.IN));
      }
      
      // analyze Matlab figure's color map and choose the best-match DataNav-supported colormap. As of Matlab 2014b,
      // the 'Colormap' property may be implicit, in which case it is set to TROPIC (Matlab's 'parula'). If it is not 
      // double-valued matrix, then it defaults to grayscale.
      double[][] matCMap;
      ColorMap cMap = ColorMap.BuiltIn.gray.cm;
      Object cmProp = hgFig.getProperty("Colormap");
      if(cmProp != null && cmProp.getClass().equals(double[][].class))
      {
         matCMap = (double[][]) cmProp;
         cMap = ColorLUT.selectBestMatchFor(matCMap).getColorMap();
      }
      else
      {
         if(cmProp == null) cMap = ColorMap.BuiltIn.tropic.cm;
         
         // if we didn't get a Colormap matrix, we still have to define one based on our selected color map in order to
         // process "indexed colors"...
         ColorLUT lut = new ColorLUT(cMap, false);
         matCMap = new double[ColorMap.LUTSIZE][];
         for(int i=0; i<matCMap.length; i++)
         {
            matCMap[i] = new double[3];
            int rgb = lut.getRGB(i);
            matCMap[i][0] = ((double) (0x0FF & (rgb >> 16))) / 255.0;
            matCMap[i][1] = ((double) (0x0FF & (rgb >> 8))) / 255.0;
            matCMap[i][2] = ((double) (0x0FF & rgb)) / 255.0;
         }
      }
      
      // convert each HG "axes" or "polaraxes" child to a FypML "graph" node. Polar and Cartesian plots are handled 
      // differently.
      // NOTE: The true polar plot object "matlab.graphics.axis.PolarAxes" was introduced in Matlab R2016a/b. It is 
      // very different from an "axes" object that has been configured to look like a polar plot!
      for(int i=0; i<hgFig.getChildCount(); i++)
      {
         HGObject hgChild = hgFig.getChildAt(i);
         if(hgChild.getType().equals("axes"))
         {
            if(hgChild.isPolarAxes())
               addPolarPlotToFigure(fig, hgFig, hgChild, cMap, matCMap);
            else if(hgChild.is3DAxes())
               add3DGraphToFigure(fig, hgFig, hgChild, cMap, matCMap);
            else
               addGraphToFigure(fig, hgFig, hgChild, cMap, matCMap);
         }
         else if(hgChild.getType().equals("matlab.graphics.axis.PolarAxes"))
            addPolarAxesToFigure_r2016(fig, hgFig, hgChild, cMap, matCMap);
      }
   }
   
   /**
    * Append a new Cartesian graph node to the FypML figure that renders the Matlab handle graphics "axes" object 
    * specified, to the extent possible, including all supported HG plot objects and HG text objects that are children 
    * of the "axes" object. If any converted plot object has (x,y) data outside the graph's data window, the graph's 
    * clip flag is set. In addition, if the figure includes a "scribe.legend" child attached to the "axes", turn on the 
    * graph node's legend and configure it accordingly; similarly, if there's an attached "scribe.colorbar", turn on
    * the graph's colorbar and configure it accordingly.
    * 
    * <p>Do NOT use this to translate a Matlab "axes" configured as a polar plot; use {@link #addPolarPlotToFigure}
    * instead.</p>
    * 
    * <p>[Added FC v5.1.2] 'Color' property is the background color of the graph's data window. If this is white and
    * the parent figure has no background, or if the parent figure has a solid background that matches this color, then
    * we leave the graph's background color as transparent (the default). Else, we use this color.</p>
    * 
    * @param fig The FypML figure node that will parent the new graph node.
    * @param hgFig The Matlab Handle Graphics object defining the Matlab figure.
    * @param hgAxes The Matlab Handle Graphics object defining the child "axes" to be converted into a FypML graph.
    * @param cm The FypML-supported colormap that best matches the Matlab figure's colormap. This will be assigned to
    * the color axis of the FypML graph node created. (A Matlab figure can only have one colormap, while FypML can 
    * assign a different colormap to each graph.)
    * @param matCM The Matlab figure's colormap. We need this to process indexed colors specified for a "patch" object
    * representing a histogram.
    */
   private static void addGraphToFigure(FigureNode fig, HGObject hgFig, HGObject hgAxes, ColorMap cm, double[][] matCM)
   {
      if(!fig.getGraphicModel().insertNode(fig, FGNodeType.GRAPH, -1))
         throw new NeverOccursException("Failed to insert new graph into FypML figure.");
      
      GraphNode graph = (GraphNode) fig.getChildAt(fig.getChildCount()-1);
      AxisNode xAxis = graph.getPrimaryAxis();
      AxisNode yAxis = graph.getSecondaryAxis();
      ColorBarNode colorBar = graph.getColorBar();
      GridLineNode xGrid = graph.getPrimaryGridLines();
      GridLineNode yGrid = graph.getSecondaryGridLines();
      
      // assign the figure's colormap to the graph's color axis
      colorBar.setColorMap(cm);
      
      // handle font-related properties
      setFontProperties(graph, hgAxes);

      // HG "LabelFontSizeMultiplier" is a scalar multiplier (default = 1.1) for the label size vs tick labels. We
      // adjust the X and Y axis font size accordingly
      double[] multiplier = HGObject.getDoubleVectorFromProperty(hgAxes.getProperty("LabelFontSizeMultiplier"));
      int labelFontSz = (int) (0.5 + graph.getFontSizeInPoints().doubleValue() * 
            ((multiplier!=null && multiplier.length == 1) ? multiplier[0] : 1.1));
      xAxis.setFontSizeInPoints(labelFontSz);
      yAxis.setFontSizeInPoints(labelFontSz);
      
      // in FypML, tick sets are children of the X and Y-axis, so they'll inherit the font size of the axis parent. But
      // in Matlab the tick labels are rendered with the font size assigned to the graph -- so we have to set the tick
      // set font size explicitly
      TickSetNode ticks = (TickSetNode) xAxis.getChildAt(0);
      ticks.setFontSizeInPoints(graph.getFontSizeInPoints());
      ticks = (TickSetNode) yAxis.getChildAt(0);
      ticks.setFontSizeInPoints(graph.getFontSizeInPoints());
      
      // HG "Position" and "Units" properties determine the location of the graph's data window WRT the parent figure.
      // Typically, "Units" defaults to "normalized", so we need the figure's dimensions in order to get the graph's
      // location and size in real units (% units not allowed in DataNav FypML).
      Object posProp = hgAxes.getProperty("Position");
      if(posProp == null || !posProp.getClass().equals(double[].class))
         throw new NeverOccursException("Handle Graphics 'axes' object missing 'Position' property");
      double[] pos = (double[]) posProp;
      if(pos.length < 4 || pos[2] <= 0 || pos[3] <= 0) 
         throw new NeverOccursException("Handle Graphics 'Position' is invalid for 'axes' object");
      
      double scaleFac = 0;  // for special case: normalized units
      Object unitsProp = hgAxes.getProperty("Units");
      if(unitsProp != null && unitsProp.getClass().equals(String.class))
      {
         String units = (String) unitsProp;
         switch(units)
         {
         case "normalized":
            scaleFac = 0;
            break;
         case "inches":
            scaleFac = 1.0;
            break;
         case "centimeters":
            scaleFac = Measure.MM2IN * 10.0;
            break;
         default:
            // "pixels" and "characters" are not supported and map arbitrarily to "points"
            scaleFac = Measure.PT2IN;
            break;
         }
      }
      if(scaleFac == 0)
      {
         // convert normalized values to inches WRT parent figure viewport
         pos[0] = fig.getWidthMI() * pos[0] / 1000.0;
         pos[1] = fig.getHeightMI() * pos[1] / 1000.0;
         pos[2] = fig.getWidthMI() * pos[2] / 1000.0;
         pos[3] = fig.getHeightMI() * pos[3] / 1000.0;
        
      }
      else for(int i=0; i<pos.length; i++) pos[i] *= scaleFac;
      
      // adjust graph dimensions for aspect ratio if Matlab axes is not in "stretch to fill" mode
      double[] aspect = getAxesAspectRatio(hgAxes);
      if(aspect != null)
      {
         if((aspect[0] / aspect[1]) >= (pos[2] / pos[3]))
         {
            double oldH = pos[3];
            pos[3] = pos[2] * aspect[1] / aspect[0];
            pos[1] += (oldH - pos[3]) / 2.0;
         }
         else
         {
            double oldW = pos[2];
            pos[2] = pos[3] * aspect[0] / aspect[1];
            pos[0] += (oldW - pos[2]) / 2.0;
         }
      }     

      graph.setX(new Measure(Utilities.limitFracDigits(pos[0], 3), Measure.Unit.IN));
      graph.setY(new Measure(Utilities.limitFracDigits(pos[1], 3), Measure.Unit.IN));
      graph.setWidth(new Measure(Utilities.limitFracDigits(pos[2], 3), Measure.Unit.IN));
      graph.setHeight(new Measure(Utilities.limitFracDigits(pos[3], 3), Measure.Unit.IN));
      
      // HG "Color" property is the axes background color, which maps to the graph's "boxColor" attribute (as of FC 
      // v5.1.2). However, if the parent figure has no background and the axes background color is opaque white, then
      // we set the FypML graph's background to transparent. Similarly, if the parent figure has a solid background
      // color that matches the "Color" property value, then we again set the graph's background to transparent. A
      // transparent "boxColor" is the default for a graph node.
      Color c = processColorSpec(hgAxes.getProperty("Color"));
      if(c == null) c = Color.WHITE;
      BkgFill figBkg = fig.getBackgroundFill();
      boolean assumeTransp = (figBkg.isTransparent() && Color.WHITE.equals(c)) ||
              (figBkg.getFillType() == BkgFill.Type.SOLID && figBkg.getColor1().equals(c));
      if(!assumeTransp) graph.setBoxColor(c);
      
      // HG "LineWidth" property maps to the graph's "strokeWidth". Units=point. Default is 0.5pt. 
      graph.setMeasuredStrokeWidth(processLineWidth(hgAxes));

      // HG "XAxisLocation" and "YAxisLocation" determine quadrant layout for graph node
      boolean xOnBottom = true;
      boolean yOnLeft = true;
      Object axisLocProp = hgAxes.getProperty("XAxisLocation");
      if(axisLocProp != null && axisLocProp.getClass().equals(String.class) && "top".equals(axisLocProp))
         xOnBottom = false;
      axisLocProp = hgAxes.getProperty("YAxisLocation");
      if(axisLocProp != null && axisLocProp.getClass().equals(String.class) && "right".equals(axisLocProp))
         yOnLeft = false;
      GraphNode.Layout layout = GraphNode.Layout.QUAD1;
      if(!xOnBottom) layout = yOnLeft ? GraphNode.Layout.QUAD4 : GraphNode.Layout.QUAD3;
      else if(!yOnLeft) layout = GraphNode.Layout.QUAD2;
      graph.setLayout(layout);
      
      // HG "XScale" and "YScale" determine coordinate system type. 
      Object scaleProp = hgAxes.getProperty("XScale");
      boolean xLinear = (scaleProp == null) || (!scaleProp.getClass().equals(String.class)) || 
            "linear".equals(scaleProp);
      scaleProp = hgAxes.getProperty("YScale");
      boolean yLinear = (scaleProp == null) || (!scaleProp.getClass().equals(String.class)) || 
            "linear".equals(scaleProp);
      GraphNode.CoordSys sys = GraphNode.CoordSys.CARTESIAN;
      if(!xLinear) sys = yLinear ? GraphNode.CoordSys.SEMILOGX : GraphNode.CoordSys.LOGLOG;
      else if(!yLinear) sys = GraphNode.CoordSys.SEMILOGY;
      graph.setCoordSys(sys);
      
      // HG "Visible" property: If off, both axes are hidden.
      Object visProp = hgAxes.getProperty("Visible");
      boolean axesHidden = (visProp != null) && visProp.getClass().equals(String.class) && 
            "off".equals(visProp);
      if(axesHidden)
      {
         xAxis.setHide(true);
         yAxis.setHide(true);
      }
      
      // HG "GridLineStyle, XGrid, YGrid" properties affect the graph's grid-line nodes
      Object gridProp = hgAxes.getProperty("XGrid");
      if(gridProp != null && gridProp.getClass().equals(String.class) && "on".equals(gridProp))
         xGrid.setHide(false);
      gridProp = hgAxes.getProperty("YGrid");
      if(gridProp != null && gridProp.getClass().equals(String.class) && "on".equals(gridProp))
         yGrid.setHide(false);
      StrokePattern patn = processLineStyle(hgAxes);
      if(patn == null)
      {
         // DataNav FypML uses 0 stroke width to achieve the "none" line style
         xGrid.setMeasuredStrokeWidth(new Measure(0, Measure.Unit.IN));
         yGrid.setMeasuredStrokeWidth(new Measure(0, Measure.Unit.IN));
         patn = StrokePattern.SOLID;
      }
      xGrid.setStrokePattern(patn);
      yGrid.setStrokePattern(patn);
      
      // HG "GridColor" property specifies the stroke color for the grid lines. Its default is a light gray.
      c = processColorSpec(hgAxes.getProperty("GridColor"));
      if(c == null) c = new Color(0.15f, 0.15f, 0.15f);
      xGrid.setStrokeColor(c);
      yGrid.setStrokeColor(c);
      
      // HG "XColor, YColor" properties set stroke and fill color for X-axis and Y-axis, respectively
      c = processColorSpec(hgAxes.getProperty("XColor"));
      if(c != null)
      {
         xAxis.setStrokeColor(c);
         xAxis.setFillColor(c);
      }
      c = processColorSpec(hgAxes.getProperty("YColor"));
      if(c != null)
      {
         yAxis.setStrokeColor(c);
         yAxis.setFillColor(c);
      }
      
      // HG "XLim, XDir, YLim, YDir" properties specify X-axis and Y-axis ranges, while "CLim" property defines the
      // color axis range. If missing, corresponding axis is auto-ranged. Note DataNav's Z-axis is really the equivalent
      // of Matlab's color bar, which is defined by a "scribe.colorbar" child of the Matlab HG figure and associated
      // by handle with an "axes". Other aspects of the Z-axis appearance in the DataNav graph are controlled by the
      // "scribe.colorbar" object. We also use "CLim" to do scaled indexed color mapping for a "patch" object....
      boolean autoX = true;
      boolean autoY = true;
      boolean autoZ = true;
      Object rngProp = hgAxes.getProperty("XLim");
      if(rngProp != null && rngProp.getClass().equals(double[].class))
      {
         double[] range = (double[]) rngProp;
         if(range.length == 2 && Utilities.isWellDefined(range))
         {
            autoX = false;
            Object dir = hgAxes.getProperty("XDir");
            boolean reverse = (dir != null) && dir.getClass().equals(String.class) && "reverse".equals(dir);
            xAxis.setStart(Utilities.limitSignificantDigits(reverse ? range[1] : range[0], 6));
            xAxis.setEnd(Utilities.limitSignificantDigits(reverse ? range[0] : range[1], 6));
         }
      }
      rngProp = hgAxes.getProperty("YLim");
      if(rngProp != null && rngProp.getClass().equals(double[].class))
      {
         double[] range = (double[]) rngProp;
         if(range.length == 2 && Utilities.isWellDefined(range))
         {
            autoY = false;
            Object dir = hgAxes.getProperty("YDir");
            boolean reverse = (dir != null) && dir.getClass().equals(String.class) && "reverse".equals(dir);
            yAxis.setStart(Utilities.limitSignificantDigits(reverse ? range[1] : range[0], 6));
            yAxis.setEnd(Utilities.limitSignificantDigits(reverse ? range[0] : range[1], 6));
         }
      }
      rngProp = hgAxes.getProperty("CLim");
      double[] cLim = null;
      if(rngProp != null && rngProp.getClass().equals(double[].class))
      {
         cLim = (double[]) rngProp;
         if(cLim.length == 2 && Utilities.isWellDefined(cLim))
         {
            autoZ = false;
            colorBar.setStart(Utilities.limitSignificantDigits(cLim[0], 6));
            colorBar.setEnd(Utilities.limitSignificantDigits(cLim[1], 6));
         }
         else cLim = null;
      }

      graph.setAutorangeAxes(autoX, autoY, autoZ);
      
      // HG "TickDir" determines orientation of tick marks in the default TickSetNode for X- and Y-axis. The default
      // (implicit) orientation is inward.
      Object tdProp = hgAxes.getProperty("TickDir");
      Orientation ori = Orientation.IN;
      if(tdProp != null && tdProp.getClass().equals(String.class))
         ori = tdProp.equals("out") ? Orientation.OUT : Orientation.IN;
      ticks = (TickSetNode) xAxis.getChildAt(0);
      ticks.setTickOrientation(ori);
      ticks = (TickSetNode) yAxis.getChildAt(0);
      ticks.setTickOrientation(ori);
      
      // HG "TickLength", if present, specifies tick length as a fraction of the longest axis. Since we ignore Z axis,
      // we can use the longer of graph's data window width or height instead. Note that property is a two-element 
      // double array [2DLength 3DLength] with implicit default [0.01 0.025]; we ignore the 3DLength.
      double tickLenFrac = 0.01;
      Object tlProp = hgAxes.getProperty("TickLength");
      if(tlProp != null && tlProp.getClass().equals(double[].class))
      {
         double[] tlen = (double[]) tlProp;
         if(tlen.length == 2 && tlen[0] >= 0 && tlen[0] < 1.0) tickLenFrac = tlen[0];
         {
         }
      }
      double tickLen = tickLenFrac * Math.max(graph.getWidth().toMilliInches(), graph.getHeight().toMilliInches());
      tickLen /= 1000.0;
      ticks = (TickSetNode) xAxis.getChildAt(0);
      ticks.setTickLength(new Measure(Utilities.limitFracDigits(tickLen, 2), Measure.Unit.IN));
      ticks = (TickSetNode) yAxis.getChildAt(0);
      ticks.setTickLength(new Measure(Utilities.limitFracDigits(tickLen, 2), Measure.Unit.IN));

      // there is no Matlab property specifying the FypML tick label gap property. As a rule of thumb (based on looking
      // at typical Matlab figures), we set it to 0.5*F for the X-axis and 0.33*F for the Y-axis, where F is the graph's
      // font size in points
      double fontSzPts = graph.getFontSizeInPoints().doubleValue();
      ticks = (TickSetNode) xAxis.getChildAt(0);
      ticks.setTickGap(new Measure((int) (fontSzPts*0.5 + 0.5), Measure.Unit.PT));
      ticks = (TickSetNode) yAxis.getChildAt(0);
      ticks.setTickGap(new Measure((int) (fontSzPts*0.33 + 0.5), Measure.Unit.PT));
      
      // HG "XTick" property, if present, is a vector specifying the locations of tick marks along the X axis. If the
      // axis is configured for auto-range adjustment, the property is ignored UNLESS it is explicitly set to an empty 
      // vector (null), in which case the AxisNode's single initial TickSetNode is deleted. Otherwise: if explicitly 
      // specified, we use the vector to determine the start, end, and tick interval for that single TickSetNode. If 
      // implicit, Matlab auto-generates the tick locations; in this case, we configure the TickSetNode to display 5 
      // tick marks over the axis range. 
      //
      // (16jan2015) X-axis tick labels. If XTickLabelMode=='manual' and XTickLabel is a String[], then the string array
      // is interpreted as a set of user-defined tick mark labels. These are applied to the first tick set node on the
      // X-axis, IF there is one. If XTickLabel is a simple non-empty string rather than a string array, then each
      // character in the string is considered a separate tick label (e.g., "ABCD").
      //
      // (30jan2015) Log axis. If the axis is logarithmic, then the tick interval should be 10, 100, etc (FC should but 
      // currently does not support intervals of 0.1, 0.01, and so on, but Matlab does). Furthermore, perLogIntv is set 
      // to [1] to ensure tick marks at the powers of 10.
      // (30jan2015) If 'XMinorTick' is 'on' and the first tick set ("major" ticks) was not removed, then we add a 
      // second tick set to render the minor tick marks. Their length is set to 1/2 that of the major tick marks, and 
      // their labels are turned off. When the axis is linear, this second tick set is configured so that the minor 
      // ticks sub-divide a single major tick interval into 10, 5 or 2 equal parts, depending on whether the # of major 
      // tick intervals is 2 or less, 3-5, or more than 5, respectively. When the axis is logarithmic, the range and
      // interval matches that of the major tick mark set, but perLogIntv is set to [2 3 4 ... 9].
      //
      // (20feb2015) Matlab 2014b apparently sets 'XTickLabelsMode' to 'manual' instead of 'XTickLabelMode' (note the
      // difference!). So now we check both properties. If either one is set to 'manual', then we use the tick mark
      // labels in 'XTickLabel', if any. The same issue applies to 'YTickLabelMode' vs 'YTickLabelsMode'.
      //
      // (28feb2018) In a .FIG generated by Matlab 2017b, 'YTickLabelMode' was missing even though 'YTickLabel' was 
      // present and contained custom tick labels. In .FIG's generated by older versions, 'YTickLabelMode' was present
      // and set to 'manual' whenever custom tick labels had been set. This makes it harder to decide whether or not
      // custom labels are present. Even though 'auto' is the default, we can no longer assume that a missing 
      // '*TickLabelMode' property means that custom labels have been manually set. New logic introduced:
      // 1) If '*TickLabelMode' exists and is set to 'auto', then assume no custom tick marks labels
      // 2) If '*TickLabelMode' does not exist, or if it exists and is set to 'manual', then check '*TickLabel' for
      // the custom tick mark labels.
      //
      ticks = (TickSetNode) xAxis.getChildAt(0);
      double start = 0;
      double end = 0;
      double intv = 0;
      boolean removeTickSet = false;
      if(hgAxes.hasProperty("XTick"))
      {
         Object tickProp = hgAxes.getProperty("XTick");
         if(tickProp == null || !tickProp.getClass().equals(double[].class))
            removeTickSet = true;
         else
         {
            double[] markLocs = (double[]) tickProp;
            if(markLocs.length == 0) removeTickSet = true;
            else if(markLocs.length == 1)
            {
               start = end = markLocs[0];
               intv = xLinear ? 1 : 10;
            }
            else
            {
               start = markLocs[0];
               end = markLocs[markLocs.length-1];
               if(xLinear)
                  intv = (end-start) / (markLocs.length - 1);
               else
                  intv = chooseLogTickInterval(start, end, markLocs.length-1);
            }
         }
      }
      
      if(removeTickSet) 
         xAxis.getGraphicModel().deleteNode(ticks);
      else if(!autoX)
      {
         if(intv == 0)
         {
            start = xAxis.getStart();
            end = xAxis.getEnd();
            if(xLinear)
            {
               intv = (end - start) / 4.0;
               if(intv == 0) intv = 1;
            }
            else
               intv = chooseLogTickInterval(start, end, 4);
         }
         
         ticks.setTrackingParentAxis(false);
         ticks.setStart(Utilities.limitSignificantDigits(start, 6));
         ticks.setEnd(Utilities.limitSignificantDigits(end, 6));
         ticks.setInterval(Utilities.limitSignificantDigits(intv,6));
         ticks.setDecadeTicks(new LogTickPattern(0x02));
         
         // choose floating-pt tick label format based on the absolute axis range
         LabelFormat tickFmt = LabelFormat.INT;
         double rng = Math.abs(end-start);
         if(rng <= 0.1) tickFmt = LabelFormat.F3;
         else if(rng <= 1) tickFmt = LabelFormat.F2;
         else if(rng <= 10) tickFmt = LabelFormat.F1;
         ticks.setTickLabelFormat(tickFmt);
         
         // handle possible custom tick mark labels
         Object prop = hgAxes.getProperty("XTickLabelMode");
         if(prop == null) prop = hgAxes.getProperty("XTickLabelsMode");
         if(prop == null || "manual".equals(prop))
         {
            prop = hgAxes.getProperty("XTickLabel");
            if(prop != null && prop.getClass().equals(String[].class) && ((String[])prop).length > 0)
            {
               String[] tLabels = (String[]) prop;
               for(int i=0; i<tLabels.length; i++) tLabels[i] = processSpecialCharacters(tLabels[i]);
               ticks.setCustomTickLabels(tLabels);
            }
            else if(prop != null && prop.getClass().equals(String.class) && !((String) prop).isEmpty())
            {
               String labelChars = (String) prop;
               String[] labels = new String[labelChars.length()];
               for(int i=0; i<labels.length; i++) labels[i] = labelChars.substring(i, i+1);
               ticks.setCustomTickLabels(labels);
            }
         }
         
         // check for Matlab minor tick marks: 'XMinorTick' == 'on' (default is 'off'). If so, we add a second tick set
         // with tick length half that of the major ticks, and no labels. 
         if("on".equals(hgAxes.getProperty("XMinorTick"))) addMinorTicksToAxis(xAxis, !xLinear);
      }
      
      // HG "YTick" property: Analogously for the Y axis...
      ticks = (TickSetNode) yAxis.getChildAt(0);
      start = 0;
      end = 0;
      intv = 0;
      removeTickSet = false;
      if(hgAxes.hasProperty("YTick"))
      {
         Object tickProp = hgAxes.getProperty("YTick");
         if(tickProp == null || !tickProp.getClass().equals(double[].class))
            removeTickSet = true;
         else
         {
            double[] markLocs = (double[]) tickProp;
            if(markLocs.length == 0) removeTickSet = true;
            else if(markLocs.length == 1)
            {
               start = end = markLocs[0];
               intv = yLinear ? 1 : 10;
            }
            else
            {
               start = markLocs[0];
               end = markLocs[markLocs.length-1];
               if(yLinear)
                  intv = (end-start) / (markLocs.length - 1);
               else
                  intv = chooseLogTickInterval(start, end, markLocs.length-1);
            }
         }
      }

      if(removeTickSet) 
         yAxis.getGraphicModel().deleteNode(ticks);
      else if(!autoY)
      {
         if(intv == 0)
         {
            start = yAxis.getStart();
            end = yAxis.getEnd();
            if(yLinear)
            {
               intv = (end - start) / 4.0;
               if(intv == 0) intv = 1;
            }
            else
               intv = chooseLogTickInterval(start, end, 4);
         }
         
         ticks.setTrackingParentAxis(false);
         ticks.setStart(Utilities.limitSignificantDigits(start, 6));
         ticks.setEnd(Utilities.limitSignificantDigits(end, 6));
         ticks.setInterval(Utilities.limitSignificantDigits(intv,6));
         ticks.setDecadeTicks(new LogTickPattern(0x02));
         
         // choose floating-pt tick label format based on the absolute axis range
         LabelFormat tickFmt = LabelFormat.INT;
         double rng = Math.abs(end-start);
         if(rng <= 0.1) tickFmt = LabelFormat.F3;
         else if(rng <= 1) tickFmt = LabelFormat.F2;
         else if(rng <= 10) tickFmt = LabelFormat.F1;
         ticks.setTickLabelFormat(tickFmt);
         
         // handle possible custom tick mark labels
         Object prop = hgAxes.getProperty("YTickLabelMode");
         if(prop == null) prop = hgAxes.getProperty("YTickLabelsMode");
         if(prop == null || "manual".equals(prop))
         {
            prop = hgAxes.getProperty("YTickLabel");
            if(prop != null && prop.getClass().equals(String[].class) && ((String[])prop).length > 0)
            {
               String[] tLabels = (String[]) prop;
               for(int i=0; i<tLabels.length; i++) tLabels[i] = processSpecialCharacters(tLabels[i]);
               ticks.setCustomTickLabels(tLabels);
            }
            else if(prop != null && prop.getClass().equals(String.class) && !((String) prop).isEmpty())
            {
               String labelChars = (String) prop;
               String[] labels = new String[labelChars.length()];
               for(int i=0; i<labels.length; i++) labels[i] = labelChars.substring(i, i+1);
               ticks.setCustomTickLabels(labels);
            }
         }
         
         // minor tick mark set?
         if("on".equals(hgAxes.getProperty("YMinorTick"))) addMinorTicksToAxis(yAxis, !yLinear);
      }

      // X- and Y-axis labels. Matlab defines these as "text" children of the HG "axes" object. We ignore all properties
      // of the "text" object except the text string itself. Note that the default FypML axis node gets a non-empty 
      // label, so we must clear this if no axis label is specified for the Matlab "axes". The axis labels could have
      // TeX-encoded special characters, which we translate if possible.
      String label = hgAxes.getXLabel();
      xAxis.setTitle(label != null ? processSpecialCharacters(label) : "");
      label = hgAxes.getYLabel();
      yAxis.setTitle(label != null ? processSpecialCharacters(label) : "");

      // axis label offsets. Positioning of axis labels is very ugly in Matlab, using a 'Position' property on the 
      // label's Text object with default units of 'data'. Rather than deal with that, we opted for these defaults:
      // For the horizontal X-axis, we set the offset to 1.5x the font size in pts. For vertical bars, we need to 
      // account for the longest tick label, which is considerably more difficult. Rather than trying to accurately 
      // measure the length of the longest tick label in pts, we assume a width/height ratio of 0.6. In this case we
      // also offset for the tick label gap and, if ticks are directed outward, the tick length.
      int ofs = (int) Math.round(graph.getFontSizeInPoints() * 1.5);
      xAxis.setLabelOffset(new Measure(ofs, Measure.Unit.PT));

      String longest = "";
      tickLen = 0;
      if(yAxis.getChildCount() > 0)
      {
         ticks = (TickSetNode) yAxis.getChildAt(0);
         longest = ticks.getLongestTickLabel();
         if(ticks.getTickOrientation() == Orientation.OUT) tickLen = ticks.getTickLength().toMilliInches(); 
      }
      ofs = (int) Math.round(longest.length() * graph.getFontSizeInPoints() * 0.6);
      ofs += (int) (graph.getFontSizeInPoints().doubleValue()*0.33 + 0.5); 
      ofs += (int) (72.0*tickLen/1000);
      yAxis.setLabelOffset(new Measure(ofs, Measure.Unit.PT));

      // in Matlab, the axis line always lies on the edge of the graph data window. There's no property that creates a
      // gap between axis line and graph edge, as is possible in FypML. So here we explicitly set the FypML spacer
      // property to 0in.
      xAxis.setSpacer(new Measure(0, Measure.Unit.IN));
      yAxis.setSpacer(new Measure(0, Measure.Unit.IN));
      
      // axes title. Matlab also defines this as a "text" child, like the X- and Y-axis labels. If the text object 
      // exists and has a non-null "String" property, we use that string as the GraphNode's non-rendered title. In the 
      // next section, a text label is added to the graph to render the title as it appears in the Matlab figure.
      String title = hgAxes.getAxesTitle();
      if(title != null) graph.setTitle(processSpecialCharacters(title));
      
      // 
      // convert each supported HG plot object child or "text" child to the appropriate figure graphics node. However, 
      // those "text" objects representing the x- and y-axis labels are skipped, as are any "text" objects that have a
      // null or empty string for their "String" property.
      for(int i=0; i<hgAxes.getChildCount(); i++)
      {
         HGObject hgChild = hgAxes.getChildAt(i);
         if(hgChild.getType().equals("graph2d.lineseries"))
         {
            if(hgChild.isCoalescedScatterPlot())
               addCoalescedScatterPlotToGraph(graph, hgChild);
            else
               addDataTraceToGraph(graph, hgChild, false);
         }
         else if(hgChild.getType().equals("specgraph.stairseries"))
            addDataTraceToGraph(graph, hgChild, false);
         else if(hgChild.getType().equals("specgraph.errorbarseries"))
            addErrorBarTraceToGraph(graph, hgChild);
         else if(hgChild.getType().equals("specgraph.scattergroup"))
            addScatterGroupToGraph(graph, hgChild, false);
         else if(hgChild.getType().equals("specgraph.barseries"))
            addBarPlotToGraph(graph, hgChild, matCM, cLim);
         else if(hgChild.getType().equals("specgraph.stemseries"))
            addDataTraceToGraph(graph, hgChild, false);
         else if(hgChild.getType().equals("specgraph.baseline"))
            addBarOrStemPlotBaselineToGraph(graph, hgChild);
         else if(hgChild.getType().equals("specgraph.areaseries"))
            addAreaChartToGraph(graph, hgChild, matCM, cLim);
         else if(hgChild.getType().equals("patch"))
         {
            if(!addPatchAsHistogram(graph, hgChild, matCM, cLim))
               addPatchAsErrorBand(graph, hgChild);
         }
         else if(hgChild.getType().equals("histogram"))
            addHistogramToGraph(graph, hgChild, false);
         else if(hgChild.getType().equals("surface") || hgChild.getType().equals("image"))
            addHeatMapToGraph(graph, hgChild);
         else if(hgChild.getType().equals("specgraph.contourgroup"))
            addContourPlotToGraph(graph, hgChild);
         else if(hgChild.getType().equals("line"))
            addLineToGraph(graph, hgChild);
         else if(hgChild.getType().equals("text") && !hgAxes.isAxesLabelHandle(hgChild.getHandle()))
            addTextLabelToGraph(graph, hgAxes, hgChild, hgAxes.isAxesTitleHandle(hgChild.getHandle()));
      }
      fixLabelLocationsInGraph(graph);
      
      // if any (x,y) data lies outside the graph's data window, go ahead and set its clip flag
      if(graph.isXYDataOutOfBounds()) graph.setClip(true);
      
      // if a HG "scribe.legend" object is attached to the "axes", then configure the GraphNode's legend accordingly.
      configureGraphLegend(graph, hgAxes, hgFig);
      
      // if a HG "scribe.colorbar" object is attached to the "axes", then configure GraphNode's colorbar accordingly.
      configureGraphColorBar(graph, hgAxes, hgFig);
   }

   /**
    * Helper method for {@link #addGraphToFigure}. It decides on a tick mark interval for a logarithmic axis covering
    * the specified range. If it encounters an unreasonable scenario (such as the axis range crossing zero, which does
    * not make much sense for a log axis!), it returns 10. The interval is always a positive integer power of 10, since
    * that's all that <i>Figure Composer</i> supports.
    * @param start Start of axis range.
    * @param end End of axis range.
    * @param n Suggested number of tick mark intervals to be displayed in the given range.
    * @return The chosen tick mark interval, which will be a positive integral power of 10.
    */
   private static double chooseLogTickInterval(double start, double end, int n)
   {
      if(start == 0 && end == 0) return(10);
      if(start <= 0 && end > 0) start = Math.min(end/10, 0.001);
      else if(start > 0 && end <= 0) end = Math.min(start/10, 0.001);

      double intv = end / start;
      if(intv < 1) intv = start / end;
      
      int numIntvs = (int) Math.log10(intv);
      int p = 1;
      if(n <= 0)  p = numIntvs / 3;
      else if(n >= 2) p = numIntvs / n;
      if(p < 1) p = 1;
      
      return(Math.pow(10, p));
   }
   
   /**
    * Helper method for {@link #addGraphToFigure}. It adds a minor tick mark set to the specified <i>FypML</i> 2D axis
    * node roughly consistent with how <i>Matlab</i> renders minor tick marks on an axis. The tick mark length is set to
    * one-half that of the first ("major") tick mark set, the tick orientation is the same, and labels are turned off.
    * @param axis A <i>FypML</i> 2D axis node.
    * @param isLog True for a logarithmic (base 10) axis; false for a linear axis.
    */
   private static void addMinorTicksToAxis(AxisNode axis, boolean isLog)
   {
      if(!axis.getGraphicModel().insertNode(axis, FGNodeType.TICKS, -1))
         throw new NeverOccursException("Failed to insert new tick set into FypML axis.");
      TickSetNode majorTicks = (TickSetNode) axis.getChildAt(0);
      TickSetNode minorTicks = (TickSetNode) axis.getChildAt(1);
      
      // minor ticks have same range as major ticks
      minorTicks.setTrackingParentAxis(false);
      minorTicks.setStart(majorTicks.getStart());
      minorTicks.setEnd(majorTicks.getEnd());
      
      if(isLog)
      {
         // if logarithmic, use same interval as for the major tick mark set, and turn on per-decade ticks at [2 .. 9].
         minorTicks.setInterval(majorTicks.getInterval());
         minorTicks.setDecadeTicks(new LogTickPattern(0x03fc));
      }
      else
      {
         // if linear, interval is 1/10, 1/5, or 1/2 that of the major tick mark set, depending on # of major tick divs.
         int n = majorTicks.getTickSet().length;
         double scale = n <= 3 ? 10.0 : (n <= 6 ? 5.0 : 2.0);
         minorTicks.setInterval(majorTicks.getInterval() / scale);
         minorTicks.setDecadeTicks(majorTicks.getDecadeTicks());
      }
      
      // no labels; tick length half that for the major tick marks; same orientation
      minorTicks.setTickLabelFormat(LabelFormat.NONE);
      Measure m = majorTicks.getTickLength();
      m = new Measure(m.getValue() / 2.0, m.getUnits());
      minorTicks.setTickLength(TickSetNode.TICKLENCONSTRAINTS.constrain(m));
      minorTicks.setTickOrientation(majorTicks.getTickOrientation());
   }
   
   /**
    * Helper method for {@link #add3DGraphToFigure}. Similar to {@link #addMinorTicksToAxis(AxisNode, boolean)}, but
    * it handles the FypML 3D graph axis, which is somewhat different than the 2D case.
    * @param a3 a <i>FypML</i> 3D axis node.
    */
   private static void addMinorTicksToAxis(Axis3DNode a3)
   {
      if(!a3.getGraphicModel().insertNode(a3, FGNodeType.TICKS3D, -1))
         throw new NeverOccursException("Failed to insert new tick set into FypML 3D axis.");
      Ticks3DNode majorTicks = (Ticks3DNode) a3.getChildAt(0);
      Ticks3DNode minorTicks = (Ticks3DNode) a3.getChildAt(1);
      
      // minor ticks have same range as major ticks. Note that setStart() fails if value is greater than current end.
      double start = majorTicks.getStart();
      if(start > minorTicks.getEnd())
      {
         minorTicks.setEnd(majorTicks.getEnd());
         minorTicks.setStart(start);
      }
      else
      {
         minorTicks.setStart(start);
         minorTicks.setEnd(majorTicks.getEnd());
      }
      
      if(a3.isLogarithmic())
      {
         // if logarithmic, use same interval as for the major tick mark set, and turn on per-decade ticks at [2 .. 9].
         minorTicks.setInterval(majorTicks.getInterval());
         minorTicks.setDecadeTicks(new LogTickPattern(0x03fc));
      }
      else
      {
         // if linear, interval is 1/10, 1/5, or 1/2 that of the major tick mark set, depending on # of major tick divs.
         int n = majorTicks.getTickSet().length;
         double scale = n <= 3 ? 10.0 : (n <= 6 ? 5.0 : 2.0);
         minorTicks.setInterval(majorTicks.getInterval() / scale);
         minorTicks.setDecadeTicks(majorTicks.getDecadeTicks());
      }
      
      // no labels; tick length half that for the major tick marks; same orientation
      minorTicks.setTickLabelFormat(LabelFormat.NONE);
      Measure m = majorTicks.getTickLength();
      m = new Measure(m.getValue() / 2.0, m.getUnits());
      minorTicks.setTickLength(TickSetNode.TICKLENCONSTRAINTS.constrain(m));
      minorTicks.setTickOrientation(majorTicks.getTickOrientation());
   }
   
   /**
    * Helper method retrieves the plot box aspect ratio for the specified Matlab HG 'axes' object. It examines the
    * 'PlotBoxAspectRatio', 'PlotBoxAspectRatioMode', and 'DataBoxAspectRatioMode' properties, but it ignores the
    * 'DataAspectRatio' property.
    * 
    * <ul>
    * <li>If either mode value is explicitly set to 'manual', then the aspect ratio [px py pz] in 'PlotBoxAspectRatio'
    * is used. However, if 'PlotBoxAspectRatio' is implicit, it uses the 'XLim', 'YLim', and 'ZLim' properties to
    * compute the aspect ratio as [ |x1-x0| |y1-y0| |z1-z0| ]. The 'DataAspectRatio' property is ignored.</li>
    * <li>Matlab 2014b and later complicates matters because it sets 'PlotBoxAspectRatioMode' to null when the related
    * property 'PlotBoxAspectRatio' is explicitly set; analogously for 'DataAspectRatio'. In this scenario, if either
    * mode value is null, but 'PlotBoxAspectRatio' is set, we use that as the aspect ratio.</li>
    * <li>Otherwise, we assume the 'axes' should be stretched to fill the available space as defined by its 'Position'
    * property.</li>
    * </ul>
    * 
    * @param hgAxes The Handle Graphics 'axes' object.
    * @return The plot box aspect ratio [px py pz]. Returns null if the axes stretches to fill the plot box specified by 
    * the object's 'Position' property.
    */
   private static double[] getAxesAspectRatio(HGObject hgAxes)
   {
      Object pbarModeProp = hgAxes.getProperty("PlotBoxAspectRatioMode");
      Object darModeProp = hgAxes.getProperty("DataAspectRatioMode");
      double[] aspect = HGObject.getDoubleVectorFromProperty(hgAxes.getProperty("PlotBoxAspectRatio"));

      // pre-2014b: if either mode value is explicitly "manual", then we're not in stretch-to-fill mode
      // 2014b and later: If 'PlotBoxAspectRatioMode' is null (meaning "manual"), or if it is explicitly "auto" but the 
      // "DataAspectRatioMode" is null, then we're not in stretch-to-fill mode.
      boolean needPBAR = "manual".equals(pbarModeProp) || "manual".equals(darModeProp) || 
            (pbarModeProp == null && aspect != null) || ("auto".equals(pbarModeProp) && darModeProp == null);
      
      // if plot box aspect ratio is not available, yet the mode values indicate that the axes is not is stretch-to-fill
      // mode, then we rely on the axis ranges to compute the aspect ratio. If we can't get all 3 axis ranges, then we
      // assume an aspect ratio of 1:1:1
      if(aspect == null && needPBAR)
      {
         double xRng = -1;
         Object rngProp = hgAxes.getProperty("XLim");
         if(rngProp != null && rngProp.getClass().equals(double[].class))
         {
            double[] range = (double[]) rngProp;
            if(range.length == 2 && Utilities.isWellDefined(range))
               xRng = Math.abs(range[1] - range[0]);
         }
         double yRng = -1;
         rngProp = hgAxes.getProperty("YLim");
         if(rngProp != null && rngProp.getClass().equals(double[].class))
         {
            double[] range = (double[]) rngProp;
            if(range.length == 2 && Utilities.isWellDefined(range))
               yRng = Math.abs(range[1] - range[0]);
         }
         
         double zRng = -1;
         rngProp = hgAxes.getProperty("ZLim");
         if(rngProp != null && rngProp.getClass().equals(double[].class))
         {
            double[] range = (double[]) rngProp;
            if(range.length == 2 && Utilities.isWellDefined(range))
               zRng = Math.abs(range[1] - range[0]);
         }
         
         if(xRng > 0 && yRng > 0 && zRng > 0)
            aspect = new double[] {xRng, yRng, zRng};
         else
            aspect = new double[] {1, 1, 1};
      }
      
      return(needPBAR ? aspect : null);
   }
   
   /**
    * Append a new polar plot node ('pgraph' element) to the FypML figure that renders a Matlab Handle Graphics "axes" 
    * object that has been configured as a polar plot.
    * 
    * <p>When an "axes" object is added to the HG object hierarchy, its properties and children are examined to see if 
    * it represents a Matlab polar plot. Matlab renders data in a rectangular coordinate system, but emulates a polar
    * plot by hiding the Cartesian axes and adding a bunch of core objects ("patch", "line", and "text") to render a
    * 4-quadrant polar coordinate grid on top of this rectangular coordinate system, with the origin at the center.
    * When this is the case, post-processing of the "axes" object tags it as a Matlab polar plot and removes those
    * child objects that represent the polar grid, while keeping the "text" objects that render the tick mark labels
    * associated with that grid. [Note: One grid line object is retained in order to replicate its styling; see {@link
    * HGObject#getPolarGridLineObject()}.]</p>
    * 
    * <p>This method converts such an "axes" object to a FypML polar plot node (introduced in FC 5.1.2), with a 360-deg
    * layout and with the polar plot's theta and radial axes configured to replicate the polar grid as much as possible.
    * Call this method rather than {@link #addGraphToFigure} whenever the "axes" object has been marked as a polar
    * plot.</p>
    * 
    * <p>Note that plot children of the "axes" have data in Cartesian coordinates. These data must be converted to polar
    * coordinates before adding the FypML data sets to the polar plot container, 'pgraph'.</p>
    * 
    * <p><i>Special case: Pie chart.</i> A Matlab pie chart is an "axes" containing a sequence of "patch"/"text" pairs
    * of children that render each "slice" of the pie with an accompanying label. During pre-processing, the "axes"
    * parent is tagged as a polar plot containing a pie chart, and the "patch" objects are coalesced into a single 
    * "patch" in which the pie chart information is consolidated. In this scenario, the polar plot node is configured
    * such that the polar grid is hidden.</p>
    * 
    * @param fig The FypML figure node that will parent the new polar plot node.
    * @param hgFig The Matlab Handle Graphics object defining the Matlab figure.
    * @param hgAxes The Matlab Handle Graphics object defining the child "axes" that is rendered as a polar plot and is
    * to be converted into a FypML polar plot that replicates its appearance to the extent possible.
    * @param cm The FypML-supported colormap that best matches the Matlab figure's colormap. This will be assigned to
    * the colorbar of the FypML polar plot created. (A Matlab figure can only have one colormap, while FypML can 
    * assign a different colormap to each 2D graph container.)
    * @param matCM The Matlab figure's colormap. We need this to process indexed colors specified for a "patch" object
    * representing a pie chart, which is only rendered in a polar plot.
    */
   private static void addPolarPlotToFigure(
         FigureNode fig, HGObject hgFig, HGObject hgAxes, ColorMap cm, double[][] matCM)
   {
      if(!fig.getGraphicModel().insertNode(fig, FGNodeType.PGRAPH, -1))
         throw new NeverOccursException("Failed to insert new polar plot into FypML figure.");
      
      PolarPlotNode pgraph = (PolarPlotNode) fig.getChildAt(fig.getChildCount()-1);
      PolarAxisNode thetaAxis = pgraph.getThetaAxis();
      PolarAxisNode rAxis = pgraph.getRadialAxis();
      ColorBarNode colorBar = pgraph.getColorBar();
      
      // assign the figure's colormap to the graph's color axis
      colorBar.setColorMap(cm);
      
      // handle font-related properties
      setFontProperties(pgraph, hgAxes);

      // HG "Position" and "Units" properties determine the location of the bounding box WRT the parent figure.
      // Typically, "Units" defaults to "normalized", so we need the figure's dimensions in order to get the bounding
      // box location and size in real units (% units not allowed in DataNav FypML).
      Object posProp = hgAxes.getProperty("Position");
      if(posProp == null || !posProp.getClass().equals(double[].class))
         throw new NeverOccursException("Handle Graphics 'axes' object missing 'Position' property");
      double[] pos = (double[]) posProp;
      if(pos.length < 4 || pos[2] <= 0 || pos[3] <= 0) 
         throw new NeverOccursException("Handle Graphics 'Position' is invalid for 'axes' object");
      
      double scaleFac = 0;  // for special case: normalized units
      Object unitsProp = hgAxes.getProperty("Units");
      if(unitsProp != null && unitsProp.getClass().equals(String.class))
      {
         String units = (String) unitsProp;
         switch(units)
         {
         case "normalized":
            scaleFac = 0;
            break;
         case "inches":
            scaleFac = 1.0;
            break;
         case "centimeters":
            scaleFac = Measure.MM2IN * 10.0;
            break;
         default:
            // "pixels" and "characters" are not supported and map arbitrarily to "points"
            scaleFac = Measure.PT2IN;
            break;
         }
      }
      if(scaleFac == 0)
      {
         // convert normalized values to inches WRT parent figure viewport
         pos[0] = fig.getWidthMI() * pos[0] / 1000.0;
         pos[1] = fig.getHeightMI() * pos[1] / 1000.0;
         pos[2] = fig.getWidthMI() * pos[2] / 1000.0;
         pos[3] = fig.getHeightMI() * pos[3] / 1000.0;
        
      }
      else for(int i=0; i<pos.length; i++) pos[i] *= scaleFac;
      
      // adjust bounding box dimensions for aspect ratio if Matlab axes is not in "stretch to fill" mode
      double[] aspect = getAxesAspectRatio(hgAxes);
      if(aspect != null)
      {
         if((aspect[0] / aspect[1]) >= (pos[2] / pos[3]))
         {
            double oldH = pos[3];
            pos[3] = pos[2] * aspect[1] / aspect[0];
            pos[1] += (oldH - pos[3]) / 2.0;
         }
         else
         {
            double oldW = pos[2];
            pos[2] = pos[3] * aspect[0] / aspect[1];
            pos[0] += (oldW - pos[2]) / 2.0;
         }
      }     

      pgraph.setX(new Measure(Utilities.limitFracDigits(pos[0], 3), Measure.Unit.IN));
      pgraph.setY(new Measure(Utilities.limitFracDigits(pos[1], 3), Measure.Unit.IN));
      pgraph.setWidth(new Measure(Utilities.limitFracDigits(pos[2], 3), Measure.Unit.IN));
      pgraph.setHeight(new Measure(Utilities.limitFracDigits(pos[3], 3), Measure.Unit.IN));
      
      // HG "LineWidth" property maps to the graph's "strokeWidth". Units=point. Default is 0.5pt. 
      pgraph.setMeasuredStrokeWidth(processLineWidth(hgAxes));

      // HG "XAxisLocation", "YAxisLocation", "XScale", "YScale" are ignored, since we already know this is a polar
      // plot. We also do not need to check "Visible". The polar plot's axes are hidden only if we're doing a pie chart.

      // the grid lines are rendered in the Matlab polar plot with a series of HG "line" objects. While these are 
      // removed during post-processing of the HG tree, one is kept so that we can access its "Color", "LineStyle", and
      // "LineWidth" properties and apply them to the theta and radial axes of the FypML polar plot node.
      Color gridC = Color.BLACK;
      StrokePattern gridPat = StrokePattern.SOLID;
      Measure gridSW = new Measure(0.5, Measure.Unit.PT);
      HGObject hgGrid = hgAxes.getPolarGridLineObject();
      if(hgGrid != null && hgGrid.getType().equals("line"))
      {
         gridC = processColorSpec(hgGrid.getProperty("Color"));
         gridSW = processLineWidth(hgGrid);
         gridPat = processLineStyle(hgGrid);
         if(gridPat == null)
         {
            // FypML uses 0 stroke width to achieve the "none" line style
            gridSW = new Measure(0, Measure.Unit.PT);
            gridPat = StrokePattern.SOLID;
         }
      }
      thetaAxis.setStrokeColor(gridC);
      rAxis.setStrokeColor(gridC);
      thetaAxis.setMeasuredStrokeWidth(gridSW);
      rAxis.setMeasuredStrokeWidth(gridSW);
      thetaAxis.setStrokePattern(gridPat);
      rAxis.setStrokePattern(gridPat);
      
      // if the "axes" contains a pie chart, then the polar grid is hidden -- hide both axes in the polar plot
      thetaAxis.setHide(hgAxes.isPieChart());
      rAxis.setHide(hgAxes.isPieChart());
      
      // HG "XColor, YColor" properties set stroke and fill color for theta axis and radial axis, respectively
      Color c = processColorSpec(hgAxes.getProperty("XColor"));
      if(c != null)
      {
         thetaAxis.setStrokeColor(c);
         thetaAxis.setFillColor(c);
      }
      c = processColorSpec(hgAxes.getProperty("YColor"));
      if(c != null)
      {
         rAxis.setStrokeColor(c);
         rAxis.setFillColor(c);
      }
      
      // Axis ranges: Theta axis range is always set to [0..360]; radial axis range is [0..R], where R is the maximum
      // radial value as determined during pre-processing of the 'axes' object. The HG "XLim, XDir, YLim, YDir" 
      // properties of the 'axes' are ignored.
      thetaAxis.setRange(0, 360);
      rAxis.setRange(0, hgAxes.getMaxRadius());
      
      // we need "CLim" to to do scaled indexed color mapping for a "patch" object representing a pie chart
      Object rngProp = hgAxes.getProperty("CLim");
      double[] cLim = null;
      if(rngProp != null && rngProp.getClass().equals(double[].class))
      {
         cLim = (double[]) rngProp;
         if(cLim.length == 2 && Utilities.isWellDefined(cLim))
         {
            colorBar.setStart(Utilities.limitSignificantDigits(cLim[0], 6));
            colorBar.setEnd(Utilities.limitSignificantDigits(cLim[1], 6));
         }
         else cLim = null;
      }

      // ignore HG "TickDir, TickLength, XTick, YTick" properties. No tick marks are drawn. When a Matlab 'axes' is 
      // configured as a polar plot, the grid divisions are regularly spaced. The grid interval for the theta axis is
      // 30 deg; the radial axis grid interval is determined when the "axes" object is processed.
      thetaAxis.setGridDivisions(new double[] {0, 0, 30});
      rAxis.setGridDivisions(new double[] {0, 0, hgAxes.getRadialTickInterval()});
      
      // plot title and axis labels: In a FypML polar plot, the theta and radial axes are not labelled. If a title is
      // associated with the Matlab 'axes', we use that as the PolarPlotNode's non-rendered title. Also, in the next
      // section, a text label is added to the polar plot to render the title as it appears in the Matlab figure.
      String title = hgAxes.getAxesTitle();
      if(title != null) pgraph.setTitle(processSpecialCharacters(title));
      
      // convert each supported HG plot object child or "text" child to the appropriate figure graphics node. However, 
      // those "text" objects representing the x- and y-axis labels are skipped, as are any "text" objects that have a
      // null or empty string for their "String" property. NOTE that we support conversion of fewer plot object types 
      // in a polar context vs a Cartesian context: "specgraph.errorbarseries", "patch", and "surface" are NOT supported
      // when the parent "axes" is polar.
      for(int i=0; i<hgAxes.getChildCount(); i++)
      {
         HGObject hgChild = hgAxes.getChildAt(i);
         if(hgChild.getType().equals("graph2d.lineseries"))
            addDataTraceToGraph(pgraph, hgChild, false);
         else if(hgChild.getType().equals("specgraph.scattergroup"))
            addScatterGroupToGraph(pgraph, hgChild, false);
         else if(hgChild.getType().equals("line"))
            addLineToGraph(pgraph, hgChild);
         else if(hgChild.getType().equals("text") && !hgAxes.isAxesLabelHandle(hgChild.getHandle()))
            addTextLabelToGraph(pgraph, hgAxes, hgChild, hgAxes.isAxesTitleHandle(hgChild.getHandle()));
         else if(hgChild.getType().equals("patch") && hgChild.isPieChart())
            addPatchAsPieChart(pgraph, hgChild, matCM, cLim);
      }
      
      // we always set the clip flag for a polar plot.
      pgraph.setClip(true);
      
      // if a HG "scribe.legend" object is attached to the "axes", then configure the polar plot's legend accordingly.
      configureGraphLegend(pgraph, hgAxes, hgFig);
      
      // if a HG "scribe.colorbar" object attached to the "axes", then configure the polar plot's color bar accordingly
      configureGraphColorBar(pgraph, hgAxes, hgFig);
   }

   
   /**
    * Convert a 3D Matlab plot, as represented by the Handle Graphics 'axes' object, to FypML's 3D graph node and append
    * it to the figure node specified.
    * 
    * <p>The method will attempt to replicate the position, size, orientation and appearance of the 3D graph to the 
    * extent possible, given the many differences in the FypML and Matlab 3D graph models. It will also convert all 
    * supported HG plot, text, and other objects that are children of the HG axes object. In addition, if the figure 
    * includes a "scribe.legend" and/or "scribe.colorbar" child attached to this "axes", the 3D graph node's legend 
    * and/or colorbar components are shown and configured  accordingly.</p>
    * 
    * <p>The specified HG "axes" object must be tagged as 3D -- see {@link HGObject#is3DAxes()}. Do NOT use this method 
    * to translate a Matlab "axes" configured as a 2D plot; use {@link #addPolarPlotToFigure} for 2D polar plots and
    * {@link #addGraphToFigure} for 2D Cartesian plots.</p>
    * 
    * @param fig The FypML figure node that will parent the new 3D graph node.
    * @param hgFig The Matlab Handle Graphics object defining the Matlab figure.
    * @param hgAxes The Matlab Handle Graphics object defining the child "axes" to be converted into a FypML 3D graph.
    * @param cm The FypML-supported colormap that best matches the Matlab figure's colormap. This will be assigned to
    * the FypML 3D graph node created. (A Matlab figure can only have one colormap, while FypML can assign a different 
    * colormap to each graph.)
    * @param matCM The Matlab figure's colormap. We need this to process indexed colors specified for a "patch" object
    * representing a histogram.
    */
   private static void add3DGraphToFigure(
         FigureNode fig, HGObject hgFig, HGObject hgAxes, ColorMap cm, double[][] matCM)
   {
      if(!fig.getGraphicModel().insertNode(fig, FGNodeType.GRAPH3D, -1))
         throw new NeverOccursException("Failed to insert new 3D graph into FypML figure.");
      
      Graph3DNode g3 = (Graph3DNode) fig.getChildAt(fig.getChildCount()-1);
      
      // assign the figure's colormap to the 3D graph's colorbar
      g3.getColorBar().setColorMap(cm);
      
      // handle font-related properties
      setFontProperties(g3, hgAxes);
      
      // HG "LineWidth" property maps to the graph's "strokeWidth". Units=point. Default is 0.5pt. 
      g3.setMeasuredStrokeWidth(processLineWidth(hgAxes));

      // HG "View" property = [az, el] holds azimuth and elevation angles in degrees, although the relationship between
      // Matlab's azimuth and FypML's azimuth is not straightforward. ALSO NOTE that "View" is considered an obsolete
      // property, so it may disappear in later versions. If it is not available, we assume az=30, el=15. Also, we
      // enforce FypML restrictions on the values of az and el.
      double azimuth = 30;
      double elev = 15;
      Object viewProp = hgAxes.getProperty("View");
      if(viewProp != null && viewProp.getClass().equals(double[].class) && ((double[])viewProp).length == 2)
      {
         double[] viewAr = (double[]) viewProp;
         azimuth = Utilities.restrictAngle(viewAr[0]);
         if(azimuth > 180) { azimuth -= 360; }
         azimuth = -azimuth + 180;
         if(azimuth > 180) azimuth -= 360;
         
         elev = Utilities.restrictAngle(viewAr[1]);
         if(elev > 180) { elev -= 360; }
         elev = Utilities.rangeRestrict(-60, 60, elev);
      }
      g3.setRotate(azimuth);
      g3.setElevate(elev);
      
      // HG "Position" and "Units" properties determine a rectangle [left bottom width height] bounding the projection
      // the 3D box, WRT the parent figure. Typically, "Units" defaults to "normalized", so we need the figure's 
      // dimensions in order to calculate this rectangle in real units (% units not allowed in DataNav FypML).
      Object posProp = hgAxes.getProperty("Position");
      if(posProp == null || !posProp.getClass().equals(double[].class))
         throw new NeverOccursException("Handle Graphics 'axes' object missing 'Position' property");
      double[] pos = (double[]) posProp;
      if(pos.length < 4 || pos[2] <= 0 || pos[3] <= 0) 
         throw new NeverOccursException("Handle Graphics 'Position' is invalid for 'axes' object");
      
      double scaleFac = 0;  // for special case: normalized units
      Object unitsProp = hgAxes.getProperty("Units");
      if(unitsProp != null && unitsProp.getClass().equals(String.class))
      {
         String units = (String) unitsProp;
         switch(units)
         {
         case "normalized":
            scaleFac = 0;
            break;
         case "inches":
            scaleFac = 1.0;
            break;
         case "centimeters":
            scaleFac = Measure.MM2IN * 10.0;
            break;
         default:
            // "pixels" and "characters" are not supported and map arbitrarily to "points"
            scaleFac = Measure.PT2IN;
            break;
         }
      }
      if(scaleFac == 0)
      {
         // convert normalized values to inches WRT parent figure viewport
         pos[0] = fig.getWidthMI() * pos[0] / 1000.0;
         pos[1] = fig.getHeightMI() * pos[1] / 1000.0;
         pos[2] = fig.getWidthMI() * pos[2] / 1000.0;
         pos[3] = fig.getHeightMI() * pos[3] / 1000.0;
      }
      else for(int i=0; i<pos.length; i++) pos[i] *= scaleFac;
      
      // get plot box aspect ratio [px py pz], then rearrange as [1 py/px pz/px]. If implicit, then use [1 1 h/w], where
      // h and w are specified by 'Position'
      double[] aspect = getAxesAspectRatio(hgAxes);
      if(aspect == null) aspect = new double[] {1, 1, pos[3]/pos[2]};
      else 
      {
         aspect[1] /= aspect[0];
         aspect[2] /= aspect[0];
         aspect[0] = 1;
      }
      
      // in FypML, the 3D graph's dimension are specified in 3D space. Using the aspect ratio, we can specify the Y- and
      // Z-extents (height and depth) in terms of the X-extent, or width. Here we start at the minimum extent of 1-in
      // and increase it in 0.1-in increments until the 3D graph's plot box width or height matches or exceeds that of 
      // the Matlab axes. The calculations are the same regardless the backdrop style or the coordinate ranges along 
      // the axes.
      double wInches = Math.max(1.0, 1.0 / Math.min(aspect[1], aspect[2])) - 0.1;
      Rectangle2D rPlotBox = new Rectangle2D.Double();
      int n = 0;
      do
      {
         wInches += 0.1;
         ++n;
         g3.setWidth(new Measure(Utilities.limitFracDigits(wInches, 3), Measure.Unit.IN));
         g3.setHeight(new Measure(Utilities.limitFracDigits(wInches*aspect[1], 3), Measure.Unit.IN));
         g3.setDepth(new Measure(Utilities.limitFracDigits(wInches*aspect[2], 3), Measure.Unit.IN));
         g3.getPlotBoxBounds(rPlotBox);
      } while((pos[2]*1000 - rPlotBox.getWidth() > 0) && (pos[3]*1000 - rPlotBox.getHeight() > 0) && n < 100);
      
      // now set the graph's (X,Y) to the center point of the rectangle specified by the 'Position' property
      g3.setX(new Measure(Utilities.limitFracDigits(pos[0] + pos[2]/2, 3), Measure.Unit.IN));
      g3.setY(new Measure(Utilities.limitFracDigits(pos[1] + pos[3]/2, 3), Measure.Unit.IN));

      // set backdrop style. Not all FypML backdrop styles are supported in Matlab.
      Graph3DNode.BackDrop backDrop;
      if("off".equals(hgAxes.getProperty("Visible"))) 
         backDrop = Graph3DNode.BackDrop.HIDDEN;
      else if("on".equals(hgAxes.getProperty("Box")))
      {
         if("full".equals(hgAxes.getProperty("BoxStyle"))) backDrop = Graph3DNode.BackDrop.BOX3D;
         else backDrop = Graph3DNode.BackDrop.OPENBOX3D;
      }
      else
      {
         backDrop = Graph3DNode.BackDrop.OPENBOX3D;
         boolean noMarks = "none".equals(hgAxes.getProperty("Color"));
         for(int i=0; i<=2 && noMarks; i++)
         {
            Object prop = hgAxes.getProperty(i==0 ? "XGrid" : (i==1 ? "YGrid" : "ZGrid"));
            noMarks = prop==null || "off".equals(prop);
         }
         if(noMarks) backDrop = Graph3DNode.BackDrop.AXESOUTER;
      }
      g3.setBackDrop(backDrop);
      
      // in the Matlab plot, all 3 backplanes have the same color, as specified by the 'Color' property
      Object colorSpec = hgAxes.getProperty("Color");
      Color c = processColorSpec(colorSpec);
      if(c == null)
      {
         // special cases: default is white, while "none" selects a transparent color (zero alpha)
         if("none".equals(colorSpec)) c = new Color(255, 255, 255, 0);
         else c = new Color(255, 255, 255);
      }
      g3.getBackPlane(Graph3DNode.Side.XY).setFillColor(c);
      g3.getBackPlane(Graph3DNode.Side.YZ).setFillColor(c);
      g3.getBackPlane(Graph3DNode.Side.XZ).setFillColor(c);
      
      // HG "GridLineStyle, GridColor, XGrid, YGrid, ZGrid" properties affect the graph's grid-line nodes. The 'none' 
      // line style is achieved by setting the stroke width to 0 on all 3 grid-line nodes. Similarly for any grid that's
      // turned 'off'. Note that the default 'GridColor' is a light gray.
      StrokePattern patn = processLineStyle(hgAxes);
      Color gridC = processColorSpec(hgAxes.getProperty("GridColor"));
      if(gridC == null) gridC = new Color(0.15f, 0.15f, 0.15f);
      Measure zeroSW = new Measure(0, Measure.Unit.IN);
      for(int i=0; i<=2; i++)
      {
         String propName = i==0 ? "XGrid" : (i==1 ? "YGrid":"ZGrid");
         Graph3DNode.Axis axis = i==0 ? Graph3DNode.Axis.X : (i==1 ? Graph3DNode.Axis.Y : Graph3DNode.Axis.Z);
         Object gridProp = hgAxes.getProperty(propName);
         if(patn==null || !"on".equals(gridProp)) g3.getGrid3DNode(axis).setMeasuredStrokeWidth(zeroSW);
         g3.getGrid3DNode(axis).setStrokePattern(patn!=null ? patn : StrokePattern.SOLID);
         g3.getGrid3DNode(axis).setStrokeColor(gridC);
      }      
      
      // HG "TickDir" determines orientation of tick marks for the first TickSetNode on each axis. 
      Orientation ori = ("out".equals(hgAxes.getProperty("TickDir"))) ? Orientation.OUT : Orientation.IN;
     
      // HG "TickLength", if present, specifies tick length as a fraction of the longest axis. Note that property is a 
      // two-element double array [2DLength 3DLength]; for 3D view, we use 3DLength. Default value is 0.025 x axisLen.
      double tickLenMI = 0.025;
      Object tlProp = hgAxes.getProperty("TickLength");
      if(tlProp != null && tlProp.getClass().equals(double[].class))
      {
         double[] tlen = (double[]) tlProp;
         if(tlen.length == 2 && tlen[1] >= 0 && tlen[1] <= 1.0) tickLenMI = tlen[1];
      }
      tickLenMI = tickLenMI * Math.max(g3.getWidth().toMilliInches(), 
            Math.max(g3.getHeight().toMilliInches(), g3.getDepth().toMilliInches()));
      Measure mTickLen = new Measure(Utilities.limitFracDigits(tickLenMI/1000, 3), Measure.Unit.IN);
      
      // set up the three graph axes and their tick marks...
      for(int i=0; i<=2; i++)
      {
         Graph3DNode.Axis axis = i==0 ? Graph3DNode.Axis.X : (i==1 ? Graph3DNode.Axis.Y : Graph3DNode.Axis.Z);
         
         // HG "X/Y/ZColor" property sets axis stroke and fill color
         c = processColorSpec(hgAxes.getProperty(i==0 ? "XColor" : (i==1 ? "YColor":"ZColor")));
         if(c != null)
         {
            g3.getAxis(axis).setStrokeColor(c);
            g3.getAxis(axis).setFillColor(c);
         }
         
         // in Matlab, the 3 axes are rendered along the edge of the 3D graph box -- so explicitly set the FypML
         // axis property "spacer" to 0in
         g3.getAxis(axis).setSpacer(new Measure(0, Measure.Unit.IN));
         
         // HG "X/Y/ZLim" property specifies axis range, while "X/Y/ZScale" determines if axis is log or linear. Note 
         // that, unlike 2D case, we ignore "X/Y/ZDir". Axis range must always be increasing for FypML 3D graph. Also, 
         // no auto-ranging feature supported currently for 3D. If we cannot get access to the Matlab axis range, then
         // we assume [0..100].
         boolean isLog = "log".equals(hgAxes.getProperty(i==0 ? "XScale" : (i==1 ? "YScale":"ZScale")));
         double rngMin = isLog ? 0.01 : 0;
         double rngMax = 100;
         Object rngProp = hgAxes.getProperty(i==0 ? "XLim" : (i==1 ? "YLim":"ZLim"));
         if(rngProp != null && rngProp.getClass().equals(double[].class))
         {
            double[] range = (double[]) rngProp;
            if(range.length == 2 && Utilities.isWellDefined(range))
            {
               rngMin = Math.min(range[0], range[1]);
               rngMax = Math.max(range[0], range[1]);
            }
         }
         g3.getAxis(axis).setRange(rngMin, rngMax, isLog);
         
         // Axis labels. Matlab defines these as "text" children of the HG "axes" object. We ignore all properties of 
         // the "text" object except the text string itself. Note that the default FypML 3D graph axis node gets a 
         // non-empty label, so we must clear this if no axis label is specified for the corresponding Matlab axis. The 
         // axis labels could have TeX-encoded special characters, which we translate if possible.
         String label = (i==0) ? hgAxes.getXLabel() : (i==1 ? hgAxes.getYLabel() : hgAxes.getZLabel());
         g3.getAxis(axis).setTitle(label != null ? processSpecialCharacters(label) : "");

         // set tick mark orientation and length on axis's first tick set node
         Ticks3DNode ticks = (Ticks3DNode) g3.getAxis(axis).getChildAt(0);
         ticks.setTickOrientation(ori);
         ticks.setTickLength(mTickLen);
         
         // HG "X/Y/ZTick" property, if present, is a vector specifying the locations of tick marks along the axis. If 
         // the property is explicitly set to an empty vector (null), then the corresponding FypML 3D graph axis will
         // have not tick marks. Otherwise we use the vector to determine the start, end, and tick interval for the 
         // first set of tick marks. 
         //
         // Tick mark labels. If the "X/Y/ZTickLabelMode" property == 'manual', and the "X/Y/ZTickLabel" property is a 
         // String[], then the string array is interpreted as a set of user-defined tick mark labels. These are applied 
         // to the first tick set, if there is one. If "X/Y/ZTickLabel" is a simple non-empty string rather than a 
         // string array, then each character in the string is considered a separate tick label (e.g., "ABCD").
         // NOTE: Matlab 2014b apparently sets "X/Y/ZTickLabelsMode" to 'manual' instead of "X/Y/ZTickLabelMode". So 
         // we have to check both properties.
         //
         // Log axis. If the axis is logarithmic, then the tick interval should be 10, 100, etc. Furthermore, perLogIntv
         // is set to [1] to ensure tick marks at the powers of 10.
         //
         // If "X/Y/ZMinorTick" is 'on' and the first tick set ("major" ticks) was not removed, then we add a second 
         // tick set to render the minor tick marks. Their length is set to 1/2 that of the major tick marks, and their
         // labels are turned off. When the axis is linear, this second tick set is configured so that the minor ticks
         // sub-divide a single major tick interval into 10, 5 or 2 equal parts, depending on whether the # of major 
         // tick intervals is 2 or less, 3-5, or more than 5, respectively. When the axis is logarithmic, the range and
         // interval matches that of the major tick mark set, but perLogIntv is set to [2 3 4 ... 9].
         //
         String tickPropName = (i==0) ? "XTick" : (i==1 ? "YTick":"ZTick");
         double start = 0;
         double end = 0;
         double intv = 0;
         boolean removeTickSet = false;
         if(hgAxes.hasProperty(tickPropName))
         {
            Object tickProp = hgAxes.getProperty(tickPropName);
            if(tickProp == null || !tickProp.getClass().equals(double[].class))
               removeTickSet = true;
            else
            {
               double[] markLocs = (double[]) tickProp;
               if(markLocs.length == 0) removeTickSet = true;
               else if(markLocs.length == 1)
               {
                  start = end = markLocs[0];
                  intv = isLog ? 10 : 1;
               }
               else
               {
                  start = markLocs[0];
                  end = markLocs[markLocs.length-1];
                  if(!isLog)
                     intv = (end-start) / (markLocs.length - 1);
                  else
                     intv = chooseLogTickInterval(start, end, markLocs.length-1);
               }
            }
         }
         
         if(removeTickSet) 
            g3.getAxis(axis).getGraphicModel().deleteNode(ticks);
         else
         {
            if(intv == 0)
            {
               start = g3.getAxis(axis).getRangeMin();
               end = g3.getAxis(axis).getRangeMax();
               if(!isLog)
               {
                  intv = (end - start) / 4.0;
                  if(intv==0) intv = 1;
               }
               else
                  intv = chooseLogTickInterval(start, end, 4);
            }
            
            ticks.setRangeAndInterval(Utilities.limitSignificantDigits(start, 6), 
                  Utilities.limitSignificantDigits(end, 6), Utilities.limitSignificantDigits(intv, 6));
            ticks.setDecadeTicks(new LogTickPattern(0x02));
            
            // choose floating-pt tick label format based on the absolute axis range
            LabelFormat tickFmt = LabelFormat.INT;
            double rng = Math.abs(end-start);
            if(rng <= 0.1) tickFmt = LabelFormat.F3;
            else if(rng <= 1) tickFmt = LabelFormat.F2;
            else if(rng <= 10) tickFmt = LabelFormat.F1;
            ticks.setTickLabelFormat(tickFmt);
            
            // handle possible custom tick mark labels
            Object prop = hgAxes.getProperty((i==0) ? "XTickLabelMode" : (i==1 ? "YTickLabelMode":"ZTickLabelMode"));
            if(prop == null) 
               prop = hgAxes.getProperty((i==0) ? "XTickLabelsMode" : (i==1 ? "YTickLabelsMode": "ZTickLabelsMode"));
            if("manual".equals(prop))
            {
               prop = hgAxes.getProperty((i==0) ? "XTickLabel" : (i==1 ? "YTickLabel":"ZTickLabel"));
               if(prop != null && prop.getClass().equals(String[].class) && ((String[])prop).length > 0)
               {
                  String[] tLabels = (String[]) prop;
                  for(int j=0; j<tLabels.length; j++) tLabels[j] = processSpecialCharacters(tLabels[j]);
                  ticks.setCustomTickLabels(tLabels);
               }
               else if(prop != null && prop.getClass().equals(String.class) && !((String) prop).isEmpty())
               {
                  String labelChars = (String) prop;
                  labelChars = processSpecialCharacters(labelChars);
                  String[] labels = new String[labelChars.length()];
                  for(int j=0; j<labels.length; j++) labels[j] = labelChars.substring(j, j+1);
                  ticks.setCustomTickLabels(labels);
               }
            }
            
            // check for Matlab minor tick marks: "X/Y/ZMinorTick" == 'on' (default is 'off'). If so, we add a second 
            // tick set with tick length half that of the major ticks, and no labels. 
            prop = hgAxes.getProperty((i==0) ? "XMinorTick" : (i==1 ? "YMinorTick":"ZMinorTick"));
            if("on".equals(prop)) addMinorTicksToAxis(g3.getAxis(axis));
         }
      }

      // set the axis range for the FypML 3D graph's color bar IAW the "CLim" property
      Object cLim = hgAxes.getProperty("CLim");
      if(cLim != null && cLim.getClass().equals(double[].class))
      {
         double[] range = (double[]) cLim;
         if(range.length == 2 && Utilities.isWellDefined(range))
         {
            g3.getColorBar().setStart(Math.min(range[0], range[1]));
            g3.getColorBar().setEnd(Math.max(range[0], range[1]));
         }
      }

      // axes title. Matlab also defines this as a "text" child, like axis labels. If the text object exists and has a 
      // non-null "String" property, we use that string as the 3D graph's non-rendered title. In the next section, a 
      // text label is added to the graph to render the title as it appears in the Matlab figure.
      String title = hgAxes.getAxesTitle();
      if(title != null) g3.setTitle(processSpecialCharacters(title));
      
      // convert each supported HG plot object child or "text" child to the appropriate figure graphics node. However, 
      // those "text" objects representing the axis labels are skipped, as are any "text" objects that have a null or 
      // empty string for their "String" property.
      for(int i=0; i<hgAxes.getChildCount(); i++)
      {
         HGObject hgChild = hgAxes.getChildAt(i);
         if(hgChild.getType().equals("specgraph.scattergroup"))
            addScatterGroupToGraph(g3, hgChild, false);
         else if(hgChild.getType().equals("specgraph.stemseries") || hgChild.getType().equals("graph2d.lineseries"))
            addStemOrLinePlotToGraph(g3, hgChild);
         else if(hgChild.getType().equals("graph3d.surfaceplot"))
            add3DSurfaceToGraph(g3, hgChild);
         else if(hgChild.getType().equals("surface") && hgChild.is3DBarPlot())
            add3DBarPlotToGraph(g3, hgChild);
         else if(hgChild.getType().equals("text") && !hgAxes.isAxesLabelHandle(hgChild.getHandle()))
            addTextLabelToGraph(g3, hgAxes, hgChild, hgAxes.isAxesTitleHandle(hgChild.getHandle()));
      }
      // fixLabelLocationsInGraph(g3);  
      
      // if a HG "scribe.legend" object is attached to "axes", then configure the graph's legend accordingly.
      configureGraphLegend(g3, hgAxes, hgFig);
      
      // if a HG "scribe.colorbar" object is attached to "axes", then configure the 3D graph's colorbar accordingly.
      configureGraphColorBar(g3, hgAxes, hgFig);
   }

   /** 
    * Check the Matlab Handle Graphics figure hierarchy to see if there is a HG "scribe.legend" object attached to the 
    * specified HG "axes". If so, turn on the automated legend for the corresponding FypML 2D or 3D graph container.
    * While we cannot faithfully reproduce the legend as it appears in the Matlab figure, we do process some of the
    * Matlab legend's properties:
    * <ul>
    * <li>HG "FontName", "FontSize", "FontUnits" (R2014a or earlier), "FontAngle", "FontWeight": These are processed in
    * order to set the FypML legend's font properties.</li>
    * <li>HG "TextColor" : Determines the text color for the FypML legend.</li>
    * <li>HG "Position", "Units", "NumColumns", "String": These are processed in order to set the FypML legend's 
    * location WRT the parent graph, the vertical separation between legend entries, and the approximate width of each
    * entry's trace line segment. FypML only supports a single-column, vertically oriented legend, however.</li>
    * <li>HG "Box", "Color", "LineWidth": If "Box=on" (default), then the FypML legend is configured to render a
    * bounding box, where "Color" is the box background color and "LineWidth" is the outline stroke width. Note that
    * "EdgeColor" is ignored; FypML always uses the "TextColor" for the box outline.</li>
    * </ul>
    * 
    * <p>The "scribe.legend" is identified by the undocumented property "LegendPeerHandle" in the "axes" object. The
    * method checks the children of the parent HG "figure" for a "scribe.legend" with that handle. A new Handle Graphics
    * object representing a polar plot, "polaraxes", was introduced in Matlab R2016a. Like the "axes" object, it can 
    * have an associated "scribe.legend", but the HG legend object's handle is stored in the undocumented property 
    * "LayoutPeers", a double-valued vector which may also hold the handle of a "scribe.colorbar" associated with the 
    * polar axes object.</p>
    * 
    * @param graph The FypML graph container. It may be any 2D or 3D graph.
    * @param hgAxes The Matlab HG "axes" or "polaraxes" object from which the graph node was constructed.
    * @param hgFig The Matlab HG figure container.
    */
   private static void configureGraphLegend(FGNGraph graph, HGObject hgAxes, HGObject hgFig)
   {
      // find the "scribe.legend" object associated with the "axes" or "polaraxes" -- if there is one
      HGObject hgLegend = null;
      
      // for an "axes", the attached legend's handle may be in 'LegendPeerHandle' property. If we get a handle,
      // search the parent figure's child list for a "scribe.legend" object with that handle.
      if(hgAxes.getType().equals("axes"))
      {
         Object prop = hgAxes.getProperty("LegendPeerHandle");
         if(prop != null && prop.getClass().equals(Double.class))
         {
            double legendHandle = (Double) prop;
            for(int i=0;  i<hgFig.getChildCount(); i++)
            {
               HGObject hgChild = hgFig.getChildAt(i);
               if(hgChild.getHandle() == legendHandle && hgChild.getType().equals("scribe.legend"))
               {
                  hgLegend = hgChild;
                  break;
               }
            }
         }
      }

      // as of R2016a (I think), the legend handle is in the vector property 'LayoutPeers', which could also have the
      // handle of an attached color bar. This is how things work for the 'polaraxes' object introduced in R2016a.
      if(hgLegend == null)
      {
         double[] layoutPeers = HGObject.getDoubleVectorFromProperty(hgAxes.getProperty("LayoutPeers"));
         if(layoutPeers != null)
         {
            for(int i=0; i<hgFig.getChildCount() && hgLegend == null; i++)
            {
               HGObject hgChild = hgFig.getChildAt(i);
               if(hgChild.getType().equals("scribe.legend"))
               {
                  for(double layoutPeer : layoutPeers)
                     if(layoutPeer == hgChild.getHandle())
                     {
                        hgLegend = hgChild;
                        break;
                     }
               }
            }
         }
      }

      // if we did not find the associated "scribe.legend", there's nothing more to do
      if(hgLegend == null) return;
      
      // found the "scribe.legend", so turn on the FypML graph's automated legend
      LegendNode legend = graph.getLegend();
      legend.setHide(false);
      
      // set its font properties. If font size is implicit, it is set to 90% of the parent axes' font size.
      setFontProperties(legend, hgLegend);
      if(null == hgLegend.getProperty("FontSize")) 
         legend.setFontSizeInPoints((int) Math.round(0.9*graph.getFontSizeInPoints().doubleValue()));
      
      // set FypML legend's text color
      Color c = processColorSpec(hgLegend.getProperty("TextColor"));
      if(c != null) legend.setFillColor(c);
      
      // HG "Position", "Units", "NumColumns", "String": Analyze these Matlab legend properties to determine the 
      // location for the FypML legend, vertical separation between entries, and entry line length....
      
      // first, find Matlab legend's bounding box in inches WRT BL corner of figure
      // NOTE: The "Position" appears to be a little off in the sense that what is saved in the FIG file does NOT match 
      // the value of "Position" when you look at the figure in Matlab itself.
      double[] pos = null;
      Object prop = hgLegend.getProperty("Position");
      if(prop != null && prop.getClass().equals(double[].class))
      {
         pos = (double[]) prop;
         if(pos.length < 4) pos = null;
         else
         {
            double scaleFac = 0;  // for special case: normalized units
            Object unitsProp = hgLegend.getProperty("Units");
            if(unitsProp != null && unitsProp.getClass().equals(String.class))
            {
               String units = (String) unitsProp;
               switch(units)
               {
               case "normalized":
                  scaleFac = 0;
                  break;
               case "inches":
                  scaleFac = 1.0;
                  break;
               case "centimeters":
                  scaleFac = Measure.MM2IN * 10.0;
                  break;
               default:
                  // "pixels" and "characters" are not supported and map arbitrarily to "points"
                  scaleFac = Measure.PT2IN;
                  break;
               }
            }
            if(scaleFac == 0)
            {
               // convert normalized values to inches WRT parent figure viewport
               FigureNode fig = (FigureNode) graph.getParent();
               pos[0] = fig.getWidthMI() * pos[0] / 1000.0;
               pos[1] = fig.getHeightMI() * pos[1] / 1000.0;
               pos[2] = fig.getWidthMI() * pos[2] / 1000.0;
               pos[3] = fig.getHeightMI() * pos[3] / 1000.0;
              
            }
            else for(int i=0; i<pos.length; i++) pos[i] *= scaleFac;
         }
      }

      // number of columns in Matlab legend
      int nCols = 1;
      double[] res = HGObject.getDoubleVectorFromProperty(hgLegend.getProperty("NumColumns"));
      if(res != null && res.length == 1 && res[0] >= 1) nCols = (int) res[0];
      
      // the legend entry labels
      String[] labels = null;
      prop = hgLegend.getProperty("String");
      if(prop != null)
      {
         if(prop.getClass().equals(String[].class)) labels = (String[]) prop;
         else if(prop.getClass().equals(String.class)) 
         {
            labels = new String[1];
            labels[0] = (String) prop;
         }
      }
      
      // vertical separation between labels: height of Matlab legend / num entries per column (default=fontSz)
      // gap between entry line segment and accompanying label: 0.33*fontSize
      // entry line width: average column width - width of longest label (default = 0.4in, min 0.2in)
      double vGapPts = legend.getFontSizeInPoints();
      double labelOfsPts = vGapPts*0.33;
      double entryLenInches = 0.4;
      if(pos != null && labels != null && labels.length > 0)
      {
         int n = labels.length;
         if(nCols > 1) n = (n%nCols == 0) ? n/nCols : (n/nCols + 1);
         vGapPts = 72.0 * pos[3]/n;
         
         double colW = pos[2] / nCols;
         String longest = "";
         for(String s : labels) if(s.length() > longest.length()) longest = s;
         double maxLabelLen = Math.round(longest.length() * legend.getFontSizeInPoints() * 0.6);
         maxLabelLen /= 72.0;
         entryLenInches = Math.max(0.2, colW - maxLabelLen);
      }
      legend.setSpacer(new Measure(Utilities.limitFracDigits(vGapPts, 1), Measure.Unit.PT));
      legend.setLabelOffset(new Measure(Utilities.limitFracDigits(labelOfsPts, 1), Measure.Unit.PT));
      legend.setLength(new Measure(Utilities.limitFracDigits(entryLenInches, 2), Measure.Unit.IN));
      
      // set FypML legend's X,Y location. Default is X=105%, Y=75% 
      double xPct = 105, yPct = 75;
      if(pos != null)
      {
         // transform the local bounding box of the parent graph (children are positioned WRT to the BL corner of this
         // box) to global figure coordinates -- in milli-inches
         Rectangle2D r = graph.getBoundingBoxLocal();
         r = graph.getLocalToGlobalTransform().createTransformedShape(r).getBounds2D();
         
         // the FypML legend's BL corner corresponds to the left endpoint of the bottom-most legend entry, but for the
         // the Matlab legend, its the BL corner of the legend's bounding box. Adjust.
         pos[0] += 0.33 * legend.getFontSizeInPoints() / 72.0;
         pos[1] += 0.5 * vGapPts / 72.0;
         
         // BL corner of legend WRT BL corner of graph bounding box, as % of graph dim (coords could be negative!)
         xPct = (pos[0]*1000.0 - r.getX()) / r.getWidth();
         xPct *= 100.0;
         yPct = (pos[1]*1000.0 - r.getY()) / r.getHeight();
         yPct *= 100.0;
      }
      legend.setXY(new Measure(Utilities.limitFracDigits(xPct, 1), Measure.Unit.PCT),
            new Measure(Utilities.limitFracDigits(yPct, 1), Measure.Unit.PCT));
      
      // HG "Box", "Color" and "LineWidth": Governs "boxed" appearance of legend.
      prop = hgLegend.getProperty("Box");
      if(prop == null || "on".equals(prop))
      {
         c = processColorSpec(hgLegend.getProperty("Color"));
         if(c == null) c = Color.WHITE;
         legend.setBoxColor(c);
         
         legend.setBorderWidth(processLineWidth(hgLegend));
      }
   }
   
   /** 
    * Check the Matlab Handle Graphics figure hierarchy to see if there is a HG "scribe.colorbar" object attached to the 
    * specified HG "axes". If so, show the colorbar for the corresponding FypML graph container and configure it IAW
    * selected properties of the "scribe.colorbar" object. We respect the "scribe.colorbar" properties listed below. 
    * <i><b>NOTE</b>. Prior to Matlab release 2014b, the colorbar was a specialized 'axes' object and had all the same 
    * properties that an 'axes' had, even though many of them made little sense. Starting with 2014b, "scribe.colorbar" 
    * is a unique object class, with far fewer properties than an 'axes'. Unless otherwise stated, the properties listed
    * below apply both pre- and post-2014b.</i> 
    * <ul>
    * <li>Use "Location" property to decide the graph edge to which the colorbar is adjacent. <i><b>Pre-2014b</b> - 
    * String values for "Location" property have upper and lower case letters, e.g., "NorthOutside"; <b>2014b</b> - same
    * string values, except all letters are lower case, e.g., "northoutide".</i></li>
    * <li>Examine the "Position" and "Units" property of the colorbar to calculate the color bar width and the size of
    * the gap separating the color bar from the adjacent edge of the graph data box. If unable to do this calculation,
    * assume a gap of 0.3in and a bar width of 0.2in.</li>
    * <li>Use "FontName" and other font properties to set font-related properties of colorbar. <i><b>2014b</b> - The 
    * "FontUnits" property no longer is used; the font units are always typographical points.</i></li>
    * <li>Use "XColor" or "YColor" (depending on orientation of colorbar) to set the stroke and fill color.
    * <i><b>2014b</b> - Use "Color" instead.</i></li>
    * <li>Use "LineWidth" to set stroke width of colorbar.</li>
    * <li>"TickDir" and "TickLength" affect direction and length of major tick mark set. <i><b>2014b</b> - Use 
    * "TickDirection" instead of "TickDir" (options are the same, "in" or "out"). Also, "TickLength" is a scalar rather
    * than a two-element array.</i></li>
    * <li>Use "XTick" or "YTick" (depending on orientation) to set major tick mark locations along colorbar. 
    * <i><b>2014b</b> - Use "Ticks" instead.</i></li>
    * <li>If "Visible" == "off", then leave the colorbar hidden.</li>
    * <li>If the "scribe.colorbar" <b>(2014b and later)</b> has the "Limits" property, a vector containing
    * the "minimum and maximum" tick marks, we may use this to set the axis range of the FypML colorbar -- but only if
    * the Matlab "axes" peer lacks an explicit "CLim" property. In this scenario, if the parent graph supports axis 
    * auto-scaling, then auto-scaling must be disabled for the colorbar (pertains to {@link GraphNode} only).</li>
    * <li>The gap separating the axis line from the color bar itself is set to 0in, since there's no such separation in
    * a Matlab colorbar.</li>
    * <li>There are no obvious Matlab colorbar properties that specify the tick label gap and the axis label offset for
    * a FypML color bar, so we use the following defaults. Let F = the font size in points. For horizontal color bars, 
    * the tick label gap is set to 0.5*F, and the axis label offset is 1.5. For vertical color bars, the tick label gap 
    * is set to 0.33*F. The axis label offset = tick label gap + 0.6*N*F + D, where N is the number of characters in the 
    * longest tick label, and D = tick length (in points) if the ticks are directed outward, or 0 if inward.</li>
    * <li>If the "scribe.colorbar" has a non-empty X- or Y-axis label (again, use X-axis label for a horizontal colorbar
    * along top or bottom edge of graph; Y-axis label for a vertical one long L or R edge), set that as the colorbar's 
    * title -- which serves as the axis label. <i><b>2014b</b> - The new 'Label' property is a text object in which the 
    * label string appears in that object's 'String' property. In 2014b and up, the "scribe.colorbar" object has no 
    * children. If the HG tree was read from a FIG file, our FIG file-parsing code does not provide access to the 
    * 'Label' property and so we cannot reproduce a colorbar label. However, we have implemented two workarounds:
    * (1) If the HG tree was prepared in the Matlab environment via <i>matfig2fyp()</i>, that function looks for 
    * colorbar.Label.String and puts its value (a Java String) in an HGObject property called "Label_Str". (2) If the 
    * FIG file is saved with the Matlab utility savefigasis(), that utility will store Label.String in the all-purpose 
    * 'UserData' field (unless that field is already in use).</i></li>
    * </ul>
    * 
    * <p>The "scribe.colorbar" is identified by the undocumented property "ColorbarPeerHandle" in the "axes" object. The
    * method checks the children of the parent HG "figure" for a "scribe.colorbar" with that handle. A new Handle 
    * Graphics object representing a polar plot, "polaraxes", was introduced in Matlab R2016a. With that release, the
    * colorbar peer handle for both 'axes' and 'polaraxes' are stored in the undocumented property "LayoutPeers", a 
    * double-valued vector which may also hold the handle of a "scribe.legend" associated with the axes.</p>
    * 
    * <p></p>
    * 
    * @param graph A FypML graph container; it can be any 2D or 3D FypML graph.
    * @param hgAxes The Matlab HG "axes" or "polaraxes" object from which the graph container was constructed.
    * @param hgFig The Matlab HG figure container.
    */
   private static void configureGraphColorBar(FGNGraph graph, HGObject hgAxes, HGObject hgFig)
   {
      // find the "scribe.colobar" object associated with the "axes" or "polaraxes" -- if there is one
      HGObject hgColorBar = null;
      
      // for an "axes", the attached colorbar's handle may be in 'ColorbarPeerHandle' property. If we get a handle,
      // search the parent figure's child list for a "scribe.colorbar" object with that handle.
      if(hgAxes.getType().equals("axes"))
      {
         Object prop = hgAxes.getProperty("ColorbarPeerHandle");
         if(prop != null && prop.getClass().equals(Double.class))
         {
            double cbHandle = (Double) prop;
            for(int i=0;  i<hgFig.getChildCount(); i++)
            {
               HGObject hgChild = hgFig.getChildAt(i);
               if(hgChild.getHandle() == cbHandle && hgChild.getType().equals("scribe.colorbar"))
               {
                  hgColorBar = hgChild;
                  break;
               }
            }
         }
      }

      // as of R2016a (I think), the colorbar handle is in the vector property 'LayoutPeers', which could also have the
      // handle of an attached legend. This is how things work for the 'polaraxes' object introduced in R2016a.
      if(hgColorBar == null)
      {
         double[] layoutPeers = HGObject.getDoubleVectorFromProperty(hgAxes.getProperty("LayoutPeers"));
         if(layoutPeers != null)
         {
            for(int i=0; i<hgFig.getChildCount() && hgColorBar == null; i++)
            {
               HGObject hgChild = hgFig.getChildAt(i);
               if(hgChild.getType().equals("scribe.colorbar"))
               {
                  for(double layoutPeer : layoutPeers)
                     if(layoutPeer == hgChild.getHandle())
                     {
                        hgColorBar = hgChild;
                        break;
                     }
               }
            }
         }
      }

      // if we did not find the associated "scribe.colorbar", there's nothing more to do.
      if(hgColorBar == null) return;
      
      ColorBarNode colorBar = graph.getColorBar();
      
      // HG "Visible" property (default = "on") determines whether or not we show the graph's colorbar
      Object prop = hgColorBar.getProperty("Visible");
      boolean hide = (prop!=null) && prop.getClass().equals(String.class) && "off".equals(prop);
      colorBar.setHide(hide);
      
      // HG "Location" property helps determine to which graph edge the colorbar is adjacent. (We ignore case b/c
      // the possible values were changed to use all lowercase letters in 2014b.) If that property is not specified or 
      // is not recognized, use the graph's layout to select an edge. For 3D and polar plots, an "all quad" layout is
      // assumed and the bar is placed along the right edge.
      ColorBarNode.Edge edge = ColorBarNode.Edge.RIGHT;
      prop = hgColorBar.getProperty("Location");
      if(prop != null && prop.getClass().equals(String.class))
      {
         String loc = (String) prop;
         if(loc.equalsIgnoreCase("North") || loc.equalsIgnoreCase("NorthOutside"))
            edge = ColorBarNode.Edge.TOP;
         else if(loc.equalsIgnoreCase("South") || loc.equalsIgnoreCase("SouthOutside"))
            edge= ColorBarNode.Edge.BOTTOM;
         else if(loc.equalsIgnoreCase("West") || loc.equalsIgnoreCase("WestOutside"))
            edge = ColorBarNode.Edge.LEFT;
      }
      colorBar.setEdge(edge);
      boolean isHorizBar = (edge==ColorBarNode.Edge.BOTTOM || edge==ColorBarNode.Edge.TOP);
      
      // HG "Position" and "Units" properties locates the color bar object within the Matlab figure. We can use that
      // information, along with the parent graph dimensions, to calculate the gap between the color bar and the 
      // adjacent graph edge, as well as the thickness of the gradient bar. If "Position" is not available or invalid, 
      // assume a gap of 0.2in and a bar size of 0.2in.
      Object posProp = hgColorBar.getProperty("Position");
      double gap = 0.2, barSz = 0.2;
      if(posProp != null && posProp.getClass().equals(double[].class))
      {
         double[] pos = (double[]) posProp;
         if(pos.length == 4 && pos[2] > 0 && pos[3] > 0) 
         {
            // calculate position of color bar in inches 
            double scaleFac = 0;  // for special case: normalized units
            Object unitsProp = hgColorBar.getProperty("Units");
            if(unitsProp != null && unitsProp.getClass().equals(String.class))
            {
               String units = (String) unitsProp;
               switch(units)
               {
               case "normalized":
                  scaleFac = 0;
                  break;
               case "inches":
                  scaleFac = 1.0;
                  break;
               case "centimeters":
                  scaleFac = Measure.MM2IN * 10.0;
                  break;
               default:
                  // "pixels" and "characters" are not supported and map arbitrarily to "points"
                  scaleFac = Measure.PT2IN;
                  break;
               }
            }
            if(scaleFac == 0)
            {
               FigureNode fig = (FigureNode) graph.getParent();
               pos[0] = fig.getWidthMI() * pos[0] / 1000.0;
               pos[1] = fig.getHeightMI() * pos[1] / 1000.0;
               pos[2] = fig.getWidthMI() * pos[2] / 1000.0;
               pos[3] = fig.getHeightMI() * pos[3] / 1000.0;
              
            }
            else for(int i=0; i<pos.length; i++) pos[i] *= scaleFac;
            
            // calculate graph-color bar gap and color bar thickness in inches. To do this, we get the local bounding
            // box for the graph and transform it to global coordinates (in milli-inches). This assumes that FC's 
            // definition of the bounding box is similar to the graph's original 'Position' property -- not so sure
            // when the graph is 3D. We can than compare this to the 'Position' of the color bar to calculate the gap;
            // the bar size is the width of a vertical bar or the height of a horizontal one.
            Rectangle2D r = graph.getBoundingBoxLocal();
            r = graph.getLocalToGlobalTransform().createTransformedShape(r).getBounds2D();
            
            switch(edge)
            {
            case RIGHT:
               gap = pos[0] - (r.getX() + r.getWidth())/1000.0;
               barSz = pos[2];
               break;
            case LEFT:
               gap = r.getX()/1000.0 - (pos[0]+pos[2]);
               barSz = pos[2];
               break;
            case BOTTOM:
               gap = r.getY()/1000.0 - (pos[1]+pos[3]);
               barSz = pos[3];
               break;
            case TOP:
               gap = pos[1] - (r.getY() + r.getHeight())/1000.0;
               barSz = pos[3];
               break;
            default:
               break;
            }
            
            // experience has shown that the 'Position' vector is not stored reliably, and that the details of 
            // positioning change with Matlab version, so the above calculations may yield bad values. If gap or bar
            // size is not within a reasonable range, use reasonable defaults.
            if(gap <= 0.05 || gap > 0.5) gap = 0.2;
            if(barSz <= 0.1 || barSz > 0.5) barSz = 0.2;
         }
      }
      colorBar.setGap(new Measure(gap, Measure.Unit.IN));
      colorBar.setBarSize(new Measure(barSz, Measure.Unit.IN));
      
      // handle font-related properties
      setFontProperties(colorBar, hgColorBar);

      // HG "LineWidth" property maps to the colorbar's "strokeWidth". Units=point. Default is 0.5pt.
      colorBar.setMeasuredStrokeWidth(processLineWidth(hgColorBar));

      // HG "TickDir" determines orientation of tick marks in the default TickSetNode for colorbar. In Matlab 2014b
      // and beyond, the relevant property is "TickDirection". The default (implicit) orientation is inward.
      prop = hgColorBar.getProperty("TickDir");
      if(prop == null) prop = hgColorBar.getProperty("TickDirection");
      Orientation ori = Orientation.IN;
      if(prop != null && prop.getClass().equals(String.class) && "out".equals(prop))
         ori = Orientation.OUT;
      TickSetNode ticks = (TickSetNode) colorBar.getChildAt(0);
      ticks.setTickOrientation(ori);
      
      // HG "TickLength", if present, specifies tick length. Prior to R2014b (when the colorbar was just a specialized
      // 'axes', it was a 2-element array [2DLength 3DLength]. We ignore 3DLength; 2DLength is the tick length as a 
      // fraction of the longer axis. For R2014b and later, it's a scalar. The implicit value is 0.01.
      double tickLenFrac = 0.01;
      double[] tlen = HGObject.getDoubleVectorFromProperty(hgColorBar.getProperty("TickLength"));
      if(tlen != null && tlen.length <= 2)
      {
         tickLenFrac = tlen[0];
         if(tickLenFrac < 0 || tickLenFrac >= 1) tickLenFrac = 0.01;
      }
      double tickLen = tickLenFrac * Math.max(graph.getWidth().toMilliInches(), graph.getHeight().toMilliInches());
      tickLen /= 1000.0;
      ticks.setTickLength(new Measure(Utilities.limitFracDigits(tickLen,2), Measure.Unit.IN));

      // There's no property that governs the gap between tick labels and the axis line (or end of tick mark). We set
      // this arbitrarily to 0.33*fontSize for vertical color bars and 0.5*fontSize for horizontal.
      int gapInPts = (int) Math.round(colorBar.getFontSizeInPoints() * (isHorizBar ? 0.5 : 0.33));
      ticks.setTickGap(new Measure(gapInPts, Measure.Unit.PT));
      
      // HG "XColor" or "YColor" (depending on orientation) property determines colorbar's stroke AND fill color, In
      // Matlab 2014b and beyond, the relevant property is "Color".
      String which = (edge == ColorBarNode.Edge.LEFT || edge==ColorBarNode.Edge.RIGHT) ? "YColor" : "XColor"; 
      prop = hgColorBar.getProperty(which);
      if(prop == null) prop = hgColorBar.getProperty("Color");
      Color c = processColorSpec(prop);
      if(c != null)
      {
         colorBar.setStrokeColor(c);
         colorBar.setFillColor(c);
      }

      // HG "Limits" property: In Matlab 2014b and later, this effectively contains the Z data range spanned by the 
      // colorbar. If present, then we ensure auto-scaling of the colorbar is disabled (for colorbars in GraphNode only)
      // and set the colorbar's range accordingly. However, we ignore "Limits" if the parent axes has an explicit "CLim"
      // property that sets the range.
      prop = hgColorBar.getProperty("Limits");
      Object cLim = hgAxes.getProperty("CLim");
      boolean cLimValid = (cLim != null) && cLim.getClass().equals(double[].class) &&
            (((double[])cLim).length == 2) && Utilities.isWellDefined((double[]) cLim);
      if(prop != null && prop.getClass().equals(double[].class) && !cLimValid)
      {
         double[] limits = (double[]) prop;
         if(limits.length == 2 && Utilities.isWellDefined(limits))
         {
            if(graph instanceof GraphNode)
            {
               GraphNode gn = (GraphNode) graph;
               GraphNode.Autorange rng = gn.getAutorangeAxes();
               gn.setAutorangeAxes(rng.isXAxisAutoranged(), rng.isYAxisAutoranged(), false);
            }
            colorBar.setStart(Math.min(limits[0], limits[1]));
            colorBar.setEnd(Math.max(limits[0], limits[1]));
         }
      }
      
      // HG "XTick" or "YTick" property (depending on colorbar orientation), if present, is a vector specifying the 
      // locations of tick marks along the colorbar axis. For Matlab 2014b and above, the relevant property is "Ticks". 
      // If the axis is configured for auto-range adjustment, the property is ignored UNLESS it is explicitly set to an 
      // empty vector (null), in which case the colorbar's single initial TickSetNode is deleted. Otherwise: if 
      // explicitly specified, we use the vector to determine the start, end, and tick interval for that single 
      // TickSetNode. If implicit, Matlab auto-generates the tick locations; in this case, we configure the TickSetNode 
      // to display 5 tick marks over the axis range. 
      double start = 0;
      double end = 0;
      double intv = 0;
      boolean removeTickSet = false;
      which = (edge == ColorBarNode.Edge.LEFT || edge==ColorBarNode.Edge.RIGHT)? "YTick" : "XTick"; 
      if(!hgColorBar.hasProperty(which)) which = "Ticks";
      
      if(hgColorBar.hasProperty(which))
      {
         Object tickProp = hgColorBar.getProperty(which);
         if(tickProp == null || !tickProp.getClass().equals(double[].class))
            removeTickSet = true;
         else
         {
            double[] markLocs = (double[]) tickProp;
            if(markLocs.length == 0) removeTickSet = true;
            else if(markLocs.length == 1)
            {
               start = end = markLocs[0];
               intv = 1;
            }
            else
            {
               start = markLocs[0];
               end = markLocs[markLocs.length-1];
               intv = (end-start) / (markLocs.length - 1);
            }
         }
      }

      if(removeTickSet)
         colorBar.getGraphicModel().deleteNode(ticks);
      else if(!colorBar.isAutoranged())
      {
         if(intv == 0)
         {
            start = colorBar.getStart();
            end = colorBar.getEnd();
            intv = (end - start) / 4.0;
            if(intv == 0) intv = 1;
         }
         ticks.setTrackingParentAxis(false);
         ticks.setStart(start);
         ticks.setEnd(end);
         ticks.setInterval(intv);
         
         // choose floating-pt tick label format based on the absolute axis range
         LabelFormat tickFmt = LabelFormat.INT;
         double rng = Math.abs(end-start);
         if(rng <= 0.1) tickFmt = LabelFormat.F3;
         else if(rng <= 1) tickFmt = LabelFormat.F2;
         else if(rng <= 10) tickFmt = LabelFormat.F1;
         ticks.setTickLabelFormat(tickFmt);
      }
      
      // gap between gradient bar and axis. In Matlab, this is always 0.
      colorBar.setBarAxisSpacer(new Measure(0, Measure.Unit.IN));
      
      // axis label offset. For horizontal color bars, we set this to 1.5x the font size in pts. For vertical bars, we
      // need to account for the longest tick label, which is considerably more difficult. Rather than trying to 
      // accurately measure the length of the longest tick label in pts, we assume a width/height ratio of 0.6...
      int ofs = (int) Math.round(colorBar.getFontSizeInPoints() * 1.5);
      if(!isHorizBar)
      {
         String longest = removeTickSet ? "" : ticks.getLongestTickLabel();
         ofs = (int) Math.round(longest.length() * colorBar.getFontSizeInPoints() * 0.6);
         
         // also adjust by tick length if ticks point outward, and by gap between tick label and end of tick mark.
         ofs += gapInPts; 
         if((!removeTickSet) && ticks.getTickOrientation()==Orientation.OUT)
         {
            double len = ticks.getTickLength().toMilliInches();
            ofs += (int) (72.0*len/1000);
         }
      }
      colorBar.setLabelOffset(new Measure(ofs, Measure.Unit.PT));
      
      // Colorbar label. If the HG colorbar object has a non-empty X- or Y-axis label (again, which we use depends on 
      // colorbar's orientation), then this becomes the colorbar label. Matlab defines the labels as "text" children of 
      // the HG "scribe.colorbar" object. We ignore all properties of the "text" object except the text string itself.
      String label = (edge == ColorBarNode.Edge.LEFT || edge == ColorBarNode.Edge.RIGHT) ?
            hgColorBar.getYLabel() : hgColorBar.getXLabel();
      if(label == null)
      {
         // In Matlab 2014b and beyond, the colorbar is no longer a specialized 'axes' and has no text children. The 
         // label is put in a Matlab text primitive in the field 'Label', but that cannot be parsed from a FIG file. 
         // However, when the HGObject hierarchy is prepared by matfig2fyp(), it will check the 'Label' text object and 
         // put the value of  'Label.String' in the HGObject under the property name "Label_Str". Alternatively, 
         // savefigasis() will put 'Label.String' in the 'UserData' field
         prop = hgColorBar.getProperty("Label_Str");
         if(prop != null && (prop.getClass().equals(String.class) || prop.getClass().equals(Character.class)))
            label = prop.toString();
         else
         {
            prop = hgColorBar.getProperty("UserData");
            if(prop != null && (prop.getClass().equals(String.class) || prop.getClass().equals(Character.class)))
               label = prop.toString();
         }
      }
      colorBar.setTitle((label != null) ? processSpecialCharacters(label) : "");
   }

   private static void setFontProperties(FGraphicNode fgn, HGObject hgObj)
   {
      Object fontProp = hgObj.getProperty("FontName");
      if(fontProp != null && fontProp.getClass() == String.class)
      {
         String font = (String)fontProp;
         fgn.setFontFamily(font); 
      }
      
      Object fontAngleProp = hgObj.getProperty("FontAngle");
      boolean isItalic = false;
      if(fontAngleProp != null && fontAngleProp.getClass().equals(String.class))
      {
         String fontAngle = (String)fontAngleProp;
         isItalic = (fontAngle.equals("italic") || fontAngle.equals("oblique"));
      }
      Object fontWtProp = hgObj.getProperty("FontWeight");
      boolean isBold = false;
      if(fontWtProp != null && fontWtProp.getClass().equals(String.class))
      {
         String fontWt = (String)fontWtProp;
         isBold = (fontWt.equals("bold") || fontWt.equals("demi"));
      }
      FontStyle fs = FontStyle.PLAIN;
      if(isItalic) fs = isBold ? FontStyle.BOLDITALIC : FontStyle.ITALIC;
      else if(isBold) fs = FontStyle.BOLD;
      fgn.setFontStyle(fs);
      
      // if HG "FontSize" implicit, ignore "FontUnits" and assume default: 10 pt. Also, starting in R2014b, font size
      // is always in points and "FontUnits" property no longer exists.
      int fontSz = 10;
      Object fontSzProp = hgObj.getProperty("FontSize");
      if(fontSzProp != null && fontSzProp.getClass().equals(Double.class))
      {
         double sz = (Double) fontSzProp;
         Object unitsProp = hgObj.getProperty("FontUnits");
         if(unitsProp != null && unitsProp.getClass().equals(String.class))
         {
            // NOTE: "normalized" and "pixels" are not supported; these map to "points" (the default)
            String units = (String) unitsProp;
            if(units.equals("centimeters")) sz *= Measure.MM2IN * 10.0/Measure.PT2IN;
            else if(units.equals("inches")) sz *= 1.0 / Measure.PT2IN;
         }
         fontSz = Utilities.rangeRestrict(FGraphicNode.MINFONTSIZE, FGraphicNode.MAXFONTSIZE, (int) (sz + 0.5));
      }
      fgn.setFontSizeInPoints(fontSz);
   }
   
   /**
    * Convert a Matlab color specification into a Java {@link Color Color} object. The Matlab color spec may
    * take four forms: an RGB triple [r g b], where each component lies in [0..1]; a hexadecimal color code; a single 
    * character representing one of 8 basic colors; or the full name of one of those colors: <i>y = yellow, m = magenta,
    * c = cyan, r = red, g = green, b = blue, w = white, k = black</i>.
    * 
    * <p>The hexadecimal color code is a string starting with the pound sign ('#') and followed by 3 or 6 hexadecimal
    * digits (0-9, A-F, upper or lower case): #RRGGBB or #RGB. The 3-digit version is converted to the 6-digit version
    * by doubling the digits: #2af => #22aaff.</p>
    * 
    * @param colorSpec The Matlab color specification (converted to a Java object: double[], Character, String).
    * @return The corresponding color, or null if color specification is null or invalid.
    */
   static Color processColorSpec(Object colorSpec)
   {
      Color c;
      if(colorSpec == null)
         c = null;
      else if(colorSpec.getClass().equals(double [].class))
      {
         double[] rgb = (double[]) colorSpec;
         if(rgb.length != 3 || rgb[0] < 0 || rgb[0] > 1.0 || rgb[1] < 0 || rgb[1] > 1.0 || rgb[2] < 0 || rgb[2] > 1.0)
            c = null;
         else 
            c = new Color((float) rgb[0], (float) rgb[1], (float) rgb[2]);
      }
      else
      {
         c = null;
         String name = colorSpec.toString();
         if(name.equals("y") || name.equals("yellow")) c = Color.YELLOW;
         else if(name.equals("m") || name.equals("magenta")) c = Color.MAGENTA;
         else if(name.equals("c") || name.equals("cyan")) c = Color.CYAN;
         else if(name.equals("r") || name.equals("red")) c = Color.RED;
         else if(name.equals("g") || name.equals("green")) c = Color.GREEN;
         else if(name.equals("b") || name.equals("blue")) c = Color.BLUE;
         else if(name.equals("w") || name.equals("white")) c = Color.WHITE;
         else if(name.equals("k") || name.equals("black")) c = Color.BLACK;
         else if(name.startsWith("#"))
         {
            String hexCode = name.toLowerCase().substring(1);
            if(hexCode.length() == 3)
            {
               // convert "RGB" to "RRGGBB"
               StringBuilder sb = new StringBuilder();
               for(int i=0; i<3; i++) sb.append(hexCode.charAt(i)).append(hexCode.charAt(i));
               hexCode = sb.toString();
            }
            if(hexCode.length() == 6) c = BkgFill.colorFromHexStringStrict(hexCode, false);
         }
      }
      return(c);
   }
   
   /**
    * Helper method processes the "GridLineStyle" property of a Matlab Handle Graphics "axes" object or the "LineStyle"
    * property of a plot object and returns the best-match FypML stroke pattern.
    * @param hgObj The Handle Graphics object. If type is "axes" or "polaraxes", the method looks at the "GridLineStyle"
    * property; else it looks at the "LineStyle" property.
    * @return The stroke pattern that best matches the "GridLineStyle" or "LineStyle" property, as appropriate. If the
    * property is not explicit, returns the Matlab default (solid line). If its value is "none", returns null.
    */
   static StrokePattern processLineStyle(HGObject hgObj)
   {
      assert(hgObj != null);
      
      String type = hgObj.getType();
      StrokePattern sp = StrokePattern.SOLID;
      boolean isAxes = type.equals("axes") || type.equals("matlab.graphics.axis.PolarAxes");
      Object prop = hgObj.getProperty(isAxes ? "GridLineStyle" : "LineStyle");
      if(prop != null)
      {
         // NOTE: may be Character or String class
         String lineStyle = prop.toString();
         switch(lineStyle)
         {
         case "none":
            sp = null;
            break;
         case "-":
            break;
         case "--":
            sp = StrokePattern.DASHED;
            break;
         case "-.":
            sp = StrokePattern.DASHDOT;
            break;
         case ":":
            sp = StrokePattern.DOTTED;
            break;
         }
      }
      return(sp);
   }

   /**
    * Helper method processes the "LineWidth" property of a Matlab Handle Graphics object and returns the equivalent 
    * {@link Measure Measure}. Note that the default "LineWidth" for is 0.5pt.
    * @param hgObj The Handle Graphics object.
    * @return The corresponding measured stroked width. Units will be in points. If the "LineWidth" property is not
    * explicitly specified for the HG object, method returns 0.5.
    */
   static Measure processLineWidth(HGObject hgObj)
   {
      double lineW = 0.5;
      Object lineWProp = hgObj.getProperty("LineWidth");
      if(lineWProp != null && lineWProp.getClass().equals(Double.class))
      {
         lineW = (Double) lineWProp;
         if(lineW < 0) lineW = 0.5;
      }
      return(new Measure(Utilities.limitFracDigits(lineW, 1), Measure.Unit.PT));
   }

   /**
    * Sometimes users create scatter plots in Matlab by plotting the points one at a time into the 'axes' object. This
    * results in a list of 'graph2D.lineseries' plot object children, each of which contain a single point and which all
    * share the same appearance properties. In this particular use case, the individual line-series objects are 
    * coalesced into a single one when the Handle Graphics object tree is processed. This method handles the details of
    * converting this coalesced scatter plot object into a FypML scatter plot node.
    */
   private static void addCoalescedScatterPlotToGraph(GraphNode graph, HGObject lineSeriesObj)
   {
      // HG "DisplayName" property maps to the scatter node's "title" property. Also, whenever "DisplayName" is defined
      // and not an empty string, we assume that the original plot object was included in any legend associated with
      // the axes -- so we set the node's "showInLegend" attribute.
      boolean showInLegend = false;
      String title = "";
      Object nameProp = lineSeriesObj.getProperty("DisplayName");
      if(nameProp != null && (nameProp.getClass().equals(String.class) || nameProp.getClass().equals(Character.class)))
      {
         title = processSpecialCharacters(nameProp.toString().trim());
         if(!title.isEmpty()) showInLegend = true;
      }

      // if HG "DisplayName" not set, we check for an undocumented property: the field 'legend_texthandle' in the HG
      // object's 'ApplicationData' property. If the field exists, the code that parses Matlab HG structure sets it as
      // a property directly on the HG object. It should be a scalar double.
      if(!showInLegend)
      {
         Object prop = lineSeriesObj.getProperty("legend_texthandle");
         if(prop != null && prop.getClass().equals(Double.class)) showInLegend = true;
      }

      // extract the coalesced scatter points as a single "ptset" data set.
      DataSet ds = lineSeriesObj.extractDataSetFromPlotObject(title, graph.isPolar(), false);
      if(ds == null) return;
      
      // HG "Marker" and "MarkerSize properties define the shape and size of the marker symbol for the scatter plot. A
      // marker must be defined -- since the points are not connected; "circle" is the default. "MarkerSize" indicates 
      // the symbol size in points (default = 6pt). The "point" marker is mapped to "circle" and is drawn at 1/3 the 
      // specified "MarkerSize" (per Matlab documentation).
      Marker mark = Marker.CIRCLE;
      double markSzPts = 6;
      Object prop = lineSeriesObj.getProperty("MarkerSize");
      if(prop != null && prop.getClass().equals(Double.class))
         markSzPts = (Double) prop;
 
      prop = lineSeriesObj.getProperty("Marker");
      boolean isPointMarker = false;
      if(prop != null && (prop.getClass().equals(String.class) || prop.getClass().equals(Character.class)))
      {
         String s = prop.toString();
         switch(s)
         {
         case "+":
            mark = Marker.TEE;
            break;
         case "o":
            break;
         case "*":
            mark = Marker.STAR;
            break;
         case ".":
            markSzPts /= 3.0;
            isPointMarker = true;
            break;
         case "x":
            mark = Marker.XHAIR;
            break;
         case "_":
            mark = Marker.HLINETHRU;
            break;
         case "|":
            mark = Marker.LINETHRU;
            break;
         case "square":
         case "s":
            mark = Marker.BOX;
            break;
         case "diamond":
         case "d":
            mark = Marker.DIAMOND;
            break;
         case "^":
            mark = Marker.UPTRIANGLE;
            break;
         case "v":
            mark = Marker.DOWNTRIANGLE;
            break;
         case ">":
            mark = Marker.RIGHTTRIANGLE;
            break;
         case "<":
            mark = Marker.LEFTTRIANGLE;
            break;
         case "pentagram":
         case "p":
            mark = Marker.PENTAGRAM;
            break;
         case "hexagram":
         case "h":
            mark = Marker.HEXAGRAM;
            break;
         }
      }
      
      Measure markSz = new Measure(Utilities.limitFracDigits(markSzPts, 1), Measure.Unit.PT);
      
      
      // HG "Color", "LineWidth", and "MarkerEdgeColor" combine to determine the stroke color and stroke width of the
      // scatter plot's marker symbols. The stroke pattern is always solid. The FypML scatter node does NOT have a 
      // component symbol node, so these properties are set on the scatter plot node itself..
      Color strokeC = processColorSpec(lineSeriesObj.getProperty("Color"));
      
      Measure strokeW = processLineWidth(lineSeriesObj);
      StrokePattern sp = StrokePattern.SOLID;
      
      prop = lineSeriesObj.getProperty("MarkerEdgeColor");
      if(prop != null)
      {
         // could be a Matlab color spec or one of two other possible strings: "none" or "auto"
         Color c = processColorSpec(prop);
         if(c != null)
            strokeC = c;
         else if(prop.getClass().equals(String.class))
         {
            // if MarkerEdgeColor = "none", then we don't stroke the markers -- set stroke with to 0.
            String s = (String) prop;
            if(s.equals("none")) strokeW = new Measure(0, Measure.Unit.IN);
         }
      }

      // HG "MarkerFaceColor" property sets the fill color for the marker symbols. It can be a Matlab color spec, OR
      // the strings "none" (hollow symbol, which is the Matlab default) or "auto" (set to same color as HG "axes" or 
      // "figure"). We map them as follows: "none"/implicit ==> fully transparent black; "auto" ==> inherited. However, 
      // if the Matlab marker type was "point", that is mapped to a filled circle in FypML, and we force the fill color 
      // to match the stroke color.
      prop = lineSeriesObj.getProperty("MarkerFaceColor");
      Color fillC;
      if(isPointMarker)
         fillC = (strokeC != null) ? strokeC : Color.BLACK;
      else if(prop == null)
         fillC = TRANSPARENTBLACK;
      else
      {
         fillC = processColorSpec(prop);
         if(fillC == null && prop.getClass().equals(String.class) && "none".equals(prop))
            fillC = TRANSPARENTBLACK;
      }
      
      // we can now append the scatter plot node as a child of the graph. The display mode in this use case is always
      // "scatter", since there is no Z data.
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.SCATTER, -1))
         throw new NeverOccursException("Failed to insert new scatter plot node into FypML graph.");
      
      ScatterPlotNode spn = (ScatterPlotNode) graph.getChildAt(graph.getChildCount()-1);
      spn.setMode(ScatterPlotNode.DisplayMode.SCATTER);
      spn.setDataSet(ds);
      spn.setShowInLegend(showInLegend);
      spn.setTitle(title);
      spn.setSymbol(mark);
      spn.setMaxSymbolSize(markSz);
      spn.setMeasuredStrokeWidth(strokeW);
      spn.setStrokePattern(sp);
      spn.setStrokeColor(strokeC);
      spn.setFillColor(fillC);
   }
   
   private final static Color TRANSPARENTBLACK = new Color(0,0,0,0);
   
   /**
    * Append a data trace defined by a Matlab Handle Graphics "graph2d.lineseries" plot object to a FypML 2D graph
    * container.
    * 
    * <p>Typically the plot object will be converted into a FypML {@link TraceNode}. However, if the line series data is
    * consistent with a raster plot -- a series of short, disjoint vertical lines all starting on one baseline Y=L or a 
    * set of baselines Y=L, L+1, ... -- then it will be converted into a FypML {@link RasterNode}. See {@link 
    * #addRasterToGraph} for details.
    * 
    * <p><i>Special case: Staircase plot</i>. The HG "specgraph.stairseries" plot object has all the same properties as
    * a "graph2d.lineseries" object. This method will translate the object to a {@link TraceNode} with its display mode
    * set to "staircase" (introduced in FypML schema version 20, app V4.7.2).</p>
    * 
    * <p><i>Special case #2: Stem plot</i>. The HG "specgraph.stemseries" plot object has many of the same properties
    * as a "graph2d.lineseries" object. This method will translate the object to a {@link TraceNode} in the "histogram"
    * display mode with zero bar width to achieve the desired effect. Previous processing ensures that the baseline
    * value for the stem plot is available in the HG object's "BaseValue" property. Note that the default value for the
    * "Marker" property is "o" (circle) for a "stemseries" object, not "none" as is the case for a "lineseries".</p>
    * 
    * <p><i>Special considerations for a polar plot:</i>:
    * <ul>
    * <li>Raster trains are not supported in a polar context.</li>
    * <li>A compass plot is rendered by putting the trace node in "histogram" display mode with zero bar width and
    * baseline. An arrow symbol sized at 0.2-in is drawn at the end of each histogram line to make the trace look like
    * a compass plot (the different-sized arrowheads that Matlab renders are not supported in FypML).</li>
    * <li>A rose plot is rendered by again using "histogram" display mode with zero baseline but with the bar width set
    * to the angle histogram's bin size in degrees. The FypML version is somewhat different from the Matlab rose plot 
    * because the endpoints of the radii bounding each "pie slice" are connected by a circular arc rather than a 
    * straight line segment.</li>
    * </ul>
    * </p>
    *  
    * @param graph The parent graph container in the FypML figure being constructed. It cannot be a 3D graph.
    * @param lineSeriesObj The HG line series object defining the trace's properties -- including the raw data!
    * @param isTruePolarAxes True if the parent HG object is a "polaraxes" rather than the "axes". This will be the
    * case for plots generated by Matlab's polarplot() function (R2016a or later).
    */
   private static void addDataTraceToGraph(FGNGraph graph, HGObject lineSeriesObj, boolean isTruePolarAxes)
   {
      // not supported for 3D graph containers
      if(graph.is3D()) return;
      
      // first check line series to see if it should be converted to a raster node. Non-polar plots only.
      if((!graph.isPolar()) && addRasterToGraph((GraphNode) graph, lineSeriesObj)) return;

      // append the trace node to the graph, initially associated with an empty data set
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.TRACE, -1))
         throw new NeverOccursException("Failed to insert new trace node into FypML 2D graph container.");
      
      TraceNode trace = (TraceNode) graph.getChildAt(graph.getChildCount()-1);
      
      // HG "Color, LineWidth, LineStyle" properties define draw-related properties of the trace
      Color c = processColorSpec(lineSeriesObj.getProperty("Color"));
      if(c != null) trace.setStrokeColor(c);
      
      Measure strokeW = processLineWidth(lineSeriesObj);
      
      // if "LineStyle==none" OR if this line-series object represents a coalesced scatter plot (points not connected),
      // then we have to set stroke width to 0 on the trace node -- but NOT on the component SymbolNode. In Matlab, the 
      // "LineWidth" property applies to the stroking of marker symbols, while "LineStyle" only applies to the line 
      // connecting the data points.
      StrokePattern sp = processLineStyle(lineSeriesObj);
      if(sp == null || lineSeriesObj.isCoalescedScatterPlot())
      {
         // DataNav FypML uses 0 stroke width for the "none" line style
         trace.setMeasuredStrokeWidth(new Measure(0, Measure.Unit.PT));
         trace.setStrokePattern(StrokePattern.SOLID);
      }
      else
      {
         trace.setMeasuredStrokeWidth(strokeW);
         trace.setStrokePattern(sp);
      }
      
      // HG "DisplayName" property maps to the trace node's "title" property. Also, whenever "DisplayName" is defined
      // and not an empty string, we assume that the original plot object was included in any legend associated with
      // the axes -- so we set the trace's "showInLegend" attribute.
      boolean showInLegend = false;
      Object nameProp = lineSeriesObj.getProperty("DisplayName");
      if(nameProp != null && (nameProp.getClass().equals(String.class) || nameProp.getClass().equals(Character.class)))
      {
         String title = processSpecialCharacters(nameProp.toString().trim());
         trace.setTitle(title);
         if(!title.isEmpty()) showInLegend = true;
      }

      // if HG "DisplayName" not set, we check for an undocumented property: the field 'legend_texthandle' in the HG
      // object's 'ApplicationData' property. If the field exists, the code that parses Matlab HG structure sets it as
      // a property directly on the HG object. It should be a scalar double.
      if(!showInLegend)
      {
         Object prop = lineSeriesObj.getProperty("legend_texthandle");
         if(prop != null && prop.getClass().equals(Double.class)) showInLegend = true;
      }
      trace.setShowInLegend(showInLegend);
      
      // HG "Marker" and "MarkerSize properties define the shape and size of the marker symbol for data points. The 
      // default is no marker, which we emulate by setting the symbol size to 0 on the trace's component SymbolNode. 
      // Otherwise, "MarkerSize" indicates the symbol size in points (default = 6pt). The "point" marker
      // is mapped to "circle", is filled with same color as it is stroked, and is drawn at 1/3 the specified 
      // "MarkerSize" (per Matlab documentation).
      // NOTE: For a "stemseries" object, the default marker is a circle, not "none"!!
      boolean isStemPlot = lineSeriesObj.getType().equals("specgraph.stemseries");
      SymbolNode symbol = trace.getSymbolNode();
      Marker mark = isStemPlot ? Marker.CIRCLE : null;
      double markSzPts = 6;
      Object prop = lineSeriesObj.getProperty("MarkerSize");
      if(prop != null && prop.getClass().equals(Double.class))
         markSzPts = (Double) prop;
      prop = lineSeriesObj.getProperty("Marker");
      boolean isPointMarker = false;
      if(prop != null && (prop.getClass().equals(String.class) || prop.getClass().equals(Character.class)))
      {
         String s = prop.toString();
         switch(s)
         {
         case "+":
            mark = Marker.TEE;
            break;
         case "o":
            mark = Marker.CIRCLE;
            break;
         case "*":
            mark = Marker.STAR;
            break;
         case ".":
            mark = Marker.CIRCLE;
            markSzPts /= 3.0;
            isPointMarker = true;
            break;
         case "x":
            mark = Marker.XHAIR;
            break;
         case "_":
            mark = Marker.HLINETHRU;
            break;
         case "|":
            mark = Marker.LINETHRU;
            break;
         case "square":
         case "s":
            mark = Marker.BOX;
            break;
         case "diamond":
         case "d":
            mark = Marker.DIAMOND;
            break;
         case "^":
            mark = Marker.UPTRIANGLE;
            break;
         case "v":
            mark = Marker.DOWNTRIANGLE;
            break;
         case ">":
            mark = Marker.RIGHTTRIANGLE;
            break;
         case "<":
            mark = Marker.LEFTTRIANGLE;
            break;
         case "pentagram":
         case "p":
            mark = Marker.PENTAGRAM;
            break;
         case "hexagram":
         case "h":
            mark = Marker.HEXAGRAM;
            break;
         }
      }
      if(mark == null)
         symbol.setSize(new Measure(0, Measure.Unit.PT));
      else
      {
         symbol.setType(mark);
         symbol.setSize(new Measure(Utilities.limitFracDigits(markSzPts, 1), Measure.Unit.PT));
         
         // SymbolNode's stroke width is determined by the Matlab 'LineWidth' property and the stroke pattern is always
         // solid, regardless what the "LineStyle" is for the "graph2d.lineseries" object.
         // Matlab default line width
         symbol.setMeasuredStrokeWidth(strokeW);
         symbol.setStrokePattern(StrokePattern.SOLID);
      }
      
      
      // HG "MarkerEdgeColor" property sets the stroke color for the marker symbols. If implicit, or if explicitly
      // set to "auto", then the SymbolNode inherits the parent TraceNode's stroke color. If "none", then we set the
      // SymbolNode's stroke width to 0.
      prop = lineSeriesObj.getProperty("MarkerEdgeColor");
      if(prop != null)
      {
         // could be a Matlab color spec or one of two other possible strings: "none" or "auto"
         c = processColorSpec(prop);
         if(c != null)
            symbol.setStrokeColor(c);
         else if(prop.getClass().equals(String.class))
         {
            String s = (String) prop;
            if(s.equals("none"))
               symbol.setMeasuredStrokeWidth(new Measure(0, Measure.Unit.IN));
         }
      }
      
      // HG "MarkerFaceColor" property sets the fill color for the marker symbols. It can be a Matlab color spec, OR
      // the strings "none" (hollow symbol, which is the Matlab default) or "auto" (set to same color as HG "axes" or 
      // "figure"). These two are NOT supported. "none"/implicit ==> WHITE; "auto" ==> inherited. However, if the
      // Matlab marker type was "point", that is mapped to a filled circle in FypML, and we force the fill color to
      // match the stroke color.
      prop = lineSeriesObj.getProperty("MarkerFaceColor");
      if(isPointMarker)
         c = symbol.getStrokeColor();
      else if(prop == null)
         c = TRANSPARENTBLACK;
      else
      {
         c = processColorSpec(prop);
         if(c == null && prop.getClass().equals(String.class) && "none".equals(prop))
            c = TRANSPARENTBLACK;
      }
      if(c != null) symbol.setFillColor(c);
      
      
      // HG "XDataMode, XData, YData" properties define the trace's raw data set.
      DataSet ds = lineSeriesObj.extractDataSetFromPlotObject(trace.getTitle(), graph.isPolar(), isTruePolarAxes);
      if(ds == null)
      {
         graph.getGraphicModel().deleteNode(trace);
         return;
      }
      else trace.setDataSet(ds);

      
      // if the data set format is MSET or MSERIES, then the line series is actually a collection of individual line
      // series -- so set the trace node's display mode accordingly!
      if(ds.getFormat().isCollection())
         trace.setMode(DisplayMode.MULTITRACE);
      
      // special cases: compass and rose plots in a Matlab "axes" configured to "look polar", or a staircase plot
      if(lineSeriesObj.isCompassPlot())
      {
         trace.setMode(DisplayMode.HISTOGRAM);
         trace.setBarWidth(0);
         trace.setBaseline(0);
         
         symbol.setType(Marker.THINARROW);
         symbol.setSize(new Measure(0.2, Measure.Unit.IN));
      }
      else if(lineSeriesObj.isRosePlot())
      {
         trace.setMode(DisplayMode.HISTOGRAM);
         trace.setBarWidth(360f/ds.getDataLength());
         trace.setBaseline(0);
         trace.setFillColor(new Color(0,0,0,0));  // transparent fill = not filled!
      }
      else if(lineSeriesObj.getType().equals("specgraph.stairseries"))
         trace.setMode(DisplayMode.STAIRCASE);
      else if(isStemPlot)
      {
         trace.setMode(DisplayMode.HISTOGRAM);
         trace.setBarWidth(0);
         double baseline = 0;
         prop = lineSeriesObj.getProperty("BaseValue");
         if(prop != null && prop.getClass().equals(Double.class)) baseline = (Double) prop;
         trace.setBaseline((float)baseline);
      }
   }
   
   /**
    * If the specified Matlab Handle graphics "graph2d.lineseries" plot object has x- and y-coordinate data consistent
    * with a raster train plot, then it is converted into a FypML {@link RasterNode RasterNode} and
    * appended to the parent graph node. Otherwise, no action is taken.
    * 
    * <p>The plot object's "XData" and "YData" properties must satisfy a number of constraints for it to be interpreted
    * as a raster train plot. See {@link HGObject#isRasterPlot()} for details.</p>
    * 
    * @param graph The parent graph node in the FypML figure being constructed.
    * @param lineSeriesObj The HG line series object defining a rendered trace -- including the raw data.
    * @return True iff the line series data is consistent with a raster train plot, in which case it is converted to a
    * FypML raster node and appended to the parent graph.
    */
   private static boolean addRasterToGraph(GraphNode graph, HGObject lineSeriesObj)
   {
      if(!lineSeriesObj.isRasterPlot()) return(false);
      
      // append a new raster node to the graph, initially associated with an empty data set
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.RASTER, -1))
         throw new NeverOccursException("Failed to insert new raster node into FypML graph.");
      
      RasterNode raster = (RasterNode) graph.getChildAt(graph.getChildCount()-1);
      
      // HG "Color, LineWidth" properties define draw-related properties of the raster. We always stroke it solid.
      Color c = processColorSpec(lineSeriesObj.getProperty("Color"));
      if(c != null) raster.setStrokeColor(c);
      
      Measure strokeW = processLineWidth(lineSeriesObj);
      raster.setMeasuredStrokeWidth(strokeW);
      
      raster.setStrokePattern(StrokePattern.SOLID);
      
      // HG "DisplayName" property maps to the raster node's "title" property.
      Object nameProp = lineSeriesObj.getProperty("DisplayName");
      if(nameProp != null && (nameProp.getClass().equals(String.class) || nameProp.getClass().equals(Character.class)))
         raster.setTitle(processSpecialCharacters(nameProp.toString().trim()));

      // the raster plot format is best emulated by the RasterNode's "trains2" display mode, in which each sample in
      // train n is drawn as a vertical line of height H sitting on baseline Y = yOffset + n, for n=0:N-1, N being the
      // number of distinct raster trains in the raster data.
      raster.setMode(RasterNode.DisplayMode.TRAINS2);

      // baseline = minimum y-coordinate value. NOTE that we've already tested the YData property to verify that it
      // satisfies the raster plot format!
      double[] yData = (double[]) lineSeriesObj.getProperty("YData");
      double baseline = Double.MAX_VALUE;
      for(int i=0; i<yData.length; i+=3) if(yData[i] < baseline) baseline = yData[i];

      raster.setBaseline((float) baseline);
      
      // subtract the baseline from the first y-coordinate in each triplet. The result is the ordinal of the raster
      // train to which that particular raster sample belongs.
      for(int i=0; i<yData.length; i+=3) yData[i] -= baseline;
      
      // get the x-coordinate data. Again, we can be confident of its nature because we already checked!
      double[] xData = (double[]) lineSeriesObj.getProperty("XData");

      // set the raster line height in stroke-width units. We know that all vertical lines defined in the line series
      // data have the same height, because this is a requirement for being interpreted as a raster train plot! If the
      // graph's Y-axis is auto-ranged, then the final range won't be set until all data sets are added -- in which
      // case we CANNOT rely on the graph's viewport to calculate the real height of the raster marks. In this case we
      // just ASSUME a raster line height of 10.
      int lineHt = 10;
      if(!graph.getSecondaryAxis().isAutoranged())
      {
         Point2D p0 = new Point2D.Double(xData[0], yData[0] + baseline);
         Point2D p1 = new Point2D.Double(xData[1], yData[1]);
         double htMI = graph.getViewport().convertUserDistanceToThousandthInches(p0, p1);
         if(htMI == 0) htMI = 200;
         lineHt = (int) (0.5 + htMI / strokeW.toMilliInches());
         if(lineHt <= 0) lineHt = 1;
      }
      raster.setLineHeight(lineHt);
      
      // determine the number N of distinct raster trains. Since the first y-coordinate in each triplet now contains the 
      // raster train ordinal, N = max(ordinal) + 1;
      double maxOrd = -1;
      for(int i=0; i<yData.length; i+=3) if(yData[i] > maxOrd) maxOrd = yData[i];
      int nTrains = 1 + ((int) (maxOrd + 0.5));
      
      // the number of raster samples is simply 1/3 the length of the line series data vectors
      int nSamples = xData.length / 3;
      
      // allocate and populate the raw data array for the raster1d data set
      int[] nEventsSoFar = new int[nTrains];
      int[] firstEventIdx = new int[nTrains];
      float[] fData = new float[nTrains + nSamples];
      for(int i=0; i<yData.length; i+=3)
      {
         int ord = (int) (yData[i] + 0.5);
         nEventsSoFar[ord]++;
      }
      
      int idx = nTrains;
      for(int i=0; i<nTrains; i++)
      {
         fData[i] = (float) nEventsSoFar[i];
         firstEventIdx[i] = idx;
         idx += nEventsSoFar[i];
         nEventsSoFar[i] = 0;
      }
      
      for(int i=0; i<yData.length; i+=3)
      {
         int ord = (int) (yData[i] + 0.5);
         idx = firstEventIdx[ord] + nEventsSoFar[ord];
         fData[idx] = (float) xData[i];
         ++nEventsSoFar[ord];
      }
      
      // create a valid ID for the data set. We try to use the raster node's title, if it has been set.
      // generate a data set ID string
      String id = null;
      if(DataSet.isValidIDString(raster.getTitle()))
      {
         id = "src_" + raster.getTitle();
         if(!DataSet.isValidIDString(id)) id = null;
      }
      if(id == null)
      {
         int n = ((int) (Math.random() * 3000.0)) % 100;
         id = "src_" + n;
      }

      // construct the raster1d data set and make it the source for the new raster node
      DataSet ds = DataSet.createDataSet(id, Fmt.RASTER1D, null, nSamples, nTrains, fData);
      if(ds == null) graph.getGraphicModel().deleteNode(raster);
      else raster.setDataSet(ds);
      
      return(true);
   }
   
   /**
    * Append a data trace ({@link TraceNode TraceNode} with error bar data to the graph, as defined
    * by a Matlab Handle Graphics "specgraph.errorbarseries" plot object. 
    * <p>In Matlab, the error bars are always stroked with a solid line style, using the same color and line width as 
    * the trace line; and the error bar endcaps in Matlab are always short horizontal lines. As of release 2016b, the
    * endcap length is set by the 'CapSize' property, which defaults to 6pt.</p>
    * <p>Release 2016b also introduced support for horizontal as well as vertical error bars. The helper method {@link 
    * HGObject#extractDataSetFromPlotObject} handles the new properties encoding the horizontal and vertical deltas.
    * Note that Matlab supports unsymmetrical error bars, while FypML does not.</p>
    * 
    * @param graph The parent graph node in the FypML figure being constructed.
    * @param ebarObj The HG "specgraph.errorbarseries" object defining the trace's properties -- including the raw data!
    */
   private static void addErrorBarTraceToGraph(GraphNode graph, HGObject ebarObj)
   {
      // append the trace node to the graph, initially associated with an empty data set
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.TRACE, -1))
         throw new NeverOccursException("Failed to insert new trace node into FypML graph.");
      
      TraceNode trace = (TraceNode) graph.getChildAt(graph.getChildCount()-1);
      
      // HG "Color, LineWidth, LineStyle" properties define draw-related properties of the trace
      Color c = processColorSpec(ebarObj.getProperty("Color"));
      if(c != null) trace.setStrokeColor(c);
      
      Measure strokeW = processLineWidth(ebarObj);
      
      // if "LineStyle==none", then we have to set stroke width to 0 on the trace node -- but NOT on the component
      // SymbolNode or ErrorBarNode. In Matlab, the "LineWidth" property applies to the stroking of marker symbols and
      // error bars, while "LineStyle" only applies to the line connecting the data points.
      StrokePattern sp = processLineStyle(ebarObj);
      if(sp == null)
      {
         // DataNav FypML uses 0 stroke width for the "none" line style
         trace.setMeasuredStrokeWidth(new Measure(0, Measure.Unit.PT));
         trace.setStrokePattern(StrokePattern.SOLID);
      }
      else
      {
         trace.setMeasuredStrokeWidth(strokeW);
         trace.setStrokePattern(sp);
      }
      
      // HG "DisplayName" property maps to the trace node's "title" property. Also, whenever "DisplayName" is defined
      // and not an empty string, we assume that the original plot object was included in any legend associated with
      // the axes -- so we set the trace's "showInLegend" attribute.
      boolean showInLegend = false;
      Object nameProp = ebarObj.getProperty("DisplayName");
      if(nameProp != null && (nameProp.getClass().equals(String.class) || nameProp.getClass().equals(Character.class)))
      {
         String title = processSpecialCharacters(nameProp.toString().trim());
         trace.setTitle(title);
         if(!title.isEmpty()) showInLegend = true;
      }

      // if HG "DisplayName" not set, we check for an undocumented property: the field 'legend_texthandle' in the HG
      // object's 'ApplicationData' property. If the field exists, the code that parses Matlab HG structure sets it as
      // a property directly on the HG object. It should be a scalar double.
      if(!showInLegend)
      {
         Object prop = ebarObj.getProperty("legend_texthandle");
         if(prop != null && prop.getClass().equals(Double.class)) showInLegend = true;
      }
      trace.setShowInLegend(showInLegend);
      
      // HG "Marker" and "MarkerSize properties define the shape and size of the marker symbol for data points. The 
      // default is no marker, which we emulate by setting the symbol size to 0 on the trace's component SymbolNode. 
      // Otherwise, "MarkerSize" indicates the symbol size in points (default = 6pt). The "point" marker is mapped to 
      // "circle", is filled with same color as it is stroked, and is drawn at 1/3 the specified "MarkerSize" (per 
      // Matlab documentation).
      SymbolNode symbol = trace.getSymbolNode();
      Marker mark = null;
      double markSzPts = 6;
      Object prop = ebarObj.getProperty("MarkerSize");
      if(prop != null && prop.getClass().equals(Double.class))
         markSzPts = (Double) prop;
      prop = ebarObj.getProperty("Marker");
      boolean isPointMarker = false;
      if(prop != null && (prop.getClass().equals(String.class) || prop.getClass().equals(Character.class)))
      {
         String s = prop.toString();
         switch(s)
         {
         case "+":
            mark = Marker.TEE;
            break;
         case "o":
            mark = Marker.CIRCLE;
            break;
         case "*":
            mark = Marker.STAR;
            break;
         case ".":
            mark = Marker.CIRCLE;
            markSzPts /= 3.0;
            isPointMarker = true;
            break;
         case "x":
            mark = Marker.XHAIR;
            break;
         case "_":
            mark = Marker.HLINETHRU;
            break;
         case "|":
            mark = Marker.LINETHRU;
            break;
         case "square":
         case "s":
            mark = Marker.BOX;
            break;
         case "diamond":
         case "d":
            mark = Marker.DIAMOND;
            break;
         case "^":
            mark = Marker.UPTRIANGLE;
            break;
         case "v":
            mark = Marker.DOWNTRIANGLE;
            break;
         case ">":
            mark = Marker.RIGHTTRIANGLE;
            break;
         case "<":
            mark = Marker.LEFTTRIANGLE;
            break;
         case "pentagram":
         case "p":
            mark = Marker.PENTAGRAM;
            break;
         case "hexagram":
         case "h":
            mark = Marker.HEXAGRAM;
            break;
         }
      }
      if(mark == null)
         symbol.setSize(new Measure(0, Measure.Unit.PT));
      else
      {
         symbol.setType(mark);
         symbol.setSize(new Measure(Utilities.limitFracDigits(markSzPts, 1), Measure.Unit.PT));
         
         // SymbolNode's stroke width is determined by the Matlab 'LineWidth' property and the stroke pattern is always
         // solid, regardless what the "LineStyle" is for the HG plot object.
         symbol.setMeasuredStrokeWidth(strokeW);
         symbol.setStrokePattern(StrokePattern.SOLID);
      }
      
      
      // HG "MarkerEdgeColor" property sets the stroke color for the marker symbols. If implicit, or if explicitly
      // set to "auto", then the SymbolNode inherits the parent TraceNode's stroke color. If "none", then we set the
      // SymbolNode's stroke width to 0.
      prop = ebarObj.getProperty("MarkerEdgeColor");
      if(prop != null)
      {
         // could be a Matlab color spec or one of two other possible strings: "none" or "auto"
         c = processColorSpec(prop);
         if(c != null)
            symbol.setStrokeColor(c);
         else if(prop.getClass().equals(String.class))
         {
            String s = (String) prop;
            if(s.equals("none"))
               symbol.setMeasuredStrokeWidth(new Measure(0, Measure.Unit.IN));
         }
      }
      
      // HG "MarkerFaceColor" property sets the fill color for the marker symbols. It can be a Matlab color spec, OR
      // the strings "none" (hollow symbol, which is the Matlab default) or "auto" (set to same color as HG "axes" or 
      // "figure"). These two are NOT supported. "none"/implicit ==> WHITE; "auto" ==> inherited. However, if the
      // Matlab marker type was "point", that is mapped to a filled circle in FypML, and we force the fill color to
      // match the stroke color.
      prop = ebarObj.getProperty("MarkerFaceColor");
      if(isPointMarker)
         c = symbol.getStrokeColor();
      else if(prop == null)
         c = Color.WHITE;
      else
      {
         c = processColorSpec(prop);
         if(c == null && prop.getClass().equals(String.class) && "none".equals(prop))
            c = Color.WHITE;
      }
      if(c != null) symbol.setFillColor(c);
      
      // configure the component ErrorBarNode to show error bars using "linethru" endcaps. Set their size IAW the 
      // 'CapSize" property, if present. Otherwise, set to 6pt (this is the default in Matlab).
      ErrorBarNode ebarNode = trace.getErrorBarNode();
      ebarNode.setHide(false);
      ebarNode.setEndCap(Marker.LINETHRU);
      double capSzPts = 6;
      prop = ebarObj.getProperty("CapSize");
      if(prop != null && prop.getClass().equals(Double.class))
         capSzPts = (Double) prop;
      ebarNode.setEndCapSize(new Measure(Utilities.limitFracDigits(capSzPts, 1), Measure.Unit.PT));
      
      // the ErrorBarNode's stroke width is determined by the Matlab 'LineWidth' property and the stroke pattern is 
      // always solid, regardless what the "LineStyle" is for the HG plot object.
      ebarNode.setMeasuredStrokeWidth(strokeW);
      ebarNode.setStrokePattern(StrokePattern.SOLID);
      
      // HG "XDataMode, XData, YData, UData, LData, X*Delta, Y*Delta" properties define the trace's raw data set.
      DataSet ds = ebarObj.extractDataSetFromPlotObject(trace.getTitle(), graph.isPolar(), false);
      if(ds == null) graph.getGraphicModel().deleteNode(trace);
      else trace.setDataSet(ds);
   }
   
   /**
    * Append a FypML 2D or 3D scatter plot node that replicates a Matlab scatter or bubble plot, as defined by the 
    * Handle Graphics "specgraph.scattergroup" plot object. 
    * 
    * <p>A FypML scatter plot, whether in 2D or 3D, comes in one of four varieties:
    * <ol>
    * <li>"scatter" : The typical X-Y or X-Y-Z scatter plot in which all markers have the same size and appearance.</li>
    * <li>"sizeBubble" : All markers have the same fill color, but their size depends on the Z-coordinate value at each
    * 3D point (X,Y,Z) -- or the W-coordinate at each 4D point (X,Y,Z,W). 
    * at each 3D point (X,Y,Z).</li>
    * <li>"colorBubble" : All marker symbols have the same size, but their fill color depends on the Z-coordinate or 
    * W-coordinate value, which is mapped linearly to the parent graph's color map.</li>
    * <li>"colorSizeBubble": In this display mode, both symbol size and fill color vary with Z.</li>
    * </ol>
    * The "bubble" display modes of a 2D scatter plot offer a way to depict 3D data in 2D, while the "bubble" modes in 
    * a 3D scatter plot depict data along a 4th dimension.</p>
    * 
    * <p>Conversion of a Matlab "specgraph.scattergroup" to a FypML scatter plot node depends pivotally on the 'XData',
    * 'YData', 'ZData', 'CData', and 'SizeData' properties. The first two must be vectors of equal length N, or the 
    * conversion fails entirely. In a 3D graph, 'ZData' must also be an N-vector, or the conversion fails. The last two
    * properties determine the display mode for the FypML scatter plot and may define another data dimension:
    * <ul>
    * <li>If either (but not both) 'CData' or 'SizeData' is an N-vector, then it specifies the Z-coordinate data for
    * a 3D data set in a 2D scatter plot, or the W-coordinate in a 4D data set within a 3D scatter plot. In this case,
    * if 'CData' is an N-vector, the "colorBubble" display mode is selected; else the "sizeBubble" mode.</li>
    * <li>If both are N-vectors, then 'CData' is preferred over 'SizeData' to specify the Z- or W-coordinate data, and
    * the scatter plot's display mode is "colorSizeBubble".</li>
    * <li>If neither are N-vectors, then there is no Z- or W-coordinate data, and the display mode is "scatter".</li>
    * </ul>
    * Note that the data set extracted from the Matlab "scattergroup" plot object will be a 2D, 3D or 4D data set --
    * depending on the graph container and the state of the 'CData' and 'SizeData' properties.</p>
    * 
    * <p>Note that, if a Matlab axes contains multiple 'specgraph.scattergroup' objects that all use the same symbol
    * at all scatter points (same size and appearance), the coordinates of the points in the individual scatter groups
    * are coalesced into a single scatter group, and the others are discarded. This is handled during parsing of the
    * HG object hierarchy. In this scenario, 'CData' and 'SizeData' will always be single-valued, not N-vectors.</p>
    * 
    * @param graph The parent graph container in the FypML figure being constructed. For the 2D case, this is an 
    * instance of {@link GraphNode} or {@link PolarPlotNode}; for 3D, it will be a {@link Graph3DNode}.
    * @param scatObj The HG "specgraph.scattergroup" object defining the scatter plot -- including raw data!
    * @param isTruePolarAxes True if the parent HG object is a "polaraxes" rather than an "axes" object.
    */
   private static void addScatterGroupToGraph(FGNGraph graph, HGObject scatObj, boolean isTruePolarAxes)
   {
      boolean is2D = !graph.is3D();
      
      // HG "DisplayName" property maps to the scatter plot node's "title" property. Also, whenever "DisplayName" is 
      // defined and not an empty string, we assume that the original plot object was included in any legend associated 
      // with the axes -- so we set the node's "showInLegend" attribute.
      boolean showInLegend = false;
      String title = "";
      Object nameProp = scatObj.getProperty("DisplayName");
      if(nameProp != null && (nameProp.getClass().equals(String.class) || nameProp.getClass().equals(Character.class)))
      {
         title = processSpecialCharacters(nameProp.toString().trim());
         if(!title.isEmpty()) showInLegend = true;
      }

      // if HG "DisplayName" not set, we check for an undocumented property: the field 'legend_texthandle' in the HG
      // object's 'ApplicationData' property. If the field exists, the code that parses Matlab HG structure sets it as
      // a property directly on the HG object. It should be a scalar double.
      if(!showInLegend)
      {
         Object prop = scatObj.getProperty("legend_texthandle");
         if(prop != null && prop.getClass().equals(Double.class)) showInLegend = true;
      }

      // extract the data set from the scatter-group object
      boolean isPolar = graph.isPolar();
      DataSet ds = scatObj.extractDataSetFromPlotObject(title, isPolar, isTruePolarAxes);
      if(ds == null) return;

      // select the display mode. For 2D data in a 2D scatter plot or 3D data in a 3D scatter plot, only the "scatter"
      // mode makes sense. Otherwise, we check the CData and SizeData properties. If both are N-vectors where N = 
      // length(XData), then choose "colorSizeBubble". If only one is a vector, then choose "sizeBubble" or 
      // "colorBubble" as appropriate.
      int n = ds.getDataLength();
      double[] szData = HGObject.getDoubleVectorFromProperty(scatObj.getProperty("SizeData"));
      double[] cData = HGObject.getDoubleVectorFromProperty(scatObj.getProperty("CData"));
      ScatterPlotNode.DisplayMode dispMode = ScatterPlotNode.DisplayMode.SCATTER;
      if((is2D && ds.getFormat() == Fmt.XYZSET) || ((!is2D) && ds.getFormat() == Fmt.XYZWSET))
      {
         boolean szVaries = szData != null && szData.length > 1 && szData.length == n;
         boolean clrVaries = cData != null && cData.length > 1 && cData.length == n;
         
         if(szVaries) 
            dispMode = clrVaries ? ScatterPlotNode.DisplayMode.COLORSIZEBUBBLE : ScatterPlotNode.DisplayMode.SIZEBUBBLE;
         else if(clrVaries) dispMode = ScatterPlotNode.DisplayMode.COLORBUBBLE;
      }

      // HG "Marker" property defines the shape of the marker symbol for data points. The default value is 'none', which
      // makes no sense for a scatter plot since then nothing is drawn; if the value is 'none', we assume a circular 
      // symbol. The "point" marker is mapped to "circle".
      Marker mark = null;
      boolean isPointMarker = false;
      Object prop = scatObj.getProperty("Marker");
      if(prop != null && (prop.getClass().equals(String.class) || prop.getClass().equals(Character.class)))
      {
         String s = prop.toString();
         switch(s)
         {
         case "+":
            mark = Marker.TEE;
            break;
         case "o":
            mark = Marker.CIRCLE;
            break;
         case "*":
            mark = Marker.STAR;
            break;
         case ".":
            mark = Marker.CIRCLE;
            isPointMarker = true;
            break;
         case "x":
            mark = Marker.XHAIR;
            break;
         case "_":
            mark = Marker.HLINETHRU;
            break;
         case "|":
            mark = Marker.LINETHRU;
            break;
         case "square":
         case "s":
            mark = Marker.BOX;
            break;
         case "diamond":
         case "d":
            mark = Marker.DIAMOND;
            break;
         case "^":
            mark = Marker.UPTRIANGLE;
            break;
         case "v":
            mark = Marker.DOWNTRIANGLE;
            break;
         case ">":
            mark = Marker.RIGHTTRIANGLE;
            break;
         case "<":
            mark = Marker.LEFTTRIANGLE;
            break;
         case "pentagram":
         case "p":
            mark = Marker.PENTAGRAM;
            break;
         case "hexagram":
         case "h":
            mark = Marker.HEXAGRAM;
            break;
         }
      }
      if(mark == null) mark = Marker.CIRCLE;
      
      // HG "MarkerEdgeColor" property sets the stroke color for the marker symbols. A Matlab scatter plot can use a 
      // different color for each symbol, in which case MarkerEdgeColor='auto' (pre-R2014b) or 'flat' (R2014b+) and 
      // CData will be an N-vector (color map indices) or Nx3 matrix (color specs). FypML does not support this use case
      // -- we use BLACK in this scenario. If MarkerEdgeColor is "none", then we set the stroke color to fully 
      // transparent black. If it is a color spec, we use that color.
      Color strokeC = null;
      prop = scatObj.getProperty("MarkerEdgeColor");
      if(prop != null)
      {
         Color c = processColorSpec(prop);
         if(c != null) strokeC = c;
         else if("none".equals(prop)) strokeC = TRANSPARENTBLACK;
      }
      if(strokeC == null)
      {
         if(dispMode == ScatterPlotNode.DisplayMode.COLORBUBBLE || 
               dispMode == ScatterPlotNode.DisplayMode.COLORSIZEBUBBLE)
            strokeC = Color.BLACK;
         else
         {
            strokeC = processColorSpec(scatObj.getProperty("CData"));
            if(strokeC == null) strokeC = Color.BLACK;
         }
      }
      
      // HG "MarkerEdgeAlpha" property sets the alpha component of the symbol stroke color (introduced in R2015b). It
      // should be a value in [0..1] and defaults to 1 (opaque). Ignored if "MarkerEdgeColor" is "none". 
      if(strokeC.getAlpha() > 0)
      {
         prop = scatObj.getProperty("MarkerEdgeAlpha");
         if(prop != null && prop.getClass().equals(Double.class))
         {
            int alpha = Utilities.rangeRestrict(0, 255, (int) (255.0* (Double) prop));
            if(alpha < 255) strokeC = new Color(strokeC.getRed(), strokeC.getGreen(), strokeC.getBlue(), alpha);
         }
      }
      
      // HG "LineWidth" property gives the stroke width with which symbols are stroke. Stroking style is always solid.
      // NOTE: If stroke color is transparent, we ignore "LineWidth" and set the stroke width to 0. This improves 
      // rendering speed significantly in FC when there are many scatter points.
      Measure strokeW = strokeC.getAlpha()==0 ? new Measure(0, Measure.Unit.PT) : processLineWidth(scatObj);
      StrokePattern sp = StrokePattern.SOLID;

      // HG "MarkerFaceColor" and "MarkerFaceAlpha" properties select the common fill color for the marker symbols when 
      // the display mode is neither "colorBubble" nor "colorSizeBubble". It can be a Matlab color spec, OR the strings 
      // "none" (hollow symbol, which is the Matlab default),  "auto" (set to same color as HG "axes" or "figure"), or 
      // "flat" (the color specified in  'CData'). "none" ==> transparent black; "auto" ==> inherited; "flat" ==> Use 
      // CData only if that property is a color spec, else use transparent black. "MarkerFaceAlpha" specifies the alpha
      // component for the marker fill color; it should be a scalar between 0 (transparent) and 1 (opaque). If the 
      // marker type is "point", then the symbol is a filled circle with the fill color set to match the stroke color --
      // and so "MarkerFaceColor/Alpha" are ignored. If the scatter plot display mode is either "colorBubble" or 
      // "colorSizeBubble", these properties are also ignored, as the fill color is different for each symbol...
      Color fillC = null;
      if(dispMode!=ScatterPlotNode.DisplayMode.COLORBUBBLE && dispMode!=ScatterPlotNode.DisplayMode.COLORSIZEBUBBLE)
      {
         if(isPointMarker) 
            fillC = strokeC;
         else
         {
            prop = scatObj.getProperty("MarkerFaceColor");
            if(prop == null || "none".equals(prop)) fillC = TRANSPARENTBLACK;
            else if(!"auto".equals(prop))
            {
               fillC = processColorSpec("flat".equals(prop) ? scatObj.getProperty("CData") : prop);
               if(fillC == null) fillC = TRANSPARENTBLACK;
               else
               {
                  prop = scatObj.getProperty("MarkerFaceAlpha");
                  if(prop != null && prop.getClass().equals(Double.class))
                  {
                     int alpha = Utilities.rangeRestrict(0,255, (int) (255* (Double) prop));
                     if(alpha < 255) fillC = new Color(fillC.getRed(), fillC.getGreen(), fillC.getBlue(), alpha);
                  }
               }
            }
         }
      }

      // HG "SizeData" property is examined to determine the maximum symbol size for the FypML scatter plot. When 
      // symbol size does not vary, SizeData is a scalar double in square points, and the symbol size is the square root
      // of this value. When symbol size varies, SizeData is a vector containing the area of each symbol in square 
      // points. In this case, we use the square root of the maximum value. In either case, max symbol size is
      // restricted to an allowed range. Matlab R2016b appears to use 36 pt^2 as the default for scalar size.
      double markSzPts = 6;
      if(szData != null)
      {
         markSzPts = 0;
         for(double szDatum : szData) if(szDatum > markSzPts) markSzPts = szDatum;

         if(markSzPts <= 0) markSzPts = 6;
         else markSzPts = Math.sqrt(markSzPts);
      }
      if(isPointMarker) markSzPts /= 3.0;
      Measure markSz = new Measure(markSzPts, Measure.Unit.PT);
      markSz = ScatterPlotNode.MAXSYMSIZECONSTRAINTS.constrain(markSz);
      
      // we can now append the scatter plot node as a child of the graph. The node type depends on whether the graph
      // container is 2D or 3D.
      FGNodeType childType = is2D ? FGNodeType.SCATTER : FGNodeType.SCATTER3D;
      if(!graph.getGraphicModel().insertNode(graph, childType, -1))
         throw new NeverOccursException("Failed to insert new scatter plot node into FypML graph container.");
      
      FGraphicNode child = graph.getChildAt(graph.getChildCount()-1);
      if(is2D)
      {
         ScatterPlotNode spn = (ScatterPlotNode) child;
         spn.setMode(dispMode);
         spn.setDataSet(ds);
         spn.setShowInLegend(showInLegend);
         spn.setTitle(title);
         spn.setSymbol(mark);
         spn.setMaxSymbolSize(markSz);
         spn.setMeasuredStrokeWidth(strokeW);
         spn.setStrokePattern(sp);
         spn.setStrokeColor(strokeC);
         spn.setFillColor(fillC);
      }
      else
      {
         Scatter3DNode spn = (Scatter3DNode) child;
         
         Scatter3DNode.DisplayMode dm3 = null;
         switch(dispMode)
         {
         case SCATTER : dm3 = Scatter3DNode.DisplayMode.SCATTER; break;
         case SIZEBUBBLE : dm3 = Scatter3DNode.DisplayMode.SIZEBUBBLE; break;
         case COLORBUBBLE : dm3 = Scatter3DNode.DisplayMode.COLORBUBBLE; break;
         case COLORSIZEBUBBLE : dm3 = Scatter3DNode.DisplayMode.COLORSIZEBUBBLE; break;
         }
         spn.setMode(dm3);
         
         spn.setDataSet(ds);
         spn.setShowInLegend(showInLegend);
         spn.setTitle(title);
         
         // the default bkg fill for 3D scatter plot is a gradient fill. Don't expect that in a Matlab 3D scatter.
         if(fillC == null) fillC = spn.getFillColor();
         spn.setBackgroundFill(BkgFill.createSolidFill(fillC));
         
         // unlike the 2D case, the 3D scatter plot has a component SymbolNode for specifying the symbol properties.
         // Also, the 3D scatter plot's stroke width must be zero, or you get a stem plot..
         spn.setMeasuredStrokeWidth(new Measure(0, Measure.Unit.IN));
         spn.getSymbolNode().setType(mark);
         spn.getSymbolNode().setSize(markSz);
         spn.getSymbolNode().setMeasuredStrokeWidth(strokeW);
         spn.getSymbolNode().setStrokePattern(sp);
         spn.getSymbolNode().setStrokeColor(strokeC);
      }
   }

   /**
    * Append a FypML 3D scatter plot node that replicates a Matlab 3D stem plot or line plot -- as defined by the Handle 
    * Graphics "specgraph.stemseries" or "graph2d.lineseries" plot object, respectively.
    * 
    * <p>The Matlab 3D stem plot (created by the <i>stem3()</i> function) is really a scatter plot in which all symbols
    * are the same size and color, and a stem is drawn from each symbol center to a "base plane" at Z=Zo, where Zo is
    * the value of the "BaseValue" property. As such, it can be represented by a FypML {@link Scatter3DNode}. This 
    * graphic node draws stems so long as its stroke width property is non-zero. The appearance of the marker symbols
    * are governed by its component {@link SymbolNode}.</p>
    * 
    * <p>A 3D line plot in Matlab (created by the <i>plot3()</i> function) can also be represented as a FypML {@link 
    * Scatter3DNode}. The conversion is much the same, except that 3D scatter plot node is configured to connect the
    * data points with a single trace line rather than drawing stem lines.</p>
    * 
    * @param g3 The parent 3D graph node in the FypML figure being constructed. 
    * @param plotObj The HG "specgraph.stemseries" defining the stem plot, or the "graph2d.lineseries" object defining
    * the 3D line plot -- including raw data!
    */
   private static void addStemOrLinePlotToGraph(Graph3DNode g3, HGObject plotObj)
   {
      boolean isStemPlot = plotObj.getType().equals("specgraph.stemseries");
      
      // HG "DisplayName" property maps to the 3D scatter plot node's "title" property. Also, whenever "DisplayName" is 
      // defined and not an empty string, we assume that the original plot object was included in any legend associated 
      // with the axes -- so we set the node's "showInLegend" attribute.
      boolean showInLegend = false;
      String title = "";
      Object prop = plotObj.getProperty("DisplayName");
      if(prop != null && (prop.getClass().equals(String.class) || prop.getClass().equals(Character.class)))
      {
         title = processSpecialCharacters(prop.toString().trim());
         if(!title.isEmpty()) showInLegend = true;
      }

      // if HG "DisplayName" not set, we check for an undocumented property: the field 'legend_texthandle' in the HG
      // object's 'ApplicationData' property. If the field exists, the code that parses Matlab HG structure sets it as
      // a property directly on the HG object. It should be a scalar double.
      if(!showInLegend)
      {
         prop = plotObj.getProperty("legend_texthandle");
         if(prop != null && prop.getClass().equals(Double.class)) showInLegend = true;
      }

      // extract the data set from the stem plot object
      DataSet ds = plotObj.extractDataSetFromPlotObject(title, false, false);
      if(ds == null) return;

      // Matlab stem or 3D lineplot does not support varying marker size or color. Display mode is always "scatter".
      Scatter3DNode.DisplayMode dispMode = Scatter3DNode.DisplayMode.SCATTER;
      
      // HG "BaseValue" property is the stem baseplane value, a numeric scalar. The default is 0. Ignored for line plot.
      double baseline = 0;
      prop = plotObj.getProperty("BaseValue");
      if(prop != null && prop.getClass().equals(Double.class))
      {
         baseline = (Double) prop;
         if(!Utilities.isWellDefined(baseline)) baseline = 0;
      }
      
      // HG "Color" property is the line stroke color. Default is black. Marker symbols will inherit the same 
      // stroke color, unless the "MarkerEdgeColor" property is set to something other than "auto".
      Color stemStrkC = processColorSpec(plotObj.getProperty("Color"));
      if(stemStrkC == null) stemStrkC = "none".equals(plotObj.getProperty("Color")) ? TRANSPARENTBLACK : Color.BLACK;
      
      // HG "LineWidth" property gives the stroke width with which plot lines and marker symbols are stroked.    
      Measure strokeW = processLineWidth(plotObj);
      
      // HG "LineStyle" property gives the plot line style. Marker symbols, however, are always stroked solid.
      StrokePattern stemSP = processLineStyle(plotObj);
      
      // HG "Marker" property selects the marker symbol associated with the plot. The default is a circle. If the
      // value is 'none', then 'MarkerSize' is ignored and the symbol size is 0in. The "point" marker is
      // mapped to "circle".
      boolean noMarker = false;
      boolean isPointMarker = false;
      Marker mark = Marker.CIRCLE;
      prop = plotObj.getProperty("Marker");
      if(prop != null && (prop.getClass().equals(String.class) || prop.getClass().equals(Character.class)))
      {
         String s = prop.toString();
         switch(s)
         {
         case "none":
            noMarker = true;
            break;
         case ".":
            isPointMarker = true;
            break;
         case "+":
            mark = Marker.TEE;
            break;
         case "o":
            break;
         case "*":
            mark = Marker.STAR;
            break;
         case "x":
            mark = Marker.XHAIR;
            break;
         case "_":
            mark = Marker.HLINETHRU;
            break;
         case "|":
            mark = Marker.LINETHRU;
            break;
         case "square":
         case "s":
            mark = Marker.BOX;
            break;
         case "diamond":
         case "d":
            mark = Marker.DIAMOND;
            break;
         case "^":
            mark = Marker.UPTRIANGLE;
            break;
         case "v":
            mark = Marker.DOWNTRIANGLE;
            break;
         case ">":
            mark = Marker.RIGHTTRIANGLE;
            break;
         case "<":
            mark = Marker.LEFTTRIANGLE;
            break;
         case "pentagram":
         case "p":
            mark = Marker.PENTAGRAM;
            break;
         case "hexagram":
         case "h":
            mark = Marker.HEXAGRAM;
            break;
         }
      }
      
      // HG "MarkerEdgeColor" property sets the stroke color for the marker symbols. The default is 'auto', which means
      // the same color as the trace line or stem lines. Otherwise, it is "none" (fully transparent black), or a Matlab 
      // color spec.
      Color markerStrkC = stemStrkC;
      prop = plotObj.getProperty("MarkerEdgeColor");
      if(prop != null)
      {
         // could be a Matlab color spec or one of two other possible strings: "none" or "auto"
         Color c = processColorSpec(prop);
         if(c != null) markerStrkC = c;
         else if("none".equals(prop)) markerStrkC = TRANSPARENTBLACK;
      }
      
      // HG "MarkerFaceColor" property sets the fill color for the marker symbols. It can be a Matlab color spec, OR 
      // the strings "none" (hollow symbol, which is the Matlab default) or "auto" (same as the stem line color). If the
      // marker type was "point", that is mapped to a filled circle in FypML, and we force the fill color to match the 
      // stroke color.
      Color markerFillC;
      if(isPointMarker) 
         markerFillC = markerStrkC;
      else
      {
         prop = plotObj.getProperty("MarkerFaceColor");
         if(prop == null || "none".equals(prop)) markerFillC = TRANSPARENTBLACK;
         else if("auto".equals(prop))
            markerFillC = stemStrkC.equals(TRANSPARENTBLACK) ? Color.BLACK : stemStrkC;
         else
         {
            markerFillC = processColorSpec(prop);
            if(markerFillC == null) markerFillC = Color.BLACK;
         }
      }
      
      // HG "MarkerSize" property specifies the marker symbol size in typographical points. Default is 6. If the 
      // chosen marker is a point, the size is set to 1/3 the value in 'MarkerSize'. If no marker is drawn, this 
      // property is ignored and marker size will be 0.
      double markSzPts = noMarker ? 0 : 6;
      prop = plotObj.getProperty("MarkerSize");
      if((!noMarker) && prop != null && prop.getClass().equals(Double.class))
         markSzPts = (Double) prop;
      if(isPointMarker) markSzPts /= 3.0;
      Measure markSz = new Measure(markSzPts, Measure.Unit.PT);
      markSz = ScatterPlotNode.MAXSYMSIZECONSTRAINTS.constrain(markSz);
      
      // we can now append the 3D scatter plot node as a child of the graph.
      if(!g3.getGraphicModel().insertNode(g3, FGNodeType.SCATTER3D, -1))
         throw new NeverOccursException("Failed to insert new scatter plot node into FypML 3D graph.");
      
      Scatter3DNode spn = (Scatter3DNode) g3.getChildAt(g3.getChildCount()-1);
      spn.setMode(dispMode);
      spn.setDataSet(ds);
      spn.setShowInLegend(showInLegend);
      spn.setTitle(title);
      spn.setStrokeColor(stemStrkC);
      spn.setStemmed(isStemPlot);
      spn.setZBase(baseline);
      spn.setMeasuredStrokeWidth(strokeW);
      spn.setStrokePattern(stemSP);
      
      // the default bkg fill for 3D scatter plot is a gradient fill. Don't expect that in a Matlab 3D scatter.
      spn.setBackgroundFill(BkgFill.createSolidFill(markerFillC));
      
      // now configure the marker symbols. In Matlab, the symbols are always stroked solid.
      spn.getSymbolNode().setType(mark);
      spn.getSymbolNode().setSize(markSz);
      spn.getSymbolNode().setMeasuredStrokeWidth(null);  // same as the stems
      spn.getSymbolNode().setStrokePattern(StrokePattern.SOLID);
      spn.getSymbolNode().setStrokeColor(markerStrkC);
      spn.getSymbolNode().setFillColor(markerFillC);
   }


   /**                    
    * Append a <i>FypML</i> bar plot node to the graph to render the a Matlab bar plot, as represented by the 
    * HG 'specgraph.barseries' object. 
    * 
    * <p>In Matlab, each data "group" in a bar plot is represented by a separate 'specgraph.barseries' object, while in
    * FypML these bar groups are all part of the same FypML 'bar' element. As the HG tree structure is parsed, all 
    * 'barseries' objects that appear to belong to the same bar plot are "coalesced" into the first 'barseries' object;
    * the Y-data, legend labels, and fill colors of the different bar groups are all collected into the coalesced 
    * 'barseries' object. If the 'barseries' object is not coalesced, then it represents a bar plot with a single data
    * group.
    * 
    * <p>Below is a summary of how the properties of the 'barseries' HG object B are processed to convert it to a 
    * <i>FypML</i> bar plot node. 
    * <ul>
    * <li>B.BarLayout and B.Horizontal determine the bar plot's display mode.</li>
    * <li>B.BaseValue is a scalar double that sets the baseline for the bar plot. If not present, 0 is assumed.</li>
    * <li>B.actualBW is the actual width of each individual bar in the bar plot. It is NOT a property of the original
    * Matlab bar series object. Instead, it is computed by analyzing the child 'patch' object that actually renders the
    * bars. This computed property is set as each individual bar series is processed, prior to coalescing. In fact,
    * this property, along with B.BarLayout, B.Horizontal and B.BaseValue, is checked when considering whether or not
    * two sibling bar series objects should be coalesced. Furthermore, B.actualBW is used to set the relative bar
    * width for the <i>FypML</i> bar plot. While the Matlab property B.BarWidth gives this value directly, there are
    * scenarios in which B.BarWidth does not accurately reflect the true relative bar width of the coalesced bar
    * groups. Therefore, we prefer to calculate the relative bar width R from the actual bar width W, the minimum 
    * interval D in the X data, the number of data groups N in the bar plot, and the bar plot layout. The algorithm used
    * is based on the observed behavior of Matlab bar plots. For a "stacked" bar plot, W = D*R. For "grouped" plots, 
    * W = D*R if N=1; W = 2*D*R/(N+3) for N=2..5; and W = 0.8*D*R/N for N&ge;6. Once calculated, R is restricted to the 
    * range [0.05 .. 1.0].</li>
    * <li>B.EdgeColor, B.EdgeAlpha, B.LineStyle, and B.LineWidth define how the bars are stroked. All bar groups are 
    * stroked in the same fashion. Unlike Matlab, FC does not support different strokes for different bar groups.</li>
    * <li>The underlying data is obtained directly from {@link HGObject#extractDataSetFromPlotObject}; it will be an
    * MSET or MSERIES source for a multi-group bar plot; PTSET or SERIES for a single-group plot.</li>
    * <li>The data group legend labels and fill colors are extracted from the 'barseries' object via {@link
    * HGObject#getDataGroupLabels()} and {@link HGObject#getDataGroupFillColors}. Since fill colors
    * may be specified as direct or scaled color map indices, figure color map information is supplied to the latter
    * method.</li>
    * </ul>
    * </p>
    * 
    * @param graph The parent graph node in the FypML figure being constructed. Cannot be a polar graph, as bar plots
    * aren't supported there.
    * @param hgBar The HG "specgraph.barseries" object representing the bar plot. If it represents a multi-group bar
    * plot, the Y-data, bar group colors, and bar group legend labels will have all been coalesced into this 'single'
    * 'barseries' object (the others having been discarded during HG processing).
    * @param matCM The Matlab figure's colormap. We need this to process the bar group colors, in case any were defined
    * as color map indices instead of RGB colors.
    * @param cLim The portion of the Matlab figure's colormap assigned to the Matlab axes object -- needed to process
    * an scale color map indices. Should be a two-element array.
    */
   private static void addBarPlotToGraph(GraphNode graph, HGObject hgBar, double[][] matCM, double[] cLim)
   {
      // B.BarLayout and B.Horizontal determine the bar plot node's display mode
      Object prop = hgBar.getProperty("BarLayout");
      boolean isGrp = (prop == null) || "grouped".equals(prop);
      prop = hgBar.getProperty("Horizontal");
      boolean isVert = (prop == null) || "off".equals(prop);
      BarPlotNode.DisplayMode dispMode;
      if(isGrp) dispMode = (isVert) ? BarPlotNode.DisplayMode.VGROUP : BarPlotNode.DisplayMode.HGROUP;
      else dispMode = (isVert) ? BarPlotNode.DisplayMode.VSTACK : BarPlotNode.DisplayMode.HSTACK;
      
      // prepare the underlying data set from the coalesced Y-data.
      int n = ((int) (Math.random() * 3000.0)) % 100;
      String id = "src_" + n;
      DataSet ds = hgBar.extractDataSetFromPlotObject(id, false, false);
      if(ds == null) return;
      
      // the bar plot node's baseline attribute is the value of the scalar property B.BaseValue (default = 0)
      double baseline = 0;
      prop = hgBar.getProperty("BaseValue");
      if(prop != null && prop.getClass().equals(Double.class))
      {
         baseline = (Double) prop;
         if(!Utilities.isWellDefined(baseline)) baseline = 0;
      }
      
      // use the computed property B.actualBW to determine relative bar width. If B.actualBW is not available (it
      // should be), use the bar-series property B.BarWidth. The relative bar width is expressed as an integer pct.
      int barWidth = -1;
      prop = hgBar.getProperty(HGObject.ACTUALBW_PROP);
      if(prop != null && prop.getClass().equals(Double.class))
      {
         double w = (Double) prop;
         
         // examine X coordinates for the minimum interval in X. The X-coordinates must be monotonically increasing
         // or decreasing. If not, then we cannot calculate a relative bar width and we stick with the default 80%.
         double minDX = Double.NaN;
         int nx = 0;
         double lastX = 0;
         for(int i=0; i<ds.getDataLength(); i++) 
         {
            double x = ds.getX(i, 0);
            if(Utilities.isWellDefined(x))
            {
               ++nx;
               if(nx == 1) lastX = x;
               else if(lastX == x)
               {
                  // no two X-coordinates can be the same
                  minDX = Double.NaN;
                  break;
               }
               else if(nx == 2)
                  minDX = x - lastX;
               else
               { 
                  double dx = x - lastX;
                  if(dx * minDX < 0) 
                  {
                     // the X-coordinate sequence must be monotonically increasing or decreasing!
                     minDX = Double.NaN;
                     break;
                  }
                  else if(dx > 0 && dx < minDX) minDX = dx;
                  else if(dx < 0 && dx > minDX) minDX = dx;
               }
            }
         }
         
         if(Utilities.isWellDefined(minDX) && minDX != 0)
         {
            int nGrps = ds.getNumberOfSets();
            if(!isGrp)
            {
               // for "stacked" layout, there's just one bar per X-coordinate
               barWidth = (int) (100.0 * Math.abs(w/minDX) + 0.5);
               barWidth = Utilities.rangeRestrict(BarPlotNode.MINRELBW, BarPlotNode.MAXRELBW, barWidth);
            }
            else if(nx > 1 && nGrps >= 1)
            {
               double extent = minDX;
               if(nGrps >= 2 && nGrps < 6) extent = minDX * 2 * nGrps / (2*nGrps + 3);
               else if(nGrps >= 6) extent = 0.8 * minDX;
               
               if(extent < 0) extent = -extent;
               barWidth = (int) (100.0 * w * nGrps / extent + 0.5);
               barWidth = Utilities.rangeRestrict(BarPlotNode.MINRELBW, BarPlotNode.MAXRELBW, barWidth);
            }
         }
      }
      
      // if we could not calculate relative bar width from the actual observed bar width, use the B.BarWidth property.
      // If that's not defined, assume the default value, 0.8 or 80%.
      if(barWidth == -1)
      {
         prop = hgBar.getProperty("BarWidth");
         if(prop != null && prop.getClass().equals(Double.class))
         {
            barWidth = (int) (100.0 * (Double) prop + 0.5);
            barWidth = Utilities.rangeRestrict(BarPlotNode.MINRELBW, BarPlotNode.MAXRELBW, barWidth);
         }
         else
            barWidth = 80;
      }
      
      // B.LineWidth, B.LineStyle, B.EdgeColor, and B.EdgeAlpha properties determine stroke properties for the bar plot 
      // node. The stroke color is black if B.EdgeColor is implicit, invalid, or set to the unsupported values "flat" or
      // "interp". If either B.LineStyle or B.EdgeColor is set to "none", then the stroke width is set to 0.
      Measure strokeW = processLineWidth(hgBar);
      StrokePattern sp = processLineStyle(hgBar);
      prop = hgBar.getProperty("EdgeColor");
      Color strokeC = null;
      if(!"none".equals(prop))
      {
         strokeC = processColorSpec(prop);
         if(strokeC == null) strokeC = Color.BLACK;
      }
      if(sp != null && strokeC != null)
      {
         prop = hgBar.getProperty("EdgeAlpha");
         if(prop != null && prop.getClass().equals(Double.class))
         {
            int alpha = Utilities.rangeRestrict(0, 255, (int) (255.0 * (Double) prop));
            if(alpha < 255) strokeC = new Color(strokeC.getRed(), strokeC.getGreen(), strokeC.getBlue(), alpha);
         }
      }
      if(sp == null || strokeC == null)
      {
         sp = StrokePattern.SOLID;
         strokeC = Color.BLACK;
         strokeW = new Measure(0, Measure.Unit.PT);
      }

      // get the bar group legend labels and fill colors, which were collected during coalescing. We supply the figure's
      // color map information to convert any fill colors that were specified as color map indices. If all labels are
      // null, then the bar plot's "show in legend" flag is cleared. Otherwise, any missing labels are set to "group I",
      // where I is the bar group index.
      List<Color> grpColors = hgBar.getDataGroupFillColors(matCM, cLim);
      List<String> grpLabels = hgBar.getDataGroupLabels();
      boolean showInLegend = false;
      for(String label : grpLabels) if(label != null)
      {
         showInLegend = true;
         break;
      }
      for(int i=0; i<grpLabels.size(); i++)
      {
         String label = grpLabels.get(i);
         if(label == null) grpLabels.set(i, "group " + i);
      }
      
      // append the bar plot node to the graph, then set its data set and configure its properties appropriately
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.BAR, -1))
         throw new NeverOccursException("Failed to insert new bar plot node into FypML graph.");
      
      BarPlotNode bp = (BarPlotNode) graph.getChildAt(graph.getChildCount()-1);
      bp.setDataSet(ds);
      bp.setTitle("");
      bp.setShowInLegend(showInLegend);
      bp.setMode(dispMode);
      bp.setBarWidth(barWidth);
      bp.setBaseline((float) baseline);
      for(int i=0; i<grpLabels.size(); i++)
      {
         bp.setDataGroupLabel(i, processSpecialCharacters(grpLabels.get(i)));
         bp.setDataGroupColor(i, grpColors.get(i));
      }
      bp.setStrokeColor(strokeC);
      bp.setStrokePattern(sp);
      bp.setMeasuredStrokeWidth(strokeW);
   }
   
   /**
    * Append a FypML line segment node to the graph to render the baseline for a Matlab bar or stem plot. The Handle
    * Graphics "specgraph.baseline" object is a specialized object that, in Matlab, represents the common baseline for 
    * any and all "specgraph.barseries" objects (for bar plot) or "specgraph.stemseries" (for stem plot) in the same 
    * axes.
    * 
    * <p>We only check the 'Color', 'LineStyle', 'LineWidth', and 'Visible' properties of the Matlab baseline object;
    * if any of these are missing, default values are assigned (black, solid, 0.5pt, and 'on'). Note that the line 
    * segment will not be added if the baseline object's 'Visible' property is "off".</p>
    * 
    * <p>In a bar plot, the orientation (H or V) and base line value (y=Yo for H, x=Xo for V) are determined by 
    * consulting the first FypML bar plot node in the parent graph. If there isn't already a bar plot node in the graph 
    * (there should be), then it is assumed that the line's orientation is H and the base line value is 0.</p>
    * 
    * <p>In a stem plot, the line segment is always horizontal, and the baseline value should be found in the 'baseline'
    * object's "BaseValue" property.</p>
    * 
    * @param graph The parent graph node in the FypML figure being constructed.
    * @param hgBase The HG "specgraph.baseline" object defining the properties of the bar or stem plot base line.
    */
   private static void addBarOrStemPlotBaselineToGraph(GraphNode graph, HGObject hgBase)
   {
      // if baseline object has 'Visible' property set to 'off', then don't add the line node!
      Object visProp = hgBase.getProperty("Visible");
      if("off".equals(visProp)) return;
      
      // get properties governing appearance of the line segment: "Color", "LineStyle", "LineWidth". If line style is
      // "none", the line segment is not added.
      Color c = processColorSpec(hgBase.getProperty("Color"));
      Measure strokeW = processLineWidth(hgBase);
      StrokePattern sp = processLineStyle(hgBase);
      if(sp == null) return;
      
      // decide if we're dealing with a stem or bar plot. If there's a FypML 'bar' node in the graph, assume the latter;
      // else assume the former.
      boolean isBarPlot = false;
      for(int i=0; i<graph.getChildCount(); i++) if(graph.getChildAt(i).getNodeType() == FGNodeType.BAR)
      {
         isBarPlot = true;
         break;
      }
      
      // determine the base line value and orientation of the line segment
      double baseValue = 0;
      boolean isH = true;
      if(!isBarPlot)
      {
         Object prop = hgBase.getProperty("BaseValue");
         if(prop != null && prop.getClass().equals(Double.class)) baseValue = (Double) prop;
      }
      else for(int i=0; i<graph.getChildCount(); i++)
      {
         FGraphicNode child = graph.getChildAt(i);
         if(child.getNodeType() == FGNodeType.BAR)
         {
            BarPlotNode bpn = (BarPlotNode) child;
            baseValue = bpn.getBaseline();
            BarPlotNode.DisplayMode mode = bpn.getMode();
            isH = (mode == BarPlotNode.DisplayMode.VGROUP || mode == BarPlotNode.DisplayMode.VSTACK);
            break;
         }
      }
      
      // determine endpoint coordinates for line segment using base line value and range of parallel axis
      double x0, x1, y0, y1;
      if(isH)
      {
         y0 = baseValue;
         y1 = baseValue;
         x0 = graph.getPrimaryAxis().getStart();
         x1 = graph.getPrimaryAxis().getEnd();
      }
      else
      {
         x0 = baseValue;
         x1 = baseValue;
         y0 = graph.getSecondaryAxis().getStart();
         y1 = graph.getSecondaryAxis().getEnd();
      }
      
      // now add the line segment and set its properties appropriately
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.LINE, -1))
         throw new NeverOccursException("Failed to insert new line segment node into FypML graph.");
      
      LineNode line = (LineNode) graph.getChildAt(graph.getChildCount()-1);
      
      // the line segment's endpoints are always in "user units" WRT the graph's coord system. We assume the graph is
      // Cartesian, as a Matlab bar or stem plot is not appropriate in a polar graph. Matlab can spit out doubles with 
      // very small fractional digits, but FypML measured values are limited to a certain # of fractional digits, and 
      // the line node will reject invalid measures -- so we constrain them.
      Measure.Constraints mc = FGraphicModel.getLocationConstraints(FGNodeType.LINE);
      
      line.setXY(mc.constrain(new Measure(x0, Measure.Unit.USER)), 
            mc.constrain(new Measure(y0, Measure.Unit.USER)));
      line.setX2(mc.constrain(new Measure(x1, Measure.Unit.USER)));
      line.setY2(mc.constrain(new Measure(y1, Measure.Unit.USER)));
      
      // set line drawing attributes
      if(c != null) line.setStrokeColor(c);
      line.setMeasuredStrokeWidth(strokeW);
      line.setStrokePattern(sp);
   }

   /**                    
    * Append a <i>FypML</i> area chart node to the graph to render a Matlab area chart, as represented by the HG
    * 'specgraph.areaseries' object. A typical area chart will have two or more bands stacked upon each other, with each
    * band representing a distinct "data group". In the Matlab area chart, each band is defined by a single 'areaseries'
    * object which, in turn, contains a single 'patch' child that renders that band. All 'areaseries' objects that are 
    * part of one stacked area chart share a common baseline, and these are coalesced into a single 'areaseries' when 
    * the HG tree structure is parsed; the Y-data, legend labels, and fill colors of the different data bands are all 
    * collected into the coalesced 'areaseries' object. It is possible, of course, that the area chart contains only a
    * single band, represented by a singleton 'areaseries' object.
    * 
    * <p>Below is a summary of how the properties of the 'areaseries' HG object B -- whether it be coalesced or a 
    * singleton -- are processed to convert it to a <i>FypML</i> area chart node. 
    * <ul>
    * <li>B.BaseValue is a scalar double that sets the baseline for the area chart. If not present, 0 is assumed.</li>
    * <li>B.EdgeColor, B.EdgeAlpha, B.LineStyle, and B.LineWidth define how the area chart is stroked. All bands are 
    * stroked in the same fashion. Unlike Matlab, FC does not support different strokes for different bands.</li>
    * <li>The underlying data is already coalesced in B; the MSET or MSERIES source is obtained from it directly via
    * {@link HGObject#extractDataSetFromPlotObject}. If a B is a singleton 'areaseries', the data
    * source will be a PTSET or SERIES.</li>
    * <li>The legend label and fill color for each band in the area chart are collected as the related 'areaseries'
    * objects are coalesced into one. They are obtained directly from the coalesced object via {@link 
    * HGObject#getDataGroupLabels()} and {@link HGObject#getDataGroupFillColors}. Since fill colors
    * may be specified as direct or scaled color map indices, figure color map information is supplied to the latter
    * method.</li>
    * </ul>
    * </p>
    * 
    * @param graph The parent graph node in the FypML figure being constructed.
    * @param hgArea The HG "specgraph.areaseries" object representing the coalesced set of area-series objects defining
    * the area chart -- or a singleton if the area chart only contains one data group. The Y-data, data group colors, 
    * and data group legend labels have all been collected in this object.
    * @param matCM The Matlab figure's colormap. We need this to process the data group colors, in case any were defined
    * as color map indices instead of RGB colors.
    * @param cLim The portion of the Matlab figure's colormap assigned to the Matlab axes object -- needed to process
    * any scaled color map indices. Should be a two-element array.
    */
   private static void addAreaChartToGraph(GraphNode graph, HGObject hgArea, double[][] matCM, double[] cLim)
   {
      // prepare the underlying data set from the coalesced Y-data.
      int n = ((int) (Math.random() * 3000.0)) % 100;
      String id = "src_" + n;
      DataSet ds = hgArea.extractDataSetFromPlotObject(id, graph.isPolar(), false);
      if(ds == null) return;
      
      // the area chart node's baseline attribute is the value of the scalar property B.BaseValue (default = 0).
      double baseline = 0;
      Object prop = hgArea.getProperty("BaseValue");
      if(prop != null && prop.getClass().equals(Double.class))
      {
         baseline = (Double) prop;
         if(!Utilities.isWellDefined(baseline)) baseline = 0;
      }
      else if(prop == null)
      {
         // when the Matlab figure is sourced from a FIG file, the BaseValue property is NOT stored. As an extra check, 
         // look at the first Y-coordinate value in the YCoords property, which should be a double-valued vector.
         double[] ycoords = HGObject.getDoubleVectorFromProperty(hgArea.getProperty("YCoords"));
         if(ycoords != null && ycoords.length > 0) baseline = ycoords[0];
      }
      
      // B.LineWidth, B.LineStyle, B.EdgeColor, B.EdgeAlpha properties determine stroke properties for the area chart 
      // node. The stroke color is black if B.EdgeColor is implicit, invalid, or set to the unsupported values "flat" or
      // "interp". If B.LineStyle or B.EdgeColor = "none", then the stroke width is set to 0 and B.EdgeAlpha is ignored.
      Measure strokeW = processLineWidth(hgArea);
      StrokePattern sp = processLineStyle(hgArea);
      prop = hgArea.getProperty("EdgeColor");
      Color strokeC = null;
      if(!"none".equals(prop))
      {
         strokeC = processColorSpec(prop);
         if(strokeC == null) strokeC = Color.BLACK;
      }
      if(sp != null && strokeC != null)
      {
         prop = hgArea.getProperty("EdgeAlpha");
         if(prop != null && prop.getClass().equals(Double.class))
         {
            int alpha = Utilities.rangeRestrict(0, 255, (int) (255.0 * (Double) prop));
            if(alpha < 255) strokeC = new Color(strokeC.getRed(), strokeC.getGreen(), strokeC.getBlue(), alpha);
         }
      }
      if(sp == null || strokeC == null)
      {
         sp = StrokePattern.SOLID;
         strokeC = Color.BLACK;
         strokeW = new Measure(0, Measure.Unit.PT);
      }

      // get the data group legend labels and fill colors, which were collected during coalescing. (If the area chart
      // contains only a single data group, each list will only have one entry. We supply the figure's color map 
      // information to convert any fill colors that were specified as color map indices. If all labels are null, then 
      // the area chart node's "show in legend" flag is cleared. Otherwise, any missing labels are set to "group I",
      // where I is the data group index.
      List<Color> grpColors = hgArea.getDataGroupFillColors(matCM, cLim);
      List<String> grpLabels = hgArea.getDataGroupLabels();
      boolean showInLegend = false;
      for(String label : grpLabels) if(label != null)
      {
         showInLegend = true;
         break;
      }
      for(int i=0; i<grpLabels.size(); i++)
      {
         String label = grpLabels.get(i);
         if(label == null) grpLabels.set(i, "group " + i);
      }
      
      // append the area chart node to the graph, then set its data set and configure its properties appropriately
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.AREA, -1))
         throw new NeverOccursException("Failed to insert new area chart node into FypML graph.");
      
      AreaChartNode ac = (AreaChartNode) graph.getChildAt(graph.getChildCount()-1);
      ac.setDataSet(ds);
      ac.setTitle("");
      ac.setShowInLegend(showInLegend);
      ac.setBaseline((float) baseline);
      for(int i=0; i<grpLabels.size(); i++)
      {
         ac.setDataGroupLabel(i, processSpecialCharacters(grpLabels.get(i)));
         ac.setDataGroupColor(i, grpColors.get(i));
      }
      ac.setStrokeColor(strokeC);
      ac.setStrokePattern(sp);
      ac.setMeasuredStrokeWidth(strokeW);
   }
   
   /**
    * Append a data trace ({@link TraceNode TraceNode} to the graph that represents a Matlab
    * histogram, as defined by a Matlab Handle Graphics "patch" plot object. 
    * <p>The "patch" object can be used to render many graphic entities other than a histogram, so this method first 
    * checks the patch properties to see if they are consistent with the following constraints.
    * <ul>
    * <li>The following properties must be explicit: 'Faces', 'Vertices'.</li>
    * <li>The 'Faces' property must be an Nx4 matrix. Each of the N "faces" is a histogram "bar" defined by its four 
    * corners: <i>p1, p2, p3, p4</i>. Note that each element in 'Faces' is an index into the 'Vertices' array.</li>
    * <li>The 'Vertices' property must be an <i>Mx2</i> or <i>Mx3</i> matrix, where <i>M = N*5 + 1</i>. The three 
    * columns contain the x-, y-, and z-coordinates of the face vertices. The third column may not be present; it will
    * be ignored regardless.</li>
    * <li>For every "bar", its vertices satisfy: <i>p1.x = p2.x, p3.x = p4.x, p2.y = p3.y, p1.y = p4.y = 0</i>.</li>
    * <li>Across all bars, <i>p3.x - p2.x</i> must have APPROXIMATELY THE SAME value. This is the histogram's bar width,
    * <i>B</i>. To allow for some jitter in the individual bar widths, we take the first bar's width as <i>B</i> and 
    * verify that all of the other bar widths are within 0.1% of this value.</li>
    * </ul>
    * If any of these constraints are violated, then the method takes no action, and the patch object is simply ignored.
    * </p>
    * <p>The 'EdgeColor', 'LineStyle', and 'LineWidth' properties define how the faces -- i.e., bars -- of the histogram
    * are stroked, while the 'FaceColor' and 'FaceVertexCData' properties define how they are filled. There's a lot of
    * flexibility in defining how each face is filled: each face can have a different color, and gradient fills are also
    * possible. However, the translated FypML histogram has only a single fill color for all bars. The alpha component
    * of that single fill color will be set IAW the patch's 'FaceAlpha' property, but only if that property is a scalar;
    * the values 'flat' and 'interp' are NOT supported.</p>
    * 
    * @param graph The parent graph node in the FypML figure being constructed.
    * @param patchObj The HG "patch" object rendering the histogram. 
    * @param matCM The Matlab figure's colormap. We need this to process an indexed color specification.
    * @param cLim The portion of the Matlab figure's colormap assigned to the Matlab axes object -- needed to process
    * an indexed color when the patch's "CDataMapping" property is "scaled". Should be a two-element array.
    * @return True if patch was successfully translated as a FypML trace in histogram mode.
    */
   private static boolean addPatchAsHistogram(GraphNode graph, HGObject patchObj, double[][] matCM, double[] cLim)
   {
      // first verify that patch object is consistent with a histogram rendering.
      double[][] faces = null;
      double[][] vertices = null;
      Object prop = patchObj.getProperty("Faces");
      boolean ok = (prop != null) && prop.getClass().equals(double[][].class);
      if(ok)
      {
         faces = (double[][]) prop;
         ok = faces.length > 1;
         for(int i=0; ok && i < faces.length; i++) ok = (faces[i] != null) && (faces[i].length == 4);
      }

      if(ok)
      {
         prop = patchObj.getProperty("Vertices");
         ok = (prop != null) && prop.getClass().equals(double[][].class);
         if(ok)
         {
            vertices = (double[][]) prop;
            ok = (vertices.length == (faces.length * 5 + 1));
            for(int i=0; ok && i < vertices.length; i++) 
               ok = (vertices[i] != null) && (vertices[i].length == 2 || vertices[i].length == 3);
         }
      }
      
      // verify that p1.x == p2.x, p3.x == p4.x, p2.y == p3.y, and p1.y == p4.y == 0 for all bars, and that the bar
      // width B ~= p3.x - p2.x is the same across all bars. At the same time we can prepare the raw data set for the
      // trace node we construct!
      int nBars = ok ? faces.length : 0;
      int nVerts = ok ? (nBars * 5 + 1) : 0;
      double barWidth = 0;
      float x0 = 0;
      float dx = 0;
      float[] yData = new float[nBars];
      for(int i=0; ok && i<faces.length; i++)
      {
         int v1 = ((int) faces[i][0]) - 1;
         int v2 = ((int) faces[i][1]) - 1;
         int v3 = ((int) faces[i][2]) - 1;
         int v4 = ((int) faces[i][3]) - 1;
         ok = (v1>=0 && v1<nVerts) && (v2>=0 && v2<nVerts) && (v3>=0 && v3<nVerts) && (v4>=0 && v4<nVerts);
         if(!ok) break;
         
         ok = (vertices[v1][0] == vertices[v2][0]) && (vertices[v3][0] == vertices[v4][0]) && 
               (vertices[v2][1] == vertices[v3][1]) && (vertices[v1][1] == 0) && (vertices[v4][1] == 0);
         if(ok)
         {
            if(i == 0)
            {
               barWidth = vertices[v3][0] - vertices[v2][0];
               x0 = (float) (vertices[v2][0] + barWidth/2.0);
            }
            else
            {
               ok = (Math.abs((vertices[v3][0] - vertices[v2][0]) - barWidth) / barWidth) <= 0.001;
               if(ok && i == 1) dx = (float) (vertices[v2][0] + barWidth/2.0 - x0);
            }
         }
         if(ok) yData[i] = (float) vertices[v2][1];
      }

      // we skip the 'patch' object if any constraint above is violated
      if(!ok) return(false);
      
      // append the trace node to the graph, initially associated with an empty data set
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.TRACE, -1))
         throw new NeverOccursException("Failed to insert new trace node into FypML graph.");
      
      TraceNode trace = (TraceNode) graph.getChildAt(graph.getChildCount()-1);
      
      // put it in histogram display mode and set histogram-specific attributes
      trace.setMode(DisplayMode.HISTOGRAM);
      trace.setBarWidth((float) barWidth);
      trace.setBaseline(0f);
      
      // HG "DisplayName" property maps to the trace node's "title" property. Also, whenever "DisplayName" is defined
      // and not an empty string, we assume that the original plot object was included in any legend associated with
      // the axes -- so we set the trace's "showInLegend" attribute.
      boolean showInLegend = false;
      Object nameProp = patchObj.getProperty("DisplayName");
      if(nameProp != null && (nameProp.getClass().equals(String.class) || nameProp.getClass().equals(Character.class)))
      {
         String title = processSpecialCharacters(nameProp.toString().trim());
         trace.setTitle(title);
         if(!title.isEmpty()) showInLegend = true;
      }

      // if HG "DisplayName" not set, we check for an undocumented property: the field 'legend_texthandle' in the HG
      // object's 'ApplicationData' property. If the field exists, the code that parses Matlab HG structure sets it as
      // a property directly on the HG object. It should be a scalar double.
      if(!showInLegend)
      {
         prop = patchObj.getProperty("legend_texthandle");
         if(prop != null && prop.getClass().equals(Double.class)) showInLegend = true;
      }
      trace.setShowInLegend(showInLegend);
      
      // prepare the raw "series" data set for the histogram
      String id = null;
      if(DataSet.isValidIDString(trace.getTitle()))
      {
         id = "src_" + trace.getTitle();
         if(!DataSet.isValidIDString(id)) id = null;
      }
      if(id == null)
      {
         int n = ((int) (Math.random() * 3000.0)) % 100;
         id = "src_" + n;
      }
      DataSet ds = DataSet.createDataSet(id, Fmt.SERIES, new float[] {dx, x0} , yData.length, 1, yData);
      if(ds == null)
      {
         graph.getGraphicModel().deleteNode(trace);
         return(false);
      }
      else trace.setDataSet(ds);
      
      // HG "LineWidth", "LineStyle", and "EdgeColor" properties determine stroke properties for trace node. The stroke
      // color is black if "EdgeColor" is implicit, invalid, or set to the unsupported values "flat" or "interp". If 
      // either "LineStyle" or "EdgeColor" is set to "none", then the stroke width is set to 0.
      Measure strokeW = processLineWidth(patchObj);
      StrokePattern sp = processLineStyle(patchObj);
      prop = patchObj.getProperty("EdgeColor");
      Color strokeC = null;
      if(!"none".equals(prop))
      {
         strokeC = processColorSpec(prop);
         if(strokeC == null) strokeC = Color.BLACK;
      }
      if(sp == null || strokeC == null)
      {
         sp = StrokePattern.SOLID;
         strokeC = Color.BLACK;
         strokeW = new Measure(0, Measure.Unit.PT);
      }
      trace.setStrokeColor(strokeC);
      trace.setStrokePattern(sp);
      trace.setMeasuredStrokeWidth(strokeW);
      
      // HG "FaceColor", "FaceVertexCData", and "CDataMapping" determine the fill color for the histogram bars. First
      // process "FaceColor" to determine if we need to look at the other properties: implicit ==> use black as the fill
      // color; "none" ==> no fill (fill color is transparent); ColorSpec ==> use that color as the fill color; 
      // "flat" ==> need to look at the other two properties; "interp" ==> not supported; map to "flat".
      prop = patchObj.getProperty("FaceColor");
      Color fillC = (prop == null) ? Color.BLACK : processColorSpec(prop);
      if(fillC == null && ("flat".equals(prop) || "interp".equals(prop)))
      {
         // get the "FaceVertexCData" property. We just want to get one color out of it, since we don't support 
         // different colors per face or per vertex. It may be indexed or true color, which complicates things. If it
         // is missing, then we use black as the fill color.
         int colorIdx = -1;
         prop = patchObj.getProperty("FaceVertexCData");
         if(prop == null) 
            fillC = Color.BLACK;
         else if(prop.getClass().equals(Double.class))
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
         
         // handle the indexed color case. We need access to the figure's color map, and we need the "CLim" property
         // for the Matlab "axes" if the mapping is scaled rather than direct.
         if(fillC == null)
         {
            if(matCM == null || colorIdx == -1 ) fillC = Color.BLACK;
            else
            {
               prop = patchObj.getProperty("CDataMapping");
               if(prop == null || "scaled".equals(prop))
               {
                  // scaled color mapping. Need to transform color index into a direct index into figure's colormap.
                  // If the axes' "CLim" property is not valid, then just use the first color.
                  if(cLim == null || cLim.length != 2) 
                     colorIdx = 1;
                  else
                  {
                     double d = ((double) (colorIdx - 1)) / (cLim[1] - cLim[0]);
                     d *= matCM.length;
                     colorIdx = Utilities.rangeRestrict(1, matCM.length, ((int) d) + 1);
                  }
               }
               
               // at this point, we should have a direct index into color map. Use it to find the fill color.
               // Remember that Matlab indices are 1-based, not 0-based!
               colorIdx = Utilities.rangeRestrict(0, matCM.length-1, colorIdx-1);
               double[] rgb = matCM[colorIdx];
               fillC = new Color((float) rgb[0], (float) rgb[1], (float) rgb[2]);
            }
         }
      }
      
      // HG "FaceAlpha" sets the alpha component for the fill color between 0 and 1. The default is 1. "flat" and 
      // "interp" are not supported. Ignored if "FaceColor" = "none".
      if(fillC == null)
         fillC = new Color(0, 0, 0, 0);
      else
      {
         prop = patchObj.getProperty("FaceAlpha");
         if(prop != null && prop.getClass().equals(Double.class))
         {
            double alpha = (Double) prop;
            if(alpha < 1.0)
            {
               int iAlpha = Utilities.rangeRestrict(0, 255, (int) (255 *alpha));
               fillC = new Color(fillC.getRed(), fillC.getGreen(), fillC.getBlue(), iAlpha);
            }
         }
      }
      
      trace.setFillColor(fillC);
      
      return(true);
   }
   
   /**
    * Append a data trace ({@link TraceNode TraceNode} to the graph that represents an "error band"
    * rendered by a Matlab Handle Graphics "patch" plot object. This method first checks the patch properties to see if 
    * they are consistent with the rendering of an error band:
    * <ul>
    * <li>The following properties must be explicit: 'Faces', 'Vertices'.</li>
    * <li>The 'Faces' property must be an 1xN array, representing a single face with N vertices. N&ge;4 must be even,
    * and the array must have the form [1 2 3 .. N].</li>
    * <li>The 'Vertices' property must be an <i>Nx2</i> matrix, V. Each column contains a point (X-coordinate in first 
    * row, Y in second) on the boundary of the error band. Furthermore, for i = 0 .. N/2 - 1, V(i, 1) == V(N-i, 1).
    * Thus, for each unique X-coordinate in the error band there are two Y-coordinates, one for a point on the upper
    * half of the error band boundary and one on the lower half.</li>
    * </ul>
    * If any of these constraints are violated, then the method takes no action, and the patch object is simply ignored.
    *
    * <p>Otherwise, the patch object is translated as a FypML trace node in the "errorband" display mode. The underlying
    * data set will contain N/2 points with a standard deviation in Y at each point, {x, y, yStd}. For i=0 .. N/2-1 :
    * <ul>
    * <li>x(i) = V(i,1)</li>
    * <li>y(i) = ( V(i,2) + V(N-i,2) ) / 2.0</li>
    * <li>yStd(i) = abs( V(N-i,2) - V(i,2) ) / 2.0</li>
    * </ul>
    * The data set will be a PTSET or SERIES, depending on the nature of the X coordinate values.
    * 
    * <p>The 'EdgeColor', 'LineStyle', and 'LineWidth' properties define how the error band is stroked, while the 
    * 'FaceColor' and 'FaceAlpha' properties determine the ARGB fill color. Note that 'FaceColor' must be 'none' (fully
    * tranparent) or an RGB color spec; the special values 'flat' and 'interp' are NOT supported and will be mapped to
    * black. 'FaceAlpha' must be a scalar in [0..1] (default is 1, ie., opaque); again, 'flat' and 'interp' are NOT 
    * supported and are mapped to alpha = 1.</p>
    * 
    * @param graph The parent graph node in the FypML figure being constructed.
    * @param patchObj A HG "patch" object. 
    * @return True if patch object was successfully translated as an errorband trace.
    */
   private static boolean addPatchAsErrorBand(GraphNode graph, HGObject patchObj)
   {
      // first verify that patch object is consistent with an errorband rendering.
      int n = 0;
      double[][] vertices = null;
      Object prop = patchObj.getProperty("Faces");
      boolean ok = (prop != null) && prop.getClass().equals(double[].class);
      if(ok)
      {
         double[] faces = (double[]) prop;
         n = faces.length;
         ok = (n >= 4) && ((n % 2) == 0);
         for(int i=0; ok && i < faces.length; i++) ok = (faces[i] == i + 1);
      }
      
      if(ok)
      {
         prop = patchObj.getProperty("Vertices");
         ok = (prop != null) && prop.getClass().equals(double[][].class);
         if(ok)
         {
            vertices = (double[][]) prop;
            ok = (vertices.length == n);
            for(int i=0; ok && i < vertices.length; i++) ok = (vertices[i] != null) && (vertices[i].length == 2);

            for(int i=0; ok && i < n/2; i++) ok = vertices[i][0] == vertices[n-1-i][0]; 
         }
      }

      // we skip the 'patch' object if any constraint above is violated
      if(!ok) return(false);
      
      // append the trace node to the graph, initially associated with an empty data set
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.TRACE, -1))
         throw new NeverOccursException("Failed to insert new trace node into FypML graph.");
      
      TraceNode trace = (TraceNode) graph.getChildAt(graph.getChildCount()-1);
      
      // put it in errorband display mode
      trace.setMode(DisplayMode.ERRORBAND);
      ErrorBarNode ebar = trace.getErrorBarNode();
      ebar.setHide(false);
      
      // HG "DisplayName" property maps to the trace node's "title" property. Also, whenever "DisplayName" is defined
      // and not an empty string, we assume that the original plot object was included in any legend associated with
      // the axes -- so we set the trace's "showInLegend" attribute.
      boolean showInLegend = false;
      Object nameProp = patchObj.getProperty("DisplayName");
      if(nameProp != null && (nameProp.getClass().equals(String.class) || nameProp.getClass().equals(Character.class)))
      {
         String title = processSpecialCharacters(nameProp.toString().trim());
         trace.setTitle(title);
         if(!title.isEmpty()) showInLegend = true;
      }

      // if HG "DisplayName" not set, we check for an undocumented property: the field 'legend_texthandle' in the HG
      // object's 'ApplicationData' property. If the field exists, the code that parses Matlab HG structure sets it as
      // a property directly on the HG object. It should be a scalar double.
      if(!showInLegend)
      {
         prop = patchObj.getProperty("legend_texthandle");
         if(prop != null && prop.getClass().equals(Double.class)) showInLegend = true;
      }
      trace.setShowInLegend(showInLegend);
      
      // prepare the raw data set for the error band trace
      String id = null;
      if(DataSet.isValidIDString(trace.getTitle()))
      {
         id = "src_" + trace.getTitle();
         if(!DataSet.isValidIDString(id)) id = null;
      }
      if(id == null)
      {
         int k = ((int) (Math.random() * 3000.0)) % 100;
         id = "src_" + k;
      }
      
      double dx = vertices[1][0] - vertices[0][0];
      boolean isSeries = Utilities.isWellDefined(dx) && (n >= 6);
      for(int i=2; isSeries && i<n/2; i++) isSeries = (dx == (vertices[i][0] - vertices[i-1][0]));
      
      DataSet ds;
      if(isSeries)
      {
         float[] params = new float[] { (float) dx, (float) vertices[0][0] };
         float[] fData = new float[n];
         for(int i=0; i<n/2; i++)
         {
            fData[2*i] = (float) ((vertices[i][1] + vertices[n-1-i][1]) / 2.0);
            fData[2*i+1] = (float) Math.abs( (vertices[n-1-i][1] - vertices[i][1]) / 2.0 );
         }
         
         ds = DataSet.createDataSet(id, Fmt.SERIES, params, n/2, 2, fData);
      }
      else
      {
         float[] fData = new float[3*n/2];
         
         for(int i=0; i<n/2; i++)
         {
            fData[3*i] = (float) vertices[i][0];
            fData[3*i+1] = (float) ((vertices[i][1] + vertices[n-1-i][1]) / 2.0);
            fData[3*i+2] = (float) Math.abs( (vertices[n-1-i][1] - vertices[i][1]) / 2.0 );
         }
         
         ds = DataSet.createDataSet(id, Fmt.PTSET, null, n/2, 3, fData);
      }
      
      if(ds == null)
      {
         graph.getGraphicModel().deleteNode(trace);
         return(false);
      }
      else trace.setDataSet(ds);
      
      // HG "LineWidth", "LineStyle", and "EdgeColor" properties determine stroke properties for the boundary of the
      // error band -- which is determined by the styling of the trace's "ebar" node. The stroke width of the trace node
      // itself is always set to 0 so that the "average trace" is not drawn on top of the error band. The stroke color 
      // is black if "EdgeColor" is implicit, invalid, or set to the unsupported values "flat" or "interp". If either
      // "LineStyle" or "EdgeColor" is set to "none", then the stroke width is set to 0 on BOTH trace and ebar nodes.
      Measure strokeW = processLineWidth(patchObj);
      StrokePattern sp = processLineStyle(patchObj);
      prop = patchObj.getProperty("EdgeColor");
      Color strokeC = null;
      if(!"none".equals(prop))
      {
         strokeC = processColorSpec(prop);
         if(strokeC == null) strokeC = Color.BLACK;
      }
      if(sp == null || strokeC == null)
      {
         sp = StrokePattern.SOLID;
         strokeC = Color.BLACK;
         strokeW = new Measure(0, Measure.Unit.PT);
      }
      trace.setStrokeColor(strokeC);
      trace.setStrokePattern(sp);
      trace.setMeasuredStrokeWidth(new Measure(0, Measure.Unit.PT));
      ebar.setMeasuredStrokeWidth(strokeW);

      // HG "FaceColor" sets the error band fill color. implicit ==> use black as the fill color; "none" ==> no fill 
      // transparent fill color); ColorSpec ==> use that color as the fill color; "flat", "interp" ==> not supported; 
      // use black.
      prop = patchObj.getProperty("FaceColor");
      Color fillC = (prop == null) ? Color.BLACK : processColorSpec(prop);
      if(fillC == null) fillC = "none".equals(prop) ? new Color(0,0,0,0) : Color.BLACK;
      
      // HG "FaceAlpha" sets the alpha component for the fill color between 0 and 1. The default is 1. "flat" and 
      // "interp" are not supported. Ignored if "FaceColor" = "none".
      if(fillC.getAlpha() > 0)
      {
         prop = patchObj.getProperty("FaceAlpha");
         if(prop != null && prop.getClass().equals(Double.class))
         {
            double alpha = (Double) prop;
            if(alpha < 1.0)
            {
               int iAlpha = Utilities.rangeRestrict(0, 255, (int) (255 *alpha));
               fillC = new Color(fillC.getRed(), fillC.getGreen(), fillC.getBlue(), iAlpha);
            }
         }
      }
      
      trace.setFillColor(fillC);
      
      return(true);
   }
   
   /**
    * Append a {@link PieChartNode} to the graph, as defined by a Matlab Handle Graphics "patch" plot object. 
    * <p>Matlab's <i>pie()</i> function renders a pie chart as a series of "patch"/"text" pairs of children under the
    * parent "axes" object. During pre-processing of the Matlab figure's HG hierarchy, this structure is detected, the
    * axes is marked as a polar plot containing a pie chart, and the relevant "patch" objects (each defining a "slice"
    * in the pie) are coalesced into a single "patch" object in which the pie chart information is stored. This method
    * handles the translation of that coalesced "patch" into a {@link PieChartNode}.</p>
    * 
    * @param pgraph The parent polar plot node in the FypML figure being constructed.
    * @param patchObj The HG "patch" object in which the pie chart information has been consolidated. 
    * @param matCM The Matlab figure's colormap. We need this to process an indexed color specification.
    * @param cLim The portion of the Matlab figure's colormap assigned to the Matlab axes object -- needed to process
    * an indexed color when the patch's "CDataMapping" property is "scaled". Should be a two-element array.
    * @return True if patch was successfully translated as a FypML pie chart node.
    */
   private static boolean addPatchAsPieChart(PolarPlotNode pgraph, HGObject patchObj, double[][] matCM, double[] cLim)
   {
      // append the pie chart node to the graph, initially associated with an empty data set
      if(!pgraph.getGraphicModel().insertNode(pgraph, FGNodeType.PIE, -1))
         throw new NeverOccursException("Failed to insert new pie chart node into FypML 2D polar plot container.");
      
      PieChartNode pie = (PieChartNode) pgraph.getChildAt(pgraph.getChildCount()-1);

      // in a typical Matlab pie chart, each slice is labeled by a 'text' node. These are used to set the legend label
      // for each data group, and they are translated into FypML labels as well. The pie chart itself will have an
      // empty title, and we check the "show in legend" flag by default.
      pie.setTitle("");
      pie.setShowInLegend(true);
      
      // inject the data set for the pie chart
      DataSet ds = patchObj.extractDataSetFromPlotObject("pie", true, false);
      if(ds == null)
      {
         pgraph.getGraphicModel().deleteNode(pie);
         return(false);
      }
      else pie.setDataSet(ds);
      
      // HG "LineWidth", "LineStyle", and "EdgeColor" properties determine stroke properties for pie chart. The stroke
      // color is black if "EdgeColor" is implicit, invalid, or set to the unsupported values "flat" or "interp". If 
      // either "LineStyle" or "EdgeColor" is set to "none", then the stroke width is set to 0.
      Measure strokeW = processLineWidth(patchObj);
      StrokePattern sp = processLineStyle(patchObj);
      Object prop = patchObj.getProperty("EdgeColor");
      Color strokeC = null;
      if(!"none".equals(prop))
      {
         strokeC = processColorSpec(prop);
         if(strokeC == null) strokeC = Color.BLACK;
      }
      if(sp == null || strokeC == null)
      {
         sp = StrokePattern.SOLID;
         strokeC = Color.BLACK;
         strokeW = new Measure(0, Measure.Unit.PT);
      }
      pie.setStrokeColor(strokeC);
      pie.setStrokePattern(sp);
      pie.setMeasuredStrokeWidth(strokeW);
      
      // set the pie chart-specific properties and the per-slice information, as stored in the patch object
      pie.setInnerRadius(0);
      pie.setOuterRadius(patchObj.getPieOuterRadius());
      pie.setRadialOffset(patchObj.getPieSliceRadialOffset());
      
      List<String> labels = patchObj.getDataGroupLabels();
      List<Color> colors = patchObj.getDataGroupFillColors(matCM, cLim);
      for(int i=0; i<pie.getNumDataGroups(); i++)
      {
         if(i < labels.size()) pie.setDataGroupLabel(i, labels.get(i));
         if(i < colors.size()) pie.setDataGroupColor(i, colors.get(i));
         pie.setSliceDisplaced(i, patchObj.isPieSliceDisplaced(i));
      }
      
      return(true);
   }
   
   /**
    * Append a heat map plot to the graph as defined by a Matlab Handle Graphics "surface" or "image" plot object (a 
    * "surface" is a 3D-capable version of "image"). The heat map plot is defined by the {@link ContourNode} -- a
    * contour plot that can be configured to display a data matrix Z(X,Y) as a standard contour plot, heat map image, or
    * a heat map with contour lines superimposed. 
    * 
    * <p>The conversion only respects a few of the properties of the original "surface" or "image":
    * <ul>
    *    <li><i>DisplayName</i>, if present, is taken as the contour node's (non-rendered) "title" property.</li>
    *    <li><i>CData, CDataMapping</i>. The raw data for the heatmap's XYZIMG data set is in <i>CData</i>, but ONLY if
    *    it is a MxN 2D matrix and <i>CDataMapping</i> == 'scaled' (the default). Otherwise, the "surface" or "image"
    *    object is ignored altogether and no contour node is created. [<i>CData</i> could be a MxNx3 matrix, in which 
    *    case it contains the RGB color for each point (m,n) -- this is not supported in FypML); also, 
    *    <i>CDataMapping</i> == 'direct' is not supported.]</li>
    *    <li><i>XData, YData</i>. We assume that these each contain a monotonically increasing or decreasing sequence
    *    of X- and Y-coordinates, respectively. The X-coordinate extent [X0..X1] and Y-coordinate extent [Y0..Y1] of
    *    the contour node's XYZIMG data set is taken as: X0=XData(1), X1=XData(end), etcetera.</li>
    * </ul>
    * Observe that we ignore the "surface" object's "ZData" property entirely; an "image" object does not have a "ZData"
    * property.
    * 
    * @param graph The parent graph node in the FypML figure being constructed.
    * @param hgSurface The HG "surface" or "image" object defining the heat map's properties -- including the raw data!
    */
   private static void addHeatMapToGraph(GraphNode graph, HGObject hgSurface)
   {
      // append the contour node to the graph, initially associated with an empty data set
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.CONTOUR, -1))
         throw new NeverOccursException("Failed to insert new contour plot node into FypML graph.");
      
      ContourNode heatMap = (ContourNode) graph.getChildAt(graph.getChildCount()-1);
      
      // HG "DisplayName" property maps to the contour node's "title" property.
      Object nameProp = hgSurface.getProperty("DisplayName");
      if(nameProp != null && (nameProp.getClass().equals(String.class) || nameProp.getClass().equals(Character.class)))
         heatMap.setTitle(processSpecialCharacters(nameProp.toString().trim()));

      // set display mode so data is rendered as a heat map image
      heatMap.setMode(ContourNode.DisplayMode.HEATMAP);
      
      // Extract XYZIMG data set based on CData,XData,YData. CDataMapping="direct" is unsupported, nor is a 3D CData
      // matrix -- in which cases no data set is created, and the heat map node is removed from the graph.
      DataSet ds = hgSurface.extractDataSetFromPlotObject(heatMap.getTitle(), graph.isPolar(), false);
      if(ds == null) graph.getGraphicModel().deleteNode(heatMap);
      else heatMap.setDataSet(ds);
   }

   /**
    * Append a contour plot to the graph as defined by a Matlab Handle Graphics "specgraph.contourgroup" plot object. 
    * The contour plot is defined in <i>FypML</i> by the {@link ContourNode}, which supports "heat map" images as well
    * as filled or unfilled contour plots.
    * 
    * <p>The conversion only respects some of the properties of the original "specgraph.contourgroup":
    * <ul>
    *    <li><i>DisplayName</i>, if present, is taken as the contour node's (non-rendered) "title" property.</li>
    *    <li><i>LineWidth</i> sets the stroke width for the level lines, while <i>LineStyle</i> specifies the stroke
    *    pattern. Since the <i>FypML</i> contour node only supports a solid stroke, <i>LineStyle</i> is ignored unless
    *    it is set to "none", in which case the level lines are not stroked at all.</li>
    *    <li><i>LineColor</i> is the stroke color for the level lines in a filled contour plot; if unfilled, each level
    *    line color is set IAW the contour level and the parent graph's color map.</li>
    *    <li>If <i>Fill</i>=='on', then a filled contour plot is generated, with level-lines superimposed. Otherwise,
    *    only the level lines are drawn.</li>
    *    <li><i>LevelList</i> is a double vector listing the contour levels. If this property is implicit, then the
    *    {@link ContourNode} will be configured to auto-select the levels -- in which case the contour plot may look
    *    significantly different in <i>FypML</i>.</li>
    *    <li><i>ZData</i> contains the raw data for the contour node's XYZIMG data set.</li>
    *    <li><i>XData, YData</i>. These fields are examined to find the X-coordinate extent [X0..X1] and Y-coordinate 
    *    extent [Y0..Y1] of the contour node's XYZIMG data set. It is assumed that they represent a regular sampling of 
    *    the rectangle [X0 Y0 X1 Y1] in X,Y-space; that's inherent to the definition of the XYZIMG data set format.</li>
    * </ul>
    * Observe that we ignore the "surface" object's "ZData" property entirely; an "image" object does not have a "ZData"
    * property.
    * 
    * @param graph The parent graph node in the FypML figure being constructed.
    * @param hgContour The HG "specgraph.contourgroup" object defining the contour plot in Matlab.
    */
   private static void addContourPlotToGraph(GraphNode graph, HGObject hgContour)
   {
      // append the contour node to the graph, initially associated with an empty data set
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.CONTOUR, -1))
         throw new NeverOccursException("Failed to insert new contour plot node into FypML graph.");
      
      ContourNode contour = (ContourNode) graph.getChildAt(graph.getChildCount()-1);
      
      // HG "DisplayName" property maps to the contour node's "title" property.
      String title = HGObject.getStringValuedProperty(hgContour, "DisplayName");
      if(title != null) contour.setTitle(processSpecialCharacters(title.trim()));

      // HG "LineColor, LineWidth, LineStyle" properties define draw-related properties of the contour. The LineStyle
      // property is ignored unless it is set to "none", in which case stroke width is zero.
      Color c = processColorSpec(hgContour.getProperty("LineColor"));
      if(c != null) contour.setStrokeColor(c);
      Measure strokeW;
      if(processLineStyle(hgContour) == null) strokeW = new Measure(0, Measure.Unit.PT);
      else strokeW = processLineWidth(hgContour);
      contour.setMeasuredStrokeWidth(strokeW);

      // HG "Fill" property selects the contour node's display mode
      String fill = HGObject.getStringValuedProperty(hgContour, "Fill");
      contour.setMode("on".equals(fill) ? ContourNode.DisplayMode.FILLEDCONTOURS : ContourNode.DisplayMode.LEVELLINES);

      // HG "LevelList" property specifies the contour levels for the plot
      double[] levels = HGObject.getDoubleVectorFromProperty(hgContour.getProperty("LevelList"));
      if(levels != null && levels.length > 0) 
         contour.setLevelList(Utilities.toString(levels, 7, 3));
      
      // extract XYZIMG data set based on "XData,YData,ZData" properties. If unable to create the data set, the 
      // contour node is removed from the FypML figure.
      DataSet ds = hgContour.extractDataSetFromPlotObject(contour.getTitle(), graph.isPolar(), false);
      if(ds == null) graph.getGraphicModel().deleteNode(contour);
      else contour.setDataSet(ds);
   }

   /**
    * Append a histogram to a 2D graph container as defined by Matlab's Handle Graphics "histogram" plot object. The 
    * histogram is imported as a {@link RasterNode} in <i>FypML</i>, using one of 3 possible histogram-type display 
    * modes. 
    * 
    * <ul>
    * <li>The HG "histogram" object is generated by the histogram() function, introduced in Matlab R2014b. It is a
    * better way to construct histograms than the hist() function, because the original sample data is available in the
    * histogram object's "Data" property.</li>
    * <li>The histogram is NOT imported if "DisplayStyle=stairs", "Orientation=horizontal", or "Visible=off". None of
    * these options are supported by {@link RasterNode}.</li>
    * <li>The "BinEdges" and "BinWidth" properties are analyzed to determine the histogram sample range, #bins, and
    * bin width for the histogram. If the vector "BinEdges" is such that not all bins are the same width, the histogram
    * is not imported -- {@link RasterNode} only supports a fixed bin width.</li>
    * <li>The "Normalization" property determines the raster node's display mode. Not all options are supported. The
    * "count" option (default) maps to the "histogram" display mode; "probability" maps to "normhist"; "pdf" maps to 
    * "pdf"; and "cdf" maps to "cdf". Otherwise, the histogram is NOT imported.</li>
    * <li>The raster node's stroke color is determined by the "EdgeColor" and "EdgeAlpha" properties. "EdgeColor" is
    * an RGB color spec, or the special values "none" or "auto". The default, implicit value is opaque black; "none" 
    * maps to fully transparent black. The value "auto" means that Matlab automatically selects the color from the list
    * of Matlab's 7 default colors based on the ordinal position of the histogram object in the list of plot objects
    * within the parent axes (if there are more than 7 plot objects, the index is modulo 7). "EdgeAlpha" is a scalar
    * in [0..1] that sets the alpha channel for the stroke color; it defaults to 1.</li>
    * <li>The HG "histogram" object's "LineWidth" and "LineStyle" properties are parsed to set the raster node's stroke
    * width and style. The defaults are 0.5pt for "LineWidth" and a solid "LineStyle".</li>
    * <li>The raster node's fill color is based on the "FaceColor" and "FaceAlpha" properties. The latter is a
    * scalar in [0..1] (default = 0.6). The former is interpreted like "EdgeColor", except that the default value is
    * "auto".</li>
    * </ul>
    * 
    * @param graph The 2D graph container node in the FypML figure being constructed. This could be an instance of
    * {@link GraphNode} or {@link PolarPlotNode}. Not supported for 3D graphs.
    * @param hgHist The HG "histogram" object that defines the histogram in Matlab.
    * @param isTruePolarAxes True if the "histogram" is a child of a Matlab "polaraxes" object. In this case, the data
    * is already in polar coordinates, with the theta coordinate in radians. <b>In addition, the "BinEdges" and 
    * "BinWidth" properties will be in radians. Both the data and these properties are converted to degrees IAW the
    * FypML convention.</b>
    */
   private static void addHistogramToGraph(FGNGraph graph, HGObject hgHist, boolean isTruePolarAxes)
   {
      // the histogram is not imported in certain scenarios...
      double[] binEdges = HGObject.getDoubleVectorFromProperty(hgHist.getProperty("BinEdges"));
      double[] binWidth = HGObject.getDoubleVectorFromProperty(hgHist.getProperty("BinWidth"));
      String norm = HGObject.getStringValuedProperty(hgHist, "Normalization");
      boolean okNorm = (norm==null) || "count".equals(norm) || "probability".equals(norm) || "pdf".equals(norm) || 
            "cdf".equals(norm);
      DataSet ds = hgHist.extractDataSetFromPlotObject(null, graph.isPolar(), isTruePolarAxes);
      if(graph.is3D() || binEdges == null || binWidth == null || binWidth.length != 1 || ds == null || ds.isEmpty() || 
            "off".equals(HGObject.getStringValuedProperty(hgHist, "Visible")) ||
            "stairs".equals(HGObject.getStringValuedProperty(hgHist, "DisplayStyle")) ||
            "horizontal".equals(HGObject.getStringValuedProperty(hgHist, "Orientation")) || !okNorm)
            
         return;
      
      // FypML restricts the number of bins and requires a fixed bin width
      int nBins = binEdges.length - 1;
      if(nBins < RasterNode.MINNUMBINS || nBins > RasterNode.MAXNUMBINS) return;      
      for(int i=1; i<binEdges.length; i++)
      {
         if(binEdges[i]-binEdges[i-1] - binWidth[0] > 0.001 * binWidth[0]) return;
      }

      // append the raster node to the graph and install the extracted data set
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.RASTER, -1))
         throw new NeverOccursException("Failed to insert new raster node into FypML 2D graph container.");
      
      RasterNode raster = (RasterNode) graph.getChildAt(graph.getChildCount()-1);
      raster.setDataSet(ds);
      
      // HG "DisplayName" property maps to the raster node's "title" property.
      String title = HGObject.getStringValuedProperty(hgHist, "DisplayName");
      if(title != null) raster.setTitle(processSpecialCharacters(title.trim()));

      // HG "Normalization" property determines the display mode
      RasterNode.DisplayMode dm = RasterNode.DisplayMode.HISTOGRAM;
      if("probability".equals(norm)) dm = RasterNode.DisplayMode.NORMHIST;
      else if("pdf".equals(norm)) dm = RasterNode.DisplayMode.PDF;
      else if("cdf".equals(norm)) dm = RasterNode.DisplayMode.CDF;
      raster.setMode(dm);
      
      // Set #bins and histogram sample range as determined from "BinEdges". We ASSUME x- and y-offsets are zero. If
      // plot object is in a Matlab "polaraxes", the property is in radians and must be converted to degrees.
      raster.setNumBins(nBins);
      if(graph.isPolar() && isTruePolarAxes)
      {
         double s = Utilities.rad2deg(binEdges[0], -1);
         double e = Utilities.rad2deg(binEdges[binEdges.length-1], -1);
         if(s > e) { double tmp = s; s = e; e = tmp; }
         if(e - s < 0.001)
         {
            if(s >= 0) s -= 360;
            else e += 360;
         }
         raster.setHistogramRange(s, e);
      }
      else
         raster.setHistogramRange(binEdges[0], binEdges[binEdges.length-1]);
      raster.setXOffset(0);
      raster.setBaseline(0);
      
      // HG "LineWidth, LineStyle" properties define stroke width and style for the histogram.    
      Measure strokeW = processLineWidth(hgHist);
      StrokePattern sp = processLineStyle(hgHist);
      if(sp == null)
      {
         // DataNav FypML uses 0 stroke width for the "none" line style
         raster.setMeasuredStrokeWidth(new Measure(0, Measure.Unit.PT));
         raster.setStrokePattern(StrokePattern.SOLID);
      }
      else
      {
         raster.setMeasuredStrokeWidth(strokeW);
         raster.setStrokePattern(sp);
      }
 
      // For HG "EdgeColor" and "FaceColor", a value of "auto" means that the color is selected from Matlab's list of 7
      // default colors based on the ordinal position of the raster node (modulo 7) among the list of all plottable  
      // nodes within the parent graph. Since the raster node is currently the last node in the parent graph's child 
      // list, this code will find its plot index (some children already added may not be plottable nodes!).
      int plotIdx = -1;
      for(int i=0; i<graph.getChildCount(); i++)
      {
         if(graph.getChildAt(i) instanceof FGNPlottable) ++plotIdx;
      }
            
      // HG "EdgeColor" and "EdgeAlpha" determines the node's stroke color.
      Object prop = hgHist.getProperty("EdgeColor");
      Color c;
      if(prop == null) c = Color.BLACK;
      else if("none".equals(prop)) c = TRANSPARENTBLACK;
      else if("auto".equals(prop)) c = DEFMATLABCOLORS[plotIdx % 7];
      else 
      {
         c = processColorSpec(prop);
         if(c == null) c = Color.BLACK;
      }
      
      if(c.getAlpha() == 255)
      {
         // default alpha is 1.0
         double[] alphaVec = HGObject.getDoubleVectorFromProperty(hgHist.getProperty("EdgeAlpha"));
         if(alphaVec != null && alphaVec.length == 1 && alphaVec[0] < 1.0)
         {
            int iAlpha = Utilities.rangeRestrict(0, 255, (int) (alphaVec[0]*255.0));
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), iAlpha);
         }
      }
      raster.setStrokeColor(c);
      
      // HG "FaceColor" and "FaceAlpha" determine the node's fill color.
      prop = hgHist.getProperty("FaceColor");
      if(prop==null || "auto".equals(prop)) c = DEFMATLABCOLORS[plotIdx % 7];
      else if("none".equals(prop)) c = TRANSPARENTBLACK;
      else 
      {
         c = processColorSpec(prop);
         if(c == null) c = Color.BLACK;
      }
      
      if(c.getAlpha() == 255)
      {
         // default alpha is 0.6
         double alpha = 0.6;
         double[] alphaVec = HGObject.getDoubleVectorFromProperty(hgHist.getProperty("FaceAlpha"));
         if(alphaVec != null && alphaVec.length == 1) alpha = alphaVec[0];
         
         if(alpha < 1.0)
         {
            int iAlpha = Utilities.rangeRestrict(0, 255, (int) (alpha*255.0));
            c = new Color(c.getRed(), c.getGreen(), c.getBlue(), iAlpha);
         }
      }
      raster.setFillColor(c);
   }
   
   /**
    * Append a 3D surface node ({@link SurfaceNode}) to the 3D graph as defined by a Matlab HG "graph3d.surfaceplot" 
    * object. The conversion only respects a few of the properties of the original surface plot:
    * <ul>
    *    <li><i>DisplayName</i>, if present, is taken as the heatmap's (non-rendered) title property.</li>
    *    <li><i>EdgeColor</i> and <i>LineWidth</i> set the stroke color and width for the surface's wire frame.</li>
    *    <li><i>FaceColor</i> determines how the surface is filled: 'none' = no fill, an RGB color triplet or Matlab
    *    color string specifies a single-color fill; and 'flat' means that the mesh is color-mapped IAW the Z-coordinate
    *    data. Note that, in Matlab, the surface coloring need not depend on Z, but FypML's surface only supports using
    *    the Z-coordinat data. Thus, the FypML surface could look very different from the Matlab version!</li>
    *    <li><i>XData, YData, ZData</i>. These determine the contents of the FypML surface node's <i>xyzimg</i> data
    *    set. For details, see {@link HGObject#extractDataSetFromPlotObject}.</li>
    * </ul>
    * 
    * @param g3 The parent 3D graph node in the FypML figure being constructed.
    * @param hgSurface The HG "graph3d.surfaceplot" object defining the 3D surface's properties -- including raw data.
    */
   private static void add3DSurfaceToGraph(Graph3DNode g3, HGObject hgSurface)
   {
      // HG "DisplayName" property maps to the surface node's "title" property.
      String title = "";
      Object prop = hgSurface.getProperty("DisplayName");
      if(prop != null && (prop.getClass().equals(String.class) || prop.getClass().equals(Character.class)))
         title = processSpecialCharacters(prop.toString().trim());

      // HG "LineWidth" property sets stroke width for the surface's wire mesh. The  mesh is always stroked solid.
      Measure strokeW = processLineWidth(hgSurface);
      
      // HG "EdgeColor" property sets stroke color for the surface. If 'none', use transparent black. Use black if
      // property is implicit, invalid, or set to the unsupported values "flat" or "interp".
      prop = hgSurface.getProperty("EdgeColor");
      Color strokeC = processColorSpec(prop);
      if(strokeC == null) strokeC = "none".equals(prop) ? TRANSPARENTBLACK : Color.BLACK;

      // HG "FaceColor" property determines whether FypML surface is filled with a single color, is color-mapped, or is
      // not filled at all. If 'flat' (the default), the surface will be color-mapped. If is a valid Matlab color spec,
      // it is filled with the specified color. Else, it is unfilled.
      prop = hgSurface.getProperty("FaceColor");
      boolean isCMapped = (prop == null || "flat".equals(prop));
      Color fillC = null;
      if(!isCMapped)
      {
         fillC = processColorSpec(prop);
         if(fillC == null) fillC = TRANSPARENTBLACK;
      }
      
      DataSet ds = hgSurface.extractDataSetFromPlotObject(title, false, false);
      if(ds == null) return;
      
      // append the surface node to the graph and set the relevant properties
      if(!g3.getGraphicModel().insertNode(g3, FGNodeType.SURFACE, -1))
         throw new NeverOccursException("Failed to insert new surface node into FypML 3D graph.");
      SurfaceNode surf = (SurfaceNode) g3.getChildAt(g3.getChildCount()-1);
      surf.setTitle(title);
      surf.setMeasuredStrokeWidth(strokeW);
      surf.setStrokeColor(strokeC);
      surf.setFillColor(fillC);
      surf.setColorMapped(isCMapped);
      surf.setDataSet(ds);
   }

   /**
    * Append a 3D scatter plot node ({@link Scatter3DNode}) to the 3D graph as defined by a Matlab HG "surface" object
    * generated by Matlab's <i>bar3()</i> function.
    * 
    * <p>The bar3() function generates one or more primitive "surface" objects, each of which renders a set of 3D bars
    * in the parent "axes". When the children of the 3D "axes" are processed, any "surface" objects that are configured
    * to render 3D bars are marked as such and kept in the child list, and all surfaces with the same appearance, bar 
    * size, and Z base plane are coalesced into a single "surface" object.</p>
    * 
    * <p>The conversion of this specialized "surface" object to a 3D scatter plot respects only a few of the HG object's
    * properties.
    * <ul>
    *    <li><i>DisplayName</i>, if present, is taken as the 3D scatter plot's (non-rendered) title property.</li>
    *    <li><i>EdgeColor</i>, <i>EdgeAlpha</i>, <i>LineWidth</i>, and <i>LineStyle</i> set the stroke properties for
    *    the bars rendered by the 3D scatter plot.</li>
    *    <li><i>FaceColor</i> and <i>FaceAlpha</i> determine how the 3D bars are painted. The <i>FaceColor</i> must be
    *    "none" (no fill), an RGB color spec, or "interp". For the first two cases, the scatter plot's display mode is
    *    set to {@link Scatter3DNode.DisplayMode#BARPLOT}, and the node's fill color is set accordingly. The property
    *    <i>FaceAlpha</i> applies only if <i>FaceColor</i> is an RGB color spec; it sets the alpha channel for that
    *    color. If<i>FaceColor="interp"</i>, then the scatter plots' display mode is set to {@link 
    *    Scatter3DNode.DisplayMode#COLORBARPLOT}, and <i>FaceAlpha</i> is ignored.</li>
    *    <li><i>XData, YData, ZData</i>. These contain the bar vertices. During processing of the HG object tree, these
    *    are analyzed to confirm the "surface" renders a set of 3D bars, and the {X,Y,Z} data points represented by the
    *    bars (one point per bar) are calculated. That set of points form the 3D scatter plot node's <i>xyzset</i>
    *    data source. For details, see {@link HGObject#extractDataSetFromPlotObject}.</li>
    *    <li>The bar size and Z base plane coordinate, which are calculated during processing of the "surface" bar
    *    vertex data, are also key properties of the 3D scatter plot. The bar size is in native units in the Matlab
    *    case, whereas it is specified as a %-age of the 3D X-axis extent in FypML case; thus, the bar size may not be
    *    reproduced accurately in FypML</li>
    * </ul>
    * </p>
    * 
    * @param g3 The parent 3D graph node in the FypML figure being constructed.
    * @param hgSurface The HG "surface" object defining the 3D bar plot's properties.
    */
   private static void add3DBarPlotToGraph(Graph3DNode g3, HGObject hgSurface)
   {
      // HG "DisplayName" property maps to the 3D scatter plot node's "title" property.
      String title = "";
      Object prop = hgSurface.getProperty("DisplayName");
      if(prop != null && (prop.getClass().equals(String.class) || prop.getClass().equals(Character.class)))
         title = processSpecialCharacters(prop.toString().trim());

      // HG "EdgeColor" and "EdgeAlpha" properties set the stroke color for the surface. If EdgeColor='none', use 
      // transparent black and ignore "EdgeAlpha" Use black if EdgeColor is implicit, invalid, or set to an unsupported 
      // value. EdgeAlpha = 1 if implicit or an unsupported value. Else it should be a scalar double in [0..1]; use
      // it to set the alpha component when EdgeColor evaluates to an RGB color.
      prop = hgSurface.getProperty("EdgeColor");
      Color strokeC = processColorSpec(prop);
      if(strokeC == null) strokeC = "none".equals(prop) ? TRANSPARENTBLACK : Color.BLACK;
      if(!"none".equals(prop))
      {
         double[] alpha = HGObject.getDoubleVectorFromProperty(hgSurface.getProperty("EdgeAlpha"));
         if(alpha != null && alpha.length == 1)
            strokeC = new Color(strokeC.getRed(), strokeC.getGreen(), strokeC.getBlue(), 
                  Utilities.rangeRestrict(0, 255, (int) (255*alpha[0])));
      }

      // HG "LineStyle" and "LineWidth" set stroke pattern and width for the 3D scatter plot
      StrokePattern sp = processLineStyle(hgSurface);
      if(sp == null) strokeC = TRANSPARENTBLACK;
      Measure strokeW = processLineWidth(hgSurface);
      
      // HG "FaceColor", "FaceAlpha" determine display mode and, possibly, the fill color for the 3D scatter plot
      Color fillC = null;
      Scatter3DNode.DisplayMode mode = Scatter3DNode.DisplayMode.BARPLOT;
      prop = hgSurface.getProperty("FaceColor");
      if("interp".equals(prop)) mode = Scatter3DNode.DisplayMode.COLORBARPLOT;
      else if("none".equals(prop)) fillC = TRANSPARENTBLACK;
      else 
      {
         fillC = processColorSpec(prop);
         if(fillC != null)
         {
            double[] alpha = HGObject.getDoubleVectorFromProperty(hgSurface.getProperty("FaceAlpha"));
            if(alpha != null && alpha.length == 1)
               fillC = new Color(fillC.getRed(), fillC.getGreen(), fillC.getBlue(), 
                     Utilities.rangeRestrict(0, 255, (int) (255*alpha[0])));
         }
      }
      
      DataSet ds = hgSurface.extractDataSetFromPlotObject(title, false, false);
      if(ds == null) return;
      
      // the bar size associated with the 'surface' node is in native units. For FypML, we have to convert this to an
      // integer %-age of the 3D x-axis extent. Obviously, we cannot reproduce the bar plot exactly...
      double xExt = g3.getAxis(Graph3DNode.Axis.X).getRangeMax() - g3.getAxis(Graph3DNode.Axis.X).getRangeMin();
      int barSz = Utilities.rangeRestrict(Scatter3DNode.MINBARSZ, Scatter3DNode.MAXBARSZ, 
            (int) Math.round(hgSurface.get3DBarPlotBarSize()*100/xExt));
      
      // append the 3D scatter plot node to the 3D graph and set its relevant properties
      if(!g3.getGraphicModel().insertNode(g3, FGNodeType.SCATTER3D, -1))
         throw new NeverOccursException("Failed to insert new 3D scatter plot node into FypML graph3d container.");
      Scatter3DNode sp3 = (Scatter3DNode) g3.getChildAt(g3.getChildCount()-1);

      sp3.setMode(mode);
      sp3.setBarSize(barSz);
      sp3.setZBase(hgSurface.get3DBarPlotBaseplaneZ());
      sp3.setDataSet(ds);
      sp3.setShowInLegend(false);
      sp3.setTitle(title);
      if(fillC != null) sp3.setFillColor(fillC);
      sp3.setStrokeColor(strokeC);
      if(sp != null) sp3.setStrokePattern(sp);
      sp3.setMeasuredStrokeWidth(strokeW);
   }
   
   /**
    * Append a FypML line segment or trace node to the graph as defined by a Matlab Handle Graphics "line" object. If 
    * the "line" object contains exactly two points and does not define a marker symbol, then a FypML line segment is 
    * appended; otherwise, a trace node is created (in "polyline" display mode).
    * @param graph The parent 2D graph container in the FypML figure being constructed. Not supported for 3D graphs.
    * @param lineObj The HG "line" object defining the properties of the line segment or trace node to be added.
    */
   private static void addLineToGraph(FGNGraph graph, HGObject lineObj)
   {
      if(graph.is3D()) return;  // not supported for 3D graph
      
      // determine whether we'll translate line object as a simple line segment node, or a trace node in the "polyline"
      // display mode.
      double[] xData = HGObject.getDoubleVectorFromProperty(lineObj.getProperty("XData"));
      double[] yData = HGObject.getDoubleVectorFromProperty(lineObj.getProperty("YData"));
      Object markProp = lineObj.getProperty("Marker");
      boolean useTrace = lineObj.isCoalescedLineSet() || (xData != null && xData.length > 2) || 
            (markProp != null && !markProp.equals("none"));
      
      // get properties governing appearance of line segment or trace: "Color", "LineStyle", "LineWidth"
      Color c = processColorSpec(lineObj.getProperty("Color"));
      Measure strokeW = processLineWidth(lineObj);
      StrokePattern sp = processLineStyle(lineObj);
      
      // handle the case of a simple straight line segment
      if(!useTrace)
      {
         // special case: a line segment with no markers and LineStyle="none" is not drawn at all. So we skip it.
         if(sp == null) return;

         if(xData == null || xData.length < 2 || yData == null || yData.length < 2) return;

         if(!graph.getGraphicModel().insertNode(graph, FGNodeType.LINE, -1))
            throw new NeverOccursException("Failed to insert new line segment node into FypML 2D graph container.");
         
         LineNode line = (LineNode) graph.getChildAt(graph.getChildCount()-1);
         
         // the line segment's endpoints are always in "user units" WRT the graph's coord system. If the graph container
         // is in polar coordinates, we have to convert the endpoints to polar coordinates (when Matlab renders a polar 
         // plot in an 'axes' object, it does so by drawing a polar grid on a Cartesian plane -- data and locations are 
         // always in Cartesian coords).
         double x1 = xData[0];
         double y1 = yData[0];
         double x2 = xData[1];
         double y2 = yData[1];
         if(graph.isPolar())
         {
            double xSq = x1*x1;
            x1 = Math.toDegrees(Math.atan2(y1, x1));
            y1 = Math.sqrt(xSq + y1*y1);
            xSq = x2*x2;
            x2 = Math.toDegrees(Math.atan2(y2, x2));
            y2 = Math.sqrt(xSq + y2*y2);
         }
         
         // Matlab can spit out doubles with very very small fractional digits, but FypML measured values are limited
         // to a certain # of fractional digits, and the line node will reject invalid measures -- so we constrain them.
         Measure.Constraints mc = FGraphicModel.getLocationConstraints(FGNodeType.LINE);
         
         line.setXY(mc.constrain(new Measure(x1, Measure.Unit.USER)), mc.constrain(new Measure(y1, Measure.Unit.USER)));
         line.setX2(mc.constrain(new Measure(x2, Measure.Unit.USER)));
         line.setY2(mc.constrain(new Measure(y2, Measure.Unit.USER)));
         
         // set line drawing attributes
         if(c != null) line.setStrokeColor(c);
         line.setMeasuredStrokeWidth(strokeW);
         line.setStrokePattern(sp);
         return;
      }
      
      //
      // OTHERWISE... we're using a trace node to render the "line" object...
      //
      
      // append the trace node to the graph, initially associated with an empty data set
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.TRACE, -1))
         throw new NeverOccursException("Failed to insert new trace node into FypML 2D graph container.");
      
      TraceNode trace = (TraceNode) graph.getChildAt(graph.getChildCount()-1);
      
      // HG "Color, LineWidth, LineStyle" properties define draw-related properties of the trace
      if(c != null) trace.setStrokeColor(c);
      
      // if "LineStyle==none", then we have to set stroke width to 0 on the trace node -- but NOT on the component 
      // SymbolNode. In Matlab, the "LineWidth" property applies to the stroking of marker symbols, while "LineStyle" 
      // only applies to the line connecting the data points. Matlab markers are always stroked with a solid line.
      if(sp == null)
      {
         // DataNav FypML uses 0 stroke width for the "none" line style
         trace.setMeasuredStrokeWidth(new Measure(0, Measure.Unit.PT));
         trace.setStrokePattern(StrokePattern.SOLID);
      }
      else
      {
         trace.setMeasuredStrokeWidth(strokeW);
         trace.setStrokePattern(sp);
      }
      
      // HG "DisplayName" property maps to the trace node's "title" property.
      Object nameProp = lineObj.getProperty("DisplayName");
      if(nameProp != null && (nameProp.getClass().equals(String.class) || nameProp.getClass().equals(Character.class)))
         trace.setTitle(processSpecialCharacters(nameProp.toString().trim()));

      // HG "Marker" and "MarkerSize properties define the shape and size of the marker symbol for data points. The 
      // default is no marker, which we emulate by setting the symbol size to 0 on the trace's component SymbolNode. 
      // Otherwise, "MarkerSize" indicates the symbol size in points (default = 6pt). The "point" marker is mapped to 
      // "circle", filled with same color as it is stroked, and drawn at 1/3 the specified "MarkerSize" (per Matlab 
      // documentation).
      SymbolNode symbol = trace.getSymbolNode();
      Marker mark = null;
      double markSzPts = 6;
      Object prop = lineObj.getProperty("MarkerSize");
      if(prop != null && prop.getClass().equals(Double.class))
         markSzPts = (Double) prop;
      boolean isPointMarker = false;
      if(markProp != null && (markProp.getClass().equals(String.class) || markProp.getClass().equals(Character.class)))
      {
         String s = markProp.toString();
         switch(s)
         {
         case "+":
            mark = Marker.TEE;
            break;
         case "o":
            mark = Marker.CIRCLE;
            break;
         case "*":
            mark = Marker.STAR;
            break;
         case ".":
            mark = Marker.CIRCLE;
            markSzPts /= 3.0;
            isPointMarker = true;
            break;
         case "x":
            mark = Marker.XHAIR;
            break;
         case "_":
            mark = Marker.HLINETHRU;
            break;
         case "|":
            mark = Marker.LINETHRU;
            break;
         case "square":
         case "s":
            mark = Marker.BOX;
            break;
         case "diamond":
         case "d":
            mark = Marker.DIAMOND;
            break;
         case "^":
            mark = Marker.UPTRIANGLE;
            break;
         case "v":
            mark = Marker.DOWNTRIANGLE;
            break;
         case ">":
            mark = Marker.RIGHTTRIANGLE;
            break;
         case "<":
            mark = Marker.LEFTTRIANGLE;
            break;
         case "pentagram":
         case "p":
            mark = Marker.PENTAGRAM;
            break;
         case "hexagram":
         case "h":
            mark = Marker.HEXAGRAM;
            break;
         }
      }
      if(mark == null)
         symbol.setSize(new Measure(0, Measure.Unit.PT));
      else
      {
         symbol.setType(mark);
         symbol.setSize(new Measure(Utilities.limitFracDigits(markSzPts, 1), Measure.Unit.PT));
         
         // SymbolNode's stroke width is determined by the Matlab 'LineWidth' property and the stroke pattern is always
         // solid, regardless what the "LineStyle" is for the HG object.
         symbol.setMeasuredStrokeWidth(strokeW);
         symbol.setStrokePattern(StrokePattern.SOLID);
      }
      
      
      // HG "MarkerEdgeColor" property sets the stroke color for the marker symbols. If implicit, or if explicitly
      // set to "auto", then the SymbolNode inherits the parent TraceNode's stroke color. If "none", then we set the
      // SymbolNode's stroke width to 0.
      prop = lineObj.getProperty("MarkerEdgeColor");
      if(prop != null)
      {
         // could be a Matlab color spec or one of two other possible strings: "none" or "auto"
         c = processColorSpec(prop);
         if(c != null)
            symbol.setStrokeColor(c);
         else if(prop.getClass().equals(String.class))
         {
            String s = (String) prop;
            if(s.equals("none"))
               symbol.setMeasuredStrokeWidth(new Measure(0, Measure.Unit.IN));
         }
      }
      
      // HG "MarkerFaceColor" property sets the fill color for the marker symbols. It can be a Matlab color spec, OR
      // the strings "none" (hollow symbol, which is the Matlab default) or "auto" (set to same color as HG "axes" or 
      // "figure"). These two are NOT supported. "none"/implicit ==> WHITE; "auto" ==> inherited. However, if the
      // Matlab marker type was "point", that is mapped to a filled circle in FypML, and we force the fill color to
      // match the stroke color.
      prop = lineObj.getProperty("MarkerFaceColor");
      if(isPointMarker)
         c = symbol.getStrokeColor();
      else if(prop == null)
         c = Color.WHITE;
      else
      {
         c = processColorSpec(prop);
         if(c == null && prop.getClass().equals(String.class) && "none".equals(prop))
            c = Color.WHITE;
      }
      if(c != null) symbol.setFillColor(c);
      

      // HG "XData, YData" properties define the trace's raw data set.
      DataSet ds = lineObj.extractDataSetFromPlotObject(trace.getTitle(), graph.isPolar(), false);
      if(ds == null) graph.getGraphicModel().deleteNode(trace);
      else trace.setDataSet(ds);
   }

   /**
    * Append a text label to the graph as defined by a Matlab Handle Graphics "text" object.
    * @param graph The parent 2D or 3D graph container in the FypML figure being constructed. It will be an instance of
    * {@link GraphNode}, {@link PolarPlotNode}, or {@link Graph3DNode}.
    * @param hgAxes The HG "axes" or "polaraxes" begin converted to a FypML 2D or 3D graph.
    * @param textObj The HG "text" object defining the properties of the text label to be added to the graph.
    * @param isTitle True if the text object corresponds to the graph's title. The corresponding text label is treated
    * specially: it is always positioned at (50%, 105%) and centered horizontally.
    */
   private static void addTextLabelToGraph(FGraphicNode graph, HGObject hgAxes, HGObject textObj, boolean isTitle)
   {
      // HG "String" property: if this is implicit, null, or an empty string, then we don't add a text label at all.
      String labelStr = textObj.getTextStringProperty();
      if(labelStr == null || labelStr.isEmpty()) return;
      
      // process the string for TeX-encoded special characters
      labelStr = processSpecialCharacters(labelStr);
      
      // HG "Position" property: Should be a 2- or 3-element double array. If not present, don't add a text label.
      double[] pos = null;
      Object posProp = textObj.getProperty("Position");
      if(posProp != null && posProp.getClass().equals(double[].class))
      {
         pos = (double[]) posProp;
         if(pos.length < 2) pos = null;
      }
      if(pos == null) return;
      
      // append the text label node to the graph container and set its label string and font-related properties
      if(!graph.getGraphicModel().insertNode(graph, FGNodeType.LABEL, -1))
         throw new NeverOccursException("Failed to insert new text label into FypML 2D or 3D graph container.");
      
      LabelNode label = (LabelNode) graph.getChildAt(graph.getChildCount()-1);
      label.setTitle(labelStr);
      setFontProperties(label, textObj);
      
      // HG "Color" property specifies the text/fill color for the LabelNode
      Color c = MatlabUtilities.processColorSpec(textObj.getProperty("Color"));
      if(c != null) label.setFillColor(c);
      
      // the 'text' object specially identified as the graph title is treated differently. It is always located at the
      // top of the graph's data window, centered horizontally.
      if(isTitle)
      {
         label.setXY(new Measure(50, Measure.Unit.PCT), new Measure(105, Measure.Unit.PCT));
         label.setHorizontalAlignment(TextAlign.CENTERED);
         label.setVerticalAlignment(TextAlign.TRAILING);
         return;
      }
      
      // HG "Position" and "Units" properties define the starting location for the text label. Note that some values for
      // "Units" may not be accurately converted. The typical value for "Units" is "data".
      Measure.Unit unit = Measure.Unit.USER;
      Object unitProp = textObj.getProperty("Units");
      if(unitProp != null && unitProp.getClass().equals(String.class))
      {
         String unitStr = (String) unitProp;
         switch(unitStr)
         {
         case "normalized":
            unit = Measure.Unit.PCT;
            pos[0] *= 100.0;
            pos[1] *= 100.0;
            break;
         case "inches":
            unit = Measure.Unit.IN;
            break;
         case "centimeters":
            unit = Measure.Unit.CM;
            break;
         case "points":
            unit = Measure.Unit.PT;
            break;
         case "pixels":
            // assume dpi = 72 pixels/inch
            unit = Measure.Unit.IN;
            pos[0] /= 72.0;
            pos[1] /= 72.0;
            break;
         case "characters":
            // assume a default system font size of 12pt and that "x" character is about half that in width!
            unit = Measure.Unit.PT;
            pos[0] *= 6.0;
            pos[1] *= 6.0;
            break;
         }
      }
      
      // if the text label appears in a 2D polar plot and is positioned in native coordinate system units, we have to
      // convert to polar coordinates: Matlab always uses a Cartesian coordinate system, while FypML expects polar
      // coordinates when positioning in "user units" in a polar graph.
      // NOTE: for a Matlab "polaraxes", "data" units will already be in polar coordinates -- but theta will be
      // in radians, not degrees.
      if(((graph instanceof PolarPlotNode) || ((graph instanceof GraphNode) && ((GraphNode)graph).isPolar())) && 
            (unit == Measure.Unit.USER))
      {
         if(hgAxes.getType().equals("axes"))
         {
            double xSq = pos[0] * pos[0];
            pos[0] = Math.toDegrees(Math.atan2(pos[1], pos[0]));
            pos[1] = Math.sqrt(xSq + pos[1]*pos[1]);
         }
         else
            pos[0] = Math.toDegrees(pos[0]);
      }
      
      // when the graph's axes are configured for automatic range adjustment, it is highly likely that the axis range
      // computed by Matlab will be different from that computed by FC. Thus, when the location is in "user" units,
      // the text label may appear in a different location in the converted FypML graph. Also, we preserve 6 significant
      // digits in case units are in 'user' units, in which case X,Y could be very large or small.
      label.setXY(new Measure(Utilities.limitSignificantDigits(pos[0], 6), unit), 
            new Measure(Utilities.limitSignificantDigits(pos[1], 6), unit));
      
      // HG "Rotation" property: 0 if implicit; else a java.lang.Double specifying the CCW rotation about the label's
      // starting location, in degrees
      Object rotProp = textObj.getProperty("Rotation");
      if(rotProp != null && rotProp.getClass().equals(Double.class))
      {
         double rotDeg = (Double) rotProp;
         label.setRotate(rotDeg);
      }
      
      // HG "HorizontalAlignment" property
      TextAlign align = TextAlign.LEADING;
      Object alignProp = textObj.getProperty("HorizontalAlignment");
      if(alignProp != null && alignProp.getClass().equals(String.class))
      {
         String s = (String) alignProp;
         if(s.equals("center")) align = TextAlign.CENTERED;
         else if(s.equals("right")) align = TextAlign.TRAILING;
      }
      label.setHorizontalAlignment(align);
      
      // HG "VerticalAlignment" property: Not all possible alignment choices in Matlab are supported: "baseline" and
      // "bottom" are treated as the same; as are "top" and "cap". Default in Matlab is "middle"
      align = TextAlign.CENTERED;
      alignProp = textObj.getProperty("VerticalAlignment");
      if(alignProp != null && alignProp.getClass().equals(String.class))
      {
         String s = (String) alignProp;
         if(s.equals("top") || s.equals("cap")) align = TextAlign.LEADING;
         else if(s.equals("bottom") || s.equals("baseline")) align = TextAlign.TRAILING;
      }
      label.setVerticalAlignment(align);
   }
   
   /**
    * Matlab R2016a introduced a new Handle Graphics object to represent a true polar plot, with an angular axis (theta)
    * and radial axis (R): "matlab.graphics.axis.PolarAxes". This object is quite different from the standard "axes" 
    * object, and its properties determine the layout of the polar grid lines and labels. As of R2016b, three different
    * polar plotting functions generate this new HG object with different plot object children:
    * <ul>
    * <li><i>polarplot()</i> creates a 'graph2d.lineseries' and is the replacement for <i>polar()</i>.</li>
    * <li><i>polarhistogram()</i> creates a 'histogram' and is the replacement for <i>rose()</i>.</li>
    * <li><i>polarscatter()</i> create a polar scatter or bubble plot with plot object 'specgraph.scattergroup'.</li>
    * </ul>
    * 
    * <p>This method handles the conversion of a 'polaraxes' object containing any combination of these child plot
    * objects to a FypML polar plot node (element 'pgraph') with the requisite trace, raster or scatter plot nodes. Some
    * notes on how this conversion works and any limitations:
    * <ul>
    * <li>'Position' and 'Units' determine the location, WRT the parent figure, of the rectangle that tightly bounds
    * the polar grid, not including the grid labels. However, in practice, it's clear that the rectangle only tightly
    * bounds the polar grid in either the horizontal or vertical dimension, whichever is smaller. This ensures the
    * grid appears circular rather than elliptical. </li>
    * <li>'FontAngle' and other font-related properties determine the font properties of the polar plot and govern the 
    * appearance of the grid labels.</li>
    * <li>If the HG object has the manufactured properties 'Title_Str' and 'Title_Color', then a label child is added
    * so that it appears above the polar plot, centered horizontally. It shares the font styling of the graph itself,
    * albeit modified IAW the HG properties 'TitleFontWeight' and 'TitleFontSizeMultiplier'.</li>
    * <li>'LineWidth', 'GridAlpha', 'GridColor' and 'GridLineStyle' govern the appearance of the polar graph's theta 
    * grid lines (rays emanating from origin) and radial grid lines (full or partial circles). Flags 'ThetaGrid' and 
    * 'RGrid' determine whether the corresponding grid lines are visible or not. If explicitly set, 'ThetaColor' and 
    * 'RColor' specify the colors of the theta and radial grid lines and associated tick labels, respectively; else,
    * 'GridColor' is used. Note that the label color is always opaque, while 'GridAlpha' determines the opacity of the 
    * grid lines.</li>
    * <li>'ThetaTick', 'ThetaTickLabel', and 'ThetaTickLabelMode' define the grid divisions and labels for the polar
    * plot's theta axis. Note that custom labels are possible. Use the labels in 'ThetaTickLabel' if it exists AND the
    * property 'ThetaTickLabelMode' is not explicitly set to 'auto' (note that this is contrary to the online 
    * documentation, which states that 'auto' is the default value for 'ThetaTickLabelMode'). Analogously for 'RTick', 
    * 'RTickLabel', and 'RTickLabelMode'. Note that the FypML 'pgraph' node supports a set of regularly spaced grid
    * divisions along with a number of custom divisions. During import, we check the contents of 'ThetaTick' and 'RTick'
    * to see if the grid divisions are regularly spaced. If not, they are all treated as custom divisions. Note that
    * the maximum number of grid divisions allowed by the 'paxis' component of a 'pgraph' is 20.</li>
    * </ul>
    * <li>'ThetaAxisUnits' specifies the units for the theta axis: 'degrees' (default) | 'radians'. In FypML, the units
    * are always degrees. These wil be the units for 'ThetaLim', 'ThetaTick', and 'RAxisLocation'.</li>
    * <li>'ThetaLim' = [thetaMin thetaMax] specifies the theta axis range, while 'ThetaDir' = 'counterclockwise' (the
    * default) | 'clockwise' specifies the direction of increasing angles. The relevant properties of the 'paxis' 
    * component representing the theta axis will be set accordingly.</li>
    * <li>'ThetaZeroLocation' = 'right' (default) | 'top' | 'left' | 'bottom' sets the location of the 0-degrees 
    * reference axis. This sets the 'refangle' property of the theta axis in the FypML polar plot: right = 0deg, top =
    * 90, left = 180, and bottom = 270.</li>
    * <li>'RLim' = [rMin rMax] specifies the radial axis range, while 'RDir' = 'normal' (default) | 'reverse' indicates
    * the direction of increasing values. The relevent properties of the 'paxis' component representing the radial axis
    * will be set accordingly.</li>
    * <li>'RAxisLocation' is a scalar T indicating the angular location of the radial axis in 'ThetaAxisUnits'. The 
    * default is 80 degrees. This corresponds to the 'refangle' attribute of the 'paxis' component representing the 
    * radial axis of the polar plot..</li>
    * <li>'Visible' = 'on' (default) | 'off' indicates whether or not the 'polaraxes' is visible. If not, the plot
    * children are still rendered, but the polar plot grid and labels are not drawn at all. In this situation, both
    * 'paxis' components of the 'pgraph' element are hidden ('hide' attribute set to 'true').</li>
    * <li>The 'Color' property is the background color of the graph's data window. If this is white and the parent 
    * figure has no background, or if the parent figure has a solid background that matches this color, then we leave 
    * the polar grid's background color as transparent (the default). Else, we use this color.</li>
    * <li>The X-coordinate data for any plot objects added to 'polaraxes' object are expected to be in radians. <b>This
    * data must be converted to degrees before converting the plot object to the corresponding FypML node!</b></li>
    * </ul>
    * </p>
    * 
    * <p>NOTE: Prior to the introduction of the 'pgraph' element in FC 5.1.2, these Matlab polar plots were mapped to a
    * 'graph' element configured in polar coordinates. However, the 'pgraph' is a much better match because it automates
    * the layout of grid labels and it offers much of the versatility found in the Matlab 'polarAxes' object. In fact,
    * this was the primary reason for adding 'pgraph' to FypML.</p>
    * 
    * @param fig The FypML figure node that will parent the new graph node.
    * @param hgFig The Matlab Handle Graphics object defining the Matlab figure.
    * @param hgPolar The Matlab Handle Graphics object defining the "polaraxes" object.
    * @param cm The FypML-supported colormap that best matches the Matlab figure's colormap. This will be assigned to
    * the color axis of the FypML graph node created. (A Matlab figure can only have one colormap, while FypML can 
    * assign a different colormap to each graph.)
    * @param matCM The Matlab figure's colormap. While currently unused, it may be needed in the future.
    */
   private static void addPolarAxesToFigure_r2016(
         FigureNode fig, HGObject hgFig, HGObject hgPolar, ColorMap cm, double[][] matCM)
   {
      // create the FypML polar plot now. We'll delete it if import fails...
      if(!fig.getGraphicModel().insertNode(fig, FGNodeType.PGRAPH, -1))
         throw new NeverOccursException("Failed to insert new 2D polar plot into FypML figure.");
      
      PolarPlotNode pgraph = (PolarPlotNode) fig.getChildAt(fig.getChildCount()-1);
      PolarAxisNode thetaAxis = pgraph.getThetaAxis();
      PolarAxisNode rAxis = pgraph.getRadialAxis();
      ColorBarNode colorBar = pgraph.getColorBar();
      
      // assign the figure's colormap to the polar plot's color bar
      colorBar.setColorMap(cm);
      
      // handle font-related properties
      setFontProperties(pgraph, hgPolar);
      
      // turn off clipping (which is set by default for PolarPlotNode). We ignore the 'Clipping' flag from Matlab,
      // because Matlab clips differently: FypML clips graphically, whereas Matlab appears to clip data points that
      // are outside the axes range, even if the rendering of the point (eg, think an extended spot in a scatter plot)
      // lies partially inside the axes range.
      pgraph.setClip(false);
      
      // HG "Position" and "Units" properties determine the location of the polar grid bounding box WRT the parent 
      // figure. Typically, "Units" defaults to "normalized", so we need the figure's dimensions in order to get the 
      // box location and size in real units (% units not allowed in DataNav FypML).
      Object posProp = hgPolar.getProperty("Position");
      if(posProp == null || !posProp.getClass().equals(double[].class))
         throw new NeverOccursException("Handle Graphics 'polarAxes' object missing 'Position' property");
      double[] pos = (double[]) posProp;
      if(pos.length < 4 || pos[2] <= 0 || pos[3] <= 0) 
         throw new NeverOccursException("Handle Graphics 'Position' is invalid for 'polarAxes' object");
      
      double scaleFac = 0;  // for special case: normalized units
      Object unitsProp = hgPolar.getProperty("Units");
      if(unitsProp != null && unitsProp.getClass().equals(String.class))
      {
         String units = (String) unitsProp;
         switch(units)
         {
         case "normalized":
            scaleFac = 0;
            break;
         case "inches":
            scaleFac = 1.0;
            break;
         case "centimeters":
            scaleFac = Measure.MM2IN * 10.0;
            break;
         default:
            // "pixels" and "characters" are not supported and map arbitrarily to "points"
            scaleFac = Measure.PT2IN;
            break;
         }
      }
      if(scaleFac == 0)
      {
         // convert normalized values to inches WRT parent figure viewport
         pos[0] = fig.getWidthMI() * pos[0] / 1000.0;
         pos[1] = fig.getHeightMI() * pos[1] / 1000.0;
         pos[2] = fig.getWidthMI() * pos[2] / 1000.0;
         pos[3] = fig.getHeightMI() * pos[3] / 1000.0;
        
      }
      else for(int i=0; i<pos.length; i++) pos[i] *= scaleFac;
      
      // IMPORTANT: Matlab documentation is inaccurate. "Position" is supposed to represent a tight bounding box 
      // around the polar grid, but such a box should be square since the polar grid is always circular. Thus, since we
      // need the tight bounding box when importing to FypML, we have to adjust the larger dimension to match the 
      // smaller one, and fix the relevant coordinate of the BL corner accordingly.
      if(pos[2] > pos[3])
      {
         double delta = pos[2] - pos[3];
         pos[2] = pos[3];
         pos[0] += delta/2.0;
      }
      else if(pos[2] < pos[3])
      {
         double delta = pos[3] - pos[2];
         pos[3] = pos[2];
         pos[1] += delta/2.0;
      }
      
      pgraph.setX(new Measure(Utilities.limitFracDigits(pos[0], 3), Measure.Unit.IN));
      pgraph.setY(new Measure(Utilities.limitFracDigits(pos[1], 3), Measure.Unit.IN));
      pgraph.setWidth(new Measure(Utilities.limitFracDigits(pos[2], 3), Measure.Unit.IN));
      pgraph.setHeight(new Measure(Utilities.limitFracDigits(pos[3], 3), Measure.Unit.IN));
      
      // HG "Color" property is the polar grid background color, which maps to the "boxColor" attribute (as of FC 
      // v5.1.2). However, if the parent figure has no background and the axes background color is opaque white, then
      // we set the polar grid's background to transparent. Similarly, if the parent figure has a solid background
      // color that matches the "Color" property value, then we again set the grid background to transparent. A
      // transparent "boxColor" is the default for a pgraph node.
      Color c = processColorSpec(hgPolar.getProperty("Color"));
      if(c == null) c = Color.WHITE;
      BkgFill figBkg = fig.getBackgroundFill();
      boolean assumeTransp = (figBkg.isTransparent() && Color.WHITE.equals(c)) ||
              (figBkg.getFillType() == BkgFill.Type.SOLID && figBkg.getColor1().equals(c));
      if(!assumeTransp) pgraph.setGridBkgColor(c);

      // HG "LineWidth" property maps to the polar plot's "strokeWidth" and is inherited by the axis components. 
      // Units=point. Default is 0.5pt. 
      pgraph.setMeasuredStrokeWidth(processLineWidth(hgPolar));
      
      // HG "GridLineStyle", "GridColor", "GridAlpha", "ThetaGrid", "RGrid", "ThetaColor", and "RColor" govern the
      // appearance of theta and radial grid lines. "Theta/RColor" apply only if explicitly set. We hide the corres.
      // polar axis element if "ThetaGrid" or "RGrid" = "off"; both are hidden if "GridLineStyle" is "none". Also, 
      // both axes are hidden if the HG "Visible" property = "off" (default is "on").
      boolean isVisible = !"off".equals(hgPolar.getProperty("Visible"));
      StrokePattern gridPat = processLineStyle(hgPolar);
      if(gridPat != null)
      {
         thetaAxis.setStrokePattern(gridPat);
         rAxis.setStrokePattern(gridPat);
      }
      thetaAxis.setHide((!isVisible) || gridPat==null || "off".equals(hgPolar.getProperty("ThetaGrid")));
      rAxis.setHide((!isVisible) || gridPat==null || "off".equals(hgPolar.getProperty("RGrid")));
      
      Color gridC = processColorSpec(hgPolar.getProperty("GridColor"));
      if(gridC == null) gridC = new Color(0.15f, 0.15f, 0.15f);
      int iAlpha = Utilities.rangeRestrict(0, 255, (int) (0.15*255));
      Object prop = hgPolar.getProperty("GridAlpha");
      if(prop != null && prop.getClass().equals(Double.class)) 
         iAlpha = Utilities.rangeRestrict(0, 255, (int) (255 * (Double) prop));
      Color thetaC = processColorSpec(hgPolar.getProperty("ThetaColor"));
      if(thetaC == null) thetaC = gridC;
      Color rC = processColorSpec(hgPolar.getProperty("RColor"));
      if(rC == null) rC = gridC;
      
      thetaAxis.setStrokeColor(new Color(thetaC.getRed(), thetaC.getGreen(), thetaC.getBlue(), iAlpha));
      rAxis.setStrokeColor(new Color(rC.getRed(), rC.getGreen(), rC.getBlue(), iAlpha));
      
      // HG "ThetaAxisUnits". If this is "radians", we need to convert affected properties to degrees.
      double convert2Deg = 1;
      if("radians".equals(hgPolar.getProperty("ThetaAxisUnits"))) convert2Deg = 180.0/Math.PI;
      
      // HG "ThetaLim": The theta axis range [S E] in "ThetaAxisUnits". Convert to degrees, then restrict range so that
      // S >= -360 and S-E <= 360. Note that Matlab requires S < E.
      double[] thetaRng = HGObject.getDoubleVectorFromProperty(hgPolar.getProperty("ThetaLim"));
      if(thetaRng == null || thetaRng.length != 2 || thetaRng[0] >= thetaRng[1]) thetaRng = new double[] {0, 360};
      thetaRng[0] *= convert2Deg;
      thetaRng[1] *= convert2Deg;
      double thetaSpan = thetaRng[1] - thetaRng[0];
      if(thetaSpan > 360) 
      { 
         thetaSpan = 360;
         thetaRng[1] = thetaRng[0] + 360;
      }
      if(thetaRng[0] < -360)
      {
         thetaRng[0] = Utilities.restrictAngle(thetaRng[0], -360);
         thetaRng[1] = thetaRng[0] + thetaSpan;
      }
      else if(thetaRng[0] > 360)
      {
         thetaRng[0] = Utilities.restrictAngle(thetaRng[0]);
         thetaRng[1] = thetaRng[0] + thetaSpan;
      }
      thetaAxis.setRange(thetaRng[0], thetaRng[1]);
 
      // HG "ThetaDir" = "counterclockwise" (default) | "clockwise". Set 'reverse' property on theta axis if CW.
      if("clockwise".equals(hgPolar.getProperty("ThetaDir"))) thetaAxis.setReversed(true);
      
      // HG "ThetaZeroLocation": Determines the reference angle for the theta axis, measured CCW from due east.
      int refAngle = 0;
      prop = hgPolar.getProperty("ThetaZeroLocation");
      if("top".equals(prop)) refAngle = 90;
      else if("left".equals(prop)) refAngle = 180;
      else if("bottom".equals(prop)) refAngle = 270;
      thetaAxis.setReferenceAngle(refAngle);
      
      // HG "ThetaTick": Set theta axis grid divisions accordingly. Must be a vector of increasing values. Default is 
      // [0 30 60 .. 360], which in FypML is a regularly spaced grid reprsented by the triplet [0 330 30]. If contents
      // of 'ThetaTick' are not regularly spaced, then we use it directly, setting the regular grid triplet to [0 0 0].
      double[] divs;
      double[] ticks = HGObject.getDoubleVectorFromProperty(hgPolar.getProperty("ThetaTick"));
      if(ticks == null) divs = new double[] {0, 330, 30};
      else if(ticks.length == 0) divs = new double[] {0, 0, 0};
      else if(ticks.length == 1) divs = new double[] {0, 0, 0, ticks[0]*convert2Deg};
      else
      {
         boolean isRegGrid = true;
         double intv = ticks[1] - ticks[0];
         for(int i=1; isRegGrid && i<ticks.length; i++)
            isRegGrid = (Math.abs(ticks[i]-ticks[i-1]-intv) < 0.001);

         if(isRegGrid) divs = new double[] {ticks[0], ticks[ticks.length-1], intv};
         else divs = ticks;
         
         for(int i=0; i<divs.length; i++) divs[i] *= convert2Deg;
      }
      thetaAxis.setGridDivisions(divs);
      
      // HG "ThetaTickLabelMode", "ThetaTickLabel": Custom grid labels for theta axis. We use the labels supplied in 
      // "ThetaTickLabel" if property exists and is valid, and if "ThetaTickLabelMode" is not explicitly set to "auto".
      String[] customLabels = null;
      prop = hgPolar.getProperty("ThetaTickLabelMode");
      if(!"auto".equals(prop))
      {
         prop = hgPolar.getProperty("ThetaTickLabel");
         if(prop != null && prop.getClass().equals(String[].class) && ((String[])prop).length > 0)
         {
            customLabels = (String[]) prop;
            for(int i=0; i<customLabels.length; i++) customLabels[i] = processSpecialCharacters(customLabels[i]);
         }
         else if(prop != null && prop.getClass().equals(String.class) && !((String) prop).isEmpty())
         {
            // there's just one custom label, which will get reused as needed!
            customLabels = new String[1];
            customLabels[0] = processSpecialCharacters((String)prop);
         }
      }
      if(customLabels != null) thetaAxis.setCustomGridLabels(customLabels);
      
      // HG "RLim": Radial axis range [S E]. If "RLim" not valid or S >= E, then [0 1] is assumed.
      double[] rRng = HGObject.getDoubleVectorFromProperty(hgPolar.getProperty("RLim"));
      if(rRng == null || rRng.length != 2 || rRng[0] >= rRng[1]) rRng = new double[] {0, 1};
      rAxis.setRange(rRng[0], rRng[1]);

      // HG "RDir" = "normal" (default) | "reverse". Set 'reverse' property on radial axis accordingly.
      if("reverse".equals(hgPolar.getProperty("RDir"))) rAxis.setReversed(true);
      
      // HG "RAxisLocation" : Reference angle for the radial axis, in "ThetaAxisUnits". Default is 80 deg
      refAngle = 80;
      prop = hgPolar.getProperty("RAxisLocation");
      if(prop != null && prop.getClass().equals(Double.class)) 
         refAngle = Utilities.rangeRestrict(-359, 359, (int) ((Double) prop *convert2Deg + 0.5));
      rAxis.setReferenceAngle(refAngle);

      // HG "RTick": Set radial axis grid divisions accordingly. Must be a vector of increasing values. Default is 
      // [0 0.2 .. 1], which in FypML is a regularly spaced grid reprsented by the triplet [0 1 0.2]. If contents
      // of 'RTick' are not regularly spaced, then we use it directly, setting the regular grid triplet to [0 0 0].
      ticks = HGObject.getDoubleVectorFromProperty(hgPolar.getProperty("RTick"));
      if(ticks == null) divs = new double[] {0, 1, 0.2};
      else if(ticks.length == 0) divs = new double[] {0, 0, 0};
      else if(ticks.length == 1) divs = new double[] {0, 0, 0, ticks[0]};
      else
      {
         boolean isRegGrid = true;
         double intv = ticks[1] - ticks[0];
         for(int i=1; isRegGrid && i<ticks.length; i++)
            isRegGrid = (Math.abs(ticks[i]-ticks[i-1]-intv) < 0.001);

         if(isRegGrid) divs = new double[] {ticks[0], ticks[ticks.length-1], intv};
         else divs = ticks;
      }
      rAxis.setGridDivisions(divs);

      // HG "RTickLabelMode", "RTickLabel": Custom grid labels for the radial axis. We use the labels supplied in 
      // "RTickLabel" if property exists and is valid, and if "RTickLabelMode" is not explicitly set to "auto".
      customLabels = null;
      prop = hgPolar.getProperty("RTickLabelMode");
      if(!"auto".equals(prop))
      {
         prop = hgPolar.getProperty("RTickLabel");
         if(prop != null && prop.getClass().equals(String[].class) && ((String[])prop).length > 0)
         {
            customLabels = (String[]) prop;
            for(int i=0; i<customLabels.length; i++) customLabels[i] = processSpecialCharacters(customLabels[i]);
         }
         else if(prop != null && prop.getClass().equals(String.class) && !((String) prop).isEmpty())
         {
            // there's just one custom label, which will get reused as needed!
            customLabels = new String[1];
            customLabels[0] = processSpecialCharacters((String)prop);
         }
      }
      if(customLabels != null) rAxis.setCustomGridLabels(customLabels);

      // If the fake property "Title_Str" is set to a non-empty string, add a FypML label representing the title of
      // the polar plot. We also use fake property "Title_Color", if present, for the title color. The font properties
      // are inherited from the polar plot, but the HG properties "TitleFontWeight" and "TitleFontSizeMultiplier" can 
      // affect them. 
      String title = HGObject.getStringValuedProperty(hgPolar, "Title_Str");
      if(title != null && !title.isEmpty())
      {
         title = processSpecialCharacters(title);
         pgraph.setTitle(title);
         
         // The title is not shown if the polar grid is hidden ("Visible" = "off").
         if(isVisible)
         {
            // append a text label node to the polar plot and set its label string and text color
            if(!pgraph.getGraphicModel().insertNode(pgraph, FGNodeType.LABEL, -1))
               throw new NeverOccursException("Failed to insert new text label into FypML polar plot.");
            
            LabelNode label = (LabelNode) pgraph.getChildAt(pgraph.getChildCount()-1);
            label.setTitle(title);
            Color titleC = processColorSpec(hgPolar.getProperty("Title_Color"));
            if(titleC != null) label.setFillColor(titleC);

            // HG "TitleFontWeight": "bold" (default) | "normal"
            boolean isNormal = "normal".equals(hgPolar.getProperty("TitleFontWeight"));
            label.setFontStyle(isNormal ? FontStyle.PLAIN : FontStyle.BOLD);
            
            // HG "TitleFontMultiplier": Scale title's font size WRT the graph's font size
            double scale = 1.1;
            prop = hgPolar.getProperty("TitleFontSizeMultiplier");
            if(prop != null && prop.getClass().equals(Double.class)) scale = (Double) prop;
            if(scale > 0)
            {
               int sz = (int) (pgraph.getFontSizeInPoints() * scale + 0.5);
               label.setFontSizeInPoints(sz);
            }
            
            // position the label above the graph's data window, centered horizontally. Since we don't have access to
            // the position info for the Matlab text object, we make an educated guess: centered above the polar grid,
            // 2.5*(font size) above the top of the polar grid's tight bounding box (to clear a label at 90deg).
            double yPct = 100.0 * ((pos[3] + 2.5*pgraph.getFontSizeInPoints()/72.0) / pos[3]);
            label.setXY(new Measure(50, Measure.Unit.PCT), new Measure(yPct, Measure.Unit.PCT));
            label.setHorizontalAlignment(TextAlign.CENTERED);
            label.setVerticalAlignment(TextAlign.TRAILING);
         }
      }
      
      // HG "CLim" property sets the range of the color axis. The default is [0 1].
      double[] cLim = HGObject.getDoubleVectorFromProperty(hgPolar.getProperty("CLim"));
      if(Utilities.isWellDefined(cLim))
      {
         colorBar.setStart(Utilities.limitSignificantDigits(cLim[0], 6));
         colorBar.setEnd(Utilities.limitSignificantDigits(cLim[1], 6));
      }
      else
      {
         colorBar.setStart(0);
         colorBar.setEnd(1);
      }

      // convert each supported HG plot object child or "text" child to the appropriate FypML node and add it as a child
      // of the FypML polar plot node.
      for(int i=0; i<hgPolar.getChildCount(); i++)
      {
         HGObject hgChild = hgPolar.getChildAt(i);
         switch(hgChild.getType())
         {
         case "graph2d.lineseries":
            addDataTraceToGraph(pgraph, hgChild, true);
            break;
         case "specgraph.scattergroup":
            addScatterGroupToGraph(pgraph, hgChild, true);
            break;
         case "histogram":
            addHistogramToGraph(pgraph, hgChild, true);
            break;
         case "text":
            addTextLabelToGraph(pgraph, hgPolar, hgChild, false);
            break;
         }
      }

      // if a HG "scribe.legend" or "scribe.colorbar" object is attached to the "polaraxes", then configure the FypML
      // GraphNode's legend or color axis, respectively.
      configureGraphLegend(pgraph, hgPolar, hgFig);
      configureGraphColorBar(pgraph, hgPolar, hgFig);
   }

   
   /**
    * When a graph axis is configured for automatic range adjustment, it is highly likely that the axis range computed
    * by FC will not exactly match the range that Matlab computes. A consequence is that any text label positioned in 
    * "user" units ("Units" = "data" in Matlab) will appear in a different location in the converted FypML figure. Since
    * we have no way of knowing what the Matlab axis range is, we cannot accurately and reliably position such a label. 
    * However, we can at least ensure that the label is near the graph's data window -- then the user should "see" the 
    * label in the rendered FypML figure and can then adjust its position as needed in Figure Composer.
    * 
    * <p>This helper method checks any {@link LabelNode} children of the converted {@link GraphNode}. If a label is 
    * positioned in user units and either coordinate lies more than 10% past either endpoint in the the range of the 
    * relevant axis, then it is reset to the closest range endpoint. The method should be invoked after all data traces 
    * have been added to the graph. If neither the X- nor Y-axis of the graph is auto-ranged, no action is taken since 
    * the graph should have the same axis limits as its Matlab "axes" counterpart.</p>
    * 
    * @param graph A FypML 2D graph to which a Matlab Handle Graphics "axes" object was converted.
    */
   private static void fixLabelLocationsInGraph(GraphNode graph)
   {
      AxisNode xAxis = graph.getPrimaryAxis();
      AxisNode yAxis = graph.getSecondaryAxis();
      if(!(xAxis.isAutoranged() || yAxis.isAutoranged())) return;
      
      for(int i=0; i<graph.getChildCount(); i++)
      {
         FGraphicNode child = graph.getChildAt(i);
         if(child.getNodeType() != FGNodeType.LABEL) continue;
         
         LabelNode label = (LabelNode) child;
         Measure x = label.getX();
         Measure y = label.getY();
         if(x.getUnits() != Measure.Unit.USER) continue;
         // (both coordinates will be in the same units, as that is how things work in Matlab!)
         
         if(xAxis.isAutoranged())
         {
            double start = xAxis.getStart();
            double end = xAxis.getEnd();
            if(start > end) { double d = start; start = end; end = d; }
            double rng = end - start;
            
            double xVal = x.getValue();
            if(xVal < start && (start-xVal) / rng > 0.1)
               label.setX(new Measure(Utilities.limitSignificantDigits(start, 6), Measure.Unit.USER));
            else if(xVal > end && (xVal-end) / rng > 0.1)
               label.setX(new Measure(Utilities.limitSignificantDigits(end, 6), Measure.Unit.USER));
         }
         
         if(yAxis.isAutoranged())
         {
            double start = yAxis.getStart();
            double end = yAxis.getEnd();
            if(start > end) { double d = start; start = end; end = d; }
            double rng = end - start;
            
            double yVal = y.getValue();
            if(yVal < start && (start-yVal) / rng > 0.1)
               label.setY(new Measure(Utilities.limitSignificantDigits(start, 6), Measure.Unit.USER));
            else if(yVal > end && (yVal-end) / rng > 0.1)
               label.setY(new Measure(Utilities.limitSignificantDigits(end, 6), Measure.Unit.USER));
         }
      }
   }
   
   /**
    * Helper method converts the Matlab string to a FypML, replacing any escaped TeX-encoded special characters with
    * their Unicode equivalent. It will also replace "\t" and "\n" with the Unicode tab and linefeed characters.
    * Algorithm:
    * <ul>
    * <li>Looks for any unescaped backslash characters in the string S. If there are none, S is returned unchanged.</li>
    * <li>For each such backslash, it checks whether the substring following that backslash matches any of the supported
    * TeX character code names. If not, the backslash is silently removed. Otherwise, the backslash and character code 
    * name are replaced with the equivalent Unicode character. If FypML does not support that character, a '?' is used 
    * instead.</li>
    * <li>NOTE that the method does NOT handle any other TeX constructs, because FypML does not support it. All other
    * TeX constructs are simply passed on as plain text.</li>
    * </ul>
    * @param s The Matlab string, possibly containing one or more TeX-encoded special characters.
    * @return The result after processing the string in the manner described.
    */
   private static String processSpecialCharacters(String s)
   {
      if(s == null || s.indexOf('\\') < 0) return(s);
      
      StringBuilder sb = new StringBuilder();
      char[] strAr = s.toCharArray();
      int i = 0;
      int n = strAr.length;
      while(i < n)
      {
         if(strAr[i] != '\\')
         {
            // not a backslash, so copy it to output
            sb.append(strAr[i++]);
         }
         else if((i == n-1) || (strAr[i+1] == '\\'))
         {
            // terminal backslash or escaped backslash. Append a single backslash to the output text.
            sb.append('\\');
            i += 2;
         }
         else
         {
            // unescaped backslash. If substring after backslash starts with a recognized TeX character code name, then
            // replace the backslash and code name with the Unicode character equivalent. Else, leave the backslash in
            // the output and move on to the next character.
            
            // skip over the backslash
            ++i;
            
            // The code names can be 1-11 or 14 characters long. So we check strings of that length following the
            // backslash to see if there's a match for any supported code name. We start with the longest code names
            // first and work down to the 1-character code names to ensure we get the right one. For example, "o" is a
            // valid code name, as is "oplus", "otimes", "oslash", and "omega"!
            for(int j=14; j>=1; j--)
            {
               if(i+j>n || j==12 || j==13) continue;
               String texCode = new String(strAr, i, j);
               Character uniCode = tex2Unicode.get(texCode);
               if(uniCode != null)
               {
                  // skip over the code name and append the equivalent Unicode character to the output
                  i += j;
                  sb.append(uniCode.charValue());
                  break;
               }
            }
         }
      }
      
      return(sb.toString());
   }
   
   /**
    * Hash map from Matlab TeX-supported character node name (e.g., "alpha") to the Unicode character code point 
    * ('\u03b1'). It also maps "t" to the Unicode tab character and "n" to the linefeed. If any Matlab TeX character is 
    * not available in Matlab, it is mapped to a question mark. NOTE that some code names overlap: "o" and "otimes", 
    * "t" and "tau" or "theta", "n" and "ni" or "nu".
    */
   private static final HashMap<String, Character> tex2Unicode = new HashMap<>();
   
   static 
   {
      // each TeX special character code is mapped to its Unicode equivalent. If any Matlab-supported TeX character
      // is not available in FypML, it is replaced by a question mark.
      tex2Unicode.put("alpha", '\u03b1');
      tex2Unicode.put("angle", '\u2220');
      tex2Unicode.put("ast", '\u002a');
      tex2Unicode.put("beta", '\u03b2');
      tex2Unicode.put("gamma", '\u03b3');
      tex2Unicode.put("delta", '\u03b4');
      tex2Unicode.put("epsilon", '\u03b5');
      tex2Unicode.put("zeta", '\u03b6');
      tex2Unicode.put("eta", '\u03b7');
      tex2Unicode.put("theta", '\u03b8');
      tex2Unicode.put("vartheta", '\u03d1');
      tex2Unicode.put("iota", '\u03b9');
      tex2Unicode.put("kappa", '\u03ba');
      tex2Unicode.put("lambda", '\u03bb');
      tex2Unicode.put("mu", '\u03bc');
      tex2Unicode.put("nu", '\u03bd');
      tex2Unicode.put("xi", '\u03be');
      tex2Unicode.put("pi", '\u03c0');
      tex2Unicode.put("rho", '\u03c1');
      tex2Unicode.put("sigma", '\u03c3');
      tex2Unicode.put("varsigma", '\u03c2');
      tex2Unicode.put("tau", '\u03c4');
      tex2Unicode.put("equiv", '\u2261');
      tex2Unicode.put("Im", '\u2111');
      tex2Unicode.put("otimes", '\u2297');
      tex2Unicode.put("cap", '\u2229');
      tex2Unicode.put("supset", '\u2283');
      tex2Unicode.put("int", '\u222b');
      tex2Unicode.put("rfloor", '?');
      tex2Unicode.put("lfloor", '?');
      tex2Unicode.put("perp", '\u22a5');
      tex2Unicode.put("wedge", '\u2227');
      tex2Unicode.put("rceil", '?');
      tex2Unicode.put("vee", '\u2228');

      tex2Unicode.put("upsilon", '\u03c5');
      tex2Unicode.put("phi", '\u03c6');
      tex2Unicode.put("chi", '\u03c7');
      tex2Unicode.put("psi", '\u03c8');
      tex2Unicode.put("omega", '\u03c9');
      tex2Unicode.put("Gamma", '\u0393');
      tex2Unicode.put("Delta", '\u0394');
      tex2Unicode.put("Theta", '\u0398');
      tex2Unicode.put("Lambda", '\u039b');
      tex2Unicode.put("Xi", '\u039e');
      tex2Unicode.put("Pi", '\u03a0');
      tex2Unicode.put("Sigma", '\u03a3');
      tex2Unicode.put("Upsilon", '\u03a5');
      tex2Unicode.put("Phi", '\u03a6');
      tex2Unicode.put("Psi", '\u03a8');
      tex2Unicode.put("Omega", '\u03a9');
      tex2Unicode.put("forall", '\u2200');
      tex2Unicode.put("exists", '\u2203');
      tex2Unicode.put("ni", '\u220b');
      tex2Unicode.put("cong", '\u2245');
      tex2Unicode.put("approx", '\u2248');
      tex2Unicode.put("Re", '\u211c');
      tex2Unicode.put("oplus", '\u2295');
      tex2Unicode.put("cup", '\u222a');
      tex2Unicode.put("subseteq", '\u2286');
      tex2Unicode.put("in", '\u2208');
      tex2Unicode.put("lceil", '?');
      tex2Unicode.put("cdot", '\u00b7');
      tex2Unicode.put("neg", '\u00ac');
      tex2Unicode.put("times", '\u00d7');
      tex2Unicode.put("surd", '\u221a');
      tex2Unicode.put("warpi", '\u03d6');
      tex2Unicode.put("rangle", '?');
      tex2Unicode.put("langle", '?');

      tex2Unicode.put("sim", '\u007e');
      tex2Unicode.put("leq", '\u2264');
      tex2Unicode.put("infty", '\u221e');
      tex2Unicode.put("clubsuit", '\u2663');
      tex2Unicode.put("diamondsuit", '\u2666');
      tex2Unicode.put("heartsuit", '\u2665');
      tex2Unicode.put("spadesuit", '\u2660');
      tex2Unicode.put("leftrightarrow", '\u2194');
      tex2Unicode.put("leftarrow", '\u2190');
      tex2Unicode.put("Leftarrow", '\u21d0');
      tex2Unicode.put("uparrow", '\u2191');
      tex2Unicode.put("rightarrow", '\u2192');
      tex2Unicode.put("Rightarrow", '\u21d2');
      tex2Unicode.put("downarrow", '\u2193');
      tex2Unicode.put("circ", '\u00ba');
      tex2Unicode.put("pm", '\u00b1');
      tex2Unicode.put("geq", '\u2265');
      tex2Unicode.put("propto", '\u221d');
      tex2Unicode.put("partial", '\u2202');
      tex2Unicode.put("bullet", '\u2022');
      tex2Unicode.put("div", '\u00f7');
      tex2Unicode.put("neq", '\u2260');
      tex2Unicode.put("aleph", '\u2135');
      tex2Unicode.put("wp", '\u2118');
      tex2Unicode.put("oslash", '\u2205');
      tex2Unicode.put("supseteq", '\u2287');
      tex2Unicode.put("subset", '\u2282');
      tex2Unicode.put("o", '\u03bf');
      tex2Unicode.put("nabla", '\u2207');
      tex2Unicode.put("ldots", '\u2026');
      tex2Unicode.put("prime", '\u2032');
      tex2Unicode.put("0", '\u2205');
      tex2Unicode.put("mid", '\u007c');
      tex2Unicode.put("copyright", '\u00a9');
      
      tex2Unicode.put("t", '\t');
      tex2Unicode.put("n", '\n');
   }
   
   /** Matlab's seven default plot colors, in the default color order (as of R2014b). */
   private final static Color[] DEFMATLABCOLORS = {
         new Color(0.0f, 0.447f, 0.741f),
         new Color(0.85f, 0.325f, 0.098f),
         new Color(0.929f, 0.694f, 0.125f),
         new Color(0.494f, 0.184f, 0.556f),
         new Color(0.466f, 0.674f, 0.188f),
         new Color(0.301f, 0.745f, 0.933f),
         new Color(0.635f, 0.078f, 0.184f),
   };

}
