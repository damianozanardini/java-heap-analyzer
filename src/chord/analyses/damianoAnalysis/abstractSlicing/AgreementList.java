package chord.analyses.damianoAnalysis.abstractSlicing;

import java.util.Enumeration;
import java.util.Hashtable;

import chord.analyses.damianoAnalysis.Utilities;

import joeq.Compiler.Quad.Quad;

public class AgreementList extends Hashtable<Quad,Agreement> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public AgreementList() { }
	
	public boolean update(Quad q,Agreement a) {
		if (containsKey(q)) {
			return get(q).lub(a);
		} else {
			put(q,a.clone());
			return true;
		}
	}
	
	public void showMe() {
		Utilities.debug("  CURRENT AGREEMENT LIST: ");
		Enumeration<Quad> keys = keys();
		while (keys.hasMoreElements()) {
			Quad q = (Quad) keys.nextElement();
			Utilities.debug("    " + q + " ------> " + get(q).toString());
		}
	}
	
}
