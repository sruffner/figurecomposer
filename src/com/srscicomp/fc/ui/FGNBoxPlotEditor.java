package com.srscicomp.fc.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.common.ui.TabStrip;
import com.srscicomp.common.ui.TabStripModel;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.SymbolNode;
import com.srscicomp.fc.fig.ViolinStyleNode;
import com.srscicomp.fc.fig.BoxPlotNode;
import com.srscicomp.fc.uibase.StyledTextEditor;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
 * <b>FGNBoxPlotEditor</b> displays/edits all properties of a box plot data presentation node, {@link BoxPlotNode}. 
 * 
 * <p>The node's title is entered in a plain text field; that title will appear in the graph legend if the adjacent 
 * check box is checked. On the next row is an embedded {@link FGNPlottableDSCard}, which exposes the ID of the data set
 * for the box plot, and provides facilities for viewing and/or editing that raw data set. Below this are widgets for 
 * specifying the display mode, the box width, the offset to the midpoint of the first box plot, and the interval 
 * separating the midpoints of adjacent box plots (one box plot is drawn for each individual data set in the node's 
 * data source).</p>
 * <p>The bottom portion of the editor panel is occupied by a three-page tabbed pane for editing the graphical styling 
 * of the box plots. The <i>Box</i> tab holds a {@link DrawStyleEditor}, which displays and edits the box plot node's 
 * draw styles -- these govern the appearance of the "box" portion of each box plot. The <i>Whiskers</i> tab is a
 * {@link FGNEBarCard}, which encapsulates widgets for editing the properties of the box plot element's component that
 * governs the appearance of the box plot whiskers. Finally, the <i>Outliers</i> tab is a {@link FGNSymbolCard}, which
 * encapsulates widgets for editing all properties of the component {@link SymbolNode}, governing the appearance of the
 * symbols marking the locations of any outlier data in the box plots. We use {@link TabStrip} to implement the
 * tabbed-pane interface.</p>
 * 
 * @author sruffner
 */
class FGNBoxPlotEditor extends FGNEditor implements TabStripModel, ActionListener
{

