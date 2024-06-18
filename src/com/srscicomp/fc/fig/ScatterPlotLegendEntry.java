package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.Projector;
import com.srscicomp.common.g2dutil.ShapePainter;
import com.srscicomp.common.ui.BkgFill;

/**
 * <b>ScatterPlotLegendEntry</b> implements a custom {@link LegendEntry} for the 2D and 3D scatter plot nodes, {@link
 * ScatterPlotNode} and {@link Scatter3DNode}.
 * 
 * <p>These nodes both have "bubble plot" display modes, in which the symbol size and/or fill color varies with the
 * Z-coordinate of the underlying data. To represent such bubble plots, the customized legend entry always includes
 * two symbols that graphically portray the range in symbol size/color spanned by the data. (In the normal "scatter"
 * display mode, only one symbol is shown -- unless the legend requires symbols at the entry end points, in which case
 * both symbols are the same.)</p>
 * 
 * <p>The 3D scatter plot is unique in that it supports a gradient fill for the scatter plot symbols. Neither type of
 * scatter plot supports a character centered within each symbol.</p>
 * 
 * <p>In addition, the 3D scatter plot also has two bar plot display modes, in which each data point is represented by a
 * 3D bar spanning from the data point (X,Y,Z) to (X,Y,Zo), where Zo is a specified "base plane" for the bar plot. In 
 * this case, the legend entry is spanned by a simple filled horizontal rectangle, stroked IAW the node's stroking
 * properties and filled IAW the display mode:
 * <ul>
 * <li>{@link Scatter3DNode.DisplayMode#BARPLOT}: Filled with 3D scatter plot node's current fill color.</li>
 * <li>{@link Scatter3DNode.DisplayMode#COLORBARPLOT}: Filled with a linear color gradient that depicts the parent 3D 
 * graph's color map.</li>
 * </ul>
 * </p>
 * 
 * @author sruffner
 */
class ScatterPlotLegendEntry extends LegendEntry
{
   ScatterPlotLegendEntry(ScatterPlotNode spn) { super(spn, null); init();}
   ScatterPlotLegendEntry(Scatter3DNode sp3) { super(sp3, null); init(); }
   
   /**
    * Helper method analyzes the 2D or 3D scatter plot node to prepare information that will be needed to render the
    * legend entry.
    */
   private void init()
   {
      is2D = (plottable.getNodeType() == FGNodeType.SCATTER);
      
      bkgFills = new BkgFill[2];
      
      ScatterPlotNode.DisplayMode m;
      double zStart, zEnd;
      boolean isLogCM;
      BkgFill defBkgFill;
      if(is2D)
      {
         ScatterPlotNode spn = (ScatterPlotNode) plottable;
         m = spn.getMode();
         
         ColorBarNode cbar = spn.getParentGraph().getColorBar();
         zStart = cbar.getStart();
         zEnd = cbar.getEnd();
         if(zStart > zEnd)
         {
            double d = zStart; 
            zStart = zEnd; 
            zEnd = d;
         }
         else if(zStart == zEnd) zEnd = zStart + 1;
         
         colorLUT = cbar.getColorLUT();
         isLogCM = cbar.isLogarithmic();
         
         defBkgFill = BkgFill.createSolidFill(spn.getFillColor());
      }
      else
      {
         Scatter3DNode sp3 = (Scatter3DNode) plottable;
         
         Scatter3DNode.DisplayMode m3 = sp3.getMode();
         switch(m3)
         {
         case SCATTER: m = ScatterPlotNode.DisplayMode.SCATTER; break;
         case SIZEBUBBLE : m = ScatterPlotNode.DisplayMode.SIZEBUBBLE; break;
         case COLORBUBBLE : m = ScatterPlotNode.DisplayMode.COLORBUBBLE; break;
         case COLORSIZEBUBBLE : m = ScatterPlotNode.DisplayMode.COLORSIZEBUBBLE; break;
         default : m = null; break;
         }
         
         Graph3DNode g3 = (Graph3DNode) sp3.getParentGraph();
         Projector prj = g3.get2DProjection();
         zStart = prj.getZMin();
         zEnd = prj.getZMax();
         colorLUT = g3.getColorBar().getColorLUT();
         isLogCM = g3.getColorBar().isLogarithmic();
         
         defBkgFill = sp3.getBackgroundFill();
         
         isBarPlot = (m==null);
         if(isBarPlot && m3==Scatter3DNode.DisplayMode.BARPLOT) colorLUT = null;
      }
      
      isSizeBubble = (m==ScatterPlotNode.DisplayMode.SIZEBUBBLE) || (m==ScatterPlotNode.DisplayMode.COLORSIZEBUBBLE);
      isColorBubble = (m==ScatterPlotNode.DisplayMode.COLORBUBBLE)||(m==ScatterPlotNode.DisplayMode.COLORSIZEBUBBLE);

      if(!isColorBubble)
      {
         bkgFills[0] = bkgFills[1] = defBkgFill;
      }
      if(isSizeBubble || isColorBubble)
      {
         float[] rng = plottable.getDataRange();
         if(isSizeBubble)
         {
            double zAbsMax = Math.max(Math.abs(rng[4]), Math.abs(rng[5]));
            minSzScale = (zAbsMax == 0) ? 1 : Math.min(Math.abs(rng[4]), Math.abs(rng[5])) / zAbsMax;
            if(minSzScale < 0.4) minSzScale = 0.4;
         }
         if(isColorBubble)
         {
            int rgbMin = colorLUT.mapValueToRGB(rng[4], zStart, zEnd, isLogCM);
            int rgbMax = colorLUT.mapValueToRGB(rng[5], zStart, zEnd, isLogCM);
            
            for(int i=0; i<2; i++)
            {
               Color c = new Color(i==0 ? rgbMin : rgbMax);
               switch(defBkgFill.getFillType())
               {
               case SOLID : 
                  bkgFills[i] = BkgFill.createSolidFill(c); break;
               case AXIAL : 
                  bkgFills[i] = BkgFill.createAxialGradientFill(defBkgFill.getOrientation(), defBkgFill.getColor1(), c);
                  break;
               case RADIAL :
                  bkgFills[i] = BkgFill.createRadialGradientFill(
                        defBkgFill.getFocusX(), defBkgFill.getFocusY(), defBkgFill.getColor1(), c);
                  break;
               }
            }
         }
      }
   }
   
