package com.srscicomp.fc.ui;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.ui.MaxLengthDocumentFilter;
import com.srscicomp.fc.fig.*;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
 * <b>FGNSymbolCard</b> displays/edits all properties of the component {@link SymbolNode} of
 * selected data presentation nodes in the <i>FypML</i> graphic model. <i>It is intended only for use inside the node 
 * property editor for the parent {@link FGNPlottable} node, to be installed in a "Symbols" tab page.</i>
 * 
 * <p>The editor includes widgets to specify the marker symbol shape and size and an embedded {@link DrawStyleEditor} 
 * that exposes the symbol's draw styles. If the symbol node supports rendering a single character centered on the 
 * marker symbol, a short text field is included to specify that character. Font characteristics are set by the parent 
 * node's text styling attributes.</p>
 *
 * <p><b></b>Special use case - 3D scatter plot</b>. The {@link Scatter3DNode} specifies a minimum symbol size, while
 * the size of its component {@link SymbolNode} is the maximum symbol size. Logically, the minimum symbol size widget
 * should be included in this editor, but it's not a property of {@link SymbolNode}. To accommodate this special case,
 * this editor can be configured to include a minimum symbol size widget, and the tool tip for the regular size widget
 * is adjusted accordingly. In this configuration, the parent of the edited {@link SymbolNode} must be an instance of
 * {@link Scatter3DNode}.</p>
 * 
 * @author sruffner
 */
class FGNSymbolCard extends JPanel implements ActionListener, FocusListener
{
   /** Construct the symbol properties editor such that it exposes all properties of a {@link SymbolNode}. */
   FGNSymbolCard() { this(false, false, false); }


   /** 
    * Construct the symbol node properties editor. 
    * @param omitSymChar True to omit the short text field exposing the symbol character.
    * @param omitFillC True to omit the fill color picker from the embedded draw styles editor.
    * @param includeMinSize If true, include minimum symbol size widget to handle the component {@link SymbolNode} for
    * a 3D scatter plot. In this case, the edited node must be a {@link Scatter3DNode} rather than {@link SymbolNode}.
    */
   FGNSymbolCard(boolean omitSymChar, boolean omitFillC, boolean includeMinSize)
   {
      super();
      setOpaque(false);
      
      typeCombo = new JComboBox<>(Marker.values());
      typeCombo.setToolTipText("Select marker symbol shape");
      typeCombo.addActionListener(this);
      add(typeCombo);
      
      sizeEditor = new MeasureEditor(0, SymbolNode.SYMBOLSIZECONSTRAINTS);
      sizeEditor.setToolTipText(includeMinSize ? "Maximum (or fixed) symbol size" : "Size of symbol bounding box");
      sizeEditor.addActionListener(this);
      add(sizeEditor);

      if(includeMinSize)
      {
         minSizeEditor =new MeasureEditor(0, SymbolNode.SYMBOLSIZECONSTRAINTS);
         minSizeEditor.setToolTipText("Minimum symbol size (must be less than max size)");
         minSizeEditor.addActionListener(this);
         add(minSizeEditor);
      }
      if(!omitSymChar)
      {
         charField = new JTextField(2);
         charField.addActionListener(this);
         charField.addFocusListener(this);
         charField.setToolTipText("Enter single symbol character");
         add(charField);
         
         // restrict input length to a single character, if we can
         Document d = charField.getDocument();
         if(d instanceof AbstractDocument)
            ((AbstractDocument)d).setDocumentFilter(new MaxLengthDocumentFilter(1));
      }

      drawStyleEditor = new DrawStyleEditor(omitFillC, false);
      add(drawStyleEditor);
      
      
      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      
      // left-right constraints. Assume widest row is the one containing the draw style editor. 
      layout.putConstraint(SpringLayout.WEST, typeCombo, 0, SpringLayout.WEST, this);
      if(minSizeEditor != null)
         layout.putConstraint(SpringLayout.WEST, minSizeEditor, GAP, SpringLayout.EAST, typeCombo);
      layout.putConstraint(SpringLayout.WEST, sizeEditor, GAP, SpringLayout.EAST,
            minSizeEditor != null ? minSizeEditor : typeCombo);
      if(charField != null)
         layout.putConstraint(SpringLayout.WEST, charField, GAP*3, SpringLayout.EAST, sizeEditor);
      
      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, drawStyleEditor);

      // top-bottom constraints: One widget in each row is used to set the constraints with row above or below. The 
      // remaining widgets are vertically centered WRT that widget.
      layout.putConstraint(SpringLayout.NORTH, typeCombo, GAP, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP, SpringLayout.SOUTH, typeCombo);
      layout.putConstraint(SpringLayout.SOUTH, this, GAP, SpringLayout.SOUTH, drawStyleEditor);
      
