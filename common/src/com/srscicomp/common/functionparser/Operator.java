package com.srscicomp.common.functionparser;

import java.util.List;

import com.srscicomp.common.util.Utilities;

/**
 * Operator represents any instance of an operator token in a parsable function string "f(x)" encapsulated by 
 * {@link FunctionParser}.  It includes static instances representing the base set of
 * operators supported by FunctionParser. 
 * 
 * @see		FunctionParser
 * @author 	sruffner
 */
final class Operator extends Token
{
	private final int nOperands;
	private final int precedence;
	private final boolean isFunction;

	/**
	 * Construct an Operator representing by the specified token, taking the specified number of operands, and having 
	 * the specified precedence.
	 * 
	 * @param 	token the string fragment which represents this operator in a parsable function string
	 * @param 	nOperands the number of operands consumed by this operator
	 * @param 	precedence the operator's precedence (larger number means higher precedence)
	 * @param 	isFunction	<code>true</code> if the operator is a function operator
	 * @throws	IllegalArgumentException if #operands less than or equal to zero.  Functions taking zero arguments are 
	 * 	not supported!
	 */
	Operator( String token, int nOperands, int precedence, boolean isFunction ) throws IllegalArgumentException
	{
		super( token );
		if( nOperands <= 0 ) throw new IllegalArgumentException( "Number of operands <= 0" );
		this.nOperands = nOperands;
		this.precedence = precedence;
		this.isFunction = isFunction;
	}

	/**
	 * Get the number of operands consumed by this Operator.
	 * 
	 * @return	number of operands
	 */
	int getNumOperands()
	{
		return( nOperands );
	}

	/**
	 * Get this operator's precedence.  A larger value indicates a higher precedence.  This class uses the following 
	 * precedences for the base set of available operators:
	 * <ul>
	 * 	<li>0:  grouping operators (parentheses, comma)</li>
	 * 	<li>1:  add,subtract</li>
	 * 	<li>2:  multiply,divide,remainder</li>
	 * 	<li>3:  raise to a power ("^")</li>
	 * 	<li>4:  unary minus (ie, negate)</li>
	 * 	<li>5:  functions</li>
	 * </ul>
	 * 
	 * @return	operator precedence
	 */
	int getPrecedence()
	{
		return( precedence );
	}

	/**
	 * This method handles the evaluation of any of the static operators in the base set defined by class Operator.  For 
	 * any other operator, this method returns {@link Double#NaN}.
	 * 
	 * <p>The operators are implemented in postfix notation, and the arguments must be provided in postfix order.  Thus, 
	 * for example:
	 * 	<li>x - 2 ==> x 2 - ==> args[0] = 2, args[1] = x.</li>
	 * 	<li>pow(x,10) ==> x 10 pow ==> arg[0] = 10, args[1] = x.</li>
	 * </p>
	 * @param 	args the arguments to the operator, LISTED IN POSTFIX ORDER, the first argument being at the top of the 
	 * 	postfix stack.
	 * @param 	nArgs number of arguments that are valid in the argument list provided
	 * @return	the result of applying the operator to the specified arguments in postfix order.  For some operators, 
	 * 	the result could be infinite or undefined. 
	 * @throws	IllegalArgumentException if the number of arguments supplied does not match the number required for this 
	 * 	operator
	 */
	double evaluate( double[] args, int nArgs ) throws IllegalArgumentException
	{
		if( args == null || args.length < nArgs || nArgs != getNumOperands() )
			throw new IllegalArgumentException("Incorrect number of operands supplied");

		double result = Double.NaN;
		if( this.equals(ADD) )
			result = args[1] + args[0];
		else if( this.equals(SUBTRACT) )
			result = args[1] - args[0];
		else if( this.equals(NEGATE) )
			result = -args[0];
		else if( this.equals(MULTIPLY) )
			result = args[1] * args[0];
		else if( this.equals(DIVIDE) )
			result = args[1] / args[0];
		else if( this.equals(POWER) || this.equals(POW) )
			result = Math.pow( args[1], args[0] );
		else if( this.equals(MODULO) )
			result = args[1] % args[0];
		else if( this.equals(SIN) )
			result = Math.sin( args[0] );
		else if( this.equals(COS) )
			result = Math.cos( args[0] );
		else if( this.equals(TAN) )
			result = Math.tan( args[0] );
		else if( this.equals(ASIN) )
			result = Math.asin( args[0] );
		else if( this.equals(ACOS) )
			result = Math.acos( args[0] );
		else if( this.equals(ATAN) )
			result = Math.atan( args[0] );
		else if( this.equals(ATAN2) )
			result = Math.atan2( args[1], args[0] );
		else if( this.equals(SQRT) )
			result = Math.sqrt( args[0] );
		else if( this.equals(EXP) )
			result = Math.exp( args[0] );
		else if( this.equals(EXPM1) )
			result = Math.exp( args[0] - 1.0 );
		else if( this.equals(LOG) )
			result = Math.log( args[0] );
		else if( this.equals(LOG1P) )
			result = Math.log( args[0] + 1.0 );
		else if( this.equals(LOG10) )
			result = Utilities.log10( args[0] );
		else if( this.equals(ABS) )
			result = Math.abs( args[0] );
		else if( this.equals(FLOOR) )
			result = Math.floor( args[0] );
		else if( this.equals(CEIL) )
			result = Math.ceil( args[0] );
		else if( this.equals(RINT) )
			result = Math.rint( args[0] );

		return( result );
	}

