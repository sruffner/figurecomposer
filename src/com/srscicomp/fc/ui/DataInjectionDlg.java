package com.srscicomp.fc.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SpringLayout;

import com.srscicomp.common.g2dviewer.Graph2DViewer;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.fig.FGNPlottableData;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.uibase.FCChooser;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * The "data injection dialog" is a convenient tool by which you can visually copy raw data sets from one or more
 * <i>FypML</i> figures into a single target figure. The source figure and destination figure are rendered side-by-side
 * in the dialog, and a data presentation node is highlighted in each. Arrow buttons are used to change the selected
 * data node in each figure. Whenever the data set in the source data presentation node is compatible with the target
 * data presentation node, the "Inject" button is enabled. Pressing this button will inject a copy of the source data
 * set into the target data presentation node. Note that only the raw data is copied; how the data is rendered -- as 
 * embodied by the data presentation node -- is unchanged.
 * 
 * <p>The motivation for introducing this dialog is that most <i>Figure Composer</i> users prefer to worked with 
 * visualized data, rather than the raw <i>FypML</i> data sets in text files, or <i>DataNav</i> .DNB or .DNR files, and
 * displayed in the spreadsheet-like table of {@link DSEditorToolDlg}. Being able to import Matlab FIG files directly
 * into FC made it much easier to get raw data into a figure; many use Matlab scripts to generate these FIG files. The
 * next step is to be able to inject data from such figures into a nicely formatted FypML figure, without affecting the
 * styling of the data. This is the sole purpose of <code>DataInjectionDlg</code>: it lets you update the "nice" figure 
 * as you refine your data in Matlab, without having to rebuild that figure from scratch. You generate one or more
 * source Matlab figures containing the data you need. Then you open your "nice" figure in FC and use this dialog to
 * inject the relevant data from the source figures into the "nice" figure.</p>
 * 
 * @author sruffner
 */
public class DataInjectionDlg extends JDialog implements ActionListener
{   
   /**
    * Raise a modal dialog by which the user can copy raw data sets from a source figure into a destination figure.
    * The figures are rendered side-by-side for easy visualization of the data. The user selects a data node in the
    * source, the target data node in the destination, and then copies -- or "injects" -- the raw data from the source
    * node to the destination node. The graphic styling of the target destination node is unchanged.
    * 
    * <p>The user can use the same dialog to inject any number of data sets from any number of source figures. A chooser
    * dialog is raised initially to select the initial source figure; after that, press the "Change..." button to select
    * a different source.</p>
    * 
    * <p>If the user cancels out of the "data injection dialog", then the destination figure is left unchanged. If one
    * or more data sets are injected, that is done as a single, reversible operation.</p>
    * 
    * @param c The GUI component which initiated the request. The dialog's owner will be this component's top-level 
    * ancestor, and the dialog is positioned relative to the component. If null, the dialog's owner will be an invisible
    * frame window and the dialog is centered on the screen.
    * @param fig The target figure. The user interacts with the dialog to inject selected data sets from one or more
    * source figures into this figure. It is left unchanged if the user cancels out of the dialog.
    * @param f The file path for the target figure. If this is a valid path, it is used as a starting point for choosing
    * the source figure file.
    */
   public static void injectDataIntoFigure(JComponent c, FGraphicModel fig, File f)
   {
      if(fig == null || fig.getAllPlottableDataNodes().isEmpty()) return;
      
      // choose the initial source figure. Abort if user cancels out of the chooser dialog.
      FCChooser chooser = FCChooser.getInstance();
      FGraphicModel srcFig = chooser.openFigure(c, f);
      if(srcFig == null) return;
      File srcFile = chooser.getSelectedFile(); 
      
      // dialog owner is invoking component's top-level ancestor window. If that's not available, then dialog will be 
      // attached to a shared invisible frame window.
      Container owner = (c == null) ? null : c.getTopLevelAncestor();
      if(!(owner instanceof Window)) owner = null;

      DataInjectionDlg injectDlg = new DataInjectionDlg((Window)owner, srcFile, srcFig,f, fig);
      
      injectDlg.pack();
      injectDlg.setMinimumSize(new Dimension(800,600));
      injectDlg.setLocationRelativeTo(owner);
      injectDlg.setVisible(true);
      
      injectDlg.dispose();
   }
   
