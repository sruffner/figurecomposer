package com.srscicomp.fc.fig;

/**
 * An enumeration of the different classes of graphic nodes that exist in the <em>DataNav</em> graphics model.
 * 
 * <p>This enumeration is primarily for reference purposes, but it can be used in certain situations to generically 
 * identify a graphics node (instead of by class).</p>
 * 
 * @author sruffner
 */
public enum FGNodeType
{
   FIGURE("Figure"), GRAPH("Graph"), LABEL("Text Label"), LINE("Line Segment"), SHAPE("Shape"), 
   CALIB("Calibration Bar"), FUNCTION("Function Trace"), TRACE("XY Data Trace"), RASTER("Raster"), 
   CONTOUR("Contour"), BAR("Bar Plot"), AREA("Area Chart"), PIE ("Pie Chart"), SCATTER("2D Scatter Plot"), 
   AXIS("Axis"), TICKS("Tick Set"), LEGEND("Legend"), GRIDLINE("Grid Lines"), SYMBOL("Symbol Style"), 
   EBAR("Error Bar Style"), CBAR("Color Bar/Axis"), TEXTBOX("Text Box"), IMAGE("Image"), GRAPH3D("3D Graph"),
   BACK3D("Backplane 3D"), GRID3D("Grid Lines 3D"), AXIS3D("Axis 3D"), TICKS3D("Tick set 3D"),
   SCATTER3D("3D Scatter/Line Plot"), SURFACE("Surface"), PGRAPH("Polar Graph"), PAXIS("Polar Axis"),
   BOX("Box Plot"), VIOLIN("Violin Plot"), SCATLINESTYLE("Scatter Plot Line Style");
   
   private String niceName;
   
   FGNodeType(String s) { niceName = s; }
   
   @Override
   public String toString() { return(super.toString().toLowerCase()); }

   /**
    * The name of the node type as it should appear in GUI components.
    * 
    * @return Human-readable name of node type for GUI display.
    */
   public String getNiceName() { return(niceName); }
}
