package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.List;

import chord.analyses.damianoAnalysis.Entry;
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
	
    public abstract void moveInfoList(List<Register> source,List<Register> dest);
    
	public abstract void copyFromCycle(Register base, Register dest);
	
	public abstract AbstractValue doGetfield(Entry entry, Quad q, Register base, Register dest, jq_Field field);
	
	public abstract AbstractValue doPutfield(Entry entry, Quad q, Register base, Register dest, jq_Field field);

	public abstract AbstractValue doInvoke(Entry entry, Entry invokedEntry, Quad q, ArrayList<Register> actualParameters, Register returnValue);

	public abstract void removeInfo(Register r);
    
	public abstract void removeInfoList(List<Register> rs);
        
	/**
	 * given a method, renames actual parameters into the corresponding formal parameters  
	 */
	public abstract void actualToFormal(List<Register> apl,Entry e);
	
	/**
	 * given a method, renames formal parameters into the corresponding actual parameters  
	 */
	public abstract void formalToActual(List<Register> apl,Entry e);
	
	public abstract void copyToGhostRegisters(Entry entry);
	
	public abstract void cleanGhostRegisters(Entry entry);

	/**
	 * Removes information about all registers which are not actual parameters
	 * 
	 * @param actualParameters the list of actual parameters (not exactly a list of
	 * registers, but the Register object can be retrieved easily)
	 */
	public abstract void filterActual(List<Register> actualParameters);

	public abstract ArrayList<Pair<FieldSet,FieldSet>> getStuples(Register r1, Register r2);
	
	public abstract ArrayList<FieldSet> getCtuples(Register r);

	public abstract boolean equals(AbstractValue av);
	
	public abstract String toString();
	
}
