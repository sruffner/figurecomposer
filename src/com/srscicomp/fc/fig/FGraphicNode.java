package com.srscicomp.fc.fig;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.*;
import java.util.regex.Pattern;

import com.srscicomp.common.g2dutil.PainterStyle;
import com.srscicomp.common.g2dutil.ResizeAnchor;
import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dutil.StrokeJoin;
import com.srscicomp.common.g2dviewer.Focusable;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.GenericFont;
import com.srscicomp.common.ui.LocalFontEnvironment;
import com.srscicomp.common.ui.UnicodeSubset;
import com.srscicomp.common.util.Utilities;

/**
 * <code>FGraphicNode</code> is the base class for all graphics objects in the <em>DataNav</em> document model.
 * 
 * <p>The <em>DataNav</em> graphics document is designed as a hierarchical tree of graphic nodes, all of which 
 * extend from <code>FGraphicNode</code>. This base class provides the basic machinery for building up this tree of 
 * nodes, it manages a number of node attributes that are common to all or most graphic node classes, and it provides 
 * an infrastructure for rendering a graphic node and updating that infrastructure when the node's definition 
 * changes.</p>
 * 
 * <p><em>Tree structure; public vs private subordinate nodes</em>. The parent-subordinate relationship among graphic 
 * nodes is fundamental to <em>DataNav</em>'s rendering model. A subordinate is rendered WRT the viewport of the parent 
 * node, and it may inherit the parent's styling. The <code>FGraphicNode</code> infrastructure supports two sets of 
 * subordinates: public vs private. Public subordinate nodes -- hereafter called, simply, <em>child nodes</em> --  can 
 * be freely accessed, inserted, and removed via public methods defined on <code>FGraphicModel</code>. Private 
 * subordinate nodes, hereafter called <em>component nodes</em>, are accessible via public methods, but they can only 
 * be added and removed using protected <code>FGraphicNode</code> methods. Whether or not a node has any component 
 * nodes is determined entirely by the implementing subclasses of <code>FGraphicNode</code>.</p>
 * 
 * <p>As an example, take the node that encapsulates a graph, which is by far the most complex graphic object in 
 * <em>DataNav</em>. A graph can contain any number of data traces, as well as text labels, line segments, and shapes. 
 * They are not absolutely required to define a graph; they serve as building blocks by which the <em>DataNav</em> 
 * author creates a unique graphical display of data. These are all examples of child nodes. A graph also has two axes, 
 * grid lines, and an automated legend. But these graphical elements are <em>intrinsic</em> to the graph; they are part 
 * of its definition, make no sense outside the context of a parent graph, and cannot be inserted or removed willy-nilly 
 * by the <em>DataNav</em> user. They are all <em>component nodes</em>.</p>
 * 
 * <p>Component nodes help to break down the task of rendering a complex object like a graph. Since they're implemented 
 * as distinct graphic nodes and are hooked into the graphic node tree, they can take advantage of the 
 * <code>FGraphicNode</code> infrastructure for rendering, hit-testing, display focus, and style inheritance. But since 
 * they are not public child nodes, they are not exposed in the "public" view of the graphic model.</p>
 * 
 * <p>Component nodes may or may not be "UI-displayable". A UI-displayable component node typically renders itself, is 
 * intended to hold the display focus in the <em>DataNav</em>, and should be made visible in a navigable tree view of 
 * the <code>FGraphicModel</code>. It may even have subordinate nodes. A non-UI-displayable node merely provides 
 * additional information to define its parent; it should not be directly accessible in the GUI. The axes and legend of 
 * a graph are examples of UI-displayable nodes, while the specialized components that define the styling of marker 
 * symbols and error bars on a data trace are not UI-displayable. The latter two component nodes exist only so that 
 * a data trace's marker symbols and error bars can be styled differently from the connecting polyline, if the 
 * <em>DataNav</em> author so chooses.</p>
 * 
 * <p>Most graphic nodes have "x" and "y" properties that specify the node's location WRT a parent viewport, some 
 * nodes have "w" and "h" properties that specify the size of the node, a few have a "rotate" property that gives 
 * the orientation of the graphic WRT the parent, and many have a generic "title" property. While these properties are 
 * not universally shared by all graphic node classes in <em>DataNav</em>, there is sufficient overlap to warrant 
 * implementing them in the base class.</p>
 * 
 * @author sruffner
 */
public abstract class FGraphicNode implements Focusable, PainterStyle, PSTransformable, Cloneable
{
   /**
    * Construct a new <b>FGraphicNode</b>. It initially has neither children nor parent, and is thus disconnected from 
    * the figure model. All style and positioning attributes managed by <b>FGraphicNode</b> itself are enabled. The 
    * node's title is configured as single-line, plain-text only.
    */
   FGraphicNode()
   {
      this(HASATTRMASK);
   }

   /**
    * Construct a new graphic node. It initially has neither children nor parent, and is thus disconnected from any 
    * figure graphics model.
    * 
    * <p>This base graphic node implementation provides support for a number of properties that are shared by most if 
    * not all of the different types of graphic nodes in the figure graphics model. These include the style-related 
    * attributes, as well as attributes defining the location, size and orientation of the node with respect to a parent 
    * container. The first argument should include some combination of the following bit flags to selectively 
    * enable/disable support for these shared properties:
    * <ul>
    * <li>{@link #HASFONTATTRS} -- Enables support for all font-related attributes (font family, style, size, alternate 
    * font, Postscript font).</li>
    * <li>{@link #HASFILLCATTR} -- Enables support for the text/fill color attribute.</li>
    * <li>{@link #HASSTROKEATTRS} -- Enables support for stroke color, width, cap and join attributes.</li>
    * <li>{@link #HASSTRKPATNATTR} -- Enables support for the stroke pattern attribute.</li>
    * <li>{@link #HASLOCATTRS} -- Enables support for the <i>(x,y)</i> coordinates of the node's "location" with respect
    * to a parent viewport. It includes support for moving the node interactively.</li>
    * <li>{@link #HASSIZEATTRS} -- Enables support for the <i>width</i> and <i>height</i> attributes, which specify a 
    * node's size relative to a parent viewport.</li>
    * <li>{@link #HASROTATEATTR} -- Enables support for the <i>rotate</i> attribute, which sets a node's orientation 
    * with respect to a parent viewport.</li>
    * <li>{@link #HASTITLEATTR} -- Enables support for the <i>title</i> attribute, a simple string token that can be 
    * any sequence of <i>FypML</i>-supported characters, including the empty string.</li>
    * <li>{@link #HASIDATTR} -- Enables support for the <i>id</i> attribute, a simple string token that can be any
    * sequence of <i>FypML</i>-supported characters, including the empty string. Unlike the <i>title</i> attribute, the
    * node ID is NEVER rendered and it must uniquely identify the graphic node among all other graphic nodes in the
    * containing figure model.</li>
    * <li>{@link #ALLOWLFINTITLE} -- If set, <i>title</i> attribute may contain linefeed ('\n') characters.</li>
    * <li>{@link #ALLOWATTRINTITLE} -- If set, <i>title</i> attribute supports "styled text format".</li>
    * </ul>
    * </p>
    * 
    * <p>All style attributes are initially implicit, meaning their value are inherited from an ancestor node. For the 
    * root node, the implicit value is the preferred value maintained statically in {@link FGNPreferences}. The other 
    * attributes handled by this base class always have explicit values; they are not inheritable.</p>
    * 
    * @param attrFlags Bit flags that selectively enable/disable graphic node base class-support for various common 
    * properties, as described above.
    */
   FGraphicNode(int attrFlags)
   {
      this.parent = null;
      this.subordinates = new ArrayList<>();
      this.nComponents = 0;
      this.attrFlags = HASATTRMASK & attrFlags;
      this.attrFlags |= ((ALLOWLFINTITLE|ALLOWATTRINTITLE) & attrFlags);
   }

   /**
    * Get the <code>FRootGraphicNode</code> that lies at the root of the graphic node tree containing this 
    * <code>FGraphicNode</code> resides. If the node is not currently a member of a properly formed tree (because it 
    * was just created, or just removed from a tree), the method returns <code>null</code>.
    * 
    * @return The root graphic node, as described.
    */
   protected final FRootGraphicNode getRootGraphicNode()
   {
      FGraphicNode n = this;
      while(n != null && !(n instanceof FRootGraphicNode))
         n = n.parent;
      return( (n != null) ? (FRootGraphicNode) n : null );
   }

   /**
    * Get a reference to the <em>DataNav</em> graphics model in which this <code>FGraphicNode</code> appears. By 
    * convention, the model's root graphic node (the figure element) maintains a reference to the owning model. The 
    * method searches up this node's ancestors until it finds an instance of <code>FRootGraphicNode</code>, from which 
    * it can obtain a reference to the containing model.
    * 
    * @return The containing graphic model, or <code>null</code> if no such model was found (which will be the case 
    * when the node has not been inserted into a valid graphic model.
    */
   public final FGraphicModel getGraphicModel()
   {
      FRootGraphicNode root = getRootGraphicNode();
      return((root != null) ? root.getOwnerModel() : null);
   }
   
   
   //
   // Support for hierarchical tree structure of <em>DataNav</em> graphic nodes: child and/or component nodes
   //

   /**
    * The graphic node that parents this one. If <code>null</code>, then this must be the root node, or the node is 
    * in the process of being removed or inserted into a <em>DataNav</em> graphics tree.
    */
   private FGraphicNode parent;

   /**
    * The list of graphic nodes that are contained by and rendered WRT to this node. Both public child nodes and 
    * private component nodes are included in this list. All components appear first, followed by any children. 
    */
   private List<FGraphicNode> subordinates;

   /**
    * The number of nodes in this <code>FGraphicNode</code>'s subordinate list that are private component nodes. Such
    * nodes are not publicly accessible for removal/insertion. All component nodes appear at the front of the list of 
    * subordinates, in the order they were added.
    */
   private int nComponents;

   /**
    * Get the type of graphic node represented by this <code>FGraphicNode</code>. This provides an alternative to 
    * identifying the type of node by its <code>Class</code>.
    * 
    * <p>Each fully-realized subclass of <code>FGraphicNode</code> should represent a distinct type of graphic node. The 
    * enumeration <code>FGNodeType</code> lists all graphic node types in the <em>DataNav</em> graphic model.</p>
    * 
    * @return A graphic node type.
    * @see FGNodeType
    */
   public abstract FGNodeType getNodeType();
   
   /**
    * Can the specified type of graphic node be inserted as a child of this <code>FGraphicNode</code>?
    * 
    * <p>This method applies only to the public children of a parent node. Component nodes intrinsic to the parent's 
    * definition are not inserted/removed via the publicly accessible API.</p>
    * 
    * @param nodeType The type of graphic node to be inserted.
    * @return <code>True</code> iff specified node type may be inserted as a child of this node; <code>false</code> if 
    * argument is <code>null</code>.
    */
   public abstract boolean canInsert(FGNodeType nodeType);

   /**
    * Is this node a private component node, intrinsic to the definition of its parent?
    * 
    * <p>This method lets the "outside world" distinguish component nodes from child nodes. Component nodes cannot be 
    * removed or inserted via the relevant publicly accessible methods in <code>FGraphicModel</code>. Implementing 
    * classes have complete control over their component nodes (if they have any!).</p>
    * 
    * <p>This default implementation returns <code>false</code> always. Any implementing class that represents a 
    * component node must override.</p>
    * 
    * @return <code>True</code> iff this is component node.
    */
   public boolean isComponentNode() { return(false); }

   /**
    * Is this <code>FGraphicNode</code> a leaf node, meaning that it does not admit the insertion or removal of 
    * child nodes?
    * 
    * <p>The method is intended for use by the <em>DataNav</em> GUI element that presents a navigable tree view of the 
    * containing <code>FGraphicModel</code>. This default implementation returns <code>false</code> always. Any 
    * implementing class which allows no children <em>(or only contains component nodes that should not be exposed in 
    * the tree view)</em> must override this, returning <code>true</code>.
    * 
    * @return <code>True</code> iff node if a leaf in the graphic node tree, as described.
    */
   public boolean isLeaf() { return(false); }

   /**
    * Get the number of child nodes in this <code>FGraphicNode</code>.
    * 
    * @return Number of children currently parented by this graphic node.
    */
   public final int getChildCount() { return(subordinates.size() - nComponents); }

   /**
    * Retrieve the child graphic node at the specified position in this <code>FGraphicNode</code>'s list of children.
    * 
    * @param index Position within child list.
    * @return The child node at the specified position.
    * @throws IndexOutOfBoundsException if index is invalid.
    */
   public final FGraphicNode getChildAt(int index) throws IndexOutOfBoundsException
   {
      return(subordinates.get(index + nComponents));
   }

   /**
    * Find the position of the specified graphic node in this <code>FGraphicNode</code>'s list of children.
    * 
    * @param n The graphic node for which an index position is sought.
    * @return The node's position in the list of children, or -1 if it was not found.
    */
   public final int getIndexOf(FGraphicNode n)
   {
      int index = subordinates.indexOf(n);
      return((index<nComponents) ? -1 : index-nComponents);
   }

   /**
    * Append the specified graphic node to the end of this <code>FGraphicNode</code>'s list of children. This is a
    * convenient shortcut for invoking {@link #insert(FGraphicNode, int)}.
    * 
    * @param n The graphic node to be appended.
    * @return <code>True</code> iff the node was successfully appended.
    */
   final boolean append(FGraphicNode n)
   {
      return(insert(n, -1));
   }

   /**
    * Insert the specified graphic node at the specified position in this <code>FGraphicNode</code>'s list of children. 
    * 
    * <p>Position in the child list reflects a graphic node's rendering order: after rendering itself, the parent 
    * invokes the <code>render()</code> method of each child node as they appear in the child list.</p>
    * 
    * <p>The node will be inserted if it is not <code>null</code>, if it does not already have a parent, and if it 
    * passes the <code>canInsert(FGNodeType)</code> test. After insertion, {@link #onSubordinateNodeInserted} is
    * invoked to update the rendering infrastructure of the inserted node and to recompute the rendered bounds of this 
    * node and its ancestors. Typically, it will trigger a re-rendering of the graphic model in the regions affected by 
    * the insertion.</p>
    * 
    * <p>[<i>NOTE</i>. Subclasses must <b>NOT</b> use this method to insert a private component node, since doing so 
    * will expose the component as a public child node subject to removal by the user. Use {@link #addComponentNode}
    * instead.]</p>
    * 
    * @param n The graphic node to be inserted. It must be non-<code>null</code>, must have no parent, and must not 
    * be an ancestor of this node.
    * @param index Insertion position. If this is not a valid position in the current child list, the method will 
    * append the node at the end of the list.
    * @return True if the node insertion succeeded.
    */
   final boolean insert(FGraphicNode n, int index)
   {
      int i = (index < 0 || index > getChildCount()) ? getChildCount() : index;
      boolean ok = false;
      if((n != null) && (n.parent == null) && !isDescendantOf(n) && !n.isComponentNode() && canInsert(n.getNodeType()))
      {
         n.parent = this;
         subordinates.add(i+nComponents, n);
         onSubordinateNodeInserted(n);
         ok = true;
      }
      return(ok);
   }

   /**
    * Is this <code>FGraphicNode</code> a descendant of the specified node?
    * 
    * @param n A graphic node. 
    * @return <code>True</code> iff specified node is an ancestor of this node. Returns <code>false</code> if node is 
    * <code>null</code>.
    */
   public final boolean isDescendantOf(FGraphicNode n)
   {
      if(n == null) return(false);
      FGraphicNode ancestor = parent;
      while(ancestor != null)
      {
         if(ancestor == n) return(true);
         ancestor = ancestor.parent;
      }
      return(false);
   }

   /**
    * Remove a graphic node from the specified position in this <code>FGraphicNode</code>'s list of children.
    * 
    * <p>If the index position is valid, the child node is removed from this node's child list, and its parent is set 
    * to <code>null</code>. Before releasing the removed node's rendering infrastructure, the method {@link 
    * #onSubordinateNodeRemoved(FGraphicNode)} is invoked to update the rendering infrastructure of this node and its 
    * ancestors and trigger a rendering pass that will reflect the node's removal.</p>
    * 
    * <p>[<em>NOTE</em>. Subclasses <b>cannot</b> use this method to remove a private component node. Use {@link 
    * #removeComponentNode(FGraphicNode)} instead.]</p>
    * 
    * @param index Position of node to be removed.
    * @return The node removed. It will have no parent, and its rendering infrastructure will be reset.
    * @throws IndexOutOfBoundsException if <code>index</code> is not a valid position in this node's child list.
    */
   final FGraphicNode remove(int index) throws IndexOutOfBoundsException
   {
      FGraphicNode n = subordinates.remove(index + nComponents);
      n.parent = null;
      onSubordinateNodeRemoved(n);
      n.releaseRenderResources();
      return(n);
   }

   /**
    * Remove the specified child graphic node from this <code>FGraphicNode</code>'s list of children.
    * 
    * <p>[<em>NOTE</em>. Subclasses <strong>cannot</strong> use this method to remove a private component node. Use 
    * <code>removeComponentNode()</code> instead.]</p>
    * 
    * @param child The node to be removed. If <code>null</code> or not a child of this node, the method aborts silently.
    */
   final void remove(FGraphicNode child)
   {
      int index = getIndexOf(child);
      if(index >= 0)
         remove(index);
   }

   /**
    * Change the position of the specified graphic node in this <code>FGraphicNode</code>'s list of children. The 
    * containing model is notified of the change and is re-rendered within the <em>current</em> rectangular bounds of 
    * the specified node.
    * 
    * <p>Since the order in the children list reflects rendering order, this method provides a means of moving child 
    * nodes up or down in the "Z-order". Component nodes cannot be reordered using this method.</p>.
    * 
    * @param child The graphic node to be repositioned. Must be a child of this node.
    * @param pos The new position. Must be in <em>[0..N-1]</em>, where <em>N</em> is the number of children in the list.
    * @return <code>True</code> iff successful. <code>False</code> if specified node is not a child of this node, or 
    * the specified position is invalid. Returns <code>true</code> but takes no action if specified position is the 
    * current position of the specified child node.
    */
   final boolean setChildPosition(FGraphicNode child, int pos)
   {
      int index = getIndexOf(child);
      if(index < 0 || pos < 0 || pos >= getChildCount()) return(false);
      if(index == pos) return(true);
      
      subordinates.remove(index + nComponents);
      subordinates.add(pos + nComponents, child);

      FGraphicModel model = getGraphicModel();
      if(model == null) return(true);

      if(child.isRendered()) 
      {
         List<Rectangle2D> dirtyAreas = new ArrayList<>();
         dirtyAreas.add( child.cachedGlobalShape.getBounds2D() );
         model.onChange(this, 1, true, dirtyAreas);
      }
      else
         model.onChange(this, 1, false, null);
      return(true);
   }

   /**
    * Get the parent container for this <code>FGraphicNode</code>.
    * 
    * @return The parent node, or <code>null</code> if this node currently has no parent or is the root node.
    */
   public final FGraphicNode getParent() { return(parent); }

   /**
    * Get the number of component nodes that are subordinate to this <code>FGraphicNode</code>.
    * 
    * <p>A <em>component node</em> is a subordinate node intrinsic to the definition of its parent, as opposed to the 
    * public child nodes that may be inserted/removed through various package-access methods of this class.</p>
    * 
    * @return The number of component graphic nodes subordinate to this node.
    */
   protected int getComponentNodeCount() { return(nComponents); }

   /**
    * Get the graphic node at the specified position within the list of component nodes subordinate to this 
    * <code>FGraphicNode</code>.
    * 
    * <p>A <em>component node</em> is a subordinate node intrinsic to the definition of its parent, as opposed to the 
    * public child nodes that may be inserted/removed through various package-access methods of this class.</p>
    *
    * @param index Position within this node's list of component nodes.
    * @return The component node requested.
    * @throws IndexOutOfBoundsException if <code>index</em> does not lie in the range [0..N-1], where N is the value 
    * returned by <code>getComponentNodeCount()</code>.
    */
   protected FGraphicNode getComponentNodeAt(int index) throws IndexOutOfBoundsException
   {
      if(index < 0 || index >= nComponents) throw new IndexOutOfBoundsException();
      return(subordinates.get(index));
   }

   /**
    * Append a graphics node to the list of component nodes subordinate to this <code>FGraphicNode</code>. This is a 
    * shortcut for <code>insertComponentNode(n, -1)</code>.
    * 
    * @param n The graphics node to be added to this node's list of intrinsic component nodes.
    * @return <code>False</code> if supplied node is <code>null</code>, already has a parent, or is not a component 
    * node.
    * @see FGraphicNode#insertComponentNode(FGraphicNode, int)
    */
   protected final boolean addComponentNode(FGraphicNode n) { return(insertComponentNode(n, -1)); }

   /**
    * Insert a graphics node into the list of component nodes subordinate to this <code>FGraphicNode</code>.
    * 
    * <p>The <code>FGraphicNode</code> infrastructure for building a hierarchical tree of graphic nodes distinguishes 
    * two types of subordinate nodes: public child nodes vs private component nodes. Child nodes are not intrinsic 
    * to the parent node's definition and may be inserted or removed at will by the user to construct a <em>DataNav</em> 
    * graphic. Component nodes are intrinsic to a parent node's definition. They make sense only in the context of a 
    * specific type of parent node, they serve to break down the task of defining and rendering a complex graphic 
    * object, and they allow for unique styling of different parts of that object. Both types of subordinate nodes 
    * must still be hooked into the tree structure to take advantage of <code>FGraphicNode</code>'s support for 
    * rendering, hit-testing, display focus, and style inheritance. But component nodes cannot be inserted/removed 
    * under UI control, and are not accessible in a tree view of the model -- unless they are "UI-displayable".</p>
    * 
    * <p>The method <code>onSubordinateNodeInserted(FGraphicNode)</code> is invoked after the insertion to ensure that 
    * the rendering infrastructure is updated as a result of the insertion.</p>
    * 
    * @param n The graphics node to be added to this node's list of intrinsic component nodes.
    * @param pos The insertion position within the list of component nodes. If this position invalid, the node is 
    * appended to the component node list.
    * @return <code>False</code> if supplied node is <code>null</code>, already has a parent, or is not a component 
    * node.
    */
   protected final boolean insertComponentNode(FGraphicNode n, int pos)
   {
      if((n == null) || (n.parent != null) || !n.isComponentNode()) return(false);
      int index = (pos < 0 || pos >= nComponents) ? nComponents : pos;
      n.parent = this;
      subordinates.add(index, n);
      ++nComponents;
      onSubordinateNodeInserted(n);
      return(true);
   }

   /**
    * Remove a graphics node from the list of component nodes subordinate to this <code>FGraphicNode</code>. If 
    * successful, the method <code>onSubordinateNodeRemoved()</code> is invoked to update the model's rendering 
    * infrastructure in response to the node's removal.
    * 
    * @param n The graphics node to be added to this node's list of intrinsic component nodes.
    * @return <code>False</code> if supplied node is <code>null</code> or is not a component of this node.
    * @see FGraphicNode#addComponentNode(FGraphicNode)
    */
   protected final boolean removeComponentNode(FGraphicNode n)
   {
      int i = subordinates.indexOf(n);
      if(i < 0 || i >= nComponents) return(false);
      n.parent = null;
      subordinates.remove(i);
      --nComponents;
      onSubordinateNodeRemoved(n);
      n.releaseRenderResources();
      return(true);
   }

   /**
    * Remove all graphic nodes descending from this <code>FGraphicNode</code>.
    * 
    * <p>This method is really only intended for use when discarding an entire figure; its purpose is to break up cyclic
    * references to speed up garbage collection of the memory released when a graphic model is discarded. It will only 
    * work when the specified node is not contained in a <code>FGraphicModel</code>.</p>
    */
   protected final void removeAllDescendants()
   {
      if(getGraphicModel() != null) return;
      Stack<FGraphicNode> nodeStack = new Stack<>();
      nodeStack.push(this);
      while(nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();
         for(FGraphicNode sub : n.subordinates)
            nodeStack.push(sub);
         n.subordinates.clear();
      }
   }
   
   
   //
   // Support for style sets
   //
   
   /**
    * Does this graphic node support copying and pasting the set of styles that generally govern its appearance?
    * <p><code>FGraphicNode</code> provides the infrastructure for supporting style sets, but its implementation of this
    * method returns false. To enable the feature, subclasses must override this and other methods. For details, see
    * {@link #getCurrentStyleSet()}.
    * 
    * @return True if node supports copying and pasting of the node's appearance-related style properties -- as well as
    * those of any component nodes -- as a unit.
    */
   public boolean supportsStyleSet() { return(false); }
   
   /**
    * Prepare a copy of this node's style set. A graphic node's "style set" is the collection of <b>all</b> node 
    * property values that have a bearing on how the node is rendered -- not just the generic inheritable text and draw 
    * styles. Any property that would be reasonably applied to similar nodes to achieve a consistent "look" should be
    * included in a node's style set. Any property that represents or is related to the specific content, location, or
    * dimensions of the node should NOT be included in the style set. If the node has any component nodes, each
    * component node's style set may be included in the parent's style set, if that component affects how the parent
    * node is rendered.
    * 
    * <p><i>This method is final, but it relies on several overridable methods to do its work:</i>
    * <ul>
    * <li>If {@link #supportsStyleSet()} returns false, the method takes no action and returns null.</li>
    * <li>Construct the {@link FGNStyleSet} object and add any generic draw/text styles the node possesses.</li>
    * <li>Call {@link #putNodeSpecificStyles(FGNStyleSet)} to add all node-specific styles to the set.</li>
    * <li>If the node has any component nodes, add each component's style set to the parent node's style set.</li>
    * </ul>
    * To enable the style set feature, a subclass must provide appropriate implementations of the methods described
    * above, as well as {@link #applyNodeSpecificStyles}.
    * </p>
    * 
    * @return The node's current style set, as described. If node does not support copying and pasting style sets, then
    * method returns null always. See {@link #supportsStyleSet()}.
    */
   public final FGNStyleSet getCurrentStyleSet() 
   { 
      if(!supportsStyleSet()) return(null); 
      
      FGNStyleSet styleSet = new FGNStyleSet(getNodeType());
      putTextDrawStyles(styleSet);
      putNodeSpecificStyles(styleSet);
      
      for(int i=0; i<getComponentNodeCount(); i++)
      {
         FGNStyleSet cmptStyle = getComponentNodeAt(i).getCurrentStyleSet();
         if(cmptStyle != null) styleSet.addComponentStyleSet(cmptStyle);
      }
      
      return(styleSet);
   }
   
