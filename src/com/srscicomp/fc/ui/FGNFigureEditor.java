package com.srscicomp.fc.ui;

import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;

import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.ui.BkgFillPicker;
import com.srscicomp.common.ui.MultiButton;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.FigureNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;
import com.srscicomp.fc.uibase.StyledTextEditor;

/**
 * <b>FGNFigureEditor</b> displays/edits all properties of the root node of a FypML figure, an instance of {@link 
 * FigureNode}. It includes a {@link TextStyleEditor} and {@link DrawStyleEditor} to expose all of the node's style 
 * properties, a {@link StyledTextEditor} to set the text for the figure's automated title, a checkbox for enabling or
 * disabling the automated title, plus widgets to specify the (x,y) coordinates of the figure's bottom-left corner, its 
 * width and height, and the width of its border. Another set of widgets let the user re-scale the entire figure or 
 * only the figure's fonts.
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNFigureEditor extends FGNEditor implements ActionListener, FocusListener, PropertyChangeListener, ItemListener
{

   /** Construct the figure node properties editor. */
   FGNFigureEditor()
   {
      super();
      
      showTitleChk = new JCheckBox("Title");
      showTitleChk.setToolTipText("Check this box to show figure title");
      showTitleChk.setContentAreaFilled(false);
      showTitleChk.addActionListener(this);
      add(showTitleChk);
            
      titleEditor = new StyledTextEditor(2);
      titleEditor.addActionListener(this);
      add(titleEditor);
      
      hAlignWidget = new MultiButton<>();
      hAlignWidget.addChoice(TextAlign.LEADING, FCIcons.V4_ALIGNLEFT_16, "Left");
      hAlignWidget.addChoice(TextAlign.CENTERED, FCIcons.V4_ALIGNCENTER_16, "Center");
      hAlignWidget.addChoice(TextAlign.TRAILING, FCIcons.V4_ALIGNRIGHT_16, "Right");
      hAlignWidget.addItemListener(this);
      hAlignWidget.setToolTipText("Horizontal alignment of figure title");
      add(hAlignWidget);
      
      vAlignWidget = new MultiButton<>();
      vAlignWidget.addChoice(TextAlign.LEADING, FCIcons.V4_VALIGNTOP_16, "Top");
      vAlignWidget.addChoice(TextAlign.CENTERED, FCIcons.V4_VALIGNMID_16, "Middle");
      vAlignWidget.addChoice(TextAlign.TRAILING, FCIcons.V4_VALIGNBOT_16, "Bottom");
      vAlignWidget.addItemListener(this);
      vAlignWidget.setToolTipText("Vertical alignment of figure title");
      add(vAlignWidget);
      

      JLabel noteLabel = new JLabel("Notes ");
      add(noteLabel);
      noteArea = new JTextArea();
      noteArea.setTabSize(6);
      noteArea.setLineWrap(true);
      noteArea.setWrapStyleWord(true);
      noteArea.addFocusListener(this);
      noteArea.setToolTipText("<html>Enter optional figure note or description.<br/>" +
            "For author use only -- the note is never rendered in any way.<br/>" +
            "<i>Hit refresh button (or <b>Shift+Enter</b>) after making any changes.</i></html>");
      noteArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "refresh");
      noteArea.getActionMap().put("refresh", 
            new AbstractAction() {
               @Override public void actionPerformed(ActionEvent e) { refreshNoteBtn.doClick(); }
      });

      JScrollPane noteScroller = new JScrollPane(noteArea);
      noteScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      add(noteScroller);
      
      refreshNoteBtn = new JButton(FCIcons.V4_REFRESH_22);
      refreshNoteBtn.setMargin(new Insets(2,4,2,4));
      refreshNoteBtn.setToolTipText("Press this button to submit changes in the figure note/description");
      refreshNoteBtn.addActionListener(this);
      add(refreshNoteBtn);
      
      JLabel xLabel = new JLabel("X= ");
      add(xLabel);
      Measure.Constraints c = FGraphicModel.getLocationConstraints(FGNodeType.FIGURE);
      xEditor = new MeasureEditor(8, c);
      xEditor.setToolTipText("Enter X-coordinate of figure's bottom-left corner");
      xEditor.addActionListener(this);
      add(xEditor);
      
      JLabel yLabel = new JLabel("Y= ");
      add(yLabel);
      yEditor = new MeasureEditor(8, c);
      yEditor.setToolTipText("Enter Y-coordinate of figure's bottom-left corner");
      yEditor.addActionListener(this);
      add(yEditor);
      
      JLabel wLabel = new JLabel("W= ");
      add(wLabel);
      c = FGraphicModel.getSizeConstraints(FGNodeType.FIGURE);
      wEditor = new MeasureEditor(8, c);
      wEditor.setToolTipText("Enter figure width");
      wEditor.addActionListener(this);
      add(wEditor);
      
      JLabel hLabel = new JLabel("H= ");
      add(hLabel);
      hEditor = new MeasureEditor(8, c);
      hEditor.setToolTipText("Enter figure height");
      hEditor.addActionListener(this);
      add(hEditor);
      
      JLabel borderLabel = new JLabel("BW= ");
      add(borderLabel);
      borderWEditor = new MeasureEditor(8, FigureNode.STROKEWCONSTRAINTS);
      borderWEditor.addActionListener(this);
      borderWEditor.setToolTipText("Enter figure border width (0 to hide border)");
      add(borderWEditor);
      
      bkgFillPicker = new BkgFillPicker(50, 50);
      bkgFillPicker.setToolTipText("Background fill");
      bkgFillPicker.addPropertyChangeListener(BkgFillPicker.BKGFILL_PROPERTY, this);
      add(bkgFillPicker);
      

      JLabel scaleLabel = new JLabel("Scale (%): ");
      add(scaleLabel);
      scalePctField = new NumericTextField(FGraphicNode.MINRESCALE, FGraphicNode.MAXRESCALE);
      scalePctField.setValue(100);
      scalePctField.addActionListener(this);
      add(scalePctField);
      
      rescaleFigBtn = new JButton("All");
      rescaleFigBtn.setOpaque(false);  // Needed this b/c default bkg showed around edges in Windows.
      rescaleFigBtn.addActionListener(this);
      rescaleFigBtn.setEnabled(false);
      add(rescaleFigBtn);
      
      rescaleFontsBtn = new JButton("Fonts");
      rescaleFontsBtn.setOpaque(false);  // Needed this b/c default bkg showed around edges in Windows.
      rescaleFontsBtn.addActionListener(this);
      rescaleFontsBtn.setEnabled(false);
      add(rescaleFontsBtn);
      
      textStyleEditor = new TextStyleEditor();
      add(textStyleEditor);

      drawStyleEditor = new DrawStyleEditor(true, false);
      add(drawStyleEditor);
      
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. Assume row with text style editor will be longest.
      layout.putConstraint(SpringLayout.WEST, showTitleChk, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, titleEditor, GAP, SpringLayout.EAST, showTitleChk);
      layout.putConstraint(SpringLayout.EAST, titleEditor, 0, SpringLayout.EAST, textStyleEditor);
      layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, hAlignWidget, 0, SpringLayout.HORIZONTAL_CENTER, showTitleChk);
      layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, vAlignWidget, 0, SpringLayout.HORIZONTAL_CENTER, showTitleChk);
      
      layout.putConstraint(SpringLayout.WEST, noteLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, noteScroller, 0, SpringLayout.EAST, noteLabel);
      layout.putConstraint(SpringLayout.EAST, refreshNoteBtn, 0, SpringLayout.EAST, textStyleEditor);
      layout.putConstraint(SpringLayout.EAST, noteScroller, -GAP, SpringLayout.WEST, refreshNoteBtn);
      
      layout.putConstraint(SpringLayout.EAST, xLabel, 0, SpringLayout.EAST, noteLabel);
      layout.putConstraint(SpringLayout.WEST, xEditor, 0, SpringLayout.EAST, xLabel);
      layout.putConstraint(SpringLayout.WEST, yLabel, GAP*2, SpringLayout.EAST, xEditor);
      layout.putConstraint(SpringLayout.WEST, yEditor, 0, SpringLayout.EAST, yLabel);

      layout.putConstraint(SpringLayout.EAST, wLabel, 0, SpringLayout.EAST, xLabel);
      layout.putConstraint(SpringLayout.WEST, wEditor, 0, SpringLayout.WEST, xEditor);
      layout.putConstraint(SpringLayout.EAST, hLabel, 0, SpringLayout.EAST, yLabel);
      layout.putConstraint(SpringLayout.WEST, hEditor, 0, SpringLayout.WEST, yEditor);
      layout.putConstraint(SpringLayout.WEST, bkgFillPicker, GAP*6, SpringLayout.EAST, hEditor);

      layout.putConstraint(SpringLayout.EAST, borderLabel, 0, SpringLayout.EAST, xLabel);
      layout.putConstraint(SpringLayout.WEST, borderWEditor, 0, SpringLayout.WEST, xEditor);

      layout.putConstraint(SpringLayout.WEST, scaleLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, scalePctField, 0, SpringLayout.EAST, scaleLabel);
      layout.putConstraint(SpringLayout.WEST, rescaleFontsBtn, GAP, SpringLayout.EAST, scalePctField);
      layout.putConstraint(SpringLayout.WEST, rescaleFigBtn, GAP, SpringLayout.EAST, rescaleFontsBtn);
      
      layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, textStyleEditor);
      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);
      
      // top-bottom constraints: 8 rows, with text and draw style editors in last two. In each of first 6 rows, one
      // widget sets the constraints with row above or below. Remaining widgets are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, titleEditor, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, showTitleChk, 0, SpringLayout.NORTH, titleEditor);
      layout.putConstraint(SpringLayout.NORTH, hAlignWidget, GAP, SpringLayout.SOUTH, showTitleChk);
      layout.putConstraint(SpringLayout.NORTH, vAlignWidget, GAP, SpringLayout.SOUTH, hAlignWidget);
      
      layout.putConstraint(SpringLayout.NORTH, noteScroller, GAP*2, SpringLayout.SOUTH, titleEditor);
      layout.putConstraint(SpringLayout.SOUTH, noteScroller, GAP*2+60, SpringLayout.SOUTH, titleEditor);
      layout.putConstraint(SpringLayout.NORTH, xEditor, GAP, SpringLayout.SOUTH, noteScroller);
      layout.putConstraint(SpringLayout.NORTH, wEditor, GAP, SpringLayout.SOUTH, xEditor);
      layout.putConstraint(SpringLayout.NORTH, borderWEditor, GAP, SpringLayout.SOUTH, wEditor);
      layout.putConstraint(SpringLayout.NORTH, scalePctField, GAP, SpringLayout.SOUTH, borderWEditor);
      layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, scalePctField);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, drawStyleEditor);
      
      String vCtr = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vCtr, noteLabel, 0, vCtr, noteScroller);
      layout.putConstraint(vCtr, refreshNoteBtn, 0, vCtr, noteScroller);
      layout.putConstraint(vCtr, xLabel, 0, vCtr, xEditor);
      layout.putConstraint(vCtr, yLabel, 0, vCtr, xEditor);
      layout.putConstraint(vCtr, yEditor, 0, vCtr, xEditor);
      layout.putConstraint(vCtr, wLabel, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, hLabel, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, hEditor, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, bkgFillPicker, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, borderLabel, 0, vCtr, borderWEditor);
      layout.putConstraint(vCtr, scaleLabel, 0, vCtr, scalePctField);
      layout.putConstraint(vCtr, rescaleFontsBtn, 1, vCtr, scalePctField);
      layout.putConstraint(vCtr, rescaleFigBtn, 1, vCtr, scalePctField);
   }

   @Override public void reload(boolean initial)
   {
      FigureNode fig = getFigureNode();
      
      titleEditor.loadContent(fig.getAttributedTitle(false), fig.getFontFamily(), 0, fig.getFillColor(), 
            fig.getFontStyle());
      showTitleChk.setSelected(!fig.isTitleHidden());
      hAlignWidget.setCurrentChoice(fig.getTitleHorizontalAlignment());
      vAlignWidget.setCurrentChoice(fig.getTitleVerticalAlignment());
      noteArea.setText(fig.getNote());
      xEditor.setMeasure(fig.getX());
      yEditor.setMeasure(fig.getY());
      wEditor.setMeasure(fig.getWidth());
      hEditor.setMeasure(fig.getHeight());
      borderWEditor.setMeasure(fig.getBorderWidth());
      bkgFillPicker.setCurrentFill(fig.getBackgroundFill(), false);
      
      textStyleEditor.loadGraphicNode(fig);
      drawStyleEditor.loadGraphicNode(fig);
   }

   /** 
    * Ensure the modeless pop-up windows associated with the Unicode text field (for figure's title), the text/fill
    * color picker button, stroke color picker, and the background fill widget are extinguished before the editor 
    * panel is hidden.
    */
   @Override void onLowered()
   {
      textStyleEditor.cancelEditing();
      drawStyleEditor.cancelEditing();
      bkgFillPicker.cancelPopup();
   }

   /**
    * If the focus is on the title editor or the note text area, the special character is inserted at the current caret 
    * position. In either case, the user will still have to hit the associated submit button or shift focus away from 
    * the widget to submit the changes. 
    */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(titleEditor.isFocusOwner()) titleEditor.insertString(s, false);
      else if(noteArea.isFocusOwner()) noteArea.replaceSelection(s);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.FIGURE == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_FIGURE_32); }
   @Override String getRepresentativeTitle() { return("Figure Properties"); }

   public void focusGained(FocusEvent e) {}
   public void focusLost(FocusEvent e)
   {
      if(e.isTemporary()) return;
      Object src = e.getSource();
      if(src == noteArea) refreshNoteBtn.doClick();
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      FigureNode fig = getFigureNode();
      if(fig == null) return;
      Object src = e.getSource();

      boolean ok = true;
      if(src == titleEditor)
      {
         String text = FGraphicNode.toStyledText(titleEditor.getCurrentContents(), 
               fig.getFontStyle(), fig.getFillColor());
         ok = fig.setTitle(text);
         if(!ok) titleEditor.loadContent(fig.getAttributedTitle(false), fig.getFontFamily(), 0, fig.getFillColor(), 
               fig.getFontStyle());
      }
      else if(src == showTitleChk)
         fig.setTitleHidden(!showTitleChk.isSelected());
      else if(src == refreshNoteBtn)
      {
         ok = fig.setNote(noteArea.getText());
         if(!ok) noteArea.setText(fig.getNote());
      }
      else if(src == xEditor)
      {
         ok = fig.setX(xEditor.getMeasure());
         if(!ok) xEditor.setMeasure(fig.getX());
      }
      else if(src == yEditor)
      {
         ok = fig.setY(yEditor.getMeasure());
         if(!ok) yEditor.setMeasure(fig.getY());
      }
      else if(src == wEditor)
      {
         ok = fig.setWidth(wEditor.getMeasure());
         if(!ok) wEditor.setMeasure(fig.getWidth());
      }
      else if(src == hEditor)
      {
         ok = fig.setHeight(hEditor.getMeasure());
         if(!ok) hEditor.setMeasure(fig.getHeight());
      }
      else if(src == borderWEditor)
      {
         ok = fig.setBorderWidth(borderWEditor.getMeasure());
         if(!ok) borderWEditor.setMeasure(fig.getBorderWidth());
      }
      else if(src == scalePctField)
      {
         boolean enaScale = (scalePctField.getValue().intValue() != 100);
         rescaleFigBtn.setEnabled(enaScale);
         rescaleFontsBtn.setEnabled(enaScale);
      }
      else if(src == rescaleFigBtn || src == rescaleFontsBtn)
      {
         int scale = scalePctField.getValue().intValue();
         if(scale == 100) return;
         fig.rescale(scale, (src == rescaleFontsBtn));
         scalePctField.setValue(100);
         rescaleFigBtn.setEnabled(false);
         rescaleFontsBtn.setEnabled(false);
      }

      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   @Override public void propertyChange(PropertyChangeEvent e)
   {
      FigureNode fig = getFigureNode();
      if(fig != null && e.getSource() == bkgFillPicker)
         fig.setBackgroundFill(bkgFillPicker.getCurrentFill());
   }

   @Override public void itemStateChanged(ItemEvent e)
   {
      FigureNode fig = getFigureNode();
      if(fig == null) return;
      Object src = e.getSource();

      if(src == hAlignWidget)
         fig.setTitleHorizontalAlignment(hAlignWidget.getCurrentChoice());
      else if(src == vAlignWidget)
         fig.setTitleVerticalAlignment(vAlignWidget.getCurrentChoice());
   }
   

   /** Convenience method casts the edited node to an instance of {@link FigureNode}. */
   private FigureNode getFigureNode() { return((FigureNode) getEditedNode()); }
   
   /** 
    * The figure's title is edited within this custom widget, which allows user to change font style or text color 
    * anywhere in the content, add superscripts or subscripts, and underline portions of the text.
    */
   private StyledTextEditor titleEditor = null;

   /** When checked, the figure's title is rendered (unless title string is empty). */
   private final JCheckBox showTitleChk;
   
   /** Multiple-choice button widget for editing the H alignment of figure's title WRT its bounding box. */
   private MultiButton<TextAlign> hAlignWidget = null;
   
   /** Multiple-choice button widget for editing the V alignment of figure's title WRT its bounding box. */
   private MultiButton<TextAlign> vAlignWidget = null;

   /** 
    * Optional figure note/description is edited within this text area to permit a multi-line description. Must press 
    * the "refresh" button to submit any changes entered here. 
    */
   private JTextArea noteArea = null;
   
   /** Click this button to submit a change in the figure note text entered in the accompanying text area. */
   private JButton refreshNoteBtn = null;
   
   /** Customized component for editing the x-coordinate of the figure's bottom-left corner. */
   private MeasureEditor xEditor = null;

   /** Customized component for editing the y-coordinate of the figure's bottom-left corner. */
   private MeasureEditor yEditor = null;

   /** Customized component for editing the figure's width. */
   private MeasureEditor wEditor = null;

   /** Customized component for editing the figure's height. */
   private MeasureEditor hEditor = null;

   /** Customized component for editing the figure's border width. */
   private MeasureEditor borderWEditor = null;

   /** Compact widget uses a pop-up panel to edit the background fill descriptor for the figure's bounding box. */
   private BkgFillPicker bkgFillPicker = null;
      
   /** Edits scale factor (as a percentage) for the re-scaling operations; 100 disables re-scaling. */
   private NumericTextField scalePctField = null;

   /** Pressing this button re-scales the entire figure. */
   private JButton rescaleFigBtn = null;

   /** Pressing this button re-scales all fonts in the figure. */
   private JButton rescaleFontsBtn = null;

   /** Self-contained editor handles the node's text style properties. */
   private TextStyleEditor textStyleEditor = null;
   
   /** Self-contained editor handles the node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
