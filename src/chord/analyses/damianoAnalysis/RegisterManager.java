package chord.analyses.damianoAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import joeq.Class.jq_LocalVarTableEntry;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import jwutil.collections.Pair;

public class RegisterManager {
	
    /* Code to return the source name of a register in the method*/
    private static Map<Register, ArrayList<String> > varToRegNameMap = null;
    
    public static ArrayList<String> my_getRegName(jq_Method m, Register v){
    	if(varToRegNameMap == null){
    		varToRegNameMap = new HashMap<Register,ArrayList<String>>();
    		getRegNames(m);
    	}
    	return varToRegNameMap.get(v);
    }
    
    private static void getRegNames(jq_Method m) {
    	ControlFlowGraph cfg = m.getCFG();
    	RegisterFactory rf = cfg.getRegisterFactory();
    	jq_Type[] paramTypes = m.getParamTypes();
    	int numArgs = paramTypes.length;
    	for (int i = 0; i < numArgs; i++) {
    		Register v = rf.get(i);
    		getLocalRegName(m,v,null);
    	}
    	for (BasicBlock bb : cfg.reversePostOrder()) {
    		for (Quad q : bb.getQuads()) {
    			processForRegName(m,q);
    		}
    	}
    }

	private static void processForRegName(jq_Method m,Quad q) {
		for(Operand op : q.getDefinedRegisters()){
			if (op instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) op;
				Register v = ro.getRegister();
				if (v.isTemp())
					getStackRegName(m,v,q);
				else
					getLocalRegName(m,v,q);
			} else if (op instanceof ParamListOperand) {
				ParamListOperand ros = (ParamListOperand) op;
				int n = ros.length();
				for (int i = 0; i < n; i++) {
					RegisterOperand ro = ros.get(i);
					if (ro == null) continue;
					Register v = ro.getRegister();
					if (v.isTemp())
						getStackRegName(m,v,q);
					else
						getLocalRegName(m,v,q);
				}
			}
		}
	}

	private static void getLocalRegName(jq_Method m, Register v, Quad q){
		//System.out.println(m.getCFG().fullDump());
		String regName = "";
		int index = -1;
		Map localNum = m.getCFG().getRegisterFactory().getLocalNumberingMap();
		for (Object o: localNum.keySet()){
			if (localNum.get(o).equals(v)){
				//System.out.println(localNum.get(o) + ":" + v);
				index = (Integer) ((Pair)o).right;
				break;
			}
		}
		
		jq_LocalVarTableEntry localVarTableEntry = null;
		if (q!=null){
			localVarTableEntry = m.getLocalVarTableEntry(q.getBCI()+1, index);
		} else {
			localVarTableEntry = m.getLocalVarTableEntry(0, index);
		}
		
		if(localVarTableEntry==null){
			getStackRegName(m, v, q);
			return;
		}			
			
		jq_NameAndDesc regNd = localVarTableEntry.getNameAndDesc();
		regName += regNd.getName() + ":" + regNd.getDesc();
		
		ArrayList<String> regNames = varToRegNameMap.get(v);
		if (regNames==null){
			regNames = new ArrayList<String>();
			regNames.add(regName);
			varToRegNameMap.put(v, regNames);
		} else regNames.add(regName);
	}

	private static void getStackRegName(jq_Method m, Register v, Quad q){
		ArrayList<String> regNames = varToRegNameMap.get(v);
		if (regNames==null){
			regNames = new ArrayList<String>();
			regNames.add(v.toString());
			varToRegNameMap.put(v, regNames);
		} else {
			//regNames.add(v.toString());
		}
	}

}
