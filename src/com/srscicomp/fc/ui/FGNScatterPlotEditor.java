package com.srscicomp.fc.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SpringLayout;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.ScatterPlotNode;
import com.srscicomp.fc.uibase.StyledTextEditor;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
 * The extension of {@link FGNEditor} that displays/edits all properties of a scatter/bubble plot node, as defined by
 * {@link ScatterPlotNode}.
 * 
 * <p>The scatter plot node's title is entered in a plain text field; however, the title is never rendered in the 
 * figure, except to label the node's legend entry in the parent graph's automated legend. Next to the text field is a
 * check box that indicates whether or not the node should be included in that legend. On the next row is an embedded 
 * {@link FGNPlottableDSCard}, which exposes the ID of the data set referenced by the node, and provides facilities for 
 * viewing and/or editing that raw data set. Below this are widgets for specifying the scatter plot's display mode, 
 * marker symbol type, and maximum symbol size. The last two rows in the editor panel are {@link DrawStyleEditor}s. The
 * first exposes the scatter plot node's draw styles (it has no text styles), which govern the appearance of the 
 * marker symbols. The second exposes the draw styles of a component {@link ScatterLineStyleNode}, which governs the
 * appearance of the LMS regression line in "trendline" mode, and the "connect the dots" polyline otherwise.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNScatterPlotEditor extends FGNEditor implements ActionListener
{
   /** Construct the scatter plot node properties editor. */
   FGNScatterPlotEditor()
   {
      showInLegendChk = new JCheckBox();
      showInLegendChk.setToolTipText(
            "Check this box to include an entry in the legend for this scatter plot");
      showInLegendChk.setContentAreaFilled(false);
      showInLegendChk.addActionListener(this);
      add(showInLegendChk);
      
      titleEditor = new StyledTextEditor(1);
      titleEditor.setToolTipText("Enter title here (it will be used as the label for the node's legend entry)");
      titleEditor.addActionListener(this);
      add(titleEditor);
      
      dsEditor = new FGNPlottableDSCard();
      add(dsEditor);

      JLabel dispLabel = new JLabel("Display: ");
      add(dispLabel);
      
      modeCombo = new JComboBox<ScatterPlotNode.DisplayMode>(ScatterPlotNode.DisplayMode.values());
      modeCombo.addActionListener(this);
      modeCombo.setToolTipText("Select scatter plot display mode");
      add(modeCombo);
      
      JLabel symbolLabel = new JLabel("Symbols: ");
      add(symbolLabel);

      typeCombo = new JComboBox<Marker>(Marker.values());
      typeCombo.setToolTipText("Select marker symbol shape");
      typeCombo.addActionListener(this);
      add(typeCombo);
      
      sizeEditor = new MeasureEditor(0, ScatterPlotNode.MAXSYMSIZECONSTRAINTS);
      sizeEditor.setToolTipText("<html>Enter marker symbol size. This will be the fixed size of all markers<br/>" +
      		"when symbol size does not vary with Z; else, it is the maximum symbol size. [0.05..2in]</html>");
      sizeEditor.addActionListener(this);
      add(sizeEditor);
      
      symbolStyleEditor = new DrawStyleEditor(false, false);
      add(symbolStyleEditor);
      
      JLabel lineLabel = new JLabel("Line: ");
      add(lineLabel);
      lineStyleEditor = new DrawStyleEditor(false, false);
      add(lineStyleEditor);
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. 
      layout.putConstraint(SpringLayout.WEST, showInLegendChk, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, titleEditor, GAP, SpringLayout.EAST, showInLegendChk);
      layout.putConstraint(SpringLayout.EAST, titleEditor, 0, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, dsEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, dsEditor, 0, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.EAST, dispLabel, 0, SpringLayout.EAST, symbolLabel);
      layout.putConstraint(SpringLayout.WEST, modeCombo, 0, SpringLayout.EAST, dispLabel);
      

      layout.putConstraint(SpringLayout.WEST, symbolLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, typeCombo, 0, SpringLayout.EAST, symbolLabel);
      layout.putConstraint(SpringLayout.WEST, sizeEditor, GAP, SpringLayout.EAST, typeCombo);
      
      layout.putConstraint(SpringLayout.WEST, symbolStyleEditor, GAP, SpringLayout.EAST, symbolLabel);
      
      layout.putConstraint(SpringLayout.EAST, lineLabel, 0, SpringLayout.EAST, symbolLabel);
      layout.putConstraint(SpringLayout.WEST, lineStyleEditor, GAP, SpringLayout.EAST, lineLabel);
      
      // top-bottom constraints: 5 rows. One widget per row is used to set the constraints with row above or below. The 
      // remaining widgets in a row are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, titleEditor, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, dsEditor, GAP*2, SpringLayout.SOUTH, titleEditor);
      layout.putConstraint(SpringLayout.NORTH, modeCombo, GAP, SpringLayout.SOUTH, dsEditor);
      layout.putConstraint(SpringLayout.NORTH, typeCombo, GAP, SpringLayout.SOUTH, modeCombo);
      layout.putConstraint(SpringLayout.NORTH, symbolStyleEditor, GAP, SpringLayout.SOUTH, typeCombo);
      layout.putConstraint(SpringLayout.NORTH, lineStyleEditor, GAP*3, SpringLayout.SOUTH, symbolStyleEditor);
      layout.putConstraint(SpringLayout.SOUTH, this, GAP, SpringLayout.SOUTH, lineStyleEditor);
      
      String vc = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vc, showInLegendChk, 0, vc, titleEditor);
      layout.putConstraint(vc, dispLabel, 0, vc, modeCombo);
      layout.putConstraint(vc, symbolLabel, 0, vc, typeCombo);
      layout.putConstraint(vc, sizeEditor, 0, vc, typeCombo);
      layout.putConstraint(vc, lineLabel, 0, vc, lineStyleEditor);
   }

   @Override public void reload(boolean initial)
   {
      ScatterPlotNode spn = getScatterPlotNode();
      if(spn == null) return;
      
      showInLegendChk.setSelected(spn.getShowInLegend());

      titleEditor.loadContent(spn.getAttributedTitle(false), spn.getFontFamily(), 0, spn.getFillColor(), 
            spn.getFontStyle());
      
      dsEditor.reload(spn);
      
      modeCombo.setSelectedItem(spn.getMode());
      typeCombo.setSelectedItem(spn.getSymbol());
      sizeEditor.setMeasure(spn.getMaxSymbolSize());
      
      symbolStyleEditor.loadGraphicNode(spn);
      lineStyleEditor.loadGraphicNode(spn.getLineStyle());
   }

   /** Close any modeless pop-up windows associated with the embedded draw styles editor. */
   @Override void onLowered() { symbolStyleEditor.cancelEditing(); lineStyleEditor.cancelEditing(); }

   /**
    * If the focus is on the title field, the special character is inserted at the current caret position. The user will
    * still have to hit the "Enter" key or shift focus away from the title field to submit the changes.
    */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(titleEditor.isFocusOwner()) titleEditor.insertString(s, false);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.SCATTER == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_SCATTER_32); }
   @Override String getRepresentativeTitle() { return("Scatter Plot Properties"); }

   @Override public void actionPerformed(ActionEvent e)
   {
      ScatterPlotNode spn = getScatterPlotNode();
      if(spn == null) return;
      Object src = e.getSource();

      boolean ok = true; 
      if(src == titleEditor)
      {
         String text = FGraphicNode.toStyledText(
               titleEditor.getCurrentContents(), spn.getFontStyle(), spn.getFillColor());
         ok = spn.setTitle(text);
         if(!ok) titleEditor.loadContent(spn.getAttributedTitle(false), spn.getFontFamily(), 0, 
               spn.getFillColor(), spn.getFontStyle());
      }
      else if(src == showInLegendChk)
         spn.setShowInLegend(showInLegendChk.isSelected());
      else if(src == modeCombo)
         spn.setMode((ScatterPlotNode.DisplayMode) modeCombo.getSelectedItem());
      else if(src == typeCombo)
      {
         ok = spn.setSymbol((Marker)typeCombo.getSelectedItem());
         if(!ok) typeCombo.setSelectedItem(spn.getSymbol());
      }
      else if(src == sizeEditor)
      {
         ok = spn.setMaxSymbolSize(sizeEditor.getMeasure());
         if(!ok) sizeEditor.setMeasure(spn.getMaxSymbolSize());
      }

      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link ScatterPlotNode}. */
   private ScatterPlotNode getScatterPlotNode() { return((ScatterPlotNode) getEditedNode()); }
   
   /** When checked, an entry for the scatter plot is included in parent graph's legend. */
   private JCheckBox showInLegendChk = null;

   /** The custom widget within which the scatter plot node's title (which may contain attributed text) is edited.  */
   private StyledTextEditor titleEditor = null;
   
   /** Self-contained editor handles editing/loading of the data set assigned to the scatter plot node. */
   private FGNPlottableDSCard dsEditor = null;
   
   /** Combo box selects the scatter plot's display mode. */
   private JComboBox<ScatterPlotNode.DisplayMode> modeCombo = null;

   /** Combo box for selecting the scatter plot's marker symbol type. */
   private JComboBox<Marker> typeCombo = null;

  /** Customized component for editing the size of the scatter plot's marker symbol. */
   private MeasureEditor sizeEditor = null;

   /** Self-contained editor handles the draw style properties for the scatter plot marker symbols. */
   private DrawStyleEditor symbolStyleEditor = null;

   /** Self-contained editor handles the draw style properties for the scatter plot polyline or LMS regression line. */
   private DrawStyleEditor lineStyleEditor = null;
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
