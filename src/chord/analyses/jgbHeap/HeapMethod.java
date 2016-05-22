package chord.analyses.jgbHeap;

import java.util.ArrayList;
import java.util.HashMap;
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

public class HeapMethod {

	/**
	 * The queue for implementing the fixpoint.
	 */
	private QuadQueue queue;
	
	protected HeapFixpoint fixPoint;
	
	public void setHeapFixPoint(HeapFixpoint fp){ this.fixPoint = fp; }
	
	public HeapMethod(){}
	
	protected boolean runM(jq_Method m){
		
		Utilities.out("- [INIT] ANALYSIS OF METHOD " + m);

		// initializing the queue
		boolean needNextIteration;
		queue = new QuadQueue(m,QuadQueue.FORWARD);
		// implementation of the fixpoint
		do {
			needNextIteration = false;
			for (Quad q : queue) needNextIteration |= fixPoint.process(q);
		} while (needNextIteration);
		
		Utilities.out("- [END] ANALYSIS OF METHOD " + m);
		return false;
	}
	
	
}
