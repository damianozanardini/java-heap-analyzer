package chord.analyses.damianoAbstractSlicing;

public class Nullity extends AbstractValue {
	
	public static final int NULL = 2;
	
	public Nullity(int i) {
		super(i);
	}

	public boolean lub(AbstractValue av) {
		int partition0 = partition;
		int pt = av.getPartition();
		if (av.isId() || this.isId()) {
			partition = ID;
		} else {
			if (partition == NULL || pt == NULL) {
				partition = NULL;
			} else partition = TOP;
		}
		return (partition != partition0);
	}

	public String toString() {
		if (isTop() || isId()) return super.toString();
		else return "NULL?";
	}
	
	public boolean isNull() {
		return (partition == NULL);
	}
	
	public boolean isCoarserEq(Nullity abstractValue) {
		if (isTop()) return true;
		if (isNull() && (abstractValue.isId() || abstractValue.isNull())) return true;
		if (isId() && abstractValue.isId()) return true;
		return false;
	}
	
	public boolean isCoarserEq(AbstractValue abstractValue) {
		if (isTop()) return true;
		if (isNull() && abstractValue.isId()) return true;
		if (isId() && abstractValue.isId()) return true;
		return false;
	}

	public boolean isFinerEq(Nullity abstractValue) {
		if (abstractValue.isTop()) return true;
		if (isId()) return true;
		if (abstractValue.isNull() && isNull()) return true;
		return false;
	}

	public boolean isFinerEq(AbstractValue abstractValue) {
		if (abstractValue.isTop()) return true;
		if (isId()) return true;
		return false;
	}
	
	public Nullity clone() {
		return new Nullity(partition);
	}

	public Nullity buildId() {
		return new Nullity(ID);
	}
	
	/**
	 * In the nullity abstract domain, sharing only has to be taken into
	 * account if the question is about the "concrete" value of a variable v,
	 * since such a value can be affected by modifications to variables sharing
	 * with v.
	 * On the other hand, no variables sharing with v can affect the nullity of
	 * v, so that neither the question about nullity nor the "top" one need to
	 * consider sharing information. 
	 */
	public boolean isSensitiveToSharing() {
		return isId();
	}

	
}
