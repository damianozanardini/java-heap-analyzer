package examples;
import java.util.Arrays;
import java.util.List;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDD.AllSatIterator;
import net.sf.javabdd.BDD.BDDIterator;
import net.sf.javabdd.BDDDomain;
import net.sf.javabdd.BDD.AllSatIterator;
import net.sf.javabdd.BDDFactory;

public class SharingBDDExampleD {
	// number of variables
	static int N;
	static int numberOfConditions;
	static BDDFactory B;
	static BDD[] X; /* BDD with conditions array */
	// This two will eventually turn into a Map. This matches the var number with the array position 
	static String[] vars = new String[]{"lparent", "lleft", "lright", "rparent", "rleft", "rright"};
	static List<String> varList = Arrays.asList(vars);
	
	public static boolean contains(int[] myArray, int element){
		// Apparently this is the most efficient way to do it.
		for (int i: myArray)
			if (i == element)
				return true;
		return false;
	}
	
	/**
	 * 
	 * NOTE 
     * Only for testing purposes and clarity, I should never use variable names once I'm into the representation
     * of an AV.
     * 
	 * Given some variable names, returns a bdd describing an AND operation between them 
     * and the rest of the variables negated.
     * 
	 * @param entries
	 * @return
	 */
    public static BDD andBDDSByNameIncludeNegated(String ... entries){
    	BDD combinedBDD = B.one();
    	int [] entriesIDs = new int[entries.length];
    	
    	for (int i =0; i<entries.length;i++){
    		entriesIDs[i] = varList.indexOf(entries[i]);
    	}
    	
    	for (int i=0; i<N;i++){
    		// TODO not very efficient since we know the size and the list is ordered
    		// change to a binarysearch in the future.
    		if (contains(entriesIDs,i)){
    			//combinedBDD.applyAll(B.ithVar(i), BDDFactory.and, B.ithVar(i).support());
    			combinedBDD.andWith(B.ithVar(i));
    		}else{
    			combinedBDD.andWith(B.nithVar(i));
    		}
    	}
    	return combinedBDD;
    } 

    /**
     * 
	 * NOTE 
     * Only for testing purposes and clarity, I should never use variable names once I'm into the representation
     * of an AV.
     * 
     * Given some variable names, returns a bdd describing an AND operation between them 
     * @param x
     * @param y
     * @return
     */
    public static BDD andBDDSByName(String ... entries){
    	BDD combinedBDD = B.one(); 
    	for (int i=0; i<entries.length;i++){
    		// TODO No tengo claro cual de las dos es la correcta, ambas parecen funcionar
    		//combinedBDD.andWith(X[varList.indexOf(entries[i])].id());
        	combinedBDD.andWith(B.ithVar(varList.indexOf(entries[i])));

    	}
    	return combinedBDD;
    } 

	public static void init(int numberOfVariables, int conditions) {
		N = numberOfVariables;
		numberOfConditions = conditions;
		// Tests shows that more than 100 nodes will be needed for sure
		// if we have less nodes than needed, the garbage collector will be invoked
		// but we should get the result anyway.
		// "java" is passed to force the use of the java implementation of buddy
		// instead of the C original library.
		B = BDDFactory.init("java",200, 100);
		B.setVarNum(N);
		X = new BDD[numberOfConditions];
	}

	
	public static BDD generateConditions(){
		//{ parent } - { parent }

		X [0] = andBDDSByNameIncludeNegated("lparent", "rparent");
		// { parent } - { left parent }
		X [1] = andBDDSByNameIncludeNegated("lparent","rleft","rparent");
		// { parent } - {parent right}
		X [2] = andBDDSByNameIncludeNegated("lparent","rparent","rright");
		// { left parent } - { left parent }
		X [3] = andBDDSByNameIncludeNegated("lleft","lparent","rleft","rparent");
		//{ left parent } - { parent right }
		X [4] = andBDDSByNameIncludeNegated("lleft", "lparent","rparent","rright");
		//{ parent right } - { parent right }
		X [5] = andBDDSByNameIncludeNegated("lparent","lright","rparent","rright");
		//{ parent } - { left parent right }
		X [6] = andBDDSByNameIncludeNegated("lparent","rleft","rparent","rright");
		//{ left parent } - { left parent right }
		X [7] = andBDDSByNameIncludeNegated("lleft", "lparent","rleft","rparent","rright");
		//{ parent right } - { left parent right }
		X [8] = andBDDSByNameIncludeNegated("lparent","lright","rleft","rparent","rright");
		//{ left parent right } - { left parent right }
		X [9] = andBDDSByNameIncludeNegated("lleft","lparent","lright","rleft","rparent","rright");

		// Now we create the disyuntive form and stack it in the first element
		for (int i=1; i < numberOfConditions; i++){
			X[0].orWith(X[i]);
		}

		return X[0];
	}
	
