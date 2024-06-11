package com.srscicomp.common.util;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


/**
 * <b>ContourGenerator</b> is a utility class that generates contour paths and filled contour bands for a rectangular
 * matrix of numerical data, Z = M(X,Y). This matrix represents a regular sampling of measured data or a function in X-Y
 * space.
 *
 * <p>Contour iso-lines are found using the "marching squares" algorithm, which is more fully explained
 * <a href="https://en.wikipedia.org/wiki/Marching_squares">here</a>. We adopted some of the implementation strategies
 * from <a href="https://udel.edu/~mm/code/marchingSquares">here</a>, but did not structure the code for
 * parallelization. To speed up the search for a new contour path segment, we used a hash map of the contour cells,
 * only included non-trivial cells in the map, and removed a cell once the contour path(s) within it had been traversed.
 * It took less than 200ms to generate 11 contours for a 500x500 data grid.</p>
 *
 * <p>Finding the filled contour regions was a significantly more difficult task in general, especially when the data
 * grid contains any undefined or "NaN" data. We employ a common "trick" to handle both NaNs and the task of closing
 * contour bands at the edges of the grid. All NaNs are replaced by a value L = Z0 - 1000*(Z1-Z0), where [Z0..Z1] is the
 * true range of Z in the data grid. In addition, single data points with Z=L are added all around the edges of the
 * original data grid. When contour iso-lines are computed under these conditions, all of them will be closed, and it is
 * a relatively simple task to construct the corresponding contour fill region and assign it to the appropriate fill
 * level.</p>
 *
 * <p>To generate contours for a particular data matrix, create an instance of the contour generator and specify the
 * data and the desired contour levels by calling {@link #setData(IDataGrid, double[])}. Then call {@link
 * #generateContours(boolean, boolean)} to produce the contour paths. You can elect to generate the unfilled contour
 * iso-lines, or both the iso-lines and the filled bands into which the iso-lines divide the data grid. When the filled
 * regions are generated, you can also choose to include the NaN regions so that they may be filled with a unique color
 * to distinguish them from the normal contour fill regions, or you can specify that all such NaN regions remain
 * transparent -- leaving see-through "holes" in the contour plot when it is rendered.</p>
 *
 * @author sruffner
 */
public class ContourGenerator
{
   /** The maximum number of contours that will be generated. */
   public static final int MAXCONTOURS = 20;
   
   /**
    * Create an empty contour generator. It lacks a data grid, so it computes no contour paths. Call {@link
    * #setData(IDataGrid, double[])} to specify the data grid and desired contour levels.
    */
   public ContourGenerator() {}
   
   /**
    * Set the data grid and contour level list for this contour generator.
    * @param dataGrid The data grid to be contoured. If null, no contours are generated.
    * @param vals The list of desired contour level values. If null or empty, the levels are selected automatically. 
    * Else, any repeat values in the list are ignored, as are any values that are outside the range of the grid data. 
    * Only the first {@link #MAXCONTOURS} will be used, as this is the maximum number of contours generated.
    */
   public void setData(IDataGrid dataGrid, double[] vals)
   {
      grid = new DGrid(dataGrid);
      chooseLevels(vals);
   }
   
   /**
    * Get the list of levels for which contours are generated. These may have been auto-selected or set manually (and
    * auto-corrected).
    * @return The list of contour levels, in ascending order. Will be empty if the data grid is such that no contours 
    * can be generated.
    */
   public double[] getContourLevels() 
   { 
      double[] out = new double[levels.size()];
      for(int i=0; i<out.length; i++) out[i] = levels.get(i);
      return(out);
   }
   
   /** A wrapper for the source data grid; initially, it behaves like a 0x0 grid. */
   private DGrid grid = new DGrid(null);
   
   /** The list of data levels for which contours are generated. */
   private List<Double> levels = new ArrayList<>();
   
   /**
    * Helper method prepares the list of data levels for which contours will be generated. Unless a list of contour
    * levels is provided, the method follows these steps for choosing the contour levels. Let the actual data range be
    * [Z0..Z1], with Z0 < Z1.
    * <ul>
    * <li>Calculate R=Z1-Z0, then Z0' = Z0 + 0.1*R, Z1' = Z1 - 0.1*R.</li>
    * <li>If there are not at least 2 integer values in [Z0'..Z1'], then choose Z0 + (0.2, 0.4, 0.6, 0.8)*R.</li>
    * <li>Otherwise, choose up to 10 integer values in [Z0'..Z1'], evenly distributed over that range.</li>
    * </ul>
    * 
    * @param vals Provided list of contour levels. If not null or empty, this list will be used -- but any out-of-range 
    * or repeat values are ignored. Also, no more than {@link #MAXCONTOURS} levels are accepted.
    */
   private void chooseLevels(double[] vals)
   {
      levels = new ArrayList<>();
      
      // first, a sanity check on the data grid. If grid is not at least 2x2 or does not contain varying Z data, there 
      // are no contours to generate.
      if(grid.getNumRows() < 2 || grid.getNumCols() < 2 || (!Utilities.isWellDefined(grid.getMinZ())) || 
            (!Utilities.isWellDefined(grid.getMaxZ())) || grid.getMinZ() == grid.getMaxZ())
         return;
      
      if(vals == null || vals.length == 0)
      {
         // auto-select levels
         double r = grid.getMaxZ() - grid.getMinZ();
         double z0 = grid.getMinZ() + 0.1*r;
         double z1 = grid.getMaxZ() - 0.1*r;
         int lo = (int) Math.ceil(z0);
         int hi = (int) Math.floor(z1);
         
         if(lo >= hi)
         {
            for(int i=1; i<= 4; i++) levels.add(grid.getMinZ() + i*0.2*r);
         }
         else
         {
            int delta = (hi - lo) / (hi-lo <= 20 ? 5 : 10);
            if(delta < 1) delta = 1;
            for(int i=lo; i<=hi; i+=delta) levels.add((double) i);
         }
      }
      else
      {
         for(int i=0; i<vals.length && levels.size() < MAXCONTOURS; i++)
         {
            double level = vals[i];
            if(grid.getMinZ() <= level && level <= grid.getMaxZ() && !levels.contains(level))
               levels.add(level);
         }
      }
      
      // sort the contour levels in ascending order
      Collections.sort(levels);
   }
   
