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
import chord.util.tuple.object.Pent;
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
    sign = "Entry,Register0,Register1,FieldSet1,FieldSet2:Entry_Register0xRegister1_FieldSet1xFieldSet2",
    consumes = { "V", "Register", "AbsField", "FieldSet", "UseDef", "Entry" }
)
public class RelShare extends ProgramRel {
	
	AccumulatedTuples accumulatedTuples;
	
	public AccumulatedTuples getAccumulatedTuples(){ return this.accumulatedTuples; }
		
	
	public void setIterable(PentIterable<Entry,Register,Register,FieldSet,FieldSet> it){
		ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>> list = new ArrayList<>();
		Iterator<Pent<Entry,Register,Register,FieldSet,FieldSet>> i = it.iterator();
		while(i.hasNext()){
			Pent<Entry,Register,Register,FieldSet,FieldSet> p = i.next();
			list.add(new Pent<>(p.val0,p.val1,p.val2,p.val3,p.val4));
		}
		for(Pent<Entry,Register,Register,FieldSet,FieldSet> s : list)
			add(s.val0,s.val1,s.val2,s.val3,s.val4);
	}
	
	public void fill() { }
	
    /**
     * This method does the job of copying tuples from a variable to another.
     * 
     * @param source The source variable.
     * @param dest The destination variable.
     * @return whether some tuples have been added
     */

    public Boolean copyTuples(Entry e, Register source, Register dest) {
    	Utilities.out("COPY TUPLES IN " +e+" OF " + source + " TO " + dest);
    	Boolean changed = false;
  
    	for (Trio<Register,FieldSet,FieldSet> t : findTuplesByFirstRegister(e,source))
    		changed |= condAdd(e,dest,t.val0,t.val1,t.val2);	
    	for (Trio<Register,FieldSet,FieldSet> t : findTuplesBySecondRegister(e,source))
    		changed |= condAdd(e,t.val0,dest,t.val1,t.val2);	
    	return changed;
    }
    
    
    public void removeTuples(Entry e, Register r) {
    	RelView view = getView();
    	PentIterable<Entry,Register,Register,FieldSet,FieldSet> tuples = view.getAry5ValTuples();
    	Iterator<Pent<Entry,Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	Pent<Entry,Register,Register,FieldSet,FieldSet> pent = null;
    	while (iterator.hasNext()) {
    		pent = iterator.next();
    		if (pent.val0 == e && (pent.val1 == r || pent.val2 == r)) {
    			this.remove(pent.val0,pent.val1,pent.val2);
    			Utilities.debug("REMOVED ( " + pent.val0 + " , " + pent.val1 + " , " + pent.val2 + " , " + pent.val3 + " , "+pent.val4+" ) FROM Share");
    		}
    	}
    }
    
    public Boolean moveTuples(Entry e, Register source, Register dest) {
    	Utilities.out("COPY TUPLES OF " + source + " TO " + dest);
    	removeTuples(e,dest);
    	boolean changed = copyTuples(e,source,dest);
    	removeTuples(e,source);
    	return changed;
    }
    
