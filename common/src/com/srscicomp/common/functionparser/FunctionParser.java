package com.srscicomp.common.functionparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * FunctionParser represents a one-variable function f(x) that can contain many of the function operators supported by 
 * {@link Math} as well as the typical mathematical operators.  The function definition is passed to an
 * instance of FunctionParser in string form as a typical mathematical expression -- eg, "x + 2*x*sin( 100*pi*x )".  
 * The letter "x" -- or "X", since FunctionParser ignores case entirely -- represents the function's independent 
 * variable.  The function string is parsed and converted internally to a list of operand and operator tokens stored in 
 * postfix order (the grouping operators "(", ")", and "," are removed in the process).  If the parse succeeded, the 
 * function can then be evaluated for different values of the independent variable x.
 * 
 * <p>Here are the set of mathematical, function, and grouping operators supported by FunctionParser:
 * <ul>
 * 	<li>"+", "-", "*", "/", "^", "%": The typical binary operators.</li>
 * 	<li>"-" (unary):  The unary minus, or negate, operator is distinguished from the subtract operator by examining 
 * 	context in the function string.</li>
 * 	<li>"(", ")", ",":  Grouping operators.  The parentheses are used to clarify precedence or to delimit the arg 
 * 	list for a function operator.  The comma operator is valid only when used to separate the arguments of a function 
 * 	operator.</li>
 * 	<li>Function operators:  "sin", "cos", "tan", "asin", "acos", "atan", "atan2", "sqrt", "pow", "exp", "expm1", 
 * 	"log", "log1p", "log10", "abs", "floor", "ceil", "rint".  All supported operators take double-valued arguments 
 * 	and return a double-valued result.  Essentially, they are implemented by invoking like-named methods from {@link 
 *    Math}, except for "exp1m", "log1p", and "log10", which are computed indirectly using that library.</li>
 * </ul>
 * Standard operator precedence is enforced (listed here from highest to lowest precedence) and associativity is 
 * left to right:
 * <ul>
 * 	<li>All function operators</li>
 * 	<li>Negate (unary "-") operator</li>
 * 	<li>Raise-to-a-power ("^") binary operator</li>
 * 	<li>Multiply, divide, and remainder</li>
 * 	<li>Add, subtract</li>
 * 	<li>Grouping operators</li>
 * </ul>
 * </p>
 * 
 * <p>FunctionParser and its supporting classes are currently NOT extensible -- it is not possible to introduce new 
 * function operators in this framework, except by making suitable changes to the framework itself.</p>
 * 
 * <p>CREDITS:  Based on C code from the original <em>Phyplot</em> UNIX application, by Steve Lisberger.</p>
 * 
 * @author 	sruffner
 */
public final class FunctionParser {
	/**
	 * The original, unaltered string that describes the mathematical function evaluated
	 */
	private String functionDefinition = null;

	/**
	 * If the function definition string could not be parsed, this holds the index of the character at which the parser 
	 * failed.  Otherwise, it is -1.
	 */
	private int parseErrorPos = -1;

	/**
	 * If the function definition string could not be parsed, this holds the reason why the parser failed.  Otherwise, 
	 * it is an empty string.
	 */
	private String parseErrorReason = "";

	/**
	 * After parsing this contains the operands and operators that represent the function, in postfix order.
	 */
	private final List<Token> postFixFunction = new ArrayList<Token>(20);


	/**
	 * Construct a FunctionParser with an empty function definition.  
	 * 
	 * <p>An empty function is invalid and will always evaluate to {@link Double#NaN}.  Call {@link
	 * #setDefinition(String)} to change the function definition.</p>
	 */
	public FunctionParser()
	{
		this( "" );
	}

	/**
	 * Construct a FunctionParser with the specified function definition. 
	 * 
	 * @param 	strFcn string defining the function f(x) represented by this FunctionParser.
	 */
	public FunctionParser( String strFcn )
	{
		super();
		setDefinition( strFcn );
	}

	/**
	 * Set the function definition string represented by this FunctionParser and parse it.  If parsing succeeds, the 
	 * function can be evaluated for different values of the independent variable "x" by invoking {@link 
	 * FunctionParser#evaluate(double)}.  If parsing failed, the function will always evaluate
	 * to {@link Double#NaN}.  Call {@link FunctionParser#isValid()} to determine
	 * whether or not the new function definition is valid.
	 * 
	 * <p>The previous function definition is lost, even if the new definition is invalid.</p>
	 * 
	 * @param 	strFcn string defining the new function f(x) to be evaluated by this FunctionParser
	 */
	public void setDefinition( String strFcn )
	{
		functionDefinition = (strFcn==null) ? "" : strFcn;
		parse();
	}

