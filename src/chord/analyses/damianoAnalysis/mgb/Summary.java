package chord.analyses.damianoAnalysis.mgb;

import javax.swing.InputMap;

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

	/**
	 * Creates a new summary. If it is called from the Tuples implementation and
	 * both implementations are active, then "partial" BothAbstractValue objects
	 * are created
	 *  
	 * @param i
	 * @param o
	 */
	public Summary(TuplesAbstractValue i, TuplesAbstractValue o) {
		if (GlobalInfo.bothImplementations()) { // this is only part of the summary info
			input = new BothAbstractValue(i,null);
			output = new BothAbstractValue(o,null);
		} else {
			input = i;
			output = o;
		}
	}
	
	/**
	 * Creates a new summary. If it is called from the BDD implementation and
	 * both implementations are active, then "partial" BothAbstractValue objects
	 * are created
	 *  
	 * @param i
	 * @param o
	 */
	public Summary(BDDAbstractValue i, BDDAbstractValue o) {
		if (GlobalInfo.bothImplementations()) { // this is only part of the summary info
			input = new BothAbstractValue(null,i);
			output = new BothAbstractValue(null,o);
		} else {
			input = i;
			output = o;
		}
	}

	/**
	 * Creates a new summary. In this case, the only possibility is that both
	 * implementation are active.
	 * 
	 * @param i
	 * @param o
	 */
	public Summary(BothAbstractValue i, BothAbstractValue o) {
		input = i;
		output = o;
	}
	
	/**
	 * This constructor should never be executed
	 * 
	 * @param i (unused)
	 * @param o (unused)
	 */
	public Summary(AbstractValue i, AbstractValue o) {
		Utilities.warn("THIS CONSTRUCTOR SHOULD NEVER BE EXECUTED");
		input = null;
		output = null;
	}

	/**
	 * Returns the input in the form required by the caller. That is, the caller
	 * object is only used to see which analysis is calling this method
	 * 
	 * @param caller
	 * @return
	 */
	public AbstractValue getInput(TuplesAbstractValue caller) {
		if (GlobalInfo.bothImplementations()) {
			return ((BothAbstractValue) input).getTuplesPart();
		} else return input;
	}
	
	public AbstractValue getInput(BDDAbstractValue caller) {
		if (GlobalInfo.bothImplementations()) {
			return ((BothAbstractValue) input).getBDDPart();
		} else return input;
	}

	public AbstractValue getInput(BothAbstractValue caller) {
		return input;
	}

	public AbstractValue getInput(AbstractValue caller) {
		Utilities.warn("THIS METHOD SHOULD NEVER BE EXECUTED");
		return null;
	}

	public AbstractValue getInput() {
		return input;
	}

	/**
	 * Returns the output in the form required by the caller. That is, the caller
	 * object is only used to see which analysis is calling this method
	 * 
	 * @param caller
	 * @return
	 */
	public AbstractValue getOutput(TuplesAbstractValue caller) {
		if (GlobalInfo.bothImplementations()) {
			return ((BothAbstractValue) output).getTuplesPart();
		} else return output;
	}
	
	public AbstractValue getOutput(BDDAbstractValue caller) {
		if (GlobalInfo.bothImplementations()) {
			return ((BothAbstractValue) output).getBDDPart();
		} else return output;
	}

	public AbstractValue getOutput(BothAbstractValue caller) {
		return output;
	}

	public AbstractValue getOutput(AbstractValue caller) {
		Utilities.warn("THIS METHOD SHOULD NEVER BE EXECUTED");
		return null;
	}

	public AbstractValue getOutput() {
		return input;
	}

	public boolean updateInput(TuplesAbstractValue a) {
		if (GlobalInfo.bothImplementations()) {
			if (input==null) {
				input = new BothAbstractValue(a,null);
				return true;
			} else return input.update(new BothAbstractValue(a,null));
		} else {
			if (input==null) {
				input = a;
				return true;
			} else return input.update(a);
		}
	}
	
	public boolean updateInput(BDDAbstractValue a) {
		if (GlobalInfo.bothImplementations()) {
			if (input==null) {
				input = new BothAbstractValue(null,a);
				return true;
			} else return input.update(new BothAbstractValue(null,a));
		} else {
			if (input==null) {
				input = a;
				return true;
			} else return input.update(a);
		}
	}

	public boolean updateInput(BothAbstractValue a) {
		if (input==null) {
			input = a;
			return true;
		} else return input.update(a);
	}

	public boolean updateInput(AbstractValue a) {
		Utilities.warn("THIS METHOD SHOULD NEVER BE EXECUTED");
		return false;
	}

	public boolean updateOutput(TuplesAbstractValue a) {
		if (GlobalInfo.bothImplementations()) {
			if (output==null) {
				output = new BothAbstractValue(a,null);
				return true;
			} else return output.update(new BothAbstractValue(a,null));
		} else {
			if (output==null) {
				output = a;
				return true;
			} else return output.update(a);
		}
	}
	
	public boolean updateOutput(BDDAbstractValue a) {
		if (GlobalInfo.bothImplementations()) {
			if (output==null) {
				output = new BothAbstractValue(null,a);
				return true;
			} else return output.update(new BothAbstractValue(null,a));
		} else {
			if (output==null) {
				output = a;
				return true;
			} else return output.update(a);
		}
	}

	public boolean updateOutput(BothAbstractValue a) {
		if (output==null) {
			output = a;
			return true;
		} else return output.update(a);
	}

	public boolean updateOutput(AbstractValue a) {
		Utilities.warn("THIS METHOD SHOULD NEVER BE EXECUTED");
		return false;
	}

}
