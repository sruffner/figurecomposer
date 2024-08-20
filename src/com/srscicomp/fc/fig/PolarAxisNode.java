package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.srscicomp.common.g2dutil.StringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.LineXY;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.fig.TickSetNode.LabelFormat;

/**
 * <b>PolarAxisNode</b> is a required component of the 2D polar graph object, {@link PolarPlotNode}. There is one such 
 * node for the angular or "theta" axis and one for the radial axis. The theta and radial axis nodes define the extent,
 * layout, and appearance of the polar coordinate grid:
 * <ul>
 * <li>The axis range. For the theta axis, the range can span no more than 360 deg.</li>
 * <li>The grid divisions and associated labels. Custom grid divisions and custom labels are possible.</li>
 * <li>The stroke properties of the grid lines (arcs for the radial axis!) and text properties for the labels.</li>
 * <li>The direction in which values increase along the axis. Thus, you can have theta increase in either the clockwise
 * or counterclockwise direction, and you can even make the radial values increase toward the origin instead of away
 * from it.</li>
 * <li>A reference angle. For the theta axis, this is the location of "0 deg", measured CCW from a ray emanating from
 * the origin and pointing to the right (parallel to the X-axis in FC's global rendering coordinate system). For the
 * radial axis, this is the angle of the ray along which radial grid labels are centered, in degrees WRT the polar
 * plot's theta axis range.</li>
 * <li>The gap separating the axis grid labels from the grid itself. For the theta axis, this determines how far away
 * the grid division labels are from the outer circumference of the polar grid. For the radial axis, this applies only
 * when the polar grid spans less than 360 degrees and the radial axis labels are positioned along a ray at either end
 * of the theta axis range. For example, if the theta span is [0 90] and the radial axis reference angle is at 0 deg, 
 * then the radial labels are centered along a line offset from the 0-deg ray by the value of the "gap" property.</li>
 * </ul>
 * 
 * <p>Unlike the axes in a 2D or 3D Cartesian graph, the "axis line segment" is not rendered, nor are there any "tick
 * mark sets -- only the labels for annotating the anglular values of radial grid lines (theta axis) or the radial 
 * values of the circular arcs (radial axis). Only a linear scale is supported for the radial axis.</p>
 * 
 * <p><b>PolarAxisNode</b> does not establish its own "rectangluar viewport" like a typical graphic container node. It 
 * renders the grid lines/arcs and associated labels in the viewport of the parent graph. It contains no child nodes, is
 * not a focusable node, and does not have a well-defined focus shape.</p>
 * 
 * <p>Unlike its more general counterpart {@link AxisNode}, {@link PolarAxisNode} requires that the specified axis
 * range <i>[S, E}</i> always be valid. As a minimum, <i>S&lt;E</i>; there are further restrictions on the allowed 
 * values for the theta axis range, which cannot span more than 360 deg. For these reasons, the axis range endpoints 
 * are set as a single composite property to ensure the range is always valid -- see {@link #setRange}.</p>
 *
 * @author sruffner
 */
public class PolarAxisNode extends FGraphicNode implements Cloneable
{
   /**
    * Construct the angular ("theta") or radial axis of a polar graph ({@link PolarPlotNode}). The axis is visible
    * initially, and all text/draw styles are inherited from the parent graph. A theta axis has an initial full span of
    * 360 degrees with grid divisions of 30 deg, oriented with 0 deg pointing to the right, and increasing in the CCW 
    * direction. A radial axis has an initial span of [0..10] with a grid interval of 2, values increasing from the 
    * origin, and the axis lying along the ray at 80 deg CCW from a rightward-pointing ray. The axis-grid label gap 
    * defaults to 0.2 in.
    * @param isTheta True identifies an angular axis; false, a radial axis.
    */
   public PolarAxisNode(boolean isTheta)
   {
      super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR);
      this.isTheta = isTheta;
      
