package chord.analyses.damianoPairSharing;

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
 * Relation containing sharing tuples (r1,r2)
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
@Chord(
    name = "PairShare",
    sign = "Register0,Register1:Register0xRegister1",
    consumes = { "V", "Register", "UseDef" }
)
public class RelPairShare extends ProgramRel {
	
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
    	for (Register r : findTuplesByFirstRegister(source))
    		changed |= condAdd(dest,r);	
    	for (Register r : findTuplesBySecondRegister(source))
    		changed |= condAdd(r,dest);	
    	return changed;
    }
    
    public void removeTuples(Register r) {
    	RelView view = getView();
    	PairIterable<Register,Register> tuples = view.getAry2ValTuples();
    	Iterator<Pair<Register,Register>> iterator = tuples.iterator();
    	Pair<Register,Register> pair = null;
    	while (iterator.hasNext()) {
    		pair = iterator.next();
    		if (pair.val0 == r || pair.val1 == r) {
    			this.remove(pair.val0,pair.val1);
    			Utilities.debug("REMOVED ( " + pair.val0 + " , " + pair.val1 + " ) FROM Share");
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
     * The same as copyTuples, but does not copy (source,source) into
     * (source,dest) and (dest,source), just to
     * (dest,dest)
     * 
     * @param source The source variable.
     * @param dest The source variable.
     * @return whether some tuples have been added
     */
    public Boolean copyTuplesPhi(Register source, Register dest) {
    	Boolean changed = false;
    	Register r = null;
    	List<Register> l1 = findTuplesByFirstRegister(source);
    	Iterator<Register> it1 = l1.iterator();
    	while (it1.hasNext()) {
    		r = it1.next();
    		if (source != r)
    			changed |= condAdd(dest,r);
    		else changed |= condAdd(dest,dest);
    	}
    	List<Register> l2 = findTuplesBySecondRegister(source);
    	Iterator<Register> it2 = l2.iterator();
    	while (it2.hasNext()) {
    		r = it2.next();
    		if (source != r)
    			changed |= condAdd(r,dest);
    	}
    	return changed;
    }
    
    public static Pair<Register,Register> order(Register r1,Register r2) {
    	Register x1 = r1;
    	Register x2 = r2;
    	if (r1.isTemp()) {
    		if (r2.isTemp()) {
    	    	if (r1.getNumber() > r2.getNumber()) {
    	    		x1 = r2;
    	    		x2 = r1;
    	    	}
    		} else {
    			x1 = r2;
    			x2 = r1;
    		}
    	} if (!r2.isTemp()) {
    		if (r1.getNumber() > r2.getNumber()) {
	    		x1 = r2;
	    		x2 = r1;
	    	}
    	}
    	return new Pair<Register,Register>(x1,x2);
    }
    
    /**
     * Adds a sharing statement if it does not already belong to the
     * relation.
     * 
     * @param r1 The first register of the tuple.
     * @param r2 The second register of the tuple.
     * @return a boolean value specifying if the tuple is a new one.
     */
    public Boolean condAdd(Register r1, Register r2) {
    	Pair<Register,Register> pr = order(r1,r2);
    	Register x1 = pr.val0;
    	Register x2 = pr.val1;
    	if (!contains(x1,x2)) {
    		add(x1,x2);
    		Utilities.debug("ADDED ( " + x1 + " , " + x2 + ") TO Share");
    	}
    	return accumulatedTuples.condAdd(x1,x2);
    }
 
    /**
     * Finds all tuples in the relation whose first register is {@code r}.
     * 
     * @param r
     * @return a list of registers s such that ({@code r},s) is in the relation.
     */
    public List<Register> findTuplesByFirstRegister(Register r) {
    	RelView view = getView();
    	PairIterable<Register,Register> tuples = view.getAry2ValTuples();
    	Iterator<Pair<Register,Register>> iterator = tuples.iterator();
    	List<Register> list = new ArrayList<Register>();
    	while (iterator.hasNext()) {
    		Pair<Register,Register> pair = iterator.next();
    		if (pair.val0 == r)
    			list.add(pair.val1);
    	}    	
    	return list;
    }
        
    /**
     * Finds all tuples in the relation whose second register is {@code r}.
     * 
     * @param r
     * @return a list of registers s such that (s,{@code r}) is in
     * the relation.
     */
    public List<Register> findTuplesBySecondRegister(Register r) {
    	RelView view = getView();
    	PairIterable<Register,Register> tuples = view.getAry2ValTuples();
    	Iterator<Pair<Register,Register>> iterator = tuples.iterator();
    	List<Register> list = new ArrayList<Register>();
    	Pair<Register,Register> pair = null;
    	while (iterator.hasNext()) {
    		pair = iterator.next();
    		if (pair.val1 == r)
    			list.add(pair.val0);
    	}    	
    	return list;
    }

    /**
     * Finds all tuples in the relation whose first or second register is
     * {@code r}.
     * 
     * @param r
     * @return a list of registers s such that either (s,{@code r}) or
     * ({@code r},s) is in the relation.
     */
    public List<Register> findTuplesByRegister(Register r) {
    	List<Register> list1 = findTuplesByFirstRegister(r);
    	List<Register> list2 = findTuplesBySecondRegister(r);
    	list1.addAll(list2);
    	return list1;
    }
            
    /**
     * Pretty-prints all the tuples in the relation.
     */
    public void output() {
    	RelView view = getView();
    	PairIterable<Register,Register> tuples = view.getAry2ValTuples();
    	Iterator<Pair<Register,Register>> iterator = tuples.iterator();
    	Pair<Register,Register> pair = null;
    	while (iterator.hasNext()) {
    		pair = iterator.next();
    		System.out.println("SHARE STATEMENT: " + pair.val0 + " / " + pair.val1);
    	}    	
    }
        
}	
