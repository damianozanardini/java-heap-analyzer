package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.List;

import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;

public abstract class AbstractValue {
		
	/**
	 * updates the existing information with new information stored in other
	 * 
	 * @param other the new abstract information
	 * @return whether the new information was not included in the old one
	 */
	public abstract boolean update(AbstractValue other);
	
	/**
	 * returns a new AbstractValue object with the same abstract information.
	 * The copy is neither completely deep nor completely shallow: for example,
	 * Register objects are not duplicated.
	 * 
	 * @return a copy of itself
	 */
	public abstract AbstractValue clone();
	
	public abstract void addSinfo(Register r1,Register r2,FieldSet fs1,FieldSet fs2);
	
	public abstract void addCinfo(Register r,FieldSet fs);
	
    /**
     * This method does the job of copying information from a register to another.
     * @param source The source register
     * @param dest The destination register
     * @return
     */
    public abstract void copyInfo(Register source,Register dest);

    public abstract void copySinfo(Register source,Register dest);
    
    public abstract void copyCinfo(Register source,Register dest);
    
    public abstract void moveInfo(Register source,Register dest);
	
    public abstract void moveSinfo(Register source,Register dest);
	
    public abstract void moveCinfo(Register source,Register dest);
	
	/**
	 * Extension of moveInfo for lists of register (a list of source registers and
	 * a list of destination registers, which are supposed to have the same length).
	 *
	 * @param source the original (source) register list
	 * @param dest the destination register list
	 */
	public void moveInfoList(List<Register> source, List<Register> dest) {
		for (int i=0; i<source.size(); i++) moveInfo(source.get(i),dest.get(i));
	}
    
	public abstract void copyFromCycle(Register base, Register dest);
	
	public abstract AbstractValue doGetfield(Entry entry, Quad q, Register base, Register dest, jq_Field field);
	
	public abstract AbstractValue doPutfield(Entry entry, Quad q, Register base, Register dest, jq_Field field);

	public abstract AbstractValue doInvoke(Entry entry, Entry invokedEntry, Quad q, ArrayList<Register> actualParameters, Register returnValue);

	public abstract void removeInfo(Register r);
    
	/**
	 * Removes the information about a list of registers. The corresponding removeInfo
	 * method is called depending on the type of this.
	 * 
	 * @param rs
	 */
	public void removeInfoList(List<Register> rs) {
		for (Register r : rs) { removeInfo(r); }
	}
        
	/**
	 * Renames actual parameters into formal parameters.
	 */
	public void actualToFormal(List<Register> apl,Entry e) {
		Utilities.begin("ACTUAL " + apl + " TO FORMAL FROM " + this);
		for (int i=0; i<apl.size(); i++) {
			try {
				Register dest = e.getNthReferenceRegister(i);
				moveInfo(apl.get(i),dest);
			} catch (IndexOutOfBoundsException exc) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		Utilities.end("ACTUAL " + apl + " TO FORMAL RESULTING IN " + this);
	}
	
	/**
	 * Renames formal parameters into actual parameters.
	 */
	public void formalToActual(List<Register> apl,Register rho,Entry e) {
		Utilities.begin("FORMAL FROM " + this + " TO ACTUAL "+ apl);
		for (int i=0; i<apl.size(); i++) {
			try {
				Register source = e.getNthReferenceRegister(i);
				moveInfo(source,apl.get(i));
			} catch (IndexOutOfBoundsException exc) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		Register out = GlobalInfo.getReturnRegister(e.getMethod());
		if (out != null && rho != null) moveInfo(out,rho);		
		Utilities.end("FORMAL TO ACTUAL " + apl + " RESULTING IN " + this);
	}

	/**
	 * Copies the information about a register into the information about its corresponding
	 * ghost register (if it has one).
	 */
	public void copyToGhostRegisters(Entry entry) {
		jq_Method method = entry.getMethod();
		Utilities.begin("COPY TO GHOST REGISTERS - " + this + " - " + GlobalInfo.getGhostCopy(method));
		for (Register r : entry.getReferenceRegisters()) {
			if (!r.getType().isPrimitiveType()) {
				Register ghost = GlobalInfo.getGhostCopy(method,r);
				if (ghost!=null) copyInfo(r,ghost);
			}
		}
		Utilities.end("COPY TO GHOST REGISTERS - " + this + " - " + GlobalInfo.getGhostCopy(method));
	}

	/**
	 * Takes the information about ghost registers and moves it to their non-ghost counterpart.
	 * The original information about non-ghost registers is lost.
	 */
	public void cleanGhostRegisters(Entry entry) {
		Utilities.begin("CLEANING GHOST INFORMATION");
		jq_Method method = entry.getMethod();
		for (Register r : entry.getReferenceFormalParameters()) {
			if (!r.getType().isPrimitiveType() && !GlobalInfo.isGhost(method,r)) {
				Register rprime = GlobalInfo.getGhostCopy(method,r);
				removeInfo(r);
				moveInfo(rprime,r);
			}
		}
		Utilities.end("CLEANING GHOST INFORMATION");
	}

	/**
	 * Removes information about all registers which are not actual parameters
	 */
	public abstract void filterActual(Entry entry,List<Register> actualParameters);

	public abstract ArrayList<Pair<FieldSet,FieldSet>> getStuples(Register r1, Register r2);
	
	public abstract ArrayList<FieldSet> getCtuples(Register r);

	public abstract boolean equals(AbstractValue av);
	
	public abstract String toString();
	
}
