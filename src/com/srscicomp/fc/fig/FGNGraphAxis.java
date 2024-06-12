package com.srscicomp.fc.fig;

import com.srscicomp.common.util.Utilities;

/**
 * <b>FGNGraphAxis</b> is the abstract base class for two classes representing a 2D graph axis: {@link AxisNode} 
 * represents the required primary and secondary axes of a 2D Cartesian graph, while {@link ColorBarNode} represents the
 * "color bar" for any 2D or 3D graph and is used to render the colormap gradient which governs how Z data are mapped to
 * color in the rendering of 3D data sets. The color bar is axis-like in the sense that it is drawn along one edge of 
 * the graph's data window, and it includes an axis line and optional tick sets to represent how the Z data range is 
 * mapped onto the colormap gradient. In fact, <b>FGNGraphAxis</b> exists only to declare all methods upon which a 
 * {@link TickSetNode} depends to render itself within its parent axis viewport, regardless whether that parent is an 
 * <b>AxisNode</b> or a <b>ColorBarNode</b>.
 * 
 * <p>NOTE: The 3D graph node {@link Graph3DNode} was introduced in FC 5.0.0, but its implementation is quite different 
 * from the 2D graph {@link GraphNode}, and its axes do not descend from <b>FGNGraphAxis</b>. A second 2D graph class,
 * {@link PolarPlotNode}, was introduced in FC 5.1.2 as a specialized and more versatile implementation of a polar plot.
 * The class that implements its theta and radial axis nodes does not descend from <b>FGNGraphAxis</b>.</p>
 * 
 * <p>However, both of these newer graph containers include a color bar. This required changes to <b>FGNGraphAxis</b> 
 * and the introduction of an abstract graph container, {@link FGNGraph}. All 2D and 3D graph containers in the FypML
 * graphic model descend from <b>FGNGraph</b>.</p>
 * 
 * @author sruffner
 */
public abstract class FGNGraphAxis extends FGraphicNode
{
   
   /**
    * Construct a new 2D graph axis. The graph axis will possess the following inheritable styles, all of
    * which are initially inherited from the parent graph: font-related styles, fill colors, stroke color, line width.
    * It also possesses a 'title' attribute, which is initially an empty string. The {@link FGraphicNode} base class
    * handles all of these attributes.
    */
   FGNGraphAxis() { super(HASFONTATTRS|HASFILLCATTR|HASSTROKEATTRS|HASTITLEATTR|ALLOWLFINTITLE|ALLOWATTRINTITLE); }
   
   /**
    * Return a reference to the graph parent container for this axis. 
    * @return The parent graph container, or null if this axis has no parent.
    */
   final FGNGraph getParentGraph()
   {
      FGraphicNode p = getParent();
      if(p == null) return(null);
      assert(p instanceof FGNGraph);
      return((FGNGraph)p);
   }

   /**
    * Does this 2D graph axis represent the primary axis of its parent graph? 
    * @return <code>True</code> if this is a primary axis; <code>false</code> otherwise.
    */
   public abstract boolean isPrimary();

   /**
    * Is this graph axis above or to the left of the parent graph's data box? The answer varies depending on the graph
    * type and layout, and the axis type. For example, the primary or x-axis will be below the graph data box in the
    * 1st- and 2nd-quadrant layouts, etcetera.
    * @return <code>True</code> if axis is above or to left of the graph's data box. Returns <code>false</code> always 
    * if the axis currently has no parent graph.
    */
   abstract boolean isAboveLeft();
   
   /**
    * Is this graph axis logarithmic in its current context? The answer varies depending on the graph type and the axis
    * type. For example, a color axis is logarithmic only if the logarithmic data-to-color mapping mode is in effect.
    * @return <code>True</code> iff axis is logarithmic. Returns <code>false</code> always if the axis currently has 
    * no parent graph.
    */
   abstract boolean isLogarithmic();
   
   /**
    * Get the logarthmic base for this graph axis. The only supported bases are 2 and 10.
    * @return 2 if base2; else 10.
    */
   abstract int getLogBase();
   
   /**
    * Does graph axis represent the theta axis of a polar plot? 
    * @return True iff this is the theta axis of a polar graph. Returns false always if axis has no parent graph.
    */
   abstract boolean isTheta();
   
