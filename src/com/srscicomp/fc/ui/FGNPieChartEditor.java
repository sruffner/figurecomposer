package com.srscicomp.fc.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.PieChartNode;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * The extension of {@link FGNEditor} that displays/edits all properties of a {@link PieChartNode}. 
 * 
 * <p>The pie chart node's title is entered in a plain text field; however, the title is never rendered in the figure -- 
 * it is for identification purposes only and will serve to distinguish the pie chart element in the figure node tree 
 * GUI. On the next row is an embedded {@link FGNPlottableDSCard}, which exposes the ID of the data set referenced by 
 * the node, and provides facilities for viewing and/or editing that raw data set. Below this are widgets for specifying
 * the pie chart's inner radius, outer radius, the relative radial offset for displaced pie slices, and the slice label
 * display mode. The next two rows are occupied by the {@link TextStyleEditor} and {@line DrawStyleEditor} exposing the
 * pie chart's text/font styles (for rendering slice labels) and draw styles.  At the bottom of the panel is an {@link 
 * DataGroupPropEditor} in which the user can view and modify the pie chart's data group properties. Each data group is 
 * represented by one pie slice in the chart, and the table in this panel edits the "displaced" flag, fill color, and 
 * group label associated with each slice. The group label may be rendered on the corresponding pie slice and/or in the
 * parent graph's legend.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNPieChartEditor extends FGNEditor implements ActionListener, FocusListener
{
   /** Construct the pie chart node properties editor. */
   FGNPieChartEditor()
   {
      showInLegendChk = new JCheckBox();
      showInLegendChk.setToolTipText(
            "Check this box to include entries in the legend for each data group in the pie chart");
      showInLegendChk.setContentAreaFilled(false);
      showInLegendChk.addActionListener(this);
      add(showInLegendChk);
      
      titleField = new JTextField();
      titleField.addActionListener(this);
      titleField.addFocusListener(this);
      add(titleField);
      
      dsEditor = new FGNPlottableDSCard();
      add(dsEditor);

      JLabel irLabel = new JLabel("IR ");
      add(irLabel);
      innerRadiusField = new NumericTextField(0, Double.MAX_VALUE, 3);
      innerRadiusField.setColumns(6);
      innerRadiusField.setToolTipText(
            "<html>Enter the pie chart's inner radius in native coordinates, &gt;= 0.<br/>" 
            + "Non-zero value yields a donut-style chart. Must be &lt; outer radius.</html>");
      innerRadiusField.addActionListener(this);
      add(innerRadiusField);
      
      JLabel orLabel = new JLabel("OR ");
      add(orLabel);
      outerRadiusField = new NumericTextField(0, Double.MAX_VALUE, 3);
      outerRadiusField.setColumns(6);
      outerRadiusField.setToolTipText(
            "<html>Enter the pie chart's outer radius in native coordinates. Must be &gt; inner radius.</html>");
      outerRadiusField.addActionListener(this);
      add(outerRadiusField);
      
      JLabel ofsLabel = new JLabel("offset % ");
      add(ofsLabel);
      offsetField = new NumericTextField(PieChartNode.MIN_RADOFS, PieChartNode.MAX_RADOFS);
      offsetField.setColumns(4);
      offsetField.setToolTipText("Enter radial offset of a displaced pie slice as a percentage in [" + 
            PieChartNode.MIN_RADOFS + ".." + PieChartNode.MAX_RADOFS + "]");
      offsetField.addActionListener(this);
      add(offsetField);
      
      JLabel modeLabel = new JLabel("Labels: ");
      add(modeLabel);
      labelModeCB = new JComboBox<PieChartNode.LabelMode>(PieChartNode.LabelMode.values());
      labelModeCB.addActionListener(this);
      labelModeCB.setToolTipText("Pie slice label display mode");
      add(labelModeCB);
      
      textStyleEditor = new TextStyleEditor();
      add(textStyleEditor);
      drawStyleEditor = new DrawStyleEditor(true, false);
      add(drawStyleEditor);
      
      slicePropEditor = new DataGroupPropEditor(true);
      add(slicePropEditor);
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. The editor panel's width is determined by text style editor
      layout.putConstraint(SpringLayout.WEST, showInLegendChk, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, titleField, GAP, SpringLayout.EAST, showInLegendChk);
      layout.putConstraint(SpringLayout.EAST, titleField, -GAP, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, dsEditor, GAP, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, dsEditor, -GAP, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, irLabel, GAP, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, innerRadiusField, 0, SpringLayout.EAST, irLabel);
      layout.putConstraint(SpringLayout.WEST, orLabel, GAP, SpringLayout.EAST, innerRadiusField);
      layout.putConstraint(SpringLayout.WEST, outerRadiusField, 0, SpringLayout.EAST, orLabel);
      layout.putConstraint(SpringLayout.WEST, ofsLabel, GAP, SpringLayout.EAST, outerRadiusField);
      layout.putConstraint(SpringLayout.WEST, offsetField, 0, SpringLayout.EAST, ofsLabel);
      layout.putConstraint(SpringLayout.WEST, modeLabel, GAP*3, SpringLayout.EAST, offsetField);
      layout.putConstraint(SpringLayout.WEST, labelModeCB, 0, SpringLayout.EAST, modeLabel);
      
      layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, textStyleEditor);
      
      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);

      layout.putConstraint(SpringLayout.WEST, slicePropEditor, GAP, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, slicePropEditor, -GAP, SpringLayout.EAST, this);
      
      // top-bottom constraints: 6 rows. One widget per row is used to set the constraints with row above or below. The 
      // remaining widgets in a row are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, titleField, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, dsEditor, GAP*2, SpringLayout.SOUTH, titleField);
      layout.putConstraint(SpringLayout.NORTH, innerRadiusField, GAP*2, SpringLayout.SOUTH, dsEditor);
      layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, innerRadiusField);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
      layout.putConstraint(SpringLayout.NORTH, slicePropEditor, GAP*2, SpringLayout.SOUTH, drawStyleEditor);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, slicePropEditor);
      
      String vc = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vc, showInLegendChk, 0, vc, titleField);
      layout.putConstraint(vc, irLabel, 0, vc, innerRadiusField);
      layout.putConstraint(vc, orLabel, 0, vc, innerRadiusField);
      layout.putConstraint(vc, outerRadiusField, 0, vc, innerRadiusField);
      layout.putConstraint(vc, ofsLabel, 0, vc, innerRadiusField);
      layout.putConstraint(vc, offsetField, 0, vc, innerRadiusField);
      layout.putConstraint(vc, modeLabel, 0, vc, innerRadiusField);
      layout.putConstraint(vc, labelModeCB, 0, vc, innerRadiusField);
   }

   @Override public void reload(boolean initial)
   {
      PieChartNode pie = getPieChartNode();
      if(pie == null) return;
      
      showInLegendChk.setSelected(pie.getShowInLegend());

      titleField.setText(pie.getTitle());
      
      dsEditor.reload(pie);
      
      innerRadiusField.setValue(pie.getInnerRadius());
      outerRadiusField.setValue(pie.getOuterRadius());
      offsetField.setValue(pie.getRadialOffset());
      labelModeCB.setSelectedItem(pie.getSliceLabelMode());
      
      textStyleEditor.loadGraphicNode(pie);
      drawStyleEditor.loadGraphicNode(pie);
      
      slicePropEditor.loadPlottableNode(pie);
   }

   /** 
    * Close any modeless pop-up windows associated with the embedded draw style and data group editors.
    */
   @Override void onLowered() 
   { 
      textStyleEditor.cancelEditing();
      drawStyleEditor.cancelEditing(); 
      slicePropEditor.cancelEditing();
   }

   /**
    * If the focus is on the title field, the special character is inserted at the current caret position. The user will
    * still have to hit the "Enter" key or shift focus away from the title field to submit the changes.
    */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(titleField.isFocusOwner()) titleField.replaceSelection(s);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.PIE == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_PIE_32); }
   @Override String getRepresentativeTitle() { return("Pie Chart Properties"); }

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
      PieChartNode pie = getPieChartNode();
      if(pie == null) return;
      Object src = e.getSource();

      boolean ok = true; 
      if(src == titleField)
      {
         ok = pie.setTitle(titleField.getText());
         if(!ok) titleField.setText(pie.getTitle());
      }
      else if(src == showInLegendChk)
         pie.setShowInLegend(showInLegendChk.isSelected());
      else if(src == innerRadiusField)
      {
         ok = pie.setInnerRadius(innerRadiusField.getValue().floatValue());
         if(!ok) innerRadiusField.setValue(pie.getInnerRadius());
      }
      else if(src == outerRadiusField)
      {
         ok = pie.setOuterRadius(outerRadiusField.getValue().floatValue());
         if(!ok) outerRadiusField.setValue(pie.getOuterRadius());
      }
      else if(src == offsetField)
      {
         ok = pie.setRadialOffset(offsetField.getValue().intValue());
         if(!ok) offsetField.setValue(pie.getRadialOffset());
      }
      else if(src == labelModeCB)
      {
         ok = pie.setSliceLabelMode((PieChartNode.LabelMode) labelModeCB.getSelectedItem());
         if(!ok) labelModeCB.setSelectedItem(pie.getSliceLabelMode());
      }

      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link PieChartNode}. */
   private PieChartNode getPieChartNode() { return((PieChartNode) getEditedNode()); }
   
   /** When checked, an entry for each slice in the pie chart is included in parent graph's legend. */
   private JCheckBox showInLegendChk = null;

   /** Text field in which pie chart node's descriptive title is edited. */
   private JTextField titleField = null;

   /** Self-contained editor handles editing/loading of the data set assigned to the pie chart node. */
   private FGNPlottableDSCard dsEditor = null;
   
   /** Numeric text field sets the pie chart's inner radius. */
   private NumericTextField innerRadiusField = null;

   /** Numeric text field sets the pie chart's outer radius. */
   private NumericTextField outerRadiusField = null;

   /** Numeric text field sets the radial offset of a displaced pie slice (an integer percentage of outer radius). */
   private NumericTextField offsetField = null;
   
   /** Combo box selects the pie slice label display mode. */
   private JComboBox<PieChartNode.LabelMode> labelModeCB = null;
   
   /** Self-contained editor handles the node's text style properties. */
   private TextStyleEditor textStyleEditor = null;
   
   /** Self-contained editor handles the node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** Per-slice properties are displayed and edited in this self-contained editor. */
   private DataGroupPropEditor slicePropEditor = null;
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
