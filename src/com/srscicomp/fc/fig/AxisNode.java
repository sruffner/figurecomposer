package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dutil.TextBoxPainter;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;

/**
 * <b>AxisNode</b> encapsulates an axis of a {@link GraphNode}, whether it be a Cartesian, loglog, semilog, or polar 
 * plot. It is an intrinsic component of a graph, and it relies on the parent graph for information critical to its 
 * proper rendering: the axis type (linear, logarithmic, or circular -- the last applicable only to the theta axis of a 
 * polar plot), whether the graph's data is above or below (or to the left/right of) the axis, and the location and 
 * length of the axis line relative to the graph viewport.
 * 
 * <p>An axis node can have any number of {@link TickSetNode}s as children. These, of course, layout tick marks and 
 * associated labels along the axis line, and exist only as children of an axis. The first such tick set is the major 
 * tick set. The major tick set along the primary axis defines the locations of the parent graph's secondary gridlines, 
 * while the secondary axis's major ticks give the locations of the primary gridlines.</p>
 * 
 * <p>When the parent graph's type changes, the axis range may no longer be valid. For example, a standard polar plot 
 * will always have a starting radius <i>r0 &gt;= 0</i> and an end radius <i>r1 &gt; r0</i>. Logarithmic axes must have 
 * strictly positive endpoints. Rather than changing the attributes that encapsulate the axis range endpoints, we have 
 * chosen to validate the axis range endpoints whenever they are "requested". If access to the actual axis range 
 * endpoints -- which may be invalid in the current graph context -- is desired, use {@link #getStart()} and {@link 
 * #getEnd()}. To get the validated range -- use {@link #getValidatedStart()} and {@link #getValidatedEnd()}. ALWAYS use
 * the validated range methods for rendering, exporting to PS, etc. The other methods should be used only when getting 
 * or setting the user-specified axis range endpoints.</p>
 * 
 * <p>As of v5.4.0 (schema version 24), <b>AxisNode</b> supports a "styled text" label, in which text color, font
 * style, underline state, superscript, and subscript can vary on a per-character basis. See {@link 
 * FGraphicNode#toStyledText}.</p>
 * 
 * @author sruffner
 */
public class AxisNode extends FGNGraphAxis implements Cloneable
{
   /**
    * Construct an axis node spanning [0..100], with a power scale of 0 (no scaling), an empty <i>units</i> token and a
    * default title of "H AXIS" if it encapsulates the primary axis, or "V AXIS" for a secondary axis. The <i>spacer</i>
    * and <i>labelOffset</i> properties are initially set IAW user-defined preferred values. A single tick set is
    * defined initially, spanning the same range with a tick interval of 10.
    */
   public AxisNode(boolean isPrimary)
   {
      super();
      this.isPrimary = isPrimary;
      setTitle(this.isPrimary ? "H AXIS" : "V AXIS");
      start = 0;
      end = 100;
      powerScale = 0;
      units = "";
      spacer = FGNPreferences.getInstance().getPreferredAxisSpacer();
      labelOffset = FGNPreferences.getInstance().getPreferredAxisLabelOffset();
      hide = false;
      log2 = false;
      lineHeight = 0.8;
      
      insert(new TickSetNode(), 0);
   }

   /** 
    * Flag, set at construction time, which defines this as the primary (horizontal or angular coord) or secondary 
    * (vertical or radial coord) axis of its parent graph.
    */
   private final boolean isPrimary;
   
   /**
    * Does this axis node represent the primary axis of its parent graph? 
    * @return True if this is a primary axis; false otherwise.
    */
   public boolean isPrimary() { return(isPrimary); }
   
   
   // 
   // Support for child nodes 
   //
   
   @Override public FGNodeType getNodeType() { return(FGNodeType.AXIS); }
   /** An axis node admits any number of tick mark sets. */
   @Override public boolean canInsert(FGNodeType nodeType) { return(nodeType == FGNodeType.TICKS); }
   /** An axis node is an instrinsic component of its parent {@link GraphNode}. */
   @Override public boolean isComponentNode() { return(true); }

   
   //
   // Properties
   //
   
   /** Start of the axis range (implicit "user" units). */
   private double start;

   /**
    * Get the start of the coordinate range spanned by this axis. The property is set by the user and may not be a valid
    * value, depending on the current context. To get the validated start of the axis range, use {@link 
    * #getValidatedStart()}.
    * 
    * @return Start of axis range, in "user" units.
    */
   public double getStart() { return(start); }

