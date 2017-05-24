package chord.analyses.damianoAnalysis.mgb;

import chord.analyses.alias.Ctxt;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

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
	
	public String toString() {
		return "< " + method + " / " + context + " / " + callSite + " >";
	}
}
