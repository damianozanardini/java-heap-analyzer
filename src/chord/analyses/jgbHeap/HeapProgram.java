package chord.analyses.jgbHeap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import chord.analyses.damianoAnalysis.DomRegister;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.method.DomM;
import chord.bddbddb.Dom;
import chord.bddbddb.Rel.PentIterable;
import chord.bddbddb.Rel.RelView;
import chord.bddbddb.Rel.TrioIterable;
import chord.bddbddb.RelSign;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Pent;
import chord.util.tuple.object.Quad;
import chord.util.tuple.object.Trio;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;

public class HeapProgram {
	
	private jq_Method mainMethod;
	
	public jq_Method getMainMethod(){ return this.mainMethod; }
	
	private Entry mainEntry;
	
	public Entry getMainEntry(){ return this.mainEntry; } 
	
	private ArrayList<Entry> listMethods;
	
	public ArrayList<Entry> getListMethods(){ return this.listMethods; }
	
	private EntryManager em;
	
	public EntryManager getEntryManager(){ return this.em; }
	
	private SummaryManager sm;
	
	public SummaryManager getSummaryManager(){ return this.sm; }
		
	private RelCycle relCycle;
	private RelShare relShare;
	private HashMap<Entry,ArrayList<Pair<Register,Register>>> ghostVariables;
	
	public HeapProgram(jq_Method mainMethod){
		
		this.mainMethod = mainMethod;
		
		// INITIALIZE STRUCTURE OF HEAP STATE
		relCycle = (RelCycle) ClassicProject.g().getTrgt("HeapCycle");
		relShare = (RelShare) ClassicProject.g().getTrgt("HeapShare");
		ghostVariables = new HashMap<>();
		
		// ENTRY AND SUMMARY MANAGER
		em = new EntryManager(mainMethod);
		sm = new SummaryManager(mainMethod,em);
		
		// CREATE STRUCTURE OF MAIN ENTRY
		this.mainEntry = em.getList().get(0);
		listMethods = em.getList();
		
		for(Entry e : listMethods)
			ghostVariables.put(e, new ArrayList<Pair<Register,Register>>());
		
		setHeap();
		
	}
	
	protected void setHeap(){
		
		AccumulatedTuples acTup = new AccumulatedTuples();
		relShare.run();
		relShare.load();
		relShare.accumulatedTuples = acTup;
		relCycle.run();
		relCycle.load();
		relCycle.accumulatedTuples = acTup;
		
	}
	
	
	
	public RelShare getRelShare(){
		return relShare;
	}
	
	public RelCycle getRelCycle(){
		return relCycle;
	}
	
	public boolean updateRels(Entry e){
		AbstractValue a = sm.getSummaryInput(e);
		STuples stuples = a.getSComp();
		CTuples ctuples = a.getCComp();
		
		
		// THIS IF IS NECCESARY BECAUSE THE METHOD MAIN ALSO IS USED IN THIS FUNCTION
		// AND THE MAIN METHOD DOESN´T HAVE CALL SITE
		ArrayList<Pair<Register, FieldSet>> cycleMoved = new ArrayList<>();
		ArrayList<chord.util.tuple.object.Quad<Register,Register,FieldSet,FieldSet>> shareMoved = new ArrayList<>();
		if(e.getCallSite() != null){
			
			int begin = 0;
	    	Utilities.out("\t PARAM WORDS " + e.getMethod().getParamWords());
	    	if(e.getMethod().isStatic()){ 
		    	begin = 0;
		    }else{ 
		    	begin = 1; 
		    }
			
	    	
			// LIST OF PARAM REGISTERS OF THE METHOD
	    	List<Register> paramCalledRegisters = new ArrayList<>();
    		List<Register> paramCallerRegisters = new ArrayList<>();
    		//Utilities.out("- PARAM CALLED REGISTERS");
    		for(int i = begin; i < e.getMethod().getParamWords(); i++){
				Register r = e.getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i, e.getMethod().getParamTypes()[i]);
				if(r.getType().isPrimitiveType()) continue;
				paramCalledRegisters.add(r);
			}
			//Utilities.out("- PARAM CALLER REGISTERS");
			for(int i = begin; i < e.getCallSite().getUsedRegisters().size(); i++){
				RegisterOperand r = e.getCallSite().getUsedRegisters().get(i);
				if(r.getRegister().getType().isPrimitiveType()) continue;
				paramCallerRegisters.add(r.getRegister());
			}
			
			

			shareMoved = stuples.moveTuplesList(paramCallerRegisters, paramCalledRegisters);
			cycleMoved = ctuples.moveTuplesList(paramCallerRegisters, paramCalledRegisters);
		}
		
		boolean changed = false;
		// COPY THE TUPLES OF THE BEFORE REGISTERS TO THE RELS OF THE METHOD
		