    public boolean joinTuples(Entry e, Register source1, Register source2, Register dest) {
    	Utilities.out("JOIN TUPLES OF " + source1 + " AND "+source2 + " IN " + dest);
    	removeTuples(e,dest);
    	boolean changed = false;
    	changed |= copyTuples(e,source1, dest);
    	changed |= copyTuples(e,source2, dest);
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

    public Boolean copyTuplesPhi(Entry e, Register source, Register dest) {

    	Boolean changed = false;
    	Trio<Register,FieldSet,FieldSet> t = null;
    	List<Trio<Register,FieldSet,FieldSet>> l1 = findTuplesByFirstRegister(e,source);
    	Iterator<Trio<Register,FieldSet,FieldSet>> it1 = l1.iterator();
    	while (it1.hasNext()) {
    		t = it1.next();
    		if (source != t.val0)
    			changed |= condAdd(e,dest,t.val0,t.val1,t.val2);
    		else changed |= condAdd(e,dest,dest,t.val1,t.val2);
    	}
    	List<Trio<Register,FieldSet,FieldSet>> l2 = findTuplesBySecondRegister(e,source);
    	Iterator<Trio<Register,FieldSet,FieldSet>> it2 = l2.iterator();
    	while (it2.hasNext()) {
    		t = it2.next();
    		if (source != t.val0)
    			changed |= condAdd(e,t.val0,dest,t.val1,t.val2);
    	}
    	return changed;
    }
    

	public Boolean copyTuplesFromCycle(Entry e, Register source,Register dest,RelCycle relCycle) {

	 	Boolean changed = false;
    	FieldSet fs = null;
    	List<FieldSet> l = relCycle.findTuplesByRegister(e,source);
    	Iterator<FieldSet> it = l.iterator();
    	while (it.hasNext()) {
    		fs = it.next();
    		changed |= condAdd(e,dest,dest,fs,fs);
    		changed |= condAdd(e,dest,dest,FieldSet.emptyset(),fs);
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

    public Boolean condAdd(Entry e, Register r1, Register r2, FieldSet fs1, FieldSet fs2) {

    	if (r1 == r2) { // self-f-sharing
    		if (!FieldSet.leq(fs1,fs2)) { // the "smaller" field set goes first
    			FieldSet x = fs1;
    			fs1 = fs2;
    			fs2 = x;
    		}
    	}
    	
    	if (!contains(e,r1,r2,fs1,fs2)) {
    		add(e,r1,r2,fs1,fs2);
    		Utilities.debug("ADDED ( "+e+" , "+ r1 + " , " + r2 + " , " + fs1 + " , " + fs2 + ") TO Share");
    	}
    	return accumulatedTuples.condAdd(e,r1,r2,fs1,fs2);
    }
  
    /**
     * Adds a sharing statement from r1 to r2 for every field set.
     * 
     * @param r1 The first register of the tuple.
     * @param r2 The second register of the tuple.
     * @return a boolean value specifying if some tuple is a new one.
     */

    public Boolean condAddTrue(Entry e, Register r1, Register r2) {

    	Boolean x = false;
    	DomFieldSet domFSet = (DomFieldSet) ClassicProject.g().getTrgt("FieldSet");
    	for (FieldSet fs1 : domFSet.getAll()) {
    		for (FieldSet fs2 : domFSet.getAll()) {
    			x |= condAdd(e, r1,r2,fs1,fs2);
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
    public List<Trio<Register,FieldSet,FieldSet>> findTuplesByFirstRegister(Entry e, Register r) {
    	RelView view = getView();
    	PentIterable<Entry,Register,Register,FieldSet,FieldSet> tuples = view.getAry5ValTuples();
    	Iterator<Pent<Entry,Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<Trio<Register,FieldSet,FieldSet>>();
    	while (iterator.hasNext()) {
    		Pent<Entry,Register,Register,FieldSet,FieldSet> pent = iterator.next();
    		if (pent.val0 == e && pent.val1 == r)
    			list.add(new Trio<Register,FieldSet,FieldSet>(pent.val2,pent.val3,pent.val4));
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
    public List<Pair<Register,FieldSet>> findTuplesByFirstRegisterEmpty1(Entry e, Register r) {
    	RelView view = getView();
    	PentIterable<Entry,Register,Register,FieldSet,FieldSet> tuples = view.getAry5ValTuples();
    	Iterator<Pent<Entry,Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Pair<Register,FieldSet>> list = new ArrayList<Pair<Register,FieldSet>>();
    	while (iterator.hasNext()) {
    		Pent<Entry,Register,Register,FieldSet,FieldSet> pent = iterator.next();
    		if (pent.val0 == e && pent.val1 == r && pent.val3 == FieldSet.emptyset())
    			list.add(new Pair<Register,FieldSet>(pent.val2,pent.val4));
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
    public List<Pair<Register,FieldSet>> findTuplesByFirstRegisterEmpty2(Entry e, Register r) {
    	RelView view = getView();
    	PentIterable<Entry,Register,Register,FieldSet,FieldSet> tuples = view.getAry5ValTuples();
    	Iterator<Pent<Entry,Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Pair<Register,FieldSet>> list = new ArrayList<Pair<Register,FieldSet>>();
    	while (iterator.hasNext()) {
    		Pent<Entry,Register,Register,FieldSet,FieldSet> pent = iterator.next();
    		if (pent.val0 == e && pent.val1 == r && pent.val3 == FieldSet.emptyset())
    			list.add(new Pair<Register,FieldSet>(pent.val2,pent.val3));
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
    public List<Trio<Register,FieldSet,FieldSet>> findTuplesBySecondRegister(Entry e, Register r) {
    	RelView view = getView();
    	PentIterable<Entry,Register,Register,FieldSet,FieldSet> tuples = view.getAry5ValTuples();
    	Iterator<Pent<Entry,Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<Trio<Register,FieldSet,FieldSet>>();
    	Pent<Entry,Register,Register,FieldSet,FieldSet> pent = null;
    	while (iterator.hasNext()) {
    		pent = iterator.next();
    		if (pent.val0 == e && pent.val2 == r)
    			list.add(new Trio<Register,FieldSet,FieldSet>(pent.val1,pent.val3,pent.val4));
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
    public List<Pair<Register,FieldSet>> findTuplesBySecondRegisterEmpty1(Entry e, Register r) {
    	RelView view = getView();
    	PentIterable<Entry,Register,Register,FieldSet,FieldSet> tuples = view.getAry5ValTuples();
    	Iterator<Pent<Entry,Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Pair<Register,FieldSet>> list = new ArrayList<Pair<Register,FieldSet>>();
    	while (iterator.hasNext()) {
    		Pent<Entry,Register,Register,FieldSet,FieldSet> pent = iterator.next();
    		if (pent.val0 == e && pent.val2 == r && pent.val3 == FieldSet.emptyset())
    			list.add(new Pair<Register,FieldSet>(pent.val1,pent.val4));
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
    public List<Pair<Register,FieldSet>> findTuplesBySecondRegisterEmpty2(Entry e, Register r) {
    	RelView view = getView();
    	PentIterable<Entry,Register,Register,FieldSet,FieldSet> tuples = view.getAry5ValTuples();
    	Iterator<Pent<Entry,Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Pair<Register,FieldSet>> list = new ArrayList<Pair<Register,FieldSet>>();
    	while (iterator.hasNext()) {
    		Pent<Entry, Register,Register,FieldSet,FieldSet> pent = iterator.next();
    		if (pent.val0 == e && pent.val2 == r && pent.val4 == FieldSet.emptyset())
    			list.add(new Pair<Register,FieldSet>(pent.val1,pent.val3));
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
    public List<Pair<Register,FieldSet>> findTuplesByReachingRegister(Entry e, Register r) {
    	List<Pair<Register,FieldSet>> list1 = findTuplesByFirstRegisterEmpty2(e,r);
    	List<Pair<Register,FieldSet>> list2 = findTuplesBySecondRegisterEmpty1(e,r);
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
    public List<Pair<Register,FieldSet>> findTuplesByReachedRegister(Entry e, Register r) {
    	List<Pair<Register,FieldSet>> list1 = findTuplesByFirstRegisterEmpty1(e,r);
    	List<Pair<Register,FieldSet>> list2 = findTuplesBySecondRegisterEmpty2(e,r);
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
    public List<FieldSet> findTuplesByReachingReachedRegister(Entry e, Register r1, Register r2) {
    	RelView view = getView();
    	PentIterable<Entry,Register,Register,FieldSet,FieldSet> tuples = view.getAry5ValTuples();
    	Iterator<Pent<Entry,Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<FieldSet> list = new ArrayList<FieldSet>();
    	while (iterator.hasNext()) {
    		Pent<Entry, Register,Register,FieldSet,FieldSet> pent = iterator.next();
    		if (pent.val0 == e && pent.val1 == r1 && pent.val2 == r2 && pent.val4 == FieldSet.emptyset())
    			list.add(pent.val3);
    		if (pent.val0 == e && pent.val1 == r2 && pent.val2 == r1 && pent.val3 == FieldSet.emptyset())
    			list.add(pent.val4);
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
    public List<Trio<Register,FieldSet,FieldSet>> findTuplesByRegister(Entry e, Register r) {
    	List<Trio<Register,FieldSet,FieldSet>> list1 = findTuplesByFirstRegister(e,r);
    	List<Trio<Register,FieldSet,FieldSet>> list2 = findTuplesBySecondRegister(e,r);
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
    public List<Pair<FieldSet,FieldSet>> findTuplesByBothRegisters(Entry e, Register r1,Register r2) {
    	RelView view = getView();
    	PentIterable<Entry,Register,Register,FieldSet,FieldSet> tuples = view.getAry5ValTuples();
    	Iterator<Pent<Entry,Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	List<Pair<FieldSet,FieldSet>> list = new ArrayList<Pair<FieldSet,FieldSet>>();
    	Pent<Entry,Register,Register,FieldSet,FieldSet> pent = null;
    	while (iterator.hasNext()) {
    		pent = iterator.next();
    		if (pent.val0 ==  e && pent.val1 == r1 && pent.val2 == r2)
    			list.add(new Pair<FieldSet,FieldSet>(pent.val3,pent.val4));
    	}    	
    	return list;
    }
    
    /**
     * Pretty-prints all the tuples in the relation.
     */
    public void output() {
    	RelView view = getView();
    	PentIterable<Entry,Register,Register,FieldSet,FieldSet> tuples = view.getAry5ValTuples();
    	Iterator<Pent<Entry,Register,Register,FieldSet,FieldSet>> iterator = tuples.iterator();
    	Pent<Entry,Register,Register,FieldSet,FieldSet> pent = null;
    	while (iterator.hasNext()) {
    		pent = iterator.next();
    		System.out.println("SHARE STATEMENT IN "+pent.val0+" : " + pent.val1 + " --> " + pent.val3 + " / " + pent.val4 + " <-- " + pent.val2);
    	}    	
    }
    
    /**
     * Pretty-prints the sharing information about the n1-nth and the
     * n2-nth local variable.
     * 
     * @param n1 The position of the source register among local variables.
     * @param n2 The position of the target register among local variables.
     */
    public void askFor(Entry e, Register r1, Register r2) {
    	List<Pair<FieldSet,FieldSet>> l = findTuplesByBothRegisters(e,r1,r2);
    	System.out.println("SHARING BETWEEN " + r1 + " AND " + r2 + " = ");
    	Iterator<Pair<FieldSet,FieldSet>> iterator = l.listIterator();
    	while (iterator.hasNext()) {
    		Pair<FieldSet,FieldSet> p = iterator.next();
    		System.out.println(" * " + p.val0 + " - " + p.val1);
    	}
    }
    
}	
