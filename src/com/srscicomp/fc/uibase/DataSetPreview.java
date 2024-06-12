package com.srscicomp.fc.uibase;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Transparency;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.srscicomp.common.g2dutil.BasicPainterStyle;
import com.srscicomp.common.g2dutil.Painter;
import com.srscicomp.common.g2dutil.PainterStyle;
import com.srscicomp.common.g2dutil.PolylinePainter;
import com.srscicomp.common.g2dutil.ShapePainter;
import com.srscicomp.common.g2dutil.StringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.AbstractRenderableModel;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.g2dviewer.RootRenderable;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.GenericFont;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.data.DataSet.Fmt;
import com.srscicomp.fc.fig.ColorLUT;

/**
 * <b>DataSetPreview</b> is a quick-and-dirty preview of the contents of a single <i>Figure Composer</i>-compatible
 * data set, {@link DataSet}. It implements {@link AbstractRenderableModel} so that it may be displayed in a {@link 
 * Graph2DViewer}. The model consists of a single {@link RootRenderable} node, which is the model itself.
 * 
 * <p>The data set is rendered within a nominal 4in-by-4in viewport in a manner appropriate for the dataset's format. X 
 * increases rightward and Y upward in this viewport. All data is clipped to appear within a 3in square the bottom-left 
 * corner of which rests at (0.5in, 0.5in). Simple labeled axes appear to the left of and below this "data box", and a 
 * brief description of the data set appears centered above it.
 * <ul>
 * <li>{@link Fmt#PTSET} and {@link Fmt#SERIES}: Data set is rendered as a single blue poly-line, with
 * gaps marking any undefined data points in the set; if the set includes error data, error bars are drawn in gray.</li>
 * <li>{@link Fmt#MSET} and {@link Fmt#MSERIES}: These collection sets are rendered as a single blue
 * poly-line, with gaps separating the individual data sets in the collection.</li>
 * <li>{@link Fmt#XYZIMG}: A gray-scale color map is applied to render this 3D data set as a "heat map" in the
 * 3-inch data window.</li>
 * <li>{@link Fmt#RASTER1D}: Only an x-axis is drawn, and the individual rasters in the data set are spaced
 * vertically in the viewport and rendered as a series of blue vertical hash marks; the hash mark size and the vertical 
 * spacing is adjusted so that all of the rasters fit within the data window.</p>
 * <li>{@link Fmt#XYZSET} and {@link Fmt#XYZWSET}: No preview is available for these data sets.</p>
 * 
 * <p><b>DataSetPreview</b> does not support the notion of display focus, nor any kind of interaction with the view.</p>
 * @author sruffner
 */
public class DataSetPreview extends AbstractRenderableModel implements RootRenderable
{
   /** Construct an empty data set preview. */
   public DataSetPreview()
   {
      ds = null;
      rmviewer = null;
   }
   
   /** 
    * Construct a preview of the specified data set. 
    * @param ds The data set to be rendered.
    */
   public DataSetPreview(DataSet ds)
   {
      this.ds = ds;
      rmviewer = null;
   }

   /** The data set currently rendered. */
   private DataSet ds = null;

   /**
    * Get the data set rendered in this preview.
    * @return The currently rendered data set, or null if there is none.
    */
   public DataSet getDataSet() { return(ds); }
   
   /**
    * Set the data set rendered in this preview. If the model is currently registered with a viewer, this method will 
    * trigger an update of the rendered view.
    * @param ds The data set to be rendered. If null, the view will be blank.
    */
   public void setDataSet(DataSet ds)
   {
      this.ds = ds;
      updatePainters = true;
      if(rmviewer != null) rmviewer.modelMutated(null);
   }
   
   public RootRenderable getCurrentRootRenderable() { return(this); }
   
   /** The fixed size of the preview's square viewport, in milli-in (= 4000). */
   private final static float FIXEDSZ = 4000;
   
   /** The fixed size of the square within which all data is drawn, in milli-in (= 3000). */
   private final static float DATABOXSZ = 3000;
   
   @Override public double getHeightMI() { return(FIXEDSZ); }
   @Override public Point2D getPrintLocationMI() { return null; }
   @Override public double getWidthMI() { return(FIXEDSZ); }
   @Override public boolean hasTranslucentRegions() { return(false); }
   
   /** This flag is set whenever the data set changes, in which case we must rebuild rendering infrastructure. */
   private boolean updatePainters = true;
   
   /** Index of first painter in painter list that should be clipped to the data box. */
   private int firstClipIndex = -1;
   
