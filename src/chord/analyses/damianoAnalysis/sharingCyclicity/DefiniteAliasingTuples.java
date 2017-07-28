package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Utilities;
import chord.bddbddb.Rel.PentIterable;
import chord.bddbddb.Rel.RelView;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Pent;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;

/**
 * This class implements the container for definite aliasing information as a
 * list of pairs (register,register).
 * 
 * @author damiano
 *
 */
public class DefiniteAliasingTuples extends Tuples {

	private ArrayList<DefiniteAliasingTuple> tuples;
	
	public DefiniteAliasingTuples() {
		tuples = new ArrayList<DefiniteAliasingTuple>();
	}
	
	public DefiniteAliasingTuples(ArrayList<DefiniteAliasingTuple> tuples) {
		this.tuples = tuples;
	}
	
	/**
	 * Since this analysis is a "definite" one, merging two abstract values
	 * boils down to list intersection.
	 * 
	 * @param others
	 * @return
	 */
	boolean meet(DefiniteAliasingTuples others) {
		boolean b = false;
		ArrayList<DefiniteAliasingTuple> otherTuples = others.getTuples();
		for (DefiniteAliasingTuple t : tuples) {
			if (!otherTuples.contains(t)) {
				tuples.remove(t);
				b = true;
			}
		}
		return b;
	}

	public ArrayList<DefiniteAliasingTuple> getTuples() {
		return tuples;
	}
	
	public void setTuples(ArrayList<DefiniteAliasingTuple> tuples) {
		this.tuples = tuples;
	}
	
	public void addTuple(DefiniteAliasingTuple t) {
		if (!contains(t)) tuples.add(t);
	}

	public void addTuple(Register r1,Register r2) {
		addTuple(new DefiniteAliasingTuple(r1,r2));
	}

	public void copyTuples(Register source,Register dest) {
		if (source==null || dest==null) return;
		ArrayList<DefiniteAliasingTuple> newTuples = new ArrayList<DefiniteAliasingTuple>();
		for (DefiniteAliasingTuple t : tuples) {
			if (t.getR1() == source && t.getR2() == source) {
				newTuples.add(new DefiniteAliasingTuple(dest,dest));
				newTuples.add(new DefiniteAliasingTuple(source,dest));
			} else if (t.getR1() == source) {
				newTuples.add(new DefiniteAliasingTuple(dest,t.getR2()));
			} else if (t.getR2() == source) {
				newTuples.add(new DefiniteAliasingTuple(t.getR1(),dest));
			}
		}
		for (DefiniteAliasingTuple t : newTuples) addTuple(t);
	}
	
	public void moveTuples(Register source,Register dest) {
		if (source==null || dest==null) return;
		for (DefiniteAliasingTuple t : tuples) {
			if (t.getR1() == source && t.getR2() == source) {
				t.setRs(dest,dest);
			} else if (t.getR1() == source)
				t.setR1(dest);
			else if (t.getR2() == source)
				t.setR2(dest);
		}
	}

    /**
     * Finds all tuples in the relation whose first or second register is
     * {@code r}.
     */
    public ArrayList<Register> findTuplesByRegister(Register r) {
    		ArrayList<Register> list = new ArrayList<Register>();
    		for (DefiniteAliasingTuple t : tuples) {
    			if (t.getR1() == r && !list.contains(t.getR2())) list.add(t.getR2());
    			if (t.getR2() == r && !list.contains(t.getR1())) list.add(t.getR1());
    		}
    		return list;
    }
    
	// WARNING: have to make sure that the iteration is point to the right element after remove()
	public void remove(Register r) {
		Iterator<DefiniteAliasingTuple> iterator = tuples.iterator();
		while (iterator.hasNext()) {
			DefiniteAliasingTuple tuple = iterator.next();
			if (tuple.getR1() == r || tuple.getR2() == r) iterator.remove();
		}
	}
		
	/**
	 * Makes a SHALLOW copy of its tuples and returns a new DefiniteAliasingTuples object.
	 * The copy is shallow because Register objects need not to be duplicated
	 */
	public DefiniteAliasingTuples clone() {
		ArrayList<DefiniteAliasingTuple> newTuples = new ArrayList<DefiniteAliasingTuple>();
		for (DefiniteAliasingTuple t : tuples) {
			newTuples.add(t.clone());
		}
		return new DefiniteAliasingTuples(newTuples);		
	}
	
	public void filterActual(List<Register> actualParameters) {
		ArrayList<DefiniteAliasingTuple> newTuples = new ArrayList<DefiniteAliasingTuple>();
		for (DefiniteAliasingTuple t : tuples)
			if (actualParameters.contains(t.getR1()) && actualParameters.contains(t.getR2()))
				newTuples.add(t);
		tuples = newTuples;
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

	public boolean isBottom() {
		return tuples.size()==0;
	}

	public boolean contains(Tuple tuple) {
		if (tuple instanceof DefiniteAliasingTuple) {
			boolean found = false;
			for (DefiniteAliasingTuple t : tuples) found |= (tuple.equals(t));
			return found;
		} else return false;
	}


}