   /**
    * Does this graph axis represent a horizontal axis in its current context -- ie, is it parallel to a line extending
    * from the bottom-left to the bottom-right corner of the parent graph's data box? The answer varies depending on the 
    * graph type and the axis type. For example, the primary or x-axis of a non-polar plot is horizontal. Also, by 
    * convention, the radial axis of a polar plot is considered horizontal. 
    * 
    * @return <code>True</code> if axis is horizontal. Returns <code>false</code> always if the axis currently has no 
    * parent graph.
    */
   abstract boolean isHorizontal();

   /**
    * Does this graph axis represent a vertical axis in its current context-- ie, is it parallel to a line extending
    * from the bottom-left to the top-left corner of the parent graph's data box? The answer varies depending on the 
    * graph type and the axis type. 
    * 
    * @return <code>True</code> if axis is vertical. Returns <code>false</code> always if the axis currently has no 
    * parent graph.
    */
   abstract boolean isVertical();
   
   /**
    * Does this axis object represent the color bar for the parent graph? The color bar defines the colormap for 
    * rendering 3D data sets with color and renders that color map with a gradient bar and accompanying axis that
    * illustrate how the Z-coordinate range maps to color.
    * 
    * @return True if this is the color bar of the parent graph.
    */
   final public boolean isColorBar() { return(getNodeType().equals(FGNodeType.CBAR)); }
   
   /**
    * Get the actual length of the axis line for this graph axis, in milli-inches. This is the design length; it does
    * not take into account adjustments made to improve alignment with the orthogonal axis.
    * @return Actual length of axis, in milli-inches. Returns 0 if the axis currently lacks a parent graph.
    */
   abstract double getLengthInMilliInches();

   /**
    * Get the amount by which the endpoints of the axis line must be extended (if positive) or shrunk (if negative) in
    * order to better align the axis with its orthogonal counterpart.
    * 
    * <p>Since a graph's axes are encapsulated by independent objects, we may need to make some fine adjustments so that
    * the axis lines in a Cartesian-type graph align nicely in any of the single-quadrant layouts. Essentially, we need 
    * to extend the axis line at both endpoints by 1/2 the thickness of the orthogonal axis, but then subtract 1/2 the
    * thickness of the axis itself IF it is to be stroked using a round- or square-projection endcap decoration (see
    * <code>StrokeCap.ROUND, .SQUARE</code>). The adjustment is not necessary in the 4-quadrant layout or in polar 
    * graphs. It is also not necessary if the orthogonal axis is hidden.</p>
    * 
    * @return Returns the amount by which the axis's endpoints should be pushed outward (if positive) or inward (if
    * negative) in order to improve alignment with the orthogonal axis. In milli-inches.
    */
   abstract double getAxisEndpointAdjustment();
   
   /**
    * Get the tick set defining the "major tick marks" for this 2D graph axis. 
    * @return The major tick mark set for this axis, or <code>null</code> if it lacks one. An axis may have one or more
    * tick mark sets but lack a "major" tick set.
    */
   abstract TickSetNode getMajorTickSet();

   /**
    * Is the automatic axis range adjustment feature enabled for this 2D graph axis?
    * @return True if auto-ranging is enabled for this graph axis; false otherwise.
    */
   final public boolean isAutoranged()
   {
      // NOTE that the feature is controlled in the parent graph, NOT on the individual axis nodes.
      FGNGraph g = getParentGraph();
      return(g != null && g.isAxisAutoranged(this));
   }
   
   /**
    * Get the start of the coordinate range spanned by this graph axis. This property is controlled by the user and may 
    * not be a valid value, depending on the current context. To get the validated start of the axis range, use 
    * <code>getValidatedStart()</code>.
    * @return Start of axis range, in "user" units.
    */
   public abstract double getStart();
   
   /**
    * Get the starting point of this graph axis's coordinate range in user units, VALIDATED in accordance with the 
    * current graph context. Use the validated axis range when performing any computations related to the actual 
    * rendering of the axis and its parent graph.
    * @return Valid starting point of axis coordinate range, in user units. Returns the nominal value provided by 
    * <code>getStart()</code> if there is no parent graph or if the nominal value is already valid.
    * @see FGNGraphAxis#getStart()
    */
   abstract double getValidatedStart();
   
