package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;

import com.srscicomp.common.g2dutil.PainterStyle;

/**
 * Helper class implements a painter style object that has the same style properties as the graphic node specified at
 * construction, except that the fill color can be changed. It is used to render data presentation nodes like bar plots,
 * area charts, and pie charts -- where the fill color is different for different regions of the plot.
 * 
 * @author sruffner
 */
class MutableFillPS implements PainterStyle
{
   /**
    * Construct a {@link PainterStyle} that reflects the style properties of the specified graphic node, but lets the
    * fill color be modified. Initially, the fill color is set to the node's current fill color.
    * @param srcNode A graphic node. Must not be null.
    */
   MutableFillPS(FGraphicNode srcNode)
   {
      if(srcNode == null) throw new NullPointerException("Null graphic node!");
      
      this.srcNode = srcNode;
      fillC = srcNode.getFillColor();
   }
   
   @Override public Font getFont() { return(srcNode.getFont()); }
   @Override public Color getStrokeColor() { return(srcNode.getStrokeColor()); }
   @Override public Stroke getStroke(float dashPhase)  { return(srcNode.getStroke(dashPhase)); }
   @Override public double getFontSize() { return(srcNode.getFontSize()); }
   @Override public double getStrokeWidth() { return(srcNode.getStrokeWidth()); }
   @Override public boolean isStrokeSolid() { return(srcNode.isStrokeSolid()); }
   @Override public boolean isStroked() { return(srcNode.isStroked()); }
   
   @Override public Color getFillColor() { return(fillC); }
   
   /**
    * Change the fill color for this painter style object.
    * @param c The new fill color. If null, opaque black is assumed.
    */
   void setFillColor(Color c) { fillC = (c==null) ? Color.BLACK : c; }

   /** The current fill color for this painter style. */
   private Color fillC;
   
   /** The graphic node that sources all style properties except the fill color. */
   private final FGraphicNode srcNode;
}
