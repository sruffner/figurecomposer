package com.srscicomp.common.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;



/**
 * <code>FUnicodeField</code> is a compound component for editing a single-line text label that may (or may not) 
 * include Unicode characters that are not readily accessible at the keyboard. It includes an embedded text field for 
 * entering normal text and a button for raising a popup dialog by which the user can add special characters to the 
 * label text. The utility class <code>JUnicodeCharacterMap</code> implements the character map.
 * 
 * <p><code>FUnicodeField</code> can optionally restrict the length of text entered. Whenever the entered text reaches 
 * the maximum length <em>N</em>, additional input is ignored.</p>
 * 
 * @see JUnicodeCharacterMap
 * @author sruffner
 */
public class FUnicodeField extends JPanel implements ActionListener, FocusListener
{
   private static final long serialVersionUID = 1L;

   /** Icon for button that raises the character map dialog. */
   private final static Icon CHARMAPICON = 
         GUIUtilities.createImageIcon(FUnicodeField.class, "/com/srscicomp/common/resources/charmap.gif", "");

   /** Text field in which label is displayed/edited. */
   private final JTextField labelTextField;

   /** Pressing this button raises the character mapper popup. */
   private JButton showMapperBtn = null;

   /**
    * Construct a <code>FUnicodeField</code>. No restriction is imposed on the length of text entered into the field.
    * 
    * @param cols Number of text columns in the embedded text field, which controls the preferred width of the field.
    * If non-positive, the field will grow to fill the width made available by its container's layout manager.
    * @param charSets Array of character subsets to make available to user when this <code>FUnicodeField</code> raises the
    * character map popup dialog. Each such subset is typically a related group of characters, such as Latin, Greek,
    * punctuation, mathematical symbols, etc. If <code>null</code> or empty, several default character sets will be 
    * listed in the character map.
    */
   public FUnicodeField(int cols, UnicodeSubset[] charSets)
   {
      super();

      allowedCharSets = charSets;
      
      labelTextField = new JTextField(Math.max(cols, 0));
      labelTextField.setToolTipText("Enter label text here");
      labelTextField.addActionListener(this);
      labelTextField.addFocusListener(this);
      
      showMapperBtn = new JButton(CHARMAPICON);
      showMapperBtn.setMargin(new Insets(0,0,0,0));
      showMapperBtn.setOpaque(false);  // needed b/c default bkg shows through in Windows
      showMapperBtn.setToolTipText("Choose extended characters...");
      showMapperBtn.addActionListener(this);
      showMapperBtn.setFocusable(false);
      
      setLayout(new BorderLayout());
      add(labelTextField, BorderLayout.CENTER);
      add(showMapperBtn, BorderLayout.EAST);
   }

   /**
    * Overridden to install the tool tip text on the embedded text field.
    * @see javax.swing.JComponent#setToolTipText(String)
    */
   @Override
   public void setToolTipText(String text) { labelTextField.setToolTipText(text); }
   
   /** 
    * Array of Unicode character subsets installed in the character map popup raised by this <code>FUnicodeField</code>. 
    * If <code>null</code> or empty, the character map will include standard Latin, Greek and punctuation characters.
    */
   private UnicodeSubset[] allowedCharSets;
   
   /**
    * Set the array of character subsets from which the user can choose when selecting a character from the character 
    * map popup dialog raised by this <code>FUnicodeField</code>. Note that this property does not restrict characters 
    * input directly into <code>FUnicodeField</code>; it only determines the character sets from which the user can 
    * select when using the popup dialog.
    * 
    * <p>Since the popup dialog is shared by all <code>FUnicodeField</code> instances in the application, calling this
    * method will not affect the current contents of the popup if it is already visible. The new character sets will be
    * available the next time the popup is raised by this <code>FUnicodeField</code> instance.</p>
    * @param sets Array of character subsets to make available to user when this <code>FUnicodeField</code> raises the
    * character map popup dialog. Each such subset is typically a related group of characters, such as Latin, Greek,
    * punctuation, mathematical symbols, etc. If <code>null</code> or empty, several default character sets will be 
    * listed in the character map.
    */
   public void setAllowedCharSets(UnicodeSubset[] sets) { allowedCharSets = sets; }
   
   /** The maximum text input length. If 0, no restriction is imposed. */
   private int maxInputLength = 0;

   /**
    * Is this <code>FUnicodeField</code> configured to restrict the length of text entered into it?
    * @return <code>True</code> iff input length is restricted.
    */
   public boolean isLengthRestricted() { return(maxInputLength > 0); }

