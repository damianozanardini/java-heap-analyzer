package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.List;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

public class BothAbstractValue extends AbstractValue {

	private TuplesAbstractValue tuplesAV;
	public TuplesAbstractValue getTuplesPart() { return tuplesAV; }

	private BDDAbstractValue bddAV;
	public BDDAbstractValue getBDDPart() { return bddAV; }
		
	public BothAbstractValue(Entry entry) {
		tuplesAV = new TuplesAbstractValue();
		bddAV = new BDDAbstractValue(entry);
	}

	/**
	 * This constructor is private because (1) it is never called from the outside;
	 * (2) in general, a BothAbstractValue object is guaranteed to have both 
	 * components non-null
	 * @param t
	 * @param b
	 */
	private BothAbstractValue(TuplesAbstractValue t, BDDAbstractValue b) {
		tuplesAV = t;
		bddAV = b;
	}

	public boolean update(AbstractValue other) {
		boolean b = false;
		if (other instanceof TuplesAbstractValue) {
			b |= tuplesAV.update(((TuplesAbstractValue) other));			
		} else if (other instanceof BDDAbstractValue) {
			b |= bddAV.update(((BDDAbstractValue) other));			
		} else if (other instanceof BothAbstractValue) {
			if (tuplesAV != null) {
				b |= tuplesAV.update(((BothAbstractValue) other).tuplesAV);
			} else tuplesAV = ((BothAbstractValue) other).tuplesAV;
			if (bddAV != null) {
				b |= bddAV.update(((BothAbstractValue) other).bddAV);
			} else bddAV = ((BothAbstractValue) other).bddAV;
		}
		return b;
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
		bddAV.copySinfo(source,dest);
	}

	public void copyCinfo(Register source, Register dest) {
		tuplesAV.copyCinfo(source,dest);
		bddAV.copyCinfo(source,dest);
	}

	public void moveInfo(Register source, Register dest) {
		tuplesAV.moveInfo(source,dest);
		bddAV.moveInfo(source,dest);
	}

	public void moveSinfo(Register source, Register dest) {
		tuplesAV.moveSinfo(source,dest);
		bddAV.moveSinfo(source,dest);
	}

	public void moveCinfo(Register source, Register dest) {
		tuplesAV.moveCinfo(source,dest);
		bddAV.moveCinfo(source,dest);
	}

	public void moveInfoList(List<Register> source, List<Register> dest) {
		tuplesAV.moveInfoList(source,dest);
		bddAV.moveInfoList(source,dest);
	}

	public void copyFromCycle(Register base, Register dest) {
		tuplesAV.copyFromCycle(base,dest);
	}

	public void removeInfo(Register r) {
		tuplesAV.removeInfo(r);
		bddAV.removeInfo(r);
	}

	public void removeInfoList(List<Register> rs) {
		tuplesAV.removeInfoList(rs);
		bddAV.removeInfoList(rs);
	}

	public void actualToFormal(List<Register> apl, Entry e) {
		tuplesAV.actualToFormal(apl,e);
		bddAV.actualToFormal(apl,e);
	}

	public void formalToActual(List<Register> apl, Entry e) {
		tuplesAV.formalToActual(apl, e);
		bddAV.formalToActual(apl, e);
	}

	public void copyToGhostRegisters(Entry entry) {
		tuplesAV.copyToGhostRegisters(entry);
		bddAV.copyToGhostRegisters(entry);
	}

	public void cleanGhostRegisters(Entry entry) {
		tuplesAV.cleanGhostRegisters(entry);
		bddAV.cleanGhostRegisters(entry);
	}

	public void filterActual(List<Register> actualParameters) {
		tuplesAV.filterActual(actualParameters);
		bddAV.filterActual(actualParameters);
	}

	public String toString() {
		String sBDD = (bddAV != null) ? bddAV.toString() : "" ;
		return tuplesAV.toString() + " <-TUPLES / BDD-> " + sBDD; 
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

	public ArrayList<Pair<FieldSet, FieldSet>> getStuples(Register r1,
			Register r2) {
		ArrayList<Pair<FieldSet, FieldSet>> tList = tuplesAV.getStuples(r1, r2);
		ArrayList<Pair<FieldSet, FieldSet>> bList = bddAV.getStuples(r1, r2);
		if (!tList.equals(bList))
			Utilities.warn("DIFFERENT LISTS FOR BOTH IMPLEMENTATIONS: " + tList + " / " + bList);
		return tList;
	}

	public ArrayList<FieldSet> getCtuples(Register r) {
		ArrayList<FieldSet> tList = tuplesAV.getCtuples(r);
		ArrayList<FieldSet> bList = bddAV.getCtuples(r);
		if (!tList.equals(bList))
			Utilities.warn("DIFFERENT LISTS FOR BOTH IMPLEMENTATIONS: " + tList + " / " + bList);
		return tList;
	}
	
}
