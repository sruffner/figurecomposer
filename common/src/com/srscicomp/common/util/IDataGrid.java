package com.srscicomp.common.util;

/**
 * This interface defines a discrete sampling of a variable Z = F(X,Y) over a regular NxM grid in (X,Y) space.
 * 
 * <p>The grid should contain at least two rows and two columns. The X-coordinate is linearly sampled across the M 
 * columns over a non-empty range [X0..X1], where X0 is the value for the first column and X1 for the last. Note that
 * X0 could be less than or greater than X1. Similarly, the Y-coordinate is linearly sampled across N rows over the 
 * range [Y0..Y1].</p>
 * 
 * @author sruffner
 */
public interface IDataGrid
{
   /** 
    * Get the number of rows in the grid, i.e., the number of samples along the Y-dimension.
    * @return The row count. 
    */
   int getNumRows();
   /** 
    * Get the number of columns in the grid, i.e., the number of samples along the X-dimension.
    * @return The column count. 
    */
   int getNumCols();
   
   /** 
    * Get the start of the X-coordinate range for the data grid, i.e., the X-coordinate for the first grid column.
    * @return X coordinate for first grid column. May be less than or greater than, but not equal to {@link #getX1()}.
    */
   double getX0();
   /** 
    * Get the end of the X-coordinate range for the data grid, i.e., the X-coordinate for the last grid column.
    * @return X coordinate for last grid column. May be less than or greater than, but not equal to {@link #getX0()}.
    */
   double getX1();
   /** 
    * Get the start of the Y-coordinate range for the data grid, i.e., the Y-coordinate for the first grid row.
    * @return Y coordinate for first grid row. May be less than or greater than, but not equal to {@link #getY1()}.
    */
   double getY0();
   /** 
    * Get the end of the Y-coordinate range for the data grid, i.e., the Y-coordinate for the last grid row.
    * @return Y coordinate for last grid row. May be less than or greater than, but not equal to {@link #getY0()}.
    */
   double getY1();
   
   /**
    * Get the minimum Z value in the data grid. 
    * @return Minimum Z value. Return NaN if grid lacks any well-defined values.
    */
   double getMinZ();
   /**
    * Get the maximum Z value in the data grid. 
    * @return Maximum Z value. Return NaN if grid lacks any well-defined values.
    */
   double getMaxZ();
   
   /**
    * Get the value of Z for the specified location in the data grid.
    * @param r The row index.
    * @param c The column index.
    * @return The Z value at (r,c).
    * @throw IndexOutOfBoundsException if either index is invalid.
    */
   double getZ(int r, int c);
}
