package com.srscicomp.fc.fig;

import java.awt.Color;
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
 * <b>ColorBarNode</b> encapsulates a specialized "color bar" for a 2D or 3D graph. This component encapsulates the
 * parent graph container's color-mapping properties, which control how Z-coordinate data in 2D graphs -- and 
 * W-coordinate data (4th dimension) in 3D graphs -- are mapped to an RGB color:
 * <ul>
 *    <li><i>Colormap, C[]</i>. This lookup table maps integer indices <i>[0..256)</i> to RGB colors. Six different 
 *    colormaps are currently supported. See {@link ColorMap} enumeration.</li>
 *    <li><i>NaN color</i>. Regardless the colormap chosen, its first entry (index 0) is always replaced by this color.
 *    Every undefined datum in a heatmap is mapped to this color.</li>
 *    <li><i>Axis range, [R0..R1]</i>. This defines the data range that is mapped onto the colormap. Any datum 
 *    <i>Z &lt; R0</i> maps to <i>R0</i>, and any datum <i>Z &gt; R1</i> maps to <i>R1</i> (a saturation effect). Note
 *    that <i>R0</i> is the user-specified start of the axis range and <i>R1</i> is the user-specified end of the 
 *    range.</li>
 *    <li><i>Mapping mode</i>. Two modes are supported, <b>linear</b> -- <i>C[ floor( (Z-R0)*N/(R1-R0) ) ]</i> -- and
 *    <b>logarithmic</b> -- <i>C[ floor( log(Z-R0+1)*N/log(R1-R0+1) ) ]</i>.</li>
 * </ul>
 * 
 * <p>Only certain kinds of data presentation nodes use the graph container's color map feature: contour or bubble plots
 * in a 2D graph, and bubble or surface plots in a 3D graph.</p>
 * 
 * <p><b>ColorBarNode</b> is a required component of its parent graph container -- whether it be a GraphNode, 
 * PolarPlotNode, or Graph3DNode. It is rendered as a thin rectangle painted with the colormap gradient, along with an 
 * axis line segment and possibly tick marks that illustrate the data range mapped onto that gradient. The rendered 
 * color bar may appear above, below, left or right of the parent graph's bounding box. It can have any number of tick 
 * sets as children. These, of course, layout tick marks and associated numeric labels along the axis line.</p>
 * 
 * <p>As of v5.4.0 (schema version 24), <b>ColorBarNode</b> supports a "styled text" label, in which text color, font
 * style, underline state, superscript, and subscript can vary on a per-character basis. See {@link 
 * FGraphicNode#toStyledText()}.</p>
 * 
 * @author 	sruffner
 */
public class ColorBarNode extends FGNGraphAxis implements Cloneable
{
   /**
    * Construct a color bar spanning [0..100], with a power scale factor of 0 (no scaling) and an empty default title.
    * It is initially hidden, uses the {@link ColorMap.BuiltIn#tropic} colormap in linear mapping mode (with NaNs mapped
    * to pure white), and is located to the right of the parent graph's bounding box. The gap between one edge of the 
    * color map gradient strip and the adjacent edge of the graph data box is set to 0.6in initially, while the width of
    * the gradient strip itself is set to 0.2in. The <i>spacer</i> and <i>labelOffset</i> properties are initially set 
    * IAW user-defined preferred values. A single tick set is defined initially, spanning the same range with a tick
    * interval of 10.
    */
   public ColorBarNode()
   {
      super();
      setTitle("colormap");
      start = 0;
      end = 100;
      powerScale = 0;
      cmap = ColorMap.BuiltIn.tropic.cm;
      colorNaN = Color.WHITE;
      mode = CMapMode.LINEAR;
      gap = new Measure(0.6, Measure.Unit.IN);
      barSize = new Measure(0.2, Measure.Unit.IN);
      edge = Edge.RIGHT;
      spacer = FGNPreferences.getInstance().getPreferredAxisSpacer();
      labelOffset = FGNPreferences.getInstance().getPreferredAxisLabelOffset();
      hide = true;
      lineHeight = 0.8;
      
      insert(new TickSetNode(), 0);
   }

   
   @Override public boolean isPrimary() { return(false); }
   @Override boolean isTheta() { return(false); }
   @Override public FGNodeType getNodeType() { return(FGNodeType.CBAR); }

   /** The color bar admits any number of tick mark sets. */
   @Override public boolean canInsert(FGNodeType nodeType) { return(nodeType == FGNodeType.TICKS); }
   /** The color bar is an instrinsic component of any graph container. */
   @Override public boolean isComponentNode() { return(true); }

   
   //
   // Properties
   //
   
   /** Start of the color bar's axis range (implicit "user" units). */
   private double start;

   @Override public double getStart() { return(start); }

   /**
    * Set the start of the coordinate range <i>[R0..R1]</i> spanned by the colormap associated with this color bar. 
    * This property is controlled by the user. Any well-defined value is accepted, even if <i>R0 &ge; R1</i>. 
    * @param d New value for the start of coordinate range. Rejected if infinite, NaN.
    * @return True if successful; false if value rejected OR if auto axis-scaling is enabled on the parent graph.
    */
   public boolean setStart(double d)
   {
      if(isAutoranged() || !Utilities.isWellDefined(d)) return(false);
      if(start != d)
      {
         if(doMultiNodeEdit(FGNProperty.START, new Double(d))) return(true);
         
         Double old = new Double(start);
         start = d;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.START);
            FGNRevEdit.post(this, FGNProperty.START, new Double(start), old, "Change start of color bar axis range");
         }
      }
      return(true);
   }

   /** End of the color bar's axis range (implicit "user" units). */
   private double end;

   @Override public double getEnd() { return(end); }

   @Override protected void setRange_NoUpdate(double s, double e) 
   {
      start = s;
      end = e;
   }
   
   /**
    * Set the end of the coordinate range <i>[R0..R1]</i> spanned by the colormap associated with this color bar. 
    * This property is controlled by the user. Any well-defined value is accepted, even if <i>R0 &ge; R1</i>. 
    * @param d New value for the end of coordinate range. Rejected if infinite, NaN.
    * @return True if successful; false if value rejected OR if auto axis-scaling is enabled on the parent graph.
    */
   public boolean setEnd(double d)
   {
      if(isAutoranged() || !Utilities.isWellDefined(d)) return(false);
      if(end != d)
      {
         if(doMultiNodeEdit(FGNProperty.END, new Double(d))) return(true);
         
         Double old = new Double(end);
         end = d;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.END);
            FGNRevEdit.post(this, FGNProperty.END, new Double(end), old, "Change end of color bar axis range");
         }
      }
      return(true);
   }

   /** Color bar axis scale factor: Exponent N such that axis tick mark labels are scaled by 10^N */
   private int powerScale;

   @Override public int getPowerScale() { return(powerScale); }

   /**
    * Set the color bar's scale factor -- an integer exponent N such that numeric tick mark labels along the color bar
    * axis are scaled by 10^N. If a change was made, {@link #onNodeModified()} is invoked.
    * 
    * @param n New value for scale factor exponent. Rejected if outside allowed range. See {@link #getPowerScale()}.
    * @return True if successful; false if value rejected.
    */
   public boolean setPowerScale(int n)
   {
      if(n < MINPOWERSCALE || n > MAXPOWERSCALE) return(false);
      if(powerScale != n)
      {
         if(doMultiNodeEdit(FGNProperty.SCALE, new Integer(n))) return(true);
         
         Integer old = new Integer(powerScale);
         powerScale = n;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.SCALE);
            FGNRevEdit.post(this, FGNProperty.SCALE, new Double(powerScale), old, "Change color bar axis power scale");
         }
      }
      return(true);
   }

   /** The current indexed colormap. */
   private ColorMap cmap;

   /**
    * Get the current colormap assigned to this color bar.
    * @return Enumerated value defining which of the supported colormaps is currently associated with this color bar.
    */
   public ColorMap getColorMap() { return(cmap); }

   /**
    * Set the current colormap for this color bar. If a change is made, {@link #onNodeModified()} is invoked.
    * @param cmap Enumerated value selecting a supported colormap. A null value is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setColorMap(ColorMap cmap)
   {
      if(cmap == null) return(false);
      if(this.cmap != cmap)
      {
         if(doMultiNodeEdit(FGNProperty.CMAP, cmap)) return(true);
         
         ColorMap old = this.cmap;
         this.cmap = cmap;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CMAP);
            FGNRevEdit.post(this, FGNProperty.CMAP, this.cmap, old);
         }
      }
      return(true);
   }

   /** If this flag is true, the color bar's color map is reversed in direction. */
   private boolean reversed;

   /**
    * Is the color bar's color map reversed in direction? For example, the normal grayscale map runs from black to 
    * white, while the flipped grayscale map goes from white to black.
    * @return True if the color map is reversed.
    */
   public boolean isReversed() { return(reversed); }

   /**
    * Set the reversed state for this color bar's underlying color map. If a change is made, {@link #onNodeModified()} 
    * is invoked.
    * @param b True to reverse the normal color map direction.
    */
   public void setReversed(boolean b)
   {
      if(reversed != b)
      {
         if(doMultiNodeEdit(FGNProperty.DIR, new Boolean(b))) return;
         
         Boolean old = new Boolean(reversed);
         reversed = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.DIR);
            String desc = "Use color map in the " + (reversed ? "reversed" : "normal") + " direction";
            FGNRevEdit.post(this, FGNProperty.DIR, new Boolean(reversed), old, desc);
         }
      }
   }
   
   /** 
    * Get this color bar's color lookup table, defined by the color map type and its direction (normal or reversed).
    * Use the lookup table to map a color index in [0..255] to the corresponding RGB color, or to generate a linear
    * gradient fill representative of the color bar's color map and direction.
    * 
    * @return The color lookup table, based on the selected color map and direction.
    */
   public ColorLUT getColorLUT() { return new ColorLUT(cmap, reversed); }

   /** 
    * The color to which all ill-defined data are mapped; assigned to index 0 in the current colormap's LUT. It must
    * be opaque.
    */
   private Color colorNaN;
   
   /**
    * Get the color to which all ill-defined data are mapped by this color bar. This color is always assigned to index 
    * entry 0 in the current colormap's lookup table (LUT).
    * @return The color to which all ill-defined (NaN or infinite) data are mapped.
    */
   public Color getColorNaN() { return(colorNaN); }
   
   /**
    * Set the color to which all ill-defined data are mapped by this color bar. This color is always assigned to index 
    * entry 0 in the current colormap's lookup table (LUT).
    * @param The color to which all ill-defined (NaN or infinite) data are mapped. The alpha component is ignored --
    * the color must be fully opaque. A null value is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setColorNaN(Color c)
   {
      if(c == null) return(false);
      if(!colorNaN.equals(c))
      {
         if(doMultiNodeEdit(FGNProperty.CMAPNAN, c)) return(true);
         
         Color old = colorNaN;
         colorNaN = new Color(c.getRed(), c.getGreen(), c.getBlue());
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CMAPNAN);
            FGNRevEdit.post(this, FGNProperty.CMAPNAN, colorNaN, old);
         }
      }
      return(true);
   }
   
   /**
    * An enumeration of the datum-to-color index mapping modes.
    * @author sruffner
    */
   public enum CMapMode 
   {
      /** Linear mapping: <i>C[ floor( (Z-R0)*N/(R1-R0) ) ]</i>. */ LINEAR,
      /** Logarithmic mapping: i>C[ floor( log(Z-R0+1)*N/log(R1-R0+1) ) ]</i>. */ LOG;

      @Override public String toString() { return(super.toString().toLowerCase()); }
   }
   
   /** The current data-to-color mapping mode. */
   private CMapMode mode;
   
   /**
    * Get the current data-to-color mapping mode for this color bar.
    * 
    * <p><b>NOTE</b>: For the 3D graph node, this property is ignored. The mapping mode will be logarithmic if the
    * graph's Z axis is logarithmic; else it will be linear.</p>
    * 
    * @return The current color mapping mode.
    */
   public CMapMode getColorMapMode() { return(mode); }

   /**
    * Set the current data-to-color mapping mode for this color bar. If a change is made, {@link #onNodeModified()} is 
    * invoked.
    * 
    * <p><b>NOTE</b>: For the 3D graph node, this property is ignored. The mapping mode will be logarithmic if the
    * graph's Z axis is logarithmic; else it will be linear.</p>
    * 
    * @param mode The desired color mapping mode. A null value is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setColorMapMode(CMapMode mode)
   {
      if(mode == null) return(false);
      if(this.mode != mode)
      {
         if(doMultiNodeEdit(FGNProperty.CMODE, mode)) return(true);
         
         CMapMode old = this.mode;
         this.mode = mode;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CMODE);
            FGNRevEdit.post(this, FGNProperty.CMODE, this.mode, old);
         }
      }
      return(true);
   }

   /** The measured gap between closest edge of gradient rectangle and adjacent edge of parent graph's data box. */
   private Measure gap;

   /**
    * Constrains the measured properties <i>gap</i>, <i>size</i>, <i>spacer</i> and <i>labelOffset</i> to use physical 
    * units, to have a maximum of 4 significant and 3 fractional digits, and to lie in the range [0..2in].
    */
   public final static Measure.Constraints GAPCONSTRAINTS = new Measure.Constraints(0.0, 2000.0, 4, 3);

   /**
    * Get the size of the gap separating the color bar from the parent graph. 
    * @return The gap between color bar and graph, with associated units of measure (only physical units allowed).
    */
   public Measure getGap() { return(gap); }

   /**
    * Set the size of the gap separating the color bar from the parent graph. If a change is made, {@link 
    * #onNodeModified()} is invoked.
    * <p>A color bar's rendering consists of a rectangle filled with the colormap LUT gradient, and an axis line 
    * (possibly with tick marks) that illustrates the data range mapping onto that gradient. The gradient rectangle is 
    * drawn along one edge (left, right, top or bottom) of the parent graph's bounding box. The <i>gap</i> property sets 
    * the distance between this edge and the closer parallel edge of the gradient-filled rectangle. The color bar's axis
    * line is drawn along the opposite edge of the rectangle.</p>
    * @param m The new gap size. The measure is constrained to satisfy {@link #GAPCONSTRAINTS}. Null value is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setGap(Measure m)
   {
      if(m == null) return(false);
      m = GAPCONSTRAINTS.constrain(m);

      boolean changed = (gap != m) && !Measure.equal(gap, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.GAP, m)) return(true);
         
         Measure oldGap = gap;
         gap = m;
         if(areNotificationsEnabled())
         {
            if(oldGap.toMilliInches() != gap.toMilliInches()) onNodeModified(FGNProperty.GAP);
            FGNRevEdit.post(this, FGNProperty.GAP, gap, oldGap, 
                  "Set graph-gradient bar separation to " + gap.toString());
         }
      }
      return(true);
   }

   /** Measured width of the gradient bar (along dimension perpendicular to graph box's adjacent edge. */
   private Measure barSize;

   /**
    * Get width of this color bar's gradient bar, along the dimension perpendicular to graph box's adjacent edge.
    * @return The gradient bar size, with associated units of measure (only physical units allowed).
    */
   public Measure getBarSize() { return(barSize); }

   /**
    * Set the width of this color bar's gradient bar. If a change is made, {@link #onNodeModified()} is invoked.
    * <p>A color bar's rendering consists of a rectangular bar filled with the colormap LUT gradient, and an axis line 
    * (possibly with tick marks) that illustrates the data range mapping onto that gradient. The gradient bar is drawn 
    * along one edge (left, right, top or bottom) of the parent graph's bounding box. This property sets the width of 
    * the bar, ie, the length of the sides perpendicular to adjaxent edge of the graph bounding box.</p>
    * 
    * @param m The new gradient bar size. The measure is constrained to satisfy {@link #GAPCONSTRAINTS}. A null value is
    * rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setBarSize(Measure m)
   {
      if(m == null) return(false);
      m = GAPCONSTRAINTS.constrain(m);

      boolean changed = (barSize != m) && !Measure.equal(barSize, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.SIZE, m)) return(true);
         
         Measure old = barSize;
         barSize = m;
         if(areNotificationsEnabled())
         {
            if(old.toMilliInches() != barSize.toMilliInches()) onNodeModified(FGNProperty.SIZE);
            FGNRevEdit.post(this, FGNProperty.SIZE, barSize, old, "Set gradient bar width to " + barSize.toString());
         }
      }
      return(true);
   }

   /** 
    * An enumeration of the four possible locations for a color bar: left of, right of, above, or below the parent 
    * graph's bounding box. In each case, the colormap gradient bar is rendered along the adjacent edge of the graph 
    * box, and the axis line decoration is located on the other side of the bar.
    * @author sruffner
    */
   public enum Edge
   {
      /** Adjacent to left edge of parent graph's data window. */ LEFT,
      /** Adjacent to right edge of parent graph's data window. */ RIGHT,
      /** Adjacent to top edge of parent graph's data window. */ TOP,
      /** Adjacent to bottom edge of parent graph's data window. */ BOTTOM;
      
      @Override public String toString() { return(super.toString().toLowerCase()); }
   }
   
   /** The graph bounding box edge along which the color bar is rendered (if not hidden). */
   private Edge edge;
   
   /**
    * Get the graph bounding box edge along which this color bar should be rendered.
    * @return Enumerated value specifying the edge location of the color bar.
    */
   public Edge getEdge() { return(edge); }
   
   /**
    * Set the graph bounding box edge along which this color bar should be rendered. If a change is made, {@link 
    * #onNodeModified()} is invoked.
    * <p>A color bar's rendering consists of a rectangular bar filled with the colormap LUT gradient, and an axis line 
    * (possibly with tick marks) that illustrates the data range mapping onto that gradient. The gradient bar is drawn 
    * along one edge (left, right, top or bottom) of the parent graph's bounding box.</p>
    * @param e Enumerated value specifying the desired edge location for the color bar. A null value is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setEdge(Edge e)
   {
      if(e == null) return(false);
      if(edge != e)
      {
         if(doMultiNodeEdit(FGNProperty.EDGE, e)) return(true);
         
         Edge old = edge;
         edge = e;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.EDGE);
            FGNRevEdit.post(this, FGNProperty.EDGE, edge, old, "Set Z axis edge location to: " + edge.toString());
         }
      }
      return(true);
   }
   
   /**
    * The perpendicular distance between the axis line and the closest parallel edge of the gradient bar. Specified as a
    * linear measurement with associated units; relative units not allowed.
    */
   private Measure spacer;

   /**
    * Get the perpendicular distance between the axis line and the closest parallel edge of the gradient bar.
    * @return The bar-axis spacer, specified as a linear measurement with associated units.
    */
   public Measure getBarAxisSpacer() { return(spacer); }

   /**
    * Set the perpendicular distance between the axis line and the closest parallel edge of the gradient bar. If a 
    * change is made, {@link #onNodeModified()} is invoked.
    * @param m The new bar-axis spacer size. The measure is constrained to satisfy {@link #GAPCONSTRAINTS}. A null value
    * is rejected.
    * @return True if change was accepted; false if rejected.
    */
   public boolean setBarAxisSpacer(Measure m)
   {
      if(m == null) return(false);
      m = GAPCONSTRAINTS.constrain(m);

      boolean changed = (spacer != m) && !Measure.equal(spacer, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.SPACER, m)) return(true);
         
         Measure old = spacer;
         spacer = m;
         if(areNotificationsEnabled())
         {
            if(old.toMilliInches() != spacer.toMilliInches()) onNodeModified(FGNProperty.SPACER);
            FGNRevEdit.post(this, FGNProperty.SPACER, spacer, old, 
                  "Set gradient bar-axis line separation to " + spacer.toString());
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
    * closer). If a change is made, {@link #onNodeModified()} is invoked.
    * 
    * @param m The new axis label offset. The measure is constrained to satisfy {@link #GAPCONSTRAINTS}. A null value is
    * rejected.
    * @return True if change was accepted; false if rejected.
    */
   public boolean setLabelOffset(Measure m)
   {
      if(m == null) return(false);
      m = GAPCONSTRAINTS.constrain(m);

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
                  "Set Z axis label offset to " + labelOffset.toString());
         }
      }
      return(true);
   }

   /** If this flag is true, the color bar is not rendered. */
   private boolean hide;

   /**
    * Get the hide state for this color bar.
    * @return True if the color bar is currently hidden.
    */
   public boolean getHide() { return(hide); }

   /**
    * Set the hide state for this color bar. If a change is made, {@link #onNodeModified()} is invoked.
    * @param b True to hide color bar, false to show it.
    */
   public void setHide(boolean b)
   {
      if(hide != b)
      {
         if(doMultiNodeEdit(FGNProperty.HIDE, new Boolean(b))) return;
         
         Boolean old = new Boolean(hide);
         hide = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.HIDE);
            String desc = (hide ? "Hide " : "Show ") + "Z (color) axis";
            FGNRevEdit.post(this, FGNProperty.HIDE, new Boolean(hide), old, desc);
         }
      }
   }

   /**
    * Text line height, as a fraction of the element's font size. Applicable only when the auto-generated axis label is
    * multi-line. Defaults to 1.2; range-restricted to [0.8, 3.0].
    */
   private double lineHeight = 1.2;

   /**
    * Get the text line height for the axis label, in the event it is laid out in two or more text lines.
    * 
    * @return The text line height <i>as a fraction of the current font size</i>. Range-restricted to [0.8, 3.0].
    */
   public double getLineHeight() {  return(lineHeight); }

   /**
    * Set the text line height for the axis label, in the event it is laid out in two or more text lines.
    * 
    * @param lh The text line height <i>as a fraction of the current font size</i>. Range-restricted to [0.8, 3.0].
    * @return True if successful; false if argument is NaN. Note that any out-of-range value will be silently corrected.
    */
   public boolean setLineHeight(double lh)
   {
      if(!Utilities.isWellDefined(lh)) return(false);
      
      lh = Utilities.rangeRestrict(0.8, 3.0, lh);
      if(lineHeight != lh)
      {
         if(doMultiNodeEdit(FGNProperty.LINEHT, new Double(lh))) return(true);
         
         Double old = new Double(lineHeight);
         lineHeight = lh;
         
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.LINEHT);
            FGNRevEdit.post(this, FGNProperty.LINEHT, new Double(lineHeight), old);
        }
      }
      return(true);
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
         case END : ok = setEnd((Double) propValue); break;
         case START : ok = setStart((Double) propValue); break;
         case SCALE : ok = setPowerScale((Integer) propValue); break;
         case CMAP : ok = setColorMap((ColorMap) propValue); break;
         case DIR : setReversed((Boolean) propValue); ok = true; break;
         case CMAPNAN : ok = setColorNaN((Color) propValue); break;
         case CMODE : ok = setColorMapMode((CMapMode) propValue); break;
         case EDGE : ok = setEdge((Edge) propValue); break;
         case GAP:  ok = setGap((Measure) propValue); break;
         case SIZE: ok = setBarSize((Measure) propValue); break;
         case LABELOFFSET: ok = setLabelOffset((Measure) propValue); break;
         case SPACER: ok = setBarAxisSpacer((Measure) propValue); break;
         case HIDE: setHide((Boolean) propValue); ok = true; break;
         case LINEHT : ok = setLineHeight((Double) propValue); break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
         case END : value = new Double(getEnd()); break;
         case START : value = new Double(getStart()); break;
         case SCALE : value = new Integer(getPowerScale()); break;
         case CMAP : value = getColorMap(); break;
         case DIR : value = new Boolean(isReversed()); break;
         case CMAPNAN : value = getColorNaN(); break;
         case CMODE : value = getColorMapMode(); break;
         case EDGE : value = getEdge(); break;
         case GAP:  value = getGap(); break;
         case SIZE: value = getBarSize(); break;
         case LABELOFFSET: value = getLabelOffset(); break;
         case SPACER: value = getBarAxisSpacer(); break;
         case HIDE: value = new Boolean(getHide()); break;
         case LINEHT : value = new Double(getLineHeight()); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }
   

   @Override protected void rescaleSelf(double scale, MultiRevEdit undoer)
   {
      Measure.Constraints c = GAPCONSTRAINTS;
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

      d = gap.getValue();
      if(d > 0)
      {
         Measure oldGap = gap;
         gap = c.constrain(new Measure(d*scale, gap.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.GAP, gap, oldGap);
      }

      d = barSize.getValue();
      if(d > 0)
      {
         Measure oldSz = barSize;
         barSize = c.constrain(new Measure(d*scale, barSize.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.SIZE, barSize, oldSz);
      }
   }

   /**
    * This override handles a number of special cases and side-effects:
    * <ul>
    *    <li>Changes in the color mapping properties require a re-rendering of any affected plottables in the parent
    *    graph. Such changes also impact the rendering of the color bar itself, unless it's currently hidden.</li>
    *    <li>If the <em>hide</em> flag is toggled on, then we need to recalculate the bar's render bounds and re-render
    *    in that area; conversely, if it is toggled off, then we erase the bar's old (cached) render bounds.</li>
    *    <li>For all other attributes, no rendering update is needed if the color bar is not rendered; otherwise, the 
    *    super-class implementation suffices.</li>
    *    <li>If the color bar currently lacks a parent graph, the method does nothing. A color bar is only renderable 
    *    in the context supplied by a parent graph container.</li>
    * </ul>
    *
    * @see FGraphicNode#onNodeModified(Object)
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGNGraph g = getParentGraph();
      if(g == null) return;
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      if(hint == null)
      {
         // this call will force a complete re-rendering of graph and its descendants
         g.onNodeModified(FGNProperty.WIDTH);
      }
      else if(hint == FGNProperty.START || hint == FGNProperty.END || hint == FGNProperty.CMAP || 
               hint == FGNProperty.DIR || hint == FGNProperty.CMAPNAN || hint == FGNProperty.CMODE)
      {
         // if we switch mapping mode and auto-scaling is enabled, we trigger an auto-scale cycle. If this triggers
         // a refresh of the entire graph, we're done.
         if(hint == FGNProperty.CMODE && g.autoScaleAxes())
            return;
         
         // this call triggers an update of the rendering infrastructure for any plottables affected by the color map
         g.onColorMapChange();
         
         // if color bar is not hidden, then it must be re-rendered. Else, just notify model of the change.
         if(!getHide()) super.onNodeModified(hint);
         else model.onChange(this, 0, false, null);
      }
      else if(hint == FGNProperty.HIDE)
      {
         // we need to recalculate bounds regardless. However, if it was just hidden, add old bounds to dirty list; 
         // else, add new bounds.
         Graphics2D g2d = model.getViewerGraphics();
         try
         {
            Shape dirtyShape = getCachedGlobalShape();               // global bounds prior to change
            getRenderBounds(g2d, true, null);                        // update this node and its ancestors
            FGraphicNode.updateAncestorRenderBounds(g2d, this); 
            if(!getHide()) dirtyShape = getCachedGlobalShape();      // global bounds after the change

            List<Rectangle2D> dirtyAreas = new ArrayList<Rectangle2D>();
            dirtyAreas.add( dirtyShape.getBounds2D() );
            model.onChange(this, 0, true, dirtyAreas);
         }
         finally { if(g2d != null) g2d.dispose(); }
      }
      else
      {
         // changes are isolated to the color bar and its children. However, if the color bar is not drawn, there are 
         // no dirty areas to re-render!
         if(!getHide())
            super.onNodeModified(hint);
         else
            model.onChange(this, 0, false, null);
      }
   }

   /**
    * This override handles a number of special cases and side-effects:
    * <ul>
    *    <li>If the color bar is currently hidden, then adding a tick set does not affect the rendering of the bar. 
    *    Otherwise, the rendering infrastructure is updated via the super-class method.</li>
    *    <li>If the color bar currently lacks a parent graph, the method does nothing. A color bar is only renderable 
    *    in the context supplied by a parent graph.</li>
    *    <li>If we just added the first tick set -- ie, the major tick set -- and the auto axis-scaling feature is
    *    enabled, trigger an axis-scaling cycle to ensure that the tick set is adjusted accordingly.</li>
    * </ul>
    *
    * @see FGraphicNode#onSubordinateNodeInserted(FGraphicNode)
    */
   @Override protected void onSubordinateNodeInserted(FGraphicNode sub)
   {
      FGNGraph g = getParentGraph();
      if(g == null) return;
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      if(isRendered())
         super.onSubordinateNodeInserted(sub);
      else
         model.onChange(this, 1, false, null);
      
      if(getMajorTickSet() == sub)
         g.autoScaleAxes();
   }

   /**
    * This override handles a number of special cases and side-effects:
    * <ul>
    *    <li>If the color bar is currently hidden, then removing a tick set does not affect the rendering of the bar. 
    *    Otherwise, the rendering infrastructure is updated via the super-class method.</li>
    *    <li>If the color bar currently lacks a parent graph, the method does nothing. A color bar is only renderable 
    *    in the context supplied by a parent graph.</li>
    * </ul>
    *
    * @see FGraphicNode#onSubordinateNodeRemoved(FGraphicNode)
    */
   @Override protected void onSubordinateNodeRemoved(FGraphicNode sub)
   {
      FGNGraph g = getParentGraph();
      if(g == null) return;
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      if(isRendered())
         super.onSubordinateNodeRemoved(sub);
      else
         model.onChange(this, 1, false, null);
   }

   /**
    * This overrides the default behavior, preventing selection of the underlying tick sets. This is because a user
    * almost always expects the color bar to get the focus when s/he clicks on or near the bar's axis line segment.
    */
   @Override protected FGraphicNode hitTest(Point2D p) { return((super.hitTest(p) != null) ? this : null); }


   // 
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet()  { return(true); }

   /** 
    * The node-specific properties exported in a color bar's style set are the various properties related to the
    * colormap and the axis layout. Axis range and power scale factor are NOT included. 
    * 
    * <p>In addition, for each tick mark set child defined on the color bar, a component style set is added to the 
    * style set. Technically, tick sets are not components of a color bar, but they are part of its appearance, so we
    * include them in this fashion.</p>
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.CMAP, getColorMap());
      styleSet.putStyle(FGNProperty.DIR, new Boolean(isReversed()));
      styleSet.putStyle(FGNProperty.CMAPNAN, getColorNaN());
      styleSet.putStyle(FGNProperty.CMODE, getColorMapMode());
      styleSet.putStyle(FGNProperty.EDGE, getEdge());
      styleSet.putStyle(FGNProperty.GAP, getGap());
      styleSet.putStyle(FGNProperty.SIZE, getBarSize());
      styleSet.putStyle(FGNProperty.LABELOFFSET, getLabelOffset());
      styleSet.putStyle(FGNProperty.SPACER, getBarAxisSpacer());
      styleSet.putStyle(FGNProperty.LINEHT, new Double(getLineHeight()));

      for(int i=0; i<getChildCount(); i++)
      {
         FGNStyleSet tickSS = getChildAt(i).getCurrentStyleSet();
         styleSet.addComponentStyleSet(tickSS);
      }
   }

   /**
    * Accounts for the color bar-specific style properties when the applied style set originated from another color
    * bar node or a 2D graph axis node. In addition, it also styles each tick mark set child for which there's a 
    * corresponding tick mark styling component in the applied style set. This is not technically correct usage of the 
    * style set feature, since tick mark sets are not true components of a color bar node. 
    */
   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      ColorMap cm = (ColorMap) applied.getCheckedStyle(FGNProperty.CMAP, getNodeType(), ColorMap.class);
      if(cm != null && !cm.equals(restore.getStyle(FGNProperty.CMAP)))
      {
         cmap = cm;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.CMAP);
      
      Boolean rev = (Boolean) applied.getCheckedStyle(FGNProperty.DIR, getNodeType(), Boolean.class);
      if(rev != null && !rev.equals(restore.getStyle(FGNProperty.DIR)))
      {
         reversed = rev;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.DIR);
      
      Color nanC = (Color) applied.getCheckedStyle(FGNProperty.CMAPNAN, getNodeType(), Color.class);
      if(nanC != null && !nanC.equals(restore.getStyle(FGNProperty.CMAPNAN)))
      {
         colorNaN = nanC;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.CMAPNAN);

      CMapMode cmMode = (CMapMode) applied.getCheckedStyle(FGNProperty.CMODE, getNodeType(), CMapMode.class);
      if(cmMode != null && !cmMode.equals(restore.getStyle(FGNProperty.CMODE)))
      {
         mode = cmMode;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.CMODE);

      Edge e = (Edge) applied.getCheckedStyle(FGNProperty.EDGE, getNodeType(), Edge.class);
      if(e != null && !e.equals(restore.getStyle(FGNProperty.EDGE)))
      {
         edge = e;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.EDGE);

      Measure m = (Measure) applied.getCheckedStyle(FGNProperty.GAP, getNodeType(), Measure.class);
      if(m != null) m = GAPCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.GAP, null, Measure.class)))
      {
         gap = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.GAP);

      m = (Measure) applied.getCheckedStyle(FGNProperty.SIZE, getNodeType(), Measure.class);
      if(m != null) m = GAPCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.SIZE, null, Measure.class)))
      {
         barSize = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.SIZE);

      // the label offset, spacer and line height properties mean the same thing on AxisNode and ColorBarNode, so we 
      // should accept these properties from either node type. We should also accept the line height property from a 
      // TextBoxNode. All other style properties are ColorBarNode-specific.
      FGNodeType nt = applied.getSourceNodeType();
      boolean isTB = nt.equals(FGNodeType.TEXTBOX);
      if(!nt.equals(FGNodeType.AXIS)) nt = getNodeType();
      
      m = (Measure) applied.getCheckedStyle(FGNProperty.LABELOFFSET, nt, Measure.class);
      if(m != null) m = GAPCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure)restore.getCheckedStyle(FGNProperty.LABELOFFSET,null,Measure.class)))
      {
         labelOffset = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LABELOFFSET);

      m = (Measure) applied.getCheckedStyle(FGNProperty.SPACER, nt, Measure.class);
      if(m != null) m = GAPCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.SPACER, null, Measure.class)))
      {
         spacer = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.SPACER);
      
      Double lineHt = (Double) applied.getCheckedStyle(FGNProperty.LINEHT, isTB ? null : nt, Double.class);
      if(lineHt != null && !lineHt.equals(restore.getStyle(FGNProperty.LINEHT)))
      {
         lineHeight = Utilities.rangeRestrict(0.8, 3.0, lineHt.doubleValue());
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LINEHT); 

      // special case: If style set source is a 2D axis or color bar, apply its tick mark set styling to this color bar.
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

   /** The color bar's edge location property determines whether it is above or left of the graph bounding box. */
   @Override boolean isAboveLeft() { return((getParentGraph() != null) && (edge == Edge.TOP || edge == Edge.LEFT)); }

   /** 
    * When the color bar uses the logarithmic mapping mode, its axis line is drawn using log coordinates. 
    * <b>However, for the color bar component of a 3D graph, the color bar's own mapping mode is ignored; instead, 
    * it reflects the linear or logarithmic mapping of the 3D graph's Z axis.</p>
    * */
   @Override boolean isLogarithmic() 
   { 
      FGNGraph g = getParentGraph();
      if(g instanceof Graph3DNode) return(((Graph3DNode)g).isLogarithmicColorMap());
      return((g != null) && (mode == CMapMode.LOG)); 
   }
   
   /** The color bar only supports log base 10. */
   @Override int getLogBase() { return(10); }
   /** The color bar is horizontally oriented if it is located above or below the parent graph's bounding box. */
   @Override boolean isHorizontal() { return((getParentGraph() != null) && (edge == Edge.TOP || edge == Edge.BOTTOM)); }
   /** The color bar is vertically oriented if it is located left or right of the parent graph's bounding box. */
   @Override boolean isVertical() { return((getParentGraph() != null) && (edge == Edge.LEFT || edge == Edge.RIGHT)); }

   @Override double getLengthInMilliInches()
   {
      FGNGraph g = getParentGraph();
      if(g == null) return(0);
      Rectangle2D rBox = g.getBoundingBoxLocal();
      return((edge == Edge.TOP || edge == Edge.BOTTOM) ? rBox.getWidth() : rBox.getHeight());
   }

   /**
    * Get physical location of the origin of this color bar WRT the parent graph's viewport, with coordinates expressed 
    * in milli-inches. For a horizontally oriented color bar, this is the location of the left endpoint of the axis line
    * (NOT the color gradient bar!); for a vertical color axis, the bottom endpoint. 
    * 
    * <p>The origin's location depends upon the current edge location of the color bar (<i>edge</i> property), the 
    * perpendicular distance separating the near edge of the gradient bar from the relevant edge of the graph's data 
    * box (<i>gap</i> property), the gradient bar width (<i>size</i> property), and the perpendicular distance between
    * the far edge of the gradient bar and the axis line (<i>spacer</i> property). The x-coord of the origin of a 
    * horizontal color axis always lines up with the left edge of the data box, while the y-coord places the line above
    * or below the box. Analogously for a vertical color axis.</p>
    * 
    * @return The color bar origin WRT the parent graph's viewport, as described. If the color bar lacks a parent graph, 
    * this method will return <b>(NaN, NaN)</b>.
    */
   private Point2D getOriginInMilliInches()
   {
      Point2D origin = new Point2D.Double(Double.NaN, Double.NaN);

      FGNGraph g = getParentGraph();
      if(g == null) return(origin);
      Rectangle2D rBox = g.getBoundingBoxLocal();
      if(rBox.isEmpty()) return(origin);

      double distFromGraphEdge = gap.toMilliInches() + barSize.toMilliInches() + spacer.toMilliInches();
      boolean isHoriz = isHorizontal();
      boolean isAboveLeft = isAboveLeft();

      double x = 0; 
      double y = 0; 
      if(isHoriz) y = isAboveLeft ? (rBox.getHeight() + distFromGraphEdge) : -distFromGraphEdge;
      else x = isAboveLeft ? -distFromGraphEdge : (rBox.getWidth() + distFromGraphEdge);
      
      origin.setLocation(x, y);
      return(origin);
   }

   @Override double getAxisEndpointAdjustment()
   {
      // Note: Not applicable to the specialized polar graph and 3D graph nodes!
      FGNGraph g2 = getParentGraph();
      if(!(g2 instanceof GraphNode)) return(0);
      GraphNode g = (GraphNode) g2;
      if(g == null || g.isPolar() || (g.getLayout() == GraphNode.Layout.ALLQUAD)) return( 0 );

      AxisNode orthoAxis = (edge == Edge.LEFT || edge == Edge.RIGHT) ? g.getPrimaryAxis() : g.getSecondaryAxis();
      double adj = orthoAxis.getStrokeWidth() / 2.0;
      if(getStrokeEndcap() != StrokeCap.BUTT) adj -= getStrokeWidth() / 2.0;
      
      return( !orthoAxis.getHide() ? adj : 0 );
   }

   /**
    * For a color bar, the valid starting point of the coordinate range is always the lesser of the nominal, user-
    * specified range endpoints. In logarithmic mapping mode, the starting point is adjusted to ensure it is &gt; 0.
    */
   @Override double getValidatedStart() 
   { 
      double validStart = Math.min(start, end);
      if(isLogarithmic() && validStart <= 0) validStart = 0.01;
      return(validStart); 
   }
 
   /**
    * For a color bar, the valid ending point of the coordinate range is always the greater of the nominal, user-
    * specified range endpoints. If they happen to be equal, this method returns the nominal value, plus 1. In the
    * logarithmic mapping mode, the end point must be &gt; 0.
    */
   @Override double getValidatedEnd() 
   { 
      double validEnd = Math.max(start, end);
      if(isLogarithmic() && validEnd <=0) validEnd = 0.01;
      double validStart = getValidatedStart();
      return((validEnd <= validStart) ? (validStart + 1.0) : validEnd);
   }

   @Override TickSetNode getMajorTickSet() { return(getChildCount() == 0 ? null : (TickSetNode)getChildAt(0)); }


   //
   // Focusable/Renderable support
   //
   
   /**
    * The rendering coordinate system for the color bar is analogous to that of a {@link LineNode}. The origin is 
    * translated to the point, in the parent graph's coord system, where the first endpoint of the bar's axis line lies.
    * The coordinate system is rotated by 90 degrees if the color bar is vertical. Then, the origin is translated 
    * downward by the physical length of the color bar's axis -- so that its axis line lies at y=L in its own local 
    * rendering coordinates.
    */
   @Override
   public AffineTransform getLocalToParentTransform()
   {
      AffineTransform identity = new AffineTransform();

      FGNGraph g = getParentGraph();
      if(g == null) return(identity);

      Point2D origin = getOriginInMilliInches();
      double len = getLengthInMilliInches();
      if(len == 0 || !Utilities.isWellDefined(origin)) return(identity);

      AffineTransform at = AffineTransform.getTranslateInstance(origin.getX(), origin.getY());
      if(isVertical()) at.rotate( Math.PI/2.0 );
      at.translate(0, -len);
      return(at);
   }

   /**
    * The color bar must define a viewport WRT which its child tick sets are rendered along its axis line. Like a 
    * generic line segment node or 2D axis node, the color bar is one-dimensional, so we must define conventions for 
    * positioning a graphic object (a tick mark label, eg) off of the color bar's axis line segment. Those conventions
    * are encapsulated by the viewport returned by this method.
    * 
    * <p>The viewport is similar to that of a {@link LineNode}, except that "user" units may apply to the x-coordinate. 
    * Keep in mind that this is a description of the color bar's viewport IAW <em>FypML</em> conventions, in which the 
    * origin lies at the viewport's bottom-left corner and the y-axis increases upward.
    * <ul>
    *    <li>Let L be the length of the color bar's axis line segment in milli-inches. The viewport is square with a 
    *    side dimension of L. The axis line segment itself lies along the top edge of this viewport, ie, along the line
    *    y=100%, or y=L. The viewport origin is at the bottom-left corner, and the viewport's x-axis (y=0) lies along 
    *    its bottom edge, with the x-coordinate increasing to the right in the direction of the axis line's endpoint. 
    *    The y-axis increases upward.</li>
    *    <li>The viewport supports "user" units along its x-axis only, which is always parallel to the color bar's
    *    rendered axis line segment.</li>
    *    <li>If the color bar is logarithmic, the viewport handles the logarithmic transformation of user units.</li>
    * </ul>
    * </p>
    * 
    * <p>The method also returns null if this color bar currently lacks a parent graph, or if the viewport origin is 
    * ill-defined, or if its real length is zero.</p>
    */
   @Override public FViewport2D getViewport()
   {
      // if the color bar lacks a parent graph, then its viewport is not defined!
      FGNGraph g = getParentGraph();
      if(g == null) return(null);

      // get validated axis range endpoints. These can never be the same.
      double start = getValidatedStart();
      double end = getValidatedEnd();
      assert(start < end);
   
      // get color bar's axis length in milli-inches; this is size of the color bar's square viewport
      double len = getLengthInMilliInches();
      if(len == 0) return(null);

      // the viewport is always oriented with the axis line segment along the top edge, starting point of axis range at 
      // the bottom left and ending point of range at bottom right. Thus, the horizontal dimension supports user units, 
      // but the vertical dimension does not.
      return(new FViewport2D(len, len, start, end, 0.0, len, isLogarithmic(), false));
   }

   /** The color bar is not rendered if it lacks a parent graph or its <em>hide</em> property is set. */
   @Override protected boolean isRendered() { return( !(getHide() || (getParentGraph() == null)) ); }

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

         FGNGraph g = getParentGraph();
         if((g == null) || getHide()) 
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
            Rectangle2D rBar = new Rectangle2D.Double();
            double offset = isAboveLeft() ? (-spacer.toMilliInches()-barSize.toMilliInches()) : spacer.toMilliInches();
            rBar.setRect(-adj, axisLen+offset, axisLen+adj, barSize.toMilliInches());
            Utilities.rectUnion(rBoundsSelf, rBar, rBoundsSelf);
         }
      }
      return((Rectangle2D) rBoundsSelf.clone());
   }   

   /**
    * The method transforms a <em>copy</em> of the provided graphics context to set up the color bar's viewport. It then 
    * renders the gradient bar, the axis line, the axis label, and any child nodes -- in that order. Horizontal and 
    * vertical color bars are rendered similarly, except that a vertical color bar "starts" at its bottom endpoint and 
    * includes a rotation of -90deg WRT the parent container. Of course, method does nothing if the color bar is
    * currently hidden.
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

         // draw the color gradient bar representing the color map. It appears "above" the axis line in the axis's
         // viewport
         double offset = isAboveLeft() ? (-spacer.toMilliInches()-barSize.toMilliInches()) : spacer.toMilliInches();
         float yBar = (float) (axisLen + offset);
         
         g2dCopy.setPaint(getColorLUT().getGradientPaint((float) -hAdj, yBar, (float)(axisLen+hAdj), yBar));
         g2dCopy.fill(new Rectangle2D.Double(-hAdj, yBar, axisLen+hAdj, barSize.toMilliInches()));
         
         // draw the axis line itself at the "top" of the viewport, unless it is not stroked
         if(isStroked()) 
         {
            g2dCopy.setColor( getStrokeColor() );
            g2dCopy.setStroke( getStroke(0) );
            g2dCopy.draw( new Line2D.Double(-hAdj, axisLen, axisLen+hAdj, axisLen) );
         }

         // draw the axis label, if there is one. When exporting to PDF, must handle font substitution in case chosen 
         // font does not handle one or more characters in the label string
         labelPainter.render(g2dCopy, null);

         // now render any children
         if(!renderSubordinates(g2dCopy, task))
            return(false);
      }
      finally { if(g2dCopy != null) g2dCopy.dispose(); }

      return((task == null) ? true : task.updateProgress());
   }

   //
   // Internal rendering resources
   //

   /**
    * Cached rectangle bounding only the marks made by this color bar -- the color gradient swath, the axis line, and 
    * the automated axis label. An empty rectangle indicates that the element makes no marks when "rendered". If null,
    * the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;

   /** This painter renders the color bar's automated text label, if it has one. It supports multi-line labels. */
   private TextBoxPainter labelPainter = null;

   /**
    * Update the internal {@link TextBoxPainter} that renders the color bar's automated label WRT the bar's viewport. 
    * This painter supports both single-line and multi-line labels. By using the same implementation for both use cases,
    * we are ensured that the axis labels of adjacent color bars align nicely even if one label is single-line while the 
    * other is multi-line.
    * 
    * <p>The text box painter is configured so that the baseline of the last text line (for a color bar above or left of
    * the parent graph) or the top of the first text line (for a color bar below or right of the graph) lies at the 
    * appropriate offset from the axis line segment. The text box's background and border are not painted, and the text 
    * lines are centered. The text box width is TWICE the length of the color bar, to allow text lines to be be longer
    * than the rendered color bar. If any text line is longer than that, it will be split into two or more lines as 
    * needed. The painter will make no marks if there is no label to render or if the color bar itself is currently not 
    * rendered.</p>
    */
   private void updateLabelPainter()
   {
      // get parent graph, axis length and orientation WRT graph viewport
      FGNGraph g = getParentGraph();
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
         labelPainter.setText(fromStyledText(getAxisLabel(), getFontFamily(), getFontSizeInPoints(), getFillColor(), 
               getFontStyle(), true));
         labelPainter.setAlignment(TextAlign.CENTERED, isAboveOrLeft ? TextAlign.TRAILING : TextAlign.LEADING);
         labelPainter.setLineHeight(lineHeight);
      }
   }

   
   //
   // PSTransformable implementation
   //

   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      // if axis is hidden, there's nothing to render!
      if(getHide()) return;

      // get axis location, length, and orientation WRT enclosing graph viewport
      Point2D origin = getOriginInMilliInches();
      double axisLen = getLengthInMilliInches();
      boolean isHoriz = isHorizontal();
      boolean isAboveLeft = isAboveLeft();
      if(!(axisLen > 0 && Utilities.isWellDefined(origin)))
         return;

      // get adjustment, if any, in tick mark/label locations that coincide with axis endpoints.  In certain situations, 
      // the rendered axis line is padded at the endpoints for better alignment/join with orthogonal axis
      double hAdj = getAxisEndpointAdjustment();

      // start the element -- this will update the graphics state IAW this element's stroke/fill colors, etc
      psDoc.startElement(this);

      // establish a new non-clipping viewport for the axis and its children such that axis line lies between 
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

      // render the gradient bar into the rectangle defined by axis origin, the horizontal adj, spacer, and bar size
      double spacerMI = spacer.toMilliInches();
      double barSizeMI = barSize.toMilliInches();
      double yBL = isAboveLeft ? (-spacerMI-barSizeMI) : spacerMI;
      double yTR = isAboveLeft ? -spacerMI : spacerMI+barSizeMI;
      Point2D botLeft = new Point2D.Double(-hAdj, axisLen + yBL);
      Point2D topRight = new Point2D.Double(axisLen+hAdj, axisLen + yTR);
      
      psDoc.renderColormapGradient(botLeft, topRight, getColorLUT());
 
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
      if(title.length() > 0 && getFillColor().getAlpha() > 0)
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

      // render subordinate nodes, with respect to the axis's viewport.
      for(int i=0; i<getChildCount(); i++) getChildAt(i).toPostscript(psDoc);

      // end the element -- restoring the graphics state
      psDoc.endElement();
   }


   //
   // Object
   //

   /**
    * This override ensures that the cloned color bar node's internal rendering resources are completely 
    * independent of the resources allocated to this node.
    */
   @Override
   protected Object clone()
   {
      ColorBarNode copy = (ColorBarNode) super.clone();
      copy.labelPainter = null;
      copy.rBoundsSelf = null;
      
      return(copy);
   }
}
