package com.srscicomp.fc.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.fc.fig.ErrorBarNode;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
 * <b>FGNEBarCard</b> displays/edits all properties of the component {@link ErrorBarNode} of selected data presentation 
 * nodes in the <i>FypML</i> graphic model. <i>It is intended only for use inside the node property editor for the 
 * parent {@link FGNPlottable} node.</i>
 * 
 * <p>The editor includes widgets to display and edit the {@link ErrorBarNode}'s properties -- the "hide all" flag, 
 * end cap symbol shape and size, and draw styles.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNEBarCard extends JPanel implements ActionListener
{
   /** Construct the error bar node properties editor. */
   FGNEBarCard()
   {
      super();
      setOpaque(false);
      
      hideChk = new JCheckBox("Hide all?");
      hideChk.setContentAreaFilled(false);
      hideChk.addActionListener(this);
      add(hideChk);

      typeCombo = new JComboBox<Marker>(Marker.values());
      typeCombo.setToolTipText("Select endcap shape");
      typeCombo.addActionListener(this);
      add(typeCombo);
      
      sizeEditor = new MeasureEditor(0, FGraphicModel.getSizeConstraints(FGNodeType.SHAPE));
      sizeEditor.setToolTipText("Enter size of endcap's bounding box");
      sizeEditor.addActionListener(this);
      add(sizeEditor);
      
      drawStyleEditor = new DrawStyleEditor(false, false);
      add(drawStyleEditor);
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. Everything sticks to left edge.
      layout.putConstraint(SpringLayout.WEST, hideChk, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, typeCombo, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, sizeEditor, GAP*2, SpringLayout.EAST, typeCombo);
      
      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);
      
      // top-bottom constraints: One widget in each row is used to set the constraints with row above or below. The 
      // remaining widgets are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, hideChk, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, typeCombo, GAP, SpringLayout.SOUTH, hideChk);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, typeCombo);
      layout.putConstraint(SpringLayout.SOUTH, this, GAP, SpringLayout.SOUTH, drawStyleEditor);
      
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, sizeEditor, 0, SpringLayout.VERTICAL_CENTER, typeCombo);
   }

   public void reload(ErrorBarNode ebn)
   {
      if(ebn != null) ebar = ebn;
      if(ebar == null) return;
      
      typeCombo.setSelectedItem(ebar.getEndCap());
      sizeEditor.setMeasure(ebar.getEndCapSize());
      hideChk.setSelected(ebar.getHide());

      drawStyleEditor.loadGraphicNode(ebar);
   }

   /** 
    * Ensure the modeless pop-up windows associated with embedded draw styles editor are extinguished before the 
    * editor panel is hidden.
    */
   void cancelEditing() { drawStyleEditor.cancelEditing(); }

   @Override public void actionPerformed(ActionEvent e)
   {
      if(ebar == null) return;
      Object src = e.getSource();

      if(src == typeCombo)
      {
         if(!ebar.setEndCap((Marker)typeCombo.getSelectedItem()))
         {
            Toolkit.getDefaultToolkit().beep();
            typeCombo.setSelectedItem(ebar.getEndCap());
         }
      }
      else if(src == sizeEditor)
      {
         if(!ebar.setEndCapSize(sizeEditor.getMeasure()))
         {
            Toolkit.getDefaultToolkit().beep();
            sizeEditor.setMeasure(ebar.getEndCapSize());
         }
      }
      else if(src == hideChk)
         ebar.setHide(hideChk.isSelected());
   }

   /** The current error bar node being edited. If null, editor is non-functional. */
   private ErrorBarNode ebar = null;
   
   /** Combo box for selecting the symbol marker shape. */
   private JComboBox<Marker> typeCombo = null;

   /** Customized component for editing the symbol marker's size. */
   private MeasureEditor sizeEditor = null;

   /** Check box sets/clears the hide flag. */
   private JCheckBox hideChk = null;
      
   /** Self-contained editor handles the node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
