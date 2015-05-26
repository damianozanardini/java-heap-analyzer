package chord.analyses.damianoCyclicity;

import java.util.ArrayList;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.util.tuple.object.Quad;

public class AccumulatedTuples {
	
	ArrayList<Pair<Register,FSet>> cycle;
	ArrayList<Quad<Register,Register,FSet,FSet>> share;
	
	public AccumulatedTuples () {
		cycle = new ArrayList<Pair<Register,FSet>>();
		share = new ArrayList<Quad<Register,Register,FSet,FSet>>();
	}
		
	// cyclicity
	public boolean condAdd(Register r1, FSet fs) {
		if (contains(r1,fs)) return false;
		else {
			cycle.add(new Pair<Register,FSet>(r1,fs));
			return true;
		}
	}

	// sharing
	public Boolean condAdd(Register r1, Register r2, FSet fs1, FSet fs2) {
		if (contains(r1,r2,fs1,fs2)) return false;
		else {
			share.add(new Quad<Register,Register,FSet,FSet>(r1,r2,fs1,fs2));
			return true;
		}
	}
	
	// cyclicity
	public boolean contains(Register r1, FSet fs) {
		for (Pair<Register,FSet> p : cycle)
			if (p.val0 == r1 && p.val1 == fs) return true;
		return false;
	}
	
	// sharing
	private boolean contains(Register r1, Register r2, FSet fs1, FSet fs2) {
		for (Quad<Register,Register,FSet,FSet> q : share)
			if (q.val0 == r1 && q.val1 == r2 && q.val2 == fs1 && q.val3 == fs2) return true;
		return false;
	}
	
	public void askForC(jq_Method m, Register r) {
		String s = RegisterManager.getVarFromReg(m,r);
		Utilities.out("");
		if (s!=null) {
			Utilities.out("CYCLICITY OF " + s + " = ");
		} else {
			Utilities.out("CYCLICITY OF " + r + " = ");
		}
    	for (Pair<Register,FSet> p : cycle) {
    		if (p.val0 == r)
    			Utilities.out(p.val1.toString());
    	}
    }
	
	public void askForS(jq_Method m, Register r1, Register r2) {
		String s1 = RegisterManager.getVarFromReg(m,r1);
		String s2 = RegisterManager.getVarFromReg(m,r2);
		Utilities.out("");
		if (s1!=null && s2!=null) {
			Utilities.out("SHARING FROM " + s1 + " TO " + s2 + " = ");
		} else {
			Utilities.out("SHARING FROM " + r1 + " TO " + r2 + " = ");
		}
		for (Quad<Register,Register,FSet,FSet> q : share) {
    		if (q.val0 == r1 && q.val1 == r2)
    			Utilities.out(q.val2 + " - " + q.val3);
    	}
    }

}