package chord.analyses.damianoAnalysis.sharingCyclicity;

import javax.swing.InputMap;

import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.Utilities;


/**
 * This class is supposed to know which implementation(s) is currently
 * used, and manage summaries accordingly
 * 
 * @author damiano
 */
public class Summary {

	private AbstractValue input;
	private AbstractValue output;
	
	public Summary(Entry entry) {
		if (GlobalInfo.bothImplementations()) {
			input = new BothAbstractValue(entry);
			output = new BothAbstractValue(entry);
		} else if (GlobalInfo.tuplesImplementation()) {
			input = new TuplesAbstractValue(entry);
			output = new TuplesAbstractValue(entry);
		} else if (GlobalInfo.bddImplementation()) {
			input = new BDDAbstractValue(entry);
			output = new BDDAbstractValue(entry);
		}
	}
			
	public AbstractValue getInput() {
		return input;
	}
	
	public AbstractValue getOutput() {
		return output;
	}

	/**
	 * Updates the input component of the summary with the given abstract
	 * information.  Purity is not copied because it is not supposed to be passed
	 * to the invoked method.
	 *  
	 * @param a
	 * @return
	 */
	public boolean updateInput(AbstractValue a) {
		boolean b = input.updateNoPurity(a);
		//input.clearPurityInfo();
		return b;
	}
	
	public boolean updateOutput(AbstractValue a) {
		return output.update(a);
	}

}
