package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.List;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

public class BothAbstractValue extends AbstractValue {

	private TuplesAbstractValue tuplesAV;
	public TuplesAbstractValue getTuplesPart() { return tuplesAV; }

	private BDDAbstractValue bddAV;
	public BDDAbstractValue getBDDPart() { return bddAV; }
	
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
			return tuplesAV.update(((BothAbstractValue) other).tuplesAV) | bddAV.update(((BothAbstractValue) other).bddAV);
		else 
			return false;
	}

	public BothAbstractValue clone() {
		return new BothAbstractValue((TuplesAbstractValue) tuplesAV.clone(),
				(bddAV==null)? null : (BDDAbstractValue) bddAV.clone());
	}

	public void addSinfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		tuplesAV.addSinfo(r1,r2,fs1,fs2); 
		bddAV.addSinfo(r1, r2, fs1, fs2);
	}

	public void addCinfo(Register r, FieldSet fs) {
		tuplesAV.addCinfo(r,fs);
		bddAV.addCinfo(r, fs);
	}

	public void copyInfo(Register source, Register dest) {
		tuplesAV.copyInfo(source,dest);
		bddAV.copyInfo(source,dest);
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
		return tuplesAV.toString() + " ++++ <-TUPLES / BDD-> ++++ " + bddAV.toString();
	}

	public boolean isBottom() {
		return tuplesAV.isBottom();
	}

	public BothAbstractValue propagateGetfield(Entry entry, Quad q, Register base,
			Register dest, jq_Field field) {
		return new BothAbstractValue(tuplesAV.propagateGetfield(entry,q,base,dest,field),
				bddAV.propagateGetfield(entry,q,base,dest,field));
	}

	public BothAbstractValue propagatePutfield(Entry entry, Quad q, Register base,
			Register dest, jq_Field field) {
		return new BothAbstractValue(tuplesAV.propagatePutfield(entry,q,base,dest,field),
				bddAV.propagatePutfield(entry,q,base,dest,field));
	}

	public BothAbstractValue propagateInvoke(Entry entry, Entry invokedEntry,
			Quad q, ArrayList<Register> actualParameters) {
		return new BothAbstractValue(tuplesAV.propagateInvoke(entry,invokedEntry,q,actualParameters),
				bddAV.propagateInvoke(entry,invokedEntry,q,actualParameters));
	}

	public List<Pair<FieldSet, FieldSet>> getStuples(Register r1,
			Register r2) {
		return tuplesAV.getStuples(r1, r2);
	}

	public List<FieldSet> getCtuples(Register r) {
		return tuplesAV.getCtuples(r);
	}
	
}
