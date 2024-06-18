package com.srscicomp.fc.fig;

import java.util.ArrayList;
import java.util.List;


/**
 * <b>FGNPlottable</b> is an abstract extension of {@link FGraphicNode} that defines and partially implements 
 * functionality common to a data presentation node or mathematical function node that is rendered in a graph. One of
 * two possible graph "container" nodes establish the coordinate system in which data  is presented: {@link GraphNode} 
 * defines a 2D graph with some support for representing 3D data using a color map, while {@link Graph3DNode} is a true 
 * 3D graph that is drawn on the 2D canvas using perspective projection.
 * 
 * <p>The method {@link #is3D()} identifies a 3D data presentation node, while {@link #hasZData()} identifies 3D 
 * presentation nodes  as well as 2D nodes that represent 3D data. The convenience methods {@link #getParentGraph()} and
 * {@link #getParentGraph3D()} returns references to the respective 2D and 3D graph containers.</p>
 * 
 * @author sruffner
 */
public abstract class FGNPlottable extends FGraphicNode implements Cloneable
{
   /**
    * Construct a new data presentation node.
    * @param attrFlags The attribute flags relevant to this node. See {@link FGraphicNode#FGraphicNode(int)}.
    */
   FGNPlottable(int attrFlags) { super(attrFlags); }

   /**
    * Initialize the default data set or mathematical function assigned to this 2D or 3D data presentation node when it 
    * is first created. This method is called prior to the node's insertion into its 2D or 3D graph container.
    * 
    * <p>A newly constructed node has no data to render, so nothing would "change" on screen when such a node is 
    * inserted into its graph container. This method's main purpose is to initialize the presentation node with a 
    * suitable data set (or mathematical function) so that something is rendered when the node is inserted into the 
    * graph. Implementations should use the axis ranges provided to decide how to define a reasonable data set or 
    * function. <i>The base implementation does nothing</i>.</p>
    * 
    * <p><b>NOTE</b>: This method is called <b>just prior</b> to the node's insertion into the figure model, so any 
    * changes made to the node will have no effect on the model's current state. Implementations must NOT make any
    * changes to the model, only to this data presentation node.</p>
    * 
    * @param axisRng A six-element array specifying the axis ranges for the parent graph container: <i>[x0 x1 y0 y1 
    * z0 z1]</i>. Axis range endpoints are specified in native coordinate system units, or "user" units. For 2D graphs,
    * <i>[z0 z1]</i> specifies the Z-data range mapped to the graph's color axis -- as 2D graphs generally use color to
    * represent a third dimension.
    * @param isPolar True if 2D graph is polar. This will always be false for a 3D graph container.
    */
   protected void initializeDefaultData(double[] axisRng, boolean isPolar) {}
   
   /**
    * Return a reference to the graph container for this data presentation node. 
    * 
    * @return The parent graph container. Returns null if this node currently has no parent graph container. 
    * Presentation nodes may only be rendered within the context of a graph container.
    */
   protected FGNGraph getParentGraph()
   {
      FGraphicNode p = getParent();
      if(!(p instanceof FGNGraph)) return(null);
      return((FGNGraph)p);
   }

   /**
    * Return a reference to the 3D graph container for this data presentation node.
    * @return The parnt 3D graph container, or null if the parent is not an instance of {@link Graph3DNode}.
    */
   protected Graph3DNode getParentGraph3D()
   {
      FGNGraph g = getParentGraph();
      return((g!=null && g.getNodeType()==FGNodeType.GRAPH3D) ? ((Graph3DNode)g) : null);
   }
   
   /** The last computed data range <i>[x0, x1, y0, y1, z0, z1]</i> for this data presentation node. */
   protected final float[] cachedDataRange = new float[] {0, 0, 0, 0, 0, 0};
   private boolean isRangeInitd = false;
   
   /**
    * Get the data range spanned by all well-defined points in the rendering of this 2D or 3D data presentation node. 
    * 
    * <p>Note that the axis auto-scaling mechanism in 2D graphs relies on this method to determine the axis ranges 
    * needed to render all 2D graph data within the data window. Auto-ranging is not supported in 3D graphs.</p>
    * 
    * <p>Implementing classes are expected to cache the current data range in the protected 6-element array member
    * <i>cachedDataRange</i>. This ensures that the presentation node can detect whenever a change in the data range 
    * occurs (due to a change in the underlying data set or in some node property that impacts the data range) -- see 
    * {@link #recalcDataRange}. This method returns a cloned copy of <i>cachedDataRange</i>.</p>
    * 
    * @return An array of six values, in order: minimum X-coordinate, maximum X-coordinate, minimum Y-coordinate, 
    * maximum Y-coordinate, minimum Z-coordinate, maximum Z-coordinate. If the node has no well-defined data for some 
    * reason, the returned range should be <i>[0 0 0 0 0 0]</i>.
    */
   final protected float[] getDataRange() 
   { 
      if(!isRangeInitd)
      {
         recalcDataRange(null);
         isRangeInitd = true;
      }
      return(cachedDataRange.clone()); 
   }

