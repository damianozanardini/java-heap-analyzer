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

public class PurityTuples extends Tuples {

	private ArrayList<PurityTuple> tuples;
	
	public PurityTuples() {
		tuples = new ArrayList<PurityTuple>();
	}
	
	public PurityTuples(ArrayList<PurityTuple> tuples) {
		this.tuples = tuples;
	}
	
	boolean join(PurityTuples others) {
		boolean newStuff = false;
		for (PurityTuple t : others.getTuples()) {
			if (!tuples.contains(t)) {
				tuples.add(t);
				newStuff = true;
			}
		}
		return newStuff;
	}
	
	public ArrayList<PurityTuple> getTuples() {
		return tuples;
	}
	
	public void setTuples(ArrayList<PurityTuple> tuples) {
		this.tuples = tuples;
	}
	
	public void addTuple(PurityTuple t) {
		if (!contains(t)) tuples.add(t);
	}
	
	public void addTuple(Register r) {
		addTuple(new PurityTuple(r));
	}

	public void copyTuples(Register source,Register dest) {
		if (contains(new PurityTuple(source)) && !contains(new PurityTuple(dest)))
			tuples.add(new PurityTuple(dest));
	}
	
	public void moveTuples(Register source,Register dest) {
		PurityTuple s = new PurityTuple(source);
		PurityTuple d = new PurityTuple(dest);
		if (contains(s)) {
			if (!tuples.contains(d)) tuples.add(d);
			tuples.remove(s);
		}
	}
	
	public void remove(Register r) {
		Iterator<PurityTuple> iterator = tuples.iterator();
		while (iterator.hasNext()) {
			PurityTuple tuple = iterator.next();
			if (tuple.getR() == r) iterator.remove();
		}
	}
	    
	/**
	 * Makes a SHALLOW copy of its tuples and returns a new PurityTuples object.
	 * The copy is shallow because Register objects need not to be duplicated
	 */
	public PurityTuples clone() {
		ArrayList<PurityTuple> newTuples = new ArrayList<PurityTuple>();
		for (PurityTuple tuple : tuples)
			newTuples.add(new PurityTuple(tuple.getR()));
		return new PurityTuples(newTuples);		
	}
		
	public void filterActual(List<Register> actualParameters) {
		ArrayList<PurityTuple> newTuples = new ArrayList<PurityTuple>();
		for (PurityTuple t : tuples)
			if (actualParameters.contains(t.getR())) newTuples.add(t);
		tuples = newTuples;
	}
		
	public boolean isBottom() {
		return (tuples.size()==0);
	}

	public boolean equals(PurityTuples other) {
		Collections.sort(tuples);
		Collections.sort(other.getTuples());
		return tuples.equals(other.getTuples());
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
		if (tuple instanceof PurityTuple) {
			boolean found = false;
			for (PurityTuple t : tuples) found |= (tuple.equals(t));
			return found;
		} else return false;
	}
	
	
}
