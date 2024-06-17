package com.srscicomp.fc.ui;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import com.srscicomp.common.ui.MultiButton;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.common.ui.RGBColorPicker;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.FigureNode;
import com.srscicomp.fc.fig.LegendNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
 * <b>FGNLegendCard</b> implements the subordinate editor that appears in the "Legend" tab in the 2D graph, 2D polar
 * plot, and 3D graph property editors. All 2D and 3D graph containers have an automated legend component encapsulated 
 * by {@link LegendNode}, and this GUI element displays/edits all properties of the legend. Custom numeric text widgets 
 * display the (x,y) coordinates of the starting position for the first entry in the legend, the legend's orientation (a 
 * rotation about that starting position), the length of each trace entry, the size of the symbols rendered for each 
 * entry (if applicable), the vertical distance separating adjacent entries, and the horizontal offset from the right 
 * edge of an entry's trace line to the start of the entry's text label. Two mutually exclusive toggle buttons select 
 * whether a representative data point symbol is rendered at the midpoint of the trace entry or at each of its end 
 * points. A check box indicates whether or not the legend is actually drawn. To create a "boxed" legend, a numeric
 * widget specifies the box border with, and an RGB color choice widget sets the box color; the box outline is stroked
 * with the legend's text color. A {@link TextStyleEditor} displays the text-related styles in which each entry's label 
 * is rendered. 
 * 
 * <p><i>NOTE</i>. This class intended only for use in the 2D and 3D graph editors. These are composite editors that
 * handle all the component nodes of a 2D or 3D graph. When the graph object is loaded for editing, these editors must
 * call {@link #loadLegend(LegendNode)} to specify the legend node that should be displayed and edited here.</p>.
 * 
 * @author sruffner
 */
final class FGNLegendCard extends FGNEditor implements ActionListener, ItemListener, PropertyChangeListener
{
   /** Construct the graph legend properties editor. */
   FGNLegendCard()
   {
      // we layout everything in a transparent panel that, in turn, is placed in the "north" of the editor panel --
      // to ensure things don't get spread out vertically
      JPanel p = new JPanel(new SpringLayout());
      p.setOpaque(false);
      SpringLayout layout = (SpringLayout) p.getLayout();

      JLabel layoutImg = new JLabel(FCIcons.LEGEND_LAYOUT);
      layoutImg.setAlignmentY(JPanel.CENTER_ALIGNMENT);
      layoutImg.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0,0,0,4),
            BorderFactory.createLoweredBevelBorder()));
      p.add(layoutImg);
      
      Measure.Constraints c = FGraphicModel.getLocationConstraints(FGNodeType.LEGEND);
      JLabel xLabel = new JLabel("X= ");
      p.add(xLabel);
      xEditor = new MeasureEditor(7, c);
      xEditor.setToolTipText("X-coord of legend's bottom-left corner");
      xEditor.addActionListener(this);
      p.add(xEditor);
      
      JLabel yLabel = new JLabel("Y= ");
      p.add(yLabel);
      yEditor = new MeasureEditor(7, c);
      yEditor.setToolTipText("Y-coord of legend's bottom-left corner");
      yEditor.addActionListener(this);
      p.add(yEditor);
      
      JLabel rotLabel = new JLabel("\u03b8= ");
      p.add(rotLabel);
      rotateField = new NumericTextField(-180.0, 180.0, 2);
      rotateField.setToolTipText("Orientation (rotation about bottom-left corner)");
      rotateField.addActionListener(this);
      p.add(rotateField);
      
      hideChk = new JCheckBox("Hide?");
      hideChk.setContentAreaFilled(false);
      hideChk.addActionListener(this);
      p.add(hideChk);
      
      JLabel spacerLabel = new JLabel("D= ");
      p.add(spacerLabel);
      spacerEditor = new MeasureEditor(0, LegendNode.LENCONSTRAINTS);
      spacerEditor.setToolTipText("Enter distance between legend entries (0-5in)");
      spacerEditor.addActionListener(this);
      p.add(spacerEditor);
      
      JLabel offsetLabel = new JLabel("G= ");
      p.add(offsetLabel);
      labelOffsetEditor = new MeasureEditor(0, LegendNode.LENCONSTRAINTS);
      labelOffsetEditor.setToolTipText("Enter offset between entry label and trace line (0-5in)");
      labelOffsetEditor.addActionListener(this);
      p.add(labelOffsetEditor);
      
      JLabel symSzLabel = new JLabel("S= ");
      p.add(symSzLabel);
      symbolSizeEditor = new MeasureEditor(0, LegendNode.LENCONSTRAINTS);
      symbolSizeEditor.setToolTipText("Enter uniform marker symbol size (0=Use actual size)");
      symbolSizeEditor.addActionListener(this);
      p.add(symbolSizeEditor);
      
      JLabel lenLabel = new JLabel("L= ");
      p.add(lenLabel);
      lengthEditor = new MeasureEditor(0, LegendNode.LENCONSTRAINTS);
      lengthEditor.setToolTipText("Enter trace line length for each legend entry (0-5in)");
      lengthEditor.addActionListener(this);
      p.add(lengthEditor);
      
      midPtMB = new MultiButton<>();
      midPtMB.addChoice(Boolean.TRUE, FCIcons.V4_MIDPT_16, "midpoint");
      midPtMB.addChoice(Boolean.FALSE, FCIcons.V4_ENDPT_16, "endpoints");
      midPtMB.setToolTipText("Location of marker symbol in legend entry");
      midPtMB.addItemListener(this);
      p.add(midPtMB);
      
      JLabel boxLabel = new JLabel("Box: fill ");
      p.add(boxLabel);
      boxColorPicker = new RGBColorPicker(true);
      boxColorPicker.setToolTipText("Background color for legend's bounding box");
      boxColorPicker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
      p.add(boxColorPicker);
      JLabel borderWLabel = new JLabel("border ");
      p.add(borderWLabel);
      borderWEditor = new MeasureEditor(0, FigureNode.STROKEWCONSTRAINTS);
      borderWEditor.addActionListener(this);
      borderWEditor.setToolTipText("Border width for legend's bounding box (0 for no border)");
      p.add(borderWEditor);

      textStyleEditor = new TextStyleEditor();
      p.add(textStyleEditor);
      
      
      // left-right constraints. Assume row with text style editor will be longest.
      int GAP = 5;
      layout.putConstraint(SpringLayout.WEST, hideChk, 0, SpringLayout.WEST, p);

      layout.putConstraint(SpringLayout.WEST, xLabel, GAP, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.WEST, xEditor, 0, SpringLayout.EAST, xLabel);
      layout.putConstraint(SpringLayout.WEST, yLabel, GAP, SpringLayout.EAST, xEditor);
      layout.putConstraint(SpringLayout.WEST, yEditor, 0, SpringLayout.EAST, yLabel);
      layout.putConstraint(SpringLayout.WEST, rotLabel, GAP, SpringLayout.EAST, yEditor);
      layout.putConstraint(SpringLayout.WEST, rotateField, 0, SpringLayout.EAST, rotLabel);

      layout.putConstraint(SpringLayout.WEST, layoutImg, GAP, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.WEST, spacerLabel, GAP, SpringLayout.EAST, layoutImg);
      layout.putConstraint(SpringLayout.WEST, spacerEditor, 0, SpringLayout.EAST, spacerLabel);
      layout.putConstraint(SpringLayout.WEST, symSzLabel, GAP*2, SpringLayout.EAST, spacerEditor);
      layout.putConstraint(SpringLayout.WEST, symbolSizeEditor, 0, SpringLayout.EAST, symSzLabel);
      layout.putConstraint(SpringLayout.WEST, midPtMB, GAP, SpringLayout.EAST, symbolSizeEditor);

      layout.putConstraint(SpringLayout.EAST, lenLabel, 0, SpringLayout.EAST, spacerLabel);
      layout.putConstraint(SpringLayout.WEST, lengthEditor, 0, SpringLayout.WEST, spacerEditor);
      layout.putConstraint(SpringLayout.EAST, offsetLabel, 0, SpringLayout.EAST, symSzLabel);
      layout.putConstraint(SpringLayout.WEST, labelOffsetEditor, 0, SpringLayout.WEST, symbolSizeEditor);
      
      layout.putConstraint(SpringLayout.WEST, boxLabel, GAP, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.WEST, boxColorPicker, 0, SpringLayout.EAST, boxLabel);
      layout.putConstraint(SpringLayout.WEST, borderWLabel, GAP*3, SpringLayout.EAST, boxColorPicker);
      layout.putConstraint(SpringLayout.WEST, borderWEditor, 0, SpringLayout.EAST, borderWLabel);

      layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.EAST, p, 0, SpringLayout.EAST, textStyleEditor);

      // top-bottom constraints: Row with hide flag, then a row with the location widgets. Then 2 rows of legend layout 
      // widgets vertically spanned by the layout picture and the midPt button. Then a row with fill color and border
      // width of legend box. Then the text style editor on the bottom. A widget in each row sets constraints with row 
      // above or below. Remaining widgets are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, hideChk, GAP, SpringLayout.NORTH, p);
      layout.putConstraint(SpringLayout.NORTH, rotateField, GAP, SpringLayout.SOUTH, hideChk);
      layout.putConstraint(SpringLayout.NORTH, spacerEditor, GAP*4, SpringLayout.SOUTH, rotateField);
      layout.putConstraint(SpringLayout.NORTH, lengthEditor, GAP, SpringLayout.SOUTH, spacerEditor);
      layout.putConstraint(SpringLayout.NORTH, boxColorPicker, GAP*4, SpringLayout.SOUTH, lengthEditor);
      layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*3, SpringLayout.SOUTH, boxColorPicker);
      layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, textStyleEditor);
      
      String vc = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vc, xLabel, 0, vc, rotateField);
      layout.putConstraint(vc, xEditor, 0, vc, rotateField);
      layout.putConstraint(vc, yLabel, 0, vc, rotateField);
      layout.putConstraint(vc, yEditor, 0, vc, rotateField);
      layout.putConstraint(vc, rotLabel, 0, vc, rotateField);

      layout.putConstraint(vc, spacerLabel, 0, vc, spacerEditor);
      layout.putConstraint(vc, symSzLabel, 0, vc, spacerEditor);
      layout.putConstraint(vc, symbolSizeEditor, 0, vc, spacerEditor);

      layout.putConstraint(vc, lenLabel, 0,vc, lengthEditor);
      layout.putConstraint(vc, offsetLabel, 0, vc, lengthEditor);
      layout.putConstraint(vc, labelOffsetEditor, 0,vc, lengthEditor);
      
      // layout picture and midPt button are vertically centered WRT the 2 rows of legend layout widgets
      layout.putConstraint(vc, layoutImg, -GAP/2, SpringLayout.NORTH, lengthEditor);
      layout.putConstraint(vc, midPtMB, -GAP/2, SpringLayout.NORTH, lengthEditor);
      
      layout.putConstraint(vc, boxLabel, 0, vc, boxColorPicker);
      layout.putConstraint(vc, borderWLabel, 0, vc, boxColorPicker);
      layout.putConstraint(vc, borderWEditor, 0, vc, boxColorPicker);

      setLayout(new BorderLayout());
      add(p, BorderLayout.NORTH);
   }
   
   /** The legend node currently loaded into this editor. */
   private LegendNode legend = null;
   
   /**
    * Load the legend element to be displayed and edited.
    * @param legend The legend element
    */
   void loadLegend(LegendNode legend) { this.legend = legend; reload(true); }
   
   @Override void reload(boolean initial)
   {
      if(legend == null) return;
      
      xEditor.setMeasure(legend.getX());
      yEditor.setMeasure(legend.getY());
      rotateField.setValue(legend.getRotate());
      hideChk.setSelected(legend.getHide());
      
      spacerEditor.setMeasure(legend.getSpacer());
      symbolSizeEditor.setMeasure(legend.getSymbolSize());
      lengthEditor.setMeasure(legend.getLength());
      labelOffsetEditor.setMeasure(legend.getLabelOffset());
      
      midPtMB.setCurrentChoice(legend.getMid());

      boxColorPicker.setCurrentColor(legend.getBoxColor(), false);
      borderWEditor.setMeasure(legend.getBorderWidth());
      
      textStyleEditor.loadGraphicNode(legend);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && n.getNodeType() == FGNodeType.LEGEND); }
   @Override ImageIcon getRepresentativeIcon() { return(null); }
   @Override String getRepresentativeTitle() { return(null); }

   @Override void onLowered() 
   { 
      textStyleEditor.cancelEditing(); 
      boxColorPicker.cancelPopup();
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      if(legend == null) return;
      Object src = e.getSource();
      
      boolean ok = true;
      if(src == hideChk)
         legend.setHide(hideChk.isSelected());
      else if(src == xEditor)
      {
         ok = legend.setX(xEditor.getMeasure());
         if(!ok) xEditor.setMeasure(legend.getX());
      }
      else if(src == yEditor)
      {
         ok = legend.setY(yEditor.getMeasure());
         if(!ok) yEditor.setMeasure(legend.getY());
      }
      else if(src == rotateField)
      {
         ok = legend.setRotate(rotateField.getValue().doubleValue());
         if(!ok) rotateField.setValue(legend.getRotate());
      }
      else if(src == spacerEditor)
      {
         ok = legend.setSpacer(spacerEditor.getMeasure());
         if(!ok) spacerEditor.setMeasure(legend.getSpacer());
      }
      else if(src == symbolSizeEditor)
      {
         ok = legend.setSymbolSize(symbolSizeEditor.getMeasure());
         if(!ok) symbolSizeEditor.setMeasure(legend.getSymbolSize());
      }
      else if(src == lengthEditor)
      {
         ok = legend.setLength(lengthEditor.getMeasure());
         if(!ok) lengthEditor.setMeasure(legend.getLength());
      }
      else if(src == labelOffsetEditor)
      {
         ok = legend.setLabelOffset(labelOffsetEditor.getMeasure());
         if(!ok) labelOffsetEditor.setMeasure(legend.getLabelOffset());
      }
      else if(src == borderWEditor)
      {
         ok = legend.setBorderWidth(borderWEditor.getMeasure());
         if(!ok) borderWEditor.setMeasure(legend.getBorderWidth());
      }

      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   @Override public void itemStateChanged(ItemEvent e)
   {
      if(legend == null || e.getSource() != midPtMB) return;
      legend.setMid(midPtMB.getCurrentChoice());
   }
   
   @Override public void propertyChange(PropertyChangeEvent e)
   {
      if((legend != null) && (e.getSource() == boxColorPicker) && 
            (e.getPropertyName().equals(RGBColorPicker.COLOR_PROPERTY)))
         legend.setBoxColor(boxColorPicker.getCurrentColor());
   }

   /** Check box sets legend's boolean-valued <i>hide</i> property. */
   private JCheckBox hideChk = null;
   
   /** Customized component for editing the x-coordinate of the legend's location. */
   private final MeasureEditor xEditor;

   /** Customized component for editing the y-coordinate of the legend's location. */
   private MeasureEditor yEditor = null;

   /** Numeric text field for editing the legend's orientation angle. */
   private NumericTextField rotateField = null;

   /** Customized component for editing the legend's line segment length. */
   private MeasureEditor lengthEditor = null;

   /** Customized component for editing the legend's spacer size. */
   private MeasureEditor spacerEditor = null;

   /** Customized component for editing the legend's entry label offset. */
   private MeasureEditor labelOffsetEditor = null;

   /** Customized component for editing the legend's uniform symbol size. */
   private MeasureEditor symbolSizeEditor = null;

   /** Edits the legend's <i>mid</i> flag (symbol at midpoint or end points). */
   private MultiButton<Boolean> midPtMB = null;
   
   /** Color picker displays/edits the background color for the legend's bounding box. */
   private RGBColorPicker boxColorPicker = null;
   
   /** Customized component for editing the stroke width for the legend's bounding box border. */
   private MeasureEditor borderWEditor = null;

   /** Self-contained editor handles the text style properties for the legend. */
   private TextStyleEditor textStyleEditor = null;
}
