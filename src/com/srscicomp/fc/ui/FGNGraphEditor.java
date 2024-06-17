package com.srscicomp.fc.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
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
import com.srscicomp.fc.fig.FGNGraph;
import com.srscicomp.fc.fig.FGNGraphAxis;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.GraphNode;
import com.srscicomp.fc.fig.GridLineNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;
import com.srscicomp.fc.uibase.StyledTextEditor;

/**
 * <code>FGNGraphEditor</code> displays/edits all properties of a graph, {@link GraphNode} --
 * including the properties of its various required "component nodes": the three axes, a legend, and H,V gridlines.
 * Unlike the other node property editors, it is a complex composite editor providing a tabbed interface to navigate
 * over all the properties of the graph node itself and any of its component nodes. A single instance of this editor
 * is installed in the figure composer {@link FigComposer}) and is raised in its "interaction layer" whenever the graph
 * itself OR one of its components gets the "display focus". When raised, the relevant tab is automatically selected so
 * that the properties of the selected component are on display.
 * 
 * <p>The subordinate editor that handles display/editing of the properties of a graph axis include the properties of
 * any tick mark set defined on the axis. The properties of the axis node itself are displayed in one tab, while each
 * defined tick set is displayed in a separate tab. Closing a tick set tab is the same as deleting that tick set, and a
 * push button is included so that the user can append new tick sets to an axis.</p>
 * 
 * <p>Implementation details. We use {@link TabStrip} and a companion container panel utilizing a card
 * layout manager to implement the tabbed-pane interface of <code>FGNGraphEditor</code> itself, and the subordinate
 * axis editor. Each subordinate editor is implemeted by a distinct inner class.</p>
 * 
 * @author sruffner
 */
class FGNGraphEditor extends FGNEditor implements TabStripModel
{
   /** Construct the graph node properties editor. */
   FGNGraphEditor()
   {
      super();

      TabStrip tabStrip = new TabStrip(this);
      
      tabPanel = new JPanel(new CardLayout());
      tabPanel.setOpaque(false);
      tabPanel.add(graphCard, CARD_MAIN);
      tabPanel.add(axisCard, CARD_AXIS);
      tabPanel.add(legendCard, CARD_LEGEND);
      tabPanel.add(gridCard, CARD_GRID);

      setLayout(new BorderLayout());
      add(tabStrip, BorderLayout.NORTH);
      add(tabPanel, BorderLayout.CENTER);
   }
   
