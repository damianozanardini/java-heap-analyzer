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

/**
 * This class in basically a wrapper for a BDD object dealing with sharing information.
 * The need for such a class comes from tha fact that we can't declare a subclass of
 * BDD (this is so because of the JavaBDD implementation).
 * Therefore, this class is the best way we found to implement our specific operators
 * on BDDs.
 *  
 * @author damiano
 *
 */
public class ShBDD {
	private Entry entry;
	
	protected static HashMap<Entry,BDDFactory> factories = new HashMap<Entry,BDDFactory>();
	protected static HashMap<Entry,BDDDomain> domains = new HashMap<Entry,BDDDomain>();

	private BDD data;
	
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
	
	// number of variables in the BDD
	private int nBDDVars_sh;
	
	static final int LEFT = -1;
	static final int RIGHT = 1;
	
    public ShBDD(Entry e, BDD sc) {
		this(e);
		// WARNING: this overrides the assignments in this(e); it is done on
		// purpose, but a better way to do it could probably be found
		data = sc;
	}

	/**
	 * Main constructor, creates a ShBDD object based on the entry information
	 * 
	 * @param entry the entry object needed to collect the information related
	 * to registers and fields
	 */
	public ShBDD(Entry e) {
		entry = e;

		// compute relevant numbers
		registerBitSize = 0;
		fieldBitSize = 0;

		nRegisters = entry.getNumberOfReferenceRegisters();
		registerList = entry.getReferenceRegisters();
		for (int i=1; i<nRegisters; i*=2) { registerBitSize++; } 
		fieldBitSize = nFields;
		nBDDVars_sh = 2*registerBitSize + 2*fieldBitSize;
		// create domain unless it already exists
		getOrCreateDomain();
		// create factory unless it already exists 
		data = getOrCreateFactory(e).zero();
	}

	/**
	 * Gets a BDDFactory associated to a given entry or creates a new one
	 * <p>
	 * Each element of the array represents the Domain regarding Sharing or Cycle.
	 *  
	 * @param e the entry object needed to find the BDDFactory in the factories map
	 * @return a BDDFactory
	 */
	private BDDFactory getOrCreateFactory(Entry e) {
		if (factories.containsKey(e)) return factories.get(e);	
		BDDFactory sFactory = BDDFactory.init("java",1000, 1000);
		sFactory.setVarNum(nBDDVars_sh);
		factories.put(e,sFactory);
		return sFactory;
	}

	/**
	 * Gets a BDDDomain associated to the current entry or creates a new one
	 * <p>
	 * In order to be able to deal with more than 64 variables, a BigInteger is used
	 * to encode the argument for the domain.
	 * 
	 * @param e the entry object needed to find the BDDDomain in the factories map
	 * @return a BDDDomain
	 */
	private BDDDomain getOrCreateDomain() {
		if (!domains.containsKey(entry)) {
			int nBytes = nBDDVars_sh/8+1;
			byte[] bytes = new byte[nBytes];
			for (int i=nBytes-1; i>0; i--) bytes[i] = 0;
			bytes[0] = (byte) (1 << (nBDDVars_sh % 8));
			BDDDomain sDomain = getOrCreateFactory(entry).extDomain(new BigInteger(1,bytes));;
			domains.put(entry,sDomain);
			return sDomain;
		}
		return domains.get(entry);
	}

	/**
	 * Joins the existing information with new one.  Join is disjunction.
	 * 
	 * @param other
	 * @return whether the information has changed
	 */
	public boolean updateSInfo(ShBDD other) {
		if (other == null) return false;
		// inclusion test
		if (other.data.imp(data).isOne()) return false;
		// the call to id() is needed in order not to consume other.data
		data.orWith(other.data.id());
		return true;
	}
	
	/**
	 * Returns a new ShBDD object with the same abstract information.
	 * 
	 * @return a copy of itself
	 */
	public ShBDD clone() {
		return new ShBDD(entry,data.id());
	}
	
