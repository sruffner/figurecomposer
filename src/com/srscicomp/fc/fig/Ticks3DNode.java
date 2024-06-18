package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.srscicomp.common.g2dutil.ListBasedProducer;
import com.srscicomp.common.g2dutil.PolylinePainter;
import com.srscicomp.common.g2dutil.StringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.LineXY;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.fig.TickSetNode.LabelFormat;
import com.srscicomp.fc.fig.TickSetNode.Orientation;

/**
 * <b>Ticks3DNode</b> defines a set of tick marks and associated numeric labels appearing along an axis of the 3D graph
 * object, {@link Graph3DNode}. It is a "leaf" node and appears only as a child of an {@link Axis3DNode}.
 * 
 * <p>The 3D tick set node possesses most of the shared styling attributes managed in the base class (font-related 
 * properties, stroke color and width, and text/fill color), plus additional properties governing the placement of the 
 * tick marks along the parent 3D axis and the appearance of the tick marks and labels.</p>
 * 
 * <p>Tick mark locations are determined by <i>start</i>, <i>end</i>, and <i>interval</i> properties. Behavior is very
 * similar to the 2D tick set {@link TickSetNode}, with these exceptions: (1) Any non-positive <i>interval</i> value is
 * rejected. (2) It must always be true that <i>start &le; end</i>. (3) There is no "track parent axis" feature.</p>
 * 
 * <p>Like its 2D counterpart, the nominal <i>start, end, interval</i> values may be auto-corrected to sensible values
 * when the parent axis is logarithmic. For example, <i>interval</i> is a multiplicative value when the axis is
 * logarithmic and is always a power of 10 greater than 0; also, if <i>start</i> is non-positive on a log axis, it will
 * be mapped to 1, and <i>end</i> adjusted as well, if necessary.</p>
 * 
 * <p>Like the other 3D components of {@link Graph3DNode}, the 3D tick set node renders itself directly into the 3D
 * graph node's viewport, relying on the graph's 3D-to-2D projection to compute tick mark end points so that each tick
 * mark is consistent with the perspective projection. It is not focusable.</p>
 * 
 * @author sruffner
 */
public class Ticks3DNode extends FGraphicNode implements Cloneable
{
   /**
    * Construct a 3D tick set node. It is configured initially with a nominal tick mark range of [0..100] and a tick
    * interval of 25. All style properties are implicit initially, and all other tick set properties are initialized IAW
    * user-defined preferences.
    */
   public Ticks3DNode()
   {
      super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS);
      
