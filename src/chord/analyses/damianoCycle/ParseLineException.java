package chord.analyses.damianoCycle;

public class ParseLineException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected String line;
	
	public ParseLineException() {
		line = "";
	}
	
	public ParseLineException(String x) {
		line = x;
	}
	
	public String getLine() {
		return line;
	}
}