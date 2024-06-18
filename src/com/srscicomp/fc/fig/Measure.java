package com.srscicomp.fc.fig;
import com.srscicomp.common.util.Utilities;

/**
 * <b>Measure</b> is a simple <i>immutable</i> encapsulation of a linear measure or coordinate as a double-precision
 * floating-point number with associated units. Four physical units of measurement are supported -- inches, centimeters,
 * millimeters, and typographical points (we assume 1 pt == 1/72 in). In addition, two relative units of measure are 
 * available: percentage and "user" units. These, of course, require some sort of measuring context or "viewport". A 
 * measure in percentage units is interpreted as a percentage of the viewport's extent, while "user" units refer to the 
 * native units of the viewport's coordinate system, whatever they may be.
 * 
 * <p><b>Measure</b> is not intended for very large or very small measurements. The {@link Constraints} inner class
 * define constraints that can be used to restrict the domain of allowed values for a <b>Measure</b> -- [min .. max] 
 * range, maximum number of significant digits in the measure's numerical value, maximum number of fractional digits 
 * (digits after the decimal point in decimal notation), and the allowed units (eg, some measures could be restricted to
 * only physical units of measurement). The maximum number of significant digits supported is 7, and the maximum number 
 * of fractional digits is 3.</p>
 * 
 * @author sruffner
 */
public class Measure implements Cloneable
{
   @Override public Measure clone() throws CloneNotSupportedException
   {
      return (Measure) super.clone();
   }

   /** An enumeration of the different measurement units supported by <b>Measure</b>. */
   public enum Unit
   {
      IN("in"), CM("cm"), MM("mm"), PT("pt"), PCT("%"), USER("u");

      final String tag;
      final boolean isRelative;
      Unit(String tag) { this.tag = tag; this.isRelative = (tag.equals("%") || tag.equals("u")); }

      public boolean isRelative() { return(isRelative); }
      
      @Override public String toString() { return(tag); }
   }

   /** The maximum number of fractional digits in the numerical value of a measurement, regardless of units. */
   private final static int MAXFRACDIGITS = 3;

   /** The maximum number of significant digits in the numerical value of a measurement, regardless of units. */
   private final static int MAXSIGDIGITS = 7;

   /** A list of the supported physical measurement units. */
   public final static Unit[] REALUNITS = new Unit[] {Unit.IN, Unit.CM, Unit.MM, Unit.PT};

   /** A list of all supported measurement units.  */
   public final static Unit[] ALLUNITS = new Unit[] {Unit.IN, Unit.CM, Unit.MM, Unit.PT, Unit.PCT, Unit.USER};

   /** Multiply a measure in typographical points by this factor to convert to inches (1pt = 1/72 in). */
   public final static double PT2IN = 0.0138888889;

   /** Multiply a measure in millimeters by this factor to convert to inches. */
   public final static double MM2IN = 0.03937;

   /** 
    * Create a measure defined by a string of the form "Nu", where the string token N is parsable as a floating-point
    * number, and the string token u is one of the recognized unit tokens.
    * 
    * @param s String definition of a measurement.
    * @return A new <b>Measure</b> consistent with the string definition, or null if that definition is invalid.
    */
   public static Measure fromString(String s)
   {
      if(s == null || s.isEmpty()) return(null);
      for(Unit allunit : ALLUNITS)
      {
         int unitIndex = s.indexOf(allunit.toString());
         if(unitIndex > 0)
         {
            double val;
            try
            {
               val = Double.parseDouble(s.substring(0, unitIndex));
            } catch(NumberFormatException nfe)
            {
               return (null);
            }

            return (new Measure(val, allunit));
         }
      }
      return(null);
   }

