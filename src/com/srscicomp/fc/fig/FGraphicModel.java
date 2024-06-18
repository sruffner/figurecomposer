package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints; 
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import com.srscicomp.common.g2dviewer.Focusable;
import com.srscicomp.common.g2dviewer.RenderableModel;
import com.srscicomp.common.g2dviewer.RenderableModelViewer;
import com.srscicomp.common.g2dviewer.RootRenderable;
import com.srscicomp.common.ui.UnicodeSubset;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSetInfo;
import com.srscicomp.fc.data.DataSet.Fmt;

/**
 * <code>FGraphicModel</code> is <em>DataNav</em>'s graphic model for a figure displaying scientific data. The model 
 * is implemented as a hierarchical tree of <code>FGraphicNode</code>s defining the layout and appearance of the 
 * figure. <code>FGraphicModel</code> also implements an undo history, provides rudimentary clipboard support for 
 * cutting and pasting graphic nodes, and provides access to all data sources currently rendered in the model's 
 * presentation nodes.
 * 
 * @author  sruffner
 */
public class FGraphicModel implements RenderableModel, Printable
{
   /**
    * Replace the entire content of one graphic model with that of another. The original content of the destination 
    * model is discarded, while the source model is reset to an empty figure with no data.
    * 
    * @param dst The destination graphic model.
    * @param src The source model.
    */
   public static void replaceContent(FGraphicModel dst, FGraphicModel src)
   {
      if(dst == null || src == null) throw new IllegalArgumentException("Null argument!");
      
      // discard old contents in destination model
      if(dst.root != null)
      {
         dst.root.setOwnerModel(null);
         dst.root.removeAllDescendants();
         dst.root = null;
      }
     
      // destination takes on source model's content, and initial selection is placed on root node.
      if(src.root == null) dst.root = new FigureNode();
      else dst.root = src.root;
      dst.currSelection = new ArrayList<>();
      dst.currSelection.add(dst.root);
      dst.root.setOwnerModel(dst);
      dst.fireFocusChanged();
      dst.isModified = src.isModified;
      dst.modifyCount = src.modifyCount;
      

      dst.clearEditHistory();    // this wholesale operation cannot be undone!
      
      // reset source model
      src.root = new FigureNode();
      src.currSelection = new ArrayList<>();
      src.currSelection.add(src.root);
      src.root.setOwnerModel(src);
   }

   /**
    * Create an empty figure with the specified size and location.
    * 
    * @param loc A 4-element array [x y w h] specifying the location (x,y) of the figure's bottom-left corner (WRT the
    * printed page) and its width and height. All measurements in inches. Both dimensions must be strictly positive.
    * @return The figure. Returns null if argument is invalid.
    */
   public static FGraphicModel createEmptyFigure(double[] loc)
   {
      if(!Utilities.isWellDefined(loc) || loc.length != 4) return(null);
      FGraphicModel fgm = new FGraphicModel();
      FigureNode fig = (FigureNode) fgm.getRoot();
      if(!fig.setXY(new Measure(loc[0], Measure.Unit.IN), new Measure(loc[1], Measure.Unit.IN))) return(null);
      if(!fig.setWidth(new Measure(loc[2], Measure.Unit.IN))) return(null);
      if(!fig.setHeight(new Measure(loc[3], Measure.Unit.IN))) return(null);
      return(fgm);
   }
   
   /**
    * Return an independent copy of a <i>DataNav</i> figure's graphic model.
    * @param src The graphic model to be copied. Its content is not changed in any way. 
    * @return An independent copy of model. Any non-persistent state (rendering infrastructure, edit history, modified
    * flag, current selection) is NOT copied. If source model is null, method returns null. 
    */
   public static FGraphicModel copy(FGraphicModel src)
   {
      if(src == null) return(null);
      
      FGraphicModel dst = new FGraphicModel();
      try {dst.root = (FigureNode) src.getRoot().clone(); }
      catch(CloneNotSupportedException e) { throw new AssertionError("Unable to clone figure!"); }
      dst.currSelection = new ArrayList<>();
      dst.currSelection.add(dst.root);
      dst.root.setOwnerModel(dst);
      dst.fireFocusChanged();
      dst.isModified = false;
      dst.modifyCount = 0L;
      dst.clearEditHistory();
      
      return(dst);
   }
   
   /* NO LONGER USED -- would need to be updated for newer graph containers Graph3DNode and PolarPlotNode
   public static FGraphicModel copyModelWithoutData(FGraphicModel src)
   {
      if(src == null) return(null);
      FGraphicModel dst = new FGraphicModel();
      
      // clone the root figure node, then empty all datasets in the cloned figure. Note that, since datasets are 
      // immutable, the datasets in the source model will be unaffected!
      boolean dataRemoved = false;
      FigureNode fig = (FigureNode) ((FigureNode)src.getRoot()).clone();
      Stack<FGraphicNode> nodeStack = new Stack<FGraphicNode>();
      nodeStack.push(fig);
      while(!nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();
         if(n instanceof FGNPlottableData) 
         {
            FGNPlottableData plottable = (FGNPlottableData) n;
            DataSet set = plottable.getDataSet();
            plottable.setDataSet(DataSet.createEmptySet(set.getFormat(), set.getID()));
            dataRemoved = true;
         }
         else if(n instanceof GraphNode || n instanceof FigureNode)
         {
            for(int i=0; i<n.getChildCount(); i++)
               nodeStack.push(n.getChildAt(i));
         }
      }

      dst.root = fig;
      dst.currSelection = new ArrayList<FGraphicNode>();
      dst.currSelection.add(dst.root);

      dst.root.setOwnerModel(dst);
      dst.fireFocusChanged();
      dst.isModified = dataRemoved ? true : src.isModified;
      dst.modifyCount = src.modifyCount + (dataRemoved ? 1 : 0);
      dst.clearEditHistory();
      
      return(dst);
   }
   */
   
   /**
    * Create an offscreen image of the specified figure that is compatiable with the given graphics configuration.
    * The image is rendered on an opaque white background with antialiasing and text-antialiasing turned on, and with 
    * rendering geared toward quality rather than speed.
    * 
    * @param model Graphic model of the figure.
    * @param gc Graphics configuration with which generated image should be compatible.
    * @param dpi Image resolution in pixels per inch, needed to calculate size of image given the figure's size in 
    * inches.
    * @return An offscreen image in which the figure has been rendered with 1-to-1 scaling. If the figure model or 
    * graphics configuration is not provided, if the image resolution is unreasonable, or if an error occurs, the 
    * method returns <code>null</code>.
    */
   public static BufferedImage getOffscreenImage(FGraphicModel model, GraphicsConfiguration gc, float dpi)
   {
      if(model == null || gc == null) return(null);
      if(dpi < 50 || dpi > 2400) return(null);
      
      RootRenderable root = model.getRoot();
      int w = (int) Math.floor( 0.5 + root.getWidthMI() * dpi / 1000);
      int h = (int) Math.floor( 0.5 + root.getHeightMI() * dpi / 1000);
      BufferedImage bi = gc.createCompatibleImage(w, h);
      if(bi == null) return(null);
      
      Graphics2D g2d = bi.createGraphics();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      
      g2d.setColor(Color.WHITE);
      g2d.fillRect(0, 0, w, h);
      g2d.scale(dpi/1000.0, -dpi/1000.0);
      g2d.translate(0, -root.getHeightMI());
      if(!root.render(g2d, null)) return(null);

      return(bi);
   }
   
   /**
    * An author may attach a unique node ID -- the <i>id</i> attribute</i> to selected graphic nodes in a figure. This
    * helper method is called to ensure ID uniqueness. It checks the string provided against all non-empty node IDs 
    * assigned to other nodes in the containing figure model. If the string does not match any existing ID, it is 
    * returned unchanged; else it is altered (by adding an integer suffix like "_1"). An empty string is returned 
    * unchanged (an empty string is the same as no ID at all). Also, if the node is not currently part of a figure 
    * model, the ID string cannot be checked for uniqueness and will be returned unchanged.
    * 
    * @param fgn The graphic node.
    * @param s The candidate object ID.
    * @return The object ID, possibly modified to ensure uniqueness.
    */
   static String ensureUniqueGraphicObjectID(FGraphicNode fgn, String s)
   {
      if(s.isEmpty()) return(s);
      FGraphicModel model = (fgn != null) ? fgn.getGraphicModel() : null;
      if(model == null) return(s);
      
      // traverse figure model object tree and collect any existing node IDs (skip the node in question, though)
      HashMap<String, Object> idMap = new HashMap<>();
      Stack<FGraphicNode> nodeStack = new Stack<>();
      nodeStack.push(model.root);
      while(!nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();
         if(n != fgn)
         {
            String id = n.getID();
            if(!id.isEmpty()) idMap.put(id, null);
         }
         
         for(int i=0; i<n.getChildCount(); i++)
            nodeStack.push(n.getChildAt(i));
      }
 
      // ensure specified node ID is unique among other existing node IDs
      String original = s;
      int i = 1;
      while(idMap.containsKey(s)) s = original + "_" + (i++);
      
      return(s);
   }
   
   /**
    * Replace the root figure's <i>note</i> property. (Intended only to support Matlab script-based modification of a 
    * <i>FypML</i> figure.)
    * 
    * @param text The replacement note/description of figure. Line-feed characters are permitted to support a multi-line
    * note. All tabs and carriage-returns are stripped. Any unsupported characters are replaced by a '?'. Null is 
    * treated as an empty string.
    * @return True if successful; false if root figure node is missing (should never happen).
    */
   public boolean replaceFigureNote(String text)
   {
      if(root == null || root.getNodeType() != FGNodeType.FIGURE) return(false);
      
      ((FigureNode) root).setNote(text);
      return(true);
   }
   
   /**
    * Replace the <i>title</i> property of an identified graphic node within this figure. (Intended only to support 
    * Matlab script-based modification of a <i>FypML</i> figure.)
    * 
    * <p><i>Special case - data presentation nodes</i>. Data presentation nodes are identified not by their own object
    * ID, but by the ID assigned to their source data set. Typically, there's a one-to-one correspondence between a
    * presentation node and it source, but not always -- <i>FypML</i> permits multiple presentation nodes to share the
    * same data set. In this latter scenario, the method will <b>change the title of every presentation node sharing the
    * data set with the identifier specified in the <i>id</i> argument</b>.</p>
    * 
    * @param id Identifier assigned to the target graphic object. For presentation nodes, this is the ID of the node's 
    * source data set. For all other graphic objects, it is the node's (unique) object ID (only certain graphic elements
    * in a <i>FypML</i> figure possess such an ID).
    * @param text The replacement title. If the target object is NOT a <i>text box</i> node, any line-feed characters
    * in the replacement text will be stripped. All tabs and carriage-returns are stripped. Any unsupported characters
    * are replaced by a '?'. Null is treated as an empty string.
    * @return True if successful; false if no target object not found.
    */
   public boolean replaceObjectTitleInFigure(String id, String text)
   {
      if(id == null || id.isEmpty()) return(false);
      
      int count = 0; // in case we're updating data presentation nodes, make sure we update at least one.
      
      Stack<FGraphicNode> nodeStack = new Stack<>();
      nodeStack.push(root);
      while(!nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();
         if(n instanceof FGNPlottableData)
         {
            if(((FGNPlottableData) n).getDataSetID().equals(id))
            {
               n.setTitle((text == null) ? "" : text);
               ++count;
            }
         }
         else if(n.getID().equals(id))
         {
            if(n.hasTitle())
            {
               n.setTitle((text == null) ? "" : text);
               return(true);
            }
            else
               return(false);
         }
         
         for(int i=0; i<n.getChildCount(); i++)
            nodeStack.push(n.getChildAt(i));
      }

      return(count > 0);
   }

   /**
    * Replace the legend label for one data group in an identified "grouped-data" presentation node within this figure.
    * Only bar, area, and pie charts fall into this category -- they present a separate legend entry for each such data
    * group. (Intended only to support Matlab script-based modification of a <i>FypML</i> figure.)
    * 
    * @param id Identifier assigned to the source data set for the target grouped-data presentation object. NOTE: Every 
    * bar/area/pie chart in the figure that shares the identified data set will be updated in the same manner. It is 
    * highly unlikely that there would be more than one such plot, but it is possible because data presentation nodes 
    * can share the same source data set.
    * @param pos The index position of the target data group within that bar/area/pie chart.
    * @param label The replacement legend label. All tabs, line-feeds and carriage-returns are stripped. Any unsupported
    * characters are replaced by a '?'. Null is treated as an empty string.
    * @return True if successful; false if target presentation node not found or data group index invalid.
    */
   public boolean replaceDataGroupLegendLabelInFigure(String id, int pos, String label)
   {
      if(id == null || id.isEmpty() || pos < 0) return(false);
      
      List<FGNPlottableData> plottables = getAllPlottableDataNodes();
      boolean updated = false;
      for(FGNPlottableData plottable : plottables) if(plottable.hasDataGroups() && plottable.getDataSetID().equals(id))
      {
         if(plottable.setDataGroupLabel(pos, label)) updated = true;
      }
      return(updated);
   }

