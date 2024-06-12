package com.srscicomp.fc.fig;

import java.util.StringTokenizer;

import com.srscicomp.common.util.Utilities;

/**
 * <code>LogTickPattern</code> is a simple <em>immutable</em> class defining the locations of tick marks within a single 
 * logarithmic decade. It is essentially a set of flags indicating whether or not a tick mark should be rendered at 
 * each tenth of a decade: 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, and 0.9. Static <code>LogTickPattern</code> instances 
 * are available for some of the most commonly used logarithmic decade tick mark combinations.
 * 
 * @author sruffner
 */
public class LogTickPattern implements Cloneable
{
   /**
    * An array of commonly used log-decade tick mark patterns.
    */
   public final static LogTickPattern[] COMMONLYUSED = new LogTickPattern[] { new LogTickPattern(),
      new LogTickPattern(1,2,3,4,5,6,7,8,9), new LogTickPattern(2,4,6,8), new LogTickPattern(1,3,5,7,9), 
      new LogTickPattern(1,3,6), new LogTickPattern(1,2,4,8), new LogTickPattern(2,4,8), new LogTickPattern(3,7),
      new LogTickPattern(3,5,7), new LogTickPattern(3,5,9)
   };
   
   /**
    * Compare two <code>LogTickPattern</code>s for equality. Equality holds iff neither pattern is <code>null</code> and 
    * (i) both reference the same Java object, or (ii) they enable the same set of log decade tick marks.
    * 
    * @param m1 The first log-decade tick mark pattern.
    * @param m2 The second log-decade tick mark pattern.
    * @return <code>True</code> iff the tick mark patterns are considered equal, as described.
    */
   public static boolean equal(LogTickPattern p1, LogTickPattern p2)
   {
      if(p1 == null || p2 == null) return(false);        // by convention, null is undefined and thus equal to nothing
      if(p1 == p2) return(true);                         // trivial case

      return(p1.tickEnaBits == p2.tickEnaBits);
   }

   /**
    * Construct a <code>LogTickPattern</code> from a definition string. The string must be parsable as a list of up to
    * nine whitespace-separated <em>single-digit non-zero integers</em>. Each integer <em>N</em> parsed from the string 
    * will enable a tick mark at <em>0.1N</em> in the <code>LogTickPattern</code> created. The integers need not be 
    * listed in ascending order, but <em>no repeats are allowed</em>. An empty definition string produces a 
    * <code>LogTickPattern</code> that enables NO tick marks within a log decade. If the definition string does not 
    * meet these criteria, <code>null</code> is returned.
    * 
    * @param defn A string listing the locations where log-decade ticks should be enabled, as described.
    * @return A <code>LogTickPattern</code> based on the string definition, or <code>null</code> if string does not 
    * meet the criteria stated above.
    */
   public static LogTickPattern fromString(String defn)
   {
      if(defn == null) return(null);
      if(defn.length() == 0) return(new LogTickPattern());

      StringTokenizer tokenizer = new StringTokenizer(defn);
      int nTokens = tokenizer.countTokens();
      if(nTokens > 9) return(null);
      if(nTokens == 0) return(new LogTickPattern());

      int flags = 0;
      for(int i = 0; i < nTokens; i++)
      {
         String token = tokenizer.nextToken();
         int n;
         try { n = Integer.parseInt(token); }
         catch(NumberFormatException nfe) {return(null);}

         if(n < 1 || n > 9 || ((flags & (1<<n)) != 0))
            return(null);

         flags |= (1<<n);
      }
      return(new LogTickPattern(flags));
   }

   /**
    * Bit mask indicating whether or not a tick mark is enabled at each tenth of a logarithmic decade. Bit 1 enables 
    * a tick at 0.1, bit 2 at 0.2, ... , and bit 9 at 0.9. Note that bit 0 is not used!
    */
   private final int tickEnaBits;

   /**
    * Construct a <code>LogTickPattern</code> which enables NO tick marks within a logarithmic decade.
    */
   public LogTickPattern()
   {
      tickEnaBits = 0;
   }

