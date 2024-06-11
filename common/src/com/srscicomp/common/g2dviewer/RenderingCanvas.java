package com.srscicomp.common.g2dviewer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import com.srscicomp.common.ui.GUIUtilities;
import com.srscicomp.common.ui.JPreferredSizePanel;
import com.srscicomp.common.util.Utilities;


/**
 * <code>RenderingCanvas</code> is a custom-painted component that implements a backbuffered canvas-like view for 
 * rendering Java2D graphics encapsulated by the <code>RootRenderable</code> interface. It is a package-private class 
 * designed solely to serve as the canvas component for the public <code>Graph2DViewer</code>. For a detailed 
 * description, see the class header for <code>Graph2DViewer</code>.
 * 
 * <p>On coordinate systems: The <em>logical</em> coordinate system associated with the <code>RenderingCanvas</code> is 
 * the coordinate system of the displayed graphic. By convention as defined in the <code>RootRenderable</code> 
 * interface, this coordinate system has its origin at the bottom-left corner of the graphic, with x-axis increasing 
 * rightward, y-axis increasing upward, and units in milli-inches. The <code>device</code> coordinate system refers to 
 * the pixel-based coordinate system of the canvas itself: origin at top-left corner of the canvas, x-axis increasing 
 * rightward, y-axis increasing downward, and units in pixels. Transforming a point from device to logical coordinates 
 * and vice versa is complicated by the fact that <code>RenderingCanvas</code> supports displaying the graphic in print 
 * preview mode, zooming in/out on the displayed graphic, and panning the canvas over the zoomed graphic (when it is 
 * larger than the canvas window). A "scale to fit" view is also supported; in this mode, zooming and panning are 
 * disabled, and the graphic is scaled to fit within the canvas window while preserving the graphic's aspect ratio.</p>
 * 
 * <p>Supported mouse-activated interactions to-date (the {MenuShortcutKey} refers to the platform-specific menu 
 * shorcut modifier; this is Ctrl in Windows and the Option key on Mac OSX):
 * <ul>
 *    <li>SHIFT-click using mouse button 1 (usually the left button): Zoom in on displayed graphic (scale *= 2, up to 
 *    the supported maximum) and recenter the graphic at the logical location that was "clicked on".</li>
 *    <li>SHIFT-click using a different mouse button: Zoom out on displayed graphic (scale /= 2, down to the supported
 *    minimum) and recenter the graphic at the logical location that was "clicked on".</li>
 *    <li>Click using a mouse button other than #1 (right-click typically): Recenters displayed graphic at the logical 
 *    location "clicked on" by adjusting the current pan position. Note that there is no context menu associated with 
 *    the <code>RenderingCanvas</code></li>
 *    <li>Drag the focus element: If mouse button #1 is held down within the focus highlight of the current focus 
 *    element, and the current <code>RenderableModel</code> supports moving the focus element, the canvas will capture 
 *    a translucent copy of the graphic under the focus highlight, and this image will follow the mouse as it is dragged 
 *    across the canvas (but not outside it!). This effectively animates moving the focus element. When the mouse 
 *    button is released, the graphic is re-rendered with the focus element at the new location. The SHIFT key must 
 *    <strong>not</strong> be down to initiate the pan; the state of other modifier keys is ignored.</li>
 *    <li>Pan-by-drag: If a mouse button other than #1 is held down, an active panning operation is initiated. The 
 *    current contents of the canvas pan with the mouse as it is dragged. When the mouse button is released, the 
 *    current pan position is updated by the net change in mouse position, and the contents of the canvas are 
 *    re-rendered accordingly. The SHIFT key must <strong>not</strong> be down to initiate the pan; the state of other 
 *    modifier keys is ignored.</li>
 * </ul>
 * </p>
 * 
 * @see Graph2DViewer, RenderableModel, RenderableModelViewer, Renderable, RootRenderable, RenderTask
 * @author 	sruffner
 */
final class RenderingCanvas extends JComponent implements MouseInputListener
{
   private static final long serialVersionUID = 1L;

   /**
    * The model currently installed in this <code>RenderingCanvas</code>.
    */
   private RenderableModel model = null;

   /**
    * The top-level graphic object currently drawn on this <code>RenderingCanvas</code>.
    */
   private RootRenderable root = null;

   /**
    * Minimum size for either dimension of the <code>RenderingCanvas</code>.
    */
   private final static int MINSIZE = 20;

   /**
    * This flag is set if the <code>RenderingCanvas</code> is configured to display a focus highlight and support 
    * mouse interactions. It is set at construction time and cannot be changed.
    */
   private boolean isInteractive = false;

   /**
    * Construct an instance of the <code>RenderingCanvas</code>, with no <code>RootRenderable</code> installed. To 
    * display graphics, install a <code>RootRenderable</code> object by calling <code>updateRendering()</code>.
    * 
    * <p>The constructor creates and starts the background renderer thread that is associated solely with this canvas 
    * instance.</p>
    * 
    * @param supportMouseInteractions If <code>true</code>, the canvas is configured to support a number of different 
    * mouse-related interactions and features. See class header for a description.
    */
   public RenderingCanvas(boolean supportMouseInteractions)
   {
      setMinimumSize(new Dimension(MINSIZE,MINSIZE));
      setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
      setPreferredSize(getMinimumSize());
      setBackground(Color.WHITE);

      isInteractive = supportMouseInteractions;
      if(isInteractive)
      {
         // for tracking mouse cursor and implementing mouse-related interactors
         addMouseListener(this);
         addMouseMotionListener(this);

         // so panning continues when we drag mouse outside window
         setAutoscrolls(true);
      }

      setPageFormat(null);
      
      renderer = new Renderer();
      renderer.setDaemon(true);
      renderer.start();
   }

   /**
    * Call this method to install a different <code>RenderableModel</code> on the canvas. The canvas requires access to 
    * the model so it can get the current <code>RootRenderable</code> and current <code>Focusable</code> graphic nodes, 
    * and so that it can inform the model when certain canvas-user interactions occur: 
    * <ul>
    *    <li>Clicking on the canvas might trigger a change in the node with the display focus, if the model 
    *    supports the feature.</li>
    *    <li>The user can interactively drag the focus node to a new position, if the model supports it. When the drag 
    *    is done, the canvas must tell the model so the focus node is updated appropriately.</li>
    * </ul>
    * 
    * @param rm The <code>RenderableModel</code> to be installed. Any reference to the previous model is lost. If 
    * <code>null</code>, then nothing will be rendered on the canvas.
    */
   void setRenderableModel(RenderableModel rm)
   {
      model = rm;
      updateRendering(null);
      updateFocusHighlight();
   }
   /**
    * Call this method to terminate the background renderer thread that handles all rendering for this canvas. The 
    * canvas is unusable at this point, so this is provided as a way to clean up prior to getting rid of the canvas
    * (eg, when the application exits).
    *
    * <p>Note that the renderer thread, is marked as a daemon, so it may not be necessary to call this function if 
    * the application is exiting anyway.</p>
    */
   void releaseRenderingResources()
   {
      listeners.clear();
      
      model = null;
      root = null;
      if(renderer != null)
      {
         renderer.die();
         renderer = null;
      }
   }

   /**
    * Get an instance of the Java2D graphics context for this <code>RenderingCanvas</code>, transformed so that logical 
    * units are in milli-inches, and taking into account the current zoom factor. The caller should not use this for 
    * drawing on the canvas, and is responsible for disposing of the graphics context when finished with it.
    * 
    * @return A <code>Graphics2D</code> context for the canvas, as described.
    */
   Graphics2D getRenderingGraphics()
   {
      Graphics2D g2d = (Graphics2D) getGraphics();
      if(g2d == null) return(null);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      double milliInToScaledPix = getCurrentScale() * getPixelsPerInch() / 1000.0;
      g2d.scale(milliInToScaledPix, -milliInToScaledPix);
      return(g2d);
   }


   //
   // CanvasListener maintenance
   //
   private final List<CanvasListener> listeners =  new ArrayList<CanvasListener>();
   
   /**
    * Add a <code>CanvasListener</code> to the canvas's listener list. All such listeners are notified of a variety of 
    * events on the <code>RenderingCanvas</code>:
    * <ul>
    *    <li>When a render job starts, stops (prematurely), and completes, as well as "in-progress" updates that are 
    *    intended to support the display of rendering progress on the UI.</li>
    *    <li>When the mouse enters, exits, or moves within the canvas.</li>
    *    <li>When the logical location or size of the canvas viewport changes (WRT the coordinate system of the 
    *    graphics displayed).</li>
    * </ul>
    * <p>There is NO guarantee that listeners will be notified in the same order that they were added to the listener
    * list.</p>
    * 
    * <p>The listener list implementation is NOT thread-safe. Therefore, invoke this method only on the Swing event 
    * thread, or during application startup/shutdown. <code>RenderingCanvas</code> notifies its registered listeners 
    * only on the Swing event thread.</p>
    * 
    * @param listener The <code>CanvasListener</code> to be appended to the canvas's current listener list. If argument 
    * is <code>null</code> OR is already in the listener list, the method does nothing.
    */
   void addListener(CanvasListener listener)
   {
      if(listener != null && !listeners.contains(listener) ) 
         listeners.add(listener);
   }

   /**
    * Delete the specified <code>CanvasListener</code> from this <code>RenderingCanvas</code>'s listener list.
    * 
    * <p>The listener list implementation is NOT thread-safe. Therefore, invoke this method only on the Swing event 
    * thread, or during application startup/shutdown. <code>RenderingCanvas</code> notifies its registered listeners 
    * only on the Swing event thread.</p>
    * 
    * @param listener The <code>CanvasListener</code> to be removed from the canvas's current listener list. If argument 
    * is <code>null</code> or is not found in the listener list, the method does nothing.
    */
   void removeListener(CanvasListener listener)
   {
      if(listener != null) listeners.remove(listener);
   }

   /**
    * Enumeration of the different kinds of events fired by <code>RenderingCanvas</code>.
    */
   private enum CanvasEventType {STARTED, INPROGRESS, COMPLETED, STOPPED, CURSORMOVED, VIEWPORTCHANGED}

    /**
    * Notifies all registered <code>CanvasListener</code>s of an event that has occurred on this 
    * <code>RenderingCanvas</code>, including events during the processing of a job on the background renderer thread. 
    * Supported events: 
    * <ul>
    *    <li><code>CanvasEventType.STARTED</code>: A rendering job has just started.</li>
    *    <li><code>CanvasEventType.INPROGRESS</code>: Progess update for an ongoing job.</li>
    *    <li><code>CanvasEventType.COMPLETED</code>: Rendering job finished successfully.</li>
    *    <li><code>CanvasEventType.STOPPED</code>: Rendering job aborted prematurely.</li>
    *    <li><code>CanvasEventType.CURSORMOVED</code>: Mouse has moved within or exited canvas.</li>
    *    <li><code>CanvasEventType.VIEWPORTCHANGED</code>: Canvas viewport logical size/pos changed.</li>
    * </ul>
    * 
    * <p>Regardless what thread calls this method, notifications are delivered on the Swing event thread.</p>
    * 
    * @param type The event type.
    * @param t Event timestamp - elapsed time since the rendering job started. Ignored for <code>STARTED, CURSORMOVED, 
    * VIEWPORTCHANGED</code>.
    * @param pctDone Estimated percentage of job done. Ignored for <code>STARTED, COMPLETED, CURSORMOVED,  
    * VIEWPORTCHANGED</code>.
    */
   private void fireCanvasEvent(CanvasEventType type, long t, double pctDone)
   {
      long tStamp = (type == CanvasEventType.STARTED) ? 0 : t;
      double pct = (type == CanvasEventType.STARTED) ? 0 : ((type == CanvasEventType.COMPLETED) ? 100 : pctDone);
      final CanvasEvent event;
      if(type==CanvasEventType.CURSORMOVED)
      {
         double x = Double.NaN;
         double y = Double.NaN;
         if(currentCursorPos != null)
         {
            // in print preview mode, logical origin for CanvasListeners should be BL corner of printed page
            x = currentCursorPos.getX();
            y = currentCursorPos.getY();
            if(printPreviewOn && root != null)
            {
               Point2D printLoc = root.getPrintLocationMI();
               x += printableBL_MI.getX() + printLoc.getX();
               y += printableBL_MI.getY() + printLoc.getY();
            }
         }
         event = CanvasEvent.createCursorEvent(x, y);
      }
      else if(type == CanvasEventType.VIEWPORTCHANGED)
      {
         Rectangle2D rVuLog = (Rectangle2D) rViewLogical.clone();
         if(printPreviewOn && root != null)
         {
            // in print preview mode, logical origin for CanvasListeners should be BL corner of printed page
            double x = rVuLog.getX();
            double y = rVuLog.getY();
            Point2D printLoc = root.getPrintLocationMI();
            x += printableBL_MI.getX() + printLoc.getX();
            y += printableBL_MI.getY() + printLoc.getY();
            rVuLog.setFrame(x, y, rVuLog.getWidth(), rVuLog.getHeight());
         }
         event = CanvasEvent.createViewportChangedEvent(rVuLog);
      }
      else
         event = CanvasEvent.createRenderProgressEvent(tStamp, pct);
      final CanvasEventType eventType = type;

      SwingUtilities.invokeLater( new Runnable() {
         public void run()
         {
            switch(eventType)
            {
               case STARTED :
                  for(CanvasListener l : listeners) 
                     l.renderingStarted(event);
                  break;
               case INPROGRESS :
                  for(CanvasListener l : listeners) 
                     l.renderingInProgress(event);
                  break;
               case COMPLETED :
                  for(CanvasListener l : listeners) 
                     l.renderingCompleted(event);
                  break;
               case STOPPED :
                  for(CanvasListener l : listeners) 
                     l.renderingStopped(event);
                  break;
               case CURSORMOVED :
                  for(CanvasListener l : listeners) 
                     l.cursorMoved(event);
                  break;
               case VIEWPORTCHANGED :
                  for(CanvasListener l : listeners) 
                     l.viewportChanged(event);
                  break;
            }
         }
      });
   }