   /**
    * Replace an existing 2D or 3D graph in this figure. (Intended only to support Matlab script-based modification of a 
    * <i>FypML</i> figure.)
    * 
    * @param id Object identifier that locates the 2D or 3D graph node to be replaced. The identified node must be an
    * instance of {@link GraphNode}, {@link PolarPlotNode}, or {@link Graph3DNode}; else the operation fails.
    * @param graph The replacement graph. It must be one of the 3 supported graph containers, <i>but it need not be the 
    * same type as the graph node it replaces</i>. The replacement graph will inherit the location, dimensions, and 
    * basic text/draw styling of the original graph -- to minimize the work that may be required to make the replacement
    * graph "look nice" within the figure as a whole.
    * @return True if successful, false otherwise (existing graph not found, insertion of replacement graph failed).
    */
   public boolean replaceGraphInFigure(String id, FGNGraph graph)
   {
      if(graph == null || id == null || id.isEmpty()) return(false);
      
      // find the graph to be replaced. It could be a 2D or a 3D graph.
      FGNGraph oldG = null;
      Stack<FGraphicNode> nodeStack = new Stack<>();
      nodeStack.push(root);
      while(!nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();
         FGNodeType nt = n.getNodeType();
         if(n.getID().equals(id) && (nt==FGNodeType.GRAPH || nt==FGNodeType.PGRAPH || nt==FGNodeType.GRAPH3D))
         {
            oldG = (FGNGraph) n;
            break;
         }
         
         for(int i=0; i<n.getChildCount(); i++)
            nodeStack.push(n.getChildAt(i));
      }
      if(oldG == null) return(false);
      
      // ensure replacement graph is not currently in another figure!
      if(graph.getGraphicModel() != null)
      {
         try { graph = graph.clone(); }
         catch(CloneNotSupportedException e)
         {
            return (false);
         }
      }
      
      // set location and size of replacement graph to match the graph being replaced. Also preserve the object ID!
      // NOTE: This does NOT work well when the two graphs are not both 2D or both 3D, because location and dimensions
      // have a different interpretation for 2D vs 3D, and it's a fairly intractable problem to make the necessary
      // conversions between the two.
      graph.setID(oldG.getID());
      graph.setXY(oldG.getX(), oldG.getY());
      graph.setWidth(oldG.getWidth());
      graph.setHeight(oldG.getHeight());
      if(graph instanceof Graph3DNode)
         ((Graph3DNode) graph).setDepth(((Graph3DNode) oldG).getDepth());
      
      // X-axis: preserve axis properties that affect spacing. If both old and new axis have a major tick set, preserve
      // tick length, gap, and direction.
      // IMPORTANT: the axes of the specialized 2D polar graph do NOT have some of the properties of the axes in the 
      // standard 2D and the 3D graph objects, and it lacks tick sets. When this polar graph object is the old or 
      // replacement graph, we don't attempt to preserve those axis properties...
      Measure spacer = null;
      Measure labelOfs = null;
      double lineHt = 0;
      Measure tickLen = null;
      Measure tickGap = null;
      TickSetNode.Orientation tickOri = null;
      boolean gotProps = true;
      if(oldG instanceof GraphNode)
      {
         AxisNode ax = ((GraphNode) oldG).getPrimaryAxis();
         spacer = ax.getSpacer();
         labelOfs = ax.getLabelOffset();
         lineHt = ax.getLineHeight();
         TickSetNode ticks = ax.getMajorTickSet();
         if(ticks != null)
         {
            tickLen = ticks.getTickLength();
            tickGap = ticks.getTickGap();
            tickOri = ticks.getTickOrientation();
         }
      }
      else if(oldG instanceof Graph3DNode)
      {
         Axis3DNode ax3 = ((Graph3DNode) oldG).getAxis(Graph3DNode.Axis.X);
         spacer = ax3.getSpacer();
         labelOfs = ax3.getLabelOffset();
         lineHt = ax3.getLineHeight();
         Ticks3DNode t3 = ax3.getMajorTickSet();
         if(t3 != null)
         {
            tickLen = t3.getTickLength();
            tickGap = t3.getTickGap();
            tickOri = t3.getTickOrientation();
         }
      }
      else gotProps = false;
      
      if(gotProps && (graph instanceof GraphNode))
      {
         AxisNode ax = ((GraphNode) graph).getPrimaryAxis();
         ax.setSpacer(spacer);
         ax.setLabelOffset(labelOfs);
         ax.setLineHeight(lineHt);
         TickSetNode ticks = ax.getMajorTickSet();
         if(ticks != null && tickLen != null)
         {
            ticks.setTickLength(tickLen);
            ticks.setTickGap(tickGap);
            ticks.setTickOrientation(tickOri);
         }
      }
      else if(gotProps && (graph instanceof Graph3DNode))
      {
         Axis3DNode ax3 = ((Graph3DNode) graph).getAxis(Graph3DNode.Axis.X);
         ax3.setSpacer(spacer);
         ax3.setLabelOffset(labelOfs);
         ax3.setLineHeight(lineHt);
         Ticks3DNode t3 = ax3.getMajorTickSet();
         if(t3 != null && tickLen != null)
         {
            t3.setTickLength(tickLen);
            t3.setTickGap(tickGap);
            t3.setTickOrientation(tickOri);
         }
      }
      
      // repeat for Y-axis
      tickLen = null;
      gotProps = true;
      if(oldG instanceof GraphNode)
      {
         AxisNode ax = ((GraphNode) oldG).getSecondaryAxis();
         spacer = ax.getSpacer();
         labelOfs = ax.getLabelOffset();
         lineHt = ax.getLineHeight();
         TickSetNode ticks = ax.getMajorTickSet();
         if(ticks != null)
         {
            tickLen = ticks.getTickLength();
            tickGap = ticks.getTickGap();
            tickOri = ticks.getTickOrientation();
         }
      }
      else if(oldG instanceof Graph3DNode)
      {
         Axis3DNode ax3 = ((Graph3DNode) oldG).getAxis(Graph3DNode.Axis.Y);
         spacer = ax3.getSpacer();
         labelOfs = ax3.getLabelOffset();
         lineHt = ax3.getLineHeight();
         Ticks3DNode t3 = ax3.getMajorTickSet();
         if(t3 != null)
         {
            tickLen = t3.getTickLength();
            tickGap = t3.getTickGap();
            tickOri = t3.getTickOrientation();
         }
      }
      else gotProps = false;
      
      if(gotProps && (graph instanceof GraphNode))
      {
         AxisNode ax = ((GraphNode) graph).getSecondaryAxis();
         ax.setSpacer(spacer);
         ax.setLabelOffset(labelOfs);
         ax.setLineHeight(lineHt);
         TickSetNode ticks = ax.getMajorTickSet();
         if(ticks != null && tickLen != null)
         {
            ticks.setTickLength(tickLen);
            ticks.setTickGap(tickGap);
            ticks.setTickOrientation(tickOri);
         }
      }
      else if(gotProps && (graph instanceof Graph3DNode))
      {
         Axis3DNode ax3 = ((Graph3DNode) graph).getAxis(Graph3DNode.Axis.Y);
         ax3.setSpacer(spacer);
         ax3.setLabelOffset(labelOfs);
         ax3.setLineHeight(lineHt);
         Ticks3DNode t3 = ax3.getMajorTickSet();
         if(t3 != null && tickLen != null)
         {
            t3.setTickLength(tickLen);
            t3.setTickGap(tickGap);
            t3.setTickOrientation(tickOri);
         }
      }
      
      // repeat for Z-axis of a 3D graph -- only if the original and replacement graphs are both 3D
      tickLen = null;
      if((oldG instanceof Graph3DNode) && (graph instanceof Graph3DNode))
      {
         Axis3DNode ax3 = ((Graph3DNode) oldG).getAxis(Graph3DNode.Axis.Z);
         spacer = ax3.getSpacer();
         labelOfs = ax3.getLabelOffset();
         lineHt = ax3.getLineHeight();
         Ticks3DNode t3 = ax3.getMajorTickSet();
         if(t3 != null)
         {
            tickLen = t3.getTickLength();
            tickGap = t3.getTickGap();
            tickOri = t3.getTickOrientation();
         }
         
         ax3 = ((Graph3DNode) graph).getAxis(Graph3DNode.Axis.Z);
         ax3.setSpacer(spacer);
         ax3.setLabelOffset(labelOfs);
         ax3.setLineHeight(lineHt);
         t3 = ax3.getMajorTickSet();
         if(t3 != null && tickLen != null)
         {
            t3.setTickLength(tickLen);
            t3.setTickGap(tickGap);
            t3.setTickOrientation(tickOri);
         }
      }
      
      // repeat for graph's color bar. All graph containers support a color bar.
      tickLen = null;
      ColorBarNode cbar = oldG.getColorBar();
      spacer = cbar.getBarAxisSpacer();
      labelOfs = cbar.getLabelOffset();
      lineHt = cbar.getLineHeight();
      Measure barSz = cbar.getBarSize();
      TickSetNode ticks = cbar.getMajorTickSet();
      if(ticks != null)
      {
         tickLen = ticks.getTickLength();
         tickGap = ticks.getTickGap();
         tickOri = ticks.getTickOrientation();
      }

      cbar = graph.getColorBar();
      cbar.setBarAxisSpacer(spacer);
      cbar.setLabelOffset(labelOfs);
      cbar.setLineHeight(lineHt);
      if(barSz != null) cbar.setBarSize(barSz);
      ticks = cbar.getMajorTickSet();
      if(ticks != null && tickLen != null)
      {
         ticks.setTickLength(tickLen);
         ticks.setTickGap(tickGap);
         ticks.setTickOrientation(tickOri);
      }
            
      // preserve style properties from original graph and its individual axes
      // NOTE: Here we can equate the X-axis of a Cartesian 2D or 3D graph with the theta axis of the specialized 2D
      // polar graph; similarly with the Y-axis and radial axis. Again, though, the axes in the specialized polar graph
      // do not have tick sets. All have the standard style properties.
      FGraphicNode ticksOld = null;
      FGraphicNode ticksNew = null;
      List<FGraphicNode> nodes = new ArrayList<>();
      nodes.add(graph);
      nodes.add(oldG);
      if(graph instanceof GraphNode)
      {
         AxisNode ax = ((GraphNode) graph).getPrimaryAxis();
         nodes.add(ax);
         ticksNew = ax.getMajorTickSet();
      }
      else if(graph instanceof PolarPlotNode)
      {
         PolarAxisNode ax = ((PolarPlotNode) graph).getThetaAxis();
         nodes.add(ax);
      }
      else if(graph instanceof Graph3DNode)
      {
         Axis3DNode ax3 = ((Graph3DNode) graph).getAxis(Graph3DNode.Axis.X);
         nodes.add(ax3);
         ticksNew = ax3.getMajorTickSet();
      }

      if(oldG instanceof GraphNode)
      {
         AxisNode ax = ((GraphNode) oldG).getPrimaryAxis();
         nodes.add(ax);
         ticksOld = ax.getMajorTickSet();
      }
      else if(oldG instanceof PolarPlotNode)
      {
         PolarAxisNode ax = ((PolarPlotNode) oldG).getThetaAxis();
         nodes.add(ax);
      }
      else if(oldG instanceof Graph3DNode)
      {
         Axis3DNode ax3 = ((Graph3DNode) oldG).getAxis(Graph3DNode.Axis.X);
         nodes.add(ax3);
         ticksOld = ax3.getMajorTickSet();
      }

      if(ticksNew != null && ticksOld != null)
      {
         nodes.add(ticksNew);
         nodes.add(ticksOld);
      }

      ticksOld = ticksNew = null;
      if(graph instanceof GraphNode)
      {
         AxisNode ax = ((GraphNode) graph).getSecondaryAxis();
         nodes.add(ax);
         ticksNew = ax.getMajorTickSet();
      }
      else if(graph instanceof PolarPlotNode)
      {
         PolarAxisNode ax = ((PolarPlotNode) graph).getRadialAxis();
         nodes.add(ax);
      }
      else if(graph instanceof Graph3DNode)
      {
         Axis3DNode ax3 = ((Graph3DNode) graph).getAxis(Graph3DNode.Axis.Y);
         nodes.add(ax3);
         ticksNew = ax3.getMajorTickSet();
      }

      if(oldG instanceof GraphNode)
      {
         AxisNode ax = ((GraphNode) oldG).getSecondaryAxis();
         nodes.add(ax);
         ticksOld = ax.getMajorTickSet();
      }
      else if(oldG instanceof PolarPlotNode)
      {
         PolarAxisNode ax = ((PolarPlotNode) oldG).getRadialAxis();
         nodes.add(ax);
      }
      else if(oldG instanceof Graph3DNode)
      {
         Axis3DNode ax3 = ((Graph3DNode) oldG).getAxis(Graph3DNode.Axis.Y);
         nodes.add(ax3);
         ticksOld = ax3.getMajorTickSet();
      }

      if(ticksNew != null && ticksOld != null)
      {
         nodes.add(ticksNew);
         nodes.add(ticksOld);
      }

      if((oldG instanceof Graph3DNode) && (graph instanceof Graph3DNode))
      {
         Axis3DNode ax3 = ((Graph3DNode) graph).getAxis(Graph3DNode.Axis.Z);
         nodes.add(ax3);
         ticksNew = ax3.getMajorTickSet();

         ax3 = ((Graph3DNode) oldG).getAxis(Graph3DNode.Axis.Z);
         nodes.add(ax3);
         ticksOld = ax3.getMajorTickSet();

         if(ticksNew != null && ticksOld != null)
         {
            nodes.add(ticksNew);
            nodes.add(ticksOld);
         }
      }
      
      // all graph containers have a color bar
      nodes.add(graph.getColorBar());
      nodes.add(oldG.getColorBar());
      ticksNew = graph.getColorBar().getMajorTickSet();
      ticksOld = oldG.getColorBar().getMajorTickSet();
      if(ticksNew != null && ticksOld != null)
      {
         nodes.add(ticksNew);
         nodes.add(ticksOld);
      }
      
      for(int i=0; i<nodes.size(); i+=2)
      {
         FGraphicNode n = nodes.get(i); 
         FGraphicNode old = nodes.get(i+1);
         n.setFontFamily(old.getFontFamily());
         n.setAltFont(old.getAltFont());
         n.setPSFont(old.getPSFont());
         n.setFontStyle(old.getFontStyle());
         n.setFontSizeInPoints(old.getFontSizeInPoints());
         n.setFillColor(old.getFillColor());
         n.setMeasuredStrokeWidth(old.getMeasuredStrokeWidth());
         n.setStrokeColor(old.getStrokeColor());
         n.setStrokeEndcap(old.getStrokeEndcap());
         n.setStrokeJoin(old.getStrokeJoin());
      }
      
      // insert replacement graph before graph being replaced, then delete the old graph
      FGraphicNode parent = oldG.getParent();
      if(insertNode(parent, graph, parent.getIndexOf(oldG)))
      {
         deleteNode(oldG);
         return(true);
      }
      
      return(false);
   }
   
   /**
    * Insert a 2D or 3D graph into this figure with specified location and dimensions. (Intended only to support Matlab 
    * script-based modification of a <i>FypML</i> figure.)
    * 
    * @param graph The graph container node to be inserted. 
    * @param loc An array specifying the location and size of the graph. For a 2D graph, it must be a 4-element array
    * <i>[x y w h]</i> specifying the location, width, and height the graph in the parent figure's viewport. For a 3D
    * graph, it must be a 5-element array <i>[x y w h d]</i> specifying the location of the 3D graph's origin in the
    * parent viewport, and its 3D width, height and depth. All measurements in inches. All dimensions must be strictly 
    * positive.
    * @return True if successful, false otherwise (invalid argument; insertion of new graph failed).
    */
   public boolean addGraphToFigure(FGNGraph graph, double[] loc)
   {
      if(graph == null || !Utilities.isWellDefined(loc)) return(false);
      
      boolean is2D = (graph.getNodeType()==FGNodeType.GRAPH || graph.getNodeType()==FGNodeType.PGRAPH);
      if(!((is2D && loc.length==4) || ((!is2D) && loc.length==5)))
         return(false);
      
      // if the graph is currently in another figure, clone it
      if(graph.getGraphicModel() != null)
      {
         try { graph = graph.clone(); }
         catch(CloneNotSupportedException e) { return(false); }
      }

      // set location and dimensions of the new graph as specified
      boolean ok = graph.setXY(new Measure(loc[0], Measure.Unit.IN), new Measure(loc[1], Measure.Unit.IN));
      if(ok) ok = graph.setWidth(new Measure(loc[2], Measure.Unit.IN));
      if(ok) ok = graph.setHeight(new Measure(loc[3], Measure.Unit.IN));
      if(ok && loc.length==5) ok = ((Graph3DNode)graph).setDepth(new Measure(loc[4], Measure.Unit.IN));
      if(!ok) return(false);
      
      // append the graph to the figure
      FigureNode fig = (FigureNode) getRoot();
      return(insertNode(fig, graph, -1));
   }

   //
   // Dataset-related operations
   //
   
   /* NO LONGER USED
   public boolean appendPlottableDataNode(DataSet ds, GraphNode g)
   {
      if(ds == null || g == null || g.getGraphicModel() != this) return(false);
      
      Fmt fmt = ds.getFormat();
      ds = DataSet.ensureUniqueIdentifier(ds, getAllDataSetsInUse());
      FGNodeType nt = null;
      switch(fmt)
      {
      case RASTER1D : nt = FGNodeType.RASTER; break;
      case XYZIMG   : nt = FGNodeType.CONTOUR; break;
      case XYZSET   : nt = FGNodeType.SCATTER; break;
      default       : nt = FGNodeType.TRACE; break;
      }
      FGNPlottableData dataNode =  (FGNPlottableData) createGraphicNode(nt);
      dataNode.setDataSet(ds);
      return(insertNode(g, dataNode, -1));   
   }
   */
   
   /**
    * Prepare a list of all data presentation nodes found in this figure graphic model. 
    * @return List of all dataset presentation nodes currently defined in model, in no particular order.
    */
   public List<FGNPlottableData> getAllPlottableDataNodes()
   {
      List<FGNPlottableData> plottables = new ArrayList<>();
      if(root == null) return(plottables);
      
      Stack<FGraphicNode> nodeStack = new Stack<>();
      nodeStack.push(root);
      while(!nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();
         if(n instanceof FGNPlottableData) 
            plottables.add((FGNPlottableData)n);
         else if(n instanceof FGNGraph || n instanceof FigureNode)
         {
            for(int i=0; i<n.getChildCount(); i++)
               nodeStack.push(n.getChildAt(i));
         }
      }
      
      return(plottables);
   }
   
   /**
    * Get a list of all datasets currently rendered within this <code>FGraphicModel</code>.
    * 
    * <p><strong>NOTE</strong>: The method silently removes duplicate dataset references (where two presentation nodes
    * hold different <code>DataSet</code> instances that are identical in every way), and ensures that no two distinct 
    * datasets have the same ID. The relevant presentation nodes are updated as these changes are made (remember that 
    * the <code>DataSet</code> object is immutable). These changes are not posted to the undo history, nor invalidate 
    * that history. Also, since the data itself is not changing, there's no effect on the current rendered appearance 
    * of the model.</p>
    * 
    * @return The list of datasets rendered in this model (possibly empty).
    */
   public List<DataSet> getAllDataSetsInUse()
   {
      blockEditHistory();
      List<DataSet> sets = new ArrayList<>();
      List<FGNPlottableData> dsNodes = getAllPlottableDataNodes();
      for(FGNPlottableData dsn : dsNodes)
      {
         DataSet ds = dsn.getDataSet();
         
         for(int i=0; i<sets.size(); i++)
         {
            DataSet other = sets.get(i);
            if(DataSet.areIdenticalSets(other, ds, true))
            {
               if(other.getID().equals(ds.getID()))
               {
                  if(other != ds) dsn.setDataSet(other);
                  ds = null;
                  break;
               }
            }
            else if(other.getID().equals(ds.getID()))
            {
               ds = DataSet.ensureUniqueIdentifier(ds, sets);
               dsn.setDataSet(ds);
            }
         }
         
         if(ds != null) sets.add(ds);
      }
      unblockEditHistory();
      
      return(sets);
   }

   /**
    * Retrieve the dataset in the current figure that has the specified ID.
    * @param id The ID of the requested dataset.
    * @return The requested dataset, or <code>null</code> if it was not found.
    */
   public DataSet getDataset(String id)
   {
      if(id == null) return(null);
      
      List<FGNPlottableData> dsNodes = getAllPlottableDataNodes();
      for(FGNPlottableData dsn : dsNodes) if(id.equals(dsn.getDataSetID())) return(dsn.getDataSet());

      return(null);
   }
   