   @Override double getOffsetToRightEdge(LegendNode legend)
   {
      if(legend == null) return(0);
      
      // get legend props we need. Note that "mid" flag is ignored for bubble plots
      double len = legend.getEntryLengthInMilliInches();
      boolean mid = (!isSizeBubble) && (!isColorBubble) && legend.getMid();
      double sw = plottable.getStrokeWidth();

      double ofs;
      if(!areSymbolsRendered()) 
         ofs = len + sw/2;
      else
      {
         double symSz = legend.getSymbolSize().toMilliInches();
         if(symSz == 0) symSz = getSymbolSizeInMilliInches();
         symSz += getSymbolStrokeWidthInMilliInches();
         
         if(mid) ofs = (symSz > (len + sw)) ? (len + symSz)/2 : len + sw/2;
         else ofs = len + Math.max(sw/2, symSz/2);
      }
      return(ofs);
   }
   
   @Override Rectangle2D getRenderBounds(double y, LegendNode legend, Rectangle2D r)
   {
      Rectangle2D rOut = r;
      if(rOut != null) rOut.setFrame(0, 0, 0, 0);
      else rOut = new Rectangle2D.Double();
      if(legend == null) return(rOut);
      
      // get legend props we need. Note that "mid" flag is ignored for bubble plots
      double len = legend.getEntryLengthInMilliInches();
      boolean mid = (!isSizeBubble) && (!isColorBubble) && legend.getMid();
      double sw = plottable.getStrokeWidth();
      double spacerMI = legend.getSpacer().toMilliInches();
      
      if(isBarPlot)
      {
         // bar plot legend entry is a rectangle with length spanning the entry and height = 1/4 the perpendicular
         // distance between successive entries
         double h = spacerMI/2;
         rOut.setFrame(-sw/2, y - h/2 - sw/2, len + sw, h + sw);
      }
      else if(!areSymbolsRendered())
         rOut.setFrame(-sw/2, y - sw/2, len + sw, sw);
      else
      {
         double symSz = legend.getSymbolSize().toMilliInches();
         if(symSz == 0) symSz = getSymbolSizeInMilliInches();
         double symSW = getSymbolStrokeWidthInMilliInches();
         
         // for size-bubble plots, the symbol at left end point is smaller.
         double leftSz = minSzScale * symSz + symSW;
         symSz += symSW;
         
         double x = Math.min(-sw/2, mid ? (len-symSz)/2.0 : -leftSz/2);
         double w = Math.abs(x);
         if(mid) w += (symSz > len+sw) ? (len+symSz)/2 : (len + sw/2);
         else w += len + Math.max(sw/2, symSz/2);
         double h = Math.max(sw, symSz);
         rOut.setFrame(x, y-h/2, w, h);
      }
      
      return(rOut);
   }
   
