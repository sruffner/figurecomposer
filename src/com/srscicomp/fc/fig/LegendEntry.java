package com.srscicomp.fc.fig;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.srscicomp.common.g2dutil.BasicPaintableShape;
import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.ShapePainter;
import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.fc.fig.FGNPlottableData.GrpInfo;

/**
 * <b>LegendEntry</b> handles the details of rendering the legend entry for a data presentation node. It supports
 * several styles of legend entries: a trace line with or without symbols(s), a symbol or symbols with no trace line, 
 * and a rectangular bar. Other legend entry styles must be handled by a subclass implementation.
 * 
 * @author sruffner
 */
class LegendEntry
{
   /**
    * Construct the legend entry renderer for the specified data presentation node.
    * @param p The data presentation node. Cannot be null.
    * @param gi For grouped-data presentation nodes only (e.g., bar charts), this contains information (label, fill
    * color) about the particular data group represented by this legend entry. Null otherwise.
    */
   LegendEntry(FGNPlottable p, GrpInfo gi)
   {
      if(p == null) throw new IllegalArgumentException("Must specify data presentation node!");
      plottable = p;
      dataGrpInfo = gi;
   }
   
   /**
    * Get the legend entry label, which may or may not contain "styled text". See{ @link FGraphicNode#toStyledText()}
    * for a description of this format.
    * 
    * @return The label. If null or empty, the graph legend will use a generic label.
    */
   String getLabel() { return(dataGrpInfo != null ? dataGrpInfo.label : plottable.getTitle()); }
   
   /**
    * The legend container must compute an appropriate offset from the left-hand edge of each entry (where <i>x==0</i>) 
    * to the starting location for each entry label. Since some legend entries may have a symbol at the right end point 
    * of the legend entry, and symbol sizes can vary, the legend container calls this method on each legend entry to 
    * find the worst-case offset from the entry's left end-point to the right edge of the rectangle bounding the entry's
    * graphic rendering, exclusive of the label.
    * 
    * @param legend The legend container.
    * @return Offset in milli-inches from the left-hand end point of the legend entry to the right-hand edge of the
    * rectangle bounding the entry's graphic rendering.
    */
   double getOffsetToRightEdge(LegendNode legend)
   {
      if(legend == null) return(0);
      
      double len = legend.getEntryLengthInMilliInches();
      boolean mid = legend.getMid();
      double sw = plottable.getStrokeWidth();

      double ofs = 0;
      if(plottable.useBarInLegend() || !areSymbolsRendered()) 
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
   
   /**
    * Calculate the rectangle bounding marks made when rendering this legend entry, excluding the entry's label.
    * 
    * @param y The Y-coordinate for the left-hand starting point for the legend entry, in the legend's own viewport.
    * The X-coordinate of that point is always zero, while the Y-coordinate depends on the entry's ordinal position in
    * the legend.
    * @param legend The legend node.
    * @param r If not null, this is set to the bounding rectangle and returned. Else, a new rectangle is allocated.
    * @return The bounding rectangle.
    */
   Rectangle2D getRenderBounds(double y, LegendNode legend, Rectangle2D r)
   {
      Rectangle2D rOut = r;
      if(rOut != null) rOut.setFrame(0, 0, 0, 0);
      else rOut = new Rectangle2D.Double();
      if(legend == null) return(rOut);
      
      double len = legend.getEntryLengthInMilliInches();
      double spacer = legend.getSpacer().toMilliInches();
      boolean mid = legend.getMid();
      double sw = plottable.getStrokeWidth();

      if(plottable.useBarInLegend())
         rOut.setFrame(-sw/2, y - spacer/4 - sw/2, len + sw, spacer/2 + sw);
      else if(!areSymbolsRendered())
         rOut.setFrame(-sw/2, y - sw/2, len + sw, sw);
      else
      {
         double symSz = legend.getSymbolSize().toMilliInches();
         if(symSz == 0) symSz = getSymbolSizeInMilliInches();
         symSz += getSymbolStrokeWidthInMilliInches();
         
         double x = Math.min(-sw/2, mid ? (len-symSz)/2.0 : -symSz/2);
         double w = Math.abs(x) * 2 + ((mid && symSz > len) ? symSz : len);
         double h = Math.max(sw, symSz);
         rOut.setFrame(x, y-h/2, w, h);
      }
      
      return(rOut);
   }
   
   /**
    * Render this entry within the viewport of the specified legend.
    * 
    * @param g2d The current graphics context for rendering.
    * @param y Y-coordinate for this legend entry. Left-hand starting point is at (0,y) in the legend's own viewport.
    * @param legend The legend node container.
    */
   void render(Graphics2D g2d, double y, LegendNode legend)
   {
      if(g2d == null || legend == null) return;
      
      double len = legend.getEntryLengthInMilliInches();
      double spacer = legend.getSpacer().toMilliInches();
      boolean mid = legend.getMid();
      
      Point2D p0 = new Point2D.Double(0, y);
      Point2D p1 = new Point2D.Double(len, y);
      List<Point2D> pts = new ArrayList<Point2D>();
      pts.add(p0);
      ShapePainter shapePainter = new ShapePainter(plottable, pts, null, 1f, null);
      
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         if(plottable.useBarInLegend())
         {
             BasicPaintableShape rBar = 
               new BasicPaintableShape(new Rectangle2D.Double(0,-spacer/4, len, spacer/2), true, true, true);
            shapePainter.setPaintedShape(rBar);
            if(dataGrpInfo != null) shapePainter.setBackgroundFill(BkgFill.createSolidFill(dataGrpInfo.fillC));
            
            shapePainter.render(g2d, null);
         }
         else
         {
            if(hasTraceLine())
            {
               g2dCopy.setStroke(plottable.getStroke(0));
               g2dCopy.setColor(plottable.getStrokeColor());
               g2dCopy.draw(new Line2D.Double(0, y, len, y));
            }
            
            if(areSymbolsRendered())
            {
               if(mid) pts.set(0, new Point2D.Double(len/2, y));
               else pts.add(p1);
               
               double symSz = legend.getSymbolSize().toMilliInches();
               if(symSz == 0) symSz = getSymbolSizeInMilliInches();
               shapePainter.setSize((float) symSz);

               SymbolNode sym = plottable.getSymbolNode();
               shapePainter.setPaintedShape(sym.getType());
               shapePainter.setStyle(sym);
               shapePainter.setTextLabel(sym.getCharacter());
               
               shapePainter.render(g2dCopy, null);
            }
         }
      }
      finally { if(g2dCopy != null) g2dCopy.dispose(); }
   }
   
