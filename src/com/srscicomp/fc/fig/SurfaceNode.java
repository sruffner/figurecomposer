package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.srscicomp.common.g2dutil.Projector;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;

/**
 * <b>SurfaceNode</b> is a <b><i>3D data presentation</i></b> element that renders a {@link Fmt#XYZIMG} data set
 * as a 3D mesh or surface plot. It may only be placed in the 3D graph container, {@link Graph3DNode}.
 * 
 * <p>The surface is rendered as a polygon mesh. For each value of (X,Y) in the data source and mesh cell dimensions 
 * (DX,DY), we compute the 3D vertices (X,Y,Z1), (X+DX,Y,Z2), (X+DX,Y+DY,Z3), and (X,Y+DY,Z4) from the data source. 
 * These are projected onto the parent 3D graph's 2D viewport using its 3D-to-2D projection, which generally results in
 * a 4-sided polygon in 2D. Each such polygon is stroked IAW the surface node's stroke properties (solid strokes only); 
 * if stroke width is 0, then this "mesh" is not rendered. The polygon fill depends on the {@link #isColorMapped()} 
 * property. If unset, then all polygons are filled IAW the node's inherited fill color; otherwise, each polygon is 
 * filled with the color that maps to its assigned Z-value, as determined by the parent graph's current color map. The 
 * Z-value of a mesh polygon is the average value of Z across its vertices (plus any "skipped" samples within the mesh 
 * cell due to sub-sampling to limit mesh count). Thus, you can render the surface as a "wire frame" mesh, a single
 * color or color-mapped surface (like a 3D heat map), or both.</p>
 * 
 * <p>The {@link Fmt#XYZIMG} data set is really an NxM matrix of Z-values. The set includes the range [X0..X1]
 * spanned by the data in X and the range [Y0..Y1] spanned in Y. Thus, DX=(X1-X0)/N and DY=(Y1-Y0)/M, and we can 
 * calculate the values of X and Y for each value of Z in the matrix. N is the mesh size in the X direction, while M is
 * the mesh size in the Y direction. As mesh size increases, the time it takes to render the surface increases 
 * dramatically. As a compromise between rendering speed and fidelity, the node includes an integer attribute setting
 * the maximum mesh size, S; its allowed range is [50..500]. As long as max(N,M) &le; S, the entire data set is plotted
 * without "sub-sampling". If, say N &gt; S, then we find the smallest integer k &ge; 2 such that N/k &le; S. Then the
 * mesh cell dimension in X will be DX*k. In computing the average Z value assigned to a mesh cell, the values at the
 * "skipped" vertices are included in the average, unless the value is NaN. An analogous approach applies to the Y 
 * dimension when M &gt; S.</p>
 * 
 * <p>What if the data set contains NaN values? As long as the Z value is well-defined at each of the 4 vertices of the
 * mesh cell, the projected polygon is rendered. If any of the 4 vertices is ill-defined, nothing is drawn for that mesh
 * cell -- leaving a "hole" in the surface.</p>
 * 
 * @author sruffner
 */
public class SurfaceNode extends FGNPlottableData implements Cloneable
{
   /**
    * Construct a 3D surface node initially configured to display an empty set as a color-mapped surface. The stroke
    * width is initially 0-in so that the wire-frame mesh is not drawn, and the mesh size limit is set to 100. All 
    * stroke properties (other than stroke width) are initially inherited; there are no font-related styles associated 
    * with this node. It has no title initially.
    */
   public SurfaceNode()
   {
      super(HASSTROKEATTRS|HASFILLCATTR|HASTITLEATTR);
      setTitle("");
      setMeasuredStrokeWidth(new Measure(0, Measure.Unit.IN));
      meshLimit = 100;
      colorMapped = true;
   }

   /**
    * When the parent graph's color map changes, this surface node's appearance is affected only if it is color-mapped. 
    * In that scenario, this method updates the polygon mesh accordingly.
    */
   @Override public boolean onColorMapChange() 
   { 
      if(!isColorMapped()) return(false);
      if(mesh != null) mesh.update();
      return(true);  
   }
   
   @Override public FGNodeType getNodeType() { return(FGNodeType.SURFACE); }