	/**
	 * Generates a new BDD describing the sharing information between the registers and
	 * fieldsets provided as parameters, and "adds" it (by logical disjunction)
	 * to the existing information.
	 * 
	 * @param r1 the first register
	 * @param r2 the second register
	 * @param fs1 the fieldset associated to r1
	 * @param fs2 the fieldset associated to r2
	 */
	public void addSInfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
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
		BDD newBDDSEntry = getOrCreateDomain().ithVar(bint);
		data.orWith(newBDDSEntry);
	}
		
	// WARNING: probably obsolete
	private BDD getSinfo(Register r1, Register r2) {
		return restrictSharingOnBothRegisters(r1,r2).data;
	}
	
	/*private void printLines() {
		Utilities.begin("PRINTING BDD SOLUTIONS");
		BDD toIterate = varIntervalToBDD(0,nBDDVars_sh, SHARE);
		BDDIterator it = sComp.iterator(toIterate);	
		while (it.hasNext()) {
			BDD b = (BDD) it.next();
			Utilities.info(b.toString());
		}
		Utilities.end("PRINTING BDD SOLUTIONS");
	}*/
	
	// TODO MIGUEL es curioso, pero si hago el toString al contrario (primero share) me da un problema con 
	// las factories y un nullpointer cuando llamo a support(), así como está funciona. Pero en el alguna operación del
	// to string de share se le hace algo a las factories que no le gusta a JavaBDD (no he sido capaz de encontrarlo aun)
	public String toString() {		
		String sS = "";
		BDD toIterate = varIntervalToBDD(0,nBDDVars_sh);
		BDDIterator it = data.iterator(toIterate);
		// Sharing information
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
			
			sS = sS + "(";
			int bits1 = 0; // BDDtoInt(r1,0,registerBitSize, SHARE);
			sS = sS + entry.getNthReferenceRegister(bits1).toString();
			sS = sS + ",";
			int bits2 = 0; // BDDtoInt(r2,registerBitSize,2*registerBitSize, SHARE);

			sS = sS + entry.getNthReferenceRegister(bits2).toString();
			sS = sS + ",{ ";
			ArrayList<Boolean> bools1 = BDDtoBools(fs1,2*registerBitSize,2*registerBitSize+fieldBitSize);
			int j = 0;
			for (boolean x : bools1) {
				if (x) sS = sS + GlobalInfo.getNthField(j);
				j++;
			}
			sS = sS + "},{ ";
			ArrayList<Boolean> bools2 = BDDtoBools(fs2,2*registerBitSize+fieldBitSize,nBDDVars_sh);
			j = 0;
			for (boolean x : bools2) {
				if (x) sS = sS + GlobalInfo.getNthField(j);
				j++;
			}
			sS = sS + "})" + (it.hasNext() ? " - " : "");
		}

		return sS;
	}
		
	private BDD varIntervalToBDD(int lower,int upper) {
		BDD x = getOrCreateFactory(entry).one();
		for (int i=lower; i<upper; i++) {
			x.andWith(getOrCreateFactory(entry).ithVar(i));
		}
		return x;
	}

	private BDD varListToBDD(ArrayList<Integer> list) {
		BDD x =  getOrCreateFactory(entry).one();
		for (int i : list) x.andWith(getOrCreateFactory(entry).ithVar(i));
		return x;
	}
	
	/**
	 * This method assumes that b is a conjunction of ithVars() or nIthVars() of
	 * variables with consecutive indexes, and returns an int whose last nBits bits
	 * contains the "truth value" of each variable involved.
	 * 
	 * @param b
	 * @return
	 */
	private int BDDtoInt(BDD b, int lower, int upper) {
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
			boolean isHere = b.exist(varListToBDD(l)).restrict(getOrCreateFactory(entry).ithVar(i)).isOne();
			acc = 2 * acc + (isHere ? 1 : 0);
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
	
	public boolean equals(ShBDD other) {
		return data.id().biimpWith(((ShBDD) other).data.id()).isOne();
	}
	
	public boolean isTop() {
		return data.isOne();
	}

	public boolean isBottom() {
		return data.isZero();
	}
	
	public ArrayList<Pair<FieldSet, FieldSet>> getStuples(Register r1, Register r2) {
		BDD sharing = getSinfo(r1,r2);
		ArrayList<BDD> list = separateSolutions(sharing,new int[nBDDVars_sh]);
		ArrayList<Pair<FieldSet, FieldSet>> pairs = new ArrayList<Pair<FieldSet, FieldSet>>();
		for (BDD b : list)
			pairs.add(bddToFieldSetPair(b));			
		return pairs;
	}

	/**
	 * Takes a linear BDD corresponding to two FieldSets, and related to sharing,
	 * and returns the pair of FieldSet objects.
	 * 
	 * @param b the input BDD (no check is done that it is indeed a linear one)
	 * @return
	 */
	private Pair<FieldSet,FieldSet> bddToFieldSetPair(BDD b) {
		BDDFactory bf = getOrCreateFactory(entry);
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
	private BDD concatBDDs(BDD b1, BDD b2) {
		BDDFactory bf = getOrCreateFactory(entry);
		ArrayList<BDD> bdds1 = separateSolutions(b1,new int[bf.varNum()]);
		ArrayList<BDD> bdds2 = separateSolutions(b2,new int[bf.varNum()]);
	
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
	 * Computing the BDDIterator and "running" it would output
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
	private ArrayList<BDD> separateSolutions(BDD bdd, int[] set) {
		BDDFactory bf = getOrCreateFactory(entry);
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
            ArrayList<BDD> listl = separateSolutions(bddl,set);
            bddl.free();

            set[bf.var2Level(bdd.var())] = 2;
            BDD bddh = bdd.high();
            ArrayList<BDD> listh = separateSolutions(bddh,set);
            bddh.free();
            
            listl.addAll(listh);

            set[bf.var2Level(bdd.var())] = 0;
            return listl;
        }
    }

	
	// Operators from the new version of the PAPER
	
	/**
	 * Computes the formula I(v,_) = I AND Lv as a new ShBDD object.
	 * 
	 * @param r
	 * @return
	 */
	public ShBDD restrictSharingOnFirstRegister(Register r) {
		return new ShBDD(entry,data.id().and(lv(r).data));
	}

	/**
	 * Computes the formula I(_,v) = I AND Rv as a new ShBDD object.
	 * 
	 * @param r
	 * @return
	 */
	public ShBDD restrictSharingOnSecondRegister(Register r) {
		return new ShBDD(entry,data.id().and(rv(r).data));		
	}
	
	/**
	 * Computes the formula I(v1,v2) = I AND Lv1 AND Rv2 as a new ShBDD object.
	 * Registers are sorted before.
	 * 
	 * @param r1
	 * @param r2
	 * @return
	 */
	public ShBDD restrictSharingOnBothRegisters(Register r1, Register r2) {
		return new ShBDD(entry,data.id().and(lv(Utilities.minReg(r1,r2)).data).and(rv(Utilities.maxReg(r1,r2)).data));		
	}

	/**
	 * Computes the formula I(v) = I(v,_) OR I(_,v) as a new ShBDD object.
	 * 
	 * @param r
	 * @return
	 */
	public ShBDD restrictSharingOnRegister(Register r) {
		return new ShBDD(entry,restrictSharingOnFirstRegister(r).data.or(restrictSharingOnSecondRegister(r).data));		
	}

	/**
	 * Removes the sharing information about r.  A new ShBDD object is returned.
	 * 
	 * @param r
	 * @return
	 */
	public ShBDD removeSharing(Register r) {
		return new ShBDD(entry,data.id().and(restrictSharingOnRegister(r).data.not()));
	}
	
	/**
	 * Moves sharing information about source into sharing information about dest.
	 * A new ShBDD object is returned.
	 * 
	 * @param source
	 * @param dest
	 * @return
	 */
	public ShBDD renameSharing(Register source,Register dest) {
		BDD x1 = data.id().and(restrictSharingOnRegister(source).data.not());
		BDD x2 = restrictSharingOnRegister(source).existsLR().restrictSharingOnBothRegisters(dest,dest).data;
		BDD x3 = restrictSharingOnFirstRegister(source).existsL().restrictSharingOnFirstRegister(dest).data;		
		BDD x4 = restrictSharingOnSecondRegister(source).existsR().restrictSharingOnSecondRegister(dest).data;
		// orWith is used because it seems to be more efficient (all BDDs but the result are consumed)
		return new ShBDD(entry,x1.orWith(x2).orWith(x3).orWith(x4));
	}
	
	/**
	 * Returns the formula encoding the bitwise representation of register r by using the first n propositions.
	 * A new ShBDD object is created.
	 * 
	 * @param r
	 * @return
	 */
	private ShBDD lv(Register r) {
		return new ShBDD(entry,registerToBDD(r,LEFT));
	}
	
	/**
	 * Returns the formula encoding the bitwise representation of register r by using the second n propositions.
	 * A new ShBDD object is created.
	 * 
	 * @param r
	 * @return
	 */
	private ShBDD rv(Register r) {
		return new ShBDD(entry,registerToBDD(r,RIGHT));
	}
	
	/**
	 * Existential quantification on the first n propositions.
	 * A new ShBDD object is returned.
	 * 
	 * @return
	 */
	private ShBDD existsL() {
		return new ShBDD(entry,data.id().exist(varIntervalToBDD(0,registerBitSize)));
	}

	/**
	 * Existential quantification on the second n propositions.
	 * A new ShBDD object is returned.
	 * 
	 * @return
	 */
	private ShBDD existsR() {
		return new ShBDD(entry,data.id().exist(varIntervalToBDD(registerBitSize,2*registerBitSize)));
	}
	
	/**
	 * Existential quantification on the first 2n propositions.
	 * A new ShBDD object is returned.
	 * 
	 * @return
	 */
	private ShBDD existsLR() {
		return new ShBDD(entry,data.id().exist(varIntervalToBDD(0,2*registerBitSize)));
	}
	
}
