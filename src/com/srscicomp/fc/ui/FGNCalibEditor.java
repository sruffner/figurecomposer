package com.srscicomp.fc.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.SpringLayout;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.fc.fig.CalibrationBarNode;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
 * <code>FGNCalibEditor</code> displays/edits all properties of a {@link CalibrationBarNode}. It
 * includes a {@link TextStyleEditor} and {@link DrawStyleEditor} to expose the node's style properties, plus widgets 
 * to specify the (x,y) coordinates of the calibration bar's center point, whether it is aligned with the primary or 
 * secondary axis of the parent graph, whether or not its label is automatically generated, and the type and size of its
 * end-cap adornments.
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNCalibEditor extends FGNEditor implements ActionListener
{

   /** Construct the text label node properties editor. */
   FGNCalibEditor()
   {
      super();
      
      primaryBtn = new JRadioButton("X(\u03B8)");    // u03b8 = Greek capital theta
      primaryBtn.addActionListener(this);
      primaryBtn.setContentAreaFilled(false);
      add(primaryBtn);
      secondaryBtn = new JRadioButton("Y(R)");
      secondaryBtn.addActionListener(this);
      secondaryBtn.setContentAreaFilled(false);
      add(secondaryBtn);
      
      ButtonGroup g = new ButtonGroup();
      g.add(primaryBtn);
      g.add(secondaryBtn);
      
      lengthField = new NumericTextField(0, Double.MAX_VALUE, 4);
      lengthField.setColumns(8);
      lengthField.setToolTipText("Calibration bar length in native units");
      lengthField.addActionListener(this);
      add(lengthField);
      
      autoLabelCheckBox = new JCheckBox("Auto label?");
      autoLabelCheckBox.setContentAreaFilled(false);
      autoLabelCheckBox.addActionListener(this);
      add(autoLabelCheckBox);
      
      Measure.Constraints c = FGraphicModel.getLocationConstraints(FGNodeType.CALIB);

      JLabel xLabel = new JLabel("X= ");
      add(xLabel);
      xEditor = new MeasureEditor(8, c);
      xEditor.setToolTipText("X-coord of calibration bar's center point");
      xEditor.addActionListener(this);
      add(xEditor);
      
      JLabel yLabel = new JLabel("Y= ");
      add(yLabel);
      yEditor = new MeasureEditor(8, c);
      yEditor.setToolTipText("Y-coord of calibration bar's center point");
      yEditor.addActionListener(this);
      add(yEditor);
      
      JLabel capLabel = new JLabel("End caps: ");
      add(capLabel);
      endcapCombo = new JComboBox<Marker>(Marker.values());
      endcapCombo.setToolTipText("Select endcap shape");
      endcapCombo.addActionListener(this);
      add(endcapCombo);
      
      endcapSizeEditor = new MeasureEditor(0, CalibrationBarNode.ENDCAPSIZECONSTRAINTS);
      endcapSizeEditor.setToolTipText("Enter size of endcap's bounding box");
      endcapSizeEditor.addActionListener(this);
      add(endcapSizeEditor);
      
      textStyleEditor = new TextStyleEditor();
      add(textStyleEditor);

      drawStyleEditor = new DrawStyleEditor(true, true);
      add(drawStyleEditor);
      
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints.
      layout.putConstraint(SpringLayout.WEST, primaryBtn, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, secondaryBtn, GAP, SpringLayout.EAST, primaryBtn);
      layout.putConstraint(SpringLayout.WEST, lengthField, GAP*3, SpringLayout.EAST, secondaryBtn);
      layout.putConstraint(SpringLayout.WEST, autoLabelCheckBox, GAP*3, SpringLayout.EAST, lengthField);

      layout.putConstraint(SpringLayout.WEST, xLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, xEditor, 0, SpringLayout.EAST, xLabel);
      layout.putConstraint(SpringLayout.WEST, yLabel, GAP*2, SpringLayout.EAST, xEditor);
      layout.putConstraint(SpringLayout.WEST, yEditor, 0, SpringLayout.EAST, yLabel);

      layout.putConstraint(SpringLayout.WEST, capLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, endcapCombo, 0, SpringLayout.EAST, capLabel);
      layout.putConstraint(SpringLayout.WEST, endcapSizeEditor, GAP, SpringLayout.EAST, endcapCombo);

      // the anchor row (longest)
      layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, textStyleEditor);
      
      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);
      
      // top-bottom constraints: 5 rows, with text and draw style editors in last two. In each of first two rows, one
      // widget sets constraints with row above or below. Remaining widgets are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, lengthField, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, xEditor, GAP, SpringLayout.SOUTH, lengthField);
      layout.putConstraint(SpringLayout.NORTH, endcapCombo, GAP, SpringLayout.SOUTH, xEditor);
      layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, endcapCombo);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, drawStyleEditor);
      
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, primaryBtn, 0, SpringLayout.VERTICAL_CENTER, lengthField);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, secondaryBtn, 0, SpringLayout.VERTICAL_CENTER, lengthField);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, autoLabelCheckBox, 0, SpringLayout.VERTICAL_CENTER, lengthField);
      
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, xLabel, 0, SpringLayout.VERTICAL_CENTER, xEditor);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, yLabel, 0, SpringLayout.VERTICAL_CENTER, xEditor);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, yEditor, 0, SpringLayout.VERTICAL_CENTER, xEditor);
 
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, capLabel, 0, SpringLayout.VERTICAL_CENTER, endcapCombo);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, endcapSizeEditor, 0, SpringLayout.VERTICAL_CENTER, endcapCombo);
   }

   @Override public void reload(boolean initial)
   {
      CalibrationBarNode calib = getCalibNode();
      
      xEditor.setMeasure(calib.getX());
      yEditor.setMeasure(calib.getY());
      lengthField.setValue(calib.getLength());
      primaryBtn.setSelected(calib.isPrimary());
      secondaryBtn.setSelected(!calib.isPrimary());
      endcapCombo.setSelectedItem(calib.getEndCap());
      endcapSizeEditor.setMeasure(calib.getEndCapSize());
      autoLabelCheckBox.setSelected(calib.isAutoLabelOn());

      textStyleEditor.loadGraphicNode(calib);
      drawStyleEditor.loadGraphicNode(calib);
   }

   /** 
    * Ensure the modeless pop-up window associated with text/fill color or stroke color picker button is extinguished 
    * before the editor panel is hidden.
    */
   @Override void onLowered()
   {
      textStyleEditor.cancelEditing();
      drawStyleEditor.cancelEditing();
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.CALIB == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_CALIB_32); }
   @Override String getRepresentativeTitle() { return("Calibration Bar Properties"); }

   @Override public void actionPerformed(ActionEvent e)
   {
      CalibrationBarNode calib = getCalibNode();
      if(calib == null) return;
      Object src = e.getSource();

      boolean ok = true;
      if(src == xEditor)
      {
         ok = calib.setX(xEditor.getMeasure());
         if(!ok) xEditor.setMeasure(calib.getX());
      }
      else if(src == yEditor)
      {
         ok = calib.setY(yEditor.getMeasure());
         if(!ok) yEditor.setMeasure(calib.getY());
      }
      else if(src == lengthField)
      {
         ok = calib.setLength(lengthField.getValue().doubleValue());
         if(!ok) lengthField.setValue(calib.getLength());
      }
      else if(src == primaryBtn || src == secondaryBtn)
         calib.setPrimary(primaryBtn.isSelected());
      else if(src == autoLabelCheckBox)
         calib.setAutoLabelOn(autoLabelCheckBox.isSelected());
      else if(src == endcapCombo)
      {
         ok = calib.setEndCap((Marker)endcapCombo.getSelectedItem());
         if(!ok) endcapCombo.setSelectedItem(calib.getEndCap());
      }
      else if(src == endcapSizeEditor)
      {
         ok = calib.setEndCapSize(endcapSizeEditor.getMeasure());
         if(!ok) endcapSizeEditor.setMeasure(calib.getEndCapSize());
      }
      
      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link CalibrationBarNode}. */
   private CalibrationBarNode getCalibNode() { return((CalibrationBarNode) getEditedNode()); }
   
   /** Customized component for editing the x-coordinate of the calibration bar's location. */
   private MeasureEditor xEditor = null;

   /** Customized component for editing the y-coordinate of the calibration bar's location. */
   private MeasureEditor yEditor = null;

   /** Numeric text field for editing the calibration bar's native length. */
   private NumericTextField lengthField = null;

   /** Radio button associates calibration bar with primary (horizontal/theta) axis of parent graph. */
   private JRadioButton primaryBtn = null;
   
   /** Radio button associates calibration bar with secondary (vertical/radial) axis of parent graph. */
   private JRadioButton secondaryBtn = null;
   
   private JCheckBox autoLabelCheckBox = null;
   
   /** Combo box for selecting the endcap adornment type. */
   private JComboBox<Marker> endcapCombo = null;

   /** Customized component for editing the endcap adornment's size. */
   private MeasureEditor endcapSizeEditor = null;
   
   /** Self-contained editor handles the label node's text styles (it has no draw styles). */
   private TextStyleEditor textStyleEditor = null;
   
   /** Self-contained editor handles the node's graphic style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
