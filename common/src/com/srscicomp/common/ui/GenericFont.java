package com.srscicomp.common.ui;

/**
 * The types of generic fonts supported in the <code>Painter</code> framework. These correspond directly to the generic 
 * fonts set up by the <code>LocalFontEnvironment</code> facilities. Note that these may not be identical to the 
 * similarly named Java logical fonts.
 * 
 * @see com.srscicomp.common.g2dutil.Painter, LocalFontEnvironment
 * @author  sruffner
 */
public enum GenericFont
{
   MONOSPACE, SERIF, SANSERIF;

   @Override
   public String toString()
   {
      return( super.toString().toLowerCase() );
   }
   
}
