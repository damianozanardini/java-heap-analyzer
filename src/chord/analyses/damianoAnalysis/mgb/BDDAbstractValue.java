package chord.analyses.damianoAnalysis.mgb;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

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
import net.sf.javabdd.BDDBitVector;
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
	static final int LEFT = -1;
	static final int UNIQUE = 0;
	static final int RIGHT = 1;
	
    public BDDAbstractValue(Entry e, BDD sc, BDD cc) {
		this(e);
		// WARNING: this overrides the assignments in this(e); it is done on
		// purpose, but a better way to do it could probably be found
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

		// compute relevant numbers
		registerBitSize = 0;
		fieldBitSize = 0;

		nRegisters = entry.getNumberOfRegisters();
		registerList = entry.getRegisterList();
		for (int i=1; i<nRegisters; i*=2) { registerBitSize++; } 
		fieldBitSize = nFields;
		nBDDVars_sh = 2*registerBitSize + 2*fieldBitSize;
		nBDDVars_cy = registerBitSize + fieldBitSize;
		// create both domain unless they already exist
		getOrCreateDomain();
		// create factories; the second call never creates factories (the first
		// call can possibly do) 
		sComp = getOrCreateFactory(e)[SHARE].zero();
		cComp = getOrCreateFactory(e)[CYCLE].zero();
	}

	/**
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

		BDDFactory [] factoriesPair = new BDDFactory[2];
		factoriesPair[SHARE] = sFactory;
		factoriesPair[CYCLE] = cFactory;
	
		factories.put(e,factoriesPair);
		return factoriesPair;
	}

	/**
	 * Gets an array of 2 BDDDomain associated to the current entry or creates a new one
	 * <p>
	 * Each element of the array represents the Domain regarding Sharing or Cycle.
	 * In order to be able to deal with more than 64 variables, a BigInteger is used
	 * to encode the argument for the domain.
	 * 
	 * @param e the entry object needed to find the BDDDomain[] in the factories map
	 * @return an array of BDDFactory, being BDDDomain[CYCLE] and BDDDomain[SHARE] the 
	 * corresponding BDDFactory for each kind of information.
	 */
	private BDDDomain[] getOrCreateDomain() {
		if (!domains.containsKey(entry)) {
			BDDDomain[] domainsPair = new BDDDomain [2];
			// sharing
			int nBytes = nBDDVars_sh/8+1;
			byte[] bytes = new byte[nBytes];
			for (int i=nBytes-1; i>0; i--) bytes[i] = 0;
			bytes[0] = (byte) (1 << (nBDDVars_sh % 8));
			domainsPair[SHARE] = getOrCreateFactory(entry)[SHARE].extDomain(new BigInteger(1,bytes));;
			// cyclicity
			nBytes = nBDDVars_cy/8+1;
			bytes = new byte[nBytes];
			for (int i=nBytes-1; i>0; i--) bytes[i] = 0;
			bytes[0] = (byte) (1 << (nBDDVars_cy % 8));
			domainsPair[CYCLE] = getOrCreateFactory(entry)[CYCLE].extDomain(new BigInteger(1,bytes));;
			domains.put(entry,domainsPair);
		}
		return domains.get(entry);
	}
	
	// WARNING: double-check it when the bdd implementation will work on its own
	public boolean update(AbstractValue other) {
		if (other == null) return false;
		if (other instanceof BDDAbstractValue) {
			BDD sNew = ((BDDAbstractValue) other).getSComp();
			BDD cNew = ((BDDAbstractValue) other).getCComp();
			// inclusion test
			if (sNew.imp(sComp).isOne() && cNew.imp(cComp).isOne()) return false;
			sComp.orWith(sNew);
			cComp.orWith(cNew);
			return true;
		} else {
			Utilities.err("BDDAbstractValue.update: wrong type of parameter - " + other);
			return false;
		}
	}
	
	/**
	 * Returns a new BDDAbstractValue object with the same abstract information.
	 * 
	 * @return a copy of itself
	 */
	public BDDAbstractValue clone() {
		return new BDDAbstractValue(entry, sComp.id(), cComp.id());
	}
	
	/**
	 * Generates a new BDD describing the sharing information between the registers and
	 * fieldsets provided as parameters, and "adds" it (by logical disjunction)
	 * to the existing information
	 * 
	 * @param r1 the first register
	 * @param r2 the second register
	 * @param fs1 the fieldset associated to r1
	 * @param fs2 the fieldset associated to r2
	 */
	// WARNING: add support for BigInteger
	public void addSinfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		BigInteger bint = BigInteger.valueOf((long) fs2.getVal());
		bint = bint.add(BigInteger.valueOf((long) fs1.getVal()).shiftLeft(fieldBitSize));
		bint = bint.add(BigInteger.valueOf((long) registerList.indexOf(r1)).shiftLeft(2*fieldBitSize));
		bint = bint.add(BigInteger.valueOf((long) registerList.indexOf(r2)).shiftLeft(2*fieldBitSize+registerBitSize));
		int bl = nBDDVars_sh;
		// reverse the bits in the BigInteger object
		for (int i=0; i<bl/2; i++) {
			boolean bleft = bint.testBit(i);
			boolean bright = bint.testBit(bl-i-1);
			if (bleft && !bright) {
				bint = bint.clearBit(i);
				bint = bint.setBit(bl-i-1);
			}
			if (!bleft && bright) {
				bint = bint.setBit(i);
				bint = bint.clearBit(bl-i-1);
			}			
		}
		//long bitsReversed = fs2.getVal() + 
		//		(fs1.getVal() << fieldBitSize) +
		//		(registerList.indexOf(r1) << (2*fieldBitSize)) +
		//		(registerList.indexOf(r2) << (2*fieldBitSize+registerBitSize));
		//// reversing the bit list
		//long bits = 0;
		//while (bitsReversed>0) {
		//	int lastBit = (int) bitsReversed % 2;
		//	bitsReversed /= 2;
		//	bits = bits*2 + lastBit;
		//}
		BDD newBDDSEntry = getOrCreateDomain()[SHARE].ithVar(bint);
		notifyBddAdded(newBDDSEntry, SHARE);
		sComp.orWith(newBDDSEntry);
	}

	/**
	 * Generates a new BDD describing the cyclicity of a given pair of 
	 * register and fieldset provided as parameters, and "adds" it (by logical disjunction)
	 * to the existing information about cyclicity 
	 * 
	 * @param r the register 
	 * @param fs the fieldset associated to r
	 */
	public void addCinfo(Register r, FieldSet fs) {
		BigInteger bint = BigInteger.valueOf((long) fs.getVal());
		bint = bint.add(BigInteger.valueOf((long) registerList.indexOf(r)).shiftLeft(fieldBitSize));
		int bl = nBDDVars_cy;
		// reverse the bits in the BigInteger object
		for (int i=0; i<bl/2; i++) {
			boolean bleft = bint.testBit(i);
			boolean bright = bint.testBit(bl-i-1);
			if (bleft && !bright) {
				bint = bint.clearBit(i);
				bint = bint.setBit(bl-i-1);
			}
			if (!bleft && bright) {
				bint = bint.setBit(i);
				bint = bint.clearBit(bl-i-1);
			}			
		}

		//long bitsReversed = fs.getVal()  +
		//		(registerList.indexOf(r) << (fieldBitSize));
		//// reversing the bit list
		//long bits = 0;
		//while (bitsReversed>0) {
		//	int lastBit = (int) bitsReversed % 2;
		//	bitsReversed /= 2;
		//	bits = bits*2 + lastBit;
		//}
		BDD newBDDCEntry = getOrCreateDomain()[CYCLE].ithVar(bint);
		notifyBddAdded(newBDDCEntry, CYCLE);
		cComp.orWith(newBDDCEntry);
	}

	/**
	 * Copies the Cyclicity and Sharing information from one register to another
	 * and keeps both in the existing information.
	 * 
	 * @param source the original register to get the information to be copied
	 * @param dest the destination register for the information.
	 */
	public void copyInfo(Register source, Register dest) {
		copySinfo(source,dest);
		copyCinfo(source,dest);
	}
	
	// WARNING: from (source,source,fs1,fs2) both (dest,source,fs1,fs2) and 
	// (source,dest,fs1,fs2) are produced; this is redundant, but we can live with it
	public void copySinfo(Register source, Register dest) {
		BDD copy = sComp.id();
		// both parts: from (source,source,fs1,fs2) to (dest,dest,fs1,fs2)
		BDD sourceBDD = registerToBDD(source,SHARE,LEFT).and(registerToBDD(source,SHARE,RIGHT));
		BDD aboutSource = copy.and(sourceBDD);
		BDD quantified = aboutSource.exist(varIntervalToBDD(0,2*registerBitSize, SHARE));
		BDD destBDD = registerToBDD(dest,SHARE,LEFT).and(registerToBDD(dest,SHARE,RIGHT));
		BDD aboutDestBoth = quantified.and(destBDD);
		// left part: from (source,other,fs1,fs2) to (dest,other,fs1,fs2)
		sourceBDD = registerToBDD(source,SHARE,LEFT);
		aboutSource = copy.and(sourceBDD);
		quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize, SHARE));
		destBDD = registerToBDD(dest,SHARE,LEFT);
		BDD aboutDestLeft = quantified.and(destBDD);
		// right part: from (other,source,fs1,fs2) to (other,dest,fs1,fs2)
		sourceBDD = registerToBDD(source,SHARE,RIGHT);
		aboutSource = copy.and(sourceBDD);
		quantified = aboutSource.exist(varIntervalToBDD(registerBitSize,2*registerBitSize, SHARE));
		destBDD = registerToBDD(dest,SHARE,RIGHT);
		BDD aboutDestRight = quantified.and(destBDD);
		sComp.orWith(aboutDestBoth).orWith(aboutDestLeft).orWith(aboutDestRight); 
	}

	public void copyCinfo(Register source, Register dest) {
		BDD copy = cComp.id();
		// from (source,fs) to (dest,fs)
		BDD sourceBDD = registerToBDD(source,CYCLE,-1);
		BDD aboutSource = copy.and(sourceBDD);
		BDD quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize, CYCLE));
		BDD destBDD = registerToBDD(dest,CYCLE,-1);
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
		BDD sourceBDD = registerToBDD(source,SHARE,LEFT).and(registerToBDD(source,SHARE,RIGHT));
		BDD rest = copy.and(sourceBDD.not());
		BDD aboutSource = copy.and(sourceBDD);
		BDD quantified = aboutSource.exist(varIntervalToBDD(0,2*registerBitSize, SHARE));
		BDD destBDD = registerToBDD(dest,SHARE,LEFT).and(registerToBDD(dest,SHARE,RIGHT));
		BDD aboutDestBoth = quantified.and(destBDD);
		// left part: from (source,other,fs1,fs2) to (dest,other,fs1,fs2)
		sourceBDD = registerToBDD(source,SHARE,LEFT);
		aboutSource = copy.and(sourceBDD);
		quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize, SHARE));
		destBDD = registerToBDD(dest,SHARE,LEFT);
		BDD aboutDestLeft = quantified.and(destBDD);
		// right part: from (other,source,fs1,fs2) to (other,dest,fs1,fs2)
		sourceBDD = registerToBDD(source,SHARE,RIGHT);
		aboutSource = copy.and(sourceBDD);
		quantified = aboutSource.exist(varIntervalToBDD(registerBitSize,2*registerBitSize, SHARE));
		destBDD = registerToBDD(dest,SHARE,RIGHT);
		BDD aboutDestRight = quantified.and(destBDD);
		sComp = rest.orWith(aboutDestBoth).orWith(aboutDestLeft).orWith(aboutDestRight); 
	}

	@Override
	public void moveCinfo(Register source, Register dest) {
		BDD copy = cComp.id();
		// from (source,fs) to (dest,fs)
		BDD sourceBDD = registerToBDD(source,CYCLE,-1);
		BDD rest = copy.and(sourceBDD.not());
		BDD aboutSource = copy.and(sourceBDD);
		BDD quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize, CYCLE));
		BDD destBDD = registerToBDD(dest,CYCLE,-1);
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
		sComp.andWith(registerToBDD(r,SHARE,LEFT).or(registerToBDD(r,SHARE,RIGHT)).not());
	}
	
	private void removeCinfo(Register r) {
		cComp.andWith(registerToBDD(r,CYCLE,-1).not());
	}

	public void removeInfoList(List<Register> rs) {
		for (Register r : rs) removeInfo(r);
	}

	public void actualToFormal(List<Register> apl, Entry e) {
		Utilities.begin("ACTUAL " + apl + " TO FORMAL FROM " + this);
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.size(); i++) {
			try {
				dest.add(e.getNthRegister(i));
				// dest.add(RegisterManager.getRegFromNumber(m,i));
				source.add(apl.get(i));
			} catch (IndexOutOfBoundsException exc) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		moveInfoList(source,dest);
		Utilities.end("ACTUAL " + apl + " TO FORMAL RESULTING IN " + this);
	}

	public void formalToActual(List<Register> apl, Entry e) {
		Utilities.begin("FORMAL FROM " + this + " TO ACTUAL "+ apl);
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.size(); i++) {
			try {
				source.add(e.getNthRegister(i));
				// source.add(RegisterManager.getRegFromNumber(m,i));
				dest.add(apl.get(i));
			} catch (IndexOutOfBoundsException exc) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		moveInfoList(source,dest);
		Utilities.end("FORMAL TO ACTUAL " + apl + " RESULTING IN " + this);
	}
	
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

	public void filterActual(List<Register> actualParameters) {
		// TODO Auto-generated method stub

	}

	private BDD getSinfo(Register r1, Register r2) {
		BDD bdd1 = registerToBDD(r1,SHARE,LEFT);
		BDD bdd2 = registerToBDD(r2,SHARE,RIGHT);
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
		BDD rbdd = registerToBDD(r,SHARE,LEFT);
		BDD result = sComp.and(rbdd);
		return result;
	}

	private BDD getSinfoSecondRegister(Register r) {
		BDD rbdd = registerToBDD(r,SHARE,RIGHT);
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
	private int BDDtoInt(BDD b, int lower, int upper, int cycleShare) {
		int[] vars = b.support().scanSet();
		// se puede sacar con getVarIndeces, que te lo da con el orden al reves.
		// BigInteger [] varIndices = getOrCreateDomain()[cycleShare].getVarIndices(b, 1);
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
	
	private BDD registerToBDD(Register r,int cycleShare,int leftRight) {
		int id = registerList.indexOf(r);
		BDD b = getOrCreateFactory(entry)[cycleShare].one();
		int offset = (cycleShare == CYCLE) ? 0 : (leftRight==LEFT) ? 0 : registerBitSize;
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
	

	public BDDAbstractValue propagateGetfield(Entry entry, Quad q, Register base,
			Register dest, jq_Field field) {
		// TODO Auto-generated method stub
		return clone();
	}

	public BDDAbstractValue propagatePutfield(Entry entry, Quad q, Register v,
			Register rho, jq_Field field) {
		BDDFactory bf = getOrCreateFactory(entry)[SHARE];

		BDD regBaseBDD = registerToBDD(v,SHARE,LEFT);
		BDD regDestBDD = registerToBDD(rho,SHARE,RIGHT);
	    FieldSet z1 = FieldSet.addField(FieldSet.emptyset(),field);
		BDD fieldBDD = fieldSetToBDD(z1, LEFT, SHARE);
		
		Utilities.debugMGB("propagatePutfield -> regBase, regBaseBDD: "+ v + ", " + regBaseBDD );
		Utilities.debugMGB("propagatePutfield -> regDest, regDestBDD: "+ rho + ",  " + regDestBDD );
		Utilities.debugMGB("propagatePutfield -> field, fieldBDD: "+ field + ", " + fieldBDD );
		BDD repAnd = bf.one();
		repAnd.andWith(regBaseBDD.id()).andWith(regDestBDD.id()).andWith(fieldBDD.id());
		Utilities.debugMGB("propagatePutfield -> repAnd: "+ repAnd );
		
		// numero de registros así itero para todo w1 y w2
		int m = entry.getMethod().getCFG().getRegisterFactory().size();
    	for (int i=0; i<m; i++) {
    		for (int j=0; j<m; j++) {
    	    	Register w1 = entry.getMethod().getCFG().getRegisterFactory().get(i);
    	    	Register w2 = entry.getMethod().getCFG().getRegisterFactory().get(j);
    	    	// case 1
    			BDD omega1A = getSinfo(w1, v).andWith(
    						fieldSetToBDD(FieldSet.emptyset(), RIGHT, SHARE));
    			BDD omega2A	= getSinfo(rho, w2);
    			
    			BDD omega2B = getSinfo(v, w2).andWith(
						fieldSetToBDD(FieldSet.emptyset(), LEFT, SHARE));
    			BDD omega1B =  getSinfo(w1, rho);
    			
    			BDD omega1C =getSinfo(w1, v).andWith( 
    					fieldSetToBDD(FieldSet.emptyset(), RIGHT, SHARE));
    			BDD omegaC = getSinfo(rho, rho);
    			
    			BDD omega2C = getSinfo(v, w2).andWith(fieldSetToBDD(FieldSet.emptyset(), LEFT, SHARE));
						   
    			
    			BDD caseA = concatModels(omega1A, omega2A);
    			BDD caseB = concatModels(omega1B, omega2B);
    			BDD caseC = concatModels(omega1C, concatModels(omegaC, omega2C));
    			
    			// montar BDDAbstractValue con la disyunción de los BDD
    		}
    	}
		
		// TODO Auto-generated method stub
		return clone();
	}

	public BDDAbstractValue propagateInvoke(Entry entry, Entry invokedEntry,
			Quad q, ArrayList<Register> actualParameters) {
		// TODO Auto-generated method stub
		return clone();
	}

	public List<Pair<FieldSet, FieldSet>> getStuples(Register r1, Register r2) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<FieldSet> getCtuples(Register r) {
		// TODO Auto-generated method stub
		return null;
	}

	public BDD getSComp() {
		return sComp;
	}

	public BDD getCComp() {
		return cComp;
	}

	public BDD concatModels(BDD b1, BDD b2) {
		// WARNING: is it guaranteed that support() returns all the "variables"
		// we actually want?
		int[] vs = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		BDDFactory bf = getOrCreateFactory(entry)[SHARE];
		BDD acc = bf.zero();

		BDDIterator it = b2.iterator(b2.support());
		while (it.hasNext()) {
			BDD b = (BDD) it.next();
			BDD c = bf.one();
			// this loop basically applies a restriction on all variables which
			// appear with a ":1" in a solution
			// (e.g., transforms <0:0, 1:0, 2:1, 3:0, 4:0, 5:1, 6:1, 7:1, 8:1,
			// 9:1> into <2:1, 5:1, 6:1, 7:1, 8:1, 9:1>
			// there is probably a better way to do this
			for (int i = 0; i < 10; i++) {
				// if
				// (b.exist(myVarSetMinusOne(vs,i)).restrict(bf.ithVar(i)).isOne())
				// {
				if (b.exist(complement(b2.support(), bf.ithVar(i))).restrict(bf.ithVar(i)).isOne()) {
					c.andWith(bf.ithVar(i));
				}
			}
			System.out.println("c =      " + c);
			BDD f = b1.id().exist(c).andWith(c);
			System.out.println("NEW LINE: " + f);
			acc.orWith(f);
		}
		return acc;
	}

	static BDD complement(BDD allVars, BDD set) {
		return (allVars.support().exist(set.support()).support());
	}

}
