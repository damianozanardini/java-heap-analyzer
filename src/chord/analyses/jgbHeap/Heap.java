package chord.analyses.jgbHeap;

import java.util.Set;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.PrintCFG;
import chord.analyses.alias.CSCG;
import chord.analyses.alias.CSCGAnalysis;
import chord.analyses.alias.Ctxt;
import chord.analyses.alias.ICSCG;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.method.DomM;
import chord.bddbddb.Rel;
import chord.bddbddb.Rel.PairIterable;
import chord.bddbddb.Rel.RelView;
import chord.program.Program;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.JavaAnalysis;
import chord.project.analyses.ProgramRel;
import chord.util.tuple.object.Pair;

@Chord(name = "heap",
       consumes = { "P", "I", "M", "V", "F", "AbsField", "FieldSet", "VT", "Register", "UseDef", "C", "CH", "CI", "reachableCM" },
       produces = { }
)
public class Heap extends JavaAnalysis {

    @Override public void run() {
    	Utilities.setVerbose(true);
    	
    	//CyclicityFixpoint fp = new CyclicityFixpoint();
    	//fp.init();
    	//fp.run();
    	
    	// PRUEBAS 19/02/2016
    	CSCGAnalysis cg = new CSCGAnalysis();
    	cg.run();
    	ICSCG callgraph = cg.getCallGraph();
    	System.out.println("UNTIL HERE");
    	Set<Pair<Ctxt, jq_Method>> nodes = callgraph.getNodes();
    	for (Pair<Ctxt, jq_Method> node : nodes) {
    		System.out.println("   NODE: " + node.val0 + " --> " + node.val1);
    	}
    	
    	ControlFlowGraph cfg = CodeCache.getCode(Program.g().getMainMethod());
		new PrintCFG().visitCFG(cfg);
    	
		ProgramRel relCI = (ProgramRel) ClassicProject.g().getTrgt("CI");
		relCI.load();
		RelView relCIview = relCI.getView();
		PairIterable<Object,Object> pairs = relCIview.getAry2ValTuples();
		for (Pair<Object,Object> p: pairs) {
			System.out.println("   CI: " + p.val0 + " --> " + p.val1);
		}
		
    	//fp.printOutput();
    }

}
