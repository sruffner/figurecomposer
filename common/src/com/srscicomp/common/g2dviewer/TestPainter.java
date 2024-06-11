package com.srscicomp.common.g2dviewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import com.srscicomp.common.g2dutil.BasicPainterStyle;
import com.srscicomp.common.g2dutil.CircularArcPainter;
import com.srscicomp.common.g2dutil.LineSegmentPainter;
import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dutil.MultiShapePainter;
import com.srscicomp.common.g2dutil.Painter;
import com.srscicomp.common.g2dutil.PainterStyle;
import com.srscicomp.common.g2dutil.PolylinePainter;
import com.srscicomp.common.g2dutil.ShapePainter;
import com.srscicomp.common.g2dutil.StringPainter;
import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dutil.StrokeJoin;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dutil.TextBoxPainter;
import com.srscicomp.common.ui.BkgFill;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.GenericFont;
import com.srscicomp.common.util.Utilities;

/**
 * @author sruffner
 */
class TestPainter implements RenderableModel, RootRenderable, Focusable
{
   private final static int NSTAGES = 14;

   private int stage = 0;

   /** stage 2 (TextBoxPainter) has bunch of substages to test out different alignments and two different fonts */
   private int substage = 0;

   private final static int NSUBSTAGES = 12;

   private final float WIDTH = 5000;
   private final float HEIGHT = 7000;
   private final int NPTS = 4000;

   private final List<Point2D> fourQuadPts = new ArrayList<Point2D>(4);
   private final List<Point2D> polyPath = new ArrayList<Point2D>(NPTS);
   private final List<Point2D> sinePath = new ArrayList<Point2D>(NPTS);
   private final List<Point2D> lineSegments = new ArrayList<Point2D>(8);
   private final List<Point2D> arcSegments = new ArrayList<Point2D>(8);
   private final List<Point2D> rectangles = new ArrayList<Point2D>(5*4);

   /**
    * Construct the test model.
    */
   public TestPainter()
   {
      super();

      fourQuadPts.add( new Point2D.Double(WIDTH/4, HEIGHT/4) );
      fourQuadPts.add( new Point2D.Double(3*WIDTH/4, HEIGHT/4) );
      fourQuadPts.add( new Point2D.Double(WIDTH/4, 3*HEIGHT/4) );
      fourQuadPts.add( new Point2D.Double(3*WIDTH/4, 3*HEIGHT/4) );

      // a bunch of random points distributed over 3/4 of the display area
      for(int i=0; i<NPTS; i++)
      {
         polyPath.add( new Point2D.Double(Math.random()*3*WIDTH/4 + WIDTH/8, Math.random()*3*HEIGHT/4 + HEIGHT/8) );
      }

      // a unit-amplitude sinusoid evaluated over [0..2PI], offset by +1.
      double deltaRad = 2*Math.PI/NPTS;
      double rad = 0;
      double y;
      AffineTransform txf = AffineTransform.getScaleInstance(3*WIDTH/(8*Math.PI), 3*HEIGHT/8);
      txf.preConcatenate(AffineTransform.getTranslateInstance(WIDTH/8, HEIGHT/8));
      for(int i=0; i<NPTS; i++)
      {
         double x = rad * 3 * WIDTH / (8 * Math.PI) + WIDTH/8;
         y = (Math.sin(rad) + 1) * 3 * HEIGHT / 8 + HEIGHT/8;
         sinePath.add( new Point2D.Double(x, y) );
         rad += deltaRad;
      }

      // some line segments to evaluate LineSegmentPainter
      lineSegments.add( new Point2D.Double(WIDTH/8, HEIGHT/4) );       // horiz L->R line at (W/4, H/4)
      lineSegments.add( new Point2D.Double(3*WIDTH/8, HEIGHT/4) );
      lineSegments.add( new Point2D.Double(3*WIDTH/4, HEIGHT/8) );     // vert B->T line at (3W/4, H/4)
      lineSegments.add( new Point2D.Double(3*WIDTH/4, 3*HEIGHT/8) );
      double delta = Math.sqrt(2) * WIDTH / 8;                         // diag BL->TR line at (W/4, 3H/4)
      lineSegments.add( new Point2D.Double(WIDTH/4 - delta, 3*HEIGHT/4 - delta) );
      lineSegments.add( new Point2D.Double(WIDTH/4 + delta, 3*HEIGHT/4 + delta) );
      lineSegments.add( new Point2D.Double(3*WIDTH/4 + delta, 3*HEIGHT/4 + delta) );  // diag TR->BL line at 
      lineSegments.add( new Point2D.Double(3*WIDTH/4 - delta, 3*HEIGHT/4 - delta) );  // (3W/4, 3H/4)

      // some arcs to evaluate CircularArcPainter -- in polar coords about (W/2, H/2)
      arcSegments.add( new Point2D.Double(-45, 500) );
      arcSegments.add( new Point2D.Double(45, 500) );
      arcSegments.add( new Point2D.Double(30, 1000) );
      arcSegments.add( new Point2D.Double(90, 1000) );
      arcSegments.add( new Point2D.Double(150, 1000) );
      arcSegments.add( new Point2D.Double(270, 1000) );
      arcSegments.add( new Point2D.Double(110, 2000) );
      arcSegments.add( new Point2D.Double(70, 2000) ); 

      // some rectangles with intervening ill-defined points -- for input to PolylinePainter
      double w = 0.1*WIDTH;
      double h = 0; 
      double x0 = 0.1*WIDTH;
      double y0 = HEIGHT/2;
      for(int i=0; i<5; i++)
      {
         h = 3*HEIGHT/8 - Math.random()*3*HEIGHT/4;
         rectangles.add( new Point2D.Double(x0, y0) );
         rectangles.add( new Point2D.Double(x0+w, y0) );
         rectangles.add( new Point2D.Double(x0+w, y0+h) );
         rectangles.add( new Point2D.Double(x0, y0+h) );
         rectangles.add( null );
         x0 += 0.15*WIDTH;
      }
      
   }

