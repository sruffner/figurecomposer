package com.srscicomp.common.g2dviewer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.srscicomp.common.ui.GUIUtilities;
import com.srscicomp.common.ui.JPreferredSizePanel;
import com.srscicomp.common.ui.LocalFontEnvironment;
import com.srscicomp.common.ui.MainFrameShower;
import com.srscicomp.common.util.ContourGenerator;
import com.srscicomp.common.util.IDataGrid;
import com.srscicomp.common.util.Utilities;

/**
 * A test fixture for use while developing and debugging the {@link ContourGenerator} class.
 * 
 * @author sruffner
 */
class CGTestApp extends JFrame
{
   public static void main(String[] args)
   {
      GUIUtilities.initLookAndFeel();
      LocalFontEnvironment.initialize();

      final CGTestApp appFrame = new CGTestApp();

      appFrame.addWindowListener( new WindowAdapter() {
         public void windowClosing( WindowEvent e ) 
         {
            appFrame.onExit();
         }
      });

      Runnable runner = new MainFrameShower( appFrame );
      SwingUtilities.invokeLater( runner );
   }


    private Graph2DViewer viewer = null;

   public CGTestApp() throws HeadlessException
   {
      super("Test ContourGenerator");

      // the test model and the model viewer
      TestCGModel model = new TestCGModel();
      viewer = new Graph2DViewer();
      viewer.setModel(model);
      JPanel viewerPanel = new JPreferredSizePanel(new BorderLayout());
      viewerPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
      viewerPanel.add(viewer, BorderLayout.CENTER);

      add(viewerPanel);
      setMinimumSize(new Dimension(800,600));
   }

   public void onExit()
   {
      viewer.releaseResources();
      System.exit(0);
   }

   private static class TestCGModel implements RenderableModel, RootRenderable
   {
      TestCGModel()
      {
         contourGen = new ContourGenerator();
         dataGrid = new TestGrid4();
         contourGen.setData(dataGrid, new double[] {-80, -60, -40, -20, 0, 20, 40, 60, 80});
         scaleXToMI = (WIDTH-2000.0) / (dataGrid.getX1()-dataGrid.getX0());
         scaleYToMI = (HEIGHT-2000.0) / (dataGrid.getY1()-dataGrid.getY0());
      }
      
      @Override public boolean render(Graphics2D g2d, RenderTask task)
      {
         prepareContourPaths();
         
         // transforms from (x,y) data grid coordinates to rendering coordinates in milli-inches
         AffineTransform at = AffineTransform.getTranslateInstance(1000.0 - dataGrid.getX0()*scaleXToMI, 
               1000.0 - dataGrid.getY0()*scaleYToMI);
         at.scale(scaleXToMI, scaleYToMI);

         g2d.setStroke(new BasicStroke(4));
         g2d.setColor(Color.GRAY);
         g2d.draw(gridPath);
         
         g2d.setStroke(new BasicStroke(8));
         for(ContourGenerator.Contour c : contours)
         {
            g2d.setColor(c.isFillRegion() ? mapToColor(c.getLevel()) : Color.BLACK);
            GeneralPath gp = c.asPath();
            gp.transform(at);
            if(c.isFillRegion()) g2d.fill(gp);
            else g2d.draw(gp);
         }
         
         return(true);
      }

      @Override  public boolean registerViewer(RenderableModelViewer viewer)
      {
         if(rmviewer != null || viewer == null) return(false);
         rmviewer = viewer;
         return(true);
      }
      @Override public void unregisterViewer(RenderableModelViewer viewer)
      {
         if(rmviewer == viewer) rmviewer = null;
      }
      @Override public RenderableModelViewer getViewer() { return(rmviewer); }
      
      @Override public RootRenderable getCurrentRootRenderable() { return(this); }
      @Override public boolean isFocusSupported() { return(false); }
      @Override public Focusable getCurrentFocusable() { return(null); }
      @Override public void moveFocusable(double dx, double dy) {}
      @Override public void pointClicked(Point2D p) {}

      @Override public double getWidthMI() { return(WIDTH); }
      @Override public double getHeightMI() { return(HEIGHT); }
      @Override public Point2D getPrintLocationMI() { return(new Point2D.Double(1000,1000)); }
      @Override public boolean hasTranslucentRegions() { return(false); }
      
      private RenderableModelViewer rmviewer = null;
      private final static double WIDTH = 7500;
      private final static double HEIGHT = 7500;
      