   /**
    * Generate the contours for the current data matrix and contour level list. If the data matrix has not been 
    * specified or is such that no contours can be generated, the method will return an empty contour list.
    * 
    * <p>The returned contours are listed in the order in which they should be rendered in a contour plot of the data 
    * matrix. Path geometry is specified in the (x,y)-coordinates of that matrix. A contour path comes in one of two 
    * flavors: an iso-line that follows the path of a single contour level over the data matrix, or a filled region 
    * bounded by two consecutive contour levels (but it can be considerably more complex, if it touches any part of the
    * grid boundary or any NaN regions). Each contour object includes the contour level assigned to it, a flag 
    * indicating whether or not it is a filled contour, and a flag indicating whether or not the path is closed (filled 
    * contours are always closed). A filled contour surrounding data below the smallest contour level specified is 
    * assigned the level Z0, the minimum observed value of Z across the supplied data matrix. If the filled contour 
    * surrounds a region of undefined data, its contour level is set to {@link Double#NaN}.</p>
    * 
    * <p>If fill regions are not requested, the method returns only the set of unfilled contour iso-lines, in no 
    * particular order (they never cross!). If fill regions are requested, then the list of contours returned include
    * the fill regions first, ordered to ensure that no fill region contains or overlaps another fill region listed
    * before it. It is therefore imperative that the contours be rendered in the order listed.</p>
    * 
    * <p>If fill regions are requested AND the current data matrix contains any undefined data points, then the list
    * will include some NaN contour fills. These, of course, should be filled with a color that distinguishes the NaN
    * regions from all other contour regions. However, if the <i>transpaentNanRegions</i> flag is set, no NaN contour 
    * fills are included in the output. Instead, all contour fill paths are modified as needed to ensure that no drawing 
    * occurs in the NaN regions.</p>
    * 
    * @param includeFillRegions If true, the output will include all contour fill regions as well as the unfilled
    * contour iso-lines, in the order that the contours must be rendered to ensure no filled region obscures another. 
    * Otherwise, only the unfilled contours are included.
    * @param transparentNanRegions If the current data matrix contains at least one undefined ("NaN") data point, then
    * there will be at least one NaN contour fill in the output. All such NaN contour fills should be filled with a 
    * unique color to distinguish them from normal contour fills. However, if this flag is set, those NaN regions are
    * omitted from the output, and the normal contour fills are adjusted to ensure that no drawing occurs in any NaN
    * regions. This is useful if you want something to show through any "holes" in the rendered contour plot. The flag
    * is ignored if all data is well-defined.
    * @return List of all contours generated, in rendering order.
    */
   public List<Contour> generateContours(boolean includeFillRegions, boolean transparentNanRegions)
   {
      // construct a list of all contour cell iso-line paths -- including the NaN boundary contours, which isolate NaNs 
      // in the data grid from well-defined data.
      List<IsoLine> isoLines = prepareContourCellIsoLines();
      
      // construct list of stroked, but not filled, contours. Order is irrelevant. Don't include NaN contours, but check
      // to see if there are any unclosed NaN iso-lines, which must terminate on the grid border. 
      boolean hasNaNEdgeRegions = false;
      List<Contour> stroked = new ArrayList<>();
      for(IsoLine iso : isoLines)
      {
         if(!iso.isNaNBoundaryPath())
         { 
            Contour c = new Contour(iso.getContourPathPoints(false), iso.getContourLevelValue(), false, iso.isClosed());
            stroked.add(c);
         }
         else if(!iso.isClosed()) 
            hasNaNEdgeRegions = true;
      }
      
      // if filled contours are not requested, we're done! We're also done if there are NO contour iso-lines at all.
      if(stroked.isEmpty() || !includeFillRegions) return(stroked);
      
      // if filled contours are requested, we employ a trick: Temporarily modify the data grid so that any NaNs in it
      // are replaced by a value L that is orders of magnitude less than the actual minimum Z value, Zmin, in the grid. 
      // In addition, a border of data points of value L are added around all sides of the grid, and one additional 
      // contour level is added at Z=Zmin. As a result, when the contour iso-lines are calculated, ALL of them will be
      // closed, even along the grid edges and around NaN regions.
      double zMin = grid.getMinZ();
      boolean addZMin = !levels.contains(zMin);
      if(addZMin) levels.add(0, zMin);
      grid.setAllowNaNs(false);
      isoLines.clear();
      isoLines = prepareContourCellIsoLines();
      
      List<Contour> fills = new ArrayList<>();
      for(IsoLine iso : isoLines) fills.add(iso.asFilledContour());
      
      // for each filled contour region, find the other regions that contain it. 
      List<List<Integer>> containers = new ArrayList<>();
      for(int i=0; i<fills.size(); i++)
      {
         List<Integer> containedBy = new ArrayList<>();
         for(int j=0; j<fills.size(); j++) if(i != j && fills.get(j).contains(fills.get(i)))
            containedBy.add(j);
         containers.add(containedBy);
      }
      
      // using the "container" lists, re-order the filled contour regions so that no region contains a region appearing 
      // BEFORE it in that list. First, we find the regions that have no regions containing them and list them first. 
      // Then remove those regions from the container lists and repeat until we're done.
      List<Contour> out = new ArrayList<>();
      while(out.size() < fills.size())
      {
         for(int i=0; i<fills.size(); i++) if(containers.get(i) != null && containers.get(i).isEmpty())
         {
            out.add(fills.get(i));
             for (List<Integer> container : containers)
                 if (container != null) {
                     container.remove(Integer.valueOf(i));
                 }
            containers.set(i, null);
         }
      }
      
      // restore normal operation once all fill contours have been computed
      if(addZMin) levels.remove(0);
      grid.setAllowNaNs(true);
      
      // if there were any NaN edge regions, those regions will NOT be among the filled contours just computed. To 
      // ensure those regions are covered, we employ another trick. We create an NaN contour that outlines the grid
      // rectangle and add it as the first filled contour -- all the other filled regions are drawn on top of it. We
      // don't bother if NaN regions should be transparent.
      if(hasNaNEdgeRegions && !transparentNanRegions)
      {
         List<Point2D> pts = new ArrayList<>();
         pts.add(new Point2D.Double(grid.getX0(), grid.getY0()));
         pts.add(new Point2D.Double(grid.getX1(), grid.getY0()));
         pts.add(new Point2D.Double(grid.getX1(), grid.getY1()));
         pts.add(new Point2D.Double(grid.getX0(), grid.getY1()));
         out.add(0, new Contour(pts, Double.NaN, true, true));
      }

      // if NaN regions should be transparent, remove all interior NaN regions from the output, and exclude these
      // regions from all remaining filled contours.
      if(transparentNanRegions)
      {
         int i=0;
         List<Contour> nanRegions = new ArrayList<>();
         while(i < out.size())
         {
            if(!Utilities.isWellDefined(out.get(i).getLevel()))
               nanRegions.add(out.remove(i));
            else ++i;
         }
         
         for(Contour c : out)
         {
            for(Contour nanC : nanRegions) c.excerptFromPath(nanC);
         }
      }
      
      // finally, append the stroked contours so they're drawn over the fill regions
      out.addAll(stroked);
      
      return(out);
   }
   
