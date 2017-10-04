package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.project.ClassicProject;
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
 * Usually, the documentation of this class will use the term "variable" to refer to
 * "logical variable", "BDD variable" or "proposition" x_i. Instead, "register" will denote
 * a "program variable" at the level of Java Bytecode.  An index is the position of a variable
 * in the BDD representation (e.g., variable x_i has index i).
 *  
 * @author damiano
 *
 */
public class ShBDD {
		
	protected static BDDFactory factory = null;
	protected static BDDDomain domain = null;

	private BDD data;
	
	public BDD getData() {
		return data;
	}

	public void setData(BDD data) {
		this.data = data;
	}

	// number of reference registers
	private static int nRegisters = GlobalInfo.getNumberOfReferenceRegisters();
	// list of reference registers
	private static ArrayList<Register> registerList = GlobalInfo.getReferenceRegisterList();

	// number of bits necessary to represent nRegisters registers (n in the paper)
	private static int registerBitSize = (int) Math.ceil(Math.log(nRegisters) / Math.log(2));
	// number of bits necessary to represent all the fieldsets (m in the paper)
	private static int fieldBitSize = GlobalInfo.getNumberOfFields();
	// list of fields
	private static ArrayList<jq_Field> fieldList = GlobalInfo.getFieldList(); 
	
	// number of variables in the BDD (2*n + 2*m in the paper)
	private static int totalBitSize = 2*registerBitSize + 2*fieldBitSize;
	
	static final int LEFT = -1;
	static final int RIGHT = 1;
	
    public ShBDD(BDD sc) {
		// create domain unless it already exists
		getOrCreateDomain();
		// create factory unless it already exists 
		getOrCreateFactory();
		setData(sc);
	}

	/**
	 * Main constructor: it creates a ShBDD object based on the entry information.
	 * 
	 * @param entry the entry object needed to collect the information related
	 * to registers and fields
	 */
	public ShBDD() {
		// create domain unless it already exists
		getOrCreateDomain();
		// create factory unless it already exists 
		setData(getOrCreateFactory().zero());
	}

	/**
	 * Gets the BDDFactory or creates a new one.
	 *  
	 * @return a BDDFactory
	 */
	private static BDDFactory getOrCreateFactory() {
		if (factory == null) {
			factory = BDDFactory.init("java",1000, 1000);
			factory.setVarNum(totalBitSize);
		}
		return factory;
	}

	/**
	 * Gets the BDDDomain or creates a new one.
	 * <p>
	 * In order to be able to deal with more than 64 variables, a BigInteger is used
	 * to encode the argument for the domain.
	 * 
	 * @return a BDDDomain
	 */
	private static BDDDomain getOrCreateDomain() {
		if (domain == null) {
			int nBytes = totalBitSize/8+1;
			byte[] bytes = new byte[nBytes];
			for (int i=nBytes-1; i>0; i--) bytes[i] = 0;
			bytes[0] = (byte) (1 << (totalBitSize % 8));
			domain = getOrCreateFactory().extDomain(new BigInteger(1,bytes));;
		}
		return domain;
	}

	/**
	 * Joins the existing information with new one.  Join is disjunction.
	 * 
	 * @param other
	 * @return whether the information has changed
	 */
	public boolean update(ShBDD other) {
		if (other == null) return false;
		// inclusion test
		if (other.getData().imp(getData()).isOne()) return false;
		// the call to id() is needed in order not to consume other.data
		getData().orWith(other.getData().id());
		return true;
	}
	
