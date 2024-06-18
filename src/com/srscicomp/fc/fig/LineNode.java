package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

import com.srscicomp.common.g2dutil.ResizeAnchor;
import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;

/**
  * <code>LineNode</code> encapsulates an arbitrary line segment in the <em>DataNav</em> graphic model. It is a general 
 * purpose graphic element that can appear as the child of a figure, a graph, or even another line segment.
 * 
 * <p>A <code>LineNode</code> can parent any combination of text labels, shapes, and other lines. Shapes are typically
 * used to place markers or endcaps at the endpoints, but they can be placed anywhere WRT the line. All child nodes are 
 * located WRT the line segment's local rendering coordinate system, which is somewhat contrived because a line is 
 * really one-dimensional. The line segment's endpoints are given by four <code>Measure</code>d coordinates: <em>(x,y) 
 * (x2,y2)</em>. Let <em>L</em> be the line segment's length. The origin of the local rendering viewport lies <em>L</em>
 * units below the line segment's starting point <em>(x,y)</em>, on a line perpendicular to the line segment. IAW 
 * <em>DataNav</em> coordinate system conventions, the x-axis increases rightward and the y-axis increases upward. Thus, 
 * the <code>LineNode</code>'s endpoints lie at (0,L) and (L,L) in its own local coordinate system. The bounding box for 
 * the <code>LineNode</code> is an LxL square, which is important when locating nodes in percentage coordinates. Eg, to 
 * place a text label to the right of the line segment, one might set the label's location to (100%, 100%).</p>
 * 
 * <p>[<em>Note</em>: It would seem more intuitive if the line lay along the bottom edge of the bounding box. However, 
 * the top edge was the convention used in <em>DataNav</em> from the beginning, when it used the SVG-based Batik engine 
 * to do rendering. In SVG, the y-axis increased downward and the typical bounding-box origin was in the top-left 
 * rather than the bottom-left corner.]</p>
 * 
* @author 	sruffner
 */
