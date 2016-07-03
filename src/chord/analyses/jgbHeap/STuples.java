package chord.analyses.jgbHeap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
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
	
	public void setTuples(ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuples){ this.tuples = tuples; }
	
	/**
	 * This method moves the tuples of a list of registers to a other list of registers. The position
	 * of the origin register in the source list corresponds with the position of the destination register in the
	 * dest list.
	 * 
	 * @param source
	 * @param dest
	 * @return
	 */
	public ArrayList<Quad<Register,Register,FieldSet,FieldSet>> moveTuplesList(List<Register> source, List<Register> dest){
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> movedTuples = new ArrayList<>();
		movedTuples.addAll(tuples);
		assert(source.size() == dest.size());
		
		for(int i = 0; i < source.size(); i++){
			for(int j = 0; j < movedTuples.size(); j++){
				Quad<Register,Register,FieldSet,FieldSet> q = movedTuples.get(j);
				if(q.val0 == source.get(i) && q.val1 == source.get(i)){
					movedTuples.set(j, new Quad<Register,Register,FieldSet,FieldSet>(dest.get(i),dest.get(i),q.val2,q.val3));
				}else if(q.val0 == source.get(i)){
					movedTuples.set(j,new Quad<Register,Register,FieldSet,FieldSet>(dest.get(i),q.val1,q.val2,q.val3));	
				}else if(q.val1 == source.get(i)){
					movedTuples.set(j,new Quad<Register,Register,FieldSet,FieldSet>(q.val0,dest.get(i),q.val2,q.val3));	
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
}
