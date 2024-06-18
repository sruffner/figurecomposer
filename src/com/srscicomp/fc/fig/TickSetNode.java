package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.PolylinePainter;
import com.srscicomp.common.g2dutil.StringPainter;
import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;

/**
 * <code>TickSetNode</code> defines a set of tick marks and associated numeric labels appearing along a graph axis in 
 * <em>DataNav</em>. It is a "leaf" node and appears only as a child of an <code>AxisNode</code>. 
 * 
 * <p>A <code>TickSetNode</code> possesses most of the shared styling attributes managed in the base class (font-related 
 * properties, stroke color and width, and text/fill color), plus additional properties governing the placement of the 
 * tick marks along the parent axis and the appearance of the tick marks and labels. Tick mark locations are governed
 * by the tick set range endpoints, the tick interval, and two auto-adjustment features:
 * <ul>
 *    <li><i>Tracking parent axis range</i>. If enabled, the <i>start</i> and <i>end</i> of the tick set's range 
 *    always matches that of its parent axis. If disabled, both <i>start</i> and <i>end</i> are set explicitly on the
 *    tick set node.</li>
 *    <li><i>Parent axis is auto-scaled</i>. If tick set is the FIRST tick set child of an axis that is auto-scaled, 
 *    then: (1) its validated range ALWAYS tracks that of the parent axis; (2) its tick interval and format are auto-
 *    adjusted (whenever possible) so that the number of axis divisions defined by the tick set is between 1 and 5 (or 
 *    8 for the first tick set of the theta axis for a polar graph in the <i>allQuad</i> layout). No such adjustments 
 *    are made for any tick set child of an axis other than the first.</li>
 * </ul>
 * </p>
 * 
 * <p>For a description of the algorithm used to determine tick placement, see <code>getTickSet()</code>.</p>
 * 
 * @author sruffner
 */
