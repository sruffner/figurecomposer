package com.srscicomp.common.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * An extension of <code>JSpinner</code> which embeds a <code>NumericTextField</code> as the editor component. It is 
 * a better alternative to using <code>JSpinner</code> with its default number editor because:
 * <ul>
 *    <li>It will not accept non-numeric character input.</li>
 *    <li>It autocorrects numeric values that are out-of-range.</li>
 *    <li>It uses the same font as a typical <code>JTextField</code>, so its appearance is consistent with that of
 *    text fields elsewhere on the GUI (not sure why <code>JSpinner</code>'s editor has a different font).</li>
 * </ul>
 * 
 * @author sruffner
 */
public class IntegerSpinner extends JSpinner implements ActionListener, ChangeListener
{
   /**
    * Construct a spinner control that will span a specified range of integer values.
    * @param currValue The current value.
    * @param minValue The minimum allowed value.
    * @param maxValue The maximum allowed value.
    * @param stepSize The increment size for spinner operation. 
    * @throws IllegalArgumentException if this expression is false: minValue <= currValue <= maxValue.
    */
   public IntegerSpinner(int currValue, int minValue, int maxValue, int stepSize)
   {
      super();
      super.setModel(new SpinnerNumberModel(currValue, minValue, maxValue, stepSize));
      NumericTextField ntf = new NumericTextField(minValue, maxValue);
      ntf.setHorizontalAlignment(NumericTextField.TRAILING);
      ntf.setValue(currValue);
      ntf.addActionListener(this);
      addChangeListener(this);
      super.setEditor(ntf);
   }

   /** 
    * Get the current integer value in this spinner's integer number model. Since the spinner model is restricted to 
    * integers, this will always return a valid value within the range specified at construction time.
    * @return The current spinner value.
    */
   public int getIntValue() { return(((Number) getValue()).intValue()); }
   
   /** Overridden to prevent changing the editor component. */
   @Override
   public void setEditor(JComponent editor) {}

   /** Overridden to prevent changing the spinner model, which is set at construction time and cannot be changed. */
   @Override
   public void setModel(SpinnerModel model) {}

   /** Overridden to install the same tip text on the spinner's editor component. */
   @Override public void setToolTipText(String text)
   {
      super.setToolTipText(text);
      NumericTextField ntf = (NumericTextField)getEditor();
      if(ntf != null) ntf.setToolTipText(text);
   }

   public void actionPerformed(ActionEvent e)
   {
      NumericTextField ntf = (NumericTextField) getEditor();
      if(e.getSource() == ntf)
      {
         // must pass an Integer object b/c SpinnerNumberModel will accept a Number and treat it as a Double!
         getModel().setValue(ntf.getValue().intValue());
      }
   }

   public void stateChanged(ChangeEvent e)
   {
      NumericTextField ntf = (NumericTextField) getEditor();
      ntf.setValue(getIntValue());
   }
}