   /**
    * Create a measure with the specified numeric value and units, but further restricted as necessary to satisfy the
    * specified constraints.
    * 
    * @param value Numerical value of measurement. An ill-defined value is treated as 0.
    * @param u Units of measurement. Null is interpreted as {@link Unit#PCT}.
    * @param c The constraints object. If null, the measure is unconstrained. Otherwise, the value and units of the
    * measure will be altered as necessary to satisfy the constraints.
    * @return The constrained measure.
    */
   public static Measure getConstrainedMeasure(double value, Unit u, Constraints c)
   {
      Measure m = new Measure(value, u);
      if(c != null) m = c.constrain(m);
      return(m);
   }
   
   /**
    * Return a measure in the specified physical units that matches the specified length in milli-inches and is further
    * restricted to satisfy the specified constraints.
    * 
    * @param dMI The length of the desired measure in milli-inches. An ill-defined value is treated as 0.
    * @param u The desired units of measure. If null, or if percentage or user units, then {@link Unit#IN} is assumed.
    * @param c The constraints object. If null, the measure is unconstrained.
    * @return The constrained measure.
    */
   public static Measure getConstrainedRealMeasure(double dMI, Unit u, Constraints c)
   {
      if(u==null || u==Unit.PCT || u==Unit.USER) u = Unit.IN;
      if(!Utilities.isWellDefined(dMI)) dMI = 0;
      Measure m = new Measure(milliInToRealUnits(dMI, u), u);
      if(c != null) m = c.constrain(m);
      return(m);
   }
   
   /**
    * Convert the measured length in the specified physical units to milli-inches. Preserves up to 3 fractional digits
    * and up to 7 significant digits in the result.
    * 
    * @param d Numerical value of the measured length.
    * @param u Measurement units. Must be a physical unit rather than one of the relative units (%, user).
    * @return The measurement expressed in milli-inches.
    */
   public static double realUnitsToMI(double d, Unit u) 
   {
      if(u == null || u == Unit.PCT || u == Unit.USER) 
         throw new IllegalArgumentException( "Unspecified units or relative units cannot be converted!" );

      double converted = d;
      if(u == Unit.IN) converted *= 1000;
      else if(u == Unit.MM) converted *= MM2IN * 1000;
      else if(u == Unit.CM) converted *= MM2IN * 10000;
      else if(u == Unit.PT) converted *= PT2IN * 1000;     // 1 pt = 1/72 in, or 1000/72 thous-in
      
      return(Utilities.limitSigAndFracDigits(converted, MAXSIGDIGITS, MAXFRACDIGITS));
   }

   /**
    * Convert the measured length in milli-inches to the specified physical units. Preserves up to 3 fractional digits
    * and up to 7 significant digits in the result.
    * 
    * @param d Numerical value of the measured length in milli-inches.
    * @param u Desired measurement units. Must be a physical unit rather than one of the relative units (%, user).
    * @return The measurement expressed in the units requested.
    */
   public static double milliInToRealUnits(double d, Unit u)
   {
      if(u == null || u == Unit.PCT || u == Unit.USER) 
         throw new IllegalArgumentException( "Unspecified units or relative units cannot be converted!" );

      double converted = d;
      if(u == Unit.IN) converted /= 1000;
      else if(u == Unit.MM) converted /= MM2IN * 1000;
      else if(u == Unit.CM) converted /= MM2IN * 10000;
      else if(u == Unit.PT) converted /= PT2IN * 1000;     // 1 pt = 1/72 in, or 1000/72 thous-in
      
      return(Utilities.limitSigAndFracDigits(converted, MAXSIGDIGITS, MAXFRACDIGITS));
   }

