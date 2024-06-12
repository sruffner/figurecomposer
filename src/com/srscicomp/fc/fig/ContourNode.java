package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.ContourGenerator;
import com.srscicomp.common.util.IDataGrid;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;

/**
 * <b>ContourNode</b> is a <b><i>presentation</i></b> element for the 3D data format {@link Fmt#XYZIMG}, which
 * defines a data matrix representing a variable that is a function of two independent variables, <i>Z(x,y)</i>, sampled
 * regularly over a rectangle in <i>(x,y)</i> space. Additional data set parameters specify the actual ranges <i>[x0 .. 
 * x1]</i> and <i>[y0 .. y1]</i> spanned by the data matrix in logical ("user") units. <b>ContourNode</b> can render the
 * data matrix in any of four types of plots, based on the node's display mode:
 * <ul>
 * <li>{@link DisplayMode#LEVELLINES}: A contour plot in which only contour level lines are drawn. The contour levels
 * may be manually specified or automatically selected. All level lines are drawn IAW this node's stroke properties,
 * but each line's color is color-mapped IAW its contour level and the parent graph's color map.</li>
 * <li>{@link DisplayMode#FILLEDCONTOURS}: A filled contour plot. The filled regions defined by the contour level list
 * are color-mapped, and any regions of undefined data ("NaN holes") are filled with the "NaN color" specified by the
 * parent graph's color axis. The level lines are also drawn, but all in the same color -- the node's stroke color.</li>
 * <li>{@link DisplayMode#HEATMAP} : A "heat map", i.e., an image in which each value in the data matrix is mapped to
 * an opaque RGB color through the parent graph's color map. See additional information below.</li>
 * <li>{@link DisplayMode#CONTOUREDHEATMAP}: A "heat map" with the contour level-lines superimposed. All level lines
 * are drawn in the node's current stroke color.</li>
 * </ul>
 * 
 * <p>The mapping of data to RGB color in the heat map image is controlled by the parent graph's color map, defined by 
 * properties on its "color" axis component. The color axis offers a number of different 256-entry RGB color lookup 
 * tables (LUT). Other properties govern how each value <i>Z</i> in the data matrix is mapped to an RGB entry in the
 * color map LUT <i>C[i]</i> of length <i>N=256</i>: the data range <i>[R0..R1]</i> that maps into the array, the 
 * mapping mode, and the "NaN color". All ill-defined data values map to color map index 0, and the RGB color at that 
 * index in the LUT is set to the "NaN color". All well-defined data map to the color indices [1..255]. All data values 
 * outside the specified range map to index 1 or 255 in the color map (a saturation effect). Two mapping modes are 
 * supported: 
 * <ul>
 *    <li><b>linear</b>: <i>C[ 1 + floor( (Z-R0)*(N-1)/(R1-R0) ) ]</i>.</li>
 *    <li><b>logarithmic</b>: <i>C[ 1 + floor( log(Z-R0+1)*(N-1)/log(R1-R0+1) ) ]</i>.</li>
 * </ul>
 * Contour levels are mapped to a color in the same manner.
 * </p>
 * 
 * <p>The implementation relies on {@link ContourGenerator} to generate the contour level lines and filled contour 
 * paths rendered in three of the 4 display modes. For the heat map display, <b>ContourNode</b> relies on the {@link 
 * DataSet#prepareImage()} method to prepare a {@link BufferedImage} that is the same width and height as the data 
 * matrix and that is initialized IAW the values in the data matrix and the current settings on the graph's color axis. 
 * The image is cached and reused each time the node is re-rendered; of course, the cached image must be recreated each 
 * time the data set or the graph's color map changes. During rendering, the cached image is copied into the parent 
 * graph's viewport; the data ranges <i>[x0..x1, y0..y1]</i> define an affine transform which scales and translates the 
 * image so that it appears at the right size and location within the graph.</p>
 * 
 * <p><b>NOTE</b>: <b>ContourNode</b>'s rendering is inherently rectangular (particularly the heat map plot), so it is 
 * NOT suited for display in polar coordinates. If embedded in a polar graph, the node will render nothing at all. In 
 * addition, if the graph is semilogX, semilogY, or log-log, no special logarithmic transformations are made when 
 * rending the image. <b>ContourNode</b> simply passes the data points <i>(x0,y0)</i> and <i>(x1, y1)</i> to the
 * parent graph's viewport for conversion to logical units in milli-inches, thereby obtaining the corners of the 
 * rectangle within which the buffered image is rendered.</p>
 * 
 * <p><b>Historical note</b>: The heat map plot has been available in <i>Figure Composer</i> since app version 3.0 and
 * <i>FypML</i> schema version 8. <b>ContourNode</b> replaced the deprecated <b>HeatMapNode</b> in app version 5.0.1 and
 * schema version 22, adding contour plot display modes to the original heat map functionality.</p>
 * 
 * @author sruffner
 */