   /**
    * Get the end of the coordinate range spanned by this graph axis. This property is controlled by the user and may 
    * not be a valid value, depending on the current context. To get the validated end of the axis range, use 
    * <code>getValidatedEnd()</code>.
    * @return Start of axis range, in "user" units.
    */
   public abstract double getEnd();

   /**
    * Get the endpoint of this graph axis's coordinate range in user units, VALIDATED in accordance with the current 
    * graph context. Use the validated axis range when performing any computations related to the actual rendering of 
    * the axis and its parent graph.
    * @return Valid endpoint of axis coordinate range, in user units. Returns the nominal value provided by 
    * <code>getEnd()</code> if there is no parent graph or if the nominal value is already valid.
    * @see FGNGraphAxis#getEnd()
    */
   abstract double getValidatedEnd();

   /**
    * The helper function <i>doAutoScale()</i> will call this method to update the start and end of the axis range. 
    * Implementing classes should simply set their range as specified. Do NOT trigger a rendering update, and do NOT
    * include the changes in the graphic model's undo history. Auto-scaling changes are NOT undoable.
    * @param s The start of the axis range.
    * @param e The end of the axis range.
    * @see FGNGraphAxis#doAutoScale()
    */
   abstract protected void setRange_NoUpdate(double s, double e);
   
   private static double[] niceDivs = new double[] {1, 1.25, 1.5, 2, 2.5, 5, 7.5};
   
