package com.srscicomp.common.ui;

import java.awt.Font;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>UnicodeSubset represents a range, or a set of disjoint ranges, of 16-bit characters from the Unicode standard.</p>
 * 
 * <p>The intent here is to provide a class which encapsulates a relatively small set of Unicode characters that are 
 * functionally related.  For example, the "Basic Latin" block in the Unicode specification includes letters, numbers, 
 * general punctuation marks, and a few mathematical operators.  But there are also Unicode blocks for "Number Forms", 
 * "General Punctuation", and "Mathematical Operators".  Thus, for presentation purposes, it might make sense to group 
 * the "Basic Latin" punctuation marks with the "General Punctuation" block, the "Basic Latin" digits 0-9 with "Number 
 * Forms", etc.  UnicodeSubset can be used to define such collections of Unicode characters.</p>
 * 
 * <p>Static instances of UnicodeSubset are provided for some of the more common character collections in the Unicode 
 * specification:  Latin letters (including accented letters, diagraphs like "ae", etc), the Greek alphabet, punctuation 
 * marks, arrows, mathematical symbols, and other symbols.  The character ranges for these collections were based on 
 * the <a href="http://www.unicode.org">Unicode 4.0 specification</a>.</p>
 *
 * @author 	sruffner
 */
public class UnicodeSubset extends Character.Subset
{
	/** 
	 * An empty Unicode subset.
	 */
	public static final UnicodeSubset EMPTY = new UnicodeSubset();

	/**
	 * Constant Unicode subset representing the set of basic uppercase and lowercase letters from the Unicode Basic Latin 
	 * code chart (0041-005A, 0061-007A hexadecimal), plus many accented letters and other special forms from the Latin-1 
	 * Supplement, Latin Extended-A, and Latin Extended-B code charts (00C0-024F).
	 */
	public static final UnicodeSubset LATIN_LETTERS = 
		new UnicodeSubset( "Latin Letters Plus", "\u0041\u005A\u0061\u007A\u00C0\u024F" );

	/**
	 * Constant Unicode subset representing the set of basic uppercase and lowercase letters from the Unicode Greek
	 * code chart (0391-039A, 03B1-03C9 hexadecimal), and a few others.
	 */
	public static final UnicodeSubset GREEK_LETTERS =
		new UnicodeSubset( "Greek Letters", "\u0391\u03A9\u03B1\u03C9\u03D1\u03D1\u03D5\u03D6\u03F5\u03F6" );

	/**
	 * Constant Unicode subset representing the set of punctuation marks found in the Unicode Basic Latin (0021-002F, 
	 * 003A-0040, 005B-0060 hexadecimal) and Latin-1 Supplement (00A1-00AC, 00AE-00BF) code charts, plus selected marks 
	 * from the General Punctuation code chart (2012-2027, 2030-2057).
	 */
	public static final UnicodeSubset PUNCTUATION = new UnicodeSubset( "General Punctuation", 
			"\u0021\u002F\u003A\u0040\u005B\u0060\u00A1\u00AC\u00AE\u00BF\u2012\u2027\u2030\u2057" );

	/**
	 * Constant Unicode subset representing the characters appearing in a hexadecimal number: 0-9, A-F, a-f.
	 */
	public static final UnicodeSubset HEXDIGITS = new UnicodeSubset("Hexadecimal Digits",
	      "\u0030\u0039\u0041\u0046\u0061\u0066");

	/**
	 * Constant Unicode subset representing numbers or number-like symbols, including the decimal digits in the Unicode 
	 * Basic Latin code chart (0030-0039 hexadecimal), numerical superscripts and vulgar fractions from the Latin-1 
	 * Supplement code chart (00B0, 00B2-00B3, 00B9, 00BC-00BE), plus the entire Number Forms code chart (2150-218F).
	 */
	public static final UnicodeSubset NUMBERS = new UnicodeSubset( "Numbers and Fractions",
			"\u0030\u0039\u00B0\u00B0\u00B2\u00B3\u00B9\u00B9\u00BC\u00BE\u2150\u218F" );