   /**
    * Add any node-specific style properties to a style set representing the node. Only properties that have a bearing
    * on how the node is rendered should be included. Properties that specify the location of the node or its unique
    * content should NOT be added to the style set. This method is called from {@link #getCurrentStyleSet()} during 
    * construction of the node's current style set, but only if the node subclass supports style sets. <i>The base class 
    * implementation does nothing.</i>
    * 
    * @param styleSet The node's current style set, under construction.
    */
   protected void putNodeSpecificStyles(FGNStyleSet styleSet) {}
   
   /**
    * Add any generic draw and text styles possessed by this graphic node to the supplied style set. Subclasses can use
    * this helper method in their implementation of {@link #getCurrentStyleSet()}.
    * 
    * @param styleSet The style set being populated. If its node type does not match this node, no action is taken.
    */
   private void putTextDrawStyles(FGNStyleSet styleSet)
   {
      if(styleSet == null || styleSet.getSourceNodeType() != getNodeType()) return;
      if(hasFontProperties())
      {
         styleSet.putStyle(FGNProperty.FONTFAMILY, getFontFamily());
         styleSet.putStyle(FGNProperty.ALTFONT, getAltFont());
         styleSet.putStyle(FGNProperty.PSFONT, getPSFont());
         styleSet.putStyle(FGNProperty.FONTSTYLE, getFontStyle());
         styleSet.putStyle(FGNProperty.FONTSIZE, getFontSizeInPoints());
      }
      if(hasFillColorProperty()) 
         styleSet.putStyle(FGNProperty.FILLC, getFillColor());
      if(hasStrokeProperties())
      {
         styleSet.putStyle(FGNProperty.STROKEW, getMeasuredStrokeWidth());
         styleSet.putStyle(FGNProperty.STROKEC, getStrokeColor());
         styleSet.putStyle(FGNProperty.STROKECAP, getStrokeEndcap());
         styleSet.putStyle(FGNProperty.STROKEJOIN, getStrokeJoin());
      }
      if(hasStrokePatternProperty())
         styleSet.putStyle(FGNProperty.STROKEPATN, getStrokePattern());
   }

   /**
    * Apply the style set to this graphic node, updating each node property for which there is a corresponding property
    * in the style set. This is a mechanism by which wholesale changes in the node's appearance can be made all at once.
    * Since the operation will typically involve multiple property changes, it is imperative that the model's undo
    * history be blocked while the changes are made. The operation must be reversible as a single "undo" operation; the
    * helper class {@link StyleSetRevEdit} is provided for this purpose.
    * 
    * <p>Let A be the applied style set. It may or may not have been extracted from a graphic node of the same type as
    * this one. All, none, or only some subset of the style properties contained in A may be applicable to this node.
    * The method performs the following steps.
    * <ol>
    * <li>Block the containing graphic model's undo history.</li>
    * <li>Get the node's current style set via {@link #getCurrentStyleSet()}. This will contain the CURRENT values of
    * each node property that is considered a style property. Call this set R.</li>
    * <li>For each generic text/draw style property in R, retrieve its value from A. If A has the property and its value
    * is different from that in R, then update the node accordingly. Otherwise, remove the property from R, since its
    * value won't need to be reverted in the event this styling operation is "undone".</li>
    * <li>Call {@link #applyNodeSpecificStyles(FGNStyleSet, FGNStyleSet)} to let the subclass handle any node-specific 
    * style properties.</li>
    * <li>If any property was changed, call {@link #onNodeModified}.</li>
    * <li>Unblock the model's undo history.</li>
    * <li>If any property was unchanged, R should now contain only those style properties that actually changed. Use it
    * to prepare an instance of {@link StyleSetRevEdit}, and post that to the model's undo history.</li>
    * </ol>
    * 
    * <p>This procedure works when there are no dependencies among the style properties. If a subclass must override
    * this implementation, be sure to follow the same basic outline. Of course, if the graphic node class does not 
    * support style sets ({@link #supportsStyleSet()}, then this method takes no action.</p>
    * 
    * @param applied The style set to be applied to this node.
    * @return True if at least one node property was changed upon applying the style set to the node; else false.
    */
   public final boolean applyStyleSet(FGNStyleSet applied) 
   { 
      if(!supportsStyleSet()) return(false);
      
      // get the node's current style set. It will contain the current values of all node attributes that are considered
      // style properties.
      FGNStyleSet currSS = getCurrentStyleSet();
      if(currSS == null) return(false);

      // this operation may involve lots of property changes, so we block the model's undo history during it.
      FGraphicModel model = getGraphicModel();
      if(model != null) model.blockEditHistory();

      boolean changed = false;
      try
      {
         // handle the generic text/draw style properties
         changed = applyTextDrawStyles(applied, currSS);
         
         // let subclass handle any node-specific styles
         if(applyNodeSpecificStyles(applied, currSS)) changed = true;

         // handle any style changes to component nodes, if any.
         if(FGraphicNode.applyComponentStyling(this, applied, currSS)) changed = true;
         
         // now update the rendering infrastructure, but only if some changes were made
         if(changed) onNodeModified(null);
      }
      finally
      {
         // unblock model's undo history and post the reversible edit that will undo the styling changes
         if(model != null)
         {
            model.unblockEditHistory();
            if(changed) model.postReversibleEdit(new StyleSetRevEdit(this, applied, currSS));
         }
      }

      return(changed); 
   }
   
   /**
    * Apply a style set to this node as one step of a reverisble multiple-node styling operation. The containing figure
    * model relies on this method to apply a particular styling to all currently selected nodes in the figure. The
    * implementation is similar to {@link #applyStyleSet(FGNStyleSet)}, except that this method does not trigger an
    * update of the containing model, does not block the model's edit history, and does not post a reversible edit to
    * undo the styling changes to this node. It is assumed that all of those tasks are handled by the caller.
    * 
    * @param undoer The reversible edit object in which all styling changes are being recorded. It also encapsulates the
    * style set being applied. If null, no action is taken.
    * @return True if at least one node property was changed upon applying the style set to the node; else false.
    */
   final boolean applyStyleSet(StyleSetRevEdit undoer)
   {
      if(undoer == null || !supportsStyleSet()) return(false);
      
      FGNStyleSet currSS = getCurrentStyleSet();
      if(currSS == null) return(false);
      FGNStyleSet applied = undoer.styleSet;
      boolean changed = applyTextDrawStyles(applied, currSS);
      if(applyNodeSpecificStyles(applied, currSS)) changed = true;
      if(FGraphicNode.applyComponentStyling(this, applied, currSS)) changed = true;
      
      // if this node was altered by the style set application, then record it in the reversible edit object
      if(changed) undoer.addAffectedNode(this, currSS);
      
      return(changed);
   }
   
   /**
    * Helper method for {@link #applyStyleSet(FGNStyleSet)}. It handles any style changes to the specified graphic 
    * node's required component nodes, if any. May be called recursively. Note that we only apply a component style set 
    * at index I to the component node at the same index, and only if their node types match.
    * 
    * @param n The <i>FypML</i> graphic node to which a style set is being applied. 
    * @param applied The style set being applied. It may or may not include relevant component style sets
    * @param restore The node's style set PRIOR to applying any style changes to its components. It will include a
    * component style set for each of the node's components (if it has any). Upon return, those component style sets
    * are revised to contain only the original values of those style properties <i>that were actually changed</i>. This
    * is important because this style set is used to "undo" the styling operation in its entirety, to include the
    * effects on any component nodes.
    * @return True if at least one node property was changed; else false.
    */
   private static boolean applyComponentStyling(FGraphicNode n, FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      if(!n.supportsStyleSet()) return(false);
      
      int nCmpts = Math.min(applied.getNumberComponentStyleSets(), restore.getNumberComponentStyleSets());
      nCmpts = Math.min(n.getComponentNodeCount(), nCmpts);
      for(int i=0; i<nCmpts; i++)
      {
         FGraphicNode cmpt = n.getComponentNodeAt(i);
         FGNodeType nt = cmpt.getNodeType();
         FGNStyleSet cmptApplied = applied.getComponentStyleSet(i);
         FGNStyleSet cmptRestore = restore.getComponentStyleSet(i);
         if(nt == cmptApplied.getSourceNodeType() && nt == cmptRestore.getSourceNodeType())
         {
            if(cmpt.applyTextDrawStyles(cmptApplied, cmptRestore)) changed = true;
            if(cmpt.applyNodeSpecificStyles(cmptApplied, cmptRestore)) changed = true;
            
            // recursion here:
            if(FGraphicNode.applyComponentStyling(cmpt, cmptApplied, cmptRestore)) changed = true;
         }
      }
      
      return(changed);
   }
   
   /**
    * Helper method for {@link #applyStyleSet(FGNStyleSet)}. It applies to the node any generic text/draw styles in the 
    * applied style set that are different from their commensurate values in the restore style set. If the applied set 
    * includes a text/draw style in the restore style set but its value is the same, then remove that property from the 
    * restore set since it will be unaffected by this styling operation. The restore style set is ultimately used to 
    * "undo" the styling operation. <i>The method does NOT notify the model that the node has changed.</i>
    *
    * @param applied The style set being applied to this graphic node.
    * @param restore The node's style set PRIOR to the pasting any relevant text/draw styles from the applied set. Upon 
    * return, it should contain only the original values of those style properties <i>that were actually changed</i>.
    * @return True if at least one generic text/draw style property was changed; else false.
    */
   protected final boolean applyTextDrawStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      boolean resolveFont = false;

      if(restore.hasStyle(FGNProperty.FONTFAMILY))
      {
         String fam = (String) applied.getCheckedStyle(FGNProperty.FONTFAMILY, null, String.class);
         if(fam != null && !fam.equals(restore.getStyle(FGNProperty.FONTFAMILY)))
         {
            fontFamily = fam;
            changed = true;
            resolveFont = true;
         }
         else
            restore.removeStyle(FGNProperty.FONTFAMILY);
      }
      if(restore.hasStyle(FGNProperty.FONTSTYLE))
      {
         FontStyle fs = (FontStyle) applied.getCheckedStyle(FGNProperty.FONTSTYLE, null, FontStyle.class);
         if(fs != null && !fs.equals(restore.getStyle(FGNProperty.FONTSTYLE)))
         {   
            fontStyle = fs;
            changed = true;
            resolveFont = true;
         }
         else
            restore.removeStyle(FGNProperty.FONTSTYLE);
      }
      if(restore.hasStyle(FGNProperty.FONTSIZE))
      {
         Integer sz = (Integer) applied.getCheckedStyle(FGNProperty.FONTSIZE, null, Integer.class);
         if(sz != null && !sz.equals(restore.getStyle(FGNProperty.FONTSIZE)))
         {   
            fontSizeInPoints = sz;
            changed = true;
            resolveFont = true;
         }
         else
            restore.removeStyle(FGNProperty.FONTSIZE);
      }
      if(restore.hasStyle(FGNProperty.ALTFONT))
      {
         GenericFont alt = (GenericFont) applied.getCheckedStyle(FGNProperty.ALTFONT, null, GenericFont.class);
         if(alt != null && !alt.equals(restore.getStyle(FGNProperty.ALTFONT)))
         {   
            altFont = alt;
            changed = true;
            resolveFont = true;
         }
         else
            restore.removeStyle(FGNProperty.ALTFONT);
      }
      if(restore.hasStyle(FGNProperty.PSFONT))
      {
         PSFont ps = (PSFont) applied.getCheckedStyle(FGNProperty.PSFONT, null, PSFont.class);
         if(ps != null && !ps.equals(restore.getStyle(FGNProperty.PSFONT)))
         {   
            psFont = ps;
            changed = true;
         }
         else
            restore.removeStyle(FGNProperty.PSFONT);
      }
      if(resolveFont) cacheResolvedFont();
      
      if(restore.hasStyle(FGNProperty.FILLC))
      {
         Color c = (Color) applied.getCheckedStyle(FGNProperty.FILLC, null, Color.class);
         if(c != null && !c.equals(restore.getStyle(FGNProperty.FILLC)))
         {   
            fillC = c;
            changed = true;
         }
         else
            restore.removeStyle(FGNProperty.FILLC);
      }
      if(restore.hasStyle(FGNProperty.STROKEC))
      {
         Color c = (Color) applied.getCheckedStyle(FGNProperty.STROKEC, null, Color.class);
         if(c != null && !c.equals(restore.getStyle(FGNProperty.STROKEC)))
         {   
            strokeC = c;
            changed = true;
         }
         else
            restore.removeStyle(FGNProperty.STROKEC);
      }
      if(restore.hasStyle(FGNProperty.STROKEW))
      {
         Measure m = (Measure) applied.getCheckedStyle(FGNProperty.STROKEW, null, Measure.class);
         if(m != null) m = STROKEWCONSTRAINTS.constrain(m);
         if(m != null && !m.equals(restore.getStyle(FGNProperty.STROKEW)))
         {   
            strokeMeasure = m;
            changed = true;
         }
         else
            restore.removeStyle(FGNProperty.STROKEW);
      }
      if(restore.hasStyle(FGNProperty.STROKECAP))
      {
         StrokeCap c = (StrokeCap) applied.getCheckedStyle(FGNProperty.STROKECAP, null, StrokeCap.class);
         if(c != null && !c.equals(restore.getStyle(FGNProperty.STROKECAP)))
         {   
            strokeCap = c;
            changed = true;
         }
         else
            restore.removeStyle(FGNProperty.STROKECAP);
      }
      if(restore.hasStyle(FGNProperty.STROKEJOIN))
      {
         StrokeJoin join = (StrokeJoin) applied.getCheckedStyle(FGNProperty.STROKEJOIN, null, StrokeJoin.class);
         if(join != null && !join.equals(restore.getStyle(FGNProperty.STROKEJOIN)))
         {   
            strokeJoin = join;
            changed = true;
         }
         else
            restore.removeStyle(FGNProperty.STROKEJOIN);
      }
      if(restore.hasStyle(FGNProperty.STROKEPATN))
      {
         StrokePattern p = (StrokePattern) applied.getCheckedStyle(FGNProperty.STROKEPATN, null, StrokePattern.class);
         if(p != null && !p.equals(restore.getStyle(FGNProperty.STROKEPATN)))
         {   
            strokePattern = p;
            changed = true;
         }
         else
            restore.removeStyle(FGNProperty.STROKEPATN);
      }

