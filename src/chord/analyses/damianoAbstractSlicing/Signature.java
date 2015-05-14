package chord.analyses.damianoAbstractSlicing;

import java.util.Enumeration;
import java.util.Hashtable;

import joeq.Compiler.Quad.RegisterFactory.Register;

public class Signature extends Hashtable<Register,AbstractValue>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Signature() {
		super();
	}
	
	public AbstractValue preserves(Register r) {
		if (contains(r)) {
			return get(r);
		} else {
			return new AbstractValue(AbstractValue.ID);
		}		
	}
	
	public boolean isInvariant(Agreement a) {
		boolean x = true;
		Enumeration<Register> rlist = keys();
		while (rlist.hasMoreElements()) {
			Register r = rlist.nextElement();
			AbstractValue av1 = get(r); // av1 is never null
			AbstractValue av2 = a.get(r);
			if (av2 != null) {
				x &= av2.isCoarserEq(av1);
			}
		}
		return x;
	}
	

}