   /**
    * Change the current focus node to the data presentation node containing the dataset with the specified ID.
    * @param id A dataset ID. If no dataset in figure has this ID, this method takes no action.
    */
   public void putFocusOnDataset(String id)
   {
      if(id == null) return;
      List<FGNPlottableData> dsNodes = getAllPlottableDataNodes();
      for(FGNPlottableData dsn : dsNodes) if(id.equals(dsn.getDataSetID())) 
      {
         setSelectedNode(dsn);
         break;
      }
   }
   
   /**
    * Find all data sets in the current figure that have the same ID and are compatible with an entry in the specified
    * array of data set summary information. Method is used to determine which data sets in a <i>DataNav</i> source file
    * might be extracted and injected into the figure.
    * @param setInfo Summary information on zero or more candidate data sets.
    * @return List of entries in the input argument that match existing data sets in this figure. If no matches found, 
    * list will be empty.
    */
   public List<DataSetInfo> findMatchingDatasetsInFigure(DataSetInfo[] setInfo)
   {
      List<DataSetInfo> matches = new ArrayList<>();
      if(setInfo == null || setInfo.length == 0) return(matches);
      
      List<FGNPlottableData> dsNodes = getAllPlottableDataNodes();
      for(DataSetInfo dsi : setInfo) 
      {
         String id = dsi.getID();
         Fmt fmt = dsi.getFormat();
         for(FGNPlottableData dsn : dsNodes) if(id.equals(dsn.getDataSetID()) && dsn.isSupportedDataFormat(fmt))
         {
            matches.add(dsi);
            break;
         }
      }
      return(matches);
   }
   
   /**
    * Replace a single data set displayed within this FypML figure. See {@link #replaceDataSetsInUse(List, boolean)}.
    * @param ds The replacement data set. If null, the method has no effect.
    * @return True if successful; false otherwise.
    */
   public boolean replaceDataSetInUse(DataSet ds)
   {
      if(ds == null) return(false);
      List<DataSet> sets = new ArrayList<>();
      sets.add(ds);
      int n = replaceDataSetsInUse(sets, true);
      return(n==1);
   }
   
   /**
    * A convenience method that replaces one or more data sets displayed within the figure graphic. It is intended to 
    * support injecting data sets into a "template" figure containing empty place holder sets. It also may be useful 
    * when reconstructing a model that was saved to file in some format.
    * <p>For each {@link FGNPlottableData} in the model, the method searches the replacement data set list for a set 
    * with the same ID as that currently rendered by the presentation element; the replacement set's data format must 
    * also be supported by that presentation element. If a such a replacement set is found <b>AND it is not identical to
    * the presentation element's current data set</b>, the replacement set becomes the element's new data set; else, the
    * data presentation element is left unchanged.</p>
    * <p><b>NOTE</b>: This method is also used when converting a FypML figure schema document into a graphic model. 
    * During conversion, the model is constructed with all data presentation nodes containing default data sets, then 
    * the actual data sets are read from the document and injected into the model with this method. If a set happens to 
    * be identical to the default data set currently installed in a data presentation node, then it would not normally
    * be counted as having been used. This would cause the conversion procedure to fail, and the valid schema document 
    * could no longer be opened. To avoid this issue, the <i>force</i> flag should be set in this use case. It should
    * not be set when injecting data in to a template figure.</p>
    * @param sets The replacement data sets. If null or empty, the method has no effect.
    * @param force If true, a replacement set is used even if it is identical to a data presentation node's current
    * data set. <i>See NOTE above.</i>
    * @return The number of data sets in the list that were actually used.
    */
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public int replaceDataSetsInUse(List<DataSet> sets, boolean force)
   {
      if(sets == null || sets.isEmpty()) return(0);
      
      // to keep track of which source sets were used
      boolean[] used = new boolean[sets.size()];

      // prepare a list of (oldDS, newDS, datanode) pairs, where oldDs and newDS are the current and replacement 
      // datasets and datanode is the target presentation node
      List replaceList = new ArrayList();
      List<FGNPlottableData> dsNodes = getAllPlottableDataNodes();
      for(FGNPlottableData dsn : dsNodes)
      {
         String id = dsn.getDataSetID();
         DataSet currDS = dsn.getDataSet();
         
         for(int i=0; i<sets.size(); i++)
         {
            DataSet ds = sets.get(i);
            if(ds == null) continue;
            if(ds.getID().equals(id) && dsn.isSupportedDataFormat(ds.getFormat()) && (force || !ds.equals(currDS)))
            {
               replaceList.add(currDS);
               replaceList.add(ds);
               replaceList.add(dsn);
               used[i] = true;
               break;
            }
         }
      }
      
      // handle special cases: 0 or 1 set replaced
      if(replaceList.isEmpty()) return(0);
      if(replaceList.size() == 3)
      {
         DataSet ds = (DataSet) replaceList.get(1);
         FGNPlottableData dsn = (FGNPlottableData) replaceList.get(2);
         dsn.setDataSet(ds);
         return(1);
      }
      
      int nUsed = 0;
      for(boolean b : used) if(b) nUsed++;
      
      // handle multiple dataset replacement as a single reversible edit. We block the edit history while replacing the
      // data sets, then unblock it and update it with a reversible edit that can undo what we've just done!
      blockEditHistory();

      for(int i=0; i<replaceList.size(); i+=3)
      {
         DataSet ds = (DataSet) replaceList.get(i+1);
         FGNPlottableData dsn = (FGNPlottableData) replaceList.get(i+2);
         dsn.setDataSet(ds);
      }
      
      unblockEditHistory();
      postReversibleEdit(new FGMRevOp(replaceList));
      return(nUsed);
   }
   
   /**
    * A convenience method that replaces one or more data sets displayed within this <code>FGraphicModel</code> with
    * an empty set of the same format. It is intended for use when reinitializing a "template" figure prior to injecting
    * new data into it. <i>If any changes are made here, they may not be undone, so the edit history is cleared.</i>
    * 
    * <p>For each data presentation element ({@link FGNPlottableData}) defined in the model, the method checks to see
    * if the specified ID list includes the ID of the set currently rendered by the presentation element. If so and if
    * the current data set is not empty, that set is replaced by an empty one in the same format; otherwise, the 
    * presentation element is left unchanged.</p>
    * @param ids IDs of the model data sets that should be emptied. If null or empty, the method has no effect.
    */
   public void emptyDataSetsInUse(List<String> ids)
   {
      if(ids == null || ids.isEmpty()) return;
      
      boolean changed = false;
      blockEditHistory();
      
      List<FGNPlottableData> dsNodes = getAllPlottableDataNodes();
      for(FGNPlottableData dsn : dsNodes)
      {
         DataSet currDS = dsn.getDataSet();
         if(currDS.isEmpty()) continue;
         
         for(String id : ids) if(currDS.getID().equals(id))
         {
            DataSet emptyDS = DataSet.createEmptySet(currDS.getFormat(), id);
            dsn.setDataSet(emptyDS);
            changed = true;
            break;
         }
      }

      unblockEditHistory();
      if(changed) clearEditHistory();
   }
   
   
   //
   // Constraints on location and size of graphic objects
   //

   /**
    * Constraints on the location coordinates of a movable graphic node (except a figure or graph). Does not limit the 
    * value or units, but limits number of significant and fractional digits to 7 and 3, respectively.
    */
   private final static Measure.Constraints COORDCONSTRAINTS = new Measure.Constraints(7, 3, true);

   /** 
    * Constraints on the location coordinates of a figure node: use physical units, limited to 5 significant and 2
    * fractional digits, and lie in the range [-100..100in], regardless what units are assigned.
    */
   private final static Measure.Constraints FIGCOORDCONSTRAINTS = new Measure.Constraints(-100000.0, 100000.0, 5, 2);

   /**
    * Constraints on the location coordinates of a 2D graph. Does not limit the value, but limits number of significant
    * and fractional digits to 7 and 3, respectively. Allows all supported units <b>EXCEPT '%' units</b>.
    */
   private final static Measure.Constraints GRAPHCOORDCONSTRAINTS = 
      new Measure.Constraints(-Double.MAX_VALUE,Double.MAX_VALUE, -Double.MAX_VALUE,Double.MAX_VALUE, 7,3, false,true);

   /**
    * Constraints on the location coordinates of a 3D graph node. Does not limit the value, but limits number of 
    * significant and fractional digits to 7 and 3, respectively. Allows all supported units <b>EXCEPT 'user' units</b>.

    */
   private final static Measure.Constraints GRAPH3DCOORDCONSTRAINTS = 
      new Measure.Constraints(-Double.MAX_VALUE,Double.MAX_VALUE, -Double.MAX_VALUE,Double.MAX_VALUE, 7,3, true,false);

   /**
    * Get a constraints object that restricts the allowed values for the location coordinates of a movable graphic 
    * object in the model.
    * @param type The graphic node type.
    * @return Measurement constraints that should be applied to the location coordinates of any instance of the 
    * specified node type.
    */
   public static Measure.Constraints getLocationConstraints(FGNodeType type)
   {
      Measure.Constraints c = COORDCONSTRAINTS;
      switch(type)
      {
      case FIGURE : c = FIGCOORDCONSTRAINTS; break;
      case GRAPH :  
      case PGRAPH : c = GRAPHCOORDCONSTRAINTS; break;
      case GRAPH3D: c= GRAPH3DCOORDCONSTRAINTS; break;
      default: break;
      }
      return(c);
   }
   
   /**
    * Constraints on the width and height of a resizable graphic node (exc. figure, graph or shape). Restricts dimension
    * to non-negative values, to 5 significant and 2 fractional digits, and allows any units other than user units.
    */
   private final static Measure.Constraints SIZECONSTRAINTS = 
      new Measure.Constraints(0, Double.MAX_VALUE, 0, Double.MAX_VALUE, 5, 2, true, false);

   /** 
    * Constraints on the width and height of a figure or 2D graph: use physical units, have a maximum of 5 significant
    * and 2 fractional digits, and lie in the range [0..200in], regardless what units are assigned.
    */
   private final static Measure.Constraints FIGSIZECONSTRAINTS = new Measure.Constraints(0, 200000.0, 5, 2);

   /**
    * Constraints on the rendered width or height of any shape node: must use non-relative units (in, cm, mm or pt); 
    * limited to 5 significant and 3 fractional digits; must be nonnegative; and maximum allowed size is 10in.
    */
   public final static Measure.Constraints SHAPESIZECONSTRAINTS = new Measure.Constraints(0, 10000.0, 5, 3);

   /**
    * Constraints on the width, height and depth of a 3D graph node: must use non-relative units (in, cm, mm or pt); 
    * limited to 5 significant and 2 fractional digits; allowed size is 1-100 inches.
    */
   public final static Measure.Constraints GRAPH3DSZCONSTRAINTS = new Measure.Constraints(1000, 100000.0, 5, 2);
   
   /**
    * Get a constraints object that restricts the allowed values for the dimensions of a resizable graphic object 
    * in the model. 
    * @param type The graphic node type.
    * @return Measurement constraints applied to the dimensions of any instance of the specified node type.
    */
   public static Measure.Constraints getSizeConstraints(FGNodeType type)
   {
      Measure.Constraints c = SIZECONSTRAINTS;
      if(type==FGNodeType.SHAPE) c = SHAPESIZECONSTRAINTS;
      else if(type==FGNodeType.FIGURE || type==FGNodeType.GRAPH || type==FGNodeType.PGRAPH) c = FIGSIZECONSTRAINTS; 
      else if(type==FGNodeType.GRAPH3D) c = GRAPH3DSZCONSTRAINTS;

      return(c);
   }
   

   /**
    * Construct a new <i>DataNav</i> graphic model containing only the root figure node. Since it is the only node in 
    * the model, it is selected for display and editing purposes. The model's modified flag is unset.
    */
   public FGraphicModel()
   {
      root = new FigureNode();
      root.setOwnerModel(this);
      currSelection = new ArrayList<>();
      currSelection.add(root);
      isModified = false;
      modifyCount = 0L;
   }

   /**
    * Reset the model so that it consists only of an empty figure node. 
    * <p>The old root figure node is first removed from the model. In an effort to speed up automatic garbage 
    * collection, the tree of nodes descending from the old root is cleared. This operation cannot be undone.</p>
    */
   public void reset()
   {
      // this operation cannot be undone. We disable undo postings during it, then clear the history afterwards.
      blockEditHistory();
      
      // discard all previous content
      if(root != null) 
      {
         root.setOwnerModel(null);
         root.removeAllDescendants();
         root = null;
      }
      
      // and start over with an empty figure
      root = new FigureNode();
      currSelection.clear();
      currSelection.add(root);
      root.setOwnerModel(this);
      fireFocusChanged();
      
      // this operation cannot be undone!
      unblockEditHistory();
      clearEditHistory();
   }

   /**
    * The root of the tree of graphic nodes that define a rendered <em>DataNav</em> graphic. This also serves as the 
    * <code>RootRenderable</code> passed to the <code>RenderableModelViewer</code> registered with this graphic model.
    */
   private FRootGraphicNode root;

   /**
    * Get the root graphic node for the tree of nodes in this graphic model. Currently, this node will <em>always</em> 
    * be an instance of <code>FigureNode</code>.
    * @return The root graphic node in the model.
    */
   public FRootGraphicNode getRoot() { return(root); }

   /**
    * The list of nodes currently selected within this model for editing and display purposes. Typically, the list has
    * a single element, but multiple-node selection is also supported. When more than one node is selected, the LAST
    * node in this list is considered the anchor for the selection.
    */
   private ArrayList<FGraphicNode> currSelection;
   
   /**
    * Get the graphics node within this model that is currently selected for GUI display and editing purposes. If more
    * than one node is currently selected, this method returns the first node in the selection list. Call {@link
    * #getSelectedNodes} to retrieve the complete list.
    * @return The first node in the model's current selection list.
    */
   public FGraphicNode getSelectedNode() { return(currSelection.get(0)); }

   /**
    * Get the list of graphics nodes within this model that are currently selected for GUI display and editing purposes.
    * The last node in the selection list is considered the "anchor" for the selection. It has meaning only when 
    * aligning the nodes in the selection: all nodes are aligned with respect to the anchor!
    * @param selected If not null, this list will be cleared and reinitialized with the list of nodes currently
    * selected. Else, a new list is constructed.
    * @return A reference to the list provided, or the newly constructed list. In either case, the returned list will
    * contain the nodes currently selected in the model.
    */
   public List<FGraphicNode> getSelectedNodes(List<FGraphicNode> selected) 
   { 
      if(selected != null) selected.clear();
      else selected = new ArrayList<>();
      selected.addAll(currSelection);
      return(selected); 
   }
   
   /**
    * Is more than one graphics node currently selected within this model for GUI display and editing purposes?
    * @return True if model's current selection list contains more than one node.
    */
   public boolean isMultiNodeSelection() { return(currSelection.size() > 1); }
   
   /**
    * Is the model's current selection list homogeneous, i.e., are all selected nodes of the same type? When this is the
    * case, it is is possible to apply property changes to all of the nodes as an atomic, reversible operation.
    * 
    * @return True if selection is homogeneous, else false.
    */
   public boolean isHomogeneousSelection()
   {
      if(currSelection.size() <= 1) return(true);
      
      FGNodeType t = currSelection.get(0).getNodeType();
      for(int i=1; i<currSelection.size(); i++)
      {
         if(t != currSelection.get(i).getNodeType()) return(false);
      }

      return(true);
   }
   
   /**
    * Is the specified graphic node among the nodes currently selected in this model?
    * @param n A graphic node.
    * @return True if the node is in the model's current selection list.
    */
   public boolean isSelectedNode(FGraphicNode n) { return(n != null && currSelection.contains(n)); }
   
   /**
    * Set the graphics node that should be currently selected in the model for GUI display and editing purposes. If the 
    * specified node is a valid node in this model, this method will update the current selection accordingly and notify 
    * registered listeners of the change.
    * <p>Note that calling this method replaces the current selection with the single node specified. Other methods are
    * provided to alter the current multiple-node selection by adding or removing nodes from it.</p>
    * 
    * @param n The node that should be selected.
    * @return False if the argument is null or is not a node within this graphic model.
    */
   public boolean setSelectedNode(FGraphicNode n)
   {
      if(n == null || n.getGraphicModel() != this) return(false);
      if(currSelection.size() > 1 || currSelection.get(0) != n)
      {
         currSelection.clear();
         currSelection.add(n);
         fireFocusChanged();   // fires Change.SELECTION as well!
      }
      return(true);
   }

