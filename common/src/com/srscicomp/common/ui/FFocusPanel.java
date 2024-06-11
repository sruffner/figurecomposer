package com.srscicomp.common.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 * <code>FFocusPanel</code> is a <code>JPanel</code> that automatically changes its border to indicate that it contains 
 * a component with the permanent keyboard focus. Thus, it provides a consistent visual cue of where the focus is within
 * a Swing application's user interface. Each major section of the GUI could either extend <code>FFocusPanel</code> or 
 * be placed inside one.
 * 
 * <p>For consistency's sake, <code>FFocusPanel</code> manages the panel border -- <code>setBorder()</code> has been 
 * overridden to prevent it being changed. Titled and untitled versions are supported. To use the titled version, or to 
 * dynamically change the title, use <code>setBorderTitle()</code>. The untitled versions have 2 pixels of empty space 
 * outside and 2 or 3 pixels of empty space inside of a simple line border. When the focus is inside the panel, the 
 * line border is 2-pix thick and bluish in color; otherwise, it is 1-pix thick and gray. The titled versions use the 
 * same line borders, but there are no empty spaces since the titled border includes empty space already. The title is 
 * vertically centered and left-justified on the top line; when the focus in inside the panel, the title is in a bold 
 * font style, if the panel's current font supports the bold font style.</p>
 * 
 * <p>Currently, there's no support for customizing the appearance of <code>FFocusPanel</code>.</p>
 * 
 * @author  sruffner
 */
public class FFocusPanel extends JPanel implements PropertyChangeListener
{
   private static final long serialVersionUID = 1L;

   public FFocusPanel(LayoutManager layout, boolean isDoubleBuffered) { super(layout, isDoubleBuffered); init(); }
   public FFocusPanel(LayoutManager layout) { super(layout); init(); }
   public FFocusPanel(boolean isDoubleBuffered) { super(isDoubleBuffered); init(); }
   public FFocusPanel() { super(); init(); }

   private void init()
   {
      super.setBorder(focussedBorder);
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", this);
   }

   /** Border color to indicate keyboard focus component is within this panel. */
   public static final Color FOCUSCOLOR = new Color(70, 130, 180);

   /** Default titleless panel border when it contains the permanent keyboard focus owner. */
   private static final Border focussedBorder_noTitle = BorderFactory.createCompoundBorder(
         BorderFactory.createEmptyBorder(2,2,2,2), BorderFactory.createCompoundBorder(
         BorderFactory.createLineBorder(FOCUSCOLOR, 2), BorderFactory.createEmptyBorder(2, 2, 2, 2)));
 
   /** Default titleless panel border when it does NOT contain the permanent keyboard focus owner. */
   private static final Border unfocussedBorder_noTitle = BorderFactory.createCompoundBorder(
         BorderFactory.createEmptyBorder(2,2,2,2), BorderFactory.createCompoundBorder(
         BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createEmptyBorder(3, 3, 3, 3)));

   /** Current border displayed when panel contains the permanent keyboard focus owner. */
   private Border focussedBorder = focussedBorder_noTitle;

   /** Current border displayed when panel does NOT contain the permanent keyboard focus owner. */
   private Border unfocussedBorder = unfocussedBorder_noTitle;

   /** Overridden to prevent changing the border. The border is strictly controlled by <code>FFocusPanel</code>. */
   @Override public void setBorder(Border b) {}

   /**
    * Set the title for a titled border.
    * 
    * @param title If <code>null</code> or empty string, the default titleless borders are used. Otherwise, the panel 
    * uses two versions of a titled border, one for the focussed state and one for the unfocussed state.
    */
  public void setBorderTitle(String title)
   {
      if((title == null || title.isEmpty()) && (focussedBorder != focussedBorder_noTitle))
      {
         focussedBorder = focussedBorder_noTitle;
         unfocussedBorder = unfocussedBorder_noTitle;
      }
      else
      {
         Border focussedLine = BorderFactory.createLineBorder(FOCUSCOLOR, 2);
         Border unfocussedLine = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), 
               BorderFactory.createEmptyBorder(1,1,1,1));
         Font focussedFont = getFont();
         focussedFont = focussedFont.deriveFont(Font.BOLD, focussedFont.getSize2D());

         unfocussedBorder = BorderFactory.createTitledBorder(unfocussedLine, title);
         focussedBorder = BorderFactory.createTitledBorder(
               focussedLine, title, TitledBorder.LEADING, TitledBorder.TOP, focussedFont);
      }
      Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
      super.setBorder( isAncestorOf(c) ? focussedBorder : unfocussedBorder);
   }

   public void propertyChange(PropertyChangeEvent e)
   {
      if("permanentFocusOwner".equals(e.getPropertyName()))
      {
         Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
         super.setBorder( isAncestorOf(c) ? focussedBorder : unfocussedBorder);
      }
   }
}
