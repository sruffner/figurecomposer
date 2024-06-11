package com.srscicomp.common.ui;

import javax.swing.Icon;
import javax.swing.event.ChangeListener;

/**
 * <code>TabStripModel</code> defines the requirements on the "data model" for the custom <code>TabStrip</code> control.
 * The underlying data model is an ordered list of tab entries; the model supplies a label, icon, and tooltip for each 
 * tab entry. In addition, the model can configure individual tabs as closable or not and provide a tooltip for each
 * closable tab's button-like "close" icon.
 * <p><code>TabStrip</code> will register itself as a <code>ChangeListener</code> on the model; whenever any change 
 * occurs in the list of tab entries, the model implementation must notify all listeners that it has changed. 
 * <code>TabStrip</code> repaints itself in response.</p>
 * @see TabStrip
 * @author sruffner
 */
public interface TabStripModel
{
   /**
    * Get the number <em>N</em> of existing tabs. Tab position is always an integer in <em>[0..N-1]</em>.
    * @return Current tab count.
    */
   int getNumTabs();
   
   /**
    * Get the position of the selected tab.
    * @return Zero-based index position of the tab that is currently selected in the model. The model should ALWAYS 
    * return a valid index unless the model is empty, in which case it should return -1.
    */
   int getSelectedTab();
   
   /**
    * Select the specified tab. <code>TabStrip</code> will invoke this method if the user clicks on a tab in the strip
    * that is not currently selected. An implementation may choose to reject the action by doing nothing. The 
    * <code>TabStrip</code> will only repaint itself if it receives a change notification from the model!
    * @param tabPos Zero-based index of tab to be selected.
    */
   void setSelectedTab(int tabPos);
   
   /**
    * Add the specified listener to this <code>TabStripModel</code>'s listener list.
    * @param l The listener. If <code>null</code>, or if the listener is already registered, method has no effect.
    */
   void addChangeListener(ChangeListener l);
   
   /**
    * Remove the specified listener from this <code>TabStripModel</code>'s listener list.
    * @param l The listener. If <code>null</code>, or if the listener is not already registered, method has no effect.
    */
   void removeChangeListener(ChangeListener l);
   
   /**
    * Get the label for the specified tab.
    * @param tabPos Zero-based index position of tab.
    * @return The tab's label. If <code>null</code>, the <code>TabStrip</code> will use the default label "Tab N", 
    * where N is the tab position. Keep labels as short as possible; supply a tooltip to provide additional identifying
    * information for the tab.
    */
   
   String getTabLabel(int tabPos);
   
   /**
    * Get the tooltip for the specified tab.
    * @param tabPos Zero-based index position of tab.
    * @return The tab's tooltip, or <code>null</code> if it has none.
    */
   String getTabToolTip(int tabPos);
   
   /**
    * Get the icon for the specified tab.
    * @param tabPos Zero-based index position of tab.
    * @return The tab's icon, or <code>null</code> if no icon should be drawn. Icons should be 16x16.
    */
   Icon getTabIcon(int tabPos);
   
   /**
    * Can the specified tab be closed by the user? If so, <code>TabStrip</code> will include a button-like icon on the
    * tab that, when pressed, will invoke this model's <code>closeTab(int)</code> method.
    * @param tabPos Zero-based index position of tab.
    * @return <code>True</code> if tab is closable; else <code>false</code>.
    */
   boolean isTabClosable(int tabPos);
   
   /** 
    * Get the tool tip for the button-like "close tab" icon on the specified tab.
    * @param tabPos Zero-based index position of tab.
    * @return A <i>short</i> tool tip for the "close tab" icon. If null or empty string is returned, the tab strip will 
    * use the default tool tip: "Close".
    */
   String getCloseTabToolTip(int tabPos);
   
   /**
    * Close the specified tab. <code>TabStrip</code> will invoke this method if the user clicks on the "close tab" icon 
    * of a tab in the strip. An implementation may choose to reject the action by doing nothing, but it is better to 
    * implement <code>isTabClosable(int)</code> so that the close action is disabled altogether (icon not drawn). The 
    * <code>TabStrip</code> will only repaint itself if it receives a change notification from the model!
    * @param tabPos Zero-based index of tab to be closed.
    */
   void closeTab(int tabPos);
   
   /** 
    * Does this tab strip model support repositioning of tabs within the strip?
    * @return True if the model supports tab repositioning. The tab strip will disable its "drag tab" feature if this
    * method returns false.
    */
   boolean supportsTabRepositioning();
   
   /**
    * Change the index position of a tab.
    * @param fromPos The current position of the tab to be moved.
    * @param toPos The target position for the tab (before it is removed from its current location). To position the 
    * tab at the end of the strip, this must be set to the number of tabs in the strip.
    * @return True if tab position was updated; false if the initial tab position is invalid (with the exception noted
    * above for <i>toPos</i>, if the two positions are the same, or if the model does not support the operation.
    */
   boolean moveTab(int fromPos, int toPos);
}
