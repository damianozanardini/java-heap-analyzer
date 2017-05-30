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
	
	// WARNING: obsolete
	// public AbstractValue(RelShare rs, RelCycle rc) {
	//	sComp = new STuples(rs.relTuples);	
	//}

	public boolean update(AbstractValue other) {
		return (sComp.join(other.getSComp()) | cComp.join(other.getCComp()));
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
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.length(); i++) {
			source.add(apl.get(i).getRegister());
			dest.add(RegisterManager.getRegFromNumber(m,i));
		}
		sComp.moveTuplesList(source,dest);
		cComp.moveTuplesList(source,dest);
	}
	
	/**
	 * In tuples, renames formal parameters into the corresponding actual parameters  
	 */
	public void formalToActual(ParamListOperand apl,jq_Method m) {
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.length(); i++) {
			dest.add(apl.get(i).getRegister());
			source.add(RegisterManager.getRegFromNumber(m,i));
		}
		sComp.moveTuplesList(source,dest);
		cComp.moveTuplesList(source,dest);
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
        
	public String toString() {
		return sComp.toString() + " / " + cComp.toString();
	}

	public void copyToGhostRegisters(RegisterFactory registerFactory) {
		for (int i=0; i<registerFactory.size(); i++) {
			Register r = registerFactory.get(i);
			if (!r.getType().isPrimitiveType())
				copyTuples(r,GlobalInfo.getGhostCopy(r));
		}
	}

	public void cleanGhostRegisters(RegisterFactory registerFactory) {
		for (int i=0; i<registerFactory.size(); i++) {
			Register r = registerFactory.get(i);
			if (!r.getType().isPrimitiveType()) {
				Register rprime = GlobalInfo.getGhostCopy(r);
				remove(r);
				moveTuples(rprime,r);
			}
		}
	}

}