   //
   // The canvas viewport
   //
   
   /**
    * The canvas viewport rectangle last time we checked, in the coordinate system of the displayed graphic (possibly 
    * in print mode), taking into account the canvas's current rendering transform.
    */
   private Rectangle2D rViewLogical = new Rectangle2D.Double();

   /**
    * The canvas window acts as a "viewport" on the displayed graphic. This method calculates the bottom-left corner 
    * and size of the viewport in logical coordinates, ie, in the coordinate system of the displayed graphic, taking 
    * into account print preview mode and the current zoom and pan transform. 
    */
   private void updateCanvasViewportSizeAndLocation()
   {
      // get the inverse of the rendering transform, which transforms device to logical coords
      AffineTransform invAt = null;
      try { invAt = getRenderingTransform().createInverse(); }
      catch( NoninvertibleTransformException ne ) {}
      if(invAt == null) invAt = new AffineTransform();
      
      rViewLogical.setRect(0, 0, getWidth(), getHeight());
      rViewLogical = invAt.createTransformedShape(rViewLogical).getBounds2D();
   }

   /**
    * Transform a canvas location in canvas device pixels to the logical coordinate system of the graphic displayed, 
    * taking into account print preview mode and the current scale & pan transform.
    * 
    * @param x X-coordinate of a point on canvas in device pixels (origin at canvas top-left corner, x-axis increasing 
    * rightward, y-axis increasing downward). 
    * @param y Y-coordinate of the canvas location.
    * @param ptLogical If not <code>null</code>, this point is modified to contain the same location expressed in 
    * milli-inches in the coordinate system of the zoomed and panned graphic. If it is <code>null</code>, a new 
    * <code>Point</code> object is allocated on the heap. Regardless, the modified point is the returned value of 
    * the method. 
    * @return The canvas location (x,y) expressed in the logical coordinate system of the displayed graphic.
    */
   Point2D deviceToLogical(int x, int y, Point2D ptLogical)
   {
      Point2D ptRet = ptLogical;
      if(ptRet == null) ptRet = new Point2D.Double();

      // use inverse of the rendering transform, which transforms device to logical coords
      ptRet.setLocation(x, y);
      try { getRenderingTransform().inverseTransform(ptRet, ptRet); }
      catch( NoninvertibleTransformException ne ) {}

      return(ptRet);
   }

   /**
    * Transform a point expressed in the logical coordinate system of the graphic displayed to the device coordinate
    * system of this rendering canvas, given the current preview mode and scale/pan transform.
    * @param p The point in logical coordinates. Upon return, it contains the point in device coordinates. If null,
    * method takes no action.
    */
   void logicalToDevice(Point2D p)
   {
      if(p==null) return;
      getRenderingTransform().transform(p, p);
   }
   
   /**
    * Transform a rectangle expressed in the logical coordinate system of the graphic displayed to the device coordinate 
    * system of this <code>RenderingCanvas</code>, given the current preview mode and scale/pan transform.
    * 
    * @param rLog The rectangle in logical coordinates. If null, method returns null.
    * @return The rectangle expressed in canvas device coordinates IAW the canvas's current state.
    */
   Rectangle2D logicalToDevice(Rectangle2D rLog)
   {
      if(rLog == null) return(null);

      return(getRenderingTransform().createTransformedShape(rLog).getBounds2D());
   }
   
   /**
    * Transform a shape expressed in the logical coordinate system of the graphic displayed to the device coordinate
    * system of this <code>RenderingCanvas</code>, given the current preview mode and scale/pan transform.
    * 
    * @param shapeLog The shape in logical coordinates. If null, method returns null.
    * @return The shape expressed in canvas device coordinates IAW the canvas's current state.
    */
   Shape logicalToDevice(Shape shapeLog)
   {
      if(shapeLog == null) return(null);
      
      PathIterator pi = shapeLog.getPathIterator(getRenderingTransform());
      GeneralPath gp = new GeneralPath();
      gp.append(pi, false);
      return(gp);
   }
   
   /**
    * Fires a <code>CanvasEventType.VIEWPORTCHANGED</code> event if either the bottom-left corner or size of the canvas 
    * viewport has changed.
    */
   private void fireViewportChangedEventIfNecessary()
   {
      double oldW = rViewLogical.getWidth();
      double oldH = rViewLogical.getHeight();
      double oldX = rViewLogical.getX();
      double oldY = rViewLogical.getY();

      updateCanvasViewportSizeAndLocation();

      if(oldW != rViewLogical.getWidth() || oldH != rViewLogical.getHeight() || 
            oldX != rViewLogical.getX() || oldY != rViewLogical.getY())
         fireCanvasEvent(CanvasEventType.VIEWPORTCHANGED, 0, 0);
   }

   
   //
   // Scaling and panning.  "Scale to fit" mode.
   //

   /**
    * If this flag is set, normal scaling and panning are disabled, and the displayed graphic -- whether in print 
    * preview or normal preview mode -- is scaled so that it just fits within the canvas window, while preserving the 
    * picture's aspect ratio.
    */
   private boolean scaleToFitOn = false;

   /**
    * Is this <code>RenderingCanvas</code> currently scaling the displayed graphic to just fit within its bounds? In 
    * this case, the user controls the size of the graphic by resizing the canvas (ie, by resizing the application 
    * window within which the canvas is embedded); normal zoom & pan features are disabled.
    * 
    * @return <code>True</code> iff "scale to fit" mode is on.
    */
   boolean isScaleToFitOn()
   {
      return(scaleToFitOn);
   }

   /**
    * Turn this <code>RenderingCanvas</code>'s "scale to fit" mode on or off. The canvas is automatically 
    * re-rendered to reflect the change in state.
    * 
    * @param on <code>True</code> to turn on "scale to fit" mode; <code>false</code> to turn it off.
    */
   void setScaleToFitOn(boolean on)
   {
      if(scaleToFitOn == on) return;
      scaleToFitOn = on;

      resetViewAndRender();
   }

   /**
    * Reset the current scale factor and panning position for the canvas. Method has no effect if "scale to fit" mode
    * is on, or if the zoom and pan transform is already reset. If the transform is indeed reset, the canvas is
    * automatically re-rendered to reflect the change.
    */
   void resetZoomAndPan() { if(canResetView()) resetViewAndRender(); }
   
   /**
    * Zoom in on the specified rectangle within the canvas, magnifying and panning the rendered graphic so that -- to 
    * the extent possible while still maintaining the graphic's aspect ratio and the canvas's maximum supported scale 
    * factor -- the portion inside the rectangle fills the canvas viewport. If the scale-to-fit mode is engaged, it will
    * necessarily be turned off in order to perform the zoom and pan transformation required.
    * 
    * @param zoomR The rectangle defining the portion of the canvas that should be magnified and centered in place. In
    * device pixels, origin at canvas's top-left corner, Y-axis increasing downward.
    */
   void zoomIn(Rectangle zoomR)
   {
      if(root == null || zoomR == null || zoomR.width <= 0 || zoomR.height <= 0) return;
      
      // get current scale factor for graphic, then be sure that scale to fit is OFF.
      double oldScale = getCurrentScale();
      scaleToFitOn = false;
      
      // compute the new scale factor so that we zoom in on specified rectangle
      double scaleX = oldScale * ((double) getWidth()) / ((double) zoomR.width);
      double scaleY = oldScale * ((double) getHeight()) / ((double) zoomR.height);

      currentScale = Math.min(Math.min(scaleX, scaleY), MAXSCALE);
      
      // adjust canvas panning position so that -- if possible -- graphic will be centered at the rectangle's center,
      // AFTER rescaling
      double oldPanX = currDevPanPos.getX();
      double oldPanY = currDevPanPos.getY();
      double adjust = currentScale/oldScale;
      double xAdj = adjust * (currDevPanPos.getX() + zoomR.getCenterX());
      double yAdj = adjust * (currDevPanPos.getY() + zoomR.getCenterY());
      currDevPanPos.setLocation(xAdj - ((double)getWidth())/2, yAdj - ((double)getHeight())/2);
      fixPanPosition();

      // update canvas state to reflect the changes
      if(oldScale != currentScale || oldPanX != currDevPanPos.getX() || oldPanY != currDevPanPos.getY())
      {
         postRenderJob(null);
         updateToolbarState();
         fireViewportChangedEventIfNecessary();
      }
   }

   /**
    * To the extent possible, pan the installed graphic by the specified amounts within the canvas viewport. No action
    * is taken if no graphic model is installed, or if the scale-to-fit mode is enabled.
    * 
    * @param dx Horizontal pan adjustment in device pixels. Negative to pan graphic rightward WRT canvas.
    * @param dy Vertical pan adjustment in device pixels. Negative to pan graphic downward WRT canvas.
    */
   void panBy(double dx, double dy)
   {
      // panning disabled, or zero pan deltas
      if((root == null) || scaleToFitOn || (dx==0 && dy==0)) return;

      // adjust pan position so that canvas viewport is centered about (x,y), if we can
      double oldX = currDevPanPos.getX();
      double oldY = currDevPanPos.getY();
      currDevPanPos.setLocation(oldX + dx, oldY + dy);
      fixPanPosition();

      // if pan position did indeed change, update canvas state to reflect it
      if(oldX != currDevPanPos.getX() || oldY != currDevPanPos.getY())
      {
         postRenderJob(null);
         updateToolbarState();
         fireViewportChangedEventIfNecessary();
      }
   }

   /**
    * The current scale factor. The displayed graphic is scaled in both dimensions by this number.
    */
   private double currentScale = 1;

   /**
    * The minimum supported scale factor.
    */
   private final static double MINSCALE = 0.05;

   /**
    * The maximum supported scale factor.
    */
   private final static int MAXSCALE = 100;

   /**
    * The current device panning position. The coordinates give the displacement of the UL corner of the canvas window 
    * from the UL corner of the displayed graphic, taking into account print preview mode and the current scale factor.
    * Units are scaled pixels, with x-coordinate increasing rightward and y-coordinate increasing downward.
    */
   private final Point2D currDevPanPos = new Point2D.Double();

   /**
    * Get the scale factor by which the current displayed graphic is multiplied.
    * 
    * <p>In normal "zoom" mode, this method returns the current scale factor.  In "scale to fit" mode, that scale factor 
    * is irrelevant, and the method computes a scale factor that will ensure the displayed graphic fits within the 
    * current bounds of the canvas window, while preserving the picture's aspect ratio.</p>
    * 
    * @return Current scale factor for graphic displayed on this <code>RenderingCanvas</code>.
    */
   private double getCurrentScale()
   {
      if(scaleToFitOn)
      {
         double scale = 1;
         if(root != null)
         {
            double canvasW = getWidth();
            double canvasH = getHeight(); 
            if(canvasW < MINSIZE) canvasW = MINSIZE;
            if(canvasH < MINSIZE) canvasH = MINSIZE;
    
            double graphicW = (pixelsPerInch /1000.0) * (isPrintPreviewOn() ? pageWidthMI : root.getWidthMI());
            double graphicH = (pixelsPerInch /1000.0) * (isPrintPreviewOn() ? pageHeightMI : root.getHeightMI());

            double scaleX = canvasW/graphicW;
            double scaleY = canvasH/graphicH;
            scale = (scaleX < scaleY) ? scaleX : scaleY;
         }
         return(scale);
      }
      else
         return(currentScale);
   }

   /**
    * Can the <code>RenderingCanvas</code> further zoom in on the displayed graphic?
    * 
    * @return <code>True</code> iff a graphic is currently displayed, "scale to fit" mode is disabled, and the current 
    * scale factor is not pegged at its maximum allowed value.
    */
   private boolean canZoomIn()
   {
      return((root != null) && (!scaleToFitOn) && currentScale < MAXSCALE);
   }
   
   /**
    * Can the <code>RenderingCanvas</code> further zoom out on the displayed graphic?
    * 
    * @return <code>True</code> iff a graphic is currently displayed, "scale to fit" mode is disabled, and the current 
    * scale factor is not pegged at its minimum allowed value.
    */
   private boolean canZoomOut()
   {
      return((root != null) && (!scaleToFitOn) && currentScale > MINSCALE);
   }

