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

	private HeapFixpoint fixPoint;

	/**
	 * The queue for implementing the fixpoint.
	 */
	private QuadQueue queue;

	private AccumulatedTuples accumulatedTuples;

	/**
	 * The sharing relation.
	 */
	private RelShare relShare;

	public RelShare getRelShare() { return relShare; }

	/**
	 * The cyclicity relation.
	 */
	private RelCycle relCycle;

	public RelCycle getRelCycle() { return relCycle; }
	
	
	/**
	 * Variables to be analyzed Ffor sharing
	 */
	private ArrayList<Pair<Register,Register>> outShare = null;

	public ArrayList<Pair<Register,Register>> getOutShare(){ return outShare; }
	
	/**
	 * Variables to be analyzed for cyclicity
	 */

	private ArrayList<Register> outCycle = null;

	public ArrayList<Register> getOutCycle(){ return outCycle; }
	
	/**
	 * Actual analyzed method
	 */
	private jq_Method acMeth;
	public void setMethod(jq_Method meth){
		this.acMeth = meth;
	}

	/**
	 * Methods who are call from acMeth
	 */
	private Map<jq_Method,ArrayList<jq_Method>> calledMethods;
	
	private EntryManager entrymanager;
	private ArrayList<Entry> entries;	
	
	public HeapMethod(HeapFixpoint fp){ fixPoint = fp; }

	public void init(){

		accumulatedTuples = new AccumulatedTuples();
		relShare = (RelShare) ClassicProject.g().getTrgt("HeapShare");
		relShare.run();
		relShare.load();
		relShare.accumulatedTuples = accumulatedTuples;
		relCycle = (RelCycle) ClassicProject.g().getTrgt("HeapCycle");
		relCycle.run();
		relCycle.load();
		relCycle.accumulatedTuples = accumulatedTuples;
		outShare = new ArrayList<Pair<Register,Register>>();
		outCycle = new ArrayList<Register>();

		calledMethods = new HashMap<>();
		fixPoint = new HeapFixpoint();
	}
	
	protected boolean runM(jq_Method m){
		
		// initializing the queue
		boolean needNextIteration;
		queue = new QuadQueue(m,QuadQueue.FORWARD);
		// implementation of the fixpoint
		do {
			needNextIteration = false;
			for (Quad q : queue) needNextIteration |= fixPoint.process(q,relCycle,relShare,m);
		} while (needNextIteration);
		
		return false;
	}

	public void clear() {

		accumulatedTuples.cycle.clear();
		accumulatedTuples.cyclePrime.clear();
		accumulatedTuples.share.clear();
		accumulatedTuples.sharePrime.clear();
		relShare.zero();
		relCycle.zero();
		outShare.clear();
		outCycle.clear();
		calledMethods.clear();

		relShare.run();
		relShare.load();
		relShare.accumulatedTuples = accumulatedTuples;
		relCycle.run();
		relCycle.load();
		relCycle.accumulatedTuples = accumulatedTuples;
	}
	
	/**
	 * For each method that is analyzed it is searched the methods called 
	 * by thats methods. 
	 * @param m
	 */
	protected void loadCalledMethods(jq_Method m){
		
		ArrayList<jq_Method> methods = new ArrayList<>();
		QuadQueue q = new QuadQueue(m,QuadQueue.FORWARD);
		for(Quad quad : q){
			Utilities.out("INSTRUCCION " + quad.getAllOperands().toString());
			if(quad.getOperator() instanceof Invoke && 
					!(quad.getOp2().toString().matches("(.*)<init>(.*)"))){
				Utilities.out("METODO LLAMADO " + quad.getMethod());
				methods.add(quad.getMethod());
			}
		}
		
		calledMethods.put(m, methods);
	}

	public void printOutput() {
		for (Pair<Register,Register> p : outShare) {
			accumulatedTuples.askForS(acMeth,p.val0,p.val1);
			//accumulatedTuples.askForSWeb("chord_output/webOutput",acMeth,p.val0,p.val1);
		}
		for (Register r : outCycle) {
			accumulatedTuples.askForC(acMeth,r);
		}
	}
}
