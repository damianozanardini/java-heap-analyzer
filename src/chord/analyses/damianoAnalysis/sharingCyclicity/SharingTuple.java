package chord.analyses.damianoAnalysis.sharingCyclicity;

import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * This class implements the minimal bit of sharing information: a tuple
 * (r1,r2,fs1,fs2) meaning that r1 and r2 may share with convergent paths
 * traversing fields according to fs1 and fs2.
 * 
 * @author damiano
 */
public class SharingTuple extends Tuple {

	/**
	 * The tuple which stores the sharing information.
	 */
	Quad<Register,Register,FieldSet,FieldSet> elem;

	/**
	 * The constructor creates a sorted tuple.
	 * 
	 * @param r1
	 * @param r2
	 * @param fs1
	 * @param fs2
	 */
	public SharingTuple(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		elem = new Quad<Register,Register,FieldSet,FieldSet>(r1,r2,fs1,fs2);
		sort();
	}
	
	/**
	 * Returns the first register of the tuple.
	 * 
	 * @return
	 */
	public Register getR1() { return elem.val0; }
	
	/**
	 * Returns the second register of the tuple.
	 * 
	 * @return
	 */
	public Register getR2() { return elem.val1; }

	/**
	 * Returns the first fieldset of the tuple.
	 * 
	 * @return
	 */
	public FieldSet getFs1() { return elem.val2; }

	/**
	 * Returns the second fieldset of the tuple.
	 * 
	 * @return
	 */
	public FieldSet getFs2() { return elem.val3; }
	
	/**
	 * Sets the first register of the tuple and sorts the tuple to account for
	 * the case in which the second register is less than r.
	 * 
	 * @param r
	 */
	public void setR1(Register r) {
		elem.val0 = r;
		sort();		
	}
	
	/**
	 * Sets the second register of the tuple and sorts the tuple to account for
	 * the case in which the first register is greater than r.
	 * 
	 * @param r
	 */
	public void setR2(Register r) {
		elem.val1 = r;
		sort();		
	}

	/**
	 * Sets the first and second register at the same time, and sorts the tuple.
	 * 
	 * @param r1
	 * @param r2
	 */
	public void setRs(Register r1,Register r2) {
		elem.val0 = r1;
		elem.val1 = r2;
		sort();		
	}
	
	/**
	 * Sorting is based on the total orders on Register and FieldSet.
	 */
	private void sort() {
		// unordered registers
		if (!Utilities.leqReg(elem.val0,elem.val1)) {
			Register auxR = elem.val0;
			elem.val0 = elem.val1;
			elem.val1 = auxR;
			FieldSet auxFs = elem.val2;
			elem.val2 = elem.val3;
			elem.val3 = auxFs;
		}
		// self-sharing
		if (elem.val0 == elem.val1) {
			// unordered fieldsets
			if (!FieldSet.leq(elem.val2,elem.val3)) {
				FieldSet auxFs = elem.val2;
				elem.val2 = elem.val3;
				elem.val3 = auxFs;
			}
		}
	}

	/**
	 * Implementation of the compareTo method for SharingTuple.
	 * As usual, "==" equality is used instead of some other kind of equality
	 * since both Register and FieldSet are domains (there are no two different
	 * Register objects with the same name "R0", nor two different Fieldset
	 * objects with the same set of fields). 
	 */
	public int compareTo(Object other) {
		if (other instanceof SharingTuple) {
			SharingTuple b = (SharingTuple) other;
			Register ra1 = getR1();
			Register ra2 = getR2();
			Register rb1 = b.getR1();
			Register rb2 = b.getR2();
			FieldSet fsa1 = getFs1();
			FieldSet fsa2 = getFs2();
			FieldSet fsb1 = b.getFs1();
			FieldSet fsb2 = b.getFs2();
			if (ra1 == rb1)
				if (ra2 == rb2)
					if (fsa1 == fsb1)
						if (fsa2 == fsb2) 
							return 0;
						else return (FieldSet.leq(fsa2,fsb2)) ? -1 : 1;
					else return (FieldSet.leq(fsa1,fsb1)) ? -1 : 1;
				else return (Utilities.leqReg(ra2,rb2)) ? -1 : 1;
			else return (Utilities.leqReg(ra1,rb1)) ? -1 : 1;
		} else return -1;
	}

	/**
	 * Equality is equivalent to a comparison with result 0.
	 */
	public boolean equals(Object other) {
		if (other instanceof SharingTuple) {
			return (compareTo(other) == 0);
		} else return false;
	}

	/**
	 * Returns a new SharingTuple object with the same registers and fieldsets.
	 * The copy and the original satisfy original.equals(copy) == true.
	 */
	public SharingTuple clone() {
		return new SharingTuple(getR1(),getR2(),getFs1(),getFs2());
	}
	
	/**
	 * Self-explaining.
	 */
	public String toString() {
		return "(" + getR1() + "," + getR2() + "," + getFs1() + "," + getFs2() + ")";
	}

}
