package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

public class BothAbstractValue extends AbstractValue {

	private TuplesAbstractValue tuplesAV;
	public TuplesAbstractValue getTuplesPart() { return tuplesAV; }

	private BDDAbstractValue bddAV;
	public BDDAbstractValue getBDDPart() { return bddAV; }
		
	public BothAbstractValue(Entry entry) {
		tuplesAV = new TuplesAbstractValue(entry);
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

	public boolean updateSInfo(AbstractValue other) {
		boolean b = false;
		if (other instanceof TuplesAbstractValue) {
			b |= tuplesAV.updateSInfo(((TuplesAbstractValue) other));			
		} else if (other instanceof BDDAbstractValue) {
			b |= bddAV.updateSInfo(((BDDAbstractValue) other));			
		} else if (other instanceof BothAbstractValue) {
			if (tuplesAV != null) {
				b |= tuplesAV.updateSInfo(((BothAbstractValue) other).tuplesAV);
			} else tuplesAV = ((BothAbstractValue) other).tuplesAV;
			if (bddAV != null) {
				b |= bddAV.updateSInfo(((BothAbstractValue) other).bddAV);
			} else bddAV = ((BothAbstractValue) other).bddAV;
		}
		return b;
	}
	
	public boolean updateCInfo(AbstractValue other) {
		boolean b = false;
		if (other instanceof TuplesAbstractValue) {
			b |= tuplesAV.updateCInfo(((TuplesAbstractValue) other));			
		} else if (other instanceof BDDAbstractValue) {
			b |= bddAV.updateCInfo(((BDDAbstractValue) other));			
		} else if (other instanceof BothAbstractValue) {
			if (tuplesAV != null) {
				b |= tuplesAV.updateCInfo(((BothAbstractValue) other).tuplesAV);
			} else tuplesAV = ((BothAbstractValue) other).tuplesAV;
			if (bddAV != null) {
				b |= bddAV.updateCInfo(((BothAbstractValue) other).bddAV);
			} else bddAV = ((BothAbstractValue) other).bddAV;
		}
		return b;
	}
	
	public boolean updateAInfo(AbstractValue other) {
		boolean b = false;
		if (other instanceof TuplesAbstractValue) {
			b |= tuplesAV.updateAInfo(((TuplesAbstractValue) other));			
		} else if (other instanceof BDDAbstractValue) {
			b |= bddAV.updateAInfo(((BDDAbstractValue) other));			
		} else if (other instanceof BothAbstractValue) {
			if (tuplesAV != null) {
				b |= tuplesAV.updateAInfo(((BothAbstractValue) other).tuplesAV);
			} else tuplesAV = ((BothAbstractValue) other).tuplesAV;
			if (bddAV != null) {
				b |= bddAV.updateAInfo(((BothAbstractValue) other).bddAV);
			} else bddAV = ((BothAbstractValue) other).bddAV;
		}
		return b;
	}
	
	public boolean updatePInfo(AbstractValue other) {
		boolean b = false;
		if (other instanceof TuplesAbstractValue) {
			b |= tuplesAV.updatePInfo(((TuplesAbstractValue) other));			
		} else if (other instanceof BDDAbstractValue) {
			b |= bddAV.updatePInfo(((BDDAbstractValue) other));			
		} else if (other instanceof BothAbstractValue) {
			if (tuplesAV != null) {
				b |= tuplesAV.updatePInfo(((BothAbstractValue) other).tuplesAV);
			} else tuplesAV = ((BothAbstractValue) other).tuplesAV;
			if (bddAV != null) {
				b |= bddAV.updatePInfo(((BothAbstractValue) other).bddAV);
			} else bddAV = ((BothAbstractValue) other).bddAV;
		}
		return b;
	}
		
	public BothAbstractValue clone() {
		return new BothAbstractValue((TuplesAbstractValue) tuplesAV.clone(),
				(bddAV==null)? null : (BDDAbstractValue) bddAV.clone());
	}

	public void addSInfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		tuplesAV.addSInfo(r1,r2,fs1,fs2); 
		bddAV.addSInfo(r1,r2,fs1,fs2);
	}

	public void addCInfo(Register r, FieldSet fs) {
		tuplesAV.addCInfo(r,fs);
		bddAV.addCInfo(r,fs);
	}

