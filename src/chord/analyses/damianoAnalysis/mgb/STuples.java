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

	private ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>> tuples;
	
	public STuples() {
		tuples = new ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>>();
	}
	
	public STuples(ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>> tuples) {
		this.tuples = tuples;
	}
	
	boolean join(STuples others) {
		boolean newStuff = false;
		for (Pent<Entry,Register,Register,FieldSet,FieldSet> t : others.getTuples()) {
			if (!tuples.contains(t)) {
				tuples.add(t);
				newStuff = true;
			}
		}
		return newStuff;
	}

	public ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>> getTuples() {
		return tuples;
	}
	
	public void setTuples(ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>> tuples) {
		this.tuples = tuples;
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
	public ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>> moveTuplesList(List<Register> source, List<Register> dest){
		ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>> movedTuples = new ArrayList<>();
		movedTuples.addAll(tuples);
		assert(source.size() == dest.size());
		
		for (int i = 0; i < source.size(); i++){
			for (int j = 0; j < movedTuples.size(); j++){
				Pent<Entry,Register,Register,FieldSet,FieldSet> p = movedTuples.get(j);
				if (p.val1 == source.get(i) && p.val2 == source.get(i)) {
					movedTuples.set(j,new Pent<Entry,Register,Register,FieldSet,FieldSet>(p.val0,dest.get(i),dest.get(i),p.val3,p.val4));
				} else if(p.val1 == source.get(i)) {
					movedTuples.set(j,new Pent<Entry,Register,Register,FieldSet,FieldSet>(p.val0,dest.get(i),p.val2,p.val3,p.val4));	
				} else if(p.val2 == source.get(i)) {
					movedTuples.set(j,new Pent<Entry,Register,Register,FieldSet,FieldSet>(p.val0,p.val1,dest.get(i),p.val3,p.val4));	
				}
			}
    	}
		
		if(movedTuples.size() > 0){
			Utilities.out("");
			Utilities.out("\t\t - TUPLES OF SHARING AFTER MOVING");
			for(Pent<Entry,Register,Register,FieldSet,FieldSet> p: movedTuples)
				Utilities.out("\t\t (" + p.val0 + "," + p.val1 + "," + p.val2 + "," + p.val3 + "," + p.val4 + ")");
			
		}
    	return movedTuples;
	}
	
	public String toString() {
		String s = "";
		for (Pent<Entry,Register,Register,FieldSet,FieldSet> t : tuples) {
			s = s + "(" + t.val1 + "," + t.val2 + "," + t.val3 + "," + t.val4 + ")  -  ";
		}
		return s;
	}
}
