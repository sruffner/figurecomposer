package com.srscicomp.fc.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SpringLayout;

import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.RasterNode;
import com.srscicomp.fc.fig.RasterNode.DisplayMode;
import com.srscicomp.fc.uibase.StyledTextEditor;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * <code>FGNRasterEditor</code> displays/edits all properties of a {@link RasterNode}.
 * 
 * <p>The raster node's title is entered in a plain text field; however, the title is never rendered in the figure -- it
 * is for identification purposes only and will serve to label a raster in the parent graph's legend. On the next row is
 * an embedded {@link FGNPlottableDSCard}, which exposes the ID of the <i>raster1d</i> data set referenced by the node, 
 * and provides facilities for viewing and/or editing that raw data set. Below this are widgets for specifying the 
 * raster's display mode, X- and Y-offset, train height and vertical separation (train modes only), the number of 
 * histogram bins (for histogram mode only), and the <i>avg?</i> flag (histogram mode only). The last row in the editor 
 * panel is an embedded {@link DrawStyleEditor} exposing the raster node's draw styles (it has no text styles).</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNRasterEditor extends FGNEditor implements ActionListener
{
   /** Construct the raster node properties editor. */
   FGNRasterEditor()
   {
      showInLegendChk = new JCheckBox();
      showInLegendChk.setToolTipText("Check this box to include entry in legend for this raster train");
      showInLegendChk.setContentAreaFilled(false);
      showInLegendChk.addActionListener(this);
      add(showInLegendChk);
      
      titleEditor = new StyledTextEditor(1);
      titleEditor.setToolTipText("Enter title here (it will be used as the label for the node's legend entry)");
      titleEditor.addActionListener(this);
      add(titleEditor);
      
      dsEditor = new FGNPlottableDSCard();
      add(dsEditor);

      rngLabel = new JLabel("Observed Range...");
      add(rngLabel);

      modeCombo = new JComboBox<DisplayMode>(DisplayMode.values());
      modeCombo.addActionListener(this);
      modeCombo.setToolTipText("Select raster display mode");
      add(modeCombo);
      
      JLabel x0Label = new JLabel("X Offset ");
      add(x0Label);
      xOffsetField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      xOffsetField.setColumns(6);
      xOffsetField.setToolTipText("Enter x-coordinate offset for histogram/raster trains");
      xOffsetField.addActionListener(this);
      add(xOffsetField);
      
      JLabel y0Label = new JLabel("Y Offset ");
      add(y0Label);
      yOffsetField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      yOffsetField.setColumns(6);
      yOffsetField.setToolTipText("Enter y-coordinate baseline offset for histogram or first raster train");
      yOffsetField.addActionListener(this);
      add(yOffsetField);
      
      JLabel htLabel = new JLabel("Line Ht ");
      add(htLabel);
      lineHeightField = new NumericTextField(1, Integer.MAX_VALUE);
      lineHeightField.setColumns(6);
      lineHeightField.setToolTipText("Enter raster train height in stroke-width increments");
      lineHeightField.addActionListener(this);
      add(lineHeightField);
      
      JLabel spacerLabel = new JLabel("Spacer ");
      add(spacerLabel);
      lineSpacerField = new NumericTextField(1, Integer.MAX_VALUE);
      lineSpacerField.setColumns(6);
      lineSpacerField.setToolTipText("Enter vertical distance between raster trains in stroke-width increments");
      lineSpacerField.addActionListener(this);
      add(lineSpacerField);
      
      JLabel binsLabel = new JLabel("#Bins ");
      add(binsLabel);
      numBinsField = new NumericTextField(RasterNode.MINNUMBINS, RasterNode.MAXNUMBINS);
      numBinsField.setColumns(6);
      numBinsField.setToolTipText("Enter number of histogram bins [" + RasterNode.MINNUMBINS + 
               "-" + RasterNode.MAXNUMBINS + "]");
      numBinsField.addActionListener(this);
      add(numBinsField);
      
      JLabel limLabel = new JLabel("Range Limits: ");
      add(limLabel);
      rngStartField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      rngStartField.setColumns(6);
      String tip = "<html>Set range limits <i>[S..E]</i> for histogram calculations. If <i>S &ge; E</i>,<br/>" +
            "the range limits are set to the actual observed sample range.</html>";
      rngStartField.setToolTipText(tip);
      rngStartField.addActionListener(this);
      add(rngStartField);
      rngEndField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      rngEndField.setColumns(6);
      rngEndField.setToolTipText(tip);
      rngEndField.addActionListener(this);
      add(rngEndField);

      add(xOffsetField);
      avgChk = new JCheckBox("Average?");
      avgChk.setToolTipText("<html>Check this box if histogram bars should reflect average rather than <br/>" + 
            "total bin counts ('histogram' mode only)</html>");
      avgChk.setContentAreaFilled(false);
      avgChk.addActionListener(this);
      add(avgChk);
      
      drawStyleEditor = new DrawStyleEditor(false, false);
      add(drawStyleEditor);
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. The editor panel's width is determined by the 6th row of widgets, which is the longest. 
      // The rightmost widget in that row is the "Average?" check box
      layout.putConstraint(SpringLayout.WEST, showInLegendChk, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, titleEditor, GAP, SpringLayout.EAST, showInLegendChk);
      layout.putConstraint(SpringLayout.EAST, titleEditor, 0, SpringLayout.EAST, avgChk);

      layout.putConstraint(SpringLayout.WEST, dsEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, dsEditor, 0, SpringLayout.EAST, avgChk);

      layout.putConstraint(SpringLayout.WEST, rngLabel, 0, SpringLayout.WEST, this);
      
      layout.putConstraint(SpringLayout.WEST, modeCombo, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, x0Label, GAP*3, SpringLayout.EAST, modeCombo);
      layout.putConstraint(SpringLayout.WEST, xOffsetField, 0, SpringLayout.EAST, x0Label);
      layout.putConstraint(SpringLayout.WEST, y0Label, GAP, SpringLayout.EAST, xOffsetField);
      layout.putConstraint(SpringLayout.WEST, yOffsetField, 0, SpringLayout.EAST, y0Label);

      layout.putConstraint(SpringLayout.WEST, htLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, lineHeightField, 0, SpringLayout.EAST, htLabel);
      layout.putConstraint(SpringLayout.WEST, spacerLabel, GAP, SpringLayout.EAST, lineHeightField);
      layout.putConstraint(SpringLayout.WEST, lineSpacerField, 0, SpringLayout.EAST, spacerLabel);
      
      layout.putConstraint(SpringLayout.WEST, binsLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, numBinsField, 0, SpringLayout.EAST, binsLabel);
      layout.putConstraint(SpringLayout.WEST, limLabel, GAP, SpringLayout.EAST, numBinsField);
      layout.putConstraint(SpringLayout.WEST, rngStartField, 0, SpringLayout.EAST, limLabel);
      layout.putConstraint(SpringLayout.WEST, rngEndField, GAP, SpringLayout.EAST, rngStartField);
      layout.putConstraint(SpringLayout.WEST, avgChk, GAP*2, SpringLayout.EAST, rngEndField);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, avgChk);

      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);
      
      // top-bottom constraints: 7 rows. One widget per row is used to set the constraints with row above or below. The 
      // remaining widgets in a row are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, titleEditor, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, dsEditor, GAP*2, SpringLayout.SOUTH, titleEditor);
      layout.putConstraint(SpringLayout.NORTH, rngLabel, GAP, SpringLayout.SOUTH, dsEditor);
      layout.putConstraint(SpringLayout.NORTH, modeCombo, GAP, SpringLayout.SOUTH, rngLabel);
      layout.putConstraint(SpringLayout.NORTH, lineHeightField, GAP, SpringLayout.SOUTH, modeCombo);
      layout.putConstraint(SpringLayout.NORTH, numBinsField, GAP, SpringLayout.SOUTH, lineHeightField);
      
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP*2, SpringLayout.SOUTH, numBinsField);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, drawStyleEditor);
      
      String vCtr = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vCtr, showInLegendChk, 0, vCtr, titleEditor);
      layout.putConstraint(vCtr, x0Label, 0, vCtr, modeCombo);
      layout.putConstraint(vCtr, y0Label, 0, vCtr, modeCombo);
      layout.putConstraint(vCtr, xOffsetField, 0, vCtr, modeCombo);
      layout.putConstraint(vCtr, yOffsetField, 0, vCtr, modeCombo);
      layout.putConstraint(vCtr, htLabel, 0, vCtr, lineHeightField);
      layout.putConstraint(vCtr, spacerLabel, 0, vCtr, lineHeightField);
      layout.putConstraint(vCtr, lineSpacerField, 0, vCtr, lineHeightField);
      layout.putConstraint(vCtr, binsLabel, 0, vCtr, numBinsField);
      layout.putConstraint(vCtr, limLabel, 0, vCtr, numBinsField);
      layout.putConstraint(vCtr, rngStartField, 0, vCtr, numBinsField);
      layout.putConstraint(vCtr, rngEndField, 0, vCtr, numBinsField);
      layout.putConstraint(vCtr, avgChk, 0, vCtr, numBinsField);
   }

   @Override public void reload(boolean initial)
   {
      RasterNode raster = getRasterNode();
      if(raster == null) return;
      
      showInLegendChk.setSelected(raster.getShowInLegend());
      titleEditor.loadContent(raster.getAttributedTitle(false), raster.getFontFamily(), 0, raster.getFillColor(), 
            raster.getFontStyle());
      
      dsEditor.reload(raster);
      
      DataSet ds = raster.getDataSet();
      String label = "Observed sample range: ";
      label += Utilities.toString(ds.getXMin(), 4, -1) + " .. " + Utilities.toString(ds.getXMax(), 4, -1);
      rngLabel.setText(label);
      
      modeCombo.setSelectedItem(raster.getMode());
      xOffsetField.setValue(raster.getXOffset());
      yOffsetField.setValue(raster.getBaseline());

      lineHeightField.setValue(raster.getLineHeight());
      lineSpacerField.setValue(raster.getLineSpacer());
      numBinsField.setValue(raster.getNumBins());
      rngStartField.setValue(raster.getHistogramRangeStart());
      rngEndField.setValue(raster.getHistogramRangeEnd());
      avgChk.setSelected(raster.getAveraged());

      DisplayMode dm = raster.getMode();
      
      boolean isTrain= (dm == DisplayMode.TRAINS || dm==DisplayMode.TRAINS2);
      lineHeightField.setEnabled(isTrain);
      lineSpacerField.setEnabled(dm==DisplayMode.TRAINS);
      numBinsField.setEnabled(!isTrain);
      rngStartField.setEnabled(!isTrain);
      rngEndField.setEnabled(!isTrain);
      avgChk.setEnabled(dm == DisplayMode.HISTOGRAM);
      
      drawStyleEditor.loadGraphicNode(raster);
   }

   /** Close any modeless pop-up windows associated with the embedded draw styles editor. */
   @Override void onLowered() { drawStyleEditor.cancelEditing(); }

   /**
    * If the focus is on the title field, the special character is inserted at the current caret position. The user will
    * still have to hit the "Enter" key or shift focus away from the title field to submit the changes.
    */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(titleEditor.isFocusOwner()) titleEditor.insertString(s, false);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.RASTER == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_RASTER_32); }
   @Override String getRepresentativeTitle() { return("Raster Properties"); }

   @Override public void actionPerformed(ActionEvent e)
   {
      RasterNode raster = getRasterNode();
      if(raster == null) return;
      Object src = e.getSource();

      boolean ok = true; 
      if(src == titleEditor)
      {
         String text = FGraphicNode.toStyledText(
               titleEditor.getCurrentContents(), raster.getFontStyle(), raster.getFillColor());
         ok = raster.setTitle(text);
         if(!ok) titleEditor.loadContent(raster.getAttributedTitle(false), raster.getFontFamily(), 0, 
               raster.getFillColor(), raster.getFontStyle());
      }
      else if(src == showInLegendChk)
         raster.setShowInLegend(showInLegendChk.isSelected());
      else if(src == modeCombo)
      {
         DisplayMode dm = (DisplayMode) modeCombo.getSelectedItem();
         boolean isTrain = (dm == DisplayMode.TRAINS || dm == DisplayMode.TRAINS2);
         lineHeightField.setEnabled(isTrain);
         lineSpacerField.setEnabled(dm==DisplayMode.TRAINS);
         numBinsField.setEnabled(!isTrain);
         rngStartField.setEnabled(!isTrain);
         rngEndField.setEnabled(!isTrain);
         avgChk.setEnabled(dm==DisplayMode.HISTOGRAM);
         
         raster.setMode(dm);
      }
      else if(src == xOffsetField)
      {
         ok = raster.setXOffset(xOffsetField.getValue().floatValue());
         if(!ok) xOffsetField.setValue(raster.getXOffset());
      }
      else if(src == yOffsetField)
      {
         ok = raster.setBaseline(yOffsetField.getValue().floatValue());
         if(!ok) yOffsetField.setValue(raster.getBaseline());
      }
      else if(src == lineHeightField)
      {
         ok = raster.setLineHeight(lineHeightField.getValue().intValue());
         if(!ok) lineHeightField.setValue(raster.getLineHeight());
      }
      else if(src == lineSpacerField)
      {
         ok = raster.setLineSpacer(lineSpacerField.getValue().intValue());
         if(!ok) lineSpacerField.setValue(raster.getLineSpacer());
      }
      else if(src == numBinsField)
      {
         ok = raster.setNumBins(numBinsField.getValue().intValue());
         if(!ok) numBinsField.setValue(raster.getNumBins());
      }
      else if(src == rngStartField || src == rngEndField)
      {
         ok = raster.setHistogramRange(rngStartField.getValue().doubleValue(), rngEndField.getValue().doubleValue());
         if(!ok) 
         {
            rngStartField.setValue(raster.getHistogramRangeStart());
            rngEndField.setValue(raster.getHistogramRangeEnd());
         }
      }
      else if(src == avgChk)
         raster.setAveraged(avgChk.isSelected());

      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link RasterNode}. */
   private RasterNode getRasterNode() { return((RasterNode) getEditedNode()); }
   
   /** Label displays the observed sample range in the 1D data set assigned to the raster node. */
   private JLabel rngLabel = null;
   
   /** When checked, an entry for the raster is included in parent graph's legend. */
   private JCheckBox showInLegendChk = null;

   /** The custom widget within which the raster node's title (which may contain attributed text) is edited.  */
   private StyledTextEditor titleEditor = null;

   /** Self-contained editor handles editing/loading of the data set assigned to the raster node. */
   private FGNPlottableDSCard dsEditor = null;
   
   /** Combo box selects the raster display mode. */
   private JComboBox<DisplayMode> modeCombo = null;

   /** Numeric text field sets the X-offset for the raster. */
   private NumericTextField xOffsetField = null;

   /** Numeric text field sets the Y-offset for the raster. */
   private NumericTextField yOffsetField = null;

   /** Numeric text field sets the raster line height; "trains" and "trains2" display modes only. */
   private NumericTextField lineHeightField = null;
   
   /** Numeric text field sets the raster line spacer in stroke width units; "trains" mode only. */
   private NumericTextField lineSpacerField = null;
   
   /** Numeric text field sets the number of histogram bins; histogram display modes only. */
   private NumericTextField numBinsField = null;
   
   /** Numeric text field sets the start of the desired histogram sample range; histogram display modes only. */
   private NumericTextField rngStartField = null;
   
   /** Numeric text field sets the end of the desired histogram sample range; histogram display modes only. */
   private NumericTextField rngEndField = null;
   
   /** Sets the state of the raster node's <i>avg</i> flag; {@link DisplayMode#HISTOGRAM} mode only. */
   private JCheckBox avgChk = null;

   /** Self-contained editor handles the node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
