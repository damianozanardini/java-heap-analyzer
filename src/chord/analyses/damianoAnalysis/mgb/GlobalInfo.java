package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import chord.analyses.damianoAnalysis.DomEntry;
import chord.analyses.damianoAnalysis.DomProgramPoint;
import chord.analyses.damianoAnalysis.DomRegister;
import chord.analyses.damianoAnalysis.Entry;
import chord.analyses.damianoAnalysis.ProgramPoint;
import chord.analyses.damianoAnalysis.Utilities;
import chord.project.ClassicProject;
import chord.util.tuple.object.Pair;

import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.BytecodeAnalysis.BasicBlock;
import joeq.Compiler.Quad.EntryOrExitBasicBlock;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.Operand.ParamListOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.RegisterFactory.Register;

// Should implement triples (Entry,Quad,AbstractValue), representing abstract information at each program point

// WARNING: this should replace relShare,relCycle,AccumulatedTuples and the like

// WARNING WARNING: this change is quite big...

public class GlobalInfo {
	static HashMap<ProgramPoint,AbstractValue> abstractStates;
	
	static SummaryManager summaryManager;
	
	static EntryManager entryManager;
	
	static private HashMap<Register,Register> ghostCopies;
	
	static void init(jq_Method m) {
		abstractStates = new HashMap<ProgramPoint,AbstractValue>();
		summaryManager = new SummaryManager();
		entryManager = new EntryManager(m);
		ghostCopies = new HashMap<Register,Register>();
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
	
	static AbstractValue getAV(ProgramPoint pp) {
		if (abstractStates.containsKey(pp)) {
			return abstractStates.get(pp);
		} else return new AbstractValue();
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
	
	static ProgramPoint getInitialPP(Entry e) {
		EntryOrExitBasicBlock entry = e.getMethod().getCFG().entry();
		if (entry.getQuads().size() > 0) {
			return getPPBefore(e,entry.getQuad(0));
		} else {
			DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
			for (int i=0; i<domPP.size(); i++) {
				ProgramPoint pp = domPP.get(i);
				if (pp.getEntry() == e && pp.getBasicBlock() == entry) return pp;
			}
			
		}
		return null;
	}
	
	static ProgramPoint getInitialPP(joeq.Compiler.Quad.BasicBlock bb) {
		DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
		for (int i=0; i<domPP.size(); i++) {
			ProgramPoint pp = domPP.get(i);
			if (pp.getBasicBlock() == bb && pp.getQuadBefore() == null) return pp;
		}
		return null;
	}
	
	/**
	 * This is not easy because we have to be sure that the abstract information
	 * must be attached also to basic blocks with no code
	 * @param e
	 * @return
	 */
	static ProgramPoint getFinalPP(Entry e) {
		EntryOrExitBasicBlock exit = e.getMethod().getCFG().exit();
		if (exit.getQuads().size() > 0) {
			return getPPAfter(e,exit.getLastQuad());
		} else {
			DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
			for (int i=0; i<domPP.size(); i++) {
				ProgramPoint pp = domPP.get(i);
				if (pp.getEntry() == e && pp.getBasicBlock() == exit) return pp;
			}
			
		}
		return null;
	}

	static ProgramPoint getFinalPP(joeq.Compiler.Quad.BasicBlock bb) {
		DomProgramPoint domPP = (DomProgramPoint) ClassicProject.g().getTrgt("ProgramPoint");
		for (int i=0; i<domPP.size(); i++) {
			ProgramPoint pp = domPP.get(i);
			if (pp.getBasicBlock() == bb && pp.getQuadAfter() == null) return pp;
		}
		return null;
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
			jq_Method method = entry.getMethod();
			List<Register> paramRegisters = new ArrayList<>();
			RegisterFactory rf = method.getCFG().getRegisterFactory();
			for (int j = 0; j < method.getParamWords(); j++){
				jq_Type type = entry.getCallSite().getUsedRegisters().get(j).getType();
				if (!rf.getOrCreateLocal(j,type).isTemp())
					paramRegisters.add(rf.getOrCreateLocal(j,method.getParamTypes()[j]));
			}
			for (int k=0; k<rf.size(); k++) {
				Register r = rf.get(k);
				if (!r.getType().isPrimitiveType()) {
					RegisterOperand rop = rf.makeRegOp(r.getType());
					Register rprime = rop.getRegister();
					domR.add(rprime);
					ghostCopies.put(r,rprime);
				}
			}
		}
		domR.save();
		Utilities.end("CREATE GHOST REGISTERS");
	}

	static Register getGhostCopy(Register r) {
		return ghostCopies.get(r);
	}

}
