package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import chord.analyses.damianoAnalysis.Entry;
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
	
	// number of variables in the BDD for sharing
	private int nBDDVars_sh;
	// number of variables in the BDD for cyclicity
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

		nRegisters = entry.getNumberOfReferenceRegisters();
		registerList = entry.getReferenceRegisterList();
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
		BDD sourceBDD = registerToBDD(source,CYCLE,UNIQUE);
		BDD aboutSource = copy.and(sourceBDD);
		BDD quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize, CYCLE));
		BDD destBDD = registerToBDD(dest,CYCLE,UNIQUE);
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

	// WARNING: now it should work, but check!
	public void moveSinfo(Register source, Register dest) {
		BDD sourceBDD = registerToBDD(source,SHARE,LEFT).or(registerToBDD(source,SHARE,RIGHT));	
		BDD rest = sComp.and(sourceBDD.not());
		// both parts: from (source,source,fs1,fs2) to (dest,dest,fs1,fs2)
		// BDD selfBDD = registerToBDD(source,SHARE,LEFT).and(registerToBDD(source,SHARE,RIGHT));
		// BDD aboutSource = copy.and(selfBDD);
		// BDD quantified = aboutSource.exist(varIntervalToBDD(0,2*registerBitSize, SHARE));
		BDD quantified = getSinfo(source,source);
		BDD aboutDestBoth = quantified.and(registerToBDD(dest,SHARE,LEFT).and(registerToBDD(dest,SHARE,RIGHT)));
		// left part: from (source,other,fs1,fs2) to (dest,other,fs1,fs2)
		// sourceBDD = registerToBDD(source,SHARE,LEFT);
		// aboutSource = copy.and(sourceBDD);
		// quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize, SHARE));
		quantified = getSinfo(source,LEFT);
		BDD aboutDestLeft = quantified.and(registerToBDD(dest,SHARE,LEFT));
		// right part: from (other,source,fs1,fs2) to (other,dest,fs1,fs2)
		// sourceBDD = registerToBDD(source,SHARE,RIGHT);
		// aboutSource = copy.and(sourceBDD);
		// quantified = aboutSource.exist(varIntervalToBDD(registerBitSize,2*registerBitSize, SHARE));
		quantified = getSinfo(source,RIGHT);
		BDD aboutDestRight = quantified.and(registerToBDD(dest,SHARE,RIGHT));
		sComp = rest.orWith(aboutDestBoth).orWith(aboutDestLeft).orWith(aboutDestRight); 
	}

	public void moveCinfo(Register source, Register dest) {
		BDD copy = cComp.id();
		// from (source,fs) to (dest,fs)
		BDD sourceBDD = registerToBDD(source,CYCLE,UNIQUE);
		BDD rest = copy.and(sourceBDD.not());
		BDD aboutSource = copy.and(sourceBDD);
		BDD quantified = aboutSource.exist(varIntervalToBDD(0,registerBitSize, CYCLE));
		BDD destBDD = registerToBDD(dest,CYCLE,UNIQUE);
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
		cComp.andWith(registerToBDD(r,CYCLE,UNIQUE).not());
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
				dest.add(e.getNthReferenceRegister(i));
				// dest.add(RegisterManager.getRegFromNumber(m,i));
				source.add(apl.get(i));
			} catch (IndexOutOfBoundsException exc) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		moveInfoList(source,dest);
		Utilities.end("ACTUAL " + apl + " TO FORMAL RESULTING IN " + this);
	}

	public void formalToActual(List<Register> apl,Register rho,Entry e) {
		Utilities.begin("FORMAL FROM " + this + " TO ACTUAL "+ apl);
		ArrayList<Register> source = new ArrayList<Register>();
		ArrayList<Register> dest = new ArrayList<Register>();
		for (int i=0; i<apl.size(); i++) {
			try {
				source.add(e.getNthReferenceRegister(i));
				// source.add(RegisterManager.getRegFromNumber(m,i));
				dest.add(apl.get(i));
			} catch (IndexOutOfBoundsException exc) {
				Utilities.warn(i + "-th REGISTER COULD NOT BE RETRIEVED");
			}
		}
		moveInfoList(source,dest);
		Register out = GlobalInfo.getReturnRegister(e.getMethod());
		if (out != null && rho != null)
			moveInfo(out,rho);		
		Utilities.end("FORMAL TO ACTUAL " + apl + " RESULTING IN " + this);
	}
	
	public void cleanGhostRegisters(Entry entry) {
		Utilities.begin("CLEANING GHOST INFORMATION");
		for (Register r : entry.getReferenceRegisterList()) { 
			if (!r.getType().isPrimitiveType()) {
				Register rprime = GlobalInfo.getGhostCopy(entry.getMethod(),r);
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
		BDD conjunction = bdd1.and(bdd2);
		return sComp.and(conjunction).exist(conjunction.support());
	}

	private BDD getSinfo(Register r,int leftRight) {
		BDD bdd = registerToBDD(r,SHARE,leftRight);
		return sComp.and(bdd).exist(bdd.support());
	}	
	
	private BDD getSinfo(Register r) {
		BDD bdd1 = registerToBDD(r,SHARE,LEFT);
		BDD bdd2 = registerToBDD(r,SHARE,RIGHT);
		BDD disjunction = bdd1.orWith(bdd2);
		return sComp.and(disjunction).exist(disjunction.support());
	}	

	private BDD getCinfo(Register r) {
		BDD bdd = registerToBDD(r,CYCLE,UNIQUE);
		return cComp.and(bdd).exist(bdd.support());
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
			sC = sC + entry.getNthReferenceRegister(bits1).toString();
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
			sS = sS + entry.getNthReferenceRegister(bits1).toString();
			sS = sS + ",";
			int bits2 = BDDtoInt(r2,registerBitSize,2*registerBitSize, SHARE);

			sS = sS + entry.getNthReferenceRegister(bits2).toString();
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
	
	public boolean equals(AbstractValue av) {
		if (av instanceof BDDAbstractValue)
			return (sComp.biimpWith(((BDDAbstractValue) av).getSComp()).isOne() && cComp.biimpWith(((BDDAbstractValue) av).getCComp()).isOne());
		else return false;
	}
	
	public boolean isTop() {
		return this.cComp.isOne() && this.sComp.isOne();
	}

	public boolean isBottom() {
		return this.cComp.isZero() && this.sComp.isZero();
	}

	@Override
	public void copyFromCycle(Register base, Register dest) {
		// TODO Auto-generated method stub
		
	}

	public void copyToGhostRegisters(Entry entry) {
		jq_Method method = entry.getMethod();
		Utilities.begin("COPY TO GHOST REGISTERS - " + this + " - " + GlobalInfo.getGhostCopy(method));
		for (Register r : entry.getReferenceRegisterList()) {
			if (!r.getType().isPrimitiveType()) {
				Register ghost = GlobalInfo.getGhostCopy(method,r);
				if (ghost!=null) copyInfo(r,ghost);
			}
		}
		Utilities.end("COPY TO GHOST REGISTERS - " + this + " - " + GlobalInfo.getGhostCopy(method));
	}
	
	private void notifyBddAdded(BDD bdd, int sharecycle){
		String kind = (sharecycle == SHARE) ? "SHARE" : "CYCLE";
		Utilities.info("ADDED TO "+ kind +" BDD: " + bdd.toString());
	}
	
	public BDDAbstractValue doGetfield(Entry entry, Quad q, Register base,
			Register dest, jq_Field field) {
		// TODO Auto-generated method stub
		return clone();
	}

	public BDDAbstractValue doPutfield(Entry entry, Quad q, Register v,
			Register rho, jq_Field field) {
		BDDFactory bf = getOrCreateFactory(entry)[SHARE];
		BDDAbstractValue avIp = clone();

		// MIGUEL Falta Z empezaria aqui

	    FieldSet z1FS = FieldSet.addField(FieldSet.emptyset(),field);
	    BDD z1 = fieldSetToBDD(z1FS, RIGHT, SHARE);
    	BDD mdls_rhov = avIp.getSinfo(rho,v);
    	ArrayList<FieldSet> z2 = new ArrayList<FieldSet>();
    	
    	// termina aqui 
    	
		BDD bddIpp = bf.zero(); 
		// numero de registros así itero para todo w1 y w2
		int m = entry.getNumberOfReferenceRegisters();
    	for (int i=0; i<m; i++) {
    		for (int j=0; j<m; j++) {
    	    	Register w1 = entry.getNthReferenceRegister(i);
    	    	Register w2 = entry.getNthReferenceRegister(i);
    	    	// case (a)
    			BDD omega1A = getSinfo(w1, v).andWith(
    						fieldSetToBDD(FieldSet.emptyset(), RIGHT, SHARE));
    			BDD omega2A	= getSinfo(rho, w2);
    			
    			BDD caseA = concatBDDs(omega1A, omega2A, SHARE);
    			bddIpp.or(caseA);
    			
    			// case (b)
    			BDD omega2B = getSinfo(v, w2).andWith(
						fieldSetToBDD(FieldSet.emptyset(), LEFT, SHARE));
    			BDD omega1B =  getSinfo(w1, rho);
    			BDD caseB = concatBDDs(omega1B, omega2B, SHARE);
    			bddIpp.or(caseB);


    			// case (c)
    			// MIGUEL: optimizar porque algunos BDDS ya los tengo arriba.
    			BDD omega1C =getSinfo(w1, v).andWith( 
    					fieldSetToBDD(FieldSet.emptyset(), RIGHT, SHARE));
    			BDD omegaC = getSinfo(rho, rho);
    			
    			BDD omega2C = getSinfo(v, w2).andWith(fieldSetToBDD(FieldSet.emptyset(), LEFT, SHARE));
    			BDD caseC = concatBDDs(omega1C, concatBDDs(omegaC, omega2C, SHARE), SHARE);
    			bddIpp.or(caseC);

    		}
    	}
    	
    	// MIGUEL: repasar ¿hace falta hacer esto? Lo he hecho como un clon de TuplesAV
    	BDDAbstractValue avIpp = new BDDAbstractValue(entry, bddIpp, avIp.getCComp());
		// TODO Auto-generated method stub
    	avIp.update(avIpp);
		return avIp;
	}

	public BDDAbstractValue doInvoke(Entry entry, Entry invokedEntry,
			Quad q, ArrayList<Register> actualParameters, Register returnValue) {
		// TODO Auto-generated method stub
		return clone();
	}

	public ArrayList<Pair<FieldSet, FieldSet>> getStuples(Register r1, Register r2) {
		BDD sharing = getSinfo(r1,r2);
		ArrayList<BDD> list = separateSolutions(sharing,new int[nBDDVars_sh],SHARE);
		ArrayList<Pair<FieldSet, FieldSet>> pairs = new ArrayList<Pair<FieldSet, FieldSet>>();
		for (BDD b : list)
			pairs.add(bddToFieldSetPair(b));			
		return pairs;
	}

	/**
	 * Takes a linear BDD corresponding to two FieldSets, and returns the
	 * pair of FieldSet objects
	 * 
	 * @param b the input BDD (no check is done that it is indeed a linear one)
	 * @return
	 */
	private Pair<FieldSet,FieldSet> bddToFieldSetPair(BDD b) {
		BDDFactory bf = getOrCreateFactory(entry)[SHARE];
		int[] vars = b.support().scanSet();
		// first FieldSet
		int val = 0;
		for (int i=0; i<vars.length/2; i++) {
			if (b.and(bf.nithVar(i)).isZero()) 
				val = val*2+1;
			else val = val*2;
		}
		FieldSet fs1 = FieldSet.getByVal(val);
		// second FieldSet
		val = 0;
		for (int i=vars.length/2; i<vars.length; i++) {
			if (b.and(bf.nithVar(i)).isZero()) 
				val = val*2+1;
			else val = val*2;
		}
		FieldSet fs2 = FieldSet.getByVal(val);
		return new Pair<FieldSet,FieldSet>(fs1,fs2);
	}
	
	public ArrayList<FieldSet> getCtuples(Register r) {
		BDD cyclicity = getCinfo(r);
		ArrayList<BDD> list = separateSolutions(cyclicity,new int[nBDDVars_cy],CYCLE);
		ArrayList<FieldSet> fss = new ArrayList<FieldSet>();
		for (BDD b : list)
			fss.add(bddToFieldSet(b));			
		return fss;
	}
	
	/**
	 * Takes a linear BDD corresponding to a FieldSets, and returns the
	 * FieldSet object
	 * 
	 * @param b the input BDD (no check is done that it is indeed a linear one)
	 * @return
	 */
	private FieldSet bddToFieldSet(BDD b) {
		BDDFactory bf = getOrCreateFactory(entry)[CYCLE];
		int[] vars = b.support().scanSet();
		int val = 0;
		for (int i=0; i<vars.length; i++) {
			if (b.and(bf.nithVar(i)).isZero()) 
				val = val*2+1;
			else val = val*2;
		}
		return FieldSet.getByVal(val);
	}

	public BDD getSComp() {
		return sComp;
	}

	public BDD getCComp() {
		return cComp;
	}

	/**
	 * One of the key algorithms in the BDD implementation. This method takes
	 * two BDDs b1 and b2 and returns their "concatenation" b. Concatenation is
	 * something different from any standard logical operator I know. Its
	 * meaning is the following: whenever there is model (a truth assignment 
	 * which makes the formula true) t1 of b1, and a model t2 of b2, then there
	 * is a model t of b such that A(t) = A(t1) \cup A(t2) where A(_) is the
	 * set of variables which are true in the given truth assignment.
	 * 
	 * For example (p, q, r, and s have indices 0, 1, 2, and 3, respectively):
	 * F1 = (p OR q) AND NOT r
	 * F2 = s AND NOT q
	 * 
	 * F1 corresponds to b1 = <0:0, 1:1, 2:0><0:1, 2:0>
	 * F2 corresponds to b2 = <1:0, 3:1>
	 * 
	 * Models of F1 = { { p }, { q }, { p, q }, { p, s }, { q, s }, { p, q, s } }
	 * Models of F2 = { { s }, { p, s }, { r, s }, { p, r, s } }
	 * where each model t is actually represented by A(t) (no information loss)
	 * 
	 * We expect that the models of F = F1 concat F2 will be all the set-unions
	 * between models of F1 and models of F2:
	 * Models of F = { { p, s }, { p, r, s }, { q, s }, { p, q, s }, { q, r, s },
	 * { p, q, r, s } }
	 * 
	 * which corresponds to b = <0:0, 1:1, 3:1><0:1, 3:1>
	 * 
	 * The implementation is quite efficient since
	 * - the two nested loops on separateSolutions typically do few iterations
	 *   (separateSolutions does NOT return all models, but basically the result
	 *   of allSat in form of an ArrayList of "linear" bdds, a linear bdd being
	 *   one for which satCount = 1)
	 * - we found the way to do efficently the test on a given variable on a
	 *   linear bdd 
	 * 
	 * @param b1 the first BDD
	 * @param b2 the second BDD
	 * @param cycleShare whether cyclicity or sharing is considered (this
	 * affects the BDDFactory to be used and the number of variables to be
	 * considered)
	 * @return the "concatenation" of b1 and b2
	 */
	private BDD concatBDDs(BDD b1, BDD b2, int cycleShare) {
		BDDFactory bf = getOrCreateFactory(entry)[cycleShare];
		ArrayList<BDD> bdds1 = separateSolutions(b1,new int[bf.varNum()],cycleShare);
		ArrayList<BDD> bdds2 = separateSolutions(b2,new int[bf.varNum()],cycleShare);
	
		for (BDD x : bdds1) System.out.println("1 -> " + x);
		for (BDD x : bdds2) System.out.println("2 -> " + x);
		
		BDD concat = bf.zero();
		for (BDD c1 : bdds1) {
			for (BDD c2 : bdds2) {
				BDD line = bf.one();
				for (int i=0; i<bf.varNum(); i++) {
					if (c1.and(bf.nithVar(i)).isZero() || c2.and(bf.nithVar(i)).isZero()) // at least one set to 1
						line.andWith(bf.ithVar(i));
					if (c1.and(bf.ithVar(i)).isZero() && c2.and(bf.ithVar(i)).isZero()) // both set to 0
						line.andWith(bf.nithVar(i));
				}
				System.out.println("line = " + line);
				concat.orWith(line);
			}
		}
		return concat;
	}

	/**
	 * Returns the solutions of a bdd in form of a list of bdds. It does the
	 * same job as allSat, but no byte[] is returned (in that case, by nextSat);
	 * instead, each solution comes as a bdd. This is different than taking a
	 * BDDiterator (bdd.iterator(bdd.support())) and iterating over it, since, in
	 * that case, the "complete" (wrt support()) models would be output
	 * 
	 * Example:
	 * Vars = { 0, 1, .., 5 } // p, q, r, s, t, u
	 * F = (p OR q) AND NOT r
	 * 
	 * Computing the BDDIterator and "running" it whould output
	 * <0:0, 1:1, 2:0> <0:1, 1:0, 2:0> <0:1, 1:1, 2:0>
	 * That is, all solutions fully specify the truth value of all variables in
	 * the support set
	 * 
	 * On the other hand, separateSolution gives 
	 * <0:0, 1:1, 2:0> <0:1, 2:0>
	 * where the second solution is implied by both the second and the third 
	 * solution above.
	 * 
	 * The difference is much bigger if the support set is large although most
	 * solutions mention few variables
	 * 
	 * @param bdd the bdd to be studied
	 * @param set a set of int whose size is the total number of variables, and
	 * which is used to store temporary information through the recursive calls 
	 * @param cycleShare whether cyclicity or sharing is considered (this
	 * affects the BDDFactory to be used and the number of variables to be
	 * considered)
	 * @return a list of bdds, each one describing a solution as allSat would
	 * find it
	 */
	private ArrayList<BDD> separateSolutions(BDD bdd, int[] set, int cycleShare) {
		BDDFactory bf = getOrCreateFactory(entry)[cycleShare];
        int n;

        if (bdd.isZero())
            return new ArrayList<BDD>();
        else if (bdd.isOne()) {
            BDD acc = bf.one();
            for (n = 0; n < set.length; n++) {
                if (set[n] > 0) {
                	acc.andWith(set[n] == 2 ? bf.ithVar(bf.level2Var(n)) : bf.nithVar(bf.level2Var(n)));
                }
            }
            ArrayList<BDD> list = new ArrayList<BDD>();
            list.add(acc);
            return list;
        } else {
            set[bf.var2Level(bdd.var())] = 1;
            BDD bddl = bdd.low();
            ArrayList<BDD> listl = separateSolutions(bddl,set,cycleShare);
            bddl.free();

            set[bf.var2Level(bdd.var())] = 2;
            BDD bddh = bdd.high();
            ArrayList<BDD> listh = separateSolutions(bddh,set,cycleShare);
            bddh.free();
            
            listl.addAll(listh);

            set[bf.var2Level(bdd.var())] = 0;
            return listl;
        }
    }
	
	

}
