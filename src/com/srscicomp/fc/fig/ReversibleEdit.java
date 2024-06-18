package com.srscicomp.fc.fig;

/**
 * Interface representing a reversible change to {@link FGraphicModel} that can be undone once applied, or redone once
 * undone.
 * 
 * <p>Any practical implementation must carry all state information and objects needed to restore the graphic model's 
 * contents to its state immediately befor or immediately after the change. <code>FGraphicModel</code> itself maintains 
 * an "action" history of up to the fifty (50) most recent reversible changes made to the model. The most recent change
 * applied to the model can be undone, and the most recent undone operation can be redone. For more information, see
 * {@link FGraphicModel#undoOrRedo}.</p>
 * 
 * @author  sruffner
 */
interface ReversibleEdit
{
   /** 
    * Return a brief description of this reversible edit. It will appear as the tool tip for an "Undo" or "Redo" action,
    * prepended with the token "Undo:" or "Redo:", respectively.
    * 
    * @return Brief description of the reversible edit.
    */
   String getDescription();
   
   /**
    * Undo or redo the change represented by this reversible edit.
    * @param redo If true, redo the change represented by this edit, else undo it.
    * @return True if the reversible edit is successfully undone or redone; false otherwise.
    */
   boolean undoOrRedo(boolean redo);
   
   /**
    * Release this reversible edit object.
    * <p>This is called prior to discarding the object. It may be useful to release any stored references to help hasten
    * garbage collection of the object.</p>
    */
   void release();
}
