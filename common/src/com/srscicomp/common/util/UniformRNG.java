package com.srscicomp.common.util;


/**
 * A pseudo-random number generator that returns a sequence of uniformly distributed floating-point values in the range
 * (0.0 .. 1.0), endpoints excluded. It encapsulates the "ran1" algorithm presented on p.282 in:
 * 
 * <p><i>Press, WH; et al.  "Numerical recipes in C: the art of acientific computing".  New York:  Cambridge University 
 * Press, Copyright 1988-1992.</i></p>
 *
 * <p>The algorithm uses a 32-entry table to shuffle the output of a "Minimal Standard" linear congruential generator, 
 * of the form I(n+1) = A*I(n) % M (with A and M carefully chosen). Schrage's method is used to compute I(n+1) without 
 * an integer overflow. The 32-bit integers output by the algorithm fall in the range [1..M-1]; dividing by M=2^31 gives
 * a double-valued output in (0..1). For algorithmic details, consult the book. The algorithm is such that we could see 
 * some skewing of the distribution at the largest float value less than 1.0.</p>
 *
 * <p>NOTE: IAW the licensing policy of "Numerical Recipes in C", this class is not distributable in source code form 
 * without obtaining the appropriate license; however, it may appear in an executable file that is distributed.</p>
 *
 * @author sruffner
 */
public class UniformRNG implements IRandomNG
{

   /** Construct a <code>UniformRNG</code> with the seed initialized to 1. */
   public UniformRNG() 
   { 
      shuffle = new int[TABLESZ];
      setSeed(1); 
   }
   
   public double next()
   {
      // compute I(n+1) = A*I(n) % M using Shrage's method to avoid integer overflows
      int k = current/LC_Q; 
      current = LC_A * (current - k*LC_Q) - k*LC_R; 
      if(current < 0) current += LC_M;

      // use last number retrieved from shuffle table to calculate index of next number to retrieve. Replace that entry
      // in shuffle table with current output of LC generator, just calculated above.
      int index = lastOut / NDIV; 
      lastOut = shuffle[index]; 
      shuffle[index] = current; 

      // convert int in [1..M-1] to floating-pt output in (0..1)
      return(DSCALE * ((double)lastOut));
   }

   public void setSeed(int seed)
   {
      // always start with a strictly positive seed
      current = (seed == 0) ? 1 : ((seed < 0) ? -seed : seed); 

      // after discarding first 8 ints generated by algorithm, full shuffle table with next TABLESZ ints generated
      int k; 
      for(int j = TABLESZ+7; j >= 0; j--)
      {
         k = current/LC_Q; 
         current = LC_A * (current - k*LC_Q) - k*LC_R; 
         if(current < 0) current += LC_M;
         if(j < TABLESZ) shuffle[j] = current;
      }

      lastOut = shuffle[0]; 
   }

   /** The shuffle table. */
   private final int[] shuffle;
   
   /** The last integer taken from the shuffle table. */
   private int lastOut;
   
   /** The current value of the linear congruential generator. */
   private int current;
   
   /** Size of the shuffle table. */
   private final static int TABLESZ = 32;
   
   // Parameters of the internal linear contruential generator, <i>I' = A*I % M</i>, using Schrage's method to avoid
   // integer overflow.
   private final static int LC_M = 2147483647; 
   private final static int LC_A = 16807;
   private final static int LC_Q = 127773;
   private final static int LC_R = 2836;
   private final static int NDIV = (1 + (LC_M - 1)/TABLESZ);
   private final static double DSCALE = 1.0/((double)LC_M); 
}
