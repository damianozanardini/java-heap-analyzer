package chord.analyses.damianoAnalysis.mgb;

import java.util.HashMap;

import chord.analyses.damianoAnalysis.DomEntry;
import chord.analyses.damianoAnalysis.DomProgramPoint;
import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.ProgramPoint;
import chord.analyses.damianoAnalysis.Utilities;
import chord.project.ClassicProject;

import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Quad;

// Should implement triples (Entry,Quad,AbstractValue), representing abstract information at each program point

// WARNING: this should replace relShare,relCycle,AccumulatedTuples and the like

// WARNING WARNING: this change is quite big...

public class GlobalInfo {

	static HeapProgram program;
	
	static HashMap<ProgramPoint,AbstractValue> abstractStates;
	
	static SummaryManager summaryManager;
	
	static EntryManager entryManager;
	
	static void init(jq_Method m) {
		// WARNING: HeapProgram does nothing with the jq_Method parameter
		// (just stores it), but we keep it for the moment, for the sake of compilation
		program = new HeapProgram(m);
		abstractStates = new HashMap<ProgramPoint,AbstractValue>();
		summaryManager = new SummaryManager();
		entryManager = new EntryManager(m);
	}
		
	/**
	 * This method joins the new abstract information about a program point
	 * with the old one (creating a new mapping if it did not exists).
	 * @param pp the program point
	 * @param av_new the new abstract information
	 * @return whether the information has changed
	 */
	static boolean update(ProgramPoint pp,AbstractValue av_new) {
		AbstractValue av_old = abstractStates.get(pp);
		if (av_old != null) {
			return av_old.update(av_new);
		} else {
			abstractStates.put(pp,av_new);
			return true;
		}
	}
	
	// WARNING: this could probably be more efficient
	static ProgramPoint getPPBefore(Entry e,Quad q) {
		DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
		for (int i=0; i<domPP.size(); i++) {
			ProgramPoint pp = domPP.get(i);
			if (pp.getEntry() == e && pp.getQuadAfter() == q) return pp;
		}
		// should never happen
		Utilities.err("PROGRAM POINT BEFORE QUAD " + q + " NOT FOUND");
		return null;		
	}
	
	// WARNING: this could probably be more efficient
	static ProgramPoint getPPAfter(Entry e,Quad q) {
		DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
		for (int i=0; i<domPP.size(); i++) {
			ProgramPoint pp = domPP.get(i);
			if (pp.getEntry() == e && pp.getQuadBefore() == q) return pp;
		}
		// should never happen
		Utilities.err("PROGRAM POINT AFTER QUAD " + q + " NOT FOUND");
		return null;		
	}
}