   /**
    * Can the <code>RenderingCanvas</code> viewport pan in any direction over the displayed graphic?
    * 
    * @return <code>True</code> iff a graphic is currently displayed, "scale to fit" mode is disabled, and at least one 
    * dimension of the canvas is smaller than the corresponding dimension of the scaled graphic.
    */
   boolean canPan()
   {
      if(root == null || scaleToFitOn) return(false);
      
      double milliInToScaledPix = getCurrentScale() * pixelsPerInch / 1000.0;
      double graphicW = milliInToScaledPix * (isPrintPreviewOn() ? pageWidthMI : root.getWidthMI());
      double graphicH = milliInToScaledPix * (isPrintPreviewOn() ? pageHeightMI : root.getHeightMI());
      return(getWidth() < graphicW || getHeight() < graphicH);
   }

   /**
    * Can the <code>RenderingCanvas</code> viewport be reset to its nominal state (no scaling, zero pan offsets)?
    * 
    * @return <code>True</code> iff "scale to fit" mode is disabled, and either the current scale factor is not unity 
    * or the current viewport pan position is not (0,0).
    */
   private boolean canResetView()
   {
      return((!scaleToFitOn) && (currentScale != 1 || currDevPanPos.getX() != 0 || currDevPanPos.getY() != 0));
   }

   /**
    * Ensure that the current device panning position is correct, given the current state of the canvas. 
    * 
    * <p>This method should be invoked whenever the canvas is resized, the current scale factor changes, the view
    * mode changes, etcetera.</p>
     */
   private void fixPanPosition()
   {
      // if no graphic displayed or we're scaling to fit, then pan position is (0,0)
      if((root == null) || scaleToFitOn)
      {
         currDevPanPos.setLocation(0,0);
         return;
      }

      double milliInToScaledPix = getCurrentScale() * pixelsPerInch / 1000.0;
      double graphicW = milliInToScaledPix * (isPrintPreviewOn() ? pageWidthMI : root.getWidthMI());
      double graphicH = milliInToScaledPix * (isPrintPreviewOn() ? pageHeightMI : root.getHeightMI());
      double canvasW = getWidth();
      double canvasH = getHeight();
      double panX = currDevPanPos.getX();
      double panY = currDevPanPos.getY();

      if(canvasH >= graphicH) panY = 0;
      else if(panY > graphicH - canvasH) panY = graphicH - canvasH;
      else if(panY < 0) panY = 0;

      if(canvasW >= graphicW) panX = 0;
      else if(panX > graphicW - canvasW) panX = graphicW - canvasW;
      else if(panX < 0) panX = 0;

      currDevPanPos.setLocation(panX, panY);
   }

   /**
    * If possible, adjust the current pan position so that the logical location corresponding to the specified 
    * device location is centered on the canvas in its current state. The canvas is re-rendered if the pan position is 
    * changed.
    * 
    * @param x The x-coordinate of a location on the canvas, in canvas device pixels. 
    * @param y The y-coordinate. 
    */
   private void panToRecenterAt(int x, int y)
   {
      // if there's no graphic or we're scaling to fit, then there's nothing to pan!
      if((root == null) || scaleToFitOn) return;

      // adjust pan position so that canvas viewport is centered about (x,y), if we can
      double oldX = currDevPanPos.getX();
      double oldY = currDevPanPos.getY();
      currDevPanPos.setLocation(oldX + x - ((double)getWidth())/2, oldY + y - ((double)getHeight())/2);
      fixPanPosition();

      // if pan position did indeed change, update canvas state to reflect it
      if(oldX != currDevPanPos.getX() || oldY != currDevPanPos.getY())
      {
         postRenderJob(null);
         updateToolbarState();
         fireViewportChangedEventIfNecessary();
      }
   }

   /**
    * If possible, zoom in or out on the displayed graphic so that the logical location corresponding to the specified 
    * canvas device location is centered on the canvas after zooming. The canvas is re-rendered if either the zoom or 
    * pan state is indeed changed.
    * 
    * @param x The x-coordinate of a location on the canvas in canvas device coordinates, prior to rescaling. 
    * @param y The y-coordinate.
    * @param magnify If <code>true</code>, zoom in on graphic (scale x 2); else zoom out (scale / 2). Zooming may not be 
    * possible if the current scale factor is already up against one of the limits of its range! In this case, the 
    * method will still -- if possible -- pan the canvas so that the specified point is centered in the view.
    */
   void zoomAndPanTo(int x, int y, boolean magnify)
   {
      // if there's no graphic or we're scaling to fit window, then this operation is not allowed!
      if((root == null) || scaleToFitOn) return;

      // compute factor by which current scale factor will be changed. Normally it's 2 or 0.5, but it could be something 
      // else if we run up against our scale factor limits.
      double oldScale = currentScale;
      if(magnify && currentScale < MAXSCALE)
      {
         currentScale *= 2;
         if(currentScale > MAXSCALE) currentScale = MAXSCALE;
      }
      else if(!magnify && currentScale > MINSCALE)
      {
         currentScale /= 2;
         if(currentScale < MINSCALE) currentScale = MINSCALE;
      }

      // adjust canvas panning position so that -- if possible -- graphic will be centered about the point that was 
      // clicked, AFTER rescaling.
      double oldPanX = currDevPanPos.getX();
      double oldPanY = currDevPanPos.getY();
      double adjust = currentScale/oldScale;
      double xAdj = adjust * (currDevPanPos.getX() + x);
      double yAdj = adjust * (currDevPanPos.getY() + y);
      currDevPanPos.setLocation(xAdj - ((double)getWidth())/2, yAdj - ((double)getHeight())/2);
      fixPanPosition();

      // update canvas state to reflect the changes
      if(oldScale != currentScale || oldPanX != currDevPanPos.getX() || oldPanY != currDevPanPos.getY())
      {
         postRenderJob(null);
         updateToolbarState();
         fireViewportChangedEventIfNecessary();
      }
   }

   /**
    * Reset the current scale factor and the viewport pan position so that displayed graphic is not scaled and its UL 
    * corner coincides with UL corner of the canvas window -- but do so without posting a render task or repainting the 
    * canvas.
    * 
    * @see RenderingCanvas#resetViewAndRender()
    */
   private void resetViewWithoutRender()
   {
      currentScale = 1;
      currDevPanPos.setLocation(0,0);
      updateToolbarState();
   }

   /**
    * Reset the current scale factor and viewport pan position, then re-render the graphic. Note that a rendering job is 
    * <em>always</em> posted by this method.
    */
   private void resetViewAndRender()
   {
      resetViewWithoutRender();
      postRenderJob(null);
      fireViewportChangedEventIfNecessary();
   }

   
   //
   // Print preview mode
   //

   /**
    * This flag is set if the <code>RenderingCanvas</code> is in "print preview" mode.
    */
   private boolean printPreviewOn = false;

   /**
    * Is the print preview mode currently on for this <code>Graph2DViewer</code>?
    * 
    * @return <code>True</code> iff print preview mode is on.
    */
   boolean isPrintPreviewOn()
   {
      return(printPreviewOn);
   }

   /**
    * Turn this <code>RenderingCanvas</code>'s print preview mode on or off. The canvas is automatically 
    * refreshed, and the current zoom & pan transform is reset.
    * 
    * @param on <code>True</code> to turn on print preview mode; <code>false</code> to turn it off.
    */
   void setPrintPreviewOn(boolean on)
   {
      if(printPreviewOn == on) return;
      printPreviewOn = on;

      resetViewAndRender();
   }

   /**
    * The page format for the canvas's "print preview" mode.
    */
   private PageFormat pgFmt = new PageFormat();

   /**
    * Current width of oriented page IAW canvas's current page format, in milli-in. Pre-calculated for ready access.
    */
   private double pageWidthMI = 8500;

   /**
    * Current height of oriented page IAW canvas's current page format, in milli-in. Pre-calculated for ready access.
    */
   private double pageHeightMI = 11000;

   /**
    * Location of BL corner of printable rectangle WRT BL corner of the page, IAW the canvas's current page format, in 
    * milli-in. Pre-calculated for ready access.
    */
   private final Point2D printableBL_MI = new Point2D.Double();

   /**
    * This path is a rectangular annulus enclosing the margins of the printed page in print preview mode. It is 
    * updated each time the page format changes. It is used to paint the margins whenever the canvas is repainted in 
    * print preview mode.
    */
   private final GeneralPath printMarginsPath = new GeneralPath();
   
   /**
    * Get a copy of the printed page format currently installed on this <code>RenderingCanvas</code>. It describes the 
    * size, orientation, and imageable rectangle of the printed page; the canvas uses this page layout only when 
    * displaying graphics in "print preview" mode.
    * 
    * @return An independent copy of the installed <code>PageFormat</code>.
    */
   PageFormat getPageFormat()
   {
      PageFormat pgf;
      synchronized(this)
      {
         pgf = (PageFormat)pgFmt.clone();
      }
      return(pgf);
   }


   private final static double PT_TO_MILLI_IN = 1000.0 / 72.0;

   /** 
    * Change the current page format installed on this <code>RenderingCanvas</code>. If the page layout has changed in 
    * any way, the canvas is refreshed automatically, and the current zoom & pan transform is reset -- but only if the 
    * canvas's print preview mode is on.
    * 
    * @param pgf The new page format. If <code>null</code>, the page format is restored to a default state: letter 
    * size, portrait orientation, with half-inch margins all around. If the specified format uses reverse landscape 
    * orientation, that is changed to normal landscape orientation -- reverse landscape is <em>not</em> supported.
    */
   void setPageFormat(PageFormat pgf)
   {
      PageFormat old = pgFmt;
      synchronized(this)
      {
         if(pgf == null) 
         {
            pgFmt = new PageFormat();
            Paper letterPaper = new Paper();
            letterPaper.setImageableArea( 36, 36, 540, 720 );
            pgFmt.setPaper( letterPaper );

         }
         else pgFmt = (PageFormat) pgf.clone();
      }

      // REVERSE_LANDSCAPE orientation not supported.
      if(pgFmt.getOrientation() == PageFormat.REVERSE_LANDSCAPE)
         pgFmt.setOrientation(PageFormat.LANDSCAPE);

      // pre-calculate page dimensions and location of BL corner of printable rectangle WRT an origin at BL corner of 
      // the page itself, in milli-in. These parameters are used to perform device-to-logical transformations in 
      // "print preview" mode.
      pageWidthMI = pgFmt.getWidth() * PT_TO_MILLI_IN;
      pageHeightMI = pgFmt.getHeight() * PT_TO_MILLI_IN;
      printableBL_MI.setLocation(pgFmt.getImageableX()*PT_TO_MILLI_IN, 
            pageHeightMI - (pgFmt.getImageableHeight() + pgFmt.getImageableY()) * PT_TO_MILLI_IN);

      // prepare rectangular annulus enclosing the print margins, WRT an origin at the BL corner of the page, in 
      // milli-in. So that only the annulus is filled using the GeneralPath.NON_ZERO winding rule, the outer rectangle 
      // is drawn CCW while the inner is CW. Whenever canvas is painted in print preview mode, this annulus is filled 
      // with dark gray and thinly outlined in black.
      printMarginsPath.reset();
      printMarginsPath.moveTo(0, 0);
      printMarginsPath.lineTo((float)pageWidthMI, 0);
      printMarginsPath.lineTo((float)pageWidthMI, (float)pageHeightMI);
      printMarginsPath.lineTo(0, (float)pageHeightMI);
      printMarginsPath.closePath();

      float x = (float) printableBL_MI.getX();
      float y = (float) printableBL_MI.getY();
      float w = (float) (pgFmt.getImageableWidth() * PT_TO_MILLI_IN);
      float h = (float) (pgFmt.getImageableHeight() * PT_TO_MILLI_IN);
      printMarginsPath.moveTo(x, y);
      printMarginsPath.lineTo(x, y+h);
      printMarginsPath.lineTo(x+w, y+h);
      printMarginsPath.lineTo(x+w, y);
      printMarginsPath.closePath();

      // if page layout is unchanged or print preview is disabled, no need to refresh canvas; otherwise, do so!
      if( !isPrintPreviewOn() ) return;
      if( (old.getWidth()==pgFmt.getWidth()) && (old.getHeight()==pgFmt.getHeight()) && 
          (old.getOrientation()==pgFmt.getOrientation()) && (old.getImageableX()==pgFmt.getImageableX()) && 
          (old.getImageableY()==pgFmt.getImageableY()) && (old.getImageableWidth()==pgFmt.getImageableWidth()) &&
          (old.getImageableHeight()==pgFmt.getImageableHeight()) )
          return;

      resetViewAndRender();
   }


   // 
   // MouseInputListener -- Any implemented mouse interactions should go in this section
   //

   /**
    * Current mouse position within the graphic displayed on this <code>RenderingCanvas</code>. If mouse is not within 
    * the canvas viewport, this will be <code>null</code>. Expressed in the logical coordinate system of the rendered 
    * graphic.
    */
   private Point2D currentCursorPos = null;
   
   /**
    * Flag set when the a "pan-by-drag" interaction or an animated panning transition is in progress. The mouse-drag
    * interaction is initiated by a mouse down on the non-primary mouse button, unless the Shift key is depressed, 
    * while the animated pan action is initiated programmatically -- see <code>animatePan()</code>.
    */
   private boolean isActivePanning = false;

   /**
    * Flag set when the current <code>Focusable</code> is being interactively repositioned via mouse drag. This 
    * "move-by-drag" interaction is initiated by a mouse down within the current focus highlight with no modifier keys 
    * down, but only if the <code>Focusable</code> node supports interactive repositioning.
    */
   private boolean isDraggingFocus = false;
   
