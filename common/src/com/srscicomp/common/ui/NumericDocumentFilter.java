package com.srscicomp.common.ui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * A document filter that restricts entry to either integer or floating-point numbers. In the latter case, the filter
 * supports entering numbers in scientific notation (e.g., "1.58E-5")  and restricts the total number of fractional 
 * digits (after the decimal point). In scientific notation, the decimal point must precede the "E", with at least one 
 * intervening digit ("-4.E" is not allowed), and the substring after the E must be a positive or negative integer. We 
 * don't restrict its value, except to verify that the numeric string is parsable as a Double.
 * 
 * @author sruffner
 */
class NumericDocumentFilter extends DocumentFilter
{
   /** The maximum number of characters that may be entered to specify a number. */
   private final int maxLength;
   /** True if negative numbers are permitted. */
   private final boolean allowNegative;
   /** True if floating-point numbers are permitted. */
   private final boolean allowReal;
   /** 
    * The maximum number of fractional digits (after the decimal point) that may be used for a floating-point number.
    * Ignored if filter is restricted to integral input.
    */
   private final int maxFracDig;

   /** The set of all possible allowed characters. We allow lower-case "e" but change it to upper-case. */
   private static final String ALLOWEDCHARS = "0123456789.-Ee";

   /**
    * Construct a document filter that restricts user input to valid integral or floating-point numbers.
    * @param maxLength The maximum number of characters that may be entered. This can be used to prevent the user from
    * entering too many digits. E.g., when restricting input to integers in [0..100], the user will never need to enter
    * more than 3 characters (note that the filter does not prevent entering a value like 101, which would be outside
    * the valid range).
    * @param allowNegative If true, negative numbers are allowed -- i.e., first character can be the minus sign.
    * @param allowReal If true, floating-point numbers are allowed; else, only integers.
    * @param maxFracDig When floating-point entry is permitted, this indicates the maximum number of fractional digits
    * (after the decimal point). Minimum of 1. Ignored if <i>allowReal</i> is false.
    */
   public NumericDocumentFilter(int maxLength, boolean allowNegative, boolean allowReal, int maxFracDig)
   {
      super();
      this.maxLength = (maxLength<1) ? 1 : maxLength;
      this.allowNegative = allowNegative;
      this.allowReal = allowReal;
      this.maxFracDig = (allowReal) ? ((maxFracDig < 1) ? 1 : maxFracDig) : 0;
   }

   @Override
   public void insertString(FilterBypass fb, int ofs, String s, AttributeSet attr) throws BadLocationException
   {
      replace(fb, ofs, 0, s, attr);
   }

