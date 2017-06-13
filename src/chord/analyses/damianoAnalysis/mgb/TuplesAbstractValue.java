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


// WARNING: should put some sort of order on Registers in order to avoid redundancies 
public class TuplesAbstractValue extends AbstractValue {

	private STuples sComp;
	private CTuples cComp;
	
	public TuplesAbstractValue() {
		sComp = new STuples();
		cComp = new CTuples();
	}
	
	protected TuplesAbstractValue(STuples st,CTuples ct) {
		sComp = st;
		cComp = ct;
	}
	
	public boolean update(AbstractValue other) {
		if (other == null) return false;
		if (other instanceof TuplesAbstractValue)
			return (sComp.join(((TuplesAbstractValue) other).getSComp()) | cComp.join(((TuplesAbstractValue) other).getCComp()));
		// should never happen
		else return false;
	}
	
	/**
	 * Returns the sharing component of the abstract value
	 * 
	 * @return
	 */	
	protected STuples getSComp() {
		return sComp;
	}
	
	/**
	 * Returns the cyclicity component of the abstract value
	 * 
	 * @return
	 */
	protected CTuples getCComp() {
		return cComp;
	}
		
	protected void setSComp(STuples stuples){
		this.sComp = stuples;
	}
	
	protected void setCComp(CTuples ctuples){
		this.cComp = ctuples;
	}

	/**
	 * Returns a new AbstractValue object with the same abstract information.
	 * The copy is neither completely deep nor completely shallow: for example,
	 * Register objects are not duplicated.
	 * 
	 * @return a copy of itself
	 */
	public TuplesAbstractValue clone() {
		return new TuplesAbstractValue(sComp.clone(),cComp.clone());
	}
	
	/**
	 * In tuples, renames actual parameters into the corresponding formal parameters  
	 */
	public void actualToFormal(List<Register> apl,jq_Method m) {
		Utilities.begin("ACTUAL " + apl + " TO FORMAL FROM " + this);
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.size(); i++) {
			try {
				dest.add(RegisterManager.getRegFromNumber(m,i));
				source.add(apl.get(i));
			} catch (IndexOutOfBoundsException e) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		sComp.moveTuplesList(source,dest);
		cComp.moveTuplesList(source,dest);
		Utilities.end("ACTUAL " + apl + " TO FORMAL RESULTING IN " + this);
	}
	
	/**
	 * In tuples, renames formal parameters into the corresponding actual parameters  
	 */
	public void formalToActual(List<Register> apl,jq_Method m) {
		Utilities.begin("FORMAL FROM " + this + " TO ACTUAL "+ apl);
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.size(); i++) {
			try {
				source.add(RegisterManager.getRegFromNumber(m,i));
				dest.add(apl.get(i));
			} catch (IndexOutOfBoundsException e) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		sComp.moveTuplesList(source,dest);
		cComp.moveTuplesList(source,dest);
		Utilities.end("FORMAL TO ACTUAL " + apl + " RESULTING IN " + this);
	}
	
	public void addSinfo(Register r1,Register r2,FieldSet fs1,FieldSet fs2) {
		getSComp().addTuple(r1, r2, fs1, fs2);
	}
	
	public void addCinfo(Register r,FieldSet fs) {
		getCComp().addTuple(r,fs);
	}
	
    /**
     * This method does the job of copying tuples from a variable to another.
     * @param source The source variable.
     * @param dest The destination variable.
     * @return
     */
    public void copyInfo(Register source,Register dest) {
    	sComp.copyTuples(source,dest);
    	cComp.copyTuples(source,dest);
    }
    
    public void copySinfo(Register source,Register dest) {
    	sComp.copyTuples(source,dest);
    }
    
    public void copyCinfo(Register source,Register dest) {
    	cComp.copyTuples(source,dest);
    }
    
    public void moveInfo(Register source,Register dest) {
    	sComp.moveTuples(source,dest);
    	cComp.moveTuples(source,dest);
    }
	
    public void moveSinfo(Register source,Register dest) {
    	sComp.moveTuples(source,dest);
    }

