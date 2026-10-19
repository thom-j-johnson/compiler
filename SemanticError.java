public class SemanticError {
	public static final int UNDECLARED_VARIABLE = 1;
	public static final int REDECLARED_VARIABLE = 2;
	public static final int TYPE_MISMATCH = 3;
	public static final int MISSING_RETURN = 4;
	public static final int OTHER = 5;
	public int type;
	public int lineNumber;
	public int characterOffset;
	public String message;

	SemanticError(int type, int lineNumber, int characterOffset, String msg)
		{
		this.type = type;
		this.lineNumber = lineNumber;
		this.characterOffset = characterOffset;
		this.message = msg;
		}
}