   @Override
   public void replace(FilterBypass fb, int ofs, int length, String s, AttributeSet attrs) throws BadLocationException
   {
      // make sure result will not be too long
      if(s==null) s = "";
      int currDocLen = fb.getDocument().getLength();
      if(currDocLen - length + s.length() > maxLength)
         return;

      // an empty string is OK, but it is not parsable as a number
      if(currDocLen - length + s.length() == 0)
      {
         super.replace(fb, 0, currDocLen, "", attrs);
         return;
      }

      // make sure there are no invalid characters in the incoming string. This lets through "-.Ee", which can only 
      // appear once or twice at most (the '-' can appear at start of string and in exponent).
      char[] srcChars = s.toCharArray();
      for(char c : srcChars) if(ALLOWEDCHARS.indexOf(c) < 0)
         return;
      
      // construct resulting string
      String currText = fb.getDocument().getText(0, currDocLen);
      String res = currText.substring(0, ofs) + s + currText.substring(ofs + length);
      String origRes = res;
      
      // the exponent separator 'E' may appear only once, and only if floating-point numbers are allowed. We accept 'e',
      // but convert it to upper-case. It must be immediately preceded by a digit. Reject entries like "..E0", "..E-0". 
      // We must allow "..E" or "..E-" (user has just started to enter the exponent).
      res = res.replace('e', 'E');
      int firstEIndex = res.indexOf('E');
      if(firstEIndex >= 0)
      {
         if((!allowReal) || (firstEIndex == 0) || (res.lastIndexOf('E') != firstEIndex)) return;
         
         if(!Character.isDigit(res.charAt(firstEIndex-1))) return;
      
         String exp = res.substring(firstEIndex+1);
         if(exp.startsWith("0") || exp.startsWith("-0")) return;
         try 
         { 
            if(!(exp.isEmpty() || "-".equals(exp))) Integer.parseInt(exp); 
         } 
         catch(NumberFormatException nfe) { return; }
      }
      
      // a minus sign may appear in up to two places: it can be the first character if negative numbers are allowed,
      // and it can appear immediately after the 'E' if floating-point numbers are allowed.
      if(res.indexOf('-') == 0 && !allowNegative) return;
      int lastMinusIndex = res.lastIndexOf('-');
      if(lastMinusIndex > 0 && ((!allowReal) || (lastMinusIndex != firstEIndex+1))) return;

      // allow "-" by itself, even though it does not parse as a number (user has just started to enter a negative #).
      if(res.equals("-"))
      {
         super.replace(fb, 0, currDocLen, res, attrs);
         return;
      }

      // the decimal point may only appear once, and only if real numbers are allowed
      int firstDotIndex = res.indexOf('.');
      if(firstDotIndex >= 0 && ((!allowReal) || res.lastIndexOf('.') != firstDotIndex))
         return;

      // if the exponent separator and decimal point are both present, the decimal point must precede it
      if(firstEIndex > 0 && firstDotIndex > firstEIndex) return;
      
      // get rid of extra leading zeros: "000" -> "0", "-09" -> "-9", "00.03" -> ".03", etc. Note that this screws up 
      // correct input like "0.03" and "-0.6", but the next step fixes that. A little extra work, but simpler code.
      int len = res.length();
      boolean isNegative = res.startsWith("-");
      int firstNonzero = isNegative ? 1 : 0;
      while(firstNonzero < len)
      {
         if(res.charAt(firstNonzero) != '0') break;
         ++firstNonzero;
      }
      if(firstNonzero == len)
         res = (isNegative) ? "-0" : "0";
      else if(isNegative)
      {
         if(firstNonzero > 1) res = "-" + res.substring(firstNonzero);
      }
      else if(firstNonzero > 0)
         res = res.substring(firstNonzero);
         
      // for floating-pt entries: If string starts with "." or "-.", replace with "0." or "-0." respectively. Also, 
      // restrict the number of fractional digits entered.
      if(allowReal)
      {
         if(res.startsWith(".")) res = "0" + res;
         else if(res.startsWith("-.")) res = "-0" + res.substring(1);
         
         int dotIndex = res.indexOf('.');
         int eIndex = res.indexOf('E');
         if(dotIndex >= 0)
         {
            if(eIndex > 0)
            {
               int nFracDigits = eIndex - dotIndex - 1;
               if(nFracDigits > maxFracDig)
               {
                  String exp = res.substring(eIndex);
                  res = res.substring(0,eIndex - (nFracDigits-maxFracDig)) + exp;
               }
            }
            else
            {
               len = res.length();
               int nFracDigits = len - dotIndex - 1;
               if(nFracDigits > maxFracDig)
               {
                  res = res.substring(0, len - (nFracDigits-maxFracDig));
               }               
            }
         }
      }

      // make sure we can parse result as an integer or double, as appropriate. Have to be careful if user is in the
      // process of entering a number in scientific notation.
      try
      {
         if(!allowReal) Integer.parseInt(res);
         else
         {
            if(res.endsWith("E") || res.endsWith("E-")) 
               Double.parseDouble(res.substring(0, res.indexOf('E')));
            else
               Double.parseDouble(res);
         }
      }
      catch(NumberFormatException nfe)
      {
         return;
      }
 
      // if we made no corrections, just pass it as is -- preserving the current caret position. If not, replace the
      // entire text, which will put the caret after the last character.
      if(origRes.equals(res)) 
         super.replace(fb, ofs, length, s, attrs);
      else
         super.replace(fb, 0, currDocLen, res, attrs);
   }

   @Override public void remove(FilterBypass fb, int ofs, int length) throws BadLocationException
   {
      replace(fb, ofs, length, "", null);
   }

}