	/**
	 * Returns a new ShBDD object with the same abstract information.
	 * 
	 * @return a copy of itself
	 */
	public ShBDD clone() {
		return new ShBDD(getData().id());
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
	 * @return whether some new information is added
	 */
	public boolean addInfo(Register r1, Register r2, FieldSet fs1, FieldSet fs2) {
		BigInteger bint = BigInteger.valueOf((long) fs2.getVal());
		bint = bint.add(BigInteger.valueOf((long) fs1.getVal()).shiftLeft(fieldBitSize));
		bint = bint.add(BigInteger.valueOf((long) registerList.indexOf(r1)).shiftLeft(2*fieldBitSize));
		bint = bint.add(BigInteger.valueOf((long) registerList.indexOf(r2)).shiftLeft(2*fieldBitSize+registerBitSize));
		int bl = totalBitSize;
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
		// inclusion test (false is returns only if we are sure that the added information is not new)
		if (newBDDSEntry.imp(data).isOne()) 
			return false;
		else {
			data.orWith(newBDDSEntry);
			return false;
		}
	}
		
	/**
	 * Returns a new ShBDD object containing the linear BDD which is the conjunction of
	 * propositions with indices from lower to upper, all non-negated.
	 * 
	 * @param lower
	 * @param upper
	 * @return
	 */
	private static ShBDD indexIntervalToBDD(int lower,int upper) {
		BDD x = getOrCreateFactory().one();
		for (int i=lower; i<upper; i++) {
			x.andWith(getOrCreateFactory().ithVar(i));
		}
		return new ShBDD(x);
	}

	/**
	 * Returns a new ShBDD object containing the linear BDD which is the conjunction of
	 * propositions with indices from the given list, all non-negated.
	 * 
	 * @param list
	 * @return
	 */
	private BDD indexListToBDD(ArrayList<Integer> list) {
		BDD x =  getOrCreateFactory().one();
		for (int index : list) x.andWith(getOrCreateFactory().ithVar(index));
		return x;
	}
	
	/**
	 * This method assumes that b is linear: a conjunction of ithVars() or nIthVars()
	 * of variables with consecutive indexes, and returns an int whose LAST nBits
	 * bits contains the "truth value" of each variable involved.
	 * 
	 * @param b
	 * @param lower
	 * @param upper
	 * @return
	 */
	private int BDDtoInt(BDD b, int lower, int upper) {
		int[] vars = b.support().scanSet();
		// TODO se puede sacar con getVarIndeces, que te lo da con el orden al reves.
		// BigInteger [] varIndices = getOrCreateDomain().getVarIndices(b, 1);
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
			boolean isHere = b.exist(indexListToBDD(l)).restrict(getOrCreateFactory().ithVar(i)).isOne();
			acc = 2 * acc + (isHere ? 1 : 0);
		}		
		return acc;
	}
	
	/**
	 * This method assumes that b is linear: a conjunction of ithVars() or nIthVars()
	 * of variables with consecutive indexes, and returns an a list of bools
	 * containing the "truth value" of each variable involved.
	 * 
	 * @param b
	 * @param lower
	 * @param upper
	 * @return
	 */
	private ArrayList<Boolean> BDDtoBools(BDD b, int lower, int upper) {
		ArrayList<Boolean> bools = new ArrayList<Boolean>();
		int bits = BDDtoInt(b,lower,upper);
		for (int i=0; i<upper-lower; i++) {
			bools.add(0,bits%2==1);
			bits = bits >>> 1;
		}
		return bools;
	}
	
	/**
	 * Returns a linear BDD encoding the bitwise representation of a register. Depending on leftRight,
	 * the BDD can either involve the first n indexes (leftRight==LEFT) or the second n indexes
	 * (leftRight==RIGHT).
	 * 
	 * @param r
	 * @param leftRight
	 * @return
	 */
	private static ShBDD registerToBDD(Register r,int leftRight) {
		int id = registerList.indexOf(r);
		BDD b = getOrCreateFactory().one();
		int offset = (leftRight==LEFT) ? 0 : registerBitSize;
		for (int i = offset+registerBitSize-1; i>=offset; i--) {
			if (id%2 == 1) b.andWith(getOrCreateFactory().ithVar(i));
			else b.andWith(getOrCreateFactory().nithVar(i));
			id /= 2;
		}
		return new ShBDD(b);
	}
	
	/**
	 * Returns a linear BDD encoding the bitwise representation of a fieldSet. Depending on leftRight,
	 * the BDD can either involve the first indexes from 2n to (2n+m-1) (if leftRight==LEFT) or indexes
	 * from (2n+m) to (2n+2m-1) (if leftRight==RIGHT).
	 * 
	 * @param r
	 * @param leftRight
	 * @return
	 */
	public static ShBDD fieldSetToBDD(FieldSet fs,int leftRight) {
		int id = fs.getVal();
		BDD b = getOrCreateFactory().one();
		int offset = (leftRight==LEFT) ? 2*registerBitSize : 2*registerBitSize+fieldBitSize;
		for (int i = offset+fieldBitSize-1; i>=offset; i--) {
			if (id%2 == 1) b.andWith(getOrCreateFactory().ithVar(i));
			else b.andWith(getOrCreateFactory().nithVar(i));
			id /= 2;
		}
		return new ShBDD(b);		
	}
	
	/**
	 * Returns a BDD encoding the given field.
	 * 
	 * @param fld
	 * @param leftRight
	 * @return
	 */
	public static ShBDD fieldToBDD(jq_Field fld,int leftRight) {
		int i = fieldToIndex(fld,leftRight);
		return new ShBDD(getOrCreateFactory().ithVar(i));
	}
	
	/**
	 * Returns the index corresponding to a given field, the left or right flavor, depending on
	 * leftRight.
	 * 
	 * @param fld
	 * @param leftRight
	 * @return
	 */
	public static int fieldToIndex(jq_Field fld,int leftRight) {
		int i = fieldList.indexOf(fld) + ((leftRight==LEFT)? registerBitSize : 2*registerBitSize);
		return i;
	}

	/**
	 * Returns a BDD representing the given index, non-negated.
	 * 
	 * @param index
	 * @return
	 */
	public ShBDD indexToBDD(int index) {
		return new ShBDD(getOrCreateFactory().ithVar(index));
	}
	
