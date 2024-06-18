package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import com.srscicomp.common.g2dutil.Painter;
import com.srscicomp.common.g2dutil.Projector;
import com.srscicomp.common.g2dutil.SingleStringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dutil.TextBoxPainter;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.LineXY;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.fig.Graph3DNode.BackDrop;

/**
 * <b>Axis3DNode</b> is a required component of the 3D graph object, {@link Graph3DNode}. There is one such node for 
 * each of the three axes -- X, Y, Z. It serves as a container governing the axis range, tick sets, and styling, giving 
 * the author fine-grained control over the appearance of each axis. The 3D axis node renders itself IAW the state of 
 * the parent 3D graph, as well as its own definition. Unlike its 2D counterpart {@link AxisNode}, it does not establish
 * its own viewport. The 3D axis and its tick sets (see {@link Ticks3DNode}) are rendered in the parent graph's 3D 
 * coordinate system, as projected onto the 2D figure canvas.</p>
 * 
 * <p>[NOTE: Since the node does not establish its own "rectangular" viewport, its render bounds and focus shape are
 * computed WRT the parent 3D graph's viewport. The axes are typically rotated WRT that viewport, so the render bounds
 * will not be particularly "tight" to the marks rendered by the 3D axis node.]</p>
 * 
 * <p>The parent graph's 3D-to-2D projection calculations expect that each axis range is specified as <i>[min, max]</i>,
 * where <i>min &lt; max</li>. Any dimension of the 3D graph can be on a logarithmic scale by making the axis range of
 * the corresponding axis logarithmic, in which case <i>min &gt; 0</i>. For these reasons, axis range and linear/log
 * scale are set as a single composite property to ensure the range is always valid -- see {@link #setRange}. This is
 * distinct from the 2D case, where you can change the direction of values along the axis by setting the start of the 
 * range greater than the end. You can always "reverse" the direction of values along the axis by changing the 
 * orientation of the 3D graph.</p>
 * 
 * <p>The 3D axis node's inheritable style properties govern the appearance of the axis line, label, and any tick marks 
 * and labels (unless the child {@link Ticks3DNode} explicitly sets different text/draw styles, of course). The 
 * <i>spacer</i> property specifies the distance (in the 3D coordinate system!) separating the axis line from the 
 * corresponding edge of the 3D graph box, and the <i>labelOffset</i> property sets the distance separating the axis 
 * label from the axis line. The <i>title</i> property gives the axis label; multi-line labels are supported, and the 
 * <i>lineHt</i> property sets the line-to-line spacing in that scenario. Finally, the entire axis (and any child tick 
 * mark sets) can be hidden by setting the <i>hide</i> flag.</p>
 * 
 * <p>The 3D axis node can have any number of {@link Ticks3DNode}s as children, although typically there is only one.
 * These, of course, layout tick marks and associated labels along the axis line, and exist only as children of a 3D 
 * axis. The first such tick set is the major tick set. The major tick set along the X axis defines the locations of 
 * constant-X grid lines in the 3D graph's XY and XZ backplanes, and so on.</p>
 * 
 * <p>As of v5.4.0 (schema version 24), <b>Axis3DNode</b> supports a "styled text" label, in which text color, font
 * style, underline state, superscript, and subscript can vary on a per-character basis. See {@link 
 * FGraphicNode#toStyledText}.</p>
 *
 * @author sruffner
 */
public class Axis3DNode extends FGraphicNode implements Cloneable
{
   /**
    * Construct a 3D axis node. The axis is visible initially, with linear scaling, a range of [0..100], and a default 
    * axis label of "X", "Y" or "Z" (depending on which axis is represented). The axis power scale (for adjusting tick
    * mark labels) defaults to 0 (10^0 = 1 --> no adjustment). The <i>spacer</i> and <i>labelOffset</i> properties are
    * set IAW user preferences. All style properties are implicit, and the <i>lineHt</i> property (for a multi-line axis
    * label) defaults to 0.8. A single tick set is defined initially, with 5 tick marks dividing the axis range.
    * @param which Indicates the 3D graph axis governed by this node.
    */
   public Axis3DNode(Graph3DNode.Axis which)
   {
      super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASTITLEATTR|ALLOWLFINTITLE|ALLOWATTRINTITLE);
      axis = which == null ? Graph3DNode.Axis.X : which;
      setTitle(axis + "-axis Label");
      range[0] = 0;
      range[1] = 100;
      isLog = false;
      log2 = false;
      powerScale = 0;
      spacer = FGNPreferences.getInstance().getPreferredAxisSpacer();
      labelOffset = FGNPreferences.getInstance().getPreferredAxisLabelOffset();
      hide = false;
      lineHeight = 0.8;
      
