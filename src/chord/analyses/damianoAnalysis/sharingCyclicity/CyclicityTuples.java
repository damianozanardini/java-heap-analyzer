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
		for (CyclicityTuple p : others.getTuples()) {
			if (!tuples.contains(p)) {
				tuples.add(p);
				newStuff = true;
			}
		}
		sort();
		return newStuff;
	}

	public ArrayList<CyclicityTuple> getTuples() {
		return tuples;
	}
	
	public void setTuples(ArrayList<CyclicityTuple> tuples) {
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

	public void copyTuples(Register source,Register dest) {
		if (source==null || dest==null) return;
		ArrayList<CyclicityTuple> newTuples = new ArrayList<CyclicityTuple>();
		for (CyclicityTuple t : tuples)
			if (t.getR() == source) newTuples.add(new CyclicityTuple(dest,t.getFs()));
		for (CyclicityTuple t : newTuples) addTuple(t);
	}
	
	public void moveTuples(Register source,Register dest) {
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
	
	// WARNING: have to make sure that the iteration is point to the right element after remove()
	public void remove(Register r) {
		Iterator<CyclicityTuple> iterator = tuples.iterator();
		while (iterator.hasNext()) {
			CyclicityTuple tuple = iterator.next();
			if (tuple.getR() == r) iterator.remove();
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
		ArrayList<CyclicityTuple> newTuples = new ArrayList<CyclicityTuple>();
		for (CyclicityTuple tuple : tuples)
			if (actualParameters.contains(tuple.getR()))
				newTuples.add(tuple);
		tuples = newTuples;
	}
		
	public String toString() {
		String s = "";
		if (tuples.size()>0) {
			CyclicityTuple t = tuples.get(0);
			s = s + "(" + t.getR() + "," + t.getFs() + ")";
			for (int i=1; i<tuples.size(); i++) { // index starts from 1 on purpose
				t = tuples.get(i);
				s = s + " - (" + t.getR() + "," + t.getFs() + ")";
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

	public void sort() {
		Collections.sort(tuples);
	}
	
	
}
