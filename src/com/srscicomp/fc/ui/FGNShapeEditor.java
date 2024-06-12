package com.srscicomp.fc.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SpringLayout;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.ui.BkgFillPicker;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.fig.ShapeNode;
import com.srscicomp.fc.uibase.StyledTextEditor;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
 * <b>FGNShapeEditor</b> displays/edits all properties of a {@link ShapeNode}. It includes a {@link
 * TextStyleEditor} and {@link DrawStyleEditor} to expose the node's style properties, plus widgets to specify the shape
 * type, the (x,y) coordinates of its center point, the width and height of its bounding box, its orientation (rotation 
 * about the center point), and the text of an interior label (which is centered both horizontally and vertically about
 * the center point). 
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNShapeEditor extends FGNEditor implements ActionListener, PropertyChangeListener
{

   /** Construct the shape node properties editor. */
   FGNShapeEditor()
   {
      super();
      
      labelEditor = new StyledTextEditor(1);
      labelEditor.addActionListener(this);
      labelEditor.setToolTipText("Enter a label for the shape; it appears centered within the shape");
      add(labelEditor);
      
      typeCombo = new JComboBox<Marker>(Marker.values());
      typeCombo.setToolTipText("Select shape design");
      typeCombo.addActionListener(this);
      add(typeCombo);
      
      JLabel xLabel = new JLabel("X= ");
      add(xLabel);
      Measure.Constraints c = FGraphicModel.getLocationConstraints(FGNodeType.SHAPE);
      xEditor = new MeasureEditor(7, c);
      xEditor.setToolTipText("X-coord of shape's center point");
      xEditor.addActionListener(this);
      add(xEditor);
      
      JLabel yLabel = new JLabel("Y= ");
      add(yLabel);
      yEditor = new MeasureEditor(7, c);
      yEditor.setToolTipText("Y-coord of shape's center point");
      yEditor.addActionListener(this);
      add(yEditor);
      
      JLabel wLabel = new JLabel("W= ");
      add(wLabel);
      c = FGraphicModel.getSizeConstraints(FGNodeType.SHAPE);
      wEditor = new MeasureEditor(7, c);
      wEditor.setToolTipText("Width of shape's bounding box");
      wEditor.addActionListener(this);
      add(wEditor);
      
      JLabel hLabel = new JLabel("H= ");
      add(hLabel);
      hEditor = new MeasureEditor(7, c);
      hEditor.setToolTipText("Height of shape's bounding box");
      hEditor.addActionListener(this);
      add(hEditor);
      
      JLabel rotLabel = new JLabel("\u03b8= ");
      add(rotLabel);
      rotateField = new NumericTextField(-180.0, 180.0, 2);
      rotateField.setToolTipText("Orientation (rotation about center point)");
      rotateField.addActionListener(this);
      add(rotateField);
      
      bkgFillPicker = new BkgFillPicker(50, 50);
      bkgFillPicker.setToolTipText("Background fill");
      bkgFillPicker.addPropertyChangeListener(BkgFillPicker.BKGFILL_PROPERTY, this);
      add(bkgFillPicker);
      
      textStyleEditor = new TextStyleEditor();
      add(textStyleEditor);

      drawStyleEditor = new DrawStyleEditor(true, false);
      add(drawStyleEditor);
      
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. Assume second row will be longest.
      layout.putConstraint(SpringLayout.WEST, labelEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, labelEditor, 0, SpringLayout.EAST, bkgFillPicker);

      layout.putConstraint(SpringLayout.WEST, typeCombo, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, typeCombo, 0, SpringLayout.EAST, yEditor);
      
      layout.putConstraint(SpringLayout.WEST, xLabel, GAP, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, xEditor, 0, SpringLayout.EAST, xLabel);
      layout.putConstraint(SpringLayout.WEST, yLabel, GAP, SpringLayout.EAST, xEditor);
      layout.putConstraint(SpringLayout.WEST, yEditor, 0, SpringLayout.EAST, yLabel);
      layout.putConstraint(SpringLayout.WEST, rotLabel, GAP, SpringLayout.EAST, yEditor);
      layout.putConstraint(SpringLayout.WEST, rotateField, 0, SpringLayout.EAST, rotLabel);
      layout.putConstraint(SpringLayout.WEST, bkgFillPicker, GAP*2, SpringLayout.EAST, rotateField);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, bkgFillPicker);
      
      layout.putConstraint(SpringLayout.EAST, wLabel, 0, SpringLayout.EAST, xLabel);
      layout.putConstraint(SpringLayout.WEST, wEditor, 0, SpringLayout.WEST, xEditor);
      layout.putConstraint(SpringLayout.EAST, hLabel, 0, SpringLayout.EAST, yLabel);
      layout.putConstraint(SpringLayout.WEST, hEditor, 0, SpringLayout.WEST, yEditor);

      layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, textStyleEditor, 0, SpringLayout.EAST, this);
      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);
      
      // top-bottom constraints: 6 rows, with text and draw style editors in last two. In multi-widget rows, one
      // widget sets constraints with row above or below. Remaining widgets are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, labelEditor, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, typeCombo, GAP*2, SpringLayout.SOUTH, labelEditor);
      layout.putConstraint(SpringLayout.NORTH, xEditor, GAP, SpringLayout.SOUTH, typeCombo);
      layout.putConstraint(SpringLayout.NORTH, wEditor, GAP, SpringLayout.SOUTH, xEditor);
      layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, wEditor);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, drawStyleEditor);
      
      String vCtr = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vCtr, xLabel, 0, vCtr, xEditor);
      layout.putConstraint(vCtr, yLabel, 0, vCtr, xEditor);
      layout.putConstraint(vCtr, yEditor, 0, vCtr, xEditor);
      layout.putConstraint(vCtr, rotLabel, 0, vCtr, xEditor);
      layout.putConstraint(vCtr, rotateField, 0, vCtr, xEditor);
      layout.putConstraint(vCtr, bkgFillPicker, 0, vCtr, xEditor);
      
      layout.putConstraint(vCtr, wLabel, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, hLabel, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, hEditor, 0, vCtr, wEditor);
   }

   @Override public void reload(boolean initial)
   {
      ShapeNode shape = getShapeNode();
      
      typeCombo.setSelectedItem(shape.getType());
      xEditor.setMeasure(shape.getX());
      yEditor.setMeasure(shape.getY());
      wEditor.setMeasure(shape.getWidth());
      hEditor.setMeasure(shape.getHeight());
      rotateField.setValue(shape.getRotate());
      bkgFillPicker.setCurrentFill(shape.getBackgroundFill(), false);
      
      labelEditor.loadContent(shape.getAttributedTitle(false), shape.getFontFamily(), 0, shape.getFillColor(), 
            shape.getFontStyle());

      textStyleEditor.loadGraphicNode(shape);
      drawStyleEditor.loadGraphicNode(shape);
   }

   /** Ensure pop-up windows for the text and draw style editors and the background fill widget are hidden. */
   @Override void onLowered()
   {
      textStyleEditor.cancelEditing();
      drawStyleEditor.cancelEditing();
      bkgFillPicker.cancelPopup();
   }

   /** If the focus is on the label editor, the special character is inserted at the current caret position. */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(labelEditor.isFocusOwner()) labelEditor.insertString(s, false);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.SHAPE == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_SHAPE_32); }
   @Override String getRepresentativeTitle() { return("Shape Properties"); }

   @Override public void propertyChange(PropertyChangeEvent e)
   {
      ShapeNode shape = getShapeNode();
      if(shape != null && e.getSource() == bkgFillPicker)
         shape.setBackgroundFill(bkgFillPicker.getCurrentFill());
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      ShapeNode shape = getShapeNode();
      if(shape == null) return;
      Object src = e.getSource();

      boolean ok = true;
      if(src == labelEditor)
      {
         String text = FGraphicNode.toStyledText(
               labelEditor.getCurrentContents(), shape.getFontStyle(), shape.getFillColor());
         ok = shape.setTitle(text);
         if(!ok) labelEditor.loadContent(shape.getAttributedTitle(false), shape.getFontFamily(), 0, 
               shape.getFillColor(), shape.getFontStyle());
      }
      else if(src == typeCombo)
      {
         ok = shape.setType((Marker)typeCombo.getSelectedItem());
         if(!ok) typeCombo.setSelectedItem(shape.getType());
      }
      else if(src == xEditor)
      {
         ok = shape.setX(xEditor.getMeasure());
         if(!ok) xEditor.setMeasure(shape.getX());
      }
      else if(src == yEditor)
      {
         ok = shape.setY(yEditor.getMeasure());
         if(!ok) yEditor.setMeasure(shape.getY());
      }
      else if(src == wEditor)
      {
         ok = shape.setWidth(wEditor.getMeasure());
         if(!ok) wEditor.setMeasure(shape.getWidth());
      }
      else if(src == hEditor)
      {
         ok = shape.setHeight(hEditor.getMeasure());
         if(!ok) hEditor.setMeasure(shape.getHeight());
      }
      else if(src == rotateField)
      {
         ok = shape.setRotate(rotateField.getValue().doubleValue());
         if(!ok) rotateField.setValue(shape.getRotate());
      }
      
      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link ShapeNode}. */
   private ShapeNode getShapeNode() { return((ShapeNode) getEditedNode()); }
   
   /** The custom widget within which the shape's label, which may be attributed text, is edited. */
   private StyledTextEditor labelEditor = null;
      
   /** Combo box for selecting the shape's design type. */
   private JComboBox<Marker> typeCombo = null;

   /** Customized component for editing the x-coordinate of the shape's location. */
   private MeasureEditor xEditor = null;

   /** Customized component for editing the y-coordinate of the shape's location. */
   private MeasureEditor yEditor = null;

   /** Customized component for editing the shape's width. */
   private MeasureEditor wEditor = null;

   /** Customized component for editing the shape's height. */
   private MeasureEditor hEditor = null;

   /** Numeric text field for editing the shape's orientation angle. */
   private NumericTextField rotateField = null;

   /** Compact widget uses a pop-up panel to edit the shape's background fill. */
   private BkgFillPicker bkgFillPicker = null;

   /** Self-contained editor handles the node's text style properties. */
   private TextStyleEditor textStyleEditor = null;
   
   /** Self-contained editor handles the node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
