package com.srscicomp.common.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;


/**
 * <code>TitleBarPanel</code> is a simple panel that provides a title bar-like decoration if properly positioned in its 
 * parent container (eg, using <code>BorderLayout.NORTH</code>). Features:
 * <ul>
 *    <li>Paints a linear L-to-R gradient background: A specified highlight color fades to the panel's bkg color.</li>
 *    <li>Keyboard focus highlight (optional). If enabled, the panel will change in appearance when the permanent 
 *    keyboard focus is on a descendant of the panel's immediate parent. User can specify a background highlight and 
 *    title text color for the "unfocussed" state as well as the "focussed" state.</li>
 * </ul>
 * @author sruffner
 */
public class TitleBarPanel extends JPreferredSizePanel implements PropertyChangeListener
{
   private static final long serialVersionUID = 1L;

   /** Background highlight color when keyboard focus is not in parent, or focus highlighting is disabled. */
   private Color unfocussedBkgHiliteColor = Color.WHITE;

   /** Title text color when keyboard focus is not in parent, or focus highlighting is disabled. */
   private Color unfocussedTextHiliteColor = Color.BLACK;

   /** Font style applied to title text when keyboard focus is not in parent, or focus highlighting is disabled. */
   private int unfocussedTextStyle = Font.PLAIN;
   
   /** Background highlight color when keyboard focus is in parent, IF focus highlighting is enabled. */
   private Color focussedBkgHiliteColor = FFocusPanel.FOCUSCOLOR;

   /** Title text color when keyboard focus is in parent, IF focus highlighting is enabled. */
   private Color focussedTextHiliteColor = Color.WHITE;

   /** Font style applied to title text when keyboard focus is in parent, IF focus highlighting is disabled. */
   private int focussedTextStyle = Font.BOLD;

   /** 
    * If <code>null</code>, the keyboard focus highlight feature is disabled. Otherwise, the focus highlight is applied 
    * whenever the keyboard focus is on a descendant of this container.
    */
   private Container focusContainer = null;

   /** 
    * Construct a <code>TitleBarPanel</code> that does NOT support the keyboard focus highlight feature, that uses 
    * default colors for the background and text, and that renders the title text in a plain font style.
    */
   public TitleBarPanel()
   {
      this(null, null, Font.PLAIN);
   }
 
   /**
    * Construct a <code>TitleBarPanel</code> that does NOT support the keyboard focus highlight feature.
    * 
    * @param bkgHilite The background highlight color. The title bar's gradient fades from this color into the panel's 
    * background color. If <code>null</code>, this defaults to <code>Color.WHITE</code>.
    * @param textHilite The color of the title text. If <code>null</code>, defaults to <code>Color.BLACK</code>.
    * @param textStyle The font style of the title text; some combination of <code>Font.BOLD</code> and
    * <code>Font.Italic</code>.
    */
   public TitleBarPanel(Color bkgHilite, Color textHilite, int textStyle)
   {
      super(false, true);
      if(bkgHilite != null) unfocussedBkgHiliteColor = bkgHilite;
      if(textHilite != null) unfocussedTextHiliteColor = textHilite;
      unfocussedTextStyle = textStyle & (Font.BOLD | Font.ITALIC);
      init();
      
   }

   /**
    * Construct a <code>TitleBarPanel</code> that DOES support the keyboard focus highlight feature, that uses default
    * colors for background and text in the focussed and unfocussed states, and that renders text in a bold font in both
    * states.
    * @param focusContainer The title bar will appear in the "focussed" state whenever the permanent keyboard focus is 
    * on a descendant of this container. If <code>null</code>, focus highlighting is disabled.
    */
   public TitleBarPanel(Container focusContainer)
   {
      this(focusContainer, null, null, Font.BOLD, null, null, Font.BOLD);
   }
   