	/**
	 * If the next token in an incompletely parsed function string is a right parenthesis, use this method to validate 
	 * the number of operands WHEN the right parenthesis and its matching left parenthesis delimit the argument list of 
	 * a function operator.  The method searches backward through the provided token list for the first unmatched left 
	 * parenthesis, incrementing the arg count (starting at one) each time a comma operator is encountered at zero 
	 * depth.  Then, IF the token preceding that left parenthesis is a function operator, the method checks the observed 
	 * arg count against that function operator's required number of operands.
	 *  
	 * @param 	tokens list of operand/operator tokens representing an incompletely parsed function string; the 
	 * 	list is NOT modified by this method.
	 * @return	<code>false</code> if the number of operands encountered counting back to the first unmatched left 
	 * 	parenthesis is NOT equal to the number of operands required by the function operator immediately preceding 
	 * 	that left parenthesis.  Returns <code>true</code> if the #operands is correct, if the operator preceding the 
	 * 	left parenthesis is NOT a function operator, or if the unmatched left parenthesis was not found.
	 */
	@SuppressWarnings("rawtypes")
	static boolean checkFunctionArgCount( List tokens )
	{
		int depth = 0;
		int nArgs = 0;
		int pos = tokens.size() - 1;
		while( pos >= 0 )
		{
			Token t = (Token) tokens.get(pos);
			if( Operator.isRightParenthesis(t) ) --depth;
			else if( Operator.isComma(t) && (depth == 0) ) ++nArgs;
			else if( Operator.isLeftParenthesis(t) )
			{
				if( depth == 0 ) 
				{
					--pos;		// move to token before the left parenthesis, which MIGHT be a function
					break;
				} 
				else ++depth;
			}
			else if( nArgs == 0 ) nArgs = 1;
						
			--pos;
		}

		if( pos >= 0 )
		{
			Token t = (Token) tokens.get(pos);
			return( !Operator.isFunction(t) || ((Operator)t).getNumOperands() == nArgs );
		}

		return( true );
	}

	/**
	 * Use this method to test whether or not a list of parsed tokens represents a state such that the next token 
	 * appended to the list will be inside the parentheses that delimit the argument list of a function operator.
	 * The method searches backward through the provided token list for the first unmatched left parenthesis.  If 
	 * the token preceding this left parenthesis is a function operator, then the method returns <code>true</code>. 
	 *  
	 * @param 	tokens list of operand/operator tokens representing an incompletely parsed function string; the 
	 * 	list is NOT modified by this method.
	 * @return	<code>true</code> if the incomplete token list represents the state of being inside a function 
	 * 	operator's argument list
	 */
	@SuppressWarnings("rawtypes")
	static boolean isInsideFunction( List tokens )
	{
		int depth = 0;
		int pos = tokens.size() - 1;
		while( pos >= 0 )
		{
			Token t = (Token) tokens.get(pos);
			if( Operator.isRightParenthesis(t) ) --depth;
			else if( Operator.isLeftParenthesis(t) )
			{
				if( depth == 0 ) 
				{
					--pos;		// move to token before the left parenthesis, which is what we need to check
					break;
				} 
				else ++depth;
			}
			--pos;
		}

		return( (pos >= 0) && Operator.isFunction((Token) tokens.get(pos)) );
	}

	/**
	 * Is this a function operator?
	 * 
	 * @return	<code>true</code> if this is a function operator
	 */
	boolean isFunction()
	{
		return( isFunction );
	}


	static boolean isOperator( Token t )
	{
		return( t instanceof Operator );
	}