      return(changed);
   }

   /**
    * Subclasses using the default implementation of {@link #applyStyleSet(FGNStyleSet)} must handle any node-specific
    * style properties -- i.e., anything other than the generic text and draw styles -- in this method. It is invoked
    * after the relevant text and draw styles have been applied. The base class implementation does nothing.
    * 
    * <p>Usage: For each node-specific style property, check for its presence in the applied style set. If it is there
    * and its value is different from the current value (which will be encapsulated in the restored style set), then 
    * apply the new value. If it is not there or if its value is unchanged, then removed the corresponding property from
    * the restored style set -- since that value won't be changed. The restored style set will be applied to the node
    * to "undo" the entire operation atomically.</p>
    * <p>Since multiple property values may be changed, do NOT notify the model after each individual change via
    * {@link #onNodeModified}. The default implementation of <code>applyStyleSet()</code> will notify the model after
    * all changes are complete.</p>
    * 
    * @param applied The style set being applied to this graphic node.
    * @param restore The node's style set PRIOR to the application of the new styling properties. Upon return, it should
    * contain only the original values of those style properties that were actually changed.
    * @return True if at least one node-specific style property was changed; else false.
    */
   protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore) { return(false); }
      

   //
   // Support for the generic inheritable text/draw styles
   //

   /**
    * Is the specified property one of the inheritable graphic styling properties?
    * @param p The property type.
    * @return <code>True</code> iff the argument represents a graphic style property.
    */
   public static boolean isStyleProperty(FGNProperty p)
   {
      return(p == FGNProperty.FONTFAMILY || p == FGNProperty.FONTSTYLE || p == FGNProperty.FONTSIZE || 
            p == FGNProperty.ALTFONT || p == FGNProperty.PSFONT || p == FGNProperty.FILLC || p == FGNProperty.STROKEC || 
            p == FGNProperty.STROKEW || p == FGNProperty.STROKECAP || p == FGNProperty.STROKEJOIN || 
            p == FGNProperty.STROKEPATN);
   }

   private final static FGNProperty[] styleIDs = new FGNProperty[] {
      FGNProperty.FONTFAMILY, FGNProperty.FONTSTYLE, FGNProperty.FONTSIZE, FGNProperty.ALTFONT, 
      FGNProperty.PSFONT, FGNProperty.FILLC, FGNProperty.STROKEC, FGNProperty.STROKEW, FGNProperty.STROKECAP,
      FGNProperty.STROKEJOIN, FGNProperty.STROKEPATN
   };
   
   /**
    * Get an array of <code>FGNProperty</code> enumerants representing all of the graphic styling properties that may 
    * be defined on a graphic node in <em>DataNav</em>.
    * @return Array of style property IDs.
    */
   public static FGNProperty[] getStylePropertyIDs() { return(styleIDs); }

   /**
    * Bit mask indicating that a graphic node possesses the font-related attributes (family, style, size, alternate 
    * font, Postscript font).
    */
   static final int HASFONTATTRS = (1);

   /**
    * Bit mask indicating that a graphic node possesses the text/fill color attribute.
    */
   static final int HASFILLCATTR = (1<<1);

   /**
    * Bit mask indicating that a graphic node possesses the stroke color and line width attributes. As of app version
    * 3.2.0 and document schema version 10, this also includes line endcap and join styles.
    */
   static final int HASSTROKEATTRS = (1<<2);

   /**
    * Bit mask indicating that a graphic node possesses the stroke pattern (dash-gap array) attribute.
    */
   static final int HASSTRKPATNATTR = (1<<3);

   /**
    * Bit mask indicating that a graphic node possesses the location attributes <em>x</em> and <em>y</em>.
    */
   static final int HASLOCATTRS = (1<<4);

   /**
    * Bit mask indicating that a graphic node possesses the size attributes <em>width</em> and <em>height</em>.
    */
   static final int HASSIZEATTRS = (1<<5);

   /**
    * Bit mask indicating that a graphic node possesses the <em>rotate</em> attribute, which generally specifies the 
    * node's orientation relative to the parent viewport.
    */
   static final int HASROTATEATTR = (1<<6);

   /** Bit mask indicating that a graphic node possesses the <em>title/em> attribute. */
   static final int HASTITLEATTR = (1<<7);

   /** Bit mask indicating that a graphic node possesses the optional <i>id</i> attribute. */
   static final int HASIDATTR = (1<<8);
   
   /** 
    * If this bit is set in attribute bit mask and the node has the <i>title</i> attribute, then values for that
    * attribute may contain one or more line feed characters ('\n').
    */
   static final int ALLOWLFINTITLE = (1<<12);
   
   /**
    * If this bit is set in attribute mask and the node has the <i>title</i> attribute, then the title string can
    * be formatted as "styled text". See {@link #toStyledText} for a full description.
    */
   static final int ALLOWATTRINTITLE = (1<<13);
   
   /**
    * Bit mask indicating that a graphic node possesses all of the shared attributes implemented by this base class,
    * with the exception of {@link #ALLOWLFINTITLE} and {@link #ALLOWATTRINTITLE}.
    */
   static final int HASATTRMASK = 
      HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR|HASLOCATTRS|HASSIZEATTRS|HASROTATEATTR|
      HASTITLEATTR|HASIDATTR;

   /** Bit mask admitting all defined graphic node attributes implemented in this base class. */
   static final int HASATTRMASKALL = HASATTRMASK|ALLOWLFINTITLE|ALLOWATTRINTITLE;
   
   /**
    * Bit mask indicating which of the shared attributes managed by <code>FGraphicNode</code> are enabled for this 
    * particular node instance. Also includes special flag {@link #ALLOWLFINTITLE}
    */
   private int attrFlags;

   /**
    * Does this <code>FGraphicNode</code> possess the font-related style properties?
    * @return <code>True</code> iff node has font-related properties: font family, alternate and PS fonts, font size, 
    * and font style.
    */
   public boolean hasFontProperties() { return((attrFlags & HASFONTATTRS) == HASFONTATTRS); }

   /**
    * Does this <code>FGraphicNode</code> possess the text/fill color style?
    * @return <code>True</code> iff node has the text/fill color style.
    */
   public boolean hasFillColorProperty() { return((attrFlags & HASFILLCATTR) == HASFILLCATTR); }

   /**
    * Does this <code>FGraphicNode</code> possess the basic stroke properties? 
    * @return <code>True</code> iff node has the basic stroking properties: width, color, endcap style and join style.
    * @see FGraphicNode#hasStrokePatternProperty()
    */
   public boolean hasStrokeProperties() { return((attrFlags & HASSTROKEATTRS) == HASSTROKEATTRS); }

   /**
    * Does this <code>FGraphicNode</code> possess the stroke pattern (dash-gap array) style?
    * @return <code>True</code> iff node has the stroke pattern style.
    */
   public boolean hasStrokePatternProperty() { return((attrFlags & HASSTRKPATNATTR) == HASSTRKPATNATTR); }

   /**
    * Has the value of the specified style property for this <code>FGraphicNode</code> been explicitly set?
    * 
    * @param style The style property to test. Must be one of the inheritable styles.
    * @return <code>True</code> iff the specified property is an inheritable style property, the node possesses that 
    * property, and its value has been explicitly set.
    */
   public boolean isStylePropertyExplicit(FGNProperty style)
   {
      boolean explicit = false;
      switch(style)
      {
         case FONTFAMILY: explicit = (fontFamily != null); break;
         case FONTSTYLE: explicit = (fontStyle != null); break;
         case FONTSIZE: explicit = (fontSizeInPoints != null); break;
         case ALTFONT: explicit = (altFont != null); break;
         case PSFONT: explicit = (psFont != null); break;
         case FILLC: explicit = (fillC != null); break;
         case STROKEC: explicit = (strokeC != null); break;
         case STROKEW: explicit = (strokeMeasure != null); break;
         case STROKECAP: explicit = (strokeCap != null); break;
         case STROKEJOIN: explicit = (strokeJoin != null); break;
         case STROKEPATN: explicit = (strokePattern != null); break;
         default : break;
      }
      return(explicit);
   }

   /**
    * The family name of font with which this graphics node renders any text. A <code>null</code> value indicates that 
    * the node should inherit the value from its parent.
    */
   private String fontFamily = null;

   /**
    * Get the font family with which this <code>FGraphicNode</code> renders any text. If this node does not possess the 
    * font-related style attributes, or if its font family is currently implicit, then the method returns the font 
    * family of the nearest ancestor for which the attribute is explicitly set. In the event that no such inherited 
    * value is found, an application default is returned.
    * 
    * <p><em>Note</em> that the font family name returned does not necessarily correspond to an actual font installed 
    * on the host. It may be that the original document was created on a different host, whose set of installed fonts 
    * could easily differ from that of the current host. In addition, <em>DataNav</em> currently lets the user set the 
    * font family name to any non-empty string, so the user could set it to something totally wacko!</p>
    * 
    * @return The font family name, as described.
    */
   public String getFontFamily()
   {
      if((attrFlags & HASFONTATTRS) != HASFONTATTRS || fontFamily == null)
      {
         String family = (String) getInheritedValueFor(FGNProperty.FONTFAMILY);
         return((family == null) ? LocalFontEnvironment.getSansSerifFont() : family);
      }
      else
         return(fontFamily);
   }

   /**
    * Set the font family with which this <code>FGraphicNode</code> renders any text. If the node does not possess the 
    * font-related style attributes, this method has no effect. Otherwise, if a change is made, the font family is 
    * updated, the node's actual font is updated accordingly, and <code>onNodeModified()</code> is invoked.
    * 
    * @param family The name of the desired font family. A <code>null</code> value makes the attribute implicit, meaning 
    * the node will inherit the style from its parent.
    * @return <code>True</code> if change was accepted; <code>false</code> if node does not possess the font-related 
    * style attributes, or if the specified family name is an empty string.
    */
   public boolean setFontFamily(String family)
   {
      if((attrFlags & HASFONTATTRS) != HASFONTATTRS || (family != null && family.isEmpty())) return(false);

      // if it's being explicitly set to the inherited value, then make it implicit!
      if(family != null && family.equals(getInheritedValueFor(FGNProperty.FONTFAMILY)))
         family = null;

      boolean changed = !Objects.equals(fontFamily, family);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.FONTFAMILY, 
               family != null ? family : getInheritedValueFor(FGNProperty.FONTFAMILY))) 
            return(true);

         // update font family, resolve the new font, trigger a re-rendering, and post an undoable change if we can
         String oldFamily = fontFamily;
         fontFamily = family;
         if(notify)
         {
            propagateFontChange();
            onNodeModified(FGNProperty.FONTFAMILY);
            FGNRevEdit.post(this, FGNProperty.FONTFAMILY, fontFamily, oldFamily);
         }
      }
      return(true);
   }

   /**
    * The style of font with which this graphics node renders any text. A <code>null</code> value indicates that the 
    * node should inherit the value from its parent.
    */
   private FontStyle fontStyle = null;

   /**
    * Get the font style variant with which this <code>FGraphicNode</code> renders any text. If this node does not 
    * possess the font-related style attributes, or if its font style is currently implicit, then the method returns the 
    * font style of the nearest ancestor for which the attribute is explicitly set. In the event that no such inherited 
    * value is found, an application default is returned.
    * 
    * @return The font style variant.
    */
   public FontStyle getFontStyle()
   {
      if((attrFlags & HASFONTATTRS) != HASFONTATTRS || fontStyle == null)
      {
         FontStyle style = (FontStyle) getInheritedValueFor(FGNProperty.FONTSTYLE);
         return((style == null) ? FontStyle.PLAIN : style);
      }
      else
         return(fontStyle);
   }

   /**
    * Set the font style variant with which this <code>FGraphicNode</code> renders any text. If the node does not 
    * possess the font-related style attributes, this method has no effect. Otherwise, if a change is made, the font 
    * style is updated, the node's actual font is updated accordingly, and <code>onNodeModified()</code> is invoked.
    * 
    * @param style The new font style variant. A <code>null</code> value makes the attribute implicit, meaning the node
    * will inherit the style from its parent.
    * @return <code>True</code> if change was accepted; <code>false</code> if node does not possess the font-related 
    * style attributes.
    */
   public boolean setFontStyle(FontStyle style)
   {
      if((attrFlags & HASFONTATTRS) != HASFONTATTRS) return(false);

      // if it's being explicitly set to the inherited value, then make it implicit!
      if(style != null && (style == getInheritedValueFor(FGNProperty.FONTSTYLE)))
         style = null;

      boolean changed = (fontStyle != style);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.FONTSTYLE, 
               style != null ? style : getInheritedValueFor(FGNProperty.FONTSTYLE))) 
            return(true);

         // update font style, resolve the new font, and trigger a re-rendering
         FontStyle oldStyle = fontStyle;
         fontStyle = style;
         if(notify)
         {
            propagateFontChange();
            onNodeModified(FGNProperty.FONTSTYLE);
            FGNRevEdit.post(this, FGNProperty.FONTSTYLE, fontStyle, oldStyle);
         }
      }
      return(true);
   }

   /**
    * The size of font with which this graphics node renders any text, measured in typographical points (1pt = 1/72 in). 
    * A <code>null</code> value indicates that the node should inherit the value from its parent.
    */
   private Integer fontSizeInPoints = null;

   /**
    * The smallest acceptable font size, in typographical points.
    */
   public final static int MINFONTSIZE = 1;

   /**
    * The largest acceptable font size, in typographical points.
    */
   public final static int MAXFONTSIZE = 99;

   /**
    * Get the attribute specifying the font size with which this <code>FGraphicNode</code> renders any text. If this 
    * node does not possess the font-related style attributes, or if its font size attribute is currently implicit, then 
    * the method returns the font size of the nearest ancestor for which the attribute is explicitly set. In the event 
    * that no such inherited value is found, an application default is returned.
    * 
    * @return The font size attribute, as described.
    */
   public Integer getFontSizeInPoints()
   {
      if((attrFlags & HASFONTATTRS) != HASFONTATTRS || fontSizeInPoints == null)
      {
         Integer size = (Integer) getInheritedValueFor(FGNProperty.FONTSIZE);
         return((size == null) ? Integer.valueOf(10) : size);
      }
      else
         return(fontSizeInPoints);
   }

   /**
    * Set the attribute specifying the font size with which this <code>FGraphicNode</code> renders any text. If the 
    * node does not possess the font-related style attributes, this method has no effect. Font sizes outside the range 
    * <code>[MINFONTSIZE..MAXFONTSIZE]</code> are rejected. Otherwise, if a change is made, the font size is updated, 
    * the node's actual font is updated accordingly, and <code>onNodeModified()</code> is invoked.
    * 
    * @param size The desired font size. A <code>null</code> value makes the attribute implicit, meaning 
    * the node will inherit the style from its parent. 
    * @return <code>True</code> if change was accepted; <code>false</code> if node does not possess the font-related 
    * style attributes, or if the specified font size is out of bounds.
    */
   public boolean setFontSizeInPoints(Integer size)
   {
      if((attrFlags & HASFONTATTRS) != HASFONTATTRS) return(false);
      if((size != null) && (size < MINFONTSIZE || size > MAXFONTSIZE)) return(false);

      // if it's being explicitly set to the inherited value, then make it implicit!
      if(size != null && size.equals(getInheritedValueFor(FGNProperty.FONTSIZE)))
         size = null;

      boolean changed = !Objects.equals(fontSizeInPoints, size);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.FONTSIZE, 
               size != null ? size : getInheritedValueFor(FGNProperty.FONTSIZE))) 
            return(true);
         
         Integer oldFontSize = fontSizeInPoints;
         fontSizeInPoints = size;
         if(notify)
         {
            propagateFontChange();
            onNodeModified(FGNProperty.FONTSIZE);
            FGNRevEdit.post(this, FGNProperty.FONTSIZE, fontSizeInPoints, oldFontSize);
         }
      }
      return(true);
   }

   /**
    * The logical font with which this graphics node should render any text if the current font family is not installed 
    * on the host. A <code>null</code> value indicates that the node should inherit the value from its parent.
    */
   private GenericFont altFont = null;

   /**
    * Get the alternate font with which this <code>FGraphicNode</code> renders any text when the node's font family is 
    * not installed on the current host. If this node does not possess the font-related style attributes, or if its 
    * alternate font is currently implicit, then the method returns the alternate font of the nearest ancestor for which 
    * the attribute is explicitly set. In the event that no such inherited value is found, an application default is
    * returned.
    * 
    * <p>The alternate font is a Java logical font, and <em>DataNav</em> guarantees that each of the different logical 
    * fonts is mapped to a font installed in the system.</p>
    * 
    * @return The alternate display font.
    */
   public GenericFont getAltFont()
   {
      if((attrFlags & HASFONTATTRS) != HASFONTATTRS || altFont == null)
      {
         GenericFont f = (GenericFont) getInheritedValueFor(FGNProperty.ALTFONT);
         return((f == null) ? GenericFont.SANSERIF : f);
      }
      else
         return(altFont);
   }

   /**
    * Set the alternate font with which this <code>FGraphicNode</code> renders any text in the event that the font 
    * family attribute does not identify an installed font in the host. If the node does not possess the font-related 
    * style attributes, this method has no effect. Otherwise, the alternate font is updated. If it is currently being 
    * used by the node (because the specified font family was not found), the node's actual font is updated accordingly, 
    * and the method <code>onNodeModified()</code> is invoked.
    * 
    * @param f The new alternate font. A <code>null</code> value makes the attribute implicit, meaning the node
    * will inherit the style from its parent.
    * @return <code>True</code> if change was accepted; <code>false</code> if node does not possess the font-related 
    * style attributes.
    */
   public boolean setAltFont(GenericFont f)
   {
      if((attrFlags & HASFONTATTRS) != HASFONTATTRS) return(false);

      // if it's being explicitly set to the inherited value, then make it implicit!
      if(f != null && (f == getInheritedValueFor(FGNProperty.ALTFONT)))
         f = null;

      boolean changed = (altFont != f);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.ALTFONT, f != null ? f : getInheritedValueFor(FGNProperty.ALTFONT))) 
            return(true);
         
         GenericFont oldAltFont = altFont;
         altFont = f;

         if(notify)
         {
            // if the node's font family attribute already identifies an installed font, then changing the alternate font 
            // will have no effect on the node's realized font nor on its rendering! Else, we must update font & rendering.
            if(!LocalFontEnvironment.isFontInstalled(getFontFamily()))
            {
               propagateFontChange();
               onNodeModified(FGNProperty.ALTFONT);
            }
            else
            {
               FGraphicModel model = getGraphicModel();
               if(model != null) model.onChange(this, 0, false, null);
            }
            
            FGNRevEdit.post(this, FGNProperty.ALTFONT, altFont, oldAltFont);
         }
      }
      return(true);
   }

   /**
    * Attribute identifies the standard Postscript font family with which this graphics node should render any text when 
    * it is exported to Postscript format. A <code>null</code> value indicates that the node should inherit the value 
    * from its parent.
    */
   private PSFont psFont = null;

   /**
    * Get the standard Postscript printer font family with which this <code>FGraphicNode</code> renders any text when 
    * it is exported to a Postscript document. If this node does not possess the font-related style attributes, or if 
    * its Postscript font is currently implicit, then the method returns the Postscript font of the nearest ancestor for
    * which the attribute is explicitly set. In the event that no such inherited value is found, an application default 
    * is returned.
    * 
    * @return A Postscript font family.
    */
   public PSFont getPSFont()
   {
      if((attrFlags & HASFONTATTRS) != HASFONTATTRS || psFont == null)
      {
         PSFont psf = (PSFont) getInheritedValueFor(FGNProperty.PSFONT);
         return((psf == null) ? PSFont.HELVETICA : psf);
      }
      else
         return(psFont);
   }

   /**
    * Set the Postscript font family with which this <code>FGraphicNode</code> renders any text when it is exported to 
    * a Postscript document. If the node does not possess the font-related style attributes, this method has no effect. 
    * 
    * @param f The new Postscript font. A <code>null</code> value makes the attribute implicit, meaning the node
    * will inherit the style from its parent.
    * @return <code>True</code> if change was accepted; <code>false</code> if node does not possess the font-related 
    * style attributes.
    */
   public boolean setPSFont(PSFont f)
   {
      if((attrFlags & HASFONTATTRS) != HASFONTATTRS) return(false);

      // if it's being explicitly set to the inherited value, then make it implicit!
      if(f != null && (f == getInheritedValueFor(FGNProperty.PSFONT)))
         f = null;

      if(doMultiNodeEdit(FGNProperty.PSFONT, f != null ? f : getInheritedValueFor(FGNProperty.PSFONT))) return(true);

      // since this does attribute does not affect screen appearance of node, just update it. Still have to tell the 
      // model that it has changed!
      PSFont oldPSFont = psFont;
      psFont = f;
      if(notify)
      {
         FGraphicModel model = getGraphicModel();
         if(oldPSFont != psFont && model != null)
         {
            model.onChange(this, 0, false, null);
            FGNRevEdit.post(this, FGNProperty.PSFONT, psFont, oldPSFont);
         }
      }

      return(true);
   }

   /**
    * The RGB color with which this graphics node should fill any text or shapes it renders. The fill color may be 
    * opaque, translucent, or fully transparent. If null, the node inherits the value from its parent.
    */
   private Color fillC = null;

   /**
    * Get the RGB color with which this graphic node fills any shapes it renders, including text. If this node does not 
    * possess the fill color attribute, or if its fill color is currently implicit, then the method returns the fill 
    * color of the nearest ancestor for which the attribute is explicitly set. The fill color may be opaque (alpha 
    * channel == 1.0), translucent, or fully transparent (alpha == 0).
    * 
    * <p><i>IMPORTANT</i>: This implements the like-named method from the {@link PainterStyle
    * PainterStyle} interface.</p>
    * 
    * @return The fill color.
    */
   public Color getFillColor()
   {
      if((attrFlags & HASFILLCATTR) != HASFILLCATTR || fillC == null)
      {
         Color c = (Color) getInheritedValueFor(FGNProperty.FILLC);
         return((c == null) ? Color.WHITE : c);
      }
      else
         return(fillC);
   }

   /**
    * Set the RGB color with which this graphic node fills any shapes it renders, including text. If a change is made, 
    * {@link #onNodeModified} is invoked. If the node does not possess the fill color attribute, this method has no
    * effect.
    * 
    * @param c The new fill color. A null value makes the attribute implicit, meaning the node will inherit the style 
    * from its parent. 
    * @return True if change was accepted; false if node does not possess the fill color style, or if the fill color
    * was unchanged.
    */
   public boolean setFillColor(Color c)
   {
      if((attrFlags & HASFILLCATTR) != HASFILLCATTR) return(false);

      // if it's being explicitly set to the inherited value, then make it implicit!
      if(c != null && c.equals(getInheritedValueFor(FGNProperty.FILLC)))
         c = null;

      boolean changed = !Objects.equals(fillC, c);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.FILLC, c != null ? c : getInheritedValueFor(FGNProperty.FILLC))) return(true);

         Color oldFillC = fillC;
         fillC = c;
         
         if(notify)
         {
            onNodeModified(FGNProperty.FILLC);
            FGNRevEdit.post(this, FGNProperty.FILLC, fillC, oldFillC);
         }
      }
      return(true);
   }

   /**
    * The RGB color with which this graphics node should stroke any shapes it renders (note: outlining of text is not 
    * supported). The stroke color may be opaque, translucent, or fully transparent. If null, the node inherits the 
    * value from its parent.
    */
   private Color strokeC = null;

   /**
    * Get the RGB color with which this graphic node strokes any shapes it renders (except text glyphs -- outlined text
    * is not supported). If this node does not possess the stroke color attribute, or if its stroke color is currently 
    * implicit, then the method returns the stroke color of the nearest ancestor for which the attribute is explicitly 
    * set. The stroke color may be opaque (alpha channel == 1.0), translucent, or fully transparent (alpha == 0).
    * 
    * <p><i>IMPORTANT</i>: This implements the like-named method from the {@link PainterStyle
    * PainterStyle} interface.</p>
    * 
    * @return The stroke color.
    */
   public Color getStrokeColor()
   {
      if((attrFlags & HASSTROKEATTRS) != HASSTROKEATTRS || strokeC == null)
      {
         Color c = (Color) getInheritedValueFor(FGNProperty.STROKEC);
         return((c == null) ? Color.BLACK : c);
      }
      else
         return(strokeC);
   }


   /**
    * Set the RGB color with which this graphic node stroke any shapes it renders (except text glyphs -- outlined text
    * is not supported). If a change is made, {@link #onNodeModified} is invoked. If the node does not possess the
    * stroke color attribute, this method has no effect.
    * 
    * @param c The new stroke color. A null value makes the attribute implicit, meaning the node will inherit the style 
    * from its parent. 
    * @return True if change was accepted; false if node does not possess the stroke color attribute, or if the stroke 
    * color was unchanged.
    */
   public boolean setStrokeColor(Color c)
   {
      if((attrFlags & HASSTROKEATTRS) != HASSTROKEATTRS) return(false);

      // if it's being explicitly set to the inherited value, then make it implicit!
      if(c != null && c.equals(getInheritedValueFor(FGNProperty.STROKEC)))
         c = null;

      boolean changed = !Objects.equals(strokeC, c);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.STROKEC, c != null ? c : getInheritedValueFor(FGNProperty.STROKEC))) 
            return(true);

         Color oldStrokeC = strokeC;
         strokeC = c;
         
         if(notify)
         {
            onNodeModified(FGNProperty.STROKEC);
            FGNRevEdit.post(this, FGNProperty.STROKEC, strokeC, oldStrokeC);
         }
      }
      return(true);
   }

   /**
    * The width of the line with which this graphics node should stroke any shapes it renders (other than text glyphs, 
    * which are never stroked). Specified as a linear measurement with associated units. A <code>null</code> value 
    * indicates that the node should inherit the value from its parent.
    */
   private Measure strokeMeasure = null;

   /**
    * Constrains the stroke width attribute to use physical units, to have a maximum of 4 significant and 3 fractional
    * digits, and to lie in the range [0..1in], regardless what units are assigned to it.
    */
   public final static Measure.Constraints STROKEWCONSTRAINTS = new Measure.Constraints(0.0, 1000.0, 4, 3);

   /**
    * Get the width of the line with which this <code>FGraphicNode</code> strokes any shapes it renders (except text 
    * glyphs -- <em>DataNav</em> does not support outlined text). If this node does not possess the basic stroke 
    * attributes, or if its stroke width is currently implicit, then the method returns the stroke width of the nearest 
    * ancestor for which the attribute is explicitly set. In the event that no such inherited value is found, an 
    * application default is returned.
    * 
    * @return The stroke width, specified as a linear measurement with associated units.
    */
   public Measure getMeasuredStrokeWidth()
   {
      if((attrFlags & HASSTROKEATTRS) != HASSTROKEATTRS || strokeMeasure == null)
      {
         Measure m = (Measure) getInheritedValueFor(FGNProperty.STROKEW);
         return((m == null) ? new Measure(0.01, Measure.Unit.IN) : m);
      }
      else
         return(strokeMeasure);
   }

   /**
    * Set the width of the line with which this <code>FGraphicNode</code> strokes any shapes it renders (except text 
    * glyphs -- <em>DataNav</em> does not support outlined text). If a change is made, <code>onNodeModified()</code> is
    * invoked. If the node does not possess the basic stroke attributes, this method has no effect.
    * 
    * <p>Stroke width must be specified in non-relative, physical units. It cannot be negative, and the maximum allowed 
    * stroke width is 1 inch. If the specified measure violates any of these constraints, it is rejected.</p>
    * 
    * @param m The new stroke width. A null value makes the attribute implicit, meaning the node will inherit the style
    * from its parent. Otherwise, the measure will be constrained to satisfy {@link #STROKEWCONSTRAINTS}.
    * @return True if change was accepted; false if rejected.
    */
   public boolean setMeasuredStrokeWidth(Measure m)
   {
      if((attrFlags & HASSTROKEATTRS) != HASSTROKEATTRS) return(false);
      if(m != null) m = STROKEWCONSTRAINTS.constrain(m);

      // if it's being explicitly set to the inherited value, then make it implicit!
      if((m != null) && Measure.equal(m, (Measure)getInheritedValueFor(FGNProperty.STROKEW)))
         m = null;

      boolean changed = (strokeMeasure != m) && !Measure.equal(strokeMeasure, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.STROKEW, m != null ? m : getInheritedValueFor(FGNProperty.STROKEW))) 
            return(true);

         Measure oldStrokeW = strokeMeasure;
         strokeMeasure = m;
         
         if(notify)
         {
            onNodeModified(FGNProperty.STROKEW);
            FGNRevEdit.post(this, FGNProperty.STROKEW, strokeMeasure, oldStrokeW);
         }
      }
      return(true);
   }

   /**
    * Enumerated attribute determines what decoration is applied to the endpoints of unclosed paths or dash segments 
    * when stroking any graphics primitives (other than text glyphs) in the rendering of this graphics node. The
    * possible values mirror those defined in Java2D and Postscript. A <code>null</code> value indicates that the node 
    * should inherit the value from its parent.
    */   
   private StrokeCap strokeCap = null;
   
   /**
    * Get the decoration that should be applied to the endpoints of unclosed paths or dash segments when stroking any 
    * graphics primitives (other than text glyphs) in the rendering of this graphics node. If this node does not possess
    * this basic stroke attribute, or if its stroke endcap style is currently implicit, then the method returns the 
    * endcap style of the nearest ancestor for which the attribute is explicitly set. In the event that no such 
    * inherited value is found, an application default (<code>StrokeCap.BUTT</code>) is returned.
    * 
    * @return The stroke endcap decoration.
    */
   public StrokeCap getStrokeEndcap()
   {
      if((attrFlags & HASSTROKEATTRS) != HASSTROKEATTRS || strokeCap == null)
      {
         StrokeCap sc = (StrokeCap) getInheritedValueFor(FGNProperty.STROKECAP);
         return((sc == null) ? StrokeCap.BUTT : sc);
      }
      else
         return(strokeCap);
   }

   /**
    * Set the decoration that should be applied to the endpoints of unclosed paths or dash segments when stroking any 
    * graphics primitives (other than text glyphs) in the rendering of this graphics node. If a change is made, 
    * <code>onNodeModified()</code> is invoked. If the node does not possess the attribute, this method has no effect.
    * 
    * @param sc The new stroke endcap decoration. A <code>null</code> value makes the attribute implicit, meaning the 
    * node will inherit the style from its parent. 
    * @return <code>True</code> if change was accepted; <code>false</code> if node does not possess this attribute.
    */
   public boolean setStrokeEndcap(StrokeCap sc)
   {
      if((attrFlags & HASSTROKEATTRS) != HASSTROKEATTRS) return(false);

      // if it's being explicitly set to the inherited value, then make it implicit!
      if(sc != null && (sc == getInheritedValueFor(FGNProperty.STROKECAP)))
         sc = null;

      boolean changed = (strokeCap != sc);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.STROKECAP, sc != null ? sc : getInheritedValueFor(FGNProperty.STROKECAP))) 
            return(true);

         StrokeCap oldStrokeCap = strokeCap;
         strokeCap = sc;
         
         if(notify)
         {
            onNodeModified(FGNProperty.STROKECAP);
            FGNRevEdit.post(this, FGNProperty.STROKECAP, strokeCap, oldStrokeCap);
         }
      }
      return(true);
   }

   /**
    * Enumerated attribute determines how path segments are joined or how a path is closed when stroking any graphics 
    * primitives (other than text glyphs) in the rendering of this graphics node. The possible values mirror those 
    * defined in Java2D and Postscript. A <code>null</code> value indicates that the node should inherit the value from 
    * its parent.
    */   
   private StrokeJoin strokeJoin = null;
   
   /**
    * Get the line-join style that determines how path segments are joined or how a path is closed when stroking any 
    * graphics primitives (other than text glyphs) in the rendering of this graphics node. If this node does not possess
    * this basic stroke attribute, or if its stroke join style is currently implicit, then the method returns the join
    * style of the nearest ancestor for which the attribute is explicitly set. In the event that no such inherited value 
    * is found, an application default (<code>StrokeJoin.MITER</code>) is returned.
    * 
    * @return The stroke join style.
    */
   public StrokeJoin getStrokeJoin()
   {
      if((attrFlags & HASSTROKEATTRS) != HASSTROKEATTRS || strokeJoin == null)
      {
         StrokeJoin join = (StrokeJoin) getInheritedValueFor(FGNProperty.STROKEJOIN);
         return((join == null) ? StrokeJoin.MITER : join);
      }
      else
         return(strokeJoin);
   }

   /**
    * Set the line-join style that determines how path segments are joined or how a path is closed when stroking any 
    * graphics primitives (other than text glyphs) in the rendering of this graphics node. If a change is made, 
    * <code>onNodeModified()</code> is invoked. If the node does not possess the attribute, this method has no effect.
    * 
    * @param join The new stroke join style. A <code>null</code> value makes the attribute implicit, meaning the 
    * node will inherit the style from its parent. 
    * @return <code>True</code> if change was accepted; <code>false</code> if node does not possess this attribute.
    */
   public boolean setStrokeJoin(StrokeJoin join)
   {
      if((attrFlags & HASSTROKEATTRS) != HASSTROKEATTRS) return(false);

      // if it's being explicitly set to the inherited value, then make it implicit!
      if(join != null && (join == getInheritedValueFor(FGNProperty.STROKEJOIN)))
         join = null;

      boolean changed = (strokeJoin != join);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.STROKEJOIN, join != null ? join : getInheritedValueFor(FGNProperty.STROKEJOIN)))
            return(true);

         StrokeJoin oldStrokeJoin = strokeJoin;
         strokeJoin = join;
         
         if(notify)
         {
            onNodeModified(FGNProperty.STROKEJOIN);
            FGNRevEdit.post(this, FGNProperty.STROKEJOIN, strokeJoin, oldStrokeJoin);
         }
      }
      return(true);
   }

   /**
    * The dashing pattern with which this graphics node should stroke any shapes it renders (other than text glyphs, 
    * which are never stroked).  A <code>null</code> value indicates that the node should inherit the value from its 
    * parent.
    */
   private StrokePattern strokePattern = null;

   /**
    * Get the dashing pattern with which this <code>FGraphicNode</code> strokes any shapes it renders (except text 
    * glyphs -- <em>DataNav</em> does not support outlined text). If this node does not possess the stroke pattern 
    * attribute, or if its stroke pattern is currently implicit, then the method returns the stroke pattern of the 
    * nearest ancestor for which the attribute is explicitly set. In the event that no such inherited value is found, 
    * an application default is returned.
    * 
    * @return The stroke pattern.
    */
   public StrokePattern getStrokePattern()
   {
      if((attrFlags & HASSTRKPATNATTR) != HASSTRKPATNATTR || strokePattern == null)
      {
         StrokePattern pat = (StrokePattern) getInheritedValueFor(FGNProperty.STROKEPATN);
         return((pat == null) ? StrokePattern.SOLID : pat);
      }
      else
         return(strokePattern);
   }

   /**
    * Set the dashing pattern with which this <code>FGraphicNode</code> strokes any shapes it renders (except text 
    * glyphs -- <em>DataNav</em> does not support outlined text).  If a change is made, <code>onNodeModified()</code> 
    * is invoked. If the node does not possess the stroke pattern attribute, this method has no effect.
    * 
    * @param sp The new stroke pattern. A <code>null</code> value makes the attribute implicit, meaning the node
    * will inherit the style from its parent. 
    * @return <code>True</code> if change was accepted; <code>false</code> if node does not possess the stroke pattern 
    * attribute.
    */
   public boolean setStrokePattern(StrokePattern sp)
   {
      if((attrFlags & HASSTRKPATNATTR) != HASSTRKPATNATTR) return(false);

      // if it's being explicitly set to the inherited value, then make it implicit!
      if((sp != null) && StrokePattern.equal(sp, (StrokePattern)getInheritedValueFor(FGNProperty.STROKEPATN)))
         sp = null;

      boolean changed = (strokePattern != sp) && !StrokePattern.equal(strokePattern, sp);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.STROKEPATN, sp != null ? sp : getInheritedValueFor(FGNProperty.STROKEPATN))) 
            return(true);

         StrokePattern oldStrokePat = strokePattern;
         strokePattern = sp;
         
         if(notify)
         {
            onNodeModified(FGNProperty.STROKEPATN);
            FGNRevEdit.post(this, FGNProperty.STROKEPATN, strokePattern, oldStrokePat);
         }
      }
      return(true);
   }

   /**
    * Find the inherited value of the specified style attribute (font family, etc; fill color; stroke color, etc). The 
    * method does so by traversing the ancestors of this <code>FGraphicNode</code>, looking for the nearest ancestor 
    * for which the specified attribute is explicitly set (ie, not <code>null</code>).
    * 
    * <p><strong>CAUTION</strong>: The method may return <code>null</code> if the graphic node is not currently 
    * installed in an instance of <code>FGraphicModel</code> -- so use with care!</p>
    * 
    * @param style The style attribute for which the inherited value is sought.
    * @return The inherited value. Callers must cast this to the appropriate type.
    */
   Object getInheritedValueFor(FGNProperty style)
   {
      Object styleRet = null;
      FGraphicNode n = parent;
      while(n != null)
      {
         switch(style)
         {
            case FONTFAMILY: styleRet = n.fontFamily; break;
            case FONTSTYLE: styleRet = n.fontStyle; break;
            case FONTSIZE: styleRet = n.fontSizeInPoints; break;
            case ALTFONT: styleRet = n.altFont; break;
            case PSFONT: styleRet = n.psFont; break;
            case FILLC: styleRet = n.fillC; break;
            case STROKEC: styleRet = n.strokeC; break;
            case STROKEW: styleRet = n.strokeMeasure; break;
            case STROKECAP: styleRet = n.strokeCap; break;
            case STROKEJOIN: styleRet = n.strokeJoin; break;
            case STROKEPATN: styleRet = n.strokePattern; break;
            default: break;
         }
         if(styleRet != null) break;
         n = n.parent;
      }

      return(styleRet == null ? getDefaultStyleValueFromPreferences(style) : styleRet);
   }

   /**
    * Get the default value for the specified style property.
    * 
    * <p>If a style property has not been explicitly set on the root graphic node, then its current value is set IAW
    * a user-defined preferred value maintained in the <em>DataNav</em> workspace. This method returns that preferred 
    * value. All style properties have such a preferred default value -- except <em>stroke pattern</em>, which always 
    * defaults to <em>solid</em>.</p>
    * 
    * @param style The requested style property.
    * @return The default value for that property.
    */
   private Object getDefaultStyleValueFromPreferences(FGNProperty style)
   {
      FGNPreferences prefs = FGNPreferences.getInstance();
      Object value = null;
      switch(style)
      {
         case FONTFAMILY: value = prefs.getPreferredFont(); break;
         case FONTSTYLE: value = prefs.getPreferredFontStyle(); break;
         case FONTSIZE: value = prefs.getPreferredFontSize(); break;
         case ALTFONT: value = prefs.getPreferredAltFont(); break;
         case PSFONT: value = prefs.getPreferredPSFont(); break;
         case FILLC: value = prefs.getPreferredFillColor(); break;
         case STROKEC: value = prefs.getPreferredStrokeColor(); break;
         case STROKEW: value = prefs.getPreferredStrokeWidth(); break;
         case STROKECAP: value = prefs.getPreferredStrokeEndcap(); break;
         case STROKEJOIN: value = prefs.getPreferredStrokeJoin(); break;
         case STROKEPATN: value = StrokePattern.SOLID;
         default: break;
      }
      return(value);
   }

   /**
    * Resets one or all of this <code>FGraphicNode</code>'s styles to their implicit, inherited values. The method will 
    * optionally reset the styles of all descendants of the node as well. The method <code>onNodeModified()</code> is 
    * then invoked to update and re-render the containing graphic model.
    * 
    * @param style If <code>null</code>, all style properties are restored to their implicit state. Otherwise, only the 
    * specified style is restored. If the argument does not identify one of the style properties, the method has no 
    * effect.
    * @param includeDescendants If this flag is set, the styles of all descendant nodes are reset as well.
    */
   public void restoreDefaultStyles(FGNProperty style, boolean includeDescendants)
   {
      // this method only affects the graphic styling properties
      if(style != null && !isStyleProperty(style)) return;
      
      // handle the simple case in which only a single property change is made
      if(style != null && !includeDescendants) 
      {
         switch(style)
         {
            case FONTFAMILY: setFontFamily(null); break;
            case FONTSTYLE: setFontStyle(null); break;
            case FONTSIZE: setFontSizeInPoints(null); break;
            case ALTFONT: setAltFont(null); break;
            case PSFONT: setPSFont(null); break;
            case FILLC: setFillColor(null); break;
            case STROKEC: setStrokeColor(null); break;
            case STROKEW: setMeasuredStrokeWidth(null); break;
            case STROKECAP: setStrokeEndcap(null); break;
            case STROKEJOIN: setStrokeJoin(null); break;
            case STROKEPATN: setStrokePattern(null); break;
            default: break;
         }
         return;
      }

      // this operation may involve lots of property changes, so we block the model's undo history during it. Abort if
      // node is not currently part of a figure model
      FGraphicModel model = getGraphicModel();
      if(model != null) model.blockEditHistory();
      else return;
      
      // prepare the reversible which will rollback (or re-apply!) all of the property changes that get made here
      String desc = "";
      desc += (style != null) ? style.getNiceName() : "All graphic styles";
      desc += " reset to " + ((style != null) ? "default value" : "defaults");
      desc += " on " + getNodeType().getNiceName().toLowerCase();
      if(includeDescendants) desc += " and all descendants";
      MultiRevEdit undoer = new MultiRevEdit(model, desc);
      
      Stack<FGraphicNode> nodeStack = new Stack<>();
      nodeStack.push(this);
      while(!nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();
         if(n.hasFontProperties())
         {
            if((style == null || style == FGNProperty.FONTFAMILY) && (n.fontFamily != null))
            {
               undoer.addPropertyChange(n, FGNProperty.FONTFAMILY, null, n.fontFamily);
               n.fontFamily = null;
            }
            if((style == null || style == FGNProperty.FONTSTYLE) && (n.fontStyle != null))
            {
               undoer.addPropertyChange(n, FGNProperty.FONTSTYLE, null, n.fontStyle);
               n.fontStyle = null;
            }
            if((style == null || style == FGNProperty.FONTSIZE) && (n.fontSizeInPoints != null))
            {
               undoer.addPropertyChange(n, FGNProperty.FONTSIZE, null, n.fontSizeInPoints);
               n.fontSizeInPoints = null;
            }
            if((style == null || style == FGNProperty.ALTFONT) && (n.altFont != null))
            {
               undoer.addPropertyChange(n, FGNProperty.ALTFONT, null, n.altFont);
               n.altFont = null;
            }
            if((style == null || style == FGNProperty.PSFONT) && (n.psFont != null))
            {
               undoer.addPropertyChange(n, FGNProperty.PSFONT, null, n.psFont);
               n.psFont = null;
            }

            if(style != FGNProperty.PSFONT)
               n.cacheResolvedFont();
         }

         if(n.hasFillColorProperty() && (style == null || style == FGNProperty.FILLC) && (n.fillC != null))
         {
            undoer.addPropertyChange(n, FGNProperty.FILLC, null, n.fillC);
            n.fillC = null;
         }

         if(n.hasStrokeProperties())
         {
            if((style == null || style == FGNProperty.STROKEC) && (n.strokeC != null))
            {
               undoer.addPropertyChange(n, FGNProperty.STROKEC, null, n.strokeC);
               n.strokeC = null;
            }
            if((style == null || style == FGNProperty.STROKEW) && (n.strokeMeasure != null))
            {
               undoer.addPropertyChange(n, FGNProperty.STROKEW, null, n.strokeMeasure);
               n.strokeMeasure = null;
            }
            if((style == null || style == FGNProperty.STROKECAP) && (n.strokeCap != null))
            {
               undoer.addPropertyChange(n, FGNProperty.STROKECAP, null, n.strokeCap);
               n.strokeCap = null;
            }
            if((style == null || style == FGNProperty.STROKEJOIN) && (n.strokeJoin != null))
            {
               undoer.addPropertyChange(n, FGNProperty.STROKEJOIN, null, n.strokeJoin);
               n.strokeJoin = null;
            }
         }

         if(n.hasStrokePatternProperty() && 
               (style == null || style == FGNProperty.STROKEPATN) && (n.strokePattern != null))
         {
            undoer.addPropertyChange(n, FGNProperty.STROKEPATN, null, n.strokePattern);
            n.strokePattern = null;
         }

         if(includeDescendants)
            for(FGraphicNode sub : n.subordinates) nodeStack.push(sub);
      }

      // now update the rendering infrastructure, but only if some changes were made
      boolean changed = (undoer.getNumberOfChanges() > 0);
      if(changed)
         onNodeModified(null);
      
      // post our multiple-change reversible edit to the model's edit history
      model.unblockEditHistory();
      if(changed) model.postReversibleEdit(undoer);
   }

   /**
    * Can this node's current styling be restored to the implicit, inherited state? Note that the action is temporarily
    * disabled whenever the owner model's current selection contains more than one object.
    * @return <code>True</code> if any of the node's style properties has been explicitly set.
    */
   final public boolean canRestoreDefaultStyles()
   {
      FGraphicModel fgm = getGraphicModel();
      if(fgm != null && fgm.isMultiNodeSelection()) return(false);
      
      boolean canRestore = false;
      if(hasFontProperties() && (fontFamily != null || fontStyle != null || fontSizeInPoints != null || 
            altFont != null || psFont != null))
         canRestore = true;
      else if(hasFillColorProperty() && fillC != null) canRestore = true;
      else if(hasStrokeProperties() && 
               (strokeC != null || strokeMeasure != null || strokeCap != null || strokeJoin != null)) 
         canRestore = true;
      else if(hasStrokePatternProperty() && strokePattern != null) canRestore = true;     
      return(canRestore);
   }

   
   //
   // PainterStyle
   //

   /**
    * Caches the font with which to render any text associated with this <code>FGraphicNode</code>. It is derived from 
    * the font resource installed on the host that best matches the node's font family, style, size, and alternate font 
    * attributes. Any time those styling attributes change, this must be nullified to force a redetermination of the 
    * best-match font.
    */
   private Font resolvedFont = null;

   /**
    * Get the installed font that best matches this node's current font-related styles. If the node does not have the 
    * font-related styles, the method will return the parent's resolved font.
    * 
    * @return A font to render text for this <code>FGraphicNode</code> as described.
    */
   private Font getResolvedFont()
   {
      if((parent != null) && 
            ((fontFamily==null && altFont==null && fontStyle==null && fontSizeInPoints==null) || !hasFontProperties()))
         return(parent.getResolvedFont());
      if(resolvedFont == null)
         cacheResolvedFont();
      return(resolvedFont);
   }

   /** 
    * Helper method recalculates and caches the installed font that best matches this node's current font-related 
    * styles. It must be called any time the font family, font style, alternate font, or font size changes.
    */
   private void cacheResolvedFont()
   {
      resolvedFont = null;
      if((parent != null) && 
            ((fontFamily==null && altFont==null && fontStyle==null && fontSizeInPoints==null) || !hasFontProperties()))
         return;
      resolvedFont = LocalFontEnvironment.resolveFont(getFontFamily(), getAltFont(), getFontStyle());
      resolvedFont = resolvedFont.deriveFont((float)getFontSize());
   }

   /**
    * Helper method recalculates and caches the resolved font for this node and all of its descendants. It should be 
    * invoked whenever a change is made to a font-related property of this node. Otherwise, a descendant node's cached 
    * font may be incorrect. (Recursion is not used.)
    */
   void propagateFontChange()
   {
      Stack<FGraphicNode> nodeStack = new Stack<>();
      nodeStack.push(this);
      while(!nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();
         n.cacheResolvedFont();
         for(FGraphicNode sub : n.subordinates) nodeStack.push(sub);
      }
   }

   public Font getFont()
   {
      return(getResolvedFont());
   }

   public double getFontSize()
   {
      // rem: logical units in rendering coordinate system are always milli-inches!
      return(getFontSizeInPoints() * Measure.PT2IN * 1000.0);
   }

   public Stroke getStroke(float dashPhase)
   {
      // note: DataNav does not support specifying a dash pattern phase.
      float w = (float) getStrokeWidth();
      StrokePattern sp = getStrokePattern();
      StrokeCap cap = getStrokeEndcap();
      StrokeJoin join = getStrokeJoin();
      if(sp.isSolid())
         return(new BasicStroke(w, cap.getJava2DLineCap(), join.getJava2DLineJoin(), 10));
      else
         return(new BasicStroke(w, cap.getJava2DLineCap(), join.getJava2DLineJoin(), 10, 
               sp.getDashGapArrayMilliIn(w, cap != StrokeCap.BUTT), 0));
   }

   public double getStrokeWidth()
   {
      return(getMeasuredStrokeWidth().toMilliInches());
   }

   public boolean isStrokeSolid()
   {
      return(getStrokePattern().isSolid());
   }

   /** Any graphic object is stroked if its stroke width is positive and its stroke color is NOT transparent. */
   public boolean isStroked()
   {
     return(getStrokeWidth() > 0 && getStrokeColor().getAlpha() != 0);
   }


   //
   // Support for location, orientation, and size properties
   //

   /**
    * The x-coordinate of the graphic node's location WRT the parent viewport. Unused if this graphic node is not 
    * relocatable.
    */
   private Measure x;

   /**
    * Get the x-coordinate of this graphic node's location WRT its parent viewport.
    * 
    * @return The x-coordinate with associated units of measure, or null if this node is not relocatable.
    */
   public Measure getX() { return(((attrFlags & HASLOCATTRS) == HASLOCATTRS) ? x : null); }

   /**
    * Set the x-coordinate of this graphic node's location WRT its parent viewport. If a change occurs, {@link 
    * #onNodeModified} is invoked.
    * 
    * @param m The new x-coordinate. It is constrained to satisfy {@link FGraphicModel#getLocationConstraints}. A null
    * value is rejected.
    * @return True if successful; false if value was rejected, or if this node is not relocatable.
    */
   public boolean setX(Measure m)
   {
      if(m == null || (attrFlags & HASLOCATTRS) != HASLOCATTRS) return(false);
      m = FGraphicModel.getLocationConstraints(getNodeType()).constrain(m);

      if((x != m) && !Measure.equal(x, m))
      {
         if(doMultiNodeEdit(FGNProperty.X, m)) return(true);

         Measure oldX = x;
         x = m;
         
         if(notify)
         {
            onNodeModified(FGNProperty.X);
            FGNRevEdit.post(this, FGNProperty.X, x, oldX);
         }
      }
      return(true);
   }

   /**
    * Y-coordinate of graphic node's location WRT the parent viewport. Unused if this graphic node is not relocatable.
    */
   private Measure y;

   /**
    * Get the y-coordinate of this graphic node's location WRT its parent viewport.
    * 
    * @return The y-coordinate with associated units of measure, or null if node is not relocatable.
    */
   public Measure getY() { return(((attrFlags & HASLOCATTRS) == HASLOCATTRS) ? y : null); }

   /**
    * Set the y-coordinate of this graphic node's location WRT its parent viewport. If a change occurs, {@link 
    * #onNodeModified} is invoked.
    * 
    * @param m The new y-coordinate. It is constrained to satisfy {@link FGraphicModel#getLocationConstraints}. A null
    * value is rejected.
    * @return True if successful; false if value was rejected, or if this node is not relocatable.
    */
   public boolean setY(Measure m)
   {
      if(m == null || (attrFlags & HASLOCATTRS) != HASLOCATTRS) return(false);
      m = FGraphicModel.getLocationConstraints(getNodeType()).constrain(m);

      if((y != m) && !Measure.equal(y, m))
      {
         if(doMultiNodeEdit(FGNProperty.Y, m)) return(true);

         Measure oldY = y;
         y = m;
         
         if(notify)
         {
            onNodeModified(FGNProperty.Y);
            FGNRevEdit.post(this, FGNProperty.Y, y, oldY);
         }
      }
      return(true);
   }

   /**
    * Set both the x- and y-coordinates of this node's location WRT its parent viewport. If a change is made, {@link 
    * #onNodeModified} is invoked (unless model notifications are currently disabled).
    * 
    * @param xm The new x-coordinate. The measure is constrained to satisfy the location constraints for this node. A 
    * null value is rejected.
    * @param ym The new y-coordinate. Same restrictions as on the x-coordinate.
    * @return True if successful; false if either value was rejected, or if this node is not relocatable.
    */
   public boolean setXY(Measure xm, Measure ym) 
   { 
      if(xm == null || ym == null || (attrFlags & HASLOCATTRS) != HASLOCATTRS) return(false);
      Measure.Constraints c = FGraphicModel.getLocationConstraints(getNodeType());
      xm = c.constrain(xm);
      ym = c.constrain(ym);

      boolean changed = ((x != xm) && !Measure.equal(x, xm)) || ((y != ym) && !Measure.equal(y, ym));
      if(changed)
      {
         Measure oldX = x;
         Measure oldY = y;
         x = xm;
         y = ym;
         
         if(notify)
         {
            onNodeModified(FGNProperty.X);
            FGNRevEdit.postMove(this, oldX, oldY);
         }
      }
      return(true);
   }

   /**
    * The measured width of the node's bounding box relative to the parent's viewport. Unused if this graphic node does
    * not possess a resizable bounding box.
    */
   private Measure width;

   /**
    * Get the measured width of this graphic node's bounding box WRT its parent viewport.
    * 
    * @return The width with associated units of measure, or null if this node does not have a resizable bounding box.
    */
   public Measure getWidth() { return(((attrFlags & HASSIZEATTRS) == HASSIZEATTRS) ? width : null); }

   /**
    * Set the measured width of this graphic node's bounding box WRT its parent viewport. If a change occurs, {@link 
    * #onNodeModified} is invoked.
    * 
    * @param m The new width. The measure is constrained by {@link FGraphicModel#getSizeConstraints}. A null value is
    * rejected. 
    * @return True if successful; false if value was rejected, or if this node does not have a resizable bounding box.
    */
   public boolean setWidth(Measure m)
   {
      if(m == null || (attrFlags & HASSIZEATTRS) != HASSIZEATTRS) return(false);
      m = FGraphicModel.getSizeConstraints(getNodeType()).constrain(m);

      if((width != m) && !Measure.equal(width, m))
      {
         if(doMultiNodeEdit(FGNProperty.WIDTH, m)) return(true);

         Measure oldW = width;
         width = m;
         
         if(notify)
         {
            onNodeModified(FGNProperty.WIDTH);
            FGNRevEdit.post(this, FGNProperty.WIDTH, width, oldW);
         }
      }
      return(true);
   }

   /**
    * The measured height of the node's bounding box relative to the parent's viewport. Unused if this graphic node does
    * not possess a resizable bounding box.
    */
   private Measure height;

   /**
    * Get the measured height of this graphic node's bounding box WRT its parent viewport.
    * 
    * @return The height with associated units of measure, or null if this node does not have a resizable bounding box.
    */
   public Measure getHeight() { return(((attrFlags & HASSIZEATTRS) == HASSIZEATTRS) ? height : null); }

   /**
    * Set the measured height of this graphic node's bounding box WRT its parent viewport. If a change occurs, {@link 
    * #onNodeModified} is invoked.
    * 
    * @param m The new height. The measure is constrained by {@link FGraphicModel#getSizeConstraints}. A null value is
    * rejected. 
    * @return True if successful; false if value was rejected, or if this node does not have a resizable bounding box.
    */
   public boolean setHeight(Measure m)
   {
      if(m == null || (attrFlags & HASSIZEATTRS) != HASSIZEATTRS) return(false);
      m = FGraphicModel.getSizeConstraints(getNodeType()).constrain(m);

      if((height != m) && !Measure.equal(height, m))
      {
         if(doMultiNodeEdit(FGNProperty.HEIGHT, m)) return(true);

         Measure oldH = height;
         height = m;
         
         if(notify)
         {
            onNodeModified(FGNProperty.HEIGHT);
            FGNRevEdit.post(this, FGNProperty.HEIGHT, height, oldH);
         }
      }
      return(true);
   }

   /**
    * The orientation of this node, expressed as the angle -- in degrees -- between the positive horizontal axes of 
    * this node's viewport and its parent viewport. Positive angles correspond to CCW rotation. Ignored if this node 
    * may not be rotated WRT its parent container. 
    */
   private double rotate = 0;

   /**
    * Get the orientation of this <code>FGraphicNode</code> with respect to its parent. Orientation is expressed as 
    * the angle of rotation between the positive horizontal axes of this node's viewport and its parent viewport. 
    * Positive angles correspond to CCW rotation.
    * 
    * @return The rotation angle in degrees. If this node may not be rotated WRT its parent, 0 is always returned.
    */
   public double getRotate() {  return(((attrFlags & HASROTATEATTR) == HASROTATEATTR) ? rotate : 0); }

   /**
    * Get the orientation of this graphic node WRT the root figure. This method simply sums up the rotation angles of
    * this node and its ancestors (if a node lacks the <i>rotate</i> property, then its rotation is 0), restricting
    * the result to [0..360) degrees.
    * @return The net rotation of this node WRT the coordinate system of the root figure.
    */
   double getNetRotate()
   {
      double r = getRotate();
      FGraphicNode fgn = getParent();
      while(fgn != null)
      {
         r += fgn.getRotate();
         fgn = fgn.getParent();
      }
      return(Utilities.restrictAngle(r));
   }
   
   /**
    * Set the orientation of this <code>FGraphicNode</code> with respect to its parent. If a change is made, 
    * <code>onNodeModified()</code> is invoked.
    * 
    * @param r The rotation angle, in degrees. Restricted to [-180..180].
    * @return <code>True</code> if new rotation angle is accepted without correction; <code>false</code> if node may not 
    * be rotated WRT its parent, or if the new rotation angle had to be corrected to the allowed range.
    */
   public boolean setRotate(double r)
   {
      if((attrFlags & HASROTATEATTR) != HASROTATEATTR) return(false);

      boolean adjusted = false;
      if(r < -180 || r > 180)
      {
         r = Utilities.restrictAngle(r) - 180;
         adjusted = true;
      }

      if(r != rotate)
      {
         if(doMultiNodeEdit(FGNProperty.ROTATE, r)) return(!adjusted);

         Double old = rotate;
         rotate = r;
         
         if(notify)
         {
            onNodeModified(FGNProperty.ROTATE);
            FGNRevEdit.post(this, FGNProperty.ROTATE, rotate, old);
         }
      }
      return(!adjusted);
   }


   //
   // Rescaling
   //
   
   /** Smallest scale factor for a rescaling operation, as a whole percentage: 10%. */
   public final static int MINRESCALE = 10;

   /** Largest scale factor for a rescaling operation, as a whole percentage: 200%. */
   public final static int MAXRESCALE = 200;
   
   /**
    * Helper method for <code>rescale()</code>. An implementing class should scale all size- or position-related 
    * properties IAW with the specified scale factor. Do not adjust relative (% or user units) measures, since these 
    * adjust automatically when the parent is resized. Do not adjust properties which are handled by the 
    * <code>FGraphicNode</code> base class itself: <em>font size, stroke width, X, Y, width,</em> and <em>height</em>.
    * <strong><em>Do not invoke <code>onNodeModified()</code> when making any property changes</em></strong>; it will 
    * be called by <code>rescale()</code> after the entire rescaling operation is complete.
    * 
    * <p>The default base class implementation is a no-op. Do not invoke this method directly.</p>
    * 
    * @param scale The multiplicative scale factor.
    * @param undoer For each property changed, register the change with this undo action so that the entire rescale 
    * operation can be redone if requested.
    */
   protected void rescaleSelf(double scale, MultiRevEdit undoer) {}

   /**
    * Rescale the graphic described by this <code>FGraphicNode</code> and its descendants.
    * 
    * <p>This method traverses the graphic tree rooted at this <code>FGraphicNode</code> and scales all size- and 
    * position-related properties of each node IAW the specified scale factor. The method updates all properties 
    * handled by the <code>FGraphicNode</code> class: font size, stroke width, X, Y, width, and height. It invokes 
    * <code>rescaleSelf(int)</code> to scale any other size- or position-related properties that are specific to the 
    * implementing class. After traversing the tree, the method invokes <code>onNodeModified()</code> to inform the 
    * containing model and trigger a re-rendering of the graphic.</p>
    * 
    * <p>The method is package-protected. It is really only intended for use on container-type nodes, like a figure or
    * graph. It is implemented here to permit a non-recursive implementation that avoids triggering model notifications 
    * until all necessary changes have been made.</p>
    *  
    * @param pct The scale factor, as a percentage. Allowed range: [<code>MINRESCALE</code>..<code>MAXRESCALE</code>].
    * @param fontSizeOnly If <code>true</code>, then only the fonts are rescaled. Otherwise, this graphic node and all 
    * of its descendants are rescaled.
    * @see FGraphicNode#rescaleSelf
    */
   void rescale(int pct, boolean fontSizeOnly)
   {
      if( pct < 10 || pct > 200 || pct == 100) return;
      double scale = ((double)pct)/100.0;

      // this operation may involve lots of property changes, so we block the model's undo history during it. Abort if
      // node is not currently part of a figure model.
      FGraphicModel model = getGraphicModel();
      if(model != null) model.blockEditHistory();
      else return;
      
      // prepare the undo action which will rollback all of the property changes that get made here
      String desc = "Rescaling of " + getNodeType().getNiceName().toLowerCase();
      desc += "(" + pct + "%" + (fontSizeOnly ? "; fonts only)" : ")");
      MultiRevEdit undoer = new MultiRevEdit(model, desc);
      
      Stack<FGraphicNode> nodeStack = new Stack<>();
      nodeStack.push(this);
      while(!nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();

         if(n.hasFontProperties() && (n == this || n.fontSizeInPoints != null))
         {
            Integer old = n.getFontSizeInPoints();
            int fontSz = (int) Math.round(scale * n.getFontSizeInPoints());
            n.fontSizeInPoints = Utilities.rangeRestrict(MINFONTSIZE, MAXFONTSIZE, fontSz);
            n.cacheResolvedFont();
            
            undoer.addPropertyChange(n, FGNProperty.FONTSIZE, n.getFontSizeInPoints(), old);
         }

         if(!fontSizeOnly)
         {
            if(n.hasStrokeProperties() && (n == this || n.strokeMeasure != null))
            {
               Measure oldSW = n.getMeasuredStrokeWidth();

               Measure m = n.getMeasuredStrokeWidth();
               m = new Measure(m.getValue()*scale, m.getUnits());
               n.strokeMeasure = STROKEWCONSTRAINTS.constrain(m);
               
               undoer.addPropertyChange(n, FGNProperty.STROKEW, n.getMeasuredStrokeWidth(), oldSW);
            }
            if((n.attrFlags & HASLOCATTRS) == HASLOCATTRS && (n != this))
            {
               Measure.Constraints c = FGraphicModel.getLocationConstraints(n.getNodeType());
               Measure m = n.getX();
               if(!m.isRelative())
               {
                  n.x = new Measure(m.getValue()*scale, m.getUnits());
                  n.x = c.constrain(n.x);
                  undoer.addPropertyChange(n, FGNProperty.X, n.x, m);
               }

               m = n.getY();
               if(!m.isRelative())
               {
                  n.y = new Measure(m.getValue()*scale, m.getUnits());
                  n.y = c.constrain(n.y);
                  undoer.addPropertyChange(n, FGNProperty.Y, n.y, m);
               }
            }
            if((n.attrFlags & HASSIZEATTRS) == HASSIZEATTRS)
            {
               Measure.Constraints c = FGraphicModel.getSizeConstraints(n.getNodeType());
               Measure m = n.getWidth();
               if((!m.isRelative()) || (n==this))
               {
                  n.width = new Measure(m.getValue()*scale, m.getUnits());
                  n.width = c.constrain(n.width);
                  undoer.addPropertyChange(n, FGNProperty.WIDTH, n.width, m);
               }
               
               m = n.getHeight();
               if((!m.isRelative()) || (n==this))
               {
                  n.height = new Measure(m.getValue()*scale, m.getUnits());
                  n.height = c.constrain(n.height);
                  undoer.addPropertyChange(n, FGNProperty.HEIGHT, n.height, m);
               }
            }

            n.rescaleSelf(scale, undoer);
         }

         for(FGraphicNode sub : n.subordinates) nodeStack.push(sub);
      }

      propagateFontChange();
      onNodeModified(null);
      
      // post the multiple-change reversible edit to the model's edit history
      model.unblockEditHistory();
      model.postReversibleEdit(undoer);
   }

   
   // 
   // Support for title property
   //

   /**
    * The generic title attribute for this graphic node.
    */
   private String title;

   /**
    * Does this graphic node possess a <i>title</i> property?
    * 
    * @return True if node has a title (even if that title is an empty string).
    */
   public boolean hasTitle() { return((attrFlags & HASTITLEATTR) == HASTITLEATTR); }
   
   /**
    * Are line feed characters ('\n') allowed in this node's title string? Certain graphic nodes admit one or more 
    * line feeds to introduce line breaks in the string. 
    * @return True if title string can contain line feeds. Returns false always if node lacks the <i>title</i> property.
    */
   public boolean allowLineBreaksInTitle() { return(hasTitle() && ((attrFlags & ALLOWLFINTITLE) == ALLOWLFINTITLE)); }
   
   /** 
    * Can this node's title be specified in "styled text" format, in which the font style, text color, underline,
    * subscript and superscript attributes can vary from one character to the next? If so, the "title" text has the
    * form "S|[codes]", where S is the actual character sequence and "[codes]" succinctly encodes any attribute changes
    * within that sequence.
    * 
    * @return True if node's title supports the "styled text" format.
    */
   public boolean allowStyledTextInTitle() { return(hasTitle() && ((attrFlags & ALLOWATTRINTITLE) == ALLOWATTRINTITLE)); }
   
   /**
    * Get the title for this graphic node.
    * 
    * @return The title string. If this node does not possess the title attribute, the method returns an empty string.
    */
   public String getTitle() { return(((attrFlags & HASTITLEATTR) == HASTITLEATTR) ? title : ""); }

   /**
   * Set the title for this graphic node. If a change is made, the method <code>onNodeModified()</code> is 
   * invoked. Unsupported characters in the string are replaced by a "?".
   * 
   * @param s The new title. Null is treated as the equivalent of an empty string. Tab and carriage return characters
   * are silently stripped from the string; while they are part of the supported character set, they're not allowed in
   * the title string. Likewise, any line feed characters are removed -- unless the graphic node allows them (certain
   * nodes use the line feed characters to break the title string into multiple text lines).
   * @return True if successful; false if this node does not possess the title attribute, or if title was updated with
   * changes (any tab, CR, or LF characters removed; any unsupported characters replaced by a question mark).
   */
   public boolean setTitle(String s)
   {
      if((attrFlags & HASTITLEATTR) != HASTITLEATTR) return(false);

      String str = (s==null) ? "" : s;
      str = str.replaceAll("\r", "");
      str = str.replaceAll("\u0009", "");
      if(!allowLineBreaksInTitle()) str = str.replaceAll("\n", "");
      if(!str.equals(title))
      {
         if(doMultiNodeEdit(FGNProperty.TITLE, str)) 
            return(FGraphicModel.replaceUnsupportedCharacters(str).equals(s));

         String oldTitle = title;
         title = FGraphicModel.replaceUnsupportedCharacters(str);
         
         if(notify)
         {
            onNodeModified(FGNProperty.TITLE);
            FGNRevEdit.post(this, FGNProperty.TITLE, title, oldTitle,
                  "Change title on " + getNodeType().getNiceName());
         }
      }
      return(str.equals(title));
   }

   /**
    * Does this graphic node's <i>title</i> property use <i>FypML</i> styled text? The styled text format was introduced
    * in FC 5.4.0; see {@link #toStyledText}.
    * 
    * <p><b>This method returns true if the title has the styled text format "S|N1:codes1,N2:codes2,...", where the 
    * codes suffix after the pipe ('|') character contains no invalid characters. It does not validate the format of the
    * codes suffix.</b></p>
    * 
    * @return True if the node's title is attributed text.
    */
   public boolean hasAttributedTextInTitle()
   {
      String s = getTitle().trim();
      int idx = s.lastIndexOf('|');
      return((idx > 0) && (idx < s.length() - 1) && styledTextSuffixPattern.matcher(s.substring(idx+1)).matches());
   }

   /**
    * Get the title for this graphic node as an attributed string for rendering purposes.
    * 
    * @param fontSzInMI True if font size should be specified in milli-inches rather than points.
    * @return The title as an attributed string. If the node lacks the <i>title</i> property, returns an empty string.
    */
   public AttributedString getAttributedTitle(boolean fontSzInMI)
   {
      return fromStyledText(getTitle().trim(), getFontFamily(), getFontSizeInPoints(), getFillColor(), 
            getFontStyle(), fontSzInMI);
   }
   
   /**
    * Prepare a short descriptive label for this graphic node to be displayed in the FigureComposer user interface.
    * 
    * <p>Returns the node's <i>title</i>property or, if that is an empty string, the node's type name. If the string 
    * exceeds 50 characters, it is truncated.</p>
    * 
    * @return The graphic node's label for user-facing display.
    */
   public AttributedString getUILabel()
   {
      if(hasAttributedTextInTitle())
      {
         AttributedString as = fromStyledText(getTitle().trim(), getFontFamily(), 
               getFontSizeInPoints(), getFillColor(), getFontStyle(), false);
         AttributedCharacterIterator aci = as.getIterator();
         int len = aci.getEndIndex() - aci.getBeginIndex();
         if(len > 0)
            return((len <= 50) ? as : new AttributedString(aci, 0, 50));
      }

      String title = getTitle().trim();
      if(title.isEmpty()) title = getNodeType().getNiceName();
      if(title.length() > 50) title = title.substring(0,50) + "...";
         
      // replace any whitespace character (other than space char) with a '|' -- since the title is intended to be
      // a simple phrase displayed on a single line
      title = title.replaceAll("[\n\r\t\f]", "|");

      return new AttributedString(title);
   }

   // 
   // Support for FypML styled text format
   //
   
   /** For validating the attributes suffix of a <i>FypML</i> styled text string. */
   private final static Pattern styledTextSuffixPattern = Pattern.compile("[a-fA-F0-9SsnUupwiI:,-]+");
   private final static Integer NORMSCRIPT = 0;
   private final static Integer UNDERLINE_OFF = -1;
   
   /** 
    * Convert an {@link AttributedString} to a <i>FypML</i> styled text string.
    * 
    * <p>The styled text format was introduced in FC 5.4.0 (schema version 24) to specify text in which a few select
    * attributes -- font style, text color, superscript, subscript, and underline -- can vary on a character by 
    * character basis. It is used for the <i>title</i> property of some elements in the <i>FypML</i> graphic model.</p>
    * 
    * <p>Avoiding the complexity of defining a separate XML element for each "text run" (like-styled character sequence)
    * within the text content, this internal format succinctly encodes any attribute changes in a plain-text "suffix" 
    * appended to the character sequence:
    * <pre>     S|N1:codes1,N2:acodes2,... </pre>
    * where S is a sequence of characters (with no leading or trailing whitespace), 0 < N1 < N2 < ... < len(S) are 
    * indices into the character sequence at which a change in one or more of the supported text attributes occurs, 
    * "codesN" is a short string defining the attribute changes applied from index N to the end of the character 
    * sequence (or the next attribute change!).</p>
    * <p>The notion of a default text color and font style are key to encoding the styled text as succinctly as
    * possible. Attribute <b>changes</b> are encoded as follows, in order:
    * <ul>
    * <li>Text color: If changing to a color other than the default text color, that color is specified as a 6-digit or
    * 8-digit (AARRGGBB) hexadecimal string. However, if changing from another color BACK to the default, the single '-'
    * character is included. If the default color applies at the start of S, no color code is needed at N=0.</li>
    * <li>Font style: If font style changes, a single character is included: 'w' for bold, 'i' for italic, 'I' for 
    * bolditalic, and 'p' for plain. Again, if the default font style applies at the start of the string, no font
    * style code is needed at N=0.</li>
    * <li>Underline state: "U" turns underlining on and "u" turns it off. If underlining is off at the start of the 
    * string, no underline code is needed at N=0.</li>
    * <li>Superscript/subscript state: "S" indicates superscript, "s" indicates subscript, and "n" normal text. Again, 
    * if neither superscript nor subscript apply at the start of the string, no code is needed at N=0.</li>
    * </ul>
    * For example, in the styled text "y = x2 + 4|5:0000ffUS,6:-un", the character 2 is blue, underlined, and 
    * superscript, and the defaults are restored at the following character (a space) in the sequence.</p>
    * <p>Any other text attributes in the attributed string are ignored. Since the "styling suffix" does not use the '|'
    * character, it is easy to separate the suffix from the character sequence S. Note that, if there are no style
    * changes at all, then this method simply returns the character sequence.
    * </p>
    * 
    * @param as An attributed string.
    * @param implicitFontStyle The default font style.
    * @param implicitTextC The default text color.
    * @return Styled text string representation in the format described above.
    */
   public static String toStyledText(AttributedString as, FontStyle implicitFontStyle, Color implicitTextC)
   {
      if(as == null) return("");
      AttributedCharacterIterator aci = as.getIterator();
      if(aci.getEndIndex() - aci.getBeginIndex() < 1) return("");
      
      if(implicitFontStyle == null) implicitFontStyle = FontStyle.PLAIN;
      if(implicitTextC == null) implicitTextC = Color.black;
      
      // first, get the actual text
      StringBuilder textBuf = new StringBuilder(aci.getEndIndex());
      for(char c=aci.first(); c != AttributedCharacterIterator.DONE; c=aci.next())
         textBuf.append(c);
      
      // then prepare the attribute code changes
      Color prevTextC = implicitTextC;
      Float prevWeight = implicitFontStyle.isBold() ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR;
      Float prevPosture = implicitFontStyle.isItalic() ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR;
      Integer prevUL = UNDERLINE_OFF;
      Integer prevScript = NORMSCRIPT;
      
      
      boolean gotCode = false;  // set true after first attribute code change is appended to textBuf
      for(char c=aci.first(); c != AttributedCharacterIterator.DONE; c=aci.next())
      {
         String attrCode = "";
         
         Object value = aci.getAttribute(TextAttribute.FOREGROUND);
         if((value instanceof Color) && !prevTextC.equals(value))
         {
            Color textC = (Color) value;
            if(textC.equals(implicitTextC) && (aci.getIndex() > 0))
               attrCode += "-";
            else
            {
               String colorCode = BkgFill.colorToHexString(textC);
               if(colorCode.equals("none")) colorCode = "000000";
               attrCode += colorCode;
            }
            prevTextC = textC;
         }
         
         value = aci.getAttribute(TextAttribute.WEIGHT);
         Float weight = (value != null) ? ((Float) value) : prevWeight;
         value = aci.getAttribute(TextAttribute.POSTURE);
         Float posture = (value != null) ? ((Float) value) : prevPosture;
         if( !(prevWeight.equals(weight) && prevPosture.equals(posture)) )
         {
            if(weight.equals(TextAttribute.WEIGHT_BOLD))
               attrCode += posture.equals(TextAttribute.POSTURE_REGULAR) ? "w" : "I";
            else if(posture.equals(TextAttribute.POSTURE_OBLIQUE))
               attrCode += "i";
            else
               attrCode += "p";
            prevWeight = weight;
            prevPosture = posture;
         }
         
         value = aci.getAttribute(TextAttribute.UNDERLINE);
         if((value instanceof Integer) && !prevUL.equals(value))
         {
            Integer ul = (Integer) value;
            attrCode += ul.equals(TextAttribute.UNDERLINE_ON) ? "U" : "u";
            prevUL = ul;
         }

         value = aci.getAttribute(TextAttribute.SUPERSCRIPT);
         if((value instanceof Integer) && !prevScript.equals(value))
         {
            Integer script = (Integer) value;
            attrCode += script.equals(TextAttribute.SUPERSCRIPT_SUPER) ? "S" : 
               (script.equals(TextAttribute.SUPERSCRIPT_SUB) ? "s" : "n");
            prevScript = script;
         }
         
         if(!attrCode.isEmpty())
         {
            if(!gotCode)
            {
               gotCode = true;
               textBuf.append('|');
            }
            textBuf.append(String.format("%d:%s,", aci.getIndex(), attrCode));
         }
            
      }
      
      // if we added any attribute codes, be sure to leave off the last ','
      return(gotCode ? textBuf.substring(0, textBuf.length()-1) : textBuf.toString());
   }
   
   /**
    * Convert a <i>FypML</i> styled text string to a {@link AttributedString}. The following text attributes are 
    * included:
    * <ul>
    * <li>{@link TextAttribute#FAMILY} -- The font family.</li>
    * <li>{@link TextAttribute#SIZE} -- The font size.</li>
    * <li>{@link TextAttribute#WEIGHT} -- The font weight (bold or regular).</li>
    * <li>{@link TextAttribute#POSTURE} -- The font posture (oblique or regular).</li>
    * <li>{@link TextAttribute#FOREGROUND} -- The text color.</li>
    * <li>{@link TextAttribute#UNDERLINE} -- The underline state: on or off.</li>
    * <li>{@link TextAttribute#SUPERSCRIPT} -- The superscript state: superscript, subscript, or neither.</li>
    * </ul>
    * 
    * <p>The styled text argument T must adhere to the format <b>S|N1:codes1,N2:codes2,...</b> as described in {@link 
    * #toStyledText}. The encoded attribute changes do not use the '|' character, so we can always separate the
    * character sequence S from the attribute changes. If the '|' separator is missing, the entire text is treated as S,
    * and the default styling is applied to the entire sequence. If S is an empty string, any attribute codes are 
    * ignored. <i>If a parsing error is detected, then the attributed string returned contains the entire text content
    * T styled uniformly IAW the other arguments.</i></p>
    * 
    * @param attrText A <i>FypML</i> styled text string.
    * @param fam The font family in which the text should be rendered. If not installed on host, will use a generic
    * sanserif font.
    * @param size The font size for rendering in typographical points. Range-restricted to 1..99.
    * @param textC The default (implied) text color.
    * @param fontStyle The default font style.
    * @param fontSzInMil If true, the font size attribute is converted to units of milli-inches.
    * @return The attributed string.
    */
   public static AttributedString fromStyledText(
         String attrText, String fam, int size, Color textC, FontStyle fontStyle, boolean fontSzInMil)
   {
      attrText = (attrText == null) ? "" : attrText.trim();
      int idx = attrText.lastIndexOf('|');
      String text = attrText;
      String[] attrRuns = new String[0];
      if((idx >= 0) && (idx < attrText.length()-1))
      {
         text = attrText.substring(0, idx);
         attrRuns = attrText.substring(idx+1).split(",");
      }
      text = text.trim();
      
      // note: you can't apply any text attributes to an empty character sequence.
      AttributedString as = new AttributedString(text);
      int len = text.length();
      if(len == 0)
         return(as);
      
      // fix style arguments if necessary
      if(!LocalFontEnvironment.isFontInstalled(fam)) fam = LocalFontEnvironment.getSansSerifFont();
      size = Utilities.rangeRestrict(1, 99, size);
      if(textC == null) textC = Color.black;
      if(fontStyle == null) fontStyle = FontStyle.PLAIN;
      
      // font family and font size apply to the entire string. Initially apply default text color, font style, underline
      // and superscript state to the entire string.
      as.addAttribute(TextAttribute.FAMILY, fam);
      as.addAttribute(TextAttribute.SIZE, fontSzInMil ? (float) (((double) size) * Measure.PT2IN * 1000.0) : size);
      as.addAttribute(TextAttribute.FOREGROUND, textC);
      as.addAttribute(TextAttribute.WEIGHT, 
            fontStyle.isBold() ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR);
      as.addAttribute(TextAttribute.POSTURE,
            fontStyle.isItalic() ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR);
      as.addAttribute(TextAttribute.UNDERLINE, UNDERLINE_OFF);
      as.addAttribute(TextAttribute.SUPERSCRIPT, NORMSCRIPT);

      // now apply the attribute code changes, if any, in sequence. Abort as soon as a parsing error occurs.
      boolean ok = true;
      int prevStartIdx = -1;
      for(String attrRun : attrRuns)
      {
         String[] parts = attrRun.split(":");
         if(parts.length != 2)  // number of ':' separators should be exactly 1
         {
            ok = false;
            break;
         }

         // get starting text position for attribute run; abort if not an integer, if position is beyond the end of
         // the character sequence, or if attribute runs are not in ascending order by starting position.
         int start;
         try
         {
            start = Integer.parseInt(parts[0]);
         } catch(NumberFormatException nfe)
         {
            break;
         }
         if((start >= text.length()) || (start <= prevStartIdx))
         {
            ok = false;
            break;
         }

         prevStartIdx = start;
         String run = parts[1];
         if(run.isEmpty())
            continue;   // no attribute codes specified - no change

         // text color, if specified, is ALWAYS first. Abort if invalid.
         Color c = null;
         if(UnicodeSubset.HEXDIGITS.contains(run.charAt(0)))
         {
            boolean hasAlpha = (run.length() >= 8) && UnicodeSubset.HEXDIGITS.containsAll(run.substring(0, 8));
            c = BkgFill.colorFromHexStringStrict(run.substring(0, hasAlpha ? 8 : 6), hasAlpha);
            if(c == null)
            {
               ok = false;
               break;
            } else if(start == 0 && c == textC)
               c = null;
         } else if((run.charAt(0) == '-') && (start > 0))
            c = textC;
         if(c != null) as.addAttribute(TextAttribute.FOREGROUND, c, start, len);

         FontStyle fs = null;
         if(run.contains("w")) fs = FontStyle.BOLD;
         else if(run.contains("i")) fs = FontStyle.ITALIC;
         else if(run.contains("I")) fs = FontStyle.BOLDITALIC;
         else if(run.contains("p")) fs = FontStyle.PLAIN;
         if(start == 0 && fs == fontStyle) fs = null;
         if(fs != null)
         {
            as.addAttribute(TextAttribute.WEIGHT,
                  fs.isBold() ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR, start, len);
            as.addAttribute(TextAttribute.POSTURE,
                  fs.isItalic() ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR, start, len);
         }

         Integer underline = null;
         if(run.contains("U"))
            underline = TextAttribute.UNDERLINE_ON;
         else if(run.contains("u") && (start > 0))
            underline = UNDERLINE_OFF;
         if(underline != null)
            as.addAttribute(TextAttribute.UNDERLINE, underline, start, len);

         Integer script = null;
         if(run.contains("S"))
            script = TextAttribute.SUPERSCRIPT_SUPER;
         else if(run.contains("s"))
            script = TextAttribute.SUPERSCRIPT_SUB;
         else if(run.contains("n") && (start > 0))
            script = NORMSCRIPT;
         if(script != null)
            as.addAttribute(TextAttribute.SUPERSCRIPT, script, start, len);

      }

      if(ok) return(as);
      
      // invalid attribute codes -- treat entire text argument as regular text, styled the same
      as = new AttributedString(attrText);
      as.addAttribute(TextAttribute.FAMILY, fam);
      as.addAttribute(TextAttribute.SIZE, fontSzInMil ? (float) (((double) size) * Measure.PT2IN * 1000.0) : size);
      as.addAttribute(TextAttribute.FOREGROUND, textC);
      as.addAttribute(TextAttribute.WEIGHT, 
            fontStyle.isBold() ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR);
      as.addAttribute(TextAttribute.POSTURE,
            fontStyle.isItalic() ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR);
      as.addAttribute(TextAttribute.UNDERLINE, UNDERLINE_OFF);
      as.addAttribute(TextAttribute.SUPERSCRIPT, NORMSCRIPT);
      return(as);
   }
   
   /** 
    * The node identifier, if applicable. It is intended ONLY for identifying a node uniquely within a figure model, 
    * and it has no effect on the rendered appearance of the node. May contain any character in the supported character
    * set, except tabs, carriage returns, and line feeds.
    */
   private String id = "";

   /**
    * Does this graphic node possess a node identifier, as specified by the <i>id</i> property?
    * 
    * @return True if node has an ID (even if that ID is an empty string).
    */
   public boolean hasID() { return((attrFlags & HASIDATTR) == HASIDATTR); }
   
   /**
    * Get the node ID for this graphic node.
    * 
    * @return The node ID string. If node lacks the ID attribute, the method returns an empty string always.
    */
   public String getID() { return(((attrFlags & HASIDATTR) == HASIDATTR) ? id : ""); }

   /**
   * Set the node ID for this graphic node. No action taken if the node lacks the <i>id</i> attribute. Otherwise, if
   * the ID has changed, the new value is saved -- replacing any unsupported characters in the ID string with a "?". 
   * Also, the node ID may be altered to ensure it uniquely identifies the node among all graphic objects in the 
   * containing figure model. Since the node ID never has an effect on the node's appearance, the model is merely 
   * informed of a change that does not require a render cycle. An undo operation is posted to the model's undo history,
   * however.
   * 
   * @param s The new ID. Null is treated as the equivalent of an empty string. Tab, carriage-return, and line-feed 
   * characters are silently stripped from the string; while they are part of the supported character set, they're not 
   * allowed in the ID string.
   * @return True if successful; false if this node does not possess the <i>id</i> attribute, or if ID is not an empty
   * string and had to be altered in some way (any tab, CR, or LF characters removed; any unsupported characters 
   * replaced by a question mark; modified to ensure uniqueness across graphic objects in model).
   */
   public boolean setID(String s)
   {
      if((attrFlags & HASIDATTR) != HASIDATTR) return(false);

      String str = (s==null) ? "" : s.trim();
      str = str.replaceAll("\r", "");
      str = str.replaceAll("\u0009", "");
      str = str.replaceAll("\n", "");
      if(!str.equals(id))
      {
         // NOTE: Since the node ID must be unique among all nodes in model, multi-object edit is not allowed here.
         
         String oldID = id;
         id = FGraphicModel.replaceUnsupportedCharacters(str);
         id = FGraphicModel.ensureUniqueGraphicObjectID(this, id);
         
         if(notify)
         {
            FGraphicModel model = getGraphicModel();
            if(model != null) model.onChange(this, 0, false, null);
            FGNRevEdit.post(this, FGNProperty.ID, id, oldID);
         }
      }
      return(str.equals(id));
   }

   
   
   //
   // Change support; notifying model
   //

   /** 
    * If flag is set, property changes and any other changes in the definition of the graphic node trigger a rendering 
    * update and are posted to the model's edit history. Flag is normally set, but may be unset to avoid unnecessary
    * notifications when multiple properties are being changed in rapid succession.
    */
   private boolean notify = true;
   
   /** 
    * Are model notifications currently enabled for this graphic node? Normally, notifications are always enabled -- so
    * that any changes in the node's definition trigger a rendering update via {@link #onNodeModified} and are posted
    * to the model's edit history. However, if a sequence of changes are being applied, notifications may be turned off
    * temporarily.
    * @return True if notifications are enabled, false otherwise.
    */
   final boolean areNotificationsEnabled() { return(notify); }
   
   /**
    * Enable/disable model notifications in response to any change in this graphic node. Normally, notifications are
    * always enabled, but they may be disabled temporarily if a sequence of changes are being applied to the node at
    * at once. <i>Be sure to wrap the code that performs the changes in a <code>try-finally</code> block to ensure that
    * notifications are always re-enabled.</i>
    * @param ena True to enable, false to disable model notifications.
    */
   final void setNotificationsEnabled(boolean ena) { notify = ena; }
   
   /**
    * This method is part of the framework supporting an undo-redo feature in the figure graphic model. Whenever a node 
    * property is modified via one of the public <i>setXXX()</i> methods, a reversible edit is posted to the "edit 
    * history" of the containing {@link FGraphicModel}. The reversible edit object reverses or reapplies the property 
    * change by invoking this method on the affected node. By design, the model's edit history should be blocked when
    * this method executes -- so that the undo/redo action will not be posted to that history.
    * 
    * <p>Undo/redo for a multi-node, multi-property edit action. When a figure or graph is rescaled, or when a style 
    * property is restored to its inherited state in all descendants of a given node, many properties may be changed 
    * across many nodes all at once. These changes are made WITHOUT triggering a rendering update after each single 
    * change. Likewise, when such a complex operation is undone or reapplied, a rendering update is triggered only after
    * all the individual property changes have been undone or reapplied. The <i>update</i> argument is cleared in this 
    * situation, and implementations should set the identified property without calling {@link #onNodeModified}.</p>
    * 
    * <p>The method relies on {@link #setPropertyValue} to actually update the property specified.</p>
    * 
    * <p>Do NOT invoke this method directly.</p>
    * 
    * @param p Identifies the property that is to be changed.
    * @param propValue The value to which the property should be set. This can be safely cast to the appropriate data
    * type for the property. For primitive types, cast to the Java class representing that type, then invoke the 
    * appropriate method to get the primitive value (eg, <code>Double.doubleValue()</code>).
    * @param update If this flag is set, then trigger a rendering update after setting the property value. Else do not.
    * The flag is cleared when the method is invoked during a multi-node, multi-property undo-redo action.
    */
   final boolean undoOrRedoPropertyChange(FGNProperty p, Object propValue, boolean update)
   {
      notify = update;
      boolean ok;
      try { ok = setPropertyValue(p, propValue); }
      finally { this.notify = true; }
      return(ok);
   }

   /**
    * Set a specified node property to the specified value.
    * 
    * <p>This method is part of the undo-redo framework. It is also used to change the same property across multiple
    * like objects. As such, the method assumes that the property value can be safely cast to the data type associated 
    * with the specified property. The default implementation handles any properties encapsulated by <b>FGraphicNode</b>
    * itself. Each subclass must override the method to handle any additional properties defined by the node class. A 
    * typical subclass implementation will switch on the property ID. Always call this base class implementation from 
    * the default case for that <code>switch</code> statement.</p>
    * 
    * @param p Identifies the property that was changed.
    * @param propValue The value to which the property should be set. This can be safely cast to the appropriate data
    * type for the property. For primitive types, cast to the Java class representing that type, then invoke the 
    * appropriate method to get the primitive value (eg, <code>Double.doubleValue()</code>).
    * @return True if property value change was successful, false otherwise.
    */
   boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
         case FONTFAMILY : ok = setFontFamily((String)propValue); break;
         case FONTSTYLE : ok = setFontStyle((FontStyle)propValue); break;
         case FONTSIZE: ok = setFontSizeInPoints((Integer)propValue); break;
         case ALTFONT: ok = setAltFont((GenericFont)propValue); break;
         case PSFONT: ok = setPSFont((PSFont)propValue); break;
         case FILLC: ok = setFillColor((Color)propValue); break;
         case STROKEC: ok = setStrokeColor((Color)propValue); break;
         case STROKEW: ok = setMeasuredStrokeWidth((Measure)propValue); break;
         case STROKECAP: ok = setStrokeEndcap((StrokeCap)propValue); break;
         case STROKEJOIN: ok = setStrokeJoin((StrokeJoin)propValue); break;
         case STROKEPATN: ok = setStrokePattern((StrokePattern)propValue); break;
         case X: ok = setX((Measure)propValue); break;
         case Y: ok = setY((Measure)propValue); break;
         case WIDTH: ok = setWidth((Measure)propValue); break;
         case HEIGHT: ok = setHeight((Measure)propValue); break;
         case ROTATE: ok = setRotate((Double) propValue); break;
         case TITLE: ok = setTitle((String)propValue); break;
         case ID: ok = setID((String)propValue); break;
         default: break;
      }
      return(ok);
   }
   
   /**
    * Get the current value for the specified node property.
    * 
    * <p>This method is used to support changing the same property across multiple like objects to the same value --
    * without having to call the property-specific accessor method. The default implementation handles any properties 
    * encapsulated by <b>FGraphicNode</b>  itself. Each subclass must override the method to handle any additional 
    * properties defined by the node class. A typical subclass implementation will switch on the property ID. Always 
    * call this base class implementation from the default case for that <code>switch</code> statement.</p>
    * 
    * @param p The property ID.
    * @return The value of that property. For properties that may be inherited, the method returns the inherited value.
    * If the node does not possess this property, null is returned.
    */
   Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
      case FONTFAMILY : value = getFontFamily(); break;
      case FONTSTYLE : value = getFontStyle(); break;
      case FONTSIZE: value = getFontSizeInPoints(); break;
      case ALTFONT: value = getAltFont(); break;
      case PSFONT: value = getPSFont(); break;
      case FILLC: value = getFillColor(); break;
      case STROKEC: value = getStrokeColor(); break;
      case STROKEW: value = getMeasuredStrokeWidth(); break;
      case STROKECAP: value = getStrokeEndcap(); break;
      case STROKEJOIN: value = getStrokeJoin(); break;
      case STROKEPATN: value = getStrokePattern(); break;
      case X: value = getX(); break;
      case Y: value = getY(); break;
      case WIDTH: value = getWidth(); break;
      case HEIGHT: value = getHeight(); break;
      case ROTATE: value = getRotate(); break;
      case TITLE: value = getTitle(); break;
      case ID: value = getID(); break;
      default: break;
      }
      return(value);
   }
   
   /**
    * A hook into the graphic model's support for editing a property value across multiple nodes of the same type.
    * The model maintains the notion of a current selection, which could include more than one object. When that is the
    * case, AND the selection list is homogeneous, the model will attempt to change the specified property to the
    * specified value on all nodes like this one in the current selection.
    * 
    * <p>Subclasses must invoke this method from their various set-property accessors in order to support the 
    * multi-object property edit feature.</p>
    * 
    * @param p The affected property.
    * @param value The property's new value.
    * @return True if the multi-object property edit was performed, in which case it should be unnecessary to update
    * the same property on this node (since it will have already been changed!).
    */
   final boolean doMultiNodeEdit(FGNProperty p, Object value)
   {
      FGraphicModel fgm = getGraphicModel();
      return(fgm != null && fgm.doMultiNodeEdit(this, p, value));
   }
   
   /**
    * This method is invoked whenever a <code>FGraphicNode</code> is inserted as a subordinate of this graphic node. 
    * It is called for both child and component node insertions.
    * 
    * <p>In general, insertion of a subordinate will invalidate the rendered bounds of this node and its ancestors. The 
    * rendered bounds of the newly inserted node must be recomputed from scratch, taking into account the new context 
    * in which it is rendered. This fresh render bounds calculation will ensure that the new node's rendering 
    * infrastructure is in place. Finally, the <em>DataNav</em> graphic model must be notified that a change has 
    * occurred in this node -- including any "dirty regions" that must be re-rendered -- by calling the method 
    * <code>FGraphicModel.onChange()</code>.</p>
    * 
    * <p>This method handles all of these tasks. Any override should be sure to invoke this super-class implementation, 
    * or handle these tasks directly!</p>
    * 
    * @param sub The graphic node that was just inserted as a subordinate this <code>FGraphicNode</code>.
    * @see FGraphicModel#onChange(FGraphicNode, int, boolean, List)
    */
   protected void onSubordinateNodeInserted(FGraphicNode sub)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null || sub == null || !subordinates.contains(sub)) return;

      Graphics2D g2d = model.getViewerGraphics();
      try
      {
         // freshly calculate child's render bounds; this will init rendering infrastructure appropriately
         Rectangle2D r = sub.getRenderBounds(g2d, true, null);

         // update render bounds of child's ancestors, to make sure that newly inserted node's bounds are included
         FGraphicNode.updateAncestorRenderBounds(g2d, sub);

         // inform model, requesting a render update if necessary
         if(!r.isEmpty()) 
         {
            List<Rectangle2D> dirtyAreas = new ArrayList<>();
            dirtyAreas.add( sub.cachedGlobalShape.getBounds2D() );
            model.onChange(this, 1, true, dirtyAreas);
         }
         else
            model.onChange(this, 1, false, null);
      }
      finally { if(g2d != null) g2d.dispose(); }
   }

   /**
    * This method is invoked whenever a <code>FGraphicNode</code> is removed as a subordinate of this graphic node.
    * It is called for both child and component node removals.
    *
    * <p>In general, removal of a subordinate will invalidate the rendered bounds of this node and its ancestors, and 
    * the graphic model must be re-rendered within the rendered bounds of the deleted node. Even if a re-rendering is 
    * not necessary, the containing model must be informed of the change.</p>
    * 
    * <p>This method handles all of these tasks. Any override should be sure to invoke this super-class implementation, 
    * or handle these tasks directly!</p>
    * 
    * @param sub The graphic node that was just removed from this <code>FGraphicNode</code>. If <code>null</code>, 
    * assume that more than subordinate was removed. In this case, the method takes the safe approach of re-rendering 
    * the graphic model within the old and new rendered bounds of this node itself.
    * @see FGraphicModel#onChange(FGraphicNode, int, boolean, List)
    */
   protected void onSubordinateNodeRemoved(FGraphicNode sub)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      Graphics2D g2d = model.getViewerGraphics();
      try
      {
         // save the old render bounds for THIS node, if available, then calculate the new bounds and update all 
         // ancestor nodes
         Rectangle2D rThisOld = cachedGlobalShape.getBounds2D();
         getRenderBounds(g2d, true, null);
         Rectangle2D rThisNew = cachedGlobalShape.getBounds2D();
         FGraphicNode.updateAncestorRenderBounds(g2d, this);

         // we should only have to re-render within the old render bounds of the removed node. If these are not 
         // available, we will have to render within the old and new bounds of this node. If these aren't both 
         // available, then the dirty region list will be empty, forcing a complete re-rendering of graphic model.
         List<Rectangle2D> dirtyAreas = new ArrayList<>();
         if(sub != null && sub.cachedLocalBounds != null && sub.cachedGlobalShape != null)
            dirtyAreas.add(sub.cachedGlobalShape.getBounds2D());
         else
         {
            if(!rThisOld.isEmpty()) dirtyAreas.add(rThisOld);
            if(!rThisNew.isEmpty()) dirtyAreas.add(rThisNew);
         }
         model.onChange(this, 1, true, dirtyAreas);
      }
      finally { if(g2d != null) g2d.dispose(); }
   }

   /**
    * This method should be invoked whenever this <code>FGraphicNode</code>'s intrinsic definition is modified -- 
    * typically when the user edits a node's definition via an external editor.
    * 
    * <p>In general, modifying a node's definition will change the rendered appearance of the node, necessitating a 
    * re-rendering of a portion of the graphic model containing the node. The rendered bounds of the node may also 
    * change, thereby affecting the rendered bounds of its ancestors.</p>
    * 
    * <p>This base-class implementation assumes that the method is invoked before the element's cached render bounds 
    * have been updated. It performs the following tasks: 
    * <ul>
    *    <li>Saves a copy of the node's "stale" render bounds.</li>
    *    <li>Recomputes the node's render bounds to account for the changes in its definition.</li>
    *    <li>Update the render bounds of the node's ancestors to accommodate the change in this node's bounds.</li>
    *    <li>Re-render the graphic model in the regions covered by the node's "stale" and "current" bounds. If the 
    *    "stale" bounds are not available, the entire model must be re-rendered.</li>
    * </ul>
    * </p>
    * 
    * <p>This approach will generally suffice, but it can be overkill -- especially if whatever has changed in the 
    * node's definition has no effect on its rendered appearance. In other cases, a property change may have side 
    * effects on the graphic rendering that might not be captured by the above algorithm. Implementing classes may wish 
    * to override this method, including an implementation-dependent "hint" object to help decide how to respond to 
    * specific changes in a node's definition. <em>Any such override should always force a recalculation of the node's 
    * render bounds, and re-render the graphic model in all affected regions! Even if a re-rendering is unnecessary, 
    * it must call <code>FGraphicModel.onChange()</code> to inform the containing model that some change has occurred.
    * Also, if the node's rendered bounds change in any way, invoke <code>updateAncestorBounds()</code> to update the 
    * render bounds of that node's ancestors</em>.</p>
    * 
    * <p><em>NOTE</em>. When any of the style or positioning attributes supported by <code>FGraphicNode</code> are 
    * modified by the <code>FGraphicNode</code> implementation, that implementation will invoke this method with an 
    * instance of <code>FGNProperty</code> as the hint, identifying the particular attribute that has changed.</p>. 
    * 
    * @param hint An object which may be used by a subclass of <code>FGraphicNode</code> to identify what aspect of the 
    * node's definition has changed. This hint might be used to determine how much needs to be done to properly update 
    * the node's rendered appearance, etc. May be <code>null</code>.
    * @see FGraphicModel#onChange(FGraphicNode, int, boolean, List)
    */
   protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      List<Rectangle2D> dirtyAreas = new ArrayList<>();

      // first we add the rectangular bounds of our cached global render shape. This represents the old rendering of 
      // the element. We CANNOT use the cached render bounds and then transform them to global coords, because the 
      // local-to-global transform may have changed!!!
      Rectangle2D r = cachedGlobalShape.getBounds2D();
      if(!r.isEmpty()) dirtyAreas.add(r);

      // now force recalculation of bounds, transform to global coords, then add bounds of resulting shape to the 
      // dirty list. This represents the new rendering of the element, after the change. Also update the render bounds 
      // of ancestor nodes.
      Graphics2D g2d = model.getViewerGraphics();
      try
      {
         getRenderBounds(g2d, true, null);
         r = cachedGlobalShape.getBounds2D();
         if(!r.isEmpty()) dirtyAreas.add(r);

         FGraphicNode.updateAncestorRenderBounds(g2d, this);
      }
      finally { if(g2d != null) g2d.dispose(); }

      // now update the model's rendering in the dirty regions
      model.onChange(this, 0, true, dirtyAreas);
   }


   //
   // Focusable/Renderable support; rendering infrastructure
   //

   /**
    * Release any internal resources used by this <code>FGraphicNode</code> and its subordinates to render the graphic 
    * content defined by them in Java2D.
    * 
    * <p>When a <code>FGraphicModel</code> is no longer rendered in the <em>DataNav</em> GUI, this method may be invoked 
    * on the model's root figure node to destroy all resources that were created and maintained by graphic nodes in the
    * model to render themselves in the model's <code>RenderableModelViewer</code>. The method is also invoked when 
    * a node is excised from a currently displayed graphic model.</p>
    * 
    * <p>This method reinitializes the cached local and global bounds maintained by <code>FGraphicNode</code> itself, 
    * invokes <code>renderRenderResourcesForSelf()</code> to give the subclass implementation an opportunity to release 
    * any rendering resources it maintains, then invokes <code>releaseRenderResources()</code> on every node subordinate
    * to this one.</p>
    */
   protected final void releaseRenderResources()
   {
      cachedGlobalShape = new Rectangle2D.Double();
      cachedLocalBounds = null;
      releaseRenderResourcesForSelf();
      for(FGraphicNode sub : subordinates)
         sub.releaseRenderResources();
   }

   /**
    * Release any internal resources created and maintained to render this node itself (not any subordinates).
    * 
    * @see FGraphicNode#releaseRenderResources()
    */
   protected abstract void releaseRenderResourcesForSelf();

   /**
    * Get the affine transformation which maps this element's local rendering coordinate system onto the rendering 
    * coordinate system of its parent container. By convention in <em>DataNav</em>, units in both coordinate systems
    * are in milli-inches. As a rule, the transformation may involve a rotation and a translation, but no scaling -- 
    * to keep the coordinate units unchanged. Elements lacking a parent container -- like the root node -- should 
    * return the identity transform.
    * 
    * <p>Again by convention in <em>DataNav</em>, the rendering coordinate system has its origin is at the bottom-left 
    * of the container, the x-axis increases to the right, and the y-axis increases upward</p>
    * 
    * @return Transformation that maps points in this element's own rendering coordinate system onto the rendering
    * coordinate system of its parent container.
    */
   public abstract AffineTransform getLocalToParentTransform();

   /**
    * Get the affine transformation which maps this <code>FGraphicNode</code>'s own rendering coordinate system onto 
    * the global coordinate system associated with the <em>DataNav</em> graphics model's root node (which is 
    * always a figure element). By convention, all <code>Renderable</code> nodes in the <em>DataNav</em> element 
    * universe define a right-handed rendering coordinate system with units of milli-inches, origin at the bottom-left, 
    * x-axis increasing rightward, and y-axis increasing upward. 
    * 
    * <p>This method concatenates all local-to-parent transforms -- see <code>getLocalToParentTransform()</code> -- in 
    * the graphic node's ancestry up to, but not including, the containing root node.</p>
    * 
    * @return The affine transform from this node's local rendering coordinates in milli-inches to the 
    * coordinate system of the root node, also in milli-inches. Returns the identity transform if this node has no 
    * parent (meaning, it's the root node).
    */
   public final AffineTransform getLocalToGlobalTransform()
   {
      // preconcatenate local-to-parent transforms from this node up to, but not including, the containing root node
      AffineTransform at = new AffineTransform();
      FGraphicNode n = this;
      while(n != null && n.parent != null)
      {
         at.preConcatenate( n.getLocalToParentTransform() );
         n = n.parent;
      }

      return( at );
   }

   /**
    * Is this node rotated with respect to the global coordinate system associated with the root figure? This will be
    * there's a non-zero shear component in the affine transform returned by {@link #getLocalToGlobalTransform()}.
    * @return True if rotated; false otherwise.
    */
   public final boolean isRotatedOnFigure()
   {
      AffineTransform at = getLocalToGlobalTransform();
      return(at.getShearX() != 0 || at.getShearY() != 0);
   }
   
   /**
    * Retrieve the local rendering viewport defined by this <code>FGraphicNode</code>. During rendering, a subordinate 
    * node will invoke this method to obtain a <code>FViewport2D</code> by which it can properly position and size 
    * itself within its parent container.
    * 
    * <p><em>Background</em>. Most of the "building blocks" of the <em>DataNav</em> graphics tree model -- graph, line, 
    * shape, etc. -- are positioned and sized with respect to a <em>viewport</em> defined by the parent container. This 
    * viewport, encapsulated by the <code>FViewport2D</code> class, is a fundamental component of <em>DataNav</em>'s 
    * rendering model. In this model, a node's rendering coordinate system is always a right-handed coordinate system 
    * (x-axis increases rightward, y-axis upward) with units in milli-inches. The root figure node establishes the 
    * initial "global" rendering coordinate system, or viewport, and each descendant node that can parent other nodes 
    * will establish its own "local" rendering coordinate system.</p>
    * 
    * <p>Obviously, it is inconvenient and unwieldy to specify the locations and sizes of all graphics objects in 
    * milli-inches WRT the figure's "global" coordinate sytem. More natural units like inches are preferred. It is also 
    * important to be able to specify an object's location with respect to its container -- so that if you move the 
    * parent container, the subordinate node moves with it. Thus, from the user's point of view, a node's location and 
    * size is always defined relative to the immediate parent container. Furthermore, it is often very useful to specify
    * locations and sizes as a percentage of the parent container's dimensions -- so a node can be automatically resized
    * if the parent is resized. In addition, a graph defines a data coordinate system the units of which will vary 
    * widely with the application; sometimes we need to locate a node (a label for a data point is the classic example) 
    * in those native coordinates.</p>
    * 
    * <p>For these reasons, attributes that define the location coordinates and dimensions of <em>DataNav</em> graphics 
    * nodes are specified as <code>Measure</code>s -- a floating-point number with associated units of measure: in, cm, 
    * mm, pt, %, and "user units", where the last refers to the units associated with a graph's data coordinate system. 
    * The viewport object is responsible for converting such <code>Measure</code>'d coordinates and lengths to local 
    * rendering coordinates in milli-inches. Local rendering coordinates can then be transformed to the "global" 
    * rendering coordinate system of the <em>DataNav</em> figure by applying the affine transformation returned by 
    * <code>getLocalToGlobalTransform()</code>.</p>
    *
    * @return  The node's viewport, or <code>null</code> if unable to construct one.
    */
   public abstract FViewport2D getViewport();
   
   /**
    * Retrieve the viewport of this <code>FGraphicNode</code>'s parent container. This is merely a convenience method 
    * for invoking <code>getViewport(FGraphicNode)</code> on this node's parent. If it has no parent, <code>null</code> 
    * is returned.
    * 
    * @return The parent viewport in which this node is rendered, if one exists; <code>null</code> otherwise.
    */
   public final FViewport2D getParentViewport()
   {
      return( (parent != null ) ? parent.getViewport() : null );
   }

   /**
    * Cached shape bounding all marks made when this <code>FGraphicNode</code> and its subordinates were last rendered, 
    * specified WRT the "global" rendering coordinate system, or "viewport", of the root graphics node. Whenever the 
    * local render bounds are recalculated in <code>getRenderBounds()</code>, this shape is computed by transforming 
    * those rectangular bounds into the root node's viewport. Must never be <code>null</code>.
    */
   private Shape cachedGlobalShape = new Rectangle2D.Double();

   /**
    * Cached rectangle bounding all marks made when painting this <code>FGraphicNode</code> and all of its subordinates, 
    * specified WRT this node's local rendering coordinate system.
    */
   protected Rectangle2D cachedLocalBounds;

   /**
    * Retrieve the rectangle bounding all marks made by this <code>FGraphicNode</code>, specified WRT its own local 
    * rendering coordinate system.
    * 
    * <p>This method is a critical component in the rendering model of <em>DataNav</em>, supporting a number of
    * important functions:
    * <ul>
    *    <li><em>Focus highlighting</em>. When a <code>Focusable</code> graphics node has the editing focus, it is 
    *    highlighted within the <code>RenderableModelViewer</code> in which a <em>DataNav</em> figure is rendered. The 
    *    default highlight shape is the local render bounds, expanded 25mi on all sides and transformed into the global 
    *    rendering coordinate system (at figure level). See <code>getFocusShape(Graphics2D)</code>.</li>
    *    <li><em>Hit testing</em>. The user can change the graphics node with the editing focus by clicking on the 
    *    desired graphics within the <code>RenderableModelViewer</code>. To support this feature, an element must be 
    *    able to determine whether or not it has been "clicked". The default hit-testing algorithm checks whether or not 
    *    a cached shape, representing the local render bounds transformed to the global rendering coordinate system, 
    *    contains the "clicked" point. See <code>FGraphicNode.hitTest(Point2D, Graphics2D)</code>.</li>
    *    <li><em>Optimizing rendering performance via "dirty regions"</em>. Complex figures displaying lots of data can 
    *    take a considerable amount of time to render in full. If the author makes a simple change such as altering the 
    *    font style of a text label, the application should <em>not</em> have to re-render the entire figure to reflect 
    *    the change. To improve performance in such situations, the <em>DataNav</em> rendering model supports the 
    *    concept of dirty regions. Whenever a graphics node property is changed, or the node itself is inserted or 
    *    deleted, the node or its parent calculates the dirty areas affected by the change. Obviously, a graphics node's 
    *    render bounds are a sensible choice as a dirty area. Then, during rendering, an element can compare its render 
    *    bounds against the list of dirty regions that need updating, to see if it can skip rendering entirely -- 
    *    thus improving overall render speed! See <code>needsRendering(RenderTask)</code>.</li>
    * </ul>
    * </p>
    * 
    * <p>It can be computationally expensive to recalculate the render bounds. Therefore, for optimal performance, 
    * <code>FGraphicNode</code> maintains a <em>cached</em> version of the local render bounds. This method will return 
    * the cached version unless a fresh recalculation is mandated by the <code>forceRecalc</code> argument. The bounds 
    * should be recalculated whenever the element is mutated in some way (attribute changed, element inserted into or 
    * removed from the graphics tree model). Whenever the bounds are recalculated, any change will be propagated down 
    * through the node's descendants and back up through its ancestors.</p>
    * 
    * <p>This method relies upon the implementations of two abstract methods:
    * <ul>
    *    <li><code>isRendered()</code> indicates whether or not the <code>FGraphicNode</code> and its descendants are 
    *    currently rendered at all. This is needed to account for nodes that can be hidden by setting an attribute or 
    *    attribute(s) appropriately. If a node is not currently rendered, its render bounds are empty.</li>
    *    <li><code>getRenderBoundsForSelf(Graphics2D g2d, boolean)</code> returns the rectangle bounding all marks 
    *    made by this <code>FGraphicNode</code> itself. This rectangle is combined with the individual render bounds 
    *    of all subordinate nodes to get the overall render bounds of this <code>FGraphicNode</code>. Of course, each 
    *    subordinate's bounds are first transformed into this node's own viewport. For a complex element with lots of 
    *    descendants, all of the transformations and rectangle allocations can take a while -- which is why we cache 
    *    the render bounds and recalculate it only when necessary!</li> 
    * </ul>
    * </p>
    * 
    * @param g2d A <code>Graphics2D</code> object encapsulating the graphics context into which this graphics node  
    * would be painted. It should not be changed nor rendered into -- it is intended only to serve as a source for the 
    * font rendering context needed by some nodes to accurately measure text bounds.
    * @param forceRecalc If <code>true</code>, do not use the cached version of the render bounds; calculate it afresh.
    * In this case, a graphics context is usually provided. If <code>false</code>, the method returns the cached 
    * version if it's available.
    * @param bounds If not <code>null</code>, this argument will be initialized with the render bounds, and a reference 
    * to it is returned. Otherwise, a new <code>Rectangle2D</code> object is allocated on the heap.
    * @return If successful, return a rectangle tightly bounding the marks made by this <code>FGraphicNode</code> and 
    * its descendants, specified in the local rendering coordinate system of this node: logical units in milli-in, 
    * origin at BL corner, right-handed coord system. Return an empty rectangle if unable to calculate bounds or if no 
    * marks are made by this graphics node.
    */
   final Rectangle2D getRenderBounds(Graphics2D g2d, boolean forceRecalc, Rectangle2D bounds)
   {
      if(forceRecalc || cachedLocalBounds == null)
      {
         if(cachedLocalBounds == null)
            cachedLocalBounds = new Rectangle2D.Double();
         if(!isRendered())
            cachedLocalBounds.setRect(0, 0, 0, 0);
         else
         {
            cachedLocalBounds.setRect(getRenderBoundsForSelf(g2d, true));
            Utilities.rectUnion(cachedLocalBounds, getSubordinateRenderBounds(this, g2d, true), cachedLocalBounds);
         }

         // update cached shape representing local render bounds transformed into root node's global coordinates
         cachedGlobalShape = getLocalToGlobalTransform().createTransformedShape(cachedLocalBounds);
      }

      Rectangle2D rect = bounds;
      if(rect == null) rect = new Rectangle2D.Double();
      rect.setRect(cachedLocalBounds);
      return(rect);
   }

   /**
    * Return the most recently cached <code>Shape</code> bounding all marks made in rendering this graphic node and its 
    * subordinates. The shape is defined in the global rendering coordinate system of the graphic model's root node.
    * 
    * <p>If the node's definition has just changed and the rendering infrastructure has not yet been updated, then the 
    * returned shape reflects the node's state prior to the change. This is useful when computing the "dirty regions" 
    * affected by a change in the node's definition.</p>
    * 
    * @return The cached shape bounding all marks made by this graphic node and its descendants, specified WRT the 
    * global rendering coordinate system of the root graphic node.
    */
   final Shape getCachedGlobalShape() { return(cachedGlobalShape); }

   /**
    * Get cached rectangle bounding all marks made in rendering this graphic node and its subordinates. The rectangle
    * is defined in the global rendering coordinate system of the graphic model's root node.
    * <p>If the node's definition has just changed and the rendering infrastructure has not yet been updated, then the 
    * returned bounds reflect the node's state prior to the change. If the node has not been rendered yet, the bounds
    * will be empty.</p>
    * 
    * @return The cached rectangle bounding all marks made by this graphic node and its descendants, specified WRT the 
    * global rendering coordinate system of the root graphic node.
    */
   public Rectangle2D getCachedGlobalBounds() { return(cachedGlobalShape.getBounds2D()); }

   /**
    * Is this <code>FGraphicNode</code> currently rendered?
    * 
    * <p>This base-class implementation returns <code>true</code> always. If an implementing class represents a 
    * graphics node that may be optionally hidden, be sure to override this method.</p>
    * 
    * @return <code>True</code> if this graphics node may make marks when rendered, given its current state. Return  
    * <code>false</code> if the node is deliberately hidden.
    * @see FGraphicNode#getRenderBounds(Graphics2D, boolean, Rectangle2D)
    */
   protected boolean isRendered() { return(true); }

   /**
    * Does this graphic node or any of its descendants make any translucent marks when rendered? <i>This base class
    * implementation returns true if the node is stroked with a translucent stroke color, or if its fill color is
    * currently translucent. If not, it then checks whether any of its component or child nodes are translucent. If
    * the node is not currently rendered ({@link #isRendered()}, then this method returns false. Subclasses should 
    * override this method as needed.</i>
    * 
    * @return True if this node or any descendant is translucent.
    */
   protected boolean isTranslucent() 
   {
      if(!isRendered()) return(false);
      
      if(((attrFlags & HASSTROKEATTRS) != 0) && isStroked() && getStrokeColor().getAlpha() < 255) return(true);
      if((attrFlags & HASFILLCATTR) !=0) 
      {
         int alpha = getFillColor().getAlpha();
         if(alpha > 0 && alpha < 255) return(true);
      }
      for(FGraphicNode subordinate : subordinates) if(subordinate.isTranslucent()) return (true);
      
      return(false);
   }
   
   /**
    * Retrieve the rectangle bounding the current rendering of this <code>FGraphicNode</code> <em>ONLY</em>, in the 
    * node's own local rendering coordinate system.
    * 
    * @param g2d A <code>Graphics2D</code> object encapsulating the graphics context in which the node would be 
    * rendered. It should not be changed nor rendered into -- it is intended only to serve as a source for the font 
    * rendering context needed by some elements to accurately measure text bounds.
    * @param forceRecalc If <code>false</code>, return the cached version of the node's own render bounds, if 
    * available. If <code>true</code>, do not return the cached version of the render bounds; calculate it afresh. In 
    * this case, a graphics context is usually provided.
    * @return Return a rectangle tightly bounding the rendering of the node, <em>EXCLUDING subordinate nodes</em>, in 
    * the node's own local rendering coordinates: logical units in milli-in, origin at BL corner, right-handed 
    * coordinate system. Return an empty rectangle if the node itself makes no marks.
    */
   protected abstract Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc);

   /**
    * Form the rectangle bounding all marks made by subordinates of the specified <code>FGraphicNode</code>, WRT the 
    * node's local rendering coordinate system.
    * 
    * <p>This helper method traverses all subordinate graphic nodes of the specified node, transforming each the render 
    * bounds of each subordinate into the parent node's own rendering coordinates and combining it with the render 
    * bounds of the other subordinates. The resultant rectangle should bound any marks made by any of the node's 
    * subordinates, but not the node itself.</p>
    * 
    * @param parent The graphic node. If <code>null</code>, the method returns an empty rectangle.
    * @param g2d A <code>Graphics2D</code> object encapsulating the graphics context in which the node and its 
    * subordinates would be rendered. It should not be changed nor rendered into -- it is intended only to serve as a 
    * source for the font rendering context needed by some nodes to accurately measure text bounds.
    * @param forceRecalc If <code>true</code>, the method forces recalculation of each subordinate's render bounds. In 
    * this case, a graphics context should be provided. If <code>false</code>, the method requests the cached version 
    * of each subordinate's bounds.
    * @return The union of all non-empty render bounds across all subordinates of the specified graphic node, WRT the 
    * local rendering coordinate system of that parent node.
    */
   static Rectangle2D getSubordinateRenderBounds(FGraphicNode parent, Graphics2D g2d, boolean forceRecalc)
   {
      if(parent == null) return( new Rectangle2D.Double() );

      Rectangle2D rUnion = new Rectangle2D.Double();
      Rectangle2D rSub = new Rectangle2D.Double();
      for(FGraphicNode sub : parent.subordinates)
      {
         sub.getRenderBounds(g2d, forceRecalc, rSub);
         if(!rSub.isEmpty())
         {
            rSub.setRect( sub.getLocalToParentTransform().createTransformedShape(rSub).getBounds2D() );
            Utilities.rectUnion(rUnion, rSub, rUnion);
         }
      }
      return(rUnion);
   }

   /**
    * Update the cached render bounds for all ancestors of the specified <code>FGraphicNode</code>, up to the root 
    * node in the containing <em>DataNav</em> graphics tree. This method should be called whenever a node is modified in
    * some way that could alter its rendered bounds. Such a change will obviously invalidate the render bounds of all 
    * of the node's ancestors.
    * 
    * @param g2d A <code>Graphics2D</code> object encapsulating the graphics context in which graphic nodes would be 
    * rendered. It should not be changed nor rendered into -- it is intended only to serve as a source for the font 
    * rendering context needed by some nodes to accurately measure text bounds.
    * @param from The <code>FGraphicNode</code> that was changed, necessitating an update in the render bounds of each 
    * of its direct ancestors.
    */
   static void updateAncestorRenderBounds(Graphics2D g2d, FGraphicNode from)
   {
      if(from == null) return;
      FGraphicNode n = from.parent;
      while(n != null)
      {
         if(n.cachedLocalBounds == null)
            n.getRenderBounds(g2d, true, null);
         else
         {
            Rectangle2D rOld = n.cachedLocalBounds;
            if(!n.isRendered())
               n.cachedLocalBounds = new Rectangle2D.Double();
            else
            {
               n.cachedLocalBounds = n.getRenderBoundsForSelf(g2d, false);
               Utilities.rectUnion(n.cachedLocalBounds, getSubordinateRenderBounds(n, g2d, false), n.cachedLocalBounds);
            }
            n.cachedGlobalShape = n.getLocalToGlobalTransform().createTransformedShape(n.cachedLocalBounds);

            // no change in bounds -- so there's no need to propagate further!
            if(rOld.equals(n.cachedLocalBounds)) 
               return;
         }
         n = n.parent;
      }
   }

   /**
    * A helper method for subclass implementations of {@link #render(Graphics2D,RenderTask)}, this method checks to see 
    * if the current cached global render shape intersects any of the "dirty rectangles" in which the graphic model must
    * be re-rendered. If not, there should be no need to render this element -- which will shorten the total time it 
    * takes to complete the rendering task.
    *  
    * @param task The current rendering task object, which includes a list of rectangles that must be re-rendered. All 
    * such rectangle are defined WRT the "global" coordinate system of the root graphics node.
    * @return True if the shape bounding this node's rendering in "global" coordinates intersects any of the rectangles 
    * in the task's dirty regions list. If that list is null or empty, true is returned.
   */
   protected final boolean needsRendering(RenderTask task)
   {
      if(task == null) return(true);
      List<Rectangle2D> dirtyAreas = task.getDirtyRegions();
      if(dirtyAreas == null || dirtyAreas.isEmpty() || cachedGlobalShape == null) return(true);

      for(Rectangle2D r : dirtyAreas) 
         if(r != null && (!r.isEmpty()) && cachedGlobalShape.intersects(r))
            return(true);

      return(false);
   }

   /**
    * A helper method for subclass implementations of <code>Renderable#render(Graphics2D,RenderTask)</code>, this method 
    * invokes <code>render()</code> on every subordinate node (all private component nodes, followed by all public 
    * children).
    * 
    * @see com.srscicomp.common.g2dviewer.Renderable#render(Graphics2D, RenderTask)
    * @return <code>True</code> if all subordinates were rendered; <code>false</code> if an error occurred or the 
    * rendering job was cancelled.
   */
   protected final boolean renderSubordinates(Graphics2D g2d, RenderTask task)
   {
      for(FGraphicNode sub : subordinates)
      {
         if(!sub.render(g2d, task))
            return(false);
      }
      return(true);
   }

   /**
    * The default focus highlight for a <code>FGraphicNode</code> is its local render bounds transformed to the root 
    * figure's "global" rendering coordinates. If the local render bounds cannot be computed, <code>null</code> is 
    * returned. The local bounds are expanded by 25 milli-in on all sides prior to transforming to global rendering 
    * coordinates -- so the focus highlight is somewhat larger than what is rendered.
    * 
    * @see FGraphicNode#getRenderBounds(Graphics2D, boolean, Rectangle2D)
    * @see Focusable#getFocusShape(Graphics2D)
    */
   public Shape getFocusShape(Graphics2D g2d)
   {
      // get the element's local render bounds (preferably the cached version). If unavailable or empty, then there is 
      // no focus highlight
      Rectangle2D rBounds = getRenderBounds(g2d, false, null);
      if(rBounds.isEmpty()) return(null);

      // expand by 25 milli-in on all sides, then transform to global painting coords
      rBounds.setRect(rBounds.getX()-25, rBounds.getY()-25, rBounds.getWidth()+50, rBounds.getHeight()+50);
      return(getLocalToGlobalTransform().createTransformedShape(rBounds));
   }

   /**
    * Can this graphic node be repositioned interactively?
    * 
    * <p>The default implementation returns true if the node posseses the location attributes <i>x</i> and <i>y</i>, and
    * the node is moved by updating the values of those attributes. Any subclass representing a relocatable node that 
    * can be interactively moved using properties other than these must override this and other relevant methods. Any
    * subclass that has the node properties <i>x</i> and <i>y</i> but should NOT be interactively moved must override
    * this method and return false.</p>
    * 
    * @return True if the node can be moved interactively via an appropriate gesture on the user interface.
    * @see Focusable#canMove()
    */
   public boolean canMove() { return((attrFlags & HASLOCATTRS) == HASLOCATTRS); }

   /**
    * Provide a cue during interactive repositioning of a "movable" node.
    * 
    * <p>This method was introduced to support interactive repositioning of the "focus node" in a GUI -- typically via
    * a drag gesture. As the user "drags" the node with the mouse, this method can be called to get a short string that 
    * indicates what node attributes would change should the user "drop" the node at the new position, and what their 
    * new values would be.</p>
    * 
    * <p>The default implementation is similar to that of {@link #executeMove}, except that a cue string of the form
    * "x=..., y=..." is prepared instead of actually adjusting their current values.</p>
    * 
    * <p>Any subclass that implements a different means of "moving" the node should override this method.</p>
    * 
    * @param dx Net change in horizontal position of node, in the root figure's rendering coordinates.
    * @param dy Net change in vertical position of node, in the root figure's rendering coordinates.
    * @return The string cue. Returns null if this node is not movable, or could not be moved IAW the <i>dx,dy</i> 
    * arguments.
    */
   public String getMoveCue(double dx, double dy)
   {
      if(!canMove()) return(null);
      
      // transform anchor point into figure-level ("global") coordinates
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(null);
      Point2D anchor = parentVP.toMilliInches(x, y);
      if(anchor == null) return(null);
      AffineTransform at = getParent().getLocalToGlobalTransform();
      at.transform(anchor, anchor);

      // adjust anchor position IAW the specified offsets
      anchor.setLocation(anchor.getX() + dx, anchor.getY() + dy);

      // transform new anchor point back to parent viewport
      try { at = at.createInverse(); }
      catch( NoninvertibleTransformException nte ) { return(null); }
      at.transform(anchor, anchor);

      // get anchor point coordinates in their original units of measure, and limit precision in a reasonable way.
      Measure.Constraints c = FGraphicModel.getLocationConstraints(getNodeType());
      Measure[] coords = parentVP.fromMilliInches(anchor.getX(), x.getUnits(), anchor.getY(), y.getUnits(), 
            c.nMaxFracDigits);
      if(coords == null) return(null);
      
      coords[0] = c.constrain(coords[0], true);
      coords[1] = c.constrain(coords[1], true);
      
      return("X=" + coords[0].toString() + ", Y=" + coords[1].toString());
   }
   
   /**
    * Adjust the current location of this graphic node IAW the specified offsets.
    * 
    * <p>This method supports interactive repositioning of one or more selected nodes in the {@link FGraphicModel}
    * figure container. After the user "drags" the selected node(s) with the mouse, this method is invoked on each 
    * node to update its position accordingly. When multiple nodes are selected, update each node with the <i>notify</i>
    * argument cleared -- so that the multi-node operation may be encapsulated as a single reversible edit.</p>
    * 
    * <p>The default implementation is a no-op, unless the node is "relocatable" -- meaning it has the <i>x</i> and 
    * <i>y</i> properties specifying the node's location WRT its parent viewport. In that case, the method will attempt 
    * to adjust the location coordinates IAW the specified offsets. It transforms the anchor point <i>(x,y)</i> from the
    * parent node's viewport to the global rendering viewport (in milli-inches), adds the offsets, then transforms back 
    * to the parent viewport. The <i>x</i> and <em>y</em> properties are adjusted accordingly, without changing their 
    * current units of measure. Enough decimal digits are preserved in the new values of <i>x</i> and <i>y</i> to 
    * achieve roughly milli-inch precision. However, if the units are %, then just one decimal digit is preserved; if 
    * user units, the maximum number of digits (based on the node's location constraints) are preserved. If successful
    * AND the <i>notify</i> flag is set, a reversible edit is posted to the model's edit history, the node's rendering 
    * infrastructure is updated, and a re-rendering of the graphic model is triggered.</p>
    * 
    * <p>Any subclass that must implement a different means of "moving" the node should override both this method and 
    * {@link #canMove()}.</p>
    * 
    * @param dx Net change in horizontal position of node, in milli-in WRT model's "global" coordinate system.
    * @param dy Net change in vertical position of node, in milli-in WRT model's "global" coordinate system.
    * @param notify If set and node's location was changed, a reversible edit is posted to the model's edit history and
    * {@link #onNodeModified} is invoked to trigger a re-rendering of the model.
    * @return True if node's location was changed, false otherwise.
    */
   boolean executeMove(double dx, double dy, boolean notify)
   {
      if(!canMove()) return(false);

      // transform anchor point into figure-level ("global") coordinates
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(false);
      Point2D anchor = parentVP.toMilliInches(x, y);
      if(anchor == null) return(false);
      AffineTransform at = getParent().getLocalToGlobalTransform();
      at.transform(anchor, anchor);

      // adjust anchor position IAW the specified offsets
      anchor.setLocation(anchor.getX() + dx, anchor.getY() + dy);

      // transform new anchor point back to parent viewport
      try { at = at.createInverse(); }
      catch( NoninvertibleTransformException nte ) { return(false); }
      at.transform(anchor, anchor);

      // get anchor point coordinates in their original units of measure
      Measure.Constraints c = FGraphicModel.getLocationConstraints(getNodeType());
      Measure[] coords = parentVP.fromMilliInches(anchor.getX(), x.getUnits(), anchor.getY(), y.getUnits(), 
            c.nMaxFracDigits);
      if(coords == null) return(false);
      
      coords[0] = c.constrain(coords[0], true);
      coords[1] = c.constrain(coords[1], true);
      
      if(!(Measure.equal(x, coords[0]) && Measure.equal(y, coords[1])))
      {
         Measure oldX = x;
         Measure oldY = y;
         x = coords[0];
         y = coords[1];
         if(notify)
         {
            onNodeModified(FGNProperty.X);
            FGNRevEdit.postMove(this, oldX, oldY);
         }
         return(true);
      }
      
      return(false);
   }

   /**
    * Can this graphic node be resized interactively?
    * 
    * <p>The default implementation returns true if the node posseses the location and size attributes <i>x</i>, 
    * <i>y</i>, <i>width</i>, and <i>height</i>, so that resizing the node involves changing the values of one or more
    * of these properties. Any subclass representing a node that can be interactively resized by changing other node 
    * properties must override this and other relevant methods. Any subclass that has the standard location and size
    * attributes, but does not support interactive resizing, should override this method and return false.</p>
    * 
    * @return True if the node can be resized interactively via an appropriate gesture on the user interface.
    */
   public boolean canResize() { return((attrFlags & (HASLOCATTRS|HASSIZEATTRS)) == (HASLOCATTRS|HASSIZEATTRS)); }

   /**
    * Get the shape defining the resize outline for this graphic node, transformed to the global coordinate system of
    * the root figure. Interactively resizing the node involves "grabbing" a point on this outline and dragging it to
    * change the node's current size.
    * 
    * <p>Default implementation simply returns the rectangle generated by {@link #getResizeRectLocal()}, transformed to
    * global figure coordinates. If the local resize rectangle is null, the implementation also returns null.</p>
    * 
    * @return The node's resize outline, transformed into a general shape in figure coordinates. Returns null if this 
    * node does not support interactive resizing, or if the outline shape could not be prepared for any reason.
    */
   public Shape getResizeShape()
   {
      return(getLocalToGlobalTransform().createTransformedShape(getResizeRectLocal()));
   }

   /**
    * Get the resize rectangle defining the resize outline for this graphic node, in the node's own local coordinate
    * system. 
    * <p>The default implementation returns a copy of the node's cached local render bounds. Returns null if the cached 
    * bounds are not available. <i>The base implementations of the other interactive resize methods all rely on this
    * method.</i></p>
    * 
    * @return The node's resize rectangle, expressed in the node's own local coordinate system. Returns null if this
    * node does not support interactive resizing, or if the resize rectangle could not be generated for any reason.
    */
   Rectangle2D getResizeRectLocal()
   {
      if((!canResize()) || cachedLocalBounds == null || cachedLocalBounds.isEmpty()) return(null);
      return(new Rectangle2D.Double(cachedLocalBounds.getX(), cachedLocalBounds.getY(),
            cachedLocalBounds.getWidth(), cachedLocalBounds.getHeight()));
   }
   
   /**
    * Get the resize anchor appropriate to resize this node interactively, if the resize starts at the specified point.
    * The point -- once transformed to local coordinates -- must be close enough to the path defining the node's current
    * resize rectangle ({@link #getResizeRectLocal()}. If the node cannot be resized interactively or the point is too 
    * far off the resize outline, then the resize anchor is undefined.
    * 
    * @param p A location in the root figure's rendering coordinates. Note that location may be altered by method.
    * @param tol The location must be within this distance from the resize outline (in milli-inches) to be considered
    * close enough.
    * @return The resize anchor appropriate to the point's location on/near the resize outline. Returns null if point is
    * not within tolerance of the resize outline. Also returns null if the resize outline is undefined or the node is 
    * not resizable.
    */
   public ResizeAnchor getResizeAnchor(Point2D p, double tol)
   {
      Rectangle2D rLocal = getResizeRectLocal();
      if(rLocal == null || p == null) return(null);
      
      // transform point to local coordinate system for comparison with local rendered bounding rectangle
      AffineTransform at = getLocalToGlobalTransform();
      try { at = at.createInverse(); } catch(NoninvertibleTransformException nte) { return(null); }
      at.transform(p, p);
      
      double left = rLocal.getX();
      double bot = rLocal.getY();
      double right = left + rLocal.getWidth();
      double top = bot + rLocal.getHeight();
      
      // if point is too far outside the local bounding box, then no the resize anchor is undefined.
      if(p.getX() < left-tol || p.getX() > right+tol || p.getY() < bot-tol || p.getY() > top+tol) return(null);
      
      boolean isLeft = Math.abs(p.getX()-left) <= tol;
      boolean isRight = (!isLeft) && (Math.abs(p.getX()-right) <= tol);
      boolean isBottom = Math.abs(p.getY()-bot) <= tol;
      boolean isTop = (!isBottom) && (Math.abs(p.getY()-top) <= tol);
      
      return(ResizeAnchor.getAnchor(isLeft, isRight, isTop, isBottom));
   }
   
   /**
    * Get the shape defining the resize outline for this graphic node during an ongoing drag-to-resize interaction.
    * 
    * <p>Default implementation calls {@link #getDragResizeRectLocal(ResizeAnchor, Point2D, Point2D)} to compute the
    * node's resize rectangle given the specified drag parameter, then transforms that rectangle back to global figure
    * coordinates. Returns null if the resize outline is undefined. Otherwise, if the cue buffer is provided, its
    * content is initialized to list the revised values for the node's <i>x, y, width, height</i> properties.</p>
    * 
    * @param anchor The resize anchor determines the kinds of changes allowed during the drag-resize interaction.
    * @param p0 The anchor point for the resize-by-drag interaction, in root figure's rendering coordinates. Note that
    * point may be altered by the method.
    * @param p1 The current drag location for the interaction, in root figure's rendering coordinates. Again, the point
    * may be altered by the method.
    * @param cueBuf (Optional) If the dynamic resize shape was successfully computed and this string buffer is not null,
    * it is initialized with a short string describing the node property values that would be updated to resize the
    * node should the drag-to-resize interaction be consummated in the current state.
    * @return The drag outline that represents the current effect of the resize-drag interaction, in root figure's 
    * rendering coordinates. Since the node could be rotated WRT the root figure, the outline is returned as a generic
    * shape rather than a rectangle. Returns null if the drag outline could not be computed, or if this node does not 
    * support interactive resizing.
    */
   public Shape getDragResizeShape(ResizeAnchor anchor, Point2D p0, Point2D p1, StringBuffer cueBuf)
   {
      Rectangle2D r = getDragResizeRectLocal(anchor, p0, p1);
      if(r == null) return(null);
      
      // prepare the resize cue if a buffer was provided. Be careful to change only those parameters that would be
      // affected given the resize anchor in effect!
      if(cueBuf != null)
      {
         Measure xRev = getX();
         Measure yRev = getY();
         Measure wRev = getWidth();
         Measure hRev = getHeight();
         
         // get location coordinates in their original units of measure, and limit precision in a reasonable way.
         FViewport2D parentVP = getParentViewport();
         boolean ok = (parentVP != null);
         if(ok)
         {
            // transform node location (x,y) to parent viewport
            AffineTransform local2P = getLocalToParentTransform();
            Point2D.Double loc = new Point2D.Double(r.getX(), r.getY());  
            local2P.transform(loc, loc);
            
            Measure.Constraints c = FGraphicModel.getLocationConstraints(getNodeType());
            Measure[] coords = parentVP.fromMilliInches(loc.getX(), x.getUnits(), loc.getY(), y.getUnits(), 
                  c.nMaxFracDigits);
            ok = (coords != null);
            if(ok)
            {
               if(anchor.isWest()) xRev = c.constrain(coords[0], true);
               if(anchor.isSouth()) yRev = c.constrain(coords[1], true);
            }
         } 
         
         // convert width and height back to their original units of measure WRT parent viewport.
         if(ok && width.getUnits() != Measure.Unit.USER && (anchor.isEast() || anchor.isWest()))
         {
            Measure.Constraints c = FGraphicModel.getSizeConstraints(getNodeType());
            wRev = c.constrain(parentVP.fromMilliInToMeasureX(r.getWidth(), width.getUnits()), true);
         }
         if(ok && height.getUnits() != Measure.Unit.USER && (anchor.isNorth() || anchor.isSouth()))
         {
            Measure.Constraints c = FGraphicModel.getSizeConstraints(getNodeType());
            hRev = c.constrain(parentVP.fromMilliInToMeasureX(r.getHeight(), height.getUnits()), true);
         }
         
         if(ok)
         {
            cueBuf.setLength(0);
            cueBuf.append("X=").append(xRev.toString());
            cueBuf.append(", Y=").append(yRev.toString());
            cueBuf.append(", W=").append(wRev.toString());
            cueBuf.append(", H=").append(hRev.toString());
         }
      }

      return(getLocalToGlobalTransform().createTransformedShape(r));
   }
   
   /**
    * Get the rectangle defining the resize outline for this graphic node during an interactive resize-drag between the
    * specified points using the specified resize anchor. The rectangle is returned in the node's local coordinates.
    * 
    * <p>Default implementation transforms the anchor and drag points to local coordinates and computes the deltas in 
    * their X and Y coordinates. The left, right, top and/or bottom edges of the initial resize rectangle, as returned
    * by {@link #getResizeRectLocal()}, are adjusted IAW these deltas. Returns null if initial resize rectangle is 
    * undefined.</p>
    * 
    * <p>Note that the resize anchor restricts the kinds of changes allowed during the drag-resize interaction. For
    * example, if the "northeast" corner anchor is being dragged, only changes to the left and top edges of the resize
    * rectangle are allowed; if the "south" edge anchor is dragged, only the bottom edge can change. Handling of the
    * non-directional anchor {@link ResizeAnchor#ANY} is implementation-specific. This default implementation equates
    * it to the "southeast" anchor.</p>
    * 
    * @param anchor The resize anchor determines the kinds of changes allowed during the drag-resize interaction.
    * @param p0 The anchor point for the resize-by-drag interaction, in root figure's rendering coordinates. Note that
    * point may be altered by the method.
    * @param p1 The current drag location for the interaction, in root figure's rendering coordinates. Again, the point
    * may be altered by the method.
    * @return The computed resize rectangle, expressed in the node's own local coordinate system. Returns null if this
    * node does not support interactive resizing, or if the resize rectangle could not be generated for any reason.
    */
   Rectangle2D getDragResizeRectLocal(ResizeAnchor anchor, Point2D p0, Point2D p1)
   {
      Rectangle2D rLocal = getResizeRectLocal();
      if(rLocal == null || anchor == null || p0 == null || p1 == null) return(null);
      
      // transform anchor and drag points to local coordinate system for comparison with local rendered bounding rect
      AffineTransform at = getLocalToGlobalTransform();
      AffineTransform invAT;
      try { invAT = at.createInverse(); } catch(NoninvertibleTransformException nte) { return(null); }
      invAT.transform(p0, p0);
      invAT.transform(p1, p1);
      
      // locally, INITIAL drag shape is the node's resize rectangle before dragging started
      double left = rLocal.getX();
      double bot = rLocal.getY();
      double right = left + rLocal.getWidth();
      double top = bot + rLocal.getHeight();
      double w = right-left;
      double h = top-bot;
      
      // deltas in X and Y, in milli-inches
      double dx = p1.getX() - p0.getX();
      double dy = p1.getY() - p0.getY();
      
      // restrict changes based on anchor. For edge anchors, only that edge can change. For corner anchors, the two
      // edges can change. But, in all cases, don't let the edge move to within 10% of the opposing edge.
      switch(anchor)
      {
      case NORTH: 
         top += dy;
         top = Math.max(top, bot + 0.1*h);
         break;
      case SOUTH:
         bot += dy;
         bot = Math.min(bot,  top - 0.1*h);
         break;
      case EAST:
         right += dx;
         right = Math.max(right,  left + 0.1*w);
         break;
      case WEST:
         left += dx;
         left = Math.min(left,  right - 0.1*w);
         break;
      case NORTHWEST:
         top += dy;
         top = Math.max(top, bot + 0.1*h);
         left += dx;
         left = Math.min(left, right - 0.1*w);
         break;
      case NORTHEAST:
         top += dy;
         top = Math.max(top, bot + 0.1*h);
         right += dx;
         right = Math.max(right,  left + 0.1*w);
         break;
      case SOUTHWEST:
         bot += dy;
         bot = Math.min(bot, top - 0.1*h);
         left += dx;
         left = Math.min(left, right - 0.1*w);
         break;
      case SOUTHEAST:
      case ANY:
         bot += dy;
         bot = Math.min(bot,  top - 0.1*h);
         right += dx;
         right = Math.max(right,  left + 0.1*w);
         break;
      }
      
      rLocal.setFrame(left, bot, right-left, top-bot);
      return(rLocal);
   }

   /**
    * Resize this graphic node IAW a drag-resize interaction that started and ended at the specified points using the
    * resize anchor specified. Whatever changes are required to resize the node are done as a single, reversible
    * operation that is posted to the containing model's "undo" history.
    * 
    * <p>The default implementation is a no-op, unless the node is resizable -- see {@link #canResize()}. In that case,
    * the method computes the final resize rectangle in local milli-inches. From this the new values for properties 
    * <i>x, y, width, height</i> are calculated, in their original units of measure. Note that, depending on the resize 
    * anchor, any subset of these four properties will change (if the "southwest" corner was dragged, then all four
    * change). The affected properties are changed, a single reversible operation is posted to the undo history that
    * includes the old values of the affected properties, and {@link #onNodeModified(Object)} is called.</p>
    * 
    * @param anchor The resize anchor determines the kinds of changes allowed during the drag-resize interaction.
    * @param p0 The anchor point for the resize-by-drag interaction, in root figure's rendering coordinates. Note that
    * point may be altered by the method.
    * @param p1 The drag point at which the interaction ended, in root figure's rendering coordinates. Again, point may
    * be altered by the method.
    */
   public void executeResize(ResizeAnchor anchor, Point2D p0, Point2D p1)
   {
      if(!canResize()) return;
      Rectangle2D r = getDragResizeRectLocal(anchor, p0, p1);
      if(r == null) return;

      // need to remember the old values for any properties affected so we can prepare the undo operation.
      Measure oldX = null;
      Measure oldY = null;
      Measure oldW = null;
      Measure oldH = null;
      
      // update only those properties that may be changed given the limitations imposed by resize anchor. If unable to
      // update any property, then don't make any changes at all. We preserve the measurement units of any property
      // that is changed.
      FViewport2D parentVP = getParentViewport();
      boolean ok = (parentVP != null);
      if(ok && (anchor.isWest() || anchor.isSouth()))
      {
         // transform node location (x,y) to parent viewport
         AffineTransform local2P = getLocalToParentTransform();
         Point2D.Double loc = new Point2D.Double(r.getX(), r.getY());  
         local2P.transform(loc, loc);
         
         Measure.Constraints c = FGraphicModel.getLocationConstraints(getNodeType());
         Measure[] coords = parentVP.fromMilliInches(loc.getX(), x.getUnits(), loc.getY(), y.getUnits(), 
               c.nMaxFracDigits);
         ok = (coords != null);
         if(ok)
         {
            if(anchor.isWest()) { oldX = x; x = c.constrain(coords[0], true); }
            if(anchor.isSouth()) { oldY = y; y = c.constrain(coords[1], true); }
         }
      } 
      
      if(ok && width.getUnits() != Measure.Unit.USER && (anchor.isEast() || anchor.isWest()))
      {
         oldW = width;
         Measure.Constraints c = FGraphicModel.getSizeConstraints(getNodeType());
         width = c.constrain(parentVP.fromMilliInToMeasureX(r.getWidth(), width.getUnits()), true);
      }
      
      if(ok && height.getUnits() != Measure.Unit.USER && (anchor.isNorth() || anchor.isSouth()))
      {
         oldH = height;
         Measure.Constraints c = FGraphicModel.getSizeConstraints(getNodeType());
         height = c.constrain(parentVP.fromMilliInToMeasureX(r.getHeight(), height.getUnits()), true);
      }

      if(ok) 
      {
         onNodeModified(null);
         FGNRevEdit.postBoxResize(this, oldX, oldY, oldW, oldH);
      }
   }
   
   /**
    * Change the location and dimensions of this graphic node to undo or redo a prior resize operation. Since we're 
    * reversing or re-applying a previously applied edit, the change is NOT posted to the containing model's edit 
    * history.
    * 
    * <p>The default implementation assumes the resize operation was performed by the default implementation of {@link
    * #executeResize}. Therefore, the affected properties may only include the node's location <i>x, y</i> and
    * dimensions <i>width, height</i>. If the affected properties include none of these, or include other properties, no
    * action is taken and the method returns false.</p>
    * 
    * @param props The node properties affected by the resize operation to be undone. 
    * @return True if successful, false otherwise.
    */
   boolean undoOrRedoResize(HashMap<FGNProperty, Measure> props)
   {
      if(props == null || !canResize()) return(false);
      
      // if affected properties include anything other than x,y,w,h then something is seriously wrong!
      Measure oldX = props.get(FGNProperty.X);
      Measure oldY = props.get(FGNProperty.Y);
      Measure oldW = props.get(FGNProperty.WIDTH);
      Measure oldH = props.get(FGNProperty.HEIGHT);
      int nProps = 0;
      if(oldX != null) ++nProps;
      if(oldY != null) ++nProps;
      if(oldW != null) ++nProps;
      if(oldH != null) ++nProps;
      if(nProps == 0 || nProps != props.keySet().size()) return(false);
      
      if(oldX != null) x = oldX;
      if(oldY != null) y = oldY;
      if(oldW != null) width = oldW;
      if(oldH != null) height = oldH;
      
      onNodeModified(null);
      return(true);
   }
   
   /**
    * Can this graphic node be aligned with respect to other relocatable nodes in the same figure? Nodes may be aligned
    * along a common left, right, top or bottom edge, or they may be centered horizontally or vertically.
    * <p>The base class implementation returns true if the node possesses the standard bounding box properties <i>X, Y,
    * width, height</i>.</p>
    * @return True if this node may be aligned with respect to another node in the same figure.
    */
   boolean canAlign() 
   { 
      return((attrFlags & (HASLOCATTRS|HASSIZEATTRS)) == (HASLOCATTRS|HASSIZEATTRS));
   }

   /**
    * Get this node's alignment locus for the specified type of alignment, given the node's current state: the X 
    * coordinate of the node's left edge, right edge or horizontal center; or the Y-coordinate of the node's bottom
    * edge, top edge, or vertical midpoint. The coordinate should be specified in milli-inches WRT the containing
    * figure's global coordinate system.
    * <p>The base class implementation transforms the node's rectangular bounding box <i>X, Y, width, height</i> to a
    * shape (if the box is rotated WRT figure!) in the figure's global coordinates, then finds the rectangular bounds 
    * <i>Xg, Yg, Wg, Hg</i> of that shape, then computes: left edge = <i>Xg</i>, right = <i>Xg + Wg</i>, horizontal 
    * center = <i>Xg + Wg/2</i>, etcetera.</p>
    * <p>If the node lacks the <i>width, height</i> attributes but still has the location attributes <i>X, Y</i>, this 
    * base class implementation will instead transform the node's cached local render bounds to the figure's global 
    * coordinates. However, to take advantage of this alternate implementation, a subclass must override {@link 
    * #canAlign()} appropriately.</p>
    * 
    * @param align The alignment type. If null, method returns null.
    * @return The alignment locus, as described. If it cannot be computed, method returns null.
    */
   Double getAlignmentLocus(FGNAlign align)
   {
      if(align == null || !canAlign()) return(null);
      
      // find the node's bounding rectangle in figure-level ("global") coordinates. If the node defines a rectangular 
      // bounding box via attributes {x,y,width,height}, then that rectangle is transformed to a shape in global 
      // coordinates, and then that shape's bounding rectangle is computed. If the node lacks the {width, height}
      // attributes, its cached local render bounds are transformed instead.
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(null);
      
      Rectangle2D r = null;
      if((attrFlags & (HASLOCATTRS|HASSIZEATTRS)) == (HASLOCATTRS|HASSIZEATTRS))
      {
         double wMI = parentVP.fromMeasureToMilliInX(width);
         double hMI = parentVP.fromMeasureToMilliInY(height);
         r = new Rectangle2D.Double(0, 0, wMI, hMI);
      }
      else if((attrFlags & HASLOCATTRS) == HASLOCATTRS)
      {
         r = (cachedLocalBounds != null && !cachedLocalBounds.isEmpty()) ? cachedLocalBounds : null;
      }
      if(r == null) return(null);
      
      r = getLocalToGlobalTransform().createTransformedShape(r).getBounds2D();

      // compute the alignment coordinate based upon the desired alignment
      Double coord = null;
      switch(align)
      {
      case LEFT: coord = r.getMinX(); break;
      case RIGHT: coord = r.getMaxX(); break;
      case BOTTOM: coord = r.getMinY(); break;
      case TOP: coord = r.getMaxY(); break;
      case HCENTER: coord = r.getCenterX(); break;
      case VCENTER: coord = r.getCenterY(); break;
      }
      return(coord);
   }
   
   /**
    * Align an edge or center line (H or V) of this graphic node to the specified locus. Make all necessary property
    * changes to achieve the desired alignment. Record all such property changes in the reversible edit object supplied.
    * <p>Since node alignment intrinsically involves repositioning two or more nodes in the same figure, it is assumed
    * that the caller has blocked the model history and will trigger a re-rendering of the entire figure once the
    * alignment operation is finished.</p>
    * <p>The base class implementation transforms the node's local rectangular bounding box -- defined by the properties
    * <i>X, Y, width, height</i> -- to figure-level coordinates, then gets the rectangle bounding that transformed 
    * shape. It then computes the necessary adjustment in the X- or Y-coordinate of the node's location to align that 
    * "global bounding rectangle" to the locus specified. The node's dimensions are unchanged.</p>
    * <p>If the node lacks the <i>width, height</i> attributes but still has the location attributes <i>X, Y</i>, this 
    * base class implementation will instead transform the node's cached local render bounds to the figure's global 
    * coordinates to compute the "global bounding rectangle". However, to take advantage of this alternate 
    * implementation, a subclass must override {@link #canAlign()} appropriately.</p>
    * 
    * @param alignType The alignment type. If null, method takes no action.
    * @param locus The X- or Y-coordinate of the alignment locus, in milli-inches WRT the figure's coordinate system.
    * @param undoer The reversible edit object in which all node property changes are recorded during the multi-step
    * alignment operation -- so that the operation may be undone or redone in one go. Must not be null
    * @return True if node's position was adjusted to achieve the desired alignment; false if alignment is not supported
    * or the necessary adjustment could not be calculated for some reason, or no adjustment was necessary.
    */
   boolean align(FGNAlign alignType, double locus, MultiRevEdit undoer)
   {
      if(alignType == null || undoer == null || (!Utilities.isWellDefined(locus)) || !canAlign()) return(false);
      
      // find the node's bounding rectangle in figure-level ("global") coordinates. If the node defines a rectangular 
      // bounding box via attributes {x,y,width,height}, then that rectangle is transformed to a shape in global 
      // coordinates, and then that shape's bounding rectangle is computed. If the node lacks the {width, height}
      // attributes, its cached local render bounds are transformed instead.
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(false);
      
      Rectangle2D r = null;
      if((attrFlags & (HASLOCATTRS|HASSIZEATTRS)) == (HASLOCATTRS|HASSIZEATTRS))
      {
         double wMI = parentVP.fromMeasureToMilliInX(width);
         double hMI = parentVP.fromMeasureToMilliInY(height);
         r = new Rectangle2D.Double(0, 0, wMI, hMI);
      }
      else if((attrFlags & HASLOCATTRS) == HASLOCATTRS)
      {
         r = (cachedLocalBounds != null && !cachedLocalBounds.isEmpty()) ? cachedLocalBounds : null;
      }
      if(r == null) return(false);
      
      r = getLocalToGlobalTransform().createTransformedShape(r).getBounds2D();

      // also transform anchor point into figure-level ("global") coordinates, in milli-inches
      Point2D anchor = parentVP.toMilliInches(x, y);
      if(anchor == null) return(false);
      AffineTransform at = getParent().getLocalToGlobalTransform(); 
      at.transform(anchor, anchor);
      if(!Utilities.isWellDefined(anchor)) return(false);
      
      // compute adjustment needed to achieve the desired alignment at figure-level
      double dx = 0;
      double dy = 0;
      switch(alignType)
      {
      case LEFT: dx = locus - r.getMinX(); break;
      case RIGHT: dx = locus - r.getMaxX(); break;
      case BOTTOM: dy = locus - r.getMinY(); break;
      case TOP: dy = locus - r.getMaxY(); break;
      case HCENTER: dx = locus - r.getCenterX(); break;
      case VCENTER: dy = locus - r.getCenterY(); break;
      }
      anchor.setLocation(anchor.getX() + dx, anchor.getY() + dy);
 
      // transform adjusted anchor point back to parent viewport
      try { at = at.createInverse(); }
      catch( NoninvertibleTransformException nte ) { return(false); }
      at.transform(anchor, anchor);

      // get anchor point coordinates in their original units of measure
      Measure.Constraints c = FGraphicModel.getLocationConstraints(getNodeType());
      Measure[] coords = parentVP.fromMilliInches(anchor.getX(), x.getUnits(), anchor.getY(), y.getUnits(), 
            c.nMaxFracDigits);
      if(coords == null) return(false);

      coords[0] = c.constrain(coords[0], true);
      coords[1] = c.constrain(coords[1], true);
      
      boolean changed = false;
      if(!Measure.equal(x, coords[0]))
      {
         changed = true;
         Measure oldX = x;
         x = coords[0];
         undoer.addPropertyChange(this, FGNProperty.X, x, oldX);
      }
      if(!Measure.equal(y, coords[1]))
      {
         changed = true;
         Measure oldY = y;
         y = coords[1];
         undoer.addPropertyChange(this, FGNProperty.Y, y, oldY);
      }

      return(changed);
   }
   
   /**
    * Return the descendant graphic node that is near or underneath the specified point. If more than one descendant
    * is under the point, attempt to return the one with the smallest bounding rectangle. If no such descendant exists, 
    * but this node is under the point, then a reference to this node is returned.
    * 
    * <p>This method provides a mechanism for interactively selecting a particular graphic object within a figure that
    * is currently rendered on a canvas view. The algorithm seeks the smallest graphic object under the specified point
    * so that the user can "pick out" an object that is "covered" by a sibling object (even though that sibling may not
    * obscure the object at all).</p>
    * 
    * <p>To keep the UI responsive, it is important that implementations execute as quickly as possible, since the 
    * recursive invocations of this method will ripple through the tree of nodes comprising the graphic model. This 
    * default implementation uses a cached {@link Shape} representing a node's rendered bounds transformed into the 
    * "global" coordinate system of the model's root node.. Since the coordinates of the mousedown point are already 
    * expressed in this global viewport, the method simply tests whether or not the cached shape contains the point. 
    * Only if it does will it search this node's list of subordinates for the smallest subordinate hit. The method is 
    * recursively called to search the subordinate list. If there is no such subordinate, the method returns a reference
    * to the element itself.</p>
    * 
    * <p>Note that the method searches the entire subordinates list, which may include private component nodes. This 
    * allows a component node to have the "focus" for display and editing purposes. If this behavior is undesired, or 
    * if a subclass needs to implement a different hit-testing algorithm entirely, then override this method.</p>
    * 
    * @param p The test point. This is expressed in the "global" rendering coordinate system of the graphic node tree's 
    * root node. By convention, this a right-handed coordinate system (x-axis increasing rightward, y-axis increasing 
    * upward) with origin at the bottom-left corner of the root node's bounding rectangle and with logical units in 
    * milli-inches. <i>It is imperative that the point not be altered.</i>
    * @return The graphic node selected by the point, as described. If null, neither this node nor any of its 
    * descendants are near or underneath the specified point.
    */
   protected FGraphicNode hitTest(Point2D p)
   {
      if(cachedGlobalShape.contains(p))
      {
         FGraphicNode smallest = findSmallestSubordinateHit(p);
         return((smallest != null) ? smallest : this);
      }

      return(null);
   }

   /**
    * Helper method which executes the {@link #hitTest(Point2D)} method on all subordinates of this graphic node, 
    * ultimately returning a reference to the smallest descendant for which a "hit" was detected. By "smallest", we
    * mean the descendant node with the smallest bounding rectangle (in area).
    * 
    * <p>An implementation of <code>FGraphicNode</code> that wants to prevent "hitting" one of its private component 
    * nodes must override this method.</p>
    * 
    * @param p The test point. This is expressed in the "global" rendering coordinate system of the graphic node tree's 
    * root node. By convention, this a right-handed coordinate system (x-axis increasing rightward, y-axis increasing 
    * upward) with origin at the bottom-left corner of the root node's bounding rectangle and with  logical units in 
    * milli-inches. <i>It is imperative that the point not be altered.
    * @return The smallest descendant under the test point, or null if none was found.
    */
   protected FGraphicNode findSmallestSubordinateHit(Point2D p)
   {
      FGraphicNode smallest = null;
      double smallestArea = 0;
      for(FGraphicNode sub : subordinates)
      {
         FGraphicNode hit = sub.hitTest(p);
         if(hit != null)
         {
            if(smallest == null)
            {
               smallest = hit;
               Rectangle2D bounds = hit.getCachedGlobalBounds();
               smallestArea = bounds.getWidth() * bounds.getHeight();
            }
            else
            {
               Rectangle2D bounds = hit.getCachedGlobalBounds();
               double area = bounds.getWidth() * bounds.getHeight();
               if(area < smallestArea)
               {
                  smallest = hit;
                  smallestArea = area;
               }
            }
         }
      }
      return(smallest);
   }
   
   
   //
   // PSTransformable
   //
   
   /**
    * Returns a Postscript comment of the form <em>{type} {title}</em>, where <em>{type}</em> is the graphic node 
    * type tag and <em>{title}</em> is the string returned by {@link #getTitle()}. Since the node title could have
    * linefeeds or carriage returns, which would mess up the Postscript document if included (because the comment 
    * would continue on a new line without the '%' character that marks it as a comment line!), all such characters
    * in the title string are replaced by a '|' character.
    */
   public String getPSComment() 
   { 
      String title = getTitle().trim();
      title = title.replace('\n', '|');
      title = title.replace('\r', '|');
      return(getNodeType().toString() + " " + title); 
   }

   public String getPSFontFace()
   {
      if(!hasFontProperties()) return(null);
      FontStyle fs = getFontStyle();
      return(PSDoc.getStandardFontFace(getPSFont(), fs.isBold(), fs.isItalic()));
   }

   /**
    * <p>When <i>traverse=true</i>, this method employs a node stack to avoid recursion -- except to call the method
    * with <i>traverse=false</i> to the font faces used by the node itself. The implementation includes the PS font face
    * returned by {@link #getPSFontFace()}; in addition, if the node's title uses attributed text, it includes any
    * additional PS font faces implied by style changes within the attributed text. Subclasses should override and 
    * handle the <i>traverse=false</i> case if this implementation does not suffice.</p>
    * 
    */
   @Override public void getAllPSFontFacesUsed(Map<String, String> fontFaceMap, boolean traverse)
   {
      if(!traverse)
      {
         String face = getPSFontFace();
         if(face != null) fontFaceMap.put(face, null);
         
         // add any font faces for style changes in node's title (w=bold, i=italic, I=bolditalic, p=plain)
         String s = getTitle();
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
         return;
      }
      
      Stack<FGraphicNode> nodeStack = new Stack<>();
      nodeStack.push(this);
      while(!nodeStack.isEmpty())
      {
         FGraphicNode n = nodeStack.pop();
         n.getAllPSFontFacesUsed(fontFaceMap, false);

         for(FGraphicNode sub : n.subordinates) nodeStack.push(sub);
      }
   }

   public double getPSFontSize() { return(getFontSize()); }
   
   /** Returns 0 if the node is not stroked, regardless the defined stroke width -- see {@link #isStroked(). */
   public double getPSStrokeWidth() { return(hasStrokeProperties() && isStroked() ? getStrokeWidth() : 0); }
   
   public int getPSStrokeEndcap() 
   { 
      return(hasStrokeProperties() ? getStrokeEndcap().getPostscriptLineCap() : 0); 
   }
   public int getPSStrokeJoin() 
   { 
      return(hasStrokeProperties() ? getStrokeJoin().getPostscriptLineJoin() : 0); 
   }

   public int[] getPSStrokeDashPattern()
   {
      if(!hasStrokePatternProperty()) return(PSDoc.SOLIDLINE);
      float[] dashGap = getStrokePattern().getDashGapArrayMilliIn(getStrokeWidth(), getStrokeEndcap()!=StrokeCap.BUTT);
      int[] iDashGap = new int[dashGap.length];
      for(int i=0; i<dashGap.length; i++) iDashGap[i] = Math.round(dashGap[i]);
      return(iDashGap);
   }

   /**
    * Currently, <em>DataNav</em> does not support the concept of an offset into the stroke dash-gap pattern.  This
    * method returns 0 always.
    * @see PSTransformable#getPSStrokeDashOffset()
    */
   public int getPSStrokeDashOffset() { return(0); }
   public int getPSStrokeColor() { return(hasStrokeProperties() ? (getStrokeColor().getRGB() & 0x00FFFFFF) : 0); }
   public int getPSTextAndFillColor() { return(hasFillColorProperty() ? (getFillColor().getRGB() & 0x00FFFFFF) : 0); }
   public boolean isPSNoFill() { return(hasFillColorProperty() && getFillColor().getAlpha() == 0); }
   public boolean isPSNoStroke() { return(hasStrokeProperties() && getStrokeColor().getAlpha() == 0); }
   
   //
   // Object
   //

   /**
    * The cloned <code>FGraphicNode</code> returned by this override will be a completely independent copy of this 
    * node. All subordinates in this node are deeply cloned into the new node. The parent of the cloned node is 
    * set to <code>null</code>, and the rendering infrastructure is set to an uninitialized state. 
    */
   @Override protected Object clone() throws CloneNotSupportedException
   {
      FGraphicNode copy = null;
      try {copy = (FGraphicNode) super.clone(); } catch(CloneNotSupportedException cnse) {assert(true);}
      assert copy != null;

      // must set parent to null before inserting clones of children; otherwise, the copy might be hooked into
      // the source model and the insertions below could trigger errant model notifications!
      copy.parent = null;
      
      // the node ID is always reset to an empty string, as graphic objects cannot have the same ID
      copy.id = "";
      
      copy.cachedLocalBounds = null;
      copy.cachedGlobalShape = new Rectangle2D.Double();

      copy.subordinates = new ArrayList<>();
      copy.nComponents = 0;
      for(int i=0; i<this.subordinates.size(); i++)
      {
         FGraphicNode subClone = (FGraphicNode) subordinates.get(i).clone();
         if(i < this.nComponents)
            copy.addComponentNode(subClone);
         else
            copy.append(subClone);
      }

      return(copy);
   }
   
      
   /**
    * A reversible edit operation which reverses the effect of applying a style set from the user's style palette to one 
    * or more graphic nodes, or reapplies the style set to those nodes.
    * 
    * @author sruffner
    */
   static class StyleSetRevEdit implements ReversibleEdit
   {
      /**
       * Construct a reversible edit which will undor or redo the application of the specified style set to one or more
       * graphic nodes. The "undo" operation restores each node's original style set, while the "redo" operation will
       * re-apply the specified styling. Affected nodes are recorded by calling {@link #addAffectedNode}.
       * 
       * @param applied The style set applied to each node to effect the reversible styling changes.
       */
      StyleSetRevEdit(FGNStyleSet applied)
      {
         if(applied == null) throw new NullPointerException();
         this.styleSet = applied;
      }
      
      /**
       * Construct a reversible edit which will undo or redo the application of a style set on the specified graphic
       * node. The "undo" operation restores the style set in effect prior to the styling changes, while the "redo"
       * operation will re-apply those changes. Additonal nodes to which the style set is applied can be recorded by
       * calling {@link #addAffectedNode}.
       * 
       * @param n The graphic node that is the subject of the reversible styling changes.
       * @param applied The style set applied to the node to effect the reversible styling changes.
       * @param oldSS The style set for the node PRIOR to the styling changes.
       */
      StyleSetRevEdit(FGraphicNode n, FGNStyleSet applied, FGNStyleSet oldSS)
      {
         this(applied);
         addAffectedNode(n, oldSS);
      }
      
      /**
       * Add a node to the list of graphic nodes to which this reversible edit object's style set has been applied. Use
       * this method when preparing a reversible edit that can undo/redo the effects of applying a particular style set
       * to more than one node in a figure -- all in one atomic operation.
       * @param n The affected node.
       * @param oldSS The style set for the node PRIOR to the styling changes.
       */
      public void addAffectedNode(FGraphicNode n, FGNStyleSet oldSS)
      {
         if(n == null || oldSS == null || oldSS.getSourceNodeType() != n.getNodeType())
            throw new IllegalArgumentException();
         affected.add(n);
         originalSS.add(oldSS);
      }
      
      @Override public String getDescription() 
      { 
         String desc = "";
         int n = affected.size();
         if(n > 0) 
            desc = "Styling changes on " + ((n>1) ? String.format("%d nodes", n) : affected.get(0).getNodeType());
         
         return(desc);
      }

      @Override public boolean undoOrRedo(boolean redo)
      {
         if(affected.isEmpty()) return(false);
         boolean ok = true;
         for(int i = 0; ok && i<affected.size(); i++)
         {
            ok = FGraphicNode.undoOrRedoStyleSetApplication(affected.get(i), redo ? styleSet : originalSS.get(i));
            affected.get(i).onNodeModified(null);
         }
         return(ok);
      }

      @Override public void release()
      {
         affected.clear();
         originalSS.clear();
         styleSet = null;
      }
      
      /** The graphic nodes that are the subject of the reversible styling change. */
      private final List<FGraphicNode> affected = new ArrayList<>();
      /** Style set applied to all nodes to effect the styling changes. It is applied again to "redo" the changes. */
      private FGNStyleSet styleSet;
      /** Each affected node's style set prior to the styling change. Applying it restores node to previous state. */
      private final List<FGNStyleSet> originalSS = new ArrayList<>();
   }
   
   /**
    * Helper method for {@link StyleSetRevEdit#undoOrRedo}. Allows recursion to handle component node styling, if
    * necessary.
    * @param n The <i>FypML</i> graphic node for which a previous styling operation is to be reversed.
    * @param styleSet The style set to be applied to the node to undo or redo a set of styling changes.
    * @return True if successful, false otherwise.
    */
   private static boolean undoOrRedoStyleSetApplication(FGraphicNode n, FGNStyleSet styleSet)
   {
      boolean ok = true;
      boolean fontAffected = false;
      Set<FGNProperty> props = styleSet.getProperties();
      for(FGNProperty prop : props)
      {
         ok = n.undoOrRedoPropertyChange(prop, styleSet.getStyle(prop), false);
         if(!ok) break;
         if(prop == FGNProperty.FONTFAMILY || prop == FGNProperty.FONTSTYLE || 
               prop == FGNProperty.FONTSIZE || prop == FGNProperty.ALTFONT)
            fontAffected = true;
      }
      
      if(fontAffected) n.propagateFontChange();
      
      if(ok)
      {
         // HACK: Axis and color axis nodes include tick mark style sets as component style sets, even though tick
         // set nodes are NOT components. Same for 3D axis nodes.
         boolean isAxis = (n.getNodeType() == FGNodeType.AXIS) || (n.getNodeType() == FGNodeType.CBAR) ||
               (n.getNodeType() == FGNodeType.AXIS3D);
         
         int nCmpts = Math.min(isAxis ? n.getChildCount() : n.getComponentNodeCount(), 
               styleSet.getNumberComponentStyleSets());
         for(int i=0; ok && i<nCmpts; i++)
         {
            FGraphicNode cmpt = isAxis ? n.getChildAt(i) : n.getComponentNodeAt(i);
            FGNStyleSet cmptStyling = styleSet.getComponentStyleSet(i);
            if(cmpt.getNodeType() == cmptStyling.getSourceNodeType())
            {
               ok = FGraphicNode.undoOrRedoStyleSetApplication(cmpt, cmptStyling);
            }
         }
      }
      
      return(ok);
   }
}
