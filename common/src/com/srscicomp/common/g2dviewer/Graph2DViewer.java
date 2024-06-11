package com.srscicomp.common.g2dviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.srscicomp.common.ui.JPreferredSizePanel;

/**
 * <code>Graph2DViewer</code> is a panel for viewing Java2D graphics encapsulated by a <code>RenderableModel</code>.
 * 
 * <p>The <code>Graph2DViewer/RenderableModel</code> framework is geared toward the display and dynamic updating of a 
 * complex vector graphic model which can take a significant amount of time to draw; thus, the framework is designed 
 * with the issues of rendering performance and GUI lockup in mind. Each "render job" is handled on a background thread, 
 * managed internally by the <code>Graph2DViewer</code> infrastructure. Rendering takes place in an offscreen "working" 
 * buffer, while the current contents of the screen are maintained in a separate buffer. This double-buffering mechanism 
 * allows the <code>Graph2DViewer</code> to repaint itself even while a rendering job is in progress. All of these 
 * mechanics are, for the most part, opaque to the <code>RenderableModel</code>, which is merely supplied a 
 * <code>Graphics2D</code> context in which to render itself.</p>
 * 
 * <p><code>RenderableModel</code> and related interfaces constitute all that the <code>Graph2DViewer</code> 
 * infrastructure "needs to know" about the graphics model that is rendered upon it:
 * <ul>
 *    <li><code>RenderableModel</code>. Represents the entire graphics model. It notifies the <code>Graph2DViewer</code> 
 *    whenever the model changes, and exposes the top-level graphics object that is to be displayed on the canvas. A 
 *    higher-level application object will typically hook the viewer to the model by invoking <code>setModel()</code>. 
 *    The viewer will register itself as a listener on the <code>RenderableModel</code>, so that it is notified whenever 
 *    the model changes. It will also notify the model when the mouse is clicked within its canvas, to support 
 *    hit-testing and the notion of "display focus".</li>
 *    <li><code>RenderableModelViewer</code>. This interface encapsulates all that the model "needs to know" about 
 *    <code>Graph2DViewer</code>. <code>Graph2DViewer</code> implements this interface and registers as a 
 *    <code>RenderableModelViewer</code> on its installed model. The model invokes methods on the interface to notify 
 *    the viewer when the model changes, when the display focus changes, or when it needs to measure text that is to be 
 *    rendered on the viewer's canvas (measuring text requires a graphics context).</li>
 *    <li><code>Renderable</code>. A piece of the model that renders itself. This interface defines the 
 *    <code>render()</code> method, which is invoked on the background renderer thread during a render cycle.</li>
 *    <li><code>RootRenderable</code>. This represents a top-level <code>Renderable</code> object that defines the 
 *    logical coordinate system in which itself and any other <code>Renderable</code>s it contains are expressed. By 
 *    design, the logical coordinate system is in units of milli-inches, with the origin at the bottom-left corner of 
 *    the <code>RootRenderable</code>'s bounding rectangle, the x-axis increasing left-to-right, and the y-axis 
 *    increasing bottom-to-top. The <code>RootRenderable</code> also specifies where the graphic's BL corner should be 
 *    placed relative to the BL corner of the printable area of a page -- to support a "print preview" mode. The 
 *    <code>Graph2DViewer</code> infrastructure uses this information to transform the <code>Graphics2D</code> context 
 *    of its offscreen buffers from device coordinates to the coordinates expected by the model!</li>
 *    <li><code>Focusable</code>. A <code>Renderable</code> node in the model that can also hold the "display focus". 
 *    A <code>RenderableModel</code> that supports display focus is responsible for keeping track of which node in the 
 *    model currently holds the focus. Any time the focus changes, it informs the <code>Graph2DViewer</code> via the 
 *    <code>RenderableModelViewer</code> interface. The viewer, in turn, requests a focus shape from the current 
 *    <code>Focusable</code>, and paints that shape on top of the current rendering, using a translucent light blue 
 *    fill so that the graphic shows through it.</li>
 * </ul>
 * </p>
 * 
 * <p><code>Graph2DViewer</code> itself is merely a JPanel-derived container that coordinates and integrates a number of 
 * different components. The heart of the viewer is the <code>RenderingCanvas</code>, which provides all of the 
 * infrastructure for actually rendering the graphic model on a background thread. Other (optional) components include 
 * a toolbar that exposes actions and settings exported by the <code>RenderingCanvas</code>, a horizontal ruler,
 * and a vertical ruler. These components are opaque as far as the graphic model is concerned. Most of them are 
 * implemented as package-private classes.</p>
 * 
 * <p>It is important that any implementation of the <code>RenderableModel</code> framework adequately decompose the 
 * rendering of the vector graphic model into a collection of much smaller pieces that can be rendered in a very short 
 * time. Why? If the model is changed by the user, the appearance of the graphic will change in some way -- so there's 
 * no point in completing the current rendering cycle, especially if it will take many seconds. So that a time-consuming 
 * rendering task can be cancelled in relatively short order without having to kill the background renderer thread from 
 * another thread, <code>Renderable</code>s are expected to check frequently to see if the task should be aborted. The 
 * <code>Renderable.render()</code> method supplies a <code>RenderTask</code> object, which is really a hook into 
 * synchronized methods of the <code>RenderingCanvas</code> itself. In addition to checking whether or not the task 
 * should be cancelled, <code>Renderable</code>s also send progress updates via the <code>RenderTask</code> hook. 
 * The idea here is that the <code>RootRenderable</code> is really a collection (typically a tree, although this is NOT 
 * a requirement) of <code>Renderable</code>s that decompose the entire graphic picture into component parts that can 
 * be rendered quickly. In its <code>render()</code> method, each <code>Renderable</code> should call
 * <code>RenderTask.shouldBeCancelled()</code> at regular intervals to see if the render job has been cancelled. If 
 * so, it would abort its rendering work immediately. In addition, the <code>Renderable</code> should call 
 * <code>RenderTask.updateTaskProgress()</code> to report progress made toward completing the rendering job.</p>
 * 
 * <p>[In the preceding discussion, notice that <code>RenderingCanvas</code> relies entirely on the model for timely 
 * cancellation of a stale rendering task and for task progress updates. We could have structured the framework so that 
 * the <code>RenderableModel</code> supplied the entire collection of <code>Renderable</code>s to the canvas's renderer 
 * thread, which would call the <code>render()</code> method on each <code>Renderable</code> in turn, checking in 
 * between to see if the task should be cancelled. We chose not to do it this way to give maximum flexibility to the 
 * model.]</p>
 * 
 * <p>Description of the rendering process.</p>
 * <p>Let's start by assuming that the background renderer thread is idle and that the <code>RenderableModel</code> has 
 * just been changed by user action. The model informs its registered <code>RenderableModelViewer</code> -- which is 
 * really the <code>Graph2DViewer</code> -- of the change (on the primary Swing event thread, of course). Upon receiving 
 * the notification, the <code>Graph2DViewer</code> updates its <code>RenderingCanvas</code> which, in turn, posts a 
 * "render task" for processing by the background renderer (again, the act of posting the task happens on the Swing 
 * event thread!). The task object includes the identity of the <code>RootRenderable</code> to be drawn, as well as the 
 * dirty regions that require updating. Much like Swing component paint events, all pending render tasks are coalesced 
 * into a single task, by combining the dirty regions.</p>
 * 
 * <p>The next time the renderer thread wakes up, it gets the pending task, prepares the backbuffer image for 
 * rendering, obtains a <code>Graphics2D</code> context from the image, and transforms it into the logical coordinate 
 * system defined by the <code>RootRenderable</code>, taking into account the current zoom/pan transform on the canvas 
 * itself. It then starts the rendering job by calling that object's <code>render()</code> method. From that point on, 
 * the renderer thread spends most of its time in the model's rendering code. Meanwhile, the user could make further 
 * changes to the graphic model as rendering proceeds, so other parts of the model's code will be touched on the Swing 
 * event thread. Obviously, then, both the <code>Graph2DViewer</code>'s <code>RenderingCanvas</code> and any 
 * <code>RenderableModel</code> implementation MUST deal with the thorny issues of thread synchronization. [One naive 
 * approach might involve making a deep clone of the model as part of the render task object; then additional changes 
 * in the original model won't corrupt the render pass in progress. However, this solution is impractical for complex, 
 * memory-intensive models.]</p>
 * 
 * <p>If the rendering operation completes successfully, the canvas's offscreen "working" buffer should now contain an 
 * up-to-date view of the graphic model. The renderer thread swaps the "working" and "current" buffers, then queues a 
 * request to repaint the canvas and returns to its idle state. Later, back in the Swing event thread, the paint 
 * request is processed by copying the contents of the "current" offscreen buffer (or some portion of it corresponding 
 * to the dirty regions!) to the canvas's primary surface. A translucent "display focus" highlight, if there is one, 
 * is painted on top of the graphics rendered.</p>
 * 
 * <p>What if the model is changed again while a rendering job is still in progress? In this case, the canvas posts a 
 * new render task when it receives notification of the model's change through its <code>Graph2DViewer</code> container. 
 * Shortly thereafter (assuming the model has been adequately partitioned into quickly rendered chunks), the model's 
 * rendering code will check its <code>RenderTask</code> and discover that the current rendering cycle should 
 * abort. The call to <code>RootRenderable.render()</code> that initiated that cycle returns false, indicating that the 
 * rendering was not completed. In this case, the background renderer thread checks for a pending render job and 
 * combines it with the one just cancelled. It then begins processing the new job. Of course, the canvas image actually 
 * displayed will remain out-of-sync with the new state of the graphic model -- but at least time won't be wasted 
 * completing a rendering pass that is already "stale".</p>
 * 
 * @see RenderableModel, RenderableModelViewer, Renderable, RootRenderable, RenderTask, RenderingCanvas
 * @author sruffner
 */