    public void moveCinfo(Register source,Register dest) {
    	sComp.moveTuples(source,dest);
    }

    public void moveInfoList(List<Register> source,List<Register> dest) {
    	sComp.moveTuplesList(source,dest);
    	cComp.moveTuplesList(source,dest);
    }
    
    public void copyFromCycle(Register source,Register dest) {
    	sComp.copyTuplesFromCycle(source,dest,cComp);
    }

    public void removeInfo(Register r) {
    	sComp.remove(r);
    	cComp.remove(r);
    }
    
	public void removeInfoList(List<Register> rs) {
		sComp.removeList(rs);
    	cComp.removeList(rs);
	}
        
	public void copyToGhostRegisters(Entry entry) {
		RegisterFactory rf = entry.getMethod().getCFG().getRegisterFactory();
		Utilities.begin("COPY TO GHOST REGISTERS - " + this + " - " + GlobalInfo.getGhostCopy(entry));
		for (int i=0; i<rf.size(); i++) {
			Register r = rf.get(i);
			// WARNING: once again, it would be better to find the way to obtain the
			// local variables of a method! (instead of the registerFactory which 
			// includes temporary and (now) ghost copies)
			if (!r.getType().isPrimitiveType()) {
				Register ghost = GlobalInfo.getGhostCopy(entry,r);
				if (ghost!=null) copyInfo(r,ghost);
			}
		}
		Utilities.end("COPY TO GHOST REGISTERS - " + this + " - " + GlobalInfo.getGhostCopy(entry));
	}

	public void cleanGhostRegisters(Entry entry) {
		Utilities.begin("CLEANING GHOST INFORMATION");
		RegisterFactory registerFactory = entry.getMethod().getCFG().getRegisterFactory();
		for (int i=0; i<registerFactory.size(); i++) {
			Register r = registerFactory.get(i);
			if (!r.getType().isPrimitiveType()) {
				Register rprime = GlobalInfo.getGhostCopy(entry,r);
				removeInfo(r);
				moveInfo(rprime,r);
			}
		}
		Utilities.end("CLEANING GHOST INFORMATION");
	}

	/**
	 * Removes from tuples all registers which are not actual parameters
	 * 
	 * @param actualParameters the list of actual parameters (not exactly a list of
	 * registers, but the Register object can be retrieved easily)
	 */
	public void filterActual(List<Register> actualParameters) {
		Utilities.begin("FILTERING: ONLY ACTUAL " + actualParameters + " KEPT");
		sComp.filterActual(actualParameters);
		cComp.filterActual(actualParameters);
		Utilities.info("NEW AV: " + this);
		Utilities.end("FILTERING: ONLY ACTUAL " + actualParameters + " KEPT");		
	}

	public List<Pair<FieldSet,FieldSet>> getSinfo(Register r1,Register r2) {
		return sComp.findTuplesByBothRegisters(r1, r2);
	}

	public List<Pair<Register,FieldSet>> getSinfoReachingRegister(Register r) {
		return sComp.findTuplesByReachingRegister(r);
	}

	public List<Pair<Register,FieldSet>> getSinfoReachedRegister(Register r) {
		return sComp.findTuplesByReachedRegister(r);
	}
	
	public List<FieldSet> getSinfoReachingReachedRegister(Register r1, Register r2) {
		return sComp.findTuplesByReachingReachedRegister(r1,r2);
	}

	public List<Trio<Register,FieldSet,FieldSet>> getSinfoFirstRegister(Register r) {
		return sComp.findTuplesByFirstRegister(r);
	}

	public List<Trio<Register,FieldSet,FieldSet>> getSinfoSecondRegister(Register r) {
		return sComp.findTuplesBySecondRegister(r);
	}

	public List<FieldSet> getCinfo(Register r) {
		return cComp.findTuplesByRegister(r);
	}
	
	public String toString() {
		return sComp.toString() + " / " + cComp.toString();
	}

	public boolean isBottom() {
		return sComp.isBottom() && cComp.isBottom();
	}

}
