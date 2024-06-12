package com.srscicomp.fc.uibase;


import java.awt.AWTEventMulticaster;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.srscicomp.common.ui.ArrowIcon;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.LocalFontEnvironment;
import com.srscicomp.common.ui.MultiButton;
import com.srscicomp.common.ui.RGBColorPicker;
import com.srscicomp.common.util.Utilities;

/**
 * <b>StyledTextEditor</b> is a custom component for displaying and editing text in which the text color, font style, 
 * underline state, and superscript state can vary on a character-by-character basis. Font family and font size 
 * attributes can be changed, but they apply to the entire text content.
 * 
 * <p>The editor may be restricted to a single line of text, or any number of lines. It uses a {@link JTextPane} to 
 * provide pseudo-WYSIWYG editing to the text. In the single-line configuration, the text pane is tailored to prevent 
 * word-wrapping, and pressing the "Enter" key notifies any registered action listeners that the contents have changed 
 * (similar to the behavior of a <b>JTextField</b>). In the multi-line configuration, word-wrapping is allowed and 
 * pressing the "Enter" key inserts a linefeed in the text at the current caret position, while pressing "Shift-Enter" 
 * notifies registered action listeners that the contents have changed.</p>
 * 
 * <p>A custom document filter installed on the text pane vetoes any change that inserts whitespace at the beginning of
 * the edited text. Any amount of whitespace is permitted at the end of the text.</p>
 * 
 * @author sruffner
 */
