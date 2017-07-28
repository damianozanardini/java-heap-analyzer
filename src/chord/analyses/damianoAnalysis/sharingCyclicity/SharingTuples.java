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

public class SharingTuples extends Tuples {

	private ArrayList<SharingTuple> tuples;
	
	public SharingTuples() {
		tuples = new ArrayList<SharingTuple>();
	}
	
	public SharingTuples(ArrayList<SharingTuple> tuples) {
		this.tuples = tuples;
	}
	
	boolean join(SharingTuples others) {
		boolean b = false;
		for (SharingTuple t : others.getTuples()) {
			if (!tuples.contains(t)) {
				tuples.add(t);
				b = true;
			}
		}
		sort();
		return b;
	}

	public ArrayList<SharingTuple> getTuples() {
		return tuples;
	}
	
	public void setTuples(ArrayList<SharingTuple> tuples) {
		this.tuples = tuples;
	}
	
	public void addTuple(Register r1,Register r2,FieldSet fs1,FieldSet fs2) {
		boolean found = false;
		if (r1 == r2) {
			for (SharingTuple t : tuples) {
				found |= (t.getR1() == r1 && t.getR2() == r2 && t.getFs1() == fs1 && t.getFs2() == fs2);
				found |= (t.getR1() == r1 && t.getR2() == r2 && t.getFs1() == fs2 && t.getFs2() == fs1);
			}
			if (!found) 	tuples.add(new SharingTuple(r1,r2,fs1,fs2));
		} else {
			for (SharingTuple t : tuples) {
				found |= (t.getR1() == r1 && t.getR2() == r2 && t.getFs1() == fs1 && t.getFs2() == fs2);
				found |= (t.getR1() == r2 && t.getR2() == r1 && t.getFs1() == fs2 && t.getFs2() == fs1);
			}
			if (!found) {
				tuples.add(new SharingTuple(r1,r2,fs1,fs2));
			}
		}
	}

	public void addTuple(SharingTuple t) {
		if (t!=null) addTuple(t.getR1(),t.getR2(),t.getFs1(),t.getFs2());
	}

	public void copyTuples(Register source,Register dest) {
		if (source==null || dest==null) return;
		ArrayList<SharingTuple> newTuples = new ArrayList<SharingTuple>();
		for (SharingTuple t : tuples) {
			if (t.getR1() == source && t.getR2() == source) {
				newTuples.add(new SharingTuple(dest,dest,t.getFs1(),t.getFs2()));
				newTuples.add(new SharingTuple(Utilities.minReg(source,dest),Utilities.maxReg(source,dest),FieldSet.emptyset(),FieldSet.emptyset()));
			} else if (t.getR1() == source) {
				newTuples.add(new SharingTuple(dest,t.getR2(),t.getFs1(),t.getFs2()));
			} else if (t.getR2() == source) {
				newTuples.add(new SharingTuple(t.getR1(),dest,t.getFs1(),t.getFs2()));
			}
		}
		for (SharingTuple t : newTuples) addTuple(t);
	}
	
	public void moveTuples(Register source,Register dest) {
		for (SharingTuple t : tuples) {
			if (t.getR1() == source && t.getR2() == source) {
				t.setRs(dest,dest);
			} else if (t.getR1() == source) t.setR1(dest);
			else if (t.getR2() == source) t.setR2(dest);
		}
	}

	public void copyTuplesFromCycle(Register source,Register dest,CyclicityTuples ctuples) {
    		FieldSet fs = null;
    		List<FieldSet> l = ctuples.findTuplesByRegister(source);
    		Iterator<FieldSet> it = l.iterator();
    		while (it.hasNext()) {
    			fs = it.next();
    			addTuple(dest,dest,fs,fs);
    			// WARNING: This is unsound (frying-pan shape)
    			// addTuple(dest,dest,FieldSet.emptyset(),fs);
    		}
	}
	
    private ArrayList<Trio<Register,FieldSet,FieldSet>> findTuplesByFirstRegister(Register r) {
    		Iterator<SharingTuple> iterator = tuples.iterator();
    		ArrayList<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<Trio<Register,FieldSet,FieldSet>>();
    		while (iterator.hasNext()) {
    			SharingTuple tuple = iterator.next();
    			if (tuple.getR1() == r)
    				list.add(new Trio<Register,FieldSet,FieldSet>(tuple.getR2(),tuple.getFs1(),tuple.getFs2()));
    		}    	
    		return list;
    }
        
