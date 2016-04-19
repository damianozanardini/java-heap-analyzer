package chord.analyses.jgbHeap;

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

	ArrayList<Pair<Register,FieldSet>> cycle;
	ArrayList<Quad<Register,Register,FieldSet,FieldSet>> share;

	public AccumulatedTuples () {
		cycle = new ArrayList<Pair<Register,FieldSet>>();
		share = new ArrayList<Quad<Register,Register,FieldSet,FieldSet>>();
	}

	// cyclicity
	public boolean condAdd(Register r1, FieldSet fs) {
		if (contains(r1,fs)) return false;
		else {
			cycle.add(new Pair<Register,FieldSet>(r1,fs));
			return true;
		}
	}

	// sharing
	public Boolean condAdd(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		if (contains(r1,r2,fs1,fs2)) return false;
		else {
			share.add(new Quad<Register,Register,FieldSet,FieldSet>(r1,r2,fs1,fs2));
			return true;
		}
	}

	// cyclicity
	public boolean contains(Register r1, FieldSet fs) {
		for (Pair<Register,FieldSet> p : cycle)
			if (p.val0 == r1 && p.val1 == fs) return true;
		return false;
	}

	// sharing
	private boolean contains(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		for (Quad<Register,Register,FieldSet,FieldSet> q : share)
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
		for (Pair<Register,FieldSet> p : cycle) {
			if (p.val0 == r)
				Utilities.out(p.val1.toString());
		}
		/*for (Trio<jq_Method,Register,FieldSet> p : cyclePrime) {
			if (p.val0 == m && p.val1 == r)
				Utilities.out(p.val1.toString());
		}*/
	}

	public void askForS(jq_Method m, Register r1, Register r2) {
		String s1 = RegisterManager.getVarFromReg(m,r1);
		String s2 = RegisterManager.getVarFromReg(m,r2);
		Utilities.out("");
		if (s1!=null && s2!=null) {
			Utilities.out("SHARING BETWEEN " + s1 + " AND " + s2 + " = ");
		} else {
			Utilities.out("SHARING BETWEEN " + r1 + " AND " + r2 + " = ");
		}
		for (Quad<Register,Register,FieldSet,FieldSet> q : share) {
			if (q.val0 == r1 && q.val1 == r2)
				Utilities.out(q.val2 + " - " + q.val3);
		}
		/*for (Pent<jq_Method,Register,Register,FieldSet,FieldSet> q : sharePrime) {
			if (q.val0 == m && q.val1 == r1 && q.val2 == r2)
				Utilities.out(q.val2 + " - " + q.val3);
		}*/
	}

	public void askForSWeb(String fileName, jq_Method m, Register r1, Register r2) {
		PrintStream file = System.out;
		System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));

		String s1 = RegisterManager.getVarFromReg(m,r1);
		String s2 = RegisterManager.getVarFromReg(m,r2);
		System.out.println("<div class=result>SHARING BETWEEN " + s1 + " AND " + s2 + " = <ul>");
		for (Quad<Register,Register,FieldSet,FieldSet> q : share) {
			if (q.val0 == r1 && q.val1 == r2)
				System.out.println("<li> " + q.val2 + " / " + q.val3);
		}
		System.out.println("</ul></div>");
		System.setOut(file); // back to the usual log.txt
	}
}