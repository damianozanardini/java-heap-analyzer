package examples;
import java.util.Arrays;
import java.util.List;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDD.AllSatIterator;
import net.sf.javabdd.BDD.BDDIterator;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDD.AllSatIterator;
import net.sf.javabdd.BDDFactory;

public class Damiano {
		
	static BDDFactory factory;
	
	public static void main(String[] args) {
		factory = MyBDD.init();
		test5();
	}
	
	public static void test1() {
		Boolean[][] table = {
				// r1          r2            fs1                  fs2
				{ true, true,   true, true,   true, true, true,    true, true, true },
				{ true,false,   true,false,   true, true, true,    true, true, true },
				{false, true,   true, true,  false, true, true,   false,false, true },
				{false,false,   true,false,  false, true, true,    true, true, true }
		};
		BDD b = MyBDD.buildFromTable(table);
		System.out.println(b);
		MyBDD.showSolutions(b);
		MyBDD.showSets(b);
		//MyBDD.show(b);
	
		int[] vars = { 1, 9 };
		//BDD x = MyBDD.filterOnVars(b.id(),vars);
		//System.out.println("AFTER FILTERING ON 1 AND 9");
		//MyBDD.showSolutions(x);
		//MyBDD.showSets(x);
		//BDD y = x.id();
		//MyBDD.showSets(x.andWith(x.getFactory().ithVar(7)));
		//BDD z = y.id();
		//MyBDD.showSets(y.andWith(y.getFactory().ithVar(0)).andWith(y.getFactory().nithVar(1)));
		
		System.out.println("+++++");
		
		BDD z = MyBDD.addVarToModels(b,1);
		// BDD z = b.restrict(b.getFactory().ithVar(7));
		// BDD z = MyBDD.get(b,true,false,true,false);
		
		MyBDD.showSolutions(z);
		MyBDD.showSets(z);
		//MyBDD.show(z);
	
	}

	public static void test2() {
		Boolean[][] t1 = {
				{ true, true,   true, true,   true, false,false,false,false,false },
				{ true,false,   true,false,   true, false,false,false,false,false },
				{false, true,   true, true,  false, false,false,false,true,false },
				{false,false,   true,false,  false, false,false,false,false,false }
		};
		BDD b1 = MyBDD.buildFromTable(t1);
		Boolean[][] t2 = {
				{false, true,   true, true,   true, true, true,    true, true, true },
				{false,false,   true,false,   true, true, true,    true, true, true },
				{false, true,   true, true,  false, true, true,   false,false, true },
				{false,false,   true,false,  false, true, true,    true, true, true }
		};
		BDD b2 = MyBDD.buildFromTable(t2);
		
		MyBDD.showSets(b1);
		MyBDD.showSets(b2);
		
		BDD b = MyBDD.concatModels3(b1,b2);
		
		MyBDD.showSets(b);
		MyBDD.showSolutions(b);
		
		
	}

	public static void test3() {
		BDD b1 = MyBDD.emptyFieldSetToBDD();
		MyBDD.showSets(b1);
		MyBDD.showSolutions(b1);

		BDD b2 = MyBDD.fieldToBDD(null);
		MyBDD.showSets(b2);
		MyBDD.showSolutions(b2);

		BDD b3 = MyBDD.fieldIdToBDD(7);
		MyBDD.showSets(b3);
		MyBDD.showSolutions(b3);

		int [] ids = {2,5,7};
		BDD b4 = MyBDD.fieldIdsToBDD(ids);
		MyBDD.showSets(b4);
		MyBDD.showSolutions(b4);
	}
	
	public static void test4() {
		int [] ids1 = {2,5,7};
		BDD b1 = MyBDD.fieldIdsToBDD(ids1);
		int [] ids2 = {2,3,4,7};
		BDD b2 = MyBDD.fieldIdsToBDD(ids2);
		b1.orWith(b2);
		b1.orWith(b1.getFactory().nithVar(5));
		for (BDD b : MyBDD.separateSolutions(b1.id(),new int[b1.getFactory().varNum()])) {
			System.out.println(b.toString());
		}
		System.out.println(b1.satCount());
	}

	public static void test5() {
		BDD b1 = factory.nithVar(2).and(factory.ithVar(0).or(factory.ithVar(1)));
		BDD b2 = factory.ithVar(3).and(factory.nithVar(1));
		
		System.out.println(b1.toString());
		MyBDD.showSets(b1);
		System.out.println(b2.toString());
		MyBDD.showSets(b2);
		
		BDD b = MyBDD.concatModels3(b1,b2);
		
		System.out.println(b.toString());
		MyBDD.showSets(b);
	}
	
	
	
}