   /**
    * Adjust the range for this axis to accommodate the specified minimum and maximum values, if possible. If the
    * axis has a major tick set, then its interval and tick label format are auto-adjusted at the same time.
    * 
    * <p><i>Summary of the range adjustment algorithm</i>. Let <i>m</i> represent the minimum coordinate value that 
    * should be contained in the axis range <i>[S..E], S&lt;E</i>; and let <i>M</i> be the maximum coordinate value. 
    * Also, if the axis has a major tick set, let <i>D</i> be the calculated tick set interval.
    * <ul>
    *    <li><i>Theta axis of a polar graph</i>. Axis range is fixed IAW the parent graph's quadrant layout. For the 
    *    single-quad layouts, <i>D=30</i> (deg); for the all-quad layout, <i>D=45</i>. Tick label format is "int".</li>
    *    <li><i>Logarithmic axis</i>. Let <i>B</i> be the axis's logarithmic base, either 2 or 10. Find integer 
    *    <i>n1</i> such that <i>B^n1 &le; m &lt; B^(n1+1)</i> and integer <i>n2&ge;(n1+1)</i> such that <i>B^(n2-1) &lt; 
    *    M &le; B^n2</i>; if no such integer exists, then set <i>n2 == (n1+1)</i>. [Special cases: If both <i>m,M</i> 
    *    are non-positive, <i>n1=0, n2=1</i> by convention. If <i>m&le;0</i> but <i>M&gt;0</i>, <i>n1=min(0,n2-1)</i>.]
    *    If the axis has a major tick set, we need to find integers <i>d,p</i> such that <i>n1 + p*d = n2</i>, where 
    *    <i>p=[1..5]</i> is the number of major axis divisions and <i>B^d</i> is the multiplicative tick interval (we 
    *    may have to increment <i>n2</i> or decrement <i>n1</i> to find a solution). Note that, if <i>n2-n1 &le; 5</i>, 
    *    then <i>d=1</i> and <i>p=n2-n1</i>. Always try to maximize the number of divisions <i>p</i>. Assign <i>S = 
    *    B^n1, E = B^n2, D = B^d</i>. The tick label format will be "int" if <i>S&ge;1</i>; "f3" otherwise.</li>
    *    <li><i>Linear axis</i>. Set the axis range so that it encompasses <i>[m..M]</i>, but try to ensure that the
    *    major axis divisions (according to the first tick set child, if defined) look reasonable. Two use cases:
    *    <ol>
    *       <li>Axis has no tick sets. In this case, we see no axis divisions, so just set <i>S=m, E=M</i>.</li>
    *       <li>The major tick set exists. Choose <i>S&le;m</i>, <i>E&ge;M</i> and <i>D</i> such that the number of 
    *       major axis divisions defined by the tick set is <i>N=[1..5]</i>. In general, <i>D = K*10^p</i> for integer 
    *       <i>p</i> and for <i>K={1, 1.25, 1.5, 2, 5, 7.5}</i> such that <i>N*D</i> spans all of the data with as 
    *       little "extra space" left over as possible. <i>S</i> and <i>E</i> are adjusted depending on the relationship
    *       between extrema <i>m</i> and <i>M</i>:
    *       <ol>
    *          <li>Empty range, <i>m==M</i>. In this case, <i>S=round(m)-D</i>, <i>E=round(m)+D</i>, and 
    *          <i>D=10^p</i>, where <i>p=0</i> if <i>abs(m)<1000</i>, else <i>p = floor(log10(abs(m)))</i>.</li>
    *          <li>Extrema are on same side of 0 and ratio of larger/smaller &ge; 10. In this case, the range starts or 
    *          ends at 0, and <i>N,p,K</i> are calculated so that the interval <i>D=K*10^p</i> and the other end of the
    *          range is at <i>N*D</i> or <i>-N*D</i>.</li>
    *          <li>Extrema are of opposite sign and ratio larger/smaller &le; 1.25. Range is symmetric about 0. Values
    *          <i>N,p,K</i> are chosen so that <i>max(-m,M) &le; N*K*10^p</i> with <i>N=[1..2]</i>. Then: <i>D=K*10^p,
    *          S=-N*D, E=N*D</i>.</li>
    *          <li>Extrema are of opposite sign but ratio larger/smaller is &ge; 10. Choose <i>N,p,K</i> so that 
    *          <i>max(-m,M) &le; N*K*10^p</i>, this time with <i>N=[1..5]</i>. Then: <i>D=K*10^p</i>; <i>S=-D, E=N*D</i>
    *          if <i>M &gt; m</i>; else, <i>S=-N*D, E=D</i>. Note we could end up with 6 major divisions here.</li>
    *          <li>Extrema are of the same sign and the ratio of larger/smaller &lt; 10; OR extrema are of opposite 
    *          sign and the ratio of larger/smaller &gt; 1.25 but &lt; 10. In this case, we choose 
    *          <i>S = (+/-)N*K*10^p &le; m</i>, then find <i>N1,K1,p1</i> such that <i>(M-S) &le; N1*K1*10^p1</i>. Then
    *          <i>D=K1*10^p1</i> and <i>E = S + N1*D</i>.</li>
    *       </ol>
    *       </li>
    *    </ol>
    *    </li>
    * </ul>
    * Note that, when this method is called, the current start of the axis range could be greater than the end, ie,
    * the axis range is descending rather than ascending. The last step in the algorithm is to swap <i>S</i> and
    * <i>E</i> if the original range was descending.</p>
    * 
    * <p>The method does NOT trigger a re-rendering of the axis or its parent graph. It is assumed that the caller will
    * handle this task. Any time the range of a graph axis changes, the entire graph should be re-rendered.</p>
    * 
    * @param minVal The minimum coordinate value across all graph data and functions.
    * @param maxVal The maximum coordinate value across all graph data and functions.
    * @return True if automatic range adjustment is enabled for this axis and either the start or end of the axis range 
    * was revised to accommodate the specified coordinate extremes. The method will take no action and return false if 
    * either argument is not a well-defined number.
    */
   final boolean doAutoRange(float minVal, float maxVal)
   {
      // NOTE: Autoscaling of axes is only supported by GraphNode, one of two possible 2D graph implementations
      if(!isAutoranged() || !Utilities.isWellDefined(minVal) || !Utilities.isWellDefined(maxVal)) 
         return(false);
      GraphNode g = (GraphNode) getParentGraph();
      
      // swap min, max if they're incorrect
      if(minVal > maxVal) {float f = minVal; minVal = maxVal; maxVal = f; }
      
      double oldS = getStart();
      double oldE = getEnd();
      boolean descending = (oldS > oldE); 
      boolean isPolar = g.isPolar();
      boolean isRadial = isPolar && !isPrimary();
      boolean log2 = (getLogBase() == 2);
      double s = 0;
      double e = 0;
      double intv = 0;
      TickSetNode.LabelFormat tickFmt = TickSetNode.LabelFormat.INT;
      TickSetNode major = getMajorTickSet();

      if(isTheta())
      {
         s = 0; e = 360; intv = 30;
         switch(g.getLayout())
         {
            case QUAD1 : e = 90; break;
            case QUAD2 : s = 90; e = 180; break;
            case QUAD3 : s = 180; e = 270; break;
            case QUAD4 : s = 270; break;
            case ALLQUAD : intv = 45; break;
         }
      }
      else if(isLogarithmic())
      {
         int n1 = 0;
         int n2 = 1;
         if(minVal > 0)
         {
            n1 = (int) Math.floor( log2 ? Utilities.log2(minVal) : Utilities.log10(minVal) );
            n2 = (int) Math.ceil( log2 ? Utilities.log2(maxVal) : Utilities.log10(maxVal) );
            if(n2 <= n1) n2 = n1 + 1;
         }
         else if(maxVal > 0)
         {
            n2 = (int) Math.ceil( log2 ? Utilities.log2(maxVal) : Utilities.log10(maxVal) );
            n1 = Math.min(0, n2-1);
         }
         
         if(n2 - n1 <= 5)
            intv = log2 ? 2 : 10;
         else
         {
            int x = 5;
            while(x >= 2)
            {
               if((n2-n1) % x == 0)
               {
                  int y = (n2 - n1) / x;
                  if(y <= 5) y = Math.min(x, y);
                  intv = Math.pow(log2 ? 2 : 10, y);
                  break;
               }
               if(x == 2)
               {
                  // we must increment n2 or decrement n1. We're assured of exiting loop on the next iteration, since
                  // (n2-n1) will now be divisible by 2.
                  if(minVal <= 0) --n1;
                  else
                  {
                     s = Math.pow(log2 ? 2 : 10, n1);
                     e = Math.pow(log2 ? 2 : 10, n2);
                     if(s/minVal < maxVal/e) ++n2;
                     else --n1;
                  }
                  x = 5;
               }
               --x;
            }
         }
         s = Math.pow(log2 ? 2 : 10, n1);
         e = Math.pow(log2 ? 2 : 10, n2);
         if(s < 1) tickFmt = TickSetNode.LabelFormat.F3;
      }
      else if(major == null)
      {
         // linear axis, but no tick marks rendered
         s = (isRadial && minVal < 0) ? 0 : minVal;
         e = (maxVal <= s) ? s+1 : maxVal;
      }
      else 
      {
         // linear axis with major divisions. Adjust start and end of axis range, and tick intv and label format.
         if(minVal == maxVal)
         {
            // CASE 1: Empty range.
            double a = Math.abs(minVal);
            double p = (a < 1000) ? 0 : (Math.floor(Utilities.log10(a)) - 2);
            intv = Math.pow(10, p);
            s = Math.round(minVal) - intv;
            e = s + 2 * intv;
         }
         else if((minVal == 0) || (minVal > 0 && maxVal/minVal >= 10) || 
                     (maxVal == 0) || (maxVal < 0 && minVal/maxVal >= 10))
         {
            // CASE 2: Extrema are on same side of 0 and ratio larger/smaller >= 10. Range should start or end at 0.
            double m = (minVal >= 0) ? maxVal : -minVal;
            double p = Math.floor(Utilities.log10(m));
            double kExact = m / Math.pow(10, p);
            double minDiff = 10;
            int nBest = 0; 
            double kBest = 0;
            for(int i=0; i<niceDivs.length; i++)
            {
               int n = (int) Math.ceil(kExact/niceDivs[i]);
               double diff = niceDivs[i]*n - kExact;
               if(n >= 1 && n <= 5 && ((diff < minDiff) || (diff == minDiff && n > nBest)))
               {
                  kBest = niceDivs[i]; 
                  nBest = n;
                  minDiff = diff;
               }
            }
            
            // decrement p and try again. If you get more divisions with this value of p, or the same #divisions and a 
            // tighter fit, choose  it.
            kExact = m / Math.pow(10, p-1);
            double minDiff1 = 10;
            int nBest1 = 0;
            double kBest1 = 0;
            for(int i=0; i<niceDivs.length; i++)
            {
               int n = (int) Math.ceil(kExact/niceDivs[i]);
               double diff = niceDivs[i]*n - kExact;
               if(n >= 1 && n <= 5 && ((diff < minDiff1) || (diff == minDiff1 && n > nBest1)))
               {
                  kBest1 = niceDivs[i]; 
                  nBest1 = n;
                  minDiff1 = diff;
               }
            }
            if(nBest1 > nBest || (nBest1 == nBest && minDiff1 < minDiff))
            {
               nBest = nBest1;
               kBest = kBest1;
               p = p - 1;
            }
            
            intv = kBest * Math.pow(10, p);
            s = (minVal >= 0) ? 0 : -nBest*intv;
            e = (minVal >= 0) ? nBest * intv : 0;
         }
         else if((minVal < 0 && maxVal > 0) && (maxVal/(-minVal) <= 1.25) && (maxVal/(-minVal) >= 0.8))
         {
            // CASE 3: Extrema are opposite sign and ratio M/(-m) lies in [0.8..1.25]: make range symmetric about 0.
            double m = Math.max(-minVal, maxVal);
            double p = Math.floor(Utilities.log10(m));
            double kExact = m / Math.pow(10, p);
            double minDiff = 10;
            int nBest = 0; 
            double kBest = 0;
            for(int i=0; i<niceDivs.length; i++)
            {
               int n = (int) Math.ceil(kExact/niceDivs[i]);
               double diff = niceDivs[i]*n - kExact;
               if(n >= 1 && n <= 2 && ((diff < minDiff) || (diff == minDiff && n > nBest)))
               {
                  kBest = niceDivs[i]; 
                  nBest = n;
                  minDiff = diff;
               }
            }
            
            // decrement p and try again. If you get more divisions with this value of p, or the same #divisions and a 
            // tighter fit, choose  it.
            kExact = m / Math.pow(10, p-1);
            double minDiff1 = 10;
            int nBest1 = 0;
            double kBest1 = 0;
            for(int i=0; i<niceDivs.length; i++)
            {
               int n = (int) Math.ceil(kExact/niceDivs[i]);
               double diff = niceDivs[i]*n - kExact;
               if(n >= 1 && n <= 2 && ((diff < minDiff1) || (diff == minDiff1 && n > nBest1)))
               {
                  kBest1 = niceDivs[i]; 
                  nBest1 = n;
                  minDiff1 = diff;
               }
            }
            if(nBest1 > nBest || (nBest1 == nBest && minDiff1 < minDiff))
            {
               nBest = nBest1;
               kBest = kBest1;
               p = p - 1;
            }
           
            intv = kBest * Math.pow(10, p);
            e = nBest * intv;
            s = -e;
         }
         else if((minVal < 0 && maxVal > 0) && ((maxVal/(-minVal) >= 10) || maxVal/(-minVal) <= 0.1))
         {
            // CASE 4: Extrema are opposite sign but ratio M/(-m) >= 10 or <= 0.1. Use the larger extremum to 
            // determine interval; add an extra division on opposite side of zero to encompass the smaller extremum.
            double m = (maxVal > -minVal) ? maxVal : -minVal;
            double p = Math.floor(Utilities.log10(m));
            double kExact = m / Math.pow(10, p);
            double minDiff = 10;
            int nBest = 0; 
            double kBest = 0;
            for(int i=0; i<niceDivs.length; i++)
            {
               int n = (int) Math.ceil(kExact/niceDivs[i]);
               double diff = niceDivs[i]*n - kExact;
               if(n >= 1 && n <= 5 && ((diff < minDiff) || (diff == minDiff && n > nBest)))
               {
                  kBest = niceDivs[i]; 
                  nBest = n;
                  minDiff = diff;
               }
            }
            
            // decrement p and try again. If you get more divisions with this value of p, or the same #divisions and a 
            // tighter fit, choose  it.
            kExact = m / Math.pow(10, p-1);
            double minDiff1 = 10;
            int nBest1 = 0;
            double kBest1 = 0;
            for(int i=0; i<niceDivs.length; i++)
            {
               int n = (int) Math.ceil(kExact/niceDivs[i]);
               double diff = niceDivs[i]*n - kExact;
               if(n >= 1 && n <= 5 && ((diff < minDiff1) || (diff == minDiff1 && n > nBest1)))
               {
                  kBest1 = niceDivs[i]; 
                  nBest1 = n;
                  minDiff1 = diff;
               }
            }
            if(nBest1 > nBest || (nBest1 == nBest && minDiff1 < minDiff))
            {
               nBest = nBest1;
               kBest = kBest1;
               p = p - 1;
            }
            
            intv = kBest * Math.pow(10, p);
            s = (maxVal > -minVal) ? -intv : -nBest*intv;
            e = (maxVal > -minVal) ? nBest*intv : intv;
         }
         else
         {
            // OTHERWISE: Extrema are of same sign and ratio larger/smaller < 10; OR extrema are of opposite sign and
            // ratio M/(-m) lies in (0.8..0.1) or (1.25..10). 
            // (1) Find s = (+/-1)*K*10^p <= minVal for K=[1..9], minimizing the difference.
            double m = Math.abs(minVal);
            double p = Math.floor(Utilities.log10(m));
            double pwr = Math.pow(10,p);
            int k = (int) Math.rint(m/pwr);
            if(minVal > 0 && k*pwr > m) --k;
            else if(minVal < 0 && k*pwr < m) ++k;
            s = ((minVal > 0) ? k : -k) * pwr;
            
            /*
            double minDiff = 10;
            int nBest = 0;
            double kBest = 0;
            for(int i=0; i<niceDivs.length; i++)
            {
               int n = (int) ((minVal > 0) ? Math.floor(kExact/niceDivs[i]) : Math.ceil(kExact/niceDivs[i]));
               double diff = Math.abs(niceDivs[i]*n - kExact);
               if(n >= 1 && n <= 5 && diff < minDiff)
               {
                  nBest = n;
                  kBest = niceDivs[i];
                  minDiff = diff;
               }
            }
            s = (minVal > 0 ? 1 : -1) * nBest*kBest*Math.pow(10,p);
            */
            
            // (2) Let D = maxVal-s. Find N'*K'*10^p' >= D, minimizing the difference.
            m = maxVal-s;
            p = Math.floor(Utilities.log10(m));
            double kExact = m /Math.pow(10, p);
            double minDiff = 10;
            double nBest = 0;
            double kBest = 0;
            for(int i=0; i<niceDivs.length; i++)
            {
               int n = (int) Math.ceil(kExact/niceDivs[i]);
               double diff = niceDivs[i]*n - kExact;
               if(n >= 1 && n <= 5 && ((diff < minDiff) || (diff == minDiff && n > nBest)))
               {
                  kBest = niceDivs[i]; 
                  nBest = n;
                  minDiff = diff;
               }
            }
            
            // (2a) decrement p and try again. If you get more divisions with this value of p, or the same #divisions 
            // and a tighter fit, choose  it.
            kExact = m / Math.pow(10, p-1);
            double minDiff1 = 10;
            int nBest1 = 0;
            double kBest1 = 0;
            for(int i=0; i<niceDivs.length; i++)
            {
               int n = (int) Math.ceil(kExact/niceDivs[i]);
               double diff = niceDivs[i]*n - kExact;
               if(n >= 1 && n <= 5 && ((diff < minDiff1) || (diff == minDiff1 && n > nBest1)))
               {
                  kBest1 = niceDivs[i]; 
                  nBest1 = n;
                  minDiff1 = diff;
               }
            }
            if(nBest1 > nBest || (nBest1 == nBest && minDiff1 < minDiff))
            {
               nBest = nBest1;
               kBest = kBest1;
               p = p - 1;
            }

            intv = kBest * Math.pow(10, p);
            e = s + nBest*intv;
         }
         
         // for all cases, choose floating-pt tick label fmt if start, end or intv is not integral
         if(Math.rint(s) != s || Math.rint(e) != e || Math.rint(intv) != intv)
            tickFmt = TickSetNode.LabelFormat.F3;
      }
      
      // swap start and end if original range was descending
      if(descending) {double d = s; s = e; e = d; };
      
      // update the axis range if there was any change. Also update major tick set interval and label format. If axis
      // is logarithmic, make sure major tick set renders at least one tick per decade. We rely on caller to trigger a 
      // refresh. 
      if(oldS != s || oldE != e  || (major != null && major.getInterval() != intv))
      {
         setRange_NoUpdate(s, e);
         if(major != null)
         {
            major.fixTickIntervalAndFormat(intv, tickFmt);
            if(major.getDecadeTicks().getEnabledTickMask() == 0)
               major.setDecadeTicks(LogTickPattern.fromString("1"));
         }
         return(true);
      }
      
      return(false);
   }
   