      private IDataGrid dataGrid = null;
      private double scaleXToMI = 0;
      private double scaleYToMI = 0;
      private ContourGenerator contourGen = null;
      private List<ContourGenerator.Contour> contours = null;
      private GeneralPath gridPath = null;

      
      private void prepareContourPaths()
      {
         if(contours != null) return;
         
         contours = contourGen.generateContours(true, true);
         
         // the bounding box and data grid lines on which the contours are overlaid
         if(gridPath != null) gridPath.reset();
         else gridPath = new GeneralPath();
         
         // the horizontal grid lines
         double delta = (dataGrid.getY1() - dataGrid.getY0())/(dataGrid.getNumRows()-1);
         for(int i=0; i<dataGrid.getNumRows(); i++)
         {
            double y = dataGrid.getY0() + i*delta;
            gridPath.moveTo(dataGrid.getX0(), y);
            gridPath.lineTo(dataGrid.getX1(), y);
         }
         
         // the vertical grid lines
         delta = (dataGrid.getX1() - dataGrid.getX0())/(dataGrid.getNumCols()-1);
         for(int i=0; i<dataGrid.getNumCols(); i++)
         {
            double x = dataGrid.getX0() + i*delta;
            gridPath.moveTo(x, dataGrid.getY0());
            gridPath.lineTo(x, dataGrid.getY1());
         }

         // transforms from (x,y) data grid coordinates to rendering coordinates in milli-inches
         AffineTransform at = AffineTransform.getTranslateInstance(1000.0 - dataGrid.getX0()*scaleXToMI, 
               1000.0 - dataGrid.getY0()*scaleYToMI);
         at.scale(scaleXToMI, scaleYToMI);
         gridPath.transform(at);
        
         // bounding box, set a little bit outward from the grid
         gridPath.moveTo(950, 950);
         gridPath.lineTo(950, HEIGHT-950);
         gridPath.lineTo(WIDTH-950, HEIGHT-950);
         gridPath.lineTo(WIDTH-950, 950);
         gridPath.closePath();
      }

      /** A color map identical to FC's "JET" color map. Did not want to introduce dependency on datanav project. */
      int[] mapArray = null;
      
      /**
       * Map specified contour level to a particular color.
       * @param level The contour level.
       * @return The mapped color. If level is undefined, returns white.
       */
      private Color mapToColor(double level)
      {
         // create color LUT on first call
         if(mapArray == null)
         {
            mapArray = new int[256];
            for(int i=0; i<32; i++) mapArray[i] = 0xFF000000 | (128 + i*4);
            for(int i=0; i<64; i++) mapArray[i+32] = 0xFF0000FF | ((i*4)<<8);
            for(int i=0; i<64; i++) mapArray[i+96] = 0xFF00FF00 | ((i*4)<<16) | (255 - i*4);
            for(int i=0; i<64; i++) mapArray[i+160] = 0xFFFF0000 | ((255 - i*4)<<8);
            for(int i=0; i<32; i++) mapArray[i+224] = 0xFF000000 | ((255 - i*4)<<16);
         }
         
         Color c = Color.WHITE;
         if(Utilities.isWellDefined(level))
         {
            double z = (level-dataGrid.getMinZ()) * 256 / (dataGrid.getMaxZ()-dataGrid.getMinZ());
            int idx = Utilities.rangeRestrict(0, 255, ((int) (z + 0.5)));
            c = new Color(mapArray[idx]);
         }
         return(c);
      }
   }

}

/** A sample data grid for testing the contour generator. Z(x,y) = x * Math.sin(2*pi*y/100). */
final class TestGrid implements IDataGrid
{

   private final int nr = 100;
   private final int nc = 100;
   
   @Override public int getNumRows() { return(nr); }
   @Override public int getNumCols() { return(nc); }

   @Override public double getX0() { return(-100); }
   @Override public double getX1() { return(100); }

   @Override public double getY0() { return(-100); }
   @Override public double getY1() { return(100); }

   @Override public double getMinZ() { return(-100); }
   @Override public double getMaxZ() { return(100); }

   @Override public double getZ(int r, int c)
   {
      if(r<0 || r>=nr || c<0 || c>=nc) throw new IndexOutOfBoundsException();
      
      double x = -100.0 + ((double) c)*200.0/((double) (nc-1));
      double y = -100.0 + ((double) r)*200.0/((double) (nr-1));
      
      return(x * Math.sin(Math.PI*y/50));
   }
   
   
}

/** A sample data grid for testing the contour generator. Z(x,y) = 50*(sin(2*pi*x/100) + cos(2*pi*y/100)). */
final class TestGrid2 implements IDataGrid
{

