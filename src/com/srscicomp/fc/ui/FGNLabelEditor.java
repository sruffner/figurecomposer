package com.srscicomp.fc.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;


import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.ui.MultiButton;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.LabelNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.uibase.StyledTextEditor;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
 * <b>FGNLabelEditor</b> displays/edits all properties of a {@link LabelNode}. It includes widgets to specify the (x,y) 
 * coordinates of the label's starting position, its orientation (a rotation about the starting position), horizontal 
 * and vertical alignment, an optional figure-wide ID (rarely used), and an embedded {@link TextStyleEditor} to set the 
 * label node's text-related style properties.
 * 
 * <p>As of v5.4.0, the label string is "attributed text" -- in which the text color, font style, underline state, and 
 * superscript/subscript state can be changed by the user on a character-by-character basis. A custom widget, {@link 
 * StyledTextEditor} handles the display and editing of the text label. See that class or <b>LabelNode</b> for 
 * details regarding the format of the attributed text string.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNLabelEditor extends FGNEditor implements ActionListener, ItemListener, FocusListener
{
   /** Construct the text label node properties editor. */
   FGNLabelEditor()
   {
      super();
      
      labelEditor = new StyledTextEditor(1);
      labelEditor.addActionListener(this);
      add(labelEditor);
      
      JLabel idLabel = new JLabel("ID (optional): ");
      add(idLabel);
      idField = new JTextField(8);
      idField.setToolTipText(FGNEditor.IDATTR_TIP);
      idField.addActionListener(this);
      idField.addFocusListener(this);
      add(idField);
      
      JLabel xLabel = new JLabel("X= ");
      add(xLabel);
      Measure.Constraints c = FGraphicModel.getLocationConstraints(FGNodeType.LABEL);
      xEditor = new MeasureEditor(6, c);
      xEditor.setToolTipText("X-coord of label's starting position");
      xEditor.addActionListener(this);
      add(xEditor);
      
      JLabel yLabel = new JLabel("Y= ");
      add(yLabel);
      yEditor = new MeasureEditor(6, c);
      yEditor.setToolTipText("Y-coord of label's starting position");
      yEditor.addActionListener(this);
      add(yEditor);
      
      JLabel rotLabel = new JLabel("\u03b8= ");
      add(rotLabel);
      rotateField = new NumericTextField(-180.0, 180.0, 2);
      rotateField.setToolTipText("Orientation (rotation about starting position)");
      rotateField.addActionListener(this);
      add(rotateField);
      
      hAlignWidget = new MultiButton<TextAlign>();
      hAlignWidget.addChoice(TextAlign.LEADING, FCIcons.V4_ALIGNLEFT_16, "Left");
      hAlignWidget.addChoice(TextAlign.CENTERED, FCIcons.V4_ALIGNCENTER_16, "Center");
      hAlignWidget.addChoice(TextAlign.TRAILING, FCIcons.V4_ALIGNRIGHT_16, "Right");
      hAlignWidget.addItemListener(this);
      hAlignWidget.setToolTipText("Horizontal alignment");
      add(hAlignWidget);
      
      vAlignWidget = new MultiButton<TextAlign>();
      vAlignWidget.addChoice(TextAlign.LEADING, FCIcons.V4_VALIGNTOP_16, "Top");
      vAlignWidget.addChoice(TextAlign.CENTERED, FCIcons.V4_VALIGNMID_16, "Middle");
      vAlignWidget.addChoice(TextAlign.TRAILING, FCIcons.V4_VALIGNBOT_16, "Bottom");
      vAlignWidget.addItemListener(this);
      vAlignWidget.setToolTipText("Vertical alignment");
      add(vAlignWidget);
      
      styleEditor = new TextStyleEditor();
      add(styleEditor);

      SpringLayout layout = new SpringLayout();
      setLayout(layout);

      // left-right constraints.
      layout.putConstraint(SpringLayout.WEST, labelEditor, GAP, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, labelEditor, -GAP, SpringLayout.EAST, this);
      
      layout.putConstraint(SpringLayout.WEST, idLabel, GAP, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, idField, 0, SpringLayout.EAST, idLabel);
      layout.putConstraint(SpringLayout.EAST, idField, -GAP, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, xLabel, GAP, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, xEditor, 0, SpringLayout.EAST, xLabel);
      layout.putConstraint(SpringLayout.WEST, yLabel, GAP, SpringLayout.EAST, xEditor);
      layout.putConstraint(SpringLayout.WEST, yEditor, 0, SpringLayout.EAST, yLabel);
      layout.putConstraint(SpringLayout.WEST, rotLabel, GAP, SpringLayout.EAST, yEditor);
      layout.putConstraint(SpringLayout.WEST, rotateField, 0, SpringLayout.EAST, rotLabel);
      layout.putConstraint(SpringLayout.WEST, hAlignWidget, GAP*2, SpringLayout.EAST, rotateField);
      layout.putConstraint(SpringLayout.WEST, vAlignWidget, GAP, SpringLayout.EAST, hAlignWidget);
      
      // anchor row -- sets panel width
      layout.putConstraint(SpringLayout.WEST, styleEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, styleEditor);
      
      // top-bottom constraints: 4 rows, with text style editor in last row. In each of first 3 rows, one widget is used
      // to set the constraints with row above or below. The remaining widgets are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, labelEditor, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, idField, GAP*3, SpringLayout.SOUTH, labelEditor);
      layout.putConstraint(SpringLayout.NORTH, rotateField, GAP, SpringLayout.SOUTH, idField);
      layout.putConstraint(SpringLayout.NORTH, styleEditor, GAP*2, SpringLayout.SOUTH, rotateField);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, styleEditor);
      
      String vCtr = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vCtr, idLabel, 0, vCtr, idField);
      
      layout.putConstraint(vCtr, xLabel, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, xEditor, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, yLabel, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, yEditor, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, rotLabel, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, hAlignWidget, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, vAlignWidget, 0, vCtr, rotateField);
   }

   /** Flag set so that actions triggered by loading the widgets are ignored. */
   private boolean isReloading = false;
   
   @Override public void reload(boolean initial)
   {
      isReloading = true;
      try
      {
         LabelNode label = getLabelNode();
         

         labelEditor.loadContent(label.getAttributedTitle(false), label.getFontFamily(), 0, 
               label.getFillColor(), label.getFontStyle());
         idField.setText(label.getID());
         xEditor.setMeasure(label.getX());
         yEditor.setMeasure(label.getY());
         rotateField.setValue(label.getRotate());

         hAlignWidget.setCurrentChoice(label.getHorizontalAlignment());
         vAlignWidget.setCurrentChoice(label.getVerticalAlignment());

         styleEditor.loadGraphicNode(label);
      }
      finally { isReloading = false; }
   }

   /** Ensure any modeless pop-up windows associated with the editor panel are extinguished before it is hidden. */
   @Override void onLowered() { styleEditor.cancelEditing(); }

   /**
    * If the focus is on the label editor or ID field, the special character is inserted at the current caret position.
    * The user will still have to hit the "Enter" key or shift focus away from the text field to submit the changes.
    */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(labelEditor.isFocusOwner()) labelEditor.insertString(s, false);
      else if(idField.isFocusOwner()) idField.replaceSelection(s);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.LABEL == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_LABEL_32); }
   @Override String getRepresentativeTitle() { return("Text Label Properties"); }

   @Override public void focusGained(FocusEvent e) 
   {
      if(e.isTemporary()) return;
      if(e.getSource() == idField) idField.selectAll();
   }
   @Override public void focusLost(FocusEvent e)
   {
      if(e.isTemporary()) return;
      if(e.getSource() == idField) idField.postActionEvent();
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      LabelNode label = getLabelNode();
      if(label == null || isReloading) return;
      Object src = e.getSource();

      boolean ok = true;
      if(src == labelEditor)
      {
         String text = FGraphicNode.toStyledText(
               labelEditor.getCurrentContents(), label.getFontStyle(), label.getFillColor());
         ok = label.setTitle(text);
         if(!ok) labelEditor.loadContent(label.getAttributedTitle(false), label.getFontFamily(), 0, 
               label.getFillColor(), label.getFontStyle());
      }
      else if(src == idField)
      {
         ok = label.setID(idField.getText());
         if(!ok) idField.setText(label.getID());
      }
      else if(src == xEditor)
      {
         ok = label.setX(xEditor.getMeasure());
         if(!ok) xEditor.setMeasure(label.getX());
      }
      else if(src == yEditor)
      {
         ok = label.setY(yEditor.getMeasure());
         if(!ok) yEditor.setMeasure(label.getY());
      }
      else if(src == rotateField)
      {
         ok = label.setRotate(rotateField.getValue().doubleValue());
         if(!ok) rotateField.setValue(label.getRotate());
      }
      
      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   @Override public void itemStateChanged(ItemEvent e)
   {
      LabelNode label = getLabelNode();
      if(label == null || isReloading) return;
      Object src = e.getSource();

      if(src == hAlignWidget)
         label.setHorizontalAlignment(hAlignWidget.getCurrentChoice());
      else if(src == vAlignWidget)
         label.setVerticalAlignment(vAlignWidget.getCurrentChoice());
   }
   
   
   /** Convenience method casts the edited node to an instance of {@link LabelNode}. */
   private LabelNode getLabelNode() { return((LabelNode) getEditedNode()); }
   
   /** The custom widget within which the label's attributed text is edited. */
   private StyledTextEditor labelEditor = null;
   
   /** Text field in which optional node ID is edited. */
   private JTextField idField = null;
   
   /** Customized component for editing the x-coordinate of the label's starting position. */
   private MeasureEditor xEditor = null;

   /** Customized component for editing the y-coordinate of the label's starting position. */
   private MeasureEditor yEditor = null;

   /** Numeric text field for editing the label's orientation angle. */
   private NumericTextField rotateField = null;

   /** Multiple-choice button widget for editing the label's horizontal text alignment. */
   private MultiButton<TextAlign> hAlignWidget = null;
   
   /** Multiple-choice button widget for editing the label's vertical text alignment. */
   private MultiButton<TextAlign> vAlignWidget = null;
   
   /** Self-contained editor handles the label node's text styles (it has no draw styles). */
   private TextStyleEditor styleEditor = null;
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