   /**
    * When the endpoints of the axis range are very small (e.g., 1e-6) or very large (1e10), the tick mark labels along
    * the axis tend to get long and are best presented in scientific notation. In that case, each tick label includes
    * an exponent: "1e-6 2e-6 3e-6 ...". 
    * 
    * <p>To achieve a more compact rendering of the axis, we could divide each tick value by a power of 10, then 
    * format the result: "1 2 3 ...", assuming a scale factor of 1e6. The common scale factor can then be included in 
    * the axis label for clarity: "label string (x10^6)".</p>
    * 
    * <p>This method returns the exponent N for the desired scale factor 10^N. Subclasses may choose to calculate the
    * best scale factor based on the current axis range, or make it a user-defined attribute of the axis. Any tick sets
    * will call this method when formatting tick mark labels, and {@link #getAxisLabel()} will include the scale factor
    * in the axis label if N != 0.</p>
    * 
    * @return Exponent of the power-of-10 scale factor for this axis, as described. Allowed range is 
    * {@link #MINPOWERSCALE} .. {@link #MAXPOWERSCALE}.
    */
   abstract int getPowerScale();
   
   /** Minimum value of exponent for the axis power-of-10 scale factor. */
   public final static int MINPOWERSCALE = -99;
   /** Maximum value of exponent for the axis power-of-10 scale factor. */
   public final static int MAXPOWERSCALE = 99;
   
