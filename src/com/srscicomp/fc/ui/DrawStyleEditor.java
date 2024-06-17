package com.srscicomp.fc.ui;


import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


import javax.swing.JPanel;
import javax.swing.SpringLayout;

import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dutil.StrokeJoin;
import com.srscicomp.common.ui.MultiButton;
import com.srscicomp.common.ui.RGBColorPicker;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;
import com.srscicomp.fc.uibase.StrokePatternCombo;

/**
 * <b>DrawStyleEditor</b> displays/edits all draw styles for a <i>Figure Composer</i> graphic node -- ie, an
 * instance of {@link FGraphicNode} or one of its subclasses.
 * 
 * <p>The following widgets are present in the editor panel, arranged in a single row from left to right:
 * <ul>
 * <li>An {@link RGBColorPicker}, for specifying the text/fill color. The button face is painted with the current color;
 * clicking the button brings up a compact dialog by which the user can change the current color.</li>
 * <li>A second {@link RGBColorPicker} for specifying the stroke color.</li>
 * <li>A {@link MeasureEditor} by which the user sets the stroke width in inches, centimeters, millimeters, or 
 * typographical points. The editor includes a numeric text field for entering the measured value, plus a static label 
 * that reflects the measurement units. Entries are restricted IAW the measurement constraints in {@link 
 * FGraphicNode#STROKEWCONSTRAINTS}. Hot keys effect a units change when the keyboard focus is on the widget: 'i' for 
 * inches, 'c' for cm, 'm' for mm, and 'p' for pt.</li>
 * <li>A specialized combo box, {@link StrokePatternCombo}, by which the user defines the stroke dash-gap pattern for 
 * the edited node. The dash-gap pattern can be entered as as an array of up to 6 whitespace-separated digits, each of 
 * which is restricted to [1..99]. The drop-down list includes the five commonly used stroking patterns, plus up to five
 * recent entries.</li>
 * <li>Multiple-choice widgets, {@link MultiButton}, for selecting the stroke end cap and join styles.</li>
 * </ul>
 * </p>
 * 
 * <p><b>DrawStyleEditor</b> is intended to be paired with {@link TextStyleEditor}, which exposes the text-related
 * graphic styles. The two sets of graphic styles are handled by distinct editor objects because certain graphic node 
 * types only have text styles, while others only have the draw styles. Note that the "restore defaults" button is 
 * present only on the text style editor even though its pop-up menu can affect any graphic style property. Also, the 
 * text/fill color property is technically both a text and draw style. <b>DrawStyleEditor</b> can be configured to omit 
 * the fill color widget -- for use when paired with a <b>TextStyleEditor</b>, or for editing a node type that lacks the
 * fill color. It can also be configured to omit the stroke pattern combo box, since a number of graphic mode types lack
 * that style property. Finally, note that the draw style editor lacks a restore button, because its functionality was
 * deemed unnecessary for node types that only possess the draw styles.</p>
 * 
 * @author sruffner
 */
