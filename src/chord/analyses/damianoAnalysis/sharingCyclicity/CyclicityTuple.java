package chord.analyses.damianoAnalysis.sharingCyclicity;

import chord.util.tuple.object.Pair;
import joeq.Compiler.Quad.RegisterFactory.Register;

public class CyclicityTuple extends Tuple {

	Pair<Register,FieldSet> elem;

	public CyclicityTuple(Register r, FieldSet fs) {
		elem = new Pair<Register,FieldSet>(r,fs);
	}
	
	public Register getR() { return elem.val0; }
	
	public FieldSet getFs() { return elem.val1; }

	public void setR(Register r) {
		elem.val0 = r;
	}
	
	public void setFs(FieldSet fs) {
		elem.val1 = fs;
	}

}
