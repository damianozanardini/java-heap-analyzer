package chord.analyses.damianoCyclicity;

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
       name = "Register",
       consumes = { "M" }
)
public class DomRegister extends ProgramDom<Register> {

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
    			int n = bb.size();
    			if (n == 0) {
    				assert (bb.isEntry() || bb.isExit());
    				continue;
    			}
    			for (Quad q : bb.getQuads()) {
    				for (RegisterOperand r : q.getDefinedRegisters()) add(r.getRegister());
    				for (RegisterOperand r : q.getUsedRegisters()) add(r.getRegister());
    			}
            }
        }
    }

}
