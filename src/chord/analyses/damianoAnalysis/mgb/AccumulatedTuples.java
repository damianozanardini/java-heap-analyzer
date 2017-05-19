package chord.analyses.damianoAnalysis.mgb;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.project.Config;
import chord.util.tuple.object.*;

public class AccumulatedTuples {

	protected ArrayList<Trio<Entry,Register,FieldSet>> cycle;
	
	public ArrayList<Trio<Entry,Register,FieldSet>> getCycle(){ return this.cycle; }
	
	ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>> share;

	public ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>> getShare(){ return this.share; }
	
	public AccumulatedTuples () {
		cycle = new ArrayList<Trio<Entry,Register,FieldSet>>();
		share = new ArrayList<Pent<Entry,Register,Register,FieldSet,FieldSet>>();
	}

	// cyclicity
	public boolean condAdd(Entry e, Register r1, FieldSet fs) {
		if (contains(e,r1,fs)) return false;
		else {
			cycle.add(new Trio<Entry,Register,FieldSet>(e,r1,fs));
			return true;
		}
	}

	// sharing
	public Boolean condAdd(Entry e, Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		if (contains(e,r1,r2,fs1,fs2)) return false;
		else {
			share.add(new Pent<Entry,Register,Register,FieldSet,FieldSet>(e,r1,r2,fs1,fs2));
			return true;
		}
	}

	// cyclicity
	public boolean contains(Entry e, Register r1, FieldSet fs) {
		for (Trio<Entry,Register,FieldSet> p : cycle)
			if (p.val0 == e && p.val1 == r1 && p.val2 == fs) return true;
		return false;
	}

	// sharing
	private boolean contains(Entry e, Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		for (Pent<Entry,Register,Register,FieldSet,FieldSet> q : share)
			if (q.val0 == e && q.val1 == r1 && q.val2 == r2 && q.val3 == fs1 && q.val4 == fs2) return true;
		return false;
	}

	public void askForC(Entry e, Register r) {
		String s = RegisterManager.getVarFromReg(e.getMethod(),r);
		Utilities.out("");
		if (s!=null) {
			Utilities.out("CYCLICITY OF " + s + " = ");
		} else {
			Utilities.out("CYCLICITY OF " + r + " = ");
		}
		for (Trio<Entry,Register,FieldSet> p : cycle) {
			if (p.val0 == e && p.val1 == r)
				Utilities.out(p.val2.toString());
		}
	}
	
	public ArrayList<Pair<Register,FieldSet>> getCFor(Entry e, Register r){
		ArrayList<Pair<Register,FieldSet>> result = new ArrayList<>();
		for (Trio<Entry,Register,FieldSet> p : cycle) {
			if (p.val0 == e && p.val1 == r){
				result.add(new Pair<Register,FieldSet>(p.val1,p.val2));
				Utilities.out("\t (" + p.val1 + "," + p.val2 + ")");
			}
		}
		return result;
	}

	public void askForS(Entry e, Register r1, Register r2) {
		String s1 = RegisterManager.getVarFromReg(e.getMethod(),r1);
		String s2 = RegisterManager.getVarFromReg(e.getMethod(),r2);
		Utilities.out("");
		if (s1!=null && s2!=null) {
			Utilities.out("SHARING BETWEEN " + s1 + " AND " + s2 + " = ");
		} else {
			Utilities.out("SHARING BETWEEN " + r1 + " AND " + r2 + " = ");
		}
		for (Pent<Entry,Register,Register,FieldSet,FieldSet> q : share) {
			//Utilities.out("q.val0: " + q.val0 + ", entry:" + e);
			if (q.val0 == e && q.val1 == r1 && q.val2 == r2)
				Utilities.out(q.val3 + " - " + q.val4);
		}
	}
	
	public ArrayList<Quad<Register,Register,FieldSet,FieldSet>> getSFor(Entry e, Register r1, Register r2){
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> result = new ArrayList<>();
		
		for (Pent<Entry,Register,Register,FieldSet,FieldSet> q : share) {
			if (q.val0 == e && q.val1 == r1 && q.val2 == r2){
				Utilities.out("\t (" + q.val1 + "," + q.val2 + "," + q.val3 + "," + q.val4 + ")");
				result.add(new Quad<Register,Register,FieldSet,FieldSet>(q.val1,q.val2,q.val3,q.val4));
			}
		}
		return result;
	}
	
	public void print(){
		for (Pent<Entry,Register,Register,FieldSet,FieldSet> q : share) {
				Utilities.out("\t ("+q.val0+ "," + q.val1 + "," + q.val2 + "," + q.val3 + "," + q.val4 + ")");
		}
		for (Trio<Entry,Register,FieldSet> p : cycle) {
				Utilities.out("\t ("+p.val0+ ","+ p.val1 + "," + p.val2 + ")");
			}
	}

	public void askForSWeb(String fileName, Entry e, Register r1, Register r2) {
		PrintStream file = System.out;
		System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));

		String s1 = RegisterManager.getVarFromReg(e.getMethod(),r1);
		String s2 = RegisterManager.getVarFromReg(e.getMethod(),r2);
		System.out.println("<div class=result>SHARING BETWEEN " + s1 + " AND " + s2 + " = <ul>");
		for (Pent<Entry,Register,Register,FieldSet,FieldSet> q : share) {
			if (q.val0 == e && q.val1 == r1 && q.val2 == r2)
				System.out.println("<li> " + q.val3 + " / " + q.val4);
		}
		System.out.println("</ul></div>");
		System.setOut(file); // back to the usual log.txt
	}
}