   /**
    * Helper method for {@link #generateContours(boolean, boolean)} . Given the data grid and contour level list, the
    * method finds all contour cell "iso-lines" that exist on the grid, including any "NaN iso-lines" that bound
    * regions of undefined data in the grid.
    * 
    * <p>ALGORITHM:
    * <ul>
    * <li>Prepare a hash map of all cells in the contour grid that are non-trivial. The contour grid has one less row 
    * and one less column than the data grid upon which it is based, since a contour cell is defined by the data values 
    * at the cell's four corners. A contour cell is non-trivial if it is touched by at least one contour path segment 
    * (including any "NaN contour" segment). Typically, only a small fraction of the cells in the contour grid will be 
    * non-trivial, and typically those will contain only one path. The cell row and column indices (R,C) are used to 
    * compute the hash key: K = R*numCols + C, where numCols is the number of columns in the contour cell grid, one less
    * than in the underlying data grid.</li>
    * <li>Search the hash map for a "seed cell" containing an "untraced" contour iso-line, then follow the path CW to
    * where it "starts": a grid edge, an NaN cell, or if the path is closed, the seed cell itself. Then traverse the 
    * entire path counterclockwise from start to finish (it may end on itself, a grid edge, or an NaN cell), 
    * constructing the list of contour cells visited in CCW order, and determining the termination conditions at either 
    * end of the path if it is not closed. Each cell on the path is marked as having been traversed at a particular
    * contour level. Once a contour cell has no remaining "untraced" iso-lines, it is removed from the hash map. This
    * process repeats with a new seed cell until the hash map is empty.</li>
    * </ul>
    * </p>
    *
    * @return The list of all contour cell iso-line paths that exist on the underlying data grid. <i><b>If the above
    * algorithm "breaks" on some yet-untested data grid scenario, the method will return an empty list.</b></i>
    */
   private List<IsoLine> prepareContourCellIsoLines()
   {
      List<IsoLine> isoLines = new ArrayList<>();
      if(levels.isEmpty()) return(isoLines);
      
      // map of contour grid cells, using cell row and column indices (R,C) to compute key: K = R*numCols + C. Only
      // non-trivial cells -- those that are traversed by at least one contour iso-line -- are included.
      HashMap<Integer, Cell> contourCellMap = new HashMap<>();
      for(int r = 0; r < grid.getNumRows()-1; r++)
      {
         for(int c=0; c < grid.getNumCols()-1; c++)
         {
            Cell gridCell = new Cell(r,c);
            if(!gridCell.hasNoContours())
               contourCellMap.put(r * (grid.getNumCols() - 1) + c, gridCell);
         }
      }
      
      // if the algorithm breaks, an exception is thrown and caught. In this scenario, the contour list will be empty.
      try
      {
         while(!contourCellMap.isEmpty())
         {
            // search contour cell map for a cell traversed by a contour iso-line (that hasn't been visited yet)
            Cell seed = null;
            int level = -1;
            for(Integer key : contourCellMap.keySet())
            {
               Cell c = contourCellMap.get(key);
               for(int i=0; i<levels.size()+1; i++) if(!(c.hasNoContourFor(i) || c.isSaddleFor(i)))
               {
                  seed = c;
                  level = i;
                  break;
               }
            }
            
            // if we don't find a seed cell, then all that remains are saddle cells. These remaining cells are ignored.
            // Hopefully this is a very rare circumstance!
            if(seed == null) break;
            
            // construct the contour cell iso-line path
            Cell nanStart = null;
            Cell nanEnd = null;
            List<Cell> cellPath = new ArrayList<>();
            
            int nRows = grid.getNumRows()-1;  // num rows in contour cell grid (one less than data grid)
            int nCols = grid.getNumCols()-1;  // num columns in contour cell grid (one less than data grid)
            
            // from seed cell, follow contour path segment CLOCKWISE back to where it starts: a grid edge, NaN boundary, 
            // or if the path is closed, we'll return to the seed cell itself. We don't mark cells as visited nor save
            // them in a list. 
            Cell start = seed;
            int startRow = start.getRowIndex();
            int startCol = start.getColIndex();
            Edge exit;
            Edge enter = start.getEntranceEdgeForContour(level, null);
            int row = startRow;
            int col = startCol;
            while(true)
            {
               // based on entrance edge from current contour cell, find row-col location and exit edge for the next.
               switch(enter)
               {
               case LEFT : 
                  col--; 
                  exit = Edge.RIGHT; 
                  break;
               case RIGHT :
                  col++;
                  exit = Edge.LEFT;
                  break;
               case TOP :
                  row++;
                  exit = Edge.BOTTOM;
                  break;
               case BOTTOM :
                  row--;
                  exit = Edge.TOP;
                  break;
               default:
                  throw new IllegalStateException("Exit edge undefined for CW traversal - prog error!");
               }
               
               // get next cell in CLOCKWISE traversal. Check for termination condition: wandered outside grid, returned
               // to starting cell, or fell into a NaN "hole" in contour grid.
               Integer key = row * nCols + col;
               Cell next = contourCellMap.get(key);
               boolean done = (row < 0) || (row>=nRows) || (col<0) || (col>=nCols) || (row==startRow && col==startCol);
               
               // path must "emerge from" an NaN boundary cell if the next cell does not exist in the contour cell map 
               // (an NaN boundary cell that has already been visited and removed from the map), or the next cell exists
               // but does not contain the non-NaN contour level currently being traversed. This next cell MUST be an 
               // NaN boundary cell. We remember the cell but do not include it in the cell path list.
               if((!done) && (next == null || (level != levels.size() && next.hasNoContourFor(level))))
               {
                  nanStart = new Cell(row, col);
                  if(!nanStart.isNaNBoundaryCell()) 
                     throw new IllegalStateException("Invalid path termination in CW traversal - prog error!");
                  done = true;
               }
               if(done) break;
               
               // follow path backwards (clockwise) to next cell location
               enter = next.getEntranceEdgeForContour(level, exit);
               start = next;
            }
            
            // path is closed if we ended up back where we started.
            boolean closed = (row==startRow && col==startCol);
            
            // now trace the path counterclockwise, marking the cells visited and adding them to the list of cells that
            // comprise the path. If the path is closed, start on the seed cell, which we know is not a saddle. It is 
            // possible that the cell we end up on during CW traversal of an unclosed path is a saddle cell adjacent to
            // the seed, and we want to avoid starting the path on a saddle. An unclosed path could still originate on a
            // saddle cell adjacent to the grid edge or an NaN region; in this situation, we need the entrance edge of 
            // that saddle as found during the CW traversal above in order to find the exit edge during CCW traversal.
            if(closed)
            {
               start = seed;
               enter = start.getEntranceEdgeForContour(level, null);
            }
            cellPath.add(start);
            startRow = start.getRowIndex();
            startCol = start.getColIndex();
            Edge startEdge = enter;
            exit = start.getExitEdgeForContour(level, enter);
            start.markAsVisited(level);
            if(start.hasNoContours()) contourCellMap.remove(startRow * nCols + startCol);
            
            row = startRow;
            col = startCol;
            while(true)
            {
               // traverse to next cell in path
               switch(exit)
               {
               case LEFT : 
                  col--; 
                  enter = Edge.RIGHT; 
                  break;
               case RIGHT :
                  col++;
                  enter = Edge.LEFT;
                  break;
               case TOP :
                  row++;
                  enter = Edge.BOTTOM;
                  break;
               case BOTTOM :
                  row--;
                  enter = Edge.TOP;
                  break;
               default:
                  throw new IllegalStateException("Exit edge undefined for CCW traversal - prog error!");
               }
               
               // get next cell in COUNTERCLOCKWISE traversal. Check for termination condition: wandered outside grid, 
               // returned to starting cell, or fell into a NaN "hole" in contour grid.
               Integer key = row * nCols + col;
               Cell next = contourCellMap.get(key);
               boolean done = (row < 0) || (row>=nRows) || (col<0) || (col>=nCols);
               
               // IMPORTANT: if CCW traversal starts on a saddle cell at the grid edge, the path could return to that 
               // cell, but it cannot end there -- it must continue into the edge cell on either side of it.
               if(!done) done = (row==startRow) && (col==startCol) && !start.isSaddleFor(level);
               
               // path must "fall into" an NaN boundary cell if the next cell does not exist in the contour cell map (an 
               // NaN boundary cell that has already been visited and removed from the map), or the next cell exists but 
               // does not contain the non-NaN contour level currently being traversed. This next cell MUST be an NaN 
               // boundary cell. We remember the cell but do not include it in the cell path list.
               if((!done) && (next == null || (level != levels.size() && next.hasNoContourFor(level))))
               {
                  nanEnd = new Cell(row, col);
                  if(!nanEnd.isNaNBoundaryCell()) 
                     throw new IllegalStateException("Invalid path termination in CCW traversal - prog error!");
                  done = true;
               }
               if(done) break;
               
               // process path through next contour cell. Mark cell as visited and remove it from contour cell map if it
               // is no longer needed.
               cellPath.add(next);
               exit = next.getExitEdgeForContour(level, enter);
               next.markAsVisited(level);
               if(next.hasNoContours()) contourCellMap.remove(key);
            }
            
            // save the contour cell iso-line just traced
            isoLines.add(new IsoLine(level, closed, cellPath, startEdge, nanStart, nanEnd));
         }
      }
      catch(Exception e)
      {
         // the algorithm broke! If this happens, clear any iso-lines found thus far
         // System.out.println(e.getMessage());
         isoLines.clear();
      }

      return(isoLines);
   }
   
   /**
    * An immutable class representing the contour path objects generated by <b>ContourGenerator</b>. A contour path
    * comes in one of two basic types: an <i>iso-line</i> that follows the path of a single contour level over the 
    * contoured data grid, or a <i>filled region</i> that is bounded by two such iso-lines and possibly a grid boundary
    * or a path enclosing a region of missing data -- an "NaN hole".
    *
    * <p>The contour level associated with a contour path is typically used to determine the color with which a contour
    * iso-line is stroked or a contour fill region is filled. For a stroked path, this is the contour level of the path;
    * for a region, it is the lesser of the two contour levels bounding the region. For regions that contain data less 
    * than the minimum contour level, the contour level is the minimum data grid value. For an NaN hole, the contour 
    * level is {@link Double#NaN}.</p>
    * 
    * @author sruffner
    */
   public static class Contour
   {
      /**
       * Get the list of points that, when connected from start to finish in the order listed, define this contour
       * path. All points are specified in the coordinate system of the data grid for which the contours were generated.
       * If the contour path is closed, the last point must be connected to the first to complete the path.
       * @return List of points defining contour path, in data grid coordinates.
       */
      public List<Point2D> asPoints()
      {
         // to keep class immutable, we must perform a deep copy
         List<Point2D> out = new ArrayList<>(pathPts.size());
          for (Point2D pathPt : pathPts) out.add((Point2D) pathPt.clone());
         
         return(out);
      }
      
      /**
       * Get the geometric path that defines this contour path.
       * @return The path object, specified in the (x,y) coordinates of the contoured data grid.
       */
      public GeneralPath asPath()
      {
         if(path == null) path = buildPathFromPoints(pathPts, closed);
         return(new GeneralPath(path));
      }
      
      /**
       * Get the data level associated with this contour path. For filled contours, this is the lesser of the two 
       * contour levels bounding the filled region; if the region contains data less than the minimum contour level, 
       * then this will be the minimum data value across the data grid. For a filled contour bounding regions of missing
       * data, this value is {@link Double#NaN}.
       * @return The level assigned to this contour path, as described.
       */
      public double getLevel() { return(level); }

      /**
       * Does this contour path represent a filled region bounded by a single, closed contour iso-line or by two 
       * neighboring contour iso-lines? 
       * @return True for a contour fill region, false for an iso-line that should NOT be filled (regardless if it is
       * closed or not).
       */
      public boolean isFillRegion() { return(filled); }
      
      /**
       * Is this contour path closed? 
       * @return True if contour path is closed. Note that a filled contour region is always closed, but a contour
       * iso-line may be closed or not.
       */
      public boolean isClosed() { return(closed); }
      
      private Contour(List<Point2D> pathPts, double level, boolean filled, boolean closed)
      {
         this.pathPts = new ArrayList<>();
         this.pathPts.addAll(pathPts);
         this.path = null;
         
         this.level = level;
         this.filled = filled;
         this.closed = filled || closed;
      }
      
      private final List<Point2D> pathPts;
      private GeneralPath path;
      private final double level;
      private final boolean filled;
      private final boolean closed;
      
