package chord.analyses.damianoAnalysis.jgbHeap;

public class ParseFieldException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final int FIELDNOTFOUND = 0;
	public static final int MULTIPLEFIELDS = 1;
	
	protected int code;
	protected String field;

	
	public ParseFieldException(int c, String str) {
		code = c;
		field = str;
	}
		
	public int getCode() {
		return code;
	}

	public String getField() {
		return field;
	}
}