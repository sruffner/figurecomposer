package com.srscicomp.common.functionparser;

/**
 * Operand represents any instance of an operand token in a parsable function string "f(x)" encapsulated by 
 * {@link FunctionParser}.  It includes static instances representing the independent variable
 * "x" and the mathematical constant PI.
 * 
 * @author 	sruffner
 */
final class Operand extends Token
{
	private double value;

	/**
	 * Construct an instance of Operand representing the independent variable "x".
	 */
	Operand()
	{
		super("x");
		value = Double.NaN;
	}

	/**
	 * Construct an operand token having the specified token string and value.
	 * 
	 * @param 	token the string fragment which represents this Operand in a parsable function string
	 * @param 	d the value of this operand
	 * @throws	IllegalArgumentException if the specified value is not a number
	 */
	Operand( String token, double d ) throws IllegalArgumentException
	{
		super( token );
		if( Double.isNaN(value) ) throw new IllegalArgumentException("Not a valid value");
		this.value = d;
	}

	/**
	 * Return the value of this operand. 
	 * 
	 * @param 	x the current value of the function's independent variable "x".  If this Operand instance represents 
	 * 	that variable, it always returns this value.  Otherwise, it returns the value specified at construction time.
	 * @return	operand's value
	 */
	double getValue( double x )
	{
		return( Double.isNaN(value) ? x : value );
	}

	/**
	 * Does the parsed function token represent an operand?
	 * 
	 * @param 	t the token to check
	 * @return	<code>true</code> if the token is an Operand instance
	 */
	static boolean isOperand( Token t ) 
	{
		return( t instanceof Operand );
	}

	/**
	 * An Operand instance that stands in for the independent variable "x".
	 */
	static final Operand X = new Operand();

	/**
	 * An Operand instance that stands in for the constant PI.  It always returns the value {@link Math#PI}.
	 */
	static final Operand PI = new Operand("pi", Math.PI);
}
