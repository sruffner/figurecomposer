package com.srscicomp.common.ui;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <code>LocalFontEnvironment</code> contains a set of static methods that provide information about fonts installed on 
 * the local host system.
 * 
 * @author 	sruffner
 */
public class LocalFontEnvironment
{
	private final static String HELVETICA = "Helvetica";
	private final static String ARIAL = "Arial";
	private final static String LUCIDASANS = "Lucida Sans";
	private final static String TIMES = "Times";
	private final static String TIMESNR = "Times New Roman";
	private final static String LUCIDABRIGHT = "Lucida Bright";
	private final static String COURIER = "Courier";
	private final static String COURIERNEW = "Courier New";
   private final static String LUCIDASANSTYPE = "Lucida Sans Typewriter";

	private final static String[] boldKeywords = new String[] {"bold", "demi", "medium", "heavy", "black"};
	private final static String[] boldAbbrevs = new String[] {"Bd", "Dm", "Md", "Hv", "Blk"};
	private final static String[] italicKeywords = new String[] {"italic", "oblique", "script", "cursiv", "inclined"};
	private final static String[] italicAbbrevs = new String[] {"It", "Ob", "Ks", "Ic", "Sl"};

	/**
	 * A map of all physical font family names found in the local graphics enviroment. The map is a collection of 
	 * <i>(key, variants)</i> pairs, where <i>key</i> is the font family name and <i>variants</i> is a non-empty list 
	 * holding the typeface names of all installed variants in that font family. For example, for the Arial font, the key 
	 * "Arial" might point to a list containing typeface variants "Arial", "Arial Bold", "Arial Italic", etc. 
	 * 
	 * <p>This internal font map is used to quickly ascertain whether a named font family is installed in the local 
	 * environment, and what typeface variants are available in a particular family.</p>
	 */
	private static Map<String, List<String>> envFonts = null;

	/** Installed font family mapped to the generic font class "sans-serif". */
	private static String sansSerifFont = null;

	/** Installed font family mapped to the generic font class "serif". */
	private static String serifFont = null;

	/** Installed font family mapped to the generic font class "monospace". */
	private static String monospaceFont = null;

	/**
	 * Query the local host's graphics environment to identify and cache the set of fonts installed on the system. 
	 * 
	 * <p>In addition, an attempt is made to find suitable physical fonts to represent three of the SVG/CSS generic font 
	 * families "serif", "sans-serif", and "monospace". The method looks for the following fonts in the order listed.  
	 * <ul>
	 * 	<li>sans-serif: Helvetica; Arial; Lucida Sans</li>
	 * 	<li>serif: Times; Times New Roman; Lucida Bright</li> 
	 * 	<li>monospace: Courier; Courier New; Lucida Sans Typewriter</li>
	 * </ul>
	 * Since these fonts are very common, it is unlikely that a match won't be found. In fact, the last three should 
	 * always be present since they come with the Java SDK. If no physical font is found, a logical font name is assigned
	 * instead.</p>
	 * 
	 * <p>This method is typically called during application startup. It can be invoked at other times to reinitialize 
	 * <code>LocalFontEnviroment</code> when fonts are installed in or removed from the local host.</p>
	 * 
	 * @throws Error if no fonts are found on the system
	 */
	public static void initialize() throws Error
	{
		// first, cache all installed fonts
		Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		envFonts = new HashMap<String, List<String>>();
		for( int i=0; i<fonts.length; i++ )
		{
			String family = fonts[i].getFamily();
			// skip logical font families; we want real font names only
			if( !(family.equalsIgnoreCase("dialog") || family.equalsIgnoreCase("dialoginput") || 
				 family.equalsIgnoreCase("serif") || family.equalsIgnoreCase("sansserif") ||
				 family.equalsIgnoreCase("monospaced")) )
			{
				List<String> variants = envFonts.get(family);
				if( variants == null )
				{
					variants = new ArrayList<String>(2);
					variants.add( fonts[i].getFontName() );
					envFonts.put(family,variants);
				}
				else 
					variants.add( fonts[i].getFontName() );
			}
		}

		// don't continue if there are no fonts on the system!
		if(envFonts.isEmpty())
			throw new Error( "Did not find any fonts installed on local host" );

		// now find a suitable physical font for each of our generic font classes. Use corresponding logical font only as
		// a last resort.
		if(isFontInstalled(HELVETICA)) sansSerifFont = HELVETICA;
		else if(isFontInstalled(ARIAL)) sansSerifFont = ARIAL;
		else if(isFontInstalled(LUCIDASANS)) sansSerifFont = LUCIDASANS;
		else sansSerifFont = "SansSerif";
			

		if(isFontInstalled(TIMES)) serifFont = TIMES;
		else if(isFontInstalled(TIMESNR)) serifFont = TIMESNR;
		else if(isFontInstalled(LUCIDABRIGHT)) serifFont = LUCIDABRIGHT;
		else serifFont = "Serif";

		if(isFontInstalled(COURIER)) monospaceFont = COURIER;
      else if(isFontInstalled(COURIERNEW)) monospaceFont = COURIERNEW;
      else if(isFontInstalled(LUCIDASANSTYPE)) monospaceFont = LUCIDASANSTYPE;
		else monospaceFont = "Monospaced";
	}

