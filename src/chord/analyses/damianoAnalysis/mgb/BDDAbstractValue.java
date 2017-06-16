package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.RegisterFactory.Register;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDD.BDDIterator;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;


public class BDDAbstractValue extends AbstractValue {
	private Entry entry;
	protected static HashMap<Entry,BDDFactory> factories = new HashMap<Entry,BDDFactory>();
	protected static HashMap<Entry,BDDDomain> domains = new HashMap<Entry,BDDDomain>();
	
	// DAMIANO: un d�a comentamos la posibilidad de crear una clase que extendiese BDD,
	// pero por lo visto no es posible: en realidad los objetos que manejamos parecen
	// ser de tipo bdd (en min�sculas), que es una clase privada de cada Factory.
	// BDD es abstracta. As� que creo que no se puede hacer nada que nos ayude mucho.
	private BDD sComp;
	private BDD cComp;
	
	// number of registers in the Entry
	private int nRegisters;
	// number of fields in the program
	private static int nFields = GlobalInfo.getNumberOfFields();
	// list of registers in the Entry
	private List<Register> registerList;
	private int registerSize;
	private int fieldSize;
	// number of bits necessary to represent nRegisters registers
	private int registerBitSize;
	// number of bits necessary to represent all the fieldsets
	private int fieldBitSize;
	
	// Representation of the number of bits to skip to get to a given value:
	// [firstReg, secondReg, firstFieldSet, secondFieldSet]
	private int[] bitOffsets;

	// DAMIANO: ll�mala c�mo quieras, pero me parece �til tener a mano el n�mero
	// total de variables del BDD
	private int nBDDVars_sh;
	private int nBDDVars_cy;	
	
    public BDDAbstractValue(Entry e, BDD sc, BDD cc) {
		this(e);
		// WARNING: this overrides the assignments in this(e); it is intended, but
		// a better way to do it could be found
    	sComp = sc;
    	cComp = cc;
	}

	/**
	 * Main constructor, creates a BDDAnstractValue object based on the
	 * entry information
	 * 
	 * @param entry the entry object needed to collect the information related
	 * to registers and fields
	 */
	public BDDAbstractValue(Entry e) {
		entry = e;

		// computing relevant numbers
		nBDDVars_sh = 0;
		nBDDVars_cy = 0;
		registerBitSize = 0;
		fieldBitSize = 0;
		/* Loading information from entry about registers and fields */
		// DAMIANO: para atributos privados como estos no s� si necesitamos getters y setters
		// DAMIANO: es verdad que podr�amos evitar llamar estos m�todos cada vez que
		// se crea un objeto
		nRegisters = entry.getNumberOfRegisters();
		registerList = entry.getRegisterList();
		// nVars increases by 2 because two registers have to be represented
		for (int i=1; i<nRegisters; i*=2) { 
			nBDDVars_sh+=2;
			nBDDVars_cy++;
			registerBitSize++;
		} 

		// nFields increases by 2 because two fieldsets have to be represented

		nBDDVars_sh += nFields*2;
		nBDDVars_cy += nFields;
		fieldBitSize = nFields;

		setBitOffsets();
		getOrCreateDomain();
		//this.printInfo();
		sComp = getOrCreateFactory(e).zero();
		cComp = getOrCreateFactory(e).zero();
	}

	/**
	 * This must be static because it is called in the constructor before the
	 * object is actually created
	 *  
	 * @param e
	 * @return
	 */
	public BDDFactory getOrCreateFactory(Entry e) {
		if (factories.containsKey(e)) return factories.get(e);
				
		// DAMIANO: no cambio estos dos "1000" porque no s� si tiene que ver
		// con el valor que pusiste para el argumento de setVarNum
		BDDFactory factory = BDDFactory.init("java",1000, 1000);
		factory.setVarNum(nBDDVars_sh);
	
		factories.put(e,factory);
		return factory;
	}