   /**
    * Make the specified graphics node the focus node for the model's current selection. The focus node is always the
    * last node in the selection list, so this method simply appends the node to the end of the selection list (removing
    * it from its old location if it was already selected.
    * @param n The node that should be selected and made the focus node for the model's current selection.
    * @return False if the argument is null or is not a node within this graphic model.
    */
   public boolean setFocusForSelection(FGraphicNode n)
   {
      if(n == null || n.getGraphicModel() != this) return(false);
      if(currSelection.get(currSelection.size()-1) == n) return(true);
      
      currSelection.remove(n);
      currSelection.add(n);
      fireFocusChanged(); // fires Change.SELECTION as well!
      return(true);
   }
   
   /**
    * Get the graphics node that is currently the focus node for the model's current selection. When only a single node
    * is selected, the selected node and focus node are one and the same. Otherwise, the focus node is the last node
    * in the selection list. 
    * @return The last graphics node in the model's current selection list.
    */
   public FGraphicNode getFocusForSelection() { return(currSelection.get(currSelection.size()-1)); }
   
   /**
    * Revise this model's current list of selected nodes by adding and removing the specified nodes. If any change in 
    * the selection list is actually made, model listeners are notified accordingly. Note that a node must always be
    * selected in the figure, so this method will not remove the last node from the selection list.
    * @param addNode A node to be selected.
    * @param rmvNode A node to be de-selected.
    */
   public void reviseSelection(FGraphicNode addNode, FGraphicNode rmvNode)
   {
      boolean changed = false;
      FGraphicNode last = currSelection.get(currSelection.size()-1);
      if(addNode != null && addNode.getGraphicModel() == this && !currSelection.contains(addNode))
      {
         changed = true;
         currSelection.add(addNode);
      }
      if(currSelection.size() > 1 && rmvNode != null && rmvNode.getGraphicModel() == this && 
            currSelection.remove(rmvNode))
      {
         changed = true;
      }
      
      if(changed) 
      {
         if(last != currSelection.get(currSelection.size()-1)) fireFocusChanged();  // for Focusable infrastructure
         else fireModelChanged(null, Change.SELECTION);
      }
   }
   
   /**
    * Revise this model's current list of selected nodes by adding and removing the specified nodes. If any change in 
    * the selection list is actually made, model listeners are notified accordingly. Note that a node must always be
    * selected in the figure, so this method will not remove all the nodes in the selection list.
    * @param addNodes A list of nodes to be selected.
    * @param rmvNodes A node of nodes to be de-selected.
    */
   public void reviseSelection(List<FGraphicNode> addNodes, List<FGraphicNode> rmvNodes)
   {
      boolean changed = false;
      FGraphicNode last = currSelection.get(currSelection.size()-1);
      int oldSz = 1;
      
      if(rmvNodes != null) for(FGraphicNode fgn : rmvNodes)
      {
         if(fgn != null && fgn.getGraphicModel() == this && currSelection.remove(fgn)) changed = true;
      }
      if(addNodes != null) for(FGraphicNode fgn : addNodes)
      {
         if(fgn != null && fgn.getGraphicModel() == this && !currSelection.contains(fgn))
         {
            changed = true;
            currSelection.add(fgn);
         }
      }
     
      // the selection list can never be empty. If it is, we put back the node that was last in the list. If that was
      // the only node selected, then the current selection is unchanged.
      if(currSelection.isEmpty())
      {
         currSelection.add(last);
         changed = false;
      }
      
      if(changed) 
      {
         if(last != currSelection.get(currSelection.size()-1)) fireFocusChanged();  // for Focusable infrastructure
         else fireModelChanged(null, Change.SELECTION);
      }
   }
   
   /**
    * If a single node is currently selected, then update the model's selection list to include all other nodes of the
    * same type as that node. However, if the currently selected node is an X-, Y- or Z-axis, then the method does NOT
    * select all axes; rather, it selects all X, all Y, or all Z axes. If any change in the selection list is actually 
    * made, model listeners are notified accordingly. No action is taken if multiple nodes are already selected.
    */
   public void selectAllNodesLikeCurrentSelection()
   {
      // no action taken if multiple nodes are selected, or if the singly selected node is the root figure
      if(isMultiNodeSelection() || (getSelectedNode().getNodeType() == FGNodeType.FIGURE)) return;
      
      // accumulate list of nodes in figure that have the same type as current singly-selected node
      List<FGraphicNode> addNodes = new ArrayList<>();
      FGraphicNode currSel = getSelectedNode();
      FGNodeType selType = currSel.getNodeType();
      
      Stack<FGraphicNode> nodeStack = new Stack<>();
      nodeStack.push(root);
      while(!nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();
         if(n != currSel && n.getNodeType() == selType)
         {
            boolean addSel = true;
            if(selType == FGNodeType.AXIS) 
               addSel = ((AxisNode)n).isPrimary() == ((AxisNode)currSel).isPrimary();
            else if(selType == FGNodeType.AXIS3D)
               addSel = ((Axis3DNode)n).getAxis() == ((Axis3DNode)currSel).getAxis();
            
            if(addSel) addNodes.add(n);
         }
         
         if(currSel.isComponentNode())
         {
            for(int i=0; i<n.getComponentNodeCount(); i++) nodeStack.push(n.getComponentNodeAt(i));
         }
         for(int i=0; i<n.getChildCount(); i++) nodeStack.push(n.getChildAt(i));
      }

      // if any "like" nodes found, add them to the current selection!
      if(!addNodes.isEmpty()) reviseSelection(addNodes, null);
   }
   
   /**
    * Can any of the currently selected nodes in this figure be repositioned interactively?
    * @return True if at least one node in the current selection can be repositioned.
    */
   public boolean canMoveCurrentSelection()
   {
      for(FGraphicNode n : currSelection) if(n.canMove()) return(true);
      return(false);
   }
   
   /**
    * Move all nodes in the current selection by the specified horizontal and vertical offsets. If there is a single
    * selected node, this method is the equivalent of {@link #moveFocusable(double, double)}. If multiple movable nodes
    * are selected, then all such nodes are repositioned by the same amount <b>as an atomic, reversible operation</b>.
    * If any change is made, the model is re-rendered accordingly.
    * 
    * <p><b>If the current selection includes any movable node that is a descendant of another movable node, that 
    * descendant will not itself be repositioned, since it is affected by the repositioning of its ancestor.</b></p>
    * 
    * @param dx Net change in horizontal position, in milli-in WRT model's "global" coordinate system.
    * @param dy Net change in vertical position, in milli-in WRT model's "global" coordinate system.
    */
   public void moveCurrentSelection(double dx, double dy)
   {
      if(dx == 0 && dy == 0) return;
      
      // check all selected nodes to see how many can move. Remove descendants of movable nodes, since they'll be moved
      // with their movable ancestor (else they would move twice as far!)
      ArrayList<FGraphicNode> movable = new ArrayList<>();
      int nMove = 0;
      for(FGraphicNode n : currSelection) if(n.canMove())
      {
         ++nMove;
         
         // discard movable descendants of this movable node
         int i = 0;
         while(i < movable.size())
         {
            if(movable.get(i).isDescendantOf(n)) movable.remove(i);
            else ++i;
         }
         
         // skip this movable node if it is a descendant of a node already in the list.
         boolean skip = false;
         for(FGraphicNode n2 : movable) if(n.isDescendantOf(n2))
         {
            skip = true; 
            break;
         }
         
         if(!skip) movable.add(n);
      }
      
      // zero or one movable node in the current selection
      if(movable.size() <= 1) 
      {
         if(movable.size() == 1) movable.get(0).executeMove(dx, dy, true);
         return;
      }
      
      // moving at least two nodes simultaneously as a single operation. Need to block edit history and rendering
      // updates until all nodes have been moved. Then post multi-move reverisble edit op to history.
      MultiRevEdit undoer = new MultiRevEdit(this, "Reposition " + nMove + " selected elements");
      blockEditHistory();
      try
      {
         for(FGraphicNode n : movable)
         {
            // get values of X,Y before the move; for line node, also get X,Y for the second endpoint
            Measure xOld = n.getX();
            Measure yOld = n.getY();
            Measure x2Old = null;
            Measure y2Old = null;
            if(n.getNodeType() == FGNodeType.LINE)
            {
               x2Old = ((LineNode) n).getX2();
               y2Old = ((LineNode) n).getY2();
            }
            
            // execute movement. If successful, get updated values for X,Y (and X2, Y2), then record each altered
            // property value in the multi-step reversible edit object.
            if(n.executeMove(dx, dy, false))
            {
               Measure x = n.getX();
               Measure y = n.getY();
               Measure x2 = null;
               Measure y2 = null;
               if(n.getNodeType() == FGNodeType.LINE)
               {
                  x2 = ((LineNode) n).getX2();
                  y2 = ((LineNode) n).getY2();
               }
               
               if(!Measure.equal(xOld, x)) undoer.addPropertyChange(n, FGNProperty.X, x, xOld);
               if(!Measure.equal(yOld, y)) undoer.addPropertyChange(n, FGNProperty.Y, y, yOld);
               if(x2 != null && !Measure.equal(x2Old, x2)) undoer.addPropertyChange(n, FGNProperty.X2, x2, x2Old);
               if(y2 != null && !Measure.equal(y2Old, y2)) undoer.addPropertyChange(n, FGNProperty.Y2, y2, y2Old);
               
               // notify model listeners that the node's definition has changed w/o triggering a re-render.
               onChange(n, 0, false, null);
            }
         }
         if(undoer.getNumberOfChanges() > 0) root.onNodeModified(null);
      }
      finally { unblockEditHistory(); }
      
      if(undoer.getNumberOfChanges() > 0) postReversibleEdit(undoer);
   }
   
   /** If this flag is set, then model has been modified since the flag was last reset. */
   private boolean isModified = false;

   /** 
    * A rough estimate of the number of atomic changes in the figure since it was last saved. NOTE that this is NOT an
    * exact count, nor is it decremented upon undoing the last change. The infrastructure for reporting model
    * notifications does not support such accuracy. Rather, this can be used to detect that some change has occurred in
    * the model since the last time the modification count was checked!
    */
   private long modifyCount = 0L;
   
   /**
    * Has this graphic model been modified since the last time the modified flag was cleared?
    * @return True if model has been modified in any way since the modified flag was last cleared.
    */
   public boolean isModified() { return(isModified); }
   
   /**
    * Set or clear this model's modified flag. It will typically be reset when the model is saved  to file. The model's 
    * internal modification count is reset to zero when the flag is cleared. Any registered model listeners are notified
    * when the flag is reset, but not when it is set (since any  change in the model will set the flag).
    * @param b Desired state for modified flag.
    */
   public void setModified(boolean b) 
   { 
      if(b != isModified)
      {
         isModified = b; 
         if(!isModified)
         {
            fireModelChanged(null, Change.MODFLAG_RESET);
            modifyCount = 0L;
         }
         else ++modifyCount;
      }
   }

   /**
    * Get an internal count of the number of changes in this figure graphic model since the last time it was saved --
    * or, more accurately, the last time the count was reset by a call to {@link #setModified}. This count is merely
    * a rough estimate and is only intended to detect that the model has changed state over time.
    * @return The model's current modification count.
    */
   public long getModifyCount() { return(modifyCount); }
   

   // 
   // Publicly-accessible support for node insertions and cut/copy/paste/delete support.
   //

   /**
    * Can a graphic node of the specified type be inserted as an optional child of the specified parent?
    * 
    * @param parent The parent graphic node.
    * @param childType The type of child node to insert.
    * @return <code>True</code> iff the parent node is non-<code>null</code>, is contained in this model, and admits 
    * children of the specified type.
    */
   public boolean canInsertNode(FGraphicNode parent, FGNodeType childType)
   {
      return(parent != null && parent.getGraphicModel() == this && parent.canInsert(childType));
   }

   /**
    * Insert a graphics node into this <code>FGraphicModel</code> as a child of another node in the model. If 
    * successful, the model is updated accordingly and re-rendered as needed. After insertion, the node is selected for 
    * editing and display purposes.
    * 
    * @param parent The proposed parent node.
    * @param child The proposed child node.
    * @param pos The insertion position. If not a valid index in the parent's child list, the child is appended to the 
    * end of the list.
    * @return <code>True</code> iff the insertion was successful.
    */
   public boolean insertNode(FGraphicNode parent, FGraphicNode child, int pos)
   {
      boolean ok = false;
      if(parent != null && parent.getGraphicModel() == this)
      {
         ok = parent.insert(child, pos);
         if(ok) 
         {
            setSelectedNode(child);
            postReversibleEdit(new FGMRevOp(child));
         }
      }
      return(ok);
   }

   /**
    * Create and insert a graphics node of the specified type into this <i>FypML</i> graphic as a child of an existing
    * node in the model. If successful, the model is updated accordingly and re-rendered as needed. The display focus is
    * also shifted to the node just inserted.
    * 
    * @param parent The proposed parent node.
    * @param childType The node type of the proposed child node. Must be one of the "publicly" created node types listed
    * in {@link #CREATENODETYPES}.
    * @param pos The insertion position. If not a valid index in the parent's child list, the child is appended to the 
    * end of the list.
    * @return True iff the insertion was successful.
    */
   public boolean insertNode(FGraphicNode parent, FGNodeType childType, int pos)
   {
      FGraphicNode fgn = createGraphicNode(childType);
      
      // give data presentation nodes an opportunity to initialize default data so they show up in their graph container
      if(fgn instanceof FGNPlottable)
      {
         double[] axisRng = new double[6];
         boolean isPolar = false;
         boolean ok = false;
         if(parent instanceof GraphNode)
         {
            GraphNode g = (GraphNode) parent;
            axisRng[0] = g.getPrimaryAxis().getValidatedStart();
            axisRng[1] = g.getPrimaryAxis().getValidatedEnd();
            axisRng[2] = g.getSecondaryAxis().getValidatedStart();
            axisRng[3] = g.getSecondaryAxis().getValidatedEnd();
            axisRng[4] = g.getColorBar().getValidatedStart();
            axisRng[5] = g.getColorBar().getValidatedEnd();
            isPolar = g.isPolar();
            ok = true;
         }
         else if(parent instanceof PolarPlotNode)
         {
            PolarPlotNode pg = (PolarPlotNode) parent;
            axisRng[0] = pg.getThetaAxis().getRangeMin();
            axisRng[1] = pg.getThetaAxis().getRangeMax();
            axisRng[2] = pg.getRadialAxis().getRangeMin();
            axisRng[3] = pg.getRadialAxis().getRangeMax();
            axisRng[4] = pg.getColorBar().getValidatedStart();
            axisRng[5] = pg.getColorBar().getValidatedEnd();
            isPolar = true;
            ok = true;
         }
         else if(parent instanceof Graph3DNode)
         {
            Graph3DNode g3 = (Graph3DNode) parent;
            axisRng[0] = g3.getAxis(Graph3DNode.Axis.X).getRangeMin();
            axisRng[1] = g3.getAxis(Graph3DNode.Axis.X).getRangeMax();
            axisRng[2] = g3.getAxis(Graph3DNode.Axis.Y).getRangeMin();
            axisRng[3] = g3.getAxis(Graph3DNode.Axis.Y).getRangeMax();
            axisRng[4] = g3.getAxis(Graph3DNode.Axis.Z).getRangeMin();
            axisRng[5] = g3.getAxis(Graph3DNode.Axis.Z).getRangeMax();
            ok = true;
         }
         
         if(ok) ((FGNPlottable) fgn).initializeDefaultData(axisRng, isPolar);
      }
      
      return(insertNode(parent, fgn, pos));
   }

   /** 
    * The array of <i>FypML</i> elements that may be created and inserted into a graphic model via the public API. All 
    * other graphic node types represent private component nodes that cannot be created "publicly".
    */
   public final static FGNodeType[] CREATENODETYPES = new FGNodeType[] { 
      FGNodeType.GRAPH, FGNodeType.PGRAPH, FGNodeType.GRAPH3D, FGNodeType.LINE, FGNodeType.SHAPE, FGNodeType.LABEL, 
      FGNodeType.TEXTBOX, FGNodeType.IMAGE, FGNodeType.TRACE, FGNodeType.FUNCTION, FGNodeType.RASTER, 
      FGNodeType.CONTOUR, FGNodeType.BAR, FGNodeType.SCATTER, FGNodeType.AREA, FGNodeType.PIE, FGNodeType.CALIB, 
      FGNodeType.TICKS, FGNodeType.TICKS3D, FGNodeType.SCATTER3D, FGNodeType.SURFACE, FGNodeType.BOX
   };

