package com.srscicomp.common.functionparser;

/**
 * Token is the base class for any valid token in a parsable function string encapsulated by 
 * {@link FunctionParser}.
 * 
 * @author 	sruffner
 */
class Token
{
	private final String token;

	/**
	 * Construct a Token represented by the specified string, which cannot contain any whitespace.
	 * 
	 * @param 	s the string fragment that represents the token in function string
	 * @throws 	IllegalArgumentException if the string fragment has any whitespace
	 */
	Token( String s ) throws IllegalArgumentException
	{
		token = (s==null) ? "" : s;
		for( int i=0; i<token.length(); i++ )
		{
			if( Character.isWhitespace( token.charAt(i) ) ) 
				throw new IllegalArgumentException( "Function token cannot have whitespace" );
		}
	}

	/**
	 * Get the string form for the token.
	 * 
	 * @return	the string fragment that represents this Token in a parsable function string
	 */
	String getToken()
	{
		return( token );
	}
}
