package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;
import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDD.BDDIterator;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;


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
		registerBitSize = 0;
		fieldBitSize = 0;
		/* Loading information from entry about registers and fields */
		// DAMIANO: para atributos privados como estos no s� si necesitamos getters y setters
		// DAMIANO: es verdad que podr�amos evitar llamar estos m�todos cada vez que
		// se crea un objeto
		nRegisters = entry.getNumberOfRegisters();
		registerList = entry.getRegisterList();
		for (int i=1; i<nRegisters; i*=2) { registerBitSize++; } 
		fieldBitSize = nFields;
		nBDDVars_sh = 2*registerBitSize + 2*fieldBitSize;
		nBDDVars_cy = registerBitSize + fieldBitSize;

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
	private BDDFactory getOrCreateFactory(Entry e) {
		if (factories.containsKey(e)) return factories.get(e);
				
		// DAMIANO: no cambio estos dos "1000" porque no s� si tiene que ver
		// con el valor que pusiste para el argumento de setVarNum
		BDDFactory factory = BDDFactory.init("java",1000, 1000);
		factory.setVarNum(nBDDVars_sh);
	
		factories.put(e,factory);
		return factory;
	}

	/**
	 * Initializes the BDD Domain
	 */
	private BDDDomain getOrCreateDomain() {
		if (!domains.containsKey(entry)) {
			long sizeExtDomain = 1 << nBDDVars_sh;
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
	 * Generates a new BDD describing the sharing between the registers and
	 * fieldsets provided as parameters, and "adds" it (by logical disjunction)
	 * to the existing information
	 */
	// DAMIANO: creo que he dado la vuelta a la secuencia de bit... ten paciencia
	public void addSinfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		long bitsReversed = fs2.getVal() + 
				(fs1.getVal() << fieldBitSize) +
				(registerList.indexOf(r1) << (2*fieldBitSize)) +
				(registerList.indexOf(r2) << (2*fieldBitSize+registerBitSize));
		// reversing the bit list
		long bits = 0;
		for (int i=0; i<nBDDVars_sh; i++) {
			int lastBit = (int) bitsReversed % 2;
			bitsReversed /= 2;
			bits = bits*2 + lastBit;
		}
		BDD newBDDSEntry = getOrCreateDomain().ithVar(bits);
				//registerList.indexOf(r1) + 
				//registerList.indexOf(r2) * bitOffsets[1] + 
				//fs1.getVal() * bitOffsets[2] + 
				//fs2.getVal() * bitOffsets[3]);
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
		copySinfo(source,dest);
		copyCinfo(source,dest);
	}

	// WARNING: from (source,source,fs1,fs2) both (dest,source,fs1,fs2) and 
	// (source,dest,fs1,fs2) are produced; this is redundant
	public void copySinfo(Register source, Register dest) {
		BDD copy = sComp.id();
		// both parts: from (source,source,fs1,fs2) to (dest,dest,fs1,fs2)
		BDD sourceBDD = registerToBDD(source,LEFT).and(registerToBDD(source,RIGHT));
		BDD aboutSource = copy.and(sourceBDD);
		BDD quantified = aboutSource.exist(varIntervalToBDD(0,2*registerBitSize));
		BDD destBDD = registerToBDD(dest,LEFT).and(registerToBDD(dest,RIGHT));
		BDD aboutDestBoth = quantified.and(destBDD);
		// left part: from (source,other,fs1,fs2) to (dest,other,fs1,fs2)
		sourceBDD = registerToBDD(source,LEFT);
		aboutSource = copy.and(sourceBDD);
		quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize));
		destBDD = registerToBDD(dest,LEFT);
		BDD aboutDestLeft = quantified.and(destBDD);
		// right part: from (other,source,fs1,fs2) to (other,dest,fs1,fs2)
		sourceBDD = registerToBDD(source,RIGHT);
		aboutSource = copy.and(sourceBDD);
		quantified = aboutSource.exist(varIntervalToBDD(registerBitSize,2*registerBitSize));
		destBDD = registerToBDD(dest,RIGHT);
		BDD aboutDestRight = quantified.and(destBDD);
		sComp.orWith(aboutDestBoth).orWith(aboutDestLeft).orWith(aboutDestRight); 
	}

	@Override
	public void copyCinfo(Register source, Register dest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void moveInfo(Register source, Register dest) {
		// TODO Auto-generated method stub

	}

	public void moveSinfo(Register source, Register dest) {
		BDD copy = sComp.id();
		// both parts: from (source,source,fs1,fs2) to (dest,dest,fs1,fs2)
		BDD sourceBDD = registerToBDD(source,LEFT).and(registerToBDD(source,RIGHT));
		BDD rest = copy.and(sourceBDD.not());
		BDD aboutSource = copy.and(sourceBDD);
		BDD quantified = aboutSource.exist(varIntervalToBDD(0,2*registerBitSize));
		BDD destBDD = registerToBDD(dest,LEFT).and(registerToBDD(dest,RIGHT));
		BDD aboutDestBoth = quantified.and(destBDD);
		// left part: from (source,other,fs1,fs2) to (dest,other,fs1,fs2)
		sourceBDD = registerToBDD(source,LEFT);
		aboutSource = copy.and(sourceBDD);
		quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize));
		destBDD = registerToBDD(dest,LEFT);
		BDD aboutDestLeft = quantified.and(destBDD);
		// right part: from (other,source,fs1,fs2) to (other,dest,fs1,fs2)
		sourceBDD = registerToBDD(source,RIGHT);
		aboutSource = copy.and(sourceBDD);
		quantified = aboutSource.exist(varIntervalToBDD(registerBitSize,2*registerBitSize));
		destBDD = registerToBDD(dest,RIGHT);
		BDD aboutDestRight = quantified.and(destBDD);
		sComp = rest.orWith(aboutDestBoth).orWith(aboutDestLeft).orWith(aboutDestRight); 
	}

	@Override
	public void moveCinfo(Register source, Register dest) {
		// TODO Auto-generated method stub
		
	}

	public void moveInfoList(List<Register> source, List<Register> dest) {
		for (int i=0; i<source.size(); i++)
			moveInfo(source.get(i),dest.get(i));
	}

	public void removeInfo(Register r) {
		removeSinfo(r);
		removeCinfo(r);
	}

	private void removeSinfo(Register r) {
		sComp.andWith(registerToBDD(r,LEFT).or(registerToBDD(r,RIGHT)).not());
	}
	
	private void removeCinfo(Register r) {
		// TODO Auto-generated method stub
	}

	public void removeInfoList(List<Register> rs) {
		for (Register r : rs) removeInfo(r);
	}

	public void actualToFormal(List<Register> apl, jq_Method m) {
		Utilities.begin("ACTUAL " + apl + " TO FORMAL FROM " + this);
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.size(); i++) {
			try {
				dest.add(RegisterManager.getRegFromNumber(m,i));
				source.add(apl.get(i));
			} catch (IndexOutOfBoundsException e) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		moveInfoList(source,dest);
		Utilities.end("ACTUAL " + apl + " TO FORMAL RESULTING IN " + this);
	}

	public void formalToActual(List<Register> apl, jq_Method m) {
		Utilities.begin("FORMAL FROM " + this + " TO ACTUAL "+ apl);
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.size(); i++) {
			try {
				source.add(RegisterManager.getRegFromNumber(m,i));
				dest.add(apl.get(i));
			} catch (IndexOutOfBoundsException e) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		moveInfoList(source,dest);
		Utilities.end("FORMAL TO ACTUAL " + apl + " RESULTING IN " + this);
	}
	
	@Override
	public void cleanGhostRegisters(Entry entry) {
		// TODO Auto-generated method stub

	}

	@Override
	public void filterActual(List<Register> actualParameters) {
		// TODO Auto-generated method stub

	}

	private BDD getSinfo(Register r1, Register r2) {
		BDD bdd1 = registerToBDD(r1,LEFT);
		BDD bdd2 = registerToBDD(r2,RIGHT);
		return sComp.and(bdd1.and(bdd2));
	}

	private List<Pair<Register, FieldSet>> getSinfoReachingRegister(Register r) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<Pair<Register, FieldSet>> getSinfoReachedRegister(Register r) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<FieldSet> getSinfoReachingReachedRegister(Register r1, Register r2) {
		// TODO Auto-generated method stub
		return null;
	}

	private BDD getSinfoFirstRegister(Register r) {
		BDD rbdd = registerToBDD(r,LEFT);
		BDD result = sComp.and(rbdd);
		return result;
	}

	private BDD getSinfoSecondRegister(Register r) {
		BDD rbdd = registerToBDD(r,RIGHT);
		BDD result = sComp.and(rbdd);
		return result;
	}

	private List<FieldSet> getCinfo(Register r) {
		// TODO Auto-generated method stub
		return null;
	}

	private void printLines() {
		Utilities.begin("PRINTING BDD SOLUTIONS");
		BDD toIterate = varIntervalToBDD(0,nBDDVars_sh);
		BDDIterator it = sComp.iterator(toIterate);	
		while (it.hasNext()) {
			BDD b = (BDD) it.next();
			Utilities.info(b.toString());
		}
		Utilities.end("PRINTING BDD SOLUTIONS");
	}
	
	public String toString() {
		String s = "";
		BDD toIterate = varIntervalToBDD(0,nBDDVars_sh);
		BDDIterator it = sComp.iterator(toIterate);	
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
			int bits1 = BDDtoInt(r1,0,registerBitSize);
			s = s + entry.getNthRegister(bits1).toString();
			s = s + ",";
			int bits2 = BDDtoInt(r2,registerBitSize,2*registerBitSize);

			s = s + entry.getNthRegister(bits2).toString();
			s = s + ",{ ";
			ArrayList<Boolean> bools1 = BDDtoBools(fs1,2*registerBitSize,2*registerBitSize+fieldBitSize);
			int j = 0;
			for (boolean x : bools1) {
				if (x) s = s + GlobalInfo.getNthField(j);
				j++;
			}
			s = s + "},{ ";
			ArrayList<Boolean> bools2 = BDDtoBools(fs2,2*registerBitSize+fieldBitSize,nBDDVars_sh);
			j = 0;
			for (boolean x : bools2) {
				if (x) s = s + GlobalInfo.getNthField(j);
				j++;
			}
			s = s + "})" + (it.hasNext() ? " - " : "");
		}		
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
	private int BDDtoInt(BDD b, int lower, int upper) {
		// DAMIANO: hay que investigar los m�todos "scan" porque creo que hacen
		// cosas �tiles
		int[] vars = b.support().scanSet();
		
		// i need the reversed array, I'd use ArrayUtils but Apache commons is not installed
		int temp;
		for (int i = 0; i < vars.length/2; i++)
		  {
		     temp = vars[i];
		     vars[i] = vars[vars.length-1 - i];
		     vars[vars.length-1 - i] = temp;
		  }
		int acc = 0;
		for (int i=lower; i<upper; i++) {
			ArrayList<Integer> l = new ArrayList<Integer>();
			for (int j=lower; j<upper; j++) if (j!=i) l.add(j);
			boolean isHere = b.exist(varListToBDD(l)).restrict(getOrCreateFactory(entry).ithVar(i)).isOne();
			acc = 2*acc + (isHere? 1 : 0);
		}
		return acc;
	}
	
	private ArrayList<Boolean> BDDtoBools(BDD b, int lower, int upper) {
		ArrayList<Boolean> bools = new ArrayList<Boolean>();
		int bits = BDDtoInt(b,lower,upper);
		for (int i=0; i<upper-lower; i++) {
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
		for (int i = offset+registerBitSize-1; i>=offset; i--) {
			if (id%2 == 1) b.andWith(getOrCreateFactory(entry).ithVar(i));
			else b.andWith(getOrCreateFactory(entry).nithVar(i));
			id /= 2;
		}
		return b;
	}
	
	private BDD fieldSetToBDD(FieldSet fs,int leftRight) {
		int id = fs.getVal();
		BDD b = getOrCreateFactory(entry).one();
		int offset = (leftRight==LEFT) ? 2*registerBitSize : 2*registerBitSize+fieldBitSize;
		for (int i = offset+fieldBitSize-1; i>=offset; i--) {
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

	public void copyToGhostRegisters(Entry entry) {
		RegisterFactory rf = entry.getMethod().getCFG().getRegisterFactory();
		Utilities.begin("COPY TO GHOST REGISTERS - " + this + " - " + GlobalInfo.getGhostCopy(entry));
		for (int i=0; i<rf.size(); i++) {
			Register r = rf.get(i);
			// WARNING: once again, it would be better to find the way to obtain the
			// local variables of a method! (instead of the registerFactory which 
			// includes temporary and (now) ghost copies)
			if (!r.getType().isPrimitiveType()) {
				Register ghost = GlobalInfo.getGhostCopy(entry,r);
				if (ghost!=null) copyInfo(r,ghost);
			}
		}
		Utilities.end("COPY TO GHOST REGISTERS - " + this + " - " + GlobalInfo.getGhostCopy(entry));
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
		Utilities.debugMGB("registerBitSize " + registerBitSize);
		Utilities.debugMGB("fieldBitSize " + fieldBitSize);
		Utilities.debugMGB("No. Fields " + nFields);
		Utilities.debugMGB("SDomain" + getOrCreateDomain());
	}

	public AbstractValue propagateGetfield(Entry entry, Quad q, Register base,
			Register dest, jq_Field field) {
		// TODO Auto-generated method stub
		return null;
	}

	public AbstractValue propagatePutfield(Entry entry, Quad q, Register base,
			Register dest, jq_Field field) {
		// TODO Auto-generated method stub
		return null;
	}

	public AbstractValue propagateInvoke(Entry entry, Entry invokedEntry,
			Quad q, ArrayList<Register> actualParameters) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<Pair<FieldSet, FieldSet>> getStuples(Register r1, Register r2) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<FieldSet> getCtuples(Register r) {
		// TODO Auto-generated method stub
		return null;
	}

}
