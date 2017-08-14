package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.Utilities;
import chord.bddbddb.Rel.PentIterable;
import chord.bddbddb.Rel.RelView;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Pent;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;

/**
 * This class implements the container for definite aliasing information as a
 * list of pairs ({@literal Register},{@literal Register}).
 * 
 * A pair ({@literal r1},{@literal r2}) is in the definite aliasing relation
 * if both {@literal r1} and {@literal r2} are definitely non-null, and both
 * definitely point to the same object.
 * 
 * @author damiano
 *
 */
public class DefiniteAliasingTuples extends Tuples {
	
	/**
	 * The abstract information itself, in form of a list of pairs of registers.
	 */
	private ArrayList<DefiniteAliasingTuple> tuples;
	
	/**
	 * Default constructor.  The bottom element (corresponding to the maximum amount
	 * of information) is the empty list because it considers all registers to be null.
	 */
	public DefiniteAliasingTuples() {
		tuples = new ArrayList<DefiniteAliasingTuple>();
	}
	
	/**
	 * Creates a {@literal DefiniteAliasingTuples} object with the given list
	 * of tuples.
	 * 
	 * @param tuples
	 */
	public DefiniteAliasingTuples(ArrayList<DefiniteAliasingTuple> tuples) {
		this.tuples = tuples;
	}
	
	/**
	 * Since this analysis is a "definite" one, merging two abstract values
	 * boils down to list intersection.
	 * 
	 * @param others The definite aliasing information of the other abstract value
	 * @return whether some tuple has been removed
	 */
	boolean meet(DefiniteAliasingTuples others) {
		boolean b = false;
		ArrayList<DefiniteAliasingTuple> otherTuples = others.getInfo();
		for (Iterator<DefiniteAliasingTuple> it = tuples.iterator(); it.hasNext(); ) {
			DefiniteAliasingTuple t = it.next();
			if (!otherTuples.contains(t)) {
				it.remove();
				b = true;
			}
		}
		return b;
	}

	/**
	 * Returns the definite aliasing information as tuples.
	 */
	public ArrayList<DefiniteAliasingTuple> getInfo() {
		return tuples;
	}
	
	/**
	 * Sets the definite aliasing information to the given value (without cloning).
	 * 
	 * @param otherTuples
	 */
	public void setInfo(ArrayList<DefiniteAliasingTuple> otherTuples) {
		tuples = otherTuples;
	}
	
	/**
	 * Adds a tuple to the definite aliasing information.  A
	 * {@literal DefiniteAliasingTuple} is always ordered, so that there is no
	 * need to sort it here.
	 * 
	 * @param t
	 */
	public void addTuple(DefiniteAliasingTuple t) {
		if (!contains(t)) tuples.add(t);
	}

	/**
	 * Adds a tuple from two registers.  Registers are ordered by the
	 * {@literal DefiniteAliasingTuple} constructor.
	 * 
	 * @param r1
	 * @param r2
	 */
	public void addTuple(Register r1,Register r2) {
		addTuple(new DefiniteAliasingTuple(r1,r2));
	}

	/**
	 * Copies definite aliasing information from register {@literal source} to
	 * register {@literal dest}.
	 * 
	 * @param source The source register
	 * @param dest The destination register
	 */
	public void copyInfo(Register source,Register dest) {
		if (source==null || dest==null) return;
		boolean nonNull = false;
		ArrayList<Pair<Register,Register>> newPairs = new ArrayList<Pair<Register,Register>>();
		for (Iterator<DefiniteAliasingTuple> it = tuples.iterator(); it.hasNext(); ) {
			DefiniteAliasingTuple tuple = it.next();
			if (tuple.getR1() == source) {
				nonNull = true;
				newPairs.add(new Pair<Register,Register>(dest,tuple.getR2()));
			}
			if (tuple.getR2() == source) {
				nonNull = true;
				newPairs.add(new Pair<Register,Register>(tuple.getR1(),dest));
			}
		}
		for (Pair<Register,Register> p: newPairs) addTuple(p.val0,p.val1);
		if (nonNull) addTuple(source,dest);
	}
	
	/**
	 * Moves definite aliasing information from register {@literal source} to
	 * register {@literal dest}.
	 * 
	 * @param source The source register
	 * @param dest The destination register
	 */
	public void moveInfo(Register source,Register dest) {
		if (source==null || dest==null) return;
		for (Iterator<DefiniteAliasingTuple> it = tuples.iterator(); it.hasNext(); ) {
			DefiniteAliasingTuple t = it.next();
			if (t.getR1() == source && t.getR2() == source) t.setRs(dest,dest);
			else if (t.getR1() == source) t.setR1(dest);
			else if (t.getR2() == source) t.setR2(dest);
		}
	}

    /**
     * Finds all tuples in the relation whose first or second register is {@literal r},
     * and builds a list of registers which are definitely aliasing with {@literal r}. 
     * 
     * @param r
     */
    public ArrayList<Register> findTuplesByRegister(Register r) {
    		ArrayList<Register> list = new ArrayList<Register>();
    		for (DefiniteAliasingTuple t : tuples) {
    			if (t.getR1() == r && !list.contains(t.getR2())) list.add(t.getR2());
    			if (t.getR2() == r && !list.contains(t.getR1())) list.add(t.getR1());
    		}
    		return list;
    }
    
    /**
     * Removes the definite aliasing information about a specific register {@literal r}.
     * 
     * @param r The register
     */
	public void remove(Register r) {
		for (Iterator<DefiniteAliasingTuple> it = tuples.iterator(); it.hasNext(); ) {
			DefiniteAliasingTuple tuple = it.next();
			if (tuple.getR1() == r || tuple.getR2() == r) it.remove();
		}
	}
		
	/**
	 * Makes a copy of its tuples and returns a new {@literal DefiniteAliasingTuples}
	 * object.  The copy is shallow because {@literal Register} objects need not to
	 * be duplicated, but {@literal DefiniteAliasingTuple} objects are duplicated.
	 */
	public DefiniteAliasingTuples clone() {
		ArrayList<DefiniteAliasingTuple> newTuples = new ArrayList<DefiniteAliasingTuple>();
		for (DefiniteAliasingTuple t : tuples) newTuples.add(t.clone());
		return new DefiniteAliasingTuples(newTuples);		
	}
	
	/**
	 * Removes information about all registers which are not actual parameters.
	 * 
	 * @param actualParameters
	 */
	public void filterActual(List<Register> actualParameters) {
		for (Iterator<DefiniteAliasingTuple> it = tuples.iterator(); it.hasNext(); ) {
			DefiniteAliasingTuple t = it.next();
			if (!actualParameters.contains(t.getR1()) || !actualParameters.contains(t.getR2()))
				it.remove();
		}
	}
	
	/**
	 * Self-explaining.
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
	 * Returns true iff the abstract information is compatible with the nullness of
	 * all registers.  This amounts to tuples being an empty list.
	 */
	public boolean isBottom() {
		return tuples.isEmpty();
	}

	/**
	 * Returns true iff the abstract information contains a tuple whose pair of
	 * registers is the same as the given tuple.
	 * 
	 * @param tuple The tuple to be searched for
	 * @return whether {@literal tuple} is in the list of tuples
	 */
	public boolean contains(Tuple tuple) {
		if (tuple instanceof DefiniteAliasingTuple) {
			boolean found = false;
			for (DefiniteAliasingTuple t : tuples) found |= (tuple.equals(t));
			return found;
		} else return false;
	}

}
