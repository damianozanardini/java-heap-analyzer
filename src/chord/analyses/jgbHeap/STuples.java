package chord.analyses.jgbHeap;

import java.util.ArrayList;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

public class STuples extends Tuples {

	private ArrayList<Trio<Register,Register,FieldSet>> tuples;
	
	public STuples() {
		tuples = new ArrayList<Trio<Register,Register,FieldSet>>();
	}
	
	boolean join(STuples others) {
		boolean newStuff = false;
		for (Trio<Register,Register,FieldSet> t : others.getTuples()) {
			if (!tuples.contains(t)) {
				tuples.add(t);
				newStuff = true;
			}
		}
		return newStuff;
	}

	public ArrayList<Trio<Register,Register,FieldSet>> getTuples() {
		return tuples;
	}
	
	public void setTuples(){}

}
