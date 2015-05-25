package chord.analyses.damianoAnalysis;

public class ParseInputLineException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	protected String line;
	
	public ParseInputLineException() {
		line = "";
	}
	
	public ParseInputLineException(String x) {
		line = x;
	}
	
	public String getLine() {
		return line;
	}
}