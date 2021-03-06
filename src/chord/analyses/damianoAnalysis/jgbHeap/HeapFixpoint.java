package chord.analyses.damianoAnalysis.jgbHeap;



import joeq.Class.jq_Field;
import joeq.Class.jq_Member;
import joeq.Class.jq_Method;
import joeq.Class.jq_Type;
import joeq.Compiler.Quad.BasicBlock;
import joeq.Compiler.Quad.Operand;
import joeq.Compiler.Quad.Operand.AConstOperand;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Operand.FieldOperand;
import joeq.Compiler.Quad.Operator;
import joeq.Compiler.Quad.Operator.ALength;
import joeq.Compiler.Quad.Operator.ALoad;
import joeq.Compiler.Quad.Operator.AStore;
import joeq.Compiler.Quad.Operator.Binary;
import joeq.Compiler.Quad.Operator.BoundsCheck;
import joeq.Compiler.Quad.Operator.Branch;
import joeq.Compiler.Quad.Operator.CheckCast;
import joeq.Compiler.Quad.Operator.Getfield;
import joeq.Compiler.Quad.Operator.Getstatic;
import joeq.Compiler.Quad.Operator.Goto;
import joeq.Compiler.Quad.Operator.InstanceOf;
import joeq.Compiler.Quad.Operator.IntIfCmp;
import joeq.Compiler.Quad.Operator.Invoke;
import joeq.Compiler.Quad.Operator.Invoke.InvokeStatic;
import joeq.Compiler.Quad.Operator.Jsr;
import joeq.Compiler.Quad.Operator.LookupSwitch;
import joeq.Compiler.Quad.Operator.MemLoad;
import joeq.Compiler.Quad.Operator.MemStore;
import joeq.Compiler.Quad.Operator.Monitor;
import joeq.Compiler.Quad.Operator.Move.MOVE_A;
import joeq.Compiler.Quad.Operator.MultiNewArray;
import joeq.Compiler.Quad.Operator.New;
import joeq.Compiler.Quad.Operator.Move;
import joeq.Compiler.Quad.Operator.NewArray;
import joeq.Compiler.Quad.Operator.NullCheck;
import joeq.Compiler.Quad.Operator.Phi;
import joeq.Compiler.Quad.Operator.Putfield;
import joeq.Compiler.Quad.Operator.Putfield.PUTFIELD_A;
import joeq.Compiler.Quad.Operator.Putstatic;
import joeq.Compiler.Quad.Operator.Ret;
import joeq.Compiler.Quad.Operator.Return;
import joeq.Compiler.Quad.Operator.Special;
import joeq.Compiler.Quad.Operator.StoreCheck;
import joeq.Compiler.Quad.Operator.TableSwitch;
import joeq.Compiler.Quad.Operator.Unary;
import joeq.Compiler.Quad.Operator.ZeroCheck;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.RegisterFactory;
import joeq.Compiler.Quad.RegisterFactory.Register;
import joeq.Compiler.Quad.PrintCFG;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import chord.analyses.damianoAnalysis.DomRegister;
import chord.analyses.damianoAnalysis.Fixpoint;
import chord.analyses.damianoAnalysis.QuadQueue;
import chord.analyses.damianoAnalysis.RegisterManager;
import chord.analyses.damianoAnalysis.Utilities;
import chord.analyses.invk.DomI;
import chord.project.ClassicProject;
import chord.util.tuple.object.Pair;
import chord.util.tuple.object.Trio;


/**
 * Implementation of the fixpoint.  It adds tuples to sharing
 * and cyclicity relation.
 *
 * @author Damiano Zanardini (damiano@fi.upm.es)
 */
public class HeapFixpoint extends Fixpoint {
    /**
     * The queue for implementing the fixpoint.
     */
	private QuadQueue queue;
	
	/**
	 * The sharing relation.
	 */
	private RelShare relShare;

	public RelShare getRelShare() { return relShare; }

