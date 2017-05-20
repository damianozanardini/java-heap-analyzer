package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chord.analyses.damianoAnalysis.QuadQueue;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.field.DomF;
import chord.analyses.method.DomM;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.util.tuple.object.Pair;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.PrintCFG;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * This class represents a method processor in which the fix-point method 
 * is applied to the instructions of a method. 
 * 
 * @author Javier
 *
 */
public class HeapMethod {

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
	protected InstructionProcessor instructionProcessor;
	
	public HeapMethod(Entry e, HeapProgram p) {
		entry = e;
		method = e.getMethod();
		instructionProcessor = new InstructionProcessor(e,p);
		instructionProcessor.setSummaryManager(p.getSummaryManager());
		instructionProcessor.setEntryManager(p.getEntryManager());
		queue = new QuadQueue(method,QuadQueue.FORWARD);
	}
	
	/**
	 * Execute the fix-point method to the method m
	 */
	protected boolean run(){
		
		Utilities.out("- [INIT] ANALYSIS OF METHOD " + method);

		// initializing the queue
		boolean needNextIteration;
		// implementation of the fixpoint
		do {
			needNextIteration = false;
			for (Quad q : queue) needNextIteration |= instructionProcessor.process(q);
		} while (needNextIteration);
		
		Utilities.out("- [END] ANALYSIS OF METHOD " + method);
		return false;
	}
	
	/**
	 * Updates the "output" part of the summary manager for the current entry.
	 * Returns true iff there are changes
	 * 
	 * @param sm
	 * @return
	 */
	public boolean updateSummary(SummaryManager sm) {
		// retrieve the tuples after all instructions have been processed, and the method-body-level fixpoint has been reached
		AccumulatedTuples acc = instructionProcessor.getAccumulatedTuples();
		ArrayList<Pair<Register,FieldSet>> cycle = new ArrayList<>();
		ArrayList<chord.util.tuple.object.Quad<Register,Register,FieldSet,FieldSet>> share = new ArrayList<>();
		List<Register> paramRegisters = new ArrayList<>();
	
		int begin = method.isStatic()? 0 : 1;
		for (int i = begin; i < method.getParamWords(); i++) {
			// WARNING: why the type argument of getOrCreateLocal is obtained in two different ways?
			// WARNING: it seem this code is never executed
			if(method.getCFG().getRegisterFactory().getOrCreateLocal(i, entry.getCallSite().getUsedRegisters().get(i).getType()).isTemp()) continue;
			paramRegisters.add(method.getCFG().getRegisterFactory().getOrCreateLocal(i, method.getParamTypes()[i]));
			System.out.println("++++ARE THEY EQUAL++++" + entry.getCallSite().getUsedRegisters().get(i).getType());
			System.out.println("++++ARE THEY EQUAL++++" + method.getParamTypes()[i]);
		}

		for(Register r : paramRegisters){
			for(Register r2 : paramRegisters){
				share.addAll(acc.getSFor(entry,r, r2));
			}
			cycle.addAll(acc.getCFor(entry,r));
		}
		
		AbstractValue av = new AbstractValue();
		av.setSComp(new STuples(share));
		av.setCComp(new CTuples(cycle));
		return sm.updateSummaryOutput(entry, av);
	}
}
