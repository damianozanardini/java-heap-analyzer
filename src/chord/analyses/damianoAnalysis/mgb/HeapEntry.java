package chord.analyses.damianoAnalysis.mgb;

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
	/**
	 * The instruction processor which takes care of bytecode instructions one by one
	 */
	private HeapProgram program;
	
	private ArrayList<Pair<Register,Register>> ghostVariables;
	
	protected InstructionProcessor instructionProcessor;
	
	public HeapEntry(Entry e, HeapProgram p) {
		entry = e;
		method = entry.getMethod();
		program = p;
		instructionProcessor = new InstructionProcessor(entry);
		queue = new QuadQueue(method,QuadQueue.FORWARD);
	}
		
	/**
	 * Execute the fix-point method to the method m
	 */
	public boolean run(){
		Utilities.begin("ANALYSIS OF METHOD " + method);

		// number of iterations so far
		int i = 1;		
		// this variable is true iff there are changes in the abstract information during the inspection of the method code
		boolean needNextIteration;
		
		// the initial abstract information for the entry is taken from both the summary input 
		// and the previous information. It is copied to ghost registers every time
		ProgramPoint pp1 = GlobalInfo.getInitialPP(entry);
		GlobalInfo.getAV(pp1).update(GlobalInfo.summaryManager.getSummaryInput(entry));		
		GlobalInfo.getAV(pp1).copyToGhostRegisters(method.getCFG().getRegisterFactory());

		// this variable is true iff there are changes AT ALL (in any iteration)
		boolean somethingChanged = false;
		// implementation of the fixpoint
		do {
			Utilities.begin("ENTRY-LEVEL ITERATION #" + i);
			needNextIteration = false;
			for (Quad q : queue) {
				boolean b = instructionProcessor.processQuad(q);
				needNextIteration |= b;
				somethingChanged |= b;
			}
			Utilities.end("ENTRY-LEVEL ITERATION #" + i + " - " + (needNextIteration? "NEED FOR ANOTHER ONE" : "NO NEED FOR ANOTHER ONE"));
			i++;
		} while (needNextIteration && i<=3); // DEBUG: put a limit to the iterations

		ProgramPoint pp2 = GlobalInfo.getFinalPP(entry);
		AbstractValue av2 = GlobalInfo.getAV(pp2);
		av2.cleanGhostRegisters(method.getCFG().getRegisterFactory());
		
		Utilities.begin("UPDATE SUMMARY FOR ENTRY " + entry);
		somethingChanged |= program.getSummaryManager().updateSummaryOutput(entry, av2);
		Utilities.info("NEW SUMMARY FOR " + entry);
		Utilities.info("  INPUT:  " + GlobalInfo.summaryManager.getSummaryInput(entry));
		Utilities.info("  OUTPUT: " + GlobalInfo.summaryManager.getSummaryOutput(entry));
		Utilities.end("UPDATE SUMMARY FOR ENTRY " + entry);
		
		Utilities.end("ANALYSIS OF METHOD " + method);
		return somethingChanged;
	}
	
	/**
	 * Updates the "output" part of the summary manager for the current entry.
	 * 
	 * @return true iff there are changes
	 */
	public boolean updateSummary() {
		Utilities.begin("UPDATE SUMMARY FOR ENTRY " + entry);

		AbstractValue av = GlobalInfo.getAV(GlobalInfo.getFinalPP(entry));
		
		boolean b = program.getSummaryManager().updateSummaryOutput(entry, av);
		Utilities.info("NEW SUMMARY FOR " + entry);
		Utilities.info("  INPUT:  " + program.getSummaryManager().getSummaryInput(entry));
		Utilities.info("  OUTPUT: " + program.getSummaryManager().getSummaryOutput(entry));
		Utilities.end("UPDATE SUMMARY FOR ENTRY " + entry);
		return b;
	}
}