   /** Construct the trace node properties editor. */
   FGNBoxPlotEditor()
   {
      showInLegendChk = new JCheckBox();
      showInLegendChk.setToolTipText("Check this box to include an entry for this box plot in the graph legend");
      showInLegendChk.setContentAreaFilled(false);
      showInLegendChk.addActionListener(this);
      add(showInLegendChk);
      
      titleEditor = new StyledTextEditor(1);
      titleEditor.setToolTipText("Enter title here (it will be used as the label for the node's legend entry)");
      titleEditor.addActionListener(this);
      add(titleEditor);
      
      dsEditor = new FGNPlottableDSCard();
      add(dsEditor);

      modeCombo = new JComboBox<>(BoxPlotNode.DisplayMode.values());
      modeCombo.addActionListener(this);
      modeCombo.setToolTipText("Select display mode");
      add(modeCombo);
      
      JLabel boxWidthLabel = new JLabel("Width: ");
      add(boxWidthLabel);
      boxWidthEditor = new MeasureEditor(10, BoxPlotNode.BOXWIDTHCONSTRAINTS);
      boxWidthEditor.setToolTipText("Enter box width in in/cm/mm/pt or as % of the graph width or height");
      boxWidthEditor.addActionListener(this);
      add(boxWidthEditor);
      
      JLabel locLabel = new JLabel("X0/DX: ");
      add(locLabel);
      offsetField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      offsetField.setColumns(4);
      offsetField.setToolTipText("Enter box plot offset along X-axis (or Y if horizontal) in user units.");
      offsetField.addActionListener(this);
      add(offsetField);
      intvField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      intvField.setColumns(4);
      intvField.setToolTipText("Enter box plot separation interval along X-axis (or Y if horizontal) in user units.");
      intvField.addActionListener(this);
      add(intvField);
      
      // set up the tabbed pane interface to edit the graphic styles of the box plot node itself (governs appearance of
      // the box portion of box plots), its component error bar node (governs appearance of whiskers), symbol node
      // (governs appearance of outlier symbols), and violin style node (appearance of companion violin plot).
      drawStyleEditor = new DrawStyleEditor(false, false);
      whiskerCard = new FGNEBarCard();
      outlierCard = new FGNSymbolCard(true, false);
      violinCard = new ViolinCard();
      
      
      TabStrip tStrip = new TabStrip(this);
      add(tStrip);
      
      tabContent = new JPanel(new CardLayout());
      tabContent.setOpaque(false);
      tabContent.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 2, 2, 2, tStrip.getSelectionColor()), 
            BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP)
      ));
      add(tabContent);
      
      JPanel boxPanel = new JPanel(new BorderLayout());
      boxPanel.setOpaque(false);
      boxPanel.add(drawStyleEditor, BorderLayout.NORTH);

      JPanel whiskerPanel = new JPanel(new BorderLayout());
      whiskerPanel.setOpaque(false);
      whiskerPanel.add(whiskerCard, BorderLayout.NORTH);

      JPanel outlierPanel = new JPanel(new BorderLayout());
      outlierPanel.setOpaque(false);
      outlierPanel.add(outlierCard, BorderLayout.NORTH);
      
      JPanel violinPanel = new JPanel(new BorderLayout());
      violinPanel.setOpaque(false);
      violinPanel.add(violinCard, BorderLayout.NORTH);

      tabContent.add(boxPanel, TAB_BOX);
      tabContent.add(whiskerPanel, TAB_WHISKERS);
      tabContent.add(outlierPanel, TAB_OUTLIERS);
      tabContent.add(violinPanel, TAB_VIOLIN);
      
      // now layout the widgets, tab strip, and tab content panel in rows.
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. Assume the 3rd row is the longest.
      layout.putConstraint(SpringLayout.WEST, showInLegendChk, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, titleEditor, GAP, SpringLayout.EAST, showInLegendChk);
      layout.putConstraint(SpringLayout.EAST, titleEditor, 0, SpringLayout.EAST, intvField);

      layout.putConstraint(SpringLayout.WEST, dsEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, dsEditor, 0, SpringLayout.EAST, intvField);

      layout.putConstraint(SpringLayout.WEST, modeCombo, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, boxWidthLabel, GAP*2, SpringLayout.EAST, modeCombo);
      layout.putConstraint(SpringLayout.WEST, boxWidthEditor, 0, SpringLayout.EAST, boxWidthLabel);
      layout.putConstraint(SpringLayout.WEST, locLabel, GAP*2, SpringLayout.EAST, boxWidthEditor);
      layout.putConstraint(SpringLayout.WEST, offsetField, 0, SpringLayout.EAST, locLabel);
      layout.putConstraint(SpringLayout.WEST, intvField, GAP/2, SpringLayout.EAST, offsetField);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, intvField);

      layout.putConstraint(SpringLayout.WEST, tStrip, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, tStrip, 0, SpringLayout.EAST, intvField);
      layout.putConstraint(SpringLayout.WEST, tabContent, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, tabContent, 0, SpringLayout.EAST, intvField);
      

      // top-bottom constraints: 4 rows, with tab strip and content pane occupying the last two. One widget per row sets
      // constraints with row above or below. Remaining widgets in a row are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, titleEditor, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, dsEditor, GAP*2, SpringLayout.SOUTH, titleEditor);
      layout.putConstraint(SpringLayout.NORTH, modeCombo, GAP, SpringLayout.SOUTH, dsEditor);
      layout.putConstraint(SpringLayout.NORTH, tStrip, GAP*2, SpringLayout.SOUTH, modeCombo);
      layout.putConstraint(SpringLayout.NORTH, tabContent, 0, SpringLayout.SOUTH, tStrip);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, tabContent);
      
      String vCtr = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vCtr, showInLegendChk, 0, vCtr, titleEditor);
      layout.putConstraint(vCtr, boxWidthLabel, 0, vCtr, modeCombo);
      layout.putConstraint(vCtr, boxWidthEditor, 0, vCtr, modeCombo);
      layout.putConstraint(vCtr, locLabel, 0, vCtr, modeCombo);
      layout.putConstraint(vCtr, offsetField, 0, vCtr, modeCombo);
      layout.putConstraint(vCtr, intvField, 0, vCtr, modeCombo);
   }

   @Override public void reload(boolean initial)
   {
      BoxPlotNode box = getBoxPlotNode();
      if(box == null) return;
      
      titleEditor.loadContent(box.getAttributedTitle(false), box.getFontFamily(), 0, box.getFillColor(), 
            box.getFontStyle());
      showInLegendChk.setSelected(box.getShowInLegend());
      
      dsEditor.reload(box);
      
      modeCombo.setSelectedItem(box.getMode());
      boxWidthEditor.setMeasure(box.getBoxWidth());
      offsetField.setValue(box.getOffset());
      intvField.setValue(box.getInterval());

      drawStyleEditor.loadGraphicNode(box);
      if(initial)
      {
         whiskerCard.reload(box.getWhiskerNode());
         outlierCard.reload(box.getSymbolNode());
         violinCard.reload(box.getViolinStyleNode());
         setSelectedTab(0);
      }
      else
      {
         whiskerCard.reload(null);
         outlierCard.reload(null);
         violinCard.reload(null);
      }
   }

   /** Cancel any modeless pop-up windows associated with the editors embedded in this {@link FGNBoxPlotEditor}. */
   @Override void onLowered()
   {
      drawStyleEditor.cancelEditing();
      whiskerCard.cancelEditing();
      outlierCard.cancelEditing();
      violinCard.cancelEditing();
   }

   /**
    * If the focus is on the title field, then the special character is inserted at the current caret position in that
    * text field. The user will still have to hit the "Enter" key to submit the change.
    */
   @Override void onInsertSpecialCharacter(String s)
   {
      if(titleEditor.isFocusOwner()) titleEditor.insertString(s, false);
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.BOX == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_BOX_32); }
   @Override String getRepresentativeTitle() { return("Box Plot Properties"); }

   @Override public int getNumTabs() { return(4); }
   @Override public int getSelectedTab() { return(iSelectedTab); }
   @Override public void setSelectedTab(int tabPos)
   {
      if(tabPos < 0 || tabPos >= getNumTabs() || tabPos == iSelectedTab) return;

      iSelectedTab = tabPos;
      fireStateChanged();
      
      CardLayout cl = (CardLayout) tabContent.getLayout();
      String tabName;
      switch(iSelectedTab)
      {
      case 1: tabName = TAB_WHISKERS; break;
      case 2: tabName = TAB_OUTLIERS; break;
      case 3: tabName = TAB_VIOLIN; break;
      default: tabName = TAB_BOX;
      }
      cl.show(tabContent, tabName);
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
      return(tabPos==0 ? TAB_BOX : (tabPos==1 ? TAB_WHISKERS : (tabPos==2 ? TAB_OUTLIERS : TAB_VIOLIN))); 
   }
   @Override public String getTabToolTip(int tabPos) { return(null); }
   @Override public Icon getTabIcon(int tabPos) { return(null); }
   @Override public boolean isTabClosable(int tabPos) { return(false); }
   @Override public String getCloseTabToolTip(int tabPos) { return(null); }
   @Override public void closeTab(int tabPos) {}
   @Override public boolean supportsTabRepositioning() { return(false); }
   @Override public boolean moveTab(int fromPos, int toPos) { return(false); }

   /** The currently selected tab position. */
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
   
   @Override public void actionPerformed(ActionEvent e)
   {
      BoxPlotNode box = getBoxPlotNode();
      if(box == null) return;
      Object src = e.getSource();

      boolean ok = true;
      if(src == titleEditor)
      {
         String text = FGraphicNode.toStyledText(
               titleEditor.getCurrentContents(), box.getFontStyle(), box.getFillColor());
         ok = box.setTitle(text);
         if(!ok) titleEditor.loadContent(box.getAttributedTitle(false), box.getFontFamily(), 0, 
               box.getFillColor(), box.getFontStyle());
      }
      else if(src == showInLegendChk)
         box.setShowInLegend(showInLegendChk.isSelected());
      else if(src == modeCombo)
      {
          ok = box.setMode((BoxPlotNode.DisplayMode) modeCombo.getSelectedItem());
          if(!ok) modeCombo.setSelectedItem(box.getMode());
      }
      else if(src == offsetField)
      {
         ok = box.setOffset(offsetField.getValue().floatValue());
         if(!ok) offsetField.setValue(box.getOffset());
      }
      else if(src == intvField)
      {
         ok = box.setInterval(intvField.getValue().floatValue());
         if(!ok) intvField.setValue(box.getInterval());
      }
      else if(src == boxWidthEditor)
      {
         ok = box.setBoxWidth(boxWidthEditor.getMeasure());
         if(!ok) boxWidthEditor.setMeasure(box.getBoxWidth());
      }

      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link BoxPlotNode}. */
   private BoxPlotNode getBoxPlotNode() { return((BoxPlotNode) getEditedNode()); }
   
   /** When checked, an entry for the data trace is included in parent graph's legend. */
   private final JCheckBox showInLegendChk;

   /** The custom widget within which the box plot node's title (which may contain attributed text) is edited.  */
   private StyledTextEditor titleEditor = null;

   /** Self-contained editor handles editing/loading of the data set assigned to the trace. */
   private FGNPlottableDSCard dsEditor = null;
   
   /** Combo box selects the trace display mode. */
   private JComboBox<BoxPlotNode.DisplayMode> modeCombo = null;

   /** Customized component for editing the box plot width. */
   private MeasureEditor boxWidthEditor = null;
   
   /** Numeric text field sets box plot offset along X-axis (or Y-axis in horizontal display) in user units. */
   private NumericTextField offsetField = null;
   
   /** Numeric text field sets box plot separation along X-axis (or Y-axis in horizontal display) in user units. */
   private NumericTextField intvField = null;
   
   /** Self-contained editor handles the box plot node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** The editor for the box plot's component error bar node, which encapsulates styling of the box plot whiskers. */
   private FGNEBarCard whiskerCard = null;
   
   /** The editor for the box plot's component symbol node, which encapsulates styling of the box plot outliers. */
   private FGNSymbolCard outlierCard = null;
   
   /** Editor for the box plot's component violin style node, encapsulating styles of the companion violin plot. */
   private ViolinCard violinCard = null;
   
   /** The title of the tab in which box plot node's styling properties are shown. */
   private final static String TAB_BOX = "Box";
   /** The title of the tab in which box plot whisker styling properties are shown. */
   private final static String TAB_WHISKERS = "Whiskers";
   /** The title of the tab in which box plot outlier symbol properties are shown. */
   private final static String TAB_OUTLIERS = "Outliers";
   /** The title of the tab in which the violin plot properties are shown. */
   private final static String TAB_VIOLIN = "Violin Plot";
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
   
   
   /**
    * <b>ViolinCard</b> displays/edits all properties of the component {@link ViolinStyleNode} of a {@link BoxPlotNode}.
    * The editor includes a widget to specify the violin plot width and an embedded {@link DrawStyleEditor} that 
    * exposes the violin's draw styles.
    */
   static class ViolinCard extends JPanel implements ActionListener
   {
      /** Construct the violin plot style properties editor. */
      ViolinCard()
      {
         super();
         setOpaque(false);
         
         JLabel wLabel = new JLabel("Width: ");
         add(wLabel);
         widthEditor = new MeasureEditor(0, BoxPlotNode.BOXWIDTHCONSTRAINTS);
         widthEditor.setToolTipText("Enter box width in in/cm/mm/pt or as % of the graph width or height (0 to hide)");
         widthEditor.addActionListener(this);
         add(widthEditor);

         drawStyleEditor = new DrawStyleEditor(false, false);
         add(drawStyleEditor);
         
         SpringLayout layout = new SpringLayout();
         setLayout(layout);
         
         // left-right constraints. Assume widest row is the one containing the draw style editor. 
         layout.putConstraint(SpringLayout.WEST, wLabel, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, widthEditor, 0, SpringLayout.EAST, wLabel);
         
         layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, drawStyleEditor);

         // top-bottom constraints: One widget in each row is used to set the constraints with row above or below. The 
         // remaining widgets are vertically centered WRT that widget.
         layout.putConstraint(SpringLayout.NORTH, widthEditor, GAP, SpringLayout.NORTH, this);
         layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, widthEditor);
         layout.putConstraint(SpringLayout.SOUTH, this, GAP, SpringLayout.SOUTH, drawStyleEditor);
         
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, wLabel, 0, SpringLayout.VERTICAL_CENTER, widthEditor);
      }

      public void reload(ViolinStyleNode vsn)
      {
         if(vsn != null) violin = vsn;
         if(violin == null) return;
         
         widthEditor.setMeasure(violin.getSize());
         drawStyleEditor.loadGraphicNode(violin);
      }

      /** Ensure the modeless pop-up windows associated with any widgets in this editor are extinguished. */
      void cancelEditing() { drawStyleEditor.cancelEditing(); }

      @Override public void actionPerformed(ActionEvent e)
      {
         if(violin == null) return;
         Object src = e.getSource();

         if(src == widthEditor)
         {
            if(!violin.setSize(widthEditor.getMeasure()))
            {
               Toolkit.getDefaultToolkit().beep();
               widthEditor.setMeasure(violin.getSize());
            }
         }
      }

      /** The violin style node being edited. If null, editor is non-functional. */
      private ViolinStyleNode violin = null;

      /** Customized component for editing the violin plot's width. */
      private final MeasureEditor widthEditor;
      
      /** Self-contained editor handles the node's draw style properties. */
      private DrawStyleEditor drawStyleEditor = null;
   }
}
