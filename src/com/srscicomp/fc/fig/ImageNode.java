package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * <code>ImageNode</code> represents an embedded image in the <em>DataNav</em> graphic model. The actual image is 
 * rendered within a rectangular bounding box, scaled to fit within that bounding box while preserving the original 
 * image's aspect ratio. The scaled image is centered both horizontally and vertically within the bounding box. The node
 * includes properties for specifying the bottom-left corner, dimensions and orientation of the bounding box, a minimum
 * margin between the bounding box and any side of the embedded image, and a background fill style.
 * 
 * <p>The original source image may be optionally cropped. In this case, the "subimage" within the cropping rectangle
 * <i>[ulx uly w h]</i> is scaled to fit the element's bounding box while preserving that rectangle's aspect ratio. The
 * original image is not discarded, in case the user decides to change or eliminate the cropping rectangle.</p>
 * 
 * <p>The location and size of the image's bounding frame depend upon the parent viewport. The image node itself does 
 * not admit any child nodes. If no source image has been defined on the node, then its rendering displays a simple
 * "default" image.</p>
 * 
 * @author sruffner
 */
public class ImageNode extends FGraphicNode implements Cloneable
{
   /**
    * Construct an image node with a 1.5-in x 2-in bounding box located at the bottom-left corner of its parent 
    * viewport. Initially, no source image is defined, so the node renders a default image instead. The inner margin 
    * (specifying the minimum separation between an edge of the scaled image and the corresponding edge of the node's 
    * rectangular bounding box) is set to 0in, the background fill is uniform white, and there is no crop rectangle.
    */
   public ImageNode()
   {
      super(HASLOCATTRS|HASSIZEATTRS|HASROTATEATTR|HASSTROKEATTRS|HASSTRKPATNATTR);
      setX(new Measure(0, Measure.Unit.PCT));
      setY(new Measure(0, Measure.Unit.PCT));
      setWidth(new Measure(1.5, Measure.Unit.IN));
      setHeight(new Measure(2, Measure.Unit.IN));
      setRotate(0);
      
      margin = new Measure(0, Measure.Unit.IN);
      bkgFill = BkgFill.createSolidFill(Color.WHITE);
      image = null;
      crop = null;
   }

   
   //
   // Image node properties
   //

   /** The background fill for the image's bounding box: solid color (possibly transparent) or gradient. */
   private BkgFill bkgFill = null;

   /**
    * Get the current background fill for the image's bounding box.
    * @return The background fill descriptor.
    */
   public BkgFill getBackgroundFill() { return(bkgFill); }
   
   /**
    * Set the background fill for this image's bounding box. If a change is made, an "undo" operation is posted and
    * {@link #onNodeModified()} is invoked.
    * @param The new background fill descriptor. A null value is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setBackgroundFill(BkgFill bf)
   {
      if(bf == null) return(false);
      if(!bkgFill.equals(bf))
      {
         if(doMultiNodeEdit(FGNProperty.BKGC, bf)) return(true);
         
         BkgFill old = bkgFill;
         bkgFill = bf;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.BKGC);
            FGNRevEdit.post(this, FGNProperty.BKGC, bkgFill, old);
         }
      }
      return(true);
   }
   
   /** Measured width of the inner margin. */
   private Measure margin;

   /**
    * Get the width of the image node's inner margin. If non-zero, the image will be scaled to fit within a rectangle
    * defined by the node's bounding box less this margin on all sides.
    * 
    * @return The margin width with associated units of measure (only physical units allowed). The same margin applies
    * to all four sides of the bounding box.
    */
   public Measure getMargin() { return(margin); }

   /**
    * Set the width of the inner margin. An "undo" operation is posted, and {@link #onNodeModified()} is invoked.
    * 
    * @param m The new margin width. The measure is constrained to satisfy {@link #STROKEWCONSTRAINTS}. A null value is 
    * rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setMargin(Measure m)
   {
      if(m==null) return(false);
      m = STROKEWCONSTRAINTS.constrain(m);

      boolean changed = (margin != m) && !Measure.equal(margin, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.GAP, m)) return(true);
         
         Measure old = margin;
         margin = m;
         if(areNotificationsEnabled())
         {
            if(old.toMilliInches() != margin.toMilliInches()) onNodeModified(FGNProperty.GAP);
            FGNRevEdit.post(this, FGNProperty.GAP, margin, old);
         }
      }
      return(true);
   }

   /** The current cropping rectangle, in image space pixels. If null, the original source image is uncropped. */
   private Rectangle crop;
   
