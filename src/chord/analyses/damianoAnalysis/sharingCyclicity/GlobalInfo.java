package chord.analyses.damianoAnalysis.sharingCyclicity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.sun.corba.se.spi.orbutil.fsm.Input;

import chord.analyses.damianoAnalysis.DomEntry;
import chord.analyses.damianoAnalysis.DomProgramPoint;
import chord.analyses.damianoAnalysis.DomRegister;
import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.ProgramPoint;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.lock.DomR;
import chord.project.ClassicProject;
import chord.util.tuple.object.Pair;

import joeq.Class.jq_Field;
import joeq.Class.jq_Method;
import joeq.Class.jq_Primitive;
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
 * <li> {@literal abstractStates}: the mapping between program points and
 * abstract values;
 * <li> {@literal summaryManager}: where the input and output summaries for each
 * entry are stored;
 * <li> {@literal entryManager}: where information about entries is stored;
 * <li> {@literal sharingQuestions} and {@literal cyclicityQuestions}: specify which
 * information will be output at the end of the analysis;
 * <li> {@literal ghostCopies}: the mapping between a register and its
 * corresponding ghost register;
 * <li> {@literal entryQueue}: the data structure to implement the global fixpoint.
 * 
 * @author damiano
 */
public class GlobalInfo {
	
	/**
	 * Whether the implementation with tuples will be executed.
	 */
	private static boolean tuplesImplementation = true;
	
	/**
	 * Whether the implementation with Binary Decision Diagrams will be executed.
	 */
	private static boolean bddImplementation = false;

	/**
	 * Set which implementation will be executed by reading the {@code Input} file.
	 * 
	 * @param s
	 */
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
	
	/**
	 * Returns true iff the implementation with tuples will be executed.
	 * @return
	 */
	public static boolean tuplesImplementation() {
		return tuplesImplementation;
	}

	/**
	 * Returns true iff the implementation with BDDs will be executed.
	 * @return
	 */
	public static boolean bddImplementation() {
		return bddImplementation;
	}

	/**
	 * Returns true iff both implementations will be executed.
	 * @return
	 */
	public static boolean bothImplementations() {
		return tuplesImplementation() && bddImplementation();
	}
	
	/**
	 * The mapping between program points and abstract values.  Initially, the
	 * {@code HashMap} is empty, and abstract values are created when needed.
	 */
	private static HashMap<ProgramPoint,AbstractValue> abstractStates;
	
	/**
	 * The summary manager storing the input and output summaries (which are
	 * actually abstract values) for each entry.
	 */
	private static SummaryManager summaryManager;
	
	/**
	 * Returns the summary manager.
	 * 
	 * @return
	 */
	public static SummaryManager getSummaryManager() {
		return summaryManager;
	}

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
	
	/**
	 * Where the correspondence between "real" reference registers of a method
	 * and their corresponding "ghost" version is stored
	 */
	static private HashMap<jq_Method,HashMap<Register,Register>> ghostCopies;

	/**
	 * The correspondence between a method and the register where the return value is stored.
	 * This is necessary since, if the method has more than one return statement, then each return
	 * statement may correspond to a different register
	 */
	static private HashMap<jq_Method,Register> returnRegisters;

	public static LinkedList<Entry> entryQueue;

