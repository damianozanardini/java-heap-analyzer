package chord.analyses.damianoAnalysis.mgb;

import java.util.HashMap;

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
	
	static void init() {
		// WARNING: HeapProgram does nothing with the jq_Method parameter
		// (just stores it), but we keep it for the moment, for the sake of compilation
		program = new HeapProgram(null);
		abstractStates = new HashMap<ProgramPoint,AbstractValue>();
		summaryManager = new SummaryManager();
		// to do this we do need an entryMethod, so that this field will be
		// initialized later by the createEntryManager method
		// entryManager = new EntryManager();
	}
	
	/**
	 * This is a step of the initialization which cannot be done before knowing which
	 * is the entry method (i.e., before reading the input file).
	 * If needed, the initialization of the program field could be put here.
	 * 
	 * @param m the entry method of the analysis
	 */
	static void createEntryManager(jq_Method m) {
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
	
	static ProgramPoint getPPBefore(Entry e,Quad q) {
		
	}
	
	static ProgramPoint getPPAfter(Entry e,Quad q) {
		
	}
}
