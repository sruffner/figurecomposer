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
import com.srscicomp.fc.fig.AreaChartNode;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * Extension of {@link FGNEditor} that displays/edits all properties of a {@link AreaChartNode}.
 * 
 * <p>The area chart node's title is entered in a plain text field; however, the title is never rendered in the figure 
 * -- it is for identification purposes only and will serve to distinguish the area chart in the figure node tree GUI.
 * On the next row is an embedded {@link FGNPlottableDSCard}, which exposes the ID of the data set referenced by the 
 * node, and provides facilities for viewing and/or editing that raw data set. Below this is a numeric text field to 
 * specify the area chart's baseline value. The next two rows are occupied by the {@link TextStyleEditor} and {@line 
 * DrawStyleEditor} exposing the area chart's text/font styles (for rendering area labels) and draw styles. At the 
 * bottom of the panel is a {@link DataGroupPropEditor} in which the user can view and modify the fill color and legend 
 * label for each of the area chart's individual data groups.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNAreaChartEditor extends FGNEditor implements ActionListener, FocusListener
{
   /** Construct the bar plot node properties editor. */
   FGNAreaChartEditor()
   {
      showInLegendChk = new JCheckBox();
      showInLegendChk.setToolTipText(
            "Check this box to include entries in the legend for each data group in the area chart");
      showInLegendChk.setContentAreaFilled(false);
      showInLegendChk.addActionListener(this);
      add(showInLegendChk);
      
      titleField = new JTextField();
      titleField.addActionListener(this);
      titleField.addFocusListener(this);
      add(titleField);
      
      dsEditor = new FGNPlottableDSCard();
      add(dsEditor);

      JLabel baseLabel = new JLabel("Baseline: ");
      add(baseLabel);
      baselineField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      baselineField.setColumns(6);
      baselineField.setToolTipText("Enter the area chart's baseline");
      baselineField.addActionListener(this);
      add(baselineField);
      
      JLabel modeLabel = new JLabel("Labels: ");
      add(modeLabel);
      labelModeCB = new JComboBox<AreaChartNode.LabelMode>(AreaChartNode.LabelMode.values());
      labelModeCB.addActionListener(this);
      labelModeCB.setToolTipText("Area label display mode");
      add(labelModeCB);
      
      textStyleEditor = new TextStyleEditor();
      add(textStyleEditor);
      drawStyleEditor = new DrawStyleEditor(true, false);
      add(drawStyleEditor);
      
      dataGrpPropEditor = new DataGroupPropEditor(false);
      add(dataGrpPropEditor);
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. The editor panel's width is determined by the embedded text style editor.
      layout.putConstraint(SpringLayout.WEST, showInLegendChk, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, titleField, GAP, SpringLayout.EAST, showInLegendChk);
      layout.putConstraint(SpringLayout.EAST, titleField, -GAP, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, dsEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, dsEditor, -GAP, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, baseLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, baselineField, 0, SpringLayout.EAST, baseLabel);
      layout.putConstraint(SpringLayout.EAST, labelModeCB, -GAP, SpringLayout.EAST, this);
      layout.putConstraint(SpringLayout.EAST, modeLabel, 0, SpringLayout.WEST, labelModeCB);

      layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, textStyleEditor);
      
      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);

      layout.putConstraint(SpringLayout.WEST, dataGrpPropEditor, GAP, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, dataGrpPropEditor, -GAP, SpringLayout.EAST, this);
      
      // top-bottom constraints: 5 rows. One widget per row is used to set the constraints with row above or below. The 
      // remaining widgets in a row are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, titleField, 0, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, dsEditor, GAP*2, SpringLayout.SOUTH, titleField);
      layout.putConstraint(SpringLayout.NORTH, baselineField, GAP*2, SpringLayout.SOUTH, dsEditor);
      layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, baselineField);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
      layout.putConstraint(SpringLayout.NORTH, dataGrpPropEditor, GAP*2, SpringLayout.SOUTH, drawStyleEditor);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, dataGrpPropEditor);
      
      String vc = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vc, showInLegendChk, 0, vc, titleField);
      layout.putConstraint(vc, baseLabel, 0, vc, baselineField);
      layout.putConstraint(vc, modeLabel, 0, vc, baselineField);
      layout.putConstraint(vc, labelModeCB, 0, vc, baselineField);
   }

   @Override public void reload(boolean initial)
   {
      AreaChartNode chart = getAreaChartNode();
      if(chart == null) return;
      
      showInLegendChk.setSelected(chart.getShowInLegend());
      titleField.setText(chart.getTitle());
      dsEditor.reload(chart);
      baselineField.setValue(chart.getBaseline());
      labelModeCB.setSelectedItem(chart.getLabelMode());
      
      textStyleEditor.loadGraphicNode(chart);
      drawStyleEditor.loadGraphicNode(chart);
      dataGrpPropEditor.loadPlottableNode(chart);
   }

   /** Close any modeless pop-up windows associated with the embedded draw style and data group editors. */
   @Override void onLowered() 
   { 
      textStyleEditor.cancelEditing();
      drawStyleEditor.cancelEditing(); 
      dataGrpPropEditor.cancelEditing();
   }

   /**
    * If the focus is on the title field, the special character is inserted at the current caret position. The user will
    * still have to hit the "Enter" key or shift focus away from the title field to submit the changes.
    */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(titleField.isFocusOwner()) titleField.replaceSelection(s);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.AREA == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_AREA_32); }
   @Override String getRepresentativeTitle() { return("Area Chart Properties"); }

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
      AreaChartNode chart = getAreaChartNode();
      if(chart == null) return;
      Object src = e.getSource();

      boolean ok = true; 
      if(src == titleField)
      {
         ok = chart.setTitle(titleField.getText());
         if(!ok) titleField.setText(chart.getTitle());
      }
      else if(src == showInLegendChk)
         chart.setShowInLegend(showInLegendChk.isSelected());
      else if(src == baselineField)
      {
         ok = chart.setBaseline(baselineField.getValue().floatValue());
         if(!ok) baselineField.setValue(chart.getBaseline());
      }
      else if(src == labelModeCB)
      {
         ok = chart.setLabelMode((AreaChartNode.LabelMode) labelModeCB.getSelectedItem());
         if(!ok) labelModeCB.setSelectedItem(chart.getLabelMode());
      }
         

      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link AreaChartNode}. */
   private AreaChartNode getAreaChartNode() { return((AreaChartNode) getEditedNode()); }
   
   /** When checked, an entry for each data group in the area chart is included in parent graph's legend. */
   private JCheckBox showInLegendChk = null;

   /** Text field in which area chart node's descriptive title is edited. */
   private JTextField titleField = null;

   /** Self-contained editor handles editing/loading of the data set assigned to the area chart node. */
   private FGNPlottableDSCard dsEditor = null;
   
   /** Numeric text field sets the area chart's baseline value. */
   private NumericTextField baselineField = null;
   
   /** Combo box selects the area label display mode. */
   private JComboBox<AreaChartNode.LabelMode> labelModeCB = null;
   
   /** Self-contained editor handles the node's text style properties. */
   private TextStyleEditor textStyleEditor = null;
   
   /** Self-contained editor handles the node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** The fill color and legend label of each data group are displayed and edited in this self-contained editor. */
   private DataGroupPropEditor dataGrpPropEditor = null;
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
