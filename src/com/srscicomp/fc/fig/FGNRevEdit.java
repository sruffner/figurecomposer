package com.srscicomp.fc.fig;

import java.awt.Color;
import java.util.HashMap;



/**
 * <code>FGNRevEdit</code> is an implementation of <code>ReversibleEdit</code> that encapsulates a reversible change 
 * in any single node in <em>DataNav</em>'s graphic model. Most such changes are atomic property changes, but the class 
 * includes support for a number of special cases. It handles all changes across all graphic node classes (rather than 
 * creating separate implementations of <code>ReversibleEdit</code> for each type of graphic object).
 * 
 * @author sruffner
 */
class FGNRevEdit implements ReversibleEdit
{
   /**
    * Construct a reversible edit encapsulating a change to a single property of a graphic node, then post it to the 
    * "edit history" of the node's figure graphic model.
    * 
    * <p>This method constructs a short description of the change that takes the form <i>Change {property name} from
    * {old value} to {updated value} on {node name}"</i>. If you want to provide a different descriptor, use 
    * {@link #post(FGraphicNode, FGNProperty, Object, Object, String)}.</p>
    * 
    * @param n The graphic node for which a property changed. If this is null or is not currently attached to a graphic
    * model, then no action is taken.
    * @param p Identifies the property that changed. If null, no action is taken.
    * @param value The updated value of the property after the change change. Null is a valid value, but only if the
    * specified property is one of the inheritable style properties.
    * @param oldValue The old value of the property prior to the change. Null is a valid value, but only if the
    * specified property is one of the inheritable style properties.
    */
   static void post(FGraphicNode n, FGNProperty p, Object value, Object oldValue)
   {
      if(n == null || p == null || ((value==null || oldValue==null) && !FGraphicNode.isStyleProperty(p))) return;
      
      FGraphicModel model = n.getGraphicModel();
      if(model == null) return;

      // style properties are inherited when null -- get inherited value if we can
      if(value == null) value = n.getInheritedValueFor(p);
      else if(oldValue == null) oldValue = n.getInheritedValueFor(p);
      
      // construct a description of the reversible property change
      StringBuilder sb = new StringBuilder();
      sb.append("Change ").append(p.getNiceName()).append(" from " );
      
      if(oldValue == null) sb.append("(default)");
      else if(oldValue instanceof Color)
      {
         Color c = (Color) oldValue;
         sb.append("(").append(c.getRed()).append(",").append(c.getGreen()).append(",").append(c.getBlue()).append(")");
         if(c.getAlpha() < 255) sb.append(" [alpha=").append(c.getAlpha()).append("]");
      }
      else if(p == FGNProperty.FONTSIZE) sb.append( ((Integer)oldValue).toString() + " pt" );
      else sb.append(oldValue.toString());
      
      sb.append(" to ");
      
      if(value == null) sb.append("(default)");
      else if(value instanceof Color)
      {
         Color c = (Color) value;
         sb.append("(").append(c.getRed()).append(",").append(c.getGreen()).append(",").append(c.getBlue()).append(")");
         if(c.getAlpha() < 255) sb.append(" [alpha=").append(c.getAlpha()).append("]");
      }
      else if(p == FGNProperty.FONTSIZE) sb.append( ((Integer)value).toString() + " pt" );
      else sb.append(value.toString());
      
      sb.append(" on ").append(n.getNodeType().getNiceName().toLowerCase());

      post(n, p, value, oldValue, sb.toString());
   }

   /**
    * Construct a reversible edit encapsulating a change to a single property of a graphic node, then post it to the 
    * "edit history" of the node's figure graphic model. 
    * 
    * @param n The graphic node for which a property changed. If this is null or is not currently attached to a graphic
    * model, then no action is taken.
    * @param p Identifies the property that changed. If null, no action is taken.
    * @param value The updated value of the property after the change change. Null is a valid value, but only if the
    * specified property is one of the inheritable style properties.
    * @param oldValue The old value of the property prior to the change. Null is a valid value, but only if the
    * specified property is one of the inheritable style properties.
    * @param desc A short description of the reversible change. If null or empty, a generic description is used.
    */
   static void post(FGraphicNode n, FGNProperty p, Object value, Object oldValue, String desc)
   {
      if(n == null || p == null || ((value==null || oldValue==null) && !FGraphicNode.isStyleProperty(p))) return;
      
      FGraphicModel model = n.getGraphicModel();
      if(model == null) return;

      if(desc == null || desc.trim().length() == 0)
         desc = "Change property '" + p.getNiceName() + "' on " + n.getNodeType().getNiceName().toLowerCase();
      model.postReversibleEdit(new FGNRevEdit(n, p, value, oldValue, desc));
   }

