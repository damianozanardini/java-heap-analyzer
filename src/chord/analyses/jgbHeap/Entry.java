package chord.analyses.jgbHeap;

import chord.analyses.alias.Ctxt;
import joeq.Class.jq_Method;

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
	
	// Context information to be added

	public Entry(jq_Method m,Ctxt c) {
		method = m;
		context = c;
	}
	
	public jq_Method getMethod() {
		return method;
	}
	
}
