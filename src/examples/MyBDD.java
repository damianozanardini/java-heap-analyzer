package examples;

import java.util.ArrayList;

import joeq.Class.jq_Field;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.damianoAnalysis.sharingCyclicity.GlobalInfo;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDD.BDDIterator;
import net.sf.javabdd.BDDBitVector;
import net.sf.javabdd.BDDFactory;
import net.sf.javabdd.BDD.AllSatIterator;

public class MyBDD {

	static BDDFactory bf;
	
	static BDDFactory init() {
		int numberOfVariables = 10;
		bf = BDDFactory.init("java",200, 100);
		bf.setVarNum(numberOfVariables);
		return bf;
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
	static BDD get(BDD original, boolean b0, boolean b1, boolean b2, boolean b3) {
		BDD x = bf.one();
		if (b0) x.andWith(bf.ithVar(0)); else x.andWith(bf.nithVar(0));
		if (b1) x.andWith(bf.ithVar(1)); else x.andWith(bf.nithVar(1));
		if (b2) x.andWith(bf.ithVar(2)); else x.andWith(bf.nithVar(2));
		if (b3) x.andWith(bf.ithVar(3)); else x.andWith(bf.nithVar(3));
		return original.and(x).exist(x.support());
	}
	
	/**
	 * USEFUL!
	 * This method creates and returns a BDD b' such that Models(b') = { m\cup\{var\} | m\in models(b) }
	 * 
	 * @param b
	 * @param var will correspond to a FIELD
	 * @return
	 */
	static BDD addVarToModels(BDD b, int var) {
		return b.id().exist(bf.ithVar(var)).andWith(bf.ithVar(var));
	}
	
	static BDD concatModels(BDD b1, BDD b2) {
		// WARNING: is it guaranteed that support() returns all the "variables" we actually want?
		int [] vs = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

		BDD acc = bf.zero();

		BDDIterator it = b2.iterator(b2.support());
		while (it.hasNext()) {
			BDD b = (BDD) it.next();
			BDD c = bf.one();
			// this loop basically applies a restriction on all variables which appear with a ":1" in a solution
			// (e.g., transforms <0:0, 1:0, 2:1, 3:0, 4:0, 5:1, 6:1, 7:1, 8:1, 9:1> into <2:1, 5:1, 6:1, 7:1, 8:1, 9:1>
			// there is probably a better way to do this
			for (int i=0; i<10; i++) {
				if (b.and(bf.nithVar(i)).isZero()) { // true iff variable i is set to 1
					c.andWith(bf.ithVar(i));
				}
				//if (b.exist(myVarSetMinusOne(vs,i)).restrict(bf.ithVar(i)).isOne()) {
				//if (b.exist(complement(b2.support(),bf.ithVar(i))).restrict(bf.ithVar(i)).isOne()) {
				//	c.andWith(bf.ithVar(i));
				//}
			}
			System.out.println("c =      " + c);
			BDD f = b1.id().exist(c).andWith(c);
			System.out.println("NEW LINE: " + f);
			acc.orWith(f);	
		}
		return acc;
	}

	static BDD concatModels2(BDD b1, BDD b2) {
		BDDIterator it1 = b1.iterator(b1.support());
		while (it1.hasNext()) {
			BDD s1 = (BDD) it1.next();
			BDDIterator it2 = b2.iterator(b2.support());
			while (it2.hasNext()) {
				BDD s2 = (BDD) it2.next();
				Utilities.info(s1 + "      " + s2);
				Utilities.info(s1.restrict(s2) + "***");
				s1.compose(bf.one(),3);
				Utilities.info(s1 + "      " + s2);
				
			}
		}
		Utilities.info(" --- " + b1.id().low());
		showSets(b1.id().low());
		showSets(b1.id().high());
		return null;
	}
	
	static BDD concatModels3(BDD b1, BDD b2) {
		ArrayList<BDD> bdds1 = separateSolutions(b1,new int[bf.varNum()]);
		ArrayList<BDD> bdds2 = separateSolutions(b2,new int[bf.varNum()]);
	
		for (BDD x : bdds1) System.out.println("1 -> " + x);
		for (BDD x : bdds2) System.out.println("2 -> " + x);
		
		BDDIterator it = b1.iterator(b1.support());
		while (it.hasNext())
			System.out.println(((BDD)it.next()).toString());
		
		BDD concat = bf.zero();
		for (BDD c1 : bdds1) {
			for (BDD c2 : bdds2) {
				BDD line = bf.one();
				for (int i=0; i<bf.varNum(); i++) {
					if (c1.and(bf.nithVar(i)).isZero() || c2.and(bf.nithVar(i)).isZero()) { // at least one set to 1
						line.andWith(bf.ithVar(i));
					}
					if (c1.and(bf.ithVar(i)).isZero() && c2.and(bf.ithVar(i)).isZero()) { // both set to 0
						line.andWith(bf.nithVar(i));
					}
				}
				System.out.println("line = " + line);
				concat.orWith(line);
			}
		}
		return concat;
	}
	
    static ArrayList<BDD> separateSolutions(BDD bdd, int[] set) {
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
            ArrayList<BDD> listl = separateSolutions(bddl, set);
            bddl.free();

            set[bf.var2Level(bdd.var())] = 2;
            BDD bddh = bdd.high();
            ArrayList<BDD> listh = separateSolutions(bddh, set);
            bddh.free();
            
            listl.addAll(listh);

            set[bf.var2Level(bdd.var())] = 0;
            return listl;
        }
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
		BDD supportVarSet = bdd.support();
		// System.out.println(supportVarSet);
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

	/**
	 * USEFUL
	 * This basically returns the set complement of a set of variables, given a universe specified by allVars
	 * @param allVars
	 * @param set
	 * @return
	 */
	static BDD complement(BDD allVars,BDD set) {
		return (allVars.support().exist(set.support()).support());
	}
	
	static BDD myVarSetMinusOne(int[] vars, int v) {
		BDD x = bf.one();
		for (int i=0; i<vars.length; i++)
			if (i!=v) x.andWith(bf.ithVar(vars[i]));
		return x;
	}
	
	static BDD emptyFieldSetToBDD() {
		int nf = 10; // GlobalInfo.getNumberOfFields();
		BDD acc = bf.one();
		for (int i=0; i<nf; i++) {
			acc.andWith(bf.nithVar(i));
		}
		return acc;
	}
	
	static BDD fieldToBDD(jq_Field f) {
		BDD empty = emptyFieldSetToBDD();
		int id = 3; // GlobalInfo.getFieldId(f);
		BDD bid = bf.ithVar(id);
		return empty.exist(bid).andWith(bid);
	}

	static BDD fieldIdToBDD(int id) {
		BDD empty = emptyFieldSetToBDD();
		BDD bid = bf.ithVar(id);
		return empty.exist(bid).andWith(bid);
	}

	static BDD fieldIdsToBDD(int [] ids) {
		BDD acc = emptyFieldSetToBDD();
		for (int id :ids) {
			BDD bid = bf.ithVar(id);
			acc = acc.exist(bid).andWith(bid);
		}
		return acc;
	}
	
}