      insert(new Ticks3DNode(), 0);
   }

   /** Indicates which 3D graph axis is defined by this node. */
   private final Graph3DNode.Axis axis;
   
   /**
    * Get the identity of the 3D graph axis defined by this node.
    * @return The relevant axis in the parent 3D graph.
    */
   public Graph3DNode.Axis getAxis() { return(axis); }
   
   /**
    * Return a reference to the 3D graph object containing this 3D axis node.
    * @return The parent 3D graph, or null if there is no parent.
    */
   final Graph3DNode getParentGraph3D()
   {
      FGraphicNode p = getParent();
      if(p == null) return(null);
      assert(p instanceof Graph3DNode);
      return((Graph3DNode)p);
   }
   
   //
   // Tick sets
   //

   /**
    * Get the 3D tick set node defining the "major tick marks" for this 3D graph axis. By convention, the first {@link 
    * Ticks3DNode} child defines the "major tick marks".
    * @return The major tick mark set for this axis, if it has one; else returns null.
    */
   Ticks3DNode getMajorTickSet() { return(getChildCount() == 0 ? null : (Ticks3DNode)getChildAt(0)); }
   
    /**
    * Does this 3D graph axis possess a major tick mark set? By convention, the first tick set child defined on the 
    * axis constitutes the major tick marks, so this method returns true so long as the axis contains one child.
    * 
    * @return True iff the axis has major tick marks.
    */
   boolean hasMajorTicks() { return(getChildCount() > 0); }

   /**
    * Retrieve the set of major tick marks defined on this 3D graph axis. By convention, the major tick marks are
    * defined by the first tick set child. It may be that an axis has no tick sets defined.
    * 
    * @param omitRangeEnds If true, tick marks located at the axis range end points are omitted. A tick mark T is
    * considered to be "at" a range end point if their difference is less that 0.1% of the axis range.
    * @return The set of major tick mark locations defined on this axis, specified in user units. If there are none, 
    * this method returns an empty array. The tick values are always listed in the array in ascending order.
    */
   double[] getMajorTicks(boolean omitRangeEnds)
   {
      double[] ticks = hasMajorTicks() ? ((Ticks3DNode)getChildAt(0)).getTickSet() : TickSetNode.NOTICKS;
      
      if(ticks.length > 0 && omitRangeEnds)
      {
         // since we know the tick marks are in ascending numerical order with no repeats, and only tick values within
         // the axis range are included, we only have to look at the first and last tick values
         double tolerance = (range[1] - range[0]) * 0.001;
         boolean omitFirst = (Math.abs(range[1]-ticks[0]) < tolerance || Math.abs(range[0]-ticks[0]) < tolerance);
         boolean omitLast = (ticks.length > 1) && (Math.abs(range[1]-ticks[ticks.length-1]) < tolerance);
         
         if(omitFirst || omitLast)
         {
            ticks = Arrays.copyOfRange(ticks, omitFirst ? 1 : 0, ticks.length - (omitLast ? 1 : 0));
         }
      }
      
      return(ticks);
   }


   //
   // Support for child nodes 
   //
   @Override public FGNodeType getNodeType() { return(FGNodeType.AXIS3D); }
   /** The 3D axis admits any number of 3D tick set nodes as children. */
   @Override public boolean canInsert(FGNodeType nodeType) { return(nodeType==FGNodeType.TICKS3D); }
   /** A 3D grid line node is an intrinsic component of its parent {@link Graph3DNode}.*/
   @Override public boolean isComponentNode() { return(true); }

   
   //
   // Properties
   //
   
   /** The axis range ("user" units): [A, B], where A must be less than B. */
   private final double[] range = new double[] {0, 100};

   /** True if axis and corresponding dimension in the 3D graph are on a logarithmic (base 10) scale; else linear. */
   private boolean isLog;
   
   /** 
    * Flag indicates whether logarithmic axis is presented as base2 (true) or base10 (false). This affects rendering
    * of child tick sets -- but ONLY when the axis is logarithmic.
    */
   private boolean log2;

   /**
    * Get the minimum value of the coordinate range spanned by this 3D axis.
    * @return The minimum of the axis range, in the native units associated with that axis ("user" units).
    */
   public double getRangeMin() { return(range[0]); }
   /**
    * Get the maximum value of the coordinate range spanned by this 3D axis.
    * @return The maximum of the axis range, in the native units associated with that axis ("user" units).
    */
   public double getRangeMax() { return(range[1]); }
   /** 
    * Does this axis use a logarithmic scale? If so, coordinates in the corresponding dimension in the parent 3D graph
    * also follow a logarithmic scale. 
    * @return True if axis is logarithmic.
    */
   public boolean isLogarithmic() { return(isLog); }
   
   /**
    * Is the axis's logarithmic base 2? This property affects rendering of child tick sets -- but ONLY when axis is 
    * logarithmic. Changing this property has no effect on rendering when the axis is scaled linearly.
    * 
    * @return True if logarithmic base is 2; otherwise, it is log base 10.
    */
   public boolean isLogBase2() { return(log2); }

   /**
    * Set the range for this 3D axis. 
    * @param min The minimum of the range. 
    * @param max The maximum of the range.
    * @param log True if axis is logarithmic scale; false if linear.
    * @return True if successful, false if either value is ill-defined. If the specified range does not satisfy <i>min 
    * &lt; max</i> -- 0 &lt; min &lt; max if axis is logarithmic -- , it will be silently auto-corrected.
    */
   public boolean setRange(double min, double max, boolean log)
   {
      if(!(Utilities.isWellDefined(min) && Utilities.isWellDefined(max))) return(false);
      
      if(min == max) max = min + 1;
      else if(min > max) { double t = min; min = max; max = t; }         
      if(log && min <= 0)
      {
         min = 0.01;
         if(min > max) max = 1;
      }
     
      if(min == range[0] && max == range[1] && log == isLog) return(true);
      
      if(doMultiNodeEdit(FGNProperty.RANGE, new double[] {min, max, log ? 1 : 0})) return(true);
      
      double[] old = new double[] {range[0], range[1], isLog ? 1 : 0};
      range[0] = min;
      range[1] = max;
      isLog = log;
      if(areNotificationsEnabled())
      {
         onNodeModified(FGNProperty.RANGE);
         FGNRevEdit.post(this, FGNProperty.RANGE, new double[] {range[0], range[1], isLog ? 1:0}, old, 
               "Change 3D " + axis.toString() + "-axis range");
      }

      return(true);
   }

   /**
    * Select the logarithmic base for this axis. If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param b True for base 2, false for base 10.
    */
   public void setLogBase2(boolean b)
   {
      if(log2 != b)
      {
         if(doMultiNodeEdit(FGNProperty.LOG2, b)) return;
         
         Boolean old = log2;
         log2 = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.LOG2);
            String desc = "Set 3D " + axis + "-axis logarithmic base = " + (log2 ? "2" : "10");
            FGNRevEdit.post(this, FGNProperty.LOG2, log2, old, desc);
         }
      }
   }

   /** Axis scale factor: Exponent N such that axis tick mark labels are scaled by 10^N */
   private int powerScale;

   /**
    * Get the axis power-of-10 scale factor affecting the scaling of tick mark labels along the axis.
    * <p>
    * When the endpoints of the 3D axis range are very small (e.g., 1e-6) or very large (1e10), the tick mark labels 
    * along the axis tend to get long and are best presented in scientific notation. In that case, each tick label 
    * includes an exponent: "1e-6 2e-6 3e-6 ...". 
    * 
    * <p>To achieve a more compact rendering of the axis, we could divide each tick value by a power of 10, then 
    * format the result: "1 2 3 ...", assuming a scale factor of 1e-6. The common scale factor can then be included in 
    * the axis label for clarity: "label string (x1E-6)".</p>.
    * 
    * <p>Rather than automating the selection of the scale factor, it is a user-controlled property -- as is the case
    * for 2D graph axes -- see {@link FGNGraphAxis}. Any subordinate tick sets will call this method when formatting
    * tick mark labels, and {@link #getAxisLabel()} will include the scale factor in the axis label if N != 0.</p>
    * 
    * @return Exponent of the power-of-10 scale factor for this axis, as described. 
    */
   public int getPowerScale() { return(powerScale); }

   /**
    * Set the axis scale factor -- an integer exponent N such that numeric axis tick mark labels are scaled by 10^N.
    * If a change was made, {@link #onNodeModified} is invoked.
    * 
    * @param n New value for scale factor exponent. Rejected if outside allowed range: {@link 
    * FGNGraphAxis#MINPOWERSCALE} .. {@link FGNGraphAxis#MAXPOWERSCALE}
    * @return True if successful; false if value rejected.
    */
   public boolean setPowerScale(int n)
   {
      if(n < FGNGraphAxis.MINPOWERSCALE || n > FGNGraphAxis.MAXPOWERSCALE) return(false);
      if(powerScale != n)
      {
         if(doMultiNodeEdit(FGNProperty.SCALE, n)) return(true);
         
         Integer old = powerScale;
         powerScale = n;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.SCALE);
            FGNRevEdit.post(this, FGNProperty.SCALE, (double) powerScale, old, "Change 3D axis power scale");
         }
      }
      return(true);
   }

   /**
    * Get the label string for this 3D axis. Generally, this is simply the string returned by {@link #getTitle()}. 
    * However, if the axis scale factor is not unity <b>and the title string is non-empty</b>, then the scale factor is
    * appended to the title string to form the axis label. Let N be the non-zero base-10 exponent returned by {@link #
    * getPowerScale()}. If 0 < N < 4, then the method appends "(xS)", where S=10^N. Otherwise, "(x10^N)" is appended.
    * This serves to clarify that any tick mark labels along the axis are scaled accordingly. <b>NOTE that the scale
    * factor is not included if the title string is empty, which could confuse the reader!</b>
    * 
    * <p>The label will be formatted as <i>FypML</i> attributed text if {@link #getTitle()} returns attributed text,
    * or if the suffix "(x10^N)" is appended to a plain-text title (in order to render the N as superscript.</p>
    * 
    * @return The axis label string, as described.
    */
   final String getAxisLabel()
   {
      String s = getTitle().trim();
      int exp = getPowerScale();
      if(exp != 0 && !s.isEmpty())
      {
         if(hasAttributedTextInTitle())
         {
            // base axis label uses attributed text: split text from attr codes before adding suffix. For the suffix,
            // we turn off underlining and super/subscript, use plain font style, and restore default text color. But
            // we superscript the exponent, if any.
            int idx = s.lastIndexOf('|');
            String label = s.substring(0, idx);
            String codes = s.substring(idx+1);
            if(exp > 0 && exp < 4)
               s = String.format("%s (\u00d7%d)|%s,%d:-unp", label, (int)Math.pow(10,exp), codes, idx);
            else
            {
               s = String.format("%s (\u00d710%d)|%s,%d:-unp,%d:S", label, exp, codes, idx, idx+5);
            }
         }
         else if(exp > 0 && exp < 4)
            s = String.format("%s (\u00d7%d)", s, (int) Math.pow(10, exp));
         else
            s = String.format("%s (\u00d710%d)|%d:S", s, exp, s.length() + 5);  // use attr text to superscript exp!
      }
      return(s);
   }


   /**
    * The perpendicular distance between the axis line and the corresponding edge of the 3D graph box. This is the
    * distance in 3D, not in the 2D projection. Specified as a linear measurement with associated units; relative units 
    * not allowed.
    */
   private Measure spacer;

   /**
    * Get the perpendicular distance between the axis line and the corresponding edge of the 3D graph box. Note that
    * this is the distance in 3D, not in the 2D projection!
    * 
    * @return The axis spacer, specified as a linear measurement with associated units.
    */
   public Measure getSpacer() { return(spacer); }

   /**
    * Set the perpendicular distance between the axis line and the corresponding edge of the 3D graph box. Note that
    * this is the distance in 3D, not in the 2D projection! If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param m The new axis spacer distance. The measure will be constrained to satisfy {@link 
    * AxisNode#SPACERCONSTRAINTS}.  A null value is rejected. 
    * @return True if change was accepted; false if rejected.
    */
   public boolean setSpacer(Measure m)
   {
      if(m == null) return(false);
      m = AxisNode.SPACERCONSTRAINTS.constrain(m);

      boolean changed = (spacer != m) && !Measure.equal(spacer, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.SPACER, m)) return(true);
         
         Measure old = spacer;
         spacer = m;
         if(areNotificationsEnabled())
         {
            if(old.toMilliInches() != spacer.toMilliInches()) onNodeModified(FGNProperty.SPACER);
            FGNRevEdit.post(this, FGNProperty.SPACER, spacer, old, "Set 3D axis line spacer to " + spacer.toString());
         }
      }
      return(true);
   }

   /** 
    * The perpendicular distance separating the axis label from the axis line. This is the  distance in 3D, not in the 
    * 2D projection. Specified as a linear measurement with associated units; relative units not allowed.
    */
   private Measure labelOffset;

   /**
    * Get the perpendicular distance between the axis line and the bottom or top of the axis label (whichever is 
    * closer). Note that this is the distance in 3D, not in the 2D projection!
    * 
    * @return The axis label offset, specified as a linear measurement with associated units.
    */
   public Measure getLabelOffset() { return(labelOffset); }

   /**
    * Set the perpendicular distance between the axis line and the bottom or top of the axis label (whichever is 
    * closer). Note that this is the distance in 3D, not in the 2D projection! If a change is made, {@link 
    * #onNodeModified} is invoked.
    * 
    * @param m The new axis label offset. The measurement will be constrained to satisfy {@link 
    * AxisNode#SPACERCONSTRAINTS}. A null value is rejected.
    * @return True if change was accepted; false otherwise
    */
   public boolean setLabelOffset(Measure m)
   {
      if(m == null) return(false);
      m = AxisNode.SPACERCONSTRAINTS.constrain(m);

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
                  "Set 3D axis label offset to " + labelOffset.toString());
         }
      }
      return(true);
   }

   /** Flag determines whether or not this 3D axis is rendered. */
   private boolean hide;

   /**
    * Get the hide state for this 3D axis.
    * 
    * @return True if the axis is currently hidden.
    */
   public boolean getHide() { return(hide); }

   /**
    * Set the hide state for this 3D axis. If a change is made, {@link #onNodeModified} is invoked.
    * 
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
            desc += "3D " + axis.toString() + "-axis";
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
    * Text line height, as a fraction of the element's font size. Applicable only when the auto-generated axis label is
    * multi-line. Range-restricted to [0.8, 3.0].
    */
   private double lineHeight;

   /**
    * Get the text line height for the 3D axis label, in the event it is laid out in two or more text lines.
    * 
    * @return The text line height <i>as a fraction of the current font size</i>. Range-restricted to [0.8, 3.0].
    */
   public double getLineHeight() {  return(lineHeight); }

   /**
    * Set the text line height for the 3D axis label, in the event it is laid out in two or more text lines.
    * 
    * @param lh The text line height <i>as a fraction of the current font size</i>. Range-restricted to [0.8, 3.0].
    * @return True if successful; false if argument is NaN. Any out-of-range value will be silently corrected.
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
         case RANGE: 
            double[] rng = (double[]) propValue;
            ok = setRange(rng[0], rng[1], rng[2] > 0);
            break;
         case LOG2: setLogBase2((Boolean) propValue); ok = true; break;
         case SCALE: ok = setPowerScale((Integer) propValue); break;
         case LABELOFFSET: ok = setLabelOffset((Measure) propValue); break;
         case SPACER: ok = setSpacer((Measure) propValue);  break;
         case HIDE: setHide((Boolean) propValue); ok = true; break;
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
         case RANGE : value = new double[] {getRangeMin(), getRangeMax(), isLogarithmic() ? 1 : 0}; break;
         case LOG2: value = isLogBase2(); break;
         case SCALE: value = getPowerScale(); break;
         case LABELOFFSET: value = getLabelOffset(); break;
         case SPACER: value = getSpacer();  break;
         case HIDE: value = getHide(); break;
         case LINEHT : value = getLineHeight(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }
   
   @Override protected void onNodeModified(Object hint)
   {
      Graph3DNode g3 = getParentGraph3D();
      if(g3 == null) return;
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      if(hint == null || hint == FGNProperty.RANGE)
      {
         // if axis range changed, tick mark locations are affected
         for(int i=0; i<getChildCount(); i++) ((Ticks3DNode) getChildAt(i)).computeTickLocations(true);
         
         // this call triggers an update of the rendering infrastructure of the 3D graph and all of its subordinates
         g3.onNodeModified(FGNProperty.WIDTH);
      }
      else if(hint == FGNProperty.LOG2)
      {
         // changing the log base when the axis is logarithmic affects tick mark locations. If the axis is linear, then
         // there's no effect on the rendering.
         if(isLogarithmic())
         {
            for(int i=0; i<getChildCount(); i++) ((Ticks3DNode) getChildAt(i)).computeTickLocations(true);
            
            if(g3.getBackDrop() == BackDrop.HIDDEN || getHide())
               model.onChange(this,  0, false, null);
            else
               super.onNodeModified(hint);
         }
         else
            model.onChange(this, 0, false, null);
      }
      else
      {
         if((g3.getBackDrop() == BackDrop.HIDDEN) || (hint!=FGNProperty.HIDE && getHide()))
            model.onChange(this,  0, false, null);
         else
            super.onNodeModified(hint);
      }
   }

   /**
    * This override handles a number of special cases and side-effects:
    * <ul>
    * <li>When first tick set is inserted, representing the axis's major tick marks, the parent 3D graph is notified if 
    * its back drop style includes grid lines.</li>
    * <li>If the 3D axis is currently not rendered, then adding a subordinate does not affect the rendering of the axis.
    * Otherwise, the rendering infrastructure is updated via the super-class method.</li>
    * <li>If the 3D axis currently lacks a parent 3D graph, the method does nothing. An axis is only renderable in the 
    * context supplied by a parent graph.</li>
    * </ul>
    */
   @Override protected void onSubordinateNodeInserted(FGraphicNode sub)
   {
      Graph3DNode g3 = getParentGraph3D();
      if(g3 == null) return;
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      if(isRendered()) super.onSubordinateNodeInserted(sub);
      else model.onChange(this, 1, false, null);

      // this call triggers an update of the rendering infrastructure of the 3D graph and all of its subordinates
      if(getChildCount() == 1 && g3.hasGridLines(axis))
         g3.onNodeModified(FGNProperty.WIDTH); 
   }

   /**
    * This override handles a number of special cases and side-effects:
    * <ul>
    * <li>If a tick set is removed, the parent 3D graph is notified if its backdrop style includes grid lines. If it had
    * been the major tick set (we don't know because it has already been removed), the corresponding grid lines must be 
    * updated accordingly.</li>
    * <li>If the 3D axis is currently not rendered, then removing a subordinate tick set does not affect its rendering.
    * Otherwise, the rendering infrastructure is updated via the super-class method.</li>
    * <li>If the 3D axis currently lacks a 3D parent graph, the method does nothing. An axis is only renderable in the 
    * context supplied by a parent graph.</li>
    * </ul>
    */
   @Override protected void onSubordinateNodeRemoved(FGraphicNode sub)
   {
      Graph3DNode g3 = getParentGraph3D();
      if(g3 == null) return;
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      if(isRendered()) super.onSubordinateNodeRemoved(sub);
      else model.onChange(this, 1, false, null);

      // this call triggers an update of the rendering infrastructure of the 3D graph and all of its subordinates
      if(g3.hasGridLines(axis)) g3.onNodeModified(FGNProperty.WIDTH);
   }

   @Override protected void rescaleSelf(double scale, MultiRevEdit undoer)
   {
      Measure.Constraints c = AxisNode.SPACERCONSTRAINTS;
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
   
   // 
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }
   
   /** 
    * The node-specific properties exported in a 3D axis node's style set are the label offset and spacer between axis 
    * line and graph, and the text line height (impacts a multi-line axis label). Axis range, logarithmic state, and
    * power scale are NOT included.
    * 
    * <p>In addition, for each 3D tick set child defined on the 3D axis, a component style set is added to the 3D axis 
    * node's style set. Technically, tick sets are not components of an axis, but they are part of the axis's rendered
    * appearance, so we include them in this fashion.</p>
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.SPACER, getSpacer());
      styleSet.putStyle(FGNProperty.LABELOFFSET, getLabelOffset());
      styleSet.putStyle(FGNProperty.LINEHT, getLineHeight());

      for(int i=0; i<getChildCount(); i++)
      {
         FGNStyleSet tickSS = getChildAt(i).getCurrentStyleSet();
         styleSet.addComponentStyleSet(tickSS);
      }
   }

   /**
    * Updates the 3D axis node's label offset, spacer, and line height properties when the applied style set originated
    * from another 3D axis node, or a 2D axis or color axis node (since the properties have the same meaning for the 
    * 2D axes). In addition, it also styles each 3D tick set child for which there's a corresponding tick mark styling 
    * component in the applied style set. This is not technically correct usage of the style set feature, since tick
    * mark sets are not true components of a 3D or 2D axis node.
    */
   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      // the label offset, spacer and line height properties mean the same thing on a 3D axis, a 2D axis, or a 2D 
      // "color" axis. If the applied style set's source node type is not one of these, then the applied property will
      // be rejected.
      FGNodeType nt = applied.getSourceNodeType();
      if(!(nt.equals(FGNodeType.AXIS) || nt.equals(FGNodeType.CBAR))) nt = getNodeType();
      
      Measure m = (Measure) applied.getCheckedStyle(FGNProperty.SPACER, nt, Measure.class);
      if(m != null) m = AxisNode.SPACERCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.SPACER, null, Measure.class)))
      {
         spacer = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.SPACER);

      m = (Measure) applied.getCheckedStyle(FGNProperty.LABELOFFSET, nt, Measure.class);
      if(m != null) m = AxisNode.SPACERCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure)restore.getCheckedStyle(FGNProperty.LABELOFFSET,null,Measure.class)))
      {
         labelOffset = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LABELOFFSET);
      
      Double lineHt = (Double) applied.getCheckedStyle(FGNProperty.LINEHT, nt, Double.class);
      if(lineHt != null && !lineHt.equals(restore.getStyle(FGNProperty.LINEHT)))
      {
         lineHeight = Utilities.rangeRestrict(0.8, 3.0, lineHt);
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LINEHT); 

      // special case: If style set source is an axis of any kind, apply its tick mark set styling to this axis.
      if(nt == FGNodeType.AXIS3D || nt == FGNodeType.AXIS || nt == FGNodeType.CBAR)
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
   // Focusable/Renderable/PSTransformable support
   //
   /** The 3D axis node renders directly into the parent 3D graph, so the identity transform is returned. */
   @Override public AffineTransform getLocalToParentTransform() { return(new AffineTransform()); }
   /** The 3D axis node renders directly into the parent 3D graph, so this method returns the parent viewport. */
   @Override public FViewport2D getViewport() { return(getParentViewport()); }

   @Override protected void releaseRenderResourcesForSelf() 
   {
      rBoundsSelf = null;
      labelPainter = null;
   }

   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null)
      {
         if(!isRendered()) 
            rBoundsSelf = new Rectangle2D.Double();
         else
         {
            updateRenderingResources();
            rBoundsSelf = isStroked() ? axisLine.getBounds2D() : new Rectangle2D.Double();
            if(isStroked())
            {
               // accounting for non-zero stroke width
               double sw = getStrokeWidth();
               rBoundsSelf.setFrame(rBoundsSelf.getX()-sw/2, rBoundsSelf.getY()-sw/2, 
                     rBoundsSelf.getWidth()+sw, rBoundsSelf.getHeight()+sw);
            }
            labelPainter.updateFontRenderContext(g2d);
            labelPainter.invalidateBounds();
            Utilities.rectUnion(rBoundsSelf, labelPainter.getBounds2D(null), rBoundsSelf);
         }
      }
      return((Rectangle2D) rBoundsSelf.clone());
   }
   
   /**
    * A 3D axis is not rendered if the 3D graph's backdrop is currently hidden, or if the 3D axis itself is currently 
    * hidden.
    */
   @Override protected boolean isRendered()
   {
      Graph3DNode g3 = getParentGraph3D();
      return( !(g3 == null || g3.getBackDrop() == BackDrop.HIDDEN || getHide()) );
   }

   public boolean render(Graphics2D g2d, RenderTask task) 
   { 
      if(!(isRendered() && needsRendering(task)))
         return(true);

      // make sure our label painter exists
      if(labelPainter == null) updateRenderingResources();

      // render axis and its tick sets in a copy of the graphics context, so we do not alter the original
      // NOTE: We don't transform graphics context because axis and tick sets are drawn in 3D graph's coords.
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         // draw the axis line itself, unless it is not stroked
         if(isStroked()) 
         {
            g2dCopy.setColor( getStrokeColor() );
            g2dCopy.setStroke( getStroke(0) );
            g2dCopy.draw(axisLine);
         }

         // draw the axis label, if there is one. When exporting to PDF, must handle font substitution in case chosen 
         // font does not handle one or more characters in the label string.
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

   /** The 3D box edge adjacent and parallel to the axis. Depends on backdrop style and 3D graph orientation. */
   private Projector.Edge adjEdge = null;
   
   /** 
    * A 3D box edge perpendicular to axis. It determines orientation of any tick marks and, for the box backdrop styles,
    * the location of the label offset from the axis. Depends on backdrop style and 3D graph orientation.
    */
   private Projector.Edge perpEdge = null;
   
   /** Line segment primitive representing the rendered axis line, in the parent 3D graph's viewport coordinates. */
   private Line2D axisLine = new Line2D.Double();
   
   /**
    * Cached rectangle bounding the marks made by the axis line and the automated axis label. Specified in the parent
    * 3D graph's viewport, since the 3D axis does not define its own viewport. An empty rectangle indicates that the 
    * element makes no marks when "rendered". If null, the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;

   /** 
    * This painter renders the axis's automated text label, if it has one. One of two possible painter implementations
    * are used, depending on whether or not the label string contains any interior line breaks. 
    */
   private Painter labelPainter = null;
   
   /**
    * Helper method updates internal infrastructure used to render this 3D axis line segment. It must be invoked
    * when certain axis properties are changed, or when the parent 3D graph's backdrop style or projection geometry
    * changes. 
    * 
    * <p>When the backdrop is hidden, the graph axes are not rendered at all, and this method takes no action. When
    * the backdrop is {@link BackDrop#AXESBACK}, the axes are drawn emanating in the cardinal directions (in 3D) from
    * the back corner of the 3D graph box. Each axis label is located beyond the axis line, offset along the line IAW
    * the "labelOffset" property. In this case, the labels are never rotated, but their location and H and V alignment 
    * are set for the most sensible appearance at any orientation of the 3D graph.</p>
    * 
    * <p>For all other backdrop styles, the axes are drawn parallel to a respective outer edge of the 3D box, separated 
    * from it IAW the "spacer" property. The corresponding label is centered horizontally about a point offset from the
    * axis line segment IAW the "labelOffset" property; it is top-aligned if it is "below" or "right" of the axis line,
    * else it is bottom-aligned. The label is rotated so that it is roughly parallel to axis line.</p>
    * 
    * <p>For consistency with Matlab, the Z axis is always located on a left edge of the 3D graph box, while the X and
    * Y axes are always located along a bottom edge. The selected edge is maintained internally in {@link #adjEdge}. In 
    * 3D space, the axis line segment may be offset (via the  <i>spacer</i> property) in either of two orthogonal 
    * directions, corresponding to one of the edges of the 3D graph box. The perpendicular edge chosen minimizes the 
    * distance <b>in 2D space</b> between an endpoint of the offset line segment and the corresponding corner of the 3D 
    * graph box; at certain extreme orientations, the distance can get quite large in one perpendicular direction while
    * being reasonably close to the desired spacing in the other direction. The chosen perpendicular edge is maintained 
    * internally in {@link #perpEdge}.</p>
    * 
    * <p>When the axis label has more than one text line, it is rendered in the same manner as a text box. The {@link 
    * TextBoxPainter}'s properties are set so that the multi-line label's presentation is similar to that of a single-
    * line label.</p>
    * 
    * <p>NOTE that the axis representation at elevation angles exceeding +/-60 degrees did not look very good, so we
    * decided to limit the allowed range of the 3D graph's elevation angle to [-60..60] degrees.</p>
    */
   private void updateRenderingResources()
   {
      // don't bother if axis is not rendered
      if(!isRendered()) return;
      
      Graph3DNode g3 = getParentGraph3D();
      assert g3 != null;
      BackDrop backDrop = g3.getBackDrop();
      Projector prj = g3.get2DProjection();
      
      // is this a multi-line label?
      String label = getAxisLabel();
      boolean isMulti = !(label.indexOf('\n') == -1);
      
      // for a multi-line label, we need to set a width for the textbox containing it. Since an axis could be very
      // foreshortened in certain perspectives, we use the length of the corresponding dimension of the 3D graph box.
      double multiLabelMaxW;
      switch(axis)
      {
      case X : multiLabelMaxW = g3.getWidth().toMilliInches(); break;
      case Y : multiLabelMaxW = g3.getHeight().toMilliInches(); break;
      default: multiLabelMaxW = g3.getDepth().toMilliInches(); break;
      }
      
      double ofsMI = labelOffset.toMilliInches();

      Point2D p1 = new Point2D.Double();
      Point2D p2 = new Point2D.Double();
      Point2D pLabel = new Point2D.Double();
      TextAlign hAlign = TextAlign.CENTERED;
      TextAlign vAlign = TextAlign.LEADING;
      double labelRot = 0;
      LineXY axisInXYPlane;
      
      // calculate endpoints of axis line segment. This also finds the box edge adjacent the axis line and one 
      // perpendicular to it (in 3D space).
      getProjectedAxisLineSegment(p1, p2);
      axisInXYPlane = new LineXY(p1, p2);
      axisLine.setLine(p1,p2);
      
      // get label location, H & V alignment...
      if(backDrop == BackDrop.AXESBACK)
      {
         pLabel.setLocation(p2);
         axisInXYPlane.getPointAlongLine(pLabel, ofsMI, p1, false);
         
         // NOTE: This is based on trial-and-error!
         double rot = Math.atan2(p2.getY()-p1.getY(), p2.getX()-p1.getX()) * 180.0 / Math.PI;
         double rotAbs = Math.abs(rot);
         if(rotAbs <= 30) 
         { 
            hAlign = TextAlign.LEADING; 
            vAlign = TextAlign.CENTERED; 
         }
         else if(rotAbs <= 150)
         {
            vAlign = (rot < 0) ? TextAlign.LEADING : TextAlign.TRAILING;
         }
         else
         {
            hAlign = TextAlign.TRAILING;
            vAlign = TextAlign.CENTERED;
         }
         
         // when label is multi-line, we have to find BL corner of text box.
         if(isMulti)
         {
            double ofsX = (hAlign == TextAlign.LEADING) ? 0 : multiLabelMaxW;
            if(hAlign == TextAlign.CENTERED) ofsX /= 2.0;
            double ofsY = (vAlign == TextAlign.LEADING) ? getFontSize() * lineHeight : 0;
            pLabel.setLocation(pLabel.getX() - ofsX, pLabel.getY() - ofsY);
         }
      }
      else
      {
         // compute label midpoint: Find line perpendicular to axis line segment in 2D space and passing through the
         // midpoint of the line segment. Then find point offset along that line, away from the inner corner of the
         // 3D box, IAW label offset. NOTE that for a multi-line label, if the label is below the axis, we have to 
         // further offset the label baseline by the height of one text line.
         Point2D pRef = prj.project(prj.getBackSideX(), prj.getBackSideY(), prj.getBackSideZ(), null);
         boolean above = axisInXYPlane.liesAbove(pRef.getX(), pRef.getY());
         
         pLabel.setLocation((p1.getX()+p2.getX())/2.0, (p1.getY()+p2.getY())/2.0);
         LineXY perpLine = axisInXYPlane.getPerpendicularLine(pLabel.getX(), pLabel.getY());
         perpLine.getPointAlongLine(pLabel, ofsMI + ((above || !isMulti) ? 0 : getFontSize()*lineHeight), pRef, false);
         
         // label orientation and vertical alignment. 
         // (NOTE: Took a lot of trial and error to find this solution!)
         labelRot = Math.atan2(axisLine.getY2()-axisLine.getY1(), 
               axisLine.getX2()-axisLine.getX1()) * 180.0 / Math.PI;
         if(axis == Graph3DNode.Axis.Z)
         {
            // ensures that Z axis label always reads "upwards", regardless which side of the 3D box it's on
            // (given the limits in elevation angle, the projected Z-axis is always near vertical).
            if(labelRot < 0) labelRot += 180;
            if(!axisInXYPlane.liesLeft(pLabel.getX(), pLabel.getY())) vAlign = TextAlign.TRAILING;
         }
         else
         {
            if(Math.abs(labelRot) > 90) 
               labelRot = Utilities.restrictAngle(labelRot+180);
            if(!axisInXYPlane.liesAbove(pLabel.getX(), pLabel.getY())) 
               vAlign = TextAlign.TRAILING;
         }

         // for a multi-line label, we have to shift it to the bottom-left corner of the text box bounds, given that 
         // the width of the text box is set to the corresponding dimension of the 3D graph.
         if(isMulti)
         {
            pRef.setLocation(pLabel);
            double rad = (labelRot+180) * Math.PI/180.0;
            pLabel.setLocation(pRef.getX() + (multiLabelMaxW/2.0) * Math.cos(rad), 
                  pRef.getY() + (multiLabelMaxW/2.0) * Math.sin(rad));
         }
      }

      // the axis label painter. The painter used depends on whether or not the label string has line breaks. We'll have
      // to switch implementations whenever label becomes multi-line or returns to single-line!
      if(!isMulti)
      {
         SingleStringPainter ssPainter;
         if(labelPainter == null || (labelPainter.getClass() != SingleStringPainter.class))
         {
            ssPainter = new SingleStringPainter();
            ssPainter.setStyle(this);
            labelPainter = ssPainter;
         }
         else ssPainter = (SingleStringPainter) labelPainter;
         
         ssPainter.setTextAndLocation(
               fromStyledText(label, getFontFamily(), getFontSizeInPoints(), getFillColor(), getFontStyle(), true), 
               pLabel
               );
         ssPainter.setRotation(labelRot);
         ssPainter.setAlignment(hAlign, vAlign);
      }
      else
      {
         TextBoxPainter tbPainter;
         if(labelPainter == null || (labelPainter.getClass() != TextBoxPainter.class))
         {
            tbPainter = new TextBoxPainter();
            tbPainter.setStyle(this);
            tbPainter.setBorderStroked(false);
            labelPainter = tbPainter;
         }
         else tbPainter = (TextBoxPainter) labelPainter;
         
         tbPainter.setBoundingBox(pLabel, multiLabelMaxW, getFontSize() * lineHeight, 0);
         tbPainter.setText(fromStyledText(label, getFontFamily(), getFontSizeInPoints(), getFillColor(), getFontStyle(),
               true));
         tbPainter.setAlignment(hAlign, vAlign);
         tbPainter.setLineHeight(lineHeight);
         tbPainter.setRotation(labelRot);
      }
   }

   /**
    * Find the endpoints of the 3D axis line segment in the parent graph's 2D projection.
    * 
    * @param p1 On return, contains endpoint further from camera viewpoint in 3D space. Must not be null.
    * @param p2 On return, contains endpoint closer to camera viewpoint in 3D space. Must not be null.
    */
   private void getProjectedAxisLineSegment(Point2D p1, Point2D p2) 
   {
      if(p1 == null || p2 == null) throw new NullPointerException("p1, p2 cannot be null");
      
      Graph3DNode g3 = getParentGraph3D();
      assert g3 != null;
      BackDrop backDrop = g3.getBackDrop();
      Projector prj = g3.get2DProjection();
      double xBack = prj.getBackSideX();
      double xFront = prj.getFrontSideX();
      double yBack = prj.getBackSideY();
      double yFront = prj.getFrontSideY();
      double zBack = prj.getBackSideZ();
      double zFront = prj.getFrontSideZ();
      double elevAbs = Math.abs(g3.getElevate());
      double spacerMI = spacer.toMilliInches();

      // traditional 3D axis backdrop: all 3 axes extending from the intersection of the three perpendicular back
      // planes. Let L be the infinite line in the 2D plane containing the axis line segment rendered. The "outer"
      // endpoint lies along L but beyond the confines of the 3D box IAW the "spacer" property.
      // For the X and Y axes, the preferred perpendicular edge is || to the other axis -- except at elevation
      // angles between -10 and 10, when it's parallel to the Z-axis. For the Z-axis, the perpendicular is || to the
      // X or Y axis, depending on which gives a less skewed appearance.
      if(backDrop == BackDrop.AXESBACK)
      {
         prj.project(xBack, yBack, zBack, p1);
         if(axis == Graph3DNode.Axis.X)
         {
            prj.project(xFront, yBack, zBack, p2); 
            adjEdge = Projector.Edge.YZBACK;
            perpEdge = (elevAbs > 10) ? Projector.Edge.XZBACK : Projector.Edge.XYBACK;
         }
         else if(axis == Graph3DNode.Axis.Y)
         {
            prj.project(xBack, yFront, zBack, p2); 
            adjEdge = Projector.Edge.XZBACK;
            perpEdge = (elevAbs > 10) ? Projector.Edge.YZBACK : Projector.Edge.XYBACK;
         }
         else
         {
            prj.project(xBack, yBack, zFront, p2); 
            adjEdge = Projector.Edge.XYBACK;
            
            Point2D pRef = prj.project((xBack+xFront)/2.0, (yFront+yBack)/2.0, zBack, null);
            LineXY adjEdgeXY = prj.getLineContainingEdge(adjEdge);
            LineXY offsetLineXY = adjEdgeXY.getParallelLine(100, pRef.getX(), pRef.getY());
            LineXY perpEdgeXY = prj.getLineContainingEdge(Projector.Edge.XZBACK);
            Point2D pInt1 = offsetLineXY.getIntersection(perpEdgeXY, null);
            Point2D pInt2 = adjEdgeXY.getIntersection(perpEdgeXY, null);
            double d1 = pInt1.distance(pInt2);
            perpEdgeXY = prj.getLineContainingEdge(Projector.Edge.YZBACK);
            offsetLineXY.getIntersection(perpEdgeXY, pInt1);
            adjEdgeXY.getIntersection(perpEdgeXY, pInt2);
            perpEdge = (d1 < pInt1.distance(pInt2)) ? Projector.Edge.XZBACK : Projector.Edge.YZBACK;
         }
                  
         LineXY axis2D = new LineXY(p1,p2);
         axis2D.getPointAlongLine(p2, spacerMI, p1, false);

         return;
      }

      // for all of the box backdrops, we find the 3D box edge adjacent to the axis line segment such that the X-axis
      // and Y-axis always sit below the box and the Z-axis to the left. We also find the perpendicular edges that will
      // locate the endpoints of the axis line segment in the 2D projection. The identity of the edges depend on the
      // 3D graph's rotation and elevation. We always choose the perpendicular edges which intersect the offset axis
      // line closest to the corresponding box corner.
      double elev = prj.getElevationAngle();
      Point2D pRef = prj.project(xBack, yBack, zBack, null);
      
      Projector.Edge perpEdge1, perpEdge2, altPerpEdge1, altPerpEdge2;         
      switch(axis)
      {
      case X :
         if(elev >= 0)
         {
            adjEdge = Projector.Edge.YFRONTZBACK;
            perpEdge1 = Projector.Edge.XZBACK; perpEdge2 = Projector.Edge.XFRONTZBACK;
            altPerpEdge1 = Projector.Edge.XBACKYFRONT; altPerpEdge2 = Projector.Edge.XYFRONT;
         }
         else
         {
            adjEdge = Projector.Edge.YBACKZFRONT;
            perpEdge1 = Projector.Edge.XBACKZFRONT; perpEdge2 = Projector.Edge.XZFRONT;
            altPerpEdge1 = Projector.Edge.XYBACK; altPerpEdge2 = Projector.Edge.XFRONTYBACK;               
         }
         break;
      case Y :
         if(elev >= 0)
         {
            adjEdge = Projector.Edge.XFRONTZBACK;
            perpEdge1 = Projector.Edge.YZBACK; perpEdge2 = Projector.Edge.YFRONTZBACK;
            altPerpEdge1 = Projector.Edge.XFRONTYBACK; altPerpEdge2 = Projector.Edge.XYFRONT;
         }
         else
         {
            adjEdge = Projector.Edge.XBACKZFRONT;
            perpEdge1 = Projector.Edge.YBACKZFRONT; perpEdge2 = Projector.Edge.YZFRONT;
            altPerpEdge1 = Projector.Edge.XYBACK; altPerpEdge2 = Projector.Edge.XBACKYFRONT;               
         }
         break;
      default :
         // choose near edge XFRONTYBACK or XBACKYFRONT, whichever is a left edge (depends on rotation angle). Perp
         // edges are either || to X- or Y-axis, depending on which yields a less skewed appearance...
         prj.project(xFront, yBack, zBack, p1);
         prj.project(xBack, yFront, zBack, p2);
         if(p1.getX() < p2.getX())
         {
            adjEdge = Projector.Edge.XFRONTYBACK;
            perpEdge1 = Projector.Edge.XFRONTZBACK; perpEdge2 = Projector.Edge.XZFRONT;
            altPerpEdge1 = Projector.Edge.YBACKZFRONT; altPerpEdge2 = Projector.Edge.YZBACK;
         }
         else
         {
            adjEdge = Projector.Edge.XBACKYFRONT;
            perpEdge1 = Projector.Edge.YFRONTZBACK; perpEdge2 = Projector.Edge.YZFRONT;
            altPerpEdge1 = Projector.Edge.XBACKZFRONT; altPerpEdge2 = Projector.Edge.XZBACK;
         }
         break;
      }
      
      // find best location for axis line segment endpoints so that they're closer to corresponding box corners. We
      // choose the pair of perpendicular edges that intersect offset line segment closer to the corresponding box.
      // SPECIAL CASE: If spacer property is zero, then the axis lies on the box edge and the endpoints are at the 
      // relevant box corners. However, we still need to identify the preferred perpendicular box edge in order to 
      // locate the axis label and to render tick marks along the axis.
      LineXY adjEdgeXY = prj.getLineContainingEdge(adjEdge);
      LineXY axisLineXY = adjEdgeXY.getParallelLine(spacerMI > 0 ? spacerMI : 100, pRef.getX(), pRef.getY());
      LineXY perpEdgeXY = prj.getLineContainingEdge(perpEdge1);
      axisLineXY.getIntersection(perpEdgeXY, p1);
      adjEdgeXY.getIntersection(perpEdgeXY, p2);
      double d1 = p1.distance(p2);
      LineXY altPerpEdgeXY = prj.getLineContainingEdge(altPerpEdge1);
      axisLineXY.getIntersection(altPerpEdgeXY, p1);
      adjEdgeXY.getIntersection(altPerpEdgeXY, p2);
      if(d1 < p1.distance(p2))
      {
         perpEdge = perpEdge1;
         axisLineXY.getIntersection(perpEdgeXY, p1);
         axisLineXY.getIntersection(prj.getLineContainingEdge(perpEdge2), p2);
      }
      else
      {
         perpEdge = altPerpEdge1;
         axisLineXY.getIntersection(prj.getLineContainingEdge(altPerpEdge2), p2);
      }
      
      // calculate endpoints when axis spacer is 0
      if(spacerMI == 0)
      {
         adjEdgeXY.getIntersection(prj.getLineContainingEdge(perpEdge1), p1);
         adjEdgeXY.getIntersection(prj.getLineContainingEdge(perpEdge2), p2);
      }
   }
   
   /**
    * Get the infinite line in the parent graph's 2D projection plane that is colinear with the line segment that
    * renders this 3D axis.
    * @return The axis line in 2D space. Specified in the parent graph's viewport coordinates.
    */
   LineXY getLineXYContainingAxis() 
   { 
      return(new LineXY(axisLine.getX1(), axisLine.getY1(), axisLine.getX2(), axisLine.getY2()));
   }
   
   /**
    * Get a point that lies on the opposite side of this 3D axis from any axis tick marks -- when those marks are 
    * oriented "outward". This is used when rendering any tick set associated with the 3D axis.
    * @return The desired point.
    */
   Point2D getPointOppositeTickMarks()
   {
      Graph3DNode g3 = getParentGraph3D();
      assert g3 != null;
      BackDrop backDrop = g3.getBackDrop();
      Projector prj = g3.get2DProjection();
      double xBack = prj.getBackSideX();
      double xFront = prj.getFrontSideX();
      double yBack = prj.getBackSideY();
      double yFront = prj.getFrontSideY();
      double zBack = prj.getBackSideZ();
      double zFront = prj.getFrontSideZ();
      
      // for any of the box-type backdrops, the intersection of the three backside planes is always inside the 3D box.
      if(backDrop != BackDrop.AXESBACK) return(prj.project(xBack, yBack, zBack, null));
      
      // it's more complicated for the AXESBACK style...
      Point2D p;
      switch(axis)
      {
      case X:
         if(perpEdge == Projector.Edge.XZBACK) p = prj.project(xBack, yFront, zBack, null);
         else p = prj.project(xBack, yBack, zFront, null);
         break;
      case Y:
         if(perpEdge == Projector.Edge.YZBACK) p = prj.project(xFront, yBack, zBack, null);
         else p = prj.project(xBack, yBack, zFront, null);
         break;
      default:
         if(perpEdge == Projector.Edge.XZBACK) p = prj.project(xBack, yFront, zBack, null);
         else p = prj.project(xFront, yBack, zBack, null);
         break;
      }
      return(p);
   }
   
   /**
    * Get the infinite line in the parent graph's 2D projection plane that is colinear with a tick mark at the specified
    * coordinate value.
    * @param tick The coordinate value (X, Y or Z, depending on the identity of this axis) for the tick mark.
    * @return The line in 2D space containing the tick mark. Specified in the parent graph's viewport coordinates.
    */
   LineXY getLineXYContainingTickMark(double tick)
   {
      Graph3DNode g3 = getParentGraph3D();
      assert g3 != null;
      BackDrop backDrop = g3.getBackDrop();
      Projector prj = g3.get2DProjection();
      double xBack = prj.getBackSideX();
      double xFront = prj.getFrontSideX();
      double yBack = prj.getBackSideY();
      double yFront = prj.getFrontSideY();
      double zBack = prj.getBackSideZ();
      double zFront = prj.getFrontSideZ();

      // find 3D point on the box edge adjacent to axis that corresponds to tick location and project it to 2D
      Point2D p;
      if(backDrop == BackDrop.AXESBACK)
      {
         switch(axis)
         {
         case X: p = prj.project(tick, yBack, zBack, null); break;
         case Y: p = prj.project(xBack, tick, zBack, null); break;
         default: p = prj.project(xBack, yBack, tick, null); break;
         }
      }
      else
      {
         switch(axis)
         {
         case X: 
            if(adjEdge == Projector.Edge.YFRONTZBACK) p = prj.project(tick, yFront, zBack, null); 
            else p = prj.project(tick, yBack, zFront, null);
            break;
         case Y:
            if(adjEdge == Projector.Edge.XFRONTZBACK) p = prj.project(xFront, tick, zBack, null); 
            else p = prj.project(xBack, tick, zFront, null);
            break;
         default:
            if(adjEdge == Projector.Edge.XFRONTYBACK) p = prj.project(xFront, yBack, tick, null); 
            else p = prj.project(xBack, yFront, tick, null);
            break;
         }
      }
      // the line containing the tick mark passes through the point computed above and is parallel to the line 
      // containing the preferred perpendicular 3D box edge
      return(prj.getLineContainingEdge(perpEdge).getParallelLine(p.getX(), p.getY()));
   }
   
   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException 
   { 
      // don't bother if axis is not rendered
      if(!isRendered()) return;

      // start the element -- this will update the graphics state IAW this element's stroke/fill colors, etc
      psDoc.startElement( this );

      // render the axis line itself
      psDoc.renderLine(axisLine.getP1(), axisLine.getP2());
      
      // render the axis label, if any.
      // NOTE: Rather than recomputing label location, alignment, and so on -- we retrieve the relevant information
      // from the internal label painter and use it to render to the Postscript doc!
      String label = getAxisLabel();
      if((!label.isEmpty()) && getFillColor().getAlpha() > 0)
      {
         if(labelPainter instanceof SingleStringPainter)
         {
            SingleStringPainter ssPainter = (SingleStringPainter) labelPainter;
            Point2D pLabel = ssPainter.getTextLocation(null);
            TextAlign hAlign = ssPainter.getHorizontalAlignment();
            TextAlign vAlign = ssPainter.getVerticalAlignment();
            double rot = ssPainter.getRotation();
            
            psDoc.translateAndRotate(pLabel, rot);
            psDoc.renderText(label, 0, 0, hAlign, vAlign);
         }
         else
         {
            TextBoxPainter tbPainter = (TextBoxPainter) labelPainter;
            
            psDoc.translateAndRotate(tbPainter.getBoundingBoxLocation(), tbPainter.getRotation());
            psDoc.renderTextInBox(label, tbPainter.getBoundingBoxWidth(), tbPainter.getBoundingBoxHeight(), 
                  getLineHeight(), tbPainter.getRotation(), tbPainter.getHorizontalAlignment(), 
                  tbPainter.getVerticalAlignment());
         }
      }
      
      // render subordinate nodes (tick sets). NOTE: Because the 3D axis and its tick sets are rendered in the viewport
      // of the 3D graph, we close the 3D axis element BEFORE rendering the ticks sets to restore the graphic state!
      psDoc.endElement();
      for(int i=0; i<getChildCount(); i++) getChildAt(i).toPostscript(psDoc);
   }
   
   
   /**
    * This override ensures that the cloned 3D axis node's internal rendering resources are completely independent of 
    * the resources allocated to this node.
    */
   @Override protected Object clone() throws CloneNotSupportedException
   {
      Axis3DNode copy = (Axis3DNode) super.clone();
      copy.adjEdge = null;
      copy.perpEdge = null;
      copy.axisLine = new Line2D.Double();
      copy.labelPainter = null;
      copy.rBoundsSelf = null;
      return(copy);
   }

}
