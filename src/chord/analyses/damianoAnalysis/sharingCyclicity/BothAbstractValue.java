package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

/**
 * This class manages the parallel execution of the "tuples" and the "BDD" (Binary
 * Decision Diagrams) implementation.  A {@literal BothAbstractValue} object contains
 * a {@literal TuplesAbstractValue} and a {@literal BDDAbstractValue}
 * object, and most methods simply call their corresponding methods.
 * 
 * @author damiano
 *
 */
public class BothAbstractValue extends AbstractValue {

	/**
	 * The {@literal TuplesAbstractValue} object
	 */
	private TuplesAbstractValue tuplesAV;
	
	/**
	 * Returns the {@literal TuplesAbstractValue} object.
	 * 
	 * @return
	 */
	public TuplesAbstractValue getTuplesPart() { return tuplesAV; }

	/**
	 * The {@literal BDDAbstractValue} object
	 */
	private BDDAbstractValue bddAV;

	/**
	 * Returns the {@literal BDDAbstractValue} object.
	 * 
	 * @return
	 */
	public BDDAbstractValue getBDDPart() { return bddAV; }
		
	/**
	 * Default constructor.  It creates both members.
	 * 
	 * @param entry
	 */
	public BothAbstractValue(Entry entry) {
		tuplesAV = new TuplesAbstractValue();
		bddAV = new BDDAbstractValue(entry);
	}

	/**
	 * This constructor is private because (1) it is never called from the outside;
	 * (2) in general, a {@literal BothAbstractValue} object is guaranteed to have
	 * both components non-null.
	 * 
	 * @param t
	 * @param b
	 */
	private BothAbstractValue(TuplesAbstractValue t, BDDAbstractValue b) {
		tuplesAV = t;
		bddAV = b;
	}

	/**
	 * Updates the existing sharing information with new sharing information stored
	 * in {@literal other}.
	 * 
	 * @param other The new abstract information
	 * @return whether The new information was not included in the old one
	 */
	public boolean updateSInfo(AbstractValue other) {
		boolean b = false;
		if (other instanceof TuplesAbstractValue) {
			b |= tuplesAV.updateSInfo(((TuplesAbstractValue) other));			
		} else if (other instanceof BDDAbstractValue) {
			b |= bddAV.updateSInfo(((BDDAbstractValue) other));			
		} else if (other instanceof BothAbstractValue) {
			if (tuplesAV != null) {
				b |= tuplesAV.updateSInfo(((BothAbstractValue) other).tuplesAV);
			} else tuplesAV = ((BothAbstractValue) other).tuplesAV;
			if (bddAV != null) {
				b |= bddAV.updateSInfo(((BothAbstractValue) other).bddAV);
			} else bddAV = ((BothAbstractValue) other).bddAV;
		}
		return b;
	}
	
	/**
	 * Updates the existing cyclicity information with new cyclicity information
	 * stored in {@literal other}.
	 * 
	 * @param other The new abstract information
	 * @return whether The new information was not included in the old one
	 */
	public boolean updateCInfo(AbstractValue other) {
		boolean b = false;
		if (other instanceof TuplesAbstractValue) {
			b |= tuplesAV.updateCInfo(((TuplesAbstractValue) other));			
		} else if (other instanceof BDDAbstractValue) {
			b |= bddAV.updateCInfo(((BDDAbstractValue) other));			
		} else if (other instanceof BothAbstractValue) {
			if (tuplesAV != null) {
				b |= tuplesAV.updateCInfo(((BothAbstractValue) other).tuplesAV);
			} else tuplesAV = ((BothAbstractValue) other).tuplesAV;
			if (bddAV != null) {
				b |= bddAV.updateCInfo(((BothAbstractValue) other).bddAV);
			} else bddAV = ((BothAbstractValue) other).bddAV;
		}
		return b;
	}
	
	/**
	 * Updates the existing definite aliasing information with new definite aliasing
	 * information stored in {@literal other}.
	 * 
	 * @param other The new abstract information
	 * @return whether The new information was not included in the old one
	 */
	public boolean updateAInfo(AbstractValue other) {
		boolean b = false;
		if (other instanceof TuplesAbstractValue) {
			b |= tuplesAV.updateAInfo(((TuplesAbstractValue) other));			
		} else if (other instanceof BDDAbstractValue) {
			b |= bddAV.updateAInfo(((BDDAbstractValue) other));			
		} else if (other instanceof BothAbstractValue) {
			if (tuplesAV != null) {
				b |= tuplesAV.updateAInfo(((BothAbstractValue) other).tuplesAV);
			} else tuplesAV = ((BothAbstractValue) other).tuplesAV;
			if (bddAV != null) {
				b |= bddAV.updateAInfo(((BothAbstractValue) other).bddAV);
			} else bddAV = ((BothAbstractValue) other).bddAV;
		}
		return b;
	}
	
