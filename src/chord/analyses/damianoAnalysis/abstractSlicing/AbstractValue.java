package chord.analyses.damianoAnalysis.abstractSlicing;

public class AbstractValue {

	public static final int TOP = 0;
	public static final int ID = 1;

	int partition;
	
	public AbstractValue(int i) {
		partition = i;
	}
	
	public int getPartition() {
		return partition;
	}
	
	public boolean isTop() {
		return (partition == TOP);
	}

	public boolean isId() {
		return (partition == ID);
	}

	public void setAbsType(int at) {
		partition = at;
	}
	
	/**
	 * The least upper bound changes the internal data of the first objects
	 * instead of creating a new object
	 * 
	 * @param av The second argument of the lub
	 * @return whether the lub is different from (greater than) the first argument 
	 */
	public boolean lub(AbstractValue av) {
		int partition0 = partition;
		int pt = av.getPartition();
		partition = (pt == ID || partition == ID) ? ID : TOP;
		return (partition != partition0);
	}

	/**
	 * The use of isFinerEq with the "arguments" swapped permits to call the
	 * comparison method of the subclass, instead of the AbstractValue one 
	 * 
	 * @param abstractValue
	 * @return
	 */
	public boolean isCoarserEq(AbstractValue abstractValue) {
		if (isTop()) return true;
		if (abstractValue.isId()) return true;
		return abstractValue.isFinerEq(this);
	}
	
	public boolean isFinerEq(AbstractValue abstractValue) {
		if (abstractValue.isTop()) return true;
		if (isId()) return true;
		return false;
	}
	
	public String toString() {
		if (partition == TOP) return "TOP";
		else if (partition == ID) return "ID";
		else return "not_a_value";
	}
	
	public AbstractValue clone() {
		return new AbstractValue(partition);
	}

	public AbstractValue buildId() {
		return new AbstractValue(ID);
	}

	public boolean isSensitiveToSharing() {
		return isId();
	}

}