   /**
    * The accumulated offset in mouse position for the horizontal direction during a mouse drag. Valid only while the 
    * "pan-by-drag" or "move-by-drag" interaction is in progress. In device pixels.
    */
   private int xDeltaDrag = 0;

   /**
    * The accumulated offset in mouse position for the vertical direction during a mouse drag. Valid only while the 
    * "pan-by-drag" or "move-by-drag" interaction is in progress. In device pixels.
    */
   private int yDeltaDrag = 0;

   /**
    * The x-coordinate of the mouse location the last time a mouse dragging event was processed during an ongoing 
    * "pan-by-drag" or "move-by-drag" interaction. In device pixels.
    */
   private int xLastDrag = 0;

   /**
    * The y-coordinate of the mouse location the last time a mouse dragging event was processed during an ongoing 
    * "pan-by-drag" or "move-by-drag" interaction. In device pixels.
    */
   private int yLastDrag = 0;

   /**
    * Location of mouse cursor when a "move-by-drag" interaction was initiated. The difference between this position 
    * and the current cursor position gives the net displacement in the focus node's position. Expressed in the logical 
    * coordinate system of the rendered graphic. Valid only during a "move-by-drag" interaction.
    */
   private Point2D ptFocusDragStart = null;

   /**
    * A copy of the image under the rectangle that bounds the current focus highlight. This image tracks the mouse as 
    * the user drags it across the canvas during a "move-by-drag" interaction. Cool effect!
    */
   private BufferedImage focusDragImage = null;

   /**
    * The location of the upper left corner of the focus image drag rectangle when a "move-by-drag" interaction was 
    * initiated, in device pixels. These coordinates, offset by the net drag offsets (dx, dy), give the current 
    * device location at which the focus drag image should be drawn.
    */
   private final Point ptULFocusDragImage = new Point(0,0);

   /**
    * This handler implements the following gestures:
    * <ul>
    *    <li>SHIFT-click using mouse button 1 (usually the left button): Zoom in on displayed graphic (scale *= 2, up 
    *    to the supported maximum) and recenter the graphic at the logical location that was "clicked on".</li>
    *    <li>SHIFT-click using a different mouse button: Zoom out on displayed graphic (scale /= 2, down to the 
    *    supported minimum) and recenter the graphic at the logical location that was "clicked on".</li>
    *    <li>Click using mouse button #1, SHIFT key NOT down: If a <code>RenderableModel</code> is installed that 
    *    supports a focus node, the model is informed that a point was clicked on the canvas. The model may then update 
    *    the identity of the focus node.</li>
    *    <li>Click using a mouse button other than #1, SHIFT key NOT down: Recenters displayed graphic at the logical 
    *    location "clicked on" by adjusting the current pan position.</li>
    * </ul>
    * 
    * <p>Note that "scale to fit" mode effectively disables zooming and panning, so all of the above mouse gestures 
    * (except for the third one) are ignored when "scale to fit" is enabled.</p>
    * 
    * @see java.awt.event.MouseListener#mouseClicked(MouseEvent)
    */
   public void mouseClicked(MouseEvent e)
   {
      e.consume();
      requestFocusInWindow();
      if(e.getClickCount() != 1) return;

      boolean shiftDown = e.isShiftDown();
      boolean btn1Clicked = (e.getButton() == MouseEvent.BUTTON1);
      if(!scaleToFitOn)
      { 
         if(shiftDown)
         {
            zoomAndPanTo(e.getX(), e.getY(), btn1Clicked);
            return;
         }
         else if(!(btn1Clicked || shiftDown))
         {
            panToRecenterAt(e.getX(), e.getY());
            return;
         }
      }

      if((model != null) && btn1Clicked && !shiftDown)
         model.pointClicked((Point2D) currentCursorPos.clone());
   }

