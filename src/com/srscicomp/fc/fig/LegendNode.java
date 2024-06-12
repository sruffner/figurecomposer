package com.srscicomp.fc.fig;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

import com.srscicomp.common.g2dutil.SingleStringPainter;
import com.srscicomp.common.g2dutil.TextAlign;
import com.srscicomp.common.g2dviewer.RenderTask;
import com.srscicomp.fc.uibase.FCIcons;

/**
 * Many plots are accompanied by a legend that includes, for each data set plotted, the line stroke and symbol used to 
 * plot the data, along with a short descriptive name. It is possible to construct a legend using a combination of 
 * generic <i>DataNav</i> graphic nodes -- {@link LineNode}, {@link ShapeNode}, and {@link LabelNode}. However, each 
 * time the author adds or deletes a data trace from a graph element or changes some aspect of an existing trace, the 
 * associated legend must also be updated. <b>LegendNode</b> is a higher-level construct offering a legend that is 
 * automatically updated in response to changes in the content and structure of its parent graph. It is a required 
 * component node of any 2D or 3D graph, and it can be hidden if the author does not wish to show it.
 * 
 * <p><i>Properties</i>. A pair of measured coordinates <i>(x, y)</i> define the location of the legend's bottom left 
 * corner WRT the parent graph's viewport, while the <i>rotate</i> property can be used to change the orientation of the 
 * legend. The <i>spacer</i> property gives the perpendicular distance separating successive entries in the legend, 
 * while the <i>len</i> property defines the length of each entry's graphical representation, not including the entry
 * label. The latter property can be zero, in which case the entry length defaults to 5x the maximum symbol size 
 * rendered, or 0.5in if there are no symbols. The <i>size</i> property specifies a uniform size for all marker symbols 
 * rendered in the legend. Otherwise, marker symbols are rendered at the same size they're rendered in the corresponding
 * data trace. The <i>labelOffset</i> property, introduced in program version 4.4.0, controls the distance between the 
 * right end of each entry's graphic representation and the start of the text label for that entry (prior to V4.4.0, the
 * offset was hard-coded at 0.1in). Finally, the boolean <i>mid</i> property determines whether markers are displayed at
 * the midpoint or at the endpoints of each legend entry.</p>
 * 
 * <p>Support for a "boxed" legend was introduced in V5.2.1. The legend's bounding box may be optionally outlined and 
 * filled. The outline's stroke width is specified by the <i>border</i> property, while the stroke color is the same as
 * the legend's text/fill color. The <i>boxColor</i> property specifies the fill color for the legend box. By default,
 * the border width is 0in and the box color is transparent black -- so that no legend box is rendered. The bounding
 * box includes a narrow margin of 0.3*F, where F is the legend node's font size.</p>
 * 
 * <p><i>Laying out the legend</i>: The parent graph is queried for a list of all {@link FGNPlottable} children it 
 * contains. Each such <i>data presentation node</i> supplies a {@link LegendEntry} object which handles the task of 
 * laying out and rendering the corresponding legend entry, based on information from the presentation node and the
 * legend container. <b>LegendNode</b> itself renders the label for each entry, which may or may not contain "attributed
 * text"; regardless, the legend node's own font family and font size apply to the text, and the initial default text
 * color and font styles are also taken from the legend node (font style and text color can change from character to
 * character in "attributed text", but font family and size cannot). The label is placed to the right of and vertically 
 * centered WRT the entry's graphical representation, offset IAW the value of the <i>labelOffset</i> attribute. If the 
 * legend entry object supplies no label, a default label of the form "data N" is used instead. The legend entries are 
 * arranged from top to bottom in the order that the data presentation nodes appear as children of the parent graph. The
 * left end point of the last entry lies one "spacer" above the legend's bottom left corner.</p>
 * 
 * <p><i>Omitting a legend entry</i>. If {@link FGNPlottable#getShowInLegend()} returns false, no entry is prepared for 
 * that particular data presentation node. This provides a mechanism by which the author can exclude selected traces 
 * from the automated legend. If all traces are thus excluded, the legend is not rendered.</p>
 * 
 * @author 	sruffner
 */
public class LegendNode extends FGraphicNode implements Cloneable
{

   /**
    * Construct a graph legend located at (0in, 0in) WRT the parent graph viewport, unrotated, with an initial entry 
    * line length of 0.5in, and a uniform symbol size of 0.1in. The box border width is 0in and the box color is 
    * transparent black -- so that the legend box is not rendered. Three other properties, the <i>mid</i>, i>spacer</i>, 
    * and <i>labelOffset</i> properties, are initialized IAW user-defined preferred values. Initially the
    * legend is hidden. To turn it on, use {@link #setHide(boolean)}.
    */
   public LegendNode()
   {
      super(HASFONTATTRS|HASFILLCATTR|HASLOCATTRS|HASROTATEATTR);
      setXY(new Measure(0, Measure.Unit.IN), new Measure(0, Measure.Unit.IN));
      setRotate(0);
      length = new Measure(0.5, Measure.Unit.IN);
      spacer = FGNPreferences.getInstance().getPreferredLegendSpacer();
      labelOffset = FGNPreferences.getInstance().getPreferredLegendLabelOffset();
      symbolSize = new Measure(0.1, Measure.Unit.IN);
      mid = FGNPreferences.getInstance().getPreferredLegendSymbolAtMidPoint().booleanValue();
      hide = true;
      borderWidth = new Measure(0, Measure.Unit.IN);
      boxColor = new Color(0f, 0f, 0f, 0f);
   }