   /**
    * Get the maximum length of a text entry permitted by this <code>FUnicodeField</code>.
    * @return The maximum length; if 0, then no length restriction is imposed.
    */
   public int getMaxInputLength() { return(maxInputLength); }

   /**
    * Set the maximum length of a text entry permitted by this <code>FUnicodeField</code>.
    * 
    * @param len The maximum length <em>N</em> allowed. If <em>N&lt;=0</em>, then no length restriction is enforced. 
    * Otherwise, if the current text entry length is more than <em>N</em>, then only the last <em>N</em> characters of 
    * the entry are preserved.
    * @return <code>True</code> if successful; <code>false</code> otherwise.
    */
   public boolean setMaxInputLength(int len)
   {
      Document d = labelTextField.getDocument();
      if(!(d instanceof AbstractDocument)) return(false);
      
      int maxLen = Math.max(len, 0);
      if(maxInputLength != maxLen)
      {
         maxInputLength = maxLen;
         ((AbstractDocument)d).setDocumentFilter((maxInputLength>0) ? new MaxLengthDocumentFilter(maxInputLength) : null);
         if(maxInputLength > 0)
         {
            String s = labelTextField.getText();
            if(s.length() > maxInputLength) labelTextField.setText(s.substring(s.length()-maxInputLength));
         }
      }
      return(true);
   }

   /** The font in which text is rendered. Character map popup only includes characters displayable in this font. */
   private Font displayFont = null;
   
   /**
    * Get the font in which text will be rendered. <em>This is NOT the <code>FUnicodeField</code>'s component 
    * font.</em> The character map popup dialog uses this font to restrict its content: only supported <em>Phyplot</em> 
    * characters that are displayable in this font will be included in the character map.
    * 
    * @return The display font.
    */
   public Font getDisplayFont() { return((displayFont != null) ? displayFont : labelTextField.getFont()); }

   /**
    * Set the font in which text will be rendered.
    * 
    * @param f The new display font.
    * @see #getDisplayFont()
    */
   public void setDisplayFont(Font f) { displayFont = f; }
   
   /**
    * Get the current text entered in this <code>FUnicodeField</code>.
    * @return The current text.
    */
   public String getText() { return( labelTextField.getText() ); }
   
   /**
    * Set the current text in this <code>FUnicodeField</code>.
    * 
    * <p>This method is intended to programmatically load the field's current text. Registered property change 
    * listeners are <strong>NOT</strong> notified that the field's value has changed. The text is initially 
    * selected.</p>
    * 
    * @param s The new text. If <code>null</code>, an empty string is assumed.
    */
   public void setText(String s) 
   { 
      labelTextField.setText((s==null) ? "" : s); 
      labelTextField.selectAll();
   }

   /** Transfers focus to the embedded text field, which will automatically select all of the text in that field. */
   @Override  public boolean requestFocusInWindow() { return(labelTextField.requestFocusInWindow()); }

   /** Property name bound to the current text content of a <code>FUnicodeField</code>. */
   public final static String VALUE_PROPERTY = "value";

   /**
    * Helper method updates the current text content of this <code>FUnicodeField</code> and notifies all registered 
    * property change listeners that its <code>VALUE_PROPERTY</code> has changed.
    * @param s The new text content for field.
    */
   private void setLabelAndNotify(String s)
   {
      String old = labelTextField.getText();
      String current = (s==null) ? "" : s;
      if(!old.equals(current))
      {
         labelTextField.setText(current);
         firePropertyChange(VALUE_PROPERTY, old, current);
      }
   }

   public void actionPerformed(ActionEvent e)
   {
      if(e.getSource() == showMapperBtn)
      {
         labelTextField.requestFocusInWindow();
         showCharacterMapperPopup(null);
      }
      else if(e.getSource() == labelTextField)
         firePropertyChange(VALUE_PROPERTY, null, labelTextField.getText());
   }
   public void focusGained(FocusEvent e) 
   {
      if(e.getSource() == labelTextField && !e.isTemporary()) labelTextField.selectAll();
   }
   public void focusLost(FocusEvent e)
   {
      if(e.getSource() == labelTextField && !e.isTemporary()) labelTextField.postActionEvent();
   }

   /** 
    * If not null, this is a reference to the character mapper popup window that is currently raised so that the user
    * can enter non-keyboard characters. It is set to null when the popup is extinguished. 
    */
   private CharMapPopup popupInProgress = null;
   
