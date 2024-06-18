package com.srscicomp.fc.fig;

import java.util.ArrayList;
import java.util.List;

/** 
 * This implementation of a reversible change is meant to undo the effects of any operation that could change many 
 * properties over multiple graphic nodes. Examples of such operations include a global rescaling of a figure or graph,
 * restoring default styles on a node and its descendants, moving the currently selected nodes in a figure as one
 * operation, etcetera.
 * 
 * <p>For each property changed, the method {@link #addPropertyChange} is invoked to save a reference to the node
 * changed, the identity of the property changed, and its values before and after the change. Undoing or redoing
 * these changes is simply a matter of setting each property to the old or new value, respectively.</p>
 * 
 * @author  sruffner
 */
public class MultiRevEdit implements ReversibleEdit
{
   /** 
    * Construct an initially empty instance of a reversible edit encapsulating one or more property changes.
    * @param owner The figure model containing all of the nodes that will be changed.
    * @param desc A brief description. If null, a default description is supplied.
    */
   MultiRevEdit(FGraphicModel owner, String desc) 
   { 
      if(owner == null) throw new NullPointerException("Must specify affected figure model");
      this.owner = owner;
      description = (desc == null) ? "Multiple property changes across multiple elements" : desc; 
   }

   /**
    * Record a property change to be encapsulated by this multiple-change reversible edit. 
    * @param n The graphic node changed. Cannot be null.
    * @param prop The identity of the property changed. Cannot be null.
    * @param value The property value after the change. Cannot be null, unless the property is one of the inherited
    * graphic styles.
    * @param oldValue The property value prior to the change. Cannot be null, unless the property is one of the
    * inherited graphic styles.
    */
   void addPropertyChange(FGraphicNode n, FGNProperty prop, Object value, Object oldValue)
   {
      if(nodes == null) return;
      if(n == null || prop == null) throw new IllegalArgumentException("Node and property ID must be specified");
      if(n.getGraphicModel() != owner) throw new IllegalArgumentException("Node is not part of figure");
      if((value == null || oldValue == null) && !FGraphicNode.isStyleProperty(prop))
         throw new IllegalArgumentException("Null property value not allowed unless an inherited style");

      nodes.add(n);
      properties.add(prop);
      values.add(value);
      oldValues.add(oldValue);
   }

   /** 
    * Get the number of property changes encapsulated in this multiple-change reversible edit.
    * @return The total number of property changes that have been added.
    */
   int getNumberOfChanges() { return(nodes == null ? 0 : nodes.size()); }


   @Override public boolean undoOrRedo(boolean redo)
   {
      if(nodes == null) return(false);
      
      boolean ok = true;
      for(int i=0; ok && i<nodes.size(); i++)
      {
         Object val = redo ? values.get(i) : oldValues.get(i);
         ok = nodes.get(i).undoOrRedoPropertyChange(properties.get(i), val, true);
         
         /* The notify/update flag is now set in the undoOrRedoPropertyChange call, so this is not necessary. While
         multiple render jobs get posted, they are consolidated into one job...
         if(ok)
         {
            FGraphicModel fgm = nodes.get(i).getGraphicModel();
            if(fgm != null) fgm.onChange(nodes.get(i), 0, false, null);
         }
         */
      }

      owner.getRoot().propagateFontChange();
      owner.getRoot().onNodeModified(null);
      
      return(ok);
   }

   @Override public void release()
   {
      if(nodes != null)
      {
         nodes.clear();
         properties.clear();
         values.clear();
         oldValues.clear();
         nodes = null;
         properties = null;
         values = null;
         oldValues = null;
         description = null;
      }
   }
   public String getDescription() { return((description == null) ? "" : description); }

   /** 
    * The model containing all nodes affected by this multiple-step edit. After all changes are undone or redone, 
    * any font changes are propagated throughout the model and the model is re-rendered entirely.
    */
   private final FGraphicModel owner;
   
   /** The graphic nodes that changed. */
   private List<FGraphicNode> nodes = new ArrayList<>();

   /** The properties changed; will be the same size as the list of graphic nodes. */
   private List<FGNProperty> properties = new ArrayList<>();
   
   /** The property values post-change; will be the same size as the list of graphic nodes. */
   private List<Object> values = new ArrayList<>();
   
   /** The property values pre-change; will be the same size as the list of graphic nodes. */
   private List<Object> oldValues = new ArrayList<>();
   
   /** A short description of the reversible change. */
   private String description;
}
