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
import com.srscicomp.fc.fig.BarPlotNode;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * The extension of {@link FGNEditor} that displays/edits all properties of a {@link BarPlotNode}.
 * 
 * <p>The bar plot node's title is entered in a plain text field; however, the title is never rendered in the figure -- 
 * it is for identification purposes only and will serve to distinguish the bar plot node in the figure node tree GUI.
 * On the next row is an embedded {@link FGNPlottableDSCard}, which exposes the ID of the data set referenced by the 
 * node, and provides facilities for viewing and/or editing that raw data set. Below this are widgets for specifying the
 * bar plot's display mode, baseline, and relative bar width -- plus a check box to enable/disable rendering the bar
 * group legend labels on the bar plot itself (an alternative to showing them in the graph legend). The next two rows
 * are occupied by the {@link TextStyleEditor} and {@link DrawStyleEditor} exposing the bar plot's text/font styles (for
 * the bar group legend labels) and draw styles. At the bottom of the panel is a {@link DataGroupPropEditor} in which 
 * the user can view and modify the bar group fill colors and legend labels.</p>
 * 
 * @author sruffner
 */
class FGNBarPlotEditor extends FGNEditor implements ActionListener, FocusListener
{
   /** Construct the bar plot node properties editor. */
   FGNBarPlotEditor()
   {
      showInLegendChk = new JCheckBox();
      showInLegendChk.setToolTipText(
            "Check this box to include entries in the legend for each data group in the bar plot");
      showInLegendChk.setContentAreaFilled(false);
      showInLegendChk.addActionListener(this);
      add(showInLegendChk);
      
      titleField = new JTextField();
      titleField.addActionListener(this);
      titleField.addFocusListener(this);
      add(titleField);
      
      dsEditor = new FGNPlottableDSCard();
      add(dsEditor);

      modeCombo = new JComboBox<>(BarPlotNode.DisplayMode.values());
      modeCombo.addActionListener(this);
      modeCombo.setToolTipText("Select bar plot display mode");
      add(modeCombo);
      
      JLabel baseLabel = new JLabel("Base ");
      add(baseLabel);
      baselineField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      baselineField.setColumns(6);
      baselineField.setToolTipText("Enter common baseline for bar plot");
      baselineField.addActionListener(this);
      add(baselineField);
      
      JLabel barWLabel = new JLabel("Bar W ");
      add(barWLabel);
      barWidthField = new NumericTextField(BarPlotNode.MINRELBW, BarPlotNode.MAXRELBW);
      barWidthField.setColumns(4);
      barWidthField.setToolTipText("Enter relative bar width as a percentage [" + BarPlotNode.MINRELBW + 
            ".." + BarPlotNode.MAXRELBW + "]");
      barWidthField.addActionListener(this);
      add(barWidthField);
      
      showLabelsChk= new JCheckBox("Labels?");
      showLabelsChk.setToolTipText(
            "Check this box to render legend labels on the bar plot itself");
      showLabelsChk.addActionListener(this);
      add(showLabelsChk);
      
      textStyleEditor = new TextStyleEditor();
      add(textStyleEditor);
      drawStyleEditor = new DrawStyleEditor(true, false);
      add(drawStyleEditor);
      
      barGrpPropEditor = new DataGroupPropEditor(false);
      add(barGrpPropEditor);
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. The editor panel's width is determined by text style editor
      layout.putConstraint(SpringLayout.WEST, showInLegendChk, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, titleField, GAP, SpringLayout.EAST, showInLegendChk);
      layout.putConstraint(SpringLayout.EAST, titleField, -GAP, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, dsEditor, GAP, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, dsEditor, -GAP, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, modeCombo, GAP, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, baseLabel, GAP, SpringLayout.EAST, modeCombo);
      layout.putConstraint(SpringLayout.WEST, baselineField, 0, SpringLayout.EAST, baseLabel);
      layout.putConstraint(SpringLayout.WEST, barWLabel, GAP, SpringLayout.EAST, baselineField);
      layout.putConstraint(SpringLayout.WEST, barWidthField, 0, SpringLayout.EAST, barWLabel);
      layout.putConstraint(SpringLayout.EAST, showLabelsChk, -GAP, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, textStyleEditor);

      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);

      layout.putConstraint(SpringLayout.WEST, barGrpPropEditor, GAP, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, barGrpPropEditor, -GAP, SpringLayout.EAST, this);
      