   //
   // Properties
   //
   
   /** Minimum allowed value for the surface plot's mesh size limit. */
   public static final int MIN_MESHLIMIT = 25;
   /** Maximum allowed value for the surface plot's mesh size limit. */
   public static final int MAX_MESHLIMIT = 500;
   
   /** The surface plot's mesh size limit. */
   private int meshLimit;
   
   /**
    * Get the surface plot's current mesh size limit. See {@link #setMeshLimit} for details.
    * @return The mesh size limit. Value is restricted to [{@link #MIN_MESHLIMIT} .. {@link #MAX_MESHLIMIT}].
    */
   public int getMeshLimit() { return(meshLimit); }

   /**
    * Set the mesh size limit for this surface plot. If a change is made, {@link #onNodeModified} is invoked.
    * 
    * <p>For performance reasons, the total number of mesh polygons making up a surface must be limited. The X-Y 
    * domain space of the data source can be thought of as an NxM rectangular grid, with N divisions in X and M in Y.
    * If all possible {X,Y,Z} points are included in the surface, a total of (N-1)x(M-1) mesh polygons must be drawn.
    * The mesh size limit S establishes a trade-off between rendering speed and fidelity. When N &gt; S, an integer K
    * must be chosen so that N/K &le; S; analogously if M &gt; S. Essentially, the surface plot is a sub-sampled 
    * version of the underlying data source, although all Z values within a mesh cell are included in the average that
    * determines the color assigned to that mesh cell.</p>
    * 
    * @param limit The desired mesh size limit. Must lie in [{@link #MIN_MESHLIMIT}..{@link #MAX_MESHLIMIT}].
    * @return True if successful; false if value was rejected.
    */
   public boolean setMeshLimit(int limit)
   {
      if(limit < MIN_MESHLIMIT || limit > MAX_MESHLIMIT) return(false);

      if(this.meshLimit != limit)
      {
         if(doMultiNodeEdit(FGNProperty.MESHLIMIT, limit)) return(true);
         
         Integer old = meshLimit;
         meshLimit = limit;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.MESHLIMIT);
            FGNRevEdit.post(this, FGNProperty.MESHLIMIT, meshLimit, old);
         }
      }
      return(true);
   }
   
   /**
    * If true, each mesh polygon on the surface is filled by the color that maps to its associated Z-value. Otherwise,
    * all mesh polygons are filled with the node's current fill color.
    */
   private boolean colorMapped;
   
   /**
    * Is this a a color-mapped or single-color surface? When color mapping is enabled, the Z-value associated with each
    * mesh polygon in the surface is mapped to a color through the parent graph's color map, and the polygon is filled
    * with that color. Else, all mesh polygons are filled with the same color as specified by {@link #getFillColor()}. 
    * <i>For a wire-frame mesh appearance, this flag should be false, and the fill color should be transparent</i>.
    * 
    * @return True if surface is color-mapped, false otherwise.
    */
   public boolean isColorMapped() { return(colorMapped); }

   /**
    * Enable or disable color-mapping of this surface. If a change is made, {@link #onNodeModified} is invoked.
    * 
    * @param b True for a color-mapped surface, false for single-color.
    */
   public void setColorMapped(boolean b)
   {
      if(colorMapped != b)
      {
         if(doMultiNodeEdit(FGNProperty.CMAP, b)) return;
         
         Boolean old = colorMapped;
         colorMapped = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CMAP);
            String desc = "Turn surface color-mapping " + (colorMapped ? "on" : "off");
            FGNRevEdit.post(this, FGNProperty.CMAP, colorMapped, old, desc);
         }
      }
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok;
      switch(p)
      {
         case MESHLIMIT: ok = setMeshLimit((Integer)propValue); break;
         case CMAP: setColorMapped((Boolean)propValue); ok = true; break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value;
      switch(p)
      {
         case MESHLIMIT: value = getMeshLimit(); break;
         case CMAP: value = isColorMapped(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }

   /** 
    * Changing the surface's fill color property has no effect on its rendering when the surface is color-mapped. This
    * override ensures that no rendering cycle is triggered in that case, while still informing the model of the 
    * change in the node's definition.
    */
   @Override protected void onNodeModified(Object hint)
   {
      FGNGraph g = getParentGraph();
      if(g == null) return;
      FGraphicModel model = getGraphicModel();
      if(model == null) return;
      
      if(hint == FGNProperty.FILLC && isColorMapped()) model.onChange(this, 0, false, null);
      else super.onNodeModified(hint);
   }
   
   //
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   /** The only node-specific style for the surface node is the color-mapped flag. */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.CMAP, isColorMapped());
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.CMAP, null, Boolean.class);
      if(b != null && !b.equals(restore.getStyle(FGNProperty.CMAP)))
      {
         setColorMapped(b);
         changed = true;
      }
      else restore.removeStyle(FGNProperty.CMAP);
      
      return(changed);
   }


   // 
   // FGNPlottableData, FGNPlottable
   //

   /**
    * Initializes the surface node's data set to the function z = S*(x-xMid)*sin(2*pi*(y-yMid)/yRange), where xMid and
    * yMid are the mid-points of the X and Y axis ranges and S = zRange/xRange. The resulting surface will span the
    * current X,Y and Z ranges of the parent 3D graph as specified by the <i>axisRng</i> argument. The source data set
    * has 50x50 samples.
    */
   @Override protected void initializeDefaultData(double[] axisRng, boolean isPolar)
   {
      float x0 = (float) axisRng[0];
      float x1 = (float) axisRng[1];
      float y0 = (float) axisRng[2];
      float y1 = (float) axisRng[3];
      float z0 = (float) axisRng[4];
      float z1 = (float) axisRng[5];
      
      float[] params = new float[] {x0, x1, y0, y1};
      float[] fData = new float[50*50];
      
      double xmid = (x0 + x1) / 2.0;
      double xscale = (x1 - x0) / 49.0;
      double ymid = (y0 + y1) / 2.0;
      double yscale = (y1 - y0) / 49.0;
      double omega = 2.0 * Math.PI/ Math.abs(y1-y0);
      double zscale = (z1-z0) / (x1-x0);
      double zmid = (z0 + z1) / 2.0;
      for(int i=0; i<50; i++)
      {
         for(int j=0; j<50; j++)
         {
            double x = params[0] + ((double) i) * xscale;
            double y = params[2] + ((double) j) * yscale;
            fData[j*50 + i] = (float) (zmid + zscale * (x-xmid) * Math.sin(omega*(y-ymid)));
         }
      }
      
      DataSet ds = DataSet.createDataSet(getDataSet().getID(), Fmt.XYZIMG, params, 50, 50, fData);
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

   @Override public boolean isSupportedDataFormat(Fmt fmt) { return(fmt == Fmt.XYZIMG); }

   /** The data formats that may be rendered by a surface node. */
   private final static Fmt[] supportedFormats = new Fmt[] {Fmt.XYZIMG};
   @Override public Fmt[] getSupportedDataFormats() { return(supportedFormats); }
   
   /** The surface plot must appear in a 3D graph container. */
   @Override public boolean is3D() { return(true); }
   
   /** There is no legend entry for a surface plot. */
   @Override public boolean isLegendEntrySupported() { return(false); }

   
   //
   // Focusable/Renderable support
   //
   
   /**
    * If the surface is color-mapped it will appear translucent if the mesh is not stroked, even though the mesh cells
    * are filled with opaque color. If it's not color-mapped, defer to the base class implementation.
    */
   @Override protected boolean isTranslucent()
   {
      if(isColorMapped()) return(!isStroked());
      return( super.isTranslucent());
   }

   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || !mesh.isInitialized()) mesh.update();
      return((Rectangle2D) mesh.rBoundsSelf.clone());
   }

   @Override protected void releaseRenderResourcesForSelf() { mesh.reset(); }

   @Override public boolean render(Graphics2D g2d, RenderTask task)
   {
      return(mesh.render(g2d, task));
   }

   
   //
   // PSTransformable
   //
   
   public void toPostscript(PSDoc psDoc)
   {
      FGNGraph g = getParentGraph();
      if(g == null) return;
      
      List<Point2D> verts = new ArrayList<>();
      List<Number> fillColors = new ArrayList<>();
      mesh.prepareMeshPolygonsForPSDoc(verts, fillColors);
      if(verts.isEmpty()) return;
      
      psDoc.startElement(this);
      boolean noFill = (!isColorMapped()) && getFillColor().getAlpha() == 0;
      psDoc.renderMesh(verts.size()/4, verts, noFill ? null : fillColors, null);
      psDoc.endElement();
      verts.clear();
      fillColors.clear();
   }
   
   
   // 
   // Object
   //

   /**
    * This override ensures that the rendering infrastructure for the clone is independent of the surface node that
    * was cloned. The clone will reference the same data set, however!
    */
   @Override protected SurfaceNode clone() throws CloneNotSupportedException
   {
      SurfaceNode copy = (SurfaceNode) super.clone();
      copy.mesh = new MeshGenerator();
      return(copy);
   }
   
   
   /** Delegate that handles computation and rendering of the surface mesh. */
   private MeshGenerator mesh = new MeshGenerator();
   
   /**
    * Helper class that builds the mesh of polygons that make up the surface when rendered on the 2D canvas. Rather
    * than maintain storage for all of the vertices, it computes them on the fly as needed. It handles three tasks:
    * the actual rendering of the surface; computing the rectangle that bounds the rendered surface; and generating
    * the sequence of vertices that are passed to the appropriate {@link PSDoc} method to render the surface in a
    * Postscript document.
    * @author sruffner
    */
   private class MeshGenerator
   {
      /** Is the surface mesh generator initialized? */
      boolean isInitialized() { return(vertices != null); }
      
      /** Reset the surface mesh generator. Must call {@link #update()} again before using the generator object. */
      void reset()
      {
         vertices = null;
         rBoundsSelf = null;
      }
      
      /**
       * Update the surface mesh generator. It stores some information internally that will be used to render the 
       * surface, and it calculates the rectangle that bounds the rendered surface WRT the viewport of the 3D graph 
       * container. <i>This method must be called whenever any property of the surface node changes that could affect 
       * its rendering, or when there is any change to the 3D coordinate system in which the surface is displayed.</i> 
       */
      void update()
      {
         if(vertices == null)
         {
            vertices = new Point2D[4];
            for(int i=0; i<4; i++) vertices[i] = new Point2D.Double();
            rBoundsSelf = new Rectangle2D.Double();
         }
         else
            rBoundsSelf.setFrame(0, 0, 0, 0);
         isRendered = false;
         
         Graph3DNode g3 = getParentGraph3D();
         if(g3 == null) return;
         prj = g3.get2DProjection();
         colorLUT = isColorMapped() ? g3.getColorBar().getColorLUT() : null;
         isLogCM = isColorMapped() && g3.isLogarithmicColorMap();
         cMapZRng = null;
         if(isColorMapped())
         {
            double start = g3.getColorBar().getStart();
            double end = g3.getColorBar().getEnd();
            if(start > end) { double t = start; start = end; end = t; }
            else if(start == end) end = start + 1;
            
            cMapZRng = new double[] { start, end };
         }
         
         ds = getDataSet();
         xyRange = ds.getParams();
         if(xyRange[0] == xyRange[1] || xyRange[2] == xyRange[3]) return;

         nColsX = ds.getDataBreadth();
         nRowsY = ds.getDataLength();
         boolean isFilled = isColorMapped() || (getFillColor().getAlpha() != 0);
         isRendered = (nColsX > 1) && (nRowsY > 1) && (isStroked() || isFilled);
         if(!isRendered) return;
         
         xMeshIntv = 1;
         while((nColsX / xMeshIntv) > meshLimit) ++xMeshIntv;
         yMeshIntv = 1;
         while((nRowsY / yMeshIntv) > meshLimit) ++yMeshIntv;
         
         // when X-axis is within +/-15 deg of being parallel to the projection screen, we should build the surface in
         // X strips (X is the inner loop variable) rather than Y strips to avoid artifacts.
         double rot = Math.abs(prj.getRotationAngle());
         doXStrips = (rot < 15) || (rot > 165);
         
         // to avoid occlusion problems, we must traverse the underlying data from "back" to "front" WRT the 3D coord
         // system. We may have to traverse backwards in X and/or Y...
         invX = ((prj.getFrontSideX()-prj.getBackSideX()) * (xyRange[1]-xyRange[0]) < 0);
         invY = ((prj.getFrontSideY()-prj.getBackSideY()) * (xyRange[3]-xyRange[2]) < 0);

         double xmin = Double.POSITIVE_INFINITY;
         double xmax = Double.NEGATIVE_INFINITY;
         double ymin = Double.POSITIVE_INFINITY;
         double ymax = Double.NEGATIVE_INFINITY;
         
         // traverse data in same direction as it would be rendered. This can make a difference when sub-sampling!
         int iDelta = (invX ? -1 : 1) * xMeshIntv;
         int jDelta = (invY ? -1 : 1) * yMeshIntv;
         
         if(doXStrips)
         {
            int j = invY ? nRowsY-1-yMeshIntv : 0;
            while(invY ? (j >= 0) : (j < nRowsY))
            {
               int i = invX ? nColsX-1-xMeshIntv : 0;
               while(invX ? (i >= 0) : (i < nColsX))
               {
                  double zAvg = calcMeshCell(i, j, invX, invY);
                  if(Utilities.isWellDefined(zAvg))
                  {
                     for(Point2D vertex : vertices)
                     {
                        double x = vertex.getX();
                        double y = vertex.getY();
                        if(x < xmin) xmin = x;
                        if(x > xmax) xmax = x;
                        if(y < ymin) ymin = y;
                        if(y > ymax) ymax = y;
                     }
                  }
                  i += iDelta;
               }
               j += jDelta;
            }
         }
         else
         {
            int i = invX ? nColsX-1-xMeshIntv : 0;
            while(invX ? (i >= 0) : (i < nColsX))
            {
               int j = invY ? nRowsY-1-yMeshIntv : 0;
               while(invY ? (j >= 0) : (j < nRowsY))
               {
                  double zAvg = calcMeshCell(i, j, invX, invY);
                  if(Utilities.isWellDefined(zAvg))
                  {
                     for(Point2D vertex : vertices)
                     {
                        double x = vertex.getX();
                        double y = vertex.getY();
                        if(x < xmin) xmin = x;
                        if(x > xmax) xmax = x;
                        if(y < ymin) ymin = y;
                        if(y > ymax) ymax = y;
                     }
                  }
                  j += jDelta;
               }
               i += iDelta;
            }           
         }
         
         if(xmin < xmax && ymin < ymax)
         {
            double sw = getStrokeWidth();
            rBoundsSelf.setFrame(xmin-sw/2.0, ymin-sw/2.0, xmax-xmin+sw, ymax-ymin+sw);
         }
         else
            isRendered = false;
      }
      
      /**
       * Calculate the four vertices of a single mesh polygon comprising the rendered surface. 
       * <p>The underlying {@link Fmt#XYZIMG XYZIMG} data source may be thought of as a NxM matrix of Z values
       * evaluated over a rectangular grid R=[0..N-1], C=[0..M-1], where the actual values of X and Y are computed from
       * the matrix row and column indices given the X and Y data ranges attached to the data source. To calculate the
       * mesh polygon at (C,R), the method uses the data set to calculate the 3D points corresponding to the matrix
       * elements (C,R), (C+1,R), (C+1,R+1), and (C,R+1). These four 3D points are then projected onto the parent 3D
       * graph's viewport to yield the 4 vertices of the mesh polygon, and the Z-value assigned to that polygon is the
       * average of the Z values at the 4 vertices.</p>
       * <p>Complications arise because the underlying matrix may be sub-sampled in either or both directions because
       * N or M is greater than the surface's current mesh size limit. Also, because it is very important to render
       * the polygons from "back" to "front" WRT the 3D graph's XYZ coordinate system, either or both matrix indices
       * may be traversed in the reverse direction. Thus, the "+1" could be replaced by "-1", "+2", etc, and when the
       * matrix is sub-sampled, all well-defined Z-values in the mesh cell are included in the calculation of the 
       * average Z value assigned to the mesh polygon. Note that all 4 vertices of the mesh polygon must be well
       * defined, or the polygon is not rendered -- creating a "hole" in the surface.</p>
       * <p><i>Note that this method uses various internal member variables that are initialized before iterating over
       * the data matrix, and the mesh polygon vertices are stored in another internal member.</i></p> 
       * 
       * @param c The matrix column index (corresponding to the X-coordinate).
       * @param r The matrix row index (corresponding to the Y-coordinate).
       * @param invC True if matrix columns are being traversed in inverse order, from last to first.
       * @param invR True if matrix rows are being traversed in inverse order, from last to first.
       * @return The Z-value assigned to the mesh polygon just calculated. If the polygon could not be calculated for
       * any reason, NaN is returned.
       */
      double calcMeshCell(int c, int r, boolean invC, boolean invR)
      {
         int c2, r2;
         if(invC)
         {
            c2 = c - xMeshIntv;
            if(c2 < 0) c2 = 0;
         }
         else
         {
            c2 = c + xMeshIntv;
            if(c2 >= nColsX) c2 = nColsX-1;
         }
         if(invR)
         {
            r2 = r - yMeshIntv;
            if(r2 < 0) r2 = 0;
         }
         else
         {
            r2 = r + yMeshIntv;
            if(r2 >= nRowsY) r2 = nRowsY-1; 
         }
         
         if(c==c2 || r==r2) return(Double.NaN);
         
         
         double xScale = (xyRange[1] - xyRange[0]) / ((double) (nColsX-1));
         double yScale = (xyRange[3] - xyRange[2]) / ((double) (nRowsY-1));
         double zSum = 0;
         
         double x = xyRange[0] + ((double) c) * xScale;
         double y = xyRange[2] + ((double) r) * yScale;         
         double z = ds.getZ(r*nColsX + c);
         prj.project(x, y, z, vertices[0]);
         if(!Utilities.isWellDefined(vertices[0])) return(Double.NaN);
         zSum += z;

         x = xyRange[0] + ((double) c2) * xScale;
         y = xyRange[2] + ((double) r) * yScale;
         z = ds.getZ(r*nColsX + c2);
         prj.project(x, y, z, vertices[1]);
         if(!Utilities.isWellDefined(vertices[1])) return(Double.NaN);
         zSum += z;
        
         x = xyRange[0] + ((double) c2) * xScale;
         y = xyRange[2] + ((double) r2) * yScale;
         z = ds.getZ(r2*nColsX + c2);
         prj.project(x, y, z, vertices[2]);
         if(!Utilities.isWellDefined(vertices[2])) return(Double.NaN);
         zSum += z;
         
         x = xyRange[0] + ((double) c) * xScale;
         y = xyRange[2] + ((double) r2) * yScale;
         z = ds.getZ(r2*nColsX + c);
         prj.project(x, y, z, vertices[3]);
         if(!Utilities.isWellDefined(vertices[3])) return(Double.NaN);
         zSum += z;
         
          // typically, expect the mesh interval to be 1 in both X and Y, in which case we're ready to calc avg Z
         if(xMeshIntv == 1 && yMeshIntv == 1)
            return(zSum/4.0);
         
         // otherwise, find the average Z-value over all well-defined (X,Y,Z) points in the mesh cell
         zSum = 0;
         int n = 0;
         for(int i=(invC ? c2 : c); i<=(invC ? c : c2); i++)
         {
            for(int j=(invR ? r2 : r); j<=(invR ? r : r2); j++)
            {
               z = ds.getZ(j*nColsX + i);
               if(Utilities.isWellDefined(z))
               {
                  zSum += z;
                  ++n;
               }
            }
         }
         return(zSum/((double) n));
      }
      
      boolean render(Graphics2D g2d, RenderTask task)
      {
         if(!isInitialized()) update();
         if(!(needsRendering(task) && isRendered)) return(true);

         GeneralPath meshPoly = new GeneralPath();
         boolean stroked = isStroked();
         Color fillC = getFillColor();
         boolean isCMapped = (colorLUT != null);
         boolean filled = isCMapped || (fillC.getAlpha() != 0);
         if(stroked) g2d.setStroke(getStroke(0));
         

         int iDelta = (invX ? -1 : 1) * xMeshIntv;
         int jDelta = (invY ? -1 : 1) * yMeshIntv;
         
         if(doXStrips)
         {
            int j = invY ? nRowsY-1-yMeshIntv : 0;
            while(invY ? (j >= 0) : (j < nRowsY))
            {
               int i = invX ? nColsX-1-xMeshIntv : 0;
               while(invX ? (i >= 0) : (i < nColsX))
               {
                  double zAvg = calcMeshCell(i, j, invX, invY);
                  if(Utilities.isWellDefined(zAvg))
                  {
                     meshPoly.reset();
                     meshPoly.moveTo(vertices[0].getX(), vertices[0].getY());
                     meshPoly.lineTo(vertices[1].getX(), vertices[1].getY());
                     meshPoly.lineTo(vertices[2].getX(), vertices[2].getY());
                     meshPoly.lineTo(vertices[3].getX(), vertices[3].getY());
                     meshPoly.closePath();
                     
                     if(filled)
                     {
                        if(isCMapped) fillC = new Color(colorLUT.mapValueToRGB(zAvg,cMapZRng[0],cMapZRng[1],isLogCM));
                        g2d.setColor(fillC);
                        g2d.fill(meshPoly);
                     }
                     if(stroked)
                     {
                        g2d.setColor(getStrokeColor());
                        g2d.draw(meshPoly);
                     }
                  }
                  i += iDelta;
               }
               
               j += jDelta;
            }            
         }
         else
         {
            int i = invX ? nColsX-1-xMeshIntv : 0;
            while(invX ? (i >= 0) : (i < nColsX))
            {
               int j = invY ? nRowsY-1-yMeshIntv : 0;
               while(invY ? (j >= 0) : (j < nRowsY))
               {
                  double zAvg = calcMeshCell(i, j, invX, invY);
                  if(Utilities.isWellDefined(zAvg))
                  {
                     meshPoly.reset();
                     meshPoly.moveTo(vertices[0].getX(), vertices[0].getY());
                     meshPoly.lineTo(vertices[1].getX(), vertices[1].getY());
                     meshPoly.lineTo(vertices[2].getX(), vertices[2].getY());
                     meshPoly.lineTo(vertices[3].getX(), vertices[3].getY());
                     meshPoly.closePath();
                     
                     if(filled)
                     {
                        if(isCMapped) fillC = new Color(colorLUT.mapValueToRGB(zAvg,cMapZRng[0],cMapZRng[1],isLogCM));
                        g2d.setColor(fillC);
                        g2d.fill(meshPoly);
                     }
                     if(stroked)
                     {
                        g2d.setColor(getStrokeColor());
                        g2d.draw(meshPoly);
                     }
                  }
                  j += jDelta;
               }
               
               i += iDelta;
            }
         }
         
         return(true);
      }
      
      /**
       * Prepare the representation of the surface mesh polygons needed to render the surface in the Postscript 
       * document via {@link PSDoc}.
       * 
       * @param verts On return, contains the 4*N vertices of the N mesh polygons comprising the surface as projected
       * onto the parent graph's 2D viewport. List will be empty if surface is not rendered.
       * @param fillColors Ignored if surface is not color-mapped. Else, contains the N fill colors for the N mesh
       * polygons; each color is in packed RGB format, 0x00RRGGBB.
       */
      void prepareMeshPolygonsForPSDoc(List<Point2D> verts, List<Number> fillColors)
      {
         if(!isInitialized()) update();
         verts.clear();
         boolean isCMapped = (colorLUT != null);
         if(isCMapped) fillColors.clear();
         if(!isRendered) return;
         
         int iDelta = (invX ? -1 : 1) * xMeshIntv;
         int jDelta = (invY ? -1 : 1) * yMeshIntv;
         
         if(doXStrips)
         {
            int j = invY ? nRowsY-1-yMeshIntv : 0;
            while(invY ? (j >= 0) : (j < nRowsY))
            {
               int i = invX ? nColsX-1-xMeshIntv : 0;
               while(invX ? (i >= 0) : (i < nColsX))
               {
                  double zAvg = calcMeshCell(i, j, invX, invY);
                  if(Utilities.isWellDefined(zAvg))
                  {
                     verts.add(new Point2D.Double(vertices[0].getX(), vertices[0].getY()));
                     verts.add(new Point2D.Double(vertices[1].getX(), vertices[1].getY()));
                     verts.add(new Point2D.Double(vertices[2].getX(), vertices[2].getY()));
                     verts.add(new Point2D.Double(vertices[3].getX(), vertices[3].getY()));
                     
                     if(isCMapped) 
                        fillColors.add(colorLUT.mapValueToRGB(zAvg, cMapZRng[0], cMapZRng[1], isLogCM));
                  }
                  i += iDelta;
               }
               
               j += jDelta;
            }            
         }
         else
         {
            int i = invX ? nColsX-1-xMeshIntv : 0;
            while(invX ? (i >= 0) : (i < nColsX))
            {
               int j = invY ? nRowsY-1-yMeshIntv : 0;
               while(invY ? (j >= 0) : (j < nRowsY))
               {
                  double zAvg = calcMeshCell(i, j, invX, invY);
                  if(Utilities.isWellDefined(zAvg))
                  {
                     verts.add(new Point2D.Double(vertices[0].getX(), vertices[0].getY()));
                     verts.add(new Point2D.Double(vertices[1].getX(), vertices[1].getY()));
                     verts.add(new Point2D.Double(vertices[2].getX(), vertices[2].getY()));
                     verts.add(new Point2D.Double(vertices[3].getX(), vertices[3].getY()));
                     
                     if(isCMapped) 
                        fillColors.add(colorLUT.mapValueToRGB(zAvg, cMapZRng[0], cMapZRng[1], isLogCM));
                  }
                  j += jDelta;
               }
               
               i += iDelta;
            }
         }
      }
      
      /** Rectangle bounding the rendered surface, computed WRT parent 3D graph's viewport. Null if not initialized. */
      Rectangle2D rBoundsSelf = null;
      
      /** Mesh sample interval over the columns (X) of the data source matrix. Typically 1. */
      int xMeshIntv = 0;
      /** Mesh sample interval over the rows (Y) of the data source matrix. Typically 1. */
      int yMeshIntv = 0;
      /** The current X and Y data ranges attached to the data source matrix: [X0 X1 Y0 Y1]. */
      float[] xyRange = null; 
      /** The number of rows in the data source matrix. */
      int nRowsY = 0;
      /** The number of columns in the data source matrix. */
      int nColsX = 0;
      /** If true. data source matrix is traversed column-wise (X) rather than row-wise (Y) when rendering surface. */
      boolean doXStrips = false;
      /** True if data source columns (X) must be traversed from last to first when rendering surface. */
      boolean invX = false;
      /** True if data source rows (Y) must be traversed from last to first when rendering surface. */
      boolean invY = false;
      
      /** 
       * The data source. Maintain a reference here in case the user changes the surface node's data source during a
       * render cycle.
       */
      DataSet ds = null;
      /**
       * The parent graph's 3D-to-2D projection, needed to project the surface mesh vertices onto the graph's 2D
       * viewport. Maintain a reference here in case user action changes the projection during a render cycle.
       */
      Projector prj = null;
      /** If surface is color-mapped, the parent 3D graph's color lookup table; else null. */
      ColorLUT colorLUT = null;
      /** 
       * The Z data range mapped to the graph's color map (this is not necessarily the same as the data set's
       * Z range or the graph's Z axis range!). Null if surface is not color mapped.
       */
      double[] cMapZRng = null;
      /** Flag set if parent graph's color map is scaled logarithmically instead of linearly. */
      boolean isLogCM = false;
      
      /** True if surface is rendered; else false. */
      boolean isRendered = false;
      
      /** 
       * The four vertices of the last surface mesh polygon calculated, in 3D graph viewport coordinates. Will be null
       * if mesh generator is not yet initialized.
       */
      Point2D[] vertices = null;
   }
}
