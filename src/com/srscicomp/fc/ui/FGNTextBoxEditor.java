package com.srscicomp.fc.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.ui.BkgFillPicker;
import com.srscicomp.common.ui.MultiButton;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.fig.TextBoxNode;
import com.srscicomp.fc.uibase.StyledTextEditor;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
 * <b>FGNTextBoxEditor</b> displays and edits all properties of a {@link TextBoxNode}. It includes widgets to specify 
 * the (x,y) coordinates of the text box's BL corner; its width, height, and orientation (a rotation about the corner); 
 * and an inner margin or gap, the same on all sides, between the text box bounds and the rectangular area to which the 
 * text content is fit (assuming it fits). A check box indicates whether or not the text block is clipped to the 
 * bounding box less the margins. Multiple-choice button widgets set the H and V alignment of the text block with 
 * respect to the bounding box, and a custom button-like component selects the background fill (solid or gradient) for 
 * the bounding box. A numeric text widget specifies the font leading as a fraction of the current font size; the 
 * leading, in turn, controls the inter-line spacing of the rendered text block. An embedded {@link TextStyleEditor} and
 * {@link DrawStyleEditor} handle the text box node's inheritable text- and draw-related style properties.
 * 
 * <p>The text content itself is displayed in an {@link StyledTextEditor} at the top of the editor panel. After 
 * making changes to the content, the user must press the editor's "ok" button (or press "Shift-Enter" with the keyboard
 * focus on the editor's text pane) to save the changes and update the text box node accordingly. Special characters not
 * accessible from the keyboard may be inserted via the special characters tool dialog, which is tied into the node 
 * properties editor framework.</p>
 * 
 * @author sruffner
 */