      range = new double[] {0, (isTheta ? 360 : 10)};
      gridDivs = isTheta ? new double[] {0, 0, 30} : new double[] {0, 0, 2};
      refAngle = isTheta ? 0 : 80;
      hide = false;
      reversed = false;
      gridLabelFormat = LabelFormat.INT;
      gap = new Measure(0.2, Measure.Unit.IN);
   }
   
   /** True for the theta axis, false for the radial axis of a polar graph. */
   private final boolean isTheta;
   
   /**
    * Does this polar graph axis represent the angular, or "theta" axis?
    * @return True for the theta axis, false for the radial axis.
    */
   public final boolean isTheta() { return(isTheta); }
   
   /**
    * Return a reference to the 2D polar graph object containing this polar axis node.
    * @return The parent polar graph, or null if there is no parent.
    */
   final PolarPlotNode getParentPolarGraph()
   {
      FGraphicNode p = getParent();
      if(p == null) return(null);
      assert(p instanceof PolarPlotNode);
      return((PolarPlotNode)p);
   }
   

   //
   // Support for child nodes -- admits no children
   //
   @Override public FGNodeType getNodeType() { return(FGNodeType.PAXIS); }
   /** The polar axis node admits no children. */
   @Override public boolean canInsert(FGNodeType nodeType) { return(false); }
   /** A polar axis node is an intrinsic component of its parent {@link PolarPlotNode}.*/
   @Override public boolean isComponentNode() { return(true); }

   
   //
   // Properties
   //
   
   /** 
    * The axis range ("user" units): [A, B], where A must be less than B. For a theta axis, implied units are degrees,
    * A and B must lie in the unit circle and the total span cannot exceed 360 degrees.
    */
   private final double[] range;

   /**
    * Get the minimum value of the coordinate range spanned by this polar graph axis.
    * @return The minimum of the axis range, in the native units associated with that axis ("user" units). 
    */
   public double getRangeMin() { return(range[0]); }
   /**
    * Get the maximum value of the coordinate range spanned by this polar graph axis.
    * @return The maximum of the axis range, in the native units associated with that axis ("user" units).
    */
   public double getRangeMax() { return(range[1]); }

   /**
    * Set the range for this polar graph axis. 
    * @param min The minimum of the range. 
    * @param max The maximum of the range.
    * @return True if successful, false if either value is ill-defined. If the specified range does not satisfy <i>min 
    * &lt; max</i>, it will be silently auto-corrected. For a theta axis, the implied units are always degrees and the 
    * range endpoints will be auto-corrected to ensure they both lie in [-360..360] and that (S-E) &le; 360 deg.
    */
   public boolean setRange(double min, double max)
   {
      if(!(Utilities.isWellDefined(min) && Utilities.isWellDefined(max))) return(false);
      
      if(min == max) max = min + (isTheta ? 90 : 1);
      else if(min > max) { double t = min; min = max; max = t; }         
      
      // for theta axis: restrict to unit circle, with max span of 360 deg
      if(isTheta)
      {
         double span = max - min;
         if(span > 360) 
         { 
            span = 360; 
            min = (min < -360) ? -360 : (min > 0 ? 0 : min);
         }
         else
         {
            if(min < -360) min = -360;
            if(min + span > 360) min = 360 - span;
         }
         max = min + span;
      }
      
      if(min == range[0] && max == range[1]) return(true);
      
      if(doMultiNodeEdit(FGNProperty.RANGE, new double[] {min, max, 0})) return(true);
      
      double[] old = new double[] {range[0], range[1], 0};
      range[0] = min;
      range[1] = max;
      if(areNotificationsEnabled())
      {
         onNodeModified(FGNProperty.RANGE);
         FGNRevEdit.post(this, FGNProperty.RANGE, new double[] {range[0], range[1], 0}, old, 
               "Change " + (isTheta ? "theta" : "radial") + "-axis range");
      }

      return(true);
   }

   /** Maximum number of grid divisions associated with this polar axis. */
   public final static int MAXDIVS = 20;
   
   /**
    * Grid division locations. The array must contain at least 3 elements; maximum length is ({@link #MAXDIVS} + 3).
    * The first 3 elements [S E M] define regularly spaced grid divisions of size M starting at S and not exceeding E.
    * If M&le;0, there are no such grid divisions. If M&gt;0 but S&ge;E, then S and E are replaced by the axis range 
    * endpoints; the advantage here is that the regular grid divisions will automatically adjust with any change in axis
    * range. Any additional array elements are interpreted as "custom" grid divisions. Any value is accepted, although 
    * any duplicate values and any value outside the current axis range is ignored. This format allows one to specify a 
    * regularly spaced grid and/or custom grid divisions. However, there can be no more than {@link #MAXDIVS} divisions
    * in total; the grid divisions are populated in increasing order until that limit is reached.
    */
   private double[] gridDivs;
   
   /**
    * Get the current grid divisions for this polar axis. 
    * <p>For a theta axis, the returned array specifies the angular locations of radial grid lines, in degrees. For a
    * radial axis, the returned array specifies the radii of concentric circular arcs, in arbitrary units.</p>
    * 
    * @return The polar axis grid divisions. <i>Contents</i>: An array of length [3..N+3], where N is {@link #MAXDIVS}.
    * The first 3 elements of the array, [S E M], define regular grid divisions of size M starting at S and not 
    * exceeding E. If M&le;0, there are no regular grid divisions. If M&gt;0 and S&ge;E, S and E ar replaced by the
    * current axis range endpoints; the advantage here is that the grid auto-adjusts with any change in axis range.
    * Any additional elements in the array are custom grid division locations. Up to {@link #MAXDIVS} such locations may 
    * be specified, although the total number of regular and custom grid divisions actually rendered cannot exceed that 
    * same limit. The rendered grid divisions are populated in increasing order until that limit is reached. Any 
    * duplicate grid divisions are ignored, as is any grid division outside the current axis range.
    */
   public double[] getGridDivisions() { return(Arrays.copyOf(gridDivs, gridDivs.length)); }
   
   /**
    * Set the current grid divisions for this polar axis. If any change is made, {@link #onNodeModified} is invoked.
    * 
    * <p>Note that only those grid divisions that fall within the current axis range [A..B] will actually be rendered.
    * Also, the rendered grid divisions are populated in increasing order starting with the first division &ge;A and 
    * ending with the last division &le;B -- unless the limit of {@link #MAXDIVS} is reached before then.</p>
    * 
    * @param divs Array of grid divisions. Must contain [3..N+3] elements, where N is {@link #MAXDIVS}. All elements 
    * must be well-defined floating-point values. First 3 elements [S E M] define regular grid divisions of size M 
    * starting at S and not exceeding E. If M&le;0, there are no such divisions. If M&gt;0 but S&ge;E, S and E are 
    * replaced by the current axis range endpoints -- so that the regular grid divisions automatically adjust when the
    * axis range changes. Any additional elements are treated as custom grid divisions; duplicate values are allowed but
    * will be ignored when rendering the actual grid divisions.
    * @return True if successful, false if argument does not satisfy stated requirements.
    */
   public boolean setGridDivisions(double[] divs)
   {
      if(divs == null || divs.length < 3 || divs.length > 3+MAXDIVS || !Utilities.isWellDefined(divs)) 
         return(false);

      if(!Arrays.equals(divs, gridDivs))
      {
         if(doMultiNodeEdit(FGNProperty.PDIVS, divs)) return(true);
         
         double[] old = gridDivs;
         gridDivs = Arrays.copyOf(divs, divs.length);
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.PDIVS);
            FGNRevEdit.post(this, FGNProperty.PDIVS, gridDivs, old, 
                  "Change grid divisions on " + (isTheta ? "theta" : "radial") + " axis");
         }
      }
      return(true);
   }
   
   /** 
    * Get the current grid divisions for this polar axis in string form, as a list of whitespace-separated tokens. There
    * are two alternative forms:
    * <ul>
    * <li>If <i>simple==true</i>, the list contains at least 3 floating-point tokens: "S E M ...". The first 3 tokens
    * define regularly spaced grid divisions of size M starting at S and not exceeding E. If M&le;0, there are no
    * regular grid divisions. If M&gt;0 but S&ge;E, S and E are ignored and replaced by the current axis range endpoints
    * -- so that the regularly spaced grid automatically adjusts with changes in axis range. Any remaining tokens in
    * the string represent individual custom grid divisions. </li>
    * <li>If <i>simple==false</i>, the regularly spaced grid is specified instead by the token "S/E/M", which will 
    * always be the first token in the list; any remaining tokens represent custom grid divisions. However, if M&le;0,
    * there is no regular grid, and the "S/E/M" token is omitted. Thus, if no custom nor regular grid divisions are 
    * currently defined, an empty string is returned.</li>
    * </ul>
    * In both forms, floating-point values are preserved with up to 3 decimal digits of precision.
    * 
    * @param simple Selects one of two alternative forms, as described.
    * @return The polar axis grid divisions in string form, as described.
    */
   public String getGridDivisionsAsString(boolean simple)
   {
      StringBuilder sb = new StringBuilder();
      if(simple)
      {
         for(int i=0; i<gridDivs.length-1; i++) sb.append(Utilities.toString(gridDivs[i], 7, 3)).append(" ");
         sb.append(Utilities.toString(gridDivs[gridDivs.length-1], 7, 3));
      }
      else
      {
         boolean hasRegGridToken = (gridDivs[2] > 0);
         if(hasRegGridToken)
            sb.append(Utilities.toString(gridDivs[0], 7, 3)).append("/").append(Utilities.toString(gridDivs[1], 7, 3)).
               append("/").append(Utilities.toString(gridDivs[2], 7, 3));
         if(gridDivs.length > 3)
         {
            if(hasRegGridToken) sb.append("  ");
            for(int i=3; i<gridDivs.length-1; i++) sb.append(Utilities.toString(gridDivs[i], 7, 3)).append(" ");
            sb.append(Utilities.toString(gridDivs[gridDivs.length-1], 7, 3));
         }
      }
      
      return(sb.toString());
   }
   
   /**
    * Set the grid divisions for this polar axis IAW the contents of the specified string. See also {@link 
    * #setGridDivisions(double[])}.
    * 
    * @param s The desired grid divisions in the string format specified by the <i>simple</i> flag. 
    * @param simple Specifies one of two alternative forms for the string argument:
    * <ul>
    * <li>If true, the string argument is a simple list of 3 or more whitespace-separated tokens, where the first
    * 3 tokens "S E M ..." define the regularly spaced grid divisions of size M starting at S and not exceeding E. See
    * {@link #getGridDivisionsAsString} for full details. Any additional tokens are custom divisions. In this form, if
    * the string is empty or has fewer than 3 tokens, it is ignored and the polar axis will have neither a regular grid 
    * nor any custom grid divisions.</li>
    * <li>If false and a regular grid is desired, the first token must take the form "S/E/M"; any remaining tokens are 
    * custom grid divisions. If the string is empty, then no regular nor custom grid divisions are defined.</li>
    * </ul>
    * In either form, all whitespace-separated tokens must be parsable as finite floating-point values. The maximum 
    * number of custom grid division tokens processed is {@link #MAXDIVS}. Any additional tokens are ignored.
    * @return True if successful, false if string cannot be parsed as described above.
    */
   public boolean setGridDivisionsFromString(String s, boolean simple)
   {
      // if null or empty string, then no grid divisions are defined
      s = (s==null) ? "" : s.trim();
      if(s.isEmpty()) return(setGridDivisions(new double[] {0, 0, 0}));
      
      double[] divs;
      String[] tokens = s.split("\\s");
      
      if(simple)
      {
         if(tokens.length < 3) 
            divs = new double[] {0, 0, 0};
         else
         {
            divs = new double[Math.min(3+MAXDIVS, tokens.length)];
            try { for(int i=0; i<divs.length; i++) divs[i] = Double.parseDouble(tokens[i]); }
            catch(NumberFormatException nfe) { return(false); }
         }
      }
      else
      {
         boolean hasRegGrid = tokens[0].contains("/");
         int nCustom = Math.min(MAXDIVS, tokens.length - (hasRegGrid ? 1 : 0));
         divs = new double[3 + nCustom];
         divs[0] = 0; divs[1] = 0; divs[2] = 0;
         if(hasRegGrid)
         {
            String[] tokens2 = tokens[0].split("/");
            if(tokens2.length != 3) return(false);
            try { for(int i=0; i<3; i++) divs[i] = Double.parseDouble(tokens2[i]); }
            catch(NumberFormatException nfe) { return(false); }
         }
         
         try 
         { 
            int ofs = -3 + (hasRegGrid ? 1 : 0);
            for(int i=3; i<divs.length; i++) 
               divs[i] = Double.parseDouble(tokens[i+ofs]);
         }
         catch(NumberFormatException nfe) { return(false); }
      }
      
      return(setGridDivisions(divs));
   }
   
   /** 
    * Array of custom grid labels. Normally this array is empty, and numeric labels are generated IAW the actual grid
    * divisions. If it is not empty, the grid lines/arcs are labeled instead with the tokens in this array, in order. If
    * there are more labels than grid lines/arcs, the extra labels are ignored. If there are too few, the labels are
    * reused until all grid lines/arcs are labeled.
    */
   private String[] customGridLabels = new String[0];
   
   /**
    * Get the array of custom grid labels assigned to this polar axis. For further details on how custom grid
    * labels are used, see {@link #getCustomGridLabelsAsString()}.
    * @return The custom tick labels, in order of appearance. If there are none, an empty array is returned.
    */
   public String[] getCustomGridLabels() {return( Arrays.copyOf(customGridLabels, customGridLabels.length)); }
   
   /**
    * Get the ordered list of custom grid labels assigned to this polar axis. When custom grid labels are defined, they
    * are used to label the rendered grid primitives (line segments emanating from the origin for a theta axis; or
    * concentric circular arcs for a radial axis) instead of the standard numeric labels. If there more custom labels 
    * than grid divisions, the extra labels are unused. If there are too few, the labels are reused until all grid
    * primitives are labeled.
    * 
    * @return String holding a comma-separated list of custom grid label tokens. If no such tokens have been defined for
    * this polar axis, an empty string is returned.
    */
   public String getCustomGridLabelsAsString() 
   {
      if(customGridLabels.length == 0) return("");
      StringBuilder sb = new StringBuilder();
      for(int i=0; i<customGridLabels.length-1; i++) sb.append(customGridLabels[i]).append(",");
      sb.append(customGridLabels[customGridLabels.length-1]);
      return(sb.toString());
   }
   
   /**
    * Set the ordered list of custom grid labels assigned to this polar axis. No label may contain a comma. For further
    * details on how custom grid labels are used, see {@link #getCustomGridLabelsAsString}.
    * @param labels The custom grid labels. A null or empty array will restore the standard auto-generated numeric 
    * labels. Otherwise, each element of the array is trimmed of whitespace and any commas are removed. Any null entry
    * is replaced by an empty string.
    */
   public void setCustomGridLabels(String[] labels)
   {
      String[] gridLabels = (labels == null) ? new String[0] : Arrays.copyOf(labels, labels.length);
      for(int i=0; i<gridLabels.length; i++)
      {
         if(gridLabels[i] == null) gridLabels[i] = "";
         else
         {
            gridLabels[i] = gridLabels[i].trim();
            gridLabels[i] = gridLabels[i].replace(',', '\0');
         }
      }
      
      if(!Arrays.equals(gridLabels,  customGridLabels))
      {
         if(doMultiNodeEdit(FGNProperty.CUSTOMTCKLBL, gridLabels)) return;
         
         Object old = customGridLabels;
         customGridLabels = gridLabels;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CUSTOMTCKLBL);
            FGNRevEdit.post(this, FGNProperty.CUSTOMTCKLBL, customGridLabels, old, 
                  "Change custom grid labels for " + (isTheta ? "theta" : "radial") + " axis");
         }
      }
   }
   
   /**
    * Assign custom grid labels to be used in place of the standard numeric labels for this polar axis. See {@link 
    * #getCustomGridLabelsAsString} for further details.
    * @param csTokens A comma-separated list of string tokens, each of which is trimmed of whitespace and saved as a
    * custom grid label. The label tokens are applied in the order they appear in the comma-separated list. Note that 
    * empty labels are allowed. For example, "A,,C" is a list of 3 label tokens, the second of which is an empty string;
    * ",,C,," is a list of 5 tokens, only one of which is non-empty! If the argument is null, empty, or contains
    * only whitespace, then NO custom grid labels are defined.
    */
   public void setCustomGridLabelsFromString(String csTokens)
   {
      if(csTokens == null) csTokens = "";
      else csTokens = csTokens.trim();
      
      String[] gridLabels = (csTokens.isEmpty()) ? new String[0] : csTokens.split(",", -1);
      
      if(!Arrays.equals(gridLabels, customGridLabels))
      {
         if(doMultiNodeEdit(FGNProperty.CUSTOMTCKLBL, gridLabels)) return;
         
         Object old = customGridLabels;
         customGridLabels = gridLabels;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CUSTOMTCKLBL);
            FGNRevEdit.post(this, FGNProperty.CUSTOMTCKLBL, customGridLabels, old, 
                  "Change custom grid labels for " + (isTheta ? "theta" : "radial") + " axis");
         }
      }
   }

   /** Polar axis reference angle, in whole degrees. */
   private int refAngle;
   
   /**
    * Get the reference angle for this polar axis. Its meaning is very different for the two polar axes:
    * <ul>
    * <li>For a theta axis, it specifies the angle at which the &theta;=0 ray points, so that a user can position
    * 0 degrees anywhere along the outer circumference of the polar grid. It is measured in degrees CCW from a 
    * horizontal ray emanating from the polar origin and pointing to the right. Thus, for example, if you want 0 deg in
    * the polar coordinate system to point straight up, the reference angle for the theta axis would be 90 deg.</li>
    * <li>For a radial axis, this specifies the angle of the ray along which the radial grid labels are drawn. <i>In 
    * this case, the angle is specified in degrees WRT angular coordinate system set by the theta axis. If the polar 
    * grid is not a full circle and this value is such that the ray lies outside the range for the theta axis, then the 
    * labels are rendered along a line parallel to a ray at one end of the theta range, but offset from it IAW {@link 
    * #getGridLabelGap()}.</li>
    * </ul>
    * @return The reference angle, as described.
    */
   public int getReferenceAngle() { return(refAngle); }
   
   /**
    * Set the reference angle for this polar axis. See {@link #getReferenceAngle()} for details. If any change is
    * made, {@link #onNodeModified} is invoked.
    * 
    * @param angle The new reference angle in whole degrees. Must lie in [-359..359].
    * @return True if successful, false if specified angle is invalid.
    */
   public boolean setReferenceAngle(int angle)
   {
      if(angle < -359 || angle > 359) return(false);
      
      if(angle != refAngle)
      {
         if(doMultiNodeEdit(FGNProperty.ANGLE, angle)) return(true);
         
         Integer old = refAngle;
         refAngle = angle;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.ANGLE);
            FGNRevEdit.post(this, FGNProperty.ANGLE, refAngle, old,
                  "Change reference angle for " + (isTheta ? "theta" : "radial") + " axis");
         }
      }
      return(true);
   }
   
   /** Flag determines whether or not this polar axis is rendered. */
   private boolean hide;

   /**
    * Get the visibility state of this polar axis.
    * @return True if hidden.
    */
   public boolean getHide() { return(hide); }

   /**
    * Set the visibility state for this polar axis. If a change is made, {@link #onNodeModified} is invoked.
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
            desc += isTheta ? "theta axis" : "radial axis";
            FGNRevEdit.post(this, FGNProperty.HIDE, hide, old, desc);
         }
      }
   }
   
   /** Flag determines whether or not the direction of increasing values is reversed for this polar axis. */
   private boolean reversed;

   /**
    * Is the direction of increasing values reversed for this polar axis?
    * <p>For a theta axis, angular values normally increase in the counter-clockwise direction; so clockwise is the 
    * reversed direction. For a radial axis, values normally increase from the origin outward; in the reverse direction, 
    * the values increase toward the origin (rare usage).</p>
    * @return True if polar axis direction is reversed.
    */
   public boolean getReversed() { return(reversed); }

   /**
    * Set the direction of increasing values for this polar axis. If a change is made, {@link #onNodeModified} is
    * invoked.
    * @param b False to select the normal direction (CCW for theta, increasing from origin for radial axis), true to
    * select the reversed direction (CW for theta, increasing toward origin for radial axis).
    */
   public void setReversed(boolean b)
   {
      if(reversed != b)
      {
         if(doMultiNodeEdit(FGNProperty.DIR, b)) return;
         
         Boolean old = reversed;
         reversed = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.DIR);
            String desc = "Set " + (reversed ? "reversed" : "normal") + " direction for the ";
            desc += isTheta ? "theta axis" : "radial axis";
            FGNRevEdit.post(this, FGNProperty.DIR, reversed, old, desc);
         }
      }
   }

   /** Numeric grid label format. */
   private LabelFormat gridLabelFormat;

   /**
    * Get the current numeric format for any grid labels rendered by this polar axis.
    * @return Current grid label format.
    */
   public LabelFormat getGridLabelFormat() { return(gridLabelFormat); }

   /**
    * Set the numeric format for the grid labels rendered by this polar axis. If a change is made, the method {@link 
    * #onNodeModified} is invoked.
    * 
    * @param fmt The new grid label numeric format. Null is rejected.
    * @return True if successful; false if argument is invalid.
    */
   public boolean setGridLabelFormat(LabelFormat fmt)
   {
      if(fmt == null) return(false);
      if(gridLabelFormat != fmt)
      {
         if(doMultiNodeEdit(FGNProperty.FMT, fmt)) return(true);
         
         LabelFormat old = gridLabelFormat;
         gridLabelFormat = fmt;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.FMT);
            FGNRevEdit.post(this, FGNProperty.FMT, gridLabelFormat, old, 
                  "Set polar axis grid label format to " + gridLabelFormat.toString());
         }
      }
      return(true);
   }


   /** Gap between a grid label and the endpoint of the corresponding grid line/arc. */
   private Measure gap;

   /**
    * Get the measured gap between a grid label and the endpoint of the corresponding grid line/arc.
    * 
    * <p>For a theta axis, this is the distance from the center of the label and the endpoint of the corresponding
    * grid line. For the radial axis, this parameter applies only if the polar grid is not a complete circle and the
    * orientation angle for the radial axis is such that it lies along one end or the other of the radial grid "arcs";
    * in this case, it is the distance separating the arc endpoint from the starting point of the label (the vertical
    * and horizontal alignment of the label will depend on where the labels are WRT the body of the polar grid.</p>
    * 
    * @return The grid label gap for this polar axis, specified as a linear measurement with associated units.
    */
   public Measure getGridLabelGap() { return(gap); }

   /**
    * Set the measured gap between a grid label and the endpoint of the corresponding grid line/arc. If a change is 
    * made, {@link #onNodeModified} is invoked.
    * 
    * @param m The new polar axis grid label gap. It is constrained to satisfy {@link AxisNode#SPACERCONSTRAINTS}. Null
    * is rejected.
    * @return True if change was accepted; false if rejected
    */
   public boolean setGridLabelGap(Measure m)
   {
      if(m == null) return(false);
      m = AxisNode.SPACERCONSTRAINTS.constrain(m);

      boolean changed = (gap != m) && !Measure.equal(gap, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.GAP, m)) return(true);
         
         Measure old = gap;
         gap = m;
         if(areNotificationsEnabled())
         {
            if(old.toMilliInches() != gap.toMilliInches()) onNodeModified(FGNProperty.GAP);
            FGNRevEdit.post(this, FGNProperty.GAP, gap, old, "Set " + (isTheta ? "theta": "radial") 
                  + " axis grid label gap to " + gap.toString());
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
            ok = setRange(rng[0], rng[1]);
            break;
         case PDIVS: ok = setGridDivisions((double[]) propValue); break;
         case CUSTOMTCKLBL: setCustomGridLabels((String[]) propValue); ok = true; break;
         case ANGLE: ok = setReferenceAngle((Integer) propValue); break;
         case HIDE: setHide((Boolean) propValue); ok = true; break;
         case DIR: setReversed((Boolean) propValue); ok = true; break;
         case FMT: ok = setGridLabelFormat((LabelFormat)propValue); break;
         case GAP: ok = setGridLabelGap((Measure) propValue);  break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }
   
   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value;
      switch(p)
      {
         case RANGE : value = new double[] {getRangeMin(), getRangeMax()}; break;
         case PDIVS: value = getGridDivisions(); break;
         case CUSTOMTCKLBL: value = getCustomGridLabels(); break;
         case ANGLE: value = getReferenceAngle(); break;
         case HIDE: value = getHide(); break;
         case DIR : value = getReversed(); break;
         case FMT: value = getGridLabelFormat(); break;
         case GAP: value = getGridLabelGap();  break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }
   
   /** 
    * This override handles some special cases:
    * <ul>
    * <li>Select properties of the polar axis will alter the layout of the polar coordinate grid and therefore must
    * trigger an update of the entire polar graph.</li>
    * <li>Otherwise, if the polar axis is hidden and some other property changes, there's no effect on the rendering
    * since the axis is already hidden.</li>
    * </ul>
    */
   @Override protected void onNodeModified(Object hint)
   {
      PolarPlotNode pg = getParentPolarGraph();
      if(pg == null) return;
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      if(hint == null || hint == FGNProperty.RANGE || hint == FGNProperty.DIR || hint == FGNProperty.ANGLE)
         pg.onNodeModified(FGNProperty.WIDTH);
      else
      {
         boolean hidden = hide || gridDivs.length == 0;
         if(hidden && hint != FGNProperty.HIDE && hint != FGNProperty.PDIVS)
            model.onChange(this, 0, false, null);
         else
            super.onNodeModified(hint);
      }
   }

   @Override protected void rescaleSelf(double scale, MultiRevEdit undoer)
   {
      Measure.Constraints c = AxisNode.SPACERCONSTRAINTS;
      double d = gap.getValue();
      if(d > 0)
      {
         Measure oldGap = gap;
         gap = c.constrain(new Measure(d*scale, gap.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.GAP, gap, oldGap);
      }
   }
   
   // 
   // Support for style sets 
   //
   
   @Override public boolean supportsStyleSet() { return(true); }
   
   /** 
    * The node-specific properties exported in a polar axis node's style set are the visibility state, the "reversed
    * direction" state, the orientation angle, the grid label gap and label format. Axis range and grid divisions are 
    * NOT included.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.HIDE, getHide());
      styleSet.putStyle(FGNProperty.DIR, getReversed());
      styleSet.putStyle(FGNProperty.ANGLE, getReferenceAngle());
      styleSet.putStyle(FGNProperty.GAP, getGridLabelGap());
      styleSet.putStyle(FGNProperty.FMT, getGridLabelFormat());
   }

   /**
    * Updates certain node-specific properties of this polar axis node when the applied style set originated from 
    * another polar axis node. No changes are mode if the applied style set source is a 2D Cartesian or a 3D axis node.
    */
   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      // the source node must be a polar axis node
      FGNodeType nt = applied.getSourceNodeType();
      if(!nt.equals(FGNodeType.PAXIS)) return(false);
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.HIDE, nt, Boolean.class);
      if(b != null && !b.equals(restore.getCheckedStyle(FGNProperty.HIDE, null, Boolean.class)))
      {
         hide = b;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.HIDE);
      
      b = (Boolean) applied.getCheckedStyle(FGNProperty.DIR, nt, Boolean.class);
      if(b != null && !b.equals(restore.getCheckedStyle(FGNProperty.DIR, null, Boolean.class)))
      {
         reversed = b;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.DIR);
      
      Integer angle = (Integer) applied.getCheckedStyle(FGNProperty.ANGLE, nt, Integer.class);
      if(angle != null && !angle.equals(restore.getCheckedStyle(FGNProperty.ANGLE, null, Integer.class)))
      {
         refAngle = angle;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.ANGLE);

      Measure m = (Measure) applied.getCheckedStyle(FGNProperty.GAP, nt, Measure.class);
      if(m != null) m = AxisNode.SPACERCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.GAP, null, Measure.class)))
      {
         gap = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.GAP);
      
      LabelFormat fmt = (LabelFormat) applied.getCheckedStyle(FGNProperty.FMT, nt, LabelFormat.class);
      if(fmt != null && !fmt.equals(restore.getCheckedStyle(FGNProperty.FMT, nt, LabelFormat.class)))
      {
         gridLabelFormat = fmt;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.FMT);

      return(changed);
   }

   
   //
   // Focusable/Renderable/PSTransformable support
   //
   /** The polar axis node renders directly into the parent polar graph, so the identity transform is returned. */
   @Override public AffineTransform getLocalToParentTransform() { return(new AffineTransform()); }
   /** The polar axis node renders directly into the parent polar graph, so this method returns the parent viewport. */
   @Override public FViewport2D getViewport() { return(getParentViewport()); }

   /** Returns null. A polar axis is a non-focusable component of its parent {@link PolarPlotNode}. */
   @Override public Shape getFocusShape(Graphics2D g2d) { return(null); }
   /**
    * Returns null. A polar axis is a non-focusable component of its parent {@link PolarPlotNode}; as such, it can not
    * be selected by a mousedown within the area to which it is rendered.
    */
   @Override protected FGraphicNode hitTest(Point2D p) { return(null); }


   @Override protected void releaseRenderResourcesForSelf() 
   {
      rBoundsSelf = null;
      if(gridPrimitives != null)
      {
         gridPrimitives.clear();
         gridPrimitives = null;
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
      if(forceRecalc || rBoundsSelf == null)
      {
         rBoundsSelf = new Rectangle2D.Double();
         if(isRendered()) 
         {
            updateRenderingResources(true);
            if(isStroked())
            {
               for(Shape s : gridPrimitives) Utilities.rectUnion(rBoundsSelf, s.getBounds2D(), rBoundsSelf);
               double sw = getStrokeWidth();
               rBoundsSelf.setFrame(rBoundsSelf.getX()-sw/2, rBoundsSelf.getY()-sw/2, 
                     rBoundsSelf.getWidth()+sw, rBoundsSelf.getHeight()+sw);
            }
            if(getFillColor().getAlpha() != 0)
            {
               labelPainter.updateFontRenderContext(g2d);
               labelPainter.invalidateBounds();
               Utilities.rectUnion(rBoundsSelf, labelPainter.getBounds2D(null), rBoundsSelf);
            }
         }
      }
      return((Rectangle2D) rBoundsSelf.clone());
   }
   
   /**
    * A polar axis node makes no marks on the canvas if: (i) it is hidden; (2) it has no grid divisions; or (3) it is
    * not stroked and its text/fill color is transparent.
    */
   @Override protected boolean isRendered()
   {
      return(!(hide || isEmptyGrid() || (getFillColor().getAlpha() == 0 && !isStroked())));
   }

   public boolean render(Graphics2D g2d, RenderTask task) 
   { 
      if(!(isRendered() && needsRendering(task)))
         return(true);

      // make sure the rendering infrastructure has been prepared.
      updateRenderingResources(false);

      // NOTE: We don't transform graphics context because polar axis grid primitives and labels are drawn in the 
      // parent graph's viewport coordinates.
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         // draw the grid primitives, unless they are not stroked
         if(isStroked()) 
         {
            g2dCopy.setColor( getStrokeColor() );
            g2dCopy.setStroke( getStroke(0) );
            for(Shape s : gridPrimitives) g2dCopy.draw(s);
         }

         // draw the associated grid labels, unless the text fill/color is transparent
         // NOTE: Does not handle font substitution when exporting to PDF!
         if(getFillColor().getAlpha() != 0)
            labelPainter.render(g2dCopy, null);
      }
      finally { if(g2dCopy != null) g2dCopy.dispose(); }

      return(task == null || task.updateProgress());
   }
   
   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException 
   { 
      // don't bother if axis is not rendered
      PolarPlotNode pp = getParentPolarGraph();
      if(pp == null || !isRendered()) return;

      // make sure the rendering infrastructure is prepared. We take advantage of the already prepared primitives, 
      // labels, and label locations to render in the PSDoc...
      updateRenderingResources(false);
      
      // start the element -- this will update the graphics state IAW this element's stroke/fill colors, etc
      psDoc.startElement( this );

      if(isTheta)
      {
         for(Shape gridPrimitive : gridPrimitives)
         {
            Line2D line2d = (Line2D) gridPrimitive;
            psDoc.renderLine(line2d.getP1(), line2d.getP2());
         }
      }
      else if(pp.isFullCircleGrid())
      {
         double[] radii = new double[gridPrimitives.size()];
         Point2D origin = null;
         for(int i=0; i<gridPrimitives.size(); i++)
         {
            Ellipse2D circle = (Ellipse2D) gridPrimitives.get(i);
            radii[i] = circle.getWidth() / 2.0;
            if(i==0) origin = new Point2D.Double(circle.getCenterX(), circle.getCenterY());
         }
         
         psDoc.renderConcentricCircles(origin, radii);
      }
      else
      {
         boolean isCW = pp.getThetaAxis().getReversed();
         double[] radii = new double[gridPrimitives.size()];
         Point2D origin = null;
         double thetaStart = 0;
         double thetaEnd = 0;
         for(int i=0; i<gridPrimitives.size(); i++)
         {
            Arc2D arc = (Arc2D) gridPrimitives.get(i);
            radii[i] = arc.getWidth() / 2.0;
            if(i==0)
            {
               // NOTE: We have to negate the start angle from Arc2D b/c it assumes a left-handed rather than a
               // right-handed coordinate system. Furthermore, PSDoc.renderConcentricArcs() requires that start < end,
               // so we have to swap the start/end angles in the CW case.
               origin = new Point2D.Double(arc.getCenterX(), arc.getCenterY());
               thetaStart = -arc.getAngleStart();
               if(!isCW) thetaEnd = thetaStart + Math.abs(arc.getAngleExtent());
               else
               {
                  thetaEnd = thetaStart;
                  thetaStart = thetaEnd - Math.abs(arc.getAngleExtent());
               }
            }
         }
         
         psDoc.renderConcentricArcs(origin, thetaStart, thetaEnd, radii);
      }
      
      for(int i=0; i<labels.size(); i++)
         psDoc.renderText(labels.get(i), labelLocs.get(i).getX(), labelLocs.get(i).getY(), 
               TextAlign.CENTERED, TextAlign.CENTERED);

      psDoc.endElement();
   }
   
   

   //
   // Internal rendering resources
   //

   /**
    * Cached rectangle bounding any marks made by this polar axis node (both grid and labels). Specified in the parent
    * graph's viewport, since the polar axis does not define its own viewport. An empty rectangle indicates that the 
    * element makes no marks when "rendered". If null, the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;

   /** Primitives representing the rendered grid for the polar axis, in the parent graph's viewport coordinates. */
   private List<Shape> gridPrimitives = null;
   
   /** Renders the grid labels associated with this polar axis node. */
   private StringPainter labelPainter = null;

   /** List serving as location provider for the string painter that renders the grid labels. */
   private List<Point2D> labelLocs = null;

   /** List serving as string provider for the string painter that renders the grid labels. */
   private List<String> labels = null;

   
   /**
    * Helper method updates internal infrastructure used to render the grid and associated labels for this polar axis 
    * node. It must be invoked when certain axis properties are changed; this happens via the {@link #onNodeModified}
    * method, which will force a recalculation of the node's render bounds whenever a property changes.
    * 
    * @param force If true, the rendering infrastructure is regenerated IAW current node properties; otherwise, it is
    * prepared only if it has not yet been allocated.
    */
   private void updateRenderingResources(boolean force)
   {
      // don't bother if axis is not rendered
      if(!isRendered()) return;
      
      if((!force) && gridPrimitives != null) return;
      
      if(gridPrimitives == null) gridPrimitives = new ArrayList<>();
      else gridPrimitives.clear();
      if(labelLocs == null) labelLocs = new ArrayList<>();
      else labelLocs.clear();
      if(labels == null) labels = new ArrayList<>();
      else labels.clear();
      if(labelPainter == null)
      {
         labelPainter = new StringPainter(this, labelLocs, labels);
         labelPainter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
      }
      
      // if there are no grid divisions, then the polar axis has no grid primitives nor associated labels
      if(isEmptyGrid()) return;
      
      // from user-defined axis range and grid divisions, calculate actual rendered grid division locations in 
      // increasing order.
      List<Double> divsList = new ArrayList<>();
      if(gridDivs[2] > 0)
      {
         // regular grid defined by first 3 elements [S E M], with M>0. If S>=E, then S and E are replaced by the 
         // current axis range endpoints, so that the grid adjusts as the range changes.
         double start = gridDivs[0], end = gridDivs[1];
         if(start >= end) { start = range[0]; end = range[1]; }

         // we don't want to wrap around the theta grid
         if(isTheta && (Utilities.restrictAngle(start) == Utilities.restrictAngle(end))) end -= 0.1;

         // NOTE: Hack to deal with the inexactness of FP values, eg: end = 0.15, div=0.150000000002.
         double div = start;
         while(div < range[0]) div += gridDivs[2];
         while(div <= range[1] && (div <= end + gridDivs[2]/10000.0) && divsList.size() < MAXDIVS)
         {
            divsList.add(div);
            div += gridDivs[2];
         }
      }

      for(int i=3; i<gridDivs.length; i++) if(range[0] <= gridDivs[i] && gridDivs[i] <= range[1] &&
            !divsList.contains(gridDivs[i]))
      {
         if(divsList.size() < MAXDIVS)
         {
            divsList.add(gridDivs[i]);
            if(divsList.size() == MAXDIVS) Collections.sort(divsList);
         }
         else 
         {
            int pos = 0;
            while(pos < MAXDIVS && divsList.get(pos) < gridDivs[i]) ++pos;
            if(pos < MAXDIVS) 
            {
               divsList.add(pos, gridDivs[i]);
               divsList.remove(MAXDIVS);
            }
         }
      }

      // for the radial axis, do NOT include a grid division at a value that maps to the polar origin!
      if((!isTheta) && !divsList.isEmpty())
      {
         if((!getReversed()) && (divsList.get(0) == range[0]))
            divsList.remove(0);
         else if(getReversed() && (divsList.get(divsList.size() - 1) == range[1]))
            divsList.remove(divsList.size()-1);
      }
      
      // we need the parent graph and its viewport. All primitives and labels are located WRT that viewport, since the
      // polar axis node does not define its own viewport.
      PolarPlotNode g = getParentPolarGraph();
      if(g == null) return;
      FViewport2D vp = g.getViewport();
      if(vp == null) return;

      // we'll need the origin of the polar coordinate system, both in polar coords and physical viewport coords.
      Point2D pOrigin = g.getOrigin();
      Point2D pOriginMils = vp.getPhysicalUserOrigin();
      if(!Utilities.isWellDefined(pOriginMils)) return;
      
      PolarAxisNode otherAxis = isTheta ? g.getRadialAxis() : g.getThetaAxis();
      Point2D p = new Point2D.Double();
      if(isTheta)
      {
         // for each division of theta axis, draw a radius from origin to the outermost circle of the polar grid. 
         // Labels are centered on each radius drawn, but pushed further out IAW the grid label gap parameter.
         double rOuterCircle = otherAxis.getReversed() ? otherAxis.getRangeMin() : otherAxis.getRangeMax();
         for(int i=0; i<divsList.size(); i++)
         {
            double div = divsList.get(i);
            p.setLocation(div, rOuterCircle);
            vp.userUnitsToThousandthInches(p);
            if(Utilities.isWellDefined(p))
            {
               gridPrimitives.add( new Line2D.Double(pOriginMils, p) );
               labels.add(formatGridLabel(i, div));
               
               // find the point offset from outer circle IAW gap parameter, but lying on the line that contains the
               // line segment representing the current theta value. This is the center point for the grid label.
               double rLbl = p.distance(pOriginMils) + gap.toMilliInches();
               double angle = Math.atan2(p.getY()-pOriginMils.getY(), p.getX()-pOriginMils.getX());
               double xLbl = rLbl*Math.cos(angle) + pOriginMils.getX();
               double yLbl = rLbl*Math.sin(angle) + pOriginMils.getY();
               labelLocs.add(new Point2D.Double(xLbl, yLbl));
            }
         }
      }
      else
      {
         // for each division of radial axis, draw a circular arc centered on the polar origin and spanning the range
         // of the theta axis (full circle if 360deg). But remember that 0deg may point in any direction, depending on
         // the orientation angle for the theta axis.
         boolean isCW = otherAxis.getReversed();
         
         // angles at which theta axis starts/ends, measured CCW from "east" in standard right-handed coord system,
         // and the theta axis range
         double thetaStart = otherAxis.getReferenceAngle() + (isCW ? -1 : 1) * otherAxis.getRangeMin();
         double thetaEnd = otherAxis.getReferenceAngle() + (isCW ? -1 : 1) * otherAxis.getRangeMax();
         double thetaSpan = otherAxis.getRangeMax() - otherAxis.getRangeMin();
         
         // reference angle for radial axis, which is measured in graph's angular coordinates. Determine if this lies 
         // inside the current theta axis range.
         double rAxisTheta = getReferenceAngle();
         if(rAxisTheta < otherAxis.getRangeMin() && rAxisTheta + 360 <= 360) rAxisTheta += 360;
         boolean rLabelsOutside = (thetaSpan < 360) && 
               !(rAxisTheta > otherAxis.getRangeMin() && rAxisTheta < otherAxis.getRangeMax());

         // if radial axis reference angle lies outside theta axis range, then the polar grid is an "incomplete pie",
         // and the radial labels are centered along a line parallel to the radial segent at one end or the other of
         // the theta axis range, offset IAW the gap parameter. We choose the theta axis range endpoint closest to the
         // radial axis reference angle.
         double thetaForRadLabels = 0;
         LineXY labelsLine = null;
         if(rLabelsOutside)
         {
            double diff0 = Math.abs(otherAxis.getRangeMin()-getReferenceAngle()); 
            double diff1 = Math.abs(otherAxis.getRangeMax()-rAxisTheta);
            boolean above;
            if(diff0 > diff1)
            {
               thetaForRadLabels = otherAxis.getRangeMax();
               double d = Utilities.restrictAngle(thetaEnd);
               if(!isCW) above = (d <= 90 || d > 270);
               else above = (d > 90 && d <= 270);
            }
            else
            {
               thetaForRadLabels = otherAxis.getRangeMin();
               double d = Utilities.restrictAngle(thetaStart);
               if(!isCW) above = (d > 90 && d <= 270);
               else above = (d <= 90 || d > 270);
            }
            p.setLocation(thetaForRadLabels, getReversed() ? getRangeMin() : getRangeMax());
            vp.userUnitsToThousandthInches(p);
            labelsLine = new LineXY(pOriginMils, p);
            labelsLine = labelsLine.getParallelLine(gap.toMilliInches(), above);
         }
         
         for(int i=0; i<divsList.size(); i++)
         {
            double div = divsList.get(i);
            p.setLocation(thetaStart, div);
            double r = vp.convertUserDistanceToThousandthInches(pOrigin, p);
            if(r <= 0) continue;
            
            // NOTE: Arc2D expects a left-handed coordinate system in which CW is postive, but the computed start angle
            // is measured in right-handed coordinate system (CCW positive). Also, the theta axis range is positive --
            // hence the negative sign on thetaStart, on thetaSpan when direction is CCW
            double left = pOriginMils.getX() - r;
            double bot = pOriginMils.getY() - r;
            if(thetaSpan >= 360)
               gridPrimitives.add(new Ellipse2D.Double(left, bot, r*2, r*2));
            else if(!isCW)
               gridPrimitives.add(new Arc2D.Double(left, bot, r*2, r*2, -thetaStart, -thetaSpan, Arc2D.OPEN));
            else
               gridPrimitives.add(new Arc2D.Double(left, bot, r*2, r*2, -thetaStart, thetaSpan, Arc2D.OPEN));
            
            if(!rLabelsOutside)
            {
               p.setLocation(rAxisTheta, div);
               vp.userUnitsToThousandthInches(p);
            }
            else
            {
               // here we find the radial arc endpoint closest to the line along which labels are laid out (see above),
               // then find the line perpendicular to the labels line and passing through this endpoint. The corres.
               // label location is the intersection of the two lines!
               p.setLocation(thetaForRadLabels, div);
               vp.userUnitsToThousandthInches(p);
               LineXY perpLine = labelsLine.getPerpendicularLine(p.getX(), p.getY());
               labelsLine.getIntersection(perpLine, p);
            }
            labels.add(formatGridLabel(i, div));
            labelLocs.add(new Point2D.Double(p.getX(), p.getY()));
         }
      }
   }

   /**
    * Format a grid label. Normally, the label is the string form of the numeric value associated with a grid primitive
    * (a line or arc), formatted IAW the polar axis node's grid label format (analogous to the label format for tick
    * marks on a Cartesian axis). However, there are two important exceptions:
    * <ol>
    * <li>If the label format is {@link LabelFormat#NONE}, then the grid label is an empty string.</li>
    * <li>If any custom grid labels are defined, these are used instead of numeric labels. If there are more custom
    * labels than there are grid primitives, then some labels are unused; if there are too few, the custom labels are 
    * reused to ensure all rendered grid primitives are labeled.</li>
    * </ol>
    * 
    * <p>Otherwise, the numeric string will be in decimal or scientific notation. Let T be the value associated with the
    * grid primitive. If 0.001 &ge; |T| &lt; 10000, then T is converted to string form in decimal notation with 0-3 
    * fractional digits preserved -- depending on the grid label format. Otherwise, T is converted to scientific
    * notation, with the desired number of fractional digits preserved in the significand.</p>
    * 
    * @param pos The ordinal position of the grid primitive in the list of primitives rendered. Applicable only when 
    * custom grid labels replace the standard numerical labels; otherwise ignored.
    * @param value The numerical value associated with the grid primitive.
    * @return The string form of the grid label, as described.
    */
   private String formatGridLabel(int pos, double value)
   {
      LabelFormat fmt = getGridLabelFormat();
      if(fmt == LabelFormat.NONE) return("");
      
      if(customGridLabels.length > 0) return(customGridLabels[pos % customGridLabels.length]);
      
      if(value == 0) return("0");
      
      double absV = Math.abs(value);
      int nDigits = 0;
      if(fmt == LabelFormat.F1) nDigits = 1; 
      else if(fmt == LabelFormat.F2) nDigits = 2;
      else if(fmt == LabelFormat.F3) nDigits = 3;

      // use scientific notation for values smaller than 0.001 and greater than or equal to 10000
      if(absV >= 1.0e4 || absV < 0.001)
      {
         int exp = (int) Math.floor(Math.log10(absV));
         value /= Math.pow(10, exp);
         
         double scale = Math.pow(10, nDigits);
         long truncValue = Math.round(value * Math.pow(10, nDigits));
         String strVal = Utilities.toString(((double)truncValue)/scale, 7, nDigits);
         return(strVal + "e" + exp);
      }
      
      double scale = Math.pow(10, nDigits);
      long truncValue = Math.round(value * Math.pow(10, nDigits));
      return(Utilities.toString(((double)truncValue)/scale, 7, nDigits));
   }

   /**
    * Helper method analyzes the regular and custom grid divisions currently defined to see if any fall within the 
    * current axis range. If not, then the grid is empty and will not be rendered for this polar axis.
    * @return True if grid is empty for this polar axis.
    */
   private boolean isEmptyGrid()
   {
      // first check if there any regular grid divisions that fall within the current axis range
      boolean gotDiv = false; 
      if(gridDivs[2] > 0)
      {
         // for regular grid [S E M], if S>=E, use current axis range instead!
         double s = (gridDivs[0] < gridDivs[1]) ? gridDivs[0] : range[0];
         double e = (gridDivs[0] < gridDivs[1]) ? gridDivs[1] : range[1];
         
         double d = s;
         for(int i=0; d <= range[1] && d <= e && i<MAXDIVS && !gotDiv; i++)
         {
            gotDiv = (d >= range[0]);
            d += gridDivs[2];
         }
      }
      
      // then check if there are any custom grid divisions that fall within the current axis range
      for(int i=3; i<gridDivs.length && !gotDiv; i++)
         gotDiv = (range[0] <= gridDivs[i]) && (gridDivs[i] <= range[1]);
         
      return(!gotDiv);
   }
   
   /**
    * This override ensures that the cloned polar axis node's internal rendering resources are completely independent of 
    * the resources allocated to this node.
    */
   @Override protected Object clone() throws CloneNotSupportedException
   {
      PolarAxisNode copy = (PolarAxisNode) super.clone();
      copy.rBoundsSelf = null;
      copy.gridPrimitives = null;
      copy.labelPainter = null;
      copy.labelLocs = null;
      copy.labels = null;
      return(copy);
   }

}