   /**
    * Get the current cropping rectangle for this image node.
    * @return The cropping rectangle: top-left corner, width and height of the rectangle in image space pixels -- where
    * (0,0) is the top-left corner of the original source image, and Y increases downward. Returns null if the image is
    * NOT cropped.
    */
   public Rectangle getCrop()
   {
      return(crop == null ? null : new Rectangle(crop));
   }
   
   /**
    * Set the current cropping rectangle for this image node. If no source image is currently defined, then the crop
    * rectangle cannot be set.
    * @param r The new cropping rectangle, in image pixels with origin at top-left corner in the original source image 
    * and Y-axis increasing downward. If this does not specify a valid rectangle within the original source image, it
    * will be rejected. It may be null or an empty rectangle (zero width or height), indicating that the image should 
    * NOT be cropped.
    * @return True if successful, false if value was rejected or if no source image is currently defined.
    */
   public boolean setCrop(Rectangle r)
   {
      if(image == null) return(false);
      
      // an empty rectangle, or a rectangle that matches the original image rectangle, is the same as null -- meaning 
      // image should not be cropped at all.
      if(r != null)
      {
         if(r.width == 0 || r.height == 0) r = null;
         else if(r.x == 0 && r.y == 0 && r.width == image.getWidth() && r.height == image.getHeight()) r = null;
      }
      
      // verify that specified rectangle is valid
      int imgW = image.getWidth();
      int imgH = image.getHeight();
      boolean valid = true;
      if(r != null)
      {
         valid = (r.x >= 0) && (r.x < imgW) && (r.y >= 0) && (r.y < imgH);
         valid = valid && (r.width > 0) && (r.height > 0) && (r.x + r.width <= imgW) && (r.y + r.height <= imgH);
      }
      if(!valid) return(false);
      
      boolean changed = (r==null) ? (crop != null) : !r.equals(crop);
      if(changed)
      {
         // we have to use an empty rectangle rather than null as the old value, or undo op will not be posted
         Rectangle old = (crop == null) ? new Rectangle() : crop;
         crop = (r == null) ? null : new Rectangle(r);
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CROP);
            String desc = ((crop == null) ? "Reset" : "Change") +  " crop rectangle on image";
            FGNRevEdit.post(this, FGNProperty.CROP, crop == null ? new Rectangle() : r, old, desc);
         }
      }
      return(true);
   }
   
   /** The (buffered) image rendered by this graphic node. */
   private BufferedImage image;
   
   /**
    * Get the source image rendered by this image node.
    * @return The original source image. Clients should not modify this image, as it is NOT an independent copy. If no
    * source image is currently defined, method returns null.
    */
   public BufferedImage getImage() { return(image); }
   
   /**
    * Get width of source image.
    * @return Width of original source image in pixels. Return 0 is no source image is defined.
    */
   public int getImageWidth() { return(image == null ? 0 : image.getWidth()); }
   
   /**
    * Get height of source image.
    * @return Height of original source image in pixels. Return 0 is no source image is defined.
    */
   public int getImageHeight() { return(image == null ? 0 : image.getHeight()); }
   
   /**
    * Set the source image rendered by this image node. The current cropping rectangle is also reset.
    * @param bi The new source image. Can be null, indicating NO source image -- in which case a default "stand-in"
    * image is used to render the node.
    * @return True if successful, false otherwise.
    */
   public boolean setImage(BufferedImage bi)
   {
      if(bi == image) return(true);
      
      BufferedImage old = image;
      Rectangle oldCrop = crop;
      image = bi;
      crop = null;
      if(areNotificationsEnabled())
      {
         onNodeModified(FGNProperty.IMG);
         
         // we store both source image AND crop rectangle (current and previous states) in the reversible edit object
         FGNRevEdit.post(this, FGNProperty.IMG, new Object[] {image, crop}, new Object[]{old, oldCrop}, 
               "Change source for image node");
      }
      return(true);
   }
   
   /**
    * Get the bounds of the drawn image -- not including the inner margin or the element border -- transformed into the
    * containing figure node's "global" rendering coordinates. Since the image element may be rotated with respect to
    * the root figure, the image bounds is returned as a generic shape rather than a rectangle.
    * 
    * @return The shape which tightly bounds the image (or sub-image, if cropped) drawn by this node, in the root
    * figure's rendering coordinates. If no source image is defined, the shape is empty.
    */
   public Shape getTightImageBoundsInFigure()
   {
      // get tight bounding rectangle in local coordinates
      FViewport2D parentVP = getParentViewport();
      Rectangle2D bbRect = (parentVP == null) ? null : parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      Rectangle2D r = new Rectangle2D.Double();
      if(bbRect != null && image != null)
      {
         double w = bbRect.getWidth();
         double h = bbRect.getHeight();
         double m = margin.toMilliInches();
         w -= 2*m;
         h -= 2*m;
         
         double imgW = crop == null ? image.getWidth(null) : crop.width;
         double imgH = crop == null ? image.getHeight(null) : crop.height;
         double scale = 1.0;
         double dx = m;
         double dy = m;
         if(imgW/imgH >= w/h)
         {
            scale = w/imgW;
            dy += (h - scale*imgH) / 2.0;
         }
         else
         {
            scale = h/imgH;
            dx += (w - scale*imgW) / 2.0;
         }

         r.setFrame(dx, dy, scale*imgW, scale*imgH);
      }
      
      return(getLocalToGlobalTransform().createTransformedShape(r));
   }
   
   /**
    * Convert a location specified in the root figure's rendering coordinates to a pixel location within the image or 
    * subimage currently rendered by this image node. The conversion takes into account the current cropped state of
    * the source image, and reports the location in pixels within the ORIGINAL source image.
    * 
    * @param p A point in the containing figure's rendering coordinates. If conversion is successful, the point will
    * contain the location in the original source image space, in pixels. The top-left corner of that image is at (0,0),
    * while the bottom-right is at (W,H), where W,H are the source image dimensions.
    * @return True if conversion is successful. Returns false if: (1) no source image is defined; (2) the specified
    * point lies outside the rectangle currently covered by the image; (3) conversion failed.
    */
   public boolean convertPointToImagePixels(Point2D p)
   {
      if(p == null || image == null) return(false);
      
      // get transform from local to figure coordinates
      FViewport2D parentVP = getParentViewport();
      Rectangle2D r = (parentVP == null) ? null : parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      if(r == null) return(false);
      AffineTransform at = getLocalToGlobalTransform();
      
      // convert point from figure coordinates to local coordinates using the inverse transform.
      try{ at.inverseTransform(p, p); } catch(NoninvertibleTransformException nte) { return(false); }
      
      // convert coordinate to milli-inches WRT BL corner of the current image rectangle. If point is not inside this
      // rectangle, then conversion fails.
      double w = r.getWidth();
      double h = r.getHeight();
      double m = margin.toMilliInches();
      w -= 2*m;
      h -= 2*m;
      
      double imgW = crop == null ? image.getWidth(null) : crop.width;
      double imgH = crop == null ? image.getHeight(null) : crop.height;
      double scale = 1.0;
      double dx = m;
      double dy = m;
      if(imgW/imgH >= w/h)
      {
         scale = w/imgW;
         dy += (h - scale*imgH) / 2.0;
      }
      else
      {
         scale = h/imgH;
         dx += (w - scale*imgW) / 2.0;
      }

      if(p.getX() < dx || p.getX() > dx + w || p.getY() < dy || p.getY() > dy + h) return(false);
      p.setLocation(p.getX()-dx, p.getY()-dy);
      
      // finally, convert coordinates to image pixel space. Note that we have to flip the Y-coordinate and take into
      // account the current crop rectangle, if there is one.
      dx = (crop == null ? 0 : crop.x) + p.getX() / scale;
      dy = (crop == null ? 0 : crop.y) + (imgH - (p.getY() / scale));
      p.setLocation(Math.round(dx), Math.round(dy));
      
      return(true);
   }
   
   /**
    * Calculate the crop rectangle containing the specified diagonal.
    * 
    * @param p1 One endpoint of the diagonal, measured in the root figure's rendering coordinates.
    * @param p2 The other diagonal endpoint, again measured in the root figure's rendering coordinates.
    * @param cropRect (Optional) If not null and the crop rectangle was successfully computed, this is initialized to
    * contain the crop rectangle's top-left corner and dimensions, in image space pixels.
    * @return The crop rectangle defined by the diagonal, again in the root figure's rendering coordinates. Since the
    * image may be rotated with respect to the figure's coordinate system, the crop rectangle must be returned as a
    * {@link Shape Shape} instead. If no source image is currently defined, if either diagonal endpoint is
    * ill-defined, or if the crop rectangle cannot be computed for any other reason, method returns null.
    */
   public Shape getCropShapeFromDiagonal(Point2D p1, Point2D p2, Rectangle cropRect)
   {
      if(!(image != null && Utilities.isWellDefined(p1) && Utilities.isWellDefined(p2))) return(null);
      
      // get transform from local to figure coordinates
      FViewport2D parentVP = getParentViewport();
      Rectangle2D bbRect = (parentVP == null) ? null : parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      if(bbRect == null) return(null);
      AffineTransform at = getLocalToGlobalTransform();
      
      // convert each point from figure coordinates to local coordinates using the inverse transform, then form a
      // rectangle from the diagonal
      try
      { 
         at.inverseTransform(p1, p1); 
         at.inverseTransform(p2, p2); 
      } 
      catch(NoninvertibleTransformException nte) { return(null); }
      
      Rectangle2D r = new Rectangle2D.Double();
      r.setFrameFromDiagonal(p1, p2);
      
      // compute the rectangle that tightly bounds the current image rectangle
      double w = bbRect.getWidth();
      double h = bbRect.getHeight();
      double m = margin.toMilliInches();
      w -= 2*m;
      h -= 2*m;
      
      double imgW = crop == null ? image.getWidth(null) : crop.width;
      double imgH = crop == null ? image.getHeight(null) : crop.height;
      double scale = 1.0;
      double dx = m;
      double dy = m;
      if(imgW/imgH >= w/h)
      {
         scale = w/imgW;
         dy += (h - scale*imgH) / 2.0;
      }
      else
      {
         scale = h/imgH;
         dx += (w - scale*imgW) / 2.0;
      }

      Rectangle2D rImg = new Rectangle2D.Double(dx, dy, scale*imgW, scale*imgH);
      
      // the intersection gives the crop rectangle in local coordinates. If it's empty, then we can't continue.
      Rectangle2D.intersect(rImg, r, r);
      if(r.isEmpty()) return(null);

      // success. Return the crop rectangle's dimensions in image space, and return the crop rectangle itself 
      // transformed back into the figure's rendering coordinates.
      if(cropRect != null)
      {
         // get rectangle's BL corner WRT BL corner of image rectangle, then convert to image space pixels. We don't
         // want to change the rectangle object -- because we need to return it, transformed to figure coords.
         dx = r.getX() - dx;
         dy = r.getMaxY() - dy;
         
         cropRect.x = (int) Math.round((crop == null ? 0 : crop.x) + dx / scale);
         cropRect.y = (int) Math.round((crop == null ? 0 : crop.y) + (imgH - (dy / scale)));
         cropRect.width = (int) Math.round(r.getWidth()/scale);
         cropRect.height = (int) Math.round(r.getHeight()/scale);
      }
      
      return(at.createTransformedShape(r));
   }
   
   
   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
      case BKGC : ok = setBackgroundFill((BkgFill) propValue); break;
      case GAP : ok = setMargin((Measure) propValue); break;
      case CROP : ok = setCrop((Rectangle) propValue); break;
      case IMG : 
         Object[] state = (Object[]) propValue;
         image = (BufferedImage) state[0];
         crop = (Rectangle) state[1];
         ok = true; 
         break;
      default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      // NOTE: No support for multi-node editing of source image or crop rectangle. So this should never be called
      // for the CROP or IMG properties.
      Object value = null;
      switch(p)
      {
      case BKGC : value = getBackgroundFill(); break;
      case GAP : value = getMargin(); break;
      case CROP : value = getCrop(); break;
      case IMG :  value = getImage(); break;
      default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }


   // 
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet()  { return(true); }

   /** The node-specific properties exported in an image node's style set are the background fill and margin. */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.BKGC, getBackgroundFill());
      styleSet.putStyle(FGNProperty.GAP, getMargin());
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      BkgFill bf = (BkgFill) applied.getCheckedStyle(FGNProperty.BKGC, null, BkgFill.class);
      if(bf != null && !bf.equals(restore.getStyle(FGNProperty.BKGC)))
      {
         bkgFill = bf;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.BKGC);
      
      // only text box and image nodes use the GAP property to specify an inner margin.
      FGNodeType nt = applied.getSourceNodeType();
      boolean applicable =  (nt == FGNodeType.TEXTBOX || nt == FGNodeType.IMAGE);
      Measure m = (Measure) applied.getCheckedStyle(FGNProperty.GAP, null, Measure.class);
      if(applicable && m != null && !Measure.equal(m, (Measure) restore.getStyle(FGNProperty.GAP)))
      {
         margin = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.GAP);

      
      return(changed);
   }


   // 
   // Support for child nodes 
   //

   /** The image node does not admit any child nodes whatsoever. */
   @Override public boolean isLeaf() { return(true); }

   /** The image node does not admit any child nodes whatsoever. */
   @Override public boolean canInsert(FGNodeType nodeType) { return(false); }

   @Override public FGNodeType getNodeType() { return(FGNodeType.IMAGE); }

   //
   // Renderable/Focusable support
   //
   
   /** Checks for a translucent background fill, else defers to super class implementation. */
   @Override protected boolean isTranslucent()
   {
      if(bkgFill.getFillType() == BkgFill.Type.SOLID)
      {
         int alpha = bkgFill.getColor1().getAlpha();
         if(alpha > 0 && alpha < 255) return(true);
      }
      return(super.isTranslucent());
   }

   /**
    * The local rendering coordinate system of an image node is defined by its location <i>(x,y)</i> and size <i>(width,
    * height)</i>, properties that are specified WRT the parent viewport. The local origin lies at <i>(x,y)</i> WRT the 
    * parent, and the coordinate system may be rotated about this point IAW the <i>rotate</i> property. This method 
    * accounts for these properties when calculating the local-to-parent transform. If the transform is ill-defined for 
    * whatever reason, the identity transform is returned.
    */
   @Override public AffineTransform getLocalToParentTransform()
   {
      AffineTransform identity = new AffineTransform();

      // get bounding rectangle with respect to the parent viewport
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(identity);
      Rectangle2D rect = parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      if(rect == null) return(identity);

      // translate origin so that it is in coord system of parent viewport
      AffineTransform at = AffineTransform.getTranslateInstance(rect.getX(), rect.getY());

      // rotate wrt parent if necessary
      double rot = getRotate();
      if(rot !=  0) at.rotate(Math.toRadians(rot));
      
      return(at);
   }

   /**
    * The viewport for an image node is simply its rectangular bounding box, with the origin at its BL corner. Never
    * used, since an image node does not admit children.
    */
   @Override public FViewport2D getViewport()
   { 
      // get bounding rectangle with respect to the parent viewport
      FViewport2D parentVP = getParentViewport();
      Rectangle2D rect = (parentVP==null) ? null : parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      return(rect == null ? new FViewport2D(1,1) : new FViewport2D(rect.getWidth(), rect.getHeight()));
   }

   /** The image node maintains no internal resources (other than the source image) for rendering itself. */
   @Override protected void releaseRenderResourcesForSelf() {}

   /**
    * Regardless the source image, the render bounds of the image node is its bounding box, expanded on all sides by 
    * a half stroke-width. If the bounding box is currently ill-defined, an empty rectangle is returned.
    */
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      Rectangle2D r = new Rectangle2D.Double();
      FViewport2D parentVP = getParentViewport();
      Rectangle2D bbRect = (parentVP==null) ? null : parentVP.toMilliInches(getX(), getY(), getWidth(), getHeight());
      double hsw = (isStroked() ? getStrokeWidth() : 0)/2.0;
      if(bbRect != null) r.setFrame(-hsw, -hsw, bbRect.getWidth() + hsw*2, bbRect.getHeight() + hsw*2);
      return(r);
   }

   public boolean render(Graphics2D g2d, RenderTask task)
   {
      FViewport2D parentVP = getParentViewport();
      Point2D loc = parentVP == null ? null : parentVP.toMilliInches(getX(), getY());
      boolean needRender = loc != null && needsRendering(task);
      double w = needRender ? parentVP.fromMeasureToMilliInX(getWidth()) : 0;
      double h = needRender ? parentVP.fromMeasureToMilliInY(getHeight()) : 0;
      needRender = needRender && w > 0 && h > 0;
      
      if(!needRender) return(task == null ? true : task.updateProgress());
      
      // render image node in a copy of the graphics context, so we do not alter the original
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         g2dCopy.translate(loc.getX(), loc.getY());
         if(getRotate() != 0) g2dCopy.rotate(Math.toRadians(getRotate()));
         
         // fill and stroke the bounding box IAW node properties
         Rectangle2D r = new Rectangle2D.Double(0, 0, w, h);
         if(!bkgFill.isTransparent())
         {
            g2dCopy.setPaint(bkgFill.getPaintForFill((float)w, (float)h));
            g2dCopy.fill(r);
         }
         if(isStroked())
         {
            g2dCopy.setColor(getStrokeColor());
            g2dCopy.setStroke(getStroke(0));
            g2dCopy.draw(r);
         }

         // now position and draw the image. It is scaled to fit within the rectangle defined by the bounding box shrunk
         // IAW the margin property, while preserving its aspect ratio. 
         // IMPORTANT: Y-coordinate in "image space" is 0 at the top of the image, not the bottom, and increases toward 
         // the bottom. This is the opposite of the image node's logical coordinate space. Hence we have to translate
         // the logical Y-coordinate to where the TL corner of the image should be, and negate the scale factor in Y.
         double m = getMargin().toMilliInches();
         w -= 2*m;
         h -= 2*m;
         if(w <= 0 || h <= 0) return((task == null) ? true : task.updateProgress());
         
         // if we're rendering into a PDF graphics context while exporting the figure to PDF, we must use a different
         // approach for the export to succeed (has to do with iText PDF library implementation). This implementation 
         // appears to be less efficient than when we're rendering to the display, so we kept both...
         if(!PDFSupport.isPDFGraphics(g2dCopy))
         {
            Image img = (image != null) ? image : FCIcons.V4_BROKEN.getImage();
            double imgW = crop == null ? img.getWidth(null) : crop.width;
            double imgH = crop == null ? img.getHeight(null) : crop.height;
            double scale = 1.0;
            double dx = m;
            double dy = m;
            if(imgW/imgH >= w/h)
            {
               scale = w/imgW;
               dy += (h - scale*imgH) / 2.0;
            }
            else
            {
               scale = h/imgH;
               dx += (w - scale*imgW) / 2.0;
            }
            
            // bottom-left and top-right corners of image (or cropped image) to be drawn, in image space pixels
            int blX = (crop==null) ? 0 : crop.x;
            int blY = (crop==null) ? ((int) imgH-1) : (crop.y + crop.height);
            int trX = (crop==null) ? ((int) imgW-1) : (crop.x + crop.width);
            int trY = (crop==null) ? 0 : crop.y;
            
            g2dCopy.translate(dx, dy);
            g2dCopy.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2dCopy.drawImage(img, 0, 0, (int) (scale*imgW), (int) (scale*imgH), blX, blY, trX, trY, null);
         }
         else
         {
            BufferedImage bi = null;
            if(image == null)
            {
               Image img = FCIcons.V4_BROKEN.getImage();
               bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
               Graphics2D g2BI = bi.createGraphics();
               g2BI.drawImage(img, 0, 0, null);
               g2BI.dispose();
            }
            else
            {
               bi = (crop == null) ? image : image.getSubimage(crop.x, crop.y, crop.width, crop.height);
               
               // need to ensure image uses a color model that AffineTransformOp can handle
               BufferedImage bi2 = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
               Graphics2D g2BI = bi2.createGraphics();
               g2BI.drawImage(bi, 0, 0, null);
               g2BI.dispose();
               bi = bi2;
            }
            
            double imgW = bi.getWidth();
            double imgH = bi.getHeight();
            double scale = 1.0;
            double dx = m;
            double dy = m;
            if(imgW/imgH >= w/h)
            {
               scale = w/imgW;
               dy += (h - scale*imgH) / 2.0;
            }
            else
            {
               scale = h/imgH;
               dx += (w - scale*imgW) / 2.0;
            }

            g2dCopy.translate(dx, dy);
            
            // these shenanigans are because the origin in image space is along the top of the image, not the bottom,
            // as in FypML's logical coordinate space.
            AffineTransform xf = AffineTransform.getScaleInstance(scale, -scale);
            xf.translate(0, -imgH);

            AffineTransformOp op = new AffineTransformOp(xf, AffineTransformOp.TYPE_BICUBIC);
            g2dCopy.drawImage(bi, op, 0, 0);
         }
      }
      finally {if(g2dCopy != null) g2dCopy.dispose(); }

      return((task == null) ? true : task.updateProgress());
   }

   
   //
   // PSTransformable implementation
   //
   
   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      Point2D loc = parentVP.toMilliInches(getX(), getY());
      double w = parentVP.fromMeasureToMilliInX(getWidth());
      double h = parentVP.fromMeasureToMilliInY(getHeight());
      if(loc == null || w <= 0 || h <= 0) return;

      psDoc.startElement(this);
      psDoc.translateAndRotate(loc, getRotate());
      
      // stroke and fill the bounding box. If the fill color is transparent, the box is not filled.
      psDoc.renderRect(w, h, getStrokeWidth(), getStrokeColor().getAlpha() != 0, bkgFill);
      
      // if the tight bounding box is empty, we're done.
      double m = getMargin().toMilliInches();
      w -= 2*m;
      h -= 2*m;
      if(w <= 0 || h <= 0)
      {
         psDoc.endElement();
         return;
      }
      
      // if no image is defined, temporarily use the stock "broken icon" image.
      BufferedImage bi = (image != null) ? image : null;
      if(bi == null)
      {
         Image img = FCIcons.V4_BROKEN.getImage();
         bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
         Graphics2D g2BI = bi.createGraphics();
         g2BI.drawImage(img, 0, 0, null);
         g2BI.dispose();
      }
      
      // size and location of image within tight bounding box are adjusted to preserve aspect ratio.
      double imgW = (crop == null) ? bi.getWidth() : crop.width;
      double imgH = (crop == null) ? bi.getHeight() : crop.height;
      Point2D botLeft = new Point2D.Double();
      Point2D topRight = new Point2D.Double();
      if(imgW/imgH >= w/h)
      {
         double adjH = w*imgH/imgW;
         botLeft.setLocation(m, m + (h-adjH)/2.0);
         topRight.setLocation(m+w, m + (h+adjH)/2.0);
      }
      else
      {
         double adjW = h*imgW/imgH;
         botLeft.setLocation(m + (w-adjW)/2.0, m);
         topRight.setLocation(m + (w+adjW)/2.0, m+h);
      }

      psDoc.renderRGBImage(bi, crop, botLeft, topRight);
      
      psDoc.endElement();
   }

   
   //
   // Object
   //

   @Override protected Object clone()
   {
      ImageNode copy = (ImageNode) super.clone();
      return(copy);
   }
}
