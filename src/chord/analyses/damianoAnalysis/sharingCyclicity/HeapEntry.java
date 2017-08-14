package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import chord.analyses.damianoAnalysis.DomRegister;
import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.ProgramPoint;
import chord.analyses.damianoAnalysis.QuadQueue;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.field.DomF;
import chord.analyses.method.DomM;
import chord.bddbddb.Rel.PentIterable;
import chord.bddbddb.Rel.TrioIterable;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.util.Timer;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import chord.util.tuple.object.Pent;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.PrintCFG;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * This class represents an entry/method processor in which the fix-point 
 * is applied to the instructions of the method body. 
 * 
 * @author Javier
 */
public class HeapEntry {
	/**
	 * The queue for implementing the fix-point.
	 */
	private QuadQueue queue;
	/**
	 * The entry to be analyzed
	 */
	private Entry entry;
	/**
	 * The method to be analyzed (actually, entry.getMethod())
	 */
	private jq_Method method;
	
	private ArrayList<Pair<Register,Register>> ghostVariables;
	
	protected TransferFunctionManager instructionProcessor;
	
	public HeapEntry(Entry e) {
		entry = e;
		method = entry.getMethod();
		instructionProcessor = new TransferFunctionManager(entry);
		queue = new QuadQueue(method,QuadQueue.FORWARD);
	}
		
	/**
	 * Executes the fix-point method to the method m
	 */
	public boolean run() {
		Utilities.mainBegin("ANALYSIS OF METHOD " + method + " (ENTRY: " + entry + ")");
		
		// number of iterations so far
		int i = 1;		
		// this variable is true iff there are changes in the abstract information during the inspection of the method code
		boolean needNextIteration;
		
		// the initial abstract information for the entry is taken from both the summary input 
		// and the previous information. It is copied to ghost registers every time
		ProgramPoint pp1 = GlobalInfo.getInitialPP(entry);

		Utilities.begin("LOADING SUMMARY INPUT");
		AbstractValue summaryInput = GlobalInfo.getSummaryManager().getSummaryInput(entry);
		Utilities.info("NEW SUMMARY INPUT: " + summaryInput + " OF TYPE: " + ((summaryInput == null) ? null : summaryInput.getClass()));
		AbstractValue av = GlobalInfo.getAV(pp1);
		Utilities.info("OLD AV AT INITIAL PP: " + av);
		av.updateInfo(summaryInput);
		Utilities.info("NEW AV (AFTER LOADING SUMMARY INPUT): " + GlobalInfo.getAV(pp1));
		Utilities.end("LOADING SUMMARY INPUT");
		av.copyToGhostRegisters(entry);
		
		// This is meant to propagate the abstract information from the VERY FIRST
		// program point (the one in the entry basic block) to the program point
		// before the first Quad (where the analysis actually begins); this could
		// be avoided if the entry (and exit) basic block were ignored when it comes
		// to fill the ProgramPoint domain
		Quad firstQuad = queue.getFirst();
		GlobalInfo.update(GlobalInfo.getPPBefore(entry,firstQuad),av.clone());
		
		// implementation of the fixpoint
		do {
			Utilities.mainBegin("ENTRY-LEVEL ITERATION #" + i);
			needNextIteration = false;
			for (Quad q : queue) {
				boolean b = instructionProcessor.transfer(q);
				needNextIteration |= b;
			}
			Utilities.mainEnd("ENTRY-LEVEL ITERATION #" + i + " - " + (needNextIteration? "NEED FOR ANOTHER ONE" : "NO NEED FOR ANOTHER ONE"));
			if (!needNextIteration)	GlobalInfo.showAVs(entry);
			i++;
		} while (needNextIteration && i<=7); // DEBUG: put a limit to the iterations

		ProgramPoint pp2 = GlobalInfo.getFinalPP(entry);
		AbstractValue av2 = GlobalInfo.getAV(pp2);
		
		Utilities.begin("UPDATE SUMMARY FOR ENTRY " + entry);
		Utilities.info("NEW OUTPUT INFO: " + av2);
		boolean b = GlobalInfo.getSummaryManager().updateSummaryOutput(entry, av2);
		Utilities.info("NEW SUMMARY FOR " + entry);
		Utilities.info("  INPUT:  " + GlobalInfo.getSummaryManager().getSummaryInput(entry));
		Utilities.info("  OUTPUT: " + GlobalInfo.getSummaryManager().getSummaryOutput(entry));
		Utilities.end("UPDATE SUMMARY FOR ENTRY " + entry);

		Utilities.mainEnd("ANALYSIS OF METHOD " + method + " (ENTRY: " + entry + ")");
		
		// "wakes up" callers to be re-analyzed
		if (b) {
			Utilities.begin("WAKING CALLERS UP");
			for (Entry caller : entry.getCallers()) GlobalInfo.wakeUp(caller);
			Utilities.end("WAKING CALLERS UP");
		}
		return b;
	}
	
}
