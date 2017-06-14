package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import chord.analyses.damianoAnalysis.DomProgramPoint;
import chord.analyses.damianoAnalysis.DomRegister;
import chord.analyses.damianoAnalysis.ProgramPoint;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.lock.DomR;
import chord.project.ClassicProject;
import chord.util.tuple.object.Pair;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.CodeCache;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.EntryOrExitBasicBlock;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;

/**
 * This class contains information which is meant to be available globally,
 * and the static methods to manipulate it. 
 * The most important pieces of information are:
 * - abstractStates: the mapping between program points and abstract value
 * - summaryManager: where the input and output summaries for each entry are stored
 * - entryManager: where information about entries is stored
 * - sharingQuestions and cyclicityQuestions: specifies which information will be 
 *   output at the end of the analysis
 * - ghostCopies: the mapping between a register and its corresponding ghost register
 * - entryQueue: the data structure to implement the global fixpoint
 * 
 * @author damiano
 *
 */
public class GlobalInfo {
	private static boolean tuplesImplementation = true;
	private static boolean bddImplementation = false;

	public static void setImplementation(String s) {
		if (s.equals("bdd")) {
			tuplesImplementation = false;
			bddImplementation = true;
		} else if (s.equals("both")) {
			tuplesImplementation = true;
			bddImplementation = true;
		} else { // default choice: tuples
			tuplesImplementation = true;
			bddImplementation = false;
		}
	}
	public static boolean tuplesImplementation() { return tuplesImplementation; }
	public static boolean bddImplementation() { return bddImplementation; }
	public static boolean bothImplementations() {return tuplesImplementation() && bddImplementation(); }
	
	/**
	 * The mapping between program points and abstract values. Initially, the
	 * HashMap is empty, and abstract values are created when needed
	 */
	static HashMap<ProgramPoint,AbstractValue> abstractStates;
	
	/**
	 * The summary manager storing the input and output summaries (which are
	 * actually abstract values) for each entry
	 */
	static SummaryManager summaryManager;

	/**
	 * The entry manager storing information about entries. In particular, it
	 * makes it possible to get the entry or entries corresponding to entity
	 * such as a Quad or a jq_Method
	 */
	private static EntryManager entryManager;
		
	static private ArrayList<Pair<Register,Register>> sharingQuestions;
	static ArrayList<Pair<Register,Register>> getSharingQuestions() { return sharingQuestions; }
	static private ArrayList<Register> cyclicityQuestions;
	static ArrayList<Register> getCyclicityQuestions() { return cyclicityQuestions; }
	
	static private HashMap<Entry,HashMap<Register,Register>> ghostCopies;

	public static LinkedList<Entry> entryQueue;

	static void init(jq_Method m) {
		abstractStates = new HashMap<ProgramPoint,AbstractValue>();
		summaryManager = new SummaryManager();
		entryManager = new EntryManager(m);
		ghostCopies = new HashMap<Entry,HashMap<Register,Register>>();
		sharingQuestions = new ArrayList<Pair<Register,Register>>();
		cyclicityQuestions = new ArrayList<Register>();
		createGhostVariables();
		entryQueue = new LinkedList<Entry>();
	}
		
	/**
	 * Joins the new abstract information about a program point
	 * with the old one (creating a new mapping if it did not exists).
	 * 
	 * @param pp the program point
	 * @param av_new the new abstract information
	 * @return whether the information has changed
	 */
	static boolean update(ProgramPoint pp,AbstractValue av_new) {
		AbstractValue av_old = abstractStates.get(pp);
		if (av_old != null) return av_old.update(av_new);
		else if (av_new!=null) {
			abstractStates.put(pp,av_new);
			return true;
		} else return false;
	}
	
	/**
	 * Returns the AbstractValue object corresponding to the specified ProgramPoint.
	 * It never returns null: if such an AbstractValue does not exist, then it is
	 * created on the fly
	 * 
	 * @param pp
	 * @return
	 */
	static AbstractValue getAV(ProgramPoint pp) {
		if (abstractStates.containsKey(pp)) {
			return abstractStates.get(pp);
		} else {
			AbstractValue av = createNewAV(pp.getEntry());			
			abstractStates.put(pp,av);
			return av;
		}
	}
	
    /**
     * This method encapsulates the double implementation of abstract values when a new
     * one is created.
     * 
     * @return either a TupleAbstractValue or a BDDAbstractValue, depending on which one is active
     */
    static AbstractValue createNewAV(Entry entry) {
    	if (bothImplementations()) return new BothAbstractValue(entry);
    	if (tuplesImplementation()) return new TuplesAbstractValue();
    	if (bddImplementation()) return new BDDAbstractValue(entry);
		return null;
    }

	static void showAVs(Entry entry) {
		Utilities.begin("SHOWING INFO FOR " + entry);
		ControlFlowGraph cfg;
		cfg = CodeCache.getCode(entry.getMethod());
		List<BasicBlock> bbs = cfg.postOrderOnReverseGraph(cfg.exit());
		for (BasicBlock bb : bbs) {
			Utilities.info("BLOCK " + bb);
			List<Quad> quads = bb.getQuads();
			if (quads.size() == 0) {
				Utilities.info("AV: " + getAV(getInitialPP(bb)));
				Utilities.info("    (PP: " + getInitialPP(bb) + ")");
			} else {
			Quad first = bb.getQuad(0);
			Utilities.info("AV BEFORE FIRST QUAD: " + getAV(getPPBefore(entry,first)));
			for (Quad q : bb.getQuads())
				Utilities.info("AV AFTER QUAD " + q + " : " + getAV(getPPAfter(entry,q)));
			}
		}
		Utilities.end("SHOWING INFO FOR " + entry);
	}
	
