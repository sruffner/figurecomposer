package com.srscicomp.common.ui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;

import com.srscicomp.common.util.Utilities;

/**
 * <b>NumericTextField</b> is a customized {@link JTextField} that restricts input to integer or floating-point values
 * within a specified range. Floating-point numbers may be expressed in scientific notation, and the total number of 
 * significant digits is restricted. Optionally, one can also restrict the number of fractional digits (to the right of 
 * the decimal point) when the number is in decimal notation to [1..3]; this has the effect of restricting the absolute
 * value of the smallest number that can be entered to <i>pow(10, -N)</i>, where N is the allowed number of fractional 
 * digits. A custom  document filter is installed on the field to restrict input as the user types.
 * 
 * @author  sruffner
 */
public class NumericTextField extends JTextField implements ActionListener, FocusListener
{
   // A temporary test fixture...
   public static void main(String[] args)
   {
      GUIUtilities.initLookAndFeel();
      LocalFontEnvironment.initialize();
      
      final JFrame appFrame = new JFrame();
      appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);      
      
      JLabel intLabel = new JLabel("Int: ");
      JLabel fpLabel = new JLabel("FP: ");
      NumericTextField intField = new NumericTextField(-100000,100000);
      NumericTextField fpField = new NumericTextField(-180, 180, 4, 1);
      
      JPanel p = new JPanel();
      p.setBorder(BorderFactory.createEmptyBorder(25,25,25,25));
      p.add(intLabel);
      p.add(fpLabel);
      p.add(intField);
      p.add(fpField);
      