class FGNTextBoxEditor extends FGNEditor implements ActionListener, ItemListener, PropertyChangeListener,
      FocusListener
{
   /** Construct the text box node properties editor. */
   FGNTextBoxEditor()
   {
      super();
      
      contentEditor = new StyledTextEditor(3);
      contentEditor.addActionListener(this);
      add(contentEditor);
      
      JLabel idLabel = new JLabel("ID (optional): ");
      add(idLabel);
      idField = new JTextField(8);
      idField.setToolTipText(FGNEditor.IDATTR_TIP);
      idField.addActionListener(this);
      idField.addFocusListener(this);
      add(idField);
      
      JLabel xLabel = new JLabel("X= ");
      add(xLabel);
      Measure.Constraints c = FGraphicModel.getLocationConstraints(FGNodeType.TEXTBOX);
      xEditor = new MeasureEditor(7, c);
      xEditor.setToolTipText("X-coordinate of bottom-left corner of bounding box");
      xEditor.addActionListener(this);
      add(xEditor);
      
      JLabel yLabel = new JLabel("Y= ");
      add(yLabel);
      yEditor = new MeasureEditor(7, c);
      yEditor.setToolTipText("Y-coordinate of bottom-left corner of bounding box");
      yEditor.addActionListener(this);
      add(yEditor);
      
      JLabel rotLabel = new JLabel("\u03b8= ");
      add(rotLabel);
      rotateField = new NumericTextField(-180.0, 180.0, 2);
      rotateField.setToolTipText("Orientation (rotation about bottom-left corner)");
      rotateField.addActionListener(this);
      add(rotateField);
      
      JLabel wLabel = new JLabel("W= ");
      add(wLabel);
      c = FGraphicModel.getSizeConstraints(FGNodeType.TEXTBOX);
      wEditor = new MeasureEditor(7, c);
      wEditor.setToolTipText("Width of bounding box");
      wEditor.addActionListener(this);
      add(wEditor);
      
      JLabel hLabel = new JLabel("H= ");
      add(hLabel);
      hEditor = new MeasureEditor(7, c);
      hEditor.setToolTipText("Height of bounding box");
      hEditor.addActionListener(this);
      add(hEditor);
      
      JLabel mLabel = new JLabel("M= ");
      add(mLabel);
      c = FGraphicNode.STROKEWCONSTRAINTS;
      marginEditor = new MeasureEditor(7, c);
      marginEditor.setToolTipText("Inner margin (0-1in)");
      marginEditor.addActionListener(this);
      add(marginEditor);
      
      clipChk = new JCheckBox("Clip?");
      clipChk.setContentAreaFilled(false);
      clipChk.addActionListener(this);
      add(clipChk);

      hAlignWidget = new MultiButton<>();
      hAlignWidget.addChoice(TextAlign.LEADING, FCIcons.V4_ALIGNLEFT_16, "Left");
      hAlignWidget.addChoice(TextAlign.CENTERED, FCIcons.V4_ALIGNCENTER_16, "Center");
      hAlignWidget.addChoice(TextAlign.TRAILING, FCIcons.V4_ALIGNRIGHT_16, "Right");
      hAlignWidget.addItemListener(this);
      hAlignWidget.setToolTipText("Horizontal alignment");
      add(hAlignWidget);
      
      vAlignWidget = new MultiButton<>();
      vAlignWidget.addChoice(TextAlign.LEADING, FCIcons.V4_VALIGNTOP_16, "Top");
      vAlignWidget.addChoice(TextAlign.CENTERED, FCIcons.V4_VALIGNMID_16, "Middle");
      vAlignWidget.addChoice(TextAlign.TRAILING, FCIcons.V4_VALIGNBOT_16, "Bottom");
      vAlignWidget.addItemListener(this);
      vAlignWidget.setToolTipText("Vertical alignment");
      add(vAlignWidget);
      
      bkgFillPicker = new BkgFillPicker(50, 50);
      bkgFillPicker.setToolTipText("Background fill");
      bkgFillPicker.addPropertyChangeListener(BkgFillPicker.BKGFILL_PROPERTY, this);
      add(bkgFillPicker);
      
      JLabel lineHtLabel = new JLabel("Line Ht: ");
      add(lineHtLabel);
      lineHtField = new NumericTextField(0.8, 3.0, 2);
      lineHtField.setToolTipText("Text line height as a fraction of font size [0.8 .. 3].");
      lineHtField.addActionListener(this);
      add(lineHtField);
      
      textStyleEditor = new TextStyleEditor();
      add(textStyleEditor);

      drawStyleEditor = new DrawStyleEditor(true, false);
      add(drawStyleEditor);

      SpringLayout layout = new SpringLayout();
      setLayout(layout);

      // left-right constraints.
      layout.putConstraint(SpringLayout.WEST, contentEditor, GAP, SpringLayout.WEST, this);
     layout.putConstraint(SpringLayout.EAST, contentEditor, -GAP, SpringLayout.EAST, this);
      
      layout.putConstraint(SpringLayout.WEST, idLabel, GAP, SpringLayout.WEST, wLabel);
      layout.putConstraint(SpringLayout.WEST, idField, 0, SpringLayout.EAST, idLabel);
      layout.putConstraint(SpringLayout.EAST, idField, -GAP, SpringLayout.EAST, this);

      // these are aligned with widgets on next row...
      layout.putConstraint(SpringLayout.WEST, xLabel, 0, SpringLayout.WEST, wLabel);
      layout.putConstraint(SpringLayout.WEST, xEditor, 0, SpringLayout.WEST, wEditor);
      layout.putConstraint(SpringLayout.EAST, yLabel, 0, SpringLayout.EAST, hLabel);
      layout.putConstraint(SpringLayout.WEST, yEditor, 0, SpringLayout.WEST, hEditor);
      layout.putConstraint(SpringLayout.WEST, rotLabel, GAP, SpringLayout.EAST, yEditor);
      layout.putConstraint(SpringLayout.WEST, rotateField, 0, SpringLayout.EAST, rotLabel);
      layout.putConstraint(SpringLayout.EAST, rotateField, 0, SpringLayout.EAST, marginEditor);

      layout.putConstraint(SpringLayout.WEST, wLabel, GAP, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, wEditor, 0, SpringLayout.EAST, wLabel);
      layout.putConstraint(SpringLayout.WEST, hLabel, GAP, SpringLayout.EAST, wEditor);
      layout.putConstraint(SpringLayout.WEST, hEditor, 0, SpringLayout.EAST, hLabel);
      layout.putConstraint(SpringLayout.WEST, mLabel, GAP, SpringLayout.EAST, hEditor);
      layout.putConstraint(SpringLayout.WEST, marginEditor, 0, SpringLayout.EAST, mLabel);
      layout.putConstraint(SpringLayout.WEST, bkgFillPicker, GAP*2, SpringLayout.EAST, marginEditor);

      layout.putConstraint(SpringLayout.WEST, lineHtLabel, 0, SpringLayout.WEST, wLabel);
      layout.putConstraint(SpringLayout.WEST, lineHtField, 0, SpringLayout.EAST, lineHtLabel);
      layout.putConstraint(SpringLayout.WEST, hAlignWidget, GAP*4, SpringLayout.EAST, lineHtField);
      layout.putConstraint(SpringLayout.WEST, vAlignWidget, GAP, SpringLayout.EAST, hAlignWidget);
      layout.putConstraint(SpringLayout.WEST, clipChk, GAP*4, SpringLayout.EAST, vAlignWidget);

      // this is the anchor row (the longest row, establishing width of the container)
      layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, textStyleEditor);

      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);

      // top-bottom constraints: 6 rows, with text and draw style editors in last two. In each of first 4 rows, one 
      // widget is used to set the constraints with row above or below. The remaining widgets are vertically centered 
      // WRT that widget. The layout constraints are used to fix the height of the text content scroll pane.
      layout.putConstraint(SpringLayout.NORTH, contentEditor, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, idField, GAP, SpringLayout.SOUTH, contentEditor);
      layout.putConstraint(SpringLayout.NORTH, rotateField, GAP*2, SpringLayout.SOUTH, idField);
      layout.putConstraint(SpringLayout.NORTH, wEditor, GAP, SpringLayout.SOUTH, rotateField);
      layout.putConstraint(SpringLayout.NORTH, hAlignWidget, GAP*2, SpringLayout.SOUTH, wEditor);
      layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, hAlignWidget);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, drawStyleEditor);
      
      String vCtr = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vCtr, idLabel, 0, vCtr, idField);
      
      layout.putConstraint(vCtr, xLabel, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, xEditor, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, yLabel, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, yEditor, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, rotLabel, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, bkgFillPicker, 0, SpringLayout.SOUTH, rotateField);

      layout.putConstraint(vCtr, wLabel, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, hLabel, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, hEditor, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, mLabel, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, marginEditor, 0, vCtr, wEditor);

      layout.putConstraint(vCtr, clipChk, 0, vCtr, hAlignWidget);
      layout.putConstraint(vCtr, vAlignWidget, 0, vCtr, hAlignWidget);
      layout.putConstraint(vCtr, lineHtLabel, 0, vCtr, hAlignWidget);
      layout.putConstraint(vCtr, lineHtField, 0, vCtr, hAlignWidget);
   }

   @Override public void reload(boolean initial)
   {
      TextBoxNode tbox = getTextBoxNode();
      
      contentEditor.loadContent(tbox.getAttributedTitle(false), tbox.getFontFamily(), 0, tbox.getFillColor(), 
            tbox.getFontStyle());

      idField.setText(tbox.getID());
      xEditor.setMeasure(tbox.getX());
      yEditor.setMeasure(tbox.getY());
      rotateField.setValue(tbox.getRotate());
      wEditor.setMeasure(tbox.getWidth());
      hEditor.setMeasure(tbox.getHeight());
      marginEditor.setMeasure(tbox.getMargin());
      clipChk.setSelected(tbox.getClip());
      hAlignWidget.setCurrentChoice(tbox.getHorizontalAlignment());
      vAlignWidget.setCurrentChoice(tbox.getVerticalAlignment());
      bkgFillPicker.setCurrentFill(tbox.getBackgroundFill(), false);
      lineHtField.setValue(tbox.getLineHeight());
      
      textStyleEditor.loadGraphicNode(tbox);
      drawStyleEditor.loadGraphicNode(tbox);
   }

   /** 
    * Ensure any modeless pop-up windows associated with any editor widgets are extinguished before the editor panel is
    * hidden. 
    */
   @Override void onLowered()
   {
      textStyleEditor.cancelEditing();
      drawStyleEditor.cancelEditing();
      bkgFillPicker.cancelPopup();
   }

   /** 
    * If the text content editor or the optional ID field is the current focus owner, then the special character will be
    * inserted at the current caret position, replacing any selected text.
    */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(contentEditor.isFocusOwner()) contentEditor.insertString(s, false);
      else if(idField.isFocusOwner()) idField.replaceSelection(s);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.TEXTBOX == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_TEXTBOX_32); }
   @Override String getRepresentativeTitle() { return("Text Box Properties"); }

   @Override public void propertyChange(PropertyChangeEvent e)
   {
      TextBoxNode tbox = getTextBoxNode();
      if(tbox != null && e.getSource() == bkgFillPicker)
         tbox.setBackgroundFill(bkgFillPicker.getCurrentFill());
   }

   @Override public void focusGained(FocusEvent e) 
   {
      if(!(e.isTemporary()) && (e.getSource() == idField)) idField.selectAll();
   }
   @Override public void focusLost(FocusEvent e)
   {
      if((!e.isTemporary()) && (e.getSource() == idField)) idField.postActionEvent();
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      TextBoxNode tbox = getTextBoxNode();
      if(tbox == null) return;
      Object src = e.getSource();

      boolean ok = true;
      if(src == contentEditor)
      {
         String text = FGraphicNode.toStyledText(
               contentEditor.getCurrentContents(), tbox.getFontStyle(), tbox.getFillColor());
         ok = tbox.setTitle(text);
         if(!ok) contentEditor.loadContent(tbox.getAttributedTitle(false), tbox.getFontFamily(), 0, tbox.getFillColor(), 
               tbox.getFontStyle());
      }
      else if(src == idField)
      {
         ok = tbox.setID(idField.getText());
         if(!ok) idField.setText(tbox.getID());
      }
      else if(src == xEditor)
      {
         ok = tbox.setX(xEditor.getMeasure());
         if(!ok) xEditor.setMeasure(tbox.getX());
      }
      else if(src == yEditor)
      {
         ok = tbox.setY(yEditor.getMeasure());
         if(!ok) yEditor.setMeasure(tbox.getY());
      }
      else if(src == rotateField)
      {
         ok = tbox.setRotate(rotateField.getValue().doubleValue());
         if(!ok) rotateField.setValue(tbox.getRotate());
      }
      else if(src == wEditor)
      {
         ok = tbox.setWidth(wEditor.getMeasure());
         if(!ok) wEditor.setMeasure(tbox.getWidth());
      }
      else if(src == hEditor)
      {
         ok = tbox.setHeight(hEditor.getMeasure());
         if(!ok) hEditor.setMeasure(tbox.getHeight());
      }
      else if(src == marginEditor)
      {
         ok = tbox.setMargin(marginEditor.getMeasure());
         if(!ok) marginEditor.setMeasure(tbox.getMargin());
      }
      else if(src == clipChk)
         tbox.setClip(clipChk.isSelected());
      else if(src == lineHtField)
      {
         ok = tbox.setLineHeight(lineHtField.getValue().doubleValue());
         if(!ok) lineHtField.setValue(tbox.getLineHeight());
      }
      
      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   @Override public void itemStateChanged(ItemEvent e)
   {
      TextBoxNode tbox = getTextBoxNode();
      if(tbox == null) return;
      Object src = e.getSource();

      if(src == hAlignWidget)
         tbox.setHorizontalAlignment(hAlignWidget.getCurrentChoice());
      else if(src == vAlignWidget)
         tbox.setVerticalAlignment(vAlignWidget.getCurrentChoice());
   }
   
   
   /** Convenience method casts the edited node to an instance of {@link TextBoxNode}. */
   private TextBoxNode getTextBoxNode() { return((TextBoxNode) getEditedNode()); }

   /** 
    * Text content is edited within this custom widget, which allows user to change font style or text color anywhere
    * in the content, add superscripts or subscripts, and underline portions of the text.
    */
   private final StyledTextEditor contentEditor;
   
   /** Text field in which optional node ID is edited. */
   private JTextField idField = null;
   
   /** Customized component for editing the x-coordinate of the text box's bottom-left corner. */
   private MeasureEditor xEditor = null;

   /** Customized component for editing the y-coordinate of the text box's bottom-left corner. */
   private MeasureEditor yEditor = null;

   /** Customized component for editing the text box width. */
   private MeasureEditor wEditor = null;

   /** Customized component for editing the text box height. */
   private MeasureEditor hEditor = null;

   /** Customized component for editing the text box's inner margin. */
   private MeasureEditor marginEditor = null;

   /** Numeric text field for editing the text box's rotation about its bottom-left corner. */
   private NumericTextField rotateField = null;

   /** Check box toggles state of the text box's clip flag. */
   private JCheckBox clipChk = null;

   /** Multiple-choice button widget for editing the H alignment of the text block WRT its bounding box. */
   private MultiButton<TextAlign> hAlignWidget = null;
   
   /** Multiple-choice button widget for editing the V alignment of the text block WRT its bounding box. */
   private MultiButton<TextAlign> vAlignWidget = null;
   
   /** Compact widget uses a pop-up panel to edit the text box's background fill. */
   private BkgFillPicker bkgFillPicker = null;

   /** Numeric text field for editing the text box's line height, or inter-line spacing. */
   private NumericTextField lineHtField = null;

   /** Self-contained editor handles the node's text-related styles. */
   private TextStyleEditor textStyleEditor = null;
   
   /** Self-contained editor handles the node's draw/stroke-related styles. */
   private DrawStyleEditor drawStyleEditor = null;
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