final class DrawStyleEditor extends JPanel implements ActionListener, PropertyChangeListener, ItemListener
{
   /** 
    * Construct the draw style properties editor. 
    * @param omitFillColor If true, the draw fill color widget is omitted.
    * @param omitStrokePattern If true, the stroke pattern combo box is omitted.
    */
   DrawStyleEditor(boolean omitFillColor, boolean omitStrokePattern)
   {
      super();
      setOpaque(false);

      if(!omitFillColor)
      {
         fillColorPicker = new RGBColorPicker();
         fillColorPicker.setToolTipText("Fill Color");
         fillColorPicker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
         add(fillColorPicker);
      }
      
      strokeColorPicker = new RGBColorPicker();
      strokeColorPicker.setToolTipText("Stroke Color");
      strokeColorPicker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
      add(strokeColorPicker);

      strokeWidthEditor = new MeasureEditor(0, FGraphicNode.STROKEWCONSTRAINTS);
      strokeWidthEditor.setToolTipText("Stroke Width");
      strokeWidthEditor.addActionListener(this);
      add(strokeWidthEditor);
      
      strokeCapMB = new MultiButton<>();
      strokeCapMB.addChoice(StrokeCap.BUTT, FCIcons.V4_CAPBUTT_16, "butt");
      strokeCapMB.addChoice(StrokeCap.SQUARE, FCIcons.V4_CAPSQUARE_16, "square");
      strokeCapMB.addChoice(StrokeCap.ROUND, FCIcons.V4_CAPROUND_16, "round");
      strokeCapMB.setToolTipText("Stroke end cap");
      strokeCapMB.addItemListener(this);
      add(strokeCapMB);

      strokeJoinMB = new MultiButton<>();
      strokeJoinMB.addChoice(StrokeJoin.MITER, FCIcons.V4_JOINMITER_16, "miter");
      strokeJoinMB.addChoice(StrokeJoin.BEVEL, FCIcons.V4_JOINBEVEL_16, "bevel");
      strokeJoinMB.addChoice(StrokeJoin.ROUND, FCIcons.V4_JOINROUND_16, "round");
      strokeJoinMB.setToolTipText("Stroke join");
      strokeJoinMB.addItemListener(this);
      add(strokeJoinMB);

      if(!omitStrokePattern)
      {
         strokePatCombo = new StrokePatternCombo();
         strokePatCombo.addActionListener(this);
         add(strokePatCombo);
      }
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      int GAP = TextStyleEditor.GAP;
      if(!omitFillColor)
      {
         layout.putConstraint(SpringLayout.WEST, fillColorPicker, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, strokeColorPicker, GAP, SpringLayout.EAST, fillColorPicker);
      }
      else
         layout.putConstraint(SpringLayout.WEST, strokeColorPicker, 0, SpringLayout.WEST, this);

      layout.putConstraint(SpringLayout.WEST, strokeWidthEditor, GAP, SpringLayout.EAST, strokeColorPicker);
      if(!omitStrokePattern)
      {
         layout.putConstraint(SpringLayout.WEST, strokePatCombo, GAP, SpringLayout.EAST, strokeWidthEditor);
         layout.putConstraint(SpringLayout.WEST, strokeCapMB, GAP, SpringLayout.EAST, strokePatCombo);
      }
      else
         layout.putConstraint(SpringLayout.WEST, strokeCapMB, GAP, SpringLayout.EAST, strokeWidthEditor);
      layout.putConstraint(SpringLayout.WEST, strokeJoinMB, GAP, SpringLayout.EAST, strokeCapMB);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, strokeJoinMB);

