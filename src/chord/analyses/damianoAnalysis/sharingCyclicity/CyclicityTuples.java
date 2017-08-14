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
 * This class implements a list of cyclicity tuples.
 *  
 * @author damiano
 *
 */
public class CyclicityTuples extends Tuples {

	/**
	 * The list of tuples.
	 */
	private ArrayList<CyclicityTuple> tuples;
	
	/**
	 * Default constructor: creates an empty list of tuples.
	 */
	public CyclicityTuples() {
		tuples = new ArrayList<CyclicityTuple>();
	}
	
	/**
	 * Joins the information of "this" with the information of "others" by
	 * computing the list union (removing duplicates) and sorting.
	 * 
	 * @param others The other CyclicityTuples object
	 * @return
	 */
	public boolean join(CyclicityTuples others) {
		boolean newStuff = false;
		for (CyclicityTuple p : others.getInfo()) {
			if (!tuples.contains(p)) {
				tuples.add(p);
				newStuff = true;
			}
		}
		sort();
		return newStuff;
	}

	/**
	 * Returns the information as a list of tuples.
	 */
	public ArrayList<CyclicityTuple> getInfo() {
		return tuples;
	}
	
	/**
	 * Sets the list of tuples to the given value.
	 * 
	 * @param tuples The given list of tuples
	 */
	public void setInfo(ArrayList<CyclicityTuple> tuples) {
		this.tuples = tuples;
	}
	
	/**
	 * Adds a bit of cyclicity information, as a register and a fieldset.
	 * 
	 * @param r
	 * @param fs
	 */
	public void addTuple(Register r,FieldSet fs) {
		addTuple(new CyclicityTuple(r,fs));
	}

	/**
	 * Adds a bit of cyclicity information, as a CyclicityTuple object.
	 * 
	 * @param t
	 */
	public void addTuple(CyclicityTuple t) {
		if (t!=null && !contains(t)) tuples.add(t);
	}

	/**
	 * Copies cyclicity information from register source to register dest.
	 * 
	 * @param source The source register
	 * @param dest The destination register
	 */
	public void copyInfo(Register source,Register dest) {
		if (source==null || dest==null) return;
		ArrayList<CyclicityTuple> newTuples = new ArrayList<CyclicityTuple>();
		for (CyclicityTuple t : tuples)
			if (t.getR() == source) newTuples.add(new CyclicityTuple(dest,t.getFs()));
		for (CyclicityTuple t : newTuples) addTuple(t);
	}
	
	/**
	 * Copies cyclicity information from register source of another SharingTuples
	 * object to register dest of the current object.
	 * 
	 * @param other The other SharingTuples object
	 * @param source The source register
	 * @param dest The destination register
	 */
	public void copyInfoFrom(CyclicityTuples other,Register source,Register dest) {
		if (source==null || dest==null) return;
		ArrayList<CyclicityTuple> newTuples = new ArrayList<CyclicityTuple>();
		for (CyclicityTuple t : other.getInfo())
			if (t.getR() == source) newTuples.add(new CyclicityTuple(dest,t.getFs()));
		for (CyclicityTuple t : newTuples) addTuple(t);
	}

	/**
	 * Moves cyclicity information from register source to register dest.
	 * 
	 * @param source The source register
	 * @param dest The destination register
	 */
	public void moveInfo(Register source,Register dest) {
		for (CyclicityTuple t : tuples)
			if (t.getR() == source) t.setR(dest);
	}
	
	/**
	 * Returns the cyclicity information about a given register in form of a
	 * list of fieldsets.
	 * 
	 * @param r The register
	 * @return The list of fieldsets about r
	 */
    public ArrayList<FieldSet> findTuplesByRegister(Register r) {
    		Iterator<CyclicityTuple> iterator = tuples.iterator();
    		ArrayList<FieldSet> list = new ArrayList<FieldSet>();
    		while (iterator.hasNext()) {
    			CyclicityTuple pair = iterator.next();
    			if (pair.getR() == r) list.add(pair.getFs());
    		}    	
    		return list;
    }
	
	/**
	 * Removes cyclicity information about a given register.
	 * 
	 * @param r The register
	 */
	public void remove(Register r) {
		for (Iterator<CyclicityTuple> it = tuples.iterator(); it.hasNext(); ) {
			CyclicityTuple tuple = it.next();
			if (tuple.getR() == r) it.remove();
		}
	}
	    
	/**
	 * Makes a shallow copy of its tuples and returns a new CTuples object.
	 * The copy is shallow because Register and FieldSet objects need not to
	 * be duplicated
	 */
	public CyclicityTuples clone() {
		ArrayList<CyclicityTuple> newTuples = new ArrayList<CyclicityTuple>();
		for (CyclicityTuple tuple : tuples)
			newTuples.add(new CyclicityTuple(tuple.getR(),tuple.getFs()));
		CyclicityTuples theClone = new CyclicityTuples();
		theClone.setInfo(newTuples);
		return theClone;
	}
		
	/**
	 * Removes information about all registers which are not actual parameters.
	 * 
	 * @param actualParameters The list of actual parameters
	 */
	public void filterActual(List<Register> actualParameters) {
		for (Iterator<CyclicityTuple> it = tuples.iterator(); it.hasNext(); ) {
			CyclicityTuple t = it.next();
			if (!actualParameters.contains(t.getR())) it.remove();
		}
	}
	
	/**
	 * Return true iff the current object contains a tuple which is equal to
	 * the given one.
	 * 
	 * @param tuple The tuple
	 */
	public boolean contains(Tuple tuple) {
		if (tuple instanceof CyclicityTuple) {
			for (CyclicityTuple t : tuples)
				if (tuple.equals(t)) return true;
			return false;
		} else return false;
	}

	/**
	 * Self-explaining.
	 */
	public String toString() {
		String s = "";
		if (tuples.size()>0) {
			s = s + tuples.get(0);
			for (int i=1; i<tuples.size(); i++) { // index starts from 1 on purpose
				s = s + " - " + tuples.get(i);
			}
		}
		return s;
	}
		
	
}
