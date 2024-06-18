package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.MultiShapePainter;
import com.srscicomp.common.g2dutil.PainterStyle;
import com.srscicomp.common.g2dutil.PolylinePainter;
import com.srscicomp.common.g2dutil.Projector;
import com.srscicomp.common.g2dutil.MultiShapePainter.PaintedShape;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.common.util.P3DComparator;
import com.srscicomp.common.util.Point3D;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;
import com.srscicomp.fc.fig.Graph3DNode.Side;

/**
 * <b>Scatter3DNode</b> is a <b><i>3D data presentation</i></b> element that renders a set of points in 3D space as a 
 * scatter, stem, line, or bar plot. It appears only in the 3D graph container, {@link Graph3DNode}.
 * 
 * <p>The 3D scatter plot currently supports either of two data set formats: a 3D point set {@link Fmt#XYZSET} or a 4D 
 * point set {@link Fmt#XYZWSET}. It does not render standard deviation data. It offers a total of six display modes, 
 * four of which are 3D versions of the display modes supported in the 2D scatter plot. Two additional 
 * modes render the data as a 3D bar plot -- one in which each bar is filled with a single color, and another in which 
 * the bars are filled with a linear color gradient IAW the graph's current color map and the Z-coordinate range spanned
 * by the bar.</p>
 * 
 * <p>The bubble plot display modes are the only modes suited to presenting a 4D data set. In these modes, the size 
 * and/or color of the marker symbol at (X, Y, Z) is scaled/mapped IAW the value of the W-coordinate (when the data set
 * is 3D, the size and/or color is set IAW the value of the Z-coordinate). In all other display modes, the W-coordinate
 * is simply ignored.</p>
 * 
 * <p>In the "scatter" and "bubble" display modes, the data points in the scatter plot may be connected to each other by
 * a "connect-the-dots" poly-line (a 3D "line" plot), or they may be connected by individual "stem lines" to a Z=Zo 
 * "base plane". If Zo is outside the parent graph's current Z-axis range, then the graph's XY backplane serves as
 * the base plane.</p>
 * 
 * <p>The user can separately style the scatter plot symbols versus the poly-line or stem lines. Lines are stroked IAW 
 * the 3D scatter plot node's own graphic styles, while a component {@link SymbolNode} specifies the marker type, size, 
 * and draw styling of the plot symbols. If you don't want lines to appear at all, simply set the stroke width to 0 or 
 * the stroke color to transparent.</p>
 * 
 * <p>Note that the symbol node's "title" and "fillColor" properties are ignored. The 3D scatter plot does NOT support 
 * drawing a character in each symbol, but it does support gradient fills. The node's background fill descriptor, {@link
 * BkgFill}, determines how closed symbols are filled. The main reason for this is to support a radial gradient fill for
 * the plot symbols. A radial fill with a circular or oval symbol type will create the "illusion" of a 3D marker. There 
 * are no text attributes associated with this presentation node.</p>
 * 
 * <p><b>Scatter3DNode</b> can render a simplified projection of the 3D scatter plot on any of the three backplanes of 
 * its parent graph. During a render cycle, {@link Graph3DNode} will invoke {@link #renderProjections} on each 3D
 * scatter plot child; these projections are rendered after the backplanes but before any data presentation nodes, 
 * including the 3D scatter plot itself, since the projections are intended to lie ON the backplanes. The projected 
 * points are represented by small filled circles limited in size to [0..10] points. The dots can be different for each 
 * backplane; see {@link #setProjectionDotSize}, {@link #setProjectionDotColor}. By default, the projections are not
 * rendered (dot size is set to 0 for all 3 backplanes).</p>
 * 
 * <p>In the two bar plot-like display modes, for each data point (Xn, Yn, Zn}, a 3D bar is drawn parallel to the Z-axis
 * and extending from the base plane Z=Zo to the plane Z=Zn, centered on (Xn,Yn) in the XY plane. In the {@link 
 * DisplayMode#BARPLOT} display mode, the three visible faces of the bar are filled with the node's current fill color. 
 * In the {@link DisplayMode#COLORBARPLOT} mode, the YZ and XZ faces are filled with a linear color gradient IAW the
 * graph's current color map and the Z-coordinate range [Zo..Zn] spanned by the bar, and the XY face is filled with the 
 * color that maps to the Z-coordinate for that face (either Zo or Zn, depending on the 3D graph's orientation). The 
 * bars are stroked IAW the 3D scatter plot's current stroking styles. The "barWidth" property sets the size of a bar's
 * square XY cross-section, defined as a percentage of the 3D X-axis extent. No symbols or backplane dot projections are
 * rendered in the bar plot display modes, nor are the {(Xn, Yn, Zn)} connected by a poly-line. If the source data set
 * is the 4D format {@link Fmt#XYZWSET}, the W-coordinate data are ignored.</p>
 * 
 * <p><b>NOTE: Limitations in rendering a color-mapped bar plot.</b> A color-mapped bar plot (with display mode {@link 
 * DisplayMode#COLORBARPLOT}) will not have a color gradient on the XZ/YZ faces of each bar when the color mapping is
 * logarithmic, as a logarithmic gradient is not supported. Instead, each bar is filled with a solid color that maps to
 * the Z-coordinate of the data point represented by that bar.</p>
 * 
 * @author sruffner
 */
public class Scatter3DNode extends FGNPlottableData implements Cloneable
{
   /**
    * Construct a 3D scatter plot node initially configured to display an empty 3D point set in the "scatter" display
    * mode, using a 0.2-in circle as the marker symbol with a radial white-to-gray gradient fill to create the illusion 
    * of a 3D marker. It is configured to draw stem lines to a base plane is at Z=0, but the stroke width is set to 0 
    * explicitly so that stems are not drawn (and the marker symbols won't be outlined either). Projections of the
    * scatter plot onto the parent graph's 3 backplanes are all turned off (dot size 0). The default bar size is set to
    * 1%, although this property only applies in the bar plot display modes. Since the data set is empty, it will make 
    * no marks in the parent 3D graph. All draw style properties (other than stroke width) are initially inherited; 
    * there are no font-related styles associated with this node. It has no title initially.
    */
   public Scatter3DNode()
   {
      super(HASSTROKEATTRS|HASSTRKPATNATTR|HASFILLCATTR|HASTITLEATTR|ALLOWATTRINTITLE);
      setTitle("");
      mode = DisplayMode.SCATTER;
      setMeasuredStrokeWidth(new Measure(0, Measure.Unit.IN));
      stemmed = true;
      zBase = 0;
      barSize = 1;
      bkgFill = BkgFill.createRadialGradientFill(75, 75, Color.WHITE, Color.DARK_GRAY);
      
      prjDotSizes = new int[] {0, 0, 0};
      prjDotColors = new Color[] {Color.BLACK, Color.BLACK, Color.BLACK};
      
      SymbolNode symbol = new SymbolNode();
      symbol.setSize(new Measure(0.2, Measure.Unit.IN));
      symbol.setMeasuredStrokeWidth(new Measure(0, Measure.Unit.IN));
      addComponentNode(symbol);
   }

   /**
    * When the parent graph's color map changes, the appearance of the 3D scatter plot is affected only if the display 
    * mode is one of the "color bubble" modes or the color-mapped bar plot display mode. In that scenario, this method 
    * will update the internal resources responsible for rendering the plot.
    */
   @Override public boolean onColorMapChange() 
   { 
      if(mode==DisplayMode.SCATTER || mode==DisplayMode.SIZEBUBBLE || mode==DisplayMode.BARPLOT) return(false);
      
      updateRenderingResources();
      return(true);  
   }
   
   @Override public FGNodeType getNodeType() { return(FGNodeType.SCATTER3D); }


   //
   // Properties
   //
   
   /** An enumeration of the different data display modes supported by {@link Scatter3DNode}. */
   public enum DisplayMode
   {
      /** 
       * Marker symbol drawn at each well-defined point (X,Y,Z). Size and fill color the same for all symbols. In 
       * this mode, the W-coordinate is ignored if the data set is 4D.
       */
      SCATTER("scatter"), 
      /** 
       * For 4D data {X,Y,Z,W}, size of marker symbol at (X,Y,Z) is proportional to W-coordinate value; for 3D data, it
       * is proportional to the Z-coordinate. 
       */
      SIZEBUBBLE("sizeBubble"),
      /** 
       * For 4D data {X,Y,Z,W}, fill color for marker symbol at (X,Y,Z) is indexed from 3D graph's color map based on 
       * W-coordinate value; for 3D data, it is mapped IAW the Z-coordinate.
       */
      COLORBUBBLE("colorBubble"),
      /** 
       * Both size and fill color of marker symbol reflect the Z-coordinate for 3D data {X,Y,Z} and the W-coordinate
       * for 4D data {X,Y,Z,W}.
       */
      COLORSIZEBUBBLE("colorSizeBubble"),
      /** 
       * A 3D bar plot with single-color fill. No symbols, poly-line or backplane dot projections rendered. The
       * W-coordinate is ignored if the data set is 4D.
       */
      BARPLOT("barPlot"),
      /** 
       * A 3D bar plot with color gradient fill.  No symbols, poly-line or backplane dot projections rendered. The
       * W-coordinate is ignored if the data set is 4D.
       */
      COLORBARPLOT("colorBarPlot");
      
      @Override public String toString() { return(niceName); }
      
      DisplayMode(String s) { niceName = s; }
      
      /** The display mode name as it should appear in GUI and in FypML markup. */
      private final String niceName;
   }

   /** The current data display mode. */
   private DisplayMode mode;

   /**
    * Get the current data display mode for this 3D scatter plot node.
    * 
    * @return The current data display mode.
    */
   public DisplayMode getMode() { return(mode); }

