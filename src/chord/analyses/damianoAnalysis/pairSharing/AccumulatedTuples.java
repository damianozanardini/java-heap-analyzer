package chord.analyses.damianoPairSharing;

import java.util.ArrayList;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;

public class AccumulatedTuples {
	
	ArrayList<Trio<Quad,Register,Register>> tuples;
	
	public AccumulatedTuples () {
		tuples = new ArrayList<Trio<Quad,Register,Register>>();
	}
		
	public Boolean condAdd(Quad q, Register r1, Register r2) {
		if (contains(q,r1,r2)) return false;
		else {
			Pair<Register,Register> p = RelPairSharing.order(r1,r2);
			tuples.add(new Trio<Quad,Register,Register>(q,p.val0,p.val1));
			return true;
		}
	}
	
	public boolean contains(Quad q, Register r1, Register r2) {
		for (Trio<Quad,Register,Register> t : tuples)
			if (t.val0 == q &&
			(t.val1 == r1 && t.val2 == r2) || (t.val1 == r2 && t.val2 == r1)) return true;
		return false;
	}
	
	public void remove(Quad q, Register r1, Register r2) {
		for (int i=0; i<tuples.size(); i++) {
			Trio<Quad,Register,Register> t = tuples.get(i);
			if (t.val0 == q &&
			(t.val1 == r1 && t.val2 == r2) || (t.val1 == r2 && t.val2 == r1)) {
				tuples.remove(i);
				return;
			}
		}
	}
	
	public void askForS(jq_Method m, Quad q, Register r1, Register r2) {
		String s1 = RegisterManager.getVarFromReg(m,r1);
		String s2 = RegisterManager.getVarFromReg(m,r2);
		boolean x = contains(q,r1,r2);
		Utilities.out("SHARING FROM " + s1 + " TO " + s2 + " AT " + q + " = " + x);
    }

}