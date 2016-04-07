package chord.analyses.jgbHeap;

import chord.analyses.alias.Ctxt;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

/**
 * Esta clase representa lo que se le pasa al análisis cuando hay que analizar
 * un método (es decir, la clase HeapMethod en realidad debería ser HeapEntry).
 * 
 * @author damiano
 *
 */
public class Entry {

	jq_Method method;
	Ctxt context;
	Quad callSite;
	
	// Context information to be added

	public Entry(jq_Method m,Ctxt c,Quad cs) {
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
}
