package chord.analyses.jgbHeap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;
import joeq.Compiler.Quad.RegisterFactory.Register;

public class CTuples extends Tuples {

	private ArrayList<Pair<Register,FieldSet>> tuples;
	
	public CTuples() {
		tuples = new ArrayList<Pair<Register,FieldSet>>();
	}
	
	public CTuples(ArrayList<Pair<Register,FieldSet>> tuples) {
		this.tuples = tuples;
	}
	
	boolean join(CTuples others) {
		boolean newStuff = false;
		for (Pair<Register,FieldSet> p : others.getTuples()) {
			if (!tuples.contains(p)) {
				//Utilities.out("///////-------***** TUPLE ADDED : (" + p.val0 + "," + p.val1 + ")");
				tuples.add(p);
				newStuff = true;
			}
		}
		return newStuff;
	}

	public ArrayList<Pair<Register,FieldSet>> getTuples() {
		return tuples;
	}
	
	public void setTuples(ArrayList<Pair<Register,FieldSet>> tuples){ this.tuples = tuples; }
	
	
	public void moveTuples(Register source, Register dest){
		// COPY OF TUPLES BECAUSE FIRST IT IS NEEDED TO DELETE ALL TUPLES OF SOURCE REGISTER
		ArrayList<Pair<Register,FieldSet>> copiedTuples = new ArrayList<>();
		copiedTuples.addAll(this.tuples);
		ArrayList<Pair<Register,FieldSet>> tuplesToDelete = new ArrayList<>(); 
		
		// DELETE ALL TUPLES OF SOURCE REGISTER FOR IF THE SOURCE AND DEST ARE THE SAME
		for(Pair<Register,FieldSet> pair : tuples)
			if(pair.val0 == source) tuplesToDelete.add(pair);
		Utilities.out("");
		Utilities.out("\t TUPLES DELETED");
		for(Pair<Register,FieldSet> p : tuplesToDelete){
			//Utilities.out("\t R: " + p.val0 + ", F: " + p.val1);
			tuples.remove(p);
		}
		
		// ADD THE TUPLES OF THE OLD REGISTER TO THE NEW
		for (FieldSet f : findTuplesByRegister(source,copiedTuples))
			tuples.add(new Pair<Register,FieldSet>(dest,f));
		
		if(tuples.size() > 0 ){
			Utilities.out("");
			Utilities.out("\t TUPLES CYCLE AFTER MOVING");
		}
		for(Pair<Register,FieldSet> p: tuples){
    		//Utilities.out("\t R: " + p.val0 + ", F: " + p.val1);
    	}
	}
	
	protected List<FieldSet> findTuplesByRegister(Register r, ArrayList<Pair<Register,FieldSet>> copiedTuples){
		Iterator<Pair<Register,FieldSet>> iterator = copiedTuples.iterator();
    	List<FieldSet> list = new ArrayList<FieldSet>();
    	while (iterator.hasNext()) {
    		Pair<Register,FieldSet> pair = iterator.next();
    		if (pair.val0 == r) list.add(pair.val1);
    	}    	
    	return list;
	}
}