   /**
    * Recalculate the current data range for this data presentation node in response to a specified property change. If
    * the range has changed, the new range <i>[xMin, xMax, yMin, yMax, zMin, zMax]</i> must be stored in the protected 
    * member array <i>cachedDataRange</i>. In fact, implementing classes should compare the newly calculated range with 
    * the cached range to determine whether or not the range actually changed.
    * 
    * <p>This method is an important part of the graph axis auto-scaling mechanism for the 2D graph container, {@link
    * GraphNode}. It is invoked whenever a node property changes (via {@link #onNodeModified}. Implementing classes
    * should check the hint object for an indication of what property changed. If that property cannot affect the data 
    * range, then a recalculation is unnecessary.</p>
    * 
    * @param hint An object used to identify what aspect of the node's definition has changed. Check it to determine
    * whether or not a data range recalculation is even necessary. May be null, in which case the data range MUST be
    * recalculated.
    * @return True if data range has changed; false if a recalculation was not necessary or if the range is unchanged.
    */
   protected abstract boolean recalcDataRange(Object hint);
   
   /**
    * Retrieve the symbol node component that governs the appearance of this data presentation node's marker symbols, 
    * if it has any. <i>The base class implementation returns <b>null</b></i>. Any implementing class that has a symbol
    * node component must override.</i>
    * 
    * @return A non-renderable graphic node defining the marker symbols for this presentation node. May be null if the 
    * implementation does not support marker symbols.
    */
   public SymbolNode getSymbolNode() { return(null); }

   /**
    * Does this data presentation node's rendering potentially use marker symbols?
    * 
    * <p><i>IMPORTANT</i>. This question is subtly different from whether or not marker symbols are actually drawn.
    * They will not be drawn if the presentation node's current definition precludes them, or if the current marker 
    * symbol, as defined by the component node returned by {@link #getSymbolNode()}, makes no marks when rendered. This 
    * method should address the previous condition, NOT the latter!</p>
    * 
    * <p><i>The base class implementation returns <b>false</b> always. Override if necessary.</i></p>
    * 
    * @return True if marker symbols are potentially included in rendering.
    */
   public boolean usesSymbols() { return(false); }

   /**
    * Does this data presentation node include any error (or variance) information in its rendering? <i>The base class 
    * implementation returns <b>false</b> always. Override if necessary.</i>
    * @return True if and only if error data is portrayed in the rendering.
    */
   public boolean hasErrorData() { return(false); }

   /** 
    * Is this a 3D data presentation node, which must be rendered in the 3D graph container, {@link Graph3DNode}? <i>The
    * base implementation returns false; any implementing class for a true 3D presentation node must override.</i>
    * @return True if and only if this is a 3D data presentation node.
    */
   public boolean is3D() { return(false); }
   
   /**
    * Does this data presentation node render 3D data? <i>The base implementation returns true for any 3D data 
    * presentation node, as determined by {@link #is3D()}. Any 2D presentation node that renders data in the Z dimension
    * must override.</i>
    * @return True if data presentation node can represent data in the Z dimension.
    */
   public boolean hasZData() { return(is3D()); }
   
   /**
    * Respond to a change in the color map in this data presentation node's 2D graph container. If the node is affected
    * by a change in the graph's color map, the implementation should update its internal rendering infrastructure
    * accordingly and return true -- thereby ensuring that the node will be re-rendered in short order. <i>This base
    * class implementation does nothing and returns false</i>.
    * 
    * @return True if node's appearance is affected by a change in the 2D graph container's color map.
    */
   public boolean onColorMapChange() { return(false); }
   
   /**
    * Does this data presentation node support a corresponding entry in the parent graph's automated legend? If so, 
    * <b>FGNPlottable</b> provides default support for hiding/showing the legend entry and getting the legend entry 
    * label.
    * 
    * <p><i>This base class implementation returns <b>true</b> always. Any 2D or 3D presentation node that does NOT 
    * support a legend entry must override this method.</i></p>
    * 
    * @return True if and only if this data presentation node supports a legend entry.
    */
   public boolean isLegendEntrySupported() { return(true); }
   
   /**
    * If set, an entry for this data presentation node is included in the parent graph's automated legend. If the 
    * particular implementation does not support a legend entry, this flag is ignored.
    */
   private boolean showInLegend = true;

   /**
    * Is an entry for this data presentation node included in the parent graph's automated legend?
    * 
    * @return True if and only if legend entries are supported and enabled.
    */
   public boolean getShowInLegend() { return(isLegendEntrySupported() && showInLegend); }

