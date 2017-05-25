package chord.analyses.damianoAnalysis.mgb;

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

/**
 * This class represents a program composed by the calls to methods
 * that occur in a Java Class.  
 * @author Javier
 *
 */
public class HeapProgram {
	
	// the method from which the analysis is supposed to begin (not necessarily the Java main method)
	private jq_Method mainMethod;
	public jq_Method getMainMethod(){ return this.mainMethod; }
	
	private Entry mainEntry;
	public Entry getMainEntry(){ return this.mainEntry; } 
	
	private ArrayList<Entry> entryList;
	public ArrayList<Entry> getEntryList(){ return this.entryList; }
	
	private EntryManager entryManager;
	public EntryManager getEntryManager(){ return this.entryManager; }
	
	private SummaryManager summaryManager;
	public SummaryManager getSummaryManager(){ return this.summaryManager; }
		
	private RelCycle relCycle;
	public RelCycle getRelCycle(){ return relCycle;	}
	
	private RelShare relShare;
	public RelShare getRelShare(){ return relShare; }
		
	private HashMap<Entry,ArrayList<Pair<Register,Register>>> ghostVariables;
	
	public HeapProgram(jq_Method m){
		
		this.mainMethod = m;
		
		// INITIALIZE THE STRUCTURE OF HEAP STATE
		relCycle = (RelCycle) ClassicProject.g().getTrgt("HeapCycle");
		relShare = (RelShare) ClassicProject.g().getTrgt("HeapShare");
		ghostVariables = new HashMap<>();
		
		// ENTRY AND SUMMARY MANAGER
		entryManager = new EntryManager(m);
		
		summaryManager = new SummaryManager(m,entryManager);
		
		// CREATE STRUCTURE OF MAIN ENTRY
		this.mainEntry = entryManager.getList().get(0);
		entryList = entryManager.getList();
		
		for(Entry e : entryList)
			ghostVariables.put(e, new ArrayList<Pair<Register,Register>>());
		
		setHeap();
		
	}
	
	/**
	 * Initialize the state of the program 
	 */
	protected void setHeap(){
		
		AccumulatedTuples acTup = new AccumulatedTuples();
		relShare.run();
		relShare.load();
		relShare.accumulatedTuples = acTup;
		relCycle.run();
		relCycle.load();
		relCycle.accumulatedTuples = acTup;
		
	}
	
	/**
	 * Updates the information of the state of the entry e in the program.
	 * To this purpose, tuples of the input of the entry are copied to the
	 * entry relations.
	 * Ghost variables are also created.
	 *   
	 * @param e
	 * @return boolean
	 */
	public boolean updateRels(Entry e){
		AbstractValue a = summaryManager.getSummaryInput(e);
		STuples stuples = a.getSComp();
		CTuples ctuples = a.getCComp();
		Utilities.info("INPUT SUMMARY VALUE FOR SHARING:   " + stuples);
		Utilities.info("INPUT SUMMARY VALUE FOR CYCLICITY: " + ctuples);

		ArrayList<Pair<Register, FieldSet>> cycleMoved = new ArrayList<>();
		ArrayList<chord.util.tuple.object.Quad<Register,Register,FieldSet,FieldSet>> shareMoved = new ArrayList<>();

		boolean changed = false;

		// the main has no call site
		if (!e.isTheMain()){
			int begin = e.getMethod().isStatic()? 0 : 1;
	    	Utilities.out("\t PARAM WORDS " + e.getMethod().getParamWords());
			
			// LIST OF PARAM REGISTERS OF THE METHOD
	    	List<Register> paramCalledRegisters = new ArrayList<>();
    		List<Register> paramCallerRegisters = new ArrayList<>();
    		//Utilities.out("- PARAM CALLED REGISTERS");
    		for(int i = begin; i < e.getMethod().getParamWords(); i++){
				Register r = e.getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i, e.getMethod().getParamTypes()[i]);
				if(r.getType().isPrimitiveType()) continue;
				paramCalledRegisters.add(r);
			}
			// Utilities.out("- PARAM CALLER REGISTERS");
			for(int i = begin; i < e.getCallSite().getUsedRegisters().size(); i++){
				RegisterOperand r = e.getCallSite().getUsedRegisters().get(i);
				if(r.getRegister().getType().isPrimitiveType()) continue;
				paramCallerRegisters.add(r.getRegister());
			}
			
			shareMoved = stuples.moveTuplesList(paramCallerRegisters, paramCalledRegisters);
			cycleMoved = ctuples.moveTuplesList(paramCallerRegisters, paramCalledRegisters);

			Utilities.begin("TUPLES COPIED TO RELS FOR ENTRY " + e);
			for (Pair<Register,FieldSet> p : cycleMoved){
				if(p == null || p.val0 == null || p.val1 == null) continue;
				Utilities.debug("(" + p.val0 + "," + p.val1 + ")");
				changed |= relCycle.condAdd(e,p.val0, p.val1);
			}
			for (Quad<Register,Register,FieldSet,FieldSet> q : shareMoved){
				if(q == null || q.val0 == null || q.val1 == null || q.val2 == null || q.val3 == null) continue;
				Utilities.debug("(" + q.val0 + "," + q.val1 + "," + q.val2 + "," + q.val3 + ")");
				changed |= relShare.condAdd(e,q.val0, q.val1, q.val2, q.val3);
			}
			Utilities.end("TUPLES COPIED TO RELS FOR ENTRY " + e);
			
			createGhostVariables(e);
		}
		
