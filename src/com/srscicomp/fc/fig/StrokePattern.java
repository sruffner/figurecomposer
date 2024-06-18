package com.srscicomp.fc.fig;

import java.util.StringTokenizer;

import com.srscicomp.common.util.Utilities;

/**
 * <code>StrokePattern</code> is a simple <em>immutable</em> class defining a graphic stroke pattern as an array of 1-6 
 * positive integers, representing a sequence of alternating dashes and gaps. Each integer represents the length of the 
 * dash or gap in increments of 0.1 line widths; thus, the stroke dashing pattern will look relatively consistent 
 * as line width changes. Each integer dash/gap length is restricted to the range [1..99]. Note that, since the first 
 * integer in the array is always the length of the first dash, any dash gap sequence of length 1 will be interpreted 
 * as a solid stroke. Several of the most common stroking patterns are represented by static <code>StrokePattern</code> 
 * instances.
 * 
 * @author sruffner
 */
public class StrokePattern implements Cloneable
{
   /** A solid stroke (dash-gap array = [10]). */
   public final static StrokePattern SOLID = new StrokePattern("solid", 10);
   
   /** A dotted stroke (dash-gap array = [10 30]). */
   public final static StrokePattern DOTTED = new StrokePattern("dotted", 10, 30);

   /** A dashed stroke (dash-gap array = [30 30]). */
   public final static StrokePattern DASHED = new StrokePattern("dashed", 30, 30);

   /** A dashdot stroke (dash-gap array = [30 30 10 30]). */
   public final static StrokePattern DASHDOT = new StrokePattern("dashdot", 30, 30, 10, 30);

   /** A dashdotdot stroke (dash-gap array = [30 30 10 30 10 30]). */
   public final static StrokePattern DASHDOTDOT = new StrokePattern("dashdotdot", 30, 30, 10, 30, 10, 30);

   /** An array of 5 often-used stroke patterns: <code>SOLID, DOTTED, DASHED, DASHDOT, DASHDOTDOT</code>. */
   public final static StrokePattern[] COMMONPATTERNS = new StrokePattern[] {SOLID,DOTTED,DASHED,DASHDOT,DASHDOTDOT};

   /**
    * Is the specified string the synonym for one of the commonly used stroking patterns?
    * 
    * @param s The string to check.
    * @return <code>True</code> iff <code>s</code> is the synonym of a member of <code>COMMONPATTERNS</code>.
    * @see StrokePattern#COMMONPATTERNS
    */
   public static boolean isCommonPatternSynonym(String s)
   {
      for(StrokePattern commonpattern : COMMONPATTERNS) if(commonpattern.synonym.equals(s)) return (true);

      return(false);
   }

   /**
    * If the specified pattern has the same dash-gap array as one of the common stroking patterns, return the common 
    * pattern that it matches.
    * @param p The pattern to test.
    * @return A member of <code>COMMONPATTERNS</code> that matches the argument, or <code>null</code> if no match.
    * @see StrokePattern#COMMONPATTERNS
    */
   public static StrokePattern getMatchingCommonPattern(StrokePattern p)
   {
      for(StrokePattern commonpattern : COMMONPATTERNS) if(StrokePattern.equal(commonpattern, p))
         return commonpattern;

      return(null);
   }

   /** Maximum number of entries in the dash-gap sequence. The sequence must always contain at least one entry. */
   public final static int MAXARRAYLEN = 6;

   /** The minimum length of a dash or gap, in 0.1 line widths. */
   public final static int MINDASHLEN = 1;

   /** The maximum length of a dash or gap, in 0.1 line widths. */
   public final static int MAXDASHLEN = 99;

   /**
    * Compare two <code>StrokePattern</code>s for equality. Equality holds iff neither pattern is <code>null</code> and 
    * (i) both reference the same Java object, or (ii) their dash-gap patterns have the same length and content. 
    * There's one exception: all <code>StrokePattern</code>s with a pattern length of 1 are considered equal, since they 
    * all represent a solid stroke.
    * 
    * @param p1 The first stroke pattern.
    * @param p2 The second stroke pattern.
    * @return <code>True</code> iff the stroke patterns are considered equal, as described.
    */
   public static boolean equal(StrokePattern p1, StrokePattern p2)
   {
      if(p1 == null || p2 == null) return(false);        // by convention, null is undefined and thus equal to nothing
      if(p1 == p2) return(true);                         // trivial case

      int len1 = p1.dashGapArray.length;
      if(len1 != p2.dashGapArray.length) return(false); 
      if(len1 == 1) return(true);                        // all patterns of length 1 are solid strokes!
      for(int i=0; i<len1; i++)
      {
         if(p1.dashGapArray[i] != p2.dashGapArray[i]) return(false);
      }
      return(true);
   }


   /**
    * The alternating dash-gap sequence defining this stroke pattern.
    */
   private final int[] dashGapArray;

   /**
    * A human-readable name for the stroking pattern. This is used only by the static instances that define some of the 
    * most common stroking patterns -- "solid", "dashed", etc.
    */
   private String synonym = null;