	/**
	 * Get the original, unaltered function definition represented by this FunctionParser.  The character position that 
	 * is returned by getParseErrorPos() points to the character in this string at which the parser failed.
	 * 
	 * @see		FunctionParser#getParseErrorPos()
	 * @return	string defining the function represented by this FunctionParser
	 */
	public String getDefinition()
	{
		return( functionDefinition );
	}

	/**
	 * Does this FunctionParser currently contain a valid function definition?
	 * 
	 * @return	<code>true</code> if current function definition was successfully parsed
	 */
	public boolean isValid()
	{
		return(parseErrorReason.isEmpty());
	}

	/** 
	 * If the current function definition is invalid, this method returns the character position at which the parser 
	 * failed.
	 * 
	 * @return	character pos in function string at which parser failed; or -1 if parsing was successful OR if the 
	 * 	function string is empty
	 */
	public int getParseErrorPos()
	{
		return( parseErrorPos );
	}

	/** 
	 * If the current function definition is invalid, this method returns a brief explanation of why the parser failed.
	 * 
	 * @return	string describing why parser failed; an empty string if parsing was successful
	 */
	public String getParseErrorReason()
	{
		return( parseErrorReason );
	}

	/**
	 * Compute the value of the function f(x) currently represented by this FunctionParser for the specified value of 
	 * the independent variable "x".
	 * 
	 * @param 	x value of function argument 
	 * @return	the value of the function at the specified value of x.  If the function is invalid, the method returns 
	 * 	NaN for all values of x.  If x is NaN, NaN is returned.  Otherwise, the function value is computed.  Note that 
	 * 	the result could be positive or negative infinity, NaN, or a finite double value.
	 */
	public double evaluate( double x )
	{
		// special cases
		if( Double.isNaN( x ) || !isValid() ) return( Double.NaN );

		// operand stack holds intermediate results as we proceed through the function in postfix order
		Stack<Double> operands = new Stack<Double>();

		// evaluate function:  Operand values (NOT the operand tokens themselves) are pushed onto the operand stack in the 
		// order encountered.  When an operator is encountered, the required number of operand values are popped from the 
		// operand stack and supplied to the operator, and then the result is pushed back onto the operand stack.  At the 
		// end of this process, the function result should be the last remaining operand on the stack.  If an intermediate 
		// result is NaN, then we stop processing immediately and return NaN.  Note that we do not modify the postfix list 
		// representing the function -- so it can be reused over and over each time the method is invoked.
		double[] args = new double[2];
		for( int i=0; i<postFixFunction.size(); i++ )
		{
			Token t = (Token) postFixFunction.get(i);
			if( Operand.isOperand(t) )
			{
				Operand arg = (Operand)t;
				operands.push(arg.getValue(x));
			}
			else 
			{
				Operator op = (Operator)t;
				int nArgs = op.getNumOperands();
				if( operands.size() < nArgs )
					throw new RuntimeException( "DEBUG:  Malformed function.  Not enough operands for " + op.getToken() );
				for( int n=0; n<nArgs; n++ ) 
					args[n] = ((Double) operands.pop()).doubleValue();
				double result = op.evaluate( args, nArgs );
				if( Double.isNaN(result) ) return( result );
				operands.push(result);
			}
		}

		if( operands.size() != 1 ) 
			throw new RuntimeException( "DEBUG:  Malformed function.  Unexpected #operands left after computation" );

		return( ((Double) operands.pop()).doubleValue() );
	}


