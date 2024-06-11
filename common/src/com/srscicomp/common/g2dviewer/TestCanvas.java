package com.srscicomp.common.g2dviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import com.srscicomp.common.ui.GUIUtilities;
import com.srscicomp.common.ui.JPreferredSizePanel;
import com.srscicomp.common.ui.LocalFontEnvironment;
import com.srscicomp.common.ui.MainFrameShower;

/**
 * TestCanvas is a JFrame container for dev/test of RenderingCanvas.
 * 
 * @author sruffner
 */
class TestCanvas extends JFrame implements ActionListener
{
   private static final long serialVersionUID = 1L;

   private final TestPainter model;

   private final Graph2DViewer viewer;

   private final JButton testBtn;

   /**
    * @throws HeadlessException if monitor, keyboard or mouse are not available.
    */
   public TestCanvas() throws HeadlessException
   {
      super("RenderingCanvas TEST");

      // the test model and the model viewer
      model = new TestPainter();
      viewer = new Graph2DViewer();
      viewer.setModel(model);
      JPanel viewerPanel = new JPreferredSizePanel(new BorderLayout());
      viewerPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
      viewerPanel.add(viewer, BorderLayout.CENTER);

      // a button for changing the model's state
      testBtn = new JButton( model.getCurrentStageLabel() );
      testBtn.addActionListener(this);

      JPanel testBtnPanel = new JPreferredSizePanel();
      testBtnPanel.setLayout( new BorderLayout() );
      testBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
      testBtnPanel.add( testBtn, BorderLayout.NORTH );
      testBtnPanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

      JSplitPane content = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );
      content.setTopComponent( testBtnPanel );
      content.setBottomComponent( viewerPanel );

      add(content);
   }

   public void onExit()
   {
      viewer.releaseResources();
      System.exit(0);
   }

   public void actionPerformed(ActionEvent e)
   {
      if(e.getSource() == testBtn)
      {
         model.nextStage();
         testBtn.setText(model.getCurrentStageLabel());
      }
   }

  /**
   * Main entry point.
   * @param args Command-line arguments (not used).
   */
   public static void main(String[] args)
   {
      GUIUtilities.initLookAndFeel();
      LocalFontEnvironment.initialize();

      final TestCanvas appFrame = new TestCanvas();

      appFrame.addWindowListener( new WindowAdapter() {
         public void windowClosing( WindowEvent e ) 
         {
            appFrame.onExit();
         }
      });

      Runnable runner = new MainFrameShower( appFrame );
      SwingUtilities.invokeLater( runner );
   }

}
