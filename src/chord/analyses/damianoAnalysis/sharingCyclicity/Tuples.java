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
	
	public void sort() {
		Collections.sort(getTuples());
	}
	
	public boolean equals(Tuples other) {
		Utilities.info("MYSELF " + this);
		Utilities.info("ITSELF " + other);
		if (other == null) {
			return isBottom();
		} else {
			sort();
			other.sort();
			Utilities.info("SORTED MYSELF " + this);
			Utilities.info("SORTED ITSELF " + other);
			return getTuples().equals(other.getTuples());
		}
	}

	public abstract <T> ArrayList<T> getTuples();

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
		return (getTuples().size() == 0);
	}
}
