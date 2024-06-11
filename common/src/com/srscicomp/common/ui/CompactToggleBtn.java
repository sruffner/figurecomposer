package com.srscicomp.common.ui;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * <code>CompactToggleBtn</code> is an icon-only, compact toggle button. It revises <code>JToggleButton</code>, 
 * disabling the "content area" provided by the L&F, and providing support for simpler visual effects that indicate 
 * whether or not the button is selected.
 * 
 * @author sruffner
 */
public class CompactToggleBtn extends JToggleButton implements MouseListener, ChangeListener
{
   private static final long serialVersionUID = 1L;

   /** The toggle button's background color when it is selected. */
   private static final Color selectedBkg;

   /** The toggle button's background color when it is not selected. */
   private static final Color unselectedBkg;
   
   /** The toggle button's border when it is selected. */
   private static final Border selectedBorder;

   /** The toggle button's border when it is unselected. */
   private static final Border unselectedBorder;

   /** The toggle button's border when mouse is hovering over it, regardless of state. */
   private static final Border hoverBorder;

   static
   {
      JPanel p = new JPanel();
      unselectedBkg = p.getBackground();
      selectedBkg = unselectedBkg.brighter().brighter().brighter().brighter();
      Color darker = unselectedBkg.darker().darker();
      Color brighter = selectedBkg.brighter().brighter();
      selectedBorder = BorderFactory.createBevelBorder(BevelBorder.LOWERED, brighter, darker);
      unselectedBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
      hoverBorder = BorderFactory.createBevelBorder(BevelBorder.RAISED, brighter, darker);
   }

   public CompactToggleBtn(Icon unselected, Icon selected)
   {
      super(unselected);
      setSelectedIcon(selected);
      setBackground(unselectedBkg);
      setBorder(unselectedBorder);
      setMargin(new Insets(2, 2, 2, 2));
      super.setContentAreaFilled(false);
      addChangeListener(this);
      addMouseListener(this);
   }


   /**
    * Overridden to be a no-op. <code>CompactToggleBtn</code> disables painting of content area permanently.
    * @see javax.swing.AbstractButton#setContentAreaFilled(boolean)
    */
   @Override public void setContentAreaFilled(boolean b) {}
   
   /**
    * Overridden to indicate that <code>CompactToggleBtn</code> is opaque. Since we disable L&F content area painting, 
    * the button UI will report the component as not opaque, which is NOT what we want. 
    */
   @Override public boolean isOpaque() { return(true); }
   
   public void mouseClicked(MouseEvent e) {}
   public void mousePressed(MouseEvent e) {}
   public void mouseReleased(MouseEvent e) {}
   public void mouseEntered(MouseEvent e) {if(e.getSource() == this) setBorder(hoverBorder); }
   public void mouseExited(MouseEvent e) { if(e.getSource() == this) updateBorder(); }
   public void stateChanged(ChangeEvent e)
   {
      if(e.getSource() == this) 
      {
         setBackground(isSelected() ? selectedBkg : unselectedBkg);
         updateBorder();
      }
   }

   private void updateBorder() { setBorder(isSelected() ? selectedBorder : unselectedBorder); }
}