      String vc = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vc, sizeEditor, 0, vc, typeCombo);
      if(minSizeEditor != null) layout.putConstraint(vc, minSizeEditor, 0, vc, typeCombo);
      if(charField != null) layout.putConstraint(vc, charField, 0, vc, typeCombo);
   }

   /**
    * Reload editor IAW contents of specified symbol node.
    *
    * @param sn The edited symbol node. If the editor is configured with the minimum symbol size widget, then the
    * parent of the symbol node must be an instance of {@link Scatter3DNode}.
    */
   public void reload(SymbolNode sn)
   {
      if(sn != null && ((minSizeEditor == null) || (sn.getParent() instanceof Scatter3DNode)))
         symbol = sn;
      if(symbol == null) return;
      
      typeCombo.setSelectedItem(symbol.getType());
      sizeEditor.setMeasure(symbol.getSize());
      if(minSizeEditor != null)
      {
         Scatter3DNode scat3d = (Scatter3DNode) symbol.getParent();
         minSizeEditor.setMeasure(scat3d.getMinSymbolSize());
      }
      if(charField != null) charField.setText(symbol.getCharacter());
      
      drawStyleEditor.loadGraphicNode(symbol);
   }

   /** Ensure the modeless pop-up windows associated with any widgets in this editor are extinguished. */
   void cancelEditing() { drawStyleEditor.cancelEditing(); }

   /**
    * This is a hook allowing the parent {@link FGNEditor} to pass on a character selected by the user via the special
    * characters tool dialog. If the symbol character field has the current keyboard focus, the specified character is
    * inserted into the field. Since the field only admits a single character, this will replace whatever was in the
    * field previously.
    * @param s A string containing the character selected.
    */
   void insertSymbolCharacter(String s)
   {
      if(charField != null && charField.isFocusOwner()) charField.setText(s.substring(0,1));
   }
   
   @Override public void focusGained(FocusEvent e) 
   {
      if(charField != null && e.getSource() == charField && !e.isTemporary()) charField.selectAll();
   }
   @Override public void focusLost(FocusEvent e)
   {
      if(charField != null && e.getSource() == charField && !e.isTemporary()) charField.postActionEvent();
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      if(symbol == null) return;
      Object src = e.getSource();

      boolean ok = true;
      if(charField != null && src == charField)
      {
         if(!symbol.setTitle(charField.getText()))
         {
            ok = false;
            charField.setText(symbol.getTitle());
         }
      }
      else if(src == typeCombo)
      {
         if(!symbol.setType((Marker)typeCombo.getSelectedItem()))
         {
            ok = false;
            typeCombo.setSelectedItem(symbol.getType());
         }
      }
      else if(src == sizeEditor)
      {
         if(minSizeEditor != null)
         {
            Scatter3DNode scat3d = (Scatter3DNode) symbol.getParent();
            if(!scat3d.setMaxSymbolSize(sizeEditor.getMeasure()))
            {
               ok = false;
               sizeEditor.setMeasure(scat3d.getMaxSymbolSize());
            }
         }
         if(!symbol.setSize(sizeEditor.getMeasure()))
         {
            ok = false;
            sizeEditor.setMeasure(symbol.getSize());
         }
      }
      else if(minSizeEditor != null && src == minSizeEditor)
      {
         Scatter3DNode scat3d = (Scatter3DNode) symbol.getParent();
         if(!scat3d.setMinSymbolSize(minSizeEditor.getMeasure()))
         {
            ok = false;
            minSizeEditor.setMeasure(scat3d.getMinSymbolSize());
         }
      }
      if(!ok) Toolkit.getDefaultToolkit().beep();
   }

   /** The current symbol node being edited. If null, editor is non-functional. */
   private SymbolNode symbol = null;
   
   /** Combo box for selecting the symbol marker shape. */
   private final JComboBox<Marker> typeCombo;

   /** Customized component for editing the symbol marker's size. */
   private MeasureEditor sizeEditor = null;

   /**
    * Customized component for editing minimum symbol size -- applicable only when the edited node is a 3D scatter
    * plot instead of a symbol node.
    */
   private MeasureEditor minSizeEditor = null;

   /** 
    * Text field for specifying a single character to be drawn centered inside symbol. Will be null if omitted from
    * the editor panel.
    */
   private JTextField charField = null;
   
   /** Self-contained editor handles the node's draw style properties. */
   private DrawStyleEditor drawStyleEditor = null;

   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
