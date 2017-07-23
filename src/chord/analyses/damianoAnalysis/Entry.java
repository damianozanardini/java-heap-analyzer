package chord.analyses.damianoAnalysis;

import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.Descriptor.Iterator;

import chord.analyses.alias.Ctxt;
import chord.analyses.damianoAnalysis.sharingCyclicity.GlobalInfo;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * This class is in charge of entries; an entry is a method with a context.
 * 
 * @author damiano
 *
 */
public class Entry {

	protected jq_Method method;
	protected Ctxt context;
	protected Quad callSite;
	protected ArrayList<Register> referenceRegisters;
	protected ArrayList<Register> registers;
	
	// Context information to be added

	public Entry(jq_Method m, Ctxt c, Quad cs) {
		method = m;
		context = c;
		callSite = cs;
		createReferenceRegisterList();
		createRegisterList();
	}
	
	public jq_Method getMethod() {
		return method;
	}
	
	public Ctxt getContext() {
		return context;
	}
	
	public Quad getCallSite(){
		return callSite;
	}
	
	/**
	 * Returns the number of registers in this Entry, including temporary
	 * and ghost registers. The order is given by RegisterFactory, not by
	 * lexicographic order.
	 * 
	 * @return the total number of registers
	 */
	public int getNumberOfReferenceRegisters() {
		return referenceRegisters.size();
	}
	
	/**
	 * Returns the list of registers (including ghost registers), maintaining
	 * the order given by RegisterFactory
	 * 
	 * @return the list of registers
	 */
	private void createReferenceRegisterList() {
		ArrayList<Register> list = new ArrayList<Register>();
		RegisterFactory rf = getMethod().getCFG().getRegisterFactory();
		java.util.Iterator<Register> it = (java.util.Iterator<Register>) rf.iterator();
		while (it.hasNext()) {
			Register r = (Register) it.next();
			if (!r.getType().isPrimitiveType()) list.add(r);
		}
		Utilities.info("XXX : " + list);
		referenceRegisters = list;
	}

	public ArrayList<Register> getReferenceRegisters() {
		return referenceRegisters;
	}
	
	private void createRegisterList() {
		ArrayList<Register> list = new ArrayList<Register>();
		RegisterFactory rf = getMethod().getCFG().getRegisterFactory();
		java.util.Iterator<Register> it = (java.util.Iterator<Register>) rf.iterator();
		while (it.hasNext()) {
			Register r = (Register) it.next();
			list.add(r);
		}
		registers = list;
	}

	/**
	 * Returns the n-th reference register in the list, without checking for bounds.
	 * 
	 * @param n
	 * @return
	 */
	public Register getNthReferenceRegister(int n) {
		return referenceRegisters.get(n);
	}
	
	public Register getNthRegister(int n) {
		return registers.get(n);
	}

	/**
	 * Returns the index of Reference Register r in the register
	 * list of this Entry, or -1 if not found
	 * 
	 * @param r
	 * @return
	 */
	public int getReferenceRegisterPos(Register r) {
		return referenceRegisters.indexOf(r);
	}
	
	public ArrayList<Register> getReferenceFormalParameters() {
		ArrayList<Register> list = new ArrayList<Register>();
		if (callSite != null) {
			int nPars = Invoke.getParamList(callSite).length();
			for (int i=0; i<nPars; i++) {
				Register r = getNthRegister(i);
				if (!r.getType().isPrimitiveType()) list.add(r);
			}
		} else { // it should only be the case of main
			list.add(getNthReferenceRegister(0)); // R0 is the String[] parameter of main
		}
		Utilities.info("REFERENCE FORMAL PARAMETERS OF " + this + "= " + list);
		return list;
	}
	
	/**
	 * Returns the entries calling this. In the absence of recursion, a caller is the entry
	 * corresponding to the method which contains the callSite Quad. However, if a method A
	 * has two recursive calls B1 and B2 to itself, the only caller of B2 should be A (neither
	 * B1 nor B2 itself). This is obtained by checking that the caller's caller is not the same
	 * method.
	 *  
	 * @return
	 */
	// WARNING: it seems that each entry corresponds to at most one call-site
	public ArrayList<Entry> getCallers() {
		if (callSite == null) return new ArrayList<Entry>();
		jq_Method m = callSite.getMethod();
		ArrayList<Entry> list = new ArrayList<Entry>();
		for (Entry e : GlobalInfo.getEntryManager().getEntriesFromMethod(m)) {
			Quad callersCaller = e.getCallSite();
			if (callersCaller == null) list.add(e);
			else if (callersCaller.getMethod() != method) list.add(e);
		}
		return list;
	}
	
	public String toString() {
		return "< " + method + " / " + context + " / " + callSite + " >";
	}
	
	public boolean isTheMain() {
		return (callSite==null);
	}
}