public class Graph2DViewer extends JPanel implements RenderableModelViewer
{
   /**
    * The canvas on which graphics are rendered. <code>Graph2DViewer</code> is simply a publicly visible container 
    * for this canvas.
    */
   private RenderingCanvas canvas = null;

   /** 
    * Construct a fully interactive <code>Graph2DViewer</code>, with scale-to-fit feature initially off and with a
    * 1-pixel border drawn.
    */
   public Graph2DViewer()
   {
      this(true, false, true);
   }

   /**
    * Construct a <code>Graph2DViewer</code> in interactive or non-interactive mode, with scale-to-fit feature
    * turned on and with a 1-pixel border drawn.
    * @param isInteractive
    */
   public Graph2DViewer(boolean isInteractive)
   {
      this(isInteractive, true, true);
   }
   
   /**
    * Construct a<code>Graph2DViewer</code> in interactive or read-only mode. In interactive mode, the viewer's 
    * canvas is accompanied by a tool bar and rulers which let the user scale the displayed graphic, switch view modes, 
    * and set the canvas resolution. In addition, several mouse-based interactions are supported:
    * <ul>
    *    <li>Click on the canvas to place the "display focus" on a particular portion of the displayed graphic (as long 
    *    as the underlying <code>RenderableModel</code> supports it).</li>
    *    <li>CTRL-click to recenter the picture about the point clicked (if possible).</li>
    *    <li>SHIFT-click to zoom in on the picture and center it about the point clicked.</li>
    *    <li>CTRL-press and drag to "grab" the picture and pan it in any direction (if possible).</li>
    * </ul>
    * 
    * <p>In non-interactive or read-only mode, the viewer lacks all of the above features. In this case, the underlying 
    * canvas is set up in scale-to-fit mode to ensure that the graphic is displayed entirely within its bounds.</p>
    * 
    * @param isInteractive If <code>true</code>, the viewer is configured to support user interactions, as described.
    * @param scaleToFit Turn scale-to-fit feature on/off. Primarily for non-interactive canvas. With an interactive
    * canvas, user can freely toggle the feature via the canvas toolbar.
    * @param border True to draw a 1-pixel gray border around the entire viewer; if false, border is omitted.
    */
   public Graph2DViewer(boolean isInteractive, boolean scaleToFit, boolean border)
   {
      super();

      // create the canvas
      canvas = new RenderingCanvas(isInteractive);
      canvas.setPixelsPerInch(103);
      canvas.setScaleToFitOn(scaleToFit);
      
      // canvas-only configuration...
      if(!isInteractive)
      {

         setLayout( new BorderLayout() );
         add(canvas, BorderLayout.CENTER);
         if(border) setBorder(BorderFactory.createLineBorder(Color.GRAY,1));

         Dimension minD = canvas.getMinimumSize();
         minD.width += 1;
         minD.height += 1;
         setMinimumSize(minD);
         return;
      }

      // create horizontal and vertical rulers and hook them into canvas
      CanvasRuler hRuler = new CanvasRuler(true);
      canvas.addListener(hRuler);
      CanvasRuler vRuler = new CanvasRuler(false);
      canvas.addListener(vRuler);
 
      // layout canvas and rulers in a panel with horizontal ruler aligned with bottom edge and vertical ruler aligned 
      // with left edge. The trick here is defining a layout that respects the fixed height of the horizontal ruler 
      // and the fixed width of the vertical ruler, letting the canvas use the remaining space.  PAIN IN THE A**!
      int rulerSz = CanvasRuler.getRulerDesignSize();
      JPanel row1 = new JPanel();
      row1.setLayout( new BoxLayout(row1, BoxLayout.LINE_AXIS) );
      JPanel vRulerPanel = new JPanel();
      vRulerPanel.setLayout( new BoxLayout(vRulerPanel, BoxLayout.PAGE_AXIS));
      vRulerPanel.add(vRuler);
      vRulerPanel.add(Box.createRigidArea(new Dimension(rulerSz,0)));
      row1.add(vRulerPanel);
      row1.add(canvas);

      JPanel row2 = new JPanel();
      row2.setLayout( new BoxLayout(row2, BoxLayout.LINE_AXIS) );
      row2.add(Box.createRigidArea(new Dimension(rulerSz,rulerSz)));
      row2.add(hRuler);

      JPanel canvasPanel = new JPanel();
      canvasPanel.setLayout( new BoxLayout(canvasPanel, BoxLayout.PAGE_AXIS));
      canvasPanel.add(row1);
      canvasPanel.add(row2);
 
      // put the canvas toolbar above the canvas
      JPanel toolbarPanel = new JPreferredSizePanel(false,true);
      toolbarPanel.setLayout(new BorderLayout());
      toolbarPanel.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
      toolbarPanel.add(canvas.getToolBarForCanvas(), BorderLayout.EAST);
      toolbarPanel.setBorder( BorderFactory.createMatteBorder(0,0,1,0,Color.DARK_GRAY) );

      setLayout( new BoxLayout(this, BoxLayout.PAGE_AXIS) );
      add(toolbarPanel);
      add(canvasPanel);
      if(border) setBorder(BorderFactory.createLineBorder(Color.GRAY,1));
      
      // calc min size of viewer in interactive mode -- include minimum sizes of toolbar & rulers
      Dimension minD = canvas.getMinimumSize();
      minD.width += rulerSz + 2;
      minD.height += rulerSz  + 2;
      Dimension tbDim = canvas.getToolBarForCanvas().getMinimumSize();
      if(minD.width < tbDim.width) minD.width = tbDim.width + 10;
      minD.height += tbDim.height + 2;
      setMinimumSize(minD);
   }

