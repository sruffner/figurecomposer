package com.srscicomp.common.ui;

/**
 * A very simple wrapper for a primitive character value that -- unlike {@link Character} -- is mutable.
 * 
 * @author 	sruffner
 */
public class MutableCharacter
{
	private char value;

	/**
	 * Construct a MutableCharacter object representing the specified character.
	 * 
	 * @param 	c the character value
	 */
	public MutableCharacter( char c )
	{
		value = c;
	}

	/**
	 * Get the character value encapsulated by this MutableCharacter object.
	 * 
	 * @return	the character value
	 */
	public char getCharValue()
	{
		return( value );
	}

	/**
	 * Set the character value encapsulated by this MutableCharacter object.
	 * 
	 * @param c The character value.
	 */
	public void setCharValue( char c )
	{
		value = c;
	}

	/**
	 * Returns the character value as a one-character string.
	 * 
	 * @return Value of this MutableCharacter object in string form
	 */
	public String toString()
	{
		return( Character.toString(value) );
	}
}
