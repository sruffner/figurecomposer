package com.srscicomp.fc.fig;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.srscicomp.common.g2dutil.Marker;
import com.srscicomp.common.g2dviewer.RenderTask;

/**
 * <code>SymbolNode</code> is a <strong><em>non-renderable, non-focusable</em></strong> graphic node that defines 
 * the appearance of marker symbols drawn at the locations of plotted points along a polyline-like rendering of a 
 * <code>FGNPlottable</code> parent node. It exists so that the author can style the marker symbols differently from 
 * the polyline itself, which is controlled by draw styles defined on the <code>FGNPlottable</code>. It extends 
 * <code>FGraphicNode</code> only so that it can take advantage of base-class support for the common <em>DataNav</em> 
 * style properties and the style inheritance mechanism.
 * 
 * <p>In addition to the <em>DataNav</em> fill color and stroking styles, <code>SymbolNode</code> includes 
 * properties defining the <em>type</em> and <em>size</em> of the symbol, plus a single-character <em>title</em> 
 * property. This latter property makes "character symbols" possible. During rendering, the specified symbol marker 
 * shape is filled with the current fill color and stroked IAW the specified stroking styles. If the <em>title</em> 
 * property is not empty, the single character is rendered at the symbol's center point, centered both horizontally and 
 * vertically. The font attributes that govern the rendering of the character glyph are inherited from the parent of the
 * <code>SymbolNode</code>, which will always be an instance of <code>FGNPlottable</code>.</p>
 * 
 * @author sruffner
 */
public class SymbolNode extends FGraphicNode implements Cloneable
{

   /**
    * Construct a <code>SymbolNode</code> representing a zero size, circle-shaped marker symbol with an empty symbol 
    * character. All relevant style attributes are initially implicit.
    */
   public SymbolNode()
   {
      super(HASFILLCATTR|HASSTROKEATTRS|HASSTRKPATNATTR|HASTITLEATTR);
      type = Marker.CIRCLE;
      size = new Measure(0.0, Measure.Unit.IN);
      setTitle("");
   }

   
   //
   // Support for child nodes -- none permitted!
   //
   @Override
   public FGNodeType getNodeType() { return(FGNodeType.SYMBOL); }

   @Override
   public boolean isLeaf() { return(true); }

   @Override
   public boolean canInsert(FGNodeType nodeType) { return(false); }

   /**
    * A <code>SymbolNode</code> is instrinsic to the definition of its parent <code>FGNPlottable</code>.
    * 
    * @see FGraphicNode#isComponentNode()
    */
   @Override
   public boolean isComponentNode() { return(true); }

   
   //
   // Properties
   //
   
   /**
    * The type of marker symbol defined by this <code>SymbolNode</code>. 
    */
   private Marker type;

   /**
    * Get the type of marker symbol defined by this <code>SymbolNode</code>. 
    * 
    * @return A <code>Marker</code> object that defines the shape within a unit "design box". This unit shape is then 
    * scaled IAW the <code>SymbolNode</code>'s <em>size</em> property.
    */
   public Marker getType() { return(type); }

