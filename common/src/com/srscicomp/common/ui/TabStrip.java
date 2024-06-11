package com.srscicomp.common.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;


/**
 * <code>TabStrip</code> is a custom component that manages a narrow strip of horizontal tabs. Unlike Swing's 
 * <code>JTabbedPane</code>, it does not manage the display of individual components associated with each tab. Instead, 
 * it is intended for use in a "multi-document" situation, in which a single GUI component can be reloaded to display
 * different instances of the same class of document (if we used <code>JTabbedPane</code> in this situation, we'd need 
 * to construct another instance of the GUI component for each tab).
 * 
 * <p>The number of tabs in the strip, each tab's appearance, and close/select actions are governed entirely by a 
 * <code>TabStripModel</code> passed to the strip control at construction time (currently no support for swapping in a 
 * different model instance). <code>TabStrip</code> registers itself as a <code>ChangeListener</code> on the supplied 
 * model. The model is responsible for firing a change notification whenever it changes state in any way -- selected 
 * tab changes, a tab is added or removed, tab label or tooltip changes, etc. In response, <code>TabStrip</code> 
 * recomputes the tab layout and repaints itself.</p>
 * 
 * <p><code>TabStrip</code> is intended to appear along the top edge of the GUI component that actually displays the 
 * "document" or other content associated with the currently selected tab. It is NOT highly configurable, as it was 
 * targeted for use in the <em>DataNav</em> application. Tabs are displayed in order from left to right. The border is 
 * a 1-pixel line (darker than the default component background) on the left, right, and top edges, with rounded corners
 * at the top-left and top-right. The bottom-edge border is a 2-pixel line in the current "selection color". The 
 * selected tab is painted with a gradient that tends from the default background color at the top to the "selection 
 * color" at the bottom. The default "selection color" is a steel blue (R=70,G=130,B=180). The strip's design height is 
 * 25 pixels; therefore, the <code>TabStripModel</code> should supply tab icons that are a maximum of 16x16 -- or 
 * expect things to look ugly!</p>
 * 
 * <p>What if the current set of tabs is such that they cannot all be displayed in the width available? In this case,
 * <code>TabStrip</code> will only display a subset of the tabs; the currently selected tab is always part of this 
 * subset. A "Show others" icon (a downward-pointing arrow) appears immediately after the last visible tab. Clicking on 
 * this icon raises a popup menu by which the user can select any one of the currently hidden tabs.</p>
 * 
 * <p><em>Selecting a different tab; closing tabs</em>. To switch tabs, the user simply mouses down on the desired tab 
 * (or chooses a hidden tab from the popup menu described above). <code>TabStrip</code> merely passes the request on to 
 * the underlying model. Thus, the model can choose to honor or veto the request. If it approves the selection, it must 
 * send a state change notification so that the strip control is updated accordingly. To close a tab, the user mouses 
 * down on the "close tab" icon that appears near the right edge of the tab (this icon is visible on the selected tab; 
 * it will appear on an unselected tab if the user rolls the cursor over the tab -- so the user can choose to close an 
 * unselected tab). Again, <code>TabStrip</code> passes the close request to the model, which can choose to accept or
 * reject it. The model determines which tabs are closable, if any, and can supply a custom tooltip for the "close tab"
 * icon of each closable tab. This icon will not appear on tabs that are not closable.</p>
 * 
 * <p><i>Repositioning tabs</i>. If the tab strip model supports tab repositioning ({@link TabStripModel
 * #supportsTabRepositioning()}, the user can mouse-drag a tab to a different position in the strip. During the drag
 * gesture, the strip updates itself to show what it would look like if the dragged tab were dropped at the current
 * mouse location. A "hole" in the strip shows the current drop location, and a translucent version of the dragged tab
 * follows the mouse cursor. Note that, while the user can drag the mouse outside the strip bounds, the translucent
 * dragged tab stays within those bounds. Also note that it is currently NOT possible to reposition the subset of hidden
 * tabs that are in the "Show others" popup menu.</p>
 * 
 * @author sruffner
 */
public class TabStrip extends JComponent implements MouseInputListener, ChangeListener, ActionListener
{
   /** Minimum width in pixels (if the layout manager will respect it!). */
   //private final static int MINWIDTH = 150;
   /** Design height in pixels. */
   private final static int TABHEIGHT = 25;
   /** Size of rounded corner on tabs and in UL and UR corners of strip's border. */
   private final static int ROUNDCORNERSZ = 8;
   /** Horizontal gap between icons and label or tab edge, in pixels.*/
   private final static int ICONGAP_H = 4;
   /** Vertical gap between top of icon and top edge of strip, in pixels. */
   private final static int ICONGAP_V = 4;
   /** The default selection color. */
   private final static Color DEF_SELECTCOLOR = new Color(70, 130, 180);
   /** The "Close" icon. */
   public final static Icon closeIcon = GUIUtilities.createImageIcon(TabStrip.class, 
         "/com/srscicomp/common/resources/close.png", "");
   /** The "Close" icon when mouse is hovering over it. */
   public final static Icon rolloverCloseIcon = GUIUtilities.createImageIcon(TabStrip.class, 
         "/com/srscicomp/common/resources/close_active.png", "");
   /** The "Show others" icon. */
   private final static ArrowIcon moreIcon = new ArrowIcon(Color.BLACK, 8, ArrowIcon.Direction.DOWN);
   
