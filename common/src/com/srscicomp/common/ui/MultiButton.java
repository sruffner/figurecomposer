package com.srscicomp.common.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;

/**
 * <code>MultiButton</code> is a specialization of {@link JButton} that acts something like an icon-only combo box. It
 * is intended to provide a relatively compact way to select among a few different objects of the same type T. The 
 * widget can be populated with up to 10 different choices, each of which is defined by a choice value T, an icon 
 * representing that choice, and an optional string that is employed as a tooltip to describe that choice in the
 * button's associated pop-up menu. The button's current icon reflects the current choice. The user interacts with the
 * button's pop-up menu to change the selection.
 * 
 * <p><i>Usage</i>. Construct the multi-choice button widget and call {@link #addChoice(Object, Icon, String) addChoice}
 * to populate it with up to 10 distinct choices. All choice values must be of the same parameterized type T. All choice
 * icons must have the same dimensions. There are no methods to remove choices, as the widget is intended for selecting 
 * among a fixed set of choice values. Once the choice list is initialized, you can set the current choice at any time 
 * with {@link #setCurrentChoice(Object) setCurrentChoice}. The button's icon will reflect that choice.</p>
 * 
 * <p>In addition to the choice icon, the button is custom-painted to include a downward arrow to the right of the icon
 * -- merely a decoration to suggest that other choices may be accessed by clicking on the widget. A button click raises
 * the widget's pop-up menu just to the right of the widget. The pop-up menu displays all of the choice icons in a 
 * compact vertical list. Click outside the pop-up menu to hide it without making a change, or click on an icon in the
 * menu to make it the current choice. If a new choice is made, the button widget fires an {@link ItemEvent} to notify
 * any interested clients. Thus, be sure to register as an {@link java.awt.event.ItemListener} on the widget, NOT as an
 * {@link ActionListener}. Retrieve the current choice at any time with {@link #getCurrentChoice()}.</p>
 * 
 * @author sruffner
 */
public class MultiButton<T> extends JButton implements ActionListener
{
   /**
    * Construct the multi-choice button widget. It will have no choices initially -- call {@link #addChoice(Object, 
    * Icon, String) addChoice} to populate it with two or more choices.
    */
   public MultiButton()
   {
      super();
      setOpaque(false);
      setContentAreaFilled(false);
      setBorderPainted(false);
      setFocusPainted(false);
      setRolloverEnabled(true);
      setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
      
      choicePopup = new JPopupMenu();
      choicePopup.setFocusable(false);
      
      addActionListener(this);
   }

   /**
    * Add a choice value to this multiple-choice button widget. The widget can handle up to 10 different choices, each 
    * of which must be accompanied by a representative icon. All choice icons must have identical dimensions. <i>It is
    * recommended that the widget's choice list be populated during the UI construction phase, before the UI is made
    * displayable.</i>
    * 
    * @param value The choice value.
    * @param icon A representative icon for that choice.
    * @param tip An optional descriptive string. If non-null, it is installed as a tooltip on the item representing the
    * choice in the widget's pop-up menu.
    * @return True if choice was successfully added; false otherwise.
    */
   public boolean addChoice(T value, Icon icon, String tip)
   {
      if(value == null || icon == null || choiceValues.size() == MAX_CHOICES) return(false);
      if(choiceValues.isEmpty())
      {
         iconWidth = icon.getIconWidth();
         iconHeight = icon.getIconHeight();
      }
      else if(icon.getIconWidth() != iconWidth || icon.getIconHeight() != iconHeight)
         return(false);
      
      choiceValues.add(value);
      iSelected = choiceValues.size()-1;
      setIcon(icon);

      IconicButton btn = new IconicButton(icon, tip, iSelected, this);
      choiceBtns.add(btn);
      choicePopup.add(btn);

      if(choiceValues.size() == 1)
      {
         Dimension d = new Dimension(iconWidth+6+12, iconHeight+6);
         setPreferredSize(d);
         setMinimumSize(d);
         setMaximumSize(d);
      }
      
      return(true);
   }
   
   /**
    * Set the current selection for this multiple-choice button widget without firing an item state change notification.
    * The button widget's icon is updated to reflect the new selection.
    * 
    * @param choice The choice value to be selected.
    * @return True if widget was updated successfully; false if specified choice value is in the widget's choice list.
    */
   public boolean setCurrentChoice(T choice)
   {
      for(int i=0; i<choiceValues.size(); i++) if(choiceValues.get(i).equals(choice))
      {
         if(i != iSelected)
         {
            iSelected = i;
            setIcon(choiceBtns.get(i).getIcon());
         }
         return(true);
      }
      return(false);
   }
   
