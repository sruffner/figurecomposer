package com.srscicomp.common.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.util.Arrays;

/**
 * A simple component focus traversal policy that cycles among a specified list of components. It will only return
 * components that are visible, focusable, enabled, and descendants of the relevant container. It does NOT check for 
 * focus traversal policy providers nor focus cycle roots. Use with care.
 * 
 * @author sruffner
 */
public class SimpleComponentListTraversalPolicy extends FocusTraversalPolicy
{
   /**
    * Construct a simple focus traversal policy that is based on the component array specified.
    * @param cmpts The array of components included in the focus traversal.
    */
   public SimpleComponentListTraversalPolicy(Component[] cmpts)
   {
      if(cmpts == null) theComponents = new Component[0];
      else theComponents = Arrays.copyOf(cmpts, cmpts.length);
   }
   
   /** 
    * Returns the first component after the specified component in the traversal policy's internal component list that:
    * (i) is a descendant of the specified container; (ii) is visible; and (iii) is focusable. Returns null if there is
    * no such component. Upon reaching the end of the list, the method wraps back to the beginning as it searches for
    * an acceptable component.
    */
   @Override public Component getComponentAfter(Container con, Component c)
   {
      if(con == null || c == null || !con.isAncestorOf(c) || theComponents.length < 2) return(null);
      
      int curr = getIndexOf(c);
      if(curr < 0) return(null);
      
      int idx = (curr + 1) % theComponents.length;
      while(idx != curr && !accept(con, theComponents[idx]))
         idx = (idx + 1) % theComponents.length;
      
      return(idx != curr ? theComponents[idx] : null);
   }

   /** 
    * Returns the first component before the specified component in the traversal policy's internal component list that:
    * (i) is a descendant of the specified container; (ii) is visible; and (iii) is focusable. Returns null if there is
    * no such component. Upon reaching the start of the list, the method wraps around to the end as it searches for an
    * acceptable component.
    */
   @Override public Component getComponentBefore(Container con, Component c)
   {
      if(con == null || c == null || !con.isAncestorOf(c) || theComponents.length < 2) return(null);
      
      int curr = getIndexOf(c);
      if(curr < 0) return(null);
      
      int idx = (curr == 0) ? theComponents.length - 1 : curr - 1;
      while(idx != curr && !accept(con, theComponents[idx]))
         idx = (idx == 0) ? theComponents.length - 1 : idx - 1;
      
      return(idx != curr ? theComponents[idx] : null);
   }

   /**
    * Returns first component in traversal policy's internal component list that: (i) is a descendant of the specified
    * container; (ii) is visible; and (iii) is focusable. Returns null if there is no such component.
    */
   @Override public Component getFirstComponent(Container con)
   {
      Component res = null;
      if(con != null && theComponents.length > 0)
      {
         for(Component c : theComponents) if(accept(con, c))
         { 
            res = c;
            break;
         }
      }
      return(res);
   }

   /**
    * Returns last component in traversal policy's internal component list that: (i) is a descendant of the specified
    * container; (ii) is visible; and (iii) is focusable. Returns null if there is no such component.
    */
   @Override public Component getLastComponent(Container con)
   {
      Component res = null;
      if(con != null && theComponents.length > 0)
      {
         for(int i=theComponents.length-1; i>=0; i--) if(accept(con, theComponents[i]))
         { 
            res = theComponents[i];
            break;
         }
      }
      return(res);
   }

   /** The first component in the component list is the default component. */
   @Override public Component getDefaultComponent(Container con) { return(getFirstComponent(con)); }

   /** 
    * Get index of specified component in the component list on which this traversal policy is based.
    * @param c A component.
    * @return Index of the component in the component traversal list; -1 if not found.
    */
   private int getIndexOf(Component c)
   {
      for(int i=0; i<theComponents.length; i++) if(c == theComponents[i]) return(i);
      return(-1);
   }
   
   /**
    * Accepts the specified component if it is a descendant of the specified container and is visible, focusable, and
    * enabled.
    * @param con A container.
    * @param c A component.
    * @return True if component is accepted, as described.
    */
   private boolean accept(Container con, Component c)
   {
      return(con != null && c != null && con.isAncestorOf(c) && c.isVisible() && c.isFocusable() && c.isEnabled());
   }
   
   private final Component[] theComponents;
}