   /**
    * Prepare the Postscript language code fragment that renders this legend entry into the specified Postscript page
    * description document.
    * @param psDoc The Postscript page description document in which the entry and its legend container are rendered.
    * @param y Y-coordinate for this legend entry. Left-hand starting point is at (0,y) in the legend's own viewport.
    * @param legend The legend node container.
    */
   void toPostscript(PSDoc psDoc, double y, LegendNode legend)
   {
      if(psDoc == null || legend == null) return;
      
      double len = legend.getEntryLengthInMilliInches();
      double spacer = legend.getSpacer().toMilliInches();
      boolean mid = legend.getMid();
      
      Point2D p0 = new Point2D.Double(0, y);
      Point2D p1 = new Point2D.Double(len, y);
      
      psDoc.startElement(plottable, plottable, true);
      if(plottable.useBarInLegend())
      {
         p0.setLocation(0, y-spacer/4);
         Color fillC = (dataGrpInfo != null) ? dataGrpInfo.fillC : plottable.getFillColor();
         psDoc.renderRect(p0, len, spacer/2, fillC.getAlpha() == 0 ? null : fillC);
      }
      else
      {
         if(hasTraceLine()) psDoc.renderLine(p0, p1);
         
         if(areSymbolsRendered())
         {
            SymbolNode sym = plottable.getSymbolNode();
            if(sym != null) psDoc.startElement(sym, sym, true);
            
            Marker m = (sym != null) ? sym.getType() : Marker.CIRCLE;
            double symSz = legend.getSymbolSize().toMilliInches();
            if(symSz == 0) symSz = getSymbolSizeInMilliInches();
            String symChar = (sym != null) ? sym.getCharacter() : "";

            if(mid) psDoc.renderAdornment(m, symSz, len/2, y, 0, symChar);
            else psDoc.renderMultipleAdornments(new Point2D[] {p0,p1}, null, m, symSz, symChar);
            
            if(sym != null) psDoc.endElement();
         }
      }
      psDoc.endElement();
   }
   
   /**
    * Are marker symbols currently drawn for this legend entry? <i>Base class implementation returns true if the data
    * presentation node uses symbols ({@link FGNPlottable#usesSymbols()}, and {@link #getSymbolSizeInMilliInches()} is 
    * positive or the presentation node has a {@link SymbolNode} component with a non-empty symbol character ({@link 
    * SymbolNode#getCharacter()}.</i>
    * @return True if a symbol or symbols should be rendered with this legend entry.
    */
   boolean areSymbolsRendered()
   {
      if(!plottable.usesSymbols()) return(false);
      if(getSymbolSizeInMilliInches() > 0) return(true);

      SymbolNode sym = plottable.getSymbolNode();
      return(sym != null && sym.getCharacter().length() > 0);
   }
   
   /**
    * Get the size of the symbol rendered by the data presentation node represented by this legend entry. The size 
    * should NOT include the stroke width applied when drawing the symbol. If more than one symbol is drawn and they are
    * not the same size, method should return the largest symbol size. <i>Base class implementation returns the size of
    * the data presentation node's component {@link SymbolNode}; if it lacks that component, 0 is returned.</i>
    * @return The symbol size in milli-inches.
    */
   double getSymbolSizeInMilliInches()
   {
      SymbolNode sym = plottable.getSymbolNode();
      return(sym != null ? sym.getSizeInMilliInches() : 0);
   }
   
   /**
    * Get the stroke width applied to any symbol rendered in this legend entry. <i>Base class implementation returns the
    * stroke width for the data presentation node's component {@link SymbolNode}. If it lacks that component, it returns
    * the presentation node's stroke width.</i>
    * @return The symbol stroke with in milli-inches.
    */
   double getSymbolStrokeWidthInMilliInches()
   {
      SymbolNode sym = plottable.getSymbolNode();
      return((sym != null) ? sym.getStrokeWidth() : plottable.getStrokeWidth());
   }
   
   /**
    * Does this legend entry include a representative trace line segment? <i>Base class implementation returns true
    * as long as the data presentation node is stroked and its legend entry is NOT a rectangular bar.</i>
    * 
    * @return True if legend entry includes a trace line segment.
    */
   boolean hasTraceLine() { return(plottable.isStroked() && !plottable.useBarInLegend()); }
   
   /** The data presentation node represented by this legend entry. */
   FGNPlottable plottable = null;
   
   /** The data group represented by this legend entry -- for grouped presentation nodes only. */
   GrpInfo dataGrpInfo = null;
}
