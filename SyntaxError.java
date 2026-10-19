public class SyntaxError {
	public static final int UNKNOWN_SYNTAX = 1;
	public static final int OTHER = 2;
	public int type;
	public int lineNumber;
	public int characterOffset;
	public String message;

	SyntaxError(int type, int lineNumber, int characterOffset, String msg)
		{
		this.type = type;
		this.lineNumber = lineNumber;
		this.characterOffset = characterOffset;
		this.message = msg;
		}
}