      /**
       * Does this contour path completely contain the specified contour path? Applicable to closed paths only.
       * @param c The contour to check.
       * @return True if this contour is closed AND it contains every point defining the path of the specified contour.
       */
      private boolean contains(Contour c)
      {
         if(!closed) return(false);
         
         if(path == null) path = buildPathFromPoints(pathPts, true);
         for(int i=0; i<c.pathPts.size(); i++)
         {
            if(!path.contains(c.pathPts.get(i))) return(false);
         }
         return(true);
      }
      
      /**
       * If this contour contains the contour path specified, modify this contour's path to exclude that contour region.
       * Applicable only if both contours are filled regions. <i>This method is intended to remove "NaN holes" from
       * non-NaN contour fill regions -- rendering the NaN regions effectively "transparent".</i>
       * 
       * @param c The contour to be excluded from this contour.
       */
      private void excerptFromPath(Contour c)
      {
         if(c != null && c.isFillRegion() && contains(c)) 
            path.append(c.asPath(), false);
      }
   }
   
   /**
    * Construct the geometric path represented by the specified list of points connected "dot-to-dot" in order.
    * @param pts List of points. All must be well-defined.
    * @param closed True if path should be closed (first point connected to the last).
    * @return The path object.
    */
   private static GeneralPath buildPathFromPoints(List<Point2D> pts, boolean closed)
   {
      GeneralPath gp = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
      Point2D p = pts.get(0);
      gp.moveTo(p.getX(), p.getY());
      for(int i=1; i<pts.size(); i++)
      {
         p = pts.get(i);
         gp.lineTo(p.getX(), p.getY());
      }
      if(closed) gp.closePath();
      return(gp);
   }
   
   /** Enumeration of the four edges of a contour cell. */
   private enum Edge { LEFT, RIGHT, TOP, BOTTOM, NONE }

    /**
    * A contour grid cell. 
    * 
    * <p>Each cell (C, R) in the contour grid is defined by four points in the data grid from which it is derived:
    * (C, R), (C, R+1), (C+1, R+1), and (C+1, R). Obviously, then, the number of rows and columns in the contour cell
    * grid is one less than the number of rows and columns in the underlying data grid!</p>
    * 
    * <p>IAW the "marching squares" algorithm, each grid cell is assigned a "case index" between 0-15 by comparing the
    * datum at each corner of the cell to the contour level V, proceeding CW from the TL corner to the BL corner:
    * bit 3 is set if TL > V, bit 2 is set if TR > V, and so on.</p>
    * 
    * <p>Cases 0 and 15 are "trivial" in the sense that the contour path for level V does not traverse a grid cell with
    * these case indices. Cases 5 and 10 are "saddle cells" in which the contour path traverses the cell twice. These 
    * are ambiguous, so we take the average over the four corners as the central value M and use it to divide these two 
    * cases into two separate instances: case 5 with M < V is assigned case index 16, while case 10 with M < V is index 
    * 17. Thus, there are 18 separate case indices: two are trivial (0, 15); four are saddles containing two contour 
    * path traversals (5, 10, 16, 17); and the rest contain a single path traversal.</p>
    * 
    * <p><b>Cell</b> is a helper class for <b>ContourGenerator</b> because it provides the case index for the grid cell
    * for every contour level in the generator's level list. Typically, a single grid cell will participate in only one
    * contour path, or none at all. If no contours pass through the cell, it can be discarded immediately by the 
    * generation algorithm -- thereby significantly reducing the search space for the start of the first contour path.
    * Furthermore, once a cell has been traversed for a given contour level V, it is marked as visited. If it does not
    * participate in any other contour paths, it can then be discarded, further reducing the search space as contour
    * paths are generated.</p>
    * 
    * <p><i>Handling NaN</i>: We need to be able to trace the "contour" that surrounds any "NaN regions" in the data 
    * grid. A contour cell that has NaN at all four corners is fully inside such a "hole" and can be discarded. A cell 
    * that has NaN at 1, 2, or 3 of its corners must contain an "NaN contour" but no real contours.</p>
    * 
    * @author sruffner
    */
   private class Cell
   {
      /**
       * Construct a contour grid cell at the specified row and column locations.
       * @param r The row index.
       * @param c The column index.
       */
      Cell(int r, int c)
      {
         if(r < 0 || r > grid.getNumRows()-2 || c < 0 || c > grid.getNumCols()-2)
            throw new IndexOutOfBoundsException("Bad row or column index");
         
         this.r = r;
         this.c = c;
         
         // the four corners of the contour cell.
         double topLeft = grid.getZ(r+1, c);
         double topRight = grid.getZ(r+1, c+1);
         double botRight = grid.getZ(r, c+1);
         double botLeft = grid.getZ(r, c);
         
         // are all four corners NaN? If so, no need to continue. Cell contains no contours whatsoever.
         boolean isTLNaN = !Utilities.isWellDefined(topLeft);
         boolean isTRNaN = !Utilities.isWellDefined(topRight);
         boolean isBRNaN = !Utilities.isWellDefined(botRight);
         boolean isBLNaN = !Utilities.isWellDefined(botLeft);
         if(isTLNaN && isTRNaN && isBRNaN && isBLNaN) return;
         
         // initially, assume cell contains no real or NaN contours -- assign all to case 0
         caseID = new int[levels.size()+1];
         Arrays.fill(caseID, 0);
         nVisits = new int[levels.size()+1];
         Arrays.fill(nVisits, 0);

         // special case: Cell contains 1, 2, or 3 NaN corners. Case number is 0 for all real contour levels. Determine
         // case for the NaN contour level, assuming NaN is less than all real contour levels. NOTE that we cannot use
         // an average value to distinguish the two possibilities for each saddle case, since two corners are NaN!
         if(isTLNaN || isTRNaN || isBRNaN || isBLNaN)
         {
            caseID[levels.size()] = (isTLNaN ? 0 : 8) | (isTRNaN ? 0 : 4) | (isBRNaN ? 0 : 2) | (isBLNaN ? 0 : 1);
            return;
         }
         
         // if all 4 corners of cell are below the minimum contour level or all are above the maximum contour level, 
         // then no contours will traverse the cell! NOTE: All tests against NaN will be false. By design the level
         // list has been sorted into ascending order.
         double iso = levels.get(0);
         if(topLeft <= iso && topRight <= iso && botLeft <= iso && botRight <= iso) return;
         iso = levels.get(levels.size()-1);
         if(topLeft > iso && topRight > iso && botLeft > iso && botRight > iso)
         {
            Arrays.fill(caseID, 15);
            return;
         }
         
         // otherwise, for each REAL contour level, determine whether or not contour line traverses the cell and in what
         // manner -- the "marching squares" case index 0-17.
         for(int i=0; i<caseID.length-1; i++)
         {
            iso = levels.get(i);
            
            // compute "case index" as follows: bit3 set if TL > level; bit2 set if TR > level; bit1 set if BR > level; 
            // and bit0 set if BL > level. Yields indices 0..15.
            caseID[i] = (topLeft>iso ? 8 : 0) | (topRight>iso ? 4 : 0) | (botRight>iso ? 2 : 0) | (botLeft>iso ? 1 : 0);
            
            // cases 5 and 10 are ambiguous saddles. Take average over 4 corners as the value at center of cell. Case 5 
            // with a central value < level is assigned case index 16, while case 10 with a central value < level is 
            // assigned index 17. All of these are saddle cases in which two iso-lines traverse the cell.
            if(caseID[i] == 5 || caseID[i] == 10)
            {
               double avg = (topLeft + topRight + botRight + botLeft) / 4;
               if(avg < iso) caseID[i] = (caseID[i] == 5) ? 16 : 17;
            }
         }
      }
      
      /** 
       * Get the row index for this cell's location in the contour cell grid. Note the number of rows in the contour 
       * cell grid is one less than the number of rows in the data grid on which it is based.
       * @return The cell's row index.
       */
      int getRowIndex() { return(r); }
      /** 
       * Get the column index for this cell's location in the contour cell grid. Note the number of columns in the 
       * contour cell grid is one less than the number of columns in the data grid on which it is based.
       * @return The cell's column index.
       */
      int getColIndex() { return(c); }
      
      /**
       * Does this cell contain an NaN boundary contour? Any cell that has 1, 2, or 3 NaN corners will contain an NaN 
       * contour.
       * @return True if this cell contains an NaN boundary contour -- regardless whether that contour has been 
       * traversed yet or not.
       */
      boolean isNaNBoundaryCell()
      {
         return(caseID != null && caseID[caseID.length-1] != 0 && caseID[caseID.length-1] != 15);
      }
      