   /** Path rendering the left edge of a tab (vertical line curving to the right at the top). */
   private final static GeneralPath LEFTEDGE;
   /** Path rendering the right edge of a tab (vertical line curving to the left at the top). */
   private final static GeneralPath RIGHTEDGE;
   static
   {
      LEFTEDGE = new GeneralPath();
      LEFTEDGE.moveTo(0, TABHEIGHT-1);
      LEFTEDGE.lineTo(0, ROUNDCORNERSZ);
      LEFTEDGE.quadTo(0, 0, ROUNDCORNERSZ, 0);
      RIGHTEDGE = new GeneralPath();
      RIGHTEDGE.moveTo(-ROUNDCORNERSZ, 0);
      RIGHTEDGE.quadTo(0, 0, 0, ROUNDCORNERSZ);
      RIGHTEDGE.lineTo(0, TABHEIGHT-1);
   }
   
   /**
    * Construct a tab strip control governed by the specified model.
    * @param model The underlying model for this <code>TabStrip</code>. Cannot be <code>null</code>.
    */
   public TabStrip(TabStripModel model)
   {
      assert(model != null);
      this.model = model;
      this.model.addChangeListener(this);
      
      layoutTabs();
      
      addMouseMotionListener(this);
      addMouseListener(this);
      setToolTipText(" ");  // enable tooltips
   }
   
   /**
    * Get the current index of the first visible tab in the tab strip. 
    * @return Index of first visible tab. Will be 0 if all tabs are currently visible, -1 if there are no tabs.
    */
   public int getIndexOfFirstVisibleTab() { return(iFirstVisTab); }
   /**
    * Get the number of visible tabs in the tab strip.
    * @return Number of visible tabs.
    */
   public int getNumVisibleTabs() { return(nVisTabs); }
   
   /**
    * Get the fixed height of the tab strip control.
    * @return The strip's design height, in pixels.
    */
   public static int getDesignHeight() { return(TABHEIGHT + 1); }
   
   /** The underlying "data model" for the tab strip: #tabs, selected tab, tab labels, etc. */
   private TabStripModel model = null;
   
   /** The current selection color. */
   private Color selectColor = DEF_SELECTCOLOR;
   
   /**
    * Get the current tab selection color. The selected tab is painted with a gradient starting with this color at the
    * bottom of the tab to the default component background color at the top. In addition, a 2-pixel line in this color
    * rests along the bottom edge of the strip.
    * @return The current tab selection color.
    */
   public Color getSelectionColor() { return(selectColor); }
   
   /**
    * Set the current tab selection color and repaint the strip control.
    * @param c The desired selection color. If <code>null</code>, the default selection color (a steel blue) is used.
    */
   public void setSelectionColor(Color c) { selectColor = (c != null)  ? c : DEF_SELECTCOLOR; repaint(); }
   
   /**
    * Overridden to restrict the height of the tab strip to its design height.
    * @see JComponent#getMaximumSize()
    */
   @Override public Dimension getMaximumSize() { return(new Dimension(Integer.MAX_VALUE, TABHEIGHT+1)); }

   /**
    * Overridden to restrict the height of the tab strip to its design height.
    * @see JComponent#getMinimumSize()
    */
   @Override public Dimension getMinimumSize() { return(new Dimension(0, TABHEIGHT+1)); }

   /**
    * Overridden to restrict the height of the tab strip to its design height and its width to "enough" horizontal space
    * to accommodate the tabs that were visible the last time the strip was laid out.
    * @see JComponent#getPreferredSize()
    */
   @Override
   public Dimension getPreferredSize()
   {
      Dimension d = new Dimension();
      for(int i=0; i<nVisTabs; i++) d.width += tabWidths.get(iFirstVisTab+i);
      if(nVisTabs < tabWidths.size()) d.width += ICONGAP_H*3 + moreIcon.getIconWidth();
      d.height = TABHEIGHT+1;
      return(d);
   }

   /** Current widths of all tabs -- recomputed whenever a resize occurs or the tab strip model changes. */
   private final List<Integer> tabWidths = new ArrayList<Integer>();
   
   /** Current tab labels -- reconstructed whenever the tab strip model changes. */
   private final List<String> tabLabels = new ArrayList<String>();
   
   /** Y-coordinate of common baseline for all visible tab labels. */
   private int yLabelBaseline = 0;
   
   /** The index of the first visible tab. */
   private int iFirstVisTab = 0;
   
   /** The number of visible tabs. */
   private int nVisTabs = 0;
   
   /** Size of strip the last time tabs were laid out. */
   private final Dimension lastSize = new Dimension(0,0);
   