	static void init(jq_Method m) {
		abstractStates = new HashMap<ProgramPoint,AbstractValue>();
		summaryManager = new SummaryManager();
		entryManager = new EntryManager(m);
		ghostCopies = new HashMap<jq_Method,HashMap<Register,Register>>();
		returnRegisters = new HashMap<jq_Method,Register>();
		sharingQuestions = new ArrayList<Pair<Register,Register>>();
		cyclicityQuestions = new ArrayList<Register>();
		createGhostRegisters();
		createReturnRegisters();
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
		if (av_old != null) return av_old.updateInfo(av_new);
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
				Utilities.info("AV: " + getAV(getInitialPP(entry,bb)));
				Utilities.info("    (PP: " + getInitialPP(entry,bb) + ")");
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
	
	static ProgramPoint getInitialPP(Entry e,BasicBlock bb) {
		DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
		for (ProgramPoint pp : domPP)
			if (pp.getEntry() == e && pp.getBasicBlock() == bb && pp.getQuadBefore() == null) return pp;
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

	static ProgramPoint getFinalPP(Entry e,BasicBlock bb) {
		DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
		for (ProgramPoint pp : domPP)
			if (pp.getEntry() == e && pp.getBasicBlock() == bb && pp.getQuadAfter() == null) return pp;
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
	
	public static jq_Field getNthField(int id) {
		DomAbsField domF = (DomAbsField) ClassicProject.g().getTrgt("AbsField");
		return domF.get(id);
	}
	
	/**
	 * Creates the ghost registers for all methods.
	 */
	static void createGhostRegisters(){
		Utilities.begin("CREATE GHOST REGISTERS");
		DomEntry domEntry = (DomEntry) ClassicProject.g().getTrgt("Entry");
		DomRegister domR = (DomRegister) ClassicProject.g().getTrgt("Register");
		for (int i=0; i<domEntry.size(); i++) {
			Entry entry = domEntry.get(i);
			jq_Method method = entry.getMethod();
			if (!ghostCopies.containsKey(method)) {
				Utilities.begin("METHOD: " + method);
				ghostCopies.put(method,new HashMap<Register,Register>());
				int offset = regOffset();
				RegisterFactory rf = method.getCFG().getRegisterFactory();
				int k=0;
				// WARNING: it should also consider temporary registers, but ignore registers (temporary or not) which are not parameters
				for (Register r : entry.getReferenceFormalParameters()) {
					Register rprime = rf.getOrCreateLocal(k+offset,r.getType());
					domR.add(rprime);
					ghostCopies.get(method).put(r,rprime);
					Utilities.info("GHOST REGISTER " + rprime + " CREATED FOR " + r);
					k++;
				}
				Utilities.info("GHOST COPIES: " + ghostCopies.get(method));
				Utilities.end("METHOD: " + method);
			}
		}
		domR.save();
		Utilities.end("CREATE GHOST REGISTERS");
	}

	static Register getGhostCopy(jq_Method m,Register r) {
		HashMap<Register,Register> map = ghostCopies.get(m);
		return map.get(r);
	}

	static HashMap<Register,Register> getGhostCopy(jq_Method m) {
		return ghostCopies.get(m);
	}

	static boolean isGhost(jq_Method m,Register r) {
		HashMap<Register,Register> map = ghostCopies.get(m);
		return map.containsValue(r);
	}
	
	/**
	 * The original purpose of this method was that each register Rx would have a ghost counterpart
	 * named Ry where y = x+10^j where j was the minimum number such that no confusion with names would arise.
	 * E.g., if the total number of registers was 67, then R3 would be mapped to R103 (i.e., the smallest 
	 * 10^i such that 10^i>67), and so on.
	 * 
	 * However, it is not clear how getOrCreateLocal works, so that the new register has no such number.
	 * In any case, we keep this code since the newly created register is still a new one (which is the
	 * important thing, and probably happens because regOffset() is "big enough" to avoid taking an existing
	 * register) 
	 * 
	 * @return
	 */
	static private int regOffset() {
		DomRegister domR = (DomRegister) ClassicProject.g().getTrgt("Register");
		int s = domR.size();
		int j = 1;
		while (j<s) j *= 10;
		return j;
	}

	/**
	 * Assigns a new register to every method which has a return value.
	 * This is used when collecting the final value since different Return statements
	 * in the method can store data into different registers.
	 */
	static void createReturnRegisters(){
		Utilities.begin("CREATE RETURN REGISTERS");
		DomEntry domEntry = (DomEntry) ClassicProject.g().getTrgt("Entry");
		DomRegister domR = (DomRegister) ClassicProject.g().getTrgt("Register");
		for (int i=0; i<domEntry.size(); i++) {
			Entry entry = domEntry.get(i);
			jq_Method method = entry.getMethod();
			if (!returnRegisters.containsKey(method)) {
				if (method.getReturnType() != jq_Primitive.VOID) {
					Utilities.begin("METHOD: " + method);
					RegisterFactory rf = method.getCFG().getRegisterFactory();
					// the first argument must be a number big enough to guarantee that a new register
					// will be created
					Register rho = rf.getOrCreateLocal(10*regOffset(),method.getReturnType());
					domR.add(rho);
					returnRegisters.put(method,rho);
					Utilities.info("RETURN REGISTER " + rho + " CREATED FOR " + method);
					Utilities.end("METHOD: " + method);
				}
			}
		}
		domR.save();
		Utilities.end("CREATE RETURN REGISTERS");
	}

	/**
	 * Returns the return register associated to the given method, or null if the return type is void.
	 * 
	 * @param m the method
	 * @return the return register or null
	 */
	static Register getReturnRegister(jq_Method m) {
		return returnRegisters.get(m);
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
			Utilities.info("CALLER WOKEN UP: " + caller);
			entryQueue.add(caller);
		}			
	}


	
}
