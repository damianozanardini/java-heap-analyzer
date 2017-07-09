package chord.analyses.damianoAnalysis.oldCyclicity;

import java.util.List;

import joeq.Class.jq_Field;
import chord.analyses.field.DomF;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;

/**
 * This domains contains all reference fields which are tracked explicitly.
 * If some reference fields are not tracked explicitly, then the 0-th element
 * is reserved to "*" (which is store as null).
 * @author Damiano Zanardini
 *
 */
@Chord(
	    name = "AbsF",
	    consumes = { "F" }
	)
public class DomAbsF extends ProgramDom<jq_Field> {
	
	/**
	 * This tells whether all fields are tracked explicitly.
	 */
	public List<jq_Field> trackedFields;
	
	/**
	 * If all fields are tracked explicitly, then FSet will contain all
	 * reference fields, excluding the "arrays" field (0-th element of the F
	 * domain).
	 * Otherwise, the 0-th index will be reserved to null, representing the "*"
	 * field, and only the reference fields which are tracked explicitly are
	 * added. 
	 */
	public void fill() {
		DomF domF = (DomF) ClassicProject.g().getTrgt("F");
		if (trackedFields == null) { // all fields are tracked explicitly
			int n = domF.size();
			for (int i=1; i<n; i++) { // i starts from 1 because we don't want to include the "arrays" field
				jq_Field f = domF.get(i);
				if (f.getType().isReferenceType()) add(f);
			}
		} else {
			add(null); 	// index 0 is taken by the "*" field
	    	for (int i=1; i<domF.size(); i++) {
	    		jq_Field f = domF.get(i);
	    		if (trackedFields.contains(f) && f.getType().isReferenceType()) add(f);
	    	}
		}
	}
	
}