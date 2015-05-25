package chord.analyses.damianoCyclicity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Utilities;
import chord.bddbddb.Rel.QuadIterable;
import chord.bddbddb.Rel.RelView;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;

/**
 * Relation containing sharing tuples (r1,r2,fs1,fs2) specifying that
 * the sets of fields (truth assignments) fs1 and fs2 are models of the
 * path-formulae F1 and F2 such that, for every shared location l, 
 * r1 F1-reaches l and r2 F2-reaches l.
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
@Chord(
    name = "Share",
    sign = "Register0,Register1,FSet1,FSet2:Register0xRegister1_FSet1xFSet2",
    consumes = { "V", "Register", "AbsF", "FSet", "UseDef" }
)
public class RelShare extends ProgramRel {
	
	AccumulatedTuples accumulatedTuples;
	
	public void fill() { }
	
    /**
     * This method does the job of copying tuples from a variable to another.
     * 
     * @param source The source variable.
     * @param dest The destination variable.
     * @return whether some tuples have been added
     */
    public Boolean copyTuples(Register source, Register dest) {
    	Boolean changed = false;
    	for (Trio<Register,FSet,FSet> t : findTuplesByFirstRegister(source))
    		changed |= condAdd(dest,t.val0,t.val1,t.val2);	
    	for (Trio<Register,FSet,FSet> t : findTuplesBySecondRegister(source))
    		changed |= condAdd(t.val0,dest,t.val1,t.val2);	
    	return changed;
    }
    
    public void removeTuples(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FSet,FSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FSet,FSet>> iterator = tuples.iterator();
    	Quad<Register,Register,FSet,FSet> quad = null;
    	while (iterator.hasNext()) {
    		quad = iterator.next();
    		if (quad.val0 == r || quad.val1 == r) {
    			this.remove(quad.val0,quad.val1);
    			Utilities.debug("REMOVED ( " + quad.val0 + " , " + quad.val1 + " , " + quad.val2 + " , " + quad.val3 + " ) FROM Share");
    		}
    	}
    }
    
    public Boolean moveTuples(Register source, Register dest) {
    	removeTuples(dest);
    	boolean changed = copyTuples(source,dest);
    	removeTuples(source);
    	return changed;
    }
    
    public boolean joinTuples(Register source1, Register source2, Register dest) {
    	removeTuples(dest);
    	boolean changed = false;
    	changed |= copyTuples(source1, dest);
    	changed |= copyTuples(source2, dest);
    	// removeTuples(source1);
    	// removeTuples(source2);
    	return changed;
    }
    
    /**
     * The same as copyTuples, but does not copy (source,source,fs1,fs2) into
     * (source,dest,fs1,fs2) and (dest,source,fs1,fs2), just to
     * (dest,dest,fs1,fs2)
     * 
     * @param source The source variable.
     * @param dest The source variable.
     * @return whether some tuples have been added
     */
    public Boolean copyTuplesPhi(Register source, Register dest) {
    	Boolean changed = false;
    	Trio<Register,FSet,FSet> t = null;
    	List<Trio<Register,FSet,FSet>> l1 = findTuplesByFirstRegister(source);
    	Iterator<Trio<Register,FSet,FSet>> it1 = l1.iterator();
    	while (it1.hasNext()) {
    		t = it1.next();
    		if (source != t.val0)
    			changed |= condAdd(dest,t.val0,t.val1,t.val2);
    		else changed |= condAdd(dest,dest,t.val1,t.val2);
    	}
    	List<Trio<Register,FSet,FSet>> l2 = findTuplesBySecondRegister(source);
    	Iterator<Trio<Register,FSet,FSet>> it2 = l2.iterator();
    	while (it2.hasNext()) {
    		t = it2.next();
    		if (source != t.val0)
    			changed |= condAdd(t.val0,dest,t.val1,t.val2);
    	}
    	return changed;
    }
    
	public Boolean copyTuplesFromCycle(Register source,Register dest,RelCycle relCycle) {
	 	Boolean changed = false;
    	FSet fs = null;
    	List<FSet> l = relCycle.findTuplesByRegister(source);
    	Iterator<FSet> it = l.iterator();
    	while (it.hasNext()) {
    		fs = it.next();
    		changed |= condAdd(dest,dest,fs,fs);
    		changed |= condAdd(dest,dest,FSet.emptyset(),fs);
    	}
    	return changed;
	}
    
    /**
     * Adds a sharing statement if it does not already belong to the
     * relation.
     * 
     * @param r1 The first register of the tuple.
     * @param r2 The second register of the tuple.
     * @param fs1 The first field set (third element of the tuple).
     * @param fs2 The second field set (fourth element of the tuple).
     * @return a boolean value specifying if the tuple is a new one.
     */
    public Boolean condAdd(Register r1, Register r2, FSet fs1, FSet fs2) {
    	if (r1 == r2) { // self-f-sharing
    		if (!FSet.leq(fs1,fs2)) { // the "smaller" field set goes first
    			FSet x = fs1;
    			fs1 = fs2;
    			fs2 = x;
    		}
    	}
    	if (!contains(r1,r2,fs1,fs2)) {
    		add(r1,r2,fs1,fs2);
    		Utilities.debug("ADDED ( " + r1 + " , " + r2 + " , " + fs1 + " , " + fs2 + ") TO Share");
    	}
    	return accumulatedTuples.condAdd(r1,r2,fs1,fs2);
    }
 
    /**
     * Adds a sharing statement from r1 to r2 for every field set.
     * 
     * @param r1 The first register of the tuple.
     * @param r2 The second register of the tuple.
     * @return a boolean value specifying if some tuple is a new one.
     */
    public Boolean condAddTrue(Register r1, Register r2) {
    	Boolean x = false;
    	DomFSet domFSet = (DomFSet) ClassicProject.g().getTrgt("FSet");
    	for (FSet fs1 : domFSet.getAll()) {
    		for (FSet fs2 : domFSet.getAll()) {
    			x |= condAdd(r1,r2,fs1,fs2);
    		}
    	}
    	return x;
    }
    
    /**
     * Finds all tuples in the relation whose first register is {@code r}.
     * 
     * @param r
     * @return a list of trios (s,f1,f2) such that ({@code r},s,f1,f2) is in the relation.
     */
    public List<Trio<Register,FSet,FSet>> findTuplesByFirstRegister(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FSet,FSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FSet,FSet>> iterator = tuples.iterator();
    	List<Trio<Register,FSet,FSet>> list = new ArrayList<Trio<Register,FSet,FSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FSet,FSet> quad = iterator.next();
    		if (quad.val0 == r)
    			list.add(new Trio<Register,FSet,FSet>(quad.val1,quad.val2,quad.val3));
    	}    	
    	return list;
    }
    
    /**
     * Finds all tuples in the relation whose first register is {@code r} and
     * the first field set is empty.
     * 
     * @param r
     * @return a list of pairs (s,f2) such that ({@code r},s,{},f2) is in the
     * relation.
     */
    public List<Pair<Register,FSet>> findTuplesByFirstRegisterEmpty1(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FSet,FSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FSet,FSet>> iterator = tuples.iterator();
    	List<Pair<Register,FSet>> list = new ArrayList<Pair<Register,FSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FSet,FSet> quad = iterator.next();
    		if (quad.val0 == r && quad.val2 == FSet.emptyset())
    			list.add(new Pair<Register,FSet>(quad.val1,quad.val3));
    	}    	
    	return list;
    }
    
    /**
     * Finds all tuples in the relation whose first register is {@code r} and
     * the second field set is empty.
     * 
     * @param r
     * @return a list of pairs (s,f1) such that ({@code r},s,f1,{}) is in the
     * relation.
     */
    public List<Pair<Register,FSet>> findTuplesByFirstRegisterEmpty2(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FSet,FSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FSet,FSet>> iterator = tuples.iterator();
    	List<Pair<Register,FSet>> list = new ArrayList<Pair<Register,FSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FSet,FSet> quad = iterator.next();
    		if (quad.val0 == r && quad.val3 == FSet.emptyset())
    			list.add(new Pair<Register,FSet>(quad.val1,quad.val2));
    	}    	
    	return list;
    }

    /**
     * Finds all tuples in the relation whose second register is {@code r}.
     * 
     * @param r
     * @return a list of trios (s,f1,f2) such that (s,{@code r},f1,f2) is in
     * the relation.
     */
    public List<Trio<Register,FSet,FSet>> findTuplesBySecondRegister(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FSet,FSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FSet,FSet>> iterator = tuples.iterator();
    	List<Trio<Register,FSet,FSet>> list = new ArrayList<Trio<Register,FSet,FSet>>();
    	Quad<Register,Register,FSet,FSet> quad = null;
    	while (iterator.hasNext()) {
    		quad = iterator.next();
    		if (quad.val1 == r)
    			list.add(new Trio<Register,FSet,FSet>(quad.val0,quad.val2,quad.val3));
    	}    	
    	return list;
    }

    /**
     * Finds all tuples in the relation whose second register is {@code r} and
     * the first field set is empty.
     * 
     * @param r
     * @return a list of pairs (s,f2) such that (s,{@code r},{},f2) is in the
     * relation.
     */
    public List<Pair<Register,FSet>> findTuplesBySecondRegisterEmpty1(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FSet,FSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FSet,FSet>> iterator = tuples.iterator();
    	List<Pair<Register,FSet>> list = new ArrayList<Pair<Register,FSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FSet,FSet> quad = iterator.next();
    		if (quad.val1 == r && quad.val2 == FSet.emptyset())
    			list.add(new Pair<Register,FSet>(quad.val0,quad.val3));
    	}    	
    	return list;
    }
    
    /**
     * Finds all tuples in the relation whose second register is {@code r} and
     * the second field set is empty.
     * 
     * @param r
     * @return a list of pairs (s,f1) such that (s,{@code r},f1,{}) is in the
     * relation.
     */
    public List<Pair<Register,FSet>> findTuplesBySecondRegisterEmpty2(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FSet,FSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FSet,FSet>> iterator = tuples.iterator();
    	List<Pair<Register,FSet>> list = new ArrayList<Pair<Register,FSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FSet,FSet> quad = iterator.next();
    		if (quad.val1 == r && quad.val3 == FSet.emptyset())
    			list.add(new Pair<Register,FSet>(quad.val0,quad.val2));
    	}    	
    	return list;
    }

    /** 
     * Returns all the pairs (s,fs) such that either (r,s,fs,{}) or (s,r,{},fs)
     * is in the relation.  In other words, s is the register fs-reached by r.
     * 
     * @param r
     * @return
     */
    public List<Pair<Register,FSet>> findTuplesByReachingRegister(Register r) {
    	List<Pair<Register,FSet>> list1 = findTuplesByFirstRegisterEmpty2(r);
    	List<Pair<Register,FSet>> list2 = findTuplesBySecondRegisterEmpty1(r);
    	list1.addAll(list2);
    	return list1;
    }
        
    /** 
     * Returns all the pairs (s,fs) such that either (r,s,{},fs) or (s,r,fs,{})
     * is in the relation.  In other words, s is the register fs-reaching r.
     * 
     * @param r
     * @return
     */
    public List<Pair<Register,FSet>> findTuplesByReachedRegister(Register r) {
    	List<Pair<Register,FSet>> list1 = findTuplesByFirstRegisterEmpty1(r);
    	List<Pair<Register,FSet>> list2 = findTuplesBySecondRegisterEmpty2(r);
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
    public List<FSet> findTuplesByReachingReachedRegister(Register r1, Register r2) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FSet,FSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FSet,FSet>> iterator = tuples.iterator();
    	List<FSet> list = new ArrayList<FSet>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FSet,FSet> quad = iterator.next();
    		if (quad.val0 == r1 && quad.val1 == r2 && quad.val3 == FSet.emptyset())
    			list.add(quad.val2);
    		if (quad.val0 == r2 && quad.val1 == r1 && quad.val2 == FSet.emptyset())
    			list.add(quad.val3);
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
    public List<Trio<Register,FSet,FSet>> findTuplesByRegister(Register r) {
    	List<Trio<Register,FSet,FSet>> list1 = findTuplesByFirstRegister(r);
    	List<Trio<Register,FSet,FSet>> list2 = findTuplesBySecondRegister(r);
    	list1.addAll(list2);
    	return list1;
    }
    
    /**
     * Finds all tuples in the relation whose first register is {@code r1} and
     * second register is {@code r2}.
	 * 
     * @param r1 The first register.
     * @param r2 The second register.
     * @return a list of pairs of field sets (f1,f2) such that
     * ({@code r1},{@code r2},f1,f2) is in the relation.
     */
    public List<Pair<FSet,FSet>> findTuplesByBothRegisters(Register r1,Register r2) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FSet,FSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FSet,FSet>> iterator = tuples.iterator();
    	List<Pair<FSet,FSet>> list = new ArrayList<Pair<FSet,FSet>>();
    	Quad<Register,Register,FSet,FSet> quad = null;
    	while (iterator.hasNext()) {
    		quad = iterator.next();
    		if (quad.val0 == r1 && quad.val1 == r2)
    			list.add(new Pair<FSet,FSet>(quad.val2,quad.val3));
    	}    	
    	return list;
    }
    
    /**
     * Pretty-prints all the tuples in the relation.
     */
    public void output() {
    	RelView view = getView();
    	QuadIterable<Register,Register,FSet,FSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FSet,FSet>> iterator = tuples.iterator();
    	Quad<Register,Register,FSet,FSet> quad = null;
    	while (iterator.hasNext()) {
    		quad = iterator.next();
    		System.out.println("SHARE STATEMENT: " + quad.val0 + " --> " + quad.val2 + " / " + quad.val3 + " <-- " + quad.val1);
    	}    	
    }
    
    /**
     * Pretty-prints the sharing information about the n1-nth and the
     * n2-nth local variable.
     * 
     * @param n1 The position of the source register among local variables.
     * @param n2 The position of the target register among local variables.
     */
    public void askFor(Register r1, Register r2) {
    	List<Pair<FSet,FSet>> l = findTuplesByBothRegisters(r1,r2);
    	System.out.println("SHARING BETWEEN " + r1 + " AND " + r2 + " = ");
    	Iterator<Pair<FSet,FSet>> iterator = l.listIterator();
    	while (iterator.hasNext()) {
    		Pair<FSet,FSet> p = iterator.next();
    		System.out.println(" * " + p.val0 + " - " + p.val1);
    	}
    }
    
}	
