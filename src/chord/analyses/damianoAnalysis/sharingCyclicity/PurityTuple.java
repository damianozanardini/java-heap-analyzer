package chord.analyses.damianoAnalysis.sharingCyclicity;

import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import joeq.Compiler.Quad.RegisterFactory.Register;

public class PurityTuple extends Tuple {

	Register elem;

	public PurityTuple(Register r) {
		elem = r;
	}
	
	public Register getR() { return elem; }
	
	public void setR(Register r) {
		elem = r;
	}
	
	public boolean equals(Tuple other) {
		if (other instanceof PurityTuple) {
			PurityTuple other2 = (PurityTuple) other;
			return (getR() == other2.getR());
		} else return false;
	}
	
	public int compareTo(Object other) {
		if (other instanceof PurityTuple) {
			PurityTuple b = (PurityTuple) other;
			Register ra = getR();
			Register rb = b.getR();
			if (ra == rb)
				return 0;
			else return (Utilities.leqReg(ra, rb)) ? -1 : 1;
		} else return 0;
	}

	public PurityTuple clone() {
		return new PurityTuple(getR());
	}

	public String toString() {
		return "<" + getR() + ">";
	}

}
