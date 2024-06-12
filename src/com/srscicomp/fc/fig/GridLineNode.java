package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.common.util.Utilities;

/**
 * <code>GridLineNode</code> encapsulates grid lines that may be optionally drawn within the data box of a graph. Every 
 * <code>GraphNode</code> contains exactly two <em>required</em> <code>GridLineNode</code>s, one for the primary and 
 * one for the secondary grid lines. The primary grid lines are associated with the primary axis, and the grid lines 
 * locations are defined by the "major" tick marks along the secondary axis (horizontal grid lines in a Cartesian plot, 
 * and circular arcs for the theta axis in a polar plot). Similarly, the secondary grid lines are associated with the 
 * secondary axis (vertical lines in a Cartesian plot, radial lines in a polar plot), and their locations are determined
 * by the major tick marks along the primary axis.
 * 
 * <p><code>GridLineNode</code> exists so that the <em>DataNav</em> user can independently turn on and control the 
 * stroking of the two sets of grid lines. In addition to the inheritable stroking attributes that define the appearance 
 * of the grid lines, the <code>GridLineNode</code> includes a "hide" flag that determines whether or not the lines are 
 * rendered at all. Grid lines are, in fact, rarely used -- so the "hide" flag is set by default.</p>
 *  
 * <p><code>GridLineNode</code> may only appear as a component of a <code>GraphNode</code>.</p>
 * 
 * @author 	sruffner
 */
public class GridLineNode extends FGraphicNode implements Cloneable
{
   /**
    * If set, this <code>GridLineNode</code> represents the primary grid lines of its parent <code>GraphNode</code>.
    * Otherwise, it represents the secondary grid lines.
    */
   private boolean isPrimary;

   /**
    * Construct a <code>GridLineNode</code> for the primary or secondary grid lines of a graph. The grid lines are 
    * hidden initially.
    * 
    * @param isPrimary If <code>true</code>, then this node represents the primary grid lines of its parent 
    * <code>GraphNode</code>; else it represents the graph's secondary grid lines.
    */
   public GridLineNode(boolean isPrimary)
   {
      super(HASSTROKEATTRS|HASSTRKPATNATTR);
      this.isPrimary = isPrimary;
      this.hide = true;
   }

   /**
    * Does this grid line node represent the primary or secondary grid lines of its parent graph.
    * @return True if primary grid lines; false if secondary.
    */
   public boolean isPrimary() { return(isPrimary); }
   
   /**
    * Return a reference to the <code>GraphNode</code> containing this <code>GridLineNode</code>. 
    * 
    * @return The parent <code>GraphNode</code>, or <code>null</code> if this node currently has no parent.
    */
   private GraphNode getParentGraph()
   {
      FGraphicNode p = getParent();
      if(p == null) return(null);
      assert(p instanceof GraphNode);
      return((GraphNode)p);
   }

   
   //
   // Support for child nodes -- there are none.
   //

   @Override
   public FGNodeType getNodeType()
   {
      return(FGNodeType.GRIDLINE);
   }

   @Override
   public boolean isLeaf() { return(true); }

   @Override
   public boolean canInsert(FGNodeType nodeType) { return(false); }

   /**
    * A <code>GridLineNode</code> is instrinsic to the definition of its parent <code>GraphNode</code>.
    * 
    * @see FGraphicNode#isComponentNode()
    */
   @Override
   public boolean isComponentNode() { return(true); }

   
   //
   // Properties
   //

   /**
    * If this flag is <code>true</code>, this <code>GridLineNode</code> is not rendered.
    */
   private boolean hide;

   /**
    * Get the hide state for this <code>GridLineNode</code>.
    * 
    * @return <code>True</code> if grid lines are currently hidden.
    */
   public boolean getHide() { return(hide); }