   /**
    * Converts a measurement from one physical unit of measure to another.
    * 
    * @param m The numerical value of the measurement.
    * @param from The current units for the measurement. Must be a physical measurement unit.
    * @param to The desired units for the measurement. Must be a physical measurement unit.
    * @param nFrac Maximum number of fractional digits in the result. Limited to [0..3].
    * @param nSig Maximum number of significant digits in the result. Limited to [1..7].
    * @return The numerical value of the measurement converted to the desired units.
    */
   private static double convertRealUnits(double m, Unit from, Unit to, int nFrac, int nSig)
   {
      if(from == null || from == Unit.PCT || from == Unit.USER || to == null || to == Unit.PCT || to == Unit.USER)
         throw new IllegalArgumentException( "Cannot convert from or to unspecified/percentage/user units" );

      nFrac = Utilities.rangeRestrict(0, MAXFRACDIGITS, nFrac);
      nSig = Utilities.rangeRestrict(1, MAXSIGDIGITS, nSig);
      
      // the silly case -- but limit # of significant and fractional digits!
      if(from == to) return(Utilities.limitSigAndFracDigits(m, nSig, nFrac));
      
      // first convert to inches
      double converted = m;
      if(from == Unit.MM) converted *= MM2IN;
      else if(from == Unit.CM) converted *= MM2IN * 10;
      else if(from == Unit.PT) converted *= PT2IN;     // 1 pt = 1/72 in, or 1000/72 thous-in

      // then convert to desired units
      if(to == Unit.MM) converted /= MM2IN;
      else if(to == Unit.CM) converted /= (MM2IN * 10);
      else if(to == Unit.PT) converted /= PT2IN;

      return(Utilities.limitSigAndFracDigits(converted, nSig, nFrac));
  }

   /**
    * Construct an equivalent measure converted to the specified units, while satisfying the specified measurement 
    * constraints. Both the target units and the units of the original measure must be a physical unit of measure. 
    * 
    * @param src The measurement to convert.
    * @param to Desired units for the converted measure.
    * @param c The constraints object. If null, no constraints are applied.
    * @return A new measure converted to the specified units.
    */
   public static Measure convertRealMeasure(Measure src, Unit to, Constraints c)
   {
      if(src == null || src.isRelative() || to == null || to == Unit.PCT || to == Unit.USER)
         throw new IllegalArgumentException("Cannot convert -- bad arguments!");
      
      Measure m;
      if(c != null)
      {
         double d = convertRealUnits(src.getValue(), src.getUnits(), to, c.nMaxFracDigits, c.nMaxSigDigits);
         m = c.constrain(new Measure(d, to));
      }
      else
         m = new Measure(convertRealUnits(src.getValue(), src.getUnits(), to, MAXFRACDIGITS, MAXSIGDIGITS), to);

      return(m);
   }

   /**
    * Compare two measures for equality. Equality holds if neither measure is null and (i) both reference the same Java 
    * object, or (ii) they have the same units of measure and the same numerical value. Two measures made in different 
    * physical units are NOT considered equal even if they represent the same physical length. Thus, this method is 
    * intended to compare the representations of a measure, not the actual measured length.
    * 
    * @param m1 The first measure.
    * @param m2 The second measure.
    * @return True the measures are considered equal, as described.
    */
   public static boolean equal(Measure m1, Measure m2)
   {
      if(m1 == null || m2 == null) return(false);  // by convention, null is undefined and thus equal to nothing
      return((m1==m2) || (m1.getValue()==m2.getValue() && m1.getUnits() == m2.getUnits()));
   }

