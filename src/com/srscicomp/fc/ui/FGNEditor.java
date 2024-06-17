package com.srscicomp.fc.ui;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import com.srscicomp.fc.fig.FGraphicNode;

/**
 * <code>FGNEditor</code> defines common functionality shared by all graphic node editor panels that appear in the 
 * "floating" node properties editor in the {@link FigComposer}'s interaction layer. Each such editor panel is 
 * dedicated to a specific node type ({@link com.srscicomp.fc.fig.FGNodeType}. Each has a transparent background because
 * the editor container in <code>FigComposer</code> applies a non-rectangular and translucent skin around the
 * node-specific panel. The individual widgets within the editor panel need not be translucent.
 * 
 * @author sruffner
 */
abstract class FGNEditor extends JPanel
{
   /** A common tool tip for widget that edits the "id" attribute, which 9 different types of nodes possess. */
   final static String IDATTR_TIP = 
         "<html>Enter optional object ID. If not an empty string, it must uniquely identify this<br/>" +
         "object among all graphic objects in the figure (value is auto-corrected if necessary).<br/>" +
         "<b>Note that ID is never rendered. Leave empty if object ID is not needed.</b></html>";
   
   FGNEditor()
   {
      super();
      super.setOpaque(false);
   }

   /** Overridden to prevent changing. The editor panel is always NOT opaque to support a transparent background. */
   @Override public void setOpaque(boolean isOpaque) {}

   /**
    * Load the properties of the specified figure graphic node for display and editing within this editor panel. If the
    * editor panel does not apply to the node specified, the panel is hidden. <i>This method is invoked whenever 
    * the identity of the current focus node changes for the figure currently on display in the {@link FigComposer}.</i>
    * 
    * @param n A <i>DataNav</i> figure model graphics node.
    * @return True if the node was loaded into this editor panel; false otherwise (in which case editor is hidden).
    */
   final boolean load(FGraphicNode n)
   {
      editedNode = isEditorForNode(n) ? n : null;
      setVisible(editedNode != null);
      if(editedNode == null) return(false);
      
      reload(true);
      return(true);
   }
   
   /** 
    * Get a reference to the figure graphic node currently installed in this editor.
    * @return The node, or null if no node is loaded in the editor.
    */
   final FGraphicNode getEditedNode() { return(editedNode); }
   
   /**
    * Reload the properties of the node currently installed in this editor. <i>This method is invoked whenever there's
    * a change in the definition of definition of the current focus node for the figure currently on display in the
    * {@link FigComposer}.</i>
    * @param initial If true, the edited node is being loaded for the first time; otherwise, it is being reloaded 
    * because one or more of its properties changed. This flag is provided because the editor's behavior may be 
    * different for the two use cases -- initial load vs. reload.
    */
   abstract void reload(boolean initial);
   
   /** 
    * This method is invoked immediately after the node editor container within {@link FigComposer} is made visible. It 
    * gives the node-specific editor panel a chance to do any initialization work that must be put off until the 
    * component is visible. <i>The default implementation takes no action.</i>
    */
   void onRaised() {}
   
   /**
    * This method is invoked immediately before the node editor container within {@link FigComposer} is hidden. It 
    * gives the node-specific editor panel a chance to extinguish any non-modal pop-up windows and do any other clean-up
    * that must be done when the editor is hidden. <i>The default implementation takes no action.</i>
    */
   void onLowered() {}
   
   /**
    * This method is invoked on the current node editor panel whenever the user selects a character from the special
    * characters tool dialog. This dialog allows the user to insert characters into text components that are not
    * accessible from a standard keyboard. By design, the dialog is not focusable, and the selected character should
    * only be inserted into a text component that has the current keyboard focus AND accepts such character input.
    * 
    * <p><i>The default implementation does nothing.</i> If a subclass representing a node-specific editor includes a
    * text widget that accepts special characters AND that text component currently has the keyboard focus, then it
    * should insert the character at the current caret position, replacing any selected text.</p>
    * @param s A string containing the user-selected special character.
    */
   void onInsertSpecialCharacter(String s) {}
   
   /**
    * Is this node editor panel applicable to the specified figure graphic node? For a complex node like a graph, a
    * single editor may handle the graph and its "component nodes" (axes, legend, grid lines).
    * @return A figure graphic node. If null, return false.
    */
   abstract boolean isEditorForNode(FGraphicNode n);
   
   /**
    * Get the 32x32-pixel icon that is representative of the type of graphic node whose properties may be displayed and
    * edited within this node editor panel. The icon appears as part of the "skin" of the node editor's container in
    * the {@link FigComposer}.
    * @return The icon for the node type handled by this editor.
    */
   abstract ImageIcon getRepresentativeIcon();
   
   /**
    * Get a short title string that is representative of the type of graphic node whose properties are displayed and
    * edited within this node editor panel. The title appears as part of the "skin" of the node editor's container in
    * the {@link FigComposer}. For consistency, the title must end with the word "Properties".
    * @return The title for this node editor.
    */
   abstract String getRepresentativeTitle();
   
   /** The figure graphic node currently installed in this node properties editor panel. */
   private FGraphicNode editedNode = null;
}
