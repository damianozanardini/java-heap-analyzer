package chord.analyses.jgbHeap;

import java.util.ArrayList;

import chord.util.tuple.object.Pair;
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
	
	public void setTuples(ArrayList<Pair<Register,FieldSet>> tuples){ this.tuples = tuples; }
	
}