	/**
	 * This method is the heart of the parser.  It parses the current function string into operand and operator tokens, 
	 * and stores these tokens in postfix order for function evaluation.  If an error occurs while parsing, the postfix 
	 * token list is cleared -- and the function will always evaluate to {@link Double#NaN}.
	 */
	@SuppressWarnings("rawtypes")
	private void parse()
	{
		// reset state
		postFixFunction.clear();
		parseErrorPos = -1;
		parseErrorReason = "";

		// phase 1:  break up string into operator and operator tokens.  validate syntax -- abort if function string 
		// is not valid.
		List tokens = tokenize();
		if( tokens == null ) return;

		// phase 2:  put tokens into postfix order, eliminating all grouping operators (ie, parentheses and commas)
		Stack<Token> opStack = new Stack<Token>();
		for( int i=0; i<tokens.size(); i++ )
		{
			Token t = (Token) tokens.get(i);
			if( Operand.isOperand(t) )
			{
				// all operands are added to the postfix list in the order encountered
				postFixFunction.add( t );
			}
			else
			{
				// operators are either grouping operators or mathematical operators. 
				Operator currOp = (Operator) t;
				if( Operator.isLeftParenthesis(currOp) )
				{
					// a left parenthesis is pushed onto operator stack when encountered.  it is removed when matching 
					// right parenthesis is processed. 
					opStack.push( currOp );
				}
				else if( Operator.isRightParenthesis(currOp) || Operator.isComma(currOp) )
				{
					// the comma and right parenthesis are processed similarly.  we pop items off the operator stack and add 
					// them to the postfix list until the first left parenthesis is found.  if we're processing a comma, the 
					// left parenthesis is left on the stack -- else the later right parenthesis will not have a match!
					// observe that none of the grouping operators is added to the postfix list.
					boolean foundLeft = false;
					while( !(foundLeft || opStack.isEmpty()) )
					{
						Token pop = (Token) opStack.pop();
						if( Operator.isLeftParenthesis(pop) ) 
						{
							// if we're processing a comma, we need to push the left paren back on stack -- else the later 
							// right paren will not have a match!
							foundLeft = true;
							if( Operator.isComma(currOp) )
								opStack.push(pop);
						}
						else 
							postFixFunction.add( pop );
					}
					if( !foundLeft ) 
						throw new RuntimeException( "DEBUG: Encountered unmatched comma or right paren in parse()!" );
				}
				else
				{
					// for all non-grouping operators, we need to pop any operators with precedence greater than or equal 
					// to the current operator, then push the current operator onto the operator stack.  stop as soon as 
					// we encounter an operator with a lesser precedence
					while( !opStack.isEmpty() )
					{
						Operator o = (Operator) opStack.peek();
						if( o.getPrecedence() >= currOp.getPrecedence() )
							postFixFunction.add( (Token) opStack.pop() );
						else
							break;
					}
					opStack.push( currOp );
				}
			}
		}

		// if there are any operators left on the stack, add them to the postfix list 
		while( !opStack.isEmpty() )
			postFixFunction.add( (Token) opStack.pop() );
	}

