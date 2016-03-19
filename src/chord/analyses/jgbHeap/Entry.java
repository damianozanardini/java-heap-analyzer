package chord.analyses.jgbHeap;

import chord.analyses.alias.Ctxt;
import joeq.Class.jq_Method;

/**
 * Esta clase representa lo que se le pasa al an�lisis cuando hay que analizar
 * un m�todo (es decir, la clase HeapMethod en realidad deber�a ser HeapEntry).
 * 
 * @author damiano
 *
 */
public class Entry {

	jq_Method method;
	Ctxt context;
	
	// Context information to be added

	public Entry(jq_Method m,Ctxt c) {
		method = m;
		context = c;
	}
	
	public jq_Method getMethod() {
		return method;
	}
	
}