	/** 
	 * Return the name of the installed font family that is currently mapped to the generic "sans-serif" font class. The
	 * font family is chosen during initialization of <code>LocalFontEnvironment</code>. In the unlikely case that a
	 * physical font is not found, method will return the Java logical font name "SansSerif".
	 * @return Name of physical font family mapped to the generic "sans-serif" font
    * @see LocalFontEnvironment#initialize()
	 */
	public static String getSansSerifFont()
	{
		if(sansSerifFont == null) initialize();
		return(sansSerifFont);
	}

	/** 
	 * Return the name of the installed font family that is currently mapped to the generic "serif" font class. The
    * font family is chosen during initialization of <code>LocalFontEnvironment</code>. In the unlikely case that a
    * physical font is not found, method will return the Java logical font name "Serif".
    * @return Name of physical font family mapped to the generic "serif" font
    * @see LocalFontEnvironment#initialize()
	 */
	public static String getSerifFont()
	{
		if(serifFont == null) initialize();
		return(serifFont);
	}

	/** 
	 * Return the name of the installed font family that is currently mapped to the generic "monospace" font class. The
    * font family is chosen during initialization of <code>LocalFontEnvironment</code>. In the unlikely case that a
    * physical font is not found, method will return the Java logical font name "Monospaced".
    * @return Name of physical font family mapped to the generic "monospace" font
    * @see LocalFontEnvironment#initialize()
	 */
	public static String getMonospaceFont()
	{
		if(monospaceFont == null ) initialize();
		return(monospaceFont);
	}

	/**
	 * Does the local host include one or more font faces from the specified font family?
	 * @param familyName Font family name to check.
	 * @return True if the specified font family is present on the local host.
	 */
	public static boolean isFontInstalled( String familyName )
	{
		if(envFonts == null) initialize();
		return((familyName != null) && envFonts.containsKey(familyName));
	}

	/**
	 * Retrieve the font family names for all physical fonts installed on the local host.
	 * @return Array of all installed font families, listed in alphabetical order.
	 */
	public static String[] getInstalledFontFamilies()
	{
		if(envFonts == null) initialize();
		Object[] keys = envFonts.keySet().toArray();
		String[] families = new String[keys.length];
		for(int i=0; i<families.length; i++) families[i] = (String) keys[i];
		Arrays.sort(families);
		return(families);
	}

	/**
	 * Retrieve the list of all font families installed on the local host that can display at least one character in the 
	 * specified Unicode character subset.
	 * 
	 * @param charSet The set of characters of interest.
	 * @return The array of installed font families that can display at least one character from the specified character 
    * set. Listed in alphabetical order.
    * @see LocalFontEnvironment#getInstalledFontFamilies(UnicodeSubset, boolean)
	 */
	public static String[] getInstalledFontFamilies(UnicodeSubset charSet)
	{
      return(getInstalledFontFamilies(charSet, false));
	}

