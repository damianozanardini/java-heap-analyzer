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

public class PurityTuples extends Tuples {

	private ArrayList<Register> tuples;
	
	public PurityTuples() {
		tuples = new ArrayList<Register>();
	}
	
	public PurityTuples(ArrayList<Register> tuples) {
		this.tuples = tuples;
	}
	
	boolean join(PurityTuples others) {
		boolean newStuff = false;
		for (Register r : others.getTuples()) {
			if (!tuples.contains(r)) {
				tuples.add(r);
				newStuff = true;
			}
		}
		return newStuff;
	}

	public ArrayList<Register> getTuples() {
		return tuples;
	}
	
	public void setTuples(ArrayList<Register> tuples) {
		this.tuples = tuples;
	}
	
	public void addTuple(Register r) {
		boolean found = false;
		for (Register r0 : tuples) found |= (r0 == r);
		if (!found) tuples.add(r);
	}

	public void copyTuples(Register source,Register dest) {
		if (tuples.contains(source) && !tuples.contains(dest))
			tuples.add(dest);
	}
	
	public void moveTuples(Register source,Register dest) {
		if (tuples.contains(source)) {
			if (!tuples.contains(dest)) tuples.add(dest);
			tuples.remove(source);
		}
	}
	
	public void remove(Register r) {
		tuples.remove(r);
	}
	
	public void removeList(List<Register> list) {
		for (Register r : list) remove(r);
	}
    
	/**
	 * Makes a SHALLOW copy of its tuples and returns a new CTuples object.
	 * The copy is shallow because Register objects need not to be duplicated
	 */
	public PurityTuples clone() {
		return (PurityTuples) tuples.clone();
	}
		
	public void filterActual(List<Register> actualParameters) {
		ArrayList<Register> newTuples = new ArrayList<Register>();
		for (Register r : tuples)
			if (actualParameters.contains(r)) newTuples.add(r);
		tuples = newTuples;
	}
		
	public boolean isBottom() {
		return (tuples.size()==0);
	}

	public boolean equals(PurityTuples other) {
		return tuples.equals(other.tuples);
	}

	public String toString() {
		String s = "";
		if (tuples.size()>0) {
			Register r = tuples.get(0);
			s = s + "<" + r + ">";
			for (int i=1; i<tuples.size(); i++) { // index starts from 1 on purpose
				r = tuples.get(i);
				s = s + " - <" + r + ">";
			}
		}
		return s;
	}

}