   /**
    * Construct a reversible edit changing the location a graphic node, then post it to the "edit history" of the node's
    * figure graphic model. Do NOT use this method to move an instance of {@link LineNode}; use {@link #postMoveLine()}
    * instead.
    * 
    * @param n The graphic node that was moved. If this is null or is not currently attached to a graphic model, then no
    * action is taken. The node's (x,y) coordinates must reflect its location <i>after</i> the move.
    * @param xOld The node's x-coordinate prior to the move. If null, no action is taken.
    * @param yOld The node's y-coordinate prior to the move. If null, no action is taken.
    */
   static void postMove(FGraphicNode n, Measure xOld, Measure yOld)
   {
      if(n == null || n instanceof LineNode || xOld == null || yOld == null) return;
      
      FGraphicModel model = n.getGraphicModel();
      if(model == null) return;
      
      StringBuilder sb = new StringBuilder();
      sb.append("Move ").append(n.getNodeType().getNiceName().toLowerCase()).append(" from (");
      sb.append(xOld.toString(5,2)).append(",").append(yOld.toString(5,2)).append(") to (");
      sb.append(n.getX().toString(5,2)).append(",").append(n.getY().toString(5,2)).append(")");
    
      FGNRevEdit re = new FGNRevEdit(MOVE_NODE, n, new FGNProperty[] {FGNProperty.X, FGNProperty.Y}, 
            new Object[] {n.getX(), n.getY()}, new Object[] {xOld, yOld}, sb.toString());
      model.postReversibleEdit(re);
   }

   /**
    * Construct a reversible edit changing the location a line segment node, then post it to the "edit history" of the 
    * node's figure graphic model.
    * 
    * @param line The line node that was moved. If this is null or is not currently attached to a graphic model, then no
    * action is taken. 
    * @param xOld X-coordinate of the line's first endpoint prior to the move. If null, no action is taken.
    * @param yOld Y-coordinate of the line's first endpoint prior to the move. If null, no action is taken.
    * @param x2Old X-coordinate of the line's second endpoint prior to the move. If null, no action is taken.
    * @param y2Old Y-coordinate of the line's second endpoint prior to the move. If null, no action is taken.
    */
   static void postMoveLine(LineNode line, Measure xOld, Measure yOld, Measure x2Old, Measure y2Old)
   {
      if(line == null || xOld == null || yOld == null || x2Old == null || y2Old == null) return;
      
      FGraphicModel model = line.getGraphicModel();
      if(model == null) return;
      
      FGNRevEdit re = new FGNRevEdit(MOVE_LINENODE, line, 
            new FGNProperty[] {FGNProperty.X, FGNProperty.Y, FGNProperty.X2, FGNProperty.Y2}, 
            new Object[] {line.getX(), line.getY(), line.getX2(), line.getY2()}, 
            new Object[] {xOld, yOld, x2Old, y2Old}, "Relocate line segment");
      model.postReversibleEdit(re);
   }
   
   /**
    * Construct a reversible edit which resizes a graphic node with a rectangular bounding box, then post it to the 
    * "edit history" of the node's figure graphic model. The method is appropriate only to resizing a 2D graph, text 
    * box, image, or shape node. When resizing a line segment node, use {@link #postLineResize()}.

    * @param n The graphic node that was resized. Must be a graph, text box, image, or shape node.
    * @param xOld X-coordinate of node's bounding box prior to resize. Null if unchanged by the resize operation.
    * @param yOld Y-coordinate of node's bounding box prior to resize. Null if unchanged by the resize operation.
    * @param wOld Width of node's bounding box prior to resize. Null if unchanged by the resize operation.
    * @param hOld Height of node's bounding box prior to resize. Null if unchanged by the resize operation.
    */
   static void postBoxResize(FGraphicNode n, Measure xOld, Measure yOld, Measure wOld, Measure hOld)
   {
      if(n == null) return;
      FGNodeType nt = n.getNodeType();
      if(nt!=FGNodeType.GRAPH && nt!=FGNodeType.PGRAPH && nt!=FGNodeType.TEXTBOX && nt!=FGNodeType.IMAGE && 
            nt!=FGNodeType.SHAPE) 
         return;
      FGraphicModel model = n.getGraphicModel();
      if(model == null) return;
      
      int i = 0;
      if(xOld != null) ++i;
      if(yOld != null) ++i;
      if(wOld != null) ++i;
      if(hOld != null) ++i;
      if(i == 0) return;
      
      FGNProperty[] props = new FGNProperty[i];
      Object[] values = new Object[i];
      Object[] oldValues = new Object[i];
      
      i = 0;
      if(xOld != null)
      {
         props[i] = FGNProperty.X;
         values[i] = n.getX();
         oldValues[i] = xOld;
         ++i;
      }
      if(yOld != null)
      {
         props[i] = FGNProperty.Y;
         values[i] = n.getY();
         oldValues[i] = yOld;
         ++i;
      }
      if(wOld != null)
      {
         props[i] = FGNProperty.WIDTH;
         values[i] = n.getWidth();
         oldValues[i] = wOld;
         ++i;
      }
      if(hOld != null)
      {
         props[i] = FGNProperty.HEIGHT;
         values[i] = n.getHeight();
         oldValues[i] = hOld;
      }
      
      String desc = "Resize " + nt.getNiceName().toLowerCase();
      
      model.postReversibleEdit(new FGNRevEdit(RESIZE_NODE, n, props, values, oldValues, desc));
   }
   
