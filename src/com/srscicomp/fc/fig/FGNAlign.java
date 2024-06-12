package com.srscicomp.fc.fig;

/**
 * A simple enumeration of the different possibilities for aligning two or more graphic nodes within a figure.
 * 
 * @author sruffner
 */
public enum FGNAlign
{
   /** Aligned along a common left edge. */ LEFT("Left"),
   /** Aligned along a common right edge. */ RIGHT("Right"),
   /** Aligned along a common bottom edge. */ BOTTOM("Bottom"),
   /** Aligned along a common top edge. */ TOP("Top"), 
   /** Centered horizontally. */ HCENTER("Horizontal Center"),
   /** Centered vertically. */ VCENTER("Vertical Center");
   
   FGNAlign(String label) { this.label = (label == null) ? name() : label; }
   
   @Override public String toString() { return(label); }
   
   private final String label;
}
