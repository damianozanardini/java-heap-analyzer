package chord.analyses.damianoAnalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import chord.analyses.var.DomV;
import chord.project.ClassicProject;

import joeq.Class.jq_LocalVarTableEntry;
import joeq.Class.jq_Method;
import joeq.Class.jq_NameAndDesc;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import jwutil.collections.Pair;

public class RegisterManager {
	
    /* Code to return the source name of a register in the method*/
    private static Map<Register, ArrayList<String> > varToRegNameMap = null;
    
    public static Register getRegisterFromInputFile(jq_Method m, String s) {
    	try {
			int n = Integer.parseInt(s);
			return RegisterManager.getRegisterByNumber(m,n);
		} catch (NumberFormatException e) {
			return RegisterManager.getRegisterFromSource(m,s);
		}
    	
    }
    
	/**
	 * Gets {@code n}th local variable (i.e., register R{@code n}) of method
	 * {@code m}.
	 * 
	 * @param m The method.
	 * @param n The position in the local variables.
	 * @return the corresponding {@code Register} object.
	 * @throws IndexOutOfBoundsException if the index is not valid
	 */
	public static Register getRegisterByNumber(jq_Method m, int n) throws IndexOutOfBoundsException {
		DomV domV = (DomV) ClassicProject.g().getTrgt("V");
		for (int i=0; i<domV.size(); i++) {
			Register r = domV.get(i);
			if (r.getNumber() == n && domV.getMethod(r) == m) return r;
		}
		throw new IndexOutOfBoundsException();
	}

	/**
	 * When possible, returns the register corresponding to source-code
	 * variable id at the beginning of m's execution 
	 * 
	 * @param m the method to which variable refer
	 * @param id the name of the variable in the source code
	 * @return the register corresponding to id
	 */
	public static Register getRegisterFromSource(jq_Method m,String id) {
		Register x = null;
		DomV domV = (DomV) ClassicProject.g().getTrgt("V");
		for (int i=0; i<domV.size() && x==null; i++) {
			Register r = domV.get(i);
			ArrayList<String> rlist = RegisterManager.getRegName(m,r);
			if (rlist != null) {
				String s = rlist.get(0);
				if (s != null) {
					if (s.equals(id) || s.substring(0,id.length()).equals(id)) x = r;
				}
			}
		}		
		return x;
	}
	
	public static Register getRegisterFromSource_end(jq_Method m,String id) {
		return getRegisterFromRegister_end(m,getRegisterFromSource(m,id));
	}
		
	public static Register getRegisterFromRegister_end(jq_Method m,Register r_begin) {
		if (r_begin == null) return null;
		boolean toBeContinued = true;
		ArrayList<BasicBlock> bblist = (ArrayList<BasicBlock>) m.getCFG().reversePostOrder();
		while (toBeContinued) {
			toBeContinued = false;
			for (BasicBlock bb : bblist) {
				for (Quad q : bb.getQuads()) {
					if (q.getOperator() instanceof Phi) {
						Register r1 = ((RegisterOperand) Phi.getSrc(q,0)).getRegister();
						Register r2 = ((RegisterOperand) Phi.getSrc(q,1)).getRegister();
						if (r1 == r_begin || r2 == r_begin) {
							r_begin = ((RegisterOperand) Phi.getDest(q)).getRegister();
							toBeContinued = true;
						}
					}
				}
			}
		}
		return r_begin;
	}

	public static Hashtable<Register,String> printSourceCodeVariables(jq_Method m) {
		Hashtable<Register,String> h = new Hashtable<Register,String>();
		DomV domV = (DomV) ClassicProject.g().getTrgt("V");
		Utilities.out("");
		for (int i=0; i<domV.size(); i++) {
			Register r = domV.get(i);
			ArrayList<String> rlist = RegisterManager.getRegName(m,r);
			Register r1 = getRegisterFromRegister_end(m,r);
			if (rlist != null) {
				String s = rlist.get(0);
				h.put(r,s);
				Utilities.out("    " + s + " ---> " + r + " (" + r1 + " AT THE END)");
			}
		}
		Utilities.out("");
		return h;
	}
	
	/**
	 * The code of this method has been taken (and modified) from
	 * joeq/src/joeq/Class/jq_Method.java
	 * 
	 * @param m
	 * @param v
	 * @return
	 */
    protected static ArrayList<String> getRegName(jq_Method m, Register v){
    	if(varToRegNameMap == null){
    		varToRegNameMap = new HashMap<Register,ArrayList<String>>();
    		getRegNames(m);
    	}
    	return varToRegNameMap.get(v);
    }
    
    /**
     * The code of this method has been taken (and modified) from
	 * joeq/src/joeq/Class/jq_Method.java
	 * 
     * @param m
     */
    protected static void getRegNames(jq_Method m) {
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

    /**
	 * The code of this method has been taken (and modified) from
	 * joeq/src/joeq/Class/jq_Method.java
	 * 
     * @param m
     * @param q
     */
	protected static void processForRegName(jq_Method m,Quad q) {
		for(Operand op : q.getDefinedRegisters()){
			if (op instanceof RegisterOperand) {
				RegisterOperand ro = (RegisterOperand) op;
				Register v = ro.getRegister();
				if (v.isTemp()) {
					getStackRegName(m,v,q);
				} else {
					getLocalRegName(m,v,q);
				}
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

	/**
	 * The code of this method has been taken (and modified) from
	 * joeq/src/joeq/Class/jq_Method.java
	 * 
	 * @param m
	 * @param v
	 * @param q
	 */
	protected static void getLocalRegName(jq_Method m, Register v, Quad q){
		String regName = "";
		int index = -1;
		Map localNum = m.getCFG().getRegisterFactory().getLocalNumberingMap();
		for (Object o : localNum.keySet()){
			if (localNum.get(o).equals(v)){
				index = (Integer) ((Pair)o).right;
				break;
			}
		}
		
		jq_LocalVarTableEntry localVarTableEntry = null;
		if (q != null) {
			localVarTableEntry = m.getLocalVarTableEntry(q.getBCI()+1, index);
			// TODO this is a(n additional) patch which is meant to cope with a
			// probable bug in joeq: at some point, I found in the local var
			// table of a class file the following:
			//    [pc: 37, pc: 66] local: e index: 4 type: nice.C
			// However, such variable was found by getLocalVarTableEntry at 37,
			// not 36, so that I add a second possibility to find the variable
			if (localVarTableEntry == null) {
				localVarTableEntry = m.getLocalVarTableEntry(q.getBCI()+2, index);				
			}
		} else {
			localVarTableEntry = m.getLocalVarTableEntry(0, index);
		}
		
		// Damiano: This patch is meant to fix the situation where a register
		// is not properly found in the local numbering map (index == -1).
		// The special case of a Phi Quad has to be ignored in order not to
		// create a wrong association.
		if (index == -1 && !(q.getOperator() instanceof Phi)) {
			int bci = q.getBCI()+1;
			for (int i = 0; i<20; i++) {
				jq_LocalVarTableEntry x = m.getLocalVarTableEntry(bci, i);
				if (x != null && x.getStartPC()==bci)
					localVarTableEntry = x;
			}
		}
		
		if(localVarTableEntry == null){
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

	/**
 	 * The code of this method has been taken (and modified) from
	 * joeq/src/joeq/Class/jq_Method.java
	 * 
	 * @param m
	 * @param v
	 * @param q
	 */
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