	static boolean isFunction( Token t )
	{
		return( (t instanceof Operator) && ((Operator)t).isFunction() );
	}

	static boolean isBinary( Token t )
	{
		boolean isBinary = (t instanceof Operator);
		if( isBinary ) 
		{
			Operator o = (Operator) t;
			isBinary = !o.isFunction;
			if( isBinary ) isBinary = (o.getNumOperands() == 2);
		}
		return( isBinary );
	}

	static boolean isNegate( Token t )
	{
		return( NEGATE.equals(t) );
	}

	static boolean isSubtract( Token t )
	{
		return( SUBTRACT.equals(t) );
	}

	static boolean isComma( Token t )
	{
		return( COMMA.equals(t) );
	}

	static boolean isLeftParenthesis( Token t )
	{
		return( LEFTPAREN.equals(t) );
	}

	static boolean isRightParenthesis( Token t )
	{
		return( RIGHTPAREN.equals(t) );
	}


	static final Operator ADD = new Operator("+", 2, 1, false);
	static final Operator SUBTRACT = new Operator("-", 2, 1, false);
	static final Operator NEGATE = new Operator("-", 1, 4, false);
	static final Operator MULTIPLY = new Operator("*", 2, 2, false);
	static final Operator DIVIDE = new Operator("/", 2, 2, false);
	static final Operator POWER = new Operator("^", 2, 3, false);
	static final Operator MODULO = new Operator("%", 2, 2, false);
	static final Operator LEFTPAREN = new Operator("(", 1, 0, false);		// note: the grouping operators are never evaluated!
	static final Operator RIGHTPAREN = new Operator(")", 1, 0, false);
	static final Operator COMMA = new Operator(",", 1, 0, false);
	static final Operator SIN = new Operator("sin", 1, 5, true);
	static final Operator COS = new Operator("cos", 1, 5, true);
	static final Operator TAN = new Operator("tan", 1, 5, true);
	static final Operator ASIN = new Operator("asin", 1, 5, true);
	static final Operator ACOS = new Operator("acos", 1, 5, true);
	static final Operator ATAN = new Operator("atan", 1, 5, true);			// careful -- when parsing look for longest possible match!
	static final Operator ATAN2 = new Operator("atan2", 2, 5, true);
	static final Operator SQRT = new Operator("sqrt", 1, 5, true);
	static final Operator POW = new Operator("pow", 2, 5, true);
	static final Operator EXP = new Operator("exp", 1, 5, true);			// careful -- when parsing look for longest possible match!
	static final Operator EXPM1 = new Operator("expm1", 1, 5, true);
	static final Operator LOG = new Operator("log", 1, 5, true);			// careful -- when parsing look for longest possible match!
	static final Operator LOG1P = new Operator("log1p", 1, 5, true);
	static final Operator LOG10 = new Operator("log10", 1, 5, true);
	static final Operator ABS = new Operator("abs", 1, 5, true);
	static final Operator FLOOR = new Operator("floor", 1, 5, true);
	static final Operator CEIL = new Operator("ceil", 1, 5, true);
	static final Operator RINT = new Operator("rint", 1, 5, true);

	/**
	 * The set of operand and operator tokens supported by FunctionParser (other than numeric operands).  The 
	 * negate, or unary minus operator is not listed here because it has the same string token ("-") as the subtract 
	 * operator.  The parser determines which operator to use based on context.
	 * <p>
	 * IMPORTANT:  The string tokens for ATAN and ATAN2 only differ in that the ATAN2 token is one character longer.
	 * The token matching algorithm stops as soon as it finds a match.  So ATAN2 in the function string will be 
	 * mistaken for ATAN unless the ATAN2 operator is listed first here.  The same issue holds for the EXP and LOG 
	 * operators.  As a general principle, we order the tokens here by descending token length to avoid this problem.
	 */
	static final Token[] TOKENS =
	{
		Operator.ATAN2, Operator.EXPM1, Operator.LOG1P, Operator.LOG10, Operator.FLOOR,
		Operator.ASIN, Operator.ACOS, Operator.ATAN, Operator.SQRT, Operator.CEIL, Operator.RINT,
		Operator.SIN, Operator.COS, Operator.TAN, Operator.POW, Operator.EXP, Operator.LOG, Operator.ABS, 
		Operand.PI, 
		Operand.X, Operator.ADD, Operator.SUBTRACT, Operator.MULTIPLY, Operator.DIVIDE, Operator.POWER, Operator.MODULO, 
		Operator.LEFTPAREN, Operator.RIGHTPAREN, Operator.COMMA
	};

}
