package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.ResizeAnchor;
import com.srscicomp.common.g2dutil.ShapePainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.common.util.Utilities;

/**
 * <b>ShapeNode</b> encapsulates a shape of arbitrary width and height. Currently, any of the resizable preset shapes 
 * defined by {@link Marker} are supported as shapes, but eventually we may add support for defining an arbitrary shape
 * by connecting a series of points within a 1x1 scalable "design box".
 * 
 * <p><b>ShapeNode</b> was originally introduced to facilitate the creation of flowcharts within a figure, but it may 
 * also find uses in annotating graphs. It can parent any number of {@link LabelNode}s, so one can place text inside or 
 * adjacent to the shape. It possesses all of the inheritable style attributes, as well as the common positioning 
 * attributes: the coordinates <i>(x, y)</i> define the shape's center point relative to a parent viewport, while
 * <i>rotate</i> specifies a rotation about that point. In addition, the string specified by the generic <i>title</i> 
 * attribute, if not empty, is rendered at the center point, centered both horizontally and vertically. This automatic 
 * label offers an economical alternative to manually adding a {@link LabelNode} child. The automated label is drawn IAW
 * the shape node's text/fill color, while the shape itself is painted IAW its background fill property, supporting 
 * solid or gradient fill styles. The shape's outline is stroked IAW the node's stroke styles.</p>
 * 
 * <p>The <i>type</i> attribute identifies the design shape ({@link Marker}), while <i>width</i> and <i>height</i> 
 * specify the shape's dimensions. Both are restricted to non-negative values up to 10inches; relative units are not 
 * allowed -- see {@link FGraphicModel#SHAPESIZECONSTRAINTS}. [NOTE: Prior to v4.7.2, the shape node only had a single 
 * <i>size</i> attribute -- so the design shape was always scaled equally in X and Y. Replacing it with separate width 
 * and height attributes allows the shape to be resized differently in X and Y. Thus, for example, you can create an 
 * ellipse or rectangle with any aspect ratio; this was not possible before.</p>
 * 
 * <p>As of v5.4.0 (schema version 24), <b>ShapeNode</b>'s automatic label may contain "styled text", in which text 
 * color, font style, underline state, superscript, and subscript can vary on a per-character basis. See {@link 
 * FGraphicNode#toStyledText()}.</p>
 * 
 * @author 	sruffner
 */
public class ShapeNode extends FGraphicNode implements Cloneable
{

   /**
    * Construct a {@link Marker#BOX} shape located at the center of its parent viewport, with a width and height of 
    * 0.5in, a solid black background fill, and no auto-generated label. All style attributes are initially implicit.
    */
   public ShapeNode()
   {
      super(
        HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR|HASLOCATTRS|HASSIZEATTRS|
        HASROTATEATTR|HASTITLEATTR|ALLOWATTRINTITLE);

      setX(new Measure(50, Measure.Unit.PCT));
      setY(new Measure(50, Measure.Unit.PCT));
      setWidth(new Measure(0.5, Measure.Unit.IN));
      setHeight(new Measure(0.5, Measure.Unit.IN));
      setRotate(0);
      setTitle("");
      type = Marker.BOX;
      bkgFill = BkgFill.createSolidFill(Color.BLACK);
   }

   
   // 
   // ShapeNode properties
   //

   /** The shape type. */
   private Marker type;

   /**
    * Get the shape type, which defines its geometry within a unit "design box". This unit shape is then scaled IAW the
    * shape node's current size.
    * @return The shape type.
    */
   public Marker getType() { return(type); }

   /**
    * Set the shape type. If a change is made, {@link #onNodeModified()} is invoked.
    * 
    * @param type The new shape type. A null value is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setType(Marker type)
   {
      if(type == null) return(false);

      if(this.type != type)
      {
         if(doMultiNodeEdit(FGNProperty.TYPE, type)) return(true);
         
         Marker oldType = this.type;
         this.type = type;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.TYPE);
            FGNRevEdit.post(this, FGNProperty.TYPE, this.type, oldType);
         }
      }
      return(true);
   }

   /** The shape's background fill: solid color (possibly transparent) or a gradient. */
   private BkgFill bkgFill = null;

   /**
    * Get the current background fill for this shape.
    * @return The background fill descriptor.
    */
   public BkgFill getBackgroundFill() { return(bkgFill); }
   