   /**
    * Helper method creates a <i>FypML</em> graphic node of the specified type, which must be one of the "public" node 
    * types listed in {@link #CREATENODETYPES}
    * 
    * @param type The type of graphic node to be created.
    * @return The new graphic node, initially parentless. If unsuccessful, null is returned.
    */
   private FGraphicNode createGraphicNode(FGNodeType type)
   {
      FGraphicNode n = null;
      switch(type)
      {
         case GRAPH : n = new GraphNode(); break;
         case PGRAPH : n = new PolarPlotNode(); break;
         case GRAPH3D : n = new Graph3DNode(); break;
         case LINE : n = new LineNode(); break;
         case SHAPE : n = new ShapeNode(); break;
         case LABEL : n = new LabelNode(); break;
         case TEXTBOX : n = new TextBoxNode(); break;
         case IMAGE : n = new ImageNode(); break;
         case TRACE : n = new TraceNode(); break;
         case RASTER : n = new RasterNode(); break;
         case CONTOUR : n = new ContourNode(); break;
         case BAR : n = new BarPlotNode(); break;
         case BOX : n = new BoxPlotNode(); break;
         case SCATTER : n = new ScatterPlotNode(); break;
         case SCATTER3D : n = new Scatter3DNode(); break;
         case SURFACE : n = new SurfaceNode(); break;
         case AREA : n = new AreaChartNode(); break;
         case PIE : n = new PieChartNode(); break;
         case FUNCTION : n = new FunctionNode(); break;
         case CALIB : n = new CalibrationBarNode(); break;
         case TICKS : n = new TickSetNode(); break;
         case TICKS3D : n = new Ticks3DNode(); break;
         default: break;
      }
      return(n);
   }

   /**
    * Change the position of a child graphic node in its parent's list of children, thereby changing its rendering 
    * order ("Z-order"). If successful, the model is updated accordingly and re-rendered as needed.
    * @param parent The parent node.
    * @param child The child node.
    * @param pos The desired position for the child node. Must be in [0..N-1], where N is the total number of child 
    * nodes. If this position matches the current position of the node, the method has no effect.
    * @return <code>True</code> if successful, <code>false</code> if any argument is invalid.
    */
   public boolean changeChildPosition(FGraphicNode parent, FGraphicNode child, int pos)
   {
      if(parent == null || child == null) return(false);
      int oldPos = parent.getIndexOf(child);
      if(oldPos == pos) return(true);
      boolean ok = ((oldPos >= 0) && (parent.getGraphicModel() == this) && parent.setChildPosition(child, pos));
      if(ok) postReversibleEdit(new FGMRevOp(child, oldPos));
      return(ok);
   }

   /**
    * Can the specified graphic node be removed from this <code>FGraphicModel</code>?
    * 
    * @param n The node to test.
    * @return <code>True</code> iff the node is non-<code>null</code>, is contained in this model, is NOT the root node, 
    * and is NOT an instrinsic <em>component</em> node. Insertion/removal of component nodes, as opposed to regular 
    * child nodes, is under the strict control of the parent node.
    */
   public boolean canDeleteNode(FGraphicNode n)
   {
      return(n != null && n != root && n.getGraphicModel() == this && !n.isComponentNode()); 
   }

   /**
    * Can the specified graphic node be copied to a clipboard shared by all instances of <code>FGraphicModel</code>?
    * 
    * @param n The node to test.
    * @return True if the node can be copied -- same conditions as for {@link #canDeleteNode(FGraphicNode)}.
    */
   public boolean canCopyNode(FGraphicNode n)
   {
      return(canDeleteNode(n)); 
   }
   
   /**
    * Permanently remove the specified graphic node from this figure. If successful, the figure model is updated and 
    * re-rendered accordingly, and its edit history is updated so that the operation can be undone.
    * 
    * <p>The node is NOT copied to the model's private clipboard, so it cannot be recovered unless the operation is 
    * undone. If it was selected, it is removed from the model's selection list; if it was the only node selected, that 
    * selection shifts to a sibling (or the parent node) just prior to removal.</p>
    * 
    * @param n The node to be removed.
    * @return True if the node was successfully removed.
    */
   public boolean deleteNode(FGraphicNode n)
   {
      if(!canDeleteNode(n)) return(false);

      // if node to be removed is the current focus node, shift focus to a sibling or parent prior to removal.
      FGraphicNode parent = n.getParent();
      int originalPos = parent.getIndexOf(n);
      if(currSelection.contains(n))
      {
         boolean last = currSelection.get(currSelection.size()-1) == n;
         currSelection.remove(n);
         if(currSelection.isEmpty())
         {
            int pos = originalPos;
            if(parent.getChildCount() == 1)
               currSelection.add(parent);
            else
            {
               pos = (pos == 0) ? 1 : pos - 1;
               currSelection.add(parent.getChildAt(pos));
            }
         }
         if(last) fireFocusChanged();
         else fireModelChanged(null, Change.SELECTION);
      }

      parent.remove(n);
      
      // make it possible to undo this reversible change
      postReversibleEdit(new FGMRevOp(n, parent, originalPos));
      
      return(true);
   }

   /**
    * Can any of the currently selected nodes be removed from this figure? 
    * 
    * @return True if the figure's current selection contains at least one node that may be removed. Intrinsic 
    * <i>component</i> nodes may not be removed via figure model methods: insertion or removal of such nodes is under
    * the strict control of the parent graphic node.
    */
   public boolean canDeleteCurrentSelection()
   {
      for(FGraphicNode n : currSelection) if(canDeleteNode(n)) return(true);
      
      return(false);
   }
   
   /**
    * Can any of the currently selected nodes be copied from this figure into the shared node clipboard? [This simply 
    * calls {@link #canDeleteCurrentSelection()} because the restrictions are the same for deleting or copying.]
    * 
    * @return True if the figure's current selection contains at least one node that may be copied. 
    */
   public boolean canCopyCurrentSelection() { return(canDeleteCurrentSelection()); }
   
   /**
    * Delete/cut all nodes in the current selection that can be removed <b>as a single, reversible operation</b>. If any
    * nodes are successfully removed, the figure is updated and re-rendered accordingly, its current selection is 
    * updated to ensure at least one node remains selected, and the model's edit history is updated so the operation can
    * be undone.
    * 
    * <p><b>The current selection will be "normalized" to ensure that no node is a descendant of another node in the
    * selection. Such descendants will be removed anyway when their ancestor is deleted.</b></p>
    * 
    * @param cut If set, the deleted nodes are copied to the shared node clipboard -- that is, the current selection is
    * "cut" rather than simply deleted. If unset, the nodes cannot be recovered unless the deletion is undone.
    */
   public void deleteCurrentSelection(boolean cut)
   {
      // check all selected nodes to see how many can be deleted. Remove descendants of removable nodes, since they'll 
      // be deleted anyway.
      ArrayList<FGraphicNode> removable = new ArrayList<>();
      for(FGraphicNode n : currSelection) if(canDeleteNode(n))
      {
         // discard removable descendants of this removable node
         int i = 0;
         while(i < removable.size())
         {
            if(removable.get(i).isDescendantOf(n)) removable.remove(i);
            else ++i;
         }
         
         // skip this removable node if it is a descendant of a node already in the delete list.
         boolean skip = false;
         for(FGraphicNode n2 : removable) if(n.isDescendantOf(n2))
         {
            skip = true; 
            break;
         }
         
         if(!skip) removable.add(n);
      }
      
      // zero or one removable node in the current selection
      if(removable.size() <= 1) 
      {
         if(removable.size() == 1) 
         {
            if(cut) FGraphicModel.copyToClipboard(removable.get(0), false);
            deleteNode(removable.get(0));
         }
         return;
      }
      
      // deleting/cutting at least two nodes simultaneously as a single operation. Need to block edit history until all 
      // nodes have been removed. Then post multi-delete reverisble edit op to history.
      if(cut) FGraphicModel.copyToClipboard(removable);
      FGMRevOp undoer = new FGMRevOp(false);
      blockEditHistory();
      try
      {
         for(FGraphicNode n : removable)
         {
            FGraphicNode parent = n.getParent();
            int pos = parent.getIndexOf(n);
            
            // each node deleted is part of the current selection. Here we remove it from the current selection before
            // deleting. If the selection is empty, ensure it contains at least one node.
            currSelection.remove(n);
            if(currSelection.isEmpty())
            {
               if(parent.getChildCount() == 1)
                  currSelection.add(parent);
               else
               {
                  int selPos = (pos == 0) ? 1 : pos - 1;
                  currSelection.add(parent.getChildAt(selPos));
               }
            }

            parent.remove(pos);  // this will trigger a re-render and the necessary model change event
            
            undoer.addDeletedNode(n, parent, pos);
         }
      }
      finally { unblockEditHistory(); }
      
      postReversibleEdit(undoer);
   }

   /** 
    * Can the specified styling be applied to at least one node in the figure's current selection list such that the
    * node's current state is altered?
    * 
    * @param ss The set of styling properties to be tested.
    * @return True if applying the specified style set would affect at least one selected node in the figure.
    */
   public boolean canApplyStylingToCurrentSelection(FGNStyleSet ss)
   {
      if(ss == null) return(false);
      for(FGraphicNode n : currSelection)
      {
         FGNStyleSet currStyling = n.getCurrentStyleSet();
         if(currStyling != null && !FGNStyleSet.matching(currStyling, ss)) return(true);
      }
      return(false);
   }
   
   /**
    * Apply the specified styling to each node in the figure's current selection list <b>as a single, reversible
    * operation</b>. 
    * @param ss The style set to be applied.
    */
   public void applyStylngToCurrentSelection(FGNStyleSet ss)
   {
      if(ss == null) return;
      
      // simple case: single node selected
      if(!isMultiNodeSelection())
      {
         FGraphicNode sel = getSelectedNode();
         if(sel != null) sel.applyStyleSet(ss);
         return;
      }
      
      // handle styling of a multi-node selection
      FGraphicNode.StyleSetRevEdit undoer = new FGraphicNode.StyleSetRevEdit(ss);
      blockEditHistory();
      boolean changed = false;
      try
      {
         for(FGraphicNode n : currSelection)
         {
            if(n.applyStyleSet(undoer)) changed = true;
         }
         
         if(changed) root.onNodeModified(null);
      }
      finally { unblockEditHistory(); }
      
      if(changed) postReversibleEdit(undoer);
   }
   
   /**
    * Can all of the graphic nodes currently selected in this figure be aligned along a common edge or their horizontal 
    * or vertical centers? By definition, only certain node types can participate in an alignment operation, and no node
    * that has been rotated supports alignment.
    * 
    * @return True if the figure's current selection includes two or more nodes, all of which can be aligned. 
    */
   public boolean canAlignCurrentSelection()
   {
      if(currSelection.size() < 2) return(false);
      for(FGraphicNode n : currSelection) if(!n.canAlign()) return(false);
      return(true);
   }
   
   /**
    * Realign all nodes in the figure's current selection along a common edge or center line. If only one node is 
    * currently selected, or if any node in the current selection cannot be aligned, no action is taken. There are six
    * possible ways to align the selected nodes:
    * <ul>
    * <li>{@link FGNAlign#LEFT} : All nodes are aligned along left edge of the focus node in the selection.</li>
    * <li>{@link FGNAlign#RIGHT} : All nodes are aligned along right edge of the focus node in the selection.</li>
    * <li>{@link FGNAlign#BOTTOM} : All nodes are aligned along bottom edge of focus node in the selection.</li>
    * <li>{@link FGNAlign#TOP} : All nodes are aligned along top edge of the focus node in the selection.</li>
    * <li>{@link FGNAlign#HCENTER} : All nodes are aligned at the X-coordinate of the center point of the focus node in 
    * the selection.</li>
    * <li>{@link FGNAlign#VCENTER} : All nodes are aligned at the Y-coordinate of the center point of the focus node in
    * the selection.</li>
    * </ul>
    * By definition, the "focus node" for the selection is always the <i>last</i> node in the selection list.
    * 
    * <p>All of the property changes necessary to align the nodes are executed as a <i>single, reversible operation</i>.
    * The figure model's edit history is blocked as the changes are made, then it is unblocked and a single reversible
    * edit that can undo all of the changes is posted to the history. The figure is also re-rendered to reflect all of
    * the changes made.</p>
    * 
    * @param align The alignment type -- indicates the edge or center line along which the selected nodes are aligned,
    * as described above.
    */
   public void alignCurrentSelection(FGNAlign align)
   {
      if(align == null || !canAlignCurrentSelection()) return;
      
      // the nodes in the selection list are aligned WRT the focus node for the selection. If we cannot compute the
      // alignment locus for that node, abort.
      Double locusD = getFocusForSelection().getAlignmentLocus(align);
      if(locusD == null || !Utilities.isWellDefined(locusD)) return;
      
      double locus = locusD;
      
      // align the objects. For each node actually changed, notify model listeners that the node's definition has
      // changed WITHOUT triggering a re-render.
      MultiRevEdit undoer = new MultiRevEdit(this, "Align nodes - " + align);
      blockEditHistory();
      try
      {
         // remember, the last node in the selection is the anchor for the alignment operation. It does not move.
         for(int i=0; i<currSelection.size()-1; i++)
         {
            FGraphicNode n = currSelection.get(i);
            if(n.align(align, locus, undoer))
               onChange(n, 0, false, null);
         }
      }
      finally { unblockEditHistory(); }
      
      // after all the changes have been made, re-render the entire figure and post the reversible edit to undo the op.
      if(undoer.getNumberOfChanges() > 0)
      {
         root.onNodeModified(null);
         postReversibleEdit(undoer);
      }
   }
   
   /** 
    * The graphic node(s) currently in the "clipboard" shared by all instances of <code>FGraphicModel</code>. It is 
    * list to support copying, cutting and pasting a multiple-node selection. Initially empty.
    */
   private static final List<FGraphicNode> nodeClipboard = new ArrayList<>();

   /**
    * Get the number of graphic nodes in the "node clipboard", a static resource shared by all instances of
    * <code>FGraphicModel</code>. It contains the last node or nodes (in the case of a multiple-node selection) copied
    * from a figure.
    * @return Number of nodes currently in the clipboard.
    */
   public static int getClipboardSize() { return(nodeClipboard.size()); }
   
   /**
    * Return the type of graphic node that currently resides in the shared clipboard.
    * @return If the clipboard contains a SINGLE node, return that node's type. Else return null.
    */
   public static FGNodeType getClipboardContentType()
   {
      return((nodeClipboard.size() == 1) ? nodeClipboard.get(0).getNodeType() : null);
   }

   /**
    * Copy the specified graphic node from its containing figure model to the shared node clipboard, optionally deleting 
    * it from the parent model in the same operation.
    * 
    * @param n The node to be copied.
    * @param cut If true, the node is excised from its owner model. The model will be updated and re-rendered as needed.
    * @return True if operation succeeded. If the node is not part of a figure, or if it cannot be copied (or cut), the 
    * method will fail.
    */
   public static boolean copyToClipboard(FGraphicNode n, boolean cut)
   {
      FGraphicModel model = n.getGraphicModel();
      if(model == null || !model.canCopyNode(n))
         return(false);

      // if cutting, delete node from its owner model
      if(cut && !model.deleteNode(n)) return(false);
      
      // always clone the node, even if it's deleted. If the deleted node was restored and then edited, and then the
      // user pasted the node from the clipboard, the node will be different from what it was when it was cut.
      try
      {
         nodeClipboard.clear();
         nodeClipboard.add((FGraphicNode) n.clone());
         return(true);
      }
      catch(CloneNotSupportedException e) { return(false); }
   }

   /**
    * Copy a list of graphic nodes from their containing figure model to the shared node clipboard.
    * 
    * @param nodes List of nodes to be copied.
    * @return True if at least one of the nodes in the list was copied to the clipboard.
    */
   public static boolean copyToClipboard(List<FGraphicNode> nodes)
   {
      if(nodes == null || nodes.isEmpty()) return(false);

      try
      {
         List<FGraphicNode> copied = new ArrayList<>();
         for(FGraphicNode n : nodes)
         {
            FGraphicModel model = n.getGraphicModel();
            if(model != null && model.canCopyNode(n)) copied.add((FGraphicNode) n.clone());
         }

         if(copied.isEmpty()) return(false);
         nodeClipboard.clear();
         nodeClipboard.addAll(copied);
         return(true);
      }
      catch(CloneNotSupportedException e) { return(false); }
   }
   
   /**
    * Can any nodes currently in the shared node clipboard be copied as children of the specified node?
    * @param parent The target parent node into which the clipboard node(s) would be inserted.
    * @return True if at least one clipboard node can be inserted into target parent.
    */
   public static boolean canPasteFromClipboard(FGraphicNode parent)
   {
      if(parent == null) return(false);
      for(FGraphicNode n : nodeClipboard) if(parent.canInsert(n.getNodeType())) return(true);
      return(false);
   }
   
