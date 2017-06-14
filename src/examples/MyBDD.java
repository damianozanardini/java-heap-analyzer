package examples;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDD.BDDIterator;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDD.AllSatIterator;

public class MyBDD {

	static BDDFactory bf;
	
	static void init() {
		int numberOfVariables = 10;
		bf = BDDFactory.init("java",200, 100);
		bf.setVarNum(numberOfVariables);
	}
	
	static BDD buildFromTable(Boolean[][] table) {
		BDD global = bf.zero();
		for (int i=0; i<table.length; i++) {
			BDD row = bf.one();
			for (int j=0; j<table[i].length; j++) {
				if (table[i][j]) row.andWith(bf.ithVar(j));
				else row.andWith(bf.nithVar(j));
			}
			global.orWith(row);
		}
		return global;
	}

	// existential quantification on an array of variables
	static BDD filterOnVars(BDD original, int[] vars) {
		return original.exist(myVarSet(vars));
	}
	
	// a version of this can be used to retrieve sharing given registers: the combination of
	// booleans would come from the bit-wise representation of a Register
	static BDD get(BDD original, boolean b1, boolean b2) {
		BDD x = bf.one();
		if (b1) x.andWith(bf.ithVar(0)); else x.andWith(bf.nithVar(0));
		if (b2) x.andWith(bf.ithVar(1)); else x.andWith(bf.nithVar(1));
		return original.restrict(x);
	}
	
	static void show(BDD bdd) {
		bdd.printDot();
		
		System.out.println("SAT ONE: " + bdd.satOne());
		AllSatIterator x = bdd.allsat();
		byte[] y = null;
		while (x.hasNext())
			y = x.nextSat();
			for (int i=0; i<y.length; i++) {
				System.out.print(" " + y[i]);
			}
			System.out.println();
		System.out.println("SAT COUNT: " + bdd.satCount());
	}
	
	/**
	 * This shows in each line a truth assignment which makes the formula true
	 * (basically, each line is a line of the table from which the BDD was built)
	 * 
	 * @param bdd
	 */
	static void showSolutions(BDD bdd) {
		System.out.println("SOLUTIONS: ");
		// all variable indexes (otherwise it does not mention all values)
		int [] vs = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		BDDIterator it = bdd.iterator(myVarSet(vs));	
		while (it.hasNext()) {
			BDD b = (BDD) it.next();
			System.out.println(b);
		}
	}
	
	static void showSets(BDD bdd) {
		System.out.println("SETS: ");
		// all variable indexes (otherwise it does not mention all values)
		int [] vs = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
		BDDIterator it = bdd.iterator(myVarSet(vs));	
		while (it.hasNext()) {
			BDD b = (BDD) it.next();
			System.out.print("{ ");
			for (int i=0; i<vs.length; i++) {
				boolean isHere = b.exist(myVarSetMinusOne(vs,i)).restrict(bf.ithVar(i)).isOne();
				System.out.print(isHere? i + ", " : "");
			}
			System.out.println(" }");
		}
	}
	
	static BDD myVarSet(int[] vars) {
		BDD x = bf.one();
		for (int i=0; i<vars.length; i++)
			x.andWith(bf.ithVar(vars[i]));
		return x;
	}

	static BDD myVarSetMinusOne(int[] vars, int v) {
		BDD x = bf.one();
		for (int i=0; i<vars.length; i++)
			if (i!=v) x.andWith(bf.ithVar(vars[i]));
		return x;
	}

	
}