		Utilities.out("\t - TUPLES COPIED TO RELS FOR ENTRY " + e);
		for(Pair<Register,FieldSet> p : cycleMoved){
			if(p == null || p.val0 == null || p.val1 == null) continue;
			Utilities.out("\t (" + p.val0 + "," + p.val1 + ")");
			changed |= relCycle.condAdd(e,p.val0, p.val1);
		}
		for(Quad<Register,Register,FieldSet,FieldSet> q : shareMoved){
			if(q == null || q.val0 == null || q.val1 == null || q.val2 == null || q.val3 == null) continue;
			Utilities.out("\t (" + q.val0 + "," + q.val1 + "," + q.val2 + "," + q.val3 + ")");
			changed |= relShare.condAdd(e,q.val0, q.val1, q.val2, q.val3);
		}
		
		createGhostVariables(e);
		
		Utilities.out("---- TUPLES AFTER UPDATE RELS FOR ENTRY " + e);
		for(Pent<Entry,Register,Register,FieldSet,FieldSet> pent: relShare.getAccumulatedTuples().share)
			if(pent.val0 == e)
				Utilities.out("\t (" + pent.val1 + "," + pent.val2 + "," + pent.val3 + "," +pent.val4 + ")");
		for(Trio<Entry,Register,FieldSet> trio: relShare.getAccumulatedTuples().cycle)
			if(trio.val0 == e)
			Utilities.out("\t (" +trio.val1 + "," + trio.val2 + ")");
		
		return changed;
		
	}
	
	/**
	 * 
	 * @param e
	 */
	protected void createGhostVariables(Entry e){
		AbstractValue input = sm.getSummaryInput(e);
		if(input == null) return;
		if(e.getCallSite() == null) return;
		
		List<Register> paramRegisters= new ArrayList<>();
		
		int begin = 0;
		if(e.getMethod().isStatic()){ 
    		begin = 0;
    	}else{ 
    		begin = 1; 
    	}
		
		DomRegister domR = (DomRegister) ClassicProject.g().getTrgt("Register");
		
		for(int i = begin; i < e.getMethod().getParamWords(); i++){
			if(e.getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i, e.getCallSite().getUsedRegisters().get(i).getType()).isTemp()) continue;
			paramRegisters.add(e.getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i, e.getMethod().getParamTypes()[i]));
		}
		
		for(Register ro : paramRegisters){
			if(ro.getType().isPrimitiveType()) continue;
			RegisterFactory rf = e.getMethod().getCFG().getRegisterFactory();
			RegisterOperand rop = rf.makeTempRegOp(ro.getType());
			Register rprime = rop.getRegister();
		
			Utilities.out("----- GHOST VARIABLE CREATED " + rprime.toString());
			Utilities.out("----- [INIT] SAVE DOM");
			
			domR = (DomRegister) ClassicProject.g().getTrgt("Register");
			domR.add(rprime);
			domR.save();
			
			ghostVariables.get(e).add(new Pair<Register,Register>(ro,rprime));
			
			Utilities.out("----- [END] SAVE DOM");
			
			
		}
		
		Utilities.out("----- [INIT] REINITIALIZE RELS ");
		relCycle.reinitialize();
		relShare.reinitialize();
	
		for(Pair<Register,Register> p : ghostVariables.get(e)){
			Utilities.out("----- COPY TUPLES FROM " + p.val0 + " TO GHOST VARIABLE " + p.val1);		
			relShare.copyTuples(e, p.val0, p.val1);
			relCycle.copyTuples(e, p.val0, p.val1);
		}
		Utilities.out("----- [END] REINITIALIZE RELS");
	}
	
	public void deleteGhostVariables(Entry e){
		if(e.getCallSite() == null) return;
		
		List<Register> paramRegisters= new ArrayList<>();
		
		int begin = 0;
		if(e.getMethod().isStatic()){ 
    		begin = 0;
    	}else{ 
    		begin = 1; 
    	}
		
		for(int i = begin; i < e.getMethod().getParamWords(); i++){
			if(e.getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i, e.getCallSite().getUsedRegisters().get(i).getType()).isTemp()) continue;
			paramRegisters.add(e.getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i, e.getMethod().getParamTypes()[i]));
		}
		
		ArrayList<Pair<Register,Register>> ghostvariablestodelete = new ArrayList<>();
		for(Register ro : paramRegisters){
			if(ro.getType().isPrimitiveType()) continue;
			for(Pair<Register,Register> p : ghostVariables.get(e)){
				if(p.val0 == ro){
					Utilities.out("----- TUPLES OF GHOST VARIABLE DELETED " + p.val1.toString());
					relShare.moveTuples(e, p.val1, p.val0);
					relShare.moveTuples(e, p.val1, p.val0);
					ghostvariablestodelete.remove(p);
					break;
				}
			}
		}
		ghostVariables.get(e).remove(ghostvariablestodelete);
	}
	
	public void printMethods(){
		for(Entry e : listMethods)
			Utilities.out("M: " + e.getMethod() + "," + e.getCallSite());
	}
	
	public void printOutput() {
	    	
	    	Hashtable<String, Pair<Register,Register>> registers = RegisterManager.printVarRegMap(mainMethod);
			for (Pair<Register,Register> p : registers.values()) 
				for(Pair<Register,Register> q : registers.values())
					relShare.accumulatedTuples.askForS(mainEntry, p.val0, q.val0);
			for (Pair<Register,Register> p : registers.values()) 
					relCycle.accumulatedTuples.askForC(mainEntry, p.val0);
		}
}
