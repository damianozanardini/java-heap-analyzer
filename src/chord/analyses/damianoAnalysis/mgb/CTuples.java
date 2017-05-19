package chord.analyses.damianoAnalysis.mgb;

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

	/**
	 * This method moves the tuples of a list of registers to a other list of registers. The position
	 * of the origin register in the source list corresponds with the position of the destination register in the
	 * dest list.
	 * 
	 * @param source
	 * @param dest
	 * @return
	 */
	public ArrayList<Pair<Register, FieldSet>> moveTuplesList(List<Register> source, List<Register> dest) {
		// COPY OF TUPLES BECAUSE FIRST IT IS NEEDED TO DELETE ALL TUPLES OF SOURCE REGISTER
		ArrayList<Pair<Register,FieldSet>> movedTuples = new ArrayList<>();
		movedTuples.addAll(tuples);
		assert(source.size() == dest.size());
				
		// ADD THE TUPLES OF THE OLD REGISTER TO THE NEW
		for(int i = 0; i < source.size(); i++){
			for(int j = 0; j < movedTuples.size(); j++){
				Pair<Register,FieldSet> p = movedTuples.get(j);
				if(p.val0 == source.get(i))
					movedTuples.set(j,new Pair<Register,FieldSet>(dest.get(i),p.val1));
			}
		}
		
		if(movedTuples.size() > 0){
			Utilities.out("");
			Utilities.out("\t\t - TUPLES OF CYCLICITY AFTER MOVING");
			for(Pair<Register,FieldSet> p: movedTuples)
				Utilities.out("\t\t (" + p.val0 + "," + p.val1 + ")");
			
		}
						
		return movedTuples;
	}
}
