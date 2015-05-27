package chord.analyses.damianoPairSharing;

import java.util.ArrayList;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.util.tuple.object.Quad;

public class AccumulatedTuples {
	
	ArrayList<Pair<Register,Register>> share;
	
	public AccumulatedTuples () {
		share = new ArrayList<Pair<Register,Register>>();
	}
		
	// sharing
	public Boolean condAdd(Register r1, Register r2) {
		if (contains(r1,r2)) return false;
		else {
			share.add(RelPairShare.order(r1,r2));
			return true;
		}
	}
	
	// sharing
	private boolean contains(Register r1, Register r2) {
		for (Pair<Register,Register> p : share)
			if ((p.val0 == r1 && p.val1 == r2) || (p.val0 == r2 && p.val1 == r1)) return true;
		return false;
	}
	
	public void askForS(jq_Method m, Register r1, Register r2) {
		String s1 = RegisterManager.getVarFromReg(m,r1);
		String s2 = RegisterManager.getVarFromReg(m,r2);
		boolean x = contains(r1,r2);
		Utilities.out("");
		if (s1!=null && s2!=null) {
			Utilities.out("SHARING FROM " + s1 + " TO " + s2 + " = " + x);
		} else {
			Utilities.out("SHARING FROM " + r1 + " TO " + r2 + " = " + x);
		}
    }

}