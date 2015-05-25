package chord.analyses.damianoCycle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Utilities;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

/**
 * Relation containing reachability tuples (r1,r2,fs) specifying that
 * the set of fields (truth assignment) fs is a model of the
 * path-formula F such that r1 F-reaches r2.
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
@Chord(
    name = "Reach",
    sign = "Register0,Register1,FSet:Register0xRegister1_FSet",
    consumes = { "V", "Register", "AbsF", "FSet", "UseDef" }
)
public class RelOldReach extends ProgramRel {
	
	AccumulatedTuples accumulatedTuples;
	
	public void fill() { }
	
    /**
     * This method does the job of copying tuples from a variable to another.
     * @param source The source variable.
     * @param dest The destination variable.
     * @return whether some tuples have been added
     */
    public Boolean copyTuples(Register source, Register dest) {
    	Boolean changed = false;
    	for (Pair<Register,FSet> p : findTuplesByFirstRegister(source))
    		changed |= condAdd(dest,p.val0,p.val1);	
    	for (Pair<Register,FSet> p : findTuplesBySecondRegister(source))
    		changed |= condAdd(p.val0,dest,p.val1);	
    	return changed;
    }    	
    
    public void removeTuples(Register r) {
    	RelView view = getView();
    	TrioIterable<Register,Register,FSet> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Register,Register,FSet>> iterator = tuples.iterator();
    	Trio<Register,Register,FSet> trio = null;
    	while (iterator.hasNext()) {
    		trio = iterator.next();
    		if (trio.val0 == r || trio.val1 == r) {
    			this.remove(trio.val0,trio.val1);
    			Utilities.debug("REMOVED ( " + trio.val0 + " , " + trio.val1 + " , " + trio.val2 + " ) FROM Reach");
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
     * The same as copyTuples, but does not copy (source,source,fs) into
     * (source,dest,fs) and (dest,source,fs), just to (dest,dest,fs)
     * @param source The source variable.
     * @param dest The source variable.
     * @return whether some tuples have been added
     */
    
    public Boolean copyTuplesPhi(Register source, Register dest) {
    	Boolean changed = false;
    	Pair<Register,FSet> p = null;
    	List<Pair<Register,FSet>> l1 = findTuplesByFirstRegister(source);
    	Iterator<Pair<Register,FSet>> it1 = l1.iterator();
    	while (it1.hasNext()) {
    		p = it1.next();
    		if (source != p.val0)
    			changed |= condAdd(dest,p.val0,p.val1);
    		else changed |= condAdd(dest,dest,p.val1);
    	}
    	List<Pair<Register,FSet>> l2 = findTuplesBySecondRegister(source);
    	Iterator<Pair<Register,FSet>> it2 = l2.iterator();
    	while (it2.hasNext()) {
    		p = it2.next();
    		if (source != p.val0)
    			changed |= condAdd(p.val0,dest,p.val1);
    	}
    	return changed;
    }
    
    public Boolean copyTuplesFromCycle(Register source, Register dest, RelCycle relCycle) {
    	Boolean changed = false;
    	FSet fs = null;
    	List<FSet> l = relCycle.findTuplesByRegister(source);
    	Iterator<FSet> it = l.iterator();
    	while (it.hasNext()) {
    		fs = it.next();
    		changed |= condAdd(dest,dest,fs);
    	}
    	return changed;
    }
    
    /**
     * Adds a reachability statement if it does not already belong to the relation.
     * @param r1 The first register of the tuple.
     * @param r2 The second register of the tuple.
     * @param fs The field set (third element of the tuple).
     * @return a boolean value specifying if the tuple is a new one.
     */
    public Boolean condAdd(Register r1, Register r2, FSet fs) {
    	if (!contains(r1,r2,fs)) {
    		add(r1,r2,fs);
    		Utilities.debug("ADDED ( " + r1 + " , " + r2 + " , " + fs + " ) TO Reach");
    	}
    	return accumulatedTuples.condAdd(r1,r2,fs);
    }
 
    /**
     * Adds a reachability statement from r1 to r2 for every field set.
     * @param r1 The first register of the tuple.
     * @param r2 The second register of the tuple.
     * @return a boolean value specifying if some tuple is a new one.
     */
    public Boolean condAddTrue(Register r1, Register r2) {
    	Boolean x = false;
    	DomFSet domFSet = (DomFSet) ClassicProject.g().getTrgt("FSet");
    	for (FSet fs : domFSet.getAll()) {
    		x |= condAdd(r1,r2,fs);
    	}
    	return x;
    }
    
    /**
     * Finds all tuples in the relation whose first register is {@code r}.
     * @param r
     * @return a list of pairs (s,f) such that ({@code r},s,f) is in the relation.
     */
    public List<Pair<Register,FSet>> findTuplesByFirstRegister(Register r) {
    	RelView view = getView();
    	TrioIterable<Register,Register,FSet> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Register,Register,FSet>> iterator = tuples.iterator();
    	List<Pair<Register,FSet>> list = new ArrayList<Pair<Register,FSet>>();
    	while (iterator.hasNext()) {
    		Trio<Register,Register,FSet> trio = iterator.next();
    		if (trio.val0 == r) list.add(new Pair<Register,FSet>(trio.val1,trio.val2));
    	}    	
    	return list;
    }
    
    /**
     * Finds all tuples in the relation whose second register is {@code r}.
     * @param r
     * @return a list of pairs (s,f) such that (s,{@code r},f) is in the relation.
     */
    public List<Pair<Register,FSet>> findTuplesBySecondRegister(Register r) {
    	RelView view = getView();
    	TrioIterable<Register,Register,FSet> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Register,Register,FSet>> iterator = tuples.iterator();
    	List<Pair<Register,FSet>> list = new ArrayList<Pair<Register,FSet>>();
    	Trio<Register,Register,FSet> trio = null;
    	while (iterator.hasNext()) {
    		trio = iterator.next();
    		if (trio.val1 == r) list.add(new Pair<Register,FSet>(trio.val0,trio.val2));
    	}    	
    	return list;
    }
    
    /**
     * Finds all tuples in the relation whose first register is {@code r1} and
     * second register is {@code r2}.
     * @param r1 The first register.
     * @param r2 The second register.
     * @return a list of field sets f such that ({@code r1},{@code r2},f) is
     * in the relation.
     */
    public List<FSet> findTuplesByBothRegisters(Register r1,Register r2) {
    	RelView view = getView();
    	TrioIterable<Register,Register,FSet> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Register,Register,FSet>> iterator = tuples.iterator();
    	List<FSet> list = new ArrayList<FSet>();
    	Trio<Register,Register,FSet> trio = null;
    	while (iterator.hasNext()) {
    		trio = iterator.next();
    		if (trio.val0 == r1 && trio.val1 == r2) list.add(trio.val2);
    	}    	
    	return list;
    }
    
    /**
     * Pretty-prints all the triples in the relation.
     */
    public void output() {
    	RelView view = getView();
    	TrioIterable<Register,Register,FSet> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Register,Register,FSet>> iterator = tuples.iterator();
    	Trio<Register,Register,FSet> trio = null;
    	while (iterator.hasNext()) {
    		trio = iterator.next();
    		System.out.println("REACH STATEMENT: " + trio.val0 + " -- " + trio.val2 + " --> " + trio.val1);
    	}    	
    }
    
    /**
     * Pretty-prints the reachability information about the n1-nth and the
     * n2-nth local variable.
     * @param n1 The position of the source register among local variables.
     * @param n2 The position of the target register among local variables.
     */
    public void askFor(Register r1, Register r2) {
    	List<FSet> l = findTuplesByBothRegisters(r1,r2);
    	System.out.println("REACHABILITY FROM " + r1 + " TO " + r2 + " = ");
    	Iterator<FSet> iterator = l.listIterator();
    	while (iterator.hasNext()) {
    		System.out.println(" * " + iterator.next());
    	}
    }
    
}	