    private ArrayList<Trio<Register, FieldSet, FieldSet>> findTuplesBySecondRegister(Register r) {
    		Iterator<SharingTuple> iterator = tuples.iterator();
    		ArrayList<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<Trio<Register,FieldSet,FieldSet>>();
    		while (iterator.hasNext()) {
    			SharingTuple tuple = iterator.next();
    			if (tuple.getR2() == r)
    				list.add(new Trio<Register,FieldSet,FieldSet>(tuple.getR1(),tuple.getFs1(),tuple.getFs2()));
    		}    	
    		return list;
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
    		Iterator<SharingTuple> iterator = tuples.iterator();
    		ArrayList<FieldSet> list = new ArrayList<FieldSet>();
    		while (iterator.hasNext()) {
    			SharingTuple tuple = iterator.next();
    			if (tuple.getR1() == r1 && tuple.getR2() == r2 && tuple.getFs2() == FieldSet.emptyset())
    				list.add(tuple.getFs1());
    			if (tuple.getR1() == r2 && tuple.getR2() == r1 && tuple.getFs1() == FieldSet.emptyset())
    				list.add(tuple.getFs2());
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
    		Iterator<SharingTuple> iterator = tuples.iterator();
    		ArrayList<Pair<FieldSet,FieldSet>> list = new ArrayList<Pair<FieldSet,FieldSet>>();
    		while (iterator.hasNext()) {
    			SharingTuple tuple = iterator.next();
    			if (tuple.getR1() == r1 && tuple.getR2() == r2)
    				list.add(new Pair<FieldSet,FieldSet>(tuple.getFs1(),tuple.getFs2()));
    			else if (tuple.getR1() == r2 && tuple.getR2() == r1)
    				list.add(new Pair<FieldSet,FieldSet>(tuple.getFs2(),tuple.getFs1()));
    		}    	
    		return list;
    }

	// WARNING: have to make sure that the iteration is point to the right element after remove()
	public void remove(Register r) {
		Iterator<SharingTuple> iterator = tuples.iterator();
		while (iterator.hasNext()) {
			SharingTuple tuple = iterator.next();
			if (tuple.getR1() == r || tuple.getR2() == r)	{
				iterator.remove();
			}
		}
	}
	
	/**
	 * Makes a SHALLOW copy of its tuples and returns a new STuples object.
	 * The copy is shallow because Register and FieldSet objects need not to
	 * be duplicated
	 */
	public SharingTuples clone() {
		ArrayList<SharingTuple> newTuples = new ArrayList<SharingTuple>();
		for (SharingTuple tuple : tuples) {
			newTuples.add(new SharingTuple(tuple.getR1(),tuple.getR2(),tuple.getFs1(),tuple.getFs2()));
		}
		return new SharingTuples(newTuples);		
	}
	
	public void filterActual(List<Register> actualParameters) {
		ArrayList<SharingTuple> newTuples = new ArrayList<SharingTuple>();
		for (SharingTuple tuple : tuples)
			if (actualParameters.contains(tuple.getR1()) && actualParameters.contains(tuple.getR2()))
				newTuples.add(tuple);
		tuples = newTuples;
	}
	
	public String toString() {
		String s = "";
		if (tuples.size()>0) {
			SharingTuple t = tuples.get(0);
			s = s + "(" + t.getR1() + "," + t.getR2() + "," + t.getFs1() + "," + t.getFs2() + ")";
			for (int i=1; i<tuples.size(); i++) { // index starts from 1 on purpose
				t = tuples.get(i);
				s = s + " - (" + t.getR1() + "," + t.getR2() + "," + t.getFs1() + "," + t.getFs2() + ")";
			}
		}
		return s;
	}

	public boolean contains(Tuple tuple) {
		if (tuple instanceof SharingTuple) {
			boolean found = false;
			for (SharingTuple t : tuples) found |= (tuple.equals(t));
			return found;
		} else return false;
	}

	public void sort() {
		Collections.sort(tuples);
	}
	
}