	/**
	 * Equality on BDDS is double implication.
	 * 
	 * @param other
	 * @return
	 */
	public boolean equals(ShBDD other) {
		return data.biimp(((ShBDD) other).getData()).isOne();
	}
	
	public boolean isTop() {
		return data.isOne();
	}

	public boolean isBottom() {
		return data.isZero();
	}
	
	/**
	 * Takes a linear BDD corresponding to two FieldSets, and returns the pair of
	 * FieldSet objects.
	 * 
	 * @param b the input BDD (no check is done that it is indeed a linear one)
	 * @return
	 */
	private Pair<FieldSet,FieldSet> bddToFieldSetPair(BDD b) {
		BDDFactory bf = getOrCreateFactory();
		int[] vars = b.support().scanSet();
		// first FieldSet
		int val = 0;
		for (int i=0; i<vars.length/2; i++) {
			if (b.and(bf.nithVar(i)).isZero()) val = val*2+1;
			else val = val*2;
		}
		FieldSet fs1 = FieldSet.getByVal(val);
		// second FieldSet
		val = 0;
		for (int i=vars.length/2; i<vars.length; i++) {
			if (b.and(bf.nithVar(i)).isZero()) val = val*2+1;
			else val = val*2;
		}
		FieldSet fs2 = FieldSet.getByVal(val);
		return new Pair<FieldSet,FieldSet>(fs1,fs2);
	}
	
	/**
	 * The standard AND operator brought to the level of ShBDD.
	 * 
	 * @param other
	 * @return
	 */
	public ShBDD and(ShBDD other) {
		return new ShBDD(data.and(other.getData()));
	}
	
	/**
	 * The standard AND operator brought to the level of ShBDD.
	 * This version takes as argument a BDD instead of a ShBDD.
	 * 
	 * @param other
	 * @return
	 */
	public ShBDD and(BDD other) {
		return new ShBDD(data.and(other));
	}

	/**
	 * The standard OR operator brought to the level of ShBDD.
	 * 
	 * @param other
	 * @return
	 */
	public ShBDD or(ShBDD other) {
		return new ShBDD(data.or(other.getData()));
	}
	
	/**
	 * The standard OR operator brought to the level of ShBDD.
	 * This version takes as argument a BDD instead of a ShBDD.
	 * 
	 * @param other
	 * @return
	 */
	public ShBDD or(BDD other) {
		return new ShBDD(data.or(other));
	}

	/**
	 * The standard NOT operator brought to the level of ShBDD.
	 * 
	 * @param other
	 * @return
	 */
	public ShBDD not() {
		return new ShBDD(data.not());
	}

	/**
	 * The standard ANDwith operator brought to the level of ShBDD.
	 * 
	 * @param other
	 * @return
	 */
	public void andWith(ShBDD other) {
		data.andWith(other.getData());
	}
	
	/**
	 * The standard ANDwith operator brought to the level of ShBDD.
	 * This version takes as argument a BDD instead of a ShBDD.
	 * 
	 * @param other
	 * @return
	 */
	public void andWith(BDD other) {
		data.andWith(other);
	}
	
	/**
	 * The standard ORwith operator brought to the level of ShBDD.
	 * 
	 * @param other
	 * @return
	 */
	public void orWith(ShBDD other) {
		data.orWith(other.getData());
	}
	
	/**
	 * The standard ORwith operator brought to the level of ShBDD.
	 * This version takes as argument a BDD instead of a ShBDD.
	 * 
	 * @param other
	 * @return
	 */
	public void orWith(BDD other) {
		data.orWith(other);
	}

	/**
	 * The NOT operator brought to the level of ShBDD.
	 * This version updates the current ShBDD object instead or returning a
	 * new object as result.
	 * 
	 * @param other
	 * @return
	 */
	public void notWith() {
		data = data.not();
	}
	
	/**
	 * Computes the formula I(v,_) = I AND Lv as a new ShBDD object.
	 * TODO WARNING: try the built-in restrict and restrictWith methods of class BDD
	 * 
	 * @param r
	 * @return
	 */
	public ShBDD restrictOnFirstRegister(Register r) {
		return this.and(lv(r));
	}

	/**
	 * Computes the formula I(v,_) = I AND Lv and NOT Rv as a new ShBDD object.
	 * PAPER: this is probably the operator we want, not the "inclusive"
	 * restrictOnFirstRegister.
	 * 
	 * @param r
	 * @return
	 */
	public ShBDD restrictOnFirstRegisterOnly(Register r) {
		return this.and(lv(r)).and(rv(r).not());
	}

	/**
	 * Computes the formula I(_,v) = I AND Rv as a new ShBDD object.
	 * 
	 * @param r
	 * @return
	 */
	public ShBDD restrictOnSecondRegister(Register r) {
		return this.and(rv(r));		
	}
	
