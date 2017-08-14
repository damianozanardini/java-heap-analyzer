package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chord.analyses.damianoAnalysis.Utilities;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * This abstract class models a container of abstract information for each
 * static analysis implemented as tuples.  Some abstract methods are provided,
 * which will be required in subclasses.
 *  
 * @author damiano
 */
public abstract class Tuples {
		
	/**
	 * Removes all the information about a specific register.
	 * 
	 * @param r
	 */
	public abstract void remove(Register r);
	
	/**
	 * Returns true iff {@code tuple} is contained in the abstract information.
	 * 
	 * @param tuple
	 * @return
	 */
	public abstract boolean contains(Tuple tuple);
	
	/**
	 * Calls the sorting algorithm.
	 * <p>
	 * Important notice: in order to work properly, the system property
	 * {@code java.util.Arrays.useLegacyMergeSort} must be set to true by the directive
	 *   {@code -Djava.util.Arrays.useLegacyMergeSort=true}
	 * in VM arguments.  This happens because, at some point, the new {@code TimSort}
	 * algorithm claims that the contract between {@code equals()} and {@code compareTo()}
	 * in {@code SharingTuple} has been broken.  We were not able to find a solution to
	 * this, so we use the old MergeSort algorithm.
	 */
	public void sort() {
		Collections.sort(getInfo());
	}
	
	/**
	 * Returns true iff {@code this} and {@other} contain equivalent information.
	 * 
	 * @param other
	 * @return
	 */
	public boolean equals(Tuples other) {
		if (other == null) {
			return isBottom();
		} else {
			sort();
			other.sort();
			return getInfo().equals(other.getInfo());
		}
	}

	/**
	 * Returns the abstract information.
	 * 
	 * @return
	 */
	public abstract <T> ArrayList<T> getInfo();
	
	/**
	 * Copies the abstract information from one register to another.
	 * 
	 * @param source The source register
	 * @param dest The destination register
	 */
	public abstract void copyInfo(Register source,Register dest);

	/**
	 * Moves the abstract information from one register to another.
	 * 
	 * @param source The source register
	 * @param dest The destination register
	 */
	public abstract void moveInfo(Register source,Register dest);

	/**
	 * Returns a copy of the current object.  The copy is deep (the tuples are
	 * duplicated), although, clearly, the specific {@code Register} and {@code FieldSet}
	 * objects are not duplicated.
	 */
	public abstract Tuples clone();
	
	/**
	 * Removes information about all registers which are not actual parameters.
	 * 
	 * @param actualParameters
	 */
	public abstract void filterActual(List<Register> actualParameters);
	
	public abstract String toString();
		
	/**
	 * Returns true iff {@code this} contains no information at all.
	 * 
	 * @return
	 */
	public boolean isBottom() {
		return (getInfo().size() == 0);
	}
}
