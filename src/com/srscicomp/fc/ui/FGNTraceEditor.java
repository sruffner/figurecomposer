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
import com.srscicomp.fc.fig.TraceNode;
import com.srscicomp.fc.uibase.StyledTextEditor;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * <code>FGNTraceEditor</code> displays/edits all properties of a {@link TraceNode}.
 * 
 * <p>The trace node's title is entered in a plain text field; that title will appear in the graph legend if the 
 * adjacent check box is checked. On the next row is an embedded {@link FGNPlottableDSCard}, which exposes the ID of the
 * data set referenced by the trace node, and provides facilities for viewing and/or editing that raw data set. Below 
 * this are widgets for specifying the trace's display mode, X- and Y-offset, plot skip interval, bar width and baseline
 * values for histogram mode, and the <i>Average?</i> flag.</p>
 * <p>The bottom portion of the editor panel is occupied by a three-page tabbed pane for editing the graphical styling 
 * of the rendered trace. The <i>Line</i> tab holds a {@link TextStyleEditor} and a {@link DrawStyleEditor}, which 
 * display the trace node's own style properties -- these govern the appearance of the polyline that connects the data
 * points in the trace. The <i>Symbols</i> tab is a {@link FGNEBarCard}, which encapsulates widgets for editing all 
 * properties of the trace node's component {@link com.srscicomp.fc.fig.SymbolNode}, and the <i>Error Bars</i> tab is 
 * a similar panel (implemented by an inner class) encapsulating widgets for the trace node's component {@link 
 * com.srscicomp.fc.fig.ErrorBarNode}: the error bar endcap symbol and size, a "hide all error bars" flag, and the
 * error bar draw styling (which may be different from the draw styles for the trace node itself). We use {@link 
 * TabStrip TabStrip} and a content panel with a {@link CardLayout} manager to implement the
 * tabbed-pane interface.</p>
 * 
 * @author sruffner
 */
class FGNTraceEditor extends FGNEditor implements TabStripModel, ActionListener
{

