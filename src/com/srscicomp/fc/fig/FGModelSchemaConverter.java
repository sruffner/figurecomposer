package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

import javax.imageio.ImageIO;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dutil.StrokeJoin;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.GenericFont;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.common.xml.BasicSchemaElement;
import com.srscicomp.common.xml.ISchema;
import com.srscicomp.common.xml.StaxWrapper;
import com.srscicomp.common.xml.XMLException;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.fypml.FGModelSchema;

/**
 * <code>FGModelSchemaConverter</code> is a static singleton class that serves as the "glue" between the <i>DataNav</i> 
 * figure graphic model and its serialized XML format, as encapsulated by the {@link ISchema
 * ISchema} instance representing the current FypML schema version. It handles the task of converting between model and
 * XML, while keeping the implementation details of each as separate as possible.
 * 
 * <p>Whenever the graphic model is revised and/or the schema specifications change, a new <code>ISchema</code> 
 * implementation is developed to encapsulate the new schema version. Then, this class must be updated to handle 
 * converting the new graphic model to/from the new XML schema.</p>
 * 
 * @author 	sruffner
 */
public class FGModelSchemaConverter
{
   /**
    * Save the graphic model to the specified file in XML format, conforming to the current <i>DataNav</i> XML schema.
    * This method may take a significant amount of time to execute, so it should be invoked on a background thread.
    * 
    * @param model The graphic model.
    * @param f The target file. If it already exists, its contents will be overwritten.
    * @return Null if operation succeeds; else a brief message explaining the error that occurred.
    */
   public static String toXML(FGraphicModel model, File f)
   {
      BufferedWriter wrt = null;
      String errDesc;
      try 
      { 
         wrt = new BufferedWriter(new FileWriter(f));
         errDesc = toXML(model, wrt); 
      }
      catch(IOException ioe) 
      { 
         errDesc = "Unable to write DataNav XML:\n" + ioe.getMessage(); 
      }
      catch(Exception e)
      {
         errDesc = "toXML() - Unexpected exception: " + e.getMessage();
      }
      finally
      {
         try { if(wrt != null) wrt.close(); } catch(IOException ignored) {}
      }
      
      return(errDesc);
   }

   /**
    * Save the graphic model to the specified stream writer in XML format, IAW the current <i>DataNav</i> XML schema. 
    * This method may take a significant amount of time to execute, so it should be invoked on a background thread.
    * 
    * @param model The graphic model.
    * @param writer The stream writer to which the XML definition of the model is written. The writer is <b>NOT</b>
    * closed after the operation is completed.
    * @return Null if operation succeeds; else a brief message explaining the error that occurred.
    */
   public static String toXML(FGraphicModel model, Writer writer) 
   {
      if(model == null || writer == null) throw new IllegalArgumentException("Null argument!");
      
      String errDesc = null;
      try
      {
         ISchema schemaDoc = fromModelToXML(model);
         StaxWrapper staxWriter = new StaxWrapper();
         staxWriter.writeContent(writer, schemaDoc, false);
      }
      catch(XMLException xe)
      {
         errDesc = "Unable to write DataNav XML:\n" + xe.getMessage();
      }

      return(errDesc);
   }

   /**
    * Construct the <i>DataNav</i> figure model defined by the contents of the specified file, which should supply the
    * the model's definition in XML format, IAW a current or older <i>DataNav</i> XML schema. If the content conforms
    * to an older schema, that schema document is first migrated to the current schema before creating the graphic 
    * model. This method may take a significant amount of time to execute, so it should be invoked on a background 
    * thread.
    * 
    * @param f The plain text file that contains the graphic model's XML definition.
    * @param errBuf If non-null and operation fails, this will contain a brief description of the error that occurred.
    * @return The graphic model defined by the XML content of the file. Returns null if operation fails.
    */
   public static FGraphicModel fromXML(File f, StringBuffer errBuf)
   {
      Reader rdr = null;
      FGraphicModel model;
      try
      {
         rdr = new BufferedReader(new FileReader(f));
         model = fromXML(rdr, errBuf);
      }
      catch(FileNotFoundException fnfe)
      {
         if(errBuf != null)
            errBuf.replace(0, errBuf.length(), "Unable to migrate/parse DataNav XML:\n" + fnfe.getMessage());
         return(null);
      }
      finally
      {
         try { if(rdr != null) rdr.close(); } catch(IOException ignored) {}
      }
      
      return(model);
   }
   
   /**
    * Construct the <i>DataNav</i> figure model defined by the contents of the specified character stream reader, which
    * should supply the model's definition in XML format, IAW a current or older <em>DataNav</em> XML schema. If the
    * content conforms to an older schema, that schema document is first migrated to the current schema before creating 
    * the graphic model. This method may take a significant amount of time to execute, so it should be invoked on a 
    * background thread.
    * 
    * @param reader The character stream reader that sources the graphic model's XML definition. It is <b>NOT</b> closed 
    * after the operation is completed. Reader must support the <code>mark()</code> and <code>reset()</code> operations!
    * @param errBuf If non-null and operation fails, this will contain a brief description of the error that occurred.
    * @return The graphic model defined by the XML content of the reader. Returns null if the operation fails.
    */
   public static FGraphicModel fromXML(Reader reader, StringBuffer errBuf)
   {
      FGraphicModel model = null;
      String errDesc = null;
      try
      {
         // parse XML document from character stream
         ISchema schema = FGModelSchema.fromXML(reader, errBuf);
         if(schema == null) return(null);

         // construct the graphic model from the XML document
         model = fromXMLToModel(schema);
         
         // for each graph in the figure, schema version 9 adds a "Z axis" that controls how XYZIMG data in a "heatmap"
         // element is mapped to RGB color. To migrate schema 8 documents as closely as possible, the mapped data range
         // for the Z-axis should match the largest data range observed across all heatmaps in the parent graph. We 
         // cannot do this during schema migration because that would require parsing the dataset content, which is 
         // actually put off until schema-to-model conversion. We perform the fix here. NOTE that it may not be 
         // necessary to do this in future versions.
         fixColorAxisIfNecessary(schema.getOriginalVersion(), model);
      }
      catch(XMLException xe)
      {
         errDesc = "Unable to migrate/parse DataNav XML:\n" + xe.getMessage();
      }
      catch(Exception e)
      {
         errDesc = "Unexpected exception:\n" + e.getMessage();
      }
      
      if(errDesc != null && errBuf != null) errBuf.replace(0, errBuf.length(), errDesc);
      return(errDesc == null ? model : null);
   }

   /**
    * Convert the specified <em>DataNav</em> figure to a <code>ISchema</code> document conforming to the current 
    * <em>DataNav</em> XML schema version.
    * 
    * @param model The in-memory graphic model of a <em>DataNav</em> figure.
    * @return A schema document that defines the specified model IAW the current version of the <em>DataNav</em> XML
    * schema. This document supports serializing the graphic model to and from disk file. If <code>model==null</code>, 
    * method returns <code>null</code>.
    * @throws XMLException if there's a programming error in the conversion code. The exception message suggests 
    * reporting the problem to <em>DataNav</em>'s developer.
    */
   public static ISchema fromModelToXML(FGraphicModel model) throws XMLException
   {
      if(model == null) return(null);
      
      ISchema schema = FGModelSchema.createCurrentSchemaDocument();
      try
      {
         // calling this before serializing ensures that each distinct dataset has a unique ID. It is VITAL that we 
         // do this before serializing each data presentation element, which will only store an ID reference to the 
         // dataset it renders
         model.getAllDataSetsInUse();
         
         graphicNodeToXML(schema, null, model.getRoot());
         putDataSetsToXML(schema, model);
      }
      catch(Throwable t)
      {
         StringBuilder sb = new StringBuilder();
         sb.append("Model-to-XML conversion error: ").append(t.getMessage());
         StackTraceElement[] trace = t.getStackTrace();
         sb.append("\nTop of stack trace:\n");
         if(trace == null || trace.length == 0) sb.append("Unavailable.");
         else
            for(StackTraceElement stackTraceElement : trace)
               sb.append(stackTraceElement.toString()).append("\n");
         sb.append("\nReport to developer.");
         throw new XMLException(sb.toString());
      }

      return(schema);
   }
   
   /**
    * Construct the <em>DataNav</em> figure defined by the specified <code>ISchema</code> document, conforming to the
    * current <em>DataNav</em> XML schema version.
    * 
    * @param schema The schema document.
    * @return The graphic model of a <em>DataNav</em> figure, as defined by the schema document.
    * @throws XMLException if schema document is null or does not conform to the current schema version, or if there's a
    * programming error in the conversion code. The exception message suggests reporting the problem to <i>DataNav</i>'s
    * developer.
    */
   public static FGraphicModel fromXMLToModel(ISchema schema) throws XMLException
   {
      if(schema == null || (schema.getVersion() != FGModelSchema.getCurrentSchemaVersion())) 
         throw new XMLException("Null or old schema!");
      
      FGraphicModel model = new FGraphicModel();
      try
      {
         graphicNodeFromXML(schema, (BasicSchemaElement) schema.getRootElement(), model.getRoot());
         getDataSetsFromXML(schema, model);
         
         // put focus on root node initially, then clear model's edit history and modified flag!
         model.setSelectedNode(model.getRoot());
         model.clearEditHistory();
         model.setModified(false);
      }
      catch(Throwable t)
      {
         t.printStackTrace();
         throw new XMLException("XML-to-model conversion error:\n" + t.getMessage() + "\nReport to developer.");
      }
      return(model);
   }
   
   /**
    * Get the tag name of the <i>FypML</i> element that encapsulates the definition of a graphic node with the specified
    * node type. Note that, with the introduction of the 3D graph node in schema version 21 (app version 5.0.0), 
    * certain node types map to the same <i>FypML</i> element. See {@link #getNodeTypeForFypMLTag}.
    * 
    * @param nt The graphic node type.
    * @return The tag of the corresponding <i>FypML</i> element. Returns null if node type not recognized.
    */
   private static String getFypMLTagForNodeType(FGNodeType nt)
   { 
      String tag = null;
      switch(nt)
      {
      case AREA:        tag = FGModelSchema.EL_AREA; break;
      
      // note that these two graphic nodes are represented by the same FypML element!
      case AXIS:
      case AXIS3D:      tag = FGModelSchema.EL_AXIS; break;

      case BACK3D:      tag = FGModelSchema.EL_BACK3D; break;
      case BAR:         tag = FGModelSchema.EL_BAR; break;
      case BOX:         tag = FGModelSchema.EL_BOX; break;
      case CALIB:       tag = FGModelSchema.EL_CALIB; break;
      case EBAR:        tag = FGModelSchema.EL_EBAR; break;
      case FIGURE:      tag = FGModelSchema.EL_FIGURE; break;
      case FUNCTION:    tag = FGModelSchema.EL_FUNCTION; break;
      case GRAPH:       tag = FGModelSchema.EL_GRAPH; break;
      case PGRAPH:      tag = FGModelSchema.EL_PGRAPH; break;
      case PAXIS:       tag = FGModelSchema.EL_PAXIS; break;
      case GRAPH3D:     tag = FGModelSchema.EL_GRAPH3D; break;
      
      // note that these two graphic nodes are represented by the same FypML element!
      case GRID3D: 
      case GRIDLINE:    tag = FGModelSchema.EL_GRIDLINE; break;
      
      case CONTOUR:     tag = FGModelSchema.EL_CONTOUR; break;
      case IMAGE:       tag = FGModelSchema.EL_IMAGE; break;
      case LABEL:       tag = FGModelSchema.EL_LABEL; break;
      case LEGEND:      tag = FGModelSchema.EL_LEGEND; break;
      case LINE:        tag = FGModelSchema.EL_LINE; break;
      case PIE:         tag = FGModelSchema.EL_PIE; break;
      case RASTER:      tag = FGModelSchema.EL_RASTER; break;
      case SCATTER:     tag = FGModelSchema.EL_SCATTER; break;
      case SCATTER3D:   tag = FGModelSchema.EL_SCATTER3D; break;
      case SURFACE:     tag = FGModelSchema.EL_SURFACE; break;
      case SHAPE:       tag = FGModelSchema.EL_SHAPE; break;
      case SYMBOL:      tag = FGModelSchema.EL_SYMBOL; break;
      case TEXTBOX:     tag = FGModelSchema.EL_TEXTBOX; break;
      
      // note that these two graphic nodes are represented by the same FypML element!
      case TICKS: 
      case TICKS3D:     tag = FGModelSchema.EL_TICKS; break;
      
      case TRACE:       tag = FGModelSchema.EL_TRACE; break;
      case CBAR:        tag = FGModelSchema.EL_COLORBAR; break;
      case VIOLIN:      tag = FGModelSchema.EL_VIOLIN; break;
      case SCATLINESTYLE: tag = FGModelSchema.EL_SCATTERLINE; break;
      default:          break;
      }
      
      return(tag);
   }
   
   /**
    * Get the node type of the graphic node defined by the specified <i>FypML</i> tag.
    * 
    * <p>There is a one-to-one correspondence between node type and <i>FypML</i> tag, with three notable exceptions
    * pertaining to components of the 2D and 3D graph objects. Hence, the node parent is specified to handle these 
    * cases properly.
    * <ul>
    * <li>If the tag is {@link FGModelSchema#EL_AXIS}, the node type for a 2D graph parent is {@link FGNodeType#AXIS};
    * for a 3D graph parent, it is {@link FGNodeType#AXIS3D}.</li>
    * <li>If the tag is {@link FGModelSchema#EL_GRIDLINE}, the node type is {@link FGNodeType#GRIDLINE} if the parent is
    * a 2D graph; for a 3D graph parent, it is {@link FGNodeType#GRID3D}.</li>
    * <li>If the tag is {@link FGModelSchema#EL_TICKS}, the node type is {@link FGNodeType#TICKS} if the parent is a 2D
    * graph axies; for a 3D graph axis, it is {@link FGNodeType#TICKS3D}.</li>
    * </ul>
    * </p>
    * @param parent The parent of the graphic node defined by the <i>FypML</i> tag.
    * @param tag The <i>FypML</i> element tag.
    * @return The graphic node type requested, or null if not found.
    */
   private static FGNodeType getNodeTypeForFypMLTag(FGraphicNode parent, String tag)
   {
      FGNodeType nt = null;
      if(FGModelSchema.EL_AREA.equals(tag)) nt = FGNodeType.AREA;
      else if(FGModelSchema.EL_AXIS.equals(tag) && parent != null)
      {
         if(parent.getNodeType() == FGNodeType.GRAPH3D) nt = FGNodeType.AXIS3D;
         else nt = FGNodeType.AXIS;
      }
      else if(FGModelSchema.EL_BACK3D.equals(tag)) nt = FGNodeType.BACK3D;
      else if(FGModelSchema.EL_BAR.equals(tag)) nt = FGNodeType.BAR;
      else if(FGModelSchema.EL_BOX.equals(tag)) nt = FGNodeType.BOX;
      else if(FGModelSchema.EL_CALIB.equals(tag)) nt = FGNodeType.CALIB;
      else if(FGModelSchema.EL_EBAR.equals(tag)) nt = FGNodeType.EBAR;
      else if(FGModelSchema.EL_FIGURE.equals(tag)) nt = FGNodeType.FIGURE;
      else if(FGModelSchema.EL_FUNCTION.equals(tag)) nt = FGNodeType.FUNCTION;
      else if(FGModelSchema.EL_GRAPH.equals(tag)) nt = FGNodeType.GRAPH;
      else if(FGModelSchema.EL_PGRAPH.equals(tag)) nt = FGNodeType.PGRAPH;
      else if(FGModelSchema.EL_PAXIS.equals(tag)) nt = FGNodeType.PAXIS;
      else if(FGModelSchema.EL_GRAPH3D.equals(tag)) nt = FGNodeType.GRAPH3D;
      else if(FGModelSchema.EL_GRIDLINE.equals(tag) && parent != null)
      {
         if(parent.getNodeType() == FGNodeType.GRAPH3D) nt = FGNodeType.GRID3D;
         else nt = FGNodeType.GRIDLINE;
      }
      else if(FGModelSchema.EL_CONTOUR.equals(tag)) nt = FGNodeType.CONTOUR;
      else if(FGModelSchema.EL_IMAGE.equals(tag)) nt = FGNodeType.IMAGE;
      else if(FGModelSchema.EL_LABEL.equals(tag)) nt = FGNodeType.LABEL;
      else if(FGModelSchema.EL_LEGEND.equals(tag)) nt = FGNodeType.LEGEND;
      else if(FGModelSchema.EL_LINE.equals(tag)) nt = FGNodeType.LINE;
      else if(FGModelSchema.EL_PIE.equals(tag)) nt = FGNodeType.PIE;
      else if(FGModelSchema.EL_RASTER.equals(tag)) nt = FGNodeType.RASTER;
      else if(FGModelSchema.EL_SCATTER.equals(tag)) nt = FGNodeType.SCATTER;
      else if(FGModelSchema.EL_SCATTER3D.equals(tag)) nt = FGNodeType.SCATTER3D;
      else if(FGModelSchema.EL_SURFACE.equals(tag)) nt = FGNodeType.SURFACE;
      else if(FGModelSchema.EL_SHAPE.equals(tag)) nt = FGNodeType.SHAPE;
      else if(FGModelSchema.EL_SYMBOL.equals(tag)) nt = FGNodeType.SYMBOL;
      else if(FGModelSchema.EL_TEXTBOX.equals(tag)) nt = FGNodeType.TEXTBOX;
      else if(FGModelSchema.EL_TICKS.equals(tag))
      {
         if(parent.getNodeType() == FGNodeType.AXIS3D) nt = FGNodeType.TICKS3D;
         else nt = FGNodeType.TICKS;
      }
      else if(FGModelSchema.EL_TRACE.equals(tag)) nt = FGNodeType.TRACE;
      else if(FGModelSchema.EL_COLORBAR.equals(tag)) nt = FGNodeType.CBAR;
      else if(FGModelSchema.EL_VIOLIN.equals(tag)) nt = FGNodeType.VIOLIN;
      else if(FGModelSchema.EL_SCATTERLINE.equals(tag)) nt = FGNodeType.SCATLINESTYLE;

      return(nt);
   }
   
