package chord.analyses.damianoAnalysis.mgb;

import chord.analyses.alias.Ctxt;
import chord.analyses.damianoAnalysis.Utilities;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.RelView;
import chord.program.Program;
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
		Quad[] qs = {};
		
		// main is added first
		add(new Entry(Program.g().getMainMethod(),new Ctxt(qs),null));
		
		// then the other entries
		ProgramRel relCI = (ProgramRel) ClassicProject.g().getTrgt("CI");
		relCI.load();
		RelView relCIview = relCI.getView();
		PairIterable<Ctxt,Quad> pairs = relCIview.getAry2ValTuples();
		for (Pair<Ctxt,Quad> p: pairs) {
			Ctxt c = p.val0;
			Quad q = p.val1;

			// exceptions (not to be added): <init> method of the object class,
			// and another strange one declared in the same class
			if (!isRegisterNativeEntry(q) && !isObjectInitEntry(q)) {
				Entry e1 = new Entry(Invoke.getMethod(q).getMethod(),c,q);
				add(e1);
			}
		
		}
	}
	
	private boolean isRegisterNativeEntry(Quad q) {
		return Invoke.getMethod(q).toString().matches("(.*)registerNatives(.*)");
	}

	private boolean	isObjectInitEntry(Quad q) {
		boolean nameMatch = Invoke.getMethod(q).getMethod().getName().toString().equals("<init>");
		boolean classMatch = Invoke.getMethod(q).getMethod().getDeclaringClass().toString().equals("java.lang.Object");
		return nameMatch && classMatch;
	}

}
