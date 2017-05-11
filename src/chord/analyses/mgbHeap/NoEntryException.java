package chord.analyses.mgbHeap;

public class NoEntryException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String errorMessage;
	
	public NoEntryException(String str) {
		errorMessage = str;
	}

	public String toString() {
		return errorMessage;
	}
	
}
