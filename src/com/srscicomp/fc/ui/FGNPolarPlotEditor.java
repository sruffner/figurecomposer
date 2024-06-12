package com.srscicomp.fc.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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

import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.ui.MultiButton;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.common.ui.RGBColorPicker;
import com.srscicomp.common.ui.TabStrip;
import com.srscicomp.common.ui.TabStripModel;
import com.srscicomp.fc.fig.AxisNode;
import com.srscicomp.fc.fig.FGNGraph;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.fig.PolarAxisNode;
import com.srscicomp.fc.fig.PolarPlotNode;
import com.srscicomp.fc.fig.TickSetNode.LabelFormat;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;
import com.srscicomp.fc.uibase.StyledTextEditor;

/**
 * <b>FGNPolarPlotEditor</b> displays/edits all properties of a polar plot as encapsulated by {@link PolarPlotNode} --
 * including the properties of its various required "component nodes": the theta and radial axes, a color bar, and an
 * automated legend.
 * <p>This is a complex composite editor providing a tabbed interface to navigate over all properties of the polar plot
 * node itself and any of its component nodes. The editor panel is displayed whenever the polar plot itself OR one of
 * its components gets the "display focus". When displayed, the relevant tab is automatically selected so that the 
 * properties of the selected component are on display.</p>
 * 
 * <p>The color bar for a polar plot is exactly like that for the standard 2D graph. It can include zero or more tick
 * mark sets as child nodes. Therefore, the subordinate editor that handles display/editing of the color bar also 
 * handles any tick sets. The properties of the color bar node itself are displayed in one tab, while each
 * defined tick set is displayed in a separate tab. Closing a tick set tab is the same as deleting that tick set, and a
 * push button is included so that the user can append new tick sets to the color bar. Note that, unlike the X,Y axes
 * of a standard 2D graph, the theta and radial axes of a polar plot do NOT admit tick mark sets.</p>
 * 
 * <p>Implementation details. We use {@link TabStrip} and a companion container panel utilizing a card layout manager to
 * implement the tabbed-pane interface of <b>FGNPolarPlotEditor</b> itself, and the subordinate color bar/axis editor.
 * Most subordinate editors are implemeted by inner classes. However, since the legend and color bar components appear
 * in more than one type of graph, the corresponding editors are implemented in stand-alone classes in this package --
 * see {@link FGNLegendCard} and {@link FGNGraphAxisCard}.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNPolarPlotEditor extends FGNEditor implements TabStripModel
{
   /** Construct the polar plot node properties editor. */
   FGNPolarPlotEditor()
   {
      super();
      
      tabStrip = new TabStrip(this);
      
      tabPanel = new JPanel(new CardLayout());
      tabPanel.setOpaque(false);
      tabPanel.add(polarPlotCard, CARD_MAIN);
      tabPanel.add(polarAxisCard, CARD_PAXIS);
      tabPanel.add(colorBarCard, CARD_CBAR);
      tabPanel.add(legendCard, CARD_LEGEND);
 
      setLayout(new BorderLayout());
      add(tabStrip, BorderLayout.NORTH);
      add(tabPanel, BorderLayout.CENTER);
   }
   
   @Override public void reload(boolean initial)
   {
      // get the polar plot node being edited. Since this editor may be raised to edit one of the polar plot's 
      // component nodes, or one of the tick sets in its color bar, we have to be careful here.
      polarPlot = null;
      FGraphicNode fgn = getEditedNode();
      FGNodeType nt = fgn.getNodeType();
      if(nt == FGNodeType.PGRAPH) polarPlot = (PolarPlotNode) fgn;
      else
      {
         FGraphicNode parent = fgn.getParent();
         if(parent != null && parent.getNodeType() == FGNodeType.PGRAPH) polarPlot = (PolarPlotNode) parent;
         else if(parent != null)
         {
            parent = parent.getParent();
            if(parent != null && parent.getNodeType() == FGNodeType.PGRAPH) polarPlot = (PolarPlotNode) parent;
         }
      }
      assert(polarPlot != null);
      
      // reload the individual editors. We have to make sure the polar axis card loads the correct component node
      polarPlotCard.reload(initial);
      if(initial)
      {
         polarAxisCard.setAxisEdited((nt != FGNodeType.PAXIS) || (fgn==polarPlot.getThetaAxis()));
         legendCard.loadLegend(polarPlot.getLegend());
         colorBarCard.loadAxis(polarPlot.getColorBar());
      }
      else
      {
         polarAxisCard.reload(false);
         legendCard.reload(false);
         colorBarCard.reload(false);
      }
      
      // switch to the tab corresponding to the edited node -- unless we're re-loading
      if(iSelectedTab < 0 || initial)
      {
         int iTab = 0;
         if(nt == FGNodeType.PAXIS)
         {
            if(fgn == polarPlot.getThetaAxis()) iTab = 1;
            else iTab = 2;
         }
         else if(nt == FGNodeType.CBAR || nt == FGNodeType.TICKS) iTab = 3;
         else if(nt == FGNodeType.LEGEND) iTab = 4;

         setSelectedTab(iTab);
      }
   }

   @Override public void onRaised() 
   {
      FGNEditor editor = getEditorForTab(iSelectedTab);
      if(editor != null) editor.onRaised();
   }

   @Override void onLowered()
   {
      FGNEditor editor = getEditorForTab(iSelectedTab);
      if(editor != null) editor.onLowered();
   }

   @Override void onInsertSpecialCharacter(String s)
   {
      FGNEditor editor = getEditorForTab(iSelectedTab);
      if(editor != null) editor.onInsertSpecialCharacter(s);
   }

   @Override boolean isEditorForNode(FGraphicNode n)
   { 
      if(n == null) return(false);
      FGNodeType nt = n.getNodeType();
      if(nt == FGNodeType.PGRAPH) return(true);
      if(nt==FGNodeType.PAXIS || nt==FGNodeType.CBAR || nt==FGNodeType.LEGEND)
         return(n.getParent() instanceof PolarPlotNode);
      if(nt==FGNodeType.TICKS)
      {
         FGraphicNode grandParent = n.getParent();
         if(grandParent != null) grandParent = grandParent.getParent();
         return(grandParent instanceof PolarPlotNode);
      }
      return(false);     
   }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_POLARG_32); }
   @Override String getRepresentativeTitle() { return("Polar Plot Properties"); }

   
   @Override public int getNumTabs() { return(TABLABELS.length); }
   @Override public int getSelectedTab() { return(iSelectedTab); }

   @Override public void setSelectedTab(int tabPos)
   {
      if(tabPos < 0 || tabPos >= getNumTabs() || tabPos == iSelectedTab) return;
      
      iSelectedTab = tabPos;
      fireStateChanged();

      // if we're switching to either of the polar axis tabs, be sure the relevant axis is loaded into the axis 
      // properties card, which is used for both tabs! The tab label is the card ID for all tabs except the axis tabs.
      String cardID = TABLABELS[iSelectedTab];
      if(iSelectedTab == 1 || iSelectedTab == 2)
      {
         cardID = CARD_PAXIS;
         polarAxisCard.setAxisEdited(iSelectedTab==1);
      }
      
      // display card corresponding to the just selected tab
      CardLayout cl = (CardLayout) tabPanel.getLayout();
      cl.show(tabPanel, cardID);
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
      return(tabPos >= 0 && tabPos<getNumTabs() ? TABLABELS[tabPos] : null);
   }
   @Override public String getTabToolTip(int tabPos) { return(null); }
   @Override public Icon getTabIcon(int tabPos) { return(null); }
   @Override public boolean isTabClosable(int tabPos) { return(false); }
   @Override public String getCloseTabToolTip(int tabPos) { return(null); }
   @Override public void closeTab(int tabPos) {}
   @Override public boolean supportsTabRepositioning() { return(false); }
   @Override public boolean moveTab(int fromPos, int toPos) { return(false); }

   
   private FGNEditor getEditorForTab(int i)
   {
      FGNEditor editor = null;
      switch(i)
      {
      case 0 : editor = polarPlotCard; break;
      case 1 :
      case 2 : editor = polarAxisCard; break;
      case 3 : editor = colorBarCard; break;
      case 4 : editor = legendCard; break;
      }
      return(editor);
   }
   
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
   

   /** The polar plot currently being displayed/edited. Null if no plot is currently loaded in the editor. */
   private PolarPlotNode polarPlot = null;
   
   /** Controls which component node property editor is "in front". */
   private TabStrip tabStrip = null;
   
   /** 
    * Container for each of the different component node property editors, arranged as different "cards", with only one 
    * visible at a time. The user selects a particular editor via the tab strip that sits above this panel.
    */
   private JPanel tabPanel = null;
   
   /** Index of the currently selected tab, determining which property editor "card" is displayed. */
   private int iSelectedTab = -1;

   /** List of all change listeners registered with the tab strip model. */
   private EventListenerList tabListeners = new EventListenerList();

   /** Property editor for the polar plot node itself. It appears in the first tab of the composite editor. */
   private PolarPlotCard polarPlotCard = new PolarPlotCard();
   private final static String CARD_MAIN = "Main";
   
   /** 
    * Common property editor for the theta and radial axes. It appears in tabs 1-2 of the composite editor. 
    * It must be reconfigured on a tab switch to display the appropriate axis.
    */
   private PolarAxisCard polarAxisCard = new PolarAxisCard();
   private final static String CARD_PAXIS = "PAxis";
   
   /** 
    * Property editor for the the polar plot's color bar. It appears in tab 3 of the composite editor. It
    * includes support for adding, removing and editing any tick sets associated with the color bar.
    */
   private FGNGraphAxisCard colorBarCard = new FGNGraphAxisCard();
   private final static String CARD_CBAR = "Color Bar";
   
   /** Property editor for the legend node. It appears in tab 4 of the composite editor. */
   private FGNLegendCard legendCard = new FGNLegendCard();
   private final static String CARD_LEGEND = "Legend";
   
   /** The tab labels for the tab strip (they never change). */
   private final static String[] TABLABELS = new String[] {
      CARD_MAIN, "\u03b8", "R", CARD_CBAR, CARD_LEGEND
   };
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
   
   
   /**
    * Helper class implements the subordinate editor that appears in the "Main" tab and displays/edits all properties
    * of the polar plot node itself. It includes the following widgets:
    * <ul>
    * <li>Styled text editor and other widgets controlling the content, visibility, and positioning of the polar plot's
    * automated title.</li>
    * <li>A text field for the <i>id</i> property.</i>
    * <li>{@link MeasureEditor} fields specifying the width, height, and (x,y) coordinates for the bottom left corner of
    * the polar plot's bounding box (which locates the plot within a figure).</li>
    * <li>Check boxes for the plot's <i>clip</i> and <i>gridOnTop</i> properties.</i>
    * <li>A {@link RGBColorPicker} for setting the background color of the polar plot grid.</li>
    * <li>Push buttons that scale the plot (including all descendants) or all text within the plot by a percentage
    * specified in an accompanying numeric text field.</li>
    * <li>An embedded {@link TextStyleEditor} and {@link DrawStyleEditor} edits the polar plot node's text-related and 
    * draw-related style properties, respectively.</li>
    * </ul>
    * 
    * @author sruffner
    */
   private class PolarPlotCard extends FGNEditor implements ActionListener, FocusListener, PropertyChangeListener,
         ItemListener
   {
      /** Construct the graph node properties editor. */
      PolarPlotCard()
      {
         // we layout everything in a transparent panel that, in turn, is placed in the "north" of the editor panel --
         // to ensure things don't get spread out vertically
         JPanel p = new JPanel(new SpringLayout());
         p.setOpaque(false);
         SpringLayout layout = (SpringLayout) p.getLayout();

         showTitleChk = new JCheckBox("Title");
         showTitleChk.setToolTipText("Check this box to show graph title");
         showTitleChk.setContentAreaFilled(false);
         showTitleChk.addActionListener(this);
         p.add(showTitleChk);
               
         titleEditor = new StyledTextEditor(2);
         titleEditor.addActionListener(this);
         p.add(titleEditor);
         
         hAlignWidget = new MultiButton<TextAlign>();
         hAlignWidget.addChoice(TextAlign.LEADING, FCIcons.V4_ALIGNLEFT_16, "Left");
         hAlignWidget.addChoice(TextAlign.CENTERED, FCIcons.V4_ALIGNCENTER_16, "Center");
         hAlignWidget.addChoice(TextAlign.TRAILING, FCIcons.V4_ALIGNRIGHT_16, "Right");
         hAlignWidget.addItemListener(this);
         hAlignWidget.setToolTipText("Horizontal alignment of graph title WRT data window");
         p.add(hAlignWidget);
         
         JLabel gapLabel = new JLabel("Gap ");
         p.add(gapLabel);
         titleGapEditor = new MeasureEditor(6, FGNGraph.TITLEGAPCONSTRAINTS);
         titleGapEditor.addActionListener(this);
         titleGapEditor.setToolTipText("Vertical space between graph title and top/bottom edge of data window");
         p.add(titleGapEditor);
         
         JLabel idLabel = new JLabel("ID (optional): ");
         p.add(idLabel);
         idField = new JTextField(8);
         idField.setToolTipText(FGNEditor.IDATTR_TIP);
         idField.addActionListener(this);
         idField.addFocusListener(this);
         p.add(idField);
         
         clipChk = new JCheckBox("Clip data?");
         clipChk.setContentAreaFilled(false);
         clipChk.addActionListener(this);
         p.add(clipChk);
         
         gridOnTopChk = new JCheckBox("Grid on top?");
         gridOnTopChk.setContentAreaFilled(false);
         gridOnTopChk.addActionListener(this);
         p.add(gridOnTopChk);
         
         
         JLabel bkgCL = new JLabel("Grid Bkg: ");
         p.add(bkgCL);
         gridBkgPicker = new RGBColorPicker(true);
         gridBkgPicker.setToolTipText("Background color for polar coordinate grid");
         gridBkgPicker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
         p.add(gridBkgPicker);
         
         JLabel xLabel = new JLabel("X= ");
         p.add(xLabel);
         Measure.Constraints c = FGraphicModel.getLocationConstraints(FGNodeType.PGRAPH);
         xEditor = new MeasureEditor(8, c);
         xEditor.setToolTipText("Enter X-coordinate of BL corner of polar plot's bounding box");
         xEditor.addActionListener(this);
         p.add(xEditor);
         
         JLabel yLabel = new JLabel("Y= ");
         p.add(yLabel);
         yEditor = new MeasureEditor(8, c);
         yEditor.setToolTipText("Enter Y-coordinate of BL corner of polar plot's bounding box");
         yEditor.addActionListener(this);
         p.add(yEditor);
         
         c = FGraphicModel.getSizeConstraints(FGNodeType.PGRAPH);
         JLabel wLabel = new JLabel("W= ");
         p.add(wLabel);
         wEditor = new MeasureEditor(8, c);
         wEditor.setToolTipText("Enter width of polar plot's bounding box");
         wEditor.addActionListener(this);
         p.add(wEditor);
         
         JLabel hLabel = new JLabel("H= ");
         p.add(hLabel);
         hEditor = new MeasureEditor(8, c);
         hEditor.setToolTipText("Enter height of polar plot's bounding box");
         hEditor.addActionListener(this);
         p.add(hEditor);
         
         JLabel scaleLabel = new JLabel("Scale (%): ");
         p.add(scaleLabel);
         scalePctField = new NumericTextField(FGraphicNode.MINRESCALE, FGraphicNode.MAXRESCALE);
         scalePctField.setValue(100);
         scalePctField.addActionListener(this);
         p.add(scalePctField);
         
         rescaleBtn = new JButton("All");
         rescaleBtn.addActionListener(this);
         rescaleBtn.setOpaque(false);  // Needed this b/c default bkg showed around edges in Windows.
         rescaleBtn.setEnabled(false);
         p.add(rescaleBtn);
         
         rescaleFontsBtn = new JButton("Fonts");
         rescaleFontsBtn.addActionListener(this);
         rescaleFontsBtn.setOpaque(false);  // Needed this b/c default bkg showed around edges in Windows.
         rescaleFontsBtn.setEnabled(false);
         p.add(rescaleFontsBtn);
         
         textStyleEditor = new TextStyleEditor();
         p.add(textStyleEditor);

         drawStyleEditor = new DrawStyleEditor(true, false);
         p.add(drawStyleEditor);
         
         // left-right constraints. Assume textStyleEditor will be longest. 
         layout.putConstraint(SpringLayout.WEST, showTitleChk, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, hAlignWidget, GAP*3, SpringLayout.EAST, showTitleChk);
         layout.putConstraint(SpringLayout.WEST, gapLabel, GAP*3, SpringLayout.EAST, hAlignWidget);
         layout.putConstraint(SpringLayout.WEST, titleGapEditor, 0, SpringLayout.EAST, gapLabel);
         
         layout.putConstraint(SpringLayout.WEST, titleEditor, GAP, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.EAST, titleEditor, -GAP, SpringLayout.EAST, textStyleEditor);

         layout.putConstraint(SpringLayout.WEST, idLabel, GAP, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, idField, 0, SpringLayout.EAST, idLabel);
         layout.putConstraint(SpringLayout.EAST, idField, -GAP, SpringLayout.EAST, p);
         
         layout.putConstraint(SpringLayout.WEST, xLabel, GAP, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, xEditor, 0, SpringLayout.EAST, xLabel);
         layout.putConstraint(SpringLayout.WEST, yLabel, GAP*2, SpringLayout.EAST, xEditor);
         layout.putConstraint(SpringLayout.WEST, yEditor, 0, SpringLayout.EAST, yLabel);
         layout.putConstraint(SpringLayout.EAST, clipChk, -GAP, SpringLayout.EAST, p);

         // the labels and editors for X,Y,W,H are aligned in a grid-like layout
         layout.putConstraint(SpringLayout.EAST, wLabel, 0, SpringLayout.EAST, xLabel);
         layout.putConstraint(SpringLayout.WEST, wEditor, 0, SpringLayout.WEST, xEditor);
         layout.putConstraint(SpringLayout.EAST, hLabel, 0, SpringLayout.EAST, yLabel);
         layout.putConstraint(SpringLayout.WEST, hEditor, 0, SpringLayout.WEST, yEditor);
         layout.putConstraint(SpringLayout.EAST, gridOnTopChk, -GAP, SpringLayout.EAST, p);

         layout.putConstraint(SpringLayout.WEST, scaleLabel, GAP, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, scalePctField, 0, SpringLayout.EAST, scaleLabel);
         layout.putConstraint(SpringLayout.WEST, rescaleFontsBtn, GAP*2, SpringLayout.EAST, scalePctField);
         layout.putConstraint(SpringLayout.WEST, rescaleBtn, GAP, SpringLayout.EAST, rescaleFontsBtn);
         layout.putConstraint(SpringLayout.EAST, gridBkgPicker, -GAP, SpringLayout.EAST, p);
         layout.putConstraint(SpringLayout.EAST, bkgCL, 0, SpringLayout.WEST, gridBkgPicker);         
         
         layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.EAST, p, 0, SpringLayout.EAST, textStyleEditor);
         
         layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, p);
         
         // top-bottom constraints: 7 rows, with text and draw style editors in last two. In each row, one widget sets
         // constraints with row above or below. Remaining widgets are vertically centered WRT that widget.
         layout.putConstraint(SpringLayout.NORTH, showTitleChk, GAP, SpringLayout.NORTH, p);
         layout.putConstraint(SpringLayout.NORTH, titleEditor, GAP, SpringLayout.SOUTH, showTitleChk);
         layout.putConstraint(SpringLayout.NORTH, idField, GAP*2, SpringLayout.SOUTH, titleEditor);
         layout.putConstraint(SpringLayout.NORTH, clipChk, GAP*2, SpringLayout.SOUTH, idField);
         layout.putConstraint(SpringLayout.NORTH, gridOnTopChk, GAP, SpringLayout.SOUTH, clipChk);
         layout.putConstraint(SpringLayout.NORTH, gridBkgPicker, GAP*2, SpringLayout.SOUTH, gridOnTopChk);
         layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, gridBkgPicker);
         layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
         layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, drawStyleEditor);
         
         String vCtr = SpringLayout.VERTICAL_CENTER;
         layout.putConstraint(vCtr, hAlignWidget, 0, vCtr, showTitleChk);
         layout.putConstraint(vCtr, gapLabel, 0, vCtr, showTitleChk);
         layout.putConstraint(vCtr, titleGapEditor, 0, vCtr, showTitleChk);
         layout.putConstraint(vCtr, idLabel, 0, vCtr, idField);
         layout.putConstraint(vCtr, xLabel, 0, vCtr, clipChk);
         layout.putConstraint(vCtr, xEditor, 0, vCtr, clipChk);
         layout.putConstraint(vCtr, yLabel, 0, vCtr, clipChk);
         layout.putConstraint(vCtr, yEditor, 0, vCtr, clipChk);
         layout.putConstraint(vCtr, wLabel, 0, vCtr, gridOnTopChk);
         layout.putConstraint(vCtr, wEditor, 0, vCtr, gridOnTopChk);
         layout.putConstraint(vCtr, hLabel, 0, vCtr, gridOnTopChk);
         layout.putConstraint(vCtr, hEditor, 0, vCtr, gridOnTopChk);
         layout.putConstraint(vCtr, bkgCL, 0, vCtr, gridBkgPicker);
         layout.putConstraint(vCtr, scaleLabel, 0, vCtr, gridBkgPicker);
         layout.putConstraint(vCtr, scalePctField, 0, vCtr, gridBkgPicker);
         layout.putConstraint(vCtr, rescaleFontsBtn, 0, vCtr, gridBkgPicker);
         layout.putConstraint(vCtr, rescaleBtn, 0, vCtr, gridBkgPicker);

         setLayout(new BorderLayout());
         add(p, BorderLayout.NORTH);
      }

      @Override void reload(boolean initial)
      {
         if(polarPlot == null) return;
         
         titleEditor.loadContent(polarPlot.getAttributedTitle(false), polarPlot.getFontFamily(), 0, 
               polarPlot.getFillColor(), polarPlot.getFontStyle());
         showTitleChk.setSelected(!polarPlot.isTitleHidden());
         hAlignWidget.setCurrentChoice(polarPlot.getTitleHorizontalAlignment());
         titleGapEditor.setMeasure(polarPlot.getTitleGap());
         
         idField.setText(polarPlot.getID());
         
         clipChk.setSelected(polarPlot.getClip());
         gridOnTopChk.setSelected(polarPlot.getGridOnTop());
         gridBkgPicker.setCurrentColor(polarPlot.getGridBkgColor(), false);
         
         xEditor.setMeasure(polarPlot.getX());
         yEditor.setMeasure(polarPlot.getY());
         wEditor.setMeasure(polarPlot.getWidth());
         hEditor.setMeasure(polarPlot.getHeight());

         boolean isMultiSel = polarPlot.getGraphicModel().isMultiNodeSelection();
         scalePctField.setEnabled(!isMultiSel);
        
         textStyleEditor.loadGraphicNode(polarPlot);
         drawStyleEditor.loadGraphicNode(polarPlot);
      }

      @Override void onLowered()
      {
         textStyleEditor.cancelEditing();
         drawStyleEditor.cancelEditing();
         gridBkgPicker.cancelPopup();
      }

      /**
       * If the focus is on a text field that allows special characters, the special character is inserted at the 
       * current caret position. The user will still have to hit the "Enter" key or shift focus away from the text field
       * to submit the changes.
       */
      @Override void onInsertSpecialCharacter(String s)
      {
         if(titleEditor.isFocusOwner()) titleEditor.insertString(s, false);
         else if(idField.isFocusOwner()) idField.replaceSelection(s);
      }

      @Override boolean isEditorForNode(FGraphicNode n)
      {
         return(n != null && n.getNodeType() == FGNodeType.PGRAPH);
      }
      @Override ImageIcon getRepresentativeIcon() { return null; }
      @Override String getRepresentativeTitle() { return(null); }

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

      @Override public void itemStateChanged(ItemEvent e)
      {
         if((polarPlot != null) && (e.getSource() == hAlignWidget))
            polarPlot.setTitleHorizontalAlignment(hAlignWidget.getCurrentChoice());
      }

      @Override public void propertyChange(PropertyChangeEvent e)
      {
         if((polarPlot != null) && (e.getSource() == gridBkgPicker) && 
               (e.getPropertyName().equals(RGBColorPicker.COLOR_PROPERTY)))
            polarPlot.setGridBkgColor(gridBkgPicker.getCurrentColor());
      }

      @Override public void actionPerformed(ActionEvent e)
      {
         if(polarPlot == null) return;
         Object src = e.getSource();

         boolean ok = true;
         if(src == titleEditor)
         {
            String text = FGraphicNode.toStyledText(titleEditor.getCurrentContents(), 
                  polarPlot.getFontStyle(), polarPlot.getFillColor());
            ok = polarPlot.setTitle(text);
            if(!ok) titleEditor.loadContent(polarPlot.getAttributedTitle(false), polarPlot.getFontFamily(), 0, 
                  polarPlot.getFillColor(), polarPlot.getFontStyle());
         }
         else if(src == showTitleChk)
            polarPlot.setTitleHidden(!showTitleChk.isSelected());
         else if(src == titleGapEditor)
         {
            ok = polarPlot.setTitleGap(titleGapEditor.getMeasure());
            if(!ok) titleGapEditor.setMeasure(polarPlot.getTitleGap());
         }
         else if(src == idField)
         {
            ok = polarPlot.setID(idField.getText());
            if(!ok) idField.setText(polarPlot.getID());
         }
         else if(src == clipChk)
            polarPlot.setClip(clipChk.isSelected());
         else if(src == gridOnTopChk)
            polarPlot.setGridOnTop(gridOnTopChk.isSelected());
         else if(src == xEditor)
         {
            ok = polarPlot.setX(xEditor.getMeasure());
            if(!ok) xEditor.setMeasure(polarPlot.getX());
         }
         else if(src == yEditor)
         {
            ok = polarPlot.setY(yEditor.getMeasure());
            if(!ok) yEditor.setMeasure(polarPlot.getY());
         }
         else if(src == wEditor)
         {
            ok = polarPlot.setWidth(wEditor.getMeasure());
            if(!ok) wEditor.setMeasure(polarPlot.getWidth());
         }
         else if(src == hEditor)
         {
            ok = polarPlot.setHeight(hEditor.getMeasure());
            if(!ok) hEditor.setMeasure(polarPlot.getHeight());
         }
         else if(src == scalePctField)
         {
            boolean enaScale = (scalePctField.getValue().intValue() != 100);
            if(enaScale) enaScale = !polarPlot.getGraphicModel().isMultiNodeSelection();
            rescaleBtn.setEnabled(enaScale);
            rescaleFontsBtn.setEnabled(enaScale);
         }
         else if(src == rescaleBtn || src == rescaleFontsBtn)
         {
            int scale = scalePctField.getValue().intValue();
            if(scale == 100) return;
            polarPlot.rescale(scale, (src == rescaleFontsBtn));
            scalePctField.setValue(100);
            rescaleBtn.setEnabled(false);
            rescaleFontsBtn.setEnabled(false);
         }

         if(!ok) Toolkit.getDefaultToolkit().beep();
      }
      
      /** 
       * The polar graph's title is edited within this custom widget, which allows user to change font style or text 
       * color anywhere in the content, add superscripts or subscripts, and underline portions of the text.
       */
      private StyledTextEditor titleEditor = null;

      /** When checked, the polar graph's title is rendered (unless title string is empty). */
      private JCheckBox showTitleChk = null;
      
      /** Multiple-choice button widget for editing the H alignment of polar graph's title WRT its bounding box. */
      private MultiButton<TextAlign> hAlignWidget = null;
      
      /** 
       * Customized component for editing the size of the vertical gap between polar graph's automated title and the top
       * or bottom edge of its bounding box.
       */
      private MeasureEditor titleGapEditor = null;

      /** Text field in which optional node ID is edited. */
      private JTextField idField = null;
      
      /** Check box toggles state of the polar plot's boolean-valued <i>clip</i> property. */
      private JCheckBox clipChk = null;

      /** Check box toggles state of the polar plot's boolean-valued <i>gridOnTop</i> property. */
      private JCheckBox gridOnTopChk = null;

      /** Color picker displays/edits the background color of the polar coordinate grid. */
      private RGBColorPicker gridBkgPicker = null;
      
      /** Customized component for editing the x-coordinate of the BL corner of polar plot's bounding box. */
      private MeasureEditor xEditor = null;

      /** Customized component for editing the y-coordinate of the BL corner of polar plot's bounding box. */
      private MeasureEditor yEditor = null;

      /** Customized component for editing the bounding box width. */
      private MeasureEditor wEditor = null;

      /** Customized component for editing the bounding box height. */
      private MeasureEditor hEditor = null;

      /** Edits scale factor (as a percentage) for the re-scaling operations; 100 disables re-scaling. */
      private NumericTextField scalePctField = null;

      /** Pressing this button re-scales the entire polar plot. */
      private JButton rescaleBtn = null;

      /** Pressing this button re-scales all fonts in the polar plot. */
      private JButton rescaleFontsBtn = null;

      /** Self-contained editor handles the node's text styles. */
      private TextStyleEditor textStyleEditor = null;
      
      /** Self-contained editor handles the node's draw style properties. */
      private DrawStyleEditor drawStyleEditor = null;
   }
   
   /**
    * Helper class implements the subordinate editor card that displays/edits the properties of the theta or radial axis
    * of a polar plot, as enscapsulated by {@link PolarAxisNode}.
    * <ul>
    * <li>Numeric text fields specifying the start and end of the axis range.</li>
    * <li>A check box to reverse the axis direction, and one to show/hide the axis.</li>
    * <li>A text field to display/edit the grid divisions for the current axis. The divisions are displayed as comma-
    * separated tokens as: "S/E/M,N0,N1,...", where the special token S/E/M defines regularly spaced grid divisions of
    * size M starting at S and not exceeding E, and the remaining tokens specify custom grid divisions. S,E,M,N0, etc
    * are all floating-point tokens. See also {@link PolarAxisNode#setGridDivisionsFromString()}.</li>
    * <li>A text field to specify a list of custom grid labels, comma-separated. The labels are applied in the order
    * listed. If there are two few, they are reused; if too many, the extras are ignored. If blank, the standard
    * numerical labels are used.</li>
    * <li>A custom text field setting the gap separating a grid label from the grid. This is how far a grid label is 
    * from the closer endpoint of the corresponding ray (theta) or arc (radial) in the grid. For the radial axis, it is
    * ignored when the grid is full circle.</li>
    * <li>A custom combo box selecting the format for the automated numeric grid labels.</li>
    * <li>An integer text field specifying the "reference angle" for the axis in degrees CCW from the positive X 
    * direction in FC's global coordinates. For the theta axis, this is the direction at which "0 degrees" points; for
    * the radial axis, this is the angle of the ray along which the radial grid labels are centered.</li>
    * <li>A {@link TextStyleEditor} for editing the node's text-related styles, and a {@link DrawStyleEditor} for its
    * draw-related styles.</li>
    * </ul>
    * 
    * <p>The <b>PolarAxisCard</b> can be reconfigured to display the properties of the polar plot's theta axis or its
    * radial axis. To load a different axis, call {@link PolarAxisCard#setAxisEdited(boolean)}.</p>
    * 
    * @author sruffner
    */
   private class PolarAxisCard extends FGNEditor implements ActionListener, FocusListener
   {
      /** Construct an editor for displaying the properties of the theta or radial axis of a polar plot */
      PolarAxisCard()
      {
         isEditingTheta = true;
         
         // we layout everything in a transparent panel that, in turn, is placed in the "north" of the editor panel --
         // to ensure things don't get spread out vertically
         JPanel p = new JPanel(new SpringLayout());
         p.setOpaque(false);
         SpringLayout layout = (SpringLayout) p.getLayout();

         String strHelp = 
               "<html>Enter start S and end E of axis range. For theta axis, enter angles in degrees.<br/>" +
              "Axis range will be auto-corrected to ensure S &lt; E; for the theta axis, E and S must lie<br/>" +
              "in [-360..360] and E-S &le; 360deg</html>";
         startField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 4, -1);
         startField.setToolTipText(strHelp);
         startField.addActionListener(this);
         p.add(startField);
         
         JLabel toLabel = new JLabel(" to ");
         p.add(toLabel);
         endField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 4, -1);
         endField.setToolTipText(strHelp);
         endField.addActionListener(this);
         p.add(endField);
         
         revChk = new JCheckBox("Reversed?");
         revChk.setToolTipText(
               "<html>Check box to reverse the normal axis direction: clockwise vs. CCW for theta; increase<br/>" +
               "toward origin vs. away from origin for the radial axis.</html>");
         revChk.setContentAreaFilled(false);
         revChk.addActionListener(this);
         p.add(revChk);
         
         hideChk = new JCheckBox("Hide?");
         hideChk.setContentAreaFilled(false);
         hideChk.addActionListener(this);
         p.add(hideChk);
         
         JLabel divsLabel = new JLabel("Divs: ");
         p.add(divsLabel);
         gridDivsField = new JTextField();
         gridDivsField.addActionListener(this);
         gridDivsField.addFocusListener(this);
         gridDivsField.setToolTipText(
               "<html>Enter grid divisions as a list of whitespace-separated floating-point tokens. The first<br/>" +
               "token may have the format 'S/E/M', defining regularly spaced grid divisions of size M starting<br/>" +
               "at S and not exceeding E. If M&le;0, there are no regular divisions. If M&gt;0 but S&ge;E, then<br/>" +
               "S and E are replaced by the current axis range endpoints, and the regular grid divisions adjust<br/>" +
               "adjust when the range changes.<br/><br/>" +
               "All remaining tokens define custom grid division locations. If blank, the axis will have NO grid<br/>" +
               "divisions whatsoever.</html>");
         p.add(gridDivsField);
         
         /*
          If M&le;0, there are no such divisions. If M&gt;0 but S&ge;E, S and E are 
    * replaced by the current axis range endpoints -- so that the regular grid divisions automatically adjust when the
    * axis range changes
          */
         JLabel labelsLabel = new JLabel("Labels: ");
         p.add(labelsLabel);
         customLabelsField = new JTextField();
         customLabelsField.addActionListener(this);
         customLabelsField.addFocusListener(this);
         customLabelsField.setToolTipText(
               "<html>Enter custom grid labels here, comma-separated. Optional. Leave blank to use standard<br/>" +
               "numeric labels.<br/>" +
               "<b>NOTE</b>: Custom labels are applied to grid divisions in the order listed. If there are more<br/>" +
               "labels than divisions, some will be unused; if there are too few, the labels are reused<br/>" +
               "to ensure all grid divisions are labeled.</html>");
         p.add(customLabelsField);
         
         JLabel gapLabel = new JLabel("Gap ");
         p.add(gapLabel);
         gapEditor = new MeasureEditor(0, AxisNode.SPACERCONSTRAINTS);
         gapEditor.setToolTipText("Enter size of grid label gap (0-2in)");
         gapEditor.addActionListener(this);
         p.add(gapEditor);
         
         LabelFormat[] fmts = LabelFormat.values();
         formatCB = new JComboBox<LabelFormat>(fmts);
         formatCB.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<? extends Object> list, Object value, 
                  int index, boolean isSelected, boolean cellHasFocus)
            {
               super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
               String s = (value instanceof LabelFormat) ? ((LabelFormat)value).getGUILabel() : "";
               setText(s);
               return(this);
            }
         });
         formatCB.setToolTipText("Select numeric grid label format");
         formatCB.addActionListener(this);
         p.add(formatCB);
         
         JLabel refLabel = new JLabel("Reference Angle: ");
         p.add(refLabel);
         refAngleField = new NumericTextField(-359, 359);
         refAngleField.setToolTipText(REFANGLETIP_THETA);
         refAngleField.addActionListener(this);
         p.add(refAngleField);
         
         textStyleEditor = new TextStyleEditor();
         p.add(textStyleEditor);
         drawStyleEditor = new DrawStyleEditor(true, false);
         p.add(drawStyleEditor);
         
         // left-right constraints. Assume row with textStyleEditor will be longest.
         layout.putConstraint(SpringLayout.WEST, startField, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, toLabel, 0, SpringLayout.EAST, startField);
         layout.putConstraint(SpringLayout.WEST, endField, 0, SpringLayout.EAST, toLabel);
         layout.putConstraint(SpringLayout.EAST, revChk, 0, SpringLayout.WEST, hideChk);
         layout.putConstraint(SpringLayout.EAST, hideChk, 0, SpringLayout.EAST, textStyleEditor);

         layout.putConstraint(SpringLayout.WEST, divsLabel, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, gridDivsField, 0, SpringLayout.EAST, divsLabel);
         layout.putConstraint(SpringLayout.EAST, gridDivsField, 0, SpringLayout.EAST, textStyleEditor);

         layout.putConstraint(SpringLayout.WEST, labelsLabel, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, customLabelsField, 0, SpringLayout.EAST, labelsLabel);
         layout.putConstraint(SpringLayout.EAST, customLabelsField, 0, SpringLayout.EAST, textStyleEditor);
         
         layout.putConstraint(SpringLayout.WEST, gapLabel, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, gapEditor, 0, SpringLayout.EAST, gapLabel);
         layout.putConstraint(SpringLayout.WEST, formatCB, GAP*2, SpringLayout.EAST, gapEditor);
         layout.putConstraint(SpringLayout.EAST, refLabel, 0, SpringLayout.WEST, refAngleField);
         layout.putConstraint(SpringLayout.EAST, refAngleField, 0, SpringLayout.EAST, textStyleEditor);

         layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.EAST, p, 0, SpringLayout.EAST, textStyleEditor);
         
         layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, p);
         
         // top-bottom constraints: A widget in each row is used to set the constraints with row above or below. The 
         // remaining widgets are vertically centered WRT that widget.
         layout.putConstraint(SpringLayout.NORTH, startField, 0, SpringLayout.NORTH, p);
         layout.putConstraint(SpringLayout.NORTH, gridDivsField, GAP, SpringLayout.SOUTH, startField);
         layout.putConstraint(SpringLayout.NORTH, customLabelsField, GAP, SpringLayout.SOUTH, gridDivsField);
         layout.putConstraint(SpringLayout.NORTH, refAngleField, GAP, SpringLayout.SOUTH, customLabelsField);
         layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, refAngleField);
         layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
         layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, drawStyleEditor);
         
         String vCtr = SpringLayout.VERTICAL_CENTER;
         layout.putConstraint(vCtr, toLabel, 0, vCtr, startField);
         layout.putConstraint(vCtr, endField, 0, vCtr, startField);
         layout.putConstraint(vCtr, revChk, 0, vCtr, startField);
         layout.putConstraint(vCtr, hideChk, 0, vCtr, startField);
         
         layout.putConstraint(vCtr, divsLabel, 0, vCtr, gridDivsField);

         layout.putConstraint(vCtr, labelsLabel, 0, vCtr, customLabelsField);
         
         layout.putConstraint(vCtr, gapLabel, 0, vCtr, refAngleField);
         layout.putConstraint(vCtr, gapEditor, 0, vCtr, refAngleField);
         layout.putConstraint(vCtr, formatCB, 0, vCtr, refAngleField);
         layout.putConstraint(vCtr, refLabel, 0, vCtr, refAngleField);

         setLayout(new BorderLayout());
         add(p, BorderLayout.NORTH);
      }

      private final static String REFANGLETIP_THETA = 
            "<html>Reference angle in deg [-359..359] measured CCW from a ray pointing to the right. This<br/>" +
            " rotates the polar coordinate system so that '0 deg' points in the direction specified.</html>";
      private final static String REFANGLETIP_RAD =
            "<html>Reference angle in deg [-359..359] WRT the polar plot's theta coordinate. This is the angle<br/>" +
            "of the ray along which radial grid labels are centered. <b>If it is outside the theta axis range,<br/>" +
            "labels are drawn at one end of the circular arcs representing the radial grid.</b></html>";

      
      /**
       * Invoke this method to change which polar axis -- theta or radial -- is loaded into the editor.
       * @param isTheta True to load theta axis, false for radial axis.
       */
      void setAxisEdited(boolean isTheta) 
      { 
         isEditingTheta = isTheta;
         reload(true);
      }

      @Override void reload(boolean initial)
      {
         if(polarPlot == null) return;
         PolarAxisNode paxis = isEditingTheta ? polarPlot.getThetaAxis() : polarPlot.getRadialAxis();
         
         startField.setValue(paxis.getRangeMin());
         endField.setValue(paxis.getRangeMax());
         revChk.setSelected(paxis.getReversed());
         hideChk.setSelected(paxis.getHide());
         
         gridDivsField.setText(paxis.getGridDivisionsAsString(false));
         customLabelsField.setText(paxis.getCustomGridLabelsAsString());
         
         gapEditor.setMeasure(paxis.getGridLabelGap());
         formatCB.removeActionListener(this);
         formatCB.setSelectedItem(paxis.getGridLabelFormat());
         formatCB.addActionListener(this);
         
         refAngleField.setValue(paxis.getReferenceAngle());
         refAngleField.setToolTipText(isEditingTheta ? REFANGLETIP_THETA : REFANGLETIP_RAD);
         
         textStyleEditor.loadGraphicNode(paxis);
         drawStyleEditor.loadGraphicNode(paxis);
      }

      @Override void onLowered()
      {
         textStyleEditor.cancelEditing();
         drawStyleEditor.cancelEditing();
      }

      /** 
       * If the focus is on the custom grid labels field, the special character is inserted at the current caret 
       * position. The user will still have to hit the "Enter" key or shift focus away from the text field to submit 
       * the changes.
       */
      @Override void onInsertSpecialCharacter(String s)
      {
         if(customLabelsField.isFocusOwner()) customLabelsField.replaceSelection(s);
      }

      @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && n.getNodeType() == FGNodeType.PAXIS); }
      @Override ImageIcon getRepresentativeIcon() { return(null); }
      @Override String getRepresentativeTitle() { return(null); }

      @Override public void focusGained(FocusEvent e) 
      {
         if(e.isTemporary()) return;
         if(e.getSource() == gridDivsField) gridDivsField.selectAll();
         else if(e.getSource() == customLabelsField) customLabelsField.selectAll();
      }
      @Override public void focusLost(FocusEvent e)
      {
         if(e.isTemporary()) return;
         if(e.getSource() == gridDivsField) gridDivsField.postActionEvent();
         else if(e.getSource()  == customLabelsField) customLabelsField.postActionEvent();
      }

      @Override public void actionPerformed(ActionEvent e)
      {
         if(polarPlot == null) return;
         Object src = e.getSource();
         PolarAxisNode paxis = isEditingTheta ? polarPlot.getThetaAxis() : polarPlot.getRadialAxis();

         boolean ok = true;
         if(src == hideChk)
            paxis.setHide(hideChk.isSelected());
         else if(src == revChk)
            paxis.setReversed(revChk.isSelected());
         else if(src == startField || src == endField)
         {
            // the axis range endpoints are always set together so they can be validated and auto-corrected
            ok = paxis.setRange(startField.getValue().doubleValue(), endField.getValue().doubleValue());
            
            // start and end of range will be silently auto-corrected if necessary. Go ahead and fix the text fields 
            // accordingly.
            if(paxis.getRangeMin() != startField.getValue().doubleValue() ||
                  paxis.getRangeMax() != endField.getValue().doubleValue())
            {
               startField.setValue(paxis.getRangeMin());
               endField.setValue(paxis.getRangeMax());
            }
         }
         else if(src == gridDivsField)
         {
            String s = gridDivsField.getText().trim();
            ok = paxis.setGridDivisionsFromString(s, false);
            if(ok)
            {
               String adj = paxis.getGridDivisionsAsString(false);
               if(!adj.equals(s)) gridDivsField.setText(adj);
            }
            else gridDivsField.setText(paxis.getGridDivisionsAsString(false));
         }
         else if(src == customLabelsField)
         {
            String s = customLabelsField.getText().trim();
            paxis.setCustomGridLabelsFromString(s);
            String adj = paxis.getCustomGridLabelsAsString();
            if(!adj.equals(s)) customLabelsField.setText(adj);
         }
         else if(src == gapEditor)
         {
            ok = paxis.setGridLabelGap(gapEditor.getMeasure());
            if(!ok) gapEditor.setMeasure(paxis.getGridLabelGap());
         }
         else if(src == formatCB)
         {
            paxis.setGridLabelFormat((LabelFormat) formatCB.getSelectedItem());
         }
         else if(src == refAngleField)
         {
            ok = paxis.setReferenceAngle(refAngleField.getValue().intValue());
            if(!ok) refAngleField.setValue(paxis.getReferenceAngle());
         }

         if(!ok) Toolkit.getDefaultToolkit().beep();
      }

      /** True to edit polar plot's theta axis; false for its radial axis. */
      private boolean isEditingTheta;
      
      /** Numeric text field edits the axis range start. */
      private NumericTextField startField = null;

      /** Numeric text field edits the axis range end. */
      private NumericTextField endField = null;

      /** Checkbox to reverse direction of the polar axis (CW for theta; increase toward origin for radial). */
      private JCheckBox revChk = null;

     /** When checked, the axis is entirely hidden. */
      private JCheckBox hideChk = null;

      /** 
       * Displays/edits grid divisions as comma-separated tokens. The <i>first</i> token may have the form "S/E/M", a
       * triplet of numeric tokens specifying regular grid divisions of size M spanning [S..E]. The remaining tokens
       * must be numeric and represent custom grid division locations.
       */
      private JTextField gridDivsField = null;
      
      /** 
       * Displays/edits custom grid labels as a comma-separated list of label tokens. If empty, then standard
       * numeric grid labels are generated.
       */
      private JTextField customLabelsField = null;

      /** Customized component for editing grid label gap. */
      private MeasureEditor gapEditor = null;

      /** Button that selects the tick mark label format. */
      private JComboBox<LabelFormat> formatCB = null;

      /** Numeric text field for editing the reference angle for the axis. */
      private NumericTextField refAngleField = null;

      /** Self-contained editor handles the node's text styles. */
      private TextStyleEditor textStyleEditor = null;
      
      /** Self-contained editor handles the node's draw style properties. */
      private DrawStyleEditor drawStyleEditor = null;
   }
}