   /**
    * Paste a copy of each graphic node currently in the shared clipboard as a child of the specified node. Any 
    * clipboard node that cannot be inserted as a child of the specified parent is simply skipped. If at least one node
    * was inserted, the owner model is updated accordingly and re-rendered as needed.
    * successful, the owner model is updated accordingly and re-rendered as needed.
    * 
    * @param parent The proposed parent node.
    * @param pos The insertion position. If not a valid index in the parent's child list, the child is appended to the 
    * end of the list.
    * @return True if at least one clipboard node was copied and inserted a child of the specified parent.
    */
   public static boolean pasteFromClipboard(FGraphicNode parent, int pos)
   {
      if(!canPasteFromClipboard(parent)) return(false);
      FGraphicModel model = parent.getGraphicModel();
      if(model == null) return(false);
      
      // clone each clipboard node that can be pasted into parent
      List<FGraphicNode> copies = new ArrayList<>();
      try
      {
         for(FGraphicNode n : nodeClipboard) if(parent.canInsert(n.getNodeType()))
         {
            copies.add((FGraphicNode) n.clone());
         }
      }
      catch(CloneNotSupportedException e) { return(false); }

      // now insert each copied node into the parent
      if(copies.isEmpty()) return(false);
      else if(copies.size() == 1) 
         return(model.insertNode(parent, copies.get(0), pos));
      else
      {
         FGMRevOp undoer = model.new FGMRevOp(true);
         model.blockEditHistory();
         try
         {
            for(FGraphicNode n : copies)
            {
               parent.insert(n, pos);           // triggers a re-render and the appropriate model change event
               pos = parent.getIndexOf(n) + 1;              
               undoer.addInsertedNode(n);
            }
            
            // after insertion, select all the nodes inserted
            model.reviseSelection(copies, model.getSelectedNodes(null));
         }
         finally { model.unblockEditHistory(); }
         
         model.postReversibleEdit(undoer);
      }
      
      return(true);
   }

   
   // 
   // Model listeners and change notifications
   //

   /**
    * An enumeration of the kinds of atomic model mutations for which registered {@link FGModelListener}s receive 
    * notification.
    */
   public enum Change 
   {
      /** Only issued when root node is set into a new model. The root node will be selected. */ 
      RELOAD,

      /** 
       * Issued whenever a node's subordinate list changes: graphic nodes are inserted or removed, or the order of the 
       * subordinates in the list changes. The model's selection list typically change in some manner after such an 
       * operation.
       */
      INSERT_OR_REMOVE,

      /** Issued whenever the definition of a graphic node is changed. */
      DEFINE,
      
      /** Issued whenever the only change in the model is a change in the list of currently selected nodes. */
      SELECTION,
      
      /** 
       * Issued whenever there's a change in the model's edit history. This is intended only for updating a UI element 
       * that reflects whether or not an "undo" or "redo" operation is currently possible.
       */
      EDIT_HISTORY,
      
      /** Issued only when model's "modified" flag is reset -- typically, when model is saved to file. */
      MODFLAG_RESET
   }

   /**
    * Each graphic node in the <code>FGraphicModel</code> must invoke this method whenever its definition changes in 
    * any way, <strong>including</strong> the addition or removal of a child node. The method will set the model's 
    * "dirty" flag, notify any registered listeners, and queue a rendering task if one is requested.
    * 
    * @param n The graphics node that was affected. Cannot be <code>null</code>. For a child list changes this should 
    * be the parent node affected. Else, it should be the node whose definition was changed.
    * @param change This must be 0 if the node's definition was changed, 1 if a subordinate node was inserted or 
    * removed or if the child list was reordered in some way, 2 if the root node was just replaced, and -1 if the node 
    * must be re-rendered as a side-effect induced by a change in another node.
    * @param needsRender If <code>true</code> and a <code>RenderableModelViewer</code> is currently registered with this 
    * model, a rendering task is queued.
    * @param dirtyRects List of dirty areas in the current graphic model that need to be re-rendered as a result of the 
    * change. Must be expressed in the "global" rendering coordinates of the <code>RootRenderable</code> node. May be 
    * <code>null</code> or an empty list, in which case a full re-rendering of the graphic model occurs. Ignored if the 
    * <code>needsRender</code> argument is <code>false</code>.
    */
   void onChange(FGraphicNode n, int change, boolean needsRender, List<Rectangle2D> dirtyRects)
   {
      assert(n != null);
      if(n.getGraphicModel() != this) return;
      if(change >= 0) 
      {
         isModified = true;
         ++modifyCount;
         if(change > 1)
            fireModelChanged(root, Change.RELOAD);
         else
            fireModelChanged(n, (change == 0) ? Change.DEFINE : Change.INSERT_OR_REMOVE);
      }
      if(needsRender)
         fireModelMutated(dirtyRects);
   }

   /**
    * The list of objects registered to listen for changes in this <code>FGraphicModel</code>.
    */
   private final List<FGModelListener> listeners = new ArrayList<>();

   /**
    * Adds a listener to this <code>FGraphicModel</code>'s listener list. If a model mutation event is currently being 
    * dispatched, the added listener will not receive it.
    * 
    * @param l The listener to be added. If <code>null</code>, or if the listener is already registered, the method 
    * takes no action.
    */
   synchronized public void addListener(FGModelListener l)
   {
      if(l != null)
      {
         for(FGModelListener listener : listeners)
            if(listener == l) return;
         listeners.add(l);
      }
   }

   /**
    * Removes a listener from this <code>FGraphicModel</code>'s listener list. <strong>If a model mutation event is 
    * currently being dispatched, the removed listener <em>may</em> still receive it</strong>.
    * 
    * @param l The listener to be removed. If <code>null</code>, or if the listener is not in the list, the method 
    * takes no action.
    */
   synchronized public void removeListener(FGModelListener l)
   {
      listeners.remove(l);
   }

   /** Inform any registered listeners that this figure graphic model has changed in some way.*/
   private void fireModelChanged(FGraphicNode affected, Change type)
   {
      // clone the listener list so that, if the list mutates during the event dispatch, the dispatch is unaffected
      List<FGModelListener> copyOfListeners;
      synchronized(listeners)
      {
         copyOfListeners = new ArrayList<>(listeners);
      }

      // dispatch the event
      for(FGModelListener l : copyOfListeners)
         l.modelChanged(this, affected, type);
   }

   
   //
   // Multi-selection edit support
   //
   
   /** Flag set whenever a multiple-selection object edit operation is in progress. */
   private boolean multiEditInProgress = false;
   
   /**
    * If the current selection list is homogeneous {@link #isHomogeneousSelection()} and contains multiple nodes, this
    * method will attempt to change the specified property to the specified value on all of the nodes <i>as a single,
    * reversible operation</i>.
    * 
    * <p>Subclasses should invoke this method from their property setters, BEFORE changing the relevant property. It 
    * should not be invoked if the property value is unchanged. If the method returns true, then the multi-object edit 
    * operation was executed on all nodes in the multiple selection, including the node on which the property edit was 
    * explicitly initiated.</p>
    * 
    * @param initialFGN The graphic node on which the property edit was initiated. It is assumed that this node is
    * among the list of currently selected nodes, or is a component or tick set under a selected node.
    * @param prop The property to be changed.
    * @param value The new value for the property. It is assumed that the value type is consistent with the specified
    * property so that it can be safely set using {@link FGraphicNode#setPropertyValue}.
    * @return False if the operation was not performed because the current selection list contains only a single node or
    * is not homogeneous -- or because a multi-object edit is already in progess. Else returns true.
    */
   boolean doMultiNodeEdit(FGraphicNode initialFGN, FGNProperty prop, Object value)
   {
      if(initialFGN == null || prop == null || multiEditInProgress) return(false);
      if(isEditHistoryBlocked()) return(false);
      if(!(isMultiNodeSelection() && isHomogeneousSelection())) return(false);
      
      List<FGraphicNode> nodes = prepareMultiNodeEditList(initialFGN);
      if(nodes.size() <= 1) return(false);
      MultiRevEdit undoer = new MultiRevEdit(this, "Multi-object property edit: " + prop.getNiceName());
      multiEditInProgress = true;
      blockEditHistory();
      try
      {
         for(FGraphicNode n : nodes)
         {
            // NOTE: In a prior version we would disable notifications before changing the property value. But some
            // changes affect the node's internal rendering infrastructure, and failure to call onNodeModified()
            // causes problems. So now we keep notifications on. This will trigger multiple render jobs, but those
            // should be consolidated into one.
            Object oldValue = n.getPropertyValue(prop);
            if(n.setPropertyValue(prop, value))
               undoer.addPropertyChange(n, prop, value, oldValue);
         }
      }
      finally
      {
         multiEditInProgress = false;
         unblockEditHistory();
      }
      
      if(undoer.getNumberOfChanges() > 0)
      {
         root.propagateFontChange();
         root.onNodeModified(null);
         postReversibleEdit(undoer);
      }
      return(true);
   }
   
   /**
    * Helper method for {@link #doMultiNodeEdit}. It prepares the list of nodes to be edited by scanning the current
    * multi-selection and finding nodes that match the node on which the property edit was initiated. This task is
    * complicated by the fact that some nodes cannot be selected in the UI for editing purposes, while others are
    * components of the complex graph node:
    * <ul>
    * <li>The {@link SymbolNode} and {@link ErrorBarNode} components of a {@link TraceNode}.</li>
    * <li>The {@link SymbolNode} component of a {@link FunctionNode}.</li>
    * <li>When multiple 2D graphs are selected, a property edit on a component node will be applied to the corresponding
    * component in all selected graphs. For example, if the node on which the property edit was initiated is a graph's
    * Y axis, then the Y axis of each selected graph will be included in the node list to be edited. An analogous 
    * approach is taken when multiple 3D graphs are selected. <i>Note that you cannot perform a multi-object edit across
    * 2D and 3D graphs, because different node types are involved.</i></li>
    * <li>When the property edit is initiated on a 2D tick set ({@link TickSetNode}) -- call it T --, then the current 
    * selection will either include all axis nodes of the same type (either {@link AxisNode} or {@link ColorBarNode}),
    * or all 2D graph nodes. In the former case, the node list will include the tick set of each selected axis that is
    * at the same position in the child list as T (note that not all axes may have such a tick set!). In the latter 
    * case, only tick sets that are on the same axis in the selected graphs (primary, secondary, or color axis) and at
    * the same position in the axis child list will be included. Again, an analogous approach is taken for 3D tick
    * sets.</li>
    * </ul>
    * 
    * @param initialFGN The node on which edit was initiated.
    * @return List of like nodes to be edited, culled from the current selection list as described above.
    */
   private List<FGraphicNode> prepareMultiNodeEditList(FGraphicNode initialFGN)
   {
      List<FGraphicNode> nodes = new ArrayList<>();
      FGNodeType nt = initialFGN.getNodeType();
      if(currSelection.get(0).getNodeType() == FGNodeType.BOX && nt != FGNodeType.BOX)
      {
         // multiple box plots are selected, but user is editing an intrinsic component of one of the box plots. In this
         // case we also edit the corresponding components in all selected box plots.
         for(FGraphicNode n : currSelection)
         {
            BoxPlotNode box = (BoxPlotNode)n;
            if(nt == FGNodeType.SYMBOL) nodes.add(box.getSymbolNode());
            else if(nt == FGNodeType.EBAR) nodes.add(box.getWhiskerNode());
            else if(nt == FGNodeType.VIOLIN) nodes.add(box.getViolinStyleNode());
         }
      }
      else if(nt == FGNodeType.SYMBOL)
      {
         // current selection must contain all trace nodes or all function nodes, which possess a symbol child.
         for(FGraphicNode n : currSelection) nodes.add(((FGNPlottable) n).getSymbolNode());
      }
      else if(nt == FGNodeType.EBAR)
      {
         // current selection must contain all trace nodes. Only trace nodes have an error bar child.
         for(FGraphicNode n : currSelection) nodes.add(((TraceNode) n).getErrorBarNode());
      }
      else if(nt == FGNodeType.TICKS)
      {
         // determine to which axis the tick set belongs, and its position in the axis's child list.
         TickSetNode tsn = (TickSetNode) initialFGN;
         FGNGraphAxis axis = (FGNGraphAxis) tsn.getParent();
         boolean isPrimary = axis.isPrimary();
         boolean isColorBar = axis.isColorBar();
         int pos = -1;
         for(int i=0; i<axis.getChildCount(); i++) if(axis.getChildAt(i) == tsn)
         {
            pos = i;
            break;
         }
         if(pos < 0) return(nodes);  // node list will be empty -- should never happen

         // either multiple axes or multiple graphs are selected. If axes are selected, only edit tick sets at the same
         // position in the axis child list. If graphs are selected, only edit tick sets under the same axis (primary,
         // secondary or Z) and at the same child position.
         if(currSelection.get(0).getNodeType() == FGNodeType.GRAPH)
         {
            for(FGraphicNode n : currSelection)
            {
               GraphNode g = (GraphNode) n;
               if(isColorBar) axis = g.getColorBar();
               else if(isPrimary) axis = g.getPrimaryAxis();
               else axis = g.getSecondaryAxis();
               if(pos < axis.getChildCount()) nodes.add(axis.getChildAt(pos));
            }
         }
         else if(isColorBar && (currSelection.get(0).getNodeType() != FGNodeType.GRAPH))
         {
            // the axes of a 3D graph use a different kind of tick set, and polar graph axes have no tick sets, but
            // both of these graph containers have a standard color bar.
            for(FGraphicNode n : currSelection)
            {
               axis = ((FGNGraph) n).getColorBar();
               if(pos < axis.getChildCount()) nodes.add(axis.getChildAt(pos));
            }
         }
         else if(currSelection.get(0) instanceof FGNGraphAxis)
         {
            for(FGraphicNode n : currSelection)
            {
               axis = (FGNGraphAxis) n;
               if(pos < axis.getChildCount()) nodes.add(axis.getChildAt(pos));
            }
         }
      }
      else if(currSelection.get(0).getNodeType() == FGNodeType.GRAPH && nt != FGNodeType.GRAPH)
      {
         // multiple 2D graphs are selected, but user is editing an intrinsic component of one of the graphs. In this
         // case we also edit the corresponding components in all selected graphs. The case of editing an axis tick
         // set is already handled above...
         for(FGraphicNode n : currSelection)
         {
            GraphNode g = (GraphNode)n;
            if(nt == FGNodeType.LEGEND) nodes.add(g.getLegend());
            else if(nt == FGNodeType.CBAR) nodes.add(g.getColorBar());
            else if(nt == FGNodeType.AXIS)
               nodes.add(((AxisNode)initialFGN).isPrimary() ? g.getPrimaryAxis() : g.getSecondaryAxis());
            else if(nt == FGNodeType.GRIDLINE)
               nodes.add( 
                     ((GridLineNode)initialFGN).isPrimary() ? g.getPrimaryGridLines() : g.getSecondaryGridLines());
         }
      }
      else if(currSelection.get(0).getNodeType() == FGNodeType.PGRAPH && nt != FGNodeType.PGRAPH)
      {
         // multiple polar graphs selected, but user is editing an intrinsic component of one of the graphs. Here we
         // edit the corresponding component in all selected graphs.
         for(FGraphicNode n : currSelection)
         {
            PolarPlotNode pg = (PolarPlotNode) n;
            if(nt == FGNodeType.LEGEND) nodes.add(pg.getLegend());
            else if(nt == FGNodeType.CBAR) nodes.add(pg.getColorBar());
            else if(nt == FGNodeType.PAXIS)
               nodes.add(((PolarAxisNode)initialFGN).isTheta() ? pg.getThetaAxis() : pg.getRadialAxis());
         }
      }
      else if(nt == FGNodeType.TICKS3D)
      {
         // determine to which axis the 3D tick set belongs, and its position in the axis's child list.
         Ticks3DNode t3 = (Ticks3DNode) initialFGN;
         Axis3DNode a3 = (Axis3DNode) t3.getParent();
         Graph3DNode.Axis which = a3.getAxis();
         int pos = -1;
         for(int i=0; i<a3.getChildCount(); i++) if(a3.getChildAt(i) == t3)
         {
            pos = i;
            break;
         }
         if(pos < 0) return(nodes);  // node list will be empty -- should never happen
         
         // either multiple 3D axes or multiple 3D graphs are selected. If axes are selected, only edit tick sets at the
         // same position in the axis child list. If graphs are selected, only edit tick sets under the same axis (X,
         // Y, Z) and at the same child position.
         boolean graphsSelected = (currSelection.get(0).getNodeType() == FGNodeType.GRAPH3D);
         for(FGraphicNode n : currSelection)
         {
            a3 = graphsSelected ? ((Graph3DNode)n).getAxis(which) : ((Axis3DNode) n);
            if(pos < a3.getChildCount()) nodes.add(a3.getChildAt(pos));
         }
      }
      else if(currSelection.get(0).getNodeType() == FGNodeType.GRAPH3D && nt != FGNodeType.GRAPH3D)
      {
         // analogously for the 3D graph case...
         for(FGraphicNode n : currSelection)
         {
            Graph3DNode g3 = (Graph3DNode)n;
            if(nt == FGNodeType.AXIS3D)
               nodes.add(g3.getAxis(((Axis3DNode)initialFGN).getAxis()));
            else if(nt == FGNodeType.GRID3D)
               nodes.add(g3.getGrid3DNode(((Grid3DNode)initialFGN).getPerpendicularAxis()));
            else if(nt == FGNodeType.BACK3D)
               nodes.add(g3.getBackPlane(((BackPlane3DNode)initialFGN).getBackplaneSide()));
            else if(nt == FGNodeType.LEGEND) nodes.add(g3.getLegend());
            else if(nt == FGNodeType.CBAR) nodes.add(g3.getColorBar());
         }
      }
      else
      {
         // all other cases: selected nodes should same type as node on which editing was initiated
         for(FGraphicNode n : currSelection) if(n.getNodeType() == nt) nodes.add(n);
      }
      return(nodes);
   }
   
