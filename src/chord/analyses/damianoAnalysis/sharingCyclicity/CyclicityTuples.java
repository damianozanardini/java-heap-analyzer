package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
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

	private ArrayList<Pair<Register,FieldSet>> tuples;
	
	public CyclicityTuples() {
		tuples = new ArrayList<Pair<Register,FieldSet>>();
	}
	
	public CyclicityTuples(ArrayList<Pair<Register,FieldSet>> tuples) {
		this.tuples = tuples;
	}
	
	boolean join(CyclicityTuples others) {
		boolean newStuff = false;
		for (Pair<Register,FieldSet> p : others.getTuples()) {
			if (!tuples.contains(p)) {
				tuples.add(p);
				newStuff = true;
			}
		}
		return newStuff;
	}

	public ArrayList<Pair<Register,FieldSet>> getTuples() {
		return tuples;
	}
	
	public void setTuples(ArrayList<Pair<Register,FieldSet>> tuples) {
		this.tuples = tuples;
	}
	
	public void addTuple(Register r,FieldSet fs) {
		boolean found = false;
		for (Pair<Register,FieldSet> t : tuples)
			found |= (t.val0 == r && t.val1 == fs);
		if (!found) {
			tuples.add(new Pair<Register,FieldSet>(r,fs));
			notifyTupleAdded(r,fs);
		}
	}

	public void addTuple(Pair<Register,FieldSet> t) {
		if (t!=null) addTuple(t.val0,t.val1);
	}

	public void copyTuples(Register source,Register dest) {
		if (source==null || dest==null) return;
		ArrayList<Pair<Register,FieldSet>> newTuples = new ArrayList<Pair<Register,FieldSet>>();
		for (Pair<Register,FieldSet> t : tuples)
			if (t.val0 == source) newTuples.add(new Pair<Register,FieldSet>(dest,t.val1));
		for (Pair<Register,FieldSet> t : newTuples) addTuple(t);
	}
	
	public void moveTuples(Register source,Register dest) {
		for (Pair<Register,FieldSet> t : tuples)
			if (t.val0 == source) t.val0 = dest;
	}

	/**
	 * This method moves the tuples of a list of registers to another list of registers. The position
	 * of the origin register in the source list corresponds with the position of the destination register in the
	 * dest list.
	 * 
	 * @param source
	 * @param dest
	 * @return
	 */
	public ArrayList<Pair<Register,FieldSet>> moveTuplesList(List<Register> source, List<Register> dest) {
		// COPY OF TUPLES BECAUSE FIRST IT IS NEEDED TO DELETE ALL TUPLES OF SOURCE REGISTER
		Utilities.info("SOURCE REGISTERS: " + source + " / DEST REGISTERS: " + dest);
		Utilities.info("INITIAL CTUPLES: " + this);
		assert(source.size() == dest.size());
				
		// ADD THE TUPLES OF THE OLD REGISTER TO THE NEW
		for(int i = 0; i < source.size(); i++){
			for(int j = 0; j < tuples.size(); j++){
				Pair<Register,FieldSet> t = tuples.get(j);
				if(t.val0 == source.get(i))
					t.val0 = dest.get(i);
				// movedTuples.set(j,new Pair<Register,FieldSet>(dest.get(i),t.val1));
			}
		}
		Utilities.info("FINAL CTUPLES: " + this);
		// WARNING: probably not needed
		return tuples;
	}
	
    public ArrayList<FieldSet> findTuplesByRegister(Register r) {
    	Iterator<Pair<Register,FieldSet>> iterator = tuples.iterator();
    	ArrayList<FieldSet> list = new ArrayList<FieldSet>();
    	while (iterator.hasNext()) {
    		Pair<Register,FieldSet> pair = iterator.next();
    		if (pair.val0 == r) list.add(pair.val1);
    	}    	
    	return list;
    }
	
	// WARNING: have to make sure that the iteration is point to the right element after remove()
	public void remove(Register r) {
		Iterator<Pair<Register,FieldSet>> iterator = tuples.iterator();
		while (iterator.hasNext()) {
			Pair<Register,FieldSet> tuple = iterator.next();
			if (tuple.val0 == r) {
				iterator.remove();
			}
		}
	}
	
	public void removeList(List<Register> list) {
		for (Register r : list) remove(r);
	}
    
	/**
	 * Makes a SHALLOW copy of its tuples and returns a new CTuples object.
	 * The copy is shallow because Register and FieldSet objects need not to
	 * be duplicated
	 */
	public CyclicityTuples clone() {
		ArrayList<Pair<Register,FieldSet>> newTuples = new ArrayList<Pair<Register,FieldSet>>();
		for (Pair<Register,FieldSet> tuple : tuples) {
			newTuples.add(new Pair(tuple.val0,tuple.val1));
		}
		return new CyclicityTuples(newTuples);		
	}
	
	private void notifyTupleAdded(Register r,FieldSet fs) {
		Utilities.info("ADDED TO CYCLE: ( " + r + ", " + fs + " )");
	}
	
	public void filterActual(List<Register> actualParameters) {
		ArrayList<Pair<Register,FieldSet>> newTuples = new ArrayList<Pair<Register,FieldSet>>();
		for (Pair<Register,FieldSet> tuple : tuples)
			if (actualParameters.contains(tuple.val0))
				newTuples.add(tuple);
		tuples = newTuples;
	}
		
	public boolean isBottom() {
		return (tuples.size()==0);
	}

	public boolean equals(CyclicityTuples other) {
		return tuples.equals(other.tuples);
	}

	public String toString() {
		String s = "";
		if (tuples.size()>0) {
			Pair<Register,FieldSet> t = tuples.get(0);
			s = s + "(" + t.val0 + "," + t.val1 + ")";
			for (int i=1; i<tuples.size(); i++) { // index starts from 1 on purpose
				t = tuples.get(i);
				s = s + " - (" + t.val0 + "," + t.val1 + ")";
			}
		}
		return s;
	}

	
	
}
