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
		
	public static void main(String[] args) {
		MyBDD.init();
		test2();
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
		
		BDD b = MyBDD.concatModels(b1,b2);
		
		MyBDD.showSets(b);
		MyBDD.showSolutions(b);
		
		
	}
	
	
}
