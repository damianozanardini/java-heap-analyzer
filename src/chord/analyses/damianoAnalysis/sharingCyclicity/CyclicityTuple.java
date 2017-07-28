package chord.analyses.damianoAnalysis.sharingCyclicity;

import chord.analyses.damianoAnalysis.Utilities;
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

	public int compareTo(Object other) {
		if (other instanceof CyclicityTuple) {
			CyclicityTuple other2 = (CyclicityTuple) other;
			Register ra = getR();
			Register rb = other2.getR();
			FieldSet fsa = getFs();
			FieldSet fsb = other2.getFs();
			if (ra == rb) {
				if (fsa == fsb) return 0;
				else return (FieldSet.leq(fsa, fsb)) ? -1 : 1;
			} else return (Utilities.leqReg(ra, rb)) ? -1 : 1;
		} else return 0;
	}
	
	public boolean equals(Object other) {
		return (compareTo(other) == 0);
	}
	
	public CyclicityTuple clone() {
		return new CyclicityTuple(getR(),getFs());
	}

	public String toString() {
		return "(" + getR() + "," + getFs() + ")";
	}

}