   /**
    * Get the current resolution of the <code>Graph2DViewer</code>'s canvas.
    * 
    * @return Current resolution in pixels per inch.
    */
   public double getResolution()
   {
      return( (canvas != null) ? canvas.getPixelsPerInch() : 0 );
   }

   /**
    * Set the current resolution of the <code>Graph2DViewer</code>'s canvas.
    * 
    * @param dpi Desired resolution in pixels per inch. Will be range-restricted to [50..200].
    */
   public void setResolution(double dpi)
   {
      if(canvas != null)
         canvas.setPixelsPerInch(dpi);
   }

   /**
    * Register a listener with the viewer's underlying graphics canvas delegate.
    * @param l The canvas listener to add.
    */
   public void addCanvasListener(CanvasListener l) { if(canvas != null && l!=null) canvas.addListener(l); }
   
   /**
    * Unregister a listener with the viewer's underlying graphics canvas delegate.
    * @param l The canvas listener to remove.
    */
   public void removeCanvasListener(CanvasListener l) { if(canvas != null && l!=null) canvas.removeListener(l); }
   
   /**
    * Disposes of resources related to the rendering infrastructure of <code>Graph2DViewer</code>. This method should 
    * only be called when disposing of the viewer. Once invoked, the viewer is no longer operational!
    */
   public void releaseResources()
   {
      setVisible(false);
      if(canvas != null)
      {
         canvas.releaseRenderingResources();
         canvas = null;
      }
      if(renderedModel != null)
      {
         renderedModel.unregisterViewer(this);
         renderedModel = null;
      }
   }