		// WARNING: not clear if this code should also be executed for main
		Utilities.info("TUPLES AFTER UPDATE RELS FOR ENTRY " + e);
		for (Pent<Entry,Register,Register,FieldSet,FieldSet> pent: relShare.getAccumulatedTuples().share)
			if (pent.val0 == e)
				Utilities.info("\t (" + pent.val1 + "," + pent.val2 + "," + pent.val3 + "," +pent.val4 + ")");
		for (Trio<Entry,Register,FieldSet> trio: relShare.getAccumulatedTuples().cycle)
			if (trio.val0 == e)
				Utilities.info("\t (" +trio.val1 + "," + trio.val2 + ")");
		
		return changed;
	}
	
	/**
	 * Creates the ghost variables of the entry e. It includes: 
	 * 		- Create a new register
	 * 		- Save the new register in DomRegister
	 * 		- Reinitialize the relations
	 * 		- Copy the tuples from the original register to the new register 
	 * 
	 * @param e
	 */
	protected void createGhostVariables(Entry e){
		Utilities.begin("CREATE GHOST VARIABLE FOR " + e);
		AbstractValue input = summaryManager.getSummaryInput(e);
		
		// nothing to do in this case (WARNING: check why)
		if (input == null) return;
				
		List<Register> paramRegisters = new ArrayList<>();
		int begin = e.getMethod().isStatic() ? 0 : 1;
		DomRegister domR = (DomRegister) ClassicProject.g().getTrgt("Register");
		
		// loop on all the parameters
		// (WARNING: shouldn't we ignore primitive types? But probably we can live without it)
		for (int i = begin; i < e.getMethod().getParamWords(); i++){
			if (e.getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i, e.getCallSite().getUsedRegisters().get(i).getType()).isTemp()) continue;
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
		Utilities.end("CREATE GHOST VARIABLE FOR " + e);
	}
	
	/**
	 * Delete the ghost variables of the entry e. For that the tuples of the ghost 
	 * variable register are moved to the original register. Moreover the Pair<Register,Register> that
	 * contains the relation between the ghost variable register and the original register is deleted.
	 * @param e
	 */
	public void deleteNonGhostVariables(Entry e){
		Utilities.begin("DELETE NON-GHOST VARIABLE FOR " + e);
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
		Utilities.end("DELETE NON-GHOST VARIABLE FOR " + e);
	}
	
	/**
	 * Print the methods of the entries of the program and their call sites
	 */
	public void printMethods(){
		for(Entry e : entryList)
			Utilities.out("M: " + e.getMethod() + "," + e.getCallSite());
	}
	
	/**
	 * Print the result of the analysis
	 */
	public void printOutput() {
	    	
		for(Entry e : entryList){
			Utilities.out("- [INIT] HEAP REPRESENTATION FOR ENTRY " + e + "(M: " + e.getMethod() + " )");
			Hashtable<String, Pair<Register,Register>> registers = RegisterManager.printVarRegMap(e.getMethod());
			for (Pair<Register,Register> p : registers.values()) 
				for(Pair<Register,Register> q : registers.values()){
					relShare.accumulatedTuples.askForS(e, p.val0, q.val0);
				}
			for (Pair<Register,Register> p : registers.values()){ 
				relCycle.accumulatedTuples.askForC(e, p.val0);
			}
			Utilities.out("- [END] HEAP REPRESENTATION FOR ENTRY " + e + "(M: " + e.getMethod() + " )");
		}
	}
	
	/**
	 * Return the entries that reference to the passed method
	 * @param act_Method
	 * @return
	 */
	public ArrayList<Entry> getEntriesMethod(jq_Method act_Method) {
		ArrayList<Entry> list = new ArrayList<>();
		
		for(Entry e : entryList)
			if(e.getMethod() == act_Method) list.add(e);
		
		return list;
	}
}
