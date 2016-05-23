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
    				
    				// CODIGO JAVIER
    				if((bb.isEntry()) && (bb.getMethod() != null)){
    					int begin;
        				if(bb.getMethod().isStatic()){ 
        		    		begin = 0;
        		    	}else{ 
        		    		begin = 1; 
        		    	}
        				for(int i = begin; i < bb.getMethod().getParamWords(); i++){
        					//if(bb.getMethod().getCFG().getRegisterFactory().getOrCreateLocal(0,bb.getMethod().getParamTypes()[i]).getType().isPrimitiveType()) continue;
        					Register r = bb.getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i,bb.getMethod().getParamTypes()[i]);
        					Utilities.out("HASH: " + r.hashCode() + ",REGISTRO: " + r + ", BB: " + bb.toString() + ", METH: " + bb.getMethod());
        					Utilities.out("ADDED?:" + add(r));
        					add(r);
        				}
        			}
    				// CODIGO JAVIER
    				
    				continue;
    			}
    			for (Quad q : bb.getQuads()) {
    				for (RegisterOperand r : q.getDefinedRegisters()){
    					Utilities.out("HASH: " + r.getRegister().hashCode() + ",REGISTRO: " + r.getRegister() + ", LINEA: " + q.getLineNumber());
    					Utilities.out("ADDED?:" + add(r.getRegister()));
    				}
    				for (RegisterOperand r : q.getUsedRegisters()){
    					Utilities.out("HASH: " + r.getRegister().hashCode() + ",REGISTRO: " + r.getRegister() + ", LINEA: " + q.getLineNumber() + ", METODO: " +q.getMethod());
    					Utilities.out("ADDED?: " + add(r.getRegister()));
    				}
    				
    			}
    			
            }
        }
    }
}