   /**
    * Include or exclude an entry for this data presentation node in the parent graph's automated legend. If a change is
    * made, the method {@link #onNodeModified} in invoked. If this node does not support a legend entry, this method
    * has no effect.
    * @param b True to enable, false to disable the legend entry.
    */
   public void setShowInLegend(boolean b)
   {
      if(isLegendEntrySupported() && (b != showInLegend))
      {
         if(doMultiNodeEdit(FGNProperty.LEGEND, b)) return;
         
         Boolean old = showInLegend;
         showInLegend = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.LEGEND);
            FGNRevEdit.post(this, FGNProperty.LEGEND, showInLegend, old);
         }
      }
   }

   /**
    * Same as {@link #setShowInLegend}, but no undo operation is posted, and {@link #onNodeModified} is not invoked.
    * @param b True to enable, false to disable the legend entry.
    */
   protected void setShowInLegendNoNotify(boolean b) { if(isLegendEntrySupported()) showInLegend = b; }
   
   /**
    * Get the number of legend entries associated with this data presentation node. See {@link #getLegendEntries()} for
    * more information. 
    * @return The number of legend entries. Returns 0 if a legend entry is not supported by this node, or if the entry
    * is currently hidden. Otherwise returns 1. <i>Must override if node has multiple associated legend entries.</i>
    */
   public int getNumLegendEntries()  { return(getShowInLegend() ? 1 : 0); }
   
   /**
    * Get the legend entry or entries associated with this data presentation node. Typically, there will be one entry
    * per node, but a composite node like a bar plot will have one entry per bar group. 
    * <p>This base-class implementation returns null if the node does not support a legend entry or the legend entry is 
    * currently hidden; otherwise, it returns a one-element list containing the {@link LegendEntry} object representing
    * this presentation node.</p>
    * @return The list of legend entries (typically just one) associated with this data presentation node. Will be null
    * if a legend entry is not supported by this node, or if the entry is currently hidden.
    */
   public List<LegendEntry> getLegendEntries()
   {
      if(!getShowInLegend()) return(null);
      List<LegendEntry> out = new ArrayList<>();
      out.add(new LegendEntry(this, null));
      return(out);
   }
   
   /**
    * Does this data presentation node use a rectangular bar for its legend entry (or entries)? 
    * <p>This base-class implementation returns false always.</p>
    * @return True if legend entry should be a rectangular bar; false if it should be a line segment.
    */
   public boolean useBarInLegend() { return(false); }
   
   /**
    * Overridden to trigger the axis auto-scaling feature in the parent graph container when this data presentation 
    * node's data range has changed. If the modified property is not one of the special cases below, the method calls 
    * {@link #recalcDataRange}, passing along the provided hint. This function will return true if and only if the
    * node's data range changed, in which case the parent graph is auto-scaled. If the graph is indeed rescaled, no 
    * further action is taken, since that will trigger a re-rendering of the graph and all of its children. If the graph
    * is not rescaled, the property change is forwarded to the super-class implementation for default handling.
    * 
    * <p>NOTE that the only graph container that supports the auto-scaling feature is {@link GraphNode}.</p>
    * 
    * <p><i>Special cases:</i>
    * <ul>
    * <li>Changes in the <i>title</i> attribute do not affect the node's rendering.</li>
    * <li>If this presentation node supports a corresponding legend entry: (1) If the parent graph's legend is visible,
    * it must be re-rendered IF this node's <i>showInLegend</i> flag is set. (2) Toggling the flag has no effect on this
    * node's rendering, but it will affect the rendering of the parent graph's legend.</li>
    * <li>Changing a font-related style will affect the node's rendering only if it includes marker symbols that have 
    * a character centered within the symbol.</li>
    * </ul>
    * </p>
    * 
    * <p><b>NOTE</b> that the 3D graph container, {@link Graph3DNode}, does not support axis auto-scaling.</p>
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      // in certain situations, we do not need to re-render this node
      boolean skip = (hint == FGNProperty.TITLE);
      if((!skip) && isLegendEntrySupported()) skip = (hint == FGNProperty.LEGEND);
      if((!skip) && (hint instanceof FGNProperty) && FGNProperty.isFontProperty((FGNProperty)hint))
      {
         SymbolNode sn = getSymbolNode();
         skip = (sn == null) || (sn.getSizeInMilliInches() == 0) || (sn.getCharacter().isEmpty());
      }
      if(skip)
         model.onChange(this, 0, false, null);
      else
      {
         if(recalcDataRange(hint))
         {
            FGNGraph g = getParentGraph();
            if(g != null && g.autoScaleAxes()) return;
         }
         super.onNodeModified(hint);
      }

      // if legend entry is supported: if parent graph's automated legend is on, and the data presentation node's legend
      // entry is enabled or the "showInLegend"  property was just changed, then we assume the legend will be affected 
      // (although in some cases it won't be)
      if(isLegendEntrySupported() && (hint == FGNProperty.LEGEND || showInLegend))
      {
         // note: there are two possible graph containers - 2D and 3D.
         FGNGraph g = getParentGraph(); 
         if(g != null)
         {
            LegendNode legend = g.getLegend();
            if(!legend.getHide()) legend.onNodeModified(null);
         }
      }
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      if(p == FGNProperty.LEGEND)
      {
         setShowInLegend((Boolean)propValue);
         return(true);
      }
      else return(super.setPropertyValue(p, propValue));
   }
   
   @Override Object getPropertyValue(FGNProperty p)
   {
      return((p == FGNProperty.LEGEND) ? Boolean.valueOf(getShowInLegend()) : super.getPropertyValue(p));
   }

   @Override protected FGNPlottable clone() throws CloneNotSupportedException
   {
      return((FGNPlottable) super.clone());
   }
}