      start = 0;
      end = 100;
      interval = 25;
      decadeTicks = FGNPreferences.getInstance().getPreferredLogTickPattern();
      tickOrientation = FGNPreferences.getInstance().getPreferredTickOrientation();
      tickLength = FGNPreferences.getInstance().getPreferredTickLength();
      tickGap = FGNPreferences.getInstance().getPreferredTickGap();
      tickLabelFormat = FGNPreferences.getInstance().getPreferredTickLabelFormat();
      customTickLabels = new String[0];
   }

   /**
    * Return a reference to the 3D graph axis object containing this 3D tick set.
    * @return The parent 3D graph axis, or null if there is no parent.
    */
   final Axis3DNode getParentAxis3D()
   {
      FGraphicNode p = getParent();
      if(p == null) return(null);
      assert(p instanceof Axis3DNode);
      return((Axis3DNode)p);
   }
   
   /**
    * Return a reference to the 3D graph object to which this 3D tick set belongs.
    * @return The 3D graph object, or null if not found (tick set node not part of a graphic model).
    */
   final Graph3DNode getGraph3D()
   {
      Axis3DNode axis = getParentAxis3D();
      return(axis != null ? axis.getParentGraph3D() : null);
   }
   
   //
   // Support for child nodes -- none permitted!
   //
   @Override public FGNodeType getNodeType() { return(FGNodeType.TICKS3D); }
   @Override public boolean isLeaf() { return(true); }
   @Override public boolean canInsert(FGNodeType nodeType) { return(false); }

   
   //
   // Properties
   //
   
   /** Start of the tick set range (in "user" units). Will always be less than or equal to the range end. */
   private double start;
   
   /**
    * Get the start of this tick set's range.
    * @return Start of tick set range, in whatever units are attached to the parent axis ("user" units).
    */
   public double getStart() { return(start); }
   
   /**
    * Set the start of this tick set's range. If a change was made, the method {@link #onNodeModified} is invoked.
    * 
    * @param d New value for the start of tick set range. Rejected if value is not well-defined or greater than the 
    * current end of the range.
    * @return True if successful; false otherwise.
    */
   public boolean setStart(double d)
   {
      if((!Utilities.isWellDefined(d)) || d > end) return(false);
      
      if(start != d)
      {
         if(doMultiNodeEdit(FGNProperty.START, d)) return(true);
         
         Double old = start;
         start = d;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.START);
            FGNRevEdit.post(this, FGNProperty.START, start, old, "Change start of 3D tick set range");
         }
      }
      return(true);
   }
   
   /** End of the tick set range (in "user" units). Will always be greater than or equal to the range start. */
   private double end;

   /**
    * Get the end of this tick set's range.
    * @return End of tick set range, in whatever units are attached to the parent axis ("user" units).
    */
   public double getEnd() { return(end); }
   
   /**
    * Set the end of this tick set's range. If a change was made, the method {@link #onNodeModified} is invoked.
    * 
    * @param d New value for the end of tick set range. Rejected if value is not well-defined or less than the current
    * start of the range.
    * @return True if successful; false otherwise.
    */
   public boolean setEnd(double d)
   {
      if((!Utilities.isWellDefined(d)) || d < start) return(false);
      
      if(end != d)
      {
         if(doMultiNodeEdit(FGNProperty.END, d)) return(true);
         
         Double old = end;
         end = d;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.END);
            FGNRevEdit.post(this, FGNProperty.END, end, old, "Change end of 3D tick set range");
         }
      }
      return(true);
   }
   
   /** 
    * Nominal tick mark interval (in "user" units). Strictly positive. Mapped to nearest positive power of 10 (or 2) 
    * when the parent axis is logarithmic.
    */
   private double interval;
   
   /**
    * Get the tick mark interval.
    * @return Tick interval, in whatever units are attached to the parent axis ("user" units).
    */
   public double getInterval() { return(interval); }
   
   /**
    * Set the tick mark interval. If a change was made, the method {@link #onNodeModified} is invoked.
    * 
    * @param d New value for the tick inteval. Rejected if value is not well-defined or non-positive.
    * @return True if successful; false otherwise.
    */
   public boolean setInterval(double d)
   {
      if((!Utilities.isWellDefined(d)) || d <= 0) return(false);
      
      if(interval != d)
      {
         if(doMultiNodeEdit(FGNProperty.INTV, d)) return(true);
         
         Double old = interval;
         interval = d;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.INTV);
            FGNRevEdit.post(this, FGNProperty.INTV, interval, old, "Change 3D tick set interval");
         }
      }
      return(true);
   }
   
   /**
    * Set the tick set range and interval, silently auto-correcting any invalid values. 
    * 
    * <p>This method is used to initialize the 3D tick set node when it is constructed from its <i>FypML</i> or
    * <i>Matlab FIG</i> counterpart. Calling {@link #setStart} and {@link #setEnd} individually may not work
    * depending on the before and after values of the range. Also, the <i>FypML</i> element does not enforce the 
    * constraints on the range and interval that this 3D tick set object enforces. For example, if <i>start &gt; 
    * end</i>, this method will swap the two values to ensure <i>start &le; end</i>. It will also replace any 
    * non-positive interval with a value that is 1/4 of the range.</p>
    * 
    * <p>Furthermore, this method does NOT notify the model of any change, because it is intended to be called only 
    * when the model is being constructed from its <i>FypML</i> representation.</p>
    * 
    * @param s The start of the range.
    * @param e The end of the range.
    * @param d The tick mark interval
    */
   public void setRangeAndInterval(double s, double e, double d)
   {
      s = Utilities.isWellDefined(s) ? s : 0;
      e = Utilities.isWellDefined(e) ? e : s + 1;
      if(s > e) { double tmp = s; s = e; e = tmp; }
      else if(s == e) e = s+1;
      start = s;
      end = e;
      interval = ((!Utilities.isWellDefined(d)) || d <= 0) ? (e-s)/4 : d;
      computeTickLocations(true);
   }
   
   /** Pattern of tick marks rendered within a decade. Applicable only when parent axis is base-10 logarithmic. */
   private LogTickPattern decadeTicks;
   
   /**
    * Get the pattern of tick marks rendered within a log decade -- <i>applicable only when the parent axis of this 
    * tick set is base10-logarithmic.</i>
    * 
    * @return Current log-decade tick pattern.
    */
   public LogTickPattern getDecadeTicks() { return(decadeTicks); }

   /**
    * Set the pattern of tick marks rendered within a log decade by this tick set when its parent axis is base10-
    * logarithmic. If a change is made, the method {@link #onNodeModified} is invoked.
    * 
    * @param p The new log-decade tick pattern. A null value is rejected.
    * @return True if successful; false if argument was null.
    */
   public boolean setDecadeTicks(LogTickPattern p)
   {
      if(p == null) return(false);
      if(!LogTickPattern.equal(decadeTicks, p))
      {
         if(doMultiNodeEdit(FGNProperty.PERLOGINTV, p)) return(true);
         
         LogTickPattern old = decadeTicks;
         decadeTicks = p;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.PERLOGINTV);
            FGNRevEdit.post(this, FGNProperty.PERLOGINTV, decadeTicks, old, 
                  "Set log-decade tick pattern to " + decadeTicks.toString());
         }
      }
      return(true);
   }

   
   /** Tick mark orientation. */
   private Orientation tickOrientation;

   /**
    * Get the current orientation of any tick marks in this 3D tick set.
    * @return Current tick mark orientation.
    */
   public Orientation getTickOrientation() { return(tickOrientation); }

   /**
    * Set the orientation of tick marks in this 3D tick set. If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param o The new tick mark orientation.
    * @return True if successful; false if argument was null.
    */
   public boolean setTickOrientation(Orientation o)
   {
      if(o == null) return(false);
      if(tickOrientation != o)
      {
         if(doMultiNodeEdit(FGNProperty.DIR, o)) return(true);
         
         Orientation old = tickOrientation;
         tickOrientation = o;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.DIR);
            FGNRevEdit.post(this, FGNProperty.DIR, tickOrientation, old, 
                  "Set tick mark orientation to " + tickOrientation.toString());
         }
      }
      return(true);
   }


   /** Tick mark length, with associated measurement units. */
   private Measure tickLength;

   /**
    * Get the length of any tick marks in this 3D tick set.
    * 
    * @return The tick mark length with associated units of measure (only physical units allowed).
    */
   public Measure getTickLength() { return(tickLength); }

   /**
    * Set the length of tick marks in this 3D tick set. If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param m The new tick length. It is constrained to satisfy {@link TickSetNode#TICKLENCONSTRAINTS}. A null value
    * is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setTickLength(Measure m)
   {
      if(m == null) return(false);
      m = TickSetNode.TICKLENCONSTRAINTS.constrain(m);

      boolean changed = (tickLength != m) && !Measure.equal(tickLength, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.LEN, m)) return(true);
         
         Measure oldLen = tickLength;
         tickLength = m;
         if(areNotificationsEnabled())
         {
            if(oldLen.toMilliInches() != tickLength.toMilliInches()) onNodeModified(FGNProperty.LEN);
            FGNRevEdit.post(this, FGNProperty.LEN, tickLength, oldLen, "Set tick length to " + tickLength.toString());
         }
      }
      return(true);
   }


   /** The tick-label gap, with associated measurement units. */
   private Measure tickGap;

   /**
    * Get the tick-label gap, ie, the distance between a tick mark label and the end of the corresponding tick mark or 
    * the parent axis line, whichever is closer (depends on current axis configuration and tick mark orientation).
    * 
    * @return The tick-label gap with associated units of measure (only physical units allowed).
    */
   public Measure getTickGap() { return(tickGap); }

   /**
    * Set the tick-label gap (see also {@link #getTickGap()}. If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param m The new tick-label gap. It is constrained to satisfy {@link TickSetNode#TICKLENCONSTRAINTS}. A null value
    * is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setTickGap(Measure m)
   {
      if(m == null) return(false);
      m = TickSetNode.TICKLENCONSTRAINTS.constrain(m);

      boolean changed = (tickGap != m) && !Measure.equal(tickGap, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.GAP, m)) return(true);
         
         Measure old = tickGap;
         tickGap = m;
         if(areNotificationsEnabled())
         {
            if(old.toMilliInches() != tickGap.toMilliInches()) onNodeModified(FGNProperty.GAP);
            FGNRevEdit.post(this, FGNProperty.GAP, tickGap, old, "Set tick-label gap size to " + tickGap.toString());
         }
      }
      return(true);
   }


   /** Tick mark numeric label format. */
   private LabelFormat tickLabelFormat;

   /**
    * Get the current format for any tick mark labels in this 3D tick set.
    * @return Current tick mark label format.
    */
   public LabelFormat getTickLabelFormat() { return(tickLabelFormat); }

   /**
    * Set the format for tick mark labels in this 3D tick set. If a change is made, {@link #onNodeModified} is invoked.
    * <p>
    * @param fmt The new tick mark label format.
    * @return True if successful; false if argument is null.
    */
   public boolean setTickLabelFormat(LabelFormat fmt)
   {
      if(fmt == null) return(false);
      if(tickLabelFormat != fmt)
      {
         if(doMultiNodeEdit(FGNProperty.FMT, fmt)) return(true);
         
         LabelFormat old = tickLabelFormat;
         tickLabelFormat = fmt;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.FMT);
            FGNRevEdit.post(this, FGNProperty.FMT, tickLabelFormat, old, 
                  "Set tick label format to " + tickLabelFormat.toString());
         }
      }
      return(true);
   }

   
   /** 
    * Array of custom tick mark labels. If empty, numeric labels are generated IAW actual tick mark locations and the
    * current label format. Otherwise, tick marks are labeled instead with the tokens in this array, in order. If there 
    * are more labels than tick marks, the extra labels are ignored. If there are too few, the labels are reused until 
    * all tick marks are labeled.
    */
   private String[] customTickLabels;
   
   /**
    * Get the array of custom tick mark labels assigned to this tick set. For further details on how custom tick labels
    * are used, see {@link #getCustomTickLabelsAsCommaSeparatedList()}.
    * @return The custom tick labels, in order of appearance. If there are none, an empty array is returned.
    */
   public String[] getCustomTickLabels() {return( Arrays.copyOf(customTickLabels, customTickLabels.length)); }
   
   /**
    * Get the ordered list of custom tick mark labels assigned to the tick set. When custom tick labels are defined, 
    * they are used to label all generated tick marks instead of the standard numeric labels (<i>unless the current tick
    * label format is "none"</i>). If there more custom labels than actual tick marks, the extra labels are unused. If 
    * there are too few, the labels are reused until all tick marks are labeled.
    * 
    * @return String holding a comma-separated list of custom tick mark label tokens. If no such tokens have been 
    * defined for this tick set, an empty string is returned.
    */
   public String getCustomTickLabelsAsCommaSeparatedList() 
   {
      if(customTickLabels.length == 0) return("");
      StringBuilder sb = new StringBuilder();
      for(int i=0; i<customTickLabels.length-1; i++) sb.append(customTickLabels[i]).append(",");
      sb.append(customTickLabels[customTickLabels.length-1]);
      return(sb.toString());
   }
   
   /**
    * Set the ordered list of custom tick mark labels assigned to the tick set. No label may contain a comma. For 
    * further details on how custom tick labels are used, see {@link #getCustomTickLabelsAsCommaSeparatedList()}.
    * @param labels The custom tick labels. A null or empty array will restore the standard auto-generated numeric 
    * labels. Otherwise, each element of the array is trimmed of whitespace and any commas are removed. Any null entry
    * is replaced by an empty string.
    */
   public void setCustomTickLabels(String[] labels)
   {
      String[] tickLabels = (labels == null) ? new String[0] : labels;
      for(int i=0; i<tickLabels.length; i++)
      {
         if(tickLabels[i] == null) tickLabels[i] = "";
         else
         {
            tickLabels[i] = tickLabels[i].trim();
            tickLabels[i] = tickLabels[i].replace(',', '\0');
         }
      }
      
      if(!Arrays.equals(tickLabels,  customTickLabels))
      {
         if(doMultiNodeEdit(FGNProperty.CUSTOMTCKLBL, tickLabels)) return;
         
         Object old = customTickLabels;
         customTickLabels = tickLabels;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CUSTOMTCKLBL);
            FGNRevEdit.post(this, FGNProperty.CUSTOMTCKLBL, customTickLabels, old, "Change custom tick mark labels");
         }
      }
   }
   
   /**
    * Assign custom tick mark labels to be used in place of the standard numeric labels (except when the tick label 
    * format is "none"). See {@link #getCustomTickLabelsAsCommaSeparatedList()} for further details.
    * @param csTokens A comma-separated list of string tokens, each of which is trimmed of whitespace and saved as a
    * custom tick mark label. The label tokens are applied in the order they appear in the comma-separated list. Note
    * that empty labels are allowed. For example, "A,,C" is a list of 3 label tokens, the second of which is an empty
    * string; ",,C,," is a list of 5 tokens, only one of which is non-empty! If the argument is null, empty, or contains
    * only whitespace, then NO custom tick labels are defined.
    */
   public void setCustomTickLabelsFromCommaSeparatedList(String csTokens)
   {
      if(csTokens == null) csTokens = "";
      else csTokens = csTokens.trim();
      
      String[] tickLabels = (csTokens.isEmpty()) ? new String[0] : csTokens.split(",", -1);
      
      if(!Arrays.equals(tickLabels, customTickLabels))
      {
         if(doMultiNodeEdit(FGNProperty.CUSTOMTCKLBL, tickLabels)) return;
         
         Object old = customTickLabels;
         customTickLabels = tickLabels;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CUSTOMTCKLBL);
            FGNRevEdit.post(this, FGNProperty.CUSTOMTCKLBL, customTickLabels, old, "Change custom tick mark labels");
         }
      }
   }
   
   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok;
      switch(p)
      {
         case START: ok = setStart((Double)propValue); break;
         case END: ok = setEnd((Double)propValue); break;
         case INTV: ok = setInterval((Double)propValue); break;
         case PERLOGINTV: ok = setDecadeTicks((LogTickPattern)propValue); break;
         case DIR: ok = setTickOrientation((Orientation)propValue); break;
         case LEN: ok = setTickLength((Measure) propValue); break;
         case GAP: ok = setTickGap((Measure) propValue); break;
         case FMT: ok = setTickLabelFormat((LabelFormat)propValue); break;
         case CUSTOMTCKLBL: setCustomTickLabels((String[]) propValue); ok = true; break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value;
      switch(p)
      {
      case START: value = getStart(); break;
         case END: value = getEnd(); break;
         case INTV: value = getInterval(); break;
         case PERLOGINTV: value = getDecadeTicks(); break;
         case DIR: value = getTickOrientation(); break;
         case LEN: value = getTickLength(); break;
         case GAP: value = getTickGap(); break;
         case FMT: value = getTickLabelFormat(); break;
         case CUSTOMTCKLBL: value = getCustomTickLabels(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }

   /**
    * Whenever this 3D tick set node's definition changes, the containing {@link Graph3DNode} is notified, since the 
    * graph's rendering must be updated accordingly.
    */
   @Override protected void onNodeModified(Object hint)
   {
      // re-compute tick mark locations if we need to
      Axis3DNode axis = getParentAxis3D();
      boolean logTicksAffected = 
            (axis != null && axis.isLogarithmic() && (!axis.isLogBase2()) && hint==FGNProperty.PERLOGINTV);
      boolean ticksAffected = (hint==null || hint==FGNProperty.START || hint==FGNProperty.END || 
            hint==FGNProperty.INTV || logTicksAffected);
      if(ticksAffected)
         computeTickLocations(true);
      
      // this will generate a rendering update to account for changes to the tick set itself
      super.onNodeModified(hint);
      
      // notify 3D graph contain if this is the first tick set of an axis, since that determines the locations of the
      // corresponding grid lines in the 3D backdrop. 
      Graph3DNode g3 = getGraph3D();
      if((g3 != null) && (axis != null) && g3.hasGridLines(axis.getAxis()) && axis.hasMajorTicks() && 
            (axis.getChildAt(0)==this) && ticksAffected)
         g3.onNodeModified(FGNProperty.WIDTH);
   }

   /** 
    * Computed tick locations, which are a function of the tick mark definition and the parent axis range. If null,
    * then tick locations have not been computed. Will be empty if no tick marks defined or within the axis range.
    */
   private double[] tickLocations = null;
   
   /**
    * Get the valid tick mark locations for this 3D tick set, given its tick set range and interval and the current
    * range of its parent 3D axis. The locations are always listed in ascending numerical order.
    * @return Array of tick mark locations that lie in the parent axis range (up to {@link TickSetNode#MAX_TICKS}).
    */
   public double[] getTickSet() 
   {
      computeTickLocations(false);
      return(tickLocations.length > 0 ? Arrays.copyOf(tickLocations, tickLocations.length) : tickLocations);
   }
   
   /**
    * Given this tick set's current range and interval, compute up to {@link TickSetNode#MAX_TICKS} valid tick mark 
    * locations that lie within the parent axis's current range.
    * @param force True to force computation of tick mark locations. If false, the locations are computed only if
    * they are currently undefined. The parent axis will call the method with <i>force==true</i> whenever the axis
    * range is modified.
    */
   void computeTickLocations(boolean force)
   {
      if(tickLocations != null && !force) return;
      
      // if there's no parent axis, or the tick set range does not intersect the axis range, there are no tick marks.
      tickLocations = TickSetNode.NOTICKS;
      Axis3DNode a3 = getParentAxis3D();
      if(a3 == null) return;
      
      double rngMin = a3.getRangeMin();
      double rngMax = a3.getRangeMax();
      double tolerance = 0.0001 * (rngMax - rngMin);
      boolean isLog = a3.isLogarithmic();
      boolean isLog2 = isLog && a3.isLogBase2();
      
      if(end < rngMin || start > rngMax) return;
      
      int NMAX = TickSetNode.MAX_TICKS;
      
      // when axis is logarithmic, we may have to adjust the start, end and interval to values that make sense. For
      // one thing, the interval is multiplicative instead of additive and must be an integer power of 2 or 10.
      if(isLog)
      {
         // interval value must be a power of B = 10 or 2. Find the nearest integer n such that n >= 1 and B^n is
         // greater than or equal to the nominal interval. B^n becomes the multiplicative interval.
         double intv = interval;
         if(isLog2) intv = Math.pow(2, Math.max(1, (int) Math.ceil(Utilities.log2(intv))));
         else intv = Math.pow(10, Math.max(1, (int) Math.ceil(Utilities.log10(intv))));
         
         // start of range is the nearest power of 10 or 2 that is less than or equal to the nominal start value.
         // If start of range is non-positive, use 1
         double s = start;
         if(s <= 0) s = 1;
         else if(isLog2) s = Math.pow(2, (int) Math.floor(Utilities.log2(s)));
         else s = Math.pow(10, (int) Math.floor(Utilities.log10(s)));

         // end of range is the nearest power of 10 or 2 that is greater than or equal to the nominal end value. If
         // end is less than start (because we had to set start to 1, above), set end to start*intv.
         double e = end;
         if(e < s) e = s * intv;
         else if(isLog2) e = Math.pow(2, (int) Math.ceil(Utilities.log2(e)));
         else e = Math.pow(10, (int) Math.ceil(Utilities.log10(e)));
         
         // populate tick mark locations for the logarithmic case. Note that a tick is included only if it's within
         // the parent axis range AND within the nominal tick set range specified prior to the corrections above.
         tickLocations = new double[NMAX];
         int nAccepted = 0;
         if(!isLog2)
         {
            int[] perLogIntv = decadeTicks.getEnabledTicks();
            double epochStart = s;
            while(epochStart <= e && nAccepted < NMAX)
            {
               for(int i=0; i<perLogIntv.length && nAccepted <= NMAX; i++)
               {
                  int j = perLogIntv[i];
                  double tick = epochStart * ((j==1) ? 1 : intv*0.1*((double)j));
                  if(start <= tick && rngMin <= tick && tick <= rngMax && tick <= end)
                     tickLocations[nAccepted++] = tick;
               }
               epochStart *= intv;
            }
         }
         else
         {
            double tick = s;
            while(tick <= e && nAccepted < NMAX)
            {
               if(start <= tick && rngMin <= tick && tick <= rngMax && tick <= end)
                  tickLocations[nAccepted++] = tick;
               tick *= intv;
            }
         }
         
         if(nAccepted < NMAX) 
            tickLocations = (nAccepted==0) ? TickSetNode.NOTICKS : Arrays.copyOf(tickLocations, nAccepted);   
         return;
      }
      
      // populate tick mark locations along a linear axis...
      if(interval <= 10*tolerance)
      {
         // additive interval too small. Locate one tick at start or end if inside axis range. Else empty tick set.
         if(start >= rngMin) tickLocations = new double[] { start };
         else if(end <= rngMax) tickLocations = new double[] { end };
         else tickLocations = TickSetNode.NOTICKS;
         return;
      }
      
      // to guard against bad user entry like an excessively small interval and endpoints that are outside of the
      // axis range, we compute no more than 5*N locations and accept only the first N ticks that lie in the axis range,
      // where N is the maximum # of tick that will be rendered by a tick set.
      tickLocations = new double[NMAX];
      double tick = start;
      int i = 0;
      int nAccepted = 0;
      while(tick <= (end+tolerance) && nAccepted < NMAX && i < 5*NMAX)
      {
         if(Math.abs(tick-rngMin) < tolerance) tickLocations[nAccepted++] = rngMin;
         else if(Math.abs(tick-rngMax) < tolerance) tickLocations[nAccepted++] = rngMax;
         else if(tick >= rngMin && tick <= rngMax) tickLocations[nAccepted++] = tick;
         tick += interval;
         ++i;
      }
      if(nAccepted < NMAX) 
         tickLocations = (nAccepted==0) ? TickSetNode.NOTICKS : Arrays.copyOf(tickLocations, nAccepted);   
   }
   
   // 
   // Support for style sets
   //
   
   /**
    * Tick mark style sets are only included as "components" of a parent axis style set -- even though tick set nodes
    * are normal children of an axis.
    */
   @Override public boolean supportsStyleSet()  { return(true); }

   /** 
    * The node-specific properties exported in a 3D tick set node's style set are the tick direction and length, the 
    * format of tick mark labels, and the gap separating each tick from its numeric label. The tick mark range, interval
    * and any custom tick labels are NOT included -- these are considered specific to each tick set.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.LEN, getTickLength());
      styleSet.putStyle(FGNProperty.GAP, getTickGap());
      styleSet.putStyle(FGNProperty.DIR, getTickOrientation());
      styleSet.putStyle(FGNProperty.FMT, getTickLabelFormat());
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      FGNodeType nt = getNodeType();
      
      Measure m = (Measure) applied.getCheckedStyle(FGNProperty.LEN, nt, Measure.class);
      if(m != null) m = TickSetNode.TICKLENCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.LEN, nt, Measure.class)))
      {
         tickLength = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LEN);

      m = (Measure) applied.getCheckedStyle(FGNProperty.GAP, nt, Measure.class);
      if(m != null) m = TickSetNode.TICKLENCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.GAP, nt, Measure.class)))
      {
         tickGap = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.GAP);
      
      Orientation ori = (Orientation) applied.getCheckedStyle(FGNProperty.DIR, nt, Orientation.class);
      if(ori != null && !ori.equals(restore.getCheckedStyle(FGNProperty.DIR, nt, Orientation.class)))
      {
         tickOrientation = ori;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.DIR);

      LabelFormat fmt = (LabelFormat) applied.getCheckedStyle(FGNProperty.FMT, nt, LabelFormat.class);
      if(fmt != null && !fmt.equals(restore.getCheckedStyle(FGNProperty.FMT, nt, LabelFormat.class)))
      {
         tickLabelFormat = fmt;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.FMT);

      return(changed);
   }

   //
   // Focusable/Renderable/PSTransformable support -- never rendered!
   //
   /** Returns null always because a 3D tick set node cannot be selected interactively. */
   @Override protected FGraphicNode hitTest(Point2D p) { return(null); }
   /** Returns null always because a 3D tick set node cannot receive the selection focus. */
   @Override public Shape getFocusShape(Graphics2D g2d) { return(null); }

   /** The 3D tick set node renders directly into its 3D graph container, so the identity transform is returned. */
   @Override public AffineTransform getLocalToParentTransform() { return(new AffineTransform()); }
   /** The 3D tick set node renders directly into its 3D graph container, so this method returns the parent viewport. */
   @Override public FViewport2D getViewport() { return(getParentViewport()); }

   @Override protected void releaseRenderResourcesForSelf() 
   {
      rBoundsSelf = null;
      tickPainter = null;
      if(tickPolyline != null) 
      {
         tickPolyline.clear();
         tickPolyline = null;
      }
      
      labelPainter = null;
      if(labelLocs != null) 
      {
         labelLocs.clear();
         labelLocs = null;
      }
      if(labels != null) 
      {
         labels.clear();
         labels = null;
      }
   }
   
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null || tickPainter == null)
      {
         if(!isRendered()) 
            rBoundsSelf = new Rectangle2D.Double();
         else
         {
            updateRenderingResources();
            tickPainter.invalidateBounds();
            labelPainter.updateFontRenderContext(g2d);
            labelPainter.invalidateBounds();

            rBoundsSelf = tickPainter.getBounds2D(rBoundsSelf);
            Utilities.rectUnion(rBoundsSelf, labelPainter.getBounds2D(null), rBoundsSelf);
         }
      }
      return((Rectangle2D)rBoundsSelf.clone());
   }
   
   /**
    * A 3D tick set is rendered if it contains at least one tick mark and its parent axis is rendered. However, if the
    * tick marks have zero length or are not stroked, AND the tick mark labels are not drawn, then the set is not
    * rendered regardless.
    */
   @Override protected boolean isRendered()
   {
      computeTickLocations(false);
      Axis3DNode axis = getParentAxis3D();
      boolean rendered = axis != null && axis.isRendered() && tickLocations != null && tickLocations.length > 0;
      if(rendered) rendered = (tickLength.toMilliInches() > 0 && isStroked()) ||
            ((tickLabelFormat != LabelFormat.NONE) && (getFillColor().getAlpha() > 0));
      return(rendered);
   }

   public boolean render(Graphics2D g2d, RenderTask task) 
   { 
      if(!(isRendered() && needsRendering(task)))
         return(true);

      // make sure internal rendering infrastructure has been set up
      if(tickPainter == null) updateRenderingResources();
      
      return(tickPainter.render(g2d, task) && labelPainter.render(g2d, task)); 
   }
   
   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException 
   { 
      // STRATEGY: We use the infrastructure already created for the internal painters that render the tick marks and
      // labels in Java 2D. As with the parent axis, all rendering is WRT the 3D graph's viewport!
      
      // don't bother if tick set is not rendered
      if(!isRendered()) return;

      // start the element -- this will update the graphics state IAW this element's stroke/fill colors, etc
      psDoc.startElement( this );

      // render the tick marks as a poly-line with gaps separating the individual marks. The location producer for our
      // internal Java 2D painter is already set-up -- just need to convert to array form.
      if(tickLength.toMilliInches() > 0 && isStroked())
         psDoc.renderPolyline(tickPolyline.toArray(new Point2D[0]), null, 0, null, null, false);
      
      // render tick mark labels. The location and text for each label has already been computed, as has the alignment
      // to use (same for all labels).
      if(tickLabelFormat != LabelFormat.NONE && getFillColor().getAlpha() > 0)
      {
         TextAlign hAlign = labelPainter.getHorizontalAlignment();
         TextAlign vAlign = labelPainter.getVerticalAlignment();
         
         for(int i=0; i<labels.size(); i++)
         {
            Point2D pLabel = labelLocs.get(i);
            psDoc.renderText(labels.get(i), pLabel.getX(), pLabel.getY(), hAlign, vAlign);
         }
      }
      
      // end the element -- restoring the graphics state
      psDoc.endElement();
  }
   
   //
   // Internal rendering resources
   //

   /**
    * Cached rectangle bounding the marks made by this 3D tick set. If null, rectangle has yet to be calculated. 
    * Specified WRT viewport of the 3D graph container, since neither the tick set nor its parent axis defines a new
    * viewport.
    */
   private Rectangle2D rBoundsSelf = null;

   /** This painter renders the tick marks. */
   private PolylinePainter tickPainter = null;

   /** 
    * This list serves as the location provider for the tick marks painter. Each tick mark is defined by a pair of
    * points, followed by (NaN, NaN) to separate each tick mark from the next one. 
    */
   private List<Point2D> tickPolyline = null;

   /** This painter renders the tick mark labels. */
   private StringPainter labelPainter = null;

   /** This list serves as the location provider for the tick mark label painter. */
   private List<Point2D> labelLocs = null;

   /** This list serves as the string provider for the tick mark label painter. */
   private List<String> labels = null;

   /**
    * Update the rendering infrastructure for this 3D tick set IAW its current definition and that of its 3D graph
    * container. This infrastructure includes separate painters for the tick marks and tick mark labels, location lists
    * for both marks and labels, and a list of label strings.
    * 
    * <p>Tick marks are drawn "in perspective" -- so that, e.g., ticks along the X-axis are perpendicular to that axis
    * in 3D -- which means they generally won't be in the 2D projection. The parent 3D axis defines the axis line, a
    * reference point that lies on the opposite side of the axis line from tick marks drawn "outward", and chooses the
    * chooses the preferred perpendicular based on the backdrop style and current orientation of the 3D graph (eg, for
    * the X-axis, tick marks could be || to the Y or Z axis in 3D space). Thus, this method relies on several {@link 
    * Axis3DNode} methods to perform the necessary calculations. Tick direction as well as the location of the parent 
    * axis WRT the 3D graph box must be taken into account when calculating tick mark end points, tick label locations, 
    * and label text alignments.</p>
    */
   private void updateRenderingResources()
   {
      // the location/string providers and the painters themselves are lazily created
      if(tickPolyline == null)
         tickPolyline = new ArrayList<>();
      if(labelLocs == null)
         labelLocs = new ArrayList<>();
      if(labels == null)
         labels = new ArrayList<>();
      if(tickPainter == null)
         tickPainter = new PolylinePainter(this, null);
      if(labelPainter == null)
         labelPainter = new StringPainter(this, null, null);

      // calculate tick locations if needed
      computeTickLocations(false);
      
      // empty the location lists for the tick mark and label painters. If tick set not rendered, we're done!
      // NOTE: The ListBasedProducer makes an internal copy of the source list, since the list could get modified
      // while a rendering is in progress in the background!
      tickPolyline.clear();
      labelLocs.clear();
      labels.clear();
      if(!isRendered())
      {
         tickPainter.setLocationProducer(new ListBasedProducer<>(tickPolyline));
         labelPainter.setLocationProducer(new ListBasedProducer<>(labelLocs));
         labelPainter.setStringProducer(new ListBasedProducer<>(labels));
         return;
      }
      
      Axis3DNode parent = getParentAxis3D();
      if(parent == null)
         return;
      
      double tickLenMI = tickLength.toMilliInches();
      double gapMI = tickGap.toMilliInches();
      
      // STRATEGY: We find the axis line itself, then define two lines parallel to it along which the tick mark
      // end points lie. The location of these lines WRT the axis itself depends on the tick orientation; their
      // perpendicular separation distance in the XY plane is the tick length. The tick marks themselves are NOT
      // drawn perpendicular to these two lines. Instead, they are drawn in perspective. This requires that we find, 
      // for each X-axis tick mark X=T, the line that intersects the X-axis line at X=T in 3D, project it to 2D, then 
      // intersect it with the two parallel lines to find the tick mark end points for X=T. Analogously for Y and Z
      // axes. For the corresponding label location, we find a third line parallel to the axis line with a perpendicular
      // separation of G + L or G + L/2 (where G = tick label gap and L = tick length), depending on the tick mark
      // orientation. The intersection of the X=T line with this third line gives the label location.
      
      TextAlign hAlign;
      TextAlign vAlign;
      LineXY axisLine, tickMarksLine1, tickMarksLine2, labelsLine;

      axisLine = parent.getLineXYContainingAxis();
      
      // a reference point inside the 3D box - used to ensure that we locate tick marks and labels on the "outside" of
      // the 3D box. 
      Point2D pRef = parent.getPointOppositeTickMarks();
      
      // get lines parallel to axis line that contain tick mark end points, tick label starting positions. Strategy:
      // (1) find a line parallel to a tick mark -- ie, perpendicular to the axis line in 3D; (2) find the two points
      // along the tick mark line that are the desired distance from the axis line; (3) for each point in (2), find the
      // line passing through the point and parallel to the axis line; (4) depending on the tick mark orientation, 
      // select the line in (3) that is further from or closer to the reference point.
      // Since the distance measurement (2) is in the 2D projection plane, the tick mark lines will be foreshortened at 
      // certain orientations. That's why the 3D axis parent chooses a perpendicular that minimizes foreshortening by 
      // choosing the perpendicular direction that is more nearly in the 2D plane.
      LineXY tickMarkXY = parent.getLineXYContainingTickMark(tickLocations[0]);
      Point2D pInt = axisLine.getIntersection(tickMarkXY, null);
      Point2D p = new Point2D.Double(pInt.getX(), pInt.getY());
      if(tickOrientation == Orientation.IN || tickOrientation == Orientation.OUT)
      {
         boolean inward = (tickOrientation == Orientation.IN);
         tickMarksLine1 = axisLine;
         
         tickMarkXY.getPointAlongLine(p, tickLenMI, pRef, true);
         LineXY alt1 = axisLine.getParallelLine(p.getX(), p.getY());
         
         p.setLocation(pInt);
         tickMarkXY.getPointAlongLine(p, tickLenMI, pRef, false);
         LineXY alt2 = axisLine.getParallelLine(p.getX(), p.getY());
         
         boolean which;
         if(alt1.getDistanceFromPoint(pRef.getX(), pRef.getY()) < alt2.getDistanceFromPoint(pRef.getX(), pRef.getY()))
         {
            tickMarksLine2 = inward ? alt1 : alt2;
            which = false;
         }
         else
         {
            tickMarksLine2 = inward ? alt2 : alt1;
            which = true;
         }

         p.setLocation(pInt);
         tickMarkXY.getPointAlongLine(p, gapMI + (inward ? 0 : tickLenMI), pRef, which);
      }
      else
      {
         tickMarkXY.getPointAlongLine(p, tickLenMI/2.0, pRef, true);
         tickMarksLine1 = axisLine.getParallelLine(p.getX(), p.getY());
         
         p.setLocation(pInt);
         tickMarkXY.getPointAlongLine(p, tickLenMI/2.0, pRef, false);
         tickMarksLine2 = axisLine.getParallelLine(p.getX(), p.getY());
         
         p.setLocation(pInt);
         boolean labelsFlag = (tickMarksLine1.getDistanceFromPoint(pRef.getX(), pRef.getY()) >
               tickMarksLine2.getDistanceFromPoint(pRef.getX(), pRef.getY()));
         tickMarkXY.getPointAlongLine(p, tickLenMI/2.0 + gapMI, pRef, labelsFlag);
      }
      labelsLine = axisLine.getParallelLine(p.getX(), p.getY());

      // for each tick location, find 3D grid line for that tick and project it to 2D, then intersect it with the
      // computed lines above to get tick mark end points and label location!
      for(int i=0; i<tickLocations.length; i++)
      {
         tickMarkXY = parent.getLineXYContainingTickMark(tickLocations[i]);
         
         tickPolyline.add(tickMarksLine1.getIntersection(tickMarkXY, null));
         tickPolyline.add(tickMarksLine2.getIntersection(tickMarkXY, null));
         tickPolyline.add(new Point2D.Double(Double.NaN, Double.NaN));
         labelLocs.add(labelsLine.getIntersection(tickMarkXY, null));
         labels.add(formatTickLabel(i, tickLocations[i]));
      }
      
      // determine H and V alignment for tick labels. When the line connecting the label locations is near horizontal,
      // H alignment is centered; else leading or trailing depending on whether the line is right or left of the 
      // previously calculated reference point. When line is nearly vertical, V alignment is centered, else leading or 
      // trailing depending on whether the line lies below or above the reference point.
      double aoi = Math.abs(labelsLine.getAngleOfInclination());
      hAlign = TextAlign.CENTERED;
      if(aoi >= 10) hAlign = labelsLine.liesLeft(pRef.getX(), pRef.getY()) ? TextAlign.TRAILING : TextAlign.LEADING;
      vAlign = labelsLine.liesAbove(pRef.getX(), pRef.getY()) ? TextAlign.TRAILING : TextAlign.LEADING;
      if(aoi >= 80) vAlign = TextAlign.CENTERED;
      
      // update the painters
      tickPainter.setLocationProducer(new ListBasedProducer<>(tickPolyline));
      labelPainter.setLocationProducer(new ListBasedProducer<>(labelLocs));
      labelPainter.setStringProducer(new ListBasedProducer<>(labels));
      labelPainter.setAlignment(hAlign, vAlign);
   }

   /**
    * Format a tick mark label. Normally, the label is the string form of the tick mark's numeric location along its 
    * parent axis, formatted IAW the tick set node's label format. However, there are two important exceptions:
    * <ol>
    * <li>If the label format is {@link LabelFormat#NONE}, then all tick labels are empty strings.</li>
    * <li>If any custom tick mark labels are defined, these are used instead of numeric labels. If there are more custom
    * labels than there are tick marks, then some labels are unused; if there are too few, the custom labels are reused
    * to ensure all rendered tick marks are labeled.</li>
    * </ol>
    * 
    * <p>When the parent axis range is something like [1e-4 9e-4] or [1e5 1e6], it makes sense to use scientific
    * notation to keep the tick labels compact. But then every label has the form "1.25e-4" or "1.8e5". In this scenario
    * the parent axis will have a non-zero power-of-10 scale factor -- see {@link FGNGraphAxis#getPowerScale()} -- and 
    * the scale factor is included in the axis label as, e.g., "(x1e-4)". Thus, the tick values should be divided by the
    * axis scale factor before converting to string from IAW the tick label format (0-3 fractional digits).</p>
    * 
    * <p>Let T be the tick value <b>after</b> dividing by the axis scale factor. If 0.001 &ge; |T| &lt; 1e6, then T is 
    * converted to string form in decimal notation with up to 0, 1, 2 or 3 fractional digits preserved -- depending on 
    * the tick label format. Otherwise, T is converted to scientific notation, with the desired number of fractional
    * digits preserved in the significand.</p>
    * 
    * @param pos The ordinal position of the tick mark in the list of tick marks generated. Applicable only when custom
    * tick mark labels replace the standard numerical labels; otherwise ignored.
    * @param value The numerical value corresponding to tick mark's calculated location along its parent axis.
    * @return The string form of the tick mark label, as described.
    */
   private String formatTickLabel(int pos, double value)
   {
      LabelFormat fmt = getTickLabelFormat();
      Axis3DNode parent = getParentAxis3D();
      if(fmt == LabelFormat.NONE || parent == null) return( "" );
      
      if(customTickLabels.length > 0) return(customTickLabels[pos % customTickLabels.length]);
      
      if(value == 0) return("0");
      
      double scaledVal = value / Math.pow(10, parent.getPowerScale());
      double absV = Math.abs(scaledVal);
      int nDigits = 0;
      if(fmt == LabelFormat.F1) nDigits = 1; 
      else if(fmt == LabelFormat.F2) nDigits = 2;
      else if(fmt == LabelFormat.F3) nDigits = 3;

      // on a linear axis, if the tick value is non-zero but much closer to zero than the tick interval size, then 
      // treat it as exactly zero. This might happen due to the floating-math used to compute tick mark values.
      double scaledIntv = interval / Math.pow(10, parent.getPowerScale());
      if((!parent.isLogarithmic()) && Math.abs(scaledIntv)/absV > Math.pow(10, nDigits))
         return("0");

      // use scientific notation for values smaller than 0.001 and greater than or equal to 1e6
      if(absV >= 1.0e6 || absV < 0.001)
      {
         int exp = (int) Math.floor(Math.log10(absV));
         scaledVal /= Math.pow(10, exp);
         
         double scale = Math.pow(10, nDigits);
         long truncValue = Math.round(scaledVal * Math.pow(10, nDigits));
         String strVal = Utilities.toString(((double)truncValue)/scale, 7, nDigits);
         return(strVal + "e" + exp);
      }
      
      double scale = Math.pow(10, nDigits);
      long truncValue = Math.round(scaledVal * Math.pow(10, nDigits));
      return(Utilities.toString(((double)truncValue)/scale, 7, nDigits));
   }


   // 
   // Object
   //

   /** Override ensures that rendering infrastructure for the 3D tick set clone is independent of the element cloned. */
   @Override public Ticks3DNode clone() throws CloneNotSupportedException
   {
      Ticks3DNode copy = (Ticks3DNode) super.clone();
      copy.tickLocations = null;
      copy.tickPainter = null;
      copy.tickPolyline = null;
      copy.labelPainter = null;
      copy.labelLocs = null;
      copy.labels = null;
      copy.rBoundsSelf = null;
      return(copy);
   }

}