   /**
    * Retrieve the list of all font families installed on the local host that can display at least one character or all 
    * characters in the specified Unicode character subset.
    * 
    * @param charSet The set of characters of interest.
    * @param strict If true, returns only those font families that can display <i>every</i> character in the set.
    * @return The array of installed font families that can display at least one or every character from the specified 
    * character set. Listed in alphabetical order.
    */
   public static String[] getInstalledFontFamilies(UnicodeSubset charSet, boolean strict)
   {
      String[] allFamilies = getInstalledFontFamilies();
      List<String> goodFamList = new ArrayList<String>(10);
      for( int i=0; i<allFamilies.length; i++ )
      {
         Font f = new Font(getInstalledVariant(allFamilies[i],false,false), Font.PLAIN, 8);
         if(charSet.isDisplayableIn(f, strict))
            goodFamList.add( allFamilies[i] );
      }

      String[] goodFamilies = new String[goodFamList.size()];
      for(int i=0; i<goodFamList.size(); i++) goodFamilies[i] = (String) goodFamList.get(i);
      return(goodFamilies);
   }

   /**
	 * Retrieve the installed font typeface in the specified font family that satisfies the specified style constraints. 
	 * If the specified font family does not include a typeface satisfying the style constraints, the best possible 
	 * match is returned. 
	 * 
	 * @param family Name of desired font family.
	 * @param isBold True to request a bold font face.
	 * @param isItalic True to request an italic font face.
	 * @return Name of the particular font face installed on the local host that is part of the specified font family and 
	 * satisfies as many of the specified style constraints as possible. Returns null if the specified font family is not 
	 * installed.
	 */
	public static String getInstalledVariant(String family, boolean isBold, boolean isItalic)
	{
		if(envFonts == null) initialize();
		List<String> variants = envFonts.get(family);
		if(variants == null) return(null);
		if(variants.size() == 1) return((String) variants.get(0));

		return(getStyleVariant(family, isBold, isItalic));
	}

	/** 
	 * Get font objects representing all style variants installed on the host system for the specified font family
	 * @param family The desired font family
	 * @return Array of font objects, each of which is a distinct style variant installed on the host system for the
	 * font family specified. Font size defaults to 10pt. If font family not found, returns null.
	 */
	public static Font[] getInstalledVariants(String family)
	{
	   if(envFonts == null) initialize();
	   List<String> variants = envFonts.get(family);
	   if(variants == null) return(null);
	   Font[] fonts = new Font[variants.size()];
	   for(int i=0; i<variants.size(); i++) fonts[i] = new Font(variants.get(i), Font.PLAIN, 10);
	   
	   return(fonts);
	}
	
   /**
    * Find the font face installed on the host system that best matches the specified parameters:
    * <ul>
    *    <li>If the specified font family is not found, the installed font family that is mapped to the specified 
    *    generic font will be used instead.</li>
    *    <li>If the resolved font family includes multiple style variants, that variant which best matches the specified 
    *    font style will be selected.</li>
    * </ul>
    * 
    * @param family The desired font family.
    * @param altFont An alternative generic font, which is always be mapped to an installed font. If null, then 
    * <code>GenericFont.SANSERIF</code> is assumed.
    * @param style The desired font style. If null, <code>FontStyle.PLAIN</code> is assumed.
    * @return A <code>Font</code> object representing the typeface installed on the local host that best matches the 
    * specified arguments to the extent possible. Font size defaults to 10pt.
    */
   public static Font resolveFont(String family, GenericFont altFont, FontStyle style)
   {
      if(envFonts == null) initialize();

      GenericFont f = (altFont != null) ? altFont : GenericFont.SANSERIF;
      FontStyle s = (style != null) ? style : FontStyle.PLAIN;
      if(!envFonts.containsKey(family)) switch(f)
      {
         case SANSERIF : family = getSansSerifFont(); break;
         case SERIF : family = getSerifFont(); break;
         default : family = getMonospaceFont(); break;
      }
      
      if(envFonts.get(family) == null)
      {
         // could happen if unable to map a physical font to one of the generic font classes, in which case we use
         // the logical font name. Constructing the font object is different in this case.
         int styleBits = Font.PLAIN;
         if(s.isBold()) styleBits |= Font.BOLD;
         if(s.isItalic()) styleBits |= Font.ITALIC;
         return(new Font(family, styleBits, 10));
      }
      else
         return( new Font(getStyleVariant(family, s.isBold(), s.isItalic()), Font.PLAIN, 10) );
   }
   
