package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import chord.analyses.damianoAnalysis.DomRegister;
import chord.analyses.damianoAnalysis.Entry;
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
		instructionProcessor = new InstructionProcessor(entry,program);
		queue = new QuadQueue(method,QuadQueue.FORWARD);
	}
	
		/**
	 * Creates the ghost variables. It includes: 
	 * 		- Create a new register
	 * 		- Save the new register in DomRegister
	 * 		- Reinitialize the relations
	 * 		- Copy the tuples from the original register to the new register 
	 */
	public void createGhostVariables(){
		Utilities.begin("CREATE GHOST VARIABLE FOR " + entry);
		AbstractValue input = program.getSummaryManager().getSummaryInput(entry);
		
		// nothing to do in this case (WARNING: check why)
		if (input == null) return;
				
		List<Register> paramRegisters = new ArrayList<>();
		int begin = method.isStatic() ? 0 : 1;
		DomRegister domR = (DomRegister) ClassicProject.g().getTrgt("Register");
		
		// loop on all the parameters
		// (WARNING: shouldn't we ignore primitive types? But probably we can live without it)
		for (int i = begin; i < method.getParamWords(); i++){
			if (method.getCFG().getRegisterFactory().getOrCreateLocal(i, entry.getCallSite().getUsedRegisters().get(i).getType()).isTemp()) continue;
			paramRegisters.add(method.getCFG().getRegisterFactory().getOrCreateLocal(i, method.getParamTypes()[i]));
		}
		
		for(Register ro : paramRegisters){
			if(ro.getType().isPrimitiveType()) continue;
			RegisterFactory rf = method.getCFG().getRegisterFactory();
			RegisterOperand rop = rf.makeTempRegOp(ro.getType());
			Register rprime = rop.getRegister();
		
			Utilities.out("----- GHOST VARIABLE CREATED " + rprime.toString());
			Utilities.out("----- [INIT] SAVE DOM");
			
			domR = (DomRegister) ClassicProject.g().getTrgt("Register");
			domR.add(rprime);
			domR.save();
			
			ghostVariables.add(new Pair<Register,Register>(ro,rprime));
			
			Utilities.out("----- [END] SAVE DOM");
		}
		
		program.getRelCycle().reinitialize();
		program.getRelShare().reinitialize();	
		for(Pair<Register,Register> p : ghostVariables){
			Utilities.begin("COPY TUPLES FROM " + p.val0 + " TO GHOST VARIABLE " + p.val1);		
			program.getRelShare().copyTuples(entry, p.val0, p.val1);
			program.getRelCycle().copyTuples(entry, p.val0, p.val1);
			Utilities.end("COPY TUPLES FROM " + p.val0 + " TO GHOST VARIABLE " + p.val1);		
		}
		Utilities.end("CREATE GHOST VARIABLE FOR " + entry);
	}

	/**
	 * Deletes the non-ghost variables of the entry.
	 */
	public void deleteNonGhostVariables(){
		if (entry.isTheMain()) return; // does nothing for the main
		Utilities.begin("DELETE NON-GHOST VARIABLES FOR " + entry);
		
		List<Register> paramRegisters= new ArrayList<>();
		
		int begin = method.isStatic()? 0 : 1;
		
		for (int i = begin; i < method.getParamWords(); i++){
			if (method.getCFG().getRegisterFactory().getOrCreateLocal(i, entry.getCallSite().getUsedRegisters().get(i).getType()).isTemp()) continue;
			paramRegisters.add(method.getCFG().getRegisterFactory().getOrCreateLocal(i, method.getParamTypes()[i]));
		}
		
		ArrayList<Pair<Register,Register>> ghostvariablestodelete = new ArrayList<>();
		for(Register ro : paramRegisters){
			if(ro.getType().isPrimitiveType()) continue;
			for(Pair<Register,Register> p : ghostVariables){
				if(p.val0 == ro){
					program.getRelShare().moveTuples(entry, p.val1, p.val0);
					program.getRelShare().moveTuples(entry, p.val1, p.val0);
					ghostvariablestodelete.remove(p);
					break;
				}
			}
		}
		ghostVariables.remove(ghostvariablestodelete);
		Utilities.end("DELETE NON-GHOST VARIABLES FOR " + entry);
	}
	
	/**
	 * Execute the fix-point method to the method m
	 */
	public boolean run(){
		
		Utilities.begin("ANALYSIS OF METHOD " + method);

		int i = 1;
				
		// this variable is true iff there are changes in the abstract information during the inspection of the method code
		boolean needNextIteration;
		
		// this variable is true iff there are changes AT ALL (in any iteration)
		boolean somethingChanged = false;
		// implementation of the fixpoint
		do {
			Utilities.begin("ENTRY-LEVEL ITERATION #" + i);
			needNextIteration = false;
			for (Quad q : queue) {
				boolean b = instructionProcessor.process(q);
				needNextIteration |= b;
				somethingChanged |= b;
			}
			Utilities.end("ENTRY-LEVEL ITERATION #" + i + " - " + (needNextIteration? "NEED FOR ANOTHER ONE" : "NO NEED FOR ANOTHER ONE"));
			i++;
		} while (needNextIteration && i<=3); // DEBUG: put a limit to the iterations
		
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
		
		// WARNING: we are avoiding the use of accumulatedTuples in any sense.
		// Have to see if this is correct
				
		AbstractValue av = new AbstractValue(program.getRelShare(),program.getRelCycle());
		
		boolean b = program.getSummaryManager().updateSummaryOutput(entry, av);
		Utilities.info("NEW SUMMARY FOR " + entry);
		Utilities.info("  INPUT:  " + program.getSummaryManager().getSummaryInput(entry));
		Utilities.info("  OUTPUT: " + program.getSummaryManager().getSummaryOutput(entry));
		Utilities.end("UPDATE SUMMARY FOR ENTRY " + entry);
		return b;
	}
}