   /**
    * Set the start of the range spanned by this axis. If a change was made, {@link #onNodeModified} is invoked.
    * 
    * @param d New value for the start of axis range. Rejected if infinite, NaN.
    * @return True if successful; false if value rejected OR if auto axis-scaling is enabled on the parent graph.
    */
   public boolean setStart(double d)
   {
      if(isAutoranged() || !Utilities.isWellDefined(d)) return(false);
      if(start != d)
      {
         if(doMultiNodeEdit(FGNProperty.START, d)) return(true);
         
         Double old = start;
         start = d;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.START);
            FGNRevEdit.post(this, FGNProperty.START, start, old, "Change start of axis range");
         }
      }
      return(true);
   }

   /** End of the axis range (implicit "user" units). */
   private double end;

   /**
    * Get the end of the coordinate range spanned by this axis. The property is set by the user and may not be a valid 
    * value, depending on the current context. To get the validated end of the axis range, use {@link 
    * #getValidatedEnd()}.
    * 
    * @return End of axis range, in "user" units.
    */
   public double getEnd() { return(end); }

   /**
    * Set the end of the range spanned by this axis. If a change was made, {@link #onNodeModified} is invoked.
    * 
    * @param d New value for the end of axis range. Rejected if infinite, NaN.
    * @return True if successful; false if value rejected OR if auto axis-scaling is enabled on the parent graph.
    */
   public boolean setEnd(double d)
   {
      if(isAutoranged() || !Utilities.isWellDefined(d)) return(false);
      if(end != d)
      {
         if(doMultiNodeEdit(FGNProperty.END, d)) return(true);
         
         Double old = end;
         end = d;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.END);
            FGNRevEdit.post(this, FGNProperty.END, end, old, "Change end of axis range");
         }
      }
      return(true);
   }

   /**
    * Set the start and end of the range spanned by this axis node in one step -- without triggering a rendering update
    * and without posting the changes to the model's edit history. Method assumes that specified range is well-defined.
    * @param s Start of range. 
    * @param e End of range.
    */
   @Override protected void setRange_NoUpdate(double s, double e)
   {
      start = s;
      end = e;
   }
   
   /** Axis scale factor: Exponent N such that axis tick mark labels are scaled by 10^N */
   private int powerScale;

   @Override public int getPowerScale() { return(powerScale); }

   /**
    * Set the axis scale factor -- an integer exponent N such that numeric axis tick mark labels are scaled by 10^N.
    * If a change was made, {@link #onNodeModified} is invoked.
    * 
    * @param n New value for scale factor exponent. Rejected if outside allowed range. See {@link #getPowerScale()}.
    * @return True if successful; false if value rejected.
    */
   public boolean setPowerScale(int n)
   {
      if(n < MINPOWERSCALE || n > MAXPOWERSCALE) return(false);
      if(powerScale != n)
      {
         if(doMultiNodeEdit(FGNProperty.SCALE, n)) return(true);
         
         Integer old = powerScale;
         powerScale = n;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.SCALE);
            FGNRevEdit.post(this, FGNProperty.SCALE, (double) powerScale, old, "Change axis power scale");
         }
      }
      return(true);
   }

   /** A string token representing the units in which coordinates are measured along this axis. It may be empty. */
   private String units;

   /**
    * Get the units token for this axis node.
    * @return The units token. It may be an empty string.
    */
   public String getUnits() { return(units); }

   /**
   * Set the units token for this axis node. If a change is made, {@link #onNodeModified} is invoked.
   * 
   * @param u The new units token. A null value is treated as the equivalent of an empty string.
   * @return True if successful; false if the new token had any unsupported characters; any such characters are replaced
   * by question marks.
   */
   public boolean setUnits(String u)
   {
      String str = FGraphicModel.replaceUnsupportedCharacters((u==null) ? "" : u);
      if(!str.equals(units))
      {
         if(doMultiNodeEdit(FGNProperty.UNITS, str)) return(str.equals(u));
         
         String old = units;
         units = FGraphicModel.replaceUnsupportedCharacters(str);
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.UNITS);
            FGNRevEdit.post(this, FGNProperty.UNITS, units, old, "Set axis units token to " + units);
         }
      }
      return(str.equals(u));
   }

   /**
    * The perpendicular distance between the axis line and the closest parallel edge of the parent graph's data box. 
    * Specified as a linear measurement with associated units; relative units not allowed.
    */
   private Measure spacer;

   /**
    * Constrains the measured properties <em>spacer</em> and <em>labelOffset</em> to use physical units, to have a 
    * maximum of 4 significant and 3 fractional digits, and to lie in the range [0..2in].
    */
   public final static Measure.Constraints SPACERCONSTRAINTS = new Measure.Constraints(0.0, 2000.0, 4, 3);

   /**
    * Get the perpendicular distance between the axis line and the closest parallel edge of the parent graph's data box.
    * @return The axis spacer, specified as a linear measurement with associated units.
    */
   public Measure getSpacer() { return(spacer); }

   /**
    * Set the perpendicular distance between the axis line and the closest parallel edge of the parent graph's data box.
    * If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param m The new axis spacer distance. The measure is constrained to satisfy {@link #SPACERCONSTRAINTS}. A null
    * value is rejected.
    * @return True if change was accepted; false if rejected.
    */
   public boolean setSpacer(Measure m)
   {
      if(m == null) return(false);
      m = SPACERCONSTRAINTS.constrain(m);

      boolean changed = (spacer != m) && !Measure.equal(spacer, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.SPACER, m)) return(true);
         
         Measure old = spacer;
         spacer = m;
         if(areNotificationsEnabled())
         {
            if(old.toMilliInches() != spacer.toMilliInches()) onNodeModified(FGNProperty.SPACER);
            FGNRevEdit.post(this, FGNProperty.SPACER, spacer, old, "Set axis line spacer to " + spacer.toString());
         }
      }
      return(true);
   }

   /** The perpendicular distance separating the axis label from the axis line. */
   private Measure labelOffset;

   /**
    * Get the perpendicular distance between the axis line and the bottom or top of the axis label (whichever is 
    * closer).
    * @return The axis label offset, specified as a linear measurement with associated units.
    */
   public Measure getLabelOffset() { return(labelOffset); }

   /**
    * Set the perpendicular distance between the axis line and the bottom or top of the axis label (whichever is 
    * closer). If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param m The new axis label offset. The measure will be constrained to satisfy {@link #SPACERCONSTRAINTS}. A null 
    * value is rejected. 
    * @return True if change was accepted; false if rejected.
    */
   public boolean setLabelOffset(Measure m)
   {
      if(m == null) return(false);
      m = SPACERCONSTRAINTS.constrain(m);

      boolean changed = (labelOffset != m) && !Measure.equal(labelOffset, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.LABELOFFSET, m)) return(true);
         
         Measure old = labelOffset;
         labelOffset = m;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.LABELOFFSET);
            FGNRevEdit.post(this, FGNProperty.LABELOFFSET, labelOffset, old, 
                  "Set axis label offset to " + labelOffset.toString());
         }
      }
      return(true);
   }

   /** If true, the axis node is not rendered. */
   private boolean hide;

   /**
    * Get the hide state for this axis node
    * @return True if the axis is currently hidden.
    */
   public boolean getHide() { return(hide); }

   /**
    * Set the hide state for this axis node. If a change is made, {@link #onNodeModified} is invoked.
    * @param b True to hide axis, false to show it.
    */
   public void setHide(boolean b)
   {
      if(hide != b)
      {
         if(doMultiNodeEdit(FGNProperty.HIDE, b)) return;
         
         Boolean old = hide;
         hide = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.HIDE);
            String desc = hide ? "Hide " : "Show ";
            desc += isPrimary ? "horizontal (or theta) axis" : "vertical (or radial) axis";
            FGNRevEdit.post(this, FGNProperty.HIDE, hide, old, desc);
         }
      }
   }

   /**
    * Same as {@link #setHide}, but without posting an "undo" operation or calling {@link #onNodeModified}. It is
    * meant to be used only when multiple properties are being modified in a single, atomic operation.
    * @param b True to hide axis, false to show it.
    */
   protected void setHideNoNotify(boolean b) { hide = b; }
   
   /** 
    * Flag indicates whether logarithmic axis is presented as base2 (true) or base10 (false). This affects rendering
    * of child tick sets and auto-scaling of the axis range -- but ONLY when the axis is logarithmic.
    */
   private boolean log2;

   /**
    * Is the axis's logarithmic base 2? This property affects rendering of child tick sets and auto-scaling of the axis
    * range -- but ONLY when axis is logarithmic.
    * 
    * @return True if logarithmic base is 2; otherwise, it is log base 10.
    */
   public boolean getLog2() { return(log2); }

   /**
    * Select the logarithmic base for this axis. If a change is made, {@link #onNodeModified} is invoked.
    * @param b True for base 2, false for base 10.
    */
   public void setLog2(boolean b)
   {
      if(log2 != b)
      {
         if(doMultiNodeEdit(FGNProperty.LOG2, b)) return;
         
         Boolean old = log2;
         log2 = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.LOG2);
            String desc = "Set axis logarithmic base = " + (log2 ? "2" : "10");
            FGNRevEdit.post(this, FGNProperty.LOG2, log2, old, desc);
         }
      }
   }

   /** Text line height, as a fraction of the element's font size. Range-restricted to [0.8, 3.0]. */
   private double lineHeight;

   /**
    * Get the text line height for the axis label (in the event it is laid out over multiple text lines).
    * @return The text line height <i>as a fraction of the current font size</i>. Range-restricted to [0.8, 3.0].
    */
   public double getLineHeight() {  return(lineHeight); }

   /**
    * Set the text line height for the axis label, in the event it is laid out over multiple text lines.
    * @param lh The text line height <i>as a fraction of the current font size</i>. Range-restricted to [0.8, 3.0].
    * @return True if successful; false if argument is NaN. Note that any out-of-range value will be silently corrected.
    */
   public boolean setLineHeight(double lh)
   {
      if(!Utilities.isWellDefined(lh)) return(false);
      
      lh = Utilities.rangeRestrict(0.8, 3.0, lh);
      if(lineHeight != lh)
      {
         if(doMultiNodeEdit(FGNProperty.LINEHT, lh)) return(true);
         
         Double old = lineHeight;
         lineHeight = lh;
         
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.LINEHT);
            FGNRevEdit.post(this, FGNProperty.LINEHT, lineHeight, old);
        }
      }
      return(true);
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok;
      switch(p)
      {
         case END : ok = setEnd((Double) propValue); break;
         case START : ok = setStart((Double) propValue); break;
         case SCALE : ok = setPowerScale((Integer) propValue); break;
         case LABELOFFSET: ok = setLabelOffset((Measure) propValue); break;
         case SPACER: ok = setSpacer((Measure) propValue);  break;
         case UNITS: ok = setUnits((String) propValue); break;
         case HIDE: setHide((Boolean) propValue); ok = true; break;
         case LOG2: setLog2((Boolean) propValue); ok = true; break;
         case LINEHT : ok = setLineHeight((Double) propValue); break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }
   
   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value;
      switch(p)
      {
         case END : value = getEnd(); break;
         case START : value = getStart(); break;
         case SCALE : value = getPowerScale(); break;
         case LABELOFFSET: value = getLabelOffset(); break;
         case SPACER: value = getSpacer();  break;
         case UNITS: value = getUnits(); break;
         case HIDE: value = getHide(); break;
         case LOG2: value = getLog2(); break;
         case LINEHT : value = getLineHeight(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }
   

   @Override protected void rescaleSelf(double scale, MultiRevEdit undoer)
   {
      Measure.Constraints c = SPACERCONSTRAINTS;
      double d = spacer.getValue();
      if(d > 0)
      {
         Measure oldSpacer = spacer;
         spacer = c.constrain(new Measure(d*scale, spacer.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.SPACER, spacer, oldSpacer);
      }

      d = labelOffset.getValue();
      if(d > 0)
      {
         Measure oldLO = labelOffset;
         labelOffset = c.constrain(new Measure(d*scale, labelOffset.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.LABELOFFSET, labelOffset, oldLO);
      }
   }

   /**
    * This override handles a number of special cases and side-effects:
    * <ul>
    *    <li>A change in the <i>units</i> attribute does not affect the rendering of the axis itself, but it may 
    *    affect the rendering of calibration bars in the parent graph.</li>
    *    <li>A change in the axis range requires a complete re-rendering of the entire parent graph.</li>
    *    <li>If the <i>hide</i> flag is toggled on, then we need to recalculate the axis's render bounds and re-render 
    *    in that area; conversely, if it is toggled off, then we need to erase the axis's old (cached) render bounds. 
    *    However, if the axis represents the theta axis of a polar graph, there's nothing to render regardless.</li>
    *    <li>For all other attributes, no rendering update is needed if the axis is not rendered; otherwise, the 
    *    super-class implementation suffices.</li>
    *    <li>If the axis currently lacks a parent graph, the method does nothing. An axis is only renderable in the 
    *    context supplied by a parent graph.</li>
    * </ul>
    */
   @Override protected void onNodeModified(Object hint)
   {
      GraphNode g = (GraphNode) getParentGraph();
      if(g == null) return;
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      if(hint == FGNProperty.UNITS)
      {
         model.onChange(this, 0, false, null);
         g.onAxisUnitsChange(isPrimary);
      }
      else if(hint == null || hint == FGNProperty.START || hint == FGNProperty.END)
      {
         // this call triggers an update of the rendering infrastructure of the graph and all of its subordinates
         g.onNodeModified(FGNProperty.WIDTH);
      }
      else if(hint == FGNProperty.LOG2 && isLogarithmic() && isAutoranged())
      {
         // when log base is toggled AND axis is logarithmic and auto-scaled, do an autoscale to fix axis range.
         g.autoScaleAxes();
      }
      else if(hint == FGNProperty.HIDE)
      {
         // if axis is a theta axis, there's no change in appearance because it is always hidden. However, we need to
         // notify the model that the property changed
         if(isTheta())
         {
            model.onChange(this, 0, false, null);
            return;
         }

         // we need to recalculate bounds regardless. However, if it was just hidden, add old bounds to dirty list; 
         // else, add new bounds.
         Graphics2D g2d = model.getViewerGraphics();
         try
         {
            Shape dirtyShape = getCachedGlobalShape();               // global bounds prior to change
            getRenderBounds(g2d, true, null);                        // update this node and its ancestors
            FGraphicNode.updateAncestorRenderBounds(g2d, this); 
            if(!getHide()) dirtyShape = getCachedGlobalShape();      // global bounds after the change

            List<Rectangle2D> dirtyAreas = new ArrayList<>();
            dirtyAreas.add( dirtyShape.getBounds2D() );
            model.onChange(this, 0, true, dirtyAreas);
         }
         finally { if(g2d != null) g2d.dispose(); }
      }
      else
      {
         // changes are isolated to the axis and its children. However, if the axis is not drawn, there are 
         // no dirty areas to re-render!
         if(!(getHide() || isTheta()))
            super.onNodeModified(hint);
         else
            model.onChange(this, 0, false, null);
      }
   }

   /**
    * This override handles a number of special cases and side-effects:
    * <ul>
    *    <li>If the node inserted is the first tick set -- representing the axis's major tick marks --, the parent 
    *    graph is notified: the corresponding grid lines may need to be updated accordingly. Also, if auto axis-scaling
    *    is currently enabled, the tick set will need to be auto-adjusted.</li>
    *    <li>If the axis is currently not rendered (hidden or theta axis), then adding a subordinate does not affect 
    *    the rendering of the axis. Otherwise, the rendering infrastructure is updated via the super-class method.</li>
    *    <li>If the axis currently lacks a parent graph, the method does nothing. An axis is only renderable in the 
    *    context supplied by a parent graph.</li>
    * </ul>
    */
   @Override protected void onSubordinateNodeInserted(FGraphicNode sub)
   {
      GraphNode g = (GraphNode) getParentGraph();
      if(g == null) return;
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      if(isRendered())
         super.onSubordinateNodeInserted(sub);
      else
         model.onChange(this, 1, false, null);

      if(getMajorTickSet() == sub)
      {
         if(!g.autoScaleAxes())
            g.onAxisMajorTicksChanged(isPrimary);
      }
   }

   /**
    * This override handles a number of special cases and side-effects:
    * <ul>
    *    <li>If a tick set is removed, the parent graph is notified. If it had been the major tick set (we don't know 
    *    because it has already been removed), the corresponding grid lines must be updated accordingly if they are 
    *    rendered.</li>
    *    <li>If the axis is currently not rendered (hidden or theta axis), then removing a subordinate does not affect 
    *    the rendering of the axis. Otherwise, the rendering infrastructure is updated via the super-class method.</li>
    *    <li>If the axis currently lacks a parent graph, the method does nothing. An axis is only renderable in the 
    *    context supplied by a parent graph.</li>
    * </ul>
    */
   @Override protected void onSubordinateNodeRemoved(FGraphicNode sub)
   {
      GraphNode g = (GraphNode) getParentGraph();
      if(g == null) return;
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      if(isRendered())
         super.onSubordinateNodeRemoved(sub);
      else
         model.onChange(this, 1, false, null);

      g.onAxisMajorTicksChanged(isPrimary);
   }

   /**
    * This overrides the default behavior, preventing selection of the underlying tick sets. This is because a user
    * almost always expects the axis element to get the focus when s/he clicks on or near the axis line.
    */
   @Override protected FGraphicNode hitTest(Point2D p) { return((super.hitTest(p) != null) ? this : null); }


   // 
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet()  { return(true); }

   /** 
    * The node-specific properties exported in a axis node's style set are the label offset and spacer between axis line
    * and graph, the units label, the 'log2' flag, and the text line height (impacts a multi-line axis label). Axis 
    * range and power scale are NOT included. 
    * 
    * <p>In addition, for each tick mark set child defined on the axis, a component style set is added to the axis 
    * node's style set. Technically, tick sets are not components of an axis, but they are part of the axis's rendered
    * appearance, so we include them in this fashion.</p>
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.SPACER, getSpacer());
      styleSet.putStyle(FGNProperty.LABELOFFSET, getLabelOffset());
      styleSet.putStyle(FGNProperty.UNITS, getUnits());
      styleSet.putStyle(FGNProperty.LOG2, getLog2());
      styleSet.putStyle(FGNProperty.LINEHT, getLineHeight());
      
      for(int i=0; i<getChildCount(); i++)
      {
         FGNStyleSet tickSS = getChildAt(i).getCurrentStyleSet();
         styleSet.addComponentStyleSet(tickSS);
      }
   }

   /**
    * Accounts for the axis-specific style properties when the applied style set originated from an axis or color axis
    * node. In addition, it also styles each tick mark set child for which there's a corresponding tick mark styling 
    * component in the applied style set. This is not technically correct usage of the style set feature, since tick
    * mark sets are not true components of an axis node. 
    */
   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      // the label offset, spacer and line height properties mean the same thing on AxisNode and ColorAxisNode, so we 
      // should accept these properties from either node type. The line height property should also be accepted from a
      // TextBoxNode. All other style properties are AxisNode-specific.
      FGNodeType nt = applied.getSourceNodeType();
      boolean isTB = nt.equals(FGNodeType.TEXTBOX);
      if(!nt.equals(FGNodeType.CBAR)) nt = getNodeType();
      
      Measure m = (Measure) applied.getCheckedStyle(FGNProperty.SPACER, nt, Measure.class);
      if(m != null) m = SPACERCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.SPACER, null, Measure.class)))
      {
         spacer = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.SPACER);

      m = (Measure) applied.getCheckedStyle(FGNProperty.LABELOFFSET, nt, Measure.class);
      if(m != null) m = SPACERCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure)restore.getCheckedStyle(FGNProperty.LABELOFFSET,null,Measure.class)))
      {
         labelOffset = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LABELOFFSET);
      
      String s = (String) applied.getCheckedStyle(FGNProperty.UNITS, getNodeType(), String.class);
      if(s != null && !s.equals(restore.getCheckedStyle(FGNProperty.UNITS, null, String.class)))
      {
         units = s;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.UNITS);
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.LOG2, getNodeType(), Boolean.class);
      if(b != null && !b.equals(restore.getCheckedStyle(FGNProperty.LOG2, null, Boolean.class)))
      {
         log2 = b;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LOG2);
      
      Double lineHt = (Double) applied.getCheckedStyle(FGNProperty.LINEHT, isTB ? null : nt, Double.class);
      if(lineHt != null && !lineHt.equals(restore.getStyle(FGNProperty.LINEHT)))
      {
         lineHeight = Utilities.rangeRestrict(0.8, 3.0, lineHt);
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LINEHT); 

      // special case: If style set source is an axis or color axis, apply its tick mark set styling to this axis.
      if(nt == FGNodeType.AXIS || nt == FGNodeType.CBAR)
      {
         for(int i=0; i<getChildCount(); i++)
         {
            if(applied.getNumberComponentStyleSets() <= i)
               restore.removeComponentStyleSet(i);
            else
            {
               FGraphicNode child = getChildAt(i);
               FGNStyleSet styling = applied.getComponentStyleSet(i);
               FGNStyleSet restoreStyling = restore.getComponentStyleSet(i);
               if(child.applyTextDrawStyles(styling, restoreStyling)) changed = true;
               if(child.applyNodeSpecificStyles(styling, restoreStyling)) changed = true;
            }
         }
      }
      
      return(changed);
   }


   //
   // Properties derived from parent graph context
   //

   /**
    * Is this axis above or to the left of the parent graph's data box?
    * 
    * <p>In Cartesian (logarithmic or not) graphs, the primary axis (x-axis) is above the data box in the 3rd- and 
    * 4th-quadrant layouts; below it in the 1st- and 2nd-quadrant layouts. The secondary axis (y-axis) is left of the 
    * data box in the 1st- and 4th-quadrant layouts; right of it in the 2nd- and 3rd-quadrant layouts. In the all-quad 
    * layout, the axes are actually inside the data box. However, by convention, the primary axis is considered below 
    * and the secondary axis to the left of the data box in the all-quad layout.</p>
    * 
    * <p>In polar plots, the secondary axis -- ie, the radial axis -- is above the data box in the third- and fourth-
    * quadrant layouts and below it otherwise. The return value makes no sense for the primary, or theta axis of a 
    * polar plot; the theta axis is never rendered anyway.</p>
    * 
    * @return True if axis is above or to left of the graph's data box. Returns false always f the axis currently has no
    * parent graph.
    */
   public boolean isAboveLeft()
   {
      GraphNode g = (GraphNode) getParentGraph();
      if(g == null) return(false);

      GraphNode.Layout layout = g.getLayout();
      if(g.isPolar())
         return( (!isPrimary) && (layout == GraphNode.Layout.QUAD3 || layout == GraphNode.Layout.QUAD4) );
      else if(isPrimary)
         return(layout == GraphNode.Layout.QUAD3 || layout == GraphNode.Layout.QUAD4);
      else
         return(!(layout == GraphNode.Layout.QUAD2 || layout == GraphNode.Layout.QUAD3));
   }

   /**
    * Does this axis node represent a logarithmic axis in its current context? 
    * 
    * <p>An axis is logarithmic if:
    * <ul>
    *    <li>The parent graph is a loglog plot.</li>
    *    <li>It is the primary axis of a <code>GraphNode.CoordSys.SEMILOGX</code> graph.</li>
    *    <li>It is the secondary axis of a <code>GraphNode.CoordSys.SEMILOGY</code> graph.</li>
    *    <li>It is the secondary axis of a <code>GraphNode.CoordSys.SEMILOGR</code> graph.</li>
    * </ul>
    * </p>
    *
    * @return True if axis is logarithmic. Returns false always if the axis currently has no parent graph.
    */
   boolean isLogarithmic()
   {
      GraphNode g = (GraphNode) getParentGraph();
      if(g == null) return(false);

      GraphNode.CoordSys coordSys = g.getCoordSys();
      return( (coordSys == GraphNode.CoordSys.LOGLOG) || (isPrimary && coordSys == GraphNode.CoordSys.SEMILOGX) ||
            ((!isPrimary) && (coordSys == GraphNode.CoordSys.SEMILOGY || coordSys == GraphNode.CoordSys.SEMILOGR)) );
   }

   @Override int getLogBase() { return(log2 ? 2 : 10); }
   
   /**
    * Does this axis node represent the theta axis of a polar plot? This will be the case for the primary axis of a 
    * graph configured in polar coordinates.
    * 
    * @return True if axis is the theta axis of a polar graph. Returns false always if the axis currently has no parent
    * graph.
    */
   boolean isTheta()
   {
      GraphNode g = (GraphNode) getParentGraph();
      if(g == null) return(false);

      return(isPrimary && g.isPolar());
   }

   /**
    * Does this axis node represent a horizontal axis in its current context? The primary or x-axis of a non-polar graph
    * is horizontal. Also, by convention, the radial axis of a polar graph is considered horizontal.
    * 
    * @return True if axis is horizontal, as described. Returns false always if the axis currently has no parent graph.
    */
   boolean isHorizontal()
   {
      GraphNode g = (GraphNode) getParentGraph();
      if(g == null) return(false);
      return(g.isPolar() != isPrimary);
   }

   /**
    * Does this axis node represent a vertical axis in its current context? Only the secondary or y-axis of a non-polar
    * graph is considered vertical.
    * 
    * @return True if axis is vertical, as described. Returns false always if the axis currently has no parent graph.
    */
   boolean isVertical()
   {
      GraphNode g = (GraphNode) getParentGraph();
      if(g == null) return(false);
      return(!(g.isPolar() || isPrimary));
   }

   /**
    * Get the actual length of the axis line, in milli-inches.
    * 
    * <p>For a horizontal axis, the axis length is the width of the graph's data box; for a vertical axis, it is the 
    * height. For the theta axis of a polar graph, this method returns 0 since a theta axis is never rendered. For the 
    * radial axis of a polar graph, this method returns the smaller dimension of the bounding box -- so that the 
    * specified radial range is entirely visible within a non-square data box. In a four-quadrant layout, the axis 
    * origin is at the center of the data box, so the radial axis length is 1/2 of the smaller dimension.</p>
    *
    * @return Actual length of axis, in milli-inches. Returns 0 if the axis currently lacks a parent graph, or if it is 
    * the theta axis in a polar graph.
    */
   double getLengthInMilliInches()
   {
      GraphNode g = (GraphNode) getParentGraph();
      if(g == null) return(0);
      Rectangle2D rBox = g.getBoundingBoxLocal();
      double len;
      if(!g.isPolar()) len = isPrimary ? rBox.getWidth() : rBox.getHeight();
      else if(isPrimary) len = 0;
      else
      {
         len = Math.min(rBox.getWidth(), rBox.getHeight());
         if(g.getLayout() == GraphNode.Layout.ALLQUAD) len /= 2.0;
      }
      return(len);
   }

   /**
    * Get physical location of the axis origin WRT the parent graph's viewport, with coordinates expressed in milli-
    * inches. For a horizontal axis, this is the location of the left end point; for a vertical axis, the bottom end 
    * point. 
    * 
    * <p>The origin's location depends upon the current graph layout and the axis's <i>spacer</i> attribute, which tells
    * how much white space to put between the closest parallel edge of the graph's data box and the axis line. The X
    * coordinate of a horizontal axis's origin always lines up with the left edge of the data box, while the Y 
    * coordinate places the line above, below or bisecting the box (depending on the layout). Analogously for a vertical
    * axis.</p>
    * 
    * <p>The radial axis of a polar graph is treated similarly to the x-axis of a Cartesian graph, except in the four-
    * quadrant layout. Here the axis origin is placed at the center of the data box. For the theta axis of a polar 
    * graph, this method returns <i>(NaN, NaN)</i> always -- since the theta axis is never rendered.
    * 
    * @return The axis origin WRT the parent graph's viewport, as described. If the axis lacks a parent graph, this 
    * method will return <i>(NaN, NaN)</i>.
    */
   Point2D getOriginInMilliInches()
   {
      Point2D origin = new Point2D.Double(Double.NaN, Double.NaN);

      GraphNode g = (GraphNode) getParentGraph();
      if(g == null || isTheta()) return(origin);
      Rectangle2D rBox = g.getBoundingBoxLocal();
      if(rBox.isEmpty()) return(origin);

      double spacerMI = spacer.toMilliInches();
      boolean isHoriz = isHorizontal();
      boolean isAboveLeft = isAboveLeft();
      boolean isRadial = g.isPolar() && !isPrimary;

      double x = 0; 
      double y = 0; 
      if(isHoriz || isRadial)
      {
         // H or radial axis: left endpoint always at left edge of viewport (unless all-quadant polar layout)
         if(g.getLayout() == GraphNode.Layout.ALLQUAD)
         {
            y = rBox.getHeight()/2.0;
            if(isRadial) x = rBox.getWidth()/2.0;
         }
         else if( isAboveLeft ) y = rBox.getHeight() + spacerMI;
         else y = -spacerMI;
      }
      else
      {
         // vertical axis: bottom endpoint always aligns w/ bot edge of viewport
         if(g.getLayout() == GraphNode.Layout.ALLQUAD) x = rBox.getWidth()/2.0;
         else if(isAboveLeft) x = -spacerMI;
         else x = rBox.getWidth() + spacerMI;
      }
      origin.setLocation(x, y);
      
      return(origin);
   }

   @Override double getAxisEndpointAdjustment()
   {
      GraphNode g = (GraphNode) getParentGraph();
      if(g == null || g.isPolar() || (g.getLayout() == GraphNode.Layout.ALLQUAD)) return( 0 );

      AxisNode otherAxis = isPrimary ? g.getSecondaryAxis() : g.getPrimaryAxis();
      double adj = otherAxis.getStrokeWidth() / 2.0;
      if(getStrokeEndcap() != StrokeCap.BUTT) adj -= getStrokeWidth() / 2.0;
      
      return( !otherAxis.getHide() ? adj : 0 );
   }

   /**
    * Are the corresponding grid lines in the parent graph -- ie, those which are located IAW the major tick marks 
    * along this axis -- currently visible?
    * 
    * <p>The method checks the <i>hide</i> property of the relevant {@link GridLineNode} component of the parent graph:
    * the primary axis grid lines are located IAW the first tick set in the secondary axis, while the secondary axis 
    * grid lines are located IAW the first tick set in the primary axis. The method is invoked when the first {@link 
    * TickSetNode} of a graph axis is inserted, removed, or modified in some way -- to determine whether the graph's 
    * grid lines need to be re-rendered accordingly.</p>
    * 
    * @return True if the corresponding graph grid lines are currently visible. Returns false if this axis ndoe 
    * currently lacks a parent graph.
    */
   boolean areCorrespondingGridLinesVisible()
   {
      GraphNode g = (GraphNode) getParentGraph();
      if(g == null) return(false);

      GridLineNode grid = isPrimary ? g.getSecondaryGridLines() : g.getPrimaryGridLines();
      return(grid != null && !grid.getHide());
   }

   /**
    * Get the starting point of this axis node's coordinate range in user units, VALIDATED in accordance with the 
    * current graph context.
    * <ul>
    *    <li>For the linear axis of a Cartesian graph, the validated range start is the same as the nominal value 
    *    returned by {@link #getStart()}.</li>
    *    <li>For a logarithmic Cartesian or radial axis, the validated range start is a strictly positive value. If the
    *    actual value is negative or zero, the method returns a default of 0.01.</li>
    *    <li>For a linear radial axis, the starting radial value must be &gt;= 0. If the actual value is negative, 
    *    the method returns a default of 0.</li>
    *    <li>For the theta axis of a polar graph, the axis range is determined by the parent graph's layout. In this 
    *    context, the axis range defined by {@link #getStart()} and {@link #getEnd()} is ignored. For completeness, 
    *    however, this method returns 0, 90, 180, 270, or 0 deg depending on whether the graph layout is, respectively, 
    *    1st-, 2nd-, 3rd-, 4th-, or all-quad.</li>
    * </ul>
    * 
    * <p><b>ALWAYS</b> use the validated axis range when performing any computations related to the actual rendering of 
    * the axis and its parent graph.</p>
    * 
    * @return Valid starting point of axis coordinate range, in user units. Returns the nominal value provided by 
    * {@link #getStart()} if there is no parent graph or if the nominal value is already valid.
    */
   double getValidatedStart()
   {
      GraphNode g = (GraphNode) getParentGraph();
      double start = getStart();
      if(g == null) return(start);

      boolean isPolar = g.isPolar();
      boolean isRadial = isPolar && !isPrimary;
      boolean isLogarithmic = isLogarithmic();

      if(!(isLogarithmic || isPolar))
         return(start);

      if(isLogarithmic)
         return((start <= 0) ? 0.01 : start);

      if(isRadial)
         return((start < 0) ? 0 : start);

      start = 0;
      if(g.getLayout() == GraphNode.Layout.QUAD2) start = 90;
      else if(g.getLayout() == GraphNode.Layout.QUAD3) start = 180;
      else if(g.getLayout() == GraphNode.Layout.QUAD4) start = 270;
      return(start);
   }

   /**
    * Get the end point of this axis node's coordinate range in user units, VALIDATED in accordance with the 
    * current graph context.
    * <ul>
    *    <li>For the linear axis of a Cartesian graph, the validated range end is the same as the nominal value returned 
    *    by {@link #getEnd()}, <b>UNLESS</b> it is the same as the validated starting point, in which case it is
    *    incremented by 1.</li>
    *    <li>For a logarithmic Cartesian or radial axis, the validated range end is a strictly positive value. If the 
    *    actual value is negative or zero, the method returns a default of 10. In addition, the range end cannot be the 
    *    same value as the validated starting point. If so, the validated range end is set to 10x the validated 
    *    starting point.</li>
    *    <li>For a linear radial axis, the maximum radial value must be strictly greater than the starting radial value. 
    *    If it is not, the method returns 10 more than the validated start of the axis range.</li>
    *    <li>For the theta axis of a polar graph, the axis range is determined by the parent graph's layout. In this 
    *    context, the axis range defined by {@link #getStart()} and {@link #getEnd()} is ignored. For completeness, 
    *    however, this method returns 90, 180, 270, 360, or 360 deg depending on whether the graph layout is, 
    *    respectively, 1st-, 2nd-, 3rd-, 4th-, or all-quad.</li>
    * </ul>
    * 
    * <p><b>ALWAYS</b> use the validated axis range when performing any computations related to the actual rendering of
    * the axis and its parent graph.</p>
    * 
    * @return Valid end point of axis coordinate range, in user units. Returns the nominal value provided by {@link 
    * #getEnd()} if there is no parent graph or if the nominal value is already valid.
    */
   double getValidatedEnd()
   {
      GraphNode g = (GraphNode) getParentGraph();
      double end = getEnd();
      double validStart = getValidatedStart();
      if(g == null) return(end);

      boolean isPolar = g.isPolar();
      boolean isRadial = isPolar && !isPrimary;
      boolean isLogarithmic = isLogarithmic();

      if(!(isLogarithmic || isPolar)) 
         return((end == validStart) ? end+1.0 : end);

      if(isLogarithmic)
      {
         if(end <= 0 || end == validStart) 
            end = validStart * 10;
         return(end);
      }

      if(isRadial)
      {
         if(end < validStart)
            end = validStart + 10;
         return(end);
      }

      end = 360;
      if(g.getLayout() == GraphNode.Layout.QUAD1) end = 90;
      else if(g.getLayout() == GraphNode.Layout.QUAD2) end = 180;
      else if(g.getLayout() == GraphNode.Layout.QUAD3) end = 270;
      return(end);
   }


   //
   // Tick sets
   //

   /**
    * Get the {@link TickSetNode} defining the "major tick marks" for this axis. By convention, the first tick set
    * child defines the "major tick marks".
    * @return The major tick mark set for this axis, if it has one. Returns null if this node has no tick sets.
    */
   @Override TickSetNode getMajorTickSet() { return(getChildCount() == 0 ? null : (TickSetNode)getChildAt(0)); }
   
   /**
    * Does this axis possess a major tick mark set? By convention, the first tick set child node constitutes the major 
    * tick marks, so this method returns true if the axis contains at least one such child.
    * 
    * @return True if the axis defines major tick marks.
    */
   boolean hasMajorTicks() { return(getChildCount() > 0); }

   /**
    * Retrieve the set of major tick marks defined on this axis.
    * 
    * @return The set of major ticks defined on this axis, specified in user units. If there are none, this method 
    * returns an empty array.
    */
   double[] getMajorTicks()
   {
      TickSetNode major = getMajorTickSet();
      return(major == null ? TickSetNode.NOTICKS : major.getTickSet());
   }


   //
   // Focusable/Renderable support
   //
   
   /**
    * The rendering coordinate system of an axis node is analogous to that of a {@link LineNode}. The origin is 
    * translated to the point, in the parent graph's viewport, where the first end point of the axis lies. The viewport
    * is rotated by 90 degrees if the axis is vertical. Then, the origin is translated downward by the physical length 
    * of the axis -- so that the axis line lies at y=L in its own local rendering coordinates.
    * 
    * <p>Since the theta axis of a polar graph is never rendered, this method will simply return an identity transform 
    * whenever this axis node represents a theta axis.</p>
    */
   @Override public AffineTransform getLocalToParentTransform()
   {
      AffineTransform identity = new AffineTransform();

      GraphNode g = (GraphNode) getParentGraph();
      if((g == null) || isTheta()) return(identity);

      Point2D origin = getOriginInMilliInches();
      double len = getLengthInMilliInches();
      if(len == 0 || !Utilities.isWellDefined(origin)) return(identity);

      AffineTransform at = AffineTransform.getTranslateInstance(origin.getX(), origin.getY());
      if( !isHorizontal() ) at.rotate( Math.PI/2.0 );
      at.translate(0, -len);
      return(at);
   }

   /**
    * An axis node must define a viewport WRT which its child tick sets are rendered. An axis is unique because its 
    * viewport depends on its context in the parent graph. In cartesian, semilog, and loglog graphs, the axes are 
    * horizontal and vertical lines representing linear or logarithmic coordinates. In a polar or semilogR plot, the 
    * radial axis is a horizontal line representing linear or log coordinates, while the theta axis traces a circular 
    * arc and represents angular coordinates. In addition, like a generic line segment, an axis is really 1D -- so we 
    * must define conventions for positioning a graphic object (a tick mark label, eg) off of the axis line. Those 
    * conventions are encapsulated by the viewport returned by this method.
    * 
    * <p>For straight-line axes (horizontal or vertical), the viewport is similar to that of a {@link LineNode}, except
    * that "user" units may apply to the X-coordinate. Keep in mind that this is a description of the axis viewport IAW 
    * <i>DataNav</i> conventions, in which the origin lies at the viewport's bottom-left corner and the Y-axis increases
    * upward.
    * <ul>
    *    <li>Let L be the length of the axis line in milli-inches. The axis viewport is square with a side dimension of
    *    L. The axis line itself lies along the top edge of this viewport, ie, along the line y=100%, or y=L. The 
    *    viewport origin is at the bottom-left corner, and the viewport's x-axis (y=0) lies along its bottom edge, 
    *    with the x-coordinate increasing to the right in the direction of the axis line's ending point. The y-axis 
    *    increases upward.</li>
    *    <li>The viewport supports "user" units along its x-axis only, which is always parallel to the axis line 
    *    itself.</li>
    *    <li>If the axis is logarithmic, the viewport handles the logarithmic transformation of user units.</li>
    * </ul>
    * </p>
    * 
    * <p>For the theta axis of a polar plot: While rendering a circular arc is no big deal, typical polar plots almost 
    * never show an axis line for this coordinate. Instead, polar plots usually have a grid visible, with radial lines 
    * drawn IAW the tick set of the theta axis, and circular arcs drawn with radii specified by the tick set of the 
    * radial axis. Therefore, we have chosen not to render the theta axis. Any tick sets defined on a theta axis will 
    * also not be rendered. The axis viewport for a theta axis is undefined by convention, and this method will return 
    * null for such an axis.</p>
    * 
    * <p>The method also returns null if this axis node currently lacks a parent graph, or if the axis origin is 
    * ill-defined, or if its real length is zero.</p>
    */
   @Override public FViewport2D getViewport()
   {
      // if the axis lacks a parent graph or is a theta axis, then the axis viewport is not defined!
      GraphNode g = (GraphNode) getParentGraph();
      if(g == null || isTheta()) return(null);

      // get validated axis range endpoints. If they're the same, then the axis cannot have a viewport
      double start = getValidatedStart();
      double end = getValidatedEnd();
      if(start == end) return(null);
   
      // get axis length in milli-inches; this is size of the axis's square viewport
      double len = getLengthInMilliInches();
      if(len == 0) return(null);

      // whether axis is linear or log, cartesian or the radial axis of a polar plot, its viewport is always oriented 
      // with the axis line along the top edge, starting point of axis range at the bottom left and ending point of 
      // range at bottom right. Thus, the horizontal dimension supports user units, but the vertical dimension does not.
      return( new FViewport2D( len, len, start, end, 0.0, len, isLogarithmic(), false ) );
   }

   /**
    * An axis is not rendered if it lacks a parent graph, if its <i>hide</i> property is set, or if it currently 
    * represents the theta axis of a polar plot.
    */
   @Override protected boolean isRendered()
   {
      GraphNode g = (GraphNode) getParentGraph();
      return( !(g == null || getHide() || isTheta()) );
   }

   @Override protected void releaseRenderResourcesForSelf()
   {
      labelPainter = null;
      rBoundsSelf = null;
   }

   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null)
      {
         if(rBoundsSelf == null) 
            rBoundsSelf = new Rectangle2D.Double();

         GraphNode g = (GraphNode) getParentGraph();
         if((g == null) || getHide() || isTheta()) 
            rBoundsSelf.setRect(0, 0, 0, 0);
         else
         {
            double axisLen = getLengthInMilliInches();
            double adj = getAxisEndpointAdjustment();
            double strokeW = isStroked() ? getStrokeWidth() : 0;
            if(getStrokeEndcap() != StrokeCap.BUTT) adj += strokeW/2.0;
            rBoundsSelf.setRect(-adj,axisLen-strokeW/2, axisLen+adj, strokeW);
            
            updateLabelPainter();
            labelPainter.updateFontRenderContext(g2d);
            labelPainter.invalidateBounds();
            Utilities.rectUnion(rBoundsSelf, labelPainter.getBounds2D(null), rBoundsSelf);
         }
      }
      return((Rectangle2D) rBoundsSelf.clone());
   }

   /**
    * The method transforms a <i>copy</i> of the provided graphics context to set up the axis viewport. It then 
    * renders the axis line itself, the axis label, and any child nodes -- in that order. Horizontal and vertical axes 
    * are rendered similarly, except that a vertical axis line "starts" at its bottom end point and includes a rotation 
    * of -90deg WRT the parent container. ONLY straight-line axes are rendered by this method. Currently, there is no 
    * support for rendering the theta axis of a polar graph.
    */
   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(!(isRendered() && needsRendering(task)))
         return(true);

      // get axis location, length, and orientation WRT enclosing graph viewport
      Point2D origin = getOriginInMilliInches();
      double axisLen = getLengthInMilliInches();
      boolean isHoriz = isHorizontal();
      if(!(axisLen > 0 && Utilities.isWellDefined(origin)))
         return(true);

      // make sure our label painter exists
      if(labelPainter == null) updateLabelPainter();

      // get adjustment, if any, in tick mark/label locations that coincide with axis endpoints. In certain situations, 
      // the rendered axis line is padded at the endpoints for better alignment/join with orthogonal axis
      double hAdj = getAxisEndpointAdjustment();

      // render axis and its tick sets in a copy of the graphics context, so we do not alter the original
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         // transform graphics context to the axis's viewport.
         g2dCopy.translate(origin.getX(), origin.getY());
         if(!isHoriz) g2dCopy.rotate(Math.PI/2);
         g2dCopy.translate(0,-axisLen);

         // draw the axis line itself at the "top" of the viewport, unless it is not stroked
         if(isStroked()) 
         {
            g2dCopy.setColor( getStrokeColor() );
            g2dCopy.setStroke( getStroke(0) );
            g2dCopy.draw( new Line2D.Double(-hAdj, axisLen, axisLen+hAdj, axisLen) );
         }

         labelPainter.render(g2dCopy, null);

         // now render any children
         if(!renderSubordinates(g2dCopy, task))
            return(false);
      }
      finally { if(g2dCopy != null) g2dCopy.dispose(); }

      return(task == null || task.updateProgress());
   }

   //
   // Internal rendering resources
   //

   /**
    * Cached rectangle bounding only the marks made by this axis node -- the axis line and the automated axis label. An 
    * empty rectangle indicates that the element makes no marks when "rendered". If null, the rectangle has yet to be 
    * calculated.
    */
   private Rectangle2D rBoundsSelf = null;

   /** This painter renders the axis's automated text label, if it has one. It supports multi-line labels. */
   private TextBoxPainter labelPainter = null;

   /**
    * Update the internal {@link TextBoxPainter} that renders this axis node's automated label WRT the axis viewport. 
    * This painter supports both single-line and multi-line labels. By using the same implementation for both use cases,
    * we are ensured that the axis labels of adjacent graphs align nicely even if one label is single-line while the 
    * other is multi-line. [This was not the case in a prior release, in which a single-line text painter was used when
    * the axis label text contained no line breaks.]
    * 
    * <p>The text box painter is configured so that the baseline of the last text line (for an axis above or left of the
    * parent graph) or the top of the first text line (for an axis below or right of the graph) lies at the appropriate
    * offset from the axis line. The text box's background and border are not painted, and the text lines are centered.
    * The text box width is TWICE the length of the axis, to allow text lines to be be longer than the rendered axis. 
    * If any text line is longer than twice the axis length, that line will be split into two or more lines as needed.
    * The painter will make no marks if there is no label to render or if the axis itself is currently not rendered.</p>
    */
   private void updateLabelPainter()
   {
      // get parent graph, axis length and orientation WRT graph viewport
      GraphNode g = (GraphNode) getParentGraph();
      double axisLen = getLengthInMilliInches();
      boolean isAboveOrLeft = isAboveLeft();
      double offsetMI = labelOffset.toMilliInches();

      // create on first use
      if(labelPainter == null)
      {
         labelPainter = new TextBoxPainter();
         labelPainter.setStyle(this);
         labelPainter.setBorderStroked(false);
      }

      // configure based on current state
      if((g == null) || getHide() || isTheta())
      {
         labelPainter.setText((String)null);
      }
      else
      {
         // BL corner of text box is located above or below axis line, width = 2 x axis length, height tall enough to
         // accommodate at least one full-height text line (not clipped anyway), no margin
         double bot = axisLen + ((isAboveOrLeft ? 1 : -1) * offsetMI);
         double lineH = getFontSize() * lineHeight;
         if(!isAboveOrLeft) bot -= lineH;
         labelPainter.setBoundingBox(new Point2D.Double(-axisLen/2.0,bot), axisLen*2.0, lineH, 0);
         labelPainter.setText(fromStyledText(getAxisLabel(), getFontFamily(), 
               getFontSizeInPoints(), getFillColor(), getFontStyle(), true));
         labelPainter.setAlignment(TextAlign.CENTERED, isAboveOrLeft ? TextAlign.TRAILING : TextAlign.LEADING);
         labelPainter.setLineHeight(lineHeight);
      }
   }

   
   //
   // PSTransformable implementation
   //

   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      // if axis is hidden or is a theta axis, there's nothing to render!
      if(getHide() || isTheta()) return;

      // get axis location, length, and orientation WRT enclosing graph viewport
      Point2D origin = getOriginInMilliInches();
      double axisLen = getLengthInMilliInches();
      boolean isHoriz = isHorizontal();
      if(!(axisLen > 0 && Utilities.isWellDefined(origin)))
         return;

      // get adjustment, if any, in tick mark/label locations that coincide with axis endpoints.  In certain situations, 
      // the rendered axis line is padded at the endpoints for better alignment/join with orthogonal axis
      double hAdj = getAxisEndpointAdjustment();

      // start the element -- this will update the graphics state IAW this element's stroke/fill colors, etc
      psDoc.startElement( this );

      // establish a new non-clipping viewport for the axis and its children such that that axis line lies between 
      // (0,L) and (L,L), where L is the length of the axis line. This is the same convention as for an ordinary line 
      // element. If the axis is vertical, we have to rotate user space (about the axis's bottom endpt) by 90deg CCW.
      if(isHoriz)
      {
         origin.setLocation(origin.getX(), origin.getY()-axisLen);
         psDoc.translateAndRotate(origin, 0);
      }
      else
      {
         psDoc.translateAndRotate(origin, 90);
         origin.setLocation(0, -axisLen);
         psDoc.translateAndRotate(origin, 0);
      }

      // now draw the axis line as a simple line segment.  Remember that it is rendered WRT to the newly established 
      // axis viewport. Apply adjustment in endpoints so that axis aligns (or joins) nicely with orthogonal axis 
      // (adjustment may be 0).
      Point2D p0 = new Point2D.Double(-hAdj,axisLen);
      Point2D p1 = new Point2D.Double(axisLen+hAdj,axisLen);
      psDoc.renderLine( p0, p1 );

      // render the auto axis label, if present. Again, the label is rendered WRT the axis viewport, where the axis 
      // line itself lies at y=L, NOT y=0! Label is not rendered if text/fill color is transparent (note that Postscript
      // cannot handle transparency). Note that label is rendered as multi-line text even if it does not contain any
      // linebreaks.
      String title = getAxisLabel();
      if((!title.isEmpty()) && getFillColor().getAlpha() > 0)
      {
         double yOffset = labelOffset.toMilliInches();
         TextAlign vAlign = TextAlign.TRAILING;
         if(!isAboveLeft()) 
         {
            yOffset = -yOffset;
            vAlign = TextAlign.LEADING;
         }
         
         // shift origin to the bottom left corner of the tight bounding box for the multi-line axis label. Since
         // we still have to render any tick mark sets, preserve the current graphics state first! Note that the
         // text box width is TWICE the axis length, centered along that length.
         psDoc.saveGraphicsState();
         double lineH = getFontSize() * lineHeight;
         if(!isAboveLeft()) yOffset -= lineH;
         p0.setLocation(-axisLen/2.0, axisLen + yOffset);
         psDoc.setViewport(p0, axisLen*2.0, lineH, 0, false);
         
         // PSDoc needs net rotation of axis WRT root figure in order to estimate font ascent/descent accurately
         double netR = getNetRotate();
         if(!isHoriz) netR = Utilities.restrictAngle(netR + 90);
         
         // render the multi-line label. It is not clipped, so it will extend beyond the top or bottom edge of the
         // bounding box specified, depending on the vertical alignment.
         psDoc.renderTextInBox(title, axisLen*2.0, lineH, getLineHeight(), netR, TextAlign.CENTERED, vAlign);
         
         // restore graphics state so any tick sets will be rendered in the right place!
         psDoc.restoreGraphicsState();
      }

      // render subordinate nodes (tick sets), with respect to the axis's viewport.
      for(int i=0; i<getChildCount(); i++) getChildAt(i).toPostscript(psDoc);

      // end the element -- restoring the graphics state
      psDoc.endElement();
   }


   //
   // Object
   //

   /**
    * This override ensures that the cloned axis node's internal rendering resources are completely independent of the 
    * resources allocated to this node.
    */
   @Override protected Object clone() throws CloneNotSupportedException
   {
      AxisNode copy = (AxisNode) super.clone();
      copy.labelPainter = null;
      copy.rBoundsSelf = null;
      return(copy);
   }
}
