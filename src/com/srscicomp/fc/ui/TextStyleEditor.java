package com.srscicomp.fc.ui;

import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SpringLayout;

import com.srscicomp.common.ui.FontFamilyButton;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.GenericFont;
import com.srscicomp.common.ui.MultiButton;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.common.ui.RGBColorPicker;
import com.srscicomp.fc.fig.FGNProperty;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.PSFont;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * <b>TextStyleEditor</b> displays/edits all text-related styles for a <i>Figure Composer</i> graphic node -- ie, an
 * instance of {@link FGraphicNode} or one of its subclasses.
 * 
 * <p>The following widgets are present in the editor panel, arranged in a single row from left to right:
 * <ul>
 * <li>A {@link RGBColorPicker}, for specifying the text/fill color. The button face is painted with the current color; 
 * clicking the button raises a compact panel by which the user can change the current color.</li>
 * <li>A {@link MultiButton} to select the font style (plain, bold, italic, bold-italic).</li>
 * <li>A numeric text field for setting the font size in typographical points. Input restricted to [1..99].</li>
 * <li>A customize push button, {@link FontFamilyButton}, for specifying the font family. The button text reflects the 
 * current font family name and is rendered in that font if the font is installed in the system (otherwise a default
 * system font is used, and "(?)" appears before the font name. Clicking the button raises a panel to change the font
 * selection, either by choosing from a list or entering a font name manually in a text field.</li>
 * <li>Standard combo boxes for selecting the alternate font (used when chosen font family is not installed), and the 
 * Postscript font (used when graphic model is exported to Postscript format).</li>
 * <li>The <i>Restore Style Defaults</i> button. A graphic node inherits the value of a style property from its parent, 
 * unless the style has been explicitly set by the user. Pressing this button brings up a pop-up menu that lets the user
 * reset all of the node's style properties, or one specific property, to the implicit, inherited state. [In the case of
 * the root figure node, the style is reset to the default value defined in the user's workspace preferences.] Also, the
 * menu lets the user choose to apply the restore action on the node itself only, or the node AND its descendants.</li>
 * </ul>
 * </p>
 * 
 * <p><b>TextStyleEditor</b> is intended to be paired with {@link DrawStyleEditor}, which exposes the draw-related
 * graphic styles. The two sets of graphic styles are handled by distinct editor objects because certain graphic node 
 * types only have text styles, while others only have the draw styles. Note that the "restore defaults" button is 
 * present only on <b>TextStyleEditor</b> even though its pop-up menu can affect any graphic style property. Also, the 
 * text/fill color property is technically both a text  and draw style. When both style editors appear together (for 
 * nodes that have both text and draw styles), the draw fill color widget is omitted in {@link DrawStyleEditor}, which
 * also lacks a restore button, because its functionality was deemed unnecessary for node types that only possess the 
 * draw styles.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
final class TextStyleEditor extends JPanel implements ActionListener, PropertyChangeListener, ItemListener
{
   /** Construct the text style properties editor. */
   TextStyleEditor()
   {
      super();
      setOpaque(false);
      
      textColorPicker = new RGBColorPicker();
      textColorPicker.setToolTipText("Text/Fill Color");
      textColorPicker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
      add(textColorPicker);
      
      fontFamilyBtn = new FontFamilyButton(100);
      fontFamilyBtn.addPropertyChangeListener(FontFamilyButton.FONTFAMILY_PROPERTY, this);
      add(fontFamilyBtn);
      
      fontStyleMB = new MultiButton<FontStyle>();
      fontStyleMB.addChoice(FontStyle.PLAIN, FCIcons.V4_FSPLAIN_16, "Plain");
      fontStyleMB.addChoice(FontStyle.ITALIC, FCIcons.V4_FSITALIC_16, "Italic");
      fontStyleMB.addChoice(FontStyle.BOLD, FCIcons.V4_FSBOLD_16, "Bold");
      fontStyleMB.addChoice(FontStyle.BOLDITALIC, FCIcons.V4_FSBOLDITALIC_16, "Bold Italic");
      fontStyleMB.addItemListener(this);
      fontStyleMB.setToolTipText("Font style");
      add(fontStyleMB);
      
      fontSizeField = new NumericTextField(FGraphicNode.MINFONTSIZE, FGraphicNode.MAXFONTSIZE);
      fontSizeField.setToolTipText("Font size (pt)");
      fontSizeField.addActionListener(this);
      add(fontSizeField);
      
      altFontCombo = new JComboBox<GenericFont>(GenericFont.values());
      altFontCombo.setToolTipText("Generic font");
      altFontCombo.setPrototypeDisplayValue(GenericFont.SERIF);
      altFontCombo.addActionListener(this);
      add(altFontCombo);
      
      psFontCombo = new JComboBox<PSFont>(PSFont.values());
      psFontCombo.setToolTipText("Postscript font");
      psFontCombo.setPrototypeDisplayValue(PSFont.HELVETICA);
      psFontCombo.addActionListener(this);
      add(psFontCombo);
      
      restoreBtn = new JButton();
      restoreBtn.setIcon(FCIcons.V4_UNDO_16);
      restoreBtn.setToolTipText("Restore style defaults...");
      restoreBtn.addActionListener(this);
      restoreBtn.setMargin(new Insets(0,0,0,0));
      restoreBtn.setOpaque(false);  // Needed this b/c default bkg showed around edges in Windows.
      add(restoreBtn);
      
      createRestoreBtnPopup();

      SpringLayout layout = new SpringLayout();
      setLayout(layout);
      layout.putConstraint(SpringLayout.WEST, textColorPicker, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, fontStyleMB, GAP, SpringLayout.EAST, textColorPicker);
      layout.putConstraint(SpringLayout.WEST, fontSizeField, GAP, SpringLayout.EAST, fontStyleMB);
      layout.putConstraint(SpringLayout.WEST, fontFamilyBtn, GAP, SpringLayout.EAST, fontSizeField);
      layout.putConstraint(SpringLayout.WEST, altFontCombo, GAP, SpringLayout.EAST, fontFamilyBtn);
      layout.putConstraint(SpringLayout.WEST, psFontCombo, GAP, SpringLayout.EAST, altFontCombo);
      layout.putConstraint(SpringLayout.WEST, restoreBtn, GAP*2, SpringLayout.EAST, psFontCombo);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, restoreBtn);
      
      layout.putConstraint(SpringLayout.NORTH, fontFamilyBtn, 0, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, fontFamilyBtn);

      String vCtr = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vCtr, textColorPicker, 0, vCtr, fontFamilyBtn);
      layout.putConstraint(vCtr, fontStyleMB, 0, vCtr, fontFamilyBtn);
      layout.putConstraint(vCtr, fontSizeField, 0, vCtr, fontFamilyBtn);
      layout.putConstraint(vCtr, altFontCombo, 0, vCtr, fontFamilyBtn);
      layout.putConstraint(vCtr, psFontCombo, 0, vCtr, fontFamilyBtn);
      layout.putConstraint(vCtr, restoreBtn, 0, vCtr, fontFamilyBtn);
   }

   /** The figure graphic node currently being edited. If null, then the editor is hidden. */
   private FGraphicNode editedNode = null;
   
   /** 
    * Get the figure graphic node currently loaded into this text style editor.
    * @return The currently edited node.
    */
   public FGraphicNode getEditedNode() { return(editedNode); }
   
   /**
    * Reload this editor to display and modify the text style properties of the specified graphic node. 
    * @param n The graphic node being edited. If null, the editor will be hidden.
    * @return True if node was loaded into editor; false if <i>n == null</i>.
    */
   public boolean loadGraphicNode(FGraphicNode n)
   {
      editedNode = n;

      setVisible(editedNode != null);
      if(editedNode == null) return(false);

      isReloading = true;
      try
      {
         if(editedNode.hasFontProperties())
         {
            fontFamilyBtn.setFontFamily(editedNode.getFontFamily(), false);
            altFontCombo.setSelectedItem(editedNode.getAltFont());
            altFontCombo.setToolTipText(String.format("Generic font: %s", editedNode.getAltFont().toString()));
            psFontCombo.setSelectedItem(editedNode.getPSFont());
            psFontCombo.setToolTipText(String.format("Postscript font: %s", editedNode.getPSFont().toString()));
            fontSizeField.setValue(editedNode.getFontSizeInPoints());
            fontStyleMB.setCurrentChoice(editedNode.getFontStyle());
         }
         
         if(editedNode.hasFillColorProperty())
            textColorPicker.setCurrentColor(editedNode.getFillColor(), false);
         
         restoreBtn.setEnabled(editedNode.canRestoreDefaultStyles() || editedNode.getChildCount() > 0);
      }
      finally { isReloading = false; }

      return(true);
   }

   /** Flag set while reloading widgets, so that we don't trigger calls into the edited node. */
   private boolean isReloading = false;
   
   /**
    * Call this method prior to hiding the text style editor. It makes sure that the transient pop-up windows associated
    * with the text color picker and font style multi-choice widget are hidden.
    */
   public void cancelEditing() { textColorPicker.cancelPopup(); fontStyleMB.cancelPopup();}

   
   //
   // ActionListener, PropertyChangeListener, ItemListener
   //
 
   public void actionPerformed(ActionEvent e)
   {
      if(editedNode == null || isReloading) return;
      Object src = e.getSource();

      if(src == fontSizeField)
      {
         if(!editedNode.setFontSizeInPoints(fontSizeField.getValue().intValue()))
         {
            Toolkit.getDefaultToolkit().beep();
            fontSizeField.setValue(editedNode.getFontSizeInPoints());
         }
      }
      else if(src == altFontCombo)
      {
         GenericFont gf = (GenericFont) altFontCombo.getSelectedItem();
         editedNode.setAltFont(gf);
         altFontCombo.setToolTipText(String.format("Generic font: %s", gf.toString()));
      }
      else if(src == psFontCombo)
      {
         PSFont psf = (PSFont) psFontCombo.getSelectedItem();
         editedNode.setPSFont(psf);
         psFontCombo.setToolTipText(String.format("Postscript font: %s", psf.toString()));
      }
      else if(src == restoreBtn)
         restorePopup.show(restoreBtn, 5, 5);
      else
      {
         // handle a selection from the "restore defaults" pop-up menu
         String cmd = e.getActionCommand();
         boolean includeDescendants = false;
         int i = cmd.indexOf('+');
         if(i > 0) return;
         else if(i == 0)
         {
            includeDescendants = true;
            cmd = cmd.substring(1);
         }

         if(cmd.equals(CMD_RESTOREALLSTYLES))
            editedNode.restoreDefaultStyles(null, includeDescendants);
         else
         {
            FGNProperty[] styles = FGraphicNode.getStylePropertyIDs();
            for(FGNProperty style : styles) if(cmd.equals(style.getNiceName()))
            {
               editedNode.restoreDefaultStyles(style, includeDescendants);
               return;
            }
         }
      }
   }

   @Override public void propertyChange(PropertyChangeEvent e)
   {
      if(isReloading || editedNode == null) return;
      String prop = e.getPropertyName();
      Object src = e.getSource();
      if(prop.equals(RGBColorPicker.COLOR_PROPERTY) && src == textColorPicker)
         editedNode.setFillColor(textColorPicker.getCurrentColor());
      else if(prop.equals(FontFamilyButton.FONTFAMILY_PROPERTY) && src == fontFamilyBtn)
         editedNode.setFontFamily(fontFamilyBtn.getFontFamily());
   }

   @Override public void itemStateChanged(ItemEvent e)
   {
      if(isReloading || editedNode == null || e.getSource() != fontStyleMB) return;
      editedNode.setFontStyle(fontStyleMB.getCurrentChoice());
   }

   
   //
   // Widgets
   //
   
   /** Compact custom button widget uses a pop-up panel to edit the text fill color. */
   private RGBColorPicker textColorPicker = null;

   /** Compact custom button widget uses a pop-up panel to change the font family selection. */
   private FontFamilyButton fontFamilyBtn = null;

   /** A multiple-choice widget to select the current font style. */
   private MultiButton<FontStyle> fontStyleMB = null;
   
   /** Text field for editing current font size in typographical points. */
   private NumericTextField fontSizeField = null;

   /** Selects the generic font that's used when the specified font family is not installed on host. */
   private JComboBox<GenericFont> altFontCombo = null;

   /** Selects a standard Postscript font to be used during export to Postscript format. */
   private JComboBox<PSFont> psFontCombo = null;

   /** Raises a pop-up by which user can restore one or all style properties to their inherited or default values. */
   private JButton restoreBtn = null;

   /** 
    * Pop-up menu for <i>Restore style defaults</i> button. Lets user select which style to restore and whether or not
    * it should be restored across all descendants of the edited node.
    */
   private JPopupMenu restorePopup = null;
   
   /** 
    * Action command for pop-up menu item to restore all graphic styles. For the menu items selecting a particular 
    * graphic style, the action command is the "nice name" of the style property. If both the edited node AND its 
    * descendants are to be changed, the action command is prepended with a '+' character.
    */
   private final static String CMD_RESTOREALLSTYLES = "all styles";

   /** 
    * TODO: Make this a static resource shared by all instances of TextStyleEditor???
    * Helper method for {@link #construct()}. It creates the pop-up menu attached to the <i>Restore style
    * defaults</i> button.
    */
   private void createRestoreBtnPopup()
   {
      if(restorePopup != null) return;
      
      restorePopup = new JPopupMenu("Restore style defaults");
      JMenu menu1 = new JMenu("On this node only");
      JMenu menu2 = new JMenu("On this node and descendants");
      restorePopup.add(menu1);
      restorePopup.add(menu2);
      
      JMenuItem menuItem = new JMenuItem(CMD_RESTOREALLSTYLES);
      menuItem.setActionCommand(CMD_RESTOREALLSTYLES);
      menuItem.addActionListener(this);
      menu1.add(menuItem);
      
      menuItem = new JMenuItem(CMD_RESTOREALLSTYLES);
      menuItem.setActionCommand("+" + CMD_RESTOREALLSTYLES);
      menuItem.addActionListener(this);
      menu2.add(menuItem);
      
      FGNProperty[] styles = FGraphicNode.getStylePropertyIDs();
      for(FGNProperty style : styles)
      {
         String name = style.getNiceName();
         menuItem = new JMenuItem(name);
         menuItem.setActionCommand(name);
         menuItem.addActionListener(this);
         menu1.add(menuItem);
         
         menuItem = new JMenuItem(name);
         menuItem.setActionCommand("+" + name);
         menuItem.addActionListener(this);
         menu2.add(menuItem);
      }
   }
   
   /** Horizontal space between widgets, in pixels. */
   final static int GAP = 5;
}