	/**
	 * Parse the current function definition into a set of valid tokens, checking for any syntax errors.  The list of 
	 * tokens found is returned in the order encountered.  A "token" is a recognized instance of {@link Token} and is
	 * either a numeric operand, the "x" operand, the "pi" constant operand, or one of the supported operators.  Any
	 * whitespace in the function definition is ignored.
	 * 
	 * @return	the ordered list of operand and/or operator tokens that comprise the function definition.  If the 
	 * 	function definition is invalid in any way, internal error information is initialized accordingly and 
	 * 	<code>null</code> is returned.
	 */
	private List<Token> tokenize()
	{
		// special case:  empty string
		if(functionDefinition.isEmpty())
		{
			parseErrorReason = "Function string is empty";
			return( null );
		}

		// if there's an unmatched left or right parenthesis, abort
		int iUnmatched = findUnmatchedParenthesis( functionDefinition );
		if( iUnmatched >= 0 )
		{
			parseErrorPos = iUnmatched;
			parseErrorReason = "Unmatched parenthesis";
			return( null );
		}

		// the list of tokens found
		List<Token> tokens = new ArrayList<Token>(20);

		int len = functionDefinition.length();
		int iPos = 0;
		while( iPos < len )
		{
			// find next non-whitespace character
			while( iPos < len && Character.isWhitespace( functionDefinition.charAt(iPos) ) )
				++iPos;
			if( iPos >= len ) break;

			// remember start of next token, for error reporting
			int iTokenStart = iPos;
			Token nextToken = null;

			// identify next token
			char firstChar = functionDefinition.charAt(iPos);
			if( firstChar == '.' || Character.isDigit(firstChar) )
			{
				// if first character of next token is a decimal point or digit, then consume token as a number operand. 
				// the number can contain at most one decimal point.  scientific format "1.0e+3" is NOT supported.
				boolean gotDecimalPt = (firstChar == '.');
				String strNum = "" + firstChar;
				while( ++iPos < len )
				{
					char c = functionDefinition.charAt(iPos);
					if( !(c == '.' || Character.isDigit(c)) )
						break;
					if( c == '.' )
					{
						if( gotDecimalPt )
						{
							parseErrorPos = iTokenStart;
							parseErrorReason = "Number operand has multiple decimal points";
							return( null );
						}
						gotDecimalPt = true;
					}
					strNum += c;
				}

				// parse the number token.  this could fail if the token is merely a decimal point!
				double d = 0;
				try { d = Double.parseDouble(strNum); } 
				catch( NumberFormatException nfe )
				{
					parseErrorPos = iTokenStart;
					parseErrorReason = "Invalid number operand";
					return( null );
				}

				nextToken = new Operand(null, d);
			}
			else 
			{
				// otherwise, check to see if it is an operator or operand token
				// NOTE:  this search works correctly only if no two tokens share the same string name, and only if the 
				// supported token list Operator.TOKENS orders the tokens by descending string length!
				for( int i=0; i<Operator.TOKENS.length; i++ )
				{
					String token = Operator.TOKENS[i].getToken();
					int iTokenEnd = iPos + token.length();
					if( iTokenEnd <= len && token.equalsIgnoreCase( functionDefinition.substring(iPos, iTokenEnd) ) )
					{
						nextToken = Operator.TOKENS[i];
						iPos = iTokenEnd;
						break;
					}
				}
			}

			// if next token was not recognized, abort
			if( nextToken == null )
			{
				parseErrorPos = iTokenStart;
				parseErrorReason = "Unrecognized token";
				return( null );
			}

			// validate the token based on the identity of the previous token.
			Token prevToken = (Token) (tokens.isEmpty() ? null : tokens.get( tokens.size()-1 ));
			boolean valid = false;
			if( Operand.isOperand(nextToken) || Operator.isFunction(nextToken) )
			{
				valid = (prevToken==null) || Operator.isLeftParenthesis(prevToken) || Operator.isNegate(prevToken) || 
					Operator.isBinary(prevToken) || Operator.isComma(prevToken);
			}
			else if( Operator.isBinary(nextToken) )
			{
				valid = Operand.isOperand(prevToken) || Operator.isRightParenthesis(prevToken);

				// if not valid but the operator is "-", then replace with unary minus if it would be valid here
				if( (!valid) && Operator.isSubtract(nextToken) )
				{
					valid = (prevToken==null) || Operator.isLeftParenthesis(prevToken) || 
						Operator.isBinary(prevToken) || Operator.isComma(prevToken);
					if( valid ) 
						nextToken = Operator.NEGATE;
				}
			}
			else if( Operator.isLeftParenthesis(nextToken) )
			{
				valid = (prevToken==null) || Operator.isLeftParenthesis(prevToken) || Operator.isNegate(prevToken) || 
					Operator.isBinary(prevToken) || Operator.isComma(prevToken) || Operator.isFunction(prevToken);
			}
			else if( Operator.isRightParenthesis(nextToken) )
			{
				valid = Operand.isOperand(prevToken) || Operator.isRightParenthesis(prevToken);
				if( valid && !Operator.checkFunctionArgCount(tokens) )
				{
					parseErrorPos = iTokenStart;
					parseErrorReason = "Function op has wrong number of args";
					return( null );					
				}
			}
			else if( Operator.isComma(nextToken) )
			{
				valid = Operand.isOperand(prevToken) || Operator.isRightParenthesis(prevToken);
				if( valid && !Operator.isInsideFunction(tokens) )
				{
					parseErrorPos = iTokenStart;
					parseErrorReason = "Invalid comma";
					return( null );
					
				}
			}

			if( !valid ) 
			{
				parseErrorPos = iTokenStart;
				parseErrorReason = "Token not allowed at this position";
				return( null );
			}

			// add the token found to our token list
			tokens.add( nextToken );
		}

		// we're done.  last token must be an operand or a right parenthesis
		if(tokens.isEmpty())
		{
			parseErrorPos = 0;
			parseErrorReason = "No valid tokens found";
			return( null );
		}
		else
		{
			Token lastToken = (Token) tokens.get( tokens.size()-1 );
			if( !(Operand.isOperand(lastToken) || Operator.isRightParenthesis(lastToken)) )
			{
				parseErrorPos = functionDefinition.length() - 1;
				parseErrorReason = "Must end with an operand or )";
				return( null );
			}
		}

		return( tokens );
	}

	/**
	 * Check provided string for an unmatched left or right parenthesis.  If the string contains an unmatched right 
	 * parenthesis, it returns the index of that character in the string.  If it contains an unmatched left parenthesis, 
	 * it returns the index of the last character in the string.
	 * 
	 * @param 	s the string to check
	 * @return	index of character in string at which an unmatched parenthesis was detected.  -1 is returned if no 
	 * 	unmatched parentheses were found.
	 */
	private static int findUnmatchedParenthesis( String s )
	{
		// special case
		if( s == null ) return( -1 );

		int depth = 0;
		int iPos = 0;
		int len = s.length();

		while( iPos < len )
		{
			int nextLeftParen = s.indexOf( '(', iPos );
			if( nextLeftParen == -1 ) nextLeftParen = len;
			int nextRightParen = s.indexOf( ')', iPos );
			if( nextRightParen == -1 ) nextRightParen = len+1;

			if( nextRightParen < nextLeftParen )
			{
				if( depth == 0 ) return( nextRightParen );		// unmatched right parenthesis
				--depth;
				iPos = nextRightParen + 1;
			}
			else if( nextLeftParen < len )
			{
				++depth;
				iPos = nextLeftParen + 1;
			}
			else																// no parentheses left
				iPos = len;
		}

		return( (depth==0) ? -1 : len - 1 );						// check for unmatched left parenthesis
	}

}
