package chord.analyses.jgbHeap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joeq.Class.jq_Method;
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
    name = "HeapShare",
    sign = "Register0,Register1,FieldSet1,FieldSet2:Register0xRegister1_FieldSet1xFieldSet2",
    consumes = { "V", "Register", "AbsField", "FieldSet", "UseDef" }
)
public class RelShare extends ProgramRel {
	
	AccumulatedTuples accumulatedTuples;
	
	public AccumulatedTuples getAccumulatedTuples(){ return this.accumulatedTuples; }
		
	public void fill() { }
	
    /**
     * This method does the job of copying tuples from a variable to another.
     * 
     * @param source The source variable.
     * @param dest The destination variable.
     * @return whether some tuples have been added
     */

    public Boolean copyTuples(Register source, Register dest) {
    	Utilities.out("COPY TUPLES OF " + source + " TO " + dest);
    	Boolean changed = false;
    	for (Trio<Register,FieldSet,FieldSet> t : findTuplesByFirstRegister(source))
    		changed |= condAdd(dest,t.val0,t.val1,t.val2);	
    	for (Trio<Register,FieldSet,FieldSet> t : findTuplesBySecondRegister(source))
    		changed |= condAdd(t.val0,dest,t.val1,t.val2);	
    	return changed;
    }
    
    public void removeTuples(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FieldSet,FieldSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	Quad<Register,Register,FieldSet,FieldSet> quad = null;
    	while (iterator.hasNext()) {
    		quad = iterator.next();
    		if (quad.val0 == r || quad.val1 == r) {
    			this.remove(quad.val0,quad.val1);
    			Utilities.debug("REMOVED ( " + quad.val0 + " , " + quad.val1 + " , " + quad.val2 + " , " + quad.val3 + " ) FROM Share");
    		}
    	}
    }
    
    public Boolean moveTuples(Register source, Register dest) {
    	Utilities.out("COPY TUPLES OF " + source + " TO " + dest);
    	removeTuples(dest);
    	boolean changed = copyTuples(source,dest);
    	removeTuples(source);
    	return changed;
    }
    
    public boolean joinTuples(Register source1, Register source2, Register dest) {
    	Utilities.out("JOIN TUPLES OF " + source1 + " AND "+source2 + " IN " + dest);
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
    	Trio<Register,FieldSet,FieldSet> t = null;
    	List<Trio<Register,FieldSet,FieldSet>> l1 = findTuplesByFirstRegister(source);
    	Iterator<Trio<Register,FieldSet,FieldSet>> it1 = l1.iterator();
    	while (it1.hasNext()) {
    		t = it1.next();
    		if (source != t.val0)
    			changed |= condAdd(dest,t.val0,t.val1,t.val2);
    		else changed |= condAdd(dest,dest,t.val1,t.val2);
    	}
    	List<Trio<Register,FieldSet,FieldSet>> l2 = findTuplesBySecondRegister(source);
    	Iterator<Trio<Register,FieldSet,FieldSet>> it2 = l2.iterator();
    	while (it2.hasNext()) {
    		t = it2.next();
    		if (source != t.val0)
    			changed |= condAdd(t.val0,dest,t.val1,t.val2);
    	}
    	return changed;
    }
    