   /**
    * The legend's parent graph container (2D or 3D graph) should invoke this method whenever a data presentation node
    * is inserted into or removed from the graph. The legend may need to be redrawn to reflect the change. If the legend
    * is currently hidden, no action is taken.
    */
   void onInsertOrRemovePlottableNode()
   {
      FGraphicModel model = getGraphicModel();
      if(model != null && !hide)
      {
         Graphics2D g2d = model.getViewerGraphics();
         if(g2d == null) return;
         try
         {
            // legend's global render bounds before the change
            List<Rectangle2D> dirty = new ArrayList<Rectangle2D>();
            Rectangle2D r = getCachedGlobalBounds();
            if(!r.isEmpty()) dirty.add(r);
            
            // force recalc after the change and recompute global render bounds
            getRenderBounds(g2d, true, null);
            r = getCachedGlobalBounds();
            if(!r.isEmpty()) dirty.add(r);
            
            // notify model to re-render in the dirty regions, if any
            if(dirty.size() > 0) model.onChange(this, -1, true, dirty);
         }
         finally { if(g2d != null) g2d.dispose(); }
      }
      
   }
   
   //
   // Support for child nodes -- none permitted
   //
   
   @Override public FGNodeType getNodeType() { return(FGNodeType.LEGEND); }
   @Override public boolean isLeaf() { return(true); }
   @Override public boolean canInsert(FGNodeType nodeType) { return(false); }

   /** The legend node is a required component of any 2D or 3D graph node. */
   @Override public boolean isComponentNode() { return(true); }
   
   
   //
   // Properties
   //

   /** The length of the trace line segment for all legend entries. */
   private Measure length;
   
   /**
    * Constrains the measured properties <i>len</i>, <i>spacer</i>, <i>size</i>, and <i>labelOffset</i> to use physical 
    * units, a maximum of 4 significant and 3 fractional digits, and lie in the range [0..5in].
    */
   public final static Measure.Constraints LENCONSTRAINTS = new Measure.Constraints(0.0, 5000.0, 4, 3);

   /**
    * Get the length of the trace line segment for a legend entry.
    * 
    * @return The legend entry line length, specified as a linear measurement with associated units.
    */
   public Measure getLength() { return(length); }

   /**
    * Set the length of the trace line segment for a legend entry. If a change is made, {@link #onNodeModified()} is 
    * invoked.
    * 
    * @param m The new legend entry line length. It is constrained to satisfy {@link #LENCONSTRAINTS}. Null is rejected.
    * @return True if change was accepted; false if rejected.
    */
   public boolean setLength(Measure m)
   {
      if(m == null) return(false);
      m = LENCONSTRAINTS.constrain(m);

      boolean changed = (length != m) && !Measure.equal(length, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.LEN, m)) return(true);
         