   /**
    * Construct a <code>LogTickPattern</code> with the specified tick enable mask.
    * 
    * @param mask A bit mask that enables or disables a tick mark at each tenth of a logarithmic decade. Bit <em>N</em> 
    * enables a tick mark at <em>0.1N</em>, where <em>N</em> lies in [1..9]. All other bits in the mask are ignored. 
    */
   public LogTickPattern(int mask)
   {
      tickEnaBits = 0x03fe & mask;
   }

   /**
    * Construct a <code>LogTickPattern<code> with tick marks enabled at selected tenths of a logarithmic decade.
    * 
    * @param tickEnable Up to 9 flags indicating whether or not a tick mark is enabled at each tenth of the decade. The 
    * first element indicates whether or not a tick mark is enabled at 0.1; the second, 0.2; and so on. If more than  
    * 9 arguments are supplied, the extras are ignored. If no arguments are supplied, NO tick marks are enabled.
    */
   public LogTickPattern(boolean... tickEnable)
   {
      int flags = 0;
      if(tickEnable != null) for(int i=0; i<9 && i<tickEnable.length; i++)
      {
         if(tickEnable[i]) flags |= (1 << (i+1));
      }
      tickEnaBits = flags;
   }

   /**
    * Construct a <code>LogTickPattern<code> with tick marks enabled at selected tenths of a logarithmic decade.
    * 
    * @param tickLocs Up to 9 integers, each of which should be a single digit <em>N</em> in [1..9], indicating that a 
    * tick mark is enabled at <em>0.1N</em>. Any integer less than 1 or greater than 9 is ignored. Additional arguments 
    * beyond the first 9 are ignored. If no arguments are supplied, NO tick marks are enabled.
    */
   public LogTickPattern(int... tickLocs)
   {
      int flags = 0;
      if(tickLocs != null) for(int i=0; i<9 && i<tickLocs.length; i++)
      {
         if(tickLocs[i] >= 1 && tickLocs[i] <= 9) flags |= (1 << tickLocs[i]);
      }
      tickEnaBits = flags;
   }

   /**
    * Return the log-decade tick locations enabled by this <code>LogTickPattern</code>. Each integer in the array will
    * be a single digit <em>N</em> in [1..9], indicating that a tick mark should be rendered at <em>0.1N</em>. The array 
    * will contain no repeats, and is sorted in ascending order. It will never be <code>null</code>, but it could be 
    * empty -- indicating that no tick marks are enabled within a logarithmic decade.
    * 
    * @return The enabled log-decade tick locations, as described.
    * @see LogTickPattern#getEnabledTickMask()
    */
   public int[] getEnabledTicks()
   {
      int nTicks = 0;
      for(int i=1; i<=9; i++)
         if((tickEnaBits & (1<<i)) != 0)
            ++nTicks;

      int[] res = new int[nTicks];
      int j = 0;
      for(int i=1; i<=9; i++)
         if((tickEnaBits & (1<<i)) != 0)
            res[j++] = i;

      return(res);
   }

   /**
    * Return the bit mask indicating which log-decade tick locations are enabled by this <code>LogTickPattern</code>. 
    * If bit <em>N</em> in the mask is set -- where N lies in [1..9] -- then a tick mark should be rendered at 
    * <em>0.1N</em>. All other bits in the mask have no meaning and will be set to 0. If the mask equals 0, then no 
    * tick marks are enabled by this <code>LogTickPattern</code>.
    * 
    * @return The log-decade tick mark enable mask, as described.
    * @see LogTickPattern#getEnabledTicks()
    */
   public int getEnabledTickMask()
   {
      return(tickEnaBits);
   }

   /**
    * Does this <code>LogTickPattern enable a log-decade tick mark at <em>0.1N</em>, where <em>N</em> lies in [1..9]?
    * 
    * @param n The tick mark location <em>N</em>, as described.
    * @return <code>True</code> if tick mark is enabled at specified location. Returns <code>false</code> if argument 
    * lies outside the range [1..9].
    */
   public boolean isTickEnabledAt(int n)
   {
      return((n < 1 || n > 9) ? false : ((tickEnaBits & (1<<n)) != 0));
   }

   @Override
   public String toString()
   {
      return(Utilities.toString(getEnabledTicks()));
   }
}
