package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.srscicomp.common.functionparser.FunctionParser;
import com.srscicomp.common.g2dutil.Painter;
import com.srscicomp.common.g2dutil.PolylinePainter;
import com.srscicomp.common.g2dutil.ShapePainter;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;

/**
 * <code>FunctionNode</code> is an implementation of <code>FGNPlottable</code> representing a mathematical function 
 * <em>y=f(x)</em> that can be rendered on a 2D graph. The function definition string specifies <em>f(x)</em> as a 
 * function of the primary coordinate <em>x</em> (or <em>theta</em>) in a somewhat natural way, e.g., 
 * "2*x*sin( 100 * x )". <code>FunctionNode</code> parses the definition string and, if successful, evaluates it at 
 * <em>[x0, x0+dx, ..., x0+N*dx, ..., x1]</em>, where range <em>[x0 .. x1]</em> and sample interval <em>dx</em> are all 
 * properties of the node. Note that only one data point will be evaluated when the sign of <em>dx</em> is such that 
 * <em>(x0 + dx) &lt; x0 &lt; x1</em>, or vice versa.</p>
 * 
 * <p>The set of points <em>{x, f(x)}</em> are then rendered in the data box of the parent <code>GraphNode</code> as a 
 * connected polyline. If there are any undefined points in the set (ie, the function evaluates to infinity or NaN at a 
 * particular value of x), the polyline will have a "gap" in it where the undefined point should be. A symbol can be 
 * rendered at each point, if desired; its appearance is governed by a single intrinsic <code>SymbolNode</code> child. 
 * No error bars are supported, since this node represents a mathematically evaluated function.</p>
 * 
 * <p>If the definition string is not parsable as a mathematical function, then <code>FunctionNode</code> is not 
 * rendered. Internally, <code>FunctionNode</code> uses a <code>FunctionParser</code> to parse and evaluate the function
 * string. </p>
 * 
 * <p><code>FunctionNode</code> has the typical styling attributes that govern the appearance of the polyline. The 
 * <em>title</em> property serves to identify the element in parent graph's automated legend, while the 
 * <em>showInLegend</em> flag (in abstract superclass <code>FGNPlottable</code>) can be cleared to exclude an entry for 
 * the function from that legend.</p> 
 * 
 * @see FunctionParser
 * @author 	sruffner
 */
public class FunctionNode extends FGNPlottable implements Cloneable
{
   /**
    * Construct a <code>FunctionNode</code> evaluating the function <em>f(x) = x</em> between 0 and 10, with a sample 
    * interval of 1. All styling attributes are initially implicit (inherited). It has no title, and its automated 
    * legend entry is enabled. It contains a single intrinsic <code>SymbolNode</code> component, which governs the 
    * appearance of any marker symbols in the function's rendering.
    */
   public FunctionNode()
   {
      super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR|HASTITLEATTR|ALLOWATTRINTITLE);
      setTitle("");
      x0 = 0;
      x1 = 10;
      dx = 1;

