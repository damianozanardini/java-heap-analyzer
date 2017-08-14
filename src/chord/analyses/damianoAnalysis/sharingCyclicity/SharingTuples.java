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
 * This class implements a list of sharing tuples.
 * 
 * @author damiano
 *
 */
public class SharingTuples extends Tuples {

	/**
	 * The abstract sharing information.
	 */
	private ArrayList<SharingTuple> tuples;
	
	/**
	 * Default constructor.
	 */
	public SharingTuples() {
		tuples = new ArrayList<SharingTuple>();
	}
	
	/**
	 * Constructor which takes the astract information from the argument.
	 * 
	 * @param otherTuples
	 */
	public SharingTuples(ArrayList<SharingTuple> otherTuples) {
		tuples = otherTuples;
	}
	
	/**
	 * Joins the information stored in {@code this} with the information stored in
	 * {@code others}.  For sharing, this amount to list union without duplicates.
	 * 
	 * @param others
	 * @return
	 */
	boolean join(SharingTuples others) {
		boolean b = false;
		for (SharingTuple t : others.getInfo()) {
			if (!contains(t)) {
				tuples.add(t);
				b = true;
			}
		}
		sort();
		return b;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<SharingTuple> getInfo() {
		return tuples;
	}
	
	/**
	 * Set the abstract information to the given information.
	 * 
	 * @param tuples
	 */
	public void setInfo(ArrayList<SharingTuple> tuples) {
		this.tuples = tuples;
	}

	/**
	 * Tuples are added as ordered tuples: in a tuple {@code (r1,r2,fs1,fs2)},
	 * {@code r1} must be less than or equal to {@code r2}.  If {@code r1==r2},
	 * then the pair of fieldsets is ordered.
	 * <p>
	 * Examples:
	 * <li> in {@code addTuple(R3,R5,fs1,fs2)}, the tuple is added as it is;
	 * <li> in {@code addTuple(R5,R3,fs1,fs2)}, the tuple {@code (R3,R5,fs2,fs1)}
	 * is added;
	 * <li> in {@code addTuple(r,r,fs1,fs2)}, the tuple
	 * {@code (r,r,min(fs1,fs2),max(fs1,fs2))} is added.
	 *  
	 * @param r1
	 * @param r2
	 * @param fs1
	 * @param fs2
	 */
	public void addTuple(Register r1,Register r2,FieldSet fs1,FieldSet fs2) {
		for (SharingTuple t : tuples) {
			if (t.getR1() == r1 && t.getR2() == r2 && t.getFs1() == fs1 && t.getFs2() == fs2)
				return;
			if (t.getR1() == r2 && t.getR2() == r1 && t.getFs1() == fs2 && t.getFs2() == fs1)
				return;
		}
		if (r1 == r2) 
			tuples.add(new SharingTuple(r1,r2,FieldSet.min(fs1,fs2),FieldSet.max(fs1,fs2)));
		else if (Utilities.leqReg(r1,r2))
			tuples.add(new SharingTuple(r1,r2,fs1,fs2));
		else
			tuples.add(new SharingTuple(r2,r1,fs2,fs1));
	}		

	/**
	 * Adds a new {@code SharingTuple} object to the sharing information.
	 * 
	 * @param t
	 */
	private void addTuple(SharingTuple t) {
		if (t!=null) addTuple(t.getR1(),t.getR2(),t.getFs1(),t.getFs2());
	}

	public void copyInfo(Register source,Register dest) {
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
	
	/**
	 * Copies sharing information from register {@code source} of another
	 * {@code SharingTuples} object to register {@code dest} of the current object.
	 * 
	 * @param other The other {@code SharingTuples} object
	 * @param source The source register
	 * @param dest The destination register
	 */
	public void copyInfoFrom(SharingTuples other,Register source,Register dest) {
		if (source==null || dest==null) return;
		ArrayList<SharingTuple> newTuples = new ArrayList<SharingTuple>();
		for (SharingTuple t : other.getInfo()) {
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

	public void moveInfo(Register source,Register dest) {
		for (SharingTuple t : tuples) {
			if (t.getR1() == source && t.getR2() == source) {
				t.setRs(dest,dest);
			} else if (t.getR1() == source) t.setR1(dest);
			else if (t.getR2() == source) t.setR2(dest);
		}
	}

	/**
	 * Copies cyclicity information about {@code source} (stored in {@code cTuples}
	 * into self-sharing information about {@code dest}.  The newly added information
	 * is not always precise because of the frying-pan shape: if the cycle was not
	 * directly reachable, then self-sharing is not implied by cyclicity.
	 * 
	 * @param source
	 * @param dest
	 * @param ctuples
	 */
	public void copyTuplesFromCycle(Register source,Register dest,CyclicityTuples ctuples) {
    		FieldSet fs = null;
    		List<FieldSet> l = ctuples.findTuplesByRegister(source);
    		Iterator<FieldSet> it = l.iterator();
    		while (it.hasNext()) {
    			fs = it.next();
    			addTuple(dest,dest,fs,fs);
    		}
	}
	
    /** 
     * Returns all the tuples {@code r2,fs1,fs2)} such that either
     * {@code (r,r2,fs1,fs2)} is in the abstract information.
     * 
     * @param r The searched-for register 
     * @return
     */
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
        
    /** 
     * Returns all the tuples {@code r1,fs1,fs2)} such that either
     * {@code (r1,r,fs1,fs2)} is in the abstract information.
     * 
     * @param r The searched-for register 
     * @return
     */
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
     * Finds all tuples whose first or second register is {@code r}.
     * 
     * @param r
     * @return a list of trios {@code (s,fs1,fs2)} such that
     * {@code (s,r,fs1,fs2)} or {@code (r,s,f1,f2)} is in the relation.
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
     * @return a list of pairs of fieldsets {@code (fs1,fs2)} such that
     * {@code (r1,r2,fs1,fs2)} is in the relation.
     */
    public ArrayList<Pair<FieldSet,FieldSet>> findTuplesByBothRegisters(Register r1,Register r2) {
		ArrayList<Pair<FieldSet,FieldSet>> list = new ArrayList<Pair<FieldSet,FieldSet>>();
    		for (Iterator<SharingTuple> it = tuples.iterator(); it.hasNext(); ) {
    			SharingTuple tuple = it.next();
    			if (tuple.getR1() == r1 && tuple.getR2() == r2)
    				list.add(new Pair<FieldSet,FieldSet>(tuple.getFs1(),tuple.getFs2()));
    			else if (tuple.getR1() == r2 && tuple.getR2() == r1)
    				list.add(new Pair<FieldSet,FieldSet>(tuple.getFs2(),tuple.getFs1()));
    		}    	
    		return list;
    }

	public void remove(Register r) {
		for (Iterator<SharingTuple> it = tuples.iterator(); it.hasNext(); ) {
			SharingTuple tuple = it.next();
			if (tuple.getR1() == r || tuple.getR2() == r) {
				it.remove();
			}
		}
	}
	
	public SharingTuples clone() {
		ArrayList<SharingTuple> newTuples = new ArrayList<SharingTuple>();
		for (SharingTuple tuple : tuples) {
			newTuples.add(new SharingTuple(tuple.getR1(),tuple.getR2(),tuple.getFs1(),tuple.getFs2()));
		}
		return new SharingTuples(newTuples);		
	}
	
	public void filterActual(List<Register> actualParameters) {
		for (Iterator<SharingTuple> it = tuples.iterator(); it.hasNext(); ) {
			SharingTuple t = it.next();
			if (!actualParameters.contains(t.getR1()) || !actualParameters.contains(t.getR2()))
				it.remove();
		}
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
		if (tuple instanceof SharingTuple) {			
			for (SharingTuple t : tuples)
				if (((SharingTuple) tuple).equals(t)) return true;
			return false;
		} else return false;
	}
	
}
