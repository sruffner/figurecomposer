package com.srscicomp.fc.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.print.PageFormat;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.text.JTextComponent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONUtilities;

import com.srscicomp.common.g2dutil.BasicPainterStyle;
import com.srscicomp.common.g2dutil.ResizeAnchor;
import com.srscicomp.common.g2dutil.StringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.CanvasEvent;
import com.srscicomp.common.g2dviewer.CanvasListener;
import com.srscicomp.common.g2dviewer.Graph2DViewer;
import com.srscicomp.common.ui.FontStyle;
import com.srscicomp.common.ui.GenericFont;
import com.srscicomp.common.ui.TabStrip;
import com.srscicomp.common.ui.TabStripModel;
import com.srscicomp.common.util.NeverOccursException;
import com.srscicomp.common.util.Utilities;
import com.srscicomp.fc.data.DataSet;
import com.srscicomp.fc.fig.FGModelListener;
import com.srscicomp.fc.fig.FGModelSchemaConverter;
import com.srscicomp.fc.fig.FGNAlign;
import com.srscicomp.fc.fig.FGNPlottableData;
import com.srscicomp.fc.fig.FGNStyleSet;
import com.srscicomp.fc.fig.FGNodeType;
import com.srscicomp.fc.fig.FGraphicModel;
import com.srscicomp.fc.fig.FGraphicNode;
import com.srscicomp.fc.fig.Graph3DNode;
import com.srscicomp.fc.fig.ImageNode;
import com.srscicomp.fc.fig.FGraphicModel.Change;
import com.srscicomp.fc.matlab.MatlabFigureImporter;
import com.srscicomp.fc.uibase.FCChooser;
import com.srscicomp.fc.uibase.FCFileType;
import com.srscicomp.fc.uibase.FCIcons;
import com.srscicomp.fc.uibase.FCUIConstants;
import com.srscicomp.fc.uibase.FCWorkspace;
import com.srscicomp.fc.uibase.PrinterSupport;
import com.srscicomp.fc.uibase.WSFigBrowser;
import com.srscicomp.fc.uibase.WrapLayout;
import com.srscicomp.fc.uibase.FCWorkspace.EventID;

/**
 * <code>FigComposer</code> is the primary GUI component of the <i>Figure Composer</i> application. Any number of FypML
 * figures can be "opened" for display and/or editing within the figure composer, which uses a tabbed-pane like UI: A 
 * {@link TabStrip} along the top edge of the view manages the list of open figures. The currently selected figure is 
 * displayed in a {@link Graph2DViewer} canvas, which takes up the central, expandable region in the figure composer's 
 * panel. A "tools panel" along the top edge manages a set of push or toggle buttons associated with a variety of file-,
 * edit-, and view-related command actions. All of the associated actions (and some others) can be accessed via the 
 * figure composer's menu bar. Tool bar buttons are 22x22. The tools panel comes in three alternative states: full 
 * (buttons include icon and label), compact (icons only), or hidden. A user configures the tools panel via the "View" 
 * menu; the tool panel state is preserved as a user preference and is restored when the application launches.
 * 
 * <p>Docked on the left side of the the figure composer panel is the figure node tree, {@link FigNodeTree}. This 
 * component displays the current figure's graphic node tree and includes a node property editor that exposes the 
 * properties of the current focus node. The node tree can be hidden when you want to maximize the size of the figure 
 * rendered in the figure canvas, but normally the user will need it whenever he is making changes to that figure.</p>
 * 
 * <p>Docked on the right side of the figure composer is the workspace figures browser, {@link WSFigBrowser}. It lets 
 * you browse all known figure files (FypML or Matlab FIG) in your workspace. You can rename, copy or delete individual
 * figure files, and you can open any figure by double-clicking on the file name within the browser. Like the node tree,
 * the workspace browser can be hidden to increase the amount of screen real estate available to the figure canvas. Use 
 * the relevant check-style menu item in the composer's "View" menu to toggle the browser on or off.</p>
 * 
 * <p>A horizontal ruler along the bottom edge of the figure canvas and a vertical ruler along the left edge depict the
 * canvas's current coordinate system in inches, given the canvas's size and its current zoom/pan transform. The rulers
 * include cursor position indicators that follow the mouse cursor when it is inside the canvas; each indicator includes
 * a readout of the relevant cursor coordinate (X for horizontal ruler, Y for vertical). Again, the rulers can be turned
 * on or off via a check-style menu item in the figure composer's "View" menu.</p>
 * 
 * <p>A transparent layer on top of and coincident with the figure canvas implements user interactions with the current
 * displayed figure. The {@link InteractionLayer} displays a translucent blue overlay highlighting the current focus 
 * node, animates focus node selection and move-by-drag gestures in the "select" cursor mode, and animates zoom and pan 
 * mouse-drag gestures in the "zoom" cursor mode. [The focus node overlay can be hidden temporarily by hitting the
 * ESCape key, so that the user has a completely unobstructed view of the figure.]</p>
 * 
 * <p>Whenever the figure composer is shown, it registers for key-pressed events with the global keyboard focus manager.
 * This is the mechanism by which a number of useful keyboard shortcuts, or "hot keys", are detected and processed. For 
 * details, see {@link #dispatchKeyEvent(KeyEvent)}. Note that the "hot keys" associated with most command actions like 
 * "New figure" or "Open figure" are handled through the menu mechanism.</p>
 * 
 * @author sruffner
 */
