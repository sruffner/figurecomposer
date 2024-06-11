package com.srscicomp.common.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.color.ColorSpace;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * <b>RGBColorPicker</b> is a custom-painted {@link JButton} that implements an RGB color chooser. It supports selecting
 * colors from the default sRGB color space, and it can be optionally configured to control the color's "alpha" 
 * component as well, allowing the user to select a translucent or fully transparent color.
 * 
 * <p>The color button itself is rendered as a flat button with a rounded rectangular outline. Its dimensions are fixed
 * fixed but may be specified at construction time. The button's overall size is (W+2M)x(H+2M), including a WxM "swatch"
 * and a boundary margin of M pixels. The rectangular swatch is painted to reflect the color currently associated with
 * the color picker button, while the margin area is reserved for painting the button's border. The border changes to 
 * reflect the button's focus, disabled, and rollover state. For details, see base class {@link SwatchButton}.</p>
 * 
 * <p>When the current color is opaque (alpha == 255), the rectangular swatch is filled with that color. If alpha < 255,
 * a gray-and-white checkerboard is drawn instead, and then the bottom-right half (below the diagonal) is filled with 
 * the translucent color, while the top-left half is filled with its opaque version. However, if alpha == 0 (fully 
 * transparent), a red "X" is drawn over the checkerboard instead.</p>
 * 
 * <p>To change the color, the user simply clicks on the button to raise a compact color chooser panel. At the top of 
 * the panel are "swatches" painted with the five most recently used colors (in the same manner as described above, and 
 * in the middle of the panel is a 5x5 array of swatches painted with 25 commonly used colors. Clicking on any swatch 
 * selects that color and extinguishes the pop-up panel. Alternatively, the lower portion of the panel includes three 
 * integer-valued spinner controls to manually set the R, G, and B components of the color chosen. Either use the 
 * spinner buttons or enter a value -- restricted to [0..255] -- manually. A fourth spinner sets the alpha component of 
 * the color; this spinner is disabled if the <b>RGBColorPicker</b> instance is restricted to opaque colors only.</p>
 * 
 * <p>A single larger swatch next to the spinners is painted with whatever color is represented by the R/G/B triplet; 
 * it is initially set to the current color of the invoking color picker button when the color chooser appears. Clicking
 * on the larger swatch, or clicking anywhere outside the pop-up panel, selects that color and hides the pop-up.</p>
 * 
 * <p>The color chooser pop-up panel, implemented by an inner class singleton, is a static resource shared by all 
 * <b>RGBColorPicker</b> instances in the same VM.</p>
 * 
 * <p>The user can view the RGB color button's current color as an RGB triplet by hovering the mouse over the button 
 * until its tool tip appears. The tool tip has the form "tip: common" or "tip: (r, g, b)", where "common" is a common 
 * name for the color, if it is one of the 25 colors made available in the color chooser panel. When the color is not 
 * opaque, the tool tip includes the alpha value: "tip: (r, g, b); alpha = xxx". All color components are reported as
 * integers ranging from 0 to 255. The "tip" prefix is the default tool tip set by {@link #setToolTipText(String)};
 * this lets a UI designer describe a role for the particular color represented by the color picker button (in lieu of 
 * putting a static text label next to it). Initially this tip text is an empty string. <b>Note</b> that calling {@link 
 * #setToolTipText(String)} with a null argument will turn off the tool tip, which is enabled when the color button is
 * constructed.</p>
 * 
 * <p>To listen for changes in the color picker button's current color, register as a {@link 
 * java.beans.PropertyChangeListener} for the property tag <b>COLOR_PROPERTY</b>.</p>
 *
 * @author sruffner
 */
public class RGBColorPicker extends SwatchButton implements ActionListener
{
   /**
    * Create an instance of the RGB color picker button with a default swatch size of 16x16 pixels and a margin of 3,
    * resulting in overall dimensions of 22x22. Its initial color is set to opaque black, its tool tip is initially 
    * enabled, and it is configured to support translucent colors (alpha < 255).
    */
   public RGBColorPicker() { this(true); }

   /**
    * Create an instance of the RGB color picker button with a default swatch size of 16x16 pixels and a margin of 3, 
    * resulting in overall dimensions of 22x22. Its initial color is set to opaque black, and its tool tip is initially 
    * enabled.
    * 
    * @param enaAlpha If this flag is set, the color picker allows control of the color's "alpha" component. If not, 
    * only opaque RGB colors may be chosen.
    */
   public RGBColorPicker(boolean enaAlpha) { this(16, 16, 3, enaAlpha); }
   
   /**
    * Create an instance of the RGB color picker button with the specified swatch size WxH and a default margin of 3 
    * pixels. The overall dimensions of the button will be (W+6) x (H+6). Its initial color is set to opaque black, and
    * its tool tip is initially enabled.
    * @param sz Dimensions (fixed) of the rectangular swatch area within the button rectangle. Both dimensions are
    * auto-corrected as needed to ensure each is a multiple of 4 in [16..100]. If null, 16x16 is assumed.
    * @param enaAlpha If this flag is set, the color picker allows control of the color's "alpha" component. If not, 
    * only opaque RGB colors may be chosen.
    */
   public RGBColorPicker(Dimension sz, boolean enaAlpha)
   {
      this(sz==null ? 16 : sz.width, sz==null ? 16 : sz.height, 3, enaAlpha);
   }
   
   /**
    * Create an instance of the RGB color picker button with the specified swatch dimensions WxH and margin size M. The
    * overall dimensions of the button will be (W+2M) x (H+2M). Its initial color is set to opaque black, and its tool 
    * tip is initially enabled.
    * 
    * @param w The (fixed) width of the rectangular swatch area within the button rectangle. It is auto-corrected as
    * needed to ensure it is a multiple of 4 in [16..100].
    * @param h The (fixed) height of the rectangular swatch area within the button rectangle. It is auto-corrected as
    * needed to ensure it is a multiple of 4 in [16..100].
    * @param m The size of the margin that surrounds the interior "swatch" in which the current color is depicted. It is
    * range-restricted to [3..12].
    * @param enaAlpha If this flag is set, the color picker allows control of the color's "alpha" component. If not, 
    * only opaque RGB colors may be chosen.
    */
   public RGBColorPicker(int w, int h, int m, boolean enaAlpha)
   {
      super(w, h, m);
      
      this.enaAlpha = enaAlpha;
      setBkgFill(Color.BLACK);
      setToolTipText("");
      addActionListener(this);
   }

   /**
    * Get the current color selected by this RGB color picker.
    * @return The current color. 
    */
   public Color getCurrentColor() { return(getBkgFill().getColor1()); }

   /**
    * Set the current color associated with this color picker button.
    * 
    * @param c The new color. If it is null, it is set to opaque black. If it is not defined on the default sRGB 
    * color space, it will be adjusted accordingly. If it is not opaque and the color picker is configured only to 
    * select opaque colors, then its alpha component is set to 255 (fully opaque). 
    * @param notify If a change is made and this flag is set, any property change listeners registered on the {@link 
    * #COLOR_PROPERTY} property will be notified. Otherwise, no notification is sent.
    */
   public void setCurrentColor(Color c, boolean notify)
   {
      Color oldColor = getCurrentColor();
      if(oldColor.equals(c)) return;

      if(c == null) 
         c = Color.BLACK;
      else
      {
         int a = c.getAlpha();
         if((a != 255 && !enaAlpha) || !c.getColorSpace().equals(ColorSpace.getInstance(ColorSpace.CS_sRGB)))
            c = new Color(c.getRed(), c.getGreen(), c.getBlue());
      }
      if(c.equals(oldColor)) return;
      
      setBkgFill(c);
      if(notify) firePropertyChange(COLOR_PROPERTY, oldColor, c);
   }

   /**
    * Enable or disable editing of the current color's alpha component. 
    * @param ena True if the color picker should support changing the color's alpha component; else, only opaque colors 
    * may be chosen.
    */
   public void setEnableAlpha(boolean ena) { enaAlpha = ena; }
   
   /** 
    * Overridden to generate a tool tip of the form "prefix: color", where "prefix" is the static tool tip text 
    * returned by {@link #getToolTipText()}, and color is a common color name or the color specification in the form
    * "(r, g, b)". If the color is not opaque, the alpha component is also included: "(r, g, b); alpha = ...". All
    * color components are integers in [0..255].
    */
   @Override public String getToolTipText(MouseEvent event)
   {
      String s = super.getToolTipText();
      if(s != null && !s.isEmpty()) s += ": ";
      else s = "";
      
      return(s + getColorString(getCurrentColor()));
   }

   
   @Override public void actionPerformed(ActionEvent e)
   {
      if(e.getSource() == this) 
      {
         requestFocusInWindow();
         RGBPopupPanel.popup(this);
      }
   }
   
   /**
    * Is RGB color associated with this color picker currently being edited in a pop-up "color chooser" panel?
    * @return True if editing in progress.
    */
   public boolean isEditing() { return(RGBPopupPanel.isRaisedBy(this)); }
   
   /**
    * Cancel and extinguish the pop-up chooser panel used to select the color associated with this RGB color picker. The
    * method has no effect if the singleton shared pop-up panel is already hidden, or if is currently visible <b>but was
    * not invoked by this particular color picker component</b>.
    */
   public void cancelPopup() { RGBPopupPanel.cancel(this); }
   
   /** If set, color picker can set the color's alpha component; else, opaque colors only. */
   private boolean enaAlpha;
   
   /**
    * A list of commonly used RGB colors to which are assigned descriptive names. They are listed here in the order 
    * they appear in the color chooser's swatch panel.
    */
   private static final List<Color> BASIC_COLORS = new ArrayList<>(25);

   /** Maps the commonly used RGB colors to their descriptive names ("black", "white", etc.). All are opaque. */
   private static final Map<Color, String> COLOR2NAME = new HashMap<>();
   static
   {
      Color c;
      c = new Color(0, 0, 0); BASIC_COLORS.add(c); COLOR2NAME.put(c, "black");
      c = new Color(64, 64, 64); BASIC_COLORS.add(c); COLOR2NAME.put(c, "dark gray");
      c = new Color(128, 128, 128); BASIC_COLORS.add(c); COLOR2NAME.put(c, "gray");
      c = new Color(211, 211, 211); BASIC_COLORS.add(c); COLOR2NAME.put(c, "light gray");
      c = new Color(255, 255, 255); BASIC_COLORS.add(c); COLOR2NAME.put(c, "white");
      c = new Color(139, 0, 0); BASIC_COLORS.add(c); COLOR2NAME.put(c, "dark red");
      c = new Color(255, 0, 0); BASIC_COLORS.add(c); COLOR2NAME.put(c, "red");
      c = new Color(255, 69, 0); BASIC_COLORS.add(c); COLOR2NAME.put(c, "orange red");
      c = new Color(255, 165, 0); BASIC_COLORS.add(c); COLOR2NAME.put(c, "orange");
      c = new Color(255, 215, 0); BASIC_COLORS.add(c); COLOR2NAME.put(c, "gold");
      c = new Color(255, 255, 0); BASIC_COLORS.add(c); COLOR2NAME.put(c, "yellow");
      c = new Color(128, 128, 0); BASIC_COLORS.add(c); COLOR2NAME.put(c, "olive");
      c = new Color(154, 205, 50); BASIC_COLORS.add(c); COLOR2NAME.put(c, "yellow green");
      c = new Color(0, 128, 0); BASIC_COLORS.add(c); COLOR2NAME.put(c, "green");
      c = new Color(0, 255, 0); BASIC_COLORS.add(c); COLOR2NAME.put(c, "lime");
      c = new Color(0, 255, 127); BASIC_COLORS.add(c); COLOR2NAME.put(c, "spring green");
      c = new Color(0, 255, 255); BASIC_COLORS.add(c); COLOR2NAME.put(c, "cyan");
      c = new Color(70, 130, 180); BASIC_COLORS.add(c); COLOR2NAME.put(c, "steel blue");
      c = new Color(65, 105, 225); BASIC_COLORS.add(c); COLOR2NAME.put(c, "royal blue");
      c = new Color(0, 0, 255); BASIC_COLORS.add(c); COLOR2NAME.put(c, "blue");
      c = new Color(0, 0, 128); BASIC_COLORS.add(c); COLOR2NAME.put(c, "navy");
      c = new Color(106, 90, 205); BASIC_COLORS.add(c); COLOR2NAME.put(c, "slate blue");
      c = new Color(153, 50, 204); BASIC_COLORS.add(c); COLOR2NAME.put(c, "dark orchid");
      c = new Color(255, 0, 255); BASIC_COLORS.add(c); COLOR2NAME.put(c, "magenta");
      c = new Color(128, 0, 128); BASIC_COLORS.add(c); COLOR2NAME.put(c, "purple");
   }

   /**
    * Generates a short string of the form "(r, g, b)" to describe the specified color. If a simple color name is
    * available for the color, that name is returned instead. If the color is not opaque, the alpha component is also
    * included: "(r, g, b); alpha = ...". All color components are represented as integers in [0..255].
    * 
    * @param c A color.
    * @return A short string describing the color, as described. If the argument is null, an empty string is returned.
    */
   public static String getColorString(Color c)
   {
      String desc = "";
      if(c != null)
      {
         String name = COLOR2NAME.get(c);
         if(name != null) desc = name;
         else if(c.getAlpha() == 255)
            desc = String.format("rgb(%d, %d, %d)", c.getRed(), c.getGreen(), c.getBlue());
         else 
            desc = String.format("rgba(%d, %d, %d, %d)", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha());
      }
      return(desc);
   }

   /**
    * Registered property change listeners are notified of changes in this property whenever the current color attached
    * to an {@link RGBColorPicker} instance is modified via the pop-up color chooser window.
    */
   public final static String COLOR_PROPERTY = "color";

   /**
    * The RGB color picker button that invokes the pop-up color chooser window will send this property change 
    * notification when the chooser window is raised and lowered.
    */
   public final static String POPUPVISIBLE_PROP = "popupvis";
   

   /**
    * A simple extension of {@link SwatchButton} representing a small square swatch painted a uniform, possibly 
    * translucent, RGB color. It is used in the color chooser pop-up panel.
    * 
    * @author sruffner
    */
   private static class RGBSwatch extends SwatchButton
   {
      RGBSwatch(Color c, int size)
      {
         super(size, size, 3);
         setBkgFill(c);
         setToolTipText("");   // to enable tooltips
      }
      @Override public String getToolTipText(MouseEvent event) { return(getColorString(getCurrentColor())); }
      
      Color getCurrentColor() { return(getBkgFill().getColor1()); }
      void setCurrentColor(Color c) { setBkgFill(c); }
   }

   /**
    * The custom singleton panel that encapsulates and manages the contents of a "color chooser" pop-up window. When the 
    * panel is raised, it is updated to reflect the current color of the invoking {@link RGBColorPicker}, and then it is 
    * installed in a mode-less dialog. When that dialog is extinguished, the content panel is removed from the dialog 
    * before the dialog is disposed -- so the content panel may be reused. The invoking color picker is updated IAW 
    * whatever color the user selected in the pop-up panel. Obviously, only one pop-up dialog may exist at a time, and 
    * the pop-up panel is intended to serve all instances of {@link RGBColorPicker} in the application.
    * 
    * @author sruffner
    */
   private static class RGBPopupPanel extends JPanel 
         implements ActionListener, ChangeListener, WindowFocusListener, FocusListener
   {
      /** 
       * Raise the color chooser panel in a mode-less dialog to let user select a new color for the invoking RGB
       * color picker button. If the singleton pop-up panel is currently in use, the method has no effect.
       * @param rgbPicker The particular RGB color picker button that invoked the pop-up.
       */
      static void popup(RGBColorPicker rgbPicker)
      {
         // popup's content panel is lazily created
         if(popupContent == null) popupContent = new RGBPopupPanel();
         
         // if invoking color picker not specified or if singleton content panel is currently in use, ignore
         if(rgbPicker == null || popupContent.getParent() != null)
            return;
         
         invoker = rgbPicker;
         popupContent.setCurrentColor(invoker.getCurrentColor());
         popupContent.alphaSpinner.setEnabled(invoker.enaAlpha);
         
         // create the modeless dialog container and insert pop-up panel into it
         JDialog dlg;
         Container owner = invoker.getTopLevelAncestor();
         if(owner instanceof Window) dlg = new JDialog((Window) owner, "", Dialog.ModalityType.MODELESS);
         else
         {
            invoker = null;
            return;
         }
         dlg.setUndecorated(true);
         dlg.setAlwaysOnTop(true);
         dlg.addWindowFocusListener(popupContent);
         dlg.add(popupContent);
         dlg.pack();
         
         // NOTE: When the popup first appears, we need to ensure the initial focus is on the red spinner and NOT on
         // any swatch. So we request focus on the spinner's editor field just prior to raising the popup. If we fail
         // to do this, the focus will go to the first MRU swatch, and that will cause the current color to change.
         popupContent.redSpinner.getEditor().requestFocusInWindow();
         
         // determine where dialog should appear, then show it
         Point pUL = invoker.getLocationOnScreen();
         GUIUtilities.adjustULToFitScreen(pUL, dlg.getPreferredSize());
         dlg.setLocation(pUL);
         dlg.setVisible(true);
      }

      /**
       * Is the singleton pop-up window currently raised to edit the RGB color associated with the specified color
       * picker button?
       * @param rgbPicker The color picker button.
       * @return True if pop-up is raised AND the specified color picker is the instance that invoked the pop-up.
       */
      static boolean isRaisedBy(RGBColorPicker rgbPicker)
      {
         return(popupContent != null && popupContent.getParent() != null && invoker != null && rgbPicker == invoker);
      }
      
      /**
       * Extinguish the color picker pop-up window <i>without</i> updating the color in the invoking color picker.
       * This provides a way to cancel the selection of a color via the pop-up window.
       * @param requestor The color picker button requesting cancellation. The request is honored only if it comes from 
       * the color picker that raised the pop-up window in the first place!
       */
      static void cancel(RGBColorPicker requestor)
      {
         if(popupContent == null || popupContent.getParent() == null || invoker == null || invoker != requestor) return;
         
         JDialog dlg = (JDialog) popupContent.getTopLevelAncestor();
         dlg.setVisible(false);
         dlg.remove(popupContent);
         dlg.removeWindowFocusListener(popupContent);
         dlg.dispose();
         invoker.firePropertyChange(POPUPVISIBLE_PROP, true, false);
         invoker = null;
      }

      /**
       * Extinguish the color picker pop-up window, remove the singleton pop-up content panel from it, and finally 
       * update the invoking RGB color picker with the color selected (or, optionally, cancel any change).
       * @param swatchSelected An RGB swatch representing the color selected in the pop-up panel. If null, no change
       * is made in the invoking color picker.
       */
      private static void extinguish(RGBSwatch swatchSelected)
      {
         if(popupContent.getParent() == null) return;
         
         JDialog dlg = (JDialog) popupContent.getTopLevelAncestor();
         dlg.setVisible(false);
         dlg.remove(popupContent);
         dlg.removeWindowFocusListener(popupContent);
         dlg.dispose();
         
         if(invoker != null)
         {
            if(swatchSelected != null)
            {
               Color c = swatchSelected.getCurrentColor();
               popupContent.postMRUColor(c);
               invoker.setCurrentColor(c, true);
            }
            
            invoker.firePropertyChange(POPUPVISIBLE_PROP, true, false); 
            invoker = null;
         }
      }
      
      
      /** Private constructor for singleton. */
      private RGBPopupPanel()
      {
         setBorder(BorderFactory.createRaisedBevelBorder());
         setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
         add(createMRUSwatchPanel());
         add(new JSeparator());
         add(createCommonSwatchPanel());
         add(new JSeparator());
         add(createEditColorPanel());
         
         // so user can cancel pop-up simply by hitting the ESCape key, or confirm via Shift+Enter (Enter is trapped
         // by the text fields on the panel).
         getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
         getActionMap().put("cancel", new ExtinguishAction(true));
         getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift ENTER"), "done");
         getActionMap().put("done", new ExtinguishAction(false));
      }
      
      /** Helper action used to bind keys that extinguish the pop-up, either confirming or canceling the change. */
      private static class ExtinguishAction extends AbstractAction
      {
         private final boolean cancel;
         ExtinguishAction(boolean cancel) { this.cancel = cancel; }
         @Override public void actionPerformed(ActionEvent e) 
         { 
            RGBPopupPanel.extinguish(cancel ? null : RGBPopupPanel.popupContent.currColorSwatch); 
         }
      }

      /** Size of the square color swatches in the first two sections of the pop-up chooser panel. */
      private final static int SWATCH_SZ = 20;
 
      /** Space between adjacent swatches (horizontally and vertically)*/
      private final static int SWATCH_GAP = 0;

      /** Number of color swatches per row in the first two sections. */
      private final static int SWATCHES_PER_ROW = 5;

      /** The singleton pop-up color chooser panel. */
      private static RGBPopupPanel popupContent = null;
      
      /** The color picker button that raised the pop-up color chooser panel. Null when pop-up panel is not in use. */
      private static RGBColorPicker invoker = null;
      
      /** The most recently used swatches (globally across the application). */
      private static RGBSwatch[] mruSwatches;
      
      /** Creates a row of {@link RGBSwatch} objects representing the most recently picked colors. */
      private JPanel createMRUSwatchPanel()
      {
         mruSwatches = new RGBSwatch[SWATCHES_PER_ROW];
         mruSwatches[0] = new RGBSwatch(Color.BLACK, SWATCH_SZ);
         mruSwatches[1] = new RGBSwatch(Color.WHITE, SWATCH_SZ);
         mruSwatches[2] = new RGBSwatch(Color.GRAY, SWATCH_SZ);
         mruSwatches[3] = new RGBSwatch(Color.RED, SWATCH_SZ);
         mruSwatches[4] = new RGBSwatch(Color.BLUE, SWATCH_SZ);

         JPanel p = new JPanel();
         p.setLayout(new GridLayout(1, mruSwatches.length, SWATCH_GAP, SWATCH_GAP));
         for(RGBSwatch s : mruSwatches)
         {
            s.addActionListener(this);
            s.addFocusListener(this);
            p.add(s);
         }
         p.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
         return(p);
      }

      /** 
       * Helper method updates the set of MRU swatches on the singleton pop-up color chooser panel. If the specified 
       * color is already represented in a MRU swatch, that swatch is moved to the front of the list. Otherwise, the 
       * last swatch on the list is dropped, and a new swatch in the given color is placed at the front.
       * @param c A recently picked color.
       */
      private void postMRUColor(Color c)
      {
         if(c == null) return;

         // if color already represented in the MRU swatches, get the index of the representative swatch
         int found = -1;
         for(int i = 0; i<mruSwatches.length; i++) if(c.equals(mruSwatches[i].getCurrentColor()))
         {
            found = i;
            break;
         }

         // adjust the colors of the MRU swatches to reflect the new MRU color. If the color was already in the MRU 
         // swatch list, it is pushed to the front of the list.
         Color mruC = (found<0) ? c : mruSwatches[found].getCurrentColor();
         if(found < 0) found = mruSwatches.length-1;
         if(found > 0) 
         {
            for(int i = found - 1; i >= 0; i--)
               mruSwatches[i+1].setCurrentColor(mruSwatches[i].getCurrentColor());
            mruSwatches[0].setCurrentColor(mruC);
         }
      }

      /** Creates a panel with an array of swatches representing a set of commonly used colors. */
      private JPanel createCommonSwatchPanel()
      {
         int n = BASIC_COLORS.size();
         JPanel p = new JPanel();
         p.setLayout(new GridLayout(n/SWATCHES_PER_ROW, SWATCHES_PER_ROW, SWATCH_GAP, SWATCH_GAP));
         for(int i=0; i<n; i++)
         {
            RGBSwatch swatch = new RGBSwatch(BASIC_COLORS.get(i), SWATCH_SZ);
            swatch.addActionListener(this);
            swatch.addFocusListener(this);
            p.add(swatch);
         }
         p.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
         return(p);
      }

      /** A spinner-numeric field control for specifying the red component of the chosen color. */
      private IntegerSpinner redSpinner = null;
      /** A spinner-numeric field control for specifying the green component of the chosen color. */
      private IntegerSpinner grnSpinner = null;
      /** A spinner-numeric field control for specifying the blue component of the chosen color. */
      private IntegerSpinner bluSpinner = null;
      /** Swatch representing the currently chosen color. Updated as user changes the individual components. */
      private RGBSwatch currColorSwatch = null;
      /** A spinner-numeric field control for specifying the alpha component of the chosen color. */
      private IntegerSpinner alphaSpinner = null;
      
      /** 
       * Helper method creates a panel with four integer spinner controls for manually specifying the individual 
       * components of the desired RGB color, including its alpha component. The specified color is displayed in a large
       * swatch adjacent to the spinners. If the invoking color picker does not permit control of the alpha component,
       * the corresponding spinner control is disabled.
       */
      private JPanel createEditColorPanel()
      {
         redSpinner = new IntegerSpinner(0, 0, 255, 5);
         redSpinner.addChangeListener(this);
         grnSpinner = new IntegerSpinner(0, 0, 255, 5);
         grnSpinner.addChangeListener(this);
         bluSpinner = new IntegerSpinner(0, 0, 255, 5);
         bluSpinner.addChangeListener(this);
         
         alphaSpinner = new IntegerSpinner(0, 0, 255, 5);
         alphaSpinner.addChangeListener(this);
         
         currColorSwatch = new RGBSwatch(Color.BLACK, 3*SWATCH_SZ);
         currColorSwatch.setToolTipText(null);
         currColorSwatch.addActionListener(this);

         Dimension hSpace2 = new Dimension(2,0);
         Dimension vSpace2 = new Dimension(0,2);

         JPanel swatchPanel = new JPanel();
         swatchPanel.setLayout(new BoxLayout(swatchPanel, BoxLayout.PAGE_AXIS));
         swatchPanel.add(Box.createVerticalGlue());
         swatchPanel.add(currColorSwatch);
         swatchPanel.add(Box.createVerticalGlue());

         JPanel redGrp = new JPanel(new BorderLayout());
         redGrp.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 3, Color.RED));
         redGrp.add(redSpinner, BorderLayout.CENTER);

         JPanel grnGrp = new JPanel(new BorderLayout());
         grnGrp.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 3, Color.GREEN));
         grnGrp.add(grnSpinner, BorderLayout.CENTER);

         JPanel bluGrp = new JPanel(new BorderLayout());
         bluGrp.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 3, Color.BLUE));
         bluGrp.add(bluSpinner, BorderLayout.CENTER);

         JPanel spinnerGrp = new JPanel();
         spinnerGrp.setLayout(new BoxLayout(spinnerGrp, BoxLayout.PAGE_AXIS));
         spinnerGrp.add(redGrp);
         spinnerGrp.add(Box.createRigidArea(vSpace2));
         spinnerGrp.add(grnGrp);
         spinnerGrp.add(Box.createRigidArea(vSpace2));
         spinnerGrp.add(bluGrp);

         JPanel alphaPanel = new JPanel(new BorderLayout());
         alphaPanel.setBorder(BorderFactory.createCompoundBorder(
               BorderFactory.createEmptyBorder(2, 0, 0, 0), 
               BorderFactory.createMatteBorder(0, 0, 0, 3, Color.WHITE)));
         alphaPanel.add(new JLabel("Alpha: ", JLabel.TRAILING), BorderLayout.CENTER);
         alphaPanel.add(alphaSpinner, BorderLayout.EAST);
         
         JPanel contentPane = new JPanel(new BorderLayout());
         contentPane.add(swatchPanel, BorderLayout.WEST);
         contentPane.add(Box.createRigidArea(hSpace2), BorderLayout.CENTER);
         contentPane.add(spinnerGrp, BorderLayout.EAST);
         contentPane.add(alphaPanel, BorderLayout.SOUTH);
         contentPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
         return(contentPane);
      }

      /** Flag set whenever state changes in the spinners should be ignored. */
      private transient boolean ignoreSpinners = false;
      
      /**
       * Set the integer spinner controls and the current color swatch to reflect the color specified. If the alpha
       * spinner is disabled, the color's alpha component is ignored; in this case, the color chooser panel is only
       * set-up to select an opaque color.
       * @param c The new current color.
       */
      private void setCurrentColor(Color c)
      {
         if(c == null) return;
         ignoreSpinners = true;
         
         boolean ignoreAlpha = !alphaSpinner.isEnabled();
         
         redSpinner.setValue(c.getRed());
         grnSpinner.setValue(c.getGreen());
         bluSpinner.setValue(c.getBlue());
         if(!ignoreAlpha) alphaSpinner.setValue(c.getAlpha());
         
         currColorSwatch.setCurrentColor((ignoreAlpha && c.getAlpha() < 255) ? 
               new Color(c.getRed(), c.getGreen(), c.getBlue()) : c);
         
         ignoreSpinners = false;
      }
      
      
      //
      // ActionListener, ChangeListener, WindowFocusListener
      //
      
      /** 
       * When the user clicks on any swatch button in the pop-up window, the invoking RGB color picker is set to the 
       * corresponding color and the pop-up is extinguished.
       */
      @Override public void actionPerformed(ActionEvent e)
      {
         Object src = e.getSource();
         if(src instanceof RGBSwatch) extinguish((RGBSwatch) src);
      }
      
      /** Whenever a spinner changes value, the large swatch is updated accordingly. */
      public void stateChanged(ChangeEvent e)
      {
         if(ignoreSpinners) return;
         
         int r = redSpinner.getIntValue();
         int g = grnSpinner.getIntValue();
         int b = bluSpinner.getIntValue();
         int a = (alphaSpinner.isEnabled()) ? alphaSpinner.getIntValue() : 255;
         currColorSwatch.setCurrentColor(new Color(r, g, b, a));
      }

      /**
       * The pop-up color chooser panel is extinguished if its dialog container loses the focus, and the invoking RGB 
       * color picker is set to the color of the large swatch in the bottom section of the chooser panel. 
       */
      public void windowLostFocus(WindowEvent e) { extinguish(currColorSwatch); }
      /**
       * When the pop-up color chooser panel is raised, we put the keyboard focus on the edit field that displays the 
       * red color component. Putting the focus on a component in the pop-up ensures the ESCape key binding for 
       * canceling the pop-up will work.
       */
      public void windowGainedFocus(WindowEvent e) 
      { 
         redSpinner.getEditor().requestFocusInWindow(); 
         if(invoker != null) invoker.firePropertyChange(POPUPVISIBLE_PROP, false, true);
      }

      /** 
       * When one of the MRU or common color swatches gains the focus, make the color of that swatch the current
       * color "selected" by the pop-up panel. See {@link #setCurrentColor(Color)}.
       */
      @Override public void focusGained(FocusEvent e)
      {
         Object src = e.getSource();
         if(src instanceof RGBSwatch) setCurrentColor(((RGBSwatch)src).getCurrentColor());
      }
      @Override public void focusLost(FocusEvent e) {}
   }

}