   /**
    * Helper method recomputes all tab widths based on the current state of the underlying tab model and the current
    * actual size of the component. It also finds the subset of visible tabs -- if the component is not wide enough to 
    * accommodate all of them. No action is taken if the component is zero-width or if it cannot obtain a graphics 
    * context (needed to compute tab label bounds).
    */
   private void layoutTabs()
   {
      int w = getWidth();
      Graphics2D g2d = (Graphics2D) getGraphics();
      if(g2d == null || w <= 0)
      {
         // whenever this is the case, we want to be absolutely sure to force a layout the next time the strip
         // is painted -- so set layout width to something other than current width
         lastSize.width = w + 1;
         return;
      }
      
      lastSize.width = w;
      lastSize.height = getHeight();
      
      int nTabs = model.getNumTabs();
      tabWidths.clear();
      tabLabels.clear();
      yLabelBaseline = 0;

      try
      {
         Font font = g2d.getFont();
         FontRenderContext frc = g2d.getFontRenderContext();
         for(int i=0; i<nTabs; i++)
         {
            Icon icon = model.getTabIcon(i);
            int tabW = ICONGAP_H*3 + closeIcon.getIconWidth();
            if(icon != null) tabW += ICONGAP_H + icon.getIconWidth();
            String label = model.getTabLabel(i);
            if(label == null) label = "Tab " + i;
            TextLayout layout = new TextLayout(label, font, frc);
            Rectangle2D bounds = layout.getBounds();
            tabW += (int) bounds.getWidth();
            tabWidths.add(tabW);
            tabLabels.add(label);
            int labelHeight = (int) bounds.getHeight();
            yLabelBaseline = Math.max(yLabelBaseline, (TABHEIGHT+labelHeight)/2 + 1);
         }
         if(yLabelBaseline <= 0 || yLabelBaseline >= TABHEIGHT) yLabelBaseline = TABHEIGHT-1;
      }
      finally { if(g2d != null) g2d.dispose(); }
      
      // if there are no tabs, there's nothing further to do!
      if(nTabs == 0)
      {
         iFirstVisTab = -1;
         nVisTabs = 0;
         return;
      }
      
      // calculate the range of visible tabs, starting at the first visible tab in the previous layout cycle. Then 
      // adjust the index of the first visible tab and the number of visible tabs if the selected tab is not in the
      // new range.
      int iSelectedTab = model.getSelectedTab();

      int availW = w - (ICONGAP_H*3 + moreIcon.getIconWidth());
      if(iFirstVisTab > iSelectedTab) iFirstVisTab = iSelectedTab;
      else if(iFirstVisTab < 0) iFirstVisTab = 0;
      nVisTabs = 0;
      int widthUsed = 0;
      while(iFirstVisTab + nVisTabs < nTabs)
      {
         int tabW = tabWidths.get(iFirstVisTab+nVisTabs);
         if(widthUsed + tabW > availW) break;
         widthUsed += tabW;
         ++nVisTabs;
      }
      
      if(nVisTabs == 0) return;
      
      // if the last tab is showing, decrement index of first visible tab if it is possible to fit in more tabs
      if(iFirstVisTab > 0 && iFirstVisTab+nVisTabs == nTabs)
      {
         while(iFirstVisTab > 0 && (widthUsed + tabWidths.get(iFirstVisTab-1)) <= availW)
         {
            --iFirstVisTab;
            ++nVisTabs;
            widthUsed += tabWidths.get(iFirstVisTab);
         }
      }
      
      if(iSelectedTab >= iFirstVisTab + nVisTabs)
      {
         iFirstVisTab = iSelectedTab - nVisTabs + 1;
         if(iFirstVisTab < 0) iFirstVisTab = 0;
         widthUsed = getTabRangeWidth(iFirstVisTab, iSelectedTab);
         while(widthUsed > availW)
         {
            ++iFirstVisTab;
            if(iFirstVisTab == iSelectedTab) break;
            widthUsed = getTabRangeWidth(iFirstVisTab, iSelectedTab);
         }
         nVisTabs = iSelectedTab - iFirstVisTab + 1;
      }
   }
   
   /**
    * Helper method for {@link #layoutTabs()} computes width occupied by a contiguous range of tabs in the strip.
    * @param start Index of first tab in range.
    * @param end Index of last tab in range.
    * @return Width of specified tab range, in pixels.
    */
   private int getTabRangeWidth(int start, int end)
   {
      int w = 0;
      for(int i=start; i<=end; i++) w += tabWidths.get(i);
      return(w);
   }
   