   /**
    * Set data display mode for this 3D scatter plot node. If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param mode The new display mode. Null is rejected.
    * @return False if argument was null; true otherwise.
    */
   public boolean setMode(DisplayMode mode)
   {
      if(mode == null) return(false);
      if(this.mode != mode)
      {
         if(doMultiNodeEdit(FGNProperty.MODE, mode)) return(true);
         
         DisplayMode oldMode = this.mode;
         this.mode = mode;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.MODE);
            FGNRevEdit.post(this, FGNProperty.MODE, mode, oldMode);
         }
      }
      return(true);
   }

   /** 
    * If true, stem lines are drawn to base plane in scatter or bubble display modes; else, data points are connected 
    * by a poly-line. Ignored in the bar plot display modes.
    */
   private boolean stemmed;
   
   /**
    * Does this 3D scatter plot render stem lines drawn from each data point to a common base plane, or are the data
    * points connected "dot-to-dot" by a single poly-line? <i>Note: This property has no effect in the bar plot display
    * modes.</i>
    * 
    * @return True if stem lines are drawn; false if points are connected by a poly-line.
    */
   public boolean getStemmed() { return(stemmed); }
   
   /** 
    * Configure this 3D scatter plot to draw stem lines from each data point to a common base plane, or connect the
    * data points with a single poly-line. <i>Note: This property is ignored in the bar plot display modes. Otherwise, 
    * if neither stem lines nor a poly-line is desired, simply set the element's stroke width to 0.</i>
    * 
    * @param ena True to draw stem lines; false to "connect the dots" with a single poly-line.
    * @return Always returns true.
    */
   public boolean setStemmed(boolean ena)
   {
      if(this.stemmed != ena)
      {
         if(doMultiNodeEdit(FGNProperty.STEMMED, ena)) return(true);
         
         Boolean old = this.stemmed;
         this.stemmed = ena;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.STEMMED);
            FGNRevEdit.post(this, FGNProperty.STEMMED, this.stemmed, old,
                  (this.stemmed ? "Show stems on " : "Connect data points in ") + "3D scatter plot");
         }
      }
      return(true);
   }
   
   /** The Z-coordinate value Zo that defines the base plane Z=Zo for stem or bar plots. */
   private double zBase;

   /**
    * Get the Z-coordinate value Zo that locates the "base plane" for this 3D scatter plot, applicable when the plot
    * is configured as a stem plot or 3D bar plot. Each stem line and 3D bar is drawn from each data point (Xn,Yn,Zn) to
    * the base plane Z=Zo. Of course, the stem lines and 3D bars are parallel to the graph's Z axis. <i>Note that if the 
    * base plane lies outside the 3D graph's data box (i.e., Zo is outside the graph's current Z-axis range), then the
    * graph's XY backplane serves as the base plane instead.</i>
    * 
    * @return The Z-coordinate value Zo that locates the base plane Z=Zo.
    */
   public double getZBase() { return(zBase); }
   
   /**
    * Set the Z-coordinate value that locates the base plane Z=Zo for this 3D scatter plot. For details, see {@link 
    * #getZBase()}.
    * @param base Z-coordinate value Zo such that Z=Zo is the base plane. Must be well-defined.
    * @return True if successful, false if value was rejected.
    */
   public boolean setZBase(double base)
   {
      if(!Utilities.isWellDefined(base)) return(false);
      
      if(zBase != base)
      {
         if(doMultiNodeEdit(FGNProperty.BASELINE, base)) return(true);
         
         Double old = zBase;
         zBase = base;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.BASELINE);
            String desc = "Set 3D scatter plot's base plane to Z=" + Utilities.toString(zBase, 6, 3);
            FGNRevEdit.post(this, FGNProperty.BASELINE, zBase, old, desc);
         }
      }
      return(true);
   }
   
   /** Minimum bar size (as a percentage of 3D X-axis extent). */
   public final static int MINBARSZ = 1;
   
   /** Maximum bar size (as a percentage of 3D X-axis extent). */
   public final static int MAXBARSZ = 20;
   
   /** 
    * Size of XY cross section of vertical bars in the two bar plot display modes, expressed as a percentage of the
    * X-axis extent.
    */
   private int barSize;
   
   /**
    * Get the size of the (square) XY cross-section of each vertical bar in either of the two bar plot display modes.
    * <i>The property has no effect on any of the scatter or bubble plot modes.</i>
    * @return The bar size expressed as a percentage of the 3D X-axis extent. Range-limited to [{@link #MINBARSZ} ..
    * {@link #MAXBARSZ}].
    */
   public int getBarSize() { return(barSize); }
   
   /**
    * Set the size of the (square) XY cross-section of each vertical bar in either of the two bar plot display modes.
    * See {@link #getBarSize()} for details.
    * @param sz The desired bar size as a percentage of the 3D X-axis extent.
    * @return True if successful, false if <i>sz</i> is outside the allowed range.
    */
   public boolean setBarSize(int sz)
   {
      if(sz < MINBARSZ || sz > MAXBARSZ) return(false);
      if(barSize != sz)
      {
         if(doMultiNodeEdit(FGNProperty.BARWIDTH, sz)) return(true);

         Integer old = barSize;
         barSize = sz;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.BARWIDTH);
            FGNRevEdit.post(this, FGNProperty.BARWIDTH, barSize, old);
         }
      }
      return(true);
   }
   
   /** The symbol's background fill: solid color (possibly transparent) or a gradient. */
   private BkgFill bkgFill;

   /**
    * Get the current background fill descriptor for this 3D scatter plot. If the node's marker symbol is a closed path,
    * all plot symbols rendered will be painted IAW this descriptor. <i>Note: This property is ignored in the bar plot 
    * display modes, since no symbols are rendered in those modes.</i>
    * @return The background fill descriptor.
    */
   public BkgFill getBackgroundFill() { return(bkgFill); }
   
   /**
    * Set the background fill descriptor for this 3D scatter plot. If a change is made, an "undo" operation is posted 
    * and {@link #onNodeModified} is invoked.
    * @param bf The new background fill descriptor. A null value is rejected.
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
            FGNRevEdit.post(this, FGNProperty.BKGC, bkgFill, old, "Change background fill for 3D scatter plot symbols");
         }
      }
      return(true);
   }
   
   /** 
    * Array holds size of the dots representing the projection of the scatter plot onto each of the parent graph's 
    * backplanes, in order: XY plane, XZ plane, YZ plane.
    */
   private final int[] prjDotSizes;
   
   /**
    * Get the size of the circular dots representing the projection of this 3D scatter plot onto the specified backplane
    * in the parent graph. <i>Note that a scatter plot projection is rendered only in the scatter or bubble plot display
    * modes, and only if the corresponding backplane is drawn.</i>
    * 
    * @param backplane The backplane; if null, {@link Side#XY} is assumed.
    * @return Dot size in typographical points. A value of 0 indicates that the projection is not drawn.
    */
   public int getProjectionDotSize(Side backplane)
   {
      return(prjDotSizes[backplane==Side.XZ ? 1 : (backplane==Side.YZ ? 2 : 0)]);
   }
   
   /**
    * Set the size of the circular dots representing the projection of this 3D scatter plot onto the specified backplane
    * in the parent graph. 
    * @param backplane The backplane.
    * @param sz Dot size in typographical points. Must lie in [0..10]; if 0, projection will not be rendered.
    * @return True if successful, false if no backplane specified or requested dot size is invalid.
    */
   public boolean setProjectionDotSize(Side backplane, int sz)
   {
      if(backplane==null || sz < 0 || sz > 10) return(false);
      
      int idx = (backplane==Side.XZ) ? 1 : (backplane==Side.YZ ? 2 : 0);
      if(prjDotSizes[idx] != sz)
      {
         int old = prjDotSizes[idx];
         prjDotSizes[idx] = sz;
         if(areNotificationsEnabled())
         {
            // index identifying backplane is included with old and new values of the dot size
            Object[] oldInfo = new Object[] {idx, old};
            Object[] newInfo = new Object[] {idx, sz};
            onNodeModified(FGNProperty.PROJSZ);
            FGNRevEdit.post(this, FGNProperty.PROJSZ, newInfo, oldInfo, 
                  "Change dot size for " + backplane + " projection");
         }
      }
      return(true);
   }
   
   /**
    * Get the dot sizes for this 3D scatter plot's three backplane projections as a list of three whitespace-separated
    * integer string tokens: <i>xySize xzSize yzSize</i>. If all three dot sizes are zero, an empty string is returned.
    * <p>This method is used exclusively while preparing the 3D scatter plot's representation in <i>FypML</i>; the 
    * integer token list serves as an attribute value for  the <i>FypML</i> element defining the node.</p>
    * 
    * @return The integer token list, as described.
    */
   String getProjectionDotSizesAsTokenList()
   {
      if(prjDotSizes[0]==0 && prjDotSizes[1]==0 && prjDotSizes[2]==0) return("");
      return(prjDotSizes[0] + " " + prjDotSizes[1] + " " + prjDotSizes[2]);
   }
   
   /**
    * Parse the whitespace-separated list of integer string tokens and from it generate the dot sizes for this 3D 
    * scatter plot's three backplane projections.
    * 
    * <p>This method is used exclusively while reconstructing a 3D scatter plot node from the <i>FypML</i> element that
    * defines it; the integer token list is the expected value for an attribute of that element. The token list takes
    * the form <i>xySize xzSize yzSize</i>, where each token should be an integer in [0..10], the dot size in points.
    * Any missing or invalid token is treated as a dot size of 0 (corresponding projection is not rendered); any extra
    * tokens are ignored.</p>
    * <p>This method should be used only when a figure is being reconstituted from its <i>FypML</i> definition; it does
    * not notify the model of a change to the 3D scatter plot node!</p>
    * 
    * @param tokenList The token list, as described. Null is treated as an empty string.
    */
   void setProjectionDotSizesFromTokenList(String tokenList)
   {
      prjDotSizes[0] = prjDotSizes[1] = prjDotSizes[2] = 0;
      if(tokenList != null)
      {
         String[] tokens = tokenList.trim().split("\\s");
         for(int i=0; i<3 && i<tokens.length; i++)
         {
            int sz = 0;
            try { sz = Integer.parseInt(tokens[i]); } catch(NumberFormatException ignored) {}
            prjDotSizes[i] = Utilities.rangeRestrict(0, 10, sz);
         }
      }
   }
   
   /** 
    * Array holds size of the dots representing the projection of the scatter plot onto each of the parent graph's 
    * backplanes, in order: XY plane, XZ plane, YZ plane.
    */
   private final Color[] prjDotColors;
   
   /**
    * Get the color of the filled dots representing the projection of this 3D scatter plot onto the specified backplane
    * in the parent graph. <i>Note that a scatter plot projection is only rendered only in the scatter or bubble plot
    * display modes, and only if the corresponding backplane is drawn.</i>
    * 
    * @param backplane The backplane; if null, {@link Side#XY} is assumed.
    * @return The dot color. If transparent, the projection is not drawn.
    */
   public Color getProjectionDotColor(Side backplane)
   {
      return(prjDotColors[backplane==Side.XZ ? 1 : (backplane==Side.YZ ? 2 : 0)]);
   }
   
   /**
    * Set the color of the filled dots representing the projection of this 3D scatter plot onto the specified backplane
    * in the parent graph. 
    * @param backplane The backplane.
    * @param c The desired dot color; if transparent, projection will not be rendered.
    * @return True if successful, false if either argument is null.
    */
   public boolean setProjectionDotColor(Side backplane, Color c)
   {
      if(backplane==null || c==null) return(false);
      
      int idx = (backplane==Side.XZ) ? 1 : (backplane==Side.YZ ? 2 : 0);
      if(!c.equals(prjDotColors[idx]))
      {
         Color old = prjDotColors[idx];
         prjDotColors[idx] = c;
         if(areNotificationsEnabled())
         {
            // index identifying backplane is included with old and new values of the dot color
            Object[] oldInfo = new Object[] {idx, old};
            Object[] newInfo = new Object[] {idx, c};
            onNodeModified(FGNProperty.PROJC);
            FGNRevEdit.post(this, FGNProperty.PROJC, newInfo, oldInfo, 
                  "Change dot color for " + backplane + " projection");
         }
      }
      return(true);
   }
   
   /**
    * Get the dot colors for this 3D scatter plot's three backplane projections as a list of three whitespace-separated
    * color tokens: <i>xyColor xzColor yzColor</i>. Each token is prepared by {@link BkgFill#colorToHexString(Color)}.
    * However, if all three dot colors are opaque black (the default color), an empty string is returned.
    * <p>This method is used exclusively while preparing the 3D scatter plot's representation in <i>FypML</i>; the 
    * token list serves as an attribute value for  the <i>FypML</i> element defining the node.</p>
    * 
    * @return The dot color token list, as described.
    */
   String getProjectionDotColorsAsTokenList()
   {
      if(Color.BLACK.equals(prjDotColors[0]) && Color.BLACK.equals(prjDotColors[1]) && 
            Color.BLACK.equals(prjDotColors[2])) 
         return("");
      return(BkgFill.colorToHexString(prjDotColors[0]) + " " +
            BkgFill.colorToHexString(prjDotColors[1]) + " " +
            BkgFill.colorToHexString(prjDotColors[2]));
   }
   
   /**
    * Parse the whitespace-separated list of color tokens and from it generate the dot colors for this 3D scatter plot's
    * three backplane projections.
    * 
    * <p>This method is used exclusively while reconstructing a 3D scatter plot node from the <i>FypML</i> element that
    * defines it. The color token list is the expected value for an attribute of that element; it takes the form 
    * <i>xyColor xzColor yzColor</i>, where each token should be parsable as an opaque or translucent color by {@link 
    * BkgFill#colorFromHexString(String)}. Any missing or invalid token is treated as a dot color of opaque black; any 
    * extra tokens are ignored.</p>
    * <p>This method should be used only when a figure is being reconstituted from its <i>FypML</i> definition; it does
    * not notify the model of a change to the 3D scatter plot node!</p>
    * 
    * @param tokenList The token list, as described. Null is treated as an empty string.
    */
   void setProjectionDotColorsFromTokenList(String tokenList)
   {
      Arrays.fill(prjDotColors, Color.BLACK);
      if(tokenList != null)
      {
         String[] tokens = tokenList.trim().split("\\s");
         for(int i=0; i<3 && i<tokens.length; i++) prjDotColors[i] = BkgFill.colorFromHexString(tokens[i]);
      }
   }
   

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok;
      Object[] arObj = null;
      switch(p)
      {
         case MODE: ok = setMode((DisplayMode)propValue); break;
         case STEMMED: ok = setStemmed((Boolean)propValue); break;
         case BASELINE: ok = setZBase((Double) propValue); break;
         case BARWIDTH: ok = setBarSize((Integer) propValue); break;
         case BKGC: ok = setBackgroundFill((BkgFill) propValue); break;
         case PROJSZ:
            // special case: The "property value" here is an array of two Integers. The first is an Integer holding the
            // index identifying the projection backplane; the second holds the dot size for that projection.
            ok = propValue != null && propValue.getClass().equals(Object[].class);
            if(ok)
            {
               arObj = (Object[]) propValue;
               ok = arObj.length == 2;
               if(ok) ok = arObj[0] != null && arObj[0].getClass().equals(Integer.class);
               if(ok) ok = arObj[1] != null && arObj[1].getClass().equals(Integer.class);
            }
            if(ok)
            {
               int idx = (Integer) arObj[0];
               int sz = (Integer) arObj[1];
               ok = idx >= 0 && idx < prjDotSizes.length && sz >= 0 && sz <= 10;
               if(ok) ok = setProjectionDotSize(idx==0 ? Side.XY : (idx==1 ? Side.XZ : Side.YZ), sz);
            }
            break;
         case PROJC:
            // special case: The "property value" here is an array of two objects. The first is an Integer holding the
            // index identifying the projection backplane; the second holds the dot color for that projection.
            ok = propValue != null && propValue.getClass().equals(Object[].class);
            if(ok)
            {
               arObj = (Object[]) propValue;
               ok = arObj.length == 2;
               if(ok) ok = arObj[0] != null && arObj[0].getClass().equals(Integer.class);
               if(ok) ok = arObj[1] != null && arObj[1].getClass().equals(Color.class);
            }
            if(ok)
            {
               int idx = (Integer) arObj[0];
               ok = idx >= 0 && idx < prjDotSizes.length;
               if(ok) ok = setProjectionDotColor(idx==0 ? Side.XY : (idx==1 ? Side.XZ : Side.YZ), (Color) arObj[1]);
            }
            break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value;
      switch(p)
      {
         case MODE: value = getMode(); break;
         case STEMMED: value = getStemmed(); break;
         case BASELINE: value = getZBase(); break;
         case BARWIDTH: value = getBarSize(); break;
         case BKGC: value = getBackgroundFill(); break;
         case PROJSZ:
         case PROJC:
            value = null;   // multi-object edit is not supported for these "indexed" properties
            break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }


   //
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   /**
    * The 3D scatter plot's display mode, bar size, and symbol background fill are included in the style set, as are the
    * boolean styles determining whether or not stems are drawn and whether or not the node is shown in the graph 
    * legend. The dot sizes and colors for the backplane projections are NOT part of the style set.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.MODE, getMode());
      styleSet.putStyle(FGNProperty.STEMMED, getStemmed());
      styleSet.putStyle(FGNProperty.LEGEND, getShowInLegend());
      styleSet.putStyle(FGNProperty.BKGC, getBackgroundFill());
      styleSet.putStyle(FGNProperty.BARWIDTH, getBarSize());
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      DisplayMode dm = (DisplayMode) applied.getCheckedStyle(FGNProperty.MODE, getNodeType(), DisplayMode.class);
      if(dm != null && !dm.equals(restore.getStyle(FGNProperty.MODE)))
      {
         mode = dm;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.MODE);
            
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.STEMMED, null, Boolean.class);
      if(b != null && !b.equals(restore.getStyle(FGNProperty.STEMMED)))
      {
         stemmed = b;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.STEMMED);
            
      b = (Boolean) applied.getCheckedStyle(FGNProperty.LEGEND, null, Boolean.class);
      if(b != null && !b.equals(restore.getStyle(FGNProperty.LEGEND)))
      {
         setShowInLegendNoNotify(b);
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LEGEND);
            
      BkgFill bf = (BkgFill) applied.getCheckedStyle(FGNProperty.BKGC, null, BkgFill.class);
      if(bf != null && !bf.equals(restore.getStyle(FGNProperty.BKGC)))
      {
         bkgFill = bf;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.BKGC);
      
      Integer barSz = (Integer) applied.getCheckedStyle(FGNProperty.BARWIDTH, null, Integer.class);
      if(barSz != null && !barSz.equals(restore.getStyle(FGNProperty.BARWIDTH)))
      {
         barSize = Utilities.rangeRestrict(MINBARSZ, MAXBARSZ, barSz);
         changed = true;
      }
      return(changed);
   }


   // 
   // FGNPlottableData, FGNPlottable
   //

   /**
    * Initializes the 3D scatter plot node's data set to a 3D set of 25 points randomly scattered over the available 
    * X-Y-Z range of the graph.
    */
   @Override protected void initializeDefaultData(double[] axisRng, boolean isPolar)
   {
      float x0 = (float) axisRng[0];
      float x1 = (float) axisRng[1];
      float y0 = (float) axisRng[2];
      float y1 = (float) axisRng[3];
      float z0 = (float) axisRng[4];
      float z1 = (float) axisRng[5];
      
      float[] fData = new float[25*3];
      for(int i=0; i<25; i++)
      {
         fData[i*3] = x0 + (x1-x0) * ((float) Math.random());
         fData[i*3+1] = y0 + (y1-y0) * ((float) Math.random());
         fData[i*3+2] = z0 + (z1-z0) * ((float) Math.random());
      }
      
      DataSet ds = DataSet.createDataSet(getDataSet().getID(), Fmt.XYZSET, null, 25, 3, fData);
      if(ds != null) setDataSet(ds);
   }

   @Override protected boolean recalcDataRange(Object hint)
   {
      boolean needRecalc = (hint == null) || (hint == FGNProperty.SRC);
      if(!needRecalc) return(false);
      
      DataSet set = getDataSet();

      boolean changed = (cachedDataRange[0] != set.getXMin()) || (cachedDataRange[1] != set.getXMax());
      changed = changed || (cachedDataRange[2] != set.getYMin()) || (cachedDataRange[3] != set.getYMax());
      changed = changed || (cachedDataRange[4] != set.getZMin()) || (cachedDataRange[5] != set.getZMax());
      if(changed)
      {
         cachedDataRange[0] = set.getXMin();
         cachedDataRange[1] = set.getXMax();
         cachedDataRange[2] = set.getYMin();
         cachedDataRange[3] = set.getYMax();
         cachedDataRange[4] = set.getZMin();
         cachedDataRange[5] = set.getZMax();
      }
      return(changed);
   }

   /** 
    * A 3D scatter plot can render a 3D point set {@link Fmt#XYZSET} or a 4D point set  {@link Fmt#XYZWSET}. Note that
    * the 4th data dimension is represented only in the bubble plot display modes.
    */
   @Override public boolean isSupportedDataFormat(Fmt fmt) { return(fmt==Fmt.XYZSET || fmt==Fmt.XYZWSET); }


   /** The data formats that may be rendered by a 3D scatter plot node. */
   private final static Fmt[] supportedFormats = new Fmt[] {Fmt.XYZSET, Fmt.XYZWSET};
   
   @Override public Fmt[] getSupportedDataFormats() { return(supportedFormats); }
   
   @Override public SymbolNode getSymbolNode() { return((SymbolNode) getComponentNodeAt(0)); }

   /** 
    * The 3D scatter plot node draws a marker symbol for each well-defined point in the data source -- unless
    * configured in one of the bar plot-like display modes.
    */
   @Override public boolean usesSymbols() { return(!isBarPlotDisplayMode()); }

   /** 
    * The 3D scatter plot node should be represented by a horizontal bar in the graph legend when in it is configured
    * in one of the bar plot-like display modes.
    */
   @Override public boolean useBarInLegend() { return(isBarPlotDisplayMode()); }
   
   /** The 3D scatter plot must appear in a 3D graph container. */
   @Override public boolean is3D() { return(true); }

   /** 
    * The legend entry for a 3D scatter plot node will typically show only the marker symbol without a trace line 
    * segment -- unless stem lines are drawn. But it also must handle the possibility that the node is configured in
    * one of the bar plot display modes, in which case the legend entry should have a different appearance. Given the 
    * way legend entries are currently handled, it is necessary to provide a custom legend entry to handle this 
    * scenario.  See {@link ScatterPlotLegendEntry}.
    */
   @Override public List<LegendEntry> getLegendEntries()
   {
      if(!getShowInLegend()) return(null);
      List<LegendEntry> out = new ArrayList<>();
      out.add(new ScatterPlotLegendEntry(this));
      return(out);
   }
   
   //
   // Focusable/Renderable support
   //
   
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null || shapePainter == null)
      {
         updateRenderingResources();
         shapePainter.updateFontRenderContext(g2d);
         shapePainter.invalidateBounds();
         polyPainter.updateFontRenderContext(g2d);
         polyPainter.invalidateBounds();
         rBoundsSelf = shapePainter.getBounds2D(null);
         
         // the polyline painter may not render anything, so we DON'T want to union with an empty rect located at (0,0)!
         Rectangle2D rPoly = polyPainter.getBounds2D(null);
         if(!rPoly.isEmpty()) Rectangle2D.union(rBoundsSelf, polyPainter.getBounds2D(null), rBoundsSelf);
         
         // if any backplane projection is rendered, we include that in the render bounds
         Graph3DNode g3 = getParentGraph3D();
         DataSet ds = getDataSet();
         Projector prj = (g3==null) ? null : g3.get2DProjection();
         Point2D p = new Point2D.Double();
         Rectangle2D rProj = new Rectangle2D.Double();
         for(Side backplane : Side.values()) if(isProjectionRendered(backplane) && prj != null)
         {
            // find bounding box of valid locations
            boolean gotValidLoc = false;
            double xMin = Double.POSITIVE_INFINITY;
            double xMax = Double.NEGATIVE_INFINITY;
            double yMin = Double.POSITIVE_INFINITY;
            double yMax = Double.NEGATIVE_INFINITY;
            
            for(int i=0; i<ds.getDataSize(-1); i++) 
            {
               p = prj.project(backplane==Side.YZ ? prj.getBackSideX() : ds.getX(i, -1), 
                     backplane==Side.XZ ? prj.getBackSideY() : ds.getY(i, -1), 
                     backplane==Side.XY ? prj.getBackSideZ() : ds.getZ(i), 
                     p);
               
               if(Utilities.isWellDefined(p))
               {
                  gotValidLoc = true;
                  if(p.getX() < xMin) xMin = p.getX();
                  if(p.getX() > xMax) xMax = p.getX();

                  if(p.getY() < yMin) yMin = p.getY();
                  if(p.getY() > yMax) yMax = p.getY();
               }
            }
            
            // grow strict bounding box by half the projection dot size to ensure it encompasses any dots lying along
            // the edges
            if(gotValidLoc)
            {
               double szMI = getProjectionDotSize(backplane) * Measure.PT2IN * 1000;
               rProj.setRect(xMin-szMI/2, yMin-szMI/2, xMax-xMin+szMI, yMax-yMin+szMI);
               Rectangle2D.union(rBoundsSelf, rProj, rBoundsSelf);
            }
         }
      }
      return((Rectangle2D)rBoundsSelf.clone());
   }

   /**
    * This method releases the internal painters used to render the 3D scatter plot, as well as a cached rectangle 
    * bounding any marks made by the node.
    */
   @Override protected void releaseRenderResourcesForSelf() 
   { 
      rBoundsSelf = null; 
      shapePainter = null;
      polyPainter = null;
   }

   /** Render this 3D scatter plot node into the current graphics context IAW its current state. */
   @Override public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(needsRendering(task))
      {
         if(shapePainter == null) updateRenderingResources();
         if(!polyPainter.render(g2d, task)) return(false);
         return shapePainter.render(g2d, task);
      }
      return(true);
   }

   /**
    * Cached rectangle bounding only the marks made by this 3D scatter plot node. An empty rectangle indicates that the
    * element makes no marks when "rendered". If null, the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;

   /** 
    * This specialized shape painter renders the entire 3D scatter plot in any display mode -- except for the polyline
    * that optionally connects the data points in the "scatter" or "bubble" modes.
    */
   private MultiShapePainter shapePainter = null;

   /** This painter renders only the "connect-the-dots" polyline connecting the data points in the 3D scatter plot. */
   private PolylinePainter polyPainter = null;
   
   /** Create and/or update the internal painters that render this 3D scatter plot node IAW its current state. */
   private void updateRenderingResources()
   {
      if(shapePainter == null) shapePainter = new MultiShapePainter();
      shapePainter.setShapeProducer(isBarPlotDisplayMode() ? new BarProducer() : new ShapeProducer());
      if(polyPainter == null) polyPainter = new PolylinePainter(this, new DataPointProducer());
      polyPainter.setStroked(!getStemmed());
   }
   
   /** 
    * Is this 3D scatter plot currently configured in one of the two bar plot display modes?
    * @return True if current display mode is {@link DisplayMode#BARPLOT} or {@link DisplayMode#COLORBARPLOT}.
    */
   public boolean isBarPlotDisplayMode() { return(mode == DisplayMode.BARPLOT || mode == DisplayMode.COLORBARPLOT); }
   
   /**
    * Deos this 3D scatter plot also render its projection onto the specified backplane in the parent graph? The
    * projection is rendered as small filled dots on the backplane. It is rendered only if the graph backdrop is such
    * that the backplane is drawn, the projection dot size is nonzero, and the projection dot color is not transparent.
    * However, the dot projections are never rendered in either of the bar plot display modes.
    * @param backplane The backplane.
    * @return True if scatter plot's projection onto that backplane is rendered.
    */
   private boolean isProjectionRendered(Side backplane)
   {
      Graph3DNode g3 = getParentGraph3D();
      boolean rendered = (backplane != null) && (g3 != null) && g3.isBackPlaneDrawn(backplane);
      if(rendered) rendered = !isBarPlotDisplayMode();
      if(rendered) rendered = (getProjectionDotSize(backplane)>0) && (getProjectionDotColor(backplane).getAlpha()>0);
      return(rendered);
   }
   
   /**
    * Render the projections of this 3D scatter plot onto each of the three backplanes in the parent graph's current
    * backdrop. A projection is rendered as a set of small filled dots at the projected coordinates. The dot size and
    * color may be different for each backplane projection; a projection is NOT rendered if the corresponding backplane
    * is not drawn, if the dot size is zero, or the dot color is transparent.
    * 
    * <p>By design, this method should be called after the graph's backplanes have been rendered but before any of its
    * children (including this 3D scatter plot itself) are rendered.</p>
    * 
    * @param g2 The graphic context.
    * @param task The render task.
    */
   void renderProjections(Graphics2D g2, RenderTask task)
   {
      boolean drawXY = isProjectionRendered(Side.XY);
      boolean drawXZ = isProjectionRendered(Side.XZ);
      boolean drawYZ = isProjectionRendered(Side.YZ);
      if(!(drawXY || drawXZ || drawYZ)) return;
      
      DataSet ds = getDataSet();
      Projector prj = getParentGraph3D().get2DProjection();
      if(prj == null) return;
      
      // since we're using the supplied graphics context, make sure we restore the current color...
      Color origColor = g2.getColor();
      
      Point2D p = new Point2D.Double();
      Ellipse2D dot = new Ellipse2D.Double();
      if(drawXY)
      {
         g2.setColor(getProjectionDotColor(Side.XY));
         double szMI = getProjectionDotSize(Side.XY) * Measure.PT2IN * 1000.0;
         dot.setFrame(-szMI/2.0, -szMI/2.0, szMI, szMI);
         for(int i=0; i< ds.getDataSize(-1); i++)
         {
            prj.project(ds.getX(i, -1), ds.getY(i, -1), prj.getBackSideZ(), p);
            if(Utilities.isWellDefined(p))
            {
               g2.translate(p.getX(), p.getY());
               g2.fill(dot);
               g2.translate(-p.getX(), -p.getY());
            }
         }
      }

      if(drawXZ)
      {
         g2.setColor(getProjectionDotColor(Side.XZ));
         double szMI = getProjectionDotSize(Side.XZ) * Measure.PT2IN * 1000.0;
         dot.setFrame(-szMI/2.0, -szMI/2.0, szMI, szMI);
         for(int i=0; i< ds.getDataSize(-1); i++)
         {
            prj.project(ds.getX(i, -1), prj.getBackSideY(), ds.getZ(i), p);
            if(Utilities.isWellDefined(p))
            {
               g2.translate(p.getX(), p.getY());
               g2.fill(dot);
               g2.translate(-p.getX(), -p.getY());
            }
         }
      }

      if(drawYZ)
      {
         g2.setColor(getProjectionDotColor(Side.YZ));
         double szMI = getProjectionDotSize(Side.YZ) * Measure.PT2IN * 1000.0;
         dot.setFrame(-szMI/2.0, -szMI/2.0, szMI, szMI);
         for(int i=0; i< ds.getDataSize(-1); i++)
         {
            prj.project(prj.getBackSideX(), ds.getY(i, -1), ds.getZ(i), p);
            if(Utilities.isWellDefined(p))
            {
               g2.translate(p.getX(), p.getY());
               g2.fill(dot);
               g2.translate(-p.getX(), -p.getY());
            }
         }
      }
      
      g2.setColor(origColor);
   }
   
   //
   // PSTransformable
   //
   
   @Override public void toPostscript(PSDoc psDoc)
   {
      // the bar plot display modes are handled differently from the scatter/bubble plot modes...
      if(!isBarPlotDisplayMode())
      {
         // if connect-the-dots polyline is drawn, prepare the array of points in the polyline
         Point2D[] polyCoords = null;
         if(isStroked() && !getStemmed())
         {
            Iterator<Point2D> iterator = new DataPointProducer();
            List<Point2D> ptList = new ArrayList<>();
            while(iterator.hasNext())
            {
               Point2D next = (Point2D) iterator.next().clone();    // b/c DataPointProducer reuses a Point2D
               ptList.add(next);
            }
            polyCoords = new Point2D[ptList.size()];
            for(int i = 0; i < ptList.size(); i++) polyCoords[i] = ptList.get(i);
            
            // in case the point list is really large
            ptList.clear();
         }
         
         ShapeProducer producer = new ShapeProducer();
         List<Point2D> pts = new ArrayList<>();
         List<Double> adornSizes = new ArrayList<>();
         List<BkgFill> adornBkgFills = new ArrayList<>();
         
         if(producer.sizeConstant) adornSizes.add(producer.maxSymSizeMI);
         if(producer.fillConstant) adornBkgFills.add(producer.bkgFill);
         boolean doStems = (producer.pStemEnd != null);
         
         while(producer.hasNext())
         {
            producer.next();
            
            Point2D p = producer.getLocation();
            pts.add(new Point2D.Double(p.getX(), p.getY()));
            if(doStems)
            {
               p = producer.getStemEnd();
               pts.add(new Point2D.Double(p.getX(), p.getY()));
            }
            
            if(!producer.sizeConstant)
               adornSizes.add(producer.currSymSzMI);
            if(!producer.fillConstant)
               adornBkgFills.add(producer.bkgFill);
         }
         
         int n = pts.size();
         if(doStems) n /= 2;
         
         // note: we configure graphics state for stroking the symbols! Stems are stroked IAW this node's styles
         // we have to do some hacking to get the graphics state correct when we draw the trace line instead of the
         // stem lines.
         if(polyCoords != null)
         {
            psDoc.startElement(this, this, false);
            psDoc.renderPolyline(polyCoords, null, 0, null, null, false);
            psDoc.startElement(this, getSymbolNode(), true);
            psDoc.renderStemmedAdornments(
                  n, producer.symbol.getType(), doStems ? this : null, pts, adornSizes, adornBkgFills);
            psDoc.endElement();
            psDoc.endElement();
         }
         else
         {
            psDoc.startElement(this, getSymbolNode(), false);
            psDoc.renderStemmedAdornments(
                  n, producer.symbol.getType(), doStems ? this : null, pts, adornSizes, adornBkgFills);
            psDoc.endElement();
         }
      }
      else
      {
         // for the bar plot display modes, we prepare a polygon mesh as for a surface; in this case, the polygons
         // don't form a mesh -- but it works because the PSDoc routine fills and strokes each polygon separately
         boolean isCMap = (mode==DisplayMode.COLORBARPLOT);
         List<Point2D> verts = new ArrayList<>();
         List<Number> fillInfo = new ArrayList<>();

         BarProducer barGenerator = new BarProducer();
         while(barGenerator.hasNext())
         {
            barGenerator.next();
            Point2D[] bar = barGenerator.getBarVerts();
            for(int i=0; i<4; i++)
               verts.add(new Point2D.Double(bar[i].getX(), bar[i].getY()));
            if(isCMap)
            {
               if(barGenerator.getFillPaint() instanceof Color)
               {
                  fillInfo.add(Double.NaN);
                  fillInfo.add(barGenerator.getPackedRGBForDataZ());
               }
               else
               {
                  Point2D p = barGenerator.getGradientFillStart();
                  fillInfo.add(p == null ? 0 : p.getX());
                  fillInfo.add(p == null ? 0 : p.getY());
                  p = barGenerator.getGradientFillEnd();
                  fillInfo.add(p == null ? 0 : p.getX());
                  fillInfo.add(p == null ? 0 : p.getY());
               }
            }
         }
         if(verts.isEmpty()) return;
         
         psDoc.startElement(this);
         boolean noFill = (!isCMap) && getFillColor().getAlpha() == 0;
         psDoc.renderMesh(verts.size()/4, verts, noFill ? null : fillInfo, isCMap ? barGenerator.colorLUT : null);
         psDoc.endElement();
         verts.clear();
         fillInfo.clear();
      }
   }
   
   /**
    * Prepare the Postscript code segment that rendered the projections of this 3D scatter plot onto the parent graph's 
    * backplanes IAW this node's current definition. This method is intended to be called while preparing the Postscript
    * representation of a figure; it should be called after drawing the 3D graph backdrop but before any children of 
    * that graph, since the scatter plot projections are intended to appear as if they lie on the backplanes.
    * 
    * @param psDoc The Postscript document being prepared.
    */
   void renderProjectionsToPostscript(PSDoc psDoc)
   {
      if(isProjectionRendered(Side.XY))
         new ProjectionPSTransformable(Side.XY).toPostscript(psDoc);
      if(isProjectionRendered(Side.XZ))
         new ProjectionPSTransformable(Side.XZ).toPostscript(psDoc);
      if(isProjectionRendered(Side.YZ))
         new ProjectionPSTransformable(Side.YZ).toPostscript(psDoc);
   }
   
   /**
    * A {@link PSTransformable} implementation for the projection of this 3D scatter plot onto one of the backplanes
    * in the parent 3D graph's current backdrop.
    * 
    * <p>Since a projection is rendered differently from the scatter plot itself, this provides the Postscript 
    * graphics state information relevant to the projection. It is configured to have no stroke, no font (it does not
    * render text), and a text/fill color that matches the projection's assigned dot color. The method {@link 
    * #toPostscript(PSDoc)} renders the actual projection by appending the necessary Postscript code segment to the
    * Postscript document being prepared.</p>
    */
   private class ProjectionPSTransformable implements PSTransformable
   {
      ProjectionPSTransformable(Side backplane) { this.backplane = backplane; }
      
      @Override public int getPSTextAndFillColor()
      {
         return((getProjectionDotColor(backplane).getRGB() & 0x00FFFFFF));
      }

      @Override public String getPSComment()
      {
         return("3D scatter plot projection onto" + backplane + " backplane");
      }

      @Override public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException 
      {
         if(!isProjectionRendered(backplane)) return;
         
         DataSet ds = getDataSet();
         Projector prj = getParentGraph3D().get2DProjection();
         if(prj == null) return;
         
         double szMI = getProjectionDotSize(Side.XY) * Measure.PT2IN * 1000.0;
         Point2D[] coords = new Point2D[ds.getDataSize(-1)];
         for(int i=0; i<coords.length; i++)
         {
            double x = (backplane == Side.YZ) ? prj.getBackSideX() : ds.getX(i, -1);
            double y = (backplane == Side.XZ) ? prj.getBackSideY() : ds.getY(i, -1);
            double z = (backplane == Side.XY) ? prj.getBackSideZ() : ds.getZ(i);
            coords[i] = prj.project(x, y, z, null);
         }
         
         psDoc.startElement(this);
         psDoc.renderMultipleAdornments(coords, null, Marker.CIRCLE, szMI, null);
         psDoc.endElement();
      }
      
      @Override public String getPSFontFace() { return(null); }
      @Override public void getAllPSFontFacesUsed(Map<String, String> fontFaceMap, boolean traverse) {}
      @Override public double getPSFontSize() { return(0); }
      @Override public double getPSStrokeWidth() { return(0); }
      @Override public int getPSStrokeEndcap() { return(0); }
      @Override public int getPSStrokeJoin() { return(0); }
      @Override public int[] getPSStrokeDashPattern() { return(null); }
      @Override public int getPSStrokeDashOffset() { return(0); }
      @Override public int getPSStrokeColor() { return(0); }
      @Override public boolean isPSNoFill() { return(false); }
      @Override public boolean isPSNoStroke() { return(true); }
      
      private final Side backplane;
   }
   
   // 
   // Object
   //

   /**
    * This override ensures that the rendering infrastructure for the clone is independent of the 3D scatter plot node
    * that was cloned. The clone will reference the same data set, however!
    */
   @Override protected Scatter3DNode clone() throws CloneNotSupportedException
   {
      Scatter3DNode copy = (Scatter3DNode) super.clone();
      copy.rBoundsSelf = null;
      copy.shapePainter = null;
      copy.polyPainter = null;
      return(copy);
   }
   
   /** 
    * Helper class extends {@link Point3D} to a 4th dimension "W" to handle points from a 4D data set yet be able to
    * order a list of such points based on the their locations in 3D space using {@link P3DComparator}.
    * @author sruffner
    */
   private static class Point4D extends Point3D
   {
      Point4D(double x, double y, double z, double w)
      {
         super(x, y, z);
         this.w = w;
      }

      public double getW() { return(w); }
      
      @Override
      public boolean isWellDefined() { return(super.isWellDefined() && Utilities.isWellDefined(w)); }
      
      /** The coordinate value in the 4th dimension. */
      private final double w;
   }
   
   /**
    * Helper class defines an iterator over the set of shapes that are to be painted by the {@link MultiShapePainter}
    * that renders the 3D scatter plot node in any of the 4 scatter plot display modes. It serves both as the iterator 
    * implementation and the iterator provider (it simply provides fresh copies of itself).
    * 
    * <p>For each well-defined data point in the underlying data set, the iterator supplies the shape primitive as well
    * as its location, the end point of an associated stem line (if any), and fill and stroking properties. The stroking
    * properties are the same for all shapes (and stem lines) and match the stroking properties
    * of the 3D scatter plot node itself.</p>
    * 
    * <p>The shape primitive is defined by the 3D scatter plot's {@link SymbolNode} component. In the {@link 
    * DisplayMode#SCATTER} and {@link DisplayMode#COLORBUBBLE} modes, that shape primitive never changes; in the 
    * {@link DisplayMode#SIZEBUBBLE} and {@link DisplayMode#COLORSIZEBUBBLE} modes, the shape primitive is rescaled so 
    * that its size is proportional to Z for a 3D data set, and proportional to W for a 4D data set. Similarly, in the
    * <b>COLORBUBBLE</b> and <b>COLORSIZEBUBBLE</b> modes, the color of the symbol reflects the value of the Z- or W-
    * coordinate.</p>
    * 
    * <p>Whether the source data set is 3D or 4D, the points are reordered so that they are rendered from "back" to 
    * "front" in 3D space. To accommodate this, the helper class {@link Point4D} encapsulates a 4D point but extends
    * {@link Point3D} so that the set of points may be reordered IAW their location in 3D space.</p>
    * 
    * <b>Do NOT use this helper class for either of the two bar plot display modes. See {@link BarProducer}.</b>
    * 
    * @author  sruffner
    */
   private class ShapeProducer implements Iterable<PaintedShape>, Iterator<PaintedShape>, PaintedShape
   {
      ShapeProducer()
      {
         symbol = getSymbolNode();
         bkgFill = Scatter3DNode.this.getBackgroundFill();

         DataSet ds = getDataSet();
         Fmt fmt = ds.getFormat();
         is4D = (fmt==Fmt.XYZWSET);
         
         // when symbol size varies, it will be scaled IAW the Z-coordinate for 3D data sets and the W-coordinate for 4D
         wDataAbsMax = is4D ? 
               Math.max(Math.abs(ds.getWMin()), Math.abs(ds.getWMax())) :
               Math.max(Math.abs(ds.getZMin()), Math.abs(ds.getZMax()));
         
         scatterPts = new ArrayList<>();
         nSoFar = 0;
         pLoc = new Point2D.Double();
         
         // here we reorder the data points IAW their locations in 3D space, from back to front. Of course, this will
         // depend on the current state of the 3D-to-2D projection. We use a 4D point so we can handle either a 3D or
         // a 4D data set. In the latter case, the 4th dimension controls the size and/or color of the symbol at each
         // well-defined data point.
         //
         // note: no points generated if the current display mode is one of the bar plot options!
         Graph3DNode g3 = getParentGraph3D();
         if(g3 != null) prj = g3.get2DProjection();
         if(prj != null && !isBarPlotDisplayMode())
         {
            for(int i=0; i<ds.getDataSize(-1); i++)
               scatterPts.add(new Point4D(ds.getX(i, -1), ds.getY(i, -1), ds.getZ(i), is4D ? ds.getW(i) : 0f));

            // check X-coord first over Y when X-axis of 3D graph is more back-to-front than side-to-side than Y axis
            double absRot = Math.abs(prj.getRotationAngle());
            P3DComparator c = new P3DComparator((absRot <= 45), 
                  prj.getBackSideX() > prj.getFrontSideX(), prj.getBackSideY() > prj.getFrontSideY());
            
            scatterPts.sort(c);
         }

         
         // if stems are drawn and the specified stem base plane is outside the graph's 3D box, then we use the graph's
         // XY backplane as the stem base plane.
         boolean doStems = (prj != null) && Scatter3DNode.this.isStroked() && Scatter3DNode.this.getStemmed();
         double base = 0;
         if(doStems)
         {
            base = getZBase();
            if(base < prj.getZMin() || base > prj.getZMax()) base = prj.getBackSideZ();
            pStemEnd = new Point2D.Double();
         }
         stemBase = base;
         
         DisplayMode mode = getMode();
         sizeConstant = (mode==DisplayMode.SCATTER) || (mode==DisplayMode.COLORBUBBLE);
         fillConstant = (mode==DisplayMode.SCATTER) || (mode==DisplayMode.SIZEBUBBLE);
         maxSymSizeMI = symbol.getSizeInMilliInches();

         shapePrimitive = new GeneralPath();
         if(sizeConstant)
         {
            PathIterator pi = symbol.getType().getDesignShape().getPathIterator(
                  AffineTransform.getScaleInstance(maxSymSizeMI, maxSymSizeMI));
            shapePrimitive.setWindingRule(pi.getWindingRule());
            shapePrimitive.append(pi, false);
            currSymSzMI = maxSymSizeMI;
         }
         else
            xfm = new AffineTransform();

         if(fillConstant || g3 == null)
         {
            wRange = null;
            colorLUT = null;
            isLogCMap = false;
         }
         else
         {
            ColorBarNode cbar = getParentGraph().getColorBar();
            double start = cbar.getStart();
            double end = cbar.getEnd();
            if(start > end)
            {
               double d = start; 
               start = end; 
               end = d;
            }
            else if(start == end) end = start + 1;
            
            wRange = new double[] { start, end };
            colorLUT = g3.getColorBar().getColorLUT();
            isLogCMap = g3.isLogarithmicColorMap();
         }
      }
      
      /** 
       * Construct a copy of the specified shape producer.
       * @param src The shape producer to copy.
       */
      ShapeProducer(ShapeProducer src)
      {
         prj = src.prj;
         is4D = src.is4D;
         scatterPts = src.scatterPts;
         wDataAbsMax = src.wDataAbsMax;
         nSoFar = 0;
         wRange = src.wRange;
         
         symbol = src.symbol;
         bkgFill = src.bkgFill;
         maxSymSizeMI = src.maxSymSizeMI;
         
         pLoc = new Point2D.Double();
         if(src.pStemEnd != null) pStemEnd = new Point2D.Double();
         stemBase = src.stemBase;
         
         sizeConstant = src.sizeConstant;
         if(sizeConstant) 
         {
            shapePrimitive = src.shapePrimitive;
            currSymSzMI = maxSymSizeMI;
         }
         else
         {
            shapePrimitive = new GeneralPath();  // this is mutable, so the clone can't reference the src member!
            xfm = new AffineTransform();
         }
         
         fillConstant = src.fillConstant;
         colorLUT = src.colorLUT;
         isLogCMap = src.isLogCMap;
      }
      
      @Override public Iterator<PaintedShape> iterator() { return( new ShapeProducer(this)); }

      @Override public boolean hasNext() { return(nSoFar < scatterPts.size()); }
      @Override public PaintedShape next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");
         
         Point4D p4 = scatterPts.get(nSoFar++);
         if(!p4.isWellDefined()) pLoc.setLocation(Double.NaN, Double.NaN);
         else
         {
            prj.project(p4, pLoc);
            if(pStemEnd != null) prj.project(p4.getX(),  p4.getY(), stemBase, pStemEnd);
            
            if(!fillConstant) 
            {
               // map W coordinate (or Z for 3D data) to solid color, then use that with the background fill descriptor 
               // to derive the fill descriptor for the next plot symbol. Note that, for gradients, the mapped color is 
               // used as the second color of the gradient. The other parameters of the gradient, as specified by the 
               // background fill, do not change.
               Color c = new Color(colorLUT.mapValueToRGB(is4D ? p4.getW() : p4.getZ(), wRange[0],wRange[1],isLogCMap));
               switch(bkgFill.getFillType())
               {
               case SOLID : 
                  bkgFill = BkgFill.createSolidFill(c); break;
               case AXIAL : 
                  bkgFill = BkgFill.createAxialGradientFill(bkgFill.getOrientation(), bkgFill.getColor1(), c);
                  break;
               case RADIAL :
                  bkgFill = BkgFill.createRadialGradientFill(
                        bkgFill.getFocusX(), bkgFill.getFocusY(), bkgFill.getColor1(), c);
                  break;
               }
            }
            if(!sizeConstant)
            {
               // map absolute value of W-coordinate (or Z for 3D data set) to a symbol size less than or equal to the 
               // maximum symbol size. We really expect all W (or Z) coordinates to be positive in this scenario... 
               // Scaled by the maximum absolute W (or Z) value.
               currSymSzMI = (wDataAbsMax == 0) ? maxSymSizeMI : 
                     Math.abs(is4D ? p4.getW() : p4.getZ()) * maxSymSizeMI / wDataAbsMax;

               // prepare the shape primitive at the size specified
               shapePrimitive.reset();
               xfm.setToIdentity();
               if(currSymSzMI > 0)
               {
                  xfm.scale(currSymSzMI, currSymSzMI);
                  PathIterator pi = symbol.getType().getDesignShape().getPathIterator(xfm);
                  shapePrimitive.setWindingRule(pi.getWindingRule());
                  shapePrimitive.append(pi, false);
               }
            }
         }
         
         return(this);
      }
      
      @Override public void remove() { throw new UnsupportedOperationException("Removal not supported by iterator."); }
 

      @Override public Font getFont() { return(symbol.getFont()); }
      @Override public Color getStrokeColor() { return(symbol.getStrokeColor()); }
      @Override public Stroke getStroke(float dashPhase) { return(symbol.getStroke(dashPhase)); }
      @Override public double getFontSize() { return(symbol.getFontSize()); }
      @Override public double getStrokeWidth() { return(symbol.getStrokeWidth()); }
      @Override public boolean isStrokeSolid() { return(symbol.isStrokeSolid()); }
      @Override public boolean isStroked() { return(symbol.isStroked()); }
      
      /** 
       * The 3D scatter plot node paints closed symbols IAW the background fill property. For completeness, we return a
       * solid color here, but the shapePainter should use the background fill.
       */
      @Override public Color getFillColor() { return(bkgFill.getColor2()); }

      @Override public Shape getShape() { return(shapePrimitive); }

      @Override public Point2D getLocation() { return(pLoc); }
      @Override public Point2D getStemEnd() { return(pStemEnd); }
      @Override public PainterStyle getStemPainterStyle() { return(Scatter3DNode.this); }
      @Override public Paint getFillPaint()
      {
         Rectangle2D r = shapePrimitive.getBounds2D();
         return(bkgFill.getPaintForFill((float)r.getWidth(), (float)r.getHeight(), (float)r.getX(), (float)r.getY()));
      }
      @Override public boolean isClosed() { return(symbol.getType().isClosed()); }
      
      /** True if source data set is 4D; else 3D. */
      final boolean is4D;
      
      /** True if all plot symbols are drawn at the same size. */
      final boolean sizeConstant;
      /** True if all symbols are filled IAW the same background fill descripter. */
      final boolean fillConstant;
      /** The maximum size for the scatter plot marker symbols, in milli-inches. */
      final double maxSymSizeMI;
      /** 
       * The axis range for the parent 3D graph's color bar; applicable when background fill varies. When data is 4D,
       * this should be comparable to the range of data along the 4th dimension; when the data is 3D, it should be 
       * comparable to the range along the 3rd dimension.
       */
      final double[] wRange;
      /** The color lookup table for the parent 3D graph; applicable only when background fill varies. */
      final ColorLUT colorLUT;
      /** True if color map mode is logarithmic; applicable only when background fill varies. */
      final boolean isLogCMap;
      /** The Z-coordinate value for the stem base plane. Unused if stems are not drawn. */
      final double stemBase;

      /** 
       * Maximum of the absolute values across the 3rd or 4th dimension of underlying data (depending on whether the
       * data is 3D or 4D) -- for computing symbol size. 
       */
      final double wDataAbsMax;
      /** Defines the 3D-to-2D projection governing the parent 3D graph. */
      Projector prj = null;
      /** The scatter points listed in drawing order to minimize occlusion issues. */
      final List<Point4D> scatterPts;
      /** The number of scatter points produced thus far. */
      int nSoFar;

      /** 
       * The 3D scatter plot's component symbol node. It governs the rendering of each plot symbol (except for the
       * background fill descriptor), but not the stem lines.
       */
      final SymbolNode symbol;

      /** 
       * The current shape location. This is reused to deliver each point. IT IS ASSUMED that the consumer will NOT 
       * store a reference to this point, but will make a copy if needed.
       */
      final Point2D pLoc;
      /** 
       * The stem end point for the current shape location. Null if stems are not drawn. Otherwise, note that it is
       * reused to deliver each point. It is assumed that the consumer will NOT store a reference to it.
       */
      Point2D pStemEnd = null;
      /** 
       * The size of the (square) bounding box for the current symbol, in milli-inches. If all are rendered at the 
       * same size, this is the same value, {@link #maxSymSizeMI}, for all data points processed.
       */
      double currSymSzMI = 0;
      
      /** Affine transform used to scale each plot symbol; unused if symbol size is fixed. */
      AffineTransform xfm = null;
      /** 
       * The current shape to draw. If symbol size is fixed, this is determined at construction time and never
       * changes. Otherwise, it is calculated for each data point (X,Y,Z) in the source data set.
       */
      final GeneralPath shapePrimitive;
      /** 
       * The background fill for the current shape. If display mode is such that the fill does not change, this is
       * determined at construction time and never changes. In either of the "color bubble" display modes, its value is
       * prepared for each data point (X,Y,Z) in the source data set.
       */
      BkgFill bkgFill;
   }
   
   /**
    * Helper class defining the data point producer for the {@link PolylinePainter} that renders the polyline connecting
    * the data points in the 3D scatter plot in any of the "scatter" or "bubble" plot modes. The 3D data points are 
    * projected into the parent 3D graph's 2D viewport IAW the graph's {@link Projector}. 
    * 
    * <p>Note that this producer is relevant only when the 3D scatter plot is configured to render that polyline with
    * a non-empty stroke. When the polyline is not drawn, the producer will generate no points.</p>
    * 
    * @author sruffner
    */
   private class DataPointProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      DataPointProducer()
      {
         ds = getDataSet();
         Graph3DNode g3 = getParentGraph3D();
         if(g3 != null) prj = g3.get2DProjection();
         nTotal = ((prj != null) && (!isBarPlotDisplayMode()) && isStroked() && !getStemmed()) ? ds.getDataSize(-1) : 0;
         nSoFar = 0;
         pCurrent = new Point2D.Double();
      }
      
      @Override public Iterator<Point2D> iterator() { return(new DataPointProducer()); }
      @Override public boolean hasNext() { return(nSoFar < nTotal); }
      
      @Override public Point2D next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");
         
         prj.project(ds.getX(nSoFar, -1), ds.getY(nSoFar, -1), ds.getZ(nSoFar), pCurrent);
         ++nSoFar;
         return(pCurrent);
      }
      
      @Override public void remove() { throw new UnsupportedOperationException("Removal not supported."); }
      
      /** The 3D scatter plot's underlying data source. */
      final DataSet ds;
      /** Defines the 3D-to-2D projection governing the parent 3D graph. */
      Projector prj = null;
      /** The number of points produced thus far. */
      int nSoFar;
      /** The total number of points to be produced. */
      final int nTotal;
      /** 
       * The current point. This is reused to deliver each point. IT IS ASSUMED that the consumer will NOT store a 
       * reference to this point, but will make a copy if needed.
       */
      final Point2D pCurrent;
   }
   
   
   /**
    * Helper class defines an iterator over the set of 3D bar faces that are to be painted by {@link MultiShapePainter}
    * when the 3D scatter plot node is configured in either of the bar plot-like display modes. It serves both as the 
    * iterator implementation and the iterator provider (it simply provides fresh copies of itself).
    * 
    * <p>For each well-defined data point (X,Y,Z) in the underlying data set, the iterator supplies the three visible
    * faces of the 3D bar that extends from (X,Y,Zo) to (X,Y,Z), where Zo = {@link #getZBase()} (note that Zo = the 3D 
    * graph's backside Z-coordinate if the user-specified base plane Z is outside the Z-axis range). Each bar has an XY 
    * cross-section (in the 3D world) with side length specified by the node's {@link #getBarSize()} property. The bar
    * faces are iterated in the sequence: XY (face parallel to XY plane of 3D graph), YZ, XZ.</p>
    * 
    * <p>In the {@link DisplayMode#BARPLOT} mode, all bar faces will be filled with the same color as specified by the
    * 3D scatter plot node's "fillColor" property. In the {@link DisplayMode#COLORBARPLOT} mode, the YZ and XZ vertical
    * faces are filled with a linear color gradient IAW the 3D graph's current color map and the Z-coordinate range
    * spanned by the bar face, while the XY face is filled with the color that maps to the Z-coordinate for that face.
    * <i>However, the linear color gradient fill is not appropriate when the Z-axis is logarithmic. In this scenario, 
    * all three faces are filled with a single-color that maps to the Z-coordinate of the data point represented by the 
    * bar.</i></p>
    * 
    * <p>All three bar faces are stroked IAW the 3D scatter plot node's stroking properties.</p>
    * 
    * <p>The bar plot display modes are only suited to portray 3D data sets. If the underlying data set is 4D, the 4th
    * data coordinate "W" is simply ignored.</p>
    * 
    * <b>Do NOT use this helper class for any of the scatter or "bubble" display modes. See {@link ShapeProducer}.</b>
    * 
    * @author  sruffner
    */
   private class BarProducer implements Iterable<PaintedShape>, Iterator<PaintedShape>, PaintedShape
   {
      BarProducer()
      {
         // get the 3D data points and sort them in back-to-front order IAW the graph's current 2D projection. Note
         // that ill-defined data points are skipped.
         DataSet ds = getDataSet();
         
         scatterPts = new ArrayList<>();
         
         // note: no points generated if display mode is not one of the bar plot modes!
         Graph3DNode g3 = getParentGraph3D();
         if(g3 != null) prj = g3.get2DProjection();
         if(prj != null && isBarPlotDisplayMode())
         {
            for(int i=0; i<ds.getDataSize(-1); i++)
            {
               Point3D p3 = new Point3D(ds.getX(i,-1), ds.getY(i,-1), ds.getZ(i));
               if(Utilities.isWellDefined(p3)) scatterPts.add(p3);
            }
            
            // check X-coord first over Y when X-axis of 3D graph is more back-to-front than side-to-side than Y axis
            double absRot = Math.abs(prj.getRotationAngle());
            P3DComparator c = new P3DComparator((absRot <= 45), 
                  prj.getBackSideX() > prj.getFrontSideX(), prj.getBackSideY() > prj.getFrontSideY());
            
            scatterPts.sort(c);
            
            // bar cross-section size in 3D world units
            barSz = prj.getXExtent() * ((double) getBarSize()) / 100.0;
            
            // location of Z base plane. If outside graph's 3D box, use the Z-coordinate for the graph's XY backplane
            zBase = (getZBase() < prj.getZMin() || getZBase() > prj.getZMax()) ? prj.getBackSideZ() : getZBase();
           
         }
         else
         {
            barSz = 0;
            zBase = 0;
         }
          
         // color mapping information - only needed when bars are color-mapped. Note that we cannot render a 
         // logarithmic color gradient. Therefore, if the graph's color map is logarithmic (this will be the case if
         // the 3D graph's Z axis is logarithmic, the bars will be rendered in a single color that maps to the 
         // Z-coordinate of the data point represented by the bar 
         // bars were requested.
         if(g3 == null || getMode() != DisplayMode.COLORBARPLOT)
         {
            zRange = null;
            colorLUT = null;
            isLogCMap = false;
         }
         else
         {
            ColorBarNode cbar = g3.getColorBar();
            double start = cbar.getStart();
            double end = cbar.getEnd();
            if(start > end)
            {
               double d = start; 
               start = end; 
               end = d;
            }
            else if(start == end) end = start + 1;
            
            zRange = new double[] { start, end };
            colorLUT = cbar.getColorLUT();
            isLogCMap = g3.isLogarithmicColorMap();
         }

         nSoFar = 0;
         stage = 0;
         barFace = new GeneralPath();
         barVerts = new Point2D[4];
         for(int i=0; i<4; i++) barVerts[i] = new Point2D.Double();
         fillPaint = Scatter3DNode.this.getFillColor();
      }
      
      BarProducer(BarProducer src)
      {
         prj = src.prj;
         scatterPts = src.scatterPts;
         barSz = src.barSz;
         zBase = src.zBase;
         zRange = src.zRange;
         colorLUT = src.colorLUT;
         isLogCMap = src.isLogCMap;
         shapeLoc = src.shapeLoc;
         
         nSoFar = 0;
         stage = 0;
         barFace = new GeneralPath();
         barVerts = new Point2D[4];
         for(int i=0; i<4; i++) barVerts[i] = new Point2D.Double();
         fillPaint = Scatter3DNode.this.getFillColor();
         packedRGB = 0;
      }
      
      @Override public Iterator<PaintedShape> iterator()  { return(new BarProducer(this)); }

      @Override public boolean hasNext() { return(nSoFar < scatterPts.size()); }

      @Override public PaintedShape next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");
         
         barFace.reset();
         Point3D p3 = scatterPts.get(nSoFar);
         double x = p3.getX(), y = p3.getY(), z = p3.getZ();
         
         // for Postscript rendering, we need packed RGB color that maps to the Z-coordinate of the data point 
         // represented by the current bar. (Gradient fill currently not supported in Postcript rendering)
         if(colorLUT != null) packedRGB = 0x00FFFFFF & colorLUT.mapValueToRGB(z, zRange[0], zRange[1], isLogCMap);

         if(stage == 0)  // face || to XY backplane -- Zo or Z, whichever is closer to front-side Z
         {
            double zFace = prj.getFrontSideZ();
            zFace = (Math.abs(zFace-zBase) < Math.abs(zFace-z)) ? zBase : z;
            prj.offsetAndProject(x, y, zFace, barSz, barSz, 0, p2); 
            barFace.moveTo(p2.getX(), p2.getY()); 
            barVerts[0].setLocation(p2); 
            prj.offsetAndProject(x, y, zFace, barSz, -barSz, 0, p2); 
            barFace.lineTo(p2.getX(), p2.getY()); 
            barVerts[1].setLocation(p2);
            prj.offsetAndProject(x, y, zFace, -barSz, -barSz, 0, p2); 
            barFace.lineTo(p2.getX(), p2.getY());
            barVerts[2].setLocation(p2);
            prj.offsetAndProject(x, y, zFace, -barSz, barSz, 0, p2); 
            barFace.lineTo(p2.getX(), p2.getY()); 
            barVerts[3].setLocation(p2);
            barFace.closePath();

            // if bar is color-mapped, find the solid color corresponding to the value of Z across the XY face.
            // BUT, if logarithmic, find color corresponding to the data point's Z-coordinate. All bar faces will be
            // filled with this color
            if(colorLUT != null) 
               fillPaint = colorLUT.mapValueToRGBColor(isLogCMap ? z : zFace, zRange[0], zRange[1], isLogCMap);
         }
         else if(stage == 1)  // face || to YZ backplane -- closest to front-side X
         {
            double delta = ((prj.getFrontSideX() > prj.getBackSideX()) ? 1.0 : -1.0) * barSz;
            prj.offsetAndProject(x, y, z, delta, delta, 0, p2); 
            barFace.moveTo(p2.getX(), p2.getY());
            barVerts[0].setLocation(p2);
            prj.offsetAndProject(x, y, z, delta, -delta, 0, p2); 
            barFace.lineTo(p2.getX(), p2.getY());
            barVerts[1].setLocation(p2);
            prj.offsetAndProject(x, y, zBase, delta, -delta, 0, p2); 
            barFace.lineTo(p2.getX(), p2.getY());
            barVerts[2].setLocation(p2);
            prj.offsetAndProject(x, y, zBase, delta, delta, 0, p2); 
            barFace.lineTo(p2.getX(), p2.getY());
            barVerts[3].setLocation(p2);
            barFace.closePath();
            
            // if bar color-mapped: If linear color map, find the linear gradient fill. Else, use the solid color
            // that map's to the data point's Z-coordinate
            if(colorLUT != null)
            {
               if(!isLogCMap)
               {
                  prj.offsetAndProject(x, y, zRange[0], delta, 0, 0, startP);
                  prj.offsetAndProject(x, y, zRange[1], delta, 0, 0, endP);
                  fillPaint = colorLUT.getGradientPaint(
                        (float)startP.getX(), (float)startP.getY(), (float)endP.getX(), (float)endP.getY());
               }
               else
                  fillPaint = colorLUT.mapValueToRGBColor(z, zRange[0], zRange[1], true);
               
            }
         }
         else   // stage == 2: face || to XZ backplane -- closest to front-side Y
         {
            double delta = ((prj.getFrontSideY() > prj.getBackSideY()) ? 1.0 : -1.0) * barSz;
            prj.offsetAndProject(x, y, z, delta, delta, 0, p2); 
            barFace.moveTo(p2.getX(), p2.getY());
            barVerts[0].setLocation(p2);
            prj.offsetAndProject(x, y, z, -delta, delta, 0, p2); 
            barFace.lineTo(p2.getX(), p2.getY());
            barVerts[1].setLocation(p2);
            prj.offsetAndProject(x, y, zBase, -delta, delta, 0, p2); 
            barFace.lineTo(p2.getX(), p2.getY());
            barVerts[2].setLocation(p2);
            prj.offsetAndProject(x, y, zBase, delta, delta, 0, p2); 
            barFace.lineTo(p2.getX(), p2.getY());
            barVerts[3].setLocation(p2);
            barFace.closePath();
            
            // if bar color-mapped: If linear color map, find the linear gradient fill. Else, use the solid color
            // that map's to the data point's Z-coordinate
            if(colorLUT != null)
            {
               if(!isLogCMap)
               {
                  prj.offsetAndProject(x, y, zRange[0], 0, delta, 0, startP);
                  prj.offsetAndProject(x, y, zRange[1], 0, delta, 0, endP);
                  fillPaint = colorLUT.getGradientPaint(
                        (float)startP.getX(), (float)startP.getY(), (float)endP.getX(), (float)endP.getY());
               }
               else
                  fillPaint = colorLUT.mapValueToRGBColor(z, zRange[0], zRange[1], true);
            }
         }

         // move on to next bar face; if done with bar, move on to next data point
         ++stage;
         if(stage > 2) { ++nSoFar; stage = 0; }

         return(this);
      }

      @Override public void remove() { throw new UnsupportedOperationException("Removal not supported by iterator."); }
      

      @Override public Font getFont() { return(Scatter3DNode.this.getFont()); }
      @Override public Color getStrokeColor() { return(Scatter3DNode.this.getStrokeColor()); }
      @Override public Stroke getStroke(float dashPhase) { return(Scatter3DNode.this.getStroke(dashPhase)); }
      @Override public double getFontSize() { return(Scatter3DNode.this.getFontSize()); }
      @Override public double getStrokeWidth() { return(Scatter3DNode.this.getStrokeWidth()); }
      @Override public boolean isStrokeSolid() { return(Scatter3DNode.this.isStrokeSolid()); }
      @Override public boolean isStroked() { return(Scatter3DNode.this.isStroked()); }
      @Override public Color getFillColor() { return(Scatter3DNode.this.getFillColor()); }

      @Override public Shape getShape() { return(barFace); }
      @Override public Point2D getLocation() { return(shapeLoc); }
      @Override public Point2D getStemEnd() { return(null); }
      @Override public PainterStyle getStemPainterStyle() { return(Scatter3DNode.this); }
      @Override public Paint getFillPaint() { return(fillPaint); }
      @Override public boolean isClosed() { return(true); }

      /** 
       * Reference to the 4-element array containing the 2D vertices of the current bar face, in the same order they
       * were used to generate the closed path returned by {@link #getShape()}. <i>For internal use - to prepare the 
       * polygon mesh in the Postscript rendering of a 3D bar plot.</i>
       * @return Array holding the 4 vertices of the closed polygon representing the current bar face generated by this
       * producer. Note that the array is reused -- be sure to clone the points provided.
       */
      Point2D[] getBarVerts() { return(barVerts); }
      
      /**
       * Returns the RGB color that maps to the Z-coordinate of the data point represented by the current bar, IAW the
       * parent 3D graph's current color map. <i>For internal use - to prepare the polygon mesh in the Postscript 
       * rendering of a 3D bar plot; linear color gradients aren't currently supported by {@link PSDoc}.</i>
       * @return The packedRGB color. Returns 0 always if not rendering a color-mapped bar plot.
       */
      int getPackedRGBForDataZ() { return(packedRGB);  }
      
      /** 
       * Get the starting point for the linear gradient paint for the current bar face.
       * @return The starting point of line segment along which the linear color gradient is defined. Returns null if
       * bar is not color-mapped or if the current bar face is the XY face, which is filled with a solid color.
       */
      Point2D getGradientFillStart() { return((colorLUT!=null && stage!=1) ? startP : null); }
      
      /** 
       * Get the ending point for the linear gradient paint for the current bar face.
       * @return The ending point of line segment along which the linear color gradient is defined. Returns null if
       * bar is not color-mapped or if the current bar face is the XY face, which is filled with a solid color.
       */
      Point2D getGradientFillEnd() { return((colorLUT!=null && stage!=1) ? endP : null); } 
      
      /** 
       * The Z data range spanned by the parent 3D graph's color map; applicable only when bars are color-mapped, else 
       * null. NOTE that this does not necessarily match the range of the 3D graph's own Z axis!!
       */
      final double[] zRange;
      /** The color lookup table for the parent 3D graph; applicable only when bars are color-mapped, else null. */
      final ColorLUT colorLUT;
      /** True if 3D graph's Z-axis is logarithmic (in which case color map is also logarithmic. */
      final boolean isLogCMap;
      
      /** Size of bar cross-section parallel to XY plane, converted to the 3D world units of the graph's projection. */
      final double barSz;
      
      /** 
       * The Z-coordinate at the base plane Z=Zo. The bar for data point (Xn,Yn,Zn) extends from that point to
       * (Xn,Yn,Zo). 
       */
      final double zBase;
      
      /** Defines the 3D-to-2D projection governing the parent 3D graph. */
      Projector prj = null;
      /** The 3D data points listed in drawing order to minimize occlusion issues. */
      final List<Point3D> scatterPts;

      /** The number of 3D bars produced thus far. */
      int nSoFar;
      /** 
       * The stage indicates where we are in generating the 3D bar for the current data point. We have to render
       * each of the three visible faces: 0 = XY face, 1 = YZ, 2 = XZ.
       */
      int stage;

      /** The bar face to be rendered next for the 3D bar currently being generated. */
      final GeneralPath barFace;
      
      /** The four 2D vertices defining the next bar face to render in the 2D projection of the 3D bar plot. */
      final Point2D[] barVerts;
      /** 
       * For the color-mapped bar plot, this is the packed RGB color that maps to the Z-coordinate of the data point 
       * represented by the current bar face.
       */
      int packedRGB = 0;
      
      /** 
       * For the color-mapped bar plot, this is set to the linear gradient paint with which the current bar face
       * should be filled; for the XY face it will be a single color corresponding to the Z-coordinate for that face.
       * For a single-color bar plot, this is set to the scatter plot node's fill color.
       */
      Paint fillPaint;
      
      /** 
       * The shape location. This is set to (0,0) and never changes, because the vertices of the bar faces are computed
       * WRT the parent graph's viewport using its 3D->2D projection.
       */
      Point2D shapeLoc = new Point2D.Double();
      
      /** Point used to compute the vertices of each bar face. */
      final Point2D p2 = new Point2D.Double();
      /** When a bar is color-mapped, this is the start point for the linear gradient paint for the current bar face. */
      final Point2D startP = new Point2D.Double();
      /** When a bar is color-mapped, this is the end point for the linear gradient paint for the current bar face. */
      final Point2D endP = new Point2D.Double();
   }
}