   /**
    * Return a snapshot of the current viewer contents within the specified rectangle.
    * @param r Rectangular bounds of requested snapshot, in model coordinates. If <code>null</code>, method retrieves 
    * a snapshot of the entire model or at least the portion currently visible on the viewer canvas, given the current
    * scaling and panning state.
    * @return A <code>BufferedImage</code> containing the requested snapshot. Returns <code>null</code> if unable to 
    * create the snapshot image (if renderer is busy, for example), if no model is currently installed in the viewer, 
    * or if the specified rectangle, when converted to device pixels, lies entirely outside the canvas bounds.
    */
   public BufferedImage takeSnapshot(Rectangle2D r)
   {
      if(canvas == null || renderedModel == null) return(null);
      if(r == null)
      {
         double x = 0; 
         double y = 0;
         RootRenderable root = renderedModel.getCurrentRootRenderable();
         if(canvas.isPrintPreviewOn())
         {
            Point2D p = root.getPrintLocationMI();
            x = p.getX();
            y = p.getY();
         }
         r = new Rectangle2D.Double(x, y, root.getWidthMI(), root.getHeightMI());
      }
      return(canvas.captureCanvasImage(r));
   }
   
   /** 
    * Is the specified point within this <code>Graph2DViewer</code>'s rendering canvas?
    * @param p A point in screen coordinates. It is converted to canvas device coordinates (pixels, origin at UL corner
    * of canvas, y-axis increasing downward, x-axis increasing rightward).
    * @return <code>True</code> iff the viewer's canvas (and NOT the toolbar or rulers!) contains the point.
    */
   public boolean isInsideCanvas(Point p)
   {
      if(p == null || canvas == null) return(false);
      SwingUtilities.convertPointFromScreen(p, canvas);
      return(canvas.contains(p));
   }
   
