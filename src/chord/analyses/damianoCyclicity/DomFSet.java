package chord.analyses.damianoCyclicity;

import java.util.ArrayList;
import java.util.List;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;

@Chord(
    name = "FSet",
    consumes = { "AbsF" }
)
public class DomFSet extends ProgramDom<FSet> {

	/**
	 * If all fields are tracked explicitly, then this is the number of
	 * program fields including the reserved "arrays" field.
	 * Otherwise, it is the number of explicitly-tracked fields plus 1 (the "*"
	 * field).
	 */
	protected int numberOfFields;
	
	/**
	 * Fills the domain with every possible field set; 
	 */
    public void fill() {
    	ClassicProject.g();
    	DomAbsF domAbsF = (DomAbsF) ClassicProject.g().getTrgt("AbsF");
    	numberOfFields = domAbsF.size();
    	for (int i=0; i<((int) Math.pow(2, numberOfFields)); i++)
    		add(new FSet(i));
    }
    
    /**
     * Returns every possible field set (representing the "true" path-formula). 
     * @return a list with all possible field sets.
     */
    public List<FSet> getAll() {
    	List<FSet> l = new ArrayList<FSet>();
    	for (int i=0; i<((int) Math.pow(2, numberOfFields)); i++)
    		l.add(get(i));
    	return l;
    }
    
}

