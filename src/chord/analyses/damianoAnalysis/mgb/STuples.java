package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Pent;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;

public class STuples extends Tuples {

	private ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuples;
	
	public STuples() {
		tuples = new ArrayList<Quad<Register,Register,FieldSet,FieldSet>>();
	}
	
	public STuples(ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuples) {
		this.tuples = tuples;
	}
	
	boolean join(STuples others) {
		boolean newStuff = false;
		for (Quad<Register,Register,FieldSet,FieldSet> t : others.getTuples()) {
			if (!tuples.contains(t)) {
				tuples.add(t);
				newStuff = true;
			}
		}
		return newStuff;
	}

	public ArrayList<Quad<Register,Register,FieldSet,FieldSet>> getTuples() {
		return tuples;
	}
	
	public void setTuples(ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuples) {
		this.tuples = tuples;
	}
	
	public void addTuple(Quad<Register,Register,FieldSet,FieldSet> tuple) {
		boolean found = false;
		for (Quad<Register,Register,FieldSet,FieldSet> t : tuples) {
			found |= (t.val0 == tuple.val0 && t.val1 == tuple.val1 && t.val2 == tuple.val2 && t.val3 == tuple.val3);
		}
		if (!found) tuples.add(tuple);		
	}
	
	public void copyTuples(Register source,Register dest) {
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> newTuples = new ArrayList<Quad<Register,Register,FieldSet,FieldSet>>();
		for (Quad<Register,Register,FieldSet,FieldSet> t : tuples) {
			if (t.val0 == source && t.val0 == source) {
				newTuples.add(new Quad<Register,Register,FieldSet,FieldSet>(dest,dest,t.val2,t.val3));
			} else if (t.val0 == source) {
				newTuples.add(new Quad<Register,Register,FieldSet,FieldSet>(dest,t.val1,t.val2,t.val3));
			} else if (t.val1 == source) {
				newTuples.add(new Quad<Register,Register,FieldSet,FieldSet>(t.val0,dest,t.val2,t.val3));
			}
			tuples.addAll(newTuples);
		}
	}
	
	public void moveTuples(Register source,Register dest) {
		boolean somethingNew = false;
		for (Quad<Register,Register,FieldSet,FieldSet> t : tuples) {
			if (t.val0 == source) t.val0 = dest;
			if (t.val1 == source) t.val1 = dest;
		}
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
	public ArrayList<Quad<Register,Register,FieldSet,FieldSet>> moveTuplesList(List<Register> source, List<Register> dest) {
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> movedTuples = new ArrayList<>();
		movedTuples.addAll(tuples);
		assert(source.size() == dest.size());
		
		for (int i = 0; i < source.size(); i++){
			for (int j = 0; j < movedTuples.size(); j++){
				Quad<Register,Register,FieldSet,FieldSet> p = movedTuples.get(j);
				if (p.val0 == source.get(i) && p.val1 == source.get(i)) {
					movedTuples.set(j,new Quad<Register,Register,FieldSet,FieldSet>(dest.get(i),dest.get(i),p.val2,p.val3));
				} else if(p.val0 == source.get(i)) {
					movedTuples.set(j,new Quad<Register,Register,FieldSet,FieldSet>(dest.get(i),p.val1,p.val2,p.val3));	
				} else if(p.val1 == source.get(i)) {
					movedTuples.set(j,new Quad<Register,Register,FieldSet,FieldSet>(p.val0,dest.get(i),p.val2,p.val3));	
				}
			}
    	}
		
		if(movedTuples.size() > 0){
			Utilities.out("");
			Utilities.out("\t\t - TUPLES OF SHARING AFTER MOVING");
			for(Quad<Register,Register,FieldSet,FieldSet> p: movedTuples)
				Utilities.out("\t\t (" + p.val0 + "," + p.val1 + "," + p.val2 + "," + p.val3 + ")");
			
		}
    	return movedTuples;
	}
	
	/**
	 * Makes a SHALLOW copy of its tuples and returns a new STuples object.
	 * The copy is shallow because Register and FieldSet objects need not to
	 * be duplicated
	 */
	public STuples clone() {
		return new STuples(((ArrayList<Quad<Register,Register,FieldSet,FieldSet>>) tuples.clone()));
	}
	
	public String toString() {
		String s = "";
		for (Quad<Register,Register,FieldSet,FieldSet> t : tuples) {
			s = s + "(" + t.val1 + "," + t.val2 + "," + t.val3 + ")  -  ";
		}
		return s;
	}
}