   /**
    * Convert the specified point from screen coordinates to logical model coordinates.
    * @param p A point in screen coordinates.
    * @return The point in logical model coordinates. If the point is not within the canvas or if no model is currently
    * installed in the viewer, method returns <code>null</code>.
    */
   public Point2D screenToLogicalPoint(Point p)
   {
      if(renderedModel == null || !isInsideCanvas(p)) return(null);
      Point2D pLog = new Point2D.Float();
      canvas.deviceToLogical(p.x, p.y, pLog);
      return(pLog);
   }

   
   //
   // RenderableModelViewer
   //

   /**
    * The model containing the top-level graphic node displayed on this <code>Graph2DViewer</code>'s canvas.
    */
   private RenderableModel renderedModel = null;

   /** 
    * Get the <code>RenderableModel</code> currently displayed in this <code>Graph2DViewer.
    * @return The renderable graphic model. Could be <code>null</code> if no mode is currently installed in viewer.
    */
   public RenderableModel getModel() { return(renderedModel); }

    /**
    * Install a <code>RenderableModel</code> on this <code>Graph2DViewer</code>.
    * 
    * <p>The viewer will register itself with the <code>RenderableModel</code> specified, reset the zoom and pan 
    * transform on its canvas, and trigger a full refresh in order to display the model in its current state.</p>
    * 
    * <p>If another model was already installed, <code>Graph2DViewer</code> first unregisters itself with that model 
    * before releasing any reference to it. For safety's sake, this method should only be invoked on the 
    * Swing event thread -- if it was invoked in another thread, it is possible that the viewer could receive a stale 
    * notification from the old model that was pending on the Swing event thread when it was preempted by the thread 
    * on which this method was called.</p>
    * 
    * @param rm The <code>RenderableModel</code> to be displayed in this viewer.  If <code>null</code>, then there is 
    * nothing to render, and the canvas will be blank. If the specified model already has a registered viewer, the 
    * method will fail, and the viewer will be left unconnected to any model!
    */
   public void setModel(RenderableModel rm)
   {
      if(canvas == null) return;

      // if model is already installed, do nothing
      if(renderedModel == rm) return;
      
      // if a model is already installed on the viewer, unregister the viewer with that model
      if(renderedModel != null)
         renderedModel.unregisterViewer(this);
 
      // install the new model and register with the model as its viewer if we can. If we cannot because another viewer 
      // is already registered, then we're left with no model installed!
      renderedModel = rm;
      if(renderedModel != null)
      {
         if(!renderedModel.registerViewer(this))
            renderedModel = null;
      }

      // update the canvas
      canvas.setRenderableModel(renderedModel);
   }