   public void nextStage()
   {
      if(stage == 2)
      {
         ++substage;
         if(substage >= NSUBSTAGES)
         {
            substage = 0;
            ++stage;
         }
      }
      else ++stage;

      if( stage >= NSTAGES ) stage = 0;
      gotClick = false;
      preparePainter();
      fireModelMutated();
      fireFocusChanged();
   }

   public String getCurrentStageLabel()
   {
      return( "Stage " + stage );
   }
 
   //
   // RenderableModelViewer maintenance
   //
   RenderableModelViewer rmviewer =  null;
   
   public boolean registerViewer(RenderableModelViewer viewer)
   {
      if(rmviewer != null || viewer == null) return(false);
      rmviewer = viewer;
      return(true);
   }

   public void unregisterViewer(RenderableModelViewer viewer)
   {
      if(rmviewer == viewer)
         rmviewer = null;
   }

   public RenderableModelViewer getViewer() { return(rmviewer); }
   
   private void fireModelMutated()
   {
      if(rmviewer != null) rmviewer.modelMutated(null);
   }

   private void fireFocusChanged()
   {
      if(rmviewer != null) rmviewer.focusChanged();
   }

   public RootRenderable getCurrentRootRenderable()
   {
      return( stage>=0 ? this : null);
   }

   public boolean isFocusSupported()
   {
      return(true);
   }

   /**
    * We use this flag so we can artificially change the focus between stages. When the stage changes, this flag is 
    * reset, and the focus is "hidden". As soon as the user clicks on the viewer, the flag is set, and the viewer is 
    * notified of a change in display focus.
    */
   private boolean gotClick = false;

   public Focusable getCurrentFocusable()
   {
      return((stage > 0 && gotClick) ? this : null);
   }

   public void pointClicked(Point2D p)
   {
      if(stage > 0 && !gotClick)
      {
         gotClick = true;
         fireFocusChanged();
      }
   }

   @Override public double getWidthMI() { return(WIDTH); }
   @Override public double getHeightMI() { return(HEIGHT); }
   @Override public Point2D getPrintLocationMI() { return( new Point2D.Double(1000,1000) ); }
   @Override public boolean hasTranslucentRegions() { return(false); }
   
   /**
    * The <code>Painter</code> that is configured to render the current stage.
    */
   private Painter painter = null;