	public Boolean copyTuplesFromCycle(Register source,Register dest,RelCycle relCycle) {

	 	Boolean changed = false;
    	FieldSet fs = null;
    	List<FieldSet> l = relCycle.findTuplesByRegister(source);
    	Iterator<FieldSet> it = l.iterator();
    	while (it.hasNext()) {
    		fs = it.next();
    		changed |= condAdd(dest,dest,fs,fs);
    		changed |= condAdd(dest,dest,FieldSet.emptyset(),fs);
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

    public Boolean condAdd(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {

    	if (r1 == r2) { // self-f-sharing
    		if (!FieldSet.leq(fs1,fs2)) { // the "smaller" field set goes first
    			FieldSet x = fs1;
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
    	DomFieldSet domFSet = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
    	for (FieldSet fs1 : domFSet.getAll()) {
    		for (FieldSet fs2 : domFSet.getAll()) {
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
    public List<Trio<Register,FieldSet,FieldSet>> findTuplesByFirstRegister(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FieldSet,FieldSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<Trio<Register,FieldSet,FieldSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> quad = iterator.next();
    		if (quad.val0 == r)
    			list.add(new Trio<Register,FieldSet,FieldSet>(quad.val1,quad.val2,quad.val3));
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
    public List<Pair<Register,FieldSet>> findTuplesByFirstRegisterEmpty1(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FieldSet,FieldSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Pair<Register,FieldSet>> list = new ArrayList<Pair<Register,FieldSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> quad = iterator.next();
    		if (quad.val0 == r && quad.val2 == FieldSet.emptyset())
    			list.add(new Pair<Register,FieldSet>(quad.val1,quad.val3));
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
    public List<Pair<Register,FieldSet>> findTuplesByFirstRegisterEmpty2(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FieldSet,FieldSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Pair<Register,FieldSet>> list = new ArrayList<Pair<Register,FieldSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> quad = iterator.next();
    		if (quad.val0 == r && quad.val3 == FieldSet.emptyset())
    			list.add(new Pair<Register,FieldSet>(quad.val1,quad.val2));
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
    public List<Trio<Register,FieldSet,FieldSet>> findTuplesBySecondRegister(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FieldSet,FieldSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<Trio<Register,FieldSet,FieldSet>>();
    	Quad<Register,Register,FieldSet,FieldSet> quad = null;
    	while (iterator.hasNext()) {
    		quad = iterator.next();
    		if (quad.val1 == r)
    			list.add(new Trio<Register,FieldSet,FieldSet>(quad.val0,quad.val2,quad.val3));
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
    public List<Pair<Register,FieldSet>> findTuplesBySecondRegisterEmpty1(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FieldSet,FieldSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Pair<Register,FieldSet>> list = new ArrayList<Pair<Register,FieldSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> quad = iterator.next();
    		if (quad.val1 == r && quad.val2 == FieldSet.emptyset())
    			list.add(new Pair<Register,FieldSet>(quad.val0,quad.val3));
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
    public List<Pair<Register,FieldSet>> findTuplesBySecondRegisterEmpty2(Register r) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FieldSet,FieldSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Pair<Register,FieldSet>> list = new ArrayList<Pair<Register,FieldSet>>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> quad = iterator.next();
    		if (quad.val1 == r && quad.val3 == FieldSet.emptyset())
    			list.add(new Pair<Register,FieldSet>(quad.val0,quad.val2));
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
    public List<Pair<Register,FieldSet>> findTuplesByReachingRegister(Register r) {
    	List<Pair<Register,FieldSet>> list1 = findTuplesByFirstRegisterEmpty2(r);
    	List<Pair<Register,FieldSet>> list2 = findTuplesBySecondRegisterEmpty1(r);
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
    public List<Pair<Register,FieldSet>> findTuplesByReachedRegister(Register r) {
    	List<Pair<Register,FieldSet>> list1 = findTuplesByFirstRegisterEmpty1(r);
    	List<Pair<Register,FieldSet>> list2 = findTuplesBySecondRegisterEmpty2(r);
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
    public List<FieldSet> findTuplesByReachingReachedRegister(Register r1, Register r2) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FieldSet,FieldSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<FieldSet> list = new ArrayList<FieldSet>();
    	while (iterator.hasNext()) {
    		Quad<Register,Register,FieldSet,FieldSet> quad = iterator.next();
    		if (quad.val0 == r1 && quad.val1 == r2 && quad.val3 == FieldSet.emptyset())
    			list.add(quad.val2);
    		if (quad.val0 == r2 && quad.val1 == r1 && quad.val2 == FieldSet.emptyset())
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
    public List<Trio<Register,FieldSet,FieldSet>> findTuplesByRegister(Register r) {
    	List<Trio<Register,FieldSet,FieldSet>> list1 = findTuplesByFirstRegister(r);
    	List<Trio<Register,FieldSet,FieldSet>> list2 = findTuplesBySecondRegister(r);
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
    public List<Pair<FieldSet,FieldSet>> findTuplesByBothRegisters(Register r1,Register r2) {
    	RelView view = getView();
    	QuadIterable<Register,Register,FieldSet,FieldSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Pair<FieldSet,FieldSet>> list = new ArrayList<Pair<FieldSet,FieldSet>>();
    	Quad<Register,Register,FieldSet,FieldSet> quad = null;
    	while (iterator.hasNext()) {
    		quad = iterator.next();
    		if (quad.val0 == r1 && quad.val1 == r2)
    			list.add(new Pair<FieldSet,FieldSet>(quad.val2,quad.val3));
    	}    	
    	return list;
    }
    
    /**
     * Pretty-prints all the tuples in the relation.
     */
    public void output() {
    	RelView view = getView();
    	QuadIterable<Register,Register,FieldSet,FieldSet> tuples = view.getAry4ValTuples();
    	Iterator<Quad<Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	Quad<Register,Register,FieldSet,FieldSet> quad = null;
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
    	List<Pair<FieldSet,FieldSet>> l = findTuplesByBothRegisters(r1,r2);
    	System.out.println("SHARING BETWEEN " + r1 + " AND " + r2 + " = ");
    	Iterator<Pair<FieldSet,FieldSet>> iterator = l.listIterator();
    	while (iterator.hasNext()) {
    		Pair<FieldSet,FieldSet> p = iterator.next();
    		System.out.println(" * " + p.val0 + " - " + p.val1);
    	}
    }
    
}	
