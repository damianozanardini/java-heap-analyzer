package chord.analyses.damianoAnalysis.sharingCyclicity;

import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import joeq.Compiler.Quad.RegisterFactory.Register;

public class DefiniteAliasingTuple extends Tuple {

	Pair<Register,Register> elem;

	public DefiniteAliasingTuple(Register r1,Register r2) {
		elem = new Pair<Register,Register>(Utilities.minReg(r1,r2),Utilities.maxReg(r1,r2));
	}
	
	public Register getR1() { return elem.val0; }
	
	public Register getR2() { return elem.val1; }

	public void setR1(Register r) {
		elem.val0 = Utilities.minReg(r,elem.val1);
		elem.val1 = Utilities.maxReg(r,elem.val1);
	}

	public void setR2(Register r) {
		elem.val0 = Utilities.minReg(r,elem.val0);
		elem.val1 = Utilities.maxReg(r,elem.val0);
	}
	
	public void setRs(Register r1,Register r2) {
		elem.val0 = Utilities.minReg(r1,r2);
		elem.val1 = Utilities.maxReg(r1,r2);
	}

	public int compareTo(Object other) {
		if (other instanceof DefiniteAliasingTuple) {
			DefiniteAliasingTuple b = (DefiniteAliasingTuple) other;
			Register ra1 = getR1();
			Register ra2 = getR2();
			Register rb1 = b.getR1();
			Register rb2 = b.getR2();
			if (ra1 == rb1)
				if (ra2 == rb2)
					return 0;
				else return (Utilities.leqReg(ra2, rb2)) ? -1 : 1;
			else return (Utilities.leqReg(ra1, rb1)) ? -1 : 1;
		} else return 0;
	}
	
	public boolean equals(Object other) {
		return (compareTo(other) == 0);
	}

	public DefiniteAliasingTuple clone() {
		return new DefiniteAliasingTuple(getR1(),getR2());
	}

	public String toString() {
		return "[" + getR1() + "*" + getR2() + "]";
	}
	

}
