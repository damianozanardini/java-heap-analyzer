package examples;
import java.util.Arrays;
import java.util.List;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;


public class ModifiedModelsExample {
	// number of variables
	static int N;
	static int numberOfConditions;
	static BDDFactory B;
	static BDD[] X; /* BDD with conditions array */
	static BDD[] Y; /* BDD with conditions array for Y*/

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

	public static void init(int numberOfVariables, int ... conditions) {
		N = numberOfVariables;
		numberOfConditions = conditions[0];
		// Tests shows that more than 100 nodes will be needed for sure
		// if we have less nodes than needed, the garbage collector will be invoked
		// but we should get the result anyway.
		// "java" is passed to force the use of the java implementation of buddy
		// instead of the C original library (that btw is not within the project's scope)
		B = BDDFactory.init("java",200, 100);
		B.setVarNum(N);
		X = new BDD[numberOfConditions];
		Y = new BDD[numberOfConditions];
	}

	
	public static BDD generateConditions(){
		//{ parent } - { parent }
		X [0] = andBDDSByNameIncludeNegated("lparent", "rparent");
		// { parent } - { left parent }
		X [1] = andBDDSByNameIncludeNegated("lparent","rleft","rparent");
		// { parent } - {parent right}
		X [2] = andBDDSByNameIncludeNegated("lparent","rparent","rright");
		//{ parent } - { left parent right }
		X [3] = andBDDSByNameIncludeNegated("lparent","rleft","rparent","rright");
		
		//{ parent } - { parent }
		Y [0] = andBDDSByNameIncludeNegated("lparent");
		// { parent } - { left parent }
		Y [1] = andBDDSByNameIncludeNegated("lparent","rleft");
		// { parent } - {parent right}
		Y [2] = andBDDSByNameIncludeNegated("lparent","rright");
		//{ parent } - { left parent right }
		Y [3] = andBDDSByNameIncludeNegated("lparent","rleft","rright");

		// Now we create the disyuntive form and stack it in the first element

		for (int i=1; i < numberOfConditions; i++){
			X[0].orWith(X[i]);
		}
		X[0].printDot();

		for (int i=1; i < numberOfConditions; i++){
			Y[0].orWith(Y[i]);
		}
		BDD all_vars = X[0].support();
		System.out.println(all_vars);
		return X[0];
	}
	public static void main(String[] args) {
//		  X
//		  { l_parent(0) } - { r_parent(3) }
//		  { l_parent(0) } - { r_left(4) r_parent(3) }
//		  { l_parent(0) } - { r_parent(3) r_right(5) }
//		  { l_parent(0) } - { r_left(4) r_parent(3) r_right(5) }
//		  Y
//		  { l_parent(0) } - { }
//		  { l_parent(0) } - { r_left(4) }
//		  { l_parent(0) } - { r_right(5) }
//		  { l_parent(0) } - { r_left(4) r_right(5) }
//		  X U Y
//		  { l_parent(0) } - { r_parent(3) }
//		  { l_parent(0) } - { r_left(4) r_parent(3) }
//		  { l_parent(0) } - { r_parent(3) r_right(5) }
//		  { l_parent(0) } - { r_left(4), r_parent(3) r_right(5) }
//		  { l_parent(0) } - { }
//		  { l_parent(0) } - { r_left(4) }
//		  { l_parent(0) } - { r_right(5) }
//		  { l_parent(0) } - { r_left(4) r_right(5) }
		  
		
		init(6, 4);
		
		// still doing this manually
		BDD conditions = generateConditions();

		System.out.println("= Condition set=");
		System.out.println(conditions);
		System.out.println("=Table=");
		B.printTable(conditions);
		System.out.println("======");
		conditions.printDot();

	}
}