   /**
    * Get the label string for this axis. Generally, this is simply the string returned by {@link #getTitle()}. However,
    * if the axis scale factor is not unity <b>and the title string is non-empty</b>, then the scale factor is appended
    * to the title string to form the axis label. Let N be the non-zero base-10 exponent returned by {@link #
    * getPowerScale()}. If 0 < N < 4, then the method appends "(xS)", where S=10^N. Otherwise, "(x1EN)" is appended.
    * This serves to clarify that any tick mark labels along the axis are scaled accordingly. <b>NOTE that the scale
    * factor is not included if the title string is empty, which could confuse the reader!</b>
    * 
    * <p>The label will be formatted as <i>FypML</i> attributed text if {@link #getTitle()} returns attributed text,
    * or if the suffix "(x10^N)" is appended to a plain-text title (in order to render the N as superscript.</p>
    * 
    * @return The axis label string, as described.
    */
   final String getAxisLabel()
   {
      String s = getTitle().trim();
      int exp = getPowerScale();
      if(exp != 0 && !s.isEmpty())
      {
         if(hasAttributedTextInTitle())
         {
            // base axis label uses attributed text: split text from attr codes before adding suffix. For the suffix,
            // we turn off underlining and super/subscript, use plain font style, and restore default text color. But
            // we superscript the exponent, if any.
            int idx = s.lastIndexOf('|');
            String label = s.substring(0, idx);
            String codes = s.substring(idx+1);
            if(exp > 0 && exp < 4)
               s = String.format("%s (\u00d7%d)|%s,%d:-unp", label, (int)Math.pow(10,exp), codes, idx);
            else
            {
               s = String.format("%s (\u00d710%d)|%s,%d:-unp,%d:S", label, exp, codes, idx, idx+5);
            }
         }
         else if(exp > 0 && exp < 4)
            s = String.format("%s (\u00d7%d)", s, (int) Math.pow(10, exp));
         else
            s = String.format("%s (\u00d710%d)|%d:S", s, exp, s.length() + 5);  // use attr text to superscript exp!
      }
      return(s);
   }
}