   @Override
   protected void paintComponent(Graphics g)
   {
      int w = getWidth();
      int h = getHeight();
      if(w <= 0 || h <= 0) return;
      
      if(lastSize.width != w || lastSize.height != h)
         layoutTabs();
            
      Graphics2D g2d = (Graphics2D) g.create();
      Color bkg = getBackground();
      Color fg = getForeground();
      Color strokeC = bkg.darker().darker();

      try
      {
         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2d.setColor(isOpaque() ? bkg : TRANSPARENT_BLACK);
         g2d.fillRect(0, 0, w, h);
         
         // outline entire strip
         GeneralPath p = new GeneralPath();
         p.moveTo(0, TABHEIGHT-1);
         p.lineTo(0, ROUNDCORNERSZ);
         p.quadTo(0, 0, ROUNDCORNERSZ, 0);
         p.lineTo(w-ROUNDCORNERSZ-1, 0);
         p.quadTo(w-1, 0, w-1, ROUNDCORNERSZ);
         p.lineTo(w-1, TABHEIGHT-1);
         p.closePath();
         g2d.setColor(strokeC);
         g2d.draw(p);
         
         // if there are no tabs, then we're done
         if(nVisTabs == 0) return;
         
         int iSelectedTab = model.getSelectedTab();
         
         // prepare the list of visible tabs. During an ongoing tab-drag gesture, the list of tabs are reordered to
         // reflect how the tabs would look if the dragged tab were dropped at the current drop location. To animate the
         // tab-drag gesture, a "hole" appears at the current drop location (including the tab's original location),
         // and a translucent version of the dragged tab follows the mouse cursor (but stays in the strip bounds).
         List<Integer> visTabs = new ArrayList<Integer>();
         for(int i=0; i<nVisTabs; i++) visTabs.add(iFirstVisTab + i);
         
         int iCurrDropLoc = isDraggingTab ? getVisibleTabUnder(dragCurrPt, true) : -1;
         if(iCurrDropLoc != -1 && iCurrDropLoc != pressTab)
         {
            visTabs.remove(Integer.valueOf(pressTab));
            int insIdx = visTabs.indexOf(iCurrDropLoc);
            if(pressTab < iCurrDropLoc) visTabs.add(insIdx+1, pressTab);
            else visTabs.add(insIdx, pressTab);
         }
         
         // draw all visible tabs except the selected tab. We have to be careful with tab indices here in case they've
         // been reordered during a tab-drag gesture!
         int xTranslation = 0;
         for(int i=0; i<visTabs.size(); i++)
         {
            int tabIdx = visTabs.get(i).intValue();
            int tabW = tabWidths.get(tabIdx);
            
            // skip over the selected tab for now. However, if it is the tab being dragged, we want to draw its left
            // edge to help mark the bounds of the "hole" in which it is to be dropped.
            if(tabIdx == iSelectedTab && !(isDraggingTab && pressTab==iSelectedTab))
            {
               xTranslation += tabW;
               g2d.translate(tabW, 0);
               continue;
            }
            
            // draw left edge of tab, except first tab drawn
            if(i>0)
            {
               g2d.setColor(strokeC);
               g2d.draw(LEFTEDGE);
            }
            
            // during a drag gesture, we skip over the current location of the tab being dragged, except to draw it's
            // left edge. We leave a "hole" in this current drop location during the gesture.
            if(isDraggingTab && tabIdx == pressTab)
            {
               xTranslation += tabW;
               g2d.translate(tabW, 0);
               continue;
            }
            
            // draw tab icon, if it has one
            Icon icon = model.getTabIcon(tabIdx);
            int xLabel = ICONGAP_H;
            if(icon != null)
            {
               icon.paintIcon(this, g2d, ICONGAP_H, ICONGAP_V);
               xLabel += ICONGAP_H + icon.getIconWidth();
            }
            
            // draw the tab label
            String label = tabLabels.get(tabIdx);
            g2d.setColor(fg);
            g2d.drawString(label, xLabel, yLabelBaseline);

            // translate to right edge of tab. Draw that edge only for the last tab drawn
            xTranslation += tabW;
            g2d.translate(tabW, 0);
            if(i == nVisTabs-1)
            {
               g2d.setColor(strokeC);
               g2d.draw(RIGHTEDGE);
            } 
         }
         
         // draw "more" icon if some tabs are hidden. But hide it during a tab-drag gesture.
         if(nVisTabs < tabWidths.size() && !isDraggingTab)
            moreIcon.paintIcon(this, g2d, ICONGAP_H*2, (TABHEIGHT-moreIcon.getIconHeight())/2);
         
         // draw the selected tab with flared-out right edge and gradient fill. But don't draw it if it is the tab
         // being dragged during a drag gesture.
         if((!isDraggingTab) || iSelectedTab != pressTab)
         {
            g2d.translate(-xTranslation, 0);
            xTranslation = 0;
            for(int i=0; i<visTabs.size(); i++)
            {
               int tabIdx = visTabs.get(i).intValue();
               if(tabIdx == iSelectedTab) break;
               xTranslation += tabWidths.get(tabIdx);
            }
            g2d.translate(xTranslation, 0);
            
            int lblW = tabWidths.get(iSelectedTab);
            p.reset();
            p.moveTo(0, TABHEIGHT-1);
            p.lineTo(0, ROUNDCORNERSZ);
            p.quadTo(0, 0, ROUNDCORNERSZ, 0);
            p.lineTo(lblW-ROUNDCORNERSZ, 0);
            p.curveTo(lblW, 0, lblW, TABHEIGHT-1, lblW+ROUNDCORNERSZ, TABHEIGHT-1);
            p.closePath();
            
            g2d.setPaint(new GradientPaint(0, 0, bkg, 0, TABHEIGHT, selectColor));
            g2d.fill(p);
            g2d.setColor(strokeC);
            g2d.draw(p);
            
            Icon icon = model.getTabIcon(iSelectedTab);
            int xLabel = ICONGAP_H;
            if(icon != null)
            {
               icon.paintIcon(this, g2d, ICONGAP_H, ICONGAP_V);
               xLabel += ICONGAP_H + icon.getIconWidth();
            }
            
            g2d.setColor(fg);
            g2d.drawString(tabLabels.get(iSelectedTab), xLabel, yLabelBaseline);
   
            if(model.isTabClosable(iSelectedTab))
               closeIcon.paintIcon(this, g2d, lblW-ICONGAP_H-closeIcon.getIconWidth(), ICONGAP_V);
         }
         
         // fill 2-pix-high rect at bottom edge of tab strip with the color matching bottom of gradient fill
         g2d.translate(-xTranslation, 0);
         g2d.setColor(selectColor);
         g2d.fillRect(0, TABHEIGHT-1, w, 2);
         
         // if user is currently dragging a tab with the mouse, draw a translucent representation of that tab at the
         // current drag location
         if(isDraggingTab)
         {
            int tabW = tabWidths.get(pressTab);
            int xOfs = dragCurrPt.x - dragOffsetX;
            
            // cannot drag the tab past the left edge of the strip, nor past the right edge of the last visible tab
            if(xOfs < 0) xOfs = 0;
            int totalTabW = 0;
            for(int i=iFirstVisTab; i<iFirstVisTab+nVisTabs; i++) totalTabW += tabWidths.get(i);
            if(xOfs + tabW > totalTabW) xOfs = totalTabW - tabW;
            
            g2d.translate(xOfs, 0);
            
            // draw dragged tab as a rounded rectangle of the correct width, filled with a translucent background
            Color transFill = (pressTab == iSelectedTab) ?
                  new Color(selectColor.getRed(), selectColor.getGreen(), selectColor.getBlue(), 192) :
                  new Color(bkg.getRed(), bkg.getGreen(), bkg.getBlue(), 192);
                  
            tabRect.setFrame(0, 1, tabW, TABHEIGHT-3);
            g2d.setColor(strokeC);
            g2d.draw(tabRect);
            g2d.setColor(transFill);
            g2d.fill(tabRect);
            
            // draw tab icon, if it has one
            Icon icon = model.getTabIcon(pressTab);
            int xLabel = ICONGAP_H;
            if(icon != null)
            {
               icon.paintIcon(this, g2d, ICONGAP_H, ICONGAP_V);
               xLabel += ICONGAP_H + icon.getIconWidth();
            }
            
            // draw the tab label
            String label = tabLabels.get(pressTab);
            g2d.setColor(fg);
            g2d.drawString(label, xLabel, yLabelBaseline);
         }
      }
      finally { if(g2d != null) g2d.dispose(); }
   }

