package chord.analyses.jgbHeap;

import chord.analyses.alias.Ctxt;
import chord.analyses.damianoAnalysis.Utilities;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.RelView;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.Operator.Invoke;

@Chord(
	    name = "Entry",
	    consumes = { "CI" }
	)

public class DomEntry extends ProgramDom<Entry> {
	
	/**
	 * Fill the domain with all the entries of the program. 
	 */
	public void fill(){
		
		
		ProgramRel relCI = (ProgramRel) ClassicProject.g().getTrgt("CI");
		relCI.load();
		RelView relCIview = relCI.getView();
		PairIterable<Ctxt,Quad> pairs = relCIview.getAry2ValTuples();
		for (Pair<Ctxt,Quad> p: pairs) {
			Quad q = p.val1;
			Operator operator = q.getOperator();
			if (operator instanceof Invoke) {
				if(Invoke.getMethod(q).toString().matches("(.*)registerNatives(.*)")){
					continue;
				}
				if(Invoke.getMethod(q).toString().matches("(.*)<init>:()(.*)")){
					continue;
				}
				
				
				Entry e1 = new Entry(Invoke.getMethod(q).getMethod(),p.val0,q);
				Utilities.out("METODO: " + e1.getMethod() + ", QUAD: " + e1.getCallSite() + ", CONTEXT: " + e1.getContext() + ", ENTRY: " + e1);
				if(contains(e1)) continue;
				add(e1);
			}
		}
		
	}
	

}
