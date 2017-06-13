package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.bddbddb.Rel.RelView;
import chord.bddbddb.Rel.TrioIterable;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Pent;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.RegisterFactory;
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
	
    public abstract void removeInfo(Register r);
    
	public abstract void removeInfoList(List<Register> rs);
        
	/**
	 * given a method, renames actual parameters into the corresponding formal parameters  
	 */
	public abstract void actualToFormal(List<Register> apl,jq_Method m);
	
	/**
	 * given a method, renames formal parameters into the corresponding actual parameters  
	 */
	public abstract void formalToActual(List<Register> apl,jq_Method m);
	
	public abstract void copyToGhostRegisters(Entry entry);
	
	public abstract void cleanGhostRegisters(Entry entry);

	/**
	 * Removes information about all registers which are not actual parameters
	 * 
	 * @param actualParameters the list of actual parameters (not exactly a list of
	 * registers, but the Register object can be retrieved easily)
	 */
	public abstract void filterActual(List<Register> actualParameters);

	public abstract List<Pair<FieldSet,FieldSet>> getSinfo(Register r1,Register r2);
	
	public abstract List<Pair<Register,FieldSet>> getSinfoReachingRegister(Register r);
	
	public abstract List<Pair<Register,FieldSet>> getSinfoReachedRegister(Register r);

	public abstract List<FieldSet> getSinfoReachingReachedRegister(Register r1, Register r2);

	public abstract List<Trio<Register,FieldSet,FieldSet>> getSinfoFirstRegister(Register r);
	
	public abstract List<Trio<Register,FieldSet,FieldSet>> getSinfoSecondRegister(Register r);

	public abstract List<FieldSet> getCinfo(Register r);

	public abstract String toString();

	public abstract boolean isBottom();

	
}
