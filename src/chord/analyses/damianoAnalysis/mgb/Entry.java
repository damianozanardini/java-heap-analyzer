package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.List;

import javassist.bytecode.Descriptor.Iterator;

import chord.analyses.alias.Ctxt;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * Esta clase representa lo que se le pasa al an�lisis cuando hay que analizar
 * un m�todo (es decir, la clase HeapMethod en realidad deber�a ser HeapEntry).
 * 
 * @author damiano
 *
 */
public class Entry {

	protected jq_Method method;
	protected Ctxt context;
	protected Quad callSite;
	
	// Context information to be added

	public Entry(jq_Method m, Ctxt c, Quad cs) {
		method = m;
		context = c;
		callSite = cs;
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
	 * Returns the number of registers in this Entry, including temporary and ghost registers.
	 * The order is given by RegisterFactory, not by lexicographic order.
	 * 
	 * @return the total number of registers
	 */
	public int getNumberOfRegisters() {
		RegisterFactory rf = getMethod().getCFG().getRegisterFactory();
		return rf.size();
	}
	
	/**
	 * Returns the list of registers, maintaining the order given by RegisterFactory
	 * 
	 * @return the list of registers
	 */
	public List<Register> getRegisterList() {
		ArrayList<Register> list = new ArrayList<Register>();
		RegisterFactory rf = getMethod().getCFG().getRegisterFactory();
		java.util.Iterator<Register> it = (java.util.Iterator<Register>) rf.iterator();
		while (it.hasNext()) list.add((Register) it.next());
		return list;
	}

	/**
	 * Returns the n-th register in the list, without checking for bounds.
	 * 
	 * @param n
	 * @return
	 */
	public Register getNthRegister(int n) {
		return getMethod().getCFG().getRegisterFactory().get(n);
	}
	
	/**
	 * Returns the index of Register r in the register list of this Entry, or -1 if not found
	 * 
	 * @param r
	 * @return
	 */
	public int getRegisterPos(Register r) {
		RegisterFactory rf = getMethod().getCFG().getRegisterFactory();
		for (int i=0; i<rf.size(); i++) {
			if (rf.get(i) == r) return i;
		}
		return -1;
	}
	
	// WARNING: it seems that each entry corresponds to at most one call-site
	public ArrayList<Entry> getCallers() {
		if (callSite == null) return new ArrayList<Entry>();
		return GlobalInfo.getEntryManager().getEntriesFromMethod(callSite.getMethod());
	}
	
	public String toString() {
		return "< " + method + " / " + context + " / " + callSite + " >";
	}
	
	public boolean isTheMain() {
		return (callSite==null);
	}
}