   @Override void render(Graphics2D g2d, double y, LegendNode legend)
   {
      if(g2d == null || legend == null) return;
      
      double len = legend.getEntryLengthInMilliInches();
      boolean mid = legend.getMid();
      
      List<Point2D> pts = new ArrayList<>();
      pts.add(new Point2D.Double(0, y));
      
      // we use a shape painter to render the two symbols in legend entry (potentially one for the "scatter" mode). 
      // The symbol styling source is different for the 2D and 3D scatter plots...
      SymbolNode sym = plottable.getSymbolNode();
      ShapePainter shapePainter = new ShapePainter(is2D ? plottable : sym, pts, 
            is2D ? ((ScatterPlotNode)plottable).getSymbol() : sym.getType(), 1f, null);
      
      // the size of a symbol in the legend entry may be determined by the legend node itself.
      double symSz = legend.getSymbolSize().toMilliInches();
      if(symSz == 0) symSz = getSymbolSizeInMilliInches();

      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         // special case: 3D bar plot. Render legend entry as a horizontal rectangle spanning length of entry and
         // having a height = 1/2 the perpendicular distance between successive entries
         if(isBarPlot)
         {
            double hMI = legend.getSpacer().toMilliInches() / 2;
            Rectangle2D bar = new Rectangle2D.Double(0, y- hMI/2, len, hMI);

            if(colorLUT == null) g2dCopy.setColor(plottable.getFillColor());
            else g2dCopy.setPaint(colorLUT.getGradientPaint(0, (float) y, (float) len, (float) y));
            g2dCopy.fill(bar);
            
            g2dCopy.setStroke(plottable.getStroke(0));
            g2dCopy.setColor(plottable.getStrokeColor());
            g2dCopy.draw(bar);
         }
         
         if(hasTraceLine())
         {
            // for 2D scatter plot, we only draw a trace line in the trendline display mode, and its styled IAW 
            // a component of the ScatterPlotNode. For 3D scatter, use the node's own draw styles.
            FGraphicNode lineStyle = is2D ? ((ScatterPlotNode)plottable).getLineStyle() : plottable;
            
            g2dCopy.setStroke(lineStyle.getStroke(0));
            g2dCopy.setColor(lineStyle.getStrokeColor());
            g2dCopy.draw(new Line2D.Double(0, y, len, y));
         }
         
         if(!areSymbolsRendered()) return;
         
         if(!(isSizeBubble || isColorBubble))
         {
            shapePainter.setSize((float) symSz);
            shapePainter.setBackgroundFill(bkgFills[0]);
            if(mid) pts.get(0).setLocation(len/2, y);
            else pts.add(new Point2D.Double(len, y));
            
            shapePainter.render(g2dCopy, null);
         }
         else
         {
            // for bubble plots, we always draw two symbols, and the legend's "mid" flag is ignored. The size and/or
            // color of the two symbols vary to reflect the variation in the bubble plot itself.
            shapePainter.setSize((float) (isSizeBubble ? minSzScale*symSz : symSz));
            shapePainter.setBackgroundFill(bkgFills[0]);
            shapePainter.render(g2dCopy, null);
            
            pts.get(0).setLocation(len, y);
            shapePainter.setSize((float) symSz);
            shapePainter.setBackgroundFill(bkgFills[1]);
            shapePainter.render(g2dCopy, null);
         }
      }
      finally { if(g2dCopy != null) g2dCopy.dispose(); }
   }
   
   @Override void toPostscript(PSDoc psDoc, double y, LegendNode legend)
   {
      if(psDoc == null || legend == null) return;
      
      double len = legend.getEntryLengthInMilliInches();
      boolean mid = legend.getMid();
      
      Point2D p0 = new Point2D.Double(0, y);
      Point2D p1 = new Point2D.Double(len, y);
      
      psDoc.startElement(plottable, plottable, true);
      
      if(isBarPlot)
      {
         // 3D bar plots represented by horizontal bar stroked and filled appropriately. For color-mapped bar plots,
         // the bar is filled with a color gradient representing that colormap.
         double hMI = legend.getSpacer().toMilliInches()/2;
         double strkW = plottable.getPSStrokeWidth();
         Point2D botLeft = new Point2D.Double(0, y-hMI/2);
         if(colorLUT==null) psDoc.renderRect(botLeft, len, hMI, strkW, true, true);
         else
         {
            psDoc.renderColormapGradient(botLeft, new Point2D.Double(len, y+hMI/2), colorLUT);
            psDoc.renderRect(botLeft, len, hMI, strkW, true, false);
         }
      }
      
      if(hasTraceLine())
      {
         // a trace line is shown for a 2D scatter plot ONLY in the "trendline" display mode, and its appearance is
         // defined by a component node
         if(is2D)
         {
            FGraphicNode lineStyle = ((ScatterPlotNode)plottable).getLineStyle();
            psDoc.startElement(lineStyle, lineStyle, true);
         }
         psDoc.renderLine(p0, p1);
         if(is2D) psDoc.endElement();
      }
      
      if(areSymbolsRendered())
      {
         // for 3D scatter plot, symbol marker type and styling is set by component symbol node
         SymbolNode sym = plottable.getSymbolNode();
         Marker m = (sym != null) ? sym.getType() : ((ScatterPlotNode)plottable).getSymbol();
         if(sym != null) psDoc.startElement(sym, sym, true);
         
         double symSz = legend.getSymbolSize().toMilliInches();
         if(symSz == 0) symSz = getSymbolSizeInMilliInches();
         
         if(!(isSizeBubble || isColorBubble))
         {
            if(mid) psDoc.renderAdornmentEx(m, len/2, y, symSz, symSz, 0, null, bkgFills[0], true);
            else
            {
               psDoc.renderAdornmentEx(m, 0, y, symSz, symSz, 0, null, bkgFills[0], true);
               psDoc.renderAdornmentEx(m, len, y, symSz, symSz, 0, null, bkgFills[0], true);
            }
         }
         else
         {
            // for bubble plots, we always draw two symbols, and the legend's "mid" flag is ignored. The size and/or
            // color of the two symbols vary to reflect the variation in the bubble plot itself.
            psDoc.renderAdornmentEx(m, 0, y, minSzScale*symSz, minSzScale*symSz, 0, null, bkgFills[0]);
            psDoc.renderAdornmentEx(m, len, y, symSz, symSz, 0, null, bkgFills[1]);

         }
         
         if(sym != null) psDoc.endElement();
      }

      psDoc.endElement();
   }
   
   /** A 2D scatter plot lacks a {@link SymbolNode}. The symbol size is set by a property on the node itself. */
   @Override double getSymbolSizeInMilliInches()
   {
      return(is2D ? ((ScatterPlotNode)plottable).getMaxSymbolSize().toMilliInches() : 
         super.getSymbolSizeInMilliInches());
   }
   
   /** 
    * The legend entry for the 2D scatter plot includes a trace line segment only if the regression line is drawn,
    * while a 3D scatter plot entry includes the line segment if stems or the "connect-the-dots" poly-line are drawn. 
    */
   @Override boolean hasTraceLine() 
   {  
      return(is2D ? ((ScatterPlotNode)plottable).getMode() == ScatterPlotNode.DisplayMode.TRENDLINE : 
         super.hasTraceLine()); 
   }

   /** Flag set for a 2D scatter plot, cleared for a 3D scatter plot. */
   boolean is2D = false;
   /** Flag set if scatter plot is in one of the bubble plot display modes in which symbol size varies. */
   boolean isSizeBubble = false;
   /** Flag set if scatter plot is in one of the bubble plot display mode in which symbol fill color varies. */
   boolean isColorBubble = false;

   /** Flag set only for a 3D scatter plot configured in one of two possible bar plot-like display modes. */
   boolean isBarPlot = false;
   /** The color lookup table applied to 3D scatter plot configured as a color-mapped bar plot. */
   ColorLUT colorLUT = null;

   /** 
    * Scale factor applied to max symbol size to get symbol size for the left-hand symbol (when size varies). Minimum
    * allowed value is 0.4 -- so that left-hand symbol doesn't get too small.
    */
   double minSzScale = 1;
   /** The background fills for the two symbols rendered in the legend entry. */
   BkgFill[] bkgFills = null;
   
   
}