   /** Construct the trace node properties editor. */
   FGNTraceEditor()
   {
      showInLegendChk = new JCheckBox();
      showInLegendChk.setToolTipText("Check this box to include entry in legend for this data trace");
      showInLegendChk.setContentAreaFilled(false);
      showInLegendChk.addActionListener(this);
      add(showInLegendChk);
      
      titleEditor = new StyledTextEditor(1);
      titleEditor.setToolTipText("Enter title here (it will be used as the label for the node's legend entry)");
      titleEditor.addActionListener(this);
      add(titleEditor);
      
      dsEditor = new FGNPlottableDSCard();
      add(dsEditor);

      modeCombo = new JComboBox<>(TraceNode.DisplayMode.values());
      modeCombo.addActionListener(this);
      modeCombo.setToolTipText("Select trace display mode");
      add(modeCombo);
      
      JLabel skipLabel = new JLabel("every ");
      add(skipLabel);
      skipIntvField = new NumericTextField(1, Integer.MAX_VALUE);
      skipIntvField.setColumns(4);
      skipIntvField.setToolTipText("Enter N > 0; every Nth data point is plotted.");
      skipIntvField.addActionListener(this);
      add(skipIntvField);

      JLabel lenLabel = new JLabel("window ");
      add(lenLabel);
      lenField = new NumericTextField(1, Integer.MAX_VALUE);
      lenField.setColumns(3);
      lenField.setToolTipText("<html>Window length N > 1 for sliding average trend line. <br>If N=1, LMS regression "
            + "line is rendered instead.</html>");
      lenField.addActionListener(this);
      add(lenField);
      
      avgChk = new JCheckBox("Average?");
      avgChk.setToolTipText(
            "<html>Check to enable display of average trace <br>(<i>multitrace mode only</i>)</html>");
      avgChk.setContentAreaFilled(false);
      avgChk.addActionListener(this);
      add(avgChk);
      
      JLabel x0Label = new JLabel("x0 ");
      add(x0Label);
      xOffsetField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      xOffsetField.setColumns(5);
      xOffsetField.setToolTipText("X (or Theta) Offset");
      xOffsetField.addActionListener(this);
      add(xOffsetField);
      
      JLabel y0Label = new JLabel("y0 ");
      add(y0Label);
      yOffsetField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      yOffsetField.setColumns(5);
      yOffsetField.setToolTipText("Y (or Radial) Offset");
      yOffsetField.addActionListener(this);
      add(yOffsetField);
      
      JLabel barWLabel = new JLabel("barW ");
      add(barWLabel);
      barWidthField = new NumericTextField(0, Double.MAX_VALUE, 3);
      barWidthField.setColumns(5);
      barWidthField.setToolTipText("Enter histogram bar width (non-negative)");
      barWidthField.addActionListener(this);
      add(barWidthField);
      
      JLabel baseLabel = new JLabel("base ");
      add(baseLabel);
      baselineField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      baselineField.setColumns(5);
      baselineField.setToolTipText("Enter histogram baseline");
      baselineField.addActionListener(this);
      add(baselineField);
      
      // set up the tabbed pane interface to access the styling properties of the trace node itself, its component
      // symbol node, and its component error bar node.
      textStyleEditor = new TextStyleEditor();
      drawStyleEditor = new DrawStyleEditor(true, false);
      symbolEditor = new FGNSymbolCard();
      ebarEditor = new FGNEBarCard();
      
      TabStrip tStrip = new TabStrip(this);
      add(tStrip);
      
      tabContent = new JPanel(new CardLayout());
      tabContent.setOpaque(false);
      tabContent.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 2, 2, 2, tStrip.getSelectionColor()), 
            BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP)
      ));
      add(tabContent);
      
      JPanel polylinePanel = new JPanel(new BorderLayout());
      polylinePanel.setOpaque(false);
      
      JPanel p = new JPanel();
      p.setOpaque(false);
      p.add(textStyleEditor);
      p.add(drawStyleEditor);
      SpringLayout layout = new SpringLayout();
      p.setLayout(layout);
      layout.putConstraint(SpringLayout.WEST, textStyleEditor, 0, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.NORTH, textStyleEditor, 0, SpringLayout.NORTH, p);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, textStyleEditor);
      layout.putConstraint(SpringLayout.SOUTH, p, 0, SpringLayout.SOUTH, drawStyleEditor);
      polylinePanel.add(p, BorderLayout.NORTH);

      JPanel symbolPanel = new JPanel(new BorderLayout());
      symbolPanel.setOpaque(false);
      symbolPanel.add(symbolEditor, BorderLayout.NORTH);

      JPanel ebarPanel = new JPanel(new BorderLayout());
      ebarPanel.setOpaque(false);
      ebarPanel.add(ebarEditor, BorderLayout.NORTH);

      tabContent.add(polylinePanel, TAB_LINE);
      tabContent.add(symbolPanel, TAB_SYMBOLS);
      tabContent.add(ebarPanel, TAB_EBARS);
      
      // now layout the widgets, tab strip, and tab content panel in rows.
      layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. Assume the 4th row is the longest.
      layout.putConstraint(SpringLayout.WEST, showInLegendChk, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, titleEditor, GAP, SpringLayout.EAST, showInLegendChk);
      layout.putConstraint(SpringLayout.EAST, titleEditor, 0, SpringLayout.EAST, baselineField);

      layout.putConstraint(SpringLayout.WEST, dsEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, dsEditor, 0, SpringLayout.EAST, baselineField);

      layout.putConstraint(SpringLayout.WEST, modeCombo, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, skipLabel, GAP*3, SpringLayout.EAST, modeCombo);
      layout.putConstraint(SpringLayout.WEST, skipIntvField, 0, SpringLayout.EAST, skipLabel);
      layout.putConstraint(SpringLayout.EAST, avgChk, 0, SpringLayout.EAST, baselineField);
      layout.putConstraint(SpringLayout.EAST, lenField, -GAP, SpringLayout.WEST, avgChk);
      layout.putConstraint(SpringLayout.EAST, lenLabel, 0, SpringLayout.WEST, lenField);

      layout.putConstraint(SpringLayout.WEST, x0Label, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, xOffsetField, 0, SpringLayout.EAST, x0Label);
      layout.putConstraint(SpringLayout.WEST, y0Label, GAP, SpringLayout.EAST, xOffsetField);
      layout.putConstraint(SpringLayout.WEST, yOffsetField, 0, SpringLayout.EAST, y0Label);
      layout.putConstraint(SpringLayout.WEST, barWLabel, GAP, SpringLayout.EAST, yOffsetField);
      layout.putConstraint(SpringLayout.WEST, barWidthField, 0, SpringLayout.EAST, barWLabel);
      layout.putConstraint(SpringLayout.WEST, baseLabel, GAP, SpringLayout.EAST, barWidthField);
      layout.putConstraint(SpringLayout.WEST, baselineField, 0, SpringLayout.EAST, baseLabel);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, baselineField);
      
      layout.putConstraint(SpringLayout.WEST, tStrip, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, tStrip, 0, SpringLayout.EAST, baselineField);
      layout.putConstraint(SpringLayout.WEST, tabContent, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, tabContent, 0, SpringLayout.EAST, baselineField);
      

      // top-bottom constraints: 5 rows, with tab strip and content pane occupying the last two. One widget per row sets
      // constraints with row above or below. Remaining widgets in a row are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, titleEditor, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, dsEditor, GAP*2, SpringLayout.SOUTH, titleEditor);
      layout.putConstraint(SpringLayout.NORTH, modeCombo, GAP, SpringLayout.SOUTH, dsEditor);
      layout.putConstraint(SpringLayout.NORTH, xOffsetField, GAP, SpringLayout.SOUTH, modeCombo);
      layout.putConstraint(SpringLayout.NORTH, tStrip, GAP*2, SpringLayout.SOUTH, xOffsetField);
      layout.putConstraint(SpringLayout.NORTH, tabContent, 0, SpringLayout.SOUTH, tStrip);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, tabContent);
      
      String vCtr = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vCtr, showInLegendChk, 0, vCtr, titleEditor);
      layout.putConstraint(vCtr, skipLabel, 0, vCtr, modeCombo);
      layout.putConstraint(vCtr, skipIntvField, 0, vCtr, modeCombo);
      layout.putConstraint(vCtr, avgChk, 0, vCtr, modeCombo);
      layout.putConstraint(vCtr, lenLabel, 0, vCtr, modeCombo);
      layout.putConstraint(vCtr, lenField, 0, vCtr, modeCombo);
      
      layout.putConstraint(vCtr, x0Label, 0, vCtr, xOffsetField);
      layout.putConstraint(vCtr, y0Label, 0, vCtr, xOffsetField);
      layout.putConstraint(vCtr, yOffsetField, 0, vCtr, xOffsetField);
      layout.putConstraint(vCtr, barWLabel, 0, vCtr, xOffsetField);
      layout.putConstraint(vCtr, barWidthField, 0, vCtr, xOffsetField);
      layout.putConstraint(vCtr, baseLabel, 0, vCtr, xOffsetField);
      layout.putConstraint(vCtr, baselineField, 0, vCtr, xOffsetField);
   }

   @Override public void reload(boolean initial)
   {
      TraceNode trace = getTraceNode();
      if(trace == null) return;
      
      titleEditor.loadContent(trace.getAttributedTitle(false), trace.getFontFamily(), 0, trace.getFillColor(), 
            trace.getFontStyle());
      showInLegendChk.setSelected(trace.getShowInLegend());
      
      dsEditor.reload(trace);
      
      modeCombo.setSelectedItem(trace.getMode());
      xOffsetField.setValue(trace.getXOffset());
      yOffsetField.setValue(trace.getYOffset());
      skipIntvField.setValue(trace.getSkip());

      lenField.setValue(trace.getSlidingWindowLength());
      lenField.setEnabled(trace.getMode() == TraceNode.DisplayMode.TRENDLINE);
      avgChk.setSelected(trace.getShowAverage());
      avgChk.setEnabled(trace.getMode() == TraceNode.DisplayMode.MULTITRACE);

      barWidthField.setValue(trace.getBarWidth());
      barWidthField.setEnabled(trace.getMode() == TraceNode.DisplayMode.HISTOGRAM);
      baselineField.setValue(trace.getBaseline());
      baselineField.setEnabled(trace.getMode() == TraceNode.DisplayMode.HISTOGRAM);
      
      avgChk.setSelected(trace.getShowAverage());
      avgChk.setEnabled(trace.getMode() == TraceNode.DisplayMode.MULTITRACE);

      textStyleEditor.loadGraphicNode(trace);
      drawStyleEditor.loadGraphicNode(trace);
      if(initial)
      {
         symbolEditor.reload(trace.getSymbolNode());
         ebarEditor.reload(trace.getErrorBarNode());
         setSelectedTab(0);
      }
      else
      {
         symbolEditor.reload(null);
         ebarEditor.reload(null);
      }
   }

   /** 
    * Ensure the modeless pop-up windows associated with the embedded style editors, marker symbol editor, or error bar 
    * editor.
    */
   @Override void onLowered()
   {
      textStyleEditor.cancelEditing();
      drawStyleEditor.cancelEditing();
      symbolEditor.cancelEditing();
      ebarEditor.cancelEditing();
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

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.TRACE == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_TRACE_32); }
   @Override String getRepresentativeTitle() { return("Data Trace Properties"); }

   @Override public int getNumTabs() { return(3); }
   @Override public int getSelectedTab() { return(iSelectedTab); }
   @Override public void setSelectedTab(int tabPos)
   {
      if(tabPos < 0 || tabPos >= getNumTabs() || tabPos == iSelectedTab) return;

      iSelectedTab = tabPos;
      fireStateChanged();
      
      CardLayout cl = (CardLayout) tabContent.getLayout();
      cl.show(tabContent, iSelectedTab==0 ? TAB_LINE : (iSelectedTab == 1 ? TAB_SYMBOLS : TAB_EBARS));
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
      return(tabPos == 0 ? TAB_LINE : (tabPos == 1 ? TAB_SYMBOLS : TAB_EBARS)); 
   }
   @Override public String getTabToolTip(int tabPos) { return(null); }
   @Override public Icon getTabIcon(int tabPos) { return(null); }
   @Override public boolean isTabClosable(int tabPos) { return(false); }
   @Override public String getCloseTabToolTip(int tabPos) { return(null); }
   @Override public void closeTab(int tabPos) {}
   @Override public boolean supportsTabRepositioning() { return(false); }
   @Override public boolean moveTab(int fromPos, int toPos) { return(false); }

   /** The currently selected tab -- "Line" (0), "Symbols" (1), or "Error Bars" tab. */
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
      TraceNode trace = getTraceNode();
      if(trace == null) return;
      Object src = e.getSource();

      boolean ok = true;
      if(src == titleEditor)
      {
         String text = FGraphicNode.toStyledText(
               titleEditor.getCurrentContents(), trace.getFontStyle(), trace.getFillColor());
         ok = trace.setTitle(text);
         if(!ok) titleEditor.loadContent(trace.getAttributedTitle(false), trace.getFontFamily(), 0, 
               trace.getFillColor(), trace.getFontStyle());
      }
      else if(src == showInLegendChk)
         trace.setShowInLegend(showInLegendChk.isSelected());
      else if(src == modeCombo)
      {
          trace.setMode((TraceNode.DisplayMode) modeCombo.getSelectedItem());
          
          // switching to multitrace mode has some side effects...
          if(trace.getMode() == TraceNode.DisplayMode.MULTITRACE)
          {
             avgChk.setSelected(trace.getShowAverage());
             ebarEditor.reload(null);
          }
          
          // some widgets are only relevant in one display mode
          lenField.setEnabled(trace.getMode() == TraceNode.DisplayMode.TRENDLINE);
          avgChk.setEnabled(trace.getMode() == TraceNode.DisplayMode.MULTITRACE);
          barWidthField.setEnabled(trace.getMode() == TraceNode.DisplayMode.HISTOGRAM);
          baselineField.setEnabled(trace.getMode() == TraceNode.DisplayMode.HISTOGRAM);
      }
      else if(src == xOffsetField)
      {
         ok = trace.setXOffset(xOffsetField.getValue().floatValue());
         if(!ok) xOffsetField.setValue(trace.getXOffset());
      }
      else if(src == yOffsetField)
      {
         ok = trace.setYOffset(yOffsetField.getValue().floatValue());
         if(!ok) yOffsetField.setValue(trace.getYOffset());
      }
      else if(src == skipIntvField)
      {
         ok = trace.setSkip(skipIntvField.getValue().intValue());
         if(!ok) skipIntvField.setValue(trace.getSkip());
      }
      else if(src == lenField)
      {
         ok = trace.setSlidingWindowLength(lenField.getValue().intValue());
         if(!ok) lenField.setValue(trace.getSlidingWindowLength());
      }
      else if(src == barWidthField)
      {
         ok = trace.setBarWidth(barWidthField.getValue().floatValue());
         if(!ok) barWidthField.setValue(trace.getBarWidth());
      }
      else if(src == baselineField)
      {
         ok = trace.setBaseline(baselineField.getValue().floatValue());
         if(!ok) baselineField.setValue(trace.getBaseline());
      }
      else if(src == avgChk)
         trace.setShowAverage(avgChk.isSelected());

      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link TraceNode}. */
   private TraceNode getTraceNode() { return((TraceNode) getEditedNode()); }
   
   /** When checked, an entry for the data trace is included in parent graph's legend. */
   private final JCheckBox showInLegendChk;

   /** The custom widget within which the trace node's title (which may contain attributed text) is edited.  */
   private StyledTextEditor titleEditor = null;

   /** Self-contained editor handles editing/loading of the data set assigned to the trace. */
   private FGNPlottableDSCard dsEditor = null;
   
   /** Combo box selects the trace display mode. */
   private JComboBox<TraceNode.DisplayMode> modeCombo = null;

   /** Numeric text field sets the X-offset for the data trace. */
   private NumericTextField xOffsetField = null;

   /** Numeric text field sets the Y-offset for the data trace. */
   private NumericTextField yOffsetField = null;

   /** Numeric text field sets the plot skip interval for the data trace. */
   private NumericTextField skipIntvField = null;

   /** Numeric text field sets the window length for the sliding average trendline for the data trace. */
   private NumericTextField lenField = null;

   /** Sets the state of the trace node's <i>avg</i> flag. */
   private JCheckBox avgChk = null;

   /** Numeric text field sets the histogram bar width, if applicable. */
   private NumericTextField barWidthField = null;
   
   /** Numeric text field sets the histogram baseline value, if applicable. */
   private NumericTextField baselineField = null;
   
   /** Self-contained editor handles the node's text style properties. */
   private TextStyleEditor textStyleEditor = null;
   
   /** Self-contained editor handles the node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** The editor for the trace node's component symbol node, which encapsulates marker symbol properties. */
   private FGNSymbolCard symbolEditor = null;
   
   /** The editor for the trace node's component error bar node, which encapsulates error bar properties. */
   private FGNEBarCard ebarEditor = null;
   
   /** The title of the tab in which trace node's styling properties are shown. */
   private final static String TAB_LINE = "Line";
   /** The title of the tab in which the embedded marker symbol editor is shown. */
   private final static String TAB_SYMBOLS = "Symbols";
   /** The title of the tab in which the embedded error bar editor is shown. */
   private final static String TAB_EBARS = "Error Bars";
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
