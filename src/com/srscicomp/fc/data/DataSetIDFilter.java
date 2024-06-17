package com.srscicomp.fc.data;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;


/**
 * This document filter may be installed on a text field that displays/edits a {@link DataSet}'s ID string. Whenever the
 * field's content changes, this filter rejects the change if the string to be inserted contains any illegal characters.
 * Also, if the resulting string would be too long for a valid ID, the string to be inserted is truncated to ensure the 
 * resulting ID length is valid.
 * 
 * <p>[NOTE (sar, 29aug2014): Previous implementation simply replaced the entire string, even if the change was valid --
 * causing the caret in the associated text component to move to the end of the string. This is undesirable when you're
 * inserting characters in the middle of the current ID string, so the implementation was revised in an effort to
 * ensure the caret position was preserved.]</p>
 * 
 * @author sruffner
 */
public class DataSetIDFilter extends DocumentFilter
{
   @Override public void insertString(FilterBypass fb, int ofs, String s, AttributeSet as) throws BadLocationException
   {
      replace(fb, ofs, 0, s, as);
   }
   
   @Override public void replace(FilterBypass fb, int ofs, int n, String s, AttributeSet as) throws BadLocationException
   {
      int currDocLen = fb.getDocument().getLength();
      
      if(s.isEmpty())
      {
         // if replacement string is empty, then we're simply removing characters. This is OK as long as the result is 
         // not an empty string.
         if(n > 0 && n >= currDocLen) n = currDocLen - 1;
         fb.replace(ofs, n, s, as);
      }
      else if(!DataSet.isValidIDString(s))
      {
         // if replacement string is invalid by itself, it contains at least one bad character. So we don't allow any
         // of it -- leaving current ID unchanged.
         fb.replace(ofs, 0, "", as);
      }
      else
      {
         // the replacement string is OK. Now we just need to make sure that once it replaces 0 or more characters in
         // the current ID, the result is not too long. If it is, we truncate the replacement string.
         if(currDocLen - n + s.length() > DataSet.MAXIDLEN)
            s = s.substring(0, DataSet.MAXIDLEN - currDocLen + n);
         fb.replace(ofs, n, s, as);
      }
   }
   
   @Override public void remove(FilterBypass fb, int offset, int length) throws BadLocationException
   {
      replace(fb, offset, length, "", null);
   }
}
