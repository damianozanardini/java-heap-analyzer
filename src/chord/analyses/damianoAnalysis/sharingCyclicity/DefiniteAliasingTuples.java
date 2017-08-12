package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.Utilities;
import chord.bddbddb.Rel.PentIterable;
import chord.bddbddb.Rel.RelView;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Pent;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;

/**
 * This class implements the container for definite aliasing information as a
 * list of pairs (register,register).
 * 
 * Symmetric pairs (r,r) are ALWAYS in the definite aliasing relation, so there
 * is no need to keep them explicitly in the tuples.  This is true even if
 * r == null because we assume that null is aliasing with itself.
 * 
 * @author damiano
 *
 */
public class DefiniteAliasingTuples extends Tuples {

	private ArrayList<DefiniteAliasingTuple> tuples;
	
	/**
	 * Default constructor.  Since this is a "definite" analysis, the bottom
	 * element (corresponding to the maximum amount of information) is the list of
	 * all possible (non-symmetric) pairs of registers (as if they were all
	 * null and, therefore, aliasing with themselves).
	 * 
	 * @param entry The entry where the abstract value lives (only necessary to
	 * retrieve the list of registers)
	 */
	public DefiniteAliasingTuples(Entry entry) {
		tuples = new ArrayList<DefiniteAliasingTuple>();
		for (Register r1 : entry.getReferenceRegisters()) {
			for (Register r2 : entry.getReferenceRegisters()) {
				addTuple(new DefiniteAliasingTuple(r1,r2));
			}
		}
	}
	
	/**
	 * Creates a DefiniteAliasingTuples object with the given list of tuples.
	 * @param tuples
	 */
	public DefiniteAliasingTuples(ArrayList<DefiniteAliasingTuple> tuples) {
		this.tuples = tuples;
	}
	
	/**
	 * Since this analysis is a "definite" one, merging two abstract values
	 * boils down to list intersection.
	 * 
	 * @param others
	 * @return
	 */
	boolean meet(DefiniteAliasingTuples others) {
		boolean b = false;
		ArrayList<DefiniteAliasingTuple> otherTuples = others.getInfo();
		for (Iterator<DefiniteAliasingTuple> it = tuples.iterator(); it.hasNext(); ) {
			DefiniteAliasingTuple t = it.next();
			if (!otherTuples.contains(t)) {
				it.remove();
				b = true;
			}
		}
		return b;
	}

	public ArrayList<DefiniteAliasingTuple> getInfo() {
		return tuples;
	}
	
	public void setInfo(ArrayList<DefiniteAliasingTuple> tuples) {
		this.tuples = tuples;
	}
	
	/**
	 * Adds a tuple to the definite aliasing information.  A DefiniteAliaisingTuple
	 * is always ordered, so that there is no need to sort it here.
	 * 
	 * @param t
	 */
	public void addTuple(DefiniteAliasingTuple t) {
		if (!contains(t) && t.getR1()!=t.getR2()) tuples.add(t);
	}

	/**
	 * Adds a tuples from two registers. Registers are ordered by the
	 * DefiniteAliasingTuple constructor.  The check for register inequality is
	 * left to addTuple(DefiniteAliasingTuple).
	 * 
	 * @param r1
	 * @param r2
	 */
	public void addTuple(Register r1,Register r2) {
		addTuple(new DefiniteAliasingTuple(r1,r2));
	}

	/**
	 * This method models the copy of source to dest: dest will be definitely
	 * aliasing (1) with every register which was definitely aliasing with source;
	 * and (2) with source itself.  By construction of addTuple, a new tuple is
	 * added only if it is not symmetric. 
	 * 
	 * @param source
	 * @param dest
	 */
	public void copyInfo(Register source,Register dest) {
		if (source==null || dest==null) return;
		remove(dest);
		ArrayList<Pair<Register,Register>> newPairs = new ArrayList<Pair<Register,Register>>();
		for (Iterator<DefiniteAliasingTuple> it = tuples.iterator(); it.hasNext(); ) {
			DefiniteAliasingTuple tuple = it.next();
			Utilities.info("XXX (" + source + " -> " + dest + " ... " + tuple);
			if (tuple.getR1() == source) newPairs.add(new Pair<Register,Register>(dest,tuple.getR2()));
			if (tuple.getR2() == source) newPairs.add(new Pair<Register,Register>(tuple.getR1(),dest));
		}
		for (Pair<Register,Register> p: newPairs) addTuple (p.val0,p.val1);
		addTuple(source,dest);
	}
	