      layout.putConstraint(SpringLayout.NORTH, strokeCapMB, 0, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, strokeCapMB);
      if(!omitFillColor)
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, fillColorPicker, 0, SpringLayout.VERTICAL_CENTER, strokeCapMB);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, strokeColorPicker, 0, SpringLayout.VERTICAL_CENTER, strokeCapMB);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, strokeWidthEditor, 0, SpringLayout.VERTICAL_CENTER, strokeCapMB);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, strokeJoinMB, 0, SpringLayout.VERTICAL_CENTER, strokeCapMB);      
      if(!omitStrokePattern)
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, strokePatCombo, 0, SpringLayout.VERTICAL_CENTER, strokeCapMB);
   }

   /**
    * Show or hide the RGB color picker widget that edits the graphic node's <i>fill color</i> style. <i>No action is
    * taken if this editor is configured without that widget.</i>
    * @param show True to show and false to hide the fill color widget.
    */
   public void showFillColorPicker(boolean show)
   {
      if(fillColorPicker != null) fillColorPicker.setVisible(show);
   }
   
   /** The figure graphic node currently being edited. If null, then the editor is hidden. */
   private FGraphicNode editedNode = null;
   
   /** 
    * Get the figure graphic node currently loaded into this draw style editor.
    * @return The currently edited node.
    */
   public FGraphicNode getEditedNode() { return(editedNode); }
   
   /**
    * Reload this editor to display and modify the draw style properties of the specified graphic node. 
    * @param n The graphic node being edited. If null, the editor will be hidden.
    * @return True if node was loaded into editor; false if <i>n == null</i>.
    */
   public boolean loadGraphicNode(FGraphicNode n)
   {
      editedNode = n;

      setVisible(editedNode != null);
      if(editedNode == null) return(false);

      if(fillColorPicker != null && editedNode.hasFillColorProperty())
         fillColorPicker.setCurrentColor(editedNode.getFillColor(), false);
      
      if(editedNode.hasStrokeProperties())
      {
         strokeColorPicker.setCurrentColor(editedNode.getStrokeColor(), false);
         strokeWidthEditor.setMeasure(editedNode.getMeasuredStrokeWidth());
         strokeCapMB.setCurrentChoice(editedNode.getStrokeEndcap());
         strokeJoinMB.setCurrentChoice(editedNode.getStrokeJoin());
         if(strokePatCombo != null && editedNode.hasStrokePatternProperty()) 
            strokePatCombo.setSelectedPattern(editedNode.getStrokePattern());
      }

      return(true);
   }

   /**
    * Call this method prior to hiding the draw style editor. It makes sure that the transient pop-up windows associated
    * with the color picker and multi-choice button widgets are hidden.
    */
   public void cancelEditing()
   { 
      if(fillColorPicker != null) fillColorPicker.cancelPopup();
      strokeColorPicker.cancelPopup();
      strokeCapMB.cancelPopup();
      strokeJoinMB.cancelPopup();
   }

   
   //
   // ActionListener, PropertyChangeListener
   //
 
   public void actionPerformed(ActionEvent e)
   {
      if(editedNode == null) return;
      Object src = e.getSource();

      if(src == strokeWidthEditor)
      {
         if(!editedNode.setMeasuredStrokeWidth(strokeWidthEditor.getMeasure()))
         {
            Toolkit.getDefaultToolkit().beep();
            strokeWidthEditor.setMeasure(editedNode.getMeasuredStrokeWidth());
         }
      }
      else if(src != null && src == strokePatCombo)
         editedNode.setStrokePattern(strokePatCombo.getSelectedPattern());
   }

   @Override public void propertyChange(PropertyChangeEvent e)
   {
      String prop = e.getPropertyName();
      if(!prop.equals(RGBColorPicker.COLOR_PROPERTY)) return;
      
      if(editedNode == null) return;
      Object src = e.getSource();
      if(src != null && src == fillColorPicker)
         editedNode.setFillColor(fillColorPicker.getCurrentColor());
      else if(src == strokeColorPicker)
         editedNode.setStrokeColor(strokeColorPicker.getCurrentColor());
   }

   @Override public void itemStateChanged(ItemEvent e)
   {
      if(editedNode == null) return;
      Object src = e.getSource();
      if(src == strokeCapMB)
         editedNode.setStrokeEndcap(strokeCapMB.getCurrentChoice());
      else if(src == strokeJoinMB)
         editedNode.setStrokeJoin(strokeJoinMB.getCurrentChoice());
   }


   /** Compact widget uses a pop-up panel to edit the draw fill color (may be omitted at construction). */
   private RGBColorPicker fillColorPicker = null;

   /** Compact widget uses a popup panel to edit the stroke color. */
   private RGBColorPicker strokeColorPicker = null;

   /** Customized component for editing stroke width. */
   private MeasureEditor strokeWidthEditor = null;

   /** Selects the stroke end cap style. */
   private MultiButton<StrokeCap> strokeCapMB = null;
   
   /** Selects the stroke join style. */
   private MultiButton<StrokeJoin> strokeJoinMB = null;

   /** Customized component for editing the stroke dash-gap pattern. */
   private StrokePatternCombo strokePatCombo = null;
}
