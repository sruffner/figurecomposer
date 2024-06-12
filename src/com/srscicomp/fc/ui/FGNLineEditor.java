package com.srscicomp.fc.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SpringLayout;

import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.LineNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
 * <code>FGNLineEditor</code> displays/edits all properties of a {@link LineNode}. It includes
 * a {@link TextStyleEditor} and {@link DrawStyleEditor} to expose the node's style properties, plus widgets to specify 
 * the (x,y) coordinates of the first and second endpoints of the line segment.
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNLineEditor extends FGNEditor implements ActionListener
{

   /** Construct the text label node properties editor. */
   FGNLineEditor()
   {
      super();
      
      Measure.Constraints c = FGraphicModel.getLocationConstraints(FGNodeType.LINE);

      JLabel fromLabel = new JLabel("P1 ");
      add(fromLabel);
      xEditor = new MeasureEditor(10,c);
      xEditor.setToolTipText("X1");
      xEditor.addActionListener(this);
      add(xEditor);
      
      yEditor = new MeasureEditor(10,c);
      yEditor.setToolTipText("Y1");
      yEditor.addActionListener(this);
      add(yEditor);
      
      JLabel toLabel = new JLabel("P2 ");
      add(toLabel);
      x2Editor = new MeasureEditor(10,c);
      x2Editor.setToolTipText("X2");
      x2Editor.addActionListener(this);
      add(x2Editor);
      
      y2Editor = new MeasureEditor(10,c);
      y2Editor.setToolTipText("Y2");
      y2Editor.addActionListener(this);
      add(y2Editor);
      
      textStyleEditor = new TextStyleEditor();
      add(textStyleEditor);

      drawStyleEditor = new DrawStyleEditor(true, false);
      add(drawStyleEditor);
      
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. Assume textStyleEditor will be longest.
      layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, textStyleEditor);
      
      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);
      
      layout.putConstraint(SpringLayout.WEST, fromLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, xEditor, 0, SpringLayout.EAST, fromLabel);
      layout.putConstraint(SpringLayout.WEST, yEditor, GAP, SpringLayout.EAST, xEditor);
      layout.putConstraint(SpringLayout.WEST, toLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, x2Editor, 0, SpringLayout.EAST, toLabel);
      layout.putConstraint(SpringLayout.WEST, y2Editor, GAP, SpringLayout.EAST, x2Editor);
      
      // top-bottom constraints: 4 rows, with text and draw style editors in last two. One widget in row 1,2 is used
      // to set constraints with row above/below; remaining widgets are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, xEditor, 0, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, x2Editor, GAP, SpringLayout.SOUTH, xEditor);
      layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, x2Editor);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, drawStyleEditor);
      
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, fromLabel, 0, SpringLayout.VERTICAL_CENTER, xEditor);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, yEditor, 0, SpringLayout.VERTICAL_CENTER, xEditor);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, toLabel, 0, SpringLayout.VERTICAL_CENTER, x2Editor);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, y2Editor, 0, SpringLayout.VERTICAL_CENTER, x2Editor);
   }

   @Override public void reload(boolean initial)
   {
      LineNode line = getLineNode();
      
      xEditor.setMeasure(line.getX());
      yEditor.setMeasure(line.getY());
      x2Editor.setMeasure(line.getX2());
      y2Editor.setMeasure(line.getY2());

      textStyleEditor.loadGraphicNode(line);
      drawStyleEditor.loadGraphicNode(line);
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

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.LINE == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_LINE_32); }
   @Override String getRepresentativeTitle() { return("Line Segment Properties"); }

   @Override public void actionPerformed(ActionEvent e)
   {
      LineNode line = getLineNode();
      if(line == null) return;
      Object src = e.getSource();

      boolean ok = false;
      if(src == xEditor)
      {
         ok = line.setX(xEditor.getMeasure());
         if(!ok) xEditor.setMeasure(line.getX());
      }
      else if(src == yEditor)
      {
         ok = line.setY(yEditor.getMeasure());
         if(!ok) yEditor.setMeasure(line.getY());
      }
      else if(src == x2Editor)
      {
         ok = line.setX2(x2Editor.getMeasure());
         if(!ok) x2Editor.setMeasure(line.getX2());
      }
      else if(src == y2Editor)
      {
         ok = line.setY2(y2Editor.getMeasure());
         if(!ok) y2Editor.setMeasure(line.getY2());
      }
      
      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link LineNode}. */
   private LineNode getLineNode() { return((LineNode) getEditedNode()); }
   
   /** Customized component for editing the x-coordinate of the line segment's first endpoint. */
   private MeasureEditor xEditor = null;

   /** Customized component for editing the y-coordinate of the line segment's first endpoint. */
   private MeasureEditor yEditor = null;

   /** Customized component for editing the x-coordinate of the line segment's second endpoint. */
   private MeasureEditor x2Editor = null;

   /** Customized component for editing the x-coordinate of the line segment's second endpoint. */
   private MeasureEditor y2Editor = null;
   
   /** Self-contained editor handles the label node's text styles (it has no draw styles). */
   private TextStyleEditor textStyleEditor = null;
   
   /** Self-contained editor handles the node's graphic style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
