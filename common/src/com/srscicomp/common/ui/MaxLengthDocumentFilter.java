package com.srscicomp.common.ui;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * A simple document filter that limits the number of characters in the text document and optionally restricts the
 * character content.
 * @author sruffner
 */
public class MaxLengthDocumentFilter extends DocumentFilter
{
   /** 
    * Construct a document filter that restricts the maximum length of the text document but not its character content.
    * @param len Maximum length of text document. If non-positive, 1 is assumed.
    */
   public MaxLengthDocumentFilter(int len) { this(len, null); }

   /** 
    * Construct a document filter that restricts both the maximum length of the text document and its character content.
    * @param len Maximum length of text document. If non-positive, 1 is assumed.
    * @param allowed The set of characters allowed. If null or empty string, then all characters are allowed.
    */
   public MaxLengthDocumentFilter(int len, String allowed)
   {
      super();
      maxLength = Math.max(len, 1);
      allowedChars = (allowed == null || allowed.isEmpty()) ? null : allowed;
   }

   @Override
   public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException
   {
      replace(fb, offset, 0, string, attr);
   }

   @Override
   public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException
   {
      // make sure result will not be too long
      String src = (text==null) ? "" : text;
      int currDocLen = fb.getDocument().getLength();
      if(currDocLen - length + src.length() > maxLength)
         return;

      // an empty string is OK 
      if(currDocLen - length + src.length() == 0)
      {
         super.replace(fb, 0, currDocLen, "", attrs);
         return;
      }

      // if character content restricted, make sure there are no invalid characters in the incoming string. 
      if(allowedChars != null)
      {
         for(int i=0; i<src.length(); i++) if(allowedChars.indexOf(src.charAt(i)) < 0)
            return;
      }
      
      // test(s) passed, so defer to base class
      super.replace(fb, offset, length, text, attrs);
   }

   @Override
   public void remove(FilterBypass fb, int offset, int length) throws BadLocationException
   {
      replace(fb, offset, length, "", null);
   }

   /** The maximum number of characters allowed by this document filter. */
   private final int maxLength;
   /** The set of characters passed by this document filter. If null, there are no character restrictions. */
   private final String allowedChars;


}
