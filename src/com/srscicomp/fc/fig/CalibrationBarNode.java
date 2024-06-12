package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

import com.srscicomp.common.g2dutil.LineSegmentPainter;
import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.Painter;
import com.srscicomp.common.g2dutil.PolylinePainter;
import com.srscicomp.common.g2dutil.SingleStringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;

/**
 * <code>CalibrationBarNode</code> encapsulates a calibration bar linked to either the primary or secondary axis of the 
 * parent graph; it makes sense only a child of a <code>GraphNode</code>. 
 * 
 * <p>In some 2D plots, authors prefer to draw a calibration bar near the edge of the graph rather than an axis. This 
 * can be accomplished using a generic <code>LineNode</code>} with an attached label. However, it would be useful to 
 * have the length of such a calibration bar expressed in the units of the associated coordinate axis so that, if the 
 * author resizes the graph, the calibration bar is automatically resized with it. <code>CalibrationBarNode</code> is 
 * defined with this auto-resizing feature in mind. It is essentially a specialized, decorated line that can only be a 
 * child of a <code>GraphNode</code>. Instead of locating the bar with two arbitrary endpoints, the author must 
 * specify the center of the bar, to which axis it is attached (which determines the bar's orientation, either vertical 
 * or horizontal), and its length in the implied units of that axis.</p>
 * 
 * <p>A calibration bar can contain any number of text labels. Such child <code>LabelNode</code>s are positioned with 
 * respect to the line that represents the bar in the same way that a text label is positioned with respect to a parent 
 * <code>LineNode</code>.</p>
 * 
 * <p><em>Auto-generated label</em>: If the <code>CalibrationBarNode</code>'s <em>autoLabel</em> property is enabled, 
 * the bar will auto-generate a label of the form "{len} {units}", where {len} is the length of the calibration bar in 
 * the same units as the corresponding axis and {units} is the value of that axis's "units" attribute. The auto-
 * generated label will always be centered along the bar, parallel to it, and located either above or below it such 
 * that the bar itself lies (if possible) between the parent graph's data box and the label.</p>
 * 
 * <p><em>Position and orientation</em>. A calibration bar is located within its parent graph by a pair of 
 * <code>Measure</code>'d coords <em>(x, y)</em>. If either coordinate is specified in "user" units, then the 
 * calibration bar is positioned WRT the graph's data coordinate system rather than physical dimensions of its data box.
 * The bar is associated with the primary axis of the parent graph if the <em>isPrimary</em> property is set; otherwise,
 * it is associated with the graph's secondary axis. The <em>length</em> property specifies the length of the 
 * calibration bar in the native units attached to the relevant graph axis.</p>
 * 
 * <p><em>Styling</em>. Like most other <em>DataNav</em> graphic objects, <code>CalibrationBarNode</code> possesses the 
 * usual text and draw style properties managed by the super-class. The text-related styles apply to the auto-generated 
 * label and can be passed on to any child <code>LabelNode</code>s, while the stroke styles define the appearance of 
 * the bar itself and its endpoint adornments. The type and size of the endpoint adornment is given by <em>endCap</em> 
 * and <em>endCapSize</em> properties.</p>
 * 
 * <p><em><strong>Special cases</strong></em>: When the parent graph is a polar plot, its primary (theta) axis is never 
 * rendered. Likewise, any primary calibration bars in the graph are not rendered. A secondary calibration bar, 
 * corresponding to the radial axis of the polar plot, will be drawn, but in the horizontal orientation. Calibration 
 * bars are not rendered when the associated axis is logarithmic.</p>
 * 
 * @author 	sruffner
 */
public class CalibrationBarNode extends FGraphicNode implements Cloneable
{
   /**
    * Construct a calibration bar linked to the primary axis of its parent graph, with length 10, auto-generated label 
    * enabled, and endcaps disabled (endcap size set to 0). The bar is initially located at (67%, -0.5in) WRT the 
    * parent graph viewport.
    */
   public CalibrationBarNode()
   {
      this(true);
   }