   /** Used to draw the dragged tab and highlight the drop location during a tab drag gesture. */
   private final RoundRectangle2D tabRect = new RoundRectangle2D.Double(0, 0, 10, 10, ROUNDCORNERSZ, ROUNDCORNERSZ);
   
   
   //
   // Mouse handler methods that trigger rollover effects and implement select, close, drag gestures
   //

   /** Whenever the cursor moves over the tab strip, update the current "rollover" effect (if any). */
   public void mouseMoved(MouseEvent e) { paintRolloverEffect(e.getPoint()); }

   /** Whenever cursor leaves the tab strip bounds, erase any existing "rollover" effect. */
   public void mouseExited(MouseEvent e) { paintRolloverEffect(null); }
   public void mouseEntered(MouseEvent e) {}

   public void mousePressed(MouseEvent e) 
   {
      pressTab = getVisibleTabUnder(e.getPoint(), false);
   }
   
   public void mouseDragged(MouseEvent e) 
   {
      if(!isDraggingTab)
      {
         isDraggingTab = (model != null) && model.supportsTabRepositioning() &&
               nVisTabs > 1 && pressTab >= iFirstVisTab && pressTab < iFirstVisTab + nVisTabs;
         if(isDraggingTab)
         {
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            dragCurrPt = e.getPoint();
            
            dragOffsetX = 0;
            for(int i=iFirstVisTab; i<pressTab; i++) dragOffsetX += tabWidths.get(i);
            dragOffsetX = dragCurrPt.x - dragOffsetX;
            
            // this timer is active only when mouse gets dragged outside the horizontal bounds of the tab strip, so 
            // that we can scroll hidden tabs into view during the drag gesture
            if(dragOutsideTimer == null) dragOutsideTimer = new Timer(300, this);
            
            adjustVisibleTabRangeDuringDrag(0);
            repaint();
         }
      }
      else if(Math.abs(dragCurrPt.x - e.getPoint().x) > 5)
      {
         dragCurrPt = e.getPoint();
         
         // periodic timer is active only when drag cursor is outside horizontal bounds of strip AND it is possible to
         // scroll another tab into view at the front or end of list.
         boolean enaTimer = (dragCurrPt.x < 0 && iFirstVisTab > 0) ||
               (dragCurrPt.x > getWidth() && iFirstVisTab + nVisTabs < tabWidths.size());
         if(enaTimer != dragOutsideTimer.isRunning())
         {
            if(enaTimer) dragOutsideTimer.start();
            else dragOutsideTimer.stop();
         }
         
         repaint();
      }
   }

