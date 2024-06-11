package com.srscicomp.common.ui;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * A table cell editor that uses a <code>JComboBox</code> as the cell editor delegate. It tweaks the super-class
 * implementation in the following ways:
 * <ul>
 * <li>The combo box's drop down list is raised immediately when the editing session begins, and the focus is always
 * put on the combo box's editor (if it is editable).</li>
 * <li>When the keyboard focus moves to some other component on the UI, the super class implementation leaves the
 * editor in place. <code>ComboCellEditor</code> cancels the editing session in this case.</li>
 * <li>When an editing session ends or is cancelled, this implementation tries to restore the current table selection to
 * the cell that was being edited in the first place. It also tries to restore the keyboard focus to the table.</li>
 * </ul>
 * @author sruffner
 */
public class ComboCellEditor<E> extends DefaultCellEditor
      implements PopupMenuListener, PropertyChangeListener, AncestorListener
{
   /**
    * Construct a combo box cell editor.
    * @param comboBox The combo box that serves as the cell editor delegate component.
    */
   public ComboCellEditor(JComboBox<E> comboBox) { super(comboBox); }

   /**
    * Return the value entered into this combo box cell editor. If the selected index is valid, the method defers to
    * {@link JComboBox#getSelectedItem()}. If not, it returns the value that was in the combo box's editor field when
    * the drop-down list last disappeared.
    */
   @Override public Object getCellEditorValue()
   {
     return((getComponent().getSelectedIndex() < 0) ? editorFieldValue : getComponent().getSelectedItem());
   }

   @Override public void cancelCellEditing()
   {
      super.cancelCellEditing();
      cleanUpAfterEditingSession();
   }

   @Override public boolean stopCellEditing()
   {
      boolean stopped = super.stopCellEditing();
      if(stopped) cleanUpAfterEditingSession();
      return(stopped);
   }

   @Override public Component getTableCellEditorComponent(
         JTable table, Object value, boolean isSelected, int row, int column) 
   {
       super.getTableCellEditorComponent(table, value, isSelected, row, column);
       prepareForEditingSession();
       clientTable = table;
       invokingRow = row;
       invokingCol = column;
       editorFieldValue = value;
       return(getComponent());
   }

   /** Shows the drop down list for the combo box cell editor. */
   protected void showPopup() 
   {
      SwingUtilities.invokeLater(() -> getComponent().setPopupVisible(true));
   }

   /**
    * Prepared for cell editor to be installed in client table. Listen for the 'ancestor-added' event on combo box that
    * will indicate when this happens, for changes in the permanent focus owner, and for pop-up menu events from the
    * combo box's drop-down list.
    */
   private void prepareForEditingSession() 
   {
      editorGotFocus = false;
      KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", this);
      getComponent().addAncestorListener(this);
      if(getComponent().isEditable()) getComponent().addPopupMenuListener(this);
   }

   /**
    * Clean up after an editing session and try to restore the focus to the client table and the current selection to 
    * the table cell that was just edited.
    */
   private void cleanUpAfterEditingSession()
   {
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("permanentFocusOwner", this);
      getComponent().removePopupMenuListener(this);
      if(clientTable != null) 
      {
         clientTable.requestFocusInWindow();
         if(invokingRow >=0 && invokingRow < clientTable.getRowCount())
            clientTable.setRowSelectionInterval(invokingRow, invokingRow);
         if(invokingCol >=0 && invokingCol < clientTable.getColumnCount())
            clientTable.setColumnSelectionInterval(invokingCol, invokingCol);
         clientTable = null;
         invokingRow = -1;
         invokingCol = -1;
      }
   }
   
   /** Convenience for type cast. */
   @SuppressWarnings("unchecked")
   @Override public JComboBox<E> getComponent() { return((JComboBox<E>) super.getComponent()); }

   /** 
    * If combo box is editable, this handler remembers the contents of the combo box editor field when the popup menu
    * disappears. This deals with the use case in which a user types a new value into the editor field that is not in
    * the combo box's drop-down list. The default combo box cell editor implementation will clear the editor field by
    * calling {@link JComboBox#setSelectedIndex(int)} with a value of -1, and the edited value is lost.
    */
   public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
   {
      editorFieldValue = getComponent().getEditor().getItem();
   }
   public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
   public void popupMenuCanceled(PopupMenuEvent e) {}

   public void propertyChange(PropertyChangeEvent e)
   {
      KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
      if(getComponent().isEditable())
      {
         if(!editorGotFocus)
         {
            if(kfm.getPermanentFocusOwner() == getComponent().getEditor().getEditorComponent()) 
            {
               editorGotFocus = true;
               showPopup();
            }
         }
         else if(kfm.getPermanentFocusOwner() != getComponent().getEditor().getEditorComponent())
         {
            // if editor loses the permanent focus once it had it, cancel editing session
            cancelCellEditing();
         }
      }
      else 
      {
         if(!editorGotFocus)
         {
            if(kfm.getPermanentFocusOwner() == getComponent())
            {
               editorGotFocus = true;
            }
         }
         else
         {
            if(kfm.getPermanentFocusOwner() != getComponent())
               cancelCellEditing();
         }
      }
   }

   public void ancestorAdded(AncestorEvent e)
   {
      getComponent().removeAncestorListener(this);
      getComponent().getEditor().getEditorComponent().requestFocusInWindow();
   }
   public void ancestorMoved(AncestorEvent e) {}
   public void ancestorRemoved(AncestorEvent e) {}
   
   /** The client table for this combo box cell editor. */
   private JTable clientTable = null;
   /** The row index of the cell for which the editor was invoked. */
   private int invokingRow = -1;
   /** The column index of the cell for which the editor was invoked. */
   private int invokingCol = -1;
   /** The value in the combo box's editor field when the drop-down list last disappeared (editable CB only). */
   private Object editorFieldValue = null;
   /** Flag set once combo box or its editor field gets the keyboard focus. Cleared when editing session starts. */
   private boolean editorGotFocus = false;
}
