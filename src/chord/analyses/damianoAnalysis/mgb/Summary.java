package chord.analyses.damianoAnalysis.mgb;

public class Summary {

	private AbstractValue input;
	private AbstractValue output;
	
	public Summary() { }
	
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
		if (input == null) {
			input = a;
			return true; // WARNING: actually, the new abstract value could be empty sometimes...
		} else return input.update(a);
	}

	public boolean updateOutput(AbstractValue a) {
		if (output == null) {
			output = a;
			return true; // WARNING: actually, the new abstract value could be empty sometimes...
		} else return output.update(a);
	}

}
