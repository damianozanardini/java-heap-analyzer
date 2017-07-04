package chord.analyses.damianoAnalysis.mgb;

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

public class STuples extends Tuples {

	private ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuples;
	
	public STuples() {
		tuples = new ArrayList<Quad<Register,Register,FieldSet,FieldSet>>();
	}
	
	public STuples(ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuples) {
		this.tuples = tuples;
	}
	
	boolean join(STuples others) {
		boolean b = false;
		for (Quad<Register,Register,FieldSet,FieldSet> t : others.getTuples()) {
			if (!tuples.contains(t)) {
				tuples.add(t);
				b = true;
			}
		}
		return b;
	}

	public ArrayList<Quad<Register,Register,FieldSet,FieldSet>> getTuples() {
		return tuples;
	}
	
	public void setTuples(ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuples) {
		this.tuples = tuples;
	}
	
	public void addTuple(Register r1,Register r2,FieldSet fs1,FieldSet fs2) {
		boolean found = false;
		if (r1 == r2) {
			for (Quad<Register,Register,FieldSet,FieldSet> t : tuples) {
				found |= (t.val0 == r1 && t.val1 == r2 && t.val2 == fs1 && t.val3 == fs2);
				found |= (t.val0 == r1 && t.val1 == r2 && t.val2 == fs2 && t.val3 == fs1);
			}
			if (!found) {
				if (FieldSet.leq(fs1,fs2)) {
					tuples.add(new Quad<Register,Register,FieldSet,FieldSet>(r1,r2,fs1,fs2));
					notifyTupleAdded(r1,r2,fs1,fs2);
				} else {
					tuples.add(new Quad<Register,Register,FieldSet,FieldSet>(r1,r2,fs2,fs1));
					notifyTupleAdded(r1,r2,fs2,fs1);
				}
			}
		} else {
			if (!Utilities.leqReg(r1,r2)) {
				Register r_aux = r1;
				r1 = r2;
				r2 = r_aux;
				FieldSet fs_aux = fs1;
				fs1 = fs2;
				fs2 = fs_aux;
			}
			for (Quad<Register,Register,FieldSet,FieldSet> t : tuples)
				found |= (t.val0 == r1 && t.val1 == r2 && t.val2 == fs1 && t.val3 == fs2);
			if (!found) {
				tuples.add(new Quad<Register,Register,FieldSet,FieldSet>(r1,r2,fs1,fs2));
				notifyTupleAdded(r1,r2,fs1,fs2);
			}
		}
	}

	public void addTuple(Quad<Register,Register,FieldSet,FieldSet> t) {
		if (t!=null) addTuple(t.val0,t.val1,t.val2,t.val3);
	}

	public void copyTuples(Register source,Register dest) {
		if (source==null || dest==null) return;
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> newTuples = new ArrayList<Quad<Register,Register,FieldSet,FieldSet>>();
		for (Quad<Register,Register,FieldSet,FieldSet> t : tuples) {
			if (t.val0 == source && t.val1 == source) {
				newTuples.add(new Quad<Register,Register,FieldSet,FieldSet>(dest,dest,t.val2,t.val3));
				newTuples.add(new Quad<Register,Register,FieldSet,FieldSet>(Utilities.minReg(source,dest),Utilities.maxReg(source, dest),FieldSet.emptyset(),FieldSet.emptyset()));
			} else if (t.val0 == source) {
				newTuples.add(new Quad<Register,Register,FieldSet,FieldSet>(Utilities.minReg(dest,t.val1),Utilities.maxReg(dest,t.val1),t.val2,t.val3));
			} else if (t.val1 == source) {
				newTuples.add(new Quad<Register,Register,FieldSet,FieldSet>(Utilities.minReg(t.val0,dest),Utilities.maxReg(t.val0,dest),t.val2,t.val3));
			}
		}
		for (Quad<Register,Register,FieldSet,FieldSet> t : newTuples) addTuple(t);
	}
	
