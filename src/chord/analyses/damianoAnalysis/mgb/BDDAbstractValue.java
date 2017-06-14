package chord.analyses.damianoAnalysis.mgb;

import java.util.List;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;


public class BDDAbstractValue extends AbstractValue {
    protected BDDFactory factory;
	private BDD sComp;
	private BDD cComp;
	private BDDDomain SDomain;
	
	private int nRegisters;
	private int nFields;
	private List<Register> registerList;
	private int registerSize;
	private int fieldSize;
	//[firstReg, secondReg, firstField, secondField]
	private int[] bitOffsets;


	private void printInfo(){
		Utilities.debugMGB("==BDD AV INFO==");
		Utilities.debugMGB("No. Registers: " + this.getNRegisters());
		Utilities.debugMGB("Reg. List " + this.getRegisterList());
		Utilities.debugMGB("No. Fields " + this.getNFields());
		Utilities.debugMGB("SDomain" + this.SDomain);
	}
	
	void setBitOffsets() {
		// initialized to something to avoid failing if nRegisters or nFields are null
		bitOffsets = new int[]{0,1,1,1};
		// registers
		long nbitsRegs = (long) (Math.ceil(Math.log(nRegisters) / Math.log(2)));
		this.registerSize = 1 << nbitsRegs;
		this.bitOffsets[0] = 0;
		this.bitOffsets[1] = registerSize;
		// fields
		long nbitsFields = (long) (Math.ceil(Math.log(nFields) / Math.log(2)));
		this.fieldSize = 1 << nbitsFields;
		this.bitOffsets[2] = bitOffsets[1] * bitOffsets[1];
		this.bitOffsets[3] = bitOffsets[2] * fieldSize;
		
	}

	
	private void initFactory(Entry entry){
		// TODO anyway to guess this number?
		int numberOfVariables = 1000;
		/* Loading information from entry about registers and fields */
		this.setNRegisters(entry.getNumberOfRegisters());
		this.setNFields(GlobalInfo.getNumberOfFields());
		this.setRegisterList(entry.getRegisterList());
		
		factory = BDDFactory.init("java",1000, 1000);
		factory.setVarNum(numberOfVariables);
	}
	
	private void initDomain(){
		int sizeExtDomain = registerSize * registerSize * fieldSize * fieldSize;
		SDomain = this.factory.extDomain(sizeExtDomain);

	}
	
	public BDDAbstractValue(Entry entry) {
		this.initFactory(entry);
		this.setBitOffsets();
		this.initDomain(); 
		//this.printInfo();
		this.sComp = factory.zero();
		this.cComp = factory.zero();
	}
	
	protected BDDAbstractValue(BDD sc,BDD cc) {
		sComp = sc;
		cComp = cc;
	}

	@Override
	public boolean update(AbstractValue other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public AbstractValue clone() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addSinfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		//TODO MGB OJO QUE ES UN FIELDSET!!!!
		BDD newSEntry = this.SDomain.ithVar(
				r1.getNumber() + 
				r2.getNumber() * bitOffsets[1] + 
				fs1.getVal() * bitOffsets[2] + 
				fs1.getVal() * bitOffsets[3]);		
		r1.getNumber();

	}

	@Override
	public void addCinfo(Register r, FieldSet fs) {
		// TODO Auto-generated method stub

	}

	@Override
	public void copyInfo(Register source, Register dest) {
		// TODO Auto-generated method stub

	}