   public void mouseReleased(MouseEvent e) 
   {
      if(isDraggingTab)
      {
         int dropPos = getVisibleTabUnder(dragCurrPt, true);
         if(dropPos != pressTab) model.moveTab(pressTab, dropPos);
         layoutTabs();
      }
      pressTab = -1;
      isDraggingTab = false;
      dragCurrPt = null;
      setCursor(Cursor.getDefaultCursor());
      repaint();
   }

   /**
    * When user clicks on an unselected tab or on the "close" button of any tab, forward the gesture to the tab model. 
    * No action is taken directly, since the model may choose to veto the request. If the user clicks on the "Show 
    * others" icon, a popup menu is raised so the user can select one of the currently hidden tabs.
    */
   public void mouseClicked(MouseEvent e) 
   {
      Point p = e.getPoint();
      if(p != null && (e.getButton() == MouseEvent.BUTTON1))
      {
         int i = getVisibleTabUnder(p, false);
         if(i < 0 && nVisTabs < model.getNumTabs() && isOnMoreIcon(p))
         {
            SwingUtilities.invokeLater(new Runnable() {
               public void run() {raiseHiddenTabsMenu();}
            });
         }
         else if(isOnTabClose(i, p))
            model.closeTab(i);
         else if(i != model.getSelectedTab())
            model.setSelectedTab(i);
      }
   }
   
   /** 
    * Helper method adjusts the visible tab range during the drag-tab gesture. During this gesture, the "more tabs"
    * icon that raises the "hidden tabs" menu is not drawn, and the number of visible tabs is set to use the entire
    * width of the tab strip, even if the last visible tab is cut off on the right.
    * <p>The current index of the first visible tab is updated IAW the argument specified, then both that index and the
    * number of visible tabs are adjusted to ensure both are valid and that the set of visible tabs fills the strip
    * width (if there are more tabs than can fit, of course!). Unlike {@link #layoutTabs()}, the visible tab range is
    * NOT adjusted to ensure it contains the current selected tab, since the user is in the middle of moving a tab to a
    * different position in the tab list.</p>
    * 
    * @param adj Change in index of first visible tab. 
    */
   private void adjustVisibleTabRangeDuringDrag(int adj)
   {
      int nTabs = tabWidths.size();
      if(adj != 0)
      {
         iFirstVisTab += adj;
         if(iFirstVisTab < 0) iFirstVisTab = 0;
         else if(iFirstVisTab + nVisTabs >= nTabs)
         {
            iFirstVisTab = nTabs - nVisTabs;
         }
      }
      
      int w = getTabRangeWidth(iFirstVisTab, iFirstVisTab+nVisTabs-1);
      while(w < getWidth() && (iFirstVisTab+nVisTabs < nTabs))
      {
         ++nVisTabs;
         w = getTabRangeWidth(iFirstVisTab, iFirstVisTab+nVisTabs-1);
      }
   }
   
   /** Index of tab pressed at the beginning of a drag gesture to reposition that tab in the strip. -1 otherwise. */
   private int pressTab = -1;
   /** Flag set during a drag gesture to reposition a tab within the strip. */
   private boolean isDraggingTab = false;
   /** Current location of mouse cursor during a drag gesture that repositions a tab within the strip. */
   private Point dragCurrPt = null;
   /** Offset from left edge of dragged tab to the drag cursor location. Need this to draw image of dragged tab. */
   private int dragOffsetX = 0;
   /** 
    * Swing timer fires at 300ms intervals when the mouse cursor is dragged outside strip and there are hidden tab 
    * positions to which the user could move the dragged tab. The timer handler increments or decrements the index of
    * the first visible tab until reaching either end of the tab list.
    */
   private Timer dragOutsideTimer = null;
   
   /** The index position of the tab position for which a rollover effect is currently painted. */
   private int rolloverTab = -1;
   /** If set, then the current rollover tab's "close" icon is highlighted because cursor is/was on top of it. */
   private boolean rolloverTabClose = false;
   /** If set, then the "Show others" icon is currently highlighted because cursor is/was on top if it. */
   private boolean rolloverMoreIcon = false;
   
   private final Color TRANSPARENT_BLACK = new Color(0,0,0,0);
   
