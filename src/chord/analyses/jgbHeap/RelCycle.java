package chord.analyses.jgbHeap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Utilities;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

/**
 * Relation containing cyclicity tuples (r,fs) specifying that
 * the set of fields (truth assignment) fs is a model of the
 * path-formula F such that r is F-cyclic.
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
@Chord(
    name = "HeapCycle",
    sign = "Register,FieldSet:Register_FieldSet",
    consumes = { "V", "Register", "FieldSet", "UseDef" }
)
public class RelCycle extends ProgramRel {
	
	AccumulatedTuples accumulatedTuples;
	
	public void setAccumulatedTuples(AccumulatedTuples tuples){ 
		this.accumulatedTuples = tuples; 
	}
	
    public void fill() { }
	
    /**
     * This method does the job of copying tuples from a variable to another.
     * @param source The source variable.
     * @param dest The destination variable.
     * @return
     */
    public Boolean copyTuples(Register source, Register dest) {
    	Utilities.out("COPY TUPLES OF " + source + " TO " + dest);
    	Boolean changed = false;
    	List<FieldSet> l = findTuplesByRegister(source);
    	for (FieldSet fs : l) {
    		changed |= condAdd(dest,fs);
    	}
    	return changed;
    }
    
    public Boolean copyTuplesTemp(Register source, Register dest){
    	
    	Utilities.out("COPY TUPLES OF " + source + " TO TEMP " + dest);
    	Boolean changed = false;
    	List<FieldSet> l = findTuplesByRegister(source);
    	for (FieldSet fs : l) {
    		changed |= condAddTemp(dest,fs);
    	}
    	return changed;
    }
    

    public void removeTuples(Register r) {

    	RelView view = getView();
    	PairIterable<Register,FieldSet> tuples = view.getAry2ValTuples();
    	Iterator<Pair<Register,FieldSet>> iterator = tuples.iterator();
    	Pair<Register,FieldSet> pair = null;
    	while (iterator.hasNext()) {
    		pair = iterator.next();
    		if (pair.val0 == r) {
    			this.remove(pair.val0);
        		Utilities.debug("REMOVED ( " + pair.val0 + " , " + pair.val1 + " ) FROM Cycle");
    		}
    	}
    }
    
    public Boolean moveTuples(Register source, Register dest) {
    	Utilities.out("MOVE TUPLES FROM " + source + " TO " + dest);
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
    	removeTuples(source1);
    	removeTuples(source2);
    	return changed;
    }
    
    /**
     * Adds a cyclicity statement if it does not already belong to the relation.
     * @param r The register of the tuple.
     * @param fs The field set (second element of the tuple).
     * @return a boolean value specifying if the tuple is a new one.
     */
    public Boolean condAdd(Register r, FieldSet fs) {
    	if (!contains(r,fs)) {
    		add(r,fs);
    		Utilities.debug("ADDED ( " + r + " , " + fs + " ) TO Cycle");
    	}
    	return accumulatedTuples.condAdd(r,fs);
    }
    
    public Boolean condAddTemp(Register r, FieldSet fs) {
    	Utilities.debug("ADDED ( " + r + " , " + fs + ") TO Cycle");
    	return accumulatedTuples.condAdd(r,fs);
    }
 
    /**
     * Finds all tuples in the relation whose register is {@code r}.
     * @param r
     * @return a list of field sets fs such that ({@code r},fs) is in the relation.
     */
    protected List<FieldSet> findTuplesByRegister(Register r) {
    	RelView view = getView();
    	PairIterable<Register,FieldSet> tuples = view.getAry2ValTuples();
    	Iterator<Pair<Register,FieldSet>> iterator = tuples.iterator();
    	List<FieldSet> list = new ArrayList<FieldSet>();
    	while (iterator.hasNext()) {
    		Pair<Register,FieldSet> pair = iterator.next();
    		if (pair.val0 == r) list.add(pair.val1);
    	}    	
    	return list;
    }
    
    /**
     * Pretty-prints all the pairs in the relation.
     */
    public void output() {
    	RelView view = getView();
    	PairIterable<Register,FieldSet> tuples = view.getAry2ValTuples();
    	Iterator<Pair<Register,FieldSet>> iterator = tuples.iterator();
    	Pair<Register,FieldSet> pair = null;
    	while (iterator.hasNext()) {
	    pair = iterator.next();
	    System.out.println("CYCLE STATEMENT: " + pair.val0 + " --0--> " + pair.val1);
    	}    	
    }
    
    /**
     * Pretty-prints the cyclicity information about the n-nth local variable. 
     * @param n The position of the register among local variables.
     */
    public void askFor(Register r) {
	List<FieldSet> l = findTuplesByRegister(r);
	System.out.println("CYCLICITY OF " + r + " = ");
	Iterator<FieldSet> iterator = l.listIterator();
	while (iterator.hasNext()) {
	    System.out.println(" * " + iterator.next());
	}
    }
    
}	