      /**
       * Find the intersection of the specified line with the path segment of a NaN iso-line traversing this contour
       * cell. If the intersection P is outside the cell rectangle, then the NaN path segment must lie along the 
       * diagonal (cell has just one NaN corner); in this case, find the intersection with one of the edges that is 
       * perpendicular to the reference edge specified. <i>This method is used to choose a termination point for a 
       * non-NaN iso-line that terminates on a NaN boundary cell.</i>
       * 
       * @param line A line specified in the data grid's X-Y coordinate space. This will be the line containing the
       * line segment at one end of a non-NaN iso-line that terminates on this contour cell.
       * @param refEdge The edge at which the specified line crosses into this contour cell. This is used if the 
       * intersection with the NaN path segment lies outside the cell rectangle.
       * @return The intersection point, as described. Returns null if: (1) this cell is NOT an NaN boundary cell; 
       * (2) it has 3 NaN corners or is a saddle -- because a non-NaN iso-line cannot terminate on such a cell.
       */
      Point2D findIntersectionWithNaNPath(LineXY line, Edge refEdge)
      {
         if(line == null) return(null);
         
         double x1, x2, y1, y2;
         int i = (caseID != null) ? caseID[caseID.length-1] : -1;
         switch(i)
         {
         case 3: x1 = c; x2 = c + 1; y1 = y2 = r + 0.01; break;
         case 6: x1 = x2 = c + 0.99; y1 = r; y2 = r + 1; break;
         case 7: x1 = c; x2 = c + 0.99; y1 = r + 0.01; y2 = r + 1; break;
         case 9: x1 = x2 = c + 0.01; y1 = r; y2 = r + 1; break;
         case 11: x1 = c + 0.01; x2 = c + 1; y1 = r + 1; y2 = r + 0.01; break;
         case 12: x1 = c; x2 = c+1; y1 = y2 = r + 0.99; break;
         case 13: x1 = c + 0.01; x2 = c + 1; y1 = r; y2 = r + 0.99; break;
         case 14: x1 = c; x2 = c + 0.99; y1 = r + 0.99; y2 = r; break;
         default:
            return(null);
         }
         
         double scaleX = (grid.getX1() - grid.getX0()) / (grid.getNumCols()-1);
         double scaleY = (grid.getY1() - grid.getY0()) / (grid.getNumRows()-1);
         Point2D p1 = new Point2D.Double(grid.getX0() + x1*scaleX, grid.getY0() + y1*scaleY);
         Point2D p2 = new Point2D.Double(grid.getX0() + x2*scaleX, grid.getY0() + y2*scaleY);

         // calculate intersection
         Point2D pInt = line.getIntersection(new LineXY(p1, p2), null);
         
         // if the line does not intersection with the NaN path, or that intersection lies OUTSIDE the cell bounds, 
         // then check to see if the line intersects with a cell edge perpendicular to the specified reference edge.
         // If so, return that point of intersection. This better handles the case of a NaN cell with a single NaN
         // corner, when a non-NaN iso-line enters one well-defined edge and exits another well-defined edge....
         double left = grid.getX0() + c * scaleX;
         double right = grid.getX0() + (c+1) * scaleX;
         double bot = grid.getY0() + r * scaleY;
         double top = grid.getY0() + (r+1) * scaleY;
         if( (!Utilities.isWellDefined(pInt)) || ((left < right) && (pInt.getX() < left || pInt.getX() > right)) ||
             ((left > right) && (pInt.getX() > left || pInt.getX() < right)) || 
             ((bot < top) && (pInt.getY() < bot || pInt.getY() > top)) ||
             ((bot > top) && (pInt.getY() > bot || pInt.getY() < top)))
         {
            
            if(refEdge == Edge.BOTTOM || refEdge == Edge.TOP)
            {
               pInt = findIntersectionWithEdge(line, Edge.LEFT);
               if(pInt == null) pInt = findIntersectionWithEdge(line, Edge.RIGHT);
            }
            else
            {
               pInt = findIntersectionWithEdge(line, Edge.TOP);
               if(pInt == null) pInt = findIntersectionWithEdge(line, Edge.BOTTOM);
            }
            // return(p1.distance(pInt) < p2.distance(pInt) ? p1 : p2);
         }

         return(pInt);
      }
      
      /**
       * Find intersection of an infinite straight line with the line <b>segment</b> defining the specified edge of this
       * contour cell.
       * @param line The line.
       * @param e The cell edge.
       * @return The point of intersection. Returns null if there is no such intersection.
       */
      Point2D findIntersectionWithEdge(LineXY line, Edge e)
      {
         if(line == null) return(null);
         
         double scaleX = (grid.getX1() - grid.getX0()) / (grid.getNumCols()-1);
         double scaleY = (grid.getY1() - grid.getY0()) / (grid.getNumRows()-1);
         double left = grid.getX0() + c * scaleX;
         double right = grid.getX0() + (c+1) * scaleX;
         double bot = grid.getY0() + r * scaleY;
         double top = grid.getY0() + (r+1) * scaleY;
         
         LineXY edgeLine;
         Point2D pInt = null;
         double x, y;
         switch(e)
         {
         case LEFT:  
         case RIGHT: 
            x = (e==Edge.LEFT) ? left : right;
            edgeLine = new LineXY(x, bot, x, top); 
            pInt = line.getIntersection(edgeLine, null);
            if(pInt != null)
            {
               if(((bot < top) && (pInt.getY() < bot || pInt.getY() > top)) ||
                     ((bot > top) && (pInt.getY() > bot || pInt.getY() < top))) 
                  pInt = null;
            }
            break;
         case TOP:
         case BOTTOM:
            y = (e==Edge.TOP) ? top : bot;
            edgeLine = new LineXY(left, y, right, y); 
            pInt = line.getIntersection(edgeLine, null);
            if(pInt != null)
            {
               if(((left < right) && (pInt.getX() < left || pInt.getX() > right)) ||
                     ((left > right) && (pInt.getX() > left || pInt.getX() < right))) 
                  pInt = null;
            }
            break;
         default:
            break;
         }
         
         return(pInt);
      }
      
      /**
       * Find the edge crossed by the iso-line for the specified contour level as it enters this contour cell, assuming
       * a <b>counterclockwise</b> traversal to encompass the corner(s) that are less than the contour level.
       * 
       * <p><b>NOTE</b>. This method is used only to find the entrance edge of the first non-saddle cell in a CCW 
       * traversal, or to find the entrance edge when working backwards from the exit edge in a CW traversal.</p>
       * 
       * @param i Index into the contour generator's level list. <i>For the special NaN contour, use N -- the size of
       * the level list.</i>
       * @param exit The exit edge for the path -- for saddle cells only. This is specified only during a CW 
       * traversal of a path. During a CCW traversal it will be null.
       * @return The entrance edge. Returns {@link Edge#NONE} if an iso-line at the specified contour level does not 
       * traverse this cell.
       */
      Edge getEntranceEdgeForContour(int i, Edge exit)
      {
         if(caseID == null || i < 0 || i > caseID.length) throw new IndexOutOfBoundsException();
         Edge edge = Edge.NONE;
         switch(caseID[i])
         {
            case 1 :
            case 3 :
            case 7 :
               edge = Edge.LEFT;
               break;
            case 4 : 
            case 12 :
            case 13 :
               edge = Edge.RIGHT;
               break;
            case 8 :
            case 9 :
            case 11 :
               edge = Edge.TOP;
               break;
            case 2 :
            case 6 :
            case 14 :
               edge = Edge.BOTTOM;
               break;
            
            // saddle cell: entering from left or right and exiting top or bottom
            case 5 :
            case 16 :
               if((caseID[i]==5 && exit==Edge.TOP) || (caseID[i]==16 && exit==Edge.BOTTOM)) 
                  edge = Edge.LEFT;
               else if((caseID[i]==5 && exit==Edge.BOTTOM) || (caseID[i]==16 && exit==Edge.TOP)) 
                  edge = Edge.RIGHT;
               break;

            // saddle cell: entering from top or bottom and exiting left or right
            case 10 :
            case 17 :
               if((caseID[i]==10 && exit==Edge.RIGHT) || (caseID[i]==17 && exit==Edge.LEFT))
                  edge = Edge.TOP;
               else if((caseID[i]==10 && exit==Edge.LEFT) || (caseID[i]==17 && exit==Edge.RIGHT))
                  edge = Edge.BOTTOM;
               break;
               
            // empty (0, 15), or invalid case index
            default :
                break;
         }
         
         return(edge);
      }

