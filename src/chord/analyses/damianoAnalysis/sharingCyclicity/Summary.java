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

	public boolean updateInput(AbstractValue a) {
		return input.update(a);
	}
	
	public boolean updateOutput(AbstractValue a) {
		return output.update(a);
	}

}