	public void moveTuples(Register source,Register dest) {
		for (Quad<Register,Register,FieldSet,FieldSet> t : tuples) {
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
	 * This method moves the tuples of a list of registers to a other list of registers. The position
	 * of the origin register in the source list corresponds with the position of the destination register in the
	 * dest list.
	 * 
	 * @param source
	 * @param dest
	 * @return
	 */
	public ArrayList<Quad<Register,Register,FieldSet,FieldSet>> moveTuplesList(List<Register> source, List<Register> dest) {
		Utilities.info("SOURCE REGISTERS: " + source + " / DEST REGISTERS: " + dest);
		Utilities.info("INITIAL STUPLES: " + this);
		assert(source.size() == dest.size());
		
		for (int i = 0; i < source.size(); i++) {
			for (int j = 0; j < tuples.size(); j++) {
				Quad<Register,Register,FieldSet,FieldSet> p = tuples.get(j);
				if (p.val0 == source.get(i) && p.val1 == source.get(i)) {
					p.val0 = dest.get(i);
					p.val1 = dest.get(i);
					// tuples.set(j,new Quad<Register,Register,FieldSet,FieldSet>(dest.get(i),dest.get(i),p.val2,p.val3));
				} else if(p.val0 == source.get(i)) {
					p.val0 = dest.get(i);
					// tuples.set(j,new Quad<Register,Register,FieldSet,FieldSet>(dest.get(i),p.val1,p.val2,p.val3));	
				} else if(p.val1 == source.get(i)) {
					// tuples.set(j,new Quad<Register,Register,FieldSet,FieldSet>(p.val0,dest.get(i),p.val2,p.val3));
					p.val1 = dest.get(i);
				}
				if (!Utilities.leqReg(p.val0,p.val1)) {
					Register s = p.val0;
					p.val0 = p.val1;
					p.val1 = s;
				}
			}
		}
		Utilities.info("FINAL STUPLES: " + this);
		// WARNING: probably not needed
		return tuples;
	}

	public void copyTuplesFromCycle(Register source,Register dest,CTuples ctuples) {
    	FieldSet fs = null;
    	List<FieldSet> l = ctuples.findTuplesByRegister(source);
    	Iterator<FieldSet> it = l.iterator();
    	while (it.hasNext()) {
    		fs = it.next();
    		addTuple(dest,dest,fs,fs);
    		addTuple(dest,dest,FieldSet.emptyset(),fs);
    	}
	}	
	
    public ArrayList<Trio<Register,FieldSet,FieldSet>> findTuplesByFirstRegister(Register r) {
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	ArrayList<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<Trio<Register,FieldSet,FieldSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> tuple = iterator.next();
    		if (tuple.val0 == r)
    			list.add(new Trio<Register,FieldSet,FieldSet>(tuple.val1,tuple.val2,tuple.val3));
    	}    	
    	return list;
    }
    
    public ArrayList<Pair<Register,FieldSet>> findTuplesByFirstRegisterEmpty1(Register r) {
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	ArrayList<Pair<Register,FieldSet>> list = new ArrayList<Pair<Register,FieldSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> tuple = iterator.next();
    		if (tuple.val0 == r && tuple.val2 == FieldSet.emptyset())
    			list.add(new Pair<Register,FieldSet>(tuple.val1,tuple.val3));
    	}    	
    	return list;
    }
    