	/**
	 * Constant Unicode subset representing the union of the Unicode Currency Symbols and Letterlike Symbols code 
	 * charts (20A0-20CF, 2100-214F hexadecimal).
	 */
	public static final UnicodeSubset LETTER_SYMBOLS = 
			new UnicodeSubset( "Letterlike Symbols", "\u20A0\u20CF\u2100\u214F" );

	/**
	 * Constant Unicode subset representing the union of the Unicode Arrows, Supplemental Arrows-A, and 
	 * Supplemental Arrows-B code charts
	 */
	public static final UnicodeSubset ARROWS = 
			new UnicodeSubset( "Arrows", "\u2190\u21FF\u27F0\u27FF\u2900\u297F" );

	/**
	 * Constant Unicode subset representing the union of the Mathematical Operators and Supplemental Mathematical 
	 * Operators code charts, as well as some other math operator or symbol characters taken from the Basic Latin, 
	 * Latin-1 Supplement and Super/Subscript code charts.
	 */
	public static final UnicodeSubset MATHOPS = 
			new UnicodeSubset( "Math", 
				"\u0026\u002B\u002D\u002D\u002F\u002F\u0030\u003A\u003C\u003E\u005B\u005E\u007B\u007E" +
				"\u00AB\u00AC\u00B0\u00B4\u00B7\u00B7\u00B9\u00B9\u00BB\u00BE\u00D7\u00D8\u00F7\u00F8" +
				"\u2070\u208E\u2200\u22FF\u2A00\u2AFF" );

	/**
	 * Constant Unicode subset representing the majority of characters in the traditional Postscript Symbol font.
	 */
	public static final UnicodeSubset PS_SYMBOL =
			new UnicodeSubset( "Postscript Symbol Chars", 
				"\u0391\u03A1\u03A3\u03A9\u03B1\u03C9\u03D1\u03D1\u03D5\u03D6\u03F6\u03F6\u2032\u2033\u20AC\u20AC" +
				"\u2111\u2111\u2118\u2118\u211C\u211C\u2122\u2122\u2135\u2135\u2190\u2194\u21B5\u21B5\u21D0\u21D4" +
				"\u2200\u2200\u2202\u2203\u2205\u2205\u2207\u2209\u220F\u220F\u2211\u2211\u2219\u221A\u221D\u221E" +
				"\u2220\u2220\u2227\u222B\u2245\u2245\u2248\u2248\u2260\u2261\u2264\u2265\u2282\u2284\u2286\u2287" +
				"\u2295\u2295\u2297\u2297\u22A5\u22A5\u22C5\u22C5\u2320\u2321\u2329\u232A\u239B\u23AF\u23D0\u23D0" +
				"\u25CA\u25CA\u2660\u2660\u2663\u2663\u2665\u2666" );


	private String charRanges;

	/**
	 * Construct an empty UnicodeSubset.  It will have the name "Null set".
	 */
	public UnicodeSubset()
	{
		super("Null set");
		charRanges = "";
	}

	/**
	 * Construct a UnicodeSubset with a single character range.
	 * 
	 * @param 	name a human-readable descriptive name for this character subset
	 * @param 	from the first character in the range, inclusive
	 * @param 	to the last character in the range, inclusive (endpoints are swapped if they are reversed)
	 */
	public UnicodeSubset( String name, char from, char to ) 
	{
		super(name);
		StringBuffer buf = new StringBuffer();

		buf.append(from);
		buf.append(to);
		
		constructRanges(buf);
	}