   /**
    * Construct a <code>TitleBarPanel</code> that DOES support the keyboard focus highlight feature.
    * 
    * @param focusContainer The title bar will appear in the "focussed" state whenever the permanent keyboard focus is 
    * on a descendant of this container. If <code>null</code>, focus highlighting is disabled.
    * @param bkgHilite The background highlight color in the "unfocussed" state. When the keyboard focus is NOT within 
    * the title bar's parent, the title bar's gradient fades from this color into the panel's background color. If 
    * <code>null</code>, defaults to <code>Color.WHITE</code>.
    * @param textHilite The color of the title text in the "unfocussed" state. If <code>null</code>, defaults to
    * <code>Color.BLACK</code>.
    * @param textStyle The font style of the title text in the "unfocussed" state; some combination of 
    * <code>Font.BOLD</code> and <code>Font.Italic</code>.
    * @param focusBkg The background highlight color in the "focussed" state. If <code>null</code>, defaults to 
    * <code>FFocusPanel.FOCUSCOLOR</code>.
    * @param focusText The color of the title text in the "focussed" state. If <code>null</code>, defaults to
    * <code>Color.WHITE</code>.
    * @param focusStyle The font style of the title text in the "focussed" state.
    */
   public TitleBarPanel(Container focusContainer, 
         Color bkgHilite, Color textHilite, int textStyle, Color focusBkg, Color focusText, int focusStyle)
   {
      super(false, true);
      this.focusContainer = focusContainer;
      if(bkgHilite != null) unfocussedBkgHiliteColor = bkgHilite;
      if(textHilite != null) unfocussedTextHiliteColor = textHilite;
      unfocussedTextStyle = textStyle & (Font.BOLD | Font.ITALIC);
      if(focusBkg != null) focussedBkgHiliteColor = focusBkg;
      if(focusText != null) focussedTextHiliteColor = focusText;
      focussedTextStyle = focusStyle & (Font.BOLD | Font.ITALIC);
      
      init();
   }

   private void init()
   {
      if(focusContainer != null)
         KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", this);

      panelTitle = new JLabel(" ");
      panelTitle.setFont(panelTitle.getFont().deriveFont(unfocussedTextStyle));
      panelTitle.setForeground(unfocussedTextHiliteColor);
      setLayout(new BorderLayout());
      setOpaque(true);
      setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
      add(panelTitle, BorderLayout.WEST);
   }

   /** This label holds the title bar's current title. */
   private JLabel panelTitle = null;
   
   /**
    * Set the title for this <code>TitleBarPanel</code>.
    * 
    * @param title The new title. If <code>null</code>, the title will be blank.
    */
   public void setTitle(String title)
   {
      panelTitle.setText(" " + ((title == null) ? "" : title));
   }

   /**
    * Get the icon for this <code>TitleBarPanel</code> (ie, the icon for the embedded title label).
    * @return The title bar's icon. If <code>null</code>, then title bar currently lacks an icon.
    */
   public Icon getIcon() { return(panelTitle.getIcon()); }
   
   /**
    * Set the icon for this <code>TitleBarPanel</code> (ie, the icon for the embedded title label).
    * @param icon The new icon. If <code>null</code>, then title bar will not display an icon.
    */
   public void setIcon(Icon icon) { panelTitle.setIcon(icon); }

   /** If set, then the panel was last painted to reflect the "focussed" state. */
   private boolean usingFocusBkg = false;

   @Override
   protected void paintComponent(Graphics g)
   {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g.create();
      try
      {
         int w = getWidth();
         int h = getHeight();
         GradientPaint p = new GradientPaint(0, h/2f, usingFocusBkg ? focussedBkgHiliteColor : unfocussedBkgHiliteColor,
               w, h/2f, getBackground());
         g2d.setPaint(p);
         g2d.fillRect(0, 0, w, h);
      }
      finally { if(g2d != null) g2d.dispose(); }
   }

   public void propertyChange(PropertyChangeEvent e)
   {
      if("permanentFocusOwner".equalsIgnoreCase(e.getPropertyName()))
      {
         if(focusContainer == null) return;
         boolean hasFocus = focusContainer.isAncestorOf( 
               KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner());
         if(hasFocus != usingFocusBkg)
         {
            usingFocusBkg = hasFocus;
            if(usingFocusBkg)
            {
               panelTitle.setFont(panelTitle.getFont().deriveFont(focussedTextStyle));
               panelTitle.setForeground(focussedTextHiliteColor);
            }
            else 
            {
               panelTitle.setFont(panelTitle.getFont().deriveFont(unfocussedTextStyle));
               panelTitle.setForeground(unfocussedTextHiliteColor);
            }

            repaint();
         }
      }
   }

}