	/**
	 * Computes the formula I(_,v) = I AND NOT Lv and Rv as a new ShBDD object.
	 * PAPER: this is probably the operator we want, not the "inclusive"
	 * restrictOnSecondRegister.
	 * 
	 * @param r
	 * @return
	 */
	public ShBDD restrictOnSecondRegisterOnly(Register r) {
		return this.and(lv(r).not()).and(rv(r));
	}

	/**
	 * Computes the formula I(v1,v2) = I AND Lv1 AND Rv2 as a new ShBDD object.
	 * Registers are sorted before.
	 * 
	 * @param r1
	 * @param r2
	 * @return
	 */
	public ShBDD restrictOnBothRegisters(Register r1, Register r2) {
		return this.and(lv(Utilities.minReg(r1,r2))).and(rv(Utilities.maxReg(r1,r2)).getData());
	}

	/**
	 * Computes the formula I(v) = I(v,_) OR I(_,v) as a new ShBDD object.
	 * 
	 * @param r
	 * @return
	 */
	public ShBDD restrictOnRegister(Register r) {
		return restrictOnFirstRegister(r).or(restrictOnSecondRegister(r));
	}

	/**
	 * Removes the sharing information about r.  A new ShBDD object is returned.
	 * 
	 * @param r
	 * @return
	 */
	public ShBDD remove(Register r) {
		return this.and(restrictOnRegister(r).not());
	}
	
	/**
	 * Removes the sharing information about r.  The current ShBDD object is modified.
	 * 
	 * @param r
	 * @return
	 */
	public void removeWith(Register r) {
		this.andWith(restrictOnRegister(r).not());
	}

	/**
	 * Moves sharing information about source into sharing information about dest.
	 * A new ShBDD object is returned.
	 * 
	 * @param source
	 * @param dest
	 * @return
	 */
	public ShBDD rename(Register source,Register dest) {
		Utilities.info("[BDD OPS] MOVING " + source + " INTO " + dest);
		ShBDD rest = this.and(restrictOnRegister(source).not());
		BDD x2 = restrictOnRegister(source).existLR().restrictOnBothRegisters(dest,dest).getData();
		BDD x3 = restrictOnFirstRegisterOnly(source).existL().restrictOnFirstRegister(dest).getData();		
		BDD x4 = restrictOnSecondRegisterOnly(source).existR().restrictOnSecondRegister(dest).getData();
		// orWith is used because it seems to be more efficient (all BDDs but the result are consumed)
		return rest.or(x2).or(x3).or(x4);
	}
	
	/**
	 * Moves sharing information about source into sharing information about dest.
	 * The current ShBDD object is modified.
	 * 
	 * @param source
	 * @param dest
	 * @return
	 */
	public void renameWith(Register source,Register dest) {
		Utilities.info("[BDD OPS] MOVING " + source + " INTO " + dest);
		BDD x2 = restrictOnRegister(source).existLR().restrictOnBothRegisters(dest,dest).getData();
		BDD x3 = restrictOnFirstRegisterOnly(source).existL().restrictOnFirstRegister(dest).getData();		
		BDD x4 = restrictOnSecondRegisterOnly(source).existR().restrictOnSecondRegister(dest).getData();
		this.andWith(restrictOnRegister(source).not());
		this.orWith(x2);
		this.orWith(x3);
		this.orWith(x4);
	}

	/**
	 * Copies sharing information about source into sharing information about dest.
	 * A new ShBDD object is returned.
	 * 
	 * @param source
	 * @param dest
	 * @return
	 */
	public ShBDD copy(Register source, Register dest) {
		Utilities.info("[BDD OPS] COPYING " + source + " INTO " + dest);
		BDD x2 = restrictOnRegister(source).existLR().restrictOnBothRegisters(dest,dest).getData();
		BDD x3 = restrictOnFirstRegister(source).existL().restrictOnFirstRegister(dest).getData();		
		BDD x4 = restrictOnSecondRegister(source).existR().restrictOnSecondRegister(dest).getData();
		return this.or(x2).or(x3).or(x4);
	}

	/**
	 * Copies sharing information about source into sharing information about dest.
	 * The current ShBDD object is modified.
	 * 
	 * @param source
	 * @param dest
	 * @return
	 */
	public void copyWith(Register source, Register dest) {
		Utilities.info("[BDD OPS] COPYING " + source + " INTO " + dest);
		BDD x2 = restrictOnRegister(source).existLR().restrictOnBothRegisters(dest,dest).getData();
		BDD x3 = restrictOnFirstRegister(source).existL().restrictOnFirstRegister(dest).getData();		
		BDD x4 = restrictOnSecondRegister(source).existR().restrictOnSecondRegister(dest).getData();
		this.orWith(x2);
		this.orWith(x3);
		this.orWith(x4);
	}

