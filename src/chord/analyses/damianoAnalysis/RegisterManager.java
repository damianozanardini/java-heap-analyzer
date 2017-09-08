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
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import jwutil.collections.Pair;

/**
 * This class is in charge to keep a correspondence between source-code-level
 * variables and bytecode-level registers. This is needed in order to 
 * interpret user-defined questions about sharing and cyclicity between
 * variables.  Such questions are written in the "input" file of each example.
 * 
 * Only the initial method has to be taken into account because questions in
 * the "input" file are only about variables of the initial method. 
 * 
 * @author damiano
 *
 */
public class RegisterManager {

	/**
	 * The map between registers and source-code variables
	 */
    private static Map<Register, ArrayList<String>> varRegMap = null;
    
    /**
     * This method takes a reference method {@code m} and a string which is a
     * token parsed from the analysis input file, and
     * (1) if the token is a number n, then the register Rn is returned
     * (or an {@code IndexOutOfBoundException} is thrown)
     * (2) otherwise, the method tries to read the token as the name of a
     * source-code variable, and return the register associated with the
     * variable (or null if it doesn't find it)
     * 
     * @param m The method of reference
     * @param s The token (a String)
     * @return the corresponding register
     */
    public static Register getRegFromInputToken(jq_Method m, String s) {
    	try {
			int n = Integer.parseInt(s);
			return RegisterManager.getRegFromNumber(m,n);
		} catch (NumberFormatException e) {
			return RegisterManager.getRegFromVar(m,s);
		}
    }
    
    /**
     * This method does the same as getRegFromInputToken with a difference: if
     * the token has to be read as the name of a source-code variable, and the
     * variable is associated with more than one register (this can happen if
     * the variable is involved in some Phi quad), then the register
     * corresponding to the end (instead of the beginning) of {@code m} is
     * returned.
     * 
     * @param m The method of reference
     * @param s The token (a String)
     * @return the corresponding register
     */
    public static Register getRegFromInputToken_end(jq_Method m, String s) {
    		try {
    			int n = Integer.parseInt(s);
    			return RegisterManager.getRegFromNumber(m,n);
		} catch (NumberFormatException e) {
			return RegisterManager.getRegFromVar_end(m,s);
		}
    	
    }

    /**
	 * Gets {@code n}th register (i.e., register R{@code n}) of method
	 * {@code m}.
	 * 
	 * @param m The method of reference
	 * @param n The position in the local variables
	 * @return the corresponding {@code Register} object
	 * @throws IndexOutOfBoundsException if the index is not valid
	 */
	public static Register getRegFromNumber(jq_Method m, int n)
			throws IndexOutOfBoundsException {
		DomV domV = (DomV) ClassicProject.g().getTrgt("V");
		for (int i=0; i<domV.size(); i++) {
			Register r = domV.get(i);
			if (r.getNumber() == n && domV.getMethod(r) == m) return r;
		}
		throw new IndexOutOfBoundsException();
	}

	/**
	 * When possible, returns the register corresponding to the source-code
	 * variable {@code v} at the beginning of {@code m}'s execution 
	 * 
	 * @param m The method of reference
	 * @param v The name of the variable in the source code
	 * @return the register corresponding to id
	 */
	public static Register getRegFromVar(jq_Method m,String v) {
		Register x = null;
		DomV domV = (DomV) ClassicProject.g().getTrgt("V");
		for (int i=0; i<domV.size() && x==null; i++) {
			Register r = domV.get(i);
			ArrayList<String> rlist = RegisterManager.getRegName(m,r);
			if (rlist != null) {
				String s = rlist.get(0);
				if (s != null) {
					if (s.equals(v) || s.substring(0,Math.min(s.length(),v.length())).equals(v)) {
						x = r;
					}
				}
			}
		}		
		return x;
	}
	
	/**
	 * Similar to getRegFromVar, but the register refers to the end of
	 * {@code m} (there can be more than one register associated to the same
	 * source-code variable if the latter is involved in some Phi quad).
	 * 
	 * @param m The method of reference
	 * @param v The name of the variable in the source code
	 * @return the register associated to id at the end of m
	 */
	public static Register getRegFromVar_end(jq_Method m,String id) {
		return getRegFromReg_end(m,getRegFromVar(m,id));
	}
	
	/**
	 * Given a register {@code r0} returns the register associated to the same
	 * source-code variable but referring to the end of {@code m}. 
	 * In most cases, both register are the same register.
	 * 
	 * @param m The method of reference
	 * @param r0 The register intended to refer to the beginning of m
	 * @return the register intended to refer to the end of m, and associated
	 * with the same variable as r0
	 */
	public static Register getRegFromReg_end(jq_Method m,Register r0) {
		if (r0 == null) return null;
		boolean toBeContinued = true;
		ArrayList<BasicBlock> bblist = (ArrayList<BasicBlock>) m.getCFG().reversePostOrder();
		while (toBeContinued) {
			toBeContinued = false;
			for (BasicBlock bb : bblist) {
				for (Quad q : bb.getQuads()) {
					if (q.getOperator() instanceof Phi) {
						Register r1 = ((RegisterOperand) Phi.getSrc(q,0)).getRegister();
						Register r2 = ((RegisterOperand) Phi.getSrc(q,1)).getRegister();
						if (r1 == r0 || r2 == r0) {
							r0 = ((RegisterOperand) Phi.getDest(q)).getRegister();
							toBeContinued = true;
						}
					}
				}
			}
		}
		return r0;
	}

