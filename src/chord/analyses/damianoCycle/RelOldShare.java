package chord.analyses.damianoCycle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.var.DomV;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

/**
 * Relation containing sharing pairs (r1,r2) specifying that r1
 * may share with r2.
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
@Chord(
    name = "OldShare",
    sign = "Register0,Register1:Register0xRegister1",
    consumes = { "V", "Register", "UseDef" }
)
public class RelOldShare extends ProgramRel {
	
	AccumulatedTuples accumulatedTuples;
	
    public void fill() { }
	
    /**
     * This method does the job of copying tuples from a variable to another.
     * 
     * @param source The source variable.
     * @param dest The destination variable.
     * @return
     */
    public boolean copyTuples(Register source, Register dest) {
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
    			Utilities.debug("REMOVED ( " + pair.val0 + " , " + pair.val1 + " ) FROM OldShare");
    		}
    	}
    }
    
    public boolean moveTuples(Register source, Register dest) {
    	removeTuples(dest);
    	boolean changed = copyTuples(source, dest);
    	removeTuples(source);
    	return changed;
    }
    
    public boolean joinTuples(Register source1, Register source2, Register dest) {
    	removeTuples(dest);
    	boolean changed = false;
    	changed |= copyTuples(source1, dest);
    	changed |= copyTuples(source2, dest);
    	removeTuples(source1);
    	removeTuples(source2);
    	return changed;
    }
    
    /**
     * Adds a sharing statement if it does not already belong to the relation,
     * taking into account that sharing is commutative.
     * 
     * @param r1 The first register of the tuple.
     * @param r2 The second register of the tuple.
     * @return a boolean value specifying if the tuple is a new one.
     */
    public Boolean condAdd(Register r1, Register r2) {
    	Register s1 = null;
    	Register s2 = null;
    	DomV domV = (DomV) ClassicProject.g().getTrgt("V");
    	if(domV.indexOf(r1) > domV.indexOf(r2)) {
    		s1 = r2;
    		s2 = r1;
    	} else {
    		s1 = r1;
    		s2 = r2;
    	}
    	if (!contains(s1,s2)) {
    		add(s1,s2);
    		Utilities.debug("ADDED ( " + s1 + " , " + s2 + " ) TO OldShare");
    	}
    	return accumulatedTuples.condAdd(s1,s2);
    }
 
    /**
     * Finds all tuples in the relation whose first register is {@code r}.
     * 
     * @param r
     * @return a list of registers s such that ({@code r},s) is in the relation.
     */
    protected List<Register> findTuplesByFirstRegister(Register r) {
    	RelView view = getView();
    	PairIterable<Register,Register> tuples = view.getAry2ValTuples();
    	Iterator<Pair<Register,Register>> iterator = tuples.iterator();
    	List<Register> list = new ArrayList<Register>();
    	while (iterator.hasNext()) {
    		Pair<Register,Register> pair = iterator.next();
    		if (pair.val0 == r) list.add(pair.val1);
    	}    	
    	return list;
    }
    
    /**
     * Finds all tuples in the relation whose second register is {@code r}.
     * 
     * @param r
     * @return a list of registers s such that ({@code r},s) is in the relation.
     */
    protected List<Register> findTuplesBySecondRegister(Register r) {
    	RelView view = getView();
    	PairIterable<Register,Register> tuples = view.getAry2ValTuples();
    	Iterator<Pair<Register,Register>> iterator = tuples.iterator();
    	List<Register> list = new ArrayList<Register>();
    	Pair<Register,Register> pair = null;
    	while (iterator.hasNext()) {
    		pair = iterator.next();
    		if (pair.val1 == r) list.add(pair.val0);
    	}    	
    	return list;
    }
    
    public List<Register> findTuplesByRegister(Register r) {
    	List<Register> l = findTuplesBySecondRegister(r);
    	l.addAll(findTuplesByFirstRegister(r));
    	return l;
    }
    
    /**
     * Pretty-prints all the pairs in the relation.
     */
    public void output() {
    	RelView view = getView();
    	PairIterable<Register,Register> tuples = view.getAry2ValTuples();
    	Iterator<Pair<Register,Register>> iterator = tuples.iterator();
    	Pair<Register,Register> pair = null;
    	while (iterator.hasNext()) {
    		pair = iterator.next();
    		System.out.println("SHARING STATEMENT: " + pair.val0 + " - " + pair.val1);
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
    	System.out.println("SHARING BETWEEN " + r1 + " TO " + r2 + " = " + contains(r1,r2));
    }
    
}	
