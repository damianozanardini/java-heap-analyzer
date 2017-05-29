package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.Utilities;
import chord.bddbddb.Rel.RelView;
import chord.bddbddb.Rel.TrioIterable;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;
import joeq.Compiler.Quad.RegisterFactory.Register;

public class CTuples extends Tuples {

	private ArrayList<Pair<Register,FieldSet>> tuples;
	
	public CTuples() {
		tuples = new ArrayList<Pair<Register,FieldSet>>();
	}
	
	public CTuples(ArrayList<Pair<Register,FieldSet>> tuples) {
		this.tuples = tuples;
	}
	
	boolean join(CTuples others) {
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
		for (Pair<Register,FieldSet> t : tuples) {
			found |= (t.val0 == r && t.val1 == fs);
		}
		if (!found) tuples.add(new Pair<Register,FieldSet>(r,fs));		
	}

	public void copyTuples(Register source,Register dest) {
		ArrayList<Pair<Register,FieldSet>> newTuples = new ArrayList<Pair<Register,FieldSet>>();
		for (Pair<Register,FieldSet> t : tuples) {
			if (t.val0 == source) {
				newTuples.add(new Pair<Register,FieldSet>(dest,t.val1));
			}
			tuples.addAll(newTuples);
		}
	}
	
	public void moveTuples(Register source,Register dest) {
		for (Pair<Register,FieldSet> t : tuples)
			if (t.val0 == source) t.val0 = dest;
	}

	/**
	 * This method moves the tuples of a list of registers to a other list of registers. The position
	 * of the origin register in the source list corresponds with the position of the destination register in the
	 * dest list.
	 * 
	 * @param source
	 * @param dest
	 * @return
	 */
	public ArrayList<Pair<Register,FieldSet>> moveTuplesList(List<Register> source, List<Register> dest) {
		// COPY OF TUPLES BECAUSE FIRST IT IS NEEDED TO DELETE ALL TUPLES OF SOURCE REGISTER
		ArrayList<Pair<Register,FieldSet>> movedTuples = new ArrayList<>();
		movedTuples.addAll(tuples);
		assert(source.size() == dest.size());
				
		// ADD THE TUPLES OF THE OLD REGISTER TO THE NEW
		for(int i = 0; i < source.size(); i++){
			for(int j = 0; j < movedTuples.size(); j++){
				Pair<Register,FieldSet> t = movedTuples.get(j);
				if(t.val0 == source.get(i))
					movedTuples.set(j,new Pair<Register,FieldSet>(dest.get(i),t.val1));
			}
		}
		
		if(movedTuples.size() > 0){
			Utilities.out("");
			Utilities.out("\t\t - TUPLES OF CYCLICITY AFTER MOVING");
			for(Pair<Register,FieldSet> t: movedTuples)
				Utilities.out("\t\t (" + t.val0 + "," + t.val1 + ")");
			
		}
		return movedTuples;
	}
	
    protected List<FieldSet> findTuplesByRegister(Register r) {
    	Iterator<Pair<Register,FieldSet>> iterator = tuples.iterator();
    	List<FieldSet> list = new ArrayList<FieldSet>();
    	while (iterator.hasNext()) {
    		Pair<Register,FieldSet> pair = iterator.next();
    		if (pair.val0 == r) list.add(pair.val1);
    	}    	
    	return list;
    }
	
	/**
	 * Makes a SHALLOW copy of its tuples and returns a new CTuples object.
	 * The copy is shallow because Register and FieldSet objects need not to
	 * be duplicated
	 */
	public CTuples clone() {
		return new CTuples(((ArrayList<Pair<Register,FieldSet>>) tuples.clone()));
	}
	
	public String toString() {
		String s = "";
		for (Pair<Register,FieldSet> t : tuples) {
			s = s + "(" + t.val0 + "," + t.val1 + ")  -  ";
		}
		return s;
	}

}