   //
   // "Undo/Redo" support
   //
   /** Maximum number of entries in the model's edit history (= 30). */
   private final static int EDITHISTORY_DEPTH = 30;

   /** The model's edit history of reversible changes, in ascending chronological order. */
   private final List<ReversibleEdit> editHistory = new ArrayList<>();
   
   /** 
    * Index of last reversible change in edit history that was undone. A "redo" operation is possible only if this
    * value is less than the current history size.
    */
   private int lastUndone = 0;
   
   /** Flag set while an undo or redo operation is being processed. Blocks posting of reversible changes. */
   private boolean undoRedoInProgress = false;

   /** Whenever this counter is positive, posting of reversible changes to the edit history is blocked. */
   private int blockHistoryCount = 0;

   /**
    * Is the model's edit history currently blocked? This will be the case if a reversible edit is being posted to the
    * edit history, or if an undo/redo operation is currently in progress.
    * @return True if edit history is blocked; else false.
    */
   private synchronized boolean isEditHistoryBlocked()
   {
      return(undoRedoInProgress || blockHistoryCount > 0);
   }
   
   /** 
    * Increment the block count on this model's edit history, temporarily block the posting of reversible changes to 
    * that history. 
    * 
    * <p>This method is intended only for use in special occasions where a particular change has side-effects that could 
    * trigger multiple postings to the edit history that are not desired. A typical use would be to block the edit
    * history while making an entire sequence of changes. The multiple changes could be recorded in a single reversible
    * edit operation, which would be posted to the edit history after unblocking it.</p>. 
    * <p>Each invocation MUST be paired with a call to {@link #unblockEditHistory()}. It is advisable to put the code
    * following invocation of this method in a <code>try-finally</code> block, with the call to unblock the history in
    * the <code>finally</code> block. The paired invocations may be nested, but it is best that all such nested pairs
    * happen in the same thread.</p>
    */
   synchronized void blockEditHistory() { ++blockHistoryCount; }

   /**
    * Decrement the block count on this model's edit history, previously incremented by a call to {@link 
    * #blockEditHistory()}. As long as the block count is positive, the model will ignore any reversible changes posted 
    * to its edit history.
    */
   synchronized void unblockEditHistory()
   {
      if(blockHistoryCount > 0) --blockHistoryCount;
   }

   /**
    * Post a reversible change to this model's edit history. If the history has reached its maximum size, the oldest 
    * change is discarded.
    * 
    * <p>If an undo or redo operation is already in progress, the method will have no effect -- so graphic nodes can use
    * the same infrastructure for performing the reversible change and its corresponding undo or redo operation without 
    * fear of "undoing of an undo"! Posting is also be blocked by calling {@link #blockEditHistory()}, which is useful
    * when performing a sequence of many changes that will be encapsulated by a single reversible edit operation. (If a
    * complex sequence of changes is irreversible, the edit history must be cleared.)</p>
    *
    * @param re Object encapsulating the state information needed to "undo" or "redo" the reversible change. If this is 
    * null, no action is taken. If the posting is blocked for whatever reason, the method will call {@link 
    * ReversibleEdit#release()} to release the object's state information, since the reversible change is ignored.
    */
   void postReversibleEdit(ReversibleEdit re)
   {
      if(re == null) return;
      
      synchronized(this)
      {
         if(undoRedoInProgress || blockHistoryCount > 0)
         {
            re.release();
            return;
         }
         
         // remove any undone changes. Once we post the new change, they cannot be redone!
         while(lastUndone < editHistory.size() && !editHistory.isEmpty())
         {
            ReversibleEdit reDiscard = editHistory.remove(editHistory.size()-1);
            reDiscard.release();
         }
         
         // if edit history has reached the maximum, discard the first (oldest) entry.
         if(editHistory.size() == EDITHISTORY_DEPTH) editHistory.remove(0).release();
      
         editHistory.add(re);
         
         lastUndone = editHistory.size();
      }
      
      fireModelChanged(null, Change.EDIT_HISTORY);
   }

   /** 
    * Get a brief description of the most recent reversible change in the model's edit history that has not yet been
    * undone. This descriptor is intended for use as a tool tip for an "Undo" button or menu item in the GUI. In fact,
    * a non-null return value indicates that an undo operation is currently possible.
    * @return Description of the reversible change that can be undone next. Returns null if there aren't any edits that 
    * can be undone
    */
   synchronized public String getUndoDescription()
   {
      return(lastUndone > 0 ? ("<html>Undo: " + editHistory.get(lastUndone-1).getDescription() + "</html>") : null);
   }
   
   /** 
    * Get a brief description of the reversible change in the model's edit history that was last undone and thus can be
    * redone. This descriptor is intended for use as a tool tip for a "Redo" button or menu item in the GUI. In fact,
    * a non-null return value indicates that a redo operation is currently possible.
    * @return Description of the reversible change that can be redone next. Returns null if there aren't any edits that 
    * can be redone.
    */
   synchronized public String getRedoDescription()
   {
      return(lastUndone < editHistory.size() ? 
            ("<html>Redo: " + editHistory.get(lastUndone).getDescription() + "</html>") : null);
   }
   
   /**
    * Undo the most recent reversible change in this model's edit history that has not already been undone, restoring 
    * the model to its state just prior to that change, <b>OR</b> redo the last reversible change that was undone, 
    * returning the model to its state just after that change. If the undo or redo operation fails for whatever reason, 
    * the model's edit history is wiped out because the model is left in an unknown state.
    * 
    * <p>This and other public methods related to the model's edit history are intended for use by a GUI controller
    * that offers "Undo" and "Redo" actions. The GUI element should also register as a {@link FGModelListener} so that
    * it will receive notification when the edit history status changes -- e.g., to enable/disable the buttons or menu
    * items by which the user initiates the "Undo" or "Redo" action.</p>
    * 
    * @param redo If true, redo the last reversible change that was undone; else undo the most recent reversible change
    * that has not been undone.
    */
   public void undoOrRedo(boolean redo)
   {
      // NOTE: We must NOT perform the operation in a synchronized block, because the operation could be posted back to
      // the model as a reversible change. This would deadlock the thread.
      
      // get the reversible edit to be undone
      ReversibleEdit re = null;
      synchronized(this)
      {
         if(!undoRedoInProgress)
         {
            if((!redo) && lastUndone > 0)
            {
               undoRedoInProgress = true;
               re = editHistory.get(lastUndone-1);
               --lastUndone;
            }
            else if(redo && lastUndone < editHistory.size())
            {
               undoRedoInProgress = true;
               re = editHistory.get(lastUndone);
               ++lastUndone;
            }
         }
      }
      if(re == null) return;
      
      boolean ok = false;
      try
      {
         ok = re.undoOrRedo(redo);
      }
      finally
      {
         // be sure to always unset the flag that blocks edit history while undo/redo in progress. Clear edit history
         // if operation failed, because model is now in an unknown state.
         synchronized(this)
         {
            undoRedoInProgress = false;
            if(!ok)
            {
               lastUndone = 0;
               while(!editHistory.isEmpty()) editHistory.remove(editHistory.size()-1).release();
            }
         }
      }
      
      // whether the operation succeeded or not, the model's edit history status has changed. Inform model listeners,
      // but NEVER within the synchronized block -- because those listeners could call in to the model.
      fireModelChanged(null, Change.EDIT_HISTORY);
   }
   
   /** Clear this model's edit history. This method MUST be called whenever an irreversible change takes place! */
   public void clearEditHistory()
   {
      // NOTE: We do this even if an undo-redo is in progress. Technically, should wait for that op to finish.
      
      boolean changed = false;
      synchronized(this)
      {
         if(!editHistory.isEmpty())
         {
            changed = true;
            lastUndone = 0;
            while(!editHistory.isEmpty()) editHistory.remove(editHistory.size()-1).release();
            blockHistoryCount = 0;
         }
      }
      
      if(changed) fireModelChanged(null, Change.EDIT_HISTORY);
   }
   
   /**
    * This inner class encapsulates all reversible edits initiated by the figure graphic model itself:
    * <ul>
    *    <li>Inserting a graphic node as the child of an existing node in the model. This includes pasting a copy of 
    *    the current clipboard node.</li>
    *    <li>Deleting a graphic node. This includes the case of cutting a graphic node to the model clipboard -- except 
    *    that the clipboard is NOT restored if the cut operation is undone!</li>
    *    <li>Deleting all nodes currently selected in the figure as a a single reversible operation.</li>
    *    <li>Changing only the ordinal position of a node within its parent's child list ("Z-order").</li>
    *    <li>Replacing multiple data sets in the figure in a single operation.</li>
    * </ul>
    * 
    * <p>Any model-level (as opposed to node-level) operation that is not listed here is considered irreversible. When
    * such an operation occurs, the model's edit history is cleared because we can no longer restore it to a previous 
    * state. An example of an irreversible operation handled by <code>FGraphicModel</code> is the wholesale replacement
    * of the model's content.</p>
    * 
    * @author  sruffner
    */
   private class FGMRevOp implements ReversibleEdit
   {
      /** 
       * Construct a reversible model-level edit representing the deletion of one or more non-component nodes, or the
       * insertion of one or more nodes. Initially contains no insertions or deletions; call {@link #addDeletedNode}
       * to record each node deletion, or {@link #addInsertedNode} to record each node insertion. NOTE that the
       * reversible edit object cannot handle a mix of deletions and insertions; only one or the other.
       * @param isInsert True is this reversible edit will contain node insertions; else, node deletions.
       */
      FGMRevOp(boolean isInsert)
      {
         type = isInsert ? NODES_INSERTED : NODES_DELETED;
      }
      
      /**
       * Construct a reversible model-level edit representing the deletion of a single non-component node.
       * 
       * @param n The node deleted.
       * @param parent The node's former parent.
       * @param pos The node's position in the parent's child list prior to deletion.
       */
      FGMRevOp(FGraphicNode n, FGraphicNode parent, int pos)
      {
         assert(n != null && parent != null && pos >= 0);
         
         type = NODES_DELETED;
         nodesAffected.add(new NodeInfo(n, parent, pos, -1));
      }
      
      /** 
       * Record the deletion of the specified node from the specified parent in the figure model. Method takes no
       * action if edit type is not {@link #NODES_DELETED}.
       * 
       * @param n The node deleted.
       * @param parent The node's former parent.
       * @param pos The node's position in the parent's child list prior to deletion.
       */
      void addDeletedNode(FGraphicNode n, FGraphicNode parent, int pos)
      {
         if(type == NODES_DELETED && n != null && parent != null && pos >= 0)
            nodesAffected.add(new NodeInfo(n, parent, pos, -1));
      }
      
      /**
       * Construct a reversible model-level edit representing the insertion of a single non-component node.
       * @param n The node inserted.
       */
      FGMRevOp(FGraphicNode n)
      {
         assert(n != null && n.getParent() != null && n.getParent().getIndexOf(n) >= 0);
         type = NODES_INSERTED;
         nodesAffected.add(new NodeInfo(n, n.getParent(), -1, n.getParent().getIndexOf(n)));
      }
      
      /** 
       * Record the insertion of the specified node in the figure model. Method takes no action if edit type is not 
       * {@link #NODES_INSERTED}.
       * 
       * @param n The node inserted.
       */
      void addInsertedNode(FGraphicNode n)
      {
         if(type == NODES_INSERTED && n != null && n.getParent() != null && n.getParent().getIndexOf(n) >= 0)
            nodesAffected.add(new NodeInfo(n, n.getParent(), -1, n.getParent().getIndexOf(n)));
      }
      
      /**
       * Construct a reversible model-level edit representing a simple change in the position of a graphic node within 
       * its parent's child list. Position in the child list determines a graphic object's "Z-order" during rendering.
       * 
       * @param n The graphic node.
       * @param pos The node's ordinal position in its parent's child list prior to the Z-order change.
       */
      FGMRevOp(FGraphicNode n, int pos)
      {
         assert(n != null && n.getParent() != null && n.getParent().getIndexOf(n) >= 0);
         type = NODE_ZORDER;
         nodesAffected.add(new NodeInfo(n, n.getParent(), pos, n.getParent().getIndexOf(n)));
      }

      /**
       * Construct a reversible model-level edit representing the replacement of two or more datasets in the figure in
       * one composite operation.
       * 
       * @param replaceList A list of the original data sets, the corresponding replacement data sets, and the affected
       * data presentation nodes. These are stored in the list in triplets: dsOld1, dsNew1, dataNode1, dsOld2, dsNew2,
       * dataNode2, etc. The list must contain at least two such triplets. Replacing a single data set is handled by
       * {@link FGNPlottableData#setDataSet}.
       */
      @SuppressWarnings("rawtypes")
      FGMRevOp(List replaceList)
      {
         assert((replaceList != null) && (replaceList.size() % 3 == 0) && (replaceList.size() >= 6));
         type = DS_REPLACED;
         dsReplaceList = replaceList;
      }
      
      @Override public String getDescription()
      {
         String desc = "";
         switch(type)
         {
            case NODES_INSERTED: 
               if(nodesAffected.size() == 1)
                  desc = "Insert " + nodesAffected.get(0).node.getNodeType().getNiceName(); 
               else
                  desc = String.format("Paste %d nodes into figure.", nodesAffected.size());
               break;
            case NODES_DELETED:
               if(nodesAffected.size() == 1) 
                  desc = "Delete " + nodesAffected.get(0).node.getNodeType().getNiceName();
               else
                  desc = String.format("Delete %d nodes from figure.", nodesAffected.size());
               break;
            case NODE_ZORDER:
               desc = "Change Z-order of " + nodesAffected.get(0).node.getNodeType().getNiceName();
               break;
            case DS_REPLACED:
               desc = "Replace " + (dsReplaceList.size() / 3) + " datasets in figure";
               break;
         }
         return(desc);
      }

      @Override public boolean undoOrRedo(boolean redo)
      {
         boolean ok = false;
         NodeInfo ni;
         List<FGraphicNode> selAfter = null;
         if((type==NODES_INSERTED && redo) || (type==NODES_DELETED && !redo)) selAfter = new ArrayList<>();
         
         switch(type)
         {
            case NODES_INSERTED:
               ok = true;
               
               for(int i=0; ok && i<nodesAffected.size(); i++)
               {
                  ni = nodesAffected.get(i);
                  ok = redo ? insertNode(ni.parent, ni.node, ni.posAfter) : deleteNode(ni.node);
                  if(ok && selAfter != null) selAfter.add(ni.node);
               }
               break;
            case NODES_DELETED:
               ok = true;
               for(int i=0; ok && i<nodesAffected.size(); i++)
               {
                  ni = nodesAffected.get(i);
                  ok = redo ? deleteNode(ni.node) : insertNode(ni.parent, ni.node, ni.posBefore);
                  if(ok && selAfter != null) selAfter.add(ni.node);
               }
               break;
            case NODE_ZORDER:
               ni = nodesAffected.get(0);
               ok = changeChildPosition(ni.node.getParent(), ni.node, redo ? ni.posAfter : ni.posBefore);
               break;
            case DS_REPLACED:
               ok = true;
               for(int i=0; ok && i<dsReplaceList.size(); i+=3)
               {
                  DataSet dsOld = (DataSet) dsReplaceList.get(i);
                  DataSet dsNew = (DataSet) dsReplaceList.get(i+1);
                  FGNPlottableData node = (FGNPlottableData) dsReplaceList.get(i+2);
                  ok = node.setDataSet(redo ? dsNew : dsOld);
               }
               break;
         }
         
         // when we insert multiple nodes by redoing an insert or undoing a delete, select the nodes inserted.
         if(ok && selAfter != null) reviseSelection(selAfter, null);
         
         return(ok);
      }

      @Override public void release()
      {
         type = -1;
         nodesAffected.clear();
         if(dsReplaceList != null)
         {
            dsReplaceList.clear();
            dsReplaceList = null;
         }
      }

      /** Op type indicating that one or more nodes were inserted into the model. */
      private final static int NODES_INSERTED = 0;
      
      /** Op type indicating that one or more (non-component) graphic node were removed from the model. */
      private final static int NODES_DELETED = 1;
   
      /** Op type indicating that the Z-order of a child graphic node was changed. */
      private final static int NODE_ZORDER = 2;
      
      /** Op type indicating that multiple datasets were replaced in a single operation. */
      private final static int DS_REPLACED = 3;
      
