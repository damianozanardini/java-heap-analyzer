package chord.analyses.damianoCycle;

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

	public QuadQueue(jq_Method meth) {
		fill(meth);
	}	
		
	public void fill(jq_Method meth) {
		ControlFlowGraph cfg = CodeCache.getCode(meth);
    	List<BasicBlock> bbs = cfg.postOrderOnReverseGraph(cfg.exit());
    	for (BasicBlock bb : bbs) {
    		for (Quad q : bb.getQuads()) {
    			addElem(q);
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