public class ContourNode extends FGNPlottableData implements Cloneable
{

   /**
    * Construct a contour plot node initially configured to display an empty data set; it will make no marks in the 
    * parent graph. It is initialized in the "contours" display mode, and contour levels are unspecified (and are thus
    * selected automatically). A user preference determines whether or not smoothed image interpolation is enabled 
    * initially (for heatmap display modes only). All styles are implicit (inherited), and the title is an empty string.
    */
   public ContourNode()
   {
      super(HASTITLEATTR|HASSTROKEATTRS);
      setTitle("");
      mode = DisplayMode.LEVELLINES;
      smoothed = FGNPreferences.getInstance().getPreferredHeatMapImageSmoothingEnable();
      levelList = "";
   }
   
   
   //
   // Support for child nodes -- none permitted!
   //
   
   @Override public FGNodeType getNodeType() { return(FGNodeType.CONTOUR); }
   
   
   //
   // Properties
   //
   
   public enum DisplayMode 
   {
      /** Color-mapped contour level-lines only. */ LEVELLINES("levelLines"),
      /** Filled, color-mapped contour bands with level-lines superimposed. */ FILLEDCONTOURS("filledContours"),
      /** As a heat-map image. */ HEATMAP("heatMap"),
      /** As a heat-map image, with contour level-lines superimposed. */ CONTOUREDHEATMAP("contouredHeatMap");
      
      @Override public String toString() { return(niceName); }
      
      private DisplayMode(String s) { niceName = s; }
      
      /** The display mode name as it should appear in GUI and in FypML markup. */
      private String niceName;
  }
   
   /** The current data display mode. */
   private DisplayMode mode;
   
   /**
    * Get the current data display mode for this contour plot node.
    * @return The display mode.
    */
   public DisplayMode getMode() { return(mode); }
   
   /**
    * Set the data display mode for this contour plot node. If a change is made, {@link #onNodeModified()} is invoked.
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
   
   /** The maximum number of different contour levels that will be rendered in a a contour plot. */
   public static final int MAXLEVELS = ContourGenerator.MAXCONTOURS;
   
   /** 
    * User-specified contour levels, as a whitespace-separated list of floating-point string tokens. Out-of-range or 
    * repeat values are ignored. May contain up to {@link #MAXLEVELS} values. If empty, levels are auto-generated.
    */
   private String levelList;
   
   /**
    * Get the contour levels at which level-lines are drawn in this contour plot, as a whitespace-separated list of 
    * numeric tokens. Note that this level list is in the form entered by the user; if it contains any repeat or 
    * out-of-range values, these are simply ignored when the contour plot is generated. It may be an empty string, in 
    * which case the contour levels are automatically selected.
    * 
    * @return The user-specified contour level list, as described.
    */
   public String getLevelList() { return(levelList); }
   