	/**
	 * <p>Construct a UnicodeSubset with the specified human-readable name and the character ranges {start0, end0, 
	 * start1, end1, ..., } implied by the sequence of characters in the string provided.</p>
	 * 
	 * <p>The provided string of length 2N is treated as a series of N character pairs [startN, endN] defining the first 
	 * and last characters (inclusive) of a character range to be included in the definition of this character subset. 
	 * Any number of character ranges may be included, but keep in mind the memory and performance impacts of having 
	 * too many or very large ranges.  A single-character range is defined by equal endpoints (startN = endN).  If 
	 * startN > endN for any given range, the endpoints are swapped.  If an odd number of characters are in the string, 
	 * the last character is treated as a single-character range.  Any overlapping ranges are merged.</p>
	 * 
	 * @param 	name a human-readable name descriptive name for this character subset
	 * @param	characters a sequence of characters {start0, end0, ...} defining the character ranges in this subset; if 
	 * 	<code>null</code> or empty, the character subset will be empty
	 */
	public UnicodeSubset( String name, String characters ) 
	{
		super(name);
		if( characters == null )
			charRanges = "";
		else
			constructRanges( new StringBuffer(characters) );
	}

	/**
	 * Sets the character ranges defining this character set IAW the range endpoints stored in the character buffer 
	 * provided.  If the buffer has an odd number of characters, the last range is treated as a single-character range.
	 * Any intersecting ranges are merged, and all remaining ranges are sorted in ascending order.
	 * 
	 * @param 	b buffer defining the ranges of characters comprising this set: {start0, end0, start1, end1, ... }
	 */
	private void constructRanges( StringBuffer b )
	{
		// if the character buffer is empty, then it's an empty character set
		if( b == null || b.length() == 0 )
		{
			charRanges = "";
			return;
		}

		// if the character buffer has an odd number of characters, then we assume the range endpoint is missing for the 
		// last character range specified.  we duplicate the last character in the buffer, making it a 1-char range
		if( b.length() % 2 == 1 )
		{
			b.append( b.charAt( b.length()-1 ) );
			// required by jdk1.5.0
			// try { b.append( b.charAt( b.length()-1 ) ); } catch(IOException ioe) {}
		}

		// for each pair of characters (first,last) defining a character range, make sure that first <= last
		for( int i=0; i<b.length(); i+=2 )
		{
			char first = b.charAt(i);
			char last = b.charAt(i+1);
			if( first > last )
			{
				b.setCharAt(i,last);
				b.setCharAt(i+1,first);
			}
		}

		// sort the character ranges into ascending order
		for( int i=0; i<b.length(); i+=2 )
		{
			for( int j=i+2; j<b.length(); j+=2 )
			{
				if( b.charAt(i) > b.charAt(j) )
				{
					char first = b.charAt(i);
					char last = b.charAt(i+1);
					b.setCharAt(i, b.charAt(j));
					b.setCharAt(i+1, b.charAt(j+1));
					b.setCharAt(j, first);
					b.setCharAt(j+1, last);
				}
			}
		}

		// merge any intersecting ranges
		int i = 0;
		while( i < b.length() - 2 )
		{
			if( b.charAt(i+1) < b.charAt(i+2) )				// no intersection -- move to the next range
				i += 2;
			else														// combine ranges into one:
			{
				if( b.charAt(i+1) < b.charAt(i+3) )			//		be sure to keep the larger endpoint
					b.setCharAt(i+1, b.charAt(i+3) );
				b.delete(i+2,i+4);								//		get rid of the extra
			}
		}

		// save the sorted, merged character ranges internally as a string
		charRanges = b.toString();
	}

   /**
    * Get the total number of Unicode character codes in this <code>UnicodeSubset</code>.
    * 
    * @return Number of character codes.
    */
   public int getNumberOfChars()
   {
      int n = 0;
      int index = 0;
      while(index < charRanges.length())
      {
         char first = charRanges.charAt(index++);
         char last = charRanges.charAt(index++);
         n += ((int)last) - ((int) first) + 1;
      }
      return(n);
   }

   /**
    * Get an iterator over every Unicode character code in this Unicode subset.
    * @return The iterator. It does NOT support removal, since this Unicode subset is immutable.
    */
   public Iterator<Character> getCharIterator()
   {
      return(new Iterator<>() {


          public boolean hasNext() {
              return (iCurrChar < nChars);
          }

          public Character next() {
              if (iCurrChar >= nChars) throw new NoSuchElementException();

              int n = 0;
              int rngIdx = 0;
              while (rngIdx < charRanges.length()) {
                  char first = charRanges.charAt(rngIdx++);
                  char last = charRanges.charAt(rngIdx++);
                  int count = ((int) last) - ((int) first) + 1;
                  if (iCurrChar < n + count) {
                      first += (char) (iCurrChar - n);
                      ++iCurrChar;
                      return (first);
                  }
                  n += count;
              }
              throw new NoSuchElementException("index=" + iCurrChar);
          }

          public void remove() {
              throw new UnsupportedOperationException("Immutable; remove() not suppored.");
          }

          private int iCurrChar = 0;
          private final int nChars = getNumberOfChars();
      });
   }
   