	/**
	 * This method prints the map between source-code variables and register
	 * (both at the beginning and the end of {@code m}).
	 * Such mapping is also returned as a Hashtable.
	 * 
	 * @param m The method of reference
	 * @return the mapping in form of a Hashtable
	 */
	public static Hashtable<String,chord.util.tuple.object.Pair<Register,Register>>
			printVarRegMap(jq_Method m) {
		Hashtable<String,chord.util.tuple.object.Pair<Register,Register>> h =
				new Hashtable<String,chord.util.tuple.object.Pair<Register,Register>>();
		DomV domV = (DomV) ClassicProject.g().getTrgt("V");
		Utilities.debug("");
		for (int i=0; i<domV.size(); i++) {
			Register r0 = domV.get(i);
			ArrayList<String> rlist = RegisterManager.getRegName(m,r0);
			Register r1 = getRegFromReg_end(m,r0);
			if (rlist != null) {
				String s = rlist.get(0);
				h.put(s,new chord.util.tuple.object.Pair<Register,Register>(r0,r1));
				Utilities.debug("    " + s + " ---> " + r0 + " (" + r1 + " AT THE END)");
			}
		}
		Utilities.debug("");
		return h;
	}
	
	/**
	 * The code of this method has been taken (and modified) from
	 * joeq/src/joeq/Class/jq_Method.java
	 * 
	 * @param m The method of reference
	 * @param r The register
	 * @return the corresponding source-code variable
	 */
    private static ArrayList<String> getRegName(jq_Method m, Register r) {
    		if (varRegMap == null) {
    			varRegMap = new HashMap<Register,ArrayList<String>>();
    			getRegNames(m);
    		}
    		return varRegMap.get(r);
    }
    
    /**
     * The code of this method has been taken (and modified) from
	 * joeq/src/joeq/Class/jq_Method.java.
	 * It populates the mapping between variables and registers.
	 * 
     * @param m The method of reference
     */
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

    /**
	 * The code of this method has been taken (and modified) from
	 * joeq/src/joeq/Class/jq_Method.java.
	 * This method examines a quad {@code q} and searches for the mapping
	 * between registers involved in the {@code q} and source-code variables.
	 * 
     * @param m The method of reference
     * @param q The quad to be inspected
     */
	private static void processForRegName(jq_Method m,Quad q) {
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
	 * joeq/src/joeq/Class/jq_Method.java.
	 * The information about the source-code variable is stored into
	 * {@code varRegMap}.
	 * This method has been heavily patched in order to "find" variables which
	 * originally were not found; probably, one of the patches is needed
	 * because of a bug in joeq.
	 * 
	 * @param m The method of reference
	 * @param r The register
	 * @param q the quad
	 */
	private static void getLocalRegName(jq_Method m, Register r, Quad q){
		String regName = "";
		int index = -1;
		Map localNum = m.getCFG().getRegisterFactory().getLocalNumberingMap();
		for (Object o : localNum.keySet()){
			if (localNum.get(o).equals(r)){
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
			getStackRegName(m, r, q);
			return;
		}			
			
		jq_NameAndDesc regNd = localVarTableEntry.getNameAndDesc();
		regName += regNd.getName() + ":" + regNd.getDesc();
		
		ArrayList<String> regNames = varRegMap.get(r);
		if (regNames==null){
			regNames = new ArrayList<String>();
			regNames.add(regName);
			varRegMap.put(r, regNames);
		} else regNames.add(regName);
	}

	/**
 	 * The code of this method has been taken (and modified) from
	 * joeq/src/joeq/Class/jq_Method.java.
	 * This method simply puts the name of a stack register into varRegMap.
	 * 
	 * @param m The method of reference
	 * @param r The stack register
	 * @param q the quad (not used)
	 */
	private static void getStackRegName(jq_Method m, Register r, Quad q){
		ArrayList<String> regNames = varRegMap.get(r);
		if (regNames == null){
			regNames = new ArrayList<String>();
			regNames.add(r.toString());
			varRegMap.put(r, regNames);
		} else {
			//regNames.add(v.toString());
		}
	}

	/**
	 * This method returns the source-code variable corresponding to a
	 * register, or "<UNKNOWN>" (this special value makes it different from
	 * getRegName, which, on the other hand, is also different because it is
	 * private).
	 * Mainly used to output analysis results.
	 * 
	 * @param m The method of reference
	 * @param r The register
	 * @return the identifier (with type)
	 */
	public static String getVarFromReg(jq_Method m, Register r) {
		if (hasName(m,r)) {
			return RegisterManager.getRegName(m,r).get(0);
		}
		ArrayList<BasicBlock> bblist = (ArrayList<BasicBlock>) m.getCFG().reversePostOrder();
		for (BasicBlock bb : bblist) {
			for (Quad q : bb.getQuads()) {
				if (q.getOperator() instanceof Move &&
						((RegisterOperand) Move.getDest(q)).getRegister() == r) {
					return r.toString();
				}
			}
		}
		for (BasicBlock bb : bblist) {
			for (Quad q : bb.getQuads()) {
				if (q.getOperator() instanceof Phi) {
					Register rd = ((RegisterOperand) Phi.getDest(q)).getRegister();
					if (r == rd) {
						Register r1 = ((RegisterOperand) Phi.getSrc(q,0)).getRegister();
						String s1 = getVarFromReg(m,r1);
						if (s1.substring(0,1).equals("R")) {
							Register r2 = ((RegisterOperand) Phi.getSrc(q,1)).getRegister();
							String s2 = getVarFromReg(m,r2);
							return s2;
						} else return s1;
					}
				}
			}
		}
		return null;
	}

	private static boolean hasName(jq_Method m, Register r) {
		return (!(RegisterManager.getRegName(m,r).get(0).equals(r.toString())));
	}
	
}