	/**
	 * sets all the offset for sharing based on the number of regs and fields
	 */
	// DAMIANO: no pasa nada, pero estos numeritos al final son f�cilmente calculables
	// sobre la marcha partiendo de nRegisters, nFields etc; pero bueno, la verdad que
	// no s� cu�l es la mejor opci�n. en todo caso, los dos bucles en getOrCreateFactory()
	// quiz� sean m�s sencillos que esto
	void setBitOffsets() {
		// initialized to something to avoid failing if nRegisters or nFields are null
		bitOffsets = new int[]{0,1,1,1};
		// registers
		long nbitsRegs = (long) (Math.ceil(Math.log(nRegisters) / Math.log(2)));
		registerSize = 1 << nbitsRegs;
		bitOffsets[0] = 0;
		bitOffsets[1] = registerSize;
		// fields
		//long nbitsFields = (long) (Math.ceil(Math.log(nFields) / Math.log(2)));
		fieldSize = 1 << nFields;
		bitOffsets[2] = bitOffsets[1] * bitOffsets[1];
		bitOffsets[3] = bitOffsets[2] * fieldSize;	
	}

	/**
	 * Initializes the BDD Domain
	 */
	private BDDDomain getOrCreateDomain() {
		if (!domains.containsKey(entry)) {
			int sizeExtDomain = registerSize * registerSize * fieldSize * fieldSize;
			domains.put(entry,getOrCreateFactory(entry).extDomain(sizeExtDomain));
		}
		return domains.get(entry);
	}
	
	@Override
	public boolean update(AbstractValue other) {
		// TODO Auto-generated method stub
		return false;
	}
	/**
	 * Returns a new BDDAbstractValue object with the same abstract information.
	 * 
	 * @return a copy of itself
	 */
	@Override
	public AbstractValue clone() {
		// DAMIANO: la copia es shallow hasta cierto punto: lo que no se duplica
		// en TuplesAbstractValue son los objetos Register y FieldSet, pero lo
		// dem�s s� se duplica
		return new BDDAbstractValue(entry, sComp.id(), cComp.id());
	}
	
	/**
	 * 
	 * Generates a new BDD describing the sharing between the registers and fieldsets provided
	 * as parameters.
	 * 
	 */
	
	@Override
	public void addSinfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		
		BDD newBDDSEntry = getOrCreateDomain().ithVar(
				registerList.indexOf(r1) + 
				registerList.indexOf(r2) * bitOffsets[1] + 
				fs1.getVal() * bitOffsets[2] + 
				fs2.getVal() * bitOffsets[3]);
		notifyBddAdded(newBDDSEntry);
		// note: newBDDSEntry is destroyed
		sComp.orWith(newBDDSEntry);
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

	// DAMIANO: modifico tu versi�n para que se vea si "pilla" correctamente los registros y fieldstes.
	// dejo tu implementaci�n por si quieres mantenerla
	// 	// Ex:[INF] OLD AV: (T3,T3,{ },{ }) / (T3,{ }) ++++ <-TUPLES / BDD-> ++++ (R0,R0,{ },{ })

	public String toString() {
		String s = "";
		BDDIterator it = sComp.iterator(varIntervalToBDD(0,nBDDVars_sh));	
		while (it.hasNext()) {
			BDD b = (BDD) it.next();
			// only the "bits" of the first register 
			BDD r1 = b.exist(varIntervalToBDD(registerBitSize,nBDDVars_sh));
			// only the "bits" of the second register 
			BDD r2 = b.exist(varIntervalToBDD(0,registerBitSize)).exist(varIntervalToBDD(2*registerBitSize,nBDDVars_sh));
			// only the "bits" of the first fieldset
			BDD fs1 = b.exist(varIntervalToBDD(0,2*registerBitSize)).exist(varIntervalToBDD(2*registerBitSize+fieldBitSize,nBDDVars_sh));
			// only the "bits" of the second fieldset 
			BDD fs2 = b.exist(varIntervalToBDD(0,2*registerBitSize+fieldBitSize));

			s = s + "(";
			int bits1 = BDDtoInt(r1,registerBitSize);
			s = s + entry.getNthRegister(bits1).toString();
			s = s + ",";
			int bits2 = BDDtoInt(r2,registerBitSize);
			s = s + entry.getNthRegister(bits2).toString();
			s = s + ",{ ";
			ArrayList<Boolean> bools1 = BDDtoBools(fs1,fieldBitSize);
			int j = 0;
			for (boolean x : bools1) {
				if (x) s = s + GlobalInfo.getNthField(j);
				j++;
			}
			s = s + "},{ ";
			ArrayList<Boolean> bools2 = BDDtoBools(fs2,fieldBitSize);
			j = 0;
			for (boolean x : bools2) {
				if (x) s = s + GlobalInfo.getNthField(j);
				j++;
			}
			s = s + "})" + (it.hasNext() ? " - " : "");
		}		
		// DAMIANO: tu implementaci�n
		// return sComp.toString()+ " / " + cComp.toString();
		return s;
	}
		
