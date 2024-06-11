package com.srscicomp.common.ui;


import java.awt.Color;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;

/**
 * <b>BkgFillPicker</code> is a custom-painted {@link JButton} that lets the user select a solid-color or gradient fill 
 * style, as encapsulated by {@link BkgFill}.
 * 
 * <p>The "picker" is a flat rectangular button, the dimensions of which may be specified in the constructor (with a 
 * minimum size of 36x36). It has a thin, gray rounded border when it does not have the keyboard focus; when it has the
 * focus or the mouse is hovering over it, the border is steel blue and twice the thickness. A rectangular region inside
 * this border is filled IAW the background fill currently associated with the component. A gray-and-white checkerboard
 * background is used to help depict translucent solid-color fills -- see the base class {@link SwatchButton} for 
 * details; gradient fills, by definition, are always opaque.</p>
 * 
 * <p>To change the background fill, the user simply clicks on the component, bringing up a pop-up window (an "always on
 * top" mode-less dialog). On this window are several controls to let the user re-define the background fill:
 * <ul>
 * <li>A combo box selects the fill type -- solid, axial gradient, radial gradient.</li>
 * <li>Two {@link RGBColorPicker} buttons select the color stops associated with a gradient fill, or the single color 
 * for a solid fill.</li>
 * <li>A numeric text field specifies the orientation angle of the axial gradient WRT the bounding box of the object
 * to be painted (integer, [0..359] degrees.</li>
 * <li>Two numeric text fields set the relative X- and Y-coordinates of the focal point of the radial gradient, as
 * a percentage of the graphic object's bounding box width and height, respectively (integer, [0..100]).</li>
 * </ul>
 * Only those widgets applicable to the currently chosen fill type are enabled or visible in the window. To the right
 * of the widgets is a 100x100 "preview swatch" that is filled with the background fill as it is currently defined -- 
 * just as the face of the picker button itself is painted. This swatch is updated as the user interacts with the 
 * controls in the pop-up window. Clicking outside the window or on the preview swatch confirms any changes made and 
 * extinguishes the pop-up. (The pop-up, implemented by an inner class singleton, is a static resource shared by all 
 * <b>BkgFillPicker</b> instances in the same VM.)</p>
 * 
 * <p>The user can view the picker component's current fill by hovering the mouse over the component until its tool tip 
 * appears. The tool tip has the form "tip: desc", where "desc" is supplied by {@link BkgFill#getDescription()}. The 
 * "tip" prefix is the default tool tip text set by calling {@link #setText(String)}; this lets a UI designer describe
 * a role for the particular fill style represented by the picker component. Initially this tip text is an empty string.
 * <b>Note</b> that calling setToolTipText() with a null argument will turn off the tool tip, which is enabled when the
 * picker button is constructed.</p>
 * 
 * <p>To listen for changes in the background fill picker button's current fill, register as a {@link 
 * PropertyChangeListener} for the property tag {@link #BKGFILL_PROPERTY}.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
public class BkgFillPicker extends SwatchButton implements ActionListener
{
   /** 
    * Create the background fill picker button at its minimum size (34x34 pixels), which includes a 6-pixel margin. It 
    * initially represents a solid fill in opaque black.
    */
   public BkgFillPicker() { this(28, 28); }

   /**
    * Construct the background fill picker button at the size specified, plus a 6-pixel margin. It initially represents
    * a solid fill in opaque black.
    * @param w Fixed width of the swatch rectangle in the button's interior, in pixels. Auto-corrected to a multiple of
    * 4 in the range [16..100].
    * @param h Fixed height of the swatch rectangle in the button's interior, in pixels. Auto-corrected to a multiple of
    * 4 in the range [16..100].
    */
   public BkgFillPicker(int w, int h)
   {
      super(w, h, 6);
      setToolTipText("");
      addActionListener(this);
   }

   /**
    * Cancel and extinguish the pop-up editor panel used to change the fill associated with this background fill picker
    * button. The method has no effect if the singleton shared pop-up panel is already hidden, or if it is currently 
    * visible <b>but was not invoked by this background fill picker</b>.
    */
   public void cancelPopup() { PopupPanel.cancel(this); }

   /**
    * Registered property change listeners are notified of changes in this property whenever the value selected by this
    * background fill picker is modified via the pop-up panel.
    */
   public final static String BKGFILL_PROPERTY = "bkgFill";

   /** 
    * Get the current fill represented by this background fill picker.
    * @return The current background fill.
    */
   public BkgFill getCurrentFill() { return(getBkgFill()); }
   
   /**
    * Set the fill currently represented by this background fill picker.
    * @param bf The new background fill. If null, a solid black fill is assumed.
    * @param notify If a change is made and this flag is set, any property change listeners registered on the {@link 
    * #BKGFILL_PROPERTY} will be notified. Otherwise, no notification is sent.
    */
   public void setCurrentFill(BkgFill bf, boolean notify)
   {
      BkgFill oldFill = getCurrentFill();
      if(oldFill.equals(bf)) return;
      setBkgFill( (bf != null) ? bf : BkgFill.createSolidFill(Color.BLACK));
      if(notify) firePropertyChange(BKGFILL_PROPERTY, oldFill, getCurrentFill());
   }
   
   /** Overridden to generate a tool tip that describes the fill represented by this background fill picker. */
   @Override public String getToolTipText(MouseEvent event) 
   { 
      String s = super.getToolTipText();
      if(s != null && !s.isEmpty()) s += ": ";
      else s = "";
      return(s + getCurrentFill().getDescription()); 
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      if(e.getSource() == this)
      {
         requestFocusInWindow();
         PopupPanel.popup(this);
      }
   }

   /**
    * The custom singleton panel that encapsulates and manages the contents of the background fill picker pop-up window.
    * When the panel is raised, it is updated to reflect the current background fill of the invoking button and then
    * installed in a mode-less dialog. When that dialog is extinguished, the content panel is removed from the dialog 
    * before the dialog is disposed -- so the content panel may be reused. The invoking background fill picker is 
    * updated IAW whatever background fill the user selected in the pop-up panel. Obviously, only one pop-up dialog may 
    * exist at a time, and the pop-up panel is intended to serve all instances of <b>BkgFillPicker</b> in the 
    * application.
    * 
    * @author sruffner
    */
   private static class PopupPanel extends JPanel 
         implements ActionListener, WindowFocusListener, PropertyChangeListener
   {
      /**
       * Raise an editor panel in a modeless "pop-up" dialog to let user define a different background fill for the 
       * invoking background fill picker button. The editor panel is an application singleton, shared by all instances
       * of <b>BkgFillPicker</b>. If it is currently in use, this method has no effect.
       * @param bfPicker The background fill picker button that invoked the pop-up.
       */
      static void popup(BkgFillPicker bfPicker)
      {
         // editor panel is lazily created
         if(editor == null) editor = new PopupPanel();
         
         // abort if pop-up in use or invoker unspecified
         if(bfPicker == null || editor.getParent() != null) return;
         
         invoker = bfPicker;
         editor.initialize(bfPicker.getCurrentFill());

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
         dlg.addWindowFocusListener(editor);
         dlg.add(editor);
         dlg.pack();
         
         // determine where dialog should appear, then show it
         Point pUL = invoker.getLocationOnScreen();
         GUIUtilities.adjustULToFitScreen(pUL, dlg.getPreferredSize());
         dlg.setLocation(pUL);
         dlg.setVisible(true);
      }
      
      /**
       * Extinguish the background fill picker pop-up editor <i>without</i> updating the background fill assigned to the
       * invoking picker button. This provides a way to cancel selection of a new background fill.
       * @param requestor The background fill picker button that requested cancellation. The request is honored only if
       * it comes from the button that raised the pop-up editor in the first place.
       */
      static void cancel(BkgFillPicker requestor)
      {
         if(editor == null || editor.getParent() == null || invoker == null || invoker != requestor) return;
         
         // make sure color picker pop-up is cancelled, if necessary
         editor.c1Picker.cancelPopup();
         editor.c2Picker.cancelPopup();
         
         JDialog dlg = (JDialog) editor.getTopLevelAncestor();
         dlg.setVisible(false);
         dlg.remove(editor);
         dlg.removeWindowFocusListener(editor);
         dlg.dispose();
         invoker = null;
      }
      
      /**
       * Extinguish the background fill pop-up editor window, remove the singleton editor panel from it, and finally
       * update the invoking background fill picker button with the new background fill selected.
       * @param cancel If true, the operation is cancelled: the pop-up is hidden, but no change is made to the invoking
       * background fill picker.
       */
      private static void extinguish(boolean cancel)
      {
         if(editor.getParent() == null) return;
         
         // make sure color picker pop-up is cancelled, if necessary
         editor.c1Picker.cancelPopup();
         editor.c2Picker.cancelPopup();
         
         JDialog dlg = (JDialog) editor.getTopLevelAncestor();
         dlg.setVisible(false);
         dlg.remove(editor);
         dlg.removeWindowFocusListener(editor);
         dlg.dispose();
         
         if(invoker != null)
         {
            if(!cancel) invoker.setCurrentFill(editor.previewSwatch.getBkgFill(), true);
            invoker = null;
         }
      }
      
      /** The singleton pop-up editor panel by which a background fill is defined and edited. */
      private static PopupPanel editor = null;
      /** The background fill picker button that raised the pop-up. Null when pop-up is not in use. */
      private static BkgFillPicker invoker = null;

      /**
       * The pop-up editor panel is extinguished if its dialog container loses the focus, and the invoking background
       * fill picker is left unchanged (operation cancelled). However, if the focus is lost because the user has clicked
       * one of the color picker buttons in order to edit a color, no action is taken.
       */
      @Override public void windowLostFocus(WindowEvent e) 
      { 
         if(!(c1Picker.isEditing() || c2Picker.isEditing())) extinguish(true); 
      }
      
      @Override public void windowGainedFocus(WindowEvent e) {}

      @Override public void actionPerformed(ActionEvent e) 
      { 
         Object src = e.getSource();
         if(src==typeCombo || src==angleField || src==fxField || src==fyField) onEdit(); 
         else if(e.getSource() == previewSwatch) extinguish(false);
      }
      
      @Override public void propertyChange(PropertyChangeEvent e) 
      { 
         if(e.getPropertyName().equals(RGBColorPicker.COLOR_PROPERTY))
            onEdit(); 
         else if(e.getPropertyName().equals(RGBColorPicker.POPUPVISIBLE_PROP) && e.getNewValue().equals(Boolean.FALSE))
         {
            // we need to check where the keyboard focus lies after a color picker's pop-up is extinguished. If the
            // focus window is not the pop-up container for this editor panel, then extinguish the panel and make no
            // change to the invoking BkgFillPicker button. NOTE that we have to queue this task on the dispatch thread 
            // to ensure the focus changes have been processed.
            SwingUtilities.invokeLater(() -> {
               Window w = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
               if(w == null || w != PopupPanel.this.getTopLevelAncestor())
                  extinguish(true);
            });
         }
      }

      private void onEdit()
      {
         BkgFill bf;
         BkgFill.Type type = (BkgFill.Type) typeCombo.getSelectedItem();
         if(type == BkgFill.Type.SOLID)
            bf = BkgFill.createSolidFill(c1Picker.getCurrentColor());
         else
         {
            // when switching from a solid to a gradient fill, make sure the two color stops aren't the same. Also make
            // sure the first color stop is opaque (it may not be if the previous fill type was solid)
            Color c1 = c1Picker.getCurrentColor();
            if(c1.getAlpha() < 255)
            {
               c1 = new Color(c1.getRed(), c1.getGreen(), c1.getBlue());
               c1Picker.setCurrentColor(c1, false);
            }
            Color c2 = c2Picker.getCurrentColor();
            if(c2.equals(c1))
            {
               c2 = c1.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
               c2Picker.setCurrentColor(c2, false);
            }
            if(type == BkgFill.Type.AXIAL)
               bf = BkgFill.createAxialGradientFill(angleField.getValue().intValue(), c1, c2);
            else
               bf = BkgFill.createRadialGradientFill(fxField.getValue().intValue(), fyField.getValue().intValue(), 
                     c1, c2);
         }
         previewSwatch.setBkgFill(bf);
         refresh();
      }
      
      void initialize(BkgFill bf)
      {
         previewSwatch.setBkgFill(bf);
         refresh();
         
         // because JComboBox.setSelectedItem will fire an action event...
         typeCombo.removeActionListener(this);
         typeCombo.setSelectedItem(bf.getFillType());
         typeCombo.addActionListener(this);
         
         c1Picker.setCurrentColor(bf.getColor1(), false);
         c2Picker.setCurrentColor(bf.getColor2(), false);
         angleField.setValue(bf.getOrientation());
         fxField.setValue(bf.getFocusX());
         fyField.setValue(bf.getFocusY());
      }
      
      private void refresh()
      {
         BkgFill bf = previewSwatch.getBkgFill();
         boolean isGrad = (bf.getFillType() != BkgFill.Type.SOLID);
         boolean isLin = (bf.getFillType() == BkgFill.Type.AXIAL);
         
         // when the fill type is solid, this color picker must allow for a translucent color.
         c1Picker.setEnableAlpha(!isGrad);
         
         c1Picker.setToolTipText(isGrad ? (isLin ? C1_TIPLINEAR : C1_TIPRADIAL) : C1_TIPSOLID);
         c2Picker.setVisible(isGrad);
         c2Picker.setToolTipText(isGrad ? (isLin ? C2_TIPLINEAR : C2_TIPRADIAL) : null);
         angleField.setEnabled(isLin);
         angleField.setToolTipText(isLin ? ANGLE_TIP : null);
         fxField.setEnabled(isGrad && !isLin);
         fxField.setToolTipText(isGrad && !isLin ? FX_TIP : null);
         fyField.setEnabled(isGrad && !isLin);
         fyField.setToolTipText(isGrad && !isLin ? FY_TIP : null);
      }
      
      /** Private constructor for the singleton. */
      private PopupPanel()
      {
         previewSwatch = new SwatchButton(100, 100, 5);
         previewSwatch.setFocusable(false);
         previewSwatch.addActionListener(this);
         add(previewSwatch);
         
         typeCombo = new JComboBox<>(BkgFill.Type.values());
         typeCombo.setToolTipText("Select the background fill type");
         typeCombo.addActionListener(this);
         add(typeCombo);
         
         JLabel colorLabel = new JLabel("Colors: ");
         add(colorLabel);
         c1Picker = new RGBColorPicker(true);
         c1Picker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
         c1Picker.addPropertyChangeListener(RGBColorPicker.POPUPVISIBLE_PROP, this);
         add(c1Picker);
         c2Picker = new RGBColorPicker(false);
         c2Picker.addPropertyChangeListener(RGBColorPicker.COLOR_PROPERTY, this);
         c2Picker.addPropertyChangeListener(RGBColorPicker.POPUPVISIBLE_PROP, this);
         add(c2Picker);
         
         JLabel angleLabel = new JLabel("Angle (deg): ");
         add(angleLabel);
         angleField = new NumericTextField(0, 359);
         angleField.setValue(0);
         angleField.addActionListener(this);
         add(angleField);
         
         JLabel fxLabel = new JLabel("Focus X (%): ");
         add(fxLabel);
         fxField = new NumericTextField(0, 100);
         fxField.setValue(50);
         fxField.addActionListener(this);
         add(fxField);
         
         JLabel fyLabel = new JLabel("Focus Y (%): ");
         add(fyLabel);
         fyField = new NumericTextField(0, 100);
         fyField.setValue(50);
         fyField.addActionListener(this);
         add(fyField);
         
         SpringLayout layout = new SpringLayout();
         setLayout(layout);
         
         layout.putConstraint(SpringLayout.NORTH, typeCombo, 10, SpringLayout.NORTH, this);
         layout.putConstraint(SpringLayout.NORTH, c1Picker, 4, SpringLayout.SOUTH, typeCombo);
         layout.putConstraint(SpringLayout.NORTH, angleField, 4, SpringLayout.SOUTH, c1Picker);
         layout.putConstraint(SpringLayout.NORTH, fxField, 4, SpringLayout.SOUTH, angleField);
         layout.putConstraint(SpringLayout.NORTH, fyField, 4, SpringLayout.SOUTH, fxField);
         layout.putConstraint(SpringLayout.SOUTH, this, 10, SpringLayout.SOUTH, fyField);
         
         String vc = SpringLayout.VERTICAL_CENTER;
         layout.putConstraint(vc, colorLabel, 0, vc, c1Picker);
         layout.putConstraint(vc, c2Picker, 0, vc, c1Picker);
         layout.putConstraint(vc, angleLabel, 0, vc, angleField);
         layout.putConstraint(vc, fxLabel, 0, vc, fxField);
         layout.putConstraint(vc, fyLabel, 0, vc, fyField);
         layout.putConstraint(vc, previewSwatch, 0, vc, this);
         
         layout.putConstraint(SpringLayout.WEST, angleLabel, 10, SpringLayout.WEST, this);
         layout.putConstraint(SpringLayout.WEST, angleField, 0, SpringLayout.EAST, angleLabel);
         layout.putConstraint(SpringLayout.WEST, previewSwatch, 10, SpringLayout.EAST, angleField);
         layout.putConstraint(SpringLayout.EAST, this, 10, SpringLayout.EAST, previewSwatch);
         
         layout.putConstraint(SpringLayout.WEST, typeCombo, 0, SpringLayout.WEST, angleLabel);
         layout.putConstraint(SpringLayout.EAST, typeCombo, 0, SpringLayout.EAST, angleField);
         layout.putConstraint(SpringLayout.WEST, colorLabel, 0, SpringLayout.WEST, angleLabel);
         layout.putConstraint(SpringLayout.WEST, c1Picker, 5, SpringLayout.EAST, colorLabel);
         layout.putConstraint(SpringLayout.WEST, c2Picker, 5, SpringLayout.EAST, c1Picker);
         layout.putConstraint(SpringLayout.EAST, fxLabel, 0, SpringLayout.EAST, angleLabel);
         layout.putConstraint(SpringLayout.WEST, fxField, 0, SpringLayout.WEST, angleField);
         layout.putConstraint(SpringLayout.EAST, fyLabel, 0, SpringLayout.EAST, angleLabel);
         layout.putConstraint(SpringLayout.WEST, fyField, 0, SpringLayout.WEST, angleField);
         
         setBorder(BorderFactory.createRaisedBevelBorder());

         // key bindings that extinguish pop-up: ESCape (change cancelled), Shift-ENTER (change confirmed). We can't use
         // Enter alone because it is bound to the text fields and the combo box.
         getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
         getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("shift ENTER"), "done");
         
         getActionMap().put("cancel", new ExtinguishAction(true));
         getActionMap().put("done", new ExtinguishAction(false));
      }
      
      /** Helper action used to bind keys that extinguish the pop-up, either confirming or canceling the change. */
      private static class ExtinguishAction extends AbstractAction
      {
         private final boolean cancel;
         ExtinguishAction(boolean cancel) { this.cancel = cancel; }
         @Override public void actionPerformed(ActionEvent e) { PopupPanel.extinguish(cancel); }
      }
      
      /** Preview of the currently defined background fill. */
      private final SwatchButton previewSwatch;
      /** Combo box selects the fill type for the background fill. */
      private JComboBox<BkgFill.Type> typeCombo = null;
      /** Color picker specifying the color for a solid fill, or the first color for a gradient fill. */
      private RGBColorPicker c1Picker = null;
      /** Color picker specifying the second color for a gradient fill. */
      private RGBColorPicker c2Picker = null;
      /** The orientation angle for a linear gradient, in integer degrees limited to [0..360). */
      private NumericTextField angleField = null;
      /** X-coordinate of focus for a radial gradient, as an integer percentage of bounding box width. */
      private NumericTextField fxField = null;
      /** Y-coordinate of focus for a radial gradient, as an integer percentage of bounding box width. */
      private NumericTextField fyField = null;
      
      private final static String C1_TIPSOLID = "Select the solid fill color";
      private final static String C1_TIPLINEAR = "Select first color stop";
      private final static String C1_TIPRADIAL = "Select color at focus";
      private final static String C2_TIPLINEAR = "Select second color stop";
      private final static String C2_TIPRADIAL = "Select color at perimeter";
      private final static String ANGLE_TIP = "Specify gradient orientation WRT bounding box, in degrees [0..359]";
      private final static String FX_TIP = "Specify X-coordinate of focus as percentage of bounding box width";
      private final static String FY_TIP = "Specify Y-coordinate of focus as percentage of bounding box height";
   }
}