   /** 
    * Display the character mapper popup which lets user enter non-keyboard characters into this 
    * <code>FUnicodeField</code>.
    * 
    * @param bounds The desired location for the mapper popup's text field, in screen coordinates. The popup window's 
    * location and size will be adjusted so that its text field overlaps this boundary rectangle as much as possible, 
    * while still ensuring that the popup window is entirely onscreen. If <code>null</code>, the popup window is 
    * position so that its text field overlays this <code>FUnicodeField</code> itself.
    */
   public void showCharacterMapperPopup(Rectangle bounds) 
   { 
      cancelCharacterMapperPopup();
     
      Container c = getTopLevelAncestor();
      if(c instanceof Frame) popupInProgress = new CharMapPopup((Frame) c);
      else if(c instanceof Dialog) popupInProgress = new CharMapPopup((Dialog) c);
      else popupInProgress = new CharMapPopup((Frame) null);
      
      popupInProgress.popup(this, bounds, allowedCharSets); 
   }

   /**
    * If the Unicode text field's pop-up character map window is currently raised, extinguish it without changing the
    * current contents of this field.
    */
   public void cancelCharacterMapperPopup() { if(popupInProgress != null) popupInProgress.cancel(this); }
 
   /**
    * <code>CharMapPopup</code> implements the character map popup for <code>FUnicodeField</code>. It is an undecorated 
    * dialog that is raised directly on top of the invoking <code>FUnicodeField</code> (it is a singleton shared 
    * resource). 
    * 
    * <p>The popup panel includes a text field, an "OK" button to dismiss the popup, and the character map -- see 
    * <code>JUnicodeCharacterMap</code>. When raised, the current text from the invoking <code>FUnicodeField</code> is 
    * placed in the popup's text field. The content of the character map is restricted to characters available in the 
    * invoking field's "display font" which are supported in <em>Phyplot</em>, and the text field's font is set to a 
    * roughly 12pt version of this "display font". To add a character from the character map to the current text, the 
    * user simply double-clicks on any character field in the map.</p>
    * 
    * <p>Pressing the "OK" button or clicking outside the bounds of the popup will dismiss it. The keyboard focus 
    * returns to the invoking <code>FUnicodeField</code>, the content of which is updated to reflect any changes the 
    * user made via the <code>CharMapPopup</code>.</p>
    * 
    * @see JUnicodeCharacterMap
    * @author  sruffner
    */
   private static class CharMapPopup extends JDialog 
         implements ActionListener, WindowFocusListener, PropertyChangeListener
   {
      private static final long serialVersionUID = 1L;
 
      /** The character map. */
      private JUnicodeCharacterMap mapper = null;

      /** Text field displaying the current text content. User can type into it directly or use the character map. */
      private JTextField text = null;

      /** An "OK" button to dismiss the popup. */
      private JButton okBtn = null;

      CharMapPopup(Frame owner) { super(owner, false); init(); }
      CharMapPopup(Dialog owner) { super(owner, false); init(); }
      void init()
      {
         setUndecorated(true);
         setAlwaysOnTop(true);
         addWindowFocusListener(this);

         mapper = new JUnicodeCharacterMap(null, null, 3, 8);
         mapper.addPropertyChangeListener(JUnicodeCharacterMap.SELCHAR_PROPERTY, this);

         text = new JTextField();
         text.addActionListener(this);
         okBtn = new JButton("OK");
         okBtn.addActionListener(this);
         
         JPanel textPanel = new JPanel();
         textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));
         textPanel.add(text);
         textPanel.add(Box.createRigidArea(new Dimension(5,0)));
         textPanel.add(okBtn);
         
