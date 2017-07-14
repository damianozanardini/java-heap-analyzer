package chord.analyses.damianoAnalysis.jgbHeap;

public class Summary {

	private AbstractValue input;
	private AbstractValue output;
	
	public Summary() {
		input = new AbstractValue();
		output = new AbstractValue();
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
		return input.update(a);
	}

	public boolean updateOutput(AbstractValue a) {
		return output.update(a);
	}

}
