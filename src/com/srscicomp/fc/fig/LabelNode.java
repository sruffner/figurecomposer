package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.srscicomp.common.g2dutil.SingleStringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;

/**
 * <b>LabelNode</b> represents a single-line relocatable text label in the <em>DataNav</em> graphic model. It includes 
 * attributes governing the location, angle, and alignment of the label, in addition to its character content. It does 
 * not have any stroke-related style attributes, and it cannot have any child nodes.
 * 
 * <p>As of v5.4.0 (schema version 24), <b>LabelNode</b> supports a "styled text" label, in which text color, font
 * style, underline state, superscript, and subscript can vary on a per-character basis. See {@link 
 * FGraphicNode#toStyledText()}.</p>
 * 
 * @author sruffner
 */
public class LabelNode extends FGraphicNode implements Cloneable
{
   /**
    * Construct a <code>LabelNode</code> located at (50%, 50%) in its parent viewport, left- and bottom-aligned, with 
    * the attributed text label set to "label" (no in-string attribute changes).
    */
   public LabelNode()
   {
      super(HASFONTATTRS|HASFILLCATTR|HASLOCATTRS|HASROTATEATTR|HASTITLEATTR|HASIDATTR|ALLOWATTRINTITLE);
      setX(new Measure(50, Measure.Unit.PCT));
      setY(new Measure(50, Measure.Unit.PCT));
      setRotate(0);
      setTitle("label");
      hAlign = TextAlign.LEADING;
      vAlign = TextAlign.TRAILING;
   }

   
   //
   // Label properties
   //

   /**
    * Horizontal alignment of label string relative to the location of this <code>LabelNode</code>.
    */
   private TextAlign hAlign;

   /**
    * Get the horizontal alignment of this <code>LabelNode</code>'s label string. The string is positioned relative to 
    * the node's location coordinates <em>(x, y)</em>. 
    * 
    * @return Horizontal alignment of label string.
    */
   public TextAlign getHorizontalAlignment() { return(hAlign); }

