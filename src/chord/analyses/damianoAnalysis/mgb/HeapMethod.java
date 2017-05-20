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
	private Entry entry;
	private jq_Method method;
	
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
	 *  
	 * @param m
	 * @return boolean
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
	
	public boolean updateSummary(SummaryManager sm) {
	
		// copy tuples to summary output
		AccumulatedTuples acc = instructionProcessor.getAccumulatedTuples();
		ArrayList<Pair<Register,FieldSet>> cycle = new ArrayList<>();
		ArrayList<chord.util.tuple.object.Quad<Register,Register,FieldSet,FieldSet>> share = new ArrayList<>();
		List<Register> paramRegisters = new ArrayList<>();
	
		int begin = method.isStatic()? 0 : 1; 
		
		for (int i = begin; i < method.getParamWords(); i++) {
			if(method.getCFG().getRegisterFactory().getOrCreateLocal(i, entry.getCallSite().getUsedRegisters().get(i).getType()).isTemp()) continue;
			paramRegisters.add(method.getCFG().getRegisterFactory().getOrCreateLocal(i, method.getParamTypes()[i]));
		}

		for(Register r : paramRegisters){
			for(Register r2 : paramRegisters){
				//Utilities.out("----- R: " + r + " --- R: " + r2);
				share.addAll(acc.getSFor(entry,r, r2));
			}
			//Utilities.out("----- R: " + r);
			cycle.addAll(acc.getCFor(entry,r));
		}
		
		AbstractValue av = new AbstractValue();
		av.setSComp(new STuples(share));
		av.setCComp(new CTuples(cycle));
		return sm.updateSummaryOutput(entry, av);
	}
}