   /**
    * Get the current selection for this multiple-choice button widget.
    * @return The current choice value. Returns null if the widget's choice list is empty.
    */
   public T getCurrentChoice() 
   { 
      return((iSelected >= 0 && iSelected < choiceValues.size()) ? choiceValues.get(iSelected) : null); 
   }
   
   
   /** 
    * Overridden to return "S: Item", where S is the multi-button's general tool tip text and "Item" is the tool tip
    * text for the currently selected item. If no tip is specified for the selected item, returns "S". But if no tool
    * tip is specified for the multi-button itself, then no tip text is returned.
    */
   @Override public String getToolTipText()
   {
      String tip = super.getToolTipText();
      if(tip != null && !tip.isEmpty())
      {
         if(iSelected >= 0 && iSelected < choiceValues.size())
         {
            String selectedTip = choiceBtns.get(iSelected).getToolTipText();
            if(selectedTip != null)
               tip = String.format("%s: %s", tip, selectedTip);
         }
      }
      return(tip);
   }

   public void actionPerformed(ActionEvent e)
   {
      Object src = e.getSource();
      if(src == this)
         raisePopup();
      else
      {
         choicePopup.setVisible(false);
         
         int idx = -1;
         try { idx = Integer.parseInt(e.getActionCommand()); } catch(NumberFormatException ignored) {}
         
         if(idx > -1 && idx < choiceValues.size() && idx != iSelected)
         {
            iSelected = idx;
            setIcon(choiceBtns.get(iSelected).getIcon());
            fireItemStateChanged(
                  new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, choiceValues.get(iSelected), ItemEvent.SELECTED));
         }
      }
   }
   
   /** The maximum number of value objects that the multi-choice button widget supports. */
   public final static int MAX_CHOICES = 10;
   
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
   }
   
   /** Hide the multi-choice button widget's choice pop-up menu if it is currently visible. */
   public void cancelPopup() { choicePopup.setVisible(false); }
   
   /**
    * Raise the pop-up menu in which the button widget's different choices are displayed. Each choice value is
    * represented by a compact button sporting that choice's representative icon. Clicking a button extinguishes the
    * menu, as does clicking outside the menu.
    */
   private void raisePopup()
   {
      if(choiceValues.size() < 2 || choicePopup.isVisible()) return;
      
      // this ensures only the button reflecting the current choice is "selected" in the pop-up menu
      for(int i=0; i<choiceBtns.size(); i++) choiceBtns.get(i).setSelected(i==iSelected);

      int x = getWidth() + 2;
      int y = getHeight()/2 - choicePopup.getPreferredSize().height/2;
      choicePopup.show(this, x, y);
   }
   
   /** Index of the current choice. Will be -1 if the choice list is empty. */
   private int iSelected = -1;
   /** The width of a choice icon -- every icon must have the same width. */
   private int iconWidth = 0;
   /** The height of the choice icon -- every icon must have the same height. */
   private int iconHeight = 0;
   
   /** The choice list. */
   private final List<T> choiceValues = new ArrayList<>();
   /** The corresponding iconic buttons in the widget's pop-up menu. */
   private final List<IconicButton> choiceBtns = new ArrayList<>();
   
   /** The button widget's pop-up menu. Clicking on the button raises the pop-up. */
   private final JPopupMenu choicePopup;
   
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
   private final static Color selectedBkgC = new Color(100, 149, 237);
   
   /**
    * The icon-only button used to populate the multiple-choice button widget's pop-up menu.
    * @author sruffner
    */
   private static class IconicButton extends JButton
   {
      IconicButton(Icon icon, String tip, int indexPos, ActionListener l)
      {
         super();
         setOpaque(false);
         setContentAreaFilled(false);
         setFocusPainted(false);
         setRolloverEnabled(true);
         
         // border includes a gray line at top to separate each entry from the previous. Don't need this line on the
         // first entry, but we want all buttons to be the same size.
         if(indexPos == 0)
            setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
         else
            setBorder(BorderFactory.createCompoundBorder(
               BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY), BorderFactory.createEmptyBorder(3,8,4,8)));
         
         setIcon(icon);
         setActionCommand(Integer.toString(indexPos));
         addActionListener(l);
         if(tip != null) setToolTipText(tip);
      }
      
      @Override protected void paintComponent(Graphics g)
      {
         Graphics2D g2d = (Graphics2D) g.create();
         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         int h = getHeight();
         int w = getWidth();

         ButtonModel model = getModel();
         g2d.setColor(model.isRollover() ? focusBkgC : (model.isSelected() ? selectedBkgC : transparentBlack));
         g2d.fillRect(0, 0, w, h);
         
         // let base class paint the icon
         super.paintComponent(g2d);
      }
   }
}
