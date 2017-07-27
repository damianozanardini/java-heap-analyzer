package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * This abstract class models a container of sharing or cyclicity information.
 * Some abstract methods are provided, which will be required in subclasses.
 *  
 * @author damiano
 */
public abstract class Tuples {
	
	ArrayList<Tuple> tuples;
	
	/**
	 * Removes all the information about a specific register.
	 * 
	 * @param r
	 */
	public abstract void remove(Register r);
	
	public abstract boolean contains(Tuple tuple);
	
	public void sort() {
		Collections.sort(tuples);
	}
	
	public boolean equals(Tuples other) {
		sort();
		other.sort();
		return tuples.equals(other.getTuples());
	}

	private ArrayList<Tuple> getTuples() {
		return tuples;
	}

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
	public abstract boolean isBottom();
}
