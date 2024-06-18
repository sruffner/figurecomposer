package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dviewer.RenderTask;

/**
 * <code>ErrorBarNode</code> is a <strong><em>non-renderable, non-focusable</em></strong> graphic node that 
 * encapsulates the styling of error bars in the polyline-like rendering of a <code>TraceNode</code> parent node. It 
 * extends <code>FGraphicNode</code> only so that it can take advantage of base-class support for the common 
 * <em>DataNav</em> style properties and the style inheritance mechanism. In addition to the <em>DataNav</em> fill 
 * color and stroking styles, <code>ErrorBarNode</code> includes properties defining the type and size of the 
 * adornment rendered at the +/-1 STD endpoints of individual error bars. It also includes a <em>hide</em> 
 * flag which, if set, disables the rendering of all error bars.</p>
 * 
 * <p>While there are four implementations of <code>FGNPlottable</code> in the <em>DataNav</em> graphic model, only 
 * <code>TraceNode</code> includes an <code>ErrorBarNode</code> as a component node.</p>
 * 
 * @author sruffner
 */
public class ErrorBarNode extends FGraphicNode implements Cloneable
{
   /**
    * Construct an <code>ErrorBarNode</code>. The error bars are hidden initially. The error bar endcap symbol and size 
    * are initialized IAW user-defined preferred values. All relevant style attributes are initially implicit.
    */
   public ErrorBarNode()
   {
      super(HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR);
      endCap = FGNPreferences.getInstance().getPreferredEBarCap();
      endCapSize = FGNPreferences.getInstance().getPreferredEBarCapSize();
      hide = true;
   }

   
   //
   // Support for child nodes -- none permitted!
   //
   @Override public FGNodeType getNodeType() { return(FGNodeType.EBAR); }

   @Override public boolean isLeaf() { return(true); }

   @Override public boolean canInsert(FGNodeType nodeType) { return(false); }

   /**
    * An <code>ErrorBarNode</code> is instrinsic to the definition of its parent <code>TraceNode</code>.
    * @see FGraphicNode#isComponentNode()
    */
   @Override public boolean isComponentNode() { return(true); }

   
   //
   // Properties
   //
   
   /** The error bar endcap adornment. */
   private Marker endCap;

   /**
    * Get the error bar endcap shape defined by this <code>ErrorBarNode</code>. 
    * 
    * @return A <code>Marker</code> object that defines the endcap shape within a unit "design box". This unit shape is 
    * then scaled IAW the <code>ErrorBarNode</code>'s <em>endCapSize</em> property.
    */
   public Marker getEndCap() { return(endCap); }