   @Override public void reload(boolean initial)
   {
      // get the graph node being edited. Since this editor may be raised to edit one of the graph's component nodes,
      // or one of the tick sets for a graph axis, we have to be careful here.
      graph = null;
      FGraphicNode fgn = getEditedNode();
      FGNodeType nt = fgn.getNodeType();
      if(nt == FGNodeType.GRAPH) graph = (GraphNode) fgn;
      else
      {
         FGraphicNode parent = fgn.getParent();
         if(parent != null && parent.getNodeType() == FGNodeType.GRAPH) graph = (GraphNode) parent;
         else if(parent != null)
         {
            parent = parent.getParent();
            if(parent != null && parent.getNodeType() == FGNodeType.GRAPH) graph = (GraphNode) parent;
         }
      }
      assert(graph != null);
      
      // reload the individual editors. We have to make sure the axis card loads the correct component node
      graphCard.reload(initial);
      gridCard.reload(initial);
      if(initial)
      {
         // if the selected node is a graph axis or color bar, that node is loaded into the axis card initially; else 
         // the X axis node is loaded
         axisCard.loadAxis((fgn instanceof FGNGraphAxis) ? ((FGNGraphAxis)fgn) : graph.getPrimaryAxis());
         legendCard.loadLegend(graph.getLegend());
      }
      else
      {
         axisCard.reload(false);
         legendCard.reload(false);
      }
      // switch to the tab corresponding to the edited node -- unless we're re-loading
      if(iSelectedTab < 0 || initial)
      {
         int iTab = 0;
         if(nt == FGNodeType.AXIS)
         {
            if(fgn == graph.getPrimaryAxis()) iTab = 1;
            else iTab = 2;
         }
         else if(nt == FGNodeType.CBAR) iTab = 3;
         else if(nt == FGNodeType.LEGEND) iTab = 4;
         else if(nt == FGNodeType.TICKS)
         {
            FGraphicNode axis = fgn.getParent();
            if(axis.getNodeType() == FGNodeType.CBAR) iTab = 3;
            else if(axis == graph.getPrimaryAxis()) iTab = 1;
            else iTab = 2;
         }
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
      if(nt == FGNodeType.GRAPH) return(true);
      if(nt==FGNodeType.AXIS || nt==FGNodeType.CBAR || nt==FGNodeType.LEGEND || nt==FGNodeType.GRIDLINE)
         return(n.getParent() instanceof GraphNode);
      if(nt==FGNodeType.TICKS)
      {
         FGraphicNode grandParent = n.getParent();
         if(grandParent != null) grandParent = grandParent.getParent();
         return(grandParent instanceof GraphNode);
      }
      return(false);     
   }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_GRAPH_32); }
   @Override String getRepresentativeTitle() { return("Graph Properties"); }

   
   @Override public int getNumTabs() { return(TABLABELS.length); }
   @Override public int getSelectedTab() { return(iSelectedTab); }

   @Override public void setSelectedTab(int tabPos)
   {
      if(tabPos < 0 || tabPos >= getNumTabs() || tabPos == iSelectedTab) return;
      
      iSelectedTab = tabPos;
      fireStateChanged();

      // if we're switching to any of the axis tabs, be sure the relevant axis is loaded into the axis properties card,
      // which is used for all 3 tabs! The tab label is the card ID for all tabs except the axis tabs.
      String cardID = TABLABELS[iSelectedTab];
      if(iSelectedTab >= 1 && iSelectedTab <= 3)
      {
         cardID = CARD_AXIS;
         axisCard.loadAxis((iSelectedTab == 1) ? graph.getPrimaryAxis() : 
            ((iSelectedTab == 2) ? graph.getSecondaryAxis() : graph.getColorBar()));
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
      case 0 : editor = graphCard; break;
      case 1 :
      case 2 : 
      case 3 : editor = axisCard; break;
      case 4 : editor = legendCard; break;
      case 5 : editor = gridCard; break;
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
   

   /** The graph currently being displayed/edited. Null if no graph is currently loaded in the editor. */
   private GraphNode graph = null;

   /**
    * Container for each of the different component node property editors, arranged as different "cards", with only one 
    * visible at a time. The user selects a particular editor via the tab strip that sits above this panel.
    */
   private final JPanel tabPanel;
   
   /** Index of the currently selected tab, determining which property editor "card" is displayed. */
   private int iSelectedTab = -1;

   /** List of all change listeners registered with the tab strip model. */
   private final EventListenerList tabListeners = new EventListenerList();

   /** Property editor for the graph node itself. It appears in the first tab of the composite graph node editor. */
   private final GraphCard graphCard = new GraphCard();
   private final static String CARD_MAIN = "Main";
   
   /** 
    * Common property editor for the 2D graph's X,Y axes and color bar. It appears in tabs 1-3 of the composite graph 
    * node editor. It must be reconfigured on a tab switch to display the appropriate node.
    */
   private final FGNGraphAxisCard axisCard = new FGNGraphAxisCard();
   private final static String CARD_AXIS = "Axis";
   
   /** Property editor for the legend node. It appears in tab 4 of the composite graph node editor. */
   private final FGNLegendCard legendCard = new FGNLegendCard();
   private final static String CARD_LEGEND = "Legend";
   
   /** Property editor for the graph's grid-line nodes. It appears in tab 5 of the composite graph node editor. */
   private final GridCard gridCard = new GridCard();
   private final static String CARD_GRID = "Grid";
   
   /** The tab labels for the tab strip (they never change). */
   private final static String[] TABLABELS = new String[] {
      CARD_MAIN, "X", "Y", "Color Bar", CARD_LEGEND, CARD_GRID
   };
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
   
   
   /**
    * Helper class implements the subordinate editor that appears in the "Main" tab and displays/edits all properties
    * of a {@link GraphNode}. It includes the following widgets:
    * <ul>
    * <li>Text fields specifying the (x,y) coordinates for the bottom left corner of the graph's data window (which 
    * locates the graph within a figure), the data window's width and height, and its orientation (a rotation about the 
    * bottom-left corner).</li>
    * <li>An iconic pushbutton for cycling through the different axis layouts (1st quadrant, 2nd, and so on); a combo
    * box for selecting the coordinate system type; and check boxes for enabling data window clipping and automatic
    * range adjustment for each graph axis.</li>
    * <li>A {@link RGBColorPicker} for setting the background color of the graph's data box.</li>
    * <li>Push buttons that scale the graph (including all descendants) or all text within the graph by a percentage
    * specified in an accompanying numeric text field.</li>
    * <li>A plain text field to specify the graph's title (currently, this is for identification purposes only; the 
    * title is not rendered on the graph.</li>
    * <li>An embedded {@link TextStyleEditor} and {@link DrawStyleEditor} edits the label node's text-related and 
    * draw-related style properties, respectively.</li>
    * </ul>
    * 
    * @author sruffner
    */
   private class GraphCard extends FGNEditor 
      implements ActionListener, FocusListener, ItemListener, PropertyChangeListener
   {
      /** Construct the graph node properties editor. */
      GraphCard()
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
         
         hAlignWidget = new MultiButton<>();
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

         coordSysCombo = new JComboBox<>(GraphNode.CoordSys.values());
         coordSysCombo.setToolTipText("Select the coordinate system type");
         coordSysCombo.addActionListener(this);
         p.add(coordSysCombo);
         
         quadMB = new MultiButton<>();
         quadMB.addChoice(GraphNode.Layout.QUAD1, FCIcons.V4_QUAD1, "1st quad");
         quadMB.addChoice(GraphNode.Layout.QUAD2, FCIcons.V4_QUAD2, "2nd quad");
         quadMB.addChoice(GraphNode.Layout.QUAD3, FCIcons.V4_QUAD3, "3rd quad");
         quadMB.addChoice(GraphNode.Layout.QUAD4, FCIcons.V4_QUAD4, "4th quad");
         quadMB.addChoice(GraphNode.Layout.ALLQUAD, FCIcons.V4_ALLQUAD, "four quadrants");
         quadMB.addItemListener(this);
         quadMB.setToolTipText("Quadrant layout");
         p.add(quadMB);
         
         clipCheckBox = new JCheckBox("Clip data?");
         clipCheckBox.setContentAreaFilled(false);
         clipCheckBox.addActionListener(this);
         p.add(clipCheckBox);
         
         JLabel autorngLabel = new JLabel("Auto-range?: ");
         p.add(autorngLabel);
         autorangeXChk = new JCheckBox("X");
         autorangeXChk.setToolTipText("Check(uncheck) to enable(disable) automatic range adjustment for X axis");
         autorangeXChk.setContentAreaFilled(false);
         autorangeXChk.addActionListener(this);
         p.add(autorangeXChk);
         
         autorangeYChk = new JCheckBox("Y");
         autorangeYChk.setToolTipText("Check(uncheck) to enable(disable) automatic range adjustment for Y axis");
         autorangeYChk.setContentAreaFilled(false);
         autorangeYChk.addActionListener(this);
         p.add(autorangeYChk);
         
         autorangeZChk = new JCheckBox("Z");
         autorangeZChk.setToolTipText("Check(uncheck) to enable(disable) automatic range adjustment for Z axis");
         autorangeZChk.setContentAreaFilled(false);
         autorangeZChk.addActionListener(this);
         p.add(autorangeZChk);
         
         JLabel boxCL = new JLabel("Box Bkg: ");
         p.add(boxCL);
         boxColorPicker = new RGBColorPicker(true);
         boxColorPicker.setToolTipText("Background color for graph's data box");
         boxColorPicker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
         p.add(boxColorPicker);
         
         JLabel xLabel = new JLabel("X= ");
         p.add(xLabel);
         Measure.Constraints c = FGraphicModel.getLocationConstraints(FGNodeType.GRAPH);
         xEditor = new MeasureEditor(8, c);
         xEditor.setToolTipText("Enter X-coordinate of BL corner of graph's data viewport");
         xEditor.addActionListener(this);
         p.add(xEditor);
         
         JLabel yLabel = new JLabel("Y= ");
         p.add(yLabel);
         yEditor = new MeasureEditor(8, c);
         yEditor.setToolTipText("Enter Y-coordinate of BL corner of graph's data viewport");
         yEditor.addActionListener(this);
         p.add(yEditor);
         
         c = FGraphicModel.getSizeConstraints(FGNodeType.GRAPH);
         JLabel wLabel = new JLabel("W= ");
         p.add(wLabel);
         wEditor = new MeasureEditor(8, c);
         wEditor.setToolTipText("Enter width of graph's data viewport");
         wEditor.addActionListener(this);
         p.add(wEditor);
         
         JLabel hLabel = new JLabel("H= ");
         p.add(hLabel);
         hEditor = new MeasureEditor(8, c);
         hEditor.setToolTipText("Enter height of graph's data viewport");
         hEditor.addActionListener(this);
         p.add(hEditor);
         
         JLabel rotLabel = new JLabel("\u03b8= ");
         p.add(rotLabel);
         rotateField = new NumericTextField(-180.0, 180.0, 2);
         rotateField.setToolTipText("Orientation (rotation in deg CCW about BL corner of data viewport)");
         rotateField.addActionListener(this);
         p.add(rotateField);
         
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
         
         // left-right constraints. Assume textStyleEditor will be longest. The large quadrant layout button spans the
         // the third and 4th widget rows vertically!
         layout.putConstraint(SpringLayout.WEST, showTitleChk, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, hAlignWidget, GAP*3, SpringLayout.EAST, showTitleChk);
         layout.putConstraint(SpringLayout.WEST, gapLabel, GAP*3, SpringLayout.EAST, hAlignWidget);
         layout.putConstraint(SpringLayout.WEST, titleGapEditor, 0, SpringLayout.EAST, gapLabel);
         
         layout.putConstraint(SpringLayout.WEST, titleEditor, GAP, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.EAST, titleEditor, -GAP, SpringLayout.EAST, textStyleEditor);

         layout.putConstraint(SpringLayout.WEST, idLabel, GAP, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, idField, 0, SpringLayout.EAST, idLabel);
         layout.putConstraint(SpringLayout.EAST, idField, -GAP, SpringLayout.EAST, p);
         
         layout.putConstraint(SpringLayout.WEST, quadMB, GAP, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, coordSysCombo, GAP*2, SpringLayout.EAST, quadMB);
         layout.putConstraint(SpringLayout.EAST, clipCheckBox, -GAP, SpringLayout.EAST, p);
         layout.putConstraint(SpringLayout.EAST, boxColorPicker, -GAP*4, SpringLayout.WEST, clipCheckBox);
         layout.putConstraint(SpringLayout.EAST, boxCL, 0, SpringLayout.WEST, boxColorPicker);
         layout.putConstraint(SpringLayout.WEST, autorngLabel, GAP*2, SpringLayout.EAST, quadMB);
         layout.putConstraint(SpringLayout.WEST, autorangeXChk, GAP, SpringLayout.EAST, autorngLabel);
         layout.putConstraint(SpringLayout.WEST, autorangeYChk, GAP, SpringLayout.EAST, autorangeXChk);
         layout.putConstraint(SpringLayout.WEST, autorangeZChk, GAP, SpringLayout.EAST, autorangeYChk);
         
         layout.putConstraint(SpringLayout.WEST, xLabel, GAP, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, xEditor, 0, SpringLayout.EAST, xLabel);
         layout.putConstraint(SpringLayout.WEST, yLabel, GAP*2, SpringLayout.EAST, xEditor);
         layout.putConstraint(SpringLayout.WEST, yEditor, 0, SpringLayout.EAST, yLabel);
         layout.putConstraint(SpringLayout.WEST, rotLabel, GAP*2, SpringLayout.EAST, yEditor);
         layout.putConstraint(SpringLayout.WEST, rotateField, 0, SpringLayout.EAST, rotLabel);
         
         // the labels and editors for X,Y,W,H are aligned in a grid-like layout
         layout.putConstraint(SpringLayout.EAST, wLabel, 0, SpringLayout.EAST, xLabel);
         layout.putConstraint(SpringLayout.WEST, wEditor, 0, SpringLayout.WEST, xEditor);
         layout.putConstraint(SpringLayout.EAST, hLabel, 0, SpringLayout.EAST, yLabel);
         layout.putConstraint(SpringLayout.WEST, hEditor, 0, SpringLayout.WEST, yEditor);
         
         layout.putConstraint(SpringLayout.WEST, scaleLabel, GAP, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, scalePctField, 0, SpringLayout.EAST, scaleLabel);
         layout.putConstraint(SpringLayout.WEST, rescaleFontsBtn, GAP*2, SpringLayout.EAST, scalePctField);
         layout.putConstraint(SpringLayout.WEST, rescaleBtn, GAP, SpringLayout.EAST, rescaleFontsBtn);

         layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.EAST, p, 0, SpringLayout.EAST, textStyleEditor);
         
         layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, p);
         
         // top-bottom constraints: 9 rows, with text and draw style editors in last two. In most rows, one
         // widget sets constraints with row above or below. Remaining widgets are vertically centered WRT that widget.
         layout.putConstraint(SpringLayout.NORTH, showTitleChk, GAP, SpringLayout.NORTH, p);
         layout.putConstraint(SpringLayout.NORTH, titleEditor, GAP, SpringLayout.SOUTH, showTitleChk);
         
         layout.putConstraint(SpringLayout.NORTH, idField, GAP*2, SpringLayout.SOUTH, titleEditor);
         layout.putConstraint(SpringLayout.NORTH, coordSysCombo, GAP*2, SpringLayout.SOUTH, idField);
         layout.putConstraint(SpringLayout.NORTH, autorangeXChk, GAP, SpringLayout.SOUTH, coordSysCombo);
         layout.putConstraint(SpringLayout.NORTH, rotateField, GAP*2, SpringLayout.SOUTH, autorangeXChk);
         layout.putConstraint(SpringLayout.NORTH, wEditor, GAP, SpringLayout.SOUTH, rotateField);
         layout.putConstraint(SpringLayout.NORTH, scalePctField, GAP, SpringLayout.SOUTH, wEditor);
         layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, scalePctField);
         layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
         layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, drawStyleEditor);
         
         String vCtr = SpringLayout.VERTICAL_CENTER;
         layout.putConstraint(vCtr, hAlignWidget, 0, vCtr, showTitleChk);
         layout.putConstraint(vCtr, gapLabel, 0, vCtr, showTitleChk);
         layout.putConstraint(vCtr, titleGapEditor, 0, vCtr, showTitleChk);
         layout.putConstraint(vCtr, idLabel, 0, vCtr, idField);
         layout.putConstraint(vCtr, clipCheckBox, 0, vCtr, coordSysCombo);
         layout.putConstraint(vCtr, boxCL, 0, vCtr, coordSysCombo);
         layout.putConstraint(vCtr, boxColorPicker, 0, vCtr, coordSysCombo);
         layout.putConstraint(vCtr, autorngLabel, 0, vCtr, autorangeXChk);
         layout.putConstraint(vCtr, autorangeYChk, 0, vCtr, autorangeXChk);
         layout.putConstraint(vCtr, autorangeZChk, 0, vCtr, autorangeXChk);

         // the large quadrant layout button spans 3rd and 4th rows  vertically!
         layout.putConstraint(vCtr, quadMB, GAP/2, SpringLayout.SOUTH, coordSysCombo);
         
         layout.putConstraint(vCtr, xLabel, 0, vCtr, rotateField);
         layout.putConstraint(vCtr, xEditor, 0, vCtr, rotateField);
         layout.putConstraint(vCtr, yLabel, 0, vCtr, rotateField);
         layout.putConstraint(vCtr, yEditor, 0, vCtr, rotateField);
         layout.putConstraint(vCtr, rotLabel, 0, vCtr, rotateField);
         layout.putConstraint(vCtr, wLabel, 0, vCtr, wEditor);
         layout.putConstraint(vCtr, hLabel, 0, vCtr, wEditor);
         layout.putConstraint(vCtr, hEditor, 0, vCtr, wEditor);
         layout.putConstraint(vCtr, scaleLabel, 0, vCtr, scalePctField);
         layout.putConstraint(vCtr, rescaleFontsBtn, 1, vCtr, scalePctField);
         layout.putConstraint(vCtr, rescaleBtn, 1, vCtr, scalePctField);

         setLayout(new BorderLayout());
         add(p, BorderLayout.NORTH);
      }

      @Override void reload(boolean initial)
      {
         titleEditor.loadContent(graph.getAttributedTitle(false), graph.getFontFamily(), 0, graph.getFillColor(), 
               graph.getFontStyle());
         showTitleChk.setSelected(!graph.isTitleHidden());
         hAlignWidget.setCurrentChoice(graph.getTitleHorizontalAlignment());
         titleGapEditor.setMeasure(graph.getTitleGap());
         
         idField.setText(graph.getID());
         
         coordSysCombo.setSelectedItem(graph.getCoordSys());
         quadMB.setCurrentChoice(graph.getLayout());
         
         clipCheckBox.setSelected(graph.getClip());
         
         GraphNode.Autorange autorng = graph.getAutorangeAxes();
         autorangeXChk.setSelected(autorng.isXAxisAutoranged());
         autorangeYChk.setSelected(autorng.isYAxisAutoranged());
         autorangeZChk.setSelected(autorng.isZAxisAutoranged());

         boxColorPicker.setCurrentColor(graph.getBoxColor(), false);
         
         xEditor.setMeasure(graph.getX());
         yEditor.setMeasure(graph.getY());
         wEditor.setMeasure(graph.getWidth());
         hEditor.setMeasure(graph.getHeight());
         rotateField.setValue(graph.getRotate());

         FGraphicModel fgm = graph.getGraphicModel();
         boolean isMultiSel = (fgm != null) && fgm.isMultiNodeSelection();
         scalePctField.setEnabled(!isMultiSel);
        
         textStyleEditor.loadGraphicNode(graph);
         drawStyleEditor.loadGraphicNode(graph);
      }

      @Override void onLowered()
      {
         textStyleEditor.cancelEditing();
         drawStyleEditor.cancelEditing();
         boxColorPicker.cancelPopup();
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
         return(n != null && n.getNodeType() == FGNodeType.GRAPH);
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
         if(graph == null) return;
         Object src = e.getSource();
         if(src == quadMB)
            graph.setLayout(quadMB.getCurrentChoice());
         else if(src == hAlignWidget)
            graph.setTitleHorizontalAlignment(hAlignWidget.getCurrentChoice());
      }

      @Override public void propertyChange(PropertyChangeEvent e)
      {
         if((graph != null) && (e.getSource() == boxColorPicker) && 
               (e.getPropertyName().equals(RGBColorPicker.COLOR_PROPERTY)))
            graph.setBoxColor(boxColorPicker.getCurrentColor());
      }

      @Override public void actionPerformed(ActionEvent e)
      {
         if(graph == null) return;
         Object src = e.getSource();

         boolean ok = true;
         if(src == titleEditor)
         {
            String text = FGraphicNode.toStyledText(titleEditor.getCurrentContents(), 
                  graph.getFontStyle(), graph.getFillColor());
            ok = graph.setTitle(text);
            if(!ok) titleEditor.loadContent(graph.getAttributedTitle(false), graph.getFontFamily(), 0, 
                  graph.getFillColor(), graph.getFontStyle());
         }
         else if(src == showTitleChk)
            graph.setTitleHidden(!showTitleChk.isSelected());
         else if(src == titleGapEditor)
         {
            ok = graph.setTitleGap(titleGapEditor.getMeasure());
            if(!ok) titleGapEditor.setMeasure(graph.getTitleGap());
         }
         else if(src == idField)
         {
            ok = graph.setID(idField.getText());
            if(!ok) idField.setText(graph.getID());
         }
         else if(src == coordSysCombo)
            graph.setCoordSys((GraphNode.CoordSys) coordSysCombo.getSelectedItem());
         else if(src == clipCheckBox)
            graph.setClip(clipCheckBox.isSelected());
         else if(src == autorangeXChk || src == autorangeYChk || src == autorangeZChk)
            graph.setAutorangeAxes(autorangeXChk.isSelected(),autorangeYChk.isSelected(),autorangeZChk.isSelected());
         if(src == xEditor)
         {
            ok = graph.setX(xEditor.getMeasure());
            if(!ok) xEditor.setMeasure(graph.getX());
         }
         else if(src == yEditor)
         {
            ok = graph.setY(yEditor.getMeasure());
            if(!ok) yEditor.setMeasure(graph.getY());
         }
         else if(src == wEditor)
         {
            ok = graph.setWidth(wEditor.getMeasure());
            if(!ok) wEditor.setMeasure(graph.getWidth());
         }
         else if(src == hEditor)
         {
            ok = graph.setHeight(hEditor.getMeasure());
            if(!ok) hEditor.setMeasure(graph.getHeight());
         }
         else if(src == rotateField)
         {
            ok = graph.setRotate(rotateField.getValue().doubleValue());
            if(!ok) rotateField.setValue(graph.getRotate());
         }
         else if(src == scalePctField)
         {
            boolean enaScale = (scalePctField.getValue().intValue() != 100);
            if(enaScale)
            {
               FGraphicModel fgm = graph.getGraphicModel();
               enaScale = (fgm == null) || !fgm.isMultiNodeSelection();
            }
            rescaleBtn.setEnabled(enaScale);
            rescaleFontsBtn.setEnabled(enaScale);
         }
         else if(src == rescaleBtn || src == rescaleFontsBtn)
         {
            int scale = scalePctField.getValue().intValue();
            if(scale == 100) return;
            graph.rescale(scale, (src == rescaleFontsBtn));
            scalePctField.setValue(100);
            rescaleBtn.setEnabled(false);
            rescaleFontsBtn.setEnabled(false);
         }

         if(!ok) Toolkit.getDefaultToolkit().beep();
      }
      
      /** 
       * The graph's title is edited within this custom widget, which allows user to change font style or text color 
       * anywhere in the content, add superscripts or subscripts, and underline portions of the text.
       */
      private StyledTextEditor titleEditor = null;

      /** When checked, the graph's title is rendered (unless title string is empty). */
      private final JCheckBox showTitleChk;
      
      /** Multiple-choice button widget for editing the H alignment of graph's title WRT its bounding box. */
      private MultiButton<TextAlign> hAlignWidget = null;
      
      /** 
       * Customized component for editing the size of the vertical gap between graph's automated title and the top or
       * bottom edge of its bounding box.
       */
      private MeasureEditor titleGapEditor = null;

      /** Text field in which optional node ID is edited. */
      private JTextField idField = null;
      
      /** Combo box selects the type of coordinate system displayed in graph. */
      private JComboBox<GraphNode.CoordSys> coordSysCombo = null;

      /** Multiple-choice button widget selects the graph's quadrant layout; current layout reflected by button icon. */
      private MultiButton<GraphNode.Layout> quadMB = null;
      
      /** Check box toggles state of the graph's boolean-valued <i>clip</i> property. */
      private JCheckBox clipCheckBox = null;

      /** When checked, automatic range adjustments are enabled for the graph's X (theta) axis. */
      private JCheckBox autorangeXChk = null;

      /** Color picker displays/edits the background color of the graph's data box. */
      private RGBColorPicker boxColorPicker = null;
      
      /** When checked, automatic range adjustments are enabled for the graph's Y (radial) axis. */
      private JCheckBox autorangeYChk = null;

      /** When checked, automatic range adjustments are enabled for the graph's Z (color) axis. */
      private JCheckBox autorangeZChk = null;

      /** Customized component for editing the x-coordinate of the graph's bottom-left corner. */
      private MeasureEditor xEditor = null;

      /** Customized component for editing the y-coordinate of the graph's bottom-left corner. */
      private MeasureEditor yEditor = null;

      /** Customized component for editing the graph's width. */
      private MeasureEditor wEditor = null;

      /** Customized component for editing the graph's height. */
      private MeasureEditor hEditor = null;

      /** Numeric text field for setting the orientation of the graph as a rotation about bottom-left corner. */
      private NumericTextField rotateField = null;

      /** Edits scale factor (as a percentage) for the re-scaling operations; 100 disables re-scaling. */
      private NumericTextField scalePctField = null;

      /** Pressing this button re-scales the entire graph. */
      private JButton rescaleBtn = null;

      /** Pressing this button re-scales all fonts in the graph. */
      private JButton rescaleFontsBtn = null;

      /** Self-contained editor handles the node's text styles. */
      private TextStyleEditor textStyleEditor = null;
      
      /** Self-contained editor handles the node's draw style properties. */
      private DrawStyleEditor drawStyleEditor = null;
   }
   
   /**
    * Helper class implements the subordinate editor that appears in the "Grid" tab and displays/edits all properties
    * of the graph's two component {@link GridLineNode}s. For each set of grid lines (horizontal
    * and vertical), it includes a check box for showing/hiding the grid lines, and an embedded {@link DrawStyleEditor} 
    * for displaying the grid line draw styles.
    * 
    * @author sruffner
    */
   private class GridCard extends FGNEditor implements ActionListener
   {
      /** Construct the graph grid properties editor. */
      GridCard()
      {
         showHChk = new JCheckBox("Show horizontal (or circular) grid lines?");
         showHChk.setContentAreaFilled(false);
         showHChk.addActionListener(this);
         add(showHChk);
         
         hDrawStyleEditor = new DrawStyleEditor(true, false);
         add(hDrawStyleEditor);

         showVChk = new JCheckBox("Show vertical (or radial) grid lines?");
         showVChk.setContentAreaFilled(false);
         showVChk.addActionListener(this);
         add(showVChk);

         vDrawStyleEditor = new DrawStyleEditor(true, false);
         add(vDrawStyleEditor);

         SpringLayout layout = new SpringLayout();
         setLayout(layout);
         
         // left-right constraints. We don't tie anything to the right edge b/c other editor tabs will be wider than
         // this one.
         layout.putConstraint(SpringLayout.WEST, showHChk, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, hDrawStyleEditor, GAP*5, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, showVChk, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, vDrawStyleEditor, GAP*5, SpringLayout.WEST, this);
         
         // top-bottom constraints. Note larger gap separating wigets for H grid lines vs those for V grid lines. We
         // don't tie anything to the bottom edge b/c other editor tabs will be taller than this one
         layout.putConstraint(SpringLayout.NORTH, showHChk, GAP, SpringLayout.NORTH, this);
         layout.putConstraint(SpringLayout.NORTH, hDrawStyleEditor, 0, SpringLayout.SOUTH, showHChk);
         layout.putConstraint(SpringLayout.NORTH, showVChk, GAP*5, SpringLayout.SOUTH, hDrawStyleEditor);
         layout.putConstraint(SpringLayout.NORTH, vDrawStyleEditor, 0, SpringLayout.SOUTH, showVChk);
      }
      
      @Override void reload(boolean initial)
      {
         GridLineNode gln = graph.getPrimaryGridLines();
         showHChk.setSelected(!gln.getHide());
         hDrawStyleEditor.loadGraphicNode(gln);
         
         gln = graph.getSecondaryGridLines();
         showVChk.setSelected(!gln.getHide());
         vDrawStyleEditor.loadGraphicNode(gln);
      }
      
      @Override void onLowered()
      {
         hDrawStyleEditor.cancelEditing();
         vDrawStyleEditor.cancelEditing();
      }

      @Override boolean isEditorForNode(FGraphicNode n)
      {
         return(n != null && n.getNodeType() == FGNodeType.GRIDLINE);
      }
      @Override ImageIcon getRepresentativeIcon() { return null; }
      @Override String getRepresentativeTitle() { return(null); }


      @Override public void actionPerformed(ActionEvent e)
      {
         if(graph == null) return;
         Object src = e.getSource();

         if(src == showHChk)
         {
            GridLineNode gln = graph.getPrimaryGridLines();
            gln.setHide(!showHChk.isSelected());
         }
         else if(src == showVChk)
         {
            GridLineNode gln = graph.getSecondaryGridLines();
            gln.setHide(!showVChk.isSelected());
         }
      }

      
      /** Check box toggles the show/hide state of the graph's horizontal (or circular) grid lines. */
      private final JCheckBox showHChk;
      /** Self-contained editor handles the draw style properties for the graph's primary axis grid lines. */
      private DrawStyleEditor hDrawStyleEditor = null;
      /** Check box toggles the show/hide state of the graph's vertical (or radial) grid lines. */
      private JCheckBox showVChk = null;
      /** Self-contained editor handles the draw style properties for the graph's secondary axis grid lines. */
      private DrawStyleEditor vDrawStyleEditor = null;
   }
}