	@Override
	public void copySinfo(Register source, Register dest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void copyCinfo(Register source, Register dest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveInfo(Register source, Register dest) {
		// TODO Auto-generated method stub

	}

	@Override
	public void moveSinfo(Register source, Register dest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveCinfo(Register source, Register dest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveInfoList(List<Register> source, List<Register> dest) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeInfo(Register r) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeInfoList(List<Register> rs) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actualToFormal(List<Register> apl, jq_Method m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void formalToActual(List<Register> apl, jq_Method m) {
		// TODO Auto-generated method stub

	}

	
	@Override
	public void cleanGhostRegisters(Entry entry) {
		// TODO Auto-generated method stub

	}

	@Override
	public void filterActual(List<Register> actualParameters) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Pair<FieldSet, FieldSet>> getSinfo(Register r1, Register r2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Pair<Register, FieldSet>> getSinfoReachingRegister(Register r) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Pair<Register, FieldSet>> getSinfoReachedRegister(Register r) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FieldSet> getSinfoReachingReachedRegister(Register r1, Register r2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Trio<Register, FieldSet, FieldSet>> getSinfoFirstRegister(Register r) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Trio<Register, FieldSet, FieldSet>> getSinfoSecondRegister(Register r) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FieldSet> getCinfo(Register r) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isBottom() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void copyFromCycle(Register base, Register dest) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void copyToGhostRegisters(Entry entry) {
		// TODO Auto-generated method stub
		
	}

	/* Getters and setters */
	
	/**
	 * @return the sComp
	 */
	protected BDD getsComp() {
		return sComp;
	}

	/**
	 * @param sComp the sComp to set
	 */
	protected void setsComp(BDD sComp) {
		this.sComp = sComp;
	}

	/**
	 * @return the cComp
	 */
	protected BDD getcComp() {
		return cComp;
	}

	/**
	 * @param cComp the cComp to set
	 */
	protected void setcComp(BDD cComp) {
		this.cComp = cComp;
	}

	/**
	 * @return the numberOfRegisters
	 */
	public int getNRegisters() {
		return nRegisters;
	}

	/**
	 * @param numberOfRegisters the numberOfRegisters to set
	 */
	public void setNRegisters(int numberOfRegisters) {
		this.nRegisters = numberOfRegisters;
	}

	/**
	 * @return the numberOfFields
	 */
	public int getNFields() {
		return nFields;
	}

	/**
	 * @param numberOfFields the numberOfFields to set
	 */
	public void setNFields(int numberOfFields) {
		this.nFields = numberOfFields;
	}

	/**
	 * @return the registerList
	 */
	public List<Register> getRegisterList() {
		return registerList;
	}

	/**
	 * @param registerList the registerList to set
	 */
	public void setRegisterList(List<Register> registerList) {
		this.registerList = registerList;
	}

	/**
	 * @return the sDomain
	 */
	protected BDDDomain getSDomain() {
		return SDomain;
	}

	/**
	 * @param sDomain the sDomain to set
	 */
	protected void setSDomain(BDDDomain sDomain) {
		SDomain = sDomain;
	}

	/**
	 * @return the nRegisters
	 */
	protected int getnRegisters() {
		return nRegisters;
	}

	/**
	 * @param nRegisters the nRegisters to set
	 */
	protected void setnRegisters(int nRegisters) {
		this.nRegisters = nRegisters;
	}

	/**
	 * @return the nFields
	 */
	protected int getnFields() {
		return nFields;
	}

	/**
	 * @param nFields the nFields to set
	 */
	protected void setnFields(int nFields) {
		this.nFields = nFields;
	}

	/**
	 * @return the registerSize
	 */
	protected int getRegisterSize() {
		return registerSize;
	}

	/**
	 * @param registerSize the registerSize to set
	 */
	protected void setRegisterSize(int registerSize) {
		this.registerSize = registerSize;
	}

	/**
	 * @return the fieldSize
	 */
	protected int getFieldSize() {
		return fieldSize;
	}

	/**
	 * @param fieldSize the fieldSize to set
	 */
	protected void setFieldSize(int fieldSize) {
		this.fieldSize = fieldSize;
	}

	/**
	 * @return the bitOffsets
	 */
	protected int[] getBitOffsets() {
		return bitOffsets;
	}

	/**
	 * @param bitOffsets the bitOffsets to set
	 */
	protected void setBitOffsets(int[] bitOffsets) {
		this.bitOffsets = bitOffsets;
	}
}
