package com.srscicomp.common.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.srscicomp.common.util.Utilities;

/**
 * FontFamilyButton is a customized {@link JButton} by which the user chooses a font family from among the fonts
 * installed on the host machine. It includes support for manually entering a font that is not installed.
 * 
 * <p>While it's certainly conceivable to augment the standard combo box to implement font family selection, we decided 
 * to roll our own control: a custom-painted button with a pop-up "font family selector" panel. The button's text 
 * reflects the font family name and is rendered in that font -- unless the font is not installed on the system, in 
 * which case a "(?)" appears in front of the font name. A downward-pointing arrow along the right edge of the button 
 * serves as a clue that the font family can be selected from a "pop-up" list by pressing the button. The pop-up panel 
 * itself includes a list of all installed fonts on the system, some sample text rendered in the current font, and a 
 * text field for manually entering a font family that is not in the displayed list. A manually entered font family is
 * added to list, again with a "(?)" before the font name (in case the user needs to select it again the next time the 
 * pop-up is raised). The pop-up panel is a static resource shared by all instances of <b>FontFamilyButton</b>.</p>
 * 
 * <p><b>FontFamilyButton</b> is intended to be a compact control. It has a fixed width that can be specified at
 * construction time, and a fixed height determined also at construction time. <i>Do not change the font for this
 * widget, since it does so automatically whenever a different font family is selected by the user (and it uses the
 * font size of the button's original font).</i></p>
 * 
 * <p>To listen for changes in the font family selector button's current value, register as a {@link 
 * java.beans.PropertyChangeListener} for the property tag {@link #FONTFAMILY_PROPERTY}.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
public class FontFamilyButton extends JButton implements ActionListener
{
   /**
    * TEMPORARY -- Will use this for testing purposes...
    * @param args Command-line arguments (not used).
    */
   public static void main(String[] args)
   {
      GUIUtilities.initLookAndFeel();
      LocalFontEnvironment.initialize();
      
      final JFrame appFrame = new JFrame();
      appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      
      FontFamilyButton ffBtn = new FontFamilyButton(160);
      
      JPanel p = new JPanel();
      p.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
      p.add(ffBtn);
      
      SpringLayout layout = new SpringLayout();
      p.setLayout(layout);
      layout.putConstraint(SpringLayout.WEST, ffBtn, 10, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.EAST, p, 10, SpringLayout.EAST, ffBtn);
      layout.putConstraint(SpringLayout.NORTH, ffBtn, 10, SpringLayout.NORTH, p);
      layout.putConstraint(SpringLayout.SOUTH, p, 10, SpringLayout.SOUTH, ffBtn);

      appFrame.add(p);
      

      Runnable runner = new MainFrameShower( appFrame );
      SwingUtilities.invokeLater( runner );
   }

   /** Construct the font family selector button with a fixed width of {@link #MIN_FIXEDWIDTH} pixels. */
   public FontFamilyButton() { this(MIN_FIXEDWIDTH); }
   
   /**
    * Construct the font family selector button with the fixed width specified. 
    * @param w The desired fixed with for the button, in pixels. Range-restricted to {@link #MIN_FIXEDWIDTH} .. {@link 
    * #MAX_FIXEDWIDTH}.
    */
   public FontFamilyButton(int w)
   {
      super();
      setOpaque(false);
      setContentAreaFilled(false);
      setBorderPainted(false);
      setFocusPainted(false);
      setRolloverEnabled(true);
      setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
      
      addActionListener(this);
      
      // populate the font family list IAW fonts installed on system. Then initialize this button to display "Arial" or
      // "Times New Roman" if either is installed. If not, use the first font in the list.
      FontFamilyButton.updateFontList(null);
      if(fontFamilies.isEmpty()) family = "No fonts found";
      else if(fontFamilies.contains("Arial")) family = "Arial";
      else if(fontFamilies.contains("Times New Roman")) family = "Times New Roman";
      else family = fontFamilies.get(0);
      updateButtonTextAndFont();
      
      // set the fixed dimensions of the button: the width is specified by argument (and range-restricted), while the
      // height is based on the current preferred height of the button given the font just installed. This may result in
      // clipping the text height-wise for some unusual fonts, but we do not want button to change height as the font 
      // family changes (the font size is kept the same).
      fixedDim = new Dimension(Utilities.rangeRestrict(MIN_FIXEDWIDTH, MAX_FIXEDWIDTH, w), 
            super.getPreferredSize().height + 4);
   }
   
   /**
    * Registered property change listeners are notified of changes in this property whenever the current font family 
    * attached to a {@link FontFamilyButton} instance is modified via the pop-up chooser panel.
    */
   public final static String FONTFAMILY_PROPERTY = "fontFamily";

   /** 
    * Get the name of the font family currently assigned to this font family selector widget.
    * @return The font family name. The specified font family may not be installed on the host machine.
    */
   public String getFontFamily() { return(family); }
   
   /**
    * Set the name of the font family assigned to this font family selector widget. The text and font of the button
    * widget are updated accordingly.
    * @param s Name of font family. Leading and trailing whitespace is ignored. If null or empty string, the font 
    * family name is set to "Unspecified". No action is taken if this matches the current assigned font family.
    * @param notify If a change is made and this flag is set, any property change listeners registered on the {@link 
    * #FONTFAMILY_PROPERTY} property will be notified. Otherwise, no notification is sent.
    */
   public void setFontFamily(String s, boolean notify)
   {
      s = (s==null) ? "" : s.trim();
      String oldFamily = family;
      family = s.isEmpty() ? "Unspecified" : s;
      if(oldFamily.equals(family)) return;
      
      updateFontList(family);
      updateButtonTextAndFont();
      if(notify) firePropertyChange(FONTFAMILY_PROPERTY, oldFamily, family);
   }
   
   /**
    * Update both the text and font of the font family button to reflect the currently selected font family. The font
    * style is set to bold, and the font size is that of the font installed prior to the change. If the selected font
    * family is not installed on the host, a default font is used, and the button text includes "(?)" to indicate that
    * the font does not exist on the system. <i>Call this method whenever the selected font family changes.</i>
    */
   private void updateButtonTextAndFont()
   {
      setText(LocalFontEnvironment.isFontInstalled(family) ? family : ("(?) " + family));
      Font resolvedFont = LocalFontEnvironment.resolveFont(family, GenericFont.SERIF, FontStyle.BOLD);
      resolvedFont = resolvedFont.deriveFont(getFont().getSize2D());
      setFont(resolvedFont);
   }
   
   /** The font family currently selected. */
   private String family;

   /** Minimum fixed width of the button in pixels. */
   private static final int MIN_FIXEDWIDTH = 100;
   /** Maximum fixed width of the button in pixels. */
   private static final int MAX_FIXEDWIDTH = 500;
   /** The button's fixed size in pixels -- set a construction time. */
   private final Dimension fixedDim;
   
   /** Overridden to enforce a fixed size for the button. */
   @Override public Dimension getPreferredSize() { return(fixedDim); }
   /** Overridden to enforce a fixed width for the button. */
   @Override public Dimension getMaximumSize() { return(fixedDim); }
   /** Overridden to enforce a fixed width for the button. */
   @Override public Dimension getMinimumSize() { return(fixedDim); }

   /** The button click raises a pop-up panel by which user can select a different font family. */
   @Override public void actionPerformed(ActionEvent e)
   {
      if(e.getSource() == this) 
      {
         requestFocusInWindow();
         FFPopup.popup(this);
      }
   }
   
   /**
    * Is the font family associated with this button currently being edited in a pop-up "chooser" panel?
    * @return True if editing in progress.
    */
   public boolean isEditing() { return(FFPopup.isRaisedBy(this)); }
   
   /**
    * Cancel and extinguish the pop-up chooser panel used to select the font family associated with this font family
    * selector button. The method has no effect if the singleton shared pop-up panel is already hidden, or if it is 
    * currently visible <b>but was not invoked by this button instance</b>.
    */
   public void cancelPopup() { FFPopup.cancel(this); }
   

   /** The down-arrow decoration that appears to the right of the icon on the face of the button widget. */
   private static final GeneralPath downArrow = new GeneralPath();
   static
   {
      downArrow.moveTo(0, 0);
      downArrow.lineTo(3, -6);
      downArrow.lineTo(-3, -6);
      downArrow.closePath();
   }
   
   private final static Color transparentBlack = new Color(0,0,0,0);
   private final static Color focusBkgC = new Color(70, 130, 180, 128);
   private final static Color rolloverBkgC = new Color(248, 248, 255);
   private final static Color disabledDecorC = Color.LIGHT_GRAY;
   private final static Color normalDecorC = Color.GRAY;
   private final static Color focusDecorC = new Color(248, 248, 255);
   private final static Color rolloverDecorC = new Color(70, 130, 180);
   // private final static Color selectedBkgC = new Color(100, 149, 237);

   @Override protected void paintComponent(Graphics g)
   {
      Graphics2D g2d = (Graphics2D) g.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int h = getHeight();
      int w = getWidth();

      g2d.setColor(transparentBlack);
      g2d.fillRect(0, 0, w, h);
      
      ButtonModel model = getModel();
      
      Color bkgC = null;
      Color decorC = normalDecorC;
      if(model.isEnabled())
      {
         if(model.isRollover()) 
         {
            bkgC = rolloverBkgC;
            decorC = rolloverDecorC;
         }
         else if(isFocusOwner())
         {
            bkgC = focusBkgC;
            decorC = focusDecorC;
         }
      }
      else
         decorC = disabledDecorC;
      
      if(bkgC != null)
      {
         g2d.setColor(bkgC);
         g2d.fillRoundRect(0, 0, w-1, h-1, 5, 5);
      }

      g2d.setColor(decorC);
      g2d.drawRoundRect(0, 0, w-1, h-1, 5, 5);    
      
      Icon icon = getIcon();
      if(icon != null) icon.paintIcon(this, g2d, 3, 3);
      
      g2d.translate(w-6, h/2 + 1);
      g2d.fill(downArrow);
      g2d.translate(6-w, -1 - h/2);
      
      // draw text vertically centered in available height
      AttributedString attrS = new AttributedString(getText());
      attrS.addAttribute(TextAttribute.FONT, getFont());
      AttributedCharacterIterator aci = attrS.getIterator();
      TextLayout layout = new TextLayout(aci, g2d.getFontRenderContext());
      Rectangle2D textBounds = layout.getBounds();
      if(textBounds.getWidth() < layout.getAdvance()) 
         textBounds.setFrame(textBounds.getX(), textBounds.getY(), layout.getAdvance(), textBounds.getHeight());
      double hAdj = 5-textBounds.getX(); 
      double vAdj = ((double) h) / 2 - (textBounds.getY() + textBounds.getHeight()/2.0);
      
      // set the tool tip text to reflect complete font family name whenever it gets clipped
      setToolTipText((textBounds.getWidth() > w-16) ? getText() : null);
      
      // set clip so that text does not invade the arrow region
      g2d.clipRect(2, 2, w-14, h-4);
      g2d.setColor(Color.BLACK);
      g2d.drawString(aci, (float) hAdj, (float) vAdj);
   }
   
   /** List of available font families. See {@link #updateFontList(String)}. */
   private static final List<String> fontFamilies = new ArrayList<>();

   /**
    * Update the font family list. 
    * 
    * <p>The font family list, created the first time this method is invoked, is presented in the pop-up panel raised
    * when the user clicks on an instance of <b>FontFamilyButton</b>. It includes an alphabetical list of all installed
    * font families that support the upper-case and lower-case ASCII letters and the ten ASCII digits. Whenever the user
    * manually enters the name of a font family, call this method to add that family name to the list (maintaining
    * alphabetical order) -- so that the user does not have to manually enter it each time (this will be a rare usage 
    * case anyway).</p>
    * 
    * @param entry A string presumably representing a font family name that was just entered into an instance of 
    * <b>FontFamilyButton</b>. A null or empty string is ignored.
    */
   private static void updateFontList(String entry)
   {
      // lazily create the (static) list of available fonts. They will be in alphabetical order
      if(fontFamilies.isEmpty())
      {
         String[] installedFonts = LocalFontEnvironment.getInstalledFontFamilies(
               new UnicodeSubset("Latin Basic", "\u0030\u0039\u0041\u005A\u0061\u007A"), true);
         fontFamilies.addAll(Arrays.asList(installedFonts));
      }

      if(entry != null)
      {
         entry = entry.trim();
         if(!entry.isEmpty() && !fontFamilies.contains(entry))
         {
            fontFamilies.add(entry);
            Collections.sort(fontFamilies);
         }
      }
   }
   
   
   //
   // Pop-up font family chooser panel -- a static resource shared by all instances of FontFamilyButton
   //
   
   /**
    * The custom singleton panel that encapsulates and manages the contents of a "font family chooser" pop-up window. 
    * When the panel is raised, it is updated to reflect the current font family associated with the invoking {@link 
    * FontFamilyButton}, and then it is installed in a mode-less dialog. When that dialog is extinguished, the panel is 
    * removed from the dialog before the dialog is disposed. The invoking font family selector button is updated IAW 
    * whatever font the user selected in the pop-up panel. Obviously, only one pop-up dialog may exist at a time, and 
    * the pop-up panel is static resource shared by all instances of {@link FontFamilyButton} in the application.
    * 
    * <p>The panel includes an alphabetic listing of all installed fonts that satisfy certain basic requirements, as
    * well as any additional font names the user has entered manually; a text field by which the user can enter a font
    * name not in the list; and a sample text label that is rendered in the currently selected font. Any font in the
    * list widget that is not installed on the system will have "(?)" appearing before the font family name.</p>
    * 
    * <p>To choose a font, the user simply double-clicks on the font name in the list widget, or enters the font name
    * in the text field. To avoid having to use the mouse to select a different font, the focus is put on the list when
    * the pop-up is raised; you can then use the arrow keys to select the desired font, and Shift+Enter to confirm the
    * selection. In any of these scenarios, the pop-up panel is hidden and the invoking font family selector button is
    * updated accordingly. Clicking anywhere outside the pop-up panel or hitting the ESCape key will extinguish the 
    * pop-up without making a change.</p>
    * 
    * @author sruffner
    */
   private static class FFPopup extends JPanel 
         implements WindowFocusListener, ListSelectionListener, MouseListener, ActionListener
   {
      /** 
       * Raise the font family selection panel in a mode-less dialog to let user choose a font family for the invoking 
       * font family selector widget. If the singleton pop-up panel is currently in use, the method has no effect.
       * @param requestor The particular font family selector widget that has requested the pop-up.
       */
      static void popup(FontFamilyButton requestor)
      {
         // popup's content panel is lazily created
         if(popupSingleton == null) popupSingleton = new FFPopup();
         
         // if the widget requesting the pop-up is unspecified or if pop-up panel is currently in use, ignore
         if(requestor == null || popupSingleton.getParent() != null)
            return;
         
         invoker = requestor;
         popupSingleton.setSelection(invoker.getFontFamily());
         
         // create the modeless dialog container and insert pop-up panel into it
         JDialog dlg;
         Container owner = invoker.getTopLevelAncestor();
         if(owner instanceof Window) dlg = new JDialog((Window) owner, "", Dialog.ModalityType.MODELESS);
         else
         {
            invoker = null;
            return;
         }
         dlg.setUndecorated(true);
         dlg.setAlwaysOnTop(true);
         dlg.addWindowFocusListener(popupSingleton);
         dlg.add(popupSingleton);
         dlg.pack();
         
         // determine where dialog should appear, then show it
         Point pUL = invoker.getLocationOnScreen();
         pUL.x += 4;
         pUL.y += invoker.getHeight();
         GUIUtilities.adjustULToFitScreen(pUL, dlg.getPreferredSize());
         dlg.setLocation(pUL);
         dlg.setVisible(true);
      }
      
      /**
       * Is the singleton pop-up panel currently raised to choose a font family, and if so, was it invoked by the font
       * family selector button specified?
       * @param requestor The font family selector button.
       * @return True if pop-up is raised AND the specified font family button is the instance that invoked the pop-up.
       */
      static boolean isRaisedBy(FontFamilyButton requestor)
      {
         return(popupSingleton!=null && popupSingleton.getParent()!=null && invoker!=null && requestor==invoker);
      }
      
      /**
       * Extinguish the singleton pop-up panel <i>without</i> updating the font family in the invoking font family
       * selector button.
       * @param requestor The font family selector button requesting cancellation. The request is honored only if it 
       * comes from the button that raised the pop-up in the first place!
       */
      static void cancel(FontFamilyButton requestor)
      {
         if(!isRaisedBy(requestor)) return;
         
         JDialog dlg = (JDialog) popupSingleton.getTopLevelAncestor();
         dlg.removeWindowFocusListener(popupSingleton);
         dlg.setVisible(false);
         dlg.remove(popupSingleton);
         dlg.dispose();
         invoker = null;
      }
      
      /**
       * Extinguish the font family selector pop-up window, remove the singleton pop-up content panel from it, and 
       * finally update the invoking font family selector widget with the font family selected.
       * @param selectedFont Font family name that was selected. Null if no selection -- in which case the operation is
       * effectively cancelled (the font family selection is unchanged in the invoking button).
       */
      static void extinguish(String selectedFont)
      {
         if(popupSingleton.getParent() == null) return;
         
         JDialog dlg = (JDialog) popupSingleton.getTopLevelAncestor();
         dlg.removeWindowFocusListener(popupSingleton);
         dlg.setVisible(false);
         dlg.remove(popupSingleton);
         dlg.dispose();
         
         if(!fontFamilies.contains(selectedFont))
         {
            updateFontList(selectedFont);
            popupSingleton.reloadFontList();
         }
         if(invoker != null)
         {
            if(selectedFont != null) invoker.setFontFamily(selectedFont, true);
            invoker = null;
         }
      }

      /**
       * Set the font family that holds the current selection in the pop-up panel.
       * @param family The font family name to be selected.
       */
      void setSelection(String family)
      {
         if(fontFamilies.contains(family)) fontList.setSelectedValue(family, true);
         else fontList.setSelectedIndex(-1);
         
         setSampleLabelFont(family);
      }
      
      /**
       * Reload the font family names displayed in the list widget in the pop-up panel. This method should be called
       * whenever a new font family name is entered manually by the user, and only when pop-up panel is hidden.
       */
      void reloadFontList()
      {
         fontList.removeListSelectionListener(this);
         fontList.setListData(fontFamilies.toArray(new String[0]));
         fontList.addListSelectionListener(this);
      }
      
      /** 
       * The pop-up is extinguished when its dialog container loses the focus and no change is made to the currently
       * selected font family.
       */
      @Override public void windowLostFocus(WindowEvent e) { extinguish(null); }
      @Override public void windowGainedFocus(WindowEvent e) { fontList.requestFocusInWindow(); }
      
      /**
       * Whenever the selection changes in the font family list, the font of the sample text label is updated to give
       * a preview of text in that font.
       */
      @Override public void valueChanged(ListSelectionEvent e)
      {
         if(!e.getValueIsAdjusting()) setSampleLabelFont(fontList.getSelectedValue());
      }
      
      /** The singleton pop-up font family chooser panel. */
      private static FFPopup popupSingleton = null;
      /** The font family button that raised the pop-up chooser panel. Null when pop-up panel is not in use. */
      private static FontFamilyButton invoker = null;
      
      /** Private constructor for singleton. */
      private FFPopup()
      {
         sampleLabel = new JLabel("1.9 Sample Text \u03B1\u03B2");
         setSampleLabelFont("Arial");
         add(sampleLabel);
         
         enterFontTF = new JTextField();
         enterFontTF.addActionListener(this);
         enterFontTF.setToolTipText("If font family is not in the list, enter the name here");
         add(enterFontTF);
         
         fontList = new JList<>();
         fontList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         fontList.setVisibleRowCount(20);
         fontList.setCellRenderer(new FontListCellRenderer());
         fontList.addMouseListener(this);
         reloadFontList();
         JScrollPane listScroller = new JScrollPane(fontList);
         add(listScroller);
         
         // hitting "Shift+Enter" confirms whatever font family is selected in the font list, and the pop-up is hidden.
         getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift ENTER"), "done");
         getActionMap().put("done", new TerminalAction("done"));
         
         // if the user hits the "Escape" key, the pop-up is extinguished without making a change
         getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
         getActionMap().put("cancel", new TerminalAction("cancel"));
         
         SpringLayout layout = new SpringLayout();
         setLayout(layout);
         layout.putConstraint(SpringLayout.WEST, listScroller, 10, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.EAST, this, 10, SpringLayout.EAST, listScroller);
         layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, sampleLabel, 0, SpringLayout.HORIZONTAL_CENTER, this);
         layout.putConstraint(SpringLayout.WEST, enterFontTF, 0, SpringLayout.WEST, listScroller);
         layout.putConstraint(SpringLayout.EAST, enterFontTF, 0, SpringLayout.EAST, listScroller);
         layout.putConstraint(SpringLayout.NORTH, enterFontTF, 40, SpringLayout.NORTH, this);
         layout.putConstraint(SpringLayout.NORTH, listScroller, 4, SpringLayout.SOUTH, enterFontTF);
         layout.putConstraint(SpringLayout.SOUTH, this, 10, SpringLayout.SOUTH, listScroller);
         layout.putConstraint(SpringLayout.VERTICAL_CENTER, sampleLabel, 20, SpringLayout.NORTH, this);

         setBorder(BorderFactory.createRaisedBevelBorder());
      }
      
      /** Label displays some sample text in the current selected font in 16 pt. */
      private final JLabel sampleLabel;
      /** The user can manually enter a font family name in this field (to enter a font name not in list). */
      private JTextField enterFontTF = null;
      /** List widget displaying the list of font family names. */
      private JList<String> fontList = null;
      
      private void setSampleLabelFont(String family)
      {
         if(family == null) family = "?";
         Font resolvedFont = LocalFontEnvironment.resolveFont(family, GenericFont.SERIF, FontStyle.BOLD);
         resolvedFont = resolvedFont.deriveFont((float)16);
         sampleLabel.setFont(resolvedFont);
      }

      /** 
       * When the use double-clicks on an item in the font family list, extinguish the pop-up and select that font
       * family for the invoking selector button.
       */
      @Override public void mouseClicked(MouseEvent e)
      {
         if(e.getSource() == fontList && e.getClickCount() == 2)
         {
            extinguish(fontList.getSelectedValue());
         }
      }

      @Override public void mouseEntered(MouseEvent e) {}
      @Override public void mouseExited(MouseEvent e) {}
      @Override public void mousePressed(MouseEvent e) {}
      @Override public void mouseReleased(MouseEvent e) {}
      
      /**
       * When the user enters a font family name in the text field, that family name is added to the font family list 
       * (unless it is already there). The pop-up is then extinguished and the specified font family is assigned to the
       * invoking widget. If the entered string is empty or contains only blanks, then no action is taken.
       */
      @Override public void actionPerformed(ActionEvent e)
      {
         if(e.getSource() != enterFontTF) return;
         
         String name = enterFontTF.getText().trim();
         enterFontTF.setText("");
         if(!name.isEmpty()) extinguish(name);
      }
      
   }
   
   /** Helper action handles termination of the pop-up: either canceling or confirming a new selection. */
   static class TerminalAction extends AbstractAction
   {
      TerminalAction(String cmd) { actionCmd = cmd; }
      
      @Override public void actionPerformed(ActionEvent e)
      {
         if("cancel".equals(actionCmd)) FFPopup.extinguish(null);
         else if("done".equals(actionCmd))
         {
            String s = FFPopup.popupSingleton.fontList.getSelectedValue();
            if(s != null) FFPopup.extinguish(s);
         }
      }
      
      private final String actionCmd;
   }
   
   /** 
    * A simple list cell renderer for the pop-up panel's list widget. It merely puts the string "(?) " before any font
    * family that is not installed on the host machine -- mirroring what' done in the font selector button itself.
    * @author sruffner
    */
   static class FontListCellRenderer extends JLabel implements ListCellRenderer<String> 
   {
      @Override public Component getListCellRendererComponent(JList<? extends String> list, String value, int idx, 
            boolean sel, boolean focus) 
      {
         boolean installed = LocalFontEnvironment.isFontInstalled(value);
         setText(installed ? value : "(?) " + value);
         if(sel) 
         {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
         } 
         else 
         {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
         }
         setEnabled(list.isEnabled());
         setFont(list.getFont());
         setOpaque(true);
         return(this);
      }
   }
}
