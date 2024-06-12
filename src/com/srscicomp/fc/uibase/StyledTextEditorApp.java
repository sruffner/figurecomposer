package com.srscicomp.fc.uibase;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.srscicomp.common.g2dutil.BasicPainterStyle;
import com.srscicomp.common.g2dutil.StrokeCap;
import com.srscicomp.common.g2dutil.StrokeJoin;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dutil.TextBoxPainter;
import com.srscicomp.common.g2dviewer.Focusable;
import com.srscicomp.common.g2dviewer.Graph2DViewer;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.g2dviewer.RenderableModel;
import com.srscicomp.common.g2dviewer.RenderableModelViewer;
import com.srscicomp.common.g2dviewer.RootRenderable;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.GUIUtilities;
import com.srscicomp.common.ui.GenericFont;
import com.srscicomp.common.ui.JPreferredSizePanel;
import com.srscicomp.common.ui.LocalFontEnvironment;
import com.srscicomp.common.ui.MainFrameShower;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.Measure;

/**
 * A test fixture for use while developing and debugging the {@link StyledTextEditor} class.
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
class StyledTextEditorApp extends JFrame implements ActionListener
{
   public static void main(String[] args)
   {
      GUIUtilities.initLookAndFeel();
      LocalFontEnvironment.initialize();

      final StyledTextEditorApp appFrame = new StyledTextEditorApp();

      appFrame.addWindowListener( new WindowAdapter() {
         public void windowClosing( WindowEvent e ) 
         {
            appFrame.onExit();
         }
      });

      Runnable runner = new MainFrameShower( appFrame );
      SwingUtilities.invokeLater( runner );
   }


   private StyledTextEditor atEditor = null;
   private TestModel model = null;
   private Graph2DViewer viewer = null;

   public StyledTextEditorApp() throws HeadlessException
   {
      super("Test StyledTextEditor");

      atEditor = new StyledTextEditor(5);
      AttributedString as = FGraphicNode.fromStyledText("Hello, my name is\nScott.\nBye!|0:U,5:u,18:w,23:p", 
            "Times New Roman", 18, Color.black, FontStyle.PLAIN, false);
      atEditor.loadContent(as, "Times New Roman", 18, Color.black, FontStyle.PLAIN);
      atEditor.addActionListener(this);

      // the test model and the model viewer
      model = new TestModel();
      viewer = new Graph2DViewer();
      viewer.setModel(model);
      JPanel viewerPanel = new JPreferredSizePanel(new BorderLayout());
      viewerPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
      viewerPanel.add(viewer, BorderLayout.CENTER);
      viewerPanel.add(atEditor, BorderLayout.NORTH);
      add(viewerPanel);
      setMinimumSize(new Dimension(800,1000));
      
      // update model to reflect current contents of the editor
      actionPerformed(null);
   }


   @Override public void actionPerformed(ActionEvent e)
   {
      model.updateText(atEditor.getCurrentContents());
   }

   public void onExit()
   {
      viewer.releaseResources();
      System.exit(0);
   }

   private class TestModel implements RenderableModel, RootRenderable
   {
      TestModel()
      {
         tbPainter = new TextBoxPainter();
         tbPainter.setStyle(
               BasicPainterStyle.createBasicPainterStyle(LocalFontEnvironment.getSansSerifFont(), 
                     GenericFont.SANSERIF, FontStyle.PLAIN, (float) (18*Measure.PT2IN*1000), 5.0f,
                     StrokeCap.BUTT, StrokeJoin.MITER, (float[]) null, Color.orange.getRGB(), Color.black.getRGB()));
         tbPainter.setBorderStroked(true);
         tbPainter.setClipped(true);
         tbPainter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
         tbPainter.setLineHeight(1.5);
         tbPainter.setBoundingBox(new Point2D.Double(WIDTH/5.0, HEIGHT/5.0), 3*WIDTH/5.0, 3*HEIGHT/5.0, 10);
      }
      
      public void updateText(AttributedString as)
      {
         // convert attributed string's font size from pt to milli-in
         AttributedCharacterIterator aci = as.getIterator();
         if(aci.getEndIndex() - aci.getBeginIndex() > 0)
         {
            Integer sz = (Integer) aci.getAttribute(TextAttribute.SIZE);
            if(sz != null)
            {
               double d = Measure.PT2IN * sz.doubleValue() * 1000.0;
               as.addAttribute(TextAttribute.SIZE, new Integer(Math.round((float)d)));
            }
         }
         tbPainter.setText(as);
         if(rmviewer != null) rmviewer.modelMutated(null);
      }
      
      @Override public boolean render(Graphics2D g2d, RenderTask task)
      {
         tbPainter.render(g2d, task);
         
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
      
      private TextBoxPainter tbPainter = null;
      private RenderableModelViewer rmviewer = null;
      private final static double WIDTH = 7500;
      private final static double HEIGHT = 7500;
   }
}
