public class LexToken {
	public static final int IDENTIFIER = 1;
	public static final int TYPE_NAME = 2;
	public static final int LITERAL_NUMBER = 3;
	public static final int LITERAL_STRING = 4;
	public static final int SYNTAX_TOKEN = 5;
	public static final int KEYWORD = 6;
	public static final int UNKNOWN = 7;
	public int type;
	public int lineNumber;
	public int characterOffset;
	public String token;

	public LexToken()
		{
		
		}

	public LexToken(int type, int lineNumber, int characterOffset, String token)
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