package com.srscicomp.fc.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextAttribute;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import com.srscicomp.common.g2dutil.BasicPainterStyle;
import com.srscicomp.common.g2dutil.SingleStringPainter;
import com.srscicomp.common.g2dutil.StringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.GenericFont;
import com.srscicomp.common.ui.JUnicodeCharacterMap;
import com.srscicomp.fc.fig.FGModelListener;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.FGraphicModel.Change;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * <code>FigNodeTree</code> displays the graphic node hierarchy underlying a <i>DataNav</i> figure, as well as the 
 * properties of the currently selected node (if only one is selected) within that figure. It is an essential part of 
 * the {@link FigComposer} view controller. It allows the user to view the figure hierarchy directly, select any node or
 * set of nodes in the hierarchy, and edit the properties of individual nodes. It has two main sub-components: a node 
 * tree canvas and a node properties editor.
 * 
 * <p>A custom-painted component, the node tree canvas behaves much like a standard Java tree control but has a unique 
 * appearance. Each graphic node is represented by a node type-specific icon followed by the node's title. If it lacks a
 * title, its node type name is used instead. The icons are all 22x22 pixels, and the title font is about 12pt. Icon and
 * title are vertically centered in a 26-pixel high row; thus, the total height of the rendered node tree is 26*N, where
 * N is the number of currently reachable nodes in the tree. A node is "reachable" if all of its ancestors, back to the 
 * root figure node, are "expanded". Each entry is offset from the left edge of the component by L*24 + 2 pixels to 
 * indicate the node's hierarchical level in the tree. The root figure node is the only node at level L=0.</p>
 * 
 * <p>Like a standard tree control, any tree node entry corresponding to a graphic node with a non-empty child list can
 * be collapsed or expanded. A collapse/expand toggle icon is rendered in the indent region, just left of the node icon;
 * clicking on the icon will toggle that node's expanded state. Note that the root figure node is always expanded and
 * lacks a collapse/expand toggle; also, any leaf node or any non-leaf childless node entry will lack the toggle icon
 * as well.</p>
 * 
 * <p>The node tree canvas is embedded in a scroll pane with vertical and horizontal scroll bars always present, so 
 * the user simply scrolls as needed to see hidden parts of the tree. </p>
 * 
 * <p>As the current figure is modified within {@link FigComposer}, the rendered node tree is updated as needed. The
 * currently selected node is highlighted by a steel blue translucent overlay rectangle. The user can select a different
 * node simply by clicking on its entry in the node tree. The focus node can also be moved up/down in the tree via
 * the appropriate arrow key press.</p>
 * 
 * <p>NOTE that the node tree canvas does NOT expose the component nodes (three axes, legend, and grid line nodes) of
 * the 2D and 3D graph containers, even though most of them can be selected in the adjacent figure canvas within {@link 
 * FigComposer}. Thus, when a graph axis or legend is selected on the figure canvas, the parent graph container is
 * highlighted in the node tree canvas.</p>
 * 
 * <p>Support for multiple-node selection was introduced in app version 4.5.0 -- allowing the author to delete, copy, 
 * move-by-drag, and align the selected nodes in one operation. When multiple nodes are selected, each corresponding
 * entry in the node tree canvas is highlighted by a steel blue translucent overlay. The so-called focus node for the 
 * selection (which is always the last node in the figure's selection list) is distinguished by a solid blue opaque
 * outline as well. Furthermore, the user can define the node selection via a number of mouse and keyboard interactions 
 * with the node tree:
 * <ul>
 * <li>Clicking on any node makes it the singly-selected node in the figure, replacing any previous selection.</li>
 * <li>If the <b>Ctrl</b> key (Command key in Mac OSX) is held down when clicking on an unselected node, that node is
 * ADDED to the selection list. If the node is already selected, it becomes the focus node for the selection. If it was
 * already the focus of the selection list, it will be de-selected (unless it is the only node selected).</li>
 * <li>If the <b>Shift</b> key is held down when clicking on a node in the tree, a range of nodes is selected, between
 * the clicked node and the current anchor node for range selection. The anchor node is updated internally and not
 * exposed in the GUI.</li>
 * <li>The method {@link #changeSelection(boolean, boolean)} changes or range-extends the current selection. It is
 * tied to keybaord shortcuts in {@link FigComposer}: Up/Down arrow key moves the single-node selection to the node in
 * the row immediately before or after the currently selected node. With the Shift key is depressed, these same keys
 * extend a range selection from the current anchor position to the node before or after the selected node furthest
 * from that anchor.</li>
 * <li><i>Drag-select gesture</i>. If you drag the mouse within the node tree canvas, an translucent blue rectangular 
 * drag outline indicates the region selected. Upon releasing the mouse, each node in a row that intersects the drag
 * rectangle will be added to the figure's current selection (if it is not already selected).</li>
 * </ul>
 * </p>
 * 
 * <p>Directly under the scroll pane containing the node tree canvas is the node properties editor panel. This is a
 * container for many different node-specific editors ({@link FGNEditor}), but only one is installed at at time -- the
 * one applicable to the currently selected node in the figure on display in <code>FigNodeTree</code>. Each time the
 * identity of the selected node changes, the relevant editor is initialized with the node's current set of properties
 * and installed in the editor panel, replacing the previous editor. A "title bar" of sorts appears along the bottom 
 * edge of the panel, showing a 32x32-pixel version of the node-specific icon and the title "[Node] Properties", where 
 * [Node] is replaced with the particular node type name ("Figure", "Graph", and so on).</p>
 * 
 * <p>Again, when a graph component node is selected, the installed node editor will display the properties of the 
 * parent graph: {@link FGNGraphEditor} includes separate tabs for viewing/editing the properties of the graph node 
 * itself, each of its component axes, the automated legend, and grid.</p>
 * 
 * <p>If multiple nodes are currently selected in the figure, the node properties editor panel is hidden altogether --
 * since it is not possible to edit the properties of multiple nodes at once. Operations on multi-node selections are
 * limited to delete, copy, move-by-drag, paste style set, and aligning the nodes in the selection.</p>
 * 
 * <p>The node tree canvas, node properties editor, and the node-specific editors have been designed and laid out to
 * occupy a <b>fixed</b> width less than 400 pixels. <code>FigNodeTree</code> should be docked to the left or right edge
 * of its container using a layout manager that restricts it to the fixed width but lets it expand vertically.</p>
 * 
 * <p><i>A note about keyboard focus and "hot keys"</i>. The graphic node operations Cut/Copy/Paste/Del are tied to 
 * keyboard accelerators, or "hot keys", through the application's "Edit" menu. However, these are standard edit 
 * operations for text components, so their "hot keys" are consumed by a text component when that text component has the
 * keyboard focus. This can be confusing to the user, if he has put the focus on a text component and then uses one of 
 * the "hot keys" to cut/copy/delete the current node selection. To make things clearer, the node tree canvas renders 
 * the node selection highlight in translucent gray when it does not have the keybaord focus, and in translucent steel 
 * blue when it does. The keyboard focus is only transferred to a widget in the node properties editor if the user 
 * actually interacts with that widget. Furthermore, whenever the identity of the selected node(s) in the displayed 
 * figure changes, the keyboard focus is returned to node tree canvas.</p>
 * 
 * @author sruffner
 */
public class FigNodeTree extends JPanel implements MouseListener, MouseMotionListener, FGModelListener, FocusListener
{
   /** Construct the view of a <i>DataNav</i> figure's graphic node tree. */
   public FigNodeTree()
   {
      canvas = new NodeTreeCanvas();
      canvas.addMouseListener(this);
      canvas.addFocusListener(this);

      // to implement drag-to-select and to auto-scroll when dragging outside the canvas
      canvas.setAutoscrolls(true);
      canvas.addMouseMotionListener(this);

      scroller = new JScrollPane(canvas);
      scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
      scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      
      // scrolling viewport has same background color as node tree canvas so that we don't see it when canvas happens
      // to be smaller than the visible viewport area.
      scroller.getViewport().setBackground(canvas.BKGCOLOR);
      
      // so we catch mouse-click events that are in the scrolling viewport but outside the canvas, which may not fill it
      scroller.getViewport().addMouseListener(this);

      propEditor = new NodeEditor();

      setLayout(new BorderLayout());
      add(scroller, BorderLayout.CENTER);
      add(propEditor, BorderLayout.SOUTH);
      
      // set minimum size to ensure it is large enough to accommodate the node tree canvas's preferred scrollable
      // viewport size, plus room for the scroll bars, plus the fixed width and minimum height of node property editor.
      Dimension d = canvas.getPreferredScrollableViewportSize();
      Dimension dScroll = scroller.getVerticalScrollBar().getMinimumSize();
      d.width += dScroll.width + 2;
      dScroll = scroller.getHorizontalScrollBar().getMinimumSize();
      d.height += dScroll.height + 2;
      Dimension d2 = propEditor.getMaximumSize();
      if(d.width < d2.width) d.width = d2.width; 
      d.height += d2.height;
      
      setMinimumSize(d);
   }
   
   /** 
    * Overridden to set fixed width matching that of the embedded node property editor panel -- so that width does not
    * change when the panel is hidden (during multi-node selection).
    */
   @Override public Dimension getMinimumSize()
   {
      Dimension sz = super.getMinimumSize();
      sz.width = propEditor.fixedWidth;
      return(sz);
   }

   /** 
    * Overridden to set fixed width matching that of the embedded node property editor panel -- so that width does not
    * change when the panel is hidden (during multi-node selection).
    */
   @Override public Dimension getMaximumSize()
   {
      Dimension sz = super.getMaximumSize();
      sz.width = propEditor.fixedWidth;
      return(sz);
   }

   /** 
    * Overridden to set fixed width matching that of the embedded node property editor panel -- so that width does not
    * change when the panel is hidden (during multi-node selection).
    */
   @Override public Dimension getPreferredSize()
   {
      Dimension sz = super.getPreferredSize();
      sz.width = propEditor.fixedWidth;
      return(sz);
   }

   /**
    * Is the figure node tree canvas the current keyboard focus owner? <code>FigNodeTree</code> is a composite panel
    * that includes the figure tree and the node property editor which, in turn, contains a host of focusable widgets.
    * The {@link FigComposer} recognizes several global hot keys which should only be active when the focus is actually 
    * on the node tree canvas, so it calls this method to check.
    * @return True if node tree canvas is the current keyboard focus owner.
    */
   boolean isFocusOnNodeTree() { return(canvas.isFocusOwner()); }
   
   /**
    * Request the keyboard focus be transferred to the node tree canvas. This ensures the focus is not on a text 
    * component widget, so that the standard cut/copy/paste keyboard shortcuts will apply to the current focus node.
    * Also see {@link #isFocusOnNodeTree()}.
    */
   void putFocusOnNodeTree() { canvas.requestFocusInWindow(); }
   
   /**
    * Load and display the node tree for the specified figure graphic.
    * @param fgm The figure to be loaded into the figure node tree panel. If null, the panel will be empty.
    */
   public void loadFigure(FGraphicModel fgm)
   {
      if(fig != null) fig.removeListener(this);
      fig = fgm;
      if(fig != null) fig.addListener(this);
      
      buildNodeTree();
      if(currSelection.isEmpty()) anchor = null;
      else anchor = reachableNodes.get(currSelection.get(0)).fgn;
      
      propEditor.loadSelectedNode();
      canvas.requestFocusInWindow();  // initially put keyboard focus in node tree canvas
   }

   /**
    * Get the rectangle bounding the text string in the row within the figure node tree canvas that corresponds to the 
    * specified graphic node. If the node is not currently reachable, the rectangle is undefined; else, the rectangle is
    * supplied in global screen coordinates. 
    * 
    * <p>The height of the rectangle is that of a single row in the node tree canvas. The rectangle will normally span
    * the row starting from the right edge of the node icon, thus excluding the indent region and only covering the
    * portion of full row rectangle that lies over the text label. However, if the indent region is wider than half the
    * width of the canvas, the indent region is ignored and the full-width row rectangle is returned.</p>
    * 
    * @param n A graphic node in the model currently loaded into the figure node tree.
    * @return The rectangle bounding the text label in the tree canvas row corresponding to that node, expressed in 
    * screen coordinates. If node is not reachable or not present in the current graphic model, then null is returned.
    */
   Rectangle getTextRectangleForNodeRow(FGraphicNode n)
   {
      // find index of row corresponding to the node specified
      int idx = -1;
      for(int i=0; i<reachableNodes.size(); i++) if(reachableNodes.get(i).fgn == n)
      {
         idx = i;
         break;
      }
      if(idx < 0) return(null);
      
      // compute indent from left edge of canvas to start of text string. Ignore indent if it's too large.
      int rh = NodeTreeCanvas.ROWHT;
      int indent = reachableNodes.get(idx).indentLevel * (rh - 2) + 2 + rh;
      if(indent > canvas.getWidth() / 2) indent = 0;
      
      Point pUL = new Point(0, idx*NodeTreeCanvas.ROWHT);
      SwingUtilities.convertPointToScreen(pUL, canvas);
      
      return(new Rectangle(pUL.x + indent, pUL.y, getWidth()-indent, rh));
   }
   
   /**
    * Change the selection from the current selected node to the next or previous reachable node in the figure node 
    * tree. A node is "reachable" if all of its ancestors in the node tree are currently expanded. <i>If more than one 
    * node is currently selected in the figure, then calling this method will restore the selection state to a single 
    * node -- the first node in the current selection (unless the <b>extendSel</b> flag is set).</i>
    * 
    * @param next If true, move selection to the next reachable node in the tree; no action taken if the current 
    * selected node is the last one. If false, shift the selection to the previous reachable node; again, no action 
    * taken if the current highlighted node is the root figure node.
    * @param extendSel If unset, this method will always restore the selection to a single node, regardless the current 
    * selection state. However, if the flag is set, this method EXTENDS the selection from the current anchor node to 
    * the reachable node that is before or after the currently selected node that is furthest from the anchor. This 
    * provides a programmatic means of selecting a range of rows in the node tree.
    */
   public void changeSelection(boolean next, boolean extendSel)
   {
      if(fig == null || reachableNodes.isEmpty() || currSelection.isEmpty()) return;
      
      if(!extendSel)
      {
         int currIdx = currSelection.get(0);
         if(currSelection.size() > 1)
            fig.setSelectedNode(reachableNodes.get(currIdx).fgn);
         if(next && (currIdx < reachableNodes.size()-1))
            fig.setSelectedNode(reachableNodes.get(currIdx+1).fgn);
         else if((!next) && (currIdx > 0))
            fig.setSelectedNode(reachableNodes.get(currIdx-1).fgn);
      }
      else
      {
         // current start of range is the anchor node, or the first node in the current selection.
         int start = -1;
         if(anchor != null) for(Integer idx : currSelection) if(anchor == reachableNodes.get(idx).fgn)
         {
            start = idx;
            break;
         }
         if(start == -1)
         {
            start = currSelection.get(0);
            anchor = reachableNodes.get(start).fgn;
         }
         
         // end of range is the node before or after the reachable node in the row furthest away from the anchor
         int diff = -1;
         int end = -1;
         for(Integer idx : currSelection)
         {
            int d = Math.abs(idx - start);
            if(d > diff)
            {
               diff = d;
               end = idx;
            }
         }
         end += next ? 1 : -1;
         if(end < 0) end = 0;
         else if(end >= reachableNodes.size()) end = reachableNodes.size()-1;
         
         doRangeSelect(end);
      }
   }
   
   /**
    * The internal representation of the node tree is reconstructed whenever nodes are added or removed from the current
    * figure. If the model's selected nodes list has changed, the affected nodes are repainted accordingly. Finally, if 
    * a node's definition changes, then its title may have changed, so we repaint the corresponding row in the rendered
    * node tree (if it is currently visible within the scrolling viewport). In addition, the title change could impact
    * the horizontal scrolling extent (if the modified node happened to be the longest row), so we layout the canvas
    * again to ensure the horizontal scroll extent is updated accordingly.
    */
   @Override public void modelChanged(FGraphicModel model, FGraphicNode n, Change change)
   {
      if(model == null || model != fig) return;
      if(change == Change.RELOAD)
      {
         buildNodeTree();
         propEditor.loadSelectedNode();
         canvas.requestFocusInWindow();
      }
      else if(change == Change.INSERT_OR_REMOVE)
      {
         // this event indicates that the specified graphic node's child list has changed. Rather than rebuilding the
         // node tree from scratch, we merely update the list of node entries directly under the entry for the specified
         // node -- while preserving the expand state of that node and any child nodes that remain after the update.
         // Since this update could affect the size or order of the reachable node list, we rebuild that list, and
         // layout and repaint the entire node tree canvas. The current selection could also be affected.
         treeRoot.updateChildEntriesFor(n);
         
         refreshSelectedNodes();
         for(FGraphicNode sel : selectedNodes) treeRoot.ensureNodeIsReachable(sel);

         reachableNodes.clear();
         treeRoot.getReachableEntries(reachableNodes);

         currSelection.clear();
         for(int i=0; i<reachableNodes.size(); i++) if(selectedNodes.contains(reachableNodes.get(i).fgn))
            currSelection.add(i);
         if(!selectedNodes.contains(anchor))
            anchor = currSelection.isEmpty() ? null : reachableNodes.get(currSelection.get(0)).fgn;
         
         canvas.layoutCanvas();
         propEditor.loadSelectedNode();
         canvas.repaint();
         canvas.requestFocusInWindow();
         SwingUtilities.invokeLater(new EnsureSelVis());
      }
      else if(change == Change.SELECTION)
      {
         updateCurrentSelection();
         propEditor.loadSelectedNode();
         canvas.requestFocusInWindow();
      }
      else if(change == Change.DEFINE)
      {
         if(n == null) return;
         canvas.layoutCanvas();
         for(int i=0; i<reachableNodes.size(); i++) if(reachableNodes.get(i).fgn == n)
         {
            canvas.repaintRow(i);
            break;
         }
         
         propEditor.reloadOnNodeChanged(n);
      }
   }

   /**
    * If the user clicks anywhere inside the node tree's scroll pane, put the keyboard focus on the tree canvas (in
    * case it was on a component in the node property editor). [Note: we listen for mouse-click events both on the 
    * node tree canvas itself and the scroll pane viewport "underneath" it, in case the canvas does not fill the
    * visible viewport area.] The focus should already be there b/c we do the same on mouse-down -- but just in case.
    * 
    * <p>If the user clicks on a node in the tree, then update the selection state as follows:
    * <ul>
    * <li>If no modifier key is down, then the clicked node becomes the single selected node in the figure, replacing 
    * any current selection.</li>
    * <li>If the Ctrl key (Command key in OSX) is down, the clicked node's selection state changes. If the node was not
    * selected, it is added to the selection (and becomes the focus node as the last node added). If it was already 
    * selected but did not have the focus, it is made the focus node of the selection (outlined in blue). Finally, if it
    * was already the selection focus, then it is de-selected (and some other selected node gets the focus highlight) --
    * with the exception that a singly-selected node cannot be de-selected. This is one mechanism by which the user can 
    * select multiple nodes in the figure, AND change the identity of the focus node.</li>
    * <li>If the Shift key is down, a range of nodes is selected, between the clicked node and the current anchor node
    * for range selection. Note that if both the Shift and Ctrl/Command keys are down, the latter takes precedence and
    * the Shift key is ignored.</li>
    * </ul>
    * </p>
    *
    * <p>If the user clicks on the collapse-expand toggle icon for a tree node, then toggle that node's expanded state.
    * If the node is collapsed and any of its descendants are currently selected, those nodes are automatically 
    * de-selected and the collapsed node is added to the figure's selection list.</p>
    * 
    * <p>Finally, if only a single node is selected and the user "double-clicks" on that node, raise a compact pop-up
    * tool that lets the user edit the node's <i>title</i> attribute "in place".</p>
    */
   @Override public void mouseClicked(MouseEvent e)
   {
      canvas.requestFocusInWindow();
      if(e.getSource() != canvas) return;
      if(fig == null) return;
      int idxClick = canvas.getIndexOfRowUnderPoint(e.getPoint());
      if(idxClick < 0) return;
      FGraphicNode clickNode = reachableNodes.get(idxClick).fgn;
      
      int ctrlCmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
      
      if(e.getClickCount() == 1)
      {
         boolean isTog = canvas.isExpandToggleIconUnder(e.getPoint());
         if(isTog) 
            toggleExpandedState(idxClick);
         else if((e.getModifiersEx() & ctrlCmdMask) == ctrlCmdMask)
         {
            // toggle the select state of the node clicked
            if(!fig.isSelectedNode(clickNode))
            {
               fig.reviseSelection(clickNode, null);
               anchor = clickNode;
            }
            else if(clickNode != fig.getFocusForSelection())
            {
               fig.setFocusForSelection(clickNode);
            }
            else
               fig.reviseSelection(null, clickNode);
         }
         else if((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK)
         {
            doRangeSelect(idxClick);
         }
         else
         {
            fig.setSelectedNode(clickNode);
            anchor = clickNode;
         }
      }
      else if(e.getClickCount() == 2)
      {
         // allow user to edit title of any node in node tree using a compact "pop-up" tool, but not if multiple nodes
         // are selected.
         if((e.getModifiersEx() & ctrlCmdMask) == 0 && (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == 0 &&
               e.getButton() == MouseEvent.BUTTON1 && !fig.isMultiNodeSelection())
         {
            TitlePopupDlg.raiseEditTitlePopup(clickNode, this);
         }   
      }
   }

   /**
    * Helper method replaces the current selection in the figure with the contiguous range of reachable nodes in the
    * node tree between the node that currently anchors a range selection, and the node specified.
    * @param idx Index (into reachable nodes array) of the node that terminates the range selection. Assumed valid.
    */
   private void doRangeSelect(int idx)
   {
      if(currSelection.isEmpty())
      {
         fig.setSelectedNode(reachableNodes.get(idx).fgn);
         anchor = reachableNodes.get(idx).fgn;
         return;
      }
      
      int idxAnchor = -1;
      if(anchor == null)
      {
         idxAnchor = currSelection.get(0);
         anchor = reachableNodes.get(idxAnchor).fgn;
      }
      else for(int i=0; i<reachableNodes.size(); i++) if(reachableNodes.get(i).fgn == anchor)
      {
         idxAnchor = i;
         break;
      }
      
      // replace current selection with range of nodes between anchor and the node clicked. Keep that portion of the
      // current selection that is in the range identified.
      List<FGraphicNode> addSel = new ArrayList<>();
      List<FGraphicNode> rmvSel = new ArrayList<>();
      
      int start = Math.min(idxAnchor, idx);
      int end = Math.max(idxAnchor, idx);
      for(int i=start; i<=end; i++) addSel.add(reachableNodes.get(i).fgn);
      for(Integer sel : currSelection) if(sel < start || sel > end) rmvSel.add(reachableNodes.get(sel).fgn);
      
      fig.reviseSelection(addSel, rmvSel);
   }
   
   /** Initiate a drag-by-select gesture if the user mouses down within the node tree canvas. */
   @Override public void mousePressed(MouseEvent e) 
   {
      // put focus on node tree canvas as soon as user mouses down on it or its scroll viewport container.
      canvas.requestFocusInWindow();
      
      if(e.getSource() == canvas)
      {
         dragOrigin = new Point(e.getPoint());
         lastDragPt = new Point(e.getPoint());
      }
   }
   
   /** 
    * Complete a drag-by-select gesture when user releases mouse. All nodes under the drag rectangle are selected. If
    * the Ctrl key (Command key in OSX) is depressed upon mouse release, the nodes are ADDED to the current selection;
    * otherwise, they REPLACE the current selection.
    */
   @Override public void mouseReleased(MouseEvent e) 
   {
      if(e.getSource() == canvas)
      {
         dragOrigin = null;
         lastDragPt = null;
         if(dragSelectR.isEmpty()) return;  // this will be the case on a mouse click!
         
         // clear any drag selection rectangle
         dirtyRect.setBounds(dragSelectR);
         dirtyRect.grow(3,3);
         canvas.paintImmediately(dirtyRect);
         
         // select all nodes under the drag rectangle, replacing the figure's current selection, or adding them to the
         // current selection if the Ctrl key is down.
         int iFirst = canvas.getIndexOfRowUnderPoint(dragSelectR.x, dragSelectR.y);
         int iLast = canvas.getIndexOfRowUnderPoint(dragSelectR.x + dragSelectR.width, 
               dragSelectR.y + dragSelectR.height);
         dragSelectR.setBounds(0,0,0,0);
         
         if(iFirst >= 0 && iLast >= 0)
         {
            if(iFirst > iLast) {int tmp = iFirst; iFirst = iLast; iLast = tmp; }
            
            List<FGraphicNode> select = new ArrayList<>();
            for(int i=iFirst; i<=iLast; i++) select.add(reachableNodes.get(i).fgn);
            
            int ctrlCmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            boolean replaceSel = ((e.getModifiersEx() & ctrlCmdMask) != ctrlCmdMask);
            fig.reviseSelection(select, replaceSel ? fig.getSelectedNodes(null) : null);
         }
      }
   }
   @Override public void mouseEntered(MouseEvent e) {}
   @Override public void mouseExited(MouseEvent e) {}
   
   /** 
    * Animate the drag-to-select mouse gesture. This simply updates the rectangular outline defined by the drag origin
    * and the current location of the mouse. The mouse must move at least 3 pixels away from the last recorded drag
    * location before an update will occur. Also, if the updated drag location is within 5 pixels of the drag origin,
    * no outline is drawn.
    */
   @Override public void mouseDragged(MouseEvent e)
   {
      if(e.getSource() == canvas)
      {
         Point p = e.getPoint();
         canvas.scrollRectToVisible(new Rectangle(p.x, p.y, 1, 1));
        
         if(p.distance(lastDragPt) > 3)
         {
            dirtyRect.setBounds(dragSelectR);
            lastDragPt = p;
            
            // determine UL corner and dimensions of selection rectangle given drag origin and current drag location.
            // Selection rectangle must have a minimum diagonal of 5 pixels.
            if(dragOrigin.distance(lastDragPt) < 5)
               dragSelectR.setBounds(dragOrigin.x, dragOrigin.y, 0, 0);
            else
            {
               Point scrollPos = scroller.getViewport().getViewPosition();
               int w = Math.min(canvas.getWidth(), scrollPos.x + scroller.getViewport().getWidth());
               int h = Math.min(canvas.getHeight(), scrollPos.y + scroller.getViewport().getHeight());
               
               int xUL = Math.max(1, Math.min(dragOrigin.x, lastDragPt.x));
               int yUL = Math.max(1,  Math.min(dragOrigin.y, lastDragPt.y));
               int xBR = Math.min(w-2, Math.max(dragOrigin.x, lastDragPt.x));
               int yBR = Math.min(h-2, Math.max(dragOrigin.y, lastDragPt.y));
               
               dragSelectR.setBounds(xUL, yUL, xBR-xUL, yBR-yUL);
            }
            
            // update animated seletion rectangle on node tree canvas
            Rectangle.union(dragSelectR, dirtyRect, dirtyRect);
            if(!dirtyRect.isEmpty())
            {
               dirtyRect.grow(3, 3);
               canvas.paintImmediately(dirtyRect);
            }
         }
      }
   }

   @Override public void mouseMoved(MouseEvent e) {}
   
   
   /**
    * All selected nodes in the tree are repainted when the node tree canvas either gains or loses the keyboard focus, 
    * since the selection highlight color serves as a indication of whether or not the canvas owns the focus.
    */
   @Override public void focusGained(FocusEvent e) { canvas.repaintCurrentSelection(); }

   /**
    * All selected nodes in the tree are repainted when the node tree canvas either gains or loses the keyboard focus, 
    * since the selection highlight color serves as a indication of whether or not the canvas owns the focus.
    */
   @Override public void focusLost(FocusEvent e) { canvas.repaintCurrentSelection(); }

   
   /** The current figure. If non-null, this figure's node tree is displayed herein. */
   private FGraphicModel fig = null;
   
   /** 
    * The list of nodes currently selected in the current figure. The last node in this list is the focus node for
    * the selection. It is highlighted with a blue outline in addition to the translucent steel blue fill applied to
    * the other nodes in the selection list.
    */
   private final List<FGraphicNode> selectedNodes = new ArrayList<>();
   
   /** The root of the figure node tree. When non-null, it corresponds to the current figure's root node. */
   private NodeEntry treeRoot = null;

   /** The list of reachable nodes in the figure node tree. A node is "reachable" if its ancestors are expanded. */
   private final List<NodeEntry> reachableNodes = new ArrayList<>();
   
   /** 
    * Indices identifying the location of all currently selected nodes in the reachable nodes array. If a node is 
    * selected, it MUST be reachable. This should always contain at least one element, unless no figure is loaded. 
    */
   private final List<Integer> currSelection = new ArrayList<>();
   
   /** 
    * The anchor node for selecting a range of consecutive reachable nodes in response to a mouse click with the Shift
    * key held down. The anchor node is updated whenever a node is clicked without the Shift key, or the selection
    * changes such that the anchor node is no longer selected.
    */
   private FGraphicNode anchor = null;
   
   /** Mouse location when a drag-to-select gesture started on node tree canvas. Null when not in use. */
   private Point dragOrigin = null;
   /** Mouse location for the last mouse-dragged event on node tree canvas. Null when not in use. */
   private Point lastDragPt = null;
   /** The rectangular area selected by a drag-to-select gesture in progress on node tree canvas. */
   private final Rectangle dragSelectR = new Rectangle();
   /** Dirty area needing repaint during an animated drag-to-select gesture on node tree canvas. */
   private final Rectangle dirtyRect = new Rectangle();
   
   /** The custom-painted component on which the node tree is rendered. */
   private final NodeTreeCanvas canvas;
   
   /** The scroll pane container for the rendered node tree. */
   private JScrollPane scroller = null;
   
   /** 
    * When a single node is selected, its properties are displayed and edited in this panel. If multiple nodes are
    * selected, this panel is hidden. 
    */
   private NodeEditor propEditor = null;
   
   /**
    * Rebuild the node tree for the current figure, with all immediate descendants of the root node initialized in the 
    * collapsed state. Next expand nodes as needed to ensure that all nodes currently selected in the figure are 
    * reachable, and initialize the list of currently reachable nodes accordingly. Invoke this method whenever a new 
    * figure is loaded.
    */
   private void buildNodeTree()
   {
      reachableNodes.clear();
      if(treeRoot != null)
      {
         treeRoot.clear();
         treeRoot = null;
      }
      currSelection.clear();
      if(fig == null)  return;
      
      treeRoot = new NodeEntry(fig.getRoot(), 0);
      
      // init the current selection. Ensure all selected nodes are reachable.
      refreshSelectedNodes();
      for(FGraphicNode n : selectedNodes) treeRoot.ensureNodeIsReachable(n);
      treeRoot.getReachableEntries(reachableNodes);
      for(int i=0; i<reachableNodes.size(); i++) if(selectedNodes.contains(reachableNodes.get(i).fgn))
         currSelection.add(i);
      
      canvas.layoutCanvas();
      canvas.repaint();
      SwingUtilities.invokeLater(new EnsureSelVis());
   }

   /**
    * Update the list of selected nodes in the current figure that are also exposed in this node tree. The node tree 
    * excludes all component nodes -- yet a graph axis (and its child tick sets) and legend are component nodes that can
    * be selected in the figure.
    */
   private void refreshSelectedNodes()
   {
      if(fig == null) selectedNodes.clear();
      else fig.getSelectedNodes(selectedNodes);

      int i=0;
      while(i<selectedNodes.size())
      {
         FGraphicNode n = selectedNodes.get(i);
         
         // replace component node or tick set node with the nearest ancestor we include in node tree. Tick sets and
         // certain component nodes can be selected in the figure but are never shown in the node tree -- because the
         // node tree does not show component nodes.
         FGraphicNode ancestor = null;
         if(n.isComponentNode()) ancestor = n.getParent();
         else if(n.getNodeType() == FGNodeType.TICKS || n.getNodeType() == FGNodeType.TICKS3D) 
            ancestor = n.getParent().getParent();
         if(ancestor != null)
         {
            if(selectedNodes.contains(ancestor)) 
            {
               selectedNodes.remove(i);
               continue;
            }
            else
               selectedNodes.set(i, ancestor);
         }
         ++i;
      }
   }
   
   /** 
    * Update the current selection on the the node tree canvas.
    * 
    * <p>Invoke this method upon receiving a selection change event from the figure model. It will do as little work as
    * possible to update the node tree canvas to reflect the current selection. If any selected node is unreachable, it
    * must expand one or more ancestors and re-layout the canvas accordingly. Otherwise, it will only repaint those
    * rows corresponding to nodes that were just de-selected or selected.</p>
    */
   private void updateCurrentSelection()
   {
      currSelection.clear();
      boolean needLayout = false;
      refreshSelectedNodes();
      for(FGraphicNode sel : selectedNodes) if(!treeRoot.isNodeReachable(sel))
      {
         needLayout = true;
         treeRoot.ensureNodeIsReachable(sel);
      }
      
      // updating the selection required expanding one or more nodes, so we have to rebuild the list of reachable nodes
      // and layout the node tree canvas accordingly
      if(needLayout)
      {
         reachableNodes.clear();
         treeRoot.getReachableEntries(reachableNodes);
         canvas.layoutCanvas();
      }
      
      for(int i=0; i<reachableNodes.size(); i++) if(selectedNodes.contains(reachableNodes.get(i).fgn))
         currSelection.add(i);
      
      canvas.repaint();
      
      // update the anchor for contiguous range-select if the current anchor is no longer selected
      if(!selectedNodes.contains(anchor)) 
         anchor = currSelection.isEmpty() ? null : reachableNodes.get(currSelection.get(0)).fgn;
      
      SwingUtilities.invokeLater(new EnsureSelVis());
   }
   
   /**
    * Toggle the collapsed/expanded state of a specified node in the list of reachable nodes in the figure node tree.
    * Note that doing so will shrink or expand the list of reachable nodes. The method rebuilds the list after changing
    * the node's state, then lays out and paints the node tree canvas. 
    * <p>If the node about to be collapsed contains one or more selected nodes, then the parent node is added to the
    * selection list if it is not already selected, and the selected descendants are removed.</p>
    * 
    * @param idx Index of the reachable node to be collapsed or expanded. If this is invalid or identifies a childless 
    * node, then no action is taken.
    */
   private void toggleExpandedState(int idx)
   {
      if(idx < 0 || idx >= reachableNodes.size()) return;
      NodeEntry entry = reachableNodes.get(idx);
      if(!entry.canCollapse()) return;
      
      // if we're collapsing a node that contains any selected nodes, de-select those nodes and make sure the collapsed
      // node is selected.
      if(entry.expanded)
      {
         List<FGraphicNode> rmvSel = new ArrayList<>();
         for(Integer sel : currSelection) if(reachableNodes.get(sel).fgn.isDescendantOf(entry.fgn))
            rmvSel.add(reachableNodes.get(sel).fgn);
         if(!rmvSel.isEmpty())
         {
            List<FGraphicNode> addSel = null;
            if(!fig.isSelectedNode(entry.fgn))
            {
               addSel = new ArrayList<>();
               addSel.add(entry.fgn);
            }
            fig.reviseSelection(addSel, rmvSel);
         }
      }
      
      // now toggle the expand/collapsed state and refresh the tree accordingly. Note that if you expand a node that
      // is earlier in the reachable node list than the current selection, you could push the selection highlight out of
      // the visible viewport. But user elected to do this by expanding that node; we do NOT scroll it back into view.
      entry.expanded = !entry.expanded;
      reachableNodes.clear();
      currSelection.clear();
      refreshSelectedNodes();
      for(FGraphicNode n : selectedNodes) treeRoot.ensureNodeIsReachable(n);
      treeRoot.getReachableEntries(reachableNodes);
      for(int i=0; i<reachableNodes.size(); i++) if(selectedNodes.contains(reachableNodes.get(i).fgn))
         currSelection.add(i);

      canvas.layoutCanvas();
      canvas.repaint();
   }
   
   /** 
    * A simple runnable that we can post on the Swing event dispatch thread to ensure the current selection is visible
    * after a re-layout of the node tree canvas.
    * @author sruffner
    */
   private class EnsureSelVis implements Runnable
   {
      @Override public void run() { canvas.ensureSelectionVisible(); }
   }
   
   /**
    * An entry representing a single graphic node in the current figure's node hierarchy. It includes a reference to 
    * the graphic node itself, a list of entries representing the node's children, the node's indent level in the tree
    * structure, and its collapsed/expanded state.
    * @author sruffner
    */
   private class NodeEntry
   {
      /** 
       * Construct an entry for the specified graphic node. It recursively constructs entries for each of that node's
       * child nodes (if there are any). If it has any children, the node is initialized in the collapsed state (except
       * for the root figure node which is always expanded and may not be collapsed).
       * 
       * @param fgn The figure graphic node.
       * @param indentLevel The node's indent level in the tree traversal list.
       */
      NodeEntry(FGraphicNode fgn, int indentLevel)
      {
         assert(fgn != null && indentLevel >= 0);
         this.fgn = fgn;
         this.indentLevel = indentLevel;
         
         if(!fgn.isLeaf())
         {
            kids = new ArrayList<>();
            for(int i=0; i<fgn.getChildCount(); i++)
               kids.add(new NodeEntry(fgn.getChildAt(i), indentLevel+1));
         }

         // the root figure node is ALWAYS expanded (even if it is empty)
         expanded = fgn.getNodeType() == FGNodeType.FIGURE;
      }
      
      /** 
       * Get the icon image for this node entry, based on the figure graphic node's type. 
       * @return A 22x22 image for the node entry's icon.
       */
      Image getIconImage()
      {
         ImageIcon icon;
         switch(fgn.getNodeType())
         {
         case FIGURE :   icon = FCIcons.V4_FIGURE_22; break; 
         case LABEL :    icon = FCIcons.V4_LABEL_22; break;
         case TEXTBOX :  icon = FCIcons.V4_TEXTBOX_22; break;
         case IMAGE :    icon = FCIcons.V4_IMAGE_22; break;
         case SHAPE :    icon = FCIcons.V4_SHAPE_22; break;
         case LINE :     icon = FCIcons.V4_LINE_22; break;
         case GRAPH :    icon = FCIcons.V4_GRAPH_22; break;
         case PGRAPH:    icon = FCIcons.V4_POLARG_22; break;
         case CALIB :    icon = FCIcons.V4_CALIB_22; break;
         case FUNCTION : icon = FCIcons.V4_FUNCTION_22; break;
         case TRACE :    icon = FCIcons.V4_TRACE_22; break;
         case RASTER :   icon = FCIcons.V4_RASTER_22; break;
         case CONTOUR :  icon = FCIcons.V4_HEATMAP_22; break;
         case BOX:       icon = FCIcons.V4_BOX_22; break;
         case BAR :      icon = FCIcons.V4_BAR_22; break;
         case AREA :     icon = FCIcons.V4_AREA_22; break;
         case SCATTER:   icon = FCIcons.V4_SCATTER_22; break;
         case PIE :      icon = FCIcons.V4_PIE_22; break;
         case GRAPH3D :  icon = FCIcons.V4_GRAPH3D_22; break;
         case SCATTER3D: icon = FCIcons.V4_SCATTER3D_22; break;
         case SURFACE:   icon = FCIcons.V4_SURFACE_22; break;
         default:        icon = FCIcons.V4_BROKEN; break;
         }
         return(icon != null ? icon.getImage() : null);
      }
      
      /** 
       * Get the label for this node entry.
       * 
       * @return The entry's rendered label.
       */
      AttributedString getLabel()
      {
         AttributedString as = fgn.getUILabel();
         Font f = canvas.getFont();
         as.addAttribute(TextAttribute.FAMILY, f.getFamily());
         as.addAttribute(TextAttribute.SIZE, f.getSize());
         return as;
      }
      
      /**
       * Can this entry in the figure node tree be collapsed (or expanded)? Any node having at least one child node may
       * be collapsed or expanded, with one exception: the root figure node is <i>always</i> in the expanded state.
       * @return True if this node entry does not correspond to the root figure node and contains at least one child.
       */
      boolean canCollapse()
      {
         return(fgn.getNodeType() != FGNodeType.FIGURE && kids != null && !kids.isEmpty());
      }
      
      /**
       * Accumulate a list of reachable node entries in the portion of the figure node tree that is under this node 
       * entry. The node entry itself is appended to the list and, if it is NOT collapsed and has any children, this 
       * method is invoked recursively on each child node entry.
       * <p>When this method is invoked on the root figure node, the accumulated list contains ALL reachable entries 
       * given the current state of the figure node tree.</p>
       * @param reachable The list of reachable node entries accumulated thus far. Must not be null. Additional entries 
       * are appended to this list as described.
       */
      void getReachableEntries(List<NodeEntry> reachable)
      {
         assert(reachable != null);
         reachable.add(this);
         if(expanded && kids != null && !kids.isEmpty())
         {
            for(NodeEntry kid : kids) kid.getReachableEntries(reachable);
         }
      }
      
      /**
       * Is the specified figure graphic node a reachable descendant of the node represented by this entry in the figure
       * node tree? It is reachable if this node entry is expanded and (recursively) every intervening ancestor of the
       * target node is also expanded. Call this method from the root node entry to check whether or not the target node
       * is currently reachable in the node tree.
       * 
       * @param fgnTgt A target figure graphic node in the figure node tree.
       * @return True if target node is a reachable descendant of the node represented by this enty; false otherwise.
       */
      boolean isNodeReachable(FGraphicNode fgnTgt)
      {
         if(!(expanded && fgnTgt.isDescendantOf(fgn))) return(false);
         for(NodeEntry kid : kids)
            if(kid.fgn == fgnTgt || kid.isNodeReachable(fgnTgt))
               return (true);
         return(false);
      }
      
      /**
       * If the specified figure graphic node is a descendant of the node represented by this entry in the figure node
       * tree, ensure it is reachable by expanding this node entry and (recursively) every intervening ancestor of the 
       * target node. Call this method from the root node entry to ensure the target node is reachable in the node tree.
       * 
       * @param fgnTgt A target figure graphic node in the figure node tree.
       * @return True if target node is a descendant of the node represented by this entry, false otherwise.
       */
      boolean ensureNodeIsReachable(FGraphicNode fgnTgt)
      {
         if(!fgnTgt.isDescendantOf(fgn)) return(false);
         expanded = true;
         for(NodeEntry kid : kids)
            if(kid.fgn == fgnTgt || kid.ensureNodeIsReachable(fgnTgt))
               break;
         return(true);
      }
      
      /**
       * Find the node tree entry in the subtree rooted at this entry that corresponds to the specified graphic node
       * (recursive operation). If that entry is found, refresh all of its child entries, while preserving the expanded
       * state of each child node entry present before and after the refresh. An entry for a new child node is created 
       * in the collapsed state.
       * @param parent The graphic node whose list of child node entries may have changed (node inserted or removed,
       * order of child nodes changed).
       */
      void updateChildEntriesFor(FGraphicNode parent)
      {
         if(fgn == parent)
         {
            if(fgn.isLeaf()) return;   // just in case, but this should never happen
            
            List<NodeEntry> updatedKids = new ArrayList<>();
            for(int i=0; i<fgn.getChildCount(); i++)
               updatedKids.add(new NodeEntry(fgn.getChildAt(i), indentLevel+1));

            // fix expand state for each kid that existed before and after the update
            for(NodeEntry entry : updatedKids)
            {
               int found = -1;
               for(int i=0; i<kids.size(); i++) if(entry.fgn == kids.get(i).fgn)
               {
                  found = i;
                  break;
               }
               if(found > -1)
               {
                  entry.expanded = kids.get(found).expanded;
                  kids.get(found).clear();
                  kids.remove(found);
               }
            }
            
            // replace old child list with the updated one.
            clear();
            kids = updatedKids;
         }
         else if(kids != null)
         {
            for(NodeEntry kid : kids) kid.updateChildEntriesFor(parent);
         }
      }

      /**
       * Clear all entries descendant of this node entry. Recursive. Intended to be called on the root of the figure
       * node tree when it is being discarded.
       */
      void clear()
      {
         if(kids != null)
         {
            for(NodeEntry kid : kids) kid.clear();
            kids.clear();
            kids = null;
         }
      }
      
      /** The figure graphic node represented by this entry. */
      private final FGraphicNode fgn;
      /** 
       * List of node entries corresponding to the children of the graphic node represented by this entry. Will be null
       * if the graphic node is a leaf (no child nodes permitted)
       */
      private List<NodeEntry> kids = null;
      /** The indent level for this entry. */
      private final int indentLevel;
      /** 
       * True if node is currently expanded, so that all of its children are displayed underneath it in the rendering
       * of the node tree. False if node's child list is empty, or if node is a leaf (no child nodes permitted).
       */
      private boolean expanded;
   }

   
   /**
    * This custom-painted component renders all reachable nodes in the current figure's node tree (a node is "reachable"
    * if each of its ancestor nodes are "expanded"). Each reachable node is rendered in a single row containing the 
    * node's type-specific icon followed by the node's title. The fixed row height is the node icon height + 4. An 
    * indent region of L*(rowHt-2) + 2 pixels separates the left edge of the node tree canvas from the left edge of the
    * node icon and indicates the node's level L in the node tree. The root figure node is the only node at level L=0;
    * all other nodes have L>0. For any non-leaf node with at least one child node, a collapse/expand toggle icon is
    * drawn in the indent region, 2 pixels left of the node icon's left edge. If a node is expanded, this icon is a
    * downward-pointing black arrow; if a node has child nodes but is collapsed, it is a rightward-pointing arrow. If 
    * the node is a leaf or has no child nodes, the expand/collapse toggle icon is not drawn. Also, there is no toggle 
    * icon for the root figure node, which is always in the expanded state.
    * 
    * @author sruffner
    */
   private class NodeTreeCanvas extends JPanel implements Scrollable
   {
      /** Construct the custom-painted canvas on which a figure's node tree is drawn. */
      NodeTreeCanvas()
      {
         hiliteRR = new RoundRectangle2D.Double(0, 0, 100, ROWHT, 8, 8);
         labelPainter = new SingleStringPainter();
         labelPainter.setAlignment(TextAlign.LEADING, TextAlign.CENTERED);
         labelPainter.setStyle(BasicPainterStyle.createBasicPainterStyle(
               BasicPainterStyle.getFontForPainter("Arial", GenericFont.SANSERIF, FontStyle.BOLD, 12), 
               2, null, Color.BLACK, Color.BLACK));
         
         int hgap = (EXPTOGRECTW-EXPTOGICONW) / 2;
         int vgap = (ROWHT-EXPTOGICONW) / 2;
         rightArrow = new GeneralPath();
         rightArrow.moveTo(hgap, vgap);
         rightArrow.lineTo(hgap+EXPTOGICONW, ROWHT/2f);
         rightArrow.lineTo(hgap, ROWHT-vgap);
         rightArrow.closePath();
         
         downArrow = new GeneralPath();
         downArrow.moveTo(hgap, vgap);
         downArrow.lineTo(hgap+EXPTOGICONW, vgap);
         downArrow.lineTo(EXPTOGRECTW/2f, ROWHT-vgap);
         downArrow.closePath();
      }

      /**
       * The preferred width of the node tree canvas is the width of the longest row among the list of reachable tree 
       * nodes, and the preferred height is the number of nodes in that list times the (fixed) row height. The 
       * dimensions are recalculated each time {@link #layoutCanvas()} is invoked.
       */
      @Override public Dimension getPreferredSize() { return(new Dimension(renderedW, renderedH)); }

      /** The scrolling viewport should at least accommodate {@link #MIN_VIS_SZ} pixels in width and height. */
      @Override public Dimension getPreferredScrollableViewportSize() { return(new Dimension(MIN_VIS_SZ, MIN_VIS_SZ)); }

      @Override public int getScrollableUnitIncrement(Rectangle rVisible, int ori, int dir) { return(ROWHT); }
      @Override public int getScrollableBlockIncrement(Rectangle rVisible, int ori, int dir)
      {
         return(ori==SwingConstants.HORIZONTAL ? ROWHT : 5*ROWHT);
      }

      @Override public boolean getScrollableTracksViewportWidth() { return(false); }
      @Override public boolean getScrollableTracksViewportHeight() { return(false); }

      @Override protected void paintComponent(Graphics g)
      {
         Graphics2D g2 = (Graphics2D) g;
         
         // we always want nice-looking renderings
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
         
         // we only want to do painting in the clip rectangle. It will be in canvas coordinates.
         g2.getClipBounds(clipR);
         
         // clear the region to be painted
         g2.setColor(Color.WHITE);
         g2.fill(clipR);
         
         // if there's nothing to paint, we're done!
         if(fig == null || reachableNodes.isEmpty()) return;
         
         // we paint only those rows that intersect clip rect
         int iFirst = clipR.y / ROWHT;
         if(iFirst < 0) iFirst = 0;
         int iLast = (clipR.y + clipR.height) / ROWHT;
         if(iLast >= reachableNodes.size()) iLast = reachableNodes.size() - 1;
         
         g2.translate(0, iFirst*ROWHT);
         
         for(int i=iFirst; i<=iLast; i++)
         {
            NodeEntry entry = reachableNodes.get(i);
            int indent = entry.indentLevel * (ROWHT - 2) + 2;  // to left edge of node's icon
            
            // if applicable, draw the collapse/expand toggle icon to reflect the node's current state. It is drawn
            // in the indent region, left of the node icon. If node is childless, don't draw the collapse/expand icon.
            // The root figure node is always expanded and may not be collapsed, so we don't drawn the icon (and there's
            // no room for it anyway).
            if(entry.canCollapse())
            {
               indent -= EXPTOGRECTW;
               g2.translate(indent, 0);
               g2.setColor(Color.BLACK);
               g2.fill(entry.expanded ? downArrow : rightArrow);
               g2.translate(-indent, 0);
               indent += EXPTOGRECTW;
            }
            g2.drawImage(entry.getIconImage(), indent, 2, null);
            labelPainter.setTextAndLocation(entry.getLabel(), indent+ROWHT, -ROWHT/2.0);
            g2.scale(1, -1);
            labelPainter.render(g2, null);
            g2.scale(1, -1);
           
            // render translucent rectangle over the icon and title of any node that is in the model's current selection
            // list. The rectangle is bluish if node tree canvas currently has the keyboard focus, else it is grayish.
            if(currSelection.contains(i))
            {
               boolean hasFocus = isFocusOwner();
               
               labelPainter.invalidateBounds();
               labelPainter.getBounds2D(titleBounds);
               int hiliteW = (int) (titleBounds.getWidth() + ROWHT + 6);
               hiliteRR.setFrame(indent - 2, 0, hiliteW, ROWHT);
               g2.setColor(hasFocus ? hiliteColor : hiliteColor_NoFocus);
               g2.fill(hiliteRR);
               
               // the focus node for the current selection is also outlined in blue to help distinguish it from the 
               // other selected nodes. We put the stroke inside the original highlight rectangle so that it doesn't get
               // painted over when we paint adjacent rows. The outline is painted even if just one node is selected.
               if(reachableNodes.get(i).fgn == fig.getFocusForSelection())
               {
                  hiliteRR.setFrame(indent-1, 1, hiliteW-2, ROWHT-3);
                  g2.setStroke(focusStroke);
                  g2.setColor(hasFocus ? focusColor : focusColor_NoFocus);
                  g2.draw(hiliteRR);
               }
            }
            
            // move down to next row
            g2.translate(0, ROWHT);
         }
         
         // paint animated drag selection rectangle, if any. Note that we have to translate back to UL corner.
         if(dragOrigin != null && !dragSelectR.isEmpty())
         {
            g2.translate(0, -(iLast+1)*ROWHT);
            g2.setColor(hiliteColor);
            g2.setStroke(dragSelectStroke);
            g2.draw(dragSelectR);
         }
      }

      /** Repaint each row corresponding to a node in the figure's current list of selected node. */
      void repaintCurrentSelection() { for(Integer row : currSelection) repaintRow(row); }

      /**
       * Adjust the current scroll position, if necessary, to bring into view the row corresponding to the focus node in
       * the figure's current list of selected nodes.
       */
      void ensureSelectionVisible()
      {
         if(selectedNodes.isEmpty()) return;
         
         FGraphicNode n = fig.getFocusForSelection();
         int focusRow = -1;
         for(int i=0; i<reachableNodes.size(); i++) if(reachableNodes.get(i).fgn == n)
         {
            focusRow = i;
            break;
         }
         if(focusRow < 0) return;
         
         Rectangle visR = scroller.getViewport().getViewRect();
         int yTop = focusRow * ROWHT;
         if(yTop >= visR.y && yTop + ROWHT < visR.y + visR.height) return;
         
         int vScroll;
         if(yTop < visR.y) vScroll = yTop;
         else
         {
            vScroll = yTop - ROWHT * ((visR.height / ROWHT) - 1);
            if(vScroll < 0) vScroll = 0;
         }
         scroller.getVerticalScrollBar().setValue(vScroll);
      }

      /**
       * Repaint the specified row in the rendered list of currently reachable tree nodes, but only if it is at least 
       * partially visible in the scrollable viewport.
       * @param idx Index of a row in the node tree traversal list.
       */
      void repaintRow(int idx)
      {
         Rectangle visR = scroller.getViewport().getViewRect();
         int yTop = idx * ROWHT;
         if((yTop >= visR.y + visR.height) || (yTop + ROWHT < visR.y)) return;
         
         visR.setFrame(0, yTop, getWidth(), ROWHT);
         repaint(visR);
      }

      /**
       * Get the index of the row in the node tree canvas that is "under" the specified point.
       * @param p A point in canvas coordinates.
       * @return Index of row (ie, index of an entry in the list of currently reachable tree nodes); -1 if not found.
       */
      int getIndexOfRowUnderPoint(Point p) { return(p==null ? -1 : getIndexOfRowUnderPoint(p.x, p.y)); }
      
      /** 
       * Get the index of the row in the node tree canvas that is "under" the point at the specified coordinates.
       * @param x X-coordinate of point, in canvas coordinates.
       * @param y Y-coordinate of point, in canvas coordinates.
       * @return Index of row (ie, index of an entry in the list of currently reachable tree nodes); -1 if not found.
       */
      int getIndexOfRowUnderPoint(int x, int y)
      {
         int row = y / ROWHT;
         if(row < 0 || row >= reachableNodes.size()) row = -1;
         return(row);
      }
      
      /**
       * Is a collapse/expand toggle icon under the specified point in the tree canvas? 
       * @param p A point in canvas coordinates.
       * @return True iff the point lies over the bounding rectangle for the collapse/expand toggle icon for a valid 
       * row in the current list of reachable tree nodes. Note that we don't render the icon for the permanently 
       * expanded root figure node, nor for a leaf node or a child-less node, so the method returns false in those 
       * cases.
       */
      boolean isExpandToggleIconUnder(Point p)
      {
         int row = p.y / ROWHT;
         boolean ok = (row > 0 && row < reachableNodes.size());    // the first row is root figure; NO toggle icon
         if(ok) ok = reachableNodes.get(row).canCollapse();
         if(ok)
         {
            int indent = reachableNodes.get(row).indentLevel * (ROWHT - 2) + 2;
            int dy = p.y - row * ROWHT;
            int vgap = (ROWHT - EXPTOGRECTW) / 2;
            ok = (p.x >= indent - EXPTOGRECTW) && (p.x < indent) && (dy >= vgap) && (dy < ROWHT-vgap);
         }
         return(ok);
      }
      
      /**
       * Call this method whenever the list of currently reachable tree nodes changes in any way. It computes the 
       * preferred size of the canvas that would accommodate the list of reachable nodes without need for scrolling. To 
       * do the width computation, it has to compute the length of every row, which involves text-length calculations. 
       * The preferred height is simply the number of rows -- i.e., the number of reachable tree nodes -- times the 
       * (fixed) row height.
       * 
       * <p>If there's a change in the computed size, the canvas is revalidated to ensure that the scrolling viewport 
       * adjusts for the change.</p>
       */
      void layoutCanvas()
      {
         int w = 0;
         Graphics2D g2 = (Graphics2D) getGraphics();
         
         for(NodeEntry entry : reachableNodes)
         {
            // rowHt = node icon width + 4. 
            // indent from left edge of canvas to left edge of node icon = L*(rowHt - 2) + 2. The collapse/expand
            // toggle icon is drawn in the indent region. Add rowHt-2 to get to left edge of node text.
            int rowLen = entry.indentLevel * (ROWHT-2)  + ROWHT;

            labelPainter.setTextAndLocation(entry.getLabel(), 0, 0);
            labelPainter.invalidateBounds();
            labelPainter.updateFontRenderContext(g2);
            labelPainter.getBounds2D(titleBounds);
            rowLen += (int) (titleBounds.getWidth()  + 5);  // extra 5 pixels just to be sure
            if(rowLen > w) w = rowLen;
         }
         int h = reachableNodes.size() * ROWHT;
         
         // force canvas to be as wide as the scrollable viewport width, which is fixed
         if(w < scroller.getViewport().getWidth()) w = scroller.getViewport().getWidth();
         
         if(w != renderedW || h != renderedH)
         {
            renderedW = w;
            renderedH = h;
            revalidate();
         }
      }

      /** The preferred width of canvas == length of longest row. */
      private int renderedW = 0;
      /** The preferred height of canvas == number of rows * {@link #ROWHT}. */
      private int renderedH = 0;
      
      /** Clip rectangle during painting (so we don't re-allocate on every paint cycle). */
      private final Rectangle clipR = new Rectangle(0,0,0,0);
      /** Rectangle used to retrieve bounds of individual node titles from string painter. */
      private final Rectangle2D titleBounds = new Rectangle2D.Double(0, 0, 0, 0);
      
      /** The canvas background color. */
      final Color BKGCOLOR = Color.WHITE;
      
      /** Translucent rounded rectangle marking the highlighted node in node tree. */
      private final RoundRectangle2D hiliteRR;
      /** Fill color for rectangular highlight (= translucent steel blue). */
      private final Color hiliteColor = new Color(70, 130, 180, 80);
      /** Fill color for rectangular highlight when component does not have keyboard focus (= translucent gray). */
      private final Color hiliteColor_NoFocus = new Color(128, 128, 128, 80);
      /** Stroke color for rectangular highlight when item is the focus for a multi-node selection (= blue). */
      private final Color focusColor = Color.BLUE;
      /** Stroke color for focus item highlight when component does not have keyboard focus (= translucent black). */
      private final Color focusColor_NoFocus = new Color(0, 0, 0, 80);
      /** Stroke style used to draw the outline around the focus node for a multi-node selection. */
      private final BasicStroke focusStroke = new BasicStroke(2);
      /** String painter used to render the label for each node in the rendered node tree traversal list. */
      private final SingleStringPainter labelPainter;
      /** Stroke style used to draw the animated drag-to-select rectangle. */
      private final BasicStroke dragSelectStroke = new BasicStroke(3);
      
      /** 
       * Rightward-pointing arrow shape used to indicate that node has children but is in the collapsed state. Vertices
       * are specified WRT top-left corner of the rectangle in which the collapse/expand indicator is drawn.
       */
      private final GeneralPath rightArrow;
      /** 
       * Downward-pointing arrow shape used to indicate that node is in the expanded state. Vertices are specified WRT 
       * top-left corner of the rectangle in which the collapse/expand indicator is drawn.
       */
      private final GeneralPath downArrow;
      
      /** The fixed height of each row representing a reachable node in the current state of the node tree. */
      private final static int ROWHT = 26;
      /** The minimum visible width and height of the node tree canvas == preferred width of its scrolling viewport. */
      private final static int MIN_VIS_SZ = ROWHT*8;
      /** Width of the rectangle in which the collapse/expand toggle icon is centered. */
      private final static int EXPTOGRECTW = 16;
      /** Width of the bounding box for the collapse/expand toggle icon. */
      private final static int EXPTOGICONW = 10;
   }


   /**
    * Helper class implementing the node properties editor panel, which is a child of and sits below the figure node
    * tree. It manages a set of node type-specific editors, one for each type of graphic node in the FypML figure model.
    * When a different node gets the display focus, the appropriate editor is loaded into this panel.
    * 
    * <p>In addition, the properties editor has an embedded {@link JUnicodeCharacterMap} by which the user can insert
    * special characters into a text component within the currently displayed node editor. The mapper is laid out above
    * the node-specific editor. Since it takes up vertical space in the figure node tree but is infrequently used, it
    * is normally hidden. A top-level menu action toggles its visibility. When displayed and the user selects a 
    * character in the character table, the character selection is forwarded to the current node editor via {@link
    * FGNEditor#onInsertSpecialCharacter(String)}; if the focus is currently on a text component within that editor, 
    * the chosen character may be inserted into that component, if appropriate.</p>
    * 
    * <p>The custom-painted panel includes a "title bar" of sorts along its bottom edge -- a node type-specific icon and
    * title ("[Node] Properties", where [Node] is replaced by the node type name) -- to identify what kind of node is
    * being edited within the panel.</p>
    * 
    * <p>Note that the graph node properties editor, {@link FGNGraphEditor}, is actually a composite editor that handles 
    * not only the graph node itself, but all of its component nodes (three axes, legend, grid lines) AND all of the 
    * tick set node children of each axis.</p>
    * 
    * @author sruffner
    */
   private class NodeEditor extends JPanel implements PropertyChangeListener
   {
      /** Construct the node properties editor panel. The embedded character mapper tool is hidden initially. */
      NodeEditor()
      {
         editorsByType = new HashMap<>();
         
         // this editor is used for the graph node and ALL of its component nodes (including tick sets of an axis)
         FGNGraphEditor graphEditor = new FGNGraphEditor();
         
         // this editor is used for the 3D graph node and ALL of its component nodes (including tick sets of an axis)
         FGNGraph3DEditor graph3DEditor = new FGNGraph3DEditor();
         
         // this editor is used for the 2D polar plot node and ALL of its component nodes
         FGNPolarPlotEditor polarPlotEditor = new FGNPolarPlotEditor();
         
         // for some reason, constructing the FGNGraphEditor is fairly time-consuming!
         FigureComposer.updateStartupProgress(45, "Constructing UI...");
         
         editorsByType.put(FGNodeType.FIGURE, new FGNFigureEditor());
         editorsByType.put(FGNodeType.LABEL, new FGNLabelEditor());
         editorsByType.put(FGNodeType.TEXTBOX, new FGNTextBoxEditor());
         editorsByType.put(FGNodeType.IMAGE, new FGNImageEditor());
         editorsByType.put(FGNodeType.SHAPE, new FGNShapeEditor());
         editorsByType.put(FGNodeType.LINE, new FGNLineEditor());
         editorsByType.put(FGNodeType.CALIB, new FGNCalibEditor());
         editorsByType.put(FGNodeType.GRAPH3D, graph3DEditor);
         editorsByType.put(FGNodeType.BACK3D, graph3DEditor);
         editorsByType.put(FGNodeType.GRID3D, graph3DEditor);
         editorsByType.put(FGNodeType.AXIS3D, graph3DEditor);
         editorsByType.put(FGNodeType.TICKS3D, graph3DEditor);
         editorsByType.put(FGNodeType.SCATTER3D, new FGNScatter3DEditor());
         editorsByType.put(FGNodeType.SURFACE, new FGNSurfaceEditor());
         editorsByType.put(FGNodeType.GRAPH, graphEditor);
         editorsByType.put(FGNodeType.AXIS, graphEditor);
         editorsByType.put(FGNodeType.GRIDLINE, graphEditor);
         editorsByType.put(FGNodeType.FUNCTION, new FGNFunctionEditor());
         editorsByType.put(FGNodeType.TRACE, new FGNTraceEditor());
         editorsByType.put(FGNodeType.RASTER, new FGNRasterEditor());
         editorsByType.put(FGNodeType.CONTOUR, new FGNContourEditor());
         editorsByType.put(FGNodeType.BOX, new FGNBoxPlotEditor());
         editorsByType.put(FGNodeType.BAR, new FGNBarPlotEditor());
         editorsByType.put(FGNodeType.SCATTER, new FGNScatterPlotEditor());
         editorsByType.put(FGNodeType.AREA, new FGNAreaChartEditor());
         editorsByType.put(FGNodeType.PIE, new FGNPieChartEditor());
         editorsByType.put(FGNodeType.PGRAPH, polarPlotEditor);
         editorsByType.put(FGNodeType.PAXIS, polarPlotEditor);
         
         
         // NOTE: The legend node type is omitted from the above map because all 3 graph container types have a legend
         // component. Similarly, the CBAR node type is omitted because both the 2D graph and polar plot containers
         // have a color bar/axis. And the TICKS node type is omitted because the color bar can have tick sets, so that
         // node appears under both the 2D graph and 2D polar plot containers.
         
         // the character mapper tool is initially hidden
         mapper = new JUnicodeCharacterMap(
               new Font("Arial", Font.PLAIN, 12), 
               FGraphicModel.getSupportedUnicodeSubsets(), 2, 15);
         mapper.setFocusable(false);
         mapper.setBorder(BorderFactory.createCompoundBorder(
               BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(6, 6, 10, 6),
                     BorderFactory.createRaisedSoftBevelBorder()),
               BorderFactory.createTitledBorder("Special characters")
               ));
         mapper.setVisible(false);
         mapper.addPropertyChangeListener(JUnicodeCharacterMap.SELCHAR_PROPERTY, this);
         
         setLayout(new BorderLayout());
         // add(currentEditor, BorderLayout.CENTER);
         add(Box.createHorizontalStrut(5), BorderLayout.WEST);
         add(Box.createHorizontalStrut(5), BorderLayout.EAST);
         add(mapper, BorderLayout.NORTH);
         add(Box.createVerticalStrut(42), BorderLayout.SOUTH);

         setBorder(normalBorder);
         
         titlePainter = new StringPainter();
         titlePainter.setAlignment(TextAlign.LEADING, TextAlign.CENTERED);
         titlePainter.setStyle(BasicPainterStyle.createBasicPainterStyle(
               BasicPainterStyle.getFontForPainter("Arial", GenericFont.SANSERIF, FontStyle.BOLD, 16), 
               2, null, Color.BLACK, Color.BLACK));
         
         // set size constraints
         fixedWidth = -1;
         maxHeight = 0;
         for(FGNodeType type : editorsByType.keySet())
         {
            Dimension dEditor = editorsByType.get(type).getPreferredSize();
            if(fixedWidth < dEditor.width) fixedWidth = dEditor.width;
            if(maxHeight < dEditor.height) maxHeight = dEditor.height;  
         }
         fixedWidth += 12;
         maxHeight += 51;
      }

      /** Overridden to set fixed width accommodating largest preferred width among the node-specific editors. */
      @Override public Dimension getMinimumSize()
      {
         Dimension sz = super.getMinimumSize();
         sz.width = fixedWidth;
         return(sz);
      }

      /** Overridden to set fixed width and to accommodate largest preferred height among the node-specific editors. */
      @Override public Dimension getMaximumSize()
      {
         return(new Dimension(fixedWidth, maxHeight));
      }

      /** Overridden to set preferred width to the largest preferred width among the node-specific editors. */
      @Override public Dimension getPreferredSize()
      {
         Dimension sz = super.getPreferredSize();
         sz.width = fixedWidth;
         return(sz);
      }

      /**
       * Reload node properties editor to display/edit the properties of the current figure's selected node. Be sure to
       * invoke this method whenever the current selection changes!
       * 
       * <p>If more than one node is selected in the figure, the properties panel is hidden because you cannot edit the
       * definition of multiple nodes simultaneously. Otherwise, the previous node-specific editor is removed from the 
       * panel, and the correct editor for the specified node is added in its place, after loading the node definition. 
       * The editor panel is then revalidated to ensure it is correctly sized to hold the new node editor. Finally, the 
       * panel is made visible (if necessary).</p>
       */
      void loadSelectedNode()
      {
         FGraphicNode n = (fig == null || !fig.isHomogeneousSelection()) ? null : fig.getFocusForSelection();
         if(n == null) 
         {
            lower();
            return;
         }

         if(n != currentNode)
         {
            currentNode = n;
            
            FGNEditor nextEditor = editorsByType.get(currentNode.getNodeType());
            
            // The legend and color/bar axis components can appear in different classes of graph containers, so we
            // choose the editor based on the parent container's node type. Similarly, a tick set in a color bar may 
            // appear in a 2D graph or 2D polar plot, so we choose the editor based on the type of graph container...
            if(nextEditor == null) 
            {
               if(currentNode.getNodeType() == FGNodeType.TICKS)
                  nextEditor = editorsByType.get(currentNode.getParent().getParent().getNodeType());
               else
                  nextEditor = editorsByType.get(currentNode.getParent().getNodeType());
            }

            if(nextEditor != currentEditor)
            {
               if(currentEditor != null)
               {
                  currentEditor.onLowered();
                  remove(currentEditor);
               }
               currentEditor = nextEditor;
               currentEditor.load(currentNode);
               currentEditor.onRaised();
               add(currentEditor, BorderLayout.CENTER);
               revalidate();
               repaint();
            }
            else 
            {
               currentEditor.onLowered();
               currentEditor.load(currentNode);
               repaint(); 
            }
            
            DSEditorToolDlg.onNodeSelected(currentNode);
         }

         mapper.setMappedFont(currentNode.getFont());
         
         setBorder(fig.isMultiNodeSelection() ? multiSelBorder : normalBorder);
         setVisible(true);
         currentEditor.onRaised();
      }
      
      /**
       * Invoked this method whenever the definition of a node in the currently rendered figure changes. If the 
       * specified node is the node currently loaded in the properties panel, the properties editor is reloaded to 
       * ensure it reflects the node's current definition. The method handles the special case of the composite graph 
       * node editors (2D and 3D), which displays the properties of multiple nodes.
       * @param n The graphic node whose definition has changed.
       */
      void reloadOnNodeChanged(FGraphicNode n)
      {
         if(n == null) return;
         
         if(currentNode != null) mapper.setMappedFont(currentNode.getFont());

         // simple case: The node currently on display was changed. Reload the node editor accordingly.
         if(currentNode == n)
         {
            currentEditor.reload(false);
            DSEditorToolDlg.onNodeChanged(currentNode); 
            return;
         }
         
         // special case: The composite 2D and 3D graph editors. User may change a component node of a 2D or 3D graph
         // object, or the tick mark set of any axis component. In such cases, the node changed could be a descendant of
         // the "current node".
         // NOTE: This also handles another case -- when the component symbol or error bar nodes of a trace node
         // are changed.
         FGNodeType nt = n.getNodeType();
         if(n.isComponentNode() || nt == FGNodeType.TICKS || nt == FGNodeType.TICKS3D)
         {
            FGraphicNode parent = n.getParent();
            if(currentNode == parent || (parent != null && currentNode == parent.getParent()))
               currentEditor.reload(false);
         }
         
         // it is also possible to get a node-changed notification for a 2D or 3D graph node when the current node is 
         // actually a component of that graph. This handles that case.
         if((nt == FGNodeType.GRAPH || nt==FGNodeType.PGRAPH || nt==FGNodeType.GRAPH3D) && 
               currentNode.isDescendantOf(n))
            currentEditor.reload(false);
      }

      /**
       * Hide the node properties editor panel. The currently installed node property editor is notified in case it
       * needs to do any clean-up or close any transient modeless pop-up windows.
       */
      void lower()
      {
         if(isVisible())
         {
            if(currentEditor != null) currentEditor.onLowered();
            setVisible(false);
            DSEditorToolDlg.onNodeSelected(null); 
         }
      }

      /**
       * Whenever user selects a character in the character mapper tool, forward that character selection to the 
       * currently installed node editor.
       */
      @Override public void propertyChange(PropertyChangeEvent e)
      {
         if(e.getSource() == mapper)
         {
            char c = mapper.getSelectedCharacter();
            if((c != 0) && isVisible()  && (currentEditor != null))
            {
               currentEditor.onInsertSpecialCharacter("" + c);
            }
         }
      }
      
      @Override protected void paintComponent(Graphics g)
      {
         Graphics2D g2 = (Graphics2D) g;
         int h = getHeight();
         
         // we always want nice-looking renderings
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
         
         
         // when the current selection contains multiple nodes of the same type, the editor remains visible so that the
         // user can make simultaneous changes to all of the nodes selected. To cue the user to this special situation,
         // the node editor's background is different.
         Color bkg = getBackground();
         if(fig != null && fig.isHomogeneousSelection() && fig.isMultiNodeSelection()) bkg = multiSelBkg;
         
         // paint the background with a gradient highlighting panel "title bar" along bottom edge
         g2.setPaint(new GradientPaint(0, h, bkg, 0, h-20, bkg.brighter().brighter(), true));
         g2.fillRect(0, h-40, getWidth(), 40);
         g2.setPaint(getBackground());
         g2.fillRect(0, 0, getWidth(), h-40);
         
         // paint "title bar" along bottom edge: icon plus label
         ImageIcon icon = (currentEditor != null) ? currentEditor.getRepresentativeIcon() : FCIcons.V4_BROKEN;
         String title = ((currentEditor != null) ? currentEditor.getRepresentativeTitle() : "Node Properties");
         g2.drawImage((icon != null) ? icon.getImage() : null, 5, h-37, null);
         titlePainter.setTextAndLocation(title, 0, 0);
         titlePainter.invalidateBounds();
         titlePainter.updateFontRenderContext(g2);
         g2.translate(42, h-21);
         g2.scale(1, -1);
         titlePainter.render(g2, null);
         g2.scale(1, -1);
         g2.translate(-42, 21-h);
      }

      /** The figure graphic node currently displayed in the node properties editor. */
      private FGraphicNode currentNode = null;
      /** The node-specific editor currently installed in the node properties editor panel. */
      private FGNEditor currentEditor = null;
      /** The node-specific editors, mapped by node type. */
      private final HashMap<FGNodeType, FGNEditor> editorsByType;
      
      /** 
       * Character mapper tool for inserting special characters into select string-valued properties of the
       * currently edited node. Usually hidden, but visibility is toggled by user action.
       */
      final JUnicodeCharacterMap mapper;

      /** Fixed width of node properties editor accounts for width of widest node-specific editor. */
      private int fixedWidth = -1;
      /** Maximum height of node properties editor for height of tallest node-specific editor. */
      private int maxHeight = -1;
      
      /** Used to paint the node editor's current title. */
      private StringPainter titlePainter = null;
      
      /** Node editor title's background color when multiple nodes selected of the same type. */
      private final Color multiSelBkg = new Color(240,128,128);
      
      /** Normal border for the node editor container. */
      private final Border normalBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5,0,0,0), BorderFactory.createMatteBorder(2, 1, 2, 1, Color.GRAY));
      /** Border for node editor container when editing a homogeneous multiple-object selection. */
      private final Border multiSelBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5,0,0,0), BorderFactory.createMatteBorder(2, 1, 2, 1, multiSelBkg));
   }
   
   
   /**
    * Show or hide the special character mapper within the node properties editor. This mapper tool takes up vertical
    * space but is infrequently used, so it is typically hidden by default. Use this method to toggle its visibility.
    * 
    * @param b True to show, false to hide the character mapper.
    */
   void setCharacterMapperVisible(boolean b)
   {
      propEditor.mapper.setVisible(b);
   }
   
   /** Is the special character mapper currently visible in the node properties editor?  */
   boolean isCharacterMapperVisible() { return(propEditor.mapper.isVisible()); }
}