   /** 
    * Helper method erases the current rollover effect (if any) and paints the new one (again, if any) based on the 
    * specified position of the cursor.
    * @param p The cursor position. If <code>null</code>, the method only erases the current rollover effect.
    */
   private void paintRolloverEffect(Point p)
   {
      int nextRolloverTab = getVisibleTabUnder(p, false);
      boolean nextRolloverTabClose = isOnTabClose(nextRolloverTab, p);
      boolean nextRolloverMoreIcon = (nextRolloverTab == -1) && isOnMoreIcon(p);
      
      if(nextRolloverTab == rolloverTab && nextRolloverTabClose == rolloverTabClose && 
            nextRolloverMoreIcon == rolloverMoreIcon) return;
      
      int iSelectedTab = model.getSelectedTab();
      
      Graphics2D g2d = (Graphics2D) getGraphics();
      if(g2d ==  null) return;
      try
      {
         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

         // erase previous rollover graphic. However, we don't hide close button on the selected tab, but we make 
         // sure it is returned to the non-rollover form if cursor is no longer over that tab.
         if(rolloverTab > -1 && model.isTabClosable(rolloverTab))
         {
            int x = 0;
            for(int i=iFirstVisTab; i<=rolloverTab; i++) x += tabWidths.get(i);
            x -= ICONGAP_H + closeIcon.getIconWidth();
            if(rolloverTab != iSelectedTab && rolloverTab != nextRolloverTab)
            {
               if(isOpaque())
               {
                  g2d.setColor(getBackground());
                  g2d.fillRect(x, ICONGAP_V, closeIcon.getIconWidth(), closeIcon.getIconHeight());
               }
               else repaint(new Rectangle(x, ICONGAP_V, closeIcon.getIconWidth(), closeIcon.getIconHeight()));
            }
            else if(rolloverTab == iSelectedTab && rolloverTabClose && nextRolloverTab != iSelectedTab)
            {
               closeIcon.paintIcon(this, g2d, x, ICONGAP_V);
            }
         }
         else if(rolloverMoreIcon)
         {
            int x = 0;
            for(int i=iFirstVisTab; i<iFirstVisTab+nVisTabs; i++) x += tabWidths.get(i);
            
            g2d.translate(x, 0);
            moreIcon.setColor(Color.BLACK);
            moreIcon.paintIcon(this, g2d, ICONGAP_H*2, (TABHEIGHT-moreIcon.getIconHeight())/2);
            g2d.translate(-x, 0);
         }
         
         // draw next rollover graphic
         rolloverTab = nextRolloverTab;
         rolloverTabClose = nextRolloverTabClose;
         rolloverMoreIcon = nextRolloverMoreIcon;
         
         if(rolloverTab > -1 && model.isTabClosable(rolloverTab))
         {
            Icon icon = rolloverTabClose ? rolloverCloseIcon : closeIcon;
            int x = 0;
            for(int i=iFirstVisTab; i<=rolloverTab; i++) x += tabWidths.get(i);
            icon.paintIcon(this, g2d, x-ICONGAP_H-icon.getIconWidth(), ICONGAP_V);
         }
         else if(rolloverMoreIcon)
         {
            int x = 0;
            for(int i=iFirstVisTab; i<iFirstVisTab+nVisTabs; i++) x += tabWidths.get(i);
            g2d.translate(x, 0);
            moreIcon.setColor(Color.WHITE);
            moreIcon.paintIcon(this, g2d, ICONGAP_H*2, (TABHEIGHT-moreIcon.getIconHeight())/2);
            g2d.translate(-x, 0);
         }
      }
      finally{ if(g2d != null) g2d.dispose(); }
   }
   
   /**
    * Get the zero-based index of the tab currently under the specified point within the tab strip, optionally ignoring
    * the point's Y-coordinate.
    * @param p A point in the tab strip, in device coordinates WRT origin at top-left corner.
    * @param force If true and the point is outside the bounds of any tab in the strip, then the method selects the 
    * tab that is closest to the point's X coordinate. The method will always return a valid tab index in this case, 
    * unless there are no tabs.
    * @return Index of tab under (or nearest, if <i>force</i> is set) the point, or -1 if there is no such tab.
    */
   private int getVisibleTabUnder(Point p, boolean force)
   {
      if(p == null || nVisTabs == 0) return(-1);
      int w = getWidth();
      int x = p.x;
      if(force)
      {
         if(p.x < 0) x = 0;
         else if(p.x >= w) x = w - 1;
      }
      int y = force ? TABHEIGHT : p.y;
      
      if(x < 0 || x >=w || y < 0 || y > TABHEIGHT) return(-1);
      
      int xTab = 0;
      for(int i=iFirstVisTab; i<iFirstVisTab+nVisTabs; i++)
      {
         int tabW = tabWidths.get(i);
         if(x >= xTab && x < xTab + tabW) return(i);
         xTab += tabW;
      }
      return(force ? (iFirstVisTab+nVisTabs-1) : -1);
   }
   