   /**
	 * <p>Does this UnicodeSubset contain the specified character?</p>
	 * 
	 * <p><strong>Note</strong>:  The method does a simple linear search through the character ranges represented by 
	 * the UnicodeSubset.</p>
	 * 
	 * @param 	c the character to test
	 * @return	<code>true</code> if this UnicodeSubset includes the character
	 */
	public boolean contains( char c )
	{
		// since the character ranges are disjoint and assorted in ascending order, we simply find the first range for 
		// which the character is less than or equal to the last character of the range and then check that it is also 
		// greater than or equal to the start character in the range
		int index = 0;
		while( (index < charRanges.length()) && (c > charRanges.charAt(index+1)) ) index += 2;

		return(index != charRanges.length() && (c >= charRanges.charAt(index)));
	}

	/**
	 * Does this UnicodeSubset contain all the characters in the specified string?  This is merely a convenience 
	 * method that invokes {@link #contains(char)} on each character of the string.
	 * 
	 * @param 	str the character string to test
	 * @return	<code>true</code> if every character in the string is contained in this UnicodeSubset; returns 
	 * 	<code>true</code> if the string is <code>null</code> or empty
	 */
	public boolean containsAll( String str )
	{
		if( str == null ) return( true );
		for( int i=0; i<str.length(); i++ ) 
		{
			if( !contains( str.charAt(i) ) )
				return( false );
		}
		return( true );
	}

	/**
	 * Does this UnicodeSubset contain all of the specified characters?  This is merely a convenience method that 
	 * invokes {@link #contains(char) contains()} on each element of the character array.
	 * 
	 * @param 	characters the character array to test
	 * @return	<code>true</code> if every character in the array is contained in this UnicodeSubset; returns 
	 * 	<code>true</code> if the array is <code>null</code> or empty
	 */
	public boolean containsAll( char[] characters )
	{
		if( characters == null ) return( true );
        for (char character : characters) {
            if (!contains(character))
                return (false);
        }
		return( true );
	}

	/**
	 * Does this UnicodeSubset contain any of the characters in the specified string?  This is merely a convenience 
	 * method that invokes {@link #contains(char)} on each character of the string until it finds a character contained
	 * in this UnicodeSubset.
	 * 
	 * @param 	str the character string to test
	 * @return	<code>true</code> if at least one character in the string is contained in this UnicodeSubset; returns 
	 * 	<code>false</code> if the string is <code>null</code> or empty
	 */
	public boolean containsAny( String str )
	{
		if( str == null ) return( false );
		for( int i=0; i<str.length(); i++ ) 
		{
			if( contains( str.charAt(i) ) )
				return( true );
		}
		return( false );
	}

	/**
	 * Does this UnicodeSubset contain any of the specified characters?  This is merely a convenience method that 
	 * invokes {@link #contains(char)} on each character in the array until it finds a character contained in this
	 * UnicodeSubset.
	 * 
	 * @param 	characters the character array to test
	 * @return	<code>true</code> if at least one character in the array is contained in this UnicodeSubset; returns 
	 * 	<code>false</code> if the array is <code>null</code> or empty
	 */
	public boolean containsAny( char[] characters )
	{
		if( characters == null ) return( false );
        for (char character : characters) {
            if (contains(character))
                return (true);
        }
		return( false );
	}

