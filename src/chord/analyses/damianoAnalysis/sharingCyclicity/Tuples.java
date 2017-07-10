package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.List;

import joeq.Compiler.Quad.RegisterFactory.Register;

public abstract class Tuples {
	
	public abstract void remove(Register r);
	
	public abstract Tuples clone();
	
	public abstract void filterActual(List<Register> actualParameters) ;
	
	public abstract String toString();
		
	public abstract boolean isBottom();
}