         Measure oldLen = length;
         length = m;
         if(areNotificationsEnabled())
         {
            if(oldLen.toMilliInches() != length.toMilliInches()) onNodeModified(FGNProperty.LEN);
            String desc = "Set legend entry length to: " + length.toString();
            FGNRevEdit.post(this, FGNProperty.LEN, length, oldLen, desc);
         }

      }
      return(true);
   }

   /**
    * The vertical distance between the trace line segments of successive legend entries. Also, the last entry is this 
    * distance above the bottom edge of the legend.
    */
   private Measure spacer;

   /**
    * Get the perpendicular distance between the trace line segments of successive legend entries.
    * @return The legend entry spacer, specified as a linear measurement with associated units.
    */
   public Measure getSpacer() { return(spacer); }

   /**
    * Set the perpendicular distance between the trace line segments of successive legend entries. If a change is made, 
    * {@link #onNodeModified()} is invoked.
    * 
    * @param m The new legend entry spacer. It is constrained to satisfy {@link #LENCONSTRAINTS}. Null is rejected.
    * @return True if change was accepted; false if rejected.
    */
   public boolean setSpacer(Measure m)
   {
      if(m == null) return(false);
      m = LENCONSTRAINTS.constrain(m);

      boolean changed = (spacer != m) && !Measure.equal(spacer, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.SPACER, m)) return(true);
         
         Measure oldSpacer = spacer;
         spacer = m;
         if(areNotificationsEnabled())
         {
            if(oldSpacer.toMilliInches() != spacer.toMilliInches()) onNodeModified(FGNProperty.SPACER);
            String desc = "set legend entry spacing to: " + spacer.toString();
            FGNRevEdit.post(this, FGNProperty.SPACER, spacer, oldSpacer, desc);
         }
      }
      return(true);
   }

   /** Horizontal offset from right end point of each entry's line segment to start of its text label. */
   private Measure labelOffset;

   /**
    * Get the horizontal offset from the right end point of a legend entry's line segment to the start of text label.
    * @return The legend label offset, specified as a linear measurement with associated units.
    */
   public Measure getLabelOffset() { return(labelOffset); }

   /**
    * Set the horizontal offset from the right end point of a legend entry's line segment to the start of text label.
    * If a change is made, {@link #onNodeModified()} is invoked.
    * 
    * @param m The new legend label offset. The measure is constrained to satisfy {@link #LENCONSTRAINTS}. A null value 
    * is rejected.
    * @return True if change was accepted; false if rejected.
    */
   public boolean setLabelOffset(Measure m)
   {
      if(m == null) return(false);
      m = LENCONSTRAINTS.constrain(m);

      boolean changed = (labelOffset != m) && !Measure.equal(labelOffset, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.LABELOFFSET, m)) return(true);
         
         Measure oldOffset = labelOffset;
         labelOffset = m;
         if(areNotificationsEnabled())
         {
            if(oldOffset.toMilliInches() != labelOffset.toMilliInches()) onNodeModified(FGNProperty.LABELOFFSET);
            String desc = "Set legend entry label offset to: " + labelOffset.toString();
            FGNRevEdit.post(this, FGNProperty.LABELOFFSET, labelOffset, oldOffset, desc);
         }
      }
      return(true);
   }

   /**
    * If nonzero, all marker symbols in the legend are rendered at this size. Otherwise, each marker symbol is rendered 
    * at the size at which it appears in the parent graph.
    */
   private Measure symbolSize;

   /**
    * Get the uniform symbol size applied to all marker symbols rendered in the legend. If the size is zero, then 
    * markers are rendered at the same size that they appear in the parent graph.
    * 
    * @return Legend's uniform marker symbol size, with associated units of measure (only physical units allowed).
    */
   public Measure getSymbolSize() { return(symbolSize); }

   /**
    * Set the uniform symbol size applied to all marker symbols rendered in the legend. If a change is made, {@link 
    * #onNodeModified()} is invoked.
    * 
    * @param m The new uniform symbol size. It is constrained to satisfy {@link #LENCONSTRAINTS}. Null is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setSymbolSize(Measure m)
   {
      if(m==null) return(false);
      m = LENCONSTRAINTS.constrain(m);

      boolean changed = (symbolSize != m) && !Measure.equal(symbolSize, m);
      if(changed)
      {
         if(doMultiNodeEdit(FGNProperty.SIZE, m)) return(true);
         
         Measure oldSize = symbolSize;
         symbolSize = m;
         if(areNotificationsEnabled())
         {
            if(oldSize.toMilliInches() != symbolSize.toMilliInches()) onNodeModified(FGNProperty.SIZE);
            String desc = "Set legend entry symbol size to: " + symbolSize.toString();
            FGNRevEdit.post(this, FGNProperty.SIZE, symbolSize, oldSize, desc);
         }
      }
      return(true);
   }

   /**
    * If this flag is set, marker symbols (if present) are drawn at the midpoint of the trace line segment in each 
    * entry; otherwise, they're drawn at both end points of the trace line.
    */
   private boolean mid;

   /**
    * Get the flag which determines whether a marker symbol is rendered at the midpoint or the end points of a legend 
    * entry.
    * 
    * @return <code>True</code> if the marker symbol should be rendered at midpoint; else, at both end points of a 
    * legend entry's line segment.
    */
   public boolean getMid() { return(mid); }

   /**
    * Set the flag which determines whether a marker symbol is rendered at the midpoint or the end points of a legend 
    * entry. If a change is made, {@link #onNodeModified()} is invoked.
    * 
    * @param b True to draw markers at midpoint of a legend entry, false to draw them at the end points.
    */
   public void setMid(boolean b)
   {
      if(mid != b)
      {
         if(doMultiNodeEdit(FGNProperty.MID, new Boolean(b))) return;
         
         Boolean old = new Boolean(mid);
         mid = b;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.MID);

            String desc = this.mid ? "Put legend entry symbol at midpoint" : "Put legend entry symbols at endpoints";
            FGNRevEdit.post(this, FGNProperty.MID, new Boolean(mid), old, desc);
         }
      }
   }

   /** If this flag is set, the legend is not rendered. */
   private boolean hide;

   /**
    * Get the hide state for this legend.
    * @return True if the legend is currently hidden.
    */
   public boolean getHide() { return(hide); }

   /**
    * Set the hide state for this legend. If a change is made, {@link #onNodeModified()} is invoked.
    * @param hide True to hide legend, false to show it.
    */
   public void setHide(boolean hide)
   {
      if(this.hide != hide)
      {
         if(doMultiNodeEdit(FGNProperty.HIDE, new Boolean(hide))) return;
         
         Boolean old = new Boolean(this.hide);
         this.hide = hide;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.HIDE);
            String desc = this.hide ? "Hide legend" : "Show legend";
            FGNRevEdit.post(this, FGNProperty.HIDE, new Boolean(this.hide), old, desc);
         }
      }
   }

   /** The measured width of the legend's box border (default is 0in). */
   private Measure borderWidth;

   /**
    * Get the width of the box border for this legend. If non-zero, the bounding box of the legend is outlined with
    * a stroke of this width, using the current text/fill color as the stroke color.
    * 
    * @return The border width with associated units of measure (only physical units allowed).
    */
   public Measure getBorderWidth() { return(borderWidth); }

   /**
    * Set the width of the box border for this legend. If a change is made, {@link #onNodeModified()} is invoked.
    * 
    * @param m The new box border width. The measure is constrained to satisfy {@link #STROKEWCONSTRAINTS}. A null 
    * value is rejected.
    * @return True if successful; false if value was rejected.
    */
   public boolean setBorderWidth(Measure m)
   {
      if(m == null) return(false);
      m = STROKEWCONSTRAINTS.constrain(m);

      boolean changed = (borderWidth != m) && !Measure.equal(borderWidth, m);
      if(changed)
      {
         Measure oldBW = borderWidth;
         borderWidth = m;
         if(areNotificationsEnabled())
         {
            if(oldBW.toMilliInches() != borderWidth.toMilliInches())
               onNodeModified(FGNProperty.BORDER);
            FGNRevEdit.post(this, FGNProperty.BORDER, borderWidth, oldBW);
         }
      }
      return(true);
   }

   /** The background color for the legend's bounding box (default is transparent black). */
   private Color boxColor;
   
   /** 
    * Get the background color for the legend's bounding box.
    * @return The background color. Includes alpha component.
    */
   public Color getBoxColor() { return(boxColor); }
   
   /** 
    * Set the background color for the legend's bounding box.
    * @param c The desired color. A null value is rejected. Translucent or transparent colors are permitted.
    * @return False if argument is null; else true.
    */
   public boolean setBoxColor(Color c)
   {
      if(c == null) return(false);
      if(!boxColor.equals(c))
      {
         if(doMultiNodeEdit(FGNProperty.BOXC, c)) return(true);
         
         Color old = boxColor;
         boxColor = c;
         if(areNotificationsEnabled())
         {
            onNodeModified(FGNProperty.BOXC);
            FGNRevEdit.post(this, FGNProperty.BOXC, boxColor, old);
         }
      }
      return(true);
   }
   

   @Override protected void rescaleSelf(double scale, MultiRevEdit undoer)
   {
      Measure.Constraints c = LENCONSTRAINTS;
      double d = length.getValue();
      if(d > 0)
      {
         Measure oldLen = length;
         length = c.constrain(new Measure(d*scale, length.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.LEN, length, oldLen);
      }

      d = spacer.getValue();
      if(d > 0)
      {
         Measure oldSpacer = spacer;
         spacer = c.constrain(new Measure(d*scale, spacer.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.SPACER, spacer, oldSpacer);
      }

      d = labelOffset.getValue();
      if(d > 0)
      {
         Measure oldLO = labelOffset;
         labelOffset = c.constrain(new Measure(d*scale, labelOffset.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.LABELOFFSET, labelOffset, oldLO);
      }

      d = symbolSize.getValue();
      if(d > 0)
      {
         Measure oldSS = symbolSize;
         symbolSize = c.constrain(new Measure(d*scale, symbolSize.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.SIZE, symbolSize, oldSS);
      }
      
      d = borderWidth.getValue();
      if(d > 0)
      {
         Measure oldBW = borderWidth;
         borderWidth = STROKEWCONSTRAINTS.constrain(new Measure(d*scale, borderWidth.getUnits()));
         undoer.addPropertyChange(this, FGNProperty.BORDER, borderWidth, oldBW);
      }
   }

   /**
    * When a legend node is hidden, changes in properties have no effect on its rendering. The override avoids 
    * unnecessary rendering updates in this case, but still notifies the graphic model that it has changed.
    */
   @Override protected void onNodeModified(Object hint)
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
      boolean ok = false;
      switch(p)
      {
         case SIZE : ok = setSymbolSize((Measure) propValue); break;
         case LEN: ok = setLength((Measure) propValue); break;
         case SPACER: ok = setSpacer((Measure) propValue); break;
         case LABELOFFSET: ok = setLabelOffset((Measure) propValue); break;
         case HIDE : setHide(((Boolean)propValue).booleanValue()); ok = true; break;
         case MID: setMid(((Boolean)propValue).booleanValue()); ok = true; break;
         case BORDER: ok = setBorderWidth((Measure) propValue); break;
         case BOXC: ok = setBoxColor((Color) propValue); break;
         default : ok = super.setPropertyValue(p, propValue); break;
      }
      return(ok);
   }

   @Override Object getPropertyValue(FGNProperty p)
   {
      Object value = null;
      switch(p)
      {
         case SIZE : value = getSymbolSize(); break;
         case LEN: value = getLength(); break;
         case SPACER: value = getSpacer(); break;
         case LABELOFFSET: value = getLabelOffset(); break;
         case HIDE : value = new Boolean(getHide()); break;
         case MID: value = new Boolean(getMid()); break;
         case BORDER: value = getBorderWidth(); break;
         case BOXC: value = getBoxColor(); break;
         default : value = super.getPropertyValue(p); break;
      }
      return(value);
   }


   // 
   // Support for style sets
   //
   
   @Override public boolean supportsStyleSet()  { return(true); }

   /** 
    * The node-specific properties exported in a legend node's style set are the legend entry length, override symbol
    * size, entry label offset, vertical spacer between entries, the flag indicating whether a symbols is drawn at
    * the midpoint of an entry line (or two at the endpoints), the border width for the legend's boxed outline, and
    * the background color for the legend box.
    */
   @Override protected void putNodeSpecificStyles(FGNStyleSet styleSet)
   {
      styleSet.putStyle(FGNProperty.LEN, getLength());
      styleSet.putStyle(FGNProperty.SPACER, getSpacer());
      styleSet.putStyle(FGNProperty.SIZE, getSymbolSize());
      styleSet.putStyle(FGNProperty.LABELOFFSET, getLabelOffset());
      styleSet.putStyle(FGNProperty.MID, new Boolean(getMid()));
      styleSet.putStyle(FGNProperty.BORDER, getBorderWidth());
      styleSet.putStyle(FGNProperty.BOXC, getBoxColor());
   }

   @Override protected boolean applyNodeSpecificStyles(FGNStyleSet applied, FGNStyleSet restore)
   {
      boolean changed = false;
      
      Measure m = (Measure) applied.getCheckedStyle(FGNProperty.LEN, getNodeType(), Measure.class);
      if(m != null) m = LENCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.LEN, null, Measure.class)))
      {
         length = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LEN);

      m = (Measure) applied.getCheckedStyle(FGNProperty.SPACER, getNodeType(), Measure.class);
      if(m != null) m = LENCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.SPACER, null, Measure.class)))
      {
         spacer = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.SPACER);
      
      m = (Measure) applied.getCheckedStyle(FGNProperty.SIZE, getNodeType(), Measure.class);
      if(m != null) m = LENCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure) restore.getCheckedStyle(FGNProperty.SIZE, null, Measure.class)))
      {
         symbolSize = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.SIZE);
      
      m = (Measure) applied.getCheckedStyle(FGNProperty.LABELOFFSET, getNodeType(), Measure.class);
      if(m != null) m = LENCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure)restore.getCheckedStyle(FGNProperty.LABELOFFSET,null,Measure.class)))
      {
         labelOffset = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.LABELOFFSET);
      
      Boolean b = (Boolean) applied.getCheckedStyle(FGNProperty.MID, getNodeType(), Boolean.class);
      if(b != null && !b.equals(restore.getCheckedStyle(FGNProperty.MID, null, Boolean.class)))
      {
         mid = b.booleanValue();
         changed = true;
      }
      else restore.removeStyle(FGNProperty.MID);
      
      m = (Measure) applied.getCheckedStyle(FGNProperty.BORDER, getNodeType(), Measure.class);
      if(m != null) m = STROKEWCONSTRAINTS.constrain(m);
      if(m != null && !Measure.equal(m, (Measure)restore.getCheckedStyle(FGNProperty.BORDER,null,Measure.class)))
      {
         borderWidth = m;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.BORDER);

      Color c = (Color) applied.getCheckedStyle(FGNProperty.BOXC, getNodeType(), Color.class);
      if(c != null && !c.equals(restore.getCheckedStyle(FGNProperty.BOXC, null, Color.class)))
      {
         boxColor = c;
         changed = true;
      }
      else restore.removeStyle(FGNProperty.BOXC);
      
      return(changed);
   }


   // 
   // Derived properties
   //

   /**
    * Get the common length of all entries in the legend. Typically, this is set by the legend's {@link #getLength()}
    * property. However, if that is zero, then the entry length is five times the maximum symbol size. If that is also
    * zero, then the entry length defaults to 0.5 inches.
    * 
    * <p><b>NOTE</b> that the entry length excludes the effects of symbols rendered at the mid point or end points of
    * the legend entry line segment (if applicable), as well as the effects of non-zero stroke width.</p>
    * @return The legend entry length in milli-inches, as described.
    */
   double getEntryLengthInMilliInches()
   {
      double lenMI = length.toMilliInches();
      if(lenMI == 0) lenMI = 5.0 * getMaxSymbolSizeInMilliInches();
      if(lenMI == 0) lenMI = 500;
      return(lenMI);
   }
   
   /**
    * Get the list of data presentation nodes in the legend's parent graph.
    * @return The list of data presentation nodes, which will be empty if the parent graph contains no such nodes, or if
    * this legend currently lacks a parent graph container.
    */
   private List<FGNPlottable> getPlottablesInParentGraph()
   {
      FGraphicNode p = getParent();
      List<FGNPlottable> out;
      if(p instanceof FGNGraph) out = ((FGNGraph)p).getPlottableNodes();
      else out = new ArrayList<FGNPlottable>();
      return(out);
   }
   
   /**
    * Get the maximum symbol size used across all legend entries. If the legend node's own <i>symbolSize</i> property is
    * nonzero, that is the maximum symbol size since it will be used for all entries. Otherwise, the method finds the 
    * largest symbol used among the visible legend entries.
    * 
    * <p>The method includes both nominal symbol sizes and rendered stroke widths in the calculations!</p>
    *
    * @return Maximum symbol size used across visible legend entries in milli-inches.
    */
   double getMaxSymbolSizeInMilliInches()
   {
      double maxSymSize = symbolSize.toMilliInches();
      boolean useFixedSize = (maxSymSize != 0);
      if(getParent() == null) return(maxSymSize);
      
      double fixedSymSize = maxSymSize;
      maxSymSize = 0;
      List<FGNPlottable> plottables = getPlottablesInParentGraph();
      for(FGNPlottable pn : plottables)
      {
         List<LegendEntry> entries = pn.getLegendEntries();
         if(entries != null) for(LegendEntry entry : entries)
         {
            if(entry.areSymbolsRendered())
            {
               double d = useFixedSize ? fixedSymSize : entry.getSymbolSizeInMilliInches();
               if(d > 0) d += entry.getSymbolStrokeWidthInMilliInches();
               if(d > maxSymSize) maxSymSize = d;
            }
         }
      }
      if(maxSymSize < 0.0) maxSymSize = 0.0;

      return(maxSymSize);
   }


   /** 
    * A legend node can be aligned with other nodes. The legend's X,Y location is adjusted to achieve the desired 
    * alignment, using the rectangle bounding the rendered legend to calculate the adjustment.
    */
   @Override boolean canAlign() { return(true); }

   // 
   // Focusable/Renderable support
   //

   @Override public AffineTransform getLocalToParentTransform()
   {
      AffineTransform identity = new AffineTransform();
      
      FViewport2D parentVP = getParentViewport();
      if(getParent() == null || parentVP == null) return(identity);

      Point2D loc = parentVP.toMilliInches(getX(), getY());
      if(loc == null) return(identity);

      AffineTransform at = AffineTransform.getTranslateInstance(loc.getX(), loc.getY());
      double rot = getRotate();
      if(rot != 0) at.rotate(Math.toRadians(rot));
      return( at );
   }

   /** The legend node does not define its own viewport, so the parent viewport is always returned. */
   @Override public FViewport2D getViewport() { return(getParentViewport()); }

   /** Cached rectangle bounding only the marks made by this legend node. Null if not yet calculated. */
   private Rectangle2D rBoundsSelf = null;

   @Override protected Rectangle2D getRenderBoundsForSelf(Graphics2D g2d, boolean forceRecalc)
   {
      // recalculate the bounds only when we have to
      if(rBoundsSelf != null && !forceRecalc) return((Rectangle2D)rBoundsSelf.clone());
      
      // (re)initialize to an empty rectangle
      rBoundsSelf = new Rectangle2D.Double();
      
      // if legend is not rendered, return an empty rectangle
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null || !isRendered())
         return((Rectangle2D)rBoundsSelf.clone());

      // get references to currently defined graph trace elements in the graph parent and determine how many have 
      // visible legend entries. Since legend is rendered, there must be at least one visible entry
      List<FGNPlottable> plottables = getPlottablesInParentGraph();
      int nVisible = 0;
      for(FGNPlottable trace : plottables) nVisible += trace.getNumLegendEntries();

      double maxSymSize = getMaxSymbolSizeInMilliInches();
      double spacerMI = spacer.toMilliInches();
      
      // calculate nominal length (excluding symbols) of each legend entry. If legend's "len" attr is nonzero, use that. 
      // Else, use 5x the max symbol size. If no symbols are rendered, use 0.5in as a fallback default.
      double lenMI = length.toMilliInches();
      if(lenMI == 0) lenMI = 5.0 * maxSymSize;
      if(lenMI == 0) lenMI = 500;

      double labelOffsetMI = calcLabelOffsetFromLeftEndPoint();
      
      // now run through the entries again and accumulate the rectangle bounding all entries and their labels. We use
      // a string painter to calculate the bounds of each entry label.
      Point2D pLabel = new Point2D.Double(labelOffsetMI, 0);
      SingleStringPainter labelPainter = new SingleStringPainter();
      labelPainter.setStyle(this);
      labelPainter.updateFontRenderContext(g2d);   // so it can measure text accurately
      labelPainter.setAlignment(TextAlign.LEADING, TextAlign.CENTERED);
      Rectangle2D r = new Rectangle2D.Double();

      double yEntry = spacerMI*(nVisible-1);
      int idxEntry = 0;
      for(FGNPlottable trace : plottables) if(trace.getShowInLegend())
      {
         List<LegendEntry> entries = trace.getLegendEntries();
         if(entries == null) continue;
         
         for(LegendEntry entry : entries)
         {
            // rectangle bounding entry graphics
            entry.getRenderBounds(yEntry, this, r);
            Rectangle2D.union(rBoundsSelf, r, rBoundsSelf);
            
            // rectangle bounding entry label
            String label = entry.getLabel();
            if(label.length() == 0) label = "data " + Integer.toString(idxEntry);
            AttributedString as = fromStyledText(label, getFontFamily(), getFontSizeInPoints(), getFillColor(), 
                  getFontStyle(), true);
            pLabel.setLocation(labelOffsetMI, yEntry);
            labelPainter.setTextAndLocation(as, pLabel);
            
            labelPainter.invalidateBounds();
            labelPainter.getBounds2D(r);
            Rectangle2D.union(rBoundsSelf, r, rBoundsSelf);
            
            // move down to the next legend entry
            yEntry -= spacerMI;
            ++idxEntry;
         }
      }

      // if the legend's bounding box is filled and/or stroked, then we append a margin of 0.3*F on all 4 sides, where
      // F is the legend's font size. If it is stroked, add the half a stroke width on all 4 sides.
      double borderMI = borderWidth.toMilliInches();
      if(boxColor.getAlpha() != 0 || (borderMI > 0 && getFillColor().getAlpha() != 0))
      {
         double m = 0.3 * getFontSizeInPoints() * Measure.PT2IN * 1000.0 + 0.5*borderMI;
         rBoundsSelf.setFrame(rBoundsSelf.getX()-m, rBoundsSelf.getY()-m, 
               rBoundsSelf.getWidth() + 2*m, rBoundsSelf.getHeight() + 2*m);
      }
      return((Rectangle2D)rBoundsSelf.clone());
   }

   /**
    * The legend's {@link #getLabelOffset()} property is the distance separating the entry label from the right edge of
    * the rectangle bounding the entry graphics. However, some entries may extend further to the right than others for
    * a variety of reasons (larger symbol, larger stroke width). In order to layout all legend entry labels so they are
    * right-aligned and do not cover the graphic representation of any entry, this method is called to find the 
    * required label offset.
    * <p>First, it scans all visible entries to find the one that extends furthest to the right. The desired label 
    * offset from the legend's left end point (x==0) is the width of that entry plus the nominal label offset returned
    * by {@link #getLabelOffset()}.</p>
    * 
    * @return Offset from legend's left end point to the start of each entry label.
    */
   private double calcLabelOffsetFromLeftEndPoint()
   {
      // run through the visible entries and find the rightmost edge so we can compute a common offset for all labels
      List<FGNPlottable> plottables = getPlottablesInParentGraph();
      double labelOffsetMI = 0;
      for(FGNPlottable trace : plottables) if(trace.getShowInLegend())
      {
         List<LegendEntry> entries = trace.getLegendEntries();
         if(entries == null) continue;
         
         for(LegendEntry entry : entries)
         {
            double d = entry.getOffsetToRightEdge(this);
            if(d > labelOffsetMI) labelOffsetMI = d;
         }
      }
      labelOffsetMI += getLabelOffset().toMilliInches();
      return(labelOffsetMI);
   }
   
   /**
    * A legend node is not rendered if it is currently hidden, if it lacks a parent graph, or if the graph lacks any 
    * data presentation nodes for which a legend entry is shown.
    */
   @Override protected boolean isRendered()
   {
      if(getParent() == null || hide) return(false);
      for(FGNPlottable trace : getPlottablesInParentGraph()) if(trace.getNumLegendEntries() > 0) return(true);

      return(false);
   }

   public boolean render(Graphics2D g2d, RenderTask task)
   {
      if(!(isRendered() && needsRendering(task)))
         return(true);

      // get the viewport of the parent of this element; if there is none, then the legend cannot be rendered
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return(true);
 
      // get references to any data presentation nodes in the graph parent and count the total number of visible legend
      // entries across all such nodes. Should be at least one, since we got past the isRendered() test!
      List<FGNPlottable> plottables = getPlottablesInParentGraph();
      int nVisible = 0;
      for(FGNPlottable trace : plottables) nVisible += trace.getNumLegendEntries();
      if(nVisible == 0) return(true);

      // get the various properties we need to render the legend, in milli-inches
      double spacerMI = spacer.toMilliInches();
      double maxSymSize = getMaxSymbolSizeInMilliInches();
      double lenMI = length.toMilliInches();
      if(lenMI == 0) lenMI = 5.0 * maxSymSize;
      if(lenMI == 0) lenMI = 500;

      double borderMI = borderWidth.toMilliInches();
      
      // location of legend's bottom left corner; rotation angle
      Point2D botLeft = parentVP.toMilliInches(getX(), getY());
      if(botLeft == null) return(true);
      double rot = getRotate();
      
      // we'll use a StringPainter to render all legend entry labels. This painter uses the LegendNode's own graphics 
      // styles to render the text. Here we also set the label location, which is reused to paint each label, and the 
      // text alignment.  All labels start at a point offset from the rightmost edge of the longest adorned line segment
      // -- so that all labels line up despite possible symbol size differences. All labels are left-aligned and 
      // centered vertically WRT trace line.
      double labelOffsetMI = calcLabelOffsetFromLeftEndPoint();
      Point2D pLabel = new Point2D.Double(labelOffsetMI, 0);
      SingleStringPainter labelPainter = new SingleStringPainter();
      labelPainter.setStyle(this);
      labelPainter.setAlignment(TextAlign.LEADING, TextAlign.CENTERED);

      // render the legend in a copy of the graphics context, so we do not alter the original
      Graphics2D g2dCopy = (Graphics2D) g2d.create();
      try
      {
         // transform graphics context into legend's own local viewport
         g2dCopy.translate(botLeft.getX(), botLeft.getY());
         if(rot != 0) g2dCopy.rotate(Math.toRadians(rot));

         // optionally fill and/or stroke the legend's bounding box
         if(boxColor.getAlpha() != 0 || (borderMI > 0 && getFillColor().getAlpha() != 0))
         {
            if(boxColor.getAlpha() != 0)
            {
               g2dCopy.setColor(boxColor);
               g2dCopy.fill(rBoundsSelf);
            }
            if(borderMI > 0 && getFillColor().getAlpha() != 0)
            {
               g2dCopy.setColor(getFillColor());
               g2dCopy.setStroke(new BasicStroke((float) borderMI));
               g2dCopy.draw(rBoundsSelf);
            }
         }
         
         // render legend entries from top to bottom in the order that traces appear as children of the graph parent
         double yEntry = spacerMI*(nVisible-1);
         int nEntry = 1;
         for(int i=0; i<plottables.size(); i++)
         {
            // skip over each data presentation node that has no associated legend entry(ies)
            List<LegendEntry> entries = plottables.get(i).getLegendEntries();
            if(entries == null) continue;

            for(LegendEntry entry : entries)
            {
               // paint the legend entry without label
               entry.render(g2dCopy, yEntry, this);
                              
               // paint the corresponding label, which may or may not contain differentially styled text.
               pLabel.setLocation(labelOffsetMI, yEntry);
               String label = entry.getLabel();
               if(label.length() == 0) label = "data " + nEntry;
               AttributedString as = fromStyledText(label, getFontFamily(), getFontSizeInPoints(), getFillColor(), 
                     getFontStyle(), true);
               labelPainter.setTextAndLocation(as, pLabel);

               // must handle font substitution when rendering in PDF graphics context...
               labelPainter.render(g2dCopy, null);

               // move down to the next legend entry
               yEntry -= spacerMI;
               ++nEntry;
            }
         }

      }
      finally { if(g2dCopy != null) g2dCopy.dispose(); }

      return((task == null) ? true : task.updateProgress());
   }

   @Override protected void releaseRenderResourcesForSelf() { rBoundsSelf = null; }


   //
   // PSTransformable implementation
   //

   public void toPostscript(PSDoc psDoc) throws UnsupportedOperationException
   {
      if(!isRendered()) return;
      FViewport2D parentVP = getParentViewport();
      if(parentVP == null) return;
 
      // count the number of visible legend entries among the data presentation nodes in the parent graph. There should
      // be at least one, since we got past the isRendered() test!
      List<FGNPlottable> plottables = getPlottablesInParentGraph();
      int nVisible = 0;
      for(FGNPlottable trace : plottables) nVisible += trace.getNumLegendEntries();
      if(nVisible == 0) return;

      // get the various properties we need to render the legend, in milli-inches
      double spacerMI = spacer.toMilliInches();
      double maxSymSize = getMaxSymbolSizeInMilliInches();
      double lenMI = length.toMilliInches();
      if(lenMI == 0) lenMI = 5.0 * maxSymSize;
      if(lenMI == 0) lenMI = 500;

      double borderMI = borderWidth.toMilliInches();

      // location of legend's bottom left corner; rotation angle
      Point2D botLeft = parentVP.toMilliInches(getX(), getY());
      if(botLeft == null) return;
      double rot = getRotate();

      // all labels start at a point offset from the rightmost edge of the longest adorned line segment -- so 
      // that all labels line up despite possible symbol size differences. All labels will be left-aligned and centered
      // vertically WRT trace line.
      double labelOffsetMI = calcLabelOffsetFromLeftEndPoint();

      // start the legend's rendering, and translate and rotate user space IAW legend attribute
      psDoc.startElement(this);
      psDoc.translateAndRotate(botLeft, rot);

      // optionally fill and/or stroke the legend's bounding box
      if(boxColor.getAlpha() != 0 || (borderMI > 0 && getFillColor().getAlpha() != 0))
      {
         // NOTE: When the legend box is rendered, we use the calculated render bounds to define it. Rather than calc
         // the bounds in the PS document (we would have to measure each text label in the entry, we accept the Java2D
         // render bounds. But the Java2D render bounds calc requires a Graphics2D (to get a font render context) and we
         // may be generating the PS document without a Graphics2D. So, to be sure, we create one from BufferedImage and
         // force a recalc of the render bounds here:
         Image img = FCIcons.V4_BROKEN.getImage();
         BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
         Graphics2D g2BI = bi.createGraphics();
         try { getRenderBoundsForSelf(g2BI, true); } finally { g2BI.dispose(); }
         
         Color strkC = (getFillColor().getAlpha()==0) ? null : getFillColor();
         Color fillC = (boxColor.getAlpha()==0) ? null : boxColor;
         psDoc.renderRect(rBoundsSelf, borderMI, strkC, fillC);
      }

      // render a legend entry for each titled data set element in the parent graph.  Note that everything here is 
      // rendered WRT the user space established by the last translateAndRotate().
      double yEntry = spacerMI*(nVisible - 1);
      int nEntry = 0;
      for(int i=0; i<plottables.size(); i++)
      {
         // skip over each data element that has no visible legend entries
         List<LegendEntry> entries = plottables.get(i).getLegendEntries();
         if(entries == null) continue;

         for(LegendEntry entry : entries)
         {
            // draw the legend entry itself
            entry.toPostscript(psDoc, yEntry, this);

            // draw the label for the legend entry. Render no text if the legend's text/fill color is transparent!
            if(getFillColor().getAlpha() > 0)
            {
               String label = entry.getLabel();
               if(label.length() == 0) label = "data " + nEntry;
               psDoc.renderText(label, labelOffsetMI, yEntry, TextAlign.LEADING, TextAlign.CENTERED);
            }
            
            // move down to the next legend entry
            yEntry -= spacerMI;
            ++nEntry;
         }
      }

      // end the element
      psDoc.endElement();
   }

   
   // 
   // Object
   //

   /** Override ensures that the rendering infrastructure for the clone is independent of the cloned legend node. */
   @Override protected Object clone()
   {
      LegendNode copy = (LegendNode) super.clone();
      copy.rBoundsSelf = null;
      return(copy);
   }
}