   public void modelMutated(List<Rectangle2D> dirtyRects)
   {
      // update canvas rendering
      if(canvas != null) canvas.updateRendering(dirtyRects);
   }

   public void focusChanged()
   {
      // update the focus highlight on canvas
      if(canvas != null) canvas.updateFocusHighlight();
   }

   public Graphics2D getViewerGraphicsContext()
   {
      Graphics2D g2d = null;
      if(canvas != null) 
         g2d = (Graphics2D) canvas.getRenderingGraphics();
      return(g2d);
   }
   

   /**
    * Get a copy of the printed page format currently installed on this <code>Graph2DViewer</code>'s canvas. It 
    * describes the size, orientation, and imageable rectangle of the printed page; the canvas uses this page layout 
    * only when displaying graphics in "print preview" mode.
    * 
    * @return An independent copy of the installed <code>PageFormat</code>.
    */
   public PageFormat getPageFormat()
   {
      return((canvas !=null) ? canvas.getPageFormat() : new PageFormat());
   }

   /** 
    * Change the current page format installed on this <code>Graph2DViewer</code>'s canvas. If the page layout has 
    * changed in any way, the canvas is refreshed automatically and its zoom & pan transform reset.
    * 
    * @param pgf The new page format. If <code>null</code>, the page format is restored to a default state: letter 
    * size, portrait orientation, with half-inch margins all around.
    */
   public void setPageFormat(PageFormat pgf)
   {
      if(canvas != null) canvas.setPageFormat(pgf);
   }

   /**
    * Is the <code>Graph2DViewer</code> in "print preview" mode?
    * 
    * @return <code>True</code> if print preview is enabled.
    */
   public boolean isPrintPreviewEnabled()
   {
      return(canvas != null && canvas.isPrintPreviewOn());
   }

   /**
    * Enable or disable "print preview" mode. In this mode, the viewer displays the rendered graphic within the 
    * context of a printed page. The page layout is defined by the viewer's current <code>PageFormat</code>.
    * 
    * @param ena <code>True</code> to enable print preview, <code>false</code> to disable.
    */
   public void setPrintPreviewEnabled(boolean ena)
   {
      if(canvas != null) canvas.setPrintPreviewOn(ena);
   }
   
