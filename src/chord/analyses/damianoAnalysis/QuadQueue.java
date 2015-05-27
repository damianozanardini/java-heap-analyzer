package chord.analyses.damianoAnalysis;

import java.util.*;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;

public class QuadQueue extends LinkedList<Quad> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final int FORWARD = 0;
	public static final int BACKWARD = 1;

	public QuadQueue(jq_Method meth,int direction) {
		if (direction==FORWARD) fill_fw(meth);
		if (direction==BACKWARD) fill_bw(meth);
	}	
		
	public void fill_fw(jq_Method meth) {
		ControlFlowGraph cfg = CodeCache.getCode(meth);
    	List<BasicBlock> bbs = cfg.postOrderOnReverseGraph(cfg.exit());
    	for (BasicBlock bb : bbs) {
    		for (Quad q : bb.getQuads()) {
    			addElem(q);
    		}
    	}
	}
	
	public void fill_bw(jq_Method meth) {
		ControlFlowGraph cfg = CodeCache.getCode(meth);
    	List<BasicBlock> bbs = cfg.reversePostOrderOnReverseGraph();
    	for (BasicBlock bb : bbs) {
    		List<Quad> qs = bb.getQuads();
    		for (int i = qs.size()-1; i>=0; i--) {
    			addElem(qs.get(i));
    		}
    	}
	}
    public void addElem(Quad q) {
    	if (!this.contains(q)) {
    		add(q);
    	}
    }

    public void addList(List<Quad> l) {
    	Iterator<Quad> iterator = l.iterator();
    	while (iterator.hasNext()) addElem(iterator.next());
    }    
}

