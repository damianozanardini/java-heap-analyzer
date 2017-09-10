package chord.analyses.damianoAnalysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.PrintCFG;
import joeq.Compiler.Quad.RegisterFactory.Register;
import chord.project.ClassicProject;
import chord.util.Timer;

public class Utilities {
	
	/**
	 * Whether the analysis output is verbose or not.
	 */
	private static boolean verbose;

	/**
	 * Every time a message is output, this variable determines how much it is
	 * indented from the left.  It is manipulated by begin(), which shifts 3 characters
	 * to the right by calling shiftRight(), and end(), which shifts 3 characters
	 * to the left by calling shiftLeft().
	 */
	private static String indent = "";
	
	private static ArrayList<Timer> timerStack = new ArrayList<Timer>();
	
	/**
	 * Adds 3 empty spaces to indent.
	 */
	private static void shiftRight() { indent = indent + "   "; }
	
	/**
	 * Removes 3 empty spaces from indent.
	 */
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

	public static void mainBegin(String str) {
		Timer timer = new Timer();
		timer.init();
		timerStack.add(timer);
		out("[BGN] " + str);
		shiftRight();
	}
	
	public static void mainEnd(String str) {
		Timer timer = timerStack.remove(timerStack.size()-1);
		timer.done();
		shiftLeft();
		out("[END " + timer.getExclusiveTimeStr() + "] " + str);
	}
	
	public static void begin(String str) {
		Timer timer = new Timer();
		timer.init();
		timerStack.add(timer);
		debug("[BGN] " + str);
		shiftRight();
	}
	
	public static void end(String str) {
		Timer timer = timerStack.remove(timerStack.size()-1);
		timer.done();
		shiftLeft();
		debug("[END " + timer.getExclusiveTimeStr() + "] " + str);
	}

	public static void err(String str) { out("[ERR] " + str); }
	
	public static void warn(String str) { out("[WRN] " + str); }
	
	public static void answer(String str) { out("[OUT] " + str); }

	public static void step(String str) { debug("[STP] " + str); }

	public static void info(String str) { debug("[INF] " + str); }
	
	public static void wp() { out("UNTIL HERE"); }
	public static void wp(Object o) { out("UNTIL HERE - " + o); }
	
	/**
	 * Tells whether the analysis output is verbose or not.
	 *  
	 * @return
	 */
	public static boolean isVerbose() { return verbose; }

	/**
	 * Sets the verbosity level (true or false).
	 * 
	 * @param verbose
	 */
	public static void setVerbose(boolean verbose) { Utilities.verbose = verbose; }

	/**
	 * Print the Control Flow Graph of each method in the program to be analyzed.
	 */
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
	
	/**
	 * Order on registers goes like this: regular registers ("R") precede temporal
	 * registers ("T"), and smaller number precede bigger numbers. This is not exactly
	 * lexicographic order on the String representation because we want R9 to precede
	 * R10 (not the other way around, as in lexicographic).
	 * 
	 * @param r1
	 * @param r2
	 * @return
	 */
	public static boolean leqReg(Register r1,Register r2) {
		String s1 = r1.toString();
		String s2 = r2.toString();
		int n1 = Integer.parseInt(s1.substring(1));
		int n2 = Integer.parseInt(s2.substring(1));
		return (s1.substring(0,1).compareTo(s2.substring(0,1)) < 0 ||
				(s1.substring(0,1).compareTo(s2.substring(0,1)) == 0 &&
				Integer.compare(n1,n2)<=0));
	}

	/**
	 * Minimum on registers.
	 * 
	 * @param r1
	 * @param r2
	 * @return
	 */
	public static Register minReg(Register r1,Register r2) {
		return leqReg(r1,r2) ? r1 : r2;
	}

	/**
	 * Maximum on registers.
	 * @param r1
	 * @param r2
	 * @return
	 */
	public static Register maxReg(Register r1,Register r2) {
		return leqReg(r1,r2) ? r2 : r1;
	}
}
