package com.srscicomp.fc.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.SurfaceNode;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * The extension of {@link FGNEditor} that displays/edits all properties of a 3D surface plot node, as defined by
 * {@link SurfaceNode}.
 * 
 * <p>The surface node's title is entered in a plain text field; however, the title is never rendered in the 
 * figure -- it only serves to label the node in the GUI. On the next row is an embedded {@link FGNPlottableDSCard}, 
 * which exposes the ID of the data set referenced by the node, and provides facilities for viewing and/or editing that 
 * raw data set. Below this is a numeric text field setting the surface node's mesh size limit and a check box which
 * selects whether the surface is single-color or color-mapped. The last row in the editor panel is an embedded 
 * {@link DrawStyleEditor} exposing the surface's draw styles (it has no text styles).</p>
 * 
 * @author sruffner
 */
class FGNSurfaceEditor extends FGNEditor implements ActionListener, FocusListener
{
   /** Construct the scatter plot node properties editor. */
   FGNSurfaceEditor()
   {
      titleField = new JTextField();
      titleField.setToolTipText("Enter title here (not rendered; for descriptive purposes only)");
      titleField.addActionListener(this);
      titleField.addFocusListener(this);
      add(titleField);
      
      dsEditor = new FGNPlottableDSCard();
      add(dsEditor);

      JLabel limitLabel = new JLabel("Mesh Limit ");
      add(limitLabel);
      meshLimitField = new NumericTextField(SurfaceNode.MIN_MESHLIMIT, SurfaceNode.MAX_MESHLIMIT);
      meshLimitField.setColumns(4);
      meshLimitField.setToolTipText(
            String.format("Enter the mesh size limit, [%d..%d]", SurfaceNode.MIN_MESHLIMIT, SurfaceNode.MAX_MESHLIMIT));
      meshLimitField.addActionListener(this);
      add(meshLimitField);
      
      cmapChk = new JCheckBox("Color-mapped?");
      cmapChk.setToolTipText(
            "If checked, surface is color-mapped; otherwise, it is painted with the node's fill color");
      cmapChk.setContentAreaFilled(false);
      cmapChk.addActionListener(this);
      add(cmapChk);
      
      drawStyleEditor = new DrawStyleEditor(false, true);
      add(drawStyleEditor);
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. The editor panel's width is determined by the embedded data set editor panel.
      layout.putConstraint(SpringLayout.WEST, titleField, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, titleField, 0, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, dsEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, dsEditor);

      layout.putConstraint(SpringLayout.WEST, limitLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, meshLimitField, 0, SpringLayout.EAST, limitLabel);
      layout.putConstraint(SpringLayout.EAST, cmapChk, 0, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);
      
      // top-bottom constraints: 4 rows. One widget per row is used to set the constraints with row above or below. The 
      // remaining widgets in a row are vertically centered WRT that widget. 
      layout.putConstraint(SpringLayout.NORTH, titleField, 0, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, dsEditor, GAP, SpringLayout.SOUTH, titleField);
      layout.putConstraint(SpringLayout.NORTH, meshLimitField, GAP, SpringLayout.SOUTH, dsEditor);
      
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP*2, SpringLayout.SOUTH, meshLimitField);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, drawStyleEditor);
      
      String vc = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vc, limitLabel, 0, vc, meshLimitField);
      layout.putConstraint(vc, cmapChk, 0, vc, meshLimitField);
   }

   @Override public void reload(boolean initial)
   {
      SurfaceNode surf = getSurfaceNode();
      if(surf == null) return;
      
      titleField.setText(surf.getTitle());
      dsEditor.reload(surf);
      meshLimitField.setValue(surf.getMeshLimit());
      cmapChk.setSelected(surf.isColorMapped());
      drawStyleEditor.loadGraphicNode(surf);
   }

   /** Close any modeless pop-up windows associated with the embedded draw styles editor. */
   @Override void onLowered() 
   { 
      drawStyleEditor.cancelEditing(); 
   }

   /**
    * If the focus is on the title field, the special character is inserted at the current caret position. The user will
    * still have to hit the "Enter" key or shift focus away from the title field to submit the changes.
    */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(titleField.isFocusOwner()) titleField.replaceSelection(s);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.SURFACE == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_SURFACE_32); }
   @Override String getRepresentativeTitle() { return("Surface Properties"); }

   @Override public void focusGained(FocusEvent e) 
   {
      if(e.isTemporary()) return;
      Object src = e.getSource();
      if(src == titleField) titleField.selectAll();
   }
   @Override public void focusLost(FocusEvent e)
   {
      if(e.isTemporary()) return;
      Object src = e.getSource();
      if(src == titleField) titleField.postActionEvent();
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      SurfaceNode surf = getSurfaceNode();
      if(surf == null) return;
      Object src = e.getSource();

      boolean ok = true; 
      if(src == titleField)
      {
         ok = surf.setTitle(titleField.getText());
         if(!ok) titleField.setText(surf.getTitle());
      }
      else if(src == cmapChk)
         surf.setColorMapped(cmapChk.isSelected());
      else if(src == meshLimitField)
      {
         ok = surf.setMeshLimit(meshLimitField.getValue().intValue());
         if(!ok) meshLimitField.setValue(surf.getMeshLimit());
      }

      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link SurfaceNode}. */
   private SurfaceNode getSurfaceNode() { return((SurfaceNode) getEditedNode()); }
   
   /** Text field in which the 3D scatter plot node's descriptive title is edited. */
   private final JTextField titleField;

   /** Self-contained editor handles editing/loading of the data set assigned to the 3D scatter plot node. */
   private FGNPlottableDSCard dsEditor = null;
   
   /** Integer text field in which the surface node's mesh size limit is edited. */
   private NumericTextField meshLimitField = null;
   
   /** When checked, the surface is color-mapped; otherwise, the surface mesh cells are filled IAW the fill color. */
   private JCheckBox cmapChk = null;

   /** Self-contained editor handles the node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