   /**
	 * Retrieve an installed font face that matches the specified family name and style constraints.
	 * 
	 * @param family Name of an installed font family; MUST be a valid, installed font family.
	 * @param isBold True to request a bold font face.
	 * @param isItalic True to request an italic font face.
	 * @return Name of the particular font face installed on the local host that is part of the specified font family 
	 * and satisfies as many of the specified style constraints as possible.
	 */
	@SuppressWarnings("rawtypes")
	private static String getStyleVariant(String family, boolean isBold, boolean isItalic)
	{
		List variants = (List) envFonts.get(family);
		if(variants == null) throw new IllegalStateException();
		
		// if the font family has a bold keyword, then we should ignore the bold style flag; similarly for italic flag
		boolean ignoreBold = hasKeyword(family, boldAbbrevs) || hasKeyword(family.toLowerCase(), boldKeywords);
		boolean ignoreItalic = hasKeyword(family, italicAbbrevs) || hasKeyword(family.toLowerCase(), italicKeywords);

		// extract plain, bold, italic, and bold-italic variants based on keyword search
		String plainVariant = null;
		String boldVariant = null;
		String italicVariant = null;
		String biVariant = null;
		for( int i=0; i<variants.size(); i++ )
		{
			String fontName = (String) variants.get(i);
			boolean bold = ignoreBold || hasKeyword( fontName.toLowerCase(), boldKeywords );
			boolean ital = ignoreItalic || hasKeyword( fontName.toLowerCase(), italicKeywords );

			if( bold && ital && biVariant == null ) biVariant = fontName;
			if( bold && !ital && boldVariant == null ) boldVariant = fontName;
			if( !bold && ital && italicVariant == null ) italicVariant = fontName;
			if( !bold && !ital && plainVariant == null ) plainVariant = fontName;
		}

		// return the variant which satisfies as many style constraints as possible.  if bold-italic variant is not 
		// available, use italic one 
		String result = null;
		if( isBold && isItalic )
		{
			if( biVariant != null ) result = biVariant;
			else if( italicVariant != null ) result = italicVariant;
			else if( boldVariant != null ) result = boldVariant;
			else if( plainVariant != null ) result = plainVariant;
		}
		else if( isBold )
		{
			if( boldVariant != null ) result = boldVariant;
			else if( plainVariant != null ) result = plainVariant;
			else if( biVariant != null ) result = biVariant;
		}
		else if( isItalic )
		{
			if( italicVariant != null ) result = italicVariant;
			else if( biVariant != null ) result = biVariant;
			else if( plainVariant != null ) result = plainVariant;
		}
		else 
		{
			result = plainVariant;
		}

		// return the best variant, or if we don't find one, return the first variant in the unconstrained variants list
		return( (result != null) ? result : (String) variants.get(0) ); 
	}

	/**
	 * Helper method checks whether or not the specified font name contains any of the specified keywords. The keywords 
	 * may or may not appear as individual words in the font name.
	 * 
	 * @param fontName The font name to be checked.
	 * @param keywords The list of keywords to look for.
	 * @return True if any keyword is found in the font name; false otherwise.
	 */
	private static boolean hasKeyword(String fontName, String[] keywords)
	{
		for(int i=0; i<keywords.length; i++) 
		{
			if(fontName.indexOf(keywords[i]) >= 0) return(true);
		}
		return(false);
	}

	/**
	 * Does the specified font face name contain any of the key words typically found in a bold font name?
	 * 
	 * @param fontFace The font face name, eg, from <b>Font.getFontName()</b>.
	 * @return True if font name contains a keyword suggesting it is a bold font.
	 */
	public static boolean isBoldFace(String fontFace)
	{
	   return fontFace != null && hasKeyword(fontFace.toLowerCase(), boldKeywords);
	}
	
   /**
    * Does the specified font face name contain any of the key words typically found in an italic font name?
    * 
    * @param fontFace The font face name, eg, from <b>Font.getFontName()</b>.
    * @return True if font name contains a keyword suggesting it is an italic font.
    */
	public static boolean isItalicFace(String fontFace)
	{
	   return fontFace != null && hasKeyword(fontFace.toLowerCase(), italicKeywords);
	}
	