      // top-bottom constraints: 4 rows. One widget per row is used to set the constraints with row above or below. The 
      // remaining widgets in a row are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, titleField, 0, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, dsEditor, GAP, SpringLayout.SOUTH, titleField);
      layout.putConstraint(SpringLayout.NORTH, modeCombo, GAP, SpringLayout.SOUTH, dsEditor);
      layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, modeCombo);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
      layout.putConstraint(SpringLayout.NORTH, barGrpPropEditor, GAP*2, SpringLayout.SOUTH, drawStyleEditor);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, barGrpPropEditor);
      
      String vc = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vc, showInLegendChk, 0, vc, titleField);
      layout.putConstraint(vc, baseLabel, 0, vc, modeCombo);
      layout.putConstraint(vc, baselineField, 0, vc, modeCombo);
      layout.putConstraint(vc, barWLabel, 0, vc, modeCombo);
      layout.putConstraint(vc, barWidthField, 0, vc, modeCombo);
      layout.putConstraint(vc, showLabelsChk, 0, vc, modeCombo);
   }

   @Override public void reload(boolean initial)
   {
      BarPlotNode bp = getBarPlotNode();
      if(bp == null) return;
      
      showInLegendChk.setSelected(bp.getShowInLegend());

      titleField.setText(bp.getTitle());
      
      dsEditor.reload(bp);
      
      modeCombo.setSelectedItem(bp.getMode());
      baselineField.setValue(bp.getBaseline());
      barWidthField.setValue(bp.getBarWidth());
      showLabelsChk.setSelected(bp.isAutoLabelOn());
      
      textStyleEditor.loadGraphicNode(bp);
      drawStyleEditor.loadGraphicNode(bp);
      
      barGrpPropEditor.loadPlottableNode(bp);
   }

   /** 
    * Close any modeless pop-up windows associated with the embedded draw style and data group editors.
    */
   @Override void onLowered() 
   { 
      textStyleEditor.cancelEditing();
      drawStyleEditor.cancelEditing(); 
      barGrpPropEditor.cancelEditing();
   }

   /**
    * If the focus is on the title field, the special character is inserted at the current caret position. The user will
    * still have to hit the "Enter" key or shift focus away from the title field to submit the changes.
    */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(titleField.isFocusOwner()) titleField.replaceSelection(s);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.BAR == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_BAR_32); }
   @Override String getRepresentativeTitle() { return("Bar Plot Properties"); }

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
      BarPlotNode bp = getBarPlotNode();
      if(bp == null) return;
      Object src = e.getSource();

      boolean ok = true; 
      if(src == titleField)
      {
         ok = bp.setTitle(titleField.getText());
         if(!ok) titleField.setText(bp.getTitle());
      }
      else if(src == showInLegendChk)
         bp.setShowInLegend(showInLegendChk.isSelected());
      else if(src == modeCombo)
         bp.setMode((BarPlotNode.DisplayMode) modeCombo.getSelectedItem());
      else if(src == baselineField)
      {
         ok = bp.setBaseline(baselineField.getValue().floatValue());
         if(!ok) baselineField.setValue(bp.getBaseline());
      }
      else if(src == barWidthField)
      {
         ok = bp.setBarWidth(barWidthField.getValue().intValue());
         if(!ok) barWidthField.setValue(bp.getBarWidth());
      }
      else if(src == showLabelsChk)
         bp.setAutoLabelOn(showLabelsChk.isSelected());
 
      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link BarPlotNode}. */
   private BarPlotNode getBarPlotNode() { return((BarPlotNode) getEditedNode()); }
   
   /** When checked, an entry for each bar group in the bar plot is included in parent graph's legend. */
   private final JCheckBox showInLegendChk;

   /** Text field in which bar plot node's descriptive title is edited. */
   private JTextField titleField = null;

   /** Self-contained editor handles editing/loading of the data set assigned to the bar plot node. */
   private FGNPlottableDSCard dsEditor = null;
   
   /** Combo box selects the bar plot's display mode. */
   private JComboBox<BarPlotNode.DisplayMode> modeCombo = null;

   /** Numeric text field sets the bar plot's baseline value. */
   private NumericTextField baselineField = null;

   /** Numeric text field sets the relative bar width (an integer percentage). */
   private NumericTextField barWidthField = null;
   
   /** When checked, bar group legend labels are drawn on the bar plot itself. */
   private JCheckBox showLabelsChk = null;

   /** Self-contained editor handles the node's text style properties. */
   private TextStyleEditor textStyleEditor = null;
   
  /** Self-contained editor handles the node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** The fill color and legend label of each bar group are displayed and edited in this self-contained editor. */
   private DataGroupPropEditor barGrpPropEditor = null;
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
