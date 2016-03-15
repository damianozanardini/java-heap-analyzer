package chord.analyses.jgbHeap;

import java.util.ArrayList;

import chord.analyses.damianoAnalysis.QuadQueue;
import chord.project.ClassicProject;
import chord.util.tuple.object.Pair;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
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

	private ArrayList<Pair<Register,Register>> outShare = null;
	
	public ArrayList<Pair<Register,Register>> getOutShare(){ return outShare; }
	
	private ArrayList<Register> outCycle = null;
	
	public ArrayList<Register> getOutCycle(){ return outCycle; }
	
	private jq_Method acMeth;
	
	public HeapMethod(){}
	
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
		
		fixPoint = new HeapFixpoint();
		
	}
	
	
	// JGB de momento ponemos void como tipo de retorno
	// JGB este el el metodo que deberia llamar luego al HeapFixpoint
	// JGB el que llama este metodo es Heap, que deberia llamarlo para todos los metodos; de momento asumimos que lo llame para el main
	// JGB la lectura de fichero de input hay que moverla a Heap y separarla del Fixpoint
	public void run(jq_Method meth) {
		
		acMeth = meth;
		 // initializing the queue
		 queue = new QuadQueue(meth,QuadQueue.FORWARD);
		// implementation of the fixpoint
	    	boolean needNextIteration;
	    	do {
	    		needNextIteration = false;
	    		for (Quad q : queue) needNextIteration |= fixPoint.process(q,relCycle,relShare);
	    	} while (needNextIteration);
		
	}
	
	public void printOutput() {
		
    	for (Pair<Register,Register> p : outShare) {
    		accumulatedTuples.askForS(acMeth,p.val0,p.val1);
    		accumulatedTuples.askForSWeb("chord_output/webOutput",acMeth,p.val0,p.val1);
    	}
    	for (Register r : outCycle) {
    		accumulatedTuples.askForC(acMeth,r);
    	}
    }

}
