package chord.analyses.damianoAnalysis.oldCyclicity;

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

@Chord(name = "cyclicity",
       consumes = { "P", "I", "M", "V", "F", "AbsF", "FSet", "VT", "Register", "UseDef", "C", "CH", "CI", "reachableCM" },
       produces = { }
)
public class Cyclicity extends JavaAnalysis {

    @Override public void run() {
    	Utilities.setVerbose(true);
    	
    	CyclicityFixpoint fp = new CyclicityFixpoint();
    	fp.init();
    	fp.run();
    	
    	fp.printOutput();
    }

}