   /** 
    * Enable/disable the focus highlight feature on a non-interactive viewer. The viewer's canvas is repainted to 
    * reflect the change. If the viewer is interactive, this method has no effect: the focus highlight is always 
    * enabled in this case. 
    * @param ena True to enable, false to disable the focus highlight.
    */
   public void setFocusHighlightEnabled(boolean ena) { if(canvas != null) canvas.setFocusHighlightEnabled(ena); }

   /**
    * Get the aspect ratio X:Y for the graphic currently rendered in this viewer. If print preview mode is on, the 
    * method returns the ratio of the imageable width over the imageable height in the current page format. Otherwise,
    * it returns the ratio of the rendered model's width over height.
    * @return The current graphic's aspect ratio, as described. If no model is installed in the viewer, returns 1.0.
    */
   public double getAspectRatio()
   {
      double aspect = 1.0;
      if(canvas.isPrintPreviewOn())
         aspect = canvas.getPageFormat().getImageableWidth() / canvas.getPageFormat().getImageableHeight();
      else if(renderedModel != null)
      {
         RootRenderable root = renderedModel.getCurrentRootRenderable();
         if(root != null)
            aspect = root.getWidthMI() / root.getHeightMI();
      }
      return(aspect);
   }
   
   /** Refresh the entire graphic currently rendered in this viewer. This repaints the entire client area. */
   public void refresh() { if(canvas != null) canvas.updateRendering(null); }
   
   /** 
    * Is this viewer currently in the scale-to-fit mode? In this view mode, the rendered graphic is always scaled to
    * ensure that it is fully visible within the current bounds of the rendering canvas; zooming and panning operations
    * are disabled.
    * @return True if viewer is configured in scale-to-fit mode; else false.
    */
   public boolean isScaleToFitOn() { return(canvas != null && canvas.isScaleToFitOn()); }
   
   /**
    * Turn this viewer's scale-to-fit mode on/off. This method provides a programmatic means of changing the view mode;
    * when the viewer is configured as interactive, it includes a toolbar button that lets the user can toggle in and 
    * out of scale-to-fit mode. If the mode is changed, the viewer redraws the graphic accordingly.
    * @param on True/false to enable/disable scale-to-fit mode.
    */
   public void setScaleToFitOn(boolean on) { if(canvas != null) canvas.setScaleToFitOn(on); }
   
   /**
    * Zoom in on the specified rectangle within the viewer's rendering canvas, magnifying and panning the rendered
    * graphic so that -- to the extent possible while still maintaining the graphic's aspect ratio and the canvas's 
    * maximum supported scale factor -- the portion inside the rectangle fills the canvas viewport
    * <p><i>This method offers a programmatic means of zooming in on a graphic installed in a viewer that may or may not
    * be interactive. If the scale-to-fit mode is engaged, it will necessarily be turned off in order to perform the
    * zoom and pan transformation required.</i></p>
    * 
    * @param zoomR The rectangle defining the portion of the viewer canvas that should be magnified and centered in 
    * place. It must be specified in canvas device coordinates (pixels, origin at top-left corner, Y increasing down).
    */
   public void zoomIn(Rectangle zoomR) { if(canvas != null) canvas.zoomIn(zoomR); }

   /**
    * If possible, zoom in or out and pan the displayed graphic so that the logical location corresponding to the 
    * specified canvas device location is centered on the canvas after zooming. If the scale-to-fit mode is enabled, it
    * is disabled before zooming.
    * 
    * @param x The x-coordinate of a location on the canvas in canvas device pixels, prior to rescaling. 
    * @param y The y-coordinate.
    * @param magnify If true, zoom in on graphic (scale x 2); else zoom out (scale / 2). Zooming may not be  possible if
    * the current scale factor is already up against one of the limits of its range. In this case, the method will
    * still, if possible, pan the canvas so that the specified point is centered in the view.
    */
   public void zoomAndPanTo(int x, int y, boolean magnify)
   { 
      if(canvas != null)
      {
         if(canvas.isScaleToFitOn()) canvas.setScaleToFitOn(false);
         canvas.zoomAndPanTo(x, y, magnify); 
      }
   }
   