   /**
    * Compare two measures for "nearness". Two measurements <i>M1</i> and <i>M2</i> are nearly equivalent if neither
    * measure is null and (i) they're exactly equal, (ii) they have the same units  and <i>abs(M1-M2) &le; T</i>, or 
    * (iii) both are in real units (not relative) and <i>abs(M1'-M2') &le; T</i>, where <i>M1'</i> and <i>M2'</i> are 
    * the measured values when converted to the larger unit of measure with the specified number of fractional digits. 
    * The threshold <i>T</i> is defined by the maximum number of fractional digits, <i>N</i>, to consider in each 
    * measure: <i>T=pow(10,-N)</i>.
    * 
    * @param m1 The first measure.
    * @param m2 The second measure.
    * @param nFrac Maximum number of fractional digits (right of decimal point in decimal notation) in the value of each
    * measure. Limited to [0..3]
    * @return True if the measures are considered equal or nearly equivalent, as described.
    */
   public static boolean near(Measure m1, Measure m2, int nFrac)
   {
      if(m1 == null || m2 == null) return(false);
      if(Measure.equal(m1,m2)) return(true);
      nFrac = Utilities.rangeRestrict(0, MAXFRACDIGITS, nFrac);
      
      Unit unit1 = m1.getUnits();
      Unit unit2 = m2.getUnits();
      double threshold = Math.pow(10, -nFrac);
      boolean isRel1 = unit1.isRelative();
      boolean isRel2 = unit2.isRelative();
      if(isRel1 != isRel2) 
         return(false);
      else if(unit1 == unit2) 
         return(Math.abs(m1.getValue()-m2.getValue()) <= threshold);
      else if(isRel1)
         return(false);

      double unitMI1 = Measure.realUnitsToMI(1, unit1);
      double unitMI2 = Measure.realUnitsToMI(1, unit2);
      Unit tgtUnit = (unitMI1 >= unitMI2) ? unit1 : unit2;
      double d1 = Measure.convertRealUnits(m1.getValue(), unit1, tgtUnit, nFrac, MAXSIGDIGITS);
      double d2 = Measure.convertRealUnits(m2.getValue(), unit2, tgtUnit, nFrac, MAXSIGDIGITS);
      return(Math.abs(d1-d2) <= threshold);
   }

   /** The numerical value of this measure. */
   private final double value;

   /** The units for this measure. */
   private final Unit units;

   /** 
    * Construct a measure.
    * @param value The numerical value. If ill-defined, 0 is assumed.
    * @param units The measurement units. If null, {@link Unit#PCT} is assumed.
    * */
   public Measure(double value, Unit units)
   {
      this.value = Utilities.isWellDefined(value) ? value : 0;
      this.units = (units != null) ? units : Unit.PCT;
   }

   /**
    * Retrieve the numerical value of this measurement.
    * 
    * @return The measurement's numerical value.
    */
   public double getValue() { return(value); }

   /**
    * Get the measurement units.
    * 
    * @return The units of measure.
    */
   public Unit getUnits() { return(units); }

   /**
    * Is this measure specified in one of the relative units (percentage or user units)?
    * 
    * @return True if this is relative measurement.
    */
   public boolean isRelative() { return(units == Unit.PCT || units == Unit.USER); }

   /**
    * Get the measured length in milli-inches, if possible.
    * 
    * @return The measurement converted to milli-inches. <i>Returns 0 if the measure is in percentage or user units, 
    * since these cannot be converted to physical units.</i>
    */
   public double toMilliInches() throws IllegalStateException
   {
      if(isRelative()) return(0);
      return(Measure.realUnitsToMI(getValue(), getUnits()));
   }

   //
   // Object
   //

   /**
    * Prepare the string representation of this measure: "{N}{u}", where {N} is replaced by the measure's numerical
    * numerical value (with up to 3 fractional digits and 7 significant digits preserved) and {u} is replaced by an 
    * abbreviation for the measure's units.
    */
   @Override public String toString() { return(toString(MAXSIGDIGITS, MAXFRACDIGITS)); }

   /**
    * Produce a string representation of this measure: "{N}{u}", where {N} is replaced by the measure's numerical value
    * -- rounded to the number of significant and fractional digits in the specified constraints object --, and {u} is 
    * replaced by an abbreviation for the measure's units.
    * @param c A constraints object. If null, then maximum # of significant and fractional digits are assumed.
    * @return The string representation of this measure, as described.
    */
   public String toString(Constraints c)
   {
      return(c==null ? toString() : toString(c.nMaxSigDigits, c.nMaxFracDigits));
   }
   