	/**
	 * Only keeps information about the actual parameters.
	 * 
	 * @param entry
	 * @param actualParameters
	 * @return
	 */
	public void filterActual(List<Register> actualParameters) {
		BDD bdd1 = getOrCreateFactory().zero();
		BDD bdd2 = getOrCreateFactory().zero();
		for (Register ap : actualParameters) {
			bdd1.orWith(registerToBDD(ap,LEFT).data);
			bdd2.orWith(registerToBDD(ap,RIGHT).data);
		}
		data.andWith(bdd1);
		data.andWith(bdd2);
	}
	
	/**
	 * Returns the formula encoding the bitwise representation of register r by
	 * using the first n variables.  A new ShBDD object is created.
	 * 
	 * @param r
	 * @return
	 */
	private static ShBDD lv(Register r) {
		return registerToBDD(r,LEFT);
	}
	
	/**
	 * Returns the formula encoding the bitwise representation of register r by
	 * using the second n variables.  A new ShBDD object is created.
	 * 
	 * @param r
	 * @return
	 */
	private ShBDD rv(Register r) {
		return registerToBDD(r,RIGHT);
	}
	
	/**
	 * Existential quantification. A new ShBDD object is returned.
	 * 
	 * @return
	 */
	public ShBDD exist(ShBDD shbdd) {
		return new ShBDD(data.exist(shbdd.getData()));
	}

	/**
	 * Existential quantification. A new ShBDD object is returned.
	 * 
	 * @return
	 */
	public ShBDD exist(BDD bdd) {
		return new ShBDD(data.exist(bdd));
	}
	
	/**
	 * Existential quantification. The current ShBDD object is modified.
	 * 
	 * @return
	 */
	public void existWith(ShBDD shbdd) {
		data = exist(shbdd).data;
	}

	/**
	 * Existential quantification. The current ShBDD object is modified.
	 * 
	 * @return
	 */
	public void existWith(BDD bdd) {
		data = exist(bdd).data;
	}

	/**
	 * Existential quantification on the first n variables.
	 * A new ShBDD object is returned.
	 * 
	 * @return
	 */
	public ShBDD existL() {
		return exist(indexIntervalToBDD(0,registerBitSize));
	}

	/**
	 * Existential quantification on the first n variables.
	 * The current ShBDD object is modified.
	 * 
	 * @return
	 */
	public void existLwith() {
		data = exist(indexIntervalToBDD(0,registerBitSize)).getData();
	}

	/**
	 * Existential quantification on the second n variables.
	 * A new ShBDD object is returned.
	 * 
	 * @return
	 */
	public ShBDD existR() {
		return exist(indexIntervalToBDD(registerBitSize,2*registerBitSize));
	}
	
	/**
	 * Existential quantification on the second n variables.
	 * The current ShBDD object is modified.
	 * 
	 * @return
	 */
	public void existRwith() {
		data = exist(indexIntervalToBDD(registerBitSize,2*registerBitSize)).getData();
	}

	/**
	 * Existential quantification on the first 2n variables.
	 * A new ShBDD object is returned.
	 * 
	 * @return
	 */
	public ShBDD existLR() {
		return exist(indexIntervalToBDD(0,2*registerBitSize));
	}

	/**
	 * Existential quantification on the first 2n variables.
	 * The current ShBDD object is modified.
	 * 
	 * @return
	 */
	public void existLRwith() {
		data = exist(indexIntervalToBDD(0,2*registerBitSize)).getData();
	}
	
	/**
	 * If-then-else operator at the level of the ShBDD class.
	 * @param bdd1
	 * @param bdd2
	 * @return
	 */
	public ShBDD ite(ShBDD bdd1, ShBDD bdd2) {
		return new ShBDD(data.ite(bdd1.getData(),bdd2.getData()));
	}
	
	/**
	 * Implementation of the oplus1 operator (field addition).
	 * 
	 * @param field
	 * @return
	 */
	public ShBDD addField1(jq_Field fld) {
		ShBDD x = fieldToBDD(fld,LEFT);
		ShBDD ex = this.exist(x);
		ShBDD z = fieldSetToBDD(FieldSet.emptyset(),RIGHT);
		ex.andWith(z);
		ex.andWith(x);
		return this.or(ex);
	}

	/**
	 * Implementation of the oplus2 operator (field addition).
	 * 
	 * @param field
	 * @return
	 */
	public ShBDD addField2(jq_Field fld) {
		ShBDD x = fieldToBDD(fld,RIGHT);
		ShBDD ex = this.exist(x);
		ShBDD z = fieldSetToBDD(FieldSet.emptyset(),LEFT);
		ex.andWith(z);
		ex.andWith(x);
		return this.or(ex);
	}