      addComponentNode( new SymbolNode() );
      parser = new FunctionParser("x");
      recalcDataRange(null);
   }

   
   //
   // Support for child nodes
   //
   
   @Override
   public FGNodeType getNodeType() { return(FGNodeType.FUNCTION); }

   /**
    * Overridden to return <code>true</code>, since a <code>FunctionNode</code> does not admit any removable children.
    * 
    * @see FGraphicNode#isLeaf()
    */
   @Override
   public boolean isLeaf() { return(true); }

   @Override
   public boolean canInsert(FGNodeType nodeType) { return(false); }

   
   //
   // Properties
   //

   /**
    * The beginning of the interval over which function is evaluated (inclusive).
    */
   private double x0;

   /**
    * Get the beginning of the interval in <em>x</em> (or <em>theta</em>, in polar graph context) over which this 
    * <code>FunctionNode</code> is evaluated.
    * 
    * @return The start of the interval (inclusive) over which mathematical function is evaluated.
    */
   public double getX0() { return(x0); }
   
   /**
    * Set the beginning of the interval in <em>x</em> (or <em>theta</em>, in polar graph context) over which this 
    * <code>FunctionNode</code> is evaluated. If a change is made, <code>onNodeModified()</code> is invoked.
    * 
    * @param x0 The interval starting value. Will be rejected if NaN or infinite.
    * @return <code>True</code> iff value was accepted.
    */
   public boolean setX0(double x0) 
   {
      if(!Utilities.isWellDefined(x0)) return(false);
      if(this.x0 != x0)
      {
         if(doMultiNodeEdit(FGNProperty.X0, new Double(x0))) return(true);
         
         Double oldX0 = new Double(this.x0);
         this.x0 = x0;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.X0);
            FGNRevEdit.post(this, FGNProperty.X0, new Double(this.x0), oldX0);
         }
      }
      return(true);
   }

   /**
    * The end of the interval over which function is evaluated (inclusive).
    */
   private double x1;

   /**
    * Get the end of the interval in <em>x</em> (or <em>theta</em>, in polar graph context) over which this 
    * <code>FunctionNode</code> is evaluated.
    * 
    * @return The end of the interval (inclusive) over which mathematical function is evaluated.
    */
   public double getX1() { return(x1); }
   
   /**
    * Set the end of the interval in <em>x</em> (or <em>theta</em>, in polar graph context) over which this 
    * <code>FunctionNode</code> is evaluated. If a change is made, <code>onNodeModified()</code> is invoked.
    * 
    * @param x1 The interval ending value. Will be rejected if NaN or infinite.
    * @return <code>True</code> iff value was accepted.
    */
   public boolean setX1(double x1) 
   {
      if(!Utilities.isWellDefined(x1)) return(false);
      if(this.x1 != x1)
      {
         if(doMultiNodeEdit(FGNProperty.X1, new Double(x1))) return(true);
         
         Double oldX1 = new Double(this.x1);
         this.x1 = x1;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.X1);
            FGNRevEdit.post(this, FGNProperty.X1, new Double(this.x1), oldX1);
         }
      }
      return(true);
   }

   /**
    * The function sample interval.
    */
   private double dx;

   /**
    * Get the function sample interval. This node's mathematical function is evaluated at <em>[x0, x0+dx, x0+2*dx, .. 
    * x1]</em>, where <em>dx</em> is the sample interval and <em>[x0 x1]</em> is the range over which the function is 
    * evaluated.
    * 
    * @return The function sample interval.
    */
   public double getDX() { return(dx); }
   
   /**
    * Set the function sample interval. If a change is made, <code>onNodeModified()</code> is invoked.
    * 
    * <p>Note that a sample interval of zero is acceptable. In this degenerate case, the function will only be evaluated 
    * at one value, the start of the evaluation interval (<code>getX0()</code>).
    * 
    * @param dx The new sample interval. Will be rejected if NaN or infinite.
    * @return <code>True</code> iff value was accepted.
    */
   public boolean setDX(double dx) 
   {
      if(!Utilities.isWellDefined(dx)) return(false);
      if(this.dx != dx)
      {
         if(doMultiNodeEdit(FGNProperty.DX, new Double(dx))) return(true);
         
         Double oldDX = new Double(this.dx);
         this.dx = dx;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.DX);
            FGNRevEdit.post(this, FGNProperty.DX, new Double(this.dx), oldDX);
         }
      }
      return(true);
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
         case X0: ok = setX0((Double)propValue); break;
         case X1: ok = setX1((Double)propValue); break;
         case DX: ok = setDX((Double)propValue); break;
         default: ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
         case X0: value = new Double(getX0()); break;
         case X1: value = new Double(getX1()); break;
         case DX: value = new Double(getDX()); break;
         default: value = super.getPropertyValue(p); break;
      }
      return(value);
   }

   
   //
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet() { return(true); }

   /** The only node-specific property exported in a function node's style set is the include-in-legend flag. */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.LEGEND, new Boolean(getShowInLegend()));
   }
   
   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.LEGEND, null, Boolean.class);
      if(b != null && !b.equals(restore.getStyle(FGNProperty.LEGEND)))
      {
         setShowInLegendNoNotify(b);
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LEGEND);
      
      return(changed);
   }
   
   
   //
   // Evaluating the function
   //

   /**
    * This parser stores, parses, and evaluates the <code>FunctionNode</code>'s function definition string.
    */
   private FunctionParser parser;

   /**
    * Get the function string defining the mathematical function <em>f(x)</em> currently rendered by this 
    * <code>FunctionNode</code>. The string is provided as is, and may not represent a valid function.
    * 
    * @return The function definition string.
    */
   public String getFunctionString() { return(parser.getDefinition()); }

   /**
    * Set the function string defining the mathematical function <em>f(x)</em> rendered by this 
    * <code>FunctionNode</code>. If the string has changed at all, the method onNodeModified() is invoked to force a 
    * re-rendering of the containing graphic model in the regions affected by the change.
    * 
    * @param defn The new function definition string. Any value, including <code>null</code> is accepted. If it does 
    * not represent a valid function, this node will render nothing.
    */
   public void setFunctionString(String defn)
   {
      String oldFormula = getFunctionString();
      if(defn.equals(oldFormula)) return;
      parser.setDefinition(defn);
      if(areNotificationsEnabled())
      {
         onNodeModified(null);
         FGNRevEdit.postFunctionFormulaChange(this, oldFormula);
      }
   }

   /**
    * Does this <code>FunctionNode</code>'s current function definition string represent a valid function 
    * <em>y=f(x)</em>? If not, call <code>getReasonFunctionInvalid()</code> for an explanatory message.
    * 
    * @return <code>True</code> iff the current function string is valid (ie, it was parsed successfully).
    */
   public boolean isFunctionValid()
   {
      return(parser.isValid());
   }

   /**
    * If the function string currently defined in this <code>FunctionElement</code> is not valid, this method returns a 
    * brief description explaining where and why the parser failed.
    * 
    * @return Description of why function string could not be parsed; an empty string if function string is valid.
    */
   public String getReasonFunctionInvalid()
   {
      if(parser.isValid()) return("");
      else return("Parse failed at index " + parser.getParseErrorPos() + ": " + parser.getParseErrorReason());
   }

   /**
    * The number of points at which this <code>FunctionNode</code> is evaluated is determined by the assigned range 
    * <em>[x0..x1]</em> and the sample interval, <em>dx</em>. This method calculates the number of points based on 
    * the current values of these properties. 
    * 
    * <p>If the sample interval <em>dx</em> is such that the second value <em>(x0 + dx)</em> falls outside the current
    * range, then the method returns 1 and the function will only be evaluated at <em>x0</em>. In the degenerate case
    * <em>dx==0</em>, the method returns 1 also. Finally, if the function definition string is invalid, the method 
    * returns 0.</p>
    * 
    * @return Number of points at which function is evaluated. <i>This is NOT necessarily the number of points actually
    * rendered, since the function may be ill-defined at some values of x in [x0..x1]!</i>
    */
   int getDataSize()
   {
      if(!parser.isValid()) return(0);
      int n = 1;
      if((dx != 0) && ((x0 < x1 && dx > 0) || (x0 > x1 && dx < 0)))
         n += (int) Math.rint((x1-x0)/dx);
      return(n);
   }

   /**
    * Get the x-coordinate (theta coordinate in a polar plot) for the specified point in the set of points at which 
    * this <code>FunctionNode</code> is evaluated. The set of x-coordinates is given by <em>x(i) = x0 + i*dx</em> for 
    * <em>i = [0..N-1]</em>, where <em>N</em> is the value returned by <code>getDataSize()</code>. 
    * 
    * @param i Index into the set of points at which this <code>FunctionNode</code> is evaluated.
    * @return The x-coordinate of the <em>i</em>-th evaluated point in this <code>FunctionNode</code>.
    * @throws IndexOutOfBoundsException if <em>i</em> is not in the interval <em>[0..N-1]</em>, or if the function 
    * definition is currently invalid.
    */
   double getX(int i) throws IndexOutOfBoundsException
   {
      if(i < 0 || i >= getDataSize()) throw new IndexOutOfBoundsException();
      return(x0 + ((double)i)*dx);
   }

   /** 
    * Get the value of the function <em>y=f(x)</em> for the specified point in the set of points at which this 
    * <code>FunctionNode</code> is evaluated. The value returned could be a finite value, +/-infinity, or NaN.
    * 
    * @param i Index into the set of points at which this <code>FunctionNode</code> is evaluated.
    * @return The value of the function <em>f(x)</em> when <em>x</em> has the value returned by <code>getX(i)</code>.
    * @throws IndexOutOfBoundsException if <em>i</em> is not in the interval <em>[0..N-1]</em>, or if the function 
    * definition is currently invalid.
    */
   double getY(int i) throws IndexOutOfBoundsException
   {
      return(parser.evaluate(getX(i)));
   }

   /**
    * Return an array of the points <em>{x, f(x)}</em> that trace out this <code>FunctionNode</code> when it is 
    * rendered, in plotting order. 
    * 
    * @return The array of points {x, f(x)} defined by this <code>FunctionNode</code>, with coordinates in milli-inches
    * WRT the parent graph's view port. If the function string is currently invalid, an empty array is returned.
    */
   private Point2D[] getRenderedCoords()
   {
      if(getDataSize() == 0 || !isFunctionValid()) return(new Point2D[0]);

      // NOTE: We use PointProducer so that the same points are generated as when the function is rendered. When the
      // number of point in the funciton exceeds a certain limit, polyline simplification kicks in to reduce rendering
      // time. We want the PS output to be the same as what is rendered onscreen.
      
      PointProducer prod = new PointProducer();
      ArrayList<Point2D> pts = new ArrayList<Point2D>();
      pts.ensureCapacity(getDataSize());
      while(prod.hasNext())
      {
         // note: PointProducer reuses a Point2D for each point, so we have to allocate a new Point2D each time!
         Point2D next = prod.next();
         pts.add(new Point2D.Double(next.getX(), next.getY()));
      }
      
      Point2D[] coords = pts.toArray(new Point2D[0]);
      pts.clear();
      pts.trimToSize();
      
      return(coords);
   }


   // 
   // FGNPlottable
   //
   
   /**
    * When the parent graph is polar, the initial function is set to "Yo + A*x/(2*pi)", where A is the radial axis 
    * range and Yo is the start of that range. Otherwise, a sinusoidal function is created that is intended to span
    * the current X-axis range and lie in the middle of the Y-axis range.
    */
   @Override protected void initializeDefaultData(double[] axisRng, boolean isPolar)
   {  
      double y0 = axisRng[2];
      double y1 = axisRng[3];
      StringBuilder sb = new StringBuilder();
      if(isPolar)
      {
         x0 = 0;
         x1 = 360;
         dx = 5;
         
         if(y0 > 0) sb.append(Utilities.toString(y0, 6, 3)).append(" + ");
         sb.append(Utilities.toString(y1-y0, 6, 3)).append("*x/360");
      }
      else
      {
         x0 = axisRng[0];
         x1 = axisRng[1];
         dx = (x1-x0) / 100.0;
         
         sb.append(Utilities.toString((y1+y0)/2.0, 6, 3)).append(" + ").append(Utilities.toString((y1-y0)/4.0, 6, 3));
         sb.append("*sin(2*pi*x/").append(Utilities.toString(x1-x0, 6, 3)).append(")");
         
      }
      parser.setDefinition(sb.toString());
   }

   @Override protected boolean recalcDataRange(Object hint)
   {
      boolean needRecalc = (hint == null) || hint == FGNProperty.X0 || hint == FGNProperty.X1 || hint == FGNProperty.DX;
      if(!needRecalc) return(false);
      
      double minX = Double.MAX_VALUE;
      double maxX = -Double.MAX_VALUE;
      double minY = Double.MAX_VALUE;
      double maxY = -Double.MAX_VALUE;

      int n = getDataSize();
      if(n == 0) 
      {
         minX = maxX = minY = maxY = 0.0;
      }
      else
      {
         double x = x0;
         for(int i = 0; i < n; i++ )
         {
            double y = parser.evaluate(x);
            if(Utilities.isWellDefined(y))
            {
               if(x < minX) minX = x;
               if(x > maxX) maxX = x;
               if(y < minY) minY = y;
               if(y > maxY) maxY = y;
            }
            x += dx;
         }
      }

      boolean changed = (cachedDataRange[0] != (float)minX) || (cachedDataRange[1] != (float)maxX);
      changed = changed || (cachedDataRange[2] != (float)minY) || (cachedDataRange[3] != (float)maxY);
      if(changed)
      {
         cachedDataRange[0] = (float) minX;
         cachedDataRange[1] = (float) maxX;
         cachedDataRange[2] = (float) minY;
         cachedDataRange[3] = (float) maxY;
      }
      return(changed);
   }


   @Override public SymbolNode getSymbolNode() { return((SymbolNode) getComponentNodeAt(0)); }

   /** A function node will always render marker symbols of nonzero size. */
   @Override public boolean usesSymbols() { return(true); }


   //
   // Focusable/Renderable support
   //

   
   /**
    * The focus shape for a <code>FunctionNode</code> is the rectangle that bounds the current rendering of the function
    * trace. If the parent graph clips its data to the data window, then the bounding rectangle is likewise clipped.
    * 
    * <p>For performance reasons, the cached bounding rectangle is used if it is available; it is recalculated only if
    * it is not yet defined. In the latter case, if the rectangle could not be calculated, method returns null.</p>
    */
   public Shape getFocusShape(Graphics2D g2d)
   {
      if(rBoundsSelf == null) getRenderBoundsForSelf(g2d, false);
      return(rBoundsSelf == null ? null : getLocalToGlobalTransform().createTransformedShape(rBoundsSelf));
   }

   /**
    * The function node is considered "hit" by the specified point if that point is within a radius of 100 milli-inches 
    * of a well-defined point in the function. However, since the function could be evaluated at many points, the method
    * will only check up to 50 different points. If a "hit" is detected, then the method returns a reference to the node
    * itself -- a function node has no renderable descendants to search!
    * 
    * @see org.hhmi.phyplot.FGraphicNode#hitTest(Point2D)
    */
   @Override
   protected FGraphicNode hitTest(Point2D p)
   {
      // only rendered in a well-defined graph viewport
      FViewport2D graphVP = getParentViewport();
      if(getParentGraph() == null || graphVP == null) return(null);

      int nTotal = getDataSize();
      double dSkip = (nTotal<=50) ? 1 : ((double)nTotal)/50.0;

      double index = 0;
      int i = 0;
      Point2D pDatum = new Point2D.Double(0,0);
      AffineTransform at = getLocalToGlobalTransform();
      while(i < nTotal)
      {
         double x = x0 + i*dx;
         pDatum.setLocation(x, parser.evaluate(x));
         graphVP.userUnitsToThousandthInches(pDatum);
         if(Utilities.isWellDefined(pDatum))
         {
            at.transform(pDatum, pDatum);
            if(pDatum.distance(p) <= 100) return(this);
         }

         index += dSkip;
         i = (int)index;
      }

      return(null);
   }

   /**
    * A <code>FunctionNode</code>s is alwayrs rendered into the data viewport of its parent graph and does not 
    * establish its own viewport. Therefore, this method returns the identity transform.
    * 
    * @see FGraphicNode#getLocalToParentTransform()
    */
   @Override
   public AffineTransform getLocalToParentTransform()
   {
      return(new AffineTransform());
   }

   /**
    * The <code>FunctionNode</code> does not admit (renderable) children and does not define its own viewport, so the 
    * parent viewport is always returned.
    * 
    * @see FGraphicNode#getViewport()
    */
   @Override
   public FViewport2D getViewport()
   {
      return(getParentViewport());
   }

   /**
    * This method clears the internal list of painter(s) used to render the <code>FunctionNode</code>, as well as the
    * cached rectangle bounding any marks made by those painters.
    * 
    * @see FGraphicNode#releaseRenderResourcesForSelf()
    */
   @Override
   protected void releaseRenderResourcesForSelf()
   {
      painters.clear();
      rBoundsSelf = null;
   }

   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      if(forceRecalc || rBoundsSelf == null || painters.size() == 0)
      {
         updatePainters();
         rBoundsSelf = new Rectangle2D.Double();
         for(Painter p : painters)
         {
            p.updateFontRenderContext(g2d);
            p.invalidateBounds();
            Utilities.rectUnion(rBoundsSelf, p.getBounds2D(null), rBoundsSelf);
         }
         
         // if parent graph clips its data, then we need to clip the render bounds to the graph's data viewport
         FGNGraph g = getParentGraph();
         if(g != null && g.getClip())
            Rectangle2D.intersect(rBoundsSelf, g.getBoundingBoxLocal(), rBoundsSelf);

      }
      return((Rectangle2D)rBoundsSelf.clone());
   }

   /**
    * Render this <code>FunctionNode</code> into the current graphics context as a polyline connecting the points at 
    * which the function string is evaluated, possibly adorned with marker symbols. The polyline is styled IAW the 
    * properties defined on this element, while the marker symbols are defined and styled IAW the definition of the 
    * node's required <code>SymbolNode</code> component.
    * 
    * <p>Rendering is handled by two <code>Painter</code> objects that are maintained and updated internally as the 
    * element's definition changes.</p>
    * 
    * @see com.srscicomp.common.g2dviewer.Renderable#render(Graphics2D, RenderTask)
    * @see FunctionNode#updatePainters()
    */
   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(needsRendering(task))
      {
         if(painters.size() == 0) 
            updatePainters();
         
         // when exporting to PDF, must handle font substitution in case chosen font does not handle the single 
         // character rendered inside each marker symbol (if any)
         SymbolNode symbol = getSymbolNode();
         String symChar = symbol.getSizeInMilliInches() > 0 ? symbol.getCharacter() : "";
         if((!symChar.isEmpty()) && PDFSupport.isPDFGraphics(g2d))
         {
            AttributedString aStr = PDFSupport.getInstance().doFontSubstitutionIfNeeded(symChar, symbol.getFont());
            if(aStr != null) ((ShapePainter) painters.get(1)).setTextLabel(aStr);
         }
         
         for(Painter p : painters)
         {
            if(!p.render(g2d, task)) return(false);
         }
      }
      return(true);
   }

   
   //
   // Internal rendering resources
   //

   /**
    * The list of <code>Painter</code>s which are responsible for rendering this <code>DataTraceElement</code> in its 
    * current state.
    */
   private List<Painter> painters = new ArrayList<Painter>();

   /**
    * Cached rectangle bounding only the marks made by this <code>FunctionNode</code>. An empty rectangle indicates 
    * that it makes no marks when "rendered". If <code>null</code>, the rectangle has yet to be calculated.
    */
   private Rectangle2D rBoundsSelf = null;

   /**
    * Create/update the set of <code>Painter</code>s responsible for rendering this <code>FunctionNode</code> IAW its
    * current definition.
    * 
    * <p>The function is rendered as a "connect the dots" polyline, possibly adorned with marker symbols. The following 
    * painters are configured, <em>in order</em>:
    * <ul>
    *    <li>A <code>PolylinePainter</code> is configured to connect the well-defined data points in the function with 
    *    a polyline styled IAW the element's own draw styles. The inner class <code>PointProducer</code> serves as 
    *    the location producer for this painter.</li>
    *    <li>A <code>ShapePainter</code> is configured to render a symbol (if any) at all well-defined points in the 
    *    set, styled IAW the node's <code>SymbolNode</code> component.
    * </ul>
    * </p>
    * 
    * <p>Note that the painters will be rendered in the order they appear in the list. Thus, any marker symbols are 
    * drawn on top of the polyline -- which makes sense, since we don't want the polyline to be drawn inside the 
    * symbols!</p>
    */
   private void updatePainters()
   {
      if(painters.size() == 0)
      {
         painters.add( new PolylinePainter(this, new PointProducer()) );

         SymbolNode symbol = getSymbolNode();
         painters.add( 
               new ShapePainter(symbol, new PointProducer(), symbol.getType(), (float) symbol.getSizeInMilliInches(), 
                     symbol.getCharacter()) );
      }
      else
      {
         SymbolNode symbol = getSymbolNode();
         ShapePainter symbolPainter = (ShapePainter) painters.get(1);
         symbolPainter.setPaintedShape(symbol.getType());
         symbolPainter.setSize((float) symbol.getSizeInMilliInches());
         symbolPainter.setTextLabel( symbol.getCharacter() );
      }
   }


   //
   // PSTranformable implementation
   //

   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;

      // get array of (x,y)-coords of all rendered points (in plotting order) in milli-in WRT the parent view port. If 
      // there are no points, there's nothing to render.
      Point2D[] coords = getRenderedCoords();
      if(coords.length == 0) return;

      psDoc.startElement( this );

      // draw the connecting polyline with symbols centered on all well-defined data points.  Symbols may have 
      // different draw styles than the polyline itself, and may include a letter centered within them.
      SymbolNode symbolInfo = getSymbolNode();
      psDoc.renderPolyline(coords, symbolInfo.getType(), symbolInfo.getSizeInMilliInches(), 
         symbolInfo, symbolInfo.getCharacter(), !isStroked());
      
      psDoc.endElement();
   }

   
   //
   // Object
   //
   
   /**
    * Overridden to ensure that the cloned <code>FunctionNode</code> gets its own independent function parser and 
    * rendering infrastructure.
    * 
    * @see Object#clone()
    */
   @Override
   protected Object clone()
   {
      FunctionNode copy = (FunctionNode) super.clone();
      copy.parser = new FunctionParser(this.parser.getDefinition());
      copy.painters = new ArrayList<Painter>();
      copy.rBoundsSelf = null;
      return(copy);
   }


   /**
    * This helper class provides an iterator over the points at which the function is evaluated. It serves both as the 
    * iterator implementation and the iterator provider (it simply provides fresh copies of itself).
    * 
    * <p><code>PointProducer</code> iterates over the points {x, f(x)}, for x = x0, x0 + dx, ..., x0+n*dx &lt;= x1. The 
    * points are transformed from "user" coordinates to "rendered" coordinates WRT the parent graph viewport. Thus, it 
    * is intended primarily for use while rendering the function into a graphics context representing that viewport.
    * <b>The implementation assumes</b> that the consumer processes each {@link Point2D} generated without
    * storing a reference to it -- so that it can safely reuse the same <code>Point2D</code> object for each point 
    * produced. Doing so greatly increases performance when generating thousands of points.</p>
    * 
    * <p><i>Polyline simplification</i>. The time it takes to render the function polyline increases as the number of
    * points increases. But the points may be so close together that the longer rendering time is not warranted. To
    * address this issue, a basic radial distance-based simplification algorithm is enabled when the function's domain
    * size exceed 5000 samples. The algorithm will "skip" points that are too close to the last well-defined point
    * produced. The "too close" metric is when the squared distance between the points is less than (2S)^2, where S is
    * the function's stroke width in milli-inches (the same units in which point coordinates are produced). If S==0, 
    * then a value of 10 is used.</p>
    * 
    * <p>Iterators provided by <code>PointProducer</code> do <i>not</i> support removal of a data point. Also, the class
    * is <i>not</i> thread-safe. Since it is used to iterate over data during rendering (which occurs in a background 
    * thread), this could be problematic!</p>
    * 
    * @author  sruffner
    */
   private class PointProducer implements Iterable<Point2D>, Iterator<Point2D>
   {
      public Iterator<Point2D> iterator()
      {
         return( new PointProducer() );
      }

      PointProducer()
      {
         graphVP = getParentViewport();
         dx = getDX();
         nPtsSoFar = 0;
         xCurr = getX0();
         pCurrent = new Point2D.Double();
         
         if(getDataSize() > 5000) 
         {
            double d = getStrokeWidth() * 2.0;
            subSampler = new RadialPolylineSubsampler( d <= 0 ? 20 : d);
         }
      }

      public boolean hasNext()
      {
         return(isFunctionValid() && nPtsSoFar < getDataSize());
      }

      public Point2D next()
      {
         if(!hasNext()) throw new NoSuchElementException("Out of elements.");

         if(subSampler == null)
         {
            prepareNextPoint();
            return(pCurrent);
         }
         
         boolean keepPt = false;
         while(hasNext() && !keepPt)
         {
            prepareNextPoint();
            keepPt = subSampler.keep(pCurrent);
         }
         
         return(pCurrent);
      }

      private void prepareNextPoint()
      {
         pCurrent.setLocation(xCurr, parser.evaluate(xCurr) );
         if(graphVP != null) graphVP.userUnitsToThousandthInches(pCurrent);

         ++nPtsSoFar;
         xCurr += dx;
      }
      
      public void remove()
      {
         throw new UnsupportedOperationException("Removal not supported by this iterator.");
      }
      
      /** The parent graph viewport converts each point from user units to rendering units. */
      final FViewport2D graphVP;
      /** Function sample interval. */
      final double dx;
      /** Number of data points processed thus far. */
      int nPtsSoFar;
      /** Current value of function's domain variable, X. */
      double xCurr;
      /** 
       * The current data point. This is reused to deliver each point. IT IS ASSUMED that the consumer will NOT store
       * a reference to this point, but will make a copy if needed.
       */
      Point2D pCurrent;
      
      /** Non-null if the polyline point sequence is being sub-sampled (when there are too many function samples). */
      RadialPolylineSubsampler subSampler = null;
   }
}
