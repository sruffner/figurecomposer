package com.srscicomp.common.ui;

import java.awt.Font;

/**
 * The types of font face variants supported in the <code>Painter</code> framework. These correspond to font face 
 * variants that can be accessed via <code>LocalFontEnvironment</code> facilities. 
 * 
 * @see com.srscicomp.common.g2dutil.Painter, LocalFontEnvironment
 * @author 	sruffner
 */
public enum FontStyle
{
   PLAIN(Font.PLAIN), BOLD(Font.BOLD), ITALIC(Font.ITALIC), BOLDITALIC(Font.BOLD+Font.ITALIC);

   private final int styleCode;

   FontStyle(int styleCode)
   {
      this.styleCode = styleCode;
   }

   public int getCode()
   {
      return(styleCode);
   }

   public boolean isBold()
   {
      return((styleCode & Font.BOLD) == Font.BOLD);
   }

   public boolean isItalic()
   {
      return((styleCode & Font.ITALIC) == Font.ITALIC);
   }

   @Override
   public String toString()
   {
      return( super.toString().toLowerCase() );
   }
   
   public static FontStyle getFontStyle(boolean bold, boolean italic)
   {
      if(bold && italic) return FontStyle.BOLDITALIC;
      else if(bold) return FontStyle.BOLD;
      else if(italic) return FontStyle.ITALIC;
      else return FontStyle.PLAIN;
   }
}