public class TickSetNode extends FGraphicNode implements Cloneable
{
   /**
    * Construct a <code>TickSetNode</code> defining tick marks along its parent axis. Initially, its range tracks that
    * of the parent axis. All other tick set properties are initialized IAW user-defined preferences. All style 
    * attributes are initially implicit (ie, inherited from parent).
    */
   public TickSetNode()
   {
      super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS);
      interval = 10;
      start = 0;
      end = 100;
      trackingParentAxis = true;
      decadeTicks = FGNPreferences.getInstance().getPreferredLogTickPattern();
      tickOrientation = FGNPreferences.getInstance().getPreferredTickOrientation();
      tickLength = FGNPreferences.getInstance().getPreferredTickLength();
      tickGap = FGNPreferences.getInstance().getPreferredTickGap();
      tickLabelFormat = FGNPreferences.getInstance().getPreferredTickLabelFormat();
   }

   /**
    * Return a reference to the graph axis containing this <code>TickSetNode</code>. 
    * 
    * @return The parent axis, or <code>null</code> if this node currently has no parent.
    */
   private FGNGraphAxis getParentAxis()
   {
      FGraphicNode p = getParent();
      if(p == null) return(null);
      assert(p instanceof FGNGraphAxis);
      return((FGNGraphAxis)p);
   }

   /**
    * Are this the tick set's range, interval, and tick mark label format automatically adjusted? This will be the case
    * only if this node is the parent axis's major tick set (first child) and auto-scaling is enabled for the axis.
    * 
    * @return True iff this tick set's range, interval and label format are controlled by the axis auto-scaling feature.
    * When this is the case, the user cannot modify these properties directly.
    */
   public boolean isAutoAdjusted()
   {
      FGNGraphAxis axis = getParentAxis();
      return(axis != null && axis.isAutoranged() && axis.getChildAt(0) == this);
   }
   
   /**
    * This updates the tick interval and label format directly without informing the model change infrastructure. It
    * is intended only to be used when auto-adjusting the interval and tick label format in response to an automated
    * rescaling of the parent axis range. Method has no effect if auto-scaling is NOT enabled on parent axis.
    * 
    * @param intv The new tick interval (assumed valid).
    * @param fmt The new tick label format.
    */
   void fixTickIntervalAndFormat(double intv, LabelFormat fmt)
   {
      if(!isAutoAdjusted()) return;
      interval = intv;
      tickLabelFormat = fmt;
   }
   
   //
   // Support for child nodes
   //

   /**
    * A <code>TickSetNode</code> admits no children.
    * @see FGraphicNode#isLeaf()
    */
   @Override public boolean isLeaf() { return(true); }

   @Override public FGNodeType getNodeType() { return(FGNodeType.TICKS); }

   /**
    * A <code>TickSetNode</code> admits no children.
    * @see FGraphicNode#canInsert(FGNodeType)
    */
   @Override public boolean canInsert(FGNodeType nodeType) { return(false); }


   //
   // Properties
   //

   /** Interval between adjacent tick marks rendered by this <code>TickSetNode</code> (implicit "user" units). */
   private double interval;

   /**
    * Get the interval between adjacent tick marks rendered by this <code>TickSetNode</code>. If this is the major tick
    * set of an auto-scaled axis, the method returns the interval calculated the last time the parent axis was 
    * auto-scaled.
    * @return Tick mark interval, in whatever units are attached to the parent axis ("user" units).
    */
   public double getInterval() { return(interval); }

   /**
    * Set the interval between adjacent tick marks rendered by this <code>TickSetNode</code>. If a change was made, the 
    * method <code>onNodeModified()</code> is invoked. The interval cannot be set here if this is the major tick set of
    * an auto-scaled axis; in this case, the tick interval and format are set whenever the axis is auto-scaled.
    * 
    * @param d The new tick mark interval. Rejected if infinite, NaN. Also rejected if this is the major tick set of an
    * auto-scaled axis. 
    * @return True if successful; false otherwise.
    */
   public boolean setInterval(double d)
   {
      if(isAutoAdjusted() || !Utilities.isWellDefined(d)) return(false);
      if(interval != d)
      {
         if(doMultiNodeEdit(FGNProperty.INTV, d)) return(true);
         
         Double old = interval;
         interval = d;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.INTV);
            FGNRevEdit.post(this, FGNProperty.INTV, interval, old, "Change tick set interval");
         }
      }
      return(true);
   }

   /**
    * Start of the range over which this <code>TickSetNode</code> renders tick marks along its parent axis (implicit 
    * "user" units).
    */
   private double start;

   /**
    * Get the start of the range over which this <code>TickSetNode</code> renders tick marks along its parent axis. If 
    * this node is currently tracking its parent axis, or if it is the major tick set of an auto-scaled axis, the 
    * method will always return the start of the axis range.
    * 
    * @return Start of tick set range, in whatever units are attached to the parent axis ("user" units).
    */
   public double getStart()
   {
      FGNGraphAxis axis = getParentAxis();
      if(axis != null && (isAutoAdjusted() || trackingParentAxis))
         start = axis.getStart();
      return(start);
   }

   /**
    * Set the start of the range over which this <code>TickSetNode</code> renders tick marks along its parent axis. If 
    * a change was made, the method <code>onNodeModified()</code> is invoked. The property cannot be changed if this 
    * tick set node is currently tracking its parent axis, or if it is the major tick set of an auto-scaled axis.
    * 
    * @param d New value for the start of tick set range. Rejected if infinite, NaN. Also rejected if node is currently
    * tracking the parent axis range, or if it is the major tick set of an auto-scaled axis. 
    * @return True if successful; false otherwise.
    */
   public boolean setStart(double d)
   {
      if(isAutoAdjusted() || trackingParentAxis || !Utilities.isWellDefined(d)) return(false);
      if(start != d)
      {
         if(doMultiNodeEdit(FGNProperty.START, d)) return(true);
         
         Double old = start;
         start = d;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.START);
            FGNRevEdit.post(this, FGNProperty.START, start, old, "Change start of tick set range");
         }
      }
      return(true);
   }

   /**
    * End of the range over which this <code>TickSetNode</code> renders tick marks along its parent axis (implicit 
    * "user" units).
    */
   private double end;

   /**
    * Get the end of the range over which this <code>TickSetNode</code> renders tick marks along its parent axis. If 
    * this node is currently tracking its parent axis, or if it is the major tick set of an auto-scaled axis, the 
    * method will always return the end of the axis range.
    * 
    * @return End of tick set range, in whatever units are attached to the parent axis ("user" units).
    */
   public double getEnd()
   {
      FGNGraphAxis axis = getParentAxis();
      if(axis != null && (isAutoAdjusted() || trackingParentAxis))
         end = axis.getEnd();
      return(end);
   }

   /**
    * Set the end of the range over which this <code>TickSetNode</code> renders tick marks along its parent axis. If 
    * a change was made, the method <code>onNodeModified()</code> is invoked. The property cannot be changed if this 
    * tick set node is currently tracking its parent axis, or if it is the major tick set of an auto-scaled axis.
    * 
    * @param d New value for the end of tick set range. Rejected if infinite, NaN. Also rejected if node is currently
    * tracking the parent axis range, or if it is the major tick set of an auto-scaled axis. 
    * @return True if successful; false otherwise.
    */
   public boolean setEnd(double d)
   {
      if(isAutoAdjusted() || trackingParentAxis || !Utilities.isWellDefined(d)) return(false);
      if(end != d)
      {
         if(doMultiNodeEdit(FGNProperty.END, d)) return(true);
         
         Double old = end;
         end = d;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.END);
            FGNRevEdit.post(this, FGNProperty.END, end, old, "Change end of tick set range");
         }
      }
      return(true);
   }

   /**
    * Whenever this flag is set, the range over which this <code>TickSetNode</code> renders tick marks will be the same 
    * as the range defined on the parent <code>AxisNode</code>. <b>NOTE: This is not a persisted property of the tick
    * set node. If set, then the <i>start, end</i> attributes will be implicit (ie, inherited from parent axis).
    */
   private boolean trackingParentAxis;

   /**
    * Is this <code>TickSetNode</code>'s range tracking that of its parent axis?
    * 
    * @return True if tick set range follows the parent axis range; false otherwise.
    */
   public boolean isTrackingParentAxis() { return(trackingParentAxis); }

   /**
    * Set flag which determines whether or not this <code>TickSetNode</code>'s range tracks that of its parent axis.
    * If the flag is turned on and causes a change in the current tick set range, <code>onNodeModified()</code> is
    * invoked.
    * 
    * @param b True to force tick set range to track parent axis range; false otherwise.
    */
   public void setTrackingParentAxis(boolean b)
   {
      if(trackingParentAxis != b)
      {
         double oldStart = start;
         double oldEnd = end;
         trackingParentAxis = b;
         boolean notified = false;
         if(trackingParentAxis)
         {
            FGNGraphAxis axis = getParentAxis();
            if((axis != null) && (start != axis.getStart() || end != axis.getEnd()))
            {
               start = axis.getStart();
               end = axis.getEnd();
               onNodeModified(FGNProperty.START);
               notified = true;
            }
         }
         
         if(!notified) 
         {
            FGraphicModel model = getGraphicModel();
            if(model != null) model.onChange(this, 0, false, null);
         }
         FGNRevEdit.postTrackAxisFlagToggle(this, !trackingParentAxis, oldStart, oldEnd);
      }
   }

   /**
    * The figure model's undo/redo infrastructure calls this method to undo or redo a prior change to the state of the
    * tick set's "track axis" flag. Engaging the tracking feature may alter the tick set range, so we have to restore
    * the previous range as well as the track axis flag when we "undo" the operation.
    * 
    * @param track The value of the track axis flag to be restored.
    * @param s The start of the tick set range to be restored.
    * @param e The end of the tick set range to be restored.
    */
   void undoTrackingAxisChange(boolean track, double s, double e)
   {
      trackingParentAxis = track;
      boolean rangeModified = start != s || end != e;
      start = s;
      end = e;
      if(rangeModified) onNodeModified(FGNProperty.START);
      else
      {
         FGraphicModel model = getGraphicModel();
         if(model != null) model.onChange(this, 0, false, null);
      }
   }
   
   /** The pattern of tick marks rendered within a log decade. */
   private LogTickPattern decadeTicks;
   
   /**
    * Get the pattern of tick marks rendered within a log decade -- applicable only when the parent axis of this 
    * <code>TickSetNode</code> is base10-logarithmic.
    * 
    * @return Current log-decade tick pattern.
    */
   public LogTickPattern getDecadeTicks() { return(decadeTicks); }

   /**
    * Set the pattern of tick marks rendered within a log decade by this <code>TickSetNode</code> when its parent axis 
    * is base10-logarithmic. If a change is made, the method <code>onNodeModified()</code> is invoked.
    * 
    * @param p The new log-decade tick pattern. A <code>null</code> value is rejected.
    * @return <code>True</code> if successful; <code>false</code> if argument was <code>null</code>.
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

   /** An enumeration of the different ways tick marks are oriented relative to an axis and the graph's data box. */
   public enum Orientation
   {
      /** Tick marks start on axis line and are drawn toward graph's data box. */
      IN,
      
      /** Tick marks start on axis line and are drawn away from graph's data box. */
      OUT, 
      
      /** Tick marks bisect the axis line. */
      THRU;

      @Override
      public String toString() { return(super.toString().toLowerCase()); }
   }

   /** Tick mark orientation. */
   private Orientation tickOrientation;

   /**
    * Get the current orientation of tick marks rendered by this <code>TickSetNode</code>.
    * @return Current tick mark orientation.
    */
   public Orientation getTickOrientation() { return(tickOrientation); }

   /**
    * Set the orientation of tick marks rendered by this <code>TickSetNode</code>. If a change is made, the method 
    * <code>onNodeModified()</code> is invoked.
    * 
    * @param o The new tick mark orientation. A <code>null</code> value is rejected.
    * @return <code>True</code> if successful; <code>false</code> if argument was <code>null</code>.
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
    * Constrains tick mark length and the tick-label gap to use physical units, to have up to 6 significant and up to 3
    * fractional digits, and to lie in the range [0..1in], regardless what units are assigned to it.
    */
   public final static Measure.Constraints TICKLENCONSTRAINTS = new Measure.Constraints(0.0, 1000.0, 6, 3);

   /**
    * Get the length of any tick marks rendered by this <code>TickSetNode</code>.
    * 
    * @return The tick mark length with associated units of measure (only physical units allowed).
    */
   public Measure getTickLength() { return(tickLength); }

   /**
    * Set the length of any tick marks rendered by this tick set. If a change is made, {@link #onNodeModified} is
    * invoked.
    * 
    * @param m The new tick length. It is constrained to satisfy {@link #TICKLENCONSTRAINTS}. A null value is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setTickLength(Measure m)
   {
      if(m == null) return(false);
      m = TICKLENCONSTRAINTS.constrain(m);

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
    * Set the tick-label gap for this tick set. If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param m The new tick-label gap. It is constrained to satisfy {@link #TICKLENCONSTRAINTS}. Null is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setTickGap(Measure m)
   {
      if(m == null) return(false);
      m = TICKLENCONSTRAINTS.constrain(m);

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

   /** An enumeration of the different numeric tick mark label formats supported by <code>TickSetNode</code>. */
   public enum LabelFormat
   {
      /** Tick mark labels are not drawn. */
      NONE("none"),
      
      /** Tick mark labels are drawn as integers, rounded as needed. */
      INT("1"), 
      
      /** Tick mark labels are draws as floating-pt numbers, with at most one digit after the decimal pt. */
      F1("1.1"),

      /** Tick mark labels are draws as floating-pt numbers, with at most two digits after the decimal pt. */
      F2("1.12"),

      /** Tick mark labels are draws as floating-pt numbers, with at most three digits after the decimal pt. */
      F3("1.125");

      LabelFormat(String guiLabel) { this.guiLabel = guiLabel; }
      
      private final String guiLabel;
      
      @Override public String toString() { return(super.toString().toLowerCase()); }
      
      public String getGUILabel() { return(guiLabel); }
   }

   /** Tick mark numeric label format. */
   private LabelFormat tickLabelFormat;

   /**
    * Get the current format for any tick mark labels rendered by this <code>TickSetNode</code>.
    * @return Current tick mark label format.
    */
   public LabelFormat getTickLabelFormat() { return(tickLabelFormat); }

   /**
    * Set the format for tick mark labels rendered by this <code>TickSetNode</code>. If a change is made, the method 
    * <code>onNodeModified()</code> is invoked. The property cannot be changed if this tick set node is the major tick 
    * set of an auto-scaled axis.
    * 
    * @param fmt The new tick mark label format. Null is rejected. Also rejected if node is the major tick set of an 
    * auto-scaled axis. 
    * @return <code>True</code> if successful; <code>false</code> if argument was <code>null</code>.
    */
   public boolean setTickLabelFormat(LabelFormat fmt)
   {
      if(fmt == null || isAutoAdjusted()) return(false);
      if(tickLabelFormat != fmt)
      {
         if(doMultiNodeEdit(FGNProperty.FMT, fmt)) return(true);
         
         LabelFormat old = tickLabelFormat;
         tickLabelFormat = fmt;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.FMT);
            FGNRevEdit.post(this, FGNProperty.FMT, tickLabelFormat, old, 
                  "Set tick label fromat to " + tickLabelFormat.toString());
         }
      }
      return(true);
   }

   /** 
    * Array of custom tick mark labels. Normally this array is empty, and numeric labels are generated IAW the actual
    * tick locations and the current label format. If it is not empty, tick marks are labeled instead with the tokens
    * in this array, in order. If there are more labels than tick marks, the extra labels are ignored. If there are too
    * few, the labels are reused until all tick marks are labeled.
    */
   private String[] customTickLabels = new String[0];
   
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
   
   /**
    * Find the longest tick mark label for this tick set.
    * @return The tick label with the most characters. Returns an empty string if there are no tick labels.
    */
   public String getLongestTickLabel()
   {
      updatePainters();
      String longest = "";
      for(String s : labels) if(s.length() > longest.length()) longest = s;
      return(longest);
   }
   
   @Override protected void rescaleSelf(double scale, MultiRevEdit undoer)
   {
      Measure.Constraints c = TICKLENCONSTRAINTS;
      double d = tickLength.getValue();
      if(d > 0)
      {
         Measure old = tickLength;
         tickLength = c.constrain(new Measure(d*scale, tickLength.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.LEN, tickLength, old);
      }

      d = tickGap.getValue();
      if(d > 0)
      {
         Measure old = tickGap;
         tickGap = c.constrain(new Measure(d*scale, tickGap.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.GAP, tickGap, old);
      }
   }

   /**
    * The first <code>TickSetNode</code> defined on an <code>AxisNode</code> represents the "major tick marks" for that 
    * axis. The major tick marks determine the locations of the relevant grid lines in the graph, if those grid lines 
    * are currently turned on. In that case, this override -- in addition to triggering a re-rendering of the tick set 
    * itself -- will force a re-rendering of the graph's data box to ensure that rendered grid lines are updated 
    * properly to reflect any changes in major tick mark locations.</p>
    * 
    * @see FGraphicNode#onNodeModified(Object)
    */
   @Override
   protected void onNodeModified(Object hint)
   {
      // this will generate a rendering update to account for changes to the tick set itself
      super.onNodeModified(hint);
      
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      // notify parent graph if this is the major tick set (ie, the first one) of the parent axis. The graph may need 
      // to update the corresponding grid lines. NOT APPLICABLE to the tick set of a color bar, for which there are no
      // associated grid lines. Also not applicable to the specialized polar plot and 3D graph containers.
      FGNGraphAxis axis = getParentAxis();
      if(axis != null && axis.getMajorTickSet() == this)
      {
         if(hint == null || hint == FGNProperty.START || hint == FGNProperty.END || hint == FGNProperty.INTV ||
               hint == FGNProperty.PERLOGINTV)
         {
            FGNGraph g = axis.getParentGraph();
            if((g instanceof GraphNode) && g.getColorBar() != axis)
               ((GraphNode)g).onAxisMajorTicksChanged(axis.isPrimary());
         }
      }
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok;
      switch(p)
      {
         case GAP: ok = setTickGap((Measure) propValue); break;
         case LEN: ok = setTickLength((Measure) propValue); break;
         case PERLOGINTV: ok = setDecadeTicks((LogTickPattern)propValue); break;
         case END: ok = setEnd((Double)propValue); break;
         case INTV: ok = setInterval((Double)propValue); break;
         case START: ok = setStart((Double)propValue); break;
         case FMT: ok = setTickLabelFormat((LabelFormat)propValue); break;
         case DIR: ok = setTickOrientation((Orientation)propValue); break;
         case CUSTOMTCKLBL: setCustomTickLabels((String[]) propValue); ok = true; break;
         default: ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value;
      switch(p)
      {
         case GAP: value = getTickGap(); break;
         case LEN: value = getTickLength(); break;
         case PERLOGINTV: value = getDecadeTicks(); break;
         case END: value = getEnd(); break;
         case INTV: value = getInterval(); break;
         case START: value = getStart(); break;
         case FMT: value = getTickLabelFormat(); break;
         case DIR: value = getTickOrientation(); break;
         case CUSTOMTCKLBL: value = getCustomTickLabels(); break;
         default: value = super.getPropertyValue(p); break;
      }
      return(value);
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
    * The node-specific properties exported in a tick set node's style set are the tick direction and length, the format
    * of tick mark labels, and the gap separating each tick from its numeric label. The tick set range, interval, and
    * log decade tick pattern are NOT included -- these are considered specific to each tick set.
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
      if(m != null) m = TICKLENCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.LEN, nt, Measure.class)))
      {
         tickLength = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LEN);

      m = (Measure) applied.getCheckedStyle(FGNProperty.GAP, nt, Measure.class);
      if(m != null) m = TICKLENCONSTRAINTS.constrain(m);
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
   // Computing tick mark locations
   //

   /**
    * Get the validated range over which this <code>TickSetNode</code> renders tick marks. The nominal range is based 
    * on the values returned by <code>getStart()</code> and <code>getEnd()</code>. The following corrections are 
    * applied, as needed:
    * <ul>
    *    <li>The nominal range endpoints are separately adjusted to ensure that they are within the validated range of 
    *    the parent axis.</li>
    *    <li>The range endpoints are swapped, if necessary, to ensure that the beginning of the range is less than or 
    *    equal to the end. Even though coordinates on the parent axis range may descend in value, tick marks are 
    *    always populated in an ascending fashion.
    * </ul>
    * 
    * @return A two-element array holding the beginning and end, respectively, of the validated tick set range. The 
    * range endpoints are in the user units associated with the parent axis.
    */
   private double[] getValidRange()
   {
      // get nominal range for tick set.
      double[] range = new double[2];
      range[0] = getStart();
      range[1] = getEnd();

      // ensure that tick set range is a subset of the validated range for the parent axis, if parent axis exists!
      FGNGraphAxis axis = getParentAxis();
      if(axis != null)
      {
         double axisStart = axis.getValidatedStart();
         double axisEnd = axis.getValidatedEnd();
         if( axisStart <= axisEnd ) for( int i=0; i<2; i++ )
         {
            if( range[i] < axisStart ) range[i] = axisStart;
            else if( range[i] > axisEnd ) range[i] = axisEnd;
         }
         if( axisStart > axisEnd ) for( int i=0; i<2; i++ )
         {
            if( range[i] > axisStart ) range[i] = axisStart;
            else if( range[i] < axisEnd ) range[i] = axisEnd;
         }
      }

      // swap endpoints to ensure that start <= end
      if(range[0] > range[1])
      {
         double d = range[1];
         range[1] = range[0];
         range[0] = d;
      }

      return(range);
   }

   /**
    * Get the validated interval between tick marks rendered by this <code>TickSetNode</code>. The nominal interval, 
    * which is under user control, is corrected IAW the current context of the tick set, if necessary. The following
    * rules are applied:
    * <ul>
    *    <li>Sign is ignored. Absolute value of the nominal interval is used.</li>
    *    <li>If parent axis is logarithmic base 10, the validated interval must be a power of 10 greater than 0. The
    *    minimum interval is 10.</li>
    *    <li>If parent axis is logarithmic base 2, the validated interval must be a power of 2 greater than 0. The
    *    minimum interval is 2.</li>
    * </ul>
    * 
    * <p>When this is the major tick set of an auto-scaled axis, then the tick set's range, interval, and label format
    * are controlled by the auto-scaling feature. In this case, the method simply returns the nominal interval, which
    * should have already been set.</p>
    * @return Validated interval between ticks in "user" units.
    */
   private double getValidTickInterval()
   {
      if(isAutoAdjusted()) return(interval);
      
      double validIntv = Math.abs(getInterval());
      FGNGraphAxis axis = getParentAxis();
      if(axis != null && axis.isLogarithmic())
      {
         double expon;
         if(axis.getLogBase() == 2)
         {
            // choose closest power of 2 >= 2^1
            expon = Math.floor( Utilities.log2(validIntv) );
            if( expon < 1 ) expon = 1;
            validIntv = Math.pow(2,expon);
         }
         else
         {
            // choose closest power of 10 >= 10^1
            expon = Math.floor( Utilities.log10(validIntv) );
            if( expon < 1 ) expon = 1;
            validIntv = Math.pow(10,expon);
         }
      }

      return(validIntv);
   }

   /**
    * The maximum number of tick marks rendered for any single tick set. This is needed because a user can easily set 
    * the tick mark interval such that a huge number of ticks have to be rendered (when auto interval adj disabled).
    */
   public final static int MAX_TICKS = 50;

   /**
    * Calculate the locations of all tick marks actually rendered by this <code>TickSetNode</code>.
    * 
    * <p>Algorithm for tick mark placement <i>when automatic interval adjustment is disabled</i>:
    * <ol>
    *    <li>Correct the tick set range and tick interval, as needed. The tick set range must be a subset of the parent 
    *    axis range, and the range endpoints are swapped if the start is greater than the end (ticks are always 
    *    populated in ascending order). The absolute value of the tick interval is used, and additional rules apply for 
    *    a logarithmic axis. For details, see <code>getValidRange()</code> and <code>getValidTickInterval()</code>. Let 
    *    <i>S</i>, <i>E</i>, and <i>D</i> be the validated values for the start of the range, end of the range, and 
    *    tick interval, respectively.</li>
    *    <li>Let <i>Nmax</i> represent the maximum number of ticks that may be rendered for any given tick set, and let 
    *    <i>N</i> be the number of ticks in this tick set. <i>N</i> lies in <i>[0..Nmax]</i>.</li>
    *    <li>If parent axis is NOT logarithmic: If <i>S==E</i> or <i>D==0</i>, a single tick is placed at <i>S</i>. 
    *    Otherwise, the tick locations are given by <i>{S, S+D, S+2*D, ... S+(N-1)*D}</i>, where the total number of 
    *    ticks <i>N</i> is such that <i>(S+(N-1)*D) &le; E) AND ((N==Nmax) OR (S+N*D &ge; E))</i>.</li>
    *    <li>If parent axis is logarithmic and its logarithmic base is 10, the validated interval defines the size of 
    *    the "logarithmic decade" (10, 100, etc). The set of log-decade ticks determines the placement of ticks within 
    *    each decade. Let <i>G</i> be the start of a decade and <i>D</i> its size. Then the log-decade tick pattern 
    *    lets the author put tick marks at any or all of the locations <i>{G*1, G*0.2*D, G*0.3, ... G*0.9*D}</i>. NOTE
    *    that, if the tick pattern is empty, then no ticks are rendered! In what follows, let <i>d&ge;1</i> be the 
    *    integer such that the logarithmic decade <i>D = 10^d</i>. Also assume that that the set of log-decade ticks is 
    *    "1 2 4 8".
    *    <ol>
    *       <li>Find two integers <i>m</i> and <i>n&ge;1</i> such that <i>10^(m+n*d) &gt; E &ge; 10^(m+(n-1)*d)</i> and 
    *       <i>10^(m+d) &gt; S &ge; 10^m</i>.</li>
    *       <li>For the decade <i>[10^m ... 10^(m+d)]</i>, ticks are placed at <i>10^m</i> (for the "1" in the decade
    *       tick pattern), <i>2*10^(m+d-1)</i>, <i>4*10^(m+d-1)</i>, and <i>8*10^(m+d-1)</i>, so long as the value also 
    *       lies in <i>[S..E]</i>.</li>
    *       <li>Proceed similarly for each succeeding decade until the maximum number of ticks <i>Nmax</i> has been 
    *       reached, or the final decade <i>[10^(m+(n-1)*d) ... 10^(m+n*d)]</i> has been processed.</li>
    *       <li>Observe that, if <i>S==E</i>, the tick set could be empty in this context.</li>
    *    </ol>
    *    </li>
    *    <li>If parent axis is logarithmic and its logarithmic base is 2, the validated interval defines the size of the
    *    "logarithmic octave" (2, 4, 8, 16, etc). In what follows, let <i>d&ge;1</i> be an integer such that the 
    *    logarithmic octave <i>D = 2^d</i>.
    *    <ol>
    *       <li>Find integer <i>m</i> such that <i>2^(m-1) &lt; S &le; 2^m &le; E</i>. If it does not exist, tick set 
    *       is empty.</li>
    *       <li>Otherwise, tick set is <i>{2^m, 2^(m+d), 2^(m+2*d), ... 2^(m+(N-1)*d)}</i>, where 
    *       <i>(2^(m+(N-1)*d) &le; E) AND ((N==Nmax) OR (2^(m+N*d) &gt; E))</i>.</li>
    *    </ol>
    *    </li>
    * </ol>
    * </p>
    * 
    * <p>Algorithm for tick mark placement <i>when adjusting interval for a fixed number of ticks N</i>. NOTE that the
    * idea of keeping the number of ticks fixed is primarily applicable to a linear axis. For a logarithmic axis, the
    * number of ticks is governed by the validated range, whether it's log2 or log10, and the log-decade tick pattern 
    * (log10 case only). In this case, <i>N-1</i> is the desired number of logarithmic decades or octaves that span the
    * validated range. It will only be possible to honor the request if the range spans 10^(N*d) or 2^(N*d), for some
    * integer <i>d&ge;1</i>.
    * <ol>
    *    <li>Get validated range for tick set: <i>[S..E]</i>.</li>
    *    <li>If parent axis is NOT logarithmic, calculate tick interval <i>T = (E-S)/(N-1)</i>, then place the tick 
    *    marks at <i>{S, S+T, .. , S+(N-2)*T, E}</i>. <i>Special cases</i> (effective interval is <i>E-S</i>):
    *    <ol>
    *       <li>For <i>N==1</i>, always return {S}.</li>
    *       <li>For <i>N==2</i>, always return {S, E}.</li>
    *    </ol>
    *    </li>
    *    <li>If parent axis is logarithmic in base 10, the tick set is likewise log10. As above, assume that the set of 
    *    log-decade ticks is "1 2 4 8". Again, if the set of log-decade ticks is empty, no ticks are rendered!
    *    <ol>
    *       <li>Find integer <i>m</i> such that <i>10^m &le; S &lt; 10^(m+1)</i>.</li>
    *       <li>Find integer <i>d&ge;1</i> such that <i>10^(m+(N-1)*d) &le; E &lt; 10^(m+N*d)</i>. If there is no such
    *       integer, then <i>d=1</i> and the number of log decades P spanning the range is less than <i>N</i>. The
    *       effective interval for the tick set will be <i>10^d</i>.</li>
    *       <li>For the decade <i>[10^m ... 10^(m+d)]</i>, ticks are placed at <i>10^m</i> (for the "1" in perLogIntv), 
    *       <i>2*10^(m+d-1)</i>, <i>4*10^(m+d-1)</i>, and <i>8*10^(m+d-1)</i>, so long as the value also lies in 
    *       [S..E].</li>
    *       <li>Proceed similarly for each succeeding decade until the final decade <i>[10^(m+(P-1)*d) ... 
    *       10^(m+P*d)]</i> has been processed; here <i>1&le;P&le;N</i>.</li>
    *    </ol>
    *    </li>
    *    <li>If parent axis is logarithmic in base 2, the tick set is likewise log2.
    *    <ol>
    *       <li>Find integer <i>m</i> such that <i>2^(m-1) &lt; S &le; 2^m &le; E</i>. If it does not exist, tick set 
    *       is empty.</li>
    *       <li>Find integer <i>d&ge;1</i> such that <i>2^m+(N-1)*d) &le; E &lt; 2^(m+N*d)</i>. If it does not exist, 
    *       then <i>d=1</i>. The effective interval for the tick set will be <i>2^d</i>.</li>
    *       <li>The tick set is <i>{2^m, 2^(m+d), ... 2^(m+(P-1)*d)}</i>, where <i>P&le;N</i>.</li>
    *    </ol>
    *    </li>
    * </ol>
    * 
    * @return Array containing tick mark locations in the native coordinates of the parent axis. If the current context
    * and definition of the tick set admit no tick marks, this method returns an empty array.
    */
   public double[] getTickSet()
   {
      FGNGraphAxis axis = getParentAxis();
      if(axis == null) return(NOTICKS);

      List<Double> ticks = new ArrayList<>();
      double[] range = getValidRange();
      double intv = getValidTickInterval();
      boolean isLog = axis.isLogarithmic();
      boolean isLog10 = isLog && (axis.getLogBase() == 10);
      
      if( !isLog )
      {
         // special case:  if intv is zero, then we just do one tick at start of range
         if( intv == 0 ) return( new double[] {range[0]} );

         double d = range[0];
         while( d <= range[1]+0.001*intv && ticks.size() < MAX_TICKS)
         {
            ticks.add(d);
            d += intv;
         }
      }
      else if( isLog10 )
      {
         int[] perLogIntv = decadeTicks.getEnabledTicks();

         // find exponent m such that 10^m <= S < 10^(m+1)
         int m = (int) Math.floor( Utilities.log10(range[0]) );
         
         // now populate the tick set...
         double epochStart = Math.pow(10,m);
         while( epochStart <= range[1] && ticks.size() <= MAX_TICKS )
         {
            for( int i=0; i<perLogIntv.length && ticks.size() <= MAX_TICKS; i++ )
            {
               int j = perLogIntv[i];
               double tick = epochStart * ((j==1) ? 1 : intv*0.1*((double)j));
               if( range[0] <= tick && tick <= range[1] )
                  ticks.add(tick);
            }
            epochStart *= intv;
         }
      }
      else
      {
         // find smallest power of 2 that is >= S
         int m = (int) Math.floor( Utilities.log2(range[0]) );
         double octave = Math.pow(2,m);
         if( octave < range[0] ) octave *= 2;

         // populate tick set (it will be empty if 2^m > E)
         while( octave <= range[1] && ticks.size() <= MAX_TICKS )
         {
            ticks.add(octave);
            octave *= intv;
         }
      }

      // return set of ticks as an array
      double[] result = new double[ticks.size()];
      for( int i=0; i<ticks.size(); i++ )
         result[i] = ticks.get(i);
      return( result );
   }

   /** An empty double-valued array representing an empty tick set. */
   public final static double[] NOTICKS = new double[] {};

   /**
    * Convert the specified set of tick mark locations from user units to thousandth-inches WRT the viewport of the 
    * parent axis -- which is identical to the viewport of this <code>TickSetNode</code>.
    * 
    * <p>Special adjustments are made for any tick mark lying at an endpoint of the axis line in an attempt to achieve
    * a nice join with the separately rendered axis line:
    * <ul>
    *    <li>In certain situations, the axis line is padded at both ends so that it aligns nicely with the orthogonal 
    *    axis in any single-quadrant layout. The same adjustment must therefore be made to a tick aligning with either 
    *    end of the axis line.</li>
    *    <li>If the tick bisects the axis and the axis line is stroked using a round- or square-projection endcap, we
    *    shift the tick mark out by 1/2 the width of the axis line, then in again by 1/2 its own stroke width, so that
    *    its outer edge just covers the axis's endcap projection.</li>
    *    <li>If the tick does NOT bisect the axis and <b>both</b> tick and axis are butt-ended, we shift the tick in by
    *    1/2 its own stroke width.</li>
    * </ul>
    * Note that nothing is done when the tick does not bisect the axis and either tick or axis is NOT butt-ended -- the
    * assumption being that the projection from either tick or axis will clean-up the join. This won't be the case if 
    * the tick stroke width does not match the axis stroke width.</p>
    * 
    * @param xTicks Array of tick locations expressed in "user" units.
    * @return A cloned array of x-coords, converted to thousandth-inches WRT parent axis viewport. If there are no tick 
    * marks or if the axis viewport is undefined for whatever reason, an empty array is returned.
    */
   private double[] convertTickLocationsToThousandthInches( double[] xTicks )
   {
      if( xTicks == null ) return(NOTICKS);

      // get viewport for axis line -- used to convert tick mark locations in user units to thousandth-inches
      FGNGraphAxis axis = getParentAxis();
      if(axis == null) return(NOTICKS);
      FViewport2D axisViewport = axis.getViewport();
      if(axisViewport == null) return(NOTICKS);

      // this information is needed to fine-tune physical locations of tick marks at parent axis endpoints
      double axisStart = axis.getValidatedStart();
      double axisEnd = axis.getValidatedEnd();
      double hAdj = axis.getAxisEndpointAdjustment();
      boolean isAxisButtEnded = (axis.getStrokeEndcap() == StrokeCap.BUTT);
      boolean isButtJoint = isAxisButtEnded && (getStrokeEndcap() == StrokeCap.BUTT);
      double axisStrokeW = axis.getStrokeWidth();
      double strokeW = getStrokeWidth();

      // we'll return a new array rather than modifying the array provided
      double[] xMils = new double[xTicks.length];

      // if there are any ticks, convert to milli-inches WRT axis viewport
      Point2D pt = new Point2D.Double(0,0);
      Orientation ori = getTickOrientation();
      for(int i = 0; i < xMils.length; i++)
      {
         pt.setLocation(xTicks[i], 0);
         axisViewport.userUnitsToThousandthInches(pt);
         xMils[i] = pt.getX();

         // special adjustments for ticks at the axis line endpoints
         if(xTicks[i] == axisStart)
         {
            xMils[i] -= hAdj;
            if(ori == Orientation.THRU && !isAxisButtEnded) xMils[i] += (strokeW - axisStrokeW) / 2.0;
            if(ori != Orientation.THRU && isButtJoint) xMils[i] += strokeW/2.0;
         }
         else if(xTicks[i] == axisEnd)
         {
            xMils[i] += hAdj;
            if(ori == Orientation.THRU && !isAxisButtEnded) xMils[i] -= (strokeW - axisStrokeW) / 2.0;
            if(ori != Orientation.THRU && isButtJoint) xMils[i] -= strokeW/2.0;
         }
      }

      return( xMils );
   }


   //
   // Focusable/Renderable support
   //
   
   /** 
    * Since the coordinate system for rendering a <code>TickSetNode</code> is the same as that of the parent axis, 
    * this method simply returns the identity transform.
    * 
    * @see FGraphicNode#getLocalToParentTransform()
    */
   public AffineTransform getLocalToParentTransform()
   {
      return( new AffineTransform() );
   }

   /**
    * The <code>TickSetNode</code> does not admit children and does not define its own viewport, so the parent viewport 
    * is always returned.
    * 
    * @see FGraphicNode#getViewport()
    */
   @Override
   public FViewport2D getViewport()
   {
      return(getParentViewport());
   }

   /* (non-Javadoc)
    * @see com.srscicomp.fc.fig.FGraphicNode#releaseRenderResourcesForSelf()
    */
   @Override
   protected void releaseRenderResourcesForSelf()
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

   @Override
   protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null || tickPainter == null)
      {
         updatePainters();
         tickPainter.invalidateBounds();
         labelPainter.updateFontRenderContext(g2d);
         labelPainter.invalidateBounds();

         rBoundsSelf = tickPainter.getBounds2D(rBoundsSelf);
         Utilities.rectUnion(rBoundsSelf, labelPainter.getBounds2D(null), rBoundsSelf);
      }

      return((Rectangle2D)rBoundsSelf.clone());
   }

   /**
    * The implementation uses a <code>PolylinePainter</code> to render the tick marks, using ill-defined points in 
    * the painter's location list so that the tick marks are not connected to each other. A <code>StringPainter</code> 
    * renders the tick mark labels, if any. The painters are typically already configured by the time this method is 
    * called -- during render bound calculations that occur whenever the tick set's definition is modified.
    * 
    * @see com.srscicomp.common.g2dviewer.Renderable#render(Graphics2D, RenderTask)
    */
   public boolean render(Graphics2D g2d, RenderTask task)
   {
      // make sure our internal painters are up-to-date
      if(tickPainter == null) updatePainters();

      // if the tick set lies in any of the task's dirty regions, then render it
      if(needsRendering(task))
      {
         tickPainter.render(g2d, null);
         labelPainter.render(g2d, null);
      }
  
      return(task == null || task.updateProgress());
   }

   //
   // Internal rendering resources
   //

   /**
    * Cached rectangle bounding the marks made by this <code>TickSetNode</code>. If <code>null</code>, the rectangle 
    * has yet to be calculated. Specified in local rendering coordinates -- in this case the rendering coordinates of 
    * the parent axis.
    */
   private Rectangle2D rBoundsSelf = null;

   /**
    * This painter renders the tick marks for this <code>TickSetNode</code>.
    */
   private PolylinePainter tickPainter = null;

   /**
    * This list serves as the location provider for the tick mark painter.
    */
   private List<Point2D> tickPolyline = null;

   /**
    * This painter renders the tick mark labels for this <code>TickSetNode</code>.
    */
   private StringPainter labelPainter = null;

   /**
    * This list serves as the location provider for the tick mark label painter.
    */
   private List<Point2D> labelLocs = null;

   /**
    * This list serves as the string provider for the tick mark label painter.
    */
   private List<String> labels = null;

   /**
    * Update the rendering infrastructure for this <code>TickSetNode</code> IAW its current definition and that of its 
    * containing axis. This infrastructure includes separate painters for the tick marks and tick mark labels, location 
    * lists for both marks and labels, and a list of label strings.
    * 
    * <p>The location of the axis line with respect to the graph's data box is crucial to the layout of tick marks and 
    * associated numeric labels. If the axis is above (or, for a V axis, to the left of) the graphed data, then 
    * "inward" tick marks are drawn downward, "outward" tick marks are drawn upward, and the numeric labels are located 
    * above (or left of) the axis line. Vice versa if the axis is below (to the right of) or within the data box. For a 
    * horizontal axis, each numeric tick label is centered horizontally at the corresponding tick mark's location. If 
    * the axis is above the data box, the labels are drawn above the axis; else below it. For a vertical axis, each 
    * label is centered vertically at the corresponding tick mark's location. If the axis is to the left of the data 
    * box, the labels are right-aligned and drawn to the left of the axis. If the axis is to the right of or within the 
    * data box, the labels are left-aligned and drawn to the right of the axis.</p>
    */
   private void updatePainters()
   {
      // the location/string providers and the painters themselves are lazily created
      if(tickPolyline == null)
         tickPolyline = new ArrayList<>();
      if(labelLocs == null)
         labelLocs = new ArrayList<>();
      if(labels == null)
         labels = new ArrayList<>();
      if(tickPainter == null)
         tickPainter = new PolylinePainter(this, tickPolyline);
      if(labelPainter == null)
         labelPainter = new StringPainter(this, labelLocs, labels);

      // we need access to the parent axis to compute absolute tick mark locations. If there is no parent axis, abort.
      FGNGraphAxis axis = getParentAxis();
      if(axis == null) return;

      // get x-coordinates of valid tick marks, in user units. If there are none, there's nothing to render.  We also 
      // have nothing to render if the tick length is zero and the tick label format is "none".
      double[] xTickUser = getTickSet();
      double tickLen = tickLength.toMilliInches();
      LabelFormat fmt = getTickLabelFormat();
      if((xTickUser.length == 0) || (tickLen==0 && (fmt == LabelFormat.NONE))) 
      {
         // with empty location providers, the two painters will render nothing!
         tickPolyline.clear();
         labelLocs.clear();
         labels.clear();
         return;
      }

      // convert x-coordinates to thousandth-in WRT the parent axis viewport
      double[] xTickMils = convertTickLocationsToThousandthInches( xTickUser );

      // get other information we need to render tick marks
      int tickDir = getTickDirection();
      double tickGapMI = tickGap.toMilliInches();
      boolean isHoriz = axis.isHorizontal();
      boolean isAboveOrLeft = axis.isAboveLeft();
      double axisLen = axis.getLengthInMilliInches();

      // prepare list of polyline locations that render the tick marks. We try to reuse the current content of the 
      // location list, to minimize memory allocations.
      // REM: We render ticks in the parent axis viewport. In that viewport, the axis is always horizontal and lies 
      // not at y=0, but at y=axisLength.
      double y0 = (tickDir == 0) ? tickLen/2 : ((tickDir>0) ? tickLen : 0);
      double y1 = (tickDir == 0) ? -tickLen/2 : ((tickDir>0) ? 0 : -tickLen);
      if(tickLen > 0) 
      {
         while(tickPolyline.size() > xTickMils.length * 3) tickPolyline.remove(0);
         while(tickPolyline.size() < xTickMils.length * 3) tickPolyline.add( new Point2D.Double() );
         for(int i=0; i<xTickMils.length; i++)
         {
            int j = i*3;
            tickPolyline.get(j).setLocation(xTickMils[i], axisLen+y0);
            tickPolyline.get(j+1).setLocation(xTickMils[i], axisLen+y1);
            tickPolyline.get(j+2).setLocation(Double.NaN, Double.NaN);  // so we don't connect individual tick marks!
         }
      }
      else
         tickPolyline.clear();

      // prepare list of locations and label strings for the label painter. Again, we reuse the lists to minimize 
      // memory allcoations.
      if(fmt != LabelFormat.NONE)
      {
         // calculate offset to the aligning edge (top, bottom, left, or right) of the tick labels.  This offset 
         // accounts for the tick gap plus that portion of tick mark on same side of axis line as the label.
         double offset;
         if( isAboveOrLeft )
         {
            offset = tickGapMI;
            if(tickDir == 0) offset += tickLen/2.0;
            else if(tickDir == 1) offset += tickLen;
         }
         else
         {
            offset = -tickGapMI;
            if(tickDir == 0) offset -= tickLen/2.0;
            else if(tickDir == -1) offset -= tickLen;
         }

         // set label painter's horizontal and vertical alignment appropriately. For a H axis: labels are centered 
         // horizontally; they are top-aligned when below the axis, bottom-aligned when above it.  For a V axis: labels 
         // are rotated 90 deg CW WRT the axis line. Once rotated, they are vertically centered and right- or 
         // left-aligned depending on location WRT axis line.
         if( isHoriz )
         {
            labelPainter.setAlignment(TextAlign.CENTERED, isAboveOrLeft ? TextAlign.TRAILING : TextAlign.LEADING);
            labelPainter.setRotation(0);
         }
         else
         {
            labelPainter.setAlignment(isAboveOrLeft ? TextAlign.TRAILING : TextAlign.LEADING, TextAlign.CENTERED);
            labelPainter.setRotation(-90);
         }

         while(labelLocs.size() > xTickMils.length) labelLocs.remove(0);
         while(labelLocs.size() < xTickMils.length) labelLocs.add( new Point2D.Double() );
         while(labels.size() > xTickMils.length) labels.remove(0);
         while(labels.size() < xTickMils.length) labels.add("");
         for(int i=0; i<xTickMils.length; i++)
         {
            labelLocs.get(i).setLocation(xTickMils[i], axisLen+offset);
            labels.set(i, formatTickLabel(i, xTickUser[i]));
         }
      }
      else
      {
         labelLocs.clear();
         labels.clear();
      }
   }

   /** 
    * Determine in which direction tick marks are drawn from the parent axis line. The direction depends upon both the 
    * tick mark orientation and the location of the parent axis WRT the data box of the enclosing graph.
    * 
    * <p>Tick marks are always orthogonal to the parent axis line. If the tick mark orientation is 
    * <code>Orientation.THRU</code>, then ticks bisect the parent axis. Else, they start at the axis line and are 
    * drawn upward or downward:
    * <ul>
    *    <li>If orientation is <code>Orientation.IN</code>, ticks are drawn toward the graph's data box: downward if 
    *    axis is above or left of the data box, upward otherwise.</li>
    *    <li>If orientation is<code>Orientation.OUT</code>, ticks are drawn away from the graph's data box: upward if
    *    axis is above or left of the data box, downward otherwise.</li>
    * </ul>
    * </p>
    * 
    * @return 0 if ticks bisect parent axis, 1 if they are drawn upward from the axis line, and -1 if they are drawn 
    * downward from the axis line.
    */
   private int getTickDirection()
   {
      int direction = 0;
      FGNGraphAxis axis = getParentAxis();
      if(tickOrientation != Orientation.THRU && (axis != null))
      {
         boolean isAboveOrLeft = axis.isAboveLeft();
         if((tickOrientation==Orientation.IN && !isAboveOrLeft) || (tickOrientation==Orientation.OUT && isAboveOrLeft))
            direction = 1;
         else
            direction = -1;
      }
      return(direction);
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
      FGNGraphAxis parent = getParentAxis();
      if(parent == null || fmt == LabelFormat.NONE) return( "" );
      
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
      if((!parent.isLogarithmic()) && (Math.abs(scaledIntv)/absV > Math.pow(10, nDigits)))
         return("0");

      // use scientific notation for values smaller than 0.001 and greater than or equal to 1e6.
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
   // PSTransformable implementation
   //

   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      // get x-coordinates of valid tick marks, in user units.  If there are none, there's nothing to render.  We also 
      // have nothing to render if the node properties are such that the tick marks are not stroked and the tick mark
      // labels are not drawn.
      FGNGraphAxis parent = getParentAxis();
      double[] xTickUser = getTickSet();
      double tickLenMI = tickLength.toMilliInches();
      boolean doTickMarks = tickLenMI > 0 && isStroked();
      boolean doTickLabels = (tickLabelFormat != LabelFormat.NONE) && (getFillColor().getAlpha() > 0);
      if((parent == null) || (xTickUser.length == 0) || !(doTickMarks || doTickLabels))
         return;

      // start the element -- this will update the graphics state IAW this element's stroke/fill colors, etc
      psDoc.startElement(this);

      // the ticks are drawn WRT the parent axis viewport. The axis line and ticks are drawn between (0,L) and (L,L) 
      // in that viewport, NOT (0,0) and (L,0)!
      double axisLenMI = parent.getLengthInMilliInches();

      // convert x-coordinates to thousandth-in WRT the parent axis viewport
      double[] xTickMils = convertTickLocationsToThousandthInches( xTickUser );

      // get other information we need to render tick marks
      int tickDir = getTickDirection();
      double tickGapMI = tickGap.toMilliInches();
      boolean isHoriz = parent.isHorizontal();
      boolean isAboveOrLeft = parent.isAboveLeft();

      // render tick marks as "lineup", "linedown", or "linethru" adorments.  Given the way in which the first two 
      // are drawn, we have to double their length to get things right.
      if(doTickMarks)
      {
         Marker cap = Marker.LINETHRU;
         if(tickDir > 0) cap = Marker.LINEUP;
         else if(tickDir < 0) cap = Marker.LINEDOWN;

         double capLen = tickLenMI;
         if(tickDir != 0) capLen *= 2;
         Point2D[] pts = new Point2D[xTickMils.length];
         for(int i=0; i<pts.length; i++) 
            pts[i] = new Point2D.Double(xTickMils[i], axisLenMI);
         psDoc.renderMultipleAdornments(pts, null, cap, capLen, null);
      }

      // render tick mark labels, if any
      if(doTickLabels)
      {
         // calculate offset to the aligning edge (top, bottom, left, or right) of the tick labels.  This offset 
         // accounts for the tick gap plus that portion of tick mark on same side of axis line as the label.
         double offset;
         if(!isAboveOrLeft)
         {
            offset = -tickGapMI;
            if(tickDir == 0) offset -= tickLenMI/2.0;
            else if(tickDir == -1) offset -= tickLenMI;
         }
         else
         {
            offset = tickGapMI;
            if(tickDir == 0) offset += tickLenMI/2.0;
            else if(tickDir == 1) offset += tickLenMI;
         }

         if(isHoriz)
         {
            // for a horizontal axis:  labels are horizontally centered at tick mark locations.  When labels are below 
            // axis, they are top-aligned; when above, bottom-aligned. Also, remember that the labels are offset WRT 
            // the axis line, which lies at y=L in the parent axis viewport.
            TextAlign vAlign = isAboveOrLeft ? TextAlign.TRAILING : TextAlign.LEADING;
            for(int i=0; i < xTickUser.length; i++)
            {
               String label = formatTickLabel(i, xTickUser[i]);
               psDoc.renderText(label, xTickMils[i], axisLenMI+offset, TextAlign.CENTERED, vAlign);
            }
         }
         else
         {
            // for a vertical axis:  labels are aligned perpendicular to the axis line.  So that we do not have to 
            // rotate each label individually, we move the origin to the bottom endpt of the axis line, then rotate 
            // user space 90deg CW. Because of this transformation, the positive x-axis is now the positive y-axis, 
            // while the positive y-axis is now the negative x-axis (hence we flip the sign of the label offset). Also, 
            // we have to vertically center each label to the left or right of its tick mark...
            Point2D ptOri = new Point2D.Double(0, axisLenMI);
            psDoc.translateAndRotate(ptOri, -90);
            TextAlign hAlign = isAboveOrLeft ? TextAlign.TRAILING : TextAlign.LEADING;
            for(int i=0; i < xTickUser.length; i++)
            {
               String label = formatTickLabel(i, xTickUser[i]);
               psDoc.renderText(label, -offset, xTickMils[i], hAlign, TextAlign.CENTERED);
            }
         }
      }

      // end the element -- restoring the graphics state
      psDoc.endElement();
   }


   // 
   // Object
   //

   /**
    * This override ensures that the rendering infrastructure for the <code>TickSetNode</code> clone is independent of
    * the element cloned.
    */
   @Override protected Object clone() throws CloneNotSupportedException
   {
      TickSetNode copy = (TickSetNode) super.clone();
      copy.tickPainter = null;
      copy.tickPolyline = null;
      copy.labelPainter = null;
      copy.labelLocs = null;
      copy.labels = null;
      copy.rBoundsSelf = null;
      return(copy);
   }
}
