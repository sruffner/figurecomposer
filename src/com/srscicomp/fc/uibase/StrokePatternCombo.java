package com.srscicomp.fc.uibase;

import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComboBoxEditor;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

import com.srscicomp.fc.fig.StrokePattern;

/**
 * <b>StrokePatternCombo</b> is a specialized combo box for setting/displaying the {@link StrokePattern} property of a
 * <i>FypML</i> graphic node. It has been customized in the following ways:
 * <ul>
 * <li>The preferred size is established by a default prototype value of "dashdotdot", one of the commonly used stroke 
 * patterns. If the current stroke pattern is longer than the prototype value, the component's tool tip is set to 
 * "Stroke Pattern: {current pattern}"; otherwise, the tip reads "Stroke Pattern". The prototype display value can be 
 * changed, but the tool tip behavior cannot be.</li>
 * <li>The drop down list displays the names of the common stroking patterns, {@link StrokePattern#COMMONPATTERNS}.</li>
 * <li>The combo box is editable, and it uses a custom text field editor. Input is restricted IAW the definition of a 
 * {@link StrokePattern}: a string of 1 or 2-digit whitespace-separated integers, each of which must lie in the range
 * [1..99]. Any entry that matches one of the five commonly used stroking patterns defined in will be replaced by the 
 * common name for that pattern. In fact, when the keyboard focus is on the text field, the user can quickly select one 
 * of the common patterns with a single key stroke: "s" for "solid", "d" for "dotted", "a" for "dashed", "t" for 
 * "dashdot", and "o" for "dashdotdot". Otherwise, to enter and validate a new stroke pattern, hit the "Enter" key. If 
 * the entered value is rejected, the previous value is restored.</li>
 * <li>Convenience methods are provided for getting and setting the stroke pattern currently entered in the combo box: 
 * {@link #getSelectedPattern()} and {@link #setSelectedPattern(StrokePattern)}.</li>
 * </ul>
 * 
 * @author 	sruffner
 */
@SuppressWarnings("serial")
public final class StrokePatternCombo extends JComboBox<StrokePattern>
{
   /** The default prototype display value, which governs preferred size of combo box. */
   private static final StrokePattern PROTOTYPE = StrokePattern.DASHDOTDOT;

   /**  Construct a <code>StrokePatternCombo</code> */
   public StrokePatternCombo()
   {
      super(StrokePattern.COMMONPATTERNS);
      super.setEditor(new PatternEditor());
      pattern = StrokePattern.SOLID;
      super.setSelectedItem(pattern);
      setPrototypeDisplayValue(PROTOTYPE);
      setToolTipText("Stroke Pattern");
      setMaximumRowCount(StrokePattern.COMMONPATTERNS.length);
      setEditable(true);     
      addActionListener(enterAction);
   }

   /** The currently selected stroke pattern in this <code>StrokePatternCombo</code>. */
   private StrokePattern pattern;

   /**
    * Get the stroke pattern currently reflected in this <code>StrokePatternCombo</code>.
    * 
    * @return The current stroke pattern.
    */
   public StrokePattern getSelectedPattern()
   { 
      StrokePattern p = (StrokePattern) super.getSelectedItem();
      if(p == null)
         super.setSelectedItem(pattern);
      else
         pattern = p;
      return(pattern);
   }

   /**
    * Set the stroke pattern reflected in this <code>StrokePatternCombo</code>.
    * 
    * @param p The new stroke pattern. A <code>null</code> value is assumed to correspond to a solid stroke.
    */
   public void setSelectedPattern(StrokePattern p)
   {
      pattern = (p != null) ? p : StrokePattern.SOLID;
      super.setSelectedItem(pattern);
      updateToolTip();
   }

   /**
    * Helper method sets tool tip to read "Stroke Pattern" or "Stroke Pattern: {current pattern}". The latter form is 
    * used when the current pattern (in string form) is longer than the prototype display value. If the prototype 
    * display value is <code>null</code> , it uses the default prototype value for <code>StrokePatternCombo</code>.
    */
   private void updateToolTip()
   {
      StrokePattern proto = getPrototypeDisplayValue();
      if(proto == null) proto = PROTOTYPE;

      if(pattern.toString().length() > proto.toString().length())
         setToolTipText("Stroke Pattern: " + pattern.toString());
      else 
         setToolTipText("Stroke Pattern");
   }

