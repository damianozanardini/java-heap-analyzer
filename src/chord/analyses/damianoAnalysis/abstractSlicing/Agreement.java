package chord.analyses.damianoAnalysis.abstractSlicing;

import java.util.Enumeration;
import java.util.Hashtable;

import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.damianoAnalysis.oldCyclicity.FSet;
import chord.analyses.damianoAnalysis.oldCyclicity.RelShare;
import chord.bddbddb.Rel.QuadIterable;
import chord.project.ClassicProject;
import chord.util.tuple.object.Trio;

import joeq.Compiler.Quad.RegisterFactory.Register;

public class Agreement extends Hashtable<Register,AbstractValue>{

	// TODO for the moment, only the nullity domain is supported, so that
	// there is no need to specify the domain
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Agreement() {
		super();
	}
	
	public Agreement(Register r,AbstractValue av) {
		super();
		put(r,av);
	}
	
	public boolean containsPair(Register r,AbstractValue av) {
		AbstractValue av0 = get(r);
		if (av0 != null) {
			return (get(r).getPartition() == av.getPartition());
		} else {
			return false;
		}
	}
		
	public boolean lub(Register r,AbstractValue av) {
		if (av == null) {
			return false;
		} else {
			if (containsKey(r)) {
				return get(r).lub(av);
			} else {
				put(r,av);
				return (!av.isTop());
			}
		}
	}
	
	/**
	 * This method returns a cloned version of an agreement, where the
	 * registers are not cloned (they are still the same objects), but abstract
	 * values are cloned 
	 */
	public Agreement clone() {
		Agreement acopy = new Agreement();
		Enumeration<Register> keys = keys();
		while (keys.hasMoreElements()) {
			Register r = keys.nextElement();
			AbstractValue av = get(r);
			AbstractValue avc = av.clone();
			acopy.put(r,avc);
		}
		return acopy;
	}
	
	public void showMe() {
		Utilities.debug(toString());
	}
	
	public String toString() {
		String acc = "";
		Enumeration<Register> keys = keys();
		while (keys.hasMoreElements()) {
			Register r = keys.nextElement();
			acc = acc + "Q ON " + r + " = " + get(r) + "; ";
		}
		return acc;
	}

	/**
	 * The least upper bound changes the internal data of the first objects
	 * instead of creating a new object
	 * 
	 * @param that The second argument of the lub
	 * @return whether the lub is different from (greater than) the first argument
	 */
	public boolean lub(Agreement that) {
		boolean x = false;
		Enumeration<Register> keys = keys();
		while (keys.hasMoreElements()) {
			Register r = keys.nextElement();
			AbstractValue avother = that.get(r);
			if (avother != null) {
				x |= get(r).lub(avother);
			}
		}
		keys = that.keys();
		while (keys.hasMoreElements()) {
			Register r = keys.nextElement();
			if (get(r) == null) {
				put(r,that.get(r).clone()); // if av1!=null then the lub has been computed in the first loop
				x = true;
			}
		}
		return x;
	}
	
}
