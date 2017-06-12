package examples;
import java.util.Arrays;
import java.util.List;
import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;

public class SharingBDDExample {
	// number of variables
	static int N;
	static int numberOfConditions;
	static BDDFactory B;
	static BDD[] X; /* BDD with conditions array */
	// This two will be eventually a Map. This matches the var number with the array position 
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

	public static void init(int numberOfVariables, int conditions) {
		N = numberOfVariables;
		numberOfConditions = conditions;
		// testing shows that more than 100 nodes will be used for sure
		// if we have less nodes than needed, the garbage collector will be invoked
		// but we should get the result anyway.
		// "java" is passed to force the use of the java implementation of buddy
		// instead of the C original library (that btw is not within the project)
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
		/*
		 * We will be modeling the following: 
		 * 
		 * SHARING BETWEEN r:Lfoo/Tree; AND
		 * r:Lfoo/Tree; = { parent } - { parent } { parent } - { left parent } {
		 * parent } - { parent right } { left parent } - { left parent } { left
		 * parent } - { parent right } { parent right } - { parent right } {
		 * parent } - { left parent right } { left parent } - { left parent
		 * right } { parent right } - { left parent right } { left parent right
		 * } - { left parent right }
		 * 
		 */
		                    
		/*                               +------+
		 *                               |      |right
		 *                             +-v--+   |
		 *                    +------->| a  +---+
		 *                    |        +--+-+
		 *                    |           ^
		 *                    |           |parent
         *             parent |           |
		 *                    |        +--+-+
		 *                    |        |  r |
		 *                    |  +-----+----+
		 *                    |  |left
		 *                    |  |
		 *                    ++-v+
		 *                    | b |
		 *                    +---+
         */
		
		init(6, 10);
		
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
