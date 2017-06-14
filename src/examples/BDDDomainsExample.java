package examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDD.AllSatIterator;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDDFactory;

public class BDDDomainsExample {
	// number of variables
	static int N;
	static int numberOfConditions;
	static BDDFactory B;
	static BDD[] X; /* BDD with conditions array */
	// This two will eventually turn into a Map. This matches the var number
	// with the array position
	static String[] vars = new String[] { "lparent", "lleft", "lright", "rparent", "rleft", "rright" };
	static List<String> varList = Arrays.asList(vars);
	static BDDFactory bdd;
	static int n = 3; // 3x3 cube
	static int k = 6; // number of moves
	static int numregs = 6;
	static int numFields = 3;
	static int firstRegisterOffset =0;
	static int secondRegisterOffset =0;
	static int firstFieldsOffset = 1;
	static int secondFieldsOffset =1;
	static int registerSize =0;
	static int fieldSize =0;


	static void setRegistersBitOffset(){
		long nbits = (long) (Math.ceil(Math.log(numregs) / Math.log(2)));
		secondRegisterOffset = 1 << nbits;
		firstRegisterOffset = 0;
		registerSize = secondRegisterOffset;
	}
	static void setFieldsBitOffset(){
		long nbits = (long) (Math.ceil(Math.log(numFields) / Math.log(2)));
		fieldSize = 1 << nbits;
		firstFieldsOffset = secondRegisterOffset*secondRegisterOffset;
		secondFieldsOffset = firstFieldsOffset*fieldSize;
	}

	public static void main(String[] args) {
		// init(6, 10);
		bdd = BDDFactory.init("java", 1000000, 100000);
		bdd.setMaxIncrease(250000);

		// still doing this manually
		// 6 posibles valores para cada variable (0 a 5)
		
		//  mejor desplazar bits
		// long bitOffsetR = (long) Math.pow(2, nbits);
		
		/*
		 * RegsL    RegsR   FL   FR
		 * -------|-------|----|----|
		 */
		
		setRegistersBitOffset();
		setFieldsBitOffset();
		
	//	System.out.println("size: " + secondRegisterOffset * secondRegisterOffset + 
	//			firstFieldsOffset * secondFieldsOffset);
		// TODO Optimizar y extender
		int sizeExtDomain = registerSize * registerSize * fieldSize * fieldSize;
		System.out.println("sizeExtDomain: " + sizeExtDomain);
		BDDDomain registersAndFields = bdd.extDomain(sizeExtDomain);
		
		BDD reg5l = registersAndFields.ithVar(6);
		System.out.println("reg5l: " + reg5l);

		System.out.println(secondRegisterOffset);
		BDD reg5r = registersAndFields.ithVar(5 * secondRegisterOffset);
		System.out.println("reg5r: " + reg5r);

		BDD fieldL = registersAndFields.ithVar(2*firstFieldsOffset);
		System.out.println("fieldL: " + fieldL);
		
		BDD fieldR = registersAndFields.ithVar(3*secondFieldsOffset);
		System.out.println("fieldR: " + fieldR);

		BDD regSuma = registersAndFields.ithVar(6 + 5 * secondRegisterOffset+2*firstFieldsOffset+3*secondFieldsOffset);
		System.out.println("regX: " + regSuma);
		System.out.println("??????");
		System.out.println("??????");

		// ---
		BDDFactory bdd2 = BDDFactory.init("java", 1000, 100);
		bdd2.setMaxIncrease(250000);
		bdd2.setVarNum(6);

		BDD reg0Manual = bdd.one();
		reg0Manual.andWith(bdd2.nithVar(0)).andWith(bdd2.nithVar(1)).andWith(bdd2.nithVar(2));
		System.out.println("=======");
		System.out.println(reg0Manual);

		BDD reg2Manual = bdd.one();
		reg2Manual.andWith(bdd2.nithVar(0)).andWith(bdd2.ithVar(1)).andWith(bdd2.nithVar(2));
		System.out.println(reg2Manual);
		BDD andR0R2 = reg0Manual.andWith(reg2Manual.id());
		System.out.println("and: " + andR0R2);
		BDD orR0R2 = reg0Manual.or(reg2Manual);
		System.out.println("or: " + orR0R2);

		System.out.println(andR0R2.support());
		BDD regSet = registersAndFields.set();
		BDD iterBdd = bdd.one();

	}
}