   /**
    * Produce a string representation of this measure: "{N}{u}", where {N} is replaced by the measure's numerical value
    * -- rounded to the specified number of significant and fractional digits --, and {u} is replaced by an abbreviation
    * for the measure's units.
    * 
    * @param nSig Maximum number of significant digits included in the measure's numerical value. [1..7]
    * @param nFrac Maximum number of fractional digits included in the measure's numerical value. [0..3].
    * @return The string representation of this measure, as described.
    */
   public String toString(int nSig, int nFrac)
   {
      nSig = Utilities.rangeRestrict(1, MAXSIGDIGITS, nSig);
      nFrac = Utilities.rangeRestrict(0, MAXFRACDIGITS, nFrac);
      return(Utilities.toString(value, nSig, nFrac) + units.toString());
   }

   /**
    * This inner class specifies constraints that may be placed on a <b>Measure</b> particularly in the context of a
    * custom editor which lets the user enter the numerical value and units for a new measurement. These constraints are
    * supported: minimum and maximum allowed numerical values (regardless of units), minimum and maximum measured values
    * in milli-inches (for measures in non-relative units only!), the maximum number of significant digits allowed in
    * the numerical value, the maximum number of fractional digits (after the decimal point in decimal notation), 
    * whether or not percent units are permitted, and whether or not user units are permitted.
    * 
    * <p>Since <b>Measure</b> will typically not be used on very large numbers, <b>Constraints</b> is designed to 
    * limit the absolute size of a measure's numerical value to no more than 0.1*{@link Double#MAX_VALUE}.</p>
    * 
    * @author  sruffner
    */
   public static class Constraints
   {
      /** The maximum absolute value that may be assigned to any measure, regardless of units. */
      private final static double MAXABSVAL = Double.MAX_VALUE/10.0;

      /** Minimum allowed numerical value, regardless of units. */
      public final double min;
      /** Maximum allowed numerical value, regardless of units. */
      public final double max;
      /** Minimum measurement in milli-inches. */
      public final double measuredMinMI;
      /** Maximum measurement in milli-inches. */
      public final double measuredMaxMI;
      /** Maximum number of significant digits in numerical value, whether in scientific or decimal notation. */
      public final int nMaxSigDigits;
      /** Maximum number of fractional digits (right of decimal point) in numerical value when in decimal notation. */
      public final int nMaxFracDigits;
      /** True if percentage units are allowed. */
      public final boolean allowPct;
      /** True if "user" units are allowed. */
      public final boolean allowUser;

      /**
       * Construct a constraints object that places no restrictions on a measurement, except those implicit to all 
       * measurements, namely:
       * <ul>
       * <li>Up to 7 significant digits in the numerical value, with up to 3 digits after the decimal point ("fractional
       * digits").</li>
       * <li>The numerical value of the measure, regardless of units, must lie in the range [-1e9 .. 1e9]</li>
       * </ul>
       */
      public Constraints()
      {
         this(-MAXABSVAL, MAXABSVAL, -MAXABSVAL, MAXABSVAL, MAXSIGDIGITS, MAXFRACDIGITS, true, true);
      }

      /**
       * Construct a constraints object restricting the maximum number of significant and fractional digits allowed in
       * any numerical value and whether or not relative units (percentage and user units) are permitted. The numerical 
       * value of the measure, regardless of units, must lie in the range [-1e9 .. 1e9].
       * 
       * @param nSig Maximum number of significant digits in the numerical value. Range-restricted to [1..7].
       * @param nFrac Maximum number of fractional digits in the numerical value. Range-restricted to [0..3].
       * @param allowAll True if all supported measurement units are allowed; false to restrict to the physical units.
       */
      public Constraints(int nSig, int nFrac, boolean allowAll)
      {
         this(-MAXABSVAL, MAXABSVAL, -MAXABSVAL, MAXABSVAL, nSig, nFrac, allowAll, allowAll);
      }