	public void addAInfo(Register r1, Register r2) {
		tuplesAV.addAInfo(r1,r2); 
		bddAV.addAInfo(r1,r2);
	}

	public void addPInfo(Register r) {
		tuplesAV.addPInfo(r);
		bddAV.addPInfo(r);
	}

	public void copyInfo(Register source, Register dest) {
		tuplesAV.copyInfo(source,dest);
		bddAV.copyInfo(source,dest);
	}

	public void copySInfo(Register source, Register dest) {
		tuplesAV.copySInfo(source,dest);
		bddAV.copySInfo(source,dest);
	}

	public void copyCInfo(Register source, Register dest) {
		tuplesAV.copyCInfo(source,dest);
		bddAV.copyCInfo(source,dest);
	}

	public void copyAInfo(Register source, Register dest) {
		tuplesAV.copyAInfo(source,dest);
		bddAV.copyAInfo(source,dest);
	}

	public void copyPInfo(Register source, Register dest) {
		tuplesAV.copyPInfo(source,dest);
		bddAV.copyPInfo(source,dest);
	}

	public void moveInfo(Register source, Register dest) {
		tuplesAV.moveInfo(source,dest);
		bddAV.moveInfo(source,dest);
	}

	public void moveSInfo(Register source, Register dest) {
		tuplesAV.moveSInfo(source,dest);
		bddAV.moveSInfo(source,dest);
	}

	public void moveCInfo(Register source, Register dest) {
		tuplesAV.moveCInfo(source,dest);
		bddAV.moveCInfo(source,dest);
	}

	public void moveAInfo(Register source, Register dest) {
		tuplesAV.moveAInfo(source,dest);
		bddAV.moveAInfo(source,dest);
	}

	public void movePInfo(Register source, Register dest) {
		tuplesAV.movePInfo(source,dest);
		bddAV.movePInfo(source,dest);
	}

	protected void copySInfoFromC(Register base, Register dest) {
		tuplesAV.copySInfoFromC(base,dest);
		bddAV.copySInfoFromC(base,dest);
	}

	public void removeInfo(Register r) {
		tuplesAV.removeInfo(r);
		bddAV.removeInfo(r);
	}

	public void removeSInfo(Register r) {
		tuplesAV.removeSInfo(r);
		bddAV.removeSInfo(r);
	}

	public void removeCInfo(Register r) {
		tuplesAV.removeCInfo(r);
		bddAV.removeCInfo(r);
	}

	public void removeAInfo(Register r) {
		tuplesAV.removeAInfo(r);
		bddAV.removeAInfo(r);
	}

	public void removePInfo(Register r) {
		tuplesAV.removePInfo(r);
		bddAV.removePInfo(r);
	}

	public void filterActual(Entry entry,List<Register> actualParameters) {
		tuplesAV.filterActual(entry, actualParameters);
		bddAV.filterActual(entry, actualParameters);
	}
	
	public BothAbstractValue doGetfield(Entry entry, Quad q, Register base,
			Register dest, jq_Field field) {
		return new BothAbstractValue(tuplesAV.doGetfield(entry,q,base,dest,field),
				bddAV.doGetfield(entry,q,base,dest,field));
	}

	public BothAbstractValue doPutfield(Entry entry, Quad q, Register base,
			Register dest, jq_Field field) {
		return new BothAbstractValue(tuplesAV.doPutfield(entry,q,base,dest,field),
				bddAV.doPutfield(entry,q,base,dest,field));
	}

	public BothAbstractValue doInvoke(Entry entry, Entry invokedEntry,
			Quad q, ArrayList<Register> actualParameters, Register returnValue) {
		return new BothAbstractValue(tuplesAV.doInvoke(entry,invokedEntry,q,actualParameters,returnValue),
				bddAV.doInvoke(entry,invokedEntry,q,actualParameters,returnValue));
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

	public boolean equals(AbstractValue av) {
		if (av instanceof BothAbstractValue)
			return tuplesAV.equals(((BothAbstractValue) av).getTuplesPart()) && bddAV.equals(((BothAbstractValue) av).getBDDPart());
		else return false;
	}	
	
	public String toString() {
		String sBDD = (bddAV != null) ? bddAV.toString() : "" ;
		return tuplesAV.toString() + " <-TUPLES / BDD-> " + sBDD; 
	}
	
}
