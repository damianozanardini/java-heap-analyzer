package chord.analyses.damianoAnalysis.sharingCyclicity;

import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;

public class SharingTuple extends Tuple {

	Quad<Register,Register,FieldSet,FieldSet> elem;

	public SharingTuple(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		if (r1 == r2) {
			if (FieldSet.leq(fs1,fs2)) {
				elem = new Quad<Register,Register,FieldSet,FieldSet>(r1,r2,fs1,fs2);
			} else {
				elem = new Quad<Register,Register,FieldSet,FieldSet>(r1,r2,fs2,fs1);
			}
		} else if (Utilities.leqReg(r1,r2)) {
			elem = new Quad<Register,Register,FieldSet,FieldSet>(r1,r2,fs1,fs2);
		} else {
			elem = new Quad<Register,Register,FieldSet,FieldSet>(r2,r1,fs2,fs1);
		}
	}
	
	public Register getR1() { return elem.val0; }
	
	public Register getR2() { return elem.val1; }

	public FieldSet getFs1() { return elem.val2; }

	public FieldSet getFs2() { return elem.val3; }
	
	public void setR1(Register r) {
		elem.val0 = r;
		sort();		
	}
	
	public void setR2(Register r) {
		elem.val1 = r;
		sort();		
	}

	public void setRs(Register r1,Register r2) {
		elem.val0 = r1;
		elem.val1 = r2;
		sort();		
	}
	
	private void sort() {
		if (!Utilities.leqReg(elem.val0,elem.val1)) {
			Register auxR = elem.val0;
			elem.val0 = elem.val1;
			elem.val1 = auxR;
			FieldSet auxFs = elem.val2;
			elem.val2 = elem.val3;
			elem.val3 = auxFs;
		}
		if (elem.val0 == elem.val1) {
			if (!FieldSet.leq(elem.val2,elem.val3)) {
				FieldSet auxFs = elem.val2;
				elem.val2 = elem.val3;
				elem.val3 = auxFs;
			}
		}
	}

}
