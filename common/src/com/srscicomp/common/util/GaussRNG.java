package com.srscicomp.common.util;

/**
 * A pseudo-random number generator that returns a sequence of normally distributed floating-point values with zero mean 
 * and unit variance. It encapsulates the "gasdev" algorithm presented on p.289 in: 
 *
 * <p><i>Press, WH; et al.  "Numerical recipes in C: the art of acientific computing".  New York:  Cambridge University 
 * Press, Copyright 1988-1992.</i></p>
 *
 * <p>The algorithm uses the polar form of the Box-Muller transformation to transform a sequence of uniform deviates to 
 * a sequence of Gaussian deviates. We use <code>UniformRNG</code> as the source of the uniform sequence. For 
 * algorithmic details, consult the book.</p>
 *
 * <p>NOTE: IAW the licensing policy of "Numerical Recipes in C", this class is not distributable in source code form 
 * without obtaining the appropriate license; however, it may appear in an executable file that is distributed.</p>
 * 
 * @author sruffner
 */
public class GaussRNG implements IRandomNG
{
   /** Construct a <code>GaussRNG</code> with the seed initialized to 1. */
   public GaussRNG() 
   { 
      uniRNG = new UniformRNG();
      setSeed(1); 
   }

   public double next()
   {
      double value;
      if(!gotNext)
      {
         // get two uniform deviates v1,v2 such that (v1,v2) lies strictly inside unit circle, but not at the origin
         double v1, v2, rsq, fac;
         do 
         {
            v1 = 2.0 * uniRNG.next() - 1.0;
            v2 = 2.0 * uniRNG.next() - 1.0;
            rsq = v1*v1 + v2*v2;
         } while( rsq >= 1.0 || rsq == 0.0 );

         // use Box-Muller transformation to transform the uniform deviates to two Gaussian deviates, one of which is
         // saved for the next call to this function.
         fac = Math.sqrt(-2.0 * Math.log(rsq) / rsq);
         nextValue = v1*fac;
         gotNext = true;
         value = v2*fac;
      }
      else 
      {
         value = nextValue;
         gotNext = false;
      }

      return(value);
   }

   public void setSeed(int seed)
   {
      uniRNG.setSeed(seed);
      gotNext = false;
   }

   /** Uniform random-number generator from which Gaussian-distributed sequence is derived. */
   private final UniformRNG uniRNG;
   
   /** Since algorithm generates two numbers at a time, we only have to run the algorithm on every other request. */
   private boolean gotNext; 
   
   /** The next number in the sequence, if it is already available. */
   double nextValue;
}
