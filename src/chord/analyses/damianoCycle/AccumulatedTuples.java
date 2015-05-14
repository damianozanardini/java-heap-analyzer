package chord.analyses.damianoCycle;

import java.util.ArrayList;

import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.util.tuple.object.Quad;

public class AccumulatedTuples {
	
	ArrayList<Pair<Register,Register>> share;
	ArrayList<Trio<Register,Register,FSet>> reach;
	ArrayList<Pair<Register,FSet>> cycle;
	ArrayList<Quad<Register,Register,FSet,FSet>> fshare;
	
	public AccumulatedTuples () {
		share = new ArrayList<Pair<Register,Register>>();
		reach = new ArrayList<Trio<Register,Register,FSet>>();
		cycle = new ArrayList<Pair<Register,FSet>>();
		fshare = new ArrayList<Quad<Register,Register,FSet,FSet>>();
	}
	
	// sharing
	public boolean condAdd(Register r1, Register r2) {
		if (contains(r1,r2)) return false;
		else {
			share.add(new Pair<Register,Register>(r1,r2));
			return true;
		}
	}
	
	// reachability
	public boolean condAdd(Register r1, Register r2, FSet fs) {
		if (contains(r1,r2,fs)) return false;
		else {
			reach.add(new Trio<Register,Register,FSet>(r1,r2,fs));
			return true;
		}
	}
	
	// cyclicity
	public boolean condAdd(Register r1, FSet fs) {
		if (contains(r1,fs)) return false;
		else {
			cycle.add(new Pair<Register,FSet>(r1,fs));
			return true;
		}
	}

	// fsharing
	public Boolean condAdd(Register r1, Register r2, FSet fs1, FSet fs2) {
		if (contains(r1,r2,fs1,fs2)) return false;
		else {
			fshare.add(new Quad<Register,Register,FSet,FSet>(r1,r2,fs1,fs2));
			return true;
		}
	}

	// sharing
	public boolean contains(Register r1, Register r2) {
		for (Pair<Register,Register> p : share)
			if (p.val0 == r1 && p.val1 == r2) return true;
		return false;
	}
	
	// reachability
	public boolean contains(Register r1, Register r2, FSet fs) {
		for (Trio<Register,Register,FSet> p : reach)
			if (p.val0 == r1 && p.val1 == r2 && p.val2 == fs) return true;
		return false;
	}
	
	// cyclicity
	public boolean contains(Register r1, FSet fs) {
		for (Pair<Register,FSet> p : cycle)
			if (p.val0 == r1 && p.val1 == fs) return true;
		return false;
	}
	
	// fsharing
	private boolean contains(Register r1, Register r2, FSet fs1, FSet fs2) {
		for (Quad<Register,Register,FSet,FSet> q : fshare)
			if (q.val0 == r1 && q.val1 == r2 && q.val2 == fs1 && q.val3 == fs2) return true;
		return false;
	}

	public void askForS(Register r1, Register r2) {
    	System.out.println("SHARING OF " + r1 + " WITH " + r2 + " = ");
    	for (Pair<Register,Register> p : share) {
    		if ((p.val0 == r1 && p.val1 == r2) || (p.val0 == r2 && p.val1 == r1)) {
    			System.out.println("yes");
    			return;
    		}
    	}
    	System.out.println("no");
    }
	
	public void askForR(Register r1, Register r2) {
    	System.out.println("REACHABILITY FROM " + r1 + " TO " + r2 + " = ");
    	for (Trio<Register,Register,FSet> t : reach) {
    		if (t.val0 == r1 && t.val1 == r2)
    			System.out.println(t.val2);
    	}
    }

	public void askForC(Register r) {
    	System.out.println("CYCLICITY OF " + r + " = ");
    	for (Pair<Register,FSet> p : cycle) {
    		if (p.val0 == r)
    			System.out.println(p.val1);
    	}
    }
	
	public void askForFS(Register r1, Register r2) {
    	System.out.println("F-SHARING FROM " + r1 + " TO " + r2 + " = ");
    	for (Quad<Register,Register,FSet,FSet> q : fshare) {
    		if (q.val0 == r1 && q.val1 == r2)
    			System.out.println(q.val2 + " - " + q.val3);
    	}
    }

}