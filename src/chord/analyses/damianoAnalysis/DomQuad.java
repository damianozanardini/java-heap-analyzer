package chord.analyses.damianoAnalysis;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;
import chord.analyses.method.DomM;

@Chord(
       name = "Quad",
       consumes = { "M" }
)
public class DomQuad extends ProgramDom<Quad> {

    public void fill() {
    	ClassicProject.g();
    	DomM domM = (DomM) ClassicProject.g().getTrgt("M");
    	int numM = domM.size();
    	for (int mIdx = 0; mIdx < numM; mIdx++) {
    		jq_Method m = domM.get(mIdx);
    		if (m.isAbstract())
    			continue;
    		ControlFlowGraph cfg = m.getCFG();
    		for (BasicBlock bb : cfg.reversePostOrder()) {
    			for (Quad q : bb.getQuads()) {
    				add(q);
    			}
    		}
    	}
    }

}