      /**
       * Find the edge crossed by the iso-line for the specified contour level as it exits this contour cell, assuming
       * a <b>counterclockwise</b> traversal to encompass the corner(s) that are less than the contour level.
       * @param i Index into the contour generator's level list. <i>For the special NaN contour, use N -- the size of
       * the level list.</i>
       * @param enter The entrance edge for the path -- for saddle cells only.
       * @return The exit edge. Returns {@link Edge#NONE} if the specified contour path does not traverse this, or if it
       * is a saddle cell and the entrance edge is invalid.
       */
      Edge getExitEdgeForContour(int i, Edge enter)
      {
         if(caseID == null || i < 0 || i > caseID.length) throw new IndexOutOfBoundsException();

         Edge edge = Edge.NONE;
         switch(caseID[i])
         {
            // no contour; or invalid case index
            case 0 :
            case 15 :
            default :
                break;
            
           // saddle cell: entering from left or right and exiting top or bottom
            case 5 :
            case 16 :
               if((caseID[i]==5 && enter==Edge.LEFT) || (caseID[i]==16 && enter==Edge.RIGHT)) 
                  edge = Edge.TOP;
               else if((caseID[i]==5 && enter==Edge.RIGHT) || (caseID[i]==16 && enter==Edge.LEFT)) 
                  edge = Edge.BOTTOM;
               break;
               
            // saddle cell: entering from top or bottom and exiting left or right
            case 10 :
            case 17 :
               if((caseID[i]==10 && enter==Edge.TOP) || (caseID[i]==17 && enter==Edge.BOTTOM))
                  edge = Edge.RIGHT;
               else if((caseID[i]==10 && enter==Edge.BOTTOM) || (caseID[i]==17 && enter==Edge.TOP))
                  edge = Edge.LEFT;
               break;
            
            case 8 :
            case 12 :
            case 14 :
               edge = Edge.LEFT;
               break;
            case 2 :
            case 3 :
            case 11 :
               edge = Edge.RIGHT;
               break;
            case 4 :
            case 6 :
            case 7 :
               edge = Edge.TOP;
               break;
            case 1 :
            case 9 :
            case 13 :
               edge = Edge.BOTTOM;
               break;
         }
         
         return(edge);
      }
            
      /**
       * Find the coordinates (X,Y) of a corner of the contour cell where the datum value Z is less than or equal to the
       * contour level specified. 
       * 
       * @param idx The contour level index. <i>For the special NaN contour, use N -- the size of the level list.</i>
       * @return Coordinates of a corner that is below the specified contour level. Returns null if all four corners are
       * above that level. <i>For the special NaN contour, returns null if the cell is not traversed by that contour, 
       * else it returns the coordinates of a corner where Z is NaN</i>.
       */
      Point2D getCornerBelowContourLevel(int idx)
      {
         if(caseID == null || idx < 0 || idx > caseID.length) throw new IndexOutOfBoundsException();
         
         int row = -1;
         int col = -1;
         switch(caseID[idx])
         {
            // cases where BL corner is below contour level
            case 0:
            case 2:
            case 4:
            case 6:
            case 8:
            case 10:
            case 12:
            case 14:
            case 17:
               row = this.r;
               col = this.c;
               break;
               
            // cases where BR corner is below contour level
            case 1:
            case 5:
            case 9:
            case 13:
            case 16:
               row = this.r;
               col = this.c + 1;
               break;
            
            // cases where TR corner is below contour level
            case 3:
            case 11:
               row = this.r + 1;
               col = this.c + 1;
               break;
               
            // cases where TL corner is below contour level
            case 7:
               row = this.r + 1;
               col = this.c;
               break;
            
            case 15:  // NO corner is below contour level!
            default:
               break;
         }
         
         Point2D out = null;
         if(row != -1)
         {
            double x = grid.getX0() +  col * (grid.getX1() - grid.getX0()) / (grid.getNumCols()-1);
            double y = grid.getY0() +  row * (grid.getY1() - grid.getY0()) / (grid.getNumRows()-1);
            out = new Point2D.Double(x,y);
         }
         return(out);
      }
      
      /**
       * Find the coordinates (X,Y) of a corner of the contour cell where the datum value Z is greater than the contour
       * level specified. 
       * 
       * @param idx The contour level index. <i>For the special NaN contour, use N -- the size of the level list.</i>
       * @return Coordinates of a corner that is above the specified contour level. Returns null if all four corners are
       * below that level. <i>For the special NaN contour, returns the coordinates of a corner where Z is well-defined;
       * if all 4 corners are NaN, returns null.</i>.
       */
      Point2D getCornerAboveContourLevel(int idx)
      {
         if(caseID == null || idx < 0 || idx > caseID.length) throw new IndexOutOfBoundsException();
         
         int row = -1;
         int col = -1;
         switch(caseID[idx])
         {
            // cases where BL corner is above contour level
            case 1:
            case 3:
            case 5:
            case 7:
            case 9:
            case 11:
            case 13:
            case 15:
            case 16:
               row = this.r;
               col = this.c;
               break;
               
            // cases where BR corner is above contour level
            case 2:
            case 6:
            case 10:
            case 14:
            case 17:
               row = this.r;
               col = this.c + 1;
               break;
            
            // cases where TR corner is above contour level
            case 4:
            case 12:
               row = this.r + 1;
               col = this.c + 1;
               break;
               
            // cases where TL corner is above contour level
            case 8:
               row = this.r + 1;
               col = this.c;
               break;
            
            case 0:  // NO corner is below contour level!
            default:
               break;
         }
         
         Point2D out = null;
         if(row != -1)
         {
            double x = grid.getX0() +  col * (grid.getX1() - grid.getX0()) / (grid.getNumCols()-1);
            double y = grid.getY0() +  row * (grid.getY1() - grid.getY0()) / (grid.getNumRows()-1);
            out = new Point2D.Double(x,y);
         }
         return(out);
      }
      
      /**
       * Each cell containing a contour path segment for a given level must be "visited" in order to trace that segment.
       * This method is called to mark the cell as having been visited. Once a cell is visited once -- twice for saddle
       * cells -- it may not be visited again for any contour path segment at the same level. This provides a way of
       * marking cells that have been used so they can be discarded by the contour generator -- thereby speeding up the 
       * search for the start of a yet-untraced contour path segment.
       * 
       * @param i Index into the contour generator's level list. <i>For the special NaN contour, use N -- the size of
       * the level list.</i>
       */
      void markAsVisited(int i)
      {
         if(caseID == null || i < 0 || i > caseID.length) throw new IndexOutOfBoundsException();
         nVisits[i]++;
      }
      
      /**
       * Does this cell contain no contour line for the specified contour level? This will be the case if the cell is
       * not traversed by a contour line at that level, or if the contour line has already been traversed. See also: 
       * {@link #markAsVisited(int)}.
       * 
       * @param i Index into the contour generator's level list. <i>For the special NaN contour, use N -- the size of
       * the level list.</i>
       * @return True if cell contains NO <b>untraced</b> contour line at the specified level; false if it DOES.
       */
      boolean hasNoContourFor(int i)
      {
         if(caseID == null) return(true);
         if(i < 0 || i >= caseID.length) throw new IndexOutOfBoundsException();
         boolean noContour = (caseID[i] == 0) || (caseID[i] == 15);
         if(!noContour) noContour = (nVisits[i] > (isSaddleFor(i) ? 1 : 0));
         return(noContour);
      }
      
      /** 
       * Is this a saddle cell (containing two contour lines) for the specified contour level?
       * @param i Index into the contour generator's level list. <i>For the special NaN contour, use N -- the size of
       * the level list.</i>
       * @return True if cell contains two contour lines at the specified level.
       */
      boolean isSaddleFor(int i) 
      {
         if(caseID == null) return(false);
         if(i < 0 || i > caseID.length) throw new IndexOutOfBoundsException();
         return(caseID[i] == 5 || caseID[i] == 10 || caseID[i] == 16 || caseID[i] == 17);
      }
      
      /**
       * Does this cell contain no contour lines whatsoever? There are two scenarios where this will be the case:
       * <ul>
       * <li>If the cell's four corners are such that no contour lines traverse it whatsoever. Such a cell will be 
       * discarded before contour path generation begins -- thereby reducing the number of contour grid cells that must
       * be searched to find the start of the first path.</li>
       * <li>During contour path generation, contour cells are marked as they are visited -- see {@link 
       * #markAsVisited(int)}. Once visited (twice in the case of a saddle cell), they are not visited again -- they
       * contain no more contour lines at that level. Once a cell has been visited the requisite number of times for 
       * each contour level that actually traverses the cell (case index not 0 or 15), then the cell contains no more
       * contour lines that have not yet been accounted for.</li>
       * </ul>
       * Once a cell has no more untraced contour paths, it can be discarded by the contour generation algorithm.
       * Typically, a contour grid cell will contain 1 or no contour paths, so this offers a mechanism for reducing the
       * search space for each subsequent contour path.
       * 
       * @return True if cell contains no contour lines that have not been traversed already, including the NaN contour.
       */
      boolean hasNoContours() 
      { 
         if(nVisits != null)
         {
            for(int i=0; i<nVisits.length; i++) 
            {
               boolean noContour = caseID[i] == 0 || caseID[i] == 15;
               if(!noContour) noContour = (nVisits[i] > (isSaddleFor(i) ? 1 : 0));
               if(!noContour) return(false);
            }
         }
         return(true);
      }