   /**
    * Private constructor. Use {@link #injectDataIntoFigure(JComponent, FGraphicModel, File)}.
    * @param owner The dialog owner.
    * @param srcFile The initial source figure file path. Must be a valid file.
    * @param srcFig The initial source figure. Cannot be null.
    * @param dstFile The destination figure's file path. May be null.
    * @param dstFig The destination figure. It must contain at least one data presentation node.
    */
   private DataInjectionDlg(Window owner, File srcFile, FGraphicModel srcFig, File dstFile, FGraphicModel dstFig)
   {
      super(owner, "Inject raw data sets into a figure", ModalityType.APPLICATION_MODAL);
      setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
      
      this.srcDataNodes = srcFig.getAllPlottableDataNodes();
      this.iSrcSel = 0;
      this.originalFig = dstFig;
      
      // we work on a COPY of the original figure. The original figure is modified only when user "OK"s the changes.
      FGraphicModel figCopy = FGraphicModel.copy(dstFig);
      this.dstDataNodes = figCopy.getAllPlottableDataNodes();
      this.iDstSel = 0;
      
      this.injectedData = new ArrayList<>();

      JLabel srcLabel = new JLabel(srcFile.getName());
      changeSrcBtn = new JButton("Change...");
      changeSrcBtn.addActionListener(this);
      
      // the source canvas panel...
      srcCanvas = new Graph2DViewer(false, true, false);
      srcCanvas.setModel(srcFig);
      srcSelectLayer = new SelectionLayer(true);
      srcPrevBtn = new JButton(FCIcons.V4_BACK_22);
      srcPrevBtn.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
      srcPrevBtn.setContentAreaFilled(false); srcPrevBtn.setBorderPainted(false); 
      srcPrevBtn.setIconTextGap(0); srcPrevBtn.setFocusable(false);
      srcPrevBtn.addActionListener(this);
      srcNextBtn = new JButton(FCIcons.V4_FWD_22);
      srcNextBtn.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
      srcNextBtn.setContentAreaFilled(false); srcNextBtn.setBorderPainted(false); 
      srcNextBtn.setIconTextGap(0); srcNextBtn.setFocusable(false);
      srcNextBtn.addActionListener(this);
      srcInfoLabel = new JLabel("select a data node");
      
      injectSelBtn = new JButton("Inject >>");
      injectSelBtn.setEnabled(false);
      injectSelBtn.addActionListener(this);
      
      JLayeredPane srcLP = new JLayeredPane();
      srcLP.setLayout(new OverlayLayout(srcLP));
      srcLP.add(srcCanvas, Integer.valueOf(100));
      srcLP.add(srcSelectLayer, Integer.valueOf(101));
      
      JPanel srcPane = new JPanel();
      srcPane.add(srcLP);
      srcPane.add(srcPrevBtn);
      srcPane.add(srcNextBtn);
      srcPane.add(srcInfoLabel);
      srcPane.add(injectSelBtn);
      
      SpringLayout layout = new SpringLayout();
      srcPane.setLayout(layout);
      layout.putConstraint(SpringLayout.NORTH, srcLP, 0, SpringLayout.NORTH, srcPane);
      layout.putConstraint(SpringLayout.NORTH, srcInfoLabel, 0, SpringLayout.SOUTH, srcLP);
      layout.putConstraint(SpringLayout.SOUTH, srcPane, 0, SpringLayout.SOUTH, srcInfoLabel);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, srcPrevBtn, 0, SpringLayout.VERTICAL_CENTER, srcInfoLabel);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, srcNextBtn, 0, SpringLayout.VERTICAL_CENTER, srcInfoLabel);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, injectSelBtn, 0, SpringLayout.VERTICAL_CENTER, srcInfoLabel);

      layout.putConstraint(SpringLayout.WEST, srcLP, 0, SpringLayout.WEST, srcPane);
      layout.putConstraint(SpringLayout.EAST, srcLP, 0, SpringLayout.EAST, srcPane);
      layout.putConstraint(SpringLayout.WEST, srcPrevBtn, 0, SpringLayout.WEST, srcPane);
      layout.putConstraint(SpringLayout.WEST, srcNextBtn, 3, SpringLayout.EAST, srcPrevBtn);
      layout.putConstraint(SpringLayout.WEST, srcInfoLabel, 5, SpringLayout.EAST, srcNextBtn);
      layout.putConstraint(SpringLayout.EAST, srcInfoLabel, -5, SpringLayout.WEST, injectSelBtn);
      layout.putConstraint(SpringLayout.EAST, injectSelBtn, 0, SpringLayout.EAST, srcPane);

      // the destination canvas panel...
      JLabel dstLabel = new JLabel((dstFile != null && dstFile.isFile()) ? dstFile.getName() : "Destination figure");
      dstCanvas = new Graph2DViewer(false, true, false);
      dstCanvas.setModel(figCopy);
      dstSelectLayer = new SelectionLayer(false);
      dstPrevBtn = new JButton(FCIcons.V4_BACK_22);
      dstPrevBtn.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
      dstPrevBtn.setContentAreaFilled(false); dstPrevBtn.setBorderPainted(false);
      dstPrevBtn.setIconTextGap(0); dstPrevBtn.setFocusable(false);
      dstPrevBtn.addActionListener(this);
      dstNextBtn = new JButton(FCIcons.V4_FWD_22);
      dstNextBtn.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
      dstNextBtn.setContentAreaFilled(false); dstNextBtn.setBorderPainted(false); 
      dstNextBtn.setIconTextGap(0); dstNextBtn.setFocusable(false);
      dstNextBtn.addActionListener(this);
      dstInfoLabel = new JLabel("(select a data node)");

      JLayeredPane dstLP = new JLayeredPane();
      dstLP.setLayout(new OverlayLayout(dstLP));
      dstLP.add(dstCanvas, Integer.valueOf(100));
      dstLP.add(dstSelectLayer, Integer.valueOf(101));
      
      JPanel dstPane = new JPanel();
      dstPane.add(dstLP);
      dstPane.add(dstPrevBtn);
      dstPane.add(dstNextBtn);
      dstPane.add(dstInfoLabel);
      
      layout = new SpringLayout();
      dstPane.setLayout(layout);
      layout.putConstraint(SpringLayout.NORTH, dstLP, 0, SpringLayout.NORTH, dstPane);
      layout.putConstraint(SpringLayout.NORTH, dstInfoLabel, 0, SpringLayout.SOUTH, dstLP);
      layout.putConstraint(SpringLayout.SOUTH, dstPane, 0, SpringLayout.SOUTH, dstInfoLabel);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, dstPrevBtn, 0, SpringLayout.VERTICAL_CENTER, dstInfoLabel);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, dstNextBtn, 0, SpringLayout.VERTICAL_CENTER, dstInfoLabel);

      layout.putConstraint(SpringLayout.WEST, dstLP, 0, SpringLayout.WEST, dstPane);
      layout.putConstraint(SpringLayout.EAST, dstLP, 0, SpringLayout.EAST, dstPane);
      layout.putConstraint(SpringLayout.WEST, dstPrevBtn, 0, SpringLayout.WEST, dstPane);
      layout.putConstraint(SpringLayout.WEST, dstNextBtn, 3, SpringLayout.EAST, dstPrevBtn);
      layout.putConstraint(SpringLayout.WEST, dstInfoLabel, 5, SpringLayout.EAST, dstNextBtn);
      layout.putConstraint(SpringLayout.EAST, dstInfoLabel, 0, SpringLayout.EAST, dstPane);

      okBtn = new JButton("OK");
      okBtn.setEnabled(false);
      okBtn.addActionListener(this);
      
      startOverBtn = new JButton("Start Over");
      startOverBtn.setToolTipText("Reverse any changes made, restoring right-hand figure to its original state");
      startOverBtn.setEnabled(false);
      startOverBtn.addActionListener(this);
      
      cancelBtn = new JButton("Cancel");
      cancelBtn.addActionListener(this);
      
      JPanel contentPane = new JPanel();
      setContentPane(contentPane);
      
      layout = new SpringLayout();
      contentPane.setLayout(layout);
      contentPane.add(changeSrcBtn);
      contentPane.add(srcLabel);
      contentPane.add(dstLabel);
      contentPane.add(srcPane);
      contentPane.add(dstPane);
      contentPane.add(okBtn);
      contentPane.add(startOverBtn);
      contentPane.add(cancelBtn);
      
      layout.putConstraint(SpringLayout.NORTH, changeSrcBtn, 10, SpringLayout.NORTH, contentPane);
      layout.putConstraint(SpringLayout.NORTH, srcPane, 5, SpringLayout.SOUTH, changeSrcBtn);
      layout.putConstraint(SpringLayout.NORTH, okBtn, 10, SpringLayout.SOUTH, srcPane);
      layout.putConstraint(SpringLayout.SOUTH, contentPane, 10, SpringLayout.SOUTH, okBtn);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, srcLabel, 0, SpringLayout.VERTICAL_CENTER, changeSrcBtn);

      layout.putConstraint(SpringLayout.VERTICAL_CENTER, dstLabel, 0, SpringLayout.VERTICAL_CENTER, changeSrcBtn);
      layout.putConstraint(SpringLayout.NORTH, dstPane, 0, SpringLayout.NORTH, srcPane);
      layout.putConstraint(SpringLayout.SOUTH, dstPane, 0, SpringLayout.SOUTH, srcPane);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, startOverBtn, 0, SpringLayout.VERTICAL_CENTER, okBtn);
      layout.putConstraint(SpringLayout.VERTICAL_CENTER, cancelBtn, 0, SpringLayout.VERTICAL_CENTER, okBtn);

      layout.putConstraint(SpringLayout.WEST, srcPane, 10, SpringLayout.WEST, contentPane);
      layout.putConstraint(SpringLayout.WEST, dstPane, 20, SpringLayout.EAST, srcPane);
      layout.putConstraint(SpringLayout.EAST, contentPane, 10, SpringLayout.EAST, dstPane);
      layout.putConstraint(SpringLayout.WEST, changeSrcBtn, 0, SpringLayout.WEST, srcPane);
      layout.putConstraint(SpringLayout.WEST, srcLabel, 3, SpringLayout.EAST,  changeSrcBtn);
      layout.putConstraint(SpringLayout.EAST, srcLabel, 0, SpringLayout.EAST, srcPane);

      layout.putConstraint(SpringLayout.HORIZONTAL_CENTER, dstLabel, 0, SpringLayout.HORIZONTAL_CENTER, dstPane);
      layout.putConstraint(SpringLayout.EAST, cancelBtn, 0, SpringLayout.EAST, dstPane);
      layout.putConstraint(SpringLayout.EAST, startOverBtn, -5, SpringLayout.WEST, cancelBtn);
      layout.putConstraint(SpringLayout.EAST, okBtn, -5, SpringLayout.WEST, startOverBtn);
      
      updateOnSelectionChange(true);
      updateOnSelectionChange(false);
   }
   
   @Override public void actionPerformed(ActionEvent e)
   {
      Object src = e.getSource();
      if(src == changeSrcBtn)
      {
         FGraphicModel srcFig = FCChooser.getInstance().openFigure(changeSrcBtn, null);
         if(srcFig != null)
         {
            srcDataNodes.clear();
            srcDataNodes = srcFig.getAllPlottableDataNodes();
            iSrcSel = srcDataNodes.isEmpty() ? -1 : 0;
            srcCanvas.setModel(srcFig);
            updateOnSelectionChange(true);
         }
      }
      else if(src == injectSelBtn)
      {
         FGNPlottableData srcDataNode = getSelectedDataNode(true);
         FGNPlottableData dstDataNode = getSelectedDataNode(false);
         if(srcDataNode == null || dstDataNode == null || 
               !dstDataNode.isSupportedDataFormat(srcDataNode.getDataSet().getFormat()))
            return;
         
         // prepare data set to be injected. It must have same ID as the set it is replacing.
         DataSet injectDS = srcDataNode.getDataSet().changeID(dstDataNode.getDataSetID());
         String id = injectDS.getID();
         
         // add it to the list of data sets that will be injected. User could replace the same set more than once, so
         // we check list to see if a data set with the same ID is already there.
         boolean found = false;
         for(int i=0; (!found) && i<injectedData.size(); i++) if(injectedData.get(i).getID().equals(id))
         {
            injectedData.set(i, injectDS);
            found = true;
         }
         if(!found) injectedData.add(injectDS);
         
         // now inject data set into the target data node, which will trigger an update of the displayed figure
         dstDataNode.setDataSet(injectDS);
         updateOnSelectionChange(false);
         
         okBtn.setEnabled(true);
         startOverBtn.setEnabled(true);
      }
      else if(src == startOverBtn)
      {
         FGraphicModel figCopy = FGraphicModel.copy(originalFig);
         dstDataNodes.clear();
         dstDataNodes = figCopy.getAllPlottableDataNodes();
         iDstSel = 0;
         injectedData.clear();
         
         dstCanvas.setModel(figCopy);
         updateOnSelectionChange(false);
         okBtn.setEnabled(false);
         startOverBtn.setEnabled(false);
      }
      else if(src == okBtn)
      {
         // inject the accumulated data sets into the target figure in one go, then extinguish the dialog
         originalFig.replaceDataSetsInUse(injectedData, false);
         setVisible(false);
      }
      else if(src == cancelBtn)
      {
         setVisible(false);
      }
      else if(src == srcPrevBtn || src == srcNextBtn)
      {
         if(srcDataNodes.size() < 2) return;
         iSrcSel += (src == srcPrevBtn) ? -1 : 1;
         if(iSrcSel < 0) iSrcSel = srcDataNodes.size() -1;
         else if(iSrcSel >= srcDataNodes.size()) iSrcSel = 0;
         
         updateOnSelectionChange(true);
      }
      else if(src == dstPrevBtn || src == dstNextBtn)
      {
         if(dstDataNodes.size() < 2) return;
         iDstSel += (src == dstPrevBtn) ? -1 : 1;
         if(iDstSel < 0) iDstSel = dstDataNodes.size() -1;
         else if(iDstSel >= dstDataNodes.size()) iDstSel = 0;
         
         updateOnSelectionChange(false);
      }
   }
   
   private void selectDataNode(boolean src, FGNPlottableData dataNode)
   {
      if(dataNode == null) return;
      List<FGNPlottableData> nodes = src ? srcDataNodes : dstDataNodes;
      
      int iNextSel = -1;
      for(int i=0; i<nodes.size(); i++) if(nodes.get(i) == dataNode)
      {
         iNextSel = i;
         break;
      }
      if(iNextSel == -1 || iNextSel == (src ? iSrcSel : iDstSel)) return;
      
      if(src) iSrcSel = iNextSel;
      else iDstSel = iNextSel;
      
      updateOnSelectionChange(src);
   }
   
   private void updateOnSelectionChange(boolean src)
   {
      (src ? srcSelectLayer : dstSelectLayer).updateSelectedNodeOverlay();
      FGNPlottableData dsn = getSelectedDataNode(src);
      JLabel lbl = src ? srcInfoLabel : dstInfoLabel;
      if(dsn != null)
      {
         String sb = "<html><i>Title:</i> <b>" + dsn.getTitle() + "</b><br/>" +
               "<i>Set ID:</i> <b>" + dsn.getDataSetID() + "</b><br/>" +
               "<i>Format:</i> <b>" + dsn.getDataSet().getInfo().getShortDescription(false) +
               "</b></html>";
         lbl.setText(sb);
      }
      else lbl.setText("No data nodes in figure!");
      
      injectSelBtn.setEnabled(canInjectSelection());
   }
   
   private void updateDataInfoOnHighlightChange(boolean src)
   {
      FGNPlottableData dsn = (src ? srcSelectLayer : dstSelectLayer).highlightedDataNode;
      if(dsn == null) dsn = getSelectedDataNode(src);
      JLabel lbl = src ? srcInfoLabel : dstInfoLabel;
      if(dsn != null)
      {
         String sb = "<html><i>Title:</i> <b>" + dsn.getTitle() + "</b><br/>" +
               "<i>Set ID:</i> <b>" + dsn.getDataSetID() + "</b><br/>" +
               "<i>Format:</i> <b>" + dsn.getDataSet().getInfo().getShortDescription(false) +
               "</b></html>";
         lbl.setText(sb);
      }
      else lbl.setText("No data nodes in figure!");      
   }
   
   private FGNPlottableData getSelectedDataNode(boolean src)
   {
      if(src)
         return((srcDataNodes!=null && !srcDataNodes.isEmpty()) ? srcDataNodes.get(iSrcSel) : null);
      else
         return((dstDataNodes!=null && !dstDataNodes.isEmpty()) ? dstDataNodes.get(iDstSel) : null);
   }
   
   private Rectangle2D getSelectedDataNodeBounds(boolean src)
   {
      FGNPlottableData dsn = getSelectedDataNode(src);
      if(dsn == null) return(new Rectangle2D.Double());
      Graph2DViewer canvas = src ? srcCanvas : dstCanvas;
      return(canvas.logicalToDevice(dsn.getCachedGlobalBounds()));
   }
   
   private boolean canInjectSelection()
   {
      FGNPlottableData srcNode = getSelectedDataNode(true);
      FGNPlottableData dstNode = getSelectedDataNode(false);
      return(srcNode != null && dstNode != null && dstNode.isSupportedDataFormat(srcNode.getDataSet().getFormat()));
   }
   
   /** List of all data presentation nodes in the source figure. */
   private List<FGNPlottableData> srcDataNodes;
   
   /** Index of data presentation node currently selected within the source figure. */
   private int iSrcSel;
   
   /** 
    * The FypML figure into which raw data sets are injected. All work is done on a copy of this figure. Only when the
    * user confirms the changes is this figure updated with the injected data. This makes it very easy to "start over"
    * or "cancel" after some data has been injected.
    */
   private final FGraphicModel originalFig;
   
   /** List of all data presentation nodes in the destination figure. */
   private List<FGNPlottableData> dstDataNodes;
   
   /** Index of data presentation node currently selected within the destination figure. */
   private int iDstSel;
   
   /** The list of data sets injected into the destination figure thus far. */
   private final List<DataSet> injectedData;


   /** Press this button to select a different source figure. */
   private final JButton changeSrcBtn;
   
   /** Canvas on which the source figure is rendered; in non-interactive mode.*/
   private Graph2DViewer srcCanvas = null;
   /** A transparent layer on top of the source figure canvas: for selecting a data node in figure. */
   private SelectionLayer srcSelectLayer = null;
   /** Selects previous node in the source figure's list of data presentation nodes, with wrap-around. */
   private JButton srcPrevBtn = null;
   /** Selects next node in the source figure's list of data presentation nodes, with wrap-around. */
   private JButton srcNextBtn = null;
   /** Displays description of data set for data presentation node currently selected in source figure. */
   private JLabel srcInfoLabel = null;
   
   /** Canvas on which the destination figure is rendered; in non-interactive mode.*/
   private Graph2DViewer dstCanvas = null;
   /** A transparent layer on top of the destination figure canvas: for selecting a data node in figure. */
   private SelectionLayer dstSelectLayer = null;
   /** Selects previous node in the destination figure's list of data presentation nodes, with wrap-around. */
   private JButton dstPrevBtn = null;
   /** Selects next node in the destination figure's list of data presentation nodes, with wrap-around. */
   private JButton dstNextBtn = null;
   /** Displays description of data set for data presentation node currently selected in destination figure. */
   private JLabel dstInfoLabel = null;
   
   /**
    * Pressing this button copies the raw data set from the data node selected in the source figure and injects it into
    * the data node selected in the destination figure. Enabled only if destination data node supports the format of
    * the data set to be injected.
    */
   private JButton injectSelBtn = null;
   
   /** 
    * The "OK" button extinguishes the dialog and updates the original figure IAW any changes made therein. The 
    * operation can still be reversed later, via the figure's undo history.
    */
   private JButton okBtn = null;
   /** The "Start Over" button restores edited figure to its original state, obliterating any edits thus far. */
   private JButton startOverBtn = null;
   /** The "Cancel" button extinguishes the dialog without changing the original figure. */
   private JButton cancelBtn = null;
   
   
   /**
    * 
    * @author sruffner
    */
   private class SelectionLayer extends JPanel implements ComponentListener, MouseListener, MouseMotionListener
   {
      /** 
       * Construct the transparent selection layer for the source or destination figure canvas.
       * @param src True if this selection layer overlays the source figure canvas; else the destination canvas.
       */
      SelectionLayer(boolean src)
      {
         this.isSrc = src;
         setOpaque(false);
         addComponentListener(this);
         addMouseListener(this);
         addMouseMotionListener(this);
      }
      
      @Override public void componentResized(ComponentEvent e) { updateSelectedNodeOverlay(); }
      @Override public void componentMoved(ComponentEvent e) {}
      @Override public void componentShown(ComponentEvent e) { updateSelectedNodeOverlay(); }
      @Override public void componentHidden(ComponentEvent e) {}
      
      @Override public void mouseMoved(MouseEvent e)
      {
         Point p = e.getPoint();
         updateHighlightedDataNodeUnder(p);
      }
      @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
      @Override public void mouseClicked(MouseEvent e) 
      {
         if(e.getClickCount() == 1 && highlightedDataNode != null)
         {
            selectDataNode(isSrc, highlightedDataNode);
         }
      }
      @Override public void mousePressed(MouseEvent e) {}
      @Override public void mouseReleased(MouseEvent e) {}
      @Override public void mouseEntered(MouseEvent e) 
      {
         setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
      }
      @Override public void mouseExited(MouseEvent e) 
      {
         updateHighlightedDataNodeUnder(null);
         setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      }
      
      @Override protected void paintComponent(Graphics g)
      {
         // we work with a copy so that we don't impact painting of other components
         Graphics2D g2 = (Graphics2D) g.create();
         try
         {
            // clear dirty rectangle, or the entire layer if dirty rectangle is empty
            if(dirtyRect.width == 0) dirtyRect.setFrame(0, 0, getWidth(), getHeight());
            g2.setColor(transparentBlack);
            g2.fill(dirtyRect);
            
            // we always want nice-looking renderings
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // paint rectangle bounding the current selected data node
            g2.setColor(selectColor);
            g2.setStroke(selectStroke);
            g2.draw(selectRR);
            
            // paint rectangle bounding the data node currently under the mouse cursor (if any). Use same stroke.
            if(highlightedBounds.width > 0)
            {
               g2.setColor(hiliteColor);
               g2.draw(highlightedBounds);
            }
         }
         finally 
         { 
            if(g2 != null) g2.dispose();
         }
      }
      
      
      /**
       * Update the steel blue translucent rounded rectangle marking the rendered bounds of the currently selected 
       * plottable data node in the underlying figure. This must be called whenever the identity of that node changes,
       * or whenever any change in the underlying figure could affect the location or size of the node.
       */
      void updateSelectedNodeOverlay()
      {
         Rectangle2D r = getSelectedDataNodeBounds(isSrc);
         
         dirtyRect.setFrame(selectRR.getX(), selectRR.getY(), selectRR.getWidth(), selectRR.getHeight());
         r.setFrame(r.getX()-5, r.getY()-5, r.getWidth()+10, r.getHeight()+10);
         selectRR.setFrame(r);
         Rectangle.union(r, dirtyRect, dirtyRect);
         dirtyRect.grow(3, 3);
         paintImmediately(dirtyRect.x, dirtyRect.y, dirtyRect.width, dirtyRect.height);
      }

      /**
       * Update the translucent gold highlight rectangle that's drawn around the bounds of the "smallest" data 
       * presentation node (ie, with the smallest bounding rectangle) in the displayed figure that is under the 
       * specified point.
       * @param p A point in figure canvas device coordinates.
       */
      private void updateHighlightedDataNodeUnder(Point p)
      {
         Graph2DViewer canvas = isSrc ? srcCanvas : dstCanvas;
         FGraphicModel fig = (FGraphicModel) canvas.getModel();
         
         FGNPlottableData n = null;
         if(p != null)
         {
            canvas.deviceToLogical(p.x, p.y, pLog);
            FGraphicNode fgn = fig.findSmallestNodeUnder(pLog);
            if(fgn instanceof FGNPlottableData) n = (FGNPlottableData) fgn;
         }
         if(n == highlightedDataNode) return;
         
         highlightedDataNode = n;
         dirtyRect.setFrame(highlightedBounds);
         
         updateDataInfoOnHighlightChange(isSrc);
         
         if(highlightedDataNode == null)
            highlightedBounds.width = highlightedBounds.height = 0;
         else
         {
            Rectangle2D r = canvas.logicalToDevice(highlightedDataNode.getCachedGlobalBounds());
            if(r == null)
               highlightedBounds.width = highlightedBounds.height = 0;
            else
               highlightedBounds.setFrame(r);
         }
         
         Rectangle.union(highlightedBounds, dirtyRect, dirtyRect);
         dirtyRect.grow(3, 3);
         paintImmediately(dirtyRect.x, dirtyRect.y, dirtyRect.width, dirtyRect.height);
      }

      /** Flag indicates whether layer overlays the source figure or the destination figure canvas. */
      private final boolean isSrc;
      
      /** 
       * The rounded rectangle covering the currently selected data node in the displayed figure. It is always stroked,
       * and it must be updated whenever the identity of the selected data node changes, or the bounds change.
       */
      private final RoundRectangle2D selectRR = new RoundRectangle2D.Double(0, 0, 0, 0, 10, 10);

      /** A point in the logical coordinate system of the rendered graphic (to avoid frequent heap allocations). */
      private final Point2D pLog = new Point2D.Double(0, 0);
      /** The data node currently "under" the mouse and highlighted in the selection layer. */
      private FGNPlottableData highlightedDataNode = null;
      /** The bounds of the currently highlighted data node, in figure canvas coordinates. */
      private final Rectangle highlightedBounds = new Rectangle(0,0,0,0);

      /** 
       * The rectangle that should be updated on each repaint cycle. When it is zero width, it is ignored and the entire
       * layer is repainted. Set to zero width after each repaint. 
       */
      private final Rectangle dirtyRect = new Rectangle(0,0,0,0);
      
      /** Dirty rectangle is filled with completely transparent black on each repaint. */
      private final Color transparentBlack = new Color(0,0,0,0);
      /** Stroke color for rectangle bounding of the currently selected data node (= translucent steel blue). */
      private final Color selectColor = new Color(70, 130, 180, 80);
      /** The line stroke for the bounds of currently selected data node. */
      private final BasicStroke selectStroke = new BasicStroke(3);
      /** The color with which bounds of current highlighted data node is stroked (= translucent goldenrod). */
      private final Color hiliteColor = new Color(221, 170, 34, 80);
   }
}