   /**
    * Construct a calibration bar linked to the specified axis of its parent graph, with length 10, auto-generated label 
    * enabled, and endcap symbol and size set IAW user-defined preferred values. The bar is initially located at 
    * (67%, -0.5in) WRT the parent graph viewport.
    * 
    * @param primary If <code>true</code>, calibration bar is linked to primary axis of parent graph; else, the 
    * secondary axis.
    */
   public CalibrationBarNode(boolean primary)
   {
      super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASLOCATTRS);
      setXY( new Measure(67, Measure.Unit.PCT), new Measure(-0.5, Measure.Unit.IN));
      length = 10;
      this.primary = primary;
      autoLabelOn = true;
      endCap = FGNPreferences.getInstance().getPreferredCalibCap();
      endCapSize = FGNPreferences.getInstance().getPreferredCalibCapSize();
   }

   
   // 
   // Support for child nodes
   //
   
   @Override
   public FGNodeType getNodeType()
   {
      return(FGNodeType.CALIB);
   }

   @Override
   public boolean canInsert(FGNodeType nodeType)
   {
      return(nodeType == FGNodeType.LABEL);
   }


   //
   // Properties
   //

   /**
    * The length of the calibration bar, in whatever units are attached to the corresponding axis in the parent graph.
    */
   private double length;

   /**
    * Get the length of the calibration bar.
    * 
    * @return Length of calibration bar, in same units as the graph axis to which it is linked.
    */
   public double getLength() { return(length); }
   
   /**
    * Set the length of the calibration bar. If a change is made, <code>onNodeModified()</code> is invoked.
    * 
    * @param New length of calibration bar, in same units as the graph axis to which it is linked. A negative value, 
    * NaN, or infinity is rejected.
    * @param <code>True</code> if successful; <code>false</code> if argument was rejected.
    */
   public boolean setLength(double d)
   {
      if(!Utilities.isWellDefined(d) || (d < 0)) return(false);
      if(length != d)
      {
         if(doMultiNodeEdit(FGNProperty.LEN, new Double(d))) return(true);

         Double old = new Double(length);
         length = d;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.LEN);
            FGNRevEdit.post(this, FGNProperty.LEN, new Double(length), old);
         }
      }
      return(true);
   }

   /**
    * If this flag is set, the calibration bar is associated with the primary axis of the parent graph; else, with the 
    * secondary axis.
    */
   private boolean primary;

   /**
    * Is this <code>CalibrationBarNode</code> linked to the primary axis of its parent graph?
    * 
    * <p>A primary calibration bar is rendered horizontal in a Cartesian graph, while a secondary calibration bar is 
    * vertical. A primary calibration bar is never rendered in a polar plot, while a secondary (corres. to radial axis) 
    * calibration bar is rendered horizontal bar. A calibration bar linked to a logaritmic axis is never rendered.</p>
    * 
    * @return <code>True</code> if this is a primary axis calibration bar; else, it's a secondary axis calibration bar.
    */
   public boolean isPrimary() { return(primary); }

   /**
    * Link this <code>CalibrationBarNode</code> to the primary or secondary axis of its parent graph. If a change is 
    * made, <code>onNodeModified()</code> is invoked.
    * 
    * @param b <code>True</code> to link bar to primary axis; <code>false</code> to link it to the secondary axis.
    * @see CalibrationBarNode#isPrimary()
    */
   public void setPrimary(boolean b)
   {
      if(primary != b)
      {
         if(doMultiNodeEdit(FGNProperty.PRIMARY, new Boolean(b))) return;
         
         Boolean old = new Boolean(primary);
         primary = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.PRIMARY);
            String desc = "Attach calibration bar to ";
            desc += primary ? "horizontal (theta) axis" : "vertical (radial) axis";
            FGNRevEdit.post(this, FGNProperty.PRIMARY, new Boolean(primary), old, desc);
         }
      }
   }

   /**
    * If this flag is set, the calibration bar's auto-generated label is rendered.
    */
   private boolean autoLabelOn;

   /**
    * Is this <code>CalibrationBarNode</code>'s auto-generated label turned on?
    * 
    * <p>If enabled, the auto label has the form "L U", where L is the bar's <em>length</em> property rounded to the 
    * nearest integer and U is the units token taken from the graph axis to which the bar is linked.</p>
    * 
    * @return <code>True</code> iff auto label is enabled.
    */
   public boolean isAutoLabelOn() { return(autoLabelOn); }

   /**
    * Turn this <code>CalibrationBarNode</code>'s auto-generated label on or off.
    * 
    * @param b <code>True</code> to turn on auto label; <code>false</code> to disable it.
    * @see CalibrationBarNode#isAutoLabelOn()
    */
   public void setAutoLabelOn(boolean b)
   {
      if(autoLabelOn != b)
      {
         if(doMultiNodeEdit(FGNProperty.AUTO, new Boolean(b))) return;
         
         Boolean old = new Boolean(autoLabelOn);
         autoLabelOn = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.AUTO);
            String desc = (autoLabelOn ? "Enable" : "Disable") + " calibration bar's auto-generated label";
            FGNRevEdit.post(this, FGNProperty.AUTO, new Boolean(autoLabelOn), old, desc);
         }
      }
   }

   /**
    * The type of shape adorning the endpoints of the calibration bar.
    */
   private Marker endCap;

   /**
    * Get the type of shape adorning the endpoints of this <code>CalibrationBarNode</code>. 
    * 
    * @return A <code>Marker</code> object that defines the endcap within a unit "design box". This unit shape is then 
    * scaled to the size returned by <code>getEndCapSize()</code>.
    */
   public Marker getEndCap() { return(endCap); }

   /**
    * Set the type of shape adorning the endpoints of this <code>CalibrationBarNode</code>. If a change is made, 
    * <code>onNodeModified()</code> is invoked.
    * 
    * @param cap A <code>Marker</code> specifying the new endcap shape. A <code>null</code> value is rejected. To avoid 
    * rendering endcaps, set the <em>endCapSize</em> property to 0.
    * @return <code>True</code> if successful; <code>false</code> if value was rejected.
    */
   public boolean setEndCap(Marker cap)
   {
      if(cap == null) return(false);
      if(endCap != cap)
      {
         if(doMultiNodeEdit(FGNProperty.CAP, cap)) return(true);
         
         Marker old = endCap;
         endCap = cap;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CAP);
            FGNRevEdit.post(this, FGNProperty.CAP, endCap, old);
         }
      }
      return(true);
   }

   /**
    * The size of the endcap adornment, with associated units of measure (only physical units allowed).
    */
   private Measure endCapSize;
   
   /**
    * Constrains the measured properties <i>endCapSize</i> to use physical units, to have a maximum of 4 significant
    * and 3 fractional digits,  and to lie in the range [0..1in], regardless what units are assigned to it.
    */
   public final static Measure.Constraints ENDCAPSIZECONSTRAINTS = new Measure.Constraints(0.0, 1000.0, 4, 3);

   /**
    * Get the size of the endcap adornments for this <code>CalibrationBarNode</code>.
    * 
    * @return The endcap adornment size, specified as a linear measurement with associated units.
    */
   public Measure getEndCapSize() { return(endCapSize); }

   /**
    * Set the size of the endcap adornments for this calibration bar. If a change is made, {@link #onNodeModified()}
    * is invoked.
    * 
    * <p>To prevent rendering of endcaps, use this method to set the endcap size to zero!</p>
    * 
    * @param m The new endcap adornment size. The measure will be constrained to satisfy {@link #ENDCAPSIZECONSTRAINTS}.
    * A null value is rejected. 
    * @return True if change was accepted; false if rejected.
    */
   public boolean setEndCapSize(Measure m)
   {
      if(m == null) return(false);
      m = ENDCAPSIZECONSTRAINTS.constrain(m);

      boolean changed = (endCapSize != m) && !Measure.equal(endCapSize, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.CAPSIZE, m)) return(true);
         
         Measure old = endCapSize;
         endCapSize = m;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CAPSIZE);
            FGNRevEdit.post(this, FGNProperty.CAPSIZE, endCapSize, old);
         }
      }
      return(true);
   }

   @Override protected void rescaleSelf(double scale, MultiRevEdit undoer)
   {
      Measure.Constraints c = ENDCAPSIZECONSTRAINTS;
      double d = endCapSize.getValue();
      if(d > 0)
      {
         Measure old = endCapSize;
         endCapSize = c.constrain(new Measure(d*scale, endCapSize.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.CAPSIZE, endCapSize, old);
      }
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
         case AUTO : setAutoLabelOn((Boolean)propValue); ok = true; break;
         case CAP : ok = setEndCap((Marker)propValue); break;
         case CAPSIZE: ok = setEndCapSize((Measure)propValue); break;
         case LEN: ok = setLength((Double)propValue); break;
         case PRIMARY: setPrimary((Boolean)propValue); ok = true; break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
         case AUTO : value = new Boolean(isAutoLabelOn()); break;
         case CAP : value = getEndCap(); break;
         case CAPSIZE: value = getEndCapSize(); break;
         case LEN: value =  new Double(getLength()); break;
         case PRIMARY: value = new Boolean(isPrimary()); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }

   
   // 
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   /**
    * The node-specific properties exported in a calibration bar node's style set are the end-cap type and size, and the
    * flag indicating whether or not a label is automatically generated for it.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.CAP, getEndCap());
      styleSet.putStyle(FGNProperty.CAPSIZE, getEndCapSize());
      styleSet.putStyle(FGNProperty.AUTO, new Boolean(isAutoLabelOn()));
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      Marker m = (Marker) applied.getCheckedStyle(FGNProperty.CAP, getNodeType(), Marker.class);
      if(m != null && !m.equals(restore.getStyle(FGNProperty.CAP)))
      {
         endCap = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.CAP);
      
      Measure sz = (Measure) applied.getCheckedStyle(FGNProperty.CAPSIZE, getNodeType(), Measure.class);
      if(sz != null) sz = ENDCAPSIZECONSTRAINTS.constrain(sz);
      if(sz != null && !Measure.equal(sz, (Measure) restore.getCheckedStyle(FGNProperty.CAPSIZE, null, Measure.class)))
      {
         endCapSize = sz;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.CAPSIZE);
      
      Boolean bAuto = (Boolean) applied.getCheckedStyle(FGNProperty.AUTO, getNodeType(), Boolean.class);
      if(bAuto != null && !bAuto.equals(restore.getStyle(FGNProperty.AUTO)))
      {
         this.autoLabelOn = bAuto.booleanValue();
         changed = true;
      }
      else restore.removeStyle(FGNProperty.AUTO);

      return(changed);
   }


   //
   // Derived properties based on context in graph
   //

   /**
    * Return a reference to the <code>GraphNode</code> containing this <code>CalibrationBarNode</code>. 
    * 
    * @return The parent <code>GraphNode</code>, or <code>null</code> if this node currently has no parent. A 
    * calibration bar may only appear as a child of a graph.
    */
   private GraphNode getParentGraph()
   {
      FGraphicNode p = getParent();
      if(p == null) return(null);
      assert(p instanceof GraphNode);
      return((GraphNode)p);
   }

   /**
    * Return the units token for the graph axis linked to this <code>CalibrationBarNode</code>.
    * 
    * @return Token describing the user units in which calibration bar is measured, as obtained from the relevant graph 
    * axis. May be an empty string if user has not assigned a units token to the axis, or if the calibration bar lacks 
    * a properly configured parent graph.
    */
   private String getUnitsToken()
   {
      GraphNode g = getParentGraph();
      if(g != null)
      {
         AxisNode axis = primary ? g.getPrimaryAxis() : g.getSecondaryAxis();
         if(axis != null)
            return(axis.getUnits());
      }
      return("");
   }

   /**
    * Get physical length of the calibration bar in milli-inches, given its current graph context.
    * 
    * <p>The method finds the real length of the associated graph axis and multiplies that by the ratio of the bar's 
    * length in user units divided by the axis's (validated) length in those same units. However, if the calibration bar 
    * currently lacks a valid parent graph, or if it is linked to the theta axis of a polar plot or to any logarithmic 
    * axis, the bar is not rendered at all. In these cases, the method returns 0.</p>
    * 
    * @return Physical length of calibration bar in milli-inches.  Returns 0 if calibration bar is not renderable in 
    * its current context.
    */
   private double getLengthInMilliInches()
   {
      GraphNode g = getParentGraph();
      if(g == null) return(0);

      AxisNode axis = primary ? g.getPrimaryAxis() : g.getSecondaryAxis();
      if(axis == null || axis.isLogarithmic() || axis.isTheta()) return(0);
      
      double axisLenUser = Math.abs(axis.getValidatedStart() - axis.getValidatedEnd());
      double axisLenMI = axis.getLengthInMilliInches();
      return(axisLenUser > 0 ? length * axisLenMI / axisLenUser : 0);
   }

   /**
    * Calculate locations of this <code>CalibrationBarNode</code>'s endpoints in the rendering coordinate system of its 
    * parent graph.
    * 
    * @return An array of two <code>Point2D</code>s containing the two endpoints, located in milli-inches in the parent 
    * graph's viewport. If the calibration bar's current context is ill-defined, the method returns <code>null</code>.
    */
   private Point2D[] getEndpointsInMilliInches() 
   {
      GraphNode g = getParentGraph();
      FViewport2D parentVP = getParentViewport();
      if(g == null || parentVP == null) return(null);

      // special case: a secondary calib assoc w/radial axis of polar plot is drawn horizontally
      boolean isHoriz = primary || g.isPolar();

      double lenMI = getLengthInMilliInches();
      Point2D p = parentVP.toMilliInches(getX(), getY());
      if(lenMI <= 0 || p == null)
         return(null);
      else
      {
         Point2D pEnd = new Point2D.Double();
         if(isHoriz)
         {
            double x0 = p.getX() - lenMI/2.0;
            p.setLocation(x0, p.getY());
            pEnd.setLocation(x0 + lenMI, p.getY());
         }
         else
         {
            double y0 = p.getY() - lenMI/2.0;
            p.setLocation( p.getX(), y0 );
            pEnd.setLocation( p.getX(), y0 + lenMI );
         }
         return( new Point2D[] {p, pEnd} );
      }
   }

   /**
    * Is this <code>CalibrationBarNode</code> above or to the left of the parent graph's data box, given its current 
    * context?
    * 
    * <p>A horizontal calibration bar (one attached to the primary axis, or to the secondary axis of a polar plot) is 
    * above the data box if its midpoint lies above the box. Similarly, a vertical calibration bar (one attached to the 
    * secondary axis, EXCEPT the secondary axis of a polar plot) is left of the data box if its midpoint lies left of 
    * the box. If the bar is currently ill-defined, the method returns <code>false</code></p>
    * 
    * @return <code>True</code> if bar is above or left of the graph's data box, as described.
    */
   private boolean isAboveOrLeft()
   {
      GraphNode g = getParentGraph();
      FViewport2D parentVP = getParentViewport();
      if(g == null || parentVP == null) return(false);
      Point2D midPt = parentVP.toMilliInches(getX(), getY());
      if(midPt == null) return(false);
      Rectangle2D rBox = g.getBoundingBoxLocal();

      // special case: a secondary calib assoc w/radial axis of polar plot is drawn horizontally
      boolean isHoriz = primary || g.isPolar();

      return(isHoriz ? (midPt.getY() >= rBox.getHeight()) : (midPt.getX() <= 0));
   }


   //
   // Alignment support
   //
   
   /** 
    * A calibration bar node can be aligned with other nodes The node's location coordinates are adjusted to achieve the
    * desired alignment, using the rectangle bounding the rendered bar to calculate the adjustment.
    */
   @Override boolean canAlign() { return(true); }

   //
   // Focusable/Renderable support
   //
   
   /**
    * The local rendering coordinate system of a <code>CalibrationBarNode</code> is analogous to that of a generic
    * <code>LineNode</code>. The bar's endpoints in the parent graph viewport are computed based upon its center 
    * point, length, and associated graph axis. The origin is translated to the first endpoint (left endpoint for a 
    * horizontal bar and bottom endpoint for a vertical one), and the coordinate system is rotated 90deg if the bar is 
    * vertical. Then, the origin is translated downward by the length L of the calibration bar, so that the bar itself
    * lies at y=L in the bar's local viewport -- in keeping with the convention for a <code>LineNode</code>. If the 
    * If the bar is not rendered in its current context, this method returns the identity transform.

    * @see FGraphicNode#getLocalToParentTransform()
    */
   @Override
   public AffineTransform getLocalToParentTransform()
   {
      double lenMI = getLengthInMilliInches();
      Point2D[] pts = getEndpointsInMilliInches();
      if(lenMI <= 0 || pts == null) return( new AffineTransform() );

      AffineTransform at = AffineTransform.getTranslateInstance(pts[0].getX(), pts[0].getY());
      double thetaRad = Math.atan2(pts[1].getY() - pts[0].getY(), pts[1].getX() - pts[0].getX());
      if(thetaRad != 0) at.rotate(thetaRad);
      at.translate(0, -lenMI);
      return( at );
   }

   /**
    * The local rendering viewport of a <code>CalibrationBarNode</code> is defined in the same manner as that of a 
    * generic <code>LineNode</code>.

    * @see FGraphicNode#getViewport()
    * @see LineNode#getViewport()
    */
   @Override
   public FViewport2D getViewport()
   {
      double lenMI = getLengthInMilliInches();
      return((lenMI <= 0) ? null : new FViewport2D(lenMI, lenMI));
   }

   @Override
   protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null)
      {
         if(rBoundsSelf == null) 
            rBoundsSelf = new Rectangle2D.Double();

         if(!isRendered()) 
            rBoundsSelf.setRect(0, 0, 0, 0);
         else
         {
            updatePainters();
            labelPainter.updateFontRenderContext(g2d);
            labelPainter.invalidateBounds();
            barPainter.invalidateBounds();
            barPainter.getBounds2D(rBoundsSelf);
            Utilities.rectUnion(rBoundsSelf, labelPainter.getBounds2D(null), rBoundsSelf);
         }
      }
      return((Rectangle2D) rBoundsSelf.clone());
   }

   /**
    * A <code>CalibrationBarNode</code> is rendered iff its effective length is strictly positive, given the current 
    * parent graph context.
    * 
    * @see FGraphicNode#isRendered()
    */
   @Override protected boolean isRendered()
   {
      return(getLengthInMilliInches() > 0);
   }

   /* (non-Javadoc)
    * @see com.srscicomp.common.g2dviewer.Renderable#render(java.awt.Graphics2D, com.srscicomp.common.g2dviewer.RenderTask)
    */
   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(!(isRendered() && needsRendering(task)))
         return(true);

      // get properties we need to transform parent graph viewport to our local rendering viewport
      double lenMI = getLengthInMilliInches();
      Point2D[] pts = getEndpointsInMilliInches();
      boolean isHoriz = primary || getParentGraph().isPolar();
      
      // make sure our painters have been initialized
      if(barPainter == null) updatePainters();

      // render calibration bar and its children in a copy of the graphics context, so we do not alter the original
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         g2dCopy.translate(pts[0].getX(), pts[0].getY());
         if(!isHoriz) g2dCopy.rotate(Math.PI/2);
         g2dCopy.translate(0, -lenMI);

         barPainter.render(g2dCopy, null);

         if(!renderSubordinates(g2dCopy, task))
            return(false);

         // when exporting to PDF, must handle font substitution in case chosen font does not handle one or more 
         // characters in the label string
         if(PDFSupport.isPDFGraphics(g2dCopy))
         {
            String labelStr = Utilities.toString(length, 6, 3) + " " + getUnitsToken();
            AttributedString aStr = PDFSupport.getInstance().doFontSubstitutionIfNeeded(labelStr, getFont());
            if(aStr != null) labelPainter.setText(aStr);
         }
         labelPainter.render(g2dCopy, null);

      }
      finally {if(g2dCopy != null) g2dCopy.dispose(); }

      return((task == null) ? true : task.updateProgress());
   }

   @Override
   protected void releaseRenderResourcesForSelf()
   {
      rBoundsSelf = null;
      barPainter = null;
      if(barLocs != null) barLocs.clear();
      barLocs = null;
      labelPainter = null;
   }


   //
   // Internal rendering resources
   //

   /**
    * Cached rectangle bounding only the marks made by this <code>CalibrationBarNode</code> -- not including the 
    * contributions of any child nodes. If <code>null</code>, the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;

   /**
    * This painter -- which may be a <code>PolylinePainter</code> or <code>LineSegmentPainter</code> -- renders the 
    * calibration bar itself, including any endpoint adornments.
    */
   private Painter barPainter = null;

   /**
    * The list of locations defining the calibration bar itself, in the bar's own painting coordinates.
    */
   private List<Point2D> barLocs = null;

   /** This painter renders the calibration bar's automated text label, if it has one. */
   private SingleStringPainter labelPainter = null;

   /**
    * Update the painters configured to draw the calibration bar -- possibly with endcap adornments and the auto label
    * -- in the bar's own local viewport, given the current defn of this <code>CalibrationBarNode</code>. 
    * 
    * <p>If the endcap is <code>Marker.LINEUP</code> or <code>Marker.LINEDOWN</code>, then it is best to use a 
    * <code>PolylinePainter</code> which draws the bar and the endcaps in one go, achieving a nice join. This is 
    * possible because the endcaps are rendered with the same draw styles as the bar itself. In all other cases, a 
    * a <code>LineSegmentPainter</code> renders the bar with its endcaps.</p>
    * 
    * <p>A <code>StringPainter</code> is used to render the auto-generated label. If the label is disabled, this painter 
    * will make no marks during rendering.</p>
    */
   private void updatePainters()
   {
      // get calibration bar properties we need. 
      double lenMI = getLengthInMilliInches();
      double capSizeMI = endCapSize.toMilliInches();
      boolean doAdorn = (capSizeMI > 0);
      boolean isLineup = (endCap == Marker.LINEUP);
      boolean isLinedown = (endCap == Marker.LINEDOWN);

      // STEP 1: PolylinePainter or LineSegmentPainter to render bar itself
      //
      // prepare the list of points which we give to the painter. Will be empty if bar not rendered.
      if(barLocs == null) barLocs = new ArrayList<Point2D>();
      if(lenMI <= 0 || !(isStroked() || doAdorn)) 
         barLocs.clear();
      else if(isLineup || isLinedown)
      {
         while(barLocs.size() < 4) barLocs.add( new Point2D.Double(0,0) );
         
         double yLineCap = (isLineup ? 1 : -1) * capSizeMI/2;
         barLocs.get(0).setLocation(0, lenMI + yLineCap);
         barLocs.get(1).setLocation(0, lenMI);
         barLocs.get(2).setLocation(lenMI, lenMI);
         barLocs.get(3).setLocation(lenMI, lenMI + yLineCap);
      }
      else
      {
         while(barLocs.size() < 2) barLocs.add( new Point2D.Double(0,0) );
         while(barLocs.size() > 2) barLocs.remove(0);

         barLocs.get(0).setLocation(0, lenMI);
         barLocs.get(1).setLocation(lenMI, lenMI);
      }

      // painter is lazily constructed. We may have to switch from PolylinePainter to LineSegmentPainter, or vice versa.
      if( barPainter == null || 
            ((isLineup || isLinedown) && !(barPainter instanceof PolylinePainter)) ||
            (!(isLineup || isLinedown) && (barPainter instanceof PolylinePainter)) )
      {
         if(barPainter != null)
         {
            barPainter.setStyle(null);
            barPainter.setLocationProducer(null);
            barPainter = null;
         }
         if(isLineup || isLinedown)
            barPainter = new PolylinePainter(this, barLocs);
         else
            barPainter = new LineSegmentPainter(this, barLocs);
      }

      // if we're using a LineSegmentPainter, set the adornment used
      if(barPainter instanceof LineSegmentPainter)
      {
         LineSegmentPainter lsp = (LineSegmentPainter) barPainter;
         lsp.setAdornment(null, doAdorn ? endCap : null, (float)capSizeMI, null, true, true, false, true);
      }
      
      // STEP 2: StringPainter for the auto-generated label
      //
      // painter is lazily constructed
      if(labelPainter == null) 
      {
         labelPainter = new SingleStringPainter();
         labelPainter.setStyle(this);           // the style info always comes from this element!
      }

      // make sure text, location and alignment are set IAW current definition
      if(lenMI <= 0 || !isAutoLabelOn())
      {
         // in this case, nothing is rendered!
         labelPainter.setTextAndLocation((String)null, null);
         labelPainter.setAlignment(TextAlign.CENTERED, TextAlign.LEADING);
      }
      else
      {
         // compose the label string
         String labelStr = Utilities.toString(length, 6, 3) + " " + getUnitsToken();

         // determine vertical offset of label string from calibration bar, and vertical text alignment
         double yOffset = 30;
         yOffset += getStrokeWidth();
         TextAlign vAlign = TextAlign.TRAILING;
         if(!isAboveOrLeft())
         {
            yOffset = -yOffset;
            vAlign = TextAlign.LEADING;
         }

         labelPainter.setTextAndLocation(labelStr, new Point2D.Double(lenMI/2, yOffset + lenMI) );
         labelPainter.setAlignment(TextAlign.CENTERED, vAlign);
      }
   }

   
   //
   // PSTransformable implementation
   //

   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      // special case: secondary calib assoc w/radial axis of polar plot is drawn horizontally
      boolean isHoriz = isPrimary();
      if((!isHoriz) && getParentGraph().isPolar()) isHoriz = true;

      // given its length, midpoint, and orientation, calculate the bar's left (or bottom) endpt in thousandth-inches 
      // wrt the parent viewport
      double barLength = getLengthInMilliInches();
      if(barLength <= 0.0) return;
      Point2D midPoint = parentVP.toMilliInches(getX(), getY());
      Point2D p0 = null;
      if(isHoriz) p0 = new Point2D.Double(midPoint.getX() - barLength/2.0, midPoint.getY());
      else p0 = new Point2D.Double(midPoint.getX(), midPoint.getY() - barLength/2.0);

       psDoc.startElement(this);

      // establish a new non-clipping viewport for the bar and its children: a square viewport with size = the bar's 
      // length and set up so that the bar itself lies along the top edge
      if(!isHoriz)
      {
         psDoc.translateAndRotate(p0, 90);
         p0.setLocation(0, -barLength);
         psDoc.translateAndRotate(p0, 0);
      }
      else
      {
         p0.setLocation(p0.getX(), p0.getY()-barLength);
         psDoc.translateAndRotate(p0, 0);
      }
      
      // render the bar itself at the top edge of the new viewport, possibly with an adornment at the endpoints 
      double capSz = endCapSize.toMilliInches();
      p0.setLocation(0,barLength);
      Point2D p1 = new Point2D.Double(barLength,barLength);
      psDoc.renderAdornedLine(p0, p1, false, endCap, capSz, endCap, capSz, null, 0);
      
      // render children, if any, with respect to the bar's viewport.
      for(int i=0; i<getChildCount(); i++) 
         getChildAt(i).toPostscript(psDoc);

      // if enabled, auto-generate a text label parallel to and centered on the bar and a little above or below it.
      // Won't be drawn if text/fill color is transparent (Postscript does not support transparency)
      if(isAutoLabelOn() && getFillColor().getAlpha() > 0)
      {
         // compose the label string
         String labelStr = Utilities.toString(getLength(), 6, 3) + " " + getUnitsToken();

         // determine vertical offset of label string from calibration bar, and vertical text alignment
         double yOffset = 30;
         yOffset += getStrokeWidth();
         TextAlign vAlign = TextAlign.TRAILING;
         if(!isAboveOrLeft())
         {
            yOffset = -yOffset;
            vAlign = TextAlign.LEADING;
         }
         psDoc.renderText(labelStr, barLength/2.0, barLength+yOffset, TextAlign.CENTERED, vAlign);
      }

      // end the element -- restoring the graphics state
      psDoc.endElement();
   }

   
   // 
   // Object
   //

   /**
    * This override ensures that the rendering infrastructure for the <code>CalibrationBarNode</code> clone is 
    * independent of the element cloned.
    */
   @Override protected Object clone()
   {
      CalibrationBarNode copy = (CalibrationBarNode) super.clone();
      copy.barLocs = null;
      copy.barPainter = null;
      copy.labelPainter = null;
      copy.rBoundsSelf = null;
      return(copy);
   }
}
