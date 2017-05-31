package chord.analyses.damianoAnalysis;

import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Quad;

public class ProgramPoint {

	private Entry entry;
	private BasicBlock basicBlock;
	
	// a program point inside a basic block is identified by Quads before and after it.
	// Program points at the beginning or end of a block can have null instead of a Quad
	private Quad quadBefore; //could be more than one?
	private Quad quadAfter; //could be more than one?
	
	public Entry getEntry() { return entry; }
	public BasicBlock getBasicBlock() { return basicBlock; }
	public Quad getQuadBefore() { return quadBefore; }
	public Quad getQuadAfter() { return quadAfter; }
	
	public ProgramPoint(Entry e, BasicBlock bb, Quad qb, Quad qa) {
		entry = e;
		basicBlock = bb;
		quadBefore = qb;
		quadAfter = qa;
	}
	
	public String toString() {
		return "<" + entry + ">.<" + basicBlock + ">.<" + quadBefore + "\\/" + quadAfter + ">";
	}
}
