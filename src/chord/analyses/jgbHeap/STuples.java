package chord.analyses.jgbHeap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;

public class STuples extends Tuples {

	private ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuples;
	
	public STuples() {
		tuples = new ArrayList<Quad<Register,Register,FieldSet,FieldSet>>();
	}
	
	public STuples(ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuples) {
		this.tuples = tuples;
	}
	
	boolean join(STuples others) {
		boolean newStuff = false;
		for (Quad<Register,Register,FieldSet,FieldSet> t : others.getTuples()) {
			if (!tuples.contains(t)) {
				tuples.add(t);
				newStuff = true;
			}
		}
		return newStuff;
	}

	public ArrayList<Quad<Register,Register,FieldSet,FieldSet>> getTuples() {
		return tuples;
	}
	
	public void setTuples(ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuples){ this.tuples = tuples; }

	public ArrayList<Quad<Register,Register,FieldSet,FieldSet>> moveTuples(Register source, Register dest){
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> movedTuples = new ArrayList<>();
		
		for(Trio<Register,FieldSet,FieldSet> t : findTuplesByBothRegisters(source))
			movedTuples.add(new Quad<Register,Register,FieldSet,FieldSet>(dest,dest,t.val1,t.val2));
		for(Trio<Register,FieldSet,FieldSet> t : findTuplesByFirstRegister(source))
    		movedTuples.add(new Quad<Register,Register,FieldSet,FieldSet>(dest,t.val0,t.val1,t.val2));	
    	for(Trio<Register,FieldSet,FieldSet> t : findTuplesBySecondRegister(source))
    		movedTuples.add(new Quad<Register,Register,FieldSet,FieldSet>(t.val0,dest,t.val1,t.val2));
    	
    	if(movedTuples.size() > 0){
    		Utilities.out("");
    		Utilities.out("\t\t - TUPLES OF SHARING AFTER MOVING");
    		for(Quad<Register,Register,FieldSet,FieldSet> p: movedTuples){
        		Utilities.out("\t\t (" + p.val0 + "," + p.val1 + "," + p.val2 + "," + p.val3);
        	}
    	}
    	
    	return movedTuples;
	}
	
	private List<Trio<Register,FieldSet,FieldSet>> findTuplesByFirstRegister(Register r){
		
		List<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<>();
		
		Iterator<Quad<Register,Register,FieldSet,FieldSet>> it = tuples.iterator();
		while(it.hasNext()){
			Quad<Register,Register,FieldSet,FieldSet> quad = it.next();
    		if (quad.val0 == r && quad.val1 != r){
    			list.add(new Trio<Register,FieldSet,FieldSet>(quad.val1,quad.val2,quad.val3));
    		}
    	}  
    	return list;
		
	}
	
	private List<Trio<Register,FieldSet,FieldSet>> findTuplesBySecondRegister(Register r){
		
		List<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<>();
		Iterator<Quad<Register,Register,FieldSet,FieldSet>> it = tuples.iterator();
		while(it.hasNext()){
			Quad<Register,Register,FieldSet,FieldSet> quad = it.next();
    		if (quad.val1 == r && quad.val0 != r){
    			list.add(new Trio<Register,FieldSet,FieldSet>(quad.val0,quad.val2,quad.val3));
    		}
    	}
    	return list;
	}
	
	private List<Trio<Register,FieldSet,FieldSet>> findTuplesByBothRegisters(Register r){
		
		List<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<>();
		Iterator<Quad<Register,Register,FieldSet,FieldSet>> it = tuples.iterator();
		while(it.hasNext()){
			Quad<Register,Register,FieldSet,FieldSet> quad = it.next();
    		if (quad.val0 == r && quad.val1 == r){
    			list.add(new Trio<Register,FieldSet,FieldSet>(quad.val0,quad.val2,quad.val3));
    		}
    	}
    	return list;
	}
	
}