   /**
    * Construct a reversible edit which "resizes" a line segment node by relocating the first or second endpoint, then 
    * post it to the "edit history" of the node's figure graphic model.

    * @param line The affected line segment node. If null, no action is taken.
    * @param isP1 True if first endpoint was relocated, false if second endpoint.
    * @param xOld X-coordinate of specified endpoint prior to the change. If null, no action is taken.
    * @param yOld Y-coordinate of specified endpoint prior to the change. If null, no action is taken.
    */
   static void postLineResize(LineNode line, boolean isP1, Measure xOld, Measure yOld)
   {
      if(line == null || xOld == null || yOld == null) return;
      FGraphicModel model = line.getGraphicModel();
      if(model == null) return;
      
      FGNProperty[] props;
      Object[] values;
      Object[] oldValues;
      StringBuilder sb = new StringBuilder();
      sb.append("Move line's ").append(isP1 ? "first " : "second ").append("endpoint from (");
      sb.append(xOld.toString(5,2)).append(",").append(yOld.toString(5,2)).append(") to (");
      
      if(isP1)
      {
         props = new FGNProperty[] { FGNProperty.X, FGNProperty.Y };
         values = new Object[] { line.getX(), line.getY() };
         oldValues = new Object[] { xOld, yOld };
         sb.append(line.getX().toString(5,2)).append(",").append(line.getY().toString(5,2)).append(")");
      }
      else
      {
         props = new FGNProperty[] { FGNProperty.X2, FGNProperty.Y2 };
         values = new Object[] { line.getX2(), line.getY2() };
         oldValues = new Object[] { xOld, yOld };
         sb.append(line.getX2().toString(5,2)).append(",").append(line.getY2().toString(5,2)).append(")");
      }
      
      model.postReversibleEdit(new FGNRevEdit(RESIZE_NODE, line, props, values, oldValues, sb.toString()));
   }
   
   /**
    * Construct a reversible edit encapulating a change in a function node's formula string (which is not treated like a
    * standard node property), then post it to the "edit history" of the node's figure graphic model.
    * 
    * @param func The function node. If null, no action is taken.
    * @param oldFormula The function's formula string prior to the change.
    */
   static void postFunctionFormulaChange(FunctionNode func, String formula)
   {
      if(func == null) return;
      FGraphicModel model = func.getGraphicModel();
      if(model == null) return;

      StringBuilder sb = new StringBuilder();
      sb.append("Change function formula to [");
      sb.append((formula==null) ? "" : (formula.length() > 20 ? (formula.substring(0,20) + "...") : formula));
      sb.append("]");
      
      FGNRevEdit re = new FGNRevEdit(FUNC_CHANGE, func, null, 
            new Object[] {func.getFunctionString()}, new Object[] {formula}, sb.toString());
      model.postReversibleEdit(re);
   }
   
