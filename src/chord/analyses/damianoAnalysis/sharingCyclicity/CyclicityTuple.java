package chord.analyses.damianoAnalysis.sharingCyclicity;

import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * This class implements the minimal bit of cyclicity information: a tuple
 * (r,fs) meaning that r may have cycles traversing fields according to fs.
 * 
 * @author damiano
 */
public class CyclicityTuple extends Tuple {

	/**
	 * The tuple which stores the cyclicity information.
	 */
	Pair<Register,FieldSet> elem;

	/**
	 * Default constructor.
	 * 
	 * @param r
	 * @param fs
	 */
	public CyclicityTuple(Register r, FieldSet fs) {
		elem = new Pair<Register,FieldSet>(r,fs);
	}
	
	/**
	 * Returns the register.
	 * 
	 * @return
	 */
	public Register getR() { return elem.val0; }
	
	/**
	 * Returns the fieldset.
	 * 
	 * @return
	 */
	public FieldSet getFs() { return elem.val1; }

	/**
	 * Sets the register.
	 * 
	 * @param r
	 */
	public void setR(Register r) {
		elem.val0 = r;
	}
	
	/**
	 * Sets the fieldset.
	 * 
	 * @param fs
	 */
	public void setFs(FieldSet fs) {
		elem.val1 = fs;
	}

	/**
	 * Implementation of the compareTo method for cyclicityTuple.
	 * As usual, "==" equality is used instead of some other kind of equality
	 * since both Register and FieldSet are domains (there are no two different
	 * Register objects with the same name "R0", nor two different Fieldset
	 * objects with the same set of fields). 
	 */
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
	
	/**
	 * Equality is equivalent to a comparison with result 0.
	 */
	public boolean equals(Object other) {
		return (compareTo(other) == 0);
	}
	
	/**
	 * Returns a new CyclicityTuple object with the same register and fieldset.
	 * The copy and the original satisfy original.equals(copy) == true.
	 */
	public CyclicityTuple clone() {
		return new CyclicityTuple(getR(),getFs());
	}

	/**
	 * Self-explaining.
	 */
	public String toString() {
		return "(" + getR() + "," + getFs() + ")";
	}

}