   @Override public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(updatePainters)
      {
         prepareView(g2d.getDeviceConfiguration());
         updatePainters = false;
      }
      if(!task.updateProgress()) return(false);
      
      for(int i=0; i<painters.size(); i++)
      {
         if(i == firstClipIndex) g2d.clip(new Rectangle2D.Float(500, 500, DATABOXSZ, DATABOXSZ));
         if(!painters.get(i).render(g2d, task)) return(false);
      }
      
      if(heatMapImg != null)
         g2d.drawImage(heatMapImg, 500, 500, (int)DATABOXSZ, (int)DATABOXSZ, null);
      return(true);
   }

   
   //
   // Rendering infrastructure
   //
   
   /** 
    * Painter style used in most rendering by within the data set preview: default font, 10-mil solid blue stroke, 
    * black text/fill color. 
    */
   private static PainterStyle style = 
      BasicPainterStyle.createBasicPainterStyle(null, 10, null, Color.BLUE, Color.BLACK);
   
   /** 
    * Painter style for axis lines and error bars: default font, 15-mil solid gray stroke with butt ends and mitered
    * joins, black text/fill color. 
    */
   private static PainterStyle axisStyle = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, 
            FontStyle.PLAIN, 8*1000/72, 15, null, null, null, 0x00808080, 0);

   
   /** A gray-scale color map for rendering the XYZIMG format (packed ARGB color model, with A in MSByte). */
   private final static int[] GRAYMAP;
   static
   {
      GRAYMAP = new int[256];
      for(int i=0; i<GRAYMAP.length; i++)
         GRAYMAP[i] = 0xFF000000 | ((i<<16) + (i<<8) + i);
   }
   
   /** Prepared list of painters that draw all or a portion of the rendered view. */
   private List<Painter> painters = new ArrayList<Painter>();
   
   /** "Heat map" image of an XYZIMG data set, using a gray-scale color map. */
   private BufferedImage heatMapImg = null;
   
   /**
    * Helper method called prior to rendering to prepare the painters that actually render the data set. These include
    * <b>StringPainter</b>s to draw any axis labels and the information string centered near the top of the view
    * (this provides a brief description of the data set) and a poly-line painter that draws the axis lines. The data 
    * rendering varies with format:
    * <ul>
    * <li>{@link Fmt#PTSET}: A 0.08-in blue circle is drawn at each data point location. If the set has standard
    * deviation data, error bars are drawn as gray lines.</li>
    * <li>{@link Fmt#SERIES}: Rendered as a single polyline. If the series includes standard deviation data, two
    * additional gray "+1STD" and "-1STD" polylines are also rendered. However, if the series includes 50 or fewer 
    * points, it is rendered in the same manner as a <b>PTSET</b>.</li>
    * <li>{@link Fmt#MSET} and {@link Fmt#MSERIES}: Each point set or series in the collection is
    * rendered as a separate polyline.</li>
    * <li>{@link Fmt#RASTER1D}. Rendered as "spike train" rasters.</li>
    * <li>{@link Fmt#XYZIMG}. A "heat map" image of the dataset is prepared using a grayscale colormap.</li>
    * <li>{@link Fmt#XYZSET} and {@link Fmt#XYZWSET}: No preview available.</li>
    * </ul>
    * <p><i>If the data set is large, it can take a significant amount of time to render -- so the rendering may be 
    * sub-sampled to save time -- since we're only drawing a "preview" here. However, the raster and image formats are 
    * not sub-sampled.</i></p>
    * @param gc The graphics configuration for the viewer in which this data set preview is rendered. It is needed to 
    * prepare a compatible buffered image for the heat map rendering of a {@link Fmt#XYZIMG} set.
    */
   private void prepareView(GraphicsConfiguration gc)
   {
      painters.clear();
      if(heatMapImg != null) {heatMapImg.flush(); heatMapImg = null;}
      
      // info string at top of viewport
      StringPainter sp = new StringPainter();
      sp.setStyle(axisStyle);
      String text = (ds==null) ? "No Dataset Selected" : ds.getInfo().getShortDescription();
      sp.setTextAndLocation(text, new Point2D.Float(FIXEDSZ/2, FIXEDSZ-250));
      sp.setAlignment(TextAlign.CENTERED, TextAlign.TRAILING);
      painters.add(sp);
      if(ds == null) return;
      
      // if dataset is empty, write "EMTPY" in the center of the view
      if(ds.isEmpty())
      {
         sp = new StringPainter();
         sp.setStyle(axisStyle);
         sp.setTextAndLocation("EMPTY", new Point2D.Float(FIXEDSZ/2, FIXEDSZ/2));
         painters.add(sp);
         return;
      }

      // no preview available for the 3D and 4D point set formats
      Fmt fmt = ds.getFormat();
      if(fmt == Fmt.XYZSET || fmt == Fmt.XYZWSET)
      {
         sp = new StringPainter();
         sp.setStyle(axisStyle);
         sp.setTextAndLocation("No preview available.", new Point2D.Float(FIXEDSZ/2, FIXEDSZ/2));
         painters.add(sp);
         return;         
      }
      
      // painter for the axes lines, including ticks at the endpoints
      PolylinePainter pp = new PolylinePainter();
      pp.setStyle(axisStyle);
      List<Point2D> axesVerts = new ArrayList<Point2D>();
      axesVerts.add(new Point2D.Float(500, 350));
      axesVerts.add(new Point2D.Float(500, 400));
      axesVerts.add(new Point2D.Float(3500, 400));
      axesVerts.add(new Point2D.Float(3500, 350));
      if(fmt != Fmt.RASTER1D)
      {
         axesVerts.add(new Point2D.Float(Float.NaN, Float.NaN));
         axesVerts.add(new Point2D.Float(350, 500));
         axesVerts.add(new Point2D.Float(400, 500));
         axesVerts.add(new Point2D.Float(400, 3500));
         axesVerts.add(new Point2D.Float(350, 3500)); 
      }
      pp.setLocationProducer(axesVerts);
      painters.add(pp);
      
      // painters for the axis labels indicating data ranges
      sp = new StringPainter(Utilities.toString(ds.getXMin(), 4, -1), new Point2D.Float(500,320));
      sp.setStyle(axisStyle);
      sp.setAlignment(TextAlign.LEADING, TextAlign.LEADING);
      painters.add(sp);
      sp = new StringPainter(Utilities.toString(ds.getXMax(), 4, -1), new Point2D.Float(3500,320));
      sp.setStyle(axisStyle);
      sp.setAlignment(TextAlign.TRAILING, TextAlign.LEADING);
      painters.add(sp);
      if(fmt != Fmt.RASTER1D)
      {
         sp = new StringPainter(Utilities.toString(ds.getYMin(), 4, -1), new Point2D.Float(320,500));
         sp.setStyle(axisStyle);
         sp.setRotation(90);
         sp.setAlignment(TextAlign.LEADING, TextAlign.TRAILING);
         painters.add(sp);
         sp = new StringPainter(Utilities.toString(ds.getYMax(), 4, -1), new Point2D.Float(320,3500));
         sp.setStyle(axisStyle);
         sp.setRotation(90);
         sp.setAlignment(TextAlign.TRAILING, TextAlign.TRAILING);
         painters.add(sp);
      }
      
      if(fmt == Fmt.XYZIMG)
      {
         heatMapImg = gc.createCompatibleImage(ds.getDataBreadth(), ds.getDataLength(), Transparency.TRANSLUCENT);
         if(!ds.prepareImage(heatMapImg, null, false, new ColorLUT().getLUT()))
         {
            if(heatMapImg != null) {heatMapImg.flush(); heatMapImg = null;}
            sp = new StringPainter("Unable to prepare heat map", new Point2D.Float(FIXEDSZ/2, FIXEDSZ/2));
            sp.setStyle(axisStyle);
            sp.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
            painters.add(sp);
         }
      }
      else
      {
         firstClipIndex = painters.size();
         if(fmt == Fmt.PTSET || (fmt == Fmt.SERIES && ds.getDataSize(-1) <= 50))
         {
            ShapePainter shapeP = new ShapePainter();
            shapeP.setStyle(style);
            shapeP.setSize(80);
            shapeP.setFilled(false);
            shapeP.setLocationProducer(new PolylineProducer());
            painters.add(shapeP);
            if(ds.hasErrorData())
               painters.add(new PolylinePainter(axisStyle, new ErrorBarProducer()));
         }
         else if(fmt == Fmt.SERIES)
         {
            painters.add(new PolylinePainter(style, new PolylineProducer()));
            if(ds.hasErrorData())
               painters.add(new PolylinePainter(axisStyle, new StdDevProducer()));
         }
         else
            painters.add(new PolylinePainter(style, new PolylineProducer()));
      }
   }
   
   private final static int LIMIT_PTSET = 500;
   private final static int LIMIT_MSET = 1000;
   
   /**
    * <b>PolylineProducer</b> is a helper class that generates the points for the utility painter that renders the 
    * current data set. It provides an iterator over the data points in that set, in a manner appropriate for the data 
    * format, and transformed into the model's logical coordinate system so that the entire data set fits within the 
    * rectangle [500 3500 500 3500] (with y-axis increasing upward). It serves both as the iterator implementation and 
    * the iterator provider (it simply provides fresh copies of itself).
    * 
    * <p>For the Cartesian 2D formats, large data sets are sub-sampled so that the total number of data points rendered 
    * does not exceed {@link #LIMIT_PTSET} for a single-set format, or {@link #LIMIT_MSET} for a collection format.</p>
    * 
    * <p>Iterators provided do <n>not</n> support removal of a data point. Also, the class is <b>not</b> thread-safe. 
    * Since it is used to iterate over data during rendering (which occurs in a background thread), this could be 
    * problematic!</p>
    * @author  sruffner
    */
   private class PolylineProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      final DataSet set;
      final float xMin;
      final float xRng;
      final float yMin;
      final float yRng;
      final float hashHt;
      final float hashSpacer;
      int nPtsSoFar;
      int nSetsSoFar;
      int nSkip;
      int which;  // RASTER1D only: 0 for bottom of hash mark, 1 for top, 2 for undefined pt between hash marks!
      boolean insertGap;
      Point2D pCurrent;

      public Iterator<Point2D> iterator() { return(new PolylineProducer()); }

      PolylineProducer()
      {
         set = ds;
         Fmt fmt = set.getFormat();
         xMin = ds.getXMin();
         xRng = ds.getXMax() - xMin;
         yMin = ds.getYMin();
         yRng = ds.getYMax() - yMin;
         hashSpacer = (fmt == Fmt.RASTER1D) ? DATABOXSZ/Math.max(1, set.getNumberOfSets()) : 0;
         hashHt = (fmt == Fmt.RASTER1D) ? (hashSpacer * 0.8f) : 0;

         // subsample the Cartesian formats and XYZSET to keep rendering time under control
         nSkip = 1;
         if(fmt != Fmt.RASTER1D && fmt != Fmt.XYZIMG)
         {
            int nTotal = 0;
            for(int i=0; i<ds.getNumberOfSets(); i++) nTotal += ds.getDataSize(i);
            int nMax = (fmt == Fmt.PTSET || fmt == Fmt.SERIES) ? LIMIT_PTSET : LIMIT_MSET;
            if(nTotal > nMax)
            {
               nSkip = nTotal/nMax;
               if(nSkip < 2) nSkip = 2;
            }
         }
         
         nPtsSoFar = 0;
         nSetsSoFar = (set.getFormat() == Fmt.XYZIMG) ? 1 : 0;  // this producer not designed for XYZIMG!
         
         // skip to the first raster that has at least one sample
         if(fmt == Fmt.RASTER1D)
         {
            while(nSetsSoFar < set.getNumberOfSets())
            {
               if(set.getDataSize(nSetsSoFar) > 0) break;
               ++nSetsSoFar;
            }
         }
         
         which = 0;
         insertGap = false;
         pCurrent = new Point2D.Float();
      }

      public boolean hasNext() { return(nSetsSoFar < set.getNumberOfSets()); }

      public Point2D next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         // between point sets or raster hash marks, insert undefined point to create a gap in the rendered polyline
         if(insertGap || which == 2)
         {
            pCurrent.setLocation(Float.NaN, Float.NaN);
            insertGap = false;
            which = 0;
            return(pCurrent);
         }
         
         float x = DATABOXSZ * (set.getX(nPtsSoFar, nSetsSoFar) - xMin) / xRng;
         float y = (hashSpacer != 0) ? (nSetsSoFar * hashSpacer + which*hashHt) : 
            DATABOXSZ * (set.getY(nPtsSoFar, nSetsSoFar) - yMin) / yRng;
         pCurrent.setLocation(x + 500, y + 500);
         
         if(hashSpacer != 0)
         {
            if(++which == 2) ++nPtsSoFar;
            if(nPtsSoFar >= set.getDataSize(nSetsSoFar)) 
            { 
               ++nSetsSoFar; 
               nPtsSoFar = 0; 
               
               // skip to the next raster that has at least one sample (if there is one!)
               while(nSetsSoFar < set.getNumberOfSets())
               {
                  if(set.getDataSize(nSetsSoFar) > 0) break;
                  ++nSetsSoFar;
               }
            }
         }
         else
         {
            nPtsSoFar += nSkip;
            if(nPtsSoFar >= set.getDataSize(nSetsSoFar))
            {
               ++nSetsSoFar;
               nPtsSoFar = 0;
               if(nSetsSoFar < set.getNumberOfSets()) insertGap = true;
            }
         }

         return(pCurrent);
      }

      public void remove()
      {
         throw new UnsupportedOperationException("Removal not supported by this iterator.");
      }
   }

   /**
    * <b>ErrorBarProducer</b> is a helper class that generates the points for the single {@link PolylinePainter} that 
    * renders error bars for a <b>PTSET</b> data set. It does not apply to any of the other data formats. It serves both
    * as the iterator implementation and the iterator provider (it simply provides fresh copies of itself). 
    * 
    * <p>Each point generated is transformed such that the actual data range maps onto the rectangle [500 3500 500 
    * 3500] in milli-inches, with y-axis increasing upward. Large point sets are sub=sampled so that the total number of 
    * data points at which error bars are rendered does not exceed {@link #LIMIT_PTSET}.</p> 
    * 
    * <p>Iterators provided by <b>ErrorBarProducer</b> do <b>not</b> support removal of a data point. Also, the class is
    * <b>not</b> thread-safe. Since it is used to iterate over data during rendering (which occurs in a background 
    * thread), this could be problematic!</p>
    * 
    * @author  sruffner
    */
   private class ErrorBarProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      final DataSet set;
      final float xMin;
      final float xRng;
      final float yMin;
      final float yRng;
      int nPtsSoFar;
      int nSkip;
      int which;  // [0..nEBarPts-1]
      int nEBarPts;
      boolean done;
      Point2D[] pEBarPts;

      public Iterator<Point2D> iterator() { return(new ErrorBarProducer()); }

      ErrorBarProducer()
      {
         set = ds;
         xMin = ds.getXMin();
         xRng = ds.getXMax() - xMin;
         yMin = ds.getYMin();
         yRng = ds.getYMax() - yMin;

         nPtsSoFar = 0;
         which = 0;
         nEBarPts = 0;
         Fmt f = set.getFormat();
         done = !((f == Fmt.PTSET) && set.hasErrorData());
         
         // subsample if necessary
         nSkip = 1;
         if(!done)
         {
            int sz = ds.getDataSize(0);
            if(sz > LIMIT_PTSET)
            {
               nSkip = sz / LIMIT_PTSET;
               if(nSkip < 2) nSkip = 2;
            }
         }
         
         pEBarPts = new Point2D[6];
         for(int i=0; i<pEBarPts.length; i++) pEBarPts[i] = new Point2D.Float();
         pEBarPts[2].setLocation(Float.NaN, Float.NaN);
         pEBarPts[5].setLocation(Float.NaN, Float.NaN);
      }

      public boolean hasNext() { return(!done); }

      public Point2D next()
      {
         if(done) throw new NoSuchElementException("Out of elements.");

         // time to generate endpoints of x- and/or y-error bars at next point in set
         if(which == 0)
         {
            while(nPtsSoFar < set.getDataSize(-1))
            {
               float x = 500 + DATABOXSZ * (set.getX(nPtsSoFar, -1) - xMin) / xRng;
               float y = 500 + DATABOXSZ * (set.getY(nPtsSoFar, -1) - yMin) / yRng;
               float xStd = DATABOXSZ * Math.abs(set.getXStdDev(nPtsSoFar)) / xRng;
               float yStd = DATABOXSZ * Math.abs(set.getYStdDev(nPtsSoFar)) / yRng;
               int xe = set.getXErrorBarStyle(nPtsSoFar);
               int ye = set.getYErrorBarStyle(nPtsSoFar);
               
               boolean hasXErr = false;
               boolean hasYErr = false;
               if(Utilities.isWellDefined(x) && Utilities.isWellDefined(y))
               {
                  hasXErr = ((Math.abs(xe) < 2) && xStd != 0 && Utilities.isWellDefined(xStd));
                  hasYErr = ((Math.abs(ye) < 2) && yStd != 0 && Utilities.isWellDefined(yStd));
               }
               if(!(hasXErr || hasYErr))
               {
                  nPtsSoFar += nSkip;
                  continue;
               }
               
               nEBarPts = (hasXErr && hasYErr) ? 6 : 3;
               if(hasXErr)
               {
                  pEBarPts[0].setLocation(x + ((xe == 0 || xe < 0) ? -xStd : 0), y);
                  pEBarPts[1].setLocation(x + ((xe > 0 || xe == 0) ? xStd : 0), y);
               }
               if(hasYErr)
               {
                  int j = hasXErr ? 3 : 0;
                  pEBarPts[j].setLocation(x, y + ((ye == 0 || ye < 0) ? -yStd : 0));
                  pEBarPts[j+1].setLocation(x, y + ((ye > 0 || ye == 0) ? yStd : 0));
               }
               break;
            }
            if(nPtsSoFar >= set.getDataSize(-1))
            {
               done = true;
               pEBarPts[0].setLocation(Float.NaN, Float.NaN);
            }
         }
         
         Point2D ret = pEBarPts[which];
         ++which;
         if(which >= nEBarPts) 
         {
            which = 0;
            nPtsSoFar += nSkip;
            done = (nPtsSoFar >= set.getDataSize(-1));
         }
         return(ret);
      }

      public void remove()
      {
         throw new UnsupportedOperationException("Removal not supported by this iterator.");
      }
   }

   /**
    * <b>StdDevProducer</b> is a helper class that generates points for a single {@link PolylinePainter} that renders
    * the +/-1STD poly-lines for a <b>SERIES</b> data set. It does not apply to any of the other data formats. It serves
    * both as the iterator implementation and the iterator provider (it simply provides fresh copies of itself). 
    * 
    * <p>Each point generated is transformed such that the actual data range maps onto the rectangle [500 3500 500 
    * 3500] in milli-inches, with y-axis increasing upward. Large series are sub-sampled so that the total number of 
    * points in each of the two poly-lines rendered does not exceed {@link #LIMIT_PTSET}.</p> 
    * 
    * <p>Iterators provided by <b>StdDevProducer</b> do <b>not</b> support removal of a data point. Also, the class is 
    * <b>not</b> thread-safe. Since it is used to iterate over data during rendering (which occurs in  a background 
    * thread), this could be problematic!</p>
    * 
    * @author  sruffner
    */
   private class StdDevProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      final DataSet set;
      final float xMin;
      final float xRng;
      final float yMin;
      final float yRng;
      int nPtsSoFar;
      int nSkip;
      int which;  // 1 for +1STD polyline, -1 for -1STD
      boolean done;
      boolean insertGap; // flag set to inser undefined point between the two polylines
      Point2D pCurrent;
      float[] fbuf = new float[] {0, 0, 0};  // to check for ill-defined coords or std dev
      
      public Iterator<Point2D> iterator() { return(new ErrorBarProducer()); }

      StdDevProducer()
      {
         set = ds;
         xMin = ds.getXMin();
         xRng = ds.getXMax() - xMin;
         yMin = ds.getYMin();
         yRng = ds.getYMax() - yMin;

         nPtsSoFar = 0;
         which = 1;
         insertGap = false; 
         Fmt f = set.getFormat();
         done = !((f == Fmt.SERIES) && set.hasErrorData());
         
         // subsample if necessary
         nSkip = 1;
         if(!done)
         {
            int sz = ds.getDataSize(0);
            if(sz > LIMIT_PTSET)
            {
               nSkip = sz / LIMIT_PTSET;
               if(nSkip < 2) nSkip = 2;
            }
         }
         
         pCurrent = new Point2D.Float();
      }

      public boolean hasNext() { return(!done); }

      public Point2D next()
      {
         if(done) throw new NoSuchElementException("Out of elements.");

         if(insertGap) 
         {
            pCurrent.setLocation(Float.NaN, Float.NaN);
            insertGap = false;
         }
         else
         {
            fbuf[0] = 500 + DATABOXSZ * (set.getX(nPtsSoFar, -1) - xMin) / xRng;
            fbuf[1] = 500 + DATABOXSZ * (set.getY(nPtsSoFar, -1) - yMin) / yRng;
            fbuf[2] = DATABOXSZ * Math.abs(set.getYStdDev(nPtsSoFar)) / yRng;
            if(Utilities.isWellDefined(fbuf))
               pCurrent.setLocation(fbuf[0], fbuf[1] + which*fbuf[2]);
            else
               pCurrent.setLocation(Float.NaN, Float.NaN);
            
            nPtsSoFar += nSkip;
            if(nPtsSoFar >= set.getDataSize(-1))
            {
               if(which == -1) done = true;
               else { which = -1; nPtsSoFar = 0; insertGap = true; }
            }
         }

         return(pCurrent);
      }

      public void remove()
      {
         throw new UnsupportedOperationException("Removal not supported by this iterator.");
      }
   }
}