	// WARNING: this could probably be more efficient
	static ProgramPoint getPPBefore(Entry e,Quad q) {
		DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
		for (ProgramPoint pp : domPP)
			if (pp.getEntry() == e && pp.getQuadAfter() == q) return pp;
		// should never happen
		Utilities.err("PROGRAM POINT BEFORE QUAD " + q + " NOT FOUND");
		return null;		
	}
	
	// WARNING: this could probably be more efficient
	static ProgramPoint getPPAfter(Entry e,Quad q) {
		DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
		for (ProgramPoint pp : domPP)
			if (pp.getEntry() == e && pp.getQuadBefore() == q) return pp;
		// should never happen
		Utilities.err("PROGRAM POINT AFTER QUAD " + q + " NOT FOUND");
		return null;		
	}
	
	/**
	 * Gets the unique ProgramPoint corresponding to the "entry" basic block
	 * 
	 * @param e
	 * @return
	 */
	static ProgramPoint getInitialPP(Entry e) {
		EntryOrExitBasicBlock entryBlock = e.getMethod().getCFG().entry();
		DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
		for (ProgramPoint pp : domPP)
			if (pp.getEntry() == e && pp.getBasicBlock() == entryBlock) return pp;
		return null;
	}
	
	static ProgramPoint getInitialPP(BasicBlock bb) {
		DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
		for (ProgramPoint pp : domPP)
			if (pp.getBasicBlock() == bb && pp.getQuadBefore() == null) return pp;
		return null;
	}
	
	/**
	 * Gets the unique ProgramPoint corresponding to the "exit" basic block
	 * 
	 * @param e
	 * @return
	 */
	static ProgramPoint getFinalPP(Entry e) {
		EntryOrExitBasicBlock exit = e.getMethod().getCFG().exit();
		DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
		for (ProgramPoint pp : domPP)
			if (pp.getEntry() == e && pp.getBasicBlock() == exit) return pp;
		return null;
	}

	static ProgramPoint getFinalPP(BasicBlock bb) {
		DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
		for (ProgramPoint pp : domPP)
			if (pp.getBasicBlock() == bb && pp.getQuadAfter() == null) return pp;
		return null;
	}
	
	public static int getNumberOfRegisters() {
		DomRegister domR = (DomRegister) ClassicProject.g().getTrgt("Register");
		return domR.size();
	}
	
	public static int getNumberOfFields() {
		DomAbsField domF = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
		return domF.size();
	}

	public static int getFieldId(jq_Field f) {
		DomAbsField domF = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
		return domF.indexOf(f);
	}

	/**
	 * Creates the ghost registers for all entries 
	 */
	static void createGhostVariables(){
		Utilities.begin("CREATE GHOST REGISTERS");
		DomEntry domEntry = (DomEntry) ClassicProject.g().getTrgt("Entry");
		DomRegister domR = (DomRegister) ClassicProject.g().getTrgt("Register");
		for (int i=0; i<domEntry.size(); i++) {
			Entry entry = domEntry.get(i);
			Utilities.begin("ENTRY: " + entry);
			ghostCopies.put(entry,new HashMap<Register,Register>());
			jq_Method method = entry.getMethod();
			RegisterFactory rf = method.getCFG().getRegisterFactory();
			int length = rf.size();
			int offset = ghostOffset();
			for (int k=0; k<length; k++) {
				Register r = rf.get(k);
				if (!(r.getType().isPrimitiveType() || r.isTemp())) {
					Utilities.info("CREATING GHOST COPY OF " + r);
					Register rprime = rf.getOrCreateLocal(k+offset,r.getType());
					domR.add(rprime);
					ghostCopies.get(entry).put(r,rprime);
					Utilities.info("GHOST REGISTER " + rprime + " CREATED FOR " + r);
				}
			}
			Utilities.info("GHOST COPIES: " + ghostCopies.get(entry));
			Utilities.end("ENTRY: " + entry);
		}
		domR.save();
		Utilities.end("CREATE GHOST REGISTERS");
	}

	static Register getGhostCopy(Entry e,Register r) {
		HashMap<Register,Register> map = ghostCopies.get(e);
		return map.get(r);
	}

	static HashMap<Register,Register> getGhostCopy(Entry e) {
		return ghostCopies.get(e);
	}

	static private int ghostOffset() {
		DomRegister domR = (DomRegister) ClassicProject.g().getTrgt("Register");
		int s = domR.size();
		int j = 1;
		while (j<s) j *= 10;
		return j;
	}

	static void addSharingQuestion(Register r1,Register r2) {
		sharingQuestions.add(new Pair<Register,Register>(r1,r2));
	}
	
	static void addCyclicityQuestion(Register r) {
		cyclicityQuestions.add(r);
	}

	public static EntryManager getEntryManager() {
		return entryManager;
	}

	public static void setEntryManager(EntryManager entryManager) {
		GlobalInfo.entryManager = entryManager;
	}

	public static void wakeUp(Entry caller) {
		if (!entryQueue.contains(caller)) {
			Utilities.info("CALLER " + caller + " WOKEN UP");
			entryQueue.add(caller);
		}			
	}


	
}