   /**
    * Set the type of marker symbol defined by this <code>SymbolNode</code>. If a change is made, the method 
    * <code>onNodeModified()</code> is invoked.
    * 
    * @param type A <code>Marker</code> specifying the new marker symbol shape. A <code>null</code> value is rejected.
    * @return <code>True</code> if successful; <code>false</code> if value was rejected.
    */
   public boolean setType(Marker type)
   {
      if(type == null) return(false);

      if(this.type != type)
      {
         if(doMultiNodeEdit(FGNProperty.TYPE, type)) return(true);
         
         Marker oldType = this.type;
         this.type = type;
         if(this.areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.TYPE);
            String desc = "Set trace symbol type to: " + this.type.toString();
            FGNRevEdit.post(this, FGNProperty.TYPE, this.type, oldType, desc);
         }
      }
      return(true);
   }

   /**
    * Constraints on the size of the marker symbol: must use non-relative units (in, cm, mm or pt); limited to 4
    * siginficant and 3 fractional digits; must be nonnegative; and maximum allowed size is 1in.
    */
   public final static Measure.Constraints SYMBOLSIZECONSTRAINTS = new Measure.Constraints(0, 1000.0, 4, 3);

   /**
    * The rendered size of the marker symbol defined by this <code>SymbolNode</code>.
    */
   private Measure size;

   /**
    * Get the <code>Measure</code> representing the rendered size of the marker symbol defined by this 
    * <code>SymbolNode</code>.
    * 
    * @return The rendered size of marker symbol, with associated units of measure (only physical units allowed).
    */
   public Measure getSize() { return(size); }

   /**
    * Set the rendered size of the marker symbol defined by this symbol node. If a change is made, {@link 
    * #onNodeModified()} is invoked.
    * 
    * @param m The new size. It is constrained to satisfy {@link #SYMBOLSIZECONSTRAINTS}. A null value is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setSize(Measure m)
   {
      if(m == null) return(false);
      m = SYMBOLSIZECONSTRAINTS.constrain(m);

      boolean changed = (size != m) && !Measure.equal(size, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.SIZE, m)) return(true);
         
         Measure oldSize = size;
         size = m;
         if(this.areNotificationsEnabled())
         {
            if(oldSize.toMilliInches() != size.toMilliInches())
               onNodeModified(FGNProperty.SIZE);
            String desc = "Set trace symbol size to: " + size.toString();
            FGNRevEdit.post(this, FGNProperty.SIZE, size, oldSize, desc);
         }
      }
      return(true);
   }

   /**
    * For a <code>SymbolNode</code>, the <em>title</em> property can only contain a single character at most. If 
    * the specified string is longer than that, it is rejected.
    * 
    * @see FGraphicNode#setTitle(String)
    */
   @Override
   public boolean setTitle(String s)
   {
      return(((s != null) && (s.length() > 1)) ? false : super.setTitle(s));
   }

   @Override protected void rescaleSelf(double scale, MultiRevEdit undoer)
   {
      Measure.Constraints c = SYMBOLSIZECONSTRAINTS;
      double d = size.getValue();
      if(d > 0)
      {
         Measure old = size;
         size = c.constrain(new Measure(d*scale, size.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.SIZE, size, old);
      }
   }

   /**
    * Whenever this <code>SymbolNode</code>'s definition changes, the parent <code>FGNPlottable</code> is 
    * notified in case the change affects that node's rendering.
    */
   @Override
   protected void onNodeModified(Object hint)
   {
      FGraphicModel model = getGraphicModel();
      if(model == null) return;

      // if the parent does not use symbols, OR if symbol size is zero and there is no symbol character (and neither 
      // of these was the property changed), then rendering is unaffected. Otherwise, inform parent, passing this node
      // as a hint!
      if(getParent() instanceof FGNPlottable)
      {
         FGNPlottable plottable = (FGNPlottable) getParent();
         if((!plottable.usesSymbols()) || 
               (hint != null && hint != FGNProperty.SIZE && hint != FGNProperty.TITLE && 
                     getSizeInMilliInches() == 0 && getTitle().length() == 0))
            model.onChange(this, 0, false, null);
         else
            plottable.onNodeModified(this);
      }
   }

   @Override boolean setPropertyValue(FGNProperty p, Object propValue)
   {
      boolean ok = false;
      switch(p)
      {
         case SIZE : ok = setSize((Measure) propValue); break;
         case TYPE : ok = setType((Marker)propValue); break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
         case SIZE : value = getSize(); break;
         case TYPE : value = getType(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }


   /**
    * Get the rendered size of the marker symbol in milli-inches.
    * @return The size in milli-inches.
    */
   public double getSizeInMilliInches() { return(size.toMilliInches()); }
   
   /**
    * Get the character that is centered within the marker symbol (if any).
    * @return A single-character string, or an empty string if no character should be drawn in the symbol.
    */
   public String getCharacter() { return(getTitle()); }
   

   //
   // Support for style sets
   //
   
   /**
    * While style sets are supported, <code>SymbolNode</code> only occurs as a component of a function or trace node,
    * and it should not receive the display focus in the UI.
    */
   @Override public boolean supportsStyleSet() { return(true); }

   /** 
    * The node-specific style properties exported in the symbol node's style set include the symbol type and size, and
    * the single character that can appear centered within the symbol.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.TYPE, getType());
      styleSet.putStyle(FGNProperty.SIZE, getSize());
      styleSet.putStyle(FGNProperty.TITLE, getCharacter());
   }

   /** Accounts for the symbol node-specific styles: symbol type, size, and centered single-character label. */
   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      Marker m = (Marker) applied.getCheckedStyle(FGNProperty.TYPE, getNodeType(), Marker.class);
      if(m != null && !m.equals(restore.getStyle(FGNProperty.TYPE)))
      {
         type = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.TYPE);
      
      Measure sz = (Measure) applied.getCheckedStyle(FGNProperty.SIZE, getNodeType(), Measure.class);
      if(sz != null) sz = SYMBOLSIZECONSTRAINTS.constrain(sz);
      if(sz != null && !Measure.equal(sz, (Measure) restore.getCheckedStyle(FGNProperty.SIZE, null, Measure.class)))
      {
         size = sz;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.SIZE);
      
      String title = (String) applied.getCheckedStyle(FGNProperty.TITLE, getNodeType(), String.class);
      if(title != null && title.length() > 1) title = title.substring(0,1);
      if(title != null && !title.equals(restore.getCheckedStyle(FGNProperty.TITLE, getNodeType(), String.class)))
      {
         if(!areNotificationsEnabled()) setTitle(title);
         else
         {
            setNotificationsEnabled(false);
            try{ setTitle(title); }
            finally{ setNotificationsEnabled(true); }
         }
         changed = true;
      }
      else restore.removeStyle(FGNProperty.TITLE);

      return(changed);
   }


   //
   // Focusable/Renderable/PSTransformable support -- never rendered directly
   //

   /** Returns null always: the symbol node is not rendered and therefore cannot be "clicked". */
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

}
