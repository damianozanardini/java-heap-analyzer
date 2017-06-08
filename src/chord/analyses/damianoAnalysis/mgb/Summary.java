package chord.analyses.damianoAnalysis.mgb;

import chord.analyses.damianoAnalysis.Utilities;

public class Summary {

	private AbstractValue input;
	private AbstractValue output;
	
	/**
	 * this constructor has to be adapted to account for BDD implementation
	 */
	public Summary() {
		if (GlobalInfo.tupleImplementation()) {
			input = new TupleAbstractValue();
			output = new TupleAbstractValue();
		}
		if (GlobalInfo.bddImplementation()) {
			input = new BDDAbstractValue();
			output = new BDDAbstractValue();
		}
	}
	
	public Summary(AbstractValue i, AbstractValue o) {
		input = i;
		output = o;
	}
	
	public AbstractValue getInput() {
		return input;
	}
	
	public AbstractValue getOutput() {
		return output;
	}
	
	public boolean updateInput(AbstractValue a) {
		if (input==null) {
			input = a;
			return true;
		}
		else return input.update(a);
	}

	public boolean updateOutput(AbstractValue a) {
		if (output==null) {
			output = a;
			return true;
		}
		else return output.update(a);
	}

}
