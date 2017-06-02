package chord.analyses.damianoAnalysis;

import java.util.List;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;
import chord.analyses.field.DomF;
import chord.analyses.method.DomM;
import chord.project.Chord;
import chord.project.ClassicProject;
import chord.project.analyses.ProgramDom;

@Chord(
	    name = "ProgramPoint",
	    consumes = { "Entry" }
	)			
public class DomProgramPoint extends ProgramDom<ProgramPoint> {

	/**
	 * Fill the domain with all the entries of the program. 
	 */
	public void fill() {
		DomEntry domEntry = (DomEntry) ClassicProject.g().getTrgt("Entry");
		Entry entry;
		ControlFlowGraph cfg;
		for (int i=0; i<domEntry.size(); i++) {
			entry = domEntry.get(i);
			cfg = CodeCache.getCode(entry.getMethod());
			List<BasicBlock> bbs = cfg.postOrderOnReverseGraph(cfg.exit());
			Quad previous = null;
			for (BasicBlock bb : bbs) {
				for (Quad q : bb.getQuads()) {
					ProgramPoint pp = new ProgramPoint(entry,bb,previous,q);
					add(pp);
					previous = q;
				}
				// the last program point in the block
				add (new ProgramPoint(entry,bb,previous,null));
			}
		}
	}

}

