package com.srscicomp.fc.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import com.srscicomp.common.ui.MultiButton;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.common.ui.RGBColorPicker;
import com.srscicomp.common.ui.TabStrip;
import com.srscicomp.common.ui.TabStripModel;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.fig.*;
import com.srscicomp.fc.fig.TickSetNode.LabelFormat;
import com.srscicomp.fc.uibase.StyledTextEditor;
import com.srscicomp.fc.uibase.ColorMapGradientRenderer;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
  * A reconfigurable editor for displaying and editing the properties of a {@link FGNGraphAxis} node, which encapsulates
 * the primary or secondary axis of the 2D multi-purpose graph container {@link com.srscicomp.fc.fig.GraphNode}, or the
 * color bar associated with any 2D or 3D graph container. A color bar is "axis like" because it includes an axis line
 * and is intended to represent how the range of Z data is mapped to a color gradient. The editor also manages the
 * number of tick mark sets defined on the axis, along with their properties.
 * 
 * <p>A 2D graph axis is represented by a {@link AxisNode}, while the color bar is encapsulated as
 * a {@link ColorBarNode}; both derive from <b>FGNGraphAxis</b>. Each tick mark set is defined by a
 * {@link TickSetNode} child of the axis or color bar. The editor provides a means of both adding
 * and removing tick sets from the parent: Click on the green "+" button in the editor's top-right corner to append a 
 * new tick set, or close any tick set tab to delete the corresponding tick set.
 * 
 * <p>The editor presents the axis or color bar properties in a tab entitled "Axis and Label", while the properties 
 * of each tick set child are displayed on a separate tab entitled "Major Ticks" (for the first tick mark set) or 
 * "Ticks N", where N is the ordinal position of the tick set node in the parent's child list. Although an axis or
 * color bar node can theoretically have any number of tick set child nodes, it is unlikely to have more than three, 
 * so this tabbed presentation is reasonable.</p>
 * 
 * <p>The editor "cards" that appear in the different tabs are implemented by inner classes: {@link XYAxisCard} 
 * displays the properties of a 2D graph axis; {@link ColorBarCard} shows the properties of a color bar; and {@link 
 * TickSetCard} shows the properties of a single tick set node.</p>
 * 
 * <p><i>NOTE</i>. This class intended only for use in one of the graph container editors. These are composite editors
 * that handle all the component nodes of a 2D or 3D graph. When the graph object is loaded for editing, these editors
 * must call {@link #loadAxis(FGNGraphAxis)} to specify the axis node that should be displayed and edited here.
 * Also note that this is not designed to handle the axis components of a polar plot or 3D graph, which are very
 * different from the axes of a 2D graph.</p>.
 *
 * @author sruffner
 */
final class FGNGraphAxisCard extends FGNEditor implements ActionListener, TabStripModel
{
   /** Construct the properties editor for a standard 2D graph axis or color bar. */
   FGNGraphAxisCard()
   {
      TabStrip tabStrip = new TabStrip(this);
      add(tabStrip);
      
      addTickSetBtn = new JButton(FCIcons.V4_ADD_22);
      addTickSetBtn.setToolTipText("Append a new tick marks set to this axis");
      addTickSetBtn.setBorder(null); addTickSetBtn.setContentAreaFilled(false); 
      addTickSetBtn.setMargin(new Insets(0,0,0,0));
      addTickSetBtn.setFocusable(false);
      addTickSetBtn.addActionListener(this);
      add(addTickSetBtn);
      
      contentPanel = new JPanel(new CardLayout());
      contentPanel.setOpaque(false);
      contentPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 2, 2, 2, tabStrip.getSelectionColor()), 
            BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP)
      ));
      contentPanel.add(xyAxisCard, CARD_XYAXIS);
      contentPanel.add(colorBarCard, CARD_CBAR);
      contentPanel.add(tickSetCard, CARD_TICKS);
      add(contentPanel);
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      layout.putConstraint(SpringLayout.WEST, contentPanel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, contentPanel);
      layout.putConstraint(SpringLayout.WEST, tabStrip, 0, SpringLayout.WEST, contentPanel);
      layout.putConstraint(SpringLayout.EAST, tabStrip, -GAP/2, SpringLayout.WEST, addTickSetBtn);
      layout.putConstraint(SpringLayout.EAST, addTickSetBtn, -GAP/2, SpringLayout.EAST, contentPanel);

      layout.putConstraint(SpringLayout.NORTH, tabStrip, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, addTickSetBtn, 0, SpringLayout.VERTICAL_CENTER, tabStrip);
      layout.putConstraint(SpringLayout.NORTH, contentPanel, 0, SpringLayout.SOUTH, tabStrip);
      layout.putConstraint(SpringLayout.SOUTH, this, GAP, SpringLayout.SOUTH, contentPanel);
   }
   
   @Override public void actionPerformed(ActionEvent e)
   {
      if(fgnAxis != null && e.getSource() == addTickSetBtn)
      {
         FGraphicModel fgm = fgnAxis.getGraphicModel();
         boolean ok = (fgm != null) && fgm.insertNode(fgnAxis, FGNodeType.TICKS, -1);
         if(ok) setSelectedTab(getNumTabs()-1);
      }
   }

   /**
    * Invoke this method to reconfigure that axis properties card to display the specified 2D graph axis.
    * @param axis The 2D graph axis or color to be loaded. If null, no action is taken, and editor should be hidden.
    */
   void loadAxis(FGNGraphAxis axis)
   {
      if(axis == null) return;

      fgnAxis = axis;
      reload(true);

      // switch to the first tab whenever we first load the axis node. We force a tab switch here to ensure that the
      // correct properties card is in front and that the tab strip is updated.
      iSelectedTab = -1;
      setSelectedTab(0);
   }

   @Override void reload(boolean initial)
   {
      xyAxisCard.reload(initial);
      colorBarCard.reload(initial);
      if(fgnAxis != null && fgnAxis.getChildCount() > 0)
      {
         if(initial) tickSetCard.loadTickSet((TickSetNode) fgnAxis.getChildAt(0));
         else tickSetCard.reload(false);
      }
   }

   @Override void onRaised()
   {
      // delegate to the card that is currently "on top"
      if(fgnAxis == null) return;
      FGNEditor card = tickSetCard;
      if(iSelectedTab == 0) card = fgnAxis.isColorBar() ? colorBarCard : xyAxisCard;
      card.onRaised();
   }

   @Override void onLowered()
   {
      // delegate to the card that is currently "on top"
      if(fgnAxis == null) return;
      FGNEditor card = tickSetCard;
      if(iSelectedTab == 0) card = fgnAxis.isColorBar() ? colorBarCard : xyAxisCard;
      card.onLowered();
   }

   @Override void onInsertSpecialCharacter(String s)
   {
      // delegate to the card that is currently "on top"
      if(fgnAxis == null) return;
      FGNEditor card = tickSetCard;
      if(iSelectedTab == 0) card = fgnAxis.isColorBar() ? colorBarCard : xyAxisCard;
      card.onInsertSpecialCharacter(s);
   }

   @Override boolean isEditorForNode(FGraphicNode n)
   {
      FGNodeType type = (n != null) ? n.getNodeType() : null;
      return(type == FGNodeType.AXIS || type == FGNodeType.CBAR);
   }
   @Override ImageIcon getRepresentativeIcon() { return(null); }
   @Override String getRepresentativeTitle() { return(null); }

   //
   // TabStripModel
   //
   
   /** 
    * Total number of tabs is one more than the number of tick sets defined on the axis. First tab holds the
    * properties of the axis node itself; the remaining tabs display the properties of each tick set child.
    */
   @Override public int getNumTabs() { return(1 + (fgnAxis == null ? 0 : fgnAxis.getChildCount())); }

   @Override public int getSelectedTab() { return(iSelectedTab); }

   @Override public void setSelectedTab(int tabPos)
   {
      if(tabPos < 0 || tabPos >= getNumTabs() || tabPos == iSelectedTab) return;
      
      iSelectedTab = tabPos;
      fireStateChanged();

      // if we're switching to a tab corresponding to one of the defined tick sets, make sure that tick set is
      // loaded into the tick set node properties card
      if(iSelectedTab > 0)
      {
        tickSetCard.loadTickSet( (TickSetNode) fgnAxis.getChildAt(iSelectedTab-1) );
      }
      
      // ensure the correct properties card is brought to the front in the tab content panel
      String cardID = CARD_TICKS;
      if(iSelectedTab == 0) cardID = fgnAxis.isColorBar() ? CARD_CBAR : CARD_XYAXIS;
      ((CardLayout) contentPanel.getLayout()).show(contentPanel, cardID);
   }

   @Override public void addChangeListener(ChangeListener l) 
   { 
      if(l != null) tabListeners.add(ChangeListener.class, l);
   }
   @Override public void removeChangeListener(ChangeListener l)
   {
      if(l != null) tabListeners.remove(ChangeListener.class, l);
   }

   /** 
    * First tab shows the axis node's properties; its label is always "Axis & Label". Remaining tabs show properties of 
    * each tick set node defined on the axis. The first such tab is specially labelled "Major Ticks", while the
    * remaining tabs are labelled generically: "Ticks N".
    */
   @Override public String getTabLabel(int tabPos)
   {
      String s;
      if(tabPos == 0) s = "Axis & Label";
      else if(tabPos == 1) s = "Major Ticks";
      else s = "Ticks " + tabPos;
      return(s);
   }

   @Override public String getTabToolTip(int tabPos) { return(null); }
   @Override public Icon getTabIcon(int tabPos) { return(null); }
   @Override public boolean isTabClosable(int tabPos) { return(tabPos > 0); }
   @Override public String getCloseTabToolTip(int tabPos) { return(tabPos > 0 ? "Delete this tick set" : null); }

   /** Any tab after the first may be closed, which effectively deletes the corresponding tick set node. */
   @Override public void closeTab(int tabPos)
   {
      if(fgnAxis != null && tabPos > 0)
      {
         FGraphicModel fgm = fgnAxis.getGraphicModel();
         if(fgm != null && fgm.deleteNode(fgnAxis.getChildAt(tabPos-1)))
         {
            if(tabPos == iSelectedTab) setSelectedTab(tabPos-1);
            else fireStateChanged();
         }
      }
   }

   @Override public boolean supportsTabRepositioning() { return(false); }
   @Override public boolean moveTab(int fromPos, int toPos) { return(false); }

   
   /** 
    * Notifies any change listeners that the list of tabs has changed in some way -- including the identity of the 
    * currently selected tab.
    */
   private void fireStateChanged()
   {
      ChangeEvent e = new ChangeEvent(this);
      Object[] listeners = tabListeners.getListenerList();
      for (int i = listeners.length-2; i>=0; i-=2) if (listeners[i] == ChangeListener.class)
         ((ChangeListener)listeners[i+1]).stateChanged(e);
   }
   
   /** The axis node currently loaded into the axis properties editor. */
   private FGNGraphAxis fgnAxis = null;
   
   /** The index of the selected tab. A positive index N corresponds to the (N-1)th tick set child of the axis. */
   private int iSelectedTab = -1;
   
   /** List of all change listeners registered with the tab strip model. */
   private final EventListenerList tabListeners = new EventListenerList();

   /** Clicking this button will append a new tick set node to the axis. */
   private final JButton addTickSetBtn;
  /** The container for the properties editor currently selected via the tab strip. Uses a card layout. */
   private JPanel contentPanel = null;
   
   /** The properties editor for a 2D graph's primary or secondary axis node. */
   private final XYAxisCard xyAxisCard = new XYAxisCard();
   /** The card ID assigned to the editor for a 2D graph's primary or secondary axis node. */
   private final static String CARD_XYAXIS = "X/Y Axis";
   
   /** The properties editor specific to a graph's color bar. */
   private final ColorBarCard colorBarCard = new ColorBarCard();
   /** The card ID assigned to the color bar properties editor. */
   private final static String CARD_CBAR = "CBar";
   
   /** The properties editor for a tick mark set. */
   private final TickSetCard tickSetCard = new TickSetCard();
   /** The card ID assigned to the tick mark set properties editor. */
   private final static String CARD_TICKS = "Ticks";

   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;

   /**
    * Helper class implements the subordinate editor card that appears in the "Axis & Label" tab of the {@link 
    * FGNGraphAxisCard} when the currently loaded {@link FGNGraphAxis} is an instance of {@link 
    * AxisNode}, representing the primary or secondary axis of a 2D graph node. It displays/edits
    * the axis properties in the following widgets:
    * <ul>
    * <li>Numeric text fields specifying the start and end of the axis range. (Note that these are disabled if auto-
    * range adjustment has been enabled for the axis on display.)</li>
    * <li>Custom text fields specifying the measured distance separating the axis line from the nearest edge of the 
    * parent graph's data window, and the distance separating the axis label from the axis line.</li>
    * <li>A check box to show/hide the axis, and configure it as log10 or log2 (logarithmic axes only).</li>
    * <li>A text field to edit the optional units string.</li>
    * <li>An {@link StyledTextEditor} to edit the axis label, which can contain attributed text and linefeed
    * characters.</li>
    * <li>Numeric text field specifying the font leading, which affects the spacing between consecutive text lines when
    * the axis' auto-generated label is multi-line.</li>
    * <li>Numeric text field specifying the axis power scale factor.</li>
    * <li>A {@link TextStyleEditor} for editing the node's text-related styles, and a {@link DrawStyleEditor} for its
    * draw-related styles.</li>
    * </ul>
    * 
    * @author sruffner
    */
   private class XYAxisCard extends FGNEditor implements ActionListener, FocusListener
   {
      XYAxisCard()
      {
         // we layout everything in a transparent panel that, in turn, is placed in the "north" of the editor panel --
         // to ensure things don't get spread out vertically
         JPanel p = new JPanel(new SpringLayout());
         p.setOpaque(false);
         SpringLayout layout = (SpringLayout) p.getLayout();

         // axis range is in user units, which could be anything. We want to be able to display ranges across many
         // orders of magnitude [1e3 .. 1e10] as well as more normal ranges like [5..10]. We place no limit on 
         // fractional digits but restrict to just 4 significant digits to keep things compact.

         startField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 4, -1);
         startField.setToolTipText("Enter start of axis range");
         startField.addActionListener(this);
         p.add(startField);
         
         JLabel toLabel = new JLabel(" to ");
         p.add(toLabel);
         endField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 4, -1);
         endField.setToolTipText("Enter end of axis range");
         endField.addActionListener(this);
         p.add(endField);
         
         log2Chk = new JCheckBox("Base2?");
         log2Chk.setToolTipText("If checked, axis's logarithmic base is 2 rather than 10 (when applicable)");
         log2Chk.setContentAreaFilled(false);
         log2Chk.addActionListener(this);
         p.add(log2Chk);
         
         hideChk = new JCheckBox("Hide?");
         hideChk.setContentAreaFilled(false);
         hideChk.addActionListener(this);
         p.add(hideChk);
         
         contentEditor = new StyledTextEditor(2);
         contentEditor.addActionListener(this);
         p.add(contentEditor);
         
         JLabel offsetLabel = new JLabel("Offset: ");
         p.add(offsetLabel);
         offsetEditor = new MeasureEditor(0, AxisNode.SPACERCONSTRAINTS);
         offsetEditor.setToolTipText("Enter distance between axis line and its label (0..2in)");
         offsetEditor.addActionListener(this);
         p.add(offsetEditor);
         
         JLabel spacerLabel = new JLabel("Gap: ");
         p.add(spacerLabel);
         spacerEditor = new MeasureEditor(0, AxisNode.SPACERCONSTRAINTS);
         spacerEditor.setToolTipText(
               "Enter distance between axis line and nearest edge of graph's data window (0..2in)");
         spacerEditor.addActionListener(this);
         p.add(spacerEditor);
         
         JLabel lineHtLabel = new JLabel("Line Ht: ");
         p.add(lineHtLabel);
         lineHtField = new NumericTextField(0.8, 3.0, 2);
         lineHtField.setToolTipText(
               "<html>Axis label text line height as a fraction of font size [0.8 .. 3]. Applicable<br/>" +
               "only when the axis's label is multi-line.</html>");
         lineHtField.addActionListener(this);
         p.add(lineHtField);
         
         JLabel scaleLabel = new JLabel("Scale: ");
         p.add(scaleLabel);
         scaleField = new NumericTextField(FGNGraphAxis.MINPOWERSCALE, FGNGraphAxis.MAXPOWERSCALE);
         scaleField.setToolTipText(
               "<html>Axis power scale factor N. Tick mark labels are scale by 10^N and '(x1E^N)' is<br/>" +
               "appended to the axis label.</html>");
         scaleField.addActionListener(this);
         p.add(scaleField);

         JLabel unitsLabel = new JLabel("Units: ");
         p.add(unitsLabel);
         unitsField = new JTextField(3);
         unitsField.addActionListener(this);
         unitsField.addFocusListener(this);
         unitsField.setToolTipText("Enter unit string here (optional); applied to any calibration bars in graph");
         p.add(unitsField);
         

         textStyleEditor = new TextStyleEditor();
         p.add(textStyleEditor);
         drawStyleEditor = new DrawStyleEditor(true, true);
         p.add(drawStyleEditor);
         
         // left-right constraints. Assume row with textStyleEditor will be longest.
         layout.putConstraint(SpringLayout.WEST, startField, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, toLabel, 0, SpringLayout.EAST, startField);
         layout.putConstraint(SpringLayout.WEST, endField, 0, SpringLayout.EAST, toLabel);
         layout.putConstraint(SpringLayout.EAST, log2Chk, 0, SpringLayout.WEST, hideChk);
         layout.putConstraint(SpringLayout.EAST, hideChk, 0, SpringLayout.EAST, textStyleEditor);

         layout.putConstraint(SpringLayout.WEST, contentEditor, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.EAST, contentEditor, 0, SpringLayout.EAST, textStyleEditor);
         
         layout.putConstraint(SpringLayout.WEST, offsetLabel, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, offsetEditor, 0, SpringLayout.EAST, offsetLabel);
         layout.putConstraint(SpringLayout.WEST, lineHtLabel, GAP*2, SpringLayout.EAST, offsetEditor);
         layout.putConstraint(SpringLayout.WEST, lineHtField, 0, SpringLayout.EAST, lineHtLabel);
         layout.putConstraint(SpringLayout.WEST, unitsLabel, GAP*2, SpringLayout.EAST, lineHtField);
         layout.putConstraint(SpringLayout.WEST, unitsField, 0, SpringLayout.EAST, unitsLabel);

         
         layout.putConstraint(SpringLayout.EAST, spacerLabel, 0, SpringLayout.EAST, offsetLabel);
         layout.putConstraint(SpringLayout.WEST, spacerEditor, 0, SpringLayout.WEST, offsetEditor);
         layout.putConstraint(SpringLayout.EAST, scaleLabel, 0, SpringLayout.EAST, lineHtLabel);
         layout.putConstraint(SpringLayout.WEST, scaleField, 0, SpringLayout.WEST, lineHtField);
         

         layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.EAST, p, 0, SpringLayout.EAST, textStyleEditor);
         
         layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, p);
         
         // top-bottom constraints: A widget in each row is used to set the constraints with row above or below. The 
         // remaining widgets are vertically centered WRT that widget.
         layout.putConstraint(SpringLayout.NORTH, startField, 0, SpringLayout.NORTH, p);
         layout.putConstraint(SpringLayout.NORTH, contentEditor, GAP*2, SpringLayout.SOUTH, startField);
         layout.putConstraint(SpringLayout.NORTH, offsetEditor, GAP*2, SpringLayout.SOUTH, contentEditor);
         layout.putConstraint(SpringLayout.NORTH, spacerEditor, GAP, SpringLayout.SOUTH, offsetEditor);
         layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, spacerEditor);
         layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
         layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, drawStyleEditor);
         
         String vCtr = SpringLayout.VERTICAL_CENTER;
         layout.putConstraint(vCtr, toLabel, 0, vCtr, startField);
         layout.putConstraint(vCtr, endField, 0, vCtr, startField);
         layout.putConstraint(vCtr, log2Chk, 0, vCtr, startField);
         layout.putConstraint(vCtr, hideChk, 0, vCtr, startField);

         layout.putConstraint(vCtr, offsetLabel, 0, vCtr, offsetEditor);
         layout.putConstraint(vCtr, lineHtLabel, 0, vCtr, offsetEditor);
         layout.putConstraint(vCtr, lineHtField, 0, vCtr, offsetEditor);
         layout.putConstraint(vCtr, unitsLabel, 0, vCtr, offsetEditor);
         layout.putConstraint(vCtr, unitsField, 0, vCtr, offsetEditor);

         layout.putConstraint(vCtr, spacerLabel, 0, vCtr, spacerEditor);
         layout.putConstraint(vCtr, scaleLabel, 0, vCtr, spacerEditor);
         layout.putConstraint(vCtr, scaleField, 0, vCtr, spacerEditor);

         setLayout(new BorderLayout());
         add(p, BorderLayout.NORTH);
      }

      private AxisNode getAxisNodeEdited()
      {
         return((fgnAxis instanceof AxisNode) ? ((AxisNode) fgnAxis) : null);
      }
      
      @Override void reload(boolean initial)
      {
         AxisNode axis = getAxisNodeEdited();
         if(axis == null) return;
         
         startField.setValue(axis.getStart());
         endField.setValue(axis.getEnd());
         scaleField.setValue(axis.getPowerScale());
         unitsField.setText(axis.getUnits());
         spacerEditor.setMeasure(axis.getSpacer());
         log2Chk.setSelected(axis.getLog2());
         hideChk.setSelected(axis.getHide());

         boolean auto = axis.isAutoranged();
         startField.setEnabled(!auto);
         endField.setEnabled(!auto);
         
         contentEditor.loadContent(axis.getAttributedTitle(false), axis.getFontFamily(), 0, axis.getFillColor(), 
               axis.getFontStyle());
         offsetEditor.setMeasure(axis.getLabelOffset());
         lineHtField.setValue(axis.getLineHeight());
         
         textStyleEditor.loadGraphicNode(axis);
         drawStyleEditor.loadGraphicNode(axis);
      }

      @Override void onLowered()
      {
         textStyleEditor.cancelEditing();
         drawStyleEditor.cancelEditing();
      }

      /**
       * If the focus is on the label editor or the units text field, the special character is inserted at the 
       * current caret position. 
       */
      @Override void onInsertSpecialCharacter(String s)
      {
         if(contentEditor.isFocusOwner()) contentEditor.insertString(s, false);
         else if(unitsField.isFocusOwner()) unitsField.replaceSelection(s);
      }

      @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && n.getNodeType() == FGNodeType.AXIS); }
      @Override ImageIcon getRepresentativeIcon() { return(null); }
      @Override String getRepresentativeTitle() { return(null); }

      @Override public void focusGained(FocusEvent e) 
      {
         if(e.isTemporary()) return;
         if(e.getSource() == unitsField) unitsField.selectAll();
      }
      @Override public void focusLost(FocusEvent e)
      {
         if((!e.isTemporary()) && (e.getSource() == unitsField)) unitsField.postActionEvent();
      }

      @Override public void actionPerformed(ActionEvent e)
      {
         AxisNode axis = getAxisNodeEdited();
         if(axis == null) return;
         Object src = e.getSource();

         boolean ok = true;
         if(src == contentEditor)
         {
            String text = FGraphicNode.toStyledText(
                  contentEditor.getCurrentContents(), axis.getFontStyle(), axis.getFillColor());
            ok = axis.setTitle(text);
            if(!ok) contentEditor.loadContent(axis.getAttributedTitle(false), axis.getFontFamily(), 0, 
                  axis.getFillColor(), axis.getFontStyle());
         }
         else if(src == unitsField)
         {
            ok = axis.setUnits(unitsField.getText());
            if(!ok) unitsField.setText(axis.getUnits());
         }
         else if(src == hideChk)
            axis.setHide(hideChk.isSelected());
         else if(src == log2Chk)
            axis.setLog2(log2Chk.isSelected());
         else if(src == startField)
         {
            ok = axis.setStart(startField.getValue().doubleValue());
            if(!ok) startField.setValue(axis.getStart());
         }
         else if(src == endField)
         {
            ok = axis.setEnd(endField.getValue().doubleValue());
            if(!ok) endField.setValue(axis.getEnd());
         }
         else if(src == scaleField)
         {
            ok = axis.setPowerScale(scaleField.getValue().intValue());
            if(!ok) scaleField.setValue(axis.getPowerScale());
         }
         else if(src == spacerEditor)
         {
            ok = axis.setSpacer(spacerEditor.getMeasure());
            if(!ok) spacerEditor.setMeasure(axis.getSpacer());
         }
         else if(src == offsetEditor)
         {
            ok = axis.setLabelOffset(offsetEditor.getMeasure());
            if(!ok) offsetEditor.setMeasure(axis.getLabelOffset());
         }
         else if(src == lineHtField)
         {
            ok = axis.setLineHeight(lineHtField.getValue().doubleValue());
            if(!ok) lineHtField.setValue(axis.getLineHeight());
         }

         if(!ok) Toolkit.getDefaultToolkit().beep();
      }

      /** Numeric text field edits the axis range start. */
      private final NumericTextField startField;

      /** Numeric text field edits the axis range end. */
      private NumericTextField endField = null;

      /** When checked, axis's logarithmic base is 2 rather than 10 (applicable only when axis is logarithmic). */
      private JCheckBox log2Chk = null;

      /** When checked, the axis is entirely hidden. */
      private JCheckBox hideChk = null;

      /** 
       * Axis label is edited within this custom widget, which allows user to change font style or text color anywhere
       * in the content, add superscripts or subscripts, and underline portions of the text. Allows linefeeds, too.
       */
      private StyledTextEditor contentEditor = null;
      
      /** Customized component for editing distance between axis and nearest edge of graph's data window. */
      private MeasureEditor spacerEditor = null;

      /** Customized component for editing offset from axis to its label. */
      private MeasureEditor offsetEditor = null;

      /** Text field edits the axis units string. */
      private JTextField unitsField = null;
      
      /** Numeric text field for editing the text line height for a multi-line axis label. */
      private NumericTextField lineHtField = null;

      /** Numeric text field for editing the axis power scale factor (integer base-10 exponent). */
      private NumericTextField scaleField = null;

      /** Self-contained editor handles the node's text styles. */
      private TextStyleEditor textStyleEditor = null;
      
      /** Self-contained editor handles the node's draw style properties. */
      private DrawStyleEditor drawStyleEditor = null;
   }

   /**
    * Helper class implements the subordinate editor card that appears in the "Axis & Label" tab of the {@link 
    * FGNGraphAxisCard} when the currently loaded {@link FGNGraphAxis} is an instance of {@link ColorBarNode}, 
    * representing a graph's color bar. It displays/edits the color bar's properties in the following widgets:
    * 
    * <ul>
    * <li>Numeric text fields specifying the start and end of the color bar's axis range. (Note that these are disabled 
    * if auto-range adjustment has been enabled for the color bar on display.)</li>
    * <li>A custom-painted combo box selects the assigned color map; a checkbox indicates whether or not the color map
    * is reversed in direction; a regular combo box selects the color-mapping mode; another combo box indicates where 
    * the color bar is rendered (along left, right, top or bottom edge of the parent graph); and a custom color picker 
    * button chooses the color assigned to ill-defined (NaN) data when a heatmap is rendered in the parent graph.</li>
    * <li>Custom text fields specify the gap between the color bar's gradient bar and the nearest edge of the graph's
    * bounding box; the width of the color-map gradient bar; the gap between the bar and the axis line; and the gap 
    * between the axis line and the axis label.</li>
    * <li>A check box to show/hide the color bar.</li>
    * <li>A text field for specifying the axis label for the color bar.</li>
    * <li>Numeric text field specifying the font leading, which affects the spacing between consecutive text lines when
    * the axis's auto-generated label is multi-line.</li>
    * <li>Numeric text field specifying axis power scale factor.</li>
    * <li>A {@link TextStyleEditor} for editing the node's text-related styles, and a {@link DrawStyleEditor} for its
    * draw-related styles.</li>
    * </ul>
    * 
    * @author sruffner
    */
   private class ColorBarCard extends FGNEditor implements ActionListener, PropertyChangeListener
   {
      ColorBarCard()
      {
         // we layout everything in a transparent panel that, in turn, is placed in the "north" of the editor panel --
         // to ensure things don't get spread out vertically
         JPanel p = new JPanel(new SpringLayout());
         p.setOpaque(false);
         SpringLayout layout = (SpringLayout) p.getLayout();

         // axis range is in user units, which could be anything. We want to be able to display ranges across many
         // orders of magnitude [1e3 .. 1e10] as well as more normal ranges like [5..10]. We place no limit on 
         // fractional digits but restrict to just 4 significant digits to keep things compact.
         
         startField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 4, -1);
         startField.setToolTipText("Enter start of axis range");
         startField.addActionListener(this);
         p.add(startField);
         
         cmapCombo = new JComboBox<>(FGNPreferences.getInstance().getAllAvailableColorMaps());
         ColorMapGradientRenderer renderer = new ColorMapGradientRenderer();
         renderer.setPreferredSize(new Dimension(150, 25));
         cmapCombo.setRenderer(renderer);
         cmapCombo.addActionListener(this);
         cmapCombo.setToolTipText("Select indexed colormap");
         p.add(cmapCombo);
         
         endField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 4, -1);
         endField.setToolTipText("Enter end of axis range");
         endField.addActionListener(this);
         p.add(endField);
         
         edgeLocCombo = new JComboBox<>(ColorBarNode.Edge.values());
         edgeLocCombo.setToolTipText("Select the graph edge beside which color axis is drawn");
         edgeLocCombo.addActionListener(this);
         p.add(edgeLocCombo);
         
         cmapModeCombo = new JComboBox<>(ColorBarNode.CMapMode.values());
         cmapModeCombo.setToolTipText("Select datum-to-color index mapping mode");
         cmapModeCombo.addActionListener(this);
         p.add(cmapModeCombo);
         
         colorNaNPicker = new RGBColorPicker(false);
         colorNaNPicker.setToolTipText("Select the color to which all ill-defined (infinite or NaN) data are mapped");
         colorNaNPicker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
         p.add(colorNaNPicker);
         
         JLabel nanLabel = new JLabel(" NaN");
         p.add(nanLabel);
         
         revChk = new JCheckBox("Reverse?");
         revChk.setContentAreaFilled(false);
         revChk.setToolTipText("Check this box to reverse the direction of the indexed colormap");
         revChk.addActionListener(this);
         p.add(revChk);
         
         hideChk = new JCheckBox("Hide?");
         hideChk.setContentAreaFilled(false);
         hideChk.addActionListener(this);
         p.add(hideChk);
         
         contentEditor = new StyledTextEditor(2);
         contentEditor.addActionListener(this);
         p.add(contentEditor);
                  
         JLabel offsetLabel = new JLabel("Offset ");
         p.add(offsetLabel);
         offsetEditor = new MeasureEditor(0, AxisNode.SPACERCONSTRAINTS);
         offsetEditor.setToolTipText("Enter label offset from axis line (0..2in)");
         offsetEditor.addActionListener(this);
         p.add(offsetEditor);

         JLabel wLabel = new JLabel("Bar Width ");
         p.add(wLabel);
         barSizeEditor = new MeasureEditor(0, ColorBarNode.GAPCONSTRAINTS);
         barSizeEditor.setToolTipText("<html>Enter width of gradient bar <i>(0..2in)</i></html>");
         barSizeEditor.addActionListener(this);
         p.add(barSizeEditor);
         
         JLabel gapLabel = new JLabel("Gap ");
         p.add(gapLabel);
         gapEditor = new MeasureEditor(0, ColorBarNode.GAPCONSTRAINTS);
         gapEditor.setToolTipText(
               "<html>Enter size of gap between the graph edge beside which color bar is drawn <br>" +
               "and the adjacent edge of the gradient bar <i>(0..2in)</i></html>");
         gapEditor.addActionListener(this);
         p.add(gapEditor);
         
         JLabel spacerLabel = new JLabel("Spacer ");
         p.add(spacerLabel);
         spacerEditor = new MeasureEditor(0, ColorBarNode.GAPCONSTRAINTS);
         spacerEditor.setToolTipText(
            "<html>Enter size of gap between axis line and the adjacent edge of gradient bar <i>(0..2in)</i></html>");
         spacerEditor.addActionListener(this);
         p.add(spacerEditor);
         
         JLabel lineHtLabel = new JLabel("Line Ht ");
         p.add(lineHtLabel);
         lineHtField = new NumericTextField(0.8, 3.0, 2);
         lineHtField.setToolTipText(
               "<html>Color bar label text line height as a fraction of font size [0.8 .. 3]. Applicable<br/>" +
               "only when the label is multi-line.</html>");
         lineHtField.addActionListener(this);
         p.add(lineHtField);
         
         JLabel scaleLabel = new JLabel("Scale ");
         p.add(scaleLabel);
         scaleField = new NumericTextField(FGNGraphAxis.MINPOWERSCALE, FGNGraphAxis.MAXPOWERSCALE);
         scaleField.setToolTipText(
               "<html>Axis power scale factor N. Tick mark labels are scale by 10^N and '(x1E^N)' is<br/>" +
               "appended to the axis label.</html>");
         scaleField.addActionListener(this);
         p.add(scaleField);
         
         textStyleEditor = new TextStyleEditor();
         p.add(textStyleEditor);
         drawStyleEditor = new DrawStyleEditor(true, true);
         p.add(drawStyleEditor);
         
         // left-right constraints. Assume row with textStyleEditor will be longest.
         layout.putConstraint(SpringLayout.WEST, startField, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, cmapCombo, GAP, SpringLayout.EAST, startField);
         layout.putConstraint(SpringLayout.EAST, cmapCombo, -GAP, SpringLayout.WEST, endField);
         layout.putConstraint(SpringLayout.EAST, endField, 0, SpringLayout.EAST, p);
         
         layout.putConstraint(SpringLayout.WEST, edgeLocCombo, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, cmapModeCombo, GAP, SpringLayout.EAST, edgeLocCombo);
         layout.putConstraint(SpringLayout.WEST, colorNaNPicker, GAP, SpringLayout.EAST, cmapModeCombo);
         layout.putConstraint(SpringLayout.WEST, nanLabel, 0, SpringLayout.EAST, colorNaNPicker);
         layout.putConstraint(SpringLayout.EAST, revChk, -5, SpringLayout.WEST, hideChk);
         layout.putConstraint(SpringLayout.EAST, hideChk, 0, SpringLayout.EAST, textStyleEditor);


         layout.putConstraint(SpringLayout.WEST, contentEditor, 0, SpringLayout.WEST, textStyleEditor);
         layout.putConstraint(SpringLayout.EAST, contentEditor, 0, SpringLayout.EAST, textStyleEditor);
         
         layout.putConstraint(SpringLayout.WEST, offsetLabel, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, offsetEditor, 0, SpringLayout.EAST, offsetLabel);
         layout.putConstraint(SpringLayout.WEST, wLabel, GAP*3, SpringLayout.EAST, offsetEditor);
         layout.putConstraint(SpringLayout.WEST, barSizeEditor, 0, SpringLayout.EAST, wLabel);
         
         layout.putConstraint(SpringLayout.EAST, gapLabel, 0, SpringLayout.EAST, offsetLabel);
         layout.putConstraint(SpringLayout.WEST, gapEditor, 0, SpringLayout.WEST, offsetEditor);
         layout.putConstraint(SpringLayout.EAST, spacerLabel, 0, SpringLayout.EAST, wLabel);
         layout.putConstraint(SpringLayout.WEST, spacerEditor, 0, SpringLayout.WEST, barSizeEditor);
         
         layout.putConstraint(SpringLayout.EAST, scaleLabel, 0, SpringLayout.EAST, offsetLabel);
         layout.putConstraint(SpringLayout.WEST, scaleField, 0, SpringLayout.WEST, offsetEditor);
         layout.putConstraint(SpringLayout.EAST, lineHtLabel, 0, SpringLayout.EAST, wLabel);
         layout.putConstraint(SpringLayout.WEST, lineHtField, 0, SpringLayout.WEST, barSizeEditor);

         layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.EAST, p, 0, SpringLayout.EAST, textStyleEditor);
         
         layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, p);
         
         // top-bottom constraints: A widget in each row is used to set the constraints with row above or below. The 
         // remaining widgets are vertically centered WRT that widget.
         // SPECIAL NOTE: We found that, for the first row of widgets, the cmapCombo was the tallest under Windows 7, 
         // while the numeric text field was tallest under Mac. We make a platform-specific tweak here. Not good!
         Component row1Tallest = Utilities.isWindows() ? cmapCombo : startField;
         Component row1Other = Utilities.isWindows() ? startField : cmapCombo;
         
         layout.putConstraint(SpringLayout.NORTH, row1Tallest, 0, SpringLayout.NORTH, p);
         layout.putConstraint(SpringLayout.NORTH, colorNaNPicker, GAP, SpringLayout.SOUTH, row1Tallest);
         layout.putConstraint(SpringLayout.NORTH, contentEditor, GAP*2, SpringLayout.SOUTH, colorNaNPicker);
         layout.putConstraint(SpringLayout.NORTH, offsetEditor, GAP*2, SpringLayout.SOUTH, contentEditor);
         layout.putConstraint(SpringLayout.NORTH, gapEditor, GAP, SpringLayout.SOUTH, offsetEditor);
         layout.putConstraint(SpringLayout.NORTH, scaleField, GAP, SpringLayout.SOUTH, gapEditor);
         layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, scaleField);
         layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
         layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, drawStyleEditor);
         
         String vCtr = SpringLayout.VERTICAL_CENTER;
         layout.putConstraint(vCtr, row1Other, 0, vCtr, row1Tallest);
         layout.putConstraint(vCtr, endField, 0, vCtr, row1Tallest);
         
         layout.putConstraint(vCtr, edgeLocCombo, 0, vCtr, colorNaNPicker);
         layout.putConstraint(vCtr, cmapModeCombo, 0, vCtr, colorNaNPicker);
         layout.putConstraint(vCtr, nanLabel, 0, vCtr, colorNaNPicker);
         layout.putConstraint(vCtr, revChk, 0, vCtr, colorNaNPicker);
         layout.putConstraint(vCtr, hideChk, 0, vCtr, colorNaNPicker);

         layout.putConstraint(vCtr, offsetLabel, 0, vCtr, offsetEditor);
         layout.putConstraint(vCtr, wLabel, 0, vCtr, offsetEditor);
         layout.putConstraint(vCtr, barSizeEditor, 0, vCtr, offsetEditor);
         
         layout.putConstraint(vCtr, gapLabel, 0, vCtr, gapEditor);
         layout.putConstraint(vCtr, spacerLabel, 0, vCtr, gapEditor);
         layout.putConstraint(vCtr, spacerEditor, 0, vCtr, gapEditor);

         layout.putConstraint(vCtr, scaleLabel, 0, vCtr, scaleField);
         layout.putConstraint(vCtr, lineHtLabel, 0, vCtr, scaleField);
         layout.putConstraint(vCtr, lineHtField, 0, vCtr, scaleField);

         setLayout(new BorderLayout());
         add(p, BorderLayout.NORTH);
      }
      
      private ColorBarNode getColorBarNodeEdited()
      {
         return((fgnAxis instanceof ColorBarNode) ? ((ColorBarNode) fgnAxis) : null);
      }
      
      
      @Override void reload(boolean initial)
      {
         ColorBarNode colorBar = getColorBarNodeEdited();
         if(colorBar == null) return;
         
         // we reload the combo box with all available color maps, in case any changes were made outside the context
         // of this editor. It could be that the color bar's current color map is a custom one that is not currently
         // in the user's list of custom color maps (stored in preferences). In this case, we add the map to the
         // combo box list
         cmapCombo.removeActionListener(this);
         cmapCombo.removeAllItems();
         ColorMap[] allMaps = FGNPreferences.getInstance().getAllAvailableColorMaps();
         ColorMap currentCM = colorBar.getColorMap();
         boolean found = false;
         for(ColorMap cm : allMaps)
         {
            if(!found) found = cm.equals(currentCM);  // Note: we don't require that the map names match.
            cmapCombo.addItem(cm);
         }
         if(!found) cmapCombo.addItem(currentCM);
         cmapCombo.setSelectedItem(colorBar.getColorMap());
         cmapCombo.addActionListener(this);
         
         startField.setValue(colorBar.getStart());
         endField.setValue(colorBar.getEnd());
         cmapModeCombo.setSelectedItem(colorBar.getColorMapMode());
         colorNaNPicker.setCurrentColor(colorBar.getColorNaN(), false);
         
         boolean auto = colorBar.isAutoranged();
         startField.setEnabled(!auto);
         endField.setEnabled(!auto);
         
         contentEditor.loadContent(colorBar.getAttributedTitle(false), colorBar.getFontFamily(), 0, 
               colorBar.getFillColor(), colorBar.getFontStyle());
         offsetEditor.setMeasure(colorBar.getLabelOffset());

         edgeLocCombo.setSelectedItem(colorBar.getEdge());
         barSizeEditor.setMeasure(colorBar.getBarSize());
         gapEditor.setMeasure(colorBar.getGap());
         spacerEditor.setMeasure(colorBar.getBarAxisSpacer());
         revChk.setSelected(colorBar.isReversed());
         hideChk.setSelected(colorBar.getHide());
         lineHtField.setValue(colorBar.getLineHeight());
         scaleField.setValue(colorBar.getPowerScale());
         
         textStyleEditor.loadGraphicNode(colorBar);
         drawStyleEditor.loadGraphicNode(colorBar);
      }

      @Override void onLowered()
      {
        colorNaNPicker.cancelPopup();
        textStyleEditor.cancelEditing();
        drawStyleEditor.cancelEditing();
      }

      /**
       * If the focus is on the custom editor for the axis label string, the special character is inserted at the 
       * current caret position.
       */
      @Override void onInsertSpecialCharacter(String s)
      {
         if(contentEditor.isFocusOwner()) contentEditor.insertString(s, false);
      }

      @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && n.getNodeType() == FGNodeType.CBAR); }
      @Override ImageIcon getRepresentativeIcon() { return(null); }
      @Override String getRepresentativeTitle() { return(null); }

      @Override public void actionPerformed(ActionEvent e)
      {
         ColorBarNode colorBar = getColorBarNodeEdited();
         if(colorBar == null) return;
         Object src = e.getSource();
         
         boolean ok = true;
         if(src == contentEditor)
         {
            String text = FGraphicNode.toStyledText(
                  contentEditor.getCurrentContents(), colorBar.getFontStyle(), colorBar.getFillColor());
            ok = colorBar.setTitle(text);
            if(!ok) contentEditor.loadContent(colorBar.getAttributedTitle(false), colorBar.getFontFamily(), 0, 
                     colorBar.getFillColor(), colorBar.getFontStyle());
         }
         else if(src == cmapCombo)
            colorBar.setColorMap((ColorMap) cmapCombo.getSelectedItem());
         else if(src == startField)
         {
            ok = colorBar.setStart(startField.getValue().doubleValue());
            if(!ok) startField.setValue(colorBar.getStart());
         }
         else if(src == endField)
         {
            ok = colorBar.setEnd(endField.getValue().doubleValue());
            if(!ok) endField.setValue(colorBar.getEnd());
         }
         else if(src == cmapModeCombo)
            colorBar.setColorMapMode((ColorBarNode.CMapMode) cmapModeCombo.getSelectedItem());
         else if(src == offsetEditor)
         {
            ok = colorBar.setLabelOffset(offsetEditor.getMeasure());
            if(!ok) offsetEditor.setMeasure(colorBar.getLabelOffset());
         }
         else if(src == edgeLocCombo)
            colorBar.setEdge((ColorBarNode.Edge) edgeLocCombo.getSelectedItem());
         else if(src == barSizeEditor)
         {
            ok = colorBar.setBarSize(barSizeEditor.getMeasure());
            if(!ok) barSizeEditor.setMeasure(colorBar.getBarSize());
         }
         else if(src == gapEditor)
         {
            ok = colorBar.setGap(gapEditor.getMeasure());
            if(!ok) gapEditor.setMeasure(colorBar.getGap());
         }
         else if(src == spacerEditor)
         {
            ok = colorBar.setBarAxisSpacer(spacerEditor.getMeasure());
            if(!ok) spacerEditor.setMeasure(colorBar.getBarAxisSpacer());
         }
         else if(src == revChk)
            colorBar.setReversed(revChk.isSelected());
         else if(src == hideChk)
            colorBar.setHide(hideChk.isSelected());
         else if(src == lineHtField)
         {
            ok = colorBar.setLineHeight(lineHtField.getValue().doubleValue());
            if(!ok) lineHtField.setValue(colorBar.getLineHeight());
         }
         else if(src == scaleField)
         {
            ok = colorBar.setPowerScale(scaleField.getValue().intValue());
            if(!ok) scaleField.setValue(colorBar.getPowerScale());
         }

         if(!ok) Toolkit.getDefaultToolkit().beep();
      }

      @Override public void propertyChange(PropertyChangeEvent e)
      {
         ColorBarNode colorBar = getColorBarNodeEdited();
         if(colorBar == null) return;
         Object src = e.getSource();

         if(src == colorNaNPicker)
            colorBar.setColorNaN(colorNaNPicker.getCurrentColor());
      }

      /** Numeric text field edits the color bar's axis range start. */
      private final NumericTextField startField;

      /** Combo box selects which colormap is currently assigned to the color bar. */
      private JComboBox<ColorMap> cmapCombo = null;

      /** Numeric text field edits the color bar's axis range end. */
      private NumericTextField endField = null;
      
      /** Selects the data-to-color mapping mode for the color bar. */
      private JComboBox<ColorBarNode.CMapMode> cmapModeCombo = null;
      
      /** Compact widget uses a popup panel to edit the "NaN" color to which all ill-defined data are mapped. */
      private RGBColorPicker colorNaNPicker = null;

      /** 
       * Color bar label is edited within this custom widget, which allows user to change font style or text color 
       * anywhere in the content, add superscripts or subscripts, and underline portions of the text. Allows linefeeds.
       */
      private StyledTextEditor contentEditor = null;
      
      /** Edits measured offset from color bar's axis line to the axis label. */
      private MeasureEditor offsetEditor = null;

      /** Selects the graph data box edge beside which the color bar is rendered. */
      private JComboBox<ColorBarNode.Edge> edgeLocCombo = null;
      
      /** Edits measured width of gradient bar (along dimension perpendicular to the nearest graph data box edge. */
      private MeasureEditor barSizeEditor = null;

      /** Edits measured distance between the nearest graph data box edge and adjacent edge of the gradient bar. */
      private MeasureEditor gapEditor = null;

      /** Edits measured distance between axis line and adjacent edge of the gradient bar. */
      private MeasureEditor spacerEditor = null;
      
      /** When checked, the color bar's color map is reversed in direction. */
      private JCheckBox revChk = null;

      /** When checked, the color bar is entirely hidden. */
      private JCheckBox hideChk = null;

      /** Numeric text field for editing the text line height for a multi-line axis label. */
      private NumericTextField lineHtField = null;

      /** Numeric text field for editing the axis power scale factor (integer base-10 exponent). */
      private NumericTextField scaleField = null;

      /** Self-contained editor handles the node's text styles. */
      private TextStyleEditor textStyleEditor = null;
      
      /** Self-contained editor handles the node's draw style properties. */
      private DrawStyleEditor drawStyleEditor = null;
   }
   
   /**
    * Helper class implements a subordinate node properties editor that displays/edits the properties of a {@link 
    * TickSetNode}. It is an "embedded" editor that appears in the <b>FGNGraphAxisCard</b> when the user has selected
    * one of the tick sets defined on the currently loaded {@link FGNGraphAxis}. Tick set node properties exposed in the
    * editor include:
    * <ul>
    * <li>Numeric text fields specifying the start and end of the tick set's range, and the interval between ticks.</li>
    * <li>A check box to force the tick set range to match the range of its parent axis.</li>
    * <li>A series of 9 check boxes to specify where tick marks appear within a logarithmic decade -- when the parent
    * axis is logarithmic.</li>
    * <li>Custom text fields specifying the measured length of each tick, and the gap between the end of a tick and the
    * bounds of the corresponding tick label.</li>
    * <li>A multi-choice button to select the tick mark style (inward, outward, bisecting).</li>
    * <li>A combo box specifying the tick mark label format.</li>
    * <li>A text field for entering custom tick mark labels as comma-separated string.</li>
    * <li>A {@link TextStyleEditor} for editing the node's text-related styles, and a {@link DrawStyleEditor} for its
    * draw-related styles.</li>
    * </ul>
    * 
    * @author sruffner
    */
   private static class TickSetCard extends FGNEditor implements ActionListener, FocusListener, ItemListener
   {
      TickSetCard()
      {         
         // we layout everything in a transparent panel that, in turn, is placed in the "north" of the editor panel --
         // to ensure things don't get spread out vertically
         JPanel p = new JPanel(new SpringLayout());
         p.setOpaque(false);
         SpringLayout layout = (SpringLayout) p.getLayout();

         // tickset range and interval are in user units, which could be anything. We want to be able to display ranges 
         // across many orders of magnitude [1e3 .. 1e10] as well as more normal ranges like [5..10]. We place no limit 
         // on fractional digits but restrict to just 4 significant digits to keep things compact.
         
         startField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 4, -1);
         startField.setToolTipText("Enter start of tick set range");
         startField.addActionListener(this);
         p.add(startField);
         
         JLabel toLabel = new JLabel(" to ");
         p.add(toLabel);
         endField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 4, -1);
         endField.setToolTipText("Enter end of tick set range");
         endField.addActionListener(this);
         p.add(endField);
         
         JLabel byLabel = new JLabel(" by ");
         p.add(byLabel);
         intvField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 4, -1);
         intvField.setToolTipText("Enter interval between tick marks");
         intvField.addActionListener(this);
         p.add(intvField);
         
         trackingCheckBox = new JCheckBox("Track?");
         trackingCheckBox.setToolTipText("Check this box to force tick set range to track the parent axis range");
         trackingCheckBox.setContentAreaFilled(false);
         trackingCheckBox.addActionListener(this);
         p.add(trackingCheckBox);
         
         logTickCheckBoxes = new JCheckBox[9];
         for(int i=0; i<logTickCheckBoxes.length; i++)
         {
            logTickCheckBoxes[i] = new JCheckBox(Integer.toString(i+1));
            logTickCheckBoxes[i].setContentAreaFilled(false);
            logTickCheckBoxes[i].addActionListener(this);
            logTickCheckBoxes[i].setActionCommand(LOGTICKCMD);
            logTickCheckBoxes[i].setToolTipText(
                  "<html>Check any or none of these boxes to indicate where tick marks should be placed within<br/>" +
                  "a logarithmic decade <b>when the parent axis is logarithmic in base 10</b>. If no boxes are<br/>" +
                  "checked, then no tick marks will be drawn.</html>");
            p.add(logTickCheckBoxes[i]);
         }

         JLabel lenLabel = new JLabel("Len ");
         p.add(lenLabel);
         lengthEditor = new MeasureEditor(0, TickSetNode.TICKLENCONSTRAINTS);
         lengthEditor.setToolTipText("Enter tick mark length (0-1in)");
         lengthEditor.addActionListener(this);
         p.add(lengthEditor);
         
         JLabel gapLabel = new JLabel("Gap ");
         p.add(gapLabel);
         gapEditor = new MeasureEditor(0, TickSetNode.TICKLENCONSTRAINTS);
         gapEditor.setToolTipText("Enter size of gap between tick marks and labels (0-1in)");
         gapEditor.addActionListener(this);
         p.add(gapEditor);
         
         tickOriMB = new MultiButton<>();
         tickOriMB.addChoice(TickSetNode.Orientation.OUT, FCIcons.V4_TICKOUT_16, "outward");
         tickOriMB.addChoice(TickSetNode.Orientation.IN, FCIcons.V4_TICKIN_16, "inward");
         tickOriMB.addChoice(TickSetNode.Orientation.THRU, FCIcons.V4_TICKTHRU_16, "bisecting");
         tickOriMB.setToolTipText("Direction of tick marks with respect to axis");
         tickOriMB.addItemListener(this);
         p.add(tickOriMB);

         LabelFormat[] fmts = LabelFormat.values();
         formatCB = new JComboBox<>(fmts);
         formatCB.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                                                                    int index, boolean isSelected, boolean cellHasFocus)
            {
               super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
               String s = (value instanceof LabelFormat) ? ((LabelFormat)value).getGUILabel() : "";
               setText(s);
               return(this);
            }
         });
         formatCB.setToolTipText("Select tick mark label format");
         formatCB.addActionListener(this);
         p.add(formatCB);
         
         customLabelsField = new JTextField();
         customLabelsField.addActionListener(this);
         customLabelsField.addFocusListener(this);
         customLabelsField.setToolTipText(
               "<html>Enter custom tick mark labels here. Leave blank to use standard numeric labels.<br/>" +
               "<b>NOTE</b>: Custom labels are applied to tick marks in the order listed. If there are more<br/>" +
               "labels than tick marks, some will be unused; <i>if there are too few, the labels are reused<br/>" +
               "to ensure all tick marks are labeled.</html>");
         p.add(customLabelsField);
         
         textStyleEditor = new TextStyleEditor();
         p.add(textStyleEditor);
         drawStyleEditor = new DrawStyleEditor(true, true);
         p.add(drawStyleEditor);
         
         // left-right constraints. Assume row containing text style editor is the longest
         layout.putConstraint(SpringLayout.WEST, trackingCheckBox, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, startField, 0, SpringLayout.EAST, trackingCheckBox);
         layout.putConstraint(SpringLayout.WEST, toLabel, 0, SpringLayout.EAST, startField);
         layout.putConstraint(SpringLayout.WEST, endField, 0, SpringLayout.EAST, toLabel);
         layout.putConstraint(SpringLayout.WEST, byLabel, 0, SpringLayout.EAST, endField);
         layout.putConstraint(SpringLayout.WEST, intvField, 0, SpringLayout.EAST, byLabel);
         
         // the log-decade check boxes are arranged in a single row.
         layout.putConstraint(SpringLayout.WEST, logTickCheckBoxes[0], 0, SpringLayout.WEST, p);
         for(int i=1; i<logTickCheckBoxes.length; i++)
            layout.putConstraint(SpringLayout.WEST, logTickCheckBoxes[i], 0, SpringLayout.EAST, logTickCheckBoxes[i-1]);

         // assume this row is the longest -- this governs the width of the container panel
         layout.putConstraint(SpringLayout.WEST, lenLabel, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, lengthEditor, 0, SpringLayout.EAST, lenLabel);
         layout.putConstraint(SpringLayout.WEST, gapLabel, GAP, SpringLayout.EAST, lengthEditor);
         layout.putConstraint(SpringLayout.WEST, gapEditor, 0, SpringLayout.EAST, gapLabel);
         layout.putConstraint(SpringLayout.WEST, tickOriMB, GAP*2, SpringLayout.EAST, gapEditor);
         layout.putConstraint(SpringLayout.WEST, formatCB, GAP, SpringLayout.EAST, tickOriMB);
         

         layout.putConstraint(SpringLayout.WEST, customLabelsField, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.EAST, customLabelsField, 0, SpringLayout.EAST, p);
         
         layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.EAST, p , 0, SpringLayout.EAST, textStyleEditor);
         layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, p);
         
         // top-bottom constraints: A widget in each row is used to set the constraints with row above or below. The 
         // remaining widgets are vertically centered WRT that widget.
         layout.putConstraint(SpringLayout.NORTH, startField, 0, SpringLayout.NORTH, p);
         layout.putConstraint(SpringLayout.NORTH, logTickCheckBoxes[0], GAP, SpringLayout.SOUTH, startField);
         layout.putConstraint(SpringLayout.NORTH, tickOriMB, GAP, SpringLayout.SOUTH, logTickCheckBoxes[0]);
         layout.putConstraint(SpringLayout.NORTH, customLabelsField, GAP*2, SpringLayout.SOUTH, tickOriMB);
         
         layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*3, SpringLayout.SOUTH, customLabelsField);
         layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP*2, SpringLayout.SOUTH, textStyleEditor);
         layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, drawStyleEditor);
         
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, trackingCheckBox, 0, SpringLayout.VERTICAL_CENTER, 
               startField);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, toLabel, 0, SpringLayout.VERTICAL_CENTER, startField);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, endField, 0, SpringLayout.VERTICAL_CENTER, startField);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, byLabel, 0, SpringLayout.VERTICAL_CENTER, startField);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, intvField, 0, SpringLayout.VERTICAL_CENTER, startField);

         for(int i=1; i<logTickCheckBoxes.length; i++)
            layout.putConstraint(SpringLayout.VERTICAL_CENTER, logTickCheckBoxes[i], 0, SpringLayout.VERTICAL_CENTER, 
                  logTickCheckBoxes[0]);
         
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, lenLabel, 0, SpringLayout.VERTICAL_CENTER, tickOriMB);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, lengthEditor, 0, SpringLayout.VERTICAL_CENTER, tickOriMB);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, gapLabel, 0, SpringLayout.VERTICAL_CENTER, tickOriMB);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, gapEditor, 0, SpringLayout.VERTICAL_CENTER, tickOriMB);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, formatCB, 0, SpringLayout.VERTICAL_CENTER, tickOriMB);

         setLayout(new BorderLayout());
         add(p, BorderLayout.NORTH);
      }

      /**
       * Invoke this method to load a different tick mark set into the editor.
       * @param n The tick mark set to be edited. If null, no action is taken.
       */
      void loadTickSet(TickSetNode n)
      {
         if(n == null) return;
         ticks = n;
         reload(true);
      }
      
      @Override void reload(boolean initial)
      {
         if(ticks == null) return;
         
         // major tick set is auto-adjusted if parent axis is auto-scaled
         boolean automated = ticks.isAutoAdjusted();
         
         boolean isTracking = automated || ticks.isTrackingParentAxis();
         startField.setValue(ticks.getStart());
         startField.setEnabled(!isTracking);
         endField.setValue(ticks.getEnd());
         endField.setEnabled(!isTracking);
         intvField.setValue(ticks.getInterval());
         intvField.setEnabled(!automated);
         trackingCheckBox.setSelected(isTracking);
         trackingCheckBox.setEnabled(!automated);
         
         LogTickPattern ltp = ticks.getDecadeTicks();
         for(int i=0; i<logTickCheckBoxes.length; i++)
            logTickCheckBoxes[i].setSelected(ltp.isTickEnabledAt(i+1));
         
         lengthEditor.setMeasure(ticks.getTickLength());
         gapEditor.setMeasure(ticks.getTickGap());
         
         tickOriMB.setCurrentChoice(ticks.getTickOrientation());
         
         formatCB.removeActionListener(this);
         formatCB.setSelectedItem(ticks.getTickLabelFormat());
         formatCB.addActionListener(this);
         formatCB.setEnabled(!automated);
         
         customLabelsField.setText(ticks.getCustomTickLabelsAsCommaSeparatedList());
         
         textStyleEditor.loadGraphicNode(ticks);
         drawStyleEditor.loadGraphicNode(ticks);
      }
      
      @Override void onLowered()
      {
         textStyleEditor.cancelEditing();
         drawStyleEditor.cancelEditing();
      }

      @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && n.getNodeType() == FGNodeType.TICKS); }
      @Override ImageIcon getRepresentativeIcon() { return(null); }
      @Override String getRepresentativeTitle() { return(null); }

      @Override public void focusGained(FocusEvent e) 
      {
         if(e.getSource() == customLabelsField && !e.isTemporary()) customLabelsField.selectAll();
      }
      @Override public void focusLost(FocusEvent e)
      {
         if(e.getSource() == customLabelsField && !e.isTemporary()) customLabelsField.postActionEvent();
      }

      @Override public void actionPerformed(ActionEvent e)
      {
         if(ticks == null) return;
         
         Object src = e.getSource();
         String cmd = e.getActionCommand();

         boolean ok = true;
         if(src == startField)
         {
            ok = ticks.setStart(startField.getValue().doubleValue());
            if(!ok) startField.setValue(ticks.getStart());
         }
         else if(src == endField)
         {
            ok = ticks.setEnd(endField.getValue().doubleValue());
            if(!ok) endField.setValue(ticks.getEnd());
         }
         else if(src == intvField)
         {
            ok = ticks.setInterval(intvField.getValue().doubleValue());
            if(!ok) intvField.setValue(ticks.getInterval());
         }
         else if(src == trackingCheckBox)
         {
            boolean isTracking = trackingCheckBox.isSelected();
            ticks.setTrackingParentAxis(isTracking);
            startField.setEnabled(!isTracking);
            endField.setEnabled(!isTracking);
            if(isTracking)
            {
               startField.setValue(ticks.getStart());
               endField.setValue(ticks.getEnd());
            }
         }
         else if(LOGTICKCMD.equals(cmd))
         {
            int enaLogTicks = 0;
            for(int i=0; i<logTickCheckBoxes.length; i++) 
               if(logTickCheckBoxes[i].isSelected())
                  enaLogTicks |= (1<<(i+1));
            ticks.setDecadeTicks(new LogTickPattern(enaLogTicks));
         }
         else if(src == formatCB)
         {
            ticks.setTickLabelFormat((LabelFormat) formatCB.getSelectedItem());
         }
         else if(src == lengthEditor)
         {
            ok = ticks.setTickLength(lengthEditor.getMeasure());
            if(!ok) lengthEditor.setMeasure(ticks.getTickLength());
         }
         else if(src == gapEditor)
         {
            ok = ticks.setTickGap(gapEditor.getMeasure());
            if(!ok) gapEditor.setMeasure(ticks.getTickGap());
         }
         else if(src == customLabelsField)
         {
            String s = customLabelsField.getText().trim();
            ticks.setCustomTickLabelsFromCommaSeparatedList(s);
            String adj = ticks.getCustomTickLabelsAsCommaSeparatedList();
            if(!adj.equals(s)) customLabelsField.setText(adj);
         }
         
         if(!ok) Toolkit.getDefaultToolkit().beep();
      }
      
      @Override public void itemStateChanged(ItemEvent e)
      {
         if(ticks == null || e.getSource() != tickOriMB) return;
         ticks.setTickOrientation(tickOriMB.getCurrentChoice());
      }
      
      private TickSetNode ticks = null;
      
      /** Numeric text field edits the tick set range start. */
      private final NumericTextField startField;

      /** Numeric text field edits the tick set range end. */
      private NumericTextField endField = null;

      /** Numeric text field edits the tick mark interval. */
      private NumericTextField intvField = null;

      /** If this box is checked, then tick set range tracks the parent axis */
      private JCheckBox trackingCheckBox = null;

      /** Array of 9 check boxes to choose where tick marks appear within each logarithmic decade (log axis only). */
      private JCheckBox[] logTickCheckBoxes = null;

      /** Shared action command from the check boxes which choose where ticks appear in a log decade. */
      private final static String LOGTICKCMD = "LogTick";

      /** Customized component for editing the tick mark length. */
      private MeasureEditor lengthEditor = null;

      /** Customized component for editing the size of the gap between tick marks and corresponding labels. */
      private MeasureEditor gapEditor = null;
      
      /** Edits the tick mark orientation (in, out, or thru). */
      private MultiButton<TickSetNode.Orientation> tickOriMB = null; 

      /** Button that selects the tick mark label format. */
      private JComboBox<LabelFormat> formatCB = null;

      /** 
       * Displays/edits custom tick mark labels as a comma-separated list of label tokens. If empty, then standard
       * numeric tick labels are generated.
       */
      private JTextField customLabelsField = null;
      
      /** Self-contained editor handles the node's text styles. */
      private TextStyleEditor textStyleEditor = null;
      
      /** Self-contained editor handles the node's draw style properties. */
      private DrawStyleEditor drawStyleEditor = null;
   }
}
