package chord.analyses.jgbHeap;

public class AbstractValue {

	private STuples sComp;
	private CTuples cComp;
	
	public AbstractValue() {
		sComp = new STuples();
		cComp = new CTuples();
	}
	
	public boolean update(AbstractValue other) {
		boolean s = sComp.join(other.getSComp());
		boolean c = cComp.join(other.getCComp());
			return ( c || s);
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
	
	
	public void setSComp(STuples stuples){
		this.sComp = stuples;
	}
	
	public void setCComp(CTuples ctuples){
		this.cComp = ctuples;
	}
	
}
