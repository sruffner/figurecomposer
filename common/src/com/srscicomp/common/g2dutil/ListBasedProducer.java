package com.srscicomp.common.g2dutil;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A simple producer that is based on initial source list of objects of type T. Since the source list could be modified
 * outside the context of the producer, the public constructor makes an internal copy of the list. The list is
 * parameterized by object type T so that it can be used for lists of various object classes. 
 * 
 * <p>It is intended as a "wrapper" of sorts to construct location and string producers for the various implementations
 * of {@link Painter} in the <i>com.srscicomp.common.g2dutil</i> package.</p>
 * 
 * @author sruffner
 */
public final class ListBasedProducer<T> implements Iterable<T>, Iterator<T>
{
   /**
    * Construct a producer of objects of type T in the specified ordered list. An internal copy of the list is made to 
    * ensure the producer is not affected by changes in that list after construction.
    * @param src The source list.
    */
   public ListBasedProducer(List<T> src)
   {
      if(src != null && !src.isEmpty())
      {
         arSrc = src.toArray();
      }
   }
   
   @Override public boolean hasNext() { return(nSoFar < arSrc.length); }
   
   @SuppressWarnings("unchecked") @Override public T next()
   {
      if(!hasNext()) throw new NoSuchElementException("Out of elements.");
      return((T) arSrc[nSoFar++]);
   }
   
   public void remove() { throw new UnsupportedOperationException("Removal not supported by this iterator."); }

   @Override public Iterator<T> iterator() { return(new ListBasedProducer<>(arSrc)); }

   private ListBasedProducer(Object[] src) {  arSrc = Arrays.copyOf(src, src.length); }
   private Object[] arSrc = new Object[0];
   private int nSoFar = 0;
}