   /**
    * Construct a reversible edit representing a change in the state of the flag that determines whether or not a tick
    * set node tracks the range of its parent axis (this flag is not a standard property of a tick set node). Post the 
    * reversible edit object to the "edit history" of the node's figure graphic model.
    * 
    * <p>Included with the old and updated values of the state flag are the old and updated values for the nominal start
    * and end of the tick set's range. These may be changed when axis tracking is turned on, so we need to be able to 
    * restore the previous values when the operation is "undone".</p>
    * 
    * 
    * @param tsn The affected tick set node. If null, no action is taken.
    * @param old True if tick set was tracking parent axis prior to toggling the flag.
    * @param oldStart Nominal start of tick set range prior to toggling the flag.
    * @param oldEnd Nominal end of tick set range priot to toggling the flag.
    */
   static void postTrackAxisFlagToggle(TickSetNode tsn, boolean old, double oldStart, double oldEnd)
   {
      if(tsn == null) return;
      FGraphicModel model = tsn.getGraphicModel();
      if(model == null) return;
      
      String desc = "Tick set " + (old ? "does not track" : "tracks") + " parent axis range";
      FGNRevEdit re = new FGNRevEdit(TRACKAXIS_CHANGE, tsn, null, 
            new Object[] {old ? Boolean.FALSE : Boolean.TRUE, new Double(tsn.getStart()), new Double(tsn.getEnd())}, 
            new Object[] {old ? Boolean.TRUE : Boolean.FALSE, new Double(oldStart), new Double(oldEnd)}, 
            desc);
      model.postReversibleEdit(re);
   }

   private FGNRevEdit(FGraphicNode n, FGNProperty p, Object updated, Object old, String desc)
   {
      changeType = PROPERTY_CHANGE;
      node = n;
      affected = new FGNProperty[] {p};
      updatedValues = new Object[] {updated};
      oldValues = new Object[] {old};
      description = desc; 
   }

   private FGNRevEdit(int type, FGraphicNode n, FGNProperty[] props, Object[] updated, Object[] old, String desc)
   {
      changeType = type;
      node = n;
      affected = props;
      updatedValues = updated;
      oldValues = old;
      description = desc;
   }

   /** Identifies a change in a single property of a graphic node. */
   private static final int PROPERTY_CHANGE = 0;
   
   /** Identifies a relocation of a graphic node (changing its x- and y-coordinates simultaneously). */
   private static final int MOVE_NODE = 1;
   
   /** Identifies a relocation of a line segment, which requires changing both endpoints. */
   private static final int MOVE_LINENODE = 2;
   
   /** Identifies a change in the formula string for a function element. */
   private static final int FUNC_CHANGE = 3;

   /** Identifies a change in the "tracking parent axis" flag for a tick set. This is not a named property. */
   private static final int TRACKAXIS_CHANGE = 4;

   /** 
    * Identifies a resize operation on a graphic node, which can affect one or more properties depending on the
    * node and the kind of resizing that occurred.
    */
   private static final int RESIZE_NODE = 5;
   
   /** The type of reversible change. */
   private int changeType = -1;

   /** The graphic node that was changed. */
   private FGraphicNode node = null;
   
   /** The node properties affected by the reversible change. */
   private FGNProperty[] affected = null;
   
   /** The corresponding values applied to the affected properties to effect the change. */
   private Object[] updatedValues = null;
   
   /** The corresponding values applied to the affected properties to undo the change. */
   private Object[] oldValues = null;
   
   /** A brief description of the change. */
   private String description;
   
   @Override public String getDescription() { return(description == null ? "" : description); }

   @Override public boolean undoOrRedo(boolean redo)
   {
      if(node == null) return(false);
      
      Object[] values = redo ? updatedValues : oldValues;
      boolean ok = false;
      try
      {
         if(changeType == PROPERTY_CHANGE) 
            ok = node.undoOrRedoPropertyChange(affected[0], values[0], true);
         else if(changeType == MOVE_NODE)
            ok = node.setXY((Measure) values[0], (Measure) values[1]);
         else if(changeType == MOVE_LINENODE)
         {
            ((LineNode)node).undoOrRedoMove(
                  (Measure) values[0], (Measure) values[1], (Measure) values[2], (Measure) values[3]);
            ok = true;
         }
         else if(changeType == FUNC_CHANGE)
         {
            ((FunctionNode)node).setFunctionString((String) values[0]);
            ok = true;
         }
         else if(changeType == TRACKAXIS_CHANGE)
         {
            ((TickSetNode)node).undoTrackingAxisChange((Boolean) values[0], (Double) values[1], (Double) values[2]);
            ok = true;
         }
         else if(changeType == RESIZE_NODE)
         {
            HashMap<FGNProperty, Measure> resizedProperties = new HashMap<FGNProperty, Measure>();
            for(int i=0; i<affected.length; i++) resizedProperties.put(affected[i], (Measure) values[i]);
            
            ok = node.undoOrRedoResize(resizedProperties);
         }
      }
      catch(Throwable t) 
      {
         System.out.println("Got exception in FGNRevEdit:undo() -- " + t.getMessage());
         t.printStackTrace();
         ok = false;
      }

      return(ok);
   }
   
   @Override public void release()
   {
      node = null;
      affected = null;
      updatedValues = null;
      oldValues = null;
      description = null;
   }
}