	/**
	 * Updates the existing purity information with new purity information stored
	 * in {@literal other}.
	 * 
	 * @param other The new abstract information
	 * @return whether The new information was not included in the old one
	 */
	public boolean updatePInfo(AbstractValue other) {
		boolean b = false;
		if (other instanceof TuplesAbstractValue) {
			b |= tuplesAV.updatePInfo(((TuplesAbstractValue) other));			
		} else if (other instanceof BDDAbstractValue) {
			b |= bddAV.updatePInfo(((BDDAbstractValue) other));			
		} else if (other instanceof BothAbstractValue) {
			if (tuplesAV != null) {
				b |= tuplesAV.updatePInfo(((BothAbstractValue) other).tuplesAV);
			} else tuplesAV = ((BothAbstractValue) other).tuplesAV;
			if (bddAV != null) {
				b |= bddAV.updatePInfo(((BothAbstractValue) other).bddAV);
			} else bddAV = ((BothAbstractValue) other).bddAV;
		}
		return b;
	}
		
	/**
	 * Returns a new {@literal BothAbstractValue} object with the same abstract
	 * information.
	 * 
	 * @return a copy of itself
	 */
	public BothAbstractValue clone() {
		return new BothAbstractValue((TuplesAbstractValue) tuplesAV.clone(),
				(bddAV==null)? null : (BDDAbstractValue) bddAV.clone());
	}

