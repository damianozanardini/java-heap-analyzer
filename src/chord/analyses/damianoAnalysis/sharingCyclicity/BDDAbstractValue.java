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


// WARNING: for the time being, cyclicity is not supported
public class BDDAbstractValue extends AbstractValue {
	private Entry entry;
	
	protected static HashMap<Entry,BDDFactory[]> factories = new HashMap<Entry,BDDFactory[]>();
	protected static HashMap<Entry,BDDDomain[]> domains = new HashMap<Entry,BDDDomain[]>();

	private ShBDD sComp;
	
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
	
	/**
	 * Main constructor, creates a BDDAnstractValue object based on the
	 * entry information
	 * 
	 * @param entry the entry object needed to collect the information related
	 * to registers and fields
	 */
	public BDDAbstractValue(Entry e) {
		entry = e;
		sComp = new ShBDD(e);
	}

    public BDDAbstractValue(Entry e, BDD sc) {
		entry = e;
		sComp = new ShBDD(e,sc);
	}

    public BDDAbstractValue(Entry e, ShBDD sc) {
		entry = e;
		sComp = sc;
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
	 * @return an array of BDDDomain, being BDDDomain[CYCLE] and BDDDomain[SHARE] the 
	 * corresponding BDDDomain for each kind of information.
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
	public boolean updateSInfo(AbstractValue other) {
		if (other == null) return false;
		if (other instanceof BDDAbstractValue) {
			ShBDD sNew = ((BDDAbstractValue) other).getSComp();
			return sComp.updateSInfo(sNew);
		} else {
			Utilities.err("BDDAbstractValue.update: wrong type of parameter - " + other);
			return false;
		}
	}
		
	public boolean updateCInfo(AbstractValue other) {
		// TODO
		return false;
	}
		
	// WARNING: double-check it when the bdd implementation will work on its own
	public boolean updateAInfo(AbstractValue other) {
		return false;
	}
		
	// WARNING: double-check it when the bdd implementation will work on its own
	public boolean updatePInfo(AbstractValue other) {
		return false;
	}
		
	public void clearPInfo() {
		// TODO
	}
	
	/**
	 * Returns a new BDDAbstractValue object with the same abstract information.
	 * 
	 * @return a copy of itself
	 */
	public BDDAbstractValue clone() {
		return new BDDAbstractValue(entry, sComp.clone());
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
	public void addSInfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		sComp.addSInfo(r1,r2,fs1,fs2);
	}

	public void addCInfo(Register r, FieldSet fs) {
		// TODO
	}

	public void addAInfo(Register r1,Register r2) {
		// TODO
	}
	
	public void addPInfo(Register r) {
		// TODO
	}

	/**
	 * Copies the Cyclicity and Sharing information from one register to another
	 * and keeps both in the existing information.
	 * 
	 * @param source the original register to get the information to be copied
	 * @param dest the destination register for the information.
	 */
	public void copyInfo(Register source, Register dest) {
		copySInfo(source,dest);
		copyCInfo(source,dest);
	}
	
	// WARNING: from (source,source,fs1,fs2) both (dest,source,fs1,fs2) and 
	// (source,dest,fs1,fs2) are produced; this is redundant, but we can live with it
	public void copySInfo(Register source, Register dest) {
		sComp = sComp.copySharing(source,dest);
	}

	public void copyCInfo(Register source, Register dest) {
		// TODO
	}

	public void copyAInfo(Register source, Register dest) {
		// TODO
	}
	
	public void copyPInfo(Register source, Register dest) {
		// TODO
	}

	/**
	 * Moves the Cyclicity and Sharing information from one register to another.
	 * All the information referencing the source register will be deleted.
	 * 
	 * @param source the original register to get the information to be moved
	 * @param dest the destination register for the information.
	 */
	public void moveInfo(Register source, Register dest) {
		moveSInfo(source, dest);
		moveCInfo(source, dest);
	}

	/**
	 * Moves the Sharing information from one register to another.
	 * All the information about the source register will be deleted.
	 * 
	 * @param source the original (source) register from which the information is moved
	 * @param dest the destination register
	 */
	public void moveSInfo(Register source, Register dest) {
		sComp = sComp.renameSharing(source,dest);
	}

	public void moveCInfo(Register source, Register dest) {
		// TODO
	}

	public void moveAInfo(Register source, Register dest) {
		// TODO
	}

	public void movePInfo(Register source, Register dest) {
		// TODO
	}

	/**
	 * Removes the sharing and cyclicity information about a register. 
	 */
	public void removeInfo(Register r) {
		removeSInfo(r);
		removeCInfo(r);
		removeAInfo(r);
		removePInfo(r);
	}

	/**
	 * Removes the sharing information about a register. 
	 */
	public void removeSInfo(Register r) {
		sComp.removeSharing(r);
	}
	
	/**
	 * Removes the cyclicity information about a register. 
	 */
	public void removeCInfo(Register r) {
		// TODO
	}
	
	/**
	 * Removes the definite aliasing information about a register. 
	 */
	public void removeAInfo(Register r) {
		// TODO
	}

	/**
	 * Removes the purity information about a register. 
	 */
	public void removePInfo(Register r) {
		// TODO
	}

	private void printLines() {
		sComp.printLines();
	}
	
	public String toString() {
		return sComp.toString();
	}
		
	public boolean equals(AbstractValue av) {
		if (av instanceof BDDAbstractValue)
		{
			boolean checkSComp = sComp.equals(((BDDAbstractValue) av).getSComp());
			boolean checkCComp = true; // TODO
			return checkSComp && checkCComp;
		}
		else return false;
	}
	
	public boolean isTop() {
		return sComp.isTop(); // TODO && cComp.isTop();
	}

	public boolean isBottom() {
		return sComp.isBottom(); // TODO && cComp.isBottom();
	}

	public void copySInfoFromC(Register base, Register dest) {
		// TODO
	}
		
	public BDDAbstractValue doGetfield(Quad q, Register base,
			Register dest, jq_Field field) {
		// TODO
		return clone();
	}

	public BDDAbstractValue doPutfield(Quad q, Register v,
			Register rho, jq_Field field) {
		// TODO
		return clone();
		
		/*BDDFactory bf = getOrCreateFactory(entry)[SHARE];
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
		avIp.updateInfo(avIpp);
		return avIp;
		*/
	}

	public BDDAbstractValue doInvoke(Entry invokedEntry,
			Quad q, ArrayList<Register> actualParameters, Register returnValue) {
		// TODO
		return clone();
	}

	public ArrayList<Pair<FieldSet, FieldSet>> getStuples(Register r1, Register r2) {
		// TODO
		return null;
	}
	//		BDD sharing = sComp.restrictSharingOnBothRegisters(r1,r2).getData();
	//		ArrayList<BDD> list = separateSolutions(sharing,new int[nBDDVars_sh],SHARE);
	//		ArrayList<Pair<FieldSet, FieldSet>> pairs = new ArrayList<Pair<FieldSet, FieldSet>>();
	//		for (BDD b : list)
	//			pairs.add(bddToFieldSetPair(b));			
	//		return pairs;
	//}

	/**
	 * Takes a linear BDD corresponding to two FieldSets, and related to sharing,
	 * and returns the pair of FieldSet objects.
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
		// TODO
		return null;
	}
	
	/**
	 * Takes a linear BDD corresponding to a FieldSet, and related to cyclicity,
	 * and returns the FieldSet object
	 * 
	 * @param b the input BDD (no check is done that it is indeed a linear one)
	 * @return
	 */
	private FieldSet bddToFieldSet(BDD b) {
		BDDFactory bf = getOrCreateFactory(entry)[CYCLE];
		int[] vars = b.support().scanSet();
		int val = 0;
		for (int i=0; i<vars.length; i++) {
			if (b.and(bf.nithVar(i)).isZero()) val = val*2+1;
			else val = val*2;
		}
		return FieldSet.getByVal(val);
	}

	public ShBDD getSComp() {
		return sComp;
	}

	// WARNING: TO BE TESTED
	public void filterActual(Entry entry, List<Register> actualParameters) {
		// sharing
		sComp = sComp.filterActual(entry,actualParameters);
		// cyclicity
		// TODO
	}
	
	
}
