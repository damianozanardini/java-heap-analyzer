package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.List;

import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.var.DomV;
import chord.project.ClassicProject;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * Abstract class modeling the abstract information computed by all the analyses.
 * Most methods are abstract because their definition is left to the subclasses
 * (which represent the different implementations of the static analysis that
 * can be used in the tool), but some of them are defined in this class because
 * they are implementation-independent.
 * 
 * An abstract value includes information about field-sensitive sharing and
 * cyclicity, together with auxiliary analyses such as definite aliasing and purity.
 * 
 * @author damiano
 *
 */
public abstract class AbstractValue {
	
	/**
	 * Updates the existing information with new information stored in other.
	 * 
	 * @param other the new abstract information
	 * @return whether the new information was not included in the old one
	 */
	public boolean updateInfo(AbstractValue other) {
		boolean b = false;
		b |= updateSInfo(other);
		b |= updateCInfo(other);
		b |= updateAInfo(other);
		b |= updatePInfo(other);
		return b;
	}
	
	/**
	 * Updates the existing sharing information with new sharing information stored
	 * in "other".
	 * 
	 * @param other The new abstract information
	 * @return whether the new information was not included in the old one
	 */
	public abstract boolean updateSInfo(AbstractValue other);
	
	/**
	 * Updates the existing cyclicity information with new cyclicity information
	 * stored in "other".
	 * 
	 * @param other The new abstract information
	 * @return whether the new information was not included in the old one
	 */
	public abstract boolean updateCInfo(AbstractValue other);
	
	/**
	 * Updates the existing definite aliasing information with new definite aliasing
	 * information stored in "other".
	 * 
	 * @param other The new abstract information
	 * @return whether the new information was not included in the old one
	 */
	public abstract boolean updateAInfo(AbstractValue other);

	/**
	 * Updates the existing purity information with new purity information stored
	 * in "other".
	 * 
	 * @param other The new abstract information
	 * @return whether the new information was not included in the old one
	 */
	public abstract boolean updatePInfo(AbstractValue other);

	/**
	 * Returns a new AbstractValue object with the same abstract information.
	 * The copy is neither completely deep nor completely shallow:
	 * - it is not completely shallow because Tuple (or subclasses of it) objects
	 *   are duplicated;
	 * - it is not completely deep because Register objects are not duplicated.
	 * 
	 * @return a copy of itself
	 */
	public abstract AbstractValue clone();
	
	/**
	 * Adds a bit of sharing information.
	 * 
	 * @param r1
	 * @param r2
	 * @param fs1
	 * @param fs2
	 */
	public abstract void addSInfo(Register r1,Register r2,FieldSet fs1,FieldSet fs2);
	
	/**
	 * Adds a bit of cyclicity information.
	 * 
	 * @param r
	 * @param fs
	 */
	public abstract void addCInfo(Register r,FieldSet fs);
	
	/**
	 * Adds a bit of definite aliasing information.
	 * 
	 * @param r1
	 * @param r2
	 */
	public abstract void addAInfo(Register r1,Register r2);
	
	/**
	 * Adds a bit of purity information.
	 * 
	 * @param r
	 */
	public abstract void addPInfo(Register r);

	/**
     * Copies information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
    public abstract void copyInfo(Register source,Register dest);

	/**
     * Copies sharing information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
    public abstract void copySInfo(Register source,Register dest);
    
	/**
     * Copies cyclicity information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
    public abstract void copyCInfo(Register source,Register dest);
    
	/**
     * Copies definite aliasing information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
    public abstract void copyAInfo(Register source,Register dest);
    
	/**
     * Copies purity information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
    public abstract void copyPInfo(Register source,Register dest);
    
	/**
	 * Copies cyclicity information about base into self-sharing information about dest.
	 * 
	 * @param base
	 * @param dest
	 */
	public abstract void copySInfoFromC(Register base, Register dest);

	/**
     * Moves information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
    public abstract void moveInfo(Register source,Register dest);
	
	/**
     * Moves sharing information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
    public abstract void moveSInfo(Register source,Register dest);
	
	/**
     * Moves cyclicity information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
    public abstract void moveCInfo(Register source,Register dest);
	
	/**
     * Moves definite aliasing information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
    public abstract void moveAInfo(Register source,Register dest);
	
	/**
     * Moves purity information from a register to another.
     * 
     * @param source The source register
     * @param dest The destination register
     * @return
     */
    public abstract void movePInfo(Register source,Register dest);
	
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
    
	/**
	 * Removes information about a register.
	 * 
	 * @param r
	 */
	public abstract void removeInfo(Register r);

	/**
	 * Removes sharing information about a register.
	 * 
	 * @param r
	 */
	public abstract void removeSInfo(Register r);

	/**
	 * Removes cyclicity information about a register.
	 * 
	 * @param r
	 */
	public abstract void removeCInfo(Register r);

	/**
	 * Removes definite aliasing information about a register.
	 * 
	 * @param r
	 */
	public abstract void removeAInfo(Register r);

	/**
	 * Removes purity information about a register.
	 * 
	 * @param r
	 */
	public abstract void removePInfo(Register r);

	/**
	 * Computes the abstract information after a getfield instruction.
	 * 
	 * @param entry The entry containing the instruction
	 * @param q The Quad object
	 * @param base The register whose field is accessed
	 * @param dest The register where the result is stored 
	 * @param field The field to be accessed
	 * @return
	 */
	public abstract AbstractValue doGetfield(Entry entry, Quad q, Register base, Register dest, jq_Field field);
	