      @Override public int hashCode() { return(31*c + r);  }

      /** Two contour cells are equal if they have the same row and column index. */
      @Override public boolean equals(Object o)
      {
         if(o == null || !o.getClass().equals(Cell.class)) return(false);
         Cell cell = (Cell) o;
         return(cell.r == r && cell.c == c);
      }

      /** The row index for this contour cell (Y-axis or vertical dimension). */
      final int r;
      /** The column index for this contour cell (X-axis or horizontal dimension). */
      final int c;
      
      /** 
       * The "marching squares" case number assigned to the the cell for each contour level. If there are N real contour
       * levels, then this array has length N+1, and the last entry is the case number for the special "NaN contour".
       * Any contour cell that has NaN at 1, 2, or 3 corners is traversed by the NaN contour, but it will contain no
       * other contours (the case number is set to 0 for all real contour levels). 
       */
      int[] caseID = null;
      
      /**
       * During contour path tracing, each contour cell containing a path segment will be visited once -- twice for 
       * saddle cells -- while traversing that segment. Once a cell has been visited the requisite number of times for
       * a given contour level, it will not be visited again and is no longer needed for that level. This is part of 
       * the mechanism for eliminating "used" cells that no longer need to be considered when searching for a contour
       * cell containing an untraced path segment.
       */
      int[] nVisits = null;
   }
   
   /**
    * This helper class encapsulates a sequence of contour cells visited while traversing a continuous contour iso-line 
    * across the underlying data grid. It includes important information needed to construct the list of actual
    * points that define the iso-line.
    * 
    * @author sruffner
    */
   private class IsoLine
   {
      /**
       * Construct a contour cell iso-line path segment.
       * 
       * @param idx Index of the contour level. For an NaN contour, use N = size of contour level list.
       * @param closed True if path is closed; else false.
       * @param cellPath The sequence of contour cells traversed by the iso-line path in the CCW direction.
       * @param startEdge The edge on which the contour path starts. This is needed only when the first cell in the
       * cell path is a saddle cell (a rare occasion).
       * @param nanStart If the iso-line emerges from an NaN region, this is the NaN cell from which it emerges. If null
       * and the path is not closed, then the iso-line must emerge from an edge of the data grid.
       * @param nanEnd If the iso-line terminates on an NaN region, this is the NaN cell on which it terminates. If null
       * and the path is not closed, then the iso-line must terminate on an edge of the data grid.
       */
      IsoLine(int idx, boolean closed, List<Cell> cellPath, Edge startEdge, Cell nanStart, Cell nanEnd)
      {
         this.levelIdx = (idx >= 0 && idx < levels.size()) ? idx : levels.size();
         this.level = (levelIdx == levels.size()) ? Double.NaN : levels.get(levelIdx);
         
         this.closed = closed;
         this.cellsInPath = cellPath;
         this.startEdge = startEdge;
         this.nanCellAtStart = nanStart;
         this.nanCellAtEnd = nanEnd;
      }
      
      /**
       * Is this contour cell iso-line a closed path?
       * @return True if path is closed. Otherwise, the path must start and end on an edge of the data grid, or on an
       * NaN contour region.
       */
      boolean isClosed() { return(closed); }
      
      /**
       * Get the contour level assigned to this contour cell iso-line.
       * @return The contour level. Will be {@link Double#NaN} for an iso-line tracing the boundary of an NaN hole.
       */
      double getContourLevelValue() { return(level); }
      /**
       * Does this contour cell iso-line trace the boundary of an NaN hole in the data grid?
       * @return True for an NaN boundary path; false otherwise.
       */
      boolean isNaNBoundaryPath() { return(levelIdx == levels.size()); }
      
      /**
       * Compute the list of points that define the actual path traversed by the iso-line in the original direction or
       * in the reverse (from end to start). This involves finding the edge crossing in each cell and using linear 
       * interpolation to pinpoint the edge intersection in data grid coordinates. If the path starts and/or ends on an 
       * NaN boundary cell, the path is extended so that it intersects with the NaN path segment within that cell (or if
       * the NaN cell has just one NaN corner, the path may instead extend to intersect the other well-defined edge).
       * 
       * @param  reverse If true, the points are listed in reverse order.
       * @return The list of points defining the path of the contour iso-line, in the requested order.
       */
      List<Point2D> getContourPathPoints(boolean reverse)
      {
         // compute points list if we've have not done so yet
         if(ptsInPath == null)
         {
            ptsInPath = new ArrayList<>();
            
            Cell next = cellsInPath.get(0);
            Edge enter = startEdge;
            ptsInPath.add(getPathIntersectionAlongEdge(next, enter));
            Edge exit = next.getExitEdgeForContour(levelIdx, enter);
            ptsInPath.add(getPathIntersectionAlongEdge(next, exit));
            
            for(int i=1; i<cellsInPath.size(); i++)
            {
               next = cellsInPath.get(i);
               
               switch(exit)
               {
               case LEFT :    enter = Edge.RIGHT; break;
               case RIGHT :   enter = Edge.LEFT; break;
               case TOP :     enter = Edge.BOTTOM; break;
               case BOTTOM :  enter = Edge.TOP; break;
               default:
                  throw new NeverOccursException("Prog error?");
               }
               
               exit = next.getExitEdgeForContour(levelIdx, enter);
               ptsInPath.add(getPathIntersectionAlongEdge(next, exit));
            }
            
            // if it starts on an NaN boundary, extend the iso-line (in a straight line) to intersect either with the
            // NaN boundary path or another edge of the NaN cell (depends on trajectory of iso-line). Analogously if it
            // ends on a NaN boundary.
            if(nanCellAtStart != null)
            {
               Edge ref;
               switch(startEdge)
               {
               case LEFT: ref = Edge.RIGHT; break;
               case RIGHT: ref = Edge.LEFT; break;
               case TOP: ref = Edge.BOTTOM; break;
               default : ref = Edge.TOP;
               }
               LineXY line = new LineXY(ptsInPath.get(0), ptsInPath.get(1));
               Point2D pInt = nanCellAtStart.findIntersectionWithNaNPath(line, ref);
               if(pInt != null) ptsInPath.add(0, pInt);
            }
            if(nanCellAtEnd != null)
            {
               Edge ref;
               switch(exit)
               {
               case LEFT: ref = Edge.RIGHT; break;
               case RIGHT: ref = Edge.LEFT; break;
               case TOP: ref = Edge.BOTTOM; break;
               default : ref = Edge.TOP;
               }
               int n = ptsInPath.size();
               LineXY line = new LineXY(ptsInPath.get(n-2), ptsInPath.get(n-1));
               Point2D pInt = nanCellAtEnd.findIntersectionWithNaNPath(line, ref);
               if(pInt != null) ptsInPath.add(pInt);
            }

         }
         
         List<Point2D> out = new ArrayList<>();
         if(!reverse) out.addAll(ptsInPath);
         else for(int i=ptsInPath.size()-1; i>=0; i--) out.add(ptsInPath.get(i));
         
         return(out);
      }
      
      /**
       * Use linear interpolation, the cell row-column location, and the data grid's X-Y extents to calculate the (X,Y)
       * coordinates of the intersection of this contour iso-line with a specified edge of a contour cell traversed by
       * the iso-line. It is assumed that the contour level falls between the data values at the end points of the 
       * specified edge.
       * <p><b>Special case</b>: When computing the intersection of an NaN contour path segment with a cell edge, linear
       * interpolation doesn't make sense -- by definition, the edge in question will have one NaN and one well-defined
       * corner. In this scenario, the intersection is placed very close to the non-NaN corner, but still inside the 
       * contour cell.</p>
       * 
       * @param cell The contour cell.
       * @param e The intersecting edge of the contour cell. If null, {@link Edge#BOTTOM} is assumed.
       * @return The point of intersection, in the coordinate system of the data grid being contoured.
       */
      Point2D getPathIntersectionAlongEdge(Cell cell, Edge e)
      {
         int r = cell.getRowIndex();
         int c = cell.getColIndex();
         double topLeft, botLeft, topRight, botRight;
         double x = c;
         double y = r;
         boolean isNaN = !Utilities.isWellDefined(level);
         if(e == Edge.LEFT)
         {
            topLeft = grid.getZ(r+1, c);
            botLeft = grid.getZ(r, c);
            if(isNaN)
               y += Utilities.isWellDefined(botLeft) ? 0.01 : 0.99;
            else 
               y += (level - botLeft) / (topLeft - botLeft);
         }
         else if(e == Edge.RIGHT)
         {
            topRight = grid.getZ(r+1, c+1);
            botRight = grid.getZ(r, c+1);               
            x += 1;
            if(isNaN)
               y += Utilities.isWellDefined(botRight) ? 0.01 : 0.99;
            else 
               y += (level - botRight) / (topRight - botRight);
         }
         else if(e == Edge.TOP)
         {
            topLeft = grid.getZ(r+1, c);
            topRight = grid.getZ(r+1, c+1);
            if(isNaN)
               x += Utilities.isWellDefined(topLeft) ? 0.01 : 0.99;
            else
               x += (level - topLeft) / (topRight - topLeft);
            y += 1;
         }
         else
         {
            botLeft = grid.getZ(r, c);              
            botRight = grid.getZ(r, c+1); 
            if(isNaN)
               x += Utilities.isWellDefined(botLeft) ? 0.01 : 0.99;
            else
               x += (level - botLeft) / (botRight - botLeft);
         }
         
         x = grid.getX0() +  x * (grid.getX1() - grid.getX0()) / (grid.getNumCols()-1);
         y = grid.getY0() +  y * (grid.getY1() - grid.getY0()) / (grid.getNumRows()-1);
         
         return(new Point2D.Double(x, y));
      }
      