      /** The undoable operation type. */
      private int type;
      
      /** Encapsulation of the operation(s) on one or more nodes in the figure. */
      private final List<NodeInfo> nodesAffected = new ArrayList<>();
            
      /**
       * For a multi-dataset replacement only. This list will contain two or more <code>DataSet,FGNPlottableData</code> 
       * pairs, where each dataset is the original dataset installed in the corresponding presentation node before the
       * replacement occurred.
       */
      @SuppressWarnings("rawtypes")
      private List dsReplaceList = null;
      
      /** 
       * A simple container for the information need to define a node deletion, insertion, or change in position 
       * within its parent's child list.
       */
      private class NodeInfo
      {
         NodeInfo(FGraphicNode node, FGraphicNode parent, int posBefore, int posAfter)
         {
            this.node = node;
            this.parent = parent;
            this.posBefore = posBefore;
            this.posAfter = posAfter;
         }
         
         final FGraphicNode node;
         final FGraphicNode parent;
         final int posBefore;
         final int posAfter;
      }
   }

   
   //
   // RenderableModel support
   //

   /**
    * The <code>RenderableModelViewer</code> in which this <em>DataNav</em> graphic document is currently viewed.
    */
   RenderableModelViewer rmviewer = null;
   
   public boolean registerViewer(RenderableModelViewer viewer)
   {
      if(rmviewer != null || viewer == null) return(false);
      rmviewer = viewer;

      // do a complete re-render in the new viewer
      Graphics2D g2d = getViewerGraphics();
      try { root.getRenderBounds(g2d, true, null); }
      finally { if(g2d != null) g2d.dispose(); }
      return(true);
   }

   public void unregisterViewer(RenderableModelViewer viewer) { if(rmviewer == viewer) rmviewer = null; }
   public RenderableModelViewer getViewer() { return(rmviewer); }

   /**
    * Retrieve a Java2D graphics context from the <code>RenderableModelViewer</code> in which this 
    * <code>FGraphicModel</code> is currently displayed. This context should never be rendered into -- rendering should 
    * only occur in the context of a rendering task scheduled by the viewer itself; but it may be used to obtain font 
    * rendering context for measuring text bounds.
    * 
    * @return A graphics context from the currently registered <code>RenderableModelViewer</code>. If there is no such 
    * viewer, the method returns <code>null</code>.
    */
   Graphics2D getViewerGraphics()
   {
      return((rmviewer == null) ? null : rmviewer.getViewerGraphicsContext());
   }

   /**
    * Helper method informs the registered <code>RenderableModelViewer</code> whenever there is any change in the 
    * graphic defined by this <code>FGraphicModel</code>. Any graphic node can use this method to trigger a re-rendering 
    * whenever the node's definition is altered. The method has no effect if there is no viewer currently registered 
    * with this <code>FGraphicModel</code>.
    * 
    * @param dirtyRects List of dirty areas in the current graphic model that need to be re-rendered as a result of the 
    * change. Must be expressed in the "global" rendering coordinates of the <code>RootRenderable</code> node. May be 
    * <code>null</code> or an empty list, in which case a full re-rendering of the graphic model occurs.
    */
   private void fireModelMutated(List<Rectangle2D> dirtyRects)
   {
      if(rmviewer != null) rmviewer.modelMutated(dirtyRects);
   }

   /**
    * Helper method informs the registered {@link RenderableModelViewer} whenever there is a change
    * in the identity of the graphic node that currently has the focus for editing and display purposes. By definition,
    * this "focus node" is always the first node in the model's selection list. Since a change in the identity of that
    * node implies a change in the model's current selection, the {@link Change#SELECTION} notification is also sent.
    */
   private void fireFocusChanged()
   {
      if(rmviewer != null) rmviewer.focusChanged();
      fireModelChanged(null, Change.SELECTION);
   }

   /** Always returns the model's root figure node. */
   public RootRenderable getCurrentRootRenderable()  { return(root); }

   public boolean isFocusSupported() { return(true); }

   /** Always returns the first node in the model's current selection list. */
   public Focusable getCurrentFocusable() { return(getSelectedNode()); }

   /**
    * In response to a "mouse click" on the renderable model viewer on which this graphic model is displayed, this 
    * method searches for a graphic node "hit" by the mouse. The search starts at the root node and works downward 
    * through the node hierarchy, looking for the smallest descendant (ie, having a bounding rectangle with the smallest
    * area) that is "hit" by the mouse. If one is found, that node becomes the model's current focus node. Also, it 
    * receives the selection for GUI display and editing purposes; any previous selection is discarded.
    */
   public void pointClicked(Point2D p)
   {
      if(root == null || rmviewer == null || p == null) return;
      FGraphicNode hitNode = root.hitTest(p);
      if(hitNode != null) setSelectedNode(hitNode);
   }

   public void moveFocusable(double dx, double dy)
   {
      FGraphicNode fgn = getSelectedNode();
      if(fgn != null) fgn.executeMove(dx, dy, true);
   }

   
   /**
    * Starting with the graphic's root node, find the smallest node -- i.e., the node with a bounding rectangle of the
    * smallest overall area -- that contains the specified point. This uses the same algorithm as {@link 
    * #pointClicked}, but it does not change the identity of the current focus node and does not post any
    * notifications to registered listeners. <i>The method only works when the graphic is installed in a viewer, which 
    * provides a context for computing node bounds.</i>
    * 
    * @param p A point in the graphic model; coordinates are in milli-inches WRT the logical painting coordinate 
    * (x-axis increasing rightward, y-axis increasing upward, origin at bottom-left) system of the root node.
    */
   public FGraphicNode findSmallestNodeUnder(Point2D p)
   {
      return(root == null || rmviewer == null || p == null ? null : root.hitTest(p));
   }

   /**
    * Find all graphic nodes in this figure, except the root figure node itself, that "intersect" the specified 
    * rectangular region. <b>NOTE: If a given node is found to intersect the rectangle, then none of its descendants 
    * are included in the list. This method is intended for use in selecting all nodes within a specified rectangular
    * region. Selecting any node implicitly selects all of its descendants.</b>
    * 
    * @param r A rectangle specified in the the logical painting coordinate system of the figure (x-axis increasing
    * rightward, y-axis increasing upward, origin at bottom-left, in milli-inches).
    * @return A list populated with all nodes in the figure (except the root figure node) that overlap the specified 
    * rectangle. Each node in the list has a cached global shape, {@link FGraphicNode#getCachedGlobalShape()}, that 
    * intersects the rectangle. As noted, if a node is included in the list, none of its descendants will be.
    */
   public List<FGraphicNode> findAllIntersectingNodes(Rectangle2D r)
   {
      List<FGraphicNode> out = new ArrayList<>();
      if(r == null) return(out);
      
      Stack<FGraphicNode> nodeStack = new Stack<>();
      for(int i=0; i<root.getChildCount(); i++) nodeStack.push(root.getChildAt(i));
      while(!nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();
         if(n.getCachedGlobalShape().intersects(r))
            out.add(n);
         else for(int i=0; i<n.getChildCount(); i++)
            nodeStack.push(n.getChildAt(i));
      }
      
      return(out);
   }
   
   //
   // Unicode characters supported in any DataNav textual content
   //

   /** Set of Unicode 16-bit characters that are supported when rendering any text in a <em>DataNav</em> figure. */
   private static final UnicodeSubset SUPPORTEDCHARS = new UnicodeSubset( "supported", 
      "\u0009\u0009\n\n\r\r" +
      "\u0020\u007E\u00A1\u00FF\u0152\u0153\u0192\u0192\u0391\u03A1\u03A3\u03A9\u03B1\u03C9\u03D1\u03D1" +
      "\u03D5\u03D6\u2013\u2014\u2018\u201A\u201C\u201E\u2020\u2022\u2026\u2026\u2030\u2030\u2032\u2033" +
      "\u2039\u203A\u2044\u2044\u20AC\u20AC\u2111\u2111\u2118\u2118\u211C\u211C\u2122\u2122\u2135\u2135" +
      "\u2190\u2194\u21B5\u21B5\u21D0\u21D4\u2200\u2200\u2202\u2203\u2205\u2205\u2207\u2209\u220B\u220B" +
      "\u220F\u220F\u2211\u2211\u221A\u221A\u221D\u221E\u2220\u2220\u2227\u222B\u2245\u2245\u2248\u2248" +
      "\u2260\u2261\u2264\u2265\u2282\u2284\u2286\u2287\u2295\u2295\u2297\u2297\u22A5\u22A5\u2320\u2321" +
      "\u25CA\u25CA\u2660\u2660\u2663\u2663\u2665\u2666" );

   /** Same as {@link #SUPPORTEDCHARS}, but without the carriage-return, line-feed and tab characters. */
   private static final UnicodeSubset SUPPORTEDCHARS_NOCRLFTAB = new UnicodeSubset( "supported_nocrlft", 
      "\u0020\u007E\u00A1\u00FF\u0152\u0153\u0192\u0192\u0391\u03A1\u03A3\u03A9\u03B1\u03C9\u03D1\u03D1" +
      "\u03D5\u03D6\u2013\u2014\u2018\u201A\u201C\u201E\u2020\u2022\u2026\u2026\u2030\u2030\u2032\u2033" +
      "\u2039\u203A\u2044\u2044\u20AC\u20AC\u2111\u2111\u2118\u2118\u211C\u211C\u2122\u2122\u2135\u2135" +
      "\u2190\u2194\u21B5\u21B5\u21D0\u21D4\u2200\u2200\u2202\u2203\u2205\u2205\u2207\u2209\u220B\u220B" +
      "\u220F\u220F\u2211\u2211\u221A\u221A\u221D\u221E\u2220\u2220\u2227\u222B\u2245\u2245\u2248\u2248" +
      "\u2260\u2261\u2264\u2265\u2282\u2284\u2286\u2287\u2295\u2295\u2297\u2297\u22A5\u22A5\u2320\u2321" +
      "\u25CA\u25CA\u2660\u2660\u2663\u2663\u2665\u2666" );

   /** Set of supported Unicode 16-bit characters, organized into related character subsets. */
   private static final UnicodeSubset[] SUPPORTED_UNICODESUBSETS = new UnicodeSubset[] {
      new UnicodeSubset("Latin-1 Supplemental", "\u00A1\u00FF\u0152\u0153\u0192\u0192" +
         "\u20AC\u20AC\u2111\u2111\u2118\u2118\u211C\u211C\u2122\u2122\u2135\u2135"),
      new UnicodeSubset("Greek Letters", "\u0391\u03A1\u03A3\u03A9\u03B1\u03C9\u03D1\u03D1\u03D5\u03D6"),
      new UnicodeSubset("Miscellaneous Punctuation", "\u2013\u2014\u2018\u201A\u201C\u201E\u2020\u2022\u2026\u2026" +
            "\u2030\u2030\u2032\u2033\u2039\u203A\u2044\u2044"),
      new UnicodeSubset("Arrows, Math, Other", 
         "\u2190\u2194\u21B5\u21B5\u21D0\u21D4\u2200\u2200\u2202\u2203\u2205\u2205\u2207\u2209\u220B\u220B" +
         "\u220F\u220F\u2211\u2211\u221A\u221A\u221D\u221E\u2220\u2220\u2227\u222B\u2245\u2245\u2248\u2248" +
         "\u2260\u2261\u2264\u2265\u2282\u2284\u2286\u2287\u2295\u2295\u2297\u2297\u22A5\u22A5\u2320\u2321" +
         "\u25CA\u25CA\u2660\u2660\u2663\u2663\u2665\u2666"),
      new UnicodeSubset("Printable 7-bit ASCII", "\u0009\u0009\n\n\r\r\u0020\u007E")
   };

   /**
    * Replace any unsupported characters in the specified string with a question mark. <em>FypML</em> supports only a
    * small part of the Unicode character set.
    * 
    * @param s The string to be filtered.
    * @return A string equal to the provided string, but with any unsupported characters replaced by question marks.
    */
   public static String replaceUnsupportedCharacters(String s)
   {
      return( SUPPORTEDCHARS.replaceBadCharacters('?', s));
   }

   /**
    * Remove any unsupported characters in the specified string. <i>FypML</i> supports only a small part of the Unicode 
    * character set.
    * @param s The string to be filtered
    * @param rmvOther If true, any tab, carriage-return, and line-feed characters are also removed.
    * @return The filtered string, possibly unchanged.
    */
   public static String removeUnsupportedCharacters(String s, boolean rmvOther)
   {
      return((rmvOther ? SUPPORTEDCHARS_NOCRLFTAB : SUPPORTEDCHARS).replaceBadCharacters('\0', s));
   }
   
   /**
    * Get the entire set of Unicode characters supported in any text rendered by <em>DataNav</em>. This set represents 
    * a very small portion of the entire Unicode character set. It is based on the set of characters defined in the 
    * Postscript Language's Standard Latin and Symbol character sets. All printable ASCII characters except the 
    * formfeed (U000C) are included, as well as the Latin-1 Supplement characters in the range U00A1-U007F. The 
    * remaining supported characters are primarily those found in the original Postscript Symbol font; these characters 
    * are scattered over several different segments of the Unicode character set.
    * 
    * @return The set of Unicode characters supported in any text rendered by <em>DataNav</em>.
    */
   public static UnicodeSubset getSupportedCharacterSet() { return(SUPPORTEDCHARS); }

   /**
    * This method returns the same set of characters provided by <code>getSupportedCharacterSet()</code>, except that 
    * it is organized into an array of related character subsets:
    * <ul>
    *    <li>Latin-1 Supplemental</li>
    *    <li>Greek Letters</li>
    *    <li>Miscellaneous Punctuation</li>
    *    <li>Arrows, Math, Other</li>
    *    <li>Printable 7-bit ASCII</li>
    * </ul>
    * 
    * @return Array of non-overlapping <code>UnicodeSubset</code>s covering the entire set of Unicode characters 
    * supported in any text rendered by <em>DataNav</em>.
    */
   public static UnicodeSubset[] getSupportedUnicodeSubsets() { return( SUPPORTED_UNICODESUBSETS ); }

   
   //
   // Printable implementation
   //

   /**
    * This method, implementing the <code>Printable</code> interface, provides support for printing the graphic model's 
    * content as Java2D graphics.
    * 
    * <p>The method makes the following assumptions about the <code>Graphics</code> context passed to it. 
    * <ul>
    *    <li>The context can be safely cast to <code>Graphics2D</code>. Since <code>java.awt.print</code> was 
    *    introduced in JDK1.2 and <em>DataNav</em> requires at least 1.4, this should always be the case.</li>
    *    <li>The graphics context is initialized so that the origin is at the very top-left corner of the printed page, 
    *    with x-axis increasing rightward, y-axis increasing downward, and units of 72 pixels/inch.</li>
    * </ul>
    * </p>
    * 
    * <p>The assumptions about the state of the graphics context are critical, since this method will transform the 
    * graphics context to the standard <em>DataNav</em> convention: x-axis increasing rightward, y-axis increasing 
    * <em>upward</em>, and units in <em>milli-inches</em>. Furthermore, it will translate the origin IAW the specified 
    * page format and the "print location" of the model's root graphic node so that the origin sits at the bottom-left 
    * corner of that node. It then simply invokes the root node's <code>render()</code> method, passing in the graphics 
    * context just prepared.</p>
    * 
    * <p>Since <em>DataNav</em> graphics always print to a single page, this method always returns 
    * <code>Printable.NO_SUCH_PAGE</code> when the <code>pageIndex</code> is greater than 0.</p>
    * 
    * @see Printable#print(Graphics, PageFormat, int)
    */
   public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException
   {
      if(root == null || pageFormat ==  null || pageIndex > 0) return(Printable.NO_SUCH_PAGE);
 
      // convert graphics context to milli-in, w/y-axis increasing upward, and origin at BL corner of page
      Graphics2D g2d = (Graphics2D) graphics;
      final double ptToMI = 1000.0/72.0;
      g2d.scale(1.0/ptToMI, -1.0/ptToMI);
      g2d.translate(0, -pageFormat.getHeight()*ptToMI);

      // now translate to BL corner of the root graphic object to be printed
      double xBL = pageFormat.getImageableX() * ptToMI;
      double yBL = (pageFormat.getHeight() - pageFormat.getImageableHeight() - pageFormat.getImageableY()) * ptToMI;
      Point2D printLoc = root.getPrintLocationMI();
      if(printLoc != null)
      {
         xBL += printLoc.getX();
         yBL += printLoc.getY();
      }
      g2d.translate(xBL, yBL);

      // paint the graphics
      root.render(g2d, null);

      return(Printable.PAGE_EXISTS);
   }

   
   //
   // Object
   //
   
   /**
    * Overridden to return the title of this graphic model's root node.
    * @see Object#toString()
    */
   @Override public String toString()
   {
      return((root != null) ? root.getTitle() : "Empty figure");
   }
}
