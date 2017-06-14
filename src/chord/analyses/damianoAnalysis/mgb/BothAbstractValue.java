package chord.analyses.damianoAnalysis.mgb;

import java.util.List;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

public class BothAbstractValue extends AbstractValue {

	private TuplesAbstractValue tuplesAV;
	private BDDAbstractValue bddAV;
	
	public BothAbstractValue(TuplesAbstractValue t, BDDAbstractValue b) {
		tuplesAV = t;
		bddAV = b;
	}
	
	public BothAbstractValue(Entry entry) {
		tuplesAV = new TuplesAbstractValue();
		bddAV = new BDDAbstractValue(entry);
	}

	public boolean update(AbstractValue other) {
		if (other instanceof BothAbstractValue)
			return tuplesAV.update(((BothAbstractValue) other).tuplesAV);
		else 
			return false;
	}

	public AbstractValue clone() {
		return new BothAbstractValue((TuplesAbstractValue) tuplesAV.clone(),
				(bddAV==null)? null : (BDDAbstractValue) bddAV.clone());
	}

	public void addSinfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		tuplesAV.addSinfo(r1,r2,fs1,fs2);
	}

	public void addCinfo(Register r, FieldSet fs) {
		tuplesAV.addCinfo(r,fs);
	}

	public void copyInfo(Register source, Register dest) {
		tuplesAV.copyInfo(source,dest);
	}

	public void copySinfo(Register source, Register dest) {
		tuplesAV.copySinfo(source,dest);
	}

	@Override
	public void copyCinfo(Register source, Register dest) {
		tuplesAV.copyCinfo(source,dest);
	}

	public void moveInfo(Register source, Register dest) {
		tuplesAV.moveInfo(source,dest);
	}

	public void moveSinfo(Register source, Register dest) {
		tuplesAV.moveSinfo(source,dest);
	}

	public void moveCinfo(Register source, Register dest) {
		tuplesAV.moveCinfo(source,dest);
	}

	public void moveInfoList(List<Register> source, List<Register> dest) {
		tuplesAV.moveInfoList(source,dest);
	}

	public void copyFromCycle(Register base, Register dest) {
		tuplesAV.copyFromCycle(base,dest);
	}

	public void removeInfo(Register r) {
		tuplesAV.removeInfo(r);
	}

	public void removeInfoList(List<Register> rs) {
		tuplesAV.removeInfoList(rs);
	}

	public void actualToFormal(List<Register> apl, jq_Method m) {
		tuplesAV.actualToFormal(apl,m);
	}

	public void formalToActual(List<Register> apl, jq_Method m) {
		tuplesAV.formalToActual(apl, m);
	}

	public void copyToGhostRegisters(Entry entry) {
		tuplesAV.copyToGhostRegisters(entry);
	}

	public void cleanGhostRegisters(Entry entry) {
		tuplesAV.cleanGhostRegisters(entry);
	}

	public void filterActual(List<Register> actualParameters) {
		tuplesAV.filterActual(actualParameters);
	}

	public List<Pair<FieldSet, FieldSet>> getSinfo(Register r1, Register r2) {
		return tuplesAV.getSinfo(r1,r2);
	}

	public List<Pair<Register, FieldSet>> getSinfoReachingRegister(Register r) {
		return tuplesAV.getSinfoReachingRegister(r);
	}

	public List<Pair<Register, FieldSet>> getSinfoReachedRegister(Register r) {
		return tuplesAV.getSinfoReachedRegister(r);
	}

	public List<FieldSet> getSinfoReachingReachedRegister(Register r1,Register r2) {
		return tuplesAV.getSinfoReachingReachedRegister(r1,r2);
	}

	public List<Trio<Register, FieldSet, FieldSet>> getSinfoFirstRegister(Register r) {
		return tuplesAV.getSinfoFirstRegister(r);
	}

	public List<Trio<Register, FieldSet, FieldSet>> getSinfoSecondRegister(Register r) {
		return tuplesAV.getSinfoSecondRegister(r);
	}

	public List<FieldSet> getCinfo(Register r) {
		return tuplesAV.getCinfo(r);
	}

	public String toString() {
		return tuplesAV.toString();
	}

	public boolean isBottom() {
		return tuplesAV.isBottom();
	}

}
