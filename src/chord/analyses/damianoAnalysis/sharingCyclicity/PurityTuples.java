package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import chord.analyses.damianoAnalysis.Utilities;
import chord.bddbddb.Rel.RelView;
import chord.bddbddb.Rel.TrioIterable;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * Class representing purity information in form of tuples (actually, a tuple
 * PurityTuple is just a Register, but it is called "tuple" in order to reuse the same
 * terminology as other analyses such as sharing, cyclicity or definite aliasing).
 * 
 * A list [r1,r2,...,rk] means that registers r1,...,rk COULD (it is a "possible"
 * analysis) have been modified during the execution of the current method.
 * 
 * @author damiano
 *
 */
public class PurityTuples extends Tuples {

	/**
	 * The purity information in form of tuples.
	 */
	private ArrayList<PurityTuple> tuples;
	
	/**
	 * Default constructor: creates a new object with an empty list of tuples.
	 */
	public PurityTuples() {
		tuples = new ArrayList<PurityTuple>();
	}
	
	/**
	 * Creates a new object with the given list of tuples.
	 * @param tuples
	 */
	public PurityTuples(ArrayList<PurityTuple> tuples) {
		this.tuples = tuples;
	}
	
	/**
	 * Implements set union on lists of tuples, and returns true iff some new
	 * tuple is added to the list of this object.
	 * 
	 * @param others
	 * @return
	 */
	public boolean join(PurityTuples others) {
		boolean b = false;
		for (PurityTuple t : others.getInfo())
			if (!tuples.contains(t)) {
				tuples.add(t);
				b = true;
			}
		return b;
	}
	
	/**
	 * Returns the list of tuples.
	 */
	public ArrayList<PurityTuple> getInfo() {
		return tuples;
	}
	
	/**
	 * Replaces the current list of tuples with the given list.
	 * 
	 * @param tuples
	 */
	public void setInfo(ArrayList<PurityTuple> tuples) {
		this.tuples = tuples;
	}
	
	/**
	 * Adds a tuple to the list of tuples, if it is not already there.
	 * Insertion is unordered.
	 * 
	 * @param t
	 */
	public void addTuple(PurityTuple t) {
		if (!contains(t)) tuples.add(t);
	}
	
	/**
	 * Creates a new PurityTuple object with the given Register, and adds it to
	 * the list, if it is not already there.  Insertion is unordered.
	 * 
	 * @param t
	 */
	public void addTuple(Register r) {
		addTuple(new PurityTuple(r));
	}

	/**
	 * Copies purity info from source to dest.  This boils down to add a tuple
	 * for dest iff source is in the list and dest is not.
	 * 
	 * @param source The source register
	 * @param dest The destination register
	 */
	public void copyInfo(Register source,Register dest) {
		if (contains(new PurityTuple(source)) && !contains(new PurityTuple(dest)))
			tuples.add(new PurityTuple(dest));
	}
	
	/**
	 * Copies purity information from register source of another PurityTuples
	 * object to register dest of the current object.
	 * 
	 * @param other The other purityTuples object
	 * @param source The source register
	 * @param dest The destination register
	 */
	public void copyInfoFrom(PurityTuples other,Register source,Register dest) {
		if (other.contains(new PurityTuple(source)) && !contains(new PurityTuple(dest)))
			tuples.add(new PurityTuple(dest));
	}
	
	/**
	 * Moves purity info from source to dest.
	 */
	public void moveInfo(Register source,Register dest) {
		remove(dest);
		if (contains(source)) {
			if (!contains(dest)) addTuple (dest);
			remove(source);
		}
	}

	/**
	 * Removes the purity information about a given register.
	 */
	public void remove(Register r) {
		for (Iterator<PurityTuple> it = tuples.iterator(); it.hasNext(); ) {
			PurityTuple t = it.next();
			if (t.getR() == r) it.remove();
		}
	}
	    
	/**
	 * Makes a SHALLOW copy of its tuples and returns a new PurityTuples object.
	 * The copy is shallow because Register objects need not to be duplicated.
	 */
	public PurityTuples clone() {
		ArrayList<PurityTuple> newTuples = new ArrayList<PurityTuple>();
		for (PurityTuple tuple : tuples)
			newTuples.add(new PurityTuple(tuple.getR()));
		return new PurityTuples(newTuples);		
	}
	
	/**
	 * Only keeps the purity information about a list of registers: the given list
	 * of actual parameters.
	 */
	public void filterActual(List<Register> actualParameters) {
		for (Iterator<PurityTuple> it = tuples.iterator(); it.hasNext(); ) {
			PurityTuple t = it.next();
			if (!actualParameters.contains(t.getR())) it.remove();
		}
	}
	
	/**
	 * Returns true iff the purity information is empty.
	 */
	public boolean isBottom() {
		return (tuples.size()==0);
	}
	
	/**
	 * Returns true iff both PurityTuples objects contains the same purity
	 * information.  Tuples are sorted to make comparison easier (sortedness
	 * is also a desirable side effect).
	 * 
	 * @param other
	 * @return
	 */
	public boolean equals(PurityTuples other) {
		ArrayList<PurityTuple> otherTuples = other.getInfo();
		if (other == null) return isBottom();
		if (tuples.size() != otherTuples.size()) return false;
		Collections.sort(tuples);
		Collections.sort(otherTuples);
		boolean b = true;
		Iterator<PurityTuple> it = tuples.iterator();
		Iterator<PurityTuple> otherIt = otherTuples.iterator();
		while (it.hasNext()) {
			PurityTuple t = it.next();
			PurityTuple otherT = otherIt.next();
			b &= t.getR() == otherT.getR();
		}
		return b;
	}

	/**
	 * Self-explaining
	 */
	public String toString() {
		String s = "";
		if (tuples.size()>0) {
			s = s + tuples.get(0);
			for (int i=1; i<tuples.size(); i++) { // index starts from 1 on purpose
				s = s + "-" + tuples.get(i);
			}
		}
		return s;
	}

	/**
	 * Returns true iff a tuple with the same content as the given tuple is contained
	 * in the list.  Note that comparison is done with "equals", not with "==".
	 */
	public boolean contains(Tuple tuple) {
		if (tuple instanceof PurityTuple) {
			boolean found = false;
			for (PurityTuple t : tuples) found |= (tuple.equals(t));
			return found;
		} else return false;
	}

	/**
	 * Returns true iff a tuple containing the given register is contained
	 * in the list.
	 */
	public boolean contains(Register r) {
		boolean found = false;
		for (PurityTuple t : tuples) found |= (r == t.getR());
		return found;
	}

	/**
	 * Removes all the purity information.
	 */
	public void clear() {
		tuples.clear();
	}
	
}