   /**
    * Set the background fill for this shape. If a change is made, an "undo" operation is posted and {@link 
    * #onNodeModified()} is invoked.
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
   
   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
         case TYPE : ok = setType((Marker)propValue); break;
         case BKGC : ok = setBackgroundFill((BkgFill) propValue); break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
         case TYPE : value = getType(); break;
         case BKGC : value = getBackgroundFill(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }
   
   //
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   /** Node-specific properties exported in a shape node's style set are its type and background fill. */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.TYPE, getType());
      styleSet.putStyle(FGNProperty.BKGC, getBackgroundFill());
   }

   /** Accounts for the shape node-specific styles: shape type and background fill. */
   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      Marker m = (Marker) applied.getCheckedStyle(FGNProperty.TYPE, getNodeType(), Marker.class);
      if(m != null && !m.equals(restore.getStyle(FGNProperty.TYPE)))
      {
         type = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.TYPE);
      
      BkgFill bf = (BkgFill) applied.getCheckedStyle(FGNProperty.BKGC, null, BkgFill.class);
      if(bf != null && !bf.equals(restore.getStyle(FGNProperty.BKGC)))
      {
         bkgFill = bf;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.BKGC);
      
      return(changed);
   }


   
   // 
   // Support for child nodes
   //

   @Override public boolean canInsert(FGNodeType nodeType) { return(nodeType == FGNodeType.LABEL); }
   @Override public FGNodeType getNodeType() { return(FGNodeType.SHAPE); }

   //
   // Alignment support
   //
   
   /** A shape node can be aligned with other nodes, using its bounding box to calculate the adjustment. */
   @Override boolean canAlign() { return(true); }

   //
   // Renderable/Focusable support
   //

   /** 
    * A shape node is resizable by changing its width and/or height. It's different from other graphic nodes with the
    * standard bounding-box properties <i>x, y, width, height</i> because <i>(x,y)</i> is the <b>center</b> of the 
    * bounding box instead of its bottom-left corner.
    */
   @Override public boolean canResize() { return(true);  }

   /** 
    * The resize rectangle for a shape node is the rectangle that tightly bounds the <i>unstroked</i> shape. This will
    * typically be somewhat smaller than the render bounds for the shape node.
    */
   @Override Rectangle2D getResizeRectLocal()
   {
      double wMI = getWidth().toMilliInches();
      double hMI = getHeight().toMilliInches();
      if(wMI <= 0.0 || hMI <= 0.0) return(null);
      Rectangle2D r = type.getTransformedShape(wMI/2.0, hMI/2.0, wMI, hMI).getBounds2D();
      return(r.isEmpty() ? null : r);
   }

   /**
    * During an interactive drag resize operation, the resize outline includes two parts: the outline of the actual
    * shape scaled to the dimensions implied by the current state of the drag, and the rectangle that tightly bounds 
    * this shape (since that's what the user "grabs" to initiate the resize). Once the adjusted width or height reaches
    * 10% of the current value, no further reduction is allowed.
    * 
    * <p>Resizing a shape is different from other graphic objects with location and dimension properties, like a graph
    * or text box, because the (x,y) location denotes the <b>center</b> of the bounding box, not the bottom-left 
    * corner. By convention, dragging the bottom edge of the bounding box outward or inward has the same effect on the 
    * top edge, and vice versa, while the left and right edges remain unchanged. Analogously, when dragging the left or
    * right edge. When dragging a corner, the opposite corner moves by the same amount in the opposite direction.
    * Effectively, the interactive resize changes the width, the height, or both, while the center point (x,y) never
    * changes -- which is not true for objects like a graph or text box.</p>
    */
   @Override public Shape getDragResizeShape(ResizeAnchor anchor, Point2D p0, Point2D p1, StringBuffer cueBuf)
   {
      if(anchor == null || p0 == null || p1 == null) return(null);
      
      // transform drag points to local coordinate system (origin at bottom-left corner of square design box)
      AffineTransform at = getLocalToGlobalTransform();
      AffineTransform invAT;
      try { invAT = at.createInverse(); } catch(NoninvertibleTransformException nte) { return(null); }
      invAT.transform(p0, p0);
      invAT.transform(p1, p1);
      
      // drag deltas in X and Y, in milli-inches
      double dx = p1.getX() - p0.getX();
      double dy = p1.getY() - p0.getY();
      
      // current width and height of shape
      double wMI = getWidth().toMilliInches();
      double hMI = getHeight().toMilliInches();

      // adjusted width and/or height, respecting anchor and ensuring that neither dimension is reduced to less than
      // 10% of its current size. Again in milli-inches.
      double wAdjMI = wMI;
      double hAdjMI = hMI;
      switch(anchor)
      {
      case NORTH : hAdjMI += 2*dy; break;
      case SOUTH : hAdjMI -= 2*dy; break;
      case WEST  : wAdjMI -= 2*dx; break;
      case EAST  : wAdjMI += 2*dx; break;
      case NORTHWEST : hAdjMI += 2*dy; wAdjMI -= 2*dx; break;
      case NORTHEAST : hAdjMI += 2*dy; wAdjMI += 2*dx; break;
      case SOUTHWEST : hAdjMI -= 2*dy; wAdjMI -= 2*dx; break;
      case SOUTHEAST :
      case ANY :       hAdjMI -= 2*dy; wAdjMI += 2*dx; break;
      }
      if(wAdjMI < 0.1*wMI) wAdjMI = 0.1*wMI;
      if(hAdjMI < 0.1*hMI) hAdjMI = 0.1*hMI;
      
      // using the adjusted width and/or height, construct the shape outline in the local coordinate system, and append
      // its bounding box outline. The shape's center point is unchanged. This will be the resize shape, which still 
      // must be transformed to global coords.
      Shape s = type.getTransformedShape(wMI/2.0, hMI/2.0, wAdjMI, hAdjMI);
      Rectangle2D r = s.getBounds2D();
      if(r.isEmpty()) return(null);
      GeneralPath gp = new GeneralPath(s);
      gp.append(r, false);
      
      // the resize cue displays the adjusted width and/or height, in the correct units.
      if(cueBuf != null)
      {
         cueBuf.setLength(0);
         Measure.Constraints c = FGraphicModel.getSizeConstraints(getNodeType());
         Measure.Unit u = getWidth().getUnits();
         Measure m = Measure.getConstrainedRealMeasure(wAdjMI, u, c);
         cueBuf.append("W=").append(c.constrain(m, true).toString());
         
         u = getHeight().getUnits();
         m = Measure.getConstrainedRealMeasure(hAdjMI, u, c);
         cueBuf.append(" H=").append(c.constrain(m, true).toString());
      }
      
      return(at.createTransformedShape(gp));
   }

   /**
    * Resizing a shape node involves changing its width, height, or both -- depending on the resize anchor being used.
    * This method calculates the new width and height based on the anchor identity, and the distance between the start
    * and end of the drag. It prevents either dimension from being reduced to less than 10% of its current value. For
    * more details, see {@link #getDragResizeShape()}.
    */
   @Override public void executeResize(ResizeAnchor anchor, Point2D p0, Point2D p1)
   {
      if(anchor == null || p0 == null || p1 == null) return;
      
      // transform drag point to local coordinate system (origin at bottom-left corner of square design box)
      AffineTransform at = getLocalToGlobalTransform();
      AffineTransform invAT;
      try { invAT = at.createInverse(); } catch(NoninvertibleTransformException nte) { return; }
      invAT.transform(p0, p0);
      invAT.transform(p1, p1);
      
      // drag deltas in X and Y, in milli-inches
      double dx = p1.getX() - p0.getX();
      double dy = p1.getY() - p0.getY();
      
      // current width and height of shape
      double wMI = getWidth().toMilliInches();
      double hMI = getHeight().toMilliInches();

      // adjusted width and/or height, respecting anchor and ensuring that neither dimension is reduced to less than
      // 10% of its current size. Again in milli-inches.
      double wAdjMI = wMI;
      double hAdjMI = hMI;
      switch(anchor)
      {
      case NORTH : hAdjMI += 2*dy; break;
      case SOUTH : hAdjMI -= 2*dy; break;
      case WEST  : wAdjMI -= 2*dx; break;
      case EAST  : wAdjMI += 2*dx; break;
      case NORTHWEST : hAdjMI += 2*dy; wAdjMI -= 2*dx; break;
      case NORTHEAST : hAdjMI += 2*dy; wAdjMI += 2*dx; break;
      case SOUTHWEST : hAdjMI -= 2*dy; wAdjMI -= 2*dx; break;
      case SOUTHEAST :
      case ANY :       hAdjMI -= 2*dy; wAdjMI += 2*dx; break;
      }
      if(wAdjMI < 0.1*wMI) wAdjMI = 0.1*wMI;
      if(hAdjMI < 0.1*hMI) hAdjMI = 0.1*hMI;

      // nothing to do if there was no change
      if(wAdjMI == wMI && hAdjMI == hMI) return;
      
      // convert the updated width and/or height from milli-inches to its current units of measure
      Measure.Constraints c = FGraphicModel.getSizeConstraints(getNodeType());
      Measure oldW = null;
      Measure updW = null;
      if(wAdjMI != wMI)
      {
         oldW = getWidth();
         Measure.Unit u = oldW.getUnits();
         updW = Measure.getConstrainedRealMeasure(wAdjMI, u, c);
      }
      Measure oldH = null;
      Measure updH = null;
      if(hAdjMI != hMI)
      {
         oldH = getHeight();
         Measure.Unit u = oldH.getUnits();
         updH = Measure.getConstrainedRealMeasure(hAdjMI, u, c);
      }

      // update the width and/or height. It's a little more complicated when both change at once.
      if(updH == null)
         setWidth(updW);
      else if(updW == null)
         setHeight(updH);
      else
      {
         setNotificationsEnabled(false);
         try
         {
            setWidth(updW);
            setHeight(updH);
         }
         finally { setNotificationsEnabled(true); }
         
         onNodeModified(null);
         FGNRevEdit.postBoxResize(this, null, null, oldW, oldH);
      }
   }

   /**
    * The local rendering coordinate system of a shape node is defined by the shape's bounding box, which is located
    * with respect to the parent viewport. The origin in the local coordinate system lies at the bottom-left corner of 
    * the box. The transformation from this local coordinate system to that of the parent viewport is complicated by the
    * fact that the bounding box is located by its center point -- NOT its bottom-left corner as is the case for most 
    * other elements -- and it may be optionally rotated about that point. Thus, the local-to-parent transformation may 
    * involve two translations sandwiching a rotation! If the shape has zero width or height, then it is not rendered, 
    * and this method returns the identity transform.
    */
   @Override public AffineTransform getLocalToParentTransform()
   {
      AffineTransform identity = new AffineTransform();

      // use identity transform if shape has zero width or height (nothing will be rendered anyway!)
      double wMI = getWidth().toMilliInches();
      double hMI = getHeight().toMilliInches();
      if(wMI <= 0.0 || hMI <= 0.0) return(identity);

      // get location of shape's center point WRT parent viewport, in thousandth-in
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(identity);
      Point2D centerPt = parentVP.toMilliInches(getX(), getY());
      if(centerPt == null) return(identity);

      // translate center pt so that it is in coord system of parent viewport
      AffineTransform at = AffineTransform.getTranslateInstance(centerPt.getX(), centerPt.getY());

      // if necessary, rotate about the center point
      double rot = getRotate();
      if(rot != 0) at.rotate(Math.toRadians(rot));

      // move origin from center of bounding box to bottom-left corner
      at.translate(-wMI/2, -hMI/2);

      return(at);
   }

   /**
    * The viewport of a shape node is defined by the bounding box within which the shape is centered. The viewport does 
    * NOT support "user" units; user units will be interpreted as thousandth-inches. If the shape's width or height is
    * zero, null is returned, since nothing can be rendered in this case.
    */
   @Override public FViewport2D getViewport()
   {
      double wMI = getWidth().toMilliInches();
      double hMI = getHeight().toMilliInches();
      return((wMI <= 0.0 || hMI <= 0.0) ? null : new FViewport2D(wMI, hMI));
   }


   /**
    * The painter that renders this shape node IAW its current definition. The painter is configured to draw WRT the 
    * node's local painting viewport (origin at bottom-left corner of shape's bounding box, no rotation required).
    */
   private ShapePainter painter = null;

   /** Location provider for the shape node's internal painter. Contains a single point. */
   private List<Point2D> painterLoc = null;

   /**
    * Update the internal cached painter that renders this shape node. The painter is configured to draw the shape and 
    * the auto-generated label into a graphics context that represents the shape's local viewport in conventional 
    * <i>DataNav</i> painting coordinates (origin at bottom-left corner of shape's bounding box, no rotation required).
    */
   private void updatePainter()
   {
      // painter is lazily constructed
      if(painter == null)
      {
         painter = new ShapePainter();
         painter.setStyle(this);
         painterLoc = new ArrayList<Point2D>(1);
         painterLoc.add(new Point2D.Double());
         painter.setLocationProducer(painterLoc);
      }

      // if shape type is null, then set up painter w/ a zero-size shape (else it draws a circle!)
      double w = getWidth().toMilliInches();
      double h = getHeight().toMilliInches();
      if(type == null) { w = 0; h = 0; }
      
      painter.setPaintedShape(type);
      painter.setBackgroundFill(bkgFill);
      painter.setSize((float)w, (float)h);
      painter.setTextLabel(getAttributedTitle(true));
      painterLoc.get(0).setLocation(w/2, h/2);
   }

   @Override protected void releaseRenderResourcesForSelf()
   {
      painter = null;
      if(painterLoc != null)
      {
         painterLoc.clear();
         painterLoc = null;
      }
   }

   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || painter == null)
      {
         updatePainter();
         painter.updateFontRenderContext(g2d);
         painter.invalidateBounds();
      }
      return(painter.getBounds2D(null));
   }

   /** If this shape node is zero size or its center point is ill-defined, then it and its children are not rendered. */
   @Override protected boolean isRendered()
   {
      if(getWidth().toMilliInches() <= 0.0 || getHeight().toMilliInches() <= 0.0) return(false);
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(false);
      Point2D centerPt = parentVP.toMilliInches(getX(), getY());
      if(!Utilities.isWellDefined(centerPt)) return(false);

      return(true);
   }

   /**
    * After transforming the graphics context so that the current origin lies at the bottom-left corner of the 
    * shape's bounding box, an internal painter draws the shape centered in this box, with its auto label (if any). 
    * Then any children are rendered in the shape's viewport.
    */
   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(!(isRendered() && needsRendering(task)))
         return(true);

      // get shape's center pt and size in milli-in wrt parent viewport. There's nothing to render if the center pt is 
      // ill-defined or the size is zero.
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(true);
      Point2D centerPt = parentVP.toMilliInches(getX(), getY());
      if(!Utilities.isWellDefined(centerPt)) return(true);
      double wMI = getWidth().toMilliInches();
      double hMI = getHeight().toMilliInches();
      if(wMI <= 0.0 || hMI <= 0.0) return(true);

      // get orientation of shape in parent viewport
      double rot = getRotate();

      // make sure internal painter is there
      if(painter == null) updatePainter();

      // render shape and children in a copy of the graphics context, so we do not alter the original
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         // transform graphics context to the shape's viewport.
         g2dCopy.translate(centerPt.getX(), centerPt.getY());
         if(rot != 0) g2dCopy.rotate(Math.toRadians(rot));
         g2dCopy.translate(-wMI/2, -hMI/2);

         // render the shape and its auto label in the shape's viewport, using a ShapePainter
         painter.render(g2dCopy, null);

         // now render any children
         if(!renderSubordinates(g2dCopy, task))
            return(false);
      }
      finally { if(g2dCopy != null) g2dCopy.dispose(); }

      return((task == null) ? true : task.updateProgress());
   }

   
   //
   // PSTransformable implementation
   //

   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      double wMI = getWidth().toMilliInches();
      double hMI = getHeight().toMilliInches();
      Point2D ctrPt = parentVP.toMilliInches(getX(), getY());
      if(wMI <= 0 || hMI <= 0 || !Utilities.isWellDefined(ctrPt)) return;

      psDoc.startElement(this);

      // establish a new viewport for the shape and its children. The shape must be rotated about its center point, so 
      // we must do a second translation after setViewport() to move the origin to the bottom-left corner of the shape's 
      // bounding box. The new viewport does NOT clip its content.
      double rot = getRotate();
      if(rot != 0)
      {
         psDoc.setViewport(ctrPt, wMI, hMI, rot, false);
         psDoc.translateAndRotate(new Point2D.Double(-wMI/2,-hMI/2), 0);
      }
      else
      {
         ctrPt.setLocation(ctrPt.getX()-wMI/2, ctrPt.getY()-hMI/2);
         psDoc.setViewport(ctrPt, wMI, hMI, 0, false);
      }

      // draw the shape, stroked IAW stroke properties, and filled IAW background fill IF the shape is closed.
      psDoc.renderAdornmentEx(type, wMI/2.0, hMI/2.0, wMI, hMI, 0, null, bkgFill);
      
      // if there's a label, render it centered in H & V at shape's center. We do this with renderText() to support
      // attributed text in the label.
      String label = getTitle().trim();
      if((label.length() > 0) && (getFillColor().getAlpha() > 0))
          psDoc.renderText(label, wMI/2.0, hMI/2.0, TextAlign.CENTERED, TextAlign.CENTERED);
      
      for(int i=0; i<getChildCount(); i++) 
         getChildAt(i).toPostscript(psDoc);
      psDoc.endElement();
   }

   
   //
   // Object
   //
   
   /** Override ensures that rendering infrastructure for the clone is independent of this shape node. */
   @Override protected Object clone()
   {
      ShapeNode copy = (ShapeNode) super.clone();
      copy.painter = null;
      copy.painterLoc = null;
      return(copy);
   }
}