   /**
    * Set the contour levels at which level-lines are drawn in this contour plot. The level list is specified in string
    * form, as a whitespace-separated list of numeric tokens. 
    * @param levels The level list -- a whitespce-separated list of numeric tokens. All tokens are parsed as integer or
    * floating-point values. Any repeat values are ignored, as are any values outside the Z-axis range of the node's 
    * data source. It may be an empty string, in which case the contour levels are automatically selected.
    * @return True if specified level list is valid. It will be rejected if it contains more than {@link @MAXLEVELS} 
    * tokens, or if any token cannot be parsed as a floating-point value.
    */
   public boolean setLevelList(String levels)
   {
      if(Utilities.parseDoubleList(levels, null, MAXLEVELS) == null) return(false);
      if(!levels.equals(levelList))
      {
         if(doMultiNodeEdit(FGNProperty.LEVELS, levels)) return(true);
         
         String oldList = this.levelList;
         this.levelList = levels;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.LEVELS);
            FGNRevEdit.post(this, FGNProperty.LEVELS, levelList, oldList);
         }
      }
      return(true);
   }
   
   /** 
    * Enable/disable smooth interpolation of heat map image when rendered at a size different from the actual cached 
    * image. Bicubic interpolation is used for smoothing. If disabled, nearest-neighbor interpolation is used.
    */
   private boolean smoothed;
   
   /**
    * Is smooth interpolation (bicubic algorithm) of the heat map image enabled during rendering? (If not, nearest-
    * neighbor interpolation is used, which results in blocky images when scaled up.)
    * @param True if smooth interpolation is enabled; false otherwise.
    */
   public boolean isSmoothed() { return(smoothed); }
   
   /**
    * Set the flag which enables/disable smooth interpolation of the heat map image during rendering. If enabled, 
    * bicubic interpolation is used to smooth the image when it is scaled up or down at rendering. If disabled, 
    * nearest-neighbor interpolation is used, which presents a blocky image when scaled up. If the flag's value is 
    * changed, {@link #onNodeModified()} is invoked.
    * 
    * @param b True to enable smooth interpolation, false to disable.
    */
   public void setSmoothed(boolean b)
   {
      if(smoothed != b)
      {
         if(doMultiNodeEdit(FGNProperty.SMOOTH, new Boolean(b))) return;
         
         Boolean old = new Boolean(smoothed);
         smoothed = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.SMOOTH);
            FGNRevEdit.post(this, FGNProperty.SMOOTH, new Boolean(smoothed), old, 
                  (smoothed ? "Enable" : "Disable") + " smooth interpolation of heatmap image");
         }
      }
   }
   
   
   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
         case MODE: value = getMode(); break;
         case LEVELS: value = getLevelList(); break;
         case SMOOTH: value = new Boolean(isSmoothed()); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }   

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
         case MODE: ok = setMode((DisplayMode)propValue); break;
         case LEVELS: ok = setLevelList((String)propValue); break;
         case SMOOTH: setSmoothed((Boolean)propValue); ok = true; break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   /**
    * When the source data set or the contour level list is modified, certain cached rendering resources (heat map 
    * image, contour paths) are invalidated if necessary to ensure they are updated before the next render cycle. In
    * addition, certain property changes will have no effect on the contour plot node's rendered appearance, depending
    * on the display mode:
    * <ul>
    * <li>For the {@link DisplayMode#HEATMAP} mode, the contour level list and stroke properties have no effect.</li>
    * <li>For the {@link DisplayMode#LEVELLINES} mode, stroke color has no effect (level lines are color-mapped).</li>
    * <li>The "smooth" flag has no effect if the display mode does not include the heat map image.</li>
    * </ul>
    * In these scenarios, the property change is forwarded to the owner model without triggering a render cycle.
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;
      
      // invalidate internal rendering infrastructure as needed
      if(hint == null || hint == FGNProperty.SRC) isCachedBIValid.set(false);
      if(hint == null || hint == FGNProperty.SRC || hint == FGNProperty.LEVELS || hint==FGNProperty.MODE) 
         contours = null;

      boolean needRender = true;
      if(hint != null)
      {
         if(hint == FGNProperty.STROKEC && (mode==DisplayMode.LEVELLINES || mode==DisplayMode.HEATMAP))
            needRender = false;
         else if(hint == FGNProperty.SMOOTH && (mode==DisplayMode.LEVELLINES || mode==DisplayMode.FILLEDCONTOURS))
            needRender = false;
         else if(mode == DisplayMode.HEATMAP && (hint==FGNProperty.LEVELS || hint==FGNProperty.STROKEW || 
               hint == FGNProperty.STROKECAP || hint==FGNProperty.STROKEJOIN))
            needRender = false;
      }
      
      if(!needRender)
         model.onChange(this, 0, false, null);
      else
         super.onNodeModified(hint);
   }

   
   //
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   /**
    * The node-specific properties exported in a contour plot node's style set include the display mode, the contour
    * level list, and the heat-map smoothing flag.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.MODE, getMode());
      styleSet.putStyle(FGNProperty.LEVELS, getLevelList());
      styleSet.putStyle(FGNProperty.SMOOTH, new Boolean(isSmoothed()));
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
      
      String s = (String) applied.getCheckedStyle(FGNProperty.LEVELS, getNodeType(), String.class);
      if(s != null && !s.equals(restore.getStyle(FGNProperty.LEVELS)))
      {
         levelList = s;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LEVELS);
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.SMOOTH, null, Boolean.class);
      if(b != null && !b.equals(restore.getStyle(FGNProperty.SMOOTH)))
      {
         smoothed = b.booleanValue();
         changed = true;
      }
      else restore.removeStyle(FGNProperty.SMOOTH);
            
      return(changed);
   }


   // 
   // FGNPlottableData, FGNPlottable
   //
   
   /**
    * When the contour plot's display mode calls for the heat map image, a color map change will invalidate the cached
    * heat map image, which will be rebuilt on the next rendering pass.
    */
   @Override public boolean onColorMapChange() { isCachedBIValid.set(false); return(true); }
   
   /**
    * Initializes the contour plot's data set so that it spans the specified X- and Y-axis ranges. The datg set will be
    * a 30x30 "image" with values populated by the function Z(c,r) = Z0 + (Z1-Z0)*sqrt(c*c + r*r)/30, where [Z0..Z1]
    * if the specified Z-axis range. If the graph is configured with a polar coordinate system, no action is taken; 
    * contour plots are currently not supported in a polar graph.
    */
   @Override protected void initializeDefaultData(double[] axisRng, boolean isPolar)
   {
      if(isPolar) return;
      
      float x0 = (float) axisRng[0];
      float x1 = (float) axisRng[1];
      float y0 = (float) axisRng[2];
      float y1 = (float) axisRng[3];
      float z0 = (float) axisRng[4];
      float z1 = (float) axisRng[5];
      
      float[] params = new float[] { x0 < x1 ? x0 : x1, x0 < x1 ? x1 : x0, y0 < y1 ? y0 : y1, y0 < y1 ? y1 : y0};
      float[] fData = new float[30*30];
      for(int r=0; r<30; r++)
      {
         for(int c=0; c<30; c++) fData[r*30+c] = z0 + (z1-z0)*((float) Math.sqrt(c*c + r*r))/30.0f;
      }
      
      DataSet ds = DataSet.createDataSet(getDataSet().getID(), Fmt.XYZIMG, params, 30, 30, fData);
      setDataSet(ds);
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

   /** The contour plot node can render a single data format: {@link Fmt#XYZIMG}. */
   @Override public boolean isSupportedDataFormat(Fmt fmt) { return(fmt == Fmt.XYZIMG); }

   /** The list of data formats that can be rendered in a contour plot. */
   private final static Fmt[] supportedFormats = new Fmt[] {Fmt.XYZIMG};
   
   @Override public Fmt[] getSupportedDataFormats() { return(supportedFormats); }
   
   /** The contour plot does NOT support a corresponding entry in its parent graph's automated legend. */
   @Override public boolean isLegendEntrySupported() { return(false); }

   /** The contour plot IS a 2D representation of 3D data. */
   @Override public boolean hasZData() { return(true); }

   
   //
   // Focusable/Renderable/PSTransformable/Object
   //
   
   /** The contour plot is not rendered if the source data set is empty, and never in a polar context. */
   @Override protected boolean isRendered()
   {
      FGNGraph g = getParentGraph();
      DataSet ds = getDataSet();
      return(g != null && (!g.isPolar()) && !ds.isEmpty());
   }

   /** 
    * Regardless the display mode, a contour plot does not make any translucent marks -- even if the stroke color is
    * translucent, that stroke is drawn over an opaque contour fill or heatmap image.
    */
   @Override protected boolean isTranslucent() { return(false); }

   /**
    * When the contour plot node is configured to render the heatmap image or filled contours, then this method assumes
    * that the rendering will fill the rectangle defined by the X/Y ranges of the underlying data matrix, transformed 
    * into the parent graph's viewport. If only contour curves are drawn, then the method finds the rectangle bounding
    * all of those curves. In either case, if the parent graph clips its content, the bounding rectangle is adjusted
    * accordingly. The bounding rectangle will be empty if the data matrix is empty, the parent graph is polar, or the
    * graph's viewport is not well-defined.
    */
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null)
      {
         FViewport2D parentVP = getParentViewport();
         FGNGraph g = getParentGraph();
         DataSet ds = getDataSet();
         rBoundsSelf = new Rectangle2D.Double(0, 0, 0, 0);
         if(g!= null && (!g.isPolar()) && parentVP != null && !ds.isEmpty())
         {
            if(mode == DisplayMode.LEVELLINES)
            {
               generateContoursIfNecessary(false);
               List<Point2D> pts = new ArrayList<Point2D>();
               for(ContourGenerator.Contour c : contours) pts.addAll(c.asPoints());
               parentVP.userUnitsToThousandthInches(pts);
               if(pts.size() > 1)
               {
                  GeneralPath gp = new GeneralPath();
                  gp.moveTo(pts.get(0).getX(), pts.get(0).getY());
                  for(Point2D p : pts) gp.lineTo(p.getX(), p.getY());
                  rBoundsSelf.setFrame(gp.getBounds2D());
               }
            }
            else
            {
               Point2D[] corners = new Point2D[2];
               corners[0] = new Point2D.Double(ds.getXMin(), ds.getYMin());
               corners[1] = new Point2D.Double(ds.getXMax(), ds.getYMax());
               parentVP.userUnitsToThousandthInches(corners);
               if(Utilities.isWellDefined(corners))
               {
                  double xMin = Math.min(corners[0].getX(), corners[1].getX());
                  double yMin = Math.min(corners[0].getY(), corners[1].getY());
                  double w = Math.abs(corners[0].getX() - corners[1].getX());
                  double h = Math.abs(corners[0].getY() - corners[1].getY());
                  rBoundsSelf.setFrame(xMin, yMin, w, h);
               }
            }
            
            if(g.getClip()) Rectangle2D.intersect(rBoundsSelf, g.getBoundingBoxLocal(), rBoundsSelf);
         }
      }
      return((Rectangle2D)rBoundsSelf.clone());
   }

   @Override protected void releaseRenderResourcesForSelf()
   {
      rBoundsSelf = null;
      if(contours != null)
      {
         contours.clear();
         contours = null;
      }
      isCachedBIValid.set(false);
      if(cachedBI != null)
      {
         cachedBI.flush();
         cachedBI = null;
      }
   }

   /**
    * Render this contour plot node into the current graphics context.
    * 
    * <p>If the current display mode requires the heat map image, and that image has not yet been prepared and cached 
    * internally (or if the cached image has been invalidated), the image is created from scratch IAW the node's current
    * data set and the parent graph's color map. This is the most time-consuming operation involved in rendering this
    * node, which is why the results are cached in off-screen memory. The heat map image is then translated and scaled 
    * as needed into the parent graph's data viewport IAW the underlying dataset's x- and y-coordinate spans, which 
    * indicate the rectangle in (x,y)-space covered by the rectangular heat map.</p>
    * 
    * <p>If the current display mode requires the contour paths, these will be generated if necessary, transformed to
    * the parent graph's viewport, and rendered. When only the contour level lines are drawn, their stroke color is
    * color-mapped IAW the contour level; otherwise, all lines are stroked using the node's stroke color. Contour fill
    * region colors are color-mapped IAW the contour level.</p>
    * 
    * <p><i>If the parent graph is polar, the node is not rendered at all.</i></p>
    */
   @Override public boolean render(Graphics2D g2d, RenderTask task)
   {
      // never rendered inside a polar plot, or if data set is empty
      FGNGraph g = getParentGraph();
      FViewport2D parentVP = getParentViewport();
      DataSet ds = getDataSet();
      if(g == null || g.isPolar() || parentVP == null || ds.isEmpty() || !needsRendering(task)) return(true);
      
      boolean ok = true;
      
      // render heat map image if display mode calls for it.
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         if(mode == DisplayMode.HEATMAP || mode == DisplayMode.CONTOUREDHEATMAP)
         {
            if(!isCachedBIValid.getAndSet(true))
            {
               ok = createCachedHeatMap(g2dCopy.getDeviceConfiguration());
               
               // preparing image could take some time, during which it could be invalidated or the render task aborted
               if(ok) ok = isCachedBIValid.get() && (task == null || task.updateProgress());
            }
            
            if(ok && cachedBI != null)
            {
               Point2D[] corners = new Point2D[2];
               corners[0] = new Point2D.Double(ds.getXMin(), ds.getYMin());
               corners[1] = new Point2D.Double(ds.getXMax(), ds.getYMax());
               parentVP.userUnitsToThousandthInches(corners);
              
               if(Utilities.isWellDefined(corners))
               {
                  double dx = corners[1].getX() - corners[0].getX();
                  double dy = corners[1].getY() - corners[0].getY();
                  AffineTransform at = AffineTransform.getTranslateInstance(corners[0].getX(), corners[0].getY());
                  at.scale(dx/ds.getDataBreadth(), dy/ds.getDataLength());
                  
                  // we use either bicubic (smooth) or nearest-neighbor (NOT smooth) interpolation when rendering the
                  // heatmap image. After rendering, restore bilinear interpolation method
                  if(!PDFSupport.isPDFGraphics(g2dCopy))
                  {
                     g2dCopy.setRenderingHint(
                           RenderingHints.KEY_INTERPOLATION, 
                           smoothed ? RenderingHints.VALUE_INTERPOLATION_BICUBIC : 
                                      RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                     g2dCopy.drawImage(cachedBI, at, null);
                     g2dCopy.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                  }
                  else
                  {
                     // 4/30/14: The above implementation does not work during export to PDF via iText PDF lib, b/c
                     // the iText PDFGraphics2D implementation appears to ignore rendering hints!
                     // 4/28/15: Performance tweak. The AffineTransformOp scaled to milli-inches will yield 1000x1000
                     // pixel images for a heatmap covering a 1-in square. This was overkill and led to large PDFs that
                     // took a long time to create when they contained multiple heatmaps. Instead, the filtered image
                     // is created at a resolution of 10mi (100dpi), and then scaled by 10 when it's drawn. This
                     // improved PDF export time by a factor of nearly 100!
                     // 5/18/20: If the X- or Y-axis is reversed, the scale factor for that dimension will be negative,
                     // and AffineTransformOp.createCompatibleImage() fails... The scale factors must be positive for
                     // the filtering transformation. When the filter image is drawn, we can flip it horizontally and/or
                     // vertically...
                     //
                     dx /= 10.0;
                     dy /= 10.0;
                     at = AffineTransform.getScaleInstance(
                           Math.abs(dx)/ds.getDataBreadth(), Math.abs(dy)/ds.getDataLength());
                     AffineTransformOp op = new AffineTransformOp(at, 
                           smoothed ? AffineTransformOp.TYPE_BICUBIC : AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                     
                     BufferedImage dstBI = op.createCompatibleDestImage(cachedBI, cachedBI.getColorModel());
                     dstBI = op.filter(cachedBI, dstBI);
                     
                     at = AffineTransform.getTranslateInstance(corners[0].getX(), corners[0].getY());
                     at.scale((dx<0 ? -1 : 1) * 10.0, (dy<0 ? -1 : 1)*10.0);
                     g2dCopy.drawImage(dstBI, at, null);
                     /*
                     at = AffineTransform.getScaleInstance(dx/ds.getDataBreadth(), dy/ds.getDataLength());
                     AffineTransformOp op = new AffineTransformOp(at, 
                           smoothed ? AffineTransformOp.TYPE_BICUBIC : AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                     g2d.drawImage(cachedBI, op, (int) corners[0].getX(), (int) corners[0].getY());
                     */
                  }
                  
                  ok = isCachedBIValid.get() && (task == null || task.updateProgress());
               }
            }
         }

         // render contour fill regions and/or level-lines as the display mode dictates
         if(ok && mode != DisplayMode.HEATMAP)
         {
            // contour level lines are not drawn if stroke width is 0 or their color is transparent. If neither the 
            // level lines nor contour fill regions are rendered, then we're done!
            boolean strk = (getStrokeWidth() > 0) && (mode==DisplayMode.LEVELLINES || getStrokeColor().getAlpha() != 0);
            if((!strk) && mode != DisplayMode.FILLEDCONTOURS) return(ok);
            
            generateContoursIfNecessary(false);
            
            // retrieve color map properties from the parent graph's Z-axis
            ColorLUT colorLUT = g.getColorBar().getColorLUT();
            Color nanC = g.getColorBar().getColorNaN();
            boolean isLog = g.getColorBar().isLogarithmic();
            double start = g.getColorBar().getStart();
            double end = g.getColorBar().getEnd();
            if(start > end)
            {
               double d = start; 
               start = end; 
               end = d;
            }
            else if(start == end) end = start + 1;
            
            // if level lines are drawn, they're all stroked the same, although the stroke color may be color-mapped
            if(strk) g2dCopy.setStroke(getStroke(0));
            
            for(ContourGenerator.Contour c : contours) if(c.isFillRegion() || strk)
            {
               List<Point2D> pts = c.asPoints();
               parentVP.userUnitsToThousandthInches(pts);
               
               GeneralPath gp = new GeneralPath();
               gp.moveTo(pts.get(0).getX(), pts.get(0).getY());
               for(Point2D p : pts) gp.lineTo(p.getX(), p.getY());
               if(c.isClosed()) gp.closePath();
               
               Color clr = nanC;
               double level = c.getLevel();
               if(!Utilities.isWellDefined(level)) clr = nanC;
               else if(mode != DisplayMode.LEVELLINES && !c.isFillRegion()) clr = getStrokeColor();
               else clr = new Color(colorLUT.mapValueToRGB(c.getLevel(), start, end, isLog));
               g2dCopy.setColor(clr);
               
               if(c.isFillRegion()) g2dCopy.fill(gp);
               else g2dCopy.draw(gp);
            }
         }
      }
      finally
      {
         if(g2dCopy != null) g2dCopy.dispose();
      }
      
      return(ok);
   }

   @Override public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      // never rendered inside a polar plot, or if data set is empty
      FGNGraph g = getParentGraph();
      if(g == null || g.isPolar()) return;
      
      FViewport2D parentVP = getParentViewport();
      DataSet ds = getDataSet();
      if(parentVP == null || ds.isEmpty()) return;
      
      Point2D[] corners = new Point2D[2];
      corners[0] = new Point2D.Double(ds.getXMin(), ds.getYMin());
      corners[1] = new Point2D.Double(ds.getXMax(), ds.getYMax());
      parentVP.userUnitsToThousandthInches(corners);
      if(!Utilities.isWellDefined(corners)) return;

      // retrieve color map properties from the parent graph
      double start = g.getColorBar().getStart();
      double end = g.getColorBar().getEnd();
      if(start > end)
      {
         double d = start; 
         start = end; 
         end = d;
      }
      else if(start == end) end = start + 1;
      
      float[] rng = new float[] { (float)start, (float)end };
      boolean isLog = g.getColorBar().isLogarithmic();
      
      ColorLUT colorLUT = g.getColorBar().getColorLUT();
      int rgbNaN = g.getColorBar().getColorNaN().getRGB() & 0x00FFFFFF;
      
      // this sets the PS document's current graphics state IAW this node's graphic styles
      psDoc.startElement(this);
      
      if(mode == DisplayMode.HEATMAP || mode == DisplayMode.CONTOUREDHEATMAP)
      {
         psDoc.renderIndexedImage(ds.getDataBreadth(), ds.getDataLength(), corners[0], corners[1], colorLUT, rgbNaN, 
               smoothed, ds.getIndexedImageDataIterator(256, rng, isLog));
      }
      
      if(mode != DisplayMode.HEATMAP)
      {
         // contour level lines are not drawn if stroke width is 0 or their color is transparent. If neither the 
         // level lines nor contour fill regions are rendered, then we're done!
         boolean strk = (getStrokeWidth() > 0) && (mode==DisplayMode.LEVELLINES || getStrokeColor().getAlpha() != 0);
         if((!strk) && mode != DisplayMode.FILLEDCONTOURS) return;
         
         generateContoursIfNecessary(false);
         
         List<List<Point2D>> paths = new ArrayList<List<Point2D>>();
         List<Boolean> closedFlags = new ArrayList<Boolean>();
         List<Integer> fillRGB = new ArrayList<Integer>();
         List<Integer> strkRGB = new ArrayList<Integer>();
         for(ContourGenerator.Contour c : contours) if(c.isFillRegion() || strk)
         {
            List<Point2D> pts = c.asPoints();
            parentVP.userUnitsToThousandthInches(pts);
            
            paths.add(pts);
            closedFlags.add(new Boolean(c.isClosed()));
            
            double level = c.getLevel();
            int mappedRGB = Utilities.isWellDefined(level) ? 
                  (0x00FFFFFF & colorLUT.mapValueToRGB(level, start, end, isLog)) : rgbNaN;
            
            if(c.isFillRegion())
            {
               fillRGB.add(new Integer(mappedRGB));
               strkRGB.add(new Integer(-1));
            }
            else
            {
               fillRGB.add(new Integer(-1));
               strkRGB.add(new Integer(mode == DisplayMode.LEVELLINES ? mappedRGB : -2));
            }
         }
         
         psDoc.renderPolyFills(paths, closedFlags, fillRGB, strkRGB);
      }
      
      psDoc.endElement();         
   }
  
   /**
    * This override ensures that the cloned contour plot node's internal rendering resources are completely independent
    * of the resources allocated to this node.
    */
   @Override protected Object clone()
   {
      ContourNode copy = (ContourNode) super.clone();
      copy.rBoundsSelf = null;
      copy.contours = null;
      copy.cachedBI = null;
      copy.isCachedBIValid = new AtomicBoolean(false);
      
      return(copy);
   }
   
   
   //
   // Internal rendering resources
   //

   /**
    * Cached rectangle bounding only the marks made by this contour plot. An empty rectangle indicates that 
    * the element makes no marks when "rendered". If null, the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;
   
   /** 
    * The current set of contour paths for this contour plot. If empty, the node's current state is such that there are
    * no contours (heat map-only display mode, empty data set). If null, then the set of contours needs to be generated.
    */
   private List<ContourGenerator.Contour> contours = null;
   
   /** 
    * Prepare the contours for this contour plot node, if necessary.
    * 
    * @param force If true, the contours are recalculated even if they have already been cached.
    */
   private void generateContoursIfNecessary(boolean force)
   {
      if(contours != null && !force) return;
      
      // in heat map-only mode, there are no contours
      if(mode == DisplayMode.HEATMAP)
      {
         contours = new ArrayList<ContourGenerator.Contour>();
         return;
      }
      
      ContourGenerator cgen = new ContourGenerator();
      IDataGrid grid = new Grid(getDataSet());
      double[] levels = Utilities.parseDoubleList(levelList, null, MAXLEVELS);
      cgen.setData(grid, levels);
      contours = cgen.generateContours(mode==DisplayMode.FILLEDCONTOURS, false);
   }
   
   /** 
    * Cached image rendering the current data matrix as a "heat map" using the current color map. As long as the data
    * source and the color map do not change, this image remains valid for use.
    */
   private BufferedImage cachedBI = null;
   
   /** 
    * Atomic flag indicates whether or not the cached heat map buffer image is valid for use. The flag must be cleared
    * whenever the data source or color map change, and it must be set immediately before preparing the image. The
    * image is always prepared during a render task, and we need to check whether it gets invalidated by user action
    * during a render cycle.
    */
   private AtomicBoolean isCachedBIValid = new AtomicBoolean(false);
   
   /**
    * Prepare and cache the "heat map image" representation of this contour node's source data set using the parent
    * graph's current color map. 
    * <p><b>IMPORTANT:</b> This method could take a significant amount of time to complete, especially if the underlying
    * data matrix is large. Be sure to execute only on a background thread, namely, during a render cycle. And call it 
    * only if the display mode requires the heat map, and the previously cached heat map image has been invalidated.</p>
    * 
    * @param gc Graphics configuration of rendering device, for creating a compatible image buffer.
    * @return True if successful, false otherwise.
    */
   private boolean createCachedHeatMap(GraphicsConfiguration gc)
   {
      FGNGraph g = getParentGraph();
      if(g == null) return(false);
      
      DataSet ds = getDataSet();
      int w = ds.getDataBreadth();
      int h = ds.getDataLength();
      if(cachedBI != null && (w != cachedBI.getWidth() || h != cachedBI.getHeight()))
      {
         cachedBI.flush(); 
         cachedBI = null;
      }
      if(w == 0 || h == 0) return(true);
      
      if(cachedBI == null)
      {
         if(gc == null) return(false);
         cachedBI = gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
      }
      
      // retrieve color map properties from the parent graph's color bar
      ColorBarNode cbar = g.getColorBar();
      double start = cbar.getStart();
      double end = cbar.getEnd();
      if(start > end)
      {
         double d = start; 
         start = end; 
         end = d;
      }
      else if(start == end) end = start + 1;
      
      float[] rng = new float[] { (float)start, (float)end };;
      int[] cmapLUT = cbar.getColorLUT().getLUT();
      cmapLUT[0] = cbar.getColorNaN().getRGB();   // replace LUT index 0 with the NaN color!

      return(ds.prepareImage(cachedBI, rng, cbar.isLogarithmic(), cmapLUT));      
   }
   
   /** 
    * Helper class wraps the contour plot node's data set as an {@link IDataGrid} for use with the {@link 
    * ContourGenerator}.
    * 
    * @author sruffner
    */
   private class Grid implements IDataGrid
   {
      /**
       * Construct a data grid object for the specified data set, which must have the {@link Fmt#XYZIMG} format.
       * @param src The data set.
       */
      Grid(DataSet src)
      {
         if(src == null || (src.getFormat() != Fmt.XYZIMG)) throw new IllegalArgumentException();
         
         this.src = src;
         float[] params = src.getParams();
         x0 = params[0];
         x1 = params[1];
         y0 = params[2];
         y1 = params[3];
      }
      
      @Override public int getNumRows() { return(src.getDataLength()); }
      @Override public int getNumCols() { return(src.getDataBreadth()); }
      @Override public double getX0() { return(x0); }
      @Override public double getX1() { return(x1); }
      @Override public double getY0() { return(y0); }
      @Override public double getY1() { return(y1); }
      @Override public double getMinZ() { return(src.getZMin()); }
      @Override public double getMaxZ() { return(src.getZMax()); }

      @Override public double getZ(int r, int c)
      {
         if(r < 0 || c < 0 || r>=getNumRows() || c>=getNumCols()) throw new IndexOutOfBoundsException();
         return(src.getZ(r*src.getDataBreadth() + c));
      }
      
      private DataSet src;
      private double x0, x1, y0, y1;
   }
}