   /**
    * This method can initiate one of two possible mouse-drag interactions:
    * <ul>
    *    <li>The "move-by-drag" interaction begins when mouse button #1 is pressed (and the SHIFT key is NOT down; other 
    *    modifier keys ignored), if the mouse location lies within the current focus highlight, <strong>and</strong> 
    *    the <code>Focusable</code> node supports interactive repositioning. If the interaction is started, the cursor 
    *    changes to the predefined "hand cursor", the current focus highlight is replaced by a semi-translucent image 
    *    of the graphics bounded by that highlight, and this "focus drag image" will begin tracking the mouse until it 
    *    is released.</li>
    *    <li>The "pan-by-drag" interaction begins if a mouse button other than #1 is pressed (and the SHIFT key is NOT 
    *    down; other modifier keys ignored), and if panning is possible given the current state of this 
    *    <code>RenderingCanvas</code>. Note that panning is disabled in "scale-to-fit" mode. If the interaction is 
    *    started, the cursor changes to the predefined "move cursor" and will remain so until the mouse is released.</li>
    * </ul>
    * 
    * @see java.awt.event.MouseListener#mousePressed(MouseEvent)
    */
   public void mousePressed(MouseEvent e)
   {
      e.consume();
      if(e.isShiftDown()) return;

      boolean isBtn1 = ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK);
      if(canPan() && !isBtn1)
      {
         isActivePanning = true;
         xLastDrag = e.getX();
         yLastDrag = e.getY();
         xDeltaDrag = 0;
         yDeltaDrag = 0;
         setCursor( Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR) );
      }
      else if(isBtn1 && (currentFocusable != null) && currentFocusable.canMove() && (currentFocusShape != null))
      {
         Point2D logPt = deviceToLogical(e.getX(), e.getY(), null);
         if(currentFocusShape.contains(logPt))
         {
            isDraggingFocus = true;
            ptFocusDragStart = logPt;
            xLastDrag = e.getX();
            yLastDrag = e.getY();
            xDeltaDrag = 0;
            yDeltaDrag = 0;
            prepareFocusDragImage();
            setCursor( Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) );
            paintImmediately(0, 0, getWidth(), getHeight());
         }
      }
   }

   /**
    * Helper method which prepares the "focus drag image" for a "move-by-drag" interaction. This image is simply a copy 
    * of a portion of the current backbuffer -- that portion which lies underneath the current focus highlight. As the 
    * mouse is dragged across the canvas, this image tracks the mouse location -- thereby animating the interaction.
    * 
    * <p>The method also stores the device coordinates of the UL corner of the image WRT the canvas window. By adjusting 
    * the location of the image IAW the net motion of the mouse since dragging began, we effectively make the drag image 
    * follow the mouse.</p>
    *
    */
   private void prepareFocusDragImage()
   {
      focusDragImage = null;
      if(currentFocusShape == null) return;
      Rectangle2D rFocus = currentFocusShape.getBounds2D();
      rFocus = logicalToDevice(rFocus);
      
      // grow by 2 pixels in all directions, but restrict to canvas window
      int x = (int) (rFocus.getX() - 2);
      int y = (int) (rFocus.getY() - 2);
      int w = (int) (rFocus.getWidth() + 4);
      int h = (int) (rFocus.getHeight() + 4);
      if(x < 0) 
      {
         w += x;
         x = 0;
      }
      if(y < 0) 
      {
         h += y;
         y = 0;
      }
      if(x + w > getWidth()) w = getWidth()-x;
      if(y + h > getHeight()) h = getHeight() - y;

      if(w <= 0 || h <= 0) return;  // focus shape is just outside window? This should not happen.

      focusDragImage = getGraphicsConfiguration().createCompatibleImage(w,h);
      if(focusDragImage != null)
      {
         boolean ok = false;
         Graphics2D g2d = focusDragImage.createGraphics();
         try
         {
            ok = renderer.copySubimageFromBackBuffer(g2d, x, y, w, h);
         }
         finally { if(g2d != null) g2d.dispose(); }
 
         if(!ok) 
         {
            focusDragImage.flush(); 
            focusDragImage = null;
         }
         else
         {
            ptULFocusDragImage.setLocation(x,y);
         }
      }
   }

   /**
    * Capture a snapshot of the canvas's current contents (from the back buffer).
    * @param rLog Rectangular area to be captured, in logical (model) coordinates.
    * @return An image containing a copy of the canvas content within the specified rectangle. If a portion of the 
    * rectangle is outside the canvas bounds, only the visible portion is returned. If the entire rectangle is outside 
    * the canvas bounds, or the image could not be copied from the back buffer, then <code>null</code> is returned.
    */
   BufferedImage captureCanvasImage(Rectangle2D rLog)
   {
      Rectangle2D rDev = logicalToDevice(rLog);
      int x = (int) rDev.getX();
      int y = (int) rDev.getY();
      int w = (int) rDev.getWidth();
      int h = (int) rDev.getHeight();
      if(x < 0) { w += x; x = 0; }
      if(y < 0) { h += y; y = 0; }
      if(x + w > getWidth()) w = getWidth()-x;
      if(y + h > getHeight()) h = getHeight() - y;
      if(w <= 0 || h <= 0) return(null);
      
      BufferedImage bi = getGraphicsConfiguration().createCompatibleImage(w,h);
      if(bi != null)
      {
         boolean ok = false;
         Graphics2D g2d = bi.createGraphics();
         try
         {
            ok = renderer.copySubimageFromBackBuffer(g2d, x, y, w, h);
         }
         finally { if(g2d != null) g2d.dispose(); }
         
         if(!ok) { bi.flush(); bi = null; }
      }
      return(bi);
   }
   
   /**
    * This method handles termination of either of the supported mouse-drag interactions:
    * <ul>
    *    <li>"Pan-by-drag" interaction. If the accumulated "pan-by-drag" offset is nonzero in X or Y, the current 
    *    device pan position is altered accordingly and the canvas is re-rendered to reflect the new pan position. In 
    *    addition, the cursor is switched back to a crosshair if the mouse is inside the canvas window, else it is 
    *    returned to the default cursor shape.</li>
    *    <li>"Move-by-drag" interaction. In this case, the method computes the net change in the mouse location in 
    *    logical coordinates, then invokes <code>RenderableModel.moveFocusable(double,double)</code> to update the 
    *    installed model appropriately. The canvas is also repainted to restore the normal focus highlight.</li>
    * </ul>
    * 
    * 
    * @see java.awt.event.MouseListener#mouseReleased(MouseEvent)
    */
   public void mouseReleased(MouseEvent e)
   {
      e.consume();
      if(isActivePanning)
      {
         isActivePanning = false;
         boolean isInCanvas = (e.getX() >= 0 && e.getX() < getWidth() && e.getY() >= 0 && e.getY() < getHeight());
         setCursor( isInCanvas ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor() );
         if(xDeltaDrag != 0 || yDeltaDrag != 0)
         {
            currDevPanPos.setLocation(currDevPanPos.getX() - xDeltaDrag, currDevPanPos.getY() - yDeltaDrag);
            fixPanPosition();
            postRenderJob(null);
            updateToolbarState();
            fireViewportChangedEventIfNecessary();
         }
      }
      else if(isDraggingFocus)
      {
         isDraggingFocus = false;
         if(focusDragImage != null)
         {
            focusDragImage.flush();
            focusDragImage = null;
         }
         boolean isInCanvas = (e.getX() >= 0 && e.getX() < getWidth() && e.getY() >= 0 && e.getY() < getHeight());
         setCursor( isInCanvas ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor() );
         Point2D pEnd = deviceToLogical(e.getX(), e.getY(), null);
         if(pEnd.getX() != ptFocusDragStart.getX() || pEnd.getY() != ptFocusDragStart.getY())
            model.moveFocusable(pEnd.getX() - ptFocusDragStart.getX(), pEnd.getY() - ptFocusDragStart.getY());
         paintImmediately(0, 0, getWidth(), getHeight());         
      }
   }

   /**
    * When the mouse cursor enters the canvas window, the cursor shape is switched to a crosshair, and a "cursor moved" 
    * event is sent to any registered canvas listeners. However, if the "pan-by-drag" interaction is still in progress 
    * (the user holds the mouse down while moving outside of and then back into the canvas window), the cursor shape is 
    * not changed.
    * 
    * @see java.awt.event.MouseListener#mouseEntered(MouseEvent)
    */
   public void mouseEntered(MouseEvent e)
   {
      if(e.getSource() == this && !isActivePanning)
         setCursor( Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) );
      mouseMoved(e);
   }

   /**
    * When the mouse cursor exits the canvas window, the cursor shape is restored to the default shape -- unless the 
    * "pan-by-drag" interaction is still in progress (user holds mouse down while moving outside window).
    * 
    * <p>If the "move-by-drag" mouse interaction is in progress, it is cancelled when the mouse exits the canvas 
    * window -- one cannot drag the focus node outside the current viewable area of the canvas.</p>
    * 
    * @see java.awt.event.MouseListener#mouseEntered(MouseEvent)
    */
   public void mouseExited(MouseEvent e)
   {
      if(e.getSource() == this)
      {
         e.consume();
         if(!isActivePanning) setCursor( Cursor.getDefaultCursor() );
         if(isDraggingFocus)
         {
            isDraggingFocus = false;
            if(focusDragImage != null)
            {
               focusDragImage.flush();
               focusDragImage = null;
            }
            paintImmediately(0, 0, getWidth(), getHeight());
         }
         currentCursorPos = null;
         fireCanvasEvent(CanvasEventType.CURSORMOVED, 0, 0);
      }
   }

   /**
    * This method animates an ongoing "pan-by-drag" or "move-by-drag" interaction.
    * 
    * <p>For a "pan-by-drag" interaction: The net drag offsets (dx, dy) are updated IAW the change in cursor position, 
    * then the canvas is immediately repainted -- the entire graphics context is temporarily translated by (dx, dy).
    * When the mouse is dragged outside window and stops moving (but the mouse button is still held down), synthetic 
    * drag events will be generated (the <code>autoscrolls</code> flag is set!). In this case, if the cursor is right 
    * of the window, the viewport pans right by 1/20th of the canvas width; analogously, when the cursor is left of, 
    * above, and below window. If the mouse has moved since the last drag event -- whether it is outside the window or 
    * not --, the viewport pans in the direction the cursor moved. <em>Note</em> that the mouse drag offsets are 
    * corrected so that user cannot pan the graphic further than is possible.</p>
    * 
    * <p>For a "move-by-drag" interaction: The current cursor position is updated and the canvas is repainted. In this 
    * case, the focus highlight is translated by the difference in the current cursor position and its position when 
    * the drag interaction began. <em>Note</em> that position displacements are calculated in <strong>logcial</strong>
    * units, not device pixels as in the "pan-by-drag" interaction.</p>
    * 
    * @see java.awt.event.MouseMotionListener#mouseDragged(MouseEvent)
    */
   public void mouseDragged(MouseEvent e)
   {
      mouseMoved(e);
      if(isActivePanning)
      {
         // if mouse is dragged outside canvas window and has not moved since the last drag, then this is a synthetic 
         // mouse drag event. If cursor is right of window, we pan the viewport right by 1/20th of canvas width; 
         // similarly for cursor left, above, and below window. If the mouse has moved, regardless if it is outside the 
         // window or not, pan in the direction the cursor moved.
         if(e.getX() == xLastDrag && e.getY() == yLastDrag)
         {
            int canvasW = getWidth();
            int canvasH = getHeight();
            if(xLastDrag < 0 || xLastDrag > canvasW || yLastDrag < 0 || yLastDrag > canvasH)
            {
               if(xLastDrag < 0) xDeltaDrag -= canvasW/20;
               else if(xLastDrag > canvasW) xDeltaDrag += canvasW/20;

               if(yLastDrag < 0) yDeltaDrag -= canvasH/20;
               else if(yLastDrag > canvasH) yDeltaDrag += canvasH/20;
            }
         }
         else
         {
            // otherwise, pan in the direction that cursor moved
            xDeltaDrag += e.getX() - xLastDrag;
            yDeltaDrag += e.getY() - yLastDrag;
            xLastDrag = e.getX();
            yLastDrag = e.getY();
         }
         
         // don't allow panning beyond what is allowed
         double milliInToScaledPix = getCurrentScale() * pixelsPerInch / 1000.0;
         double graphicW = milliInToScaledPix * (isPrintPreviewOn() ? pageWidthMI : root.getWidthMI());
         double graphicH = milliInToScaledPix * (isPrintPreviewOn() ? pageHeightMI : root.getHeightMI());
         double canvasW = getWidth();
         double canvasH = getHeight();
         if(canvasW >= graphicW) xDeltaDrag = 0;
         else
         {
            double panX = currDevPanPos.getX() - xDeltaDrag;
            if(panX < 0) xDeltaDrag = (int) currDevPanPos.getX();
            else if(panX + canvasW > graphicW) xDeltaDrag = (int) (currDevPanPos.getX() + canvasW - graphicW);
         }
         if(canvasH >= graphicH) yDeltaDrag = 0;
         else
         {
            double panY = currDevPanPos.getY() - yDeltaDrag;
            if(panY < 0) yDeltaDrag = (int) currDevPanPos.getY();
            else if(panY + canvasH > graphicH) yDeltaDrag = (int) (currDevPanPos.getY() + canvasH - graphicH);
         }

         paintImmediately(0, 0, getWidth(), getHeight());
      }
      else if(isDraggingFocus)
      {
         xDeltaDrag += e.getX() - xLastDrag;
         yDeltaDrag += e.getY() - yLastDrag;
         xLastDrag = e.getX();
         yLastDrag = e.getY();
         paintImmediately(0, 0, getWidth(), getHeight());
      }
   }

   /**
    * Whenever the mouse moves within the canvas window, this method sends a "cursor moved" event to any registered 
    * listeners of the <code>RenderingCanvas</code>.
    * 
    * @see java.awt.event.MouseMotionListener#mouseMoved(MouseEvent)
    */
   public void mouseMoved(MouseEvent e)
   {
      if(e.getSource() == this)
      {
         e.consume();
         updateCurrentCursorPos(e.getPoint(), CanvasEventType.CURSORMOVED);
      }
   }

   /**
    * Update cached copy of the cursor's current position within the canvas window, and send a cursor-related event 
    * to any registered canvas listeners.
    * 
    * @param p The new cursor location, in canvas device pixels.
    * @param t The <code>CanvasEventType</code> to be sent.
    */
   private void updateCurrentCursorPos(Point p, CanvasEventType t)
   {
      if(p == null) return;
      currentCursorPos = deviceToLogical(p.x, p.y, currentCursorPos);
      fireCanvasEvent(t, 0, 0);
   }


   //
   // The canvas toolbar
   //

   /**
    * A toolbar for the canvas. One and only one instance will be created, for use within <code>Graph2DViewer</code>.
    */
   private ToolBar canvasToolBar = null;
   
   private void updateToolbarState()
   {
      if(canvasToolBar != null)
         canvasToolBar.updateState();
   }

   /**
    * Get a toolbar that is hooked into this <code>RenderingCanvas</code>. Only one toolbar instance is created for the 
    * canvas, and it is intended only for use by <code>Graph2DViewer</code>.
    * 
    * @return A horizontal, non-floating toolbar that provides a means of controlling the canvas state.
    */
   JToolBar getToolBarForCanvas()
   {
      if(canvasToolBar == null) canvasToolBar = new ToolBar();
      return(canvasToolBar);
   }

   /**
    * <code>ToolBar</code> is a <code>JToolBar</code>-derived UI element tailored specifically to expose the available 
    * actions and user-modifiable properties of the <code>RenderingCanvas</code>.
    * @author sruffner
    */
   private class ToolBar extends JToolBar implements ActionListener, CanvasListener
   {
      private static final long serialVersionUID = 1L;

      private JCheckBox printPreviewCheck = null;

      private JButton zoomInBtn = null;
      private JButton zoomOutBtn = null;
      private JButton refreshBtn = null;
      private JButton resetBtn = null;
      private JCheckBox scaleToFitCheck = null;

      ToolBar()
      {
         RenderingCanvas.this.addListener(this);

         zoomInBtn = new JButton(GUIUtilities.createImageIcon(RenderingCanvas.class, 
               "/com/srscicomp/common/resources/zoomin.gif", ""));
         zoomInBtn.setToolTipText("Zoom in on picture");
         zoomInBtn.addActionListener(this);
         zoomInBtn.setVisible(!isScaleToFitOn());
         add(zoomInBtn);
         
         zoomOutBtn = new JButton(GUIUtilities.createImageIcon(RenderingCanvas.class, 
               "/com/srscicomp/common/resources/zoomout.gif", ""));
         zoomOutBtn.setToolTipText("Zoom out on picture");
         zoomOutBtn.addActionListener(this);
         zoomOutBtn.setVisible(!isScaleToFitOn());
         add(zoomOutBtn);

         refreshBtn = new JButton(GUIUtilities.createImageIcon(RenderingCanvas.class, 
               "/com/srscicomp/common/resources/refresh.gif", ""));
         refreshBtn.setToolTipText("Refresh");
         refreshBtn.addActionListener(this);
         add(refreshBtn);

         resetBtn = new JButton(GUIUtilities.createImageIcon(RenderingCanvas.class, 
               "/com/srscicomp/common/resources/resetvu.gif", ""));
         resetBtn.setToolTipText("Restore normal view");
         resetBtn.addActionListener(this);
         resetBtn.setVisible(!isScaleToFitOn());
         add(resetBtn);
         
         // check box to turn scale-to-fit mode on/off
         scaleToFitCheck =  new JCheckBox("Scale to fit");
         scaleToFitCheck.setSelected( isScaleToFitOn() );
         scaleToFitCheck.addActionListener(this);
         add(scaleToFitCheck);

         addSeparator();

         // check box to turn print preview mode on/off
         printPreviewCheck = new JCheckBox("Print preview");
         printPreviewCheck.setSelected( isPrintPreviewOn() );
         printPreviewCheck.addActionListener(this);

         JPanel p = new JPreferredSizePanel();
         p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
         printPreviewCheck.setAlignmentY(Component.CENTER_ALIGNMENT);
         p.add(printPreviewCheck);
         p.add(Box.createHorizontalStrut(5));

         add(p);
         
         setMinimumSize(getPreferredSize());
      }

      private void updateState()
      {
         zoomInBtn.setEnabled(canZoomIn());
         zoomOutBtn.setEnabled(canZoomOut());
         resetBtn.setEnabled(canResetView());
         refreshBtn.setEnabled(RenderingCanvas.this.root != null);
      }
      
      @Override
      public int getOrientation()
      {
         // orientation is always horizontal
         return(JToolBar.HORIZONTAL);
      }

      @Override
      public boolean isFloatable()
      {
         // never floats
         return(false);
      }

      public void actionPerformed(ActionEvent e)
      {
         Object src = e.getSource();
         if(src == printPreviewCheck)
         {
            boolean on = printPreviewCheck.isSelected();
            setPrintPreviewOn(on);
         }
         else if(src == scaleToFitCheck)
         {
            boolean on = scaleToFitCheck.isSelected();
            setScaleToFitOn(on);
            zoomInBtn.setVisible(!on);
            zoomOutBtn.setVisible(!on);
            resetBtn.setVisible(!on);
         }
         else if(src == zoomInBtn)
         {
            if(canZoomIn()) 
               zoomAndPanTo(RenderingCanvas.this.getWidth()/2, RenderingCanvas.this.getHeight()/2, true);
         }
         else if(src == zoomOutBtn)
         {
            if(canZoomOut())
               zoomAndPanTo(RenderingCanvas.this.getWidth()/2, RenderingCanvas.this.getHeight()/2, false);
         }
         else if(src == refreshBtn)
            postRenderJob(null);
         else if(src == resetBtn)
         {
            if(canResetView())
               resetViewAndRender();
         }
      }

      public void renderingStarted(CanvasEvent e) {}
      public void renderingStopped(CanvasEvent e) {}
      public void renderingCompleted(CanvasEvent e) {}
      public void renderingInProgress(CanvasEvent e) {}
      public void cursorMoved(CanvasEvent e) {}
      public void viewportChanged(CanvasEvent e) 
      {
         // make sure that toolbar's display of print preview flag is up-to-date
         boolean on = printPreviewCheck.isSelected();
         boolean actualOn = isPrintPreviewOn();
         if(on != actualOn)
            printPreviewCheck.setSelected(actualOn);
      }
   }

      
   //
   // Display focus -- available only if canvas is interactive or focus highlight is specifically enabled
   //

   /** Flag set to enable focus highlight on a non-interactive canvas. Ignored if canvas is interactive. */
   private boolean isFocusHighlightEnabled = false;

   /** 
    * Enable/disable the focus highlight feature on a non-interactive canvas. The canvas is repainted to reflect the
    * change. If canvas is interactive, this method has no effect: the focus highlight is always enabled on an 
    * interactive canvas. 
    * @param b <code>True</code> to enable, <code>false</code> to disable the focus highlight.
    */
   void setFocusHighlightEnabled(boolean b) 
   {
      if(isInteractive) return;
      if(isFocusHighlightEnabled != b)
      {
         isFocusHighlightEnabled = b;
         if(!isFocusHighlightEnabled)
         {
            currentFocusable = null;
            currentFocusShape = null;
            repaint();
         }
         else updateFocusHighlight();
      }
   }
   
   /**
    * The <code>Focusable</code> node representing the portion of the rendered graphic that holds the display focus.
    */
   private Focusable currentFocusable = null;

   /**
    * Cached shape representing the focus highlight for the graphics node that currently holds the display focus. The 
    * shape is recached whenever the focus node changes or the canvas is re-rendered. It is cached only in support of 
    * the "move-by-drag" mouse interaction.
    */
   private Shape currentFocusShape = null;

   /**
    * Update the graphic node that holds the display focus on the canvas. The method always causes a repaint of the 
    * canvas. The method has no effect unless canvas is interactive or the focus highlight has been explicitly enabled 
    * for a non-interacative canvas.
    * 
    * <p>If the scaled graphic is larger than the canvas viewport, it it possible that the new <code>Focusable</code> 
    * node will not be in view. If this is the case, the method will pan the canvas so that (if possible) the focussed 
    * node will be centered on the canvas.</p>
    */
   void updateFocusHighlight()
   {
      if(!(isInteractive || isFocusHighlightEnabled)) return;

      // query the installed model for the identity of the current focus node. If it has not changed, do nothing.
      Focusable f = (model != null) ? model.getCurrentFocusable() : null;
      if(currentFocusable == f) return;

      currentFocusable = f;
      currentFocusShape = null;

      // if the rectangular bounds of the new focus highlight do not intersect the visible canvas viewport, then we 
      // pan the canvas window so that it is centered on that focus highlight. Else we just repaint to show the new 
      // highlight. Getting the focus shape requires a graphics context, which we need to dispose of.
      if(currentFocusable != null)
      {
         Graphics2D g2d = getRenderingGraphics();
         if(g2d == null) 
         {
            repaint();
            return;
         }

         try
         {
            currentFocusShape = currentFocusable.getFocusShape(g2d);
            if(currentFocusShape != null)
            {
               Rectangle2D bounds = logicalToDevice(currentFocusShape.getBounds2D());
               if(bounds != null)
               {
                  if(!bounds.intersects(5, 5, getWidth()-5, getHeight()-5))
                  {
                     panToRecenterAt((int) (bounds.getX() + bounds.getWidth()/2), 
                           (int) (bounds.getY()+bounds.getHeight()/2)); 
                     return;
                  }
               }
            }
         }
         finally
         {
            g2d.dispose();
         }

      }
      repaint();
   }

   
   //
   // Painting the canvas
   //

   /**
    * Thread object representing the background thread in which all rendering jobs take place.
    */
   private Renderer renderer = null;

   /**
    * The canvas size, the last time we checked. We compare this with the current size on each call to 
    * <code>paintComponent()</code> to catch resizes and respond accordingly.
    */
   private final Dimension lastCanvasSize = new Dimension(0,0);

   /**
    * The solid translucent color used to paint the focus highlight on top of the canvas rendering.
    */
   private final static Color focusHiliteColor = new Color(100, 100, 255, 100);

   /**
    * The solid translucent color used to paint the focus highlight when a "move-by-drag" interaction is in progress --
    * BUT only if we were unable to prepare the "focus drag image".
    */
   private final static Color moveFocusColor = new Color(100, 255, 100, 75);

   /**
    * Paint the canvas by simply acquiring the offscreen buffer from the renderer thread and copying its contents to 
    * the screen. If the backbuffer is unavailable -- because the renderer thread is currently using it --, the method 
    * simply ignores the paint request -- because the renderer will fire a new paint request very shortly. If print 
    * preview mode is turned on, the print margins will be grayed out. If a focus highlight is defined, that will be 
    * painted on top of the rendered image.
    * 
    * <p>When a "pan-by-drag" interaction is in progress, the interaction is "animated" by translating the canvas's 
    * graphic context by the current accumulated "pan-by-drag" offsets. No re-rendering is done -- so if the scaled 
    * graphic is larger than the current backbuffer, the user will see only part of the complete graphic as he pans its 
    * around. However, when the interaction is terminated, a rendering job is posted to update the canvas properly IAW 
    * the change in the device pan position.</p>
    * 
    * <p>When a "move-by-drag" interaction is in progress, the interaction is "animated" by creating a "focus drag 
    * image" and forcing that image to track the mouse. The "drag image" is simply a copy of the rendered graphics that 
    * lie beneath the focus highlight -- giving the impression that the user is actually dragging the graphic around.
    * If we are unable to create this drag image, we fallback on a simpler approach: forcing the focus highlight shape 
    * to move with the mouse and painting it is a different color from the normal focus highlight.</p>
    * 
    * <p><em>Note</em> that the two distinct mouse-drag interactions can never be active simultaneously.</p>
    */
   @Override
   protected void paintComponent(Graphics g)
   {
      // if background renderer does not exist, there's nothing to paint!
      if(renderer == null) return;

      // get current canvas size and check for a resize
      int w = lastCanvasSize.width;
      int h = lastCanvasSize.height;
      getSize(lastCanvasSize);
      boolean resized = (w != lastCanvasSize.width) || (h != lastCanvasSize.height);
      w = lastCanvasSize.width;
      h = lastCanvasSize.height;

      // if canvas was resized, fix the device pan position before we copy buffer into the canvas!
      if(resized)
         fixPanPosition();
      
      // clear entire canvas 
      g.setColor(getBackground());
      g.fillRect(0, 0, w, h);

      // animate "pan-by-drag" effect
      if(isActivePanning)
         g.translate(xDeltaDrag, yDeltaDrag);

      // copy current backbuffer into canvas
      Graphics gCopy = g.create();
      boolean abort = false;
      try
      {
         abort = !renderer.paintFromOffscreen(gCopy);
      }
      finally
      {
         gCopy.dispose();
      }

      // if the canvas was resized, we need to update the enable state of our zoom/panning actions (we already fixed 
      // the canvas pan position earlier). We also must re-render so that the buffers are resized to sync with the 
      // canvas.
      if(resized)
      {
         updateToolbarState();
         fireViewportChangedEventIfNecessary();
         postRenderJob(null);
      }

      // paint margins for print preview mode and the focus highlight, if applicable
      if(abort || ((!printPreviewOn) && (root == null || currentFocusable == null))) return;
      Graphics2D g2d = (Graphics2D) g.create();
      try
      {
         // transform graphics context so that (1) logical units are milli-inches instead of pixels, and the y-axis 
         // increases upwards rather than downwards; (2) logical origin is at the BL corner of the printed page if
         // print preview is on, else at BL corner of displayed graphic; (3) the current zoom/pan factors are 
         // accounted for.
         double milliInToScaledPix = getCurrentScale() * getPixelsPerInch() / 1000.0;
         g2d.scale(milliInToScaledPix, -milliInToScaledPix);
         double hGraphic = printPreviewOn ? pageHeightMI : root.getHeightMI();
         double dx = -currDevPanPos.getX() / milliInToScaledPix;
         double dy = currDevPanPos.getY() / milliInToScaledPix;
         g2d.translate(dx, dy - hGraphic);                  

         // if print preview is on, we fill the annular region constituting the margins with dark gray and outline it 
         // in black. We then translate to the BL corner of the graphic displayed on the page.
         if(printPreviewOn)
         {
            // translucent mask over region of canvas that's outside the margins (and therefore not printed)
            outsideMarginPath.reset();
            double wMI = w / milliInToScaledPix;
            double hMI = h / milliInToScaledPix;
            double x0 = -dx;
            double y0 = -(dy - hGraphic) - hMI;
            outsideMarginPath.moveTo(x0, y0);
            outsideMarginPath.lineTo(x0 + wMI, y0);
            outsideMarginPath.lineTo(x0 + wMI, y0 + hMI);
            outsideMarginPath.lineTo(x0, y0 + hMI);
            outsideMarginPath.closePath();
            
            x0 = printableBL_MI.getX();
            y0 = printableBL_MI.getY();
            wMI = (pgFmt.getImageableWidth() * PT_TO_MILLI_IN);
            hMI = (float) (pgFmt.getImageableHeight() * PT_TO_MILLI_IN);
            outsideMarginPath.moveTo(x0, y0);
            outsideMarginPath.lineTo(x0, y0+hMI);
            outsideMarginPath.lineTo(x0+wMI, y0+hMI);
            outsideMarginPath.lineTo(x0+wMI, y0);
            outsideMarginPath.closePath();

            g2d.setColor(outsideMarginFill);
            g2d.fill(outsideMarginPath);

            // highlight margins by stroking the printable and page rectangles
            g2d.setColor(Color.BLACK);
            g2d.setStroke(marginStroke);
            g2d.draw(printMarginsPath);

            
            g2d.translate(printableBL_MI.getX(), printableBL_MI.getY());
            if(root != null)
            {
               Point2D botLeft = root.getPrintLocationMI();
               if(botLeft != null) g2d.translate(botLeft.getX(), botLeft.getY());
            }
         }

         // get the focus shape for the current focusable graphic node and fill it w/ the focus highlight color.
         // If we're animating a "move-by-drag" interaction, we use cached focus shape for performance reasons. We also 
         // translate the shape by how much the cursor has moved (in logical coords!) since the interaction started, 
         // and paint it a different color during that interaction.
         if(isDraggingFocus)
         {
            // if we're animating a "move-by-drag" interaction, we draw a "focus drag image" that tracks the cursor. If
            // for some reason that image could not be created, then we fallback on a simpler scheme: We use the cached 
            // focus shape, translate it by how much the cursor has moved (in logical coords!) since the drag 
            // interaction started, and paint it in a different color than the normal focus highlight.
            if(focusDragImage != null)
            {
               Graphics2D gCopy2 = (Graphics2D) g.create();
               try
               {
                  gCopy2.setComposite( AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f) );
                  gCopy2.drawImage(focusDragImage, ptULFocusDragImage.x + xDeltaDrag, 
                        ptULFocusDragImage.y + yDeltaDrag, null);
               }
               finally { gCopy2.dispose(); }
            }
            else
            {
               g2d.translate(currentCursorPos.getX() - ptFocusDragStart.getX(), 
                     currentCursorPos.getY() - ptFocusDragStart.getY());
               g2d.setColor(moveFocusColor);
               g2d.fill(currentFocusShape);
            }
            
         }
         else
         {
            // if we're not dragging the focus node around, then we just draw the normal focus highlight (if any)
            currentFocusShape = (currentFocusable != null) ? currentFocusable.getFocusShape(g2d) : null;
            if(currentFocusShape != null)
            {
               g2d.setColor(focusHiliteColor);
               g2d.fill(currentFocusShape);
            }
         }
      }
      finally
      {
         g2d.dispose();
      }

   }

   /** Stroke that outlines the page margins and page boundary (print preview only). (In milli-inches.)*/
   private static final BasicStroke marginStroke =
         new BasicStroke(20, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {100, 50} , 0);
   /** Translucent color masking areas on canvas that are not printed (print preview only). */
   private static final Color outsideMarginFill = new Color(92, 92, 92, 128);
   
   /** Annular region on canvas that is outside printable rectangle (print preview only). */
   private final GeneralPath outsideMarginPath = new GeneralPath();
   
   /**
    * Prepare the affine transformation from the logical coordinate system of the current <code>RootRenderable</code> 
    * to the device coordinate system of this <code>RenderingCanvas</code>. This so-called "rendering transform" 
    * takes into account print preview mode and the current zoom and pan state of the canvas, as well as the canvas's 
    * resolution in pixels-per-inch. The transform involves only a scaling and translation, so it should always be 
    * invertible. 
    * 
    * @return The current rendering transform.
    */
   private AffineTransform getRenderingTransform()
   {
      // logical units are milli-inches instead of pixels, and the y-axis increases upwards rather than downwards. 
      // Account for the canvas's current scale factor.
      double milliInToScaledPix = getCurrentScale() * getPixelsPerInch() / 1000.0;
      AffineTransform at = AffineTransform.getScaleInstance(milliInToScaledPix, -milliInToScaledPix);

      // translate origin to the BL corner of the current root renderable (if any). If print preview is on, we first 
      // have to take into account the page dimensions, margins, and the graphic's print location! Remember that the 
      // y-axis has been inverted above!
      double dx = 0;
      double dy = 0;
      if(printPreviewOn)
      {
         dy += -pageHeightMI + printableBL_MI.getY();
         dx += printableBL_MI.getX();
         if(root != null)
         {
            Point2D pBL = root.getPrintLocationMI();
            if(pBL != null)
            {
               dx += pBL.getX();
               dy += pBL.getY();
            }
         }
      }
      else if(root != null)
         dy = -root.getHeightMI();

      // also take into account the current canvas pan position (converted from pixels to milli-in
      dx += -currDevPanPos.getX() / milliInToScaledPix;
      dy += currDevPanPos.getY() / milliInToScaledPix;
      at.translate(dx, dy);

      return(at);
   }

   /**
    * Posts a new rendering task for the canvas's internal renderer.
    * 
    * @param dirtyRects If the root renderable has not changed, this should be a list of rectangular dirty regions that 
    * need to be redrawn (to minimize length of rendering task).  If the root renderable or the zoom/pan transform has 
    * changed, this must be <code>null</code> -- a complete re-rendering is essential.
    */
   private void postRenderJob(List<Rectangle2D> dirtyRects)
   {
      if(renderer != null)
         renderer.postRenderJob(root, dirtyRects, getSize(), getRenderingTransform());
   }

   /**
    * RenderingCanvas responds to changes in its installed model by posting a new render job on the background renderer 
    * thread.
    */
   void updateRendering(List<Rectangle2D> dirtyRects)
   {
      RootRenderable oldRoot = root;
      root = (model != null) ? model.getCurrentRootRenderable() : null;
      if(oldRoot != root)
         resetViewWithoutRender();
      else
      {
         fixPanPosition();
         updateToolbarState();
      }
      postRenderJob( (oldRoot != root) ? null : dirtyRects );
      fireViewportChangedEventIfNecessary();
   }

   /**
    * The user-specified "dots-per-inch" resolution for the physical screen on which this RenderingCanvas is displayed. 
    * This factor is essential for converting the logical coordinates of the rendered model to the device coordinates 
    * of the canvas. Initial value is 72.
    */
   private double pixelsPerInch = 72;

   /**
    * Set the number of pixels per inch on the physical screen on which this RenderingCanvas is displayed.  Setting 
    * this accurately is important to achieve a WYSIWYG rendering of the installed graphic model.
    * 
    * <p>If a graphic model is currently installed, invoking this method will trigger a complete refresh of the 
    * canvas. Also, the normal view (no zoom or pan) will be restored.</p>
    * 
    * @param dpi Number of pixels occupying one inch along screen.  Assumed to be the same in both horizontal and 
    * vertical directions.  Range-restricted to [50..200].
    * @return The new value for pixels per inch.  Will be same value as argument, unless argument is out of range.
    */
   public synchronized double setPixelsPerInch(double dpi)
   {
      double oldDPI = pixelsPerInch;
      pixelsPerInch = (dpi<50) ? 50 : ((dpi>200) ? 200 : dpi);
      if(oldDPI != pixelsPerInch)
      {
         fireViewportChangedEventIfNecessary();
         if(root != null)
         {
            resetViewWithoutRender();
            postRenderJob(null);
         }
      }
      return( pixelsPerInch );
   }

   public synchronized double getPixelsPerInch()
   {
      return( pixelsPerInch );
   }


   //
   // Rendering in a background thread.
   //

   /**
    * <code>Renderer</code> encapsulates the background thread that does all rendering for <code>RenderingCanvas</code>.
    * All rendering is done on a working buffer in system memory, a <code>BufferedImage</code> that is compatible with 
    * the graphics configuration of the canvas. While this likely limits us to software rendering performance, I've 
    * read that all desktop graphics hardware is still essentially single-threaded -- so a lengthy rendering to a 
    * hardware-accelerated <code>VolatileImage</code> could effectively block painting operations on the Swing event 
    * thread!
    * 
    * <p>A second offscreen <code>BufferedImage</code>, the "backbuffer", backs up the current contents of the canvas. 
    * Whenever the canvas needs painting <strong>on the Swing event thread</strong>, this backbuffer is copied to the 
    * primary surface. This really amounts to triple-buffering, since Swing components are already double-buffered on 
    * most platforms.  However, it is necessary so that paint events are handled properly while the working buffer is 
    * being rendered upon in the background thread (which could take many seconds!). Whenever the rendering completes 
    * successfully, the working buffer contents are copied into the backbuffer <strong>on the background thread</strong>, 
    * and a repaint request is queued.</p>
    * 
    * <p>Whenever a render job is posted because the user zoomed, panned or resized the canvas, a full re-rendering 
    * of the current <code>RootRenderable</code> is performed. The same is true when the <code>RootRenderable</code> is 
    * replaced. In this case, the fastest way to move the graphic into the current backbuffer is to simply swap the 
    * buffers. However, this introduces a problem when the next render job involves updating only a small portion of 
    * the graphic picture -- encapsulated by a list of one or more "dirty regions". If a buffer swap occurred on the 
    * last rendering pass, then the new working buffer does not necessarily hold the same image as the current 
    * backbuffer. To handle such a "render update" task, <code>Renderer</code> will copy the entire backbuffer into 
    * the working buffer, then erase only the rectangles in the dirty region list. The model's rendering code should 
    * check this list and re-render only what is necessary. After the rendering is finished, the buffers are swapped 
    * as usual. In this way, a "render update" should in many cases execute much faster than a full re-rendering of the 
    * graphic picture.</p>
    * 
    * <p>Both the working buffer and the backbuffer are encapsulated by <code>Renderer</code>, which synchronizes 
    * access to the two resources using <code>Lock</code>s. Whenever the canvas needs to repaint itself, it invokes 
    * <code>Renderer.paintFromOffscreen()</code>, which will copy the backbuffer into the provided Graphics context. 
    * This method is invoked only on the Swing event thread. If the backbuffer is not available because it is currently 
    * being updated on the renderer thread, the method will simply skip the paint request. The renderer thread will 
    * trigger a repaint shortly, since the working buffer -> backbuffer copy occurs at the tail end of a rendering 
    * job.</p>
    * 
    * <p>The size of the two buffers tracks the size of the canvas itself. When the canvas is resized by the user, the 
    * change in size is detected during the subsequent repaint on the Swing thread, and a rendering job is queued. 
    * During that rendering job, both buffers will eventually be resized to sync with the new canvas dimensions.</p>
    */
   private class Renderer extends Thread implements RenderTask
   {
      /**
       * The surface upon which all rendering occurs in the background rendering thread.
       */
      private BufferedImage workingBuffer = null;

      /**
       * Backbuffer for current contents of the canvas. When a rendering is completed, the buffers are swapped.
       */
      private BufferedImage currentBuffer = null;

      private final Lock workingBufferLock = new ReentrantLock();
      private final Lock currentBufferLock = new ReentrantLock();

      /**
       * This method should be invoked only on the Swing event thread to repaint the <code>RenderingCanvas</code>. It 
       * does so simply by copying the contents (or a portion thereof) of the current offscreen backbuffer, which is 
       * always created to be compatible with the graphics configuration of the canvas. 
       * 
       * <p>The "current" backbuffer is distinct from the "working" buffer, on which graphics are drawn in the renderer 
       * thread. The double-buffered design allows us to repaint the canvas as needed while the renderer thread is 
       * processing a lengthy render task.</p>
       * 
       * @param g Graphics context, presumably for the <code>RenderingCanvas</code> itself.
       * @return <code>True</code> if paint was performed. Returns <code>false</code> if unable to paint immediately 
       * because the renderer thread has locked the current backbuffer. In this case, the canvas will be repainted 
       * shortly anyway.
       */
      boolean paintFromOffscreen(Graphics g)
      {
         // we must have exclusive access to the current backbuffer. If we can't get it, then the renderer thread is 
         // either copying from the backbuffer or swapping buffers, so we'll just skip this paint request. 
         if( !currentBufferLock.tryLock() )
            return(false);
         else
         {
            try
            {
               // copy backbuffer into canvas (if it exists!)
               if(currentBuffer != null)
                  g.drawImage(currentBuffer, 0, 0, null);
            }
            finally
            {
               currentBufferLock.unlock();
            }
         }

         return(true);
      }

      /**
       * Copy the specified rectangle in the current backbuffer into the top-left cornder of the graphics context 
       * provided. 
       *
       * @param g Destination graphics context.
       * @param x X-coordinate of top-left corner of source rectangle in the current backbuffer.
       * @param y Y-coordinate of top-left corner of source rectangle in the current backbuffer.
       * @param w Width of source rectangle.
       * @param h Height of source rectangle.
       * @return <code>True</code> if successful; <code>false</code> if unable to access the current backbuffer.
       */
      boolean copySubimageFromBackBuffer(Graphics2D g, int x, int y, int w, int h)
      {
         boolean ok = false;
         if(currentBufferLock.tryLock())
         {
            try
            {
               g.drawImage(currentBuffer, 0, 0, w, h, x, y, x+w, y+h, null);
               ok = true;
            }
            finally
            {
               currentBufferLock.unlock();
            }
         }
         return(ok);
      }

      // 
      // Posting/retrieving a rendering task
      //

      /**
       * This is just a container for the different bits of information that need to be encapsulated at the moment a 
       * rendering job is created in the Swing event thread for later retrieval in the background renderer thread.
       * It does handle the task of coalescing dirty areas -- in case another job is posted for the same graphic under 
       * the same conditions as the currently pending job.
       * 
       * @author  sruffner
       */
      class RenderJobInfo
      {
         final RootRenderable root;
         final List<Rectangle2D> dirtyRects;
         final AffineTransform renderXfm;
         final Dimension canvasSz;

         RenderJobInfo(RootRenderable r, List<Rectangle2D> rList, Dimension canvasSz, AffineTransform renderXfm)
         {
            this.root = r;

            dirtyRects = (rList == null || rList.isEmpty()) ? null : rList;
            trimRects(dirtyRects);

            this.canvasSz = canvasSz;
            this.renderXfm = renderXfm;
         }

         /**
          * Combine the specified list of dirty regions to the current list of dirty regions for the (pending) render 
          * job represented by this <code>RenderJobInfo</code>. If it lacks a dirty region list, then the pending job 
          * demands a full rendering, and this method does nothing.
          * 
          * <p>Trimming the dirty list of unnecessary rectangles: If one of the new rectangles is completely contained 
          * by one of the existing rectangles, it is not added to the dirty list. If a new rectangle completely 
          * contains one of the existing rectangles, it replaces that rectangle in the dirty list. Otherwise, it is 
          * added to the list.</p>
          * 
          * @param rList A list of additional rectangular regions to add to the current list of dirty regions (if any) 
          * for this <code>RenderJobInfo</code>.
          */
         void addDirtyAreas(List<Rectangle2D> rList)
         {
            if(dirtyRects == null || rList == null || rList.isEmpty()) return;

            dirtyRects.addAll(rList);
            trimRects(dirtyRects);
         }

         /**
          * Helper method removes any rectangle from the specified list if it is completely contained by another 
          * rectangle in the list.
          * 
          * @param rList The list of rectangles to be trimmed.
          */
         private void trimRects(List<Rectangle2D> rList)
         {
            if(rList == null) return;
            int i = 0;
            while(i < rList.size())
            {
               Rectangle2D r = rList.get(i);
               boolean remove = false;
               if(r==null || r.isEmpty())
                  remove = true;
               else
               {
                  int j = i + 1;
                  while(j < rList.size())
                  {
                     Rectangle2D r2 = rList.get(j);
                     if(r.contains(r2.getX(), r2.getY(), r2.getWidth(), r2.getHeight()))
                        rList.remove(j);
                     else if(r2.contains(r.getX(), r.getY(), r.getWidth(), r.getHeight()))
                     {
                        remove = true;
                        j = rList.size();
                     }
                     else ++j;
                  }
               }

               if(remove) rList.remove(i);
               else ++i;
            }
         }

      }
 
      /**
       * This flag is set whenever a rendering job is posted to the Renderer, normally in the Swing event thread.  It 
       * is cleared whenever the Renderer starts a new job in the background thread.
       */
      private boolean isJobPending = false;

      /**
       * The pending render task.
       */
      private RenderJobInfo nextJob = null;

      private final Lock pendingJobLock = new ReentrantLock();
      private final Condition isJobPendingCondition = pendingJobLock.newCondition();

      /**
       * Post a new rendering job to the Renderer. If a job is already pending on the same <code>RootRenderable</code>
       * graphic, then the method merely adds the dirty regions of the new job to the pending task. If a different 
       * <code>RootRenderable</code> is specified or no dirty regions are given, then a complete refresh of the canvas 
       * will be posted, using the <code>RootRenderable</code> node specified.
       * 
       * <p>Dirty regions are always specified in the logical coordinate system of the <code>RootRenderable</code> 
       * given as the first argument to the method. This coordinate system must satisfy the assumptions laid out in the 
       * definition of the <code>RootRenderable</code> interface.</p>
       *
       * <p>If the background renderer thread is already working on a rendering task, that task should be aborted 
       * shortly and the new task started -- if the installed model was designed in accordance with the guidelines 
       * defined in the <code>RenderableModel</code> interface.  If the renderer thread is idle waiting for a new task, 
       * it should wake up shortly and begin work.</p>
       * 
       * @param r The top-level graphic object to be drawn. Can be <code>null</code>, which implies there is nothing to 
       * render -- so the canvas will ultimately be cleared. All other arguments are ignored in this case.
       * @param dirtyRects The list of rectangular regions that need to be rendered, in the logical coordinate space 
       * defined by the specified <code>RootRenderable</code>. If this list is <code>null</code> or empty, then the 
       * entire graphic is rendered.
       * @param sz Size of canvas, when this job was posted.
       * @param xfm The rendering transform, when this job was posted. This transforms the canvas device coordinates to 
       * the logical coordinate space of the <code>RootRenderable</code>. It takes into account the current zoom and 
       * pan state of the canvas, print preview mode, and the canvas resolution in pixels per inch.
       */
      void postRenderJob(RootRenderable r, List<Rectangle2D> dirtyRects, Dimension sz, AffineTransform xfm)
      {
         pendingJobLock.lock();
         try
         {
            if(nextJob == null || r == null || dirtyRects ==  null || dirtyRects.isEmpty())
               nextJob = new RenderJobInfo(r, dirtyRects, sz, xfm);
            else if(nextJob.root != r || !nextJob.canvasSz.equals(sz) || !nextJob.renderXfm.equals(xfm))
               nextJob = new RenderJobInfo(r, null, sz, xfm);
            else
            {
               // in this case we're drawing the same picture on an unchanged canvas with an unchanged rendering xfm.
               // So just combine the dirty regions of the new job with those in the currently pending job
               nextJob.addDirtyAreas(dirtyRects);
            }
            isJobPending = true;
            isJobPendingCondition.signal();
         }
         finally
         {
            pendingJobLock.unlock();
         }
      }

      /**
       * This method is called by the background renderer thread to wait (in an efficient wait state) for a new render 
       * task to be posted.  Upon return, the pending render task is now the current render task, and there is no longer 
       * a task pending.
       * 
       * @throws InterruptedException if thread is interrupted while waiting for a render job to be posted.
       */
      private void waitForNextRenderJob() throws InterruptedException
      {
         pendingJobLock.lock();
         try
         {
            isRenderJobInProgress = false;
            while( !isJobPending )
               isJobPendingCondition.await();
            currentJob = nextJob;
            nextJob = null;
            isJobPending = false;
            isRenderJobInProgress = true;
         }
         finally
         {
            // NOTE: if thread that's waiting is interrupted, then this code is executed by a different thread and 
            // the lock object will throw an unchecked exception
            try{ pendingJobLock.unlock(); } catch(Throwable t) {}
         }
      }

      /**
       * The render job currently being processed in the background renderer thread.
       */
      private RenderJobInfo currentJob = null;

      /**
       * Flag set while a rendering job is in progress on the renderer thread.
       */
      private boolean isRenderJobInProgress = false;

      /**
       * Flag set if the previous rendering job did not finish successfully. In that case, the next rendering job must 
       * be a full re-rendering to make sure the buffers are up-to-date.
       */
      private boolean lastJobFailed = false;

      /**
       * Approximate system time (ms) at which the current rendering task started.
       */
      private long tJobStart = 0;
 
      /**
       * Elapsed time, in ms since job start, at which last progress update event was posted for the current rendering 
       * job.
       */
      private long tLastProgressUpdate = 0;

      /**
       * Our current estimate of how long it will take to complete the current rendering task, in ms. When a rendering
       * job completes successfully, the job's elapsed time becomes the estimate for the next job. When the current 
       * rendering job exceeds the estimate, it is adjusted upward. Initially set to 1000ms.
       */
      private long tJobEstimate = 1000;

      /**
       * Flag set to (eventually) kill the background renderer thread. Initially <code>false</code>.
       */
      private boolean dieAsap = false;

      /**
       * Invoke this method (on the Swing event thread) to inform the background renderer thread that it should die as 
       * soon as possible.  Once you call this method, avoid invoking any other methods on <code>Renderer</code> 
       * (except isAlive(), of course!).
       */
      void die()
      {
         if( dieAsap ) return;

         dieAsap = true;
         this.interrupt();
      }

      /**
       * The background renderer thread's runtime loop.
       * 
       * <p>The renderer thread simply services render jobs posted to it by the enclosing <code>RenderingCanvas</code> 
       * instance. Such tasks are posted if a change is made to the model installed in the canvas, or if the canvas 
       * changes size, or if the user zooms, pans or otherwise changes the rendering transform for the canvas. The 
       * working buffer and current backbuffer are created just before starting the first rendering job. For each job, 
       * the renderer prepares a graphics context for rendering into its working buffer, then passes that context to 
       * the <code>render()</code> method of the model's <code>RootRenderable</code> node. If the rendering completes 
       * succesfully, the contents of the working buffer and backbuffer are swapped, and a repaint is queued. The 
       * renderer thread then goes idle waiting for the next job, and a short time later the canvas is updated on the 
       * Swing event thread to reflect the new state of the graphics model.</p>
       */
      @Override
      public void run()
      {
         while( !dieAsap )
         {
            // wait for a new render job to be posted. Abort if told to die while waiting.
            try
            {
               waitForNextRenderJob();
            }
            catch( InterruptedException ie ) {}
            if( dieAsap )
               break;
            else if( !isRenderJobInProgress )   // we were interrupted while waiting, but NOT told to die!
               continue;

            // lock working render buffer
            workingBufferLock.lock();

            // execute rendering job
            boolean doRepaint = false;
            try
            {
               // job is starting...
               tJobStart = System.currentTimeMillis();
               tLastProgressUpdate = 0;
               RenderingCanvas.this.fireCanvasEvent(CanvasEventType.STARTED, 0, 0);

               RootRenderable r = currentJob.root;
               AffineTransform xfm = currentJob.renderXfm;
               Dimension canvasSize = currentJob.canvasSz;
               
               // we do a full render if no dirty areas specified, if the last render job failed, or if the current
               // graphic has translucent regions (you get artifacts in translucent regions if you only update parts of
               // those regions!)
               boolean doFullRender = (currentJob.dirtyRects == null) || lastJobFailed || r.hasTranslucentRegions();

               // we cannot create the working buffer until we can get a graphics configuration for the canvas. Here's 
               // where we "lazily" create the buffer, or recreate it when a size change is warranted.
               if(workingBuffer == null || 
                     workingBuffer.getWidth() != canvasSize.width || workingBuffer.getHeight() != canvasSize.height)
               {
                  GraphicsConfiguration gcfg = getGraphicsConfiguration();
                  if(gcfg == null || canvasSize.width <= 0 || canvasSize.height <= 0)
                  {
                     // job failed b/c we couldn't create buffers
                     fireCanvasEvent(CanvasEventType.STOPPED, System.currentTimeMillis()-tJobStart, 0);
                     lastJobFailed = true;
                     continue;
                  }
                  if(workingBuffer != null)
                  {
                     workingBuffer.flush();
                     workingBuffer = null;
                  }
                  workingBuffer = gcfg.createCompatibleImage(canvasSize.width, canvasSize.height);
                  doFullRender = true;
               }

               // get the inverse of the rendering transform, which will transform canvas device coordinates to logical 
               // coordinates for the displayed graphic. Rendering transform should always be invertible!
               AffineTransform invXfm = null;
               try { invXfm = xfm.createInverse(); }
               catch( NoninvertibleTransformException ne ) {}
               if(invXfm == null) invXfm = new AffineTransform();

               // compute intersection of the bounds of the entire graphic with the buffer rect, in logical coords
               Rectangle2D rBuf = new Rectangle2D.Double(0, 0, canvasSize.width, canvasSize.height);
               rBuf = invXfm.createTransformedShape(rBuf).getBounds2D();
               if(r != null)
               {
                  Rectangle2D rGraphic = new Rectangle2D.Double(0, 0, r.getWidthMI(), r.getHeightMI());
                  Rectangle2D.intersect(rBuf, rGraphic, rBuf);
               }

               // special case: the graphic is completely outside of the buffer rect (this could happen when panning
               // in print preview mode). In this case, clear the entire working buffer and skip rendering altogether.
               boolean skipRender = rBuf.isEmpty();

               // prepare dirty regions in logical coordinates
               dirtyRegions.clear();
               if(!skipRender)
               {
                  if(doFullRender)
                  {
                     // in this case, the "dirty region" is the entire graphic, intersected with the buffer rect
                     dirtyRegions.add(rBuf);
                  }
                  else
                  {
                     // in this case, intersect the current job's dirty rects with the buffer rect. If the result is 
                     // not empty, include it in the list of dirty regions.
                     for(Rectangle2D rect : currentJob.dirtyRects) if((rect != null) && !rect.isEmpty())
                     {
                        Rectangle2D.intersect(rect, rBuf, rect);
                        if(!rect.isEmpty()) 
                           dirtyRegions.add(rect);
                     }
                  }
               }

               // clear dirty areas within buffer
               Graphics2D g2d = workingBuffer.createGraphics();
               g2d.setColor( RenderingCanvas.this.getBackground() );
               if(doFullRender || skipRender)
               {
                  // for a full rendering, or if graphic is entirely outside buffer, we clear the entire buffer
                  g2d.fillRect(0, 0, canvasSize.width, canvasSize.height);
               }
               else
               {
                  // in this case, we only want to render in the dirty areas. First, we must copy the current 
                  // backbuffer into the working buffer, because the working buffer could contain old stuff!
                  currentBufferLock.lock();
                  try
                  {
                     g2d.drawImage(currentBuffer, 0, 0, null);
                  }
                  finally
                  {
                     currentBufferLock.unlock();
                  }

                  // next, clear the relevant dirty rectangles and limit drawing to those regions. Each dirty rect is 
                  // expanded by 2pix to ensure we erase everything we need to (to account for antialiasing effects). 
                  // The clip area is the "sum" of all of these expanded dirty rectangles. NOTE that the fill and 
                  // clip calculations are done in pixel space.
                  //
                  // MacOSX-specific: Cannot use Area to defined clip shape in MacOSX Java; instead, we use the union 
                  // of the dirty rects.
                  Area clipArea = new Area();
                  Rectangle2D clipRect = new Rectangle2D.Double();
                  boolean isMac = Utilities.isMacOS();
                  for(Rectangle2D rLog : dirtyRegions)
                  {
                     Rectangle2D rDev = xfm.createTransformedShape(rLog).getBounds2D();
                     if(!rDev.isEmpty())
                     {
                        rDev.setRect(rDev.getX()-2, rDev.getY()-2, rDev.getWidth()+5, rDev.getHeight()+5);
                        g2d.fill(rDev);
                        rLog.setFrame(invXfm.createTransformedShape(rDev).getBounds2D());
                        if(isMac) Utilities.rectUnion(clipRect, rDev, clipRect);
                        else clipArea.add(new Area(rDev));
                     }
                  }
                  g2d.clip(isMac ? clipRect : clipArea);
               }

               // perform rendering as needed
               boolean success = true;
               if(r != null && !skipRender)
               {
                  // we always want nice-looking renderings
                  g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                  g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                  g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                  
                  // install the rendering transform so logical coordinates are correct
                  g2d.transform(xfm);

                  try
                  {
                     success = r.render(g2d, this);
                  }
                  catch(Exception e)
                  {
                      //noinspection CallToPrintStackTrace
                      e.printStackTrace();
                     success = false;
                  }
               }
               long tDone = System.currentTimeMillis();
               
               // clean up after render cycle completed/stopped
               if(!dieAsap) 
               {
                  if(success)
                  {
                     // success! swap working buffer with current backbuffer, then issue a repaint.
                     lastJobFailed = false;
                     currentBufferLock.lock();
                     try
                     {
                        BufferedImage tmp = currentBuffer;
                        currentBuffer = workingBuffer;
                        workingBuffer = tmp;
                     }
                     finally
                     {
                        currentBufferLock.unlock();
                     }
                     fireCanvasEvent(CanvasEventType.COMPLETED, tDone-tJobStart, 100);
                     doRepaint = true;

                     // elapsed time of completed job will serve as our "guess" for the next job!
                     tJobEstimate = tDone-tJobStart;

                     // if the *new* working buffer is not the right size, reallocate it
                     if(workingBuffer == null || workingBuffer.getWidth() != canvasSize.width || 
                           workingBuffer.getHeight() != canvasSize.height)
                     {
                        GraphicsConfiguration gcfg = getGraphicsConfiguration();
                        if(gcfg != null)
                        {
                           if(workingBuffer != null)
                           {
                              workingBuffer.flush();
                              workingBuffer = null;
                           }
                           workingBuffer = gcfg.createCompatibleImage(canvasSize.width, canvasSize.height);
                        }
                     }
                  }
                  else
                  {
                     // failure! if elapsed time is greater than our estimate, we have to adjust the estimate since the 
                     // job did not really finish
                     lastJobFailed = true;
                     long tElapsed = tDone-tJobStart;
                     if(tElapsed > tJobEstimate) tJobEstimate = tElapsed + 500;
                     double pctDone = tElapsed * 100.0 / tJobEstimate;
                     fireCanvasEvent(CanvasEventType.STOPPED, tElapsed, pctDone);
                 }
               }
            }
            finally
            {
               workingBufferLock.unlock();
            }

            if( doRepaint ) repaint();
         }
 
         // release buffers
         workingBufferLock.lock();
         try
         {
            if(workingBuffer != null) 
            {
               workingBuffer.flush();
               workingBuffer = null;
            }
         }
         finally
         {
            workingBufferLock.unlock();
         }

         currentBufferLock.lock();
         try
         {
            if(currentBuffer != null) 
            {
               currentBuffer.flush();
               currentBuffer = null;
            }
         }
         finally
         {
            currentBufferLock.unlock();
         }

      }

      /**
       * A list of rectangles covering the regions of a graphic that must be redrawn during the current rendering pass.
       * If it is empty, then the entire graphic must be redrawn.
       */
      private final List<Rectangle2D> dirtyRegions = new ArrayList<Rectangle2D>();

      public List<Rectangle2D> getDirtyRegions()
      {
         return( dirtyRegions );
      }

      /**
       * Each time this method is called, it will post a progress update event iff the previous progress update was 
       * more than 200ms ago. The appropriate <code>CanvasEvent</code> is posted on the Swing event queue to inform 
       * registered <code>CanvasListener</code>s of progress made in completing the current render task.
       * 
       * <p>We use the elapsed time of the last completed rendering job as the estimate of the current job's 
       * execution time. Then the percentage of job completed is the 100 times the actual elapsed job time divided by 
       * this estimate. Should the actual elapsed time exceed the estimate, the estimate is increased by 20% or 500ms, 
       * whichever is greater.</p>
       * 
       * @see RenderTask#updateProgress()
       */
      public boolean updateProgress()
      {
         // ignore if no render job is in progress
         if(!isRenderJobInProgress) return(false);

         // is it time for a progress update?
         long tElapsed = System.currentTimeMillis() - tJobStart;
         if(tLastProgressUpdate + 200 < tElapsed)
         {
            tLastProgressUpdate = tElapsed;

            // if elapsed time has exceeded our current job estimate, adjust the estimate upward!
            if(tElapsed > tJobEstimate)
               tJobEstimate = tElapsed + Math.max(2*tJobEstimate/10, 500);

            // fire progress update event
            RenderingCanvas.this.fireCanvasEvent(CanvasEventType.INPROGRESS, tElapsed, tElapsed*100.0/tJobEstimate);
         }
         // job should continue unless a new job is pending or the renderer thread needs to die
         return(!(isJobPending || dieAsap));
      }
   }
}