	/**
	 * The cyclicity relation.
	 */
	private RelCycle relCycle;
	
	public RelCycle getRelCycle() { return relCycle; }
	
	
	/**
	 * 
	 */
	private AccumulatedTuples accumulatedTuples;
	
	public AccumulatedTuples getAccumulatedTuples(){ return this.accumulatedTuples; }
	
	private SummaryManager sm;
	
	public void setSummaryManager(SummaryManager sm){ this.sm = sm; }
	
	private EntryManager em;
	
	public void setEntryManager(EntryManager em){ this.em = em; }
	
	protected jq_Method acMeth;
	
	protected Entry acEntry;
	
	protected HeapProgram actProgram;
	
	
	public HeapFixpoint(Entry entry, HeapProgram p){
		
		this.actProgram = p;
		this.acEntry = entry;
		this.acMeth = entry.getMethod();
		this.relShare = p.getRelShare();
		this.relCycle = p.getRelCycle();
		this.accumulatedTuples = relShare.getAccumulatedTuples();
	}
    
    /**
     * This method processes a Quad object {@code q}, branching on the operator.
     * 
     * @param q The Quad to be processed.
     * @return whether new tuples have been added.
     */
    protected boolean process(Quad q) {
    	
    	Operator operator = q.getOperator();
    	if (operator instanceof ALength) {
    		Utilities.debug("IGNORING ALENGTH INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof ALoad) {
    		return processALoad(q);
    	}
    	if (operator instanceof AStore) {
    		return processAStore(q);
    	}
    	if (operator instanceof Binary) {
    		// NOTE: it is not clear what the subclass ALIGN_P of Binary does; here
    		// we assume that all subclasses manipulate primitive types  
    		Utilities.debug("IGNORING BINARY INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof BoundsCheck) {
    		Utilities.debug("IGNORING BOUNDSCHECK INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Branch) {
    		Utilities.debug("IGNORING BRANCH INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof CheckCast) {
    		Utilities.debug("IGNORING CHECKCAST INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Getfield) {
    		return processGetfield(q);
    	}
    	if (operator instanceof Getstatic) {
    		// TO-DO: currently unsupported
    		Utilities.debug("IGNORING GETSTATIC INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Goto) {
    		Utilities.debug("IGNORING GOTO INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof InstanceOf) {
    		Utilities.debug("IGNORING INSTANCEOF INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof IntIfCmp) {
    		Utilities.debug("IGNORING INTIFCMP INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Invoke) {
    		// TO-DO: currently unsupported
    		if ((operator instanceof InvokeStatic) &&
    				q.getOp2().toString().matches("(.*)<init>(.*)")) {
    			Utilities.debug("PROCESSING <init> INVOKESTATIC: " +q);
    			Register r = Invoke.getParam(q,0).getRegister();
    			relShare.removeTuples(acEntry,r);
    			relCycle.removeTuples(acEntry,r);
    			//return processInvokeMethod(q);
    		} else if(q.getOp2().toString().matches("<init>:()V@java.lang.Object")){
    			Utilities.debug("IGNORING INVOKE INSTRUCTION: " + q);
    			return false;
    		} else {   			
    			Utilities.debug("PROCESSING INVOKE INSTRUCTION: " + q);
    			return processInvokeMethod(q);
    		}
    		return false;
    	}
    	if (operator instanceof Jsr) {
    		Utilities.debug("IGNORING JSR INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof LookupSwitch) {
    		// TO-DO: maybe the treatment of this instruction is needed
    		Utilities.debug("IGNORING LOOKUPSWITCH INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof MemLoad) {
    		// TO-DO: not clear; currently unsupported
    		Utilities.debug("IGNORING MEMLOAD INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof MemStore) {
    		// TO-DO: not clear; currently unsupported
    		Utilities.debug("IGNORING MEMSTORE INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Monitor) {
    		// TO-DO: currently unsupported
    		Utilities.debug("IGNORING MONITOR INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Move) {
    		if (operator instanceof MOVE_A)
    			return processMove(q);
    		else Utilities.debug("IGNORING NON-REFERENCE MOVE INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof MultiNewArray) {
    		// TO-DO: currently unsupported
    		Utilities.debug("IGNORING MULTINEWARRAY INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof New) {
    		return processNew(q);
    	}
    	if (operator instanceof NewArray) {
    		return processNewArray(q);
    	}
    	if (operator instanceof NullCheck) {
    		// TO-DO: maybe there could be some optimization here (flow-sensitive)
    		Utilities.debug("IGNORING NULLCHECK INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Phi) {
    		return processPhi(q);
    	}
    	if (operator instanceof Putfield) {
    		// TO-DO: check if there are other subclasses to be processed  
    		if (operator instanceof PUTFIELD_A)
    			return processPutfield(q);
    		else Utilities.debug("IGNORING NON-REFERENCE PUTFIELD INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Putstatic) {
    		// TO-DO: currently unsupported
    		Utilities.debug("IGNORING PUTSTATIC INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Ret) {
    		Utilities.debug("IGNORING RET INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Return) {
    		// TO-DO: currently unsupported
    		Utilities.debug("IGNORING RETURN INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Special) {
    		// TO-DO: currently unsupported, not clear when it is used
    		Utilities.debug("IGNORING SPECIAL INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof StoreCheck) {
    		Utilities.debug("IGNORING STORECHECK INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof TableSwitch) {
    		// TO-DO: currently unsupported
    		Utilities.debug("IGNORING TABLESWITCH INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof Unary) {
    		// TO-DO: subclasses involving addresses and object
    		// (ADDRESS_2OBJECT, OBJECT_2ADDRESS) unsupported
    		Utilities.debug("IGNORING UNARY INSTRUCTION: " + q);
    		return false;
    	}
    	if (operator instanceof ZeroCheck) {
    		Utilities.debug("IGNORING ZEROCHECK INSTRUCTION: " + q);
    		return false;
    	}
    	// This should never happen
    	Utilities.debug("CANNOT DEAL WITH QUAD" + q);
    	return false;
    }

    /**
     * This method simply copies the information about the array variable
     * into the destination, unless the latter has primitive type.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processALoad(Quad q) {
    	Utilities.debug("PROCESSING ALOAD INSTRUCTION: " + q);
    	if (((RegisterOperand) ALoad.getDest(q)).getType().isPrimitiveType())
    		return false;
    	Register base = ((RegisterOperand) ALoad.getBase(q)).getRegister();
    	Register dest = ((RegisterOperand) ALoad.getDest(q)).getRegister();
    	return (relShare.copyTuples(acEntry,base,dest) | relCycle.copyTuples(acEntry,base,dest));
    }
    
    /**
     * This method simply copies the information about the value into the array
     * variable, unless the value has primitive type.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processAStore(Quad q) {
    	Utilities.debug("PROCESSING ASTORE INSTRUCTION: " + q);
    	if (((RegisterOperand) AStore.getValue(q)).getType().isPrimitiveType())
    		return false;
    	Register base = ((RegisterOperand) AStore.getBase(q)).getRegister();
    	Register value = ((RegisterOperand) AStore.getValue(q)).getRegister();
    	return (relShare.moveTuples(acEntry,value,base) | relCycle.moveTuples(acEntry,value,base));
    }
    
    /**
     * This method adds:
     * <ul>
     * <li> a cyclicity tuple ({@code dest},FS) whenever
     * ({@code base},FS) is in the cyclicity relation;
     * <li> a sharing tuple ({@code dest},{@code dest},FS,{})
     * whenever ({@code base},FS) is in the cyclicity relation;
     * <li> a sharing tuple ({@code dest},r,FS-{f},{}) for each
     * sharing tuple ({@code base},r,FS,{});
     * <li> a sharing tuple (r,{@code dest},FS,{}) for every sharing
     * tuple (r,{@code base},FS+{f},{});
     * <li> a sharing tuple (r,{@code dest},FS1,{}) for every sharing tuple
     * (r,{@code base},FS1,{f})
     * <li> two sharing tuples (r,{@code dest},FS1,FS) and
     * (r,{@code dest},FS1,FS+{f}) for every sharing tuple
     * (r,{@code base},FS1,FS+{f});
     * </ul>
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processGetfield(Quad q) {
    	Utilities.debug("PROCESSING GETFIELD INSTRUCTION: " + q);
    	if (((RegisterOperand) Getfield.getDest(q)).getType().isPrimitiveType()) return false;
    	Register base = ((RegisterOperand) Getfield.getBase(q)).getRegister();
    	Register dest = ((RegisterOperand) Getfield.getDest(q)).getRegister();
    	jq_Field field = ((FieldOperand) Getfield.getField(q)).getField();
    	Boolean changed = false;
    	// copy cyclicity from base to dest
    	changed |= relCycle.copyTuples(acEntry,base,dest);
    	// copy self-"reachability" of dest from from cyclicity of base
    	changed |= relShare.copyTuplesFromCycle(acEntry,base,dest,relCycle);
    	// add "reachability" from the "reachability" from base, removing the field
    	for (Pair<Register,FieldSet> p : relShare.findTuplesByReachingRegister(acEntry,base)) {
    		FieldSet fs1 = FieldSet.removeField(p.val1,field);
    		changed |= relShare.condAdd(acEntry,dest,p.val0,fs1,FieldSet.emptyset());
    		// the old field set is still there
    		changed |= relShare.condAdd(acEntry,dest,p.val0,p.val1,FieldSet.emptyset());
    	}
    	// add "reachability" from the "reachability" to base, adding the field
    	for (Pair<Register,FieldSet> p : relShare.findTuplesByReachedRegister(acEntry,base)) {
    		FieldSet fs2 = FieldSet.addField(p.val1,field);
    		changed |= relShare.condAdd(acEntry,p.val0,dest,fs2,FieldSet.emptyset());
    	}
    	// add "reachability" to dest and sharing between r and dest from
    	// sharing between r and base 
    	for (Trio<Register,FieldSet,FieldSet> p : relShare.findTuplesByFirstRegister(acEntry,base)) {
    		if (p.val1.containsOnly(field)) {
    				changed |= relShare.condAdd(acEntry,p.val0,dest,p.val2,FieldSet.emptyset());
    			}
    		FieldSet fs3 = FieldSet.removeField(p.val1,field);
    		changed |= relShare.condAdd(acEntry,base,p.val0,p.val2,fs3);
    		changed |= relShare.condAdd(acEntry,base,p.val0,p.val2,p.val1);
    	}
    	for (Trio<Register,FieldSet,FieldSet> p : relShare.findTuplesBySecondRegister(acEntry,base)) {
    		if (p.val2.containsOnly(field)) {
    				changed |= relShare.condAdd(acEntry,p.val0,dest,p.val1,FieldSet.emptyset());
    			}
    		FieldSet fs4 = FieldSet.removeField(p.val2,field);
    		changed |= relShare.condAdd(acEntry,base,p.val0,p.val1,fs4);
    		changed |= relShare.condAdd(acEntry,base,p.val0,p.val1,p.val2);
    	}
    	return changed;
    }

    /**
     * This method adds a sharing tuple (r,r,{},{}) and a cyclicity tuple
     * (r,{}), where r contains the object created by {@code q}.  In the case
     * of sharing, the empty field set means that the variable is not null (so
     * that it aliases with itself) but does not reach any other variable, nor
     * is reached from any variable.  As for cyclicity, the empty field set
     * means that the only cycle has length 0.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processNew(Quad q) {
    	Utilities.debug("PROCESSING NEW INSTRUCTION: " + q);
    	Register r = ((RegisterOperand) New.getDest(q)).getRegister();
    	return (relCycle.condAdd(acEntry,r,FieldSet.emptyset()) |
    			relShare.condAdd(acEntry,r,r,FieldSet.emptyset(),FieldSet.emptyset()));
    }
    
    /**
     * This method adds a sharing tuple (r,r,{},{}) and a cyclicity tuple
     * (r,{}), where r contains the object created by {@code q}.  In the case
     * of sharing, the empty field set means that the variable is not null (so
     * that it aliases with itself) but does not reach any other variable, nor
     * is reached from any variable.  As for cyclicity, the empty field set
     * means that the only cycle has length 0.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processNewArray(Quad q) {
    	Utilities.debug("PROCESSING NEWARRAY INSTRUCTION: " + q);
    	Register r = ((RegisterOperand) NewArray.getDest(q)).getRegister();
    	return (relCycle.condAdd(acEntry,r,FieldSet.emptyset()) |
    			relShare.condAdd(acEntry,r,r,FieldSet.emptyset(),FieldSet.emptyset()));
    }
    
    /**
     * This method copies all tuples about the source variable into the
     * destination variable.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processMove(Quad q) {
    	Utilities.debug("PROCESSING MOVE INSTRUCTION: " + q);
    	Operand op = Move.getSrc(q);
    	if (op instanceof AConstOperand) return false;
    	if (op instanceof RegisterOperand) {
    		Register src = ((RegisterOperand) op).getRegister();
    		Register dest = ((RegisterOperand) Move.getDest(q)).getRegister();
    		if (src.isTemp() && !dest.isTemp()) { // from a stack variable to a local variable
    			return (relCycle.moveTuples(acEntry, src,dest) | relShare.moveTuples(acEntry, src,dest));
    		} else {
    			return (relCycle.copyTuples(acEntry, src,dest) |	relShare.copyTuples(acEntry, src,dest));
    		}
    	}
    	return false;
    }
    
    /**
     * This method copies all tuples about each of the source variables into the
     * destination variable.
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processPhi(Quad q) {
    	Utilities.debug("PROCESSING PHI INSTRUCTION: " + q);
    	Register src1 = ((RegisterOperand) Phi.getSrc(q,0)).getRegister();
    	Register src2 = ((RegisterOperand) Phi.getSrc(q,1)).getRegister();
    	Register destination = ((RegisterOperand) Phi.getDest(q)).getRegister();
    	relCycle.removeTuples(acEntry,destination);
    	relShare.removeTuples(acEntry,destination);
    	return (relCycle.joinTuples(acEntry,src1,src2,destination) |
    			relShare.joinTuples(acEntry,src1,src2,destination));
    }
    
    /**
     * This method takes a putfield Quad and accounts for new paths in the heap
     * which are created by the heap update {@code base}.f={@code src}.
     * Concretely, it adds:
     * <ul>
     * <li> A new sharing tuple (r1,r2,FS,emptyset) for each r1, r2 and FS such
     * that:
     * <ul>
     * <li> r1 was reaching {@code base} via a path traversing some field set
     * FS1;
     * <li> {@code src} was reaching r2 via a path traversing some field set
     * FS2;
     * <li> FS is the union of FS1, {f} and FS2;
     * </ul>
     * <li> A new cyclicity tuple (r,FS) for each r reaching {@code base}, such
     * that FS is the "reachability" from {@code src} to {@code base} plus {f};
     * <li> A new cyclicity tuple (r,FS) for every cyclicity tuple
     * ({@code src},FS), for every r reaching {@code base};
     * <li> A sharing tuple (r,{@code base},FS1,FS2+{f}) for every sharing
     *  tuple (r,{@code src},FS1,FS2);
     * <li> 
     * </ul>
     * 
     * @param q The Quad to be processed.
     */
    protected boolean processPutfield(Quad q) {
    	Utilities.debug("PROCESSING PUTFIELD INSTRUCTION: " + q);
    	
    	System.out.println("SRC: " + Putfield.getSrc(q));
    	
    	if (Putfield.getSrc(q) instanceof AConstOperand){
    		return false;    	
    	}
    	if (((RegisterOperand) Putfield.getSrc(q)).getType().isPrimitiveType()){
    		return false;
    	}
    	
    	System.out.println("BASE: " + Putfield.getBase(q));
    	
    	Register base = ((RegisterOperand) Putfield.getBase(q)).getRegister();//r6
    	Register src = ((RegisterOperand) Putfield.getSrc(q)).getRegister();//r1
    	jq_Field field = ((FieldOperand) Putfield.getField(q)).getField();//left
    	Boolean changed = false;
    	// add "reachability" created by the new path
    	for (Pair<Register,FieldSet> p1 : relShare.findTuplesByReachedRegister(acEntry,base)) {
        	for (Pair<Register,FieldSet> p2 : relShare.findTuplesByReachingRegister(acEntry,src)) {
    			FieldSet fs1 = FieldSet.union(p1.val1,FieldSet.addField(p2.val1,field));//left
    			changed |= relShare.condAdd(acEntry,p1.val0,p2.val0,fs1,FieldSet.emptyset());//
    			changed |= relShare.condAdd(acEntry,p1.val0,p1.val0,fs1,fs1);
    			for (FieldSet fs2 : relShare.findTuplesByReachingReachedRegister(acEntry,src,base)) {
    				FieldSet fs3 = FieldSet.union(fs1,fs2);
    				changed |= relShare.condAdd(acEntry,p1.val0,p2.val0,fs3,FieldSet.emptyset());
    				changed |= relShare.condAdd(acEntry,p1.val0,p1.val0,fs3,fs3);
    			}
    		}
    	}
    	// add cyclicity of variables "reaching" base
    	for (Pair<Register,FieldSet> p : relShare.findTuplesByReachedRegister(acEntry,base)) {
    		for (FieldSet fs : relShare.findTuplesByReachingReachedRegister(acEntry,src,base)) {
    			FieldSet fs0 = FieldSet.addField(fs,field);
    			changed |= relCycle.condAdd(acEntry,p.val0,fs0);
    			changed |= relShare.condAdd(acEntry,p.val0,p.val0,fs0,fs0);
    		}
    	}
    	// copy cyclicity of src into variables which "reach" base
    	for (Pair<Register,FieldSet> p : relShare.findTuplesByReachedRegister(acEntry,base)) {
    		changed |= relCycle.copyTuples(acEntry,src,p.val0);
    		changed |= relShare.copyTuplesFromCycle(acEntry,src,p.val0,relCycle);
    	}
        // add sharing from sharing
    	for (Trio<Register,FieldSet,FieldSet> t : relShare.findTuplesByFirstRegister(acEntry,src)) {
    		FieldSet fs = FieldSet.addField(t.val1,field);
    		changed |= relShare.condAdd(acEntry,base,t.val0,fs,t.val2);
    	}
    	for (Trio<Register,FieldSet,FieldSet> t : relShare.findTuplesBySecondRegister(acEntry,src)) {
    		FieldSet fs = FieldSet.addField(t.val2,field);
    		changed |= relShare.condAdd(acEntry,t.val0,base,t.val1,fs);
    	}
    	return changed;
    }
    
    /**
     * This method takes an invoke Quad and process it. It includes: 
     * 		-	Update the input of the called entry with the tuples of the registers which are
     * 			passed as params.
     * 		- 	Update the information of the relations of the entry which proccess this invoke Quad
     * 			with the information of the output of the called entry. For this is necessary to change 
     * 			the local registers of the called method to the registers of the calling method. 
     *   
     * @param q
     * @return boolean
     */
    protected boolean processInvokeMethod(Quad q){
    	boolean changed = false;
    	
    	// COPY TUPLES OF INPUT REGISTERS OF CALLED METHOD TO THE SUMMARYMANAGER
    	Utilities.out("- [INIT] COPY TUPLES OF INPUT REGISTERS OF CALLED METHOD TO THE SUMMARYMANAGER FOR ENTRY " + acEntry);
    	AbstractValue av = new AbstractValue();
    	ArrayList<Pair<Register,FieldSet>> cycle = new ArrayList<>();
    	ArrayList<chord.util.tuple.object.Quad<Register,Register,FieldSet,FieldSet>> share = new ArrayList<>();
        
    	//Utilities.out("\t DEFINED AND USED REGISTERS IN INVOKE INSTRUCTION: " + q.getDefinedRegisters().size() + ", " + q.getUsedRegisters().size());
    	// IF METHOD IS STATIC, THE FIRST REGISTER USED AND PARAM BELONG TO THE FIRST PARAM
    	// IF METHOD ISN�T STATIC, THE FIRST REGISTER USED AND PARAM BELONG TO THE CALLER OBJECT (THIS)
    	int begin = 0;
    	try {
    		Utilities.out("\t PARAM WORDS " + em.getRelevantEntry(q).getMethod().getParamWords());
    		if(em.getRelevantEntry(q).getMethod().isStatic()){ 
	    		begin = 0;
	    	}else{ 
	    		begin = 1; 
	    	}
		} catch (NoEntryException e2) { e2.printStackTrace(); }
    	Utilities.out("\t USED REGISTERS BEGIN IN: " + begin);
    	
    	
    	for(int i = begin; i < q.getUsedRegisters().size(); i++){
    		RegisterOperand r = q.getUsedRegisters().get(i);
    		if(r.getType().isPrimitiveType()) continue;
    		
    		Utilities.out("");
    		Utilities.out("\t VARIABLE AS PARAM FOR CYCLICITY " + RegisterManager.getVarFromReg(acMeth,r.getRegister()) + " IN REGISTER " + r.getRegister());
    		
    		cycle.addAll(accumulatedTuples.getCFor(acEntry,r.getRegister()));
    	}
    	CTuples ctuples = new CTuples(cycle);
    	av.setCComp(ctuples);
    	
    	for(int i = begin; i < q.getUsedRegisters().size(); i++){
    		RegisterOperand r = q.getUsedRegisters().get(i);
    		if(r.getType().isPrimitiveType()) continue;
    		for(int j = begin; j < q.getUsedRegisters().size(); j++){
    			RegisterOperand r2 = q.getUsedRegisters().get(j);
    			if(r2.getType().isPrimitiveType()) continue;
    			Utilities.out("\t VARIABLE AS PARAM FOR SHARING " + RegisterManager.getVarFromReg(acMeth,r.getRegister()) + "IN REGISTER " + r.getRegister());
    			share.addAll(accumulatedTuples.getSFor(acEntry, r.getRegister(),r2.getRegister()));
    		}
    	}
    	av.setSComp(new STuples(share));
    	
    	// UPDATE INPUT OF ENTRY
    	boolean changedprime = false;
    	try {
    		changedprime = sm.updateSummaryInput(em.getRelevantEntry(q), av);
			changed |= changedprime;
		} catch (NoEntryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	if(changedprime){
    		Utilities.out("- [FINISHED] COPY TUPLES OF INPUT REGISTERS OF CALLED METHOD TO THE SUMMARYMANAGER WITH CHANGES FOR ENTRY "+ acEntry);
    	}else{
    		Utilities.out("- [FINISHED] COPY TUPLES OF INPUT REGISTERS OF CALLED METHOD TO THE SUMMARYMANAGER WITH NO CHANGES FOR ENTRY " + acEntry);	
    	}
    	
    	// UPDATE ACTUAL INFORMATION WITH OUTPUT OF ENTRY CALLED
    	AbstractValue output = null;
    	try {
			output = sm.getSummaryOutput(em.getRelevantEntry(q));
		} catch (NoEntryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	if(output != null){
    		Utilities.out("- [INIT] CHANGE REGISTERS FROM THE CALLED METHOD TO THE CALLER METHOD FOR ENTRY " +acEntry);
    		STuples shar = output.getSComp();
    		CTuples cycl = output.getCComp();
    		
    		
    		List<Register> paramCalledRegisters = new ArrayList<>();
    		List<Register> paramCallerRegisters = new ArrayList<>();
    		try {
				
				
				for(int i = begin; i < em.getRelevantEntry(q).getMethod().getParamWords(); i++){
					Register r = em.getRelevantEntry(q).getMethod().getCFG().getRegisterFactory().getOrCreateLocal(i, em.getRelevantEntry(q).getMethod().getParamTypes()[i]);
					if(r.getType().isPrimitiveType()) continue;
					paramCalledRegisters.add(r);
				}
				
				for(int i = begin; i < q.getUsedRegisters().size(); i++){
					RegisterOperand r = q.getUsedRegisters().get(i);
					if(r.getRegister().getType().isPrimitiveType()) continue;
					paramCallerRegisters.add(r.getRegister());
				}
			} catch (NoEntryException e) {
				e.printStackTrace();
			}
			
			if(paramCalledRegisters.isEmpty() || paramCallerRegisters.isEmpty()) {
				Utilities.out("- [FINISHED] CHANGE REGISTERS FROM THE CALLED METHOD TO THE CALLER METHOD (PARAM REGISTERS EMPTY)");
				return changed;
			}
			
			
			ArrayList<Pair<Register, FieldSet>> cycleMoved = new ArrayList<>();
			ArrayList<chord.util.tuple.object.Quad<Register,Register,FieldSet,FieldSet>> shareMoved = new ArrayList<>();

			shareMoved = shar.moveTuplesList(paramCalledRegisters, paramCallerRegisters);
			cycleMoved = cycl.moveTuplesList(paramCalledRegisters, paramCallerRegisters);
			
			Utilities.out("- [FINISHED] CHANGE REGISTERS FROM THE CALLED METHOD TO THE CALLER METHOD");
			Utilities.out("- [INIT] COPY BEFORE TUPLES TO RELS OF CURRENT METHOD");
			for(Pair<Register,FieldSet> p : cycleMoved){
				changed |= relCycle.condAdd(acEntry,p.val0, p.val1);
			}
			for(chord.util.tuple.object.Quad<Register,Register,FieldSet,FieldSet> qu : shareMoved){
				changed |= relShare.condAdd(acEntry,qu.val0, qu.val1, qu.val2, qu.val3);
			}
			Utilities.out("- [FINISHED] COPY BEFORE TUPLES TO RELS OF CURRENT METHOD");
    	}
    	
    	return changed;
    }
    
    public void printOutput() {
    	
    	Hashtable<String, Pair<Register,Register>> registers = RegisterManager.printVarRegMap(acMeth);
    	
		for (Pair<Register,Register> p : registers.values()) 
			for(Pair<Register,Register> q : registers.values())
				accumulatedTuples.askForS(acEntry, p.val0, q.val0);
		for (Pair<Register,Register> p : registers.values()) 
				accumulatedTuples.askForC(acEntry, p.val0);
	}
    
    /**
     * Inserts in the Quad queue all Quads which depend on {@code q} according
     * to USE-DEF analysis (i.e., Quads which are using variables defined by
     * {@code q}). Currently, all quads of a method are inserted in the queue,
     * but this is clearly sub-optimal.
     * 
     * @param q The Quad to be processed.
     */
    protected void wakeUp(Quad q) {
    	queue.fill_fw(getMethod());
    }

	public void save() {
		relShare.save();
		relCycle.save();
	}
}	
