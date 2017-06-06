package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import chord.analyses.damianoAnalysis.Entry;
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

public class AbstractValue {

	private STuples sComp;
	private CTuples cComp;
	
	public AbstractValue() {
		sComp = new STuples();
		cComp = new CTuples();
	}
	
	public AbstractValue(STuples st,CTuples ct) {
		sComp = st;
		cComp = ct;
	}
	
	public boolean update(AbstractValue other) {
		if (other == null) return false;
		else return (sComp.join(other.getSComp()) | cComp.join(other.getCComp()));
	}
	
	/**
	 * Returns the sharing component of the abstract value
	 * 
	 * @return
	 */	
	public STuples getSComp() {
		return sComp;
	}
	
	/**
	 * Returns the cyclicity component of the abstract value
	 * 
	 * @return
	 */
	public CTuples getCComp() {
		return cComp;
	}
		
	public void setSComp(STuples stuples){
		this.sComp = stuples;
	}
	
	public void setCComp(CTuples ctuples){
		this.cComp = ctuples;
	}

	/**
	 * Returns a new AbstractValue object with the same abstract information.
	 * The copy is neither completely deep nor completely shallow: for example,
	 * Register objects are not duplicated.
	 * 
	 * @return a copy of itself
	 */
	public AbstractValue clone() {
		return new AbstractValue(sComp.clone(),cComp.clone());
	}
	
	/**
	 * In tuples, renames actual parameters into the corresponding formal parameters  
	 */
	public void actualToFormal(ParamListOperand apl,jq_Method m) {
		Utilities.begin("ACTUAL " + apl + " TO FORMAL FROM " + this);
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.length(); i++) {
			try {
				dest.add(RegisterManager.getRegFromNumber(m,i));
				source.add(apl.get(i).getRegister());
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
	public void formalToActual(ParamListOperand apl,jq_Method m) {
		Utilities.begin("FORMAL FROM " + this + " TO ACTUAL "+ apl);
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.length(); i++) {
			try {
				source.add(RegisterManager.getRegFromNumber(m,i));
				dest.add(apl.get(i).getRegister());
			} catch (IndexOutOfBoundsException e) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		sComp.moveTuplesList(source,dest);
		cComp.moveTuplesList(source,dest);
		Utilities.end("FORMAL TO ACTUAL " + apl + " RESULTING IN " + this);
	}
	
    /**
     * This method does the job of copying tuples from a variable to another.
     * @param source The source variable.
     * @param dest The destination variable.
     * @return
     */
    public void copyTuples(Register source,Register dest) {
    	sComp.copyTuples(source,dest);
    	cComp.copyTuples(source,dest);
    }
    
    public void moveTuples(Register source,Register dest) {
    	sComp.moveTuples(source,dest);
    	cComp.moveTuples(source,dest);
    }
	
    public void moveTuplesList(List<Register> source,List<Register> dest) {
    	sComp.moveTuplesList(source,dest);
    	cComp.moveTuplesList(source,dest);
    }
    
    public void remove(Register r) {
    	sComp.remove(r);
    	cComp.remove(r);
    }
    
	public void removeList(List<Register> rs) {
		sComp.removeList(rs);
    	cComp.removeList(rs);
	}
        
	public void copyToGhostRegisters(Entry entry, RegisterFactory registerFactory) {
		Utilities.begin("COPY TO GHOST REGISTERS - " + this + " - " + GlobalInfo.getGhostCopy(entry));
		for (int i=0; i<registerFactory.size(); i++) {
			Register r = registerFactory.get(i);
			// WARNING: once again, it would be better to find the way to obtain the
			// local variables of a method! (instead of the registerFactory which 
			// includes temporary and (now) ghost copies)
			if (!r.getType().isPrimitiveType()) {
				Register ghost = GlobalInfo.getGhostCopy(entry,r);
				if (ghost!=null) copyTuples(r,ghost);
			}
		}
		Utilities.end("COPY TO GHOST REGISTERS - " + this + " - " + GlobalInfo.getGhostCopy(entry));
	}

	public void cleanGhostRegisters(Entry entry, RegisterFactory registerFactory) {
		Utilities.begin("CLEANING GHOST INFORMATION");
		for (int i=0; i<registerFactory.size(); i++) {
			Register r = registerFactory.get(i);
			if (!r.getType().isPrimitiveType()) {
				Register rprime = GlobalInfo.getGhostCopy(entry,r);
				remove(r);
				moveTuples(rprime,r);
			}
		}
		Utilities.end("CLEANING GHOST INFORMATION");
	}

	public void filterActual(ParamListOperand actualParameters) {
		Utilities.begin("FILTERING: ONLY ACTUAL " + actualParameters + " KEPT");
		sComp.filterActual(actualParameters);
		cComp.filterActual(actualParameters);
		Utilities.info("NEW AV: " + this);
		Utilities.end("FILTERING: ONLY ACTUAL " + actualParameters + " KEPT");		
	}

	public String toString() {
		return sComp.toString() + " / " + cComp.toString();
	}

	public boolean isBottom() {
		return sComp.isBottom() && cComp.isBottom();
	}

}