   /**
    * Set the type of error bar endcap shape defined by this <code>ErrorBarNode</code>. If a change is made, the 
    * method <code>onNodeModified()</code> is invoked.
    * 
    * <p>A <code>null</code> value is not permitted. To prevent rendering of an endcap, set the endcap size to zero.</p>
    * 
    * @param endCap A <code>Marker</code> specifying the new error bar endcap shape. A <code>null</code> value is 
    * rejected.
    * @return <code>True</code> if successful; <code>false</code> if value was rejected.
    */
   public boolean setEndCap(Marker endCap)
   {
      if(endCap == null) return(false);

      if(this.endCap != endCap)
      {
         if(doMultiNodeEdit(FGNProperty.CAP, endCap)) return(true);
         
         Marker oldCap = this.endCap;
         this.endCap = endCap;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.CAP);
            String desc = "Set endcap on trace error bars to: " + this.endCap.toString();
            FGNRevEdit.post(this, FGNProperty.CAP, this.endCap, oldCap, desc);
         }
      }
      return(true);
   }

   /**
    * Constraints on the size of the error bar end-cap adornment defined: must use non-relative units (in, cm, mm or 
    * pt); limited to 4 significant and 3 fractional digits; must be nonnegative; and maximum allowed size is 1in.
    */
   public final static Measure.Constraints EBARCAPSIZECONSTRAINTS = new Measure.Constraints(0, 1000.0, 4, 3);

   /** The error bar endcap adornment size. */
   private Measure endCapSize;

   /**
    * Get the <code>Measure</code> representing the rendered size of the error bar endcap defined by this 
    * <code>ErrorBarNode</code>.
    * 
    * @return The rendered size of error bar endcap, with associated units of measure (only physical units allowed). 
    * Zero size means that endcaps are not rendered.
    */
   public Measure getEndCapSize() { return(endCapSize); }

   /**
    * Set the rendered size of the error bar endcap defined by this <code>ErrorBarNode</code>. If a change is made, 
    * <code>onNodeModified()</code> is invoked.
    * 
    * @param m The new size. The measure is constrained to satisfy {@link #EBARCAPSIZECONSTRAINTS}. Null is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setEndCapSize(Measure m)
   {
      if(m == null) return(false);
      m = EBARCAPSIZECONSTRAINTS.constrain(m);

      boolean changed = (endCapSize != m) && !Measure.equal(endCapSize, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.CAPSIZE, m)) return(true);
         
         Measure oldSize = endCapSize;
         endCapSize = m;
         if(areNotificationsEnabled())
         {
            if(oldSize.toMilliInches() != endCapSize.toMilliInches())
               onNodeModified(FGNProperty.CAPSIZE);
            String desc = "Set endcap size on trace error bars to: " + endCapSize.toString();
            FGNRevEdit.post(this, FGNProperty.CAPSIZE, endCapSize, oldSize, desc);
         }
      }
      return(true);
   }

   /**
    * Same as {@link #setEndCapSize(Measure)}, except no undo operation is posted and {@link #onNodeModified} is not
    * invoked.
    * @param m The new size. The measure is constrained to satisfy {@link #EBARCAPSIZECONSTRAINTS}. Null is rejected.
    * @return True if successful; false if value was rejected.
    */
   boolean setEndCapSizeNoNotify(Measure m)
   {
      if(m == null) return(false);
      m = EBARCAPSIZECONSTRAINTS.constrain(m);

      endCapSize = m;
      return(true);
   }
   
   /**
    * Convenience method returns the rendered size of error bar endcap in milli-inches.
    * @return Error bar endcap size in milli-inches.
    */
   public double getEndCapSizeInMilliInches() { return(endCapSize.toMilliInches()); }

   /** If this flag is set, error bars are not rendered at all by the parent <code>FGNPlottable</code>. */
   private boolean hide;

   /**
    * Get the current state of the <em>hide</em> flag, which governs whether or not error bars are rendered at all by 
    * the parent <code>FGNPlottable</code>.
    * 
    * @return <code>True</code> iff error bars should be hidden.
    */
   public boolean getHide() { return(hide); }

   /**
    * Set the current state of the <em>hide</em> flag, which governs whether or not error bars are rendered at all by 
    * the parent <code>FGNPlottable</code>. If a change is made, <code>onNodeModified()</code> is invoked.
    * 
    * @param b <code>True</code> to hide error bars; <code>false</code> to show them.
    */
   public void setHide(boolean b)
   {
      if(hide != b)
      {
         if(doMultiNodeEdit(FGNProperty.HIDE, b)) return;
         
         Boolean old = hide;
         hide = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.HIDE);
            String desc = (hide ? "Hide " : "Show ") + "error bars/band for trace";
            FGNRevEdit.post(this, FGNProperty.HIDE, hide, old, desc);
         }
      }
   }

   @Override protected void rescaleSelf(double scale, MultiRevEdit undoer)
   {
      double d = endCapSize.getValue();
      if(d > 0)
      {
         Measure old = endCapSize;
         endCapSize = EBARCAPSIZECONSTRAINTS.constrain(new Measure(d*scale, endCapSize.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.CAPSIZE, endCapSize, old);
      }
   }

   /**
    * Whenever this <code>ErrorBarNode</code>'s definition changes, the parent <code>FGNPlottable</code> is 
    * notified in case the change affects that node's rendering.
    */
   @Override
   protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      // if the parent does not have error data, then its rendering is unaffected by any change to this node. Otherwise,
      // force a re-rendering of the parent.
      if(getParent() instanceof FGNPlottable)
      {
         FGNPlottable plottable = (FGNPlottable) getParent();
         if(!plottable.hasErrorData())
            model.onChange(this, 0, false, null);
         else
            plottable.onNodeModified(this);
      }
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok;
      switch(p)
      {
         case CAP : ok = setEndCap((Marker) propValue); break;
         case CAPSIZE : ok = setEndCapSize((Measure) propValue); break;
         case HIDE: setHide((Boolean)propValue); ok = true; break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value;
      switch(p)
      {
         case CAP : value = getEndCap(); break;
         case CAPSIZE : value = getEndCapSize(); break;
         case HIDE: value = getHide(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }

   
   // 
   // Support for style sets
   //
   
   /**
    * While style sets are supported, <code>ErrorBarNode</code> only occurs as a component of a trace node. It is 
    * assumed the parent trace node provides a suitable text summary of the error bar styling.
    */
   @Override public boolean supportsStyleSet() { return(true); }

   /** 
    * The node-specific style properties exported in the error bar node's style set include the end-cap type and size, 
    * and the flag which governs whether or not error information is rendered at all.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.CAP, getEndCap());
      styleSet.putStyle(FGNProperty.CAPSIZE, getEndCapSize());
      styleSet.putStyle(FGNProperty.HIDE, getHide());
   }

   /** Accounts for the error bar node-specific styles: end-cap type and size, and the "hide error bars" flag. */
   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      Marker m = (Marker) applied.getCheckedStyle(FGNProperty.CAP, getNodeType(), Marker.class);
      if(m != null && !m.equals(restore.getStyle(FGNProperty.CAP)))
      {
         endCap = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.CAP);
      
      Measure sz = (Measure) applied.getCheckedStyle(FGNProperty.CAPSIZE, getNodeType(), Measure.class);
      if(sz != null) sz = EBARCAPSIZECONSTRAINTS.constrain(sz);
      if(sz != null && !Measure.equal(sz, (Measure) restore.getCheckedStyle(FGNProperty.CAPSIZE, null, Measure.class)))
      {
         endCapSize = sz;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.CAPSIZE);
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.HIDE, getNodeType(), Boolean.class);
      if(b != null && !b.equals(restore.getCheckedStyle(FGNProperty.HIDE, getNodeType(), Boolean.class)))
      {
         hide = b;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.HIDE);
      
      return(changed);
   }


   //
   // Focusable/Renderable/PSTransformable support -- never rendered!
   //

   @Override protected FGraphicNode hitTest(Point2D p) { return(null); }
   @Override protected void releaseRenderResourcesForSelf() {}
   @Override public AffineTransform getLocalToParentTransform() { return(new AffineTransform()); }
   @Override public FViewport2D getViewport() { return(null); }
   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      return(new Rectangle2D.Double());
   }
   public boolean render(Graphics2D g2d, RenderTask task) { return(true); }
   
   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException { /* NOOP */ }

   @Override public ErrorBarNode clone() throws CloneNotSupportedException
   {
      return (ErrorBarNode) super.clone();
   }
}