      /**
       * Get the filled contour region bounded by this closed contour cell iso-line. Applicable to closed paths only.
       * @return The filled contour bounded by this iso-line. Returns null if iso-line path is NOT closed.
       */
      Contour asFilledContour()
      {
         if(!closed) return(null);
         
         List<Point2D> pts = getContourPathPoints(false);
         
         // with the trick we employ to compute filled contour regions, all NaNs and the border around the original
         // data grid are set to a value L far less than Z0, the actual minimum Z value in the data grid, and Z0 is
         // temporarily added as the first contour level. The contour iso-lines found will all be closed, making it
         // easy to create the filled contour regions. In this scenario, a fill index < 0 corresponds to a region 
         // containing only values less than Z0 -- which are the NaN regions!
         int fillIdx = -1;
         GeneralPath gp = ContourGenerator.buildPathFromPoints(pts, true);
         for(Cell c : cellsInPath)
         {
            Point2D p = c.getCornerBelowContourLevel(levelIdx);
            if(p != null && gp.contains(p)) { fillIdx = levelIdx - 1; break; }
            p = c.getCornerAboveContourLevel(levelIdx);
            if(p != null && gp.contains(p)) { fillIdx = levelIdx; break; }
         }
         double level = (fillIdx<0 || fillIdx >= levels.size()) ? Double.NaN : levels.get(fillIdx);
         
         return(new Contour(pts, level, true, true));
      }
      
      /** Index of the contour level traversed by this contour cell iso-line. */
      final int levelIdx;
      /** 
       * The actual contour level traversed by this contour cell iso-line. Will be {@link Double#NaN} for an iso-line
       * tracing the boundary of an NaN hole in the data grid.
       */
      final double level;
      
      /** List of contour cells traversed by the iso-line, from first to last in a CCW traversal. */
      final List<Cell> cellsInPath;
      
      /** The edge (of the first contour cell) on which the path starts). */
      final Edge startEdge;
      
      /** List of points tracing the iso-line in data grid coordinates, from first to last in a CCW traversal. */
      List<Point2D> ptsInPath = null;
      
      /** 
       * True if the contour cell iso-line is a closed path. If it is not closed, it starts and ends on an edge of the 
       * data grid, or on an NaN boundary. 
       */
      final boolean closed;
      
      /** The NaN boundary cell from which iso-line emerges, if applicable. Not included in the cell list. */
      final Cell nanCellAtStart;
      /** The NaN boundary cell on which iso-line terminates, if applicable. Not included in the cell list. */
      final Cell nanCellAtEnd;
   }
   
   /**
    * A wrapper for the contour generator's source data grid. Its sole purpose is to configure the data grid for two
    * distinct contour computations:
    * <ul>
    * <li><i>For computing the stroked contour iso-lines</i>. In this case, the wrapper does nothing but forward the 
    * information from the source data grid.</li>
    * <li><i>For computing the filled contour regions between iso-lines</i>. Here we want all contour iso-lines to be
    * closed paths. The edges of the data grid and any NaNs in the source data grid would prevent this. To circumvent
    * the issue and "trick" the contour generator into constructing all closed paths, the wrapper computes a value L
    * that is much smaller than the actual minimum Z value in the source grid: <i>L = Z0 - G*(Z1-Z0)</i>, where the true
    * Z-data range of the source matrix is [Z0..Z1], Z0<Z1, and G = 1000. Furthermore, it adds a border of data points
    * around the source matrix, with Z=L at all such points.</li>
    * </ul>
    * <p>When computing the filled contours, an extra contour level is added at Z0. Closed contours at this level will
    * bound the original data grid's border, as well as any regions of NaN data within the original grid. To switch 
    * between the two alternate configurations, call {@link #setAllowNaNs(boolean)}.</p>
    * 
    * @author sruffner
    */
   private static class DGrid implements IDataGrid
   {
      /** 
       * Construct a wrapper for the specified data matrix.
       * @param src The source data matrix. If null, then wrapper behaves like an empty matrix.
       */
      DGrid(IDataGrid src)
      {
         this.src = src;
         setAllowNaNs(true);
      }
      
      /**
       * Reconfigure the data grid wrapper.
       * 
       * @param allow If true, the wrapper behaves exactly like the source data grid. Otherwise, it adds a border of 
       * data points around all sides of the original grid; all such border points and any NaNs in the original grid
       * are set to a value L that is about 3 orders of magnitude less than the minimum Z value of the source grid.
       */
      void setAllowNaNs(boolean allow)
      {
         allowNaN = allow;
         
         // NOTE: Scale factor was 10^6, but this caused problems with contains() checks for filled contours that wind
         // around the grid border. When computing filled contours, we cheat by growing the data grid by one on all 
         // sides and set Z on all sides to this very low value -- forcing all contour iso-lines to be closed. But this
         // means the lowest level contour will wind around the grid, the next level contour will be just inside it,
         // and so on -- only broken up when the contours move into the grid interior. With too large a scale factor,
         // the contains() check would fail, and the filled contours were not ordered properly.
         // 
         // Changed to 10^3.
         minZ = (src==null) ? 0 : (src.getMinZ() - 10e3 * (src.getMaxZ()-src.getMinZ()));
         
         xRng[0] = xRng[1] = 0;
         yRng[0] = yRng[1] = 0;
         nr = nc = 0;
         if(src != null)
         {
            double adj = allow ? 0 : (src.getX1()-src.getX0())/(src.getNumCols()-1);
            xRng[0] = src.getX0() - adj;
            xRng[1] = src.getX1() + adj;
            
            adj = allow ? 0 : (src.getY1()-src.getY0())/(src.getNumRows()-1);
            yRng[0] = src.getY0() - adj;
            yRng[1] = src.getY1() + adj;
            
            nr = src.getNumRows() + (allow ? 0 : 2);
            nc = src.getNumCols() + (allow ? 0 : 2);
         }
      }
      
      @Override public int getNumRows() { return(nr); }
      @Override public int getNumCols() { return(nc); }

      @Override public double getX0() { return(xRng[0]); }
      @Override public double getX1() { return(xRng[1]); }
      @Override public double getY0() { return(yRng[0]); }
      @Override public double getY1() { return(yRng[1]); }
      @Override public double getMinZ() { return(src != null ? src.getMinZ() : 0); }
      @Override public double getMaxZ() { return(src != null ? src.getMaxZ() : 0); }
      @Override public double getZ(int r, int c)
      {
         if(r<0 || r>=getNumRows() || c<0 || c>=getNumCols()) throw new IndexOutOfBoundsException();
         
         if(allowNaN) return(src.getZ(r,c));
         
         if(r==0 || r==getNumRows()-1 || c==0 || c==getNumCols()-1) return(minZ);
         double d = src.getZ(r-1, c-1);
         return(Utilities.isWellDefined(d) ? d : minZ);
      }
      
      /** The original data grid. If null, wrapper behaves like a 0x0 matrix. */
      private final IDataGrid src;
      /** 
       * If false, all NaNs are replaced by a value L far less than the minimum Z value in the source grid and a border
       * of data points at value L are added on all sides of the source grid. If true, the wrapper behaves exactly like
       * the source grid. 
       */
      private boolean allowNaN;
      private int nr;
      private int nc;
      private final double[] xRng = new double[] {0, 0};
      private final double[] yRng = new double[] {0, 0};
      /** A value far less than the minimum Z value in the source data grid. */
      private double minZ;
   }
}