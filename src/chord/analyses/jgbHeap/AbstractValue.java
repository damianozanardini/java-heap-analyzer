package chord.analyses.jgbHeap;

public class AbstractValue {

	private STuples sComp;
	private CTuples cComp;
	
	public AbstractValue() {
		sComp = new STuples();
		cComp = new CTuples();
	}
	
	public boolean update(AbstractValue other) {
		return (sComp.join(other.getSComp()) || cComp.join(other.getCComp()));
	}
	
	/**
	 * Returns the sharing component of the abstract value
	 * 
	 * TO-DO
	 * @return
	 */	
	public STuples getSComp() {
		return sComp;
	}
	
	/**
	 * Returns the cyclicity component of the abstract value
	 * 
	 * TO-DO
	 * @return
	 */
	public CTuples getCComp() {
		return cComp;
	}

}