   private void preparePainter()
   {
      // stage 0: empty model
      if(stage == 0) 
         painter = null;

      // stage 1: test MultiShapePainter
      if(stage == 1)
      {
         List<MultiShapePainter.PaintedShape> shapes = new ArrayList<MultiShapePainter.PaintedShape>();
         for(int i=0; i<100; i++) shapes.add(new PaintedShapeImpl());
         
         painter = new MultiShapePainter(shapes);
      }
      
      // stage 2: test TextBoxPainter. This includes a bunch of substages to test out different alignment scenarios
      // and two different fonts.
      if(stage == 2)
      {
         PainterStyle style = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, 
               substage < 6 ? FontStyle.PLAIN : FontStyle.BOLDITALIC, 
               substage < 6 ? 150.0f : 250.0f, 
               12f, null, null, null, 0, 0);
         String text = 
               "This is a test of the TextBoxPainter, a Painter implementation which "
               + "paints a block of text within a specified bounding box (if possible). "
               + "In this example, the bounding box is 3 inches wide and 2 inches tall, "
               + "and is outlined in red. In the different substages we test different "
               + "block alignments and font sizes. Box is clipped for larger font";
         
         TextBoxPainter tbPainter = new TextBoxPainter();
         tbPainter.setStyle(style);
         tbPainter.setText(text);
         tbPainter.setRotation(0);
         tbPainter.setBoundingBox(new Point2D.Double(WIDTH/2, HEIGHT/2), 3000, 2000, 0);
         
         // testing linear and radial gradient fills...
         tbPainter.setBackgroundFill(BkgFill.createAxialGradientFill(substage*30, Color.RED, Color.WHITE));
         if(substage == 0) 
            tbPainter.setBackgroundFill(BkgFill.createRadialGradientFill(25, 25, Color.WHITE, Color.RED));
         
         TextAlign ha = TextAlign.LEADING;
         TextAlign va = TextAlign.LEADING;
         switch(substage)
         {
         case 1 : case 7 : ha = TextAlign.TRAILING; break;
         case 2 : case 8 : ha = TextAlign.CENTERED; break;
         case 3 : case 9 : va = TextAlign.TRAILING; break;
         case 4 : case 10: va = TextAlign.CENTERED; break;
         case 5 : case 11: ha = TextAlign.CENTERED; va = TextAlign.CENTERED; break;
         }
         tbPainter.setAlignment(ha, va);
         
         
         // tbPainter.setClipped( substage >= 6 );
         
         painter = tbPainter;
      }
      
      // stage 3: test ShapePainter, drawing a randomly chosen marker at ten locations along a circle, setting each 
      // marker's rotation angle IAW the angular position of the marker in polar coordinates. NOTE that I'm trying
      // out the rounded linecap and linejoin properties, introduced as of v3.2.0.
      if(stage == 3)
      {
         List<Point2D> tenPts = new ArrayList<Point2D>(10);
         List<Float> angles = new ArrayList<Float>(10);
         double radius = Math.min(WIDTH, HEIGHT) * 0.5;
         for(int i=0; i<10; i++)
         {
            float angleDeg = (float) (360 * i) / 10;
            double rad = Math.toRadians(angleDeg);
            Point2D p = new Point2D.Double(radius + radius * Math.cos(rad), radius + radius * Math.sin(rad));
            tenPts.add(p);
            angles.add(angleDeg);
         }

         PainterStyle style= BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, FontStyle.ITALIC, 
               50f, 10f, StrokeCap.ROUND, StrokeJoin.ROUND, null, 0, 0x00ff0000);
         ShapePainter shapePainter = new ShapePainter();
         shapePainter.setStyle(style);
         shapePainter.setSize(100);
         shapePainter.setLocationProducer(tenPts);
         shapePainter.setRotationProducer(angles);
         
         Marker[] availableMarkers = Marker.values();
         int index = (int) (Math.random() * availableMarkers.length + 0.5);
         if(index < 0 || index >= availableMarkers.length) index = 0;
         shapePainter.setPaintedShape(availableMarkers[index]);