      /**
       * Construct a constraints object that requires a measure to use physical units and restricts its measured range 
       * and precision.
       * 
       * @param minInMI The minimum value of the measure in milli-inches. Range restricted to [-1e9 .. 1e9].
       * @param maxInMI The maximum value of the measure in milli-inches. Range likewise restricted.
       * @param nSig Maximum number of significant digits in the numerical value. Range-restricted to [1..7].
       * @param nFrac Maximum number of fractional digits in the numerical value. Allowed range is [0..3].
       */
      public Constraints(double minInMI, double maxInMI, int nSig, int nFrac)
      {
         this(-MAXABSVAL, MAXABSVAL, minInMI, maxInMI, nSig, nFrac, false, false);
      }

      /**
       * Construct a constraints object for a linear measure.
       * 
       * <p>All arguments specifying numerical limits are range-restricted to [-1e9 .. 1e9]. An ill-defined lower limit 
       * is set to -1e9, while an ill-defined upper limit is set to 1e9.
       * 
       * @param min The minimum allowed numerical value, regardless of units.
       * @param max The maximum allowed numerical value, regardless of units.
       * @param minInMI When the measure is NOT in relative units, this specifies a minimum value for the measure in
       * milli-inches. Obviously, this constraint cannot be enforced when the units are relative (% or u).
       * @param maxInMI When the measure is NOT in relative units, this specifies a maximum value for the measure in
       * milli-inches. Obviously, this constraint cannot be enforced when the units are relative (% or u).
       * @param nSig Maximum number of significant digits in the numerical value. Allowed range is [1..7].
       * @param nFrac Maximum number of fractional digits in the numerical value. Allowed range is [0..3]. 
       * @param allowPct True if percentage units are allowed; false otherwise.
       * @param allowUser True if user units are allowed; false otherwise
       */
      public Constraints(double min, double max, double minInMI, double maxInMI, int nSig, int nFrac, 
            boolean allowPct, boolean allowUser)
      {
         this.allowPct = allowPct;
         this.allowUser = allowUser;
         this.nMaxSigDigits = Utilities.rangeRestrict(1, MAXSIGDIGITS, nSig);
         this.nMaxFracDigits = Utilities.rangeRestrict(0, MAXFRACDIGITS, nFrac);
         
         double d1, d2;
         d1 = Utilities.isWellDefined(min) && Math.abs(min) <= MAXABSVAL ? min : -MAXABSVAL;
         d2 = Utilities.isWellDefined(max) && Math.abs(max) <= MAXABSVAL ? max : MAXABSVAL;
         this.min = Math.min(d1, d2);
         this.max = Math.max(d1, d2);
         
         d1 = Utilities.isWellDefined(minInMI) && Math.abs(minInMI) <= MAXABSVAL ? minInMI : -MAXABSVAL;
         d2 = Utilities.isWellDefined(maxInMI) && Math.abs(maxInMI) <= MAXABSVAL ? maxInMI : MAXABSVAL;
         this.measuredMinMI = Math.min(d1, d2);
         this.measuredMaxMI = Math.max(d1, d2);
      }

      /**
       * Does the specified measure meet the restrictions defined in this constraints object?
       * 
       * @param m The measure to test. A null measure always fails.
       * @return True if all constraints are satisfied.
       */
      public boolean satisfiedBy(Measure m)
      {
         return((m != null) && satisfiedBy(m.getValue(), m.getUnits()));
      }

      /**
       * Does the numerical value and units of measurement satisfy the restrictions defined in this constraints object?
       * 
       * @param value Numerical value of measurement.
       * @param u Units of measure.
       * @return True if all constraints are satisfied.
       */
      public boolean satisfiedBy(double value, Unit u)
      {
         boolean isPct = (u == null) || (u == Unit.PCT);
         boolean isUser = (!isPct) && (u == Unit.USER);
         boolean ok = Utilities.isWellDefined(value) && (allowPct || !isPct) && (allowUser || !isUser);
         if(ok) ok = (value >= min) && (value <= max);
         if(ok) ok = (Utilities.limitSigAndFracDigits(value, nMaxSigDigits, nMaxFracDigits) == value);
         if(ok && !(isPct || isUser))
         {
            double d = Measure.realUnitsToMI(value, u);
            ok = (d >= measuredMinMI) && (d <= measuredMaxMI);
         }
         return(ok);
      }
      