   private final int nr = 100;
   private final int nc = 100;
   
   @Override public int getNumRows() { return(nr); }
   @Override public int getNumCols() { return(nc); }

   @Override public double getX0() { return(-100); }
   @Override public double getX1() { return(100); }

   @Override public double getY0() { return(-100); }
   @Override public double getY1() { return(100); }

   @Override public double getMinZ() { return(-100); }
   @Override public double getMaxZ() { return(100); }

   @Override public double getZ(int r, int c)
   {
      if(r<0 || r>=nr || c<0 || c>=nc) throw new IndexOutOfBoundsException();
      
      double x = -100 + ((double) c)*200.0/((double) (nc-1));
      double y = -100 + ((double) r)*200.0/((double) (nr-1));
      
      return(50*(Math.sin(2*Math.PI*x/100.0) + Math.cos(2*Math.PI*y/100.0)));
   }
}

/** 
 * A sample data grid for testing the contour generator: A 2D symmetricalgaussian function peaking at the origin, with
 * a std dev W = 40 in both X and Y directions: Z(x,y) = -100 + 200*exp(-[(x^2 + y^2) / (2*(W^2))])
 */
final class TestGrid3 implements IDataGrid
{

   private final int nr = 100;
   private final int nc = 100;
   
   @Override public int getNumRows() { return(nr); }
   @Override public int getNumCols() { return(nc); }

   @Override public double getX0() { return(-100); }
   @Override public double getX1() { return(100); }

   @Override public double getY0() { return(-100); }
   @Override public double getY1() { return(100); }

   @Override public double getMinZ() { return(-100); }
   @Override public double getMaxZ() { return(100); }

   @Override public double getZ(int r, int c)
   {
      if(r<0 || r>=nr || c<0 || c>=nc) throw new IndexOutOfBoundsException();
      
      double x = -100.0 + ((double) c)*200.0/((double) (nc-1));
      double y = -100 + ((double) r)*200.0/((double) (nr-1));
      double r2 = (x*x +y*y) / (2.0 * 1600.0);
      return(-100 + 200*Math.exp(-r2));
   }
}

/** 
 * A sample data grid for testing the contour generator. Z(x,y) = 50*(sin(2*pi*x/100) + cos(2*pi*y/100)), but with
 * a variety of NaN "holes" in the data.
 */
final class TestGrid4 implements IDataGrid
{

   private final int nr = 100;
   private final int nc = 100;
   
   @Override public int getNumRows() { return(nr); }
   @Override public int getNumCols() { return(nc); }

   @Override public double getX0() { return(-100); }
   @Override public double getX1() { return(100); }

   @Override public double getY0() { return(-100); }
   @Override public double getY1() { return(100); }

   @Override public double getMinZ() { return(-100); }
   @Override public double getMaxZ() { return(100); }

   @Override public double getZ(int r, int c)
   {
      if(r<0 || r>=nr || c<0 || c>=nc) throw new IndexOutOfBoundsException();
      
      double x = -100 + ((double) c)*200.0/((double) (nc-1));
      double y = -100 + ((double) r)*200.0/((double) (nr-1));
      
      // interior NaN holes
      if(r>=48 && r<=52 && c>=54 && c<=58) return(Double.NaN);
      if(r>=23 && r<=27 && c>=42 && c<=46) return(Double.NaN);
      if(r>=24 && r<=26 && c>=47 && c<=50) return(Double.NaN);
      if(r>=24 && r<=26 && c>=19 && c<=28) return(Double.NaN);
      if(r>=4 && r<=11 && c>=11 && c<=15) return(Double.NaN);
      
      // a NaN region in one corner of the grid
      if(Math.sqrt(r*r + c*c) < 6) return(Double.NaN);
      
      // a NaN on a grid edge
      if((r==0 || r==1) && c==64) return(Double.NaN);
      
      // a single NaN at a grid corner
      if(r==99 && c==0) return(Double.NaN);
      
      // NaN ring in the interior
      if((r==10 || r==20) && c>=75 && c<=85) return(Double.NaN);
      if((c==75 || c==85) && r>=10 && r<=20) return(Double.NaN);
      
      // NaN ring at the top left corner
      if((r==95 && c>=95) || (r>=95 && c==95)) return(Double.NaN);
      
      // two NaNs in a row separated by a well-defined point
      if((r==81 && c==11) || (r==81 && c==13)) return(Double.NaN);
      
      return(50*(Math.sin(2*Math.PI*x/100.0) + Math.cos(2*Math.PI*y/100.0)));
   }
}