public class LineNode extends FGraphicNode implements Cloneable
{
   /**
    * Construct a <code>LineNode</code> representing a horizontal line segment extending from (25%, 50%) to (75%, 50%) 
    * in the parent's viewport. All style attributes are initially implicit, and the node contains no children.
    */
   public LineNode()
   {
      super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR|HASLOCATTRS);
      setX(new Measure(25, Measure.Unit.PCT));
      setY(new Measure(50, Measure.Unit.PCT));
      x2 = new Measure(75, Measure.Unit.PCT);
      y2 = new Measure(50, Measure.Unit.PCT);
   }

   
   //
   // LineNode properties
   //

   /** The x-coordinate of the line's second endpoint WRT the parent viewport. */
   private Measure x2;

   /**
    * Get the x-coordinate of this line's second endpoint WRT its parent viewport. Use {@link #getX()} to retrieve the 
    * x-coordinate for the first endpoint.
    * 
    * @return The second endpoint's x-coordinate with associated units of measure.
    */
   public Measure getX2() { return(x2); }

   /**
    * Set the x-coordinate of this line's second endpoint WRT its parent viewport. If a change occurs, {@link 
    * #onNodeModified} is invoked. Use {@link #setX} to set the x-coordinate for the first endpoint.
    * 
    * @param m The new x-coordinate. It is constrained to satisfy {@link FGraphicModel#getLocationConstraints}. A null
    * value is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setX2(Measure m)
   {
      if(m == null) return(false);
      m = FGraphicModel.getLocationConstraints(getNodeType()).constrain(m);
 
      if((x2 != m) && !Measure.equal(x2, m))
      {
         if(doMultiNodeEdit(FGNProperty.X2, m)) return(true);
         
         Measure oldX2 = x2;
         x2 = m;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.X2);
            FGNRevEdit.post(this, FGNProperty.X2, x2, oldX2);
         }
      }
      return(true);
   }

   /** The y-coordinate of the line's second endpoint WRT the parent viewport. */
   private Measure y2;

   /**
    * Get the y-coordinate of this line's second endpoint WRT its parent viewport. Use {@link #getY()} to  retrieve the 
    * y-coordinate for the first endpoint.
    * 
    * @return The second endpoint's y-coordinate with associated units of measure.()
    */
   public Measure getY2() { return(y2); }

   /**
    * Set the y-coordinate of this line's second endpoint WRT its parent viewport. If a change occurs, {@link 
    * #onNodeModified} is invoked. Use {@link #setY} to set the y-coordinate for the first endpoint.
    * 
    * @param m The new y-coordinate. It is constrained to satisfy {@link FGraphicModel#getLocationConstraints}. A null
    * value is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setY2(Measure m)
   {
      if(m == null) return(false);
      m = FGraphicModel.getLocationConstraints(getNodeType()).constrain(m);
 
      if((y2 != m) && !Measure.equal(y2, m))
      {
         if(doMultiNodeEdit(FGNProperty.Y2, m)) return(true);
         
         Measure oldY2 = y2;
         y2 = m;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.Y2);
            FGNRevEdit.post(this, FGNProperty.Y2, y2, oldY2);
         }
      }
      return(true);
   }

   /**
    * Calculate the length of the line segment in milli-inches WRT the <code>LineNode</code>'s parent viewport. If the 
    * node is not located in a parent viewport, the line length is undefined.
    * 
    * @return Length of line in milli-inches, or -1 if the line length is currently undefined.
    */
   private double getLengthInMilliInches()
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(-1);
      Point2D p1 = parentVP.toMilliInches(getX(), getY());
      Point2D p2 = parentVP.toMilliInches(x2, y2);
      if( !(Utilities.isWellDefined(p1) && Utilities.isWellDefined(p2)) ) return(-1);

      return(p1.distance(p2));
   }

   @Override protected void rescaleSelf(double scale, MultiRevEdit undoer)
   {
      Measure.Constraints c = FGraphicModel.getLocationConstraints(getNodeType());
      Measure m = getX2();
      if(!m.isRelative())
      {
         x2 = new Measure(m.getValue()*scale, m.getUnits());
         x2 = c.constrain(m);
         undoer.addPropertyChange(this, FGNProperty.X2, x2, m);
      }

      m = getY2();
      if(!m.isRelative())
      {
         y2 = new Measure(m.getValue()*scale, m.getUnits());
         y2 = c.constrain(m);
         undoer.addPropertyChange(this, FGNProperty.Y2, y2, m);
      }
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok;
      switch(p)
      {
         case X2 : ok = setX2((Measure) propValue); break;
         case Y2 : ok = setY2((Measure) propValue); break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }
   
   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value;
      switch(p)
      {
         case X2 : value = getX2(); break;
         case Y2 : value = getY2(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }

   
   // 
   // Support for style sets
   //
   
   /**
    * The only style properties included a line segment node's style set are the generic text/draw styles. Since these
    * are handled by the base class already, we only have to enable the style set feature via this override.
    */
   @Override public boolean supportsStyleSet()  { return(true); }

   //
   // Support for child nodes
   //

   @Override
   public boolean canInsert(FGNodeType nodeType)
   {
      return(nodeType == FGNodeType.LABEL || nodeType == FGNodeType.SHAPE || nodeType == FGNodeType.LINE);
   }

   @Override
   public FGNodeType getNodeType() {  return(FGNodeType.LINE); }


   //
   // Alignment support
   //
   
   /** 
    * A line node can be aligned with other nodes. The line's endpoints are both adjusted to achieve the desired 
    * alignment without changing the line's length or orientation.
    */
   @Override boolean canAlign() { return(true); }

   /**
    * The line's two endpoints are transformed to figure coordinates and examined to determine the alignment locus. If 
    * the points cannot be transformed for whatever reason, null is returned. The line's stroke width and any children
    * are NOT take into account.
    */
   @Override Double getAlignmentLocus(FGNAlign align)
   {
      if(align == null || !canAlign()) return(null);
      
      // transform line endpoints into figure-level ("global") coordinates, in milli-inches
      Measure x1 = getX();
      Measure y1 = getY();
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(null);
      Point2D p1 = parentVP.toMilliInches(x1, y1);
      Point2D p2 = parentVP.toMilliInches(x2, y2);
      if(p1 == null || p2 == null) return(null);
      AffineTransform at = getParent().getLocalToGlobalTransform();
      at.transform(p1, p1);
      at.transform(p2, p2);
      if(!Utilities.isWellDefined(new Point2D[] {p1, p2})) return(null);
      
      // alignment locus is a function of the alignment type and the endpoint coordinates
      double locus = 0;
      switch(align)
      {
      case LEFT : locus = Math.min(p1.getX(), p2.getX()); break;
      case RIGHT : locus = Math.max(p1.getX(), p2.getX()); break;
      case BOTTOM : locus =  Math.min(p1.getY(), p2.getY()); break;
      case TOP : locus =  Math.max(p1.getY(), p2.getY()); break;
      case HCENTER : locus = (p1.getX() + p2.getX()) / 2.0; break;
      case VCENTER : locus = (p1.getY() + p2.getY()) / 2.0; break;
      }
      return(locus);
   }

   /**
    * The desired alignment is achieved by adjusting the X or Y coordinates of both endpoints by the same amount in 
    * order to preserve the line segment's length and orientation.
    */
   @Override boolean align(FGNAlign alignType, double locus, MultiRevEdit undoer)
   {
      if(alignType == null || undoer == null || (!Utilities.isWellDefined(locus)) || !canAlign()) return(false);
      
      // transform line endpoints into figure-level ("global") coordinates, in milli-inches
      Measure x1 = getX();
      Measure y1 = getY();
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(false);
      Point2D p1 = parentVP.toMilliInches(x1, y1);
      Point2D p2 = parentVP.toMilliInches(x2, y2);
      if(p1 == null || p2 == null) return(false);
      AffineTransform at = getParent().getLocalToGlobalTransform();
      at.transform(p1, p1);
      at.transform(p2, p2);
      if(!Utilities.isWellDefined(new Point2D[] {p1, p2})) return(false);
      

      // adjust the X or Y coordinates of both endpoints to achieve the desired alignment
      double dx = 0;
      double dy = 0;
      switch(alignType)
      {
      case LEFT: dx = (p1.getX() <= p2.getX()) ? locus-p1.getX() : locus-p2.getX(); break;
      case RIGHT: dx = (p1.getX() >= p2.getX()) ? locus-p1.getX() : locus-p2.getX(); break;
      case BOTTOM: dy = (p1.getY() <= p2.getY()) ? locus-p1.getY() : locus-p2.getY(); break;
      case TOP: dy = (p1.getY() >= p2.getY()) ? locus-p1.getY() : locus-p2.getY(); break;
      case HCENTER: dx = locus - (p1.getX() + p2.getX()) / 2.0; break;
      case VCENTER: dy = locus - (p1.getY() + p2.getY()) / 2.0; break;
      }
      p1.setLocation(p1.getX() + dx, p1.getY() + dy);
      p2.setLocation(p2.getX() + dx, p2.getY() + dy);
            
      // transform the endpoints back to parent viewport
      try { at = at.createInverse(); }
      catch( NoninvertibleTransformException nte ) { return(false); }
      at.transform(p1, p1);
      at.transform(p2, p2);
      if(!Utilities.isWellDefined(new Point2D[] {p1, p2})) return(false);
      
      // get endpoint coordinates in their original units of measure
      Measure.Constraints c = FGraphicModel.getLocationConstraints(getNodeType());
      Measure[] coords1 = parentVP.fromMilliInches(p1.getX(), x1.getUnits(), p1.getY(), y1.getUnits(), 
            c.nMaxFracDigits);
      if(coords1 == null) return(false);
      Measure[] coords2 = parentVP.fromMilliInches(p2.getX(), x2.getUnits(), p2.getY(), y2.getUnits(), 
            c.nMaxFracDigits);
      if(coords2 == null) return(false);
      
      coords1[0] = c.constrain(coords1[0], true);
      coords1[1] = c.constrain(coords1[1], true);
      coords2[0] = c.constrain(coords2[0], true);
      coords2[1] = c.constrain(coords2[1], true);
      
      // update any altered property values and record the changes in the reversible edit object supplied
      setNotificationsEnabled(false);
      boolean changed = false;
      try
      {
         if(!Measure.equal(x1, coords1[0]))
         {
            changed = true;
            setX(coords1[0]);
            undoer.addPropertyChange(this, FGNProperty.X, coords1[0], x1);
         }
         if(!Measure.equal(y1, coords1[1]))
         {
            changed = true;
            setY(coords1[1]);
            undoer.addPropertyChange(this, FGNProperty.Y, coords1[1], y1);
         }
         if(!Measure.equal(x2, coords2[0]))
         {
            changed = true;
            Measure old = x2;
            x2 = coords2[0];
            undoer.addPropertyChange(this, FGNProperty.X2, x2, old);
         }
         if(!Measure.equal(y2, coords2[1]))
         {
            changed = true;
            Measure old = y2;
            y2 = coords2[1];
            undoer.addPropertyChange(this, FGNProperty.Y2, y2, old);
         }
      }
      finally { setNotificationsEnabled(true); }
      
      return(changed);
   }


   //
   // Renderable/Focusable support
   //

   /**
    * Interactive repositioning of a line is supported, but it requires adjusting both of the line's endpoints, 
    * represented by the four coordinate properties <em>(x, y), (x2, y2)</em>. The approach is the same as that 
    * described for the base-class implementation, except that two points are transformed instead of one.
    */
   @Override boolean executeMove(double dx, double dy, boolean notify)
   {
      if(!canMove()) return(false);

      // transform endpoints into figure-level ("global") coordinates
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(false);
      Point2D anchor1 = parentVP.toMilliInches(getX(), getY());
      if(anchor1 == null) return(false);
      Point2D anchor2 = parentVP.toMilliInches(getX2(), getY2());
      if(anchor2 == null) return(false);
      AffineTransform at = getParent().getLocalToGlobalTransform();
      at.transform(anchor1, anchor1);
      at.transform(anchor2, anchor2);

      // adjust endpoint locations IAW the specified offsets
      anchor1.setLocation(anchor1.getX() + dx, anchor1.getY() + dy);
      anchor2.setLocation(anchor2.getX() + dx, anchor2.getY() + dy);
      
      // transform new endpoint locations back to parent viewport
      try { at = at.createInverse(); }
      catch( NoninvertibleTransformException nte ) { return(false); }
      at.transform(anchor1, anchor1);
      at.transform(anchor2, anchor2);

      // get endpoint coordinates in their original units of measure
      Measure.Constraints c = FGraphicModel.getLocationConstraints(getNodeType());
      Measure[] coords1 = parentVP.fromMilliInches(anchor1.getX(), getX().getUnits(), 
            anchor1.getY(), getY().getUnits(), c.nMaxFracDigits);
      Measure[] coords2 = parentVP.fromMilliInches(anchor2.getX(), x2.getUnits(), 
            anchor2.getY(), y2.getUnits(), c.nMaxFracDigits);
      if(coords1 == null || coords2 == null) return(false);
      
      coords1[0] = c.constrain(coords1[0], true);
      coords1[1] = c.constrain(coords1[1], true);
      coords2[0] = c.constrain(coords2[0], true);
      coords2[1] = c.constrain(coords2[1], true);
      
      if(!(Measure.equal(coords1[0], getX()) && Measure.equal(coords1[1], getY()) &&
            Measure.equal(coords2[0], x2) && Measure.equal(coords2[1], y2)))
      {
         Measure oldX1 = getX();
         Measure oldY1 = getY();
         Measure oldX2 = x2;
         Measure oldY2 = y2;
         
         // update first endpoint; this is awkward because we don't have direct access to its coordinates. We don't
         // want the call to setXY() to post a reversible edit nor update the model, so turn notifications off.
         boolean toggleNotify = false;
         boolean ok;
         if(areNotificationsEnabled()) 
         {
            toggleNotify = true;
            setNotificationsEnabled(false);
         }
         try { ok = setXY(coords1[0], coords1[1]); }
         finally
         {
            if(toggleNotify) setNotificationsEnabled(true); 
         }
         if(!ok) return(false);
         
         // update second endpoint
         x2 = coords2[0];
         y2 = coords2[1];
         
         // update rendering and post reversible edit if requested.
         if(notify)
         {
            onNodeModified(null);
            FGNRevEdit.postMoveLine(this, oldX1, oldY1, oldX2, oldY2);   
         }
         return(true);
      }
      return(false);
   }

   /**
    * This method is invoked to undo or redo a reversible relocation of the line segment via {@link #executeMove}.
    * Since we're reversing or re-applying a previously applied edit, the change is NOT posted to the containing model's
    * edit history.
    * 
    * @param x X-coordinate for line segment's first endpoint. 
    * @param y Y-coordinate for line segment's first endpoint. 
    * @param x2 X-coordinate for line segment's second endpoint. 
    * @param y2 Y-coordinate for line segment's second endpoint. 
    */
   void undoOrRedoMove(Measure x, Measure y, Measure x2, Measure y2)
   {
      Measure currX2 = this.x2;
      Measure currY2 = this.y2;
      this.x2 = x2;
      this.y2 = y2;
      
      if(!areNotificationsEnabled())
      {
         if(!setXY(x, y))
         {
            this.x2 = currX2;
            this.y2 = currY2;
         }
         return;
      }
      
      setNotificationsEnabled(false);
      boolean ok;
      try { ok = setXY(x, y); }
      finally { setNotificationsEnabled(true); }
      
      if(!ok)
      {
         this.x2 = currX2;
         this.y2 = currY2;
         return;
      }
      onNodeModified(null);
   }
   
   /**
    * A line node supports interactive resize, but the resizing action equates to changing the position of one endpoint,
    * NOT changing the thickness of the line segment.
    */
   @Override public boolean canResize() { return(true); }

   /**
    * The resize rectangle for a line node is the rectangle that tightly bounds the stroked line segment, expanded
    * outwards by 25mi along the length of the line. If the line is zero length, the resize rectangle is undefined.
    */
   @Override Rectangle2D getResizeRectLocal()
   {
      double len = getLengthInMilliInches();
      if(len <= 0) return(null);
      
      double hsw = (isStroked() ? getStrokeWidth() : 0) / 2.0;
      double endAdj = (getStrokeEndcap() == StrokeCap.BUTT) ? 0 : hsw; 
      return(new Rectangle2D.Double(-endAdj, len-hsw-25, len + endAdj*2, hsw*2 + 50));
   }

   /**
    * A line node has just two anchors for interactive resize: the endpoints of the line segment. The resize interaction
    * simply changes the location of one endpoint while the other stays fixed. Method returns the non-directional resize
    * anchor if the specified point is close enough to one of the endpoints; else it returns null.
    */
   @Override public ResizeAnchor getResizeAnchor(Point2D p, double tol)
   {
      if(p == null) return(null);
      
      // current line length in milli-inches. If non-positive, can't define a resize anchor.
      double len = getLengthInMilliInches();
      if(len <= 0) return(null);
      
      // transform point to local coordinate system for comparison with local rendered bounding rectangle
      AffineTransform at = getLocalToGlobalTransform();
      try { at = at.createInverse(); } catch(NoninvertibleTransformException nte) { return(null); }
      at.transform(p, p);
      
      // in the line's awkward coordinate system, the endpoints are at 0,L and L,L
      return((p.distance(0, len) <= tol || p.distance(len, len) <= tol) ? ResizeAnchor.ANY : null);
   }


   /**
    * During an interactive drag resize operation, the resize outline is simply the line segment, with one endpoint at
    * the current drag location and the other endpoint unchanged. The method adjusts the endpoint that is closest to
    * the specified anchor point <i>p0</i>, favoring the second endpoint if they're equidistant. The resize anchor is
    * ignored, since a non-directional resize anchor should be used to resize a line node anyway.
    * <p>The resize cue reflects the change in the coordinates of the endpoint that is being moved during the resize
    * interaction.</p>
    */
   @Override public Shape getDragResizeShape(ResizeAnchor anchor, Point2D p0, Point2D p1, StringBuffer cueBuf)
   {
      double len = getLengthInMilliInches();
      if(len <= 0 || p0 == null || p1 == null) return(null);
      
      // transform anchor and drag points to local coordinate system
      AffineTransform at = getLocalToGlobalTransform();
      AffineTransform invAT;
      try { invAT = at.createInverse(); } catch(NoninvertibleTransformException nte) { return(null); }
      invAT.transform(p0, p0);
      invAT.transform(p1, p1);
      
      // we move one or the other endpoint to the location of the current drag point. The endpoint that is closer to
      // the drag origin stays fixed. The resulting shape simply connects the two endpoints.
      Line2D lineSeg;
      boolean isP1 = false;
      if(p0.distance(0,len) >= p0.distance(len,len))
         lineSeg = new Line2D.Double(0, len, p1.getX(), p1.getY());
      else
      {
         lineSeg = new Line2D.Double(p1.getX(), p1.getY(), len, len);
         isP1 = true;
      }
      
      // the resize cue is simply the updated location of the dragged endpoint, which is specified in the parent's
      // viewport.
      if(cueBuf != null)
      {
         cueBuf.setLength(0);
         
         getLocalToParentTransform().transform(p1, p1);
         
         // get location coordinates in their original units of measure, and limit precision in a reasonable way.
         FViewport2D parentVP = getParentViewport();
         boolean ok = (parentVP != null);
         if(ok)
         {
            Measure endPtX = (isP1) ? getX() : x2;
            Measure endPtY = (isP1) ? getY() : y2;
            Measure.Constraints c = FGraphicModel.getLocationConstraints(getNodeType());
            Measure[] coords = parentVP.fromMilliInches(p1.getX(), endPtX.getUnits(), p1.getY(), endPtY.getUnits(), 
                  c.nMaxFracDigits);
            ok = (coords != null);
            if(ok)
            {
               coords[0] = c.constrain(coords[0], true);
               coords[1] = c.constrain(coords[1], true);
               cueBuf.append(isP1 ? "P1=" : "P2=");
               cueBuf.append(coords[0].toString()).append(", ").append(coords[1].toString());
            }
         } 
      }
      
      return(at.createTransformedShape(lineSeg));
   }

   /**
    * Completes an interactive resize operation by changing the coordinates of the line segment endpoint that was 
    * dragged during the interaction.
    */
   @Override public void executeResize(ResizeAnchor anchor, Point2D p0, Point2D p1)
   {
      double len = getLengthInMilliInches();
      if(len <= 0 || p0 == null || p1 == null) return;
      
      // transform anchor and drag points to local coordinate system
      AffineTransform at = getLocalToGlobalTransform();
      AffineTransform invAT;
      try { invAT = at.createInverse(); } catch(NoninvertibleTransformException nte) { return; }
      invAT.transform(p0, p0);
      invAT.transform(p1, p1);
      
      // we move one or the other endpoint to the location of the current drag point. The endpoint that is closer to
      // the drag origin stays fixed. The resulting shape simply connects the two endpoints.
      boolean isP1 = (p0.distance(0,len) < p0.distance(len,len));
      
      // get location coordinates in their original units of measure, and limit precision in a reasonable way.
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;
      
      getLocalToParentTransform().transform(p1, p1);
      Measure oldX = (isP1) ? getX() : x2;
      Measure oldY = (isP1) ? getY() : y2;
      Measure.Constraints c = FGraphicModel.getLocationConstraints(getNodeType());
      Measure[] coords = parentVP.fromMilliInches(p1.getX(), oldX.getUnits(), p1.getY(), oldY.getUnits(), 
            c.nMaxFracDigits);
      if(coords == null) return;
      Measure updX = c.constrain(coords[0], true);
      Measure updY = c.constrain(coords[1], true);
      
      if(isP1)
      {
         boolean wasEna = areNotificationsEnabled();
         setNotificationsEnabled(false);
         try { setXY(updX, updY); }
         finally { if(wasEna) setNotificationsEnabled(true); }
      }
      else
      {
         x2 = updX;
         y2 = updY;
      }
      onNodeModified(null);
      FGNRevEdit.postLineResize(this, isP1, oldX, oldY);
   }

   @Override boolean undoOrRedoResize(HashMap<FGNProperty, Measure> props)
   {
      if(props == null) return(false);
      
      // the affected properties in a resize are X,Y for the first endpoint OR X2,Y2 for the second
      Measure oldX = props.get(FGNProperty.X);
      Measure oldY = props.get(FGNProperty.Y);
      Measure oldX2 = props.get(FGNProperty.X2);
      Measure oldY2 = props.get(FGNProperty.Y2);
      
      if(oldX != null && oldY != null && oldX2 == null && oldY2 == null)
      {
         boolean wasEna = areNotificationsEnabled();
         setNotificationsEnabled(false);
         try { setXY(oldX, oldY); }
         finally { if(wasEna) setNotificationsEnabled(true); }
      }
      else if(oldX == null && oldY == null && oldX2 != null && oldY2 != null)
      {
         x2 = oldX2;
         y2 = oldY2;
      }
      else
         return(false);
      
      if(areNotificationsEnabled()) onNodeModified(null);
      return(true);
   }

   /**
    * The painting coordinate system of a <code>LineNode</code> is defined by the line's endpoints, which are located 
    * with respect to the parent viewport. The origin is translated to the first endpoint, and the coordinate system is 
    * rotated by the line's orientation WRT the parent. Then the origin is translated one line-length downward, in 
    * keeping with the convention that the line segment itself lies at y=100% in "local" coordinates. If the line's 
    * endpoints are coincident (zero line length), then the <code>LineNode</code> and its children are not rendered and
    * this method returns the identity transform.
    * 
    * @see FGraphicNode#getLocalToParentTransform()
    */
   @Override
   public AffineTransform getLocalToParentTransform()
   {
      AffineTransform identity = new AffineTransform();

      // get line's endpoints, line length in milli-in with respect to the parent viewport. If endpoints are not 
      // well-defined or line length is zero, return identity (line and its children will not be rendered anyway).
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(identity);
      Point2D p1 = parentVP.toMilliInches(getX(), getY());
      Point2D p2 = parentVP.toMilliInches(x2, y2);
      if( !(Utilities.isWellDefined(p1) && Utilities.isWellDefined(p2)) ) return(identity);
      double lineLength = p1.distance(p2);
      if(lineLength <= 0) return(identity);

      // calculate orientation of line in parent viewport
      double thetaRad = Math.atan2( p2.getY()-p1.getY(), p2.getX()-p1.getX() );

      // translate origin so that it is in coord system of parent viewport
      AffineTransform at = AffineTransform.getTranslateInstance(p1.getX(), p1.getY());

      // rotate wrt parent if necessary
      if(thetaRad != 0) at.rotate(thetaRad);

      // finally, translate downward by one line-length
      at.translate(0, -lineLength);
 
      return(at);
   }

   /**
    * The viewport of a <code>LineNode</code> is somewhat nebulous since a line segment is one-dimensional. To make it 
    * easy to align children at any point along the line (eg, centering a text label under the line), we define the 
    * line's local viewport as follows:
    * <ul>
    *    <li>The bounding box is square, with side dimension equal to the line's length in milli-inches.</li>
    *    <li>The viewport does NOT support "user" units. User units are treated as milli-inches.</li>
    *    <li>The line itself lies along the top edge of the bounding box.  Thus, the line's starting point lies at 
    *    (0,L) and its ending point at (L,L) in viewport coordinates, where L is the line's real length in 
    *    milli-inches.</li>
    *    <li>IAW <em>DataNav</em> conventions for rendering coordinates, the x-axis increases rightward and the y-axis 
    *    increases upward.</li>
    *    <li>If the line's endpoints are coincident, the <code>LineNode</code> and its children will <em>not</em> be 
    *    rendered, and this method returns <code>null</code> -- the viewport is not well-defined in this case.</li>
    * </ul>
    * </p>
    * <p>This viewport can cause some confusion, since the origin does not lie at the line segment's first endpoint. 
    * For example, to center a text label child both vertically and horizontally WRT the line, the label's location in
    * <code>Measure</code>d coordinates should be set to (50%, 100%) -- since Y=100% corresponds to the top edge of the 
    * viewport, where the line is drawn.</p>
    * 
    * @see FGraphicNode#getViewport()
    */
   @Override
   public FViewport2D getViewport()
   {
      double linelength = getLengthInMilliInches();
      if(linelength <= 0.0) 
         return(null);
      else
         return(new FViewport2D(linelength, linelength));
   }

   /**
    * Since <code>LineNode</code> does not currently maintain any internal rendering resources, this method is a no-op.
    * 
    * @see FGraphicNode#releaseRenderResourcesForSelf()
    */
   @Override
   protected void releaseRenderResourcesForSelf() {}

   /**
    * The rendered bounds for a <code>LineNode</code> covers only the line itself, taking into account the current 
    * stroke width and end cap (there's an extra half stroke-width at each endpoint for the square and round caps). If 
    * the stroke width is zero, the bounding rectangle is empty!
    * 
    * @see FGraphicNode#getRenderBoundsForSelf(Graphics2D, boolean)
    */
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      // compute the rectangle that just covers the line itself -- drawn at the TOP of its viewport!
      Rectangle2D rLine = new Rectangle2D.Double();
      double strokeW = isStroked() ? getStrokeWidth() : 0;
      if(strokeW <= 0) return(rLine);
      double lineLength = getLengthInMilliInches();
      if(lineLength <= 0) return(rLine);

      double capAdj = (getStrokeEndcap() == StrokeCap.BUTT) ? 0 : strokeW/2.0;
      
      rLine.setRect(-capAdj,lineLength-strokeW/2, lineLength + 2*capAdj, strokeW);
      return(rLine);
   }

   /**
    * A <code>LineNode</code> and its children cannot be rendered if the line segment's current length is zero or not 
    * well-defined.
    * 
    * @see LineNode#getLengthInMilliInches()
    * @see FGraphicNode#isRendered()
    */
   @Override protected boolean isRendered()
   {
      return(getLengthInMilliInches() > 0);
   }

   /**
    * Let the line segment be well-defined with nonzero line length <em>L</em>. After transforming the graphics context 
    * so that the first endpoint of the line segment lies at <em>(0,L)</em> and the second endpoint at <em>(L,L)</em>, 
    * this method strokes the line segment IAW the current stroke styles, then renders any child nodes. If the line 
    * segment is not currently well-defined, nothing is rendered.
    * 
    * @see com.srscicomp.common.g2dviewer.Renderable#render(Graphics2D, RenderTask)
    */
   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(!(isRendered() && needsRendering(task)))
         return(true);

      // get line's endpoints and length in milli-in wrt parent viewport. There's nothing to render if either endpoint
      // is ill-defined or line length is zero.
      FViewport2D parentViewport = getParentViewport();
      if(parentViewport == null) return(true);
      Point2D p1 = parentViewport.toMilliInches(getX(), getY());
      Point2D p2 = parentViewport.toMilliInches(x2, y2);
      if( !(Utilities.isWellDefined(p1) && Utilities.isWellDefined(p2)) ) return(true);
      double lineLength = p1.distance(p2);
      if(lineLength <= 0) return(true);

      // calculate orientation of line in parent viewport
      double theta = Math.atan2( p2.getY()-p1.getY(), p2.getX()-p1.getX() );

      // render line and children in a copy of the graphics context, so we do not alter the original
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         // transform graphics context to the line's viewport.
         g2dCopy.translate(p1.getX(), p1.getY());
         g2dCopy.rotate(theta);
         g2dCopy.translate(0, -lineLength);

         // draw the line itself at y=L in its own viewport, unless it is not stroked
         if(isStroked()) 
         {
            g2dCopy.setColor( getStrokeColor() );
            g2dCopy.setStroke( getStroke(0) );
            g2dCopy.draw( new Line2D.Double(0, lineLength, lineLength, lineLength) );
         }

         // now render any children in the same viewport
         if(!renderSubordinates(g2dCopy, task))
            return(false);
      }
      finally { if(g2dCopy != null) g2dCopy.dispose(); }

      return(task == null || task.updateProgress());
   }

   
   //
   // PSTransformable implementation
   //
   
   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;
      Point2D p0 = parentVP.toMilliInches(getX(), getY());
      Point2D p1 = parentVP.toMilliInches(getX2(), getY2());
      if(p0 == null || p1 == null) return;
      
      // calc length and orientation of line in parent viewport; zero-length lines are not rendered
      double lineLength = p0.distance(p1);
      if(lineLength == 0.0) return;
      double theta = Math.toDegrees( Math.atan2( p1.getY()-p0.getY(), p1.getX()-p0.getX() ) );

      psDoc.startElement(this);

      // establish a new viewport for the line and its children:  a square viewport with size = the line's length and 
      // set up so that the line itself lies along the top edge. We have to translate to first endpoint, rotate, then 
      // translate down by one line-length to set things up correctly.
      psDoc.translateAndRotate(p0,theta);
      p0.setLocation(0,-lineLength);
      psDoc.translateAndRotate(p0, 0);
      
      // render the line itself at the top edge of the new viewport, unless it is not stroked
      if(isStroked()) 
      {
         p0.setLocation(0, lineLength);
         p1.setLocation(lineLength, lineLength);
         psDoc.renderLine(p0, p1);
      }

      for(int i=0; i<getChildCount(); i++) 
         getChildAt(i).toPostscript(psDoc);
      psDoc.endElement();
   }

   @Override public LineNode clone() throws CloneNotSupportedException
   {
      return (LineNode) super.clone();
   }
}