   /**
    * Is the specified point currently on the "close" icon of the specified tab?
    * @param tabPos Zero-based tab position.
    * @param p A point in the tab strip, in device coordinates WRT origin at top-left corner.
    * @return <code>True</code> iff point is within the bounds of the specified tab's close icon <em>AND the specified 
    * tab is closable</em>.
    */
   private boolean isOnTabClose(int tabPos, Point p)
   {
      if(tabPos < iFirstVisTab || tabPos >= iFirstVisTab+nVisTabs || p == null || !model.isTabClosable(tabPos))
         return(false);
      if(p.y < ICONGAP_V || p.y >= ICONGAP_V+closeIcon.getIconHeight()) return(false);
      int x = 0;
      for(int i=iFirstVisTab; i<=tabPos; i++) x += tabWidths.get(i);
      x -= ICONGAP_H + closeIcon.getIconWidth();
      return(p.x >= x && p.x <= x + closeIcon.getIconWidth());
   }
   
   /**
    * Is the specified point currently on the tab strip's "Show others" icon?
    * @param p A point in the tab strip, in device coordinates WRT origin at top-left corner.
    * @return <code>True</code> iff point is within the bounds of the "Show others" icon.
    */
   private boolean isOnMoreIcon(Point p)
   {
      if(p == null || nVisTabs == model.getNumTabs()) return(false);
      if(p.y < ICONGAP_V || p.y > ICONGAP_V+16) return(false);
      int x = ICONGAP_H;
      for(int i=iFirstVisTab; i<iFirstVisTab+nVisTabs; i++) x += tabWidths.get(i);
      return(p.x >= x && p.x <= x+16);
   }

   /**
    * Whenever the underlying tab model changes state, the <code>TabStrip</code> rebuilds the tab layout and repaints 
    * itself entirely, erasing any existing rollover effect.
    * @see ChangeListener#stateChanged(ChangeEvent)
    */
   public void stateChanged(ChangeEvent e)
   {
      if(e.getSource() == model)
      {
         layoutTabs();
         rolloverTab = -1;
         rolloverTabClose = false;
         rolloverMoreIcon = false;
         revalidate();
         repaint();
      }
   }
   
   /**
    * Helper method prepares and raises the popup menu by which the user can peruse all currently hidden tabs and 
    * select one of them. The popup menu is raised under the "Show others" icon, although its position may be adjusted 
    * as needed to ensure it is onscreen.
    */
   private void raiseHiddenTabsMenu()
   {
      if(nVisTabs == model.getNumTabs()) return;
      
      JPopupMenu popup = new JPopupMenu();
      for(int i=0; i<model.getNumTabs(); i++) if(i < iFirstVisTab || i >= iFirstVisTab+nVisTabs)
      {
         JMenuItem mi = new JMenuItem(tabLabels.get(i), model.getTabIcon(i));
         mi.setActionCommand(Integer.toString(i));
         mi.addActionListener(this);
         popup.add(mi);
      }
      popup.pack();
      
      int x = ICONGAP_H;
      for(int i=iFirstVisTab; i<iFirstVisTab+nVisTabs; i++) x += tabWidths.get(i);
      popup.show(this, x, ICONGAP_V+16);
   }

   /**
    * Handler responds to selections from the popup menu that lists any hidden tabs, or two regular timer events fired
    * during an animated drag-tab gesture when the drag location is outside the horizontal bounds of the tab strip.
    * <p>In the former case, the request to display and select the hidden tab is forwarded to the model, which may 
    * choose to veto it. In the latter case, depending on the current drag location and the index of the first 
    * visible tab, that index may be incremented or decremented to bring a hidden tab into view, and then the strip
    * is repainted.</p>
    */
   public void actionPerformed(ActionEvent e)
   {
      if(e.getSource() == dragOutsideTimer)
      {
         if(dragCurrPt == null || !isDraggingTab) 
            dragOutsideTimer.stop();
         else if(dragCurrPt.x < 0 && iFirstVisTab > 0)
         {
            adjustVisibleTabRangeDuringDrag(-1);
            repaint();
         }
         else if(dragCurrPt.x > getWidth() && (iFirstVisTab + nVisTabs < tabWidths.size()))
         {
            adjustVisibleTabRangeDuringDrag(1);
            repaint();
         }
         return;
      }
      
      int which = -1;
      try { which = Integer.parseInt(e.getActionCommand()); }
      catch(Throwable t) {}
      if(which >= 0)
         model.setSelectedTab(which);
   }
   
   /** 
    * Overridden to display the tooltip associated with the particular tab underneath the cursor. If the cursor is over
    * a "close tab" icon on a closable tab, it displays a model-provided custom tip for that tab's close action, or the
    * default tip "Close" if no custom tip is provided. Finally, if the cursor is over the "Show others" icon, the tip 
    * simply reads "Show others".
    * @see JComponent#getToolTipText(MouseEvent)
    */
   @Override
   public String getToolTipText(MouseEvent e)
   {
      Point p = e.getPoint();
      int iTab = getVisibleTabUnder(p, false);
      String s = null;
      if(iTab >= 0)
      {
         if(isOnTabClose(iTab, p))
         {
            s = model.getCloseTabToolTip(iTab);
            if(s == null || s.isEmpty()) s = "Close";
         }
         else s = model.getTabToolTip(iTab);
      }
      else if(isOnMoreIcon(p)) 
         s = "Show Others";

      return(s);
   }
}
