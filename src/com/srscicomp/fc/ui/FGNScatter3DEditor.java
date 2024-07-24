package com.srscicomp.fc.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import com.srscicomp.common.ui.BkgFillPicker;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.common.ui.RGBColorPicker;
import com.srscicomp.common.ui.TabStrip;
import com.srscicomp.common.ui.TabStripModel;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.Graph3DNode.Side;
import com.srscicomp.fc.fig.Scatter3DNode;
import com.srscicomp.fc.fig.SymbolNode;
import com.srscicomp.fc.uibase.StyledTextEditor;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * The extension of {@link FGNEditor} that displays/edits all properties of a 3D scatter plot node, as defined by
 * {@link Scatter3DNode}.
 * 
 * <p>The scatter plot node's title is entered in a plain text field; however, the title is never rendered in the 
 * figure, except to label the node's legend entry in the parent graph's automated legend. Next to the text field is a
 * check box that indicates whether or not the node should be included in that legend. On the next row is an embedded 
 * {@link FGNPlottableDSCard}, which exposes the ID of the data set referenced by the node, and provides facilities for 
 * viewing and/or editing that raw data set. Below this is a widget for specifying the scatter plot's display mode, then
 * a 3-page tab panel containing widgets that govern the appearance of the plot. On the "Line/Stems/Bars" tab is a pair
 * of mutually exclusive radio buttons to select whether a single trace line is drawn connecting the data points or a 
 * stem line is drawn from each point to the stem base plane; a numeric text field exposing the base plane value, Zo, 
 * for stem lines, or for the 3D bars in either of the bar plot display modes; a numeric text field exposing the bar
 * size; and an embedded {@link DrawStyleEditor} exposing the 3D scatter plot node's draw styles -- since these 
 * determine the appearance of the single trace line or stem lines, or the 3D bars. The "Symbols" tab contains an 
 * embedded {@link FGNSymbolCard} exposing the properties of the 3D scatter plot's component {@link SymbolNode}, along 
 * with a custom widget exposing the background fill applied to the symbols (to support gradient fills). Finally, the 
 * "Projected Dots" tab includes widgets for specifying the dot size and fill color for the projection of the scatter 
 * plot onto each of the parent graph's backplanes. Dot size is limited to 0..10 points, where 0 indicates that the 
 * projection is not rendered. Of course, if the backplane is not rendered, nor will the corresponding projection. Also,
 * note that the last two tabs are disabled when the 3D scatter plot is configured in either of the bar plot display
 * modes, since neither symbols nor dot projections are rendered in those modes.</p>
 * 
 * @author sruffner
 */
