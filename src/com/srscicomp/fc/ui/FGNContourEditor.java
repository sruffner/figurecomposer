package com.srscicomp.fc.ui;

import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;

import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.fig.ContourNode;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * <b>FGNContourEditor</b> displays/edits the properties of a contour plot node, {@link ContourNode}. This graphic node
 * presents a 3D data set in a 2D graph, either as a contour plot or heat map. The node has very few properties, so this
 * editor panel is very simple.
 * 
 * <p>The contour node's title is entered in a plain text field; however, the title is never rendered in the figure
 * itself -- it is for identification purposes only. On the next row  is an embedded {@link FGNPlottableDSCard}, which 
 * exposes the ID of the <i>xyzimg</i> data set referenced by the node, and provides facilities for viewing and/or 
 * editing that raw data set. Below this is a static label displaying the observed range in the Z-coordinate of the raw 
 * data. A drop-down combo box selects the display mode: contour lines only, filled contours, heat map image, or a
 * heat map with contour lines superimposed. A check box controls whether or not the heat map image is smoothed, while a
 * text field lets the user enter specific contour levels for the contour plot modes. An embedded {@link 
 * DrawStyleEditor} exposes the node's stroke-related properties.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNContourEditor extends FGNEditor implements ActionListener, FocusListener
{
   /** Construct the heatmap node properties editor. */
   FGNContourEditor()
   {
      JLabel titleLabel = new JLabel("Title ");
      add(titleLabel);
      titleField = new JTextField();
      titleField.addActionListener(this);
      titleField.addFocusListener(this);
      add(titleField);
      
      dsEditor = new FGNPlottableDSCard();
      add(dsEditor);

      zRangeLabel = new JLabel("Observed Range...");
      add(zRangeLabel);
      
      modeCombo = new JComboBox<ContourNode.DisplayMode>(ContourNode.DisplayMode.values());
      modeCombo.addActionListener(this);
      modeCombo.setToolTipText("Select display mode");
      add(modeCombo);

      JLabel levelLabel = new JLabel("Levels: ");
      add(levelLabel);
      levelListArea = new JTextArea();
      levelListArea.setLineWrap(true);
      levelListArea.setWrapStyleWord(true);
      levelListArea.addFocusListener(this);
      levelListArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "refresh");
      levelListArea.getActionMap().put("refresh", 
            new AbstractAction() {
               @Override public void actionPerformed(ActionEvent e) { refreshBtn.doClick(); }
      });
      levelListArea.setToolTipText("<html>Enter desired contour levels, separated by blanks. If empty, levels<br/>" +
            "are auto-selected. Up to " + ContourNode.MAXLEVELS + " levels accepted. " +
            "Repeat or out-of-range levels ignored.<br/>" +
            "<i>Hit refresh button (or <b>Shift+Enter</b>) to submit any changes.</i></html>");
      
      JScrollPane levelScroller = new JScrollPane(levelListArea);
      levelScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
      add(levelScroller);
      
      refreshBtn = new JButton(FCIcons.V4_REFRESH_22);
      refreshBtn.setMargin(new Insets(2,4,2,4));
      refreshBtn.setToolTipText("Press this button to submit changes in the contour level list.");
      refreshBtn.addActionListener(this);
      add(refreshBtn);
      
      smoothCheck = new JCheckBox("Smooth heat map?");
      smoothCheck.setContentAreaFilled(false);
      smoothCheck.addActionListener(this);
      add(smoothCheck);
      
      drawStyleEditor = new DrawStyleEditor(true, true);
      add(drawStyleEditor);

      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. The editor panel's width is determined by the embedded data set editor.
      layout.putConstraint(SpringLayout.WEST, titleLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, titleField, 0, SpringLayout.EAST, titleLabel);
      layout.putConstraint(SpringLayout.EAST, titleField, 0, SpringLayout.EAST, dsEditor);

      layout.putConstraint(SpringLayout.WEST, dsEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, dsEditor);

      layout.putConstraint(SpringLayout.WEST, zRangeLabel, GAP*2, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, modeCombo, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, smoothCheck, 0, SpringLayout.EAST, this);
      
      layout.putConstraint(SpringLayout.WEST, levelLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, levelScroller, 0, SpringLayout.EAST, levelLabel);
      layout.putConstraint(SpringLayout.EAST, refreshBtn, 0, SpringLayout.EAST, this);
      layout.putConstraint(SpringLayout.EAST, levelScroller, -GAP, SpringLayout.WEST, refreshBtn);

      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);

      // top-bottom constraints. 
      String vc = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(SpringLayout.NORTH, titleField, 0, SpringLayout.NORTH, this);
      layout.putConstraint(vc, titleLabel, 0, vc, titleField);
      layout.putConstraint(SpringLayout.NORTH, dsEditor, GAP, SpringLayout.SOUTH, titleField);
      layout.putConstraint(SpringLayout.NORTH, zRangeLabel, 0, SpringLayout.SOUTH, dsEditor);
      layout.putConstraint(SpringLayout.NORTH, modeCombo, GAP*3, SpringLayout.SOUTH, zRangeLabel);
      layout.putConstraint(vc, smoothCheck, 0, vc, modeCombo);
      
      layout.putConstraint(SpringLayout.NORTH, levelScroller, GAP, SpringLayout.SOUTH, modeCombo);
      layout.putConstraint(SpringLayout.SOUTH, levelScroller, GAP+70, SpringLayout.SOUTH, modeCombo);
      layout.putConstraint(vc, levelLabel, 0, vc, levelScroller);
      layout.putConstraint(vc, refreshBtn, 0, vc, levelScroller);

      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP*2, SpringLayout.SOUTH, levelScroller);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, drawStyleEditor);
   }

   @Override public void reload(boolean initial)
   {
      ContourNode cn = getContourNode();
      if(cn == null) return;
      
      titleField.setText(cn.getTitle());
      
      dsEditor.reload(cn);
      
      DataSet ds = cn.getDataSet();
      String label = "Observed range in Z: ";
      label += Utilities.toString(ds.getZMin(), 4, -1) + " .. " + Utilities.toString(ds.getZMax(), 4, -1);
      zRangeLabel.setText(label);
      modeCombo.setSelectedItem(cn.getMode());
      smoothCheck.setSelected(cn.isSmoothed());
      levelListArea.setText(cn.getLevelList());
      drawStyleEditor.loadGraphicNode(cn);
   }

   /** Ensure modeless pop-ups associated with any editor widgets are extinguished before editor panel is hidden. */
   @Override void onLowered() { drawStyleEditor.cancelEditing(); }

   /**
    * If the focus is on the title field, the special character is inserted at the current caret position. The user will
    * still have to hit the "Enter" key or shift focus away from the title field to submit the changes.
    */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(titleField.isFocusOwner()) titleField.replaceSelection(s);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.CONTOUR == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_HEATMAP_32); }
   @Override String getRepresentativeTitle() { return("Contour Plot Properties"); }

   @Override public void focusGained(FocusEvent e) 
   {
      if(e.isTemporary()) return;
      Object src = e.getSource();
      if(src == titleField) titleField.selectAll();;
   }
   @Override public void focusLost(FocusEvent e)
   {
      if(e.isTemporary()) return;
      Object src = e.getSource();
      if(src == titleField) titleField.postActionEvent();
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      ContourNode cn = getContourNode();
      if(cn == null) return;
      Object src = e.getSource();
      
      boolean ok = true; 
      if(src == smoothCheck)
         cn.setSmoothed(smoothCheck.isSelected());
      else if(src == titleField)
      {
         ok = cn.setTitle(titleField.getText());
         if(!ok) titleField.setText(cn.getTitle());
      }
      else if(src == modeCombo)
         cn.setMode((ContourNode.DisplayMode) modeCombo.getSelectedItem());
      else if(src == refreshBtn)
      {
         ok = cn.setLevelList(levelListArea.getText());
         if(!ok) levelListArea.setText(cn.getTitle());
      }
      
      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link ContourNode}. */
   private ContourNode getContourNode() { return((ContourNode) getEditedNode()); }
   
   /** Text field in which contour node's descriptive title is edited. */
   private JTextField titleField = null;

   /** Self-contained editor handles editing/loading of the data set assigned to the contour node. */
   private FGNPlottableDSCard dsEditor = null;
   
   /** Label displays the observed range in the Z-coordinate of data set assigned to the contour node. */
   private JLabel zRangeLabel = null;
   
   /** Combo box selects the display mode. */
   private JComboBox<ContourNode.DisplayMode> modeCombo = null;

   /** Contour level list is displayed/edited in this text area. */
   private JTextArea levelListArea = null;
   
   /** Click this button to update the contour level list as entered in the accompanying text widget. */
   private JButton refreshBtn = null;

   /** Check box enables/disables smoothing of the heat map image when rendered. */
   private JCheckBox smoothCheck = null;

   /** Self-contained editor handles the node's draw/stroke-related styles. */
   private DrawStyleEditor drawStyleEditor = null;
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
