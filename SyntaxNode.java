public class SyntaxNode {
	public static final int PROGRAM = 1;
	public static final int FUNCTION_LIST = 2;
	public static final int FUNCTION = 3;
	public static final int PARAMETER_LIST = 4;
	public static final int PARAMETER = 5;
	public static final int STATEMENT_LIST = 6;
	public static final int STATEMENT = 7;
	public static final int IF = 8;
	public static final int WHILE = 9;
	public static final int RETURN = 10;
	public static final int DECLARE = 11;
	public static final int ASSIGN = 12;
	public static final int EXPRESSION = 13;
	public static final int MATH = 14;
	public static final int MATH_ADD = 15;
	public static final int MATH_SUBTRACT = 16;
	public static final int TERM_MULTIPLY = 17;
	public static final int TERM_DIVIDE = 18;
	public static final int COMPARISON = 19;
	public static final int TERM = 20;
	public static final int FACTOR = 21;
	public static final int IDENTIFIER = 22;
	public static final int TYPE_NAME = 23;
	public static final int NUMBER = 24;
	public static final int STRING = 25;

	public int type;
	public int lineNumber;
	public int characterOffset;
	public String token;
	SyntaxNode children[];

	public SyntaxNode(int type, int lineNumber, int characterOffset, String token)
		{
		this.type = type;
		this.lineNumber = lineNumber;
		this.characterOffset = characterOffset;
		this.token = token;
		}

	public String toString()
		{
		//you are permitted to modify this function implementation
		
		if (type == IDENTIFIER)
			return "IDENTIFIER [" + token + "]";
		
		return "UNKNOWN";
		}
}