	/**
	 * Return the specified string with any invalid characters -- ie, those not found in this UnicodeSubset -- replaced 
	 * by the specified character.
	 * 
	 * @param 	replaceC the replacement character; it does not have to be part of this UnicodeSubset
	 * @param 	s the string to be filtered
	 * @return	If no characters are replaced, the method merely returns a reference to the string argument.  Otherwise, 
	 * 	it returns a new string object with the replaced characters.  If the specified string is <code>null</code>, an 
	 * 	empty string is returned.
	 */
	public String replaceBadCharacters( char replaceC, String s )
	{
		if( s == null ) return( "" );
		char[] characters = s.toCharArray();
		boolean changed = false;
		for( int i=0; i<characters.length; i++ ) if( !contains(characters[i]) )
		{
			changed = true;
			characters[i] = replaceC;
		}

		if(changed)
		{
		   // if the replacement character was the null char, remove
		   String out = new String(characters);
		   return((replaceC == '\0') ? out.replaceAll("\0", "") : out);
		}
		else
		   return(s);
	}

   /**
    * Is this <code>UnicodeSubset</code> displayable in the specified font?
    * 
    * @param f The font to check.
    * @param strict If this flag is set, the test font must have a glyph for <em>every<em> character in the set. Else, 
    * it must have a glyph for <em>at least one</em> character in the set.
    * @return <code>True</code> iff font has a glyph for every character (<code>strict==true</code>) or at least one 
    * character (<code>strict==false</code>) of the set. If the set is empty or the font argument is <code>null</code>, 
    * the method returns <code>false</code>.
    */
   public boolean isDisplayableIn(Font f, boolean strict)
   {
      // if no font or if this is a null set, there's nothing to do
      if(f == null || charRanges.isEmpty()) return(false);

      int index = 0;
      while( index < charRanges.length() )
      {
         char first = charRanges.charAt(index++);
         char last = charRanges.charAt(index++);

         for(char c=first; c<=last; c++)
         {
            boolean canDo = f.canDisplay(c);
            if(strict && !canDo) return(false);
            else if(canDo && !strict) return(true);
         }
      }

      return(strict);
   }

   /**
	 * <p>Get the set of characters in this UnicodeSubset that are displayable using the specified font.</p>
	 * 
	 * @param 	f the font to test
	 * @return	character buffer containing all characters from this subset that can be displayed in the font; if the 
	 * 	font is <code>null</code>, an empty buffer is returned
	 */
	public StringBuffer getDisplayableCharactersIn( Font f )
	{
		StringBuffer displayable = new StringBuffer();
		buildDisplayableSetForFont( f, displayable );
		return( displayable );
	}

	/**
	 * <p>Get the set of characters in this UnicodeSubset that are displayable using the specified font.  This version 
	 * is provided in case callers want to reuse a character buffer rather than allocate a new one.</p>
	 * 
	 * @param	f	the font to test
	 * @param	displayable character buffer for storing all characters from this subset that can be displayed in the 
	 * 	font.  Previous contents of the buffer are lost.  If font is <code>null</code>, the buffer will be empty.
	 */
	public void getDisplayableCharactersIn( Font f, StringBuffer displayable )
	{
		buildDisplayableSetForFont( f, displayable );
	}

	/**
	 * Fill the provided character buffer with all characters from this UnicodeSubset that are displayable in the 
	 * given font.  Previous contents of the buffer are lost.
	 * 
	 * @see #getDisplayableCharactersIn(Font, StringBuffer)
	 */
	private void buildDisplayableSetForFont( Font f, StringBuffer displayable )
	{
		// first, empty the buffer
		if( displayable.length() > 0 ) 
			displayable.delete( 0, displayable.length() );

		// if no font or if this is a null set, there's nothing to do
		if( f == null || charRanges.isEmpty())
			return;

		// append all displayable characters to the buffer
		int index = 0;
		while( index < charRanges.length() )
		{
			char first = charRanges.charAt(index++);
			char last = charRanges.charAt(index++);

			for( char c=first; c<=last; c++ )
			{
				if( f.canDisplay( c ) ) 
				{
					displayable.append( c );
					// required by jdk1.5.0
					// try { displayable.append( c ); } catch(IOException ioe) {}
				} 
			}
		}
	}
}