	public static void main(String[] args) {
		MyBDD.init();
		Boolean[][] table = {
				{ true, true,   true, true, true, true, true, true, true, true },
				{ true,false,   true,false, true, true, true, true, true, true },
				{false, true,   true, true,false, true, true,false,false, true },
				{false,false,   true,false,false, true, true, true, true, true }
		};
		BDD b = MyBDD.buildFromTable(table);
	
		MyBDD.showSolutions(b);
		
		int[] vars = { 1, 9 };
		BDD x = MyBDD.filterOnVars(b,vars);
		System.out.println("AFTER FILTERING ON 1 AND 9");
		MyBDD.showSolutions(x);
		MyBDD.showSets(x);
		BDD y = x.id();
		MyBDD.showSets(x.andWith(x.getFactory().ithVar(7)));
		BDD z = y.id();
		MyBDD.showSets(y.andWith(y.getFactory().ithVar(0)).andWith(y.getFactory().nithVar(1)));
		MyBDD.showSets(MyBDD.get(z, true, false));
		MyBDD.show(MyBDD.get(z,false,true));
	
		
		
	
	}

	public static void damianoWhiteBoard() {
		init(6,10);
		
		BDDDomain dom = B.extDomain(64); // 2^(number of variables)
		System.out.println(dom.getIndex());
		
		BDD x1 = B.one();
		x1.andWith(B.ithVar(0));
		x1.andWith(B.nithVar(1));
		x1.andWith(B.nithVar(2));
		x1.andWith(B.ithVar(3));
		x1.andWith(B.nithVar(4));
		x1.andWith(B.nithVar(5));
		BDD x2 = B.one();
		x2.andWith(B.ithVar(0));
		x2.andWith(B.nithVar(1));
		x2.andWith(B.nithVar(2));
		x2.andWith(B.ithVar(3));
		x2.andWith(B.ithVar(4));
		x2.andWith(B.nithVar(5));
		BDD x3= B.one();
		x3.andWith(B.ithVar(0));
		x3.andWith(B.nithVar(1));
		x3.andWith(B.nithVar(2));
		x3.andWith(B.ithVar(3));
		x3.andWith(B.nithVar(4));
		x3.andWith(B.ithVar(5));
		BDD x4 = B.one();
		x4.andWith(B.ithVar(0));
		x4.andWith(B.nithVar(1));
		x4.andWith(B.nithVar(2));
		x4.andWith(B.ithVar(3));
		x4.andWith(B.ithVar(4));
		x4.andWith(B.ithVar(5));
		BDD y = x1.id(); // makes a copy
		y.orWith(x2);
		y.orWith(x3);
		y.orWith(x4);
				
		System.out.println(y.satCount());

		System.out.println(B.ithVar(3).andWith(B.ithVar(5)));
		BDD y2 = y.exist(B.ithVar(3).andWith(B.ithVar(5)));
		System.out.println(dom.set());
		System.out.println(" ++++++++ " + B.ithVar(3).andWith(B.ithVar(5)).var());
		BDD allVars = y2.support();
		System.out.println(allVars);
		BDD sone = y2.satOne();
		System.out.println(y2.satCount());

		System.out.println(sone);
		
		// y.restrict(B.ithVar(3));
		
		sone.free();
		sone = y.satOne();
		System.out.println(sone);
		AllSatIterator as = y.allsat();
		
		while (as.hasNext()) {
			System.out.println("SOLUTION:");
			byte[] bs = as.nextSat();
			for (int i=0; i<6; i++) {
				System.out.println("---- " + bs[i]);
			}
		}
		
	}

}