	/**
	 * "In-place" implementation of the oplus1 operator (field addition).
	 * 
	 * @param field
	 * @return
	 */
	public void addField1with(jq_Field fld) {
		ShBDD x = fieldToBDD(fld,LEFT);
		ShBDD ex = this.exist(x);
		ShBDD z = fieldSetToBDD(FieldSet.emptyset(),RIGHT);
		ex.andWith(z);
		ex.andWith(x);
		this.orWith(ex);
	}

	/**
	 * "In-place" implementation of the oplus2 operator (field addition).
	 * 
	 * @param field
	 * @return
	 */
	public void addField2with(jq_Field fld) {
		ShBDD x = fieldToBDD(fld,RIGHT);
		ShBDD ex = this.exist(x);
		ShBDD z = fieldSetToBDD(FieldSet.emptyset(),LEFT);
		ex.andWith(z);
		ex.andWith(x);
		this.orWith(ex);
	}

	/**
	 * Implementation of the ominus (path-difference) operator.
	 */
	public ShBDD pathDifference(ArrayList<jq_Field> leftList, ArrayList<jq_Field> rightList) {
		BDD bddY = getOrCreateFactory().one();
		for (jq_Field fld : leftList) bddY.andWith(fieldToBDD(fld,LEFT).getData());
		for (jq_Field fld : rightList) bddY.andWith(fieldToBDD(fld,RIGHT).getData());
		return new ShBDD(data.and(bddY).exist(bddY));		
	}

	/**
	 * Implementation of the ominus (path-difference) operator with indexes instead of fields.
	 */
	public ShBDD pathDifference(ArrayList<Integer> list) {
		BDD bddY = getOrCreateFactory().one();
		for (int i : list) bddY.andWith(indexToBDD(i).data);
		return new ShBDD(data.and(bddY).exist(bddY));		
	}

	/**
	 * "In-place" implementation of the ominus (path-difference) operator.
	 */
	public void pathDifferenceWith(ArrayList<jq_Field> leftList, ArrayList<jq_Field> rightList) {
		BDD bddY = getOrCreateFactory().one();
		for (jq_Field fld : leftList) bddY.andWith(fieldToBDD(fld,LEFT).getData());
		for (jq_Field fld : rightList) bddY.andWith(fieldToBDD(fld,RIGHT).getData());
		this.andWith(bddY);
		this.existWith(bddY);		
	}

	/**
	 * "In-place" implementation of the ominus (path-difference) operator with indexes
	 * instead of fields.
	 */
	public void pathDifferenceWith(ArrayList<Integer> list) {
		BDD bddY = getOrCreateFactory().one();
		for (int i : list) bddY.andWith(indexToBDD(i).data);
		this.andWith(bddY);
		this.existWith(bddY);		
	}

	/**
	 * Returns the solutions of a bdd in form of a list of bdds. It does the
	 * same job as allSat, but no byte[] is returned (in that case, by nextSat);
	 * instead, each solution comes as a bdd. This is different than taking a
	 * BDDiterator (bdd.iterator(bdd.support())) and iterating over it, since,
	 * in that case, the "complete" (wrt support()) models would be output.
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
		BDDFactory bf = getOrCreateFactory();
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

	/**
	 * One of the key algorithms in the BDD implementation. This method takes
	 * two BDDs b1 and b2 and returns their "concatenation" b, i.e., the oplus
	 * operator is implemented here. Concatenation is something different from
	 * any standard logical operator I know. Its meaning is the following:
	 * whenever there is model (a truth assignment which makes the formula true)
	 * t1 of b1, and a model t2 of b2, then there is a model t of b such that
	 * A(t) = A(t1) \cup A(t2) where A(_) is the set of variables which are true
	 * in the given truth assignment.
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
	// TODO WARNING: this is the old implementation; the new one is below
	public BDD concatBDDsOld(BDD b1, BDD b2) {
		BDDFactory bf = getOrCreateFactory();
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
	 * Given a linear BDD lbdd, returns whether the i-th variable is true
	 * in lbdd.
	 *  
	 * @param lbdd
	 * @param i
	 * @return
	 */
	private static boolean isTrueVar(BDD lbdd, int i) {
		return lbdd.andWith(getOrCreateFactory().nithVar(i)).isZero();
	}
	
	/**
	 * Computes the linear BDD whose support is the set of field-related
	 * variables (the last 2m variables) which are true in bdd (which
	 * is also linear).
	 * 
	 * @param bdd
	 * @return
	 */
	private static BDD getTrueVarsLast2M(BDD bdd) {
		BDD x = getOrCreateFactory().one();
		for (int i=2*registerBitSize; i<totalBitSize; i++) {
			if (isTrueVar(bdd,i))
				x.andWith(getOrCreateFactory().ithVar(i));
		}
		return x;
	}
	