   /**
    * Private constructor used by to create the static <code>StrokePattern</code> instances, which have human-readable 
    * names associated with them. These names are returned by the <code>toString</code> method.
    */
   private StrokePattern(String synonym, int... segments)
   {
      this(segments);
      this.synonym = synonym;
   }

   /**
    * Construct a <code>StrokePattern</code> with the specified sequence of alternating dashes and gaps.
    * 
    * @param segments An alternating sequence of up to 6 dash and gap lengths, the first of which is a dash. If there 
    * are more than six arguments, the additional arguments are ignored. If invoked with zero or 1 arguments, the 
    * resulting stroke pattern will be solid. Each dash and gap length is range-restricted to [1..99].
    */
   public StrokePattern(int... segments)
   {
      if(segments.length == 0) dashGapArray = new int[] {1};
      else
      {
         dashGapArray = new int[Math.min(MAXARRAYLEN, segments.length)];
         for(int i=0; i<dashGapArray.length; i++)
         {
            int j = segments[i];
            dashGapArray[i] = Utilities.rangeRestrict(MINDASHLEN, MAXDASHLEN, j);
         }
      }
   }

   /**
    * Does this <code>StrokePattern</code> represent a solid stroke? Any pattern with a dash-gap sequence of length 1 
    * corresponds to a solid stroke (it has no gaps!).
    * 
    * @return <code>True</code> if this <code>StrokePattern</code> is solid.
    */
   public boolean isSolid()
   {
      return(dashGapArray.length < 2);
   }

   /**
    * Returns a copy of the dash-gap array that defines the stroking pattern. Note that the dash and gap lengths are 
    * expressed in units of 0.1 line widths.
    * 
    * @return The dash-gap array, as described.
    */
   public int[] getDashGapArray()
   {
      return dashGapArray.clone();
   }

   /**
    * Get the dash-gap array defined by this <code>StrokePattern</code> for the specified line width.
    * 
    * <p><i>Adjustment for the stroke endcap style</i>. In both Java2D and Postscript, the "linecap" stroking property
    * may be set to "round" or "square", which puts a round or square decoration at the ends of unclosed paths <i><b>and
    * dash segments</b></i>, projecting beyond the true end of the path by half a line-width. In these cases, the actual
    * dash segment length is one line-width longer than the design length, intruding into the space occupied by the gaps
    * on either side. To compensate for this, set the <code>adjGaps</code> flag.</p>
    * 
    * @param lineWidth The target line width in milli-inches.
    * @param adjGaps If this flag is set and the current dash gap array has at least two entries (at least one dash
    * and one gap!), each gap length in the returned array will be lengthened by one linewidth.
    * @return The dash-gap array for the specified line width, with each dash/gap length expressed in milli-inches. If 
    * the line width is non-positive, the method returns {1}, representing a solid line.
    */
   public float[] getDashGapArrayMilliIn(double lineWidth, boolean adjGaps)
   {
      if(lineWidth <= 0) return( new float[] {1} );

      float[] result = new float[dashGapArray.length];
      for(int i = 0; i < result.length; i++)
         result[i] = (float) (dashGapArray[i] * lineWidth / 10.0);

      if(adjGaps && result.length > 1) for(int i=1; i<result.length; i+=2)
      {
         result[i] += ((float) lineWidth);
      }
      
      return(result);
   }
   
   @Override
   public String toString()
   {
      return((synonym != null) ? synonym : Utilities.toString(dashGapArray));
   }

   /**
    * Create a <code>StrokePattern</code> encapsulating a stroke dash-gap pattern defined by a string argument.
    * 
    * @param defn The definition string. This may be the synonym of any of the common stroking patterns; otherwise it 
    * must be parsable as an integer array of 1-6 whitespace-separated integers in [1..99].
    * @return The corresponding <code>StrokePattern</code>, or <code>null</code> if string could not be parsed.
    */
   public static StrokePattern fromString(String defn)
   {
      if(defn == null || defn.isEmpty()) return(null);
      for(StrokePattern commonpattern : COMMONPATTERNS)
         if(defn.equals(commonpattern.synonym))
            return commonpattern;

      StringTokenizer tokenizer = new StringTokenizer(defn);
      int n = tokenizer.countTokens();
      if(n > MAXARRAYLEN) return(null);
      int[] vals = new int[n];
      int i = 0;
      while(tokenizer.hasMoreTokens())
      {
         try { vals[i] = Integer.parseInt(tokenizer.nextToken()); }
         catch(NumberFormatException nfe) {return(null);}
         if(vals[i] < MINDASHLEN || vals[i] > MAXDASHLEN) return(null);
         ++i;
      }

      StrokePattern patn = new StrokePattern(vals);
      for(i=0; i<COMMONPATTERNS.length; i++)
         if(StrokePattern.equal(patn, COMMONPATTERNS[i])) 
            return(COMMONPATTERNS[i]);
      
      return(patn);
   }

   @Override public StrokePattern clone() throws CloneNotSupportedException
   {
      return (StrokePattern) super.clone();
   }
}
