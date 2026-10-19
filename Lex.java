/*
logic order:
1. Postition tracking --> keep index into the input string, plus current line number, if \n incremenet line and reset column, record line and offset at moment a token starts
2. Main loop -->
	a. if character is whitespace, skip
	b. if character is ", enter string mode
	c. if its a digit, start a number
	d. if its a letter, start a word
	e. if its a syntax-token char, start a syntax token
	f. recordd an error or unknown token if none above applies
3. Accumulate before classifying --> chars into "current token" until a separator ends it, the decide what it is
	a. words: collect letters and digits, avoid if chains
	b. numbers: collect digits, if see '.' followed by digit, assume fractional
4. Maximal munch for syntax token
	On = peak at next character, if its = emit ==, otherwise emit =.
	*, /, +, -, (, ), [, ], ;, , and . are single-character tokens, though . also needs the number lookahead above.
5. Separators end tokens
	even if no whitespace, if hit syntax-token, emit what is collected.
6. String literal modde:
	On quote, enter a mode where no other rules apply.
7. Errors without stopping
	getErrors returns list, continue to scan after error where possible.
	Each error needs type from the LexError constants and an offset pointing at t
*/

public class Lex {
	String stream;
	private String[] KEYWORD = { "if", "else", "endif", "while", "endw", "endf", "return"};
	private String[] TYPE_NAME = {"int", "float", "string", "bool"};

	//helper functions
	public static boolean isWhiteSpace(char c)
	{
		return c == ' '|| c == '\t' || c == '\n' || c == '\r';
	}

	public static boolean isDigit(char c)
	{
		return c >= '0' && c <= '9';
	}

	public static boolean isLetter(char c)
	{
		return (c >= 'a' && c <='z')||(c >='A' && c <= 'Z');
	}

	public static boolean isSyntaxChar(char c)
	{
		switch (c) {
			case '=': case '*': case '/': case '+': case '-':
			case '(': case ')': case'[': case']':
			case';': case',': case'.':
				return true;
			default:
				return false;
		}
	}

	public boolean isKeyword(String token)
	{
		for (String keyword: KEYWORD)
		{
			if (keyword.equal(token))
			{
				return true;
			}
		}
		return false;
	}

	public boolean isTyping(String token)
	{
		for (String type: TYPE_NAME)
		{
			if (type.equal(token))
			{
				return true;
			}
		}
		return false;
	}


	Lex(String input)
	{
		stream = input;
		
	}
	
	//return an ordered array off the lexical tokens that were found in the input token
	//
	LexToken[] getTokens()
	{
		return null;
	}
	
	LexError[] getErrors()
	{
		return null;
	}
}
