package com.srscicomp.fc.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.common.ui.TabStrip;
import com.srscicomp.common.ui.TabStripModel;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.FunctionNode;
import com.srscicomp.fc.uibase.StyledTextEditor;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * <code>FGNFunctionEditor</code> displays/edits all properties of a {@link FunctionNode}. The
 * function formula <i>f(x)</i> is entered in a text field; an accompanying icon indicates whether or not the entered 
 * formula is valid. Above the formula field is one text field that sets the node title, which will appear in the graph 
 * legend if the adjacent check box is checked. Under the formula field are three numeric text fields that specify the 
 * range and interval for the values of <i>x</i> at which <i>f(x)</i> is evaluated.
 * 
 * <p>The bottom portion of the editor panel is occupied by two-page tabbed pane for editing the graphical styling of 
 * the rendered function. The <i>Polyline</i> tab holds a {@link TextStyleEditor} and a {@link DrawStyleEditor}, which 
 * display the function node's own style properties -- these govern the appearance of the polyline that connects the 
 * points at which the function is evaluated. The <i>Symbols</i> tab is a {@link FGNSymbolCard}, which encapsulates 
 * widgets for editing all properties of the function node's component {@link com.srscicomp.fc.fig.SymbolNode}. We use
 * {@link TabStrip TabStrip} and a content panel with a {@link CardLayout} manager to implement
 * the tabbed-pane interface.</p>
 * 
 * @author sruffner
 */
class FGNFunctionEditor extends FGNEditor implements TabStripModel, ActionListener, FocusListener
{

   /** Construct the figure node properties editor. */
   FGNFunctionEditor()
   {
      showInLegendChk = new JCheckBox();
      showInLegendChk.setToolTipText("Check this box to include entry in legend for this function");
      showInLegendChk.setContentAreaFilled(false);
      showInLegendChk.addActionListener(this);
      add(showInLegendChk);
      
      titleEditor = new StyledTextEditor(1);
      titleEditor.setToolTipText("Enter title here (it will be used as the label for the node's legend entry)");
      titleEditor.addActionListener(this);
      add(titleEditor);
            
      functionField = new JTextField(15);
      functionField.setToolTipText("f(x)");
      functionField.addActionListener(this);
      functionField.addFocusListener(this);
      add(functionField);
      
      validFcnLabel = new JLabel(FCIcons.V4_OK_16);
      add(validFcnLabel);
      
      x0Field = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      x0Field.setColumns(6);
      x0Field.setToolTipText("x0");
      x0Field.addActionListener(this);
      add(x0Field);
      
      JLabel toLabel = new JLabel(" to ");
      add(toLabel);
      x1Field = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      x1Field.setColumns(6);
      x1Field.setToolTipText("x1");
      x1Field.addActionListener(this);
      add(x1Field);
      
      JLabel byLabel = new JLabel(" by ");
      add(byLabel);
      dxField = new NumericTextField(-Double.MAX_VALUE, Double.MAX_VALUE, 3);
      dxField.setColumns(6);
      dxField.setToolTipText("dx");
      dxField.addActionListener(this);
      add(dxField);
      
      // set up the tabbed pane interface to access the polyline styling properties and the marker symbol properties
      textStyleEditor = new TextStyleEditor();
      drawStyleEditor = new DrawStyleEditor(true, false);
      symbolEditor = new FGNSymbolCard();
      
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

      tabContent.add(polylinePanel, TAB_POLYLINE);
      tabContent.add(symbolPanel, TAB_SYMBOLS);
      
      // now layout the widgets, tab strip, and tab content panel in rows.
      layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. Assume the tab content panel will be longest.
      layout.putConstraint(SpringLayout.WEST, tabContent, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, tabContent);
      
      layout.putConstraint(SpringLayout.WEST, showInLegendChk, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, titleEditor, GAP, SpringLayout.EAST, showInLegendChk);
      layout.putConstraint(SpringLayout.EAST, titleEditor, 0, SpringLayout.EAST, tabContent);
      
      layout.putConstraint(SpringLayout.WEST, functionField, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, functionField, -GAP, SpringLayout.WEST, validFcnLabel);
      layout.putConstraint(SpringLayout.EAST, validFcnLabel, 0, SpringLayout.EAST, tabContent);
      
      layout.putConstraint(SpringLayout.WEST, x0Field, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, toLabel, 0, SpringLayout.EAST, x0Field);
      layout.putConstraint(SpringLayout.WEST, x1Field, 0, SpringLayout.EAST, toLabel);
      layout.putConstraint(SpringLayout.WEST, byLabel, 0, SpringLayout.EAST, x1Field);
      layout.putConstraint(SpringLayout.WEST, dxField, 0, SpringLayout.EAST, byLabel);
      
      layout.putConstraint(SpringLayout.WEST, tStrip, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, tStrip, 0, SpringLayout.EAST, tabContent);
      
      // top-bottom constraints: 5 rows, with tab strip and content pane occupying the last two. One widget per row sets 
      // constraints with row above or below. The remaining widgets in a row are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, titleEditor, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, functionField, GAP*2, SpringLayout.SOUTH, titleEditor);
      layout.putConstraint(SpringLayout.NORTH, x0Field, GAP, SpringLayout.SOUTH, functionField);
      layout.putConstraint(SpringLayout.NORTH, tStrip, GAP/2, SpringLayout.SOUTH, x0Field);
      layout.putConstraint(SpringLayout.NORTH, tabContent, 0, SpringLayout.SOUTH, tStrip);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, tabContent);
      