@SuppressWarnings("serial")
public class FigComposer extends JPanel implements TabStripModel, FGModelListener, FCWorkspace.Listener, 
      ComponentListener, KeyEventDispatcher, WSFigBrowser.Listener, CanvasListener
{
   /** 
    * Construct the figure composer. In addition to constructing the UI, the auto-save facility is initialized, and a 
    * series of background tasks are spawned to reload the set of FypML figure files that were open when the application
    * last exited. 
    * @param appActions If non-null, this array must contain three top-level application actions which will be 
    * incorporated into the figure composer's menu bar: "About", "Preferences", and "Exit" -- in that order.
    */
   public FigComposer(Action[] appActions)
   {
      super();
      
      createActions();
      createMenuBar(appActions);
      construct();
      
      FigureComposer.updateStartupProgress(60, "Restoring open figures...");
      
      // initialize auto-save facility. If a crash occurred and there's stale auto-saved state to restore, start a
      // background task to restore the list of open figures IAW the recovered auto-save state. Otherwise, spawn 
      // individual tasks to load each of the last open figures.
      autoSaver.initialize();
      if(autoSaver.isRestoreFromCrashNeeded())
      {
         initWithBlankFigure();
         autoSaver.startRestore();
      }
      else
         reopenLastOpenFigures();
      
      FCWorkspace.getInstance().addWorkspaceListener(this);
      addComponentListener(this);
      
      // whenever the current keyboard focus owner is a text component, that component will consume the "hot keys" for
      // the standard Undo/Cut/Copy/Paste/Del operations -- so we need to disable the analogous focus node-related
      // operations.
      KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
      focusManager.addPropertyChangeListener(e -> {
         if("focusOwner".equals(e.getPropertyName())) refreshActions();
      });
      
      refreshActions();
   }

   /** 
    * Whenever there's a change in the workspace path cache, check the list of current figures to see if any of the
    * figure files have been deleted. If a figure file has been deleted AND the figure has not yet been modified in the
    * figure composer, then that figure and corresponding tab are removed automatically. If the figure is modified, the
    * corresponding tab is kept, but its source file is set to null so that it behaves like a figure that has never 
    * been saved.
    * <p>On the other hand, if the user renames a file that's currently on display in the figure composer (via the
    * workspace browser), then we simply update the the figure's source file accordingly.</p>
    */
   @Override public void onWorkspaceUpdate(EventID id, File fp1, File fp2)
   {
      if((id == EventID.PATHCACHE || id == EventID.PATHRENAME))
      {
         boolean changed = false;
         List<Tab> toBeRemoved = new ArrayList<>();
         for(Tab tab : openFigures) if(tab.src != null && !tab.src.isFile())
         {
            if(id == EventID.PATHRENAME && Utilities.filesEqual(tab.src, fp1))
               tab.src = fp2;
            else if(tab.fig.isModified())
            {
               tab.src = null;
               tab.modified = true;
            }
            else
               toBeRemoved.add(tab);
            changed = true;
         }
         
         if(!toBeRemoved.isEmpty())
         {
            Tab selectedTab = openFigures.get(iCurrentFig);
            for(Tab tab : toBeRemoved) openFigures.remove(tab);
            iCurrentFig = openFigures.indexOf(selectedTab);
            if(iCurrentFig == -1) 
            {
               if(openFigures.isEmpty())
                  initWithBlankFigure();
               else
               {
                  iCurrentFig = 0;
                  loadSelectedFigure();
               }
            }
         }
         
         if(changed) fireStateChanged();
      }
   }
   
   @Override public void onWSFBOpenFigure(File f) { openFigure(f); }
   @Override public void onWSFBClose()
   {
      revalidate();
      actions.get(FCActionType.VIEW_TOGGLEWSFIG).refreshState();
   }

   /**
    * Whenever there's a change in the figure currently on display, the enable state and tooltip of the various edit
    * actions are refreshed accordingly, and the focus node overlay is typically repainted.
    */
   @Override public void modelChanged(FGraphicModel model, FGraphicNode n, Change change)
   {
      if(model == figCanvas.getModel())
      {
         if(change != Change.MODFLAG_RESET)
         {
            refreshActions();
            mouseLayer.onModelChanged(change);
         }
      }
   }

   /** 
    * Whenever the figure canvas viewport changes, update the canvas rulers and the focus node overlay to reflect the 
    * changes.
    */
   @Override public void viewportChanged(CanvasEvent e)
   {
      resetRulers();
      mouseLayer.updateSelectionOverlay();
   }
   
   @Override public void renderingStarted(CanvasEvent e) {}
   @Override public void renderingStopped(CanvasEvent e) {}
   @Override public void renderingCompleted(CanvasEvent e) {}
   @Override public void renderingInProgress(CanvasEvent e) {}
   @Override public void cursorMoved(CanvasEvent e) {}


   /**
    * Whenever the user changes the screen resolution in his workspace preferences, the new resolution is set on the
    * embedded figure canvas and its contents are redrawn to reflect the resolution change.
    */
   public void onScreenResolutionChange() 
   { 
      figCanvas.setResolution(FCWorkspace.getInstance().getScreenDPI());
      figCanvas.modelMutated(null);
   }
   
   /**
    * When application window opens, the figure canvas, node tree, and workspace browser are all reloaded. All of these
    * components will not be properly rendered until they can obtain a graphics context, and that is not possible until
    * the top-level frame window has opened. 
    * @param fileOpenOnAppStart If non-null, user started the application by selecting this figure file to open. If it
    * is not already in the list of open figures, add it. Then ensure it is the initial figure displayed in the figure
    * composer.
    */
   public void onApplicationWindowOpened(File fileOpenOnAppStart)
   {
      FGraphicModel old = (FGraphicModel) figCanvas.getModel();
      if(old != null) old.removeListener(this);
      figCanvas.setModel(null);
      loadSelectedFigure();
      figBrowser.onApplicationStartOrExit(false);
      if(fileOpenOnAppStart != null) openFigure(fileOpenOnAppStart);
      
      // start auto-saving open figures in the background
      autoSaver.resume();
   }

   /**
    * If any open figure files have been modified, we need to ask user whether or not the changes should be saved
    * before the application exits. Before doing so, we bring this view controller to the front so that the user can
    * see the files that need to be saved. The user may opt to cancel shutdown during this process.
    * 
    * <p>In addition, we remember the list of open figures, the currently selected figure, and the on/off state of the 
    * figure tree view so it can be restored the next time the application launches.</p>
    */
   public boolean onVetoableApplicationExit() 
   { 
      // do any open files have unsaved changes? 
      boolean fileChanged = false;
      for(Tab tab : openFigures) if(tab.modified)
      {
         fileChanged = true;
         break;
      }
      
      // give the user a chance to save each figure with unsaved changes. User may choose to cancel shutdown instead.
      if(fileChanged)
      {
         // go through list of figures and accumulate the figures (and file destinations) to be saved before exit. In
         // any figure has never been saved, we'll have to request a file path from the user.
         FCChooser chooser = FCChooser.getInstance();
         List<FGraphicModel> figsToSave = new ArrayList<>();
         List<File> dstFiles = new ArrayList<>();
         List<Integer> tabIndices = new ArrayList<>();
         String[] options = new String[] {"Cancel", "Save none", "Save all", "No", "Yes"};
         boolean saveRemaining = false;
         for(int i=0; i<openFigures.size(); i++)
         {
            Tab tab = openFigures.get(i);
            if(!tab.modified) continue;
            setSelectedTab(i);
            
            if(saveRemaining)
            {
               tabIndices.add(i);
               figsToSave.add(tab.fig);
               
               // if the figure has never been saved, request a file path from user. If user cancels, then abort exit.
               if(tab.src == null)
               {
                  if(!chooser.saveFigure(this, tab.fig, null, false, true)) return(false);
                  else tab.src = chooser.getSelectedFile();
               }
               dstFiles.add(tab.src);

               continue;
            }
            
            if(figsToSave.size() == 1) { options[1] = "Skip remaining"; options[2] = "Save remaining"; }
            int choice = JOptionPane.showOptionDialog(this,
                  (tab.src != null) ? "Save changes to " + tab.src.getName() + "?" : "Save new figure?",
                  "Save before exit",
                  JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                  options, options[4]);
            if(choice == JOptionPane.CLOSED_OPTION || choice == 0) return(false);
            else if(choice == 1)
               break;
            else if(choice == 2 || choice == 4)
            {
               tabIndices.add(i);
               figsToSave.add(tab.fig);
               
               // if the figure has never been saved, request a file path from user. If user cancels, then abort exit.
               if(tab.src == null)
               {
                  if(!chooser.saveFigure(this, tab.fig, null, false, true)) return(false);
                  else tab.src = chooser.getSelectedFile();
               }
               dstFiles.add(tab.src);

               saveRemaining = (choice == 2);
            }
         }
         
         // save all the figures that user wanted saved. If an error occurs, then update figure tabs to reflect the
         // files that were saved, and abort application exit.
         if(!figsToSave.isEmpty())
         {
            int nSaved = chooser.saveAllFigures(this, figsToSave, dstFiles);
            if(nSaved < figsToSave.size())
            {
               for(int i=0; i<nSaved; i++)
               {
                  Tab tab = openFigures.get(tabIndices.get(i));
                  tab.fig.setModified(false);
               }
               return(false);
            }
         }
      }

      // remember all figures currently open, the currently selected file, and the on/off state of the figure tree and
      // the workspace figures browser for restoration the next time the application is launched.
      return(true);
   }
   
   /** 
    * This override saves all state that needs to be restored the next time the application launches: the list of open
    * open figures, the currently selected figure, and the on/off state of the figure tree view and workspace browser.
    * It also shuts down the auto-save facility. All auto-saved content is deleted in a normal shutdown because the 
    * user has already had a chance to save or discard any changes in the open figures.
    */
   public void onApplicationWillExit()
   {
      autoSaver.shutDown();
      
      FCWorkspace ws = FCWorkspace.getInstance();
      ws.setLastOpenFigures(getAllOpenFigureFiles());
      ws.setLastSelectedFigure(getCurrentSelectedFigureFile());
      ws.setFigComposerTreeViewOn(figureTreeView.isVisible());
      figBrowser.onApplicationStartOrExit(true);
   }

   public JMenuBar getMenuBar() { return(menuBar); }

   /** 
    * Whenever figure composer is shown, register with the keyboard focus manager to listen for selected "hot keys". 
    */
   @Override public void componentShown(ComponentEvent e) 
   {
      if(e.getSource() == this)
      {
         KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(this);
      }
   }
   
   /** Whenever figure composer is hidden, de-register with the keyboard focus manager. */
   @Override public void componentHidden(ComponentEvent e) 
   {
      if(e.getSource() == this)
      {
         KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
      }
   }
   @Override public void componentResized(ComponentEvent e) {}
   @Override public void componentMoved(ComponentEvent e)  {}
   
   /**
    * If the specified key stroke corresponds to one of the global keyboard accelerators, or "hot keys", recognized by
    * the figure composer, then perform the relevant action. The recognized keystrokes are:
    * <ul>
    *    <li><i>Esc</i> key: Hides the selection node overlay so user has an unobstructed view of current figure.</li>
    *    <li><i>Up/Down arrow</i> keys: Move selection to the previous/next node in the list of nodes <b>currently 
    *    reachable</b> in the figure node tree. (The node tree is implemented as a list of reachable nodes; when a 
    *    parent node is collapsed, its children are not reachable.)</li>
    *    <li><i>Left arrow</i> key: Move selection to the parent of the currently selected node. If the root node is
    *    currently selected, the key has no effect.</li>
    *    <li><i>Right arrow</i> key: If the currently selected node is a collapsed parent node, expand that node's 
    *    child list in the node tree and move the selection to its first child. Else, the key has no effect.</li>
    *    <li><i>Up/Down arrow</i> keys with Shift key depressed (and NOT the Alt key): Extends a range selection from 
    *    the current anchor row to the row before or after the selected row furthest from the anchor row.</li>
    *    <li><i>Home</i> key: Shifts selection to root node of the current figure.</li>
    *    <li><i>Up arrow</i> key with Control (Command on Mac OSX) key depressed: If a single node is selected and it is
    *    not a component node (nor a tick set) and is not the first child in its parent's child list, move the node up 
    *    one position in the child list, which effectively lowers its Z-order. The first child is always "underneath" 
    *    all of its siblings.</li>
    *    <li><i>Down arrow</i> key with Control (Command on Mac OSX) key depressed: If a single node is selected and it
    *    is not a component node (nor a tick set) and is not the last child in its parent's child list, move the node 
    *    down one position in the child list, which effectively raises its Z-order. The last child is always "on top" 
    *    of all of its siblings.</li>
    *    <li><i>Up/Down/Left/Right arrow</i> key with Alt or Alt+Shift keys depressed: If the currently selected node 
    *    can be moved interactively, "nudge" ALL movable nodes up/down/left/right from their current respective 
    *    locations by 50 mi (Alt only) or 5 mi (Alt+Shift)</li>
    * </ul>
    * Note that most of the above actions pertain to the figure's node tree, and all apply to the current focus node in
    * the current figure. However, many of the keystrokes could also apply to other controls like a list control, the 
    * drop-down list for a combo box, etcetera. Therefore, to ensure specificity of action and reduce the chances of 
    * interfering with the normal operation of lower-level controls, these hot keys are processed only if the figure 
    * node tree currently holds the keyboard focus ({@link FigNodeTree#isFocusOnNodeTree()}). Whenever you click on a
    * node in the figure canvas, keyboard focus is automatically transferred to the node tree (since the canvas itself
    * does not hold the focus).
    * 
    * <p>The "hot keys" associated with the figure composer's various action commands are not normally processed here,
    * but through the menu bar hierarchy. However, when the figure composer is configured to edit a hub navigation view
    * template, then it will be installed in a modal dialog and its associated menu bar is not available. In this
    * special case, the "hot keys" associated with any edit- or view-related actions are dispatched here (but not the
    * file-related actions, as these are not available in this special configuration).</p>
    * 
    * @param e The key event.
    * @return True if the specified key stroke was processed as a "hot key" for a figure composer action.
    */
   @Override public boolean dispatchKeyEvent(KeyEvent e)
   {
      if(e.isConsumed()) return(true);
      if(!isVisible()) return(false);
      
      boolean focusOnNodeTree = figureTreeView.isFocusOnNodeTree();
      
      boolean consume = false;
      int id = e.getID();
      int key = e.getKeyCode();
      int mod = e.getModifiersEx();
      
      // is the Shift key depressed WITHOUT the Alt key as well?
      boolean shiftDown = (mod & (KeyEvent.ALT_DOWN_MASK |KeyEvent.SHIFT_DOWN_MASK)) == KeyEvent.SHIFT_DOWN_MASK;
      
      KeyStroke ks =  KeyStroke.getKeyStroke(key, mod);
      
      // when focus is NOT on a text component and the mouse is inside the canvas interaction layer, holding down the 
      // space key enables zoom actions on the figure canvas...
      if((!focusOnTextComponent) && key == KeyEvent.VK_SPACE && mod == 0 && 
            (id == KeyEvent.KEY_PRESSED || id==KeyEvent.KEY_RELEASED) && mouseLayer.isInside())
      {
         consume = true;
         mouseLayer.enableZoomMode(id==KeyEvent.KEY_PRESSED);
      }
      
      // when focus is on the node tree and the Alt key is pressed/released, turn on/off the position cue for the first 
      // selected node (if that node may be moved interactively). Since the Alt key is a required modifier for the 
      // arrow keys that "nudge" a node U/D/L/R, this ensures that the position cue is shown while "nudging"
      if((!consume) && focusOnNodeTree && 
            (id==KeyEvent.KEY_PRESSED || id==KeyEvent.KEY_RELEASED) && key == KeyEvent.VK_ALT)
      {
         consume = true;
         mouseLayer.updatePositionCue(id==KeyEvent.KEY_PRESSED);
      }
      
      if((!consume) && id == KeyEvent.KEY_PRESSED)
      {
         if((mod == 0 || shiftDown) && focusOnNodeTree)
         {
            if(key == KeyEvent.VK_ESCAPE)
            {
               mouseLayer.showSelectionOverlay(false);
               consume = true;
            }
            else if(key == KeyEvent.VK_HOME)
            {
               FGraphicModel fgm = getSelectedFigureModel();
               if(fgm != null)
               {
                  fgm.setSelectedNode(fgm.getRoot());
                  consume = true;
               }
            }
            else if(key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN)
            {
               figureTreeView.changeSelection(key == KeyEvent.VK_DOWN, shiftDown);
               consume = true;
            }
            else if(mod == 0 && (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT))
            {
               FGraphicModel fgm = getSelectedFigureModel();
               FGraphicNode selected = (fgm != null) ? fgm.getFocusForSelection() : null;
               if(selected != null)
               {
                  if(key == KeyEvent.VK_LEFT)
                  {
                     FGraphicNode parent = selected.getParent();
                     if(parent != null && parent.isComponentNode()) parent = parent.getParent();
                     if(parent != null) fgm.setSelectedNode(parent);
                  }
                  else if(selected.getChildCount() > 0)
                  {
                     fgm.setSelectedNode(selected.getChildAt(0));
                  }
               }
               consume = true;
            }
         }
                  
         // recognize and process the keystrokes that move the focus node up or down in Z-order among its siblings. 
         // Operation disabled if more than one node is selected.
         if((!consume) && focusOnNodeTree && (DECZ_HOTKEY.equals(ks) || INCZ_HOTKEY.equals(ks)))
         {
            FGraphicModel fgm = getSelectedFigureModel();
            FGraphicNode selected = (fgm != null && !fgm.isMultiNodeSelection()) ? fgm.getSelectedNode() : null;
            FGNodeType selType = (selected == null) ? null : selected.getNodeType();
            if(selected != null && selected.getParent() != null && (!selected.isComponentNode()) && 
                  (selType != FGNodeType.TICKS) && (selType != FGNodeType.TICKS3D))
            {
               FGraphicNode parent = selected.getParent();
               int pos = parent.getIndexOf(selected) + (DECZ_HOTKEY.equals(ks) ? -1 : 1);
               if(pos >= 0 && pos < parent.getChildCount())
                  fgm.changeChildPosition(parent, selected, pos);
            }
            consume = true;
         }
         
         // recognize and process U/D/L/R arrow with Alt or Alt+Shift modifier: nudge all movable nodes that are
         // currently selected in the figure U/D/L/R by 50 mi (Alt only) or 5 mi (Alt+Shift)
         if((!consume) && focusOnNodeTree && ((mod & (KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)) != 0) &&
               (key==KeyEvent.VK_UP || key==KeyEvent.VK_DOWN || key==KeyEvent.VK_LEFT || key==KeyEvent.VK_RIGHT))
         {
            double nudge = 0;
            if(mod == KeyEvent.ALT_DOWN_MASK) nudge = 50;
            else if(mod == (KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)) nudge = 5;
            FGraphicModel fgm = getSelectedFigureModel();
            if(nudge > 0 && fgm != null && fgm.canMoveCurrentSelection())
            {
               double dx = (key==KeyEvent.VK_LEFT) ? -nudge : (key==KeyEvent.VK_RIGHT ? nudge : 0); 
               double dy = (key==KeyEvent.VK_DOWN) ? -nudge : (key==KeyEvent.VK_UP ? nudge : 0); 
               fgm.moveCurrentSelection(dx, dy);
               
               // turn off node highlight when user uses keys to nudge the focus node. Also update the position cue
               // that shows the current position of the node just "nudged"
               mouseLayer.updateHighlightNodeUnder(null); 
               mouseLayer.updatePositionCue(true);
            }
            consume = true;
         }
      }
      
      if(consume) e.consume();
      return(consume);
   }

   /**
    * Open the specified file and load the figure for editing within the figure composer.
    * 
    * <p>The method can open either FypML figure files or Matlab FIG files. If the source file is already opened in the 
    * composer, the corresponding tab is selected. .</p>
    * 
    * <p>Otherwise, if the figure graphic model is already available in the workspace's model cache, it is loaded 
    * immediately in a new tab. If not, a new tab is created for the figure (initially containing a blank figure), 
    * and a background task is spawned to load the figure model from the FypML file or import it from the FIG file. Once
    * that thread completes (assuming the load is successful), the loaded figure is installed in that tab. If the source 
    * file was a FIG file, then the tab will indicate that the figure is new (no source file path) and modified but
    * unsaved -- <i>DataNav</i> CANNOT save FIG files, only open them!</p>
    * 
    * @param src The FypML file defining the figure to be opened for editing, or the Matlab FIG file to be imported as
    * a new FypML figure. If null, if file does not exist, or if the filename does not have a valid extension, then no 
    * action is taken. 
    */
   public void openFigure(File src)
   {
      if(src == null || !src.isFile()) return;
      FCFileType ft = FCFileType.getFileType(src);
      if(ft != FCFileType.FYP && ft != FCFileType.FIG) return; 
      
      // if the FypML figure file is already open, just switch to the corresponding tab.
      for(int i=0; i<openFigures.size(); i++) 
      {
         Tab tab = openFigures.get(i);
         if(Utilities.filesEqual(tab.src, src))
         {
            setSelectedTab(i);
            return;
         }
      }
      
      // if the figure graphic is already cached in workspace, add it to the set of open figures
      FGraphicModel fgm = (FGraphicModel) FCWorkspace.getInstance().getModelFromCache(src, null);
      if(fgm != null)
      {
         openFigures.add(new Tab(fgm, src));
         iCurrentFig = openFigures.size() - 1;
         loadSelectedFigure();
         FCWorkspace.getInstance().addToRecentFiles(src);
         fireStateChanged();
         return;
      }
      
      // otherwise, add a new tab for the figure and spawn a worker task to load it from source file
      openFigures.add(new Tab(null, src));
      iCurrentFig = openFigures.size() - 1;
      loadSelectedFigure();
      fireStateChanged();
      
      FigureLoader loader = new FigureLoader(src);
      loadersInProgress.add(loader);
      loader.execute();
   }

   /**
    * Get the list of all FypML figure files currently open in the figure composer. It will NOT include any figures
    * that have been created but not yet saved to file.
    * @return The list of figure files currently open.
    */
   List<File> getAllOpenFigureFiles()
   {
      List<File> filesOpen = new ArrayList<>();
      for(Tab tab : openFigures) if(tab.src != null) filesOpen.add(tab.src);
      return(filesOpen);
   }
   
   /**
    * Get the FypML figure file that is currently selected for editing in the figure composer, if it exists.
    * @return The path of the FypML source file for the currently selected figure. Returns null if the current figure 
    * has not yet been saved to file. 
    */
   File getCurrentSelectedFigureFile()
   {
      if(iCurrentFig < 0 || iCurrentFig >= openFigures.size()) return(null);
      return(openFigures.get(iCurrentFig).src);
   }

   /** 
    * Hot key combination: Command-Up or Control-Up. Decrements focus node position in sibling list, thereby 
    * decrementing its Z-order. Not associated with a tool button.
    */
   private final static KeyStroke DECZ_HOTKEY = 
         KeyStroke.getKeyStroke(KeyEvent.VK_UP, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
   /** 
    * Hot key combination: Command-Down or Control-Down. Decrements focus node position in sibling list, thereby 
    * decrementing its Z-order. Not associated with a tool button.
    */
   private final static KeyStroke INCZ_HOTKEY = 
         KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
   
   /** Delegate handles task of backing up open figures on a background thread, "in case disaster strikes". */
   private final AutoSaver autoSaver = new AutoSaver();
   
   /** Canvas on which the currently selected figure is rendered; in non-interactive mode.*/
   private Graph2DViewer figCanvas = null;
   
   /** The horizontal canvas ruler. */
   private Ruler hRuler = null;
   /** The vertical canvas ruler. */
   private Ruler vRuler = null;
   
   /** 
    * Layer that highlights the focus node in the currently displayed figure and implements a number of mouse-mediated
    * user interactions.
    */
   private InteractionLayer mouseLayer = null;
   
   /** A tree-like view displaying the graphic node tree for the currently selected figure. */
   private FigNodeTree figureTreeView = null;
   
   /** Tab strip showing the file names of all open figures; used to select current figure, close a figure. */
   private TabStrip tabStrip = null;
   
   /** 
    * A compact figure finder that lists the figures in a given folder. Double-clicking on a figure file here will
    * open that figure in the figure composer proper. Can be shown/hidden.
    */
   private WSFigBrowser figBrowser = null;
   
   /** List of all figures currently open. */
   private final List<Tab> openFigures = new ArrayList<>();
   
   /** Index or tab position of the open figure that is currently selected for display and editing. */
   private int iCurrentFig = -1;
   
   /** 
    * Figure loading worker threads in progress. Upon completion, back on the event dispatch thread, the thread object
    * removes itself from this list.
    */
   private final List<FigureLoader> loadersInProgress = new ArrayList<>();

   /** Construct the figure composer UI. */
   private void construct()
   {
      mouseLayer = new InteractionLayer();

      figCanvas = new Graph2DViewer(false, true, false);
      figCanvas.setResolution(FCWorkspace.getInstance().getScreenDPI());
      figCanvas.addMouseListener(mouseLayer);
      figCanvas.addMouseMotionListener(mouseLayer);
      figCanvas.addCanvasListener(this);
      
      // we listen for size changes in figure canvas so the rulers and focus node overlay are updated accordingly
      figCanvas.addComponentListener(this);
      
      // the figure's node tree view -- initial visibility depends on workspace setting
      figureTreeView = new FigNodeTree();
      figureTreeView.setVisible(FCWorkspace.getInstance().getFigComposerTreeViewOn());
      
      tabStrip = new TabStrip(this);

      // for convenience, add a context menu to the tab strip that is copy of the File|Close submenu from the menu bar 
      JPopupMenu closeMenu = new JPopupMenu();
      closeMenu.add(actions.get(FCActionType.FILE_CLOSE));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSEALL));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSEVIS));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSEHIDDEN));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSEALLBUTCURR));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSELEFT));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSERIGHT));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSEUNMOD));
      tabStrip.setComponentPopupMenu(closeMenu);

      // add a title at the top of the context menu to clearly indicate that these are "Close" actions
      TitledBorder labelBorder = BorderFactory.createTitledBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, closeMenu.getForeground().brighter().brighter()), "Close",
              TitledBorder.CENTER, TitledBorder.ABOVE_TOP, closeMenu.getFont(), closeMenu.getForeground());
      closeMenu.setBorder(BorderFactory.createCompoundBorder(closeMenu.getBorder(), labelBorder));
      
      // a compact figure finder lets users browse contents of one workspace folder at a time
      figBrowser = new WSFigBrowser();
      figBrowser.addListener(this);
      
      // assemble the layers: figure canvas covered by mouse interaction layer
      JLayeredPane layers = new JLayeredPane();
      layers.setLayout(new OverlayLayout(layers));
      layers.add(figCanvas, Integer.valueOf(100));
      layers.add(mouseLayer, Integer.valueOf(101));
      
      // put canvas in a panel with a horizontal ruler along the bottom and vertical ruler along left edge
      hRuler = new Ruler(true);
      vRuler = new Ruler(false);
      JPanel canvasPanel = new JPanel(new BorderLayout());
      canvasPanel.add(layers, BorderLayout.CENTER);
      canvasPanel.add(hRuler, BorderLayout.SOUTH);
      canvasPanel.add(vRuler,  BorderLayout.WEST);

      // the tool button panel with pseudo tool bars for file ops, edit ops, insert-node ops, and view ops
      createToolsPanel();
      
      JPanel center = new JPanel(new BorderLayout());
      center.add(tabStrip, BorderLayout.NORTH);
      center.add(canvasPanel, BorderLayout.CENTER);
      center.add(figureTreeView, BorderLayout.WEST);
      
      setLayout(new BorderLayout());
      add(toolsPanel, BorderLayout.NORTH);
      add(center, BorderLayout.CENTER);
      add(figBrowser, BorderLayout.EAST);

      setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
      
      // the tools panel is initialized in the "full" state. We deliberately change it here to ensure that the toolbar
      // buttons have been laid out (preferred heights computed to accommodate text in the full state). We then set the
      // state according to the current user preference.
      setToolsPanelState(TOOLS_COMPACT, false);
      String state = FCWorkspace.getInstance().getFigComposerToolbarState();
      if(state.isEmpty()) FCWorkspace.getInstance().setFigComposerToolbarState(TOOLS_COMPACT);
      else if(!TOOLS_COMPACT.equals(state)) setToolsPanelState(state, false);
      
      // the figure composer must be wide enough to accommodate the figure tree view, workspace figures browser, and at 
      // least 300 pixels for the figure canvas. It must be tall enough to accommodate the minimum heights of the 
      // tree view (plus tab strip) and figures browser, and be no less than 600 pixels high; then add on to that the 
      // height of the tool panel -- which in "full" mode could have 4 rows of labelled buttons.
      Dimension d = figureTreeView.getMinimumSize();
      d.width += figBrowser.getMinimumSize().width;
      d.width += 300;
      
      d.height = Math.max(600, 
            Math.max(d.height + tabStrip.getPreferredSize().height, figBrowser.getMinimumSize().height));
      d.height += openRecentBtn.getPreferredSize().height * 4;
      setMinimumSize(d);
   }

   /** 
    * The tool button panel, which is docked along the top edge of the figure composer. The panel has four sections, or
    * "tool bars", each with a set of buttons: file ops, edit ops, insert-node ops, and view ops.
    */
   private JPanel toolsPanel = null;
   
   /**
    * The set of push and toggle buttons in the tools panel. We keep a reference to each button so each can be 
    * reconfigured for the "full" (with short labels) or "compact" (no labels) state of the tools panel.
    */
   private List<AbstractButton> toolButtons = null;
   
   /** Tool button for the "Open Recent" action; we keep a reference so we can raise a pop-up menu next to it. */
   private JButton openRecentBtn = null;

   /**
    * Construct the figure composer's tool bar panel, initially in the {@link #TOOLS_FULL} state, including a short
    * label for each button in the panel. Takes no action if tool bar panel has already been created.
    */
   private void createToolsPanel()
   {
      if(toolsPanel != null) return;
      toolButtons = new ArrayList<>();

      JPanel fileOpsToolPanel = new TBPanel();
      fileOpsToolPanel.setLayout(new BoxLayout(fileOpsToolPanel, BoxLayout.LINE_AXIS));
      
      fileOpsToolPanel.add(Box.createHorizontalStrut(8));  // some blank space for vertical rule on left edge
      
      JButton btn =  createToolButton(FCActionType.FILE_NEW, FCIcons.V4_NEW_22);
      fileOpsToolPanel.add(btn);
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.FILE_DUPE, FCIcons.V4_DUPE_22);
      fileOpsToolPanel.add(btn);
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.FILE_OPEN, FCIcons.V4_OPEN_22);
      fileOpsToolPanel.add(btn); 
      toolButtons.add(btn);

      btn =  createToolButton(FCActionType.FILE_OPENRECENT, FCIcons.V4_OPENRECENT_22);
      fileOpsToolPanel.add(btn); 
      toolButtons.add(btn);
      openRecentBtn = btn;
      
      btn = createToolButton(FCActionType.FILE_SAVE, FCIcons.V4_SAVE_22);
      fileOpsToolPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.FILE_SAVEAS, FCIcons.V4_SAVEAS_22);
      fileOpsToolPanel.add(btn); 
      toolButtons.add(btn);

      btn =  createToolButton(FCActionType.FILE_EXPORT, FCIcons.V4_EXPORT_22);
      fileOpsToolPanel.add(btn);
      toolButtons.add(btn);

      btn =  createToolButton(FCActionType.FILE_PAGESETUP, FCIcons.V4_PAGESETUP_22);
      fileOpsToolPanel.add(btn);
      toolButtons.add(btn);

      btn =  createToolButton(FCActionType.FILE_PRINT, FCIcons.V4_PRINT_22);
      fileOpsToolPanel.add(btn);    
      toolButtons.add(btn);

      fileOpsToolPanel.add(Box.createHorizontalStrut(10));  // some blank space after last button in tool button panel
      
      
      TBPanel editToolPanel = new TBPanel();
      editToolPanel.setLayout(new BoxLayout(editToolPanel, BoxLayout.LINE_AXIS));
      
      editToolPanel.add(Box.createHorizontalStrut(8));  // some blank space for vertical rule on left edge

      btn =  createToolButton(FCActionType.EDIT_UNDO, FCIcons.V4_UNDO_22);
      editToolPanel.add(btn);
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.EDIT_REDO, FCIcons.V4_REDO_22);
      editToolPanel.add(btn);
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.EDIT_CUT, FCIcons.V4_CUT_22);
      editToolPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.EDIT_COPY, FCIcons.V4_COPY_22);
      editToolPanel.add(btn);
      toolButtons.add(btn);

      btn =  createToolButton(FCActionType.EDIT_PASTE, FCIcons.V4_PASTE_22);
      editToolPanel.add(btn); 
      toolButtons.add(btn);

      editToolPanel.add(Box.createHorizontalStrut(10)); // some blank space to separate sections of tool panel
      
      btn =  createToolButton(FCActionType.EDIT_COPYSTYLE, FCIcons.V4_COPYSTYLE_22);
      editToolPanel.add(btn); 
      toolButtons.add(btn);

      btn =  createToolButton(FCActionType.EDIT_PASTESTYLE, FCIcons.V4_PASTESTYLE_22);
      editToolPanel.add(btn); 
      toolButtons.add(btn);

      editToolPanel.add(Box.createHorizontalStrut(10)); // some blank space to separate sections of tool panel
      
      btn =  createToolButton(FCActionType.EDIT_EXTDATA, FCIcons.V4_EXTDATA_22);
      editToolPanel.add(btn); 
      toolButtons.add(btn);

      btn =  createToolButton(FCActionType.EDIT_INJDATA, FCIcons.V4_INJDATA_22);
      editToolPanel.add(btn); 
      toolButtons.add(btn);

      editToolPanel.add(Box.createHorizontalStrut(10));  // some blank space after last button in tool button panel

      // tool button panel for inserting basic objects that don't have to be in a graph container.
      TBPanel insertBasicPanel = new TBPanel();
      insertBasicPanel.setLayout(new BoxLayout(insertBasicPanel, BoxLayout.LINE_AXIS));
      insertBasicPanel.add(Box.createHorizontalStrut(8));  // some blank space for vertical rule on left edge
      
      btn =  createToolButton(FCActionType.ADD_LABEL, FCIcons.V4_LABEL_22);
      insertBasicPanel.add(btn);
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_TEXTBOX, FCIcons.V4_TEXTBOX_22);
      insertBasicPanel.add(btn);
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_IMAGE, FCIcons.V4_IMAGE_22);
      insertBasicPanel.add(btn);
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_SHAPE, FCIcons.V4_SHAPE_22);
      insertBasicPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_LINE, FCIcons.V4_LINE_22);
      insertBasicPanel.add(btn); 
      toolButtons.add(btn);
      
      insertBasicPanel.add(Box.createHorizontalStrut(10)); // some blank space to separate sections of tool panel
      
      // tool button panel for 2D graph objects
      TBPanel insert2DPanel = new TBPanel();
      insert2DPanel.setLayout(new BoxLayout(insert2DPanel, BoxLayout.LINE_AXIS));
      insert2DPanel.add(Box.createHorizontalStrut(8));  // some blank space for vertical rule on left edge

      btn =  createToolButton(FCActionType.ADD_GRAPH, FCIcons.V4_GRAPH_22);
      insert2DPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_POLARG, FCIcons.V4_POLARG_22);
      insert2DPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_CALIB, FCIcons.V4_CALIB_22);
      insert2DPanel.add(btn); 
      toolButtons.add(btn);

      btn =  createToolButton(FCActionType.ADD_TRACE, FCIcons.V4_TRACE_22);
      insert2DPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_FUNCTION, FCIcons.V4_FUNCTION_22);
      insert2DPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_RASTER, FCIcons.V4_RASTER_22);
      insert2DPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_CONTOUR, FCIcons.V4_HEATMAP_22);
      insert2DPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_BOX, FCIcons.V4_BOX_22);
      insert2DPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_BAR, FCIcons.V4_BAR_22);
      insert2DPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_AREA, FCIcons.V4_AREA_22);
      insert2DPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_PIE, FCIcons.V4_PIE_22);
      insert2DPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_SCATTER, FCIcons.V4_SCATTER_22);
      insert2DPanel.add(btn); 
      toolButtons.add(btn);
      
      insert2DPanel.add(Box.createHorizontalStrut(10)); // some blank space to separate sections of tool panel

      // tool button panel for 3D graph objects
      TBPanel insert3DPanel = new TBPanel();
      insert3DPanel.setLayout(new BoxLayout(insert3DPanel, BoxLayout.LINE_AXIS));
      insert3DPanel.add(Box.createHorizontalStrut(8));  // some blank space for vertical rule on left edge

      btn =  createToolButton(FCActionType.ADD_GRAPH3D, FCIcons.V4_GRAPH3D_22);
      insert3DPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_SCATTER3D, FCIcons.V4_SCATTER3D_22);
      insert3DPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.ADD_SURFACE, FCIcons.V4_SURFACE_22);
      insert3DPanel.add(btn); 
      toolButtons.add(btn);
      
      insert3DPanel.add(Box.createHorizontalStrut(10)); // some blank space to separate sections of tool panel

      // tool button panel for view-related action commands
      TBPanel viewToolPanel = new TBPanel();
      viewToolPanel.setLayout(new BoxLayout(viewToolPanel, BoxLayout.LINE_AXIS));
      
      viewToolPanel.add(Box.createHorizontalStrut(10));  // some blank space for vertical rule on left edge

      btn =  createToolButton(FCActionType.VIEW_SPECIALCHARS, FCIcons.V4_INSCHAR_22);
      viewToolPanel.add(btn); 
      toolButtons.add(btn);
      
      JToggleButton scale2FitTog = new JToggleButton(actions.get(FCActionType.VIEW_TOGGLEMODE));
      scale2FitTog.setIconTextGap(2);
      scale2FitTog.setIcon(FCIcons.V4_SCALETOFITOFF_22);
      scale2FitTog.setSelectedIcon(FCIcons.V4_SCALETOFITON_22);
      scale2FitTog.setSelected(true);
      scale2FitTog.setBorder(BorderFactory.createEmptyBorder(3,0,3,12)); 
      scale2FitTog.setContentAreaFilled(false); scale2FitTog.setBorderPainted(false);
      scale2FitTog.setFocusable(false); 
      viewToolPanel.add(scale2FitTog);
      toolButtons.add(scale2FitTog);

      btn =  createToolButton(FCActionType.VIEW_RESETZOOM, FCIcons.V4_RESETZOOM_22);
      viewToolPanel.add(btn); 
      toolButtons.add(btn);
      
      btn =  createToolButton(FCActionType.VIEW_REFRESH, FCIcons.V4_REFRESH_22);
      viewToolPanel.add(btn); 
      toolButtons.add(btn);
      
      toolsPanel = new JPanel(new WrapLayout(WrapLayout.LEADING, 0, 0));
      toolsPanel.add(fileOpsToolPanel);
      toolsPanel.add(editToolPanel);
      toolsPanel.add(insertBasicPanel);
      toolsPanel.add(insert2DPanel);
      toolsPanel.add(insert3DPanel);
      toolsPanel.add(viewToolPanel);
   }

   /**
    * Helper method creates one of the push buttons that appear in the figure composer's tool bar panel. The button 
    * object is associated with the specified figure composer action, and it is initially configured to appear with a
    * short label (the action "name"). All buttons are configured as not focusable, with a blank border and the content 
    * area unfilled. All have an icon text gap of 2 pixels, and the blank border puts 3 pixels above and below and 12 
    * pixels on the right to create adequate space between adjacent tool buttons and between successive rows.
    *
    * @param type The type of figure composer command action attached to the button.
    * @param icon The button's icon.
    * @return The button object.
    */
   private JButton createToolButton(FCActionType type, ImageIcon icon)
   {
      JButton btn = new JButton(actions.get(type));
      btn.setIconTextGap(2);
      btn.setIcon(icon);
      btn.setBorder(BorderFactory.createEmptyBorder(3,0,3,12));
      btn.setContentAreaFilled(false); btn.setBorderPainted(false);
      btn.setFocusable(false);
      return(btn);
   }

   /** Constant identifying the "hidden" state of the figure composer's tool bar panel. */
   private final String TOOLS_HIDDEN = "Hidden";
   /** Constant identifying the "compact" state (button icons only) of the figure composer's tool bar panel. */
   private final String TOOLS_COMPACT = "Compact";
   /** Constant identifying the "full" state (buttons w/ icons and labels) of the figure composer's tool bar panel. */
   private final String TOOLS_FULL = "Full";
   
   /** The current state of the figure composer's tool bar panel. */
   private String toolsPanelState = TOOLS_FULL;
   
   /**
    * Change the state/configuration of the figure composer's tool bar panel. 
    * @param state The desired state for the tool bar panel: {@link #TOOLS_HIDDEN}, {@link #TOOLS_COMPACT}, or {@link 
    * #TOOLS_FULL}. No action is taken if the tools panel is already in this state.
    * @param save If true and the state changed, the new state is saved in the user's workspace settings.
    */
   private void setToolsPanelState(String state, boolean save)
   {
      if(toolsPanelState.equals(state)) return;
      
      if(TOOLS_HIDDEN.equals(state))
         toolsPanel.setVisible(false);
      else if(TOOLS_COMPACT.equals(state))
      {
         if(!openRecentBtn.getHideActionText())
         {
            for(AbstractButton btn : toolButtons)
            {
               btn.setHideActionText(true);
            }
         }
         toolsPanel.setVisible(true);
      }
      else if(TOOLS_FULL.equals(state))
      {
         if(openRecentBtn.getHideActionText())
         {
            for(AbstractButton btn : toolButtons)
            {
               btn.setHideActionText(false);
            }
         }
         toolsPanel.setVisible(true);
      }
      else
         return;
      
      toolsPanelState = state;
      if(save) FCWorkspace.getInstance().setFigComposerToolbarState(toolsPanelState);
      revalidate();
   }
   
   /**
    * A simple extension of {@link JPanel} that paints a narrow vertical rule near the left edge. It is used
    * as a tool bar-like action button container for the four tool bars comprising the figure composer's tools panel.
    * @author sruffner
    */
   private static class TBPanel extends JPanel
   {
      @Override protected void paintComponent(Graphics g)
      {
         super.paintComponent(g);
         Graphics gCopy = g.create();
         try
         {
            gCopy.setColor(getBackground());
            gCopy.draw3DRect(2, 4, 3, getHeight()-8, true);
         }
         finally { if(gCopy != null) gCopy.dispose(); }
      }
   }
   
   /**
    * Set the file system path for the disk file to which the currently selected <i>standalone</i> figure should be 
    * serialized. If the target file is valid, it is added to the set of most recently used files maintained by the
    * <i>DataNav</i> workspace manager.
    * @param f The target file. Will be <code>null</code> if the current figure has never been serialized before.
    */
   private void setCurrentFile(File f)
   {
      openFigures.get(iCurrentFig).src = f;
      if(f != null) 
      {
         FCWorkspace.getInstance().addToRecentFiles(f);
         if(FCFileType.getFileType(f) == FCFileType.FYP) 
            openFigures.get(iCurrentFig).srcLastMod = f.lastModified();
      }
      fireStateChanged();
   }

   /**
    * Helper method called during application startup that checks the workspace manager for the list of figures that 
    * were open in the figure composer when the application last exited. It prepares the tabs for these figures and
    * spawns worker threads to load the figures from their respective source files. As each worker finishes, it installs
    * the loaded figure in the corresponding tab (back on the Swing event dispatch thread, of course). It also checks
    * the workspace manager for the figure file that was last selected, and selects the corresponding tab (if that file
    * still exists, of course).
    * 
    * <p>If there are no "last open" figures, then the method tries to open the most recently used figure. If that does
    * not exist either -- which could be the case the very first time the application runs --, then the figure composer
    * is initialized with a single blank, unsaved figure.</p>
    */
   private void reopenLastOpenFigures()
   {
      FCWorkspace ws = FCWorkspace.getInstance();
      List<File> filesOpen = ws.getLastOpenFigures();
      File fileLast = ws.getLastSelectedFigure();
      if(filesOpen.isEmpty())
      {
         File mruFile = ws.getMostRecentFile(false);
         if(mruFile != null) filesOpen.add(mruFile);
      }
      
      if(filesOpen.isEmpty())
      {
         initWithBlankFigure();
         return;
      }

      // populate all figure tabs and prepare, BUT DO NOT START, the worker threads that will read the source files
      // and load the figure graphic models. The workers are started after we're done setting up the tabs and displaying
      // the initial selected figure (which will be blank initially). If we start the figure loaders as tabs are
      // created, the initially selected figure comes up blank on occasion -- probably due to a worker stepping on
      // this code section.
      iCurrentFig = 0;
      int idx = 0;
      for(File f : filesOpen)
      {
         openFigures.add(new Tab(null, f));
         if(f.equals(fileLast)) iCurrentFig = idx;
         FigureLoader loader = new FigureLoader(f);
         loadersInProgress.add(loader);
         ++idx;
      }
      loadSelectedFigure();
      fireStateChanged();
      
      for(FigureLoader loader : loadersInProgress) loader.execute();
   }

   
   /** The application menu bar specific to the figure composer. */
   private JMenuBar menuBar = null;
   /** The "Open Recent" submenu in the "File" drop-down menu. */
   private JMenu mruMenu = null;
   /** A right-click context menu that exposes selected figure composer action commands. */
   private JPopupMenu contextMenu = null;
   
   /**
    * Helper method creates the menu bar specific to the figure composer. It exposes the various actions that are also
    * available in the tool bar panel. It also creates a context menu exposing a subset of the same actions; this 
    * context menu is raised when the user right-clicks on the canvas (except in the "zoom" cursor mode).
    * 
    * @param appActions If non-null, this array must contain three top-level application actions which will be 
    * incorporated into the figure composer's menu bar: "About", "Preferences", and "Exit" -- in that order.
    */
   private void createMenuBar(Action[] appActions)
   {
      createActions();
      
      Action about = null;
      Action prefs = null;
      Action exit = null;
      if(appActions != null && appActions.length >= 3)
      {
         about = appActions[0];
         prefs = appActions[1];
         exit = appActions[2];
      }
      
      JMenu fileMenu = new JMenu("File");
      fileMenu.addMenuListener(new MenuListener() {

         @Override public void menuSelected(MenuEvent e)
         {
            // each time the File dropdown menu is selected, the MRU file menu is repopulated
            prepareMRUFileMenu(false);
         }

         @Override public void menuDeselected(MenuEvent e) {}
         @Override public void menuCanceled(MenuEvent e) {}
      });
      
      fileMenu.setMnemonic(KeyEvent.VK_F);
      if((about != null || prefs != null) && !Utilities.isMacOS())
      {
         if(about != null)
         {
            fileMenu.add(new JMenuItem(about));
            fileMenu.addSeparator();
         }
         if(prefs != null)
         {
            fileMenu.add(new JMenuItem(prefs));
            fileMenu.addSeparator();
         }
      }
      
      fileMenu.add(actions.get(FCActionType.FILE_NEW));
      fileMenu.add(actions.get(FCActionType.FILE_DUPE));
      fileMenu.add(actions.get(FCActionType.FILE_OPEN));
      
      mruMenu = new JMenu("Open Recent");
      mruMenu.setMnemonic(KeyEvent.VK_R);
      fileMenu.add(mruMenu);
      
      JMenu closeMenu = new JMenu("Close");
      closeMenu.setMnemonic(KeyEvent.VK_C);
      closeMenu.add(actions.get(FCActionType.FILE_CLOSE));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSEALL));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSEVIS));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSEHIDDEN));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSEALLBUTCURR));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSELEFT));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSERIGHT));
      closeMenu.add(actions.get(FCActionType.FILE_CLOSEUNMOD));
      fileMenu.add(closeMenu);
      
      fileMenu.add(actions.get(FCActionType.FILE_REVERT));
      fileMenu.add(actions.get(FCActionType.FILE_SAVE));
      fileMenu.add(actions.get(FCActionType.FILE_SAVEAS));
      fileMenu.addSeparator();
      fileMenu.add(actions.get(FCActionType.FILE_INJECTDATA));
      fileMenu.addSeparator();
      fileMenu.add(actions.get(FCActionType.FILE_EXPORT));
      fileMenu.add(actions.get(FCActionType.FILE_EXPALL_PS));
      fileMenu.add(actions.get(FCActionType.FILE_EXPALL_PDF));
      fileMenu.add(actions.get(FCActionType.FILE_EXPALL_JPG));
      fileMenu.add(actions.get(FCActionType.FILE_EXPALL_PNG));
      fileMenu.add(actions.get(FCActionType.FILE_EXPALL_SVG));
      fileMenu.addSeparator();
      fileMenu.add(actions.get(FCActionType.FILE_PAGESETUP));
      fileMenu.add(actions.get(FCActionType.FILE_PRINT));
      fileMenu.add(actions.get(FCActionType.FILE_PRINTALL));
      
      if(exit != null && !Utilities.isMacOS())
      {
         fileMenu.addSeparator();
         fileMenu.add(new JMenuItem(exit));
      }
      
      JMenu editMenu = new JMenu("Edit");
      editMenu.setMnemonic(KeyEvent.VK_E);
      editMenu.add(actions.get(FCActionType.EDIT_UNDO));
      editMenu.add(actions.get(FCActionType.EDIT_REDO));
      editMenu.addSeparator();
      editMenu.add(actions.get(FCActionType.EDIT_CUT));
      editMenu.add(actions.get(FCActionType.EDIT_COPY));
      editMenu.add(actions.get(FCActionType.EDIT_PASTE));
      editMenu.add(actions.get(FCActionType.EDIT_DEL));
      editMenu.add(actions.get(FCActionType.EDIT_SELECT));
      editMenu.addSeparator();
      editMenu.add(actions.get(FCActionType.EDIT_EXTDATA));
      editMenu.add(actions.get(FCActionType.EDIT_INJDATA));
      editMenu.add(actions.get(FCActionType.EDIT_COPYSTYLE));
      editMenu.add(actions.get(FCActionType.EDIT_PASTESTYLE));
      editMenu.addSeparator();
      JMenu alignMenu = new JMenu("Align");
      alignMenu.setMnemonic(KeyEvent.VK_A);
      editMenu.add(alignMenu);
      alignMenu.add(actions.get(FCActionType.ALIGN_LEFT));
      alignMenu.add(actions.get(FCActionType.ALIGN_RIGHT));
      alignMenu.add(actions.get(FCActionType.ALIGN_BOTTOM));
      alignMenu.add(actions.get(FCActionType.ALIGN_TOP));
      alignMenu.add(actions.get(FCActionType.ALIGN_HCENTER));
      alignMenu.add(actions.get(FCActionType.ALIGN_VCENTER));
      editMenu.addSeparator();
      JMenu insertMenu = new JMenu("Insert");
      insertMenu.setMnemonic(KeyEvent.VK_I);
      editMenu.add(insertMenu);
      JMenu menu3D = new JMenu("3D");
      menu3D.setMnemonic(KeyEvent.VK_3);
      insertMenu.add(menu3D);
      menu3D.add(actions.get(FCActionType.ADD_GRAPH3D));
      menu3D.add(actions.get(FCActionType.ADD_SCATTER3D));
      menu3D.add(actions.get(FCActionType.ADD_SURFACE));
      JMenu menu2D = new JMenu("2D");
      menu2D.setMnemonic(KeyEvent.VK_2);
      insertMenu.add(menu2D);
      menu2D.add(actions.get(FCActionType.ADD_GRAPH));
      menu2D.add(actions.get(FCActionType.ADD_POLARG));
      menu2D.add(actions.get(FCActionType.ADD_TRACE));
      menu2D.add(actions.get(FCActionType.ADD_FUNCTION));
      menu2D.add(actions.get(FCActionType.ADD_RASTER));
      menu2D.add(actions.get(FCActionType.ADD_CONTOUR));
      menu2D.add(actions.get(FCActionType.ADD_BOX));
      menu2D.add(actions.get(FCActionType.ADD_BAR));
      menu2D.add(actions.get(FCActionType.ADD_AREA));
      menu2D.add(actions.get(FCActionType.ADD_PIE));
      menu2D.add(actions.get(FCActionType.ADD_SCATTER));
      menu2D.add(actions.get(FCActionType.ADD_CALIB));
      insertMenu.add(actions.get(FCActionType.ADD_LABEL));
      insertMenu.add(actions.get(FCActionType.ADD_TEXTBOX));
      insertMenu.add(actions.get(FCActionType.ADD_IMAGE));
      insertMenu.add(actions.get(FCActionType.ADD_SHAPE));
      insertMenu.add(actions.get(FCActionType.ADD_LINE));
      
      JMenu viewMenu = new JMenu("View");
      viewMenu.setMnemonic(KeyEvent.VK_V);
      JMenu toolbarStateMenu = new JMenu("Toolbar");
      toolbarStateMenu.setMnemonic(KeyEvent.VK_T);
      toolbarStateMenu.add(new JCheckBoxMenuItem(actions.get(FCActionType.VIEW_TOOLBAR_HIDE)));
      toolbarStateMenu.add(new JCheckBoxMenuItem(actions.get(FCActionType.VIEW_TOOLBAR_COMPACT)));
      toolbarStateMenu.add(new JCheckBoxMenuItem(actions.get(FCActionType.VIEW_TOOLBAR_FULL)));
      viewMenu.add(toolbarStateMenu);
      
      viewMenu.add(new JCheckBoxMenuItem(actions.get(FCActionType.VIEW_TOGGLETREE)));
      viewMenu.add(new JCheckBoxMenuItem(actions.get(FCActionType.VIEW_TOGGLERULERS)));
      viewMenu.add(new JCheckBoxMenuItem(actions.get(FCActionType.VIEW_SPECIALCHARS)));
      viewMenu.add(new JCheckBoxMenuItem(actions.get(FCActionType.VIEW_TOGGLEWSFIG)));
      viewMenu.add(new JCheckBoxMenuItem(actions.get(FCActionType.VIEW_TOGGLEMODE)));
      viewMenu.add(new JCheckBoxMenuItem(actions.get(FCActionType.VIEW_TOGGLEPRINTPREVIEW)));
      viewMenu.addSeparator();
      viewMenu.add(actions.get(FCActionType.VIEW_RESETZOOM));
      viewMenu.add(actions.get(FCActionType.VIEW_REFRESH));      
      
      menuBar = new JMenuBar();
      menuBar.add(fileMenu);
      menuBar.add(editMenu);
      menuBar.add(viewMenu);
      
      contextMenu = new JPopupMenu();
      contextMenu.add(actions.get(FCActionType.EDIT_CUT));
      contextMenu.add(actions.get(FCActionType.EDIT_COPY));
      contextMenu.add(actions.get(FCActionType.EDIT_PASTE));
      contextMenu.add(actions.get(FCActionType.EDIT_DEL));
      contextMenu.add(actions.get(FCActionType.EDIT_SELECT));
      contextMenu.addSeparator();
      contextMenu.add(actions.get(FCActionType.EDIT_COPYSTYLE));
      contextMenu.add(actions.get(FCActionType.EDIT_PASTESTYLE));
      contextMenu.addSeparator();
      alignMenu = new JMenu("Align");
      contextMenu.add(alignMenu);
      alignMenu.add(actions.get(FCActionType.ALIGN_LEFT));
      alignMenu.add(actions.get(FCActionType.ALIGN_RIGHT));
      alignMenu.add(actions.get(FCActionType.ALIGN_BOTTOM));
      alignMenu.add(actions.get(FCActionType.ALIGN_TOP));
      alignMenu.add(actions.get(FCActionType.ALIGN_HCENTER));
      alignMenu.add(actions.get(FCActionType.ALIGN_VCENTER));
      contextMenu.addSeparator();
      insertMenu = new JMenu("Insert");
      contextMenu.add(insertMenu);
      menu3D = new JMenu("3D");
      insertMenu.add(menu3D);
      menu3D.add(actions.get(FCActionType.ADD_GRAPH3D));
      menu3D.add(actions.get(FCActionType.ADD_SCATTER3D));
      menu3D.add(actions.get(FCActionType.ADD_SURFACE));
      menu2D = new JMenu("2D");
      insertMenu.add(menu2D);
      menu2D.add(actions.get(FCActionType.ADD_GRAPH));
      menu2D.add(actions.get(FCActionType.ADD_POLARG));
      menu2D.add(actions.get(FCActionType.ADD_TRACE));
      menu2D.add(actions.get(FCActionType.ADD_FUNCTION));
      menu2D.add(actions.get(FCActionType.ADD_RASTER));
      menu2D.add(actions.get(FCActionType.ADD_CONTOUR));
      menu2D.add(actions.get(FCActionType.ADD_BOX));
      menu2D.add(actions.get(FCActionType.ADD_BAR));
      menu2D.add(actions.get(FCActionType.ADD_AREA));
      menu2D.add(actions.get(FCActionType.ADD_PIE));
      menu2D.add(actions.get(FCActionType.ADD_SCATTER));
      menu2D.add(actions.get(FCActionType.ADD_CALIB));
      insertMenu.add(actions.get(FCActionType.ADD_LABEL));
      insertMenu.add(actions.get(FCActionType.ADD_TEXTBOX));
      insertMenu.add(actions.get(FCActionType.ADD_IMAGE));
      insertMenu.add(actions.get(FCActionType.ADD_SHAPE));
      insertMenu.add(actions.get(FCActionType.ADD_LINE));
      contextMenu.addSeparator();
      contextMenu.add(new JCheckBoxMenuItem(actions.get(FCActionType.VIEW_TOGGLERULERS)));
      contextMenu.add(new JCheckBoxMenuItem(actions.get(FCActionType.VIEW_TOGGLEMODE)));
      contextMenu.add(new JCheckBoxMenuItem(actions.get(FCActionType.VIEW_TOGGLEPRINTPREVIEW)));
      contextMenu.add(actions.get(FCActionType.VIEW_RESETZOOM));
      contextMenu.add(actions.get(FCActionType.VIEW_REFRESH));      
   }
   
   /** All action commands available in the figure composer. */
   private HashMap<FCActionType, FCAction> actions = null;
   
   /** 
    * Helper method creates the set of all action commands available in the figure composer. It is invoked prior to
    * construction of the composer's menu and tool bar panel, which expose the various commands to the user.
    */
   private void createActions()
   {
      if(actions != null) return;
      
      actions = new HashMap<>();
      
      // The typical menu shortcut modifier key. Typically the <i>Command</i> key or the <i>Control</i> key.
      int accMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
      
      // NOTE that the tooltips for the file-related actions never change, so their tooltips (including the hot key
      // mnemnoic if the action has a hot key assigned) are set here. The tooltips for the edit actions and the cursor
      // mode toggle action are updated dynamically.
      
      FCAction fca = new FCAction(FCActionType.FILE_NEW, "New", 
            KeyStroke.getKeyStroke(KeyEvent.VK_N, accMod), FCUIConstants.MODCMD + "N");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Create a new, empty figure  (" + FCUIConstants.MODCMD + "N)");
      actions.put(FCActionType.FILE_NEW, fca);
      
      fca = new FCAction(FCActionType.FILE_DUPE, "Duplicate", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Duplicate the current figure");
      actions.put(FCActionType.FILE_DUPE, fca);

      fca = new FCAction(FCActionType.FILE_OPEN, "Open",
            KeyStroke.getKeyStroke(KeyEvent.VK_O, accMod), FCUIConstants.MODCMD + "O");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Open an existing FypML or Matlab figure  (" + FCUIConstants.MODCMD + "O)");
      actions.put(FCActionType.FILE_OPEN, fca);

      fca = new FCAction(FCActionType.FILE_OPENRECENT, "Recent", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Open a recent figure");
      actions.put(FCActionType.FILE_OPENRECENT, fca);

      fca = new FCAction(FCActionType.FILE_CLOSE, "Current figure", KeyStroke.getKeyStroke(KeyEvent.VK_W, accMod), 
            FCUIConstants.MODCMD + "W");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Close the currently selected figure  (" + FCUIConstants.MODCMD + "W)");
      actions.put(FCActionType.FILE_CLOSE, fca);

      fca = new FCAction(FCActionType.FILE_CLOSEALL, "All figures",
            KeyStroke.getKeyStroke(KeyEvent.VK_W, accMod|ActionEvent.ALT_MASK), 
            FCUIConstants.MODCMD + FCUIConstants.MODALT + "W");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "All open figures  (" + 
            FCUIConstants.MODCMD + FCUIConstants.MODALT + "W)");
      actions.put(FCActionType.FILE_CLOSEALL, fca);

      fca = new FCAction(FCActionType.FILE_CLOSEVIS, "All visible figures", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Close all figures in visible tabs");
      actions.put(FCActionType.FILE_CLOSEVIS, fca);

      fca = new FCAction(FCActionType.FILE_CLOSEHIDDEN, "All hidden figures", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Close all figures NOT among the visible tabs");
      actions.put(FCActionType.FILE_CLOSEHIDDEN, fca);

      fca = new FCAction(FCActionType.FILE_CLOSEALLBUTCURR, "All except current figure", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Close all open figure except the current selection");
      actions.put(FCActionType.FILE_CLOSEALLBUTCURR, fca);

      fca = new FCAction(FCActionType.FILE_CLOSELEFT, "All left of current figure", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, 
            "Close all open figures to the left of the current selection (including any hidden figures)");
      actions.put(FCActionType.FILE_CLOSELEFT, fca);

      fca = new FCAction(FCActionType.FILE_CLOSERIGHT, "All right of current figure", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, 
            "Close all open figures to the right of the current selection (including any hidden figures)");
      actions.put(FCActionType.FILE_CLOSERIGHT, fca);

      fca = new FCAction(FCActionType.FILE_CLOSEUNMOD, "All unmodified figures", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Close all open figures that have not yet been changed");
      actions.put(FCActionType.FILE_CLOSEUNMOD, fca);

      fca = new FCAction(FCActionType.FILE_REVERT, "Revert", 
            KeyStroke.getKeyStroke(KeyEvent.VK_R, accMod|ActionEvent.SHIFT_MASK), 
            FCUIConstants.MODSHIFT + FCUIConstants.MODCMD + "R");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Discard changes to selected figure and revert to last saved state (" + 
            FCUIConstants.MODSHIFT + FCUIConstants.MODCMD + "R");
      actions.put(FCActionType.FILE_REVERT, fca);

      fca = new FCAction(FCActionType.FILE_SAVE, "Save", KeyStroke.getKeyStroke(KeyEvent.VK_S, accMod), 
            FCUIConstants.MODCMD + "S");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Save changes in the current figure  (" + FCUIConstants.MODCMD + "S)");
      actions.put(FCActionType.FILE_SAVE, fca);

      fca = new FCAction(FCActionType.FILE_SAVEAS, "Save As",
            KeyStroke.getKeyStroke(KeyEvent.VK_S, accMod|ActionEvent.SHIFT_MASK), 
            FCUIConstants.MODCMD + FCUIConstants.MODSHIFT + "S");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Save current figure as...  (" + 
            FCUIConstants.MODCMD + FCUIConstants.MODSHIFT + "S)");
      actions.put(FCActionType.FILE_SAVEAS, fca);

      fca = new FCAction(FCActionType.FILE_INJECTDATA, "Inject Data", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Inject raw data sets from a source figure into the current figure");
      actions.put(FCActionType.FILE_INJECTDATA, fca);
      
      fca = new FCAction(FCActionType.FILE_EXPORT, "Export", 
            KeyStroke.getKeyStroke(KeyEvent.VK_E, accMod|ActionEvent.SHIFT_MASK), 
            FCUIConstants.MODCMD + FCUIConstants.MODSHIFT + "E");
      fca.putValue(FCAction.SHORT_DESCRIPTION, 
            "Export current figure to another file format (" + FCUIConstants.MODSHIFT + FCUIConstants.MODCMD + "E");
      actions.put(FCActionType.FILE_EXPORT, fca);

      fca = new FCAction(FCActionType.FILE_EXPALL_PS, "Export all to EPS", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Export all open figures as Encapsulated Postscript documents");
      actions.put(FCActionType.FILE_EXPALL_PS, fca);

      fca = new FCAction(FCActionType.FILE_EXPALL_PDF, "Export all to PDF", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Export all open figures as Portable Document Format files");
      actions.put(FCActionType.FILE_EXPALL_PDF, fca);

      fca = new FCAction(FCActionType.FILE_EXPALL_JPG, "Export all to JPEG", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Export all open figures as JPEG images");
      actions.put(FCActionType.FILE_EXPALL_JPG, fca);

      fca = new FCAction(FCActionType.FILE_EXPALL_PNG, "Export all to PNG", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Export all open figures as PNG images");
      actions.put(FCActionType.FILE_EXPALL_PNG, fca);

      fca = new FCAction(FCActionType.FILE_EXPALL_SVG, "Export all to SVG", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Export all open figures as Scalable Vector Graphics (SVG) files");
      actions.put(FCActionType.FILE_EXPALL_SVG, fca);

      fca = new FCAction(FCActionType.FILE_PAGESETUP,  "Page Setup",
            KeyStroke.getKeyStroke(KeyEvent.VK_P, accMod|ActionEvent.SHIFT_MASK), 
            FCUIConstants.MODCMD + FCUIConstants.MODSHIFT + "P");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Change printer page layout for current figure  (" + 
            FCUIConstants.MODCMD + FCUIConstants.MODSHIFT + "P)");
      actions.put(FCActionType.FILE_PAGESETUP, fca);

      fca = new FCAction(FCActionType.FILE_PRINT, "Print", 
            KeyStroke.getKeyStroke(KeyEvent.VK_P, accMod), FCUIConstants.MODCMD + "P");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Print the current figure (" + FCUIConstants.MODCMD + "P)");
      actions.put(FCActionType.FILE_PRINT, fca);

      fca = new FCAction(FCActionType.FILE_PRINTALL, "Print all", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Print all open figures");
      actions.put(FCActionType.FILE_PRINTALL, fca);

      fca = new FCAction(FCActionType.EDIT_COPY, "Copy", KeyStroke.getKeyStroke(KeyEvent.VK_C, accMod), 
            FCUIConstants.MODCMD + "C");
      actions.put(FCActionType.EDIT_COPY, fca);
      
      fca = new FCAction(FCActionType.EDIT_CUT, "Cut", KeyStroke.getKeyStroke(KeyEvent.VK_X, accMod), 
            FCUIConstants.MODCMD + "X");
      actions.put(FCActionType.EDIT_CUT, fca);
      
      fca = new FCAction(FCActionType.EDIT_PASTE, "Paste", KeyStroke.getKeyStroke(KeyEvent.VK_V, accMod), 
            FCUIConstants.MODCMD + "V");
      actions.put(FCActionType.EDIT_PASTE, fca);
      
      // on Mac keybaord, the key labeled "delete" is actually VK_BACK_SPACE
      fca = new FCAction(FCActionType.EDIT_DEL, "Delete",
            KeyStroke.getKeyStroke(Utilities.isMacOS() ? KeyEvent.VK_BACK_SPACE : KeyEvent.VK_DELETE, 0), "Del");
      actions.put(FCActionType.EDIT_DEL, fca);
      
      fca = new FCAction(FCActionType.EDIT_SELECT, "Select like elements", 
            KeyStroke.getKeyStroke(KeyEvent.VK_A, accMod), 
            FCUIConstants.MODCMD + "A");
      fca.putValue(FCAction.SHORT_DESCRIPTION, 
            "Select all elements in figure with the same type as the currently selected element");
      actions.put(FCActionType.EDIT_SELECT, fca);
      
      fca = new FCAction(FCActionType.EDIT_UNDO, "Undo", KeyStroke.getKeyStroke(KeyEvent.VK_Z, accMod), 
            FCUIConstants.MODCMD + "Z");
      actions.put(FCActionType.EDIT_UNDO, fca);

      fca = new FCAction(FCActionType.EDIT_REDO, "Redo", 
            KeyStroke.getKeyStroke(KeyEvent.VK_Z, accMod|ActionEvent.SHIFT_MASK), 
            FCUIConstants.MODCMD + FCUIConstants.MODSHIFT + "Z");
      actions.put(FCActionType.EDIT_REDO, fca);

      fca = new FCAction(FCActionType.EDIT_EXTDATA, "Extract raw data", 
            KeyStroke.getKeyStroke(KeyEvent.VK_1, accMod), 
            FCUIConstants.MODCMD + "1");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Extract a copy of the raw data set for this plottable data node");
      actions.put(FCActionType.EDIT_EXTDATA, fca);
      
      fca = new FCAction(FCActionType.EDIT_INJDATA, "Inject raw data", 
            KeyStroke.getKeyStroke(KeyEvent.VK_2, accMod), 
            FCUIConstants.MODCMD + "2");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Inject raw data set from clipboard into this plottable data node");
      actions.put(FCActionType.EDIT_INJDATA, fca);
      
      fca = new FCAction(FCActionType.EDIT_COPYSTYLE, "Copy styling", 
            KeyStroke.getKeyStroke(KeyEvent.VK_3, accMod), 
            FCUIConstants.MODCMD + "3");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Copy the style property set for this node");
      actions.put(FCActionType.EDIT_COPYSTYLE, fca);
      
      fca = new FCAction(FCActionType.EDIT_PASTESTYLE, "Paste styling", 
            KeyStroke.getKeyStroke(KeyEvent.VK_4, accMod), 
            FCUIConstants.MODCMD + "4");
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Apply the last-copied style property set to this node");
      actions.put(FCActionType.EDIT_PASTESTYLE, fca);
      
      fca = new FCAction(FCActionType.ALIGN_LEFT, FGNAlign.LEFT.toString(), null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Align selected nodes along a common left edge");
      actions.put(FCActionType.ALIGN_LEFT, fca);
      
      fca = new FCAction(FCActionType.ALIGN_RIGHT, FGNAlign.RIGHT.toString(), null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Align selected nodes along a common right edge");
      actions.put(FCActionType.ALIGN_RIGHT, fca);
      
      fca = new FCAction(FCActionType.ALIGN_BOTTOM, FGNAlign.BOTTOM.toString(), null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Align selected nodes along a common bottom edge");
      actions.put(FCActionType.ALIGN_BOTTOM, fca);
      
      fca = new FCAction(FCActionType.ALIGN_TOP, FGNAlign.TOP.toString(), null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Align selected nodes along a common top edge");
      actions.put(FCActionType.ALIGN_TOP, fca);
      
      fca = new FCAction(FCActionType.ALIGN_HCENTER, FGNAlign.HCENTER.toString(), null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Horizontally center the selected nodes");
      actions.put(FCActionType.ALIGN_HCENTER, fca);
      
      fca = new FCAction(FCActionType.ALIGN_VCENTER, FGNAlign.VCENTER.toString(), null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Vertically center the selected nodes");
      actions.put(FCActionType.ALIGN_VCENTER, fca);
      
      fca = new FCAction(FCActionType.ADD_SHAPE, "Shape",
            KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "W");
      actions.put(FCActionType.ADD_SHAPE, fca);

      fca = new FCAction(FCActionType.ADD_LABEL, "Text label",
            KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "L");
      actions.put(FCActionType.ADD_LABEL, fca);

      fca = new FCAction(FCActionType.ADD_TEXTBOX, "Text box",
            KeyStroke.getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "E");
      actions.put(FCActionType.ADD_TEXTBOX, fca);

      fca = new FCAction(FCActionType.ADD_IMAGE, "Image",
            KeyStroke.getKeyStroke(KeyEvent.VK_M, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "M");
      actions.put(FCActionType.ADD_IMAGE, fca);

      fca = new FCAction(FCActionType.ADD_LINE, "Line segment",
            KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "N");
      actions.put(FCActionType.ADD_LINE, fca);

      fca = new FCAction(FCActionType.ADD_GRAPH3D, "3D Graph",
            KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "D");
      actions.put(FCActionType.ADD_GRAPH3D, fca);

      fca = new FCAction(FCActionType.ADD_SCATTER3D, "3D Scatter/Line Plot", null, null);
      actions.put(FCActionType.ADD_SCATTER3D, fca);

      fca = new FCAction(FCActionType.ADD_SURFACE, "Surface Plot", null, null);
      actions.put(FCActionType.ADD_SURFACE, fca);

      fca = new FCAction(FCActionType.ADD_GRAPH, "Graph",
            KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "G");
      actions.put(FCActionType.ADD_GRAPH, fca);

      fca = new FCAction(FCActionType.ADD_POLARG, "Polar Plot",
            KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "P");
      actions.put(FCActionType.ADD_POLARG, fca);

      fca = new FCAction(FCActionType.ADD_TRACE, "Trace",
            KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "T");
      actions.put(FCActionType.ADD_TRACE, fca);

      fca = new FCAction(FCActionType.ADD_RASTER, "Raster",
            KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "R");
      actions.put(FCActionType.ADD_RASTER, fca);

      fca = new FCAction(FCActionType.ADD_CONTOUR, "Contour Plot",
            KeyStroke.getKeyStroke(KeyEvent.VK_H, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "H");
      actions.put(FCActionType.ADD_CONTOUR, fca);

      fca = new FCAction(FCActionType.ADD_BOX, "Box Plot", null, null);
      actions.put(FCActionType.ADD_BOX, fca);

      fca = new FCAction(FCActionType.ADD_BAR, "Bar Plot",
            KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "B");
      actions.put(FCActionType.ADD_BAR, fca);

      fca = new FCAction(FCActionType.ADD_AREA, "Area Chart",
            KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "A");
      actions.put(FCActionType.ADD_AREA, fca);

      fca = new FCAction(FCActionType.ADD_PIE, "Pie Chart",
            KeyStroke.getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "Y");
      actions.put(FCActionType.ADD_PIE, fca);

      fca = new FCAction(FCActionType.ADD_SCATTER, "2D Scatter Plot",
            KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "K");
      actions.put(FCActionType.ADD_SCATTER, fca);

      fca = new FCAction(FCActionType.ADD_FUNCTION, "Function",
            KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "F");
      actions.put(FCActionType.ADD_FUNCTION, fca);

      fca = new FCAction(FCActionType.ADD_CALIB, "Calibration bar",
            KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK|KeyEvent.SHIFT_DOWN_MASK), 
            FCUIConstants.MODCTRLSHIFT + "C");
      actions.put(FCActionType.ADD_CALIB, fca);

      fca = new FCAction(FCActionType.VIEW_TOGGLETREE, "Tree view",
            KeyStroke.getKeyStroke(KeyEvent.VK_T, accMod), FCUIConstants.MODCMD + "T");
      actions.put(FCActionType.VIEW_TOGGLETREE, fca);

      fca = new FCAction(FCActionType.VIEW_TOOLBAR_HIDE, TOOLS_HIDDEN, null, null);
      actions.put(FCActionType.VIEW_TOOLBAR_HIDE, fca);

      fca = new FCAction(FCActionType.VIEW_TOOLBAR_COMPACT, TOOLS_COMPACT, null, null);
      actions.put(FCActionType.VIEW_TOOLBAR_COMPACT, fca);

      fca = new FCAction(FCActionType.VIEW_TOOLBAR_FULL, TOOLS_FULL, null, null);
      actions.put(FCActionType.VIEW_TOOLBAR_FULL, fca);

      fca = new FCAction(FCActionType.VIEW_TOGGLERULERS, "Rulers", 
            KeyStroke.getKeyStroke(KeyEvent.VK_R, accMod), FCUIConstants.MODCMD + "R");
      actions.put(FCActionType.VIEW_TOGGLERULERS, fca);

      fca = new FCAction(FCActionType.VIEW_SPECIALCHARS, "Special Characters", 
            KeyStroke.getKeyStroke(KeyEvent.VK_K, accMod), FCUIConstants.MODCMD + "K");
      actions.put(FCActionType.VIEW_SPECIALCHARS, fca);

      fca = new FCAction(FCActionType.VIEW_TOGGLEWSFIG, "Workspace Figures", 
            KeyStroke.getKeyStroke(KeyEvent.VK_F, accMod), FCUIConstants.MODCMD + "F");
      actions.put(FCActionType.VIEW_TOGGLEWSFIG, fca);

      fca = new FCAction(FCActionType.VIEW_TOGGLEMODE, "Scale-to-fit", null, null);
      fca.putValue(FCAction.SELECTED_KEY, Boolean.TRUE);
      actions.put(FCActionType.VIEW_TOGGLEMODE, fca);

      fca = new FCAction(FCActionType.VIEW_TOGGLEPRINTPREVIEW, "Print preview", 
            KeyStroke.getKeyStroke(KeyEvent.VK_P, accMod|ActionEvent.ALT_MASK), 
            FCUIConstants.MODALT + FCUIConstants.MODCMD + "P");
      actions.put(FCActionType.VIEW_TOGGLEPRINTPREVIEW, fca);

      fca = new FCAction(FCActionType.VIEW_RESETZOOM, "Actual size", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Reset current zoom and pan transform and restore actual size");
      actions.put(FCActionType.VIEW_RESETZOOM, fca);

      fca = new FCAction(FCActionType.VIEW_REFRESH, "Refresh", null, null);
      fca.putValue(FCAction.SHORT_DESCRIPTION, "Refresh currently displayed figure");
      actions.put(FCActionType.VIEW_REFRESH, fca);
   }

   /**
    * Get the figure graphic node type corresponding to the specified insert-node action type.
    * @param type One of the figure composer's "insert graphic node" action types.
    * @return Type of graphic node inserted by the specified action. Returns null if specified action type is not an
    * insert-node action.
    */
   private static FGNodeType getNodeTypeForInsertAction(FCActionType type)
   {
      FGNodeType nodeType;
      switch(type)
      {
      case ADD_LABEL : nodeType = FGNodeType.LABEL; break;
      case ADD_TEXTBOX : nodeType = FGNodeType.TEXTBOX; break;
      case ADD_IMAGE : nodeType = FGNodeType.IMAGE; break;
      case ADD_LINE : nodeType = FGNodeType.LINE; break;
      case ADD_SHAPE : nodeType = FGNodeType.SHAPE; break;
      case ADD_GRAPH : nodeType = FGNodeType.GRAPH; break;
      case ADD_TRACE : nodeType = FGNodeType.TRACE; break;
      case ADD_FUNCTION : nodeType = FGNodeType.FUNCTION; break;
      case ADD_RASTER : nodeType = FGNodeType.RASTER; break;
      case ADD_CONTOUR : nodeType = FGNodeType.CONTOUR; break;
      case ADD_BOX : nodeType = FGNodeType.BOX; break;
      case ADD_BAR : nodeType = FGNodeType.BAR; break;
      case ADD_AREA : nodeType = FGNodeType.AREA; break;
      case ADD_PIE : nodeType = FGNodeType.PIE; break;
      case ADD_SCATTER : nodeType = FGNodeType.SCATTER; break;
      case ADD_CALIB : nodeType = FGNodeType.CALIB; break;
      case ADD_GRAPH3D : nodeType = FGNodeType.GRAPH3D; break;
      case ADD_SCATTER3D : nodeType = FGNodeType.SCATTER3D; break;
      case ADD_SURFACE : nodeType = FGNodeType.SURFACE; break;
      case ADD_POLARG : nodeType = FGNodeType.PGRAPH; break;
      default : nodeType = null; break;
      }
      return(nodeType);
   }
   
   /** Refresh the enabled state and tool tip for all installed actions. Invoke whenever the focus node changes. */
   private void refreshActions() 
   { 
      // is keyboard focus on a text component? If so the standard edit operations cut/copy/paste apply to the
      // text component and NOT to the current focus node, so we need to disable the corres. node-related edit ops!
      Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
      focusOnTextComponent = (c instanceof JTextComponent);
      
      for(FCAction fca : actions.values()) fca.refreshState();
   }
   
   /** Flag set if keyboard focus is on a text component. */
   private transient boolean focusOnTextComponent = false;
   
   /** The last raw data set copied via the {@link FCActionType#EDIT_EXTDATA} action. */
   private DataSet lastDSCopied = null;
   
   /** The last style set copied via the {@link FCActionType#EDIT_COPYSTYLE} action. Null at start-up. */
   private FGNStyleSet lastStyleSetCopied = null;
   
   /**
    * An enumeration of all action commands available in the figure composer.
    * @author sruffner
    */
   private enum FCActionType
   {
      /** Create a new, empty figure. */ FILE_NEW,
      /** Duplicate the currently selected figure. */ FILE_DUPE,
      /** Open an existing figure (raise file chooser) -- either FypML or Matlab FIG. */ FILE_OPEN, 
      /** Open a recent figure. */ FILE_OPENRECENT,
      /** Close the currently selected figure. */ FILE_CLOSE,
      /** Close all open figures. */ FILE_CLOSEALL,
      /** Close all visible figures (in exposed tabs). */ FILE_CLOSEVIS,
      /** Close any hidden figures (not currently exposed on tabs). */ FILE_CLOSEHIDDEN,
      /** Close all open figures except the current selection. */ FILE_CLOSEALLBUTCURR,
      /** Close all open figures left of current selection. */ FILE_CLOSELEFT,
      /** Close all open figures right of current selection. */ FILE_CLOSERIGHT,
      /** Close all figures that have not yet been modified. */ FILE_CLOSEUNMOD,
      /** Discard changes to current figure and revert to last saved state. */ FILE_REVERT,
      /** Save any changes in currently selected figure. */ FILE_SAVE,
      /** Save currently selected figure to another location in file system. */ FILE_SAVEAS,
      /** Inject raw data from a source figure into currently selected figure. */ FILE_INJECTDATA,
      /** Export currently selected figure to a supported file format. */ FILE_EXPORT,
      /** Export all open figures as Encapsulated Postscript documents. */FILE_EXPALL_PS,
      /** Export all open figures as Portable Document Format documents. */FILE_EXPALL_PDF,
      /** Export all open figures as JPEG images. */ FILE_EXPALL_JPG,
      /** Export all open figures as PNG images. */ FILE_EXPALL_PNG,
      /** Export all open figures as Scalable Vector Graphics documents. */FILE_EXPALL_SVG,
      /** Change the page layout for the currently selected figure. */ FILE_PAGESETUP,
      /** Print the currently selected figure. */ FILE_PRINT,
      /** Print all open figures. */ FILE_PRINTALL,
      /** Copy the focus node in the currently selected figure. */ EDIT_COPY,
      /** Cut the focus node in the currently selected figure. */ EDIT_CUT,
      /** Paste clipboard node as a child or sibling of the current focus node in current figure. */ EDIT_PASTE,
      /** Delete the focus node in the currently selected figure, not saving it to clipboard. */ EDIT_DEL,
      /** Select all elements with the same type as the current focus node. */ EDIT_SELECT,
      /** Undo the last edit on the currently selected figure, if possible. */ EDIT_UNDO,
      /** Redo the last undone edit on the currently selected figure, if possible. */ EDIT_REDO,
      /** Copy the focus node's style set so it can be applied to another object. */ EDIT_COPYSTYLE,
      /** Apply the last-copied style set to the current focus node. */ EDIT_PASTESTYLE,
      /** Extract copy of the raw data set for the current focus node (plottable data nodes only). */ EDIT_EXTDATA,
      /** Inject the last copied raw data set into the current focus node (plottable data nodes only). */ EDIT_INJDATA,
      /** Align nodes in current selection along a common left edge. */ ALIGN_LEFT,
      /** Align nodes in current selection along a common right edge. */ ALIGN_RIGHT,
      /** Align nodes in current selection along a common bottom edge. */ ALIGN_BOTTOM,
      /** Align nodes in current selection along a common top edge. */ ALIGN_TOP,
      /** Horizontally center nodes in the current selection. */ ALIGN_HCENTER,
      /** Vertically center nodes in the current selection. */ ALIGN_VCENTER,
      /** Insert a shape node as a child of the current focus node. */ ADD_SHAPE,
      /** Insert a text label as a child of the current focus node. */ ADD_LABEL,
      /** Insert a text box as a child of the current focus node. */ ADD_TEXTBOX,
      /** Insert an image node as a child of the current focus node. */ ADD_IMAGE,
      /** Insert a line segment as a child of the current focus node. */ ADD_LINE,
      /** Insert a 3D graph as a child of the current focus node. */ ADD_GRAPH3D,
      /** Insert a 3D scatter plot as a child of the current focus node. */ ADD_SCATTER3D,
      /** Insert a 3D surface plot as a child of the current focus node. */ ADD_SURFACE,
      /** Insert a graph as a child of the current focus node. */ ADD_GRAPH,
      /** Insert a 2D polar graph as a child of the current focus node. */ ADD_POLARG,
      /** Insert a trace node as a child of the current focus node. */ ADD_TRACE,
      /** Insert a raster node as a child of the current focus node. */ ADD_RASTER,
      /** Insert a contour plot node as a child of the current focus node. */ ADD_CONTOUR,
      /** Insert a box plot node as a child of the current focus node. */ ADD_BOX,
      /** Insert a bar plot node as a child of the current focus node. */ ADD_BAR,
      /** Insert an area chart as a child of the current focus node. */ ADD_AREA,
      /** Insert a pie chart as a child of the current focus node. */ ADD_PIE, 
      /** Insert a scatter plot node a a child of the current focus node. */ ADD_SCATTER,
      /** Insert a function node as a child of the current focus node. */ ADD_FUNCTION,
      /** Insert a calibration bar as a child of the current focus node. */ ADD_CALIB,
      /** Hide toolbar. */ VIEW_TOOLBAR_HIDE,
      /** Show toolbar in compact state (no labels). */ VIEW_TOOLBAR_COMPACT,
      /** Show toolbar in full state (with labels). */ VIEW_TOOLBAR_FULL,
      /** Toggle on/off the node tree view for the current figure. */ VIEW_TOGGLETREE,
      /** Toggle on/off the canvas H and V rulers. */ VIEW_TOGGLERULERS,
      /** Toggle on/off the special characters tool dialog. */ VIEW_SPECIALCHARS,
      /** Toggle on/off the compact workspace figures browser. */ VIEW_TOGGLEWSFIG,
      /** Toggle between normal and scale-to-fit view modes. */ VIEW_TOGGLEMODE,
      /** Toggle on/off the print preview mode, which shows figure laid out on printed page. */ VIEW_TOGGLEPRINTPREVIEW,
      /** Reset current zoom and pan transform, if any. */ VIEW_RESETZOOM,
      /** Refresh the figure displayed in the figure canvas. */ VIEW_REFRESH
   }

   /**
    * Helper class implements each of the different command actions available on the figure composer.
    * @author sruffner
    */
   private class FCAction extends AbstractAction
   {
      /**
       * Construct a figure composer action.
       * @param type The action type ID.
       * @param name The action command name.
       * @param accel The keyboard shortcut, or "hot key", associated with this action. Null if there is none.
       * @param accelMnemonic The mnemonic for the keyboard shortcut; null if there is no shortcut.
       */
      FCAction(FCActionType type, String name, KeyStroke accel, String accelMnemonic)
      {
         assert(type != null);
         this.type = type;
         putValue(NAME, name);
         putValue(ACTION_COMMAND_KEY, type.toString());
         if(accel != null) putValue(ACCELERATOR_KEY, accel);
         hotKeyMnemonic = accelMnemonic;
      }
      
      @Override public void actionPerformed(ActionEvent e)
      {
         JFrame appFrame = null;
         Object ancestor = SwingUtilities.getWindowAncestor(FigComposer.this);
         if(ancestor instanceof JFrame) appFrame = (JFrame) ancestor;
         
         FCChooser chooser = FCChooser.getInstance();
         FGraphicModel currFig = getSelectedFigureModel();
         FGraphicNode sel = (currFig == null || currFig.isMultiNodeSelection()) ? null : currFig.getSelectedNode();
         File currFile = getSelectedFigureFile();
         FGraphicModel[] figs;
         
         switch(type)
         {
         case FILE_NEW :
         case FILE_DUPE :
         case FILE_OPEN :
            doOpen(type);
            break;
         case FILE_OPENRECENT :
            prepareMRUFileMenu(true);
            break;
         case FILE_CLOSE :
         case FILE_CLOSEALL :
         case FILE_CLOSEVIS :
         case FILE_CLOSEHIDDEN :
         case FILE_CLOSEALLBUTCURR :
         case FILE_CLOSELEFT :
         case FILE_CLOSERIGHT :
         case FILE_CLOSEUNMOD :
            doClose(type);
            break;
         case FILE_REVERT :
            revertCurrentFigure();
            break;
         case FILE_SAVE :
         case FILE_SAVEAS :
            saveCurrentFigure(type);
            break;
         case FILE_INJECTDATA :
            DataInjectionDlg.injectDataIntoFigure(FigComposer.this, currFig, currFile);
            break;
         case FILE_EXPORT :
            chooser.exportFigure(FigComposer.this, currFig, currFile);
            break;
         case FILE_EXPALL_PS :
         case FILE_EXPALL_PDF :
         case FILE_EXPALL_JPG :
         case FILE_EXPALL_PNG :
         case FILE_EXPALL_SVG :
            if(openFigures.isEmpty()) return;
            figs = new FGraphicModel[openFigures.size()];
            File[] files = new File[openFigures.size()];
            for(int i=0; i<openFigures.size(); i++)
            {
               figs[i] = openFigures.get(i).fig;
               files[i] = openFigures.get(i).src;
            }
            FCFileType exportType = FCFileType.PS;
            if(type == FCActionType.FILE_EXPALL_PDF) exportType = FCFileType.PDF;
            else if(type == FCActionType.FILE_EXPALL_JPG) exportType = FCFileType.JPEG;
            else if(type == FCActionType.FILE_EXPALL_PNG) exportType = FCFileType.PNG;
            else if(type == FCActionType.FILE_EXPALL_SVG) exportType = FCFileType.SVG; 
            
            chooser.exportAllFigures(FigComposer.this, figs, files, exportType);
            break;
         case FILE_PAGESETUP :
            PageFormat pgf = PrinterSupport.getInstance().pageDialog(currFig, appFrame);
            if(pgf != null)
            {
               figCanvas.setPageFormat(pgf);
               mouseLayer.updateSelectionOverlay();
            }
            break;
         case FILE_PRINT :
         case FILE_PRINTALL :
            if(type == FCActionType.FILE_PRINT) 
               figs = new FGraphicModel[] { currFig };
            else
            {
               figs = new FGraphicModel[openFigures.size()];
               for(int i=0; i<openFigures.size(); i++) figs[i] = openFigures.get(i).fig;
            }
            
            PrinterSupport.getInstance().printDialog(figs, appFrame);
            break;
         case EDIT_UNDO :
            if(currFig != null) currFig.undoOrRedo(false);
            break;
         case EDIT_REDO :
            if(currFig != null) currFig.undoOrRedo(true);
            break;
         case EDIT_COPY :
            if(currFig != null && FGraphicModel.copyToClipboard(currFig.getSelectedNodes(null)))
            {
               // a successful copy action affects the state of the paste action
               actions.get(FCActionType.EDIT_PASTE).refreshState();
            }
            break;
         case EDIT_CUT :
            if(currFig != null && currFig.canDeleteCurrentSelection())
            {
               currFig.deleteCurrentSelection(true);
               actions.get(FCActionType.EDIT_PASTE).refreshState();
            }
            break;
         case EDIT_DEL :
            if(currFig != null && currFig.canDeleteCurrentSelection()) currFig.deleteCurrentSelection(false);
            break;
         case EDIT_SELECT :
            if(sel != null) currFig.selectAllNodesLikeCurrentSelection();
            break;
         case EDIT_PASTE :
            insertOrPasteNode(null);
            break;
         case EDIT_EXTDATA :
            if(sel instanceof FGNPlottableData)
            {
               lastDSCopied = ((FGNPlottableData) sel).getDataSet();
               actions.get(FCActionType.EDIT_INJDATA).refreshState();
            }
            break;
         case EDIT_INJDATA :
            if((lastDSCopied != null) && (sel instanceof FGNPlottableData) && 
                  ((FGNPlottableData)sel).isSupportedDataFormat(lastDSCopied.getFormat()))
            {
               FGNPlottableData plottable = (FGNPlottableData) sel;
               String oldID = plottable.getDataSetID();
               plottable.setDataSet(lastDSCopied.changeID(oldID));
            }
            break;
         case EDIT_COPYSTYLE :
            if(sel != null && sel.supportsStyleSet())
            {
               FGNStyleSet styleSet = sel.getCurrentStyleSet();
               if(styleSet != null)
               {
                  lastStyleSetCopied = styleSet;
                  actions.get(FCActionType.EDIT_PASTESTYLE).refreshState();
               }
            }
            break;
         case EDIT_PASTESTYLE :
            if(lastStyleSetCopied != null && currFig != null && 
                  currFig.canApplyStylingToCurrentSelection(lastStyleSetCopied))
            {
               currFig.applyStylngToCurrentSelection(lastStyleSetCopied);
               actions.get(FCActionType.EDIT_PASTESTYLE).refreshState();
            }
            break;
         case ALIGN_LEFT :
         case ALIGN_RIGHT :
         case ALIGN_BOTTOM :
         case ALIGN_TOP :
         case ALIGN_HCENTER :
         case ALIGN_VCENTER :
            if(currFig != null && currFig.canAlignCurrentSelection())
            {
               FGNAlign align;
               switch(type)
               {
               case ALIGN_RIGHT : align = FGNAlign.RIGHT; break;
               case ALIGN_BOTTOM : align = FGNAlign.BOTTOM; break;
               case ALIGN_TOP : align = FGNAlign.TOP; break;
               case ALIGN_HCENTER : align = FGNAlign.HCENTER; break;
               case ALIGN_VCENTER : align = FGNAlign.VCENTER; break;
               case ALIGN_LEFT :
               default: align = FGNAlign.LEFT; break;
               }
               currFig.alignCurrentSelection(align);
            }
            break;

         case VIEW_TOOLBAR_HIDE:
         case VIEW_TOOLBAR_COMPACT:
         case VIEW_TOOLBAR_FULL:
            String state = TOOLS_HIDDEN;
            if(type == FCActionType.VIEW_TOOLBAR_COMPACT) state = TOOLS_COMPACT;
            else if(type == FCActionType.VIEW_TOOLBAR_FULL) state = TOOLS_FULL;
            
            setToolsPanelState(state, true);
            
            // the 3 toolbar states are mutually exclusive!
            actions.get(FCActionType.VIEW_TOOLBAR_HIDE).refreshState();
            actions.get(FCActionType.VIEW_TOOLBAR_COMPACT).refreshState();
            actions.get(FCActionType.VIEW_TOOLBAR_FULL).refreshState();
            break;
         case VIEW_TOGGLETREE :
            figureTreeView.setVisible(!figureTreeView.isVisible());
            FigComposer.this.revalidate();
            refreshState();
            break;
         case VIEW_TOGGLERULERS :
            showRulers(!areRulersShown());
            FigComposer.this.revalidate();
            refreshState();
            break;
         case VIEW_SPECIALCHARS :
            // toggle the visibility of the special characters map within the node tree's property editor
            figureTreeView.setCharacterMapperVisible(!figureTreeView.isCharacterMapperVisible());
            refreshState();
            break;
         case VIEW_TOGGLEWSFIG :
            figBrowser.setVisible(!figBrowser.isVisible());
            FigComposer.this.revalidate();
            refreshState();
            break;
         case VIEW_TOGGLEMODE :
            figCanvas.setScaleToFitOn(!figCanvas.isScaleToFitOn());
            refreshState();
            actions.get(FCActionType.VIEW_RESETZOOM).refreshState();
            break;
         case VIEW_TOGGLEPRINTPREVIEW :
            figCanvas.setPrintPreviewEnabled(!figCanvas.isPrintPreviewEnabled());
            refreshState();
            break;
         case VIEW_RESETZOOM :
            figCanvas.resetZoomAndPan();
            break;
         case VIEW_REFRESH :
            figCanvas.refresh();
            break;
         default :
            // handle one of the insert-node actions: we always try to insert the new node as a child of the focus
            // node. If that's not possible, then try to insert it as a sibling of the focus node.
            if(sel != null) insertOrPasteNode(getNodeTypeForInsertAction(type));
            break;
         }
         
      }
      
      void refreshState()
      {
         boolean ena = true;
         String tip = null;
         
         FGraphicModel fig = getSelectedFigureModel();
         FGraphicNode sel = fig == null ? null : fig.getSelectedNode();
         String nodeTypeName = (sel == null) ? "" : sel.getNodeType().getNiceName().toLowerCase();
         boolean isMultiSel = (fig != null) && fig.isMultiNodeSelection();

         switch(type)
         {
         case FILE_NEW :
         case FILE_OPEN :
         case FILE_OPENRECENT :
         case FILE_CLOSE :
         case FILE_CLOSEALL :
         case FILE_CLOSEVIS :
         case FILE_CLOSEHIDDEN :
         case FILE_CLOSEALLBUTCURR :
         case FILE_CLOSELEFT :
         case FILE_CLOSERIGHT :
         case FILE_CLOSEUNMOD :
         case FILE_REVERT :
         case FILE_SAVE :
         case FILE_SAVEAS :
         case FILE_EXPORT : 
         case FILE_EXPALL_PS :
         case FILE_EXPALL_PDF :
         case FILE_EXPALL_JPG :
         case FILE_EXPALL_PNG :
         case FILE_EXPALL_SVG :
         case FILE_PAGESETUP : 
         case FILE_PRINT : 
         case FILE_PRINTALL :
            // most file-related actions are always enabled, and the tool tip never changes
            setEnabled(true);
            return;

         case FILE_DUPE:
            setEnabled(canDuplicateSelectedFigure());
            return;

         case FILE_INJECTDATA :
            // this command is available only when the current figure has at least one data presentation node
            setEnabled( (fig != null) && !(fig.getAllPlottableDataNodes().isEmpty()) );
            return;
         
         case ALIGN_LEFT :
         case ALIGN_RIGHT :
         case ALIGN_BOTTOM :
         case ALIGN_TOP :
         case ALIGN_HCENTER :
         case ALIGN_VCENTER :
            // these commands available only when multiple nodes are selected and can be aligned. Tip doesn't change.
            setEnabled((fig != null) && fig.canAlignCurrentSelection());
            return;
            
         case VIEW_REFRESH :
            // this action is always enabled, and its tool tip never changes
            return;

         case EDIT_COPY : 
            ena = (!focusOnTextComponent) && fig != null && fig.canCopyCurrentSelection();
            if(ena) tip = "Copy selected nodes to clipboard";
            break;
         case EDIT_CUT : 
            ena = (!focusOnTextComponent) && fig != null && fig.canDeleteCurrentSelection();
            if(ena) tip = "Cut selected nodes and save them to the clipboard";
            break;
         case EDIT_PASTE : 
            int n = FGraphicModel.getClipboardSize();
            ena = (!focusOnTextComponent) && (!isMultiSel) && (sel != null) && n > 0;
            if(ena) 
            {
               if(n > 1)
               {
                  ena = FGraphicModel.canPasteFromClipboard(sel);
                  if(ena) tip = String.format("Paste up to %d nodes from clipboard", n);
               }
               else
               {
                  FGNodeType pasteType = FGraphicModel.getClipboardContentType();
                  ena = canInsertNode(pasteType);
                  if(ena)
                     tip = String.format("Paste %s from clipboard",
                           (pasteType != null) ? pasteType.getNiceName().toLowerCase() : "");
               }
            }
            break;
         case EDIT_DEL : 
            ena = (!focusOnTextComponent) && fig != null && fig.canDeleteCurrentSelection();
            if(ena) tip = "Delete selected nodes";
            break;
         case EDIT_SELECT :
            ena = fig != null && !isMultiSel;
            if(ena) tip = "Select all elements in figure with the same type as the currently selected element";
            break;
         case EDIT_UNDO : 
            if(fig != null) tip = fig.getUndoDescription();
            ena = tip != null;
            break;
         case EDIT_REDO : 
            if(fig != null) tip = fig.getRedoDescription();
            ena = tip != null;
            break;
         case EDIT_EXTDATA :
            ena = (!isMultiSel) && (!focusOnTextComponent) && (sel instanceof FGNPlottableData);
            if(ena) tip = "Copy raw data from this plottable node into clipboard";
            break;
         case EDIT_INJDATA :
            ena = (lastDSCopied != null) && (!isMultiSel) && (!focusOnTextComponent) && (sel instanceof FGNPlottableData);
            if(ena) ena = ((FGNPlottableData) sel).isSupportedDataFormat(lastDSCopied.getFormat());
            if(ena) 
               tip = "Inject raw data from clipboard: " + lastDSCopied.getInfo().getShortDescription(false);
            break;
         case EDIT_COPYSTYLE :
            ena = (!isMultiSel) && (!focusOnTextComponent) && (sel != null) && sel.supportsStyleSet();
            if(ena) tip = "Copy style set from " + nodeTypeName;
            break;
         case EDIT_PASTESTYLE :
            ena = (lastStyleSetCopied != null) && (!focusOnTextComponent) && fig != null && 
                  fig.canApplyStylingToCurrentSelection(lastStyleSetCopied);
            if(ena) tip = "Apply style set from clipboard to selected nodes";
            break;
         case VIEW_TOOLBAR_HIDE:
            tip = "Hide toolbar buttons";
            putValue(SELECTED_KEY, TOOLS_HIDDEN.equals(toolsPanelState));
            break;
         case VIEW_TOOLBAR_COMPACT:
            tip = "Show compact toolbar buttons";
            putValue(SELECTED_KEY, TOOLS_COMPACT.equals(toolsPanelState));
            break;
         case VIEW_TOOLBAR_FULL:
            tip = "Show toolbar buttons with labels";
            putValue(SELECTED_KEY, TOOLS_FULL.equals(toolsPanelState));
            break;
         case VIEW_TOGGLETREE : 
            tip = "Show or hide the figure's node tree";
            putValue(SELECTED_KEY, figureTreeView.isVisible());
            break;
         case VIEW_TOGGLERULERS : 
            tip = "Show or hide the figure canvas rulers";
            putValue(SELECTED_KEY, areRulersShown());
            break;
         case VIEW_SPECIALCHARS :
            tip = "Show or hide the special characters map";
            putValue(SELECTED_KEY, figureTreeView.isCharacterMapperVisible());
            break;
         case VIEW_TOGGLEWSFIG : 
            tip = "Show or hide the workspace figures browser";
            putValue(SELECTED_KEY, figBrowser.isVisible());
            break;
        case VIEW_TOGGLEMODE : 
            tip = "Enable or disable scaled-to-fit display mode";
            putValue(SELECTED_KEY, figCanvas.isScaleToFitOn());
            break;
         case VIEW_TOGGLEPRINTPREVIEW : 
            tip = "Turn print preview mode on or off";
            putValue(SELECTED_KEY, figCanvas.isPrintPreviewEnabled());
            break;
         case VIEW_RESETZOOM : 
            ena = !figCanvas.isScaleToFitOn();
            if(ena) tip = "Reset zoom and pan transform";
            break;

         default : 
            // the insert-node action types...
            FGNodeType nodeType = getNodeTypeForInsertAction(type);
            ena = (!isMultiSel) && canInsertNode(nodeType);
            if(ena) tip = "Insert " + nodeType.getNiceName().toLowerCase();
            break;
         }
         
         setEnabled(ena);
         if(tip != null && hotKeyMnemonic != null) tip += "  (" + hotKeyMnemonic + ")";
         putValue(SHORT_DESCRIPTION, tip);
      }
      
      /** This action's unique command ID/type. */
      private final FCActionType type;
      /** If a keyboard accelerator is associated with the action, this is the hot key's mnemonic (for tool tips). */
      private final String hotKeyMnemonic;
   }

   /**
    * Helper method that handles the actions that insert a new graphic node or paste the graphic nodes currently in the
    * FypML node clipboard. The location at which the node is inserted depends on the current selected node, and in some
    * cases there will be two alternatives: append the node as a child of the focus node, or insert it as a sibling of
    * the focus node. When only one of the insert locations is permitted, then this method will go ahead and make the
    * insertion. However, if both locations are possible, the method raises a modal dialog to query the user.
    * 
    * <p>If the shared node clipboard currently holds more than one node, then the paste operation will only insert 
    * those nodes that can be children of the currently selected node (rather than inserting some as children and 
    * some as siblings and raising a dialog in every case that's ambiguous!).</p>
    * 
    * <p>If multiple nodes are selected in the current figure, this method takes no action: the insert and paste
    * operations are disabled in this case.</p>
    * 
    * @param insertType For an "insert new node" operation, this is the type of node to be inserted. If null, the node
    * or nodes in the FypML node clipboard will be pasted instead.
    */
   private void insertOrPasteNode(FGNodeType insertType)
   {
      FGraphicModel currFig = getSelectedFigureModel();
      if(currFig == null || currFig.isMultiNodeSelection()) return;
      
      FGraphicNode sel = currFig.getSelectedNode();
      
      // UI lets focus be put on a graph's axis or legend. Switch focus to the graph itself for the insert/paste op.
      if(sel != null && sel.isComponentNode()) sel = sel.getParent();
      if(sel == null) return;
      
      // if it's a paste op and the clipboard contains more than one node, we insert all nodes in the clipboard that
      // can be children of the currently selected node
      boolean isPaste = (insertType == null);
      if(isPaste && FGraphicModel.getClipboardSize() > 1)
      {
         FGraphicModel.pasteFromClipboard(sel, -1);
         return;
      }
      
      // pasting a single node: get its node type from the clipboard. Abort if nothing to paste
      if(isPaste) insertType = FGraphicModel.getClipboardContentType();
      if(insertType == null) return;
      
      // check which insert locations are valid; if neither is valid, abort.
      boolean asChild = sel.canInsert(insertType);
      FGraphicNode parent = sel.getParent();
      boolean asSib = (parent != null) && parent.canInsert(insertType);
      if(!(asChild || asSib)) return;
      
      // if both insert locations are possible, ask user to choose...
      if(asChild && asSib)
      {
         String title = (isPaste ? "Paste" : "Insert") + " graphic";
         String message = 
               (isPaste ? "Paste " : "Insert ")  + insertType.getNiceName() + (isPaste ? " from clipboard" : "") +
               " as a child or sibling of the " + sel.getNodeType().getNiceName() + "?";
         Object[] options = { "Cancel", "As sibling", "As child" };
         int choice = JOptionPane.showOptionDialog(this, message, title,
               JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
               null, options, options[2]);
         
         if(choice == 1) asChild = false;
         else if(choice == 0)
            return;  // user cancelled
      }
      
      // complete the operation
      if(isPaste)
      {
         if(asChild) FGraphicModel.pasteFromClipboard(sel, -1);
         else FGraphicModel.pasteFromClipboard(parent, parent.getIndexOf(sel));
      }
      else
      {
         if(asChild) currFig.insertNode(sel, insertType, -1);
         else currFig.insertNode(parent, insertType, parent.getIndexOf(sel));
      }
   }

   /**
    * Helper method checks the identity of the current focus node to see if it is possible to insert the specified
    * graphic node type as a child or sibling of the focus node. If the focus is currently on an axis or legend -- which
    * are intrinsic components of a graph --, then the focus is assumed to be on the graph itself for the purposes of
    * deciding whether or not the specified node type can be inserted.
    * 
    * @param insertType The type of FypML graphic node to be inserted.
    * @return True iff insertion of that node is currently possible, either as child or sibling of focus node.
    */
   private boolean canInsertNode(FGNodeType insertType)
   {
      FGraphicModel currFig = getSelectedFigureModel();
      FGraphicNode focus = (currFig == null) ? null : currFig.getSelectedNode();
      
      // UI lets focus be put on a graph's axis or legend. Switch focus to the graph itself for the purposes of making
      // this decision
      if(focus != null && focus.isComponentNode()) focus = focus.getParent();
      
      return((focus != null && focus.canInsert(insertType)) || 
                  (focus != null && focus.getParent() != null && focus.getParent().canInsert(insertType)));
   }

   /**
    * Handler for the {@link FCActionType#FILE_SAVE} and {@link FCActionType#FILE_SAVEAS} actions. The selected figure 
    * is saved to its original FypML source file. However, if the figure was never saved before, if the original source 
    * was a Matlab FIG file, or if the "save as" action was chosen, then the chooser appears to get a valid FypML file 
    * pathname from user. No action is taken if the figure has not been modified yet -- unless it's a "save as" action.
    * 
    * <p>For the "save" operation, the original source file is normally overwritten. However, if that file's "last 
    * modified" time has changed since the figure was loaded into the figure composer, then the user is alerted to the
    * situation and given a chance to cancel the file save.</p>
    * @param type The save action type. 
    */
   private void saveCurrentFigure(FCActionType type)
   {
      if(type != FCActionType.FILE_SAVE && type != FCActionType.FILE_SAVEAS) return;
      
      FGraphicModel fgm = getSelectedFigureModel();
      if(type == FCActionType.FILE_SAVE && !fgm.isModified()) return;
      
      FCChooser chooser = FCChooser.getInstance();
      File f = getSelectedFigureFile();
      
      // for the save file op, if the source FYP file exists we'll overwrite it. Before doing so, verify that the source
      // file has not changed since it was opened in FC (check last modified time). If it has been changed -- presumably
      // by an external application --, ask user if s/he really wants to overwrite it.
      Tab tab = openFigures.get(iCurrentFig);
      if(type == FCActionType.FILE_SAVE && f != null && FCFileType.getFileType(f) == FCFileType.FYP &&
            f.lastModified() != tab.srcLastMod)
      {
         int res = JOptionPane.showConfirmDialog(this, 
               "<html>The source file has been changed by another application since the figure was opened.<br/>" +
               "If you save to the same file, those changes will be lost. Overwrite anyway?</html>", 
               "Source file out-of-sync", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
         if(res != JOptionPane.OK_OPTION) return;
      }
      
      if(chooser.saveFigure(this, fgm, f, type == FCActionType.FILE_SAVEAS, false)) 
      {
         fgm.setModified(false);
         setCurrentFile(chooser.getSelectedFile());
      }
   }
   
   /**
    * If the currently selected figure has been modified, discard all changes and revert it to its last saved state. If
    * the figure lacks a source file (it was created from scratch and never saved), then it is replaced by a brand-new,
    * empty figure. Takes no action if the selected figure has not yet been modified.
    */
   private void revertCurrentFigure()
   {
      if(iCurrentFig < 0 || iCurrentFig >= openFigures.size()) return;
      
      Tab tab = openFigures.get(iCurrentFig);
      if(!tab.revert()) return;
      
      // if the figure graphic is already cached in workspace, then just use that
      FGraphicModel fgm = null;
      if(tab.src != null)
      {
         fgm = (FGraphicModel) FCWorkspace.getInstance().getModelFromCache(tab.src, null);
         if(fgm != null) tab.onFigureLoaded(fgm, tab.src.lastModified());
      }
      
      loadSelectedFigure();
      fireStateChanged();

      // if figure graphic wasn't cached AND there is a source file, spawn a task to reload it from that file
      if(tab.src != null && fgm == null)
      {
         FigureLoader loader = new FigureLoader(tab.src);
         loadersInProgress.add(loader);
         loader.execute();
      }
   }
   
   /**
    * Open a brand new figure, duplicate the currently selected figure, or open an existing FypML, or import an
    * existing Matlab figure, converting it to an equivalent (to the extent possible) FypML figure. The figure is
    * figure added to the list of figures currently open, and it becomes the current selected figure. If it is 
    * already open, then the corresponding tab is simply brought to the front.
    * 
    * @param fcaType {@link FCActionType#FILE_NEW}, or {@link FCActionType#FILE_OPEN}
    */
   private void doOpen(FCActionType fcaType)
   {
      FCChooser chooser = FCChooser.getInstance();
      
      // create a new empty, unsaved figure; create a duplicate of the current figure, or open a figure from file
      FGraphicModel modelOpened;
      File fileOpened = null;
      if(fcaType == FCActionType.FILE_NEW)
         modelOpened = new FGraphicModel();
      else if(fcaType == FCActionType.FILE_DUPE)
      {
         modelOpened = FGraphicModel.copy(getSelectedFigureModel());
      }
      else if(fcaType == FCActionType.FILE_OPEN)
      {
         modelOpened = chooser.openFigure(this, getSelectedFigureFile());
         if(modelOpened != null) 
         {
            fileOpened = chooser.getSelectedFile();
            
            // if the file is already open, just switch to the corresponding tab.
            for(int i=0; i<openFigures.size(); i++) 
            {
               Tab tab = openFigures.get(i);
               if(Utilities.filesEqual(tab.src, fileOpened))
               {
                  setSelectedTab(i);
                  return;
               }
            }
         }
      }
      else
         return;
      
      if(modelOpened != null)
      {
         // if we duplicated the current figure, insert duplicate immediately after it in the tab order. Also, mark it
         // as modified.
         if(fcaType == FCActionType.FILE_DUPE)
         {
            openFigures.add(iCurrentFig+1, new Tab(modelOpened, null));
            iCurrentFig++;
            openFigures.get(iCurrentFig).modified = true;
         }
         else
         {
            // find the first available slot containing an unmodified standalone figure that is not associated with a file, 
            // and put the opened figure there. Otherwise, add it to the open figure list. In either case, bring it to
            // the front!
            int replacePos = -1;
            for(int i=0; i<openFigures.size(); i++)
            {
               Tab tab = openFigures.get(i);
               if(tab.src == null && !tab.modified) 
               {
                  replacePos = i;
                  break;
               }
            }
            
            if(replacePos >= 0)
            {
               openFigures.set(replacePos, new Tab(modelOpened, fileOpened));
               iCurrentFig = replacePos;
            }
            else
            {
               openFigures.add(new Tab(modelOpened, fileOpened));
               iCurrentFig = openFigures.size() - 1;
            }
         }
         
         loadSelectedFigure();
         FCWorkspace.getInstance().addToRecentFiles(fileOpened);
         fireStateChanged();
      }
   }

   /**
    * Helper method handles all the various "close figure tab" operations: {@link FCActionType#FILE_CLOSE}, {@link 
    * FCActionType#FILE_CLOSEALL}, etcetera.
    * @param type The type of close action to perform.
    */
   private void doClose(FCActionType type)
   {
      if(type == FCActionType.FILE_CLOSE)
      {
         closeTab(iCurrentFig);
         return;
      }
      
      // remember which tab was selected. It may get removed, or its position may change.
      Tab selectedTab = openFigures.get(iCurrentFig);
      List<Tab> tabsToRemove = new ArrayList<>();
      if(type == FCActionType.FILE_CLOSEUNMOD)
      {
         for(Tab tab : openFigures) if(!tab.modified) tabsToRemove.add(tab);
      }
      else if(type == FCActionType.FILE_CLOSEALL || type == FCActionType.FILE_CLOSEALLBUTCURR ||
            type == FCActionType.FILE_CLOSEVIS || type == FCActionType.FILE_CLOSEHIDDEN || 
            type == FCActionType.FILE_CLOSELEFT || type == FCActionType.FILE_CLOSERIGHT)
      {
         // for the particular type of close action, get the indices of the figure tabs to be closed. We do it this
         // way because, for FILE_CLOSEHIDDEN and FILE_CLOSEALLBUTCURR, there tab index range to check could be broken
         // into two pieces. 
         List<Integer> tabsToClose = new ArrayList<>();
         int first;
         switch(type)
         {
         case FILE_CLOSEALL :
         case FILE_CLOSEALLBUTCURR :
            for(int i=0; i<openFigures.size(); i++) tabsToClose.add(i);
            if(type == FCActionType.FILE_CLOSEALLBUTCURR) tabsToClose.remove(Integer.valueOf(iCurrentFig));
            break;
         case FILE_CLOSEVIS :
            first = tabStrip.getIndexOfFirstVisibleTab();
            for(int i=0; i<tabStrip.getNumVisibleTabs(); i++) tabsToClose.add(first + i);
            break;
         case FILE_CLOSEHIDDEN :
            first = tabStrip.getIndexOfFirstVisibleTab();
            for(int i=0; i<first; i++) tabsToClose.add(i);
            first += tabStrip.getNumVisibleTabs();
            for(int i=first; i<openFigures.size(); i++) tabsToClose.add(i);
            break;
         case FILE_CLOSELEFT:
            for(int i=0; i<iCurrentFig; i++) tabsToClose.add(i);
            break;
         case FILE_CLOSERIGHT:
            for(int i=iCurrentFig+1; i<openFigures.size(); i++) tabsToClose.add(i);
            break;
         default :  // should never get here!
            break;
         }
         
         // go through list of figures and accumulate the figures (and file destinations) to be saved before exit. In
         // any figure has never been saved, we'll have to request a file path from the user.
         FCChooser chooser = FCChooser.getInstance();
         List<FGraphicModel> figsToSave = new ArrayList<>();
         List<File> dstFiles = new ArrayList<>();
         List<Integer> tabIndices = new ArrayList<>();
         String[] options = new String[] {"Cancel", "Save none", "Save all", "No", "Yes"};
         boolean saveRemaining = false;
         boolean dontSaveRemaining = false;
         for(Integer tabIdx : tabsToClose)
         {
            Tab tab = openFigures.get(tabIdx);
            tabsToRemove.add(tab);
            if(dontSaveRemaining || !tab.modified) continue;
            
            if(saveRemaining)
            {
               tabIndices.add(tabIdx);
               figsToSave.add(tab.fig);
               
               // if the figure has never been saved, request a file path from user. If user cancels, then abort exit.
               if(tab.src == null)
               {
                  setSelectedTab(tabIdx);
                  
                  if(!chooser.saveFigure(this, tab.fig, tab.src, false, true)) return;
                  else tab.src = chooser.getSelectedFile();
               }
               dstFiles.add(tab.src);

               continue;
            }
            
            setSelectedTab(tabIdx);
            
            if(figsToSave.size() == 1) { options[1] = "Skip remaining"; options[2] = "Save remaining"; }
            int choice = JOptionPane.showOptionDialog(this,
                  (tab.src != null) ? "Save changes to " + tab.src.getName() + "?" : "Save new figure?",
                  "Save figure before close",
                  JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                  options, options[4]);
            if(choice == JOptionPane.CLOSED_OPTION || choice == 0) return;
            else if(choice == 1)
               dontSaveRemaining = true;
            else if(choice == 2 || choice == 4)
            {
               tabIndices.add(tabIdx);
               figsToSave.add(tab.fig);
               
               // if the figure has never been saved, request a file path from user. If user cancels, then abort exit.
               if(tab.src == null)
               {
                  if(!chooser.saveFigure(this, tab.fig, null, false, true)) return;
                  else tab.src = chooser.getSelectedFile();
               }
               dstFiles.add(tab.src);

               saveRemaining = (choice == 2);
            }
         }
         
         // save all the figures that user wanted saved. If an error occurs, then update figure tabs to reflect the
         // files that were saved, and abort the multi-file close operation
         if(!figsToSave.isEmpty())
         {
            int nSaved = chooser.saveAllFigures(this, figsToSave, dstFiles);
            for(int i=0; i<nSaved; i++)
            {
               Tab tab = openFigures.get(tabIndices.get(i));
               tab.fig.setModified(false);
               tab.modified = false;
            }
            if(nSaved < figsToSave.size()) return;
         }
      }
      
      if(tabsToRemove.isEmpty()) return;
      
      for(Tab tab : tabsToRemove) openFigures.remove(tab);
      
      if(openFigures.isEmpty())
         initWithBlankFigure();
      else
      {
         iCurrentFig = openFigures.indexOf(selectedTab);
         if(iCurrentFig == -1) iCurrentFig = 0;
         loadSelectedFigure();
      }
      fireStateChanged();
   }
   
   /**
    * Helper method services two distinct mechanisms by which the user can choose to open one of the most recently used
    * (MRU) figure files, as obtained from the workspace manager:
    * <ul>
    *    <li>Stand-alone pop-up menu attached to the "Open Recent" button in the tool bar panel. In this case, a new 
    *    pop-up menu is constructed and populated with the pathnames of all MRU files. If the user selects an item in 
    *    the menu, the corresponding figure is opened. Once populated, the pop-up menu is raised adjacent to the "Open 
    *    Recent" button.</li>
    *    <li>Pop-up menu for the "Open Recent" sub-menu under the "File" menu in the figure composer's menu bar. In this
    *    case, the method retrieves the pop-up menu attached to the "Open Recent" sub-menu, clears its old content, then
    *    re-populates it with the pathnames of all MRU files. The pop-up menu is NOT raised, since it's tied into the 
    *    menu bar hierarchy. This method should be called whenever the "File" drop-down menu gets selected.</li>
    * </ul>
    * @param standalone True to configure and raise the stand-alone MRU pop-up menu; false to clear and reload the MRU
    * pop-up menu associated with the "File | Open Recent" menu item.
    */
   private void prepareMRUFileMenu(boolean standalone)
   {
      JPopupMenu popup;
      if(standalone) popup = new JPopupMenu();
      else
      {
         popup = mruMenu.getPopupMenu();
         popup.removeAll();
      }
      
      List<File> mru = FCWorkspace.getInstance().getRecentFiles(false);
      if(mru.isEmpty())
      {
         if(!standalone) mruMenu.setEnabled(false);
         else return;
      }

      for(File f : mru)
      {
         JMenuItem mi = new JMenuItem(f.getName() + " (" + f.getAbsolutePath() + ")");
         mi.setActionCommand("file:" + f.getAbsolutePath());
         mi.addActionListener(popupMenuHandler);
         popup.add(mi);
      }
      
      if(standalone)
      {
         int ofs = openRecentBtn.getInsets().left + openRecentBtn.getIcon().getIconWidth() + 
               openRecentBtn.getIconTextGap();
         popup.show(openRecentBtn, ofs, 0);
      }
   }

   /** 
    * Delegate handles user selection from the pop-up menu associated with the "Open Recent" tool bar button, or the 
    * subordinate menu associated with the "Open Recent" menu item in the "File" menu.
    */
   private final ActionListener popupMenuHandler = e -> {
      String cmd = e.getActionCommand();
      if(cmd.startsWith("file:"))
      {
         String path = cmd.substring(5).trim();
         File f = new File(path);
         if(f.isFile()) openFigure(f);
      }
   };
   

   //
   // TabStripModel -- Support for having multiple figures open at once.
   //
   
   /**
    * Helper class encapsulates information needed to implement the tab strip data model. Each "tab" represents an open 
    * figure and includes: the graphic model of the figure, the source file, and a flag indicating whether it has been 
    * modified yet). In addition, it includes a unique auto-save ID assigned to the figure, once it has been registered
    * to be backed-up by the auto-saver.
    */
   private class Tab implements FGModelListener
   {
      /**
       * Construct a tab representing a figure currently open in the figure composer.
       * @param fig The figure's graphic model. If null, an empty figure is assumed.
       * @param src Pathname of the FypML or Matlab figure file that is the "source" for the model. If null, then 
       * figure is considered "new". If the source is a Matlab figure file (FIG file format), then the model is marked 
       * as "modified". When the user attempts to save the figure, he will be forced to save it as a FypML file. 
       * <i>NOTE: If <i>fig==null</i> but <i>src != null</i>, then it is assumed that the figure model is currently 
       * being loaded on a background thread.
       */
      Tab(FGraphicModel fig, File src)
      {
         this.fig = (fig == null) ? new FGraphicModel() : fig;
         this.src = src;
         if(fig != null && src != null && FCFileType.getFileType(src) == FCFileType.FIG)
            this.fig.setModified(true);
         this.modified = this.fig.isModified();
         this.loading = (fig == null) && (src != null);
         this.syncing = false;
         this.srcLastMod = (src != null) ? src.lastModified() : -1;
         this.autoSaveID = null;
         this.fig.addListener(this);
      }

      /**
       * Whenever the figure on this tab changes, this method checks to see if the figure's modified flag has changed. 
       * If so, the new flag state is remembered and the tab strip on which this tab is displayed is updated 
       * accordingly: An "*" precedes the file names of all modified figures.
       */
      public void modelChanged(FGraphicModel model, FGraphicNode n, Change change)
      {
         if(model.isModified() != modified)
         {
            modified = model.isModified();
            FigComposer.this.fireStateChanged();
         }
      }
      
      /**
       * Invoked by figure loader task (but back on the Swing event dispatch thread) when the figure has been loaded
       * from the source file. If loading was successful, the temporary blank figure is replaced by the just-loaded
       * graphic, and the figure composer is updated accordingly.
       * @param fig The figure graphic model that was just loaded from file.
       * @param lastMod The last-modified time of the source file just prior to loading (FypML files only)
       */
      void onFigureLoaded(FGraphicModel fig, long lastMod)
      {
         if((fig == null) || (src == null) || !(loading || syncing)) return;
         this.fig.removeListener(this);
         this.fig = fig;
         this.fig.addListener(this);
         loading = false;
         syncing = false;
         modified = this.fig.isModified();
         this.srcLastMod = lastMod;
         FigComposer.this.fireStateChanged();
      }

      /**
       * Call this method to discard the figure graphic model in this tab prior to reloading the figure from its source
       * file. The figure is replaced by a brand-new, empty figure model. No action is taken if the figure has not yet
       * been modified, or if it is currently being loaded from a source file.
       * <p>When the figure lacks a source file, calling this method has the effect of reverting the figure to the empty
       * state. If it has a source file, it is assumed that the caller will initiate a task to reload the figure from
       * that file; the load operation is NOT initiated here.</p>
       * 
       * @return False if no action taken for whatever reason. Else returns true.
       */
      boolean revert()
      {
         if(loading || syncing || !fig.isModified()) return(false);
         
         fig.removeListener(this);
         fig = new FGraphicModel();
         fig.addListener(this);
         modified = false;
         srcLastMod = -1L;
         if(src != null) loading = true;
         return(true);
      }
      
      /** Graphic model of the open figure. */
      FGraphicModel fig;
      
      /** Flag indicating whether figure has been modified (so we can put an "*" on the figure's tab label). */
      boolean modified;
      
      /** 
       * Flag set if figure model is currently being loaded from source file on a background thread. Model will be 
       * initialized as an empty figure; it will be replaced (on the Swing event dispatch thread) by the actual model
       * when the background task finishes. During load, tab title will display "LOADING..." instead of file name.
       */
      boolean loading;
      
      /** 
       * The original source file path. It could be a FypML file or a Matlab FIG file. However, in the latter case,
       * the figure model CANNOT be saved to that path. The figure composer can only save figures as FypML documents!
       */
      File src;
      
      /** 
       * Original source file's last modified time when it was last opened or saved by this application. We keep track
       * of this so we can detect when a source file is changed by an external application.
       */
      long srcLastMod;
      
      /** 
       * Flag set if figure model is currently being RELOADED from source file (in the background) because that file
       * was changed externally. Such a reload will happen only if the figure has not yet been modified when the "out
       * of sync" condition was detected.
       */
      boolean syncing;
      
      /** ID assigned to the figure by the auto-saver. */
      Integer autoSaveID;
   }
   
   /** Clear the list of open figures and reinitialize with a single empty figure. */
   private void initWithBlankFigure()
   {
      openFigures.clear();
      openFigures.add(new Tab(new FGraphicModel(), null));
      iCurrentFig = 0;
      loadSelectedFigure();
   }
   
   /** 
    * Get the figure model that is currently selected for display and editing in the figure composer. By design, there
    * is ALWAYS at least one figure open for editing.
    * 
    * @return The currently selected figure model.
    */
   private FGraphicModel getSelectedFigureModel() 
   {
      return(openFigures.get(iCurrentFig).fig);
   }
   
   /** The selected figure can be duplicated unless it is a brand-new empty figure that has never been saved. */
   private boolean canDuplicateSelectedFigure()
   {
      Tab tab = openFigures.get(iCurrentFig);
      return(tab.src != null || tab.modified);
   }
   
   /** 
    * Get the abstract pathname of the FypML figure definition file for the figure that is currently selected for 
    * display and editing in the figure composer.
    * @return Path of currently selected figure's definition file, or null if figure has never been saved.
    */
   File getSelectedFigureFile() 
   { 
      return(openFigures.get(iCurrentFig).src); 
   }
   
   /** Reloads the figure currently selected for display and editing within the figure composer. */
   private void loadSelectedFigure()
   {
      FGraphicModel old = (FGraphicModel) figCanvas.getModel();
      if(old != null) old.removeListener(this);
      
      FGraphicModel selected = getSelectedFigureModel();
      selected.addListener(this);
      figCanvas.setModel(selected);
      if(selected.getSelectedNode() == null)
         selected.setSelectedNode(selected.getRoot());
      else
         mouseLayer.onModelChanged(Change.RELOAD);
      
      figureTreeView.loadFigure(selected);
      
      // if the selected figure has been saved to file, select the corresponding folder in the workspace browser
      File src = getSelectedFigureFile();
      if(src != null && src.isFile()) figBrowser.selectFigureFolder(src.getParentFile());
   }
   
   /** List of all tab strip model change listeners registered with the figure composer. */
   private final EventListenerList tabListeners = new EventListenerList();
   
   @Override public void addChangeListener(ChangeListener l) { if(l != null) tabListeners.add(ChangeListener.class, l); }
   @Override public void removeChangeListener(ChangeListener l) 
   { 
      if(l != null) tabListeners.remove(ChangeListener.class, l); 
   }
   
   /** 
    * Notifies any tab strip model change listeners that the list of "open figure" tabs has changed in some way. This 
    * must be invoked whenever any relevant change occurs (figure opened, closed or saved; change in unmodified figure).
    */
   private void fireStateChanged()
   {
      ChangeEvent e = new ChangeEvent(this);
      Object[] listeners = tabListeners.getListenerList();
      for (int i = listeners.length-2; i>=0; i-=2) if (listeners[i] == ChangeListener.class)
         ((ChangeListener)listeners[i+1]).stateChanged(e);
   }
   
   @Override public boolean isTabClosable(int tabPos) { return(true); }
   @Override public String getCloseTabToolTip(int tabPos) { return(null); }
   
   @Override public void closeTab(int tabPos)
   {
      if(tabPos < 0 || tabPos >= getNumTabs()) return;
      
      Tab tab = openFigures.get(tabPos);
      boolean closeOK = !(tab.modified || tab.fig.isModified());
      if(!closeOK)
      {
         int result = JOptionPane.showConfirmDialog(this, "Save changes before closing?", "Save figure", 
               JOptionPane.YES_NO_CANCEL_OPTION );
         if(result == JOptionPane.NO_OPTION) closeOK = true;
         else if(result == JOptionPane.YES_OPTION)
         {
            closeOK = FCChooser.getInstance().saveFigure(this, tab.fig, tab.src, false, false);
            if(closeOK) tab.fig.setModified(false);
         }
      }
      if(!closeOK) return;
      
      closeTabNoConfirm(tabPos);
   }

   /** 
    * Helper method removes the specified tab from the composer's tab strip regardless the modified state of the
    * corresponding figure model, and adjusts the tab strip control state accordingly.
    * @param tabPos Index position of tab to be closed. No action taken if this is invalid.
    */
   private void closeTabNoConfirm(int tabPos)
   {
      if(tabPos < 0 || tabPos >= getNumTabs()) return;
      openFigures.remove(tabPos);
      if(openFigures.isEmpty())
         initWithBlankFigure();
      else if(iCurrentFig >= tabPos)
      {
         boolean removedSelectedFig = (tabPos == iCurrentFig);
         --iCurrentFig;
         if(iCurrentFig < 0) iCurrentFig = 0;
         if(removedSelectedFig) loadSelectedFigure();
      }
      fireStateChanged();
   }
   
   @Override public int getNumTabs() { return(openFigures.size()); }
   @Override public int getSelectedTab() { return(iCurrentFig); }
   @Override public Icon getTabIcon(int tabPos)
   { 
      Icon icon = null;
      if(tabPos >= 0 && tabPos < getNumTabs())
      {
         Tab tab = openFigures.get(tabPos);
         if(tab.src == null || FCFileType.getFileType(tab.src) == FCFileType.FYP) icon = FCIcons.V4_FIGURE_16;
         else icon = FCIcons.V4_MATFIG_16;
      }
      return(icon); 
   }
   
   @Override public String getTabLabel(int tabPos) 
   { 
      if(tabPos < 0 || tabPos >= getNumTabs()) return(null);
      
      Tab tab = openFigures.get(tabPos);
      if(tab.loading || tab.syncing) return(tab.syncing ? "RELOADING..." : "LOADING...");
      String label = tab.modified ? "*" : "";
      File f = tab.src;
      label += (f == null) ? "<New figure>" : f.getName();
      return(label);
   }

   @Override public String getTabToolTip(int tabPos)
   {
      if(tabPos < 0 || tabPos >= getNumTabs()) return(null);
      Tab tab = openFigures.get(tabPos);
      return((tab.src != null) ? tab.src.getAbsolutePath() : null);
   }

   @Override public void setSelectedTab(int tabPos)
   {
      if(tabPos < 0 || tabPos >= openFigures.size() || tabPos == iCurrentFig) return;
      iCurrentFig = tabPos;
      loadSelectedFigure();
      fireStateChanged();
      refreshActions();
   }
   
   @Override public boolean supportsTabRepositioning() { return(true); }
   @Override public boolean moveTab(int fromPos, int toPos) 
   { 
      if(fromPos == toPos || fromPos < 0 || fromPos >= getNumTabs() || toPos < 0 || toPos > getNumTabs())
         return(false); 
      
      Tab selectedTab = openFigures.get(iCurrentFig);
      int nTabs = openFigures.size();
      Tab tab = openFigures.remove(fromPos);
      if(toPos == nTabs) openFigures.add(tab);
      else openFigures.add(toPos, tab);
      
      iCurrentFig = openFigures.indexOf(selectedTab);
      
      fireStateChanged();
      return(true);
   }

   /**
    * An enumeration of the different mouse cursor modes associated with the {@link InteractionLayer}.
    * 
    * @author sruffner
    */
   private enum CursorMode
   {
      /** Select mode. This is the default mode. */
      SELECT(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)),
      /** Move mode, enabled only when mouse is over a movable node that has the current focus. */
      MOVE(FCIcons.V4_MOVECURSOR),
      /** Crop mode, enabled only when mouse is over the rendered image of an image element with the current focus. */
      CROP(FCIcons.V4_CROPCURSOR),
      /** Rotate mode, enabled only when mouse is near the origin of a 3D graph element with the current focus. */
      ROTATE(FCIcons.V4_ROT3DCURSOR),
      /** Zoom mode, enabled only when space key is depressed and mouse is inside the interaction layer. */
      ZOOM(FCIcons.V4_ZOOMCURSOR),
      /** Pan mode, enabled only when right mouse button is depressed and the current graphic can be panned. */
      PAN(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)),
      /** Resize mode: Dragging north edge of focus rectangle. */
      N_RESIZE(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)),
      /** Resize mode: Dragging south edge of focus rectangle. */
      S_RESIZE(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)),
      /** Resize mode: Dragging east edge of focus rectangle. */
      E_RESIZE(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)),
      /** Resize mode: Dragging west edge of focus rectangle. */
      W_RESIZE(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)),
      /** Resize mode: Dragging northeast corner of focus rectangle. */
      NE_RESIZE(Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR)),
      /** Resize mode: Dragging northwest corner of focus rectangle. */
      NW_RESIZE(Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR)),
      /** Resize mode: Dragging southeast corner of focus rectangle. */
      SE_RESIZE(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)),
      /** Resize mode: Dragging southwest corner of focus rectangle. */
      SW_RESIZE(Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR)),
      /** Resize mode: Non-directional -- used when resized node is rotated WRT figure. */
      ANY_RESIZE(FCIcons.V4_RESIZECURSOR);
      
      /**
       * Get the resize cursor mode corresponding to the specified resize anchor position.
       * @param ra The resize anchor position: one of the corners or edges on the focus node's bounding rectangle.
       * @param isRotated True if focus node is rotated WRT the figure. In this case, as long as the resize anchor
       * position is non-null, the {@link #ANY_RESIZE} mode is returned, which uses a non-directional resize cursor.
       * @return The appropriate resize cursor mode -- or null if the resize anchor position is null.
       */
      static CursorMode resizeModeFromAnchorPosition(ResizeAnchor ra, boolean isRotated)
      {
         if(ra == null) return(null);
         CursorMode cm = ANY_RESIZE;
         if(!isRotated) switch(ra)
         {
         case NORTH: cm = N_RESIZE; break;
         case SOUTH: cm = S_RESIZE; break;
         case WEST:  cm = W_RESIZE; break;
         case EAST:  cm = E_RESIZE; break;
         case NORTHEAST: cm = NE_RESIZE; break;
         case NORTHWEST: cm = NW_RESIZE; break;
         case SOUTHEAST: cm = SE_RESIZE; break;
         case SOUTHWEST: cm = SW_RESIZE; break;
         default: break;
         }
         return(cm);
      }
      
      CursorMode(Cursor c) { cursor = c; }
      boolean isResizeMode() 
      { 
         return(this!=SELECT && this!=CROP && this!=ROTATE && this!=MOVE && this!=ZOOM && this!=PAN); 
      }
      
      private final Cursor cursor;
   }

   /**
    * Helper class implements a transparent layer that renders node selection and other highlights on top of the figure 
    * canvas and animates a number of mouse-related interactive gestures:
    * <ul>
    * <li>When the mouse enters the layer -- which lies on top of and covers the same region as the figure canvas --,
    * the cursor changes to a crosshair. This is the "select" cursor mode. When the mouse leaves the layer, the default
    * cursor shape is restored.</li>
    * <li>As the mouse moves around over the figure canvas in "select" mode, the graphic node underneath the current 
    * mouse position (the deepest node in the Z-order) is highlighted with a translucent golden outline. This feature 
    * helps the user select a particular node among several neighboring and partially overlapping graphic objects, such 
    * as data presentation nodes rendered in a graph.</li>
    * <li>In the "select" cursor mode, if the user clicks the primary mouse button somewhere on the figure canvas, the 
    * graphic node under the mouse becomes the <i><b>currently selected node</b></i> for editing purposes. The selected 
    * node is covered by a translucent steel blue shape -- the selection highlight.</li>
    * <li>If the user "double-clicks" on the <i>current singly-selected node</i> AND that node is a <i>FypML</i> label
    * node, then a pop-up text editor is raised so that user can change the label text "in place". The pop-up is
    * extinguised by pressing the <b>Enter</b> key (confirming any change in the label text) or by clicking anywhere
    * outside the pop-up (no change made).</li>
    * <li>As of v4.5.0, more than one node may be selected at once to perform certain operations (delete or copy all
    * selected nodes, apply style set to all nodes, move nodes by dragging, align nodes). All nodes in the figure's
    * <i>current selection</i> are covered by a selection highlight. Furthermore, if the Ctrl key (Command key in Mac
    * OSX) is down when the user "clicks" on a node, that node is added to the figure's current selection if it was
    * unselected, or de-selected if it was already selected (unless it is the only node selected).</li>
    * <li>When the mouse moves inside a selection highlight AND the corresponding node can be moved by dragging, the 
    * cursor changes shape to indicate the layer is in "move cursor" mode. If the user then holds down the primary mouse 
    * button and starts dragging the mouse, a "move-by-drag" gesture is initiated. As the user drags the mouse, the
    * node's selection shape, outlined in a dotted black line, follows it. Upon release, that node is relocated and the 
    * figure is re-rendered accordingly. If multiple nodes are selected, all nodes (that can be moved) are moved by the
    * same amount.</li>
    * <li>If the singly-selected node can be resized interactively, the selection highlight includes a dotted outline
    * shape (typ. rectangular) -- its "resize outline". When the mouse is close enough to an edge or corner of this 
    * outline, the cursor changes shape to indicate the layer is in "resize cursor" mode. If the user then holds down 
    * the primary mouse button and starts dragging, a "resize-by-drag" gesture is initiated. As the user drags the 
    * mouse, the resize outline shrinks or expands, and a textual cue indicates how the node's location and dimensions
    * would change. Upon release, the node is resized and the figure is re-rendered accordingly. This interaction is
    * disabled whenever multiple nodes are selected.</li>
    * <li>If the singly-selected node is an image element and the mouse enters the region covered by the rendered image 
    * (an image element can have a border and margin), the "move cursor" is replaced by an "image crop" cursor. The user
    * can then press the primary mouse button and drag the mouse to re-define the current crop rectangle for that image 
    * element. While dragging, a thin black rectangle highlights the current crop rectangle; upon releasing the mouse, 
    * the image is updated accordingly. This interaction is disabled whenever multiple nodes are selected.</li>
    * <li>If the singly-selected node is a 3D graph and the mouse enters the region near the graph's origin, the "move"
    * cursor is replaced by a "rotate" cursor. The user can press the primary mouse button and drag the mouse to change 
    * the 3D graph's current rotation and elevation angles. The "rotate outline" (outlines the graph's 3D box) is 
    * updated to reflect how the graph would be oriented if the mouse were released at its current location; upon
    * releasing the mouse, the 3D graph is updated accordingly.</li>
    * <li>When the mouse is inside the layer, the keyboard focus is NOT on a text component, and the user holds down the
    * space key, "zoom" cursor mode is enabled: the cursor becomes a small magnifying glass. In this mode, holding the 
    * primary mouse button down initiates a "zoom by mouse drag" operation. As the user drags the mouse over the canvas,
    * a translucent gray rectangle roughly follows the mouse position -- while maintaining the aspect ratio of the 
    * displayed figure. Upon release, the figure canvas is scaled and panned so that the area within the zoom rectangle 
    * is magnified and centered in the canvas (to the extent possible).</li>
    * <li>Again in "zoom" mode, clicking the primary button scales the figure by a factor of 2 and recenters it over the
    * mouse-click location (if possible); when a non-primary button (there could be 3) is clicked, the canvas zooms
    * out by a factor of 2.</li>
    * <li>In any mode other than "zoom" mode, holding down and dragging the secondary mouse button will pan the graphic 
    * with the mouse (again, to the extent possible). The cursor changes to the system "hand" cursor during the panning 
    * gesture. Of course, if the canvas cannot be panned (because the entire graphic is smaller than the visible canvas 
    * size), the "hand" cursor will not appear and dragging will have no effect.</li>
    * <li>In "select" cursor mode, holding down and dragging the primary mouse button will initiate a "drag-to-select"
    * gesture. A translucent blue outline indicates the rectangular region selected by the gesture. Upon release of the
    * mouse, all graphic nodes in the figure that intersect that region are selected, replacing the figure's current
    * selection. If the Ctrl key (Command key in OSX) is depressed when the mouse is released, those nodes are ADDED to
    * the current selection.</li>
    * </ul>
    * 
    * @author sruffner
    */
   private class InteractionLayer extends JPanel implements MouseListener, MouseMotionListener
   {
      /** Construct the mouse interaction layer. */
      InteractionLayer()
      {
         setOpaque(false);
         
         strPainter = new StringPainter();
         strPainter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
         strPainter.setStyle(BasicPainterStyle.createBasicPainterStyle(
               BasicPainterStyle.getFontForPainter("Arial", GenericFont.SANSERIF, FontStyle.BOLD, 14), 
               1, null, Color.BLACK, Color.BLACK));
      }
      
      void onModelChanged(Change change)
      {
         FGraphicModel fig = getSelectedFigureModel();
         if(fig == null) selectedNodes.clear();
         else fig.getSelectedNodes(selectedNodes);
         if(selectedNodes.isEmpty())
         {
            showSelectionOverlay(false);
            return;
         }
         
         showSelectionOverlay(true);
         if(selectionShapes.size() == 1 && 
               (change == Change.SELECTION || change==Change.RELOAD || change==Change.INSERT_OR_REMOVE))
         {
            // whenever a SINGLE node is selected and the entire figure is not visible within the confines of the figure
            // canvas, the selected node may not be visible because it lies outside that canvas. Here we verify that the
            // canvas bounds contains at least the center point of the node's bounding rectangle. If it does not, we 
            // attempt to pan the graphic so that center point is centered on the canvas. 
            Rectangle rCanvas = getBounds();
            Shape selShape = selectionShapes.get(0);
            Rectangle rShape = selShape == null ?  null : selShape.getBounds();
            if(rShape != null && !rCanvas.contains(rShape.getCenterX(), rShape.getCenterY()))
            {
               figCanvas.panBy(rShape.getCenterX()-rCanvas.getCenterX(), rShape.getCenterY()-rCanvas.getCenterY());
               updateSelectionOverlay();
            }
         }
      }

      /**
       * Is the mouse cursor currently inside the interaction layer? If so, then it is also over the figure canvas, 
       * since the interaction layer is the same size as and lies directly over the canvas.
       * @return True if mouse is inside the interaction layer.
       */
      boolean isInside() { return(inside); }

      /**
       * Enable or disable zoom mode in the interaction layer. Zoom mode is intended to override the current cursor
       * mode -- so long as the user is not in the middle of a drag gesture. In zoom mode, the cursor becomes an hour
       * glass and three interactions are possible:
       * <ul>
       *    <li>Left click to zoom in on the figure by a factor of 2 (and pan to re-center on mouse, if possible).</li>
       *    <li>Right click to zoom out on the figure by a factor of 2 (and pan to re-center on mouse).</li>
       *    <li>Depress left mouse button and drag to zoom in on a rectangular region within figure.</li>
       * </ul>
       * <p>Zoom mode is intended to be activated by holding down the space key. No action is taken if mouse is not 
       * inside the layer.</p>
       * @param ena True to enable and false to disable the interaction layer's zoom mode.
       */
      void enableZoomMode(boolean ena)
      {
         // ignore if mouse is outside layer
         if(!inside) return;
         
         // ignore if we're in the middle of a drag gesture and we're not currently in zoom mode
         if(isDragging && mode != CursorMode.ZOOM) return;

         boolean isEnabled = (mode == CursorMode.ZOOM);
         if(isEnabled != ena)
         {
            mode = ena ? CursorMode.ZOOM : CursorMode.SELECT;
            setCursor(mode.cursor);
            
            // if we turned off zoom mode in the middle of a zoom-by-drag gesture, cancel the gesture.
            if((!ena) && isDragging) stopDragging(true, null);
            
            // if we just enabled zoom mode, turn off the node highlight (it stays off until we exit zoom mode)
            if(mode == CursorMode.ZOOM) updateHighlightNodeUnder(null); 
         }
      }
      
      /**
       * Update the steel blue translucent overlay covering each node in the figure's current selection. This must be
       * whenever the current selection changes, or whenever any change in the graphic model could affect the location
       * or size of any node that is currently selected.
       */
      void updateSelectionOverlay()
      {
         // accumulate the dirty areas covering the old selection. Assume resize outline and image crop shape, if
         // applicable, are covered.
         dirtyRect.setFrame(0,0,0,0);         
         for(Shape selShape : selectionShapes) if(selShape != null)
         {
            if(dirtyRect.isEmpty()) dirtyRect.setFrame(selShape.getBounds2D());
            else Rectangle.union(selShape.getBounds2D(), dirtyRect, dirtyRect);
         }
         
         // get the shape(s) covering the updated selection. If a single image node is selected, get the image crop
         // shape. If a single resizable node is selected, get its resize outline.
         selectionShapes.clear();
         resizeOutline = null;
         if(!selectedNodes.isEmpty())
         {
            // number of selection shapes must ALWAYS match the number of selected nodes
            for(int i=0; i<selectedNodes.size(); i++) selectionShapes.add(null);
            
            Graphics2D g2 = (Graphics2D) figCanvas.getGraphics();
            if(g2 != null)
            {
               try 
               { 
                  for(int i=0; i<selectedNodes.size(); i++) 
                     selectionShapes.set(i, figCanvas.logicalToDevice(selectedNodes.get(i).getFocusShape(g2)));
               }
               finally { g2.dispose(); }
            }
            
            // if one node selected, get resize outline, image crop shape, and 3D graph "orient-by-drag" trigger zone,
            // as applicable.
            if(selectedNodes.size() == 1)
            {
               FGraphicNode n = selectedNodes.get(0);
               FGNodeType nt = n.getNodeType();
               currImageCropShape = (nt != FGNodeType.IMAGE) ? null : 
                  figCanvas.logicalToDevice(((ImageNode) n).getTightImageBoundsInFigure());
               curr3DGraphOrientZone = (nt != FGNodeType.GRAPH3D) ? null :
                  figCanvas.logicalToDevice(((Graph3DNode) n).getReorientByDragHotSpot());
               if(n.canResize()) resizeOutline = figCanvas.logicalToDevice(n.getResizeShape());
            }
         }
         else
         {
            currImageCropShape = null;
            curr3DGraphOrientZone = null;
         }

         // now accumulate the dirty areas for the new selection. Assume new resize outline & crop image shape will be
         // covered by the selection shape for the corresponding node.
         for(Shape selShape : selectionShapes) if(selShape != null)
         {
            if(dirtyRect.isEmpty()) dirtyRect.setFrame(selShape.getBounds2D());
            else Rectangle.union(selShape.getBounds2D(), dirtyRect, dirtyRect);
         }

         // if there's any painting to be done, do it immediately.
         if(!dirtyRect.isEmpty())
         {
            dirtyRect.grow(5,5);
            paintImmediately(dirtyRect);
         }
      }
      
      /**
       * Show/hide the blue translucent highlight that covers all currently selected nodes in the figure. Typically, the
       * selection highlight is enabled, but it can be hidden to allow an unobstructed view of the current figure.
       * @param show True to show the selection highlight; false to hide it.
       */
      void showSelectionOverlay(boolean show)
      {
         showSelection = show;
         updateSelectionOverlay();
      }
      
      /**
       * Turn on, turn off, or update the X,Y position cue displayed over the first node in the current selection. The 
       * cue string has the form "X=0.00in, Y=0.00in", but only if the selected node is a movable node. If the node
       * cannot be moved interactively, no cue is shown. <i>The cue is similar to that shown when a movable node is 
       * dragged interactively by the mouse. This method is used when the user is "nudging" the position of the current 
       * selection via the Alt+arrow or Alt+Ctrl+arrow keys.</i>
       * @param show True to turn on the position cue if the selected node is movable; false to turn off the position
       * cue regardless.
       */
      void updatePositionCue(boolean show)
      {
         dirtyRect.setFrame(0,0,0,0);
         
         FGraphicNode fgn = selectedNodes.isEmpty() ? null : selectedNodes.get(0);
         boolean showCue = show && (fgn != null) && fgn.canMove();
         if(showCue)
         {
            if(!cueBounds.isEmpty()) dirtyRect.setFrame(cueBounds);
            cue = fgn.getMoveCue(0, 0);   // the cue will reflect the node's current (X,Y) position!
            if(cue != null)
            {
               // center the cue over the node
               Rectangle2D r = selectionShapes.get(0).getBounds2D();
               cueBounds.setFrame(r.getCenterX()-300, r.getCenterY()-26, 600, 100);
               Rectangle.union(cueBounds, dirtyRect, dirtyRect);
            }
         }
         else if(cue != null)
         {
            if(!cueBounds.isEmpty()) dirtyRect.setFrame(cueBounds);
            cue = null;
            cueBounds.setFrame(0,0,0,0);
         }
         
         if(!dirtyRect.isEmpty())
         {
            dirtyRect.grow(5, 5);
            paintImmediately(dirtyRect);
         }
      }
      
      /**
       * Whenever the mouse enters the figure canvas, the cursor shape is updated, as is the cursor position readout on 
       * the figure canvas rulers.
       */
      @Override public void mouseEntered(MouseEvent e) 
      {
         inside = true;
         mode = CursorMode.SELECT;
         setCursor(mode.cursor);
         updateCursorPositionOnRulers(true, e.getX(), e.getY());
      }
      
      /**
       * Whenever the mouse exits the figure canvas: any drag gesture in progress is cancelled, and the current 
       * node-under-mouse highlight is turned off. The cursor is also restored to the default system cursor.
       */
      @Override public void mouseExited(MouseEvent e) 
      { 
         inside = false;
         stopDragging(true, e);
         updateHighlightNodeUnder(null);
         mode = CursorMode.SELECT;
         setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
         updateCursorPositionOnRulers(false, e.getX(), e.getY());
      }

      /**
       * Response to a mouse press depends on the current cursor mode and which mouse button is pressed:
       * <ul>
       * <li>In zoom cursor mode, holding down the primary mouse button enables the "zoom-in" gesture, but there's no  
       * visible change until the user actually starts dragging the mouse.</li>
       * <li>In move cursor mode, holding down the primary mouse button enables the "move-by-drag" gesture. In this 
       * case, a dotted black outline appears around the rendered bounds of <i>each movable node in the current
       * selection</i>.</li>
       * <li>In a resize cursor mode, depressing the primary mouse button enables the "resize-by-drag" gesture, and the
       * initial resize outline is drawn.</li>
       * <li>In crop cursor mode, holding down the primary mouse button enables the "crop-by-drag" gesture. There's no
       * visible change until the user drags the mouse far enough away from the original mouse-down location.</li>
       * <li>In 3D rotate cursor mode, holding down the primary mouse button enables the "orient-by-drag" gesture, and
       * the initial "orient-by-drag" shape is drawn, reflecting the current orientation of the 3D braph.</li>
       * <li>In any cursor mode other than zoom mode, depressing the secondary mouse button enables a "pan-by-drag" 
       * gesture if the current canvas contents can indeed be panned; the cursor changes to a "hand", but no other 
       * visible change occurs until the user starts dragging the mouse.</li>
       * <li>In select cursor mode, depressing the primary mouse button enables the "drag-to-select" gesture. Again, 
       * there's no visible change until the user drags mouse away from the original mouse-down location.</li>
       * </ul>
       */
      @Override public void mousePressed(MouseEvent e)
      {
         Point p = e.getPoint();
         boolean isBtn1 = (e.getButton() == MouseEvent.BUTTON1);
         boolean initDrag = false;
         
         // whenever the user mouses down over the interaction layer, the keyboard focus should be returned to the
         // figure node tree (the interaction layer and figure canvas never hold the focus) so that future keyboard
         // shortcuts will apply to the current focus node
         figureTreeView.putFocusOnNodeTree();
         refreshActions();
         
         // in zoom mode, enable zoom in/out on simple click of primary or non-primary mouse button
         if(mode == CursorMode.ZOOM) enaZoomOnClick = true;
         
         // if the user mouses down on any selected node when the selection highlight is hidden, go ahead and turn the
         // highlight back on (except in zoom mode)
         if(mode != CursorMode.ZOOM && isBtn1 && !showSelection)
         {
            FGraphicModel fig = getSelectedFigureModel();
            if(fig != null)
            {
               figCanvas.deviceToLogical(p.x, p.y, pLog);
               FGraphicNode n = fig.findSmallestNodeUnder(pLog);
               if(selectedNodes.contains(n)) showSelectionOverlay(true);
            }
         }

         dirtyRect.setFrame(0,0,0,0);
         
         // set these now since we use them in initialization code for certain drag gestures.
         dragOrigin = p;
         lastDragPt = new Point(p);
         isDragging = true;
         dragNode = null;
         
         if(!isBtn1)
         {
            // depressing non-primary mouse button in any mode other than zoom switches to pan mode IF the current 
            // canvas graphic can be panned in its current state. The cursor is updated accordingly.
            if(mode != CursorMode.ZOOM && figCanvas.canPan())
            {
               initDrag = true;
               mode = CursorMode.PAN;
               setCursor(mode.cursor);
            }
         }
         else if(mode == CursorMode.ZOOM)
         {
            initDrag = true;
            graphicAR = figCanvas.getAspectRatio();
            dragShape = new Rectangle(0,0,0,0);
         }
         else if(mode == CursorMode.SELECT)
         {
            initDrag = true;
            dragShape = new Rectangle(0,0,0,0);
         }
         else if(mode == CursorMode.CROP)
         {
            dragNode = (selectedNodes.size() == 1) ? selectedNodes.get(0) : null;
            initDrag = dragNode != null && dragNode.getNodeType() == FGNodeType.IMAGE && 
                  currImageCropShape != null && currImageCropShape.contains(p);
            if(initDrag)
            {
               // the dynamic cropping shape is defined by the drag origin and the current drag location. When the
               // drag is initiated, it is empty.
               dragShape = null;
               
               // hide the cue that displayed the mouse position in image pixels. This will be replaced by a 
               // similar cue that shows the crop rectangle's TL corner and size -- but we don't display the cue until
               // the dynamic crop shape is big enough.
               if(dirtyRect.isEmpty()) dirtyRect.setFrame(cueBounds);
               else Rectangle.union(cueBounds, dirtyRect, dirtyRect);
               cueBounds.setFrame(0,0,0,0);
               cue = null;
            }
         }
         else if(mode == CursorMode.ROTATE)
         {
            dragNode = (selectedNodes.size() == 1) ? selectedNodes.get(0) : null;
            initDrag = dragNode != null && dragNode.getNodeType() == FGNodeType.GRAPH3D && 
                  curr3DGraphOrientZone != null && curr3DGraphOrientZone.contains(p);
            if(initDrag)
            {
               // get initial orient-by-drag shape
               figCanvas.deviceToLogical(p.x, p.y, pLog);
               pLog2.setLocation(pLog.getX(), pLog.getY());
               
               dragShape = figCanvas.logicalToDevice(
                     ((Graph3DNode) dragNode).getReorientByDragShape(pLog, pLog2, null));
               if(dragShape == null) initDrag = false;
               else
               {
                   if(dirtyRect.isEmpty()) dirtyRect.setFrame(dragShape.getBounds2D());
                   else Rectangle.union(dragShape.getBounds2D(), dirtyRect, dirtyRect);
                   
                   // will descriptive cue once user starts dragging mouse
                   cue = null;
                   cueBounds.setFrame(0,0,0,0);
               }
            }
         }
         else if(mode == CursorMode.MOVE)
         {
            // the drag shape will consist of the selection shapes for all movable nodes in the current selection. Also 
            // verify that one of the selected movable nodes contains the mouse-down location.
            dragShape = null;
            int nMoved = 0;
            for(int i=0; i<selectedNodes.size(); i++) if(selectedNodes.get(i).canMove())
            {
               Shape selShape = selectionShapes.get(i);
               if(selShape != null)
               {
                  if(selShape.contains(p))
                  {
                     dragNode = selectedNodes.get(i);
                     initDrag = true;
                  }
                  if(dragShape == null) dragShape = new GeneralPath(selShape);
                  else ((GeneralPath) dragShape).append(selShape, false);
               }
               ++nMoved;
            }
            
            if(initDrag)
            {
               if(dirtyRect.isEmpty()) dirtyRect.setFrame(dragShape.getBounds2D());
               else Rectangle.union(dragShape.getBounds2D(), dirtyRect, dirtyRect);
               
               // will display move cue once user starts dragging mouse
               cue = null;
               cueBounds.setFrame(0,0,0,0);
               
               // if multiple movable nodes are selected, nullify dragged node to indicate this
               if(nMoved > 1) dragNode = null;
               
               initialMoveDragShape = dragShape;
            }
         }
         else if(mode.isResizeMode())
         {
            dragNode = (selectedNodes.size() == 1) ? selectedNodes.get(0) : null;
            initDrag = dragNode != null && dragNode.canResize() && selectionShapes.get(0) != null && anchor != null;
            if(initDrag)
            {
               // get initial resize drag shape
               figCanvas.deviceToLogical(p.x, p.y, pLog);
               pLog2.setLocation(pLog.getX(), pLog.getY());
               
               dragShape = figCanvas.logicalToDevice(dragNode.getDragResizeShape(anchor, pLog, pLog2, cueBuffer));
               if(dragShape == null) initDrag = false;
               else
               {
                   if(dirtyRect.isEmpty()) dirtyRect.setFrame(dragShape.getBounds2D());
                   else Rectangle.union(dragShape.getBounds2D(), dirtyRect, dirtyRect);
                   
                   // will display resize cue once user starts dragging mouse
                   cue = null;
                   cueBounds.setFrame(0,0,0,0);
               }
            }
         }
         
         if(initDrag)
         {
            // always turn off highlight of node under mouse when drag gesture is initiated
            if(highlightedNodeShape != null)
            {
               if(dirtyRect.isEmpty()) dirtyRect.setFrame(highlightedNodeShape.getBounds2D());
               else Rectangle.union(highlightedNodeShape.getBounds2D(), dirtyRect, dirtyRect);
            }
            currHighlightedNode = null;
            highlightedNodeShape = null;
         }
         else
         {
            isDragging = false;
            dragOrigin = null;
            lastDragPt = null;
            dragShape = null;
            initialMoveDragShape = null;
            dragNode = null;
         }
         
         if(!dirtyRect.isEmpty())
         {
            dirtyRect.grow(5, 5);
            paintImmediately(dirtyRect);
         }
      }
      
      /**
       * Complete one of the mouse drag-mediated operations: zoom-in, pan, drag-to-select, image crop, moving or 
       * resizing the selected node. The figure canvas or displayed figure is updated as needed, and the interaction 
       * layer is repainted to indicate that the animated gesture has finished.
       */
      @Override public void mouseReleased(MouseEvent e) 
      { 
         Point p = e.getPoint();
         if(lastDragPt != null && lastDragPt.distance(p) >= 5) lastDragPt.setLocation(p);
         stopDragging(false, e);
         
         mouseMoved(e);   // update cursor mode if necessary
         updateCursorPositionOnRulers(true, e.getX(), e.getY());
      }
      
      /**
       * When the primary mouse button is single-clicked in any cursor mode other than "zoom mode", the deepest (in the 
       * Z-order) graphic node under the mouse-click location is made the current selected node in the displayed figure.
       * If the Control key (Command key on Mac OS X) is depressed uring the mouse click, then the selection state of 
       * the node under the mouse is toggled among three possible states: selected, selected AND the focus node of the 
       * selection, unselected (except that one cannot de-select a singly-selected node). This is the means by which the
       * user can interactively select multiple graphic nodes on the figure canvas.
       * 
       * <p>If the user "double-clicks" on the current focus node (using the primary mouse button), and that node is a 
       * text label, graph axis, or text box, then an in-place "pop-up" editor is raised on top of the node so that the 
       * user can immediately edit the node's "title" attribute. The pop-up can be extinguished by clicking outside it 
       * (no changes made), or by entering a new value for the title attribute. This provides a way to edit label text,
       * text box content, or an axis label without having to shift one's attention to the node property editor.</p>
       * 
       * <p>When the non-primary mouse button is single-clicked ("right-click") in any cursor mode other than "zoom
       * mode", a context menu is raised that offers convenient access to a subset of the figure composer's action
       * commands, primarily those relevant to the currently selected nodes in the displayed figure, or to the figure
       * canvas itself.</p>
       * 
       * <p>In zoom mode, a single left(right)-click doubles (halves) the current scale factor, while maintaining the 
       * current pan position, if possible. (NOTE this should not conflict with the zoom-by-drag gesture, since a mouse 
       * click event is NOT sent after dragging and releasing the mouse.</p>
       */
      @Override public void mouseClicked(MouseEvent e) 
      {
         Point p = e.getPoint();
         boolean isBtn1 = (e.getButton() == MouseEvent.BUTTON1);
         if(mode != CursorMode.ZOOM && (e.getClickCount() == 1) && !isBtn1)
         {
            // if user right-clicks on a selected nodes and more than one node is currently selected, make that
            // node the focus of the current selection
            FGraphicModel fig = getSelectedFigureModel();
            FGraphicNode n;
            figCanvas.deviceToLogical(p.x, p.y, pLog);
            n = fig.findSmallestNodeUnder(pLog);
            if(n != null && n.getNodeType() != FGNodeType.FIGURE && selectedNodes.contains(n) &&
                  n != fig.getFocusForSelection())
               fig.setFocusForSelection(n);
            
            // now display the right-click context menu near the mouse click location
            refreshActions();
            contextMenu.show(this, p.x + 5, p.y + 5);
         }
         else if(mode != CursorMode.ZOOM)
         {
            FGraphicModel fig = getSelectedFigureModel();
            if(fig != null)
            {
               FGraphicNode n;
               figCanvas.deviceToLogical(p.x, p.y, pLog);
               n = fig.findSmallestNodeUnder(pLog);
               if(n == null) return;
               
               if((!showSelection) && selectedNodes.contains(n))
                  showSelectionOverlay(true);

               int ctrlCmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
               if((e.getModifiersEx() & ctrlCmdMask) == ctrlCmdMask)
               {
                  // toggle select state of the node clicked
                  if(!fig.isSelectedNode(n)) fig.reviseSelection(n, null);
                  else if(n != fig.getFocusForSelection()) fig.setFocusForSelection(n);
                  else fig.reviseSelection(null, n);  // no effect if single node selected
               }
               else if(n == fig.getSelectedNode())
               {  
                  // if user double-clicks on the currently selected node and no other nodes are selected, a modal popup 
                  // dialog MAY be raised nearby to let user edit that node's title property "in place".
                  if(e.getClickCount() == 2 && (!fig.isMultiNodeSelection()))
                     TitlePopupDlg.raiseEditTitlePopup(n, figCanvas);
               }
               else 
                  fig.setSelectedNode(n);
            }
         }
         else if(e.getClickCount() == 1 && enaZoomOnClick)
         {
            boolean wasScaleToFit = figCanvas.isScaleToFitOn();
            figCanvas.zoomAndPanTo(e.getX(), e.getY(), isBtn1);
            updateCursorPositionOnRulers(true, e.getX(), e.getY());
            
            // if canvas was scaled to fit before the scale adjustment, it will be no longer be in that mode after. So 
            // we need to update the relevant command actions
            if(wasScaleToFit != figCanvas.isScaleToFitOn())
            {
               actions.get(FCActionType.VIEW_TOGGLEMODE).refreshState();
               actions.get(FCActionType.VIEW_RESETZOOM).refreshState();
            }
         }
         enaZoomOnClick = false;
         
         // update cursor mode if necessary
         mouseMoved(e);
      }
      
      /**
       * Handles animation of an ongoing mouse-drag gesture. The method immediately repaints the smallest possible 
       * rectangle that must be repainted to update the relevant animation.
       */
      @Override public void mouseDragged(MouseEvent e)
      {
         if(!isDragging) return;
         
         // if mouse has not moved far enough from its previous drag location, do nothing (hysteresis)
         Point p = e.getPoint();
         if(p.distance(lastDragPt) < 5) return;
         
         int oldX = lastDragPt.x;
         int oldY = lastDragPt.y;
         lastDragPt.setLocation(p);
         
         dirtyRect.setFrame(0,0,0,0);
         
         if(mode == CursorMode.PAN)
         {
            figCanvas.panBy(oldX - p.x, oldY - p.y);
         }
         else if(mode == CursorMode.ZOOM)
         {
            // erase old location of drag shape
            Rectangle r = (Rectangle) dragShape;
            if(dirtyRect.isEmpty()) dirtyRect.setFrame(r);
            else Rectangle.union(r, dirtyRect, dirtyRect);
            
            // determine UL corner and dimensions of zoom rectangle given the drag origin and the current drag location.
            int xUL = Math.min(dragOrigin.x, lastDragPt.x);
            int yUL = Math.min(dragOrigin.y, lastDragPt.y);
            int rw = Math.max(dragOrigin.x, lastDragPt.x) - xUL;
            int rh = Math.max(dragOrigin.y, lastDragPt.y) - yUL;
            
            // fix rect dimensions to match graphic's aspect ratio. We may also have to fix the rect's UL corner.
            if(rh != 0)
            {
               double aspect = ((double) rw) / ((double) rh);
               if(aspect > graphicAR)
               {
                  rw = (int) (graphicAR * rh);
                  if(xUL != dragOrigin.x) xUL = dragOrigin.x - rw; 
               }
               else
               {
                  rh = (int) (((double) rw) / graphicAR);
                  if(yUL != dragOrigin.y) yUL = dragOrigin.y - rh;
               }
            }
            
            // if zoom rectangle is less than 20 pixels on either side, set its dimensions to zero.
            r.x = xUL;
            r.y = yUL;
            r.width = (rw < 20 || rh < 20) ? 0 : rw;
            r.height = (rw < 20 || rh < 20) ? 0 : rh;

            Rectangle.union(r, dirtyRect, dirtyRect);
         }
         else if(mode == CursorMode.SELECT)
         {
            // erase old location of selection rectangle
            Rectangle r = (Rectangle) dragShape;
            if(dirtyRect.isEmpty()) dirtyRect.setFrame(r);
            else Rectangle.union(r, dirtyRect, dirtyRect);
            
            // determine UL corner and dimensions of selection rectangle given drag origin and current drag location.
            int xUL = Math.min(dragOrigin.x, lastDragPt.x);
            int yUL = Math.min(dragOrigin.y, lastDragPt.y);
            int rw = Math.max(dragOrigin.x, lastDragPt.x) - xUL;
            int rh = Math.max(dragOrigin.y, lastDragPt.y) - yUL;
            
            // if selection rectangle is less than 3 pixels on either side, set its dimensions to zero.
            r.x = xUL;
            r.y = yUL;
            r.width = (rw < 3 || rh < 3) ? 0 : rw;
            r.height = (rw < 3 || rh < 3) ? 0 : rh;

            Rectangle.union(r, dirtyRect, dirtyRect);
         }
         else if(mode == CursorMode.MOVE)
         {
            // erase old drag shape (which is translated copy of the node's current selection shape)
            if(dirtyRect.isEmpty()) dirtyRect.setFrame(dragShape.getBounds2D());
            else Rectangle.union(dragShape.getBounds2D(), dirtyRect, dirtyRect);
            
            // construct new drag shape by translating the current selection shape
            int dx = lastDragPt.x - dragOrigin.x;
            int dy = lastDragPt.y - dragOrigin.y;
            GeneralPath gp = new GeneralPath();
            gp.append(initialMoveDragShape.getPathIterator(AffineTransform.getTranslateInstance(dx,dy)), false);
            dragShape = gp;
            
            // ensure area occupied by new drag shape is included in repaint
            Rectangle.union(dragShape.getBounds2D(), dirtyRect, dirtyRect);
            
            // get move cue and include it in repaint. Put move cue just below current drag location. When multiple
            // nodes are being dragged, we just indicate the change in X and Y in inches. Else, query the dragged node
            // for the move cue.
            if(cue != null) Rectangle.union(cueBounds, dirtyRect, dirtyRect);
            figCanvas.deviceToLogical(dragOrigin.x, dragOrigin.y, pLog);
            figCanvas.deviceToLogical(lastDragPt.x, lastDragPt.y, pLog2);
            
            if(dragNode != null) 
               cue = dragNode.getMoveCue(pLog2.getX() - pLog.getX(), pLog2.getY() - pLog.getY());
            else
               cue = String.format("\u0394x = %.3f in, \u0394y = %.3f in", (pLog2.getX() - pLog.getX()) / 1000.0,
                     (pLog2.getY() - pLog.getY()) / 1000.0);
            
            if(cue != null)
            {
               // we make the cue bounds more than wide enough since we're not measuring the text bounds here
               cueBounds.setFrame(lastDragPt.x-300, lastDragPt.y-26, 600, 100);
               Rectangle.union(cueBounds, dirtyRect, dirtyRect);
            }
         }
         else if(mode == CursorMode.CROP)
         {
            // erase the previous cue and crop shape.
            if(dirtyRect.isEmpty()) dirtyRect.setFrame(cueBounds);
            else Rectangle.union(cueBounds, dirtyRect, dirtyRect);

            if(dragShape != null) Rectangle.union(dragShape.getBounds2D(), dirtyRect, dirtyRect);
            
            if(Math.abs(dragOrigin.x - lastDragPt.x) < 10 || Math.abs(dragOrigin.y - lastDragPt.y) < 10)
            {
               // if current drag location is within 10 px of the drag origin in H or V, hide the drag shape and cue --
               // considered too small
               cue = null;
               cueBounds.setFrame(0,0,0,0);
               dragShape = null;
            }
            else
            {
               figCanvas.deviceToLogical(dragOrigin.x, dragOrigin.y, pLog);
               figCanvas.deviceToLogical(lastDragPt.x, lastDragPt.y, pLog2);
               
               dragShape = figCanvas.logicalToDevice(
                     ((ImageNode) dragNode).getCropShapeFromDiagonal(pLog, pLog2, rDev));
               if(dragShape == null)
               {
                  cueBounds.setFrame(0,0,0,0);
                  cue = null;
               }
               else
               {
                  cue = rDev.width + "x" + rDev.height + " at [" + rDev.x + "," + rDev.y + "]";
                  Rectangle2D bounds = dragShape.getBounds2D();
                  cueBounds.setFrame(bounds.getCenterX()-100, bounds.getMinY()-70, 200, 100);
                  
                  Rectangle.union(bounds, dirtyRect, dirtyRect);
                  Rectangle.union(cueBounds, dirtyRect, dirtyRect);
               }
            }
         }
         else if(mode == CursorMode.ROTATE || mode.isResizeMode())
         {
            // erase old drag shape and descriptive cue
            if(dragShape != null)
            {
               if(dirtyRect.isEmpty()) dirtyRect.setFrame(dragShape.getBounds2D());
               else Rectangle.union(dragShape.getBounds2D(), dirtyRect, dirtyRect);
               
               if(cue != null && !cueBounds.isEmpty()) Rectangle.union(cueBounds, dirtyRect, dirtyRect);
            }
            cue = null;
            cueBounds.setFrame(0,0,0,0);
            
            // get new drag shape and decriptive cue given new location of mouse
            figCanvas.deviceToLogical(dragOrigin.x, dragOrigin.y, pLog);
            figCanvas.deviceToLogical(p.x, p.y, pLog2);
            if(mode == CursorMode.ROTATE)
            {
               Graph3DNode g3 = (Graph3DNode) dragNode;
               dragShape = figCanvas.logicalToDevice(g3.getReorientByDragShape(pLog, pLog2, cueBuffer));
            }
            else 
               dragShape = figCanvas.logicalToDevice(dragNode.getDragResizeShape(anchor, pLog, pLog2, cueBuffer));

            // ensure area occupied by new drag shape and descriptive cue is included in repaint
            if(dragShape != null)
            {
               Rectangle2D bounds = dragShape.getBounds2D();
               if(dirtyRect.isEmpty()) dirtyRect.setFrame(bounds);
               else Rectangle.union(bounds, dirtyRect, dirtyRect);
               
               if(cueBuffer.length() > 0)
               {
                  cue = cueBuffer.toString();
                  cueBuffer.setLength(0);
                  
                  // we make the cue bounds more than wide enough since we're not measuring the text bounds here
                  cueBounds.setFrame(lastDragPt.x-300, lastDragPt.y-26, 600, 100);
                  Rectangle.union(cueBounds, dirtyRect, dirtyRect);
               }
            }
         }

         if(!dirtyRect.isEmpty())
         {
            dirtyRect.grow(5,5);
            paintImmediately(dirtyRect);
         }
         
         updateCursorPositionOnRulers(true, p.x, p.y);
      }

      /**
       * Terminate a mouse drag-mediated operation, optionally canceling its effect on the current figure. 
       * @param cancel If true, any change in the selected node or the figure implied by the mouse drag in progress is 
       * discarded. Ignored in the case of pan-by-drag.
       * @param e The mouse event that terminated the drag gesture. May be null.
       */
      private void stopDragging(boolean cancel, MouseEvent e)
      {
         if(!isDragging) return;
         
         isDragging = false;
         if(mode == CursorMode.PAN)
         {
            // panning mode is only active during a mouse drag, so we return to select mode
            mode = CursorMode.SELECT;
            if(inside) setCursor(mode.cursor);
         }
         else if(mode == CursorMode.ZOOM)
         {
            // always cancel zoom in if the zoom rectangle is less than 20 pixels on either side.
            Rectangle r = (Rectangle) dragShape;
            if(r.width < 20 || r.height < 20) cancel = true;
            
            if(!cancel)
            {
               boolean wasScaleToFit = figCanvas.isScaleToFitOn();
               figCanvas.zoomIn(r);

               // if canvas was scaled to fit before the zoom-in, it will be no longer be in that mode after. So we need
               // to update the relevant command actions
               if(wasScaleToFit)
               {
                  actions.get(FCActionType.VIEW_TOGGLEMODE).refreshState();
                  actions.get(FCActionType.VIEW_RESETZOOM).refreshState();
               }
            }
         }
         else if(mode == CursorMode.SELECT)
         {
            // select all nodes intersecting the drag rectangle upon mouse release
            Rectangle r = (Rectangle) dragShape;
            if(!cancel) cancel = r.isEmpty();
            
            FGraphicModel fig = getSelectedFigureModel();
            if(fig != null && !cancel)
            {
               figCanvas.deviceToLogical(r.x, r.y, pLog);
               figCanvas.deviceToLogical(r.x + r.width, r.y + r.height, pLog2);
               dragSelectRLog.setFrameFromDiagonal(pLog, pLog2);
               
               // select all nodes intersecting the drag rect, replacing the current selection. However, if the Ctrl
               // (Command in OSX) key is depressed at mouse release, add them to the current selection.
               List<FGraphicNode> nodes = fig.findAllIntersectingNodes(dragSelectRLog);
               if(!nodes.isEmpty())
               {
                  boolean replaceSel = true;
                  if(e != null)
                  { 
                     int ctrlCmdMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
                     if((e.getModifiersEx() & ctrlCmdMask) == ctrlCmdMask) replaceSel = false;
                  }
                  fig.reviseSelection(nodes, replaceSel ? fig.getSelectedNodes(null) : null);
               }
            }
         }
         else if(mode == CursorMode.MOVE)
         {
            if(!cancel) cancel = (dragOrigin.distance(lastDragPt) < 5);
            
            if(!cancel)
            {
               figCanvas.deviceToLogical(dragOrigin.x, dragOrigin.y, pLog);
               figCanvas.deviceToLogical(lastDragPt.x, lastDragPt.y, pLog2);
               FGraphicModel fig = getSelectedFigureModel();
               if(fig != null && (pLog2.getX() != pLog.getX() || pLog2.getY() != pLog.getY()))
                  fig.moveCurrentSelection(pLog2.getX() - pLog.getX(), pLog2.getY() - pLog.getY());
            }
         }
         else if(mode == CursorMode.CROP)
         {
            // during crop-by-drag gesture, rDev holds the dynamic crop rectangle in image space pixels
            if(dragShape != null && !cancel) ((ImageNode) dragNode).setCrop(rDev);
         }
         else if(mode == CursorMode.ROTATE || mode.isResizeMode())
         {
            if(!cancel) cancel = (dragOrigin.distance(lastDragPt) < 5);
            
            if(!cancel)
            {
               figCanvas.deviceToLogical(dragOrigin.x, dragOrigin.y, pLog);
               figCanvas.deviceToLogical(lastDragPt.x, lastDragPt.y, pLog2);
               if(mode == CursorMode.ROTATE) ((Graph3DNode) dragNode).executeReorientByDrag(pLog, pLog2);
               else dragNode.executeResize(anchor, pLog, pLog2);
            }
         }
         
         // erase last drag shape and cue, if any
         dirtyRect.setFrame(0,0,0,0);
         
         if(dragShape != null) dirtyRect.setFrame(dragShape.getBounds2D());
         if(!cueBounds.isEmpty())
         {
            if(dirtyRect.isEmpty()) dirtyRect.setFrame(cueBounds);
            else Rectangle.union(cueBounds, dirtyRect, dirtyRect);
         }

         dragOrigin = null;
         lastDragPt = null;
         anchor = null;
         dragShape = null;
         initialMoveDragShape = null;
         dragNode = null;
         cue = null;
         cueBounds.setFrame(0,0,0,0);

         if(!dirtyRect.isEmpty())
         {
            dirtyRect.grow(5,5);
            paintImmediately(dirtyRect);
         }
      }
      
      /**
       * If not in zoom or pan cursor modes, update cursor mode IAW the current mouse position:
       * <ul>
       *    <li>If a single node is selected in the figure AND the node supports interactive resizing AND the mouse is
       *    close enough to the node's resize outline, then enter one of the resize cursor modes.</li>
       *    <li>If the mouse is inside the rectangular bounds of ANY selected node that is movable, enter the move
       *    cursor mode. All movable nodes in the current selection will be affected by the move-by-drag gesture.</li>
       *    <li>If the selected node is an image element, AND it is the only node selected, AND the mouse is over the 
       *    current rendered image, then enter the crop cursor mode instead.</li>
       *    <li>If the selected node is a 3D graph, AND it is the only node selected, AND the mouse is over the trigger
       *    zone for a "orient-by-drag" gesture, then enter the rotate cursor mode instead.</li>
       *    <li>Otherwise, the cursor mode is the default "select" mode (cross-hair cursor).</li>
       * </ul>
       * 
       * <p>In the select cursor mode only, update the translucent gold highlight around the deepest graphic node that's 
       * under the current mouse position (if it has changed).</p>
       */
      @Override public void mouseMoved(MouseEvent e) 
      { 
         if(mode == CursorMode.ZOOM || mode == CursorMode.PAN) return;
         
         // based on mouse location WRT the currently selected node(s), choose the cursor mode...
         Point p = e.getPoint();
         CursorMode nextMode = getResizeCursorMode(p);
         FGraphicNode selNode = null;
         if(nextMode == null)
         {
            if(selectedNodes.size() > 1)
            {
               for(int i=0; i<selectedNodes.size(); i++) 
               {
                  Shape selShape = selectionShapes.get(i);
                  if(selShape != null && selShape.contains(p) && selectedNodes.get(i).canMove())
                  {
                     selNode = selectedNodes.get(i);
                     nextMode = CursorMode.MOVE;
                     break;
                  }
               }
            }
            else if(selectedNodes.size() == 1)
            {
               selNode = selectedNodes.get(0);
               Shape selShape = selectionShapes.get(0);
               if(selShape != null && selShape.contains(p))
               {
                  if(selNode.canMove()) nextMode = CursorMode.MOVE;
                  if(selNode.getNodeType() == FGNodeType.IMAGE && currImageCropShape != null &&
                        currImageCropShape.contains(p))
                     nextMode = CursorMode.CROP;
                  else if(selNode.getNodeType() == FGNodeType.GRAPH3D && curr3DGraphOrientZone != null &&
                        curr3DGraphOrientZone.contains(p))
                     nextMode = CursorMode.ROTATE;
               }
            }
         }
         if(nextMode == null) nextMode = CursorMode.SELECT;
         
         dirtyRect.setFrame(0,0,0,0);
         
         if(nextMode != mode)
         {
            CursorMode old = mode;
            mode = nextMode;
            setCursor(mode.cursor);
            
            // whenever we enter CROP mode, we highlight the rendered image. When we leave, we have to turn it off, and
            // erase the crop label cue
            if((old == CursorMode.CROP || mode == CursorMode.CROP))
            {
               if(currImageCropShape != null)
               {
                  if(dirtyRect.isEmpty()) dirtyRect.setFrame(currImageCropShape.getBounds2D());
                  else Rectangle.union(currImageCropShape.getBounds2D(), dirtyRect, dirtyRect);
               }
               if(dirtyRect.isEmpty()) dirtyRect.setFrame(cueBounds);
               else Rectangle.union(cueBounds, dirtyRect, dirtyRect);
               
               if(old == CursorMode.CROP) cue = null;
            }
         }
         
         if(mode == CursorMode.CROP)
         {
            // as mouse moves around over the image rectangle, report its current coordinates in image pixels via a
            // a small label drawn on the interaction layer. The label is drawn above the cursor position, centered
            // horizontally. Note hysteresis of roughly 5 device pixels before label is updated.
            ImageNode img = (ImageNode) selNode;
            if((img != null) && (cue == null || p.distance(cueBounds.getCenterX(), cueBounds.getCenterY()) > 5))
            {
               if(dirtyRect.isEmpty()) dirtyRect.setFrame(cueBounds);
               else Rectangle.union(cueBounds, dirtyRect, dirtyRect);
               
               figCanvas.deviceToLogical(p.x, p.y, pLog);
               
               if(img.convertPointToImagePixels(pLog))
               {
                  cue = "[" + ((int) pLog.getX()) + "," + ((int) pLog.getY()) + "]";
                  cueBounds.setRect(p.x - 100, p.y - 65, 200, 100);
               }
               else
               {
                  cue = null;
                  cueBounds.setRect(0,0,0,0);
               }
               
               Rectangle.union(cueBounds, dirtyRect, dirtyRect);
            }
         }
         
         if(!dirtyRect.isEmpty())
         {
            dirtyRect.grow(5,5);
            paintImmediately(dirtyRect);
         }

         if(mode != CursorMode.ZOOM) updateHighlightNodeUnder(p);
         updateCursorPositionOnRulers(true, p.x, p.y);
      }

      /**
       * Returns the appropriate resize cursor mode if the current focus node is resizable and the specified point is
       * close enough to a corner or edge of the node's current focus highlight path. Corner modes take precedence
       * over edge modes. If the focus node is a line segment node or the node is rotated WRT the figure, the
       * non-directional resize cursor is selected.
       * 
       * @param p A point in figure canvas device pixels.
       * @return The appropriate resize cursor mode, or null if current focus node is not resizable or the point is not
       * close enough to any corner or edge of the focus highlight rectangle.
       */
      private CursorMode getResizeCursorMode(Point p)
      {
         anchor = null;
         FGraphicNode selNode = (selectedNodes.size() == 1) ? selectedNodes.get(0) : null;
         if((!showSelection) || selNode == null || !selNode.canResize()) return(null);
         
         // want a tolerance of +/-3 pixels. Need to convert that to logical units
         figCanvas.deviceToLogical(0, 0, pLog2);
         double tol = pLog2.getX();
         figCanvas.deviceToLogical(3, 0, pLog2);
         tol = Math.abs(pLog2.getX()-tol);
         
         figCanvas.deviceToLogical(p.x, p.y, pLog);
         anchor = selNode.getResizeAnchor(pLog, tol);
         boolean nonDir = (selNode.getNodeType()==FGNodeType.LINE) || selNode.isRotatedOnFigure(); 
         return(CursorMode.resizeModeFromAnchorPosition(anchor, nonDir));
      }
      
      /**
       * Update the translucent gold highlight that's drawn around the shape bounding the smallest graphic node that is
       * under the specified point in the displayed figure.
       * @param p A point in figure canvas device coordinates.
       */
      private void updateHighlightNodeUnder(Point p)
      {
         FGraphicModel fig = getSelectedFigureModel();
         if(fig == null) return;
         
         FGraphicNode n = null;
         if(p != null)
         {
            figCanvas.deviceToLogical(p.x, p.y, pLog);
            n = fig.findSmallestNodeUnder(pLog);
         }
         if(n == currHighlightedNode) return;
         
         currHighlightedNode = n;
         
         // include bounds of old highlight so that it will be erased
         dirtyRect.setFrame(0,0,0,0);
         if(highlightedNodeShape != null) dirtyRect.setFrame(highlightedNodeShape.getBounds2D());

         // get the highlight shape for the node under the mouse, and include its bounds in dirty area for repaint.
         highlightedNodeShape = null;
         if(currHighlightedNode != null)
         {
            Graphics2D g2 = figCanvas.getViewerGraphicsContext();
            if(g2 != null)
            {
               highlightedNodeShape = figCanvas.logicalToDevice(currHighlightedNode.getFocusShape(g2));
               g2.dispose();
            }
         }
         if(highlightedNodeShape != null)
         {
            if(dirtyRect.isEmpty()) dirtyRect.setFrame(highlightedNodeShape.getBounds2D());
            else Rectangle.union(highlightedNodeShape.getBounds2D(), dirtyRect, dirtyRect);
         }
         
         if(!dirtyRect.isEmpty())
         {
            dirtyRect.grow(5, 5);
            paintImmediately(dirtyRect);
         }
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
            
            // paint the current selection highlight
            if(showSelection && !selectionShapes.isEmpty())
            {
               g2.setColor(translucentSteelBlue);
               for(Shape selShape : selectionShapes)
               {
                  if(selShape == null) continue;
                  g2.fill(selShape);
               }
               
               // the last (or only) node in the selection list also gets a blue outline to mark it as the "focus" node
               // for the selection.
               Shape selShape = selectionShapes.get(selectionShapes.size()-1);
               if(selShape != null)
               {
                  g2.setColor(Color.BLUE);
                  g2.setStroke(solidLine6mi);
                  g2.draw(selShape);
               }
               
               // if the (singly) selected node can be resized interactively, draw its resize outline to indicate this.
               FGraphicNode singleSelNode = (selectedNodes.size() == 1) ? selectedNodes.get(0) : null;
               if(singleSelNode != null && singleSelNode.canResize() && resizeOutline != null)
               {
                  g2.setColor(translucentWhite);
                  g2.setStroke(solidLine3mi);
                  g2.draw(resizeOutline);
                  g2.setColor(steelBlue);
                  g2.setStroke(dottedLine2mi);
                  g2.draw(resizeOutline);
               }
            }
            
            // in "crop" cursor mode, highlight the shape that covers the rendered image to be cropped.
            if(mode == CursorMode.CROP)
            {
               if(currImageCropShape != null)
               {
                  g2.setColor(translucentWhite);
                  g2.fill(currImageCropShape);
               }
            }
            
            // if no mouse-drag gesture is in progress, highlight the deepest node under the mouse, if any. Otherwise,
            // paint the drag shape and cue string for the drag gesture in progress.
            if(!isDragging)
            {
               if(highlightedNodeShape != null)
               {
                  g2.setColor(translucentGoldenrod);
                  g2.setStroke(solidLine3mi);
                  g2.draw(highlightedNodeShape);
               }
            }
            else if(mode == CursorMode.ZOOM)
            {
               // fill a small oval near the drag origin and the current zoom rectangle
               g2.setColor(translucentGray);
               g2.fillOval(dragOrigin.x-5, dragOrigin.y-5, 10, 10);
               if(dragShape != null) g2.fill(dragShape);
            }
            else if(mode == CursorMode.SELECT)
            {
               // draw a translucent steel blue outline around the current "drag-to-select" rectangle
               g2.setColor(translucentSteelBlue);
               g2.setStroke(solidLine3mi);
               if(dragShape != null) g2.draw(dragShape);
            }
            else if(mode == CursorMode.MOVE || mode == CursorMode.CROP || mode == CursorMode.ROTATE ||
                  mode.isResizeMode())
            {
               // outline the current drag shape and draw the cue string if there is one
               g2.setColor(Color.BLACK);
               g2.setStroke(dottedLine2mi);
               if(dragShape != null) g2.draw(dragShape);
            }
         
            // draw the cue string and update its rendered bounds -- so we can erase it properly on the next repaint.
            // Before painting string, we get an accurate bounding box and fill it with translucent white to make sure
            // there's a decent background for reading the string.
            if(cue != null)
            {
               double cx = cueBounds.getCenterX();
               double cy = cueBounds.getCenterY();
               g2.translate(cx, cy);
               strPainter.setTextAndLocation(cue, 0, 0);
               g2.scale(1,-1);
               strPainter.updateFontRenderContext(g2);
               strPainter.invalidateBounds();
               strPainter.getBounds2D(cueBounds);
               cueBounds.setFrame(cueBounds.getX()-2, cueBounds.getY()-2, cueBounds.getWidth() + 4, 
                     cueBounds.getHeight() + 4);
               g2.setColor(translucentWhite);
               g2.fill(cueBounds);
               strPainter.render(g2, null);
               g2.scale(1,-1);
               g2.translate(-cx, -cy);
               
               // to erase the cue next time, we have to put the bounding box back in original canvas device coords!
               cueBounds.setRect(cueBounds.getX()+cx, cueBounds.getY()+cy, 
                     cueBounds.getWidth(), cueBounds.getHeight());
            }
         }
         finally
         {
            if(g2 != null) g2.dispose();
         }

         // reset dirty rect so that any paint cycle we don't initiate internally paints the entire layer
         dirtyRect.width = dirtyRect.height = 0;
      }
      
      /** True if cursor is currently inside the layer, which is on top of and coincident with figure canvas. */
      private boolean inside = false;
      
      /** The current cursor mode. */
      private CursorMode mode = CursorMode.SELECT;
      /** 
       * In any of the resize cursor modes, this is the resize anchor type. In certain scenarios (rotated node, a line
       * segment node), the actual cursor shown may be the generic non-directional resize cursor; so we need to keep
       * track of the resize anchor in play so the focus node can restrict the resize op accordingly.
       */
      private ResizeAnchor anchor = null;
      
      /** 
       * The list of nodes currently selected in the figure. Only a few operations are enabled when multiple nodes
       * are selected.
       */
      private final List<FGraphicNode> selectedNodes = new ArrayList<>();
      
      /**
       * The shapes covering the currently selected nodes in the figure, in figure canvas device coordinates. Any shapes
       * in this list are always painted, unless the {@link #showSelection} flag is cleared. This list must be updated 
       * whenever the figure's current selection changes, or the boundary of a selected node changes.
       */
      private final List<Shape> selectionShapes = new ArrayList<>();
      
      /** 
       * Shape representing the resize outline for the current singly-seleced node. Null if node cannot be interactively
       * resized, or if multiple nodes are selected. It must be updated whenever the figure's current selection changes,
       * or the boundary of the singly-selected node changes. User initiates a resize drag interaction by mousing down 
       * somewhere near the outline path.
       */
      private Shape resizeOutline = null;
      /** Clear this flag to hide the translucent shapes covering all selected nodes (normally flag is set). */
      private boolean showSelection = false;
      
      /** The node currently "under" the mouse and highlighted in the interaction layer. */
      private FGraphicNode currHighlightedNode = null;
      /** The shape covering the node currently "under" the mouse, in figure canvas device coordinates. */
      private Shape highlightedNodeShape = null;

      /** 
       * When an image node is singly-selected, this is the shape that tightly bounds the image itself, in figure canvas
       * device coordinates. Crop cursor mode is engaged whenever the mouse position lies inside this boundary. Will be
       * null if multiple nodes are selected, or if the singly-selected node is not an image node.
       */
      private Shape currImageCropShape = null;
      
      /**
       * When a 3D graph node is singly-selected, this shape represents the "trigger zone" for the "orient-by-drag"
       * gesture, in figure canvas coordinates. The 3D rotate cursor mode is engaged whenever the mouse position lies
       * within this shape. Will be null if multiple nodes are selected, or the single-selected node is not a 3D graph.
       */
      private Shape curr3DGraphOrientZone = null;
      
      /**
       * Flag set once a drag gesture has started. It is not set until mouse has moved at least 3 pixels away from the
       * location of the mouse-down event.
       */
      private boolean isDragging = false;
      /** Mouse location when a drag gesture started, in figure canvas device coords. Null when not in use. */
      private Point dragOrigin = null;
      /** Mouse location for the last mouse-dragged event, in figure canvas device coords. Null when not in use. */
      private Point lastDragPt = null;
      /** 
       * A shape drawn and/or filled during a mouse-drag gesture, in figure canvas device coords. Exact shape depends
       * on the type of drag gesture in progress. Null when not in use. 
       */
      private Shape dragShape = null;
      /** During a drag-to-select gesture, this is the current selection rectangle in rendered figure coordinates. */
      private final Rectangle2D dragSelectRLog = new Rectangle2D.Double();
      
      /** 
       * For multi-node move-by-drag only: In this scenario, the dragged shape is an unconnected collection of the 
       * selection shapes of all movable nodes in the current selection. We maintain the initial drag shape so we can 
       * update the animated drag shape relatively quickly as the mouse is dragged.
       */
      private Shape initialMoveDragShape = null;
      /** 
       * The graphic node affected by a drag gesture in the move, resize, and crop cursor modes. Null otherwise, and
       * also null if multiple-node selection is being moved.
       */
      private FGraphicNode dragNode = null;
      
      /** The aspect ratio of the graphic rendered on the figure canvas; zoom rectangle will have same aspect ratio. */
      private double graphicAR = 1.0;
      /** 
       * This flag is set each time the left or right mouse button is pressed while the cursor is in zoom mode. If it is
       * released without dragging, then we respond to the single click by zooming in (left-click) or out (right-click)
       * by a factor of 2.
       */
      private boolean enaZoomOnClick = false;
      
      /** 
       * A small label updated dynamically in certain situations: (1) display cursor location in image space pixels in 
       * crop cursor mode (selected node must be an image element); (2) display coordinates and size of crop rectangle 
       * in image space pixels during a crop drag gesture; (3) display similar cues during a move drag or resize drag
       * gesture. Null when no cue is displayed.
       */
      private String cue = null;
      /** 
       * The rectangle bounding the current cue label -- so it can be erased whenever it is updated. The cue string is
       * centered both horizontally and vertically about the center of this rectangle.
       */
      private final Rectangle2D cueBounds = new Rectangle2D.Double();
      /** A buffer in which the drag resize cue is prepared. */
      private final StringBuffer cueBuffer = new StringBuffer();
      /** Paints the cue string. */
      private final StringPainter strPainter;
      
      /** 
       * The rectangle that should be updated on each repaint cycle. When it is zero width, it is ignored and the entire
       * layer is repainted. Set to zero width after each repaint. 
       */
      private final Rectangle dirtyRect = new Rectangle(0,0,0,0);
      /** A point in rendered figure coordinates (to avoid frequent heap allocations). */
      private final Point2D pLog = new Point2D.Double(0, 0);
      /** Another point in rendered figure coordinates (to avoid frequent heap allocations). */
      private final Point2D pLog2 = new Point2D.Double(0, 0);
      /** A rectangle in figure canvas device pixels, for general use (to avoid frequent heap allocations). */
      private final Rectangle rDev = new Rectangle(0,0,0,0);

      private final Color transparentBlack = new Color(0,0,0,0);
      private final Color translucentWhite = new Color(255,255,255,192);
      private final Color translucentSteelBlue = new Color(70, 130, 180, 80);
      private final Color translucentGoldenrod = new Color(221, 170, 34, 80);
      private final BasicStroke solidLine3mi = new BasicStroke(3);
      private final BasicStroke solidLine6mi = new BasicStroke(3);
      private final Color translucentGray = new Color(80, 80, 80, 80);
      private final BasicStroke dottedLine2mi = new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
            10f, new float[] {3f, 3f}, 0f);
      private final Color steelBlue = new Color(70, 130, 180);
   }

   /**
    * This method refreshes the current tick marks and labels laid out on the horizontal and vertical rulers for the
    * figure canvas. It should be called whenever the canvas changes size, a different figure is installed in the 
    * canvas, or its current zoom and pan transform changes.
    */
   private void resetRulers()
   {
      if(!hRuler.isVisible()) return;
      hRuler.reset();
      vRuler.reset();  
   }

   /**
    * The horizontal and vertical rulers for the figure canvas can display a pointer at a specified coordinate along
    * the ruler's length, plus a label reflecting the coordinate value. This "cursor position indicator" is intended to
    * be turned on and updated as the cursor moves within the figure canvas. Invoke this method to update the cursor
    * position indicators for both rulers whenever the mouse enters or leaves the figure canvas or moves within it.
    * 
    * @param on True to show the cursor position indicators, false to hide them.
    * @param x The x-coordinate of the mouse cursor within the figure canvas, in device pixels.
    * @param y The y-coordinate of the mouse cursor within the figure canvas, in device pixels.
    */
   private void updateCursorPositionOnRulers(boolean on, int x, int y)
   {
      if(!hRuler.isVisible()) return;
      hRuler.updateCursorIndicator(on, x);
      vRuler.updateCursorIndicator(on, y);
   }

   /**
    * Are the figure canvas rulers currently shown?
    * @return True if rulers are visible, else false.
    */
   private boolean areRulersShown() { return(hRuler.isVisible()); }
   
   /**
    * Show or hide the figure canvas rulers.
    * @param show True to show rulers, false to hide them.
    */
   private void showRulers(boolean show)
   {
      if(show == hRuler.isVisible()) return;
      
      hRuler.setVisible(show);
      vRuler.setVisible(show);
      if(show) resetRulers();
   }
   
   /**
    * Helper class implements a horizontal or vertical ruler with a cursor position indicator that tracks and displays
    * the current position of the mouse cursor within the figure canvas so long as mouse position updates are delivered
    * via {@link #updateCursorIndicator(boolean, int)}.
    * 
    * <p>The ruler is constrained to {@link FigComposer#RULER_FIXED_SZ} pixels in thickness, and can grow as long as
    * necessary in the dimension that it measures. It normally displays major and minor tick marks, with labels on the
    * major ticks, reflecting position on the figure canvas in inches. The largest support major tick interval is 10in,
    * while the smallest is 0.01in; in the latter case, there are no minor tick marks. Be sure to call {@link #reset()}
    * whenever the size of the figure canvas changes, a different figure is loaded, or the canvas zoom/pan transform is
    * modified. This will recompute the ruler's set of tick marks and repaint it.</p>
    * 
    * <p>The implementation assumes that the vertically oriented ruler lies along the left edge of the canvas with its
    * length matching the canvas height. The horizontally oriented ruler lies along the bottom edge of the canvas and
    * includes the extra <code>RULER_FIXED_SZ</code> pixels occupied by the vertical ruler.</p>
    * 
    * @author sruffner
    */
   private class Ruler extends JPanel
   {
      /**
       * Construct a horizontal or vertical ruler for the figure canvas.
       * @param isHoriz True for horizontal orientation; false for vertical.
       */
      Ruler(boolean isHoriz)
      {
         this.isHoriz = isHoriz;
         
         // restrict height,width to 30 pixels for the horizontal, vertical ruler
         if(isHoriz)
         {
            setMinimumSize(new Dimension(RULER_UNCONSTRAINED_MINSZ, RULER_FIXED_SZ));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, RULER_FIXED_SZ));
            setPreferredSize(new Dimension(3*RULER_UNCONSTRAINED_MINSZ, RULER_FIXED_SZ));
         }
         else
         {
            setMinimumSize(new Dimension(RULER_FIXED_SZ, RULER_UNCONSTRAINED_MINSZ));
            setMaximumSize(new Dimension(RULER_FIXED_SZ, Integer.MAX_VALUE));
            setPreferredSize(new Dimension(RULER_FIXED_SZ, 3*RULER_UNCONSTRAINED_MINSZ));
         }
         
         tickLabelPainter = new StringPainter();
         tickLabelPainter.setAlignment(isHoriz ? TextAlign.CENTERED : TextAlign.TRAILING, 
               isHoriz ? TextAlign.LEADING : TextAlign.CENTERED);
         tickLabelPainter.setStyle(BasicPainterStyle.createBasicPainterStyle(
               BasicPainterStyle.getFontForPainter("Arial", GenericFont.SANSERIF, FontStyle.PLAIN, 10), 
               1, null, Color.BLACK, Color.BLACK));
         
         // the cursor arrow and a rounded rectangle that serves as the background for the cursor label. The arrow
         // points at the current cursor location and is generally centered WRT the long dimension of the rectangle,
         // except when it is near the ruler's extremes.
         if(isHoriz)
         {
            cursorArrow = new GeneralPath();
            cursorArrow.moveTo(0, 0); cursorArrow.lineTo(5, 8); cursorArrow.lineTo(-5, 8); cursorArrow.closePath();
            cursorLabelR = new RoundRectangle2D.Double(0, 0, 52, 18, 8, 8);
         }
         else
         {
            cursorArrow = new GeneralPath();
            cursorArrow.moveTo(0, 0); cursorArrow.lineTo(-8, 5); cursorArrow.lineTo(-8, -5); cursorArrow.closePath();
            cursorLabelR = new RoundRectangle2D.Double(0, 0, 18, 52, 8, 8);
         }
         
         cursorLabelPainter = new StringPainter();
         cursorLabelPainter.setAlignment(TextAlign.CENTERED, TextAlign.CENTERED);
         if(!isHoriz) cursorLabelPainter.setRotation(90);
         cursorLabelPainter.setStyle(BasicPainterStyle.createBasicPainterStyle(
               BasicPainterStyle.getFontForPainter("Arial", GenericFont.SANSERIF, FontStyle.BOLD, 14), 
               1, null, Color.BLACK, Color.BLACK));
      }
      
      /**
       * Reset the ruler: reset its major and minor tick marks based on the current size and zoom/pan transform for the
       * figure canvas, then repaint it entirely.
       */
      void reset()
      {
         prepareTickMarks();
         repaint();
      }
      
      /**
       * Update the current position and on/off state of the ruler's cursor position indicator. The indicator is 
       * repainted immediately to reflect the change. If cursor's state is unchanged, no action is taken.
       * @param ena True to show the cursor, false to hide it.
       * @param currPos The relevant coordinate - X for horizontal ruler, Y for vertical ruler -- of the current cursor 
       * position within the figure canvas, in device pixels. Ignored if cursor is being turned off.
       */
      void updateCursorIndicator(boolean ena, int currPos)
      {
         // cursor is off and will remain off: nothing to do!
         if(!(ena || cursorOn)) return;
         
         // calculate logical cursor coordinate in inches.
         figCanvas.deviceToLogical(currPos, currPos, ptLog);
         double currPosLog = (isHoriz ? ptLog.getX() : ptLog.getY()) / 1000.0;
         
         // cursor is on and will remain on: nothing to do if cursor position is unchanged in both device and logical
         // units (the device coordinate could be unchanged, but the logical coordinate changed b/c of a zoom transform
         // on the figure canvas).
         if(ena && cursorOn)
         {
            if((currPosLog == cursorLog) && cursorDev == ((isHoriz) ? currPos + 30 : currPos)) return;
         }
         
         // the endpoints of ruler region covered by cursor shape before and after update: the region to repaint
         int p0 = 0;
         int p1 = 0;
         int span = isHoriz ? getWidth() : getHeight();
         
         // if cursor indicator is on or is being turned on, save the new location in inches and in pixels. Also, be 
         // sure that repaint region includes covers the old and new locations of the indicator if cursor was on and
         // will remain on. If indicator is being turned on or off, we just need to repaint the region occupied by the 
         // new or old location, respectively.
         // NOTE: The indicator includes two shapes: an arrow that points to the current cursor coordinate along the 
         // ruler line, and a 52-pixel wide rectangle that serves as the background for the coordinate label. The arrow
         // is allowed to move right up against either end of the ruler. The rectangle stays centered WRT the arrow 
         // until its left or right edge touches the the left or right edge of the ruler, at which point it is fixed.
         // We need to be careful about this when computing the repaint region!!
         if(ena)
         {
            if(cursorOn)
            {
               p0 = cursorDev - 27;
               p1 = cursorDev + 27;
               if(p0 < 0) { p0 = 0; p1 = 54; }
               else if(p1 >= span) { p0 = span - 54; p1 = span; }
            }
            
            cursorLog = currPosLog;
            cursorDev = isHoriz ? currPos + 30 : currPos;
            
            if(cursorOn)
            {
               int p0Next = cursorDev - 27;
               int p1Next = cursorDev + 27;
               if(p0Next < 0) { p0Next = 0; p1Next = 54; }
               else if(p1Next >= span) { p0Next = span - 54; p1Next = span; }
               
               p0 = Math.min(p0, p0Next);
               p1 = Math.max(p1, p1Next);
            }
            else
            {
               p0 = cursorDev - 27;
               p1 = cursorDev + 27;
               if(p0 < 0) { p0 = 0; p1 = 54; }
               else if(p1 >= span) { p0 = span - 54; p1 = span; }
            }
            
            cursorOn = true;
         }
         else
         {
            // repaint the entire ruler whenever we turn off the cursor position indicator
            p0 = cursorDev - 27;
            p1 = cursorDev + 27;
            if(p0 < 0) { p0 = 0; p1 = 54; }
            else if(p1 >= span) { p0 = span - 54; p1 = span; }
            
            cursorLog = 0;
            cursorDev = 0;
            cursorOn = false;
         }
         
         if(isHoriz) paintImmediately(p0, 0, p1-p0, RULER_FIXED_SZ);
         else paintImmediately(0, p0, RULER_FIXED_SZ, p1-p0);
      }
      
      @Override protected void paintComponent(Graphics g)
      {
         int w = getWidth();
         int h = getHeight();
         
         // we work with a copy so that we don't impact painting of other components
         Graphics2D g2 = (Graphics2D) g.create();
         try
         {
            g2.setColor(rulerBkg);
            g2.fillRect(0, 0, w, h);
            
            // we always want nice-looking renderings
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // stroke the path that renders the ruler line, major tick marks, and minor tick marks
            g2.setColor(Color.BLACK);
            g2.setStroke(rulerStroke);
            g2.draw(tickPath);
            
            // draw the major tick mark labels
            g2.scale(1, -1);
            for(Integer tick : majorTicks.keySet())
            {
               int tickDev = tick;
               if(isHoriz)
                  tickLabelPainter.setTextAndLocation(majorTicks.get(tick), tickDev, -15);
               else
                  tickLabelPainter.setTextAndLocation(majorTicks.get(tick), RULER_FIXED_SZ-8, -tickDev);
               tickLabelPainter.render(g2, null);
            }
            g2.scale(1, -1);
            
            // draw the cursor position indicator, if it is enabled
            if(!cursorOn) return;
               
            if(isHoriz)
            {
               g2.translate(cursorDev, 0);
               g2.setColor(cursorBkg);
               g2.draw(cursorArrow);
               g2.fill(cursorArrow);
               
               // compute offset to center cursor bkg rect WRT cursor arrow -- unless we're near the ruler's ends!
               int ofs = -26;
               if(cursorDev < 26) ofs = -cursorDev;
               else if(cursorDev >= w-27) ofs = w-54-cursorDev;
               
               g2.translate(ofs, 8);
               g2.draw(cursorLabelR);
               g2.fill(cursorLabelR);

               cursorLabelPainter.setTextAndLocation(String.format("%.2f", cursorLog), 26, -9);
            }
            else
            {
               g2.translate(RULER_FIXED_SZ-1, cursorDev);
               g2.setColor(cursorBkg);
               g2.draw(cursorArrow);
               g2.fill(cursorArrow);
               
               // compute offset to center cursor bkg rect WRT cursor arrow -- unless we're near the ruler's ends!
               int ofs = -26;
               if(cursorDev < 26) ofs = -cursorDev;
               else if(cursorDev >= h-27) ofs = h-54-cursorDev;
               
               g2.translate(-26, ofs);
               g2.draw(cursorLabelR);
               g2.fill(cursorLabelR);

               cursorLabelPainter.setTextAndLocation(String.format("%.2f", cursorLog), 9, -26);
            }
            g2.scale(1, -1);
            cursorLabelPainter.render(g2, null);
            g2.scale(1, -1);
         }
         finally
         {
            if(g2 != null) g2.dispose();
         }
      }
      
      /** True for a horizontal ruler, false for vertical orientation. */
      private final boolean isHoriz;
      /** Renders any tick mark labels on the ruler. */
      private final StringPainter tickLabelPainter;
      /** Renders the coordinate label on the cursor position indicator. */
      private final StringPainter cursorLabelPainter;
      /** Fixed path that renders the arrow for the cursor position indicator. */
      private final GeneralPath cursorArrow;
      /** Fixed path that renders a rounded rectangular background for the coordinate label of the cursor indicator. */
      private final RoundRectangle2D cursorLabelR;
      
      /** Current position of cursor indicator in ruler's device pixels (0 if cursor indicator is turned off). */
      private int cursorDev = 0;
      /** Current position of cursor indicator in logical inches along the relevant dimension of figure canvas. */
      private double cursorLog = 0;
      /** True if cursor indicator is displayed; else false. */
      private boolean cursorOn = false;
      
      /** A single polyline path that renders the ruler line and its current set of major and minor tick marks. */
      private final GeneralPath tickPath = new GeneralPath();
      /** Ruler's current major tick mark labels, keyed by the tick mark position in pixels along ruler's length. */
      private final HashMap<Integer, String> majorTicks = new HashMap<>();
      
      /** A general-purpose point, to avoid frequent re-allocations. */
      private final Point2D ptLog = new Point2D.Double();
      
      /**
       * Prepare the polyline path that renders the ruler line with its major and minor tick marks. Also prepares the
       * major tick mark labels, keyed by their location in device pixels so that they can be drawn in the correct 
       * place in {@link #paintComponent(Graphics)}.
       */
      private void prepareTickMarks()
      {
         majorTicks.clear();
         tickPath.reset();
         
         // get ruler's span in logical inches and device pixels. For vertical ruler, logical and device coordinates
         // increase in opposite directions, so we swap endpoints. Also, the horizontal ruler includes 30 pixels left of
         // the figure canvas's left edge (because the vertical ruler occupies this space).
         int w = figCanvas.getWidth();
         int h = figCanvas.getHeight();

         figCanvas.deviceToLogical(-30, 0, ptLog);
         double start = (isHoriz ? ptLog.getX() : ptLog.getY()) / 1000.0;
         figCanvas.deviceToLogical(w, h, ptLog);
         double end = (isHoriz ? ptLog.getX() : ptLog.getY()) / 1000.0;
         if(!isHoriz) { double tmp = start; start = end; end = tmp; }
         
         double spanLog = end - start;
         double span = isHoriz ? (w + 30) : h;
         
         // choose major and minor tick intervals. We want major tick interval to lie between 100 and 200 pixels. We
         // start at 1-in ticks and scale down as far as 0.01-in if necessary, and scale up as far as 10-in. If the 
         // major tick interval is 0.01-in, then there are no minor ticks.
         int idxMajor = -1;
         int idxMinor = -1;
         for(int i=0; i<rulerTickIntervals.length; i++)
         {
            double pix = span * rulerTickIntervals[i] / spanLog;
            if(pix >= 40 && pix < 200)
            {
               idxMajor = i;
               if(i < rulerTickIntervals.length - 1)
               {
                  idxMinor = i+1;
                  pix = span * rulerTickIntervals[idxMinor] / spanLog;
                  if(pix < 10) idxMinor = -1;
               }
               break;
            }
         }
         if(idxMajor == -1)
         {
            double pix = span * rulerTickIntervals[0] / spanLog;
            if(pix <= 100)
            {
               idxMajor = 0;
               pix = span * rulerTickIntervals[1] / spanLog;
               idxMinor = (pix < 10) ? -1 : 1;
            }
            else idxMajor = rulerTickIntervals.length - 1;
         }
         
         // draw a single line spanning the ruler's length
         if(isHoriz) 
         {
            tickPath.moveTo(1, 1);
            tickPath.lineTo(w+29, 1);
         }
         else
         {
            tickPath.moveTo(RULER_FIXED_SZ-2, 1);
            tickPath.lineTo(RULER_FIXED_SZ-2, h-1);
         }
         
         // prepare the major tick mark labels that are within current logical bounds of ruler and add the actual tick
         // marks to the tick path. Note that we're computing the DEVICE coordinates of each tick mark, but the labels 
         // show the logical value.
         double majDivLog = rulerTickIntervals[idxMajor];
         double curr = Math.floor(start/majDivLog) * majDivLog;
         while(curr <= end)
         {
            if(curr >= start)
            {
               int tick = (int) ((curr - start) * span / spanLog);
               if(!isHoriz) tick = h - tick;
               String label;
               if(majDivLog >= 1.0) label = String.format("%d", (int) curr);
               else if(majDivLog >= 0.1) label = String.format("%.1f", curr);
               else label = String.format("%.2f", curr);
               
               majorTicks.put(tick, label);
               
               if(isHoriz)
               {
                  tickPath.moveTo(tick, 1);
                  tickPath.lineTo(tick, 10);
               }
               else
               {
                  tickPath.moveTo(RULER_FIXED_SZ-2, tick);
                  tickPath.lineTo(RULER_FIXED_SZ-5, tick);
               }
            }
            curr += majDivLog;
         }
         
         // add the minor tick marks (if any) that are within current logical bounds of ruler to the tick path. We skip 
         // any minor ticks that correspond to one of the major ticks computed above. Again, we're computing the DEVICE 
         // coordinates of each tick mark within the ruler's client area.
         if(idxMinor != -1)
         {
            double minDivLog = rulerTickIntervals[idxMinor];
            curr = Math.floor(start/minDivLog) * minDivLog;
            while(curr <= end)
            {
               if(curr >= start)
               {
                  double currDev = (curr - start) * span / spanLog;
                  if(!isHoriz) currDev = h - currDev;
                  int tick = (int) currDev;
                  if(!majorTicks.containsKey(tick))
                  {
                     if(isHoriz)
                     {
                        tickPath.moveTo(tick, 1);
                        tickPath.lineTo(tick, 6);
                     }
                     else
                     {
                        tickPath.moveTo(RULER_FIXED_SZ-2, tick);
                        tickPath.lineTo(RULER_FIXED_SZ-7, tick);
                     }
                  }
               }
               curr += minDivLog;
            }
         }
      }
   }

   /** The different supported tick intervals for the figure canvas rulers. */
   private final static double[] rulerTickIntervals = new double[] {10.0, 5.0, 1.0, 0.5, 0.1, 0.05, 0.01};
   /** The stroke for drawing ruler lines and tick marks. */
   private final static BasicStroke rulerStroke = new BasicStroke(1.0f);
   /** The background color for the figure canvas rulers. */
   private final static Color rulerBkg = new Color(224, 224, 224);
   /** The background color for the cursor position indicator in each canvas ruler. */
   private final static Color cursorBkg = new Color(70, 130, 180, 192);
   /** The fixed dimension of a canvas ruler: the height of the horizontal ruler, width of vertical ruler. */
   private final static int RULER_FIXED_SZ = 30;
   /** Minimum length of the unconstrained dimension of a canvas ruler. The ruler is likely to be much longer. */
   private final static int RULER_UNCONSTRAINED_MINSZ = 100;

   
   /**
    * A {@link SwingWorker SwingWorker} that loads a <i>DataNav</i> figure graphic from a FypML figure
    * definition file or Matlab FIG file. In the latter case, the generated graphic is the best-possible reproduction of
    * the Matlab figure defined in the FIG file, given the many differences between the FypML and Matlab graphic models.
    * After the task has finished, back on the Swing event dispatch thread, the prepared figure model is loaded into
    * the tab that was created for it prior to spawning this task. If the load task fails, that tab is removed and a 
    * modal popup dialog displays a brief description of the problem encountered (but only if no other figure loading
    * tasks are in progress).
    * 
    * @author sruffner
    */
   private class FigureLoader extends SwingWorker<FGraphicModel, Object>
   {
      /**
       * Construct a Swing worker thread loads the figure defined in the specified source file.
       * @param srcFile Pathname for the source file from which figure is loaded. This will be a FypML figure definition
       * (*.fyp) or a Matlab figure (*.fig).
       */
      FigureLoader(File srcFile) { this.figSrcFile = srcFile; }

      @Override protected FGraphicModel doInBackground() throws Exception
      {
         FGraphicModel model;
         StringBuffer sb = new StringBuffer();
         if(FCFileType.getFileType(figSrcFile) == FCFileType.FYP)
         {
            srcLastMod = figSrcFile.lastModified();
            model = FGModelSchemaConverter.fromXML(figSrcFile, sb);
         }
         else
            model = MatlabFigureImporter.importMatlabFigureFromFile(figSrcFile, sb);
         
         if(model == null)
         {
            errMsg = sb.toString();
            if(errMsg.length() > 100) errMsg = errMsg.substring(0, 100) + "...";
         }
         return(model);
      }
      
      @Override protected void done()
      {
         if(!isCancelled())
         {
            FGraphicModel model = null;
            try{ model = get(); } catch(Exception ignored) {}
            
            boolean isMatFig = (FCFileType.getFileType(figSrcFile) == FCFileType.FIG);

            // find the open tab for the figure file just loaded
            int iTab = -1;
            for(int i=0; i<openFigures.size(); i++)
            {
               Tab tab = openFigures.get(i);
               if(Utilities.filesEqual(figSrcFile, tab.src))
               {
                  iTab = i;
                  break;
               }
            }
            
            // if unable to load the figure, close the corresponding tab and report the problem. However, if attempting
            // to "re-sync" a figure file that can now no longer be parsed, instead mark the figure as having been
            // modified (if it hasn't already been modified).
            // Else install the model in that tab. If it is also the currently selected tab, load the figure 
            // for display. If a model is already installed in the tab, then it is being reloaded because the source
            // file was changed outside the application. In this scenario, we will not replace the model if it has been
            // modified (this is possible if the user makes a change while the loader thread is running in the
            // background).
            if(iTab > -1)
            {
               Tab tgtTab = openFigures.get(iTab);
               if(model == null)
               {
                  if(!tgtTab.syncing) 
                  {
                     closeTabNoConfirm(iTab);
                     
                     // we display an error message in a popup modal dialog, but ONLY if no other figure loading tasks 
                     // are under way!
                     if(loadersInProgress.size() <= 1)
                     {
                        if(errMsg == null) errMsg = "Unknown error occurred while loading/importing figure";
                        JOptionPane.showMessageDialog(
                              FigComposer.this, errMsg, "Load failed", JOptionPane.ERROR_MESSAGE);
                     }
                  }
                  else
                  {
                     tgtTab.fig.setModified(true);
                     tgtTab.syncing = false;
                     tgtTab.modified = true;
                     FigComposer.this.fireStateChanged();
                  }
               }
               else if(tgtTab.loading || tgtTab.syncing && !tgtTab.fig.isModified())
               {
                  
                  // if we loaded figure from a Matlab FIG file, mark the model as modified -- since we've never saved
                  // it as a FypML file (we cannot save it as a FIG!)
                  if(isMatFig) model.setModified(true);
                  tgtTab.onFigureLoaded(model, srcLastMod);
                  FCWorkspace.getInstance().addToRecentFiles(tgtTab.src);
                  if(iTab == iCurrentFig) loadSelectedFigure();
               }
            }
         }
         
         loadersInProgress.remove(this);
      }

      /** The figure source file processed by this figure loader. */
      private final File figSrcFile;
      
      /** The source file's last-modified time just prior to loading the figure from it (FypML files only). */
      private long srcLastMod = -1;
      
      /** After a failed load, this contains a brief error description; else null. */
      private String errMsg = null;
   }
   
   
   /**
    * Delegate which handles the figure composer's "auto-save" facility, including crash recovery.
    * 
    * <p>Once the auto-saver is initialized and started, a Swing timer triggers an auto-save cycle approximately every
    * 20 seconds. During any such cycle, the auto-saver scans the set of currently open figures, checking for figures
    * that have been removed, added, or modified since the last cycle. If a figure has undergone 10 or more edits since
    * it was last auto-saved, or it has undergone at least one edit and has not been auto-saved for over 2 minutes, then
    * it will be selected for back-up. During each cycle, if there any changes in the auto-saver's state or if a figure
    * has been selected for back-up, a background task is spawned to do the work. State information and auto-saved 
    * figure files are kept in a dedicated directory in the user's <i>DataNav</i> workspace.</p>
    * 
    * <p>During a normal application exit, the user will elect to save or discard any unsaved figures. In this scenario,
    * auto-save state information and any auto-saved figure files are deleted from the auto-save directory. However, if
    * the system or application crashes unexpectedly, those files will remain. Then, the next time the application is
    * run, this delegate will restore the auto-saved versions of any figures found in the auto-save directory. This
    * should help minimize the amount of work the user loses in the event of a crash.</p>
    * 
    * <p>The auto-save facility is intended to be as non-intrusive as possible. During an auto-save cycle, it will only
    * queue one figure for back-up because a copy of the figure must be constructed for passing to the worker thread.
    * Any interaction with the GUI or content that backs the GUI always takes place on the Swing event dispatch thread
    * to avoid any multi-threading issues. If the auto-save update thread fails to complete its task, it will simply
    * force another update rather than alert the user. The whole idea is that the auto-saver runs in the background 
    * without the user being aware of it. The only instance in which the user is notified is after a crash recovery at
    * application start-up.</p>
    * 
    * <p><i>Monitoring changes in the source file for each currently open figure</i>. The auto-saver's Swing timer
    * actually fires once every 2 seconds even though auto-save update checks happen roughly every 20 seconds. This is
    * because we use the same timer handler to more frequently check the last-modified time of the FypML source file
    * for each figure currently open in the figure composer component. If last-modified time does not match the time
    * when the file was initially loaded, then the file was likely modified outside the application. In this scenario,
    * <b>if the user has not yet modified the figure within this application</b>, then a figure loader thread is
    * spawned to reload the figure from the updated source file. If the figure has been changed, no reload occurs -- as
    * that would obliterate any changes made to the figure.</p>
    * @author sruffner
    */
   private class AutoSaver implements ActionListener
   {
      /**
       * Response to each expiration of the auto-saver's periodic Swing timer. 
       * 
       * <p>On every expiration, this handler checks the source files of any currently open figures <b>that have not 
       * been modified yet</b>. If a source file's last-modified time does not match the last-modified time when the
       * figure was initially loaded, then it is assumed that the file was changed outside this application, and a
       * background task is spawned to reload the figure.</p>
       * <p>On every 10th expiration of the timer, the current list of open figures are scanned to determine whether an 
       * auto-save task is needed. An update is needed if:
       * <ul>
       * <li>The user has closed one or more figures since the last check. These can be removed from the auto-saver's
       * current state.</li>
       * <li>There has been a change in the source path attached to an open figure. Path information must be persisted
       * in the auto-saver's state so that, upon recovering from a prior crash, a recovered figure can be associated 
       * with its original source file.</li>
       * <li>An open figure has not yet been auto-saved. Every open figure is auto-saved, even if it has not yet been
       * modified.</li>
       * <li>An open figure has been modified since the last auto-save and either 2 minutes have elapsed since it was
       * auto-saved, or the number of modifications is 10 or more.</li>
       * <li>The previous auto-save update task failed in some fashion.</li>
       * </ul>
       * 
       * <p>If an update is required AND the last auto-save update task is not still running, a new background thread is
       * spawned to perform the necessary work. Note that only one figure is auto-saved per check. Since this method is 
       * invoked on the Swing event dispatch thread, we want to minimize its execution time. The figure to be auto-saved
       * MUST be copied to avoid MT issues when the worker thread executes. Copying a bunch of figures all at once could
       * freeze the GUI.</p>
       * 
       * <p>If the auto-saver has not been successfully initialized, this method will attempt to initialize it.</p>
       */
      @Override public void actionPerformed(ActionEvent ae)
      {
         // on every timer expiration, we check the list of open figures to see if any source file has changed since
         // the figure was opened. This indicates that the source file was modified outside the application. In this
         // scenario, as long as the open figure has not been modified yet, a background thread is spawned to reload
         // the figure. Only one such task will be initiated on each timer expiration.
         for(Tab tab : openFigures)
         {
            boolean needSync = (tab.src != null) && (tab.src.isFile()) && (tab.fig != null) && 
                  (!tab.fig.isModified()) && (tab.srcLastMod > -1) && (tab.srcLastMod != tab.src.lastModified()) && 
                  !(tab.loading || tab.syncing);
            if(needSync)
            {
               tab.syncing = true;
               FigureLoader loader = new FigureLoader(tab.src);
               loadersInProgress.add(loader);
               loader.execute();
               FigComposer.this.fireStateChanged();
               break;
            }
         }
         
         // an auto-update check occurs for every 10 ticks of the Swing timer
         if(--ticksUntilCheck > 0) return;
         ticksUntilCheck = 10;
         
         // normally, auto-saver is initialized at startup, but if that failed, we'll keep trying.
         if(!initialized)
         {
            initialize();
            return;
         }
         
         // if a previously started worker is still running, skip this update. Should almost never happen.
         if(workerInProgress != null) return;
         
         // if the previous auto-save update failed, we force another update.
         boolean needUpdate = forceAutosaveUpdate;
         forceAutosaveUpdate = false;
         
         // de-register any figures that the user has closed since we last checked
         List<Integer> removeIDs = new ArrayList<>();
         for(Integer id : entries.keySet())
         {
            boolean found = false;
            for(Tab tab : openFigures) if(id.equals(tab.autoSaveID))
            {
               found = true;
               break;
            }
            
            if(!found) removeIDs.add(id);
         }
         for(Integer id : removeIDs) entries.remove(id);
         if(!removeIDs.isEmpty())
            needUpdate = true;
         
         // next check list of currently open figures for any changes in the source file paths
         for(Tab tab : openFigures)
         {
            Entry e = entries.get(tab.autoSaveID);
            if(e != null && tab.src != null && !Utilities.filesEqual(tab.src, e.origSrc))
            {
               e.origSrc = tab.src;
               needUpdate = true;
            }
         }
         
         // finally, check for a figure not yet registered with auto-saver or a figure due to be auto-saved. As soon as 
         // we find a modified figure that's due to be auto-saved, make an independent copy of that figure and stop 
         // checking. We DON'T want to copy a bunch of figures in one go.
         long now = System.currentTimeMillis();
         FGraphicModel figToSave = null; 
         Integer tgtID = null;
         for(Tab tab : openFigures)
         {
            if(tab.autoSaveID == null)
            {
               if(!tab.loading)
               {
                  tgtID = nextEntryID++;
                  Entry e = new Entry(tgtID, tab.src, now, tab.fig.getModifyCount());
                  entries.put(tgtID, e);
                  tab.autoSaveID = tgtID;
                  needUpdate = true;
                  
                  // we do NOT queue a newly registered figure for auto-save if it has not been modified yet!
                  if(tab.fig.isModified())
                  {
                     figToSave = FGraphicModel.copy(tab.fig);
                     break;
                  }
               }
            }
            else
            {
               Entry e = entries.get(tab.autoSaveID);
               if(e == null)
               {
                  // this should never happen, but just in case
                  tab.autoSaveID = null;
               }
               else
               {
                  long diff = tab.fig.getModifyCount() - e.modCount;
                  if(diff < 0 || diff >= 10 || (diff > 0 && (now - e.tStamp) > 120000L))
                  {
                     // make an independent copy of model that we can queue for saving in the background
                     figToSave = FGraphicModel.copy(tab.fig);
                     tgtID = tab.autoSaveID;
                     e.tStamp = now;
                     e.modCount = tab.fig.getModifyCount();
                     needUpdate = true;
                     break;
                  }
               }
            }
         }
         
         // start worker thread to perform the necessary update of the auto-saver state
         if(needUpdate)
         {
            HashMap<Integer, Entry> currState = new HashMap<>();
            for(Integer key : entries.keySet())
            {
               Entry e = entries.get(key);
               Entry eCopy = new Entry(e.id, e.origSrc, e.tStamp, e.modCount);
               currState.put(key, eCopy);
            }
            
            workerInProgress = new Job(currState, removeIDs, figToSave, tgtID);
            workerInProgress.execute();
         }
      } 
      
      /**
       * Initialize the figure composer's auto-save facility. The method first verifies that the auto-save directory
       * (a directory in the user's DataNav workspace) exists, and creates it otherwise. It then acquires an exclusive
       * lock on a dedicated flie in the directory, creating the file if it does not yet exist. This file lock mechanism
       * is intended to prevent multiple instances of the application from accessing the auto-save directory. If a
       * second running instance is unable to acquire the file lock, then its auto-saver facility will remain
       * uninitialized.
       * <p>Finally, the method checks the auto-save directory for the presence of any <i>FypML</i> figure files. If
       * any are found, it is assumed that a previous instance of the application crashed and recovery is needed -- see
       * {@link #isRestoreFromCrashNeeded()} </p>
       */
      @SuppressWarnings("ResultOfMethodCallIgnored")
      void initialize()
      {
         if(initialized) return;
         
         // ensure that the auto-save directory exists within user's DataNav workspace
         File dir = new File(FCWorkspace.getInstance().getWorkspaceHome(), AUTOSAVEDIR);
         if(!dir.isDirectory())
         {
            if(!dir.mkdir())
            {
               isFirstInit = false;
               return;
            }
         }
         
         // open and acquire exclusive lock on lock file in the auto-save directory. Might have to create the file.
         File lockFN = null;
         try
         {
            lockFN = new File(dir, AUTOSAVELOCKFILE);
            if(!lockFN.isFile())
            {
               JSONObject creationStamp = new JSONObject();
               creationStamp.put("created", (new Date()).toString());
               JSONUtilities.writeJSONObject(lockFN, creationStamp, false);
            }
            
            lockFile = new RandomAccessFile(lockFN, "rw");
            lock = lockFile.getChannel().tryLock();

         }
         catch(JSONException jse) { throw new NeverOccursException(jse); }
         catch(IOException ignored) {}
         
         if(lock == null)
         {
            isFirstInit = false;
            return;
         }
         
         // if there are any files in the auto-save directory other than the lock file, then we presume that a crash
         // occurred and that open figures should be restored from their last auto-saved state. However, a crash 
         // recovery is allowed only if the first attempt at initializing the auto-save facility was successful. If 
         // this is a subsequent attempt, then we simply delete the stale state.
         File[] files = dir.listFiles();
         if(isFirstInit)
         {
            if(files != null)
               for(File f : files) if(FCFileType.getFileType(f) == FCFileType.FYP)
                  {
                     restoreNeeded = true;
                     break;
                  }
         }
         if(!restoreNeeded)
         {
            if(files != null)
               for(File f : files) if(!Utilities.filesEqual(f, lockFN))
                  f.delete();
         }
         
         isFirstInit = false;
         initialized = true;
      }
      
      /**
       * Normally, when the auto-saver is stopped during application shutdown, all files in the user's auto-save 
       * directory are deleted -- since a normal shutdown implies that the user successfully saved all the figure files 
       * that mattered. Thus, if any files remain in the directory, it is likely that the previous running instance of
       * the application crashed for some reason. During initialization, the auto-saver checks for the presence of any
       * files in the auto-save directory; if there are any, this method returns true. The internal flag is reset after
       * a successful restore.
       * @return True if a restore is recommended; false otherwise.
       */
      boolean isRestoreFromCrashNeeded() { return(restoreNeeded); }
      
      /**
       * Spawn a special background task to restore the figure composer state in accordance with the contents of the
       * user's auto-save directory.
       * <p>When the figure composer's auto-save facility does not shutdown normally, the auto-saved figure files and
       * state information are not deleted from the dedicated auto-save directory. The next time the application runs
       * and the auto-save facility is initialized, the stale auto-save state is detected, and this method can be called
       * to spawn a one-time background task to restore the list of open figures to their last auto-saved state. This
       * task reads in the auto-saved figures, plus any additional figures that were "last open" according the user's
       * workspace settings. If any "last open" figure is among the auto-saved figures, the auto-saved state is taken as
       * the most recent version of that figure, and the figure is marked as modified to emphasize that it was restored
       * and needs to be saved explicitly to its original source file. Once the task has finished, back on the Swing 
       * event dispatch thread, it re-populates the figure composer's set of open "tabs" appropriately, raises a modal
       * dialog to alert the user to the crash recovery, and the resume normal auto-save updates.</p>
       */
      void startRestore()
      {
         if(initialized && restoreNeeded)
         {
            pause();
            workerInProgress = new Job();
            workerInProgress.execute();
         }
      }
      
      /**
       * Shutdown the auto-saver facility normally. Any auto-saver background task progress is cancelled, all files in
       * the auto-save directory are removed -- except for the lock file that mediates exclusive access to the 
       * directory --, and the exclusive file lock is then released.
       */
      @SuppressWarnings({"BusyWait", "ResultOfMethodCallIgnored"})
      void shutDown()
      {
         pause();
         if(timer != null)
         {
            timer.removeActionListener(this);
            timer = null;
         }
         
         // wait up to 2 seconds for any background task in progress to finish. Most tasks will take much less time. If
         // the task has not finished in that time, cancel it.
         long tStart = System.currentTimeMillis();
         while(workerInProgress != null && (System.currentTimeMillis()-tStart < 2000))
         {
            try{ Thread.sleep(20); } catch(InterruptedException ignored) {}
         }
         if(workerInProgress != null)
         {
            workerInProgress.cancel(false);
            workerInProgress = null;
         }
         
         // delete all files in auto-save directory except the lock file
         File dir = new File(FCWorkspace.getInstance().getWorkspaceHome(), AUTOSAVEDIR);
         File fLock = new File(dir, AUTOSAVELOCKFILE);
         if(dir.isDirectory())
         {
            File[] files = dir.listFiles();
            if(files != null)
               for(File f : files) if(!Utilities.filesEqual(f, fLock))
                  f.delete();
         }
         
         // release the lock file that guards access to auto-save directory; but don't delete it.
         try
         {
            if(lock != null) lock.release();
            if(lockFile != null) lockFile.close();
         }
         catch(IOException ignored) {}
         
         lock = null;
         lockFile = null;
         entries.clear();
      }
      
      /** Pause the auto-saver facility. This stops the Swing timer that triggers auto-saver checks. */
      void pause()
      {
         if(timer == null || !timer.isRunning()) return;
         timer.stop();
      }
      
      /** 
       * Resume the auto-saver facility. This starts the Swing timer that triggers auto-saver checks.
       * It creates the timer object if it has not yet been created -- so call this method to initiate the auto-saver
       * in the first place. Note that the auto-saver timer can run even if the auto-saver facility was not successfully
       * initialized. In this scenario, the auto-saver will keep trying to initialize itself each time the Swing timer
       * fires -- in the hopes that a future attempt will succeed.
       */
      void resume()
      {
         if(timer != null && timer.isRunning()) return;
         
         // if a crash recovery is still in progress, do NOT resume. Once that recovery task has finished, it will
         // invoke this method to resume normal auto-save updates.
         if(workerInProgress != null && workerInProgress.isRecoveryTask) return;

         if(timer == null) timer = new Timer(2000, this);
         timer.start();
      }

      /** Directory within user's workspace directory in which all auto-saved figure files are stored. */
      private final String AUTOSAVEDIR = "autosave";
      /** 
       * Name of lock file within auto-save directory. Opening and getting an exclusive lock on this file is how we
       * prevent a second instance of the auto-saver from interfering with the one that "locked down" the auto-save
       * directory first. The second instance will be disabled until it can acquire the file lock.
       */
      private final String AUTOSAVELOCKFILE = "autosave.lock";
      /** 
       * This file holds the auto-saver's last persisted state. Crash recovery relies on this file at next start-up to
       * offer the user a chance to recover any figures they may have been working on at the time of the crash.
       */
      private final String AUTOSAVESTATEFILE = "autosave.json";
      /** Base name prefix for auto-saved figure files. */
      private final String TMPFILEBASE = "tmp_";
      
      /** 
       * This flag is cleared after the first attempt -- successful or not -- at initializing the auto-saver. If
       * initialization succeeds on a future attempt, crash recovery is no longer an option because user may have made 
       * any number of changes to the set of open figures.
       */
      private boolean isFirstInit = true;
      
      /** Flag set once the auto-saver is initialized. Auto-saving won't start until initialization succeeds. */
      private boolean initialized = false;
      
      /** Flag set during initialization if any back-up files were found in auto-save directory. Reset after restore. */
      private boolean restoreNeeded = false;
      
      /** This "lock file" in the auto-save directory is opened and locked before accessing other files in directory. */
      private RandomAccessFile lockFile = null;
      
      /** 
       * Lock on auto-save directory's "lock file" is non-null when this auto-saver instance has exclusive access to 
       * the file (and "by agreement", the directory's content). This only ensures that two auto-saver instances running
       * in different JVMs do not step on each other.
       */
      private FileLock lock = null;
      
      /** 
       * The Swing timer that triggers periodic checks of the auto-save state. The auto-saver is disabled if this timer 
       * is not running.
       */
      private Timer timer = null;
      
      /** 
       * The auto-saver performs a check of the auto-save state for every 10 ticks of its Swing timer. During the other
       * nine ticks it checks the source files of all currently open figures to see if any file has been changed by an
       * external application. This counter keeps track of the "ticks".
       */
      private int ticksUntilCheck = 10;
      
      /** Current auto-saved entries, keyed by unique integer ID. */
      private final HashMap<Integer, Entry> entries = new HashMap<>();
      
      /** Integer ID to assign to the auto-saver's next new entry. */
      private int nextEntryID = 0;
      
      /** A background auto-save task in progess. The task object nullifies this reference when it has finished. */
      private Job workerInProgress = null;
      
      /** If a background auto-save task fails, it sets this flag to force another update on the next check. */
      private boolean forceAutosaveUpdate = false;
      
      /** Information maintained about each figure file that is being backed up by the auto-saver. */
      private class Entry
      {
         Entry(int id, File src, long t, long mod)
         {
            this.id = id;
            this.origSrc = src;
            this.tStamp = t;
            this.modCount = mod;
         }
         
         /** The unique ID that was assigned to the figure by the auto-saver. */
         private final int id;
         /** The original source file from which figure was read. Null for a figure that has never been saved. */
         private File origSrc;
         private long tStamp;
         /** The figure's modify count when it was saved. */
         private long modCount;
      }
      
      /**
       * Write the auto-saver's state to the file specified. For each auto-saved figure, only the assigned auto-save ID
       * and the full path to the figure's source file are persisted. This information is needed in order to restore
       * an auto-saved figure in the event of a system or program crash. A single mixed-content JSON array of the form
       * <i>[id1, path1, id2, path2, ...]</i> is written to the file, where <i>idN</i> is the integer auto-save ID and
       * <i>pathN</i> is a string containing the full file-system path for the <i>N</i>-th auto-saved figure.
       * 
       * @param state The auto-save state.
       * @param f The file to which the state is saved. An existing file will be overwritten.
       * @return True if successful, false if an IO error occurred while writing the file.
       */
      private boolean writeAutosaveStateFile(HashMap<Integer, Entry> state, File f)
      {
         JSONArray jsonAr = new JSONArray();
         if(state != null) for(Integer key : state.keySet())
         {
            Entry e = state.get(key);
            if(e != null)
            {
               jsonAr.put(key.intValue());
               jsonAr.put(e.origSrc == null ? "" : e.origSrc.getAbsolutePath());
            }
         }
         
         boolean ok = false;
         try 
         { 
            JSONUtilities.writeJSONArray(f, jsonAr, true);
            ok = true;
         }
         catch(Exception ignored) {}
         
         return(ok);
      }
      
      /**
       * The inverse of {@link #writeAutosaveStateFile(HashMap, File)}.
       * @param f The JSON-formatted file that contains the persisted auto-saver's state.
       * @return The auto-saver's state as read from the file, or null if operation failed. Each entry will contain the
       * auto-save ID and source path of each auto-saved figure. If a figure was never explicitly saved by the user,
       * or its path no longer exists, the path is set to null. 
       */
      private HashMap<Integer, Entry> readAutosaveStateFile(File f)
      {
         HashMap<Integer, Entry> state = new HashMap<>();
         boolean ok = false;
         try
         {
            JSONArray jsonAr = JSONUtilities.readJSONArray(f);
            if((jsonAr.length() % 2) != 0) throw new JSONException("Invalid content in auto-save state file");
            for(int i=0; i<jsonAr.length(); i+=2)
            {
               int id = jsonAr.getInt(i);
               String path = jsonAr.getString(i+1);
               File src = null;
               if(!path.isEmpty())
               {
                  src = new File(path);
                  if(!src.isFile()) src = null;
               }
               state.put(id, new Entry(id, src, 0, 0));
            }
            ok = true;
         }
         catch(Exception ignored) {}
         
         return(ok ? state : null);
      }
      
      /**
       * The background task object which performs an auto-save update task, or a recovery task. Only one background 
       * task should run at a time.
       * 
       * @author sruffner
       */
      private class Job extends SwingWorker<Object, Object>
      {
         /**
          * Construct a special background task that recovers the state of any auto-saved figures after a program or
          * system crash. 
          */
         Job()
         {
            this.isRecoveryTask = true;
         }
         
         /**
          * Construct an auto-save update task. This will auto-save one of the figures currently open in the figure 
          * composer component and persist the auto-saver's updated state to a dedicated file in the auto-save folder.
          * 
          * @param state The auto-saver's state, to be persisted to the dedicated auto-saver state file during the 
          * update task. This should never be null.
          * @param removeList The list of auto-save IDs that are no longer valid; back-up figure files for these IDs
          * should be deleted from the auto-save directory. Ignored if null or empty.
          * @param fgm The graphic model of a figure to be auto-saved. Ignored if null.
          * @param saveID The corresponding auto-save ID assigned to the open figure to be auto-saved. Ignored if null.
          */
         Job(HashMap<Integer, Entry> state, List<Integer> removeList, FGraphicModel fgm, Integer saveID)
         {
            this.isRecoveryTask = false;
            this.state = state;
            this.removeList = removeList;
            this.fgm = fgm;
            this.saveID = saveID;
         }
         
         @SuppressWarnings("ResultOfMethodCallIgnored")
         @Override protected Object doInBackground() throws Exception
         {
            // special case: recovering from a crash at application start-up
            if(isRecoveryTask)
            {
               doCrashRecovery();
               return(null);
            }
            
            // cannot auto-save a figure nor persist the auto-save state if auto-save directory does not exist
            File dir = new File(FCWorkspace.getInstance().getWorkspaceHome(), AUTOSAVEDIR);
            if(!dir.isDirectory()) return(null);
            
            // remove any figure files that are no longer registered with the auto-saver
            if(removeList != null) for(Integer id : removeList)
            {
               File f = new File(dir, TMPFILEBASE + id.toString() + ".fyp");
               if(f.isFile()) f.delete();
            }
            if(isCancelled()) return(null);
            
            // if there is a figure to be auto-saved, save it
            if(fgm  != null && saveID != null)
            {
               File f = new File(dir, TMPFILEBASE + saveID + ".fyp");
               figSaveOK = (null == FGModelSchemaConverter.toXML(fgm, f));
            }
            if(isCancelled()) return(null);
            
            // always persist the latest snapshot of the auto-saver's state.
            if(state != null)
            {
               File stateFile = new File(dir, AUTOSAVESTATEFILE);
               stateSaveOK = writeAutosaveStateFile(state, stateFile);
            }
            
            return(null);
         }

         @Override protected void done()
         {
            // whether cancelled or not, we must nullify reference to this task so a new update can be issued.
            workerInProgress = null;
            
            if(isCancelled()) return;
            
            if(isRecoveryTask)
            {
               // clear the crash recovery flag and resume normal auto-save updates
               restoreNeeded = false;
               resume();
               
               if(recoveredFigs.isEmpty()) return;
               
               // populate the figure composer tabs with the figures loaded during the recovery task. If the last open
               // figure file is among them, it will get the initial selection.
               openFigures.clear();  // get rid of the initial blank figure that was added when crash recovery started
               File fileLast = FCWorkspace.getInstance().getLastSelectedFigure();
               iCurrentFig = 0;
               for(int i=0; i<recoveredFigs.size(); i++)
               {
                  FGraphicModel fig = recoveredFigs.get(i);
                  File f = pathsForRecoveredFigs.get(i);
                  openFigures.add(new Tab(fig, f));
                  if(f.equals(fileLast)) iCurrentFig = i;
               }
               loadSelectedFigure();
               fireStateChanged();

               // if any auto-saved figures were recovered, alert user to the crash recovery. Note that this method
               // is invoked on the Swing event dispatch thread, so this should be safe...
               if(recoveryOK)
               {
                  JOptionPane.showMessageDialog(FigComposer.this, 
                        "<html>The application did not shutdown normally last time. More recent versions of<br/>" +
                        "one or more figures have been restored from the auto-save directory.</html>",
                        "Crash recovery", 
                        JOptionPane.WARNING_MESSAGE);
               }
               
               return;
            }
            
            // if update task failed to save a figure or failed to persist the latest auto-save state, force another 
            // update. To force a figure to be auto-saved again, we set the modification count for the entry to ensure 
            // that the next check will trigger an auto-save for that figure.
            if(!(figSaveOK && stateSaveOK))
            {
               forceAutosaveUpdate = true;
               if(!figSaveOK)
               {
                  Entry e = entries.get(saveID);
                  if(e != null) e.modCount = -10;
               }
            }
         }
         
         @SuppressWarnings("ResultOfMethodCallIgnored")
         private void doCrashRecovery()
         {
            // step 1: read the auto-save state file. If read fails, the "prior" auto-save state will be empty, so we'll
            // just end up restoring any figures that were last open (and that we can successfully read in)
            FCWorkspace ws = FCWorkspace.getInstance();
            HashMap<Integer, Entry> priorState = null;
            File dir = new File(ws.getWorkspaceHome(), AUTOSAVEDIR);
            File stateFile = new File(dir, AUTOSAVESTATEFILE);
            if(stateFile.isFile()) priorState = readAutosaveStateFile(stateFile);
            if(priorState == null) priorState = new HashMap<>();
            
            // step 2: check workspace for the list of "last open" figure files. Typically, most or all of these will be
            // among the set of auto-saved figures...
            List<File> filesOpen = ws.getLastOpenFigures();
            if(filesOpen.isEmpty())
            {
               File mruFile = ws.getMostRecentFile(false);
               if(mruFile != null) filesOpen.add(mruFile);
            }

            // step 3: load "last open" figures. If any figure was auto-saved, try to load its auto-saved version first.
            // If that fails, load it from the original file.
            recoveredFigs = new ArrayList<>();
            pathsForRecoveredFigs = new ArrayList<>();
            for(File f : filesOpen)
            {
               FGraphicModel fig = null;

               // read in auto-saved version, if it exists
               Integer autosaveID = null;
               for(Integer key : priorState.keySet())
               {
                  Entry e = priorState.get(key);
                  if(Utilities.filesEqual(f, e.origSrc))
                  {
                     autosaveID = key;
                     File autosaveFile = new File(dir, TMPFILEBASE + key.toString() + ".fyp");
                     fig = FGModelSchemaConverter.fromXML(autosaveFile, null);
                     break;
                  }
               }
               boolean wasRecovered = (fig != null);
               
               // remove entry from auto-save state if we found a match -- even if we could not read auto-saved version
               if(autosaveID != null) priorState.remove(autosaveID);
               
               // if auto-saved version not found or could not be read, load figure from original source file.
               if(!wasRecovered) fig = FGModelSchemaConverter.fromXML(f, null);

               // if the figure loaded successfully, add it to the list of loaded figures. Mark an auto-saved version as
               // modified to emphasize that it needs to be saved. Verify that directory containing original source path
               // still exists. If not, set path to null (like a brand-new figure).
               if(fig != null)
               {
                  recoveredFigs.add(fig);
                  if(f.getParentFile() == null || !f.getParentFile().isDirectory()) f = null;
                  pathsForRecoveredFigs.add(f);
                  
                  if(wasRecovered) recoveryOK = true;
                  if(wasRecovered || f == null) fig.setModified(true);
               }
            }
            
            // step 4: load any auto-saved figures that were not among the "last open" set.
            for(Integer key : priorState.keySet())
            {
               File autosaveFile = new File(dir, TMPFILEBASE + key.toString() + ".fyp");
               FGraphicModel fig = FGModelSchemaConverter.fromXML(autosaveFile, null);
               if(fig != null)
               {
                  recoveryOK = true;
                  fig.setModified(true);
                  File f = priorState.get(key).origSrc;
                  if(f != null && (f.getParentFile() == null || !f.getParentFile().isDirectory())) f = null;
                  
                  recoveredFigs.add(fig);
                  pathsForRecoveredFigs.add(f);
               }
            }
            
            // step 5: delete contents of auto-save directory (except the lock file)
            File fLock = new File(dir, AUTOSAVELOCKFILE);
            File[] files = dir.listFiles();
            if(files != null)
               for(File f : files) if(!Utilities.filesEqual(f, fLock))
                  f.delete();
         }
         
         /** Flag set for a crash recovery task, as opposed to a normal auto-save update task. */
         private final boolean isRecoveryTask;
         /** Flag set during crash recovery task IF at least one auto-saved figure was successfully recovered. */
         private boolean recoveryOK = false;
         /** List of figures loaded during a crash recovery task. */
         private List<FGraphicModel> recoveredFigs = null;
         /** Original source files for each figure loaded during a crash recovery task. Some entries could be null. */
         private List<File> pathsForRecoveredFigs = null;
         
         /** The auto-saver state to be persisted by this auto-save update task. */
         private HashMap<Integer, Entry> state;
         /** 
          * List of IDs representing auto-saved figures that have been de-registered. If not empty, the corresponding
          * back-up figure files in the auto-save directory will be deleted by this auto-save update task.
          */
         private List<Integer> removeList;
         /** Graphic model of the figure to be auto-saved. */
         private FGraphicModel fgm;
         /** Auto-save ID assigned to the figure to be auto-saved. */
         private Integer saveID;
         
         /** Was figure auto-save successful (or not required during this auto-save update)? */
         private boolean figSaveOK = true;
         /** Was auto-save state successfully persisted during this auto-save update? */
         private boolean stateSaveOK = true;
      }
   }
}
