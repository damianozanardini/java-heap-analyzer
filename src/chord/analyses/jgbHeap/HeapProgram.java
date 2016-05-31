package chord.analyses.jgbHeap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import chord.analyses.damianoAnalysis.DomRegister;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.method.DomM;
import chord.bddbddb.Rel.PentIterable;
import chord.bddbddb.Rel.RelView;
import chord.bddbddb.Rel.TrioIterable;
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
		mainEntry = em.getList().get(0);
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
		ArrayList<Pair<Register,FieldSet>> movedCycle = new ArrayList<>();
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> movedShare = new ArrayList<>();
		if(e.getCallSite() != null){
			
			int begin = 0;
	    	Utilities.out("\t PARAM WORDS " + e.getMethod().getParamWords());
	    	if(e.getMethod().isStatic()){ 
		    	begin = 0;
		    }else{ 
		    	begin = 1; 
		    }
			
			// LIST OF PARAM REGISTERS OF THE METHOD
	    	List<Register> paramRegisters = new ArrayList<>();
			for(int i = begin; i < e.getMethod().getParamWords(); i++){
				paramRegisters.add(e.getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i, e.getMethod().getParamTypes()[i]));
			}
			
			// MOVE TUPLES FROM REGISTERS OF CALLING METHOD TO THE
			// REGISTERS OF THE CALLED METHOD
			int count = 0;
			for(int i = begin; i < e.getCallSite().getUsedRegisters().size(); i++){
				RegisterOperand ro = e.getCallSite().getUsedRegisters().get(i);
				if(ro.getType().isPrimitiveType()) continue;
				
				Utilities.out("\t - MOVE TUPLES FROM CALLER REGISTER " + ro.getRegister() + " TO CALLED REGISTER " +  paramRegisters.get(count));
				movedShare = stuples.moveTuples(ro.getRegister(), paramRegisters.get(count));
				movedCycle = ctuples.moveTuples(ro.getRegister(), paramRegisters.get(count));
				count++;
			}
		}
		
		boolean changed = false;
		// COPY THE TUPLES OF THE BEFORE REGISTERS TO THE RELS OF THE METHOD
		
		Utilities.out("\t - TUPLES COPIED TO RELS FOR ENTRY " + e);
		for(Pair<Register,FieldSet> p : movedCycle){
			if(p == null || p.val0 == null || p.val1 == null) continue;
			//Utilities.out("\t (" + p.val0 + "," + p.val1 + ")");
			changed |= relCycle.condAdd(e,p.val0, p.val1);
		}
		for(Quad<Register,Register,FieldSet,FieldSet> q : movedShare){
			if(q == null || q.val0 == null || q.val1 == null || q.val2 == null || q.val3 == null) continue;
			//Utilities.out("\t (" + q.val0 + "," + q.val1 + "," + q.val2 + "," + q.val3 + ")");
			changed |= relShare.condAdd(e,q.val0, q.val1, q.val2, q.val3);
		}
		
		
		createGhostVariables(e,movedCycle,movedShare);
		
		Utilities.out("---- TUPLES AFTER UPDATE RELS FOR ENTRY " + e);
		for(Pent<Entry,Register,Register,FieldSet,FieldSet> pent: relShare.getAccumulatedTuples().share)
			if(pent.val0 == e)
				Utilities.out("\t (" + pent.val1 + "," + pent.val2 + "," + pent.val3 + "," +pent.val4 + ")");
		for(Trio<Entry,Register,FieldSet> trio: relShare.accumulatedTuples.cycle)
			if(trio.val0 == e)
			Utilities.out("\t (" +trio.val1 + "," + trio.val2 + ")");
		
		return changed;
		
	}
	
	/**
	 * 
	 * @param e
	 */
	protected void createGhostVariables(Entry e, ArrayList<Pair<Register,FieldSet>> cycle, ArrayList<Quad<Register,Register,FieldSet,FieldSet>> share){
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
		
			DomRegister dom = (DomRegister) ClassicProject.g().getTrgt("Register");
			while(dom.contains(rprime)){
				rop = rf.makeTempRegOp(ro.getType());
				rprime = rop.getRegister();
			}
			Utilities.out("----- GHOST VARIABLE CREATED " + rprime.toString());
			Utilities.out("----- [INIT] SAVE DOM");
			
			dom.add(rprime);
			dom.save();
			
			ghostVariables.get(e).add(new Pair<Register,Register>(ro,rprime));
			
			Utilities.out("----- [END] SAVE DOM");
			
			
		}
		
		Utilities.out("----- [INIT] REINITIALIZE RELS ");
		PentIterable<Entry,Register,Register,FieldSet,FieldSet> shareIterable = relShare.getView().getAry5ValTuples();
		TrioIterable<Entry,Register,FieldSet> cycleIterable = relCycle.getView().getAry3ValTuples();
		relShare.zero();
		relShare.setIterable(shareIterable);
		relCycle.zero();
		relCycle.setIterable(cycleIterable);
	
		for(Pair<Register,Register> p : ghostVariables.get(e)){
			Utilities.out("----- COPY TUPLES FROM " + p.val0 + " TO GHOST VARIABLE " + p.val1);		
			for(Quad<Register,Register,FieldSet,FieldSet> quad : share){
				Utilities.out(quad.val0 + "," + quad.val1 + "," + quad.val2 + "," + quad.val3);
				if(quad.val0 == p.val0){
					Utilities.out("(" + p.val1 + "," + quad.val1 + "," + quad.val2 + "," + quad.val3 + ")");
					relShare.condAdd(e,p.val1, quad.val1,quad.val2,quad.val3);
				}else if(quad.val1 == p.val0){
					Utilities.out("(" + quad.val0 + "," + p.val1 + "," + quad.val2 + "," + quad.val3 + ")");
					relShare.condAdd(e,quad.val0, p.val1,quad.val2,quad.val3);
				}
			}
			for(Pair<Register,FieldSet> pair : cycle){
				if(pair.val0 == p.val0){
					Utilities.out("(" + p.val1 + "," + pair.val1 + ")");
					relCycle.condAdd(e,p.val1, pair.val1);
				}
			}
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