    public ArrayList<Pair<Register,FieldSet>> findTuplesByFirstRegisterEmpty2(Register r) {
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	ArrayList<Pair<Register,FieldSet>> list = new ArrayList<Pair<Register,FieldSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> tuple = iterator.next();
    		if (tuple.val0 == r && tuple.val3 == FieldSet.emptyset())
    			list.add(new Pair<Register,FieldSet>(tuple.val1,tuple.val2));
    	}    	
    	return list;
    }

    public ArrayList<Trio<Register, FieldSet, FieldSet>> findTuplesBySecondRegister(Register r) {
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	ArrayList<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<Trio<Register,FieldSet,FieldSet>>();
    	while (iterator.hasNext()) {
        	Quad<Register,Register,FieldSet,FieldSet> tuple = iterator.next();
    		if (tuple.val1 == r)
    			list.add(new Trio<Register,FieldSet,FieldSet>(tuple.val0,tuple.val2,tuple.val3));
    	}    	
    	return list;
    }

    public ArrayList<Pair<Register,FieldSet>> findTuplesBySecondRegisterEmpty1(Register r) {
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	ArrayList<Pair<Register,FieldSet>> list = new ArrayList<Pair<Register,FieldSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> tuple = iterator.next();
    		if (tuple.val1 == r && tuple.val2 == FieldSet.emptyset())
    			list.add(new Pair<Register,FieldSet>(tuple.val0,tuple.val3));
    	}    	
    	return list;
    }

    public ArrayList<Pair<Register,FieldSet>> findTuplesBySecondRegisterEmpty2(Register r) {
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	ArrayList<Pair<Register,FieldSet>> list = new ArrayList<Pair<Register,FieldSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> tuple = iterator.next();
    		if (tuple.val1 == r && tuple.val3 == FieldSet.emptyset())
    			list.add(new Pair<Register,FieldSet>(tuple.val0,tuple.val2));
    	}    	
    	return list;
    }

    public ArrayList<Pair<Register,FieldSet>> findTuplesByReachingRegister(Register r) {
    	ArrayList<Pair<Register,FieldSet>> list1 = findTuplesByFirstRegisterEmpty2(r);
    	ArrayList<Pair<Register,FieldSet>> list2 = findTuplesBySecondRegisterEmpty1(r);
    	list1.addAll(list2);
    	return list1;
    }
        
    public ArrayList<Pair<Register,FieldSet>> findTuplesByReachedRegister(Register r) {
    	ArrayList<Pair<Register,FieldSet>> list1 = findTuplesByFirstRegisterEmpty1(r);
    	ArrayList<Pair<Register,FieldSet>> list2 = findTuplesBySecondRegisterEmpty2(r);
    	list1.addAll(list2);
    	return list1;
    }

    /** 
     * Returns all the field sets fs such that either (r1,r2,fs,{}) or (r2,r1,{},fs)
     * is in the relation.  In other words, r1 is the register fs-reaching r2.
     * 
     * @param r1 The reaching register 
     * @param r2 The reached register
     * @return all the field sets fs such that either (r1,r2,fs,{}) or
     * (r2,r1,{},fs) is in the relation
     */
    public ArrayList<FieldSet> findTuplesByReachingReachedRegister(Register r1, Register r2) {
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	ArrayList<FieldSet> list = new ArrayList<FieldSet>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> tuple = iterator.next();
    		if (tuple.val0 == r1 && tuple.val1 == r2 && tuple.val3 == FieldSet.emptyset())
    			list.add(tuple.val2);
    		if (tuple.val0 == r2 && tuple.val1 == r1 && tuple.val2 == FieldSet.emptyset())
    			list.add(tuple.val3);
    	}
    	return list;
    }
    
    /**
     * Finds all tuples in the relation whose first or second register is
     * {@code r}.
     * 
     * @param r
     * @return a list of trios (s,f1,f2) such that (s,{@code r},f1,f2) or
     * ({@code r},s,f1,f2) is in the relation.
     */
    public ArrayList<Trio<Register,FieldSet,FieldSet>> findTuplesByRegister(Register r) {
    	ArrayList<Trio<Register,FieldSet,FieldSet>> list1 = findTuplesByFirstRegister(r);
    	ArrayList<Trio<Register,FieldSet,FieldSet>> list2 = findTuplesBySecondRegister(r);
    	list1.addAll(list2);
    	return list1;
    }
    
    /**
     * Finds all tuples in the relation where one register is {@code r1} and
     * the other is {@code r2}.
	 * 
     * @param r1 The first register.
     * @param r2 The second register.
     * @return a list of pairs of field sets (f1,f2) such that
     * ({@code r1},{@code r2},f1,f2) is in the relation.
     */
    public ArrayList<Pair<FieldSet,FieldSet>> findTuplesByBothRegisters(Register r1,Register r2) {
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	ArrayList<Pair<FieldSet,FieldSet>> list = new ArrayList<Pair<FieldSet,FieldSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> pent = iterator.next();
    		if (pent.val0 == r1 && pent.val1 == r2)
    			list.add(new Pair<FieldSet,FieldSet>(pent.val2,pent.val3));
    		else if (pent.val0 == r2 && pent.val1 == r1)
    			list.add(new Pair<FieldSet,FieldSet>(pent.val3,pent.val2));
    	}    	
    	return list;
    }

	// WARNING: have to make sure that the iteration is point to the right element after remove()
	public void remove(Register r) {
		Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
		while (iterator.hasNext()) {
			Quad<Register,Register,FieldSet,FieldSet> tuple = iterator.next();
			if (tuple.val0 == r || tuple.val1 == r)	{
				iterator.remove();
				Utilities.info("REMOVED ( " + tuple.val0 + " , " + tuple.val1 + " , " + tuple.val2 + " , "+tuple.val3 +" ) FROM Share");
			}
		}
	}
	
	public void removeList(List<Register> list) {
		for (Register r : list) remove(r);
	}
	
	/**
	 * Makes a SHALLOW copy of its tuples and returns a new STuples object.
	 * The copy is shallow because Register and FieldSet objects need not to
	 * be duplicated
	 */
	public STuples clone() {
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> newTuples = new ArrayList<Quad<Register,Register,FieldSet,FieldSet>>();
		for (Quad<Register,Register,FieldSet,FieldSet> tuple : tuples) {
			newTuples.add(new Quad<Register,Register,FieldSet,FieldSet>(tuple.val0,tuple.val1,tuple.val2,tuple.val3));
		}
		return new STuples(newTuples);		
	}
	
	private void notifyTupleAdded(Register r1,Register r2,FieldSet fs1,FieldSet fs2) {
		Utilities.info("ADDED TO SHARE: ( " + r1 + ", " + r2 + ", " + fs1 + ", " + fs2 + " )");
	}

	public void filterActual(List<Register> actualParameters) {
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> newTuples = new ArrayList<Quad<Register,Register,FieldSet,FieldSet>>();
		for (Quad<Register,Register,FieldSet,FieldSet> tuple : tuples)
			if (actualParameters.contains(tuple.val0) && actualParameters.contains(tuple.val1))
				newTuples.add(tuple);
		tuples = newTuples;
	}
	
	public String toString() {
		String s = "";
		if (tuples.size()>0) {
			Quad<Register,Register,FieldSet,FieldSet> t = tuples.get(0);
			s = s + "(" + t.val0 + "," + t.val1 + "," + t.val2 + "," + t.val3 + ")";
			for (int i=1; i<tuples.size(); i++) { // index starts from 1 on purpose
				t = tuples.get(i);
				s = s + " - (" + t.val0 + "," + t.val1 + "," + t.val2 + "," + t.val3 + ")";
			}
		}
		return s;
	}

	public boolean isBottom() {
		return (tuples.size()==0);
	}

}
