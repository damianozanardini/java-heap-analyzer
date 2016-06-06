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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    				//if(bb.getMethod() != null) continue;
    				
    				for (RegisterOperand r : q.getDefinedRegisters()){
    					//Utilities.out("HASH: " + r.getRegister().hashCode() + ",REGISTRO: " + r.getRegister() + ", LINEA: " + q.getLineNumber());
    					//Utilities.out("ADDED?:" + add(r.getRegister()));
    					add(r.getRegister());
    				}
    				for (RegisterOperand r : q.getUsedRegisters()){
    					//Utilities.out("HASH: " + r.getRegister().hashCode() + ",REGISTRO: " + r.getRegister() + ", LINEA: " + q.getLineNumber() + ", METODO: " +q.getMethod());
    					//Utilities.out("ADDED?: " + add(r.getRegister()));
    					add(r.getRegister());
    				}
    				
    			}
            }	
        }
    	
    	Iterator<jq_Method> methods = domM.iterator();
    	while(methods.hasNext()){
    		jq_Method m = methods.next();
    		int begin = 0;
    		if(m.isStatic()){ 
		    	begin = 0;
		    }else{ 
		    	begin = 1; 
		    }
			
			// LIST OF PARAM REGISTERS OF THE METHOD
			for(int i = begin; i < m.getParamWords(); i++){
				Utilities.out("\t REGISTER PARAMETER "+m.getCFG().getRegisterFactory().getOrCreateLocal(i, m.getParamTypes()[i])+" ADDED TO THE DOM IN METHOD " +m);
				add(m.getCFG().getRegisterFactory().getOrCreateLocal(i, m.getParamTypes()[i]));
			}
    		
    	}	
    }
}
