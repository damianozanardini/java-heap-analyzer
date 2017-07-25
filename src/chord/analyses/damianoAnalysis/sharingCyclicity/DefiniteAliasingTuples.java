package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
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

public class DefiniteAliasingTuples extends Tuples {

	private ArrayList<Pair<Register,Register>> tuples;
	
	public DefiniteAliasingTuples() {
		tuples = new ArrayList<Pair<Register,Register>>();
	}
	
	public DefiniteAliasingTuples(ArrayList<Pair<Register,Register>> tuples) {
		this.tuples = tuples;
	}
	
	boolean join(DefiniteAliasingTuples others) {
		boolean b = false;
		for (Pair<Register,Register> t : others.getTuples()) {
			if (!tuples.contains(t)) {
				tuples.add(t);
				b = true;
			}
		}
		return b;
	}

	public ArrayList<Pair<Register,Register>> getTuples() {
		return tuples;
	}
	
	public void setTuples(ArrayList<Pair<Register,Register>> tuples) {
		this.tuples = tuples;
	}
	
	public void addTuple(Register r1,Register r2) {
		boolean found = false;
		if (r1 == r2) {
			for (Pair<Register,Register> t : tuples) {
				found |= (t.val0 == r1 && t.val1 == r2);
				found |= (t.val0 == r1 && t.val1 == r2);
			}
			if (!found)
				tuples.add(new Pair<Register,Register>(r1,r2));
		} else {
			if (!Utilities.leqReg(r1,r2)) {
				Register r_aux = r1;
				r1 = r2;
				r2 = r_aux;
			}
			for (Pair<Register,Register> t : tuples)
				found |= (t.val0 == r1 && t.val1 == r2);
			if (!found)
				tuples.add(new Pair<Register,Register>(r1,r2));
		}
	}

	public void addTuple(Pair<Register,Register> t) {
		if (t!=null) addTuple(t.val0,t.val1);
	}

	public void copyTuples(Register source,Register dest) {
		if (source==null || dest==null) return;
		ArrayList<Pair<Register,Register>> newTuples = new ArrayList<Pair<Register,Register>>();
		for (Pair<Register,Register> t : tuples) {
			if (t.val0 == source && t.val1 == source) {
				newTuples.add(new Pair<Register,Register>(dest,dest));
				newTuples.add(new Pair<Register,Register>(Utilities.minReg(source,dest),Utilities.maxReg(source, dest)));
			} else if (t.val0 == source) {
				newTuples.add(new Pair<Register,Register>(Utilities.minReg(dest,t.val1),Utilities.maxReg(dest,t.val1)));
			} else if (t.val1 == source) {
				newTuples.add(new Pair<Register,Register>(Utilities.minReg(t.val0,dest),Utilities.maxReg(t.val0,dest)));
			}
		}
		for (Pair<Register,Register> t : newTuples) addTuple(t);
	}
	
	public void moveTuples(Register source,Register dest) {
		for (Pair<Register,Register> t : tuples) {
			if (t.val0 == source) t.val0 = dest;
			if (t.val1 == source) t.val1 = dest;
			if (!Utilities.leqReg(t.val0,t.val1)) {
				Register s = t.val0;
				t.val0 = t.val1;
				t.val1 = s;
			}
		}
	}