   /** Overridden to ensure that internal copy of currently selected item is up-to-date. */
   @Override public Object getSelectedItem() { return(getSelectedPattern()); }

   /** Overridden to validate selected item as an instance of <code>StrokePattern</code>. */
   @Override public void setSelectedItem(Object anObject) 
   {  
      if(anObject == null || anObject instanceof StrokePattern) setSelectedPattern((StrokePattern)anObject);
   }

   /** Overridden to protecte the combo box editor used. */
   @Override public void setEditor(ComboBoxEditor cbe) { if(cbe instanceof PatternEditor) super.setEditor(cbe); }


   /** Handler invoked whenever <strong>Enter</strong> key is pressed in a <code>StrokePatternCombo</code>. */
   private static final Action enterAction;

   /** Action which selects the common pattern <code>StrokePattern.SOLID</code>. */
   private static final Action selectSolid;

   /** Action which selects the common pattern <code>StrokePattern.DOTTED</code>. */
   private static final Action selectDotted;

   /** Action which selects the common pattern <code>StrokePattern.DASHED</code>. */
   private static final Action selectDashed;

   /** Action which selects the common pattern <code>StrokePattern.DASHDOT</code>. */
   private static final Action selectDashDot;

   /** Action which selects the common pattern <code>StrokePattern.DASHDOTDOT</code>. */
   private static final Action selectDashDotDot;

   static
   {
      enterAction = new UpdateHandler();
      selectSolid = new UpdateHandler();
      selectSolid.putValue(UpdateHandler.ACTION_COMMAND_KEY, StrokePattern.SOLID.toString());
      selectDotted = new UpdateHandler();
      selectDotted.putValue(UpdateHandler.ACTION_COMMAND_KEY, StrokePattern.DOTTED.toString());
      selectDashed = new UpdateHandler();
      selectDashed.putValue(UpdateHandler.ACTION_COMMAND_KEY, StrokePattern.DASHED.toString());
      selectDashDot = new UpdateHandler();
      selectDashDot.putValue(UpdateHandler.ACTION_COMMAND_KEY, StrokePattern.DASHDOT.toString());
      selectDashDotDot = new UpdateHandler();
      selectDashDotDot.putValue(UpdateHandler.ACTION_COMMAND_KEY, StrokePattern.DASHDOTDOT.toString());
   }

   /**
    * Helper class serves as an <code>ActionListener</code> for <code>StrokePatternCombo</code> itself, and as an 
    * <code>Action</code> object to implement key-stroke actions that change the current pattern to any one of the 
    * available common stroke patterns. In the latter role, it is placed in the input map of the embedded text 
    * component in the combo box's editor. It also updates the global input history whenever a new stroke pattern is 
    * selected or entered into a <code>StrokePatternCombo</code>. 
    * @author  sruffner
    */
   private static class UpdateHandler extends AbstractAction
   {
      private static final long serialVersionUID = 1L;

      public void actionPerformed(ActionEvent e)
      {
         Object src = e.getSource();
         if(src instanceof StrokePatternCombo)
         {
            StrokePatternCombo spc = (StrokePatternCombo)src;
            spc.updateToolTip();
         }
         else if(src instanceof JTextField)
         {
            Container c = ((JTextField)src).getParent();
            while(c != null && !(c instanceof StrokePatternCombo)) c = c.getParent();
            if(c != null)
            {
               StrokePatternCombo spc = (StrokePatternCombo) c;
               String cmd = e.getActionCommand();
               if(StrokePattern.isCommonPatternSynonym(cmd))
                  spc.setSelectedPattern(StrokePattern.fromString(cmd));
            }
         }
      }
   }

  
   /**
    * A custom <code>ComboBoxEditor</code> for <code>StrokePatternCombo</code>. The editor component is a 
    * <code>JTextField</code> in which a custom document filter has been installed to restrict input so that it is 
    * consistent with the definition string for a <code>StrokePattern</code>. Note that the filter will accept any 
    * operation such that the result string is empty (representing a temporary state) or can be parsed by the method 
    * <code>StrokePattern.fromString()</code>. Thus, the human-readable synonyms for the commonly used stroking 
    * patterns are acceptable as <em>result</em> strings. 
    * 
    * <p>Keystroke interface: The following keystroke shortcuts will change the current stroke pattern to one of the 
    * commonly used patterns:
    * <ul>
    *    <li>The 's' key: <code>StrokePattern.SOLID</code>.</li>
    *    <li>The 'd' key: <code>StrokePattern.DOTTED</code>.</li>
    *    <li>The 'a' key: <code>StrokePattern.DASHED</code>.</li>
    *    <li>The 't' key: <code>StrokePattern.DASHDOT</code>.</li>
    *    <li>The 'o' key: <code>StrokePattern.DASHDOTDOT</code>.</li>
    * </ul>
    * 
    * @author sruffner
    * @see StrokePattern#fromString(String)
    */
   private static class PatternEditor implements ComboBoxEditor
   {
      private final PatternField editor;
      private StrokePattern oldValue;
      