   /**
    * This method takes a graphic node from the <em>DataNav</em> figure graphic model and recursively constructs the 
    * tree of schema elements that define the node and its descendants.
    * 
    * @param schema The schema content document (conforms to the current schema version).
    * @param parent The immediate parent of the schema element to be constructed by this method. The element is appended 
    * to this parent's child list. If the graphic node being converted is the root figure node, then this argument is 
    * <code>null</code>.
    * @param n The graphic node to be converted to XML.
    * @throws XMLException If an exception is thrown, it indicates an error in the conversion code.
    */
   private static void graphicNodeToXML(ISchema schema, BasicSchemaElement parent, FGraphicNode n) throws XMLException
   {
      if(schema == null || n == null) return;
      FGNodeType nodeType = n.getNodeType();
      if(parent == null && n.getNodeType() != FGNodeType.FIGURE)
         throw new XMLException("Root node must be a figure");

      BasicSchemaElement e = (BasicSchemaElement) schema.createElement(getFypMLTagForNodeType(nodeType));
      propertiesToXML(e, n);
      switch(nodeType)
      {
         case FIGURE:
         case LABEL:
         case LINE:
         case SHAPE:
         case CALIB:
         case AXIS:
         case CBAR:
         case AXIS3D:
         case LEGEND:
         case GRIDLINE:
         case GRID3D:
         case BACK3D:
         case SYMBOL:
         case EBAR:
         case RASTER:
         case CONTOUR:
         case SURFACE:
         case VIOLIN:
         case SCATLINESTYLE:
            break;
         case GRAPH: 
            GraphNode graph = (GraphNode)n;
            graphicNodeToXML(schema, e, graph.getPrimaryAxis());
            graphicNodeToXML(schema, e, graph.getSecondaryAxis());
            graphicNodeToXML(schema, e, graph.getColorBar());
            graphicNodeToXML(schema, e, graph.getPrimaryGridLines());
            graphicNodeToXML(schema, e, graph.getSecondaryGridLines());
            graphicNodeToXML(schema, e, graph.getLegend());
            break;
         case PGRAPH:
            PolarPlotNode pgraph = (PolarPlotNode)n;
            graphicNodeToXML(schema, e, pgraph.getThetaAxis());
            graphicNodeToXML(schema, e, pgraph.getRadialAxis());
            graphicNodeToXML(schema, e, pgraph.getColorBar());
            graphicNodeToXML(schema, e, pgraph.getLegend());
            break;
         case GRAPH3D:
            Graph3DNode g3 = (Graph3DNode)n;
            graphicNodeToXML(schema, e, g3.getAxis(Graph3DNode.Axis.X));
            graphicNodeToXML(schema, e, g3.getAxis(Graph3DNode.Axis.Y));
            graphicNodeToXML(schema, e, g3.getAxis(Graph3DNode.Axis.Z));
            graphicNodeToXML(schema, e, g3.getGrid3DNode(Graph3DNode.Axis.X));
            graphicNodeToXML(schema, e, g3.getGrid3DNode(Graph3DNode.Axis.Y));
            graphicNodeToXML(schema, e, g3.getGrid3DNode(Graph3DNode.Axis.Z));
            graphicNodeToXML(schema, e, g3.getBackPlane(Graph3DNode.Side.XY));
            graphicNodeToXML(schema, e, g3.getBackPlane(Graph3DNode.Side.XZ));
            graphicNodeToXML(schema, e, g3.getBackPlane(Graph3DNode.Side.YZ));
            graphicNodeToXML(schema, e, g3.getLegend());
            graphicNodeToXML(schema, e, g3.getColorBar());
            break;
         case TEXTBOX:  // the XML text content of a "text box" is mapped to the "title" property in the model node
            e.setTextContent(n.getTitle(), false);
            break;
         case IMAGE:   // the XML text content of this node is the source image in PNG format, base64-encoded binary
            try
            {
               BufferedImage bi = ((ImageNode) n).getImage();
               if(bi == null) 
                  e.setTextContent("", false);
               else
               {
                  int sz = bi.getHeight()*bi.getWidth();
                  ByteArrayOutputStream bos = new ByteArrayOutputStream(sz);
                  OutputStream b64os = Base64.getMimeEncoder().wrap(bos);
                  ImageIO.write(bi, "png", b64os);
                  b64os.close();
                  e.setTextContent(bos.toString(), false);
               }
            }
            catch(IOException ioe)
            {
               throw new XMLException("Unable to embed PNG image as base64-encoded text content");
            }
            break;
         case FUNCTION:
            FunctionNode func = (FunctionNode)n;
            graphicNodeToXML(schema, e, func.getSymbolNode());
            e.setTextContent(func.getFunctionString(), false);
            break;
         case TRACE:
            TraceNode trace = (TraceNode)n;
            graphicNodeToXML(schema, e, trace.getSymbolNode());
            graphicNodeToXML(schema, e, trace.getErrorBarNode());
            break;
         case PAXIS:
            {
               // text content is a comma-separated list of custom polar grid labels
               String content = ((PolarAxisNode) n).getCustomGridLabelsAsString();
               if(!content.isEmpty()) e.setTextContent(content, false);
            }
            break;
         case TICKS:
            {
               // text content is a comma-separated list of custom tick mark labels, if any are defined.
               String content = ((TickSetNode) n).getCustomTickLabelsAsCommaSeparatedList();
               if(!content.isEmpty()) e.setTextContent(content, false);
            }
            break;
         case TICKS3D:
            {
               // text content is a comma-separated list of custom tick mark labels, if any are defined.
               String content = ((Ticks3DNode) n).getCustomTickLabelsAsCommaSeparatedList();
               if(!content.isEmpty()) e.setTextContent(content, false);
            }
            break;
         case BAR:
         case AREA:
         case PIE:
            {
               // these are all "grouped-data" presentation nodes, with a different color and legend label assigned to 
               // each group. The colors and labels are stored in the text content.
               String content = ((FGNPlottableData) n).getDataGroupInfoAsTokenizedString();
               if(!content.isEmpty()) e.setTextContent(content, false);
            }
            break;
         case SCATTER:
            graphicNodeToXML(schema, e, ((ScatterPlotNode)n).getLineStyle());
            break;
         case SCATTER3D:
            graphicNodeToXML(schema, e, ((Scatter3DNode)n).getSymbolNode());
            break;
         case BOX:
            BoxPlotNode box = (BoxPlotNode)n;
            graphicNodeToXML(schema, e, box.getSymbolNode());
            graphicNodeToXML(schema, e, box.getWhiskerNode());
            graphicNodeToXML(schema, e, box.getViolinStyleNode());
            break;
      }
      
      if(parent == null)
         schema.setRootElement(e, false);
      else
         parent.add(e);
      
      for(int i=0; i<n.getChildCount(); i++)
         graphicNodeToXML(schema, e, n.getChildAt(i));
   }
   
