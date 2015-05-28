package chord.analyses.damianoPairSharing;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.bddbddb.Rel.QuadIterable;
import chord.bddbddb.Rel.RelView;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

/**
 * Relation containing sharing tuples (r1,r2)
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
@Chord(
    name = "PairShare",
    sign = "Quad0,Register0,Register1:Quad0xRegister0xRegister1",
    consumes = { "V", "Register", "Quad", "UseDef" }
)
public class RelPairSharing extends ProgramRel {
	
	public ArrayList<Trio<Quad,Register,Register>> tuples;
		
	public void fill() { }
	
    /**
     * This method does the job of copying tuples from a variable to another.
     * 
     * @param src The source variable.
     * @param dest The destination variable.
     * @return whether some tuples have been added
     */
    public boolean copyTuples(Quad qsrc, Quad qdest, Register src, Register dest) {
    	boolean changed = false;
    	for (Register r : findTuplesByRegister(qsrc,src)) {
    		changed |= condAdd(qdest,src,r);
    		changed |= condAdd(qdest,dest,r);
    	}
    	if (findTuplesByBothRegisters(qsrc,src,src)) {
    		changed |= condAdd(qdest,src,src);
    		changed |= condAdd(qdest,dest,dest);
    	}
    	return changed;
    }
        
    public boolean copyTuples(Quad qsrc, Quad qdest) {
    	boolean changed = false;
    	for (Pair<Register,Register> p : findTuples(qsrc)) {
    		changed |= condAdd(qdest,p.val0,p.val1);
    	}
    	return changed;
    }

    public void removeTuples(Register r) {
    	RelView view = getView();
    	TrioIterable<Quad,Register,Register> ts = view.getAry3ValTuples();
    	Iterator<Trio<Quad,Register,Register>> iterator = ts.iterator();
    	Trio<Quad,Register,Register> trio = null;
    	while (iterator.hasNext()) {
    		trio = iterator.next();
    		if (trio.val1 == r || trio.val2 == r) {
    			remove(trio.val0,trio.val1,trio.val2);
    			Utilities.debug("REMOVED ( " + trio.val1 + " , " + trio.val2 + ") AT " + trio.val0 + " FROM Share");
    		}
    	}
    }

    public void remove(Quad q, Register r1, Register r2) {
		for (int i=0; i<tuples.size(); i++) {
			Trio<Quad,Register,Register> t = tuples.get(i);
			if (t.val0 == q &&
			(t.val1 == r1 && t.val2 == r2) || (t.val1 == r2 && t.val2 == r1)) {
				tuples.remove(i);
				return;
			}
		}
	}
    
    public boolean contains(Quad q, Register r1, Register r2) {
		for (Trio<Quad,Register,Register> t : tuples)
			if (t.val0 == q &&
			((t.val1 == r1 && t.val2 == r2) || (t.val1 == r2 && t.val2 == r1))) return true;
		return false;
	}
    
    public void removeTuples(Quad q, Register r) {
    	RelView view = getView();
    	TrioIterable<Quad,Register,Register> ts = view.getAry3ValTuples();
    	Iterator<Trio<Quad,Register,Register>> iterator = ts.iterator();
    	Trio<Quad,Register,Register> trio = null;
    	while (iterator.hasNext()) {
    		trio = iterator.next();
    		if (trio.val0 == q && (trio.val1 == r || trio.val2 == r)) {
    			remove(trio.val0,trio.val1,trio.val2);
    			Utilities.debug("REMOVED ( " + trio.val1 + " , " + trio.val2 + ") AT " + trio.val0 + " FROM Share");
    		}
    	}
    }
    
    public boolean moveTuples(Quad qsrc, Quad qdest, Register src, Register dest) {
    	boolean changed = copyTuples(qsrc,qdest,src,dest);
    	removeTuples(qsrc,src);
    	return changed;
    }
    
    public boolean joinTuples(Quad qsrc, Quad qdest, Register src1, Register src2, Register dest) {
    	boolean changed = false;
    	changed |= copyTuples(qsrc, qdest, src1, dest);
    	changed |= copyTuples(qsrc, qdest, src2, dest);
    	// removeTuples(source1);
    	// removeTuples(source2);
    	return changed;
    }
    
    /**
     * The same as copyTuples, but does not copy (source,source) into
     * (source,dest) and (dest,source), just to
     * (dest,dest)
     * 
     * @param src The source variable.
     * @param dest The source variable.
     * @return whether some tuples have been added
     */
    public boolean copyTuplesPhi(Quad qsrc, Quad qdest, Register src, Register dest) {
    	boolean changed = false;
    	Register r = null;
    	List<Register> l = findTuplesByFirstRegister(qsrc,src);
    	Iterator<Register> it = l.iterator();
    	while (it.hasNext()) {
    		r = it.next();
    		if (src != r)
    			changed |= condAdd(qdest,dest,r);
    		else changed |= condAdd(qdest,dest,dest);
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
     * @param q The quad.
     * @param r1 The first register of the tuple.
     * @param r2 The second register of the tuple.
     * @return a boolean value specifying if the tuple is a new one.
     */
    public boolean condAdd(Quad q, Register r1, Register r2) {
    	Pair<Register,Register> pr = order(r1,r2);
    	Register x1 = pr.val0;
    	Register x2 = pr.val1;
    	if (!contains(q,x1,x2)) {
    		add(q,x1,x2);
    		tuples.add(new Trio<Quad,Register,Register>(q,x1,x2));
    		Utilities.debug("    ADDED ( " + x1 + " , " + x2 + ") AT " + q + " TO Share");
    		return true;
    	}
    	return false;
    }
    
    public List<Pair<Register,Register>> findTuples(Quad q) {
    	RelView view = getView();
    	TrioIterable<Quad,Register,Register> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Quad,Register,Register>> iterator = tuples.iterator();
    	List<Pair<Register,Register>> list = new ArrayList<Pair<Register,Register>>();
    	while (iterator.hasNext()) {
    		Trio<Quad,Register,Register> trio = iterator.next();
    		if (trio.val0 == q)
    			list.add(new Pair<Register,Register>(trio.val1,trio.val2));
    	}    	
    	return list;
    	
    }
    
    /**
     * Finds all tuples in the relation whose first register is {@code r}.
     * 
     * @param q The quad.
     * @param r
     * @return a list of registers s such that ({@code r},s) is in the relation.
     */
    public List<Register> findTuplesByFirstRegister(Quad q, Register r) {
    	RelView view = getView();
    	TrioIterable<Quad,Register,Register> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Quad,Register,Register>> iterator = tuples.iterator();
    	List<Register> list = new ArrayList<Register>();
    	while (iterator.hasNext()) {
    		Trio<Quad,Register,Register> trio = iterator.next();
    		if (trio.val0 == q && trio.val1 == r)
    			list.add(trio.val2);
    	}    	
    	return list;
    }
        
    /**
     * Finds all tuples in the relation whose second register is {@code r}.
     * 
     * @param q The quad.
     * @param r
     * @return a list of registers s such that (s,{@code r}) is in
     * the relation.
     */
    public List<Register> findTuplesBySecondRegister(Quad q, Register r) {
    	RelView view = getView();
    	TrioIterable<Quad,Register,Register> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Quad,Register,Register>> iterator = tuples.iterator();
    	List<Register> list = new ArrayList<Register>();
    	Trio<Quad,Register,Register> trio = null;
    	while (iterator.hasNext()) {
    		trio = iterator.next();
    		if (trio.val0 == q && trio.val2 == r)
    			list.add(trio.val1);
    	}    	
    	return list;
    }

    /**
     * Finds all tuples in the relation whose first or second register is
     * {@code r}.
     * 
     * @param q The quad.
     * @param r
     * @return a list of registers s such that either (s,{@code r}) or
     * ({@code r},s) is in the relation.
     */
    public List<Register> findTuplesByRegister(Quad q, Register r) {
    	List<Register> list1 = findTuplesByFirstRegister(q,r);
    	List<Register> list2 = findTuplesBySecondRegister(q,r);
    	list1.addAll(list2);
    	return list1;
    }
    
    /**
     * Returns true iff there is a tuple ({@code r1},{@code r2}).
     * 
     * @param q The quad.
     * @param r1
     * @param r2
     * @return whether ({@code r1},{@code r2}) is in the relation.
     */
    public boolean findTuplesByBothRegisters(Quad q, Register r1, Register r2) {
    	List<Register> list1 = findTuplesByRegister(q,r1);
    	return list1.contains(r2);
    }
    
    /**
     * Pretty-prints all the tuples in the relation.
     */
    public void prettyPrint(jq_Method m) {
    	RelView view = getView();
    	TrioIterable<Quad,Register,Register> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Quad,Register,Register>> iterator = tuples.iterator();
    	Trio<Quad,Register,Register> trio = null;
    	while (iterator.hasNext()) {
    		trio = iterator.next();
    		String s1 = RegisterManager.getVarFromReg(m,trio.val1);
    		String s2 = RegisterManager.getVarFromReg(m,trio.val2);
    		Utilities.out("SHARING: " + trio.val1 + " (" + s1 + ") WITH " + trio.val1 + " (" + s2 + ") AT " + trio.val0);
    	}    	
    }
    
	public void prettyPrint(jq_Method m, Register r1, Register r2) {
		String s1 = RegisterManager.getVarFromReg(m,r1);
		String s2 = RegisterManager.getVarFromReg(m,r2);
    	RelView view = getView();
    	TrioIterable<Quad,Register,Register> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Quad,Register,Register>> iterator = tuples.iterator();
    	Trio<Quad,Register,Register> trio = null;
    	while (iterator.hasNext()) {
    		trio = iterator.next();
    		if ((trio.val1 == r1 && trio.val2 == r2) || (trio.val1 == r2 && trio.val2 == r1)) {
    			Utilities.out("SHARING: " + trio.val1 + " (" + s1 + ") WITH " + trio.val2 + " (" + s2 + ") AT " + trio.val0);
    		}
    	}
    }

	public void prettyPrint(jq_Method m, Quad q, Register r1, Register r2) {
		String s1 = RegisterManager.getVarFromReg(m,r1);
		String s2 = RegisterManager.getVarFromReg(m,r2);
    	RelView view = getView();
    	TrioIterable<Quad,Register,Register> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Quad,Register,Register>> iterator = tuples.iterator();
    	Trio<Quad,Register,Register> trio = null;
    	while (iterator.hasNext()) {
    		trio = iterator.next();
    		if (trio.val0 == q &&
    				((trio.val1 == r1 && trio.val2 == r2) || 
    						(trio.val1 == r2 && trio.val2 == r1))) {
    			Utilities.out("SHARING: " + trio.val1 + " (" + s1 + ") WITH " + trio.val2 + " (" + s2 + ") AT " + q);
    		}
    	}
    }
        
}	