      SpringLayout layout = new SpringLayout();
      p.setLayout(layout);
      layout.putConstraint(SpringLayout.WEST, intLabel, 0, SpringLayout.WEST, p);
      layout.putConstraint(SpringLayout.WEST, intField, 0, SpringLayout.EAST, intLabel);
      layout.putConstraint(SpringLayout.WEST, fpLabel, 10, SpringLayout.EAST, intField);
      layout.putConstraint(SpringLayout.WEST, fpField, 0, SpringLayout.EAST, fpLabel);
      layout.putConstraint(SpringLayout.EAST, p, 0, SpringLayout.EAST, fpField);
      layout.putConstraint(SpringLayout.NORTH, intField, 10, SpringLayout.NORTH, p);
      layout.putConstraint(SpringLayout.SOUTH, p, 10, SpringLayout.SOUTH, intField);
      String vc = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vc, intLabel, 0, vc, intField);
      layout.putConstraint(vc, fpLabel, 0, vc, intField);
      layout.putConstraint(vc, fpField, 0, vc, intField);

      appFrame.add(p);

      Runnable runner = new MainFrameShower( appFrame );
      SwingUtilities.invokeLater( runner );
   }
   
   /** Construct a numeric text field restricted to integer input in [0..100]. */
   public NumericTextField() { this(0, 100); }

   /**
    * Construct a numeric text field restricted to integer input in a specified range. Initially, the field will display
    * "0" or, if "0" is outside the allowed range, the minimum or maximum integer value.
    * 
    * @param min The minimum integer value that may be entered.
    * @param max The maximum integer value that may be entered. Must be strictly less than <i>min</i>.
    */
   public NumericTextField(int min, int max)
   {
      super();
      setInputRestrictions(min, max, 0);
      addActionListener(this);
      addFocusListener(this);
   }

   /**
    * Restrict input to integer values in the specified range.
    * 
    * <p><b>Note</b>. If the number of text columns has not been set, changing input restrictions may change the field's
    * preferred size.</p>
    * 
    * @param min The minimum integer value that may be entered.
    * @param max The maximum integer value that may be entered. Must be strictly less than <i>min</i>.
    * @param curr The value that should be set in the field once restrictions are in place. The value is auto-corrected 
    * if it is out of bounds.
    */
   public void setInputRestrictions(int min, int max, int curr)
   {
      if(min >= max) throw new IllegalArgumentException("Minimum > maximum!");
      
      isInteger = true;
      minValue = min;
      maxValue = max;

      maxCharsNeeded = 0;
      calcMetrics();
      
      Document doc = getDocument();
      assert(doc instanceof AbstractDocument);
      NumericDocumentFilter filter = new NumericDocumentFilter(maxCharsNeeded, (minValue.intValue() < 0), false, 0);
      ((AbstractDocument)doc).setDocumentFilter(filter);
      
      setValue(curr);

      invalidate();
   }

   /**
    * Construct a numeric text field that admits floating-point numbers within a specified range and limits the number
    * of significant digits allowed in any numeric entry. The number of fractional digits (digits to the right of the
    * decimal point when the number is in decimal notation) is not restricted. Initially, the field will display 0 if it
    * is in range; otherwise, the minimum or maximum value.
    * 
    * <p><i>Note</i>. Since the number of fractional digits is not restricted, one can enter any floating-point value
    * in range. However, if that number has more significant digits than is allowed, it will be rounded accordingly.</p>
    * 
    * @param min The minimum value that may be entered. Cannot be NaN nor +/-Inf.
    * @param max The maximum value that may be entered. Cannot be NaN nor +/-Inf. 
    * @param nSig The maximum number of significant digits in any value entered. The <i>min</i> and <i>max</i>
    * arguments will be rounded if they have too many significant digits. Any number entered into the text field will 
    * likewise be rounded, if necessary. Restricted to [1..16].
    */
   public NumericTextField(double min, double max, int nSig)
   {
      this(min, max, nSig, 0);
   }

   /**
    * Construct a numeric text field that admits floating-point numbers within a specified range and limits both the
    * number of significant digits and the number of fractional digits in any numeric entry. Initially, the field will 
    * display 0 if it is in range; otherwise, the minimum or maximum value.
    * 
    * <p>"Fractional digits" refer to the digits right of the decimal point when the number is in decimal notation. When
    * the maximum number of fractional digits is set to N, the smallest non-zero floating-point value that can be 
    * entered is <i>+/-pow(10, -N)</i> -- if that value is within the specified range. For example, if N=2 and 
    * range = [-100..100], the absolute value of the smallest non-zero entry is 0.01, and one cannot enter values like 
    * 50.003, even though that falls within the specified range.</p>
    * 
    * <p>The limit on fractional digits does not apply to a number entered in scientific notation; that depends solely
    * on the maximum number of significant digits allowed.</p>
    * 
    * @param min The minimum value that may be entered. Cannot be NaN nor +/-Inf.
    * @param max The maximum value that may be entered. Cannot be NaN nor +/-Inf. 
    * @param nSig The maximum number of significant digits in any value entered. The <i>min</i> and <i>max</i>
    * arguments will be rounded if they have too many significant digits. Any number entered into the text field will 
    * likewise be rounded, if necessary. Restricted to [1..16].
    * @param nFrac The maximum number of fractional digits. Valid values lie in [1..3]. Otherwise, the argument is 
    * ignored and the number of fractional digits is only restricted by the number of significant digits allowed.
    */
   public NumericTextField(double min, double max, int nSig, int nFrac)
   {
      super();
      setInputRestrictions(min, max, 0.0, nSig, nFrac);
      addActionListener(this);
      addFocusListener(this);
      
   }
   /**
    * Restrict input to floating-point values within the specified range and with the specified number of significant
    * digits and fractional digits (the digits right of the decimal point in decimal notation).
    * 
    * <p>"Fractional digits" refer to the digits right of the decimal point when the number is in decimal notation. When
    * the maximum number of fractional digits is set to N, the smallest non-zero floating-point value that can be
    * entered is <i>+/-pow(10, -N)</i> -- if that value is within the specified range. For example, if N=2 and 
    * range = [-100..100], the absolute value of the smallest non-zero entry is 0.01, and one cannot enter values like 
    * 50.003, even though that falls within the specified range.</p>
    * 
    * <p>The limit on fractional digits does not apply to a number entered in scientific notation; that depends solely
    * on the maximum number of significant digits allowed.</p>
    * 
    * <p><i>Note</i>. If the number of text columns has not been set, changing input restrictions may change the 
    * field's preferred size.</p>
    * 
    * @param min The minimum value that may be entered. Cannot be NaN nor +/-Inf.
    * @param max The maximum value that may be entered. Cannot be NaN nor +/-Inf. 
    * @param nSig The maximum number of significant digits in any value entered. The <i>min</i> and <i>max</i>
    * arguments will be rounded if they have too many significant digits. Any number entered into the text field will 
    * likewise be rounded, if necessary. Restricted to [1..16].
    * @param nFrac The maximum number of fractional digits. Valid values lie in [1..3]. Otherwise, the argument is 
    * ignored and the number of fractional digits is not restricted.
    */
   public void setInputRestrictions(double min, double max, double curr, int nSig, int nFrac)
   {
      boolean ok = Utilities.isWellDefined(min) && Utilities.isWellDefined(max);
      if(!ok) throw new IllegalArgumentException("Invalid range specified");
      
      isInteger = false;
      maxSigDig = Utilities.rangeRestrict(RANGE_SIGDIG[0], RANGE_SIGDIG[1], nSig);
      maxFracDig = (nFrac >= RANGE_FRACDIG[0] && nFrac <= RANGE_FRACDIG[1]) ? nFrac : -1;
      
      minValue = Utilities.limitSigAndFracDigits(min, maxSigDig, maxFracDig);
      maxValue = Utilities.limitSigAndFracDigits(max, maxSigDig, maxFracDig);
      if(minValue.doubleValue() >= maxValue.doubleValue())
         throw new IllegalArgumentException("Bad range afer rounding to specified precision!");
      
      maxCharsNeeded = 0;
      calcMetrics();

      Document doc = getDocument();
      assert(doc instanceof AbstractDocument);
      NumericDocumentFilter filter = 
            new NumericDocumentFilter(maxCharsNeeded, (minValue.doubleValue() < 0), true, maxSigDig);
      ((AbstractDocument)doc).setDocumentFilter(filter);

      setValue(curr);

      invalidate();
   }

   /**
    * Get the numeric value currently entered in this numeric text field.
    * 
    * <p><b>NOTE</b> that not every real number can be represented by {@link Double}; "0.1" is the classic example. This
    * method returns a floating-point value close as possible to the numeric text entered in this field.</p>
    * 
    * @return A {@link Number} encapsulating the value entered. The method always returns a valid value, even if the
    * current text content does not represent a valid value (eg, when the contents are being actively modified). 
    */
   public Number getValue()
   {
      String s = validateNumeric(getText());
      return(isInteger ? Integer.parseInt(s) : Double.parseDouble(s));
   }

   /**
    * Set the number that appears in this numeric text field, auto-corrected and rounded as needed to satisfy the 
    * current input restrictions.
    * 
    * @param n The desired number. 
    */
   public void setValue(Number n)
   {
      if(isInteger)
      {
         int value = n.intValue();
         if(value < minValue.intValue()) value = minValue.intValue();
         else if(value > maxValue.intValue()) value = maxValue.intValue();
         lastValidValue = value;
         super.setText(Integer.toString(value));
      }
      else
      {
         double value = Utilities.limitSigAndFracDigits(n.doubleValue(), maxSigDig, maxFracDig);
         if(!Utilities.isWellDefined(value) || value < minValue.doubleValue()) value = minValue.doubleValue();
         else if(value > maxValue.doubleValue()) value = maxValue.doubleValue();
         
         lastValidValue = value;
         super.setText(Utilities.toString(value, maxSigDig, maxFracDig, true));    
      }
   }

   /**
    * Overridden to reject any text input that does not parse as a numeric value satisfying the constraints defined on
    * this numeric text field. If the text is numeric but violates a constraint, it will be auto-corrected to satisfy
    * the constraint. If it cannot be parsed at all, it is rejected and the last valid value is used.
    */
   @Override public void setText(String t) { super.setText(validateNumeric(t)); }

   /** Validates content of the numeric text field, auto-correcting invalid entries. */
   public void actionPerformed(ActionEvent e) { if(e.getSource() == this) validateContent(); }

   /**
    * Posts an action event whenever the keyboard focus is lost, so that text content is validated IAW the current 
    * input restrictions.
    */
   public void focusLost(FocusEvent e) { postActionEvent(); }
   public void focusGained(FocusEvent e) {}

   /** Helper method validates the current text content, auto-correcting it if necessary. */
   private void validateContent()
   {
      String s = getText();
      String sCorr = validateNumeric(s);
      if(!s.equals(sCorr))
      {
         super.setText(sCorr);
      }
   }

   /** 
    * Check the specified string to see if it can be parsed as a number which satisfies the current input restrictions 
    * on this numeric text field. If not, it is auto-corrected and a valid numeric string is returned.
    * @param s The string to be validated.
    */
   private String validateNumeric(String s)
   {
      String sRet = s;
      if(isInteger)
      {
         boolean corrected = false;
         int iValue;
         try
         {
            iValue = Integer.parseInt(s);
            if(iValue < minValue.intValue()) {corrected = true; iValue = minValue.intValue();}
            else if(iValue > maxValue.intValue()) {corrected = true; iValue = maxValue.intValue();}
         }
         catch(NumberFormatException nfe)
         {
            corrected = true;
            iValue = lastValidValue.intValue();
         }
         if(corrected)
            sRet = Integer.toString(iValue);
      }
      else
      {
         double dValue;
         try
         {
            dValue = Utilities.limitSigAndFracDigits(Double.parseDouble(s), maxSigDig, maxFracDig);
            if(dValue < minValue.doubleValue()) dValue = minValue.doubleValue();
            else if(dValue > maxValue.doubleValue()) dValue = maxValue.doubleValue();
         }
         catch(NumberFormatException nfe)
         {
            dValue = lastValidValue.doubleValue();
         }
         
         // even if value entered is in range, it may have had too many significant digits, or it could be presented
         // more compactly. This ensures we catch those scenarios.
         sRet = Utilities.toString(dValue, maxSigDig, maxFracDig, true);
      }
      
      return(sRet);
   }

   
   //
   // Sizing the field
   //
   
   /** Given input restrictions, this is the number of characters in the longest string that can be entered. */
   private int maxCharsNeeded = 0;
   
   /** Given input restrictions, this is the approx. length of the longest string that can be entered. */
   private int longestAdvance = 0;

   /** 
    * Get the maximum number of characters in the longest string that can be entered into the field, given the 
    * field's input restrictions.
    * @return The maximum number of characters that can be entered into this numeric text field.
    */
   public int getMaxCharsAllowed() { return(maxCharsNeeded); }
   
   /**
    * To provide a better estimates for the column width and preferred size of this numeric text string, two metrics are
    * lazily calculated and cached: the number of characters <i>N</i> and the length in pixels <i>L</i> of the longest
    * string that can be entered. <i>N</i> depends on the current input restrictions of the field, while <i>L</i> 
    * depends on <i>N</i> and the field's current font. To estimate <i>L</i>, the method forms a test string of length 
    * <i>N+1</i> of the form "0505...", then uses the font metrics of the field's current font to measure the total 
    * width of the test string.
    * 
    * <p>The metrics are recalculated only if they have been reset to zero, which happens whenever the input 
    * restrictions or the font changes.</p>
    */
   private void calcMetrics()
   {
      boolean includeMinus = false;
      boolean includeDot = false;
      if(maxCharsNeeded == 0)
      {
         if(isInteger)
         {
            maxCharsNeeded = Integer.toString(minValue.intValue()).length();
            maxCharsNeeded = Math.max(maxCharsNeeded, Integer.toString(maxValue.intValue()).length());
            includeMinus = (minValue.intValue() < 0);
         }
         else
         {
            includeDot = true;
            includeMinus = (minValue.doubleValue() < 0);
            
            // # sig digits + '-' (if negative values allowed) + 1 (for '.') + 4 (for exponent part "E-NN")
            maxCharsNeeded = maxSigDig + (includeMinus ? 6 : 5);
         }
         longestAdvance = 0;
      }

      if(longestAdvance == 0)
      {
         char[] biggestStr = new char[maxCharsNeeded];
         int iStart = 0;
         if(includeDot) biggestStr[iStart++] = '.';
         if(includeMinus) biggestStr[iStart++] = '-';
         for(int i = iStart; i < biggestStr.length; i++) biggestStr[i] = (i%2 == 0) ? '0' : '5';
         FontMetrics metrics = getFontMetrics(getFont());
         longestAdvance = metrics.charsWidth(biggestStr, 0, biggestStr.length);
         longestAdvance = longestAdvance * (2*maxCharsNeeded + 1) / (2*maxCharsNeeded);
      }
   }

   /** 
    * Resets the cached estimate of the length of the longest string that can be entered so that it will be 
    * recalculated for the new font.
    */
   @Override public void setFont(Font f) { longestAdvance = 0; super.setFont(f); }

   /**
    * This override offers an estimate of column width that is better suited to a text field restricted to numeric 
    * input. [The super implementation uses the width of the character 'm' to define column width.]
    */
   @Override public int getColumnWidth()
   {
      calcMetrics();
      return( (int) Math.round( ((double)longestAdvance) / ((double)maxCharsNeeded) ) );
   }

   /**
    * If the number of columns has <i><i>not</b></i> been set to restrict the preferred size of this numeric text field,
    * then this method will use the estimated longest advance possible -- given the restrictions on input to the field 
    * and its current font -- to calculate the preferred size.
    */
   @Override public Dimension getPreferredSize()
   {
      calcMetrics();
      Dimension size = super.getPreferredSize();
      if(getColumns() == 0)
      {
         Insets insets = getInsets();
         size.width = longestAdvance + insets.left + insets.right;
      }
      return(size);
   }

   /**  Overridden so that minimum size is no smaller than preferred size. */
   @Override public Dimension getMinimumSize() { return(getPreferredSize()); }
   
   /** Supported range for the maximum number of significant digits allowed for floating-point numeric input. */
   private static final int[] RANGE_SIGDIG = {1, 16};
   
   /** Supported range for the maximum number of fractional digits allowed for floating-point numeric input. */
   private static final int[] RANGE_FRACDIG = {1, 3};
   
   /** Does this numeric text field restrict input to integers only? */
   private boolean isInteger;

   /** The minimum allowed value. An instance of {@link Integer} or {@link Double}. */
   private Number minValue;

   /** The maximum allowed value. An instance of {@link Integer} or {@link Double}. */
   private Number maxValue;
   
   /** 
    * The maximum number of digits that can appear after the decimal point when in decimal notation -- applicable only
    * if floating-point input is allowed. If 0, then the number of fractional digits is not restricted.
    */
   private int maxFracDig;
   
   /** The maximum number of significant digits allowed, if floating-point input is allowed. */
   private int maxSigDig;

   /** 
    * The last valid value entered. If an invalid value is entered and cannot be parsed, contents are reset to this. 
    * It will be an instance of {@link Integer} or {@link Double}.
    */
   private Number lastValidValue;
}