   /**
    * This method takes a schema content document and a particular schema element within that document and 
    * <em>recursively</em> constructs the tree of graphic nodes defined by that element and its descendants.
    * 
    * @param schema The schema content document (conforms to the current schema version).
    * @param e The schema element defining a graphic node and it descendants.
    * @param n The graphic node being constructed from XML.
    * @throws XMLException If an exception is thrown, it indicates an error in the conversion code.
    */
   private static void graphicNodeFromXML(ISchema schema, BasicSchemaElement e, FGraphicNode n) throws XMLException
   {
      if(schema == null || e == null || n == null) return;
      String elTag = e.getTag();
      FGNodeType nodeType = n.getNodeType();
      if(getNodeTypeForFypMLTag(n.getParent(), elTag) != nodeType) return;
      
      propertiesFromXML(e, n);
      int nComponents = 0;
      switch(nodeType)
      {
         case FIGURE:
         case LABEL:
         case LINE:
         case SHAPE:
         case CALIB:
         case LEGEND:
         case GRIDLINE:
         case GRID3D:
         case BACK3D:
         case SYMBOL:
         case EBAR:
         case RASTER:
         case CONTOUR:
         case SURFACE:
         case VIOLIN:
         case SCATLINESTYLE:
            break;
         case GRAPH: 
            GraphNode graph = (GraphNode)n;
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(0), graph.getPrimaryAxis());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(1), graph.getSecondaryAxis());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(2), graph.getColorBar());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(3), graph.getPrimaryGridLines());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(4), graph.getSecondaryGridLines());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(5), graph.getLegend());
            nComponents = 6;
            break;
         case PGRAPH:
            PolarPlotNode pgraph = (PolarPlotNode)n;
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(0), pgraph.getThetaAxis());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(1), pgraph.getRadialAxis());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(2), pgraph.getColorBar());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(3), pgraph.getLegend());
            nComponents = 4;
            break;
         case GRAPH3D:
            Graph3DNode g3 = (Graph3DNode)n;
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(0), g3.getAxis(Graph3DNode.Axis.X));
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(1), g3.getAxis(Graph3DNode.Axis.Y));
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(2), g3.getAxis(Graph3DNode.Axis.Z));
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(3), g3.getGrid3DNode(Graph3DNode.Axis.X));
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(4), g3.getGrid3DNode(Graph3DNode.Axis.Y));
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(5), g3.getGrid3DNode(Graph3DNode.Axis.Z));
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(6), g3.getBackPlane(Graph3DNode.Side.XY));
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(7), g3.getBackPlane(Graph3DNode.Side.XZ));
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(8), g3.getBackPlane(Graph3DNode.Side.YZ));
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(9), g3.getLegend());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(10), g3.getColorBar());
            nComponents = 11;
            break;
         case TEXTBOX:  // the XML text content of a "text box" is mapped to the "title" property in the model node
            n.setTitle(e.getTextContent());
            break;
         case IMAGE:   // the XML text content of this node is the source image in PNG format, base64-encoded binary
            ImageNode img = (ImageNode) n;
            try
            {
               // note: if text content is empty, then there is no image defined.
               String b64EncodedImage = e.getTextContent();
               if(!b64EncodedImage.isEmpty())
               {
                  ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getMimeDecoder().decode(b64EncodedImage));
                  img.setImage(ImageIO.read(bis));
                  bis.close();
               }
            }
            catch(IOException ioe)
            {
               throw new XMLException("Unable to extract embedded PNG image from base64-encoded text content");
            }
            
            // because crop rectangle can't be set until a source image is defined, we handle the "crop" property here.
            // If the crop rectangle is not valid for the source image, then it won't be honored.
            if(img.getImageWidth() > 0)
            {
               Rectangle r = rectangleFromString(e.getAttributeValueByName(FGModelSchema.A_CROP));
               if(r != null) img.setCrop(r);
            }
            break;
         case FUNCTION:
            FunctionNode func = (FunctionNode)n;
            func.setFunctionString(e.getTextContent());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(0), func.getSymbolNode());
            nComponents = 1;
            break;
         case TRACE:
            TraceNode trace = (TraceNode)n;
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(0), trace.getSymbolNode());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(1), trace.getErrorBarNode());
            nComponents = 2;
            break;
         case AXIS:
         case CBAR:
         case AXIS3D:
            // ensure the axis initially has no children. The constructors for these node types automatically insert a 
            // single tick set child with default properties.
            FGraphicModel model = n.getGraphicModel();
            if(model != null)
               while(n.getChildCount() > 0) model.deleteNode(n.getChildAt(0));
            break;
         case PAXIS:
            // unlike the other axis node types, the polar axis does not allow any child nodes. In addition, the text
            // content is a comma-separated list of custom polar grid labels (if any).
            ((PolarAxisNode) n).setCustomGridLabelsFromString(e.getTextContent().trim());
            break;
         case TICKS:
            // text content is a comma-separated list of custom tick mark labels (typically empty)
            ((TickSetNode) n).setCustomTickLabelsFromCommaSeparatedList(e.getTextContent().trim());
            break;
         case TICKS3D:
            // text content is a comma-separated list of custom tick mark labels (typically empty)
            ((Ticks3DNode) n).setCustomTickLabelsFromCommaSeparatedList(e.getTextContent().trim());
            break;
         case BAR:
         case AREA:
         case PIE:
            // these are "grouped-data" presentation nodes, with a different color and legend label assigned to each 
            // group. The colors and labels are listed in the text content.
            ((FGNPlottableData) n).setDataGroupInfoFromTokenizedString(e.getTextContent().trim());
            break;
         case SCATTER:
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(0), ((ScatterPlotNode)n).getLineStyle());
            nComponents = 1;
            break;
         case SCATTER3D:
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(0), ((Scatter3DNode)n).getSymbolNode());
            nComponents = 1;
            break;
         case BOX:
            BoxPlotNode box = (BoxPlotNode)n;
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(0), box.getSymbolNode());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(1), box.getWhiskerNode());
            graphicNodeFromXML(schema, (BasicSchemaElement) e.getChildAt(2), box.getViolinStyleNode());
            nComponents = 3;
            break;

      }
      
      FGraphicModel model = n.getGraphicModel();
      assert model != null;
      boolean isFigure = FGModelSchema.EL_FIGURE.equals(elTag);
      for(int i=nComponents; i<e.getChildCount(); i++)
      {
         BasicSchemaElement eChild = (BasicSchemaElement) e.getChildAt(i);
         
         // the ref element is the last child of the figure element in the XML schema; no corres node in the model!
         if(isFigure && FGModelSchema.EL_REF.equals(eChild.getTag()))
            break;

         if(!model.insertNode(n, getNodeTypeForFypMLTag(n, eChild.getTag()), -1))
            throw new XMLException("Conversion from XML failed on element tag = " + eChild.getTag());
         graphicNodeFromXML(schema, eChild, n.getChildAt(n.getChildCount()-1));
      }
   }

   /**
    * Helper method sets the various attributes of an XML schema element IAW the properties of the corresponding 
    * graphic node. If the node has a style property that is implicit (inherited), or another kind of property that 
    * currently is set to a schema-defined default, then the corresponding XML attribute is NOT explicitly set on the 
    * schema element -- UNLESS it is the root figure node, for which all style properties must be explicitly set in 
    * the XML schema.
    * 
    * @param e An XML schema element that defines a <em>DataNav</em> graphic node.
    * @param n The graphic node being converted to an XML schema element
    * @throws XMLException If an exception is thrown, it indicates that there is an error in the conversion code!
    */
   private static void propertiesToXML(BasicSchemaElement e, FGraphicNode n) throws XMLException
   {
      // all style properties: corresponding XML is explicitly set only if the property has been explicitly set. BUT,  
      // for the figure node, we must explicitly set the attribute in the corresponding XML element!
      boolean isFig = (n.getNodeType() == FGNodeType.FIGURE);
      if(isFig || n.isStylePropertyExplicit(FGNProperty.FONTFAMILY)) 
         e.setAttributeValueByName(FGModelSchema.A_FONT, n.getFontFamily());
      if(isFig || n.isStylePropertyExplicit(FGNProperty.PSFONT))
         e.setAttributeValueByName(FGModelSchema.A_PSFONT, n.getPSFont().toString());
      if(isFig || n.isStylePropertyExplicit(FGNProperty.ALTFONT))
         e.setAttributeValueByName(FGModelSchema.A_ALTFONT, n.getAltFont().toString());
      if(isFig || n.isStylePropertyExplicit(FGNProperty.FONTSIZE))
         e.setAttributeValueByName(FGModelSchema.A_FONTSIZE, n.getFontSizeInPoints().toString());
      if(isFig || n.isStylePropertyExplicit(FGNProperty.FONTSTYLE))
         e.setAttributeValueByName(FGModelSchema.A_FONTSTYLE, n.getFontStyle().toString());
      if(isFig || n.isStylePropertyExplicit(FGNProperty.FILLC))
         e.setAttributeValueByName(FGModelSchema.A_FILLCOLOR, BkgFill.colorToHexString(n.getFillColor()));
      if(isFig || n.isStylePropertyExplicit(FGNProperty.STROKEW))
         e.setAttributeValueByName(FGModelSchema.A_STROKEWIDTH, 
               n.getMeasuredStrokeWidth().toString(FGraphicNode.STROKEWCONSTRAINTS));
      if(isFig || n.isStylePropertyExplicit(FGNProperty.STROKECAP))
         e.setAttributeValueByName(FGModelSchema.A_STROKECAP, n.getStrokeEndcap().toString());
      if(isFig || n.isStylePropertyExplicit(FGNProperty.STROKEJOIN))
         e.setAttributeValueByName(FGModelSchema.A_STROKEJOIN, n.getStrokeJoin().toString());
      if(isFig || n.isStylePropertyExplicit(FGNProperty.STROKEC))
         e.setAttributeValueByName(FGModelSchema.A_STROKECOLOR, BkgFill.colorToHexString(n.getStrokeColor()));
      if(isFig || n.isStylePropertyExplicit(FGNProperty.STROKEPATN))
         e.setAttributeValueByName(FGModelSchema.A_STROKEPAT, n.getStrokePattern().toString());

      // selected graphic node types have an optional "id" attribute 
      String id = n.getID();
      if(n.hasID() && !id.equals(FGModelSchema.DEFAULT_OBJ_ID)) 
         e.setAttributeValueByName(FGModelSchema.A_ID, id);
      
      // remaining properties by node type. Note default attribute value handling!!
      Measure.Constraints locCon = FGraphicModel.getLocationConstraints(n.getNodeType());
      Measure.Constraints szCon = FGraphicModel.getSizeConstraints(n.getNodeType());
      Measure.Constraints swCon = FGraphicNode.STROKEWCONSTRAINTS;
      String s;
      switch(n.getNodeType())
      {
         case FIGURE: 
            FigureNode fig = (FigureNode)n;
            s = fig.getBorderWidth().toString(swCon);
            if(!FGModelSchema.DEFAULT_BORDER.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BORDER, s);
            s = fig.getBackgroundFill().toXML();
            if(!FGModelSchema.DEFAULT_FIGURE_BKG.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BKG, s);
            s = fig.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = fig.getNote();
            if(!FGModelSchema.DEFAULT_NOTE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_NOTE, s);
            
            s = fig.isTitleHidden() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_FIGURE_HIDE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HIDE, s);
            s = fromTextAlignmentToXML(true, fig.getTitleHorizontalAlignment());
            if(!FGModelSchema.DEFAULT_FIGURE_HALIGN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HALIGN, s);
            s = fromTextAlignmentToXML(false, fig.getTitleVerticalAlignment());
            if(!FGModelSchema.DEFAULT_FIGURE_VALIGN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_VALIGN, s);
            
            e.setAttributeValueByName(FGModelSchema.A_LOC, n.getX().toString(locCon) + " " + n.getY().toString(locCon));
            e.setAttributeValueByName(FGModelSchema.A_WIDTH, n.getWidth().toString(szCon));
            e.setAttributeValueByName(FGModelSchema.A_HEIGHT, n.getHeight().toString(szCon));
            break;
         case GRAPH: 
            GraphNode g = (GraphNode)n;
            s = Utilities.toString(g.getRotate(),5,2);
            if(!FGModelSchema.DEFAULT_ROTATE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_ROTATE, s);
            s = g.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = g.getCoordSys().toString();
            if(!FGModelSchema.DEFAULT_GRAPH_TYPE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TYPE, s);
            s = g.getLayout().toString();
            if(!FGModelSchema.DEFAULT_LAYOUT.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LAYOUT, s);
            s = g.getClip() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_CLIP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_CLIP, s);
            s = g.getAutorangeAxes().toString();
            if(!FGModelSchema.DEFAULT_AUTORANGE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_AUTORANGE, s);
            s = BkgFill.colorToHexString(g.getBoxColor());
            if(!FGModelSchema.DEFAULT_BOXCOLOR.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BOXCOLOR, s);
            s = g.isTitleHidden() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_GRAPH_HIDE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HIDE, s);
            s = fromTextAlignmentToXML(true, g.getTitleHorizontalAlignment());
            if(!FGModelSchema.DEFAULT_GRAPH_HALIGN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HALIGN, s);
            s = g.getTitleGap().toString(FGNGraph.TITLEGAPCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_GRAPH_GAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_GAP, s);
            
            e.setAttributeValueByName(FGModelSchema.A_LOC, n.getX().toString(locCon) + " " + n.getY().toString(locCon));
            e.setAttributeValueByName(FGModelSchema.A_WIDTH, n.getWidth().toString(szCon));
            e.setAttributeValueByName(FGModelSchema.A_HEIGHT, n.getHeight().toString(szCon));
            break;
         case PGRAPH:
            PolarPlotNode pg = (PolarPlotNode)n;
            s = pg.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = pg.getGridOnTop() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_GRIDONTOP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_GRIDONTOP, s);
            s = pg.getClip() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_PGRAPH_CLIP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_CLIP, s);
            s = BkgFill.colorToHexString(pg.getGridBkgColor());
            if(!FGModelSchema.DEFAULT_BOXCOLOR.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BOXCOLOR, s);
            s = pg.isTitleHidden() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_GRAPH_HIDE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HIDE, s);
            s = fromTextAlignmentToXML(true, pg.getTitleHorizontalAlignment());
            if(!FGModelSchema.DEFAULT_GRAPH_HALIGN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HALIGN, s);
            s = pg.getTitleGap().toString(FGNGraph.TITLEGAPCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_GRAPH_GAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_GAP, s);

            e.setAttributeValueByName(FGModelSchema.A_LOC, n.getX().toString(locCon) + " " + n.getY().toString(locCon));
            e.setAttributeValueByName(FGModelSchema.A_WIDTH, n.getWidth().toString(szCon));
            e.setAttributeValueByName(FGModelSchema.A_HEIGHT, n.getHeight().toString(szCon));
            break;
         case GRAPH3D:
            Graph3DNode g3 = (Graph3DNode)n;
            s = Utilities.toString(g3.getRotate(), 5,2);
            if(!FGModelSchema.DEFAULT_GRAPH3D_ROTATE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_ROTATE, s);
            s = Utilities.toString(g3.getElevate(), 5,2);
            if(!FGModelSchema.DEFAULT_ELEVATE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_ELEVATE, s);
            s = g3.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = g3.getBackDrop().toString();
            if(!FGModelSchema.DEFAULT_BACKDROP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BACKDROP, s);
            s = Integer.toString(g3.getProjectionDistanceScale());
            if(!FGModelSchema.DEFAULT_PSCALE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_PSCALE, s);
            s = g3.isTitleHidden() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_GRAPH_HIDE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HIDE, s);
            s = fromTextAlignmentToXML(true, g3.getTitleHorizontalAlignment());
            if(!FGModelSchema.DEFAULT_GRAPH_HALIGN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HALIGN, s);
            s = g3.getTitleGap().toString(FGNGraph.TITLEGAPCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_GRAPH_GAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_GAP, s);

            e.setAttributeValueByName(FGModelSchema.A_LOC, g3.getX().toString(locCon) + " " + g3.getY().toString(locCon));
            e.setAttributeValueByName(FGModelSchema.A_WIDTH, g3.getWidth().toString(szCon));
            e.setAttributeValueByName(FGModelSchema.A_HEIGHT, g3.getHeight().toString(szCon));
            e.setAttributeValueByName(FGModelSchema.A_DEPTH, g3.getDepth().toString(szCon));
            break;
         case TEXTBOX:
            TextBoxNode tbox = (TextBoxNode)n;
            s = Utilities.toString(n.getRotate(), 5,2);
            if(!FGModelSchema.DEFAULT_ROTATE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_ROTATE, s);
            
            s = fromTextAlignmentToXML(true, tbox.getHorizontalAlignment());
            if(!FGModelSchema.DEFAULT_TEXTBOX_HALIGN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HALIGN, s); 
            s = fromTextAlignmentToXML(false, tbox.getVerticalAlignment());
            if(!FGModelSchema.DEFAULT_TEXTBOX_VALIGN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_VALIGN, s);
            s = tbox.getClip() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_CLIP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_CLIP, s);
            s = tbox.getMargin().toString(swCon);
            if(!FGModelSchema.DEFAULT_TEXTBOX_GAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_GAP, s);
            s = tbox.getBackgroundFill().toXML();
            if(!FGModelSchema.DEFAULT_TEXTBOX_BKG.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BKG, s);

            s = Utilities.toString(tbox.getLineHeight(),3,2);
            if(!FGModelSchema.DEFAULT_LINEHT.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LINEHT, s);
            
            e.setAttributeValueByName(FGModelSchema.A_LOC, n.getX().toString(locCon) + " " + n.getY().toString(locCon));
            e.setAttributeValueByName(FGModelSchema.A_WIDTH, n.getWidth().toString(szCon));
            e.setAttributeValueByName(FGModelSchema.A_HEIGHT, n.getHeight().toString(szCon));
            break;
         case IMAGE:
            ImageNode img = (ImageNode)n;
            s = Utilities.toString(img.getRotate(), 5,2);
            if(!FGModelSchema.DEFAULT_ROTATE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_ROTATE, s);
            
            s = img.getMargin().toString(swCon);
            if(!FGModelSchema.DEFAULT_IMAGE_GAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_GAP, s);
            s = img.getBackgroundFill().toXML();
            if(!FGModelSchema.DEFAULT_IMAGE_BKG.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BKG, s);

            s = rectangleToString(img.getCrop());
            if(s != null) e.setAttributeValueByName(FGModelSchema.A_CROP, s);
            
            e.setAttributeValueByName(FGModelSchema.A_LOC, n.getX().toString(locCon) + " " + n.getY().toString(locCon));
            e.setAttributeValueByName(FGModelSchema.A_WIDTH, n.getWidth().toString(szCon));
            e.setAttributeValueByName(FGModelSchema.A_HEIGHT, n.getHeight().toString(szCon));
            break;
         case LABEL:
            LabelNode label = (LabelNode)n;
            s = Utilities.toString(n.getRotate(), 5,2);
            if(!FGModelSchema.DEFAULT_ROTATE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_ROTATE, s);
            
            s = fromTextAlignmentToXML(true, label.getHorizontalAlignment());
            if(!FGModelSchema.DEFAULT_LABEL_HALIGN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HALIGN, s);
            s = fromTextAlignmentToXML(false, label.getVerticalAlignment());
            if(!FGModelSchema.DEFAULT_LABEL_VALIGN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_VALIGN, s);

            e.setAttributeValueByName(FGModelSchema.A_LOC, n.getX().toString(locCon) + " " + n.getY().toString(locCon));
            e.setAttributeValueByName(FGModelSchema.A_TITLE, n.getTitle());
            break;
         case LINE:
            LineNode line = (LineNode)n;
            e.setAttributeValueByName(FGModelSchema.A_P0, line.getX().toString(locCon) + " " + line.getY().toString(locCon));
            e.setAttributeValueByName(FGModelSchema.A_P1, line.getX2().toString(locCon) + " " + line.getY2().toString(locCon));
            break;
         case SHAPE:
            ShapeNode shape = (ShapeNode)n;
            s = Utilities.toString(n.getRotate(), 5,2);
            if(!FGModelSchema.DEFAULT_ROTATE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_ROTATE, s);
            s = shape.getType().toString();
            if(!FGModelSchema.DEFAULT_SYMBOL.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TYPE, s);
            s = shape.getWidth().toString(szCon);
            if(!FGModelSchema.DEFAULT_SHAPE_DIM.equals(s)) e.setAttributeValueByName(FGModelSchema.A_WIDTH, s);
            s = shape.getHeight().toString(szCon);
            if(!FGModelSchema.DEFAULT_SHAPE_DIM.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HEIGHT, s);
            s = shape.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = shape.getBackgroundFill().toXML();
            if(!FGModelSchema.DEFAULT_SHAPE_BKG.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BKG, s);

            e.setAttributeValueByName(FGModelSchema.A_LOC, n.getX().toString(locCon) + " " + n.getY().toString(locCon));
            break;
         case CALIB:
            CalibrationBarNode calib = (CalibrationBarNode)n;
            s = (calib.isPrimary()) ? "true" : "false";
            if(!FGModelSchema.DEFAULT_PRIMARY.equals(s)) e.setAttributeValueByName(FGModelSchema.A_PRIMARY, s);
            s = (calib.isAutoLabelOn()) ? "true" : "false";
            if(!FGModelSchema.DEFAULT_AUTO.equals(s)) e.setAttributeValueByName(FGModelSchema.A_AUTO, s);
            s = calib.getEndCap().toString();
            if(!FGModelSchema.DEFAULT_CALIB_CAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_CAP, s);
            s = calib.getEndCapSize().toString(CalibrationBarNode.ENDCAPSIZECONSTRAINTS);
            if(!FGModelSchema.DEFAULT_CAPSIZE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_CAPSIZE, s);
            e.setAttributeValueByName(FGModelSchema.A_LEN, Utilities.toString(calib.getLength(),7,3));
            e.setAttributeValueByName(FGModelSchema.A_LOC, n.getX().toString(locCon) + " " + n.getY().toString(locCon));
            break;
         case FUNCTION:
            FunctionNode func = (FunctionNode)n;
            s = func.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = func.getShowInLegend() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LEGEND.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEGEND, s);
            s = Utilities.toString(func.getX0(), 7,3);
            if(!FGModelSchema.DEFAULT_X0.equals(s)) e.setAttributeValueByName(FGModelSchema.A_X0, s);
            s = Utilities.toString(func.getX1(), 7,3);
            if(!FGModelSchema.DEFAULT_X1.equals(s)) e.setAttributeValueByName(FGModelSchema.A_X1, s);
            s = Utilities.toString(func.getDX(), 7,3);
            if(!FGModelSchema.DEFAULT_DX.equals(s)) e.setAttributeValueByName(FGModelSchema.A_DX, s);
            break;
         case TRACE:
            TraceNode trace = (TraceNode)n;
            s = trace.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = trace.getShowInLegend() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LEGEND.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEGEND, s);
            s = Integer.toString(trace.getSkip());
            if(!FGModelSchema.DEFAULT_SKIP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SKIP, s);
            s = trace.getMode().toString();
            if(!FGModelSchema.DEFAULT_MODE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_MODE, s);
            s = Utilities.toString(trace.getBarWidth(), 7,3);
            if(!FGModelSchema.DEFAULT_BARWIDTH.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BARWIDTH, s);
            s = Utilities.toString(trace.getBaseline(), 7,3);
            if(!FGModelSchema.DEFAULT_BASELINE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BASELINE, s);
            s = Utilities.toString(trace.getXOffset(), 7,3);
            if(!FGModelSchema.DEFAULT_XYOFF.equals(s)) e.setAttributeValueByName(FGModelSchema.A_XOFF, s);
            s = Utilities.toString(trace.getYOffset(), 7,3);
            if(!FGModelSchema.DEFAULT_XYOFF.equals(s)) e.setAttributeValueByName(FGModelSchema.A_YOFF, s);
            s = trace.getShowAverage() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_TRACE_AVG.equals(s)) e.setAttributeValueByName(FGModelSchema.A_AVG, s);
            s = Integer.toString(trace.getSlidingWindowLength());
            if(!FGModelSchema.DEFAULT_TRACE_LEN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEN, s);
            
            e.setAttributeValueByName(FGModelSchema.A_SRC, "#" + trace.getDataSetID());
            break;
         case RASTER:
            RasterNode raster = (RasterNode)n;
            s = raster.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = raster.getShowInLegend() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LEGEND.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEGEND, s);
            s = raster.getMode().toString();
            if(!FGModelSchema.DEFAULT_RASTER_MODE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_MODE, s);
            s = Utilities.toString(raster.getBaseline(), 7,3);
            if(!FGModelSchema.DEFAULT_BASELINE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BASELINE, s);
            s = Utilities.toString(raster.getXOffset(), 7,3);
            if(!FGModelSchema.DEFAULT_XYOFF.equals(s)) e.setAttributeValueByName(FGModelSchema.A_XOFF, s);
            s = Integer.toString(raster.getLineHeight());
            if(!FGModelSchema.DEFAULT_RASTER_HEIGHT.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HEIGHT, s);
            s = Integer.toString(raster.getLineSpacer());
            if(!FGModelSchema.DEFAULT_RASTER_SPACER.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SPACER, s);
            s = Integer.toString(raster.getNumBins());
            if(!FGModelSchema.DEFAULT_NBINS.equals(s)) e.setAttributeValueByName(FGModelSchema.A_NBINS, s);
            s = raster.getAveraged() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_RASTER_AVG.equals(s)) e.setAttributeValueByName(FGModelSchema.A_AVG, s);
            s = Utilities.toString(raster.getHistogramRangeStart(), 7,3);
            if(!FGModelSchema.DEFAULT_RASTER_STARTEND.equals(s)) e.setAttributeValueByName(FGModelSchema.A_START, s);
            s = Utilities.toString(raster.getHistogramRangeEnd(), 7,3);
            if(!FGModelSchema.DEFAULT_RASTER_STARTEND.equals(s)) e.setAttributeValueByName(FGModelSchema.A_END, s);

            e.setAttributeValueByName(FGModelSchema.A_SRC, "#" + raster.getDataSetID());
            break;
         case CONTOUR:
            ContourNode contour = (ContourNode)n;
            s = contour.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = contour.isSmoothed() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_HEATMAP_SMOOTH.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SMOOTH, s);
            s = contour.getMode().toString();
            if(!FGModelSchema.DEFAULT_CONTOUR_MODE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_MODE, s);
            s = contour.getLevelList();
            if(!FGModelSchema.DEFAULT_LEVELS.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEVELS, s);
            
            e.setAttributeValueByName(FGModelSchema.A_SRC, "#" + contour.getDataSetID());
            break;
         case SURFACE:
            SurfaceNode surf = (SurfaceNode)n;
            s = surf.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = surf.isColorMapped() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_SURFACE_CMAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_CMAP, s);
            s = Integer.toString(surf.getMeshLimit());
            if(!FGModelSchema.DEFAULT_LIMIT.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LIMIT, s);

            e.setAttributeValueByName(FGModelSchema.A_SRC, "#" + surf.getDataSetID());
            break;
         case BAR:
            BarPlotNode bp = (BarPlotNode)n;
            s = bp.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = bp.getMode().toString();
            if(!FGModelSchema.DEFAULT_BAR_MODE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_MODE, s);
            s = Utilities.toString(bp.getBaseline(), 7,3);
            if(!FGModelSchema.DEFAULT_BASELINE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BASELINE, s);
            s = Integer.toString(bp.getBarWidth());
            if(!FGModelSchema.DEFAULT_BAR_BARWIDTH.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BARWIDTH, s);
            s = bp.getShowInLegend() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LEGEND.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEGEND, s);
            s = bp.isAutoLabelOn() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_BAR_AUTO.equals(s)) e.setAttributeValueByName(FGModelSchema.A_AUTO, s);
            
            e.setAttributeValueByName(FGModelSchema.A_SRC, "#" + bp.getDataSetID());
            break;
         case BOX:
            BoxPlotNode box = (BoxPlotNode)n;
            s = box.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = box.getMode().toString();
            if(!FGModelSchema.DEFAULT_BOX_MODE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_MODE, s);
            s = box.getBoxWidth().toString(BoxPlotNode.BOXWIDTHCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_BOX_BARWIDTH.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BARWIDTH, s);
            s = Utilities.toString(box.getOffset(), 7,3);
            if(!FGModelSchema.DEFAULT_X0.equals(s)) e.setAttributeValueByName(FGModelSchema.A_X0, s);
            s = Utilities.toString(box.getInterval(), 7,3);
            if(!FGModelSchema.DEFAULT_DX.equals(s)) e.setAttributeValueByName(FGModelSchema.A_DX, s);
            s = box.getShowInLegend() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LEGEND.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEGEND, s);
            
            e.setAttributeValueByName(FGModelSchema.A_SRC, "#" + box.getDataSetID());
            break;
         case VIOLIN:
            ViolinStyleNode violin = (ViolinStyleNode)n;
            s = violin.getSize().toString(BoxPlotNode.BOXWIDTHCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_VIOLIN_SIZE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SIZE, s);
            break;
         case SCATTER:
            ScatterPlotNode spn = (ScatterPlotNode)n;
            s = spn.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = spn.getShowInLegend() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LEGEND.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEGEND, s);
            s = spn.getMode().toString();
            if(!FGModelSchema.DEFAULT_SCATTER_MODE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_MODE, s);
            s = spn.getSymbol().toString();
            if(!FGModelSchema.DEFAULT_SCATTER_TYPE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TYPE, s);
            s = spn.getMaxSymbolSize().toString(ScatterPlotNode.MAXSYMSIZECONSTRAINTS);
            if(!FGModelSchema.DEFAULT_SCATTER_SIZE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SIZE, s);
            s = spn.getMinSymbolSize().toString(ScatterPlotNode.MAXSYMSIZECONSTRAINTS);
            if(!FGModelSchema.DEFAULT_MINSIZE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_MINSIZE, s);

            e.setAttributeValueByName(FGModelSchema.A_SRC, "#" + spn.getDataSetID());
            break;
         case SCATTER3D:
            Scatter3DNode sp3 = (Scatter3DNode)n;
            s = sp3.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = sp3.getShowInLegend() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LEGEND.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEGEND, s);
            s = sp3.getMode().toString();
            if(!FGModelSchema.DEFAULT_SCATTER_MODE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_MODE, s);
            s = sp3.getStemmed() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_STEMMED.equals(s)) e.setAttributeValueByName(FGModelSchema.A_STEMMED, s);
            s = Utilities.toString(sp3.getZBase(), 7,3);
            if(!FGModelSchema.DEFAULT_BASELINE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BASELINE, s);
            s = Integer.toString(sp3.getBarSize());
            if(!FGModelSchema.DEFAULT_SCAT3D_BARWIDTH.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BARWIDTH, s);
            s = sp3.getBackgroundFill().toXML();
            if(!FGModelSchema.DEFAULT_SHAPE_BKG.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BKG, s);
            s = sp3.getProjectionDotSizesAsTokenList();
            if(!FGModelSchema.DEFAULT_DOTSIZE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_DOTSIZE, s);
            s = sp3.getProjectionDotColorsAsTokenList();
            if(!FGModelSchema.DEFAULT_DOTCOLOR.equals(s)) e.setAttributeValueByName(FGModelSchema.A_DOTCOLOR, s);
            s = sp3.getMinSymbolSize().toString(SymbolNode.SYMBOLSIZECONSTRAINTS);
            if(!FGModelSchema.DEFAULT_MINSIZE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_MINSIZE, s);

            e.setAttributeValueByName(FGModelSchema.A_SRC, "#" + sp3.getDataSetID());
            break;
         case AREA:
            AreaChartNode ac = (AreaChartNode)n;
            s = ac.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = Utilities.toString(ac.getBaseline(), 7,3);
            if(!FGModelSchema.DEFAULT_BASELINE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BASELINE, s);
            s = ac.getShowInLegend() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LEGEND.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEGEND, s);
            s = ac.getLabelMode().toString();
            if(!FGModelSchema.DEFAULT_AREA_MODE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_MODE, s);
            
            e.setAttributeValueByName(FGModelSchema.A_SRC, "#" + ac.getDataSetID());
            break;
         case PIE:
            PieChartNode pie = (PieChartNode)n;
            s = pie.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = pie.getShowInLegend() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LEGEND.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEGEND, s);
            s = Integer.toString(pie.getRadialOffset());
            if(!FGModelSchema.DEFAULT_PIE_GAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_GAP, s);
            s = Integer.toString(pie.getDisplacedBits());
            if(!FGModelSchema.DEFAULT_PIE_DISPLACE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_DISPLACE, s);
            s = pie.getSliceLabelMode().toString();
            if(!FGModelSchema.DEFAULT_PIE_MODE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_MODE, s);
            
            e.setAttributeValueByName(FGModelSchema.A_START, Utilities.toString(pie.getInnerRadius(), 7,3));
            e.setAttributeValueByName(FGModelSchema.A_END, Utilities.toString(pie.getOuterRadius(), 7,3));
            e.setAttributeValueByName(FGModelSchema.A_SRC, "#" + pie.getDataSetID());
            break;
         case AXIS:
            AxisNode axis = (AxisNode)n;
            s = axis.getHide() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_AXIS_HIDE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HIDE, s);
            s = axis.getLog2() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LOG2.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LOG2, s);
            s = axis.getUnits();
            if(!FGModelSchema.DEFAULT_UNITS.equals(s)) e.setAttributeValueByName(FGModelSchema.A_UNITS, s);
            s = axis.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = axis.getSpacer().toString(AxisNode.SPACERCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_AXIS_SPACER.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SPACER, s);
            s = axis.getLabelOffset().toString(AxisNode.SPACERCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_LABELOFFSET.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LABELOFFSET, s);
            s = Utilities.toString(axis.getLineHeight(), 3,2);
            if(!FGModelSchema.DEFAULT_LINEHT.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LINEHT, s);
            s = Integer.toString(axis.getPowerScale());
            if(!FGModelSchema.DEFAULT_SCALE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SCALE, s);

            e.setAttributeValueByName(FGModelSchema.A_START, Utilities.toString(axis.getStart(), 7,3));
            e.setAttributeValueByName(FGModelSchema.A_END, Utilities.toString(axis.getEnd(), 7,3));
            break;
         case CBAR:
            ColorBarNode cbar = (ColorBarNode)n;
            s = cbar.getHide() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_COLORBAR_HIDE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HIDE, s);
            s = cbar.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = cbar.getBarAxisSpacer().toString(ColorBarNode.GAPCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_AXIS_SPACER.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SPACER, s);
            s = cbar.getLabelOffset().toString(ColorBarNode.GAPCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_LABELOFFSET.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LABELOFFSET, s);
            s = cbar.getGap().toString(ColorBarNode.GAPCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_COLORBAR_GAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_GAP, s);
            s = cbar.getBarSize().toString(ColorBarNode.GAPCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_COLORBAR_SIZE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SIZE, s);
            s = ColorMap.asString(cbar.getColorMap());
            if(!FGModelSchema.DEFAULT_COLORBAR_CMAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_CMAP, s);
            s = cbar.isReversed() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_REVERSE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_REVERSE, s);
            s = cbar.getColorMapMode().toString();
            if(!FGModelSchema.DEFAULT_COLORBAR_CMODE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_CMODE, s);
            s = cbar.getEdge().toString();
            if(!FGModelSchema.DEFAULT_EDGE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_EDGE, s);
            s = BkgFill.colorToHexString(cbar.getColorNaN());
            if(!FGModelSchema.DEFAULT_COLORBAR_CMAPNAN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_CMAPNAN, s);
            s = Utilities.toString(cbar.getLineHeight(), 3,2);
            if(!FGModelSchema.DEFAULT_LINEHT.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LINEHT, s);
            s = Integer.toString(cbar.getPowerScale());
            if(!FGModelSchema.DEFAULT_SCALE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SCALE, s);

            e.setAttributeValueByName(FGModelSchema.A_START, Utilities.toString(cbar.getStart(), 7,3));
            e.setAttributeValueByName(FGModelSchema.A_END, Utilities.toString(cbar.getEnd(), 7,3));
            break;
         case PAXIS:
            PolarAxisNode paxis = (PolarAxisNode)n;
            s = paxis.getHide() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_AXIS_HIDE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HIDE, s);
            s = paxis.getReversed() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_REVERSE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_REVERSE, s);
            s = paxis.getGridLabelGap().toString(AxisNode.SPACERCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_PAXIS_GAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_GAP, s);
            s = paxis.getGridLabelFormat().toString();
            if(!FGModelSchema.DEFAULT_TICK_FMT.equals(s)) e.setAttributeValueByName(FGModelSchema.A_FMT, s);
            s = Integer.toString(paxis.getReferenceAngle());
            if(!FGModelSchema.DEFAULT_REFANGLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_REFANGLE, s);
            
            e.setAttributeValueByName(FGModelSchema.A_START, Utilities.toString(paxis.getRangeMin(), 7,3));
            e.setAttributeValueByName(FGModelSchema.A_END, Utilities.toString(paxis.getRangeMax(), 7,3));
            e.setAttributeValueByName(FGModelSchema.A_PDIVS, paxis.getGridDivisionsAsString(true));
            break;
         case AXIS3D:
            Axis3DNode a3 = (Axis3DNode)n;
            s = a3.getHide() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_AXIS_HIDE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HIDE, s);
            s = a3.isLogBase2() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LOG2.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LOG2, s);
            s = a3.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            s = a3.getSpacer().toString(AxisNode.SPACERCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_AXIS_SPACER.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SPACER, s);
            s = a3.getLabelOffset().toString(AxisNode.SPACERCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_LABELOFFSET.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LABELOFFSET, s);
            s = Utilities.toString(a3.getLineHeight(), 3,2);
            if(!FGModelSchema.DEFAULT_LINEHT.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LINEHT, s);
            s = Integer.toString(a3.getPowerScale());
            if(!FGModelSchema.DEFAULT_SCALE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SCALE, s);

            s = a3.isLogarithmic() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LOG.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LOG, s);
            e.setAttributeValueByName(FGModelSchema.A_START, Utilities.toString(a3.getRangeMin(), 7,3));
            e.setAttributeValueByName(FGModelSchema.A_END, Utilities.toString(a3.getRangeMax(), 7,3));
            break;
         case TICKS:
            TickSetNode ticks = (TickSetNode)n;
            s = ticks.getDecadeTicks().toString();
            if(!FGModelSchema.DEFAULT_PERLOGINTV.equals(s)) e.setAttributeValueByName(FGModelSchema.A_PERLOGINTV, s);
            s = ticks.getTickOrientation().toString();
            if(!FGModelSchema.DEFAULT_TICK_DIR.equals(s)) e.setAttributeValueByName(FGModelSchema.A_DIR, s);
            s = ticks.getTickLabelFormat().toString();
            if(!FGModelSchema.DEFAULT_TICK_FMT.equals(s)) e.setAttributeValueByName(FGModelSchema.A_FMT, s);
            s = ticks.getTickLength().toString(TickSetNode.TICKLENCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_TICK_LEN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEN, s);
            s = ticks.getTickGap().toString(TickSetNode.TICKLENCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_TICK_GAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_GAP, s);
            if(!ticks.isTrackingParentAxis())
            {
               e.setAttributeValueByName(FGModelSchema.A_START, Utilities.toString(ticks.getStart(), 7,3));
               e.setAttributeValueByName(FGModelSchema.A_END, Utilities.toString(ticks.getEnd(), 7,3));
            }
            e.setAttributeValueByName(FGModelSchema.A_INTV, Utilities.toString(ticks.getInterval(), 7,3));
            break;
         case TICKS3D:
            Ticks3DNode t3 = (Ticks3DNode)n;
            s = t3.getDecadeTicks().toString();
            if(!FGModelSchema.DEFAULT_PERLOGINTV.equals(s)) e.setAttributeValueByName(FGModelSchema.A_PERLOGINTV, s);
            s = t3.getTickOrientation().toString();
            if(!FGModelSchema.DEFAULT_TICK_DIR.equals(s)) e.setAttributeValueByName(FGModelSchema.A_DIR, s);
            s = t3.getTickLabelFormat().toString();
            if(!FGModelSchema.DEFAULT_TICK_FMT.equals(s)) e.setAttributeValueByName(FGModelSchema.A_FMT, s);
            s = t3.getTickLength().toString(TickSetNode.TICKLENCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_TICK_LEN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEN, s);
            s = t3.getTickGap().toString(TickSetNode.TICKLENCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_TICK_GAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_GAP, s);
            
            // unlike the 2D tick set, the 3D tick set has no "track parent axis" feature
            e.setAttributeValueByName(FGModelSchema.A_START, Utilities.toString(t3.getStart(), 7,3));
            e.setAttributeValueByName(FGModelSchema.A_END, Utilities.toString(t3.getEnd(), 7,3));
            e.setAttributeValueByName(FGModelSchema.A_INTV, Utilities.toString(t3.getInterval(), 7,3));
            break;
         case LEGEND:
            LegendNode legend = (LegendNode)n;
            s = Utilities.toString(n.getRotate(), 5,2);
            if(!FGModelSchema.DEFAULT_ROTATE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_ROTATE, s);
            s = legend.getSpacer().toString(LegendNode.LENCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_LEGEND_SPACER.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SPACER, s);
            s = legend.getLabelOffset().toString(LegendNode.LENCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_LEGEND_LABELOFFSET.equals(s)) 
               e.setAttributeValueByName(FGModelSchema.A_LABELOFFSET, s);
            s = legend.getSymbolSize().toString(LegendNode.LENCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_LEGEND_SIZE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SIZE, s);
            s = legend.getLength().toString(LegendNode.LENCONSTRAINTS);
            if(!FGModelSchema.DEFAULT_LEGEND_LEN.equals(s)) e.setAttributeValueByName(FGModelSchema.A_LEN, s);
            s = legend.getMid() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_MID.equals(s)) e.setAttributeValueByName(FGModelSchema.A_MID, s);
            s = legend.getHide() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_LEGEND_HIDE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HIDE, s);
            s = BkgFill.colorToHexString(legend.getBoxColor());
            if(!FGModelSchema.DEFAULT_BOXCOLOR.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BOXCOLOR, s);
            s = legend.getBorderWidth().toString(swCon);
            if(!FGModelSchema.DEFAULT_BORDER.equals(s)) e.setAttributeValueByName(FGModelSchema.A_BORDER, s);

            e.setAttributeValueByName(FGModelSchema.A_LOC, n.getX().toString(locCon) + " " + n.getY().toString(locCon));
            break;
         case GRIDLINE:
            s = ((GridLineNode)n).getHide() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_GRIDLINE_HIDE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HIDE, s);
            break;
         case GRID3D:
         case BACK3D:
         case SCATLINESTYLE:
            // these nodes have only basic style properties. Nothing to do here.
            break;
         case SYMBOL:
            SymbolNode symbol = (SymbolNode)n;
            s = symbol.getType().toString();
            if(!FGModelSchema.DEFAULT_SYMBOL.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TYPE, s);
            s = symbol.getSize().toString(SymbolNode.SYMBOLSIZECONSTRAINTS);
            if(!FGModelSchema.DEFAULT_SYMBOL_SIZE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_SIZE, s);
            s = symbol.getTitle();
            if(!FGModelSchema.DEFAULT_TITLE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_TITLE, s);
            break;
         case EBAR:
            ErrorBarNode ebar = (ErrorBarNode)n;
            s = ebar.getEndCap().toString();
            if(!FGModelSchema.DEFAULT_EBAR_CAP.equals(s)) e.setAttributeValueByName(FGModelSchema.A_CAP, s);
            s = ebar.getEndCapSize().toString(ErrorBarNode.EBARCAPSIZECONSTRAINTS);
            if(!FGModelSchema.DEFAULT_CAPSIZE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_CAPSIZE, s);
            s = ebar.getHide() ? "true" : "false";
            if(!FGModelSchema.DEFAULT_EBAR_HIDE.equals(s)) e.setAttributeValueByName(FGModelSchema.A_HIDE, s);
            break;
      }
   }
   
   /**
    * Helper method converts a rectangle to string form: "x y w h", where (x,y) is the coordinates of the top-left
    * corner, w is the width, and h is the height of the rectangle.
    * @param r The rectangle. Note that the corner coordinates and dimensions are all integers.
    * @return Rectangle as a string. Returns null if argument is null.
    */
   private static String rectangleToString(Rectangle r)
   {
      return(r==null ? null : r.x + " " + r.y + " " + r.width + " " + r.height);
   }
   
   /**
    * The inverse of {@link #rectangleToString(Rectangle)}.
    * @param strR A string in the form "x y w h", where each token is an integer in string form. Tokens x and y must be
    * non-negative, while w and h must be strictly positive. If string is null, or if it doesn't conform to this format,
    * method returns null. 
    * @return The rectangle represented by the specified string, or null if string is invalid.
    */
   private static Rectangle rectangleFromString(String strR)
   {
      if(strR == null) return(null);
      StringTokenizer tokenizer = new StringTokenizer(strR);
      if(tokenizer.countTokens() != 4) return(null);

      try
      {
         int x = Integer.parseInt(tokenizer.nextToken());
         int y = Integer.parseInt(tokenizer.nextToken());
         int w = Integer.parseInt(tokenizer.nextToken());
         int h = Integer.parseInt(tokenizer.nextToken());
         return((x>=0) && (y>=0) && (w>0) && (h>0) ? new Rectangle(x, y, w, h) : null);
      }
      catch(NumberFormatException nfe) { return(null); }
   }

   /**
    * Helper method maps the horizontal or vertical text alignment to the correct XML string attribute value. 
    * @param horiz True for horizontal alignment, false for vertical.
    * @param ta The node's current alignment in the graphic model.
    * @return The corresponding XML string attribute value. 
    */
   private static String fromTextAlignmentToXML(boolean horiz, TextAlign ta)
   {
      String xmlAlign = null;
      switch(ta)
      {
      case LEADING : xmlAlign = horiz ? FGModelSchema.HALIGN_LEFT : FGModelSchema.VALIGN_TOP; break;
      case TRAILING: xmlAlign = horiz ? FGModelSchema.HALIGN_RIGHT : FGModelSchema.VALIGN_BOTTOM; break;
      case CENTERED: xmlAlign = horiz ? FGModelSchema.HALIGN_CENTER : FGModelSchema.VALIGN_CENTER; break;
      }
      return(xmlAlign);
   }
   
   /**
    * The inverse of {@link #fromTextAlignmentToXML}.
    * @param horiz True for horizontal alignment, false for vertical.
    * @param xmlAlign The alignment as persisted in the XML schema representation of the node.
    * @return The corresponding text alignment in the graphic model.
    */
   private static TextAlign fromXMLToTextAlignment(boolean horiz, String xmlAlign)
   {
      TextAlign ta;
      if(xmlAlign.equals(FGModelSchema.HALIGN_RIGHT) || xmlAlign.equals(FGModelSchema.VALIGN_BOTTOM))
         ta = TextAlign.TRAILING;
      else if(xmlAlign.equals(FGModelSchema.HALIGN_LEFT) || xmlAlign.equals(FGModelSchema.VALIGN_TOP))
         ta = TextAlign.LEADING;
      else
         ta = TextAlign.CENTERED;
      
      return(ta);
   }
   
   /**
    * Helper method sets the various properties of the <em>DataNav</em> graphic node IAW the attributes of the XML 
    * schema element that defines it. If a style-related attribute has not been explicitly set on the schema element, 
    * then the corresponding property is left implicit on the graphic node. If any other attribute has not been 
    * explicitly set on the schema element, then the corresponding graphic node property is set to its schema-defined 
    * default value.
    * 
    * <p>The method handles the excruciating details of converting XML attribute values -- which are just strings -- to 
    * the various property data types employed throughout the <em>DataNav</em> graphic model. Also, measured properties 
    * (like stroke width, location coordinates, etc) are constrained by the appropriate constraints object (measurement 
    * constraints were introduced in schema version# 7).</p>
    * 
    * @param e An XML schema element that defines a <em>DataNav</em> graphic node.
    * @param n The graphic node being constructed from an XML schema element
    * @throws XMLException If an exception is thrown, it indicates that there is an error in the conversion code!
    */
   private static void propertiesFromXML(BasicSchemaElement e, FGraphicNode n) throws XMLException
   {
      // all style properties... 
      // NOTE: if the attribute value is implicit, then it is inherited from its parent. Since the node may have been
      // initialized otherwise, we must be sure to "reset" the property as implicit. This is done by passing null to
      // the relevant setter method
      String s;
      if(e.hasAttribute(FGModelSchema.A_FONT))
         n.setFontFamily(e.getAttributeValueByName(FGModelSchema.A_FONT));
      if(e.hasAttribute(FGModelSchema.A_PSFONT))
      {
         s = e.getAttributeValueByName(FGModelSchema.A_PSFONT);
         n.setPSFont(Utilities.getEnumValueFromString(s, PSFont.values()));
      }
      if(e.hasAttribute(FGModelSchema.A_ALTFONT))
      {
         s = e.getAttributeValueByName(FGModelSchema.A_ALTFONT);
         n.setAltFont(Utilities.getEnumValueFromString(s, GenericFont.values()));
      }
      if(e.hasAttribute(FGModelSchema.A_FONTSIZE))
      {
         s = e.getAttributeValueByName(FGModelSchema.A_FONTSIZE);
         n.setFontSizeInPoints(s == null ? null : Integer.parseInt(s));
      }
      if(e.hasAttribute(FGModelSchema.A_FONTSTYLE))
      {
         s = e.getAttributeValueByName(FGModelSchema.A_FONTSTYLE);
         n.setFontStyle(Utilities.getEnumValueFromString(s, FontStyle.values()));
      }
      if(e.hasAttribute(FGModelSchema.A_FILLCOLOR))
      {
         s = e.getAttributeValueByName(FGModelSchema.A_FILLCOLOR);
         n.setFillColor(s == null ? null : BkgFill.colorFromHexString(s));
      }
      if(e.hasAttribute(FGModelSchema.A_STROKEWIDTH))
      {
         s = e.getAttributeValueByName(FGModelSchema.A_STROKEWIDTH);
         if(s == null) n.setMeasuredStrokeWidth(null);
         else n.setMeasuredStrokeWidth(FGraphicNode.STROKEWCONSTRAINTS.constrain(Measure.fromString(s)));
      }
      if(e.hasAttribute(FGModelSchema.A_STROKECAP))
      {
         s = e.getAttributeValueByName(FGModelSchema.A_STROKECAP);
         n.setStrokeEndcap(Utilities.getEnumValueFromString(s, StrokeCap.values()));
      }
      if(e.hasAttribute(FGModelSchema.A_STROKEJOIN))
      {
         s = e.getAttributeValueByName(FGModelSchema.A_STROKEJOIN);
         n.setStrokeJoin(Utilities.getEnumValueFromString(s, StrokeJoin.values()));
      }
      if(e.hasAttribute(FGModelSchema.A_STROKECOLOR))
      {
         s = e.getAttributeValueByName(FGModelSchema.A_STROKECOLOR);
         n.setStrokeColor(s == null ? null : BkgFill.colorFromHexString(s));
      }
      if(e.hasAttribute(FGModelSchema.A_STROKEPAT))
      {
         s = e.getAttributeValueByName(FGModelSchema.A_STROKEPAT);
         n.setStrokePattern(s == null ? null : StrokePattern.fromString(s));
      }
      
      // selected graphic node types have an optional "id" attribute 
      if(e.hasAttribute(FGModelSchema.A_ID) && ((s = e.getAttributeValueByName(FGModelSchema.A_ID)) != null))
      {
         if(!FGModelSchema.DEFAULT_OBJ_ID.equals(s)) n.setID(s);
      }      

      // remaining properties by node type. Note default attribute value handling!!
      Measure.Constraints locCon = FGraphicModel.getLocationConstraints(n.getNodeType());
      Measure.Constraints szCon = FGraphicModel.getSizeConstraints(n.getNodeType());
      Measure m, m2;
      int index;
      DataSet ds;
      String s2;
      switch(n.getNodeType())
      {
         case FIGURE: 
            FigureNode fig = (FigureNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_BORDER);
            if(s == null) s = FGModelSchema.DEFAULT_BORDER;
            m = Measure.fromString(s);
            fig.setBorderWidth(FigureNode.STROKEWCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_BKG);
            if(s == null) s = FGModelSchema.DEFAULT_FIGURE_BKG;
            fig.setBackgroundFill(BkgFill.fromXML(s));
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            fig.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_NOTE);
            fig.setNote((s != null) ? s : FGModelSchema.DEFAULT_NOTE);
            s = e.getAttributeValueByName(FGModelSchema.A_HIDE);
            if(s == null) s = FGModelSchema.DEFAULT_FIGURE_HIDE;
            fig.setTitleHidden("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_HALIGN);
            if(s == null) s = FGModelSchema.DEFAULT_FIGURE_HALIGN;
            fig.setTitleHorizontalAlignment(fromXMLToTextAlignment(true, s));
            s = e.getAttributeValueByName(FGModelSchema.A_VALIGN);
            if(s == null) s = FGModelSchema.DEFAULT_FIGURE_VALIGN;
            fig.setTitleVerticalAlignment(fromXMLToTextAlignment(false, s));
            
            s = e.getAttributeValueByName(FGModelSchema.A_LOC);
            index = s.indexOf(' ');
            m = Measure.fromString(s.substring(0, index));
            m2 = Measure.fromString(s.substring(index+1));
            fig.setXY(locCon.constrain(m), locCon.constrain(m2));

            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_WIDTH));
            fig.setWidth(szCon.constrain(m));
            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_HEIGHT));
            fig.setHeight(szCon.constrain(m));
            break;
         case GRAPH: 
            GraphNode g = (GraphNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_ROTATE);
            g.setRotate(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_ROTATE));
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            g.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_TYPE);
            if(s == null) s = FGModelSchema.DEFAULT_GRAPH_TYPE;
            g.setCoordSys(Utilities.getEnumValueFromString(s, GraphNode.CoordSys.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_LAYOUT);
            if(s == null) s = FGModelSchema.DEFAULT_LAYOUT;
            g.setLayout(Utilities.getEnumValueFromString(s, GraphNode.Layout.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_CLIP);
            if(s == null) s = FGModelSchema.DEFAULT_CLIP;
            g.setClip("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_AUTORANGE);
            if(s == null) s = FGModelSchema.DEFAULT_AUTORANGE;
            g.setAutorangeAxes(Utilities.getEnumValueFromString(s, GraphNode.Autorange.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_BOXCOLOR);
            if(s == null) s = FGModelSchema.DEFAULT_BOXCOLOR;
            g.setBoxColor(BkgFill.colorFromHexString(s));
            s = e.getAttributeValueByName(FGModelSchema.A_HIDE);
            if(s == null) s = FGModelSchema.DEFAULT_GRAPH_HIDE;
            g.setTitleHidden("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_HALIGN);
            if(s == null) s = FGModelSchema.DEFAULT_GRAPH_HALIGN;
            g.setTitleHorizontalAlignment(fromXMLToTextAlignment(true, s));
            s = e.getAttributeValueByName(FGModelSchema.A_GAP);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_GRAPH_GAP);
            g.setTitleGap(FGNGraph.TITLEGAPCONSTRAINTS.constrain(m));

            s = e.getAttributeValueByName(FGModelSchema.A_LOC);
            index = s.indexOf(' ');
            m = Measure.fromString(s.substring(0, index));
            m2 = Measure.fromString(s.substring(index+1));
            g.setXY(locCon.constrain(m), locCon.constrain(m2));

            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_WIDTH));
            g.setWidth(szCon.constrain(m));
            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_HEIGHT));
            g.setHeight(szCon.constrain(m));
            break;
         case PGRAPH:
            PolarPlotNode pg = (PolarPlotNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            pg.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_GRIDONTOP);
            if(s == null) s = FGModelSchema.DEFAULT_GRIDONTOP;
            pg.setGridOnTop("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_CLIP);
            if(s == null) s = FGModelSchema.DEFAULT_PGRAPH_CLIP;
            pg.setClip("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_BOXCOLOR);
            if(s == null) s = FGModelSchema.DEFAULT_BOXCOLOR;
            pg.setGridBkgColor(BkgFill.colorFromHexString(s));
            s = e.getAttributeValueByName(FGModelSchema.A_HIDE);
            if(s == null) s = FGModelSchema.DEFAULT_GRAPH_HIDE;
            pg.setTitleHidden("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_HALIGN);
            if(s == null) s = FGModelSchema.DEFAULT_GRAPH_HALIGN;
            pg.setTitleHorizontalAlignment(fromXMLToTextAlignment(true, s));
            s = e.getAttributeValueByName(FGModelSchema.A_GAP);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_GRAPH_GAP);
            pg.setTitleGap(FGNGraph.TITLEGAPCONSTRAINTS.constrain(m));

            s = e.getAttributeValueByName(FGModelSchema.A_LOC);
            index = s.indexOf(' ');
            m = Measure.fromString(s.substring(0, index));
            m2 = Measure.fromString(s.substring(index+1));
            pg.setXY(locCon.constrain(m), locCon.constrain(m2));

            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_WIDTH));
            pg.setWidth(szCon.constrain(m));
            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_HEIGHT));
            pg.setHeight(szCon.constrain(m));
            break;
         case GRAPH3D:
            Graph3DNode g3 = (Graph3DNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_ROTATE);
            g3.setRotate(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_GRAPH3D_ROTATE));
            s = e.getAttributeValueByName(FGModelSchema.A_ELEVATE);
            g3.setElevate(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_ELEVATE));
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            g3.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_BACKDROP);
            if(s == null) s = FGModelSchema.DEFAULT_BACKDROP;
            g3.setBackDrop(Utilities.getEnumValueFromString(s, Graph3DNode.BackDrop.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_PSCALE);
            g3.setProjectionDistanceScale(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_PSCALE));
            s = e.getAttributeValueByName(FGModelSchema.A_HIDE);
            if(s == null) s = FGModelSchema.DEFAULT_GRAPH_HIDE;
            g3.setTitleHidden("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_HALIGN);
            if(s == null) s = FGModelSchema.DEFAULT_GRAPH_HALIGN;
            g3.setTitleHorizontalAlignment(fromXMLToTextAlignment(true, s));
            s = e.getAttributeValueByName(FGModelSchema.A_GAP);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_GRAPH_GAP);
            g3.setTitleGap(FGNGraph.TITLEGAPCONSTRAINTS.constrain(m));

            s = e.getAttributeValueByName(FGModelSchema.A_LOC);
            index = s.indexOf(' ');
            m = Measure.fromString(s.substring(0, index));
            m2 = Measure.fromString(s.substring(index+1));
            g3.setXY(locCon.constrain(m), locCon.constrain(m2));

            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_WIDTH));
            g3.setWidth(szCon.constrain(m));
            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_HEIGHT));
            g3.setHeight(szCon.constrain(m));
            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_DEPTH));
            g3.setDepth(szCon.constrain(m));
            break;
         case TEXTBOX:
            TextBoxNode tbox = (TextBoxNode) n;
            s = e.getAttributeValueByName(FGModelSchema.A_ROTATE);
            tbox.setRotate(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_ROTATE));
            s = e.getAttributeValueByName(FGModelSchema.A_HALIGN);
            if(s == null) s = FGModelSchema.DEFAULT_TEXTBOX_HALIGN;
            tbox.setHorizontalAlignment(fromXMLToTextAlignment(true, s));
            s = e.getAttributeValueByName(FGModelSchema.A_VALIGN);
            if(s == null) s = FGModelSchema.DEFAULT_TEXTBOX_VALIGN;
            tbox.setVerticalAlignment(fromXMLToTextAlignment(false, s));
            s = e.getAttributeValueByName(FGModelSchema.A_CLIP);
            if(s == null) s = FGModelSchema.DEFAULT_CLIP;
            tbox.setClip("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_GAP);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_TEXTBOX_GAP);
            tbox.setMargin(FGraphicNode.STROKEWCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_BKG);
            if(s == null) s = FGModelSchema.DEFAULT_TEXTBOX_BKG;
            tbox.setBackgroundFill(BkgFill.fromXML(s));
            s = e.getAttributeValueByName(FGModelSchema.A_LINEHT);
            tbox.setLineHeight(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_LINEHT));
            
            s = e.getAttributeValueByName(FGModelSchema.A_LOC);
            index = s.indexOf(' ');
            m = Measure.fromString(s.substring(0, index));
            m2 = Measure.fromString(s.substring(index+1));
            tbox.setXY(locCon.constrain(m), locCon.constrain(m2));

            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_WIDTH));
            tbox.setWidth(szCon.constrain(m));
            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_HEIGHT));
            tbox.setHeight(szCon.constrain(m));
            break;
         case IMAGE:
            ImageNode img = (ImageNode) n;
            s = e.getAttributeValueByName(FGModelSchema.A_ROTATE);
            img.setRotate(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_ROTATE));
            s = e.getAttributeValueByName(FGModelSchema.A_GAP);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_IMAGE_GAP);
            img.setMargin(FGraphicNode.STROKEWCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_BKG);
            if(s == null) s = FGModelSchema.DEFAULT_IMAGE_BKG;
            img.setBackgroundFill(BkgFill.fromXML(s));
            
            // NOTE: We can't set crop rectangle here, because source image (if any) has NOT been read yet! We take
            // care of this in graphicNodeFromXML()...
            
            s = e.getAttributeValueByName(FGModelSchema.A_LOC);
            index = s.indexOf(' ');
            m = Measure.fromString(s.substring(0, index));
            m2 = Measure.fromString(s.substring(index+1));
            img.setXY(locCon.constrain(m), locCon.constrain(m2));

            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_WIDTH));
            img.setWidth(szCon.constrain(m));
            m = Measure.fromString(e.getAttributeValueByName(FGModelSchema.A_HEIGHT));
            img.setHeight(szCon.constrain(m));
            break;
         case LABEL:
            LabelNode label = (LabelNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_ROTATE);
            label.setRotate(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_ROTATE));
            s = e.getAttributeValueByName(FGModelSchema.A_HALIGN);
            if(s == null) s = FGModelSchema.DEFAULT_LABEL_HALIGN;
            label.setHorizontalAlignment(fromXMLToTextAlignment(true, s));
            s = e.getAttributeValueByName(FGModelSchema.A_VALIGN);
            if(s == null) s = FGModelSchema.DEFAULT_LABEL_VALIGN;
            label.setVerticalAlignment(fromXMLToTextAlignment(false, s));

            s = e.getAttributeValueByName(FGModelSchema.A_LOC);
            index = s.indexOf(' ');
            m = Measure.fromString(s.substring(0, index));
            m2 = Measure.fromString(s.substring(index+1));
            label.setXY(locCon.constrain(m), locCon.constrain(m2));

            label.setTitle(e.getAttributeValueByName(FGModelSchema.A_TITLE));
            break;
         case LINE:
            LineNode line = (LineNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_P0);
            index = s.indexOf(' ');
            line.setX(locCon.constrain(Measure.fromString(s.substring(0,index))));
            line.setY(locCon.constrain(Measure.fromString(s.substring(index+1))));
            s = e.getAttributeValueByName(FGModelSchema.A_P1);
            index = s.indexOf(' ');
            line.setX2(locCon.constrain(Measure.fromString(s.substring(0,index))));
            line.setY2(locCon.constrain(Measure.fromString(s.substring(index+1))));
            break;
         case SHAPE:
            ShapeNode shape = (ShapeNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_ROTATE);
            shape.setRotate(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_ROTATE));
            s = e.getAttributeValueByName(FGModelSchema.A_TYPE);
            if(s == null) s = FGModelSchema.DEFAULT_SYMBOL;
            shape.setType(Utilities.getEnumValueFromString(s, Marker.values()));
            
            s = e.getAttributeValueByName(FGModelSchema.A_WIDTH);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_SHAPE_DIM);
            shape.setWidth(szCon.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_HEIGHT);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_SHAPE_DIM);
            shape.setHeight(szCon.constrain(m));
            
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            shape.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_BKG);
            if(s == null) s = FGModelSchema.DEFAULT_SHAPE_BKG;
            shape.setBackgroundFill(BkgFill.fromXML(s));

            s = e.getAttributeValueByName(FGModelSchema.A_LOC);
            index = s.indexOf(' ');
            m = Measure.fromString(s.substring(0, index));
            m2 = Measure.fromString(s.substring(index+1));
            shape.setXY(locCon.constrain(m), locCon.constrain(m2));
            break;
         case CALIB:
            CalibrationBarNode calib = (CalibrationBarNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_PRIMARY);
            if(s == null) s = FGModelSchema.DEFAULT_PRIMARY;
            calib.setPrimary("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_AUTO);
            if(s == null) s = FGModelSchema.DEFAULT_AUTO;
            calib.setAutoLabelOn("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_CAP);
            if(s == null) s = FGModelSchema.DEFAULT_CALIB_CAP;
            calib.setEndCap(Utilities.getEnumValueFromString(s, Marker.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_CAPSIZE);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_CAPSIZE);
            calib.setEndCapSize(CalibrationBarNode.ENDCAPSIZECONSTRAINTS.constrain(m));

            calib.setLength(Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_LEN)));

            s = e.getAttributeValueByName(FGModelSchema.A_LOC);
            index = s.indexOf(' ');
            m = Measure.fromString(s.substring(0, index));
            m2 = Measure.fromString(s.substring(index+1));
            calib.setXY(locCon.constrain(m), locCon.constrain(m2));
            break;
         case FUNCTION:
            FunctionNode func = (FunctionNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            func.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_LEGEND);
            if(s == null) s = FGModelSchema.DEFAULT_LEGEND;
            func.setShowInLegend("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_X0);
            func.setX0(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_X0));
            s = e.getAttributeValueByName(FGModelSchema.A_X1);
            func.setX1(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_X1));
            s = e.getAttributeValueByName(FGModelSchema.A_DX);
            func.setDX(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_DX));
            break;
         case TRACE:
            TraceNode trace = (TraceNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            trace.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_LEGEND);
            if(s == null) s = FGModelSchema.DEFAULT_LEGEND;
            trace.setShowInLegend("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_SKIP);
            trace.setSkip(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_SKIP));
            s = e.getAttributeValueByName(FGModelSchema.A_MODE);
            if(s == null) s = FGModelSchema.DEFAULT_MODE;
            trace.setMode(Utilities.getEnumValueFromString(s, TraceNode.DisplayMode.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_BARWIDTH);
            trace.setBarWidth(Float.parseFloat((s != null) ? s : FGModelSchema.DEFAULT_BARWIDTH));
            s = e.getAttributeValueByName(FGModelSchema.A_BASELINE);
            trace.setBaseline(Float.parseFloat((s != null) ? s : FGModelSchema.DEFAULT_BASELINE));
            s = e.getAttributeValueByName(FGModelSchema.A_XOFF);
            trace.setXOffset(Float.parseFloat((s != null) ? s : FGModelSchema.DEFAULT_XYOFF));
            s = e.getAttributeValueByName(FGModelSchema.A_YOFF);
            trace.setYOffset(Float.parseFloat((s != null) ? s : FGModelSchema.DEFAULT_XYOFF));
            s = e.getAttributeValueByName(FGModelSchema.A_AVG);
            if(s == null) s = FGModelSchema.DEFAULT_TRACE_AVG;
            trace.setShowAverage("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_LEN);
            trace.setSlidingWindowLength(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_TRACE_LEN));
            
            // A_SRC="#srcid" is required. Initially dataset is an empty placeholder with this ID. The actual datasets 
            // are injected after the model is constructed.
            s = e.getAttributeValueByName(FGModelSchema.A_SRC);
            if(s == null) throw new XMLException("Missing required attribute", FGModelSchema.EL_TRACE, FGModelSchema.A_SRC);
            ds = trace.getDataSet().changeID(s.substring(1));
            if(ds == null) throw new XMLException("Bad dataset ID reference", FGModelSchema.EL_TRACE, FGModelSchema.A_SRC);
            trace.setDataSet(ds);
            break;
         case RASTER:
            RasterNode raster = (RasterNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            raster.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_LEGEND);
            if(s == null) s = FGModelSchema.DEFAULT_LEGEND;
            raster.setShowInLegend("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_MODE);
            if(s == null) s = FGModelSchema.DEFAULT_RASTER_MODE;
            raster.setMode(Utilities.getEnumValueFromString(s, RasterNode.DisplayMode.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_BASELINE);
            raster.setBaseline(Float.parseFloat((s != null) ? s : FGModelSchema.DEFAULT_BASELINE));
            s = e.getAttributeValueByName(FGModelSchema.A_XOFF);
            raster.setXOffset(Float.parseFloat((s != null) ? s : FGModelSchema.DEFAULT_XYOFF));
            s = e.getAttributeValueByName(FGModelSchema.A_HEIGHT);
            raster.setLineHeight(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_RASTER_HEIGHT));
            s = e.getAttributeValueByName(FGModelSchema.A_SPACER);
            raster.setLineSpacer(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_RASTER_SPACER));
            s = e.getAttributeValueByName(FGModelSchema.A_NBINS);
            raster.setNumBins(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_NBINS));
            s = e.getAttributeValueByName(FGModelSchema.A_AVG);
            if(s == null) s = FGModelSchema.DEFAULT_RASTER_AVG;
            raster.setAveraged("true".equals(s));
            
            s = e.getAttributeValueByName(FGModelSchema.A_START);
            s2 = e.getAttributeValueByName(FGModelSchema.A_END);
            raster.setHistogramRange(
                  Float.parseFloat((s != null) ? s : FGModelSchema.DEFAULT_RASTER_STARTEND),
                  Float.parseFloat((s2 != null) ? s2 : FGModelSchema.DEFAULT_RASTER_STARTEND));

            // A_SRC="#srcid" is required. Initially dataset is an empty placeholder with this ID. The actual datasets 
            // are injected after the model is constructed.
            s = e.getAttributeValueByName(FGModelSchema.A_SRC);
            if(s == null) throw new XMLException("Missing required attribute", FGModelSchema.EL_RASTER, FGModelSchema.A_SRC);
            ds = raster.getDataSet().changeID(s.substring(1));
            if(ds == null) throw new XMLException("Bad dataset ID reference", FGModelSchema.EL_RASTER, FGModelSchema.A_SRC);
            raster.setDataSet(ds);
            break;
         case CONTOUR:
            ContourNode contour = (ContourNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            contour.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_SMOOTH);
            if(s == null) s = FGModelSchema.DEFAULT_HEATMAP_SMOOTH;
            contour.setSmoothed("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_MODE);
            if(s == null) s = FGModelSchema.DEFAULT_CONTOUR_MODE;
            contour.setMode(Utilities.getEnumValueFromString(s, ContourNode.DisplayMode.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_LEVELS);
            if(s == null) s = FGModelSchema.DEFAULT_LEVELS;
            contour.setLevelList(s);
            
            // A_SRC="#srcid" is required. Initially dataset is an empty placeholder with this ID. The actual datasets 
            // are injected after the model is constructed.
            s = e.getAttributeValueByName(FGModelSchema.A_SRC);
            if(s == null) throw new XMLException("Missing required attribute", FGModelSchema.EL_CONTOUR, FGModelSchema.A_SRC);
            ds = contour.getDataSet().changeID(s.substring(1));
            if(ds == null) throw new XMLException("Bad dataset ID reference", FGModelSchema.EL_CONTOUR, FGModelSchema.A_SRC);
            contour.setDataSet(ds);
            break;
         case SURFACE:
            SurfaceNode surf = (SurfaceNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            surf.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_CMAP);
            if(s == null) s = FGModelSchema.DEFAULT_SURFACE_CMAP;
            surf.setColorMapped("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_LIMIT);
            surf.setMeshLimit(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_LIMIT));

            // A_SRC="#srcid" is required. Initially dataset is an empty placeholder with this ID. The actual datasets 
            // are injected after the model is constructed.
            s = e.getAttributeValueByName(FGModelSchema.A_SRC);
            if(s == null) throw new XMLException("Missing required attribute", FGModelSchema.EL_SURFACE, FGModelSchema.A_SRC);
            ds = surf.getDataSet().changeID(s.substring(1));
            if(ds == null) throw new XMLException("Bad dataset ID reference", FGModelSchema.EL_SURFACE, FGModelSchema.A_SRC);
            surf.setDataSet(ds);
            break;
         case BAR:
            BarPlotNode bp = (BarPlotNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            bp.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_MODE);
            if(s == null) s = FGModelSchema.DEFAULT_BAR_MODE;
            bp.setMode(Utilities.getEnumValueFromString(s, BarPlotNode.DisplayMode.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_BASELINE);
            bp.setBaseline(Float.parseFloat((s != null) ? s : FGModelSchema.DEFAULT_BASELINE));
            s = e.getAttributeValueByName(FGModelSchema.A_BARWIDTH);
            bp.setBarWidth(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_BAR_BARWIDTH));
            s = e.getAttributeValueByName(FGModelSchema.A_LEGEND);
            if(s == null) s = FGModelSchema.DEFAULT_LEGEND;
            bp.setShowInLegend("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_AUTO);
            if(s == null) s = FGModelSchema.DEFAULT_BAR_AUTO;
            bp.setAutoLabelOn("true".equals(s));
            
            // A_SRC="#srcid" is required. Initially dataset is an empty placeholder with this ID. The actual datasets 
            // are injected after the model is constructed.
            s = e.getAttributeValueByName(FGModelSchema.A_SRC);
            if(s == null) throw new XMLException("Missing required attribute", FGModelSchema.EL_BAR, FGModelSchema.A_SRC);
            ds = bp.getDataSet().changeID(s.substring(1));
            if(ds == null) throw new XMLException("Bad dataset ID reference", FGModelSchema.EL_BAR, FGModelSchema.A_SRC);
            bp.setDataSet(ds);
            break;
         case BOX:
            BoxPlotNode box = (BoxPlotNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            box.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_MODE);
            if(s == null) s = FGModelSchema.DEFAULT_BOX_MODE;
            box.setMode(Utilities.getEnumValueFromString(s, BoxPlotNode.DisplayMode.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_BARWIDTH);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_BOX_BARWIDTH);
            box.setBoxWidth(BoxPlotNode.BOXWIDTHCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_X0);
            box.setOffset(Float.parseFloat((s != null) ? s : FGModelSchema.DEFAULT_X0));
            s = e.getAttributeValueByName(FGModelSchema.A_DX);
            box.setInterval(Float.parseFloat((s != null) ? s : FGModelSchema.DEFAULT_DX));
            s = e.getAttributeValueByName(FGModelSchema.A_LEGEND);
            if(s == null) s = FGModelSchema.DEFAULT_LEGEND;
            box.setShowInLegend("true".equals(s));
            
            // A_SRC="#srcid" is required. Initially dataset is an empty placeholder with this ID. The actual datasets 
            // are injected after the model is constructed.
            s = e.getAttributeValueByName(FGModelSchema.A_SRC);
            if(s == null) 
               throw new XMLException("Missing required attribute", FGModelSchema.EL_BOX, FGModelSchema.A_SRC);
            ds = box.getDataSet().changeID(s.substring(1));
            if(ds == null) 
               throw new XMLException("Bad dataset ID reference", FGModelSchema.EL_BOX, FGModelSchema.A_SRC);
            box.setDataSet(ds);
            break;
         case VIOLIN:
            ViolinStyleNode violin = (ViolinStyleNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_SIZE);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_VIOLIN_SIZE);
            violin.setSize(BoxPlotNode.BOXWIDTHCONSTRAINTS.constrain(m));
            break;
         case SCATTER:
            ScatterPlotNode spn = (ScatterPlotNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            spn.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_LEGEND);
            if(s == null) s = FGModelSchema.DEFAULT_LEGEND;
            spn.setShowInLegend("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_MODE);
            if(s == null) s = FGModelSchema.DEFAULT_SCATTER_MODE;
            spn.setMode(Utilities.getEnumValueFromString(s, ScatterPlotNode.DisplayMode.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_TYPE);
            if(s == null) s = FGModelSchema.DEFAULT_SCATTER_TYPE;
            spn.setSymbol(Utilities.getEnumValueFromString(s, Marker.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_SIZE);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_SCATTER_SIZE);
            spn.setMaxSymbolSize(ScatterPlotNode.MAXSYMSIZECONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_MINSIZE);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_MINSIZE);
            spn.setMinSymbolSize(ScatterPlotNode.MAXSYMSIZECONSTRAINTS.constrain(m));

            // A_SRC="#srcid" is required. Initially dataset is an empty placeholder with this ID. The actual datasets 
            // are injected after the model is constructed.
            s = e.getAttributeValueByName(FGModelSchema.A_SRC);
            if(s == null)
               throw new XMLException("Missing required attribute", FGModelSchema.EL_SCATTER, FGModelSchema.A_SRC);
            ds = spn.getDataSet().changeID(s.substring(1));
            if(ds == null) 
               throw new XMLException("Bad dataset ID reference", FGModelSchema.EL_SCATTER, FGModelSchema.A_SRC);
            spn.setDataSet(ds);
            break;
         case SCATTER3D:
            Scatter3DNode sp3 = (Scatter3DNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            sp3.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_LEGEND);
            if(s == null) s = FGModelSchema.DEFAULT_LEGEND;
            sp3.setShowInLegend("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_MODE);
            if(s == null) s = FGModelSchema.DEFAULT_SCATTER_MODE;
            sp3.setMode(Utilities.getEnumValueFromString(s, Scatter3DNode.DisplayMode.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_STEMMED);
            if(s == null) s = FGModelSchema.DEFAULT_STEMMED;
            sp3.setStemmed("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_BASELINE);
            sp3.setZBase(Float.parseFloat((s != null) ? s : FGModelSchema.DEFAULT_BASELINE));
            s = e.getAttributeValueByName(FGModelSchema.A_BARWIDTH);
            sp3.setBarSize(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_SCAT3D_BARWIDTH));
            s = e.getAttributeValueByName(FGModelSchema.A_BKG);
            if(s == null) s = FGModelSchema.DEFAULT_SHAPE_BKG;
            sp3.setBackgroundFill(BkgFill.fromXML(s));
            s = e.getAttributeValueByName(FGModelSchema.A_DOTSIZE);
            if(s == null) s = FGModelSchema.DEFAULT_DOTSIZE;
            sp3.setProjectionDotSizesFromTokenList(s);
            s = e.getAttributeValueByName(FGModelSchema.A_DOTCOLOR);
            if(s == null) s = FGModelSchema.DEFAULT_DOTCOLOR;
            sp3.setProjectionDotColorsFromTokenList(s);
            s = e.getAttributeValueByName(FGModelSchema.A_MINSIZE);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_MINSIZE);
            sp3.setMinSymbolSize(SymbolNode.SYMBOLSIZECONSTRAINTS.constrain(m));

            // A_SRC="#srcid" is required. Initially dataset is an empty placeholder with this ID. The actual datasets 
            // are injected after the model is constructed.
            s = e.getAttributeValueByName(FGModelSchema.A_SRC);
            if(s == null)
               throw new XMLException("Missing required attribute", FGModelSchema.EL_SCATTER3D, FGModelSchema.A_SRC);
            ds = sp3.getDataSet().changeID(s.substring(1));
            if(ds == null) 
               throw new XMLException("Bad dataset ID reference", FGModelSchema.EL_SCATTER3D, FGModelSchema.A_SRC);
            sp3.setDataSet(ds);
            break;
         case AREA:
            AreaChartNode ac = (AreaChartNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            ac.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_BASELINE);
            ac.setBaseline(Float.parseFloat((s != null) ? s : FGModelSchema.DEFAULT_BASELINE));
            s = e.getAttributeValueByName(FGModelSchema.A_LEGEND);
            if(s == null) s = FGModelSchema.DEFAULT_LEGEND;
            ac.setShowInLegend("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_MODE);
            if(s == null) s = FGModelSchema.DEFAULT_AREA_MODE;
            ac.setLabelMode(Utilities.getEnumValueFromString(s, AreaChartNode.LabelMode.values()));
            
            // A_SRC="#srcid" is required. Initially dataset is an empty placeholder with this ID. The actual datasets 
            // are injected after the model is constructed.
            s = e.getAttributeValueByName(FGModelSchema.A_SRC);
            if(s == null) throw new XMLException("Missing required attr", FGModelSchema.EL_AREA, FGModelSchema.A_SRC);
            ds = ac.getDataSet().changeID(s.substring(1));
            if(ds == null) throw new XMLException("Bad dataset ID", FGModelSchema.EL_AREA, FGModelSchema.A_SRC);
            ac.setDataSet(ds);
            break;
         case PIE:
            PieChartNode pie = (PieChartNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            pie.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_LEGEND);
            if(s == null) s = FGModelSchema.DEFAULT_LEGEND;
            pie.setShowInLegend("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_GAP);
            pie.setRadialOffset(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_PIE_GAP));
            s = e.getAttributeValueByName(FGModelSchema.A_DISPLACE);
            pie.setDisplacedBits(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_PIE_DISPLACE));
            s = e.getAttributeValueByName(FGModelSchema.A_MODE);
            if(s == null) s = FGModelSchema.DEFAULT_PIE_MODE;
            pie.setSliceLabelMode(Utilities.getEnumValueFromString(s, PieChartNode.LabelMode.values()));
            
            pie.setInnerRadius(Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_START)));
            pie.setOuterRadius(Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_END)));

            // A_SRC="#srcid" is required. Initially dataset is an empty placeholder with this ID. The actual datasets 
            // are injected after the model is constructed.
            s = e.getAttributeValueByName(FGModelSchema.A_SRC);
            if(s == null) 
               throw new XMLException("Missing required attribute", FGModelSchema.EL_PIE, FGModelSchema.A_SRC);
            ds = pie.getDataSet().changeID(s.substring(1));
            if(ds == null) 
               throw new XMLException("Bad dataset ID reference", FGModelSchema.EL_PIE, FGModelSchema.A_SRC);
            pie.setDataSet(ds);
            break;
         case AXIS:
            AxisNode axis = (AxisNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_HIDE);
            if(s == null) s = FGModelSchema.DEFAULT_AXIS_HIDE;
            axis.setHide("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_LOG2);
            if(s == null) s = FGModelSchema.DEFAULT_LOG2;
            axis.setLog2("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_UNITS);
            axis.setUnits((s != null) ? s : FGModelSchema.DEFAULT_UNITS);
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            axis.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_SPACER);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_AXIS_SPACER);
            axis.setSpacer(AxisNode.SPACERCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_LABELOFFSET);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_LABELOFFSET);
            axis.setLabelOffset(AxisNode.SPACERCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_LINEHT);
            axis.setLineHeight(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_LINEHT));
            s = e.getAttributeValueByName(FGModelSchema.A_SCALE);
            axis.setPowerScale(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_SCALE));

            axis.setStart(Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_START)));
            axis.setEnd(Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_END)));
            break;
         case CBAR:
            ColorBarNode cbar = (ColorBarNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_HIDE);
            if(s == null) s = FGModelSchema.DEFAULT_COLORBAR_HIDE;
            cbar.setHide("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            cbar.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_SPACER);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_AXIS_SPACER);
            cbar.setBarAxisSpacer(ColorBarNode.GAPCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_LABELOFFSET);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_LABELOFFSET);
            cbar.setLabelOffset(ColorBarNode.GAPCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_GAP);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_COLORBAR_GAP);
            cbar.setGap(ColorBarNode.GAPCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_SIZE);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_COLORBAR_SIZE);
            cbar.setBarSize(ColorBarNode.GAPCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_CMAP);
            if(s == null) s = FGModelSchema.DEFAULT_COLORBAR_CMAP;
            cbar.setColorMap(ColorMap.fromString(s));
            s = e.getAttributeValueByName(FGModelSchema.A_REVERSE);
            if(s == null) s = FGModelSchema.DEFAULT_REVERSE;
            cbar.setReversed("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_CMODE);
            if(s == null) s = FGModelSchema.DEFAULT_COLORBAR_CMODE;
            cbar.setColorMapMode(Utilities.getEnumValueFromString(s, ColorBarNode.CMapMode.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_EDGE);
            if(s == null) s = FGModelSchema.DEFAULT_EDGE;
            cbar.setEdge(Utilities.getEnumValueFromString(s, ColorBarNode.Edge.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_CMAPNAN);
            if(s == null) s = FGModelSchema.DEFAULT_COLORBAR_CMAPNAN;
            cbar.setColorNaN(BkgFill.colorFromHexString(s));
            s = e.getAttributeValueByName(FGModelSchema.A_LINEHT);
            cbar.setLineHeight(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_LINEHT));
            s = e.getAttributeValueByName(FGModelSchema.A_SCALE);
            cbar.setPowerScale(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_SCALE));

            cbar.setStart(Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_START)));
            cbar.setEnd(Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_END)));
            break;
         case PAXIS:
            PolarAxisNode paxis = (PolarAxisNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_HIDE);
            if(s == null) s = FGModelSchema.DEFAULT_AXIS_HIDE;
            paxis.setHide("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_REVERSE);
            if(s == null) s = FGModelSchema.DEFAULT_REVERSE;
            paxis.setReversed("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_GAP);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_PAXIS_GAP);
            paxis.setGridLabelGap(AxisNode.SPACERCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_FMT);
            if(s == null) s = FGModelSchema.DEFAULT_TICK_FMT;
            paxis.setGridLabelFormat(Utilities.getEnumValueFromString(s, TickSetNode.LabelFormat.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_REFANGLE);
            int angle = Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_REFANGLE);
            paxis.setReferenceAngle(Utilities.rangeRestrict(-359, 359, angle));

            // polar axis range endpoints are set together and they are silently auto-corrected if necessary
            paxis.setRange(Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_START)),
                  Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_END)));
            
            // if specified grid divisions invalid, then configure with NO grid divisions
            if(!paxis.setGridDivisionsFromString(e.getAttributeValueByName(FGModelSchema.A_PDIVS), true))
               paxis.setGridDivisions(new double[] {0, 0, 0});
            break;
         case AXIS3D:
            Axis3DNode a3 = (Axis3DNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_HIDE);
            if(s == null) s = FGModelSchema.DEFAULT_AXIS_HIDE;
            a3.setHide("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_LOG2);
            if(s == null) s = FGModelSchema.DEFAULT_LOG2;
            a3.setLogBase2("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            a3.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            s = e.getAttributeValueByName(FGModelSchema.A_SPACER);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_AXIS_SPACER);
            a3.setSpacer(AxisNode.SPACERCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_LABELOFFSET);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_LABELOFFSET);
            a3.setLabelOffset(AxisNode.SPACERCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_LINEHT);
            a3.setLineHeight(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_LINEHT));
            s = e.getAttributeValueByName(FGModelSchema.A_SCALE);
            a3.setPowerScale(Integer.parseInt((s != null) ? s : FGModelSchema.DEFAULT_SCALE));

            // for a 3D axis, range and log state are set together.
            s = e.getAttributeValueByName(FGModelSchema.A_LOG);
            if(s == null) s = FGModelSchema.DEFAULT_LOG;
            a3.setRange(Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_START)),
                  Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_END)), "true".equals(s));
            break;
         case TICKS:
            TickSetNode ticks = (TickSetNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_PERLOGINTV);
            ticks.setDecadeTicks(LogTickPattern.fromString((s != null) ? s : FGModelSchema.DEFAULT_PERLOGINTV));
            s = e.getAttributeValueByName(FGModelSchema.A_DIR);
            if(s == null) s = FGModelSchema.DEFAULT_TICK_DIR;
            ticks.setTickOrientation(Utilities.getEnumValueFromString(s, TickSetNode.Orientation.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_FMT);
            if(s == null) s = FGModelSchema.DEFAULT_TICK_FMT;
            ticks.setTickLabelFormat(Utilities.getEnumValueFromString(s, TickSetNode.LabelFormat.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_LEN);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_TICK_LEN);
            ticks.setTickLength(TickSetNode.TICKLENCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_GAP);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_TICK_GAP);
            ticks.setTickGap(TickSetNode.TICKLENCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_START);
            String end = e.getAttributeValueByName(FGModelSchema.A_END);
            if(s == null && end == null)
               ticks.setTrackingParentAxis(true);
            else
            {
               BasicSchemaElement parentAxis = (BasicSchemaElement) e.getParent();
               if(s == null) 
               {
                  s = parentAxis.getAttributeValueByName(FGModelSchema.A_START);
                  if(s == null) s = "0";
               }
               if(end == null)
               {
                  end = parentAxis.getAttributeValueByName(FGModelSchema.A_END);
                  if(end == null) end = "100";
               }
               ticks.setTrackingParentAxis(false);
               ticks.setStart(Double.parseDouble(s));
               ticks.setEnd(Double.parseDouble(end));
            }
            ticks.setInterval(Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_INTV)));
            break;
         case TICKS3D:
            Ticks3DNode t3 = (Ticks3DNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_PERLOGINTV);
            t3.setDecadeTicks(LogTickPattern.fromString((s != null) ? s : FGModelSchema.DEFAULT_PERLOGINTV));
            s = e.getAttributeValueByName(FGModelSchema.A_DIR);
            if(s == null) s = FGModelSchema.DEFAULT_TICK_DIR;
            t3.setTickOrientation(Utilities.getEnumValueFromString(s, TickSetNode.Orientation.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_FMT);
            if(s == null) s = FGModelSchema.DEFAULT_TICK_FMT;
            t3.setTickLabelFormat(Utilities.getEnumValueFromString(s, TickSetNode.LabelFormat.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_LEN);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_TICK_LEN);
            t3.setTickLength(TickSetNode.TICKLENCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_GAP);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_TICK_GAP);
            t3.setTickGap(TickSetNode.TICKLENCONSTRAINTS.constrain(m));
            
            // when "ticks" element is used to define a 3D tick set, we expect start <= end and intv > 0 always 
            // explicitly set. But they might not be, since schema object does not enforce it -- because there are
            // no constraints on a 2D tick set range and interval, and 2D tick set has the track axis feature.
            {
               BasicSchemaElement parentAxis = (BasicSchemaElement) e.getParent();
               s = e.getAttributeValueByName(FGModelSchema.A_START);
               if(s == null) s = parentAxis.getAttributeValueByName(FGModelSchema.A_START);
               if(s == null) s = "0";
               double rngMin = Double.parseDouble(s);
               s2 = e.getAttributeValueByName(FGModelSchema.A_END);
               if(s2 == null) s2 = parentAxis.getAttributeValueByName(FGModelSchema.A_END);
               if(s2 == null) s2 = "100";
               double rngMax = Double.parseDouble(s2);
               double intv = Double.parseDouble(e.getAttributeValueByName(FGModelSchema.A_INTV));
               t3.setRangeAndInterval(rngMin, rngMax, intv);
            }
            break;
         case LEGEND:
            LegendNode legend = (LegendNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_ROTATE);
            legend.setRotate(Double.parseDouble((s != null) ? s : FGModelSchema.DEFAULT_ROTATE));
            s = e.getAttributeValueByName(FGModelSchema.A_SPACER);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_LEGEND_SPACER);
            legend.setSpacer(LegendNode.LENCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_LABELOFFSET);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_LEGEND_LABELOFFSET);
            legend.setLabelOffset(LegendNode.LENCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_SIZE);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_LEGEND_SIZE);
            legend.setSymbolSize(LegendNode.LENCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_LEN);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_LEGEND_LEN);
            legend.setLength(LegendNode.LENCONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_MID);
            if(s == null) s = FGModelSchema.DEFAULT_MID;
            legend.setMid("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_HIDE);
            if(s == null) s = FGModelSchema.DEFAULT_LEGEND_HIDE;
            legend.setHide("true".equals(s));
            s = e.getAttributeValueByName(FGModelSchema.A_BOXCOLOR);
            if(s == null) s = FGModelSchema.DEFAULT_BOXCOLOR;
            legend.setBoxColor(BkgFill.colorFromHexString(s));
            s = e.getAttributeValueByName(FGModelSchema.A_BORDER);
            if(s == null) s = FGModelSchema.DEFAULT_BORDER;
            legend.setBorderWidth(Measure.fromString(s));

            s = e.getAttributeValueByName(FGModelSchema.A_LOC);
            index = s.indexOf(' ');
            m = Measure.fromString(s.substring(0, index));
            m2 = Measure.fromString(s.substring(index+1));
            legend.setXY(locCon.constrain(m), locCon.constrain(m2));
            break;
         case GRIDLINE:
            s = e.getAttributeValueByName(FGModelSchema.A_HIDE);
            if(s == null) s = FGModelSchema.DEFAULT_GRIDLINE_HIDE;
            ((GridLineNode)n).setHide("true".equals(s));
            break;
         case GRID3D:
         case BACK3D:
         case SCATLINESTYLE:
            // these nodes only have draw styles
            break;
         case SYMBOL:
            SymbolNode symbol = (SymbolNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_TYPE);
            if(s == null) s = FGModelSchema.DEFAULT_SYMBOL;
            symbol.setType(Utilities.getEnumValueFromString(s, Marker.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_SIZE);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_SYMBOL_SIZE);
            symbol.setSize(SymbolNode.SYMBOLSIZECONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_TITLE);
            symbol.setTitle((s != null) ? s : FGModelSchema.DEFAULT_TITLE);
            break;
         case EBAR:
            ErrorBarNode ebar = (ErrorBarNode)n;
            s = e.getAttributeValueByName(FGModelSchema.A_CAP);
            if(s == null) s = FGModelSchema.DEFAULT_EBAR_CAP;
            ebar.setEndCap(Utilities.getEnumValueFromString(s, Marker.values()));
            s = e.getAttributeValueByName(FGModelSchema.A_CAPSIZE);
            m = Measure.fromString((s != null) ? s : FGModelSchema.DEFAULT_CAPSIZE);
            ebar.setEndCapSize(ErrorBarNode.EBARCAPSIZECONSTRAINTS.constrain(m));
            s = e.getAttributeValueByName(FGModelSchema.A_HIDE);
            if(s == null) s = FGModelSchema.DEFAULT_EBAR_HIDE;
            ebar.setHide("true".equals(s));
            break;
      }
   }

   /**
    * Helper method handles the details of saving all data sets in a <code>FGraphicModel</code> into the XML schema 
    * content document that defines that model. As of schema version #6, all such data sets are stored in a "ref" 
    * element, which must appear as the last child of the root schema element. Thus, this method must be called after 
    * the root element has been populated with all other content from the model. (There is no graphic node that 
    * corresponds to the "ref" schema element; the graphic model manages all data sets internally.)
    * 
    * <p>NOTE: Schema version 8 was the initial version under <em>DataNav</em>, which integrated and superceded the 
    * original Java-based <em>Phyplot</em> in 2008. Dataset handling was significantly revamped in this version.</p>
    * 
    * @param schema The XML schema content document.
    * @param model The graphic model being converted to XML.
    * @throws XMLException If an exception is thrown, it indicates an error in the conversion code!
    */
   private static void putDataSetsToXML(ISchema schema, FGraphicModel model) throws XMLException
   {
      List<DataSet> datasets = model.getAllDataSetsInUse();
      BasicSchemaElement ref = (BasicSchemaElement) schema.createElement(FGModelSchema.EL_REF);
      for(int i=0; i<datasets.size(); i++)
      {
         DataSet ds = datasets.get(i);
         String id = ds.getID();
         String fmt = ds.getFormat().toString();
         String using = null;
         for(int j=0; j<i && using == null; j++)
         {
            DataSet other = datasets.get(j);
            if(DataSet.areIdenticalSets(ds, other, true))
               using = other.getID();
         }
         
         BasicSchemaElement eSet = (BasicSchemaElement) schema.createElement(FGModelSchema.EL_SET);
         eSet.setAttributeValueByName(FGModelSchema.A_ID, id);
         eSet.setAttributeValueByName(FGModelSchema.A_FMT, fmt);
         if(using != null) 
            eSet.setAttributeValueByName(FGModelSchema.A_USING, using);
         else
            eSet.setTextContent(DataSet.toBase64(ds, true), false);

         ref.add(eSet);
      }
      ((BasicSchemaElement) schema.getRootElement()).add(ref);
   }

   /**
    * Helper method extracts all defined data sets from the schema document and injects them into the graphic model 
    * under construction. It must be called after populating the graphic model via {@link #graphicNodeFromXML}, at
    * which point all data presentation elements will exist but will have empty data sets with IDs corresponding to the 
    * data sets stored in the schema's "ref" element. After extracting the sets from this schema element, the method
    * injects them into the model via {@link FGraphicModel#replaceDataSetsInUse}. If any data set goes unused, then
    * there must be something wrong with the schema document, and the method throws an exception.
    * 
    * @param schema The XML schema content document.
    * @param model The graphic model being constructed from the schema document.
    * @throws XMLException If an exception is thrown, it indicates an error in the conversion code!
    */
   private static void getDataSetsFromXML(ISchema schema, FGraphicModel model) throws XMLException
   {
      if(schema == null || schema.getRootElement() == null || model == null) return;
      List<DataSet> sets = new ArrayList<>();
      BasicSchemaElement fig = (BasicSchemaElement) schema.getRootElement();
      BasicSchemaElement ref = (BasicSchemaElement) fig.getChildAt(fig.getChildCount()-1);
      
      for(int i=0; i<ref.getChildCount(); i++)
      {
         BasicSchemaElement eSet = (BasicSchemaElement) ref.getChildAt(i);
         String id = eSet.getAttributeValueByName(FGModelSchema.A_ID);
         String fmt = eSet.getAttributeValueByName(FGModelSchema.A_FMT);
         String using = eSet.getAttributeValueByName(FGModelSchema.A_USING);
         boolean isV7 = "true".equals(eSet.getAttributeValueByName(FGModelSchema.A_V7));
         if(using == null)
         {
            DataSet ds;
            if(isV7)
            {
               float dx = 1;
               try { dx = Float.parseFloat(eSet.getAttributeValueByName(FGModelSchema.A_DX)); }
               catch(Throwable ignored) {}
               DataSet.Fmt dsFmt = Utilities.getEnumValueFromString(fmt, DataSet.Fmt.values());
               ds = DataSet.fromCommaSeparatedTuples(id, dsFmt, dx, eSet.getTextContent());
            }
            else ds = DataSet.fromBase64(eSet.getTextContent());
            
            if(ds == null) throw new XMLException("Bad dataset content found", FGModelSchema.EL_SET, null);
            if(!(ds.getID().equals(id) && ds.getFormat().toString().equals(fmt)))
               throw new XMLException("Attributes inconsistent with dataset content", FGModelSchema.EL_SET, null);
            sets.add(ds);
         }
         else
         {
            // in this case, we need to add a data set with a different ID that is otherwise identical to a data set 
            // that has already been extracted.
            DataSet found = null;
            for(DataSet ds : sets) if(ds.getID().equals(using) && ds.getFormat().toString().equals(fmt))
            {
               found = ds;
               break;
            }
            if(found == null)
               throw new XMLException("Cannot find referenced dataset: " + using, FGModelSchema.EL_SET, 
                     FGModelSchema.A_USING);
            sets.add(found.changeID(id));
         }
      }
      
      int nUsed = model.replaceDataSetsInUse(sets, true);
      if(nUsed != sets.size())
      {
         System.out.println("DBG: nUsed=" + nUsed + "; nSets=" + sets.size());
         throw new XMLException("One or more defined datasets were not used in model!");
      }
   }
   
   
   /**
    * Graphs in <i>DataNav</i> figures created prior to schema version 9 lack a "Z axis". As of version 9, this Z axis
    * controls how XYZIMG data in a {@link ContourNode} are mapped to RGB color in the heat map image. Heat maps,
    * introduced in schema 8, originally mapped the entire observed data range linearly onto a specified color map LUT.
    * During migration of schema 8 to schema 9, the new Z axis is defined to use the LUT for the first contour node 
    * (in schema 9, this was a <b>HeatMapNode</b>, which was deprecated in favor of a more versatile contour node in
    * schema 22) found in the parent graph and defaults to a linear mapping. However, the Z axis range defaults to 
    * [0..100]; it is not set to the observed data range during schema migration because that would require parsing the 
    * data set text content, which is actually put off until the fully migrated schema is converted to a graphic model. 
    * 
    * <p>This method attempts to fix this issue AFTER schema migration is complete. For each 2D graph in the figure that
    * contains at least one contour node, it resets the mapped data range of the Z axis to match the observed data range
    * of the first contour node child found in the graph. This fix addresses the most common use case -- a single heat 
    * map image per graph. If a graph contains more than one heat map, the result will most likely look different from 
    * the schema 8 version, unless all heat maps in the graph used the same color map LUT and all heat map data sets 
    * happen to span the same data range. This is unavoidable; by design, the Z axis controls the color-mapping of 
    * <i>all</i> elements in the parent graph.</p>
    * 
    * <p><i>This fix is not applicable to the specialized 2D polar graph, {@link PolarPlotNode}, which was not 
    * introduced until schema version 22.</i></p>
    * 
    * @param srcVersion The schema version number of the source from which graphic model was built. <b>The fix is 
    * required only if the original schema version is 8, when heat maps were first introduced but a Z axis was lacking.
    * @param model The graphic model. If any changes are made, the model's <i>modified</i> flag will be reset upon 
    * returning.
    */
   private static void fixColorAxisIfNecessary(int srcVersion, FGraphicModel model)
   {
      if(srcVersion != 8) return;
      
      // get a list of all graphs in the figure. We must fix each separately, and graphs can be nested!
      List<GraphNode> graphs = new ArrayList<>();
      FigureNode fig = (FigureNode) model.getRoot();
      Stack<FGraphicNode> nodeStack = new Stack<>();
      nodeStack.push(fig);
      while(!nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();
         if(n instanceof GraphNode) graphs.add((GraphNode)n);
         for(int i=0; i<n.getChildCount(); i++) 
         {
            FGraphicNode child = n.getChildAt(i);
            if(child instanceof GraphNode) nodeStack.push(child);
         }
      }
      
      // fix each graph separately
      for(GraphNode graph : graphs)
      {
         // find first heatmap in graph. If there is none, there's nothing to fix
         ContourNode heatmap = null;
         for(int i=0; i<graph.getChildCount(); i++)
         {
            FGraphicNode child = graph.getChildAt(i);
            if(child instanceof ContourNode)
            {
               heatmap = (ContourNode) child;
               break;
            }
         }
         if(heatmap == null) continue;
         
         graph.getColorBar().setStart(heatmap.getDataSet().getZMin());
         graph.getColorBar().setEnd(heatmap.getDataSet().getZMax());
      }
      
      // reset model's modified flag
      model.setModified(false);
   }
   
   
   /**
    * Convert the specified figure graphic node property value to string form as it would be persisted in the 
    * <i>FypML</i> document defining the figure containing that node.
    * 
    * <p><b>NOTE: This was developed in order to save a {@link FGNStyleSet} as a JSON object in a way that is consistent
    * with the current <i>FypML</i> schema, without having access to the actual graphic node. We've put it here 
    * because it is important that it be updated appropriately each time there is a change in the schema.</b></p>
    * 
    * <p><b>NO LONGER USED</b>. As of V4.7.0 (May 2015), we no longer support a "style set palette" that was preserved
    * in the user's workspace. This method was used to save a style set in JSON format. It and its inverse, {@link 
    * #fgnPropertyFromString}, are no longer used. <i>If the two methods are put back into use, then they probably
    * need a careful vetting since they may not handle more recently added properties.</i></p>
    * 
    * @param nodeType The figure graphic node type.
    * @param prop The node property type.
    * @param value The value of the property. The object's class is ASSUMED to be correct for the identified property.
    * If it is not correct, the method will either return null or the string returned may not be the correct 
    * representation of the property value in string form.
    * @return The property value in string form. Returns null if unable to compute the string form, if any of the
    * arguments are null, or if the class of the value object is not recognized as a valid class for a figure graphic
    * node property value.
    */
   static String fgnPropertyToString(FGNodeType nodeType, FGNProperty prop, Object value)
   {
      if(nodeType == null || prop == null || value == null) return(null);
      
      String strProp = null;
      Class<?> c = value.getClass();
      if(Boolean.class.equals(c) || Integer.class.equals(c) || 
            StrokePattern.class.equals(c) || LogTickPattern.class.equals(c))
         strProp = value.toString();
      else if(String.class.equals(c))
      {
         if(prop == FGNProperty.SRC) strProp = '#' + value.toString();
         else strProp = value.toString();
      }
      else if(Color.class.equals(c))
         strProp = BkgFill.colorToHexString((Color) value);
      else if(Double.class.equals(c))
      {
          strProp = Utilities.toString((Double)value, 7, 3);
      }
      else if(c.isEnum())
      {
         if(TextAlign.class.equals(c))
         {
            // the horizontal and vertical alignment enums for a LabelNode are not translated in the straightforward
            // fashion to a string. The typical left and bottom alignments are implicit values in FypML. Here we map
            // them to empty strings
            TextAlign align = (TextAlign) value;
            if(prop == FGNProperty.HALIGN)
            {
               strProp = "";
               if(align == TextAlign.TRAILING) strProp = FGModelSchema.HALIGN_RIGHT;
               else if(align == TextAlign.CENTERED) strProp = FGModelSchema.HALIGN_CENTER; 
               
            }
            else if(prop == FGNProperty.VALIGN)
            {
               strProp = "";
               if(align == TextAlign.LEADING) strProp = FGModelSchema.VALIGN_TOP;
               else if(align == TextAlign.CENTERED) strProp = FGModelSchema.VALIGN_CENTER; 
            }
         }
         else
            strProp = value.toString();
      }
      else if(Measure.class.equals(c))
      {
         // all measures are converted to a string in the same way, but a measurement constraints object is required to
         // limit the precision of the measured value in string form, and that constraint varies with the property, or
         // with the node type.
         Measure.Constraints con = null;
         if(prop == FGNProperty.X || prop == FGNProperty.X2 || prop == FGNProperty.Y || prop == FGNProperty.Y2)
            con = FGraphicModel.getLocationConstraints(nodeType);
         else if(prop == FGNProperty.WIDTH || prop == FGNProperty.HEIGHT || prop == FGNProperty.DEPTH)
            con = FGraphicModel.getSizeConstraints(nodeType);
         else if(prop == FGNProperty.STROKEW || prop == FGNProperty.BORDER)
            con = FGraphicNode.STROKEWCONSTRAINTS;
         else if(prop == FGNProperty.GAP && (nodeType == FGNodeType.TEXTBOX || nodeType == FGNodeType.IMAGE))
            con = FGraphicNode.STROKEWCONSTRAINTS;
         else if(nodeType == FGNodeType.CALIB)
            con = CalibrationBarNode.ENDCAPSIZECONSTRAINTS;
         else if(nodeType == FGNodeType.AXIS || nodeType == FGNodeType.AXIS3D)
            con = AxisNode.SPACERCONSTRAINTS;
         else if(nodeType == FGNodeType.CBAR)
            con = ColorBarNode.GAPCONSTRAINTS;
         else if(nodeType == FGNodeType.TICKS || nodeType == FGNodeType.TICKS3D)
            con = TickSetNode.TICKLENCONSTRAINTS;
         else if(nodeType == FGNodeType.LEGEND)
            con = LegendNode.LENCONSTRAINTS;
         else if(nodeType == FGNodeType.SYMBOL)
            con = SymbolNode.SYMBOLSIZECONSTRAINTS;
         else if(nodeType == FGNodeType.EBAR)
            con = ErrorBarNode.EBARCAPSIZECONSTRAINTS;
         else if(nodeType == FGNodeType.SCATTER)
            con = ScatterPlotNode.MAXSYMSIZECONSTRAINTS;
         
         if(con != null) strProp = ((Measure) value).toString(con.nMaxSigDigits, con.nMaxFracDigits);
      }
      else if(BkgFill.class.equals(c))
         strProp = ((BkgFill) value).toXML();
            
      return(strProp);
   }
   
   /**
    * The inverse of {@link #fgnPropertyToString(FGNodeType, FGNProperty, Object)}
    * 
    * @param nodeType The figure graphic node type.
    * @param prop The node property type.
    * @param strValue The property value is string form.
    * @return The property value object. The returned object's class depends on the property type and the type of node
    * on which the property is defined. Returns null if string value cannot be converted to an instance of the expected
    * model class.
    */
   static Object fgnPropertyFromString(FGNodeType nodeType, FGNProperty prop, String strValue)
   {
      if(nodeType == null || prop == null || strValue == null) return(null);
      
      Object propObj = null;

      // for properties with Measure values, the big switch just sets the constraints object for the Measure. The
      // conversion happens after the switch...
      Measure.Constraints mCon = null;
      
      switch(prop)
      {
      // all of these properties have Boolean values. The string form must be either "true" or "false".
      case CLIP :
      case PRIMARY :
      case AUTO :
      case LEGEND :
      case AVG :
      case SMOOTH :
      case HIDE :
      case LOG2 :
      case MID :
         if(strValue.equalsIgnoreCase("true")) propObj = Boolean.TRUE;
         else if(strValue.equalsIgnoreCase("false")) propObj = Boolean.FALSE;
         break;

      // these properties are just String values
      case FONTFAMILY :
      case TITLE :
      case ID :
      case NOTE :
      case UNITS :
         propObj = strValue;
         break;
      // the data set ID is just a String value, but it is persisted with a '#' in front that we have to remove
      case SRC : 
         propObj = (strValue.startsWith("#") && strValue.length() > 1) ? strValue.substring(1) : null;
         break;
         
      // these properties are Integers. 
      case FONTSIZE :
      case SKIP :
      case NBINS :
      case SLICEOFS :
      case PSCALE :
      case MESHLIMIT :
         try { propObj = Integer.parseInt(strValue); } catch(NumberFormatException ignored) {}
         break;
      
      // these properties are floating-point values, all mapped to Double 
      case ROTATE :
      case ELEVATE :
      case X0 :
      case X1 :
      case DX :
      case BASELINE :
      case XOFF :
      case YOFF :
      case START :
      case END :
      case INTV :
      case LINEHT :
      case IRAD :
      case ORAD :
         try { propObj = Double.parseDouble(strValue); } catch(NumberFormatException ignored) {}
         break;
     
      // barWidth is an Integer for the bar plot and scatter3d node; else it is a Double
      case BARWIDTH :
         try 
         { 
            if(nodeType==FGNodeType.BAR || nodeType==FGNodeType.SCATTER3D) propObj = Integer.parseInt(strValue);
            else propObj = Double.parseDouble(strValue); 
         } 
         catch(NumberFormatException ignored) {}
         break;
         
      // these properties are all Color values
      case FILLC :
      case STROKEC :
      case CMAPNAN :
      case BOXC :
         propObj = BkgFill.colorFromHexString(strValue);
         break;

      // this property is a BkgFill
      case BKGC :
         propObj = BkgFill.fromXML(strValue);
         break;
         
      // these properties all map to enum objects
      case ALTFONT :
         propObj = Utilities.getEnumValueFromString(strValue, GenericFont.values());
         break;
      case PSFONT :
         propObj = Utilities.getEnumValueFromString(strValue, PSFont.values());
         break;
      case FONTSTYLE :
         propObj = Utilities.getEnumValueFromString(strValue, FontStyle.values());
         break;
      case STROKECAP :
         propObj = Utilities.getEnumValueFromString(strValue, StrokeCap.values());
         break;
      case STROKEJOIN :
         propObj = Utilities.getEnumValueFromString(strValue, StrokeJoin.values());
         break;
      case LAYOUT :
         propObj = Utilities.getEnumValueFromString(strValue, GraphNode.Layout.values());
         break;
      case AUTORANGE :
         propObj = Utilities.getEnumValueFromString(strValue, GraphNode.Autorange.values());
         break;
      case CAP :
         propObj = Utilities.getEnumValueFromString(strValue, Marker.values());
         break;
      case MODE :
         if(nodeType == FGNodeType.TRACE) 
            propObj = Utilities.getEnumValueFromString(strValue, TraceNode.DisplayMode.values());
         else if(nodeType == FGNodeType.RASTER)
            propObj = Utilities.getEnumValueFromString(strValue, RasterNode.DisplayMode.values());
         else if(nodeType == FGNodeType.BAR)
            propObj = Utilities.getEnumValueFromString(strValue, BarPlotNode.DisplayMode.values());
         else if(nodeType == FGNodeType.SCATTER)
            propObj = Utilities.getEnumValueFromString(strValue, ScatterPlotNode.DisplayMode.values());
         else if(nodeType == FGNodeType.SCATTER3D)
            propObj = Utilities.getEnumValueFromString(strValue, Scatter3DNode.DisplayMode.values());
         break;
      case TYPE :
         if(nodeType == FGNodeType.GRAPH)
            propObj = Utilities.getEnumValueFromString(strValue, GraphNode.CoordSys.values());
         else if(nodeType == FGNodeType.SHAPE || nodeType == FGNodeType.SYMBOL || nodeType == FGNodeType.SCATTER)
            propObj = Utilities.getEnumValueFromString(strValue, Marker.values());
         break;
      case CMAP :
         if(nodeType == FGNodeType.SURFACE)
         {
            if(strValue.equalsIgnoreCase("true")) propObj = Boolean.TRUE;
            else if(strValue.equalsIgnoreCase("false")) propObj = Boolean.FALSE;
         }
         else
            propObj = ColorMap.fromString(strValue);
         break;
      case CMODE :
         propObj = Utilities.getEnumValueFromString(strValue, ColorBarNode.CMapMode.values());
         break;
      case EDGE :
         propObj = Utilities.getEnumValueFromString(strValue, ColorBarNode.Edge.values());
         break;
      case DIR :
         propObj = Utilities.getEnumValueFromString(strValue, TickSetNode.Orientation.values());
         break;
      case FMT :
         propObj = Utilities.getEnumValueFromString(strValue, TickSetNode.LabelFormat.values());
         break;

      // these two properties map to enum values, but the mapping is not straightforward...
      case HALIGN :
         if(strValue.isEmpty()) propObj = TextAlign.LEADING;
         else if(strValue.equalsIgnoreCase(FGModelSchema.HALIGN_RIGHT)) propObj = TextAlign.TRAILING;
         else if(strValue.equalsIgnoreCase(FGModelSchema.HALIGN_CENTER)) propObj = TextAlign.CENTERED; 
         break;
      case VALIGN :
         if(strValue.isEmpty()) propObj = TextAlign.TRAILING;
         else if(strValue.equalsIgnoreCase(FGModelSchema.VALIGN_TOP)) propObj = TextAlign.LEADING;
         else if(strValue.equalsIgnoreCase(FGModelSchema.VALIGN_CENTER)) propObj = TextAlign.CENTERED; 
         break;
      
      // these two properties map to unique classes which themselves handle the conversion from string form
      case STROKEPATN : propObj = StrokePattern.fromString(strValue); break;
      case PERLOGINTV : propObj = LogTickPattern.fromString(strValue); break;
      
      // these are Measure-valued properties that have to be constrained after conversion to a Measure. Here we just
      // specify the constraints object. The conversion is then done after the switch.
      case STROKEW :
      case BORDER :
         mCon = FGraphicNode.STROKEWCONSTRAINTS;
         break;
      case X :
      case Y :
      case X2 :
      case Y2 :
         mCon = FGraphicModel.getLocationConstraints(nodeType);
         break;
      case WIDTH :
      case DEPTH :
         mCon = FGraphicModel.getSizeConstraints(nodeType);
         break;
      case SIZE :
         // this Measure property has different constraints depending on node type to which it is assigned
         if(nodeType == FGNodeType.CBAR) mCon = ColorBarNode.GAPCONSTRAINTS;
         else if(nodeType == FGNodeType.LEGEND) mCon = LegendNode.LENCONSTRAINTS;
         else if(nodeType == FGNodeType.SYMBOL) mCon = SymbolNode.SYMBOLSIZECONSTRAINTS;
         else if(nodeType == FGNodeType.SCATTER) mCon = ScatterPlotNode.MAXSYMSIZECONSTRAINTS;
         break;
      case CAPSIZE :
         // this Measure property has different constraints depending on node type to which it is assigned
         if(nodeType == FGNodeType.CALIB) mCon = CalibrationBarNode.ENDCAPSIZECONSTRAINTS;
         else if(nodeType == FGNodeType.EBAR) mCon = ErrorBarNode.EBARCAPSIZECONSTRAINTS; 
         break;
      case LABELOFFSET :
         // this Measure property has different constraints depending on node type to which it is assigned
         if(nodeType==FGNodeType.AXIS || nodeType==FGNodeType.AXIS3D) mCon = AxisNode.SPACERCONSTRAINTS;
         else if(nodeType == FGNodeType.CBAR) mCon = ColorBarNode.GAPCONSTRAINTS;
         else if(nodeType == FGNodeType.LEGEND) mCon = LegendNode.LENCONSTRAINTS;
         break;
      case GAP :
         // this Measure property has different constraints depending on node type to which it is assigned
         if(nodeType == FGNodeType.CBAR) mCon = ColorBarNode.GAPCONSTRAINTS;
         else if(nodeType==FGNodeType.TICKS || nodeType==FGNodeType.TICKS3D) mCon = TickSetNode.TICKLENCONSTRAINTS; 
         else if(nodeType == FGNodeType.TEXTBOX || nodeType == FGNodeType.IMAGE) 
            mCon = FGraphicNode.STROKEWCONSTRAINTS;
         break;

      // the value class for each of these properties vary depending on the node type to which property is assigned
      case SPACER :
         if(nodeType == FGNodeType.RASTER)
         {
            try { propObj = Integer.parseInt(strValue); } catch(NumberFormatException ignored) {}
         }
         else if(nodeType==FGNodeType.AXIS || nodeType==FGNodeType.AXIS3D) mCon = AxisNode.SPACERCONSTRAINTS;
         else if(nodeType == FGNodeType.CBAR) mCon = ColorBarNode.GAPCONSTRAINTS;
         else if(nodeType == FGNodeType.LEGEND) mCon = LegendNode.LENCONSTRAINTS;
         break;
      case HEIGHT :
         if(nodeType == FGNodeType.RASTER)
         {
            try { propObj = Integer.parseInt(strValue); } catch(NumberFormatException ignored) {}
         }
         else
         {
            // for all other node types, HEIGHT is a Measure
            mCon = FGraphicModel.getSizeConstraints(nodeType);
         }
         break;
      case LEN :
         if(nodeType == FGNodeType.CALIB)
         {
            try { propObj = Double.parseDouble(strValue); } catch(NumberFormatException ignored) {}
         }
         else if(nodeType == FGNodeType.TICKS) mCon = TickSetNode.TICKLENCONSTRAINTS; 
         else if(nodeType == FGNodeType.LEGEND) mCon = LegendNode.LENCONSTRAINTS; 
         break;

      default :
         break;
      }
      
      // convert Measure-valued properties, if we can
      if(mCon != null)
      {
         Measure m = Measure.fromString(strValue);
         if(m != null) propObj = mCon.constrain(m);
      }
      
      return(propObj);
   }

}
