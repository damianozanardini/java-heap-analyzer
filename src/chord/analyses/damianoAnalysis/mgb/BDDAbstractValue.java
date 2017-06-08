package chord.analyses.damianoAnalysis.mgb;

import java.util.List;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.Entry;
import chord.util.tuple.object.Pair;

public class BDDAbstractValue extends AbstractValue {

	@Override
	public boolean update(AbstractValue other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AbstractValue clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addSinfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addCinfo(Register r, FieldSet fs) {
		// TODO Auto-generated method stub

	}

	@Override
	public void copyInfo(Register source, Register dest) {
		// TODO Auto-generated method stub

	}

	@Override
	public void moveInfo(Register source, Register dest) {
		// TODO Auto-generated method stub

	}

	@Override
	public void moveInfoList(List<Register> source, List<Register> dest) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeInfo(Register r) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeInfoList(List<Register> rs) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actualToFormal(List<Register> apl, jq_Method m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void formalToActual(List<Register> apl, jq_Method m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void copyToGhostRegisters(Entry entry,
			RegisterFactory registerFactory) {
		// TODO Auto-generated method stub

	}

	@Override
	public void cleanGhostRegisters(Entry entry, RegisterFactory registerFactory) {
		// TODO Auto-generated method stub

	}

	@Override
	public void filterActual(List<Register> actualParameters) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Pair<FieldSet, FieldSet>> getSinfo(Register r1, Register r2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FieldSet> getCinfo(Register r) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isBottom() {
		// TODO Auto-generated method stub
		return false;
	}

}