	/**
	 * Computes the abstract information after a putfield instruction.
	 * 
	 * @param entry The entry containing the instruction
	 * @param q The putfield instruction 
	 * @param v The register whose field is to be updated
	 * @param rho The new value to be stored
	 * @param field The field to be modified
	 * @return
	 */
	public abstract AbstractValue doPutfield(Entry entry, Quad q, Register v, Register rho, jq_Field field);

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
	public abstract AbstractValue doInvoke(Entry entry, Entry invokedEntry, Quad q, ArrayList<Register> actualParameters, Register returnValue);

	/**
	 * Removes the information about a list of registers.  The corresponding removeInfo
	 * method is called depending on the type of "this".
	 * 
	 * @param rs
	 */
	public void removeInfoList(List<Register> rs) {
		for (Register r : rs) { removeInfo(r); }
	}
        
	/**
	 * Removes the information about the given list of actual parameters.
	 * It is similar to removeInfoList, but only removes information about 
	 * non-temporary (R_) registers.
	 * 
	 * @param rs
	 */
	public void removeActualParameters(List<Register> rs) {
		for (Register r : rs) {
			if (r.isTemp()) removeInfo(r);
		}
	}

	/**
	 * Renames actual parameters into formal parameters.
	 * 
	 * @param apl
	 * @param e
	 */
	public void actualToFormal(List<Register> apl,Entry e) {
		Utilities.begin("ACTUAL " + apl + " TO FORMAL FROM " + this);
		for (int i=0; i<apl.size(); i++) {
			// non-reference registers are also taken, because the list of 
			// actual parameters includes them
			Register source = apl.get(i);
			Register dest = e.getNthRegister(i);
			if (source.getType().isPrimitiveType()) {
				Utilities.info(source + " HAS PRIMITIVE TYPE");
			} else {
				Utilities.info("MOVING " + source + " TO " + dest);
				moveInfo(source,dest);
			}
		}
		Utilities.end("ACTUAL " + apl + " TO FORMAL RESULTING IN " + this);
	}
	
	/**
	 * Renames formal parameters into actual parameters.
	 * 
	 * @param apl the list of actual parameters
	 * @param rho the return register (in case the method has returns a value)
	 * @param e the invoked entry
	 */
	public void formalToActual(List<Register> apl,Register rho,Entry e) {
		Utilities.begin("FORMAL FROM " + this + " TO ACTUAL "+ apl);
		// remove information about registers which are not formal parameters
		Register out = GlobalInfo.getReturnRegister(e.getMethod());
		for (int j=apl.size(); j<e.getNumberOfRegisters(); j++) {
			Register rx = e.getNthRegister(j);
			if (rx!=out) removeInfo(rx);
		}
		for (int i=0; i<apl.size(); i++) {
			// non-reference registers are also taken, because the list of 
			// actual parameters includes them
			Register dest = apl.get(i);
			Register source = e.getNthRegister(i);
			if (source.getType().isPrimitiveType()) {
				Utilities.info(source + " HAS PRIMITIVE TYPE");
			} else {
				Utilities.info("MOVING " + source + " TO " + dest);
				moveInfo(source,dest);
			}
		}
		if (out != null && rho != null) {
			Utilities.info("MOVING " + out + " TO " + rho + " (RETURN VALUE)");
			moveInfo(out,rho);		
		}
		Utilities.end("FORMAL TO ACTUAL " + apl + " RESULTING IN " + this);
	}

	/**
	 * Copies the information about a register into its corresponding ghost
	 * register (if it has one).
	 * 
	 * @param entry
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
	 * Takes the information about ghost registers and moves it to their non-ghost
	 * counterpart.  The original information about non-ghost registers is lost.
	 * 
	 * @param entry
	 */
	public void cleanGhostRegisters(Entry entry) {
		Utilities.begin("CLEANING GHOST INFORMATION");
		jq_Method method = entry.getMethod();
		for (Register r : entry.getReferenceFormalParameters()) {
			if (!GlobalInfo.isGhost(method,r)) {
				Register rprime = GlobalInfo.getGhostCopy(method,r);
				removeInfo(r);
				moveInfo(rprime,r);
			}
		}
		Utilities.end("CLEANING GHOST INFORMATION");
	}

	/**
	 * Removes information about all registers which are not actual parameters.
	 * 
	 * @param entry
	 * @param actualParameters
	 */
	public abstract void filterActual(Entry entry,List<Register> actualParameters);

	/**
	 * Gets a list of tuples (FieldSet,FieldSet) representing the sharing information
	 * about the given two registers.
	 * 
	 * @param r1
	 * @param r2
	 * @return
	 */
	public abstract ArrayList<Pair<FieldSet,FieldSet>> getStuples(Register r1, Register r2);
	
	/**
	 * Gets a list of FieldSet objects representing the cyclicity information about
	 * the given register.
	 * 
	 * @param r
	 * @return
	 */
	public abstract ArrayList<FieldSet> getCtuples(Register r);

	/**
	 * Returns true iff {@code this} and {@code av} contain equivalent abstract
	 * information.
	 * 
	 * @param av
	 * @return
	 */
	public abstract boolean equals(AbstractValue av);
	
	/**
	 * Pretty-prints the abstract information.
	 */
	public abstract String toString();
	
}