   /**
    * Is it possible to pan the installed graphic within the viewport of the viewer's rendering canvas?
    * @return True if panning is currently possible.
    */
   public boolean canPan() { return(canvas != null && canvas.canPan()); }
   
   /**
    * To the extent possible, pan the installed graphic by the specified amounts within the viewport of the viewer's
    * rendering canvas.
    * <p><i>This method offers a programmatic means of panning a graphic installed in a viewer that may or may not be
    * be interactive. If the scale-to-fit mode is engaged, NO action is taken.</i></p>
    * 
    * @param dx Horizontal pan adjustment in device pixels. A negative (positive) value moves the canvas viewport left
    * (right) WRT the rendered graphic; since the canvas viewport doesn't move, the effect is to move the graphic to
    * the right (left).
    * @param dy Vertical pan adjustment in device pixels. A negative (positive) value moves the canvas viewport up
    * (down) WRT the rendered graphic; since the canvas viewport doesn't move, the effect is to move the graphic to
    * the down (up).
    */
   public void panBy(double dx, double dy) { if(canvas != null) canvas.panBy(dx, dy); }
   
   /**
    * Reset the rendering canvas's current scale factor to 1.0 and the current panning position to (0, 0), and 
    * re-render the displayed graphic model accordingly. No action is taken if scale-to-fit mode is enabled.
    */
   public void resetZoomAndPan() { if(canvas != null) canvas.resetZoomAndPan(); }
   
   /**
    * Transform a canvas location in canvas device pixels to the logical coordinate system of the graphic displayed, 
    * taking into account print preview mode and the current zoom & pan transform.
    * 
    * @param x X-coordinate of a point on canvas in device pixels (origin at canvas top-left corner, x-axis increasing 
    * rightward, y-axis increasing downward). 
    * @param y Y-coordinate of the canvas location.
    * @param ptLogical If not null, this point is modified to contain the same location expressed in milli-inches in the
    * coordinate system of the zoomed and panned graphic. If it is null, a new {@link Point2D Point2D} is
    * allocated on the heap. Regardless, the modified point is the returned value of the method. 
    * @return The canvas location (x,y) expressed in the logical coordinate system of the displayed graphic.
    */
   public Point2D deviceToLogical(int x, int y, Point2D ptLogical)
   {
      if(canvas != null) ptLogical = canvas.deviceToLogical(x, y, ptLogical);
      return(ptLogical);
   }
   
   /**
    * Transform a point expressed in the logical coordinate system of the graphic displayed to a canvas location in
    * canvas device pixels, given the current preview mode and zoom and pan transform.
    * @param p The point in logical coordinates. Upon return, it contains the point in canvas pixels. If null, no action
    * is taken.
    */
   public void logicalToDevice(Point2D p) { if(canvas != null) canvas.logicalToDevice(p); }
   
   /**
    * Transform a rectangle specified in the logical coordinate system of the graphic displayed to the canvas's own
    * device coordinate system in pixels, taking into account print preview mode and the current zoom and pan transform.
    * @param rLog The rectangle in logical coordinates. If null, the method returns null.
    * @return The rectangle expressed in canvas device coordinates IAW the canvas's current state. 
    */
   public Rectangle2D logicalToDevice(Rectangle2D rLog)
   {
      return(canvas == null ? null : canvas.logicalToDevice(rLog));
   }
   
   /**
    * Transform a shape specified in the logical coordinate system of the graphic displayed to the canvas's own device
    * coordinate system in pixels, taking into account print preview mode and the current zoom and pan transform.
    * @param shapeLog The shape in logical coordinates. If null, the method returns null.
    * @return The shape expressed in canvas device coordinates IAW the canvas's current state. 
    */
   public Shape logicalToDevice(Shape shapeLog)
   {
      return(canvas == null ? null : canvas.logicalToDevice(shapeLog));
   }
}
