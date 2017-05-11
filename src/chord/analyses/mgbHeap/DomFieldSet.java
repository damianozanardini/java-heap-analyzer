package chord.analyses.mgbHeap;

import java.util.ArrayList;
import java.util.List;

import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;

@Chord(
    name = "FieldSet",
    consumes = { "AbsField" }
)
public class DomFieldSet extends ProgramDom<FieldSet> {

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
    	DomAbsField domAbsField = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
    	numberOfFields = domAbsField.size();
    	for (int i=0; i<((int) Math.pow(2, numberOfFields)); i++)
    		add(new FieldSet(i));
    }
    
    /**
     * Returns every possible field set (representing the "true" path-formula). 
     * @return a list with all possible field sets.
     */
    public List<FieldSet> getAll() {
    	List<FieldSet> l = new ArrayList<FieldSet>();
    	for (int i=0; i<((int) Math.pow(2, numberOfFields)); i++)
    		l.add(get(i));
    	return l;
    }
    
}