public class StyledTextEditor extends JPanel implements PropertyChangeListener, ItemListener,
      ActionListener, CaretListener, FocusListener
{
   /** The default text color. */
   private Color implicitTextColor = Color.black;
   
   /** The default font style. */
   private FontStyle implicitFontStyle = FontStyle.PLAIN;
   
   /** 
    * The number of text lines displayed in the text pane, set at construction. If 1, the editor is configured to show
    * only a single line of text, and pressing "Enter" notifies any registered action listeners of a change in the 
    * editor's content. If > 1, "Enter" inserts a linefeed in the text content, and "Shift-Enter" notifies.
    * <p>In addition, the scroll pane height is adjusted so that (roughly) this many text lines are visible without 
    * scrolling.</p>
    */
   private final int nLines;
   
   /** 
    * If set, registered action listeners are notified of a possible change in the editor's content ONLY when the
    * user explicitly "submits" the change via keyboard shortcut (<b>Enter</b> or <b>Shift-Enter</b>) or the explicit 
    * "enter" button, or if the editor loses the permanent keyboard focus.
    */
   private final boolean notifyOnlyOnEnter;
   
   /** The text editing component. */
   private JTextPane textPane = null;
   /** The scroll pane in which text editing component is housed - so we can adapt its height to the font size. */
   private JScrollPane scrollPane = null;
   
   /** Compact custom button widget uses a pop-up panel to edit the text fill color. */
   private final RGBColorPicker textColorPicker;
   /** A multiple-choice widget to select the current font style. */
   private MultiButton<FontStyle> fontStyleMB = null;
   
   /** A multiple-choice widget to select superscript, subscript or normal text. */
   private MultiButton<Integer> superscriptMB = null;

   /** Check this box to underline text. */
   private JCheckBox underlineChk = null;

   /** In multi-line configuration only, this button is an alternative to the "Shift-Enter" keyboard shortcut. */
   private JButton enterBtn = null;

   /** A container for the attribute widgets and optional "enter" button. */
   private JPanel widgetPanel = null;
   
   /** In the single-line configuration only, this button toggles the visibility of the attribute widgets. */
   private JButton toggleWidgetVisBtn = null;
   
   private final static int DEF_FONTSZ = 14;
   private final static ArrowIcon openWidgetsIcon = new ArrowIcon(Color.black, 8, ArrowIcon.Direction.LEFT);
   private final static ArrowIcon closeWidgetsIcon = new ArrowIcon(Color.black, 8, ArrowIcon.Direction.RIGHT);
   
   /** Construct an <b>StyledTextEditor</b> for editing a single line of text. */
   public StyledTextEditor() { this(1, false); }
   
   /**
    * Construct an <b>StyledTextEditor</b> for editing one or more lines of text. 
    * 
    * <p>If <b>n==1</b>, pressing the "Enter" key notifies registered action listeners that the editor's contents may 
    * have changed. If <b>n>1</b>, pressing "Enter" inserts a linefeed in the current text content, while "Shift-Enter" 
    * notifies registered action listeners. The editor panel also includes an actual "Enter" button in the multi-line
    * configuration. All attribute controls are visible -- above the text pane in the multi-line configuration, and
    * right of the pane in the single-line configuration.
    * 
    * @param n Number of visible text lines. Range-restricted to [1..10].
    */
   public StyledTextEditor(int n) { this(n, n>1); }
   
   /** 
    * Construct an <b>StyledTextEditor</b> for editing one or more lines of text, with or without an explicit 
    * "Enter" button on the editor panel. Registered action listeners are notified when the user "enters" the changes 
    * explicitly, but also when some text is selected in the editor pane and the user changes the attributes of the 
    * selected text.
    * 
    * @param n Number of visible text lines. Range-restricted to [1..10].
    * @param showEnterBtn If true, the editor includes an explicit "Enter" button.
    */
   public StyledTextEditor(int n, boolean showEnterBtn) { this(n, showEnterBtn, false); }
   
   /**
    * Construct a <b>StyledTextEditor</b>. The text pane is empty initially, and the attribute controls are set to
    * the following defaults: black text color, plain font style, underline off, super/subscript off.
    * 
    * <p>The editor is configured at construction time to allow only a single line of text, or multiple lines. In the
    * single-line configuration, pressing the "Enter" key fires an action event to notify any registered action 
    * listeners that the editor's contents have changed. In the multi-line configuration, pressing "Enter" merely
    * inserts a new line into the text content at the current caret position; while "Shift-Enter" fires the action
    * event indicating that the contents have changed.</p>
    * 
    * <p>In the single-line configuration, an iconic arrow button along the right edge of the editor panel toggles the
    * visibility of the attribute widgets (and "enter" button, if present). Hiding the widgets provides more horizontal
    * space for the text pane; they are hidden initially.</p>
    * 
    * <p>In lieu of using the widgets, the user can rely on the following shortcuts, which are active whenever the 
    * keyboard focus is on the text pane:
    * <ul>
    * <li><b>Enter</b> (<b>Shift-Enter</b> in multi-line configuration): Submit changes. All registered action listeners
    * are notified.</li>
    * <li><b>Ctrl-I, Ctrl-B, Ctrl-U</b> (command key on MacOS): Toggle italic, bold, and underline on/off.</li>
    * <li><b>Ctrl-[up arrow], Ctrl-[down arrow]</b> : Toggle superscript, subscript on/off.</li>
    * <li><b>Ctrl-[right arrow]</b>: Toggle visibility of the attribute widgets.</li>
    * </ul>
    * Except for the last, these keyboard accelerators are also active in the multi-line configuration. Note that you
    * must reveal the widgets in order to make any changes in text color within the styled text content.</p>
    * 
    * @param n Number of visible text lines. If this is 1, the editor is configured to edit a single line of text; 
    * else, any number of text lines are permitted, and <i>n</i> governs the height of the scroll pane in which the
    * text is edited (which, in turn, affects the overall height of this component). Range-restricted to [1..10].
    * @param showEnterBtn If true, the editor includes an "Enter" button that serves the same purpose as the keyboard 
    * shortcut "Enter" in the single-line configuration and "Shift-Enter" in the multi-line configuration.
    * @param notifyOnlyOnEnter Notify registered action listeners only when the "enter" hot key or the explicit "Enter"
    * button is pressed. If false, action listeners are also notified when there's a non-empty selection and the user
    * changes its font style, text color, underline or superscript state.
    * widgets are hidden.
    */
   public StyledTextEditor(int n, boolean showEnterBtn, boolean notifyOnlyOnEnter)
   {
      nLines = Utilities.rangeRestrict(1, 10, n);
      this.notifyOnlyOnEnter = notifyOnlyOnEnter;
      
      textColorPicker = new RGBColorPicker();
      textColorPicker.setToolTipText("Text Color");
      textColorPicker.setCurrentColor(implicitTextColor, false);
      textColorPicker.addPropertyChangeListener(this);
      textColorPicker.setFocusable(false);

      fontStyleMB = new MultiButton<>();
      fontStyleMB.addChoice(FontStyle.PLAIN, FCIcons.V4_FSPLAIN_16, "Plain");
      fontStyleMB.addChoice(FontStyle.ITALIC, FCIcons.V4_FSITALIC_16, "Italic");
      fontStyleMB.addChoice(FontStyle.BOLD, FCIcons.V4_FSBOLD_16, "Bold");
      fontStyleMB.addChoice(FontStyle.BOLDITALIC, FCIcons.V4_FSBOLDITALIC_16, "Bold Italic");
      fontStyleMB.setCurrentChoice(implicitFontStyle);
      fontStyleMB.addItemListener(this);
      fontStyleMB.setToolTipText("Font style");
      fontStyleMB.setFocusable(false);
      
      underlineChk = new JCheckBox("ul");
      underlineChk.setContentAreaFilled(false);
      underlineChk.addActionListener(this);
      underlineChk.setToolTipText("Underline");
      underlineChk.setFocusable(false);
      
      superscriptMB = new MultiButton<>();
      superscriptMB.addChoice(NORMSCRIPT, FCIcons.V4_NORMSCRIPT_16, "Normal");
      superscriptMB.addChoice(TextAttribute.SUPERSCRIPT_SUPER, FCIcons.V4_SUPERSCRIPT_16, "Superscript");
      superscriptMB.addChoice(TextAttribute.SUPERSCRIPT_SUB, FCIcons.V4_SUBSCRIPT_16, "Subscript");
      superscriptMB.setCurrentChoice(NORMSCRIPT);
      superscriptMB.addItemListener(this);
      superscriptMB.setToolTipText("Baseline");
      superscriptMB.setFocusable(false);
      
      SimpleAttributeSet sas = new SimpleAttributeSet();
      StyleConstants.setForeground(sas, implicitTextColor);
      StyleConstants.setFontFamily(sas, LocalFontEnvironment.getSansSerifFont());
      StyleConstants.setBold(sas, implicitFontStyle.isBold());
      StyleConstants.setItalic(sas, implicitFontStyle.isItalic());
      StyleConstants.setFontSize(sas, DEF_FONTSZ);
      StyleConstants.setSubscript(sas, false);
      StyleConstants.setSuperscript(sas, false);
      StyleConstants.setUnderline(sas, false);
      
      // configure text pane for single-line or multi-line configuration. In single-line case, we effectively disable
      // word-wrapping and scroll horizontally when the text content is longer than the viewport. "Enter" fires an
      // action indicating the user "entered" something; in the multi-line case, "Shift-Enter" serves this purpose, 
      // while "Enter" inserts a linefeed.
      if(nLines == 1)
         textPane = new JTextPane() {
            @Override public boolean getScrollableTracksViewportWidth()
            {
             return(getUI().getPreferredSize(this).width <= getParent().getSize().width);
            }
         };
      else
         textPane = new JTextPane();

      Action fireActionPerformed = new AbstractAction()
      {
         @Override public void actionPerformed(ActionEvent e) { fireActionEvent(); }
      };
      textPane.getInputMap().put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, (nLines == 1) ? 0 : KeyEvent.SHIFT_DOWN_MASK),
            fireActionPerformed);
      
      // user can toggle underline, bold and italic styling using the usual hot keys, when focus is on text pane
      int accMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

      Action toggleUnderline = new AbstractAction()
      {
         @Override public void actionPerformed(ActionEvent e) { underlineChk.doClick(); }
      };
      textPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_U, accMod), toggleUnderline);

      Action toggleBold = new AbstractAction()
      {
         @Override public void actionPerformed(ActionEvent e)
         {
            FontStyle fs=fontStyleMB.getCurrentChoice();
            FontStyle updated=FontStyle.getFontStyle(!fs.isBold(), fs.isItalic());
            fontStyleMB.setCurrentChoice(updated);
            onFontStyleChanged();
         }
      };
      textPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_B, accMod), toggleBold);

      Action toggleItalic = new AbstractAction()
      {
         @Override public void actionPerformed(ActionEvent e)
         {
            FontStyle fs=fontStyleMB.getCurrentChoice();
            FontStyle updated=FontStyle.getFontStyle(fs.isBold(), !fs.isItalic());
            fontStyleMB.setCurrentChoice(updated);
            onFontStyleChanged();
         }
      };
      textPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_I, accMod), toggleItalic);

      Action toggleSuperscript = new AbstractAction()
      {
         @Override public void actionPerformed(ActionEvent e)
         {
            Integer choice=superscriptMB.getCurrentChoice();
            if(choice.equals(TextAttribute.SUPERSCRIPT_SUPER)) choice=NORMSCRIPT;
            else choice=TextAttribute.SUPERSCRIPT_SUPER;
            superscriptMB.setCurrentChoice(choice);
            onSuperscriptStateChanged();
         }
      };
      textPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, accMod), toggleSuperscript);

      Action toggleSubscript = new AbstractAction()
      {
         @Override public void actionPerformed(ActionEvent e)
         {
            Integer choice=superscriptMB.getCurrentChoice();
            if(choice.equals(TextAttribute.SUPERSCRIPT_SUB)) choice=NORMSCRIPT;
            else choice=TextAttribute.SUPERSCRIPT_SUB;
            superscriptMB.setCurrentChoice(choice);
            onSuperscriptStateChanged();
         }
      };
      textPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, accMod), toggleSubscript);

      if(nLines == 1)
      {
         Action toggleWidgets = new AbstractAction()
         {
            @Override public void actionPerformed(ActionEvent e)
            {
               if(toggleWidgetVisBtn != null) toggleWidgetVisBtn.doClick();
            }
         };
         textPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, accMod), toggleWidgets);
      }
      textPane.setCharacterAttributes(sas, true);
      textPane.addCaretListener(this);
      textPane.addFocusListener(this);
      scrollPane = new JScrollPane(textPane);
      
      fixScrollPaneHeight();
      
      // install document filter to prevent inserting whitespace at the start of the document (as well as restricting
      // to FC-supported characters...
      StyledDocument doc = textPane.getStyledDocument();
      if(doc instanceof AbstractDocument)
      {
         ((AbstractDocument) doc).setDocumentFilter(new DocFilter());
      }
      
      widgetPanel = new JPanel();
      widgetPanel.add(textColorPicker);
      widgetPanel.add(fontStyleMB);
      widgetPanel.add(underlineChk);
      widgetPanel.add(superscriptMB);
      if(showEnterBtn)
      {
         enterBtn = new JButton(FCIcons.V4_ENTER_22);
         enterBtn.setBorder(null);
         enterBtn.setBorderPainted(false);
         enterBtn.setFocusable(false);
         enterBtn.setToolTipText("<html>Confirm changes (<b>" + ((nLines==1) ? "" : "Shift-") + "Enter</b>)</html>");
         enterBtn.addActionListener(this);
         widgetPanel.add(enterBtn);
      }
      
      SpringLayout layout = new SpringLayout();
      widgetPanel.setLayout(layout);
      layout.putConstraint(SpringLayout.WEST, textColorPicker, 0, SpringLayout.WEST, widgetPanel);
      layout.putConstraint(SpringLayout.WEST, fontStyleMB, 3, SpringLayout.EAST, textColorPicker);
      layout.putConstraint(SpringLayout.WEST, underlineChk, 3, SpringLayout.EAST, fontStyleMB);
      layout.putConstraint(SpringLayout.WEST, superscriptMB, 3, SpringLayout.EAST, underlineChk);
      if(showEnterBtn)
      {
         layout.putConstraint(SpringLayout.WEST, enterBtn, 10, SpringLayout.EAST, superscriptMB);
         layout.putConstraint(SpringLayout.EAST, widgetPanel, 0, SpringLayout.EAST, enterBtn);
      }
      else 
         layout.putConstraint(SpringLayout.EAST, widgetPanel, 0, SpringLayout.EAST, superscriptMB);
      if(nLines == 1)
         widgetPanel.setBorder(BorderFactory.createCompoundBorder(
               BorderFactory.createEmptyBorder(1, 3, 1, 0), 
               BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                     BorderFactory.createEmptyBorder(0,0,0,2)))
               );

      String vCtr = SpringLayout.VERTICAL_CENTER;
      if(nLines > 1)
      {
         layout.putConstraint(SpringLayout.NORTH, textColorPicker, 0, SpringLayout.NORTH, widgetPanel);
         layout.putConstraint(SpringLayout.SOUTH, widgetPanel, 0, SpringLayout.SOUTH, textColorPicker);
      }
      else
         layout.putConstraint(vCtr, textColorPicker, 0, vCtr, widgetPanel);
      layout.putConstraint(vCtr, fontStyleMB, 0, vCtr, textColorPicker);
      layout.putConstraint(vCtr, underlineChk, 0, vCtr, textColorPicker);
      layout.putConstraint(vCtr, superscriptMB, 0, vCtr, textColorPicker);
      if(showEnterBtn)
         layout.putConstraint(vCtr, enterBtn, 0, vCtr, widgetPanel);
      
      if(nLines == 1)
      {
         toggleWidgetVisBtn = new JButton(openWidgetsIcon);
         toggleWidgetVisBtn.setBorder(BorderFactory.createCompoundBorder(
               BorderFactory.createEmptyBorder(1, 2, 1, 2), 
               BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),
                     BorderFactory.createEmptyBorder(0,0,0,2)))
               );
         toggleWidgetVisBtn.setContentAreaFilled(false);
         toggleWidgetVisBtn.setFocusable(false);
         toggleWidgetVisBtn.addActionListener(this);
         
         widgetPanel.setVisible(false);
      }

      // left-right and top-bottom constraints...
      
      if(nLines > 1)
      {
         add(scrollPane);
         add(widgetPanel);
         layout = new SpringLayout();
         setLayout(layout);
         
         
         layout.putConstraint(SpringLayout.WEST, scrollPane, 0, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, scrollPane);
         layout.putConstraint(SpringLayout.EAST, widgetPanel, 0, SpringLayout.EAST, this);
         
         layout.putConstraint(SpringLayout.NORTH, widgetPanel, 0, SpringLayout.NORTH, this);
         layout.putConstraint(SpringLayout.NORTH, scrollPane, 5, SpringLayout.SOUTH, widgetPanel);
         layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, scrollPane);
      }
      else
      {
         setLayout(new BorderLayout());
         add(scrollPane, BorderLayout.CENTER);
         JPanel p = new JPanel(new BorderLayout());
         p.add(toggleWidgetVisBtn, BorderLayout.CENTER);
         p.add(widgetPanel, BorderLayout.WEST);
         add(p, BorderLayout.EAST);
      }
      
      setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getForeground().brighter().brighter()), 
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
      ));
   }
   
   /**
    * In the single-line configuration only, show or hide the attribute widget controls to the left of the editor's
    * text pane. THe user can also use the open/close button to interactively toggle the widgets' visibility. <i>This
    * method has no effect in the multi-line configuration.</i>
    * @param show True to show attribute widget controls, false to hide them.
    */
   public void showHideAttributeWidgets(boolean show)
   {
      if((nLines == 1) && show != widgetPanel.isVisible())
      {
         widgetPanel.setVisible(show);
         toggleWidgetVisBtn.setIcon(show ? closeWidgetsIcon : openWidgetsIcon);
      }
   }
   
   /**
    * Is the popup dialog associated with the RGB color picker widget currently raised?
    * @return True if color picker's popup dialog is raised, indicating the user is currently selecting a color.
    */
   public boolean isColorPickerPopupRaised()
   {
      return textColorPicker.isEditing();
   }
   
   /**
    * Adjust the scroll pane height to accommodate the configured number of text lines in the embeded text pane without 
    * vertical scrolling. To make room for both superscripted and subscripted text on any line, the line height is 
    * estimated as 1.3 x the font height, for the font currently installed in the styled document in the text pane.
    * 
    * <p>This may not work for all fonts. Also, depending on the layout manager for the editor's container, the change
    * in scroll pane size may be ignored.</p>
    */
   private void fixScrollPaneHeight()
   {
      AttributeSet as = textPane.getStyledDocument().getCharacterElement(0).getAttributes();
      String fam = StyleConstants.getFontFamily(as);
      int sz =  StyleConstants.getFontSize(as);
      
      Font f = new Font(fam, Font.PLAIN, sz);
      FontMetrics fm = getFontMetrics(f);
      sz = fm.getHeight();

      Dimension d = scrollPane.getSize();
      d.height = (int) (1.3 * sz * nLines);
      d.height += ((Integer) UIManager.get("ScrollBar.width")) + 4;  // for horizontal scroll bar.
      scrollPane.setSize(d);
      scrollPane.setPreferredSize(d);
      revalidate();
   }
   
   /**
    * Load and configure the editor's content.
    * 
    * @param as The attributed string. Null is treated as an empty string.
    * @param fam The font family for the editor. Applies to the entire text content. If not installed on host system, an
    * installed sanserif font family is used instead.
    * @param size The font size for the editor in points. Applies to the entire text content. Allowed range is 8-48; set
    * to a default size if out of this range.
    * @param textC The default text color. Null is interpreted as opaque black.
    * @param fontStyle The default font style. Null is interpreted as the plain font style.
    */
   public void loadContent(AttributedString as, String fam, int size, Color textC, FontStyle fontStyle)
   {
      if(matchesCurrentContents(as)) return;
      
      if(as == null) as = new AttributedString("");
      AttributedCharacterIterator aci = as.getIterator();
      
      // first, get the actual text
      StringBuilder textBuf = new StringBuilder(aci.getEndIndex());
      for(char c=aci.first(); c != AttributedCharacterIterator.DONE; c=aci.next())
         textBuf.append(c);
      String text = textBuf.toString();
      
      // put the extracted character sequence in the text pane
      textPane.setText(text);
      
      // initial text attributes: specified font family and size (which don't change); specified defaults for text color
      // and font style; underline off; super/subscript off.
      implicitTextColor = (textC == null) ? Color.black : textC;
      implicitFontStyle = (fontStyle == null) ? FontStyle.PLAIN : fontStyle;
      if(!LocalFontEnvironment.isFontInstalled(fam)) fam = LocalFontEnvironment.getSansSerifFont();
      size = (size < 8 || size > 48) ? DEF_FONTSZ : size;
      SimpleAttributeSet sas = new SimpleAttributeSet();
      StyleConstants.setFontFamily(sas, fam);
      StyleConstants.setFontSize(sas, size);
      StyleConstants.setForeground(sas, implicitTextColor);
      StyleConstants.setBold(sas, implicitFontStyle.isBold());
      StyleConstants.setItalic(sas, implicitFontStyle.isItalic());
      StyleConstants.setUnderline(sas, false);
      StyleConstants.setSubscript(sas, false);
      StyleConstants.setSuperscript(sas, false);
      
      // if text is an empty string, install initial text attributed as the input attributes on the text pane, and
      // initialize style widgets to reflect these input attributes.
      if(text.isEmpty())
      {
         textPane.setCharacterAttributes(sas, false);

         textColorPicker.setCurrentColor(implicitTextColor, false);
         fontStyleMB.setCurrentChoice(implicitFontStyle);
         underlineChk.setSelected(false);
         superscriptMB.setCurrentChoice(NORMSCRIPT);
         
         fixScrollPaneHeight();
         return;
      }

      // for non-empty text string, first apply the font family and size (which don't change) and the style defaults to 
      // the entire document
      StyledDocument doc = textPane.getStyledDocument();
      int len = doc.getLength();
      doc.setCharacterAttributes(0, len, sas, false);

      // then apply style changes as they occur in the attributed string...
      Color prevTextC = implicitTextColor;
      Float prevWeight = implicitFontStyle.isBold() ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR;
      Float prevPosture = implicitFontStyle.isItalic() ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR;
      Integer prevUL = UNDERLINE_OFF;
      Integer prevScript = NORMSCRIPT;

      for(char c=aci.first(); c != AttributedCharacterIterator.DONE; c=aci.next())
      {
         sas = new SimpleAttributeSet();

         Object value = aci.getAttribute(TextAttribute.FOREGROUND);
         if((value instanceof Color) && !prevTextC.equals(value))
         {
            StyleConstants.setForeground(sas, (Color) value);
            prevTextC = (Color) value;
         }
         
         value = aci.getAttribute(TextAttribute.WEIGHT);
         Float weight = (value != null) ? ((Float) value) : prevWeight;
         if(!prevWeight.equals(weight))
         {
            StyleConstants.setBold(sas, !weight.equals(TextAttribute.WEIGHT_REGULAR));
            prevWeight = weight;
         }
         value = aci.getAttribute(TextAttribute.POSTURE);
         Float posture = (value != null) ? ((Float) value) : prevPosture;
         if(!prevPosture.equals(posture))
         {
            StyleConstants.setItalic(sas, !posture.equals(TextAttribute.POSTURE_REGULAR));
            prevPosture = posture;
         }
         
         value = aci.getAttribute(TextAttribute.UNDERLINE);
         if((value instanceof Integer) && !prevUL.equals(value))
         {
            Integer ul = (Integer) value;
            StyleConstants.setUnderline(sas, !ul.equals(UNDERLINE_OFF));
            prevUL = ul;
         }

         value = aci.getAttribute(TextAttribute.SUPERSCRIPT);
         if((value instanceof Integer) && !prevScript.equals(value))
         {
            int script = (Integer) value;
            StyleConstants.setSuperscript(sas, (script > 0));
            StyleConstants.setSubscript(sas, (script < 0));
            prevScript = script;
         }
         
         // if there were any attribute changes at the current character, apply them to the text pane doc.
         if(!sas.isEmpty())
         {
            int start = aci.getIndex();
            doc.setCharacterAttributes(start, len-start, sas, false);
         }
      }
      
      fixScrollPaneHeight();
      
      // move caret to end of document -- hopefully the style widgets are updated accordingly
      textPane.setCaretPosition(len);
   }

   /** Text attributes checked when comparing an attributed string against current contents of editor. */
   static final TextAttribute[] textAttrs = new TextAttribute[] {
         TextAttribute.FOREGROUND, TextAttribute.FAMILY, TextAttribute.POSTURE, TextAttribute.WEIGHT, 
         TextAttribute.SUPERSCRIPT, TextAttribute.UNDERLINE
   };
   
   /**
    * Helper method for {@link #loadContent(AttributedString, String, int, Color, FontStyle)}.
    * @param as An attributed string.
    * @return True if argument matches the current contents of this editor.
    */
   private boolean matchesCurrentContents(AttributedString as)
   {
      if(as == null) return(false);
      
      AttributedCharacterIterator aci = as.getIterator();
      AttributedCharacterIterator currACI = getCurrentContents().getIterator();
      if((aci.getBeginIndex() != currACI.getBeginIndex()) || (aci.getEndIndex() != currACI.getEndIndex()))
         return(false);
      
      
      for(int i=aci.getBeginIndex(); i<aci.getEndIndex() ; i++)
      {
         char c = aci.setIndex(i);
         if(c != currACI.setIndex(i))
            return(false);
         
         for(TextAttribute ta : textAttrs)
         {
            Object currVal = currACI.getAttribute(ta);
            Object value = aci.getAttribute(ta);
            if(currVal == null || !currVal.equals(value)) return(false);
         }
      }
      return(true);
   }
   
   /**
    * Set the font family for the editor. Its current content is updated accordingly.
    * 
    * @param family The font family name. No action taken if the specified font is not installed on the system.
    * @param notify If true and the current text is not an empty string, an action event is fired to indicate that the
    * attributed text may have changed.
    */
   public void setFontFamily(String family, boolean notify)
   {
      if(!LocalFontEnvironment.isFontInstalled(family)) return;
      SimpleAttributeSet sas = new SimpleAttributeSet();
      StyleConstants.setFontFamily(sas, family);
      StyledDocument doc = textPane.getStyledDocument();
      if(doc.getLength() == 0)
         textPane.setCharacterAttributes(sas, false);
      else
      {
         doc.setCharacterAttributes(0, doc.getLength(), sas, false);
         if(notify) fireActionEvent();
      }
      fixScrollPaneHeight();
   }
   
   /**
    * Set the font size for the the editor. Its current content is updated accordingly.
    * 
    * @param sz The font size in typographical points. Default font size used if outside range[8..48].
    * @param notify If true and the current text is not an empty string, an action event is fired to indicate that the
    * attributed text may have changed.
    */
   public void setFontSize(int sz, boolean notify)
   {
      int adjSz = (sz < 8 || sz > 48) ? DEF_FONTSZ : sz;
      SimpleAttributeSet sas = new SimpleAttributeSet();
      StyleConstants.setFontSize(sas, adjSz);
      StyledDocument doc = textPane.getStyledDocument();
      if(doc.getLength() == 0)
         textPane.setCharacterAttributes(sas, false);
      else
      {
         doc.setCharacterAttributes(0, doc.getLength(), sas, false);
         if(notify) fireActionEvent();
      }
      
      fixScrollPaneHeight();
   }
   
   /**
    * Set the default text color for the editor. Its current content is updated accordingly. The current selection is
    * cleared and the caret is moved to the end of the current text content.
    * 
    * @param c The default color. If null, {@link Color#black} is assumed.
    * @param notify If true and the current text is not an empty string, an action event is fired to indicate that the
    * attributed text may have changed.
    */
   public void setDefaultTextColor(Color c, boolean notify)
   {
      if(c == null) c = Color.black;
      if(c == implicitTextColor) return;
      Color oldTextC = implicitTextColor;
      implicitTextColor = c;
      
      SimpleAttributeSet textColorSas = new SimpleAttributeSet();
      StyleConstants.setForeground(textColorSas, implicitTextColor);
      
      StyledDocument doc = textPane.getStyledDocument();
      int len = doc.getLength();
      if(len == 0)
      {
         // no text content. Update text pane input attributes and the text color widget.
         textColorPicker.setCurrentColor(implicitTextColor, false);
         textPane.setCharacterAttributes(textColorSas, false);
         return;
      }
      
      // for each character rendered in the previous default color, replace it with the new default color.
      for(int i=0; i<len; i++)
      {
         AttributeSet as = doc.getCharacterElement(i).getAttributes();
         if(StyleConstants.getForeground(as).equals(oldTextC)) 
            doc.setCharacterAttributes(i, 1, textColorSas, false);
            
      }
      
      // clear selection and move caret to end of document
      textPane.setSelectionEnd(len);
      textPane.setSelectionStart(len);
      textPane.setCaretPosition(len);
      
      if(notify) fireActionEvent();
   }
   
   /**
    * Get the editor's current default text color.
    * 
    * @return The default color.
    */
   public Color getDefaultTextColor() { return( implicitTextColor ); }
   
   /**
    * Set the default font style for the editor. Its current content is updated accordingly.
    * 
    * @param fs The default font style. If null, {@link FontStyle#PLAIN} is assumed.
    * @param notify If true and the current text is not an empty string, an action event is fired to indicate that the
    * attributed text may have changed.
    */
   public void setDefaultFontStyle(FontStyle fs, boolean notify)
   {
      if(fs == null) fs = FontStyle.PLAIN;
      if(fs == implicitFontStyle) return;
      FontStyle oldFS = implicitFontStyle;
      implicitFontStyle = fs;
      
      SimpleAttributeSet fontStyleSas = new SimpleAttributeSet();
      StyleConstants.setBold(fontStyleSas, implicitFontStyle.isBold());
      StyleConstants.setItalic(fontStyleSas, implicitFontStyle.isItalic());
      
      StyledDocument doc = textPane.getStyledDocument();
      int len = doc.getLength();
      if(len == 0)
      {
         // no text content. Update text pane input attributes and the font style widget.
         fontStyleMB.setCurrentChoice(implicitFontStyle);
         textPane.setCharacterAttributes(fontStyleSas, false);
         return;
      }

      // for each character rendered in the previous default font style, replace it with the new default font style.
      for(int i=0; i<len; i++)
      {
         AttributeSet as = doc.getCharacterElement(i).getAttributes();
         if((StyleConstants.isBold(as) == oldFS.isBold()) && (StyleConstants.isItalic(as) == oldFS.isItalic())) 
            doc.setCharacterAttributes(i, 1, fontStyleSas, false);
            
      }
      
      // clear selection and move caret to end of document
      textPane.setSelectionEnd(len);
      textPane.setSelectionStart(len);
      textPane.setCaretPosition(len);
      
      if(notify) fireActionEvent();
   }
   
   /**
    * Get the editor's current default font style.
    * 
    * @return The default fonts style.
    */
   public FontStyle getDefaultFontStyle() { return( implicitFontStyle ); }

   /**
    * Get the attributed string currently displayed in this editor.
    * 
    * <p>The editor's configured font family ({@link TextAttribute#FAMILY}) and size ({@link TextAttribute#SIZE}) will
    * apply across the entire string, while the following attributes may vary from character to character:
    * <ul>
    * <li>{@link TextAttribute#WEIGHT} : Text weight - bold or regular.</li>
    * <li>{@link TextAttribute#POSTURE} : Text posture - oblique or regular.</li>
    * <li>{@link TextAttribute#FOREGROUND} : Text color.</li>
    * <li>{@link TextAttribute#UNDERLINE} : Underline - off or on.</li>
    * <li>{@link TextAttribute#SUPERSCRIPT}: Superscript, subscript, or normal text.</li>
    * </ul>
    * No other text attributes may be set or changed within this editor.</p>
    * 
    * <p>NOTE: If there is no text in the editor, an empty attributed string, with no characters AND no text attributes,
    * is returned. By design, the editor does not let the user enter leading whitespace, but it must allow trailing 
    * whitespace --  for example, if the user wants to separate two lines of text with a blank line. <b>However, the 
    * attributed string returned by this method will NOT include any trailing whitespace.</b></p>
    * 
    * @return The current content of the editor as an attributed string.
    */
   public AttributedString getCurrentContents()
   {
      StyledDocument doc = textPane.getStyledDocument();
      String text = textPane.getText();
      text = (text == null) ? "" : text.trim();
      
      // note: you can't apply any text attributes to an empty character sequence.
      AttributedString as = new AttributedString(text);
      int len = text.length();
      if(len == 0)
         return(as);
      
      Color prevTextC = implicitTextColor;
      FontStyle prevFontStyle = implicitFontStyle;
      Integer prevUL = UNDERLINE_OFF;
      Integer prevScript = NORMSCRIPT;
      
      // font family and font size apply to the entire string. Initially apply default text color, font style, underline
      // and superscript state to the entire string.
      AttributeSet attrs = doc.getCharacterElement(0).getAttributes();
      as.addAttribute(TextAttribute.FAMILY, StyleConstants.getFontFamily(attrs));
      as.addAttribute(TextAttribute.SIZE, StyleConstants.getFontSize(attrs));
      as.addAttribute(TextAttribute.FOREGROUND, prevTextC);
      as.addAttribute(TextAttribute.WEIGHT, 
            prevFontStyle.isBold() ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR);
      as.addAttribute(TextAttribute.POSTURE,
            prevFontStyle.isItalic() ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR);
      as.addAttribute(TextAttribute.UNDERLINE, prevUL);
      as.addAttribute(TextAttribute.SUPERSCRIPT, prevScript);

      for(int i=0; i<len; i++)
      {
         attrs = doc.getCharacterElement(i).getAttributes();
         
         Color c = StyleConstants.getForeground(attrs);
         if(!prevTextC.equals(c))
         {
            as.addAttribute(TextAttribute.FOREGROUND, c, i, len);
            prevTextC = c;
         }
         
         FontStyle fs = FontStyle.getFontStyle(StyleConstants.isBold(attrs), StyleConstants.isItalic(attrs));
         if(prevFontStyle != fs)
         {
            as.addAttribute(TextAttribute.WEIGHT, 
                  fs.isBold() ? TextAttribute.WEIGHT_BOLD : TextAttribute.WEIGHT_REGULAR, i, len);
            as.addAttribute(TextAttribute.POSTURE,
                  fs.isItalic() ? TextAttribute.POSTURE_OBLIQUE : TextAttribute.POSTURE_REGULAR, i, len);
            prevFontStyle = fs;
         }
         
         Integer ul = StyleConstants.isUnderline(attrs) ? TextAttribute.UNDERLINE_ON : UNDERLINE_OFF;
         if(!prevUL.equals(ul))
         {
            as.addAttribute(TextAttribute.UNDERLINE, ul, i, len);
            prevUL = ul;
         }
         
         Integer script = StyleConstants.isSuperscript(attrs) ? TextAttribute.SUPERSCRIPT_SUPER : 
            (StyleConstants.isSubscript(attrs) ? TextAttribute.SUPERSCRIPT_SUB : NORMSCRIPT);
         if(!prevScript.equals(script))
         {
            as.addAttribute(TextAttribute.SUPERSCRIPT, script, i, len);
            prevScript = script;
         }
      }

      return(as);
   }
   
   /**
    * Insert specified string at the current caret position in the editor's text pane. If any text is currently
    * selected in the text pane, that text is replaced.
    * 
    * @param s The string to insert. No action taken if null or empty.
    * @param notify If true and the inserted string is not empty, an action event is fired to indicate that the 
    * attributed text has changed.
    */
   public void insertString(String s, boolean notify)
   {
      if(s != null && (!s.isEmpty()))
      {
         textPane.replaceSelection(s);
         if(notify) fireActionEvent();
      }
   }
   
   @Override public boolean isRequestFocusEnabled() { return(textPane.isRequestFocusEnabled()); }
   @Override public void requestFocus() { textPane.requestFocus(); }
   @Override public boolean requestFocusInWindow() { return(super.requestFocusInWindow()); }
   @Override public void setRequestFocusEnabled(boolean b) { textPane.setRequestFocusEnabled(b); }
   @Override public boolean hasFocus() { return(textPane.hasFocus()); }


   @Override public void setToolTipText(String text)
   {
      textPane.setToolTipText(text);
   }

   /**
    * Whenever the caret position changes in the text pane, update the various style widgets to reflect the current
    * text attributes at that position.
    */
   @Override public void caretUpdate(CaretEvent e)
   {
      if(e.getSource() == textPane)
      {
         SwingUtilities.invokeLater(() -> {
            int pos = textPane.getCaretPosition();
            int start = (textPane.getSelectedText() != null) ? textPane.getSelectionStart() : -1;
            StyledDocument doc = textPane.getStyledDocument();
            int len = doc.getLength();
            pos = (start > -1) ? start : (pos > 0 ? (pos-1) : 0);
            AttributeSet attrs =
                  (len == 0) ? textPane.getInputAttributes() : doc.getCharacterElement(pos).getAttributes();

            // font family and size should not vary with the position of the caret, but we do this anyway...
            textColorPicker.setCurrentColor(StyleConstants.getForeground(attrs), false);
            boolean bold = StyleConstants.isBold(attrs);
            boolean italic = StyleConstants.isItalic(attrs);
            FontStyle fs = bold ? (italic ? FontStyle.BOLDITALIC : FontStyle.BOLD) :
               (italic ? FontStyle.ITALIC : FontStyle.PLAIN);
            fontStyleMB.setCurrentChoice(fs);
            Integer sup = StyleConstants.isSuperscript(attrs) ? TextAttribute.SUPERSCRIPT_SUPER :
               (StyleConstants.isSubscript(attrs) ? TextAttribute.SUPERSCRIPT_SUB : NORMSCRIPT);
            superscriptMB.setCurrentChoice(sup);
            underlineChk.setSelected(StyleConstants.isUnderline(attrs));
         });
      }
   }

   /**
    * Updates current text pane attributes when a change is made in the font style or superscript state.
    */
   @Override public void itemStateChanged(ItemEvent e)
   {
      Object src = e.getSource();
      if(src == fontStyleMB) onFontStyleChanged();
      else if(src == superscriptMB) onSuperscriptStateChanged();
   }

   /** Updates current text pane attributes whenever the user selects a new color in the color picker widget. */
   @Override public void propertyChange(PropertyChangeEvent e)
   {
      String prop = e.getPropertyName();
      Object src = e.getSource();
      if(src == textColorPicker)
      {
         if(prop.equals(RGBColorPicker.COLOR_PROPERTY))
         {
            SimpleAttributeSet sas = new SimpleAttributeSet();
            StyleConstants.setForeground(sas, textColorPicker.getCurrentColor());
            textPane.setCharacterAttributes(sas, false);
            if((textPane.getSelectionStart() < textPane.getSelectionEnd()) && !notifyOnlyOnEnter)
               fireActionEvent();
         }
      }
      
   }
   
   /**
    * Updates current text pane attributes whenever user clicks the underline checkbox or the "enter" button.
    */
   @Override public void actionPerformed(ActionEvent e)
   {
      Object src = e.getSource();
      if(src == enterBtn)
         fireActionEvent();
      else if(src == toggleWidgetVisBtn)
      {
         showHideAttributeWidgets(!widgetPanel.isVisible());
      }
      else if(src == underlineChk)
      {
         SimpleAttributeSet sas = new SimpleAttributeSet();
         StyleConstants.setUnderline(sas, underlineChk.isSelected());
         
         textPane.setCharacterAttributes(sas, false);
         if((textPane.getSelectionStart() < textPane.getSelectionEnd()) && !notifyOnlyOnEnter)
            fireActionEvent();
      }
   }

   @Override public void focusGained(FocusEvent e) 
   {
   }

   /**
    * Whenever the text pane permanently loses the keyboard focus, fire an action event to indicate that the current
    * attributed text may have changed.
    */
   @Override public void focusLost(FocusEvent e)
   {
      if(!e.isTemporary()) fireActionEvent();
   }


   /** The action listeners registered with this editor. */
   private ActionListener actionListener = null;

   /**
    * Add the specified action listener to this editor's listener list. Registered action listeners are notified 
    * whenever the user hits the "Enter" key while the focus is within the text pane, or whenever the text pane loses
    * the permanent keyboard focus.
    * @param l The listener to add. If <b>null</b>, method has no effect.
    */
   public synchronized void addActionListener(ActionListener l) 
   {
      actionListener = AWTEventMulticaster.add(actionListener, l);
   }
   
   /**
    * Remove the specified action listener from this editor's listener list.
    * @param l The listener to remove. If <code>null</code> or not a registered listener, method has no effect.
    */
   public synchronized void removeActionListener(ActionListener l) 
   {
      actionListener = AWTEventMulticaster.remove(actionListener, l);
   }
   
   /** Send an action event to all registered listeners, indicating that the value in this editor has changed. */
   private void fireActionEvent() 
   {
       ActionListener listener = actionListener;
       if(listener != null) listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
   }
   
   private final static Integer NORMSCRIPT = 0;
   private final static Integer UNDERLINE_OFF = -1;

   /**
    * Update character attributes at current position in text pane whenever the current font style changes. If some
    * text is selected, that text is updated and an action event is fired to notify action listeners that the content
    * of this editor has changed.
    */
   private void onFontStyleChanged()
   {
      FontStyle fontStyle = fontStyleMB.getCurrentChoice();
      SimpleAttributeSet sas = new SimpleAttributeSet();
      StyleConstants.setBold(sas, fontStyle.isBold());
      StyleConstants.setItalic(sas, fontStyle.isItalic());
      textPane.setCharacterAttributes(sas, false);
      if((textPane.getSelectionStart() < textPane.getSelectionEnd()) && !notifyOnlyOnEnter)
         fireActionEvent();
   }
   
   /**
    * Update character attributes at current position in text pane whenever the current super/subscript state changes. 
    * If some text is selected, that text is updated and an action event is fired to notify action listeners that the 
    * content of this editor has changed.
    */
   private void onSuperscriptStateChanged()
   {
      Integer choice = superscriptMB.getCurrentChoice();
      SimpleAttributeSet sas = new SimpleAttributeSet();
      StyleConstants.setSubscript(sas, choice.equals(TextAttribute.SUPERSCRIPT_SUB));
      StyleConstants.setSuperscript(sas, choice.equals(TextAttribute.SUPERSCRIPT_SUPER));
      textPane.setCharacterAttributes(sas, false);
      if((textPane.getSelectionStart() < textPane.getSelectionEnd()) && !notifyOnlyOnEnter)
         fireActionEvent();
   }
   
   /**
    * A custom document filter which removes carriage returns and tabs, and accepts linefeeds only if the editor is 
    * configured to allow multiple text lines. It also does not allow any whitespace at the start of the document.
    * Whitespace is allowed at the end of the document.
    * 
    * @author sruffner
    *
    */
   class DocFilter extends DocumentFilter
   {
      @Override public void remove(FilterBypass fb, int ofs, int length) throws BadLocationException
      {
         replace(fb, ofs, length, "", null);
      }

      @Override public void insertString(FilterBypass fb, int ofs, String s, AttributeSet attr)
            throws BadLocationException
      {
         replace(fb, ofs, 0, s, attr);
      }

      @Override public void replace(FilterBypass fb, int ofs, int length, String s, AttributeSet attrs)
            throws BadLocationException
      {
         if(s == null) s = "";
         
         // remove any unsupported characters from the input string
         String regex = (nLines == 1) ? "[\t\r\f\n]" : "[\t\r\f]";
         s = s.replaceAll(regex, "");
         
         // if the final result is an empty string, that's ok
         int currDocLen = fb.getDocument().getLength();
         if(currDocLen - length + s.length() == 0)
         {
            super.replace(fb, 0, currDocLen, "", attrs);
            return;
         }
         
         // construct the result after replacement
         String currText = fb.getDocument().getText(0, currDocLen);
         String res = currText.substring(0, ofs) + s + currText.substring(ofs + length);
         
         // prevent edit if the resulting string starts with a whitespace character
         if((!res.isEmpty()) && Character.isWhitespace(res.charAt(0)))
         {
            Toolkit.getDefaultToolkit().beep();
         }
         else
            super.replace(fb, ofs, length, s, attrs);
      }
      
   }
}