         painter = shapePainter;
      }

      // stage 4: test StringPainter. During rendering, it is used to render a bunch of string with different rotations 
      // and different alignments. We just allocate the painter and set its style here. Other properties are changed 
      // on the fly during rendering.
      if(stage == 4)
      {
         PainterStyle style = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, FontStyle.PLAIN, 
                  80.0f, 10f, null, null, null, 0, 0);
         painter = new StringPainter();
         painter.setStyle(style);
      }

      // stages 5: test ShapePainter with a larger, rotated shape. The rotation angle is randomly chosen.
      if(stage == 5)
      {
         PainterStyle style = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, FontStyle.ITALIC, 
               150f, 1f, StrokeCap.BUTT, StrokeJoin.BEVEL, null, 0x00ff0000, 0x00ffffff);
         ShapePainter sp = new ShapePainter(style, fourQuadPts, Marker.DIAMOND21, WIDTH/8, "Rotating ShapePainter");
         sp.setLabelRotation(-80);
         sp.setRotation( Math.random()*360 );
         painter = sp;
      }

      // stage 6: test ShapePainter with differential scaling in X and Y (previously, scaling was always the same in
      // both dimensions.
      if(stage == 6)
      {
         ShapePainter sp = new ShapePainter();
         PainterStyle style = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, FontStyle.ITALIC, 
               150f, 1f, StrokeCap.BUTT, StrokeJoin.BEVEL, null, 0, 0);
         sp.setStyle(style);
         sp.setBackgroundFill(BkgFill.createRadialGradientFill(25, 75, Color.WHITE, Color.RED));
         painter = sp;
      }
      
      // stage 7: use a PolylinePainter to draw a 5000-pt polyline
      if(stage == 7)
      {
         PainterStyle lineStyle = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, 
               FontStyle.PLAIN, 50f, 4f, StrokeCap.ROUND, StrokeJoin.ROUND, new float[] {100f,50f}, 0, 0x00ff0000);
         PolylinePainter polyPainter = new PolylinePainter(lineStyle, polyPath);
         polyPainter.setFilled(false);
         polyPainter.setAllowChunking(true);

         painter = polyPainter;
      }

      // stage 8: use a ShapePainter to draw diamonds at each point along the path that was drawn in stage 6 (the 
      // polyline and symbols are NOT rendered together of course!)
      if(stage == 8)
      {
         PainterStyle style = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, FontStyle.PLAIN, 
               50f, 3f, StrokeCap.BUTT, StrokeJoin.ROUND, null, 0, 0x00ffffff);
         painter = new ShapePainter(style, polyPath, Marker.DIAMOND, 50f, "");
      }

      // stage 9: evaluate effects of chunking by PolylinePainter. During rendering, the painter is used to draw 
      // the sinewave first with chunking, and then again w/o. The second sinewave is shifted WRT the first. Here we 
      // just set up the painter, initially allowing chunking.
      if(stage == 9)
      {
         PainterStyle lineStyle = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, 
               FontStyle.PLAIN, 50f, 10f, null, null, new float[] {100}, 0, 0x00ffffff);
         PolylinePainter polyPainter = new PolylinePainter(lineStyle, sinePath);
         polyPainter.setAllowChunking(true);
         painter = polyPainter;
      }

      // stage 10: test LineSegmentPainter
      if(stage == 10)
      {
         PainterStyle style = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, FontStyle.ITALIC, 
               50f, 20f, StrokeCap.SQUARE, StrokeJoin.MITER, new float[] {100f}, 0, 0x00ff0000);
         PainterStyle adornStyle = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, FontStyle.ITALIC, 
               50f, 8f, null, null, null, 0x000000ff, 0x00ff0000);
         LineSegmentPainter lsp = new LineSegmentPainter(style, lineSegments);
         lsp.setAdornment(adornStyle, Marker.RIGHTARROW, 200f, "F", true, true, false, true);

         painter = lsp;
      }

      // stage 11 -- test CircularArcPainter
      if(stage == 11)
      {
         PainterStyle style = BasicPainterStyle.createBasicPainterStyle( 
               BasicPainterStyle.getFontForPainter("Arial", GenericFont.SANSERIF, FontStyle.PLAIN, 50f), 10f, 
               new float[] {30f}, Color.BLACK, new Color(200,0,0,128) );
         PainterStyle adornStyle = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, FontStyle.ITALIC, 
               50f, 5f, null, StrokeJoin.ROUND, null, 0, 0x00ff0000);
         CircularArcPainter arcP = new CircularArcPainter(style, arcSegments, new Point2D.Double(WIDTH/2, HEIGHT/2));
         arcP.setAdornment(adornStyle, Marker.RIGHTDART, 200f, "A", true, true, false, true);
         arcP.setClosure(CircularArcPainter.Closure.SECTION);
         arcP.setReferenceRadius(1500);
         arcP.setFilled(true);

         painter = arcP;
      }

      // stage 12 : use PolylinePainter to stroke some rectangles
      if(stage == 12)
      {
         PainterStyle lineStyle = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, 
               FontStyle.PLAIN, 50f, 20f, null, StrokeJoin.ROUND, null, 0x00ff0000, 0x00003300);
         PolylinePainter polyPainter = new PolylinePainter(lineStyle, rectangles);
         polyPainter.setConnectionPolicy(PolylinePainter.ConnectPolicy.CLOSED);
         polyPainter.setFilled(true);

         painter = polyPainter;
      }
      
      // stage 13 : test accuracy of bounding-rect calculation in StringPainter for different-length strings. Here we
      // just set up the painter. Strings and locations are provided during render phase.
      if(stage == 13)
      {
         PainterStyle style = BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, FontStyle.PLAIN, 
                  100.0f, 12f, null, null, null, 0, 0);
         painter = new StringPainter();
         painter.setStyle(style);        
      }
   }

   /* (non-Javadoc)
    * @see com.srscicomp.common.g2dviewer.Renderable#render(java.awt.Graphics2D, com.srscicomp.common.g2dviewer.RenderTask)
    */
   public boolean render(Graphics2D g2d, RenderTask progressHook)
   {
      if(stage == 0 || painter == null) 
         return(true);
      else if(stage == 1 || stage == 2)
      {
         // MultiShapePainter and TextBoxPainter: after painter has rendered on canvas, highlight painter's bounds
         if(!painter.render(g2d, progressHook)) return(false);
         
         g2d.setColor(new Color(0,50,200,92));
         g2d.fill(painter.getBounds2D(null));
         
         // for TextBoxPainter test: draw a thin line on canvas near right edge of box (unless it's rotated)
         if(stage == 2)
         {
            g2d.setColor(Color.BLACK);
            g2d.drawLine(4000, 0, 4000, (int)HEIGHT);
         }
         
         return(true);
      }
      else if(stage == 4)
      {
         // in this stage the painter is StringPainter, and we reuse it to paint strings at different locations, with 
         // different rotation angles and text alignments.
         Line2D line = new Line2D.Double();
         g2d.setColor(Color.RED);
         g2d.setStroke( new BasicStroke(1f) );
 
         StringPainter textPainter = (StringPainter) painter;

         TextAlign[] hAlign = new TextAlign[] {
               TextAlign.LEADING, TextAlign.CENTERED, TextAlign.TRAILING, TextAlign.LEADING, TextAlign.LEADING
         };
         TextAlign[] vAlign = new TextAlign[] {
               TextAlign.TRAILING, TextAlign.TRAILING, TextAlign.TRAILING, TextAlign.CENTERED, TextAlign.LEADING
         };
         String[] strings = new String[] {
               "LEFT+Botp", "CENTER+Bot", "RIGHT+Bot", "LEFT+Middlep", "LEFT+Top"
         };

         double[] rots = new double[] {0, 45, 90, 135, 180, 225, 270, 315};

         double x = 0;
         double dx = WIDTH/strings.length;

         Color highlight = new Color(0,50,200,128);

         Point2D p = new Point2D.Double();
         for(int i=0; i<strings.length; i++)
         {
            textPainter.setAlignment(hAlign[i], vAlign[i]);

            double y = 0.9*HEIGHT;
            double dy = 0.1*HEIGHT;
            for(int j=0; j<rots.length; j++)
            {
               line.setLine(x, y, x+dx, y);
               g2d.draw(line);
               line.setLine(x, y-0.5*HEIGHT, x, y+0.5*HEIGHT);
               g2d.draw(line);

               p.setLocation(x, y);
               textPainter.setTextAndLocation(strings[i], p);
               textPainter.setRotation(rots[j]);
               if( !textPainter.render(g2d, progressHook) )
                  return(false);

               g2d.setColor(highlight);
               textPainter.invalidateBounds();
               g2d.fill( textPainter.getBounds2D(null) );
               g2d.setColor(Color.RED);

               y -= dy;
            }
            x += dx;
         }
         
         return(true);
      }
      else if(stage == 6)
      {
         // in this stage a ShapePainter is used to draw the same shape at eight different locations, but with 
         // different widths and heights
         // NOTE: Unresolved -- This stage takes unusually long to render when the shape's background fill is a
         // radial gradient. Yet, when I create a similar kind of figure in FC, it's very fast. Don't know why.
         ShapePainter sp = (ShapePainter) painter;
         Point2D p = new Point2D.Double();
         List<Point2D> onePt = new ArrayList<Point2D>();
         onePt.add(p);
         sp.setLocationProducer(onePt);

         for(int i=0; i<8; i++)
         {
            float w = (i<4) ? WIDTH/4 : WIDTH/i;
            float h = (i<4) ? WIDTH/(4+i) : WIDTH/4;
            p.setLocation((i<4) ? WIDTH/3 : 2*WIDTH/3, ((i%4) + 1) * HEIGHT / 5);
            
            sp.setSize(w,h);
            sp.setTextLabel(String.format("W=%d, H=%d", (int) w, (int) h));
            if(!sp.render(g2d,  progressHook)) return(false);
         }
         
         return(true);
      }
      else if(stage == 9)
      {
         // in this stage, we use the same polyline painter to render a sinewave with chunking, then without
         PolylinePainter polyPainter = (PolylinePainter) painter;
         if( !polyPainter.render(g2d, progressHook) )
            return(false);

         // shift all points left and down by 0.1in, then draw w/o chunking, then put points back!
         for(Point2D p : sinePath) p.setLocation( p.getX() + 100, p.getY() - 100);
         polyPainter.setAllowChunking(false);
         boolean ok = polyPainter.render(g2d, progressHook);
         for(Point2D p : sinePath) p.setLocation( p.getX() - 100, p.getY() + 100);
         if(!ok) return(false);

         // put vertical red lines at every 500th point
         g2d.setColor(Color.RED);
         Line2D line = new Line2D.Double();
         for( int i=0; i<NPTS; i+=500)
         {
            Point2D p = sinePath.get(i);
            line.setLine(p.getX(), p.getY()-500, p.getX(), p.getY()+500);
            g2d.draw(line);
         }
         return(true);
      }
      else if(stage == 13)
      {
         // similar to stage 3, but we paint a bunch of strings of different lengths, trying to assess accuracy of
         // text bounding rect..
         StringPainter textPainter = (StringPainter) painter;

         String[] strings = new String[] {
            "Instruction",
            "Instruction-test",
            "Instruction-test interval (seconds)",
            "Instruction-test interval (seconds) - Instruction-test",
            "Instruction-test interval (seconds) - Instruction-test interval (seconds)",
            "Instruction-test interval (seconds) - Instruction-test interval (seconds) - Instruction-test interval (seconds)"
         };

         double dy = HEIGHT/(strings.length + 1);

         Color highlight = new Color(0,50,200,128);

         Point2D p = new Point2D.Double();
         for(int i=0; i<strings.length; i++)
         {
            p.setLocation(0.05*WIDTH, (i+1)*dy);
            textPainter.setTextAndLocation(strings[i], p);
            if( !textPainter.render(g2d, progressHook) )
               return(false);
            g2d.setColor(highlight);
            textPainter.invalidateBounds();
            g2d.fill( textPainter.getBounds2D(null) );
         }
         
         return(true);
      }
      else
      {
         // in all other stages, we just let the prepared painter render itself
         return(painter.render(g2d,progressHook));
      }
   }

   /**
    * This merely returns the last computed rendering bounds for the current painter, if there is one.
    * 
    * @see Focusable#getFocusShape(Graphics2D)
    */
   public Shape getFocusShape(Graphics2D g2d)
   {
      if(painter != null)
         return(painter.getBounds2D(null));
      else
         return(null);
   }

   /**
    * <code>TestPainter</code> does not support interactive repositioning via mouse drags on the viewer canvas.
    * 
    * @see Focusable#canMove()
    */
   public boolean canMove()
   {
      return(false);
   }

   /**
    * <code>TestPainter</code> does not support interactive repositioning via mouse drags on the viewer canvas.
    * 
    * @see RenderableModel#moveFocusable(double, double)
    */
   public void moveFocusable(double dx, double dy)
   {
   }

   
   private final static Color[] COLORCHOICES = new Color[] { 
      Color.RED, Color.GRAY, Color.GREEN, Color.BLACK, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA,
      Color.LIGHT_GRAY, Color.ORANGE, Color.PINK, Color.WHITE, Color.DARK_GRAY
   };
   
   private final static float[][] STROKEPATCHOICES = new float[][] {
      new float[] {30}, new float[] {10, 30}, new float[] {30, 30}
   };
   
   private final static Marker[] MARKERCHOICES = new Marker[] {
      Marker.CIRCLE, Marker.BOX, Marker.OVAL12, Marker.OVAL21, Marker.DIAMOND, Marker.UPTRIANGLE, Marker.DOWNTRIANGLE,
      Marker.LEFTTRIANGLE, Marker.RIGHTTRIANGLE, Marker.DART, Marker.STAR, Marker.TEE, Marker.ARROW
   };
   
   /** 
    * A random painted shape. The shape chosen, its location and size, its fill color, and selected stroking properties
    * are all randomly selected at construction time. Specifically for testing {@link MultiShapePainter}.
    * @author sruffner
    */
   private class PaintedShapeImpl implements MultiShapePainter.PaintedShape
   {
      PaintedShapeImpl()
      {
         int len = COLORCHOICES.length;
         int idx = Utilities.rangeRestrict(0, len-1, (int) (Math.random() * len + 0.5));
         fillColor = COLORCHOICES[idx];
         
         idx = Utilities.rangeRestrict(0, len-1, (int) (Math.random() * len + 0.5));
         strokeColor = COLORCHOICES[idx];
         
         len = 3;
         idx = Utilities.rangeRestrict(0, len-1, (int) (Math.random() * len + 0.5));
         float sw = (len==0) ? 5 : (len==1 ? 10 : 20);
         
         len = STROKEPATCHOICES.length;
         idx = Utilities.rangeRestrict(0, len-1, (int) (Math.random() * len + 0.5));
         float[] strokePat = STROKEPATCHOICES[idx];
         
         style= BasicPainterStyle.createBasicPainterStyle("Arial", GenericFont.SANSERIF, FontStyle.ITALIC, 
               50f, sw, StrokeCap.ROUND, StrokeJoin.ROUND, strokePat, 0, 0);
         
         len = MARKERCHOICES.length;
         idx = Utilities.rangeRestrict(0, len-1, (int) (Math.random() * len + 0.5));
         GeneralPath gp = new GeneralPath();
         double sz = Math.random() * 900 + 100;
         PathIterator pi = MARKERCHOICES[idx].getDesignShape().getPathIterator(AffineTransform.getScaleInstance(sz,sz));
         gp.append(pi, false);
         shape = gp;
         isClosed = MARKERCHOICES[idx].isClosed();
         
         location = new Point2D.Double(Math.random()*3*WIDTH/4 + WIDTH/8, Math.random()*3*HEIGHT/4 + HEIGHT/8);
      }
      
      public Font getFont() { return(style.getFont()); }
      public Color getFillColor() { return(fillColor); }
      public Color getStrokeColor() { return(strokeColor); }
      public Stroke getStroke(float dashPhase) { return(style.getStroke(dashPhase)); }
      public double getFontSize() { return(style.getFontSize()); }
      public double getStrokeWidth() { return(style.getStrokeWidth()); }
      public boolean isStrokeSolid() { return(style.isStrokeSolid()); }
      public boolean isStroked() { return(style.isStroked()); }

      public Shape getShape() { return(shape); }
      public Point2D getLocation() { return(location); }
      public Point2D getStemEnd() { return(null); }
      public PainterStyle getStemPainterStyle() { return(null); }
      public Paint getFillPaint() { return(null); }
      public boolean isClosed() { return(isClosed); }
      
      private final PainterStyle style;
      private final Shape shape;
      private final boolean isClosed;
      private final Point2D location;
      private final Color fillColor;
      private final Color strokeColor;
   }
}