   /**
    * Set the horizontal alignment of this <code>LabelNode</code>'s label string.
    * 
    * @param align The new horizontal alignment.
    */
   public void setHorizontalAlignment(TextAlign align)
   {
      if(hAlign != align)
      {
         if(doMultiNodeEdit(FGNProperty.HALIGN, align)) return;

         TextAlign old = hAlign;
         hAlign = align;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.HALIGN);
            FGNRevEdit.post(this, FGNProperty.HALIGN, hAlign, old);
         }
      }
   }
   
   /** Vertical alignment of label string relative to the location of this <code>LabelNode</code>. */
   private TextAlign vAlign;

   /**
    * Get the vertical alignment of this <code>LabelNode</code>'s label string. The string is positioned relative to 
    * the node's location coordinates <em>(x, y)</em>.
    * 
    * @return Vertical alignment of label string.
    */
   public TextAlign getVerticalAlignment() { return(vAlign); }

   /**
    * Set the vertical alignment of this <code>LabelNode</code>'s label string.
    * 
    * @param align The new vertical alignment.
    */
   public void setVerticalAlignment(TextAlign align)
   {
      if(vAlign != align)
      {
         if(doMultiNodeEdit(FGNProperty.VALIGN, align)) return;

         TextAlign old = vAlign;
         vAlign = align;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.VALIGN);
            FGNRevEdit.post(this, FGNProperty.VALIGN, vAlign, old);
         }
      }
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
         case HALIGN : setHorizontalAlignment((TextAlign)propValue); ok = true; break;
         case VALIGN : setVerticalAlignment((TextAlign)propValue); ok = true; break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
         case HALIGN : value = getHorizontalAlignment(); break;
         case VALIGN : value = getVerticalAlignment(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }


   // 
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet()  { return(true); }

   /** The node-specific properties exported in a label node's style set are the H and V text alignment. */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.HALIGN, getHorizontalAlignment());
      styleSet.putStyle(FGNProperty.VALIGN, getVerticalAlignment());
   }

   /** Accounts for the two label node-specific styles: horizontal and vertical text alignment. */
   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      TextAlign ta = (TextAlign) applied.getCheckedStyle(FGNProperty.HALIGN, getNodeType(), TextAlign.class);
      if(ta != null && !ta.equals(restore.getStyle(FGNProperty.HALIGN)))
      {
         hAlign = ta;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.HALIGN);
      
      ta = (TextAlign) applied.getCheckedStyle(FGNProperty.VALIGN, getNodeType(), TextAlign.class);
      if(ta != null && !ta.equals(restore.getStyle(FGNProperty.VALIGN)))
      {
         vAlign = ta;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.VALIGN);
      
      return(changed);
   }


   // 
   // Support for child nodes 
   //

   /**
    * <code>LabelNode</code> does not admit any child nodes whatsoever.
    * 
    * @see FGraphicNode#isLeaf()
    */
   @Override
   public boolean isLeaf() { return(true); }

   /**
    * <code>LabelNode</code> does not admit any child nodes whatsoever.
    * 
    * @see FGraphicNode#canInsert(FGNodeType)
    */
   @Override
   public boolean canInsert(FGNodeType nodeType) { return(false); }

   @Override
   public FGNodeType getNodeType() { return(FGNodeType.LABEL); }

   
   //
   // Alignment support
   //
   
   /** 
    * A label node can be aligned with other nodes. The label's location and text alignment may be adjusted to achieve 
    * the desired alignment.
    */
   @Override boolean canAlign() { return(true); }

   /**
    * The rendered bounds of the text label are used to compute the alignment locus. If the bounds are not available for
    * whatever reason, null is returned.
    */
   @Override Double getAlignmentLocus(FGNAlign align)
   {
      if(align == null || !canAlign()) return(null);
      
      Rectangle2D r = getCachedGlobalBounds();
      if(r == null || r.isEmpty()) return(null);
      
      double locus = 0;
      switch(align)
      {
      case LEFT : locus = r.getMinX(); break;
      case RIGHT : locus = r.getMaxX(); break;
      case BOTTOM : locus = r.getMinY(); break;
      case TOP : locus = r.getMaxY(); break;
      case HCENTER : locus = r.getCenterX(); break;
      case VCENTER : locus = r.getCenterY(); break;
      }
      return(new Double(locus));
   }

   @Override boolean align(FGNAlign alignType, double locus, MultiRevEdit undoer)
   {
      if(alignType == null || undoer == null || (!Utilities.isWellDefined(locus)) || !canAlign()) return(false);
      
      // transform current label location into figure-level ("global") coordinates, in milli-inches
      Measure currX = getX();
      Measure currY = getY();
      
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(false);
      Point2D loc = parentVP.toMilliInches(currX, currY);
      if(loc == null) return(false);
      AffineTransform at = getParent().getLocalToGlobalTransform();
      at.transform(loc, loc);

      // is there a net rotation on the label?
      double rot = getRotate();
      FGraphicNode parent = getParent();
      while(parent != null) { rot += parent.getRotate(); parent = parent.getParent(); }
      while(rot < 0) rot += 360;
      rot = rot % 360.0;
      if(rot < 0.01) rot = 0;
      
      // when the label is rotated WRT the containing figure, it makes no sense to adjust the text alignment. In this
      // scenario, we get the label's global render bounds and compute the adjustment in its X- or Y-coordinate so that
      // its global bounds line up with the locus as specified. When the label is not rotated, we adjust its H or V
      // alignment and its X- or Y- coordinate IAW the alignment type
      TextAlign ha = hAlign;
      TextAlign va = vAlign;

      if(rot == 0)
      {
         if(alignType == FGNAlign.LEFT || alignType == FGNAlign.RIGHT || alignType == FGNAlign.HCENTER)
            loc.setLocation(locus, loc.getY());
         else
            loc.setLocation(loc.getX(), locus);
         
         switch(alignType)
         {
         case LEFT : ha = TextAlign.LEADING; break;
         case RIGHT : ha = TextAlign.TRAILING; break;
         case HCENTER : ha = TextAlign.CENTERED; break;
         case BOTTOM : va = TextAlign.TRAILING; break;
         case TOP : va = TextAlign.LEADING; break;
         case VCENTER: va = TextAlign.CENTERED; break;
         }
      }
      else
      {
         Rectangle2D r = getCachedGlobalBounds();
         if(r == null || r.isEmpty()) return(false);
         
         double dx = 0;
         double dy = 0;
         switch(alignType)
         {
         case LEFT: dx = locus - r.getMinX(); break;
         case RIGHT: dx = locus - r.getMaxX(); break;
         case BOTTOM: dy = locus - r.getMinY(); break;
         case TOP: dy = locus - r.getMaxY(); break;
         case HCENTER: dx = locus - r.getCenterX(); break;
         case VCENTER: dy = locus - r.getCenterY(); break;
         }
         loc.setLocation(loc.getX() + dx, loc.getY() + dy);
      }
      
      // transform new location back to parent viewport
      try { at = at.createInverse(); }
      catch( NoninvertibleTransformException nte ) { return(false); }
      at.transform(loc, loc);

      // get location coordinates in their original units of measure
      Measure.Constraints c = FGraphicModel.getLocationConstraints(getNodeType());
      Measure[] coords = parentVP.fromMilliInches(loc.getX(), currX.getUnits(), loc.getY(), currY.getUnits(), 
            c.nMaxFracDigits);
      if(coords == null) return(false);
      
      coords[0] = c.constrain(coords[0], true);
      coords[1] = c.constrain(coords[1], true);
      
      // update any altered property values and record the changes in the reversible edit object supplied
      setNotificationsEnabled(false);
      boolean changed = false;
      try
      {
         if(!Measure.equal(currX, coords[0]))
         {
            changed = true;
            setX(coords[0]);
            undoer.addPropertyChange(this, FGNProperty.X, coords[0], currX);
         }
         if(!Measure.equal(currY, coords[1]))
         {
            changed = true;
            setY(coords[1]);
            undoer.addPropertyChange(this, FGNProperty.Y, coords[1], currY);
         }
         if(ha != hAlign)
         {
            changed = true;
            TextAlign old = hAlign;
            hAlign = ha;
            undoer.addPropertyChange(this, FGNProperty.HALIGN, hAlign, old);
         }
         if(va != vAlign)
         {
            changed = true;
            TextAlign old = vAlign;
            vAlign = va;
            undoer.addPropertyChange(this, FGNProperty.VALIGN, vAlign, old);
         }
      }
      finally { setNotificationsEnabled(true); }
      
      return(changed);
   }


   //
   // Renderable/Focusable support
   //
   
   /**
    * The local rendering coordinate system of a label node is defined by its location <i>(x,y)</i> and orientation 
    * <i>rotate</i>, properties that are specified WRT the parent viewport. The local origin lies at <i>(x,y)</i> WRT 
    * the parent, and the label may be rotated about this point IAW the <i>rotate</i> property. This method accounts for
    * these properties when calculating the local-to-parent transform. If the transform is ill-defined for whatever 
    * reason, the identity transform is returned.
    */
   @Override public AffineTransform getLocalToParentTransform()
   {
      AffineTransform identity = new AffineTransform();

      // get bounding rectangle with respect to the parent viewport
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(identity);
      Point2D loc = parentVP.toMilliInches(getX(), getY());
      if(loc == null) return(identity);

      // translate origin so that it is in coord system of parent viewport
      AffineTransform at = AffineTransform.getTranslateInstance(loc.getX(), loc.getY());

      // rotate wrt parent if necessary
      double rot = getRotate();
      if(rot !=  0) at.rotate(Math.toRadians(rot));
      
      return(at);
   }

   /** A label node does not admit children and does not define its own viewport, so the parent viewport is returned. */
   @Override public FViewport2D getViewport() { return(getParentViewport()); }

   /** The painter which renders this <code>LabelNode</code> IAW its current properties. */
   private SingleStringPainter textPainter = null;

   /**
    * Update the internal string painter that renders the label. The painter is set up to draw the text label at (0,0)
    * with no rotation. Any required translation and rotation must be applied for calling the painter's render method.
    * If the label node's current location is ill-defined in the parent viewport, or the parent viewport does not exist,
    * the painter's location is set to null, and the painter will render nothing.
    */
   private void updatePainter()
   {
      // painter is lazily constructed
      if(textPainter == null) 
      {
         textPainter = new SingleStringPainter();
         textPainter.setStyle(this); 
      }

      FViewport2D parentVP = getParentViewport();
      Point2D loc = (parentVP == null) ? null : parentVP.toMilliInches(getX(), getY());
      if(loc != null) loc.setLocation(0, 0);
      
      textPainter.setTextAndLocation(getAttributedTitle(true), loc);
      textPainter.setAlignment(hAlign, vAlign);
   }

   @Override protected void releaseRenderResourcesForSelf() { textPainter = null; }

   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || textPainter == null)
      {
         updatePainter();
         textPainter.updateFontRenderContext(g2d);
         textPainter.invalidateBounds();
      }
      
      return(textPainter.getBounds2D(null));
   }

   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(textPainter == null) updatePainter();
      FViewport2D parentVP = getParentViewport();
      Point2D loc = (parentVP == null) ? null : parentVP.toMilliInches(getX(), getY());

      if(loc != null && needsRendering(task))
      {
         Graphics2D g2dCopy = (Graphics2D) g2d.create();
         try
         {
            g2dCopy.translate(loc.getX(), loc.getY());
            if(getRotate() != 0) g2dCopy.rotate(Math.toRadians(getRotate()));
            
            textPainter.render(g2dCopy, task);
         }
         finally {if(g2dCopy != null) g2dCopy.dispose(); }
      }
      
      return((task == null) ? true : task.updateProgress());
   }

   
   //
   // PSTransformable implementation
   //
   
   /** 
    * Overridden to just specify the first 10 characters of the text label.
    */
   @Override public String getPSComment()
   {
      String s = getTitle();
      if(s.length() > 10) s = s.substring(0,10) + " ...";
      return(getNodeType().toString() + ": " + s);
   }

   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      FViewport2D parentVP = getParentViewport();
      
      // no text is rendered if the text/fill color is transparent. Postscript does not support transparency.
      String s = getTitle();
      if(parentVP == null || getFillColor().getAlpha() == 0 || s.length() == 0) return;
      
      psDoc.startElement(this);
      psDoc.translateAndRotate(parentVP.toMilliInches(getX(), getY()), getRotate());
      psDoc.renderText(s, 0, 0, hAlign, vAlign);
      psDoc.endElement();
   }

   
   //
   // Object
   //

   /**
    * This override ensures that the rendering infrastructure for the <code>LabelNode</code> clone is independent of 
    * the element cloned.
    */
   @Override
   protected Object clone()
   {
      LabelNode copy = (LabelNode) super.clone();
      copy.textPainter = null;
      return(copy);
   }
}
