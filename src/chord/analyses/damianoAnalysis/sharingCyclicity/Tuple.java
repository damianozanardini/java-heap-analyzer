package chord.analyses.damianoAnalysis.sharingCyclicity;

import chord.analyses.damianoAnalysis.Utilities;

/**
 * Generic container for abstract information in a tuple-based static analysis.
 * 
 * @author damiano
 *
 */
public abstract class Tuple implements Comparable {

	private Object elem;
	
	public Object getElem() {
		return elem;
	}
	
	public abstract Tuple clone();
	
}
