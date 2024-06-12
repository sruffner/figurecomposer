package com.srscicomp.fc.fig;

/**
 * The listener interface for receiving mutation events from the <i>DataNav</i> figure model, {@link FGraphicModel}. The
 * model informs all registered listeners whenever the model is reloaded (as when read in from file), when a single 
 * child node is inserted or removed from the model, when a node's definition is modified, when the state of the model's
 * current selection changes (the model supports multiple-node selection, but typically only one node is selected at a
 * time), and in certain other situations. 
 * 
 * <p><i><b>NOTE</b></i>. This simple listener interface is intended only to inform registered listeners that the 
 * graphic model has changed in some way. It is intended for use by <i>DataNav</i>-specific GUI elements that need to 
 * update themselves whenever the model changes. These elements will query the model for whatever information they need 
 * to respond appropriately to the change.</p>
 * 
 * @author 	sruffner
 */
public interface FGModelListener
{
   /**
    * Invoked whenever the figure model is modified in some way.
    * 
    * @param model The figure model that sourced this event.
    * @param n A graphic node in the model. For node insertions or removals, this will be the parent node affected. For 
    * a node definition change, this is the node modified. For all other model changes, this is ignored.
    * @param change The type of change that occurred.
    */
   public void modelChanged(FGraphicModel model, FGraphicNode n, FGraphicModel.Change change);
}
