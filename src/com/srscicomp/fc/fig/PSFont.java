package com.srscicomp.fc.fig;

/** 
 * An enumeration of the eight font families represented in the standard set of 35 fonts that are almost univerally 
 * supported by Postscript printer products.
 * 
 * @author sruffner
 */
public enum PSFont
{
   AVANTGARDE("Avant Garde"), BOOKMAN("Bookman"), COURIER("Courier"), HELVETICA("Helvetica"), 
   HELVNARROW("Helvetica Narrow"), NCSCHOOLBOOK("New Century Schoolbook"), PALATINO("Palatino"), TIMES("Times");

   private final String name;

   PSFont(String name) { this.name = name; }

   @Override
   public String toString() { return(name); }
}