      PatternEditor()
      {
         editor =new PatternField();
         oldValue = StrokePattern.SOLID;
         editor.setText(oldValue.toString());
         installKeyBindings();
      }
      

      public Component getEditorComponent() { return(editor); }
      public void setItem(Object anObject)
      {
         if(anObject instanceof StrokePattern) 
         {
            oldValue = (StrokePattern) anObject;
            editor.setText(anObject.toString());
         }
         if(editor.hasFocus()) editor.selectAll(); 
      }
      public Object getItem()
      {
         StrokePattern p = StrokePattern.fromString(editor.getText());
         if(p == null) setItem(oldValue);
         else oldValue = p;
         return(p);
      }
      public void selectAll()
      {
         editor.selectAll();
         editor.requestFocusInWindow();
      }
      public void addActionListener(ActionListener l) { editor.addActionListener(l); }
      public void removeActionListener(ActionListener l) { editor.removeActionListener(l); }

      private void installKeyBindings()
      {
         InputMap im = editor.getInputMap();
         im.put(KeyStroke.getKeyStroke('s'), selectSolid);
         im.put(KeyStroke.getKeyStroke('d'), selectDotted);
         im.put(KeyStroke.getKeyStroke('a'), selectDashed);
         im.put(KeyStroke.getKeyStroke('t'), selectDashDot);
         im.put(KeyStroke.getKeyStroke('o'), selectDashDotDot);
      }

      static class PatternField extends JTextField
      {
         private static final long serialVersionUID = 1L;

         PatternField()
         {
            super(PROTOTYPE.toString());
            setPreferredSize(getPreferredSize());
            setText(StrokePattern.SOLID.toString());
            // super.setBorder(null);
            Document doc = getDocument();
            assert(doc instanceof AbstractDocument);
            ((AbstractDocument)doc).setDocumentFilter(new StrokePatternDocumentFilter());
         }
         
         // @Override public void setBorder(Border b) {}
      }

      static class StrokePatternDocumentFilter extends DocumentFilter
      {
         /** Maximum length of a <code>StrokePattern</code> definition string if it contains 6 2-digit integers. */
         private static final int MAXLEN = 17;
         
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
            if(currDocLen - length + src.length() > MAXLEN)
            {
               Toolkit.getDefaultToolkit().beep();
               return;
            }

            // an empty string is OK from the filter's standpoint
            if(currDocLen - length + src.length() == 0)
            {
               super.replace(fb, 0, currDocLen, "", attrs);
               return;
            }

            // construct resulting string. Whenever the current contents match the name of one of the common stroke 
            // patterns, the current contents are replaced entirely -- regardless of current selection!
            String s = fb.getDocument().getText(0, currDocLen);
            String res = (StrokePattern.isCommonPatternSynonym(s)) ? 
                  src : s.substring(0, offset) + src + s.substring(offset + length);

            // now try to construct a StrokePattern from the result. If we can't, then reject!
            if(StrokePattern.fromString(res) == null)
            {
               Toolkit.getDefaultToolkit().beep();
               return;
            }

            super.replace(fb, 0, currDocLen, res, attrs);
         }

         @Override
         public void remove(FilterBypass fb, int offset, int length) throws BadLocationException
         {
            replace(fb, offset, length, "", null);
         }
      }
   }
}
