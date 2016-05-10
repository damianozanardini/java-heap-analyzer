package chord.analyses.jgbHeap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import chord.analyses.damianoAnalysis.Utilities;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;
import joeq.Class.jq_Method;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;

public class HeapProgram {
	
	private jq_Method mainMethod;
	
	public jq_Method getMainMethod(){ return this.mainMethod; }
	
	private Entry mainEntry;
	
	public Entry getMainEntry(){ return this.mainEntry; } 
	
	public void setMainMethod(Entry e){ 
		this.mainMethod = e.getMethod();
		setHeap(e);
	}
	
	private ArrayList<Entry> listMethods;
	
	public void setListMethods(ArrayList<Entry> methods){
		this.listMethods = methods; 
		
		for(Entry e : listMethods){
			setHeap(e);
		}
	}
	
	public ArrayList<Entry> getListMethods(){ return this.listMethods; }
	
	private EntryManager em;
	
	public EntryManager getEntryManager(){ return this.em; }
	
	private SummaryManager sm;
	
	public SummaryManager getSummaryManager(){ return this.sm; }
	
	private HashMap<Entry,ArrayList<Pair<Register,Register>>> outShares;
	private HashMap<Entry,ArrayList<Register>> outCycles;	
	private HashMap<Entry,RelCycle> relCycles;
	private HashMap<Entry,RelShare> relShares;
	
	public HeapProgram(jq_Method mainMethod){
		
		this.mainMethod = mainMethod;
		
		// INITIALIZE STRUCTURE OF HEAP STATE
		outShares = new HashMap<Entry,ArrayList<Pair<Register,Register>>>(); 
		outCycles = new HashMap<Entry,ArrayList<Register>>();
		relCycles = new HashMap<Entry,RelCycle>();
		relShares = new HashMap<Entry,RelShare>();
		
		// ENTRY AND SUMMARY MANAGER
		em = new EntryManager(mainMethod);
		sm = new SummaryManager(mainMethod,em);
		
		// CREATE STRUCTURE OF MAIN ENTRY
		mainEntry = em.getList().get(0);
		setHeap(mainEntry);
		
		listMethods = em.getList();
		for(Entry e : listMethods){
			setHeap(e);
		}
	}
	
	protected void setHeap(Entry entry){
		
		AccumulatedTuples acTup = new AccumulatedTuples();
		RelShare share = (RelShare) ClassicProject.g().getTrgt("HeapShare");
		relShares.put(entry, share);
		relShares.get(entry).run();
		relShares.get(entry).load();
		relShares.get(entry).setAccumulatedTuples(acTup);
		RelCycle cycle = (RelCycle) ClassicProject.g().getTrgt("HeapCycle");
		relCycles.put(entry,cycle);
		relCycles.get(entry).run();
		relCycles.get(entry).load();
		relCycles.get(entry).setAccumulatedTuples(acTup);
		outShares.put(entry, new ArrayList<Pair<Register,Register>>());
		outCycles.put(entry, new ArrayList<Register>());
	}
	
	
	
	public RelShare getRelShare(Entry e){
		return relShares.get(e);
	}
	
	public RelCycle getRelCycle(Entry e){
		return relCycles.get(e);
	}
	
	public ArrayList<Pair<Register,Register>> getOutShare(Entry e){
		return outShares.get(e);
	}
	
	public ArrayList<Register> getOutCycle(Entry e){
		return outCycles.get(e);
	}
	
	public boolean updateRels(Entry e){
		AbstractValue a = sm.getSummaryInput(e);
		STuples stuples = a.getSComp();
		CTuples ctuples = a.getCComp();
		
		// THIS IF IS NECCESARY BECAUSE THE METHOD MAIN ALSO IS USED IN THIS FUNCTION
		// AND THE MAIN METHOD DOESN´T HAVE CALL SITE 
		if(e.getCallSite() != null){
			
			int begin = 0;
	    	Utilities.out("\t PARAM WORDS " + e.getMethod().getParamWords());
	    	if(e.getMethod().isStatic()){ 
		    	begin = 0;
		    }else{ 
		    	begin = 1; 
		    }
			
			// LIST OF PARAM REGISTERS OF THE METHOD
			List<Register> registers = e.getMethod().getLiveRefVars();
			List<Register> paramRegisters = new ArrayList<>();
			for(int i = begin; i < e.getMethod().getParamWords(); i++){
				paramRegisters.add(registers.get(i));
			}
			
			// MOVE TUPLES FROM REGISTERS OF CALLING METHOD TO THE
			// REGISTERS OF THE CALLED METHOD
			int count = 0;
			for(int i = begin; i < e.getCallSite().getUsedRegisters().size(); i++){
				RegisterOperand ro = e.getCallSite().getUsedRegisters().get(i);
				if(ro.getType().isPrimitiveType()) continue;
				
				Utilities.out("\t MOVE TUPLES FROM CALLER REGISTER " + ro.getRegister() + " TO CALLED REGISTER " +  paramRegisters.get(count));
				stuples.moveTuples(ro.getRegister(), paramRegisters.get(count));
				ctuples.moveTuples(ro.getRegister(), paramRegisters.get(count));
				count++;
			}
		}
		
		boolean changed = false;
		// COPY THE TUPLES OF THE BEFORE REGISTERS TO THE RELS OF THE METHOD
		for(Quad<Register,Register,FieldSet,FieldSet> q : stuples.getTuples()){
			if(q == null || q.val0 == null || q.val1 == null || q.val2 == null || q.val3 == null) continue;
			//Utilities.out("R: " + q.val0 + ", R: " + q.val1 + ", F: " + q.val2 + ", F: " + q.val3);
			changed |= relShares.get(e).condAdd(q.val0, q.val1, q.val2, q.val3);
		}
		for(Pair<Register,FieldSet> p : ctuples.getTuples()){
			if(p == null || p.val0 == null || p.val1 == null) continue;
			//Utilities.out("R: " + p.val0 + ", F: " + p.val1);
			changed |= relCycles.get(e).condAdd(p.val0, p.val1);
		}
		//createGhostVariables(e);
		
		return changed;
	}
	
	protected void createGhostVariables(Entry e){
		AbstractValue input = sm.getSummaryInput(e);
		if(input == null) return;
		if(e.getCallSite() == null) return;
		
		ArrayList<Pair<Register,FieldSet>> cycle = input.getCComp().getTuples();
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> share = input.getSComp().getTuples();
		
		List<Register> registers = e.getMethod().getLiveRefVars();
		List<Register> paramRegisters= new ArrayList<>();
		for(int i = 0; i < e.getMethod().getParamWords(); i++){
			paramRegisters.add(registers.get(i));
		}
		for(Register ro : paramRegisters){
			if(ro.getType().isPrimitiveType()) continue;
			RegisterFactory rf = e.getMethod().getCFG().getRegisterFactory();
			
			//Register rprime = op.getRegister();
			//Register rprime2 = rprime.copy();
			//Register rprime = p.val0.copy();
			//relCycles.get(e).copyTuplesTemp(ro, rprime);
			//relShares.get(e).copyTuplesTemp(ro, rprime);
		}
	}
}
