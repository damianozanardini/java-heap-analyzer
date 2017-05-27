package chord.analyses.damianoAnalysis;

import chord.analyses.damianoAnalysis.mgb.Entry;
import joeq.Compiler.Quad.Quad;

public class ProgramPoint {

	private Entry entry;
	
	// a program point inside a basic block is identified by Quads before and after it.
	// Program points at the beginning or end of a block can have null instead of a Quad
	private Quad quadBefore; //could be more than one?
	private Quad quadAfter; //could be more than one?
	
	
	
}