	public ShBDD concat(ShBDD other) {
		BDD bdd1 = this.data.id();
		BDD bdd2 = other.data.id();
		
		BDD temp = getOrCreateFactory().zero();
		// TODO WARNING: like this or using an iterator?
		ArrayList<BDD> other_solutions = separateSolutions(bdd2,new int[totalBitSize]);
		for (BDD w : other_solutions) {
			BDD fw = bdd1;
			
			// get (as a linear BDD) the set Y_w of variables which are true in w
			BDD y = getTrueVarsLast2M(w);
			
			BDD z = getOrCreateFactory().one();			
			for (int i=0; i<registerBitSize; i++) {
				if (isTrueVar(w,i))
					z.andWith(getOrCreateFactory().ithVar(i+registerBitSize));
				else
					z.andWith(getOrCreateFactory().nithVar(i+registerBitSize));
			}
			
			fw.andWith(z);
			fw.exist(w);
			fw.andWith(w);
			temp.orWith(fw);
		}
		return new ShBDD(temp);
	}

	public void concatWith(ShBDD other) {
		BDD bdd1 = this.data;
		BDD bdd2 = other.data.id();
		
		BDD temp = getOrCreateFactory().zero();
		// TODO WARNING: like this or using an iterator?
		ArrayList<BDD> other_solutions = separateSolutions(bdd2,new int[totalBitSize]);
		for (BDD w : other_solutions) {
			BDD fw = bdd1;
			
			// get (as a linear BDD) the set Y_w of variables which are true in w
			BDD y = getTrueVarsLast2M(w);
			
			BDD z = getOrCreateFactory().one();			
			for (int i=0; i<registerBitSize; i++) {
				if (isTrueVar(w,i))
					z.andWith(getOrCreateFactory().ithVar(i+registerBitSize));
				else
					z.andWith(getOrCreateFactory().nithVar(i+registerBitSize));
			}
			
			fw.andWith(z);
			fw.exist(w);
			fw.andWith(w);
			temp.orWith(fw);
		}
		data = temp;
	}

	/**
	 * The P_F operator for a single model.
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	public static ShBDD pathFormulaToBDD(FieldSet left,FieldSet right) {
		return fieldSetToBDD(left,LEFT).and(fieldSetToBDD(right,RIGHT));
	}
	
	/**
	 * The F operator from the paper (Figure 8). It is a recursive procedure whose
	 * base case invokes capitalG (the G operator). The "this" argument is meant to be
	 * I''_s while bdd (first formal parameter) is meant to be I_s. The recursive scheme
	 * has been reversed (from 0 to m-1, instead of from m to 0) in order to keep
	 * fieldBitSize a private field of ShBDD.
	 * 
	 * @param bdd
	 * @param vi
	 * @param vj
	 * @param k
	 * @return
	 */
	public ShBDD capitalF(ShBDD bdd,Register vi, Register vj, int k) {
		if (k==fieldBitSize) return this.capitalG(bdd,vi,vj,0);
		int kk = fieldBitSize-k;
		ShBDD x = bdd.restrictOnFirstRegister(vj).and(indexToBDD(2*registerBitSize+kk));
		// recursive call
		ShBDD rec = capitalF(bdd,vi,vj,k+1);
		ArrayList<Integer> l = new ArrayList<Integer>();
		l.add(2*registerBitSize+fieldBitSize+kk);
		return x.ite(rec.pathDifference(l),rec);
	}

	/**
	 * The G operator from the paper (Figure 8). It is a recursive procedure.  The "this"
	 * argument is supposed to be I''_s while bdd (first formal parameter) is supposed to be
	 * I_s. The recursive scheme has been reversed (from 0 to m-1, instead of from m to 0)
	 * in order to keep fieldBitSize a private field of ShBDD.
	 * 
	 * @param bdd
	 * @param vi
	 * @param vj
	 * @param k
	 * @return
	 */
	private ShBDD capitalG(ShBDD bdd,Register vi, Register vj, int k) {
		if (k==fieldBitSize) return this.restrictOnBothRegisters(vi,vj); 
		int kk = fieldBitSize-k;
		ShBDD x = bdd.restrictOnSecondRegister(vi).and(indexToBDD(2*registerBitSize+fieldBitSize+kk));
		// recursive call
		ShBDD rec = capitalG(bdd,vi,vj,k+1);
		ArrayList<Integer> l = new ArrayList<Integer>();
		l.add(2*registerBitSize+kk);
		return x.ite(rec.pathDifference(l),rec);
	}

