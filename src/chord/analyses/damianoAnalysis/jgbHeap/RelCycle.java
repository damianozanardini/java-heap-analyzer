package chord.analyses.damianoAnalysis.jgbHeap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Utilities;
import chord.bddbddb.Rel.PentIterable;
import chord.bddbddb.Rel.RelView;
import chord.project.Chord;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Pent;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;

/**
 * Relation containing cyclicity tuples (r,fs) specifying that
 * the set of fields (truth assignment) fs is a model of the
 * path-formula F such that r is F-cyclic.
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
@Chord(
    name = "JHeapCycle",
    sign = "JEntry,Register,JFieldSet:JEntry_Register_JFieldSet",
    consumes = { "V", "Register", "JFieldSet", "UseDef", "JEntry" }
)
public class RelCycle extends ProgramRel {
	
	AccumulatedTuples accumulatedTuples;
	ArrayList<Trio<Entry,Register,FieldSet>> relTuples = new ArrayList<>();
	
	
	public AccumulatedTuples getAccumulatedTuples(){ 
		return this.accumulatedTuples; 
	}
	
	public void setIterable(TrioIterable<Entry,Register,FieldSet> it){
		ArrayList<Trio<Entry,Register,FieldSet>> list  = new ArrayList<>();
		Iterator<Trio<Entry,Register,FieldSet>> i = it.iterator();
		while(i.hasNext()){
			Trio<Entry,Register,FieldSet> t = i.next();
			list.add(new Trio<>(t.val0,t.val1,t.val2));
		}
		for(Trio<Entry,Register,FieldSet> p : list)
			add(p.val0,p.val1,p.val2);
	}
	
    public void fill() { 
    	
    	for(Trio<Entry,Register,FieldSet> t : relTuples)
    		condAdd(t.val0,t.val1,t.val2);
    }
	
    /**
     * This method does the job of copying tuples from a variable to another.
     * @param source The source variable.
     * @param dest The destination variable.
     * @return
     */
    public Boolean copyTuples(Entry e,Register source, Register dest) {
    	Utilities.out("COPY TUPLES IN " + e +" OF " + source + " TO " + dest);
    	Boolean changed = false;
    	List<FieldSet> l = findTuplesByRegister(e,source);
    	if(l.size() == 0) Utilities.out("LA LISTA DE TUPLAS DE SOURCE ES 0");
    	for (FieldSet fs : l) {
    		changed |= condAdd(e,dest,fs);
    	}
    	return changed;
    }
    

    public void removeTuples(Entry e, Register r) {

    	RelView view = getView();
    	TrioIterable<Entry,Register,FieldSet> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Entry,Register,FieldSet>> iterator = tuples.iterator();
    	Trio<Entry,Register,FieldSet> trio = null;
    	while (iterator.hasNext()) {
    		trio = iterator.next();
    		if (trio.val0 == e && trio.val1 == r) {
    			this.remove(trio.val0);
        		Utilities.debug("REMOVED ( " + trio.val0 + " , " + trio.val1 + " , " + trio.val2 + " ) FROM Cycle");
    		}
    	}
    }

    
    public Boolean moveTuples(Entry e, Register source, Register dest) {
    	Utilities.out("MOVE TUPLES FROM " + source + " TO " + dest);
    	removeTuples(e,dest);
    	boolean changed = copyTuples(e,source,dest);
    	removeTuples(e,source);
    	return changed;
    }

    public boolean joinTuples(Entry e,Register source1, Register source2, Register dest) {
    	Utilities.out("JOIN TUPLES IN "+e+" OF " + source1 + " AND "+source2 + " IN " + dest);
    	removeTuples(e,dest);
    	boolean changed = false;
    	changed |= copyTuples(e,source1, dest);
    	changed |= copyTuples(e,source2, dest);
    	removeTuples(e,source1);
    	removeTuples(e,source2);
    	return changed;
    }
    
    /**
     * Adds a cyclicity statement if it does not already belong to the relation.
     * @param r The register of the tuple.
     * @param fs The field set (second element of the tuple).
     * @return a boolean value specifying if the tuple is a new one.
     */
    public Boolean condAdd(Entry e, Register r, FieldSet fs) {
    	if (!contains(e,r,fs)) {
    		add(e,r,fs);
    		Utilities.debug("ADDED ( " + r + " , " + fs + " ) TO Cycle");
    	}
    	return accumulatedTuples.condAdd(e,r,fs);
    }
 
    /**
     * Finds all tuples in the relation whose register is {@code r}.
     * @param r
     * @return a list of field sets fs such that ({@code r},fs) is in the relation.
     */
    protected List<FieldSet> findTuplesByRegister(Entry e, Register r) {
    	RelView view = getView();
    	TrioIterable<Entry,Register,FieldSet> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Entry,Register,FieldSet>> iterator = tuples.iterator();
    	List<FieldSet> list = new ArrayList<FieldSet>();
    	while (iterator.hasNext()) {
    		Trio<Entry,Register,FieldSet> trio = iterator.next();
    		if (trio.val0 == e && trio.val1 == r) list.add(trio.val2);
    	}    	
    	return list;
    }
    
    /**
     * Pretty-prints all the pairs in the relation.
     */
    public void output() {
    	RelView view = getView();
    	TrioIterable<Entry,Register,FieldSet> tuples = view.getAry3ValTuples();
    	Iterator<Trio<Entry,Register,FieldSet>> iterator = tuples.iterator();
    	Trio<Entry,Register,FieldSet> trio = null;
    	while (iterator.hasNext()) {
	    trio = iterator.next();
	    System.out.println("CYCLE STATEMENT IN " +trio.val0+": " + trio.val1 + " --0--> " + trio.val2);
    	}    	
    }
    
    /**
     * Pretty-prints the cyclicity information about the n-nth local variable. 
     * @param n The position of the register among local variables.
     */
    public void askFor(Entry e,Register r) {
	List<FieldSet> l = findTuplesByRegister(e,r);
	System.out.println("CYCLICITY OF " + r + " = ");
	Iterator<FieldSet> iterator = l.listIterator();
	while (iterator.hasNext()) {
	    System.out.println(" * " + iterator.next());
	}
    }
    
    public void reinitialize(){
    	RelView view = getView();
    	TrioIterable<Entry,Register,FieldSet> iterable = view.getAry3ValTuples();
    	Iterator<Trio<Entry,Register,FieldSet>> iterator = iterable.iterator();
    	while(iterator.hasNext()){
    		Trio<Entry,Register,FieldSet> p = iterator.next();
    		relTuples.add(new Trio<Entry,Register,FieldSet>(p.val0,p.val1,p.val2));
    	}
    	run();
    	load();
    }
}	