	/**
	 * This method moves the tuples of a list of registers to a other list of registers.
	 * The position of the origin register in the source list corresponds with the
	 * position of the destination register in the dest list.
	 * 
	 * @param source
	 * @param dest
	 * @return
	 */
	public ArrayList<Pair<Register,Register>> moveTuplesList(List<Register> source, List<Register> dest) {
		Utilities.info("SOURCE REGISTERS: " + source + " / DEST REGISTERS: " + dest);
		Utilities.info("INITIAL DATUPLES: " + this);
		assert(source.size() == dest.size());
		
		for (int i = 0; i < source.size(); i++) {
			for (int j = 0; j < tuples.size(); j++) {
				Pair<Register,Register> p = tuples.get(j);
				if (p.val0 == source.get(i) && p.val1 == source.get(i)) {
					p.val0 = dest.get(i);
					p.val1 = dest.get(i);
				} else if(p.val0 == source.get(i)) {
					p.val0 = dest.get(i);
				} else if(p.val1 == source.get(i)) {
					p.val1 = dest.get(i);
				}
				if (!Utilities.leqReg(p.val0,p.val1)) {
					Register s = p.val0;
					p.val0 = p.val1;
					p.val1 = s;
				}
			}
		}
		Utilities.info("FINAL DATUPLES: " + this);
		// WARNING: probably not needed
		return tuples;
	}

    public ArrayList<Register> findTuplesByFirstRegister(Register r) {
    		Iterator<Pair<Register,Register>> iterator = tuples.iterator();
    		ArrayList<Register> list = new ArrayList<Register>();
    		while (iterator.hasNext()) {
    			Pair<Register,Register> tuple = iterator.next();
    			if (tuple.val0 == r)
    				list.add(tuple.val1);
    		}    	
    		return list;
    }
    
    public ArrayList<Register> findTuplesBySecondRegister(Register r) {
    		Iterator<Pair<Register,Register>> iterator = tuples.iterator();
    		ArrayList<Register> list = new ArrayList<Register>();
    		while (iterator.hasNext()) {
    			Pair<Register,Register> tuple = iterator.next();
    			if (tuple.val1 == r)
    				list.add(tuple.val0);
    		}    	
    		return list;
    }

    /**
     * Finds all tuples in the relation whose first or second register is
     * {@code r}.
     */
    public ArrayList<Register> findTuplesByRegister(Register r) {
    		ArrayList<Register> list1 = findTuplesByFirstRegister(r);
    		ArrayList<Register> list2 = findTuplesBySecondRegister(r);
    		list1.addAll(list2);
    		return list1;
    }
    
	// WARNING: have to make sure that the iteration is point to the right element after remove()
	public void remove(Register r) {
		Iterator<Pair<Register,Register>> iterator = tuples.iterator();
		while (iterator.hasNext()) {
			Pair<Register,Register> tuple = iterator.next();
			if (tuple.val0 == r || tuple.val1 == r) iterator.remove();
		}
	}
	
	public void removeList(List<Register> list) {
		for (Register r : list) remove(r);
	}
	
	/**
	 * Makes a SHALLOW copy of its tuples and returns a new DefiniteAliasingTuples object.
	 * The copy is shallow because Register objects need not to be duplicated
	 */
	public DefiniteAliasingTuples clone() {
		ArrayList<Pair<Register,Register>> newTuples = new ArrayList<Pair<Register,Register>>();
		for (Pair<Register,Register> tuple : tuples) {
			newTuples.add(new Pair<Register,Register>(tuple.val0,tuple.val1));
		}
		return new DefiniteAliasingTuples(newTuples);		
	}
	
	public void filterActual(List<Register> actualParameters) {
		ArrayList<Pair<Register,Register>> newTuples = new ArrayList<Pair<Register,Register>>();
		for (Pair<Register,Register> tuple : tuples)
			if (actualParameters.contains(tuple.val0) && actualParameters.contains(tuple.val1))
				newTuples.add(tuple);
		tuples = newTuples;
	}
	
	public String toString() {
		String s = "";
		if (tuples.size()>0) {
			Pair<Register,Register> t = tuples.get(0);
			s = s + "[" + t.val0 + "*" + t.val1 + "]";
			for (int i=1; i<tuples.size(); i++) { // index starts from 1 on purpose
				t = tuples.get(i);
				s = s + " - [" + t.val0 + "*" + t.val1 + "]";
			}
		}
		return s;
	}
	
	public boolean equals(DefiniteAliasingTuples other) {
		return tuples.equals(other.tuples);
	}

	public boolean isBottom() {
		return tuples.size()==0;
	}
	
}
