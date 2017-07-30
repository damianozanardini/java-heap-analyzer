package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import chord.analyses.damianoAnalysis.Utilities;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * This abstract class models a container of sharing or cyclicity information.
 * Some abstract methods are provided, which will be required in subclasses.
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
	
	public abstract boolean contains(Tuple tuple);
	
	/**
	 * Calling the sorting algorithm.
	 * Important notice: in order to work properly, the System property
	 * java.util.Arrays.useLegacyMergeSort must be set to true by the directive
	 *   -Djava.util.Arrays.useLegacyMergeSort=true
	 * in VM args.
	 * This happens because, at some point, the new TimSort algorithm claims that
	 * the contract between equals and compareTo in SharingTuple has been broken.
	 * We were not able to find a solution to this, so we use the old MergeSort
	 * algorithm.
	 */
	public void sort() {
		Collections.sort(getInfo());
	}
	
	public boolean equals(Tuples other) {
		if (other == null) {
			return isBottom();
		} else {
			sort();
			other.sort();
			return getInfo().equals(other.getInfo());
		}
	}

	public abstract <T> ArrayList<T> getInfo();
	
	public abstract void copyInfo(Register source,Register dest);

	public abstract void moveInfo(Register source,Register dest);

	/**
	 * Returns a copy of the current object.  The copy is deep (the tuples are duplicated), although,
	 * clearly, the specific Register and FieldSet objects are not duplicated.
	 */
	public abstract Tuples clone();
	
	/**
	 * Removes information about all registers which are NOT actual parameters.
	 * 
	 * @param actualParameters
	 */
	public abstract void filterActual(List<Register> actualParameters) ;
	
	/**
	 * Visualization of a Tuples object as a String.
	 */
	public abstract String toString();
		
	/**
	 * Returns true iff there is no information at all.
	 * 
	 * @return
	 */
	public boolean isBottom() {
		return (getInfo().size() == 0);
	}
}
