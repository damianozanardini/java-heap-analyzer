package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;

import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Pent;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;

public class AbstractValue {

	private STuples sComp;
	private CTuples cComp;
	
	public AbstractValue() {
		sComp = new STuples();
		cComp = new CTuples();
	}
	
	public AbstractValue(RelShare rs, RelCycle rc) {
		sComp = new STuples(rs.relTuples);
	
	}

	public boolean update(AbstractValue other) {
		boolean s = sComp.join(other.getSComp());
		boolean c = cComp.join(other.getCComp());
			return ( c || s);
	}
	
	/**
	 * Returns the sharing component of the abstract value
	 * 
	 * TO-DO
	 * @return
	 */	
	public STuples getSComp() {
		return sComp;
	}
	
	/**
	 * Returns the cyclicity component of the abstract value
	 * 
	 * TO-DO
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

	// returns a NEW abstract value, without modifying the existing one.
	// I do this because I'm not sure about what doing otherwise would imply
	// WARNING: it is somehow a shallow copy
	// 
	// apl is the list of actual parameters, which is the destination here
	public AbstractValue getRenamedCopyListIn(ParamListOperand apl,jq_Method m) {
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.length(); i++) {
			source.add(apl.get(i).getRegister());
			dest.add(RegisterManager.getRegFromNumber(m,i));
		}		
		
		System.out.println("ZZZZZZZ - " + source + " -> " + dest);
		
		AbstractValue av = new AbstractValue();
		
		STuples st = new STuples();
		st.setTuples((ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>>) sComp.getTuples().clone());
		st.moveTuplesList(source, dest);
		
		CTuples ct = new CTuples();
		ct.setTuples((ArrayList<Trio<Entry,Register,FieldSet>>) cComp.getTuples().clone());
		ct.moveTuplesList(source, dest);		
		
		av.setSComp(st);
		av.setCComp(ct);
		return av;
	}

	// returns a NEW abstract value, without modifying the existing one.
	// I do this because I'm not sure about what doing otherwise would imply
	// WARNING: it is somehow a shallow copy
	// 
	// apl is the list of actual parameters, which is the destination here
	public AbstractValue getRenamedCopyListOut(ParamListOperand apl,jq_Method m) {
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.length(); i++) {
			dest.add(apl.get(i).getRegister());
			source.add(RegisterManager.getRegFromNumber(m,i));
		}		
		
		System.out.println("ZZZZZZZ - " + source + " -> " + dest);
		
		AbstractValue av = new AbstractValue();
		
		STuples st = new STuples();
		st.setTuples((ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>>) sComp.getTuples().clone());
		st.moveTuplesList(source, dest);
		
		CTuples ct = new CTuples();
		ct.setTuples((ArrayList<Trio<Entry,Register,FieldSet>>) cComp.getTuples().clone());
		ct.moveTuplesList(source, dest);		
		
		av.setSComp(st);
		av.setCComp(ct);
		return av;
	}
	
	public String toString() {
		return sComp.toString() + " / " + cComp.toString();
	}
}
