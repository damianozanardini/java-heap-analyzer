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

public class CyclicityTuples extends Tuples {

	private ArrayList<CyclicityTuple> tuples;
	
	public CyclicityTuples() {
		tuples = new ArrayList<CyclicityTuple>();
	}
	
	public CyclicityTuples(ArrayList<CyclicityTuple> tuples) {
		this.tuples = tuples;
	}
	
	boolean join(CyclicityTuples others) {
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

	public ArrayList<CyclicityTuple> getInfo() {
		return tuples;
	}
	
	public void setInfo(ArrayList<CyclicityTuple> tuples) {
		this.tuples = tuples;
	}
	
	public void addTuple(Register r,FieldSet fs) {
		boolean found = false;
		for (CyclicityTuple t : tuples)
			found |= (t.getR() == r && t.getFs() == fs);
		if (!found) {
			tuples.add(new CyclicityTuple(r,fs));
		}
	}

	public void addTuple(CyclicityTuple t) {
		if (t!=null) addTuple(t.getR(),t.getFs());
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

	public void moveInfo(Register source,Register dest) {
		for (CyclicityTuple t : tuples)
			if (t.getR() == source) t.setR(dest);
	}
	
    public ArrayList<FieldSet> findTuplesByRegister(Register r) {
    		Iterator<CyclicityTuple> iterator = tuples.iterator();
    		ArrayList<FieldSet> list = new ArrayList<FieldSet>();
    		while (iterator.hasNext()) {
    			CyclicityTuple pair = iterator.next();
    			if (pair.getR() == r) list.add(pair.getFs());
    		}    	
    		return list;
    }
	
	public void remove(Register r) {
		for (Iterator<CyclicityTuple> it = tuples.iterator(); it.hasNext(); ) {
			CyclicityTuple tuple = it.next();
			if (tuple.getR() == r) it.remove();
		}
	}
	    
	/**
	 * Makes a SHALLOW copy of its tuples and returns a new CTuples object.
	 * The copy is shallow because Register and FieldSet objects need not to
	 * be duplicated
	 */
	public CyclicityTuples clone() {
		ArrayList<CyclicityTuple> newTuples = new ArrayList<CyclicityTuple>();
		for (CyclicityTuple tuple : tuples)
			newTuples.add(new CyclicityTuple(tuple.getR(),tuple.getFs()));
		return new CyclicityTuples(newTuples);		
	}
		
	public void filterActual(List<Register> actualParameters) {
		for (Iterator<CyclicityTuple> it = tuples.iterator(); it.hasNext(); ) {
			CyclicityTuple t = it.next();
			if (!actualParameters.contains(t.getR())) it.remove();
		}
	}
		
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

	public boolean contains(Tuple tuple) {
		if (tuple instanceof CyclicityTuple) {
			boolean found = false;
			for (CyclicityTuple t : tuples) found |= (tuple.equals(t));
			return found;
		} else return false;
	}
	
	
}