      /**
       * Constrain the specified measurement.
       * 
       * @param m The measurement to constrain.
       * @return Returns the original measure if it already satisfies these constraints. Otherwise, returns a new 
       * measure that satisfies the constraints and is close as possible to the original measure.
       */
      public Measure constrain(Measure m) { return(constrain(m, false)); }
      
      /**
       * Constrain the specified measurement IAW the following algorithm:
       * <ol>
       * <li>If original measure is null or uses a unit forbidden by this constraints object, return a new measure set 
       * to the minimum numerical value for this constraints object and units of "inches".</li>
       * <li>If the original measure already satisfies this constraints object and <i>milliInPrec==false</i>, then that
       * measure is returned.</li>
       * <li>Otherwise, a new measure is generated with a (possibly) adjusted numerical value:
       * <ul>
       *    <li><i>User units</i>: Round the original value as necessary to limit the # of fractional digits and 
       *    significant digits, then range-restrict the result IAW the settings of this constraints object.</li>
       *    <li><i>Percentage units</i>: Same as for "user" units, except that, if <i>milliInPrec==true</i>, the # of
       *    fractional digits preserved is 1 rather than the max number specified by this constraints object.</li>
       *    <li><i>Any physical unit</i>: First, adjust the original measured value if it is out-of-range when converted
       *    to milli-inches. Then round the resulting numerical value as necessary to limit the # of fractional digits
       *    and significant digits, then range-restrict the result IAW the setting of this constraints object. If 
       *    <i>milliInPrec==true</i>, then the # of allowed fractional digits is determined by the units of the original
       *    measure rather than the maximum number allowed by this constraints object: 3 digits for measures in inches
       *    or cm, 2 for mm, and 1 for pts. Of course, the restriction on the total number of significant digits will 
       *    always be enforced.</li>
       * </ul>
       * </li>
       * </ol>
       * 
       * @param m The measurement to constrain.
       * @param milliInPrec If this flag is set, then the measure is further constrained to roughly milli-inch precision
       * if the units are in/cm/mm/pt, or to 0.1 for % units. See above for details.
       * @return A new measure satisfying this constraints object. If the measure already satisfies the constraints and
       * <i>milliInPrec==false</i>, the method returns a reference to the original measure.
       */
      public Measure constrain(Measure m, boolean milliInPrec)
      {
         if(m == null) return(new Measure(min, Unit.IN));
         else if((!milliInPrec) && satisfiedBy(m)) return(m);
         else if((m.getUnits()==Unit.PCT && !allowPct) || (m.getUnits()==Unit.USER && !allowUser))
            return(new Measure(min, Unit.IN));
         else
         {
            Unit u = m.getUnits();
            double value = m.getValue();
            if(!Utilities.isWellDefined(value)) value = min;
            
            if(u==Unit.PCT || u==Unit.USER)
            {
               int nLimFrac = nMaxFracDigits;
               if(u==Unit.PCT && milliInPrec) nLimFrac = 1;
               
               value = Utilities.limitSigAndFracDigits(value, nMaxSigDigits, nLimFrac);
            }
            else
            {
               double d = Measure.realUnitsToMI(value, u);
               if(d < measuredMinMI || d > measuredMaxMI)
               {
                  d = (d<measuredMinMI) ? measuredMinMI : measuredMaxMI;
                  value = Measure.milliInToRealUnits(d, u);
               }
               
               int nLimFrac = nMaxFracDigits;
               if(milliInPrec) nLimFrac = (u==Unit.PT) ? 1 : (u==Unit.MM ? 2 : 3); 
               
               value = Utilities.limitSigAndFracDigits(value, nMaxSigDigits, nLimFrac);
            }
            value = Utilities.rangeRestrict(min, max, value);
            return(new Measure(value, u));
         }
      }
   }
}
