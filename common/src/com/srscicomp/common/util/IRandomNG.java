package com.srscicomp.common.util;

/**
 * Interface representing a pseudo-random number generator that produces double-precision floating-point values in 
 * (0.0..1.0), endpoints excluded.
 * @author sruffner
 */
public interface IRandomNG
{
   /** 
    * Reinitialize the random-number generator with the specified seed value.
    * @param seed The new seed. Should be non-zero. If a zero seed value is provided, implementing classes should 
    * choose a default alternative seed.
    */
   void setSeed(int seed);
   
   /** 
    * Generate the next value in the pseudo-random number sequence.
    * @return Next pseudo-random value in (0.0..1.0), endpoints excluded.
    */
   double next();
}
