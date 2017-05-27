package chord.analyses.damianoAnalysis.mgb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import chord.analyses.damianoAnalysis.DomRegister;
import chord.analyses.damianoAnalysis.Entry;
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
		
	private AccumulatedTuples accumulatedTuples;
	public AccumulatedTuples getAccumulatedTuples() { return accumulatedTuples; }
	
	private HashMap<Entry,ArrayList<Pair<Register,Register>>> ghostVariables;
	
	public HeapProgram(jq_Method m){
		
		this.mainMethod = m;
		
		// INITIALIZE THE STRUCTURE OF HEAP STATE
		relCycle = (RelCycle) ClassicProject.g().getTrgt("HeapCycle");
		relCycle.setProgram(this);
		relShare = (RelShare) ClassicProject.g().getTrgt("HeapShare");
		relShare.setProgram(this);
		ghostVariables = new HashMap<>();
		
		// ENTRY AND SUMMARY MANAGER
		entryManager = new EntryManager(m);
		
		summaryManager = new SummaryManager();
		
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
		
		accumulatedTuples = new AccumulatedTuples();
		relShare.run();
		relShare.load();
		relCycle.run();
		relCycle.load();		
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
					accumulatedTuples.askForS(e, p.val0, q.val0);
				}
			for (Pair<Register,Register> p : registers.values()){ 
				accumulatedTuples.askForC(e, p.val0);
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