	/**
	 * This method models the copy of source from another DefiniteAliasingTuples
	 * object to dest of the current one: dest will be definitely aliasing
	 * (1) with every register which was definitely aliasing with source;
	 * and (2) with source itself.  By construction of addTuple, a new tuple is
	 * added only if it is not symmetric. 
	 * 
	 * @param other The other DefiniteAliasingTuples object
	 * @param source The source register
	 * @param dest The destination register
	 */
	public void copyInfoFrom(DefiniteAliasingTuples other,Register source,Register dest) {
		if (source==null || dest==null) return;
		remove(dest);
		for (DefiniteAliasingTuple t : other.getInfo()) {
			if (t.getR1() == source) addTuple(dest,t.getR2());
			if (t.getR2() == source) addTuple(t.getR1(),dest);
		}
		addTuple(source,dest);
	}

	/**
	 * This method replaces every occurrence of source with dest in the definite
	 * aliasing information.  If the transformed tuple is symmetric, it is removed. 
	 * 
	 * @param source
	 * @param dest
	 */
	public void moveInfo(Register source,Register dest) {
		if (source==null || dest==null) return;
		remove(dest);
		for (Iterator<DefiniteAliasingTuple> it = tuples.iterator(); it.hasNext(); ) {
			DefiniteAliasingTuple t = it.next();
			if (t.getR1() == source) {
				if (t.getR2() != dest) t.setR1(dest);
				else it.remove();
			} else if (t.getR2() == source)
				if (t.getR1() != dest) t.setR2(dest);
				else it.remove();
		}
	}

    /**
     * Finds all tuples in the relation whose first or second register is
     * {@code r}, and builds a list of registers which are definitely aliasing with
     * r, plus r itself. 
     */
    public ArrayList<Register> findTuplesByRegister(Register r) {
    		ArrayList<Register> list = new ArrayList<Register>();
    		list.add(r);
    		for (DefiniteAliasingTuple t : tuples) {
    			if (t.getR1() == r && !list.contains(t.getR2())) list.add(t.getR2());
    			if (t.getR2() == r && !list.contains(t.getR1())) list.add(t.getR1());
    		}
    		return list;
    }
    
	public void remove(Register r) {
		for (Iterator<DefiniteAliasingTuple> it = tuples.iterator(); it.hasNext(); ) {
			DefiniteAliasingTuple tuple = it.next();
			if (tuple.getR1() == r || tuple.getR2() == r) it.remove();
		}
	}
		
	/**
	 * Makes a copy of its tuples and returns a new DefiniteAliasingTuples object.
	 * The copy is shallow because Register objects need not to be duplicated, but 
	 * DefiniteAliasingTuple objects are duplicated.
	 */
	public DefiniteAliasingTuples clone() {
		ArrayList<DefiniteAliasingTuple> newTuples = new ArrayList<DefiniteAliasingTuple>();
		for (DefiniteAliasingTuple t : tuples) newTuples.add(t.clone());
		return new DefiniteAliasingTuples(newTuples);		
	}
	
	public void filterActual(List<Register> actualParameters) {
		for (Iterator<DefiniteAliasingTuple> it = tuples.iterator(); it.hasNext(); ) {
			DefiniteAliasingTuple t = it.next();
			if (!actualParameters.contains(t.getR1()) || !actualParameters.contains(t.getR2()))
				it.remove();
		}
	}
	
	public String toString() {
		String s = "";
		if (tuples.size()>0) {
			s = s + tuples.get(0);
			for (int i=1; i<tuples.size(); i++) { // index starts from 1 on purpose
				s = s + "-" + tuples.get(i);
			}
		}
		return s;
	}

	public boolean isBottom() {
		return tuples.size()==0;
	}

	public boolean contains(Tuple tuple) {
		if (tuple instanceof DefiniteAliasingTuple) {
			boolean found = false;
			for (DefiniteAliasingTuple t : tuples) found |= (tuple.equals(t));
			return found;
		} else return false;
	}


}