class FGNScatter3DEditor extends FGNEditor implements ActionListener, PropertyChangeListener, TabStripModel
{
   /** Construct the scatter plot node properties editor. */
   FGNScatter3DEditor()
   {
      showInLegendChk = new JCheckBox();
      showInLegendChk.setToolTipText(
            "Check this box to include an entry in the legend for this 3D scatter plot");
      showInLegendChk.setContentAreaFilled(false);
      showInLegendChk.addActionListener(this);
      add(showInLegendChk);
      
      titleEditor = new StyledTextEditor(1);
      titleEditor.setToolTipText("Enter title here (it will be used as the label for the node's legend entry)");
      titleEditor.addActionListener(this);
      add(titleEditor);
            
      dsEditor = new FGNPlottableDSCard();
      add(dsEditor);

      JLabel modeLabel = new JLabel("Display Mode ");
      add(modeLabel);
      modeCombo = new JComboBox<>(Scatter3DNode.DisplayMode.values());
      modeCombo.addActionListener(this);
      add(modeCombo);
      
      TabStrip tStrip = new TabStrip(this);
      add(tStrip);
      
      tabContent = new JPanel(new CardLayout());
      tabContent.setOpaque(false);
      tabContent.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 2, 2, 2, tStrip.getSelectionColor()), 
            BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP)
      ));
      add(tabContent);
      
      // the "Line/Stems/Bars" tab panel: radio button group to select trace line vs stem lines; base plane Zo field; 
      // the bar size field; and a draw styles editor exposing the draw styles for the scatter plot node itself, as 
      // these affect rendering of the trace line, stem lines, and 3D bars.
      JPanel stemsPanel = new JPanel(new BorderLayout());
      stemsPanel.setOpaque(false);
      
      traceRB = new JRadioButton("Traced");
      traceRB.addActionListener(this);
      traceRB.setToolTipText("Connect data points with a single trace line");
      stemRB = new JRadioButton("Stems", true);
      stemRB.addActionListener(this);
      stemRB.setToolTipText("Draw a stem line from each data poin to the stem baseplane, Z=Zo");
      
      ButtonGroup bg = new ButtonGroup();
      bg.add(traceRB);
      bg.add(stemRB);
      
      JLabel baseLabel = new JLabel("Base Z = ");
      baseZField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      baseZField.setColumns(5);
      baseZField.setToolTipText(
            "<html>Enter Z-coordinate for the stem/bar base plane, Z=Zo. <i>If Zo lies outside the graph's<br/>"
            + "current Z axis range, stems and bars are drawn to the XY backplane.</i></html>");
      baseZField.addActionListener(this);
      
      JLabel barSzLabel = new JLabel("Bar Size = ");
      barSizeField = new NumericTextField(Scatter3DNode.MINBARSZ, Scatter3DNode.MAXBARSZ);
      barSizeField.setColumns(2);
      barSizeField.setToolTipText(
            "<html>Enter the size of bar cross-section parallel to XY plane, as a percentage of the 3D X-axis<br/>"
            + "extent. Range-restricted to [1..20].</html>");
      barSizeField.addActionListener(this);
      
      drawStyleEditor = new DrawStyleEditor(false, false);
      add(drawStyleEditor);
      
      JPanel p = new JPanel();
      p.setOpaque(false);
      p.add(traceRB);
      p.add(stemRB);
      p.add(baseLabel);
      p.add(baseZField);
      p.add(barSzLabel);
      p.add(barSizeField);
      p.add(drawStyleEditor);
      SpringLayout layout = new SpringLayout();
      p.setLayout(layout);
      layout.putConstraint(SpringLayout.WEST, stemRB, 0, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.WEST, traceRB, GAP, SpringLayout.EAST, stemRB);
      layout.putConstraint(SpringLayout.WEST, baseLabel, GAP*2, SpringLayout.EAST, traceRB);
      layout.putConstraint(SpringLayout.WEST, baseZField, 0, SpringLayout.EAST, baseLabel);
      layout.putConstraint(SpringLayout.WEST, barSzLabel, GAP*2, SpringLayout.EAST, baseZField);
      layout.putConstraint(SpringLayout.WEST, barSizeField, 0, SpringLayout.EAST, barSzLabel);
      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.NORTH, baseZField, 0, SpringLayout.NORTH, p);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, baseZField);
      layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, drawStyleEditor);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, baseLabel, 0, SpringLayout.VERTICAL_CENTER, baseZField);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, stemRB, 0, SpringLayout.VERTICAL_CENTER, baseZField);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, traceRB, 0, SpringLayout.VERTICAL_CENTER, baseZField);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, barSzLabel, 0, SpringLayout.VERTICAL_CENTER, baseZField);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, barSizeField, 0, SpringLayout.VERTICAL_CENTER, baseZField);
      stemsPanel.add(p, BorderLayout.NORTH);

      // the "Symbols" tab panel contains an embedded editor for the node's component symbol node, plus the background
      // fill picker widget.
      symbolEditor = new FGNSymbolCard(true, true, true);
      bkgFillPicker = new BkgFillPicker(50, 50);
      bkgFillPicker.setToolTipText("Background fill");
      bkgFillPicker.addPropertyChangeListener(BkgFillPicker.BKGFILL_PROPERTY, this);
      
      JPanel symbolPanel = new JPanel(new BorderLayout());
      symbolPanel.setOpaque(false);
      p = new JPanel();
      p.setOpaque(false);
      p.add(symbolEditor);
      p.add(bkgFillPicker);
      layout = new SpringLayout();
      p.setLayout(layout);
      layout.putConstraint(SpringLayout.WEST, symbolEditor, 0, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.WEST, bkgFillPicker, GAP, SpringLayout.EAST, symbolEditor);
      layout.putConstraint(SpringLayout.EAST, p, 0, SpringLayout.EAST, bkgFillPicker);
      layout.putConstraint(SpringLayout.NORTH, symbolEditor, 0, SpringLayout.NORTH, p);
      layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, symbolEditor);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, bkgFillPicker, 0, SpringLayout.VERTICAL_CENTER, symbolEditor);
      symbolPanel.add(p, BorderLayout.NORTH);

      // the "Projected Dots" tab panel includes widgets to set the dot color and size for the projections of the
      // scatter plot onto the parent 3D graph's three backplanes (XY, XZ, YZ)
      p = new JPanel();
      p.setOpaque(false);
      JLabel xyLabel = new JLabel("XY Plane: ");
      p.add(xyLabel);
      xyDotColorPicker = new RGBColorPicker();
      xyDotColorPicker.setToolTipText("Dot color for projection onto XY backplane");
      xyDotColorPicker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
      p.add(xyDotColorPicker);
      xyDotSizeField = new NumericTextField(0, 10);
      xyDotSizeField.setColumns(3);
      xyDotSizeField.setToolTipText("Dot size (pts, [0..10]) for projection onto XY backplane");
      xyDotSizeField.addActionListener(this);
      p.add(xyDotSizeField);

      JLabel xzLabel = new JLabel("XZ Plane: ");
      p.add(xzLabel);
      xzDotColorPicker = new RGBColorPicker();
      xzDotColorPicker.setToolTipText("Dot color for projection onto XZ backplane");
      xzDotColorPicker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
      p.add(xzDotColorPicker);
      xzDotSizeField = new NumericTextField(0, 10);
      xzDotSizeField.setColumns(3);
      xzDotSizeField.setToolTipText("Dot size (pts, [0..10]) for projection onto XZ backplane");
      xzDotSizeField.addActionListener(this);
      p.add(xzDotSizeField);

      JLabel yzLabel = new JLabel("YZ Plane: ");
      p.add(yzLabel);
      yzDotColorPicker = new RGBColorPicker();
      yzDotColorPicker.setToolTipText("Dot color for projection onto YZ backplane");
      yzDotColorPicker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
      p.add(yzDotColorPicker);
      yzDotSizeField = new NumericTextField(0, 10);
      yzDotSizeField.setColumns(3);
      yzDotSizeField.setToolTipText("Dot size (pts, [0..10]) for projection onto YZ backplane");
      yzDotSizeField.addActionListener(this);
      p.add(yzDotSizeField);

      layout = new SpringLayout();
      p.setLayout(layout);
      layout.putConstraint(SpringLayout.WEST, xyLabel, 0, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.WEST, xyDotColorPicker, GAP, SpringLayout.EAST, xyLabel);
      layout.putConstraint(SpringLayout.WEST, xyDotSizeField, GAP, SpringLayout.EAST, xyDotColorPicker);
      layout.putConstraint(SpringLayout.WEST, xzLabel, GAP*4, SpringLayout.EAST, xyDotSizeField);
      layout.putConstraint(SpringLayout.WEST, xzDotColorPicker, GAP, SpringLayout.EAST, xzLabel);
      layout.putConstraint(SpringLayout.WEST, xzDotSizeField, GAP, SpringLayout.EAST, xzDotColorPicker);
      layout.putConstraint(SpringLayout.WEST, yzLabel, 0, SpringLayout.WEST, xyLabel);
      layout.putConstraint(SpringLayout.WEST, yzDotColorPicker, 0, SpringLayout.WEST, xyDotColorPicker);
      layout.putConstraint(SpringLayout.WEST, yzDotSizeField, 0, SpringLayout.WEST, xyDotSizeField);
      
      layout.putConstraint(SpringLayout.NORTH, xyDotSizeField, 0, SpringLayout.NORTH, p);
      layout.putConstraint(SpringLayout.NORTH, yzDotSizeField, GAP, SpringLayout.SOUTH, xyDotSizeField);
      layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, yzDotSizeField);
      String vc = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vc, xyLabel, 0, vc, xyDotSizeField);
      layout.putConstraint(vc, xyDotColorPicker, 0, vc, xyDotSizeField);
      layout.putConstraint(vc, xzLabel, 0, vc, xyDotSizeField);
      layout.putConstraint(vc, xzDotColorPicker, 0, vc, xyDotSizeField);
      layout.putConstraint(vc, xzDotSizeField, 0, vc, xyDotSizeField);
      layout.putConstraint(vc, yzLabel, 0, vc, yzDotSizeField);
      layout.putConstraint(vc, yzDotColorPicker, 0, vc, yzDotSizeField);

      JPanel dotsPanel = new JPanel(new BorderLayout());
      dotsPanel.setOpaque(false);
      dotsPanel.add(p, BorderLayout.NORTH);
      
      tabContent.add(stemsPanel, TAB_STEMS);
      tabContent.add(symbolPanel, TAB_SYMBOLS);
      tabContent.add(dotsPanel, TAB_PRJDOTS);
      
      // now layout the widgets, tab strip, and tab content panel in rows.
      layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. The editor panel's width is determined by the width of the tab content panel
      layout.putConstraint(SpringLayout.WEST, showInLegendChk, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, titleEditor, GAP, SpringLayout.EAST, showInLegendChk);
      layout.putConstraint(SpringLayout.EAST, titleEditor, 0, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, dsEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, dsEditor, 0, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, modeLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, modeCombo, 0, SpringLayout.EAST, modeLabel);

      layout.putConstraint(SpringLayout.WEST, tStrip, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, tStrip, 0, SpringLayout.EAST, tabContent);

      layout.putConstraint(SpringLayout.WEST, tabContent, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, tabContent);
      
      // top-bottom constraints: 5 rows. One widget per row is used to set the constraints with row above or below. The 
      // remaining widgets in a row are vertically centered WRT that widget. 
      layout.putConstraint(SpringLayout.NORTH, titleEditor, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, dsEditor, GAP*2, SpringLayout.SOUTH, titleEditor);
      layout.putConstraint(SpringLayout.NORTH, modeCombo, GAP, SpringLayout.SOUTH, dsEditor);
      layout.putConstraint(SpringLayout.NORTH, tStrip, GAP*2, SpringLayout.SOUTH, modeCombo);
      layout.putConstraint(SpringLayout.NORTH, tabContent, 0, SpringLayout.SOUTH, tStrip);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, tabContent);

      layout.putConstraint(vc, showInLegendChk, 0, vc, titleEditor);
      layout.putConstraint(vc, modeLabel, 0, vc, modeCombo);
   }

   @Override public void reload(boolean initial)
   {
      Scatter3DNode spn = getScatter3DNode();
      if(spn == null) return;
      
      showInLegendChk.setSelected(spn.getShowInLegend());
      titleEditor.loadContent(spn.getAttributedTitle(false), spn.getFontFamily(), 0, spn.getFillColor(), 
            spn.getFontStyle());
      dsEditor.reload(spn);
      modeCombo.setSelectedItem(spn.getMode());
      
      traceRB.setSelected(!spn.getStemmed());
      stemRB.setSelected(spn.getStemmed());
      baseZField.setValue(spn.getZBase());
      barSizeField.setValue(spn.getBarSize());
      drawStyleEditor.loadGraphicNode(spn);
      if(initial)
      {
         symbolEditor.reload(spn.getSymbolNode());
         setSelectedTab(0);
         
         // update visibility of "Symbols" and "Projection Dots" tabs depending on whether node is configured in one
         // of the bar plot display modes or not
         fireStateChanged();
      }
      else symbolEditor.reload(null);
      
      bkgFillPicker.setCurrentFill(spn.getBackgroundFill(), false);
      
      xyDotColorPicker.setCurrentColor(spn.getProjectionDotColor(Side.XY), false);
      xzDotColorPicker.setCurrentColor(spn.getProjectionDotColor(Side.XZ), false);
      yzDotColorPicker.setCurrentColor(spn.getProjectionDotColor(Side.YZ), false);
      xyDotSizeField.setValue(spn.getProjectionDotSize(Side.XY));
      xzDotSizeField.setValue(spn.getProjectionDotSize(Side.XZ));
      yzDotSizeField.setValue(spn.getProjectionDotSize(Side.YZ));
      
      // some properties on Line/Stems/Bars tab are irrelevant in certain display modes.
      traceRB.setEnabled(!spn.isBarPlotDisplayMode());
      stemRB.setEnabled(!spn.isBarPlotDisplayMode());
      barSizeField.setEnabled(spn.isBarPlotDisplayMode());
   }

   /** Close any modeless pop-up windows associated with any widgets on this node editor. */
   @Override void onLowered() 
   { 
      drawStyleEditor.cancelEditing(); 
      symbolEditor.cancelEditing();
      bkgFillPicker.cancelPopup();
      xyDotColorPicker.cancelPopup();
      xzDotColorPicker.cancelPopup();
      yzDotColorPicker.cancelPopup();
   }

   /**
    * If the focus is on the title field or the symbol character field in the marker symbol editor, then the special 
    * character is inserted at the current caret position in that text field. The user will still have to hit the 
    * "Enter" key or shift focus away from the field to submit the change.
    */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(titleEditor.isFocusOwner()) titleEditor.insertString(s, false);
      else if(getSelectedTab() == 1) symbolEditor.insertSymbolCharacter(s);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.SCATTER3D == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_SCATTER3D_32); }
   @Override String getRepresentativeTitle() { return("3D Scatter/Line Plot Properties"); }

   @Override public void propertyChange(PropertyChangeEvent e)
   {
      Scatter3DNode spn = getScatter3DNode();
      Object src = e.getSource();
      if(spn == null) return;
      if(src == bkgFillPicker && BkgFillPicker.BKGFILL_PROPERTY.equals(e.getPropertyName()))
         spn.setBackgroundFill(bkgFillPicker.getCurrentFill());
      else if(RGBColorPicker.COLOR_PROPERTY.equals(e.getPropertyName()))
      {
         if(src==xyDotColorPicker) spn.setProjectionDotColor(Side.XY, xyDotColorPicker.getCurrentColor());
         else if(src==xzDotColorPicker) spn.setProjectionDotColor(Side.XZ, xzDotColorPicker.getCurrentColor());
         else spn.setProjectionDotColor(Side.YZ, yzDotColorPicker.getCurrentColor());
      }
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      Scatter3DNode spn = getScatter3DNode();
      if(spn == null) return;
      Object src = e.getSource();

      boolean ok = true; 
      if(src == titleEditor)
      {
         String text = FGraphicNode.toStyledText(
               titleEditor.getCurrentContents(), spn.getFontStyle(), spn.getFillColor());
         ok = spn.setTitle(text);
         if(!ok) titleEditor.loadContent(spn.getAttributedTitle(false), spn.getFontFamily(), 0, 
               spn.getFillColor(), spn.getFontStyle());
      }
      else if(src == showInLegendChk)
         spn.setShowInLegend(showInLegendChk.isSelected());
      else if(src == traceRB || src == stemRB)
         spn.setStemmed(stemRB.isSelected());
      else if(src == modeCombo)
      {
         boolean wasBarPlotMode = spn.isBarPlotDisplayMode();
         ok = spn.setMode((Scatter3DNode.DisplayMode) modeCombo.getSelectedItem());
         if(!ok) modeCombo.setSelectedItem(spn.getMode());
         else
         {
            // hide/show the "Symbols" and "Projection Dots" tabs upon switching to/from a bar plot display mode. These
            // two tabs are not applicable in the bar plot modes.
            boolean isBarPlotMode = spn.isBarPlotDisplayMode();
            if(wasBarPlotMode != isBarPlotMode)
            {
               setSelectedTab(0);
               fireStateChanged();
            }
         }
      }
      else if(src == baseZField)
      {
         ok = spn.setZBase(baseZField.getValue().doubleValue());
         if(!ok) baseZField.setValue(spn.getZBase());
      }
      else if(src == barSizeField)
      {
         ok = spn.setBarSize(barSizeField.getValue().intValue());
         if(!ok) barSizeField.setValue(spn.getBarSize());
      }
      else if(src == xyDotSizeField)
      {
         ok = spn.setProjectionDotSize(Side.XY, xyDotSizeField.getValue().intValue());
         if(!ok) xyDotSizeField.setValue(spn.getProjectionDotSize(Side.XY));
      }
      else if(src == xzDotSizeField)
      {
         ok = spn.setProjectionDotSize(Side.XZ, xzDotSizeField.getValue().intValue());
         if(!ok) xzDotSizeField.setValue(spn.getProjectionDotSize(Side.XZ));
      }
      else if(src == yzDotSizeField)
      {
         ok = spn.setProjectionDotSize(Side.YZ, yzDotSizeField.getValue().intValue());
         if(!ok) yzDotSizeField.setValue(spn.getProjectionDotSize(Side.YZ));
      }
      
      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** 
    * When a 3D scatter plot node is put in either of the bar plot display modes, then the "Symbols" and "Projection
    * Dots" tabs are effectively hidden because none of the properties on those tab panels are applicable in the bar
    * plot modes.
    */
   @Override public int getNumTabs()
   { 
      Scatter3DNode sp3 = getScatter3DNode();
      return((sp3==null || !sp3.isBarPlotDisplayMode()) ? 3 : 1); 
   }
   @Override public int getSelectedTab() { return(iSelectedTab); }
   @Override public void setSelectedTab(int tabPos)
   {
      if(tabPos < 0 || tabPos >= getNumTabs() || tabPos == iSelectedTab) return;

      iSelectedTab = tabPos;
      fireStateChanged();
      
      CardLayout cl = (CardLayout) tabContent.getLayout();
      cl.show(tabContent, iSelectedTab==0 ? TAB_STEMS : (iSelectedTab==1 ? TAB_SYMBOLS : TAB_PRJDOTS));
   }

   @Override public void addChangeListener(ChangeListener l)
   {
      if(l != null) tabListeners.add(ChangeListener.class, l);
   }
   @Override public void removeChangeListener(ChangeListener l)
   {
      if(l != null) tabListeners.remove(ChangeListener.class, l);
   }

   @Override public String getTabLabel(int tabPos) 
   { 
      return(tabPos == 0 ? TAB_STEMS : (tabPos == 1 ? TAB_SYMBOLS : TAB_PRJDOTS)); 
   }
   @Override public String getTabToolTip(int tabPos) { return(null); }
   @Override public Icon getTabIcon(int tabPos) { return(null); }
   @Override public boolean isTabClosable(int tabPos) { return(false); }
   @Override public String getCloseTabToolTip(int tabPos) { return(null); }
   @Override public void closeTab(int tabPos) {}
   @Override public boolean supportsTabRepositioning() { return(false); }
   @Override public boolean moveTab(int fromPos, int toPos) { return(false); }

   /** The currently selected tab index. */
   private int iSelectedTab = -1;
   
   /** The tab content panel -- uses a {@link CardLayout} manager to display the correct tab content. */
   private JPanel tabContent = null;
   
   /** List of all change listeners registered with the tab strip model. */
   private final EventListenerList tabListeners = new EventListenerList();

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
   
   /** Convenience method casts the edited node to an instance of {@link Scatter3DNode}. */
   private Scatter3DNode getScatter3DNode() { return((Scatter3DNode) getEditedNode()); }
   
   /** When checked, an entry for the 3D scatter plot is included in parent graph's automated legend. */
   private final JCheckBox showInLegendChk;

   /** The custom widget within which the 3D scatter plot's title (which may contain attributed text) is edited.  */
   private StyledTextEditor titleEditor = null;

   /** Self-contained editor handles editing/loading of the data set assigned to the 3D scatter plot node. */
   private FGNPlottableDSCard dsEditor = null;
   
   /** Combo box for selecting the 3D scatter plot's display mode. */
   private JComboBox<Scatter3DNode.DisplayMode> modeCombo = null;

   /** If selected, scatter plot's data points are connected by a single trace line. */
   private JRadioButton traceRB = null;
   /** If selected, a stem line is drawn from each data point in scatter plot to the stem base plane. */
   private JRadioButton stemRB = null;
   
   /** Numeric field sets the value Zo for the stem base plane, Z=Zo. */
   private NumericTextField baseZField = null;
   
   /** Numeric field sets the bar size for the node's two bar plot-like display modes. */
   private NumericTextField barSizeField = null;
   
   /** Self-contained editor handles the 3D scatter plot node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** Compact widget uses a pop-up panel to edit the background fill descriptor for the scatter plot symbols. */
   private BkgFillPicker bkgFillPicker = null;

   /** 
    * The editor for the 3D scatter plot node's component symbol node, which encapsulates marker symbol properties
    * other than the background fill descriptor. 
    */
   private FGNSymbolCard symbolEditor = null;
   
   /** Widget displays/selects dot color for the scatter plot projection onto the XY backplane. */
   private RGBColorPicker xyDotColorPicker = null;
   /** Numeric field sets the dot size for the scatter plot projection onto the XY backplane. */
   private NumericTextField xyDotSizeField = null;
   /** Widget displays/selects dot color for the scatter plot projection onto the XZ backplane. */
   private RGBColorPicker xzDotColorPicker = null;
   /** Numeric field sets the dot size for the scatter plot projection onto the XZ backplane. */
   private NumericTextField xzDotSizeField = null;
   /** Widget displays/selects dot color for the scatter plot projection onto the YZ backplane. */
   private RGBColorPicker yzDotColorPicker = null;
   /** Numeric field sets the dot size for the scatter plot projection onto the YZ backplane. */
   private NumericTextField yzDotSizeField = null;
   
   /** The title of the tab in which the 3D scatter plot's line/stem/bar styling properties are shown. */
   private final static String TAB_STEMS = "Line/Stems/Bars";
   /** The title of the tab in which are shown properties govering the appearance of the scatter plot symbols. */
   private final static String TAB_SYMBOLS = "Symbols";
   /** The title of the tab in which are shown dot size and color for the scatter plot projections onto backplanes. */
   private final static String TAB_PRJDOTS = "Projection Dots";
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
