package com.srscicomp.fc.uibase;

import java.awt.AWTEventMulticaster;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.Border;

import com.srscicomp.common.ui.JPreferredSizePanel;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.fig.Measure.Constraints;

/**
 * <b>MeasureEditor</b> is a custom component for displaying and editing a linear measurement, {@link Measure}. It 
 * consists of a numeric text field and an accompanying units label wrapped in a {@link JPreferredSizePanel}. The user 
 * enters the numeric value of the measurement in the text field, and selects units via one of several recognized 
 * keystrokes: 'i' for inches, 'c' for centimeters, 'm' for millimeters, 'p' for typographical points, '%' or 'n' for 
 * percentage units, and 'u' for user units. A {@link Constraints} object passed to the constructor restricts
 * the kinds and range of measurements that the user may enter into the editor.
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
public class MeasureEditor extends JPreferredSizePanel implements ActionListener, FocusListener
{
   /** Tool tip installed on units label that lists the keystrokes that select measurement units. */
   private final static String unitsTip = "Unit keystrokes: i=inches, c=cm, m=mm, p=pt, n=%, %, u";
   
   /** The numeric value of the measurement is entered in this customized text field. */
   private NumericTextField valueField;

   /** The units of measurement are reflected in this label. */
   private JLabel unitLabel;

   /** The current measure entered in this editor AND validated. */
   private Measure currentValidMeasure;

   /**
    * Get the current valid measurement entered into this <b>MeasureEditor</b>. 
    * @return The current measurement reflected in the editor. If the user is currently entering a new value into the 
    * editor, this method does NOT return the current value, which may not be valid; it only returns the last 
    * <b>validated</b> entry.
    */
   public Measure getMeasure() { return(currentValidMeasure); }

   /**
    * Set the measurement reflected in this <b>MeasureEditor</b>. An action event is NOT fired.
    * 
    * @param m The new measurement. Any value that does not satisfy the measure constraints imposed on this editor will
    * be rejected.
    * @return True if new measurement was set; false if it was rejected.
    */
   public boolean setMeasure(Measure m)
   {
      if(!constraints.satisfiedBy(m)) return(false);
      currentValidMeasure = m;
      valueField.setValue(m.getValue());
      unitLabel.setText(m.getUnits().toString());
      return(true);
   }

   /** The constraints enforced on any measurement entered in this <b>MeasureEditor</b>. */
   private Constraints constraints;

   /**
    * Construct a <b>MeasureEditor</b>. 
    * @param columns The number of text columns in the embedded numeric field. If greater than 0, this will govern 
    * the preferred size of the field. Otherwise, the preferred size is based on the longest numerical value that 
    * may be entered, given the measurement constraints supplied.
    * @param c The constraints enforced on any measurement entered. If this is null, only the minimum possible 
    * constraints are placed on any entered measurement.
    */
   public MeasureEditor(int columns, Constraints c)
   {
      constraints = (c == null) ? new Constraints() : c;
      valueField = new NumericTextField(constraints.min, constraints.max, 
            constraints.nMaxSigDigits, constraints.nMaxFracDigits);
      valueField.addActionListener(this);
      valueField.addFocusListener(this);
      if(columns > 0) valueField.setColumns(columns);
      else
      {
         double min = (constraints.allowPct || constraints.allowUser) ? constraints.min : constraints.measuredMinMI;
         double max = (constraints.allowPct || constraints.allowUser) ? constraints.max : constraints.measuredMaxMI;
         
         int iMin = constraints.nMaxFracDigits + ((min < 0) ? 2 : 1);
         double d = Math.abs(min);
         iMin += (d <= 1) ? 1 : Utilities.log10(d);
         
         int iMax = constraints.nMaxFracDigits + ((max < 0) ? 2 : 1);
         d = Math.abs(max);
         iMax += (d <= 1) ? 1 : Utilities.log10(d);
         
         valueField.setColumns(Math.min((iMin < iMax) ? iMax : iMin, 20));
      }
         
      valueField.setHorizontalAlignment(NumericTextField.RIGHT);
      currentValidMeasure = new Measure(valueField.getValue().doubleValue(), Measure.Unit.IN);
      installKeyBindings();
      
      unitLabel = new JLabel("mm", JLabel.CENTER);
      Font font = valueField.getFont();
      unitLabel.setFont(font.deriveFont(Font.BOLD, font.getSize2D()));
      unitLabel.setPreferredSize(unitLabel.getPreferredSize());
      unitLabel.setText("in");
      unitLabel.setToolTipText(unitsTip);
      
      setLayout(new BorderLayout());
      add(valueField, BorderLayout.CENTER);
      add(unitLabel, BorderLayout.EAST);
      Border border = BorderFactory.createLineBorder(unitLabel.getBackground().darker().darker(), 1);
      valueField.setBorder(border);
      setBorder(border);
      setAlignmentY(JPanel.CENTER_ALIGNMENT);
   }

   @Override public boolean isRequestFocusEnabled() { return(valueField.isRequestFocusEnabled()); }
   @Override public void requestFocus() { valueField.requestFocus(); }
   @Override public boolean requestFocusInWindow() { return(super.requestFocusInWindow()); }
   @Override public void setRequestFocusEnabled(boolean b) { valueField.setRequestFocusEnabled(b); }
   @Override public boolean hasFocus() { return(valueField.hasFocus()); }
   
   /**
    * Overridden so that the specified tool tip is displayed when the mouse hovers over the embedded numeric text field.
    * A fixed tip explaining the unit-selection keystrokes is installed on the embedded units label.
    * @see javax.swing.JComponent#setToolTipText(String)
    */
   @Override public void setToolTipText(String tip) { valueField.setToolTipText(tip); }

   /** 
    * The action listener registered with this editor. Actually an instance of <code>AWTEventMulticaster</code>, so 
    * multiple listeners are supported.
    */
   private ActionListener actionListener = null;

   /**
    * Add the specified action listener to this <code>MeasureEditor</code>'s listener list. Registered action listeners 
    * are notified whenever the user changes the units and/or value of the edited <code>Measure</code>.
    * @param l The listener to add. If <code>null</code>, method has no effect.
    */
   public synchronized void addActionListener(ActionListener l) 
   {
      actionListener = AWTEventMulticaster.add(actionListener, l);
   }
   
   /**
    * Remove the specified action listener from this <code>MeasureEditor</code>'s listener list.
    * @param l The listener to remove. If <code>null</code> or not a registered listener, method has no effect.
    */
   public synchronized void removeActionListener(ActionListener l) 
   {
      actionListener = AWTEventMulticaster.remove(actionListener, l);
   }
   
   /** 
    * Send an action event to all registered listeners, indicating that the value in this <code>MeasureEditor</code> 
    * has changed in some way.
    */
   private void fireActionEvent() 
   {
       ActionListener listener = actionListener;
       if(listener != null) listener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ""));
   }

   /**
    * Update the current measurement displayed in this <code>MeasureEditor</code>. If the update is successful, notify 
    * all registered action listeners of the change.
    * @param m The new measurement.
    * @return <code>True</code> if change was successful, <code>false</code> if specified measure does not satisfy the
    * measurement constraints imposed on this editor.
    */
   private boolean setMeasureAndNotify(Measure m)
   {
      if(!setMeasure(m)) return(false);
      fireActionEvent();
      return(true);
   }
   
   /** Helper method that sets up key bindings for changing the current measurement units. */
   private void installKeyBindings()
   {
      InputMap im = valueField.getInputMap();
      im.put(KeyStroke.getKeyStroke('i'), switchToInches);
      im.put(KeyStroke.getKeyStroke('c'), switchToCM);
      im.put(KeyStroke.getKeyStroke('m'), switchToMM);
      im.put(KeyStroke.getKeyStroke('p'), switchToPT);
      im.put(KeyStroke.getKeyStroke('%'), switchToPercent);
      im.put(KeyStroke.getKeyStroke('n'), switchToPercent);
      im.put(KeyStroke.getKeyStroke('u'), switchToUserUnits);
   }
   
   /** Action which changes current units to inches. */
   private static Action switchToInches;

   /** Action which changes current units to centimeters. */
   private static Action switchToCM;

   /** Action which changes current units to millimeters. */
   private static Action switchToMM;

   /** Action which changes current units to typographical points. */
   private static Action switchToPT;

   /** Action which changes current units to percentage units. */
   private static Action switchToPercent;

   /** Action which changes current units to user units. */
   private static Action switchToUserUnits;

   static
   {
      switchToInches = new UpdateHandler();
      switchToInches.putValue(UpdateHandler.ACTION_COMMAND_KEY, Measure.Unit.IN.toString());
      switchToCM = new UpdateHandler();
      switchToCM.putValue(UpdateHandler.ACTION_COMMAND_KEY, Measure.Unit.CM.toString());
      switchToMM = new UpdateHandler();
      switchToMM.putValue(UpdateHandler.ACTION_COMMAND_KEY, Measure.Unit.MM.toString());
      switchToPT = new UpdateHandler();
      switchToPT.putValue(UpdateHandler.ACTION_COMMAND_KEY, Measure.Unit.PT.toString());
      switchToPercent = new UpdateHandler();
      switchToPercent.putValue(UpdateHandler.ACTION_COMMAND_KEY, Measure.Unit.PCT.toString());
      switchToUserUnits = new UpdateHandler();
      switchToUserUnits.putValue(UpdateHandler.ACTION_COMMAND_KEY, Measure.Unit.USER.toString());
   }

   /**
    * Helper class serves as an <code>Action</code> object to implement key-stroke actions that change the units of the 
    * currently edited measurement. It is placed in the input map of the <code>MeasureEditor</code>'s numeric text 
    * field. 
    * @author  sruffner
    */
   private static class UpdateHandler extends AbstractAction
   {
      private static final long serialVersionUID = 1L;

      public void actionPerformed(ActionEvent e)
      {
         String cmd = e.getActionCommand();
         Object src = e.getSource();
         MeasureEditor editor = null;
         if(src instanceof NumericTextField)
         {
            Container parent = ((NumericTextField)src).getParent();
            while(parent != null)
            {
               if(parent instanceof MeasureEditor)
               {
                  editor = (MeasureEditor) parent;
                  break;
               }
               parent = parent.getParent();
            }
         }
         else if(src instanceof MeasureEditor)
            editor = (MeasureEditor) src;
         
         if(editor == null) return;
         for(int i=0; i<Measure.ALLUNITS.length; i++) if(cmd.equals(Measure.ALLUNITS[i].toString()))
         {
            editor.changeUnits(Measure.ALLUNITS[i]);
            return;
         }
      }
   }

   /**
    * Change the units of the measurement currently entered into this <code>MeasureEditor</code>.
    * 
    * <p>This method may be called under two distinct circumstances:
    * <ul>
    * <li>The user has not changed the numeric value of the measurement. If both current and new units are non-relative, 
    * the measurement is converted to the new units such that the measured length is roughly unchanged (the precision 
    * constraint will prevent an exact conversion!). If the change does not violate the constraints imposed on this 
    * component, the current measurement is updated accordingly. Otherwise, the last valid measure is restored.</li>
    * <li>The user has changed the numeric value of the measurement. In this case, the new numerical value and the 
    * selected units define a new measure (no conversion is done). If this measure satisies the current measurement 
    * constraints, it is accepted. Otherwise, the last valid measure is restored.</li>
    * </ul>
    * </p>
    * @param u The new measurement units. If same as current units, no action is taken.
    */
   private void changeUnits(Measure.Unit u)
   {
      if(u == null) return;
      
      double d = valueField.getValue().doubleValue();
      if(d == currentValidMeasure.getValue())
      {
         Measure next = null;
         if(!(u.isRelative() || currentValidMeasure.getUnits().isRelative()))
            next = Measure.convertRealMeasure(currentValidMeasure, u, constraints);
         else
            next = new Measure(currentValidMeasure.getValue(), u);
         if(setMeasureAndNotify(next)) valueField.selectAll();
         else Toolkit.getDefaultToolkit().beep();
      }
      else
      {
         Measure next = new Measure(
               Utilities.limitSigAndFracDigits(d, constraints.nMaxSigDigits, constraints.nMaxFracDigits), u);
         if(!setMeasureAndNotify(next))
         {
            valueField.setValue(currentValidMeasure.getValue());
            valueField.selectAll();
            Toolkit.getDefaultToolkit().beep();
         }
         else
            valueField.selectAll();
      }
   }

   /**
    * Handler responds to changes in the numeric text field, updating the current measure and notifying any registered 
    * action listeners of the change. If the new measure is invalid, the last valid measure is restored.
    * @see ActionListener#actionPerformed(ActionEvent)
    */
   public void actionPerformed(ActionEvent e)
   {
      if(e.getSource() == valueField)
      {
         double d = valueField.getValue().doubleValue();
         Measure next = new Measure(
               Utilities.limitSigAndFracDigits(d, constraints.nMaxSigDigits, constraints.nMaxFracDigits), 
               currentValidMeasure.getUnits());
         if(!setMeasureAndNotify(next))
         {
            valueField.setValue(currentValidMeasure.getValue());
            valueField.selectAll();
            Toolkit.getDefaultToolkit().beep();
         }
      }
   }

   /**
    * When this <b>MeasureEditor</b> gets the keyboard focus, the numeric text field's contents are selected initially. 
    * That way, the user can immediately start typing in a new value.
    */
   public void focusGained(FocusEvent e) {valueField.selectAll();}
   public void focusLost(FocusEvent e) {}
}
