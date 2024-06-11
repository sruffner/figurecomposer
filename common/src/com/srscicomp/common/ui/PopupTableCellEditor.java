package com.srscicomp.common.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.TableCellEditor;

import com.srscicomp.common.util.Utilities;


/**
 * A table cell editor for selecting the value of an object from among a list of fixed choices.
 * 
 * <p>As its name suggests, <b>PopupTableCellEditor</b> uses a {@link JPopupMenu} to select the value for the table
 * cell being edited. It is parameterized by the value class E, and E.toString() is used to display the value of each
 * choice in the popup menu.</p>
 * 
 * <p>To use the editor, simply construct it and install it in a client table. You can reconfigure it with a different
 * choice list via {@link #configure(List, int, int)}, but only when it is not in use.</p>
 * 
 * <p>The "editor component", i.e., the component actually installed in the table cell, is simply a button that reflects
 * the current value E in the cell; the custom-painted button includes a small downward-pointing arrow on the right side
 * as a hint that a drop-down or pop-up menu is associated with the editor. In fact, when the editor is invoked by a
 * mouse press in the cell, the client table will normally repost that event to the button; upon release of the mouse
 * (inside the button), the subsequent action event triggers the popup menu to appear. Once the user either selects a
 * value from the popup menu or cancels out of it, the editing operation ends.</p>
 * 
 * <p>If the editor is invoked in a different manner, or the button fails to receive the mouse press-release-click 
 * sequence because the user quickly moved the mouse outside the cell bounds, the editor button stays in place until the
 * user clicks it to reveal the popup.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
final public class PopupTableCellEditor<E> extends AbstractCellEditor
      implements TableCellEditor, ActionListener, ChangeListener, MouseWheelListener, PopupMenuListener
{
   /**
    * Construct the popup multi-choice table cell editor with an initial choice set, visible item count, and scrolling
    * interval.
    * 
    * @param choices The choice list. <i>Null and repeat entries are ignored.</i>
    * @param nVis The maximum number of choices visible at one time in the editor's popup menu. Scrolling features are
    * enabled in the popup when the number of choices exceeds this value. If the specified value does not lie in the
    * range [{@link #MINVISITEMS} .. {@link #MAXVISITEMS}], a default value is chosen.
    * @param sIntv The scrolling interval assigned to the up/down scroll buttons in the editor's popup menu. These
    * buttons are present at the top/bottom of the menu only when the number of available choices exceeds the visible
    * item count. If the specified value does not lie in the range [{@link #MINSCROLLINTV} .. {@link #MAXSCROLLINTV}],
    * a default value is chosen.
    */
   public PopupTableCellEditor(List<E> choices, int nVis, int sIntv)
   {
      cellBtn = new PTCEButton(false, true);
      cellBtn.setBorder(BorderFactory.createEmptyBorder());
      
      popup = new JPopupMenu();
      popup.addMouseWheelListener(this);
      popup.addPopupMenuListener(this);
      
      choiceList = new ArrayList<E>();
      menuItems = new ArrayList<JCheckBoxMenuItem>();
      configure(choices, nVis, sIntv);
   }

   /**
    * Configure/reconfigure this pop-up multi-choice table cell editor. <i>This method must only be called to set up the
    * cell editor prior to using it.</i>
    * 
    * @param choices The choice list. <i>Null and repeat entries are ignored.</i>
    * @param nVis The maximum number of choices visible at one time in the editor's popup menu. Scrolling features are
    * enabled in the popup when the number of choices exceeds this value. If the specified value does not lie in the
    * range [{@link #MINVISITEMS} .. {@link #MAXVISITEMS}], a default value is chosen.
    * @param sIntv The scrolling interval assigned to the up/down scroll buttons in the editor's popup menu. These
    * buttons are present at the top/bottom of the menu only when the number of available choices exceeds the visible
    * item count. If the specified value does not lie in the range [{@link #MINSCROLLINTV} .. {@link #MAXSCROLLINTV}],
    * a default value is chosen.
    * @return True if successful. Returns false if the editor is in use (pop-up menu raised).
    */
   public boolean configure(List<E> choices, int nVis, int sIntv)
   {
      if(popup.isVisible()) return(false);
      
      visItemCount = (nVis < MINVISITEMS || nVis > MAXVISITEMS) ? DEFVISITEMS : nVis;
      int scrollIntv = (sIntv < MINSCROLLINTV || sIntv > MAXSCROLLINTV) ? DEFSCROLLINTV : sIntv;
      
      popup.removeAll();
      choiceList.clear();
      for(JCheckBoxMenuItem mi : menuItems) mi.removeActionListener(this);
      menuItems.clear();
      
      if(choices != null) 
      {
         for(E choice : choices) if(choice != null && !choiceList.contains(choice))
         {
            JCheckBoxMenuItem mi = new JCheckBoxMenuItem(choice.toString());
            mi.addActionListener(this);
            menuItems.add(mi);
            choiceList.add(choice);
         }
      }
      
      // discard or create scroll buttons and scroll timer as needed. Update timer interval
      if(choiceList.size() > visItemCount && upBtn == null)
      {
         upBtn = new PTCEButton(true, false);
         upBtn.addChangeListener(this);
         downBtn = new PTCEButton(false, false);
         downBtn.addChangeListener(this);
         
         scrollTimer = new Timer(scrollIntv, this);
         scrollTimer.setInitialDelay(30);
      }
      else if(choiceList.size() <= visItemCount && upBtn != null)
      {
         upBtn.removeChangeListener(this);
         upBtn = null;
         downBtn.removeChangeListener(this);
         downBtn = null;
         scrollTimer.stop();
         scrollTimer = null;
      }
      else if(scrollTimer != null) scrollTimer.setDelay(scrollIntv);
      
      return(true);
   }
   /** 
    * The popup menu's visible item count, i.e., the maximum number of items visible at once in the menu. When the 
    * number of available choices exceeds this, then the popup menu will include scroll up/down buttons and also enable
    * scrolling with the mouse wheel.
    */
   private int visItemCount = DEFVISITEMS;

    @Override public Object getCellEditorValue() { return(currValue); }

   @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSel, int r, int c)
   {
      currValue = value;
      
      // prepare the cell-embedded button to fire the action that raises the popup menu
      cellBtn.setText(currValue!=null ? currValue.toString() : "");
      cellBtn.removeActionListener(this); // just in case
      cellBtn.addActionListener(this);
      
      return(cellBtn);
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      Object src = e.getSource();
      if(src == cellBtn)
      {
         // we only want to raise the popup once; when popup is extinguished, the cell edit is terminated
         cellBtn.removeActionListener(this);
         
         // ensure that only the menu item corresponding to the current value is selected.
         int idx = choiceList.indexOf(currValue);
         for(int i=0; i<menuItems.size(); i++) menuItems.get(i).setSelected(i==idx);
         
         // when the number of items exceeds the visible item count, we have to manage the sublist of menu items 
         // installed in the menu, with the up/down scroll buttons appearing at  the top/bottom of the menu. When 
         // raising the popup, we try to set the first visible item index so that the current selection is mid-range.
         if(menuItems.size() > visItemCount)
         {
            popup.removeAll();
            popup.add(upBtn);
            
            firstVisibleIndex = idx - (visItemCount/2);
            if(firstVisibleIndex < 0) 
               firstVisibleIndex = 0;
            else if(firstVisibleIndex + visItemCount > menuItems.size()) 
               firstVisibleIndex = menuItems.size() - visItemCount;
            
            for(int i=firstVisibleIndex; i<firstVisibleIndex+visItemCount; i++)
               popup.add(menuItems.get(i));
            
            popup.add(downBtn);
            
            upBtn.setEnabled(firstVisibleIndex > 0);
            downBtn.setEnabled(firstVisibleIndex < menuItems.size() - visItemCount);
         }
         else
         {
            popup.removeAll();
            for(JCheckBoxMenuItem mi : menuItems) popup.add(mi);
         }

         // try to position popup so that is centered vertically and just right of the button that's installed in the 
         // table cell. If either popup width or height is zero (this always happened the first time it was shown), 
         // calculate manually.
         int w = popup.getWidth();
         int h = popup.getHeight();
         if(w==0 || h==0)
         {
            w = 0; h = 0;
            for(int i=0; i<popup.getComponentCount(); i++)
            {
               Dimension d = popup.getComponent(i).getPreferredSize();
               h += d.height;
               w = Math.max(w, d.width);
            }
         }
         popup.show(cellBtn, cellBtn.getWidth()+1, (cellBtn.getHeight()-h)/2);
      }
      else if(SCROLLUP.equals(e.getActionCommand()) || SCROLLDOWN.equals(e.getActionCommand()))
      {
         // scroll popup menu items up or down -- by removing at one end and inserting at the other... Then update 
         // enable state of up/down buttons and repaint popup menu to reflect the change.
         boolean up = SCROLLUP.equals(e.getActionCommand());
         if(up && firstVisibleIndex==0 || ((!up) && firstVisibleIndex==menuItems.size()-visItemCount))
         {
            scrollTimer.stop();
            return;
         }
         
         scrollMenu(up ? -1 : 1);
      }
      else if(e.getSource() instanceof JCheckBoxMenuItem)
      {
         // an item was selected from the popup menu. Remember the choice selected and stop editing.
         int idx = menuItems.indexOf((JCheckBoxMenuItem) e.getSource());
         currValue = choiceList.get(idx);
         fireEditingStopped();
       }
   }

   @Override public void mouseWheelMoved(MouseWheelEvent e)
   {
      if(menuItems.size() > visItemCount)
      {
         int delta = 0;
         if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
         {
            delta = e.getUnitsToScroll();
            if(Math.abs(delta) > 4) delta = (delta < 0) ? -4 : 4;
         }
         else delta = e.getWheelRotation() < 0 ? -4 : 4;
         scrollMenu(delta);
      }
      e.consume();
   }

   /** Helper method handles scrolling of the items in the popup menu. */
   private void scrollMenu(int delta)
   {
      if((delta < 0 && firstVisibleIndex > 0) || (delta > 0 && firstVisibleIndex < menuItems.size() - visItemCount))
      {
         firstVisibleIndex = Utilities.rangeRestrict(0, menuItems.size()-visItemCount, firstVisibleIndex + delta);
         
         popup.removeAll();
         popup.add(upBtn);
         for(int i=0; i<visItemCount; i++) popup.add(menuItems.get(firstVisibleIndex+i));
         popup.add(downBtn);
         
         upBtn.setEnabled(firstVisibleIndex > 0);
         downBtn.setEnabled(firstVisibleIndex < menuItems.size()-visItemCount);
         popup.revalidate();
         popup.repaint();
      }
   }
   
   @Override public void stateChanged(ChangeEvent e)
   {
      Object src = e.getSource();
      if(src == upBtn || src == downBtn)
      {
         ButtonModel bm = (src==upBtn ? upBtn : downBtn).getModel();
         boolean startScroll = bm.isEnabled() && bm.isPressed();
         
         // if the scroll timer is currently running, stop it if we can no longer scroll in the direction for which
         // it is currently configured
         if(scrollTimer.isRunning())
         {
            if((src==upBtn && scrollTimer.getActionCommand().equals(SCROLLUP)) || 
                  (src==downBtn && scrollTimer.getActionCommand().equals(SCROLLDOWN)))
            {
               if(!startScroll) scrollTimer.stop();
            }
         }
         
         // if the target scroll button is pressed, assign the appropriate action command to the scroll timer and
         // start it.
         if(startScroll)
         {
            scrollTimer.setActionCommand((src==upBtn) ? SCROLLUP : SCROLLDOWN);
            scrollTimer.start();
         }
      }
   }

   @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
   @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { }
   @Override public void popupMenuCanceled(PopupMenuEvent e) { fireEditingCanceled(); }

   /** The current value in the editor. */
   private Object currValue;
   /** The list of available choices installed in the editor. */
   private List<E> choiceList = null;
   
   /** Button installed as the editor component in the client table; it fires the action event that raises the popup. */
   private PTCEButton cellBtn = null;
   /** Scroll-up button installed as the first item in the popup menu, when needed. */
   private PTCEButton upBtn = null;
   /** Scroll-down button installed as the first item in the popup menu, when needed. */
   private PTCEButton downBtn = null;
   /** Timer fires at regular intervals while either scroll button is depressed in the popup menu. */
   private Timer scrollTimer = null;
   /** The popup menu. */
   private JPopupMenu popup = null;
   /** 
    * List of all menu items in the popup, in the same order as the choice list. If the choice list exceeds the
    * editor's visible item count, only a contiguous subset of these items are installed in the popup. 
    */
   private List<JCheckBoxMenuItem> menuItems = null;
   /** 
    * When the number of available choices exceeds the editor's visible item count, this is the index (into the menu 
    * item list) of the first visible menu item. Otherwise, it is not used.
    */
   private int firstVisibleIndex = -1;

   /** Scroll-up action fired by timer at regular intervals when the scroll-up button is depressed. */
   private static final String SCROLLUP = "up";
   /** Scroll-down action fired by timer at regular intervals when the scroll-down button is depressed. */
   private static final String SCROLLDOWN = "down";
   /** Minimum allowed value for the interval between scrolling events (in milliseconds). */
   public static final int MINSCROLLINTV = 30;
   /** Maximum allowed value for the interval between scrolling events (in milliseconds). */
   public static final int MAXSCROLLINTV = 250;
   /** The default scrolling interval in milliseconds. */
   private static final int DEFSCROLLINTV = 120;
   /** Minimum visible item count for the popup menu. */
   public static final int MINVISITEMS = 8;
   /** Maximum visible item count for the popup menu. */
   public static final int MAXVISITEMS = 40;
   /** The default visible item count. */
   private static final int DEFVISITEMS = MINVISITEMS;
   
   /** 
    * Custom-painted button that implements the "editor component" that is embedded in the client table, as well as
    * the scroll up/down buttons that appear at the top and bottom of the popup menu when the number of available
    * choices exceeds the editor's visible item count.
    * 
    * @author sruffner
    */
   private static class PTCEButton extends JButton
   {
      PTCEButton(boolean up, boolean isCellBtn)
      {
         setFocusable(false);
         setRolloverEnabled(true);
         setContentAreaFilled(false);
         setBorderPainted(false);
         if(!isCellBtn) setPreferredSize(new Dimension(20,16));
         else 
         {
            Font f = getFont();
            setFont(f.deriveFont(f.getSize2D()-2.0f));
            setForeground(Color.WHITE);
            setHorizontalAlignment(JButton.LEFT);
         }
         setEnabled(true);

         arrow = new GeneralPath();
         if(up && !isCellBtn) { arrow.moveTo(1,9); arrow.lineTo(5,1); arrow.lineTo(9, 9); arrow.closePath(); }
         else { arrow.moveTo(1,1); arrow.lineTo(5,9); arrow.lineTo(9, 1); arrow.closePath(); }
         
         this.isCellBtn = isCellBtn;
      }
      
      public void paintComponent(Graphics g) 
      {
         Graphics2D g2d = (Graphics2D) g.create();
         try
         {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if(isCellBtn)
            {
               g2d.setColor(azure);
               g2d.fillRect(0,0, getWidth(), getHeight());
               g2d.setColor(Color.WHITE);
               g2d.translate(getWidth()-12, getHeight()/2 - 5);
               g2d.fill(arrow);
               g2d.translate(12-getWidth(), 5 - getHeight()/2);
               g2d.clipRect(0, 0, getWidth()-12, getHeight());
               super.paintComponent(g2d);  // let super class paint the text
               return;
            }
            
            ButtonModel bm = getModel();
            
            g2d.setColor(model.isEnabled() && (bm.isRollover() || bm.isPressed()) ? azure : Color.LIGHT_GRAY);
            if(bm.isEnabled() && bm.isPressed())
               g2d.fillRect(2, 1, getWidth()-4, getHeight()-2);
            else
               g2d.drawRect(2, 1, getWidth()-4, getHeight()-2);
            
            g2d.translate(getWidth()/2 - 5, getHeight()/2 - 5);
            g2d.setColor(model.isEnabled() ? 
                  (model.isPressed() ? Color.WHITE : (model.isRollover() ? azure : Color.BLACK)) : Color.LIGHT_GRAY);
            g2d.fill(arrow);
         }
         finally { if(g2d != null) g2d.dispose(); }
      }
      
      private GeneralPath arrow = null;
      private boolean isCellBtn = false;
   }

   private final static Color azure = new Color(0, 128, 255);
}

