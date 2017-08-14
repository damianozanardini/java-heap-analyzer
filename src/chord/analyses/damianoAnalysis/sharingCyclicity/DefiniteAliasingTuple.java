package chord.analyses.damianoAnalysis.sharingCyclicity;

import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * This class manages a bit of definite aliasing information in form of an
 * ordered pair of registers.
 *  
 * @author damiano
 *
 */
public class DefiniteAliasingTuple extends Tuple {

	/**
	 * The data.  Pairs of registers are kept ordered by the constructor
	 * and modifying methods.
	 */
	Pair<Register,Register> elem;

	/**
	 * Default constructor.  It creates an ordered pair of registers.
	 * 
	 * @param r1 The first register
	 * @param r2 The second register
	 */
	public DefiniteAliasingTuple(Register r1,Register r2) {
		elem = new Pair<Register,Register>(Utilities.minReg(r1,r2),Utilities.maxReg(r1,r2));
	}
	
	/**
	 * Returns the first register of the tuple.
	 * 
	 * @return
	 */
	public Register getR1() {
		return elem.val0;
	}
	
	/**
	 * Returns the second register of the tuple.
	 * 
	 * @return
	 */
	public Register getR2() {
		return elem.val1;
	}

	/**
	 * Updates the first element of the pair, also keeping the pair ordered (i.e.,
	 * updating {@code (R2,R5)} with {@code R6} gives {@code (R5,R6)}).
	 * 
	 * @param r
	 */
	public void setR1(Register r) {
		elem.val0 = Utilities.minReg(r,elem.val1);
		elem.val1 = Utilities.maxReg(r,elem.val1);
	}

	/**
	 * Updates the second element of the pair, also keeping the pair ordered (i.e.,
	 * updating {@code (R2,R5)} with {@code R0} gives {@code (R0,R2)}).
	 * 
	 * @param r
	 */
	public void setR2(Register r) {
		elem.val0 = Utilities.minReg(r,elem.val0);
		elem.val1 = Utilities.maxReg(r,elem.val0);
	}
	
	/**
	 * Updates both elements of the pair, also keeping the pair ordered.
	 * 
	 * @param r1
	 * @param r2
	 */	
	public void setRs(Register r1,Register r2) {
		elem.val0 = Utilities.minReg(r1,r2);
		elem.val1 = Utilities.maxReg(r1,r2);
	}

	/**
	 * Implementation of the compareTo method for DefiniteAliasingTuple.
	 * Tuples are compared w.r.t. lexicographic order on the registers of the pair.
	 * E.g., {@code (R3,R4) < (R3,R5) < (R4,R4)}.
	 */
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
	
	/**
	 * Equality is equivalent to having both registers equal.
	 */
	public boolean equals(Object other) {
		return (compareTo(other) == 0);
	}

	/**
	 * Returns a new {@literal DefiniteAliasingTuple} which is equal to
	 * {@literal this} with respect to {@literal equals()}.
	 */
	public DefiniteAliasingTuple clone() {
		return new DefiniteAliasingTuple(getR1(),getR2());
	}

	/**
	 * Self-explaining.
	 */
	public String toString() {
		return "[" + getR1() + "*" + getR2() + "]";
	}
	

}
