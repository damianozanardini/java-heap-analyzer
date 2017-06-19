package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.util.tuple.object.Pair;
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
	
	protected static HashMap<Entry,BDDFactory[]> factories = new HashMap<Entry,BDDFactory[]>();
	protected static HashMap<Entry,BDDDomain[]> domains = new HashMap<Entry,BDDDomain[]>();

	private BDD sComp;
	private BDD cComp;
	
	// number of registers in the Entry
	private int nRegisters;
	// number of fields in the program
	private static int nFields = GlobalInfo.getNumberOfFields();
	// list of registers in the Entry
	private List<Register> registerList;

	// number of bits necessary to represent nRegisters registers
	private int registerBitSize;
	// number of bits necessary to represent all the fieldsets
	private int fieldBitSize;
	

	private int nBDDVars_sh;
	private int nBDDVars_cy;	
	
	static final int SHARE = 0;
	static final int CYCLE = 1;
	static final int LEFT = 0;
	static final int RIGHT = 1;
	
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

		nRegisters = entry.getNumberOfRegisters();
		registerList = entry.getRegisterList();
		for (int i=1; i<nRegisters; i*=2) { registerBitSize++; } 
		fieldBitSize = nFields;
		nBDDVars_sh = 2*registerBitSize + 2*fieldBitSize;
		nBDDVars_cy = registerBitSize + fieldBitSize;
		
		getOrCreateDomain();
		
		sComp = getOrCreateFactory(e)[SHARE].zero();
		cComp = getOrCreateFactory(e)[CYCLE].zero();

	}

	/**
	 * 
	 * Gets an array of 2 BDDFactory associated to a given entry or creates a new one
	 * <p>
	 * Each element of the array represents the Domain regarding Sharing or Cycle.
	 *  
	 * @param e the entry object needed to find the BDDFactory[] in the factories map
	 * @return an array of BDDFactory, being BDDFactory[CYCLE] and BDDFactory[SHARE] the 
	 * corresponding BDDFactory for each kind of information.
	 */
	private BDDFactory[] getOrCreateFactory(Entry e) {
		if (factories.containsKey(e)) return factories.get(e);	
		//sharing factory
		BDDFactory sFactory = BDDFactory.init("java",1000, 1000);
		sFactory.setVarNum(nBDDVars_sh);

		BDDFactory cFactory = BDDFactory.init("java",1000, 1000);
		cFactory.setVarNum(nBDDVars_cy);

		BDDFactory [] factoriesElem = new BDDFactory [2];
		factoriesElem[SHARE] = sFactory;
		factoriesElem[CYCLE] = cFactory;
	
		factories.put(e,factoriesElem);
		return factoriesElem;
	}



	/**
	 * Gets an array of 2 BDDDomains associated to the current entry or creates a new one
	 * <p>
	 * Each element of the array represents the Domain regarding Sharing or Cycle.
	 * 
	 * @param e the entry object needed to find the BDDDomain[] in the factories map
	 * @return an array of BDDFactory, being BDDDomain[CYCLE] and BDDDomain[SHARE] the 
	 * corresponding BDDFactory for each kind of information.
	 */
	private BDDDomain[] getOrCreateDomain() {
		// Sharing Domain
		if (!domains.containsKey(entry)) {
			long sizeSExtDomain = 1 << nBDDVars_sh;
			long sizeCExtDomain = 1 << nBDDVars_cy;
			
			BDDDomain [] domainsElem = new BDDDomain [2];
			domainsElem[SHARE] = getOrCreateFactory(entry)[SHARE].extDomain(sizeSExtDomain);;
			domainsElem[CYCLE] = getOrCreateFactory(entry)[CYCLE].extDomain(sizeCExtDomain);

			domains.put(entry,domainsElem);
		}
		return domains.get(entry);
	}
	
	
	@Override
	
	// TODO MIGUEL deberia comprobar que no está ya ese valor o se optimiza solo?
	public boolean update(AbstractValue other) {
		if (other == null) return false;
		if (other instanceof BDDAbstractValue){
			BDD sCompCopy = sComp.id();
			BDD cCompCopy = cComp.id();
			sComp.orWith(((BDDAbstractValue) other).getSComp());
			cComp.orWith(((BDDAbstractValue) other).getCComp());
			// TODO  MIGUEL revisar que este equals es valido para este caso
			return !sComp.equals(sCompCopy) | !cComp.equals(cCompCopy) ;
		}else
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
	 * Generates a new BDD describing the sharing information between the registers and
	 * fieldsets provided as parameters, and "adds" it (by logical disjunction)
	 * to the existing information
	 * @param r1 the first register
	 * @param r2 the second register
	 * @param fs1 the fieldset associated to r1
	 * @param fs2 the fieldset associated to r2
	 */
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
		BDD newBDDSEntry = getOrCreateDomain()[SHARE].ithVar(bits);
		notifyBddAdded(newBDDSEntry, SHARE);
		// note: newBDDSEntry is destroyed
		sComp.orWith(newBDDSEntry);
	}

	/**
	 * Generates a new BDD describing the cyclicity of a given pair of 
	 * register and fieldset provided as parameters, and "adds" it (by logical disjunction)
	 * to the existing information about cyclicity 
	 * @param r the register 
	 * @param fs the fieldset associated to r
	 */

	@Override
	public void addCinfo(Register r, FieldSet fs) {
		long bitsReversed = fs.getVal()  +
				(registerList.indexOf(r) << (fieldBitSize));
		// reversing the bit list
		long bits = 0;
		for (int i=0; i<nBDDVars_cy; i++) {
			int lastBit = (int) bitsReversed % 2;
			bitsReversed /= 2;
			bits = bits*2 + lastBit;
		}
		BDD newBDDCEntry = getOrCreateDomain()[CYCLE].ithVar(bits);
		notifyBddAdded(newBDDCEntry, CYCLE);
		// note: newBDDSEntry is destroyed
		cComp.orWith(newBDDCEntry);
	}

	/**
	 * Copies the Cyclicity and Sharing information from one register to another
	 * and keeps both in the existing information. 
	 * @param source the original register to get the information to be copied
	 * @param dest the destination register for the information.
	 */
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
		BDD sourceBDD = registerToBDD(source,LEFT, SHARE).and(registerToBDD(source,RIGHT, SHARE));
		BDD aboutSource = copy.and(sourceBDD);
		BDD quantified = aboutSource.exist(varIntervalToBDD(0,2*registerBitSize, SHARE));
		BDD destBDD = registerToBDD(dest,LEFT, SHARE).and(registerToBDD(dest,RIGHT, SHARE));
		BDD aboutDestBoth = quantified.and(destBDD);
		// left part: from (source,other,fs1,fs2) to (dest,other,fs1,fs2)
		sourceBDD = registerToBDD(source,LEFT, SHARE);
		aboutSource = copy.and(sourceBDD);
		quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize, SHARE));
		destBDD = registerToBDD(dest,LEFT, SHARE);
		BDD aboutDestLeft = quantified.and(destBDD);
		// right part: from (other,source,fs1,fs2) to (other,dest,fs1,fs2)
		sourceBDD = registerToBDD(source,RIGHT, SHARE);
		aboutSource = copy.and(sourceBDD);
		quantified = aboutSource.exist(varIntervalToBDD(registerBitSize,2*registerBitSize, SHARE));
		destBDD = registerToBDD(dest,RIGHT, SHARE);
		BDD aboutDestRight = quantified.and(destBDD);
		sComp.orWith(aboutDestBoth).orWith(aboutDestLeft).orWith(aboutDestRight); 
	}

	@Override
	public void copyCinfo(Register source, Register dest) {
		BDD copy = cComp.id();
		// from (source,fs) to (dest,fs)
		BDD sourceBDD = registerToBDD(source,LEFT, CYCLE);
		BDD aboutSource = copy.and(sourceBDD);
		BDD quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize, CYCLE));
		BDD destBDD = registerToBDD(dest,LEFT, CYCLE);
		BDD aboutDest = quantified.and(destBDD);
		
		cComp.orWith(aboutDest);
	}
	/**
	 * Moves the Cyclicity and Sharing information from one register to another
	 * <p>
	 * All the information referencing the source register will be deleted.
	 * 
	 * @param source the original register to get the information to be moved
	 * @param dest the destination register for the information.
	 */
	@Override
	public void moveInfo(Register source, Register dest) {
		moveSinfo(source, dest);
		moveCinfo(source, dest);
	}

	public void moveSinfo(Register source, Register dest) {
		BDD copy = sComp.id();
		// both parts: from (source,source,fs1,fs2) to (dest,dest,fs1,fs2)
		BDD sourceBDD = registerToBDD(source,LEFT, SHARE).and(registerToBDD(source,RIGHT, SHARE));
		BDD rest = copy.and(sourceBDD.not());
		BDD aboutSource = copy.and(sourceBDD);
		BDD quantified = aboutSource.exist(varIntervalToBDD(0,2*registerBitSize, SHARE));
		BDD destBDD = registerToBDD(dest,LEFT, SHARE).and(registerToBDD(dest,RIGHT, SHARE));
		BDD aboutDestBoth = quantified.and(destBDD);
		// left part: from (source,other,fs1,fs2) to (dest,other,fs1,fs2)
		sourceBDD = registerToBDD(source,LEFT, SHARE);
		aboutSource = copy.and(sourceBDD);
		quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize, SHARE));
		destBDD = registerToBDD(dest,LEFT, SHARE);
		BDD aboutDestLeft = quantified.and(destBDD);
		// right part: from (other,source,fs1,fs2) to (other,dest,fs1,fs2)
		sourceBDD = registerToBDD(source,RIGHT, SHARE);
		aboutSource = copy.and(sourceBDD);
		quantified = aboutSource.exist(varIntervalToBDD(registerBitSize,2*registerBitSize, SHARE));
		destBDD = registerToBDD(dest,RIGHT, SHARE);
		BDD aboutDestRight = quantified.and(destBDD);
		sComp = rest.orWith(aboutDestBoth).orWith(aboutDestLeft).orWith(aboutDestRight); 

	}

	@Override
	public void moveCinfo(Register source, Register dest) {
		BDD copy = cComp.id();
		// from (source,fs) to (dest,fs)
		BDD sourceBDD = registerToBDD(source,LEFT, CYCLE);
		BDD rest = copy.and(sourceBDD.not());
		BDD aboutSource = copy.and(sourceBDD);
		BDD quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize, CYCLE));
		BDD destBDD = registerToBDD(dest,LEFT, CYCLE);
		BDD aboutDest = quantified.and(destBDD);
		cComp = rest.orWith(aboutDest);		
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
		sComp.andWith(registerToBDD(r,LEFT, SHARE).or(registerToBDD(r,RIGHT, SHARE)).not());
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
	
	// TODO MIGUEL estos metodos no deberian estar en la clase padre? 
	public void cleanGhostRegisters(Entry entry) {
		Utilities.begin("CLEANING GHOST INFORMATION");
		RegisterFactory registerFactory = entry.getMethod().getCFG().getRegisterFactory();
		for (int i=0; i<registerFactory.size(); i++) {
			Register r = registerFactory.get(i);
			if (!r.getType().isPrimitiveType()) {
				Register rprime = GlobalInfo.getGhostCopy(entry,r);
				removeInfo(r);
				moveInfo(rprime,r);
			}
		}
	}

	@Override
	public void filterActual(List<Register> actualParameters) {
		// TODO Auto-generated method stub

	}

	private BDD getSinfo(Register r1, Register r2) {
		BDD bdd1 = registerToBDD(r1,LEFT, SHARE);
		BDD bdd2 = registerToBDD(r2,RIGHT, SHARE);
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
		BDD rbdd = registerToBDD(r,LEFT, SHARE);
		BDD result = sComp.and(rbdd);
		return result;
	}

	private BDD getSinfoSecondRegister(Register r) {
		BDD rbdd = registerToBDD(r,RIGHT, SHARE);
		BDD result = sComp.and(rbdd);
		return result;
	}

	private List<FieldSet> getCinfo(Register r) {
		return null;
	}

	private void printLines() {
		Utilities.begin("PRINTING BDD SOLUTIONS");
		BDD toIterate = varIntervalToBDD(0,nBDDVars_sh, SHARE);
		BDDIterator it = sComp.iterator(toIterate);	
		while (it.hasNext()) {
			BDD b = (BDD) it.next();
			Utilities.info(b.toString());
		}
		Utilities.end("PRINTING BDD SOLUTIONS");
	}
	
	// TODO MIGUEL es curioso, pero si hago el toString al contrario (primero share) me da un problema con 
	// las factories y un nullpointer cuando llamo a support(), así como está funciona. Pero en el alguna operación del
	// to string de share se le hace algo a las factories que no le gusta a JavaBDD (no he sido capaz de encontrarlo aun)
	public String toString() {
		// Cycle information
		BDD toIterate = varIntervalToBDD(0, nBDDVars_cy, CYCLE);
		BDDIterator it = cComp.iterator(toIterate);
		String sC = "";
		while (it.hasNext()) {
			BDD b = (BDD) it.next();
			// only the "bits" of the register
			BDD r = b.exist(varIntervalToBDD(registerBitSize, nBDDVars_cy, CYCLE));
			// only the "bits" of the fieldset
			BDD fs = b.exist(varIntervalToBDD(0, registerBitSize, CYCLE))
					.exist(varIntervalToBDD(registerBitSize + fieldBitSize, nBDDVars_cy, CYCLE));
			sC = sC + "(";
			int bits1 = BDDtoInt(r, 0, registerBitSize, CYCLE);
			sC = sC + entry.getNthRegister(bits1).toString();
			sC = sC + ",{ ";
			ArrayList<Boolean> bools1 = BDDtoBools(fs, registerBitSize, registerBitSize + fieldBitSize, CYCLE);
			int j = 0;
			for (boolean x : bools1) {
				if (x)
					sC = sC + GlobalInfo.getNthField(j);
				j++;
			}
			sC = sC + "})" + (it.hasNext() ? " - " : "");
		}
		
		String sS = "";
		toIterate = varIntervalToBDD(0,nBDDVars_sh,SHARE);
		it = sComp.iterator(toIterate);
		// Sharing information
		while (it.hasNext()) {
			BDD b = (BDD) it.next();
			// only the "bits" of the first register 
			BDD r1 = b.exist(varIntervalToBDD(registerBitSize,nBDDVars_sh, SHARE));
			// only the "bits" of the second register 
			BDD r2 = b.exist(varIntervalToBDD(0,registerBitSize, SHARE)).exist(varIntervalToBDD(2*registerBitSize,nBDDVars_sh, SHARE));
			// only the "bits" of the first fieldset
			BDD fs1 = b.exist(varIntervalToBDD(0,2*registerBitSize, SHARE)).exist(varIntervalToBDD(2*registerBitSize+fieldBitSize,nBDDVars_sh, SHARE));
			// only the "bits" of the second fieldset 
			BDD fs2 = b.exist(varIntervalToBDD(0,2*registerBitSize+fieldBitSize, SHARE));
			
			sS = sS + "(";
			int bits1 = BDDtoInt(r1,0,registerBitSize, SHARE);
			sS = sS + entry.getNthRegister(bits1).toString();
			sS = sS + ",";
			int bits2 = BDDtoInt(r2,registerBitSize,2*registerBitSize, SHARE);

			sS = sS + entry.getNthRegister(bits2).toString();
			sS = sS + ",{ ";
			ArrayList<Boolean> bools1 = BDDtoBools(fs1,2*registerBitSize,2*registerBitSize+fieldBitSize, SHARE);
			int j = 0;
			for (boolean x : bools1) {
				if (x) sS = sS + GlobalInfo.getNthField(j);
				j++;
			}
			sS = sS + "},{ ";
			ArrayList<Boolean> bools2 = BDDtoBools(fs2,2*registerBitSize+fieldBitSize,nBDDVars_sh, SHARE);
			j = 0;
			for (boolean x : bools2) {
				if (x) sS = sS + GlobalInfo.getNthField(j);
				j++;
			}
			sS = sS + "})" + (it.hasNext() ? " - " : "");
		}
			sS += " / ";

		return sS + sC;
	}
		
	private BDD varIntervalToBDD(int lower,int upper, int cycleshare) {
		BDD x = getOrCreateFactory(entry)[cycleshare].one();
		for (int i=lower; i<upper; i++) {
			x.andWith(getOrCreateFactory(entry)[cycleshare].ithVar(i));
		}
		return x;
	}

	private BDD varListToBDD(ArrayList<Integer> list, int cycleshare) {
		BDD x =  getOrCreateFactory(entry)[cycleshare].one();
		for (int i : list) x.andWith(getOrCreateFactory(entry)[cycleshare].ithVar(i));
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
	private int BDDtoInt(BDD b, int lower, int upper, int cycleShare) {
		int[] vars = b.support().scanSet();

		int temp;
		for (int i = 0; i < vars.length / 2; i++) {
			temp = vars[i];
			vars[i] = vars[vars.length - 1 - i];
			vars[vars.length - 1 - i] = temp;
		}
		int acc = 0;
		for (int i = lower; i < upper; i++) {
			ArrayList<Integer> l = new ArrayList<Integer>();
			for (int j = lower; j < upper; j++)
				if (j != i)
					l.add(j);
			boolean isHere = b.exist(varListToBDD(l, cycleShare)).restrict(getOrCreateFactory(entry)[cycleShare].ithVar(i)).isOne();
			acc = 2 * acc + (isHere ? 1 : 0);
		}
		return acc;
	}
	
	private ArrayList<Boolean> BDDtoBools(BDD b, int lower, int upper, int cycleShare) {
		ArrayList<Boolean> bools = new ArrayList<Boolean>();
		int bits = BDDtoInt(b,lower,upper, cycleShare);
		for (int i=0; i<upper-lower; i++) {
			bools.add(0,bits%2==1);
			bits = bits >>> 1;
		}
		return bools;
	}
	

	private BDD registerToBDD(Register r,int leftRight, int cycleShare) {
		int id = registerList.indexOf(r);
		BDD b = getOrCreateFactory(entry)[cycleShare].one();
		int offset =  (cycleShare == CYCLE) ? 0 : (leftRight==LEFT) ? 0 : registerBitSize;
		for (int i = offset+registerBitSize-1; i>=offset; i--) {
			if (id%2 == 1) b.andWith(getOrCreateFactory(entry)[cycleShare].ithVar(i));
			else b.andWith(getOrCreateFactory(entry)[cycleShare].nithVar(i));
			id /= 2;
		}
		return b;
	}
	
	private BDD fieldSetToBDD(FieldSet fs,int leftRight, int cycleShare) {
		int id = fs.getVal();

		BDD b = getOrCreateFactory(entry)[cycleShare].one();
		int offset = (leftRight==LEFT) ? 2*registerBitSize : 2*registerBitSize+fieldBitSize;
		for (int i = offset+fieldBitSize-1; i>=offset; i--) {
			if (id%2 == 1) b.andWith(getOrCreateFactory(entry)[cycleShare].ithVar(i));
			else b.andWith(getOrCreateFactory(entry)[cycleShare].nithVar(i));
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


	private void notifyBddAdded(BDD bdd, int sharecycle){
		String kind = (sharecycle == SHARE) ? "SHARE" : "CYCLE";
		Utilities.info("ADDED TO "+ kind +" BDD: " + bdd.toString());
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
	
	public BDD getSComp(){
		return sComp;
	}

	public BDD getCComp(){
		return cComp;
	}
	

}