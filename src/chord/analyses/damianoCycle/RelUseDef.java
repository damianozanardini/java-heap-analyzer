package chord.analyses.damianoCycle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Quad;
import chord.analyses.point.DomP;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

/**
 * Relation containing pairs of quads (q1,q2) specifying that
 * q1 defines registers which are used by q2.  This is currently 
 * INTRAPROCEDURAL (q1 and q2 are in the same method)
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
@Chord(
    name = "UseDef",
    sign = "P0,P1:P0xP1"
)
public class RelUseDef extends ProgramRel {
    
    public void fill() {
        DomP ppoints = (DomP) ClassicProject.g().getTrgt("P");
    	
    	int s = ppoints.size();
    	Quad q1 = null;
    	Quad q2 = null;
    	for (int i=0; i<s; i++) {
    		Object x1 = ppoints.get(i);
    		if (x1 instanceof Quad) {
    			q1 = (Quad) x1;
    			for (int j=0; j<s; j++) {
    				Object x2 = ppoints.get(j);
    				if ((x2 instanceof Quad) && ((Quad) x2).getMethod() == q1.getMethod()) {
    					q2 = (Quad) x2;
    					if (q1!=q2 && usesDefinedRegister(q1,q2)) { // should not be reflexive
    						add(q1,q2);
    					}
    				}
    			}
    		}
    	}
    }

    protected boolean usesDefinedRegister(Quad q1, Quad q2) {
    	List<RegisterOperand> def;
    	if (q1.getOperator() instanceof Putfield) {
    		RegisterOperand r = q1.getUsedRegisters().get(0);
    		def = new ArrayList<RegisterOperand>();
    		def.add(r);
    	} else def = q1.getDefinedRegisters();
    	List<RegisterOperand> use = q2.getUsedRegisters();
		for (RegisterOperand r1 : def)
			for (RegisterOperand r2 : use)
				if (r1.getRegister() == r2.getRegister()) return true;
		return false;
    }
    
    public List<Quad> getByFirstArg(Quad q) {
    	RelView view = getView();
    	PairIterable<Quad,Quad> tuples = view.getAry2ValTuples();
    	Iterator<Pair<Quad,Quad>> iterator = tuples.iterator();
    	List<Quad> list = new ArrayList<Quad>();
    	while (iterator.hasNext()) {
    		Pair<Quad,Quad> pair = iterator.next();
    		if (pair.val0 == q) list.add(pair.val1);
    	}    	
    	return list;
    }
    
}	
