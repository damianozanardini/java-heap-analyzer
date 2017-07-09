package chord.analyses.damianoAnalysis;

import java.util.ArrayList;
import java.util.Iterator;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.PrintCFG;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.ClassicProject;

public class Utilities {
	
	private static boolean verbose;
	private static String indent = "";
	
	private static void shiftRight() { indent = indent + "   "; }
	private static void shiftLeft() { indent = indent.substring(0, indent.length()-3); }
	
	public static void out(String str) { System.out.println(indent + str); }

	public static void out0(String str) { System.out.print(indent + str); }

	public static void debug(String str) {
		if (isVerbose()) {
			System.out.println(indent + str);
		}
	}
	// TODO Miguel. just a quick way to find my debugging messages, delete.
	public static void debugMGB(String str) {
		if (isVerbose()) {
			System.out.println(indent + "[MGB] " + str);
		}
	}
	
	public static void debug0(String str) {
		if (isVerbose()) {
			System.out.print(str);
		}
	}

	public static void begin(String str) {
		debug("[BGN] " + str);
		shiftRight();
	}
	
	public static void end(String str) {
		shiftLeft();
		debug("[END] " + str);	}
	
	public static void err(String str) { out("[ERR] " + str); }
	
	public static void warn(String str) { out("[WRN] " + str); }
	
	public static void step(String str) { debug("[STP] " + str); }

	public static void info(String str) { debug("[INF] " + str); }

	public static void wp() { out("UNTIL HERE"); }
	public static void wp(Object o) { out("UNTIL HERE - " + o); }
	
	public static boolean isVerbose() { return verbose; }

	public static void setVerbose(boolean verbose) { Utilities.verbose = verbose; }

	public static void printCFGs() {
		ArrayList<jq_Method> alreadyPrinted = new ArrayList<jq_Method>();
		ControlFlowGraph cfg;
		DomEntry dome = (DomEntry) ClassicProject.g().getTrgt("Entry");
		Iterator<Entry> it = dome.iterator();
		while(it.hasNext()){
			Entry e = it.next();
			jq_Method m = e.getMethod();
			if (!alreadyPrinted.contains(m)) {
				begin("PRINT CFG");
				cfg = CodeCache.getCode(m);
				new PrintCFG().visitCFG(cfg);
				alreadyPrinted.add(m);
				end("PRINT CFG");
			}
		}	
	}
	
	public static boolean leqReg(Register r1,Register r2) {
		return (r1.toString().compareTo(r2.toString()) <= 0);
	}

	public static Register minReg(Register r1,Register r2) {
		return leqReg(r1,r2) ? r1 : r2;
	}

	public static Register maxReg(Register r1,Register r2) {
		return leqReg(r1,r2) ? r2 : r1;
	}
}
