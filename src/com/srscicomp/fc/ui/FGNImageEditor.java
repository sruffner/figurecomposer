package com.srscicomp.fc.ui;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SpringLayout;

import com.srscicomp.common.ui.BkgFillPicker;
import com.srscicomp.common.ui.NumericTextField;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.ImageNode;
import com.srscicomp.fc.fig.Measure;
import com.srscicomp.fc.uibase.FCChooser;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.MeasureEditor;

/**
 * <code>FGNImageEditor</code> displays and edits all properties of a {@link ImageNode ImageNode}.
 * It includes widgets to specify the (x,y) coords of the BL corner of the image's bounding box; the bounding box width,
 * height, and orientation (a rotation about the BL corner); and an inner margin or gap, the same on all sides, between 
 * the edges of the bounding box and the corresponding edges of the rectangular image. A color picker button selects the
 * background fill color for the bounding box, and an embedded {@link DrawStyleEditor} handles the inheritable 
 * draw-related style properties that define how the bounding box is stroked.
 * 
 * <p>The image itself cannot be edited, but a different source image can be chosen by clicking the "Load Image" button 
 * to browse the file system for the desired image file; supported formats are JPEG or PNG. Four integer-valued text
 * fields specify the top-left corner and dimensions of the cropping rectangle in pixel units. Input is restricted to 
 * ensure that the rectangle specified is completely contained within the original source image's rectangle. A "Reset"
 * button lets the user clear the cropping rectangle. If no image is defined, the cropping widgets are disabled.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class FGNImageEditor extends FGNEditor implements ActionListener, PropertyChangeListener
{
   /** Construct the text box node properties editor. */
   FGNImageEditor()
   {
      super();
      
      loadBtn = new JButton("Load image...");
      loadBtn.addActionListener(this);
      add(loadBtn);
      
      dimLabel = new JLabel("<no image>");
      add(dimLabel);
      
      JLabel cropLabel = new JLabel("Crop:");
      add(cropLabel);
      
      cropXField = new NumericTextField(0, 9999);
      cropXField.setToolTipText("X-coordinate of top-left corner of cropping rectangle in image pixels");
      cropXField.addActionListener(this);
      add(cropXField);

      cropYField = new NumericTextField(0, 9999);
      cropYField.setToolTipText("Y-coordinate of top-left corner of cropping rectangle in image pixels");
      cropYField.addActionListener(this);
      add(cropYField);

      cropWField = new NumericTextField(1, 9999);
      cropWField.setToolTipText("Width of cropping rectangle in image pixels");
      cropWField.addActionListener(this);
      add(cropWField);

      cropHField = new NumericTextField(1, 9999);
      cropHField.setToolTipText("Height of cropping rectangle in image pixels");
      cropHField.addActionListener(this);
      add(cropHField);

      cropResetBtn = new JButton("Reset");
      cropResetBtn.setToolTipText("Press this button to restore the original, uncropped source image");
      cropResetBtn.addActionListener(this);
      add(cropResetBtn);
      
      JLabel xLabel = new JLabel("X= ");
      add(xLabel);
      Measure.Constraints c = FGraphicModel.getLocationConstraints(FGNodeType.IMAGE);
      xEditor = new MeasureEditor(6, c);
      xEditor.setToolTipText("X-coordinate of bottom-left corner of bounding box");
      xEditor.addActionListener(this);
      add(xEditor);
      
      JLabel yLabel = new JLabel("Y= ");
      add(yLabel);
      yEditor = new MeasureEditor(6, c);
      yEditor.setToolTipText("Y-coordinate of bottom-left corner of bounding box");
      yEditor.addActionListener(this);
      add(yEditor);
      
      JLabel rotLabel = new JLabel("\u03b8= ");
      add(rotLabel);
      rotateField = new NumericTextField(-180.0, 180.0, 2);
      rotateField.setToolTipText("Orientation (rotation about bottom-left corner)");
      rotateField.addActionListener(this);
      add(rotateField);
      
      JLabel wLabel = new JLabel("W= ");
      add(wLabel);
      c = FGraphicModel.getSizeConstraints(FGNodeType.IMAGE);
      wEditor = new MeasureEditor(6, c);
      wEditor.setToolTipText("Width of bounding box");
      wEditor.addActionListener(this);
      add(wEditor);
      
      JLabel hLabel = new JLabel("H= ");
      add(hLabel);
      hEditor = new MeasureEditor(6, c);
      hEditor.setToolTipText("Height of bounding box");
      hEditor.addActionListener(this);
      add(hEditor);
      
      JLabel mLabel = new JLabel("M= ");
      add(mLabel);
      c = FGraphicNode.STROKEWCONSTRAINTS;
      marginEditor = new MeasureEditor(6, c);
      marginEditor.setToolTipText("Inner margin (0-1in)");
      marginEditor.addActionListener(this);
      add(marginEditor);
      
      bkgFillPicker = new BkgFillPicker(50, 50);
      bkgFillPicker.setToolTipText("Background fill");
      bkgFillPicker.addPropertyChangeListener(BkgFillPicker.BKGFILL_PROPERTY, this);
      add(bkgFillPicker);
      
      drawStyleEditor = new DrawStyleEditor(true, false);
      add(drawStyleEditor);

      SpringLayout layout = new SpringLayout();
      setLayout(layout);

      // left-right constraints.
      layout.putConstraint(SpringLayout.WEST, loadBtn, 0, SpringLayout.WEST, wLabel);
      layout.putConstraint(SpringLayout.WEST, dimLabel, GAP, SpringLayout.EAST, loadBtn);
      layout.putConstraint(SpringLayout.EAST, dimLabel, 0, SpringLayout.EAST, this);

      layout.putConstraint(SpringLayout.WEST, cropLabel, 0, SpringLayout.WEST, wLabel);
      layout.putConstraint(SpringLayout.WEST, cropXField, GAP, SpringLayout.EAST, cropLabel);
      layout.putConstraint(SpringLayout.WEST, cropYField, GAP, SpringLayout.EAST, cropXField);
      layout.putConstraint(SpringLayout.WEST, cropWField, GAP, SpringLayout.EAST, cropYField);
      layout.putConstraint(SpringLayout.WEST, cropHField, GAP, SpringLayout.EAST, cropWField);
      layout.putConstraint(SpringLayout.WEST, cropResetBtn, GAP, SpringLayout.EAST, cropHField);

      layout.putConstraint(SpringLayout.EAST, xLabel, 0, SpringLayout.EAST, wLabel);
      layout.putConstraint(SpringLayout.WEST, xEditor, 0, SpringLayout.WEST, wEditor);
      layout.putConstraint(SpringLayout.EAST, yLabel, 0, SpringLayout.EAST, hLabel);
      layout.putConstraint(SpringLayout.WEST, yEditor, 0, SpringLayout.WEST, hEditor);
      layout.putConstraint(SpringLayout.EAST, rotLabel, 0, SpringLayout.EAST, mLabel);
      layout.putConstraint(SpringLayout.WEST, rotateField, 0, SpringLayout.WEST, marginEditor);
      layout.putConstraint(SpringLayout.EAST, rotateField, 0, SpringLayout.EAST, marginEditor);

      // this is the anchor row (the longest row, establishing width of the container)
      layout.putConstraint(SpringLayout.WEST, wLabel, 0, SpringLayout.WEST, this);
      layout.putConstraint(SpringLayout.WEST, wEditor, 0, SpringLayout.EAST, wLabel);
      layout.putConstraint(SpringLayout.WEST, hLabel, GAP, SpringLayout.EAST, wEditor);
      layout.putConstraint(SpringLayout.WEST, hEditor, 0, SpringLayout.EAST, hLabel);
      layout.putConstraint(SpringLayout.WEST, mLabel, GAP*3, SpringLayout.EAST, hEditor);
      layout.putConstraint(SpringLayout.WEST, marginEditor, 0, SpringLayout.EAST, mLabel);
      layout.putConstraint(SpringLayout.WEST, bkgFillPicker, GAP, SpringLayout.EAST, marginEditor);
      layout.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, bkgFillPicker);
      
      layout.putConstraint(SpringLayout.WEST, drawStyleEditor, 0, SpringLayout.WEST, wLabel);

      // top-bottom constraints: 5 rows, with draw style editor in last row. In each of first 4 rows, one widget is used
      // to set the constraints with row above or below. The remaining widgets are vertically centered WRT that widget. 
      layout.putConstraint(SpringLayout.NORTH, loadBtn, 0, SpringLayout.NORTH, this);
      layout.putConstraint(SpringLayout.NORTH, cropResetBtn, GAP, SpringLayout.SOUTH, loadBtn);
      layout.putConstraint(SpringLayout.NORTH, rotateField, GAP*3, SpringLayout.SOUTH, cropResetBtn);
      layout.putConstraint(SpringLayout.NORTH, wEditor, GAP, SpringLayout.SOUTH, rotateField);
      layout.putConstraint(SpringLayout.NORTH, drawStyleEditor, GAP*2, SpringLayout.SOUTH, wEditor);
      layout.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, drawStyleEditor);
      
      String vCtr = SpringLayout.VERTICAL_CENTER;
      layout.putConstraint(vCtr, dimLabel, 0, vCtr, loadBtn);

      layout.putConstraint(vCtr, cropLabel, 0, vCtr, cropResetBtn);
      layout.putConstraint(vCtr, cropXField, 0, vCtr, cropResetBtn);
      layout.putConstraint(vCtr, cropYField, 0, vCtr, cropResetBtn);
      layout.putConstraint(vCtr, cropWField, 0, vCtr, cropResetBtn);
      layout.putConstraint(vCtr, cropHField, 0, vCtr, cropResetBtn);

      layout.putConstraint(vCtr, xLabel, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, xEditor, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, yLabel, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, yEditor, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, rotLabel, 0, vCtr, rotateField);
      layout.putConstraint(vCtr, bkgFillPicker, 0, SpringLayout.SOUTH, rotateField);
      
      layout.putConstraint(vCtr, wLabel, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, hLabel, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, hEditor, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, mLabel, 0, vCtr, wEditor);
      layout.putConstraint(vCtr, marginEditor, 0, vCtr, wEditor);
   }

   @Override public void reload(boolean initial)
   {
      ImageNode img = getImageNode();
      
      xEditor.setMeasure(img.getX());
      yEditor.setMeasure(img.getY());
      rotateField.setValue(img.getRotate());
      wEditor.setMeasure(img.getWidth());
      hEditor.setMeasure(img.getHeight());
      marginEditor.setMeasure(img.getMargin());
      bkgFillPicker.setCurrentFill(img.getBackgroundFill(), false);
      
      drawStyleEditor.loadGraphicNode(img);
      updateDimensionsAndCrop();
   }

   /**
    * Update the text of the label reflecting the pixel dimensions of the current image, and the text fields defining
    * the current crop rectangle. If no image is defined, then the label reads "<no image>" and the crop rectangle
    * widgets are disabled.
    */
   private void updateDimensionsAndCrop()
   {
      ImageNode img = getImageNode();
      BufferedImage bi = (img == null) ? null : img.getImage();
      Rectangle rCrop = (bi == null) ? null : img.getCrop();
      
      if(bi == null) dimLabel.setText("<no image>");
      else dimLabel.setText("" + bi.getWidth() + " x " + bi.getHeight());
      
      cropXField.setValue(rCrop==null ? 0 : rCrop.x);
      cropYField.setValue(rCrop==null ? 0 : rCrop.y);
      cropWField.setValue(rCrop==null ? (bi==null ? 1 : bi.getWidth()) : rCrop.width);
      cropHField.setValue(rCrop==null ? (bi==null ? 1 : bi.getHeight()) : rCrop.height);
      
      cropXField.setEnabled(bi != null);
      cropYField.setEnabled(bi != null);
      cropWField.setEnabled(bi != null);
      cropHField.setEnabled(bi != null);
      cropResetBtn.setEnabled(bi != null && rCrop != null);
   }
   
   /** 
    * Ensure any modeless pop-up windows associated with any editor widgets are extinguished before the editor panel is
    * hidden. 
    */
   @Override void onLowered()
   {
      drawStyleEditor.cancelEditing();
      bkgFillPicker.cancelPopup();
   }

   @Override boolean isEditorForNode(FGraphicNode n) { return(n != null && FGNodeType.IMAGE == n.getNodeType()); }
   @Override public ImageIcon getRepresentativeIcon() { return(FCIcons.V4_IMAGE_32); }
   @Override String getRepresentativeTitle() { return("Image Properties"); }

   @Override public void propertyChange(PropertyChangeEvent e)
   {
      ImageNode img = getImageNode();
      if(img != null && e.getSource() == bkgFillPicker)
         img.setBackgroundFill(bkgFillPicker.getCurrentFill());
   }

   @Override public void actionPerformed(ActionEvent e)
   {
      ImageNode img = getImageNode();
      if(img == null) return;
      Object src = e.getSource();

      if(src == loadBtn)
      {
         FCChooser chooser = FCChooser.getInstance();
         BufferedImage bi = chooser.loadImage(this, lastImgFile);
         if(bi != null)
         {
            lastImgFile = chooser.getSelectedFile();
            if(img.setImage(bi)) updateDimensionsAndCrop();
            else Toolkit.getDefaultToolkit().beep();
         }
      }
      else if(src == cropXField)
      {
         // whenever the X-coordinate of the crop rectangle's top-left corner changes, we may also have to fix the
         // width to ensure crop rectangle remains inside the original source image rectangle.
         Rectangle rCrop = img.getCrop();
         int imgW = img.getImageWidth();
         if(imgW == 0) return;
         if(rCrop == null) rCrop = new Rectangle(0, 0, imgW, img.getImageHeight());
         int x = cropXField.getValue().intValue();
         if(x >= imgW)
         {
            Toolkit.getDefaultToolkit().beep();
            cropXField.setValue(rCrop.x);
         }
         else
         {
            rCrop.x = x;
            if(x + rCrop.width > imgW) rCrop.width = imgW - x;
            if(!img.setCrop(rCrop))
            {
               Toolkit.getDefaultToolkit().beep();
               cropXField.setValue(rCrop.x);
            }
            else
               cropWField.setValue(rCrop.width);
         }
      }
      else if(src == cropYField)
      {
         // whenever the Y-coordinate of the crop rectangle's top-left corner changes, we may also have to fix the
         // height to ensure crop rectangle remains inside the original source image rectangle.
         Rectangle rCrop = img.getCrop();
         int imgH = img.getImageHeight();
         if(imgH == 0) return;
         if(rCrop == null) rCrop = new Rectangle(0, 0, img.getImageWidth(), imgH);
         int y = cropYField.getValue().intValue();
         if(y >= imgH)
         {
            Toolkit.getDefaultToolkit().beep();
            cropYField.setValue(rCrop.y);
         }
         else
         {
            rCrop.y = y;
            if(y + rCrop.height > imgH) rCrop.height = imgH - y;
            if(!img.setCrop(rCrop))
            {
               Toolkit.getDefaultToolkit().beep();
               cropYField.setValue(rCrop.y);
            }
            else
               cropHField.setValue(rCrop.height);
         }
      }
      else if(src == cropWField)
      {
         // crop width is auto-corrected to ensure crop rectangle remains inside the original source image rectangle
         Rectangle rCrop = img.getCrop();
         int imgW = img.getImageWidth();
         if(imgW == 0) return;
         if(rCrop == null) rCrop = new Rectangle(0, 0, imgW, img.getImageHeight());
         int oldW = rCrop.width;
         rCrop.width = cropWField.getValue().intValue();
         if(rCrop.x + rCrop.width > imgW) rCrop.width = imgW - rCrop.x;
         if(!img.setCrop(rCrop)) 
         {
            Toolkit.getDefaultToolkit().beep();
            rCrop.width = oldW;
         }
         cropWField.setValue(rCrop.width);
      }
      else if(src == cropHField)
      {
         // crop height is auto-corrected to ensure crop rectangle remains inside the original source image rectangle
         Rectangle rCrop = img.getCrop();
         int imgH = img.getImageHeight();
         if(imgH == 0) return;
         if(rCrop == null) rCrop = new Rectangle(0, 0, img.getImageWidth(), imgH);
         int oldH = rCrop.height;
         rCrop.height = cropHField.getValue().intValue();
         if(rCrop.y + rCrop.height > imgH) rCrop.height = imgH - rCrop.y;
         if(!img.setCrop(rCrop)) 
         {
            Toolkit.getDefaultToolkit().beep();
            rCrop.height = oldH;
         }
         cropHField.setValue(rCrop.height);
      }
      else if(src == cropResetBtn)
      {
         img.setCrop(null);
      }
      else if(src == xEditor)
      {
         if(!img.setX(xEditor.getMeasure()))
         {
            Toolkit.getDefaultToolkit().beep();
            xEditor.setMeasure(img.getX());
         }
      }
      else if(src == yEditor)
      {
         if(!img.setY(yEditor.getMeasure()))
         {
            Toolkit.getDefaultToolkit().beep();
            yEditor.setMeasure(img.getY());
         }
      }
      else if(src == rotateField)
      {
         if(!img.setRotate(rotateField.getValue().doubleValue()))
         {
            Toolkit.getDefaultToolkit().beep();
            rotateField.setValue(img.getRotate());
         }
      }
      else if(src == wEditor)
      {
         if(!img.setWidth(wEditor.getMeasure()))
         {
            Toolkit.getDefaultToolkit().beep();
            wEditor.setMeasure(img.getWidth());
         }
      }
      else if(src == hEditor)
      {
         if(!img.setHeight(hEditor.getMeasure()))
         {
            Toolkit.getDefaultToolkit().beep();
            hEditor.setMeasure(img.getHeight());
         }
      }
      else if(src == marginEditor)
      {
         if(!img.setMargin(marginEditor.getMeasure()))
         {
            Toolkit.getDefaultToolkit().beep();
            marginEditor.setMeasure(img.getMargin());
         }
      }
   }

   
   /** Convenience method casts the edited node to an instance of {@link ImageNode}. */
   private ImageNode getImageNode() { return((ImageNode) getEditedNode()); }
   
   /** Click this button to load a different source image for the image node being edited. */
   private JButton loadBtn = null;
   
   /** Abstract pathname of the source file from which last image was loaded (for initializing file chooser state). */
   private File lastImgFile = null;
   
   /** Label reflects pixel dimensions of current image. If there is no image, label reads "<no image>" */
   private JLabel dimLabel = null;
   
   /** Numeric text field edits the X-coordinate of the current cropping rectangle (in image pixels). */
   private NumericTextField cropXField = null;
   /** Numeric text field edits the Y-coordinate of the current cropping rectangle (in image pixels). */
   private NumericTextField cropYField = null;
   /** Numeric text field edits the width of the current cropping rectangle (in image pixels). */
   private NumericTextField cropWField = null;
   /** Numeric text field edits the height of the current cropping rectangle (in image pixels). */
   private NumericTextField cropHField = null;
   /** Click this button to clear the current cropping rectangle so that full source image is drawn. */
   private JButton cropResetBtn = null;
   
   /** Customized component for editing the x-coordinate of the BL corner of the image's bounding box. */
   private MeasureEditor xEditor = null;

   /** Customized component for editing the y-coordinate of the BL corner of the image's bounding box. */
   private MeasureEditor yEditor = null;

   /** Customized component for editing the width of the image's bounding box. */
   private MeasureEditor wEditor = null;

   /** Customized component for editing the height of the image's bounding box. */
   private MeasureEditor hEditor = null;

   /** Customized component for editing the inner margin. */
   private MeasureEditor marginEditor = null;

   /** Numeric text field for editing the rotation of the image's bounding box about its bottom-left corner. */
   private NumericTextField rotateField = null;

   /** Compact widget uses a pop-up panel to edit the background fill descriptor for the image's bounding box. */
   private BkgFillPicker bkgFillPicker = null;
   
   /** Self-contained editor handles the node's draw/stroke-related styles. */
   private DrawStyleEditor drawStyleEditor = null;
   
   /** Horizontal/vertical space between widgets, in pixels. */
   private final static int GAP = 5;
}