   /**
    * Set the hide state for this grid line node. If a change is made, {@link #onNodeModified()} is invoked.
    * @param b True to hide grid lines, false to show them.
    */
   public void setHide(boolean b)
   {
      if(hide != b)
      {
         if(doMultiNodeEdit(FGNProperty.HIDE, new Boolean(b))) return;
         
         Boolean old = new Boolean(hide);
         hide = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.HIDE);
            String desc = hide ? "Hide " : "Show ";
            desc += isPrimary ? "horizontal (or theta) grid lines" : "vertical (or radial) grid lines";
            FGNRevEdit.post(this, FGNProperty.HIDE, new Boolean(hide), old, desc);
         }
      }
   }

   /**
    * Same as {@link #setHide()}, but without posting an "undo" operation or calling {@link #onNodeModified()}. It is
    * meant to be used only when multiple properties are being modified in a single, atomic operation.
    * @param b True to hide grid lines, false to show them.
    */
   protected void setHideNoNotify(boolean b) { hide = b; }
   
   /**
    * When a <code>GridLineNode</code> is hidden, changes in its stroking styles have no effect on its rendering. The 
    * override avoids unnecessary rendering updates in this case.
    * 
    * @see FGraphicNode#onNodeModified(Object)
    */
   @Override
   protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;
      
      if((hint == FGNProperty.HIDE) || !hide)
         super.onNodeModified(hint);
      else
         model.onChange(this, 0, false, null);
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      if(p == FGNProperty.HIDE)
      {
         setHide(((Boolean)propValue).booleanValue());
         return(true);
      }
      return(super.setPropertyValue(p, propValue));
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      return((p==FGNProperty.HIDE) ? new Boolean(getHide()) : super.getPropertyValue(p));
   }
   
   // 
   // Support for style sets
   //
   
   /**
    * While style sets are supported, <code>GridLineNode</code> only occurs as a component of a graph node, and unlike
    * the axes and legend components, it cannot receive the focus in the UI. It is assumed the parent graph node 
    * provides a suitable text summary of the grid line styling.
    */
   @Override public boolean supportsStyleSet() { return(true); }

   /** The only node-specific style property exported in the grid line node's style set is the hide flag. */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.HIDE, new Boolean(getHide()));
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.HIDE, getNodeType(), Boolean.class);
      if(b != null && !b.equals(restore.getCheckedStyle(FGNProperty.HIDE, getNodeType(), Boolean.class)))
      {
         hide = b.booleanValue();
         changed = true;
      }
      else restore.removeStyle(FGNProperty.HIDE);
      
      return(changed);
   }

   //
   // Focusable/Renderable support
   //

   /**
    * The <code>GridLineNode</code> is rendered directly into its parent viewport, so this method returns the 
    * identity transform.
    * 
    * @see FGraphicNode#getLocalToParentTransform()
    */
   @Override
   public AffineTransform getLocalToParentTransform()
   {
      return( new AffineTransform() );
   }

   /**
    * The <code>GridLineNode</code> does not admit children and does not define its own viewport, so the parent 
    * viewport is always returned.
    * 
    * @see FGraphicNode#getViewport()
    */
   @Override
   public FViewport2D getViewport()
   {
      return(getParentViewport());
   }

   /**
    * A <code>GridLineNode</code> is a non-focusable component of its parent <code>GraphNode</code>. This override 
    * returns <code>null</code> always.
    */
   @Override
   public Shape getFocusShape(Graphics2D g2d)
   {
      return(null);
   }

   /**
    * The <code>GridLineNode</code> is a non-focusable component of its parent <code>GraphNode</code>; it can never be 
    * selected by a mousedown within the area to which it is rendered. This override returns <code>null</code> always.
    */
   @Override
   protected FGraphicNode hitTest(Point2D p)
   {
      return(null);
   }

   /**
    * A grid line node is not rendered if its "hide" flag is set, if it is not stroked, if its parent is not an instance
    * of {@link GraphNode}, or if there are no tick marks defined along the perpendicular axis in the graph.
    */
   @Override protected boolean isRendered()
   {
      if(hide || !isStroked())
         return(false);

      GraphNode g = getParentGraph();
      if(g == null) return(false);
      if(isPrimary)
         return(g.getSecondaryAxis().hasMajorTicks());
      else
         return(g.getPrimaryAxis().hasMajorTicks());
   }

   /**
    * Helper method which prepares the list of lines or circular arcs, defined WRT the parent graph's viewport, that 
    * must be stroked to render this <code>GridLineNode</code> IAW the current state of the node and its parent graph.
    * If the grid lines are not rendered, an empty list is returned.
    */
   private List<Shape> prepareGridPrimitives()
   {
      List<Shape> gridLines = new ArrayList<Shape>();

      // if node is currently not rendered, for whatever reason, return an empty list
      if(!isRendered()) return(gridLines);

      // get parent graph element, which provides some information that affects rendering of axis
      GraphNode g = getParentGraph();
      if(g == null) return(gridLines);

      // get graph viewport object so we can translate user -> physical coords 
      FViewport2D viewport = g.getViewport();
      if(viewport == null) return(gridLines);

      // get the axis endpoints and major ticks -- which we need to determine placement of grid lines
      AxisNode axis = g.getPrimaryAxis();                   // x- or theta-axis
      double[] ax1Ticks = axis.getMajorTicks();             // x-coords of V lines, or theta-coords of radial lines
      double ax1Start = axis.getValidatedStart();           // maps to left edge of data box in Cartesian coord sys
      double ax1End = axis.getValidatedEnd();               // maps to right edge of data box in Cartesian coord sys

      axis = g.getSecondaryAxis();                          // y- or radial-axis
      double[] ax2Ticks = axis.getMajorTicks();             // y-coords of H lines, or radial-coords of theta circles 
      double ax2Start = axis.getValidatedStart();           // maps to bot edge of data box in Cartesian coord sys
      double ax2End = axis.getValidatedEnd();               // maps to top edge of data box in Cartesian coord sys

      // there may be no grid lines at all!
      if(isPrimary ? (ax2Ticks==null) : (ax1Ticks==null))
         return(gridLines);

      // construct each individual grid line or arc that is stroked when rendering this GridLineElement
      Point2D p0 = new Point2D.Double();
      Point2D p1 = new Point2D.Double();
      if(!g.isPolar())
      {
         if(isPrimary) 
         {
            // Cartesian primary axis grid lines -- horiz lines located IAW major ticks along secondary (Y) axis
            for(int i=0; i<ax2Ticks.length; i++)
            {
               p0.setLocation(ax1Start, ax2Ticks[i]);
               p1.setLocation(ax1End, ax2Ticks[i]);
               viewport.userUnitsToThousandthInches( p0 );
               viewport.userUnitsToThousandthInches( p1 );
               if(Utilities.isWellDefined(p0) && Utilities.isWellDefined(p1))
                  gridLines.add( new Line2D.Double(p0, p1) );
            }
         }
         else 
         {
            // Cartesian secondary axis grid lines -- vert lines located IAW major ticks along primary (X) axis
            for(int i=0; i<ax1Ticks.length; i++ )
            {
               p0.setLocation(ax1Ticks[i],ax2Start);
               p1.setLocation(ax1Ticks[i],ax2End);
               viewport.userUnitsToThousandthInches( p0 );
               viewport.userUnitsToThousandthInches( p1 );
               if(Utilities.isWellDefined(p0) && Utilities.isWellDefined(p1))
                  gridLines.add( new Line2D.Double(p0, p1) );
            }
         }
      }
      else
      {
         // need user and physical coords of polar origin when graph is polar or semilogR.  We cannot render the grid 
         // lines if this origin is not well-defined
         Point2D pOriginUser = new Point2D.Double(ax1Start, ax2Start);
         Point2D pOriginMils = new Point2D.Double(ax1Start, ax2Start);
         viewport.userUnitsToThousandthInches(pOriginMils);
         if(!Utilities.isWellDefined(pOriginMils)) return(gridLines);

         if(isPrimary) 
         {
            // primary axis grid lines are a series of concentric circles or 90-deg arcs about the polar origin; their 
            // radii are specified by the major ticks along the secondary (radial) axis. 
            
            for( int i=0; i<ax2Ticks.length; i++ )
            {
               p1.setLocation( ax1Start, ax2Ticks[i] );
               double r = viewport.convertUserDistanceToThousandthInches(pOriginUser, p1);
               if(r <= 0) continue;

               double left = pOriginMils.getX()-r;
               double bot = pOriginMils.getY()-r;
               double size = r *2;
               if(g.getLayout() == GraphNode.Layout.ALLQUAD)
                  gridLines.add( new Ellipse2D.Double(left, bot, size, size) );
               else
                  gridLines.add( new Arc2D.Double(left, bot, size, size, -ax1Start, -90, Arc2D.OPEN) );
            }
         }
         else
         {
            // radial grid lines emanate from origin out to the maximum radial value, at angular intervals indicated 
            // by the major ticks along the primary (theta) axis.
            for( int i=0; i<ax1Ticks.length; i++ )
            {
               p1.setLocation(ax1Ticks[i], ax2End);
               viewport.userUnitsToThousandthInches( p1 );
               if( Utilities.isWellDefined(p1) )
                  gridLines.add( new Line2D.Double(pOriginMils, p1) );
            }
         }
      }

      return(gridLines);
   }

   /**
    * <code>GridLineNode</code> does not maintain any internal rendering resources.
    * 
    * @see FGraphicNode#releaseRenderResourcesForSelf()
    */
   @Override
   protected void releaseRenderResourcesForSelf() {}

   /**
    * The render bounds for a <code>GridLineNode</code> is empty if the grid lines are not rendered; otherwise, it is 
    * always the data box of its parent <code>GraphNode</code>.
    * 
    * @see FGraphicNode#getRenderBoundsForSelf(Graphics2D, boolean)
    */
   @Override
   protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      Rectangle2D rSelf = new Rectangle2D.Double();
      if(isRendered())
         rSelf.setRect( getParentGraph().getBoundingBoxLocal() );
      return(rSelf);
   }

   /**
   * In a Cartesian graph, the primary axis grid lines are a set of horizontal lines while the secondary axis grid 
   * lines are vertical. In a polar graph, the primary axis grid is a set of circular arcs, while the secondary axis 
   * grid is a set of radial lines. Only major grid lines are drawn, corresponding to the first set of tick marks 
   * (designated the "major" tick marks) defined on the <em>other</em> axis. If an axis's major tick set is empty or 
   * nonexistent, then the corresponding grid lines will be absent. The grid lines or arcs are stroked IAW the stroke 
   * characteristics defined on this <code>GridLineNode</code>. Each grid line or arc is rendered separately, rather 
   * than as part of a single complex path. This ensures that a non-solid dash pattern lines up cleanly on adjacent 
   * grid lines!
   * 
   * @see com.srscicomp.common.g2dviewer.Renderable#render(Graphics2D, RenderTask)
   */
   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(!isRendered() || !needsRendering(task)) return(true);
      
      // prepare list of primitives that must be stroked to render the grid. If it's empty, there's nothing to do.
      List<Shape> gridLines = prepareGridPrimitives();
      if(gridLines.size() == 0) return(true);

      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         g2dCopy.setColor(getStrokeColor());
         g2dCopy.setStroke(getStroke(0));
         for(Shape line : gridLines) 
            g2dCopy.draw(line);
      }
      finally { if(g2dCopy != null) g2dCopy.dispose(); }

      return((task == null) ? true : task.updateProgress());
   }

   
   //
   // PSTransformable implementation
   //

   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null || getHide() || !isStroked()) return;
      
      GraphNode g = getParentGraph();
      boolean isPolar = g.isPolar();

      // get the axis endpoints and major ticks -- which we need to determine placement of grid lines
      AxisNode axis = g.getPrimaryAxis();             // x- or theta-axis
      double[] ax1Ticks = axis.getMajorTicks();       // x-coords of vertical lines, or theta-coords of radial lines
      double ax1Start = axis.getValidatedStart();     // maps to left edge of data box in Cartesian coord sys
      double ax1End = axis.getValidatedEnd();         // maps to right edge of data box in Cartesian coord sys

      axis = g.getSecondaryAxis();                    // y- or radial-axis
      double[] ax2Ticks = axis.getMajorTicks();       // y-coords of horiz lines, or radial-coords of theta circles 
      double ax2Start = axis.getValidatedStart();     // maps to bot edge of data box in Cartesian coord sys
      double ax2End = axis.getValidatedEnd();         // maps to top edge of data box in Cartesian coord sys

      // there may be no grid lines at all!
      if(isPrimary ? (ax2Ticks.length == 0) : (ax1Ticks.length == 0))
         return;

      Point2D p0 = new Point2D.Double();
      Point2D p1 = new Point2D.Double();

      // start element, setting up graphics state IAW styles on this GridElement
      psDoc.startElement(this);

      // render the grid lines IAW graph type...
      if(!isPolar)
      {
         if(isPrimary) 
         {
            // primary axis grid lines -- horizontal lines located IAW major ticks along secondary (Y) axis
            for(int i = 0; i < ax2Ticks.length; i++)
            {
               p0.setLocation(ax1Start, ax2Ticks[i]);
               p1.setLocation(ax1End, ax2Ticks[i]);
               parentVP.userUnitsToThousandthInches(p0);
               parentVP.userUnitsToThousandthInches(p1);
               if(Utilities.isWellDefined(p0) && Utilities.isWellDefined(p1))
                  psDoc.renderLine(p0, p1);
            }
         }
         else 
         {
            // secondary axis grid lines -- vertical lines located IAW major ticks along primary (X) axis
            for(int i = 0; i < ax1Ticks.length; i++)
            {
               p0.setLocation(ax1Ticks[i],ax2Start);
               p1.setLocation(ax1Ticks[i], ax2End);
               parentVP.userUnitsToThousandthInches(p0);
               parentVP.userUnitsToThousandthInches(p1);
               if(Utilities.isWellDefined(p0) && Utilities.isWellDefined(p1))
                  psDoc.renderLine(p0, p1);
            }
         }
      }
      else
      {
         // need user and physical coords of polar origin when graph is polar or semilogR.  We cannot render the grid 
         // lines if this origin is not well-defined
         Point2D pOriginUser = new Point2D.Double(ax1Start, ax2Start);
         Point2D pOriginMils = new Point2D.Double(ax1Start, ax2Start);
         parentVP.userUnitsToThousandthInches(pOriginMils);
         if(Utilities.isWellDefined(pOriginMils))
         {
            if(isPrimary) 
            {
               // primary axis grid lines are a series of concentric arcs about the polar origin; their radii are 
               // specified by the major ticks along the secondary (radial) axis. In an "all-quad" graph, the arcs are 
               // actually complete circles, else they are quarter-circles.

               // compute radii, in physical units, of the concentric circles.
               double[] radii = new double[ax2Ticks.length];
               for(int i = 0; i < ax2Ticks.length; i++)
               {
                  p1.setLocation(ax1Start, ax2Ticks[i]);
                  radii[i] = parentVP.convertUserDistanceToThousandthInches(pOriginUser, p1);
               }

               if(g.getLayout() == GraphNode.Layout.ALLQUAD)
                  psDoc.renderConcentricCircles(pOriginMils, radii);
               else
                  psDoc.renderConcentricArcs(pOriginMils, ax1Start, ax1Start+90, radii);
            }
            else
            {
               // radial grid lines emanate from origin out to the maximum radial value, at angular intervals indicated 
               // by the major ticks along the primary (theta) axis.
               for(int i = 0; i < ax1Ticks.length; i++)
               {
                  p1.setLocation(ax1Ticks[i], ax2End);
                  parentVP.userUnitsToThousandthInches(p1);
                  if(Utilities.isWellDefined(p1))
                     psDoc.renderLine(pOriginMils, p1);
               }
            }
         }
      }

      psDoc.endElement();
   }
}