      String vCtr = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vCtr, showInLegendChk, 0, vCtr, titleEditor);
      layout.putConstraint(vCtr, validFcnLabel, 0, vCtr, functionField);
      layout.putConstraint(vCtr, toLabel, 0, vCtr, x0Field);
      layout.putConstraint(vCtr, x1Field, 0, vCtr, x0Field);
      layout.putConstraint(vCtr, byLabel, 0, vCtr, x0Field);
      layout.putConstraint(vCtr, dxField, 0, vCtr, x0Field);
   }

   @Override public void reload(boolean initial)
   {
      FunctionNode fcn = getFunctionNode();
      if(fcn == null) return;
      
      functionField.setText(fcn.getFunctionString());
      boolean valid = fcn.isFunctionValid();
      validFcnLabel.setIcon(valid ? FCIcons.V4_OK_16 : FCIcons.V4_NOTOK_16);
      validFcnLabel.setToolTipText(valid ? null : fcn.getReasonFunctionInvalid());

      x0Field.setValue(fcn.getX0());
      x1Field.setValue(fcn.getX1());
      dxField.setValue(fcn.getDX());
      
      titleEditor.loadContent(fcn.getAttributedTitle(false), fcn.getFontFamily(), 0, fcn.getFillColor(), 
            fcn.getFontStyle());
      showInLegendChk.setSelected(fcn.getShowInLegend());

      textStyleEditor.loadGraphicNode(fcn);
      drawStyleEditor.loadGraphicNode(fcn);
      if(initial)
      {
         symbolEditor.reload(fcn.getSymbolNode());
         setSelectedTab(0);
      }
      else symbolEditor.reload(null);
   }

   /** 
    * Ensure the modeless pop-up windows associated with the Unicode text field (for function's title) and any color
    * picker button associated with the embedded style editors or marker symbol editor.
    */
   @Override void onLowered()
   {
      textStyleEditor.cancelEditing();
      drawStyleEditor.cancelEditing();
      symbolEditor.cancelEditing();
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

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.FUNCTION == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_FUNCTION_32); }
   @Override String getRepresentativeTitle() { return("Function Properties"); }

   @Override public int getNumTabs() { return(2); }
   @Override public int getSelectedTab() { return(iSelectedTab); }
   @Override public void setSelectedTab(int tabPos)
   {
      if(tabPos < 0 || tabPos >= getNumTabs() || tabPos == iSelectedTab) return;

      iSelectedTab = tabPos;
      fireStateChanged();
      
      CardLayout cl = (CardLayout) tabContent.getLayout();
      cl.show(tabContent, iSelectedTab==0 ? TAB_POLYLINE : TAB_SYMBOLS);
   }

   @Override public void addChangeListener(ChangeListener l)
   {
      if(l != null) tabListeners.add(ChangeListener.class, l);
   }
   @Override public void removeChangeListener(ChangeListener l)
   {
      if(l != null) tabListeners.remove(ChangeListener.class, l);
   }

   @Override public String getTabLabel(int tabPos) { return(tabPos == 0 ? TAB_POLYLINE : TAB_SYMBOLS); }
   @Override public String getTabToolTip(int tabPos) { return(null); }
   @Override public Icon getTabIcon(int tabPos) { return(null); }
   @Override public boolean isTabClosable(int tabPos) { return(false); }
   @Override public String getCloseTabToolTip(int tabPos) { return(null); }
   @Override public void closeTab(int tabPos) {}
   @Override public boolean supportsTabRepositioning() { return(false); }
   @Override public boolean moveTab(int fromPos, int toPos) { return(false); }
   
   /** The currently selected tab -- either the "Polyline" (0) or "Symbols" (1) tab. */
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
   
   /** Posts action event on the title or function formula field if that field loses the keyboard focus. */
   @Override public void focusLost(FocusEvent e)
   {
      if(e.isTemporary()) return;
      Object src = e.getSource();
      if(src == functionField) functionField.postActionEvent();
   }
   /** Selects all the text in the title or function formula field if that field gains the focus. */
   @Override public void focusGained(FocusEvent e) 
   { 
      if(e.isTemporary()) return;
      Object src = e.getSource();
      if(src == functionField) functionField.selectAll();
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      FunctionNode fcn = getFunctionNode();
      if(fcn == null) return;
      Object src = e.getSource();

      boolean ok = true;
      if(src == titleEditor)
      {
         String text = FGraphicNode.toStyledText(
               titleEditor.getCurrentContents(), fcn.getFontStyle(), fcn.getFillColor());
         ok = fcn.setTitle(text);
         if(!ok) titleEditor.loadContent(fcn.getAttributedTitle(false), fcn.getFontFamily(), 0, 
               fcn.getFillColor(), fcn.getFontStyle());
      }
      else if(src == x0Field)
      {
         ok = fcn.setX0(x0Field.getValue().doubleValue());
         if(!ok) x0Field.setValue(fcn.getX0());
      }
      else if(src == x1Field)
      {
         ok = fcn.setX1(x1Field.getValue().doubleValue());
         if(!ok) x1Field.setValue(fcn.getX1());
      }
      else if(src == dxField)
      {
         ok = fcn.setDX(dxField.getValue().doubleValue());
         if(!ok) dxField.setValue(fcn.getDX());
      }
      else if(src == functionField)
      {
         boolean wasValid = fcn.isFunctionValid();
         fcn.setFunctionString(functionField.getText());
         boolean valid = fcn.isFunctionValid();
         if(wasValid == valid) return;
         validFcnLabel.setIcon(valid ? FCIcons.V4_OK_16 : FCIcons.V4_NOTOK_16);
         validFcnLabel.setToolTipText(valid ? null : fcn.getReasonFunctionInvalid());
      }
      else if(src == showInLegendChk)
         fcn.setShowInLegend(showInLegendChk.isSelected());

      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** Convenience method casts the edited node to an instance of {@link FunctionNode}. */
   private FunctionNode getFunctionNode() { return((FunctionNode) getEditedNode()); }
   
   /** The text field in which user enters the function formula. */
   private JTextField functionField = null;

   /** Iconic label indicates whether or not current function formula is valid. Tool tip shows error description. */
   private JLabel validFcnLabel = null;
   
   /** Numeric text field for the function's <i>x0</i> property. */
   private NumericTextField x0Field = null;

   /** Numeric text field for the function's <i>x1</i> property. */
   private NumericTextField x1Field = null;

   /** Numeric text field for the function's <i>dx</i> property. */
   private NumericTextField dxField = null;
   
   /** The custom widget within which the trace node's title (which may contain attributed text) is edited.  */
   private StyledTextEditor titleEditor = null;

   /** When checked, an entry for the function is included in parent graph's legend. */
   private final JCheckBox showInLegendChk;

   /** Self-contained editor handles the node's text style properties. */
   private TextStyleEditor textStyleEditor = null;
   
   /** Self-contained editor handles the node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** The editor for the function's component symbol node, which encapsulates marker symbol properties. */
   private FGNSymbolCard symbolEditor = null;
   
   /** The title of the tab in which function node's styling properties are shown. */
   private final static String TAB_POLYLINE = "Polyline";
   /** The title of the tab in which the embedded marker symbol editor is shown. */
   private final static String TAB_SYMBOLS = "Symbols";
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
