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
				Utilities.out("///////-------***** TUPLA ADDED: (" + t.val0 + "," + t.val1 + "," + t.val2 + "," + t.val3 + ")");
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

	public void moveTuples(Register source, Register dest){
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> copiedTuples = new ArrayList<>();
		copiedTuples.addAll(this.tuples);
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuplesToDelete = new ArrayList<>();
		
		// DELETE ALL TUPLES OF SOURCE REGISTER FOR IF THE SOURCE AND DEST ARE THE SAME
		for(Quad<Register,Register,FieldSet,FieldSet> p : tuples){
			if(p.val0 == source || p.val1 == source) tuplesToDelete.add(p);
		}
		Utilities.out("");
		Utilities.out("\t TUPLES DELETED");		
		for(Quad<Register,Register,FieldSet,FieldSet> t : tuplesToDelete){
			//Utilities.out("\t R: " + t.val0 + ", R: " + t.val1 + ", F: " +t.val2 + ", F: " + t.val3);
			tuples.remove(t);
		}
		
		for(Trio<Register,FieldSet,FieldSet> t : findTuplesByBothRegisters(source,copiedTuples))
			tuples.add(new Quad<Register,Register,FieldSet,FieldSet>(dest,dest,t.val1,t.val2));
		for (Trio<Register,FieldSet,FieldSet> t : findTuplesByFirstRegister(source,copiedTuples))
    		tuples.add(new Quad<Register,Register,FieldSet,FieldSet>(dest,t.val0,t.val1,t.val2));	
    	for (Trio<Register,FieldSet,FieldSet> t : findTuplesBySecondRegister(source,copiedTuples))
    		tuples.add(new Quad<Register,Register,FieldSet,FieldSet>(t.val0,dest,t.val1,t.val2));
    	
    	if(tuples.size() > 0){
    		Utilities.out("");
    		Utilities.out("\t TUPLES SHARING AFTER MOVING");
    	}
    	for(Quad<Register,Register,FieldSet,FieldSet> p: tuples){
    		//Utilities.out("\t R: " + p.val0 + ", R: " + p.val1 + ", F: " + p.val2 + ", F: " + p.val3);
    	}
	}
	
	private List<Trio<Register,FieldSet,FieldSet>> findTuplesByFirstRegister(Register r, ArrayList<Quad<Register,Register,FieldSet,FieldSet>> copiedTuples){
		
		List<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<>();
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuplesToDelete = new ArrayList<>();
		
		Iterator<Quad<Register,Register,FieldSet,FieldSet>> it = copiedTuples.iterator();
		while(it.hasNext()){
			Quad<Register,Register,FieldSet,FieldSet> quad = it.next();
    		if (quad.val0 == r){
    			tuplesToDelete.add(quad);
    			list.add(new Trio<Register,FieldSet,FieldSet>(quad.val1,quad.val2,quad.val3));
    		}
    	}  
		copiedTuples.removeAll(tuplesToDelete);
    	return list;
		
	}
	
	private List<Trio<Register,FieldSet,FieldSet>> findTuplesBySecondRegister(Register r, ArrayList<Quad<Register,Register,FieldSet,FieldSet>> copiedTuples){
		
		List<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<>();
		Iterator<Quad<Register,Register,FieldSet,FieldSet>> it = copiedTuples.iterator();
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuplesToDelete = new ArrayList<>();
		while(it.hasNext()){
			Quad<Register,Register,FieldSet,FieldSet> quad = it.next();
    		if (quad.val1 == r){
    			tuplesToDelete.remove(quad);
    			list.add(new Trio<Register,FieldSet,FieldSet>(quad.val0,quad.val2,quad.val3));
    		}
    	}
		copiedTuples.removeAll(tuplesToDelete);
    	return list;
	}
	
	private List<Trio<Register,FieldSet,FieldSet>> findTuplesByBothRegisters(Register r, ArrayList<Quad<Register,Register,FieldSet,FieldSet>> copiedTuples){
		
		List<Trio<Register,FieldSet,FieldSet>> list = new ArrayList<>();
		Iterator<Quad<Register,Register,FieldSet,FieldSet>> it = copiedTuples.iterator();
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> tuplesToDelete = new ArrayList<>();
		while(it.hasNext()){
			Quad<Register,Register,FieldSet,FieldSet> quad = it.next();
    		if (quad.val0 == r && quad.val1 == r){
    			tuplesToDelete.remove(quad);
    			list.add(new Trio<Register,FieldSet,FieldSet>(quad.val0,quad.val2,quad.val3));
    		}
    	}
		copiedTuples.removeAll(tuplesToDelete);
    	return list;
	}
	
}
