package chord.analyses.jgbHeap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import chord.analyses.damianoAnalysis.DomRegister;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.method.DomM;
import chord.bddbddb.Rel.RelView;
import chord.program.Program;
import chord.project.ClassicProject;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Quad;
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
	private HashMap<Entry,ArrayList<Pair<Register,Register>>> ghostVariables;
	
	public HeapProgram(jq_Method mainMethod){
		
		this.mainMethod = mainMethod;
		
		// INITIALIZE STRUCTURE OF HEAP STATE
		outShares = new HashMap<Entry,ArrayList<Pair<Register,Register>>>(); 
		outCycles = new HashMap<Entry,ArrayList<Register>>();
		relCycles = new HashMap<Entry,RelCycle>();
		relShares = new HashMap<Entry,RelShare>();
		ghostVariables = new HashMap<>();
		
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
		RelShare sharePrincipal = (RelShare) ClassicProject.g().getTrgt("HeapShare");
		RelShare share = new RelShare();
		share.setSign(sharePrincipal.getSign());
		share.setDoms(sharePrincipal.getDoms());
		share.setName(sharePrincipal.getName() + entry.toString());
		relShares.put(entry, share);
		relShares.get(entry).run();
		relShares.get(entry).load();
		relShares.get(entry).setAccumulatedTuples(acTup);
		RelCycle cyclePrincipal = (RelCycle) ClassicProject.g().getTrgt("HeapCycle");
		RelCycle cycle = new RelCycle();
		cycle.setSign(cyclePrincipal.getSign());
		cycle.setDoms(cyclePrincipal.getDoms());
		cycle.setName(cyclePrincipal.getName() + entry.toString());
		relCycles.put(entry,cycle);
		relCycles.get(entry).run();
		relCycles.get(entry).load();
		relCycles.get(entry).setAccumulatedTuples(acTup);
		outShares.put(entry, new ArrayList<Pair<Register,Register>>());
		outCycles.put(entry, new ArrayList<Register>());
		ghostVariables.put(entry, new ArrayList<Pair<Register,Register>>());
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
			changed |= relCycles.get(e).condAdd(p.val0, p.val1);
		}
		for(Quad<Register,Register,FieldSet,FieldSet> q : movedShare){
			if(q == null || q.val0 == null || q.val1 == null || q.val2 == null || q.val3 == null) continue;
			//Utilities.out("\t (" + q.val0 + "," + q.val1 + "," + q.val2 + "," + q.val3 + ")");
			changed |= relShares.get(e).condAdd(q.val0, q.val1, q.val2, q.val3);
		}
		
		
		createGhostVariables(e);
		
		Utilities.out("---- TUPLES AFTER UPDATE RELS FOR ENTRY " + e);
		for(Quad<Register,Register,FieldSet,FieldSet> quad: relShares.get(e).accumulatedTuples.share)
			Utilities.out("\t (" + quad.val0 + "," + quad.val1 + "," + quad.val2 + "," +quad.val3 + ")");
		for(Pair<Register,FieldSet> pair: relShares.get(e).accumulatedTuples.cycle)
			Utilities.out("\t (" + pair.val0 + "," + pair.val1 + ")");
		
		return changed;
		
	}
	
	protected void createGhostVariables(Entry e){
		AbstractValue input = sm.getSummaryInput(e);
		if(input == null) return;
		if(e.getCallSite() == null) return;
		
		ArrayList<Pair<Register,FieldSet>> cycle = input.getCComp().getTuples();
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> share = input.getSComp().getTuples();
		
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
			Utilities.out("----- [INIT] SAVE DOM AND REINITIALIZE RELS");
			
			dom.add(rprime);
			dom.save();
			AccumulatedTuples acc = relShares.get(e).getAccumulatedTuples();
			relShares.get(e).zero();
			relShares.get(e).setAccumulatedTuples(acc);
			relCycles.get(e).zero();
			relCycles.get(e).setAccumulatedTuples(acc);
			ghostVariables.get(e).add(new Pair<Register,Register>(ro,rprime));
			
			Utilities.out("----- [END] SAVE DOM AND REINITIALIZE RELS");
			
			// COPY TUPLES FROM TO COPY TO COPIED
			Utilities.out("----- COPY TUPLES FROM " + ro + " TO GHOST VARIABLE " + rprime);
			ArrayList<Pair<Register,FieldSet>> cycleToCopy = new ArrayList<>();
			ArrayList<Quad<Register,Register,FieldSet,FieldSet>> shareToCopy = new ArrayList<>();			
			for(Quad<Register,Register,FieldSet,FieldSet> quad : share){
				if(quad.val0 == ro){
					//Utilities.out("(" + rprime + "," + quad.val1 + "," + quad.val2 + "," + quad.val3 + ")");
					relShares.get(e).condAdd(rprime, quad.val1,quad.val2,quad.val3);
					shareToCopy.add(new Quad<Register,Register,FieldSet,FieldSet>(rprime,quad.val1,quad.val2,quad.val3));
				}else if(quad.val1 == ro){
					//Utilities.out("(" + quad.val0 + "," + rprime + "," + quad.val2 + "," + quad.val3 + ")");
					relShares.get(e).condAdd(quad.val0, rprime,quad.val2,quad.val3);
					shareToCopy.add(new Quad<Register,Register,FieldSet,FieldSet>(quad.val0,rprime,quad.val2,quad.val3));
				}
			}
			for(Pair<Register,FieldSet> pair : cycle){
				if(pair.val0 == ro){
					//Utilities.out("(" + rprime + "," + pair.val1 + ")");
					relCycles.get(e).condAdd(rprime, pair.val1);
					cycleToCopy.add(new Pair<Register,FieldSet>(rprime,pair.val1));
				}
			}
		}
	}
	
	public void deleteGhostVariables(Entry e){
		AbstractValue output = sm.getSummaryOutput(e);
		if(output == null) return;
		if(e.getCallSite() == null) return;
		
		ArrayList<Pair<Register,FieldSet>> cycle = output.getCComp().getTuples();
		ArrayList<Quad<Register,Register,FieldSet,FieldSet>> share = output.getSComp().getTuples();
		
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
		
		for(Register ro : paramRegisters){
			if(ro.getType().isPrimitiveType()) continue;
			for(Pair<Register,Register> p : ghostVariables.get(e)){
				if(p.val0 == ro){
					ArrayList<Pair<Register,FieldSet>> cyclesToDelete = new ArrayList<>();
					ArrayList<Quad<Register,Register,FieldSet,FieldSet>> sharesToDelete = new ArrayList<>();
					Utilities.out("----- TUPLES OF GHOST VARIABLE DELETED " + p.val1.toString());
					for(Pair<Register,FieldSet> pair : cycle){ 
						if(pair.val0 == p.val1) {
							Utilities.out("(" + pair.val0 + "," + pair.val1 + ")");
							cyclesToDelete.add(pair);
						}
					}
					for(Quad<Register,Register,FieldSet,FieldSet> quad : share){
						if(quad.val0 == p.val1 || quad.val1 == p.val1) {
							Utilities.out("(" + quad.val0 + "," + quad.val1 + "," + quad.val2 + "," + quad.val3 + ")");
							sharesToDelete.add(quad);	
						}
					}
					cycle.removeAll(cyclesToDelete);
					share.removeAll(sharesToDelete);
					relShares.get(e).removeTuples(p.val1);
					relCycles.get(e).removeTuples(p.val1);
					ghostVariables.get(e).remove(p);
					break;
				}
			}
		}
	}
	
	public void printMethods(){
		for(Entry e : listMethods)
			Utilities.out("M: " + e.getMethod() + "," + e.getCallSite());
	}
	
	public void printOutput() {
	    	
	    	Hashtable<String, Pair<Register,Register>> registers = RegisterManager.printVarRegMap(mainMethod);
			for (Pair<Register,Register> p : registers.values()) 
				for(Pair<Register,Register> q : registers.values())
					relShares.get(mainEntry).accumulatedTuples.askForS(mainMethod, p.val0, q.val0);
			for (Pair<Register,Register> p : registers.values()) 
					relCycles.get(mainEntry).accumulatedTuples.askForC(mainMethod, p.val0);
		}
}