	/**
	 * Return the style of the installed typeface in the specified font family that best matches the specified style.
	 * 
	 * @param family The font family 
	 * @param desiredStyle The desired style should be java.awt.Font.PLAIN, BOLD, ITALIC, or BOLD+ITALIC.
	 * @return The style of the installed typeface in the specified font family that best matches the specified style.
	 * If the font family is not installed on the local host, java.awt.Font.PLAIN is returned.
	 */
	@SuppressWarnings("rawtypes")
	public static int getBestMatchFontStyle(String family, int desiredStyle)
	{
		if(envFonts == null) initialize();
		List variants = (List) envFonts.get(family);
		if(variants == null) return(Font.PLAIN);

		// if the font family has a bold keyword, then we should ignore the bold style flag; similarly for italic flag
		boolean ignoreBold = hasKeyword(family, boldAbbrevs) || hasKeyword(family.toLowerCase(), boldKeywords);
		boolean ignoreItalic = hasKeyword(family, italicAbbrevs) || hasKeyword(family.toLowerCase(), italicKeywords);

		// extract plain, bold, italic, and bold-italic variants based on keyword search
		String plainVariant = null;
		String boldVariant = null;
		String italicVariant = null;
		String biVariant = null;
		for( int i=0; i<variants.size(); i++ )
		{
			String fontName = (String) variants.get(i);
			boolean bold = ignoreBold || hasKeyword( fontName.toLowerCase(), boldKeywords );
			boolean ital = ignoreItalic || hasKeyword( fontName.toLowerCase(), italicKeywords );

			if( bold && ital && biVariant == null ) biVariant = fontName;
			if( bold && !ital && boldVariant == null ) boldVariant = fontName;
			if( !bold && ital && italicVariant == null ) italicVariant = fontName;
			if( !bold && !ital && plainVariant == null ) plainVariant = fontName;
		}

		// return the best style match available
		int bestMatch = Font.PLAIN;
		if( desiredStyle == (Font.BOLD + Font.ITALIC) )
		{
			if( biVariant != null ) bestMatch = Font.BOLD + Font.ITALIC;
			else if( italicVariant != null ) bestMatch = Font.ITALIC;
			else if( boldVariant != null ) bestMatch = Font.BOLD;
		}
		else if( desiredStyle == Font.BOLD )
		{
			if( boldVariant != null ) bestMatch = Font.BOLD;
			else if( plainVariant != null ) bestMatch = Font.PLAIN;
			else if( biVariant != null ) bestMatch = Font.BOLD + Font.ITALIC;
		}
		else if( desiredStyle == Font.ITALIC )
		{
			if( italicVariant != null ) bestMatch = Font.ITALIC;
			else if( biVariant != null ) bestMatch = Font.BOLD + Font.ITALIC;
		}

		return(bestMatch);
	}

	/**
	 * For test/debug only.
	 * 
	 * <p>This program initializes the LocalFontEnvironment and dumps all installed font family names to the console. 
	 * If it cannot find a default physical font for any of the generic font classes, it will throw up a dialog asking 
	 * the user to choose one.</p>
	 * 
	 * @param 	args	the command-line arguments (NOT USED)
	 */
	public static void main(String[] args)
	{
		GUIUtilities.initLookAndFeel();
		LocalFontEnvironment.initialize();
		String[] installed = getInstalledFontFamilies();
		System.out.println( "Installed font families:" );
		for(int i=0; i<installed.length; i++)
		{
			System.out.println( "   " + installed[i] );
			System.out.println( "      plain=" + getStyleVariant(installed[i], false, false) );
			System.out.println( "      bold=" + getStyleVariant(installed[i], true, false) );
			System.out.println( "      italic=" + getStyleVariant(installed[i], false, true) );
			System.out.println( "      bolditalic=" + getStyleVariant(installed[i], true, true) );
		}
		System.out.println( "Generic font mappings:" );
		System.out.println( "   sans-serif=" + getSansSerifFont() );
		System.out.println( "   serif=" + getSerifFont() );
		System.out.println( "   monospace=" + getMonospaceFont() );

		Object[] charSetNames = Charset.availableCharsets().keySet().toArray();
		System.out.println( "Supported charsets:" );
		for( int i=0; i<charSetNames.length; i++ )
			System.out.println( charSetNames[i] );
	}
}
