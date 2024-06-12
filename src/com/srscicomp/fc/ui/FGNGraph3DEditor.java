package com.srscicomp.fc.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

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

import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.ui.MultiButton;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.common.ui.TabStrip;
import com.srscicomp.common.ui.TabStripModel;
import com.srscicomp.fc.fig.Axis3DNode;
import com.srscicomp.fc.fig.AxisNode;
import com.srscicomp.fc.fig.FGNGraph;
import com.srscicomp.fc.fig.FGNGraphAxis;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.Graph3DNode;
import com.srscicomp.fc.fig.LogTickPattern;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.fig.TickSetNode;
import com.srscicomp.fc.fig.Ticks3DNode;
import com.srscicomp.fc.fig.TickSetNode.LabelFormat;
import com.srscicomp.fc.uibase.StyledTextEditor;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
 * <b>FGNGraph3DEditor</b> displays/edits all properties of a 3D graph, {@link Graph3DNode} -- including the properties
 * of its various required "component nodes": three backplanes (XY, YZ, XZ); three gridlines (X,Y,Z); three axes
 * (X,Y,Z); and the legend and color bar. It is a complex composite editor providing a tabbed interface to navigate over
 * all the properties of the 3D graph node itself and any of its component nodes. A single instance of this editor is 
 * installed in the figure composer {@link FigComposer}) and is raised whenever the graph itself gets the "display 
 * focus".
 * 
 * <p>The subordinate editor that handles display/editing of the properties of a graph axis include the properties of
 * any tick mark set defined on the axis. The properties of the axis node itself are displayed in one tab, while each
 * defined tick set is displayed in a separate tab. Closing a tick set tab is the same as deleting that tick set, and a
 * push button is included so that the user can append new tick sets to an axis.</p>
 * 
 * <p>Implementation details. We use {@link TabStrip} and a companion container panel utilizing a card layout manager to
 * implement the tabbed-pane interface, and the subordinate axis editor. Each subordinate editor is implemeted by a 
 * distinct inner class.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNGraph3DEditor extends FGNEditor implements TabStripModel
{
   /** Construct the 3D graph node properties editor. */
   FGNGraph3DEditor()
   {
      super();
      
      tabStrip = new TabStrip(this);
      
      tabPanel = new JPanel(new CardLayout());
      tabPanel.setOpaque(false);
      tabPanel.add(mainCard, CARD_MAIN);
      tabPanel.add(backDropCard, CARD_BACK);
      tabPanel.add(axis3dCard, CARD_AXIS);
      tabPanel.add(colorBarCard, CARD_CBAR);
      tabPanel.add(legendCard, CARD_LEGEND);
      
      setLayout(new BorderLayout());
      add(tabStrip, BorderLayout.NORTH);
      add(tabPanel, BorderLayout.CENTER);
   }
   
   @Override public void reload(boolean initial)
   {
      // get the graph node being edited. Since this editor may be raised to edit one of the graph's component nodes,
      // or one of the tick sets for a graph axis, we have to be careful here.
      graph3d = null;
      FGraphicNode fgn = getEditedNode();
      FGNodeType nt = fgn.getNodeType();
      if(nt == FGNodeType.GRAPH3D) graph3d = (Graph3DNode) fgn;
      else
      {
         FGraphicNode parent = fgn.getParent();
         if(parent != null && parent.getNodeType() == FGNodeType.GRAPH3D) graph3d = (Graph3DNode) parent;
         else if(parent != null)
         {
            parent = parent.getParent();
            if(parent != null && parent.getNodeType() == FGNodeType.GRAPH3D) graph3d = (Graph3DNode) parent;
         }
      }
      assert(graph3d != null);
      
      // reload the individual editors. We have to make sure the axis card loads the correct component node
      mainCard.reload(initial);
      backDropCard.reload(initial);
      if(initial)
      {
         Graph3DNode.Axis whichAxis = Graph3DNode.Axis.X;
         if(nt == FGNodeType.AXIS3D) whichAxis = ((Axis3DNode)fgn).getAxis();
         else if(nt == FGNodeType.TICKS3D) whichAxis = ((Axis3DNode) fgn.getParent()).getAxis();
         axis3dCard.loadAxis(whichAxis);
         
         colorBarCard.loadAxis(graph3d.getColorBar());
         legendCard.loadLegend(graph3d.getLegend());
      }
      else
      {
         axis3dCard.reload(false);
         colorBarCard.reload(false);
         legendCard.reload(false);
      }
      
      // switch to the tab corresponding to the edited node -- unless we're re-loading
      if(iSelectedTab < 0 || initial)
      {
         int iTab = 0;
         if(nt == FGNodeType.BACK3D || nt == FGNodeType.GRID3D) iTab = 1;
         else if(nt == FGNodeType.AXIS3D || nt == FGNodeType.TICKS3D)
         {
            Axis3DNode axis3DNode = (Axis3DNode) (nt==FGNodeType.AXIS3D ? fgn : fgn.getParent());
            switch(axis3DNode.getAxis())
            {
            case X : iTab = 2; break;
            case Y : iTab = 3; break;
            default: iTab = 4; break;
            }
         }
         else if(nt == FGNodeType.CBAR || nt == FGNodeType.TICKS) iTab = 5;
         else if(nt == FGNodeType.LEGEND) iTab = 6;
         
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
      FGraphicNode parent = n.getParent();
      boolean isLegend = (FGNodeType.LEGEND==nt) && (parent != null) && (parent.getNodeType()==FGNodeType.GRAPH3D);
      boolean isColorBar = (FGNodeType.CBAR==nt) && (parent != null) && (parent.getNodeType()==FGNodeType.GRAPH3D);
      boolean isCBTicks = (FGNodeType.TICKS==nt) && (parent != null) && (parent.getNodeType()==FGNodeType.CBAR);
      if(isCBTicks)
      {
         parent = parent.getParent();
         isCBTicks = (parent != null) && (parent.getNodeType()==FGNodeType.GRAPH3D);
      }
      
      return(FGNodeType.GRAPH3D == nt || FGNodeType.BACK3D == nt || FGNodeType.GRID3D == nt || 
            FGNodeType.AXIS3D == nt || FGNodeType.TICKS3D == nt || isLegend || isColorBar || isCBTicks); 

   }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_GRAPH3D_32); }
   @Override String getRepresentativeTitle() { return("3D Graph Properties"); }

   
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
      if(iSelectedTab >= 2 && iSelectedTab <= 4)
      {
         cardID = CARD_AXIS;
         axis3dCard.loadAxis(iSelectedTab==2 ? Graph3DNode.Axis.X : 
            (iSelectedTab==3 ? Graph3DNode.Axis.Y : Graph3DNode.Axis.Z));
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
      case 0 : editor = mainCard; break;
      case 1 : editor = backDropCard; break;
      case 5 : editor = colorBarCard; break;
      case 6 : editor = legendCard; break;
      default : editor = axis3dCard; break;
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

   /** The 3D graph currently being displayed/edited. Null if no graph is currently loaded in the editor. */
   private Graph3DNode graph3d = null;
   
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

   /** Property editor for the 3D graph node itself. It appears in the first tab of the composite editor. */
   private MainCard mainCard = new MainCard();
   private final static String CARD_MAIN = "Main";
   
   /** Property editor for the 3D graph's backdrop. It appears in the second tab of the composite editor. */
   private BackDropCard backDropCard = new BackDropCard();
   private final static String CARD_BACK = "Backdrop";
   
   /** 
    * Common property editor for the three graph axes. It appears in tabs 2-4 of the composite 3D graph node editor. It 
    * must be reconfigured on a tab switch to display the appropriate axis.
    */
   private Axis3DCard axis3dCard = new Axis3DCard();
   private final static String CARD_AXIS = "Axis";
   
   /** Property editor for the color bar node. It appears in tab 5 of the composite 3D graph node editor. */
   private FGNGraphAxisCard colorBarCard = new FGNGraphAxisCard();
   private final static String CARD_CBAR = "Color Bar";
   
   /** Property editor for the legend node. It appears in tab 6 of the composite 3D graph node editor. */
   private FGNLegendCard legendCard = new FGNLegendCard();
   private final static String CARD_LEGEND = "Legend";
   
   /** The tab labels for the tab strip (they never change). */
   private final static String[] TABLABELS = new String[] {
      CARD_MAIN, CARD_BACK, "X", "Y", "Z", CARD_CBAR, CARD_LEGEND
   };
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
   
   
   /**
    * Helper class implements the subordinate editor that appears in the "Main" tab and displays/edits all properties
    * of the 3D graph node itself -- except its backdrop style. It includes the following widgets:
    * <ul>
    * <li>Text fields specifying the (x,y) coordinates for the origin of the graph's data cube (which locates the graph 
    * within a figure), and the data cube's dimensions in 3D space.</li>
    * <li>Text fields to specify the projection geometry for the 3D graph: azimuth and elevation angles, and the
    * projection distance scale factor.</li>
    * <li>A plain text field to specify the graph's title and unique ID.</li>
    * <li>An embedded {@link TextStyleEditor} and {@link DrawStyleEditor} edits the 3D graph node's text-related and 
    * draw-related style properties, respectively.</li>
    * </ul>
    * 
    * @author sruffner
    */
   private class MainCard extends FGNEditor implements ActionListener, FocusListener, ItemListener
   {
      /** Construct the 3D graph node property editor for the "Main" tab. */
      MainCard()
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

         JLabel xLabel = new JLabel("X= ");
         p.add(xLabel);
         Measure.Constraints c = FGraphicModel.getLocationConstraints(FGNodeType.GRAPH3D);
         xEditor = new MeasureEditor(8, c);
         xEditor.setToolTipText("Enter X-coordinate for the location of the 3D graph's origin");
         xEditor.addActionListener(this);
         p.add(xEditor);
         
         JLabel yLabel = new JLabel("Y= ");
         p.add(yLabel);
         yEditor = new MeasureEditor(8, c);
         yEditor.setToolTipText("Enter Y-coordinate for the location of the 3D graph's origin");
         yEditor.addActionListener(this);
         p.add(yEditor);
         
         c = FGraphicModel.getSizeConstraints(FGNodeType.GRAPH3D);
         JLabel wLabel = new JLabel("W= ");
         p.add(wLabel);
         wEditor = new MeasureEditor(8, c);
         wEditor.setToolTipText("Enter width of graph in 3D (length spanned by X-axis)");
         wEditor.addActionListener(this);
         p.add(wEditor);
         
         JLabel hLabel = new JLabel("H= ");
         p.add(hLabel);
         hEditor = new MeasureEditor(8, c);
         hEditor.setToolTipText("Enter height of graph in 3D (length spanned by Y-axis)");
         hEditor.addActionListener(this);
         p.add(hEditor);
         
         JLabel depthLabel = new JLabel("D= ");
         p.add(depthLabel);
         depthEditor = new MeasureEditor(8, c);
         depthEditor.setToolTipText("Enter depth of graph in 3D (length spanned by Z-axis)");
         depthEditor.addActionListener(this);
         p.add(depthEditor);
         
         JLabel scaleLabel = new JLabel("S= ");
         p.add(scaleLabel);
         distScaleField = new NumericTextField(2, 20);
         distScaleField.setToolTipText("<html>Enter projection distance scale factor [2..20].<br>"
               + "<i>Actual projection distance is product of this factor and the largest graph dimension.</i></html>");
         distScaleField.addActionListener(this);
         p.add(distScaleField);
         
         JLabel rotLabel = new JLabel("\u03b8(Z)= ");
         p.add(rotLabel);
         rotateField = new NumericTextField(-180.0, 180.0, 1);
         rotateField.setToolTipText("Rotation of 3D graph box about its Z-axis (deg CCW) [-180..180]");
         rotateField.addActionListener(this);
         p.add(rotateField);
         
         JLabel elevLabel = new JLabel("\u03b8(X)= ");
         p.add(elevLabel);
         elevField = new NumericTextField(-60.0, 60.0, 1);
         elevField.setToolTipText("Rotation of 3D graph box about its X-axis (deg CCW) [-60..60]");
         elevField.addActionListener(this);
         p.add(elevField);
         
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
         
         // the labels and editors for X,Y,W,H,D,S,R,E are aligned in a grid-like layout
         layout.putConstraint(SpringLayout.WEST, xLabel, GAP, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, xEditor, 0, SpringLayout.EAST, xLabel);
         layout.putConstraint(SpringLayout.WEST, yLabel, GAP*2, SpringLayout.EAST, xEditor);
         layout.putConstraint(SpringLayout.WEST, yEditor, 0, SpringLayout.EAST, yLabel);
         layout.putConstraint(SpringLayout.WEST, depthLabel, GAP*2, SpringLayout.EAST, yEditor);
         layout.putConstraint(SpringLayout.WEST, depthEditor, 0, SpringLayout.EAST, depthLabel);

         layout.putConstraint(SpringLayout.EAST, wLabel, 0, SpringLayout.EAST, xLabel);
         layout.putConstraint(SpringLayout.WEST, wEditor, 0, SpringLayout.WEST, xEditor);
         layout.putConstraint(SpringLayout.EAST, hLabel, 0, SpringLayout.EAST, yLabel);
         layout.putConstraint(SpringLayout.WEST, hEditor, 0, SpringLayout.WEST, yEditor);

         layout.putConstraint(SpringLayout.EAST, scaleLabel, 0, SpringLayout.EAST, xLabel);
         layout.putConstraint(SpringLayout.WEST, distScaleField, 0, SpringLayout.WEST, xEditor);
         layout.putConstraint(SpringLayout.EAST, rotLabel, 0, SpringLayout.EAST, yLabel);
         layout.putConstraint(SpringLayout.WEST, rotateField, 0, SpringLayout.WEST, yEditor);
         layout.putConstraint(SpringLayout.EAST, elevLabel, 0, SpringLayout.EAST, depthLabel);
         layout.putConstraint(SpringLayout.WEST, elevField, 0, SpringLayout.WEST, depthEditor);

         layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.EAST, p, 0, SpringLayout.EAST, textStyleEditor);
         
         layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, p);
         
         // top-bottom constraints: 6 rows, with text and draw style editors in last two. In each of first 5 rows, one
         // widget sets constraints with row above or below. Remaining widgets are vertically centered WRT that widget.
         layout.putConstraint(SpringLayout.NORTH, showTitleChk, GAP, SpringLayout.NORTH, p);
         layout.putConstraint(SpringLayout.NORTH, titleEditor, GAP, SpringLayout.SOUTH, showTitleChk);
         layout.putConstraint(SpringLayout.NORTH, idField, GAP*2, SpringLayout.SOUTH, titleEditor);
          layout.putConstraint(SpringLayout.NORTH, xEditor, GAP*2, SpringLayout.SOUTH, idField);
         layout.putConstraint(SpringLayout.NORTH, wEditor, GAP, SpringLayout.SOUTH, xEditor);
         layout.putConstraint(SpringLayout.NORTH, rotateField, GAP, SpringLayout.SOUTH, wEditor);
         layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*2, SpringLayout.SOUTH, rotateField);
         layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
         layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, drawStyleEditor);
         
         String vCtr = SpringLayout.VERTICAL_CENTER;
         layout.putConstraint(vCtr, hAlignWidget, 0, vCtr, showTitleChk);
         layout.putConstraint(vCtr, gapLabel, 0, vCtr, showTitleChk);
         layout.putConstraint(vCtr, titleGapEditor, 0, vCtr, showTitleChk);
         layout.putConstraint(vCtr, idLabel, 0, vCtr, idField);
         layout.putConstraint(vCtr, xLabel, 0, vCtr, xEditor);
         layout.putConstraint(vCtr, yLabel, 0, vCtr, xEditor);
         layout.putConstraint(vCtr, yEditor, 0, vCtr, xEditor);
         layout.putConstraint(vCtr, wLabel, 0, vCtr, wEditor);
         layout.putConstraint(vCtr, hLabel, 0, vCtr, wEditor);
         layout.putConstraint(vCtr, hEditor, 0, vCtr, wEditor);
         layout.putConstraint(vCtr, depthLabel, 0, vCtr, wEditor);
         layout.putConstraint(vCtr, depthEditor, 0, vCtr, wEditor);
         layout.putConstraint(vCtr, scaleLabel, 0, vCtr, rotateField);
         layout.putConstraint(vCtr, distScaleField, 0, vCtr, rotateField);
         layout.putConstraint(vCtr, rotLabel, 0, vCtr, rotateField);
         layout.putConstraint(vCtr, elevLabel, 0, vCtr, rotateField);
         layout.putConstraint(vCtr, elevField, 0, vCtr, rotateField);

         setLayout(new BorderLayout());
         add(p, BorderLayout.NORTH);
      }

      @Override void reload(boolean initial)
      {
         titleEditor.loadContent(graph3d.getAttributedTitle(false), graph3d.getFontFamily(), 0, 
               graph3d.getFillColor(), graph3d.getFontStyle());
         showTitleChk.setSelected(!graph3d.isTitleHidden());
         hAlignWidget.setCurrentChoice(graph3d.getTitleHorizontalAlignment());
         titleGapEditor.setMeasure(graph3d.getTitleGap());
         
         idField.setText(graph3d.getID());
         
         xEditor.setMeasure(graph3d.getX());
         yEditor.setMeasure(graph3d.getY());
         wEditor.setMeasure(graph3d.getWidth());
         hEditor.setMeasure(graph3d.getHeight());
         depthEditor.setMeasure(graph3d.getDepth());
         
         distScaleField.setValue(graph3d.getProjectionDistanceScale());
         rotateField.setValue(graph3d.getRotate());
         elevField.setValue(graph3d.getElevate());

         textStyleEditor.loadGraphicNode(graph3d);
         drawStyleEditor.loadGraphicNode(graph3d);
      }

      @Override void onLowered()
      {
         textStyleEditor.cancelEditing();
         drawStyleEditor.cancelEditing();
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
         return(n != null && n.getNodeType() == FGNodeType.GRAPH3D);
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
         if((graph3d != null) && (e.getSource() == hAlignWidget))
            graph3d.setTitleHorizontalAlignment(hAlignWidget.getCurrentChoice());
      }

      @Override public void actionPerformed(ActionEvent e)
      {
         if(graph3d == null) return;
         Object src = e.getSource();

         boolean ok = true;
         if(src == titleEditor)
         {
            String text = FGraphicNode.toStyledText(titleEditor.getCurrentContents(), 
                  graph3d.getFontStyle(), graph3d.getFillColor());
            ok = graph3d.setTitle(text);
            if(!ok) titleEditor.loadContent(graph3d.getAttributedTitle(false), graph3d.getFontFamily(), 0, 
                  graph3d.getFillColor(), graph3d.getFontStyle());
         }
         else if(src == showTitleChk)
            graph3d.setTitleHidden(!showTitleChk.isSelected());
         else if(src == titleGapEditor)
         {
            ok = graph3d.setTitleGap(titleGapEditor.getMeasure());
            if(!ok) titleGapEditor.setMeasure(graph3d.getTitleGap());
         }
         else if(src == idField)
         {
            ok = graph3d.setID(idField.getText());
            if(!ok) idField.setText(graph3d.getID());
         }
         else if(src == xEditor)
         {
            ok = graph3d.setX(xEditor.getMeasure());
            if(!ok) xEditor.setMeasure(graph3d.getX());
         }
         else if(src == yEditor)
         {
            ok = graph3d.setY(yEditor.getMeasure());
            if(!ok) yEditor.setMeasure(graph3d.getY());
         }
         else if(src == wEditor)
         {
            ok = graph3d.setWidth(wEditor.getMeasure());
            if(!ok) wEditor.setMeasure(graph3d.getWidth());
         }
         else if(src == hEditor)
         {
            ok = graph3d.setHeight(hEditor.getMeasure());
            if(!ok) hEditor.setMeasure(graph3d.getHeight());
         }
         else if(src == depthEditor)
         {
            ok = graph3d.setDepth(depthEditor.getMeasure());
            if(!ok) depthEditor.setMeasure(graph3d.getDepth());
         }
         else if(src == distScaleField)
         {
            ok = graph3d.setProjectionDistanceScale(distScaleField.getValue().intValue());
            if(!ok) distScaleField.setValue(graph3d.getProjectionDistanceScale());
         }
         else if(src == rotateField)
         {
            ok = graph3d.setRotate(rotateField.getValue().doubleValue());
            if(!ok) rotateField.setValue(graph3d.getRotate());
         }
         else if(src == elevField)
         {
            ok = graph3d.setElevate(elevField.getValue().doubleValue());
            if(!ok) elevField.setValue(graph3d.getElevate());
         }

         if(!ok) Toolkit.getDefaultToolkit().beep();
      }
      
      /** 
       * The graph's title is edited within this custom widget, which allows user to change font style or text color 
       * anywhere in the content, add superscripts or subscripts, and underline portions of the text.
       */
      private StyledTextEditor titleEditor = null;

      /** When checked, the graph's title is rendered (unless title string is empty). */
      private JCheckBox showTitleChk = null;
      
      /** Multiple-choice button widget for editing the H alignment of graph's title WRT its bounding box. */
      private MultiButton<TextAlign> hAlignWidget = null;
      
      /** 
       * Customized component for editing the size of the vertical gap between graph's automated title and the top or
       * bottom edge of its bounding box.
       */
      private MeasureEditor titleGapEditor = null;

      /** Text field in which optional node ID is edited. */
      private JTextField idField = null;
      
      /** The x-coordinate of the 3D graph's origin WRT parent's viewport. */
      private MeasureEditor xEditor = null;

      /** The y-coordinate of the 3D graph's origin WRT parent's viewport */
      private MeasureEditor yEditor = null;

      /** Length spanned by the graph's X-axis in 3D -- ie, width of the 3D graph "cube". */
      private MeasureEditor wEditor = null;

      /** Length spanned by the graph's Y-axis in 3D -- ie, height of the 3D graph "cube". */
      private MeasureEditor hEditor = null;

      /** Length spanned by the graph's Z-axis in 3D -- ie, depth of the 3D graph "cube". */
      private MeasureEditor depthEditor = null;

      /** Projection distance scale factor. */
      private NumericTextField distScaleField = null;

      /** Rotation angle (deg) of the 3D graph about its Z-axis (CCW positive). */
      private NumericTextField rotateField = null;
      
      /** Elevation angle (deg), ie, rotation of the 3D graph about its X-axis (CCW positive). */
      private NumericTextField elevField = null;

      /** Self-contained editor handles the node's text styles. */
      private TextStyleEditor textStyleEditor = null;
      
      /** Self-contained editor handles the node's draw style properties. */
      private DrawStyleEditor drawStyleEditor = null;
   }
   
   /**
    * Helper class implements the subordinate editor that appears in the "Backdrop" tab and displays/edits the 3D 
    * graph's backdrop style, along with the appearance of its XY/XZ/YZ backplanes and X/Y/Z grid lines. For each  
    * backplane and grid line component, it includes an embedded {@link DrawStyleEditor} -- giving the user independent
    * control of the appearance of each component.
    * 
    * @author sruffner
    */
   private class BackDropCard extends FGNEditor implements ActionListener
   {
      /** Construct the 3D graph backdrop properties editor. */
      BackDropCard()
      {
         JLabel styleLabel = new JLabel("Backdrop Style: " );
         add(styleLabel);
         backDropCombo = new JComboBox<Graph3DNode.BackDrop>(Graph3DNode.BackDrop.values());
         backDropCombo.addActionListener(this);
         add(backDropCombo);
         
         JLabel bpLabel = new JLabel("Backplanes:");
         add(bpLabel);
         
         JLabel xyLabel = new JLabel("XY: ");
         add(xyLabel);
         xyDrawStyleEditor = new DrawStyleEditor(false, false);
         add(xyDrawStyleEditor);

         JLabel xzLabel = new JLabel("XZ: ");
         add(xzLabel);
         xzDrawStyleEditor = new DrawStyleEditor(false, false);
         add(xzDrawStyleEditor);

         JLabel yzLabel = new JLabel("YZ: ");
         add(yzLabel);
         yzDrawStyleEditor = new DrawStyleEditor(false, false);
         add(yzDrawStyleEditor);

         JLabel gridLabel = new JLabel("Grid Lines:");
         add(gridLabel);
         
         JLabel xGridLabel = new JLabel("X: ");
         add(xGridLabel);
         xGridDrawStyleEditor = new DrawStyleEditor(true, false);
         add(xGridDrawStyleEditor);

         JLabel yGridLabel = new JLabel("Y: ");
         add(yGridLabel);
         yGridDrawStyleEditor = new DrawStyleEditor(true, false);
         add(yGridDrawStyleEditor);

         JLabel zGridLabel = new JLabel("Z: ");
         add(zGridLabel);
         zGridDrawStyleEditor = new DrawStyleEditor(true, false);
         add(zGridDrawStyleEditor);

         SpringLayout layout = new SpringLayout();
         setLayout(layout);
         
         // left-right constraints. We don't constrain the right edge b/c other editor tabs will be wider than this one.
         layout.putConstraint(SpringLayout.WEST, styleLabel, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, backDropCombo, 0, SpringLayout.EAST, styleLabel);
         
         layout.putConstraint(SpringLayout.WEST, bpLabel, 0, SpringLayout.WEST, this);

         layout.putConstraint(SpringLayout.WEST, xyLabel, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, xyDrawStyleEditor, 0, SpringLayout.EAST, xyLabel);
         layout.putConstraint(SpringLayout.WEST, xzLabel, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, xzDrawStyleEditor, 0, SpringLayout.EAST, xzLabel);
         layout.putConstraint(SpringLayout.WEST, yzLabel, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, yzDrawStyleEditor, 0, SpringLayout.EAST, yzLabel);
         
         layout.putConstraint(SpringLayout.WEST, gridLabel, 0, SpringLayout.WEST, this);

         layout.putConstraint(SpringLayout.WEST, xGridLabel, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, xGridDrawStyleEditor, 0, SpringLayout.WEST, yzDrawStyleEditor);
         layout.putConstraint(SpringLayout.WEST, yGridLabel, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, yGridDrawStyleEditor, 0, SpringLayout.WEST, yzDrawStyleEditor);
         layout.putConstraint(SpringLayout.WEST, zGridLabel, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, zGridDrawStyleEditor, 0, SpringLayout.WEST, yzDrawStyleEditor);

         // top-bottom constraints. We don't constrain the bottom edge b/c other editor tabs will be taller.
         layout.putConstraint(SpringLayout.NORTH, backDropCombo, GAP, SpringLayout.NORTH, this);
         layout.putConstraint(SpringLayout.NORTH, bpLabel, GAP*2, SpringLayout.SOUTH, backDropCombo);
         layout.putConstraint(SpringLayout.NORTH, xyDrawStyleEditor, GAP, SpringLayout.SOUTH, bpLabel);
         layout.putConstraint(SpringLayout.NORTH, xzDrawStyleEditor, GAP, SpringLayout.SOUTH, xyDrawStyleEditor);
         layout.putConstraint(SpringLayout.NORTH, yzDrawStyleEditor, GAP, SpringLayout.SOUTH, xzDrawStyleEditor);
         layout.putConstraint(SpringLayout.NORTH, gridLabel, GAP*2, SpringLayout.SOUTH, yzDrawStyleEditor);
         layout.putConstraint(SpringLayout.NORTH, xGridDrawStyleEditor, GAP, SpringLayout.SOUTH, gridLabel);
         layout.putConstraint(SpringLayout.NORTH, yGridDrawStyleEditor, GAP, SpringLayout.SOUTH, xGridDrawStyleEditor);
         layout.putConstraint(SpringLayout.NORTH, zGridDrawStyleEditor, GAP, SpringLayout.SOUTH, yGridDrawStyleEditor);

         String vCtr = SpringLayout.VERTICAL_CENTER;
         layout.putConstraint(vCtr, styleLabel, 0, vCtr, backDropCombo);
         layout.putConstraint(vCtr, xyLabel, 0, vCtr, xyDrawStyleEditor);
         layout.putConstraint(vCtr, xzLabel, 0, vCtr, xzDrawStyleEditor);
         layout.putConstraint(vCtr, yzLabel, 0, vCtr, yzDrawStyleEditor);
         layout.putConstraint(vCtr, xGridLabel, 0, vCtr, xGridDrawStyleEditor);
         layout.putConstraint(vCtr, yGridLabel, 0, vCtr, yGridDrawStyleEditor);
         layout.putConstraint(vCtr, zGridLabel, 0, vCtr, zGridDrawStyleEditor);
      }
      
      @Override void reload(boolean initial)
      {
         backDropCombo.setSelectedItem(graph3d.getBackDrop());
         
         xyDrawStyleEditor.loadGraphicNode(graph3d.getBackPlane(Graph3DNode.Side.XY));
         xzDrawStyleEditor.loadGraphicNode(graph3d.getBackPlane(Graph3DNode.Side.XZ));
         yzDrawStyleEditor.loadGraphicNode(graph3d.getBackPlane(Graph3DNode.Side.YZ));
         xGridDrawStyleEditor.loadGraphicNode(graph3d.getGrid3DNode(Graph3DNode.Axis.X));
         yGridDrawStyleEditor.loadGraphicNode(graph3d.getGrid3DNode(Graph3DNode.Axis.Y));
         zGridDrawStyleEditor.loadGraphicNode(graph3d.getGrid3DNode(Graph3DNode.Axis.Z));
      }
      
      @Override void onLowered()
      {
         xyDrawStyleEditor.cancelEditing();
         xzDrawStyleEditor.cancelEditing();
         yzDrawStyleEditor.cancelEditing();
         xGridDrawStyleEditor.cancelEditing();
         yGridDrawStyleEditor.cancelEditing();
         zGridDrawStyleEditor.cancelEditing();
      }

      @Override boolean isEditorForNode(FGraphicNode n)
      {
         FGNodeType nt = (n != null) ? n.getNodeType() : null;
         return(nt == FGNodeType.GRAPH3D || nt == FGNodeType.BACK3D || nt == FGNodeType.GRID3D);
      }
      @Override ImageIcon getRepresentativeIcon() { return(null); }
      @Override String getRepresentativeTitle() { return(null); }


      @Override public void actionPerformed(ActionEvent e)
      {
         if(graph3d == null) return;
         Object src = e.getSource();

         if(src == backDropCombo)
            graph3d.setBackDrop((Graph3DNode.BackDrop) backDropCombo.getSelectedItem());
      }

      /** Combo box selects the 3D graph's backdrop style. */
      private JComboBox<Graph3DNode.BackDrop> backDropCombo = null;
      
     /** Self-contained editor handles the draw style properties for the 3D graph's XY backplane. */
      private DrawStyleEditor xyDrawStyleEditor = null;
      /** Self-contained editor handles the draw style properties for the 3D graph's XZ backplane. */
      private DrawStyleEditor xzDrawStyleEditor = null;
      /** Self-contained editor handles the draw style properties for the 3D graph's YZ backplane. */
      private DrawStyleEditor yzDrawStyleEditor = null;
      /** Self-contained editor handles the draw style properties for the 3D graph's constant-X grid lines. */
      private DrawStyleEditor xGridDrawStyleEditor = null;
      /** Self-contained editor handles the draw style properties for the 3D graph's constant-Y grid lines. */
      private DrawStyleEditor yGridDrawStyleEditor = null;
      /** Self-contained editor handles the draw style properties for the 3D graph's constant-Z grid lines. */
      private DrawStyleEditor zGridDrawStyleEditor = null;
   }

   /**
    * Helper class implements a reconfigurable editor for displaying the properties of any of the three axes of a 3D
    * graph, along with the properties of any tick mark sets defined on the axis. A 3D graph axis is represented by a 
    * {@link Axis3DNode}, while a tick mark set is defined by a {@link Ticks3DNode} child of the axis node. The editor 
    * provides a means of both adding and removing tick sets from the parent axis: Click on the green "+" button in the 
    * editor's top-right corner to append a new tick set, or close any tick set tab to delete the corresponding set.
    * 
    * <p>The editor presents the axis properties in a tab entitled "Axis and Label", while the properties of each tick 
    * set child are displayed on a separate tab entitled "Major Ticks" (for the first tick mark set) or "Ticks N", 
    * where N is the ordinal position of the tick set node in the axis node's child list. Although an axis node can
    * theoretically have any number of tick set child nodes, it is unlikely to have more than one or two, so this tabbed
    * presentation is reasonable.</p>
    * 
    * <p>The editor "cards" that appear in the different tabs are implemented by other inner classes: {@link 
    * Basic3DAxisCard} displays the properties of the 3D axis node itself, while {@link Ticks3DCard} shows the 
    * properties of a single 3D tick set node. <b><i>Note that the 3D axis and tick set nodes are somewhat different
    * than their 2D counterparts.</i></b></p>
    * 
    * <p>To change the type of axis loaded and edited here, call {@link #loadAxis()}.</p>
    * @author sruffner
    */
   private class Axis3DCard extends FGNEditor implements ActionListener, TabStripModel
   {
      /** Construct the 3D graph axis properties editor. */
      Axis3DCard()
      {
         tabStrip = new TabStrip(this);
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
         contentPanel.add(xyzAxisCard, CARD_XYZAXIS);
         contentPanel.add(ticks3DCard, CARD_TICKS);
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
         if(graph3d != null && axis3d != null && e.getSource() == addTickSetBtn)
         {
            boolean ok = axis3d.getGraphicModel().insertNode(axis3d, FGNodeType.TICKS3D, -1);
            if(ok) setSelectedTab(getNumTabs()-1);
         }
      }

      /**
       * Invoke this method to reconfigure the editor to display the specified 3D graph axis.
       * @param which The 3D graph axis identifier. If null, the X-axis is loaded.
       */
      void loadAxis(Graph3DNode.Axis which)
      {
         if(graph3d == null) return;
         if(which == null) which = Graph3DNode.Axis.X;
         
         axis3d = graph3d.getAxis(which);
         
         xyzAxisCard.setEditedAxis(which);

         if(axis3d.getChildCount() > 0)
            ticks3DCard.loadTickSet((Ticks3DNode) axis3d.getChildAt(0));
         
         // switch to the first tab whenever we first load the axis node. We force a tab switch here to ensure that the
         // correct properties card is in front and that the tab strip is updated.
         iSelectedTab = -1;
         setSelectedTab(0);
      }

      @Override void reload(boolean initial)
      {
         xyzAxisCard.reload(initial);
         ticks3DCard.reload(initial);
      }

      @Override void onRaised()
      {
         // delegate to the card that is currently "on top"
         if(axis3d == null) return;
         if(iSelectedTab == 0) xyzAxisCard.onRaised();
         else ticks3DCard.onRaised();
      }

      @Override void onLowered()
      {
         // delegate to the card that is currently "on top"
         if(axis3d == null) return;
         if(iSelectedTab == 0) xyzAxisCard.onLowered();
         else ticks3DCard.onLowered();
      }

      @Override void onInsertSpecialCharacter(String s)
      {
         // delegate to the card that is currently "on top"
         if(axis3d == null) return;
         if(iSelectedTab == 0) xyzAxisCard.onInsertSpecialCharacter(s);
         else ticks3DCard.onInsertSpecialCharacter(s);
      }

      @Override boolean isEditorForNode(FGraphicNode n)
      {
         FGNodeType type = (n != null) ? n.getNodeType() : null;
         return(type == FGNodeType.AXIS3D);
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
      @Override public int getNumTabs() { return(1 + (axis3d == null ? 0 : axis3d.getChildCount())); }

      @Override public int getSelectedTab() { return(iSelectedTab); }

      @Override public void setSelectedTab(int tabPos)
      {
         if(tabPos < 0 || tabPos >= getNumTabs() || tabPos == iSelectedTab) return;
         
         iSelectedTab = tabPos;
         fireStateChanged();

         // if we're switching to a tab corresponding to one of the defined tick sets, make sure that tick set is
         // loaded into the tick set node properties card
         if(iSelectedTab > 0)
            ticks3DCard.loadTickSet( (Ticks3DNode) axis3d.getChildAt(iSelectedTab-1) );
         
         // ensure the correct properties card is brought to the front in the tab content panel
         ((CardLayout) contentPanel.getLayout()).show(contentPanel, iSelectedTab == 0 ? CARD_XYZAXIS : CARD_TICKS);
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
       * First tab shows the 3D graph axis node's properties; its label is always "Axis & Label". Remaining tabs show 
       * properties of each tick set node defined on the axis. The first such tab is specially labelled "Major Ticks", 
       * while the remaining tabs are labelled generically: "Ticks N".
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
         if(axis3d != null && tabPos > 0)
         {
            if(axis3d.getGraphicModel().deleteNode(axis3d.getChildAt(tabPos-1)))
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
      
      /** The 3D graph axis node currently loaded into the axis properties editor. */
      private Axis3DNode axis3d = null;
      
      /** The index of the selected tab. A positive index N corresponds to the (N-1)th tick set child of the axis. */
      private int iSelectedTab = -1;
      
      /** List of all change listeners registered with the tab strip model. */
      private EventListenerList tabListeners = new EventListenerList();

      /** First tab shows the axis properties; then there's one tab for each tick mark set defined on the axis. */
      private TabStrip tabStrip = null;
      /** Clicking this button will append a new tick set node to the axis. */
      private JButton addTickSetBtn = null;
     /** The container for the properties editor currently selected via the tab strip. Uses a card layout. */
      private JPanel contentPanel = null;
      
      /** The properties editor for any 3D graph axis node. */
      private Basic3DAxisCard xyzAxisCard = new Basic3DAxisCard();
      /** The card ID assigned to the basic 3D axis properties editor. */
      private final static String CARD_XYZAXIS = "XYZ Axis";
      
      /** The properties editor for a 3D tick mark set. */
      private Ticks3DCard ticks3DCard = new Ticks3DCard();
      /** The card ID assigned to the 3D tick mark set properties editor. */
      private final static String CARD_TICKS = "Ticks";
   }


   /**
    * Helper class implements the subordinate editor card that may appear in the "Axis & Label" tab of the {@link 
    * Axis3DCard}; it displays/edits the properties of a {@link Axis3DNode}:
    * <ul>
    * <li>Numeric text fields specifying the start and end of the axis range.</li>
    * <li>Custom text fields specifying the measured distance separating the axis line from the nearest edge of the 
    * parent 3D graph's data box, and the distance separating the axis label from the axis line.</li>
    * <li>A check box to show/hide the axis.</li>
    * <li>An {@link StyledTextEditor} to specify edit the axis title, which may contain attributed text and
    * linefeeds.</li>
    * <li>Numeric text field specifying the line height, which affects the spacing between consecutive text lines when
    * the axis' auto-generated label is multi-line.</li>
    * <li>Numeric text field specifying the axis power scale factor.</li>
    * <li>A {@link TextStyleEditor} for editing the node's text-related styles, and a {@link DrawStyleEditor} for its
    * draw-related styles.</li>
    * </ul>
    * 
    * <p>The editor can be reloaded to display the properties of any of the 3D graph's axes. To load a different 3D
    * axis node, call {@link #setEditedAxis()}.</p>
    * 
    * @author sruffner
    */
   private class Basic3DAxisCard extends FGNEditor implements ActionListener
   {
      /** Construct an editor for displaying the properties of a 3D axis node. */
      Basic3DAxisCard()
      {
         whichAxis = Graph3DNode.Axis.X;
         
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
         endField.setToolTipText("<html>Enter end of axis range. <i>For 3D axis, start must be &lt; end</i>.</html>");
         endField.addActionListener(this);
         p.add(endField);
         
         logChk = new JCheckBox("Log?");
         logChk.setToolTipText("If checked, the corresponding graph dimension is logarithmic instead of linear.");
         logChk.setContentAreaFilled(false);
         logChk.addActionListener(this);
         p.add(logChk);
         
         base2Chk = new JCheckBox("Base2?");
         base2Chk.setToolTipText("If checked, axis's logarithmic base is 2 rather than 10 (when applicable)");
         base2Chk.setContentAreaFilled(false);
         base2Chk.addActionListener(this);
         p.add(base2Chk);
         
         hideChk = new JCheckBox("Hide?");
         hideChk.setContentAreaFilled(false);
         hideChk.addActionListener(this);
         p.add(hideChk);
         
         contentEditor = new StyledTextEditor(2);
         contentEditor.addActionListener(this);
         p.add(contentEditor);
         
         JLabel spacerLabel = new JLabel("Gap: ");
         p.add(spacerLabel);
         spacerEditor = new MeasureEditor(0, AxisNode.SPACERCONSTRAINTS);
         spacerEditor.setToolTipText("Enter distance between axis line and nearest edge of 3D graph box (0..2in)");
         spacerEditor.addActionListener(this);
         p.add(spacerEditor);
         
         JLabel offsetLabel = new JLabel("Offset: ");
         p.add(offsetLabel);
         offsetEditor = new MeasureEditor(0, AxisNode.SPACERCONSTRAINTS);
         offsetEditor.setToolTipText("Enter distance between axis line and its label (0..2in)");
         offsetEditor.addActionListener(this);
         p.add(offsetEditor);
         
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


         textStyleEditor = new TextStyleEditor();
         p.add(textStyleEditor);
         drawStyleEditor = new DrawStyleEditor(true, true);
         p.add(drawStyleEditor);
         
         // left-right constraints. Assume row with textStyleEditor will be longest.
         layout.putConstraint(SpringLayout.WEST, startField, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, toLabel, 0, SpringLayout.EAST, startField);
         layout.putConstraint(SpringLayout.WEST, endField, 0, SpringLayout.EAST, toLabel);
         layout.putConstraint(SpringLayout.EAST, hideChk, 0, SpringLayout.EAST, textStyleEditor);
         layout.putConstraint(SpringLayout.EAST, base2Chk, -GAP/2, SpringLayout.WEST, hideChk);
         layout.putConstraint(SpringLayout.EAST, logChk, -GAP/2, SpringLayout.WEST, base2Chk);

         layout.putConstraint(SpringLayout.WEST, contentEditor, 0, SpringLayout.WEST, textStyleEditor);
         layout.putConstraint(SpringLayout.EAST, contentEditor, 0, SpringLayout.EAST, textStyleEditor);
         
         layout.putConstraint(SpringLayout.WEST, offsetLabel, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, offsetEditor, 0, SpringLayout.EAST, offsetLabel);
         layout.putConstraint(SpringLayout.WEST, lineHtLabel, GAP*2, SpringLayout.EAST, offsetEditor);
         layout.putConstraint(SpringLayout.WEST, lineHtField, 0, SpringLayout.EAST, lineHtLabel);

         
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
         layout.putConstraint(vCtr, hideChk, 0, vCtr, startField);
         layout.putConstraint(vCtr, base2Chk, 0, vCtr, startField);
         layout.putConstraint(vCtr, logChk, 0, vCtr, startField);

         layout.putConstraint(vCtr, offsetLabel, 0, vCtr, offsetEditor);
         layout.putConstraint(vCtr, lineHtLabel, 0, vCtr, offsetEditor);
         layout.putConstraint(vCtr, lineHtField, 0, vCtr, offsetEditor);

         layout.putConstraint(vCtr, spacerLabel, 0, vCtr, spacerEditor);
         layout.putConstraint(vCtr, scaleLabel, 0, vCtr, spacerEditor);
         layout.putConstraint(vCtr, scaleField, 0, vCtr, spacerEditor);

         setLayout(new BorderLayout());
         add(p, BorderLayout.NORTH);
      }

      /**
       * Invoke this method to change which 3D graph axis -- X, Y or Z -- is loaded into the editor.
       * @param axis Identifies the axis to load.
       */
      void setEditedAxis(Graph3DNode.Axis axis)
      { 
         whichAxis = (axis == null) ? Graph3DNode.Axis.X : axis;
         reload(true);
      }

      @Override void reload(boolean initial)
      {
         Axis3DNode axis3d = graph3d.getAxis(whichAxis);
         
         startField.setValue(axis3d.getRangeMin());
         endField.setValue(axis3d.getRangeMax());
         logChk.setSelected(axis3d.isLogarithmic());
         base2Chk.setSelected(axis3d.isLogBase2());
         hideChk.setSelected(axis3d.getHide());
         
         contentEditor.loadContent(axis3d.getAttributedTitle(false), axis3d.getFontFamily(), 0, axis3d.getFillColor(), 
               axis3d.getFontStyle());

         spacerEditor.setMeasure(axis3d.getSpacer());
         offsetEditor.setMeasure(axis3d.getLabelOffset());
         lineHtField.setValue(axis3d.getLineHeight());
         scaleField.setValue(axis3d.getPowerScale());
         
         textStyleEditor.loadGraphicNode(axis3d);
         drawStyleEditor.loadGraphicNode(axis3d);
      }

      @Override void onLowered()
      {
         textStyleEditor.cancelEditing();
         drawStyleEditor.cancelEditing();
      }

      /** If the focus is on the label editor, the special character is inserted at the current caret position. */
      @Override void onInsertSpecialCharacter(String s)
      {
         if(contentEditor.isFocusOwner()) contentEditor.insertString(s, false);
      }

      @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && n.getNodeType() == FGNodeType.AXIS3D); }
      @Override ImageIcon getRepresentativeIcon() { return(null); }
      @Override String getRepresentativeTitle() { return(null); }

      @Override public void actionPerformed(ActionEvent e)
      {
         if(graph3d == null) return;
         Object src = e.getSource();
         Axis3DNode axis3d = graph3d.getAxis(whichAxis);

         boolean ok = true;
         if(src == contentEditor)
         {
            String text = FGraphicNode.toStyledText(
                  contentEditor.getCurrentContents(), axis3d.getFontStyle(), axis3d.getFillColor());
            ok = axis3d.setTitle(text);
            if(!ok) contentEditor.loadContent(axis3d.getAttributedTitle(false), axis3d.getFontFamily(), 0, 
                  axis3d.getFillColor(), axis3d.getFontStyle());
         }
         else if(src == hideChk)
            axis3d.setHide(hideChk.isSelected());
         else if(src == base2Chk)
            axis3d.setLogBase2(base2Chk.isSelected());
         else if(src == startField || src == endField || src == logChk)
         {
            // the axis range and log/linear state are always set together
            ok = axis3d.setRange(
                  startField.getValue().doubleValue(), endField.getValue().doubleValue(), logChk.isSelected());
            
            // start and end of range will be silently auto-corrected is start > end or if either is negative when the
            // axis is logarithmic. Go ahead and fix the text fields accordingly.
            if(axis3d.getRangeMin() != startField.getValue().doubleValue())
            {
               startField.setValue(axis3d.getRangeMin());
               endField.setValue(axis3d.getRangeMax());
            }
         }
         else if(src == spacerEditor)
         {
            ok = axis3d.setSpacer(spacerEditor.getMeasure());
            if(!ok) spacerEditor.setMeasure(axis3d.getSpacer());
         }
         else if(src == offsetEditor)
         {
            ok = axis3d.setLabelOffset(offsetEditor.getMeasure());
            if(!ok) offsetEditor.setMeasure(axis3d.getLabelOffset());
         }
         else if(src == lineHtField)
         {
            ok = axis3d.setLineHeight(lineHtField.getValue().doubleValue());
            if(!ok) lineHtField.setValue(axis3d.getLineHeight());
         }
         else if(src == scaleField)
         {
            ok = axis3d.setPowerScale(scaleField.getValue().intValue());
            if(!ok) scaleField.setValue(axis3d.getPowerScale());
         }

         if(!ok) Toolkit.getDefaultToolkit().beep();
      }

      /** Identifies which 3D axis is currently being edited. */
      private Graph3DNode.Axis whichAxis;
      
      /** Numeric text field edits the axis range start. */
      private NumericTextField startField = null;

      /** Numeric text field edits the axis range end. */
      private NumericTextField endField = null;

      /** When checked, axis and corresponding graph dimension are logarithmically instead of linearly scaled. */
      private JCheckBox logChk = null;

      /** When checked, axis's logarithmic base is 2 rather than 10 (applicable only when axis is logarithmic). */
      private JCheckBox base2Chk = null;

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
    * Helper class implements a subordinate node properties editor that appears in a tab of the {@link Axis3DCard} and
    * displays/edits the properties of a {@link Ticks3DNode}:
    * <ul>
    * <li>A text field for entering the tick mark locations in one of three forms as described by {@link 
    * Ticks3DNode#setTickMarkSpec()}.</li>
    * <li>A text field for entering custom tick mark labels as a comma-separated string (optional).</li>
    * <li>Custom text fields specifying the measured length of each tick, and the gap between the end of a tick and the
    * bounds of the corresponding tick label.</li>
    * <li>A multi-choice button to select the tick mark style (inward, outward, bisecting).</li>
    * <li>A combo box specifying the tick mark label format.</li>
    * <li>A {@link TextStyleEditor} for editing the node's text-related styles, and a {@link DrawStyleEditor} for its
    * draw-related styles.</li>
    * </ul>
    * 
    * <p>{@link Axis3DCard} uses a single instance of <b>Ticks3DCard</b> to edit the properties of any tick set node
    * defined on the 3D graph axis, invoking {@link #loadTickSet()} to load a different tick set into the editor.</p>
    * 
    * @author sruffner
    */
   private class Ticks3DCard extends FGNEditor implements ActionListener, ItemListener, FocusListener
   {
      /** Construct an editor for displaying the properties of a 3D tick set node. */
      Ticks3DCard()
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
         startField.setToolTipText("<html>Enter start of tick set range. <i>Must be less than or equal to<br/>"
               + "range end.</i></html>");
         startField.addActionListener(this);
         p.add(startField);
         
         JLabel toLabel = new JLabel(" to ");
         p.add(toLabel);
         endField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 4, -1);
         endField.setToolTipText("<html>Enter end of tick set range. <i>Must be greater than or equal to<br/>"
               + "range start.</i></html>");
         endField.addActionListener(this);
         p.add(endField);
         
         // note: interval must be positive, but we allow 0 anyway. It will be rejected.
         JLabel byLabel = new JLabel(" by ");
         p.add(byLabel);
         intvField = new NumericTextField(0, Double.MAX_VALUE, 4, -1);
         intvField.setToolTipText("<html>Enter strictly positive interval between tick marks. <i>When axis is<br/>"
               + "logarithmic, this is a multiplicative interval and will be auto-corrected to the nearest power<br/>"
               + "of 10 (or 2, if axis log base 2) greater than 1.</i></html>");
         intvField.addActionListener(this);
         p.add(intvField);
                  
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

         JLabel lblsLabel = new JLabel("Labels ");
         p.add(lblsLabel);
         customLabelsField = new JTextField();
         customLabelsField.addActionListener(this);
         customLabelsField.addFocusListener(this);
         customLabelsField.setToolTipText(
               "<html>Enter custom tick mark labels here, comma-separated. Optional. Leave blank to use standard<br/>" +
               "numeric labels.<br/>" +
               "<b>NOTE</b>: Custom labels are applied to tick marks in the order listed. If there are more<br/>" +
               "labels than tick marks, some will be unused; if there are too few, the labels are reused<br/>" +
               "to ensure all tick marks are labeled.</html>");
         p.add(customLabelsField);
         
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
         
         tickOriMB = new MultiButton<TickSetNode.Orientation>();
         tickOriMB.addChoice(TickSetNode.Orientation.OUT, FCIcons.V4_TICKOUT_16, "outward");
         tickOriMB.addChoice(TickSetNode.Orientation.IN, FCIcons.V4_TICKIN_16, "inward");
         tickOriMB.addChoice(TickSetNode.Orientation.THRU, FCIcons.V4_TICKTHRU_16, "bisecting");
         tickOriMB.setToolTipText("Direction of tick marks with respect to axis");
         tickOriMB.addItemListener(this);
         p.add(tickOriMB);

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
         formatCB.setToolTipText("Select tick mark label format");
         formatCB.addActionListener(this);
         p.add(formatCB);
         
         textStyleEditor = new TextStyleEditor();
         p.add(textStyleEditor);
         drawStyleEditor = new DrawStyleEditor(true, true);
         p.add(drawStyleEditor);
         
         // left-right constraints. Assume row containing text style editor is longest.
         layout.putConstraint(SpringLayout.WEST, startField, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, toLabel, 0, SpringLayout.EAST, startField);
         layout.putConstraint(SpringLayout.WEST, endField, 0, SpringLayout.EAST, toLabel);
         layout.putConstraint(SpringLayout.WEST, byLabel, 0, SpringLayout.EAST, endField);
         layout.putConstraint(SpringLayout.WEST, intvField, 0, SpringLayout.EAST, byLabel);

         layout.putConstraint(SpringLayout.WEST, lblsLabel, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, customLabelsField, 0, SpringLayout.EAST, lblsLabel);
         layout.putConstraint(SpringLayout.EAST, customLabelsField, 0, SpringLayout.EAST, p);

         // the log-decade check boxes are arranged in a single row.
         layout.putConstraint(SpringLayout.WEST, logTickCheckBoxes[0], 0, SpringLayout.WEST, p);
         for(int i=1; i<logTickCheckBoxes.length; i++)
            layout.putConstraint(SpringLayout.WEST, logTickCheckBoxes[i], 0, SpringLayout.EAST, logTickCheckBoxes[i-1]);

         layout.putConstraint(SpringLayout.WEST, lenLabel, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.WEST, lengthEditor, 0, SpringLayout.EAST, lenLabel);
         layout.putConstraint(SpringLayout.WEST, gapLabel, GAP, SpringLayout.EAST, lengthEditor);
         layout.putConstraint(SpringLayout.WEST, gapEditor, 0, SpringLayout.EAST, gapLabel);
         layout.putConstraint(SpringLayout.WEST, tickOriMB, GAP*2, SpringLayout.EAST, gapEditor);
         layout.putConstraint(SpringLayout.WEST, formatCB, GAP, SpringLayout.EAST, tickOriMB);

         layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, p);
         layout.putConstraint(SpringLayout.EAST, p , 0, SpringLayout.EAST, textStyleEditor);
         layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, p);
         
         // top-bottom constraints: A widget in each row is used to set the constraints with row above or below. The 
         // remaining widgets are vertically centered WRT that widget.
         layout.putConstraint(SpringLayout.NORTH, startField, 0, SpringLayout.NORTH, p);
         layout.putConstraint(SpringLayout.NORTH, logTickCheckBoxes[0], GAP, SpringLayout.SOUTH, startField);
         layout.putConstraint(SpringLayout.NORTH, customLabelsField, GAP, SpringLayout.SOUTH, logTickCheckBoxes[0]);
         layout.putConstraint(SpringLayout.NORTH, tickOriMB, GAP, SpringLayout.SOUTH, customLabelsField);
         layout.putConstraint(SpringLayout.NORTH, textStyleEditor, GAP*3, SpringLayout.SOUTH, tickOriMB);
         layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP*2, SpringLayout.SOUTH, textStyleEditor);
         layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, drawStyleEditor);
         
         String vc = SpringLayout.VERTICAL_CENTER;
         layout.putConstraint(vc, toLabel, 0, vc, startField);
         layout.putConstraint(vc, endField, 0, vc, startField);
         layout.putConstraint(vc, byLabel, 0, vc, startField);
         layout.putConstraint(vc, intvField, 0, vc, startField);
         layout.putConstraint(vc, lblsLabel, 0, vc, customLabelsField);
         layout.putConstraint(vc, lenLabel, 0, vc, tickOriMB);
         layout.putConstraint(vc, lengthEditor, 0, vc, tickOriMB);
         layout.putConstraint(vc, gapLabel, 0, vc, tickOriMB);
         layout.putConstraint(vc, gapEditor, 0, vc, tickOriMB);
         layout.putConstraint(vc, formatCB, 0, vc, tickOriMB);

         for(int i=1; i<logTickCheckBoxes.length; i++)
            layout.putConstraint(vc, logTickCheckBoxes[i], 0, vc, logTickCheckBoxes[0]);
         
         setLayout(new BorderLayout());
         add(p, BorderLayout.NORTH);
      }

      /**
       * Invoke this method to load a different 3D tick mark set into the editor.
       * @param n The 3D tick mark set to be edited. If null, no action is taken.
       */
      void loadTickSet(Ticks3DNode n)
      {
         if(n == null) return;
         ticks = n;
         reload(true);
      }
      
      @Override void reload(boolean initial)
      {
         if(ticks == null) return;
         
         startField.setValue(ticks.getStart());
         endField.setValue(ticks.getEnd());
         intvField.setValue(ticks.getInterval());
         customLabelsField.setText(ticks.getCustomTickLabelsAsCommaSeparatedList());
         
         LogTickPattern ltp = ticks.getDecadeTicks();
         for(int i=0; i<logTickCheckBoxes.length; i++) logTickCheckBoxes[i].setSelected(ltp.isTickEnabledAt(i+1));
         
         lengthEditor.setMeasure(ticks.getTickLength());
         gapEditor.setMeasure(ticks.getTickGap());
         
         tickOriMB.setCurrentChoice(ticks.getTickOrientation());
         
         formatCB.removeActionListener(this);
         formatCB.setSelectedItem(ticks.getTickLabelFormat());
         formatCB.addActionListener(this);
         
         textStyleEditor.loadGraphicNode(ticks);
         drawStyleEditor.loadGraphicNode(ticks);
      }
      
      @Override void onLowered()
      {
         textStyleEditor.cancelEditing();
         drawStyleEditor.cancelEditing();
      }

      @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && n.getNodeType() == FGNodeType.TICKS3D); }
      @Override ImageIcon getRepresentativeIcon() { return(null); }
      @Override String getRepresentativeTitle() { return(null); }

      @Override public void focusGained(FocusEvent e) 
      {
         Object src = e.getSource();
         if(src.getClass().equals(JTextField.class) &&  !e.isTemporary())
            ((JTextField)src).selectAll();
      }
      @Override public void focusLost(FocusEvent e)
      {
         Object src = e.getSource();
         if(src.getClass().equals(JTextField.class) &&  !e.isTemporary())
            ((JTextField)src).postActionEvent();
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
            ok = true;
         }
         else if(LOGTICKCMD.equals(cmd))
         {
            int enaLogTicks = 0;
            for(int i=0; i<logTickCheckBoxes.length; i++) 
               if(logTickCheckBoxes[i].isSelected())
                  enaLogTicks |= (1<<(i+1));
            ticks.setDecadeTicks(new LogTickPattern(enaLogTicks));
         }

         if(!ok) Toolkit.getDefaultToolkit().beep();
      }
      
      @Override public void itemStateChanged(ItemEvent e)
      {
         if(ticks == null || e.getSource() != tickOriMB) return;
         ticks.setTickOrientation(tickOriMB.getCurrentChoice());
      }
      
      private Ticks3DNode ticks = null;
      
      /** Numeric text field edits the tick set range start. */
      private NumericTextField startField = null;

      /** Numeric text field edits the tick set range end. */
      private NumericTextField endField = null;

      /** Numeric text field edits the tick mark interval. */
      private NumericTextField intvField = null;

      /** Array of 9 check boxes to choose where tick marks appear within each logarithmic decade (log axis only). */
      private JCheckBox[] logTickCheckBoxes = null;

      /** Shared action command from the check boxes which choose where ticks appear in a log decade. */
      private final static String LOGTICKCMD = "LogTick";

     /** 
       * Displays/edits custom tick mark labels as a comma-separated list of label tokens. If empty, then standard
       * numeric tick labels are generated.
       */
      private JTextField customLabelsField = null;
      
      /** Customized component for editing the tick mark length. */
      private MeasureEditor lengthEditor = null;

      /** Customized component for editing the size of the gap between tick marks and corresponding labels. */
      private MeasureEditor gapEditor = null;
      
      /** Edits the tick mark orientation (in, out, or thru). */
      private MultiButton<TickSetNode.Orientation> tickOriMB = null; 

      /** Button that selects the tick mark label format. */
      private JComboBox<LabelFormat> formatCB = null;

      /** Self-contained editor handles the node's text styles. */
      private TextStyleEditor textStyleEditor = null;
      
      /** Self-contained editor handles the node's draw style properties. */
      private DrawStyleEditor drawStyleEditor = null;
   }

}