	private BDD varIntervalToBDD(int lower,int upper) {
		BDD x = getOrCreateFactory(entry).one();
		for (int i=lower; i<upper; i++) {
			x.andWith(getOrCreateFactory(entry).ithVar(i));
		}

		return x;
	}

	private BDD varListToBDD(ArrayList<Integer> list) {
		BDD x = getOrCreateFactory(entry).one();
		for (int i : list) x.andWith(getOrCreateFactory(entry).ithVar(i));
		return x;
	}

	/**
	 * This method assumes that b is a conjunction of ithVars() or nIthVars() of
	 * variables with consecutive indexes, and returns an int whose last nBits bits
	 * contains the "truth value" of each variable involved 
	 * @param b
	 * @return
	 */
	// DAMIANO: hago esto porque no s� de qu� otra manera hacerlo... a nivel de
	// performance no es problem�tico porque al fin y al cabo, si no estamos en
	// "modo debug", esto s�lo se calcula al final del todo
	private int BDDtoInt(BDD b, int nBits) {
		// DAMIANO: hay que investigar los m�todos "scan" porque creo que hacen
		// cosas �tiles
		int[] vars = b.scanSet();
		int acc = 0;
		for (int i : vars) {
			ArrayList<Integer> l = new ArrayList<Integer>();
			for (int j : vars) if (j!=i) l.add(j);
			boolean isHere = b.exist(varListToBDD(l)).restrict(getOrCreateFactory(entry).ithVar(i)).isOne();
			acc = 2*acc + (isHere? 1 : 0);
		}

		return acc;
	}
	
	private ArrayList<Boolean> BDDtoBools(BDD b, int nBits) {
		ArrayList<Boolean> bools = new ArrayList<Boolean>();
		int bits = BDDtoInt(b,nBits);
		for (int i=0; i<nBits; i++) {
			bools.add(0,bits%2==1);
			bits = bits >>> 1;
		}
		return bools;
	}
	
	static final int LEFT = 0;
	static final int RIGHT = 1;
	private BDD registerToBDD(Register r,int leftRight) {
		int id = registerList.indexOf(r);
		BDD b = getOrCreateFactory(entry).one();
		int offset = (leftRight==LEFT) ? 0 : registerBitSize;
		for (int i = offset+registerBitSize-1; i>offset; i--) {
			if (id%2 == 1) b.andWith(getOrCreateFactory(entry).ithVar(i));
			else b.andWith(getOrCreateFactory(entry).nithVar(i));
			id /= 2;
		}
		return b;
	}
	
	@Override
	public boolean isBottom() {
		return this.cComp.isZero() && this.sComp.isZero();
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
	
	private void notifyBddAdded(BDD bdd){
		Utilities.info("ADDED TO SHARE BDD: " + bdd.toString());
	}
	
	/**
	 * Used for debugging. MGB
	 */
	private void printInfo(){
		Utilities.debugMGB("==BDD AV INFO==");
		Utilities.debugMGB("No. Registers: " + nRegisters);
		Utilities.debugMGB("Reg. List " + registerList);
		Utilities.debugMGB("No. Fields " + nFields);
		Utilities.debugMGB("SDomain" + getOrCreateDomain());
	}

}
