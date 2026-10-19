public class LexError {
	public static final int UNKNOWN_NUMBER_FORMAT = 1;
	public static final int UNKNOWN_STRING_FORMAT = 2;
	public static final int UNKNOWN_ESCAPE_CHARACTER = 3;
	public static final int OTHER = 4;
	public int type;
	public int lineNumber;
	public int characterOffset;
	public String message;

	LexError(int type, int lineNumber, int characterOffset, String msg)
		{
		this.type = type;
		this.lineNumber = lineNumber;
		this.characterOffset = characterOffset;
		this.message = msg;
		}
}