         JPanel contentPane = new JPanel();
         contentPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(), 
               BorderFactory.createEmptyBorder(3,3,3,3)));
         contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
         contentPane.add(textPanel);
         contentPane.add(Box.createRigidArea(new Dimension(0,3)));
         contentPane.add(new JSeparator());
         contentPane.add(mapper);
         getContentPane().add(contentPane);
         pack();
      }

      /** The invoking <code>FUnicodeField</code>. */
      private FUnicodeField invoker = null;

      /**
       * Raise the <code>CharMapPopup</code> to edit the current contents of the specified <code>FUnicodeField</code>. 
       * @param ucField The <code>FUnicodeField</code> which raised the <code>CharMapPopup</code>. Its content will be 
       * updated when the popup is extinguished.
       * @param bounds The preferred bounds for the popup's text field, in screen coordinates. This is used to help 
       * position and size the popup window. It is only used as a guide; adjustments are made to ensure the popup is 
       * completely on screen. If it is <code>null</code>, the popup window is sized and positioned so that its 
       * text field overlays the invoker's text component.
       * @param charSets The array of character subsets that should be installed in the popup's character mapper. If 
       * <code>null</code> or empty, the character mapper's default character sets are provided.
       */
      void popup(FUnicodeField ucField, Rectangle bounds, UnicodeSubset[] charSets)
      {
         // if we're already visible and invoking FUnicodeField is null, then hide
         if(isVisible() && ucField == null)
         {
            invoker = null;
            setVisible(false);
            return;
         }

         invoker = ucField;
         
         mapper.setAvailableCharacterSets(charSets);
         
         // set size and position of popup so that it overlaps the invoking field. We try to position the popup and size 
         // its text field IAW the rectangle bounds provided. If that argument is null, we use the bounds of the 
         // invoker's embedded text component. The number of character columns in the character map is adjusted IAW the
         // preferred size of the popup so that user does not have to scroll it horizontally.
         Point pUL = (bounds == null) ? invoker.getLocationOnScreen() : bounds.getLocation();
         int invokerW = (bounds == null) ? invoker.labelTextField.getSize().width : bounds.width;
         Dimension screenSz = Toolkit.getDefaultToolkit().getScreenSize();
         
         Dimension fieldSz = new Dimension(Math.max(invokerW, 100), text.getHeight());
         text.setPreferredSize(fieldSz);
         Insets mapInsets = mapper.getInsets();
         int nMapCols = fieldSz.width + okBtn.getSize().width + 5 - mapInsets.left - mapInsets.right;
         nMapCols /= JUnicodeCharacterMap.FIXED_COL_W;
         mapper.setRowsAndColumns(3, nMapCols);
         pack();
         Dimension mySz = getPreferredSize();
         Insets myInsets = getInsets();
         
         pUL.x = pUL.x - myInsets.left;
         pUL.y = pUL.y - myInsets.top;
         if(pUL.y + mySz.height > screenSz.height)
         {
            pUL.y = screenSz.height - 1 - mySz.height;
            if(pUL.y < 0) pUL.y = 0;
         }
         if(pUL.x + mySz.width > screenSz.width)
         {
            pUL.x = screenSz.width - 1 - mySz.width;
            if(pUL.x < 0) pUL.x = 0;
         }
         setLocation(pUL);

         MaxLengthDocumentFilter docFilter = null;
         if(invoker.isLengthRestricted()) docFilter = new MaxLengthDocumentFilter(invoker.getMaxInputLength());
         ((AbstractDocument)text.getDocument()).setDocumentFilter(docFilter);
         
         text.setText(invoker.getText());

         Font f = invoker.getDisplayFont();
         text.setFont(f.deriveFont(Font.PLAIN, 12));
         mapper.setMappedFont(f);
         
         setVisible(true);
         text.requestFocusInWindow();
         text.selectAll();
      }

      /**
       * Hide the character map pop-up <i>without making any changes to the invoking Unicode text field</i>.
       * @param requestor The Unicode text field which requested cancellation. If this does not match the invoking
       * field, the pop-up will not be extinguished.
       */
      void cancel(FUnicodeField requestor)
      {
         if(invoker != null && invoker == requestor)
         {
            invoker.popupInProgress = null;
            invoker = null;
            setVisible(false);
            dispose();
         }
      }

      
      //
      // ActionListener, WindowFocusListener, PropertyChangeListener
      //
      
      /** Hide the <code>CharMapPopup</code> and update current text of invoking <code>FUnicodeField</code>. */
      private void extinguish()
      {
         setVisible(false);
         if(invoker != null)
         {
            invoker.popupInProgress = null;
            invoker.setLabelAndNotify(text.getText());
            invoker = null;
         }
         dispose();
      }

      public void actionPerformed(ActionEvent e) { if(e.getSource() == okBtn || e.getSource() == text) extinguish(); }
      public void windowGainedFocus(WindowEvent e) {}
      public void windowLostFocus(WindowEvent e) { if(e.getSource() == this) extinguish(); }

      public void propertyChange(PropertyChangeEvent e)
      {
         if(e.getSource() == mapper)
         {
            char c = mapper.getSelectedCharacter();
            if(c != 0)
            {
               String s = "" + c;
               text.replaceSelection(s);
            }
         }
      }
   }
}