	/**
	 * The H operator from the paper (Figure 8), in the left version. It is a recursive
	 * procedure. The "this" argument is meant to be I''_s while bdd (first formal
	 * parameter) is meant to be I_s.
	 * The recursive scheme has been reversed (from 0 to m-1, instead of from m to 0) in
	 * order to keep fieldBitSize a private field of ShBDD.
	 * 
	 * @param bdd
	 * @param vi
	 * @param ret
	 * @param k
	 * @return
	 */
	public ShBDD capitalHl(ShBDD bdd,Register vi, Register ret, int k) {
		if (k==fieldBitSize) return bdd.restrictOnFirstRegister(vi);
		int kk = fieldBitSize-k;
		ShBDD x = this.restrictOnBothRegisters(ret,vi).and(indexToBDD(2*registerBitSize+fieldBitSize+kk));
		// recursive call
		ShBDD rec = capitalHl(bdd,vi,ret,k+1);
		ArrayList<Integer> l = new ArrayList<Integer>();
		l.add(2*registerBitSize+kk);
		return x.ite(rec.pathDifference(l),rec);
	}

	/**
	 * The H operator from the paper (Figure 8), in the right version. It is a recursive
	 * procedure. The "this" argument is meant to be I''_s while bdd (first formal
	 * parameter) is meant to be I_s.
	 * The recursive scheme has been reversed (from 0 to m-1, instead of from m to 0) in
	 * order to keep fieldBitSize a private field of ShBDD.
	 * 
	 * @param bdd
	 * @param vi
	 * @param ret
	 * @param k
	 * @return
	 */
	public ShBDD capitalHr(ShBDD bdd,Register vi, Register ret, int k) {
		if (k==fieldBitSize) return bdd.restrictOnSecondRegister(vi);
		int kk = fieldBitSize-k;
		ShBDD x = this.restrictOnBothRegisters(vi,ret).and(indexToBDD(2*registerBitSize+kk));
		// recursive call
		ShBDD rec = capitalHr(bdd,vi,ret,k+1);
		ArrayList<Integer> l = new ArrayList<Integer>();
		l.add(2*registerBitSize+fieldBitSize+kk);
		return x.ite(rec.pathDifference(l),rec);
	}

	public void printLines() {
		Utilities.begin("PRINTING ShBDD SOLUTIONS");
		BDD toIterate = indexIntervalToBDD(0,totalBitSize).getData();
		BDDIterator it = data.iterator(toIterate);	
		while (it.hasNext()) {
			BDD b = (BDD) it.next();
			Utilities.info(b.toString());
		}
		Utilities.end("PRINTING ShBDD SOLUTIONS");
	}

	/**
	 * The usual toString method. It is designed to get the same output as its tuples counterpart.
	 * 
	 * @return
	 */
	public String toString() {
		if (data.isZero()) return "false";
		String sS = "";
		// the iterator is supposed to refer to ALL variables, i.e., we want to explicitly
		// compute all the complete models (as in the tuples implementation)
		BDD toIterate = indexIntervalToBDD(0,totalBitSize).getData();
		BDDIterator it = getData().iterator(toIterate);
		// for each model
		while (it.hasNext()) {
			BDD b = (BDD) it.next();
			// only the "bits" of the first register
			BDD bdd_r1 = b.exist(indexIntervalToBDD(registerBitSize,totalBitSize).getData());
			int bits1 = BDDtoInt(bdd_r1,0,registerBitSize);
			Register r1 = GlobalInfo.getNthReferenceRegister(bits1);
			// only the "bits" of the second register 
			BDD bdd_r2 = b.exist(indexIntervalToBDD(0,registerBitSize).getData()).exist(indexIntervalToBDD(2*registerBitSize,totalBitSize).getData());
			int bits2 = BDDtoInt(bdd_r2,registerBitSize,2*registerBitSize);	
			Register r2 = GlobalInfo.getNthReferenceRegister(bits2);
			
			if (Utilities.leqReg(r1, r2)) {
				// only the "bits" of the first fieldset
				BDD bdd_fs1 = b.exist(indexIntervalToBDD(0,2*registerBitSize).getData()).exist(indexIntervalToBDD(2*registerBitSize+fieldBitSize,totalBitSize).getData());
				// only the "bits" of the second fieldset 
				BDD bdd_fs2 = b.exist(indexIntervalToBDD(0,2*registerBitSize+fieldBitSize).getData());
				
				sS = sS + "(" + r1.toString() + "," + r2.toString() + ",{";
				ArrayList<Boolean> bools1 = BDDtoBools(bdd_fs1,2*registerBitSize,2*registerBitSize+fieldBitSize);
				int j = 0;
				for (boolean x : bools1) {
					if (x) sS = sS + GlobalInfo.getNthField(j);
					j++;
				}
				sS = sS + "},{";
				ArrayList<Boolean> bools2 = BDDtoBools(bdd_fs2,2*registerBitSize+fieldBitSize,totalBitSize);
				j = 0;
				for (boolean x : bools2) {
					if (x) sS = sS + GlobalInfo.getNthField(j);
					j++;
				}
				sS = sS + "})" + (it.hasNext() ? " - " : "");
			}
		}
		// TODO check the result of this!
		Utilities.info("WITH VARINDICES: " + getOrCreateDomain().getVarIndices(data, 1));
		
		return sS;
	}
	
}