	/**
	 * Adds a bit of sharing information.
	 * 
	 * @param r1
	 * @param r2
	 * @param fs1
	 * @param fs2
	 */
	public void addSInfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		tuplesAV.addSInfo(r1,r2,fs1,fs2); 
		bddAV.addSInfo(r1,r2,fs1,fs2);
	}

	/**
	 * Adds a bit of cyclicity information.
	 * 
	 * @param r1
	 * @param r2
	 * @param fs1
	 * @param fs2
	 */
	public void addCInfo(Register r, FieldSet fs) {
		tuplesAV.addCInfo(r,fs);
		bddAV.addCInfo(r,fs);
	}

	/**
	 * Adds a bit of definite aliasing information.
	 * 
	 * @param r1
	 * @param r2
	 * @param fs1
	 * @param fs2
	 */
	public void addAInfo(Register r1, Register r2) {
		tuplesAV.addAInfo(r1,r2); 
		bddAV.addAInfo(r1,r2);
	}

	/**
	 * Adds a bit of purity information.
	 * 
	 * @param r1
	 * @param r2
	 * @param fs1
	 * @param fs2
	 */
	public void addPInfo(Register r) {
		tuplesAV.addPInfo(r);
		bddAV.addPInfo(r);
	}

	/**
     * Copies information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
	public void copyInfo(Register source, Register dest) {
		tuplesAV.copyInfo(source,dest);
		bddAV.copyInfo(source,dest);
	}

	/**
     * Copies sharing information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
	public void copySInfo(Register source, Register dest) {
		tuplesAV.copySInfo(source,dest);
		bddAV.copySInfo(source,dest);
	}

	/**
     * Copies cyclicity information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
	public void copyCInfo(Register source, Register dest) {
		tuplesAV.copyCInfo(source,dest);
		bddAV.copyCInfo(source,dest);
	}

	/**
     * Copies definite aliasing information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
	public void copyAInfo(Register source, Register dest) {
		tuplesAV.copyAInfo(source,dest);
		bddAV.copyAInfo(source,dest);
	}

	/**
     * Copies purity information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
	public void copyPInfo(Register source, Register dest) {
		tuplesAV.copyPInfo(source,dest);
		bddAV.copyPInfo(source,dest);
	}

	/**
	 * Copies cyclicity information about base into self-sharing information
	 * about {@literal dest}.
	 * 
	 * @param base
	 * @param dest
	 */
	public void copySInfoFromC(Register base, Register dest) {
		tuplesAV.copySInfoFromC(base,dest);
		bddAV.copySInfoFromC(base,dest);
	}

	/**
     * Moves information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
	public void moveInfo(Register source, Register dest) {
		tuplesAV.moveInfo(source,dest);
		bddAV.moveInfo(source,dest);
	}

	/**
     * Moves sharing information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
	public void moveSInfo(Register source, Register dest) {
		tuplesAV.moveSInfo(source,dest);
		bddAV.moveSInfo(source,dest);
	}

	/**
     * Moves cyclicity information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
	public void moveCInfo(Register source, Register dest) {
		tuplesAV.moveCInfo(source,dest);
		bddAV.moveCInfo(source,dest);
	}

	/**
     * Moves defintie aliasing information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
	public void moveAInfo(Register source, Register dest) {
		tuplesAV.moveAInfo(source,dest);
		bddAV.moveAInfo(source,dest);
	}

	/**
     * Moves purity information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
	public void movePInfo(Register source, Register dest) {
		tuplesAV.movePInfo(source,dest);
		bddAV.movePInfo(source,dest);
	}

	/**
	 * Removes information about a register.
	 * 
	 * @param r
	 */
	public void removeInfo(Register r) {
		tuplesAV.removeInfo(r);
		bddAV.removeInfo(r);
	}

	/**
	 * Removes sharing information about a register.
	 * 
	 * @param r
	 */
	public void removeSInfo(Register r) {
		tuplesAV.removeSInfo(r);
		bddAV.removeSInfo(r);
	}

	/**
	 * Removes cyclicity information about a register.
	 * 
	 * @param r
	 */
	public void removeCInfo(Register r) {
		tuplesAV.removeCInfo(r);
		bddAV.removeCInfo(r);
	}

	/**
	 * Removes defintie aliasing information about a register.
	 * 
	 * @param r
	 */
	public void removeAInfo(Register r) {
		tuplesAV.removeAInfo(r);
		bddAV.removeAInfo(r);
	}

	/**
	 * Removes purity information about a register.
	 * 
	 * @param r
	 */
	public void removePInfo(Register r) {
		tuplesAV.removePInfo(r);
		bddAV.removePInfo(r);
	}

	/**
	 * Removes information about all registers which are not actual parameters.
	 * 
	 * @param entry
	 * @param actualParameters
	 */
	public void filterActual(Entry entry,List<Register> actualParameters) {
		tuplesAV.filterActual(entry, actualParameters);
		bddAV.filterActual(entry, actualParameters);
	}
	
	/**
	 * Computes the abstract information after a {@code getfield} instruction.
	 * 
	 * @param entry The entry containing the instruction
	 * @param q The Quad object
	 * @param base The register whose field is accessed
	 * @param dest The register where the result is stored 
	 * @param field The field to be accessed
	 * @return
	 */
	public BothAbstractValue doGetfield(Entry entry, Quad q, Register base,
			Register dest, jq_Field field) {
		return new BothAbstractValue(tuplesAV.doGetfield(entry,q,base,dest,field),
				bddAV.doGetfield(entry,q,base,dest,field));
	}

	/**
	 * Computes the abstract information after a {@code putfield} instruction.
	 * 
	 * @param entry The entry containing the instruction
	 * @param q The putfield instruction 
	 * @param v The register whose field is to be updated
	 * @param rho The new value to be stored
	 * @param field The field to be modified
	 * @return
	 */
	public BothAbstractValue doPutfield(Entry entry, Quad q, Register v,
			Register rho, jq_Field field) {
		return new BothAbstractValue(tuplesAV.doPutfield(entry,q,v,rho,field),
				bddAV.doPutfield(entry,q,v,rho,field));
	}

	/**
	 * Computes the abstract information after a method invocation.
	 * 
	 * @param entry The entry containing the invocation
	 * @param invokedEntry The invoked entry
	 * @param q The method invocation instruction
	 * @param actualParameters The list of actual parameters
	 * @param returnValue The return value of the invoked method (or null)
	 * @return
	 */
	public BothAbstractValue doInvoke(Entry entry, Entry invokedEntry,
			Quad q, ArrayList<Register> actualParameters, Register returnValue) {
		return new BothAbstractValue(tuplesAV.doInvoke(entry,invokedEntry,q,actualParameters,returnValue),
				bddAV.doInvoke(entry,invokedEntry,q,actualParameters,returnValue));
	}

	/**
	 * Gets a list of tuples ({@literal FieldSet},{@literal FieldSet}) representing
	 * the sharing information about the given two registers.
	 * 
	 * @param r1
	 * @param r2
	 * @return
	 */
	public ArrayList<Pair<FieldSet, FieldSet>> getStuples(Register r1,
			Register r2) {
		ArrayList<Pair<FieldSet, FieldSet>> tList = tuplesAV.getStuples(r1, r2);
		ArrayList<Pair<FieldSet, FieldSet>> bList = bddAV.getStuples(r1, r2);
		if (!tList.equals(bList))
			Utilities.warn("DIFFERENT LISTS FOR BOTH IMPLEMENTATIONS: " + tList + " / " + bList);
		return tList;
	}

	/**
	 * Gets a list of {@literal FieldSet} objects representing the cyclicity
	 * information about the given register.
	 * 
	 * @param r
	 * @return
	 */
	public ArrayList<FieldSet> getCtuples(Register r) {
		ArrayList<FieldSet> tList = tuplesAV.getCtuples(r);
		ArrayList<FieldSet> bList = bddAV.getCtuples(r);
		if (!tList.equals(bList))
			Utilities.warn("DIFFERENT LISTS FOR BOTH IMPLEMENTATIONS: " + tList + " / " + bList);
		return tList;
	}

	/**
	 * Returns true iff {@literal this} and {@literal av} contain equivalent
	 * abstract information.
	 * 
	 * @param av
	 * @return
	 */
	public boolean equals(AbstractValue av) {
		if (av instanceof BothAbstractValue)
			return tuplesAV.equals(((BothAbstractValue) av).getTuplesPart()) && bddAV.equals(((BothAbstractValue) av).getBDDPart());
		else return false;
	}	
	
	/**
	 * Self-explaining. 
	 */
	public String toString() {
		String sBDD = (bddAV != null) ? bddAV.toString() : "" ;
		return tuplesAV.toString() + " <-TUPLES / BDD-> " + sBDD; 
	}
	
}
